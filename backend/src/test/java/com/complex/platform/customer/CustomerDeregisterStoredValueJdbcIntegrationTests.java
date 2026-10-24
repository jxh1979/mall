package com.complex.platform.customer;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * TK-21-R3 客人自助注销（BSM「账户注销」：「发起注销时系统须提醒其名下尚有……储值卡余额，客人可以选择……放弃余额，径行注销；
 * 放弃的储值卡余额归发卡主体（通用卡归长沙项目公司，商户级卡归发卡商户）。需要退还本金的注销事项按本文件「储值」条自动生成待退事项、
 * 通知发卡主体执行，并跟踪至实际退款成功；已明确确认放弃余额的仍按上述例外办理」；ENUM「储值卡类型与收款主体」：
 * 「不扩大为禁止会员按既有规则明确放弃余额后注销」）。
 *
 * <p>走真实接口（客人 token）与真实迁移库，{@code @Transactional} 每用例回滚；请求体按前端真实形状（蛇形键 waive_balance）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerDeregisterStoredValueJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String customerId;
    private String token;
    private String merchantId;
    private String generalCardId;
    private String merchantCardId;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        customerId = "cust_dereg_" + suffix;
        token = "cust_token_dereg_" + suffix;
        merchantId = "mch_dereg_" + suffix;
        generalCardId = "svacc_dereg_g_" + suffix;
        merchantCardId = "svacc_dereg_m_" + suffix;
        Instant now = Instant.now();
        String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        customerRepository.saveCustomer(new CustomerRecord(
                customerId, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        customerRepository.saveSession(new CustomerSessionRecord(
                "sess_dereg_" + suffix, customerId, token, "miniapp", now, now.plusSeconds(3600), "active"));
        // 综合体通用卡：本金 3000 / 赠送金 500；商户级卡（商户预收、规则全额可退）：本金 2000。
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                generalCardId, customerId, "prj_dereg_" + suffix, null, 3_500L, "active", now, now, null, 3_000L, 500L));
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                merchantCardId, customerId, null, merchantId, 2_000L, "active", now, now, "merchant_prepaid", 2_000L, 0L));
        customerRepository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId, true, 10_000, true, "merchant_prepaid", now, now));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 注销预览按卡分列本金与赠送金。 */
    @Test
    void previewListsPrincipalAndBonusPerCard() throws Exception {
        mockMvc.perform(get("/api/v1/customer/deregister/preview")
                        .header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", token))
                .andExpect(status().isOk())
                // ROOT-03-R21：预览为 {stored_value_cards, saved_bottles}。
                .andExpect(jsonPath("$.data.stored_value_cards.length()").value(2))
                .andExpect(jsonPath("$.data.stored_value_cards[?(@.account_id == '" + generalCardId + "')].principal_cent", Matchers.contains(3000)))
                .andExpect(jsonPath("$.data.stored_value_cards[?(@.account_id == '" + generalCardId + "')].bonus_cent", Matchers.contains(500)))
                .andExpect(jsonPath("$.data.stored_value_cards[?(@.account_id == '" + generalCardId + "')].card_scope", Matchers.contains("project")))
                .andExpect(jsonPath("$.data.stored_value_cards[?(@.account_id == '" + merchantCardId + "')].principal_cent", Matchers.contains(2000)))
                .andExpect(jsonPath("$.data.stored_value_cards[?(@.account_id == '" + merchantCardId + "')].merchant_id", Matchers.contains(merchantId)))
                .andExpect(jsonPath("$.data.saved_bottles.length()").value(0));
    }

    /** 不放弃余额：通用卡本金登记待退、商户卡按规则登记待退（卡冻结），赠送金作废，账号置已注销。 */
    @Test
    void deregisterWithoutWaiveCreatesPendingRefundsForGeneralAndMerchantCards() throws Exception {
        deregister("{\"waive_balance\": false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("deregistered"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, customerId))
                .isEqualTo("deregistered");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT refund_amount_cent FROM finance_refunds WHERE target_id = ? AND status = 'requested'", Long.class, generalCardId))
                .isEqualTo(3_000L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT refund_amount_cent FROM finance_refunds WHERE target_id = ? AND status = 'requested'", Long.class, merchantCardId))
                .isEqualTo(2_000L);
        Map<String, Object> general = account(generalCardId);
        assertThat(general.get("status")).isEqualTo("closed");
        assertThat(general.get("balance_cent")).isEqualTo(0L);
        assertThat(singleTxn(generalCardId, "adjust").get("bonus_cent")).isEqualTo(-500L);
        Map<String, Object> merchant = account(merchantCardId);
        assertThat(merchant.get("status")).isEqualTo("frozen");
        assertThat(merchant.get("principal_cent")).isEqualTo(2_000L);
    }

    /** 明确放弃余额：各卡本金与赠送金作废留痕、卡 closed，不生成任何待退事项。 */
    @Test
    void deregisterWaivingBalanceVoidsBothSegmentsAndClosesCards() throws Exception {
        deregister("{\"waive_balance\": true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("deregistered"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM finance_refunds WHERE target_id IN (?, ?)", Integer.class, generalCardId, merchantCardId)).isZero();
        Map<String, Object> general = account(generalCardId);
        assertThat(general.get("status")).isEqualTo("closed");
        assertThat(general.get("principal_cent")).isEqualTo(0L);
        assertThat(general.get("bonus_cent")).isEqualTo(0L);
        Map<String, Object> waivedGeneral = singleTxn(generalCardId, "adjust");
        assertThat(waivedGeneral.get("amount_cent")).isEqualTo(-3_500L);
        assertThat(waivedGeneral.get("principal_cent")).isEqualTo(-3_000L);
        assertThat(waivedGeneral.get("bonus_cent")).isEqualTo(-500L);
        Map<String, Object> merchant = account(merchantCardId);
        assertThat(merchant.get("status")).isEqualTo("closed");
        assertThat(merchant.get("balance_cent")).isEqualTo(0L);
        assertThat(singleTxn(merchantCardId, "adjust").get("principal_cent")).isEqualTo(-2_000L);
    }

    /** 无余额：直接注销，不写流水、不生成事项（老客户端无请求体也可）。 */
    @Test
    void deregisterWithNoBalanceJustChangesStatus() throws Exception {
        jdbcTemplate.update("UPDATE customer_stored_value_accounts SET principal_cent = 0, bonus_cent = 0, balance_cent = 0 WHERE customer_id = ?", customerId);

        mockMvc.perform(post("/api/v1/customer/deregister")
                        .header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("deregistered"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_stored_value_transactions WHERE account_id IN (?, ?)", Integer.class, generalCardId, merchantCardId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM finance_refunds WHERE target_id IN (?, ?)", Integer.class, generalCardId, merchantCardId)).isZero();
        assertThat(account(generalCardId).get("status")).isEqualTo("closed");
        assertThat(account(merchantCardId).get("status")).isEqualTo("closed");
    }

    // ---------- helpers ----------

    private ResultActions deregister(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/customer/deregister")
                .header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

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
}
