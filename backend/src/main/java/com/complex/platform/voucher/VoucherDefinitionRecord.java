package com.complex.platform.voucher;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 券定义（模板）：类型三类（merchant_discount 商户折扣券 / in_store_goods 店内实物券 /
 * external_partner 外部伙伴券——本轮实例仍在 V282 权益侧，统一表不发该类实例）。
 * 核销方式（in_store_staff/onsite_code/external_reflow）×结算口径（none/internal_price/pass_settlement）
 * 都是定义级策略字段。
 */
public record VoucherDefinitionRecord(
        String id,
        String name,
        String voucherKind,
        String redeemMethod,
        String settlementMode,
        String merchantId,
        Long faceValueCent,
        String inventoryItemId,
        String stockLocationId,
        Integer stockQuantity,
        Long internalPriceCent,
        Integer validDays,
        Instant validUntil,
        String status,
        // V313 券种级"是否可转让"开关（2026-07-19 用户拍板，默认 true）：该券种下发的所有券继承此设置；
        // false＝该券种发出的券不可会员间转赠（VoucherTransferService.initiate 前置拒绝）。
        boolean transferable,
        Instant createdAt,
        Instant updatedAt,
        // 操作员裁决 2026-10-20 第三轮第 9 条：券定义商户的业态（merchants.merchant_type，只读派生，不落 voucher_definitions）；
        // 「必须结账抵扣」只对 merchant_type=bar 的商户折扣券生效。
        String merchantType
) {

    /** 未带商户业态的构造（写入面与旧调用点）：merchantType 为空。 */
    public VoucherDefinitionRecord(String id, String name, String voucherKind, String redeemMethod, String settlementMode,
                                   String merchantId, Long faceValueCent, String inventoryItemId, String stockLocationId,
                                   Integer stockQuantity, Long internalPriceCent, Integer validDays, Instant validUntil,
                                   String status, boolean transferable, Instant createdAt, Instant updatedAt) {
        this(id, name, voucherKind, redeemMethod, settlementMode, merchantId, faceValueCent, inventoryItemId, stockLocationId,
                stockQuantity, internalPriceCent, validDays, validUntil, status, transferable, createdAt, updatedAt, null);
    }

    /**
     * 内部结算价按权限脱敏（BarProductRecord.withoutInternalPrice 同款铁律，2026-07-08 拍板）：
     * 无 voucher.internal_price.view 权限的读端返回副本内部价置 null（成本仅授权后台可见，店员/客人不可见）。
     */
    public VoucherDefinitionRecord withoutInternalPrice() {
        return new VoucherDefinitionRecord(id, name, voucherKind, redeemMethod, settlementMode, merchantId,
                faceValueCent, inventoryItemId, stockLocationId, stockQuantity,
                null, validDays, validUntil, status, transferable, createdAt, updatedAt, merchantType);
    }
}
