package com.complex.platform.integration;

import static com.complex.platform.integration.IntegrationValidation.*;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationService {

    private static final long OPEN_SIGNATURE_TOLERANCE_MILLIS = 300_000;
    private static final String OPEN_PERMISSION = "not_applicable";
    private final IntegrationRepository repository;
    private final AuditEventService auditEventService;
    private final SecretResolver secretResolver;
    private final Clock clock;
    private final OpenPlatformCrypto openPlatformCrypto = new OpenPlatformCrypto();
    private final AuditedOperationExecutor auditedExecutor;
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
            com.complex.platform.gate.TerminalSwitchGuard terminalSwitchGuard,
            ChannelTicketIssuePort channelTicketIssuePort
    ) {
        this(repository, idempotencyService, auditEventService, Clock.systemUTC(), secretResolver,
                terminalSwitchGuard, channelTicketIssuePort);
    }

    // 兼容构造（既有单测/无出票端口场景）：ticket 回流 fail-closed 拒收，其余能力不变。
    IntegrationService(
            IntegrationRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            Clock clock,
            SecretResolver secretResolver,
            com.complex.platform.gate.TerminalSwitchGuard terminalSwitchGuard
    ) {
        this(repository, idempotencyService, auditEventService, clock, secretResolver, terminalSwitchGuard, null);
    }

    IntegrationService(
            IntegrationRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            Clock clock,
            SecretResolver secretResolver,
            com.complex.platform.gate.TerminalSwitchGuard terminalSwitchGuard,
            ChannelTicketIssuePort channelTicketIssuePort
    ) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.clock = clock;
        this.secretResolver = secretResolver;
        this.auditedExecutor = new AuditedOperationExecutor(idempotencyService, auditEventService);
        this.providerAdapterService = new ProviderAdapterService(
                repository, clock, auditedExecutor, idempotencyService, auditEventService);
        this.orderSyncService = new IntegrationOrderSyncService(repository, clock, auditedExecutor,
                idempotencyService, auditEventService, terminalSwitchGuard, channelTicketIssuePort);
        this.reconciliationService = new IntegrationReconciliationService(repository, clock, auditedExecutor);
        this.thirdPartyService = new ThirdPartyScaffoldingService(
                repository, clock, auditEventService, terminalSwitchGuard, openPlatformCrypto, secretResolver);
    }


    public OpenEventResultResponse getOpenEventResult(String endpointKey, String eventId, OpenSignatureRequest request) {
        try {
            OpenAppCredentialRecord credential = verifyOpenSignature(request, endpointKey, true);
            OpenApiEventRecord event = repository.findOpenEventById(endpointKey, eventId)
                    .orElseThrow(() -> ApiException.notFound("OPEN_EVENT_NOT_FOUND", "开放平台事件不存在或已被删除"));
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
            Long timestamp,
            String nonce,
            String idempotencyKey,
            Map<String, Object> payload
    ) {
        try {
            String resolvedIdempotencyKey = idempotencyKey(idempotencyKey, payload);
            requireText(endpointKey, "endpoint_key");
            requireText(provider, "provider");
            requireText(event, "event");
            requireText(resolvedIdempotencyKey, "idempotency_key");
            // 防重放：要求新鲜 timestamp + 唯一 nonce，二者参与签名（与 open auth 路径一致）。
            requireText(nonce, "nonce");
            String resolvedNonce = nonce.trim();
            long resolvedTimestamp = timestamp == null ? 0L : timestamp;
            rejectStaleTimestamp(resolvedTimestamp);
            OpenEndpointConfigRecord endpoint = repository.findEndpointConfig(endpointKey)
                    .filter(config -> "active".equals(config.status()))
                    .orElseThrow(() -> ApiException.unauthorized("OPEN_ENDPOINT_DISABLED", "该开放平台接口已停用"));
            if (!provider.equals(endpoint.providerCode()) || !endpoint.webhookEnabled()) {
                throw ApiException.unauthorized("OPEN_WEBHOOK_REJECTED", "开放平台回调接口未启用");
            }
            String payloadHash = openPlatformCrypto.payloadHash(payload);
            openPlatformCrypto.verifySignature(signature, openPlatformCrypto.webhookCanonical(
                    endpointKey, provider, event, resolvedIdempotencyKey, payloadHash, resolvedTimestamp, resolvedNonce),
                    resolveSecret(endpoint.secretRef()), "OPEN_WEBHOOK_SIGNATURE_INVALID");
            // 签名通过后预留 nonce（以 endpoint_key 为命名空间）：同一传输（同 nonce）再发即拒，堵原报文重放。
            // 合法重试应换新 nonce + 复用 idempotency_key，下方幂等去重返回原结果。
            if (!repository.reserveOpenNonce(endpointKey, resolvedNonce, resolvedTimestamp)) {
                throw ApiException.unauthorized("OPEN_WEBHOOK_NONCE_REPLAYED", "开放平台回调随机数已被使用");
            }

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

    public IntegrationPollingCheckRecord recordPollingCheck(IntegrationPollingCheckRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.operatorNote(), "operator_note");
        List<String> checkedEndpoints = safeList(request.checkedEndpoints()).stream()
                .filter(endpoint -> endpoint != null && !endpoint.isBlank())
                .distinct()
                .toList();
        if (checkedEndpoints.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "checked_endpoints is required");
        }
        int pendingCount = nonNegative(request.pendingCount(), "pending_count");
        int sourceCount = nonNegative(request.sourceCount(), "source_count");
        if (sourceCount < checkedEndpoints.size()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "数据源数量必须覆盖所有已检查的接口");
        }
        List<String> evidenceAttachmentIds = safeList(request.evidenceAttachmentIds());
        String operationId = "recordPollingCheck:" + String.join(",", checkedEndpoints);
        return auditedExecutor.executeWrite(
                operationId,
                request.idempotencyKey(),
                request,
                IntegrationPollingCheckRecord.class,
                AuditedOperation.of(
                        "integration.polling_check.recorded",
                        "integration.polling_check.record_failed",
                        "integration.retry.manage",
                        "integration_polling_check"),
                () -> new IntegrationPollingCheckRecord(
                        "polling_check_" + UUID.randomUUID().toString().replace("-", ""),
                        "recorded",
                        request.operatorNote().trim(),
                        checkedEndpoints,
                        pendingCount,
                        sourceCount,
                        evidenceAttachmentIds,
                        clock.instant(),
                        "platform_task_status_only_not_supplier_execution"
                ),
                saved -> ((IntegrationPollingCheckRecord) saved).id(),
                Map.of("operation_id", operationId)
        );
    }

    public PageResponse<IntegrationOrderSyncRecord> listOrderSyncs() {
        return orderSyncService.listOrderSyncs();
    }

    // 批6 事务边界：ticket 回流分支内含本地下单/支付/出票多写路径，与回流台账落库同一事务——
    // 出票链任一步失败整体回滚，不留「出了票没台账 / 有台账没票」的半截状态。
    @Transactional
    public IntegrationOrderSyncRecord createOrderSync(IntegrationOrderSyncRequest request) {
        return orderSyncService.createOrderSync(request);
    }

    public PageResponse<IntegrationChannelTicketTypeMapRecord> listChannelTicketTypeMaps() {
        return orderSyncService.listChannelTicketTypeMaps();
    }

    public IntegrationChannelTicketTypeMapRecord saveChannelTicketTypeMap(IntegrationChannelTicketTypeMapRequest request) {
        return orderSyncService.saveChannelTicketTypeMap(request);
    }

    public IntegrationChannelTicketTypeMapRecord archiveChannelTicketTypeMap(String id, StatusChangeRequest request) {
        return orderSyncService.archiveChannelTicketTypeMap(id, request);
    }

    public PageResponse<IntegrationOrderSyncRecord> listUnattributedOrderSyncs() {
        return orderSyncService.listUnattributedOrderSyncs();
    }

    public PageResponse<IntegrationChannelStoreMapRecord> listChannelStoreMaps() {
        return orderSyncService.listChannelStoreMaps();
    }

    public IntegrationChannelStoreMapRecord saveChannelStoreMap(IntegrationChannelStoreMapRequest request) {
        return orderSyncService.saveChannelStoreMap(request);
    }

    public IntegrationChannelStoreMapRecord archiveChannelStoreMap(String id, StatusChangeRequest request) {
        return orderSyncService.archiveChannelStoreMap(id, request);
    }

    /** 自动对账（P0#11）：从 order_syncs 标准模型自动比对生成对账差异（开关 off → dryRun，不伪造）。 */
    public ChannelReconciliationResult reconcileChannel(ChannelReconciliationRequest request) {
        return orderSyncService.reconcileChannel(request);
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

    /**
     * 登记对账差异（P0-E：金额一律系统取数，请求体禁手填）。
     * actorUserId 由 controller 从会话取得，落 registered_by_user_id 供复核差人守卫。
     */
    public ReconciliationDifferenceRecord createReconciliationDifference(
            ReconciliationDifferenceRequest request, String actorUserId) {
        return reconciliationService.createReconciliationDifference(request, actorUserId);
    }

    /** 复核（处置）对账差异（P0-E：登记人不得自审）。 */
    public ReconciliationDifferenceRecord resolveReconciliationDifference(
            String id,
            ReconciliationDifferenceResolveRequest request,
            String actorUserId
    ) {
        return reconciliationService.resolveReconciliationDifference(id, request, actorUserId);
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
    // B5 事务边界：信封落库 + 审计留痕为多写路径，在 Spring bean 入口包一个事务（内部骨架 new 出非 bean，注解须在此）。
    @Transactional
    public WebhookAcceptedResponse receiveTypedWebhook(String type, String provider, String signature,
            Long timestamp, String nonce, String idempotencyKey, Map<String, Object> payload) {
        return thirdPartyService.receiveTypedWebhook(type, provider, signature, timestamp, nonce, idempotencyKey, payload);
    }

    /** 机器人被动留痕：写 integration_robot_task_events，受 integration.robot.sync 开关门控。 */
    @Transactional
    public RobotTaskEventRecord reportRobotTaskEvent(RobotTaskEventRequest request, String provider,
            String signature, Long timestamp, String nonce) {
        return thirdPartyService.reportRobotTaskEvent(request, provider, signature, timestamp, nonce);
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
                .orElseThrow(() -> ApiException.unauthorized("OPEN_APP_DISABLED", "该开放平台应用已停用"));
        if (endpointKey != null && !credential.allowedEndpointKeys().contains(endpointKey)) {
            throw ApiException.forbidden("OPEN_ENDPOINT_FORBIDDEN", "该开放平台应用无权访问此接口");
        }
        rejectStaleTimestamp(request.timestamp());
        openPlatformCrypto.verifySignature(
                request.signature(),
                openPlatformCrypto.openAuthCanonical(request.appKey(), request.timestamp(), request.nonce()),
                resolveSecret(credential.secretRef()),
                "OPEN_SIGNATURE_INVALID"
        );
        if (reserveNonce && !repository.reserveOpenNonce(request.appKey(), request.nonce(), request.timestamp())) {
            throw ApiException.unauthorized("OPEN_NONCE_REPLAYED", "开放平台请求随机数已被使用");
        }
        return credential;
    }

    private void rejectStaleTimestamp(long timestamp) {
        long skew = Math.abs(clock.millis() - timestamp);
        if (skew > OPEN_SIGNATURE_TOLERANCE_MILLIS) {
            throw ApiException.unauthorized("OPEN_TIMESTAMP_STALE", "开放平台请求时间戳已过期");
        }
    }

    private String resolveSecret(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            throw ApiException.unauthorized("OPEN_SECRET_UNAVAILABLE", "开放平台密钥不可用，请联系管理员");
        }
        Optional<String> resolved;
        try {
            resolved = secretResolver.resolve(secretRef);
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApiException.unauthorized("OPEN_SECRET_UNAVAILABLE", "开放平台密钥不可用，请联系管理员");
        }
        return resolved
                .filter(secret -> !secret.isBlank())
                .orElseThrow(() -> ApiException.unauthorized("OPEN_SECRET_UNAVAILABLE", "开放平台密钥不可用，请联系管理员"));
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
