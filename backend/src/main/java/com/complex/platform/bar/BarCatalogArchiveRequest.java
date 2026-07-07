package com.complex.platform.bar;

/** 酒吲目录/卡座预订存档请求。reason=存档理由(存档时必填,业务HTML 2026-07-07)。 */
public record BarCatalogArchiveRequest(String reason) {
}
