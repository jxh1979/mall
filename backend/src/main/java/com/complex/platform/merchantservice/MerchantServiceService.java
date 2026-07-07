package com.complex.platform.merchantservice;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderActionRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderAssignRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCloseRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCreateRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteApprovalRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteDecision;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteRequest;
import java.time.Instant;
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

@Service
public class MerchantServiceService {

    private static final int DEFAULT_PAGE_LIMIT = 100;
    // SLA 升级单批上限：一次定时扫描最多处理的工单数，防止单次事务过大。
    private static final int ESCALATE_BATCH_LIMIT = 200;
    // 系统操作者标识：SLA 自动升级无登录用户，留痕 operator_user_id 用此系统标识。
    private static final String SYSTEM_OPERATOR = "system";
    private static final String BUSINESS_TYPE = "merchant_service_work_order";
    private static final Set<String> TYPES = Set.of("repair", "complaint", "rectification", "service");
    private static final Set<String> PRIORITIES = Set.of("low", "medium", "high", "critical");
    private static final Set<String> ACTION_TARGET_STATUSES = Set.of("assigned", "processing", "pending_merchant", "resolved");
    private static final Set<String> TERMINAL_STATUSES = Set.of("closed", "cancelled");

    // Allowed forward/lateral status transitions for addAction. Re-affirming the current status is a
    // comment (always allowed) and handled separately. This forbids illegal skips (e.g. created->resolved)
    // and regressions (e.g. resolved->assigned). Terminal states (closed/cancelled) are rejected earlier.
    private static final Map<String, Set<String>> ALLOWED_ACTION_TRANSITIONS = Map.of(
            "created", Set.of("assigned"),
            "assigned", Set.of("processing", "pending_merchant", "resolved"),
            "processing", Set.of("pending_merchant", "resolved"),
            "pending_merchant", Set.of("processing", "resolved"),
            "resolved", Set.of());

    private final MerchantServiceRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public MerchantServiceService(
            MerchantServiceRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public PageResponse<MerchantWorkOrderRecord> listWorkOrders() {
        return listWorkOrders(false);
    }

    /** V264：includeArchived=false（默认）排除已存档工单；true 时含存档（供后台管理存档件）。 */
    public PageResponse<MerchantWorkOrderRecord> listWorkOrders(boolean includeArchived) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        List<MerchantWorkOrderRecord> records =
                repository.findWorkOrdersByScope(scope, includeArchived, DEFAULT_PAGE_LIMIT);
        auditEventService.recordSuccess(
                "merchant_service.work_order.viewed",
                "merchant.service_work_order.view",
                BUSINESS_TYPE,
                "all",
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, DEFAULT_PAGE_LIMIT, records.size());
    }

    @Transactional
    public MerchantWorkOrderRecord createWorkOrder(MerchantWorkOrderCreateRequest request) {
        String operationId = "createMerchantWorkOrder:" + authorizationService.currentActor().id();
        return executeWrite(
                operationId,
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.work_order.create_failed",
                "merchant.service_work_order.create",
                "new",
                () -> validateCreateRequest(request),
                () -> createWorkOrderOnce(request)
        );
    }

    @Transactional
    public MerchantWorkOrderRecord assignWorkOrder(String id, MerchantWorkOrderAssignRequest request) {
        String workOrderId = requireText(id, "id");
        return executeWrite(
                "assignMerchantWorkOrder:" + workOrderId,
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.work_order.assign_failed",
                "merchant.service_work_order.assign",
                workOrderId,
                () -> {
                    validateAssignRequest(request);
                    ensureWorkOrderCanChange(workOrderId);
                },
                () -> assignWorkOrderOnce(workOrderId, request)
        );
    }

    @Transactional
    public MerchantWorkOrderRecord addAction(String id, MerchantWorkOrderActionRequest request) {
        String workOrderId = requireText(id, "id");
        return executeWrite(
                "addMerchantWorkOrderAction:" + workOrderId,
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.work_order.action_failed",
                "merchant.service_work_order.process",
                workOrderId,
                () -> {
                    validateActionRequest(request);
                    ensureWorkOrderCanChange(workOrderId);
                },
                () -> addActionOnce(workOrderId, request)
        );
    }

