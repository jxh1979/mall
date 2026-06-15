package com.complex.platform.amusement.dto;

import java.time.Instant;
import java.util.List;

public record AmusementWaiverAcknowledgementRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectId,
        String amusementProjectId,
        String sessionId,
        String customerId,
        String visitorName,
        String signerName,
        String acknowledgementMethod,
        String contentVersion,
        String contentHash,
        Instant acknowledgedAt,
        String operatorUserId
) {
}
