package com.complex.platform.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * R5 T6 供应合同（三模式）。
 *
 * <p>{@code contractMode} 三模式：{@code platform_self_use}/{@code merchant_self_purchase}/
 * {@code platform_purchase_distribution}。审批 {@code approvalStatus}（draft/approved/rejected）
 * 与生命周期 {@code status}（active/suspended/expired）分列。{@code purchasableScope} 为可采购物料
 * id 列表，approved 且在有效期内方可作为采购依据并卡控可采购范围。
 */
public record SupplyContractRecord(
        String id,
        String contractNo,
        String supplierId,
        String projectId,
        String merchantId,
        String contractMode,
        String supplyScope,
        String priceTerms,
        Integer paymentTermDays,
        LocalDate validFrom,
        LocalDate validTo,
        String summary,
        List<String> purchasableScope,
        String approvalStatus,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
