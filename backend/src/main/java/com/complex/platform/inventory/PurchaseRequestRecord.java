package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseRequestRecord(
        String id,
        String requestNo,
        String supplierId,
        String itemId,
        int quantity,
        LocalDate expectedArrivalDate,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId
) {
}
