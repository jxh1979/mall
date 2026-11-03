package com.complex.platform.amusement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.amusement.dto.AmusementIncidentActionRequest;
import com.complex.platform.amusement.dto.AmusementIncidentCloseRequest;
import com.complex.platform.amusement.dto.AmusementIncidentCreateRequest;
import com.complex.platform.amusement.dto.AmusementSafetyNoticeCreateRequest;
import com.complex.platform.amusement.dto.AmusementWaiverAcknowledgementRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.RiskAlertService;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class AmusementSafetyService {

    private static final Set<String> NOTICE_TYPES = Set.of("safety_notice", "waiver", "risk_disclosure");
    private static final Set<String> STATUSES = Set.of("active", "inactive", "archived");
    private static final Set<String> SEVERITIES = Set.of("low", "medium", "high", "critical");
    private static final Set<String> ACTION_TARGET_STATUSES = Set.of("processing", "pending_external", "resolved");
    private static final Set<String> TERMINAL_STATUSES = Set.of("closed", "cancelled");

    private final AmusementSafetyRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    // P1：evidence 统一异常责任链服务。安全事件除写孤岛 amusement_incidents 外，
    // 再接入统一 risk_alerts（source_domain='amusement'）。
    // 可为 null（向后兼容/单测无 SecurityContext 环境）；接入为 fail-safe，不阻塞事件创建主链。
    private final RiskAlertService riskAlertService;

    @org.springframework.beans.factory.annotation.Autowired
    public AmusementSafetyService(
            AmusementSafetyRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            RiskAlertService riskAlertService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.riskAlertService = riskAlertService;
    }

    public PageResponse<AmusementSafetyNoticeRecord> listSafetyNotices() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        List<AmusementSafetyNoticeRecord> records = scope.isPresent()
                ? repository.findSafetyNoticesByMerchant(scope.get())
                : repository.findAllSafetyNotices();
        auditEventService.recordSuccess(
                "amusement.safety_notice.list",
                "amusement.safety_notice.view",
                "amusement_safety_notice",
                "all",
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, 50, records.size());
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_notice.manage")
    @Transactional
    public AmusementSafetyNoticeRecord createSafetyNotice(AmusementSafetyNoticeCreateRequest request) {
        return executeNoticeWrite(
                "createAmusementSafetyNotice",
                request == null ? null : request.idempotencyKey(),
                request,
                "amusement.safety_notice.create_failed",
                "amusement.safety_notice.manage",
                "new",
                () -> validateNoticeRequest(request),
                () -> createSafetyNoticeOnce(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.waiver.acknowledge")
    @Transactional
    public AmusementWaiverAcknowledgementRecord acknowledgeWaiver(
            String noticeId,
            AmusementWaiverAcknowledgementRequest request
    ) {
        String id = requireText(noticeId, "notice_id");
        return executeAcknowledgementWrite(
                "acknowledgeAmusementWaiver:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                "amusement.waiver.acknowledge_failed",
                "amusement.waiver.acknowledge",
                id,
                () -> {
                    ensureNoticeExists(id);
                    validateAcknowledgementRequest(request);
                },
                () -> acknowledgeWaiverOnce(id, request)
        );
    }

    public PageResponse<AmusementIncidentRecord> listIncidents() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        List<AmusementIncidentRecord> records = scope.isPresent()
                ? repository.findIncidentsByMerchant(scope.get())
                : repository.findAllIncidents();
        auditEventService.recordSuccess(
                "amusement.incident.list",
                "amusement.incident.view",
                "amusement_incident",
                "all",
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, 50, records.size());
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.incident.create")
    @Transactional
    public AmusementIncidentRecord createIncident(AmusementIncidentCreateRequest request) {
        return executeIncidentWrite(
                "createAmusementIncident",
                request == null ? null : request.idempotencyKey(),
                request,
                "amusement.incident.create_failed",
                "amusement.incident.create",
                "new",
                () -> validateIncidentCreateRequest(request),
                () -> createIncidentOnce(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.incident.process")
    @Transactional
    public AmusementIncidentRecord addIncidentAction(String incidentId, AmusementIncidentActionRequest request) {
        String id = requireText(incidentId, "incident_id");
        return executeIncidentWrite(
                "addAmusementIncidentAction:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                "amusement.incident.action_failed",
                "amusement.incident.process",
                id,
                () -> {
                    validateIncidentActionRequest(request);
                    rejectClosed(findIncident(id));
                },
                () -> addIncidentActionOnce(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.incident.close", admissionAction = "amusement.incident_close", objectType = "amusement_incident")
    @Transactional
    public AmusementIncidentRecord closeIncident(String incidentId, AmusementIncidentCloseRequest request) {
        String id = requireText(incidentId, "incident_id");
        return executeIncidentWrite(
                "closeAmusementIncident:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                "amusement.incident.close_failed",
                "amusement.incident.close",
                id,
                () -> {
                    validateIncidentCloseRequest(request);
                    // M724-F11（治理地图第 303 行「同一次操作重复提交时，返回第一次的结果并提示已完成。」）：这里只核对事故存在，
                    // 不再核对「已关闭」——关闭成功后同一提交编号、同一正文重提要由幂等层返回第一次的结果，排在幂等层之前的
                    // 「已关闭」校验会先把它拒掉（409 INCIDENT_CLOSED）。首次执行的「已关闭」校验在幂等层之内（closeIncidentOnce）：
                    // 换新编号再关已关闭的事故照旧被拒。
                    findIncident(id);
                },
                () -> closeIncidentOnce(id, request)
        );
    }

    private AmusementSafetyNoticeRecord createSafetyNoticeOnce(AmusementSafetyNoticeCreateRequest request) {
        Instant now = Instant.now();
        AmusementSafetyNoticeRecord record = repository.saveSafetyNotice(new AmusementSafetyNoticeRecord(
                "asn_" + slug(request.idempotencyKey()),
                requireText(request.projectId(), "project_id"),
                trimToNull(request.amusementProjectId()),
                normalizeAllowed(request.noticeType(), "notice_type", NOTICE_TYPES),
                requireText(request.title(), "title"),
                requireText(request.contentVersion(), "content_version"),
                requireText(request.contentText(), "content_text"),
                normalizeAllowed(request.status(), "status", STATUSES),
                request.effectiveFrom(),
                request.effectiveTo(),
                requireText(request.operatorUserId(), "operator_user_id"),
                evidenceIds(request.evidenceAttachmentIds()),
                now,
                now,
                authorizationService.currentMerchantScope().orElse(null)
        ));
        auditEventService.recordSuccess(
                "amusement.safety_notice.create",
                "amusement.safety_notice.manage",
                "amusement_safety_notice",
                record.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", record.status())
        );
        return record;
    }

    private AmusementWaiverAcknowledgementRecord acknowledgeWaiverOnce(
            String noticeId,
            AmusementWaiverAcknowledgementRequest request
    ) {
        Instant now = Instant.now();
        AmusementWaiverAcknowledgementRecord record = repository.saveWaiverAcknowledgement(
                new AmusementWaiverAcknowledgementRecord(
                        "awa_" + slug(request.idempotencyKey()),
                        noticeId,
                        requireText(request.projectId(), "project_id"),
                        trimToNull(request.amusementProjectId()),
                        trimToNull(request.sessionId()),
                        requireText(request.customerId(), "customer_id"),
                        requireText(request.visitorName(), "visitor_name"),
                        requireText(request.signerName(), "signer_name"),
                        requireText(request.acknowledgementMethod(), "acknowledgement_method"),
                        requireText(request.contentVersion(), "content_version"),
                        trimToNull(request.contentHash()),
                        request.acknowledgedAt() == null ? now : request.acknowledgedAt(),
                        requireText(request.operatorUserId(), "operator_user_id"),
                        evidenceIds(request.evidenceAttachmentIds()),
                        now
                )
        );
        auditEventService.recordSuccess(
                "amusement.waiver.acknowledge",
                "amusement.waiver.acknowledge",
                "amusement_waiver_acknowledgement",
                record.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("notice_id", noticeId)
        );
        return record;
    }

    private AmusementIncidentRecord createIncidentOnce(AmusementIncidentCreateRequest request) {
        Instant now = Instant.now();
        AmusementIncidentRecord record = repository.saveIncident(new AmusementIncidentRecord(
                "ami_" + slug(request.idempotencyKey()),
                requireText(request.projectId(), "project_id"),
                trimToNull(request.amusementProjectId()),
                trimToNull(request.sessionId()),
                trimToNull(request.deviceId()),
                trimToNull(request.customerId()),
                normalizeAllowed(request.severity(), "severity", SEVERITIES),
                "accident",
                "created",
                requireText(request.title(), "title"),
                requireText(request.description(), "description"),
                requireText(request.reportedByUserId(), "reported_by_user_id"),
                requireText(request.reportedChannel(), "reported_channel"),
                request.occurredAt() == null ? now : request.occurredAt(),
                null,
                null,
                evidenceIds(request.evidenceAttachmentIds()),
                now,
                now,
                authorizationService.currentMerchantScope().orElse(null)
        ));
        repository.saveIncidentAction(action(
                record.id(),
                "created",
                null,
                "created",
                record.reportedByUserId(),
                defaultNote(request.operatorNote(), "created"),
                record.evidenceAttachmentIds()
        ));
        // 双写统一责任链（fail-safe，不阻塞事件创建主链；孤岛 amusement_incidents 仍权威）。
        reportToUnifiedRiskAlerts(record);
        // 事故停运联动：high/critical 安全事故自动停运停售（项目 open→paused，CAS 幂等：仅当前为 open 才流转，
        // 已 paused/maintenance/closed 不重复动作、不报错）。消除"出事故仍照常售票/核销"的安全敞口；
        // 恢复营业须走巡检/检修复核流程（reopen），不在此自动恢复。
        // CG-02：记原因 safety_incident（V443）——与硬超容自动停运区分开，出场人数回落的自动恢复只认 hard_capacity；
        // 项目正处硬超容自动停运时发生高危事故，原因升级为 safety_incident，回落后同样不自动恢复。
        boolean autoPaused = false;
        if ("high".equals(record.severity()) || "critical".equals(record.severity())) {
            autoPaused = repository.autoPauseProject(record.projectId(), "safety_incident");
            if (!autoPaused) {
                repository.escalateAutoPausedReasonToSafetyIncident(record.projectId());
            }
        }
        auditEventService.recordSuccess(
                "amusement.incident.create",
                "amusement.incident.create",
                "amusement_incident",
                record.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", record.status(), "severity", record.severity(),
                        "project_auto_paused", autoPaused)
        );
        return record;
    }

    /**
     * 把安全事件双写到统一 risk_alerts（source_domain='amusement'，alert_type='safety_incident'）。
     *
     * <p>fail-safe：未注入服务（单测/无 SecurityContext）或上报异常都不阻塞事件创建主链——
     * 孤岛 amusement_incidents 仍是落地权威，统一表为汇聚镜像。
     * idempotencyKey 用 incidentId+":unified" 稳定唯一，重复创建不重复上报。
     */
    private void reportToUnifiedRiskAlerts(AmusementIncidentRecord incident) {
        if (riskAlertService == null) {
            return;
        }
        try {
            riskAlertService.report(new RiskAlertReportRequest(
                    incident.id() + ":unified",
                    "amusement",
                    incident.id(),
                    "safety_incident",
                    mapRiskLevel(incident.severity()),
                    "amusement safety incident: " + incident.title(),
                    incident.projectId(),
                    "incident=" + incident.id() + "; severity=" + incident.severity(),
                    List.of()));
        } catch (RuntimeException exception) {
            // 统一表上报失败不影响事件创建主链（incident 已落孤岛表 amusement_incidents）。
            auditEventService.recordFailure(
                    "amusement.risk.unified_alert_failed",
                    "amusement.incident.create",
                    "risk_alert",
                    incident.id(),
                    requestId(),
                    incident.id() + ":unified",
                    exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName());
        }
    }

    /**
     * 把游乐域 severity 映射到统一表合法 risk_level（仅 normal/suspicious/high/critical）。
     * medium → suspicious（否则统一表 VALIDATION_FAILED 被吞=丢数据）；high/critical 直透；low → normal。
     */
    private String mapRiskLevel(String severity) {
        String normalized = severity == null ? "" : severity.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "medium" -> "suspicious";
            case "high" -> "high";
            case "critical" -> "critical";
            case "low", "normal" -> "normal";
            default -> "suspicious";
        };
    }

    private AmusementIncidentRecord addIncidentActionOnce(String incidentId, AmusementIncidentActionRequest request) {
        AmusementIncidentRecord existing = findIncident(incidentId);
        checkIncidentOwnership(existing);
        rejectClosed(existing);
        String targetStatus = normalizeAllowed(request.targetStatus(), "target_status", ACTION_TARGET_STATUSES);
        AmusementIncidentRecord saved = repository.saveIncident(existing.transitionTo(targetStatus, Instant.now()));
        repository.saveIncidentAction(action(
                incidentId,
                existing.status().equals(targetStatus) ? "commented" : "status_changed",
                existing.status(),
                saved.status(),
                requireText(request.operatorUserId(), "operator_user_id"),
                requireText(request.operatorNote(), "operator_note"),
                evidenceIds(request.evidenceAttachmentIds())
        ));
        auditEventService.recordSuccess(
                "amusement.incident.action",
                "amusement.incident.process",
                "amusement_incident",
                saved.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", saved.status())
        );
        return saved;
    }

    private AmusementIncidentRecord closeIncidentOnce(String incidentId, AmusementIncidentCloseRequest request) {
        AmusementIncidentRecord existing = findIncident(incidentId);
        checkIncidentOwnership(existing);
        rejectClosed(existing);
        AmusementIncidentRecord closed = repository.saveIncident(existing.close(
                requireText(request.resolution(), "resolution"),
                Instant.now()
        ));
        repository.saveIncidentAction(action(
                incidentId,
                "closed",
                existing.status(),
                "closed",
                requireText(request.closedByUserId(), "closed_by_user_id"),
                defaultNote(request.operatorNote(), "closed"),
                evidenceIds(request.evidenceAttachmentIds())
        ));
        auditEventService.recordSuccess(
                "amusement.incident.close",
                "amusement.incident.close",
                "amusement_incident",
                closed.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", closed.status())
        );
        return closed;
    }

    private AmusementIncidentRecord findIncident(String id) {
        return repository.findIncidentById(id)
                .orElseThrow(() -> ApiException.notFound("INCIDENT_NOT_FOUND", "游乐事件不存在或已被删除"));
    }

    private void ensureNoticeExists(String id) {
        AmusementSafetyNoticeRecord notice = repository.findSafetyNoticeById(id)
                .orElseThrow(() -> ApiException.notFound("SAFETY_NOTICE_NOT_FOUND", "安全告示不存在或已被删除"));
        authorizationService.currentMerchantScope().ifPresent(merchantId -> {
            if (!merchantId.equals(notice.merchantId())) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "该安全告示不属于当前商户");
            }
        });
    }

    private void validateNoticeRequest(AmusementSafetyNoticeCreateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.projectId(), "project_id");
        normalizeAllowed(request.noticeType(), "notice_type", NOTICE_TYPES);
        requireText(request.title(), "title");
        requireText(request.contentVersion(), "content_version");
        requireText(request.contentText(), "content_text");
        normalizeAllowed(request.status(), "status", STATUSES);
        requireText(request.operatorUserId(), "operator_user_id");
    }

    private void validateAcknowledgementRequest(AmusementWaiverAcknowledgementRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.projectId(), "project_id");
        requireText(request.customerId(), "customer_id");
        requireText(request.visitorName(), "visitor_name");
        requireText(request.signerName(), "signer_name");
        requireText(request.acknowledgementMethod(), "acknowledgement_method");
        requireText(request.contentVersion(), "content_version");
        requireText(request.operatorUserId(), "operator_user_id");
    }

    private void validateIncidentCreateRequest(AmusementIncidentCreateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.projectId(), "project_id");
        normalizeAllowed(request.severity(), "severity", SEVERITIES);
        requireText(request.title(), "title");
        requireText(request.description(), "description");
        requireText(request.reportedByUserId(), "reported_by_user_id");
        requireText(request.reportedChannel(), "reported_channel");
    }

    private void validateIncidentActionRequest(AmusementIncidentActionRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.operatorNote(), "operator_note");
        normalizeAllowed(request.targetStatus(), "target_status", ACTION_TARGET_STATUSES);
        requireText(request.operatorUserId(), "operator_user_id");
    }

    private void validateIncidentCloseRequest(AmusementIncidentCloseRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.resolution(), "resolution");
        requireText(request.closedByUserId(), "closed_by_user_id");
    }

    private AmusementIncidentActionRecord action(
            String incidentId,
            String actionType,
            String fromStatus,
            String toStatus,
            String operatorUserId,
            String operatorNote,
            List<String> evidenceAttachmentIds
    ) {
        return new AmusementIncidentActionRecord(
                UUID.randomUUID().toString(),
                incidentId,
                actionType,
                fromStatus,
                toStatus,
                operatorUserId,
                operatorNote,
                evidenceAttachmentIds,
                Instant.now()
        );
    }

    private AmusementSafetyNoticeRecord executeNoticeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            String failureEvent,
            String permissionCode,
            String businessId,
            Runnable preValidation,
            Supplier<AmusementSafetyNoticeRecord> action
    ) {
        try {
            preValidation.run();
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    action,
                    AmusementSafetyNoticeRecord.class
            );
        } catch (RuntimeException exception) {
            recordFailure(failureEvent, permissionCode, "amusement_safety_notice", businessId, idempotencyKey, exception);
            throw exception;
        }
    }

    private AmusementWaiverAcknowledgementRecord executeAcknowledgementWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            String failureEvent,
            String permissionCode,
            String businessId,
            Runnable preValidation,
            Supplier<AmusementWaiverAcknowledgementRecord> action
    ) {
        try {
            preValidation.run();
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    action,
                    AmusementWaiverAcknowledgementRecord.class
            );
        } catch (RuntimeException exception) {
            recordFailure(failureEvent, permissionCode, "amusement_waiver_acknowledgement", businessId, idempotencyKey, exception);
            throw exception;
        }
    }

    private AmusementIncidentRecord executeIncidentWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            String failureEvent,
            String permissionCode,
            String businessId,
            Runnable preValidation,
            Supplier<AmusementIncidentRecord> action
    ) {
        try {
            preValidation.run();
            return idempotencyService.execute(operationId, idempotencyKey, request, action, AmusementIncidentRecord.class);
        } catch (RuntimeException exception) {
            recordFailure(failureEvent, permissionCode, "amusement_incident", businessId, idempotencyKey, exception);
            throw exception;
        }
    }

    private void recordFailure(
            String failureEvent,
            String permissionCode,
            String businessType,
            String businessId,
            String idempotencyKey,
            RuntimeException exception
    ) {
        auditEventService.recordFailure(
                failureEvent,
                permissionCode,
                businessType,
                businessId,
                requestId(),
                idempotencyKey,
                errorCode(exception)
        );
    }

    private void rejectClosed(AmusementIncidentRecord incident) {
        if (TERMINAL_STATUSES.contains(incident.status())) {
            throw ApiException.conflict("INCIDENT_CLOSED", "已关闭或已取消的事件不能再变更");
        }
    }

    private void checkIncidentOwnership(AmusementIncidentRecord incident) {
        authorizationService.currentMerchantScope().ifPresent(merchantId -> {
            if (!merchantId.equals(incident.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该事件不属于当前商户");
            }
        });
    }

    private String normalizeAllowed(String value, String field, Set<String> allowed) {
        String normalized = requireText(value, field).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return normalized;
    }

    private String defaultNote(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private List<String> evidenceIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                cleaned.add(trimmed);
            }
        }
        return List.copyOf(cleaned);
    }

    private String slug(String value) {
        String source = requireText(value, "idempotency_key");
        UUID uuid = UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(asBytes(uuid)).substring(0, 24);
    }

    private byte[] asBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        for (int index = 0; index < 8; index++) {
            bytes[index] = (byte) (most >>> (8 * (7 - index)));
            bytes[index + 8] = (byte) (least >>> (8 * (7 - index)));
        }
        return bytes;
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR";
    }
}
