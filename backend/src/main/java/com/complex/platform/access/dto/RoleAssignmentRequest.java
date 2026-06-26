package com.complex.platform.access.dto;

public record RoleAssignmentRequest(
        String idempotencyKey,
        String operatorNote,
        String userId,
        String roleId
) {
}
