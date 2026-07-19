package com.complex.platform.voucher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 一次性券状态机（纯函数，零依赖）：issued → redeemed / expired / revoked；终态不可再迁。
 * 过期采用惰性口径：不跑定时任务，读/核销时按 expires_at 折算有效状态（核销路径顺手落库）。
 */
public final class VoucherStateMachine {

    public static final String ISSUED = "issued";
    public static final String REDEEMED = "redeemed";
    public static final String EXPIRED = "expired";
    public static final String REVOKED = "revoked";

    private VoucherStateMachine() {
    }

    /** 仅允许 issued → redeemed/expired/revoked；其余（含终态出发、未知态）一律拒绝。 */
    public static boolean canTransition(String from, String to) {
        return ISSUED.equals(from)
                && (REDEEMED.equals(to) || EXPIRED.equals(to) || REVOKED.equals(to));
    }

    /**
     * 计算实例过期时间：按天数（发放时刻+validDays）与固定截止（validUntil）并存取更早；都空＝不过期。
     */
    public static Instant computeExpiry(Instant issuedAt, Integer validDays, Instant validUntil) {
        Instant byDays = validDays == null ? null : issuedAt.plus(validDays, ChronoUnit.DAYS);
        if (byDays == null) {
            return validUntil;
        }
        if (validUntil == null) {
            return byDays;
        }
        return byDays.isBefore(validUntil) ? byDays : validUntil;
    }

    /** 有效状态（惰性过期）：issued 且已到/过 expires_at → expired；其余原样。 */
    public static String effectiveStatus(String status, Instant expiresAt, Instant now) {
        if (ISSUED.equals(status) && expiresAt != null && !expiresAt.isAfter(now)) {
            return EXPIRED;
        }
        return status;
    }

    /** 核销资格校验：返回 null＝可核销；否则＝拒绝错误码（VOUCHER_ALREADY_REDEEMED/REVOKED/EXPIRED/NOT_REDEEMABLE）。 */
    public static String redeemBlockReason(String status, Instant expiresAt, Instant now) {
        String effective = effectiveStatus(status, expiresAt, now);
        if (ISSUED.equals(effective)) {
            return null;
        }
        if (REDEEMED.equals(effective)) {
            return "VOUCHER_ALREADY_REDEEMED";
        }
        if (REVOKED.equals(effective)) {
            return "VOUCHER_REVOKED";
        }
        if (EXPIRED.equals(effective)) {
            return "VOUCHER_EXPIRED";
        }
        return "VOUCHER_NOT_REDEEMABLE";
    }
}
