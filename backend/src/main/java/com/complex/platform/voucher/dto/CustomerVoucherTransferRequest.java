package com.complex.platform.voucher.dto;

/**
 * V313 发起券转赠。channel=phone 需 receiverPhone；channel=link（微信/二维码）服务端生成一次性领取码。
 * voucherInstanceId＝要转赠的统一台账券实例 id（客人「我的券包」里 source=unified 的可用券）。
 */
public record CustomerVoucherTransferRequest(
        String idempotencyKey,
        String voucherInstanceId,
        String channel,
        String receiverPhone
) {
}
