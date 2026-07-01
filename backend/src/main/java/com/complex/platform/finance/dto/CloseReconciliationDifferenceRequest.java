package com.complex.platform.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 正式关闭对账差异请求（1.6）。close_reason 进审计与落库；idempotency_key 幂等去重。 */
public record CloseReconciliationDifferenceRequest(
        @NotBlank @Size(max = 128) String idempotencyKey,
        @NotBlank @Size(max = 1000) String closeReason,
        List<String> evidenceAttachmentIds
) {
}
