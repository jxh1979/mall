package com.complex.platform.access.dto;

// 文件版本：V0.01

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record AccessUserRequest(
        @JsonAlias("idempotency_key") String idempotencyKey,
        @JsonAlias("operator_note") String operatorNote,
        @JsonAlias("evidence_attachment_ids") List<String> evidenceAttachmentIds,
        String username,
        String password,
        @com.fasterxml.jackson.annotation.JsonIgnore String phone,
        @JsonAlias("account_type") String accountType,
        @JsonAlias("organization_id") String organizationId,
        @JsonAlias("merchant_id") String merchantId,
        String status,
        @JsonAlias("consent_reference") String consentReference
) {
    public AccessUserRequest(
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
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, username, password, phone,
                accountType, organizationId, merchantId, status, null);
    }
}
