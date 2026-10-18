package com.complex.platform.inventory.dto;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

/** 实到必须等于合格加拒收，部分验收保留原采购量。 */
public record PurchaseRequestReceiveRequest(String idempotencyKey,String locationId,String warehouseId,
        List<String> evidenceAttachmentIds,String note,Long unitCostCent,
        Integer arrivedQuantity,Integer acceptedQuantity,Integer rejectedQuantity,Instant receivedAt) {
    public PurchaseRequestReceiveRequest(String idempotencyKey,String locationId,String warehouseId,
            List<String> evidenceAttachmentIds,String note,Long unitCostCent) {
        this(idempotencyKey,locationId,warehouseId,evidenceAttachmentIds,note,unitCostCent,null,null,null,null);
    }
}
