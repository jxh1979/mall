package com.complex.platform.ticketing;

// 文件版本：V0.05

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.*;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.ticketing.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 在线票务入口必须走真实游乐项目、场次及项目硬容量；缺定位不得转通用消费。
 * 第十八批 Q 段 154-R1（BSM:1689「通票权益核销后才按所属域的已生效规则与实际提供方结算」；BSM:753、ENUM:56）：成功入场落一条结算流水。
 * 第十八批 AH 段第 219 条：票内游乐须本场已检票、本场按一份封顶；同一开放场次里多次票再玩照常入场（第 219 条补充）。
 */
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
    "spring.datasource.username=complex",
    "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@AutoConfigureMockMvc
class AmusementVerifyRoutingJdbcIntegrationTests {
    private static final ObjectMapper JSON=new ObjectMapper();
    @Autowired TicketingService tickets;
    @Autowired AmusementService amusement;
    @Autowired AmusementRepository repository;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired AmusementBusinessDayService days;
    @Autowired SupervisorReviewService reviews;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;
    private AmusementTicketJdbcFixture.Data f;
    private final List<AmusementTicketJdbcFixture.Data> seeded=new ArrayList<>();
    private final List<String> entryCodes=new ArrayList<>();

    @AfterEach void cleanup(){
        try {
            for(String entry:entryCodes){
                com.complex.platform.support.AppendOnlyTestBypass.update(jdbc,"DELETE FROM settlement_transactions WHERE entitlement_id=?",entry);
                jdbc.update("DELETE FROM ticket_verifications WHERE entitlement_id=?",entry);
                jdbc.update("DELETE FROM issued_entitlements WHERE id=?",entry);
                jdbc.update("DELETE FROM entitlement_templates WHERE id=?",entry);
            }
            for(var ticket:seeded)AmusementTicketJdbcFixture.cleanupTicket(jdbc,ticket);
            if(f!=null)AmusementTicketJdbcFixture.cleanupProject(jdbc,f);
        } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    }

