package com.complex.platform.access.dto;

import java.time.Instant;
import java.util.List;

public record PermissionTemplateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String templateName,
        String templateType,
        String sourceTemplateId,
        List<String> permissionCodes,
        String dataScope,
        Instant validFrom,
        Instant validUntil,
        Boolean singleUse
) {
}
