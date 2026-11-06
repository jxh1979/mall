package com.complex.platform.finance;

// 文件版本：V0.18

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

    /** 18F 第 80 条：合同所属项目（营业额抽成应收带合同项目）。default 返回 null 兼容既有测试双子。 */
    default String findContractProjectId(String contractId) {
        return null;
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
     * 回流营业额取数：只读 integration_order_syncs，该商户在 [from, to) 经营日窗口的餐饮、零售回流 order 与 refund，逐笔（155-R3）。
     * 第十八批 Q 段第 138 条 138-R2：渠道售出的门票回流已本地出票、计入门票；单店门票票款按 BSM:1347 不进抽成基数，通票不是商户成交。
     * default 返回空兼容既有测试双子。
     */
    default List<TurnoverSourceRow> listMerchantReflowTurnoverRows(String merchantId, Instant fromInclusive, Instant toExclusive) {
        return List.of();
    }

    /**
     * 第十八批 U 段第 155 条 155-R1（DEC-R8-54「实现须为每笔计入抽成基数的销售保存其原账期与所适用档位费率」）：营业额抽成基数的逐笔来源行。
     * kind：bar_bill 酒吧整单已确认商品实收（at＝结清时点）；bar_goods_refund 已退成的商品分项（金额取负，originId＝所退账单，
     * originAt＝所退账单收款完成时点）；bar_withheld_fee 月结服务费扣留（id＝账单号|营业日，带营业日，at 由调用方按营业日起点补）；
     * bar_deposit_forfeited 没收的订台定金（at＝没收时点）；service_order 已完成的服务点单（at＝下单时点）。
     * 155-R3：reflow_order 餐饮、零售回流订单（at＝落库时点）；reflow_refund 回流退款（金额取负，originId、originAt＝本系统所退原订单回流行及其落库时点，
     * 查不到为空）；voucher_settlement 券结算付款（at＝付款时点）。
     */
    record TurnoverSourceRow(String kind, String id, Instant at, LocalDate businessDate, long amountCent, String originId, Instant originAt) {
        TurnoverSourceRow withAt(Instant time) {
            return new TurnoverSourceRow(kind, id, time, businessDate, amountCent, originId, originAt);
        }

        TurnoverSourceRow plus(long cent) {
            return new TurnoverSourceRow(kind, id, at, businessDate, Math.addExact(amountCent, cent), originId, originAt);
        }
    }

    /** 155-R1：一笔销售在原账期当月累计中的位置所适用的档位费率分段（合计＝netAmountCent）。 */
    record TurnoverSaleSnapshot(String kind, String id, long netAmountCent, List<TurnoverRentMath.Segment> segments) {
    }

    /** 155-R1：已落库的快照及原月是否按保底收租（第十八批裁决 36）。 */
    record BilledTurnoverSale(TurnoverSaleSnapshot sale, boolean billedByMinimum) {
    }

    /**
     * 第十八批 P 段第 131 条：该商户以没收时点（deposit_settled_at）落在 [from, to) 内没收的订台定金，逐笔（155-R1）。
     * default 返回空兼容既有测试双子。
     */
    default List<TurnoverSourceRow> listForfeitedBarDepositRows(String merchantId, Instant fromInclusive, Instant toExclusive) {
        return List.of();
    }

    /**
     * 第十八批 Q 段第 138 条 138-R1：酒吧商户的平台内酒吧收入＝[from, to) 内结清的整单已确认商品收入（折后实收减服务费、小费分项）
     * ＋[firstDay, dayAfterLast) 营业日的月结服务费扣留数−[from, to) 内已退成的商品分项，逐行返回（155-R1）；来源未定抛 409 BAR_REVENUE_PENDING。
     * default 返回空兼容既有测试双子。
     */
    default List<TurnoverSourceRow> listBarPlatformTurnoverRows(String merchantId, Instant fromInclusive, Instant toExclusive,
            LocalDate firstDay, LocalDate dayAfterLast) {
        return List.of();
    }

    /**
     * 第十八批 Q 段第 138 条 138-R3：非酒吧商户已完成结算的券结算金额＝该商户为对手方的 partner_voucher 应付
     * （统一券 internal_price 核销、通票合作方券 internal_purchased 核销）在 [from, to) 内的实际付款，逐笔（155-R3）。
     * default 返回空兼容既有测试双子。
     */
    default List<TurnoverSourceRow> listSettledVoucherSettlementRows(String merchantId, Instant fromInclusive, Instant toExclusive) {
        return List.of();
    }

    /**
     * 第十八批 Q 段第 138 条 138-R4：酒吧商户已完成的服务点单（点舞等）售价全额，按下单时点在 [from, to) 归期，逐笔（155-R1）。
     * default 返回空兼容既有测试双子。
     */
    default List<TurnoverSourceRow> listCompletedServiceOrderRows(String merchantId, Instant fromInclusive, Instant toExclusive) {
        return List.of();
    }

    /** 155-R1：按 (来源类型, 来源 id) 取原账期快照。default 返回空兼容既有测试双子。 */
    default Optional<BilledTurnoverSale> findTurnoverSaleSnapshot(String kind, String id) {
        return Optional.empty();
    }

    /** 155-R1：写本期逐笔快照（同一笔已有快照的不覆盖），billedByMinimum＝本期按保底收租。default 空操作兼容既有测试双子。 */
    default void insertTurnoverSaleSnapshots(String merchantId, String period, List<TurnoverSaleSnapshot> snapshots,
            boolean billedByMinimum) {
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
        return listArApItemsForProjects(merchantId,projectId==null ? null : java.util.Set.of(projectId),keyword,page,size);
    }

    /** 项目集合与商户范围取交集；null 仅供平台全量，空集合没有可见记录。JDBC 下推相同筛选和总数。 */
    default PageResponse<ArApItemRecord> listArApItemsForProjects(
            String merchantId, java.util.Set<String> projectIds, String keyword, int page, int size) {
        return listArApItemsForProjects(merchantId,projectIds,keyword,page,size,null,null);
    }

    default PageResponse<ArApItemRecord> listArApItemsForProjects(
            String merchantId, java.util.Set<String> projectIds, String keyword, int page, int size,
            java.util.Set<String> payrollItemIds, String lockedDayCloseId) {
        // 第十八批 R 段 143-R4：lockedDayCloseId 只在 Jdbc 下推（内存替身的台账记录不带锁列，不按它筛）。
        java.util.List<ArApItemRecord> all = merchantId == null
                ? listArApItems() : listArApItemsByMerchant(merchantId);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        java.util.List<ArApItemRecord> filtered = all.stream()
                .filter(it -> projectIds == null || (it.projectId()!=null && projectIds.contains(it.projectId())))
                .filter(it -> !"staff_payroll".equals(it.sourceType()) || payrollItemIds==null || payrollItemIds.contains(it.id()))
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

    default Optional<ArApItemRecord> findCurrentPurchasePayable(String sourceType, String sourceId) {
        List<ArApItemRecord> current = listArApItems().stream()
                .filter(item -> "payable".equals(item.direction()) && sourceType.equals(item.sourceType())
                        && sourceId.equals(item.sourceId()) && item.amountCent() > 0
                        && !"reversed".equals(item.status()) && !isPurchasePayableReplaced(item.id()))
                .toList();
        if (current.size() > 1) throw com.complex.platform.common.api.ApiException.conflict(
                "PURCHASE_PAYABLE_AMBIGUOUS", "采购来源存在多张有效应付，请核实更正链");
        return current.stream().findFirst();
    }

    /** 该台账项是否已有派生行回指（origin_item_id＝该项：合同账单作废冲正、V344 补录红冲/重述）——这样的原行不再是有效款项。 */
    default boolean hasCorrectionItems(String itemId) {
        return listArApItems().stream().anyMatch(item -> itemId.equals(item.originItemId()));
    }

    default boolean isPurchasePayableReplaced(String itemId) {
        return listArApItems().stream().anyMatch(item -> itemId.equals(item.originItemId())
                && item.amountCent() < 0 && "reversed".equals(item.status()));
    }

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

    /**
     * TK-14（ENUM「异常状态的统一出口」：异常单据未处置完毕，所属日结不得确认锁定）：原核销落在该营业日窗口 [from, to)、
     * 仍待主管处置的票务核销异常是否存在。范围口径同 {@link #aggregateOfficialTicket(LocalDate, Instant, Instant, String, String)}：
     * merchant、bar 范围只看本商户；project 范围只看原核销所属订单属该综合体、核销商户不是酒吧的（第十八批 S 段第 161 条 161-R2）；
     * 其余平台口径。default 无库替身视为没有；Jdbc 覆写。
     */
    default boolean openTicketVerificationAnomalyExists(Instant from, Instant to, String scopeType, String scopeId) {
        return false;
    }

    /**
     * 第十六批第 16 条（ENUM「异常状态的统一出口」：异常单据未处置完毕，所属日结不得确认锁定）：开单或收款落在该营业日窗口 [from, to)、
     * 仍处于异常（payment_status='abnormal'）的酒吧账单是否存在。范围口径：merchant、bar 范围只看本商户，
     * project 范围不命中（酒吧逐店另出日结，161-R2），其余平台口径。
     * default 无库替身视为没有；Jdbc 覆写。
     */
    default boolean openBarBillAnomalyExists(Instant from, Instant to, String scopeType, String scopeId) {
        return false;
    }

    /** 第十八批第 1 条：酒吧账单逐家按各自营业日窗（barWindows 里的酒吧）判定，其余酒吧用 [from, to)。default 无库替身忽略逐家窗。 */
    default boolean openBarBillAnomalyExists(Instant from, Instant to, String scopeType, String scopeId,
            java.util.Map<String, Instant[]> barWindows) {
        return openBarBillAnomalyExists(from, to, scopeType, scopeId);
    }

    /**
     * 第十八批 Q 段第 144 条（BSM:1106「未结清订单阻止交班和日结」）：开单落在该营业日（酒吧逐家按各自营业日窗）、仍未结清的酒吧账单号
     * （settlement_status 不是 settled、merged、void，与关台闸同口径），最多 10 张。范围口径同酒吧账单异常：merchant、bar 范围只看本商户，project 范围不命中，其余平台口径。
     * default 无库替身视为没有；Jdbc 覆写。
     */
    default List<String> unsettledBarBillNos(Instant from, Instant to, String scopeType, String scopeId,
            java.util.Map<String, Instant[]> barWindows) {
        return List.of();
    }

    /**
     * 第十八批 R 段第 142 条 142-R2（ENUM:190「由当班收银员完成现金清点与盘收银，接班人与其对接完成交接，并由财务跟班跟进交接完成」；
     * BSM:1081「财务跟班见证后归档」）：开班落在该营业日（酒吧逐家按各自营业日窗）、财务尚未见证归档的收银班次（含开班中、未平账、未签字）
     * 是否存在。范围口径同酒吧账单异常：merchant、bar 范围只看本酒吧，project 范围不命中，其余平台口径。default 无库替身视为没有；Jdbc 覆写。
     */
    default boolean unfinishedBarShiftExists(Instant from, Instant to, String scopeType, String scopeId,
            java.util.Map<String, Instant[]> barWindows) {
        return false;
    }

    /**
     * 第十八批 R 段第 143 条 143-R6（ENUM:115「有差异待复核」；BSM:1081「出现任何金额的差额一律进复核、定位责任，不设容差」）：
     * 开班落在该营业日、有差异待复核（142-R1：已班结且 needs_review）的收银班次是否存在；窗口与范围同 {@link #unfinishedBarShiftExists}。
     * default 无库替身视为没有；Jdbc 覆写。
     */
    default boolean unbalancedBarShiftExists(Instant from, Instant to, String scopeType, String scopeId,
            java.util.Map<String, Instant[]> barWindows) {
        return false;
    }

    /**
     * 第十八批 S 段第 158 条 158-R1（ENUM:191「差异未关闭期间，高价值差异所涉营业日的日结不得确认锁定」「以该笔差异所涉金额与「高价值判定」条所配的
     * 同一金额阈值比较，高于阈值即高价值……阈值未配置一律按高价值处理」）：该营业日有没有未关闭的高价值对账差异。财务差异按所属批次的营业日、
     * status 不是 closed；第三方差异按所属批次对账期间覆盖该营业日（ENUM:187、BSM:1729 按营业日、按日对账）、status 不是 resolved、matched
     * （DEC-R4-2026-08-04-43 已删「已忽略」，ignored 不是关闭出口）；金额大于 thresholdCent 即高价值，thresholdCent 为 null 一律按高价值。
     * 范围口径同其余几道日结闸：merchant、bar 范围只看本商户（财务批次范围为该商户、第三方差异的回流门店为该商户）；project 范围只看该综合体
     * （财务批次范围为该综合体或其非酒吧商户、第三方差异的回流门店属该综合体且不是酒吧，161-R2）；其余平台口径。
     * default 无库替身视为没有；Jdbc 覆写。
     */
    default boolean openHighValueReconciliationDifferenceExists(LocalDate businessDate, String scopeType, String scopeId,
            Long thresholdCent) {
        return false;
    }

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

    /** 同一外部原付款的待退建账串行，防并发回调覆盖已办理结果。 */
    default void lockRefundSource(String refundId) { }

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

    /** FS-021-R6：按可触及项目集合（null=平台全量）与商户（null=不限）列退款。 */
    default List<FinanceRefundRecord> listRefundsForProjects(String merchantId, java.util.Set<String> projectIds) {
        throw new UnsupportedOperationException("退款仓储未实现按项目列表");
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

    /** 第十八批 R 段 143-R2：给日结记一类数据的最终标记（每类一行；不可撤销，不提供删改）。default 抛错，测试双子按需实现。 */
    default void saveDailyCloseFinalMark(String dailyCloseId, String category, Instant markedAt) {
        throw new UnsupportedOperationException("日结仓储未实现五类最终标记");
    }

    /** 写入一条六源汇总（幂等 ON CONFLICT(daily_close_id, source_stream)）。 */
    DailyCloseSummaryRecord saveDailyCloseSummary(DailyCloseSummaryRecord summary);

    /** 列出某日结批次的六源汇总。 */
    List<DailyCloseSummaryRecord> listDailyCloseSummaries(String dailyCloseId);

    /**
     * 第十八批 P 段第 133 条 133-R2：日结批次按营业日倒序分页。merchantId 非空只看该商户的 merchant、bar 范围；
     * projectIds 非空只看这些项目的 project 范围与项目下商户的 merchant、bar 范围，空集合没有可见记录；
     * 两者都为 null＝平台全量。第十八批 R 段 143-R1：businessDate、scopeType、scopeId 非空时再按营业日与范围过滤（页面查「待生成」）。
     * default 返回空页兼容既有测试双子。
     */
    default PageResponse<DailyCloseRecord> listDailyCloses(String merchantId, java.util.Set<String> projectIds,
            LocalDate businessDate, String scopeType, String scopeId, int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0L);
    }

    /**
     * 第十八批 V 段第 159 条 159-R1（第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」）：五类最终标记的检查单位。bar 非空＝酒吧单位
     * （id 即该酒吧，name 取商户名，project 取所属综合体）；否则为综合体单位（id＝project＝该综合体，name 取综合体名）。
     */
    record FinalMarkUnit(String bar, String id, String name, String project, Instant created) {
    }

    /** 159-R1：检查单位——在营未存档的酒吧，再加启用的综合体（逾期待办与下游闸同一集合）。default 返回空兼容既有测试双子。 */
    default List<FinalMarkUnit> listFinalMarkUnits() {
        return List.of();
    }

    /**
     * 159-R1：单位在 from～to（含两端）各营业日已标为最终的类别。barId 非空取该酒吧 bar、merchant 范围的日结，否则取 projectId 的 project 范围日结；
     * 同日多张日结取并集，没有日结的营业日没有键。default 返回空兼容既有测试双子。
     */
    default java.util.Map<LocalDate, java.util.Set<String>> finalMarkCategoriesByDay(String barId, String projectId, LocalDate from,
            LocalDate to) {
        return java.util.Map.of();
    }

    /**
     * 把 business_date 当日、且属于本次 scope 的 ar_ap_items 打上 locked_day_close_id。
     * 当 merchantId 非空时仅锁定该商户行（隔离）。返回打标行数。
     */
    int lockArApItemsForDailyClose(String dailyCloseId, LocalDate businessDate, String merchantId);

    /**
     * 第十八批 S 段第 161 条 161-R2（第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」）：综合体（project 范围）日结锁定时，给 business_date
     * 到期、未锁、属该综合体（台账项目，空则取商户所属项目）且商户不是酒吧的台账打锁（酒吧逐店另出日结）。返回打标行数。
     * default 无库替身沿用不分范围打锁；Jdbc 覆写。
     */
    default int lockProjectArApItemsForDailyClose(String dailyCloseId, LocalDate businessDate, String projectId) {
        return lockArApItemsForDailyClose(dailyCloseId, businessDate, null);
    }

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
     *       {@code scope_type IN ('merchant','bar') AND scope_id=:merchant_id} 的 locked 日结才拦截，
     *       即只受本商户封账约束，他商户封账不冻结本商户写入（第十八批 S 段第 161 条 161-R1：bar 范围同 merchant 范围，
     *       scope_id 即该酒吧商户）。</li>
     * </ul>
     * 第十八批 S 段第 161 条 161-R2 起 Jdbc 实现同 {@link #lockedDailyCloseExistsForDate(LocalDate, String, String)} 传空项目
     * （非酒吧商户另受其所属综合体的项目范围日结约束）。
     */
    boolean lockedDailyCloseExistsForDate(LocalDate businessDate, String merchantId);

    /**
     * 第十八批 S 段第 161 条 161-R2（第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」；BSM:1682 综合体与酒吧各配一套营业日；
     * ENUM:190「日结锁定后的补录须上级审批，且不改动已锁定的日结金额」）：带台账项目判封账——商户是酒吧按上一方法的商户口径（161-R1）；
     * 否则能定综合体（projectId，空则取商户所属项目）的，该综合体项目范围日结已锁定或已归档即拦（商户自身的商户范围日结照旧也算）；
     * 商户与项目都定不出的照旧任一封账即拦。default 无库替身退回上一方法；Jdbc 覆写。
     */
    default boolean lockedDailyCloseExistsForDate(LocalDate businessDate, String merchantId, String projectId) {
        return lockedDailyCloseExistsForDate(businessDate, merchantId);
    }

    /**
     * 六源汇总聚合：按经营日窗口 [fromInclusive, toExclusive) 聚合源表，得到 amount/refund/count。
     * 窗口由 {@link com.complex.platform.common.BusinessDayResolver} 按营业日（出厂 10:00，按综合体／酒吧可配）派生；
     * official_ticket 以 visit_date（DATE 列）为经营日，故同时透传 businessDate。
     */
    DailyCloseSourceTotals aggregateOfficialTicket(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    DailyCloseSourceTotals aggregateChannelTicket(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    DailyCloseSourceTotals aggregateBar(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    DailyCloseSourceTotals aggregateAmusement(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    DailyCloseSourceTotals aggregateSupplyCost(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    DailyCloseSourceTotals aggregateThirdpartyReflow(LocalDate businessDate, Instant fromInclusive, Instant toExclusive);

    // ── P2-A：六源汇总必须按 scope 取数(与 lockArApItemsForDailyClose 口径对齐) ──
    // scopeType/scopeId 决定源表谓词:merchant、bar scope 只归本商户(161-R1),无 merchant 维的源(游乐/供应链/第三方)归零;
    // project scope 只归本综合体除酒吧外的业务(161-R2:酒吧两源归零);161-R3 起不再有 ticketing、amusement 范围。
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

    /** 第十八批第 1 条：酒吧来源逐家按各自营业日窗（barWindows 里的酒吧）取数，其余酒吧用 [from, to)。default 无库替身忽略逐家窗。 */
    default DailyCloseSourceTotals aggregateBar(LocalDate businessDate, Instant from, Instant to, String scopeType,
            String scopeId, java.util.Map<String, Instant[]> barWindows) {
        return aggregateBar(businessDate, from, to, scopeType, scopeId);
    }

    /**
     * 第十八批 O 段第 126 条 126-R1（BSM:990「……必须由授权员工记录到订单、账单、日结和对账里。」）：订台定金源——当日到账的定金（各渠道）
     * 计金额与笔数，当日实际退回成功的定金计退款；与酒吧源同按各酒吧营业日窗。default 无库替身归零。
     */
    default DailyCloseSourceTotals aggregateBarDeposit(LocalDate businessDate, Instant from, Instant to, String scopeType,
            String scopeId, java.util.Map<String, Instant[]> barWindows) {
        return DailyCloseSourceTotals.empty();
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

    /**
     * 第十八批 R 段第 143 条 143-R4（ENUM:190「……在原营业日的日结锁定表单上关联显示该笔补录的金额与情况，两边互相可查」）：
     * 挂在该日结上的锁定后补录（locked_day_close_id），按申请时间倒序。default 返回空兼容既有测试双子。
     */
    default List<ArApAdjustmentRecord> listAdjustmentsByDailyClose(String dailyCloseId) {
        return List.of();
    }

    /** 六源聚合结果（金额分 / 退款分 / 计数）。 */
    record DailyCloseSourceTotals(long amountCent, long refundCent, int orderCount) {
        public static DailyCloseSourceTotals empty() {
            return new DailyCloseSourceTotals(0L, 0L, 0);
        }
    }
}
