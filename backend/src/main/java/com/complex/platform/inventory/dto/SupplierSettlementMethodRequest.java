package com.complex.platform.inventory.dto;

/**
 * P1#18 建/改供应商结算档请求。{@code settlementMethod} 必填五类之一。
 */
public record SupplierSettlementMethodRequest(
        String idempotencyKey,
        String supplierId,
        String settlementMethod,
        Integer paymentTermDays,
        String applicableCategory,
        Boolean isDefault,
        String status
) {
}
