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
        Instant validUntil,
        // V313 券种级"是否可转让"开关（默认 true）：null＝按默认可转让；false＝该券种发出的券不可会员间转赠。
        Boolean transferable
) {
}
