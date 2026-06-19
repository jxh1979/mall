package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * P1#18 供应商资质（进货查验绑定）。
 *
 * <p>{@code qualificationType} 七类：{@code business_license}/{@code food_business_license}/
 * {@code production_license}/{@code inspection_report}/{@code hygiene_permit}/{@code import_record}/
 * {@code other}。{@code validTo} 为有效期止（进货查验过期判定依据，可空=长期有效）。
 * {@code status}：active/suspended/archived。
 */
public record SupplierQualificationRecord(
        String id,
        String supplierId,
        String qualificationType,
        String qualificationNo,
        String issuedBy,
        LocalDate validFrom,
        LocalDate validTo,
        List<String> attachmentIds,
        String status,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId
) {
}
