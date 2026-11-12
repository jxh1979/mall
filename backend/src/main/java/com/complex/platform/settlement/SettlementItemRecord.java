package com.complex.platform.settlement;

/**
 * 结算重构批2-3：各酒吧分账明细（settlement_items 一行）。金额均整数分。
 * 第十八批 AI 段 212-R1：singleBarRouteFeeCent／singleBarChannelFeeCent＝专属直付明细中已扣的单一酒吧门票通路费、销售渠道费（V621），其余分配方式与追收为 0。
 */
public record SettlementItemRecord(
        String id,
        String billId,
        String merchantId,
        int verifiedCount,
        int guestCount,
        Integer rankNo,
        String distributionMethod,
        long calculatedAmountCent,
        long finalAmountCent,
        String status,
        long singleBarRouteFeeCent,
        long singleBarChannelFeeCent
) {
}
