package com.complex.platform.dealer;

/** 经销订单明细；priceSource 在通知付款时固化（catalog 目录版本 / inquiry 已报价询价单）。 */
public record DealerOrderLineRecord(
        String id,
        String orderId,
        String itemId,
        String spec,
        int quantity,
        long unitPriceCent,
        Integer taxRateBps,
        String priceSource,
        String inquiryId,
        long lineAmountCent
) {
}
