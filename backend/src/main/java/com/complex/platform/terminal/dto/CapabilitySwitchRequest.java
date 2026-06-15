package com.complex.platform.terminal.dto;

import java.util.List;

public record CapabilitySwitchRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Boolean enabled,
        String terminalType,
        String capabilityCode,
        String scopeId
) {
}
