package com.complex.platform.contract.dto;

import java.util.List;

/**
 * 配置审批链（按综合体覆盖默认链）：steps 同 stageSeq = 一个并行阶段。
 * projectId 为空 = 改平台默认链。
 */
public record ContractApprovalFlowRequest(
        String idempotencyKey,
        String projectId,
        String name,
        List<Step> steps
) {
    public record Step(int stageSeq, String stageName, String approverPermissionCode) {
    }
}
