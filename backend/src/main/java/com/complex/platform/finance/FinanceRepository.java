package com.complex.platform.finance;

import java.time.Instant;
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

    /** 按 id 查支付意图（储值充值/会员开通绑支付意图：入账前校验支付意图存在且已成功）。 */
    default Optional<PaymentIntentRecord> findPaymentIntentById(String id) {
        return Optional.empty();
    }

    OfflinePaymentConfirmationRecord saveOfflinePaymentConfirmation(OfflinePaymentConfirmationRecord confirmation);

    FinanceRefundRecord saveRefund(FinanceRefundRecord refund);

    List<ReconciliationBatchRecord> listReconciliationBatches();

    ReconciliationBatchRecord saveReconciliationBatch(ReconciliationBatchRecord batch);

    List<ReconciliationDifferenceRecord> listReconciliationDifferences();

    ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord difference);

    // ── R5 T7：日结锁定 + 补录审批 + 多源汇总 ──────────────────────────────

    /** 返回 finance_daily_closes 单条（含 status），用于锁定/汇总查询。 */
    Optional<DailyCloseRecord> findDailyCloseById(String id);

    /** 写入一条六源汇总（幂等 ON CONFLICT(daily_close_id, source_stream)）。 */
    DailyCloseSummaryRecord saveDailyCloseSummary(DailyCloseSummaryRecord summary);

    /** 列出某日结批次的六源汇总。 */
    List<DailyCloseSummaryRecord> listDailyCloseSummaries(String dailyCloseId);

    /**
     * 把 business_date 当日、且属于本次 scope 的 ar_ap_items 打上 locked_day_close_id。
     * 当 merchantId 非空时仅锁定该商户行（隔离）。返回打标行数。
     */
    int lockArApItemsForDailyClose(String dailyCloseId, LocalDate businessDate, String merchantId);

    /** 查目标台账项当前的 locked_day_close_id（empty=未锁定），用于硬拦截。 */
    Optional<String> findArApItemLockedDayCloseId(String arApItemId);

    /** 该经营日是否已存在 locked 状态的日结（用于拦截向已锁定经营日新增/回填台账）。 */
    boolean lockedDailyCloseExistsForDate(LocalDate businessDate);

    /**
     * 六源汇总聚合：按经营日窗口 [fromInclusive, toExclusive) 聚合源表，得到 amount/refund/count。
     * 窗口由 {@link com.complex.platform.common.BusinessDayResolver} 按可配日切（默认 04:00）派生；
     * official_ticket 以 visit_date（DATE 列）为经营日，故同时透传 businessDate。
     */
    DailyCloseSourceTotals aggregateOfficialTicket(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    DailyCloseSourceTotals aggregateChannelTicket(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    DailyCloseSourceTotals aggregateBar(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    DailyCloseSourceTotals aggregateAmusement(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    DailyCloseSourceTotals aggregateSupplyCost(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    DailyCloseSourceTotals aggregateThirdpartyReflow(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    ArApAdjustmentRecord saveArApAdjustment(ArApAdjustmentRecord adjustment);

    Optional<ArApAdjustmentRecord> findArApAdjustmentByIdForUpdate(String id);

    ArApAdjustmentRecord updateArApAdjustment(ArApAdjustmentRecord adjustment);

    /** 六源聚合结果（金额分 / 退款分 / 计数）。 */
    record DailyCloseSourceTotals(long amountCent, long refundCent, int orderCount) {
        public static DailyCloseSourceTotals empty() {
            return new DailyCloseSourceTotals(0L, 0L, 0);
        }
    }
}
