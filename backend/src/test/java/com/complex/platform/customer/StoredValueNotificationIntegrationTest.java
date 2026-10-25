package com.complex.platform.customer;

// 文件版本：V0.08

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class StoredValueNotificationIntegrationTest {

    private static final String TEST_PHONE_PREFIX = "15588";
    private static final String PROJECT_ID = "sv_notif_project";

    @Autowired
    private StoredValueService storedValueService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private com.complex.platform.common.crypto.PiiCipher piiCipher;

    @Autowired
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor("usr_master_authority", "master-authority",
                                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"), null, java.util.List.of()));
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

        CustomerStoredValueTransactionRecord txn = settleTopup(customerId, 5_000L, prefix, null);

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
        String merchantId = prefix + "_merchant";
        settleTopup(customerId, 8_000L, prefix, merchantId);
        TestSecurityActors.asMerchantUser(merchantId);

        CustomerStoredValueTransactionRecord txn = storedValueService.consumeInternal(
                customerId,
                null,
                merchantId,
                2_300L,
                "notification test consume",
                prefix + "_consume");

        assertThat(logCount("consume", phone)).isEqualTo(1);
        assertThat(logValue("consume", phone, "request_id")).isEqualTo("svconsume:" + txn.id());
        assertThat(logValue("consume", phone, "template_code")).isEqualTo("sms.stored_value.consume");
        assertThat(logValue("consume", phone, "status")).isEqualTo("sent");
        assertThat(logParam("consume", phone, "amount_cent")).isEqualTo("2300");
        assertThat(logParam("consume", phone, "balance_after_cent")).isEqualTo("5700");
        assertThat(logParam("consume", phone, "card_type")).isEqualTo("merchant");
    }

    /**
     * TK-21-R1 / 复核 html m11（BSM L833「系统自动生成应退本金事项并通知发卡主体获授权人员」）：拉黑后商户卡待退事项生成，事务提交后
     * 短信通知发卡商户内持 customer.stored_value.refund 权限的员工（员工档案手机号）；无该权限的员工、商户档案联系电话、客人本人都不收。
     */
    @Test
    void accountClosureWithMerchantCardNotifiesAuthorizedIssuingMerchantStaff() {
        String prefix = unique("pending");
        String customerId = prefix + "_customer";
        String merchantId = prefix + "_merchant";
        String cardId = prefix + "_card";
        String customerPhone = uniquePhone();
        String merchantPhone = uniquePhone();
        String staffPhone = uniquePhone();
        String otherStaffPhone = uniquePhone();
        insertCustomer(customerId, customerPhone);
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, contact_phone, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, 'active', now(), now())", merchantId, "待退通知商户" + prefix, piiCipher.encrypt(merchantPhone));
        merchantStaff(prefix + "_u1", merchantId, staffPhone, true);
        merchantStaff(prefix + "_u2", merchantId, otherStaffPhone, false);
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                cardId, customerId, null, merchantId, 2_000L, "active", java.time.Instant.now(), java.time.Instant.now(),
                "merchant_prepaid", 2_000L, 0L));
        customerRepository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId, true, 10_000, true, "merchant_prepaid", java.time.Instant.now(), java.time.Instant.now()));

        TestSecurityActors.asPlatformAdmin();
        customerService.blacklistCustomer(customerId, "notification test blacklist");

        assertThat(logCount("pending_refund", staffPhone)).isEqualTo(1);
        assertThat(logValue("pending_refund", staffPhone, "request_id")).isEqualTo("svpend:" + cardId + ":" + prefix + "_u1");
        assertThat(logValue("pending_refund", staffPhone, "template_code")).isEqualTo("sms.stored_value.pending_refund");
        assertThat(logParam("pending_refund", staffPhone, "account_id")).isEqualTo(cardId);
        assertThat(logParam("pending_refund", staffPhone, "payout_cent")).isEqualTo("2000");
        assertThat(logParam("pending_refund", staffPhone, "customer_phone")).isEqualTo(maskPhone(customerPhone));
        assertThat(logCount("pending_refund", otherStaffPhone)).isZero();
        assertThat(logCount("pending_refund", merchantPhone)).isZero();
        assertThat(logCount("pending_refund", customerPhone)).isZero();
    }

    private void merchantStaff(String userId, String merchantId, String phone, boolean withRefundPermission) {
        jdbcTemplate.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff','org_1',?,'active',now())",
                userId, userId, merchantId);
        jdbcTemplate.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,phone,registration_status,status,created_at,updated_at) VALUES(?,?,?,?,?,?,'approved','active',now(),now())",
                "sp_" + userId, "org_1", merchantId, userId, "员工" + userId, phone);
        if (withRefundPermission) {
            String role = "role_" + userId;
            jdbcTemplate.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'待退通知','project','active',now())",
                    role, "待退通知员工角色" + System.nanoTime());
            jdbcTemplate.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'customer.stored_value.refund')", role);
            jdbcTemplate.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        }
    }

    /**
     * OBS-13-04（BSM 第 1598 行「涉及客人金额发生变化的通知（储值消费扣款、储值充值到账……）」不在可关闭范围内，不得按类或按条关闭）：
     * 总开关、储值域开关、充值与扣款事件开关全关，充值到账与消费扣款照发。
     */
    @Test
    void topupAndConsumeStillSendWhenMainDomainAndEventSwitchesAllOff() {
        setSwitch("sms_notification", false);
        setSwitch("sms.notify.stored_value", false);
        setSwitch("sms.notify.stored_value.topup", false);
        setSwitch("sms.notify.stored_value.consume", false);
        String prefix = unique("off");
        String customerId = prefix + "_customer";
        String phone = uniquePhone();
        insertCustomer(customerId, phone);
        String merchantId = prefix + "_merchant";

        settleTopup(customerId, 3_000L, prefix, merchantId);
        TestSecurityActors.asMerchantUser(merchantId);
        storedValueService.consumeInternal(customerId, null, merchantId, 1_000L, "不可关闭扣款通知", prefix + "_consume");

        assertThat(logCount("topup", phone)).isEqualTo(1);
        assertThat(logValue("topup", phone, "status")).isEqualTo("sent");
        assertThat(logCount("consume", phone)).isEqualTo(1);
        assertThat(logValue("consume", phone, "status")).isEqualTo("sent");
    }

    @Test
    void projectCardInternalConsumeWithoutMerchantScopeRemainsRejected() {
        String prefix = unique("scope");
        String customerId = prefix + "_customer";
        String phone = uniquePhone();
        insertCustomer(customerId, phone);
        settleTopup(customerId, 5_000L, prefix, null);
        assertThatThrownBy(() -> storedValueService.consumeInternal(customerId, PROJECT_ID, null,
                1_000L, "无商户范围项目卡扣款", prefix + "_consume"))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MERCHANT_SCOPE_REQUIRED"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT balance_cent FROM customer_stored_value_accounts WHERE customer_id = ? AND project_id = ? AND merchant_id IS NULL",
                Long.class, customerId, PROJECT_ID)).isEqualTo(5_000L);
        assertThat(logCount("consume", phone)).isZero();
    }

    private CustomerStoredValueTransactionRecord settleTopup(
            String customerId,
            long amountCent,
            String prefix,
            String merchantId
    ) {
        String paymentIntentId = prefix + "_payment_intent";
        insertCreatedPaymentIntent(paymentIntentId, customerId, amountCent);
        // AUD-058：settleTopup 只认充值时绑定的卡范围，夹具需同步落绑定行（等价 topup 落库动作）。
        insertTopupIntentBinding(paymentIntentId, customerId, merchantId);
        storedValueService.confirmOfflineTopupAndSettle(paymentIntentId,
                new com.complex.platform.finance.dto.OfflinePaymentConfirmRequest(
                        prefix + "_pos_confirm", null, null, paymentIntentId, amountCent, "pos", java.time.Instant.now()));
        return customerRepository.findStoredValueTransactionByIdempotencyKey("sv_settle_" + paymentIntentId).orElseThrow();
    }

    private void insertTopupIntentBinding(String paymentIntentId, String customerId, String merchantId) {
        jdbcTemplate.update("""
                INSERT INTO stored_value_topup_intents (payment_intent_id, customer_id, project_id, merchant_id, created_at)
                VALUES (?, ?, ?, ?, now())
                ON CONFLICT (payment_intent_id) DO UPDATE SET
                    customer_id = EXCLUDED.customer_id,
                    project_id = EXCLUDED.project_id,
                    merchant_id = EXCLUDED.merchant_id
                """, paymentIntentId, customerId, merchantId == null ? PROJECT_ID : null, merchantId);
    }

    private void insertCreatedPaymentIntent(String id, String customerId, long amountCent) {
        jdbcTemplate.update("""
                INSERT INTO finance_payment_intents (
                    id, business_order_type, business_order_id, amount_cent, currency, channel,
                    status, created_at, updated_at
                )
                VALUES (?, 'stored_value_topup', ?, ?, 'CNY', 'pos', 'created', now(), now())
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
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE 'sv_notif_%'");
        // 收件人手机号落密文（V547）：取出解密后按测试手机号筛。
        com.complex.platform.support.NotificationSendLogRows.delete(jdbcTemplate, com.complex.platform.support.NotificationSendLogRows.ids(
                jdbcTemplate, piiCipher, "stored_value", null,
                r -> r.startsWith(TEST_PHONE_PREFIX) || java.util.Set.of("15500020001", "15500020002", "15500020003").contains(r)));
    }

    private String idFor(String capabilityCode) {
        return "test_tcs_" + capabilityCode.replace(".", "_");
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
        return com.complex.platform.support.NotificationSendLogRows.ids(jdbcTemplate, piiCipher, "stored_value", event, recipient::equals);
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
