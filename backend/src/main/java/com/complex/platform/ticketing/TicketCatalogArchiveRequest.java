package com.complex.platform.ticketing;

/** 票务配置(票种/价格规则)存档/取消存档请求。仅需幂等键。 */
public record TicketCatalogArchiveRequest(String idempotencyKey) {
}
