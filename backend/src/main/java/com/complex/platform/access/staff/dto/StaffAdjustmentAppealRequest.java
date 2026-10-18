package com.complex.platform.access.staff.dto;

// 文件版本：V0.01

/** 申诉事由必填，人力会签人按员工部门的组织授权配置自动解析。 */
public record StaffAdjustmentAppealRequest(String idempotencyKey, String reason) {
}
