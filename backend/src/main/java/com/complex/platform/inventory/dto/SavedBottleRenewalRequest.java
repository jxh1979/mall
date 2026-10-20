package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/**
 * FS-025 员工代办转存 / 营销主管复核共用请求体。代办时 verificationCode 必填（客人本人短信验证码，与转让同款）；
 * 复核时忽略 verificationCode，decision 必填（approve=通过并顺延到期日 | reject=驳回、到期日不动、不计转存次数），reject 时 reason 必填。
 */
public record SavedBottleRenewalRequest(
        String idempotencyKey,
        String operatorNote,
        String verificationCode,
        String decision,
        String reason
) {
    /** 代办转存（无复核决定）。 */
    public SavedBottleRenewalRequest(String idempotencyKey, String operatorNote, String verificationCode) {
        this(idempotencyKey, operatorNote, verificationCode, null, null);
    }
}
