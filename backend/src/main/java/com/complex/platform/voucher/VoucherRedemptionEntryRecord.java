package com.complex.platform.voucher;

import java.time.Instant;

/**
 * 统一核销记录（append-only）：核销点/操作员/结算挂钩一张表。
 * settlement_mode=internal_price 时 settlementAmountCent=内部结算价、arApItemId=应付挂钩；
 * 实物券出库以 stockMovementRef 关联库存流水（幂等键）。
 */
public record VoucherRedemptionEntryRecord(
        String id,
        String voucherInstanceId,
        String definitionId,
        String voucherKind,
        String customerId,
        String merchantId,
        String operatorUserId,
        String redeemMethod,
        String settlementMode,
        Long settlementAmountCent,
        String arApItemId,
        String stockMovementRef,
        String externalRedemptionRef,
        String note,
        Instant redeemedAt,
        Instant createdAt
) {

    /**
     * 内部结算价脱敏（settlement_amount_cent 即内部结算价，2026-07-08 铁律）：无 voucher.internal_price.view
     * 权限的读端返回副本 settlement_amount_cent 置 null；结算模式/应付 id 保留（非成本数值本身）。
     */
    public VoucherRedemptionEntryRecord withoutInternalPrice() {
        return new VoucherRedemptionEntryRecord(id, voucherInstanceId, definitionId, voucherKind, customerId,
                merchantId, operatorUserId, redeemMethod, settlementMode,
                null, arApItemId, stockMovementRef, externalRedemptionRef, note, redeemedAt, createdAt);
    }
}
