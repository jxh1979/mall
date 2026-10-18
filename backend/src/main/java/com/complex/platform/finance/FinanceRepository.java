package com.complex.platform.finance;

// 文件版本：V0.02

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.complex.platform.common.api.PageResponse;

public interface FinanceRepository {

    // ── P1#13 库存差异 → AR/AP（V108）─────────────────────────────────────────

    /** 按 id 查库存差异（入账前校验存在 + 复核状态 + 幂等）。default 空实现兼容既有测试双子。 */
    default Optional<InventoryVarianceRecord> findInventoryVarianceById(String id) {
        return Optional.empty();
    }

    /**
     * P1 修复：条件回填库存差异已入账。仅当 posted_ar_ap_item_id IS NULL 时才 UPDATE，返回是否成功（受影响行数==1）。
     * 并发双开时只有一个事务能把 NULL→arApItemId，另一个 UPDATE 命中 0 行返回 false，服务层据此抛冲突，
     * 杜绝同一 variance 重复挂应付台账。default 恒返回 true，兼容既有测试双子。
     */
    default boolean markInventoryVariancePostedIfUnposted(String inventoryVarianceId, String arApItemId) {
        return true;
    }

    // ── P1#14 综合体计费（V110）：营业额抽成 / 保证金 / CAM ───────────────────

    /** 按 id 查营业额抽成规则。default 空实现兼容既有测试双子。 */
    default Optional<TurnoverRentRuleRecord> findTurnoverRentRuleById(String id) {
        return Optional.empty();
    }

    /** 按合同查其生效(active)提成规则,取最新生效。default 空兼容测试双子。 */
    default Optional<TurnoverRentRuleRecord> findActiveTurnoverRentRuleByContract(String contractId) {
        return Optional.empty();
    }

    /** 按合同、商户和完整账期查已复核规则；被新版本替换的历史规则仍参与期间选择。 */
    default List<TurnoverRentRuleRecord> findTurnoverRentRulesForPeriod(
            String contractId, String merchantId, LocalDate periodStart, LocalDate periodEnd) {
        return List.of();
    }

    /** 保存提成租金规则。default no-op 兼容测试双子。 */
    /**
     * P0-E（v7 审计·抽成规则草案闸）：建/改抽成规则一律<b>落草案</b>（is_draft=TRUE）+ 记起草人 updated_by。
     * 转正走结算配置中心 {@code POST /api/v1/settlement/config/turnover_rent_rule/{id}/activate}（差人复核）。
     * default 委托旧直写实现，兼容既有测试双子。
     */
    default TurnoverRentRuleRecord saveTurnoverRentRuleDraft(TurnoverRentRuleRecord rule, String updatedBy) {
        return saveTurnoverRentRule(rule);
    }

    /**
     * P0-E：该抽成规则是否仍是未转正草案。草案不得用于计费（billTurnoverRent 据此 fail-loud）。
     * default 返回 false 以兼容既有测试双子。
     */
    default boolean isTurnoverRentRuleDraft(String id) {
        return false;
    }

    default TurnoverRentRuleRecord saveTurnoverRentRule(TurnoverRentRuleRecord rule) {
        return rule;
    }

    /** 保存提成租金阶梯档位。default no-op 兼容测试双子。 */
    default TurnoverRentRuleTierRecord saveTurnoverRentRuleTier(TurnoverRentRuleTierRecord tier) {
        return tier;
    }

