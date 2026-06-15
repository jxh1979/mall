package com.complex.platform.merchantservice.dto;

import java.util.List;

public record MerchantWorkOrderAssignRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ownerDepartment,
        String ownerUserId,
        String operatorUserId
) {
}
