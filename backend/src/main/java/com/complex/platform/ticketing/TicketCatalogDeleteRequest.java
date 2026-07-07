package com.complex.platform.ticketing;

/** 票务配置删除申请：须先存档；reason 记录删除理由供审批人参考。 */
public record TicketCatalogDeleteRequest(String idempotencyKey, String reason) {
}
