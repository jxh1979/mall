package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public record InventorySupplierRecord(
        String id,
        String supplierName,
        String supplierType,
        String contactName,
        String contactPhone,
        String status,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId
) {

    /**
     * P0-A/GAP-07 展示脱敏：无 {@code pii.contact_phone.view} 权限的读端返回掩码手机号。
     * 脱敏放在 controller（service 外），与 V279 内部结算价同款范式。
     */
    public InventorySupplierRecord withMaskedPhone() {
        return new InventorySupplierRecord(id, supplierName, supplierType, contactName,
                com.complex.platform.common.PhoneMask.mask(contactPhone), status, createdAt, updatedAt, merchantId);
    }
}
