package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record BarTableGuestCheckinRecord(
        String id,
        String reservationId,
        String barId,
        String tableId,
        String guestIdentifier,
        String verificationResult,
        int checkedInCountAfter,
        String deviceId,
        String staffId,
        List<String> evidenceAttachmentIds,
        Instant createdAt
) {
}
