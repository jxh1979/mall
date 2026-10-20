package com.complex.platform.customer.dto;

// 文件版本：V0.01

/** FS-025 客人端本人转存：短信验证码确认（ENUM L120「转存须客人本人短信验证码确认（与转让同款）」）。 */
public record CustomerSavedBottleRenewRequest(
        String idempotencyKey,
        String smsCode
) {
}
