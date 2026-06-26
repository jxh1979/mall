package com.complex.platform.access.dto;

public record PositionRoleTemplateRequest(
        String idempotencyKey,
        String operatorNote,
        String positionCode,
        String staffLevelId,
        String roleId,
        String templateId,
        String status
) {
}
