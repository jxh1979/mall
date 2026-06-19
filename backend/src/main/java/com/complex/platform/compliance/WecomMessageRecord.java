package com.complex.platform.compliance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** R5 T8 企微消息：发送方/脱敏展示内容/原文密文或引用/质检命中/匿名化标记/business_date。 */
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
        Instant createdAt
) {
}
