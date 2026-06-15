package com.complex.platform.bar;

import java.util.List;
import java.util.Map;

public record BarOrderDeliveryRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        List<Map<String, Object>> deliveredItems,
        String deliveredByUserId,
        List<String> deliveryPhotoAttachmentIds
) {
}
