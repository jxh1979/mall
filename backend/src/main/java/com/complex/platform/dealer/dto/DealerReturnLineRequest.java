package com.complex.platform.dealer.dto;

// 文件版本：V0.01
public record DealerReturnLineRequest(String itemId,String spec,Integer quantity,String sourceReceiptLineId) {
    public DealerReturnLineRequest(String item,String spec,Integer quantity){this(item,spec,quantity,null);}
}
