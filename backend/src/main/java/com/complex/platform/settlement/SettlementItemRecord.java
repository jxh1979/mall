package com.complex.platform.settlement;

/** 结算重构批2-3：各酒吧分账明细（settlement_items 一行）。金额均整数分。 */
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
        String status
) {
}
