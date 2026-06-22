package com.complex.platform.ticketing.dto;

/**
 * 确认票务订单支付成功（2a-2 支付回写闭环）。
 *
 * <p>两条来源共用此入口（用户裁定"两者都做"）：
 * <ul>
 *   <li>内部确认：staff 代客收款 / 对账后由后台确认（gatewayRef 可空）。</li>
 *   <li>真实第三方回调：支付渠道 webhook 通知成功（gatewayRef 携带渠道流水号）。</li>
 * </ul>
 * 服务端据 paymentIntentId 校验归属与金额后，将支付意图置 succeeded 并回写订单 pay_status=paid。
 */
public record ConfirmTicketOrderPaymentRequest(
        String idempotencyKey,
        String paymentIntentId,
        // 真实回调渠道流水号（可空）：内部确认可留空，仅作留痕。
        String gatewayRef
) {
}
