package com.complex.platform.customer;

// 文件版本：V0.01

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

    /** 充值按项目赠送比例拆为本金与赠送金两段分别记账；余额 = 两段之和；流水记录拆分。 */
    @Test
    void topupSplitsPrincipalAndBonusByProjectPolicy() {
        storedValueService.configureBonusPolicy(projectId, 1000, 0, 2000, "测试赠送比例");
        topupViaCounter(projectId, null, 10_000L);

        Map<String, Object> account = account(projectId, null);
        assertThat(account.get("principal_cent")).isEqualTo(10_000L);
        assertThat(account.get("bonus_cent")).isEqualTo(1_000L);
        assertThat(account.get("balance_cent")).isEqualTo(11_000L);
        Map<String, Object> topup = singleTxn(account, "topup");
        assertThat(topup.get("amount_cent")).isEqualTo(11_000L);
        assertThat(topup.get("principal_cent")).isEqualTo(10_000L);
        assertThat(topup.get("bonus_cent")).isEqualTo(1_000L);
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
        storedValueService.configureBonusPolicy(projectId, 1000, 0, 2000, "测试赠送比例");
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

    /** 赠送比例须同时配置上下限且取值落在闭区间内。 */
    @Test
    void bonusPolicyRequiresBoundsAndValueWithinThem() {
        assertThatThrownBy(() -> storedValueService.configureBonusPolicy(projectId, 3000, 0, 2000, "越界"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> storedValueService.configureBonusPolicy(projectId, 1000, null, 2000, "缺下限"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("VALIDATION_FAILED");
        assertThat(customerRepository.findStoredValueBonusPolicy(projectId)).isEmpty();

        StoredValueBonusPolicyRecord saved = storedValueService.configureBonusPolicy(projectId, 1000, 500, 2000, "正常配置");
        assertThat(saved.bonusBps()).isEqualTo(1000);
        assertThat(jdbcTemplate.queryForMap("SELECT bonus_bps, min_bps, max_bps FROM stored_value_bonus_policies WHERE project_id = ?",
                projectId)).containsEntry("bonus_bps", 1000).containsEntry("min_bps", 500).containsEntry("max_bps", 2000);
    }

    // ---------- helpers ----------

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
                "SELECT amount_cent, principal_cent, bonus_cent FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = ?",
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
