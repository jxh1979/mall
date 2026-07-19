package com.complex.platform.voucher;

import java.time.Instant;

/**
 * 券实例（统一台账）：收编来源＝积分兑换（points_redemption）＋营销赠发（marketing_grant）。
 * 状态机 issued → redeemed / expired / revoked（见 {@link VoucherStateMachine}）。
 */
public record VoucherInstanceRecord(
        String id,
        String definitionId,
        String customerId,
        String code,
        String sourceType,
        String sourceRef,
        String status,
        Instant issuedAt,
        Instant expiresAt,
        Instant redeemedAt,
        Instant revokedAt,
        String revokeReason,
        Instant createdAt,
        Instant updatedAt
) {
}
