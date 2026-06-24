package com.complex.platform.customer.dto;

import java.util.List;
import java.util.Map;

/**
 * 客人自助扫码点单请求（customer-token）。session_id 走路径参（扫桌台二维码得），
 * bar/桌台/预订/商户由后端取自会话，客人只传所点 items（[{product_id, quantity, is_gift?}]）。
 */
public record CustomerBarScanOrderRequest(
        String idempotencyKey,
        List<Map<String, Object>> items
) {
}
