package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.*;
import com.complex.platform.amusement.*;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** PRD §6.3：真实综合体营业日首刷扣次，重入不重复结算；不再预置自然日兼容激活。 */
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
    "spring.datasource.username=complex",
    "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@Transactional
class UnlimitedInDayJdbcIntegrationTests {
    @Autowired TicketingService tickets;
    @Autowired AmusementService amusement;
    @Autowired AmusementRepository repository;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired AmusementBusinessDayService days;
    @Autowired SupervisorReviewService reviews;
    @Autowired TicketAmusementAdmissionService admissions;
    @Autowired JdbcTemplate jdbc;
    private AmusementTicketJdbcFixture.Data f;
    @AfterEach void clear(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}

    @Test void singleTicketFirstSwipeConsumesOnceAndRecordsDayActivationAndSettlementPrice() {
        seed(1,"unlimited_in_day");
        assertThat(verify("first").status()).isEqualTo("success");
        assertThat(used()).isEqualTo(1);
        assertThat(activations()).isEqualTo(1);
        assertThat(prices()).containsExactly(5500L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entitlement_day_activations WHERE entitlement_id=?",Integer.class,f.entitlement())).isZero();
    }

    @Test void singleTicketSameBusinessDayReswipePassesWithoutConsumingOrNewRevenue() {
        seed(1,"unlimited_in_day");
        verify("first"); exit();
        assertThat(verify("again").resultReason()).isEqualTo("day_replay");
        assertThat(used()).isEqualTo(1);
        assertThat(activations()).isEqualTo(1);
        assertThat(prices()).containsExactly(5500L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_verifications WHERE entitlement_id=? AND status='success'",Integer.class,f.entitlement())).isEqualTo(2);
    }

    @Test void singleTicketNextBusinessDayHasNoRemainingUse() {
        seed(1,"unlimited_in_day");
        var window=days.requireWindow(f.project(),Instant.now());
        consume(window.startAt().plusSeconds(60),AmusementEntitlementGateway.Consumption.CONSUMED);
        consume(window.endAt().plusSeconds(1),AmusementEntitlementGateway.Consumption.UNAVAILABLE);
        assertThat(used()).isEqualTo(1);
        assertThat(activations()).isEqualTo(1);
    }

    @Test void multiTicketFirstBusinessDayConsumesOnceThenRealReentryPasses() {
        seed(3,"unlimited_in_day");
        verify("first"); exit(); verify("again");
        assertThat(used()).isEqualTo(1);
        assertThat(activations()).isEqualTo(1);
        assertThat(prices()).containsExactly(5500L);
    }

    @Test void multiTicketConsumesOncePerApprovedBusinessDayThenUsedUp() {
        seed(3,"unlimited_in_day");
        Instant at=days.requireWindow(f.project(),Instant.now()).startAt().plusSeconds(60);
        for(int index=0;index<3;index++) {
            consume(at,AmusementEntitlementGateway.Consumption.CONSUMED);
            consume(at.plusSeconds(1),AmusementEntitlementGateway.Consumption.DAY_REPLAY);
            at=days.requireWindow(f.project(),at).endAt().plusSeconds(1);
        }
        consume(at,AmusementEntitlementGateway.Consumption.UNAVAILABLE);
        assertThat(used()).isEqualTo(3);
        assertThat(activations()).isEqualTo(3);
    }

    @Test void perUseEntitlementStillConsumesEachRealEntry() {
        seed(3,"per_use");
        verify("first"); exit(); verify("second");
        assertThat(used()).isEqualTo(2);
        assertThat(activations()).isZero();
        assertThat(prices()).containsExactly(5500L,5500L);
    }

    private void seed(int uses,String window){f=AmusementTicketJdbcFixture.seed(jdbc,repository,lifecycle,days,reviews,uses,window,20);}
    private VerificationResponse verify(String key){return tickets.createVerification(new VerificationRequest("unlimited_"+key+"_"+f.key(),"entitlement_code",f.code(),f.device(),f.point(),Map.of("staff_id","gate")));}
    private void exit(){amusement.exitVerification(new AmusementExitRequest("exit_"+f.key(),f.code(),f.point(),f.device(),"gate",List.of()));}
    private void consume(Instant at,AmusementEntitlementGateway.Consumption expected){assertThat(admissions.consume(f.entitlement(),days.requireWindow(f.project(),at),at)).isEqualTo(expected);}
    private int used(){return jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,f.entitlement());}
    private int activations(){return jdbc.queryForObject("SELECT count(*) FROM entitlement_business_day_activations WHERE entitlement_id=?",Integer.class,f.entitlement());}
    private List<Long> prices(){return jdbc.queryForList("SELECT settlement_price_cent FROM ticket_verifications WHERE entitlement_id=? AND status='success' AND settlement_price_cent IS NOT NULL ORDER BY created_at",Long.class,f.entitlement());}
}
