package com.complex.platform.ticketing.dto;

// 文件版本：V0.01

import java.util.List;

/**
 * 第十八批 AJ 段 236-R3：票种发布请求。前四项与 {@link StatusChangeRequest} 同名同序；跨域组合通票须另带最终审批人
 * （ENUM:32「项目之上的平台权限统一审批」；PGM:528「发起单据时只需指定该事项的最终审批人」），其余票种不填。
 */
public record TicketTypePublishRequest(
        String idempotencyKey,
        String action,
        String reason,
        List<String> evidenceAttachmentIds,
        String finalApproverId
) {
    public TicketTypePublishRequest(String idempotencyKey, String action, String reason, List<String> evidenceAttachmentIds) {
        this(idempotencyKey, action, reason, evidenceAttachmentIds, null);
    }
}
