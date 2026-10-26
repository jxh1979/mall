package com.complex.platform.integration;

// 文件版本：V0.01

import static com.complex.platform.integration.IntegrationValidation.*;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

final class ProviderAdapterService {

    private final IntegrationRepository repository;
    private final Clock clock;
    private final AuditedOperationExecutor executor;
    // 四个写路径失败留痕带业务对象号（providerCode/id），executor 失败侧固定 businessId=null 无法表达，保留手写直连。
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    ProviderAdapterService(IntegrationRepository repository, Clock clock, AuditedOperationExecutor executor,
                           IdempotencyService idempotencyService, AuditEventService auditEventService) {
        this.repository = repository;
        this.clock = clock;
        this.executor = executor;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    PageResponse<ProviderAdapterRecord> listProviderAdapters() {
        return executor.readPage(AuditedOperation.of("provider_adapter.viewed", "provider_adapter.view_failed",
                "integration.provider_adapter.view", "integration_provider_adapter"), repository::listProviderAdapters);
    }

    ProviderAdapterRecord createProviderAdapter(ProviderAdapterRequest request) {
        try {
            return idempotencyService.execute(
                    "createProviderAdapter",
                    request == null ? null : request.idempotencyKey(),
                    request,
                    () -> createProviderAdapterOnce(request),
                    ProviderAdapterRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "provider_adapter.create_failed",
                    "integration.provider_adapter.create",
                    "integration_provider_adapter",
                    request == null ? null : request.providerCode(),
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    ProviderAdapterRecord updateProviderAdapter(String id, ProviderAdapterUpdateRequest request) {
        try {
            return idempotencyService.execute(
                    "updateProviderAdapter",
                    request == null ? null : request.idempotencyKey(),
                    new UpdateProviderAdapterFingerprint(id, request),
                    () -> updateProviderAdapterOnce(id, request),
                    ProviderAdapterRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "provider_adapter.update_failed",
                    "integration.provider_adapter.update",
                    "integration_provider_adapter",
                    id,
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    ProviderAdapterRecord markProductionReady(String id, StatusChangeRequest request) {
        try {
            return idempotencyService.execute(
                    "markProviderAdapterProductionReady",
                    request == null ? null : request.idempotencyKey(),
                    new StatusChangeFingerprint(id, request),
                    () -> markProductionReadyOnce(id, request),
                    ProviderAdapterRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "provider_adapter.production_ready_failed",
                    "integration.provider_adapter.production_ready",
                    "integration_provider_adapter",
                    id,
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    AdapterTestRecord testProviderAdapterMock(String id, ProviderAdapterTestRequest request) {
        try {
            return idempotencyService.execute(
                    "testProviderAdapterMock",
                    request == null ? null : request.idempotencyKey(),
                    new ProviderAdapterTestFingerprint(id, request),
                    () -> testProviderAdapterMockOnce(id, request),
                    AdapterTestRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "provider_adapter.test_failed",
                    "integration.provider_adapter.test",
                    "integration_provider_adapter",
                    id,
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    /** 保持本类既有留痕口径不变：req_ 随机 request_id / 非业务异常兜底 INTERNAL_ERROR。 */
    private String requestId() {
        return "req_" + UUID.randomUUID();
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }

    private ProviderAdapterRecord createProviderAdapterOnce(ProviderAdapterRequest request) {
        validateCreateRequest(request);
        boolean webhookEnabled = booleanConfig(request.webhookConfig(), "enabled");
        boolean pollingEnabled = booleanConfig(request.pollingConfig(), "enabled");
        boolean productionReady = Boolean.TRUE.equals(request.productionReady());
        // 第十六批第 34 条：新建一律「待接入」（ENUM L211 接入状态七态）。
        String status = "pending";
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
        auditEventService.recordSuccess(
                "provider_adapter.created",
                "integration.provider_adapter.create",
                "integration_provider_adapter",
                saved.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("provider_code", saved.providerCode(), "mode", saved.mode())
        );
        return saved;
    }

    private ProviderAdapterRecord updateProviderAdapterOnce(String id, ProviderAdapterUpdateRequest request) {
        validateUpdateRequest(request);
        rejectSecretLikeCredentialRef(request.credentialSecretRef());
        ProviderAdapterRecord existing = repository.findProviderAdapterById(id)
                .orElseThrow(() -> ApiException.notFound("PROVIDER_ADAPTER_NOT_FOUND", "渠道适配器不存在或已被删除"));
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
                "active".equals(request.status()),
                request.operatorNote(),
                safeList(request.evidenceAttachmentIds()),
                existing.createdAt(),
                clock.instant()
        );
        if (updated.productionReady()) {
            requireProductionReady(updated);
        }
        ProviderAdapterRecord saved = repository.saveProviderAdapter(updated);
        auditEventService.recordSuccess(
                "open_mapping.changed",
                "integration.provider_adapter.update",
                "integration_provider_adapter",
                saved.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("provider_code", saved.providerCode(), "status", saved.status())
        );
        return saved;
    }

    private ProviderAdapterRecord markProductionReadyOnce(String id, StatusChangeRequest request) {
        validateStatusChange(request);
        ProviderAdapterRecord existing = repository.findProviderAdapterById(id)
                .orElseThrow(() -> ApiException.notFound("PROVIDER_ADAPTER_NOT_FOUND", "渠道适配器不存在或已被删除"));
        ProviderAdapterRecord ready = new ProviderAdapterRecord(
                existing.id(),
                existing.providerCode(),
                existing.adapterType(),
                existing.mode(),
                existing.capabilities(),
                existing.environment(),
                "active",
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
        auditEventService.recordSuccess(
                "provider_adapter.production_ready_marked",
                "integration.provider_adapter.production_ready",
                "integration_provider_adapter",
                saved.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("reason", request.reason())
        );
        return saved;
    }

    private AdapterTestRecord testProviderAdapterMockOnce(String id, ProviderAdapterTestRequest request) {
        validateTestRequest(request);
        ProviderAdapterRecord adapter = repository.findProviderAdapterById(id)
                .orElseThrow(() -> ApiException.notFound("PROVIDER_ADAPTER_NOT_FOUND", "渠道适配器不存在或已被删除"));
        if (!adapter.providerCode().equals(request.providerCode()) || !adapterInUse(adapter)) {
            throw new ApiException(HttpStatus.FAILED_DEPENDENCY, "PROVIDER_ADAPTER_NOT_TESTABLE", "该渠道适配器不支持连通性测试");
        }
        Map<String, Object> mockPayload = safeMap(request.mockPayload());
        String testStatus = resolveMockTestStatus(request.scenario(), mockPayload);
        Map<String, Object> result;
        if ("failed".equals(testStatus)) {
            String mockExpect = mockExpect(mockPayload);
            result = Map.of(
                    "scenario", request.scenario(),
                    "adapter_id", adapter.id(),
                    "reason_code", "MOCK_SCENARIO_FAILURE",
                    "mock_expect", mockExpect == null ? "" : mockExpect
            );
        } else {
            result = Map.of("scenario", request.scenario(), "adapter_id", adapter.id());
        }
        AdapterTestRecord saved = repository.saveAdapterTest(new AdapterTestRecord(
                "adapter_test_" + UUID.randomUUID(),
                adapter.id(),
                adapter.providerCode(),
                request.scenario(),
                mockPayload,
                testStatus,
                result,
                clock.instant()
        ));
        auditEventService.recordSuccess(
                "provider_adapter.tested",
                "integration.provider_adapter.test",
                "integration_provider_adapter",
                adapter.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("scenario", request.scenario(), "status", saved.testStatus())
        );
        return saved;
    }

    /**
     * 纯函数：依据 mockPayload 显式期望与 scenario 语义前缀决定 mock 测试判定结果。
     * 优先级 1：mockPayload.expect / mockPayload.force_status 显式指定（failed/passed）→ 直接采用。
     * 优先级 2：scenario 语义前缀——success/paid/ok → passed；failure/failed/error/timeout/reject → failed。
     * 兜底：未识别 scenario → passed（向后兼容既有联调脚本，避免误判）。
     */
    static String resolveMockTestStatus(String scenario, Map<String, Object> mockPayload) {
        String explicit = mockExpect(mockPayload);
        if (explicit != null) {
            return "failed".equals(explicit) ? "failed" : "passed";
        }
        String normalized = scenario == null ? "" : scenario.trim().toLowerCase();
        if (startsWithAny(normalized, "success", "paid", "ok")) {
            return "passed";
        }
        if (startsWithAny(normalized, "failure", "failed", "error", "timeout", "reject")) {
            return "failed";
        }
        return "passed";
    }

    private static String mockExpect(Map<String, Object> mockPayload) {
        if (mockPayload == null) {
            return null;
        }
        Object expect = mockPayload.get("expect");
        if (expect == null) {
            expect = mockPayload.get("force_status");
        }
        if (expect == null) {
            return null;
        }
        return expect.toString().trim().toLowerCase();
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private record UpdateProviderAdapterFingerprint(String id, ProviderAdapterUpdateRequest request) {
    }

    private record StatusChangeFingerprint(String id, StatusChangeRequest request) {
    }

    private record ProviderAdapterTestFingerprint(String id, ProviderAdapterTestRequest request) {
    }
}