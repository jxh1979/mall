package com.complex.platform.voucher.dto;

import java.util.List;

/**
 * 统一核销请求：按核销码或实例 id 二选一定位券。第十八批 L 段 103-R1（BSM:1197「……出库须同时核验订单和物品码」）：
 * 店内实物券带 {@code traceCodes} 逐件扫的物品码。
 */
public record VoucherRedeemRequest(
        String idempotencyKey,
        String code,
        String voucherInstanceId,
        String note,
        List<String> traceCodes
) {
    public VoucherRedeemRequest(String idempotencyKey, String code, String voucherInstanceId, String note) {
        this(idempotencyKey, code, voucherInstanceId, note, null);
    }
}
