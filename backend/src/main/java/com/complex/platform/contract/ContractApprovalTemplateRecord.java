package com.complex.platform.contract;

// 文件版本：V0.01

import java.util.List;
import java.util.Set;

/**
 * 合同审批流程模板（V494，HP-C01）：按 (综合体, 合同类型) 一条生效模板，projectId 空 = 平台默认。
 * BSM L1349「合同的新建、到期终止、变更分别使用各自配置的审批流程模板」——requiredLineCodes 为平行相关部门线（approval_lines.line_code），
 * defaultFinalApproverId 为主线默认最终审批人（空 = 后台配置或发起时指定）。
 * minMainLevels/maxMainLevels（V496，HP-C01-R5）：主线自动补全后层级数的上下限（BSM L1349「阶段数与审批岗位按综合体后台可配
 * （业务参数，按综合体可配，须同时配置上下限）」、2026-10-18 裁决第 1 条「主线层级数配上下限」）；两者皆空＝未配置不校验。
 */
public record ContractApprovalTemplateRecord(
        String id,
        String projectId,
        String contractType,        // new / expiry_termination / change
        String name,
        List<String> requiredLineCodes,
        String defaultFinalApproverId,
        boolean active,
        Integer minMainLevels,
        Integer maxMainLevels
) {
    public static final String TYPE_NEW = "new";
    public static final String TYPE_EXPIRY_TERMINATION = "expiry_termination";
    public static final String TYPE_CHANGE = "change";
    public static final Set<String> TYPES = Set.of(TYPE_NEW, TYPE_EXPIRY_TERMINATION, TYPE_CHANGE);
}
