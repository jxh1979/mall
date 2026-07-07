package com.complex.platform.ticketing.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.Map;

/**
 * C1/REQ-05 人工放行（主管）请求：核销凭证四要素 + 放行原因 reason（必填）。
 * 幂等键必填、reason 必填；权限在端点层按主管权限码 verification.override 控制。
 * 承载与 VerificationRequest 同构的凭证字段，另加 reason，避免复用冲正 DTO（那是按 id 冲正，无凭证）。
 */
public record VerificationManualOverrideRequest(
        String idempotencyKey,
        String credentialType,
        @JsonAlias({"entitlement_code", "entitlementCode"})
        String credentialPayload,
        String deviceId,
        String pointId,
        Map<String, Object> businessContext,
        String reason
) {
    public VerificationRequest toVerificationRequest() {
        return new VerificationRequest(
                idempotencyKey, credentialType, credentialPayload, deviceId, pointId, businessContext);
    }
}
