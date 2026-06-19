package com.complex.platform.governance.review;

import java.util.List;

/**
 * 督导复核策略。
 *
 * <p>{@code requiredApproverPermission}（双轨收口，platform-governance §24.3）：当 {@code approvalLevel} 为
 * {@code specific_role}（不按 level_rank 计算）时，指定审批人必须通过其角色持有的权限码——审批时据此按角色解析，
 * 而非旁路 rank 闸即放行。对按 rank 的升级类型该字段可为 {@code null}（不参与）。为向后兼容，提供不带该字段的
 * 紧凑构造，默认 {@code null}。
 */
public record SupervisorReviewPolicyRecord(
        String policyCode,
        String actionType,
        long minAmountCent,
        int requiredLevel,
        boolean blocksByDefault,
        List<String> requiredEvidenceTypes,
        String status,
        String approvalLevel,
        String requiredApproverPermission
) {

    /** 向后兼容构造：不指定 specific_role 角色权限（{@code null}）。 */
    public SupervisorReviewPolicyRecord(
            String policyCode,
            String actionType,
            long minAmountCent,
            int requiredLevel,
            boolean blocksByDefault,
            List<String> requiredEvidenceTypes,
            String status,
            String approvalLevel
    ) {
        this(policyCode, actionType, minAmountCent, requiredLevel, blocksByDefault,
                requiredEvidenceTypes, status, approvalLevel, null);
    }
}
