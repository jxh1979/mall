package com.complex.platform.integration;

import static com.complex.platform.integration.IntegrationValidation.*;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

final class IntegrationOrderSyncService {

    private final IntegrationRepository repository;
    private final Clock clock;
    private final IntegrationOperationSupport support;

    IntegrationOrderSyncService(IntegrationRepository repository, Clock clock, IntegrationOperationSupport support) {
        this.repository = repository;
        this.clock = clock;
        this.support = support;
    }

    PageResponse<IntegrationOrderSyncRecord> listOrderSyncs() {
        return support.readPage("integration.order_sync.viewed", "integration.order_sync.view_failed",
                "integration.order_sync.view", "integration_order_sync", repository::listOrderSyncs);
    }

    IntegrationOrderSyncRecord createOrderSync(IntegrationOrderSyncRequest request) {
        return support.executeWrite(
                "createIntegrationOrderSync",
                request == null ? null : request.idempotencyKey(),
                request,
                IntegrationOrderSyncRecord.class,
                "integration.order_sync.recorded",
                "integration.order_sync.record_failed",
                "integration.order_sync.manage",
                "integration_order_sync",
                () -> createOrderSyncOnce(request)
        );
    }

    PageResponse<IntegrationTaskRecord> listRetryTasks() {
        return support.readPage("integration.retry_task.viewed", "integration.retry_task.view_failed",
                "integration.retry.view", "integration_retry_task", repository::listRetryTasks);
    }

    IntegrationTaskRecord createRetryTask(IntegrationTaskRequest request) {
        return support.executeWrite(
                "createIntegrationRetryTask",
                request == null ? null : request.idempotencyKey(),
                request,
                IntegrationTaskRecord.class,
                "integration.retry_task.created",
                "integration.retry_task.create_failed",
                "integration.retry.manage",
                "integration_retry_task",
                () -> createTaskOnce("retry_task", request)
        );
    }

    IntegrationTaskRecord runRetryTaskNow(String id, StatusChangeRequest request) {
        String taskId = requireTextValue(id, "id");
        return support.executeWrite(
                "runIntegrationRetryTaskNow:" + taskId,
                request == null ? null : request.idempotencyKey(),
                new StatusChangeFingerprint(taskId, request),
                IntegrationTaskRecord.class,
                "integration.retry_task.run_now",
                "integration.retry_task.run_now_failed",
                "integration.retry.manage",
                "integration_retry_task",
                () -> runRetryTaskNowOnce(taskId, request)
        );
    }

    PageResponse<IntegrationTaskRecord> listCompensationTasks() {
        return support.readPage("integration.compensation_task.viewed", "integration.compensation_task.view_failed",
                "integration.compensation.view", "integration_compensation_task", repository::listCompensationTasks);
    }

    IntegrationTaskRecord createCompensationTask(IntegrationTaskRequest request) {
        return support.executeWrite(
                "createIntegrationCompensationTask",
                request == null ? null : request.idempotencyKey(),
                request,
                IntegrationTaskRecord.class,
                "integration.compensation_task.created",
                "integration.compensation_task.create_failed",
                "integration.compensation.manage",
                "integration_compensation_task",
                () -> createTaskOnce("compensation_task", request)
        );
    }

    IntegrationTaskRecord resolveCompensationTask(String id, IntegrationTaskResolveRequest request) {
        String taskId = requireTextValue(id, "id");
        return support.executeWrite(
                "resolveIntegrationCompensationTask:" + taskId,
                request == null ? null : request.idempotencyKey(),
                new ResolveTaskFingerprint(taskId, request),
                IntegrationTaskRecord.class,
                "integration.compensation_task.resolved",
                "integration.compensation_task.resolve_failed",
                "integration.compensation.manage",
                "integration_compensation_task",
                () -> resolveCompensationTaskOnce(taskId, request)
        );
    }

    private IntegrationOrderSyncRecord createOrderSyncOnce(IntegrationOrderSyncRequest request) {
        validateOrderSyncRequest(request);
        Instant now = clock.instant();
        IntegrationOrderSyncRecord record = new IntegrationOrderSyncRecord(
                repository.findOrderSyncByProviderAndExternalId(
                                request.providerCode(),
                                request.businessType(),
                                request.syncType(),
                                request.externalBusinessId()
                        )
                        .map(IntegrationOrderSyncRecord::id)
                        .orElse("order_sync_" + UUID.randomUUID()),
                request.providerCode(),
                request.businessType(),
                request.syncType(),
                request.externalBusinessId(),
                request.status(),
                nonNegative(request.amountCent(), "amount_cent"),
                currency(request.currency()),
                safeMap(request.providerPayload()),
                now,
                now
        );
        return repository.saveOrderSync(record);
    }

    private IntegrationTaskRecord createTaskOnce(String category, IntegrationTaskRequest request) {
        validateTaskRequest(request);
        Instant now = clock.instant();
        IntegrationTaskRecord record = new IntegrationTaskRecord(
                ("retry_task".equals(category) ? "retry_" : "compensation_") + UUID.randomUUID(),
                category,
                request.providerCode(),
                request.taskType(),
                request.businessType(),
                request.externalBusinessId(),
                taskStatus(request.status()),
                0,
                Math.max(0, request.maxRetryCount() == null ? 0 : request.maxRetryCount()),
                request.nextRetryAt(),
                null,
                null,
                safeMap(request.payload()),
                safeList(request.evidenceAttachmentIds()),
                now,
                now
        );
        return "retry_task".equals(category)
                ? repository.saveRetryTask(record)
                : repository.saveCompensationTask(record);
    }

    private IntegrationTaskRecord runRetryTaskNowOnce(String id, StatusChangeRequest request) {
        validateStatusChange(request);
        IntegrationTaskRecord existing = repository.findRetryTaskById(id)
                .orElseThrow(() -> ApiException.notFound("INTEGRATION_RETRY_TASK_NOT_FOUND", "integration retry task not found"));
        IntegrationTaskRecord updated = new IntegrationTaskRecord(
                existing.id(),
                existing.taskCategory(),
                existing.providerCode(),
                existing.taskType(),
                existing.businessType(),
                existing.externalBusinessId(),
                "ready",
                existing.retryCount() + 1,
                existing.maxRetryCount(),
                clock.instant(),
                null,
                request.reason(),
                existing.payload(),
                safeList(request.evidenceAttachmentIds()),
                existing.createdAt(),
                clock.instant()
        );
        return repository.saveRetryTask(updated);
    }

    private IntegrationTaskRecord resolveCompensationTaskOnce(String id, IntegrationTaskResolveRequest request) {
        validateTaskResolveRequest(request);
        IntegrationTaskRecord existing = repository.findCompensationTaskById(id)
                .orElseThrow(() -> ApiException.notFound("INTEGRATION_COMPENSATION_TASK_NOT_FOUND", "integration compensation task not found"));
        IntegrationTaskRecord updated = new IntegrationTaskRecord(
                existing.id(),
                existing.taskCategory(),
                existing.providerCode(),
                existing.taskType(),
                existing.businessType(),
                existing.externalBusinessId(),
                taskStatus(request.status()),
                existing.retryCount(),
                existing.maxRetryCount(),
                existing.nextRetryAt(),
                request.operatorUserId(),
                request.resolutionNote(),
                existing.payload(),
                safeList(request.evidenceAttachmentIds()),
                existing.createdAt(),
                clock.instant()
        );
        return repository.saveCompensationTask(updated);
    }

    private record StatusChangeFingerprint(String id, StatusChangeRequest request) {
    }

    private record ResolveTaskFingerprint(String id, IntegrationTaskResolveRequest request) {
    }
}