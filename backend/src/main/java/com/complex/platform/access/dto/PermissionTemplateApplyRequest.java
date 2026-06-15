package com.complex.platform.access.dto;

import java.util.List;

public record PermissionTemplateApplyRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String templateId,
        List<String> targetUserIds,
        String mode
) {
}
