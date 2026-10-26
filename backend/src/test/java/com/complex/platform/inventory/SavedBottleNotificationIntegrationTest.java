package com.complex.platform.inventory;

// 文件版本：V0.04

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
    private static final String SHORT_PHONE = "15500010006";

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
                .isEqualTo("swexpiring:" + expiringBottleId + ":2026-07-02:3");
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

    /**
     * BSM L1072「到期前按后台配置的提醒节点各发一次短信提醒（业务参数，按项目可配，出厂默认到期前第 14 天、第 7 天、第 3 天、第 1 天）；
     * ……超出本次存酒实际有效期的节点自动跳过」：出厂节点逐个各发一次（同一节点内重跑不重发），有效期短于节点的瓶跳过该节点。
     */
    @Test
    void expiringReminderSendsOncePerFactoryNodeAndSkipsNodesBeyondValidity() {
        String prefix = unique("nodes");
        Instant expireAt = Instant.parse("2026-07-20T04:00:00Z"); // 北京 2026-07-20 12:00
        insertCustomer(prefix + "_customer", EXPIRING_PHONE);
        insertCustomer(prefix + "_short_customer", SHORT_PHONE);
        insertInventoryItem(prefix + "_item", prefix + "_sku", "Node Riesling");
        insertStoredBottle(prefix + "_bottle", prefix + "_customer", prefix + "_item", "Node Riesling", expireAt);
        insertStoredBottle(prefix + "_short", prefix + "_short_customer", prefix + "_item", "Node Riesling", expireAt,
                expireAt.minus(java.time.Duration.ofDays(10)));

        notificationJob.runOnce(Instant.parse("2026-07-06T02:00:00Z")); // 北京 07-06：到期前第 14 天
        notificationJob.runOnce(Instant.parse("2026-07-08T02:00:00Z")); // 第 12 天：仍是 14 天节点，不重发
        notificationJob.runOnce(Instant.parse("2026-07-13T02:00:00Z")); // 第 7 天
        notificationJob.runOnce(Instant.parse("2026-07-17T02:00:00Z")); // 第 3 天
        notificationJob.runOnce(Instant.parse("2026-07-19T02:00:00Z")); // 第 1 天

        String key = "swexpiring:" + prefix + "_bottle:2026-07-20:";
        assertThat(requestIds("expiring", EXPIRING_PHONE)).containsExactlyInAnyOrder(key + 14, key + 7, key + 3, key + 1);
        // 有效期 10 天的瓶：第 14 天节点超出本次有效期，跳过；其余节点照发。
        String shortKey = "swexpiring:" + prefix + "_short:2026-07-20:";
        assertThat(requestIds("expiring", SHORT_PHONE)).containsExactlyInAnyOrder(shortKey + 7, shortKey + 3, shortKey + 1);
    }

    /** BSM L1072「按项目可配」：项目单独配置的节点取代出厂节点；节点须为不重复的到期前第 N 天；配置留痕 updated_by。 */
    @Test
    void projectReminderNodesOverrideFactoryNodes() {
        String prefix = unique("project");
        String projectId = prefix + "_p";
        String barId = prefix + "_bar";
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', 'Reminder Project', 'active', now(), now())", projectId);
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) VALUES (?, 'Reminder Bar', 'bar', ?, 'active', now(), now())", barId, projectId);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> inventoryService.configureReminderPolicy(
                        new com.complex.platform.inventory.dto.SavedBottleReminderPolicyRequest(prefix + "_cfg0", "project:" + projectId, List.of(5, 5))))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
        inventoryService.configureReminderPolicy(new com.complex.platform.inventory.dto.SavedBottleReminderPolicyRequest(
                prefix + "_cfg1", "project:" + projectId, List.of(2, 5)));
        assertThat(jdbcTemplate.queryForMap("SELECT reminder_days, updated_by FROM saved_bottle_reminder_policy WHERE scope_id = ?",
                "project:" + projectId)).containsEntry("reminder_days", "5,2").containsEntry("updated_by", "usr_admin");

        Instant expireAt = Instant.parse("2026-07-20T04:00:00Z");
        insertCustomer(prefix + "_customer", EXPIRING_PHONE);
        insertInventoryItem(prefix + "_item", prefix + "_sku", "Project Riesling");
        insertStoredBottle(prefix + "_bottle", prefix + "_customer", prefix + "_item", "Project Riesling", expireAt);
        jdbcTemplate.update("UPDATE saved_bottles SET bar_project_id = ? WHERE id = ?", barId, prefix + "_bottle");

        notificationJob.runOnce(Instant.parse("2026-07-13T02:00:00Z")); // 到期前第 7 天：本项目不在节点内
        notificationJob.runOnce(Instant.parse("2026-07-15T02:00:00Z")); // 第 5 天
        notificationJob.runOnce(Instant.parse("2026-07-18T02:00:00Z")); // 第 2 天

        String key = "swexpiring:" + prefix + "_bottle:2026-07-20:";
        assertThat(requestIds("expiring", EXPIRING_PHONE)).containsExactlyInAnyOrder(key + 5, key + 2);
    }

    /**
     * BSM L1072「到期前按后台配置的提醒节点……按项目可配，出厂默认到期前第 14 天、第 7 天、第 3 天、第 1 天」：后台读得出出厂行与项目行；
     * 项目员工只看出厂行与本项目，商户账号拒绝（范围闸在读之前，fail-closed）。
     */
    @Test
    void listReminderPoliciesShowsFactoryRowAndScopesProjectRows() {
        String prefix = unique("list");
        String own = prefix + "_own";
        String other = prefix + "_other";
        jdbcTemplate.update("INSERT INTO saved_bottle_reminder_policy (scope_id, reminder_days) VALUES (?, '10,2'), (?, '9')",
                "project:" + own, "project:" + other);

        Map<String, List<Integer>> all = reminderPolicies();
        assertThat(all).containsEntry("global", List.of(14, 7, 3, 1))
                .containsEntry("project:" + own, List.of(10, 2))
                .containsEntry("project:" + other, List.of(9));

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor("usr_" + own, "project_staff", "staff", own, null, "active"),
                        "token", List.of()));
        Map<String, List<Integer>> mine = reminderPolicies();
        assertThat(mine).containsEntry("global", List.of(14, 7, 3, 1)).containsEntry("project:" + own, List.of(10, 2))
                .doesNotContainKey("project:" + other);

        TestSecurityActors.asMerchantUser(prefix + "_merchant");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> inventoryService.listReminderPolicies())
                .isInstanceOf(com.complex.platform.common.api.ApiException.class).extracting("code").isEqualTo("PLATFORM_SCOPE_REQUIRED");
    }

    private Map<String, List<Integer>> reminderPolicies() {
        return inventoryService.listReminderPolicies().items().stream()
                .collect(java.util.stream.Collectors.toMap(SavedBottleReminderPolicyRecord::scopeId, SavedBottleReminderPolicyRecord::reminderDays));
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

    /** 默认按 30 天有效期登记（登记时间＝到期前 30 天）。 */
    private void insertStoredBottle(String id, String customerId, String itemId, String bottleName, Instant expireAt) {
        insertStoredBottle(id, customerId, itemId, bottleName, expireAt, expireAt.minus(java.time.Duration.ofDays(30)));
    }

    private void insertStoredBottle(String id, String customerId, String itemId, String bottleName, Instant expireAt,
            Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO saved_bottles (
                    id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity,
                    remaining_quantity, unit, expire_at, status, evidence_attachment_ids,
                    created_at, updated_at, is_high_value, bottle_label_value,
                    customer_name_or_nickname, registration_photo_refs
                )
                VALUES (?, ?, 'bar_notif', ?, ?, 1, 1, 'bottle', ?, 'stored', '[]'::jsonb,
                        ?, now(), false, 'LABEL-001', 'Guest A', '["registration-photo"]'::jsonb)
                """, id, customerId, itemId, bottleName, java.sql.Timestamp.from(expireAt), java.sql.Timestamp.from(createdAt));
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
                java.util.Set.of(DEPOSIT_PHONE, CLAIM_PHONE, SWITCH_OFF_PHONE, EXPIRED_PHONE, EXPIRING_PHONE, SHORT_PHONE)::contains));
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
        jdbcTemplate.update("DELETE FROM saved_bottle_reminder_policy WHERE scope_id LIKE 'project:sw_notif_%'");
        jdbcTemplate.update("DELETE FROM merchants WHERE id LIKE 'sw_notif_%'");
        jdbcTemplate.update("DELETE FROM projects WHERE id LIKE 'sw_notif_%'");
        jdbcTemplate.update("DELETE FROM customers WHERE id LIKE 'sw_notif_%'");
    }

    private String idFor(String capabilityCode) {
        return "test_tcs_" + capabilityCode.replace(".", "_");
    }

    private String savedBottleStatus(String id) {
        return jdbcTemplate.queryForObject("SELECT status FROM saved_bottles WHERE id = ?", String.class, id);
    }

    private List<String> requestIds(String event, String recipient) {
        return logIds(event, recipient).stream()
                .map(id -> jdbcTemplate.queryForObject("SELECT request_id FROM notification_send_log WHERE id = ?", String.class, id))
                .toList();
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
