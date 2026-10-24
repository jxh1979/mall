package com.complex.platform.finance.gateway;

// 文件版本：V0.01

import java.util.Map;

/**
 * ROOT-03-R18 客人端拉起支付的参数（BSM 第 1552～1554 行「聚合支付」「各渠道（微信、支付宝等）的支付与退款结果……」「不自研支付通道、不沉淀资金」）。
 *
 * @param kind   拉起方式闭集：h5_url（H5 跳转）、app_wechat（App 微信）、app_alipay（App 支付宝）、miniapp_wechat（小程序微信）
 * @param params 由真实适配器按供应商规则签发、原样透传给客户端 SDK 的参数（后端不解释字段）；渠道未配置时整个对象为空
 */
public record PaymentClientLaunch(String kind, Map<String, String> params) {
}
