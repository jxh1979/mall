package com.complex.platform.inventory;

import java.time.Instant;
import java.util.List;

public record SavedBottleClaimRecord(
        String id,
        String savedBottleId,
        int claimQuantity,
        String claimStaffId,
        String claimTerminalType,
        String claimDeviceId,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt
) {
}
