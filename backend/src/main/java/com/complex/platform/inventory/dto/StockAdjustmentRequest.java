package com.complex.platform.inventory.dto;

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
        Long unitCostCents,
        // R5 T6-4：命中高价值时需照片+视频双证据各非空（建复核单）；非高价值可不传（向后兼容）。
        List<String> photoAttachmentIds,
        List<String> videoAttachmentIds
) {
}
