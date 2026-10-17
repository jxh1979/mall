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
        long lineAmountCent,
        /** CG-11c：累计已退数量，只由退货链回写，明细保存不覆盖。 */
        int returnedQuantity
) {
    public DealerOrderLineRecord(String id, String orderId, String itemId, String spec, int quantity, long unitPriceCent, Integer taxRateBps, String priceSource,
            String inquiryId, long lineAmountCent) {
        this(id, orderId, itemId, spec, quantity, unitPriceCent, taxRateBps, priceSource, inquiryId, lineAmountCent, 0);
    }
}
