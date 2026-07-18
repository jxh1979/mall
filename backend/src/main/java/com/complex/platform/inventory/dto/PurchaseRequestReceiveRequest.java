package com.complex.platform.inventory.dto;

import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * R5 T6 采购单入库确认请求（arrived → received）。
 *
 * <p>{@code locationId} 落库位（必填）；{@code warehouseId} 关联结构化库房（可空，挂在余额上）；
 * 入库数量取采购单 quantity；生成 inbound 入库流水并关联采购单。
 *
 * <p>V296：{@code unitCostCent} 进货单价（整数分，可空，兼容存量调用）——入库确认时录入，
 * 滚动该物料的移动加权均价；缺省表示未录价（无成本基准，不编造）。
 */
public record PurchaseRequestReceiveRequest(
        String idempotencyKey,
        String locationId,
        String warehouseId,
        List<String> evidenceAttachmentIds,
        String note,
        @PositiveOrZero Long unitCostCent
) {
}
