package com.complex.platform.dividend.dto;

// 文件版本：V0.03

import java.util.List;

/**
 * 分红方案配置（新建/修改共用）。凡涉数据皆可配：比例/税率/周期/基数构成开关全部可调，
 * 空缺字段按默认口径补齐（80/20、股息红利 20%、半年+月度预览、净利润全项）；periodType 取 monthly / quarterly / semiannual / annual。
 * 修改时 merchantId 不可变更；status 可置 'ended' 停用方案。
 * <p>234-R1（BSM:521）：新建与修改都是提交一个新版本，effectiveFrom（生效起始日期，须晚于该商户当前营业日）与 reason（调整理由）必填；
 * status='ended' 表示本版本自生效起始日起停用。
 *
 * <p>V303 扣除项三开关（可空=修改时不改现值 / 新建时默认关）：
 * lossCarryforwardEnabled 亏损结转、arrearsOffsetEnabled 欠款抵扣、payoutFloorEnabled 发放保护线；
 * payoutFloorBps/payoutFloorFixedCent 为保护线留存参数（比例万分比 / 固定额分）。
 *
 * <p>V304 发放保护线留存·释放规则（可空=修改时不改现值 / 新建时默认手动）：
 * reserveReleaseMode 释放时点（manual/next_period/fixed_delay/annual）、
 * reserveReleaseDelayPeriods 固定延后模式的周期数（>=1）。
 *
 * <p>V308 主理人类型（可空=修改时不改现值 / 新建时默认 individual）：principalType（individual/company）。
 * <p>V309 自定义扣除项（可空=修改时不改现值 / 新建时默认无）：customDeductions 非空时整表替换该方案的扣除项。
 * <p>V616 211-R2 发放付账日（新建必填 1～3；修改时可空=不改现值）：payoutPaySlot。
 * <p>V642 244-R1 分红周期上下限（按月数 1、3、6、12，新建必填；修改时可空=沿用改前版本）：periodMinMonths、periodMaxMonths，
 * 分红周期须落在上下限之内。
 * <p>V643 234-R2 其他股东（BSM:1775；修改时可空＝照改前版本复制名单，给了按请求整份落新版本）：otherShareholders，每项名称必填、
 * 类型 individual 个人股东或 company 公司股东、持股比例 1～10000 万分比；平台与其他股东合计不超过 10000，主理人为余下部分。
 * <p>控制席并入（BSM:1774）：includeInventoryCost 只能为空或 true，expenseTypes 只能为空或全部六类。
 */
