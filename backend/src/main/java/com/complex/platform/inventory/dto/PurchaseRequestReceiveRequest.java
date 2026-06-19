package com.complex.platform.inventory.dto;

import java.util.List;

/**
 * R5 T6 采购单入库确认请求（arrived → received）。
 *
 * <p>{@code locationId} 落库位（必填）；{@code warehouseId} 关联结构化库房（可空，挂在余额上）；
 * 入库数量取采购单 quantity；生成 inbound 入库流水并关联采购单。
 */
public record PurchaseRequestReceiveRequest(
        String idempotencyKey,
        String locationId,
        String warehouseId,
        List<String> evidenceAttachmentIds,
        String note
) {
}
