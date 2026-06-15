package com.complex.platform.terminal.dto;

import java.util.List;

public record DeviceUnbindRequest(
        String idempotencyKey,
        String reason,
        String operatorNote,
        List<String> evidenceAttachmentIds
) {
}
