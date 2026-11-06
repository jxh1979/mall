package com.complex.platform.settlement;

// 文件版本：V0.05

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.ticketing.SettlementSettledEvent;
import com.complex.platform.ticketing.VerificationReversedEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 结算/退款状态机重构集成验收(审计 AUD-012/013/017/019 资金门禁)。
 *
 * <p>直接对拍真实业务表(不走核销主链):seed settlement_transactions / issued_entitlements / ticket_verifications,
 * 调消费者/作业可调方法,断言不可变账本 + 冲正净额 + 到期互斥 + 重建结果。非 @Transactional(真提交,@AfterEach 清理)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementStateMachineJdbcIntegrationTests {

    @Autowired private SettlementSettledConsumer settledConsumer;
    @Autowired private SettlementReversedConsumer reversedConsumer;
    @Autowired private SettlementExpiryJob expiryJob;
    @Autowired private SettlementReconciliationJob reconciliationJob;
    @Autowired private JdbcSettlementBillRepository billRepository;
    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private SettlementPostingService postingService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private BusinessDayResolver businessDays;

    private final List<String> projects = new ArrayList<>();
    private final List<String> entIds = new ArrayList<>();
    private final List<String> verIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String v : verIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE transaction_no IN (?, ?)",
                    "ST-VS-" + v, "ST-VR-" + v);
            jdbcTemplate.update("DELETE FROM ticket_verifications WHERE id = ?", v);
        }
        for (String e : entIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", e);
            jdbcTemplate.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", e);
            jdbcTemplate.update("DELETE FROM ticket_instances WHERE id = ?", "inst_" + e);
            jdbcTemplate.update("DELETE FROM issued_entitlements WHERE id = ?", e);
            jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + e);
        }
        if (!entIds.isEmpty()) { // 夹具建的提供方商户与售票项目(在引用它的权益/订单之后删,FK 顺序)
            jdbcTemplate.update("DELETE FROM merchants WHERE id = 'mch_sm'");
            jdbcTemplate.update("DELETE FROM projects WHERE id = 'proj_sm_exp'");
        }
        for (String p : projects) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE project_id = ?", p);
            jdbcTemplate.update("DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)", p);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", p);
            com.complex.platform.support.PassPoolFixtures.deleteByProject(jdbcTemplate, p);
            jdbcTemplate.update("DELETE FROM ticket_distribution_rank_tiers WHERE project_id = ?", p);
        }
    }

    // ─── AUD-012：核销按 verification_id 写不可变增量，按业务时间落月 ───────────────────

    @Test
    void verificationIncrementsAreAttributedToTheirOwnBusinessMonthNotFirstInsert() {
        String p = uniqueProject();
        Instant jan = Instant.parse("2011-01-15T10:00:00Z");
        Instant feb = Instant.parse("2011-02-15T10:00:00Z");
        // 同权益跨月两次核销：各写一条增量行(1000/1000)，business_at 各归其月。
        seedSettledFact("v_" + p + "_1", "ent_" + p, p, "mch_" + p, jan, 1000L, null);
        seedSettledFact("v_" + p + "_2", "ent_" + p, p, "mch_" + p, feb, 1000L, null);

        assertThat(billRepository.sumPooledTotals(p, monthStart(jan), monthEnd(jan)).revenueCent()).isEqualTo(1000L);
        assertThat(billRepository.sumPooledTotals(p, monthStart(feb), monthEnd(feb)).revenueCent()).isEqualTo(1000L);
    }

    @Test
    void settledConsumerWritesImmutableIncrementAndIsReplayIdempotent() {
        String p = uniqueProject();
        String ent = "ent_" + p;
        entIds.add(ent);
        Instant t = Instant.parse("2011-03-10T09:00:00Z");
        // 第 1 次核销：allocated=1000,total=2 → 增量 f(1)-f(0)=500。
        SettlementSettledEvent e1 = new SettlementSettledEvent(
                "vc_" + p + "_1", t, ent, "order_x", "tt_x", "entry", "mch_" + p, 2, 1, 1, 1000L,
                t.plusSeconds(3600), null, null, null, null, "tkt_" + p);
        settledConsumer.onSettlementSettled(e1);
        settledConsumer.onSettlementSettled(e1); // 重放：ON CONFLICT DO NOTHING
        // 第 2 次核销(k=2)：增量 f(2)-f(1)=500，独立成行。
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                "vc_" + p + "_2", t, ent, "order_x", "tt_x", "entry", "mch_" + p, 2, 2, 1, 1000L,
                t.plusSeconds(3600), null, null, null, null, "tkt_" + p));

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement_transactions WHERE entitlement_id = ? AND transaction_type='verification_settled'",
                Integer.class, ent);
        Long sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(verified_amount_cent),0) FROM settlement_transactions WHERE entitlement_id = ?",
                Long.class, ent);
        assertThat(rows).isEqualTo(2);      // 两次核销两行(重放不增)
        assertThat(sum).isEqualTo(1000L);   // 增量相加 == f(2) == allocated
    }

    // ─── AUD-013：冲正写负向结算，净额一次且可审计 ────────────────────────────────

    @Test
    void reversalWritesNegativeFactAndUnpostedMonthNetsToZeroIdempotently() {
        String p = uniqueProject();
        Instant jan = Instant.parse("2011-04-15T10:00:00Z");
        seedSettledFact("v_" + p, "ent_" + p, p, "mch_" + p, jan, 1000L, null);

        VerificationReversedEvent ev = new VerificationReversedEvent(
                "v_" + p, "rev_" + p, "ent_" + p, "order_x", "mch_" + p, 1);
        reversedConsumer.onVerificationReversed(ev);
        reversedConsumer.onVerificationReversed(ev); // 重放：同 reversalId 幂等

        Integer negRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement_transactions WHERE transaction_no = ?",
                Integer.class, "ST-VR-rev_" + p);
        assertThat(negRows).isEqualTo(1);
        // 未过账月：正+负净为 0。
        assertThat(billRepository.sumPooledTotals(p, monthStart(jan), monthEnd(jan)).revenueCent()).isZero();
    }

    @Test
    void reversalWithoutOriginalSettlementIsNoOp() {
        String p = uniqueProject();
        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                "missing_" + p, "rev_" + p, "ent_" + p, "order_x", "mch_" + p, 1));
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement_transactions WHERE transaction_no = ?",
                Integer.class, "ST-VR-rev_" + p);
        assertThat(rows).isZero(); // 无正向可冲 → 不写负向(净额已正确)
    }

    @Test
    void reversalOfPostedMonthClawsBackInCurrentMonthNotEditingHistory() {
        String p = uniqueProject();
        Instant jan = Instant.parse("2011-05-15T10:00:00Z");
        seedSettledFact("v_" + p, "ent_" + p, p, "mch_" + p, jan, 1000L, null);
        seedBill(p, YearMonth.from(jan.atZone(ZoneOffset.UTC)), "settled"); // 已过账

        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                "v_" + p, "rev_" + p, "ent_" + p, "order_x", "mch_" + p, 1));

        // 历史(Jan)不改：仍为原正向 1000（不删历史行）。
        assertThat(billRepository.sumPooledTotals(p, monthStart(jan), monthEnd(jan)).revenueCent()).isEqualTo(1000L);
        // 负向红冲落当月(now)，不在 Jan 窗内。
        Instant vrBusiness = jdbcTemplate.queryForObject(
                "SELECT business_at FROM settlement_transactions WHERE transaction_no = ?",
                Instant.class, "ST-VR-rev_" + p);
        assertThat(vrBusiness).isAfter(monthEnd(jan));
    }

    /**
     * FS-021-R2 生产形态：原月由月结作业出平台单（project_id 为空、覆盖全部项目）并已结付，核销交易行带核销项目——
     * 撤销仍须落当月，由当月单列追收（操作员裁决 2026-10-19 第 4 条），不得落回已结付的原月被丢弃。
     */
    @Test
    void reversalOfMonthSettledByPlatformBillLandsInCurrentMonth() {
        String p = uniqueProject();
        Instant nov = Instant.parse("2011-11-20T10:00:00Z");
        seedSettledFact("v_" + p, "ent_" + p, p, "mch_" + p, nov, 1000L, null);
        YearMonth month = YearMonth.from(nov.atZone(ZoneOffset.UTC));
        String platformBill = "sb_platform_" + p;
        jdbcTemplate.update("""
                INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)
                VALUES (?, NULL, ?, ?, 'settled', now(), now())
                """, platformBill, java.sql.Date.valueOf(month.atDay(1)), java.sql.Date.valueOf(month.atEndOfMonth()));
        try {
            reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                    "v_" + p, "rev_" + p, "ent_" + p, "order_x", "mch_" + p, 1));

            Instant vrBusiness = jdbcTemplate.queryForObject(
                    "SELECT business_at FROM settlement_transactions WHERE transaction_no = ?", Instant.class, "ST-VR-rev_" + p);
            assertThat(vrBusiness).as("原月平台单已结付 → 冲正落当月").isAfter(monthEnd(nov));
        } finally {
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE id = ?", platformBill);
        }
    }

    // ─── FS-021-R2：已结付月份后撤销的核销单列追收、按原月重算（操作员裁决 2026-10-19 第 4 条） ───────────

    /**
     * 单店（专属直付）：原月已结付后撤销的那笔，按该次核销原结付金额单列追收；当月新核销照常全额应付，两笔分别收付（BSM L755 不得轧差）。
     */
    @Test
    void exclusivePostedMonthReversalIsSeparateRecoveryNotNetted() {
        String p = uniqueProject();
        String bar = "smx_bar"; // 明细编号含账单号与商户号，商户号取短值
        Instant sep = Instant.parse("2011-09-28T10:00:00Z");
        seedSettledFact("vx_old_" + p, "ent_old_" + p, p, bar, sep, 2000L, bar);
        seedBill(p, YearMonth.from(sep.atZone(ZoneOffset.UTC)), "settled"); // 原月已结付
        YearMonth current = YearMonth.from(businessDays.businessDate(p, null, Instant.now()));
        seedSettledFact("vx_new_" + p, "ent_new_" + p, p, bar, current.atDay(1).atTime(12, 0).toInstant(ZoneOffset.UTC),
                10000L, bar);
        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                "vx_old_" + p, "rx_" + p, "ent_old_" + p, "order_x", bar, 1)); // 冲正落当月

        String billId = settlementService.settleMonth(p, current);

        assertThat(itemAmount(billId, bar, "exclusive_full")).as("当月应付照常全额").isEqualTo(10000L);
        assertThat(itemAmount(billId, bar, "exclusive_recovery")).as("撤销那笔单列追收原结付金额").isEqualTo(-2000L);
        String originalBill = "sb_" + p + "_" + YearMonth.from(sep.atZone(ZoneOffset.UTC));
        assertThat(jdbcTemplate.queryForObject("""
                SELECT r.amount_cent FROM settlement_item_recoveries r JOIN settlement_items i ON i.id = r.item_id
                 WHERE i.bill_id = ? AND r.original_bill_id = ?
                """, Long.class, billId, originalBill)).isEqualTo(-2000L);

        com.complex.platform.support.InternalPayDayFixtures.seed(jdbcTemplate);
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            postingService.postBill(billId);
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
        assertThat(jdbcTemplate.queryForObject("""
                SELECT a.amount_cent FROM ar_ap_items a JOIN settlement_items i ON i.ar_ap_item_id = a.id
                 WHERE i.bill_id = ? AND i.distribution_method = 'exclusive_full' AND a.direction = 'payable'
                """, Long.class, billId)).isEqualTo(10000L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT a.amount_cent FROM ar_ap_items a JOIN settlement_items i ON i.ar_ap_item_id = a.id
                 WHERE i.bill_id = ? AND i.distribution_method = 'exclusive_recovery' AND a.direction = 'receivable'
                   AND a.source_type = 'ticket_distribution_recovery'
                """, Long.class, billId)).isEqualTo(2000L);
    }

    /**
     * 分池：原月（已结付）其中一笔核销次月撤销 → 按原月剔除该次核销后重算，各家差额逐家追回或补付；当月池只按当月核销分，不含该冲正。
     * 第十八批第 2 条起按单张票的池快照逐份分账：原月票 T1 覆盖 A、B（池 2000，参与 2 家行 57/43），票 T2 只覆盖 A（池 1000）。
     * 原月 A 入场 2 人排第 1：T1 A 1140 / B 860，T2 A 1000 → A 2140、B 860；撤销 A 在 T1 的入场后 A、B 当期各 1 人并列各 50%，
     * T1 里 A 未核销其 1000 按减除额处理、B 拿 1000，T2 A 1000 → A 1000、B 1000；差额 A −1140、B +140。当月 B 新票 T3（池 5000）独家拿。
     */
    @Test
    void pooledPostedMonthReversalRecomputesOriginalMonthPerBar() {
        String p = uniqueProject();
        String a = "smp_a";
        String b = "smp_b";
        com.complex.platform.support.PassPoolFixtures.snapshot(jdbcTemplate, "tk1_" + p, p, 2000L, a, b);
        com.complex.platform.support.PassPoolFixtures.snapshot(jdbcTemplate, "tk2_" + p, p, 1000L, a);
        com.complex.platform.support.PassPoolFixtures.snapshot(jdbcTemplate, "tk3_" + p, p, 5000L, b);
        Instant aug = Instant.parse("2011-08-20T10:00:00Z");
        seedSettledFact("vp_a1_" + p, "ent_a1_" + p, p, a, aug, 0L, null, "tk1_" + p);
        seedSettledFact("vp_b1_" + p, "ent_b1_" + p, p, b, aug.plusSeconds(60), 0L, null, "tk1_" + p);
        seedSettledFact("vp_a2_" + p, "ent_a2_" + p, p, a, aug.plusSeconds(120), 0L, null, "tk2_" + p);
        seedBill(p, YearMonth.from(aug.atZone(ZoneOffset.UTC)), "settled");
        YearMonth current = YearMonth.from(businessDays.businessDate(p, null, Instant.now()));
        seedSettledFact("vp_b2_" + p, "ent_b2_" + p, p, b, current.atDay(1).atTime(12, 0).toInstant(ZoneOffset.UTC),
                0L, null, "tk3_" + p);
        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                "vp_a1_" + p, "rp_" + p, "ent_a1_" + p, "order_x", a, 1));

        String billId = settlementService.settleMonth(p, current);

        assertThat(itemAmount(billId, b, "by_rank_tier")).as("当月池不含原月冲正").isEqualTo(5000L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_items WHERE bill_id = ? AND merchant_id = ? AND distribution_method = 'by_rank_tier'",
                Integer.class, billId, a)).isZero();
        assertThat(itemAmount(billId, a, "pooled_recovery")).isEqualTo(-1140L);
        assertThat(itemAmount(billId, b, "pooled_recovery")).isEqualTo(140L);
        assertThat(num(jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE id = ?", billId), "available_pool_cent"))
                .isEqualTo(5000L);
    }

    /**
     * 操作员裁决 2026-10-19 第二轮第 6 条「分池重算后的正差额：补付」（现状保持）：原月（已结付）撤销名次在前一家的一笔核销后名次互换，
     * 多得的家按正差额补付（挂通票分账应付）、少得的家按负差额追回（挂追收应收），与当月应付分别收付、互不轧差。
     * 原月：T1 覆盖 A、B（池 2000），T2、T3 只覆盖 B、T4 只覆盖 A（各池 1000）；B 入场 3 人排第 1：T1 B 1140 / A 860 → A 1860、B 3140；
     * 撤销 B 在 T3 的入场后 A、B 各 2 人并列各 50%：T1 各 1000，T3 已无入场、不再结算 → A 2000、B 2000；差额 A +140、B −1140。
     * 当月 B 新票 T5（池 5000）独家拿。
     */
    @Test
    void pooledRecomputationPaysPositiveDeltaAndRecoversNegativeSeparately() {
        String p = uniqueProject();
        String a = "smq_a";
        String b = "smq_b";
        com.complex.platform.support.PassPoolFixtures.snapshot(jdbcTemplate, "tk1_" + p, p, 2000L, a, b);
        com.complex.platform.support.PassPoolFixtures.snapshot(jdbcTemplate, "tk2_" + p, p, 1000L, b);
        com.complex.platform.support.PassPoolFixtures.snapshot(jdbcTemplate, "tk3_" + p, p, 1000L, b);
        com.complex.platform.support.PassPoolFixtures.snapshot(jdbcTemplate, "tk4_" + p, p, 1000L, a);
        com.complex.platform.support.PassPoolFixtures.snapshot(jdbcTemplate, "tk5_" + p, p, 5000L, b);
        Instant jul = Instant.parse("2011-07-20T10:00:00Z");
        seedSettledFact("vq_a1_" + p, "ent_a1_" + p, p, a, jul, 0L, null, "tk1_" + p);
        seedSettledFact("vq_b1_" + p, "ent_b1_" + p, p, b, jul.plusSeconds(60), 0L, null, "tk1_" + p);
        seedSettledFact("vq_b2_" + p, "ent_b2_" + p, p, b, jul.plusSeconds(120), 0L, null, "tk2_" + p);
        seedSettledFact("vq_b3_" + p, "ent_b3_" + p, p, b, jul.plusSeconds(180), 0L, null, "tk3_" + p);
        seedSettledFact("vq_a2_" + p, "ent_a2_" + p, p, a, jul.plusSeconds(240), 0L, null, "tk4_" + p);
        seedBill(p, YearMonth.from(jul.atZone(ZoneOffset.UTC)), "settled");
        YearMonth current = YearMonth.from(businessDays.businessDate(p, null, Instant.now()));
        seedSettledFact("vq_b4_" + p, "ent_b4_" + p, p, b, current.atDay(1).atTime(12, 0).toInstant(ZoneOffset.UTC),
                0L, null, "tk5_" + p);
        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                "vq_b3_" + p, "rq_" + p, "ent_b3_" + p, "order_x", b, 1));

        String billId = settlementService.settleMonth(p, current);

        assertThat(itemAmount(billId, a, "pooled_recovery")).as("多得的家按正差额补付").isEqualTo(140L);
        assertThat(itemAmount(billId, b, "pooled_recovery")).as("少得的家按负差额追回").isEqualTo(-1140L);
        assertThat(itemAmount(billId, b, "by_rank_tier")).as("当月应付照常").isEqualTo(5000L);

        com.complex.platform.support.InternalPayDayFixtures.seed(jdbcTemplate);
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            postingService.postBill(billId);
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
        // 收付分列：正差额为应付（ticket_distribution）、负差额为追收应收（ticket_distribution_recovery）、当月应付另一行，不轧差。
        assertThat(arApForItem(billId, a, "pooled_recovery")).containsEntry("direction", "payable")
                .containsEntry("source_type", "ticket_distribution").containsEntry("amount_cent", 140L);
        assertThat(arApForItem(billId, b, "pooled_recovery")).containsEntry("direction", "receivable")
                .containsEntry("source_type", "ticket_distribution_recovery").containsEntry("amount_cent", 1140L);
        assertThat(arApForItem(billId, b, "by_rank_tier")).containsEntry("direction", "payable")
                .containsEntry("source_type", "ticket_distribution").containsEntry("amount_cent", 5000L);
    }

    // ─── AUD-019：到期归项目排除退款/退款申请中 ─────────────────────────────────

    @Test
    void expiryForfeitsActiveUnusedButExcludesRefundedAndPendingRefund() {
        Instant past = Instant.parse("2011-06-01T00:00:00Z");
        Instant now = Instant.parse("2011-07-01T00:00:00Z");
        // (a) active 未用 → 归项目；(b) refunded → 排除；(c) 父实例 refund_pending(权益仍 active) → 排除。
        seedExpiredEntitlement("a", 2, 0, 1000L, past, "active", "active");
        seedExpiredEntitlement("b", 2, 0, 1000L, past, "refunded", "refunded");
        seedExpiredEntitlement("c", 2, 0, 1000L, past, "active", "refund_pending");

        expiryJob.expireUnverified(now);

        assertThat(forfeited("ent_a")).isEqualTo(1);
        assertThat(forfeited("ent_b")).isZero();
        assertThat(forfeited("ent_c")).isZero();
    }

    // ─── AUD-017：按核销权威事实重建缺失结算 ────────────────────────────────────

    @Test
    void rebuildBackfillsMissingSettlementFromAuthorityIdempotently() {
        Instant past = Instant.parse("2011-08-01T00:00:00Z");
        // 权威核销事实(success)存在，但其 ST-VS 行缺失(消费者提交后宕机)。
        seedExpiredEntitlement("r", 2, 1, 1000L, past, "active", "active");
        seedSuccessVerification("vr_r", "ent_r", past);

        int rebuilt = reconciliationJob.rebuildMissingSettlements(100);
        int rebuiltAgain = reconciliationJob.rebuildMissingSettlements(100); // 幂等

        assertThat(rebuilt).isGreaterThanOrEqualTo(1);
        assertThat(rebuiltAgain).isZero();
        Long sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(verified_amount_cent),0) FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, "ST-VS-vr_r");
        assertThat(sum).isEqualTo(500L); // f(1)-f(0) = round(1000×1/2) = 500
    }

    // ─── Critical(复核):重建/冲正与遗留 entitlement-keyed 累计行共存,不双计、历史冲正生效 ─────

    @Test
    void rebuildDoesNotDoubleCountEntitlementWithLegacyCumulativeRow() {
        Instant past = Instant.parse("2011-09-01T00:00:00Z");
        // 遗留权益:已有「每权益一行」累计行 f(used)=1000(verification_id NULL),且有 success 核销但无 per-verification 行。
        seedExpiredEntitlement("lg", 2, 1, 1000L, past, "active", "active");
        seedLegacyCumulativeFact("ent_lg", "mch_sm", past, 1000L, 2, 1000L);
        seedSuccessVerification("vlg", "ent_lg", past);

        int rebuilt = reconciliationJob.rebuildMissingSettlements(100);

        // 该权益被遗留行覆盖 → 重建跳过,不建 ST-VS-<verId>,聚合不翻倍(仍=1000)。
        Integer newRow = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement_transactions WHERE transaction_no = ?", Integer.class, "ST-VS-vlg");
        Long sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(verified_amount_cent),0) FROM settlement_transactions WHERE entitlement_id = ?",
                Long.class, "ent_lg");
        assertThat(newRow).isZero();
        assertThat(sum).isEqualTo(1000L); // 不翻倍
    }

    @Test
    void legacyVerificationReversalRedReversesAgainstLegacyRowNettingToZero() {
        Instant past = Instant.parse("2011-10-01T00:00:00Z");
        // 单次遗留权益:累计行 f(1)=1000,一次 success 核销 V,无 per-verification 行。
        seedExpiredEntitlement("lr", 1, 1, 1000L, past, "active", "active");
        seedLegacyCumulativeFact("ent_lr", "mch_sm", past, 1000L, 1, 1000L);
        seedSuccessVerification("vlr", "ent_lr", past);
        markVerificationReversed("vlr"); // 复现生产:发冲正事件时原核销已翻转 reversed(k 须把它算进 => status IN(success,reversed))

        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                "vlr", "revlr", "ent_lr", "order_ent_lr", "mch_sm", 1));

        // 遗留兜底:红冲单笔增量 f(1)-f(0)=1000 → ST-VR 行 -1000,该权益净额 0(遗留正向已被冲销)。
        Long vr = jdbcTemplate.queryForObject(
                "SELECT verified_amount_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, "ST-VR-revlr");
        Long net = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(verified_amount_cent),0) FROM settlement_transactions WHERE entitlement_id = ?",
                Long.class, "ent_lr");
        assertThat(vr).isEqualTo(-1000L);
        assertThat(net).isZero();
    }

    @Test
    void legacyReversalExcludesFailedFromKAndCountsReversedOriginalSoIncrementsAreExact() {
        // 复核缺口:k 反推必须只数计入 used_times 的成功核销(success/reversed),排除 failed/manual_overridden。
        // 场景 alloc=100,total=3:f(1)=33,f(2)=67。前置一笔 failed(t0),两笔成功 success1(t1)/success2(t2),遗留累计 f(2)=67。
        // 生产态:逐笔冲正前原核销已翻转 reversed。修前(k 含 failed 或漏 reversed)增量错配:
        //   含 failed → success1 得 f(2)-f(1)=34(应 33);漏 reversed → success2 得 f(0)-f(-1)=33(应 34)。
        Instant t0 = Instant.parse("2011-11-01T00:00:00Z");
        Instant t1 = Instant.parse("2011-11-01T00:10:00Z");
        Instant t2 = Instant.parse("2011-11-01T00:20:00Z");
        seedExpiredEntitlement("kf", 3, 2, 100L, t0, "active", "active");
        seedLegacyCumulativeFact("ent_kf", "mch_sm", t0, 100L, 3, 67L);
        seedVerification("vkf_fail", "ent_kf", t0, "failed");     // 前置失败核销:不进 used_times,不该抬高 k
        seedSuccessVerification("vkf_s1", "ent_kf", t1);
        seedSuccessVerification("vkf_s2", "ent_kf", t2);

        markVerificationReversed("vkf_s1");
        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                "vkf_s1", "rev_kf1", "ent_kf", "order_ent_kf", "mch_sm", 1));
        markVerificationReversed("vkf_s2");
        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                "vkf_s2", "rev_kf2", "ent_kf", "order_ent_kf", "mch_sm", 1));

        // 逐笔增量精确(k1=33,k2=34),failed 不参与、reversed 原核销被计入;全量冲销后该权益净额 0。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT verified_amount_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, "ST-VR-rev_kf1")).isEqualTo(-33L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT verified_amount_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, "ST-VR-rev_kf2")).isEqualTo(-34L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(verified_amount_cent),0) FROM settlement_transactions WHERE entitlement_id = ?",
                Long.class, "ent_kf")).isZero();
    }

    // ─── seed helpers ────────────────────────────────────────────────────────

    /** 直插一条<b>遗留</b>「每权益一行」累计核销结算行(verification_id NULL,transaction_no=ST-VS-&lt;entId&gt;,值=f(used))。 */
    private void seedLegacyCumulativeFact(String entId, String merchantId, Instant businessAt,
                                          long allocated, int total, long verifiedCumulative) {
        java.sql.Timestamp ts = java.sql.Timestamp.from(businessAt);
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type,
                    merchant_id, settlement_status, entitlement_valid_to, created_at, updated_at)
                VALUES (?, ?, 'verification_settled', ?, NULL, 'entry', 1, ?, 1, ?, ?, 0, 'merchant',
                    ?, 'draft', ?, ?, ?)
                """, "st_vs_" + entId, "ST-VS-" + entId, entId, total, allocated, verifiedCumulative,
                merchantId, ts, ts, ts);
    }


    private String uniqueProject() {
        String p = "proj_sm_" + System.nanoTime();
        projects.add(p);
        return p;
    }

    /** 直插一条不可变增量核销结算行(exclusiveMerchantId 可空)。 */
    private void seedSettledFact(String verId, String entId, String projectId, String merchantId,
                                 Instant businessAt, long verified, String exclusiveMerchantId) {
        seedSettledFact(verId, entId, projectId, merchantId, businessAt, verified, exclusiveMerchantId, null);
    }

    /** parentTicket 非空＝池快照父票的核销（第十八批第 2 条：金额取快照，核销行只决定谁核销、当期入场人数）。 */
    private void seedSettledFact(String verId, String entId, String projectId, String merchantId,
                                 Instant businessAt, long verified, String exclusiveMerchantId, String parentTicket) {
        java.sql.Timestamp ts = java.sql.Timestamp.from(businessAt);
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type,
                    merchant_id, project_id, exclusive_merchant_id, settlement_status, entitlement_valid_to,
                    created_at, updated_at, business_at, parent_ticket_id)
                VALUES (?, ?, 'verification_settled', ?, ?, 'entry', 1, 2, 1, ?, ?, 0, 'merchant',
                    ?, ?, ?, 'draft', ?, ?, ?, ?, ?)
                """, "st_vs_" + verId, "ST-VS-" + verId, entId, verId, verified, verified,
                merchantId, projectId, exclusiveMerchantId, ts, ts, ts, ts, parentTicket);
    }

    private void seedBill(String projectId, YearMonth month, String status) {
        jdbcTemplate.update("""
                INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, now(), now())
                """, "sb_" + projectId + "_" + month, projectId,
                java.sql.Date.valueOf(month.atDay(1)), java.sql.Date.valueOf(month.atEndOfMonth()), status);
    }

    /** seed 已过期权益(+父实例)，可控 entitlement 状态与实例状态。 */
    private void seedExpiredEntitlement(String suffix, int total, int used, long allocated,
                                        Instant createdAt, String entStatus, String instanceStatus) {
        // 2026-08-03:到期归属按提供方业态分流,查不到业态一律 fail-loud 阻断 → 夹具必须建真商户行。
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES ('mch_sm', 'mch_sm', 'bar', 'active', now(), now())
                ON CONFLICT (id) DO UPDATE SET merchant_type = EXCLUDED.merchant_type""");
        // FS-023-R1:到期余额按售票订单锁定的项目解析域公司,订单无项目即阻断 → 夹具订单须带项目。
        jdbcTemplate.update("""
                INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)
                VALUES ('proj_sm_exp', 'complex', 'proj_sm_exp', 'active', now(), now()) ON CONFLICT (id) DO NOTHING""");
        String ent = "ent_" + suffix;
        entIds.add(ent);
        String typeId = "tt_" + suffix;
        String templateId = "et_" + suffix;
        String orderId = "order_" + ent;
        String instId = "inst_" + ent;
        java.sql.Timestamp ct = java.sql.Timestamp.from(createdAt);
        java.sql.Timestamp validTo = java.sql.Timestamp.from(createdAt.plusSeconds(3600)); // 已过期(远早于 now)
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at) VALUES (?, 'SM', 'fixed', FALSE, 'active', ?, ?)
                ON CONFLICT (id) DO NOTHING""", typeId, ct, ct);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, bar_item_ref, created_at, updated_at)
                VALUES (?, '入园', 'entry', ?, 0, 'active', NULL, ?, ?) ON CONFLICT (id) DO NOTHING""",
                templateId, total, ct, ct);
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, customer_id, ticket_type_id, quantity, visit_date,
                    sales_channel, amount_cent, currency, pay_status, issue_status, status, created_at, updated_at, project_id)
                VALUES (?, ?, NULL, ?, ?, NULL, 'web', 0, 'CNY', 'paid', 'issued', 'created', ?, ?, 'proj_sm_exp')
                ON CONFLICT (id) DO NOTHING""", orderId, "ON-" + ent, typeId, total, ct, ct);
        jdbcTemplate.update("""
                INSERT INTO ticket_instances (id, ticket_no, order_id, ticket_type_id, seq_in_order, num_people,
                    total_times, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, 1, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING""",
                instId, "TN-" + ent, orderId, typeId, total, instanceStatus, ct, ct);
        jdbcTemplate.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id,
                    entitlement_code, entitlement_type, merchant_id, venue_id, point_id, total_times, used_times,
                    value_amount_cent, status, valid_from, valid_to, created_at, updated_at, num_people,
                    allocated_revenue_cent, parent_ticket_id)
                VALUES (?, ?, ?, ?, ?, 'entry', 'mch_sm', NULL, ?, ?, ?, 0, ?, ?, ?, ?, ?, 1, ?, ?)""",
                ent, typeId, orderId, templateId, "code_" + suffix, "point_" + suffix, total, used, entStatus,
                ct, validTo, ct, ct, allocated, instId);
    }

    private void seedSuccessVerification(String verId, String entId, Instant at) {
        seedVerification(verId, entId, at, "success");
    }

    /** 直插一条前向核销记录(reverses_verification_id NULL),status 可控(success/failed/manual_overridden…)。 */
    private void seedVerification(String verId, String entId, Instant at, String status) {
        verIds.add(verId);
        java.sql.Timestamp ts = java.sql.Timestamp.from(at);
        // ticket_verifications.device_id 有 FK → terminal_devices(id)(V159);先种一台合法已登记设备(照既有测试范式:kiosk/venue/bound)。
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    status, created_at, updated_at)
                VALUES ('dev_sm', 'dev-dev_sm', 'DC-dev_sm', 'kiosk', 'venue', 'bound', ?, ?)
                ON CONFLICT (id) DO NOTHING""", ts, ts);
        jdbcTemplate.update("""
                INSERT INTO ticket_verifications (id, verification_no, entitlement_id, entitlement_code,
                    credential_type, credential_payload, device_id, point_id, status, source_type, created_at,
                    verified_people)
                VALUES (?, ?, ?, ?, 'qr', 'x', 'dev_sm', 'pt', ?, 'admin', ?, 1)""",
                verId, "VN-" + verId, entId, "code_" + entId, status, ts);
    }

    /** 复现生产:冲正主链在发事件前已把原核销翻转成 reversed(TicketVerificationService markVerificationReversed)。 */
    private void markVerificationReversed(String verId) {
        jdbcTemplate.update("UPDATE ticket_verifications SET status = 'reversed' WHERE id = ?", verId);
    }

    private long forfeited(String entId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement_transactions WHERE entitlement_id = ? AND transaction_type='unverified_expired'",
                Integer.class, entId);
        return n == null ? 0 : n;
    }

    private long itemAmount(String billId, String merchantId, String method) {
        return jdbcTemplate.queryForObject("""
                SELECT final_amount_cent FROM settlement_items WHERE bill_id = ? AND merchant_id = ? AND distribution_method = ?
                """, Long.class, billId, merchantId, method);
    }

    /** 该明细过账回写的应收应付行（方向、来源、金额）。 */
    private java.util.Map<String, Object> arApForItem(String billId, String merchantId, String method) {
        return jdbcTemplate.queryForMap("""
                SELECT a.direction, a.source_type, a.amount_cent FROM ar_ap_items a JOIN settlement_items i ON i.ar_ap_item_id = a.id
                 WHERE i.bill_id = ? AND i.merchant_id = ? AND i.distribution_method = ?
                """, billId, merchantId, method);
    }

    private static long num(java.util.Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }

    private Instant monthStart(Instant in) {
        return YearMonth.from(in.atZone(ZoneOffset.UTC)).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant monthEnd(Instant in) {
        return YearMonth.from(in.atZone(ZoneOffset.UTC)).plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
