package com.complex.platform.dividend;

import com.complex.platform.common.api.PageResponse;
import com.complex.platform.dividend.DividendRecords.DividendPlanRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementLineRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 持股分红仓储：方案配置 / 计算单头（状态机列更新） / 明细行（仅追加） / 三来源期间取数。 */
public interface DividendRepository {

    // ── 方案配置 ──────────────────────────────────────────────
    List<DividendPlanRecord> listPlans(String merchantId);

    Optional<DividendPlanRecord> findPlanById(String id);

    Optional<DividendPlanRecord> findActivePlanByMerchant(String merchantId);

    List<DividendPlanRecord> listActivePlans();

    /** 插入方案；同商户已有 active 方案时（部分唯一索引冲突）返回 false。 */
    boolean insertPlan(DividendPlanRecord plan);

    /** 更新方案；ended 改回 active 撞同商户唯一生效索引时返回 false（insertPlan 同口径）。 */
    boolean updatePlan(DividendPlanRecord plan);

    // ── V309 自定义扣除项（方案级配置，随方案整表替换）────────────────────────
    /** 某方案的自定义扣除项（按 sort_order 升序）；供读方案与算式取数。 */
    List<DividendRecords.DividendCustomDeductionRecord> listCustomDeductions(String planId);

    /** 整表替换某方案的自定义扣除项（先删后插）；配置改动不影响历史单（历史靠算式明细快照）。 */
    void replaceCustomDeductions(String planId, List<DividendRecords.DividendCustomDeductionRecord> items);

    // ── 计算单 ────────────────────────────────────────────────
    PageResponse<DividendStatementRecord> pageStatements(
            String merchantId, String kind, String periodLabel, String status, int page, int pageSize);

    Optional<DividendStatementRecord> findStatementById(String id);

    /** 同商户同类别同期的未作废计算单（uq_dividend_statements_period 谓词同款）。 */
    Optional<DividendStatementRecord> findActiveStatement(String merchantId, String kind, String periodLabel);

    /**
     * V303 亏损结转期初累计：该商户 period_start 之前最近一张已结（approved/paid）正式单的
     * loss_carryforward_after_cent；无则 0。累计不另建可变台账——已结正式单快照即账本，
     * 天然幂等（draft/preview/voided 不计）+ voided 自动回滚。
     */
    long priorLossCarryforwardCent(String merchantId, java.time.LocalDate periodStart);

    void insertStatement(DividendStatementRecord statement);

    void insertLine(DividendStatementLineRecord line);

    /** 明细行按单读取；merchantId 非空时叠加商户归属谓词（商户 scope 纵深）。 */
    List<DividendStatementLineRecord> listLines(String statementId, String merchantId);

    // 仅更新状态机/审批留痕列（金额列冻结：本仓储不提供任何金额列 UPDATE）。
    // C-1 资金安全：每条 UPDATE 都带期望前态谓词（乐观守卫），返回影响行数；
    // 0 行 = 并发下状态已被他人变更，调用方必须按业务冲突处理，禁止盲改。

    /** 作废：仅 preview/draft/rejected 可作废（进入审批流的单不作废）。返回影响行数。 */
    int markVoided(String id, String actor, Instant now);

    /** draft → pending_review。返回影响行数（0=当前非 draft）。 */
    int markSubmitted(String id, String actor, Instant now);

    /** pending_review → approved。返回影响行数（0=当前非 pending_review）。 */
    int markApproved(String id, String actor, String arApItemId, Instant now);

    /** pending_review → rejected（落 rejected_by/rejected_at）。返回影响行数（0=当前非 pending_review）。 */
    int markRejected(String id, String actor, String reason, Instant now);

    /** approved → paid。返回影响行数（0=当前非 approved）。 */
    int markPaid(String id, String actor, Instant now);

    // ── V304 发放保护线留存·释放 ──────────────────────────────
    /** 释放台账入库；uq(statement_id) 冲突（同一计算单重复释放）返回 false。 */
    boolean insertReserveRelease(DividendRecords.DividendReserveReleaseRecord release);

    /**
     * 待释放/已释放留存台账分页：有留存的正式单（approved/paid + reserve_withheld_cent>0）LEFT JOIN 释放台账。
     * releasedFilter：'pending'=仅待释放 / 'released'=仅已释放 / null=全部；merchantId 非空叠加商户谓词。
     */
    PageResponse<DividendRecords.DividendReserveLedgerRecord> pageReserveLedger(
            String merchantId, String releasedFilter, int page, int pageSize);

    /** 自动释放候选：待释放留存（approved/paid + reserve>0 + 未释放）+ 所属方案释放规则，供定时作业判到期。 */
    List<DividendRecords.DividendReserveReleaseCandidateRecord> listReserveReleaseCandidates();

    // ── 三来源期间取数（成本侧走 InventoryRepository.sumOutboundCost，不在本仓储）──
    /** 酒吧实收：bar_bill_settlements 结账流水（reversed=false），settled_at 落窗口（revenue.net 商户行级同谓词区间版）。 */
    long sumBarRevenueCent(String merchantId, Instant from, Instant to);

    /** 酒吧退款：bar_bill_adjustments(adjustment_type='refund') 经 bar_bills 归商户，refunded_at 落窗口。 */
    long sumBarRefundCent(String merchantId, Instant from, Instant to);

    /** 售票分量：issued_entitlements.allocated_revenue_cent（NULL 不计），created_at 落窗口。 */
    long sumTicketAllocatedCent(String merchantId, Instant from, Instant to);

    /** 售票退款：ticket_refunds（V220 直挂 merchant_id），created_at 落窗口。 */
    long sumTicketRefundCent(String merchantId, Instant from, Instant to);

    /** 费用净额：V297 merchant_expense_entries 按月份桶 + 计入类别分类型汇总（冲销行为负值自然抵减）。 */
    List<DividendRecords.ExpenseTypeSumRecord> sumExpenseByType(
            String merchantId, List<String> months, List<String> expenseTypes);
}
