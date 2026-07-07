package com.complex.platform.ticketing.dto;

/** 权益模板存档/取消存档请求。reason=存档理由(存档时必填,业务HTML 2026-07-07)。 */
public record EntitlementTemplateArchiveRequest(String idempotencyKey, String reason) {
}
