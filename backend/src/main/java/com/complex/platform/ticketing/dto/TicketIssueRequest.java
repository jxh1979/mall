package com.complex.platform.ticketing.dto;

import java.util.List;

public record TicketIssueRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String orderId,
        String issueMode,
        List<String> credentialTypes
) {
}
