package com.complex.platform.terminal.dto;

import java.util.List;

/** R5 T9 MDM 自有侧：设备违规上报请求。 */
public record MdmPolicyViolationRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String deviceId,
        String violationType,
        String detail
) {
}
