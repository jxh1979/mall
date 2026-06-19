package com.complex.platform.compliance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** R5 T8 企微合规链持久化口：会话/消息/留存策略/原文调阅审计。 */
public interface WecomComplianceRepository {

    // 留存策略
    WecomRetentionPolicyRecord saveRetentionPolicy(WecomRetentionPolicyRecord record);

    List<WecomRetentionPolicyRecord> findAllRetentionPolicies();

    Optional<WecomRetentionPolicyRecord> findEffectivePolicy(String projectId);

    // 会话
    WecomConversationRecord saveConversation(WecomConversationRecord record);

    Optional<WecomConversationRecord> findConversationByExternalId(String externalConversationId);

    Optional<WecomConversationRecord> findConversationById(String id);

    List<WecomConversationRecord> findConversationsDueForRetention(LocalDate today);

    void updateConversationArchiveStatus(String id, String archiveStatus);

    // 消息
    WecomMessageRecord saveMessage(WecomMessageRecord record);

    List<WecomMessageRecord> findMessagesByConversation(String conversationId);

    void anonymizeMessagesByConversation(String conversationId);

    void deleteMessagesByConversation(String conversationId);

    // 原文/脱敏调阅审计
    WecomAccessAuditRecord saveAccessAudit(WecomAccessAuditRecord record);

    List<WecomAccessAuditRecord> findAccessAuditsByConversation(String conversationId);
}
