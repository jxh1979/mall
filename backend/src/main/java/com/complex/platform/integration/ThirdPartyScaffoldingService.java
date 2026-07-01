package com.complex.platform.integration;

import static com.complex.platform.integration.IntegrationValidation.requireText;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.TerminalSwitchGuard;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * R5 T14 第三方接口/开关预留骨架。
 *
 * <p>口径（裁决③）：仅边界预留——统一 webhook 入口 + 开关门控 + 信封落库（不伪造对账）、
 * 机器人写回调被动留痕、视频点位时间锚点 + 调阅审计。真实回流/自动对账待厂商资料 + 开关开启后交付。
 */
final class ThirdPartyScaffoldingService {

    /** 受 T2 终端能力开关门控的第三方类型；capability = integration.{type}.sync。 */
    private static final Set<String> WEBHOOK_TYPES = Set.of(
            "catering_pos", "retail_pos", "robot", "video", "wecom", "ticket_channel", "payment", "face_age");

    private static final Set<String> ROBOT_EVENT_TYPES = Set.of(
            "created", "picked", "delivering", "delivered", "failed");

    private static final String VIDEO_VIEW_PERMISSION = "integration.video.view";

    /** webhook/robot 回调签名时间戳容差（与 open 平台一致 5 分钟）。 */
    private static final long SIGNATURE_TOLERANCE_MILLIS = 300_000;

    private final IntegrationRepository repository;
    private final Clock clock;
    private final AuditEventService auditEventService;
    private final TerminalSwitchGuard terminalSwitchGuard;
    private final OpenPlatformCrypto crypto;
    private final SecretResolver secretResolver;

    ThirdPartyScaffoldingService(
            IntegrationRepository repository,
            Clock clock,
            AuditEventService auditEventService,
            TerminalSwitchGuard terminalSwitchGuard,
            OpenPlatformCrypto crypto,
            SecretResolver secretResolver
    ) {
        this.repository = repository;
        this.clock = clock;
        this.auditEventService = auditEventService;
        this.terminalSwitchGuard = terminalSwitchGuard;
        this.crypto = crypto;
        this.secretResolver = secretResolver;
    }

