package com.complex.platform.voucher;

import java.time.Instant;

/**
 * 券实例（统一台账）：收编来源＝积分兑换（points_redemption）＋营销赠发（marketing_grant）。
 * 状态机 issued → redeemed / expired / revoked（见 {@link VoucherStateMachine}）。
 *
 * <p>stockReserved（V312 发券预占库存）：true＝该实例发券时已从承接商户实物库存预占了
 * 定义 stock_quantity 件（reserved_quantity+=n / available_quantity-=n）。核销时把预占转实扣、
 * 过期/作废时释放预占，都以此标记为准；历史券（本轮前发的实物券）为 false，走旧 available 直扣路径。
 * 仅 in_store_goods 券可能为 true。
 */
public record VoucherInstanceRecord(
        String id,
        String definitionId,
        String customerId,
        String code,
        String sourceType,
        String sourceRef,
        String status,
        Instant issuedAt,
        Instant expiresAt,
        Instant redeemedAt,
        Instant revokedAt,
        String revokeReason,
        boolean stockReserved,
        Instant createdAt,
        Instant updatedAt
) {
}
