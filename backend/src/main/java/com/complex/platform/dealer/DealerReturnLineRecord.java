package com.complex.platform.dealer;

// 文件版本：V0.02
/** 退货明细：验收数量拆为可复售（回可用库存）与质量问题（隔离禁售）；金额按单价 × 验收数量，单价为原成交价，原签收行含已完成减价留用的为该行净额÷实收件数（尾差落最后一件）。 */
public record DealerReturnLineRecord(
        String id,
        String returnId,
        String orderLineId,
        String itemId,
        String spec,
        int requestedQuantity,
        int acceptedQuantity,
        int resalableQuantity,
        int quarantinedQuantity,
        long unitPriceCent,
        long acceptedAmountCent,
        String inspectionNote,
        String sourceReceiptLineId
) {
    public DealerReturnLineRecord(String id,String returnId,String orderLineId,String itemId,String spec,int requestedQuantity,int acceptedQuantity,
            int resalableQuantity,int quarantinedQuantity,long unitPriceCent,long acceptedAmountCent,String inspectionNote) {
        this(id,returnId,orderLineId,itemId,spec,requestedQuantity,acceptedQuantity,resalableQuantity,quarantinedQuantity,unitPriceCent,acceptedAmountCent,inspectionNote,null);
    }
}
