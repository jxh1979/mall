package com.complex.platform.merchantbilling;

import java.time.Instant;

public record MerchantFeeNotificationRecipientRecord(
        String id,
        String merchantId,
        String feeEvent,
        String recipientName,
        String recipientPhone,
        String status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}
