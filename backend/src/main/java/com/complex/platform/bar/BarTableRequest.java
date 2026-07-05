package com.complex.platform.bar;

import java.util.List;

public record BarTableRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String barId,
        String tableName,
        String tableType,
        Long minimumSpendCent,
        Integer allowedFreeEntryCount,
        Integer capacity,
        String status
) {
    /** 向后兼容构造器（capacity 前签名）：既有调用方零改动，capacity 默认继承免票人数口径由服务层处理。 */
    public BarTableRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String barId,
            String tableName,
            String tableType,
            Long minimumSpendCent,
            Integer allowedFreeEntryCount,
            String status
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, barId, tableName, tableType,
                minimumSpendCent, allowedFreeEntryCount, null, status);
    }
}
