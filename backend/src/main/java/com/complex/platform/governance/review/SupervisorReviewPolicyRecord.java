package com.complex.platform.governance.review;

import java.util.List;

/**
 * 督导复核策略。
 *
 * <p>{@code requiredApproverPermission}（双轨收口，platform-governance §24.3）：当 {@code approvalLevel} 为
 * {@code specific_role}（不按 level_rank 计算）时，指定审批人必须通过其角色持有的权限码——审批时据此按角色解析，
 * 而非旁路 rank 闸即放行。对按 rank 的升级类型该字段可为 {@code null}（不参与）。
 *
 * <p>{@code parallelLineCodes}（M087-A-03，PGM-1.7.1 平行线 / PGM-1.10.1 税费三审）：该事项固定要求的平行审批线；
 * 发起时必须为每条线指定最终审批人，否则拒绝发起（fail-closed）。为向后兼容，提供不带这两个字段的紧凑构造。
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
        String requiredApproverPermission,
        List<String> parallelLineCodes
) {
    public SupervisorReviewPolicyRecord {
        parallelLineCodes = parallelLineCodes == null ? List.of() : List.copyOf(parallelLineCodes);
    }

    /** 向后兼容构造：不指定 specific_role 角色权限（{@code null}），无固定平行线。 */
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
                requiredEvidenceTypes, status, approvalLevel, null, List.of());
    }

    /** 向后兼容构造：指定 specific_role 角色权限，无固定平行线。 */
    public SupervisorReviewPolicyRecord(
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
        this(policyCode, actionType, minAmountCent, requiredLevel, blocksByDefault,
                requiredEvidenceTypes, status, approvalLevel, requiredApproverPermission, List.of());
    }
}
