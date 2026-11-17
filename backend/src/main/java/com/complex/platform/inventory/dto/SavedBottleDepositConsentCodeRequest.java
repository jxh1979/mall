package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/** 第十八批 AP 段 254-R4（第十八批裁决 57「短信验证码或者小程序，这个点击同意都可以。」）：存酒登记前向客人手机发隐私同意验证码。 */
public record SavedBottleDepositConsentCodeRequest(
        String idempotencyKey,
        String customerPhone
) {
}
