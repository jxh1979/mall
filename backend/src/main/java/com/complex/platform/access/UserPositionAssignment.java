package com.complex.platform.access;

import java.time.Instant;

public record UserPositionAssignment(
        String userId,
        String positionCode,
        String staffLevelId,
        String organizationId,
        String merchantId,
        String assignedBy,
        Instant assignedAt
) {
}
