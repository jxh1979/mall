package com.complex.platform.merchantservice.dto;

// 文件版本：V0.01

/**
 * 第十八批 M 段第 111 条 111-R2（第十八批裁决 27「派单前商户自己取消；派单后，重复提或误提的由客户关怀部填原因取消」）：
 * 商户专区取消与后台客户关怀部取消共用；reason 必填，写在取消动作的说明里。
 */
public record MerchantWorkOrderCancelRequest(
        String idempotencyKey,
        String reason
) {
}
