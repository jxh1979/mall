package com.complex.platform.dealer.dto;

// 文件版本：V0.01
/** 验收关联真实原签收批次，成本与数量都从该来源恢复。单一来源可由服务端核定。 */
public record DealerReturnInspectionLineRequest(String itemId,String spec,Integer acceptedQuantity,Integer resalableQuantity,
        Integer quarantinedQuantity,String note,String returnLineId) {
    public DealerReturnInspectionLineRequest(String item,String spec,Integer accepted,Integer resalable,Integer quarantined,String note) {
        this(item,spec,accepted,resalable,quarantined,note,null);
    }
}
