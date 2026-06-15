package com.complex.platform.access.dto;

import java.util.List;

public record RoleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String roleName,
        String description,
        List<String> permissionCodes,
        String dataScope
) {
}
