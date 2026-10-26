package com.complex.platform.integration;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.settlement.JdbcSettlementBillRepository;
import com.complex.platform.ticketing.IssuedEntitlementRecord;
import com.complex.platform.ticketing.TicketTypeEntitlementRecord;
import com.complex.platform.ticketing.TicketTypeRecord;
import com.complex.platform.ticketing.TicketingRepository;
import com.complex.platform.ticketing.TicketingService;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 批6 第三方通票适配端到端（真 PostgreSQL + Spring 真实装配）：
 * 模拟渠道回流订单 → 本地票生成（走既有出票主链）→ 权益核销 → 核销回传入队 →
 * 渠道收入落 meituan 渠道桶（S11 渠道费计费基数）→ channel-auto 自动对账 matched。
 * 另证：开关关拒收（渠道未启用）、映射缺失拒收、同渠道订单号重复回流只出一次票。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class ThirdPartyPassTicketEndToEndJdbcTests {

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private TicketingService ticketingService;

    @Autowired
    private TicketingRepository ticketingRepository;

    @Autowired
    private JdbcSettlementBillRepository settlementBillRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper json;

    private String suffix;
    private String provider;
    private String ticketTypeId;
    private String deviceId;
    private String projectId;
    private String merchantId;
    private String sellerEntityId;

    @BeforeEach
    void setUp() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        suffix = Long.toString(System.nanoTime());
        provider = "tp-e2e-" + suffix;
        ticketTypeId = "tt_tp_" + suffix;
        deviceId = "device_tp_" + suffix;
        projectId = "project_tp_" + suffix;
        merchantId = "merchant_tp_" + suffix;
        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','渠道票测试综合体','active',now(),now())", projectId);
        jdbcTemplate.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'渠道票履约商户','amusement','active',?,now(),now())", merchantId, projectId);
        // V071 种子默认关（tcs_int_ticket_chan_off）→ 本测试打开；@AfterEach 复位为关。
        jdbcTemplate.update(
                "UPDATE terminal_capability_switches SET enabled = TRUE WHERE capability_code = 'integration.ticket_channel.sync'");
        Instant now = Instant.now();
        // 通票票种（平台售、published、有效 2 天、门市价 8800 分）。
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                ticketTypeId, "第三方回流测试通票", "fixed", 8800L, 8800L, 8800L,
                false, "published", 2, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, merchant_id, created_at, updated_at)
                VALUES (?, '入场', 'entry', 1, 0, 'active', ?, now(), now())
                """, "et_tp_" + suffix, merchantId);
        // S11：分项票面原价必配（componentPriceCent=8800），否则出票按业态拆分账拒绝。
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "tte_tp_" + suffix, ticketTypeId, "et_tp_" + suffix, 1, 1, 8800L, null, 1, 8800L, now, now));
        sellerEntityId = com.complex.platform.support.TicketSalesContractTestFixtures.approvedPublished(
                jdbcTemplate, json, ticketTypeId, projectId, merchantId);
        // 核销设备（场馆级 kiosk）。
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    status, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', now(), now())
                """, deviceId, "dev-" + deviceId, "DC-" + deviceId);
        // 渠道费率：meituan 专属 10%（S11 分账时按 ticket_orders.sales_channel 命中本行）。
        // 先清历史 meituan 行（自然键唯一，前次异常残留会让 ON CONFLICT DO NOTHING 静默跳插）。
        jdbcTemplate.update("DELETE FROM channel_fee_configs WHERE channel = 'meituan' AND project_id IS NULL");
        jdbcTemplate.update("""
                INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, is_draft)
                VALUES (?, NULL, 'meituan', 1000, FALSE)
                """, "cfc_tp_" + suffix);
        // 供应商适配器（webhook 开）→ channel-auto 对账非 dryRun 落库。
        jdbcTemplate.update("""
                INSERT INTO integration_provider_adapters (id, provider_code, adapter_type, mode, capabilities,
                    environment, status, webhook_enabled, polling_check_enabled, field_mapping_json,
                    webhook_config_json, polling_config_json, production_ready, created_at, updated_at)
                VALUES (?, ?, 'ticket_channel', 'mock', '["webhook"]'::jsonb, 'mock', 'pending',
                    TRUE, FALSE, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb, FALSE, now(), now())
                """, "adapter_tp_" + suffix, provider);
        // 渠道票种映射：美团团购商品 → 本地通票（门市价出票）。
        integrationService.saveChannelTicketTypeMap(new IntegrationChannelTicketTypeMapRequest(
                "map-tp-" + suffix, provider, "meituan", "mt-prod-" + suffix, ticketTypeId, null));
    }

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        // 复位开关为出厂默认（关）。
        jdbcTemplate.update(
                "UPDATE terminal_capability_switches SET enabled = FALSE WHERE capability_code = 'integration.ticket_channel.sync'");
        // 本例须真提交以验证 AFTER_COMMIT 回传。保留售票、核销、结算、合同及其来源事实；
        // 各例以唯一 provider/项目/票种隔离，只清可变待发队列和费率配置，不禁用账本触发器。
        jdbcTemplate.update("DELETE FROM integration_retry_tasks WHERE provider_code = ?", provider);
        jdbcTemplate.update("UPDATE integration_provider_adapters SET webhook_enabled=FALSE, polling_check_enabled=FALSE WHERE provider_code=?", provider);
        jdbcTemplate.update("DELETE FROM channel_fee_configs WHERE id = ?", "cfc_tp_" + suffix);
    }

    private IntegrationOrderSyncRequest reflowRequest(String idempotencyKey, String externalBusinessId) {
        return new IntegrationOrderSyncRequest(
                idempotencyKey, "e2e", List.of(), provider, "ticket", "order",
                externalBusinessId, "received", 8800L, "CNY", "meituan", "mtord-" + externalBusinessId,
                null, Map.of("external_product_id", "mt-prod-" + suffix, "quantity", 1));
    }

    @Test
    void channelReflowIssuesVerifiesEnqueuesCallbackAndReconciles() {
        String externalId = "ext-tp-" + suffix;

        // ① 渠道回流 → 本地出票（既有出票主链）。
        IntegrationOrderSyncRecord sync = integrationService.createOrderSync(reflowRequest("tp-e2e-1-" + suffix, externalId));
        assertThat(sync.status()).isEqualTo("synced");
        String localOrderId = sync.providerPayload().get("local_ticket_order_id").toString();

        // 本地订单：sales_channel=meituan（S11 渠道费按此解析）、已支付（paid_at 非空）、已出票。
        Map<String, Object> order = jdbcTemplate.queryForMap("SELECT * FROM ticket_orders WHERE id = ?", localOrderId);
        assertThat(order.get("sales_channel")).isEqualTo("meituan");
        assertThat(order.get("pay_status")).isEqualTo("paid");
        assertThat(order.get("issue_status")).isEqualTo("issued");
        assertThat(order.get("paid_at")).isNotNull();
        assertThat(order.get("project_id")).isEqualTo(projectId);
        assertThat(jdbcTemplate.queryForObject("SELECT seller_entity_id FROM ticket_order_sales_contracts WHERE order_id=?",
                String.class, localOrderId)).isEqualTo(sellerEntityId);
        assertThat(((Number) order.get("amount_cent")).longValue()).isEqualTo(8800L);
        assertThat(sync.providerPayload()).containsEntry("amount_matched", true);

        // 权益已发放（票码防伪/父票实例走既有链路）。
        List<IssuedEntitlementRecord> entitlements = ticketingRepository.listIssuedEntitlementsByOrderId(localOrderId);
        assertThat(entitlements).hasSize(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_instances WHERE order_id = ?", Long.class, localOrderId)).isEqualTo(1L);

        // ② 核销成功。
        var verification = ticketingService.createVerification(new VerificationRequest(
                "tp-verify-" + suffix, "qr", entitlements.get(0).entitlementCode(), deviceId,
                "point_tp_" + suffix, Map.of("source", "e2e")));
        assertThat(verification.status()).isEqualTo("success");

        // ③ 核销回传出站队列：AFTER_COMMIT 落 integration_retry_tasks（真实适配器未接入，队列待发）。
        Map<String, Object> task = jdbcTemplate.queryForMap(
                "SELECT * FROM integration_retry_tasks WHERE id = ?", "retry_vcb_" + verification.id());
        assertThat(task.get("task_type")).isEqualTo("verification_callback");
        assertThat(task.get("business_type")).isEqualTo("ticket");
        assertThat(task.get("external_business_id")).isEqualTo(externalId);
        assertThat(task.get("provider_code")).isEqualTo(provider);

        // ④ 渠道收入落 meituan 渠道桶（S11 渠道费计费基数=按 sales_channel 汇总的当期实付额），
        //    渠道费率配置可命中（meituan 专属 10%），分账时 fee = 桶收入 × 费率（V381 已测算法）。
        Instant monthStart = Instant.now().truncatedTo(ChronoUnit.DAYS).minus(2, ChronoUnit.DAYS);
        Instant monthEnd = Instant.now().plus(2, ChronoUnit.DAYS);
        var meituanBucket = settlementBillRepository.aggregateChannelRevenue(null, monthStart, monthEnd).stream()
                .filter(row -> "meituan".equals(row.salesChannel()))
                .findFirst();
        assertThat(meituanBucket).isPresent();
        assertThat(meituanBucket.get().revenueCent()).isGreaterThanOrEqualTo(8800L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT fee_bps FROM channel_fee_configs WHERE channel = 'meituan' AND id = ?",
                Integer.class, "cfc_tp_" + suffix)).isEqualTo(1000);

        // ⑤ 重复回流（换 idempotency_key）→ 只出一次票。
        IntegrationOrderSyncRecord replay = integrationService.createOrderSync(reflowRequest("tp-e2e-2-" + suffix, externalId));
        assertThat(replay.id()).isEqualTo(sync.id());
        assertThat(replay.providerPayload().get("local_ticket_order_id")).isEqualTo(localOrderId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_orders WHERE id = ?", Long.class, localOrderId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM integration_order_syncs WHERE provider_code = ?", Long.class, provider)).isEqualTo(1L);

        // ⑥ channel-auto 自动对账（平台侧期望=本地订单实收，系统取数）→ 对得上，零差异。
        ChannelReconciliationResult reconciliation = integrationService.reconcileChannel(new ChannelReconciliationRequest(
                "tp-recon-" + suffix, provider, "ticket", "meituan",
                LocalDate.now().minusDays(1), LocalDate.now(), false, null));
        assertThat(reconciliation.dryRun()).isFalse();
        assertThat(reconciliation.expectedCount()).isEqualTo(1);
        assertThat(reconciliation.actualCount()).isEqualTo(1);
        assertThat(reconciliation.differenceCount()).isZero();
    }

    @Test
    void reflowRejectedWhenSwitchOffAndWhenMappingMissing() {
        // 开关关（出厂默认）→ 拒收：渠道未启用，零落库、零出票。
        jdbcTemplate.update(
                "UPDATE terminal_capability_switches SET enabled = FALSE WHERE capability_code = 'integration.ticket_channel.sync'");
        assertThatThrownBy(() -> integrationService.createOrderSync(reflowRequest("tp-off-" + suffix, "ext-off-" + suffix)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("INTEGRATION_TICKET_CHANNEL_DISABLED");
                    assertThat(exception.getMessage()).contains("渠道未启用");
                });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM integration_order_syncs WHERE provider_code = ?", Long.class, provider)).isZero();

        // 开关开但映射缺失 → fail-closed 中文拒收。
        jdbcTemplate.update(
                "UPDATE terminal_capability_switches SET enabled = TRUE WHERE capability_code = 'integration.ticket_channel.sync'");
        IntegrationOrderSyncRequest unmapped = new IntegrationOrderSyncRequest(
                "tp-nomap-" + suffix, "e2e", List.of(), provider, "ticket", "order",
                "ext-nomap-" + suffix, "received", 8800L, "CNY", "meituan", "mtord-nomap-" + suffix,
                null, Map.of("external_product_id", "mt-prod-unknown-" + suffix));
        assertThatThrownBy(() -> integrationService.createOrderSync(unmapped))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("INTEGRATION_CHANNEL_TICKET_MAP_MISSING");
                    assertThat(exception.getMessage()).contains("渠道票种映射缺失");
                });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM integration_order_syncs WHERE provider_code = ?", Long.class, provider)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_orders WHERE ticket_type_id = ?", Long.class, ticketTypeId)).isZero();
    }
}
