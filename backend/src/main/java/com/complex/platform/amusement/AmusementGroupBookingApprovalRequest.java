package com.complex.platform.amusement;

import java.util.List;

public record AmusementGroupBookingApprovalRequest(
        String idempotencyKey,
        String approvalNote,
        List<String> evidenceAttachmentIds
) {
}
