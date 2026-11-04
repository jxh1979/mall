package com.complex.platform.ticketing;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.*;
import com.complex.platform.amusement.*;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.ticketing.dto.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** 在线票务入口必须走真实游乐项目、场次及项目硬容量；缺定位不得转通用消费。 */
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
    "spring.datasource.username=complex",
    "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
class AmusementVerifyRoutingJdbcIntegrationTests {
    @Autowired TicketingService tickets;
    @Autowired AmusementService amusement;
    @Autowired AmusementRepository repository;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired AmusementBusinessDayService days;
    @Autowired SupervisorReviewService reviews;
    @Autowired JdbcTemplate jdbc;
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
        assertThat(entries()).isEqualTo(1);
        assertThat(inPlay()).isEqualTo(1);
    }

    private void seed(int capacity){f=AmusementTicketJdbcFixture.seed(jdbc,repository,lifecycle,days,reviews,3,"per_use",capacity);seeded.add(f);}
    private VerificationResponse verify(AmusementTicketJdbcFixture.Data ticket,String request,String point){return tickets.createVerification(new VerificationRequest("route_"+request+"_"+ticket.code(),"entitlement_code",ticket.code(),ticket.device(),point,Map.of("staff_id","gate")));}
    private int used(AmusementTicketJdbcFixture.Data ticket){return jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,ticket.entitlement());}
    private List<String> outcomes(AmusementTicketJdbcFixture.Data ticket){return jdbc.queryForList("SELECT status||'/'||coalesce(result_reason,'')||'/'||source_type FROM ticket_verifications WHERE entitlement_id=? ORDER BY created_at",String.class,ticket.entitlement());}
    private int successes(AmusementTicketJdbcFixture.Data ticket){return jdbc.queryForObject("SELECT count(*) FROM ticket_verifications WHERE entitlement_id=? AND status='success'",Integer.class,ticket.entitlement());}
    private int entries(){return jdbc.queryForObject("SELECT count(*) FROM amusement_verifications WHERE project_id=? AND direction='entry'",Integer.class,f.project());}
    private int inPlay(){return jdbc.queryForObject("SELECT COALESCE(SUM(occupied_count),0) FROM amusement_capacity_snapshot WHERE project_id=? AND dimension='project'",Integer.class,f.project());}
}
