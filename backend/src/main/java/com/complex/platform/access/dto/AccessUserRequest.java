package com.complex.platform.access.dto;

import java.util.List;

public record AccessUserRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String username,
        String password,
        String phone,
        String accountType,
        String organizationId,
        String merchantId,
        String status
) {
}
