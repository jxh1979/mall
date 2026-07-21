package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

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
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementStateMachineJdbcIntegrationTests {

    @Autowired private SettlementSettledConsumer settledConsumer;
    @Autowired private SettlementReversedConsumer reversedConsumer;
    @Autowired private SettlementExpiryJob expiryJob;
    @Autowired private SettlementReconciliationJob reconciliationJob;
    @Autowired private JdbcSettlementBillRepository billRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<String> projects = new ArrayList<>();
    private final List<String> entIds = new ArrayList<>();
    private final List<String> verIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String v : verIds) {
            jdbcTemplate.update("DELETE FROM settlement_transactions WHERE transaction_no IN (?, ?)",
                    "ST-VS-" + v, "ST-VR-" + v);
            jdbcTemplate.update("DELETE FROM ticket_verifications WHERE id = ?", v);
        }
        for (String e : entIds) {
            jdbcTemplate.update("DELETE FROM settlement_transactions WHERE entitlement_id = ?", e);
            jdbcTemplate.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", e);
            jdbcTemplate.update("DELETE FROM ticket_instances WHERE id = ?", "inst_" + e);
            jdbcTemplate.update("DELETE FROM issued_entitlements WHERE id = ?", e);
            jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + e);
        }
        for (String p : projects) {
            jdbcTemplate.update("DELETE FROM settlement_transactions WHERE project_id = ?", p);
            jdbcTemplate.update("DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)", p);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", p);
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

        assertThat(billRepository.sumVerifiedRevenueCent(p, monthStart(jan), monthEnd(jan))).isEqualTo(1000L);
        assertThat(billRepository.sumVerifiedRevenueCent(p, monthStart(feb), monthEnd(feb))).isEqualTo(1000L);
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
                t.plusSeconds(3600), null, null);
        settledConsumer.onSettlementSettled(e1);
        settledConsumer.onSettlementSettled(e1); // 重放：ON CONFLICT DO NOTHING
        // 第 2 次核销(k=2)：增量 f(2)-f(1)=500，独立成行。
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                "vc_" + p + "_2", t, ent, "order_x", "tt_x", "entry", "mch_" + p, 2, 2, 1, 1000L,
                t.plusSeconds(3600), null, null));

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
        assertThat(billRepository.sumVerifiedRevenueCent(p, monthStart(jan), monthEnd(jan))).isZero();
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
        assertThat(billRepository.sumVerifiedRevenueCent(p, monthStart(jan), monthEnd(jan))).isEqualTo(1000L);
        // 负向红冲落当月(now)，不在 Jan 窗内。
        Instant vrBusiness = jdbcTemplate.queryForObject(
                "SELECT business_at FROM settlement_transactions WHERE transaction_no = ?",
                Instant.class, "ST-VR-rev_" + p);
        assertThat(vrBusiness).isAfter(monthEnd(jan));
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

    // ─── seed helpers ────────────────────────────────────────────────────────

    private String uniqueProject() {
        String p = "proj_sm_" + System.nanoTime();
        projects.add(p);
        return p;
    }

    /** 直插一条不可变增量核销结算行(exclusiveMerchantId 可空)。 */
    private void seedSettledFact(String verId, String entId, String projectId, String merchantId,
                                 Instant businessAt, long verified, String exclusiveMerchantId) {
        java.sql.Timestamp ts = java.sql.Timestamp.from(businessAt);
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type,
                    merchant_id, project_id, exclusive_merchant_id, settlement_status, entitlement_valid_to,
                    created_at, updated_at, business_at)
                VALUES (?, ?, 'verification_settled', ?, ?, 'entry', 1, 2, 1, ?, ?, 0, 'merchant',
                    ?, ?, ?, 'draft', ?, ?, ?, ?)
                """, "st_vs_" + verId, "ST-VS-" + verId, entId, verId, verified, verified,
                merchantId, projectId, exclusiveMerchantId, ts, ts, ts, ts);
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
                VALUES (?, 'E', 'entry', ?, 0, 'active', NULL, ?, ?) ON CONFLICT (id) DO NOTHING""",
                templateId, total, ct, ct);
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, customer_id, ticket_type_id, quantity, visit_date,
                    sales_channel, amount_cent, currency, pay_status, issue_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, ?, NULL, 'web', 0, 'CNY', 'paid', 'issued', 'created', ?, ?)
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
        verIds.add(verId);
        java.sql.Timestamp ts = java.sql.Timestamp.from(at);
        jdbcTemplate.update("""
                INSERT INTO ticket_verifications (id, verification_no, entitlement_id, entitlement_code,
                    credential_type, credential_payload, device_id, point_id, status, source_type, created_at,
                    verified_people)
                VALUES (?, ?, ?, ?, 'qr', 'x', 'dev', 'pt', 'success', 'admin', ?, 1)""",
                verId, "VN-" + verId, entId, "code_" + entId, ts);
    }

    private long forfeited(String entId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement_transactions WHERE entitlement_id = ? AND transaction_type='unverified_expired'",
                Integer.class, entId);
        return n == null ? 0 : n;
    }

    private Instant monthStart(Instant in) {
        return YearMonth.from(in.atZone(ZoneOffset.UTC)).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant monthEnd(Instant in) {
        return YearMonth.from(in.atZone(ZoneOffset.UTC)).plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
