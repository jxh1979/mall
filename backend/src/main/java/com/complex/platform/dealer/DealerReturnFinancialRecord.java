package com.complex.platform.dealer;

// 文件版本：V0.01
import java.util.List;
public record DealerReturnFinancialRecord(String returnId, long goodsAmountCent, long goodsOffsetCent, long goodsRefundCent,
        long goodsRefundedCent, String freightAdjustmentId, String freightStatus, long firstLegAdjustmentCent,
        long firstLegOffsetCent, long firstLegRefundCent, long firstLegRefundedCent, List<Refund> refunds) {
    public record Refund(String id,String component,long amountCent,String status,String financeRefundId) {}
}
