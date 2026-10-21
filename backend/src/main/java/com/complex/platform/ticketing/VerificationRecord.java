package com.complex.platform.ticketing;

// 文件版本：V0.01

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
        String amusementVerificationId,
        // TK-14（V524，ENUM「核销记录状态」「异常状态的统一出口」）：status=anomaly 的异常复核记录指向的原成功核销；
        // 处置（restored 更正后恢复 / voided_reversed 作废并冲正）、处置人、时间、原因写在异常记录上，待处置时均为 null。
        String anomalyOfVerificationId,
        String anomalyResolution,
        String anomalyResolvedBy,
        Instant anomalyResolvedAt,
        String anomalyResolutionReason
) {

    /** 既有 24 参数调用点（含游乐交叉引用）零改动：异常复核字段默认 null。 */
    public VerificationRecord(
            String id, String verificationNo, String entitlementId, String entitlementCode,
            String credentialType, String credentialPayload, String deviceId, String pointId,
            String status, String resultReason, String sourceType, String clientTaskId, Instant createdAt,
            String nonce, String staffId, String merchantId, Boolean windowValid, Boolean signatureValid,
            int verifiedPeople, String reversesVerificationId, String reversedBy, Instant reversedAt,
            Long settlementPriceCent, String amusementVerificationId) {
        this(id, verificationNo, entitlementId, entitlementCode, credentialType, credentialPayload, deviceId,
                pointId, status, resultReason, sourceType, clientTaskId, createdAt, nonce, staffId, merchantId,
                windowValid, signatureValid, verifiedPeople, reversesVerificationId, reversedBy, reversedAt,
                settlementPriceCent, amusementVerificationId, null, null, null, null, null);
    }

    /** 读库时补上异常复核列（行映射器只映到游乐交叉引用为止）。 */
    VerificationRecord withAnomaly(String of, String resolution, String resolvedBy, Instant resolvedAt, String resolutionReason) {
        return new VerificationRecord(id, verificationNo, entitlementId, entitlementCode, credentialType, credentialPayload,
                deviceId, pointId, status, resultReason, sourceType, clientTaskId, createdAt, nonce, staffId, merchantId,
                windowValid, signatureValid, verifiedPeople, reversesVerificationId, reversedBy, reversedAt,
                settlementPriceCent, amusementVerificationId, of, resolution, resolvedBy, resolvedAt, resolutionReason);
    }

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
