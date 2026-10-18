package com.complex.platform.dealer.dto;

// 文件版本：V0.01
import java.util.List;
public record DealerInvoiceReversalResultRequest(String idempotencyKey,String result,String referenceNo,
        List<String> evidenceAttachmentIds,String failureReason) {}
