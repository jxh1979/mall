package com.complex.platform.governance;

// 文件版本：V0.01

/**
 * 第十八批 AB 段第 190 条 190-R1（BSM:1079「同一会话的转台次数不得超过后台配置的上限（业务参数，按酒吧可配，出厂默认 10 次，须同时配置上下限）」；
 * 决策台账 DEC-R6-19 同句）：酒吧（商户）转台次数上限。值与上下限同配：未配置三者皆空、生效值取出厂 10 次。
 */
public record MerchantTableTransferLimitRecord(
        String merchantId,
        Integer valueCount,
        Integer minCount,
        Integer maxCount,
        int effectiveCount
) {
    public static final int FACTORY_DEFAULT_COUNT = 10;

    public static MerchantTableTransferLimitRecord of(String merchantId, Integer value, Integer min, Integer max) {
        return new MerchantTableTransferLimitRecord(merchantId, value, min, max, value == null ? FACTORY_DEFAULT_COUNT : value);
    }
}
