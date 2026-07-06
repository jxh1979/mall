package com.complex.platform.ticketing.dto;

/** 权益模板删除审批：decision=approved|rejected；comment 可空。 */
public record EntitlementTemplateDeleteApprovalRequest(String idempotencyKey, String decision, String comment) {
}
