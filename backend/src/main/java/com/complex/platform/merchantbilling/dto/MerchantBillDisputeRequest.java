package com.complex.platform.merchantbilling.dto;

import java.util.List;

public record MerchantBillDisputeRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String reasonCode,
        String description,
        String operatorUserId
) {
}
