package com.complex.platform.finance.dto;

// 文件版本：V0.01

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record RefundExecutionRequest(
        @NotBlank String idempotencyKey, @NotBlank String operatorNote,
        @NotEmpty List<String> evidenceAttachmentIds,
        @NotBlank String paymentReference, String deviceId) {
}
