package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/** FS-016：员工侧「向当前持有人发送短信验证码」；scene ∈ transfer | claim。 */
public record SavedBottleVerificationCodeRequest(
        String idempotencyKey,
        String scene
) {
}
