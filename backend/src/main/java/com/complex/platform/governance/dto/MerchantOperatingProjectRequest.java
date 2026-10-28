package com.complex.platform.governance.dto;

// 文件版本：V0.01

/**
 * 商户加经营项目申请（第十八批 E 段第 20 条 20-R1，治理地图第 659 行）。projectId 为要加的经营项目，reason 必填；
 * finalApproverId 为主线最终审批人，不填按发起人直接上级（第 528 行）。
 */
public record MerchantOperatingProjectRequest(String idempotencyKey, String projectId, String reason, String finalApproverId) {
}
