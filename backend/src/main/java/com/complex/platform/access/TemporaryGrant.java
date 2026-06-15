package com.complex.platform.access;

import java.time.Instant;
import java.util.List;

public record TemporaryGrant(
        String id,
        String targetUserId,
        List<String> permissionCodes,
        Instant validFrom,
        Instant validUntil,
        boolean singleUse,
        boolean consumed,
        String status,
        String reason,
        String grantedBy,
        Instant grantedAt,
        String revokedBy,
        Instant revokedAt
) {
}
