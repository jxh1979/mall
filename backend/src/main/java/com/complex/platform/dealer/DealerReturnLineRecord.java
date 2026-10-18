package com.complex.platform.dealer;

// 文件版本：V0.01
/** 退货明细：验收数量拆为可复售（回可用库存）与质量问题（隔离禁售）；金额按原成交价 × 验收数量。 */
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
