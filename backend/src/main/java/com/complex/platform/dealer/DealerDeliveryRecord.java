package com.complex.platform.dealer;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

/** 数量交付、应收金额、原路退款分别展示；批准本身不表示实物或款项完成。 */
public record DealerDeliveryRecord(String orderId, Instant deliveryCompletedAt, long netReceivableCent,
        long receivedCent, long pendingRefundCent, String moneyStatus, List<Batch> batches,
        List<Receipt> receipts, List<Discrepancy> discrepancies) {
    public record Batch(String id, String orderId, String resolutionId, String shippedBy, Instant shippedAt, List<BatchLine> lines, List<String> evidenceAttachmentIds) {}
    public record BatchLine(String orderLineId, int quantity, int recordedQuantity, int acceptedQuantity) {}
    public record Receipt(String id, String batchId, Instant confirmedAt, String buyerContact, List<String> evidenceAttachmentIds,
            String note, String createdBy, List<ReceiptLine> lines) {}
    public record ReceiptLine(String id, String orderLineId, int acceptedQuantity, int rejectedQuantity, int damagedQuantity, int shortQuantity) {}
    public record Discrepancy(String id, String orderId, String receiptLineId, String orderLineId, String discrepancyType,
            int quantity, int reservedQuantity, int completedQuantity, List<Resolution> resolutions) {}
    public record Resolution(String id, String discrepancyId, int quantity, String resolutionType, String physicalRoute,
            long adjustmentCent, String responsibilityParty, String agreementBasis, List<String> evidenceAttachmentIds,
            String reviewId, String status, String requestedBy, Instant requestedAt, Instant physicalCompletedAt,
            List<String> physicalEvidenceAttachmentIds, String physicalCompletedBy, Instant adjustmentAppliedAt, Instant completedAt) {}
}
