package com.complex.platform.dividend;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 持股分红域记录集合（方案配置 / 计算单头 / 算式明细行 / 计算结果）。 */
public final class DividendRecords {

    private DividendRecords() {
    }

    /** 分红方案配置（商户/主理人维度；凡涉数据皆可配，80/20 与 20% 税率仅默认值）。 */
    public record DividendPlanRecord(
            String id,
            String merchantId,
            String principalUserId,
            String principalName,
            int platformShareBps,
            int taxRateBps,
            String periodType,
            List<String> revenueDomains,
            boolean includeInventoryCost,
            List<String> expenseTypes,
            LocalDate effectiveFrom,
            String status,
            String note,
            String updatedBy,
            Instant createdAt,
            Instant updatedAt,
            // V303 扣除项三开关 + 发放保护线参数（默认全关；关时不改 V299 口径）。
            boolean lossCarryforwardEnabled,
            boolean arrearsOffsetEnabled,
            boolean payoutFloorEnabled,
            int payoutFloorBps,
            long payoutFloorFixedCent) {
    }

    /** 期间分红计算单头（配置快照 + 算式结果 + 状态机）。 */
    public record DividendStatementRecord(
            String id,
            String merchantId,
            String planId,
            String kind,
            String periodLabel,
            LocalDate periodStart,
            LocalDate periodEnd,
            String principalUserId,
            int platformShareBps,
            int taxRateBps,
            List<String> revenueDomains,
            boolean includeInventoryCost,
            List<String> expenseTypes,
            long revenueCent,
            long costCent,
            long expenseCent,
            long netProfitCent,
            long distributableCent,
            long platformShareCent,
            long principalGrossCent,
            long taxCent,
            long principalNetCent,
            long noCostBasisCount,
            String status,
            String rejectReason,
            String arApItemId,
            String computedBy,
            String submittedBy,
            Instant submittedAt,
            String approvedBy,
            Instant approvedAt,
            String paidBy,
            Instant paidAt,
            Instant createdAt,
            Instant updatedAt,
            // V303 三扣除入账留痕（冻结在单上，供逐项算式追溯；关时全 0，实发=税后应得）。
            boolean lossCarryforwardEnabled,
            long lossCarryforwardInCent,
            long lossCarryforwardAppliedCent,
            long lossCarryforwardAfterCent,
            long arrearsOffsetCent,
            long reserveWithheldCent,
            long payoutCent) {

        /** 商户侧（主理人视图）脱敏：内部操作员 id 与应付款项 id 不外发（平台侧照旧全量）。 */
        @com.fasterxml.jackson.annotation.JsonIgnore
        public DividendStatementRecord forMerchantView() {
            return new DividendStatementRecord(id, merchantId, planId, kind, periodLabel, periodStart, periodEnd,
                    principalUserId, platformShareBps, taxRateBps, revenueDomains, includeInventoryCost,
                    expenseTypes, revenueCent, costCent, expenseCent, netProfitCent, distributableCent,
                    platformShareCent, principalGrossCent, taxCent, principalNetCent, noCostBasisCount, status,
                    rejectReason, null, null, null, submittedAt, null, approvedAt, null, paidAt,
                    createdAt, updatedAt,
                    lossCarryforwardEnabled, lossCarryforwardInCent, lossCarryforwardAppliedCent,
                    lossCarryforwardAfterCent, arrearsOffsetCent, reserveWithheldCent, payoutCent);
        }
    }

    /** 算式明细行（append-only；detail 说明取数口径，可追溯到三个来源台账）。 */
    public record DividendStatementLineRecord(
            String id,
            String statementId,
            String merchantId,
            int seq,
            String lineType,
            long amountCent,
            String detail,
            Instant createdAt) {
    }

    /** 计算单详情 = 头 + 算式明细行。 */
    public record DividendStatementDetailRecord(
            DividendStatementRecord statement,
            List<DividendStatementLineRecord> lines) {
    }

    /** 费用侧分类型净额（V297 台账期间汇总，供逐类落算式明细行）。 */
    public record ExpenseTypeSumRecord(String expenseType, long totalCent) {
    }

    /** 期间作业跳过项（商户 + 原因，如已进入审批流不重算）。 */
    public record DividendComputeSkipRecord(String merchantId, String reason) {
    }

    /** 期间作业结果：本次生成的计算单 + 跳过清单。 */
    public record DividendComputeResultRecord(
            String periodLabel,
            String kind,
            List<DividendStatementRecord> computed,
            List<DividendComputeSkipRecord> skipped) {
    }
}
