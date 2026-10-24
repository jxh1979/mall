package com.complex.platform.customer.dto;

// 文件版本：V0.01

/**
 * ROOT-03-R4 客人在线付款发起：只收请求编号与渠道（wechat／alipay，小程序只能 wechat；不填＝wechat），金额由服务端按订单待付金额或定金定，
 * 客户端传来的金额一律不认。
 */
public record CustomerPaymentRequest(String idempotencyKey, String channel) {
}
