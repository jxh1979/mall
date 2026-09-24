package com.complex.platform.governance.dto;
// 文件版本：V0.03

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
        String status,
        String projectId
) {
    public BusinessEntityRequest(
            String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String entityName,
            String entityRole, String unifiedSocialCreditCode, String legalRepresentative, String bankAccountRef,
            String status) {
        this(
                idempotencyKey, operatorNote, evidenceAttachmentIds, entityName, entityRole,
                unifiedSocialCreditCode, legalRepresentative, bankAccountRef, status, null);
    }
}
