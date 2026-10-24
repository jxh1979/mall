package com.complex.platform.customer.dto;

// 文件版本：V0.01

import java.util.List;
import java.util.Map;

/**
 * 客人扫码点单请求（ROOT-03-R7）。台位走路径参（扫固定桌台二维码得 table_id），当前开台会话、酒吧、商户由后端定位；
 * 客人只传所点 items（[{product_id, quantity}]，不得带 is_gift，ROOT-03-R24）与可选付款渠道 channel（App 可选 wechat/alipay，小程序只能 wechat，不填＝wechat）。
 */
public record CustomerBarScanOrderRequest(
        String idempotencyKey,
        List<Map<String, Object>> items,
        String channel
) {
}
