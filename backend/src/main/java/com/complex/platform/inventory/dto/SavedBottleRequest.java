package com.complex.platform.inventory.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SavedBottleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String customerId,
        String barId,
        List<Map<String, Object>> items,
        Instant expireAt
) {
}
