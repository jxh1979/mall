package com.complex.platform.merchantservice.dto;

/** 商户服务工单删除审批：decision=approved|rejected；comment 可空。 */
public record MerchantWorkOrderDeleteApprovalRequest(String decision, String comment) {
}
