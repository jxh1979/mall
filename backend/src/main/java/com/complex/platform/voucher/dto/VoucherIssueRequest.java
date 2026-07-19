package com.complex.platform.voucher.dto;

/** 营销赠发券请求（后台发放入口）：给客人按定义发 quantity 张券（默认 1，上限 100）。 */
public record VoucherIssueRequest(
        String idempotencyKey,
        String definitionId,
        String customerId,
        Integer quantity
) {
}
