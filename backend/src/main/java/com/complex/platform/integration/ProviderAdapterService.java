package com.complex.platform.integration;

import static com.complex.platform.integration.IntegrationValidation.*;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

final class ProviderAdapterService {

    private final IntegrationRepository repository;
    private final Clock clock;
    private final IntegrationOperationSupport support;

    ProviderAdapterService(IntegrationRepository repository, Clock clock, IntegrationOperationSupport support) {
        this.repository = repository;
        this.clock = clock;
        this.support = support;
    }

    PageResponse<ProviderAdapterRecord> listProviderAdapters() {
        try {
            List<ProviderAdapterRecord> items = repository.listProviderAdapters();
            support.recordSuccess(
                    "provider_adapter.viewed",
                    "integration.provider_adapter.view",
                    "integration_provider_adapter",
                    "all",
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, 1, 50, items.size());
        } catch (RuntimeException exception) {
            support.recordFailure(
                    "provider_adapter.view_failed",
                    "integration.provider_adapter.view",
                    "integration_provider_adapter",
                    "all",
                    null,
                    support.errorCode(exception)
            );
            throw exception;
        }
    }

    ProviderAdapterRecord createProviderAdapter(ProviderAdapterRequest request) {
        try {
            return support.executeIdempotent(
                    "createProviderAdapter",
                    request == null ? null : request.idempotencyKey(),
                    request,
                    () -> createProviderAdapterOnce(request),
                    ProviderAdapterRecord.class
            );
        } catch (RuntimeException exception) {
            support.recordFailure(
                    "provider_adapter.create_failed",
                    "integration.provider_adapter.create",
                    "integration_provider_adapter",
                    request == null ? null : request.providerCode(),
                    request == null ? null : request.idempotencyKey(),
                    support.errorCode(exception)
            );
            throw exception;
        }
    }

    ProviderAdapterRecord updateProviderAdapter(String id, ProviderAdapterUpdateRequest request) {
        try {
            return support.executeIdempotent(
                    "updateProviderAdapter",
                    request == null ? null : request.idempotencyKey(),
                    new UpdateProviderAdapterFingerprint(id, request),
                    () -> updateProviderAdapterOnce(id, request),
                    ProviderAdapterRecord.class
            );
        } catch (RuntimeException exception) {
            support.recordFailure(
                    "provider_adapter.update_failed",
                    "integration.provider_adapter.update",
                    "integration_provider_adapter",
                    id,
                    request == null ? null : request.idempotencyKey(),
                    support.errorCode(exception)
            );
            throw exception;
        }
    }

    ProviderAdapterRecord markProductionReady(String id, StatusChangeRequest request) {
        try {
            return support.executeIdempotent(
                    "markProviderAdapterProductionReady",
                    request == null ? null : request.idempotencyKey(),
                    new StatusChangeFingerprint(id, request),
                    () -> markProductionReadyOnce(id, request),
                    ProviderAdapterRecord.class
            );
        } catch (RuntimeException exception) {
            support.recordFailure(
                    "provider_adapter.production_ready_failed",
                    "integration.provider_adapter.production_ready",
                    "integration_provider_adapter",
                    id,
                    request == null ? null : request.idempotencyKey(),
                    support.errorCode(exception)
            );
            throw exception;
        }
    }

    AdapterTestRecord testProviderAdapterMock(String id, ProviderAdapterTestRequest request) {
        try {
            return support.executeIdempotent(
                    "testProviderAdapterMock",
                    request == null ? null : request.idempotencyKey(),
                    new ProviderAdapterTestFingerprint(id, request),
                    () -> testProviderAdapterMockOnce(id, request),
                    AdapterTestRecord.class
            );
        } catch (RuntimeException exception) {
            support.recordFailure(
                    "provider_adapter.test_failed",
                    "integration.provider_adapter.test",
                    "integration_provider_adapter",
                    id,
                    request == null ? null : request.idempotencyKey(),
                    support.errorCode(exception)
            );
            throw exception;
        }
    }

    private ProviderAdapterRecord createProviderAdapterOnce(ProviderAdapterRequest request) {
        validateCreateRequest(request);
        boolean webhookEnabled = booleanConfig(request.webhookConfig(), "enabled");
        boolean pollingEnabled = booleanConfig(request.pollingConfig(), "enabled");
        boolean productionReady = Boolean.TRUE.equals(request.productionReady());
        String status = "mock".equals(request.mode()) ? "mock_ready" : "draft";
        ProviderAdapterRecord adapter = new ProviderAdapterRecord(
                "adapter_" + UUID.randomUUID(),
                request.providerCode(),
                request.adapterType(),
                request.mode(),
                List.of(),
                request.mode(),
                status,
                webhookEnabled,
                pollingEnabled,
                safeMap(request.fieldMapping()),
                safeMap(request.webhookConfig()),
                safeMap(request.pollingConfig()),
                null,
                productionReady,
                request.operatorNote(),
                safeList(request.evidenceAttachmentIds()),
                clock.instant(),
                clock.instant()
        );
        if (productionReady) {
            requireProductionReady(adapter);
        }
        ProviderAdapterRecord saved = repository.saveProviderAdapter(adapter);
        support.recordSuccess(
                "provider_adapter.created",
                "integration.provider_adapter.create",
                "integration_provider_adapter",
                saved.id(),
                request.idempotencyKey(),
                Map.of("provider_code", saved.providerCode(), "mode", saved.mode())
        );
        return saved;
    }

