package com.complex.platform.merchantservice.dto;

/** 商户服务工单删除审批结果：id + decision(approved|rejected) + 是否已物理删除。 */
public record MerchantWorkOrderDeleteDecision(String id, String decision, boolean deleted) {
}
