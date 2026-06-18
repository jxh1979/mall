package com.complex.platform.integration;

import static com.complex.platform.integration.IntegrationValidation.*;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

final class IntegrationReconciliationService {

    private final IntegrationRepository repository;
    private final Clock clock;
    private final IntegrationOperationSupport support;

    IntegrationReconciliationService(IntegrationRepository repository, Clock clock, IntegrationOperationSupport support) {
        this.repository = repository;
        this.clock = clock;
        this.support = support;
    }

    PageResponse<ReconciliationBatchRecord> listReconciliationBatches() {
        return support.readPage("integration.reconciliation_batch.viewed", "integration.reconciliation_batch.view_failed",
                "integration.reconciliation.view", "integration_reconciliation_batch", repository::listReconciliationBatches);
    }

    ReconciliationBatchRecord createReconciliationBatch(ReconciliationBatchRequest request) {
        return support.executeWrite(
                "createIntegrationReconciliationBatch",
                request == null ? null : request.idempotencyKey(),
                request,
                ReconciliationBatchRecord.class,
                "integration.reconciliation_batch.created",
                "integration.reconciliation_batch.create_failed",
                "integration.reconciliation.manage",
                "integration_reconciliation_batch",
                () -> createReconciliationBatchOnce(request)
        );
    }

    PageResponse<ReconciliationDifferenceRecord> listReconciliationDifferences() {
        return support.readPage("integration.reconciliation_difference.viewed", "integration.reconciliation_difference.view_failed",
                "integration.reconciliation.view", "integration_reconciliation_difference", repository::listReconciliationDifferences);
    }

    ReconciliationDifferenceRecord createReconciliationDifference(ReconciliationDifferenceRequest request) {
        return support.executeWrite(
                "createIntegrationReconciliationDifference",
                request == null ? null : request.idempotencyKey(),
                request,
                ReconciliationDifferenceRecord.class,
                "integration.reconciliation_difference.created",
                "integration.reconciliation_difference.create_failed",
                "integration.reconciliation.manage",
                "integration_reconciliation_difference",
                () -> createReconciliationDifferenceOnce(request)
        );
    }

    ReconciliationDifferenceRecord resolveReconciliationDifference(
            String id,
            ReconciliationDifferenceResolveRequest request
    ) {
        String differenceId = requireTextValue(id, "id");
        return support.executeWrite(
                "resolveIntegrationReconciliationDifference:" + differenceId,
                request == null ? null : request.idempotencyKey(),
                new ResolveDifferenceFingerprint(differenceId, request),
                ReconciliationDifferenceRecord.class,
                "integration.reconciliation_difference.resolved",
                "integration.reconciliation_difference.resolve_failed",
                "integration.reconciliation.resolve",
                "integration_reconciliation_difference",
                () -> resolveReconciliationDifferenceOnce(differenceId, request)
        );
    }

    private ReconciliationBatchRecord createReconciliationBatchOnce(ReconciliationBatchRequest request) {
        validateReconciliationBatchRequest(request);
        int expectedCount = nonNegative(request.expectedCount(), "expected_count");
        int actualCount = nonNegative(request.actualCount(), "actual_count");
        int differenceCount = nonNegative(request.differenceCount(), "difference_count");
        if (expectedCount != actualCount && differenceCount == 0) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "difference_count must be positive when expected_count differs from actual_count");
        }
        Instant now = clock.instant();
        return repository.saveReconciliationBatch(new ReconciliationBatchRecord(
                "reconciliation_batch_" + UUID.randomUUID(),
                request.providerCode(),
                request.businessType(),
                request.periodStart(),
                request.periodEnd(),
                differenceCount > 0 ? "difference_found" : "matched",
                expectedCount,
                actualCount,
                differenceCount,
                nonNegative(request.totalDifferenceCent(), "total_difference_cent"),
                now,
                now
        ));
    }

    private ReconciliationDifferenceRecord createReconciliationDifferenceOnce(ReconciliationDifferenceRequest request) {
        validateReconciliationDifferenceRequest(request);
        ReconciliationBatchRecord batch = repository.findReconciliationBatchById(request.batchId())
                .orElseThrow(() -> ApiException.notFound("RECONCILIATION_BATCH_NOT_FOUND", "reconciliation batch not found"));
        if (!batch.providerCode().equals(request.providerCode()) || !batch.businessType().equals(request.businessType())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "difference scope must match reconciliation batch");
        }
        long internalAmountCent = nonNegative(request.internalAmountCent(), "internal_amount_cent");
        long providerAmountCent = nonNegative(request.providerAmountCent(), "provider_amount_cent");
        long differenceAmountCent = Math.abs(internalAmountCent - providerAmountCent);
        Instant now = clock.instant();
        return repository.saveReconciliationDifference(new ReconciliationDifferenceRecord(
                "reconciliation_difference_" + UUID.randomUUID(),
                request.batchId(),
                request.providerCode(),
                request.businessType(),
                request.externalBusinessId(),
                requireTextValue(request.differenceType(), "difference_type"),
                internalAmountCent,
                providerAmountCent,
                differenceAmountCent,
                "open",
                request.assignedToUserId(),
                null,
                safeMap(request.providerPayload()),
                safeList(request.evidenceAttachmentIds()),
                now,
                now
        ));
    }

    private ReconciliationDifferenceRecord resolveReconciliationDifferenceOnce(
            String id,
            ReconciliationDifferenceResolveRequest request
    ) {
        validateReconciliationDifferenceResolveRequest(request);
        ReconciliationDifferenceRecord existing = repository.findReconciliationDifferenceById(id)
                .orElseThrow(() -> ApiException.notFound("RECONCILIATION_DIFFERENCE_NOT_FOUND", "reconciliation difference not found"));
        ReconciliationDifferenceRecord updated = new ReconciliationDifferenceRecord(
                existing.id(),
                existing.batchId(),
                existing.providerCode(),
                existing.businessType(),
                existing.externalBusinessId(),
                existing.differenceType(),
                existing.internalAmountCent(),
                existing.providerAmountCent(),
                existing.differenceAmountCent(),
                reconciliationStatus(request.status()),
                request.operatorUserId(),
                request.resolutionNote(),
                existing.providerPayload(),
                safeList(request.evidenceAttachmentIds()),
                existing.createdAt(),
                clock.instant()
        );
        return repository.saveReconciliationDifference(updated);
    }

    private record ResolveDifferenceFingerprint(String id, ReconciliationDifferenceResolveRequest request) {
    }
}