    private ProviderAdapterRecord updateProviderAdapterOnce(String id, ProviderAdapterUpdateRequest request) {
        validateUpdateRequest(request);
        rejectSecretLikeCredentialRef(request.credentialSecretRef());
        ProviderAdapterRecord existing = repository.findProviderAdapterById(id)
                .orElseThrow(() -> ApiException.notFound("PROVIDER_ADAPTER_NOT_FOUND", "provider adapter not found"));
        ProviderAdapterRecord updated = new ProviderAdapterRecord(
                existing.id(),
                request.providerCode(),
                existing.adapterType(),
                existing.mode(),
                safeList(request.capabilities()),
                request.environment(),
                request.status(),
                Boolean.TRUE.equals(request.webhookEnabled()),
                Boolean.TRUE.equals(request.pollingCheckEnabled()),
                safeMap(request.fieldMappingPlaceholder()),
                existing.webhookConfig(),
                existing.pollingConfig(),
                request.credentialSecretRef(),
                "production_ready".equals(request.status()),
                request.operatorNote(),
                safeList(request.evidenceAttachmentIds()),
                existing.createdAt(),
                clock.instant()
        );
        if (updated.productionReady()) {
            requireProductionReady(updated);
        }
        ProviderAdapterRecord saved = repository.saveProviderAdapter(updated);
        support.recordSuccess(
                "open_mapping.changed",
                "integration.provider_adapter.update",
                "integration_provider_adapter",
                saved.id(),
                request.idempotencyKey(),
                Map.of("provider_code", saved.providerCode(), "status", saved.status())
        );
        return saved;
    }

    private ProviderAdapterRecord markProductionReadyOnce(String id, StatusChangeRequest request) {
        validateStatusChange(request);
        ProviderAdapterRecord existing = repository.findProviderAdapterById(id)
                .orElseThrow(() -> ApiException.notFound("PROVIDER_ADAPTER_NOT_FOUND", "provider adapter not found"));
        ProviderAdapterRecord ready = new ProviderAdapterRecord(
                existing.id(),
                existing.providerCode(),
                existing.adapterType(),
                existing.mode(),
                existing.capabilities(),
                existing.environment(),
                "production_ready",
                existing.webhookEnabled(),
                existing.pollingCheckEnabled(),
                existing.fieldMapping(),
                existing.webhookConfig(),
                existing.pollingConfig(),
                existing.credentialSecretRef(),
                true,
                existing.operatorNote(),
                existing.evidenceAttachmentIds(),
                existing.createdAt(),
                clock.instant()
        );
        requireProductionReady(ready);
        ProviderAdapterRecord saved = repository.saveProviderAdapter(ready);
        support.recordSuccess(
                "provider_adapter.production_ready_marked",
                "integration.provider_adapter.production_ready",
                "integration_provider_adapter",
                saved.id(),
                request.idempotencyKey(),
                Map.of("reason", request.reason())
        );
        return saved;
    }

    private AdapterTestRecord testProviderAdapterMockOnce(String id, ProviderAdapterTestRequest request) {
        validateTestRequest(request);
        ProviderAdapterRecord adapter = repository.findProviderAdapterById(id)
                .orElseThrow(() -> ApiException.notFound("PROVIDER_ADAPTER_NOT_FOUND", "provider adapter not found"));
        if (!adapter.providerCode().equals(request.providerCode()) || "disabled".equals(adapter.status())) {
            throw new ApiException(HttpStatus.FAILED_DEPENDENCY, "PROVIDER_ADAPTER_NOT_TESTABLE", "provider adapter is not testable");
        }
        AdapterTestRecord saved = repository.saveAdapterTest(new AdapterTestRecord(
                "adapter_test_" + UUID.randomUUID(),
                adapter.id(),
                adapter.providerCode(),
                request.scenario(),
                safeMap(request.mockPayload()),
                "passed",
                Map.of("scenario", request.scenario(), "adapter_id", adapter.id()),
                clock.instant()
        ));
        support.recordSuccess(
                "provider_adapter.tested",
                "integration.provider_adapter.test",
                "integration_provider_adapter",
                adapter.id(),
                request.idempotencyKey(),
                Map.of("scenario", request.scenario(), "status", saved.testStatus())
        );
        return saved;
    }

    private record UpdateProviderAdapterFingerprint(String id, ProviderAdapterUpdateRequest request) {
    }

    private record StatusChangeFingerprint(String id, StatusChangeRequest request) {
    }

    private record ProviderAdapterTestFingerprint(String id, ProviderAdapterTestRequest request) {
    }
}