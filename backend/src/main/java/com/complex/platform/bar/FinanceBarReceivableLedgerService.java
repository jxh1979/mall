package com.complex.platform.bar;

import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApItemRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class FinanceBarReceivableLedgerService implements BarReceivableLedgerService {

    private final FinanceService financeService;

    FinanceBarReceivableLedgerService(FinanceService financeService) {
        this.financeService = financeService;
    }

    @Override
    public boolean isBusinessDayLocked(LocalDate businessDate) {
        return financeService.isBusinessDayLocked(businessDate);
    }

    @Override
    public String createReceivableForOnAccountBill(
            BarBillRecord bill,
            BarOrderRecord order,
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds
    ) {
        LocalDate today = LocalDate.now();
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-on-account-" + bill.id(),
                "bar bill on account receivable: " + bill.id(),
                List.of(),
                "receivable",
                counterpartyId(bill, order),
                "bar_bill",
                bill.id(),
                bill.payableAmountCent(),
                today.plusDays(30),
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    @Override
    public String createPayableForCommission(BarCommissionCalculationRecord commission) {
        LocalDate today = LocalDate.now();
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-commission-" + commission.id(),
                "bar table commission payable: " + commission.id(),
                List.of(),
                "payable",
                commission.salesStaffId(),
                "employee_commission",
                commission.id(),
                commission.commissionAmountCent(),
                today.plusDays(30),
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    @Override
    public String createRefundPayable(
            BarBillRecord bill,
            BarOrderRecord order,
            String adjustmentId,
            long refundAmountCent,
            String idempotencyKey,
            String reason,
            List<String> evidenceAttachmentIds
    ) {
        LocalDate today = LocalDate.now();
        // RC1①：退款过账为 direction='payable'、source_type='refund' 的应付台账项（"应退/已退"）。
        // source_id 取退款调整记录 id（唯一），与 V008 唯一索引 (direction, source_type, source_id) 对齐，幂等。
        // 退款过账与退款主流程（V079 减项）同事务执行（applyBillAdjustment @Transactional / createArApItem @Transactional
        // 复用同一外层事务）：过账失败将回滚退款，保证财务台账与账单减项强一致，不出现"退了钱但台账无记录"。
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-refund-" + adjustmentId,
                "bar bill refund payable: " + bill.id() + " (" + reason + ")",
                evidenceAttachmentIds == null ? List.of() : List.copyOf(evidenceAttachmentIds),
                "payable",
                counterpartyId(bill, order),
                "refund",
                adjustmentId,
                refundAmountCent,
                today,
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    @Override
    public String reverseRefundPayable(
            BarBillRecord bill,
            BarOrderRecord order,
            String reversalAdjustmentId,
            long refundAmountCent,
            String idempotencyKey,
            String reason,
            List<String> evidenceAttachmentIds
    ) {
        LocalDate today = LocalDate.now();
        // 延期项④：驳回冲销过账为 direction='receivable'、source_type='refund' 的对冲应收项。
        // source_id 取驳回冲销调整记录 id（与原退款应付项 source_id 不同），direction 亦不同，
        // 与 V008 唯一索引 (direction, source_type, source_id) 对齐，冲销项与原退款项净额相抵。
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-refund-reversal-" + reversalAdjustmentId,
                "bar bill refund reversal (void rejected): " + bill.id() + " (" + reason + ")",
                evidenceAttachmentIds == null ? List.of() : List.copyOf(evidenceAttachmentIds),
                "receivable",
                counterpartyId(bill, order),
                "refund",
                reversalAdjustmentId,
                refundAmountCent,
                today,
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    @Override
    public String createPayableForGiftSettlement(String verificationId, String merchantId, long internalPriceCent) {
        LocalDate today = LocalDate.now();
        // 通票免费酒水：核销发放 → 平台按内部结算价向酒吧(merchant)挂应付。源/幂等键 = verificationId（一次核销至多一笔）。
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-gift-settle-" + verificationId,
                "platform-to-bar pass drink settlement: verification " + verificationId,
                List.of(),
                "payable",
                merchantId,
                "pass_drink_settlement",
                verificationId,
                internalPriceCent,
                today.plusDays(30),
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    @Override
    public String reverseGiftSettlementPayable(String verificationId, String merchantId, long internalPriceCent) {
        LocalDate today = LocalDate.now();
        // 赠饮冲正 → 对冲应收（同 source_id=verificationId、direction 不同 → 与原应付 V008 唯一索引不冲突且净额归零）。
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-gift-settle-reverse-" + verificationId,
                "reverse platform-to-bar pass drink settlement: verification " + verificationId,
                List.of(),
                "receivable",
                merchantId,
                "pass_drink_settlement",
                verificationId,
                internalPriceCent,
                today.plusDays(30),
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    private String counterpartyId(BarBillRecord bill, BarOrderRecord order) {
        if (order.customerId() != null && !order.customerId().isBlank()) {
            return order.customerId();
        }
        if (bill.reservationId() != null && !bill.reservationId().isBlank()) {
            return bill.reservationId();
        }
        return bill.tableId();
    }
}
