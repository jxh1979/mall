package com.complex.platform.dealer.dto;

// 文件版本：V0.01

/** 第十八批第 31 条：合同销售主体指定经办人（BSM:1247）。 */
public record DealerDesignatedHandlerRequest(String idempotencyKey, String handlerUserId) {
}
