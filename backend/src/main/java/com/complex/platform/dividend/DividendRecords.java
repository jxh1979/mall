package com.complex.platform.dividend;

// 文件版本：V0.03

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
            long payoutFloorFixedCent,
            // V304 发放保护线留存·释放规则（默认手动=不自动释放）。
            String reserveReleaseMode,
            int reserveReleaseDelayPeriods,
            // V308 主理人类型（individual 个人 / company 公司）：公司股东不代扣个税、全额发放。
            String principalType,
            // V309 自定义扣除项（读路径由仓储装填；写路径经方案请求整表替换）。
            List<DividendCustomDeductionRecord> customDeductions,
            // V616 211-R2 发放付账日：全平台统一每月三个付账日中的第几个（1～3，BSM:1787「由该公司在分红周期配置中指定」）。
            Integer payoutPaySlot) {

        /** 向后兼容构造（V616 之前的 25 参形态）：发放付账日未指定。 */
        public DividendPlanRecord(
                String id, String merchantId, String principalUserId, String principalName,
                int platformShareBps, int taxRateBps, String periodType, List<String> revenueDomains,
                boolean includeInventoryCost, List<String> expenseTypes, LocalDate effectiveFrom, String status,
                String note, String updatedBy, Instant createdAt, Instant updatedAt,
                boolean lossCarryforwardEnabled, boolean arrearsOffsetEnabled, boolean payoutFloorEnabled,
                int payoutFloorBps, long payoutFloorFixedCent, String reserveReleaseMode,
                int reserveReleaseDelayPeriods, String principalType, List<DividendCustomDeductionRecord> customDeductions) {
            this(id, merchantId, principalUserId, principalName, platformShareBps, taxRateBps, periodType,
                    revenueDomains, includeInventoryCost, expenseTypes, effectiveFrom, status, note, updatedBy,
                    createdAt, updatedAt, lossCarryforwardEnabled, arrearsOffsetEnabled, payoutFloorEnabled,
                    payoutFloorBps, payoutFloorFixedCent, reserveReleaseMode, reserveReleaseDelayPeriods,
                    principalType, customDeductions, null);
        }

        /** 向后兼容构造（V308/V309 之前的 23 参形态）：类型默认个人、自定义扣除空。 */
        public DividendPlanRecord(
                String id, String merchantId, String principalUserId, String principalName,
                int platformShareBps, int taxRateBps, String periodType, List<String> revenueDomains,
                boolean includeInventoryCost, List<String> expenseTypes, LocalDate effectiveFrom, String status,
                String note, String updatedBy, Instant createdAt, Instant updatedAt,
                boolean lossCarryforwardEnabled, boolean arrearsOffsetEnabled, boolean payoutFloorEnabled,
                int payoutFloorBps, long payoutFloorFixedCent, String reserveReleaseMode,
                int reserveReleaseDelayPeriods) {
            this(id, merchantId, principalUserId, principalName, platformShareBps, taxRateBps, periodType,
                    revenueDomains, includeInventoryCost, expenseTypes, effectiveFrom, status, note, updatedBy,
                    createdAt, updatedAt, lossCarryforwardEnabled, arrearsOffsetEnabled, payoutFloorEnabled,
                    payoutFloorBps, payoutFloorFixedCent, reserveReleaseMode, reserveReleaseDelayPeriods,
                    "individual", List.of(), null);
        }

        /** 向后兼容构造（V304 之前的 21 参形态）：释放规则默认手动、延后 1 期；类型默认个人、自定义扣除空。 */
        public DividendPlanRecord(
                String id, String merchantId, String principalUserId, String principalName,
                int platformShareBps, int taxRateBps, String periodType, List<String> revenueDomains,
                boolean includeInventoryCost, List<String> expenseTypes, LocalDate effectiveFrom, String status,
                String note, String updatedBy, Instant createdAt, Instant updatedAt,
                boolean lossCarryforwardEnabled, boolean arrearsOffsetEnabled, boolean payoutFloorEnabled,
                int payoutFloorBps, long payoutFloorFixedCent) {
            this(id, merchantId, principalUserId, principalName, platformShareBps, taxRateBps, periodType,
                    revenueDomains, includeInventoryCost, expenseTypes, effectiveFrom, status, note, updatedBy,
                    createdAt, updatedAt, lossCarryforwardEnabled, arrearsOffsetEnabled, payoutFloorEnabled,
                    payoutFloorBps, payoutFloorFixedCent, "manual", 1, "individual", List.of(), null);
        }
    }

    /**
     * V309 自定义扣除项（方案级配置）。sourceType=fixed 用 amountCent；ratio 用 ratioBps × ratioBasis
     * （net_profit 净利润 / revenue 营收）。enabled=false 不参与算式。
     */
    public record DividendCustomDeductionRecord(
            String id,
            String planId,
            String name,
            String sourceType,
            long amountCent,
            int ratioBps,
            String ratioBasis,
            boolean enabled,
            int sortOrder) {
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
            long payoutCent,
            // V308 主理人类型快照（公司股东单 taxCent 恒 0，chk_ds_company_no_tax 硬保证）。
            String principalType,
            // V309 自定义扣除合计快照（逐项留痕在明细行；发放额恒等含本项）。
            long customDeductionCent,
            // V507 FS-014 财务登记的依法可分配金额快照（未核定为空；本次分配 = min(经营候选额, 本额)）。
            Long legalDistributableCent,
            // V616 210-R3 该期税费：方案商户经营主体已生效、所属期末月落在本期内的税费实际申报金额合计（净利润已扣；逐税种见 tax_period 明细行）。
            long periodTaxCent) {

        /** 向后兼容构造（V616 之前的形态）：该期税费 0。 */
        public DividendStatementRecord(
                String id, String merchantId, String planId, String kind, String periodLabel,
                LocalDate periodStart, LocalDate periodEnd, String principalUserId, int platformShareBps,
                int taxRateBps, List<String> revenueDomains, boolean includeInventoryCost, List<String> expenseTypes,
                long revenueCent, long costCent, long expenseCent, long netProfitCent, long distributableCent,
                long platformShareCent, long principalGrossCent, long taxCent, long principalNetCent,
                long noCostBasisCount, String status, String rejectReason, String arApItemId, String computedBy,
                String submittedBy, Instant submittedAt, String approvedBy, Instant approvedAt, String paidBy,
                Instant paidAt, Instant createdAt, Instant updatedAt, boolean lossCarryforwardEnabled,
                long lossCarryforwardInCent, long lossCarryforwardAppliedCent, long lossCarryforwardAfterCent,
                long arrearsOffsetCent, long reserveWithheldCent, long payoutCent, String principalType,
                long customDeductionCent, Long legalDistributableCent) {
            this(id, merchantId, planId, kind, periodLabel, periodStart, periodEnd, principalUserId,
                    platformShareBps, taxRateBps, revenueDomains, includeInventoryCost, expenseTypes, revenueCent,
                    costCent, expenseCent, netProfitCent, distributableCent, platformShareCent, principalGrossCent,
                    taxCent, principalNetCent, noCostBasisCount, status, rejectReason, arApItemId, computedBy,
                    submittedBy, submittedAt, approvedBy, approvedAt, paidBy, paidAt, createdAt, updatedAt,
                    lossCarryforwardEnabled, lossCarryforwardInCent, lossCarryforwardAppliedCent,
                    lossCarryforwardAfterCent, arrearsOffsetCent, reserveWithheldCent, payoutCent, principalType,
                    customDeductionCent, legalDistributableCent, 0L);
        }

        /** 向后兼容构造（V507 之前的形态）：未核定依法可分配金额。 */
        public DividendStatementRecord(
                String id, String merchantId, String planId, String kind, String periodLabel,
                LocalDate periodStart, LocalDate periodEnd, String principalUserId, int platformShareBps,
                int taxRateBps, List<String> revenueDomains, boolean includeInventoryCost, List<String> expenseTypes,
                long revenueCent, long costCent, long expenseCent, long netProfitCent, long distributableCent,
                long platformShareCent, long principalGrossCent, long taxCent, long principalNetCent,
                long noCostBasisCount, String status, String rejectReason, String arApItemId, String computedBy,
                String submittedBy, Instant submittedAt, String approvedBy, Instant approvedAt, String paidBy,
                Instant paidAt, Instant createdAt, Instant updatedAt, boolean lossCarryforwardEnabled,
                long lossCarryforwardInCent, long lossCarryforwardAppliedCent, long lossCarryforwardAfterCent,
                long arrearsOffsetCent, long reserveWithheldCent, long payoutCent, String principalType,
                long customDeductionCent) {
            this(id, merchantId, planId, kind, periodLabel, periodStart, periodEnd, principalUserId,
                    platformShareBps, taxRateBps, revenueDomains, includeInventoryCost, expenseTypes, revenueCent,
                    costCent, expenseCent, netProfitCent, distributableCent, platformShareCent, principalGrossCent,
                    taxCent, principalNetCent, noCostBasisCount, status, rejectReason, arApItemId, computedBy,
                    submittedBy, submittedAt, approvedBy, approvedAt, paidBy, paidAt, createdAt, updatedAt,
                    lossCarryforwardEnabled, lossCarryforwardInCent, lossCarryforwardAppliedCent,
                    lossCarryforwardAfterCent, arrearsOffsetCent, reserveWithheldCent, payoutCent, principalType,
                    customDeductionCent, null, 0L);
        }

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
                    lossCarryforwardAfterCent, arrearsOffsetCent, reserveWithheldCent, payoutCent,
                    principalType, customDeductionCent, legalDistributableCent, periodTaxCent);
        }
    }

    /** V507 FS-014 财务登记的依法可分配金额（商户 × 期间；财税核算依据与分配决议必填）。 */
    public record DividendLegalConfirmationRecord(
            String id,
            String merchantId,
            String periodLabel,
            long legalDistributableCent,
            String taxAccountingRef,
            String resolutionRef,
            String note,
            String confirmedBy,
            Instant confirmedAt) {
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

    /** 210-R3 该期税费分税种合计（已生效实际申报金额；periods 为计入的各登记所属期，如「2026-01～2026-03」，按所属期末月排序以「、」连接）。 */
    public record PeriodTaxSumRecord(String taxType, long totalCent, String periods) {
    }

    /**
     * 211-R1 整期暂不分红／暂缓分红登记（V616 dividend_period_holds；BSM:1777；第十八批裁决 43）。holdType：skip 暂不分红 / defer 暂缓分红；
     * liftedBy、liftedAt、liftResult 为暂缓分红恢复时登记（恢复的协商结果），未恢复为空。
     */
    public record DividendPeriodHoldRecord(
            String id, String merchantId, String periodLabel, String holdType, String reason, String negotiationResult,
            String recordedBy, Instant recordedAt, String liftedBy, Instant liftedAt, String liftResult) {
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

    // ── V304 发放保护线留存·释放 ─────────────────────────────────

    /** 留存释放留痕（一单一释放；追溯释放时间/触发方式/关联原计算单/金额/被释放应付项）。 */
    public record DividendReserveReleaseRecord(
            String id,
            String statementId,
            String merchantId,
            String principalUserId,
            long reserveCent,
            String arApItemId,
            String triggerType,
            String releasedBy,
            Instant releasedAt,
            String note) {

        /** 商户侧（主理人视图）脱敏：不外发操作员 id 与应付款项 id。 */
        @com.fasterxml.jackson.annotation.JsonIgnore
        public DividendReserveReleaseRecord forMerchantView() {
            return new DividendReserveReleaseRecord(id, statementId, merchantId, principalUserId,
                    reserveCent, null, triggerType, null, releasedAt, note);
        }
    }

    /**
     * 待释放留存台账行（后台/商户专区展示）：某张有留存的正式计算单 + 释放状态（待释放/已释放）。
     * released=false 即待释放；released=true 时补释放留痕（时间/触发方式/操作人）。
     */
    public record DividendReserveLedgerRecord(
            String statementId,
            String merchantId,
            String principalUserId,
            String periodLabel,
            LocalDate periodStart,
            LocalDate periodEnd,
            String statementStatus,
            long reserveWithheldCent,
            boolean released,
            String triggerType,
            String releasedBy,
            Instant releasedAt) {

        /** 商户侧脱敏：不外发主理人账户以外的操作员 id。 */
        @com.fasterxml.jackson.annotation.JsonIgnore
        public DividendReserveLedgerRecord forMerchantView() {
            return new DividendReserveLedgerRecord(statementId, merchantId, principalUserId, periodLabel,
                    periodStart, periodEnd, statementStatus, reserveWithheldCent, released, triggerType,
                    null, releasedAt);
        }
    }

    /** 自动释放候选（定时作业用）：待释放留存 + 方案释放规则，供 isDue 判定到期后释放。 */
    public record DividendReserveReleaseCandidateRecord(
            String statementId,
            String merchantId,
            String principalUserId,
            long reserveWithheldCent,
            LocalDate periodEnd,
            String periodType,
            String reserveReleaseMode,
            int reserveReleaseDelayPeriods) {
    }

    /** 定时/手动批量释放结果：本次释放清单 + 跳过（未到期/已释放）清单。 */
    public record DividendReserveReleaseRunRecord(
            List<DividendReserveReleaseRecord> released,
            List<DividendComputeSkipRecord> skipped) {
    }
}
