package com.complex.platform.bar;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record BarEntitlementDrinkRedemptionRequest(
        @JsonAlias("idempotency_key")
        String idempotencyKey,
        @JsonAlias("verification_id")
        String verificationId,
        @JsonAlias("operator_note")
        String operatorNote,
        @JsonAlias("evidence_attachment_ids")
        List<String> evidenceAttachmentIds
) {
}
