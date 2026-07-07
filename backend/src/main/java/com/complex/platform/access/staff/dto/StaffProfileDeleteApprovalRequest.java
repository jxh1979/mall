package com.complex.platform.access.staff.dto;

/** 员工档案删除审批：decision=approved|rejected；comment 可空。 */
public record StaffProfileDeleteApprovalRequest(String decision, String comment) {
}
