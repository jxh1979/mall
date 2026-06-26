package com.complex.platform.access;

public record AccessGrantPositionSummary(
        String positionCode,
        String staffLevelId,
        String permissionCode,
        long requestCount,
        String recommendation
) {
}
