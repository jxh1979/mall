package com.complex.platform.bar;

import java.time.Instant;

/**
 * R5 T15 赠饮兑换桥：一次赠饮核销 → 一张 0 元赠饮单的关联留痕。
 *
 * <p>幂等以 {@code verificationId} 唯一键保证一次核销至多一单；{@code inventoryMovementId} 在出品 deliver
 * 实际扣减后回填；冲正时 {@code status=reversed} 并（若已扣）回补库存。
 */
public record BarGiftRedemptionRecord(
        String id,
        String entitlementId,
        String verificationId,
        String barOrderId,
        String barId,
        String merchantId,
        // 赠饮→吧台商品/SKU 映射引用；无可靠映射来源时为 null（跳过实际扣减并以 note 标注）。
        String itemRef,
        String inventoryMovementId,
        // created / delivered / reversed
        String status,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
}
