package com.complex.platform.inventory;

// 文件版本：V0.01

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/** FS-016：官方动态取酒码（saved_bottle_pickup_codes）；只存哈希，一瓶同时只一枚 active。 */
public record SavedBottlePickupCodeRecord(
        String id,
        String savedBottleId,
        String customerId,
        @JsonIgnore String codeHash,
        Instant issuedAt,
        Instant expiresAt,
        int refreshIntervalSeconds,
        String status,
        String consumedClaimId,
        Instant createdAt
) {
    SavedBottlePickupCodeRecord withStatus(String newStatus, String claimId) {
        return new SavedBottlePickupCodeRecord(id, savedBottleId, customerId, codeHash, issuedAt, expiresAt,
                refreshIntervalSeconds, newStatus, claimId, createdAt);
    }
}
