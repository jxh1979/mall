package com.complex.platform.inventory.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * R5 T6 建供应合同（三模式）请求。
 *
 * <p>{@code contractMode} 必填三模式之一。{@code purchasableScope} 为可采购物料 id 列表，用于审批后
 * 卡控可采购范围。{@code merchantId} 仅平台管理员可显式指定；商户上下文调用时强制取当前商户作用域。
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
