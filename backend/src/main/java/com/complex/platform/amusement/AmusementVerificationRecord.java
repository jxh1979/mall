package com.complex.platform.amusement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

public record AmusementVerificationRecord(
        String id,
        String projectId,
        String sessionId,
        @JsonIgnore
        String credentialPayload,
        String deviceId,
        String staffId,
        String verificationResult,
        String failureReason,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        int verifiedPeople
) {
}
