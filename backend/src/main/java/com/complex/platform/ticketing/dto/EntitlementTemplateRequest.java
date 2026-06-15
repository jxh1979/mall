package com.complex.platform.ticketing.dto;

import java.util.List;

public record EntitlementTemplateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String name,
        String entitlementType,
        Integer defaultTotalTimes,
        Long defaultValueCent,
        String status
) {
}
