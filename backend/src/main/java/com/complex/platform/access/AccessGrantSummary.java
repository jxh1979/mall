package com.complex.platform.access;

import java.util.List;

public record AccessGrantSummary(
        List<AccessGrantPositionSummary> positionRecommendations,
        List<AccessGrantUserSummary> userAnomalies
) {
}
