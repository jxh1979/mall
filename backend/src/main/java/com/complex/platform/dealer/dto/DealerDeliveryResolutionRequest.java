package com.complex.platform.dealer.dto;

// 文件版本：V0.01

import java.util.List;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;

public record DealerDeliveryResolutionRequest(String idempotencyKey, int quantity, String resolutionType,
        String physicalRoute, long adjustmentCent, String responsibilityParty, String agreementBasis,
        List<String> evidenceAttachmentIds, String finalApproverId, List<ParallelLine> parallelLines) {}