    @Test void amusementEntitlementRoutesToRealAdmissionAndCrossReferencedTicketFact() {
        seed(5);
        var result=verify(f,"first",f.point());
        assertThat(result.status()).isEqualTo("success");
        assertThat(used(f)).isEqualTo(1);
        assertThat(entries()).isEqualTo(1);
        assertThat(inPlay()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT used_capacity FROM amusement_sessions WHERE id=?",Integer.class,f.session())).isZero();
        String reference=jdbc.queryForObject("SELECT amusement_verification_id FROM ticket_verifications WHERE id=?",String.class,result.id());
        assertThat(reference).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT session_id FROM amusement_verifications WHERE id=?",String.class,reference)).isEqualTo(f.session());
        assertSettledOnce(f,result.id());
    }

    // 154-R1：按安卓门票核销屏实发形状（蛇形键、credential_type=qr、business_context 带 scenario 与 staff_id）走真实入口与授权链。
    @Test void terminalShapedHttpVerificationSettlesAmusementAdmission() throws Exception {
        seed(5);
        var actor=(AccessActor)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String body=JSON.writeValueAsString(Map.of("idempotency_key","route_http_"+f.code(),"credential_type","qr",
                "credential_payload",f.code(),"device_id",f.device(),"point_id",f.point(),
                "business_context",Map.of("scenario","ticket_checkin","staff_id","gate")));
        String response=mockMvc.perform(post("/api/v1/ticketing/verifications").with(request->{
                    request.addHeader("X-Terminal-Type","web");
                    return authentication(new UsernamePasswordAuthenticationToken(actor,null,
                            List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);})
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertSettledOnce(f,JSON.readTree(response).path("data").path("id").asText());
    }

    // 134-R1（CG 裁决第 46 项）：规则拒绝在业务事务回滚、放开权益行锁之后另起事务落 rejected；分线程超时守住同线程两连接互等。
    @Test @Timeout(value=60,threadMode=Timeout.ThreadMode.SEPARATE_THREAD)
    void fullProjectRejectsAnotherTicketWithoutConsumingOrCreatingSuccess() {
        seed(1);
        verify(f,"fill",f.point());
        var next=AmusementTicketJdbcFixture.anotherTicket(jdbc,f,3,"per_use");seeded.add(next);
        assertThatThrownBy(()->verify(next,"over_capacity",f.point()))
                .isInstanceOfSatisfying(ApiException.class,error->assertThat(error.code()).isEqualTo("SESSION_CAPACITY_EXHAUSTED"));
        assertThat(outcomes(next)).containsExactly("rejected/session_capacity_exhausted/android_terminal_online");
        assertThat(used(next)).isZero();
        assertThat(successes(next)).isZero();
        assertThat(entries()).isEqualTo(1);
        assertThat(inPlay()).isEqualTo(1);
    }

    @Test void unboundPointRejectsWithoutGenericFallback() {
        seed(5);
        assertThatThrownBy(()->verify(f,"unbound","orphan_"+f.key()))
                .isInstanceOfSatisfying(ApiException.class,error->assertThat(error.code()).isEqualTo("AMUSEMENT_PROJECT_UNRESOLVED"));
        assertThat(outcomes(f)).containsExactly("rejected/amusement_project_unresolved/android_terminal_online");
        assertThat(used(f)).isZero();
        assertThat(successes(f)).isZero();
        assertThat(entries()).isZero();
    }

    @Test void noCurrentOpenSessionRejectsWithoutDirtyData() {
        seed(5);
        jdbc.update("UPDATE amusement_sessions SET start_time=now()-interval '2 hours',end_time=now()-interval '1 hour' WHERE id=?",f.session());
        assertThatThrownBy(()->verify(f,"past_session",f.point()))
                .isInstanceOfSatisfying(ApiException.class,error->assertThat(error.code()).isEqualTo("AMUSEMENT_NO_OPEN_SESSION"));
        assertThat(outcomes(f)).containsExactly("rejected/amusement_no_open_session/android_terminal_online");
        assertThat(used(f)).isZero();
        assertThat(successes(f)).isZero();
        assertThat(entries()).isZero();
    }

    @Test void identicalOnlineRequestReturnsTheSameRealAdmissionWithoutDoubleConsumption() {
        seed(5);
        var first=verify(f,"same_request",f.point());
        var second=verify(f,"same_request",f.point());
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(used(f)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM settlement_transactions WHERE entitlement_id=? AND transaction_type='verification_settled'",
                Integer.class,f.entitlement())).isEqualTo(1);
        assertThat(entries()).isEqualTo(1);
        assertThat(inPlay()).isEqualTo(1);
    }

    // 第十八批 AH 段第 219 条 219-R1（BSM:800「该票所含的各项权益（各酒吧入场、酒水赠饮、游乐、餐饮券、零售券等），只能在完成检票环节所在的那一个场次内核销」
    // 「后续场次的附带权益也不得提前到当前场次使用」；ENUM:46「该窗口内附带权益按完整一份配足、仅限当场消耗」）：组合通票同人份入园 2 次、游乐 4 次（每场一份 2 次），
    // 按员工端蛇形键走真实入口：未检票先核游乐 409 ENTITLEMENT_SESSION_NOT_CHECKED_IN 不扣次；检票后游乐核 2 次 200，第 3 次 409 ENTITLEMENT_SESSION_SHARE_USED_UP
    // 不扣次；把本场核销挪到上一营业日（模拟下一场）再检票，游乐又可核 2 次。修复前未检票核游乐 200，第一处断言即红。
    @Test void comboTicketAmusementNeedsThisSessionsCheckInAndIsCappedAtOneShare() throws Exception {
        seed(4,10);
        String entry=addEntry(f,2);
        http(f.code(),"ah219a_early").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_SESSION_NOT_CHECKED_IN"));
        assertThat(used(f)).isZero();
        http(entry,"ah219a_in1").andExpect(status().isOk());
        http(f.code(),"ah219a_p1").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        http(f.code(),"ah219a_p2").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        http(f.code(),"ah219a_p3").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_SESSION_SHARE_USED_UP"));
        assertThat(used(f)).isEqualTo(2);
        jdbc.update("UPDATE ticket_verifications SET created_at=created_at-interval '1 day' WHERE entitlement_id IN (?,?)",f.entitlement(),entry);
        http(entry,"ah219a_in2").andExpect(status().isOk());
        http(f.code(),"ah219a_p4").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        http(f.code(),"ah219a_p5").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        assertThat(used(f)).isEqualTo(4);
    }

    // 219-R1（BSM:800「该窗口内票面附带的各项权益……按完整一份配足、完整可用」）：单次票（入园 1 次、游乐 3 次）检票后唯一这一场 3 次游乐都 200。
    @Test void singleVisitTicketUsesAllItsAmusementInItsOnlySession() throws Exception {
        seed(3,10);
        String entry=addEntry(f,1);
        http(entry,"ah219b_in").andExpect(status().isOk());
        for(int i=1;i<=3;i++)
            http(f.code(),"ah219b_p"+i).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        assertThat(used(f)).isEqualTo(3);
    }

    // 第十八批 AH 段第 219 条补充（BSM:780「按可用次数逐次核销（每用一次扣一次，剩余为 0 才算用完）」；ENUM:144「含一人多次票当天多次入场、当天不限次数一类表述」；
    // BSM:1263 在玩人数由入场核销增加、出场自动扣减与场次或营业日结束释放）：出场感应未接、首次入场尚未释放时，多次游乐票同一开放场次再玩一次照常——
    // 200、各扣一次、入场两条、在玩 2 人。修复前第二次撞 V234 唯一约束整笔回滚 409 STATE_CONFLICT，状态断言即红。
    @Test void multiUseAmusementPlaysAgainInTheSameOpenSessionBeforeRelease() throws Exception {
        seed(3,10);
        http(f.code(),"ah219c_p1").andExpect(status().isOk());
        http(f.code(),"ah219c_p2").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        assertThat(used(f)).isEqualTo(2);
        assertThat(entries()).isEqualTo(2);
        assertThat(inPlay()).isEqualTo(2);
    }

    // 18AH 复核修复（BSM:1263「已售但未履行的权益按既有取消、补偿规则处理并留痕」；决策台账 TK-18「已入场（已扣次）的客人按 ENUM L148……补偿（原票下恢复本次 1 次）」；
    // ENUM:148「同一故障下同一原人份或次份只补偿一次」）：同一权益同一开放场次入场两次后提前结束，入场待办按权益只建一条（取最新那条入场，与补偿取数同一条），
    // 两岗确认发放后写回，不再留 pending 的入场待办。全程真实入口、蛇形键。修复前每条入场各建一条，较早那条一直 pending，断言即红。
    @Test void sameEntitlementEnteredTwiceLeavesNoPendingEntryFollowupAfterEarlyEndCompensationIssued() throws Exception {
        seed(3,10);
        var t=f;
        http(t.code(),"ah219f_p1").andExpect(status().isOk());
        http(t.code(),"ah219f_p2").andExpect(status().isOk());
        as(staff(t.complex(),List.of("amusement.session.end","amusement.session.view")),"/api/v1/amusement/sessions/"+t.session()+"/end",
                Map.of("idempotency_key","ah219f_end_"+t.code(),"clearance_confirmed",true,"cleared_at",java.time.Instant.now().toString(),
                        "reason","设备临时停机，现场清场完成","evidence_attachment_ids",List.of()))
                .andExpect(status().isOk());
        // 清场原件不可删除（同 AmusementHardCapacityAutoReopenJdbcIntegrationTests）：本用例的票与项目留作测试证据，不进 @AfterEach 清理。
        seeded.remove(t);
        f=null;
        String batch=jdbc.queryForObject("SELECT DISTINCT batch_id FROM amusement_fault_compensation_items WHERE entitlement_id=?",String.class,t.entitlement());
        as(staff(t.complex(),List.of("amusement.park_fault.confirm_as_head","amusement.park_fault.view")),
                "/api/v1/amusement/compensation-batches/"+batch+"/confirm",
                Map.of("idempotency_key","ah219f_head_"+t.code(),"role","amusement_head","note","负责人确认"))
                .andExpect(status().isOk());
        as(staff(t.complex(),List.of("amusement.park_fault.confirm_as_engineering_safety","amusement.park_fault.view")),
                "/api/v1/amusement/compensation-batches/"+batch+"/confirm",
                Map.of("idempotency_key","ah219f_eng_"+t.code(),"role","engineering_safety","note","工程岗确认"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("issued"));
        assertThat(jdbc.queryForList("""
            SELECT f.status FROM amusement_session_followups f JOIN amusement_session_endings e ON e.id=f.ending_id
             WHERE e.session_id=? AND f.source_type='entry'
            """,String.class,t.session())).as("入场待办按权益一条、发放后写回").containsExactly("completed");
    }

    private void seed(int capacity){f=AmusementTicketJdbcFixture.seed(jdbc,repository,lifecycle,days,reviews,3,"per_use",capacity);seeded.add(f);}
    private void seed(int uses,int capacity){f=AmusementTicketJdbcFixture.seed(jdbc,repository,lifecycle,days,reviews,uses,"per_use",capacity);seeded.add(f);}
    /** 219-R1：同父票 1 号人份加一张入园权益（总次数 times），码即 id。 */
    private String addEntry(AmusementTicketJdbcFixture.Data ticket,int times){
        String code="entry_"+ticket.code();
        jdbc.update("""
            INSERT INTO entitlement_templates(id,name,entitlement_type,default_total_times,default_value_cent,status,created_at,updated_at)
            VALUES(?,'组合通票入园','entry',?,0,'active',now(),now())
            """,code,times);
        jdbc.update("""
            INSERT INTO issued_entitlements(id,ticket_type_id,order_id,entitlement_template_id,entitlement_code,
              entitlement_type,total_times,used_times,value_amount_cent,allocated_revenue_cent,status,
              valid_from,valid_to,parent_ticket_id,slot_index,created_at,updated_at)
            VALUES(?,?,?,?,?,'entry',?,0,0,0,'active',now()-interval '7 days',now()+interval '7 days',?,1,now(),now())
            """,code,ticket.type(),ticket.order(),code,code,times,ticket.parent());
        entryCodes.add(code);
        return code;
    }
    /** 员工端门票核销屏实发形状（蛇形键、credential_type=qr）走真实入口与授权链。 */
    private AccessActor actor;
    private ResultActions http(String code,String request) throws Exception {
        // 夹具把操作人放在安全上下文里，首个请求结束即被清掉：首次取用时留存。
        if(actor==null)actor=(AccessActor)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String body=JSON.writeValueAsString(Map.of("idempotency_key","route_"+request+"_"+code,"credential_type","qr",
                "credential_payload",code,"device_id",f.device(),"point_id",f.point(),
                "business_context",Map.of("scenario","ticket_checkin","staff_id","gate")));
        return mockMvc.perform(post("/api/v1/ticketing/verifications").with(r->{
                    r.addHeader("X-Terminal-Type","web");
                    return authentication(new UsernamePasswordAuthenticationToken(actor,null,
                            List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(r);})
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    /** 18AH 复核修复：以指定操作人按真实请求形状（蛇形键）调后台写入口。 */
    private ResultActions as(AccessActor who,String path,Map<String,?> body) throws Exception {
        return mockMvc.perform(post(path).with(r->{
                    r.addHeader("X-Terminal-Type","web");
                    return authentication(new UsernamePasswordAuthenticationToken(who,null,List.of())).postProcessRequest(r);})
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }
    /** 本综合体员工（中文角色名、后缀纯数字，见 AmusementBusinessDayTestFixture.actor）。 */
    private AccessActor staff(String complex,List<String> permissions){
        String user=AmusementBusinessDayTestFixture.actor(jdbc,complex,permissions);
        return new AccessActor(user,user,"staff",complex,null,"active");
    }
    private VerificationResponse verify(AmusementTicketJdbcFixture.Data ticket,String request,String point){return tickets.createVerification(new VerificationRequest("route_"+request+"_"+ticket.code(),"entitlement_code",ticket.code(),ticket.device(),point,Map.of("staff_id","gate")));}
    private int used(AmusementTicketJdbcFixture.Data ticket){return jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,ticket.entitlement());}
    private List<String> outcomes(AmusementTicketJdbcFixture.Data ticket){return jdbc.queryForList("SELECT status||'/'||coalesce(result_reason,'')||'/'||source_type FROM ticket_verifications WHERE entitlement_id=? ORDER BY created_at",String.class,ticket.entitlement());}
    private int successes(AmusementTicketJdbcFixture.Data ticket){return jdbc.queryForObject("SELECT count(*) FROM ticket_verifications WHERE entitlement_id=? AND status='success'",Integer.class,ticket.entitlement());}
    private int entries(){return jdbc.queryForObject("SELECT count(*) FROM amusement_verifications WHERE project_id=? AND direction='entry'",Integer.class,f.project());}
    private void assertSettledOnce(AmusementTicketJdbcFixture.Data ticket,String verificationId){
        assertThat(jdbc.queryForList("SELECT verification_id||'/'||transaction_type||'/'||entitlement_id||'/'||(length(id)<=64) FROM settlement_transactions WHERE transaction_no=?",
                String.class,"ST-VS-"+verificationId)).containsExactly(verificationId+"/verification_settled/"+ticket.entitlement()+"/true");
    }
    private int inPlay(){return jdbc.queryForObject("SELECT COALESCE(SUM(occupied_count),0) FROM amusement_capacity_snapshot WHERE project_id=? AND dimension='project'",Integer.class,f.project());}
}
