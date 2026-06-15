package com.complex.platform.merchantbilling.dto;

import java.time.Instant;

public record MerchantBillDunningRequest(
        String idempotencyKey,
        String operatorNote,
        String dunningChannel,
        String messageTemplate,
        String status,
        Instant sentAt,
        String operatorUserId
) {
}
