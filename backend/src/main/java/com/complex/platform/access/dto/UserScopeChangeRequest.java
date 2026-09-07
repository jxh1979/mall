package com.complex.platform.access.dto;

/**
 * M087-A-04（PGM-1.10.1「保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批」）：
 * 人员范围变更入参。organizationId / merchantId 为变更后的归属（merchantId 为空 = 平台员工）；positionCode / staffLevelId 为空时
 * 沿用当前岗位与职级；finalApproverId 可指定主线最终审批人（PGM-1.7.1）。
 */
public record UserScopeChangeRequest(
        String idempotencyKey,
        String operatorNote,
        String organizationId,
        String merchantId,
        String positionCode,
        String staffLevelId,
        Long baseSalaryAmountCent,
        String salaryCurrency,
        String salaryDetail,
        String reason,
        String finalApproverId
) {
}
