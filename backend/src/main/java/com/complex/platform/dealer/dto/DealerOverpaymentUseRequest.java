package com.complex.platform.dealer.dto;

/** usage：prepayment 转预付款 / offset_order 抵扣指定订单（targetOrderId，须同一经销商且同一销售主体）。 */
public record DealerOverpaymentUseRequest(String idempotencyKey, String usage, String targetOrderId, String note) {
}
