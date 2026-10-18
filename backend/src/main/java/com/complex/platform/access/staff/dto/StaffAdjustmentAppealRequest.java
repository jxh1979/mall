package com.complex.platform.access.staff.dto;

/** CG-12（BSM:1487「员工可对扣分提出申诉，申诉同样报至 HR 与直属领导向上三级裁定」）：申诉事由必填，hrApproverId 为 HR 会签人。 */
public record StaffAdjustmentAppealRequest(String idempotencyKey, String reason, String hrApproverId) {
}
