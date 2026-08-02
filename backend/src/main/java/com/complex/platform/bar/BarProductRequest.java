package com.complex.platform.bar;

import java.util.List;

public record BarProductRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String barId,
        String productName,
        String category,
        Long priceCent,
        Long costCent,
        String skuCode,
        String status,
        String inventoryItemId,
        String stockLocationId,
        Boolean stockDeductionRequired,
        Integer standardPourVolume,
        String pourVolumeUnit,
        Integer lossThreshold,
        // ⚠️ 批10/E(V380)起本字段已封写:内部结算价唯一可编辑处 = 内部结算 SKU 目录(草案→他人复核转正)。
        //   请求携带非空值一律 400(防"以为改好了"的无声空转);商品上的 internal_settlement_price_cent 只是目录价快照。
        Long internalSettlementPriceCent,
        // V256 图形化点单 POS：分类 FK（可空=未分类）+ 商品图片相对路径（可空）。
        String categoryId,
        String imageUrl,
        // 批10/E(V380)：内部结算 SKU 目录引用(internal_price_catalog.item_code,可空)。
        //   引用既有条目或先「就地新建」再引用;条目绑定商户必须 = 本商品所属酒吧(bar_id)。
        String catalogItemCode
) {

}
