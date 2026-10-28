package com.complex.platform.leasing.dto;

// 文件版本：V0.01

/**
 * 黑名单例外放行申请（第十八批第 30 条，PGM L502）。targetType：lead_create（新建同品牌线索，填 brandId）／
 * intent_create（由拉黑前既有线索新建意向，填 leadId，品牌取该线索的品牌）。reason 必填；finalApproverId 为主线最终审批人，
 * 不填按发起人直接上级（PGM L540）。
 */
public record LeasingBlacklistExceptionRequest(
        String targetType,
        String brandId,
        String leadId,
        String reason,
        String finalApproverId,
        String idempotencyKey
) {
}
