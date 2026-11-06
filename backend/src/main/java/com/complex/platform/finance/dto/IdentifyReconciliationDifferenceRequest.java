package com.complex.platform.finance.dto;

// 文件版本：V0.01

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 第十八批 Y 段第 181 条 181-R1（BSM:1729「对不上的进差异处理，按原因定位、复核后处理」）：识别对账差异请求——cause_note 为定位到的差异原因，
 * 写入差异并转待处理。
 */
public record IdentifyReconciliationDifferenceRequest(
        @NotBlank @Size(max = 128) String idempotencyKey,
        @NotBlank @Size(max = 1000) String causeNote
) {
}
