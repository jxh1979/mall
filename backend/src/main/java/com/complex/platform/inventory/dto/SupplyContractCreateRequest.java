package com.complex.platform.inventory.dto;

// 文件版本：V0.01

import java.time.LocalDate;
import java.util.List;

/**
 * R5 T6 建供应合同（三模式）请求。
 *
 * <p>{@code contractMode} 必填三模式之一。{@code purchasableScope} 为可采购物料 id 列表，用于审批后
 * 卡控可采购范围。非商户人员须以真实项目权限指定采购货权；商户上下文强制使用当前商户，平台采购模式不挂商户货权。
 */
public record SupplyContractCreateRequest(
        String idempotencyKey,
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
        List<String> purchasableScope
) {
}
