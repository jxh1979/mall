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
