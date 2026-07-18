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

    void updatePlan(DividendPlanRecord plan);

    // ── 计算单 ────────────────────────────────────────────────
    PageResponse<DividendStatementRecord> pageStatements(
            String merchantId, String kind, String periodLabel, String status, int page, int pageSize);

    Optional<DividendStatementRecord> findStatementById(String id);

    /** 同商户同类别同期的未作废计算单（uq_dividend_statements_period 谓词同款）。 */
    Optional<DividendStatementRecord> findActiveStatement(String merchantId, String kind, String periodLabel);

    void insertStatement(DividendStatementRecord statement);

    void insertLine(DividendStatementLineRecord line);

    /** 明细行按单读取；merchantId 非空时叠加商户归属谓词（商户 scope 纵深）。 */
    List<DividendStatementLineRecord> listLines(String statementId, String merchantId);

    /** 仅更新状态机/审批留痕列（金额列冻结：本仓储不提供任何金额列 UPDATE）。 */
    void markVoided(String id, String actor, Instant now);

    void markSubmitted(String id, String actor, Instant now);

    void markApproved(String id, String actor, String arApItemId, Instant now);

    void markRejected(String id, String actor, String reason, Instant now);

    void markPaid(String id, String actor, Instant now);

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
