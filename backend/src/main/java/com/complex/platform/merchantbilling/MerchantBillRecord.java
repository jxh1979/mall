package com.complex.platform.merchantbilling;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MerchantBillRecord(
        String id,
        String merchantId,
        String arApItemId,
        String billNo,
        LocalDate billPeriodStart,
        LocalDate billPeriodEnd,
        LocalDate dueDate,
        long totalAmountCent,
        String currency,
        String status,
        int agingDays,
        String sourceType,
        String sourceId,
        List<MerchantBillLineRecord> lines,
        Instant createdAt,
        Instant updatedAt
) {
    public MerchantBillRecord withStatus(String status, Instant updatedAt) {
        return new MerchantBillRecord(
                id,
                merchantId,
                arApItemId,
                billNo,
                billPeriodStart,
                billPeriodEnd,
                dueDate,
                totalAmountCent,
                currency,
                status,
                agingDays,
                sourceType,
                sourceId,
                lines,
                createdAt,
                updatedAt
        );
    }
}
