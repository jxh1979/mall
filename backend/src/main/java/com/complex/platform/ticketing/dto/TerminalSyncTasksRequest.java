package com.complex.platform.ticketing.dto;

import java.util.List;

public record TerminalSyncTasksRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String deviceCode,
        String staffId,
        List<TerminalSyncTaskItemRequest> tasks
) {
}
