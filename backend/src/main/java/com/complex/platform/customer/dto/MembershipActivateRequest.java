package com.complex.platform.customer.dto;

/** 付费会员生效请求（支付成功后，校验支付意图已 succeeded 再生效）。 */
public record MembershipActivateRequest(
        String planId,
        String projectId,
        String paymentIntentId,
        Boolean autoRenew
) {
}
