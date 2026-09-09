package com.complex.platform.governance;
// 文件版本：V0.01

import java.time.Instant;

public record BusinessEntityRecord(
        String id,
        String entityName,
        String entityRole,
        String unifiedSocialCreditCode,
        String legalRepresentative,
        String bankAccountRef,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
