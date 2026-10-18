package com.complex.platform.access.staff.dto;

// 文件版本：V0.04

/**
 * U05-011：写口带幂等键，与本控制器其它写口同规。
 * CG-12（BSM:1478「须经 HR 与该员工直属领导、向上三级复核后生效」）：状态变更只是发起复核，人力会签人按员工部门的组织授权配置自动解析。
 */
public record StaffEmploymentStatusRequest(String idempotencyKey, String employmentStatus, String reason) {}
