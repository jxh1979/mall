package com.complex.platform.finance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinanceRepository {

    // ── P1#13 库存差异 → AR/AP（V108）─────────────────────────────────────────

    /** 按 id 查库存差异（入账前校验存在 + 复核状态 + 幂等）。default 空实现兼容既有测试双子。 */
    default Optional<InventoryVarianceRecord> findInventoryVarianceById(String id) {
        return Optional.empty();
    }

    /** 回填库存差异已入账（posted_ar_ap_item_id），用于幂等防重复入账。default 空实现兼容既有测试双子。 */
    default void markInventoryVariancePosted(String inventoryVarianceId, String arApItemId) {
    }

    /**
     * P1 修复：条件回填库存差异已入账。仅当 posted_ar_ap_item_id IS NULL 时才 UPDATE，返回是否成功（受影响行数==1）。
     * 并发双开时只有一个事务能把 NULL→arApItemId，另一个 UPDATE 命中 0 行返回 false，服务层据此抛冲突，
     * 杜绝同一 variance 重复挂应付台账。default 委托旧方法并恒返回 true，兼容既有测试双子。
     */
    default boolean markInventoryVariancePostedIfUnposted(String inventoryVarianceId, String arApItemId) {
        markInventoryVariancePosted(inventoryVarianceId, arApItemId);
        return true;
    }

    // ── P1#14 综合体计费（V110）：营业额抽成 / 保证金 / CAM ───────────────────

    /** 按 id 查营业额抽成规则。default 空实现兼容既有测试双子。 */
    default Optional<TurnoverRentRuleRecord> findTurnoverRentRuleById(String id) {
        return Optional.empty();
    }

    /**
     * 回流营业额取数：只读 integration_order_syncs，聚合该商户在 [from, to) 经营日窗口的回流 order 金额（分）。
     * default 返回 0 兼容既有测试双子。
     */
    default long aggregateMerchantReflowTurnover(String merchantId, Instant fromInclusive, Instant toExclusive) {
        return 0L;
    }

    /** 写一条保证金台账。default 空实现兼容既有测试双子。 */
    default DepositLedgerRecord saveDepositLedger(DepositLedgerRecord ledger) {
        return ledger;
    }

    /** 取该商户当前保证金余额（最新一行 balance_after_cent；无台账=0）。default 返回 0 兼容既有测试双子。 */
    default long currentDepositBalance(String merchantId) {
        return 0L;
    }

    /**
     * P1 修复：取该商户当前保证金余额并对最新台账行加 FOR UPDATE 行锁（必须在事务内调用）。
     * 并发 collect/deduct 在锁内串行化读取同一权威余额，避免读到旧余额各自 append 导致 balance_after 互覆盖丢更新、
     * 超额 deduct 校验被绕过。default 委托无锁实现，兼容既有测试双子。
     */
    default long currentDepositBalanceForUpdate(String merchantId) {
        return currentDepositBalance(merchantId);
    }

    /** 写一条 CAM 分摊行。default 空实现兼容既有测试双子。 */
    default CamAllocationRecord saveCamAllocation(CamAllocationRecord allocation) {
        return allocation;
    }

    // ── P2#④ CAM 年度 true-up（V118）：已预收归集 + 差额台账 ─────────────────────

    /**
     * 该商户在某 settlementPeriod 的 CAM 已预收合计（分）：按 cam_allocations.settlement_period 归集 allocated_amount_cent。
     * default 返回 0 兼容既有测试双子。
     */
    default long sumCamPrepaidByMerchantAndPeriod(String merchantId, String settlementPeriod) {
        return 0L;
    }

    /** 标记某 settlementPeriod 既有 CAM 预收行已被 true-up 纳入对账（trued_up=TRUE，防重复）。default 空实现兼容测试双子。 */
    default void markCamAllocationsTruedUp(String settlementPeriod) {
    }

    /** 写一条 CAM true-up 批次。default 原样返回兼容既有测试双子。 */
    default CamTrueUpRunRecord saveCamTrueUpRun(CamTrueUpRunRecord run) {
        return run;
    }

    /** 写一条 CAM true-up 逐户差额行。default 原样返回兼容既有测试双子。 */
    default CamTrueUpLineRecord saveCamTrueUpLine(CamTrueUpLineRecord line) {
        return line;
    }

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

    /**
     * P1 修复：同 findArApPaymentById，但对该 payment 行加 FOR UPDATE 行锁（必须在事务内调用）。
     * 退款前先锁定 target payment，使并发退款在累计校验前串行化，避免 sumRefundedAmount TOCTOU 导致退款合计超过 payment 金额。
     * default 委托无锁实现，兼容既有测试双子。
     */
    default Optional<ArApPaymentRecord> findArApPaymentByIdForUpdate(String id) {
        return findArApPaymentById(id);
    }

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

    /** 按 id 查对账批次（创建差异前校验所属批次存在）。default 空实现避免破坏既有测试双子。 */
    default Optional<ReconciliationBatchRecord> findReconciliationBatchById(String id) {
        return Optional.empty();
    }

    /**
     * 平台侧 expected 系统取数：以 business_ref 关联平台权威支付台账聚合应有金额（分）。
     * 返回 0 表示无平台侧系统取数支撑（服务层据此拒绝纯手填差异）。default 返回 0 以兼容既有测试双子。
     */
    default long aggregatePlatformExpectedForBusinessRef(String businessRef) {
        return 0L;
    }

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
