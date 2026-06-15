package com.complex.platform.amusement;

import java.util.List;

public record AmusementGroupBookingRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectId,
        String sessionId,
        String bookingType,
        String customerName,
        String contactPhone,
        Integer partySize,
        Integer reservedCapacity
) {
}
