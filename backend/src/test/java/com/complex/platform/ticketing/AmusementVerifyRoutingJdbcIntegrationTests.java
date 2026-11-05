package com.complex.platform.ticketing;

// 文件版本：V0.04

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

/**
 * 在线票务入口必须走真实游乐项目、场次及项目硬容量；缺定位不得转通用消费。
 * 第十八批 Q 段 154-R1（BSM:1689「通票权益核销后才按所属域的已生效规则与实际提供方结算」；BSM:753、ENUM:56）：成功入场落一条结算流水。
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

    @AfterEach void cleanup(){
        try {
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

    private void seed(int capacity){f=AmusementTicketJdbcFixture.seed(jdbc,repository,lifecycle,days,reviews,3,"per_use",capacity);seeded.add(f);}
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
