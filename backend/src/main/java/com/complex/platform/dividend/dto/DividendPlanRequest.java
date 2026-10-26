package com.complex.platform.dividend.dto;

// 文件版本：V0.01

import java.util.List;

/**
 * 分红方案配置（新建/修改共用）。凡涉数据皆可配：比例/税率/周期/基数构成开关全部可调，
 * 空缺字段按默认口径补齐（80/20、股息红利 20%、半年+月度预览、净利润全项）；periodType 取 monthly / quarterly / semiannual / annual。
 * 修改时 merchantId 不可变更；status 可置 'ended' 停用方案。
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
        List<CustomDeductionItem> customDeductions) {

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
                payoutFloorFixedCent, reserveReleaseMode, reserveReleaseDelayPeriods, null, null);
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
                payoutFloorFixedCent, null, null, null, null);
    }
}
