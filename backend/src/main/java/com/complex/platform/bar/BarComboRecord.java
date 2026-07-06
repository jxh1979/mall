package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

/** 酒吧套餐（V257）：后台从 SKU 组合而成，有独立售价/内部结算价/图片。items 为组合明细。 */
public record BarComboRecord(
        String id,
        String barId,
        String merchantId,
        String comboName,
        long priceCent,
        Long internalSettlementPriceCent,
        String imageUrl,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<BarComboItemRecord> items
) {
}
