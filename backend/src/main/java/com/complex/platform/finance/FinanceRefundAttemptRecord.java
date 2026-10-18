package com.complex.platform.finance;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

public record FinanceRefundAttemptRecord(
        String id, String refundId, String operation, String status,
        String gatewayProvider, String gatewayRef, String message,
        String paymentReference, String paymentOperatorId, String verifiedBy,
        List<String> evidenceAttachmentIds, Instant createdAt) {
    public FinanceRefundAttemptRecord {
        evidenceAttachmentIds = evidenceAttachmentIds == null ? List.of() : List.copyOf(evidenceAttachmentIds);
    }
}
