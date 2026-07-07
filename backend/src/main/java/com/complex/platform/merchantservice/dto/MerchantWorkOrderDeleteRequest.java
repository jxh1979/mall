package com.complex.platform.merchantservice.dto;

/** 商户服务工单删除申请（数据删除 Tier-B）：须先存档；reason 记录删除理由供审批人参考。 */
public record MerchantWorkOrderDeleteRequest(String reason) {
}
