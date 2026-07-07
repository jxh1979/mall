package com.complex.platform.access.staff.dto;

/** 员工档案删除审批结果：id + decision(approved|rejected) + 是否已物理删除。 */
public record StaffProfileDeleteDecision(String id, String decision, boolean deleted) {
}
