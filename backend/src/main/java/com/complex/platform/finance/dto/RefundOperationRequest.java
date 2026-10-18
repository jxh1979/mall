package com.complex.platform.finance.dto;

// 文件版本：V0.01

import java.util.List;
import jakarta.validation.constraints.NotBlank;

public record RefundOperationRequest(
        @NotBlank String idempotencyKey, String operatorNote,
        List<String> evidenceAttachmentIds, String deviceId) {
}
