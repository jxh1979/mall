package com.complex.platform.merchantbilling;

import java.time.Instant;
import java.util.List;

public record MerchantBillDisputeRecord(
        String id,
        String billId,
        String merchantId,
        String reasonCode,
        String description,
        String status,
        List<String> evidenceAttachmentIds,
        String operatorUserId,
        Instant createdAt,
        Instant updatedAt
) {
}
