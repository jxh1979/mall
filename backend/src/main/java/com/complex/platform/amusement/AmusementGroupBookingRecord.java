package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;

public record AmusementGroupBookingRecord(
        String id,
        String projectId,
        String sessionId,
        String bookingType,
        String customerName,
        String contactPhone,
        int partySize,
        int reservedCapacity,
        String bookingStatus,
        String approvalNote,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Instant approvedAt,
        Instant releasedAt,
        Instant createdAt,
        Instant updatedAt,
        // V231 强制口径：团体预定确认时刻。本域无独立支付步，审批确认(booking_status→approved)即预定确认落点，paid_at=approved_at；未确认为 null。
        Instant paidAt
) {
    /** V231 兼容构造器（16 参，无 paidAt）：paidAt 默认 null（既有创建/测试调用零改动）。 */
    public AmusementGroupBookingRecord(
            String id,
            String projectId,
            String sessionId,
            String bookingType,
            String customerName,
            String contactPhone,
            int partySize,
            int reservedCapacity,
            String bookingStatus,
            String approvalNote,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            Instant approvedAt,
            Instant releasedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, projectId, sessionId, bookingType, customerName, contactPhone, partySize, reservedCapacity,
                bookingStatus, approvalNote, operatorNote, evidenceAttachmentIds, approvedAt, releasedAt,
                createdAt, updatedAt, null);
    }

    /**
     * XM-009（PGM-1.15.2.c2「员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关」）：员工端读端只出掩码联系人手机号。
     * 脱敏放在 controller（service 外）：服务层内部仍需明文（回写密文），与 V339 多主体联系人同款范式。
     */
    public AmusementGroupBookingRecord withMaskedPhone() {
        return new AmusementGroupBookingRecord(id, projectId, sessionId, bookingType, customerName,
                com.complex.platform.common.PhoneMask.mask(contactPhone), partySize, reservedCapacity, bookingStatus,
                approvalNote, operatorNote, evidenceAttachmentIds, approvedAt, releasedAt, createdAt, updatedAt, paidAt);
    }
}
