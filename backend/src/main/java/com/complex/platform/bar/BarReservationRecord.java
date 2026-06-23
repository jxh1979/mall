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
        String depositSettlement,
        // V155 订卡邀请函：签发令牌（不可猜随机串）+ HMAC 防伪签名 + 卡座可进人数快照 + 状态（none/active/revoked）+ 签发时点。
        String invitationToken,
        String invitationSignature,
        Integer invitationCapacity,
        String invitationStatus,
        Instant invitationIssuedAt
) {
    /** 向后兼容构造器（V155 邀请函前签名）：既有调用方零改动，邀请函字段默认未签发（status=none）。 */
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
            String depositRule,
            String depositIntentId,
            Instant depositPaidAt,
            String depositSettlement
    ) {
        this(id, barId, merchantId, tableId, customerId, reservedAt, guestCount, allowedPassFreeGuestCount,
                depositAmountCent, depositStatus, orderSource, salesStaffId, status, createdTerminal, deviceId,
                evidenceAttachmentIds, createdAt, updatedAt, depositRule, depositIntentId, depositPaidAt, depositSettlement,
                null, null, null, "none", null);
    }

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
