package com.complex.platform.ops.productionreadiness;

import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessItemUpdateRequest;
import java.time.Instant;
import java.time.LocalDate;

public record ProductionReadinessItem(
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
    public ProductionReadinessItem updateFrom(
            ProductionReadinessItemUpdateRequest request,
            Instant updatedAt,
            String updatedBy
    ) {
        return new ProductionReadinessItem(
                id,
                category,
                itemName,
                defaultSolution,
                blankToExisting(request.ownerDepartment(), ownerDepartment),
                blankToExisting(request.ownerPerson(), ownerPerson),
                request.supplierName(),
                request.status(),
                request.targetDate(),
                request.evidenceLink(),
                request.lastTestTime(),
                blankToExisting(request.blockingScope(), blockingScope),
                blankToExisting(request.fallbackMode(), fallbackMode),
                request.credentialRef(),
                blankToExisting(request.runbookRef(), runbookRef),
                blankToExisting(request.nextAction(), nextAction),
                updatedAt,
                updatedBy
        );
    }

    private String blankToExisting(String value, String existing) {
        return value == null || value.isBlank() ? existing : value;
    }
}
