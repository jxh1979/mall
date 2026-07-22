package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * AUD-011 复核修正的真 PostgreSQL 集成测试（此前自动退款链只有内存仓覆盖，varchar 列宽逃逸正是因此）：
 * ① 生产形态长 id（订单28+字符/实例36字符）下自动退款单 id 恒 ≤64、真 INSERT 落库；
 * ② 人工退款审批中（父实例 refund_pending、子权益仍 active）的到期票不作废不重退，留给退款链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TicketExpiryAutoRefundJdbcIntegrationTests {

    @Autowired private TicketingService ticketingService;
    @Autowired private JdbcTicketingRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final Instant now = Instant.now();
    private final Instant past = now.minusSeconds(86_400);

    @BeforeEach
    void setUpActor() {
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void clearActor() {
        TestSecurityActors.clear();
    }

    @Test
    void expiryAutoRefundPersistsWithBoundedIdAndSkipsRefundPendingInstances() {
        String hex = UUID.randomUUID().toString().replace("-", "");
        String typeId = "tt_exp_" + hex.substring(0, 12);
        // 生产形态长 id：订单 28+ 字符、实例 "tkt_"+32hex=36 字符——修复前拼接后必超 id varchar(64)。
        String orderId = "ticket_order_tk_" + hex.substring(0, 12);
        String instActive = "tkt_" + UUID.randomUUID().toString().replace("-", "");
        String instPending = "tkt_" + UUID.randomUUID().toString().replace("-", "");

        jdbcTemplate.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, refundable, created_at, updated_at) "
                + "VALUES (?, '到期自动退测试票', 'fixed', 'published', true, now(), now())", typeId);
        jdbcTemplate.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, created_at, updated_at) "
                + "VALUES (?, '到期测试权益', 'ticket', 1, 100, 'active', now(), now()) ON CONFLICT (id) DO NOTHING", "et_exp");
        jdbcTemplate.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, "
                + "pay_status, issue_status, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, 2, 'web', 19800, 'paid', 'issued', 'created', now(), now())",
                orderId, "ONO_" + hex.substring(0, 10), typeId);

        repository.saveTicketInstance(new TicketInstanceRecord(
                instActive, "TNO_A_" + hex.substring(0, 8), orderId, typeId, 1, 1, 1,
                "c1", "c1", "active", null, null, now, now));
        repository.saveTicketInstance(new TicketInstanceRecord(
                instPending, "TNO_P_" + hex.substring(0, 8), orderId, typeId, 2, 1, 1,
                "c1", "c1", "refund_pending", null, null, now, now));

        String entActive = "ent_" + UUID.randomUUID().toString().replace("-", "");
        String entPending = "ent_" + UUID.randomUUID().toString().replace("-", "");
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                entActive, "code_" + entActive, typeId, orderId, "et_exp", "ticket", "m1", null, null,
                1, 0, 9900L, "active", past.minusSeconds(60), past, now, now,
                1, 9900L, null, null, null, null, instActive, 1));
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                entPending, "code_" + entPending, typeId, orderId, "et_exp", "ticket", "m1", null, null,
                1, 0, 9900L, "active", past.minusSeconds(60), past, now, now,
                1, 9900L, null, null, null, null, instPending, 1));

        int processed = ticketingService.processExpiredPaidEntitlements(now);

        // ① 完全未核销的 active 实例整张自动退：退款单真落库且 id ≤64（修复前此 INSERT 必「value too long」）。
        List<String> refundIds = jdbcTemplate.queryForList(
                "SELECT id FROM ticket_refunds WHERE target_id = ? AND status = 'success'", String.class, orderId);
        assertThat(refundIds).hasSize(1);
        assertThat(refundIds.getFirst()).startsWith("ticket_refund_auto_expiry_");
        assertThat(refundIds.getFirst().length()).isLessThanOrEqualTo(64);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT refund_amount_cent FROM ticket_refunds WHERE id = ?", Long.class, refundIds.getFirst()))
                .isEqualTo(9900L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM issued_entitlements WHERE id = ?", String.class, entActive))
                .isEqualTo("refunded");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM ticket_instances WHERE id = ?", String.class, instActive))
                .isEqualTo("refunded");

        // ② 人工退款审批中的实例：权益不作废、不重退，实例状态不变——留给退款链批准/驳回后处置。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM issued_entitlements WHERE id = ?", String.class, entPending))
                .isEqualTo("active");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM ticket_instances WHERE id = ?", String.class, instPending))
                .isEqualTo("refund_pending");
        // processExpiredPaidEntitlements 是【全库】过期归属 Job,processed 计当次处理的所有过期权益数,
        // 非本测试独占——共享测试库里其它用例遗留的已提交过期权益会一并被处理致 processed>1(隔离 flaky)。
        // 本测试的行为正确性由上面「entActive→refunded / instActive→refunded / entPending 仍 active / instPending 仍
        // refund_pending」四条独占断言精确覆盖;此处只需确认本测试的 active 权益确被处理(≥1),不假设全库仅此一条。
        assertThat(processed).isGreaterThanOrEqualTo(1);
    }
}
