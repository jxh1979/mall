package com.complex.platform.finance;

import java.util.List;
import java.util.Optional;

public interface FinanceRepository {
    ArApItemRecord saveArApItem(ArApItemRecord item);

    ArApItemSourceRecord saveArApItemSource(ArApItemSourceRecord source);

    List<ArApItemRecord> listArApItems();

    Optional<ArApItemRecord> findArApItemById(String id);

    ArApItemRecord updateArApItemLedger(ArApItemRecord item);

    ArApPaymentRecord saveArApPayment(ArApPaymentRecord payment);

    DailyCloseRecord saveDailyClose(DailyCloseRecord dailyClose);

    PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent);

    OfflinePaymentConfirmationRecord saveOfflinePaymentConfirmation(OfflinePaymentConfirmationRecord confirmation);

    FinanceRefundRecord saveRefund(FinanceRefundRecord refund);

    List<ReconciliationBatchRecord> listReconciliationBatches();

    ReconciliationBatchRecord saveReconciliationBatch(ReconciliationBatchRecord batch);

    List<ReconciliationDifferenceRecord> listReconciliationDifferences();

    ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord difference);
}
