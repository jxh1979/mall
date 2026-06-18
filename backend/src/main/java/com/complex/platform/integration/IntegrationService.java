package com.complex.platform.integration;

import static com.complex.platform.integration.IntegrationValidation.*;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IntegrationService {

    private static final long OPEN_SIGNATURE_TOLERANCE_MILLIS = 300_000;
    private static final long OPEN_TOKEN_TTL_SECONDS = 3_600;
    private static final String OPEN_PERMISSION = "not_applicable";
    private final IntegrationRepository repository;
    private final AuditEventService auditEventService;
    private final SecretResolver secretResolver;
    private final Clock clock;
    private final OpenPlatformCrypto openPlatformCrypto = new OpenPlatformCrypto();
    private final IntegrationOperationSupport operationSupport;
    private final ProviderAdapterService providerAdapterService;
    private final IntegrationOrderSyncService orderSyncService;
    private final IntegrationReconciliationService reconciliationService;

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
        this.auditEventService = auditEventService;
        this.clock = clock;
        this.secretResolver = secretResolver;
        this.operationSupport = new IntegrationOperationSupport(idempotencyService, auditEventService);
        this.providerAdapterService = new ProviderAdapterService(repository, clock, operationSupport);
        this.orderSyncService = new IntegrationOrderSyncService(repository, clock, operationSupport);
        this.reconciliationService = new IntegrationReconciliationService(repository, clock, operationSupport);
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
            String rawToken = openPlatformCrypto.newRawToken();
            repository.saveOpenToken(new OpenAppTokenRecord(
                    UUID.randomUUID().toString(),
                    credential.appKey(),
                    openPlatformCrypto.sha256(rawToken),
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
            String payloadHash = openPlatformCrypto.payloadHash(payload);
            openPlatformCrypto.verifySignature(signature, openPlatformCrypto.webhookCanonical(endpointKey, provider, event, resolvedIdempotencyKey, payloadHash), resolveSecret(endpoint.secretRef()), "OPEN_WEBHOOK_SIGNATURE_INVALID");

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
        return orderSyncService.listOrderSyncs();
    }

    public IntegrationOrderSyncRecord createOrderSync(IntegrationOrderSyncRequest request) {
        return orderSyncService.createOrderSync(request);
    }

    public PageResponse<IntegrationTaskRecord> listRetryTasks() {
        return orderSyncService.listRetryTasks();
    }

    public IntegrationTaskRecord createRetryTask(IntegrationTaskRequest request) {
        return orderSyncService.createRetryTask(request);
    }

    public IntegrationTaskRecord runRetryTaskNow(String id, StatusChangeRequest request) {
        return orderSyncService.runRetryTaskNow(id, request);
    }

    public PageResponse<IntegrationTaskRecord> listCompensationTasks() {
        return orderSyncService.listCompensationTasks();
    }

    public IntegrationTaskRecord createCompensationTask(IntegrationTaskRequest request) {
        return orderSyncService.createCompensationTask(request);
    }

    public IntegrationTaskRecord resolveCompensationTask(String id, IntegrationTaskResolveRequest request) {
        return orderSyncService.resolveCompensationTask(id, request);
    }

    public PageResponse<ReconciliationBatchRecord> listReconciliationBatches() {
        return reconciliationService.listReconciliationBatches();
    }

    public ReconciliationBatchRecord createReconciliationBatch(ReconciliationBatchRequest request) {
        return reconciliationService.createReconciliationBatch(request);
    }

    public PageResponse<ReconciliationDifferenceRecord> listReconciliationDifferences() {
        return reconciliationService.listReconciliationDifferences();
    }

    public ReconciliationDifferenceRecord createReconciliationDifference(ReconciliationDifferenceRequest request) {
        return reconciliationService.createReconciliationDifference(request);
    }

    public ReconciliationDifferenceRecord resolveReconciliationDifference(
            String id,
            ReconciliationDifferenceResolveRequest request
    ) {
        return reconciliationService.resolveReconciliationDifference(id, request);
    }

    public PageResponse<ProviderAdapterRecord> listProviderAdapters() {
        return providerAdapterService.listProviderAdapters();
    }

    public ProviderAdapterRecord createProviderAdapter(ProviderAdapterRequest request) {
        return providerAdapterService.createProviderAdapter(request);
    }

    public ProviderAdapterRecord updateProviderAdapter(String id, ProviderAdapterUpdateRequest request) {
        return providerAdapterService.updateProviderAdapter(id, request);
    }

    public ProviderAdapterRecord markProductionReady(String id, StatusChangeRequest request) {
        return providerAdapterService.markProductionReady(id, request);
    }

    public AdapterTestRecord testProviderAdapterMock(String id, ProviderAdapterTestRequest request) {
        return providerAdapterService.testProviderAdapterMock(id, request);
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
        openPlatformCrypto.verifySignature(
                request.signature(),
                openPlatformCrypto.openAuthCanonical(request.appKey(), request.timestamp(), request.nonce()),
                resolveSecret(credential.secretRef()),
                "OPEN_SIGNATURE_INVALID"
        );
        if (reserveNonce && !repository.reserveOpenNonce(request.appKey(), request.nonce(), request.timestamp())) {
            throw ApiException.unauthorized("OPEN_NONCE_REPLAYED", "open nonce was already used");
        }
        return credential;
    }

    private void rejectStaleTimestamp(long timestamp) {
        long skew = Math.abs(clock.millis() - timestamp);
        if (skew > OPEN_SIGNATURE_TOLERANCE_MILLIS) {
            throw ApiException.unauthorized("OPEN_TIMESTAMP_STALE", "open timestamp is stale");
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

}
