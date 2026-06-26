package com.complex.platform.contract;

/** 审批链步骤（配置）：同 (flowId, stageSeq) 多行 = 一个并行阶段（会签）。 */
public record ContractApprovalFlowStepRecord(
        String id,
        String flowId,
        String projectId,       // 该链所属综合体（NULL=平台默认链）
        int stageSeq,
        String stageName,
        String approverPermissionCode,
        boolean active
) {
}
