package com.complex.platform.merchantservice.dto;

/** 工单存档请求。reason=存档理由(存档时必填,业务HTML 2026-07-07)。 */
public record MerchantWorkOrderArchiveRequest(String reason) {
}
