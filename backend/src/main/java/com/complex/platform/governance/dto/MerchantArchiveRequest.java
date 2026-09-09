package com.complex.platform.governance.dto;
// 文件版本：V0.01

/** 商户存档/取消存档请求。idempotencyKey 必填；reason 在存档时必填（BSM-004「存档及删除都需要填写理由」），取消存档可空。 */
public record MerchantArchiveRequest(String idempotencyKey, String reason) {
}
