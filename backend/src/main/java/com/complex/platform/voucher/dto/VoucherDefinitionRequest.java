package com.complex.platform.voucher.dto;

import java.time.Instant;

/** 新建券定义请求。类型形状校验见 VoucherService.createDefinition。 */
public record VoucherDefinitionRequest(
        String idempotencyKey,
        String name,
        String voucherKind,
        String redeemMethod,
        String settlementMode,
        String merchantId,
        Long faceValueCent,
        String inventoryItemId,
        String stockLocationId,
        Integer stockQuantity,
        Long internalPriceCent,
        Integer validDays,
        Instant validUntil
) {
}
