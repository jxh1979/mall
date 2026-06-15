package com.complex.platform.amusement;

import java.util.List;

public record AmusementOpenEligibilityResponse(
        String projectId,
        String sessionId,
        boolean allowed,
        List<String> blockedReasons
) {
}