    /** 列某规则阶梯档位(seq 升序)。default 空兼容测试双子。 */
    default List<TurnoverRentRuleTierRecord> listTurnoverRentRuleTiers(String ruleId) {
        return List.of();
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

    // ── F-OPS-08 物业费固定计费（V150，替代已废止 CAM）────────────────────────────

    /** upsert 物业费费率（ON CONFLICT(id) 更新单价/状态）。default 原样返回兼容既有测试双子。 */
    default PropertyFeeRateRecord savePropertyFeeRate(PropertyFeeRateRecord rate) {
        return rate;
    }

    /**
     * 解析某项目某期的生效费率：精确 effective_period 命中优先，否则回退到该项目默认费率（effective_period IS NULL），
     * 仅取 status='active'。无任何生效费率返回 empty。default 返回 empty 兼容既有测试双子。
     */
    default Optional<PropertyFeeRateRecord> findEffectivePropertyFeeRate(String projectId, String period) {
        return Optional.empty();
    }

    /** 列出某项目的所有物业费费率（按 effective_period，NULL 默认在前）。default 返回空兼容既有测试双子。 */
    default List<PropertyFeeRateRecord> listPropertyFeeRates(String projectId) {
        return List.of();
    }

    /** 只读跨域取商户面积（merchants.area_square_meter，NUMERIC(18,2)）；无此商户/面积为空返回 null。default null 兼容测试双子。 */
    default BigDecimal findMerchantAreaSquareMeter(String merchantId) {
        return null;
    }

    /** 某 (项目,期,商户) 是否已计过物业费（不退不补、幂等跳过依据）。default false 兼容既有测试双子。 */
    default boolean propertyFeeBillExists(String projectId, String period, String merchantId) {
        return false;
    }

    /** 写一条物业费计费留痕（ON CONFLICT(project_id,period,merchant_id) DO NOTHING 幂等）。default 原样返回兼容测试双子。 */
    default PropertyFeeBillRecord savePropertyFeeBill(PropertyFeeBillRecord bill) {
        return bill;
    }

    ArApItemRecord saveArApItem(ArApItemRecord item);

    /**
     * P0-B（V344）红冲专用纯追加写入：锁定后补录审批通过时落「冲正行 / 重述行」。
     * 与 {@link #saveArApItem} 的 upsert 不同，命中既有 id 一律不改写（append-only）。
     * default 委派 saveArApItem，供内存测试双子零改动兼容。
     */
    default ArApItemRecord appendArApItem(ArApItemRecord item) {
        return saveArApItem(item);
    }

    ArApItemSourceRecord saveArApItemSource(ArApItemSourceRecord source);

    List<ArApItemRecord> listArApItems();

    List<ArApItemRecord> listArApItemsByMerchant(String merchantId);

    /**
     * 服务端分页 + 关键词(跨 单号/方向/状态)筛选。merchantId 非空即商户 scope(WHERE merchant_id=),
     * 为 null 即平台全量;两分支都必须保持既有租户隔离谓词。返回真实 total。
     * default 兜底(内存 filter+slice)让测试假仓储无需改动;生产 Jdbc 实现 @Override 为真 SQL LIMIT/OFFSET。
     */
    default PageResponse<ArApItemRecord> listArApItemsPaged(String merchantId, String keyword, int page, int size) {
        java.util.List<ArApItemRecord> all = merchantId == null
                ? listArApItems() : listArApItemsByMerchant(merchantId);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        java.util.List<ArApItemRecord> filtered = all.stream()
                .filter(it -> kw.isEmpty()
                        || (it.itemNo() != null && it.itemNo().toLowerCase().contains(kw))
                        || (it.direction() != null && it.direction().toLowerCase().contains(kw))
                        || (it.status() != null && it.status().toLowerCase().contains(kw)))
                .toList();
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 20 : size;
        int from = Math.min((safePage - 1) * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        return new PageResponse<>(filtered.subList(from, to), safePage, safeSize, filtered.size());
    }

    /**
     * P1-A 财务项目级隔离:在 merchant 维之上叠加 project 维谓词(两维并存)。
     * merchantId 非空→WHERE merchant_id=;projectId 非空→AND project_id=;均空=平台全量。
     * default 兜底内存 filter,生产 Jdbc @Override 为真 SQL。
     */
    default PageResponse<ArApItemRecord> listArApItemsPaged(
            String merchantId, String projectId, String keyword, int page, int size) {
        java.util.List<ArApItemRecord> all = merchantId == null
                ? listArApItems() : listArApItemsByMerchant(merchantId);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        java.util.List<ArApItemRecord> filtered = all.stream()
                .filter(it -> projectId == null || projectId.equals(it.projectId()))
                .filter(it -> kw.isEmpty()
                        || (it.itemNo() != null && it.itemNo().toLowerCase().contains(kw))
                        || (it.direction() != null && it.direction().toLowerCase().contains(kw))
                        || (it.status() != null && it.status().toLowerCase().contains(kw)))
                .toList();
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 20 : size;
        int from = Math.min((safePage - 1) * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        return new PageResponse<>(filtered.subList(from, to), safePage, safeSize, filtered.size());
    }

    Optional<ArApItemRecord> findArApItemById(String id);

    /** Same as findArApItemById but holds a row-level write lock for the current transaction. */
    Optional<ArApItemRecord> findArApItemByIdForUpdate(String id);

    ArApItemRecord updateArApItemLedger(ArApItemRecord item);

    /**
     * V304 分红留存释放：某计算单的待释放留存应付（direction=payable, source_type='dividend_reserve_withheld',
     * source_id=statementId），FOR UPDATE 行锁（须在事务内调用）。default 兜底内存 filter 供测试双子。
     */
    default Optional<ArApItemRecord> findReserveWithheldByStatementForUpdate(String statementId) {
        return listArApItems().stream()
                .filter(i -> "payable".equals(i.direction())
                        && "dividend_reserve_withheld".equals(i.sourceType())
                        && statementId.equals(i.sourceId()))
                .findFirst();
    }

    /**
     * V304 分红留存释放：re-tag 应付项 source_type（前态守卫：仅当现值=expected 才翻，幂等）。返回影响行数。
     * default 兜底返回 0（测试双子无需真改；真 Jdbc @Override 为带守卫的 UPDATE）。
     */
    default int retagArApItemSourceType(
            String id, String expectedSourceType, String newSourceType, java.time.Instant now) {
        return 0;
    }

    /**
     * S6：按 (direction, source_type, source_id) 判断分录是否存在（对齐 uq_ar_ap_items_direction_source 幂等键维度）。
     * 用途：冲销一笔「从未挂出」的应付必须 no-op——原应付因商户让利型/自营吧等被跳过时，冲正不得凭空造一笔反向分录。
     * default 兜底内存 filter 供测试双子；真 Jdbc @Override 为 EXISTS 查询。
     */
    default boolean existsArApItemBySource(String direction, String sourceType, String sourceId) {
        return listArApItems().stream()
                .anyMatch(i -> direction.equals(i.direction())
                        && sourceType.equals(i.sourceType())
                        && sourceId.equals(i.sourceId()));
    }

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

    default long sumSucceededRefundAmountByTargetId(String targetId) {
        throw new UnsupportedOperationException("退款仓储未实现成功金额汇总");
    }

    DailyCloseRecord saveDailyClose(DailyCloseRecord dailyClose);

    boolean dailyCloseExists(LocalDate businessDate, String scopeType, String scopeId);

    PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent);

    /** 按 id 查支付意图（储值充值/会员开通绑支付意图：入账前校验支付意图存在且已成功）。 */
    default Optional<PaymentIntentRecord> findPaymentIntentByIdForUpdate(String id) {
        return findPaymentIntentById(id);
    }

    default Optional<PaymentIntentRecord> findPaymentIntentById(String id) {
        return Optional.empty();
    }

    OfflinePaymentConfirmationRecord saveOfflinePaymentConfirmation(OfflinePaymentConfirmationRecord confirmation);

    FinanceRefundRecord saveRefund(FinanceRefundRecord refund);

    /** 商户经营项目取主项目与追加项目的去重集合；退款不得用当前登录项目代替真实归属。 */
    default List<String> listMerchantOperatingProjectIds(String merchantId) {
        throw new UnsupportedOperationException("退款仓储未实现商户项目查询");
    }

    default Optional<FinanceRefundRecord> findRefundById(String id, boolean forUpdate) {
        throw new UnsupportedOperationException("退款仓储未实现读取");
    }

    default List<FinanceRefundRecord> listRefunds(String merchantId, String projectId) {
        throw new UnsupportedOperationException("退款仓储未实现列表");
    }

    default void appendRefundAttempt(FinanceRefundAttemptRecord attempt) {
        throw new UnsupportedOperationException("退款仓储未实现尝试留痕");
    }

    default boolean claimRefundAttempt(String attemptId) {
        throw new UnsupportedOperationException("退款仓储未实现派发认领");
    }

    default Optional<FinanceRefundAttemptRecord> findReadyRefundAttempt(String refundId) {
        throw new UnsupportedOperationException("退款仓储未实现待派发读取");
    }

    default List<FinanceRefundAttemptRecord> listRefundAttempts(String refundId) {
        throw new UnsupportedOperationException("退款仓储未实现尝试读取");
    }

    default List<ArApPaymentRecord> listArApPayments(String itemId) {
        throw new UnsupportedOperationException("财务仓储未实现原收款读取");
    }

    List<ReconciliationBatchRecord> listReconciliationBatches();

    ReconciliationBatchRecord saveReconciliationBatch(ReconciliationBatchRecord batch);

    List<ReconciliationDifferenceRecord> listReconciliationDifferences();

    ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord difference);

    Optional<ReconciliationDifferenceRecord> findReconciliationDifferenceById(String id);

    /** 正式关闭对账差异（1.6）：置 status=closed 并落 close_reason/closed_by/closed_at，返回更新后记录。 */
    ReconciliationDifferenceRecord closeReconciliationDifference(
            String id, String closeReason, String closedBy, java.time.Instant closedAt);

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

    /**
     * 该经营日是否已存在会锁定本调用方台账的 locked 日结（用于拦截向已锁定经营日新增/回填台账）。
     *
     * <p>F-OPS-09（裁决#9 日结锁商户级）：原实现只按 {@code business_date + status='locked'} 判定，
     * 无 scope 过滤，导致任一商户封账即冻结全平台该经营日的 AR/AP 写入。本方法与
     * {@link #lockArApItemsForDailyClose(String, java.time.LocalDate, String)} 的锁定口径对齐：
     * <ul>
     *   <li>{@code merchantId == null}（平台口径调用方）：该经营日任一 locked 日结即拦截
     *       （平台口径写入受所有封账约束，与平台锁定 locks-all 语义一致）。</li>
     *   <li>{@code merchantId != null}（商户 scope 调用方）：仅当存在
     *       {@code scope_type='merchant' AND scope_id=:merchant_id} 的 locked 日结才拦截，
     *       即只受本商户封账约束，他商户封账不冻结本商户写入。</li>
     * </ul>
     */
    boolean lockedDailyCloseExistsForDate(LocalDate businessDate, String merchantId);

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

    // ── P2-A：六源汇总必须按 scope 取数(与 lockArApItemsForDailyClose 口径对齐) ──
    // scopeType/scopeId 决定源表谓词:merchant scope 只归本商户,无 merchant 维的源(游乐/供应链/第三方)归零;
    // project scope 只归本综合体;其余 scopeType(bar/ticketing/amusement) 不额外切片(平台口径)。
    // default 委托无 scope 版=平台全量,生产 Jdbc @Override 下推真谓词。
    default DailyCloseSourceTotals aggregateOfficialTicket(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        return aggregateOfficialTicket(businessDate, from, to);
    }

    default DailyCloseSourceTotals aggregateChannelTicket(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        return aggregateChannelTicket(businessDate, from, to);
    }

    default DailyCloseSourceTotals aggregateBar(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        return aggregateBar(businessDate, from, to);
    }

    default DailyCloseSourceTotals aggregateAmusement(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        return aggregateAmusement(businessDate, from, to);
    }

    default DailyCloseSourceTotals aggregateSupplyCost(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        return aggregateSupplyCost(businessDate, from, to);
    }

    default DailyCloseSourceTotals aggregateThirdpartyReflow(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        return aggregateThirdpartyReflow(businessDate, from, to);
    }

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
