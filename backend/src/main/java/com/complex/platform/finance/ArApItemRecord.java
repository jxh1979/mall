package com.complex.platform.finance;

import java.time.Instant;
import java.time.LocalDate;

public record ArApItemRecord(
        String id,
        String itemNo,
        String direction,
        String counterpartyId,
        String sourceType,
        String sourceId,
        long amountCent,
        long paidAmountCent,
        long unpaidAmountCent,
        String currency,
        LocalDate dueDate,
        String accountingPeriod,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
