package com.complex.platform.dividend;

// 文件版本：V0.04

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

    /**
     * 第十八批 V 段第 159 条 159-R10（BSM:1777「每月另出一张只读的预览单，让主理人提前看到当月大致情况（预览只能看，不参与审批、不发放）」
     * 「标记完成即自动生成」）：preview 为真时，尚未月结定格（扣留数未定）的服务费分项暂不计、不拦；商品收入来源未定照旧拦。
     * preview 为假即三参版（BSM:1076「月结定格的扣留部分，扣留金额计入该商户营业额、抽成基数与分红净利润基数」，正式单照旧要求月结定格）。
     */
    default long sumBarRevenueCent(String merchantId, Instant from, Instant to, boolean preview) {
        return sumBarRevenueCent(merchantId, from, to);
    }

    /** 酒吧退款：bar_bill_adjustments(adjustment_type='refund') 经 bar_bills 归商户，refunded_at 落窗口。 */
    long sumBarRefundCent(String merchantId, Instant from, Instant to);

    /**
     * FS-015 已核销门票结算额：期间内已确认（confirmed/settled）月度分账单上该商户明细 final_amount_cent 合计
     * （单一酒吧门票直付额 + 通票池分配额；未核销票款、通票未核销减除额、退票均不在其中）。
     */
    long sumTicketSettledCent(String merchantId, java.time.LocalDate periodStart, java.time.LocalDate periodEnd);

    /** FS-015 期间内尚无已确认（confirmed/settled）月度分账单的月份（yyyy-MM；缺单或超池待特批 draft）。 */
    List<String> listUnconfirmedTicketSettlementMonths(List<String> months);

    /** FS-013/FS-015 单一酒吧门票综合服务费：该酒吧计费月落在期间内的月应收总额合计（按售出月、退票剔除后封顶）。 */
    long sumSingleBarServiceFeeCent(String merchantId, java.time.LocalDate periodStart, java.time.LocalDate periodEnd);

    // ── V507 FS-014 依法可分配金额 ─────────────────────────────
    Optional<DividendRecords.DividendLegalConfirmationRecord> findLegalConfirmation(String merchantId, String periodLabel);

    /** 同商户同期重登记覆盖（uq(merchant_id, period_label)）。 */
    void upsertLegalConfirmation(DividendRecords.DividendLegalConfirmationRecord confirmation);

    /** 费用净额：V297 merchant_expense_entries 按月份桶 + 计入类别分类型汇总（冲销行为负值自然抵减）。 */
    List<DividendRecords.ExpenseTypeSumRecord> sumExpenseByType(
            String merchantId, List<String> months, List<String> expenseTypes);

    // ── V616 210-R3 该期税费与第十八批裁决 42 出单闸（经营主体同 210-R1 口径：生效绑定的运营主体，否则商户档案上的经营主体）──
    /** 方案商户经营主体已生效（approved）、所属期末月落在 months 内的税费实际申报金额，按税种汇总（按季申报的整季计入季末那个月）。 */
    List<DividendRecords.PeriodTaxSumRecord> sumEntityTaxByType(String merchantId, List<String> months);

    /** months 中没有被方案商户经营主体任一已生效申报登记的所属期（首月～末月）覆盖的月份，升序。 */
    List<String> listMonthsWithoutApprovedTax(String merchantId, List<String> months);

    // ── V616 211-R1 整期暂不分红／暂缓分红登记（第十八批裁决 43）──
    /** 该商户该期间未恢复的登记（同一商户同一期间至多一条）。 */
    Optional<DividendRecords.DividendPeriodHoldRecord> findOpenHold(String merchantId, String periodLabel);

    Optional<DividendRecords.DividendPeriodHoldRecord> findHoldById(String id);

    /** 新登记；该商户该期间已有未恢复的登记（部分唯一索引）返回 false。 */
    boolean insertHold(DividendRecords.DividendPeriodHoldRecord hold);

    /** 恢复暂缓分红：只改未恢复的 defer 行，返回改动行数。 */
    int liftHold(String id, String actor, String liftResult, Instant now);

    /** 全部登记，最近登记在前。 */
    List<DividendRecords.DividendPeriodHoldRecord> listHolds();
}
