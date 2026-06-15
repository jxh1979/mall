package com.complex.platform.governance.dto;

import java.util.List;

public record MerchantProfileRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String merchantName,
        String merchantType,
        String businessEntityId,
        String contractEntityId,
        String settlementEntityId,
        String contactName,
        String contactPhone,
        String status
) {
}
