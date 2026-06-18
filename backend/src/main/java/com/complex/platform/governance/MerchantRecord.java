package com.complex.platform.governance;

import java.math.BigDecimal;
import java.time.Instant;

public record MerchantRecord(
        String id,
        String merchantName,
        String merchantType,
        String businessEntityId,
        String contractEntityId,
        String settlementEntityId,
        String contactName,
        String contactPhone,
        String mallBlockCode,
        BigDecimal areaSquareMeter,
        BigDecimal leasedAreaSquareMeter,
        String secondaryContactName,
        String secondaryContactPhone,
        BigDecimal rentPerSquareMeter,
        String cooperationForm,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
