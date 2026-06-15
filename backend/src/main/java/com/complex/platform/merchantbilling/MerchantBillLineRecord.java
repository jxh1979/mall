package com.complex.platform.merchantbilling;

import java.time.Instant;

public record MerchantBillLineRecord(
        String id,
        String billId,
        String feeType,
        String description,
        long amountCent,
        String currency,
        Instant createdAt
) {
}
