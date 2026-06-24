package com.complex.platform.leasing;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/** 租赁报价（leasing_offers，V164，② 履约闭环 S4）：针对 lead+空间出报价。status: draft→sent→accepted/rejected/expired→converted。 */
public record LeasingOfferRecord(
        String id,
        String leadId,
        String spaceId,
        String merchantId,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        Map<String, Object> rentTerms,
        LocalDate validUntil,
        String status,
        String convertedContractId,
        String projectId,
        Instant createdAt,
        Instant updatedAt
) {
}
