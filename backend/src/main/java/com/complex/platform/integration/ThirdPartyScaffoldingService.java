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

    private final IntegrationRepository repository;
    private final Clock clock;
    private final AuditEventService auditEventService;
    private final TerminalSwitchGuard terminalSwitchGuard;

    ThirdPartyScaffoldingService(
            IntegrationRepository repository,
            Clock clock,
            AuditEventService auditEventService,
            TerminalSwitchGuard terminalSwitchGuard
    ) {
        this.repository = repository;
        this.clock = clock;
        this.auditEventService = auditEventService;
        this.terminalSwitchGuard = terminalSwitchGuard;
    }

    WebhookAcceptedResponse receiveTypedWebhook(String type, String idempotencyKey, Map<String, Object> payload) {
        String resolvedType = normalizeType(type);
        // 编程式门控：capability 随 {type} 动态，@GuardedAction 静态值不适用；开关关 → TERMINAL_DISABLED。
        terminalSwitchGuard.check("integration." + resolvedType + ".sync");

        String resolvedIdempotencyKey = idempotencyKey(idempotencyKey, payload);
        requireText(resolvedIdempotencyKey, "idempotency_key");
        try {
            Map<String, Object> safePayload = payload == null ? Map.of() : Map.copyOf(payload);
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
                    false,
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

    RobotTaskEventRecord reportRobotTaskEvent(RobotTaskEventRequest request) {
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
            List<VideoAnchorRecord> items = repository.listVideoAnchors();
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
