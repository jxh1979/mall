package com.complex.platform.merchantbilling.dto;

public record MerchantFeeNotificationRecipientRequest(
        String merchantId,
        String feeEvent,
        String recipientName,
        String recipientPhone,
        String operatorUserId
) {
}
