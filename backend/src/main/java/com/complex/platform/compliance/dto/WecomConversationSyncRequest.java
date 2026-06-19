package com.complex.platform.compliance.dto;

import java.time.Instant;
import java.util.List;

/**
 * R5 T8 企微会话同步入站（webhook/拉取）。受 T2 开关 integration.wecom.sync 门控。
 * messages 原文落 content_raw_ref（密文/引用），展示内容由服务端脱敏生成，不接受调用方传入展示文本。
 */
public record WecomConversationSyncRequest(
        String idempotencyKey,
        String externalConversationId,
        String projectId,
        String ownerStaffId,
        String customerRef,
        List<Message> messages
) {

    public record Message(
            String externalMessageId,
            String senderRef,
            String content,
            Instant sentAt
    ) {
    }
}
