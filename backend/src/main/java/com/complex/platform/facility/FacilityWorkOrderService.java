package com.complex.platform.facility;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityWorkOrderActionRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderPolicyRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 楼宇运营 S2 维保/物业工单。 */
@Service
public class FacilityWorkOrderService {

    static final Set<String> TYPES = Set.of("repair", "cleaning", "complaint", "rectification", "inspection", "other");
    static final Set<String> PRIORITIES = Set.of("urgent", "high", "normal");
    static final Set<String> SOURCES = Set.of("merchant_self", "platform", "equipment_alarm", "inspection");
    static final Set<String> ACCEPTANCE_BY = Set.of("handler", "supervisor", "reporter");
    static final Set<String> MERCHANT_SELF_TYPES = Set.of("repair", "cleaning", "complaint", "other");
    static final Set<String> ACTIVE_STATUSES =
            Set.of("pending_assign", "assigned", "processing", "pending_review", "pending_acceptance");
    private static final String PERM = "facility.work_order.manage";
    private static final String BUSINESS_TYPE = "facility_work_order";

    private final FacilityWorkOrderRepository repository;
    private final FacilityRepository facilityRepository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public FacilityWorkOrderService(FacilityWorkOrderRepository repository, FacilityRepository facilityRepository,
            AuditEventService auditEventService, AuthorizationService authorizationService) {
        this.repository = repository;
        this.facilityRepository = facilityRepository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public PageResponse<FacilityWorkOrderRecord> list() {
        List<FacilityWorkOrderRecord> items = repository.list();
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public PageResponse<FacilityWorkOrderRecord> listOverdue() {
        List<FacilityWorkOrderRecord> items = repository.listOverdue(Instant.now());
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public FacilityWorkOrderRecord findById(String id) {
        return load(id);
    }

    @Transactional
    public FacilityWorkOrderRecord create(FacilityWorkOrderRequest request) {
        authorizationService.requirePlatformScope();
        return createInternal(require(request), request == null ? null : request.idempotencyKey());
    }

    @Transactional
    public FacilityWorkOrderRecord assign(String id, FacilityWorkOrderAssignRequest request) {
        authorizationService.requirePlatformScope();
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        FacilityWorkOrderRecord cur = load(id);
        requireStatus(cur, "pending_assign", "can only assign a pending work order");
        Instant now = Instant.now();
        FacilityWorkOrderRecord assigned = new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(),
                cur.priority(), "assigned", cur.title(), cur.description(), cur.source(), cur.spaceId(),
                cur.equipmentId(), cur.reporterId(), cur.reporterType(), requireText(request.assigneeId(), "assignee_id"),
                cur.responseDueAt(), cur.handleDueAt(), now, cur.respondedAt(), cur.completedAt(), cur.verifiedAt(),
                cur.verifierId(), cur.satisfaction(), cur.overtime(), cur.resolution(), cur.projectId(),
                cur.merchantId(), cur.createdAt(), now);
        repository.updateWorkOrder(assigned);
        audit("facility.work_order_assigned", assigned.id(), request.idempotencyKey(),
                Map.of("status", assigned.status(), "assignee_id", assigned.assigneeId()));
        return assigned;
    }

    @Transactional
    public FacilityWorkOrderRecord start(String id, FacilityWorkOrderActionRequest request) {
        authorizationService.requirePlatformScope();
        FacilityWorkOrderActionRequest r = requireAction(request);
        FacilityWorkOrderRecord cur = load(id);
        requireStatus(cur, "assigned", "can only start an assigned work order");
        Instant now = Instant.now();
        FacilityWorkOrderRecord processing = new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(),
                cur.priority(), "processing", cur.title(), cur.description(), cur.source(), cur.spaceId(),
                cur.equipmentId(), cur.reporterId(), cur.reporterType(), cur.assigneeId(), cur.responseDueAt(),
                cur.handleDueAt(), cur.assignedAt(), now, cur.completedAt(), cur.verifiedAt(), cur.verifierId(),
                cur.satisfaction(), cur.overtime(), cur.resolution(), cur.projectId(), cur.merchantId(),
                cur.createdAt(), now);
        repository.updateWorkOrder(processing);
        audit("facility.work_order_started", processing.id(), r.idempotencyKey(), metadata(processing, r.note()));
        return processing;
    }

    @Transactional
    public FacilityWorkOrderRecord complete(String id, FacilityWorkOrderActionRequest request) {
        authorizationService.requirePlatformScope();
        FacilityWorkOrderActionRequest r = requireAction(request);
        FacilityWorkOrderRecord cur = load(id);
        requireStatus(cur, "processing", "can only complete a processing work order");
        String acceptanceBy = policy(cur.workOrderType(), cur.priority()).acceptanceBy();
        String nextStatus = switch (acceptanceBy) {
            case "handler" -> "closed";
            case "supervisor" -> "pending_acceptance";
            case "reporter" -> "pending_review";
            default -> throw ApiException.badRequest("VALIDATION_FAILED", "acceptance_by is invalid");
        };
        Instant now = Instant.now();
        FacilityWorkOrderRecord completed = new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(),
                cur.priority(), nextStatus, cur.title(), cur.description(), cur.source(), cur.spaceId(),
                cur.equipmentId(), cur.reporterId(), cur.reporterType(), cur.assigneeId(), cur.responseDueAt(),
                cur.handleDueAt(), cur.assignedAt(), cur.respondedAt(), now, cur.verifiedAt(), cur.verifierId(),
                cur.satisfaction(), cur.overtime(), requireText(r.resolution(), "resolution"), cur.projectId(),
                cur.merchantId(), cur.createdAt(), now);
        repository.updateWorkOrder(completed);
        resetEquipmentIfClosed(cur, completed);
        audit("facility.work_order_completed", completed.id(), r.idempotencyKey(), metadata(completed, r.note()));
        return completed;
    }

