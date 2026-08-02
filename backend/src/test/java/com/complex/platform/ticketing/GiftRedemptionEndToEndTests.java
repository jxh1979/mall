package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.bar.BarGiftRedemptionRecord;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * R5 T15 端到端验收（事件驱动，真 DB 全 Spring 上下文）：核销赠饮权益(gift_drink) → ApplicationEventPublisher
 * 发 ticket.gift_verified → BarGiftConsumer 同步监听 → 0 元 bar_order(order_type='gift') + 出品 deliver 事件 +
 * bar_gift_redemptions 桥表落地。修 R5「无端到端验收」缺口。
 *
 * <p>AUD-015：赠饮缺配置(无 bar_item_ref)/缺库存时核销 fail-closed（抛错回滚，票权益不被消费），绝不静默标记 delivered；
 * 配置齐全时链路（核销→0元单→出品 deliver 事件→扣库 redeem_consume→桥表）全打通且可追溯。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class GiftRedemptionEndToEndTests {

    @Autowired
    private TicketVerificationService verificationService;

    @Autowired
    private com.complex.platform.terminal.TerminalDeviceService terminalDeviceService;

    @Autowired
    private JdbcTicketingRepository ticketingRepository;

    @Autowired
    private BarRepository barRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
                ON CONFLICT (id) DO NOTHING
                """, ts, ts);
    }

    @AfterEach
    void clearAuth() {
        com.complex.platform.support.TestSecurityActors.clear();
    }

    // 权益保留证明：生产入口 TicketingService.createVerification 是 @Transactional；这里用 TransactionTemplate
    // 复刻同一事务边界（本测试 NOT_SUPPORTED 无环境事务 → 模板真开新事务、抛错时真回滚），从而可观察到权益未被消费。
    // 种子直插自动提交、唯一 suffix 隔离残留（种子行 append-only/FK 不可删，无需清理）。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void giftDrinkVerificationWithoutBarItemRefIsRejectedFailClosed() {
        // S5：赠饮权益没绑提供方商品 → 取不到内部结算价 → fail-closed 抛错回滚整笔核销，
        // 票权益不被消费；绝不静默回退票面价（票面原价与内部结算价不可混用）。
        String suffix = Long.toString(System.nanoTime());
        String barId = "bar_t15_" + suffix;
        seedBarTable(barId);
        String deviceId = seedDeviceForBar(barId);
        String code = "gift-code-" + suffix;
        seedGiftEntitlement(code, barId); // 无 bar_item_ref

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                tx.executeWithoutResult(status -> verificationService.createVerification(new VerificationRequest(
                        "req-" + suffix, "qr", code, deviceId, barId, Map.of("source", "test")))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ENTITLEMENT_PRICE_UNRESOLVED"));

        // 核销事务已回滚 → issued entitlement 的 used_times 仍为 0（权益未被消费，可再次核销）。
        Integer usedTimes = jdbcTemplate.queryForObject(
                "SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, "ent_" + code);
        assertThat(usedTimes).isZero();
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
        String productId = "barprod_" + suffix;
        seedBarProduct(productId, barId, itemId, 4200L);
        String deviceId = seedDeviceForBar(barId);
        String code = "gift-code-" + suffix;
        seedGiftEntitlement(code, barId, 1, productId);

        var response = verificationService.createVerification(new VerificationRequest(
                "req-" + suffix, "qr", code, deviceId, barId, Map.of("source", "test")));
        assertThat(response.status()).isEqualTo("success");

        // S5「本刀不产生金额」：内部结算价只固化在核销记录上，逐分等于该商品的内部结算价。
        Long settlementPrice = jdbcTemplate.queryForObject(
                "SELECT settlement_price_cent FROM ticket_verifications WHERE id = ?", Long.class, response.id());
        assertThat(settlementPrice).isEqualTo(4200L);

        BarGiftRedemptionRecord redemption =
                barRepository.findGiftRedemptionByVerification(response.id()).orElseThrow();
        // S5：itemRef 是提供方商品 id；库存扣的是该商品绑定的库存物料。
        assertThat(redemption.itemRef()).isEqualTo(productId);
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
        String itemId = "item_" + suffix;
        seedStockItem(itemId, 5);
        String productId = "barprod_" + suffix;
        seedBarProduct(productId, barId, itemId, 4200L);
        String deviceId = seedDeviceForBar(barId);
        String code = "gift-code-" + suffix;
        seedGiftEntitlement(code, barId, 2, productId);

        var first = verificationService.createVerification(new VerificationRequest(
                "req-a-" + suffix, "qr", code, deviceId, barId, Map.of()));
        var second = verificationService.createVerification(new VerificationRequest(
                "req-b-" + suffix, "qr", code, deviceId, barId, Map.of()));

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
        String itemId = "item_" + suffix;
        seedStockItem(itemId, 5);
        String productId = "barprod_" + suffix;
        seedBarProduct(productId, barId, itemId, 4200L);
        String deviceId = seedDeviceForBar(barId);
        String code = "gift-code-" + suffix;
        seedGiftEntitlement(code, barId, 1, productId);

        var response = verificationService.createVerification(new VerificationRequest(
                "req-" + suffix, "qr", code, deviceId, barId, Map.of()));
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

    /**
     * S5 K16：A 吧的赠酒拿到 B 吧核销 → 拒。与角色无关（本用例用平台管理员，权限层本来放行）。
     * 「只拒不改钱」：被拒后权益未消费、核销记录/结算流水/往来项一条都不落。
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void giftRedeemedAtAnotherBarIsRejectedAndWritesNoMoney() {
        String suffix = Long.toString(System.nanoTime());
        String providerBar = "bar_a_" + suffix;
        String otherBar = "bar_b_" + suffix;
        seedBarTable(providerBar);
        String itemId = "item_" + suffix;
        seedStockItem(itemId, 5);
        String productId = "barprod_" + suffix;
        seedBarProduct(productId, providerBar, itemId, 4200L);
        String otherBarDevice = seedDeviceForBar(otherBar); // 设备绑的是 B 吧
        String code = "gift-code-k16-" + suffix;
        seedGiftEntitlement(code, providerBar, 1, productId); // 权益是 A 吧请的

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                tx.executeWithoutResult(status -> verificationService.createVerification(new VerificationRequest(
                        "req-k16-" + suffix, "qr", code, otherBarDevice, otherBar, Map.of()))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ENTITLEMENT_PROVIDER_MISMATCH"));

        // 只拒不改钱：权益未消费、核销记录/结算流水/往来项都没有。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, "ent_" + code)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_verifications WHERE entitlement_id = ?", Integer.class, "ent_" + code))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_transactions WHERE entitlement_id = ?", Integer.class, "ent_" + code))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ar_ap_items WHERE source_type = 'pass_drink_settlement' AND source_id LIKE ?",
                Integer.class, "%" + suffix + "%")).isZero();
    }

    /** S5 K16：离线同步旁路走同一入口，同样拦得住（不能靠离线补单绕过跨吧核销）。 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void giftRedeemedAtAnotherBarIsAlsoRejectedOnOfflineSyncPath() {
        String suffix = Long.toString(System.nanoTime());
        String providerBar = "bar_a_off_" + suffix;
        String otherBar = "bar_b_off_" + suffix;
        seedBarTable(providerBar);
        String itemId = "item_off_" + suffix;
        seedStockItem(itemId, 5);
        String productId = "barprod_off_" + suffix;
        seedBarProduct(productId, providerBar, itemId, 4200L);
        String otherBarDevice = seedDeviceForBar(otherBar);
        String deviceCode = jdbcTemplate.queryForObject(
                "SELECT device_code FROM terminal_devices WHERE id = ?", String.class, otherBarDevice);
        String code = "gift-code-k16off-" + suffix;
        seedGiftEntitlement(code, providerBar, 1, productId);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                tx.executeWithoutResult(status -> verificationService.syncTerminalTasks(
                        new com.complex.platform.ticketing.dto.TerminalSyncTasksRequest(
                                "idem-k16-" + suffix, null, java.util.List.of(), deviceCode, "staff_1",
                                java.util.List.of(new com.complex.platform.ticketing.dto.TerminalSyncTaskItemRequest(
                                        "task-k16-" + suffix, "offline_verification", "2026-06-18T10:00:00Z",
                                        Map.of("entitlementCode", code, "pointId", otherBar)))))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ENTITLEMENT_PROVIDER_MISMATCH"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, "ent_" + code)).isZero();
    }

    /**
     * K16 归属跟着改绑走：设备绑在提供方 A 吧时核销放行；把设备改绑到 B 吧后，
     * 同一台设备再核 A 吧的赠饮被拒（ENTITLEMENT_PROVIDER_MISMATCH）——
     * 商户归属写入面（PUT /terminals/devices/{id}/merchant）改的就是 K16 的核销上下文。
     */
    @Test
    void rebindingDeviceMerchantMovesK16VerificationContext() {
        String suffix = Long.toString(System.nanoTime());
        String providerBar = "bar_k16re_" + suffix;
        String otherBar = "bar_k16re_b_" + suffix;
        seedBarTable(providerBar);
        String itemId = "item_k16re_" + suffix;
        seedStockItem(itemId, 5);
        String productId = "barprod_k16re_" + suffix;
        seedBarProduct(productId, providerBar, itemId, 4200L);
        String deviceId = seedDeviceForBar(providerBar); // 设备起初绑提供方 A 吧
        String code1 = "gift-code-k16re-1-" + suffix;
        String code2 = "gift-code-k16re-2-" + suffix;
        seedGiftEntitlement(code1, providerBar, 1, productId);
        seedGiftEntitlement(code2, providerBar, 1, productId);

        // 改绑前：设备归属 = 提供方 → K16 放行，核销成功。
        var response = verificationService.createVerification(new VerificationRequest(
                "req-k16re-1-" + suffix, "qr", code1, deviceId, providerBar, Map.of()));
        assertThat(response.status()).isEqualTo("success");

        // 改绑：设备挪到 B 吧（目标商户须存在于 merchants 且未归档）。
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES (?, ?, 'bar', 'active', ?, ?)
                """, otherBar, "B吧" + suffix, ts, ts);
        terminalDeviceService.updateDeviceMerchant(deviceId,
                new com.complex.platform.terminal.dto.DeviceMerchantRequest(
                        "dm-k16re-" + suffix, otherBar, "设备挪到 B 吧"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT merchant_id FROM terminal_devices WHERE id = ?", String.class, deviceId))
                .isEqualTo(otherBar);

        // 改绑后：同一设备核 A 吧赠饮 → K16 判定跟着新归属走，拒绝。
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                verificationService.createVerification(new VerificationRequest(
                        "req-k16re-2-" + suffix, "qr", code2, deviceId, providerBar, Map.of())))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ENTITLEMENT_PROVIDER_MISMATCH"));

        // 改绑留痕：append-only 状态事件一条（谁改的/为什么在审计事件，此处验事件与理由）。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM terminal_device_status_events WHERE device_id = ? AND event_type = 'terminal.device.merchant_changed'",
                Integer.class, deviceId)).isEqualTo(1);
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
                java.time.LocalDate.parse("2026-06-18"), "web", null, 0L, "CNY", "paid", "issued", "created", now, now));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "gift_drink",
                merchantId, null, barId, 1, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                null, null));
    }

    /**
     * S5 K16：核销设备必须绑定商户，否则说不清这杯酒在哪家核的 → 一律拒绝。
     * 每个用例一台绑到本吧的设备（用例间 barId 唯一，互不串味）。
     */
    private String seedDeviceForBar(String barId) {
        String deviceId = "dev_" + barId;
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    status, merchant_id, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET merchant_id = EXCLUDED.merchant_id
                """, deviceId, "d-" + deviceId, "DC-" + deviceId, barId, ts, ts);
        return deviceId;
    }

    /** S5：提供方商品（bar_item_ref = bar_products.id），带内部结算价与库存物料。 */
    private void seedBarProduct(String productId, String barId, String inventoryItemId, Long internalPriceCent) {
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        jdbcTemplate.update("""
                INSERT INTO bar_products (id, bar_id, product_name, product_type, price_cent, currency,
                    inventory_item_id, stock_location_id, stock_deduction_required, status,
                    internal_settlement_price_cent, created_at, updated_at)
                VALUES (?, ?, 'Gift Drink', 'beverage', 3800, 'CNY', ?, 'default', ?, 'active', ?, ?, ?)
                """, productId, barId, inventoryItemId, inventoryItemId != null, internalPriceCent, ts, ts);
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
                java.time.LocalDate.parse("2026-06-18"), "web", null, 0L, "CNY", "paid", "issued", "created", now, now));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code,        // id
                code,                 // entitlementCode
                typeId,               // ticketTypeId
                "order_" + code,      // orderId（V159 起有 FK → ticket_orders）
                templateId,           // entitlementTemplateId
                "gift_drink",         // entitlementType
                barId,                // merchantId（S5：提供方商户＝这家吧，客人须到这家核销、成本按这家的内部价计）
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
                null,
                null, null));               // allocatedRevenueCent
    }
}
