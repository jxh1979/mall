package com.complex.platform.dealer.dto;

// 文件版本：V0.01

import java.util.List;

public record DealerAdmissionSubmitRequest(String idempotencyKey, List<String> companyAttachmentIds, List<String> contractAttachmentIds,
        List<String> invoiceAttachmentIds, List<String> bankAttachmentIds) {
}
