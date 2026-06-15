package com.complex.platform.integration;

import java.util.Map;

public record OpenEventResultResponse(
        String eventId,
        String eventStatus,
        Map<String, Object> result
) {
}
