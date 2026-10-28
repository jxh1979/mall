package com.complex.platform.customer;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-06a 储值本金 / 赠送金两段记账（ENUM「储值卡类型与收款主体」：「每笔充值拆为本金与赠送金两段分别记账，赠送比例可配……
 * 消费扣款顺序写死为先扣本金、后扣赠送金……仅商户级卡例外……退卡只退未消费的本金，赠送金不退现……销户时……再退剩余本金，赠送金清零」；
 * BSM「我的余额」：「含本金与赠送金分列……每笔充值……本金与赠送金拆分……每笔扣款……扣减的本金与赠送金各多少」；
 * BSM「支付方式」：「商户级卡消费扣款顺序由发卡商户自定、由平台运营代为录入后台」）。
 *
 * <p>第十八批第 11 条：赠送比例取 ticket_reward_config_versions 中已审批、已生效的版本（通用卡 stored_value_bonus 按项目，
 * 商户级卡 merchant_card_bonus 按发卡商户；ENUM L86、BSM L521），充值流水记下所用版本 bonus_config_version_id。
 *
 * <p>走真实 Flyway 迁移库，{@code @Transactional} 每用例回滚；充值走正路 topup → 柜台确认入账，消费 / 退卡走正路服务方法，
 * 追 {@code customer_stored_value_accounts.principal_cent / bonus_cent / balance_cent / status} 与
 * {@code customer_stored_value_transactions.principal_cent / bonus_cent / amount_cent / txn_type} 列。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class StoredValuePrincipalBonusJdbcIntegrationTests {

    @Autowired private StoredValueService storedValueService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private com.complex.platform.finance.FinanceRepository financeRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String customerId;
    private String projectId;
    private String merchantId;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        customerId = "cust_cg06_" + suffix;
        projectId = "prj_cg06_" + suffix;
        merchantId = "mch_cg06_" + suffix;
        Instant now = Instant.now();
        String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        customerRepository.saveCustomer(new CustomerRecord(
                customerId, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        authenticateWithStoredValuePermissions();
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 充值按项目已生效的赠送比例版本拆为本金与赠送金两段分别记账；余额 = 两段之和；流水记录拆分并锁定所用版本。 */
    @Test
    void topupSplitsPrincipalAndBonusByProjectPolicy() {
        String version = bonusVersion("stored_value_bonus", projectId, null, 1000, "approved", 1);
        assertThat(storedValueService.topupRules(projectId, null).bonusBps()).isEqualTo(1000);
        topupViaCounter(projectId, null, 10_000L);

        Map<String, Object> account = account(projectId, null);
        assertThat(account.get("principal_cent")).isEqualTo(10_000L);
        assertThat(account.get("bonus_cent")).isEqualTo(1_000L);
        assertThat(account.get("balance_cent")).isEqualTo(11_000L);
        Map<String, Object> topup = singleTxn(account, "topup");
        assertThat(topup.get("amount_cent")).isEqualTo(11_000L);
        assertThat(topup.get("principal_cent")).isEqualTo(10_000L);
        assertThat(topup.get("bonus_cent")).isEqualTo(1_000L);
        assertThat(topup.get("bonus_config_version_id")).isEqualTo(version);
    }

    /** 第十八批第 11 条（ENUM L86「商户级卡同样可以设赠送，每笔充值一样拆为本金与赠送金两段分别记账」）：商户级卡按发卡商户已审批生效的版本赠送并锁版本。 */
    @Test
    void merchantCardTopupUsesApprovedEffectiveBonusVersion() {
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)"
                + " VALUES (?, '赠送测试商户', 'catering', 'active', now(), now())", merchantId);
        Instant now = Instant.now();
        customerRepository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId, true, 10_000, true, "merchant_prepaid", now, now));
        String version = bonusVersion("merchant_card_bonus", null, merchantId, 1000, "approved", 1);

        topupViaCounter(null, merchantId, 10_000L);

        Map<String, Object> account = account(null, merchantId);
        assertThat(account.get("principal_cent")).isEqualTo(10_000L);
        assertThat(account.get("bonus_cent")).isEqualTo(1_000L);
        Map<String, Object> topup = singleTxn(account, "topup");
        assertThat(topup.get("bonus_cent")).isEqualTo(1_000L);
        assertThat(topup.get("bonus_config_version_id")).isEqualTo(version);
        assertThat(storedValueService.topupRules(null, merchantId).bonusBps()).isEqualTo(1000);
    }

    /** BSM L521「经高敏权限“转正”审批通过后按指定日期生效」：待审批、生效后才批准、未到生效时间的版本都不赠送。 */
    @Test
    void unapprovedLateApprovedOrFutureVersionsGrantNoBonus() {
        bonusVersion("stored_value_bonus", projectId, null, 1000, "pending_review", 1);
        bonusVersion("stored_value_bonus", projectId, null, 2000, "approved_late", 2);
        bonusVersion("stored_value_bonus", projectId, null, 3000, "future", 3);

        topupViaCounter(projectId, null, 10_000L);

        Map<String, Object> topup = singleTxn(account(projectId, null), "topup");
        assertThat(topup.get("bonus_cent")).isEqualTo(0L);
        assertThat(topup.get("bonus_config_version_id")).isNull();
        assertThat(storedValueService.topupRules(projectId, null).bonusBps()).isZero();
    }

    /** 未配置赠送比例的项目不赠送（本金 = 实付，赠送金 0）。 */
    @Test
    void topupWithoutPolicyHasNoBonus() {
        topupViaCounter(projectId, null, 5_000L);
        Map<String, Object> account = account(projectId, null);
        assertThat(account.get("principal_cent")).isEqualTo(5_000L);
        assertThat(account.get("bonus_cent")).isEqualTo(0L);
        assertThat(account.get("balance_cent")).isEqualTo(5_000L);
    }

    /** 通用卡消费先扣本金、后扣赠送金；流水记录扣减的本金与赠送金各多少。 */
    @Test
    void consumeDeductsPrincipalBeforeBonusOnGeneralCard() {
        bonusVersion("stored_value_bonus", projectId, null, 1000, "approved", 1);
        topupViaCounter(projectId, null, 10_000L);

        storedValueService.consumeInternal(customerId, projectId, null, 10_500L, "测试消费", "cg06-consume-" + suffix);

        Map<String, Object> account = account(projectId, null);
        assertThat(account.get("principal_cent")).isEqualTo(0L);
        assertThat(account.get("bonus_cent")).isEqualTo(500L);
        assertThat(account.get("balance_cent")).isEqualTo(500L);
        Map<String, Object> consume = singleTxn(account, "consume");
        assertThat(consume.get("amount_cent")).isEqualTo(-10_500L);
        assertThat(consume.get("principal_cent")).isEqualTo(-10_000L);
        assertThat(consume.get("bonus_cent")).isEqualTo(-500L);
    }

    /** 商户级卡按发卡商户自定（平台代录）的扣款顺序扣减：bonus_first 时先扣赠送金。 */
    @Test
    void merchantCardFollowsConfiguredConsumeOrder() {
        Instant now = Instant.now();
        customerRepository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId, true, 10_000, true, "merchant_prepaid", now, now,
                StoredValueMerchantRefundRuleRecord.CONSUME_BONUS_FIRST));
        seedAccount("svacc_cg06m_" + suffix, null, merchantId, 3_000L, 500L, "merchant_prepaid");

        storedValueService.consumeInternal(customerId, null, merchantId, 400L, "商户卡测试消费", "cg06-mconsume-" + suffix);

        Map<String, Object> account = account(null, merchantId);
        assertThat(account.get("principal_cent")).isEqualTo(3_000L);
        assertThat(account.get("bonus_cent")).isEqualTo(100L);
        assertThat(account.get("balance_cent")).isEqualTo(3_100L);
        Map<String, Object> consume = singleTxn(account, "consume");
        assertThat(consume.get("principal_cent")).isEqualTo(0L);
        assertThat(consume.get("bonus_cent")).isEqualTo(-400L);
    }

    /** 操作员裁决 2026-10-20 第三轮第 7 条「退卡即全退并关卡」：通用卡部分退 400；全退（即使未勾销卡）一次退清本金、赠送金清零、卡关闭。 */
    @Test
    void generalCardRefundIsAlwaysFullAndClosesCard() {
        seedAccount("svacc_cg06f_" + suffix, projectId, null, 3_000L, 500L, null);

        assertThatThrownBy(() -> storedValueService.refund(customerId, projectId, null, 1_000L, "退一部分", false,
                "cg06-refund-part-" + suffix))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("STORED_VALUE_GENERAL_CARD_FULL_REFUND_ONLY");
        assertThat(account(projectId, null).get("balance_cent")).isEqualTo(3_500L);

        storedValueService.refund(customerId, projectId, null, 3_000L, "退卡", false, "cg06-refund-full-" + suffix);

        Map<String, Object> account = account(projectId, null);
        assertThat(account.get("principal_cent")).isEqualTo(0L);
        assertThat(account.get("bonus_cent")).isEqualTo(0L);
        assertThat(account.get("status")).isEqualTo("closed");
    }

    /** 退卡只退未消费的本金：超过本金被拒；销户退剩余本金后赠送金清零、卡置 closed，流水分别留痕。 */
    @Test
    void refundReturnsPrincipalOnlyAndClosingVoidsBonus() {
        seedAccount("svacc_cg06g_" + suffix, projectId, null, 3_000L, 500L, null);

        assertThatThrownBy(() -> storedValueService.refund(customerId, projectId, null, 3_500L, "退卡", false,
                "cg06-refund-over-" + suffix))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("STORED_VALUE_PRINCIPAL_INSUFFICIENT");
        assertThat(account(projectId, null).get("balance_cent")).isEqualTo(3_500L);

        storedValueService.refund(customerId, projectId, null, 3_000L, "销户退本金", true, "cg06-refund-close-" + suffix);

        Map<String, Object> account = account(projectId, null);
        assertThat(account.get("principal_cent")).isEqualTo(0L);
        assertThat(account.get("bonus_cent")).isEqualTo(0L);
        assertThat(account.get("balance_cent")).isEqualTo(0L);
        assertThat(account.get("status")).isEqualTo("closed");
        Map<String, Object> refund = singleTxn(account, "refund");
        assertThat(refund.get("amount_cent")).isEqualTo(-3_000L);
        assertThat(refund.get("principal_cent")).isEqualTo(-3_000L);
        assertThat(refund.get("bonus_cent")).isEqualTo(0L);
        Map<String, Object> voided = singleTxn(account, "adjust");
        assertThat(voided.get("amount_cent")).isEqualTo(-500L);
        assertThat(voided.get("principal_cent")).isEqualTo(0L);
        assertThat(voided.get("bonus_cent")).isEqualTo(-500L);
    }

    // ---------- helpers ----------

    /**
     * 直插一条赠送比例版本（payload 与 TicketRewardConfigService 写入同形：驼峰 fixedValue/minimumValue/maximumValue）。
     * state：approved＝已批准且已生效；pending_review＝待审批；approved_late＝生效时间过后才批准；future＝已批准但未到生效时间。
     */
    private String bonusVersion(String kind, String project, String merchant, int bps, String state, int seq) {
        String id = "trc_cg06_" + UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        Instant effective = "future".equals(state) ? now.plusSeconds(3600) : now.minusSeconds(600 + seq);
        Instant approved = "approved_late".equals(state) ? effective.plusSeconds(60) : effective.minusSeconds(60);
        jdbcTemplate.update("INSERT INTO ticket_reward_config_versions(id,kind,project_id,merchant_id,version,payload,effective_at,status,created_by,created_at,approved_at)"
                        + " VALUES(?,?,?,?,?,?::jsonb,?,?,'test',now(),?)",
                id, kind, project, merchant, "V0.0" + seq, "{\"fixedValue\":" + bps + ",\"minimumValue\":0,\"maximumValue\":10000}",
                java.sql.Timestamp.from(effective), "pending_review".equals(state) ? "pending_review" : "approved",
                "pending_review".equals(state) ? null : java.sql.Timestamp.from(approved));
        return id;
    }

    /** 柜台 POS 充值：支付意图（pos 通路）+ 卡范围绑定直接落库，再走正路柜台确认入账（与 M006 柜台充值用例同范式）。 */
    private void topupViaCounter(String project, String merchant, long amountCent) {
        Instant now = Instant.now();
        String paymentId = "pay_cg06_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        financeRepository.savePaymentIntent(new PaymentIntentRecord(paymentId,
                merchant == null ? "stored_value_topup" : "stored_value_topup_merchant", customerId, amountCent,
                "CNY", "pos", "created", now, now));
        customerRepository.bindStoredValueTopupIntent(paymentId, customerId, project, merchant, now);
        storedValueService.confirmOfflineTopupAndSettle(paymentId, new OfflinePaymentConfirmRequest(
                "cg06-confirm-" + UUID.randomUUID(), null, List.of(), paymentId, amountCent, "pos", now));
    }

    private void seedAccount(String id, String project, String merchant, long principal, long bonus, String fundOwnership) {
        Instant now = Instant.now();
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                id, customerId, project, merchant, principal + bonus, "active", now, now, fundOwnership, principal, bonus));
    }

    private Map<String, Object> account(String project, String merchant) {
        return jdbcTemplate.queryForMap("SELECT id, principal_cent, bonus_cent, balance_cent, status FROM customer_stored_value_accounts"
                + " WHERE customer_id = ? AND project_id IS NOT DISTINCT FROM ? AND merchant_id IS NOT DISTINCT FROM ?",
                customerId, project, merchant);
    }

    private Map<String, Object> singleTxn(Map<String, Object> account, String type) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT amount_cent, principal_cent, bonus_cent, bonus_config_version_id FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = ?",
                account.get("id"), type);
        assertThat(rows).as("txn_type=" + type).hasSize(1);
        return rows.get(0);
    }

    private void authenticateWithStoredValuePermissions() {
        String userId = "usr_cg06_" + suffix;
        String roleId = "role_cg06_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_cg06_" + suffix);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId,
                "储值两段记账测试角色" + System.nanoTime());
        for (String code : List.of("customer.stored_value.topup", "customer.stored_value.consume",
                "customer.stored_value.refund", "customer.stored_value.view", "finance.payment.register")) {
            jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        AccessActor actor = new AccessActor(userId, "u_cg06_" + suffix, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }
}
