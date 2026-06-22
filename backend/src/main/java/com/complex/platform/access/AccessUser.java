package com.complex.platform.access;

import java.time.Instant;
import java.util.List;

public record AccessUser(
        String id,
        String username,
        String accountType,
        String organizationId,
        String merchantId,
        String status,
        Instant createdAt,
        String phoneHash,
        String createdVia,
        String consentReference,
        List<String> evidenceAttachmentIds,
        String operatorNote
) {
    public AccessUser {
        evidenceAttachmentIds = evidenceAttachmentIds == null ? List.of() : List.copyOf(evidenceAttachmentIds);
    }

    public AccessUser(
            String id,
            String username,
            String accountType,
            String organizationId,
            String merchantId,
            String status,
            Instant createdAt
    ) {
        this(id, username, accountType, organizationId, merchantId, status, createdAt, null, "legacy", null, List.of(), null);
    }
}
