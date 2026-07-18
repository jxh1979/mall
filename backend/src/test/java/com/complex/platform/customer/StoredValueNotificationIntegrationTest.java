package com.complex.platform.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.TestSecurityActors;
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
        "spring.datasource.password=complex_dev_pwd"
})
class StoredValueNotificationIntegrationTest {

    private static final String TEST_PHONE_PREFIX = "15588";
    private static final String PROJECT_ID = "sv_notif_project";

    @Autowired
    private StoredValueService storedValueService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.complex.platform.common.crypto.PiiCipher piiCipher;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        cleanupNotificationData();
        setSwitch("sms_notification", true);
        setSwitch("sms.notify.stored_value", true);
        setSwitch("sms.notify.stored_value.topup", true);
        setSwitch("sms.notify.stored_value.consume", true);
    }

    @AfterEach
    void tearDown() {
        cleanupNotificationData();
        setSwitch("sms_notification", false);
        setSwitch("sms.notify.stored_value", true);
        setSwitch("sms.notify.stored_value.topup", true);
        setSwitch("sms.notify.stored_value.consume", true);
        TestSecurityActors.clear();
    }

    @Test
    void settleTopupWritesStoredValueTopupNotificationLogWhenSwitchesOn() {
        String prefix = unique("topup");
        String customerId = prefix + "_customer";
        String phone = uniquePhone();
        insertCustomer(customerId, phone);

        CustomerStoredValueTransactionRecord txn = settleTopup(customerId, 5_000L, prefix);

        assertThat(logCount("topup", phone)).isEqualTo(1);
        assertThat(logValue("topup", phone, "request_id")).isEqualTo("svtopup:" + txn.id());
        assertThat(logValue("topup", phone, "template_code")).isEqualTo("sms.stored_value.topup");
        assertThat(logValue("topup", phone, "status")).isEqualTo("sent");
        assertThat(logParam("topup", phone, "amount_cent")).isEqualTo("5000");
        assertThat(logParam("topup", phone, "balance_after_cent")).isEqualTo("5000");
        assertThat(logParam("topup", phone, "card_type")).isEqualTo("project");

        // V231 资金/时间铁律：充值即支付，topup 流水 paid_at 落库(append-only 表 INSERT 携带)，非 null 且 ≈ 交易时刻。
        java.sql.Timestamp paidAt = jdbcTemplate.queryForObject(
                "SELECT paid_at FROM customer_stored_value_transactions WHERE id = ?",
                java.sql.Timestamp.class, txn.id());
        assertThat(paidAt).isNotNull();
        assertThat(paidAt.toInstant())
                .isCloseTo(java.time.Instant.now(),
                        org.assertj.core.api.Assertions.within(2, java.time.temporal.ChronoUnit.MINUTES));
    }

    @Test
    void consumeWritesStoredValueConsumeNotificationLogWhenSwitchesOn() {
        String prefix = unique("consume");
        String customerId = prefix + "_customer";
        String phone = uniquePhone();
        insertCustomer(customerId, phone);
        settleTopup(customerId, 8_000L, prefix);

        CustomerStoredValueTransactionRecord txn = storedValueService.consume(
                customerId,
                PROJECT_ID,
                null,
                2_300L,
                "notification test consume",
                prefix + "_consume");

        assertThat(logCount("consume", phone)).isEqualTo(1);
        assertThat(logValue("consume", phone, "request_id")).isEqualTo("svconsume:" + txn.id());
        assertThat(logValue("consume", phone, "template_code")).isEqualTo("sms.stored_value.consume");
        assertThat(logValue("consume", phone, "status")).isEqualTo("sent");
        assertThat(logParam("consume", phone, "amount_cent")).isEqualTo("2300");
        assertThat(logParam("consume", phone, "balance_after_cent")).isEqualTo("5700");
        assertThat(logParam("consume", phone, "card_type")).isEqualTo("project");
    }

    @Test
    void settleTopupWritesSkippedNotificationLogWhenMainSwitchOff() {
        setSwitch("sms_notification", false);
        String prefix = unique("off");
        String customerId = prefix + "_customer";
        String phone = uniquePhone();
        insertCustomer(customerId, phone);

        settleTopup(customerId, 3_000L, prefix);

        assertThat(logCount("topup", phone)).isEqualTo(1);
        assertThat(logValue("topup", phone, "status")).isEqualTo("skipped_switch_off");
        assertThat(logValue("topup", phone, "reason")).isEqualTo("switch_off:sms_notification");
    }

    private CustomerStoredValueTransactionRecord settleTopup(
            String customerId,
            long amountCent,
            String prefix
    ) {
        String paymentIntentId = prefix + "_payment_intent";
        insertSucceededPaymentIntent(paymentIntentId, customerId, amountCent);
        // AUD-058：settleTopup 只认充值时绑定的卡范围，夹具需同步落绑定行（等价 topup 落库动作）。
        insertTopupIntentBinding(paymentIntentId, customerId);
        return storedValueService.settleTopup(customerId, PROJECT_ID, null, paymentIntentId);
    }

    private void insertTopupIntentBinding(String paymentIntentId, String customerId) {
        jdbcTemplate.update("""
                INSERT INTO stored_value_topup_intents (payment_intent_id, customer_id, project_id, merchant_id, created_at)
                VALUES (?, ?, ?, NULL, now())
                ON CONFLICT (payment_intent_id) DO UPDATE SET
                    customer_id = EXCLUDED.customer_id,
                    project_id = EXCLUDED.project_id,
                    merchant_id = EXCLUDED.merchant_id
                """, paymentIntentId, customerId, PROJECT_ID);
    }

    private void insertSucceededPaymentIntent(String id, String customerId, long amountCent) {
        jdbcTemplate.update("""
                INSERT INTO finance_payment_intents (
                    id, business_order_type, business_order_id, amount_cent, currency, channel,
                    status, created_at, updated_at
                )
                VALUES (?, 'stored_value_topup', ?, ?, 'CNY', 'wechat', 'succeeded', now(), now())
                ON CONFLICT (id) DO UPDATE SET
                    business_order_type = EXCLUDED.business_order_type,
                    business_order_id = EXCLUDED.business_order_id,
                    amount_cent = EXCLUDED.amount_cent,
                    currency = EXCLUDED.currency,
                    channel = EXCLUDED.channel,
                    status = EXCLUDED.status,
                    updated_at = now()
                """, id, customerId, amountCent);
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

    private void setSwitch(String capabilityCode, boolean enabled) {
        jdbcTemplate.update("""
                INSERT INTO terminal_capability_switches (
                    id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at
                )
                VALUES (?, '*', ?, 'global', 'global', ?, 'stored value notification test switch override', now(), now())
                ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO UPDATE SET
                    enabled = EXCLUDED.enabled,
                    reason = EXCLUDED.reason,
                    updated_at = now()
                """, idFor(capabilityCode), capabilityCode, enabled);
    }

    private void cleanupNotificationData() {
        // AUD-070：本用例产生的 customers / customer_stored_value_accounts /
        // customer_stored_value_transactions / finance_payment_intents 无法物理清理：
        // customer_stored_value_transactions 属 V140 append-only 不可变表（UPDATE/DELETE 触发器对 owner 也拦截，
        // 抛 COMPLEX_APPEND_ONLY_VIOLATION），且 customer→accounts→transactions 皆 ON DELETE CASCADE，
        // 删 customer/account 会级联触碰不可变账本致整事务回滚；finance_payment_intents 亦被 delete_only 触发器锁。
        // 用例已用每次随机 UUID 客户号 + 手机号前缀（15588…）自隔离，残留行属 append-only 留痕（按设计永久），
        // 故此处仅清理可删的 notification_send_log / idempotency_records。
        jdbcTemplate.update("""
                DELETE FROM idempotency_records
                 WHERE idempotency_key LIKE 'sv_notif_%'
                    OR idempotency_key IN (
                        SELECT request_id
                          FROM notification_send_log
                         WHERE domain = 'stored_value'
                           AND (recipient LIKE ?
                                OR recipient IN ('15500020001', '15500020002', '15500020003'))
                    )
                """, TEST_PHONE_PREFIX + "%");
        jdbcTemplate.update("""
                DELETE FROM notification_send_log
                 WHERE domain = 'stored_value'
                   AND (recipient LIKE ?
                        OR recipient IN ('15500020001', '15500020002', '15500020003'))
                """, TEST_PHONE_PREFIX + "%");
    }

    private String idFor(String capabilityCode) {
        return "test_tcs_" + capabilityCode.replace(".", "_");
    }

    private int logCount(String event, String recipient) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM notification_send_log
                 WHERE domain = 'stored_value'
                   AND event_key = ?
                   AND recipient = ?
                """, Integer.class, event, recipient);
        return count == null ? 0 : count;
    }

    private String logValue(String event, String recipient, String column) {
        return jdbcTemplate.queryForObject("""
                SELECT %s
                  FROM notification_send_log
                 WHERE domain = 'stored_value'
                   AND event_key = ?
                   AND recipient = ?
                 ORDER BY created_at DESC
                 LIMIT 1
                """.formatted(column), String.class, event, recipient);
    }

    private String logParam(String event, String recipient, String key) {
        return jdbcTemplate.queryForObject("""
                SELECT params ->> ?
                  FROM notification_send_log
                 WHERE domain = 'stored_value'
                   AND event_key = ?
                   AND recipient = ?
                 ORDER BY created_at DESC
                 LIMIT 1
                """, String.class, key, event, recipient);
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 7) {
            return phone.charAt(0) + "****" + phone.charAt(phone.length() - 1);
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String unique(String label) {
        return "sv_notif_" + label + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String uniquePhone() {
        String digits = UUID.randomUUID().toString().replaceAll("\\D", "");
        while (digits.length() < 6) {
            digits += "0";
        }
        return TEST_PHONE_PREFIX + digits.substring(0, 6);
    }
}
