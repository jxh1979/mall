package com.complex.platform.merchantbilling.dto;

public record MerchantBillConfirmRequest(
        String idempotencyKey,
        String operatorNote,
        String operatorUserId
) {
}
