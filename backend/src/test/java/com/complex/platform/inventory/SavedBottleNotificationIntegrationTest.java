package com.complex.platform.inventory;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SavedBottleNotificationIntegrationTest {

    private static final String DEPOSIT_PHONE = "15500010001";
    private static final String CLAIM_PHONE = "15500010002";
    private static final String SWITCH_OFF_PHONE = "15500010003";
    private static final String EXPIRED_PHONE = "15500010004";
    private static final String EXPIRING_PHONE = "15500010005";

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SavedBottleNotificationJob notificationJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.complex.platform.common.crypto.PiiCipher piiCipher;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        cleanupData();
        setSwitch("sms_notification", true);
        setSwitch("sms.notify.stored_wine", true);
        setSwitch("sms.notify.stored_wine.deposit", true);
        setSwitch("sms.notify.stored_wine.claim", true);
        setSwitch("sms.notify.stored_wine.expiring", true);
        setSwitch("sms.notify.stored_wine.expired", true);
    }

    @AfterEach
    void tearDown() {
        cleanupData();
        setSwitch("sms_notification", false);
        setSwitch("sms.notify.stored_wine", true);
        setSwitch("sms.notify.stored_wine.deposit", true);
        setSwitch("sms.notify.stored_wine.claim", true);
        setSwitch("sms.notify.stored_wine.expiring", true);
        setSwitch("sms.notify.stored_wine.expired", true);
        TestSecurityActors.clear();
    }

    @Test
    void createAndClaimSavedBottleWriteNotificationLogsWhenSwitchesOn() {
        String prefix = unique("success");
        String customerId = prefix + "_customer";
        String itemId = prefix + "_item";
        insertCustomer(customerId, DEPOSIT_PHONE);
        insertInventoryItem(itemId, prefix + "_sku", "Deposit Merlot");

        // 到期日取相对未来时刻（now+30天，截断到秒避免 DB 微秒精度回读漂移）：硬编码 2026-07-10 一旦成为过去，
        // 后续 claimSavedBottle 因 expireAt 已过而抛 SAVED_BOTTLE_EXPIRED（InventoryService 552-553），本用例即 flaky。
        Instant expireAt = Instant.now()
                .plus(java.time.Duration.ofDays(30))
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        SavedBottleRecord bottle = inventoryService.createSavedBottle(savedBottleRequest(
                prefix + "_create",
                customerId,
                itemId,
                "Deposit Merlot",
                expireAt
        ));

        assertThat(logCount("deposit", DEPOSIT_PHONE)).isEqualTo(1);
        assertThat(logValue("deposit", DEPOSIT_PHONE, "request_id")).isEqualTo("swdeposit:" + bottle.id());
        assertThat(logValue("deposit", DEPOSIT_PHONE, "template_code")).isEqualTo("sms.stored_wine.deposit");
        assertThat(logValue("deposit", DEPOSIT_PHONE, "status")).isEqualTo("sent");
        assertThat(logParam("deposit", DEPOSIT_PHONE, "bottle")).isEqualTo("Deposit Merlot");
        assertThat(logParam("deposit", DEPOSIT_PHONE, "item")).isEqualTo(itemId);
        // 客人看到的是中文北京日期(如 2026年8月3日),不是 Instant.toString() 的 2026-08-02T18:00:00Z。
        assertThat(logParam("deposit", DEPOSIT_PHONE, "expireAt")).isEqualTo(
                java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日")
                        .format(expireAt.atZone(com.complex.platform.common.LocalZone.ID)));

        jdbcTemplate.update("UPDATE customers SET phone = ?, masked_phone = ?, updated_at = now() WHERE id = ?",
                piiCipher.encrypt(CLAIM_PHONE), maskPhone(CLAIM_PHONE), customerId);
        SavedBottleClaimRecord claim = inventoryService.claimSavedBottle(bottle.id(), new SavedBottleClaimRequest(
                prefix + "_claim",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                CLAIM_PHONE,
                "000000",
                "LABEL-001",
                "Guest A"
        ));

        assertThat(claim.status()).isEqualTo("picked_up");
        assertThat(logCount("claim", CLAIM_PHONE)).isEqualTo(1);
        assertThat(logValue("claim", CLAIM_PHONE, "request_id")).isEqualTo("swclaim:" + claim.id());
        assertThat(logValue("claim", CLAIM_PHONE, "template_code")).isEqualTo("sms.stored_wine.claim");
        assertThat(logValue("claim", CLAIM_PHONE, "status")).isEqualTo("sent");
        assertThat(logParam("claim", CLAIM_PHONE, "claim_id")).isEqualTo(claim.id());
        assertThat(logParam("claim", CLAIM_PHONE, "claim_quantity")).isEqualTo("1");
    }

    @Test
    void notificationJobWritesExpiredAndExpiringLogsAndMarksExpiredBottle() {
        String prefix = unique("job");
        String expiredCustomerId = prefix + "_expired_customer";
        String expiringCustomerId = prefix + "_expiring_customer";
        String expiredItemId = prefix + "_expired_item";
        String expiringItemId = prefix + "_expiring_item";
        String expiredBottleId = prefix + "_expired_bottle";
        String expiringBottleId = prefix + "_expiring_bottle";
        Instant now = Instant.parse("2026-06-29T12:00:00Z");

        insertCustomer(expiredCustomerId, EXPIRED_PHONE);
        insertCustomer(expiringCustomerId, EXPIRING_PHONE);
        insertInventoryItem(expiredItemId, prefix + "_expired_sku", "Expired Syrah");
        insertInventoryItem(expiringItemId, prefix + "_expiring_sku", "Expiring Riesling");
        insertStoredBottle(expiredBottleId, expiredCustomerId, expiredItemId, "Expired Syrah", now.minusSeconds(3600));
        insertStoredBottle(expiringBottleId, expiringCustomerId, expiringItemId, "Expiring Riesling", now.plusSeconds(3 * 86400));

        notificationJob.runOnce(now);

        assertThat(savedBottleStatus(expiredBottleId)).isEqualTo("expired");
        assertThat(savedBottleStatus(expiringBottleId)).isEqualTo("stored");
        assertThat(logCount("expired", EXPIRED_PHONE)).isEqualTo(1);
        assertThat(logValue("expired", EXPIRED_PHONE, "request_id")).isEqualTo("swexpired:" + expiredBottleId);
        assertThat(logValue("expired", EXPIRED_PHONE, "template_code")).isEqualTo("sms.stored_wine.expired");
        assertThat(logValue("expired", EXPIRED_PHONE, "status")).isEqualTo("sent");

        assertThat(logCount("expiring", EXPIRING_PHONE)).isEqualTo(1);
        assertThat(logValue("expiring", EXPIRING_PHONE, "request_id"))
                .isEqualTo("swexpiring:" + expiringBottleId + ":2026-07-02");
        assertThat(logValue("expiring", EXPIRING_PHONE, "template_code")).isEqualTo("sms.stored_wine.expiring");
        assertThat(logValue("expiring", EXPIRING_PHONE, "status")).isEqualTo("sent");

        notificationJob.runOnce(now.plusSeconds(60));

        assertThat(logCount("expired", EXPIRED_PHONE)).isEqualTo(1);
        assertThat(logCount("expiring", EXPIRING_PHONE)).isEqualTo(1);
    }

    /**
     * OBS-13-04（BSM 第 1598 行「存酒到期提醒」不在可关闭范围内，不得按类或按条关闭）：总开关、存酒域开关、事件开关全关，
     * 到期提醒照发；「存酒已过期」不在名单内，仍按开关跳过。
     */
    @Test
    void expiringReminderStillSendsWhenAllSwitchesOffWhileExpiredNoticeStaysClosable() {
        setSwitch("sms_notification", false);
        setSwitch("sms.notify.stored_wine", false);
        setSwitch("sms.notify.stored_wine.expiring", false);
        setSwitch("sms.notify.stored_wine.expired", false);
        String prefix = unique("mandatory");
        Instant now = Instant.parse("2026-06-29T12:00:00Z");
        insertCustomer(prefix + "_expired_customer", EXPIRED_PHONE);
        insertCustomer(prefix + "_expiring_customer", EXPIRING_PHONE);
        insertInventoryItem(prefix + "_expired_item", prefix + "_expired_sku", "Expired Syrah");
        insertInventoryItem(prefix + "_expiring_item", prefix + "_expiring_sku", "Expiring Riesling");
        insertStoredBottle(prefix + "_expired_bottle", prefix + "_expired_customer", prefix + "_expired_item", "Expired Syrah",
                now.minusSeconds(3600));
        insertStoredBottle(prefix + "_expiring_bottle", prefix + "_expiring_customer", prefix + "_expiring_item", "Expiring Riesling",
                now.plusSeconds(3 * 86400));

        notificationJob.runOnce(now);

        assertThat(logValue("expiring", EXPIRING_PHONE, "status")).isEqualTo("sent");
        assertThat(logValue("expired", EXPIRED_PHONE, "status")).isEqualTo("skipped_switch_off");
    }

    @Test
    void createSavedBottleWritesSkippedNotificationLogWhenMainSwitchOff() {
        setSwitch("sms_notification", false);
        String prefix = unique("off");
        String customerId = prefix + "_customer";
        String itemId = prefix + "_item";
        insertCustomer(customerId, SWITCH_OFF_PHONE);
        insertInventoryItem(itemId, prefix + "_sku", "Switch Off Malbec");

        inventoryService.createSavedBottle(savedBottleRequest(
                prefix + "_create",
                customerId,
                itemId,
                "Switch Off Malbec",
                Instant.parse("2026-07-10T00:00:00Z")
        ));

        assertThat(logCount("deposit", SWITCH_OFF_PHONE)).isEqualTo(1);
        assertThat(logValue("deposit", SWITCH_OFF_PHONE, "status")).isEqualTo("skipped_switch_off");
        assertThat(logValue("deposit", SWITCH_OFF_PHONE, "reason")).isEqualTo("switch_off:sms_notification");
    }

    private SavedBottleRequest savedBottleRequest(
            String idempotencyKey,
            String customerId,
            String itemId,
            String bottleName,
            Instant expireAt
    ) {
        return new SavedBottleRequest(
                idempotencyKey,
                null,
                List.of(),
                customerId,
                "bar_notif",
                List.of(Map.of("item_id", itemId, "quantity", 1, "bottle_name", bottleName, "unit", "bottle")),
                expireAt,
                "LABEL-001",
                "Guest A",
                DEPOSIT_PHONE,
                List.of("registration-photo"),
                null
        );
    }

    private void insertCustomer(String id, String phone) {
        jdbcTemplate.update("""
                INSERT INTO customers (id, phone, masked_phone, status, birth_date, last_login_at, created_at, updated_at)
                VALUES (?, ?, ?, 'active', NULL, NULL, now(), now())
                ON CONFLICT (id) DO UPDATE SET
                    phone = EXCLUDED.phone,
                    masked_phone = EXCLUDED.masked_phone,
                    status = EXCLUDED.status,
                    updated_at = now()
                """, id, piiCipher.encrypt(phone), maskPhone(phone));
    }

    private void insertInventoryItem(String id, String sku, String name) {
        jdbcTemplate.update("""
                INSERT INTO inventory_items (
                    id, sku_code, item_name, item_type, unit, barcode, trace_required,
                    min_quantity, status, created_at, updated_at
                )
                VALUES (?, ?, ?, 'wine', 'bottle', NULL, false, 0, 'active', now(), now())
                ON CONFLICT (id) DO UPDATE SET
                    sku_code = EXCLUDED.sku_code,
                    item_name = EXCLUDED.item_name,
                    status = EXCLUDED.status,
                    updated_at = now()
                """, id, sku, name);
    }

    private void insertStoredBottle(String id, String customerId, String itemId, String bottleName, Instant expireAt) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottles (
                    id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity,
                    remaining_quantity, unit, expire_at, status, evidence_attachment_ids,
                    created_at, updated_at, is_high_value, bottle_label_value,
                    customer_name_or_nickname, registration_photo_refs
                )
                VALUES (?, ?, 'bar_notif', ?, ?, 1, 1, 'bottle', ?, 'stored', '[]'::jsonb,
                        now(), now(), false, 'LABEL-001', 'Guest A', '["registration-photo"]'::jsonb)
                """, id, customerId, itemId, bottleName, java.sql.Timestamp.from(expireAt));
    }

    private void setSwitch(String capabilityCode, boolean enabled) {
        jdbcTemplate.update("""
                INSERT INTO terminal_capability_switches (
                    id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at
                )
                VALUES (?, '*', ?, 'global', 'global', ?, 'saved bottle notification test switch override', now(), now())
                ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO UPDATE SET
                    enabled = EXCLUDED.enabled,
                    reason = EXCLUDED.reason,
                    updated_at = now()
                """, idFor(capabilityCode), capabilityCode, enabled);
    }

    private void cleanupData() {
        jdbcTemplate.update("""
                DELETE FROM idempotency_records
                 WHERE idempotency_key LIKE 'sw_notif_%'
                    OR idempotency_key LIKE 'swexpired:sw_notif_%'
                    OR idempotency_key LIKE 'swexpiring:sw_notif_%'
                """);
        // 收件人手机号落密文（V547）：取出解密后按测试手机号筛。
        com.complex.platform.support.NotificationSendLogRows.delete(jdbcTemplate, com.complex.platform.support.NotificationSendLogRows.ids(
                jdbcTemplate, piiCipher, "stored_wine", null,
                java.util.Set.of(DEPOSIT_PHONE, CLAIM_PHONE, SWITCH_OFF_PHONE, EXPIRED_PHONE, EXPIRING_PHONE)::contains));
        jdbcTemplate.update("""
                DELETE FROM saved_bottle_claims
                 WHERE saved_bottle_id IN (
                    SELECT id
                      FROM saved_bottles
                     WHERE customer_id LIKE 'sw_notif_%'
                        OR id LIKE 'sw_notif_%'
                 )
                """);
        jdbcTemplate.update("""
                DELETE FROM saved_bottle_transfers
                 WHERE saved_bottle_id IN (
                    SELECT id
                      FROM saved_bottles
                     WHERE customer_id LIKE 'sw_notif_%'
                        OR id LIKE 'sw_notif_%'
                 )
                """);
        jdbcTemplate.update("""
                DELETE FROM saved_bottles
                 WHERE customer_id LIKE 'sw_notif_%'
                    OR id LIKE 'sw_notif_%'
                """);
        jdbcTemplate.update("DELETE FROM inventory_items WHERE id LIKE 'sw_notif_%'");
        jdbcTemplate.update("DELETE FROM customers WHERE id LIKE 'sw_notif_%'");
    }

    private String idFor(String capabilityCode) {
        return "test_tcs_" + capabilityCode.replace(".", "_");
    }

    private String savedBottleStatus(String id) {
        return jdbcTemplate.queryForObject("SELECT status FROM saved_bottles WHERE id = ?", String.class, id);
    }

    private int logCount(String event, String recipient) {
        return logIds(event, recipient).size();
    }

    private String logValue(String event, String recipient, String column) {
        return jdbcTemplate.queryForObject("SELECT %s FROM notification_send_log WHERE id = ?".formatted(column), String.class,
                logIds(event, recipient).getFirst());
    }

    private String logParam(String event, String recipient, String key) {
        return jdbcTemplate.queryForObject("SELECT params ->> ? FROM notification_send_log WHERE id = ?", String.class, key,
                logIds(event, recipient).getFirst());
    }

    /** 收件人手机号落密文（V547），按明文筛行须解密后比。 */
    private java.util.List<String> logIds(String event, String recipient) {
        return com.complex.platform.support.NotificationSendLogRows.ids(jdbcTemplate, piiCipher, "stored_wine", event, recipient::equals);
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 7) {
            return phone.charAt(0) + "****" + phone.charAt(phone.length() - 1);
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String unique(String label) {
        return "sw_notif_" + label + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
