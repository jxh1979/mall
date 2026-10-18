// 文件版本：V0.01
package com.complex.platform.compliance;

import java.util.List;
import java.util.Optional;

/** R5 T8 企微合规链持久化口：会话/消息/原文调阅审计。 */
public interface WecomComplianceRepository {

    /** 校验 projectId 是否为已登记项目（堵 syncWecom 裸信客户端 projectId 向任意/不存在项目注入会话）。 */
    boolean projectExists(String projectId);

    // 会话
    WecomConversationRecord saveConversation(WecomConversationRecord record);

    Optional<WecomConversationRecord> findConversationByExternalId(String externalConversationId);

    Optional<WecomConversationRecord> findConversationById(String id);

    // 消息
    WecomMessageRecord saveMessage(WecomMessageRecord record);

    List<WecomMessageRecord> findMessagesByConversation(String conversationId);

    // 原文/脱敏调阅审计
    WecomAccessAuditRecord saveAccessAudit(WecomAccessAuditRecord record);

    // P0#8 企微违规规则码（读 analytics_risk_rules：domain=compliance、enabled=true）
    List<WecomRiskRuleRecord> findEnabledWecomRiskRules();

    // P0#8 企微违规命中（wecom_risk_hits）
    WecomRiskHitRecord saveRiskHit(WecomRiskHitRecord record);

    List<WecomRiskHitRecord> findRiskHits(String status, String conversationId);

    Optional<WecomRiskHitRecord> findRiskHitById(String hitId);

    WecomRiskHitRecord updateRiskHitReview(
            String hitId, String status, String reviewerId, String reviewNote);

    // P0#8 复核队列衔接（孤岛 analytics_risk_alerts，source_domain=wecom；统一表就位后改指向）
    String enqueueRiskAlert(WecomRiskAlertEnqueue enqueue);

}
