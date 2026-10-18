package com.complex.platform.inventory;

// 文件版本：V0.01

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;

public final class PurchaseWorkflowRequests {
    private PurchaseWorkflowRequests() {}
    public record Terms(String idempotencyKey,PurchaseContractTerms terms,List<String> evidenceAttachmentIds) {}
    public record Resolution(String idempotencyKey,String resolutionType,int quantity,LocalDate expectedArrivalDate,
            String reason,List<String> evidenceAttachmentIds,String finalApproverId,List<ParallelLine> parallelLines,String beneficiaryId) {}
    public record Sale(String idempotencyKey,String receiptId,int quantity,String saleBasis,String sourceReference,Instant soldAt,
            List<String> evidenceAttachmentIds) {}
    public record SaleConfirmation(String idempotencyKey,String decision,String reason,List<String> evidenceAttachmentIds) {}
    public record Allocation(String idempotencyKey,String sourcePaymentId,String targetItemId,long amountCent,List<String> evidenceAttachmentIds) {}
    public record Return(String idempotencyKey,String receiptId,int quantity,long returnUnitPriceCent,String responsibilityTerms,
            String reason,List<String> evidenceAttachmentIds,String finalApproverId,List<ParallelLine> parallelLines,String beneficiaryId,String consignmentSaleId) {}
    public record ReturnFinanceReview(String idempotencyKey,LocalDate supplierRefundDueDate,String reason,List<String> evidenceAttachmentIds) {}
    public record PrepaymentResolution(String idempotencyKey,String resolutionType,String sourcePaymentId,long amountCent,
            LocalDate refundDueDate,String contractBasis,String reason,List<String> evidenceAttachmentIds,
            String finalApproverId,List<ParallelLine> parallelLines,String beneficiaryId) {}
    public record PrepaymentPost(String idempotencyKey,String reason,List<String> evidenceAttachmentIds) {}
    public record Handover(String idempotencyKey,Instant handedOverAt,List<String> evidenceAttachmentIds) {}
}
