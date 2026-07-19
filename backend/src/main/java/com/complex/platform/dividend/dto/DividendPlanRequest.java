package com.complex.platform.dividend.dto;

import java.util.List;

/**
 * 分红方案配置（新建/修改共用）。凡涉数据皆可配：比例/税率/周期/基数构成开关全部可调，
 * 空缺字段按默认口径补齐（80/20、股息红利 20%、季度+月度预览、净利润全项）。
 * 修改时 merchantId 不可变更；status 可置 'ended' 停用方案。
 *
 * <p>V303 扣除项三开关（可空=修改时不改现值 / 新建时默认关）：
 * lossCarryforwardEnabled 亏损结转、arrearsOffsetEnabled 欠款抵扣、payoutFloorEnabled 发放保护线；
 * payoutFloorBps/payoutFloorFixedCent 为保护线留存参数（比例万分比 / 固定额分）。
 *
 * <p>V304 发放保护线留存·释放规则（可空=修改时不改现值 / 新建时默认手动）：
 * reserveReleaseMode 释放时点（manual/next_period/fixed_delay/annual）、
 * reserveReleaseDelayPeriods 固定延后模式的周期数（>=1）。
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
        Integer reserveReleaseDelayPeriods) {

    /** 向后兼容构造（V304 之前的 18 参形态）：释放规则两参默认 null（服务层补默认手动）。 */
    public DividendPlanRequest(
            String idempotencyKey, String merchantId, String principalUserId, String principalName,
            Integer platformShareBps, Integer taxRateBps, String periodType, List<String> revenueDomains,
            Boolean includeInventoryCost, List<String> expenseTypes, String effectiveFrom, String status,
            String note, Boolean lossCarryforwardEnabled, Boolean arrearsOffsetEnabled, Boolean payoutFloorEnabled,
            Integer payoutFloorBps, Long payoutFloorFixedCent) {
        this(idempotencyKey, merchantId, principalUserId, principalName, platformShareBps, taxRateBps, periodType,
                revenueDomains, includeInventoryCost, expenseTypes, effectiveFrom, status, note,
                lossCarryforwardEnabled, arrearsOffsetEnabled, payoutFloorEnabled, payoutFloorBps,
                payoutFloorFixedCent, null, null);
    }
}
