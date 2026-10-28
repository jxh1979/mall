package com.complex.platform.customer;

// 文件版本：V0.02

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03-R30 我的余额逐笔、充值前规则告知、我的存酒所属酒吧：BSM 第 868 行「每笔充值（时间、金额、本金与赠送金拆分、支付通路）、每笔扣款（商户、扣减的
 * 本金与赠送金各多少）」；ENUM 第 86 行「储值卡的消费扣款顺序、赠送金的性质与退卡口径，须在客人充值之前告知」；BSM 第 867 行「客人端「我的存酒」为跨店汇总
 * 展示，每条记录标明所属酒吧」。客人端请求按前端真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerWalletReadModelJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private CustomerTicketPaymentFixture f;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 10_000L);
    }

    @Test
    void topupRowHasPaymentChannel() throws Exception {
        String txn = "svtxn_wrm_top_" + f.suffix;
        jdbc.update("INSERT INTO customer_stored_value_transactions (id, account_id, customer_id, txn_type, amount_cent, balance_after_cent,"
                + " reason, idempotency_key, created_at, paid_at, principal_cent, bonus_cent) VALUES (?, ?, ?, 'topup', 10000, 10500, '充值',"
                + " ?, now(), now(), 10000, 500)", txn, f.accountId, f.customerId, "wrm_top_" + f.suffix);
        jdbc.update("INSERT INTO stored_value_topup_batches (id, account_id, customer_id, project_id, payment_channel, fee_bps, principal_cent,"
                + " remaining_principal_cent, paid_at) VALUES (?, ?, ?, ?, 'wechat', 60, 10000, 10000, now())",
                txn, f.accountId, f.customerId, f.projectId);
        transactions(f.accountId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + txn + "')].payment_channel").value(contains("wechat")))
                .andExpect(jsonPath("$.data[?(@.id == '" + txn + "')].bonus_cent").value(contains(500)));
    }

    @Test
    void merchantCardConsumeRowHasMerchant() throws Exception {
        String bar = "bar_wrm_" + f.suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '逐笔测试酒吧', 'bar', 'active', now(), now(), ?)", bar, f.projectId);
        Instant now = Instant.now();
        String card = "svacc_wrm_bar_" + f.suffix;
        customers.saveStoredValueAccount(new CustomerStoredValueAccountRecord(card, f.customerId, null, bar, 3000L, "active", now, now,
                null, 3000L, 0L));
        String txn = "svtxn_wrm_con_" + f.suffix;
        jdbc.update("INSERT INTO customer_stored_value_transactions (id, account_id, customer_id, txn_type, amount_cent, balance_after_cent,"
                + " reason, idempotency_key, created_at, principal_cent, bonus_cent) VALUES (?, ?, ?, 'consume', -2000, 3000,"
                + " '酒吧结账-储值支付', ?, now(), -2000, 0)", txn, card, f.customerId, "wrm_con_" + f.suffix);
        transactions(card).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + txn + "')].merchant_id").value(contains(bar)))
                .andExpect(jsonPath("$.data[?(@.id == '" + txn + "')].merchant_name").value(contains("逐笔测试酒吧")))
                .andExpect(jsonPath("$.data[?(@.id == '" + txn + "')].principal_cent").value(contains(-2000)));
    }

    @Test
    void topupRulesTellConsumeOrderBonusAndRefundBeforeTopup() throws Exception {
        Instant now = Instant.now();
        // 第十八批第 11 条：通用卡赠送比例取已审批、已生效的版本（ticket_reward_config_versions，payload 驼峰键）。
        jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,project_id,version,payload,effective_at,status,created_by,created_at,approved_at)"
                + " VALUES(?,'stored_value_bonus',?,'V0.01','{\"fixedValue\":500,\"minimumValue\":0,\"maximumValue\":1000}'::jsonb,?,'approved','usr_wrm',now(),?)",
                "trc_wrm_" + f.suffix, f.projectId, java.sql.Timestamp.from(now.minusSeconds(60)), java.sql.Timestamp.from(now.minusSeconds(120)));
        mockMvc.perform(get("/api/v1/customer/stored-value/topup-rules").param("project_id", f.projectId)
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.card_scope").value("general"))
                .andExpect(jsonPath("$.data.bonus_bps").value(500))
                .andExpect(jsonPath("$.data.consume_order").value("principal_first"))
                .andExpect(jsonPath("$.data.consume_order_text").value("先扣本金、后扣赠送金"))
                .andExpect(jsonPath("$.data.refund_rule_text").value(containsString("退卡只退未消费的本金，赠送金不退现")));
        String shop = "merchant_wrm_" + f.suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '规则测试商户', 'catering', 'active', now(), now(), ?)", shop, f.projectId);
        customers.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(shop, true, 9000, true, "merchant_prepaid",
                now, now, StoredValueMerchantRefundRuleRecord.CONSUME_BONUS_FIRST));
        mockMvc.perform(get("/api/v1/customer/stored-value/topup-rules").param("merchant_id", shop)
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.card_scope").value("merchant"))
                .andExpect(jsonPath("$.data.consume_order").value("bonus_first"))
                .andExpect(jsonPath("$.data.consume_order_text").value("先扣赠送金、后扣本金"))
                .andExpect(jsonPath("$.data.refund_allowed").value(true))
                .andExpect(jsonPath("$.data.refund_ratio_bps").value(9000));
    }

    @Test
    void mySavedBottlesCarryBarName() throws Exception {
        String item = "ii_wrm_" + f.suffix;
        jdbc.update("INSERT INTO inventory_items (id, sku_code, item_name, item_type, unit, trace_required, min_quantity, status,"
                + " created_at, updated_at) VALUES (?, ?, '逐笔测试威士忌', 'wine', 'bottle', false, 0, 'active', now(), now())", item, item);
        String bottle = "sb_wrm_" + f.suffix;
        jdbc.update("""
                INSERT INTO saved_bottles (id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity, remaining_quantity, unit,
                    expire_at, status, created_at, updated_at, customer_name_or_nickname, customer_phone_hash)
                VALUES (?, ?, ?, ?, '逐笔测试威士忌', 1, 1, 'bottle', ?, 'stored', now(), now(), '张先生', 'hash_wrm')
                """, bottle, f.customerId, f.merchantId, item, Timestamp.from(Instant.now().plusSeconds(86_400 * 10)));
        mockMvc.perform(get("/api/v1/customer/my-saved-bottles").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + bottle + "')].bar_name").value(contains("支付组游乐商户")));
    }

    private ResultActions transactions(String accountId) throws Exception {
        return mockMvc.perform(get("/api/v1/customer/stored-value/accounts/" + accountId + "/transactions")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token));
    }
}
