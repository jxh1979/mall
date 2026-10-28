package com.complex.platform.dealer.dto;

// 文件版本：V0.02

import java.time.Instant;
import java.util.List;

public record DealerDeliveryReceiptRequest(String idempotencyKey, String deliveryBatchId, Instant confirmedAt,
        String buyerContact, List<String> evidenceAttachmentIds, List<Line> lines, String note, String counterpartyConfirmationId) {
    public DealerDeliveryReceiptRequest(String idempotencyKey, String deliveryBatchId, Instant confirmedAt, String buyerContact,
            List<String> evidenceAttachmentIds, List<Line> lines, String note) {
        this(idempotencyKey, deliveryBatchId, confirmedAt, buyerContact, evidenceAttachmentIds, lines, note, null);
    }
    public record Line(String orderLineId, int acceptedQuantity, int rejectedQuantity, int damagedQuantity, int shortQuantity) {}
}
