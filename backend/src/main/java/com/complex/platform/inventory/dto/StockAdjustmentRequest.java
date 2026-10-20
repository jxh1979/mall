package com.complex.platform.inventory.dto;

// 文件版本：V0.02

import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Map;

public record StockAdjustmentRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String adjustmentType,
        List<Map<String, Object>> items,
        String reason,
        // R5 T6-4：报损/盘点差异高价值判定单价（整数分，可空）。与物料 high_value_threshold_cents 共同判定。
        @PositiveOrZero Long unitCostCents,
        // R5 T6-4：命中高价值时需照片+视频双证据各非空（建复核单）；非高价值可不传（向后兼容）。
        List<String> photoAttachmentIds,
        List<String> videoAttachmentIds
) {
    /**
     * FS-018 R1：typed stock-movements 的报损族（damage/loss/waste）改写为同一份库存调整请求，
     * 与 /stock-adjustments 走完全同一实现（高价值判定 + 双证据 + 主管复核批准后才扣减）。
     * 库位取 from_location_id（调整按 item.location_id 取库位）；原因取 operator_note（为空由 requireText(reason) 拦下）；
     * 单价不传（高价值判定只看最近一次实际到货价）。
     */
    public static StockAdjustmentRequest fromTypedMovement(String type, StockMovementRequest r) {
        List<Map<String, Object>> items = r.items() == null ? null : r.items().stream().map(item -> {
            Map<String, Object> copy = new java.util.LinkedHashMap<>(item);
            copy.put("location_id", r.fromLocationId());
            return copy;
        }).toList();
        return new StockAdjustmentRequest(r.idempotencyKey(), r.operatorNote(), r.evidenceAttachmentIds(), type, items,
                r.operatorNote(), null, r.photoAttachmentIds(), r.videoAttachmentIds());
    }
}
