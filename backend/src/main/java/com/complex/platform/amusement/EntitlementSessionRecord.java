package com.complex.platform.amusement;

import java.time.Instant;

/**
 * 权益会话记录（entitlement_sessions，V129/REQ-53）。
 *
 * <p>承载「工作手机扫附赠权益码 → 发起会话 → 一体机展示 → 领取核销（防错领 + 会话时效）」闭环。
 * status：initiated（发起）→ displayed（展示）→ claimed（领取核销）；expired/cancelled 为终态。
 * entitlementRef：被扫权益的通用字符串引用（发放 id 或权益码），跨域不强加 FK。
 * merchantId：直接归属列（domain-ownership direct scope，强制取自认证上下文）。
 */
public record EntitlementSessionRecord(
        String id,
        String sessionCode,
        String entitlementRef,
        String merchantId,
        String projectId,
        String status,
        Instant expiresAt,
        String initiatedBy,
        String claimedBy,
        Instant claimedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
