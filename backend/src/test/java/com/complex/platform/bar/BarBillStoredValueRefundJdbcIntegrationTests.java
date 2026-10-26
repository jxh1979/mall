package com.complex.platform.bar;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerStoredValueAccountRecord;
import com.complex.platform.finance.RefundPayableSettled;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * TK-20-R2 酒吧账单退款/退品：储值收款部分回原卡、混合收款后收先退（ENUM「储值卡类型与收款主体」：「消费退款均退回原消费卡余额」；
 * ENUM「退款资金流向」/ BSM「退款资金流向」：「混合支付的退款按收款相反顺序逐笔原路退回（后收的先退，单笔退足再退上一笔）；
 * 储值部分回冲仍按先赠送金后本金的既有顺序」；CG-06a 第 12 条「酒吧消费获准退款均回原卡」）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class BarBillStoredValueRefundJdbcIntegrationTests {

    @Autowired private BarBillingService billingService;
    @Autowired private BarOrderService orderService;
    @Autowired private JdbcBarRepository barRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private com.complex.platform.access.AccessRepository accessRepository;
    @Autowired private org.springframework.context.ApplicationEventPublisher events;

    private String suffix;
    private String merchantId;
    private String customerId;
    private String phone;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        merchantId = "mch_tk20r2_" + suffix;
        customerId = "cust_tk20r2_" + suffix;
        // 本商户员工（与 BarStoredValueFundChainJdbcIntegrationTests 同范式）：结账/退款/退品均在发卡商户范围内。
        String userId = "usr_" + merchantId;
        accessRepository.createUser(new com.complex.platform.access.AccessUser(
                userId, "merchant_user", "staff", "org_1", merchantId, "active", Instant.now()));
        accessRepository.saveUserPermissionOverrides(userId,
                List.of("customer.stored_value.refund", "bar.bill.refund", "bar.order_item.void_decide"),
                com.complex.platform.access.AccessRepository.Effect.GRANT, "储值退款测试授权", userId);
        TestSecurityActors.asMerchantUser(merchantId);
        // 酒吧退款分级授权闸（AUD-007）：给操作者本域自助额度，否则 REFUND_REVIEW_REQUIRED。
        jdbcTemplate.update("INSERT INTO refund_authority_limits (id, subject_type, subject_id, refund_domain, limit_amount_cent)"
                + " VALUES (?, 'staff', ?, 'bar', 100000000) ON CONFLICT DO NOTHING", "ral_tk20r2_" + suffix, userId);
        phone = "139" + String.format("%08d", Math.abs(suffix.hashCode()) % 100_000_000);
        customerRepository.saveCustomer(new com.complex.platform.customer.CustomerRecord(
                customerId, phone, "139****" + phone.substring(7), "active", null, null, Instant.now(), Instant.now()));
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                "svacc_tk20r2_" + suffix, customerId, null, merchantId, 8_000L, "active", Instant.now(), Instant.now(),
                "merchant_prepaid", 8_000L, 0L));
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 单一储值结算账单退款：全部回原卡、不生成现金应付。 */
    @Test
    void refundOnStoredValueSettledBillCreditsCardAndCreatesNoPayable() {
        String billId = seedUnpaidBill(5_000L);
        settle(billId, "sv-" + suffix, 5_000L, "stored_value");
        assertThat(cardBalance()).isEqualTo(3_000L);

        BarBillAdjustmentRecord adjustment = billingService.applyBillAdjustment(billId, new BarBillAdjustmentRequest(
                "refund-" + suffix, null, List.of(), "refund", 2_000L, "客人退款", null));

        assertThat(cardBalance()).isEqualTo(5_000L);
        assertThat(adjustment.arApItemId()).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = 'refund' AND source_id = ?",
                Integer.class, adjustment.id())).isZero();
        Map<String, Object> reversal = jdbcTemplate.queryForMap(
                "SELECT amount_cent, txn_type FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = 'consume_reversal'",
                "svacc_tk20r2_" + suffix);
        assertThat(reversal).containsEntry("amount_cent", 2_000L);
    }

    /** 先储值 300 后现金 200，退 250 → 后收的现金先退 200（应付台账）+ 储值回卡 50。 */
    @Test
    void mixedSettlementRefundsLastCollectedFirst() {
        String billId = seedUnpaidBill(5_000L);
        settle(billId, "sv-mix-" + suffix, 3_000L, "stored_value");
        billingService.settleBill(billId, new BarBillSettleRequest(
                "cash-mix-" + suffix, null, List.of(), "pay_cash_" + suffix, 2_000L, "cash"));
        assertThat(cardBalance()).isEqualTo(5_000L);

        BarBillAdjustmentRecord adjustment = billingService.applyBillAdjustment(billId, new BarBillAdjustmentRequest(
                "refund-mix-" + suffix, null, List.of(), "refund", 2_500L, "客人退款", null));

        assertThat(cardBalance()).isEqualTo(5_500L);
        assertThat(adjustment.arApItemId()).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE id = ?", Long.class, adjustment.arApItemId()))
                .isEqualTo(2_000L);
    }

    /** 退品路径（已结账单退未出品商品）：储值结算账单的退品款回原卡、不生成应付。 */
    @Test
    void returnedItemOnStoredValueBillReversesToCard() {
        Instant now = Instant.now();
        String billId = seedUnpaidBill(5_000L);
        String orderId = "order_tk20r2_" + suffix;
        barRepository.saveProduct(new BarProductRecord("product_tk20r2_" + suffix, "bar_tk20r2_" + suffix, merchantId, "退品酒", "spirit",
                2_000L, "CNY", null, null, false, "active", now, now, null, null, null, false, null, null, null));
        String itemId = "item_tk20r2_" + suffix;
        barRepository.saveOrderItem(new BarOrderItemRecord(itemId, orderId, "product_tk20r2_" + suffix, "退品酒", 1, 2_000L, 2_000L,
                "pending", now, now, null, 0L, false, null, null, null, false, false, null));
        settle(billId, "sv-void-" + suffix, 5_000L, "stored_value");
        assertThat(cardBalance()).isEqualTo(3_000L);

        orderService.voidOrderItem(orderId, new BarOrderItemVoidRequest(
                "void-" + suffix, null, List.of(), itemId, "客人退品", false, billId, null));

        assertThat(cardBalance()).isEqualTo(5_000L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = 'refund' AND source_id LIKE 'bill_adjustment_void_%'"
                + " AND counterparty_id IN (?, ?)", Integer.class, customerId, merchantId)).isZero();
    }

    /** 复核 money I3：退品驳回把回卡的储值扣回后，同一账单再退款不再 409（扣回冲减原消费已回冲累计）。 */
    @Test
    void refundAfterRejectedReturnStillReversesToCard() {
        Instant now = Instant.now();
        String billId = seedUnpaidBill(5_000L);
        String orderId = "order_tk20r2_" + suffix;
        barRepository.saveProduct(new BarProductRecord("product_tk20r2_" + suffix, "bar_tk20r2_" + suffix, merchantId, "退品酒", "spirit",
                2_000L, "CNY", null, null, false, "active", now, now, null, null, null, false, null, null, null));
        String itemId = "item_tk20r2_" + suffix;
        barRepository.saveOrderItem(new BarOrderItemRecord(itemId, orderId, "product_tk20r2_" + suffix, "退品酒", 1, 2_000L, 2_000L,
                "pending", now, now, null, 0L, false, null, null, null, false, false, null));
        settle(billId, "sv-rj-" + suffix, 5_000L, "stored_value");
        BarOrderItemVoidRecord voided = orderService.voidOrderItem(orderId, new BarOrderItemVoidRequest(
                "void-rj-" + suffix, null, List.of(), itemId, "客人退品", false, billId, null));
        assertThat(cardBalance()).isEqualTo(5_000L);
        orderService.decideOrderItemVoid(voided.id(), new BarOrderItemVoidDecisionRequest("void-rj-decide-" + suffix, "rejected", "不予退品"));
        assertThat(cardBalance()).isEqualTo(3_000L);

        org.assertj.core.api.Assertions.assertThatCode(() -> billingService.applyBillAdjustment(billId, new BarBillAdjustmentRequest(
                "refund-rj-" + suffix, null, List.of(), "refund", 5_000L, "整单退款", null))).doesNotThrowAnyException();

        assertThat(cardBalance()).isEqualTo(8_000L);
    }

    /**
     * 第十六批第 16 条（ENUM L101「账单状态 待结、部分支付、已结、部分退款、已退款、异常」；BSM L1062「只有实际退款成功才记为已退」）：
     * 储值收款的退款回卡即成功——部分退为部分退款，退满实收为已退款；结算状态仍为已结。
     */
    @Test
    void storedValueRefundMarksBillPartialThenFullyRefunded() {
        String billId = seedUnpaidBill(5_000L);
        settle(billId, "sv-st-" + suffix, 5_000L, "stored_value");

        billingService.applyBillAdjustment(billId, new BarBillAdjustmentRequest(
                "refund-st1-" + suffix, null, List.of(), "refund", 2_000L, "客人退款", null));
        assertThat(billStatus(billId)).containsEntry("payment_status", "partial_refunded")
                .containsEntry("settlement_status", "settled");

        billingService.applyBillAdjustment(billId, new BarBillAdjustmentRequest(
                "refund-st2-" + suffix, null, List.of(), "refund", 3_000L, "客人退款", null));
        assertThat(billStatus(billId)).containsEntry("payment_status", "refunded")
                .containsEntry("settlement_status", "settled");
    }

    /** 第十六批第 16 条：现金等渠道的退款挂出应付，财务实付结清（RefundPayableSettled）前账单不算已退，结清后按金额置已退款。 */
    @Test
    void cashRefundCountsOnlyAfterRefundPayableSettled() {
        String billId = seedUnpaidBill(5_000L);
        billingService.settleBill(billId, new BarBillSettleRequest(
                "cash-st-" + suffix, null, List.of(), "pay_cash_st_" + suffix, 5_000L, "cash"));
        BarBillAdjustmentRecord adjustment = billingService.applyBillAdjustment(billId, new BarBillAdjustmentRequest(
                "refund-cash-" + suffix, null, List.of(), "refund", 5_000L, "整单退款", null));
        assertThat(adjustment.arApItemId()).isNotNull();
        assertThat(billStatus(billId)).containsEntry("payment_status", "paid");

        jdbcTemplate.update("UPDATE ar_ap_items SET status = 'settled', paid_amount_cent = amount_cent, unpaid_amount_cent = 0 WHERE id = ?",
                adjustment.arApItemId());
        events.publishEvent(new RefundPayableSettled(adjustment.arApItemId(), adjustment.id()));

        assertThat(billStatus(billId)).containsEntry("payment_status", "refunded");
    }

    /** 第十六批第 16 条：已结账单退品的退款在退品待审核期间不算成功，审批通过后才置部分退款。 */
    @Test
    void returnedItemRefundCountsAfterVoidApproved() {
        Instant now = Instant.now();
        String billId = seedUnpaidBill(5_000L);
        String orderId = "order_tk20r2_" + suffix;
        barRepository.saveProduct(new BarProductRecord("product_tk20r2_" + suffix, "bar_tk20r2_" + suffix, merchantId, "退品酒", "spirit",
                2_000L, "CNY", null, null, false, "active", now, now, null, null, null, false, null, null, null));
        String itemId = "item_tk20r2_" + suffix;
        barRepository.saveOrderItem(new BarOrderItemRecord(itemId, orderId, "product_tk20r2_" + suffix, "退品酒", 1, 2_000L, 2_000L,
                "pending", now, now, null, 0L, false, null, null, null, false, false, null));
        settle(billId, "sv-vst-" + suffix, 5_000L, "stored_value");
        BarOrderItemVoidRecord voided = orderService.voidOrderItem(orderId, new BarOrderItemVoidRequest(
                "void-st-" + suffix, null, List.of(), itemId, "客人退品", false, billId, null));
        assertThat(billStatus(billId)).containsEntry("payment_status", "paid");

        orderService.decideOrderItemVoid(voided.id(), new BarOrderItemVoidDecisionRequest("void-st-decide-" + suffix, "approved", "同意退品"));

        assertThat(billStatus(billId)).containsEntry("payment_status", "partial_refunded");
    }

    // ---------- 造数 ----------

    private Map<String, Object> billStatus(String billId) {
        return jdbcTemplate.queryForMap("SELECT payment_status, settlement_status FROM bar_bills WHERE id = ?", billId);
    }

    private void settle(String billId, String key, long amountCent, String channel) {
        billingService.settleBill(billId, new BarBillSettleRequest(
                key, null, null, "pay_" + key, amountCent, channel, null, null, phone, "000000"));
    }

    private long cardBalance() {
        return jdbcTemplate.queryForObject("SELECT balance_cent FROM customer_stored_value_accounts WHERE id = ?",
                Long.class, "svacc_tk20r2_" + suffix);
    }

    private String seedUnpaidBill(long payableCent) {
        Instant now = Instant.now();
        String tableId = "table_tk20r2_" + suffix;
        barRepository.saveTable(new BarTableRecord(
                tableId, "bar_tk20r2_" + suffix, merchantId, "T-" + suffix, "储值退款测试桌", "card_seat",
                4, 1, 0L, "CNY", "active", now, now));
        barRepository.saveOrder(new BarOrderRecord(
                "order_tk20r2_" + suffix, "bar_tk20r2_" + suffix, merchantId, tableId, null, customerId,
                "staff", "created", payableCent, "CNY", "web", null, null, now, now));
        String billId = "bill_tk20r2_" + suffix;
        barRepository.saveBill(new BarBillRecord(
                billId, "BILL-" + suffix.toUpperCase(), "order_tk20r2_" + suffix, "bar_tk20r2_" + suffix, merchantId,
                tableId, null, payableCent, 0L, 0L, payableCent, 0L, "CNY", "unpaid", "unsettled",
                null, null, now, now));
        return billId;
    }
}
