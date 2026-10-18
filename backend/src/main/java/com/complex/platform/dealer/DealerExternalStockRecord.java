package com.complex.platform.dealer;

import java.time.LocalDate;

/**
 * 经销库存外显（BSM:1221 / ENUM:175）：只给 有货（in_stock）、紧张（low）、缺货（out_of_stock）、预计交期（expected_restock）四态，
 * 不带任何数量、仓库总库存或内部调拨信息。
 */
public record DealerExternalStockRecord(
        String itemId,
        String itemName,
        String spec,
        long unitPriceCent,
        String availability,
        LocalDate expectedRestockDate
) {
    public static final String IN_STOCK = "in_stock";
    public static final String LOW = "low";
    public static final String OUT_OF_STOCK = "out_of_stock";
    public static final String EXPECTED_RESTOCK = "expected_restock";
}
