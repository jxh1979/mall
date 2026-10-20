package com.complex.platform.inventory;

// 文件版本：V0.01

import java.time.Instant;

/** FS-016：员工侧向当前持有人发送短信验证码的回执（不含手机号明文）。 */
public record SavedBottleVerificationCodeReceipt(
        String savedBottleId,
        String scene,
        Instant requestedAt
) {
}
