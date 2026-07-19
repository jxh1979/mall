package com.complex.platform.bar;

/**
 * 订单列表内联的账单摘要：/api/v1/bar/orders 每单带出，供后台账单列显示与「结算」「计算提成」定位账单。
 * status 取账单支付状态（unpaid/paid/partial_paid，展示层按统一码表转中文）。
 */
public record BarBillSummary(String id, String billNo, String status) {
}
