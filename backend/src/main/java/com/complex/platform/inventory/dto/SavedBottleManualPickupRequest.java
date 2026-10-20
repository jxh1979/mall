package com.complex.platform.inventory.dto;

// 文件版本：V0.01

import java.util.List;

/** FS-016 人工例外取酒：店长发起 / 另一主管复核 / 营销确认 三步共用的请求体（reason 发起时必填）。 */
public record SavedBottleManualPickupRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String reason
) {
}
