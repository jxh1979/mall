package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;

public record AmusementWaiverAcknowledgementRecord(
        String id,
        String noticeId,
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
        String operatorUserId,
        List<String> evidenceAttachmentIds,
        Instant createdAt
) {
}
