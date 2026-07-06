package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.complex.platform.ticketing.dto.VerificationRequest;

/**
 * 结算重构批1 端到端验收：核销成功 → ticketing 发 settlement.settled → settlement 域 AFTER_COMMIT 双写
 * settlement_transactions（verification_settled）。金额三段(verified+unverified=allocated)、幂等累积、direct 跳过。
 *
 * <p><b>非 @Transactional</b>：本消费者用 {@code @TransactionalEventListener(AFTER_COMMIT)}——仅当外层事务真正提交后才触发。
 * 若类上加 @Transactional（测试事务回滚），AFTER_COMMIT 永不触发。故本类真提交、@AfterEach 按 suffix 清理落地行。
 * 走 {@link TicketingService#createVerification}（@Transactional 生产入口）驱动，方有真提交触发 AFTER_COMMIT。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
class SettlementDoubleWriteEndToEndTests {

    @Autowired
    private TicketingService ticketingService;

    @Autowired
    private JdbcTicketingRepository ticketingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> seededCodes = new ArrayList<>();
    private String deviceCode;

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        for (String code : seededCodes) {
            jdbcTemplate.update("DELETE FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM issued_entitlements WHERE id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + code);
            jdbcTemplate.update("DELETE FROM entitlement_templates WHERE id = ?", "et_" + code);
            jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", "tt_" + code);
        }
        if (deviceCode != null) {
            jdbcTemplate.update("DELETE FROM terminal_devices WHERE id = ?", deviceCode);
        }
    }

    @Test
    void verificationDoubleWritesSettlementRowWithCorrectAmountSplit() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        // 2 次权益、总分配 10001 分：核销 1 次 → verified=round(10001*1/2)=5001，unverified=5000，和=10001。
        String code = seedMerchantEntitlement(suffix, 2, 10001L, "merchant_a");

        var response = ticketingService.createVerification(new VerificationRequest(
                "idem-" + suffix, "qr", code, deviceCode, "point_" + suffix, Map.of("source", "test")));
        assertThat(response.status()).isEqualTo("success");

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
        assertThat(row.get("transaction_type")).isEqualTo("verification_settled");
        assertThat(row.get("beneficiary_type")).isEqualTo("merchant");
        assertThat(row.get("merchant_id")).isEqualTo("merchant_a");
        assertThat(row.get("settlement_status")).isEqualTo("draft");
        assertThat(row.get("project_id")).isNull();
        assertThat(row.get("transaction_no")).isEqualTo("ST-VS-ent_" + code);
        assertThat(((Number) row.get("verified_count")).intValue()).isEqualTo(1);
        long allocated = ((Number) row.get("allocated_revenue_cent")).longValue();
        long verified = ((Number) row.get("verified_amount_cent")).longValue();
        long unverified = ((Number) row.get("unverified_amount_cent")).longValue();
        assertThat(allocated).isEqualTo(10001L);
        assertThat(verified).isEqualTo(5001L);
        assertThat(unverified).isEqualTo(5000L);
        assertThat(verified + unverified).isEqualTo(allocated);
    }

    @Test
    void repeatedVerificationsUpsertSingleRowAccumulatingVerifiedCount() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String code = seedMerchantEntitlement(suffix, 2, 10000L, "merchant_a");

        ticketingService.createVerification(new VerificationRequest(
                "idem-a-" + suffix, "qr", code, deviceCode, "point_" + suffix, Map.of()));
        ticketingService.createVerification(new VerificationRequest(
                "idem-b-" + suffix, "qr", code, deviceCode, "point_" + suffix, Map.of()));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_transactions WHERE entitlement_id = ? AND transaction_type = 'verification_settled'",
                Integer.class, "ent_" + code);
        assertThat(count).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
        // 2 次核销全用尽 → verified_count=2、verified=10000、unverified=0。
        assertThat(((Number) row.get("verified_count")).intValue()).isEqualTo(2);
        assertThat(((Number) row.get("verified_amount_cent")).longValue()).isEqualTo(10000L);
        assertThat(((Number) row.get("unverified_amount_cent")).longValue()).isEqualTo(0L);
    }

    @Test
    void directIssuedEntitlementWithNullAllocatedIsSkipped() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        // allocated=null（direct 出票无分摊快照）→ 消费者跳过不写。
        String code = seedMerchantEntitlement(suffix, 1, null, "merchant_a");

        var response = ticketingService.createVerification(new VerificationRequest(
                "idem-" + suffix, "qr", code, deviceCode, "point_" + suffix, Map.of()));
        assertThat(response.status()).isEqualTo("success");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_transactions WHERE entitlement_id = ?",
                Integer.class, "ent_" + code);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void diningEntitlementSettlesInternalPriceToRestaurantMerchant() {
        // 餐饮结算(用户2026-07-06)：内部结算价 3000/次、专属餐厅 rest_a、纯赠送(allocated=null)。
        //   核销 1 次 → verified_amount_cent = 3000×1 = 3000，全额归 rest_a(exclusive_merchant_id)，不因 allocated 缺失被跳过。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String code = seedDiningEntitlement(suffix, 2, null, "rest_a", 3000L);

        var response = ticketingService.createVerification(new VerificationRequest(
                "idem-dining-" + suffix, "qr", code, deviceCode, "point_" + suffix, Map.of()));
        assertThat(response.status()).isEqualTo("success");

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
        // 结算金额=内部结算价×核销数(不是把面值/折扣当结算价乱记)；归属=专属餐厅商户。
        assertThat(((Number) row.get("verified_amount_cent")).longValue()).isEqualTo(3000L);
        assertThat(row.get("exclusive_merchant_id")).isEqualTo("rest_a");
        assertThat(row.get("merchant_id")).isEqualTo("rest_a");
        assertThat(row.get("beneficiary_type")).isEqualTo("merchant");
        // 核销记录 settlement_price_cent 也是内部结算价(对账口径)。
        Long settlementPrice = jdbcTemplate.queryForObject(
                "SELECT settlement_price_cent FROM ticket_verifications WHERE entitlement_id = ? AND status = 'success'",
                Long.class, "ent_" + code);
        assertThat(settlementPrice).isEqualTo(3000L);
    }

    @Test
    void diningEntitlementWithZeroPriceExchangeSettlesInternalPriceNotFaceValue() {
        // 纯兑换：面值(value_amount)=0，但内部结算价=2500 → 餐厅仍按内部价 2500 结算(证「价格可为0+内部结算价」口径)。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String code = seedDiningEntitlement(suffix, 1, 0L, "rest_b", 2500L);

        var response = ticketingService.createVerification(new VerificationRequest(
                "idem-dining0-" + suffix, "qr", code, deviceCode, "point_" + suffix, Map.of()));
        assertThat(response.status()).isEqualTo("success");

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
        assertThat(((Number) row.get("verified_amount_cent")).longValue()).isEqualTo(2500L);
        assertThat(row.get("exclusive_merchant_id")).isEqualTo("rest_b");
    }

    /**
     * 餐饮权益(dining)：virtual + agreement_price_cent(内部结算价) + exclusive_merchant_id(餐厅)。
     * valueAmountCent 可为 null(纯赠送/direct 无 allocated) 或 0(纯兑换) —— 结算按内部结算价，不受其影响。
     */
    private String seedDiningEntitlement(String suffix, int totalTimes, Long allocatedCent,
                                         String restaurantMerchantId, long internalPriceCent) {
        String code = "st-dining-" + suffix;
        seededCodes.add(code);
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'Dining Type', 'fixed', FALSE, 'active', ?, ?)
                """, typeId, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, bar_item_ref, entitlement_kind, agreement_price_cent,
                    exclusive_merchant_id, created_at, updated_at)
                VALUES (?, 'Dining', 'dining', ?, 0, 'active', NULL, 'virtual', ?, ?, ?, ?)
                """, templateId, totalTimes, internalPriceCent, restaurantMerchantId, ts, ts);
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, totalTimes,
                java.time.LocalDate.parse("2026-06-18"), "web", null, 0L, "CNY", "paid", "issued", "created", now, now));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "dining",
                restaurantMerchantId, null, "point_" + suffix, totalTimes, 0, allocatedCent, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, allocatedCent,
                "virtual", internalPriceCent, null, restaurantMerchantId));
        return code;
    }

    private void seedDevice(String suffix) {
        deviceCode = "device_st_" + suffix;
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    status, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, ?)
                """, deviceCode, "dev-" + deviceCode, "DC-" + deviceCode, ts, ts);
    }

    /** 普通(非赠饮)商户权益：merchantId 有值、allocatedRevenueCent 可空(null=direct 跳过)。 */
    private String seedMerchantEntitlement(String suffix, int totalTimes, Long allocatedCent, String merchantId) {
        String code = "st-code-" + suffix;
        seededCodes.add(code);
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'Settlement Type', 'fixed', FALSE, 'active', ?, ?)
                """, typeId, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, bar_item_ref, created_at, updated_at)
                VALUES (?, 'Entry', 'entry', ?, 0, 'active', NULL, ?, ?)
                """, templateId, totalTimes, ts, ts);
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, totalTimes,
                java.time.LocalDate.parse("2026-06-18"), "web", null, 0L, "CNY", "paid", "issued", "created", now, now));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "entry",
                merchantId, null, "point_" + suffix, totalTimes, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, allocatedCent,
                null, null));
        return code;
    }
}
