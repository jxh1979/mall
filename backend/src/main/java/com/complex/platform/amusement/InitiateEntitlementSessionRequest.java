package com.complex.platform.amusement;

/**
 * 发起权益会话请求体（V129/REQ-53，工作手机扫附赠权益码）。
 *
 * <p>entitlementRef：被扫权益引用（发放 id 或权益码，必填，非空）。
 * projectId：可选游乐项目（可空）。
 * ttlSeconds：会话有效期秒数（必填且 &gt;0，决定 expires_at = now + ttlSeconds）。
 * initiatedBy：工作手机操作员（可空，缺省取系统占位）。
 * merchant_id 不由请求体传入，强制取自认证上下文（防越权指派他商户）。
 */
public record InitiateEntitlementSessionRequest(
        String idempotencyKey,
        String entitlementRef,
        String projectId,
        Integer ttlSeconds,
        String initiatedBy
) {
}
