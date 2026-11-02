package com.complex.platform.voucher;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 第十八批 M 段第 112 条 112-R1（BSM:947「工作手机扫描权益二维码，核销赠饮、礼物或其他领取资格。」；BSM:985「扫码查看客人当前可领取的……
 * 领取状态。」）：按客人出示的券码查券的只读结果。goodsQuantity＝店内实物券每张件数（要逐件扫的物品码个数），其他券为空；
 * blockReason 为空＝可核销，否则同核销拒绝码（VOUCHER_ALREADY_REDEEMED／VOUCHER_REVOKED／VOUCHER_EXPIRED）。
 */
public record VoucherRedeemPreviewRecord(
        String voucherInstanceId,
        String name,
        String voucherKind,
        Integer goodsQuantity,
        String status,
        Instant expiresAt,
        String blockReason
) {
}