public record DividendPlanRequest(
        String idempotencyKey,
        String merchantId,
        String principalUserId,
        String principalName,
        Integer platformShareBps,
        Integer taxRateBps,
        String periodType,
        List<String> revenueDomains,
        Boolean includeInventoryCost,
        List<String> expenseTypes,
        String effectiveFrom,
        String status,
        String note,
        Boolean lossCarryforwardEnabled,
        Boolean arrearsOffsetEnabled,
        Boolean payoutFloorEnabled,
        Integer payoutFloorBps,
        Long payoutFloorFixedCent,
        String reserveReleaseMode,
        Integer reserveReleaseDelayPeriods,
        String principalType,
        List<CustomDeductionItem> customDeductions,
        Integer payoutPaySlot,
        String reason,
        Integer periodMinMonths,
        Integer periodMaxMonths,
        List<OtherShareholderItem> otherShareholders) {

    /** 234-R2 其他股东请求项（工商登记在册的真实股东）。shareholderType：individual / company；shareBps：持股比例（万分比）。 */
    public record OtherShareholderItem(
            String shareholderName,
            String shareholderType,
            Integer shareBps) {
    }

    /** 向后兼容构造（234-R2 之前的 26 参形态）：其他股东默认 null（新建为无，修改时照改前版本复制）。 */
    public DividendPlanRequest(
            String idempotencyKey, String merchantId, String principalUserId, String principalName,
            Integer platformShareBps, Integer taxRateBps, String periodType, List<String> revenueDomains,
            Boolean includeInventoryCost, List<String> expenseTypes, String effectiveFrom, String status,
            String note, Boolean lossCarryforwardEnabled, Boolean arrearsOffsetEnabled, Boolean payoutFloorEnabled,
            Integer payoutFloorBps, Long payoutFloorFixedCent, String reserveReleaseMode,
            Integer reserveReleaseDelayPeriods, String principalType, List<CustomDeductionItem> customDeductions,
            Integer payoutPaySlot, String reason, Integer periodMinMonths, Integer periodMaxMonths) {
        this(idempotencyKey, merchantId, principalUserId, principalName, platformShareBps, taxRateBps, periodType,
                revenueDomains, includeInventoryCost, expenseTypes, effectiveFrom, status, note,
                lossCarryforwardEnabled, arrearsOffsetEnabled, payoutFloorEnabled, payoutFloorBps,
                payoutFloorFixedCent, reserveReleaseMode, reserveReleaseDelayPeriods, principalType, customDeductions,
                payoutPaySlot, reason, periodMinMonths, periodMaxMonths, null);
    }

    /**
     * V309 自定义扣除项请求。name 必填；sourceType=fixed 用 amountCent（默认 0），ratio 用 ratioBps +
     * ratioBasis（net_profit/revenue，默认 net_profit）；enabled 默认 true。
     */
    public record CustomDeductionItem(
            String name,
            String sourceType,
            Long amountCent,
            Integer ratioBps,
            String ratioBasis,
            Boolean enabled) {
    }

    /** 向后兼容构造（244-R1 之前的 24 参形态）：周期上下限默认 null（新建时必填校验拦下，修改时沿用改前版本）。 */
    public DividendPlanRequest(
            String idempotencyKey, String merchantId, String principalUserId, String principalName,
            Integer platformShareBps, Integer taxRateBps, String periodType, List<String> revenueDomains,
            Boolean includeInventoryCost, List<String> expenseTypes, String effectiveFrom, String status,
            String note, Boolean lossCarryforwardEnabled, Boolean arrearsOffsetEnabled, Boolean payoutFloorEnabled,
            Integer payoutFloorBps, Long payoutFloorFixedCent, String reserveReleaseMode,
            Integer reserveReleaseDelayPeriods, String principalType, List<CustomDeductionItem> customDeductions,
            Integer payoutPaySlot, String reason) {
        this(idempotencyKey, merchantId, principalUserId, principalName, platformShareBps, taxRateBps, periodType,
                revenueDomains, includeInventoryCost, expenseTypes, effectiveFrom, status, note,
                lossCarryforwardEnabled, arrearsOffsetEnabled, payoutFloorEnabled, payoutFloorBps,
                payoutFloorFixedCent, reserveReleaseMode, reserveReleaseDelayPeriods, principalType, customDeductions,
                payoutPaySlot, reason, null, null);
    }

    /** 向后兼容构造（234-R1 之前的 23 参形态）：调整理由默认 null（提交时必填校验拦下）。 */
    public DividendPlanRequest(
            String idempotencyKey, String merchantId, String principalUserId, String principalName,
            Integer platformShareBps, Integer taxRateBps, String periodType, List<String> revenueDomains,
            Boolean includeInventoryCost, List<String> expenseTypes, String effectiveFrom, String status,
            String note, Boolean lossCarryforwardEnabled, Boolean arrearsOffsetEnabled, Boolean payoutFloorEnabled,
            Integer payoutFloorBps, Long payoutFloorFixedCent, String reserveReleaseMode,
            Integer reserveReleaseDelayPeriods, String principalType, List<CustomDeductionItem> customDeductions,
            Integer payoutPaySlot) {
        this(idempotencyKey, merchantId, principalUserId, principalName, platformShareBps, taxRateBps, periodType,
                revenueDomains, includeInventoryCost, expenseTypes, effectiveFrom, status, note,
                lossCarryforwardEnabled, arrearsOffsetEnabled, payoutFloorEnabled, payoutFloorBps,
                payoutFloorFixedCent, reserveReleaseMode, reserveReleaseDelayPeriods, principalType, customDeductions,
                payoutPaySlot, null);
    }

    /** 向后兼容构造（V616 之前的 22 参形态）：发放付账日默认 null（新建时必填校验拦下）。 */
    public DividendPlanRequest(
            String idempotencyKey, String merchantId, String principalUserId, String principalName,
            Integer platformShareBps, Integer taxRateBps, String periodType, List<String> revenueDomains,
            Boolean includeInventoryCost, List<String> expenseTypes, String effectiveFrom, String status,
            String note, Boolean lossCarryforwardEnabled, Boolean arrearsOffsetEnabled, Boolean payoutFloorEnabled,
            Integer payoutFloorBps, Long payoutFloorFixedCent, String reserveReleaseMode,
            Integer reserveReleaseDelayPeriods, String principalType, List<CustomDeductionItem> customDeductions) {
        this(idempotencyKey, merchantId, principalUserId, principalName, platformShareBps, taxRateBps, periodType,
                revenueDomains, includeInventoryCost, expenseTypes, effectiveFrom, status, note,
                lossCarryforwardEnabled, arrearsOffsetEnabled, payoutFloorEnabled, payoutFloorBps,
                payoutFloorFixedCent, reserveReleaseMode, reserveReleaseDelayPeriods, principalType, customDeductions, null);
    }

    /** 向后兼容构造（V308/V309 之前的 20 参形态）：类型/自定义扣除两参默认 null。 */
    public DividendPlanRequest(
            String idempotencyKey, String merchantId, String principalUserId, String principalName,
            Integer platformShareBps, Integer taxRateBps, String periodType, List<String> revenueDomains,
            Boolean includeInventoryCost, List<String> expenseTypes, String effectiveFrom, String status,
            String note, Boolean lossCarryforwardEnabled, Boolean arrearsOffsetEnabled, Boolean payoutFloorEnabled,
            Integer payoutFloorBps, Long payoutFloorFixedCent, String reserveReleaseMode,
            Integer reserveReleaseDelayPeriods) {
        this(idempotencyKey, merchantId, principalUserId, principalName, platformShareBps, taxRateBps, periodType,
                revenueDomains, includeInventoryCost, expenseTypes, effectiveFrom, status, note,
                lossCarryforwardEnabled, arrearsOffsetEnabled, payoutFloorEnabled, payoutFloorBps,
                payoutFloorFixedCent, reserveReleaseMode, reserveReleaseDelayPeriods, null, null, null);
    }

    /** 向后兼容构造（V304 之前的 18 参形态）：释放规则/类型/自定义扣除默认 null（服务层补默认）。 */
    public DividendPlanRequest(
            String idempotencyKey, String merchantId, String principalUserId, String principalName,
            Integer platformShareBps, Integer taxRateBps, String periodType, List<String> revenueDomains,
            Boolean includeInventoryCost, List<String> expenseTypes, String effectiveFrom, String status,
            String note, Boolean lossCarryforwardEnabled, Boolean arrearsOffsetEnabled, Boolean payoutFloorEnabled,
            Integer payoutFloorBps, Long payoutFloorFixedCent) {
        this(idempotencyKey, merchantId, principalUserId, principalName, platformShareBps, taxRateBps, periodType,
                revenueDomains, includeInventoryCost, expenseTypes, effectiveFrom, status, note,
                lossCarryforwardEnabled, arrearsOffsetEnabled, payoutFloorEnabled, payoutFloorBps,
                payoutFloorFixedCent, null, null, null, null, null);
    }
}
