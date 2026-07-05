package com.complex.platform.amusement;

import java.time.Instant;

/**
 * 游乐核销记录 只读列表项（阶段3 后台展示）。
 *
 * <p>在核销记录本体列上叠加「关联通票号」：经 ticket_verifications.amusement_verification_id
 * 反查 verification_no（D3 交叉引用）。非游乐正路核销/历史记录该列为 null。
 * 只读投影，不参与任何写路径（核销/容量/释放主逻辑均不经过本记录）。
 */
public record AmusementVerificationListItem(
        String id,
        String projectId,
        String sessionId,
        String deviceId,
        String staffId,
        String verificationResult,
        int verifiedPeople,
        String direction,
        Instant createdAt,
        Instant releasedAt,
        String entryVerificationId,
        String entitlementId,
        String locationPointId,
        String linkedTicketNo
) {
}
