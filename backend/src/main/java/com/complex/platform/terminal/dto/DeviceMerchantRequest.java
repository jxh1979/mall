package com.complex.platform.terminal.dto;

/**
 * 设备商户归属设置/更换/解除请求（K16 核销上下文写入面）。
 * merchant_id 传目标商户 = 设置/更换；传空 = 解除绑定（该设备上赠饮/餐饮类权益将无法核销，K16 fail-closed）。
 * reason 必填：改归属改的是“核销记谁的账”，必须留痕说清为什么。
 */
public record DeviceMerchantRequest(
        String idempotencyKey,
        String merchantId,
        String reason
) {
}
