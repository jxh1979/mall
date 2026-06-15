package com.complex.platform.merchantservice.dto;

import java.util.List;

public record MerchantWorkOrderActionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String targetStatus,
        String operatorUserId
) {
}
