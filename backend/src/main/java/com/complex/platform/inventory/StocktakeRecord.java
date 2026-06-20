package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDate;

/**
 * V108 盘点单（stocktake）。盘点流程由供应链/库存域写入，finance 仅只读差异并入账。
 *
 * <p>{@code status}：counting（盘点中）| reviewing（复核中）| closed（关闭）。{@code stocktakeNo} 业务单号唯一。
 * merchantId 强制取自认证上下文，归属隔离。
 */
public record StocktakeRecord(
        String id,
        String stocktakeNo,
        String warehouseId,
        @JsonIgnore String merchantId,
        LocalDate businessDate,
        String status,
        String remark,
        Instant createdAt,
        Instant updatedAt
) {
}
