package com.complex.platform.governance;

// 文件版本：V0.01

/**
 * 第十批 TK-27（BSM L1058「待清台超过后台配置时限（业务参数，按酒吧可配，出厂默认 15 分钟，须同时配置上下限）」）：
 * 酒吧（商户）待清台提醒时限。值与上下限同配：未配置三者皆空、生效值取出厂 15 分钟。
 */
public record MerchantPendingClearAlertRecord(
        String merchantId,
        Integer valueMinutes,
        Integer minMinutes,
        Integer maxMinutes,
        int effectiveMinutes
) {
    public static final int FACTORY_DEFAULT_MINUTES = 15;

    public static MerchantPendingClearAlertRecord of(String merchantId, Integer value, Integer min, Integer max) {
        return new MerchantPendingClearAlertRecord(merchantId, value, min, max, value == null ? FACTORY_DEFAULT_MINUTES : value);
    }
}
