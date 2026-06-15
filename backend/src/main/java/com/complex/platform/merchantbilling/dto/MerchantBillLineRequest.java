package com.complex.platform.merchantbilling.dto;

public record MerchantBillLineRequest(
        String feeType,
        String description,
        Long amountCent
) {
}
