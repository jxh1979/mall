package com.complex.platform.terminal.dto;

import java.util.List;

public record DeviceReturnRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String deviceCode,
        String staffId,
        Boolean hasPendingTasks,
        String reason
) {
}
