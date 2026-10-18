package com.complex.platform.dealer.dto;

// 文件版本：V0.01

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** prepayment 转预付；offset_order 未分配款抵订单；prepayment_offset 已转预付款抵订单。 */
public record DealerOverpaymentUseRequest(@NotBlank String idempotencyKey, @NotBlank String usage,
        String targetOrderId, @NotBlank String note, @NotNull @Positive Long amountCent,
        @NotEmpty List<String> confirmationAttachmentIds) {
}
