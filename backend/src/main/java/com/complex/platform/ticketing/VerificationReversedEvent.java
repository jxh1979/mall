package com.complex.platform.ticketing;

/**
 * P1 清单#19 退款/结算冲正联动对接点：核销冲正（T5 reverseVerification）成功后发布本事件，
 * 作为 ticketing 域对外的"核销已冲正"权威信号，供下游（finance 结算冲正、退款侧）订阅做对应冲正。
 *
 * <p>本任务仅在 ticketing 侧发布事件、预留对接点，不改 finance/退款审批主链：
 * <ul>
 *   <li>结算冲正（T7）：finance 域消费者据 verificationId/entitlementId 反向冲销该核销带来的结算确认；</li>
 *   <li>退款联动：若该核销对应订单后续退款，退款侧据 orderId 关联本冲正口径（避免对已冲正核销重复退款/结算）。</li>
 * </ul>
 * 下游未上线前本事件为"发后即弃"（无消费者即 noop），不影响核销冲正主链。
 *
 * @param verificationId    被冲正的原核销记录 id
 * @param reversalId        新建的 status=reversed 冲正记录 id
 * @param entitlementId     被冲正核销对应的权益 id
 * @param orderId           权益所属订单 id（可空：直接发放无订单场景）
 * @param merchantId        权益归属商户 id（结算冲正的业态/商户聚合维度，可空）
 * @param verifiedPeople    被冲正核销的核销人数（多人票冲正口径）
 */
public record VerificationReversedEvent(
        String verificationId,
        String reversalId,
        String entitlementId,
        String orderId,
        String merchantId,
        int verifiedPeople
) {
}
