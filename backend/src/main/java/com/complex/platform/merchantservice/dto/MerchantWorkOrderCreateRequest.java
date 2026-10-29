package com.complex.platform.merchantservice.dto;

import java.util.List;

public record MerchantWorkOrderCreateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String merchantId,
        String spaceId,
        String contractId,
        String workOrderType,
        String priority,
        String title,
        String description,
        String reportedByUserId,
        String reportedChannel
) {
}
