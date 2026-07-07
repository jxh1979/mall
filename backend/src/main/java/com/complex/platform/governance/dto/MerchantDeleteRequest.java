package com.complex.platform.governance.dto;

/** 商户删除申请：须先存档；reason 记录删除理由供审批人参考。 */
public record MerchantDeleteRequest(String idempotencyKey, String reason) {
}
