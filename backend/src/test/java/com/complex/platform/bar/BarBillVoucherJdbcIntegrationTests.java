package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.voucher.VoucherDefinitionRecord;
import com.complex.platform.voucher.VoucherInstanceRecord;
import com.complex.platform.voucher.VoucherRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十批 TK-28-R1：结账时核销商户折扣券并原子抵扣账单应付。
 * 依据：ENUM L81「商户折扣券——到对应商户抵扣」；ENUM L83「券抵扣部分不折现」；ENUM L108「券抵扣的金额不计入…销售提成基数」；
 * BSM L1064「不倒扣达标进度」；PGM L303「只有这次操作成功完成…才算用掉」；操作员裁决（2026-10-20）「一次结账一张」。
 * 本类【非事务】：每个用例在独立事务里跑 settleBill，失败即真实回滚，券状态按库内实态断言。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class BarBillVoucherJdbcIntegrationTests {

    @Autowired private BarBillingService billingService;
    @Autowired private JdbcBarRepository barRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager txManager;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        tx = new TransactionTemplate(txManager);
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void settleWithMerchantDiscountVoucherDeductsPayableAndRedeems() {
        String s = suffix();
        String merchant = "merchant_vch_" + s;
        // 低消 = 消费额：券抵扣后仍须可结账（BSM L1064 不倒扣达标进度）。
        String billId = seedBill(s, merchant, 30000L, 30000L);
        VoucherInstanceRecord voucher = seedVoucher(s + "a", merchant, 5000L);

        BarBillRecord settled = tx.execute(st -> billingService.settleBill(billId, new BarBillSettleRequest(
                "vch-settle-" + s, null, List.of(), "pay_vch_" + s, 25000L, "aggregate_pay",
                null, null, null, null, voucher.code(), null)));

        assertThat(settled.discountAmountCent()).isEqualTo(5000L);
        assertThat(settled.payableAmountCent()).isEqualTo(25000L);
        assertThat(settled.paidAmountCent()).isEqualTo(25000L);
        assertThat(settled.settlementStatus()).isEqualTo("settled");
        assertThat(voucherRepository.findInstanceById(voucher.id()).orElseThrow().status()).isEqualTo("redeemed");
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT bill_type, bill_id, deduction_amount_cent FROM voucher_redemption_records WHERE voucher_instance_id = ?",
                voucher.id());
        assertThat(row.get("bill_type")).isEqualTo("bar_bill");
        assertThat(row.get("bill_id")).isEqualTo(billId);
        assertThat(((Number) row.get("deduction_amount_cent")).longValue()).isEqualTo(5000L);
    }

    @Test
    void otherMerchantVoucherRejectedAndNotConsumed() {
        String s = suffix();
        String billId = seedBill(s, "merchant_vch_" + s, 30000L, 0L);
        VoucherInstanceRecord other = seedVoucher(s + "o", "merchant_other_" + s, 5000L);

        assertThatThrownBy(() -> tx.execute(st -> billingService.settleBill(billId, new BarBillSettleRequest(
                "vch-other-" + s, null, List.of(), "pay_vch_" + s, 25000L, "aggregate_pay",
                null, null, null, null, other.code(), null))))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_MERCHANT_MISMATCH"));

        assertThat(voucherRepository.findInstanceById(other.id()).orElseThrow().status()).isEqualTo("issued");
        assertThat(redemptionCount(other.id())).isZero();
        BarBillRecord bill = barRepository.findBillById(billId).orElseThrow();
        assertThat(bill.discountAmountCent()).isZero();
        assertThat(bill.settlementStatus()).isEqualTo("unsettled");
    }

    @Test
    void settlementFailureRollsBackVoucher() {
        String s = suffix();
        String merchant = "merchant_vch_" + s;
        String billId = seedBill(s, merchant, 30000L, 0L);
        VoucherInstanceRecord voucher = seedVoucher(s + "r", merchant, 5000L);

        // 扣券后剩余 25000，本次却要收 30000 → 结账失败；券核销须随整笔结账一起回滚（PGM L303）。
        assertThatThrownBy(() -> tx.execute(st -> billingService.settleBill(billId, new BarBillSettleRequest(
                "vch-fail-" + s, null, List.of(), "pay_vch_" + s, 30000L, "aggregate_pay",
                null, null, null, null, voucher.code(), null))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("剩余未付");

        assertThat(voucherRepository.findInstanceById(voucher.id()).orElseThrow().status()).isEqualTo("issued");
        assertThat(redemptionCount(voucher.id())).isZero();
        assertThat(barRepository.findBillById(billId).orElseThrow().discountAmountCent()).isZero();
    }

    @Test
    void voucherFaceAboveRemainingIsCappedNotRefunded() {
        String s = suffix();
        String merchant = "merchant_vch_" + s;
        String billId = seedBill(s, merchant, 3000L, 0L);
        VoucherInstanceRecord voucher = seedVoucher(s + "c", merchant, 5000L);

        BarBillRecord closed = tx.execute(st -> billingService.settleBill(billId, new BarBillSettleRequest(
                "vch-cap-" + s, null, List.of(), "pay_vch_" + s, null, "aggregate_pay",
                null, null, null, null, null, voucher.id())));

        assertThat(closed.discountAmountCent()).isEqualTo(3000L);
        assertThat(closed.payableAmountCent()).isZero();
        assertThat(closed.paidAmountCent()).isZero();
        assertThat(closed.settlementStatus()).isEqualTo("settled");
        // 0 元关单：无收款流水、无任何退款/找零行；券整张用掉（ENUM L83 不折现）。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM bar_bill_settlements WHERE bill_id = ?", Integer.class, billId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM bar_bill_adjustments WHERE bill_id = ?", Integer.class, billId)).isZero();
        assertThat(voucherRepository.findInstanceById(voucher.id()).orElseThrow().status()).isEqualTo("redeemed");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deduction_amount_cent FROM voucher_redemption_records WHERE voucher_instance_id = ?",
                Long.class, voucher.id())).isEqualTo(3000L);
    }

    @Test
    void secondVoucherInSameSettlementRejected() {
        String s = suffix();
        String merchant = "merchant_vch_" + s;
        String billId = seedBill(s, merchant, 30000L, 0L);
        VoucherInstanceRecord first = seedVoucher(s + "1", merchant, 5000L);
        VoucherInstanceRecord second = seedVoucher(s + "2", merchant, 5000L);

        assertThatThrownBy(() -> tx.execute(st -> billingService.settleBill(billId, new BarBillSettleRequest(
                "vch-two-" + s, null, List.of(), "pay_vch_" + s, 20000L, "aggregate_pay",
                null, null, null, null, first.code(), second.id()))))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_ONE_PER_SETTLEMENT"));

        assertThat(voucherRepository.findInstanceById(first.id()).orElseThrow().status()).isEqualTo("issued");
        assertThat(voucherRepository.findInstanceById(second.id()).orElseThrow().status()).isEqualTo("issued");
        assertThat(barRepository.findBillById(billId).orElseThrow().discountAmountCent()).isZero();
    }

    // ── 种子 ─────────────────────────────────────────────────────────────

    private static String suffix() {
        return Long.toString(System.nanoTime(), 36);
    }

    private String seedBill(String s, String merchantId, long subtotalCent, long minConsumptionCent) {
        Instant now = Instant.now();
        String tableId = "table_vch_" + s;
        String orderId = "order_vch_" + s;
        String billId = "bill_vch_" + s;
        tx.executeWithoutResult(st -> {
            barRepository.saveTable(new BarTableRecord(
                    tableId, "bar_vch", merchantId, "T-VCH" + s, "折扣券测试桌", "card_seat",
                    4, 1, minConsumptionCent, "CNY", "active", now, now));
            barRepository.saveOrder(new BarOrderRecord(
                    orderId, "bar_vch", merchantId, tableId, null, "customer_vch_" + s,
                    "staff", "created", subtotalCent, "CNY", "web", null, null, now, now));
            barRepository.saveBill(new BarBillRecord(
                    billId, "BILL-VCH-" + s, orderId, "bar_vch", merchantId,
                    tableId, null, subtotalCent, 0L, 0L, subtotalCent, 0L, "CNY",
                    "unpaid", "unsettled", null, null, now, now));
        });
        return billId;
    }

    private VoucherInstanceRecord seedVoucher(String s, String merchantId, long faceValueCent) {
        Instant now = Instant.now();
        String definitionId = "vdef_vch_" + s;
        VoucherInstanceRecord instance = new VoucherInstanceRecord(
                "vi_vch_" + s, definitionId, "customer_vch", "VCVCH" + s, "marketing_grant", "vgrant_vch_" + s,
                "issued", now, now.plusSeconds(86400L), null, null, null, false, now, now);
        tx.executeWithoutResult(st -> {
            voucherRepository.insertDefinition(new VoucherDefinitionRecord(
                    definitionId, "满减券 " + s, "merchant_discount", "in_store_staff", "none", merchantId,
                    faceValueCent, null, null, null, null, 30, null, "active", true, now, now));
            voucherRepository.insertInstance(instance);
        });
        return instance;
    }

    private int redemptionCount(String instanceId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM voucher_redemption_records WHERE voucher_instance_id = ?", Integer.class, instanceId);
    }
}
