package com.complex.platform.inventory.dto;

// 文件版本：V0.02

import java.util.List;
import java.util.Map;

public record StockMovementRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String fromLocationId,
        String toLocationId,
        List<Map<String, Object>> items,
        String traceCode,
        // FS-018 R1：typed 报损族（damage/loss/waste）转入库存调整契约，命中高价值时需照片+视频双证据各非空；其余类型不读。
        List<String> photoAttachmentIds,
        List<String> videoAttachmentIds
) {
    public StockMovementRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String fromLocationId,
            String toLocationId,
            List<Map<String, Object>> items,
            String traceCode
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, fromLocationId, toLocationId, items, traceCode, null, null);
    }
}
