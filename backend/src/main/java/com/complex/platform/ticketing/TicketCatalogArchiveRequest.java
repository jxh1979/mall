package com.complex.platform.ticketing;

/** 票务配置(票种/价格规则)存档/取消存档请求。reason=存档理由(存档时必填,业务HTML 2026-07-07)。 */
public record TicketCatalogArchiveRequest(String idempotencyKey, String reason) {
}
