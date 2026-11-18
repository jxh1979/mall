package com.complex.platform.finance;

// 文件版本：V0.02

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 经营主体税费实际申报金额登记（第十八批 AC 段第 210 条 210-R2，第十八批裁决 42；V616 第 2 节 entity_tax_declarations）。
 * status：pending_review 待财税法审核与最高确认 → approved 已生效／rejected 已驳回（可重登）。
 * 第十八批 AR 段第 233 条（V645）：paidAmountCent、paymentDifferenceCent、paymentBreakdown＝最近一条已生效实缴登记的实际缴纳金额、缴税差异
 * （实际缴纳−更正后的实际申报）与差异分类，没有已生效实缴时为空；paymentPending＝有待审批的实缴登记；adjustments＝关联本申报的差异调整单（含待审与驳回）。
 * 233-R5（V647）：systemCalculatedAmountCent＝登记时现算的系统计算金额（其他税费为空）；declarationDifferenceCent＝申报−系统计算；
 * differenceMechanism＝adjustment 补提／冲回调整单（增值税、文化事业建设费、附加税）或 alignment 按季核对拉平（企业所得税、其他税费，第十八批裁决 59）；
 * calculationBasis＝逐月、逐收入类型的计算依据。
 * 233-R7：alignedAmountCent＝按季核对拉平税种（企业所得税、其他税费）已生效后本平台该税该期的金额，以财税软件结果为准＝申报额＋已生效差异调整单，
 * 其余为空。
 */
public record EntityTaxDeclaration(
        String id,
        String businessEntityId,
        String entityName,
        String taxType,
        String periodStartMonth,
        String periodEndMonth,
        long declaredAmountCent,
        String reason,
        List<String> evidenceAttachmentIds,
        String status,
        String reviewId,
        String createdBy,
        Instant createdAt,
        Instant approvedAt,
        Long paidAmountCent,
        Long paymentDifferenceCent,
        List<Breakdown> paymentBreakdown,
        boolean paymentPending,
        List<Adjustment> adjustments,
        Long systemCalculatedAmountCent,
        Long declarationDifferenceCent,
        String differenceMechanism,
        List<CalculationBasis> calculationBasis,
        Long alignedAmountCent
) {
    /**
     * 233-R5 系统计算依据一行（BSM:521「把所用配置版本固化在单上」）：所属月、收入类型、含税收入、比例与所用税率版本 id（settlement_tax_configs.id，
     * 没配或该版本没有这笔费用为空、比例记 0）、税额；附加税另带作为基数的系统计算增值税及其比例、版本 id。233-R7 企业所得税一行：月份为所属期末月（取该月月末日生效的比例）、
     * 收入类型 all、收入为所属期收入合计、税额＝max(0, 利润)×比例，profit 带收入与成本分项和利润（233-R6）。
     */
    public record CalculationBasis(String month, String incomeType, long incomeCent, int rateBps, String rateVersionId, long taxAmountCent,
            Long vatAmountCent, Integer vatRateBps, String vatRateVersionId, EntityProfitService.Profit profit) {
    }

    /** 233-R1 缴税差异分类（BSM:1760 五类：unpaid 未缴、prepaid 预缴、overpaid 多缴、offset 抵扣、refund_pending 待退），金额大于 0。 */
    public record Breakdown(String category, Long amountCent) {
    }

    /** 233-R1 实际缴纳金额登记（V645 entity_tax_payments）：paymentDifferenceCent＝实际缴纳−登记时更正后的实际申报金额。 */
    public record Payment(
            String id,
            String declarationId,
            long paidAmountCent,
            long paymentDifferenceCent,
            List<Breakdown> differenceBreakdown,
            String reason,
            String status,
            String reviewId
    ) {
    }

    /**
     * 233-R3 差异调整单（V645 entity_tax_adjustments）：kind period_difference 原期间已申报的更正（可正可负）、supplement 补提、reversal 冲回；
     * originPeriod* 关联原所属期；accountingMonth＝生效当月（北京时间），待审时为空。
     */
    public record Adjustment(
            String id,
            String declarationId,
            String kind,
            long amountCent,
            String originPeriodStartMonth,
            String originPeriodEndMonth,
            String accountingMonth,
            String reason,
            String status,
            String reviewId
    ) {
    }

    /** 233-R1 已生效行「登记实际缴纳」：paidAmountCent＝截至本次该申报累计实际缴纳；差额不为 0 时 differenceBreakdown 合计等于差额绝对值。 */
    public record PaymentRequest(
            String idempotencyKey,
            Long paidAmountCent,
            List<Breakdown> differenceBreakdown,
            String reason,
            List<String> evidenceAttachmentIds,
            List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines
    ) {
    }

    /** 233-R3 已生效行「生成差异调整单」：amountCent＝更正差额（多申报为负），不能为 0。 */
    public record AdjustmentRequest(
            String idempotencyKey,
            Long amountCent,
            String reason,
            List<String> evidenceAttachmentIds,
            List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines
    ) {
    }

    /**
     * 233-R2 税率版本（V646 settlement_tax_configs，按主体 × 收入类型 × 税种）：businessEntityId 为空＝平台默认；effectiveFrom 为空＝出厂种子、自始有效；
     * status 由 is_draft 与 status 两列换算：pending_review 待财税法审核与最高确认、approved 已生效、rejected 已驳回。
     */
    public record Rate(
            String id,
            String businessEntityId,
            String entityName,
            String incomeType,
            String taxType,
            boolean applicable,
            int rateBps,
            LocalDate effectiveFrom,
            String reason,
            String status,
            String reviewId
    ) {
    }

    /** 233-R2「提交税率版本」：rateBps 万分比 0～10000（页面按百分数输入后转换）；企业所得税 incomeType 固定 all；其他税费不配比例。 */
    public record RateRequest(
            String idempotencyKey,
            String businessEntityId,
            String incomeType,
            String taxType,
            Boolean applicable,
            Integer rateBps,
            LocalDate effectiveFrom,
            String reason,
            List<String> evidenceAttachmentIds,
            List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines
    ) {
    }

    /** 登记即提交审批：parallel_lines 为财务、税务、法务三条线各自的最终审批人（照 PayrollTaxVersion.SubmitRequest）。 */
    public record Request(
            String idempotencyKey,
            String businessEntityId,
            String taxType,
            String periodStartMonth,
            String periodEndMonth,
            Long declaredAmountCent,
            String reason,
            List<String> evidenceAttachmentIds,
            List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines
    ) {
    }
}
