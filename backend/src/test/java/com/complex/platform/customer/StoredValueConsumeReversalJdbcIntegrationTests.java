package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * TK-20-R1 消费退款回原卡内核（ENUM「储值卡类型与收款主体」：「综合体通用卡和商户级卡的消费退款均退回原消费卡余额，固定先回冲该笔消费中
 * 已扣且尚未回冲的赠送金，该笔消费已扣赠送金全部回冲完毕后，才回冲该笔消费中已扣且尚未回冲的本金；分次退款每次均按此顺序继续回冲，
 * 赠送金与本金各自累计回冲金额不得超过该笔消费中各自的原扣款金额，不因原消费扣款顺序不同或后续消费配置变更而改变」；裁决 Q34）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class StoredValueConsumeReversalJdbcIntegrationTests {

    @Autowired private StoredValueService storedValueService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String customerId;
    private String projectId;
    private String merchantId;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        customerId = "cust_tk20_" + suffix;
        projectId = "prj_tk20_" + suffix;
        merchantId = "mch_tk20_" + suffix;
        Instant now = Instant.now();
        String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        customerRepository.saveCustomer(new CustomerRecord(
                customerId, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** Q34 商户卡例：本 100 赠 50、先赠后本消费 80（扣赠 50 本 30）；退 20/20/40 → 70/20、70/40、100/50。 */
    @Test
    void merchantCardBonusFirstConsumeRefundsBonusThenPrincipalInInstallments() {
        Instant now = Instant.now();
        customerRepository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId, true, 10_000, true, "merchant_prepaid", now, now,
                StoredValueMerchantRefundRuleRecord.CONSUME_BONUS_FIRST));
        String accountId = seedAccount(null, merchantId, 10_000L, 5_000L, "merchant_prepaid");
        var consume = storedValueService.consumeInternal(customerId, null, merchantId, 8_000L, "商户卡消费", "tk20-mc-" + suffix);
        assertThat(account(accountId)).containsEntry("principal_cent", 7_000L).containsEntry("bonus_cent", 0L);

        var first = storedValueService.refundConsumption(consume.id(), 2_000L, "退 20", "tk20-mc-r1-" + suffix);
        assertThat(account(accountId)).containsEntry("principal_cent", 7_000L).containsEntry("bonus_cent", 2_000L);
        assertThat(first.txnType()).isEqualTo("consume_reversal");
        assertThat(first.principalCent()).isEqualTo(0L);
        assertThat(first.bonusCent()).isEqualTo(2_000L);

        storedValueService.refundConsumption(consume.id(), 2_000L, "退 20", "tk20-mc-r2-" + suffix);
        assertThat(account(accountId)).containsEntry("principal_cent", 7_000L).containsEntry("bonus_cent", 4_000L);

        var third = storedValueService.refundConsumption(consume.id(), 4_000L, "退 40", "tk20-mc-r3-" + suffix);
        assertThat(account(accountId)).containsEntry("principal_cent", 10_000L).containsEntry("bonus_cent", 5_000L)
                .containsEntry("balance_cent", 15_000L);
        assertThat(third.principalCent()).isEqualTo(3_000L);
        assertThat(third.bonusCent()).isEqualTo(1_000L);
        assertThat(jdbcTemplate.queryForObject("SELECT original_txn_id FROM customer_stored_value_transactions WHERE id = ?",
                String.class, third.id())).isEqualTo(consume.id());
    }

    /** Q34 通用卡例：本 100 赠 50、先本后赠消费 120（扣本 100 赠 20）；退 30 → 先回赠 20 再回本 10 → 10/50。 */
    @Test
    void generalCardPrincipalFirstConsumeStillRefundsBonusFirst() {
        String accountId = seedAccount(projectId, null, 10_000L, 5_000L, null);
        var consume = storedValueService.consumeInternal(customerId, projectId, null, 12_000L, "通用卡消费", "tk20-gc-" + suffix);
        assertThat(account(accountId)).containsEntry("principal_cent", 0L).containsEntry("bonus_cent", 3_000L);

        var reversal = storedValueService.refundConsumption(consume.id(), 3_000L, "退 30", "tk20-gc-r1-" + suffix);

        assertThat(account(accountId)).containsEntry("principal_cent", 1_000L).containsEntry("bonus_cent", 5_000L);
        assertThat(reversal.principalCent()).isEqualTo(1_000L);
        assertThat(reversal.bonusCent()).isEqualTo(2_000L);
    }

    /** 赠送金与本金各自累计回冲不超原扣：退清后再退 1 分 → 409。 */
    @Test
    void cumulativeReversalCannotExceedOriginalSegments() {
        String accountId = seedAccount(projectId, null, 10_000L, 5_000L, null);
        var consume = storedValueService.consumeInternal(customerId, projectId, null, 12_000L, "通用卡消费", "tk20-cap-" + suffix);
        storedValueService.refundConsumption(consume.id(), 3_000L, "退 30", "tk20-cap-r1-" + suffix);
        storedValueService.refundConsumption(consume.id(), 9_000L, "退 90", "tk20-cap-r2-" + suffix);
        assertThat(account(accountId)).containsEntry("principal_cent", 10_000L).containsEntry("bonus_cent", 5_000L);

        assertThatThrownBy(() -> storedValueService.refundConsumption(consume.id(), 1L, "超退", "tk20-cap-r3-" + suffix))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("STORED_VALUE_REVERSAL_EXCEEDS_ORIGINAL");
        assertThat(account(accountId)).containsEntry("balance_cent", 15_000L);
    }

    /** 卡已关闭（账户关闭/注销）：本金部分按「储值本金退还本人」登记待退、赠送金作废留痕，卡不复活。 */
    @Test
    void reversalOnClosedCardRegistersPrincipalPayoutAndVoidsBonus() {
        String accountId = seedAccount(projectId, null, 10_000L, 5_000L, null);
        var consume = storedValueService.consumeInternal(customerId, projectId, null, 12_000L, "通用卡消费", "tk20-closed-" + suffix);
        jdbcTemplate.update("UPDATE customer_stored_value_accounts SET status = 'closed', principal_cent = 0, bonus_cent = 0, balance_cent = 0 WHERE id = ?", accountId);

        var reversal = storedValueService.refundConsumption(consume.id(), 12_000L, "全退", "tk20-closed-r1-" + suffix);

        assertThat(reversal.principalCent()).isEqualTo(10_000L);
        assertThat(reversal.bonusCent()).isEqualTo(2_000L);
        assertThat(account(accountId)).containsEntry("status", "closed").containsEntry("principal_cent", 0L)
                .containsEntry("bonus_cent", 0L).containsEntry("balance_cent", 0L);
        Map<String, Object> payout = jdbcTemplate.queryForMap(
                "SELECT refund_amount_cent, status FROM finance_refunds WHERE target_id = ? AND source_type = 'stored_value_account'", accountId);
        assertThat(payout).containsEntry("refund_amount_cent", 10_000L).containsEntry("status", "requested");
        List<Map<String, Object>> voids = jdbcTemplate.queryForList(
                "SELECT amount_cent, bonus_cent FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = 'adjust'", accountId);
        assertThat(voids).hasSize(1);
        assertThat(voids.get(0)).containsEntry("amount_cent", -2_000L).containsEntry("bonus_cent", -2_000L);
    }

    /** 同幂等键重放返回第一次的回冲流水、不二次加余额。 */
    @Test
    void sameIdempotencyKeyReplaysFirstReversal() {
        String accountId = seedAccount(projectId, null, 10_000L, 0L, null);
        var consume = storedValueService.consumeInternal(customerId, projectId, null, 4_000L, "通用卡消费", "tk20-idem-" + suffix);

        var first = storedValueService.refundConsumption(consume.id(), 1_000L, "退 10", "tk20-idem-r1-" + suffix);
        var replay = storedValueService.refundConsumption(consume.id(), 1_000L, "退 10", "tk20-idem-r1-" + suffix);

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(account(accountId)).containsEntry("principal_cent", 7_000L);
    }

    /** 平台代收商户卡：消费时挂的「平台应付商户」按回冲额追加冲正行（V344 范式：回指原行、金额取反）。 */
    @Test
    void platformCollectMerchantCardReversalOffsetsPayable() {
        Instant now = Instant.now();
        customerRepository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId, true, 10_000, true, "platform_collect", now, now,
                StoredValueMerchantRefundRuleRecord.CONSUME_PRINCIPAL_FIRST));
        seedAccount(null, merchantId, 10_000L, 0L, "platform_collect");
        var consume = storedValueService.consumeInternal(customerId, null, merchantId, 6_000L, "代收商户卡消费", "tk20-pc-" + suffix);
        String payableId = jdbcTemplate.queryForObject(
                "SELECT id FROM ar_ap_items WHERE source_type = 'stored_value_consume' AND source_id = ? AND origin_item_id IS NULL",
                String.class, consume.id());

        storedValueService.refundConsumption(consume.id(), 2_500L, "退 25", "tk20-pc-r1-" + suffix);

        List<Map<String, Object>> reversals = jdbcTemplate.queryForList(
                "SELECT amount_cent, unpaid_amount_cent, status, counterparty_id FROM ar_ap_items WHERE origin_item_id = ?", payableId);
        assertThat(reversals).hasSize(1);
        assertThat(reversals.get(0)).containsEntry("amount_cent", -2_500L).containsEntry("unpaid_amount_cent", -2_500L)
                .containsEntry("status", "reversed").containsEntry("counterparty_id", merchantId);
    }

    // ---------- helpers ----------

    private String seedAccount(String project, String merchant, long principal, long bonus, String fundOwnership) {
        Instant now = Instant.now();
        String id = "svacc_tk20_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                id, customerId, project, merchant, principal + bonus, "active", now, now, fundOwnership, principal, bonus));
        return id;
    }

    private Map<String, Object> account(String accountId) {
        return jdbcTemplate.queryForMap("SELECT principal_cent, bonus_cent, balance_cent, status FROM customer_stored_value_accounts WHERE id = ?", accountId);
    }
}
