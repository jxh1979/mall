package com.complex.platform.finance.dto;

/**
 * 员工退款自助额度配置（upsert，Stage C）。
 * 自然键 (subject_type, subject_id, refund_domain) 幂等：已存在则更新 limit_amount_cent / status。
 * limitAmountCent 必须 >= 0（分）；status 缺省 active。
 */
public record RefundAuthorityLimitRequest(
        String idempotencyKey,
        String subjectType,        // staff | staff_level
        String subjectId,          // 员工 id 或职级 code
        String refundDomain,       // ticket | bar | finance | all
        Long limitAmountCent,      // >= 0（分）
        String projectId,          // 可空：综合体级；空=全局
        String status              // active | inactive（缺省 active）
) {
}
