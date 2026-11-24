package com.complex.platform.ticketing;

// 文件版本：V0.06

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
    @Autowired TicketingRepository ticketing;
    private AmusementTicketJdbcFixture.Data f;
    private final List<AmusementTicketJdbcFixture.Data> seeded=new ArrayList<>();
    private final List<String> entryCodes=new ArrayList<>();
    private final List<AmusementTicketJdbcFixture.Data> projects=new ArrayList<>();

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
            for(var project:projects)AmusementTicketJdbcFixture.cleanupProject(jdbc,project);
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

    // 第十八批 CC 段 400-R1（第十八批裁决 83「游乐票的「一次」即一个营业日（场次）：当天首次入场扣一次，当天内各设施任意进、每次进设施都核销留痕」；
    // BSM:800「该票所含的各项权益（各酒吧入场、酒水赠饮、游乐、餐饮券、零售券等），只能在完成检票环节所在的那一个场次内核销」）：组合通票同人份入园 2 次、
    // 游乐按营业日计 2 天，按员工端蛇形键走真实入口：未检票先核游乐 409 ENTITLEMENT_SESSION_NOT_CHECKED_IN 不扣次；检票后游乐首入 200 扣一次，当天再入 200
    // 记 day_replay、不扣次，入场两条。修复前按营业日计次的游乐不判本场检票，第一处断言即红。
    @Test void businessDayAmusementFirstEntryStillNeedsThisSessionsCheckIn() throws Exception {
        seed(2,10,"unlimited_in_day");
        String entry=addEntry(f,2);
        http(f.code(),"cc400a_early").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_SESSION_NOT_CHECKED_IN"));
        assertThat(used(f)).isZero();
        http(entry,"cc400a_in").andExpect(status().isOk());
        http(f.code(),"cc400a_p1").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        http(f.code(),"cc400a_p2").andExpect(status().isOk()).andExpect(jsonPath("$.data.result_reason").value("day_replay"));
        assertThat(used(f)).isEqualTo(1);
        assertThat(entries()).isEqualTo(2);
    }

    // 第十八批 CC 段 400-R2（第十八批裁决 83「游乐票的「一次」即一个营业日（场次）」「多次票即可在多天、不同场次各入场一次」；BSM:780「剩余入场次数跨场次保留至
    // 用尽或票面有效期届满」；BSM:800「已经完成检票环节且本场次履约受影响的人份或次份，另补一次完整票面权益」）：同一人份各游乐设施（组合页每设施一条权益）合计
    // 可入场的营业日数不超过所买次数。纯游乐 1 天票（甲、乙两条权益同在一个项目，跨项目见下一条）：甲权益上一营业日已入场，本营业日用乙权益 409 ENTITLEMENT_USED、不扣次，预检不放行；乙为补偿补发
    // （同一补偿记录号自成一组）时预检放行，甲乙同属一次补发则合计计天、不放行；乙买 2 天时照常入场。修复前乙权益自己计天照常 200，409 那一步即红。
    @Test void amusementBusinessDaysAreCappedPerPersonAcrossEntitlements() throws Exception {
        seed(1,10,"unlimited_in_day");
        var next=AmusementTicketJdbcFixture.anotherTicket(jdbc,f,1,"unlimited_in_day");seeded.add(next);
        jdbc.update("UPDATE issued_entitlements SET parent_ticket_id=?,slot_index=1 WHERE id=?",f.parent(),next.entitlement());
        http(f.code(),"cc400b_day1").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        jdbc.update("""
            UPDATE entitlement_business_day_activations SET business_date=business_date-1,business_start_at=business_start_at-interval '1 day',
              business_end_at=business_end_at-interval '1 day',first_activated_at=first_activated_at-interval '1 day' WHERE entitlement_id=?
            """,f.entitlement());
        http(next.code(),"cc400b_day2").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_USED"));
        assertThat(used(next)).isZero();
        preview(next.code(),"cc400b_pv1").andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(false));
        String compensation="cmp_"+f.key();
        jdbc.update("UPDATE issued_entitlements SET compensation_item_id=? WHERE id=?",compensation,next.entitlement());
        preview(next.code(),"cc400b_pv2").andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));
        jdbc.update("UPDATE issued_entitlements SET compensation_item_id=? WHERE id=?",compensation,f.entitlement());
        preview(next.code(),"cc400b_pv3").andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(false));
        jdbc.update("UPDATE issued_entitlements SET compensation_item_id=NULL WHERE id IN (?,?)",f.entitlement(),next.entitlement());
        jdbc.update("UPDATE issued_entitlements SET total_times=2 WHERE id=?",next.entitlement());
        http(next.code(),"cc400b_day2b").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
    }

    // 第十八批 CC 段 400-R2 复核修复（第十八批裁决 83「游乐票的「一次」即一个营业日（场次）：当天首次入场扣一次，当天内各设施任意进、每次进设施都核销留痕」
    // 「多次票即可在多天、不同场次各入场一次」）：同父票同一人份在甲、乙两个游乐项目各一条 1 天权益（模板分别指定甲、乙，同组合页每设施一条），按员工端蛇形键
    // 走真实入口：同一营业日先甲后乙都 200、各扣一次、两个项目各一条入场；把这一营业日的入场挪到上一营业日（模拟次日）后，乙项目再一条 1 天权益首入
    // 409 ENTITLEMENT_USED、不扣次。去掉合计计天里的「不含当天」，乙当天那一步即红。
    @Test void oneBusinessDayCoversEveryProjectOfThePersonThenCapsTheNextDay() throws Exception {
        seed(1,10,"unlimited_in_day");
        jdbc.update("UPDATE entitlement_templates SET amusement_project_id=? WHERE id=?",f.project(),f.template());
        http(f.code(),"cc400c_a").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        var b=otherProject(f);
        var rideB=ride(b.project());
        http(rideB.code(),"cc400c_b",b.device(),b.point()).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        assertThat(used(f)).isEqualTo(1);
        assertThat(used(rideB)).isEqualTo(1);
        assertThat(entries(f.project())).isEqualTo(1);
        assertThat(entries(b.project())).isEqualTo(1);
        jdbc.update("""
            UPDATE entitlement_business_day_activations SET business_date=business_date-1,business_start_at=business_start_at-interval '1 day',
              business_end_at=business_end_at-interval '1 day',first_activated_at=first_activated_at-interval '1 day' WHERE entitlement_id IN (?,?)
            """,f.entitlement(),rideB.entitlement());
        var nextB=ride(b.project());
        http(nextB.code(),"cc400c_b2",b.device(),b.point()).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_USED"));
        assertThat(used(nextB)).isZero();
    }

    // 第十八批 CC 段 400-R1 复核修复（ENUM:48「多人票中本人从未刷码的人份……于该场次结束时按本条处理，不等票面有效期届满」；BSM:809「转出时，系统须同时向
    // 转出人与受赠人明示这一场次限制」「若该人份权益所绑定的场次已经结束……此时不发生领取」）：双人纯游乐票 1 号人份按员工端蛇形键首入后，父票首场次键为
    // 入场营业日，首场次（门票转赠场次限制所示）为该营业日、结束时刻定得出（该营业日闭店时点）。修复前首场次键为「amusement:综合体编号:营业日」，键断言即红。
    @Test void twoPersonAmusementOnlyTicketKeysItsFirstSessionByTheBusinessDay() throws Exception {
        seed(1,10,"unlimited_in_day");
        jdbc.update("UPDATE ticket_instances SET num_people=2 WHERE id=?",f.parent());
        http(f.code(),"cc400d_p1").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        String day=jdbc.queryForObject("SELECT business_date::text FROM entitlement_business_day_activations WHERE entitlement_id=?",String.class,f.entitlement());
        assertThat(jdbc.queryForObject("SELECT first_session_key FROM ticket_instances WHERE id=?",String.class,f.parent())).isEqualTo(day);
        var session=ticketing.findFirstSession(f.parent()).orElseThrow();
        assertThat(session.label()).isEqualTo(day);
        assertThat(session.end()).isNotNull();
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
    private void seed(int uses,int capacity){seed(uses,capacity,"per_use");}
    /** 400-R1：消耗窗口由参数给。 */
    private void seed(int uses,int capacity,String window){f=AmusementTicketJdbcFixture.seed(jdbc,repository,lifecycle,days,reviews,uses,window,capacity);seeded.add(f);}
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
    /** 400-R2 复核修复：同父票 1 号人份再加一条 1 天游乐权益，模板指定游乐项目 project。 */
    private AmusementTicketJdbcFixture.Data ride(String project){
        var ride=AmusementTicketJdbcFixture.anotherTicket(jdbc,f,1,"unlimited_in_day");seeded.add(ride);
        jdbc.update("UPDATE issued_entitlements SET parent_ticket_id=?,slot_index=1 WHERE id=?",f.parent(),ride.entitlement());
        jdbc.update("UPDATE entitlement_templates SET amusement_project_id=? WHERE id=?",project,ride.template());
        return ride;
    }
    /** 400-R2 复核修复：同一综合体再建游乐项目乙（类型绑定、巡检通过、空间、开放场次、设备，写法同 AmusementEntitlementProjectBindingJdbcIntegrationTests.otherProject）。 */
    private AmusementTicketJdbcFixture.Data otherProject(AmusementTicketJdbcFixture.Data source){
        String k=source.key()+"c";
        var other=new AmusementTicketJdbcFixture.Data(k,source.complex(),"ajf_project_"+k,"ajf_session_"+k,"ajf_safety_"+k,
                null,null,null,null,null,null,"ajf_device_"+k,"ajf_point_"+k);
        projects.add(other);
        jdbc.update("""
            INSERT INTO amusement_safety_templates(id,template_name,items_json,status,created_at,updated_at)
            VALUES(?,'跨项目测试巡检','[{"name":"restraint","required":true}]'::jsonb,'active',now(),now())
            """,other.safety());
        jdbc.update("""
            INSERT INTO amusement_projects(id,project_name,capacity,open_status,safety_template_id,
              eligibility_rule_json,location_point_id,created_at,updated_at)
            VALUES(?,'跨项目测试乙项目',10,'open',?,'{}'::jsonb,?,now(),now())
            """,other.project(),other.safety(),other.point());
        jdbc.update("INSERT INTO amusement_project_entitlements(id,project_id,entitlement_type,created_at) VALUES(?,?,'amusement_pass',now())",
                "ajf_binding_"+k,other.project());
        jdbc.update("""
            INSERT INTO amusement_safety_inspections(id,project_id,template_id,items_json,overall_result,
              inspection_status,rectification_status,reopen_status,inspected_at,submitted_at,created_at,updated_at)
            VALUES(?,?,?,'[{"name":"restraint","result":"pass"}]'::jsonb,'pass','submitted',
              'not_required','not_required',now(),now(),now(),now())
            ""","ajf_inspect_"+k,other.project(),other.safety());
        AmusementBusinessDayTestFixture.bind(jdbc,other.project(),source.complex());
        jdbc.update("""
            INSERT INTO amusement_sessions(id,project_id,start_time,end_time,capacity,used_capacity,status,created_at,updated_at)
            VALUES(?,?,now()-interval '1 hour',now()+interval '1 hour',10,0,'open',now(),now())
            """,other.session(),other.project());
        lifecycle.lockSessionWindow(repository.findSessionById(other.session()).orElseThrow());
        jdbc.update("""
            INSERT INTO terminal_devices(id,device_id,device_code,terminal_type,binding_scope,status,created_at,updated_at)
            VALUES(?,?,?,'kiosk','venue','bound',now(),now())
            """,other.device(),"physical_"+k,"terminal_"+k);
        return other;
    }
    /** 员工端门票核销屏实发形状（蛇形键、credential_type=qr）走真实入口与授权链。 */
    private AccessActor actor;
    private ResultActions http(String code,String request) throws Exception {return http(code,request,f.device(),f.point());}
    /** 400-R2 复核修复：在指定设备与点位核销。 */
    private ResultActions http(String code,String request,String device,String point) throws Exception {return http("/api/v1/ticketing/verifications",code,request,device,point);}
    /** 400-R2：核销预检，同一请求体。 */
    private ResultActions preview(String code,String request) throws Exception {return http("/api/v1/ticketing/verifications/preview",code,request,f.device(),f.point());}
    private ResultActions http(String path,String code,String request,String device,String point) throws Exception {
        // 夹具把操作人放在安全上下文里，首个请求结束即被清掉：首次取用时留存。
        if(actor==null)actor=(AccessActor)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String body=JSON.writeValueAsString(Map.of("idempotency_key","route_"+request+"_"+code,"credential_type","qr",
                "credential_payload",code,"device_id",device,"point_id",point,
                "business_context",Map.of("scenario","ticket_checkin","staff_id","gate")));
        return mockMvc.perform(post(path).with(r->{
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
    private int entries(){return entries(f.project());}
    private int entries(String project){return jdbc.queryForObject("SELECT count(*) FROM amusement_verifications WHERE project_id=? AND direction='entry'",Integer.class,project);}
    private void assertSettledOnce(AmusementTicketJdbcFixture.Data ticket,String verificationId){
        assertThat(jdbc.queryForList("SELECT verification_id||'/'||transaction_type||'/'||entitlement_id||'/'||(length(id)<=64) FROM settlement_transactions WHERE transaction_no=?",
                String.class,"ST-VS-"+verificationId)).containsExactly(verificationId+"/verification_settled/"+ticket.entitlement()+"/true");
    }
    private int inPlay(){return jdbc.queryForObject("SELECT COALESCE(SUM(occupied_count),0) FROM amusement_capacity_snapshot WHERE project_id=? AND dimension='project'",Integer.class,f.project());}
}
