package com.complex.platform.access.staff.dto;

/** 员工档案删除申请（数据删除 Tier-B）：须先存档；reason 记录删除理由供审批人参考。 */
public record StaffProfileDeleteRequest(String reason) {
}
