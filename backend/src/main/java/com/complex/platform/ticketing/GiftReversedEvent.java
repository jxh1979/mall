package com.complex.platform.ticketing;

/**
 * R5 T15 事件 {@code bar.gift_reversed} 触发源：核销冲正（T5 reverseVerification）时，若该核销曾触发过
 * 赠饮兑换，则发布本事件，由 bar 域 {@code BarGiftConsumer} 将对应 bar_gift_redemptions 置 reversed、
 * 回补已扣库存，并按原价对冲应付。
 *
 * @param verificationId       被冲正的原核销记录 id（与 bar_gift_redemptions.verification_id 对齐）
 * @param settlementPriceCent  原核销当时固化的内部结算价（分）。S5：冲正按「当时那个价」逐分对冲，
 *                             不回头重查目录（业务 HTML §内部结算价目录「已发生的不可改」）
 */
public record GiftReversedEvent(String verificationId, Long settlementPriceCent) {
}
