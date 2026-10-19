package com.complex.platform.contract.dto;

// 文件版本：V0.01

import java.util.List;

/**
 * 配置合同审批流程模板（V494，HP-C01）：按 (综合体, 合同类型) 覆盖平台默认；projectId 为空 = 改平台默认模板。
 * contractType ∈ new / expiry_termination / change；requiredLineCodes 为平行相关部门线码（须在 approval_lines 且 active）；
 * defaultFinalApproverId 为主线默认最终审批人（可空 = 发起时指定；非空须为 active 用户）。
 * minMainLevels/maxMainLevels（HP-C01-R5）：主线自动补全后层级数的最少/最多；都不填＝不校验，填则两者同时填写且 1≤最少≤最多。
 */
public record ContractApprovalFlowRequest(
        String idempotencyKey,
        String projectId,
        String contractType,
        String name,
        List<String> requiredLineCodes,
        String defaultFinalApproverId,
        Integer minMainLevels,
        Integer maxMainLevels
) {
}
