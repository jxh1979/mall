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
