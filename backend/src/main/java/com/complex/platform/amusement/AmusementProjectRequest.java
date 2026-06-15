package com.complex.platform.amusement;

import java.util.List;
import java.util.Map;

public record AmusementProjectRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectName,
        Integer capacity,
        String openStatus,
        String safetyTemplateId,
        Map<String, Object> eligibilityRule
) {
}
