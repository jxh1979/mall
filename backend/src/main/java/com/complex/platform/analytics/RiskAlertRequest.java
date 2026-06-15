package com.complex.platform.analytics;

import java.util.List;

public record RiskAlertRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ruleId,
        String alertType,
        String riskLevel,
        String objectType,
        String objectId,
        String summary
) {
}
