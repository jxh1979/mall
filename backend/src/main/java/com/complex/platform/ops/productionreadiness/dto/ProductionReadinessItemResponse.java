package com.complex.platform.ops.productionreadiness.dto;

import com.complex.platform.ops.productionreadiness.ProductionReadinessStatus;
import java.time.Instant;
import java.time.LocalDate;

public record ProductionReadinessItemResponse(
        String id,
        String category,
        String itemName,
        String defaultSolution,
        String ownerDepartment,
        String ownerPerson,
        String supplierName,
        ProductionReadinessStatus status,
        LocalDate targetDate,
        String evidenceLink,
        Instant lastTestTime,
        String blockingScope,
        String fallbackMode,
        String credentialRef,
        String runbookRef,
        String nextAction,
        Instant updatedAt,
        String updatedBy
) {
}
