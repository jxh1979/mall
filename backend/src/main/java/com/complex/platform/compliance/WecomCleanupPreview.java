// 文件版本：V0.01
package com.complex.platform.compliance;

import java.time.Instant;
import java.util.List;

/** 只保存范围、理由和摘要，不复制待清理消息原文。 */
public record WecomCleanupPreview(String id, String createdBy, List<Candidate> candidates,
        String scopeDigest, int messageCount, int riskHitCount, Instant createdAt,
        Instant executedAt, String executedBy, String keyFingerprint) {
    public record Item(String messageId, String reason) {}
    public record Candidate(String messageId, String conversationId, String projectId,
            Instant sentAt, String reason, String messageDigest, List<Hit> riskHits) {}
    public record Hit(String hitId, String ruleCode, String status, String digest) {}
    public record Request(List<Item> items) {}
    public record ExecuteRequest(String challenge, String signature) {}
}