    @Transactional
    public FacilityWorkOrderRecord accept(String id, FacilityWorkOrderActionRequest request) {
        authorizationService.requirePlatformScope();
        FacilityWorkOrderActionRequest r = requireAction(request);
        validateSatisfaction(r.satisfaction());
        FacilityWorkOrderRecord cur = load(id);
        requireStatus(cur, "pending_acceptance", "can only accept a work order pending acceptance");
        FacilityWorkOrderRecord closed = verifyClosed(cur, r);
        repository.updateWorkOrder(closed);
        resetEquipmentIfClosed(cur, closed);
        audit("facility.work_order_accepted", closed.id(), r.idempotencyKey(), metadata(closed, r.note()));
        return closed;
    }

    @Transactional
    public FacilityWorkOrderRecord review(String id, FacilityWorkOrderActionRequest request) {
        authorizationService.requirePlatformScope();
        FacilityWorkOrderActionRequest r = requireAction(request);
        validateSatisfaction(r.satisfaction());
        FacilityWorkOrderRecord cur = load(id);
        requireStatus(cur, "pending_review", "can only review a work order pending review");
        FacilityWorkOrderRecord closed = verifyClosed(cur, r);
        repository.updateWorkOrder(closed);
        resetEquipmentIfClosed(cur, closed);
        audit("facility.work_order_reviewed", closed.id(), r.idempotencyKey(), metadata(closed, r.note()));
        return closed;
    }

    @Transactional
    public FacilityWorkOrderRecord suspend(String id, FacilityWorkOrderActionRequest request) {
        authorizationService.requirePlatformScope();
        FacilityWorkOrderActionRequest r = requireAction(request);
        FacilityWorkOrderRecord cur = load(id);
        if (!ACTIVE_STATUSES.contains(cur.status())) {
            throw ApiException.badRequest("INVALID_STATE", "can only suspend an active work order");
        }
        FacilityWorkOrderRecord suspended = withStatus(cur, "suspended", Instant.now());
        repository.updateWorkOrder(suspended);
        audit("facility.work_order_suspended", suspended.id(), r.idempotencyKey(), metadata(suspended, r.note()));
        return suspended;
    }

    @Transactional
    public FacilityWorkOrderRecord resume(String id, FacilityWorkOrderActionRequest request) {
        authorizationService.requirePlatformScope();
        FacilityWorkOrderActionRequest r = requireAction(request);
        FacilityWorkOrderRecord cur = load(id);
        requireStatus(cur, "suspended", "can only resume a suspended work order");
        FacilityWorkOrderRecord processing = withStatus(cur, "processing", Instant.now());
        repository.updateWorkOrder(processing);
        audit("facility.work_order_resumed", processing.id(), r.idempotencyKey(), metadata(processing, r.note()));
        return processing;
    }

