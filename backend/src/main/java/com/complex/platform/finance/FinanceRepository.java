package com.complex.platform.finance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinanceRepository {
    ArApItemRecord saveArApItem(ArApItemRecord item);

    ArApItemSourceRecord saveArApItemSource(ArApItemSourceRecord source);

    List<ArApItemRecord> listArApItems();

    List<ArApItemRecord> listArApItemsByMerchant(String merchantId);

    Optional<ArApItemRecord> findArApItemById(String id);

    /** Same as findArApItemById but holds a row-level write lock for the current transaction. */
    Optional<ArApItemRecord> findArApItemByIdForUpdate(String id);

    ArApItemRecord updateArApItemLedger(ArApItemRecord item);

    ArApPaymentRecord saveArApPayment(ArApPaymentRecord payment);

    Optional<ArApPaymentRecord> findArApPaymentById(String id);

    long sumRefundedAmountByTargetId(String targetId);

    DailyCloseRecord saveDailyClose(DailyCloseRecord dailyClose);

    boolean dailyCloseExists(LocalDate businessDate, String scopeType, String scopeId);

    PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent);

    OfflinePaymentConfirmationRecord saveOfflinePaymentConfirmation(OfflinePaymentConfirmationRecord confirmation);

    FinanceRefundRecord saveRefund(FinanceRefundRecord refund);

    List<ReconciliationBatchRecord> listReconciliationBatches();

    ReconciliationBatchRecord saveReconciliationBatch(ReconciliationBatchRecord batch);

    List<ReconciliationDifferenceRecord> listReconciliationDifferences();

    ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord difference);
}
