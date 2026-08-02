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
        // ⚠️ 批10/E(V380)起本字段已封写:内部结算价唯一可编辑处 = 内部结算 SKU 目录;携带非空值一律 400。
        Long internalPriceCent,
        Integer validDays,
        Instant validUntil,
        // V313 券种级"是否可转让"开关（默认 true）：null＝按默认可转让；false＝该券种发出的券不可会员间转赠。
        Boolean transferable,
        // 批10/E(V380)：内部结算 SKU 目录引用(internal_price_catalog.item_code)。
        //   internal_price 结算口径必填(条目须已复核转正且生效价>0);其余口径不得填。
        String catalogItemCode
) {
    /** 向后兼容构造器（批10/E 目录引用前的 14 字段签名）：既有调用方零改动，引用置空。 */
    public VoucherDefinitionRequest(String idempotencyKey, String name, String voucherKind, String redeemMethod,
            String settlementMode, String merchantId, Long faceValueCent, String inventoryItemId,
            String stockLocationId, Integer stockQuantity, Long internalPriceCent, Integer validDays,
            Instant validUntil, Boolean transferable) {
        this(idempotencyKey, name, voucherKind, redeemMethod, settlementMode, merchantId, faceValueCent,
                inventoryItemId, stockLocationId, stockQuantity, internalPriceCent, validDays, validUntil,
                transferable, null);
    }
}
