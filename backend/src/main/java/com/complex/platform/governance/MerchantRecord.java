package com.complex.platform.governance;

import java.time.Instant;

public record MerchantRecord(
        String id,
        String merchantName,
        String merchantType,
        String businessEntityId,
        String contractEntityId,
        String settlementEntityId,
        String contactName,
        String contactPhone,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
