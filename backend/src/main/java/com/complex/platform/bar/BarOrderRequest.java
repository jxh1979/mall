package com.complex.platform.bar;

import java.util.List;
import java.util.Map;

public record BarOrderRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String barId,
        String tableId,
        String reservationId,
        List<Map<String, Object>> items,
        String salespersonUserId,
        List<String> serviceUserIds,
        String paymentMode
) {
}
