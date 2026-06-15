package com.complex.platform.governance.dto;

import java.util.List;

public record BusinessEntityRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String entityName,
        String entityRole,
        String unifiedSocialCreditCode,
        String legalRepresentative,
        String bankAccountRef,
        String status
) {
}
