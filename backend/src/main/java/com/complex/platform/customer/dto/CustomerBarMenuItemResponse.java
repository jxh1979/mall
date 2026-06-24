package com.complex.platform.customer.dto;

/**
 * 客人侧扫码点单菜单项（安全投影）：只暴露客人可见的售卖信息。
 * 刻意不含 internal_settlement_price_cent（平台↔酒吧内部结算成本价，敏感，绝不下发客人端）。
 */
public record CustomerBarMenuItemResponse(
        String productId,
        String productName,
        String productType,
        long priceCent,
        String currency,
        boolean soldOut
) {
}
