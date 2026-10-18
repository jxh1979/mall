package com.complex.platform.dealer;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

public record DealerAdmissionRecord(String dealerId, List<String> companyAttachmentIds, List<String> contractAttachmentIds,
        List<String> invoiceAttachmentIds, List<String> bankAttachmentIds, String submittedBy, Instant submittedAt) {
}
