package com.complex.platform.amusement;

import java.util.List;

public record AmusementProjectRuleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ruleType,
        String ruleValue,
        String effectiveFrom,
        String effectiveTo,
        Boolean blocking
) {
}
