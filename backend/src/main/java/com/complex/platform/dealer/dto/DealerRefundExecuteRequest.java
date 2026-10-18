package com.complex.platform.dealer.dto;

// 文件版本：V0.01

import jakarta.validation.constraints.NotBlank;

/** 将已批准、已核实到账的原路退款交统一财务办理；本请求不确认现金出款。 */
public record DealerRefundExecuteRequest(@NotBlank String idempotencyKey) {
}
