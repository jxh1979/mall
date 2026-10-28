package com.complex.platform.customer;

// 文件版本：V0.03

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 奖励规则版本。第十一批 TK-04-a：增 merchant_points / merchant_growth 两类（按 businessFormat 商户业态全国生效，
 * fixedValue＝每产生 1 积分／记 1 成长值所需有效消费，分），全国参数增商户消费等待期三项（与门票等待期分开）。
 * 第十八批第 11 条：增 merchant_card_bonus（商户级卡充值赠送比例，按发卡商户 merchantId）与 stored_value_bonus（综合体通用卡充值赠送比例，按项目），
 * fixedValue＝赠送比例（万分比）。
 */
public record TicketRewardConfig(String id, String kind, String ticketTypeId, String projectId,
        String version, Values values, Instant effectiveAt, Instant publishedAt, String status,
        String reviewId, String createdBy, Instant createdAt, Instant approvedAt, String businessFormat, String merchantId) {
    public record Values(Long fixedValue, Long minimumValue, Long maximumValue,
            Integer waitingHours, Integer minimumWaitingHours, Integer maximumWaitingHours,
            BigDecimal issuancePriceCent, BigDecimal minimumPriceCent, BigDecimal maximumPriceCent,
            String receiverEntityId, String sellerEntityId, String sellerMerchantId,
            String contractNumber, List<String> attachmentIds,
            Integer merchantWaitingHours, Integer minimumMerchantWaitingHours, Integer maximumMerchantWaitingHours) {
        public Values(Long fixedValue, Long minimumValue, Long maximumValue,
                Integer waitingHours, Integer minimumWaitingHours, Integer maximumWaitingHours,
                BigDecimal issuancePriceCent, BigDecimal minimumPriceCent, BigDecimal maximumPriceCent,
                String receiverEntityId, String sellerEntityId, String sellerMerchantId,
                String contractNumber, List<String> attachmentIds) {
            this(fixedValue, minimumValue, maximumValue, waitingHours, minimumWaitingHours, maximumWaitingHours,
                    issuancePriceCent, minimumPriceCent, maximumPriceCent, receiverEntityId, sellerEntityId,
                    sellerMerchantId, contractNumber, attachmentIds, null, null, null);
        }
    }
    public record Request(String idempotencyKey, String kind, String ticketTypeId, String projectId,
            Values values, Instant effectiveAt, String reason, String finalApproverId,
            List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines, String businessFormat, String merchantId) {
        public Request(String idempotencyKey, String kind, String ticketTypeId, String projectId,
                Values values, Instant effectiveAt, String reason, String finalApproverId,
                List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines) {
            this(idempotencyKey, kind, ticketTypeId, projectId, values, effectiveAt, reason, finalApproverId,
                    parallelLines, null, null);
        }
        public Request(String idempotencyKey, String kind, String ticketTypeId, String projectId,
                Values values, Instant effectiveAt, String reason, String finalApproverId,
                List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines, String businessFormat) {
            this(idempotencyKey, kind, ticketTypeId, projectId, values, effectiveAt, reason, finalApproverId,
                    parallelLines, businessFormat, null);
        }
    }
    public record PublishRequest(String idempotencyKey, String reason) {}
}
