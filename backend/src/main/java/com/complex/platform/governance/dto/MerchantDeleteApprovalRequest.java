package com.complex.platform.governance.dto;

/** 商户删除审批：decision=approved|rejected；comment 可空。 */
public record MerchantDeleteApprovalRequest(String idempotencyKey, String decision, String comment) {
}
