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
        Instant updatedAt
) {
}
