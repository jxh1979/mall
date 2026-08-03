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
 * 门票到期处理的真 PostgreSQL 集成测试。业务权威口径：{@code business-enum-quick-reference-v0.1.html}
 * 「售票与退款原则」——门票售出后原则上不退款，<b>未到场、忘记使用或主动放弃都不是退款理由</b>；
 * 「到期未核销款」——到期未核销的票先且只扣一次第三方渠道费用、余额按票种经营域归属。业务口径从未授权「到期自动退」。
 * <ul>
 *   <li>① 即便票种 refundable=true（=客人可申请整票退款），到期完全未核销的票也只作废（forfeited），
 *       <b>不产生任何退款单</b>；</li>
 *   <li>② 人工退款审批中（父实例 refund_pending、子权益仍 active）的到期票不作废，留给退款链处置。</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TicketExpiryForfeitJdbcIntegrationTests {

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
    void expiredTicketsAreForfeitedWithoutRefundAndSkipRefundPendingInstances() {
        String hex = UUID.randomUUID().toString().replace("-", "");
        String typeId = "tt_exp_" + hex.substring(0, 12);
        // 生产形态长 id：订单 28+ 字符、实例 "tkt_"+32hex=36 字符——修复前拼接后必超 id varchar(64)。
        String orderId = "ticket_order_tk_" + hex.substring(0, 12);
        String instActive = "tkt_" + UUID.randomUUID().toString().replace("-", "");
        String instPending = "tkt_" + UUID.randomUUID().toString().replace("-", "");

        // refundable=true 故意保留：证明「可申请退款的票种」到期同样只作废、不自动退。
        jdbcTemplate.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, refundable, created_at, updated_at) "
                + "VALUES (?, '到期作废测试票', 'fixed', 'published', true, now(), now())", typeId);
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
                1, 9900L, null, null, null, null, instActive, 1, null, null));
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                entPending, "code_" + entPending, typeId, orderId, "et_exp", "ticket", "m1", null, null,
                1, 0, 9900L, "active", past.minusSeconds(60), past, now, now,
                1, 9900L, null, null, null, null, instPending, 1, null, null));

        int processed = ticketingService.processExpiredPaidEntitlements(now);

        // ① 到期完全未核销的 active 实例：一律作废，绝不产生退款单（票种 refundable=true 也一样）。
        List<String> refundIds = jdbcTemplate.queryForList(
                "SELECT id FROM ticket_refunds WHERE target_id = ?", String.class, orderId);
        assertThat(refundIds).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM issued_entitlements WHERE id = ?", String.class, entActive))
                .isEqualTo("forfeited");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM ticket_instances WHERE id = ?", String.class, instActive))
                .isEqualTo("active");

        // ② 人工退款审批中的实例：权益不作废、不重退，实例状态不变——留给退款链批准/驳回后处置。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM issued_entitlements WHERE id = ?", String.class, entPending))
                .isEqualTo("active");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM ticket_instances WHERE id = ?", String.class, instPending))
                .isEqualTo("refund_pending");
        // processExpiredPaidEntitlements 是【全库】过期归属 Job,processed 计当次处理的所有过期权益数,
        // 非本测试独占——共享测试库里其它用例遗留的已提交过期权益会一并被处理致 processed>1(隔离 flaky)。
        // 本测试的行为正确性由上面「无退款单 / entActive→forfeited / entPending 仍 active / instPending 仍
        // refund_pending」四条独占断言精确覆盖;此处只需确认本测试的 active 权益确被处理(≥1),不假设全库仅此一条。
        assertThat(processed).isGreaterThanOrEqualTo(1);
    }
}
