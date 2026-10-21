package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.ticketing.TicketRewardFacts;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * TK-05-R4 退款成功后的结算冲回（BSM:804「已完成分账的票发生退款时，按与结算相反的顺序逐段冲回：先冲回域内实际提供方已收份额，再冲回域间份额，
 * 最后处理第三方渠道费用……所退票已计收的门票综合服务费随票全额退回……每一段冲回都生成反向结算记录，不覆盖原归属、原计算与原结算记录」）。
 * 通票分账在每次核销时形成不可变核销结算事实（ST-VS），月结按事实逐段拆提供方份额、业态份额与渠道费；退款成功后对所退人份每次成功核销追加一条
 * 严格取反的冲回事实（原行不改），月结按同一链路逐段冲回；原月已结付的落当月，由既有「已结付月撤销核销单列追收」口径追收。
 * 非事务（消费者 REQUIRES_NEW 要读已提交数据），@AfterEach 清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class TicketRefundSettlementReversalJdbcIntegrationTests {

    @Autowired private SettlementReversedConsumer consumer;
    @Autowired private JdbcSettlementBillRepository billRepository;
    @Autowired private JdbcTemplate jdbc;

    private String key;
    private String project;
    private String order;
    private String type;
    private final Instant march = Instant.parse("2012-03-15T10:00:00Z");

    @BeforeEach
    void seed() {
        key = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        project = "proj_rr_" + key;
        order = "o_rr_" + key;
        type = "tt_rr_" + key;
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at) VALUES (?, '冲回通票', 'fixed', FALSE, 'active', now(), now())", type);
        jdbc.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,
                    created_at, updated_at) VALUES (?, '入场', 'entry', 1, 0, 'active', now(), now())""", "et_rr_" + key);
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency, pay_status,
                    issue_status, status, created_at, updated_at, project_id)
                VALUES (?, ?, ?, 1, 'web', 10000, 'CNY', 'paid', 'issued', 'created', now(), now(), NULL)""", order, order, type);
        jdbc.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, created_at, updated_at)
                VALUES ('dev_rr', 'dev-dev_rr', 'DC-dev_rr', 'kiosk', 'venue', 'bound', now(), now()) ON CONFLICT (id) DO NOTHING""");
    }

    @AfterEach
    void cleanup() {
        AppendOnlyTestBypass.update(jdbc, "DELETE FROM settlement_transactions WHERE order_id = ?", order);
        jdbc.update("DELETE FROM settlement_bills WHERE project_id = ?", project);
        AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_refund_lines", "trg_append_only_d",
                "DELETE FROM ticket_refund_lines WHERE order_id = ?", order);
        AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_refunds", "trg_append_only_d",
                "DELETE FROM ticket_refunds WHERE target_id = ?", order);
        jdbc.update("DELETE FROM ticket_verifications WHERE entitlement_id IN (SELECT id FROM issued_entitlements WHERE order_id = ?)", order);
        jdbc.update("DELETE FROM issued_entitlements WHERE order_id = ?", order);
        jdbc.update("DELETE FROM ticket_orders WHERE id = ?", order);
        jdbc.update("DELETE FROM ticket_types WHERE id = ?", type);
        jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", "et_rr_" + key);
    }

    /** 安全例外退款成功：所退人份已形成分账的每次核销各追加一条取反冲回事实（含随票全额退回的门票综合服务费）；重放幂等；未过账月净额归零。 */
    @Test
    void safetyRefundAppendsReverseFactsForEachSettledUse() {
        String entry = entitlement("e", 1);
        String drink = entitlement("d", 0);
        String verification = verification(entry);
        settledFact(verification, entry, 6000L, 500L);
        String refund = refund("success", List.of(entry, drink));

        consumer.onTicketRefunded(new TicketRewardFacts.Refunded(refund));
        consumer.onTicketRefunded(new TicketRewardFacts.Refunded(refund));

        List<Map<String, Object>> reversed = jdbc.queryForList("""
                SELECT transaction_type, verification_id, verified_amount_cent, comp_service_fee_cent, num_people, merchant_id
                  FROM settlement_transactions WHERE order_id = ? AND transaction_no LIKE 'ST-RR-%'""", order);
        assertThat(reversed).hasSize(1);
        assertThat(reversed.getFirst()).containsEntry("transaction_type", "verification_reversed")
                .containsEntry("verification_id", verification).containsEntry("verified_amount_cent", -6000L)
                .containsEntry("comp_service_fee_cent", -500L).containsEntry("num_people", -1)
                .containsEntry("merchant_id", "mch_rr_" + key);
        assertThat(billRepository.sumPooledTotals(project, monthStart(march), monthEnd(march)).revenueCent()).isZero();
    }

    /** 普通退款只发生在完全未核销的票上：没有已形成分账的核销，不生成冲回事实；未成功的退款也不冲回。 */
    @Test
    void ordinaryUnusedRefundCreatesNoSettlementReversal() {
        String unused = entitlement("u", 0);
        String used = entitlement("x", 1);
        settledFact(verification(used), used, 6000L, 0L);
        consumer.onTicketRefunded(new TicketRewardFacts.Refunded(refund("success", List.of(unused))));
        consumer.onTicketRefunded(new TicketRewardFacts.Refunded(refund("approved", List.of(used))));

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM settlement_transactions WHERE order_id = ? AND transaction_no LIKE 'ST-RR-%'", Integer.class, order))
                .isZero();
    }

    /** 原核销所属月已结付：冲回事实落当月，由当月单列追收（原月账单与已付款不回改）。 */
    @Test
    void settledMonthSafetyRefundLandsInCurrentMonthForRecovery() {
        String entry = entitlement("e", 1);
        String verification = verification(entry);
        settledFact(verification, entry, 6000L, 0L);
        YearMonth month = YearMonth.from(march.atZone(ZoneOffset.UTC));
        jdbc.update("""
                INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'settled', now(), now())""", "sb_rr_" + key, project,
                java.sql.Date.valueOf(month.atDay(1)), java.sql.Date.valueOf(month.atEndOfMonth()));

        consumer.onTicketRefunded(new TicketRewardFacts.Refunded(refund("success", List.of(entry))));

        Instant businessAt = jdbc.queryForObject(
                "SELECT business_at FROM settlement_transactions WHERE order_id = ? AND transaction_no LIKE 'ST-RR-%'", Instant.class, order);
        assertThat(businessAt).isAfter(monthEnd(march));
        assertThat(billRepository.sumPooledTotals(project, monthStart(march), monthEnd(march)).revenueCent()).isEqualTo(6000L);
        assertThat(billRepository.findRecoveredOriginalMonths(project, monthStart(businessAt), monthEnd(businessAt)))
                .containsExactly(month.atDay(1));
    }

    // ---------- 造数 ----------

    private String entitlement(String tag, int used) {
        String id = "ent_rr_" + tag + "_" + key;
        jdbc.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code, entitlement_type,
                    total_times, used_times, value_amount_cent, status, valid_from, valid_to, created_at, updated_at, num_people,
                    allocated_revenue_cent)
                VALUES (?, ?, ?, ?, ?, 'entry', 1, ?, 0, 'active', now(), now() + interval '1 day', now(), now(), 1, 6000)""",
                id, type, order, "et_rr_" + key, "C-" + id, used);
        return id;
    }

    private String verification(String entitlementId) {
        String id = "v_rr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("""
                INSERT INTO ticket_verifications (id, verification_no, entitlement_id, entitlement_code, credential_type,
                    credential_payload, device_id, point_id, status, source_type, created_at, verified_people)
                VALUES (?, ?, ?, ?, 'qr', 'x', 'dev_rr', 'pt', 'success', 'admin', ?, 1)""",
                id, "VN-" + id, entitlementId, "C-" + entitlementId, Timestamp.from(march));
        return id;
    }

    private void settledFact(String verificationId, String entitlementId, long verified, long compFee) {
        Timestamp ts = Timestamp.from(march);
        jdbc.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id, order_id, ticket_type_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type,
                    merchant_id, project_id, settlement_status, entitlement_valid_to, created_at, updated_at, business_at,
                    comp_service_fee_cent)
                VALUES (?, ?, 'verification_settled', ?, ?, ?, ?, 'entry', 1, 1, 1, ?, ?, 0, 'merchant',
                    ?, ?, 'draft', ?, ?, ?, ?, ?)""",
                "st_vs_" + verificationId, "ST-VS-" + verificationId, entitlementId, order, type, verificationId,
                verified, verified, "mch_rr_" + key, project, ts, ts, ts, ts, compFee);
    }

    private String refund(String status, List<String> entitlementIds) {
        String id = "ticket_refund_rr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("""
                INSERT INTO ticket_refunds (id, target_id, refund_amount_cent, reason, refund_channel, status, requested_by,
                    created_at, updated_at, reason_code)
                VALUES (?, ?, 10000, '安全例外', 'original', ?, 'tester', now(), now(), 'safety_exception')""", id, order, status);
        int i = 0;
        for (String entitlementId : entitlementIds) {
            jdbc.update("""
                    INSERT INTO ticket_refund_lines (id, refund_id, order_id, entitlement_id, entitlement_type, line_type,
                        refund_amount_cent, remaining_times_before, used_times_before, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'entry', 'entitlement', 1, 0, 0, 'approved', now(), now())""",
                    id + "_l" + i++, id, order, entitlementId);
        }
        return id;
    }

    private static Instant monthStart(Instant at) {
        return YearMonth.from(at.atZone(ZoneOffset.UTC)).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static Instant monthEnd(Instant at) {
        LocalDate next = YearMonth.from(at.atZone(ZoneOffset.UTC)).plusMonths(1).atDay(1);
        return next.atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
