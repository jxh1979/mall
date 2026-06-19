package com.complex.platform.terminal.dto;

import java.util.List;

/** R5 T9 MDM 自有侧：设备纳管/策略同步请求。 */
public record MdmDeviceEnrollRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String deviceId,
        String policyStatus,
        String reason
) {
}
