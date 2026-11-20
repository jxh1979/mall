package com.complex.platform.access.staff.dto;

/**
 * 员工档案删除审批：decision=approved|rejected；comment 可空。
 * idempotencyKey 请求编号（第十八批 BG 段第 261 条 261-B21-R4）：经接口提交必填，同一编号重发返回首次审批结果。
 */
public record StaffProfileDeleteApprovalRequest(String decision, String comment, String idempotencyKey) {

    public StaffProfileDeleteApprovalRequest(String decision, String comment) {
        this(decision, comment, null);
    }
}
