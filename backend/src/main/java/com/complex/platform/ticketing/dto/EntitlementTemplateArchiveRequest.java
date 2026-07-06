package com.complex.platform.ticketing.dto;

/** 权益模板存档/取消存档请求。仅需幂等键（存档动作无额外入参）。 */
public record EntitlementTemplateArchiveRequest(String idempotencyKey) {
}