    @Transactional
    public int markOverdue(String idempotencyKey) {
        authorizationService.requirePlatformScope();
        Instant now = Instant.now();
        List<FacilityWorkOrderRecord> overdue = repository.listOverdue(now);
        for (FacilityWorkOrderRecord cur : overdue) {
            repository.updateWorkOrder(new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(),
                    cur.priority(), cur.status(), cur.title(), cur.description(), cur.source(), cur.spaceId(),
                    cur.equipmentId(), cur.reporterId(), cur.reporterType(), cur.assigneeId(), cur.responseDueAt(),
                    cur.handleDueAt(), cur.assignedAt(), cur.respondedAt(), cur.completedAt(), cur.verifiedAt(),
                    cur.verifierId(), cur.satisfaction(), true, cur.resolution(), cur.projectId(), cur.merchantId(),
                    cur.createdAt(), now));
        }
        auditEventService.recordSuccess("facility.work_order_overdue_marked", PERM, BUSINESS_TYPE, "all",
                null, idempotencyKey, Map.of("count", overdue.size()));
        return overdue.size();
    }

    @Transactional
    public FacilityWorkOrderPolicyRecord savePolicy(FacilityWorkOrderPolicyRequest request) {
        authorizationService.requirePlatformScope();
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        String type = normalizeAllowed(request.workOrderType(), "work_order_type", TYPES);
        String priority = normalizeAllowed(request.priority(), "priority", PRIORITIES);
        String acceptanceBy = normalizeAllowed(request.acceptanceBy(), "acceptance_by", ACCEPTANCE_BY);
        int responseMinutes = positive(request.responseMinutes(), "response_minutes");
        int handleMinutes = positive(request.handleMinutes(), "handle_minutes");
        Instant now = Instant.now();
        FacilityWorkOrderPolicyRecord saved = repository.savePolicy(new FacilityWorkOrderPolicyRecord(
                "fwop_" + type + "_" + priority, type, priority, responseMinutes, handleMinutes,
                Boolean.TRUE.equals(request.requiresAcceptance()), acceptanceBy,
                request.active() == null || request.active(), now, now));
        auditEventService.recordSuccess("facility.work_order_policy_saved", PERM, "facility_work_order_policy",
                saved.id(), null, request.idempotencyKey(), Map.of("type", type, "priority", priority));
        return saved;
    }

    @EventListener
    @Transactional
    public void onEquipmentAlarmed(FacilityEquipmentAlarmedEvent event) {
        authorizationService.requirePlatformScope();
        FacilityWorkOrderRequest request = new FacilityWorkOrderRequest("repair", "high",
                "设备告警: " + requireText(event.equipmentName(), "equipment_name"), event.note(),
                "equipment_alarm", null, event.equipmentId(), "system", "system", null, null, null);
        createInternal(request, null);
    }

    private FacilityWorkOrderRecord createInternal(FacilityWorkOrderRequest request, String idempotencyKey) {
        String type = normalizeAllowed(request.workOrderType(), "work_order_type", TYPES);
        String priority = normalizeAllowed(defaultValue(request.priority(), "normal"), "priority", PRIORITIES);
        String source = normalizeAllowed(defaultValue(request.source(), "platform"), "source", SOURCES);
        if ("merchant_self".equals(source) && !MERCHANT_SELF_TYPES.contains(type)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "merchant_self can only create repair, cleaning, complaint or other work orders");
        }
        FacilityWorkOrderPolicyRecord policy = policy(type, priority);
        Instant now = Instant.now();
        FacilityWorkOrderRecord rec = new FacilityWorkOrderRecord("fwo_" + UUID.randomUUID(),
                "FWO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT),
                type, priority, "pending_assign", requireText(request.title(), "title"), request.description(),
                source, trimToNull(request.spaceId()), trimToNull(request.equipmentId()),
                trimToNull(request.reporterId()), trimToNull(request.reporterType()), null,
                now.plus(policy.responseMinutes(), ChronoUnit.MINUTES),
                now.plus(policy.handleMinutes(), ChronoUnit.MINUTES), null, null, null, null, null, null,
                false, null, trimToNull(request.projectId()), trimToNull(request.merchantId()), now, now);
        repository.insertWorkOrder(rec);
        audit("facility.work_order_created", rec.id(), idempotencyKey,
                Map.of("type", rec.workOrderType(), "priority", rec.priority(), "source", rec.source()));
        return rec;
    }

    private FacilityWorkOrderRecord verifyClosed(FacilityWorkOrderRecord cur, FacilityWorkOrderActionRequest r) {
        Instant now = Instant.now();
        return new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(), cur.priority(), "closed",
                cur.title(), cur.description(), cur.source(), cur.spaceId(), cur.equipmentId(), cur.reporterId(),
                cur.reporterType(), cur.assigneeId(), cur.responseDueAt(), cur.handleDueAt(), cur.assignedAt(),
                cur.respondedAt(), cur.completedAt(), now, actorId(), r.satisfaction(), cur.overtime(),
                cur.resolution(), cur.projectId(), cur.merchantId(), cur.createdAt(), now);
    }

    private FacilityWorkOrderRecord withStatus(FacilityWorkOrderRecord cur, String status, Instant now) {
        return new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(), cur.priority(), status,
                cur.title(), cur.description(), cur.source(), cur.spaceId(), cur.equipmentId(), cur.reporterId(),
                cur.reporterType(), cur.assigneeId(), cur.responseDueAt(), cur.handleDueAt(), cur.assignedAt(),
                cur.respondedAt(), cur.completedAt(), cur.verifiedAt(), cur.verifierId(), cur.satisfaction(),
                cur.overtime(), cur.resolution(), cur.projectId(), cur.merchantId(), cur.createdAt(), now);
    }

    private void resetEquipmentIfClosed(FacilityWorkOrderRecord before, FacilityWorkOrderRecord after) {
        if ("closed".equals(before.status()) || !"closed".equals(after.status()) || isBlank(after.equipmentId())) {
            return;
        }
        facilityRepository.findEquipmentById(after.equipmentId()).ifPresent(e -> facilityRepository.updateEquipment(
                new FacilityEquipmentRecord(e.id(), e.name(), e.equipmentType(), e.spaceId(), e.brand(), e.model(),
                        e.vendor(), e.responsibleUserId(), "normal", e.maintenanceCycleDays(), e.lastMaintenanceAt(),
                        e.nextMaintenanceDue(), e.installDate(), e.warrantyUntil(), null, null, null,
                        e.projectId(), e.merchantId(), e.createdAt(), Instant.now())));
    }

    private FacilityWorkOrderRecord load(String id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "facility work order not found"));
    }

    private FacilityWorkOrderPolicyRecord policy(String type, String priority) {
        return repository.findPolicy(type, priority)
                .orElseThrow(() -> ApiException.badRequest("POLICY_NOT_FOUND", "facility work order policy not found"));
    }

    private FacilityWorkOrderRequest require(FacilityWorkOrderRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private FacilityWorkOrderActionRequest requireAction(FacilityWorkOrderActionRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private void requireStatus(FacilityWorkOrderRecord cur, String expected, String message) {
        if (!expected.equals(cur.status())) {
            throw ApiException.badRequest("INVALID_STATE", message);
        }
    }

    private void validateSatisfaction(Integer satisfaction) {
        if (satisfaction != null && (satisfaction < 1 || satisfaction > 5)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "satisfaction must be between 1 and 5");
        }
    }

    private int positive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be positive");
        }
        return value;
    }

    private String normalizeAllowed(String value, String field, Set<String> allowed) {
        String normalized = requireText(value, field).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return normalized;
    }

    private String requireText(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return trimmed;
    }

    private String defaultValue(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String actorId() {
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException exception) {
            return "system";
        }
    }

    private Map<String, Object> metadata(FacilityWorkOrderRecord record, String note) {
        String trimmed = trimToNull(note);
        return trimmed == null ? Map.of("status", record.status()) : Map.of("status", record.status(), "note", trimmed);
    }

    private void audit(String event, String id, String idempotencyKey, Map<String, Object> metadata) {
        auditEventService.recordSuccess(event, PERM, BUSINESS_TYPE, id, null, idempotencyKey, metadata);
    }
}
