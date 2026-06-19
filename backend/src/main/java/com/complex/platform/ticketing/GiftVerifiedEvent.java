package com.complex.platform.ticketing;

/**
 * R5 T15 事件 {@code ticket.gift_verified}：赠饮权益（entitlement_type='gift_drink'）核销成功后发布。
 *
 * <p>事件驱动、同步发布（便于测试且与核销同事务）。bar 域 {@code BarGiftConsumer} 监听后生成 0 元赠饮单
 * 并扣吧台库存。本事件不与 T4/T5 核销主链耦合——主链照常完成，本事件仅作为下游联动触发器。
 *
 * @param entitlementId   被核销的 issued_entitlement id
 * @param verificationId  本次核销记录 id（幂等键来源：bar_gift_redemptions.verification_id 唯一）
 * @param itemRef         赠饮→吧台商品/SKU 映射引用；当前票种/权益模板无该配置来源时为 null，
 *                        下游据此跳过实际扣减并在 redemption 标注（见 BarGiftConsumer TODO）
 * @param barId           赠饮所属吧台 id（来源：权益 point_id；缺失则下游标注无法落单）
 * @param tableSessionId  关联台位会话 id（可选，留痕用）
 * @param merchantId      商户 id（成本归平台，不计商户应收，仅留痕归属）
 */
public record GiftVerifiedEvent(
        String entitlementId,
        String verificationId,
        String itemRef,
        String barId,
        String tableSessionId,
        String merchantId
) {
}
