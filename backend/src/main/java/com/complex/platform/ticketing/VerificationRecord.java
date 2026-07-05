package com.complex.platform.ticketing;

import java.time.Instant;

public record VerificationRecord(
        String id,
        String verificationNo,
        String entitlementId,
        String entitlementCode,
        String credentialType,
        String credentialPayload,
        String deviceId,
        String pointId,
        String status,
        String resultReason,
        String sourceType,
        String clientTaskId,
        Instant createdAt,
        String nonce,
        String staffId,
        String merchantId,
        Boolean windowValid,
        Boolean signatureValid,
        // R5 T5：本次核销人数（多人票=num_people，普通票=1）。
        int verifiedPeople,
        // R5 T5 核销冲正：本记录冲正了哪条核销（reversed 记录指向原 success 记录），原记录置 reversed 后记录 reversedBy/reversedAt。
        String reversesVerificationId,
        String reversedBy,
        Instant reversedAt,
        // 结算重构批3-1(PRD §6.1)：本次核销判定出的结算价(分)。实物取酒吧内部结算价/虚拟取签约单价/回退 value_amount。
        // 仅 success 记录写入；expired/invalid/reversal 及旧记录为 null。供对账与 by_entitlement_price 分账取用。
        Long settlementPriceCent,
        // 阶段1（V233）游乐核销分流交叉引用：本条通票核销对应的 amusement_verifications.id
        // （仅游乐类权益走正路核销时写入，便于两侧对账；非游乐核销/历史为 null）。
        String amusementVerificationId
) {

    /**
     * 向后兼容构造（无游乐交叉引用）：既有 23 参数调用点零改动，amusementVerificationId 默认 null。
     */
    public VerificationRecord(
            String id, String verificationNo, String entitlementId, String entitlementCode,
            String credentialType, String credentialPayload, String deviceId, String pointId,
            String status, String resultReason, String sourceType, String clientTaskId, Instant createdAt,
            String nonce, String staffId, String merchantId, Boolean windowValid, Boolean signatureValid,
            int verifiedPeople, String reversesVerificationId, String reversedBy, Instant reversedAt,
            Long settlementPriceCent) {
        this(id, verificationNo, entitlementId, entitlementCode, credentialType, credentialPayload, deviceId,
                pointId, status, resultReason, sourceType, clientTaskId, createdAt, nonce, staffId, merchantId,
                windowValid, signatureValid, verifiedPeople, reversesVerificationId, reversedBy, reversedAt,
                settlementPriceCent, null);
    }
}
