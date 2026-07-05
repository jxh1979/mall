package com.complex.platform.amusement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/**
 * 游乐核销记录。
 *
 * <p>阶段2（2026-07-05 用户拍板）新增出场/释放语义列：
 * <ul>
 *   <li>{@code direction}：entry=入场核销（阶段1 既有语义，容量 +N）；exit=出场核销（自由进出型并发容量 -N）。</li>
 *   <li>{@code releasedAt}：该入场记录被出场/主管释放的时刻；null=仍在场。</li>
 *   <li>{@code entryVerificationId}：exit 记录指向其配对入场记录 id（幂等：一入场至多一出场）。</li>
 *   <li>{@code entitlementId}：入场用到的票务权益实例 id（并发硬化唯一约束键 + 出场定位）。</li>
 *   <li>{@code locationPointId}：入场核销点位（出场按同一 point/device 维度键释放并发快照）。</li>
 * </ul>
 */
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
        int verifiedPeople,
        String direction,
        Instant releasedAt,
        String entryVerificationId,
        String entitlementId,
        String locationPointId
) {

    /** 入场核销记录工厂（direction=entry、未释放）：阶段1 既有构造语义的兼容入口。 */
    public static AmusementVerificationRecord entry(
            String id,
            String projectId,
            String sessionId,
            String credentialPayload,
            String deviceId,
            String staffId,
            String verificationResult,
            String failureReason,
            List<String> evidenceAttachmentIds,
            Instant createdAt,
            int verifiedPeople,
            String entitlementId,
            String locationPointId
    ) {
        return new AmusementVerificationRecord(
                id, projectId, sessionId, credentialPayload, deviceId, staffId,
                verificationResult, failureReason, evidenceAttachmentIds, createdAt, verifiedPeople,
                "entry", null, null, entitlementId, locationPointId);
    }
}
