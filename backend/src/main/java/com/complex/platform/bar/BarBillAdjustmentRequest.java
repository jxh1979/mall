package com.complex.platform.bar;
// 文件版本：V0.01
import java.util.List;
public record BarBillAdjustmentRequest(String idempotencyKey,String operatorNote,List<String> evidenceAttachmentIds,
        String adjustmentType,Long amountCent,String reason,String requestedByUserId,List<RefundLine> refundLines) {
    public record RefundLine(String sourceBillId,String componentType,long amountCent) {}
    public BarBillAdjustmentRequest(String idempotencyKey,String operatorNote,List<String> evidenceAttachmentIds,
            String adjustmentType,Long amountCent,String reason,String requestedByUserId) {
        this(idempotencyKey,operatorNote,evidenceAttachmentIds,adjustmentType,amountCent,reason,requestedByUserId,List.of());
    }
}
