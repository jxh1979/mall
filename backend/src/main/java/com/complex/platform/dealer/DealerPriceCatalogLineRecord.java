package com.complex.platform.dealer;

import java.time.LocalDate;

/** 价格目录明细：商品 + 规格 → 合同单价（分）；税率只作审计快照（ENUM:180）；预计交期用于缺货外显（BSM:1221）。 */
public record DealerPriceCatalogLineRecord(
        String id,
        String catalogId,
        String itemId,
        String spec,
        long unitPriceCent,
        Integer taxRateBps,
        LocalDate expectedRestockDate
) {
}
