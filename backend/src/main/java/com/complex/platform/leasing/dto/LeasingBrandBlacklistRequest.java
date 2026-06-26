package com.complex.platform.leasing.dto;

/** 品牌拉黑/解黑请求（V161 重建，高危需审批 leasing.brand.blacklist）。blacklist=true 拉黑、false 解黑。 */
public record LeasingBrandBlacklistRequest(
        boolean blacklist,
        String reason,
        String idempotencyKey
) {
}
