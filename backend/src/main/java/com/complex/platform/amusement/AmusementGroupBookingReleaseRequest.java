package com.complex.platform.amusement;

import java.util.List;

public record AmusementGroupBookingReleaseRequest(
        String idempotencyKey,
        String reason,
        List<String> evidenceAttachmentIds
) {
}
