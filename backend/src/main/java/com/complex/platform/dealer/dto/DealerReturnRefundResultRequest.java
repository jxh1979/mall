package com.complex.platform.dealer.dto;

/** 退款执行结果：result success / failed；只有成功的退款才计入已退金额（BSM:1242）。 */
public record DealerReturnRefundResultRequest(String idempotencyKey, String result, String referenceNo, String failureReason) {
}
