package com.complex.platform.compliance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * R5 T8 企微消息：发送方/脱敏展示内容/原文密文或引用/质检命中/匿名化标记/business_date。
 *
 * <p>V096 起新增 AI 语义质检结果预留字段（与关键词命中 riskFlags 独立并存）：
 * {@code aiQcStatus}（skipped/not_configured/analyzed/failed，对齐 wecom_messages.ai_qc_status）、
 * {@code aiQcResult}（结构化结果 JSON 串，占位为 null）。开关关→skipped；开关开无厂商→not_configured。
 */
public record WecomMessageRecord(
        String id,
        String conversationId,
        String senderRef,
        String contentRawRef,
        String contentRedacted,
        List<String> riskFlags,
        boolean anonymized,
        Instant sentAt,
        LocalDate businessDate,
        Instant createdAt,
        String aiQcStatus,
        String aiQcResult
) {

    /**
     * 向后兼容构造：AI 质检未涉及时（既有调用点/测试），默认 skipped、无结构化结果。
     */
    public WecomMessageRecord(
            String id,
            String conversationId,
            String senderRef,
            String contentRawRef,
            String contentRedacted,
            List<String> riskFlags,
            boolean anonymized,
            Instant sentAt,
            LocalDate businessDate,
            Instant createdAt
    ) {
        this(id, conversationId, senderRef, contentRawRef, contentRedacted, riskFlags, anonymized,
                sentAt, businessDate, createdAt, "skipped", null);
    }
}
