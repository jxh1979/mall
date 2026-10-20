package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/**
 * FS-025 转存业务参数配置（ENUM L120）：policy_key=max_renewals（scope_id=global | project:&lt;id&gt;）或
 * extension_days（scope_id=global | bar:&lt;id&gt;）；value 与 min_value/max_value 须同时填写；每次保存 version+1 并留痕。
 */
public record SavedBottleRenewalPolicyRequest(
        String idempotencyKey,
        String policyKey,
        String scopeId,
        Integer value,
        Integer minValue,
        Integer maxValue,
        String reason
) {
}
