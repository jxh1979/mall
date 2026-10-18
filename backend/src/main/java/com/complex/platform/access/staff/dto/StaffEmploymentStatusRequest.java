package com.complex.platform.access.staff.dto;

// 文件版本：V0.03

/**
 * U05-011：写口带幂等键，与本控制器其它写口同规。
 * CG-12（BSM:1478「须经 HR 与该员工直属领导、向上三级复核后生效」）：状态变更只是发起复核，hrApproverId 为 HR 会签人（须经角色持有 staff.hr.review）。
 */
public record StaffEmploymentStatusRequest(String idempotencyKey, String employmentStatus, String reason, String hrApproverId) {}
