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

    /**
     * P0-A/GAP-07 展示脱敏：无 {@code pii.contact_phone.view} 权限的读端返回掩码手机号。
     * 脱敏放在 controller（service 外），与 V279 内部结算价同款范式。
     */
    public MerchantFeeNotificationRecipientRecord withMaskedPhone() {
        return new MerchantFeeNotificationRecipientRecord(id, merchantId, feeEvent, recipientName,
                com.complex.platform.common.PhoneMask.mask(recipientPhone), status, createdBy,
                createdAt, updatedAt, deletedAt);
    }
}
