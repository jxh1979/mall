package com.complex.platform.inventory.dto;

import java.util.List;

/** 存酒作废请求（A1/REQ-32）。高价值存酒作废必须提供照片+视频双证据（>=2 个证据附件）。 */
public record SavedBottleCancelRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String reason
) {
}
