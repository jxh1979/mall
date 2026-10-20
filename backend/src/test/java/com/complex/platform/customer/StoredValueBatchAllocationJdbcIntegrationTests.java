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
 * TK-19 储值充值批次与消费分摊、退款恢复批次（ENUM「储值卡类型与收款主体」：「通用卡的每笔充值分别保留当前剩余本金、支付通路和
 * 支付成功时固化费率，同一通路不同费率的充值分批保存」「每次消费需扣本金时，按消费前各批次剩余本金占总剩余本金的比例分摊本次本金扣减，
 * 并逐批按其原固化费率计算对应通路费；本金已耗尽的批次不参与。本金扣减为零的消费不运行本金批次分摊」「后续充值不重算已发生消费的批次构成」
 * 「该本金按原消费各充值批次已扣且尚未回冲本金的比例恢复到原批次……各批次累计恢复金额不得超过该消费原扣该批次的本金。原消费未使用的批次
 * （包括后续充值批次）不参与本次恢复」；裁决 Q35/Q35A、DEC-P1A-04）。
 *
 * <p>真实 Flyway 迁移库，{@code @Transactional} 每用例回滚；充值走柜台确认入账正路，消费走 consumeInternal，退款走 refundConsumption。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class StoredValueBatchAllocationJdbcIntegrationTests {

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
        customerId = "cust_tk19_" + suffix;
        projectId = "prj_tk19_" + suffix;
        merchantId = "mch_tk19_" + suffix;
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

    // ---------- TK-19-R1 充值批次 ----------

    /** 通用卡充值即登记批次：剩余本金=本金、支付通路、支付成功时按项目×通路固化费率。 */
    @Test
    void topupOnGeneralCardCreatesBatchWithLockedChannelRate() {
        seedChannelFee("pos", 150);
        String txnId = topupViaCounter(projectId, null, 30_000L, "pos");

        List<Map<String, Object>> batches = batches(projectId, null);
        assertThat(batches).hasSize(1);
        assertThat(batches.get(0).get("id")).isEqualTo(txnId);
        assertThat(batches.get(0).get("payment_channel")).isEqualTo("pos");
        assertThat(batches.get(0).get("fee_bps")).isEqualTo(150);
        assertThat(batches.get(0).get("principal_cent")).isEqualTo(30_000L);
        assertThat(batches.get(0).get("remaining_principal_cent")).isEqualTo(30_000L);
    }

    /** 同一通路费率变更后再充值：分批保存、旧批次费率不被覆盖（DEC-P1A-04）。 */
    @Test
    void sameChannelDifferentRatesKeepSeparateBatches() {
        seedChannelFee("pos", 100);
        topupViaCounter(projectId, null, 30_000L, "pos");
        jdbcTemplate.update("UPDATE channel_fee_configs SET fee_bps = 200 WHERE project_id = ? AND channel = 'pos'", projectId);
        topupViaCounter(projectId, null, 70_000L, "pos");

        List<Map<String, Object>> batches = batches(projectId, null);
        assertThat(batches).extracting(b -> b.get("principal_cent")).containsExactly(30_000L, 70_000L);
        assertThat(batches).extracting(b -> b.get("fee_bps")).containsExactly(100, 200);
    }

    /** 商户级卡充值不建批次（本条只约束通用卡）。 */
    @Test
    void merchantCardTopupCreatesNoBatch() {
        Instant now = Instant.now();
        customerRepository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId, true, 10_000, true, "merchant_prepaid", now, now,
                StoredValueMerchantRefundRuleRecord.CONSUME_PRINCIPAL_FIRST));
        topupViaCounter(null, merchantId, 5_000L, "pos");
        assertThat(account(null, merchantId).get("principal_cent")).isEqualTo(5_000L);
        assertThat(batches(null, merchantId)).isEmpty();
    }

    // ---------- TK-19-R2 消费分摊 ----------

    /** 批次 300@1%、700@2%，消费本金 500 → 按剩余比例分摊 150/350，通路费按各自原费率 1.5 元 / 7 元。 */
    @Test
    void consumeSplitsPrincipalAcrossOpenBatchesProRata() {
        String batchA = seedBatch(30_000L, 100, "pos");
        String batchB = seedBatch(70_000L, 200, "pos");

        var consume = storedValueService.consumeInternal(customerId, projectId, null, 50_000L, "分摊测试消费", "tk19-consume-" + suffix);

        List<Map<String, Object>> allocations = allocations(consume.id());
        assertThat(allocations).hasSize(2);
        assertThat(allocationOf(allocations, batchA)).containsEntry("principal_cent", 15_000L).containsEntry("fee_bps", 100).containsEntry("fee_cent", 150L);
        assertThat(allocationOf(allocations, batchB)).containsEntry("principal_cent", 35_000L).containsEntry("fee_bps", 200).containsEntry("fee_cent", 700L);
        assertThat(remaining(batchA)).isEqualTo(15_000L);
        assertThat(remaining(batchB)).isEqualTo(35_000L);
    }

    /** 本金已耗尽的批次不参与分摊。 */
    @Test
    void exhaustedBatchDoesNotParticipate() {
        String exhausted = seedBatch(10_000L, 100, "pos");
        jdbcTemplate.update("UPDATE stored_value_topup_batches SET remaining_principal_cent = 0 WHERE id = ?", exhausted);
        String open = seedBatch(20_000L, 300, "pos");
        // 卡本金 20_000（耗尽批次已不在余额里）
        jdbcTemplate.update("UPDATE customer_stored_value_accounts SET principal_cent = 20000, balance_cent = 20000 WHERE id = ?", accountId());

        var consume = storedValueService.consumeInternal(customerId, projectId, null, 5_000L, "耗尽批次测试", "tk19-exh-" + suffix);

        List<Map<String, Object>> allocations = allocations(consume.id());
        assertThat(allocations).hasSize(1);
        assertThat(allocations.get(0).get("batch_id")).isEqualTo(open);
        assertThat(allocations.get(0).get("principal_cent")).isEqualTo(5_000L);
        assertThat(remaining(exhausted)).isEqualTo(0L);
    }

    /** 后续充值不重算已发生消费的批次构成。 */
    @Test
    void laterTopupDoesNotRecomputeEarlierConsume() {
        String batchA = seedBatch(10_000L, 100, "pos");
        var consume = storedValueService.consumeInternal(customerId, projectId, null, 4_000L, "先消费", "tk19-early-" + suffix);
        seedChannelFee("pos", 500);
        topupViaCounter(projectId, null, 50_000L, "pos");

        List<Map<String, Object>> allocations = allocations(consume.id());
        assertThat(allocations).hasSize(1);
        assertThat(allocations.get(0).get("batch_id")).isEqualTo(batchA);
        assertThat(allocations.get(0).get("principal_cent")).isEqualTo(4_000L);
        assertThat(allocations.get(0).get("fee_bps")).isEqualTo(100);
    }

    /** 本金扣减为零（只扣赠送金）的消费不运行批次分摊。 */
    @Test
    void bonusOnlyConsumeRunsNoAllocation() {
        String open = seedBatch(10_000L, 100, "pos");
        // 卡本金已为 0、只剩赠送金（批次剩余与卡本金的对账不在本用例）
        jdbcTemplate.update("UPDATE customer_stored_value_accounts SET principal_cent = 0, bonus_cent = 3000, balance_cent = 3000 WHERE id = ?", accountId());

        var consume = storedValueService.consumeInternal(customerId, projectId, null, 1_000L, "只扣赠送金", "tk19-bonus-" + suffix);

        assertThat(allocations(consume.id())).isEmpty();
        assertThat(remaining(open)).isEqualTo(10_000L);
    }

    /** 三批各 100 分、消费 100 分：33/33/34，分摊总和恒等于本次本金扣减。 */
    @Test
    void remainderAllocationSumsExactly() {
        seedBatch(100L, 100, "pos");
        seedBatch(100L, 100, "pos");
        seedBatch(100L, 100, "pos");

        var consume = storedValueService.consumeInternal(customerId, projectId, null, 100L, "尾差测试", "tk19-rem-" + suffix);

        List<Map<String, Object>> allocations = allocations(consume.id());
        assertThat(allocations).hasSize(3);
        long sum = allocations.stream().mapToLong(a -> (Long) a.get("principal_cent")).sum();
        assertThat(sum).isEqualTo(100L);
        assertThat(allocations).extracting(a -> a.get("principal_cent")).containsExactlyInAnyOrder(33L, 33L, 34L);
    }

    // ---------- TK-19-R3 退款恢复批次 ----------

    /** Q35A：原消费 A150/B350，退本金 200 → A+60/B+140，通路费按各自原费率冲减。 */
    @Test
    void refundRestoresPrincipalToOriginalBatchesProRata() {
        String batchA = seedBatch(30_000L, 100, "pos");
        String batchB = seedBatch(70_000L, 200, "pos");
        var consume = storedValueService.consumeInternal(customerId, projectId, null, 50_000L, "待退消费", "tk19-r3-" + suffix);

        var reversal = storedValueService.refundConsumption(consume.id(), 20_000L, "部分退款", "tk19-r3-refund-" + suffix);

        assertThat(reversal).isNotNull();
        assertThat(remaining(batchA)).isEqualTo(21_000L);
        assertThat(remaining(batchB)).isEqualTo(49_000L);
        List<Map<String, Object>> allocations = allocations(consume.id());
        assertThat(allocationOf(allocations, batchA).get("restored_principal_cent")).isEqualTo(6_000L);
        assertThat(allocationOf(allocations, batchB).get("restored_principal_cent")).isEqualTo(14_000L);
        List<Map<String, Object>> restorations = jdbcTemplate.queryForList(
                "SELECT r.principal_cent, r.fee_bps, r.fee_adjust_cent, a.batch_id FROM stored_value_consume_restorations r"
                        + " JOIN stored_value_consume_allocations a ON a.id = r.allocation_id WHERE r.reversal_txn_id = ?", reversal.id());
        assertThat(restorations).hasSize(2);
        assertThat(allocationOf(restorations, batchA)).containsEntry("principal_cent", 6_000L).containsEntry("fee_bps", 100).containsEntry("fee_adjust_cent", -60L);
        assertThat(allocationOf(restorations, batchB)).containsEntry("principal_cent", 14_000L).containsEntry("fee_bps", 200).containsEntry("fee_adjust_cent", -280L);
    }

    /** 分次退款各批次累计恢复不超原扣：200 + 300 退完后各批恢复到原扣额、批次剩余回到充值额；再退 1 分被拒。 */
    @Test
    void installmentRefundsNeverExceedOriginalPerBatch() {
        String batchA = seedBatch(30_000L, 100, "pos");
        String batchB = seedBatch(70_000L, 200, "pos");
        var consume = storedValueService.consumeInternal(customerId, projectId, null, 50_000L, "分次退", "tk19-inst-" + suffix);

        storedValueService.refundConsumption(consume.id(), 20_000L, "第一次退", "tk19-inst-1-" + suffix);
        storedValueService.refundConsumption(consume.id(), 30_000L, "第二次退", "tk19-inst-2-" + suffix);

        List<Map<String, Object>> allocations = allocations(consume.id());
        assertThat(allocationOf(allocations, batchA).get("restored_principal_cent")).isEqualTo(15_000L);
        assertThat(allocationOf(allocations, batchB).get("restored_principal_cent")).isEqualTo(35_000L);
        assertThat(remaining(batchA)).isEqualTo(30_000L);
        assertThat(remaining(batchB)).isEqualTo(70_000L);
        assertThatThrownBy(() -> storedValueService.refundConsumption(consume.id(), 1L, "超退", "tk19-inst-3-" + suffix))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("STORED_VALUE_REVERSAL_EXCEEDS_ORIGINAL");
    }

    /** 原消费未使用的后续充值批次不参与恢复。 */
    @Test
    void batchToppedUpAfterConsumeNotRestored() {
        String batchA = seedBatch(30_000L, 100, "pos");
        var consume = storedValueService.consumeInternal(customerId, projectId, null, 10_000L, "先消费", "tk19-later-" + suffix);
        String later = seedBatch(50_000L, 300, "pos");

        storedValueService.refundConsumption(consume.id(), 10_000L, "全退", "tk19-later-refund-" + suffix);

        assertThat(remaining(batchA)).isEqualTo(30_000L);
        assertThat(remaining(later)).isEqualTo(50_000L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stored_value_consume_restorations r JOIN stored_value_consume_allocations a ON a.id = r.allocation_id WHERE a.batch_id = ?",
                Integer.class, later)).isZero();
    }

    // ---------- helpers ----------

    private void seedChannelFee(String channel, int feeBps) {
        jdbcTemplate.update("INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, status, is_draft) VALUES (?, ?, ?, ?, 'active', FALSE)",
                "cfc_tk19_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12), projectId, channel, feeBps);
    }

    /** 复核 money m1：卡已关闭后收到消费退款——本金登记待退本人，不再把本金加回原充值批次。 */
    @Test
    void consumeRefundOnClosedCardDoesNotRestoreBatches() {
        String batch = seedBatch(10_000L, 60, "wechat");
        var consume = storedValueService.consumeInternal(customerId, projectId, null, 6_000L, "消费", "tk19-m1-" + suffix);
        jdbcTemplate.update("UPDATE customer_stored_value_accounts SET status = 'closed', principal_cent = 0, bonus_cent = 0, balance_cent = 0 WHERE id = ?",
                accountId());

        storedValueService.refundConsumption(consume.id(), 6_000L, "关卡后退款", "tk19-m1-r-" + suffix);

        assertThat(jdbcTemplate.queryForObject("SELECT remaining_principal_cent FROM stored_value_topup_batches WHERE id = ?", Long.class, batch))
                .isEqualTo(4_000L);
    }

    /** 柜台 POS 充值：支付意图 + 卡范围绑定直接落库，再走正路柜台确认入账；返回充值流水 id。 */
    private String topupViaCounter(String project, String merchant, long amountCent, String channel) {
        Instant now = Instant.now();
        String paymentId = "pay_tk19_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        // 业务单号带支付意图号：线下确认按 (stored_value_topup, 业务单号) 写内部核算行，同客户两次柜台充值不撞唯一索引。
        financeRepository.savePaymentIntent(new PaymentIntentRecord(paymentId,
                merchant == null ? "stored_value_topup" : "stored_value_topup_merchant", customerId + "|" + paymentId, amountCent,
                "CNY", channel, "created", now, now));
        customerRepository.bindStoredValueTopupIntent(paymentId, customerId, project, merchant, now);
        storedValueService.confirmOfflineTopupAndSettle(paymentId, new OfflinePaymentConfirmRequest(
                "tk19-confirm-" + UUID.randomUUID(), null, List.of(), paymentId, amountCent, channel, now));
        return jdbcTemplate.queryForObject("SELECT id FROM customer_stored_value_transactions WHERE payment_intent_id = ? AND txn_type = 'topup'",
                String.class, paymentId);
    }

    /** 直接造一批（充值流水 + 批次行）并把卡本金加上去：用于只验分摊/恢复的用例。 */
    private String seedBatch(long principalCent, int feeBps, String channel) {
        Instant now = Instant.now();
        String accountId = accountId();
        Map<String, Object> acc = account(projectId, null);
        long principalAfter = (Long) acc.get("principal_cent") + principalCent;
        long bonus = (Long) acc.get("bonus_cent");
        jdbcTemplate.update("UPDATE customer_stored_value_accounts SET principal_cent = ?, balance_cent = ? WHERE id = ?",
                principalAfter, principalAfter + bonus, accountId);
        String txnId = "svtxn_" + UUID.randomUUID().toString().replace("-", "");
        customerRepository.saveStoredValueTransaction(new CustomerStoredValueTransactionRecord(
                txnId, accountId, customerId, "topup", principalCent, principalAfter + bonus, null, "seed batch",
                "tk19-seed-" + txnId, now, now, principalCent, 0L));
        jdbcTemplate.update("INSERT INTO stored_value_topup_batches (id, account_id, customer_id, project_id, payment_channel, fee_bps, principal_cent, remaining_principal_cent, paid_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", txnId, accountId, customerId, projectId, channel, feeBps, principalCent, principalCent,
                java.sql.Timestamp.from(now));
        return txnId;
    }

    private String accountId() {
        return customerRepository.findStoredValueAccount(customerId, projectId, null)
                .orElseGet(() -> customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                        "svacc_tk19_" + suffix, customerId, projectId, null, 0L, "active", Instant.now(), Instant.now(), null, 0L, 0L)))
                .id();
    }

    private Map<String, Object> account(String project, String merchant) {
        accountId();
        return jdbcTemplate.queryForMap("SELECT id, principal_cent, bonus_cent, balance_cent, status FROM customer_stored_value_accounts"
                + " WHERE customer_id = ? AND project_id IS NOT DISTINCT FROM ? AND merchant_id IS NOT DISTINCT FROM ?",
                customerId, project, merchant);
    }

    private List<Map<String, Object>> batches(String project, String merchant) {
        return jdbcTemplate.queryForList("SELECT b.id, b.payment_channel, b.fee_bps, b.principal_cent, b.remaining_principal_cent"
                + " FROM stored_value_topup_batches b JOIN customer_stored_value_accounts a ON a.id = b.account_id"
                + " WHERE a.customer_id = ? AND a.project_id IS NOT DISTINCT FROM ? AND a.merchant_id IS NOT DISTINCT FROM ? ORDER BY b.paid_at, b.id",
                customerId, project, merchant);
    }

    private List<Map<String, Object>> allocations(String consumeTxnId) {
        return jdbcTemplate.queryForList("SELECT id, batch_id, principal_cent, restored_principal_cent, fee_bps, fee_cent"
                + " FROM stored_value_consume_allocations WHERE consume_txn_id = ?", consumeTxnId);
    }

    private static Map<String, Object> allocationOf(List<Map<String, Object>> rows, String batchId) {
        return rows.stream().filter(r -> batchId.equals(r.get("batch_id"))).findFirst().orElseThrow();
    }

    private long remaining(String batchId) {
        return jdbcTemplate.queryForObject("SELECT remaining_principal_cent FROM stored_value_topup_batches WHERE id = ?", Long.class, batchId);
    }

    private void authenticateWithStoredValuePermissions() {
        String userId = "usr_tk19_" + suffix;
        String roleId = "role_tk19_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_tk19_" + suffix);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId,
                "储值批次测试角色" + System.nanoTime());
        for (String code : List.of("customer.stored_value.topup", "customer.stored_value.consume",
                "customer.stored_value.refund", "customer.stored_value.view", "finance.payment.register")) {
            jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        AccessActor actor = new AccessActor(userId, "u_tk19_" + suffix, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }
}
