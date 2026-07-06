package com.complex.platform.ticketing.dto;

/** 权益模板删除申请：须先存档；reason 记录删除理由供审批人参考。 */
public record EntitlementTemplateDeleteRequest(String idempotencyKey, String reason) {
}
