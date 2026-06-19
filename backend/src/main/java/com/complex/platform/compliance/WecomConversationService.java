package com.complex.platform.compliance;

import com.complex.platform.common.PiiTextRedactor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.compliance.dto.WecomConversationSyncRequest;
import com.complex.platform.compliance.dto.WecomRetentionPolicyRequest;
import com.complex.platform.compliance.qc.WecomQcPort;
import com.complex.platform.compliance.qc.WecomQcRequest;
import com.complex.platform.compliance.qc.WecomQcResult;
import com.complex.platform.compliance.qc.WecomQcRouter;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.complex.platform.gate.TerminalSwitchGuard;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * R5 T8 企微会话合规链（PIPL 平台侧合规能力）。
 *
 * <ul>
 *   <li><b>同步入站</b>：受 T2 终端能力开关 {@code integration.wecom.sync} 编程式门控（默认关→拒绝、不落库）；
 *       开关关时不阻塞其它功能。落库时调 {@link PiiTextRedactor} 生成展示用脱敏文本，原文落 content_raw_ref（引用/密文）。
 *   <li><b>分级查看</b>：普通展示返回脱敏文本并写 redacted 审计；查看原文需 wecom.view_raw + 审批引用，
 *       返回原文并写 raw 审计 + 事件 wecom.raw_viewed / sensitive.viewed。
 *   <li><b>留存到期</b>：{@link #applyRetention(LocalDate)} 可单测直调；按策略 expiry_action 执行
 *       archive/delete/anonymize；@Scheduled 仅薄包装（默认不注册）。
 *   <li><b>质检</b>：基础关键词敏感命中（私单/私收款/辱骂/诱导私微等）标记 risk_flags 进复核。
 *       AI 语义质检（V096）按"第三方一律预留接口 + 开关默认关"经 {@link WecomQcPort} 路由：
 *       开关 {@code wecom.ai_qc.enabled} 关（默认）→ 不调 AI、仅关键词质检（行为不变）；
 *       开 → 调 {@link WecomQcPort}（当前无真实厂商=占位 NOT_CONFIGURED，不崩、不阻塞），
 *       AI 结果叠加落 ai_qc_status/ai_qc_result，关键词 risk_flags 始终独立保留。
 * </ul>
 */
@Service
public class WecomConversationService {

    static final String SYNC_CAPABILITY = "integration.wecom.sync";
    static final int DEFAULT_RETENTION_DAYS = 180;
    private static final int MAX_REDACTED_CHARS = 4000;

    private static final Set<String> SCOPE_TYPES = Set.of("global", "project", "merchant");
    private static final Set<String> EXPIRY_ACTIONS = Set.of("archive", "delete", "anonymize");

    // 基础质检关键词 → risk_flags 规则码（P0 关键词命中；AI 语义质检 P2）。
    private static final Map<String, List<String>> RISK_KEYWORDS = Map.of(
            "private_deal", List.of("私单", "走私单", "线下交易"),
            "private_payment", List.of("私收款", "私人收款", "转我", "扫我码", "私户"),
            "lure_private_contact", List.of("加我微信", "私微", "私聊", "扫码加我"),
            "abuse", List.of("辱骂", "傻", "滚")
    );

    private final WecomComplianceRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final TerminalSwitchGuard terminalSwitchGuard;
    private final WecomQcPort wecomQcPort;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public WecomConversationService(
            WecomComplianceRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            TerminalSwitchGuard terminalSwitchGuard,
            WecomQcPort wecomQcPort,
            ObjectMapper objectMapper
    ) {
        this(repository, idempotencyService, auditEventService, terminalSwitchGuard,
                wecomQcPort, objectMapper, Clock.systemUTC());
    }

    WecomConversationService(
            WecomComplianceRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            TerminalSwitchGuard terminalSwitchGuard,
            WecomQcPort wecomQcPort,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.terminalSwitchGuard = terminalSwitchGuard;
        // AI 质检端口：默认 fail-safe 当作关（仅关键词质检）；可为 null（向后兼容/单测）。
        this.wecomQcPort = wecomQcPort == null ? WecomQcRouter.alwaysDisabled() : wecomQcPort;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper.copy().findAndRegisterModules();
        this.clock = clock;
    }

    // ----------------------------------------------------------------------------------------- 同步入站

    /** 企微会话同步入站：开关门控 + 脱敏落库 + 基础质检。 */
    public WecomSyncResult syncConversation(WecomConversationSyncRequest request) {
        try {
            // 编程式门控：开关关 → TerminalSwitchGuard.check 抛 TERMINAL_DISABLED，不落库（仍写失败留痕）。
            terminalSwitchGuard.check(SYNC_CAPABILITY);
            validateSyncRequest(request);
            return idempotencyService.execute(
                    "syncWecomConversation",
                    request.idempotencyKey(),
                    request,
                    () -> syncConversationOnce(request),
                    WecomSyncResult.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "wecom.conversation_sync_failed",
                    "wecom.sync",
                    "wecom_conversation",
                    request == null ? "unknown" : blankToUnknown(request.externalConversationId()),
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private WecomSyncResult syncConversationOnce(WecomConversationSyncRequest request) {
        Instant now = clock.instant();
        String externalId = request.externalConversationId().trim();
        String projectId = request.projectId().trim();

        WecomRetentionPolicyRecord policy = repository.findEffectivePolicy(projectId)
                .orElse(defaultPolicy(now));
        int retentionDays = policy.retentionDays() > 0 ? policy.retentionDays() : DEFAULT_RETENTION_DAYS;

        WecomConversationRecord conversation = repository.findConversationByExternalId(externalId)
                .orElseGet(() -> repository.saveConversation(new WecomConversationRecord(
                        "wcv_" + UUID.randomUUID(),
                        externalId,
                        projectId,
                        trimToNull(request.ownerStaffId()),
                        trimToNull(request.customerRef()),
                        "active",
                        null,
                        now,
                        now
                )));

        int stored = 0;
        int flagged = 0;
        LocalDate latestRetentionUntil = conversation.retentionUntil();
        List<WecomConversationSyncRequest.Message> messages =
                request.messages() == null ? List.of() : request.messages();
        for (WecomConversationSyncRequest.Message message : messages) {
            if (message == null) {
                continue;
            }
            String content = message.content() == null ? "" : message.content();
            // 普通展示脱敏（手机号/身份证/银行卡等正则）。
            String redacted = PiiTextRedactor.redactAndTruncate(content, MAX_REDACTED_CHARS).value();
            // 基础关键词质检（始终执行；行为不变，AI 不影响此结果）。
            List<String> riskFlags = detectRiskFlags(content);
            if (!riskFlags.isEmpty()) {
                flagged++;
            }
            // AI 语义质检（第三方预留接口 + 开关默认关）：经路由按 wecom.ai_qc.enabled 选实现——
            //   关→SKIPPED（不调 AI）；开→占位 NOT_CONFIGURED（不崩、不阻塞）。任何状态都不覆盖关键词结果。
            WecomQcResult aiQc = wecomQcPort.analyzeMessage(
                    new WecomQcRequest(conversation.id(), content, riskFlags));
            Instant sentAt = message.sentAt() == null ? now : message.sentAt();
            LocalDate businessDate = LocalDate.ofInstant(sentAt, ZoneOffset.UTC);
            LocalDate retentionUntil = businessDate.plusDays(retentionDays);
            if (latestRetentionUntil == null || retentionUntil.isAfter(latestRetentionUntil)) {
                latestRetentionUntil = retentionUntil;
            }
            repository.saveMessage(new WecomMessageRecord(
                    "wmsg_" + UUID.randomUUID(),
                    conversation.id(),
                    trimToNull(message.senderRef()),
                    // 原文以引用/密文形式落库（本期占位引用；真实密文待企微 SDK 对接）。
                    "wecom-raw:" + (message.externalMessageId() == null ? UUID.randomUUID().toString()
                            : message.externalMessageId().trim()),
                    redacted,
                    riskFlags,
                    false,
                    sentAt,
                    businessDate,
                    now,
                    aiQc.persistenceStatus(),
                    aiQcResultJson(aiQc)
            ));
            stored++;
        }

        // 按最晚消息 + 策略 retention_days 推算会话留存到期。
        if (latestRetentionUntil != null) {
            repository.saveConversation(new WecomConversationRecord(
                    conversation.id(),
                    conversation.externalConversationId(),
                    conversation.projectId(),
                    conversation.ownerStaffId(),
                    conversation.customerRef(),
                    conversation.archiveStatus(),
                    latestRetentionUntil,
                    conversation.createdAt(),
                    now
            ));
        }

        auditEventService.recordSuccess(
                "wecom.conversation_synced",
                "wecom.sync",
                "wecom_conversation",
                conversation.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("messages_stored", stored, "flagged_for_review", flagged, "retention_days", retentionDays)
        );
        return new WecomSyncResult(conversation.id(), stored, flagged);
    }

    // ----------------------------------------------------------------------------------------- 分级查看

    /**
     * 分级查看会话消息。
     *
     * @param mode redacted（脱敏，wecom.view）或 raw（原文，wecom.view_raw + 审批）。
     * @param viewerUserId 调阅人（必填，写审计）。
     * @param approvalRef raw 模式必填审批引用。
     */
    public PageResponse<WecomMessageView> viewMessages(
            String conversationId,
            String mode,
            String viewerUserId,
            String approvalRef
    ) {
        String resolvedMode = mode == null || mode.isBlank() ? "redacted" : mode.trim().toLowerCase(Locale.ROOT);
        if (!"redacted".equals(resolvedMode) && !"raw".equals(resolvedMode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "mode must be redacted or raw");
        }
        String resolvedConversationId = requireText(conversationId, "conversation_id");
        String resolvedViewer = requireText(viewerUserId, "viewer_user_id");
        boolean raw = "raw".equals(resolvedMode);
        String resolvedApprovalRef = trimToNull(approvalRef);
        if (raw && resolvedApprovalRef == null) {
            // 原文调阅最小授权：必须带审批引用，否则拒绝。
            ApiException denied = ApiException.forbidden(
                    "WECOM_RAW_APPROVAL_REQUIRED", "viewing raw WeCom content requires an approval reference");
            auditEventService.recordFailure(
                    "wecom.raw_view_failed",
                    "wecom.view_raw",
                    "wecom_conversation",
                    resolvedConversationId,
                    requestId(),
                    null,
                    denied.code()
            );
            throw denied;
        }

        WecomConversationRecord conversation = repository.findConversationById(resolvedConversationId)
                .orElseThrow(() -> ApiException.notFound("WECOM_CONVERSATION_NOT_FOUND", "conversation not found"));
        List<WecomMessageRecord> messages = repository.findMessagesByConversation(conversation.id());

        Instant now = clock.instant();
        // 调阅即写审计（redacted/raw 都留痕；raw 带 approval_ref）。
        repository.saveAccessAudit(new WecomAccessAuditRecord(
                "wacc_" + UUID.randomUUID(),
                conversation.id(),
                resolvedViewer,
                resolvedMode,
                raw ? resolvedApprovalRef : null,
                now
        ));

        List<WecomMessageView> views = new ArrayList<>();
        for (WecomMessageRecord message : messages) {
            views.add(new WecomMessageView(
                    message.id(),
                    message.conversationId(),
                    message.senderRef(),
                    message.contentRedacted(),
                    raw ? message.contentRawRef() : null,
                    message.riskFlags(),
                    message.anonymized(),
                    message.sentAt(),
                    message.businessDate()
            ));
        }

        auditEventService.recordSuccess(
                raw ? "wecom.raw_viewed" : "wecom.redacted_viewed",
                raw ? "wecom.view_raw" : "wecom.view",
                "wecom_conversation",
                conversation.id(),
                requestId(),
                null,
                Map.of("view_type", resolvedMode, "viewer_user_id", resolvedViewer, "count", views.size())
        );
        if (raw) {
            // 敏感原文调阅再记一条统一敏感访问事件，便于横向风控聚合。
            auditEventService.recordSuccess(
                    "sensitive.viewed",
                    "wecom.view_raw",
                    "wecom_conversation",
                    conversation.id(),
                    requestId(),
                    null,
                    Map.of("approval_ref", resolvedApprovalRef, "viewer_user_id", resolvedViewer)
            );
        }
        return new PageResponse<>(views, 1, views.size(), views.size());
    }

    // ----------------------------------------------------------------------------------------- 留存策略

    public PageResponse<WecomRetentionPolicyRecord> listRetentionPolicies() {
        List<WecomRetentionPolicyRecord> records = repository.findAllRetentionPolicies();
        return new PageResponse<>(records, 1, 50, records.size());
    }

    public WecomRetentionPolicyRecord upsertRetentionPolicy(WecomRetentionPolicyRequest request) {
        try {
            validatePolicyRequest(request);
            return idempotencyService.execute(
                    "upsertWecomRetentionPolicy",
                    request.idempotencyKey(),
                    request,
                    () -> upsertRetentionPolicyOnce(request),
                    WecomRetentionPolicyRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "wecom.retention_policy_failed",
                    "wecom.retention.manage",
                    "wecom_retention_policy",
                    request == null ? "unknown" : blankToUnknown(request.scopeType()),
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private WecomRetentionPolicyRecord upsertRetentionPolicyOnce(WecomRetentionPolicyRequest request) {
        Instant now = clock.instant();
        String scopeType = request.scopeType().trim().toLowerCase(Locale.ROOT);
        String scopeId = trimToNull(request.scopeId());
        if ("global".equals(scopeType)) {
            scopeId = "*";
        } else if (scopeId == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "scope_id is required for non-global scope");
        }
        WecomRetentionPolicyRecord record = repository.saveRetentionPolicy(new WecomRetentionPolicyRecord(
                "wrp_" + UUID.randomUUID(),
                scopeType,
                scopeId,
                request.retentionDays() == null ? DEFAULT_RETENTION_DAYS : request.retentionDays(),
                request.expiryAction().trim().toLowerCase(Locale.ROOT),
                request.enabled() == null || request.enabled(),
                now,
                now
        ));
        auditEventService.recordSuccess(
                "wecom.retention_policy_set",
                "wecom.retention.manage",
                "wecom_retention_policy",
                record.scopeType() + ":" + record.scopeId(),
                requestId(),
                request.idempotencyKey(),
                Map.of("retention_days", record.retentionDays(), "expiry_action", record.expiryAction())
        );
        return record;
    }

    // ----------------------------------------------------------------------------------------- 留存到期处理

    /**
     * 留存到期处理（可单测直调）：扫描 retention_until &lt; today 且 active 的会话，
     * 按其生效策略 expiry_action 执行 archive/delete/anonymize。
     */
    public WecomRetentionRunResult applyRetention(LocalDate today) {
        int archived = 0;
        int deleted = 0;
        int anonymized = 0;
        for (WecomConversationRecord conversation : repository.findConversationsDueForRetention(today)) {
            WecomRetentionPolicyRecord policy = repository.findEffectivePolicy(conversation.projectId())
                    .orElse(defaultPolicy(clock.instant()));
            String action = policy.expiryAction();
            switch (action) {
                case "delete" -> {
                    repository.deleteMessagesByConversation(conversation.id());
                    repository.updateConversationArchiveStatus(conversation.id(), "deleted");
                    deleted++;
                }
                case "anonymize" -> {
                    repository.anonymizeMessagesByConversation(conversation.id());
                    repository.updateConversationArchiveStatus(conversation.id(), "anonymized");
                    anonymized++;
                }
                default -> {
                    // archive：保留密文/脱敏，仅置归档状态（不再普通展示）。
                    repository.updateConversationArchiveStatus(conversation.id(), "archived");
                    archived++;
                }
            }
            auditEventService.recordSuccess(
                    "wecom.retention_applied",
                    "wecom.retention.manage",
                    "wecom_conversation",
                    conversation.id(),
                    requestId(),
                    null,
                    Map.of("expiry_action", action, "retention_until",
                            conversation.retentionUntil() == null ? "" : conversation.retentionUntil().toString())
            );
        }
        return new WecomRetentionRunResult(archived, deleted, anonymized);
    }

    // ----------------------------------------------------------------------------------------- 质检

    /** 基础关键词敏感命中（P0）；AI 语义质检 P2。 */
    List<String> detectRiskFlags(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : RISK_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (content.contains(keyword)) {
                    flags.add(entry.getKey());
                    break;
                }
            }
        }
        return List.copyOf(flags);
    }

    /**
     * AI 质检结构化结果转 JSON 串落 ai_qc_result（仅 ANALYZED 且有内容时回填；占位/跳过/失败=null）。
     * 序列化异常不阻塞落库——返回 null（结果留空，关键词质检与状态码不受影响）。
     */
    private String aiQcResultJson(WecomQcResult result) {
        if (result == null || !result.hasStructuredResult()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(result.toResultMap());
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    // ----------------------------------------------------------------------------------------- 校验/工具

    private void validateSyncRequest(WecomConversationSyncRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.externalConversationId(), "external_conversation_id");
        requireText(request.projectId(), "project_id");
    }

    private void validatePolicyRequest(WecomRetentionPolicyRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        String scopeType = requireText(request.scopeType(), "scope_type").toLowerCase(Locale.ROOT);
        if (!SCOPE_TYPES.contains(scopeType)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "scope_type is invalid");
        }
        String action = requireText(request.expiryAction(), "expiry_action").toLowerCase(Locale.ROOT);
        if (!EXPIRY_ACTIONS.contains(action)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "expiry_action is invalid");
        }
        if (request.retentionDays() != null && (request.retentionDays() < 1 || request.retentionDays() > 3650)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "retention_days must be between 1 and 3650");
        }
    }

    private WecomRetentionPolicyRecord defaultPolicy(Instant now) {
        return new WecomRetentionPolicyRecord(
                "wrp_global_default", "global", "*", DEFAULT_RETENTION_DAYS, "anonymize", true, now, now);
    }

    private String requireText(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String blankToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR";
    }
}
