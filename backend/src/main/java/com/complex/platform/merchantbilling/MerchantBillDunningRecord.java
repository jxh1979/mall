package com.complex.platform.merchantbilling;

import java.time.Instant;

public record MerchantBillDunningRecord(
        String id,
        String billId,
        String merchantId,
        String dunningChannel,
        String messageTemplate,
        String status,
        Instant sentAt,
        String operatorUserId,
        Instant createdAt
) {
}
