package com.complex.platform.integration;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class IntegrationService {

    private static final long OPEN_SIGNATURE_TOLERANCE_MILLIS = 300_000;
    private static final long OPEN_TOKEN_TTL_SECONDS = 3_600;
    private static final String OPEN_PERMISSION = "not_applicable";
    private static final Set<String> ADAPTER_TYPES = Set.of(
            "payment",
            "ticket_channel",
            "face_age",
            "wecom",
            "robot",
            "video",
            "device",
            "retail",
            "catering"
    );
    private static final Set<String> MODES = Set.of("mock", "sandbox", "production");
    private static final Set<String> STATUSES = Set.of("draft", "mock_ready", "test_ready", "production_ready", "disabled");
    private static final Set<String> TEST_SCENARIOS = Set.of("auth", "webhook", "polling", "reconcile", "field_mapping", "failure_retry");
    private static final Set<String> BUSINESS_TYPES = Set.of("catering", "retail");
    private static final Set<String> SYNC_TYPES = Set.of("order", "refund", "voucher_verification");
    private static final Set<String> SYNC_STATUSES = Set.of("received", "synced", "failed", "difference_found");
    private static final Set<String> TASK_STATUSES = Set.of("open", "ready", "running", "failed", "resolved", "ignored");
    private static final Set<String> RECONCILIATION_STATUSES = Set.of("open", "difference_found", "matched", "resolved", "ignored");
    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("(?i).*(password|passwd|secret|api[_-]?key|access[_-]?token|private[_-]?key)\\s*[:=].+"),
            Pattern.compile("(?i).*(sk-live|sk_test|sk-proj|AKIA[0-9A-Z]{16}|-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----).*"),
            Pattern.compile("(?i).*bearer\\s+[a-z0-9._\\-]{20,}.*")
    );

    private final IntegrationRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final SecretResolver secretResolver;
    private final Clock clock;
    private final ObjectMapper signatureObjectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public IntegrationService(
            IntegrationRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            SecretResolver secretResolver
    ) {
        this(repository, idempotencyService, auditEventService, Clock.systemUTC(), secretResolver);
    }

    IntegrationService(
            IntegrationRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            Clock clock,
            SecretResolver secretResolver
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.clock = clock;
        this.secretResolver = secretResolver;
        this.signatureObjectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public OpenAuthTokenResponse issueOpenToken(OpenAuthTokenRequest request) {
        String appKey = request == null ? null : request.appKey();
        String nonce = request == null ? null : request.nonce();
        try {
            OpenAppCredentialRecord credential = verifyOpenSignature(new OpenSignatureRequest(
                    appKey,
                    request == null ? null : request.timestamp(),
                    nonce,
                    request == null ? null : request.signature()
            ), null, true);
            Instant issuedAt = clock.instant();
            Instant expiresAt = issuedAt.plusSeconds(OPEN_TOKEN_TTL_SECONDS);
            String rawToken = newRawToken();
            repository.saveOpenToken(new OpenAppTokenRecord(
                    UUID.randomUUID().toString(),
                    credential.appKey(),
                    sha256(rawToken),
                    issuedAt,
                    expiresAt,
                    "active",
                    nonce,
                    issuedAt
            ));
            auditEventService.recordSuccess(
                    "open_auth.succeeded",
                    OPEN_PERMISSION,
                    "open_app_credential",
                    credential.appKey(),
                    requestId(),
                    null,
                    Map.of("app_key", credential.appKey())
            );
            return new OpenAuthTokenResponse(rawToken, "Bearer", expiresAt);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "open_auth.failed",
                    OPEN_PERMISSION,
                    "open_app_credential",
                    blankToUnknown(appKey),
                    requestId(),
                    null,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    public OpenEventResultResponse getOpenEventResult(String endpointKey, String eventId, OpenSignatureRequest request) {
        try {
            OpenAppCredentialRecord credential = verifyOpenSignature(request, endpointKey, true);
            OpenApiEventRecord event = repository.findOpenEventById(endpointKey, eventId)
                    .orElseThrow(() -> ApiException.notFound("OPEN_EVENT_NOT_FOUND", "open event not found"));
            auditEventService.recordSuccess(
                    "api_call.logged",
                    OPEN_PERMISSION,
                    "open_api_event",
                    event.id(),
                    requestId(),
                    null,
                    Map.of("app_key", credential.appKey(), "endpoint_key", endpointKey)
            );
            return new OpenEventResultResponse(event.id(), event.eventStatus(), event.resultJson());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "api_call.rejected",
                    OPEN_PERMISSION,
                    "open_api_event",
                    blankToUnknown(eventId),
                    requestId(),
                    null,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    public WebhookAcceptedResponse receiveWebhook(
            String endpointKey,
            String provider,
            String event,
            String signature,
            String idempotencyKey,
            Map<String, Object> payload
    ) {
        try {
            String resolvedIdempotencyKey = idempotencyKey(idempotencyKey, payload);
            requireText(endpointKey, "endpoint_key");
            requireText(provider, "provider");
            requireText(event, "event");
            requireText(resolvedIdempotencyKey, "idempotency_key");
            OpenEndpointConfigRecord endpoint = repository.findEndpointConfig(endpointKey)
                    .filter(config -> "active".equals(config.status()))
                    .orElseThrow(() -> ApiException.unauthorized("OPEN_ENDPOINT_DISABLED", "open endpoint is disabled"));
            if (!provider.equals(endpoint.providerCode()) || !endpoint.webhookEnabled()) {
                throw ApiException.unauthorized("OPEN_WEBHOOK_REJECTED", "open webhook endpoint is not enabled");
            }
            String payloadHash = payloadHash(payload);
            verifySignature(signature, webhookCanonical(endpointKey, provider, event, resolvedIdempotencyKey, payloadHash), resolveSecret(endpoint.secretRef()), "OPEN_WEBHOOK_SIGNATURE_INVALID");

            WebhookAcceptedResponse response = repository.findOpenEventByIdempotency(endpointKey, provider, resolvedIdempotencyKey)
                    .map(existing -> new WebhookAcceptedResponse(existing.id(), true))
                    .orElseGet(() -> {
                        Instant now = clock.instant();
                        String eventId = eventId(payload);
                        repository.saveOpenEvent(new OpenApiEventRecord(
                                eventId,
                                endpointKey,
                                provider,
                                event,
                                resolvedIdempotencyKey,
                                true,
                                "accepted",
                                payload == null ? Map.of() : Map.copyOf(payload),
                                Map.of("accepted", true, "payload_hash", payloadHash),
                                null,
                                now,
                                now
                        ));
                        return new WebhookAcceptedResponse(eventId, false);
                    });
            auditEventService.recordSuccess(
                    "api_call.logged",
                    OPEN_PERMISSION,
                    "open_webhook",
                    response.eventId(),
                    requestId(),
                    resolvedIdempotencyKey,
                    Map.of("endpoint_key", endpointKey, "provider", provider, "deduplicated", response.deduplicated())
            );
            return response;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "api_call.rejected",
                    OPEN_PERMISSION,
                    "open_webhook",
                    blankToUnknown(endpointKey),
                    requestId(),
                    idempotencyKey,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    public PageResponse<IntegrationLedgerRecord> listLedger(String ledgerType, String successEvent, String failureEvent) {
        try {
            List<IntegrationLedgerRecord> items = repository.listLedger(ledgerType);
            auditEventService.recordSuccess(
                    successEvent,
                    "integration.provider_adapter.view",
                    "integration_read_ledger",
                    ledgerType,
                    requestId(),
                    null,
                    Map.of("count", items.size(), "ledger_type", ledgerType)
            );
            return new PageResponse<>(items, 1, 50, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    "integration.provider_adapter.view",
                    "integration_read_ledger",
                    ledgerType,
                    requestId(),
                    null,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    public PageResponse<IntegrationOrderSyncRecord> listOrderSyncs() {
        return readPage("integration.order_sync.viewed", "integration.order_sync.view_failed",
                "integration.order_sync.view", "integration_order_sync", repository::listOrderSyncs);
    }

    public IntegrationOrderSyncRecord createOrderSync(IntegrationOrderSyncRequest request) {
        return executeWrite(
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

    public PageResponse<IntegrationTaskRecord> listRetryTasks() {
        return readPage("integration.retry_task.viewed", "integration.retry_task.view_failed",
                "integration.retry.view", "integration_retry_task", repository::listRetryTasks);
    }

    public IntegrationTaskRecord createRetryTask(IntegrationTaskRequest request) {
        return executeWrite(
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

    public IntegrationTaskRecord runRetryTaskNow(String id, StatusChangeRequest request) {
        String taskId = requireTextValue(id, "id");
        return executeWrite(
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

    public PageResponse<IntegrationTaskRecord> listCompensationTasks() {
        return readPage("integration.compensation_task.viewed", "integration.compensation_task.view_failed",
                "integration.compensation.view", "integration_compensation_task", repository::listCompensationTasks);
    }

    public IntegrationTaskRecord createCompensationTask(IntegrationTaskRequest request) {
        return executeWrite(
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

    public IntegrationTaskRecord resolveCompensationTask(String id, IntegrationTaskResolveRequest request) {
        String taskId = requireTextValue(id, "id");
        return executeWrite(
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

    public PageResponse<ReconciliationBatchRecord> listReconciliationBatches() {
        return readPage("integration.reconciliation_batch.viewed", "integration.reconciliation_batch.view_failed",
                "integration.reconciliation.view", "integration_reconciliation_batch", repository::listReconciliationBatches);
    }

    public ReconciliationBatchRecord createReconciliationBatch(ReconciliationBatchRequest request) {
        return executeWrite(
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

    public PageResponse<ReconciliationDifferenceRecord> listReconciliationDifferences() {
        return readPage("integration.reconciliation_difference.viewed", "integration.reconciliation_difference.view_failed",
                "integration.reconciliation.view", "integration_reconciliation_difference", repository::listReconciliationDifferences);
    }

    public ReconciliationDifferenceRecord createReconciliationDifference(ReconciliationDifferenceRequest request) {
        return executeWrite(
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

    public ReconciliationDifferenceRecord resolveReconciliationDifference(
            String id,
            ReconciliationDifferenceResolveRequest request
    ) {
        String differenceId = requireTextValue(id, "id");
        return executeWrite(
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

    public PageResponse<ProviderAdapterRecord> listProviderAdapters() {
        try {
            List<ProviderAdapterRecord> items = repository.listProviderAdapters();
            auditEventService.recordSuccess(
                    "provider_adapter.viewed",
                    "integration.provider_adapter.view",
                    "integration_provider_adapter",
                    "all",
                    requestId(),
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, 1, 50, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "provider_adapter.view_failed",
                    "integration.provider_adapter.view",
                    "integration_provider_adapter",
                    "all",
                    requestId(),
                    null,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    public ProviderAdapterRecord createProviderAdapter(ProviderAdapterRequest request) {
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

    public ProviderAdapterRecord updateProviderAdapter(String id, ProviderAdapterUpdateRequest request) {
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

    public ProviderAdapterRecord markProductionReady(String id, StatusChangeRequest request) {
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

    public AdapterTestRecord testProviderAdapterMock(String id, ProviderAdapterTestRequest request) {
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

    private ReconciliationBatchRecord createReconciliationBatchOnce(ReconciliationBatchRequest request) {
        validateReconciliationBatchRequest(request);
        int differenceCount = nonNegative(request.differenceCount(), "difference_count");
        Instant now = clock.instant();
        return repository.saveReconciliationBatch(new ReconciliationBatchRecord(
                "reconciliation_batch_" + UUID.randomUUID(),
                request.providerCode(),
                request.businessType(),
                request.periodStart(),
                request.periodEnd(),
                differenceCount > 0 ? "difference_found" : "matched",
                nonNegative(request.expectedCount(), "expected_count"),
                nonNegative(request.actualCount(), "actual_count"),
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
        Instant now = clock.instant();
        return repository.saveReconciliationDifference(new ReconciliationDifferenceRecord(
                "reconciliation_difference_" + UUID.randomUUID(),
                request.batchId(),
                request.providerCode(),
                request.businessType(),
                request.externalBusinessId(),
                requireTextValue(request.differenceType(), "difference_type"),
                nonNegative(request.internalAmountCent(), "internal_amount_cent"),
                nonNegative(request.providerAmountCent(), "provider_amount_cent"),
                nonNegative(request.differenceAmountCent(), "difference_amount_cent"),
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

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        try {
            List<T> items = action.get();
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    requestId(),
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, 1, 50, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "all",
                    requestId(),
                    null,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T saved = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(saved),
                                requestId(),
                                idempotencyKey,
                                Map.of("operation_id", operationId)
                        );
                        return saved;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "unknown",
                    requestId(),
                    idempotencyKey,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private OpenAppCredentialRecord verifyOpenSignature(OpenSignatureRequest request, String endpointKey, boolean reserveNonce) {
        validateOpenSignatureRequest(request);
        OpenAppCredentialRecord credential = repository.findOpenAppCredential(request.appKey())
                .filter(found -> "active".equals(found.status()))
                .orElseThrow(() -> ApiException.unauthorized("OPEN_APP_DISABLED", "open app is disabled"));
        if (endpointKey != null && !credential.allowedEndpointKeys().contains(endpointKey)) {
            throw ApiException.forbidden("OPEN_ENDPOINT_FORBIDDEN", "open app cannot access endpoint");
        }
        rejectStaleTimestamp(request.timestamp());
        verifySignature(
                request.signature(),
                openAuthCanonical(request.appKey(), request.timestamp(), request.nonce()),
                resolveSecret(credential.secretRef()),
                "OPEN_SIGNATURE_INVALID"
        );
        if (reserveNonce && !repository.reserveOpenNonce(request.appKey(), request.nonce(), request.timestamp())) {
            throw ApiException.unauthorized("OPEN_NONCE_REPLAYED", "open nonce was already used");
        }
        return credential;
    }

    private void validateOpenSignatureRequest(OpenSignatureRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.appKey(), "app_key");
        if (request.timestamp() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "timestamp is required");
        }
        requireText(request.nonce(), "nonce");
        requireText(request.signature(), "signature");
    }

    private void rejectStaleTimestamp(long timestamp) {
        long skew = Math.abs(clock.millis() - timestamp);
        if (skew > OPEN_SIGNATURE_TOLERANCE_MILLIS) {
            throw ApiException.unauthorized("OPEN_TIMESTAMP_STALE", "open timestamp is stale");
        }
    }

    private void verifySignature(String actual, String canonical, String key, String errorCode) {
        requireText(actual, "signature");
        String expected = hmacHex(key, canonical);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.unauthorized(errorCode, "signature is invalid");
        }
    }

    private String hmacHex(String key, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    private String openAuthCanonical(String appKey, long timestamp, String nonce) {
        return "app_key=" + appKey + "\nnonce=" + nonce + "\ntimestamp=" + timestamp;
    }

    private String webhookCanonical(String endpointKey, String provider, String event, String idempotencyKey, String payloadHash) {
        return "endpoint_key=" + endpointKey + "\nevent=" + event + "\nidempotency_key=" + idempotencyKey + "\npayload_hash=" + payloadHash + "\nprovider=" + provider;
    }

    private String payloadHash(Map<String, Object> payload) {
        try {
            return sha256(signatureObjectMapper.writeValueAsString(payload == null ? Map.of() : payload));
        } catch (Exception exception) {
            throw ApiException.badRequest("OPEN_WEBHOOK_PAYLOAD_INVALID", "open webhook payload is not serializable");
        }
    }

    private String resolveSecret(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            throw ApiException.unauthorized("OPEN_SECRET_UNAVAILABLE", "open platform secret is unavailable");
        }
        Optional<String> resolved;
        try {
            resolved = secretResolver.resolve(secretRef);
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApiException.unauthorized("OPEN_SECRET_UNAVAILABLE", "open platform secret is unavailable");
        }
        return resolved
                .filter(secret -> !secret.isBlank())
                .orElseThrow(() -> ApiException.unauthorized("OPEN_SECRET_UNAVAILABLE", "open platform secret is unavailable"));
    }

    private void validateCreateRequest(ProviderAdapterRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.providerCode(), "provider_code");
        requireEnum(ADAPTER_TYPES, request.adapterType(), "adapter_type");
        requireEnum(MODES, request.mode(), "mode");
    }

    private void validateUpdateRequest(ProviderAdapterUpdateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.providerCode(), "provider_code");
        if (request.capabilities() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "capabilities is required");
        }
        requireEnum(MODES, request.environment(), "environment");
        requireEnum(STATUSES, request.status(), "status");
    }

    private void validateStatusChange(StatusChangeRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.reason(), "reason");
    }

    private void validateTestRequest(ProviderAdapterTestRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.providerCode(), "provider_code");
        requireEnum(TEST_SCENARIOS, request.scenario(), "scenario");
    }

    private void validateOrderSyncRequest(IntegrationOrderSyncRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.providerCode(), "provider_code");
        requireEnum(BUSINESS_TYPES, request.businessType(), "business_type");
        requireEnum(SYNC_TYPES, request.syncType(), "sync_type");
        requireText(request.externalBusinessId(), "external_business_id");
        requireEnum(SYNC_STATUSES, request.status(), "status");
        currency(request.currency());
    }

    private void validateTaskRequest(IntegrationTaskRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.providerCode(), "provider_code");
        requireText(request.taskType(), "task_type");
        requireEnum(BUSINESS_TYPES, request.businessType(), "business_type");
        requireText(request.externalBusinessId(), "external_business_id");
        taskStatus(request.status());
    }

    private void validateTaskResolveRequest(IntegrationTaskResolveRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.operatorUserId(), "operator_user_id");
        taskStatus(request.status());
        requireText(request.resolutionNote(), "resolution_note");
    }

    private void validateReconciliationBatchRequest(ReconciliationBatchRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.providerCode(), "provider_code");
        requireEnum(BUSINESS_TYPES, request.businessType(), "business_type");
        if (request.periodStart() == null || request.periodEnd() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period_start and period_end are required");
        }
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period_end must be on or after period_start");
        }
    }

    private void validateReconciliationDifferenceRequest(ReconciliationDifferenceRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.batchId(), "batch_id");
        requireText(request.providerCode(), "provider_code");
        requireEnum(BUSINESS_TYPES, request.businessType(), "business_type");
        requireText(request.externalBusinessId(), "external_business_id");
        requireText(request.differenceType(), "difference_type");
    }

    private void validateReconciliationDifferenceResolveRequest(ReconciliationDifferenceResolveRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.operatorUserId(), "operator_user_id");
        reconciliationStatus(request.status());
        requireText(request.resolutionNote(), "resolution_note");
    }

    private void requireProductionReady(ProviderAdapterRecord adapter) {
        if (!"production".equals(adapter.environment())
                || !adapter.webhookEnabled()
                || !adapter.pollingCheckEnabled()
                || adapter.capabilities().isEmpty()
                || adapter.credentialSecretRef() == null
                || adapter.credentialSecretRef().isBlank()) {
            throw ApiException.conflict("PROVIDER_ADAPTER_NOT_PRODUCTION_READY", "provider adapter is not production ready");
        }
        rejectSecretLikeCredentialRef(adapter.credentialSecretRef());
    }

    private void rejectSecretLikeCredentialRef(String credentialRef) {
        if (credentialRef == null || credentialRef.isBlank()) {
            return;
        }
        for (Pattern pattern : SECRET_PATTERNS) {
            if (pattern.matcher(credentialRef).matches()) {
                throw ApiException.badRequest("SECRET_VALUE_REJECTED", "credential_secret_ref must be a reference, not a secret value");
            }
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
    }

    private String requireTextValue(String value, String fieldName) {
        requireText(value, fieldName);
        return value;
    }

    private void requireEnum(Set<String> values, String value, String fieldName) {
        requireText(value, fieldName);
        if (!values.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
    }

    private int nonNegative(Integer value, String fieldName) {
        int resolved = value == null ? 0 : value;
        if (resolved < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return resolved;
    }

    private long nonNegative(Long value, String fieldName) {
        long resolved = value == null ? 0L : value;
        if (resolved < 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return resolved;
    }

    private String currency(String value) {
        String resolved = value == null || value.isBlank() ? "CNY" : value;
        if (!"CNY".equals(resolved)) {
            throw ApiException.badRequest("UNSUPPORTED_CURRENCY", "only CNY is supported");
        }
        return resolved;
    }

    private String taskStatus(String value) {
        requireEnum(TASK_STATUSES, value, "status");
        return value;
    }

    private String reconciliationStatus(String value) {
        requireEnum(RECONCILIATION_STATUSES, value, "status");
        return value;
    }

    private boolean booleanConfig(Map<String, Object> config, String fieldName) {
        Object value = config == null ? null : config.get(fieldName);
        return value instanceof Boolean enabled && enabled;
    }

    private Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : Map.copyOf(value);
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : List.copyOf(value);
    }

    private String eventId(Map<String, Object> payload) {
        Object eventId = payload == null ? null : payload.get("event_id");
        if (eventId instanceof String value && !value.isBlank()) {
            return value;
        }
        return "evt_" + UUID.randomUUID();
    }

    private String idempotencyKey(String headerValue, Map<String, Object> payload) {
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue;
        }
        Object bodyValue = payload == null ? null : payload.get("idempotency_key");
        if (bodyValue instanceof String value && !value.isBlank()) {
            return value;
        }
        return null;
    }

    private String newRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "p0_open_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }

    private String requestId() {
        return "req_" + UUID.randomUUID();
    }

    private String blankToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private String businessId(Object saved) {
        if (saved instanceof IntegrationOrderSyncRecord record) {
            return record.id();
        }
        if (saved instanceof IntegrationTaskRecord record) {
            return record.id();
        }
        if (saved instanceof ReconciliationBatchRecord record) {
            return record.id();
        }
        if (saved instanceof ReconciliationDifferenceRecord record) {
            return record.id();
        }
        if (saved instanceof ProviderAdapterRecord record) {
            return record.id();
        }
        if (saved instanceof AdapterTestRecord record) {
            return record.id();
        }
        return "unknown";
    }

    private record UpdateProviderAdapterFingerprint(String id, ProviderAdapterUpdateRequest request) {
    }

    private record StatusChangeFingerprint(String id, StatusChangeRequest request) {
    }

    private record ProviderAdapterTestFingerprint(String id, ProviderAdapterTestRequest request) {
    }

    private record ResolveTaskFingerprint(String id, IntegrationTaskResolveRequest request) {
    }

    private record ResolveDifferenceFingerprint(String id, ReconciliationDifferenceResolveRequest request) {
    }
}
