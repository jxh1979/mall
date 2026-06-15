package com.complex.platform.inventory.dto;

import java.util.List;
import java.util.Map;

public record SavedBottleClaimRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        List<Map<String, Object>> claimItems,
        String claimByPhone,
        String verificationCode
) {
}
