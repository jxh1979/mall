package com.complex.platform.dealer.dto;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

public record DealerDeliveryReceiptRequest(String idempotencyKey, String deliveryBatchId, Instant confirmedAt,
        String buyerContact, List<String> evidenceAttachmentIds, List<Line> lines, String note) {
    public record Line(String orderLineId, int acceptedQuantity, int rejectedQuantity, int damagedQuantity, int shortQuantity) {}
}
