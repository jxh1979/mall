package com.complex.platform.facility;

// 文件版本：V0.05

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityWorkOrderActionRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderConfirmerRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderPolicyRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderReassignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

/** 楼宇运营 S2 维保/物业工单。 */
@Service
public class FacilityWorkOrderService {

    /** ENUM:260、BSM:1377「报修、保洁、投诉、整改、巡检、内部协作工单（内部部门间发起与流转）、其他服务请求」（第十六批第 35 条补 internal_collaboration）。 */
    static final Set<String> TYPES = Set.of("repair", "cleaning", "complaint", "rectification", "inspection",
            "internal_collaboration", "other");
    static final Set<String> PRIORITIES = Set.of("urgent", "high", "normal");
    /** ROOT-03 clients-R8（BSM 第 1379 行）：customer＝消费客人通过客人端投诉，客人即提单方（只经 {@link #createCustomerComplaint} 建单）；
     * 第十八批第 18 条（BSM:1201「超过约定归还时间仍未归还的，系统自动生成一张工单」）：lending_overdue＝借用逾期由系统自动生成（只经 {@link #createLendingOverdue}）。 */
    static final Set<String> SOURCES = Set.of("merchant_self", "platform", "equipment_alarm", "inspection", "customer", "lending_overdue");
    /** 已写处理结论的状态（V166 chk_fwo_status 闭集里完工之后的各态）：客人可对本人投诉评满意度（ROOT-03-R26）。 */
    private static final Set<String> RESOLVED_STATUSES = Set.of("pending_review", "pending_acceptance", "accepted", "closed");
    /**
     * 验收方式（后台按类型×优先级配置）。HP-F02 主管／物业验收＋提单方确认双闸不是可配的验收方式：第十二批操作员裁决第 12 条
     * 「关联设备的报修才两道」——关联了设备的报修单（含设备告警自动生成的）一律两道，没关联设备的普通报修按这里配置的方式（见 {@link #dual}）。
     */
    static final Set<String> ACCEPTANCE_BY = Set.of("handler", "supervisor", "reporter");
    static final Set<String> MERCHANT_SELF_TYPES = Set.of("repair", "cleaning", "complaint", "other");
    static final Set<String> ACTIVE_STATUSES =
            Set.of("pending_assign", "assigned", "processing", "pending_review", "pending_acceptance", "accepted");
    /** 双闸下可做主管验收、提单确认或退回返工的状态（已验收＝主管已验收、待提单方确认）。 */
    private static final Set<String> DUAL_PENDING_STATUSES = Set.of("pending_acceptance", "accepted");
    private static final String PERM = "facility.work_order.manage";
    private static final String BUSINESS_TYPE = "facility_work_order";
    private static final String INSPECTION_PERM = "facility.inspection.manage";
    private static final String INSPECTION_TASK_BUSINESS_TYPE = "facility_inspection_task";

    private final FacilityWorkOrderRepository repository;
    private final FacilityRepository facilityRepository;
    private final FacilityInspectionRepository inspectionRepository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final FacilityAlarmService alarmService;
    private final AccessRepository accessRepository;
    private final IdempotencyService idempotencyService;

    @Autowired
    public FacilityWorkOrderService(FacilityWorkOrderRepository repository, FacilityRepository facilityRepository,
            AuditEventService auditEventService, AuthorizationService authorizationService,
            FacilityInspectionRepository inspectionRepository, FacilityAlarmService alarmService,
            AccessRepository accessRepository, IdempotencyService idempotencyService) {
        this.repository = repository;
        this.facilityRepository = facilityRepository;
        this.inspectionRepository = inspectionRepository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.alarmService = alarmService;
        this.accessRepository = accessRepository;
        this.idempotencyService = idempotencyService;
    }

    public FacilityWorkOrderService(FacilityWorkOrderRepository repository, FacilityRepository facilityRepository,
            AuditEventService auditEventService, AuthorizationService authorizationService,
            FacilityInspectionRepository inspectionRepository) {
        this(repository, facilityRepository, auditEventService, authorizationService, inspectionRepository, null, null,
                null);
    }

    public FacilityWorkOrderService(FacilityWorkOrderRepository repository, FacilityRepository facilityRepository,
            AuditEventService auditEventService, AuthorizationService authorizationService) {
        this(repository, facilityRepository, auditEventService, authorizationService, null, null, null, null);
    }

    /** 测试与内部装配用：带在岗判定所需的账号仓储（无接警服务）。 */
    public FacilityWorkOrderService(FacilityWorkOrderRepository repository, FacilityRepository facilityRepository,
            AuditEventService auditEventService, AuthorizationService authorizationService,
            AccessRepository accessRepository) {
        this(repository, facilityRepository, auditEventService, authorizationService, null, null, accessRepository,
                null);
    }

    public PageResponse<FacilityWorkOrderRecord> list() {
        AccessActor actor = requireScopeActor();
        List<FacilityWorkOrderRecord> items = authorizationService.isOutsourcedVendor(actor)
                ? repository.listAssignedTo(actor.id(), authorizationService.requireProjectScope(actor))
                : repository.list();
        return new PageResponse<>(items, 1, 500, items.size());
    }

