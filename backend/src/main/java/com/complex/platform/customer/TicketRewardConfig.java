package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TicketRewardConfig(String id, String kind, String ticketTypeId, String projectId,
        String version, Values values, Instant effectiveAt, Instant publishedAt, String status,
        String reviewId, String createdBy, Instant createdAt, Instant approvedAt) {
    public record Values(Long fixedValue, Long minimumValue, Long maximumValue,
            Integer waitingHours, Integer minimumWaitingHours, Integer maximumWaitingHours,
            BigDecimal issuancePriceCent, BigDecimal minimumPriceCent, BigDecimal maximumPriceCent,
            String receiverEntityId, String sellerEntityId, String sellerMerchantId,
            String contractNumber, List<String> attachmentIds) {}
    public record Request(String idempotencyKey, String kind, String ticketTypeId, String projectId,
            Values values, Instant effectiveAt, String reason, String finalApproverId,
            List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines) {}
    public record PublishRequest(String idempotencyKey, String reason) {}
}
