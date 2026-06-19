package com.complex.platform.gate;

/**
 * supervisor_review_policies 表的准入闸策略行（R5 T3）。三态 outcome=allow/warn/block。
 */
public record AdmissionPolicyRecord(
        String policyCode,
        String actionType,
        String objectType,
        String triggerMode,
        String outcome,
        String approvalLevel,
        int requiredLevel,
        boolean enabled
) {
}
