package com.complex.platform.finance.dto;

// 文件版本：V0.01

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RefundResultConfirmRequest(
        @NotBlank String idempotencyKey, @NotBlank String operatorNote,
        @NotEmpty List<String> evidenceAttachmentIds, @NotBlank String status,
        @NotNull @Positive Long amountCent, @NotBlank String paymentReference,
        @NotBlank String paymentOperatorId, String deviceId) {
}
