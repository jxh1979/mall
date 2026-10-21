// 文件版本：V0.01
package com.complex.platform.compliance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * R5 T8 企微消息分级视图。redacted 模式 content 为脱敏文本、rawContent 始终 null；
 * raw 模式（需 wecom.view_raw + 审批）才回填 rawContent 会话原文（落库密文解密后返回）。
 */
public record WecomMessageView(
        String id,
        String conversationId,
        String senderRef,
        String content,
        String rawContent,
        List<String> riskFlags,
        boolean anonymized,
        Instant sentAt,
        LocalDate businessDate
) {
}
