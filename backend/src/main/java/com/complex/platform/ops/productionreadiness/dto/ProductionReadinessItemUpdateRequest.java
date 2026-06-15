package com.complex.platform.ops.productionreadiness.dto;

import com.complex.platform.ops.productionreadiness.ProductionReadinessStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProductionReadinessItemUpdateRequest(
        String idempotencyKey,
        ProductionReadinessStatus status,
        String ownerDepartment,
        String ownerPerson,
        String supplierName,
        LocalDate targetDate,
        String evidenceLink,
        Instant lastTestTime,
        String blockingScope,
        String fallbackMode,
        String credentialRef,
        String runbookRef,
        String nextAction,
        String operatorNote,
        List<String> evidenceAttachmentIds
) {
}
