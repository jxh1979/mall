package com.complex.platform.inventory;

// 文件版本：V0.02

import java.time.Instant;

/** FS-016：员工侧向当前持有人发送短信验证码的回执（不含手机号明文）；第十八批 AP 段 254-R4 登记隐私同意验证码回执无存酒编号、scene=deposit。 */
public record SavedBottleVerificationCodeReceipt(
        String savedBottleId,
        String scene,
        Instant requestedAt
) {
}