    @Transactional
    public MerchantWorkOrderRecord closeWorkOrder(String id, MerchantWorkOrderCloseRequest request) {
        String workOrderId = requireText(id, "id");
        return executeWrite(
                "closeMerchantWorkOrder:" + workOrderId,
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.work_order.close_failed",
                "merchant.service_work_order.close",
                workOrderId,
                () -> {
                    validateCloseRequest(request);
                    ensureWorkOrderCanChange(workOrderId);
                },
                () -> closeWorkOrderOnce(workOrderId, request)
        );
    }

    /**
     * SLA 自动超时升级（可直接调用，供 @Scheduled 薄包装与单测调用）：
     * 扫描 due_at 已过、未完成（status 非 resolved/closed/cancelled）且尚未升级（escalated_at IS NULL）的工单，
     * 逐单打标 sla_breached + escalated_at（markSlaBreached，不改 status），并写一条 'escalated' 动作留痕。
     * 留痕的 from/to status 一致（升级不迁移状态），operator 用系统标识（无登录上下文）。
     * 一次调用最多处理 escalateBatchLimit 条；返回本次升级的工单数。
     *
     * @param now 参照时刻（due_at &lt;= now 视为已超时）
     */
    @Transactional
    public int escalateOverdueWorkOrders(Instant now) {
        if (now == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "now is required");
        }
        List<MerchantWorkOrderRecord> overdue = repository.findOverdueOpenWorkOrders(now, ESCALATE_BATCH_LIMIT);
        int escalated = 0;
        for (MerchantWorkOrderRecord record : overdue) {
            // 并发护栏：条件 UPDATE WHERE escalated_at IS NULL，受影响行数==1 才算本调用赢得升级。
            // 行数==0 表示已被其它并发事务（定时 vs 手动触发）抢先升级，跳过留痕/审计避免双重升级、双留痕。
            int affected = repository.markSlaBreachedIfNotEscalated(record.id(), now);
            if (affected == 0) {
                continue;
            }
            repository.saveAction(action(
                    record.id(),
                    "escalated",
                    record.status(),
                    record.status(),
                    SYSTEM_OPERATOR,
                    "SLA overdue auto-escalation",
                    List.of()
            ));
            auditEventService.recordSuccess(
                    "merchant_service.work_order.escalated",
                    "merchant.service_work_order.process",
                    BUSINESS_TYPE,
                    record.id(),
                    requestId(),
                    null,
                    Map.of("status", record.status(), "sla_breached", true)
            );
            escalated++;
        }
        return escalated;
    }

    private MerchantWorkOrderRecord createWorkOrderOnce(MerchantWorkOrderCreateRequest request) {
        String type = normalizeAllowed(request.workOrderType(), "work_order_type", TYPES);
        String priority = normalizeAllowed(request.priority(), "priority", PRIORITIES);
        // Merchant scope from token; platform admin must supply merchant_id in request
        String merchantId = authorizationService.currentMerchantScope()
                .orElseGet(() -> requireText(request.merchantId(), "merchant_id"));
        // Operator identity always comes from the authenticated principal
        String reportedByUserId = authorizationService.currentActor().id();
        Instant now = Instant.now();
        MerchantWorkOrderRecord record = repository.saveWorkOrder(new MerchantWorkOrderRecord(
                "mswo_" + UUID.randomUUID().toString().replace("-", ""),
                merchantId,
                trimToNull(request.spaceId()),
                trimToNull(request.contractId()),
                type,
                priority,
                "created",
                requireText(request.title(), "title"),
                requireText(request.description(), "description"),
                reportedByUserId,
                requireText(request.reportedChannel(), "reported_channel"),
                null,
                null,
                request.dueAt(),
                null,
                null,
                null,
                evidenceIds(request.evidenceAttachmentIds()),
                false,
                null,
                now,
                now
        ));
        repository.saveAction(action(
                record.id(),
                "created",
                null,
                "created",
                reportedByUserId,
                defaultNote(request.operatorNote(), "created"),
                record.evidenceAttachmentIds()
        ));
        auditEventService.recordSuccess(
                "merchant_service.work_order.created",
                "merchant.service_work_order.create",
                BUSINESS_TYPE,
                record.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", record.status())
        );
        return record;
    }

    private MerchantWorkOrderRecord assignWorkOrderOnce(String id, MerchantWorkOrderAssignRequest request) {
        MerchantWorkOrderRecord existing = findWorkOrder(id);
        rejectTerminal(existing);
        if (!"created".equals(existing.status())) {
            throw ApiException.conflict("INVALID_STATE", "can only assign a work order in created status");
        }
        MerchantWorkOrderRecord assigned = repository.saveWorkOrder(existing.assign(
                requireText(request.ownerDepartment(), "owner_department"),
                requireText(request.ownerUserId(), "owner_user_id"),
                Instant.now()
        ));
        repository.saveAction(action(
                id,
                "assigned",
                existing.status(),
                assigned.status(),
                authorizationService.currentActor().id(),
                requireText(request.operatorNote(), "operator_note"),
                evidenceIds(request.evidenceAttachmentIds())
        ));
        auditEventService.recordSuccess(
                "merchant_service.work_order.assigned",
                "merchant.service_work_order.assign",
                BUSINESS_TYPE,
                assigned.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", assigned.status())
        );
        return assigned;
    }

    private MerchantWorkOrderRecord addActionOnce(String id, MerchantWorkOrderActionRequest request) {
        MerchantWorkOrderRecord existing = findWorkOrder(id);
        rejectTerminal(existing);
        String operatorUserId = authorizationService.currentActor().id();
        String operatorNote = requireText(request.operatorNote(), "operator_note");
        String targetStatus = trimToNull(request.targetStatus());
        MerchantWorkOrderRecord saved = existing;
        String actionType = "commented";
        if (targetStatus != null) {
            targetStatus = normalizeAllowed(targetStatus, "target_status", ACTION_TARGET_STATUSES);
            requireValidActionTransition(existing.status(), targetStatus);
            saved = repository.saveWorkOrder(existing.transitionTo(targetStatus, Instant.now()));
            actionType = existing.status().equals(targetStatus) ? "commented" : "status_changed";
        }
        repository.saveAction(action(
                id,
                actionType,
                existing.status(),
                saved.status(),
                operatorUserId,
                operatorNote,
                evidenceIds(request.evidenceAttachmentIds())
        ));
        auditEventService.recordSuccess(
                "merchant_service.work_order.action_added",
                "merchant.service_work_order.process",
                BUSINESS_TYPE,
                saved.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", saved.status())
        );
        return saved;
    }

    private MerchantWorkOrderRecord closeWorkOrderOnce(String id, MerchantWorkOrderCloseRequest request) {
        MerchantWorkOrderRecord existing = findWorkOrder(id);
        rejectTerminal(existing);
        // 工单只能从 resolved 态收口关闭：resolved 是唯一"已处理待关闭"前置态
        // （ALLOWED_ACTION_TRANSITIONS 中 resolved 的后继为空集，唯一出口即 close）。
        // 禁止 created/assigned/processing/pending_merchant 直接 close 跳步。
        if (!"resolved".equals(existing.status())) {
            throw ApiException.conflict("INVALID_STATE", "can only close a work order in resolved status");
        }
        String resolution = requireText(request.resolution(), "resolution");
        validateSatisfactionScore(request.satisfactionScore());
        MerchantWorkOrderRecord closed = repository.saveWorkOrder(existing.close(
                resolution,
                request.satisfactionScore(),
                Instant.now()
        ));
        repository.saveAction(action(
                id,
                "closed",
                existing.status(),
                "closed",
                authorizationService.currentActor().id(),
                defaultNote(request.operatorNote(), "closed"),
                evidenceIds(request.evidenceAttachmentIds())
        ));
        auditEventService.recordSuccess(
                "merchant_service.work_order.closed",
                "merchant.service_work_order.close",
                BUSINESS_TYPE,
                closed.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", closed.status())
        );
        return closed;
    }

    // ================= V264 数据存档与审批后删除范式 =================

    /** 当前操作者 id（存档/删除留痕 + 自审校验）；无鉴权上下文（测试/内部）回退 null。 */
    private String actorId() {
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException unauthenticated) {
            return null;
        }
    }

    /** 存档/取消存档工单（软标记，可逆；列表默认过滤已存档）。取消存档时把 pending 删除申请回退 none。 */
    @Transactional
    public MerchantWorkOrderRecord archiveWorkOrder(String id, boolean archive) {
        MerchantWorkOrderRecord order = findWorkOrder(id);
        checkMerchantOwnership(order.merchantId());
        Instant now = Instant.now();
        MerchantWorkOrderRecord updated = archive
                ? order.withArchive(now, actorId(), now)
                : order.withArchive(null, null, now).withDelete("none", null, null, null, now);
        MerchantWorkOrderRecord saved = repository.saveWorkOrder(updated);
        auditEventService.recordSuccess(archive ? "merchant_service.work_order.archived" : "merchant_service.work_order.unarchived",
                "merchant.service_work_order.close", BUSINESS_TYPE, id, requestId(), null,
                Map.of("archived", archive));
        return saved;
    }

    /** 发起删除申请：须先存档，记 requested_by/at/reason，状态置 pending。 */
    @Transactional
    public MerchantWorkOrderRecord requestDeleteWorkOrder(String id, MerchantWorkOrderDeleteRequest request) {
        MerchantWorkOrderRecord order = findWorkOrder(id);
        checkMerchantOwnership(order.merchantId());
        if (order.archivedAt() == null) {
            throw ApiException.badRequest("WORK_ORDER_NOT_ARCHIVED", "工单必须先存档才能申请删除");
        }
        if ("pending".equals(order.deleteStatus())) {
            throw ApiException.conflict("WORK_ORDER_DELETE_ALREADY_PENDING", "该工单已有待审批的删除申请");
        }
        Instant now = Instant.now();
        String reason = request == null ? null : trimToNull(request.reason());
        MerchantWorkOrderRecord saved = repository.saveWorkOrder(order.withDelete("pending", actorId(), now, reason, now));
        auditEventService.recordSuccess("merchant_service.work_order.delete_requested",
                "merchant.service_work_order.close", BUSINESS_TYPE, id, requestId(), null, Map.of());
        return saved;
    }

    /** 审批删除申请。approved 且无活引用（工单已终态）→ 物理删除；rejected → 回 none。禁自审。 */
    @Transactional
    public MerchantWorkOrderDeleteDecision approveDeleteWorkOrder(String id, MerchantWorkOrderDeleteApprovalRequest request) {
        MerchantWorkOrderRecord order = findWorkOrder(id);
        checkMerchantOwnership(order.merchantId());
        if (!"pending".equals(order.deleteStatus())) {
            throw ApiException.badRequest("WORK_ORDER_DELETE_NOT_PENDING", "该工单没有待审批的删除申请");
        }
        String decision = request == null || request.decision() == null
                ? null : request.decision().trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
        }
        String actor = actorId();
        if (actor != null && actor.equals(order.deleteRequestedBy())) {
            throw ApiException.forbidden("WORK_ORDER_SELF_APPROVE_FORBIDDEN", "删除申请发起人不能审批自己的申请");
        }
        if ("rejected".equals(decision)) {
            repository.saveWorkOrder(order.withDelete("none", null, null, null, Instant.now()));
            auditEventService.recordSuccess("merchant_service.work_order.delete_rejected",
                    "merchant.service_work_order.approve", BUSINESS_TYPE, id, requestId(), null, Map.of());
            return new MerchantWorkOrderDeleteDecision(id, "rejected", false);
        }
        long refs = repository.countWorkOrderReferences(id);
        if (refs > 0) {
            throw ApiException.conflict("WORK_ORDER_IN_USE", "工单未终结（须先关闭或取消），不可删除");
        }
        repository.deleteWorkOrder(id);
        auditEventService.recordSuccess("merchant_service.work_order.delete_approved",
                "merchant.service_work_order.approve", BUSINESS_TYPE, id, requestId(), null, Map.of());
        return new MerchantWorkOrderDeleteDecision(id, "approved", true);
    }

    private MerchantWorkOrderRecord findWorkOrder(String id) {
        return repository.findWorkOrderById(id)
                .orElseThrow(() -> ApiException.notFound("WORK_ORDER_NOT_FOUND", "merchant service work order not found"));
    }

    private void ensureWorkOrderCanChange(String id) {
        MerchantWorkOrderRecord existing = findWorkOrder(id);
        rejectTerminal(existing);
        checkMerchantOwnership(existing.merchantId());
    }

    private void checkMerchantOwnership(String recordMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isEmpty()) {
            return; // platform admin sees all merchants
        }
        if (!scope.get().equals(recordMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "access to another merchant's work order is forbidden");
        }
    }

    private void validateCreateRequest(MerchantWorkOrderCreateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        normalizeAllowed(request.workOrderType(), "work_order_type", TYPES);
        normalizeAllowed(request.priority(), "priority", PRIORITIES);
        requireText(request.title(), "title");
        requireText(request.description(), "description");
        requireText(request.reportedChannel(), "reported_channel");
    }

    private void validateAssignRequest(MerchantWorkOrderAssignRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.ownerDepartment(), "owner_department");
        requireText(request.ownerUserId(), "owner_user_id");
        requireText(request.operatorNote(), "operator_note");
    }

    private void validateActionRequest(MerchantWorkOrderActionRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.operatorNote(), "operator_note");
        String targetStatus = trimToNull(request.targetStatus());
        if (targetStatus != null) {
            normalizeAllowed(targetStatus, "target_status", ACTION_TARGET_STATUSES);
        }
    }

    private void validateCloseRequest(MerchantWorkOrderCloseRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.resolution(), "resolution");
        validateSatisfactionScore(request.satisfactionScore());
    }

    private MerchantWorkOrderActionRecord action(
            String workOrderId,
            String actionType,
            String fromStatus,
            String toStatus,
            String operatorUserId,
            String operatorNote,
            List<String> evidenceAttachmentIds
    ) {
        return new MerchantWorkOrderActionRecord(
                UUID.randomUUID().toString(),
                workOrderId,
                actionType,
                fromStatus,
                toStatus,
                operatorUserId,
                operatorNote,
                evidenceAttachmentIds,
                Instant.now()
        );
    }

    private MerchantWorkOrderRecord executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            String failureEvent,
            String permissionCode,
            String businessId,
            Runnable preValidation,
            Supplier<MerchantWorkOrderRecord> action
    ) {
        try {
            preValidation.run();
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    action,
                    MerchantWorkOrderRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    BUSINESS_TYPE,
                    businessId,
                    requestId(),
                    idempotencyKey,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private void rejectTerminal(MerchantWorkOrderRecord existing) {
        if (TERMINAL_STATUSES.contains(existing.status())) {
            throw ApiException.conflict("WORK_ORDER_CLOSED", "closed or cancelled work order cannot be changed");
        }
    }

    private void requireValidActionTransition(String fromStatus, String toStatus) {
        if (fromStatus.equals(toStatus)) {
            return; // re-affirming the current status is a comment, not a transition
        }
        if (!ALLOWED_ACTION_TRANSITIONS.getOrDefault(fromStatus, Set.of()).contains(toStatus)) {
            throw ApiException.conflict(
                    "MERCHANT_WORK_ORDER_INVALID_TRANSITION",
                    "cannot move work order from " + fromStatus + " to " + toStatus);
        }
    }

    private void validateSatisfactionScore(Integer score) {
        if (score != null && (score < 1 || score > 5)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "satisfaction_score must be between 1 and 5");
        }
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR";
    }
}
