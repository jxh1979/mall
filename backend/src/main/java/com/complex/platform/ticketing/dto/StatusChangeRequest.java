package com.complex.platform.ticketing.dto;

// 文件版本：V0.02

import java.util.List;

// TK-06（BSM:843「不设需要人脸照的票种」）：出票等状态变更不再收人脸照与人脸同意。
public record StatusChangeRequest(
        String idempotencyKey,
        String action,
        String reason,
        List<String> evidenceAttachmentIds,
        // 操作员裁决第十三批第 11 条「客人端自助申请」：客人自助申请的退票不填退款责任主体，由批准的员工在批准退款时补填
        // （BSM「系统完整记录退款原因、责任主体」）；只在退款批准入口读取，其余状态变更不用。
        String responsibleEntityId
) {
    public StatusChangeRequest(String idempotencyKey, String action, String reason, List<String> evidenceAttachmentIds) {
        this(idempotencyKey, action, reason, evidenceAttachmentIds, null);
    }
}
