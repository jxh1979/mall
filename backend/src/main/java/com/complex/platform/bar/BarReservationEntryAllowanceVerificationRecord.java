package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record BarReservationEntryAllowanceVerificationRecord(
        String id,
        String reservationId,
        String barId,
        String tableId,
        String guestIdentifier,
        boolean allowed,
        String reasonCode,
        int checkedInCount,
        int allowedGuestCount,
        int remainingGuestCount,
        String deviceId,
        String staffId,
        List<String> evidenceAttachmentIds,
        Instant verifiedAt
) {
}
