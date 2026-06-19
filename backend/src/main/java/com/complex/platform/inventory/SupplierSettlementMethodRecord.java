package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * P1#18 供应商结算多档/账期（主表 settlement_method 保留为兼容默认）。
 *
 * <p>{@code settlementMethod} 五类：{@code cash_on_delivery}/{@code monthly}/{@code credit_term}/
 * {@code prepaid}/{@code consignment}。{@code paymentTermDays} 账期（可空=即时/无账期）；
 * {@code applicableCategory} 适用品类（空=不限）；{@code isDefault} 默认档（每供应商最多一条 active 默认）。
 */
public record SupplierSettlementMethodRecord(
        String id,
        String supplierId,
        String settlementMethod,
        Integer paymentTermDays,
        String applicableCategory,
        boolean isDefault,
        String status,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId
) {
}
