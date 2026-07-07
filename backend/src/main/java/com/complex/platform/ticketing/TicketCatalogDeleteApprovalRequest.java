package com.complex.platform.ticketing;

/** 票务配置删除审批：decision=approved|rejected；comment 可空。 */
public record TicketCatalogDeleteApprovalRequest(String idempotencyKey, String decision, String comment) {
}
