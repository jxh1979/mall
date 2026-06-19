package com.complex.platform.inventory.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * P1#18 建/改供应商资质请求。{@code qualificationType} 必填七类之一。
 */
public record SupplierQualificationRequest(
        String idempotencyKey,
        String supplierId,
        String qualificationType,
        String qualificationNo,
        String issuedBy,
        LocalDate validFrom,
        LocalDate validTo,
        List<String> attachmentIds,
        String status
) {
}
