package com.complex.platform.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.access.AccessActor;
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
 * CG-06b 关闭客人账户（列入黑名单）后储值本金退还本人（BSM「核销单位」：「关闭该客人账户、该账号作废，并将其列入黑名单不允许再进入；
 * 关闭后该账户名下的储值本金退还本人，其余比照本文件「账户注销」条处理」；BSM「储值」：「出口只有本人消费、退卡、
 * 账户注销、账户关闭四条……账户关闭的，储值本金退还本人」「综合体通用卡的未消费本金默认全额可退，赠送金不退现、未使用部分按活动规则作废或收回」；
 * ENUM「储值卡类型与收款主体」：「商户级卡是否可退、退多少比例由发卡商户自设规则」）。
 *
 * <p>走真实 Flyway 迁移库，{@code @Transactional} 每用例回滚；拉黑走正路 {@code CustomerService.blacklistCustomer}，
 * 追 {@code customers.status}、{@code customer_stored_value_accounts.principal_cent / bonus_cent / status}、
 * {@code customer_stored_value_transactions}、{@code finance_refunds} 列。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class CustomerBlacklistStoredValueRefundJdbcIntegrationTests {

    @Autowired private CustomerService customerService;
    @Autowired private StoredValueService storedValueService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String customerId;
    private String projectId;
    private String merchantId;
    private String generalCardId;
    private String merchantCardId;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        customerId = "cust_cg06b_" + suffix;
        projectId = "prj_cg06b_" + suffix;
        merchantId = "mch_cg06b_" + suffix;
        generalCardId = "svacc_cg06b_g_" + suffix;
        merchantCardId = "svacc_cg06b_m_" + suffix;
        Instant now = Instant.now();
        String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        customerRepository.saveCustomer(new CustomerRecord(
                customerId, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        // 综合体通用卡：本金 3000 / 赠送金 500；商户级卡（商户预收）：本金 2000。
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                generalCardId, customerId, projectId, null, 3_500L, "active", now, now, null, 3_000L, 500L));
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                merchantCardId, customerId, null, merchantId, 2_000L, "active", now, now, "merchant_prepaid", 2_000L, 0L));
        customerRepository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId, true, 10_000, true, "merchant_prepaid", now, now));
        authenticateWithAccountAndStoredValuePermissions();
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 拉黑后通用卡未消费本金全额生成退款、赠送金作废留痕、卡置 closed；商户级卡余额与状态不变。 */
    @Test
    void blacklistRefundsGeneralCardPrincipalVoidsBonusAndClosesCard() {
        customerService.blacklistCustomer(customerId, "查实将票借给他人使用");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, customerId))
                .isEqualTo("blacklisted");
        Map<String, Object> general = account(generalCardId);
        assertThat(general.get("principal_cent")).isEqualTo(0L);
        assertThat(general.get("bonus_cent")).isEqualTo(0L);
        assertThat(general.get("balance_cent")).isEqualTo(0L);
        assertThat(general.get("status")).isEqualTo("closed");
        Map<String, Object> refund = singleTxn(generalCardId, "refund");
        assertThat(refund.get("amount_cent")).isEqualTo(-3_000L);
        assertThat(refund.get("principal_cent")).isEqualTo(-3_000L);
        assertThat(refund.get("bonus_cent")).isEqualTo(0L);
        Map<String, Object> voided = singleTxn(generalCardId, "adjust");
        assertThat(voided.get("amount_cent")).isEqualTo(-500L);
        assertThat(voided.get("bonus_cent")).isEqualTo(-500L);
        // 退款打款登记：长沙项目公司（通用卡余额管理与退款主体）名下待兑付的退款台账。
        List<Map<String, Object>> payouts = jdbcTemplate.queryForList(
                "SELECT refund_amount_cent, status FROM finance_refunds WHERE target_id = ?", generalCardId);
        assertThat(payouts).hasSize(1);
        assertThat(payouts.get(0).get("refund_amount_cent")).isEqualTo(3_000L);

        Map<String, Object> merchant = account(merchantCardId);
        assertThat(merchant.get("principal_cent")).isEqualTo(2_000L);
        assertThat(merchant.get("balance_cent")).isEqualTo(2_000L);
        assertThat(merchant.get("status")).isEqualTo("active");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_stored_value_transactions WHERE account_id = ?", Integer.class, merchantCardId))
                .isZero();
    }

    /** 商户级卡不自动退款，由发卡商户按自设规则到店办理：既有柜台退卡路径在拉黑后仍可办。 */
    @Test
    void merchantCardCounterRefundRemainsAvailableAfterBlacklist() {
        customerService.blacklistCustomer(customerId, "查实将票借给他人使用");

        storedValueService.counterRefund(customerId, null, merchantId, 2_000L, "到店退卡", true,
                "凭证-" + suffix, "cg06b-counter-" + suffix);

        Map<String, Object> merchant = account(merchantCardId);
        assertThat(merchant.get("principal_cent")).isEqualTo(0L);
        assertThat(merchant.get("status")).isEqualTo("closed");
        assertThat(singleTxn(merchantCardId, "refund").get("amount_cent")).isEqualTo(-2_000L);
    }

    /** 重复关闭不重复退：第二次拉黑不再生成退款流水与打款登记。 */
    @Test
    void repeatedBlacklistDoesNotRefundTwice() {
        customerService.blacklistCustomer(customerId, "查实将票借给他人使用");
        customerService.blacklistCustomer(customerId, "重复提交");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = 'refund'",
                Integer.class, generalCardId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM finance_refunds WHERE target_id = ?", Integer.class, generalCardId)).isEqualTo(1);
        assertThat(account(generalCardId).get("status")).isEqualTo("closed");
    }

    // ---------- helpers ----------

    private Map<String, Object> account(String id) {
        return jdbcTemplate.queryForMap(
                "SELECT principal_cent, bonus_cent, balance_cent, status FROM customer_stored_value_accounts WHERE id = ?", id);
    }

    private Map<String, Object> singleTxn(String accountId, String type) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT amount_cent, principal_cent, bonus_cent FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = ?",
                accountId, type);
        assertThat(rows).as("txn_type=" + type).hasSize(1);
        return rows.get(0);
    }

    private void authenticateWithAccountAndStoredValuePermissions() {
        String userId = "usr_cg06b_" + suffix;
        String roleId = "role_cg06b_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_cg06b_" + suffix);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId,
                "账户关闭退本金测试角色" + System.nanoTime());
        for (String code : List.of("customer.account.manage", "customer.stored_value.refund")) {
            jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        AccessActor actor = new AccessActor(userId, "u_cg06b_" + suffix, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }
}
