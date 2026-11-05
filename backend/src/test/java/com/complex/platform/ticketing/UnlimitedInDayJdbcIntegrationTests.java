package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.*;
import com.complex.platform.amusement.*;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.settlement.SettlementReconciliationJob;
import com.complex.platform.settlement.SettlementSettledConsumer;
import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * PRD §6.3：真实综合体营业日首刷扣次，重入不重复结算；不再预置自然日兼容激活。
 * 第十八批 Q 段 154-R2（BSM:1689「通票权益核销后才按所属域的已生效规则与实际提供方结算；未核销不产生提供方结算」）：
 * 补建只补在线消费者会发结算事件的成功核销，序号按这些核销编号；当天重入（day_replay）不补结算行。
 */
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
    "spring.datasource.username=complex",
    "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@Transactional
@RecordApplicationEvents
class UnlimitedInDayJdbcIntegrationTests {
    @Autowired TicketingService tickets;
    @Autowired AmusementService amusement;
    @Autowired AmusementRepository repository;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired AmusementBusinessDayService days;
    @Autowired SupervisorReviewService reviews;
    @Autowired TicketAmusementAdmissionService admissions;
    @Autowired JdbcTemplate jdbc;
    @Autowired ApplicationEvents events;
    @Autowired SettlementSettledConsumer settled;
    @Autowired SettlementReconciliationJob reconciliation;
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

    @Test void rebuildSkipsAmusementDayReplayAndMatchesOnlineSettlement() {
        seedAllocated(3,"unlimited_in_day");
        var first=verify("first"); exit();
        assertThat(verify("again").resultReason()).isEqualTo("day_replay");
        assertRebuildMatchesOnline(first.id(),1);
    }

    @Test void rebuildSkipsGeneralTicketDayReplayAndMatchesOnlineSettlement() {
        seedAllocated(3,"unlimited_in_day");
        jdbc.update("UPDATE issued_entitlements SET entitlement_type='entry' WHERE id=?",f.entitlement());
        var first=verify("first");
        assertThat(verify("again").resultReason()).isEqualTo("day_replay");
        assertRebuildMatchesOnline(first.id(),1);
    }

    @Test void rebuildNumbersConsumedVerificationsLikeUsedTimes() {
        seedAllocated(3,"per_use");
        verify("first"); exit();
        assertRebuildMatchesOnline(verify("second").id(),2);
    }

    /**
     * 154-R2：在线消费者按本事务发出的结算事件落行（提交后监听在回滚的测试事务里不触发，交消费者本体执行）；
     * 删掉 lost 那次的结算行模拟在线消费失败，再跑补建，该权益的结算行须与在线逐行一致。
     */
    private void assertRebuildMatchesOnline(String lost,int consumed) {
        SettlementSettledConsumer consumer=AopTestUtils.getUltimateTargetObject(settled);
        events.stream(SettlementSettledEvent.class)
            .filter(event->f.entitlement().equals(event.entitlementId())).forEach(consumer::onSettlementSettled);
        List<String> online=settledRows();
        assertThat(online).hasSize(consumed);
        assertThat(AppendOnlyTestBypass.update(jdbc,"DELETE FROM settlement_transactions WHERE verification_id=?",lost)).isEqualTo(1);
        reconciliation.rebuildMissingSettlements(Integer.MAX_VALUE);
        assertThat(settledRows()).containsExactlyInAnyOrderElementsOf(online);
    }

    // 154-R2：权益带分摊额，结算行金额按 f(k)−f(k−1) 落非零值（1000 分三次：333、334、333）。
    private void seedAllocated(int uses,String window){seed(uses,window);
        jdbc.update("UPDATE issued_entitlements SET allocated_revenue_cent=1000 WHERE id=?",f.entitlement());}
    private void seed(int uses,String window){f=AmusementTicketJdbcFixture.seed(jdbc,repository,lifecycle,days,reviews,uses,window,20);}
    private VerificationResponse verify(String key){return tickets.createVerification(new VerificationRequest("unlimited_"+key+"_"+f.key(),"entitlement_code",f.code(),f.device(),f.point(),Map.of("staff_id","gate")));}
    private void exit(){amusement.exitVerification(new AmusementExitRequest("exit_"+f.key(),f.code(),f.point(),f.device(),"gate",List.of()));}
    private void consume(Instant at,AmusementEntitlementGateway.Consumption expected){assertThat(admissions.consume(f.entitlement(),days.requireWindow(f.project(),at),at)).isEqualTo(expected);}
    private List<String> settledRows(){return jdbc.queryForList("SELECT verification_id||'/'||verified_amount_cent FROM settlement_transactions WHERE entitlement_id=? AND transaction_type='verification_settled'",String.class,f.entitlement());}
    private int used(){return jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,f.entitlement());}
    private int activations(){return jdbc.queryForObject("SELECT count(*) FROM entitlement_business_day_activations WHERE entitlement_id=?",Integer.class,f.entitlement());}
    private List<Long> prices(){return jdbc.queryForList("SELECT settlement_price_cent FROM ticket_verifications WHERE entitlement_id=? AND status='success' AND settlement_price_cent IS NOT NULL ORDER BY created_at",Long.class,f.entitlement());}
}