    /**
     * BYOD 一期(2026-07-02):员工自有手机「我的工单」——凭 view_assigned 只看流转到自己的,不见别人；第十二批操作员裁决第 13 条：
     * 一并列出「待我提单确认」的单（本人是提单确认承接人、走两道、提单确认未做、待验收或已验收），确认与退回返工走 /review、/rework。
     */
    public PageResponse<FacilityWorkOrderRecord> listAssignedToMe() {
        authorizationService.requirePermission("facility.work_order.view_assigned");
        AccessActor actor = authorizationService.currentActor();
        List<FacilityWorkOrderRecord> items = repository.listAssignedToMe(actor.id());
        return new PageResponse<>(items, 1, 200, items.size());
    }

    public PageResponse<FacilityWorkOrderRecord> listOverdue() {
        authorizationService.requirePlatformScope();
        List<FacilityWorkOrderRecord> items = repository.listOverdue(Instant.now());
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public FacilityWorkOrderRecord findById(String id) {
        AccessActor actor = requireScopeActor();
        FacilityWorkOrderRecord record = load(id);
        enforceAssignedScope(actor, record);
        return record;
    }

    @Transactional
    public FacilityWorkOrderRecord create(FacilityWorkOrderRequest request) {
        authorizationService.requirePlatformScope();
        FacilityWorkOrderRequest r = require(request);
        if ("customer".equals(trimToNull(r.source()))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "客人投诉只能由客人在官方 App、小程序提交");
        }
        if ("lending_overdue".equals(trimToNull(r.source()))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "借用逾期工单由系统自动生成，不能人工新建");
        }
        // 第十二批操作员裁决第 15 条「提单员工就是承接人」：员工建单的提单员工＝在后台点「新建工单」的当前登录员工，不取客户端传的 reporter_id。
        if ("staff".equals(trimToNull(r.reporterType()))) {
            r = new FacilityWorkOrderRequest(r.workOrderType(), r.priority(), r.title(), r.description(), r.source(), r.spaceId(),
                    r.equipmentId(), authorizationService.currentActor().id(), r.reporterType(), r.projectId(), r.merchantId(),
                    r.idempotencyKey());
        }
        return createInternal(r, r.idempotencyKey(), null);
    }

    @Transactional
    public FacilityWorkOrderRecord assign(String id, FacilityWorkOrderAssignRequest request) {
        try {
            authorizationService.requirePlatformScope();
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
            }
            FacilityWorkOrderRecord cur = load(id);
            requireStatus(cur, "pending_assign", "只有待派单的工单才能派单");
            if ("equipment_alarm".equals(cur.source()) && alarmService != null) {
                alarmService.requireTakenOverBeforeDispatch(cur.id());
            }
            Instant now = Instant.now();
            FacilityWorkOrderRecord assigned = new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(),
                    cur.priority(), "assigned", cur.title(), cur.description(), cur.source(), cur.spaceId(),
                    cur.equipmentId(), cur.reporterId(), cur.reporterType(), requireText(request.assigneeId(), "assignee_id"),
                    cur.responseDueAt(), cur.handleDueAt(), now, cur.respondedAt(), cur.completedAt(), cur.verifiedAt(),
                    cur.verifierId(), cur.satisfaction(), cur.overtime(), cur.resolution(), cur.projectId(),
                    cur.merchantId(), cur.createdAt(), now, cur.confirmerId(), cur.reporterConfirmedAt(),
                    cur.reporterConfirmedBy());
            repository.updateWorkOrder(assigned);
            audit("facility.work_order_assigned", assigned.id(), request.idempotencyKey(),
                    Map.of("status", assigned.status(), "assignee_id", assigned.assigneeId()));
            return assigned;
        } catch (RuntimeException exception) {
            recordFailure("facility.work_order_assign_failed", id, request == null ? null : request.idempotencyKey(), exception);
            throw exception;
        }
    }

    /**
     * 第十二批操作员裁决第 9 条「补转派」：已派单或处理中时，由物业主管改派给他人；处理时限不重算（只换处理人，状态、响应／处理时限、
     * 派单时间与超时标记都不动，超时照常升级）；留痕原处理人、新处理人和原因（原因必填）。
     */
    @Transactional
    public FacilityWorkOrderRecord reassign(String id, FacilityWorkOrderReassignRequest request) {
        try {
            authorizationService.requirePlatformScope();
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
            }
            return idempotencyService.execute("facilityWorkOrderReassign:" + id, request.idempotencyKey(), request, () -> {
                String assignee = requireText(request.assigneeId(), "assignee_id");
                String reason = requireText(request.reason(), "reason");
                FacilityWorkOrderRecord cur = loadForUpdate(id);
                if (!Set.of("assigned", "processing").contains(cur.status())) {
                    throw ApiException.badRequest("INVALID_STATE", "只有已派单或处理中的工单才能转派");
                }
                if (assignee.equals(cur.assigneeId())) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "转派须改派给原处理人以外的人员");
                }
                FacilityWorkOrderRecord moved = new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(),
                        cur.priority(), cur.status(), cur.title(), cur.description(), cur.source(), cur.spaceId(),
                        cur.equipmentId(), cur.reporterId(), cur.reporterType(), assignee, cur.responseDueAt(),
                        cur.handleDueAt(), cur.assignedAt(), cur.respondedAt(), cur.completedAt(), cur.verifiedAt(),
                        cur.verifierId(), cur.satisfaction(), cur.overtime(), cur.resolution(), cur.projectId(),
                        cur.merchantId(), cur.createdAt(), Instant.now(), cur.confirmerId(), cur.reporterConfirmedAt(),
                        cur.reporterConfirmedBy());
                repository.updateWorkOrder(moved);
                audit("facility.work_order_reassigned", moved.id(), request.idempotencyKey(), Map.of("status", moved.status(),
                        "previous_assignee_id", cur.assigneeId(), "assignee_id", assignee, "reason", reason));
                return moved;
            }, FacilityWorkOrderRecord.class);
        } catch (RuntimeException exception) {
            recordFailure("facility.work_order_reassign_failed", id, request == null ? null : request.idempotencyKey(),
                    exception);
            throw exception;
        }
    }

    @Transactional
    public FacilityWorkOrderRecord start(String id, FacilityWorkOrderActionRequest request) {
        AccessActor actor = requireScopeActor();
        FacilityWorkOrderActionRequest r = requireAction(request);
        // 与转派同锁工单行：转派与原处理人「开始」「完成」并发时串行，后者按最新处理人与状态校验，不以旧快照整行写回覆盖转派。
        FacilityWorkOrderRecord cur = loadForUpdate(id);
        enforceAssignedScope(actor, cur);
        requireStatus(cur, "assigned", "只有已派单的工单才能开始处理");
        Instant now = Instant.now();
        FacilityWorkOrderRecord processing = new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(),
                cur.priority(), "processing", cur.title(), cur.description(), cur.source(), cur.spaceId(),
                cur.equipmentId(), cur.reporterId(), cur.reporterType(), cur.assigneeId(), cur.responseDueAt(),
                cur.handleDueAt(), cur.assignedAt(), now, cur.completedAt(), cur.verifiedAt(), cur.verifierId(),
                cur.satisfaction(), cur.overtime(), cur.resolution(), cur.projectId(), cur.merchantId(),
                cur.createdAt(), now, cur.confirmerId(), cur.reporterConfirmedAt(),
                cur.reporterConfirmedBy());
        repository.updateWorkOrder(processing);
        audit("facility.work_order_started", processing.id(), r.idempotencyKey(), metadata(processing, r.note()));
        return processing;
    }

    @Transactional
    public FacilityWorkOrderRecord complete(String id, FacilityWorkOrderActionRequest request) {
        AccessActor actor = requireScopeActor();
        FacilityWorkOrderActionRequest r = requireAction(request);
        FacilityWorkOrderRecord cur = loadForUpdate(id);
        enforceAssignedScope(actor, cur);
        requireStatus(cur, "processing", "只有处理中的工单才能完成");
        // 双闸（关联设备的报修）完工即进待验收，不看配置的验收方式；每次完工开始新一轮验收：之前的主管验收、提单确认与评分作废
        // （挂起→恢复→再完工同此）。
        boolean fresh = dual(cur);
        String nextStatus = fresh ? "pending_acceptance" : switch (policy(cur.workOrderType(), cur.priority()).acceptanceBy()) {
            case "handler" -> "closed";
            case "supervisor" -> "pending_acceptance";
            case "reporter" -> "pending_review";
            default -> throw ApiException.badRequest("VALIDATION_FAILED", "acceptance_by is invalid");
        };
        Instant now = Instant.now();
        FacilityWorkOrderRecord completed = new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(),
                cur.priority(), nextStatus, cur.title(), cur.description(), cur.source(), cur.spaceId(),
                cur.equipmentId(), cur.reporterId(), cur.reporterType(), cur.assigneeId(), cur.responseDueAt(),
                cur.handleDueAt(), cur.assignedAt(), cur.respondedAt(), now, fresh ? null : cur.verifiedAt(),
                fresh ? null : cur.verifierId(), fresh ? null : cur.satisfaction(), cur.overtime(),
                requireText(r.resolution(), "resolution"), cur.projectId(), cur.merchantId(), cur.createdAt(), now,
                cur.confirmerId(), fresh ? null : cur.reporterConfirmedAt(), fresh ? null : cur.reporterConfirmedBy());
        recordCustomerFeedbackOnClose(cur, completed, r);
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
        FacilityWorkOrderRecord cur = loadForUpdate(id);
        requireStatus(cur, "pending_acceptance", "只有待验收的工单才能验收");
        Instant now = Instant.now();
        // 双闸：主管验收只记验收事实（不评满意度，满意度由提单方评）；提单方已确认才关闭，否则为「已验收」待提单确认。
        FacilityWorkOrderRecord closed = dual(cur)
                ? withConfirmations(cur, cur.reporterConfirmedAt() != null ? "closed" : "accepted", cur.completedAt(),
                        now, actorId(), cur.satisfaction(), cur.reporterConfirmedAt(), cur.reporterConfirmedBy(), now)
                : verifyClosed(cur, r);
        recordCustomerFeedbackOnClose(cur, closed, r);
        repository.updateWorkOrder(closed);
        resetEquipmentIfClosed(cur, closed);
        audit("facility.work_order_accepted", closed.id(), r.idempotencyKey(), metadata(closed, r.note()));
        return closed;
    }

    @Transactional
    public FacilityWorkOrderRecord review(String id, FacilityWorkOrderActionRequest request) {
        try {
            authorizationService.requirePlatformScope();
            FacilityWorkOrderActionRequest r = requireAction(request);
            validateSatisfaction(r.satisfaction());
            FacilityWorkOrderRecord cur = loadForUpdate(id);
            if (!confirmerSelf(cur)) {
                authorizationService.requirePermission(PERM);
            }
            FacilityWorkOrderRecord closed;
            if (dual(cur)) {
                // 双闸的提单方确认：主管验收前后都可确认；只确认一次；两项齐全才关闭。
                if (!DUAL_PENDING_STATUSES.contains(cur.status())) {
                    throw ApiException.badRequest("INVALID_STATE", "只有待验收或已验收的工单才能做提单确认");
                }
                if (cur.reporterConfirmedAt() != null) {
                    throw ApiException.badRequest("INVALID_STATE", "提单方已确认");
                }
                if (confirmerBound(cur)) {
                    requireConfirmer(cur);
                }
                Instant now = Instant.now();
                closed = withConfirmations(cur, cur.verifiedAt() != null ? "closed" : cur.status(), cur.completedAt(),
                        cur.verifiedAt(), cur.verifierId(), r.satisfaction(), now, actorId(), now);
            } else {
                requireStatus(cur, "pending_review", "只有待审核的工单才能审核");
                closed = verifyClosed(cur, r);
            }
            recordCustomerFeedbackOnClose(cur, closed, r);
            repository.updateWorkOrder(closed);
            resetEquipmentIfClosed(cur, closed);
            audit("facility.work_order_reviewed", closed.id(), r.idempotencyKey(), metadata(closed, r.note()));
            return closed;
        } catch (RuntimeException exception) {
            recordFailure("facility.work_order_review_failed", id, request == null ? null : request.idempotencyKey(), exception);
            throw exception;
        }
    }

    /**
     * 第十二批操作员裁决第 2 条「退回原维修人返工」：主管验收或提单确认不通过，填原因后回「处理中」由原维修人继续修；
     * 另一方已做的确认作废，修好后两方重新确认；时效不重算、超时照常升级（处理时限与超时标记不动）；换人用既有转派。
     * 第十二批操作员裁决第 16 条「只许承接人本人（推荐）」：绑定承接人的两道单（{@link #confirmerBound}）主管已验收（accepted）后
     * 只剩提单确认这一道，退回也只许承接人本人（同提单确认的承接人校验，旁人 403 CONFIRMER_MISMATCH）；待验收时主管验收不通过的退回照旧。
     */
    @Transactional
    public FacilityWorkOrderRecord rework(String id, FacilityWorkOrderActionRequest request) {
        try {
            authorizationService.requirePlatformScope();
            FacilityWorkOrderActionRequest r = requireAction(request);
            return idempotencyService.execute("facilityWorkOrderRework:" + id, r.idempotencyKey(), r, () -> {
                String reason = trimToNull(r.note());
                if (reason == null) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "验收或提单确认不通过须填写原因");
                }
                FacilityWorkOrderRecord cur = loadForUpdate(id);
                if (confirmerSelf(cur) || ("accepted".equals(cur.status()) && confirmerBound(cur))) {
                    requireConfirmer(cur);
                } else {
                    authorizationService.requirePermission(PERM);
                }
                if (!DUAL_PENDING_STATUSES.contains(cur.status()) || !dual(cur)) {
                    throw ApiException.badRequest("INVALID_STATE", "只有待验收或已验收的大型维修工单才能退回返工");
                }
                Instant now = Instant.now();
                FacilityWorkOrderRecord reworked = withConfirmations(cur, "processing", null, null, null, null, null, null, now);
                repository.updateWorkOrder(reworked);
                audit("facility.work_order_rework_requested", reworked.id(), r.idempotencyKey(), metadata(reworked, reason));
                return reworked;
            }, FacilityWorkOrderRecord.class);
        } catch (RuntimeException exception) {
            recordFailure("facility.work_order_rework_failed", id, request == null ? null : request.idempotencyKey(),
                    exception);
            throw exception;
        }
    }

    /**
     * HP-F02（BSM L1385「自动生成的大型维修工单，由该设备或点位的在岗责任人承接提单确认；责任人不在岗或本人负责此次维修时，
     * 由物业主管指定其他人员并留痕」）：仅在现承接人缺失、不在岗或就是本次维修人时可指定；被指定人须为本项目在岗员工且不是维修人。
     * 员工手工建的关联设备报修单同此（第十二批操作员裁决第 15 条「提单人就是本次维修人时，由物业主管指定他人并留痕」）。
     */
    @Transactional
    public FacilityWorkOrderRecord designateConfirmer(String id, FacilityWorkOrderConfirmerRequest request) {
        try {
            authorizationService.requirePlatformScope();
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
            }
            return idempotencyService.execute("facilityWorkOrderConfirmer:" + id, request.idempotencyKey(), request, () -> {
                FacilityWorkOrderRecord cur = loadForUpdate(id);
                if (!confirmerBound(cur) || "closed".equals(cur.status()) || cur.reporterConfirmedAt() != null) {
                    throw ApiException.badRequest("INVALID_STATE",
                            "只有未关闭且未做提单确认的设备告警自动工单或员工手工建的关联设备报修单才能指定提单确认人");
                }
                String previous = cur.confirmerId();
                String reason = previous == null ? "missing"
                        : previous.equals(cur.assigneeId()) ? "is_repairer"
                        : !onDuty(previous, cur.projectId()) ? "not_on_duty" : null;
                if (reason == null) {
                    throw ApiException.conflict("CONFIRMER_DESIGNATION_NOT_ALLOWED", "提单确认承接人在岗且不是本次维修人，由其承接提单确认，不需另行指定");
                }
                String confirmer = requireText(request.confirmerId(), "confirmer_id");
                if (confirmer.equals(cur.assigneeId()) || !onDuty(confirmer, cur.projectId())) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "提单确认人须为本项目在岗员工，且不是本次维修人");
                }
                FacilityWorkOrderRecord designated = new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(),
                        cur.priority(), cur.status(), cur.title(), cur.description(), cur.source(), cur.spaceId(),
                        cur.equipmentId(), cur.reporterId(), cur.reporterType(), cur.assigneeId(), cur.responseDueAt(),
                        cur.handleDueAt(), cur.assignedAt(), cur.respondedAt(), cur.completedAt(), cur.verifiedAt(),
                        cur.verifierId(), cur.satisfaction(), cur.overtime(), cur.resolution(), cur.projectId(),
                        cur.merchantId(), cur.createdAt(), Instant.now(), confirmer, null, null);
                repository.updateWorkOrder(designated);
                Map<String, Object> metadata = new LinkedHashMap<>();
                if (previous != null) {
                    metadata.put("previous_confirmer_id", previous);
                }
                metadata.put("confirmer_id", confirmer);
                metadata.put("reason", reason);
                if (trimToNull(request.note()) != null) {
                    metadata.put("note", trimToNull(request.note()));
                }
                audit("facility.work_order_confirmer_designated", designated.id(), request.idempotencyKey(), metadata);
                return designated;
            }, FacilityWorkOrderRecord.class);
        } catch (RuntimeException exception) {
            recordFailure("facility.work_order_confirmer_designate_failed", id,
                    request == null ? null : request.idempotencyKey(), exception);
            throw exception;
        }
    }

    @Transactional
    public FacilityWorkOrderRecord suspend(String id, FacilityWorkOrderActionRequest request) {
        authorizationService.requirePlatformScope();
        FacilityWorkOrderActionRequest r = requireAction(request);
        FacilityWorkOrderRecord cur = loadForUpdate(id);
        if (!ACTIVE_STATUSES.contains(cur.status())) {
            throw ApiException.badRequest("INVALID_STATE", "只有进行中的工单才能挂起");
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
        FacilityWorkOrderRecord cur = loadForUpdate(id);
        requireStatus(cur, "suspended", "只有已挂起的工单才能恢复");
        // 待派单时挂起的（没有处理人）恢复回待派单：派单仍只走 assign 及其告警接手闸（BSM L1417「接手后派相应维修人员」），
        // 不会出现没有处理人的处理中工单。
        FacilityWorkOrderRecord resumed = withStatus(cur, cur.assigneeId() == null ? "pending_assign" : "processing", Instant.now());
        repository.updateWorkOrder(resumed);
        audit("facility.work_order_resumed", resumed.id(), r.idempotencyKey(), metadata(resumed, r.note()));
        return resumed;
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
                    cur.createdAt(), now, cur.confirmerId(), cur.reporterConfirmedAt(),
                cur.reporterConfirmedBy()));
        }
        auditEventService.recordSuccess("facility.work_order_overdue_marked", PERM, BUSINESS_TYPE, "all",
                null, idempotencyKey, Map.of("count", overdue.size()));
        return overdue.size();
    }

    @Transactional
    public FacilityWorkOrderPolicyRecord savePolicy(FacilityWorkOrderPolicyRequest request) {
        authorizationService.requirePlatformScope();
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
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

    // ponytail: 刻意同步 @EventListener，跑在发布者(changeStatus)事务内——告警、自动工单与接警单 either-both-or-neither，
    // 避免孤儿工单/静默丢失，失败可见可重试。升级路径(若要求告警必须独立于工单创建留存)：
    // 改 @TransactionalEventListener(AFTER_COMMIT) + @Transactional(REQUIRES_NEW) + 失败落 outbox 重试。
    @EventListener
    @Transactional
    public void onEquipmentAlarmed(FacilityEquipmentAlarmedEvent event) {
        authorizationService.requirePlatformScope();
        // HP-F01：优先级取上报时当场选的告警优先级；项目与位置取设备台账（当班接警按项目解析）。
        FacilityWorkOrderRequest request = new FacilityWorkOrderRequest("repair", event.priority(),
                "设备告警: " + requireText(event.equipmentName(), "equipment_name"), event.note(),
                "equipment_alarm", event.spaceId(), event.equipmentId(), "system", "system", event.projectId(), null, null);
        // HP-F02：提单确认承接人取设备（点位）责任人；提单方仍记为系统。
        FacilityWorkOrderRecord workOrder = createInternal(request, null, event.responsibleUserId());
        if (alarmService != null) {
            alarmService.open(event, workOrder);
        }
    }

    // ponytail: 同上——刻意同步同事务原子(巡检异常与自动整改工单 either-both-or-neither)；
    // 独立留存需求出现时按 onEquipmentAlarmed 注释的升级路径(AFTER_COMMIT + REQUIRES_NEW + outbox)。
    @EventListener
    @Transactional
    public void onInspectionAbnormal(FacilityInspectionAbnormalEvent event) {
        authorizationService.requirePlatformScope();
        String taskId = requireText(event.taskId(), "task_id");
        FacilityWorkOrderRequest request = new FacilityWorkOrderRequest("rectification", "normal",
                "巡检异常整改: " + requireText(event.title(), "title"), event.abnormalNote(),
                "inspection", event.spaceId(), null, trimToNull(event.inspectorId()), "staff",
                event.projectId(), null, null);
        FacilityWorkOrderRecord workOrder = createInternal(request, null, null);
        if (inspectionRepository != null) {
            inspectionRepository.updateTaskWorkOrderId(taskId, workOrder.id(), Instant.now());
            auditEventService.recordSuccess("facility.inspection_work_order_linked", INSPECTION_PERM,
                    INSPECTION_TASK_BUSINESS_TYPE, taskId, null, null, Map.of("work_order_id", workOrder.id()));
        }
    }

    /**
     * ROOT-03 clients-R8（BSM 第 869 行「客人可在官方 App、小程序提交投诉」；第 1379 行「客人（消费客人通过客人端投诉，客人即提单方……）」）：
     * 客人本人投诉建单——类型投诉、来源 customer、提单方＝客人本人，时限按「投诉×普通」策略。客人会话由调用方校验，不走员工范围闸。
     */
    public FacilityWorkOrderRecord createCustomerComplaint(String customerId, String projectId, String title, String description,
            String idempotencyKey) {
        return createInternal(new FacilityWorkOrderRequest("complaint", "normal", title, description, "customer", null, null,
                customerId, "customer", projectId, null, idempotencyKey), idempotencyKey, null);
    }

    /**
     * 第十八批第 18 条（BSM:1201「超过约定归还时间仍未归还的，系统自动生成一张工单并通知借出方主管，按与其他工单相同的超时升级路径继续催办」；
     * BSM:1372「都进统一工单」；BSM:1385「工单由客户关怀部负责处理」）：借用逾期系统建单——类型其他、普通优先级（时限按「其他×普通」策略）、
     * 来源 lending_overdue、提单方系统、待派单，记借出方与其项目。系统作业没有登录人，不走员工范围闸；超时由现有超时清单与标记处理。
     */
    @Transactional
    public FacilityWorkOrderRecord createLendingOverdue(String projectId, String lenderMerchantId, String title, String description) {
        return createInternal(new FacilityWorkOrderRequest("other", "normal", title, description, "lending_overdue", null, null,
                "system", "system", projectId, lenderMerchantId, null), null, null);
    }

    /**
     * 第 63 条（BSM:800「安检环节不产生按人记录，仅记设备状态、故障事件与维修工单留痕……留痕与溯源以设备状态、故障事件与维修工单为终点」）：
     * 安检 X 光设备人工登记故障即开一张维修工单——类型报修、普通优先级、来源平台、提单方为登记故障的员工、记设备所在空间与项目、待派单。
     * X 光设备在设备管理台账（terminal_devices，PGM:618、PGM:623），不在楼宇机电台账，不填 equipment_id，按普通报修走既有派单与验收。
     */
    @Transactional
    public FacilityWorkOrderRecord createSecurityXrayFault(String projectId, String spaceId, String deviceCode, String reason,
            String reporterId) {
        return createInternal(new FacilityWorkOrderRequest("repair", "normal", "安检 X 光设备故障：" + deviceCode, reason,
                "platform", spaceId, null, reporterId, "staff", projectId, null, null), null, null);
    }

    /** 借用物全部归还或实物处置完成：系统结束该逾期工单（已关闭的不动），写处理结论并留痕。 */
    @Transactional
    public void closeLendingOverdue(String workOrderId, String resolution) {
        FacilityWorkOrderRecord cur = loadForUpdate(workOrderId);
        if ("closed".equals(cur.status())) {
            return;
        }
        Instant now = Instant.now();
        FacilityWorkOrderRecord closed = new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(), cur.priority(), "closed",
                cur.title(), cur.description(), cur.source(), cur.spaceId(), cur.equipmentId(), cur.reporterId(),
                cur.reporterType(), cur.assigneeId(), cur.responseDueAt(), cur.handleDueAt(), cur.assignedAt(),
                cur.respondedAt(), cur.completedAt(), cur.verifiedAt(), cur.verifierId(), cur.satisfaction(), cur.overtime(),
                resolution, cur.projectId(), cur.merchantId(), cur.createdAt(), now, cur.confirmerId(), cur.reporterConfirmedAt(),
                cur.reporterConfirmedBy());
        repository.updateWorkOrder(closed);
        audit("facility.work_order_closed_by_system", closed.id(), null, Map.of("status", "closed", "source", closed.source()));
    }

    /** clients-R8 本人投诉列表：只返回提单方为本人的客人投诉。 */
    public List<FacilityWorkOrderRecord> listCustomerComplaints(String customerId) {
        return repository.listCustomerComplaints(customerId);
    }

    /**
     * ROOT-03-R26（BSM 第 1385 行「满意度在回访/验收时由提单方评分……客人投诉由客人自行在客人端提交，客人即为提单方」）：客人对本人投诉评
     * 1～5 分；已写处理结论后才可评，同单只评一次；不作为关闭条件（关闭仍由客户关怀部记录方案与客人反馈）。
     */
    public FacilityWorkOrderRecord rateByCustomer(String customerId, String workOrderId, Integer score) {
        FacilityWorkOrderRecord cur = loadForUpdate(workOrderId);
        if (!"customer".equals(cur.source()) || !Objects.equals(customerId, cur.reporterId())) {
            throw ApiException.forbidden("CUSTOMER_COMPLAINT_FORBIDDEN", "该投诉不属于当前客人");
        }
        if (score == null || score < 1 || score > 5) {
            throw ApiException.badRequest("VALIDATION_FAILED", "满意度评分必须在 1 到 5 之间");
        }
        if (!RESOLVED_STATUSES.contains(cur.status())) {
            throw ApiException.conflict("COMPLAINT_NOT_RESOLVED", "投诉尚未给出处理结论，暂不能评价");
        }
        if (cur.satisfaction() != null) {
            throw ApiException.conflict("FACILITY_SATISFACTION_ALREADY_RATED", "该投诉已评价");
        }
        FacilityWorkOrderRecord rated = withConfirmations(cur, cur.status(), cur.completedAt(), cur.verifiedAt(),
                cur.verifierId(), score, cur.reporterConfirmedAt(), cur.reporterConfirmedBy(), Instant.now());
        repository.updateWorkOrder(rated);
        return rated;
    }

    /**
     * clients-R8（BSM 第 1379 行、ENUM 第 262 行「消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭」）：客人投诉转为关闭时
     * 请求须带客人反馈并落库；不要求客人在端上确认。
     */
    private void recordCustomerFeedbackOnClose(FacilityWorkOrderRecord before, FacilityWorkOrderRecord after,
            FacilityWorkOrderActionRequest request) {
        if (!"customer".equals(before.source()) || !"closed".equals(after.status())) {
            return;
        }
        String feedback = trimToNull(request.customerFeedback());
        if (feedback == null) {
            throw ApiException.conflict("COMPLAINT_CUSTOMER_FEEDBACK_REQUIRED", "关闭客人投诉须记录客人反馈");
        }
        repository.saveCustomerFeedback(after.id(), feedback);
    }

    private FacilityWorkOrderRecord createInternal(FacilityWorkOrderRequest request, String idempotencyKey,
            String confirmerId) {
        String type = normalizeAllowed(request.workOrderType(), "work_order_type", TYPES);
        String equipmentId = trimToNull(request.equipmentId());
        String projectId = trimToNull(request.projectId());
        // 第十二批操作员裁决第 15 条「和告警单一样，提单员工就是承接人」：员工手工建的关联设备报修单（两道），提单员工记为提单确认
        // 承接人；承接人在岗按工单项目判，后台建单不带项目时同告警单取设备台账的项目。
        if (confirmerId == null && "repair".equals(type) && equipmentId != null && "staff".equals(trimToNull(request.reporterType()))) {
            confirmerId = request.reporterId();
            if (projectId == null) {
                projectId = facilityRepository.findEquipmentById(equipmentId).map(FacilityEquipmentRecord::projectId).orElse(null);
            }
        }
        String priority = normalizeAllowed(defaultValue(request.priority(), "normal"), "priority", PRIORITIES);
        String source = normalizeAllowed(defaultValue(request.source(), "platform"), "source", SOURCES);
        if ("merchant_self".equals(source) && !MERCHANT_SELF_TYPES.contains(type)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "商户自助只能创建报修、保洁、投诉或其他类工单");
        }
        FacilityWorkOrderPolicyRecord policy = policy(type, priority);
        Instant now = Instant.now();
        FacilityWorkOrderRecord rec = new FacilityWorkOrderRecord("fwo_" + UUID.randomUUID(),
                "FWO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT),
                type, priority, "pending_assign", requireText(request.title(), "title"), request.description(),
                source, trimToNull(request.spaceId()), equipmentId,
                trimToNull(request.reporterId()), trimToNull(request.reporterType()), null,
                now.plus(policy.responseMinutes(), ChronoUnit.MINUTES),
                now.plus(policy.handleMinutes(), ChronoUnit.MINUTES), null, null, null, null, null, null,
                false, null, projectId, trimToNull(request.merchantId()), now, now,
                trimToNull(confirmerId), null, null);
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
                cur.respondedAt(), cur.completedAt(), now, actorId(),
                // ROOT-03-R26（BSM 第 1385 行「满意度在回访/验收时由提单方评分」）：客人投诉的满意度只由客人本人评，员工关闭不改写。
                "customer".equals(cur.source()) ? cur.satisfaction() : r.satisfaction(), cur.overtime(),
                cur.resolution(), cur.projectId(), cur.merchantId(), cur.createdAt(), now, cur.confirmerId(), cur.reporterConfirmedAt(),
                cur.reporterConfirmedBy());
    }

    private FacilityWorkOrderRecord withStatus(FacilityWorkOrderRecord cur, String status, Instant now) {
        return new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(), cur.priority(), status,
                cur.title(), cur.description(), cur.source(), cur.spaceId(), cur.equipmentId(), cur.reporterId(),
                cur.reporterType(), cur.assigneeId(), cur.responseDueAt(), cur.handleDueAt(), cur.assignedAt(),
                cur.respondedAt(), cur.completedAt(), cur.verifiedAt(), cur.verifierId(), cur.satisfaction(),
                cur.overtime(), cur.resolution(), cur.projectId(), cur.merchantId(), cur.createdAt(), now, cur.confirmerId(), cur.reporterConfirmedAt(),
                cur.reporterConfirmedBy());
    }

    /** 只改验收/确认相关事实与状态的副本（其余字段原样，含处理时限与超时标记）。 */
    private FacilityWorkOrderRecord withConfirmations(FacilityWorkOrderRecord cur, String status, Instant completedAt,
            Instant verifiedAt, String verifierId, Integer satisfaction, Instant reporterConfirmedAt,
            String reporterConfirmedBy, Instant now) {
        return new FacilityWorkOrderRecord(cur.id(), cur.code(), cur.workOrderType(), cur.priority(), status,
                cur.title(), cur.description(), cur.source(), cur.spaceId(), cur.equipmentId(), cur.reporterId(),
                cur.reporterType(), cur.assigneeId(), cur.responseDueAt(), cur.handleDueAt(), cur.assignedAt(),
                cur.respondedAt(), completedAt, verifiedAt, verifierId, satisfaction, cur.overtime(), cur.resolution(),
                cur.projectId(), cur.merchantId(), cur.createdAt(), now, cur.confirmerId(), reporterConfirmedAt,
                reporterConfirmedBy);
    }

    /** 第十二批操作员裁决第 12 条「关联设备的报修才两道」：关联了设备的报修单（含设备告警自动生成的）走主管验收＋提单方确认。 */
    private boolean dual(FacilityWorkOrderRecord cur) {
        return "repair".equals(cur.workOrderType()) && !isBlank(cur.equipmentId());
    }

    /**
     * 只许提单确认承接人本人做提单确认的两道单：设备告警自动工单（BSM L1385「由该设备或点位的在岗责任人承接提单确认」），
     * 以及员工手工建的关联设备报修单（第十二批操作员裁决第 15 条「和告警单一样，提单员工就是承接人」）。
     */
    private boolean confirmerBound(FacilityWorkOrderRecord cur) {
        return dual(cur) && ("equipment_alarm".equals(cur.source()) || "staff".equals(cur.reporterType()));
    }

    /**
     * 本人即上述两道单的提单确认承接人：凭承接身份做提单确认或退回返工，不需另持 facility.work_order.manage；其余调用方须持该权限。
     */
    private boolean confirmerSelf(FacilityWorkOrderRecord cur) {
        return confirmerBound(cur) && cur.confirmerId() != null && cur.confirmerId().equals(actorId());
    }

    /** 承接人确认的两道单的提单确认：承接人须在岗、不是本次维修人，且由其本人确认。 */
    private void requireConfirmer(FacilityWorkOrderRecord cur) {
        String confirmer = cur.confirmerId();
        if (confirmer == null || confirmer.equals(cur.assigneeId()) || !onDuty(confirmer, cur.projectId())) {
            throw ApiException.conflict("CONFIRMER_DESIGNATION_REQUIRED",
                    "提单确认承接人不在岗或本人负责此次维修，须由物业主管指定提单确认人");
        }
        if (!confirmer.equals(actorId())) {
            throw ApiException.forbidden("CONFIRMER_MISMATCH", "只能由提单确认承接人确认");
        }
    }

    /**
     * 「在岗」沿用仓内口径（StaffHandoverService、EnergyWorkflowRepository.activeStaff）：本项目有效员工账号且停岗/离职未生效。
     * 休息、不当班不算不在岗。无账号仓储时按不在岗处理（fail-closed）。
     */
    private boolean onDuty(String userId, String projectId) {
        if (accessRepository == null || userId == null) {
            return false;
        }
        return accessRepository.findUserById(userId)
                .filter(u -> "staff".equals(u.accountType()) && "active".equals(u.status())
                        && Objects.equals(u.organizationId(), projectId))
                .isPresent() && !accessRepository.isEmploymentBusinessBlocked(userId, Instant.now());
    }

    private void recordFailure(String event, String id, String idempotencyKey, RuntimeException error) {
        try {
            auditEventService.recordFailure(event, PERM, BUSINESS_TYPE, id, null, idempotencyKey,
                    error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
        } catch (RuntimeException auditError) {
            error.addSuppressed(auditError);
        }
    }

    private void resetEquipmentIfClosed(FacilityWorkOrderRecord before, FacilityWorkOrderRecord after) {
        if ("closed".equals(before.status()) || !"closed".equals(after.status()) || isBlank(after.equipmentId())) {
            return;
        }
        // HP-F03：维修中经审批报废的设备，工单关闭也不改回在用。
        facilityRepository.findEquipmentById(after.equipmentId()).filter(e -> !"retired".equals(e.status()))
                .ifPresent(e -> facilityRepository.updateEquipment(
                new FacilityEquipmentRecord(e.id(), e.name(), e.equipmentType(), e.spaceId(), e.brand(), e.model(),
                        e.vendor(), e.responsibleUserId(), "normal", e.maintenanceCycleDays(), e.lastMaintenanceAt(),
                        e.nextMaintenanceDue(), e.installDate(), e.warrantyUntil(), null, null, null,
                        e.projectId(), e.merchantId(), e.createdAt(), Instant.now())));
    }

    private FacilityWorkOrderRecord load(String id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "设施工单不存在或已被删除"));
    }

    private FacilityWorkOrderRecord loadForUpdate(String id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "设施工单不存在或已被删除"));
    }

    private AccessActor requireScopeActor() {
        AccessActor actor = authorizationService.currentActor();
        if (!authorizationService.isOutsourcedVendor(actor)) {
            authorizationService.requirePlatformScope();
        }
        return actor;
    }

    private void enforceAssignedScope(AccessActor actor, FacilityWorkOrderRecord record) {
        authorizationService.enforceOutsourcedAssigneeScope(actor, record.assigneeId(), record.projectId());
    }

    private FacilityWorkOrderPolicyRecord policy(String type, String priority) {
        return repository.findPolicy(type, priority)
                .orElseThrow(() -> ApiException.badRequest("POLICY_NOT_FOUND", "设施工单策略不存在或已被删除"));
    }

    private FacilityWorkOrderRequest require(FacilityWorkOrderRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private FacilityWorkOrderActionRequest requireAction(FacilityWorkOrderActionRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "满意度评分必须在 1 到 5 之间");
        }
    }

    private int positive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须大于 0");
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

    private String defaultValue(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
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
