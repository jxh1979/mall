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
    private final ThirdPartyScaffoldingService thirdPartyService;

    @Autowired
    public IntegrationService(
            IntegrationRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            SecretResolver secretResolver,
            com.complex.platform.gate.TerminalSwitchGuard terminalSwitchGuard
    ) {
        this(repository, idempotencyService, auditEventService, Clock.systemUTC(), secretResolver, terminalSwitchGuard);
    }

    IntegrationService(
            IntegrationRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            Clock clock,
            SecretResolver secretResolver,
            com.complex.platform.gate.TerminalSwitchGuard terminalSwitchGuard
    ) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.clock = clock;
        this.secretResolver = secretResolver;
        this.operationSupport = new IntegrationOperationSupport(idempotencyService, auditEventService);
        this.providerAdapterService = new ProviderAdapterService(repository, clock, operationSupport);
        this.orderSyncService = new IntegrationOrderSyncService(repository, clock, operationSupport);
        this.reconciliationService = new IntegrationReconciliationService(repository, clock, operationSupport);
        this.thirdPartyService = new ThirdPartyScaffoldingService(
                repository, clock, auditEventService, terminalSwitchGuard);
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
                        Map<String, Object> safePayload = payload == null ? Map.of() : Map.copyOf(payload);
                        WebhookMapping mappingResult = mapPayload(provider, endpoint, safePayload);
                        Map<String, Object> resultJson = new java.util.LinkedHashMap<>();
                        resultJson.put("accepted", true);
                        resultJson.put("payload_hash", payloadHash);
                        resultJson.put("process_status", mappingResult.processStatus());
                        resultJson.put("mapped", "mapped".equals(mappingResult.processStatus()));
                        if (!mappingResult.mapping().isEmpty()) {
                            resultJson.put("mapping", mappingResult.mapping());
                        }
                        repository.saveOpenEvent(new OpenApiEventRecord(
                                eventId,
                                endpointKey,
                                provider,
                                event,
                                resolvedIdempotencyKey,
                                true,
                                mappingResult.processStatus(),
                                safePayload,
                                Map.copyOf(resultJson),
                                mappingResult.failureReason(),
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

    /**
     * webhook 入站字段映射的最小落地（受裁决约束）。
     *
     * <p>映射配置来源：本期代码未实现 {@code integration_endpoint_mappings} 表，
     * 复用既有 {@link ProviderAdapterRecord#fieldMapping()} 作为 request_mapping（源字段→标准字段），
     * 其内可选子键 {@code status_mapping}（供应商状态码→平台标准状态）；
     * 开关复用适配器既有 {@link ProviderAdapterRecord#webhookEnabled()}。
     *
     * <p>判定：
     * <ul>
     *   <li>无匹配适配器 / 开关关闭 / 无 request_mapping 配置 → {@code accepted}（维持现状，仅留信封）；</li>
     *   <li>映射成功 → {@code mapped}，产出标准事件投影；</li>
     *   <li>映射异常 → {@code map_failed}，落错误占位，<b>不抛异常中断</b> webhook 接收。</li>
     * </ul>
     */
    private WebhookMapping mapPayload(String provider, OpenEndpointConfigRecord endpoint, Map<String, Object> payload) {
        Optional<ProviderAdapterRecord> adapterOpt = repository.listProviderAdapters().stream()
                .filter(adapter -> provider.equals(adapter.providerCode()))
                .filter(adapter -> !"disabled".equals(adapter.status()))
                .findFirst();
        if (adapterOpt.isEmpty()) {
            return WebhookMapping.accepted();
        }
        ProviderAdapterRecord adapter = adapterOpt.get();
        // 开关兜底：端点或适配器任一未开启 webhook 即维持 accepted（裁决"开关关只留信封"）。
        if (!endpoint.webhookEnabled() || !adapter.webhookEnabled()) {
            return WebhookMapping.accepted();
        }
        Map<String, Object> fieldMapping = adapter.fieldMapping();
        if (fieldMapping == null || fieldMapping.isEmpty()) {
            return WebhookMapping.accepted();
        }
        try {
            Map<String, Object> projection = new java.util.LinkedHashMap<>();
            Object statusMappingRaw = fieldMapping.get("status_mapping");
            for (Map.Entry<String, Object> entry : fieldMapping.entrySet()) {
                String standardField = entry.getKey();
                if ("status_mapping".equals(standardField)) {
                    continue;
                }
                Object sourcePathValue = entry.getValue();
                if (!(sourcePathValue instanceof String sourcePath)) {
                    throw new IllegalArgumentException("invalid request_mapping entry for " + standardField);
                }
                Object mapped = resolvePath(payload, sourcePath);
                if (mapped != null) {
                    projection.put(standardField, mapped);
                }
            }
            // status_mapping：将投影中标准 status 字段的供应商值翻译为平台标准状态。
            if (statusMappingRaw instanceof Map<?, ?> statusMapping && projection.get("status") instanceof String providerStatus) {
                Object platformStatus = statusMapping.get(providerStatus);
                if (platformStatus != null) {
                    projection.put("status", platformStatus);
                }
            }
            return WebhookMapping.mapped(projection);
        } catch (RuntimeException exception) {
            return WebhookMapping.mapFailed(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Object resolvePath(Map<String, Object> payload, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Object current = payload;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private record WebhookMapping(String processStatus, Map<String, Object> mapping, String failureReason) {
        static WebhookMapping accepted() {
            return new WebhookMapping("accepted", Map.of(), null);
        }

        static WebhookMapping mapped(Map<String, Object> projection) {
            return new WebhookMapping("mapped", Map.copyOf(projection), null);
        }

        static WebhookMapping mapFailed(String reason) {
            return new WebhookMapping("map_failed", Map.of("reason_code", "WEBHOOK_MAP_FAILURE", "error", reason), reason);
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

    // ── R5 T14 第三方接口/开关预留骨架 ───────────────────────────────────────────

    /**
     * 各类第三方统一 webhook 入口。capability 随 {type} 动态拼接为 integration.{type}.sync，
     * 在 handler 内编程式调用 TerminalSwitchGuard.check（@GuardedAction 静态值不适用）。
     * 开关关 → 抛 TERMINAL_DISABLED；通过则落 open_api_events 信封（mapping 占位空，
     * processStatus=accepted，不伪造对账）。
     */
    public WebhookAcceptedResponse receiveTypedWebhook(String type, String idempotencyKey, Map<String, Object> payload) {
        return thirdPartyService.receiveTypedWebhook(type, idempotencyKey, payload);
    }

    /** 机器人被动留痕：写 integration_robot_task_events，受 integration.robot.sync 开关门控。 */
    public RobotTaskEventRecord reportRobotTaskEvent(RobotTaskEventRequest request) {
        return thirdPartyService.reportRobotTaskEvent(request);
    }

    /** 视频锚点内部记录：业务侧关键动作（核销/交接/异常）调用，clip_index_ref 留空待厂商。 */
    public VideoAnchorRecord recordVideoAnchor(String pointId, Instant eventTime, String businessRef) {
        return thirdPartyService.recordVideoAnchor(pointId, eventTime, businessRef);
    }

    /** 视频锚点调阅：敏感操作，需 integration.video.view 权限 + 写 integration_video_access_audits。 */
    public PageResponse<VideoAnchorRecord> listVideoAnchors(String viewerUserId, String approvalRef) {
        return thirdPartyService.listVideoAnchors(viewerUserId, approvalRef);
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
