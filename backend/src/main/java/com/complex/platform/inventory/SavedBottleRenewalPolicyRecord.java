package com.complex.platform.inventory;

// 文件版本：V0.01

/**
 * FS-025 转存业务参数（saved_bottle_renewal_policy）。ENUM L120：转存次数上限「按项目可配，出厂默认 2 次」（max_renewals，
 * scope=project:&lt;id&gt;，缺行回落 global）；每次顺延期限「按酒吧可配，出厂默认 30 天」（extension_days，scope=bar:&lt;id&gt;，
 * 缺行回落 global）；「须同时配置上下限」——出厂行上下限留空，首次配置须同时填写。configVersion 供转存流水固化「当时的配置版本」。
 */
public record SavedBottleRenewalPolicyRecord(
        String policyKey,
        String scopeId,
        int numericValue,
        Integer minValue,
        Integer maxValue,
        int version
) {
    public String configVersion() {
        return scopeId + ":v" + version;
    }
}
