package com.complex.platform.amusement;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AmusementSafetyTemplateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String templateName,
        List<Map<String, Object>> items,
        LocalDate effectiveFrom
) {
}
