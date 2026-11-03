package com.complex.platform.merchantservice;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.EmployeeNoticeRecord;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.access.staff.StaffProfileRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.WorkOrderMaterialService;
import com.complex.platform.facility.dto.WorkOrderMaterialRequests;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformRepository;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderActionRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderAssignRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCancelRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCloseMethodRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCloseRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCreateRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteApprovalRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteDecision;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderMerchantConfirmRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderOwnerCandidate;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderPolicyRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class MerchantServiceService {

    private static final int DEFAULT_PAGE_LIMIT = 100;
    // SLA 升级单批上限：一次定时扫描最多处理的工单数，防止单次事务过大。
    private static final int ESCALATE_BATCH_LIMIT = 200;
    // 系统操作者标识：SLA 自动升级无登录用户，留痕 operator_user_id 用此系统标识。
    private static final String SYSTEM_OPERATOR = "system";
    private static final String BUSINESS_TYPE = "merchant_service_work_order";
    // 第 93 条 93-R1：报修/保洁/投诉/其他服务请求（BSM:1323、BSM:1385；裁决 D-12 不含整改、含保洁）；与 V582 CHECK 一致。
    private static final Set<String> TYPES = Set.of("repair", "cleaning", "complaint", "service");
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

    // 77-R2 超时通知用的中文名：类型与优先级同后台页选项，状态同 ENUM:264（升级只扫未处理完的四种状态）。
    private static final String ASSIGN_PERMISSION = "merchant.service_work_order.assign";
    private static final String PROCESS_PERMISSION = "merchant.service_work_order.process";
    // 第 102 条 102-R3（裁决 26）：可配关单方式的类型与三种关单方式（V585 CHECK 同）；保洁、投诉照 BSM:1385 写死。
    private static final Set<String> CLOSE_METHOD_TYPES = Set.of("repair", "service");
    private static final Set<String> CLOSE_METHODS = Set.of("handler", "staff_acceptance", "merchant_confirm");
    // 102-R2：商户能确认关闭的状态＝ENUM:264「待商户确认、已处理待关闭」。
    private static final Set<String> MERCHANT_CONFIRM_STATUSES = Set.of("pending_merchant", "resolved");
    // 第 111 条 111-R2（第十八批裁决 27）：派单前（已提交）商户自己取消；派单后、关闭前由客户关怀部取消。
    private static final Set<String> MERCHANT_CANCEL_STATUSES = Set.of("created");
    private static final Set<String> STAFF_CANCEL_STATUSES = Set.of("assigned", "processing", "pending_merchant", "resolved");
    private static final String CLOSE_PERMISSION = "merchant.service_work_order.close";
    // 第十八批 M 段第 117 条 117-R5：能登记用料的状态＝已派单、处理中、待商户确认（已处理待关闭之前）。
    private static final Set<String> MATERIAL_STATUSES = Set.of("assigned", "processing", "pending_merchant");
    private static final Map<String, String> TYPE_NAMES =
            Map.of("repair", "报修", "cleaning", "保洁", "complaint", "投诉", "service", "其他服务请求");
    private static final Map<String, String> PRIORITY_NAMES = Map.of("low", "低", "medium", "中", "high", "高", "critical", "紧急");
    private static final Map<String, String> STATUS_NAMES =
            Map.of("created", "已提交", "assigned", "已派单", "processing", "处理中", "pending_merchant", "待商户确认");
    private static final DateTimeFormatter DUE_TEXT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai"));

    private final MerchantServiceRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    // 77-R2 超时通知主管（字段注入；单测直接 new 时为空＝只升级标记、不通知）。
    @Autowired(required = false)
    AccessRepository accessRepository;

    @Autowired(required = false)
    JdbcStaffRepository staffRepository;

    @Autowired(required = false)
    PlatformRepository platformRepository;

    // 第十八批 M 段第 117 条 117-R5：工单用料与费用承担方（字段注入；单测直接 new 时为空＝跳过）；@Lazy 同设施工单（库存门面经要货流转成环）。
    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    WorkOrderMaterialService materials;

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
        List<MerchantWorkOrderRecord> records;
        try {
            records = repository.findWorkOrdersByScope(authorizationService.currentMerchantScope(), includeArchived, DEFAULT_PAGE_LIMIT);
        } catch (ApiException exception) {
            Set<String> projects = staffReach(exception, "merchant.service_work_order.view").organizationIds();
            records = projects.isEmpty() ? List.of()
                    : repository.findWorkOrdersByMerchantProjects(projects, includeArchived, DEFAULT_PAGE_LIMIT);
        }
        auditEventService.recordSuccess(
                "merchant_service.listMerchantServiceWorkOrders.succeeded",
                "merchant.service_work_order.view",
                BUSINESS_TYPE,
                "all",
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, DEFAULT_PAGE_LIMIT, records.size());
    }

    /** 第十八批 M 段第 111 条 111-R1（BSM:1466「我的工单：只看派给自己的工单和处理进度。」）：员工手机「我的工单」只读列出派给本人的商户服务工单。 */
    public PageResponse<MerchantWorkOrderRecord> listAssignedToMe() {
        List<MerchantWorkOrderRecord> records = repository.findWorkOrdersByOwner(authorizationService.currentActor().id(), 200);
        auditEventService.recordSuccess(
                "merchant_service.work_order.viewed",
                PROCESS_PERMISSION,
                BUSINESS_TYPE,
                "all",
                requestId(),
                null,
                Map.of("scope", "assigned_to_me", "count", records.size())
        );
        return new PageResponse<>(records, 1, 200, records.size());
    }

    @Transactional
    public MerchantWorkOrderRecord createWorkOrder(MerchantWorkOrderCreateRequest request) {
        String operationId = "createMerchantWorkOrder:" + authorizationService.currentActor().id();
        return executeWrite(
                operationId,
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.createMerchantServiceWorkOrder.failed",
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
                "merchant_service.assignMerchantServiceWorkOrder.failed",
                "merchant.service_work_order.assign",
                workOrderId,
                () -> {
                    validateAssignRequest(request);
                    ensureWorkOrderCanChange(workOrderId, "merchant.service_work_order.assign");
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
                "merchant_service.addMerchantServiceWorkOrderAction.failed",
                "merchant.service_work_order.process",
                workOrderId,
                () -> {
                    validateActionRequest(request);
                    ensureWorkOrderCanChange(workOrderId, "merchant.service_work_order.process");
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
                "merchant_service.closeMerchantServiceWorkOrder.failed",
                "merchant.service_work_order.close",
                workOrderId,
                () -> {
                    validateCloseRequest(request);
                    ensureWorkOrderCanChange(workOrderId, "merchant.service_work_order.close");
                },
                () -> closeWorkOrderOnce(workOrderId, request)
        );
    }

    /**
     * 第十八批 L 段第 102 条 102-R2：商户在商户专区确认已解决即关闭（BSM:1385「商户投诉须由商户确认投诉已解决」「未点击关闭」；
     * ENUM:264「待商户确认、已处理待关闭」）。只有本商户账号能确认：平台员工与平台管理员 403 MERCHANT_SCOPE_REQUIRED，别的商户 403 CROSS_MERCHANT_FORBIDDEN，
     * 闸在幂等之前。102-R3：只对关单方式为「须商户确认」的单（投诉；配成须商户确认的报修、其他服务请求，裁决 26）。
     */
    @Transactional
    public MerchantWorkOrderRecord confirmByMerchant(String id, MerchantWorkOrderMerchantConfirmRequest request) {
        String workOrderId = requireText(id, "id");
        return executeWrite(
                "confirmMerchantWorkOrder:" + workOrderId,
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.work_order.merchant_confirm_failed",
                "merchant.service_work_order.create",
                workOrderId,
                () -> {
                    if (request == null) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
                    }
                    requireText(request.idempotencyKey(), "idempotency_key");
                    validateSatisfactionScore(request.satisfactionScore());
                    MerchantWorkOrderRecord existing = findWorkOrder(workOrderId);
                    rejectTerminal(existing);
                    String merchantId = authorizationService.currentMerchantScope()
                            .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "只有提单商户能确认工单已解决"));
                    if (!merchantId.equals(existing.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的工单");
                    }
                },
                () -> confirmByMerchantOnce(workOrderId, request)
        );
    }

    /**
     * 第十八批 M 段第 111 条 111-R2（第十八批裁决 27「派单前商户自己取消」；ENUM:264「已取消（商户专区发起……）」）：还没派单（已提交）的单，
     * 本商户账号填原因取消即为已取消；闸同 {@link #confirmByMerchant}（平台员工与平台管理员 403 MERCHANT_SCOPE_REQUIRED，别的商户 403
     * CROSS_MERCHANT_FORBIDDEN），在幂等之前。
     */
    @Transactional
    public MerchantWorkOrderRecord cancelByMerchant(String id, MerchantWorkOrderCancelRequest request) {
        String workOrderId = requireText(id, "id");
        return executeWrite(
                "cancelMerchantWorkOrder:" + workOrderId,
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.work_order.cancel_failed",
                "merchant.service_work_order.create",
                workOrderId,
                () -> {
                    validateCancelRequest(request);
                    MerchantWorkOrderRecord existing = findWorkOrder(workOrderId);
                    rejectTerminal(existing);
                    String merchantId = authorizationService.currentMerchantScope()
                            .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "只有提单商户能取消未派单的工单"));
                    if (!merchantId.equals(existing.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的工单");
                    }
                },
                () -> cancelOnce(workOrderId, request, MERCHANT_CANCEL_STATUSES, "merchant.service_work_order.create",
                        "只有已提交（未派单）的工单才能由商户取消；已派单的照常处理关单")
        );
    }

    /**
     * 111-R2（第十八批裁决 27「派单后，重复提或误提的由客户关怀部填原因取消」；BSM:1385「工单由客户关怀部负责处理」）：已派单、未关闭的单，
     * 持关闭权限的员工填原因取消；闸同员工关单（{@link #ensureWorkOrderCanChange}），在幂等之前。
     */
    @Transactional
    public MerchantWorkOrderRecord cancelByStaff(String id, MerchantWorkOrderCancelRequest request) {
        String workOrderId = requireText(id, "id");
        return executeWrite(
                "staffCancelMerchantWorkOrder:" + workOrderId,
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.work_order.cancel_failed",
                CLOSE_PERMISSION,
                workOrderId,
                () -> {
                    validateCancelRequest(request);
                    ensureWorkOrderCanChange(workOrderId, CLOSE_PERMISSION);
                },
                () -> cancelOnce(workOrderId, request, STAFF_CANCEL_STATUSES, CLOSE_PERMISSION,
                        "只有已派单、未关闭的工单才能由客户关怀部取消；未派单的由商户自己取消")
        );
    }

    /**
     * 第十八批 M 段第 111 条 111-R3 客诉跟进的商户服务工单链闸＝员工关单的闸（BSM:1385「工单由客户关怀部负责处理」）：平台口径、持关闭权限、
     * 该单商户的经营项目在关闭权限可触及范围内（平台管理员不限），否则 403 CROSS_PROJECT_FORBIDDEN。
     */
    public MerchantWorkOrderRecord findForComplaintFollowup(String id) {
        authorizationService.requirePlatformScope();
        authorizationService.requirePermission(CLOSE_PERMISSION);
        MerchantWorkOrderRecord order = findWorkOrder(id);
        checkMerchantOwnership(order.merchantId(), CLOSE_PERMISSION);
        return order;
    }

    /**
     * SLA 自动超时升级（可直接调用，供 @Scheduled 薄包装与单测调用）：
     * 扫描 due_at 已过、未完成（status 非 resolved/closed/cancelled）且尚未升级（escalated_at IS NULL）的工单，
     * 逐单打标 sla_breached + escalated_at（markSlaBreached，不改 status），并写一条 'escalated' 动作留痕。
     * 留痕的 from/to status 一致（升级不迁移状态），operator 用系统标识（无登录上下文）。
     * 第十八批 I 段 77-R2：赢得升级的单同时通知主管（ENUM:265「超时自动升级通知主管」）——见 {@link #staff(String)}。
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
        List<Staff> dispatchers = null; // 本轮有单升级时才算一次
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
            if (dispatchers == null) {
                dispatchers = staff(ASSIGN_PERMISSION);
            }
            int notified = notifyDispatchers(record, dispatchers, now);
            auditEventService.recordSuccess(
                    "merchant_service.work_order.escalated",
                    "merchant.service_work_order.process",
                    BUSINESS_TYPE,
                    record.id(),
                    requestId(),
                    null,
                    Map.of("status", record.status(), "sla_breached", true, "notified", notified)
            );
            escalated++;
        }
        return escalated;
    }

    /** 77-R2 超时收件人、102-R1 派单负责人：账号、员工档案与其该权限码可触及的项目范围。 */
    private record Staff(AccessUser user, StaffProfileRecord profile, AuthorizationService.ProjectReach reach) {
    }

    /**
     * 77-R2：主管＝在用、平台口径（无商户归属、非外包账号，与入口 requirePlatformScope 同口径，PGM:528④）、
     * 有效持派单权限（派单是客户关怀部主管动作，BSM:1385）、有员工档案的账号；各算一次派单码可触及的项目（没有项目归属的跳过）。
     * 102-R1：派单负责人同一口径，按处理权限码取（BSM:1380「派单到具体人员或班组」）。
     */
    private List<Staff> staff(String permissionCode) {
        if (accessRepository == null || staffRepository == null || platformRepository == null) {
            return List.of();
        }
        List<Staff> result = new ArrayList<>();
        for (AccessUser user : accessRepository.listUsers()) {
            staff(user, permissionCode).ifPresent(result::add);
        }
        return result;
    }

    private Optional<Staff> staff(AccessUser user, String permissionCode) {
        AccessActor actor = new AccessActor(user.id(), user.username(), user.accountType(), user.organizationId(),
                user.merchantId(), user.status());
        if (!"active".equals(user.status()) || trimToNull(user.merchantId()) != null
                || authorizationService.isOutsourcedVendor(actor)
                || !authorizationService.hasEffectivePermission(actor, permissionCode)) {
            return Optional.empty();
        }
        Optional<StaffProfileRecord> profile = staffRepository.findProfileByUserId(user.id());
        if (profile.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Staff(user, profile.get(), authorizationService.projectReach(actor, permissionCode)));
        } catch (ApiException noProjectScope) {
            return Optional.empty(); // 没有项目归属，够不到任何项目
        }
    }

    /**
     * 第十八批 L 段第 102 条 102-R1：可派的负责人＝{@link #staff} 按处理码取、处理码可触及该单商户任一经营项目的平台员工（照 listPolicies 不记审计）。
     * 读闸同派单：平台管理员不限、商户账号只本商户、其余平台员工按派单码可触及该商户经营项目。
     */
    public List<MerchantWorkOrderOwnerCandidate> listOwnerCandidates(String id) {
        MerchantWorkOrderRecord order = findWorkOrder(requireText(id, "id"));
        checkMerchantOwnership(order.merchantId(), ASSIGN_PERMISSION);
        List<Staff> candidates = staff(PROCESS_PERMISSION);
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<String> projects = platformRepository.listMerchantOperatingProjectIds(order.merchantId());
        return candidates.stream()
                .filter(candidate -> projects.stream().anyMatch(candidate.reach()::covers))
                .map(candidate -> new MerchantWorkOrderOwnerCandidate(candidate.user().id(), candidate.user().username(), candidate.profile().name()))
                .toList();
    }

    /** 102-R1：负责人须为在岗、持商户工单处理权限、处理码可触及该单商户任一经营项目的平台员工；查不到即不行。 */
    private boolean canOwn(String userId, MerchantWorkOrderRecord order) {
        if (accessRepository == null || staffRepository == null || platformRepository == null) {
            return false;
        }
        List<String> projects = platformRepository.listMerchantOperatingProjectIds(order.merchantId());
        return accessRepository.findUserById(userId)
                .flatMap(user -> staff(user, PROCESS_PERMISSION))
                .filter(owner -> projects.stream().anyMatch(owner.reach()::covers))
                .isPresent();
    }

    /** 第 102 条 102-R3：关单方式。保洁＝处理人完成即结束、投诉＝须商户确认（BSM:1385 写死）；报修、其他服务请求按后台配置（裁决 26）。 */
    private String closeMethod(String workOrderType) {
        return switch (workOrderType) {
            case "cleaning" -> "handler";
            case "complaint" -> "merchant_confirm";
            default -> repository.findCloseMethod(workOrderType);
        };
    }

    /**
     * 77-R2：给派单码可触及该单商户任一经营项目（主项目或追加的经营项目，裁决 M737-iter7）的主管各发一条员工系统通知；返回通知人数。
     */
    private int notifyDispatchers(MerchantWorkOrderRecord record, List<Staff> dispatchers, Instant now) {
        if (dispatchers.isEmpty()) {
            return 0;
        }
        List<String> projects = platformRepository.listMerchantOperatingProjectIds(record.merchantId());
        String merchantName = platformRepository.findMerchantById(record.merchantId())
                .map(MerchantRecord::merchantName).orElse(record.merchantId());
        String title = "商户工单超时：" + record.title();
        String body = merchantName + "的工单「" + record.title() + "」（"
                + TYPE_NAMES.getOrDefault(record.workOrderType(), record.workOrderType()) + "/"
                + PRIORITY_NAMES.getOrDefault(record.priority(), record.priority()) + "）已超过处理时限（"
                + DUE_TEXT.format(record.dueAt()) + "，北京时间）仍未处理完，当前状态"
                + STATUS_NAMES.getOrDefault(record.status(), record.status()) + "。";
        int notified = 0;
        for (Staff dispatcher : dispatchers) {
            if (projects.stream().anyMatch(dispatcher.reach()::covers)) {
                staffRepository.saveNotice(new EmployeeNoticeRecord(
                        "notice_" + UUID.randomUUID().toString().replace("-", ""), dispatcher.profile().id(), "system_notice",
                        title, body, false, "pending", null, SYSTEM_OPERATOR, now, now));
                notified++;
            }
        }
        return notified;
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
        // 77-R3：截止时间＝建单时刻＋该类型×优先级的处理时限（BSM:1385 后台配置）；未配置的组合不设截止时间、不升级。
        Instant dueAt = repository.findHandleMinutes(type, priority)
                .map(minutes -> now.plus(Duration.ofMinutes(minutes)))
                .orElse(null);
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
                dueAt,
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
                "merchant_service.createMerchantServiceWorkOrder.succeeded",
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
            throw ApiException.conflict("INVALID_STATE", "只有新建状态的工单才能派单");
        }
        String ownerUserId = requireText(request.ownerUserId(), "owner_user_id");
        if (!canOwn(ownerUserId, existing)) {
            throw ApiException.badRequest("WORK_ORDER_OWNER_INVALID", "负责人须为在岗、持商户工单处理权限且能管到该商户经营项目的员工");
        }
        MerchantWorkOrderRecord assigned = repository.saveWorkOrder(existing.assign(
                requireText(request.ownerDepartment(), "owner_department"),
                ownerUserId,
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
                "merchant_service.assignMerchantServiceWorkOrder.succeeded",
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
            if ("resolved".equals(targetStatus) && !"resolved".equals(existing.status())) {
                requireMaterialsSettled(id); // 117-R5：用了库房物料须先标明承担方；承担方争议判定前不能转已处理待关闭（裁决 29）
            }
            if ("resolved".equals(targetStatus) && !"resolved".equals(existing.status())
                    && "handler".equals(closeMethod(existing.workOrderType()))) {
                // 102-R3：处理人完成即结束（BSM:1385「保洁等无争议类由处理人完成即结束」；报修、其他服务请求按裁决 26 配置），处理说明即处理结论
                saved = repository.saveWorkOrder(existing.close(operatorNote, null, Instant.now()));
                actionType = "closed";
            } else {
                saved = repository.saveWorkOrder(existing.transitionTo(targetStatus, Instant.now()));
                actionType = existing.status().equals(targetStatus) ? "commented" : "status_changed";
            }
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
        postMaterialReceivable(saved); // 117-R5：处理人完成即结束的关单
        auditEventService.recordSuccess(
                "merchant_service.addMerchantServiceWorkOrderAction.succeeded",
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
            throw ApiException.conflict("INVALID_STATE", "只有已解决状态的工单才能关闭");
        }
        String resolution = requireText(request.resolution(), "resolution");
        validateSatisfactionScore(request.satisfactionScore());
        // 102-R2：商户未确认的投诉，客户关怀部记关闭缘由后可以关闭（BSM:1385）；102-R3：配成「须商户确认」的报修、其他服务请求员工不能关（裁决 26）。
        String closeReason = null;
        if ("complaint".equals(existing.workOrderType())) {
            closeReason = requireText(request.closeReason(), "close_reason");
        } else if ("merchant_confirm".equals(closeMethod(existing.workOrderType()))) {
            throw ApiException.conflict("MERCHANT_CONFIRM_REQUIRED", "该类工单按后台配置须商户确认后关闭");
        }
        requireMaterialsSettled(id); // 117-R5／裁决 29
        MerchantWorkOrderRecord closed = repository.saveWorkOrder(existing.close(
                resolution,
                request.satisfactionScore(),
                Instant.now()
        ));
        postMaterialReceivable(closed);
        repository.saveAction(action(
                id,
                "closed",
                existing.status(),
                "closed",
                authorizationService.currentActor().id(),
                closeReason != null ? closeReason : defaultNote(request.operatorNote(), "closed"),
                evidenceIds(request.evidenceAttachmentIds())
        ));
        auditEventService.recordSuccess(
                "merchant_service.closeMerchantServiceWorkOrder.succeeded",
                "merchant.service_work_order.close",
                BUSINESS_TYPE,
                closed.id(),
                requestId(),
                request.idempotencyKey(),
                closeReason == null ? Map.of("status", closed.status()) : Map.of("status", closed.status(), "close_reason", closeReason)
        );
        return closed;
    }

    private MerchantWorkOrderRecord confirmByMerchantOnce(String id, MerchantWorkOrderMerchantConfirmRequest request) {
        MerchantWorkOrderRecord existing = findWorkOrder(id);
        rejectTerminal(existing);
        if (!MERCHANT_CONFIRM_STATUSES.contains(existing.status())) {
            throw ApiException.conflict("INVALID_STATE", "只有待商户确认或已处理待关闭的工单才能由商户确认");
        }
        if (!"merchant_confirm".equals(closeMethod(existing.workOrderType()))) {
            throw ApiException.conflict("MERCHANT_CONFIRM_NOT_APPLICABLE", "该类工单不由商户确认关闭");
        }
        requireMaterialsSettled(id); // 117-R5／裁决 29
        MerchantWorkOrderRecord closed = repository.saveWorkOrder(existing.close(existing.resolution(), request.satisfactionScore(), Instant.now()));
        postMaterialReceivable(closed);
        repository.saveAction(action(id, "closed", existing.status(), "closed", authorizationService.currentActor().id(), "商户确认已解决", List.of()));
        auditEventService.recordSuccess(
                "merchant_service.work_order.merchant_confirmed",
                "merchant.service_work_order.create",
                BUSINESS_TYPE,
                closed.id(),
                requestId(),
                request.idempotencyKey(),
                request.satisfactionScore() == null ? Map.of("status", closed.status())
                        : Map.of("status", closed.status(), "satisfaction_score", request.satisfactionScore())
        );
        return closed;
    }

    /** 111-R2：取消即为已取消（关闭时间不写），取消动作的操作人为当前账号、说明为原因。 */
    private MerchantWorkOrderRecord cancelOnce(String id, MerchantWorkOrderCancelRequest request, Set<String> allowedStatuses,
            String permissionCode, String stateMessage) {
        MerchantWorkOrderRecord existing = findWorkOrder(id);
        rejectTerminal(existing);
        if (!allowedStatuses.contains(existing.status())) {
            throw ApiException.conflict("INVALID_STATE", stateMessage);
        }
        String reason = requireText(request.reason(), "reason");
        MerchantWorkOrderRecord cancelled = repository.saveWorkOrder(existing.transitionTo("cancelled", Instant.now()));
        repository.saveAction(action(id, "cancelled", existing.status(), "cancelled", authorizationService.currentActor().id(), reason, List.of()));
        auditEventService.recordSuccess(
                "merchant_service.work_order.cancelled",
                permissionCode,
                BUSINESS_TYPE,
                cancelled.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", cancelled.status(), "from_status", existing.status(), "reason", reason)
        );
        return cancelled;
    }

    // ================= 第十八批 M 段第 117 条 117-R5 工单用料与费用承担方 =================
    // BSM:1372「商户自己在商户专区报的单，按枚举速查表「商户服务工单状态」那条的状态走」；BSM:1380「用料必须从指定库房出库并按当前平均进货价计成本，
    // 同时在工单上标明费用承担方……商户自有设施、以及因商户责任导致损坏的用料由该商户承担，并自动生成一笔应收进入其费用账单。承担方有争议的，
    // 工单挂起转上级判定，判定结果与依据留痕后才能结案。」；第十八批裁决 29「不加状态，停在原状态」：有争议时单子停在原状态转上级判定，判定前不能转
    // 「已处理待关闭」、不能关闭；判定结果与依据留痕后照常往下走（复用 117-R2 的审批策略与判定回写）。只给平台员工（商户账号不登记库房出库），
    // 闸同处理（持处理码、该单商户的经营项目可触及），在幂等之前；商户承担的商户就是提单商户。

    /** 117-R5：用料面板——用料明细与合计、承担方、争议记录、工单项目的平台库房余额行。 */
    public WorkOrderMaterialService.Materials materials(String id) {
        return materialView(materialOrder(id));
    }

    /** 117-R5：已派单、处理中、待商户确认的工单登记用料＝从所选库位出库一次，按当前平均进货价计成本、挂本工单。 */
    @Transactional
    public WorkOrderMaterialService.Materials issueMaterial(String id, WorkOrderMaterialRequests.Issue request) {
        String key = request == null ? null : request.idempotencyKey();
        return materialWrite("merchantWorkOrderMaterial:", id, key, request, "merchant_service.work_order.material_issue_failed", () -> {
            MerchantWorkOrderRecord order = findWorkOrder(id);
            if (!MATERIAL_STATUSES.contains(order.status())) {
                throw ApiException.conflict("INVALID_STATE", "只有已派单、处理中或待商户确认的工单才能登记用料");
            }
            var movement = materials.issue(BUSINESS_TYPE, id, request.locationId(), request.itemId(), request.quantity(), key);
            auditEventService.recordSuccess("merchant_service.work_order.material_issued", PROCESS_PERMISSION, BUSINESS_TYPE, id, requestId(), key,
                    Map.of("item_id", movement.itemId(), "location_id", movement.sourceLocationId(), "quantity", movement.quantity(),
                            "total_cost_cent", Objects.toString(movement.totalCostCent(), "")));
            return materialView(order);
        });
    }

    /** 117-R5：标明费用承担方（物业承担／商户承担＝提单商户）；已关闭或已取消的不能改，承担方争议待判定时不能改。 */
    @Transactional
    public WorkOrderMaterialService.Materials markCostBearer(String id, WorkOrderMaterialRequests.Bearer request) {
        String key = request == null ? null : request.idempotencyKey();
        return materialWrite("merchantWorkOrderCostBearer:", id, key, request, "merchant_service.work_order.cost_bearer_mark_failed", () -> {
            MerchantWorkOrderRecord order = findWorkOrder(id);
            rejectTerminal(order);
            materials.requireNoPendingDispute(BUSINESS_TYPE, id);
            materials.markBearer(BUSINESS_TYPE, id, request.costBearer(), order.merchantId());
            WorkOrderMaterialService.Materials view = materialView(order);
            auditEventService.recordSuccess("merchant_service.work_order.cost_bearer_marked", PROCESS_PERMISSION, BUSINESS_TYPE, id, requestId(), key,
                    Map.of("cost_bearer", view.bearer().costBearer()));
            return view;
        });
    }

    /**
     * 117-R5（第十八批裁决 29）：承担方有争议的，单子停在原状态转提出人的直接上级（或其指定的最终审批人）在审批链判定，审批意见即判定依据；
     * 须已标承担方、原因必填；判定前不能转已处理待关闭、不能关闭、不能改承担方、不能再提。
     */
    @Transactional
    public WorkOrderMaterialService.Materials disputeBearer(String id, WorkOrderMaterialRequests.Dispute request) {
        String key = request == null ? null : request.idempotencyKey();
        return materialWrite("merchantWorkOrderBearerDispute:", id, key, request, "merchant_service.work_order.bearer_dispute_failed", () -> {
            MerchantWorkOrderRecord order = findWorkOrder(id);
            rejectTerminal(order);
            materials.requireNoPendingDispute(BUSINESS_TYPE, id);
            WorkOrderMaterialService.Dispute dispute = materials.openDispute(BUSINESS_TYPE, id, "商户服务工单 " + id, request.claimedCostBearer(),
                    order.merchantId(), request.reason(), request.finalApproverId(), key);
            auditEventService.recordSuccess("merchant_service.work_order.bearer_disputed", PROCESS_PERMISSION, BUSINESS_TYPE, id, requestId(), key,
                    Map.of("review_id", dispute.reviewId(), "from_cost_bearer", dispute.fromCostBearer(),
                            "claimed_cost_bearer", dispute.claimedCostBearer()));
            return materialView(order);
        });
    }

    private WorkOrderMaterialService.Materials materialView(MerchantWorkOrderRecord order) {
        return materials.view(BUSINESS_TYPE, order.id(), materials.merchantServiceProjectId(order.id()));
    }

    /** 117-R5 闸：平台口径（商户账号不登记库房出库），该单商户的经营项目在处理码可触及范围内。 */
    private MerchantWorkOrderRecord materialOrder(String id) {
        authorizationService.requirePlatformScope();
        MerchantWorkOrderRecord order = findWorkOrder(requireText(id, "id"));
        checkMerchantOwnership(order.merchantId(), PROCESS_PERMISSION);
        return order;
    }

    private WorkOrderMaterialService.Materials materialWrite(String operation, String id, String key, Object request, String failureEvent,
            Supplier<WorkOrderMaterialService.Materials> action) {
        try {
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
            }
            materialOrder(id); // 范围闸在幂等之前
            return idempotencyService.execute(operation + id, key, request, action, WorkOrderMaterialService.Materials.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(failureEvent, PROCESS_PERMISSION, BUSINESS_TYPE, id, requestId(), key, errorCode(exception));
            throw exception;
        }
    }

    /** 117-R5：转已处理待关闭与关闭前——用了库房物料须先标明承担方；承担方争议待上级判定时不能转、不能关（裁决 29）。 */
    private void requireMaterialsSettled(String id) {
        if (materials != null) {
            materials.requireBearerIfMaterials(BUSINESS_TYPE, id);
            materials.requireNoPendingDispute(BUSINESS_TYPE, id);
        }
    }

    /** 117-R5：本次转为关闭时按最终承担方给提单商户生成一笔应收（物业承担不生成）。 */
    private void postMaterialReceivable(MerchantWorkOrderRecord saved) {
        if (materials != null && "closed".equals(saved.status())) {
            materials.postReceivableOnClose(BUSINESS_TYPE, saved.id(), "工单用料费（商户服务工单 " + saved.id() + "）",
                    materials.merchantServiceProjectId(saved.id()));
        }
    }

    // ================= 第十八批 I 段 77-R3 处理时限（工单类型×优先级） =================

    public List<MerchantWorkOrderPolicyRecord> listPolicies() {
        return repository.listPolicies();
    }

    /** 保存一格处理时限（平台口径；商户账号与外包账号拒绝）。只影响此后新建的工单。 */
    @Transactional
    public MerchantWorkOrderPolicyRecord savePolicy(MerchantWorkOrderPolicyRequest request) {
        try {
            authorizationService.requirePlatformScope();
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
            }
            String type = normalizeAllowed(request.workOrderType(), "work_order_type", TYPES);
            String priority = normalizeAllowed(request.priority(), "priority", PRIORITIES);
            if (request.handleMinutes() == null || request.handleMinutes() <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "handle_minutes 必须大于 0");
            }
            return idempotencyService.execute("saveMerchantWorkOrderPolicy:" + type + ":" + priority, request.idempotencyKey(), request,
                    () -> {
                        MerchantWorkOrderPolicyRecord saved = repository.savePolicy(new MerchantWorkOrderPolicyRecord(
                                type, priority, request.handleMinutes(), actorId(), Instant.now()));
                        auditEventService.recordSuccess("merchant_service.work_order_policy_saved", "merchant.service_work_order.assign",
                                "merchant_service_work_order_policy", type + ":" + priority, requestId(), request.idempotencyKey(),
                                Map.of("handle_minutes", saved.handleMinutes()));
                        return saved;
                    }, MerchantWorkOrderPolicyRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("merchant_service.work_order_policy_save_failed", "merchant.service_work_order.assign",
                    "merchant_service_work_order_policy", null, requestId(), request == null ? null : request.idempotencyKey(),
                    errorCode(exception));
            throw exception;
        }
    }

    // ================= 第十八批 L 段第 102 条 102-R3 关单方式（报修、其他服务请求；裁决 26） =================

    public List<MerchantWorkOrderCloseMethodRecord> listCloseMethods() {
        return repository.listCloseMethods();
    }

    /** 改报修或其他服务请求的关单方式（平台口径；商户账号与外包账号拒绝）。按单子当时的类型配置判，已在途的单也照新配置关。 */
    @Transactional
    public MerchantWorkOrderCloseMethodRecord saveCloseMethod(MerchantWorkOrderCloseMethodRequest request) {
        try {
            authorizationService.requirePlatformScope();
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
            }
            String type = normalizeAllowed(request.workOrderType(), "work_order_type", CLOSE_METHOD_TYPES);
            String closeMethod = normalizeAllowed(request.closeMethod(), "close_method", CLOSE_METHODS);
            return idempotencyService.execute("saveMerchantWorkOrderCloseMethod:" + type, request.idempotencyKey(), request,
                    () -> {
                        MerchantWorkOrderCloseMethodRecord saved = repository.saveCloseMethod(
                                new MerchantWorkOrderCloseMethodRecord(type, closeMethod, actorId(), Instant.now()));
                        auditEventService.recordSuccess("merchant_service.work_order_close_method_saved", ASSIGN_PERMISSION,
                                "merchant_service_work_order_close_method", type, requestId(), request.idempotencyKey(),
                                Map.of("close_method", closeMethod));
                        return saved;
                    }, MerchantWorkOrderCloseMethodRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("merchant_service.work_order_close_method_save_failed", ASSIGN_PERMISSION,
                    "merchant_service_work_order_close_method", null, requestId(), request == null ? null : request.idempotencyKey(),
                    errorCode(exception));
            throw exception;
        }
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
    public MerchantWorkOrderRecord archiveWorkOrder(String id, boolean archive, String reason) {
        MerchantWorkOrderRecord order = findWorkOrder(id);
        checkMerchantOwnership(order.merchantId(), "merchant.service_work_order.close");
        String r = archive ? requireText(reason, "reason") : null;
        Instant now = Instant.now();
        MerchantWorkOrderRecord updated = archive
                ? order.withArchive(now, actorId(), now)
                : order.withArchive(null, null, now).withDelete("none", null, null, null, now);
        MerchantWorkOrderRecord saved = repository.saveWorkOrder(updated);
        repository.setWorkOrderArchiveReason(id, r);
        auditEventService.recordSuccess(archive ? "merchant_service.work_order.archived" : "merchant_service.work_order.unarchived",
                "merchant.service_work_order.close", BUSINESS_TYPE, id, requestId(), null,
                Map.of("archived", archive));
        return saved;
    }

    /** 发起删除申请：须先存档，记 requested_by/at/reason，状态置 pending。 */
    @Transactional
    public MerchantWorkOrderRecord requestDeleteWorkOrder(String id, MerchantWorkOrderDeleteRequest request) {
        MerchantWorkOrderRecord order = findWorkOrder(id);
        checkMerchantOwnership(order.merchantId(), "merchant.service_work_order.close");
        if (order.archivedAt() == null) {
            throw ApiException.badRequest("WORK_ORDER_NOT_ARCHIVED", "工单必须先存档才能申请删除");
        }
        if ("pending".equals(order.deleteStatus())) {
            throw ApiException.conflict("WORK_ORDER_DELETE_ALREADY_PENDING", "该工单已有待审批的删除申请");
        }
        Instant now = Instant.now();
        String reason = requireText(request == null ? null : request.reason(), "reason");
        MerchantWorkOrderRecord saved = repository.saveWorkOrder(order.withDelete("pending", actorId(), now, reason, now));
        auditEventService.recordSuccess("merchant_service.work_order.delete_requested",
                "merchant.service_work_order.close", BUSINESS_TYPE, id, requestId(), null, Map.of());
        return saved;
    }

    /** 审批删除申请。approved 且无活引用（工单已终态）→ 物理删除；rejected → 回 none。禁自审。 */
    @Transactional
    public MerchantWorkOrderDeleteDecision approveDeleteWorkOrder(String id, MerchantWorkOrderDeleteApprovalRequest request) {
        MerchantWorkOrderRecord order = findWorkOrder(id);
        checkMerchantOwnership(order.merchantId(), "merchant.service_work_order.approve");
        if (!"pending".equals(order.deleteStatus())) {
            throw ApiException.badRequest("WORK_ORDER_DELETE_NOT_PENDING", "该工单没有待审批的删除申请");
        }
        String decision = request == null || request.decision() == null
                ? null : request.decision().trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
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
                .orElseThrow(() -> ApiException.notFound("WORK_ORDER_NOT_FOUND", "商户服务工单不存在或已被删除"));
    }

    private void ensureWorkOrderCanChange(String id, String permissionCode) {
        MerchantWorkOrderRecord existing = findWorkOrder(id);
        rejectTerminal(existing);
        checkMerchantOwnership(existing.merchantId(), permissionCode);
    }

    private void checkMerchantOwnership(String recordMerchantId, String permissionCode) {
        Optional<String> scope;
        try {
            scope = authorizationService.currentMerchantScope();
        } catch (ApiException exception) {
            AuthorizationService.ProjectReach reach = staffReach(exception, permissionCode);
            List<String> projects = platformRepository == null ? List.of() : platformRepository.listMerchantOperatingProjectIds(recordMerchantId);
            if (projects.stream().noneMatch(reach::covers)) {
                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "该工单所属商户不在本权限可触及的项目范围内");
            }
            return;
        }
        if (scope.isEmpty()) {
            return; // platform admin sees all merchants
        }
        if (!scope.get().equals(recordMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的工单");
        }
    }

    /**
     * 第 93 条 93-R3：平台管理员（不限，D-032）与商户账号（本商户）照 currentMerchantScope 判；它对其余账号抛的 MERCHANT_SCOPE_REQUIRED
     * 到这里改判：外包账号照旧 403（requirePlatformScope，与 77-R2 收件人、时限入口同一口径），其余平台员工（BSM:1385「工单由客户关怀部负责处理」）
     * 返回入口权限码可触及的项目，按工单所属商户的经营项目判（裁决 M737-iter7；含可跨项目角色勾选的项目，PGM:528）。
     */
    private AuthorizationService.ProjectReach staffReach(ApiException exception, String permissionCode) {
        if (!"MERCHANT_SCOPE_REQUIRED".equals(exception.code())) {
            throw exception;
        }
        authorizationService.requirePlatformScope();
        return authorizationService.projectReach(authorizationService.currentActor(), permissionCode);
    }

    private void validateCreateRequest(MerchantWorkOrderCreateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.ownerDepartment(), "owner_department");
        requireText(request.ownerUserId(), "owner_user_id");
        requireText(request.operatorNote(), "operator_note");
    }

    private void validateActionRequest(MerchantWorkOrderActionRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.resolution(), "resolution");
        validateSatisfactionScore(request.satisfactionScore());
    }

    private void validateCancelRequest(MerchantWorkOrderCancelRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.reason(), "reason");
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
            throw ApiException.conflict("WORK_ORDER_CLOSED", "已关闭或已取消的工单不能再变更");
        }
    }

    private void requireValidActionTransition(String fromStatus, String toStatus) {
        if (fromStatus.equals(toStatus)) {
            return; // re-affirming the current status is a comment, not a transition
        }
        if (!ALLOWED_ACTION_TRANSITIONS.getOrDefault(fromStatus, Set.of()).contains(toStatus)) {
            throw ApiException.conflict(
                    "MERCHANT_WORK_ORDER_INVALID_TRANSITION",
                    "工单状态不能从 " + fromStatus + " 流转到 " + toStatus);
        }
    }

    private void validateSatisfactionScore(Integer score) {
        if (score != null && (score < 1 || score > 5)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "满意度评分必须在 1 到 5 之间");
        }
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

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR";
    }
}