    WebhookAcceptedResponse receiveTypedWebhook(String type, String provider, String signature, Long timestamp,
            String nonce, String idempotencyKey, Map<String, Object> payload) {
        String resolvedType = normalizeType(type);
        // 编程式门控：capability 随 {type} 动态，@GuardedAction 静态值不适用；开关关 → TERMINAL_DISABLED（无集成权限）。
        terminalSwitchGuard.check("integration." + resolvedType + ".sync");

        String resolvedIdempotencyKey = idempotencyKey(idempotencyKey, payload);
        requireText(resolvedIdempotencyKey, "idempotency_key");
        try {
            Map<String, Object> safePayload = payload == null ? Map.of() : Map.copyOf(payload);
            // 2.1：验签+防重放——无签名/签名错→401；nonce 重放→401；未配签名密钥→403（未启用）。
            verifyWebhookSignature(resolvedType, provider, "integration." + resolvedType + ".webhook",
                    resolvedIdempotencyKey, safePayload, signature, timestamp, nonce);
            String eventId = eventId(safePayload);
            Instant now = clock.instant();
            // mapping 占位空；processStatus=accepted——只留信封，不伪造对账（厂商映射未配）。
            Map<String, Object> resultJson = new LinkedHashMap<>();
            resultJson.put("accepted", true);
            resultJson.put("process_status", "accepted");
            resultJson.put("mapped", false);
            resultJson.put("third_party_type", resolvedType);
            resultJson.put("mapping", Map.of());
            repository.saveOpenEvent(new OpenApiEventRecord(
                    eventId,
                    "integration-" + resolvedType,
                    resolvedType,
                    "integration." + resolvedType + ".webhook",
                    resolvedIdempotencyKey,
                    true,
                    "accepted",
                    safePayload,
                    Map.copyOf(resultJson),
                    null,
                    now,
                    now
            ));
            auditEventService.recordSuccess(
                    "integration.webhook_received",
                    "not_applicable",
                    "integration_typed_webhook",
                    eventId,
                    requestId(),
                    resolvedIdempotencyKey,
                    Map.of("third_party_type", resolvedType, "process_status", "accepted")
            );
            return new WebhookAcceptedResponse(eventId, false);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "integration.webhook_rejected",
                    "not_applicable",
                    "integration_typed_webhook",
                    resolvedType,
                    requestId(),
                    idempotencyKey,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    RobotTaskEventRecord reportRobotTaskEvent(RobotTaskEventRequest request, String provider, String signature,
            Long timestamp, String nonce) {
        // 机器人上报亦受开关门控（被动留痕，但仍属第三方回流通道）。
        terminalSwitchGuard.check("integration.robot.sync");
        try {
            requireText(request == null ? null : request.robotTaskRef(), "robot_task_ref");
            String eventType = request.eventType();
            if (eventType == null || !ROBOT_EVENT_TYPES.contains(eventType)) {
                throw ApiException.badRequest("ROBOT_EVENT_TYPE_INVALID",
                        "robot event_type must be one of " + ROBOT_EVENT_TYPES);
            }
            Map<String, Object> payload = request.payload() == null ? Map.of() : Map.copyOf(request.payload());
            // 2.1：验签+防重放——签名覆盖任务引用/事件类型/交接/载荷。
            Map<String, Object> signable = new LinkedHashMap<>();
            signable.put("robot_task_ref", request.robotTaskRef());
            signable.put("event_type", eventType);
            signable.put("handover_id", request.handoverId());
            signable.put("payload", payload);
            verifyWebhookSignature("robot", provider, "integration.robot.report",
                    request.idempotencyKey(), signable, signature, timestamp, nonce);
            RobotTaskEventRecord record = new RobotTaskEventRecord(
                    "robotevt_" + UUID.randomUUID(),
                    request.robotTaskRef(),
                    eventType,
                    request.handoverId(),
                    payload,
                    clock.instant()
            );
            repository.saveRobotTaskEvent(record);
            auditEventService.recordSuccess(
                    "integration.robot_reported",
                    "not_applicable",
                    "integration_robot_task_event",
                    record.id(),
                    requestId(),
                    request.idempotencyKey(),
                    Map.of("robot_task_ref", record.robotTaskRef(), "event_type", record.eventType())
            );
            return record;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "integration.robot_report_failed",
                    "not_applicable",
                    "integration_robot_task_event",
                    request == null ? "unknown" : blankToUnknown(request.robotTaskRef()),
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    /**
     * 2.1 第三方 webhook/robot 回调验签+防重放：无签名/签名错→401；时间戳过期→401；nonce 重放→401；
     * 未配置签名密钥（未启用该集成）→403。通过后以 type 为命名空间预留 nonce（同一传输再发即拒）。
     */
    private void verifyWebhookSignature(String type, String provider, String event, String idempotencyKey,
            Map<String, Object> payload, String signature, Long timestamp, String nonce) {
        if (signature == null || signature.isBlank()) {
            throw ApiException.unauthorized("INTEGRATION_WEBHOOK_SIGNATURE_INVALID",
                    "integration webhook signature is required");
        }
        requireText(provider, "provider");
        requireText(nonce, "nonce");
        String resolvedNonce = nonce.trim();
        long ts = timestamp == null ? 0L : timestamp;
        if (Math.abs(clock.millis() - ts) > SIGNATURE_TOLERANCE_MILLIS) {
            throw ApiException.unauthorized("INTEGRATION_WEBHOOK_TIMESTAMP_STALE",
                    "integration webhook timestamp is stale");
        }
        String secret = resolveWebhookSecret(type);
        String payloadHash = crypto.payloadHash(payload);
        crypto.verifySignature(signature,
                crypto.webhookCanonical(type, provider, event, idempotencyKey, payloadHash, ts, resolvedNonce),
                secret, "INTEGRATION_WEBHOOK_SIGNATURE_INVALID");
        if (!repository.reserveOpenNonce(type, resolvedNonce, ts)) {
            throw ApiException.unauthorized("INTEGRATION_WEBHOOK_NONCE_REPLAYED",
                    "integration webhook nonce was already used");
        }
    }

    private String resolveWebhookSecret(String type) {
        return secretResolver.resolve("integration." + type + ".webhook")
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> ApiException.forbidden("INTEGRATION_WEBHOOK_NOT_ENABLED",
                        "integration webhook is not enabled: no signing secret configured for type " + type));
    }

    VideoAnchorRecord recordVideoAnchor(String pointId, Instant eventTime, String businessRef) {
        requireText(pointId, "point_id");
        Instant resolvedEventTime = eventTime == null ? clock.instant() : eventTime;
        VideoAnchorRecord record = new VideoAnchorRecord(
                "videoanchor_" + UUID.randomUUID(),
                pointId,
                resolvedEventTime,
                businessRef,
                null, // clip_index_ref 留空待厂商监控片段索引接入
                clock.instant()
        );
        repository.saveVideoAnchor(record);
        auditEventService.recordSuccess(
                "integration.video_anchor_recorded",
                "not_applicable",
                "integration_video_anchor",
                record.id(),
                requestId(),
                null,
                Map.of("point_id", pointId, "business_ref", businessRef == null ? "" : businessRef)
        );
        return record;
    }

    PageResponse<VideoAnchorRecord> listVideoAnchors(String viewerUserId, String approvalRef) {
        try {
            // 2.2：视频调阅强制复核——approval_ref 必填，且须对应 approved 主管审批、查看人/对象匹配、未过期，否则 403+失败审计。
            if (approvalRef == null || approvalRef.isBlank()) {
                throw ApiException.forbidden("VIDEO_ACCESS_APPROVAL_REQUIRED",
                        "video access requires an approved approval_ref");
            }
            String ref = approvalRef.trim();
            VideoAccessApprovalRecord approval = repository.findVideoAccessApproval(ref)
                    .filter(a -> "approved".equals(a.status()))
                    .orElseThrow(() -> ApiException.forbidden("VIDEO_ACCESS_NOT_APPROVED",
                            "approval_ref is not an approved video access review"));
            if (!approval.viewerUserId().equals(viewerUserId)) {
                throw ApiException.forbidden("VIDEO_ACCESS_VIEWER_MISMATCH",
                        "approval_ref is not granted to this viewer");
            }
            if (approval.validUntil() != null && clock.instant().isAfter(approval.validUntil())) {
                throw ApiException.forbidden("VIDEO_ACCESS_APPROVAL_EXPIRED", "video access approval has expired");
            }
            List<VideoAnchorRecord> all = repository.listVideoAnchors();
            // 对象匹配：审批限定点位则只返回该点位锚点（NULL=全部点位）。
            List<VideoAnchorRecord> items = approval.pointId() == null ? all
                    : all.stream().filter(a -> approval.pointId().equals(a.pointId())).toList();
            // 调阅敏感：逐条写调阅审计（viewer + approval_ref）。
            Instant now = clock.instant();
            for (VideoAnchorRecord anchor : items) {
                repository.saveVideoAccessAudit(
                        "videoaudit_" + UUID.randomUUID(),
                        anchor.id(),
                        viewerUserId,
                        approvalRef,
                        now
                );
            }
            auditEventService.recordSuccess(
                    "integration.video_clip_viewed",
                    VIDEO_VIEW_PERMISSION,
                    "integration_video_anchor",
                    "all",
                    requestId(),
                    null,
                    Map.of("count", items.size(), "viewer_user_id", blankToUnknown(viewerUserId))
            );
            return new PageResponse<>(items, 1, 50, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "integration.video_clip_view_failed",
                    VIDEO_VIEW_PERMISSION,
                    "integration_video_anchor",
                    "all",
                    requestId(),
                    null,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw ApiException.badRequest("THIRD_PARTY_TYPE_REQUIRED", "third-party type is required");
        }
        String normalized = type.trim();
        if (!WEBHOOK_TYPES.contains(normalized)) {
            throw ApiException.badRequest("THIRD_PARTY_TYPE_UNSUPPORTED",
                    "third-party type is not supported: " + normalized);
        }
        return normalized;
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
