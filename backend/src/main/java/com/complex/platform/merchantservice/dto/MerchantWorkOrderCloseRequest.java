package com.complex.platform.merchantservice.dto;

import java.util.List;

public record MerchantWorkOrderCloseRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String resolution,
        Integer satisfactionScore,
        String operatorUserId
) {
}
