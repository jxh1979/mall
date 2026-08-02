package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.CustomerStoredValueAccountRecord;
import com.complex.platform.customer.CustomerStoredValueTransactionRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.StoredValueService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * AUD-001/002/003/014 储值资金链真 PostgreSQL 集成测试（此前内存仓测试绕过了真实 CHECK/幂等约束）：
 * 酒吧储值结账入库、渠道 CHECK 白名单、幂等指纹防跨操作重放、保留前缀命名空间、
 * 柜台确认充值即入账（含重复确认幂等）、商户预收卡柜台退款路径。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class BarStoredValueFundChainJdbcIntegrationTests {

    private static final String PROJECT_ID = "proj_b1_fund_chain";

    @Autowired
    private BarBillingService billingService;

    @Autowired
    private JdbcBarRepository barRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StoredValueService storedValueService;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.complex.platform.common.crypto.PiiCipher piiCipher;

    @BeforeEach
    void setUpActor() {
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void clearActor() {
        TestSecurityActors.clear();
    }

    // ---------------- AUD-001 储值结账渠道 CHECK ----------------

    @Test
    void settleBillWithStoredValueChannelPersistsSettlementAndDeductsBalance() {
        String customerId = seedCustomer("svchain1");
        String billId = seedUnpaidBill("svchain1", customerId, "merchant_svchain1", 5000L);
        seedAccount(customerId, null, "merchant_svchain1", 8000L, null);

        BarBillRecord settled = billingService.settleBill(billId, new BarBillSettleRequest(
                "svchain1key", null, null, "pay_svchain1", null, "stored_value"));

        assertThat(settled.settlementStatus()).isEqualTo("settled");
        assertThat(settled.paidAmountCent()).isEqualTo(5000L);
        // 真实 INSERT 穿过 chk_bbs_channel（AUD-001 之前此处必 CHECK 失败回滚）。
        String channel = jdbcTemplate.queryForObject(
                "SELECT settle_channel FROM bar_bill_settlements WHERE bill_id = ?", String.class, billId);
        assertThat(channel).isEqualTo("stored_value");
        assertThat(accountBalance(customerId, "merchant_svchain1")).isEqualTo(3000L);
    }

    @Test
    void settleChannelCheckStillRejectsUnknownValues() {
        String customerId = seedCustomer("svchain2");
        String billId = seedUnpaidBill("svchain2", customerId, "merchant_svchain2", 5000L);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO bar_bill_settlements (id, bill_id, settle_channel, settlement_amount_cent, settled_at)
                VALUES (?, ?, 'wechat', 100, now())
                """, "settle_bogus_" + UUID.randomUUID(), billId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------- AUD-002 幂等指纹 + 命名空间 ----------------

    @Test
    void settleBillRejectsReplayOfForeignConsumeTransaction() {
        String customerId = seedCustomer("svchain3");
        String billId = seedUnpaidBill("svchain3", customerId, "merchant_svchain3", 5000L);
        seedAccount(customerId, null, "merchant_svchain3", 8000L, null);
        // 预占键：同客户先以 barsv_k3attack 做了一笔小额消费（历史数据/内部键碰撞）。
        storedValueService.consumeInternal(customerId, null, "merchant_svchain3", 100L,
                "earlier small consume", "barsv_k3attack");

        // 结账幂等键派生出同一个 barsv_k3attack——AUD-002 之前返回旧流水不扣本次应付仍标已支付；
        // 现在指纹(金额/事由)不符 → 409，整单回滚，不产生假已支付账单。
        assertThatThrownBy(() -> billingService.settleBill(billId, new BarBillSettleRequest(
                "k3attack", null, null, "pay_svchain3", null, "stored_value")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("已被其他操作使用");
    }

    @Test
    void clientSuppliedIdempotencyKeyMustNotUseReservedPrefix() {
        String customerId = seedCustomer("svchain4");
        seedAccount(customerId, PROJECT_ID, null, 5000L, null);

        assertThatThrownBy(() -> storedValueService.consume(customerId, PROJECT_ID, null,
                100L, "reserved prefix probe", "sv_settle_pay_intent_squat"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("系统保留前缀");
        assertThatThrownBy(() -> storedValueService.refund(customerId, PROJECT_ID, null,
                100L, "reserved prefix probe", false, "barsv_squat"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("系统保留前缀");
    }

    @Test
    void idempotentReplayWithSameFingerprintReturnsOriginalTransaction() {
        String customerId = seedCustomer("svchain5");
        seedAccount(customerId, PROJECT_ID, null, 5000L, null);

        CustomerStoredValueTransactionRecord first = storedValueService.consume(customerId, PROJECT_ID, null,
                1200L, "same request retry", "retrykey_svchain5");
        CustomerStoredValueTransactionRecord replay = storedValueService.consume(customerId, PROJECT_ID, null,
                1200L, "same request retry", "retrykey_svchain5");

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(projectCardBalance(customerId, PROJECT_ID)).isEqualTo(3800L);

        // 同键改金额=指纹不符，拒绝而非错误重放。
        assertThatThrownBy(() -> storedValueService.consume(customerId, PROJECT_ID, null,
                999L, "same request retry", "retrykey_svchain5"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("已被其他操作使用");
    }

    // ---------------- AUD-014 柜台确认充值即入账 ----------------

    @Test
    void counterOfflineConfirmSettlesTopupOnceEvenWhenRepeated() {
        String customerId = seedCustomer("svchain6");
        PaymentIntentRecord intent = storedValueService.topup(customerId, PROJECT_ID, null,
                6000L, "wechat", "topup_svchain6");
        assertThat(intent.status()).isEqualTo("created");

        OfflinePaymentConfirmRequest confirm = new OfflinePaymentConfirmRequest(
                "confirm_svchain6", null, null, intent.id(), 6000L, "cash", Instant.now());
        storedValueService.confirmOfflineTopupAndSettle(intent.id(), confirm);

        assertThat(financeService.findPaymentIntent(intent.id()).orElseThrow().status()).isEqualTo("succeeded");
        assertThat(projectCardBalance(customerId, PROJECT_ID)).isEqualTo(6000L);

        // 重复确认（柜台重试，同幂等键）：确认/入账全链幂等，不得二次加余额。
        storedValueService.confirmOfflineTopupAndSettle(intent.id(), confirm);
        assertThat(projectCardBalance(customerId, PROJECT_ID)).isEqualTo(6000L);

        // 异键重复确认（另一台柜台/另一次点击）：确认记录可再落一条，但入账仍单次。
        storedValueService.confirmOfflineTopupAndSettle(intent.id(), new OfflinePaymentConfirmRequest(
                "confirm_svchain6_again", null, null, intent.id(), 6000L, "cash", Instant.now()));
        assertThat(projectCardBalance(customerId, PROJECT_ID)).isEqualTo(6000L);
    }

    // ---------------- AUD-003 商户预收卡柜台退款 ----------------

    @Test
    void merchantPrepaidCardRefundsAtCounterWhileOnlineRefundStaysRejected() {
        String customerId = seedCustomer("svchain7");
        seedAccount(customerId, null, "merchant_svchain7", 10000L, "merchant_prepaid");

        // 线上退款仍无条件拒（AUD-060 口径不变）。
        assertThatThrownBy(() -> storedValueService.refund(customerId, null, "merchant_svchain7",
                10000L, "online refund attempt", true, "refund_svchain7_online"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("到发卡商户门店办理");

        // 柜台路径必填兑付凭证。
        assertThatThrownBy(() -> storedValueService.counterRefund(customerId, null, "merchant_svchain7",
                10000L, "counter refund", true, " ", "refund_svchain7_noveh"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("redemption_voucher_no");

        CustomerStoredValueTransactionRecord txn = storedValueService.counterRefund(customerId, null,
                "merchant_svchain7", 10000L, "counter refund", true, "VOUCHER-7788", "refund_svchain7");
        assertThat(txn.txnType()).isEqualTo("refund");
        assertThat(accountBalance(customerId, "merchant_svchain7")).isEqualTo(0L);
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM customer_stored_value_accounts WHERE customer_id = ? AND merchant_id = ?",
                String.class, customerId, "merchant_svchain7");
        assertThat(status).isEqualTo("closed");

        // 柜台路径同参重试：命中 svcr_ 命名空间原流水，安全重放同一笔。
        CustomerStoredValueTransactionRecord replay = storedValueService.counterRefund(customerId, null,
                "merchant_svchain7", 10000L, "counter refund", true, "VOUCHER-7788", "refund_svchain7");
        assertThat(replay.id()).isEqualTo(txn.id());
    }

    @Test
    void onlineAndCounterRefundKeysLiveInSeparateNamespaces() {
        String customerId = seedCustomer("svchain8");
        seedAccount(customerId, null, "merchant_svchain8", 9000L, "platform_collect");

        // 线上退款(platform_collect 卡允许)用键 X。
        CustomerStoredValueTransactionRecord online = storedValueService.refund(customerId, null,
                "merchant_svchain8", 3000L, "cross path key", false, "xpathkey_svchain8");
        assertThat(accountBalance(customerId, "merchant_svchain8")).isEqualTo(6000L);

        // 柜台退款复用同一个客户端键 X：不得命中线上流水冒充「柜台退款成功」（否则柜台再兑付现金=双重兑付），
        // 而是作为柜台命名空间下的独立新退款正常执行。
        CustomerStoredValueTransactionRecord counter = storedValueService.counterRefund(customerId, null,
                "merchant_svchain8", 3000L, "cross path key", false, "VOUCHER-8899", "xpathkey_svchain8");
        assertThat(counter.id()).isNotEqualTo(online.id());
        assertThat(accountBalance(customerId, "merchant_svchain8")).isEqualTo(3000L);
    }

    // ---------------- 造数 ----------------

    private String seedCustomer(String prefix) {
        String id = prefix + "_customer";
        String phone = "139" + String.format("%08d", Math.abs(prefix.hashCode()) % 100_000_000);
        jdbcTemplate.update("""
                INSERT INTO customers (id, phone, masked_phone, status, birth_date, last_login_at, created_at, updated_at)
                VALUES (?, ?, ?, 'active', NULL, NULL, now(), now())
                ON CONFLICT (id) DO UPDATE SET updated_at = now()
                """, id, piiCipher.encrypt(phone), "139****" + phone.substring(7));
        return id;
    }

    private String seedUnpaidBill(String prefix, String customerId, String merchantId, long payableCent) {
        Instant now = Instant.now();
        String tableId = "table_" + prefix;
        barRepository.saveTable(new BarTableRecord(
                tableId, "bar_" + prefix, merchantId, "T-" + prefix.toUpperCase(), "储值链测试桌", "card_seat",
                4, 1, 0L, "CNY", "active", now, now));
        barRepository.saveOrder(new BarOrderRecord(
                "order_" + prefix, "bar_" + prefix, merchantId, tableId, null, customerId,
                "staff", "created", payableCent, "CNY", "web", null, null, now, now));
        String billId = "bill_" + prefix;
        barRepository.saveBill(new BarBillRecord(
                billId, "BILL-" + prefix.toUpperCase(), "order_" + prefix, "bar_" + prefix, merchantId,
                tableId, null, payableCent, 0L, 0L, payableCent, 0L, "CNY", "unpaid", "unsettled",
                null, null, now, now));
        return billId;
    }

    private void seedAccount(String customerId, String projectId, String merchantId, long balanceCent,
                             String fundOwnership) {
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                "svacc_" + UUID.randomUUID().toString().replace("-", ""),
                customerId, projectId, merchantId, balanceCent, "active", Instant.now(), Instant.now(),
                fundOwnership));
    }

    private long accountBalance(String customerId, String merchantId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance_cent FROM customer_stored_value_accounts WHERE customer_id = ? AND merchant_id = ?",
                Long.class, customerId, merchantId);
    }

    private long projectCardBalance(String customerId, String projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance_cent FROM customer_stored_value_accounts WHERE customer_id = ? AND project_id = ? AND merchant_id IS NULL",
                Long.class, customerId, projectId);
    }
}
