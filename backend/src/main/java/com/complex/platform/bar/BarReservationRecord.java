package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record BarReservationRecord(
        String id,
        String barId,
        String merchantId,
        String tableId,
        String customerId,
        Instant reservedAt,
        int guestCount,
        int allowedPassFreeGuestCount,
        long depositAmountCent,
        String depositStatus,
        String orderSource,
        String salesStaffId,
        String status,
        String createdTerminal,
        String deviceId,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt,
        String depositRule,
        String depositIntentId,
        Instant depositPaidAt,
        String depositSettlement
) {
    /** 向后兼容构造器（T16 前签名）：既有调用方零改动，定金流转字段默认空。 */
    public BarReservationRecord(
            String id,
            String barId,
            String merchantId,
            String tableId,
            String customerId,
            Instant reservedAt,
            int guestCount,
            int allowedPassFreeGuestCount,
            long depositAmountCent,
            String depositStatus,
            String orderSource,
            String salesStaffId,
            String status,
            String createdTerminal,
            String deviceId,
            List<String> evidenceAttachmentIds,
            Instant createdAt,
            Instant updatedAt,
            String depositRule
    ) {
        this(id, barId, merchantId, tableId, customerId, reservedAt, guestCount, allowedPassFreeGuestCount,
                depositAmountCent, depositStatus, orderSource, salesStaffId, status, createdTerminal, deviceId,
                evidenceAttachmentIds, createdAt, updatedAt, depositRule, null, null, null);
    }
}
