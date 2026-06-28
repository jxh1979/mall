package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.bar.BarGiftRedemptionRecord;
import com.complex.platform.bar.BarOrderRecord;
import com.complex.platform.bar.BarRepository;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * R5 T15 端到端验收（事件驱动，真 DB 全 Spring 上下文）：核销赠饮权益(gift_drink) → ApplicationEventPublisher
 * 发 ticket.gift_verified → BarGiftConsumer 同步监听 → 0 元 bar_order(order_type='gift') + 出品 deliver 事件 +
 * bar_gift_redemptions 桥表落地。修 R5「无端到端验收」缺口。
 *
 * <p>生产落地形态：当前赠饮→吧台 SKU 无映射来源(itemRef=null)，故实际库存扣减跳过、inventory_movement_id 留空、
 * redemption.note 标注未配置 SKU；链路（核销→0元单→出品事件→桥表）全打通且可追溯。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class GiftRedemptionEndToEndTests {

    @Autowired
    private TicketVerificationService verificationService;

    @Autowired
    private JdbcTicketingRepository ticketingRepository;

    @Autowired
    private BarRepository barRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void authenticate() {
        // 生产中核销/冲正必有已认证主体；P0-2 后在线核销主链会按调用者商户范围校验归属。
        // E2E 默认以平台管理员上下文驱动（scope 为空=放行），契合真实调用形态；跨商户用例内部另行覆盖。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        // V159 起 ticket_verifications.device_id 外键引用 terminal_devices；核销主链统一用 device_t15，先建父设备行。
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    status, created_at, updated_at)
                VALUES ('device_t15', 'dev-device_t15', 'DC-device_t15', 'kiosk', 'venue', 'bound', ?, ?)
                """, ts, ts);
    }

    @AfterEach
    void clearAuth() {
        com.complex.platform.support.TestSecurityActors.clear();
    }

    @Test
    void giftDrinkVerificationCreatesZeroAmountGiftOrderAndRedemptionThroughEvent() {
        String suffix = Long.toString(System.nanoTime());
        String barId = "bar_t15_" + suffix;
        seedBarTable(barId);
        String code = "gift-code-" + suffix;
        seedGiftEntitlement(code, barId);

        var response = verificationService.createVerification(new VerificationRequest(
                "req-" + suffix, "qr", code, "device_t15", barId, Map.of("source", "test")));
        assertThat(response.status()).isEqualTo("success");

        // 桥表：一次核销一单（delivered），verification_id 关联本次核销。
        BarGiftRedemptionRecord redemption =
                barRepository.findGiftRedemptionByVerification(response.id()).orElseThrow();
        assertThat(redemption.status()).isEqualTo("delivered");
        assertThat(redemption.barId()).isEqualTo(barId);
        // 无 SKU 映射来源：扣减跳过、movement 留空、note 标注未配置 SKU。
        assertThat(redemption.inventoryMovementId()).isNull();
        assertThat(redemption.note()).contains("no item mapping source");

        // 0 元赠饮单（order_type=gift, amount=0）。
        BarOrderRecord order = barRepository.findOrderById(redemption.barOrderId()).orElseThrow();
        assertThat(order.orderType()).isEqualTo("gift");
        assertThat(order.totalAmountCent()).isZero();

        // 出品 deliver 事件已落（inventory_movement_id 列存在、本期为 null）。
        Integer deliverEvents = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM bar_production_ticket_events WHERE order_id = ? AND action_type = 'deliver'",
                Integer.class, order.id());
        assertThat(deliverEvents).isEqualTo(1);
    }

    // R5 已落地#2：gift_drink 模板配 bar_item_ref=item_X → 核销 → 事件 itemRef=item_X →
    // BarGiftConsumer 对 item_X 走 redeem_consume 扣减（接 T15）。
    @Test
    void giftDrinkWithBarItemRefDecreasesStockViaRedeemConsume() {
        String suffix = Long.toString(System.nanoTime());
        String barId = "bar_t15_" + suffix;
        seedBarTable(barId);
        String itemId = "item_" + suffix;
        seedStockItem(itemId, 5);
        String code = "gift-code-" + suffix;
        seedGiftEntitlement(code, barId, 1, itemId);

        var response = verificationService.createVerification(new VerificationRequest(
                "req-" + suffix, "qr", code, "device_t15", barId, Map.of("source", "test")));
        assertThat(response.status()).isEqualTo("success");

        BarGiftRedemptionRecord redemption =
                barRepository.findGiftRedemptionByVerification(response.id()).orElseThrow();
        // 模板配了 bar_item_ref → itemRef 透传、实际扣减、movement 回填、note 标注扣减成功。
        assertThat(redemption.itemRef()).isEqualTo(itemId);
        assertThat(redemption.inventoryMovementId()).isNotNull();
        assertThat(redemption.note()).contains("stock decreased via redeem_consume");

        // 库存从 5 扣到 4（redeem_consume 扣 1）。
        Integer available = jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM inventory_stock_balances WHERE item_id = ? AND location_id = 'default'",
                Integer.class, itemId);
        assertThat(available).isEqualTo(4);

        // redeem_consume 流水落地，关联本赠饮单。
        Integer movements = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inventory_stock_movements WHERE item_id = ? AND movement_type = 'redeem_consume'",
                Integer.class, itemId);
        assertThat(movements).isEqualTo(1);
    }

    @Test
    void repeatedGiftDrinkVerificationsRemainIdempotentPerVerification() {
        String suffix = Long.toString(System.nanoTime());
        String barId = "bar_t15_" + suffix;
        seedBarTable(barId);
        String code = "gift-code-" + suffix;
        seedGiftEntitlement(code, barId, 2);

        var first = verificationService.createVerification(new VerificationRequest(
                "req-a-" + suffix, "qr", code, "device_t15", barId, Map.of()));
        var second = verificationService.createVerification(new VerificationRequest(
                "req-b-" + suffix, "qr", code, "device_t15", barId, Map.of()));

        // 两次核销 → 两条不同 verification_id → 各自一单（每次核销幂等到一单）。
        assertThat(barRepository.findGiftRedemptionByVerification(first.id())).isPresent();
        assertThat(barRepository.findGiftRedemptionByVerification(second.id())).isPresent();
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void reversingGiftDrinkVerificationMarksRedemptionReversed() {
        String suffix = Long.toString(System.nanoTime());
        String barId = "bar_t15_" + suffix;
        seedBarTable(barId);
        String code = "gift-code-" + suffix;
        seedGiftEntitlement(code, barId);

        var response = verificationService.createVerification(new VerificationRequest(
                "req-" + suffix, "qr", code, "device_t15", barId, Map.of()));
        assertThat(barRepository.findGiftRedemptionByVerification(response.id()).orElseThrow().status())
                .isEqualTo("delivered");

        // P0 越权修复后 reverseVerification 会按调用者商户范围校验归属；冲正在生产中必有认证主体（ticket.verify.reverse）。
        // 此处以平台管理员上下文驱动（scope 为空=放行），契合"已认证主体发起冲正"的真实场景。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            verificationService.reverseVerification(response.id(), "customer left", "staff_1");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }

        assertThat(barRepository.findGiftRedemptionByVerification(response.id()).orElseThrow().status())
                .isEqualTo("reversed");
    }

    @Test
    void crossMerchantOnlineVerificationIsForbidden() {
        // P0-2 回归：A 商户核销端不得核销 B 商户客人的票（跨租户烧票 IDOR）；在线核销主链现按调用者商户范围校验归属。
        String suffix = Long.toString(System.nanoTime());
        String code = "gift-code-xm-" + suffix;
        seedGiftEntitlementForMerchant(code, "bar_xm_" + suffix, "merchant_b");
        com.complex.platform.support.TestSecurityActors.asMerchantUser("merchant_a");
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    verificationService.createVerification(new VerificationRequest(
                            "req-xm-" + suffix, "qr", code, "device_t15", "bar_xm_" + suffix, Map.of())))
                    .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                            e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    private void seedGiftEntitlementForMerchant(String code, String barId, String merchantId) {
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'Gift Type', 'fixed', FALSE, 'active', ?, ?)
                """, typeId, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, bar_item_ref, created_at, updated_at)
                VALUES (?, 'Gift Drink', 'gift_drink', 1, 0, 'active', NULL, ?, ?)
                """, templateId, ts, ts);
        // V159 起 issued_entitlements.order_id 外键引用 ticket_orders，先建父订单避免 FK 违反。
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                java.time.LocalDate.parse("2026-06-18"), "web", null, 0L, "CNY", "paid", "issued", "paid", now, now));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "gift_drink",
                merchantId, null, barId, 1, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null));
    }

    private void seedBarTable(String barId) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type,
                    capacity, allowed_pass_free_guest_count, min_consumption_cent, currency, status,
                    created_at, updated_at)
                VALUES (?, ?, NULL, ?, 'Gift Table', 'standard', 4, 0, 0, 'CNY', 'active', ?, ?)
                """, "table_" + barId, barId, "TC_" + barId, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
    }

    private void seedStockItem(String itemId, int quantity) {
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at)
                VALUES (?, 'Gift Drink Item', 'beverage', 'cup', 'active', ?, ?)
                """, itemId, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity,
                    available_quantity, updated_at)
                VALUES (?, ?, 'default', ?, 0, ?, ?)
                """, "bal_" + itemId, itemId, quantity, quantity, ts);
    }

    private void seedGiftEntitlement(String code, String barId) {
        seedGiftEntitlement(code, barId, 1);
    }

    private void seedGiftEntitlement(String code, String barId, int totalTimes) {
        seedGiftEntitlement(code, barId, totalTimes, null);
    }

    private void seedGiftEntitlement(String code, String barId, int totalTimes, String barItemRef) {
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'Gift Type', 'fixed', FALSE, 'active', ?, ?)
                """, typeId, ts, ts);
        // R5 已落地#2：模板配 bar_item_ref（可空）——赠饮→吧台 SKU 映射来源。
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, bar_item_ref, created_at, updated_at)
                VALUES (?, 'Gift Drink', 'gift_drink', ?, 0, 'active', ?, ?, ?)
                """, templateId, totalTimes, barItemRef, ts, ts);
        // V159 起 issued_entitlements.order_id 外键引用 ticket_orders，先建父订单避免 FK 违反。
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, totalTimes,
                java.time.LocalDate.parse("2026-06-18"), "web", null, 0L, "CNY", "paid", "issued", "paid", now, now));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code,        // id
                code,                 // entitlementCode
                typeId,               // ticketTypeId
                "order_" + code,      // orderId（V159 起有 FK → ticket_orders）
                templateId,           // entitlementTemplateId
                "gift_drink",         // entitlementType
                null,                 // merchantId
                null,                 // venueId
                barId,                // pointId（赠饮所属吧台来源）
                totalTimes,           // totalTimes
                0,                    // usedTimes
                0L,                   // valueAmountCent
                "active",             // status
                now.minusSeconds(60), // validFrom
                now.plusSeconds(3600),// validTo
                now,                  // createdAt
                now,                  // updatedAt
                1,                    // numPeople
                null));               // allocatedRevenueCent
    }
}
