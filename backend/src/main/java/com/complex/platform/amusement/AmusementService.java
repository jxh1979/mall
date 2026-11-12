package com.complex.platform.amusement;

// 文件版本：V0.06

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.RiskAlertService;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class AmusementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> PROJECT_STATUSES = Set.of("open", "paused", "closed", "maintenance");
    private static final Set<String> SESSION_STATUSES = Set.of("scheduled", "open", "closed", "cancelled");
    private static final Set<String> RULE_TYPES = Set.of("age", "height", "weight", "capacity", "safety_notice", "manual");
    private static final Set<String> DEVICE_STATUSES = Set.of("active", "inactive", "maintenance", "retired");

    // 游乐权益类前缀（§6.4/§12.1：游乐只消费票务生成的游乐权益）。
    // entitlement_type 以 "amusement" 开头视为游乐类（amusement_single / amusement_pass / 游乐通票等）。
    private static final String AMUSEMENT_ENTITLEMENT_TYPE_PREFIX = "amusement";

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.ticketing.ParkWideFaultSalesGate parkWideFaultSalesGate;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.ticketing.BarMerchantDirectory merchantDirectory;
    /** 第十八批 N 段第 120 条 120-R5：状态看板读取留痕记可信终端、设备号与绑定点位。字段注入免改构造点；单测（无 Spring）为 null 则不记。 */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.terminal.TerminalDeviceService terminalDeviceService;

    private final AmusementRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AmusementBookingService bookingService;
    private final AmusementInspectionService inspectionService;
    private final AmusementMaintenanceService maintenanceService;
    private final AuthorizationService authorizationService;
    private final AmusementEntitlementGateway entitlementGateway;
    private final AmusementSafetyRepository safetyRepository;
    // P1：evidence 统一异常责任链服务。游乐超容除写孤岛 amusement_incidents 外，
    // 再接入统一 risk_alerts（source_domain='amusement'），让统一表真实承载游乐风险数据。
    // 可为 null（向后兼容/单测无 SecurityContext 环境）；接入为 fail-safe，不阻塞核销主链。
    private final RiskAlertService riskAlertService;

    /**
     * CG-02：硬超容自动停运及其留痕要在独立事务里落库。字段注入免改构造点；生产恒注入，单测（无 Spring）为 null 则就地执行。
     * 原因：硬超容阻断后核销主事务随 SESSION_CAPACITY_EXHAUSTED 回滚，同事务内写的 over_capacity incident、
     * open→paused、超容事件会一起被回滚（真库实测只剩失败审计）——「阻断并自动停运」在库里从未生效。
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    org.springframework.transaction.PlatformTransactionManager transactionManager;

    // T3.2（A4 根因修，功能标志 amusement.device-registry.enforce.enabled，默认关）：是否强制游乐项目建设备档案。
    // 开启后「零在役设备档案」项目拒核销，从根上消除 fail-open；默认关给存量项目建档过渡期，ops 建档后开启即生效。
    // 用字段注入（非构造）以免改动既有多处构造点；包私有便于单测直接置位。
    @org.springframework.beans.factory.annotation.Value("${amusement.device-registry.enforce.enabled:false}")
    boolean enforceDeviceRegistry;

    @org.springframework.beans.factory.annotation.Autowired
    public AmusementService(
            AmusementRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            AmusementEntitlementGateway entitlementGateway,
            AmusementSafetyRepository safetyRepository,
            RiskAlertService riskAlertService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.entitlementGateway = entitlementGateway;
        this.safetyRepository = safetyRepository;
        this.riskAlertService = riskAlertService;
        this.bookingService = new AmusementBookingService(repository, idempotencyService, auditEventService, authorizationService, () -> sessionLifecycle);
        this.inspectionService = new AmusementInspectionService(repository, idempotencyService, auditEventService, authorizationService);
        this.maintenanceService = new AmusementMaintenanceService(repository, idempotencyService, auditEventService, authorizationService);
    }

    public PageResponse<AmusementDeviceStatusRecord> listDeviceStatuses() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.device_status.viewed",
                "amusement.device_status.viewed_failed",
                "amusement.device_status.view",
                "amusement_device_status",
                scope.isPresent() ? () -> repository.listDeviceStatusesByMerchant(scope.get()) : repository::listDeviceStatuses
        );
    }

    /** T3.4 巡检前端：列出巡检到期待办（V143 amusement_inspection_tasks，由 AmusementInspectionTaskJob 预生成）。 */
    public PageResponse<AmusementInspectionTaskRecord> listInspectionTasks() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.inspection_task.viewed",
                "amusement.inspection_task.viewed_failed",
                "amusement.safety_inspection.view",
                "amusement_inspection_task",
                () -> repository.listInspectionTasks(scope)
        );
    }

    public PageResponse<AmusementProjectRecord> listProjects() {
        return readPage(
                "amusement.session.viewed",
                "amusement.session.view_failed",
                "amusement.session.view",
                "amusement_project",
                () -> visibleProjectRows(repository.listProjects(), AmusementProjectRecord::id, "amusement.session.view")
        );
    }

    /**
     * 第十八批 N 段第 120 条 120-R5（BSM:991「查看游乐设施状态：开放、排队、容量、暂停、关闭、故障、维护或安全提醒。」；BSM:1268「通过触摸屏一体机展示
     * 游乐设施开放、排队、容量、暂停、故障和维护状态。」；BSM:1002「……展示游乐设施开放状态、容量和异常，并形成状态检查……记录。」；BSM:1004「……一体机的下单、
     * 领取、状态检查和异常提示都要记录设备、点位和时间。」）：游乐设施状态看板，与项目列表同权限同范围。每行在玩人数取项目维容量快照（没有即 0），排队人数取
     * 排队中，维护设备数取设备状态为维护，故障数取未结（已登记、处理中、待外部）的故障事件，安全提醒取当前生效的安全提示标题；不出客人编号与排队号。
     * 读取留痕即状态检查记录：记可信终端类型、设备号与绑定点位（时间即留痕时间）。
     */
    public PageResponse<AmusementProjectStatusRecord> listProjectStatusBoard() {
        Map<String, Object> metadata = terminalDeviceService == null ? new java.util.HashMap<>() : terminalDeviceService.terminalTraceMetadata();
        try {
            List<AmusementProjectRecord> projects = visibleProjectRows(repository.listProjects(), AmusementProjectRecord::id, "amusement.session.view");
            Map<String, Integer> occupied = new java.util.HashMap<>();
            repository.listCapacitySnapshots(authorizationService.currentActor().merchantId()).stream()
                    .filter(row -> "project".equals(row.dimension()))
                    .forEach(row -> occupied.put(row.projectId(), row.occupiedCount()));
            Map<String, Integer> waiting = repository.countWaitingQueueEntriesByProject();
            Map<String, Integer> faults = repository.countOpenFaultIncidentsByProject();
            Map<String, Long> maintenance = repository.listDeviceStatuses().stream()
                    .filter(row -> "maintenance".equals(row.status()))
                    .collect(java.util.stream.Collectors.groupingBy(AmusementDeviceStatusRecord::projectId, java.util.stream.Collectors.counting()));
            Instant now = Instant.now();
            Map<String, List<String>> notices = safetyRepository.findAllSafetyNotices().stream()
                    .filter(n -> "active".equals(n.status()) && "safety_notice".equals(n.noticeType()) && n.amusementProjectId() != null
                            && (n.effectiveFrom() == null || !n.effectiveFrom().isAfter(now))
                            && (n.effectiveTo() == null || n.effectiveTo().isAfter(now)))
                    .collect(java.util.stream.Collectors.groupingBy(AmusementSafetyNoticeRecord::amusementProjectId,
                            java.util.stream.Collectors.mapping(AmusementSafetyNoticeRecord::title, java.util.stream.Collectors.toList())));
            List<AmusementProjectStatusRecord> rows = projects.stream().map(project -> new AmusementProjectStatusRecord(
                    project.id(), project.projectName(), project.openStatus(), project.capacity(),
                    occupied.getOrDefault(project.id(), 0), waiting.getOrDefault(project.id(), 0),
                    maintenance.getOrDefault(project.id(), 0L).intValue(), faults.getOrDefault(project.id(), 0),
                    notices.getOrDefault(project.id(), List.of()))).toList();
            metadata.put("count", rows.size());
            auditEventService.recordSuccess("amusement.status_board.viewed", "amusement.session.view", "amusement_project", "all",
                    null, null, metadata);
            return new PageResponse<>(rows, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, rows.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("amusement.status_board.view_failed", "amusement.session.view", "amusement_project", "all",
                    null, null, codeOf(exception));
            throw exception;
        }
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.session.manage")
    @Transactional
    public AmusementProjectRecord createProject(AmusementProjectRequest request) {
        return executeWrite(
                "createAmusementProject",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementProjectRecord.class,
                "amusement.session.changed",
                "amusement.session.change_failed",
                "amusement.session.manage",
                "amusement_project",
                () -> createProjectInternal(request)
        );
    }

    public PageResponse<AmusementProjectSpaceBacklogRecord> listProjectsWithoutSpace() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.project_space_backlog.viewed",
                "amusement.project_space_backlog.view_failed",
                "amusement.session.manage",
                "amusement_project",
                () -> repository.listProjectsWithoutSpace(scope)
        );
    }

    /**
     * T3.3 存量零档建档：列出无任何在役设备档案、待建档的项目。
     * 运营据此在开启强制建档总开关（{@link #enforceDeviceRegistry}，T3.2）前补齐设备档案；
     * 开关一旦开启，清单内项目即被拒核销（AMUSEMENT_DEVICE_REGISTRY_MISSING）。
     */
    public PageResponse<AmusementProjectDeviceRegistryBacklogRecord> listProjectsMissingDeviceRegistry() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.device_registry_backlog.viewed",
                "amusement.device_registry_backlog.view_failed",
                "amusement.session.manage",
                "amusement_project",
                () -> repository.listProjectsMissingDeviceRegistry(scope)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.session.manage")
    @Transactional
    public AmusementProjectSpaceAssignmentRecord assignProjectSpace(String projectId, AmusementProjectSpaceAssignmentRequest request) {
        String id = requireText(projectId, "project_id");
        return executeWrite(
                "assignAmusementProjectSpace:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementProjectSpaceAssignmentRecord.class,
                "amusement.project_space.assigned",
                "amusement.project_space.assign_failed",
                "amusement.session.manage",
                "amusement_project",
                () -> assignProjectSpaceInternal(id, request)
        );
    }

    private AmusementProjectSpaceAssignmentRecord assignProjectSpaceInternal(String projectId, AmusementProjectSpaceAssignmentRequest request) {
        AmusementProjectSpaceAssignmentRequest actual = requireRequest(request);
        String spaceId = requireText(actual.spaceId(), "space_id");
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
        checkProjectOwnership(projectId);
        // 空间必须存在；若属于某商户，必须与项目同商户（不可指派他商户空间，currentMerchantScope 隔离）。
        String spaceMerchant = repository.findSpaceMerchant(spaceId)
                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "铺位不存在或已被删除"));
        if (!spaceMerchant.isBlank() && project.merchantId() != null && !spaceMerchant.equals(project.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该铺位属于其他商户");
        }
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !spaceMerchant.isBlank() && !scope.get().equals(spaceMerchant)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该铺位属于其他商户");
        }
        if (repository.assignProjectSpace(projectId, spaceId) == 0) {
            throw ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除");
        }
        return new AmusementProjectSpaceAssignmentRecord(projectId, spaceId, Instant.now());
    }

    public AmusementOpenEligibilityResponse getOpenEligibility(String projectId) {
        String id = requireText(projectId, "project_id");
        checkProjectOwnership(id);
        try {
            AmusementOpenEligibilityResponse response = evaluateOpenEligibility(id, null, null);
            if (response.allowed()) {
                auditEventService.recordSuccess(
                        "amusement.open_eligibility.viewed",
                        "amusement.safety_inspection.view",
                        "amusement_project",
                        id,
                        null,
                        null,
                        Map.of("blocked_reasons", response.blockedReasons())
                );
            } else {
                auditEventService.recordFailure(
                        "amusement.open_eligibility.blocked",
                        "amusement.safety_inspection.view",
                        "amusement_project",
                        id,
                        null,
                        null,
                        String.join(",", response.blockedReasons())
                );
            }
            return response;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "amusement.open_eligibility.blocked",
                    "amusement.safety_inspection.view",
                    "amusement_project",
                    id,
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.project.manage")
    @Transactional
    public AmusementProjectRuleRecord createProjectRule(String projectId, AmusementProjectRuleRequest request) {
        String id = requireText(projectId, "project_id");
        return executeWrite(
                "createAmusementProjectRule:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementProjectRuleRecord.class,
                "amusement.project_rule.changed",
                "amusement.project_rule.changed_failed",
                "amusement.project.manage",
                "amusement_project_rule",
                () -> createProjectRuleInternal(id, request)
        );
    }

    /** 默认视图：只返回生效中绑定（已存档/已删除墓碑不展示，删除铁律「前台不显」）。 */
    public PageResponse<AmusementProjectEntitlementRecord> listProjectEntitlements(String projectId) {
        return listProjectEntitlements(projectId, false);
    }

    /** includeArchived=true：连同已存档与已删除墓碑一并返回，供后台「查看已归档」与恢复用。 */
    public PageResponse<AmusementProjectEntitlementRecord> listProjectEntitlements(String projectId, boolean includeArchived) {
        String id = requireText(projectId, "project_id");
        checkProjectOwnership(id);
        return readPage(
                "amusement.project_entitlement.viewed",
                "amusement.project_entitlement.view_failed",
                "amusement.project_entitlement.manage",
                "amusement_project_entitlement",
                () -> repository.listProjectEntitlements(id, includeArchived)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.project_entitlement.manage")
    @Transactional
    public AmusementProjectEntitlementRecord createProjectEntitlement(String projectId, AmusementProjectEntitlementRequest request) {
        String id = requireText(projectId, "project_id");
        return executeWrite(
                "createAmusementProjectEntitlement:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementProjectEntitlementRecord.class,
                "amusement.project_entitlement.changed",
                "amusement.project_entitlement.change_failed",
                "amusement.project_entitlement.manage",
                "amusement_project_entitlement",
                () -> createProjectEntitlementInternal(id, request)
        );
    }

    // ================= V341（v7 审计 R-12）游乐权益绑定：物理删 → 软删（范式照抄 V255/V260/V271/V272） =================

    /**
     * 存档（=原「解除绑定/停用」）：不删行，只打存档标记，理由必填。存档后核销绑定校验不再采信该绑定。
     * 权限 amusement.project_entitlement.manage（日常运营）；恢复见 {@link #restoreProjectEntitlement}。
     */
    @com.complex.platform.gate.GuardedAction(capability = "amusement.project_entitlement.manage")
    @Transactional
    public AmusementProjectEntitlementRecord archiveProjectEntitlement(
            String projectId, String entitlementType, String reason, String idempotencyKey) {
        String id = requireText(projectId, "project_id");
        String type = requireText(entitlementType, "entitlement_type");
        // 存档必填理由（业务HTML 2026-07-07「存档及删除需要填写存档及删除理由」）。
        String archiveReason = requireArchiveReason(reason, "存档理由不能为空");
        return executeWrite(
                "archiveAmusementProjectEntitlement:" + id + ":" + type,
                idempotencyKey,
                Map.of("project_id", id, "entitlement_type", type, "reason", archiveReason),
                AmusementProjectEntitlementRecord.class,
                "amusement.project_entitlement.changed",
                "amusement.project_entitlement.change_failed",
                "amusement.project_entitlement.manage",
                "amusement_project_entitlement",
                () -> {
                    checkProjectOwnership(id);
                    AmusementProjectEntitlementRecord existing = requireProjectEntitlement(id, type);
                    if (existing.archivedAt() != null) {
                        throw ApiException.conflict("PROJECT_ENTITLEMENT_ALREADY_ARCHIVED", "该权益绑定已存档");
                    }
                    if (!"none".equals(existing.deleteStatus())) {
                        throw ApiException.conflict("PROJECT_ENTITLEMENT_DELETE_IN_PROGRESS", "该权益绑定已进入删除流程");
                    }
                    repository.archiveProjectEntitlement(id, type, archiveReason, currentActorId());
                    return requireProjectEntitlement(id, type);
                }
        );
    }

    /**
     * 恢复（取消存档 / 墓碑复活）：清存档标记 + 删除轴回退 none。
     * 已被审批通过删除（墓碑）的绑定只能由最高权限本人恢复（BSM 第 570 行）。
     */
    @com.complex.platform.gate.GuardedAction(capability = "amusement.project_entitlement.manage")
    @Transactional
    public AmusementProjectEntitlementRecord restoreProjectEntitlement(
            String projectId, String entitlementType, String idempotencyKey) {
        String id = requireText(projectId, "project_id");
        String type = requireText(entitlementType, "entitlement_type");
        return executeWrite(
                "restoreAmusementProjectEntitlement:" + id + ":" + type,
                idempotencyKey,
                Map.of("project_id", id, "entitlement_type", type),
                AmusementProjectEntitlementRecord.class,
                "amusement.project_entitlement.changed",
                "amusement.project_entitlement.change_failed",
                "amusement.project_entitlement.manage",
                "amusement_project_entitlement",
                () -> {
                    boolean master = isMasterActor();
                    if (!master) {
                        checkProjectOwnership(id);
                    }
                    AmusementProjectEntitlementRecord existing = requireProjectEntitlement(id, type);
                    if (existing.active()) {
                        throw ApiException.conflict("PROJECT_ENTITLEMENT_NOT_ARCHIVED", "该权益绑定处于生效中，无需恢复");
                    }
                    if ("approved".equals(existing.deleteStatus()) && !master) {
                        // 墓碑复活=撤销一次最高权限批准的删除，只认最高权限本人（BSM 第 570 行）。
                        throw ApiException.forbidden("MASTER_APPROVAL_REQUIRED", "已删除的项目权益绑定须由最高权限本人恢复");
                    }
                    repository.restoreProjectEntitlement(id, type);
                    return requireProjectEntitlement(id, type);
                }
        );
    }

    /** 发起删除申请：须先存档（防误删在用绑定）、理由必填；状态 none→pending。 */
    @com.complex.platform.gate.GuardedAction(capability = "amusement.project_entitlement.manage")
    @Transactional
    public AmusementProjectEntitlementRecord requestDeleteProjectEntitlement(
            String projectId, String entitlementType, String reason, String idempotencyKey) {
        String id = requireText(projectId, "project_id");
        String type = requireText(entitlementType, "entitlement_type");
        String deleteReason = requireArchiveReason(reason, "删除理由不能为空");
        return executeWrite(
                "requestDeleteAmusementProjectEntitlement:" + id + ":" + type,
                idempotencyKey,
                Map.of("project_id", id, "entitlement_type", type, "reason", deleteReason),
                AmusementProjectEntitlementRecord.class,
                "amusement.project_entitlement.changed",
                "amusement.project_entitlement.change_failed",
                "amusement.project_entitlement.manage",
                "amusement_project_entitlement",
                () -> {
                    boolean master = isMasterActor();
                    if (!master) {
                        checkProjectOwnership(id);
                    }
                    AmusementProjectEntitlementRecord existing = requireProjectEntitlement(id, type);
                    if (existing.archivedAt() == null) {
                        throw ApiException.badRequest("PROJECT_ENTITLEMENT_NOT_ARCHIVED", "请先存档再申请删除");
                    }
                    if (!"none".equals(existing.deleteStatus())) {
                        throw ApiException.conflict("PROJECT_ENTITLEMENT_DELETE_ALREADY_PENDING", "该权益绑定已有待审批的删除申请");
                    }
                    repository.requestDeleteProjectEntitlement(id, type, deleteReason, currentActorId());
                    if (master) {
                        // BSM 第 570 行：持授权 U 盘密钥的最高权限本人发起的删除不需要经过任何审批。
                        repository.decideDeleteProjectEntitlement(id, type, true);
                    }
                    return requireProjectEntitlement(id, type);
                }
        );
    }

    /**
     * 审批删除申请：approved → delete_status='approved'（墓碑，行留库、全读取路径过滤、可恢复，不物理删）；
     * rejected → 回 none。审批人只认最高权限本人（BSM 第 570 行），且申请人禁自审。
     */
    @com.complex.platform.gate.GuardedAction(capability = "amusement.project_entitlement.approve")
    @Transactional
    public AmusementProjectEntitlementRecord decideDeleteProjectEntitlement(
            String projectId, String entitlementType, String decision, String idempotencyKey) {
        String id = requireText(projectId, "project_id");
        String type = requireText(entitlementType, "entitlement_type");
        String verdict = decision == null ? null : decision.trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(verdict) && !"rejected".equals(verdict)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
        }
        if (!isMasterActor()) {
            throw ApiException.forbidden("MASTER_APPROVAL_REQUIRED", "项目权益绑定删除须由最高权限本人批准");
        }
        return executeWrite(
                "decideDeleteAmusementProjectEntitlement:" + id + ":" + type,
                idempotencyKey,
                Map.of("project_id", id, "entitlement_type", type, "decision", verdict),
                AmusementProjectEntitlementRecord.class,
                "amusement.project_entitlement.changed",
                "amusement.project_entitlement.change_failed",
                "amusement.project_entitlement.approve",
                "amusement_project_entitlement",
                () -> {
                    AmusementProjectEntitlementRecord existing = requireProjectEntitlement(id, type);
                    if (!"pending".equals(existing.deleteStatus())) {
                        throw ApiException.badRequest("PROJECT_ENTITLEMENT_DELETE_NOT_PENDING", "该权益绑定没有待审批的删除申请");
                    }
                    // 职责分离：删除申请发起人不得审批自己的申请（照 supervisor_review SELF_REVIEW_FORBIDDEN 原则）。
                    String actorId = currentActorId();
                    if (actorId != null && actorId.equals(existing.deleteRequestedBy())) {
                        throw ApiException.forbidden("PROJECT_ENTITLEMENT_SELF_APPROVE_FORBIDDEN", "删除申请发起人不能审批自己的申请");
                    }
                    repository.decideDeleteProjectEntitlement(id, type, "approved".equals(verdict));
                    return requireProjectEntitlement(id, type);
                }
        );
    }

    /** BSM 第 570 行：配置数据删除审批与墓碑恢复只认持授权 U 盘密钥的最高权限本人；这些单据级路径对其逐笔放行（经 executeWrite 留痕）。 */
    private boolean isMasterActor() {
        return authorizationService.isMasterAuthority(authorizationService.currentActor());
    }

    private AmusementProjectEntitlementRecord requireProjectEntitlement(String projectId, String entitlementType) {
        return repository.findProjectEntitlement(projectId, entitlementType)
                .orElseThrow(() -> ApiException.notFound("PROJECT_ENTITLEMENT_NOT_FOUND", "游乐项目权益绑定不存在或已被删除"));
    }

    /** 存档/删除理由必填（空白即 400），与 V271 全域口径一致。 */
    private String requireArchiveReason(String reason, String message) {
        String trimmed = trimToNull(reason);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", message);
        }
        return trimmed;
    }

    private String currentActorId() {
        try {
            var actor = authorizationService.currentActor();
            return actor == null ? null : actor.id();
        } catch (RuntimeException ignored) {
            // 无鉴权上下文（内部调用/单测）：不阻断软删主链；操作者留空，留痕由 audit_events 兜底。
            // 此时禁自审闸自动跳过（照 TicketCatalogService「actorId 为 null 时跳过该闸」既有口径）。
            return null;
        }
    }

    public PageResponse<AmusementCapacityConfigRecord> listCapacityConfigs(String projectId) {
        String id = requireText(projectId, "project_id");
        checkProjectOwnership(id);
        return readPage(
                "amusement.capacity_config.viewed",
                "amusement.capacity_config.view_failed",
                "amusement.capacity_config.manage",
                "amusement_capacity_config",
                () -> repository.listCapacityConfigs(id)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.capacity_config.manage")
    @Transactional
    public AmusementCapacityConfigRecord createCapacityConfig(String projectId, AmusementCapacityConfigRequest request) {
        String id = requireText(projectId, "project_id");
        if (sessionLifecycle != null) sessionLifecycle.requireProjectScope(id, "amusement.capacity_config.manage");
        return executeWrite(
                "createAmusementCapacityConfig:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementCapacityConfigRecord.class,
                "amusement.capacity_config.changed",
                "amusement.capacity_config.change_failed",
                "amusement.capacity_config.manage",
                "amusement_capacity_config",
                () -> createCapacityConfigInternal(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.capacity_config.manage")
    @Transactional
    public AmusementCapacityConfigRecord disableCapacityConfig(String configId, String idempotencyKey) {
        String id = requireText(configId, "config_id");
        return executeWrite(
                "disableAmusementCapacityConfig:" + id,
                idempotencyKey,
                Map.of("config_id", id),
                AmusementCapacityConfigRecord.class,
                "amusement.capacity_config.changed",
                "amusement.capacity_config.change_failed",
                "amusement.capacity_config.manage",
                "amusement_capacity_config",
                () -> disableCapacityConfigInternal(id)
        );
    }

    public PageResponse<AmusementDeviceRecord> listDevices(String projectId) {
        String id = requireText(projectId, "project_id");
        checkProjectOwnership(id);
        return readPage(
                "amusement.device.viewed",
                "amusement.device.view_failed",
                "amusement.device.view",
                "amusement_device",
                () -> repository.listDevicesByProject(id)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.device.manage")
    @Transactional
    public AmusementDeviceRecord createDevice(String projectId, AmusementDeviceRequest request) {
        String id = requireText(projectId, "project_id");
        return executeWrite(
                "createAmusementDevice:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementDeviceRecord.class,
                "amusement.device.changed",
                "amusement.device.change_failed",
                "amusement.device.manage",
                "amusement_device",
                () -> createDeviceInternal(id, request)
        );
    }

    /**
     * P0 清单#6 读「超容处置策略」（V104）。未配置返回禁用占位（与既有读路径
     * {@link AmusementRepository#findOverCapacityPolicy(String)} 语义一致，向后兼容）。
     * 权限点 amusement.over_capacity_policy.view；审计 amusement.over_capacity_policy.viewed。
     */
    public AmusementOverCapacityPolicy getOverCapacityPolicy(String projectId) {
        String id = requireText(projectId, "project_id");
        try {
            repository.findProjectById(id)
                    .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
            checkProjectOwnership(id);
            AmusementOverCapacityPolicy policy = repository.findOverCapacityPolicy(id);
            auditEventService.recordSuccess(
                    "amusement.over_capacity_policy.viewed",
                    "amusement.over_capacity_policy.view",
                    "amusement_over_capacity_policy",
                    id, null, null, Map.of());
            return policy;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "amusement.over_capacity_policy.view_failed",
                    "amusement.over_capacity_policy.view",
                    "amusement_over_capacity_policy",
                    id, null, null, codeOf(exception));
            throw exception;
        }
    }

    /**
     * P0 清单#6 配置（幂等 upsert）「超容处置策略」（V104 写入侧）。
     * 校验 project 存在/归属、disposal_mode 枚举、operating_capacity>=0；按 project_id 幂等 upsert。
     * 权限点 amusement.over_capacity_policy.configure（risk 偏高）；审计 amusement.over_capacity_policy.configured。
     */
    @com.complex.platform.gate.GuardedAction(capability = "amusement.over_capacity_policy.configure")
    @Transactional
    public AmusementOverCapacityPolicy configureOverCapacityPolicy(
            String projectId, ConfigureOverCapacityPolicyRequest request) {
        String id = requireText(projectId, "project_id");
        return executeWrite(
                "configureAmusementOverCapacityPolicy:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementOverCapacityPolicy.class,
                "amusement.over_capacity_policy.configured",
                "amusement.over_capacity_policy.configure_failed",
                "amusement.over_capacity_policy.configure",
                "amusement_over_capacity_policy",
                () -> configureOverCapacityPolicyInternal(id, request)
        );
    }

    private AmusementOverCapacityPolicy configureOverCapacityPolicyInternal(
            String projectId, ConfigureOverCapacityPolicyRequest request) {
        ConfigureOverCapacityPolicyRequest actual = requireRequest(request);
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
        checkProjectOwnership(projectId);
        int operatingCapacity = nonNegativeOrDefault(actual.operatingCapacity(), 0, "operating_capacity");
        String disposalMode = enumOrDefault(
                actual.disposalMode(),
                AmusementOverCapacityPolicy.MODE_BLOCK_HARD,
                Set.of(AmusementOverCapacityPolicy.MODE_BLOCK_HARD, AmusementOverCapacityPolicy.MODE_WARN_ONLY),
                "disposal_mode");
        boolean enabled = actual.enabled() == null || actual.enabled();
        return repository.saveOverCapacityPolicy(new AmusementOverCapacityPolicy(
                projectId, operatingCapacity, disposalMode, enabled));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REQ-53 权益会话（V129 entitlement_sessions）：发起 → 展示 → 领取核销（防错领 + 会话时效）。
    // ─────────────────────────────────────────────────────────────────────────

    private static final Set<String> ENTITLEMENT_SESSION_CLAIMABLE = Set.of("initiated", "displayed");

    public PageResponse<EntitlementSessionRecord> listEntitlementSessions() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.entitlement_session.listed",
                "amusement.entitlement_session.list_failed",
                "amusement.entitlement_session.view",
                "entitlement_session",
                scope.isPresent()
                        ? () -> repository.listEntitlementSessionsByMerchant(scope.get())
                        : repository::listEntitlementSessions
        );
    }

    /**
     * REQ-53 发起会话（工作手机扫附赠权益码）。status=initiated，merchant 强制注入，幂等。
     * 校验：entitlement_ref 非空、ttl_seconds>0。expires_at = now + ttlSeconds。
     */
    @com.complex.platform.gate.GuardedAction(capability = "amusement.entitlement_session.initiate")
    @Transactional
    public EntitlementSessionRecord initiateEntitlementSession(InitiateEntitlementSessionRequest request) {
        return executeWrite(
                "initiateEntitlementSession",
                request == null ? null : request.idempotencyKey(),
                request,
                EntitlementSessionRecord.class,
                "amusement.entitlement_session.initiated",
                "amusement.entitlement_session.initiate_failed",
                "amusement.entitlement_session.initiate",
                "entitlement_session",
                () -> initiateEntitlementSessionInternal(request)
        );
    }

    private EntitlementSessionRecord initiateEntitlementSessionInternal(InitiateEntitlementSessionRequest request) {
        InitiateEntitlementSessionRequest actual = requireRequest(request);
        String entitlementRef = requireText(actual.entitlementRef(), "entitlement_ref");
        int ttlSeconds = positiveInteger(actual.ttlSeconds(), "ttl_seconds");
        String projectId = trimToNull(actual.projectId());
        if (projectId != null) {
            repository.findProjectById(projectId)
                    .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
            checkProjectOwnership(projectId);
        }
        // merchant 强制注入（direct scope）；无 scope（平台视角）退化为 null。
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        String slug = slug(actual.idempotencyKey());
        return repository.saveEntitlementSession(new EntitlementSessionRecord(
                "esession_" + slug,
                "escode_" + slug,
                entitlementRef,
                merchantId,
                projectId,
                "initiated",
                now.plusSeconds(ttlSeconds),
                trimToDefault(actual.initiatedBy(), "work_phone"),
                null,
                null,
                now,
                now
        ));
    }

    /**
     * REQ-53 一体机查询展示（按 id 或 session_code）。读时把 initiated → displayed（条件 UPDATE，幂等读语义）。
     * 强制 merchant 归属隔离。
     */
    public EntitlementSessionRecord getEntitlementSession(String idOrCode) {
        String key = requireText(idOrCode, "id");
        try {
            EntitlementSessionRecord session = repository.findEntitlementSessionById(key)
                    .or(() -> repository.findEntitlementSessionByCode(key))
                    .orElseThrow(() -> ApiException.notFound(
                            "ENTITLEMENT_SESSION_NOT_FOUND", "权益核销会话不存在或已被删除"));
            checkEntitlementSessionOwnership(session);
            // 一体机首次展示：initiated → displayed（仅 initiated 命中；已展示/已领/过期/取消保持原状）。
            if ("initiated".equals(session.status())) {
                repository.markEntitlementSessionDisplayed(session.id(), Instant.now());
                session = repository.findEntitlementSessionById(session.id()).orElse(session);
            }
            auditEventService.recordSuccess(
                    "amusement.entitlement_session.viewed",
                    "amusement.entitlement_session.view",
                    "entitlement_session",
                    session.id(), null, null, Map.of("status", session.status()));
            return session;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "amusement.entitlement_session.view_failed",
                    "amusement.entitlement_session.view",
                    "entitlement_session",
                    key, null, null, codeOf(exception));
            throw exception;
        }
    }

    /**
     * REQ-53 一体机领取核销（防错领条件 UPDATE：仅 initiated/displayed 且未过期可领，→claimed）。幂等。
     * 过期 / 已领 / 重复领 / 已取消均拒（条件 UPDATE 0 行 → ENTITLEMENT_SESSION_NOT_CLAIMABLE）。
     */
    @com.complex.platform.gate.GuardedAction(capability = "amusement.entitlement_session.claim")
    @Transactional
    public EntitlementSessionRecord claimEntitlementSession(String id, ClaimEntitlementSessionRequest request) {
        String sessionId = requireText(id, "id");
        if (locationGuard != null) {
            locationGuard.check("claimAmusementEntitlementSession", () -> repository.findEntitlementSessionById(sessionId)
                    .map(session -> session.projectId() != null ? amusementBusinessDayService.complexId(session.projectId())
                            : locationGuard.projectOfMerchant(session.merchantId())).orElse(null));
        }
        return executeWrite(
                "claimEntitlementSession:" + sessionId,
                request == null ? null : request.idempotencyKey(),
                request,
                EntitlementSessionRecord.class,
                "amusement.entitlement_session.claimed",
                "amusement.entitlement_session.claim_failed",
                "amusement.entitlement_session.claim",
                "entitlement_session",
                () -> claimEntitlementSessionInternal(sessionId, request)
        );
    }

    private EntitlementSessionRecord claimEntitlementSessionInternal(String sessionId, ClaimEntitlementSessionRequest request) {
        ClaimEntitlementSessionRequest actual = requireRequest(request);
        EntitlementSessionRecord session = repository.findEntitlementSessionById(sessionId)
                .orElseThrow(() -> ApiException.notFound(
                        "ENTITLEMENT_SESSION_NOT_FOUND", "权益核销会话不存在或已被删除"));
        checkEntitlementSessionOwnership(session);
        String claimedBy = trimToDefault(actual.claimedBy(), "kiosk");
        Instant now = Instant.now();
        // 防错领核心：条件 UPDATE 受影响 1 行才成功；过期/已领/重复领/已取消均 0 行被拒。
        if (!repository.claimEntitlementSession(sessionId, claimedBy, now)) {
            // 区分过期与状态非法，给出明确错误码（便于一体机提示）。
            if (session.expiresAt() != null && !now.isBefore(session.expiresAt())
                    && ENTITLEMENT_SESSION_CLAIMABLE.contains(session.status())) {
                throw ApiException.conflict("ENTITLEMENT_SESSION_EXPIRED", "权益核销会话已过期");
            }
            throw ApiException.conflict("ENTITLEMENT_SESSION_NOT_CLAIMABLE",
                    "权益核销会话当前不可核销（状态：" + session.status() + "）");
        }
        return repository.findEntitlementSessionById(sessionId).orElse(session);
    }

    private void checkEntitlementSessionOwnership(EntitlementSessionRecord session) {
        authorizationService.currentMerchantScope().ifPresent(merchantId -> {
            if (!merchantId.equals(session.merchantId())) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "该权益核销会话不属于当前商户");
            }
        });
    }

    public PageResponse<AmusementQueueEntryRecord> listQueueEntries(int page, int size) {
        return bookingService.listQueueEntries(page, size);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.queue.manage")
    @Transactional
    public AmusementQueueEntryRecord createQueueEntry(AmusementQueueEntryRequest request) {
        return bookingService.createQueueEntry(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.queue.manage")
    @Transactional
    public AmusementQueueEntryRecord changeQueueEntryStatus(String queueEntryId, StatusChangeRequest request) {
        return bookingService.changeQueueEntryStatus(queueEntryId, request);
    }

    public PageResponse<AmusementReservationRecord> listReservations(int page, int size) {
        return bookingService.listReservations(page, size);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.reservation.manage")
    @Transactional
    public AmusementReservationRecord createReservation(AmusementReservationRequest request) {
        return bookingService.createReservation(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.reservation.manage")
    @Transactional
    public AmusementReservationRecord changeReservationStatus(String reservationId, StatusChangeRequest request) {
        return bookingService.changeReservationStatus(reservationId, request);
    }

    public PageResponse<AmusementGroupBookingRecord> listGroupBookings(int page, int size) {
        return bookingService.listGroupBookings(page, size);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.group_booking.manage")
    @Transactional
    public AmusementGroupBookingRecord createGroupBooking(AmusementGroupBookingRequest request) {
        return bookingService.createGroupBooking(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.group_booking.approve", admissionAction = "amusement.group_booking_approve", objectType = "amusement_group_booking")
    @Transactional
    public AmusementGroupBookingRecord approveGroupBooking(String bookingId, AmusementGroupBookingApprovalRequest request) {
        return bookingService.approveGroupBooking(bookingId, request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.group_booking.manage")
    @Transactional
    public AmusementGroupBookingRecord releaseGroupBooking(String bookingId, AmusementGroupBookingReleaseRequest request) {
        return bookingService.releaseGroupBooking(bookingId, request);
    }

    public PageResponse<AmusementSafetyInspectionRecord> listSafetyInspections() {
        return inspectionService.listSafetyInspections();
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_inspection.create")
    @Transactional
    public AmusementSafetyInspectionRecord createSafetyInspection(AmusementSafetyInspectionRequest request) {
        return inspectionService.createSafetyInspection(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_inspection.submit")
    @Transactional
    public AmusementSafetyInspectionRecord submitSafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        return inspectionService.submitSafetyInspection(inspectionId, request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_inspection.rectify")
    @Transactional
    public AmusementSafetyInspectionRecord rectifySafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        return inspectionService.rectifySafetyInspection(inspectionId, request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_inspection.recheck")
    @Transactional
    public AmusementSafetyInspectionRecord recheckSafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        return inspectionService.recheckSafetyInspection(inspectionId, request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_inspection.approve_reopen", admissionAction = "amusement.reopen", objectType = "inspection")
    @Transactional
    public AmusementSafetyInspectionRecord approveReopen(String inspectionId, StatusChangeRequest request) {
        return inspectionService.approveReopen(inspectionId, request);
    }

    public PageResponse<AmusementMaintenanceRecord> listMaintenanceRecords() {
        return maintenanceService.listMaintenanceRecords();
    }

    public AmusementMaintenanceRecord getMaintenanceRecord(String recordId) {
        return maintenanceService.getMaintenanceRecord(recordId);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.maintenance.submit")
    @Transactional
    public AmusementMaintenanceRecord createMaintenanceRecord(AmusementMaintenanceRecordRequest request) {
        return maintenanceService.createMaintenanceRecord(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.maintenance.confirm", admissionAction = "amusement.reopen", objectType = "amusement_maintenance_record")
    @Transactional
    public AmusementMaintenanceRecord confirmMaintenanceResume(String recordId, AmusementMaintenanceResumeRequest request) {
        return maintenanceService.confirmMaintenanceResume(recordId, request);
    }

    public PageResponse<AmusementSafetyTemplateRecord> listSafetyTemplates() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.safety_template.viewed",
                "amusement.safety_template.view_failed",
                "amusement.safety_template.view",
                "amusement_safety_template",
                scope.isPresent() ? () -> repository.listSafetyTemplatesByMerchant(scope.get()) : repository::listSafetyTemplates
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.safety_template.manage")
    @Transactional
    public AmusementSafetyTemplateRecord createSafetyTemplate(AmusementSafetyTemplateRequest request) {
        return executeWrite(
                "createAmusementSafetyTemplate",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSafetyTemplateRecord.class,
                "amusement.safety_template.changed",
                "amusement.safety_template.change_failed",
                "amusement.safety_template.manage",
                "amusement_safety_template",
                () -> createSafetyTemplateInternal(request)
        );
    }

    public PageResponse<AmusementSessionRecord> listSessions() {
        return readPage(
                "amusement.session.viewed",
                "amusement.session.view_failed",
                "amusement.session.view",
                "amusement_session",
                () -> {
                    authorizationService.requirePermission("amusement.session.view");
                    var actor = authorizationService.currentActor();
                    var reach = authorizationService.projectReach(actor, "amusement.session.view");
                    String merchant = trimToNull(actor.merchantId());
                    // 第十八批 E 段 20-R4（治理地图第 659 行「一个商户在两个或更多项目中有经营的，看到的是每一个项目内他经营的商户」）：商户账号只按商户取，不再限本人项目
                    return visibleProjectRows(repository.listSessionsScoped(reach.platformAdmin() || merchant != null,
                                    reach.organizationIds(), merchant),
                            AmusementSessionRecord::projectId, reach, merchant);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.session.manage")
    @Transactional
    public AmusementSessionRecord createSession(AmusementSessionRequest request) {
        if (sessionLifecycle != null) sessionLifecycle.requireProjectScope(requireRequest(request).projectId(), "amusement.session.manage");
        return executeWrite(
                "createAmusementSession",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSessionRecord.class,
                "amusement.session.changed",
                "amusement.session.change_failed",
                "amusement.session.manage",
                "amusement_session",
                () -> createSessionInternal(request)
        );
    }

    public PageResponse<AmusementVerificationRecord> listVerifications() {
        return readPage(
                "amusement.verification.viewed",
                "amusement.verification.view_failed",
                "amusement.verification.view",
                "amusement_verification",
                () -> visibleProjectRows(repository.listVerifications(), AmusementVerificationRecord::projectId, "amusement.verification.view")
        );
    }

    /** 按项目/场次/方向/日期筛选核销记录，并取综合体权限与商户身份范围的交集。 */
    public PageResponse<AmusementVerificationListItem> listVerificationItems(AmusementVerificationQuery query) {
        return readPage(
                "amusement.verification.viewed",
                "amusement.verification.view_failed",
                "amusement.verification.view",
                "amusement_verification",
                () -> visibleProjectRows(repository.listVerificationItems(query, authorizationService.currentActor().merchantId()),
                        AmusementVerificationListItem::projectId, "amusement.verification.view")
        );
    }

    /** 项目实时在玩人数快照，仅返回当前操作人有权查看的综合体和商户。 */
    public PageResponse<AmusementCapacitySnapshotRecord> listCapacitySnapshots() {
        return readPage(
                "amusement.verification.viewed",
                "amusement.verification.view_failed",
                "amusement.verification.view",
                "amusement_capacity_snapshot",
                () -> visibleProjectRows(repository.listCapacitySnapshots(authorizationService.currentActor().merchantId()).stream()
                        .filter(row -> "project".equals(row.dimension())).toList(),
                        AmusementCapacitySnapshotRecord::projectId, "amusement.verification.view")
        );
    }

    private <T> List<T> visibleProjectRows(List<T> rows, Function<T, String> projectId, String permission) {
        authorizationService.requirePermission(permission);
        var actor = authorizationService.currentActor();
        var reach = authorizationService.projectReach(actor, permission);
        return visibleProjectRows(rows, projectId, reach, trimToNull(actor.merchantId()));
    }

    private <T> List<T> visibleProjectRows(List<T> rows, Function<T, String> projectId,
            AuthorizationService.ProjectReach reach, String merchant) {
        Map<String, Boolean> visible = new java.util.HashMap<>();
        return rows.stream().filter(row -> visible.computeIfAbsent(projectId.apply(row), id -> {
            var project = repository.findProjectById(id).orElse(null);
            if (project == null || (merchant != null && !merchant.equals(project.merchantId()))) return false;
            // 第十八批 E 段 20-R4（治理地图第 659 行「一个商户在两个或更多项目中有经营的，看到的是每一个项目内他经营的商户」）：
            // 商户账号（非平台管理员）只按商户，不再限本人项目；本方法只用于读。
            if (merchant != null && !reach.platformAdmin()) return true;
            try {
                return reach.covers(amusementBusinessDayService.complexId(id));
            } catch (ApiException unresolved) {
                if ("AMUSEMENT_BUSINESS_DAY_CONFIGURATION_REQUIRED".equals(unresolved.code())
                        || "AMUSEMENT_PROJECT_NOT_FOUND".equals(unresolved.code())) return false;
                throw unresolved;
            }
        })).toList();
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.verification.execute")
    @Transactional
    public AmusementVerificationRecord createVerification(VerificationRequest request) {
        if (sessionLifecycle != null) {
            VerificationRequest actual = requireRequest(request);
            sessionLifecycle.requireVerificationScope(requireText(textFromMap(actual.businessContext(),
                    "session_id", "sessionId"), "session_id"), "amusement.verification.execute");
        }
        if (locationGuard != null) {
            locationGuard.check("createAmusementVerification", () -> amusementBusinessDayService.complexId(textFromMap(
                    requireRequest(request).businessContext(), "project_id", "projectId", "business_id", "businessId")));
        }
        return executeWrite(
                "createAmusementVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementVerificationRecord.class,
                "amusement.verification_succeeded",
                "amusement.verification_failed",
                "amusement.verification.execute",
                "amusement_verification",
                () -> createVerificationInternal(request)
        );
    }

    private AmusementSafetyTemplateRecord createSafetyTemplateInternal(AmusementSafetyTemplateRequest request) {
        AmusementSafetyTemplateRequest actual = requireRequest(request);
        if (actual.items() == null || actual.items().isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "items is required");
        }
        Instant now = Instant.now();
        return repository.saveSafetyTemplate(new AmusementSafetyTemplateRecord(
                "template_" + slug(actual.idempotencyKey()),
                requireText(actual.templateName(), "template_name"),
                List.copyOf(actual.items()),
                actual.effectiveFrom() == null ? LocalDate.now() : actual.effectiveFrom(),
                "active",
                evidence(actual.evidenceAttachmentIds()),
                now,
                now,
                authorizationService.currentMerchantScope().orElse(null)
        ));
    }

    private AmusementProjectRecord createProjectInternal(AmusementProjectRequest request) {
        AmusementProjectRequest actual = requireRequest(request);
        String templateId = trimToNull(actual.safetyTemplateId());
        if (templateId != null) {
            repository.findSafetyTemplateById(templateId)
                    .orElseThrow(() -> ApiException.notFound("SAFETY_TEMPLATE_NOT_FOUND", "安检模板不存在或已被删除"));
        }
        Instant now = Instant.now();
        return repository.saveProject(new AmusementProjectRecord(
                "project_" + slug(actual.idempotencyKey()),
                requireText(actual.projectName(), "project_name"),
                positiveInteger(actual.capacity(), "capacity"),
                enumOrDefault(actual.openStatus(), "open", PROJECT_STATUSES, "open_status"),
                templateId,
                actual.eligibilityRule() == null ? Map.of() : Map.copyOf(actual.eligibilityRule()),
                trimToNull(actual.locationPointId()),
                now,
                now,
                authorizationService.currentMerchantScope().orElse(null)
        ));
    }

    private AmusementSessionRecord createSessionInternal(AmusementSessionRequest request) {
        AmusementSessionRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
        checkProjectOwnership(projectId);
        Instant start = actual.startTime(), end = actual.endTime();
        if (start == null || end == null) throw ApiException.badRequest("VALIDATION_FAILED", "场次开始和结束时间必填");
        if (actual.capacity() != null && actual.capacity() != 0)
            throw ApiException.badRequest("SESSION_CAPACITY_RETIRED", "场次不再设置独立硬容量，请维护项目硬安全容量");
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "结束时间必须晚于开始时间");
        }
        Instant now = Instant.now();
        AmusementSessionRecord saved = repository.saveSession(new AmusementSessionRecord(
                "session_" + slug(actual.idempotencyKey()),
                projectId,
                start,
                end,
                0,
                0,
                0,
                enumOrDefault(actual.status(), "scheduled", SESSION_STATUSES, "status"),
                evidence(actual.evidenceAttachmentIds()),
                now,
                now
        ));
        if (sessionLifecycle != null) sessionLifecycle.lockSessionWindow(saved);
        return saved;
    }

    private AmusementProjectRuleRecord createProjectRuleInternal(String projectId, AmusementProjectRuleRequest request) {
        AmusementProjectRuleRequest actual = requireRequest(request);
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
        checkProjectOwnership(projectId);
        String ruleType = enumOrDefault(actual.ruleType(), null, RULE_TYPES, "rule_type");
        Instant now = Instant.now();
        return repository.saveProjectRule(new AmusementProjectRuleRecord(
                "rule_" + slug(actual.idempotencyKey()),
                projectId,
                ruleType,
                requireText(actual.ruleValue(), "rule_value"),
                trimToNull(actual.effectiveFrom()),
                trimToNull(actual.effectiveTo()),
                actual.blocking() == null || actual.blocking(),
                "active",
                evidence(actual.evidenceAttachmentIds()),
                now,
                now
        ));
    }

    private AmusementProjectEntitlementRecord createProjectEntitlementInternal(String projectId, AmusementProjectEntitlementRequest request) {
        AmusementProjectEntitlementRequest actual = requireRequest(request);
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
        checkProjectOwnership(projectId);
        String entitlementType = requireText(actual.entitlementType(), "entitlement_type");
        // 仅允许绑定游乐类权益类型（与核销在线校验 §6.4/§12.1 口径一致）。
        if (!isAmusementEntitlementType(entitlementType)) {
            throw ApiException.badRequest("ENTITLEMENT_NOT_AMUSEMENT", "权益类型必须为游乐类权益");
        }
        return repository.saveProjectEntitlement(new AmusementProjectEntitlementRecord(
                "ament_" + slug(actual.idempotencyKey()),
                projectId,
                entitlementType,
                Instant.now()
        ));
    }

    private AmusementCapacityConfigRecord createCapacityConfigInternal(String projectId, AmusementCapacityConfigRequest request) {
        AmusementCapacityConfigRequest actual = requireRequest(request);
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
        checkProjectOwnership(projectId);
        throw ApiException.conflict("POINT_CAPACITY_RETIRED", "点位或设备不再设置重复硬容量，请维护项目硬安全容量");
    }

    private AmusementDeviceRecord createDeviceInternal(String projectId, AmusementDeviceRequest request) {
        AmusementDeviceRequest actual = requireRequest(request);
        checkProjectOwnership(projectId);
        String deviceCode = requireText(actual.deviceCode(), "device_code");
        String status = enumOrDefault(actual.status(), "active", DEVICE_STATUSES, "status");
        if (actual.safetyCheckIntervalHours() != null) {
            positiveInteger(actual.safetyCheckIntervalHours(), "safety_check_interval_hours");
        }
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        return repository.saveDevice(new AmusementDeviceRecord(
                "amusement_device_" + slug(actual.idempotencyKey()),
                projectId,
                deviceCode,
                trimToNull(actual.name()),
                status,
                actual.safetyCheckIntervalHours(),
                trimToNull(actual.locationPointId()),
                null,
                merchantId,
                now,
                now
        ));
    }

    private AmusementCapacityConfigRecord disableCapacityConfigInternal(String configId) {
        AmusementCapacityConfigRecord existing = repository.findCapacityConfigById(configId)
                .orElseThrow(() -> ApiException.notFound("CAPACITY_CONFIG_NOT_FOUND", "容量配置不存在或已被删除"));
        checkProjectOwnership(existing.projectId());
        return repository.saveCapacityConfig(new AmusementCapacityConfigRecord(
                existing.id(),
                existing.projectId(),
                existing.locationPointId(),
                existing.deviceId(),
                existing.maxConcurrent(),
                existing.effectiveSessionPattern(),
                false,
                existing.createdAt(),
                Instant.now()
        ));
    }

    private AmusementVerificationRecord createVerificationInternal(VerificationRequest request) {
        VerificationRequest actual = requireRequest(request);
        String credential = requireText(actual.credentialPayload(), "credential_payload");
        String projectId = requireText(textFromMap(actual.businessContext(), "project_id", "projectId", "business_id", "businessId"), "project_id");
        String sessionId = requireText(textFromMap(actual.businessContext(), "session_id", "sessionId"), "session_id");
        String staffId = trimToDefault(textFromMap(actual.businessContext(), "staff_id", "staffId"), "staff");
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
        if (sessionLifecycle != null) sessionLifecycle.requireProjectScope(projectId, "amusement.verification.execute");
        else checkProjectOwnership(projectId);
        AmusementSessionRecord session = repository.findSessionById(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "场次不存在或已被删除"));
        if (!project.id().equals(session.projectId())) {
            throw ApiException.conflict("SESSION_PROJECT_MISMATCH", "场次不属于该游乐项目");
        }
        // 开放状态和时段只限制新的入场；已完成请求仍须经共用幂等及实时权限核验。
        // F-OPS-04：核销针对具体设备时传 deviceId，准入闸校验该设备巡检有效期（过期则 SAFETY_INSPECTION_EXPIRED）。
        String deviceId = trimToNull(actual.deviceId());
        String pointId = trimToNull(textFromMap(actual.businessContext(),
                "location_point_id", "locationPointId", "point_id", "pointId"));
        return runAmusementVerification(
                project, session, credential, pointId, deviceId, staffId, actual.idempotencyKey(), Instant.now(),
                "amusement.verification.execute");
    }

    /** 票务预览只读定位真实项目及生效营业日，不能激活或消费权益。 */
    public AmusementBusinessDayService.Window requireEntryWindow(String pointId, String deviceId, Instant at) {
        String projectId = repository.findProjectIdByPointOrDevice(trimToNull(pointId), trimToNull(deviceId))
                .orElseThrow(() -> ApiException.conflict("AMUSEMENT_PROJECT_UNRESOLVED",
                        "游乐核销须先明确所属项目的点位或设备"));
        sessionLifecycle.requireProjectScope(projectId, "verification.execute");
        return amusementBusinessDayService.requireWindow(projectId, at == null ? Instant.now() : at);
    }

    /**
     * 阶段1（2026-07-05 用户拍板）：票务核销 → 游乐正路分流入口。
     *
     * <p>依赖方向 ticketing → amusement（与 {@link AmusementEntitlementGateway} 相反的另一条单向边：
     * ticketing 直调本方法把游乐类权益核销分流进游乐正路；amusement 仍不依赖 ticketing 任何 Service。
     * 原为单实现、单消费者的分流端口，2026-08-02 收编为直连，语义不变）。
     *
     * <p>与 controller 直连路径（{@link #createVerificationInternal} 从 businessContext 取 project/session）不同，
     * 本路径【从 point/device + now 自动定位项目与当前 open 场次】——ticketing 侧检出游乐类权益后调本方法，
     * 复用同一份准入+项目硬容量+消费权益+写 amusement_verifications 的正路核心（{@link #runAmusementVerification}）。
     *
     * <p>返回语义：
     * <ul>
     *   <li>{@code Optional.empty()}：无法从点位或设备定位游乐项目，调用方应要求补齐真实归属，不能退回自然日核销。</li>
     *   <li>{@code Optional.of(record)}：已成功入场，返回原始事实及真实时间；票务侧读取同事务生成的关联核销记录。</li>
     * </ul>
     *
     * <p>【调用方事务内】执行（不加 @Transactional/@GuardedAction：核销权限已在 ticketing verify 端点授权，
     * 本方法仅承接分流，不作为独立高危动作二次门控）。一旦定位到游乐项目（即本核销确在游乐闸），后续任一步失败
     * （项目未开、当前无进行中场次、安全闸拦截、本场满等）即抛 {@link com.complex.platform.common.api.ApiException}，
     * 且【不净消费权益】（容量失败已补偿回补），由 ticketing 侧原样上抛。
     *
     * @param request 核销上下文（凭证 payload、point/device、staff、幂等键、now）
     */
    public Optional<AmusementVerificationRecord> verifyAmusementEntry(AmusementEntryRequest request) {
        AmusementEntryRequest actual = requireRequest(request);
        String credential = requireText(actual.credentialPayload(), "credential_payload");
        String pointId = trimToNull(actual.pointId());
        String deviceId = trimToNull(actual.deviceId());
        Instant now = actual.now() == null ? Instant.now() : actual.now();
        // 点位或设备必须能定位真实游乐项目；无法定位时返回空，由票务明确拒绝，不能推定日切。
        Optional<String> resolved = repository.findProjectIdByPointOrDevice(pointId, deviceId);
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        String projectId = resolved.get();
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
        if (sessionLifecycle != null) sessionLifecycle.requireProjectScope(projectId, "verification.execute");
        else checkProjectOwnership(projectId);
        // 当前场次属于新入场的处理结果，放在幂等首执行内解析，闭场后重试仍返回原件。
        String staffId = trimToDefault(actual.staffId(), "staff");
        AmusementVerificationRecord record = runAmusementVerification(
                project, null, credential, pointId, deviceId, staffId, actual.idempotencyKey(), now,
                "verification.execute");
        return Optional.of(record);
    }

    /**
     * 游乐核销正路核心（controller 直连 & ticketing 分流共用）：项目 open+巡检准入 → 识别/校验权益 →
     * 按锁定营业日激活或消费权益 → 项目实时硬容量占用 → 写游乐与票务首刷事实；重复入场不重复扣次。
     */
    private AmusementVerificationRecord runAmusementVerification(
            AmusementProjectRecord project,
            AmusementSessionRecord session,
            String credential,
            String pointId,
            String deviceId,
            String staffId,
            String idempotencyKey,
            Instant now,
            String verificationPermission) {
        if (sessionLifecycle != null && session != null)
            sessionLifecycle.requireVerificationScope(session.id(), verificationPermission);
        AmusementVerificationRecord result = idempotencyService.execute(
                "admitAmusementEntry", idempotencyKey,
                java.util.Arrays.asList(project.id(), credential, pointId, deviceId, staffId),
                () -> {
                    AmusementSessionRecord admissionSession = session == null
                            ? repository.resolveOpenSessionForProject(project.id(), now)
                                .orElseThrow(() -> ApiException.conflict(
                                        "AMUSEMENT_NO_OPEN_SESSION", "当前时间没有开放中的游乐场次"))
                            : session;
                    return performAmusementVerification(project, admissionSession, credential, pointId, deviceId, staffId,
                            idempotencyKey, now, verificationPermission);
                },
                AmusementVerificationRecord.class);
        if (sessionLifecycle != null) sessionLifecycle.requireVerificationScope(result.sessionId(), verificationPermission);
        // 直连入口显式指定场次；自动定位入口不把当前场次误当成重试请求的输入。
        if (session != null && !session.id().equals(result.sessionId()))
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "该请求编号已用于其他场次，本次操作未执行");
        return result;
    }

    private AmusementVerificationRecord performAmusementVerification(
            AmusementProjectRecord project,
            AmusementSessionRecord session,
            String credential,
            String pointId,
            String deviceId,
            String staffId,
            String idempotencyKey,
            Instant now,
            String verificationPermission) {
        AmusementBusinessDayService.Window businessWindow = null;
        if (sessionLifecycle != null) {
            var admission = sessionLifecycle.prepareEntry(project.id(), session.id(), verificationPermission);
            now = admission.occurredAt();
            businessWindow = admission.window();
            project = repository.findProjectById(project.id()).orElseThrow(() ->
                    ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在"));
            session = repository.findSessionById(session.id()).orElseThrow(() ->
                    ApiException.notFound("SESSION_NOT_FOUND", "游乐场次不存在"));
        }
        if (!"open".equals(project.openStatus())) {
            throw ApiException.conflict("PROJECT_NOT_OPEN", "该游乐项目未开放");
        }
        if (!project.id().equals(session.projectId())) {
            throw ApiException.conflict("SESSION_PROJECT_MISMATCH", "场次不属于该游乐项目");
        }
        if (!"open".equals(session.status())) {
            throw ApiException.conflict("SESSION_NOT_OPEN", "该场次未开放");
        }
        if (parkWideFaultSalesGate != null) {
            String complexId = businessWindow == null ? null : businessWindow.complexId();
            if (parkWideFaultSalesGate.salesSuspended(complexId))
                throw ApiException.conflict("PARK_WIDE_FAULT_VERIFICATION_SUSPENDED", "全园故障期间暂停游乐入场核销");
        }
        AmusementOpenEligibilityResponse eligibility = evaluateOpenEligibility(project.id(), null, deviceId);
        if (!eligibility.allowed()) {
            throw ApiException.conflict("SAFETY_BLOCKED", "安全闸未放行，游乐核销被拦截");
        }

        // §6.4 步骤 2/3：识别票/权益实例 → 在线校验（存在、active、有效期、剩余次数、游乐类、绑定本项目）。
        AmusementEntitlementView entitlement = entitlementGateway.findEntitlementByCredential(credential)
                .orElseThrow(() -> ApiException.notFound("TICKET_NOT_FOUND", "票券权益不存在或已被删除"));
        if (parkWideFaultSalesGate != null && entitlement.complexProjectId() != null
                && parkWideFaultSalesGate.salesSuspended(entitlement.complexProjectId()))
            throw ApiException.conflict("PARK_WIDE_FAULT_VERIFICATION_SUSPENDED", "原门票所属综合体全园故障，暂停游乐入场核销");
        if (!"active".equals(entitlement.status())) {
            throw ApiException.conflict("ENTITLEMENT_USED", "该权益未生效");
        }
        // 第十八批 AH 段 239-R1（ENUM:46「上述有效期类型决定的是「这张票在多长时间内可以开始使用」」）：票面到期改由票务侧消费回调
        // （TicketVerificationService.requireAmusementConsumption）按「本场已检票的票本场内不受截断」复核，这里只拦尚未生效。
        if (entitlement.validFrom() != null && now.isBefore(entitlement.validFrom())) {
            throw ApiException.conflict("ENTITLEMENT_EXPIRED", "该权益不在有效期内");
        }
        if (!isAmusementEntitlementType(entitlement.entitlementType())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_AMUSEMENT", "该权益不是游乐类权益");
        }
        // 绑定本项目：该权益 type 在 amusement_project_entitlements 内。
        // 回退口径（V082 向后兼容）：项目无任何绑定配置时，仅按「游乐类」放行（上方已校验）。
        List<String> boundTypes = repository.listProjectEntitlementTypes(project.id());
        if (!boundTypes.isEmpty() && !boundTypes.contains(entitlement.entitlementType())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_FOR_PROJECT", "该权益未绑定到此游乐项目");
        }

        int peopleCount = Math.max(entitlement.numPeople(), 1);
        // §6.4 步骤 4/6：先消费票务权益（原子扣 used_times），再按项目硬上限增加在场人数。
        // 顺序与回滚：权益扣减失败不扣容量；容量任一维度扣减失败则回补权益（补偿），保证两者一致。
        AmusementEntitlementGateway.Consumption consumption =
                entitlementGateway.consumeEntitlement(entitlement.id(), businessWindow, now);
        if (consumption == AmusementEntitlementGateway.Consumption.UNAVAILABLE) {
            throw ApiException.conflict("ENTITLEMENT_USED", "该权益剩余次数已用完且当前营业日没有有效激活");
        }
        // 清单#6 超容处置策略（§6.4/§7.18 + R6-D10）：硬上限阻断+软上限告警放行，均落
        // amusement.incident.over_capacity 事件；处置模式后台可切换（block_hard 默认/warn_only 仅告警留痕不阻断）。
        AmusementOverCapacityPolicy policy = repository.findOverCapacityPolicy(project.id());
        try {
            // 单项目实时在玩人数原子占用，场次、点位和团体预约不形成重复硬上限。
            CapacityClaimResult claim = repository.claimCapacityAcrossDimensions(new CapacityClaimContext(
                    project.id(),
                    session.id(),
                    pointId,
                    deviceId,
                    project.capacity(),
                    peopleCount
            ));
            if (!claim.claimed()) {
                // F-OPS-03（裁决 #6）：硬上限=安全容量=人身安全红线，任何处置模式都不可旁路。
                // warn_only 仅作用于软上限（运营容量），对硬上限无效；硬超容一律阻断并由
                // recordOverCapacity(severity=high) 触发自动停运。catch 块负责回补已消费权益。
                // 留痕与失败审计同语义（与既有 recordFailure 一致，同事务内尽力写入）。
                recordHardOverCapacityDurably(project, session, deviceId, claim.exhaustedDimension(), policy);
                throw ApiException.conflict("SESSION_CAPACITY_EXHAUSTED",
                        "项目在场人数已达硬安全容量");
            }
        } catch (RuntimeException exception) {
            // 容量扣减失败：回补已消费的权益，避免权益被白扣。
            if (consumption == AmusementEntitlementGateway.Consumption.CONSUMED) {
                try {
                    entitlementGateway.reverseEntitlement(entitlement.id(), businessWindow, now);
                } catch (RuntimeException reversalFailure) {
                    exception.addSuppressed(reversalFailure);
                }
            }
            throw exception;
        }
        // 软上限（运营容量）告警：放行后项目在场人数达到运营容量时留痕，不阻断。
        if (policy.hasOperatingCapacity()
                && repository.findProjectInPlayCount(project.id()).orElse(0) >= policy.operatingCapacity()) {
            recordOverCapacity(project, session, deviceId, "soft", "operating_capacity", policy);
        }
        // 入场保留权益、点位和设备事实；出场只释放该入场对应的项目人数。
        // 第十八批 AH 段第 219 条补充（V619 去掉 V234 唯一约束）：同一权益同一场次可有多条未释放入场（BSM:780 多次票每用一次扣一次）。
        AmusementVerificationRecord saved = repository.saveVerification(AmusementVerificationRecord.entry(
                "verification_" + slug(idempotencyKey),
                project.id(),
                session.id(),
                credential,
                deviceId,
                staffId,
                "success",
                consumption == AmusementEntitlementGateway.Consumption.DAY_REPLAY ? "day_replay" : null,
                List.of(),
                now,
                peopleCount,
                entitlement.id(),
                pointId));
        if (businessWindow != null) sessionLifecycle.recordEntryWindow(saved.id(), businessWindow);
        entitlementGateway.recordAdmission(saved, businessWindow, consumption);
        return saved;
    }

    /**
     * 阶段2（2026-07-05 用户拍板）出场核销：自由进出型（并发容量）客人/闸机出场扫码 → 并发容量 -N 释放。
     *
     * <p>据凭证定位未释放的成功入场，按该入场人数释放项目在场人数，保存出场记录及释放来源。
     * 同一入场由出场、场次结束或营业日结束仅释放一次；后续出场不能误减新入场人数。
     * 无进行中入场且无既有出场记录时返回 ENTITLEMENT_NO_ACTIVE_ENTRY。
     */
    @com.complex.platform.gate.GuardedAction(capability = "amusement.verification.execute")
    @Transactional
    public AmusementVerificationRecord exitVerification(AmusementExitRequest request) {
        AmusementExitRequest actual = requireRequest(request);
        String idempotencyKey = requireText(actual.idempotencyKey(), "idempotency_key");
        String credential = requireText(actual.credentialPayload(), "credential_payload");
        String staffId = trimToDefault(actual.staffId(), "staff");
        Instant now = Instant.now();
        // 凭证 → 权益实例 → 未释放入场记录。定位不到入场即报错（出场无通用回退旧路）。
        AmusementEntitlementView entitlement = entitlementGateway.findEntitlementByCredential(credential)
                .orElseThrow(() -> ApiException.notFound("TICKET_NOT_FOUND", "票券权益不存在或已被删除"));
        final String entId = entitlement.id();
        java.util.Optional<AmusementVerificationRecord> active = repository.findActiveEntryByEntitlement(entId);
        if (active.isEmpty()) {
            // 无未释放入场：要么已出场（幂等：返回既有 exit，不重复减容量），要么从未入场（报错）。
            AmusementVerificationRecord previous = repository.findLatestExitByEntitlement(entId)
                    .orElseThrow(() -> ApiException.conflict(
                            "ENTITLEMENT_NO_ACTIVE_ENTRY", "该权益没有进行中的游乐入场可释放"));
            if (sessionLifecycle != null) sessionLifecycle.requireVerificationScope(previous.sessionId(), "amusement.verification.execute");
            return previous;
        }
        AmusementVerificationRecord entry = active.get();
        if (sessionLifecycle != null) sessionLifecycle.requireVerificationScope(entry.sessionId(), "amusement.verification.execute");
        repository.lockProjectForSafety(entry.projectId());
        if (sessionLifecycle != null) {
            sessionLifecycle.requireVerificationScope(entry.sessionId(), "amusement.verification.execute");
            sessionLifecycle.requireEntryFacts(entry.id());
        }
        now = Instant.now();
        AmusementVerificationRecord exitRecord = new AmusementVerificationRecord(
                "verification_exit_" + slug(idempotencyKey),
                entry.projectId(),
                entry.sessionId(),
                credential,
                trimToNull(actual.deviceId()),
                staffId,
                "success",
                null,
                evidence(actual.evidenceAttachmentIds()),
                now,
                entry.verifiedPeople(),
                "exit",
                now,
                entry.id(),
                entitlement.id(),
                trimToNull(actual.pointId()));
        // 原子释放：条件标记入场 released_at（并发/幂等赢家）→ 赢家 -N 并发快照 + 写 exit 记录。
        boolean released = repository.releaseEntryConcurrentCapacity(entry, exitRecord, now);
        if (!released) {
            // 入场已被并发/前次出场释放：幂等返回既有 exit 记录，不重复减容量。
            return repository.findExitByEntry(entry.id()).orElseThrow(() -> ApiException.conflict(
                    "ENTITLEMENT_NO_ACTIVE_ENTRY", "该入场已由清场或营业日结束释放"));
        }
        // CG-02（BSM「因达到硬安全容量而自动停运的项目，在玩人数回落到硬安全容量以下后由系统自动恢复开放」）：
        // 本次真实释放后，若项目仍处硬超容自动停运且项目维在玩人数已 < 硬安全容量，由系统 CAS paused→open 并留痕。
        autoReopenIfHardCapacityRecovered(entry, exitRecord, idempotencyKey, now);
        return exitRecord;
    }

    /**
     * CG-02 出场回落自动恢复开放。条件与翻转由仓储单条原子 UPDATE 完成（paused ∧ auto_paused_reason=hard_capacity ∧
     * 项目维 occupied_count &lt; capacity），并发多路出场只有一路翻转成功、只留一条痕。事故停运（safety_incident）、
     * 人工 maintenance/closed/paused 不满足条件，不动（恢复仍走巡检/检修复核）。
     * 留痕 amusement.project.auto_reopened：业务主体=项目，triggered_by=system，关联触发的出场/入场记录与当时在玩人数。
     */
    private void autoReopenIfHardCapacityRecovered(
            AmusementVerificationRecord entry, AmusementVerificationRecord exitRecord, String idempotencyKey, Instant now) {
        if (!reopenAfterSafetyCheck(entry.projectId(), now)) {
            return;
        }
        int inPlay = repository.findProjectInPlayCount(entry.projectId()).orElse(-1);
        int safetyCapacity = repository.findProjectById(entry.projectId())
                .map(AmusementProjectRecord::capacity).orElse(-1);
        auditEventService.recordSuccess(
                "amusement.project.auto_reopened",
                "amusement.verification.execute",
                "amusement_project",
                entry.projectId(),
                null,
                idempotencyKey,
                Map.of("reason", "hard_capacity_recovered",
                        "triggered_by", "system",
                        "trigger_exit_verification_id", exitRecord.id(),
                        "trigger_entry_verification_id", entry.id(),
                        "in_play_count", inPlay,
                        "safety_capacity", safetyCapacity));
    }

    /**
     * CG-02：硬超容的停运 + incident + 事件在 REQUIRES_NEW 独立事务里提交，不随即将回滚的核销主事务消失
     * （BSM「一旦达到…必须阻断核销入场，并自动让该项目停止运营」；停运持久化是「人数回落后自动恢复开放」的前提）。
     * 无事务管理器（单测、无 Spring 容器）则就地执行，语义同旧。
     */
    private void recordHardOverCapacityDurably(
            AmusementProjectRecord project,
            AmusementSessionRecord session,
            String deviceId,
            String dimension,
            AmusementOverCapacityPolicy policy
    ) {
        if (transactionManager == null) {
            recordOverCapacity(project, session, deviceId, "hard", dimension, policy);
            return;
        }
        org.springframework.transaction.support.TransactionTemplate durable =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        durable.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Runnable persist = () -> durable.executeWithoutResult(status ->
                recordOverCapacity(project, session, deviceId, "hard", dimension, policy));
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCompletion(int status) { persist.run(); }
                    });
        } else persist.run();
    }

    /**
     * 清单#6：超容（硬/软）落 over_capacity incident（复用 amusement_incidents 写入路径）+
     * 发 amusement.incident.over_capacity 事件（复用既有 AuditEventService）。
     *
     * @param overType 超容类型：hard=安全容量（硬上限）/ soft=运营容量（软上限）
     * @param dimension 触发依据（project_safety / operating_capacity）
     */
    private void recordOverCapacity(
            AmusementProjectRecord project,
            AmusementSessionRecord session,
            String deviceId,
            String overType,
            String dimension,
            AmusementOverCapacityPolicy policy
    ) {
        Instant now = Instant.now();
        // F-OPS-03（裁决 #6）：硬超容=安全容量红线，严重度恒 high（触发自动停运），
        // 不因 warn_only 处置模式降级；软超容/运营容量为 medium。
        String severity = "hard".equals(overType) ? "high" : "medium";
        String incidentId = "ami_oc_" + slug(project.id() + ":" + session.id() + ":"
                + overType + ":" + now.toEpochMilli());
        AmusementIncidentRecord incident = safetyRepository.saveIncident(new AmusementIncidentRecord(
                incidentId,
                project.id(),
                project.id(),
                session.id(),
                deviceId,
                null,
                severity,
                "over_capacity",
                "created",
                "游乐超容（" + overType + "）",
                "amusement over capacity: type=" + overType + ", dimension=" + dimension
                        + ", disposal_mode=" + policy.disposalMode(),
                "system",
                "amusement_verification",
                now,
                null,
                null,
                List.of(),
                now,
                now,
                project.merchantId()
        ));
        // 双写统一责任链（fail-safe，不阻塞核销主链；孤岛 amusement_incidents 仍权威）。
        reportToUnifiedRiskAlerts(
                "over_capacity",
                incident.id(),
                incident.severity(),
                project.id(),
                "amusement over capacity: type=" + overType + ", dimension=" + dimension,
                "incident=" + incident.id() + "; over_type=" + overType + "; dimension=" + dimension);
        // 硬超容（severity=high）自动停运停售（open→paused，CAS 幂等），并记原因 hard_capacity（V443）：
        // BSM「因达到硬安全容量而自动停运的项目，在玩人数回落到硬安全容量以下后由系统自动恢复开放」——
        // 恢复在出场释放路径 exitVerification 由系统自动完成（不设人工恢复按钮）；事故停运（safety_incident）不在此列。
        if ("high".equals(incident.severity())) {
            safetyRepository.autoPauseProject(project.id(), "hard_capacity");
        }
        auditEventService.recordSuccess(
                "amusement.incident.over_capacity",
                "amusement.verification.execute",
                "amusement_incident",
                incident.id(),
                null,
                null,
                Map.of(
                        "project_id", project.id(),
                        "session_id", session.id(),
                        "over_type", overType,
                        "dimension", dimension,
                        "disposal_mode", policy.disposalMode()
                )
        );
    }

    /**
     * 把游乐超容/安全事件双写到统一 risk_alerts（source_domain='amusement'）。
     *
     * <p>fail-safe：未注入服务（单测/无 SecurityContext）或上报异常都不阻塞核销主链——
     * 孤岛 amusement_incidents 仍是落地权威，统一表为汇聚镜像。
     * idempotencyKey 用 incidentId+":unified" 稳定唯一，重复核销不重复上报。
     */
    private void reportToUnifiedRiskAlerts(
            String alertType,
            String incidentId,
            String severity,
            String projectId,
            String summary,
            String reason
    ) {
        if (riskAlertService == null) {
            return;
        }
        Runnable report = () -> {
            try {
                Runnable write = () -> riskAlertService.report(new RiskAlertReportRequest(
                        incidentId + ":unified", "amusement", incidentId, alertType, mapRiskLevel(severity),
                        summary, projectId, reason, List.of()));
                if (transactionManager == null) write.run();
                else {
                    var independent = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
                    independent.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    independent.executeWithoutResult(status -> write.run());
                }
            } catch (RuntimeException exception) {
                auditEventService.recordFailure(
                        "amusement.risk.unified_alert_failed", "amusement.verification.execute", "risk_alert",
                        incidentId, null, incidentId + ":unified",
                        exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName());
            }
        };
        // 统一风险镜像失败不得回滚已确认的事故和停运；仅在原事实提交后单独尝试。
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() { report.run(); }
                    });
        } else report.run();
    }

    /**
     * 把游乐域 severity 映射到统一表合法 risk_level（仅 normal/suspicious/high/critical）。
     * medium → suspicious（否则统一表 VALIDATION_FAILED 被吞=丢数据）；high/critical/low 直透 / 兜底。
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

    private boolean isAmusementEntitlementType(String entitlementType) {
        return entitlementType != null
                && entitlementType.trim().toLowerCase(Locale.ROOT).startsWith(AMUSEMENT_ENTITLEMENT_TYPE_PREFIX);
    }

    private void checkProjectOwnership(String projectId) {
        authorizationService.currentMerchantScope().ifPresent(merchantId -> {
            AmusementProjectRecord project = repository.findProjectById(projectId)
                    .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
            if (!merchantId.equals(project.merchantId())) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "该游乐项目不属于当前商户");
            }
        });
    }

    /**
     * T3.1 巡检到期调度（激活 V143 已建表 amusement_inspection_tasks）：遍历每个项目的在役(active)设备，按
     * 「设备级安全检查周期（缺省回退项目级，默认 24h）」从最近 PASS 巡检时间推算到期点（从未巡检则立即到期），
     * 幂等 upsert 一张 pending 待办（同到期点不重复）；再把已到期未完成的待办批量翻 overdue。
     * 由 {@link AmusementInspectionTaskJob} 薄 @Scheduled 包装调用（默认关），亦供单测直调对拍。返回本次遍历产生的 upsert 次数。
     */
    // ============================================================
    // CG-03 在玩人数的系统自动清零（BSM「出场自动扣减」：「按场清场的场次型项目，散场时由系统按场次结束自动清空；
    // 非场次型项目，营业日结束时由系统自动清零在玩人数」；BSM「在玩人数不允许人工调整」）。
    // 两个写口只由调度作业（系统主体）调用，不暴露端点、不接受人工参数。
    // ============================================================

    // 按实际综合体已批准配置锁定场次及核销营业日，旧全域配置不再用于游乐。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    AmusementBusinessDayService amusementBusinessDayService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    AmusementSessionLifecycleService sessionLifecycle;

    // 第十八批 N 段第 123 条 123-R2：敏感操作定位闸（字段注入免改构造点；单测未注入即不查）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.gate.SensitiveLocationGuard locationGuard;

    /**
     * 营业日结束自动清零（非场次型）：对每个项目，把上一营业日结束时刻之前最后变动的并发快照（项目维 / 点位设备维）清零；
     * 新营业日已产生的计数不动。每项目每营业日只执行一次（留痕唯一键），重复调度不重复记；留系统审计事件。
     * 返回本次新清零的项目数。
     */
    @org.springframework.transaction.annotation.Transactional
    public int resetConcurrentCountsAtBusinessDayEnd(Instant now) {
        return sessionLifecycle.resetExpiredBusinessDays(now);
    }

    /** 按结束原件仅释放本场未出场记录，不改其他场次或新营业日记录。 */
    @org.springframework.transaction.annotation.Transactional
    public int closeEndedSessionsAndClearCounts(Instant now) {
        return sessionLifecycle.closeEndedSessions(now);
    }

    public int generateDueInspectionTasks(Instant now) {
        int upserted = 0;
        for (AmusementProjectRecord project : repository.listProjects()) {
            int projectInterval = repository.findProjectSafetyCheckIntervalHours(project.id());
            for (AmusementDeviceRecord device : repository.listDevicesByProject(project.id())) {
                if (!"active".equals(device.status())) {
                    continue; // 退役/停用/检修设备不排巡检
                }
                int intervalHours = device.safetyCheckIntervalHours() != null
                        ? device.safetyCheckIntervalHours() : projectInterval;
                Instant dueAt = device.lastPassInspectionAt() != null
                        ? device.lastPassInspectionAt().plus(java.time.Duration.ofHours(intervalHours))
                        : now; // 从未巡检 → 立即到期，必须尽快巡检
                repository.upsertInspectionTask(new AmusementInspectionTaskRecord(
                        "itask_" + UUID.randomUUID().toString().replace("-", ""),
                        project.id(), device.id(), dueAt, "pending", now,
                        null, null, device.merchantId(), now, now));
                upserted++;
            }
        }
        repository.markOverdueInspectionTasks(now);
        return upserted;
    }

    private boolean reopenAfterSafetyCheck(String projectId, Instant now) {
        repository.lockProjectForSafety(projectId);
        if (!repository.isHardCapacityPaused(projectId)) return false;
        AmusementOpenEligibilityResponse safety = evaluateOpenEligibility(projectId, null, null, true);
        if (!safety.allowed()) {
            auditEventService.recordSuccess("amusement.project.auto_reopen_blocked", null,
                    "amusement_project", projectId, null, null, Map.of("reasons", safety.blockedReasons()));
            return false;
        }
        return repository.reopenProjectIfHardCapacityRecovered(projectId, now);
    }

    @Transactional
    public int reassessHardCapacityRecovery(Instant now) {
        int reopened = 0;
        for (AmusementProjectRecord project : repository.listProjects()) {
            if (reopenAfterSafetyCheck(project.id(), now)) {
                auditEventService.recordSuccess("amusement.project.auto_reopened", null,
                        "amusement_project", project.id(), null, null,
                        Map.of("reason", "hard_capacity_recovered", "triggered_by", "system"));
                reopened++;
            }
        }
        return reopened;
    }

    private AmusementOpenEligibilityResponse evaluateOpenEligibility(String projectId, String sessionId, String deviceId) {
        return evaluateOpenEligibility(projectId, sessionId, deviceId, false);
    }

    private AmusementOpenEligibilityResponse evaluateOpenEligibility(
            String projectId, String sessionId, String deviceId, boolean hardCapacityRecovery) {
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
        List<String> reasons = new ArrayList<>();
        if (!"open".equals(project.openStatus()) && !hardCapacityRecovery) {
            reasons.add("PROJECT_NOT_OPEN");
        }
        repository.latestSafetyInspectionForProject(projectId)
                .ifPresentOrElse(inspection -> {
                    if ("pass".equals(inspection.overallResult())) {
                        if (!"submitted".equals(inspection.inspectionStatus())) {
                            reasons.add("SAFETY_INSPECTION_NOT_SUBMITTED");
                        }
                    } else if (!"approved".equals(inspection.reopenStatus())) {
                        reasons.add("SAFETY_REOPEN_PENDING");
                    }
                }, () -> reasons.add("MISSING_SAFETY_INSPECTION"));
        // F-OPS-04（裁决#6）：设备级巡检有效期。核销针对具体已注册设备时，校验该设备最近 PASS 巡检在周期内；
        // 周期取设备级配置（缺省回退项目级，默认 24h）。设备未注册主数据则跳过（向后兼容存量自由 device_id）。
        if (deviceId != null) {
            repository.findDeviceByCode(projectId, deviceId).ifPresent(device -> {
                int intervalHours = device.safetyCheckIntervalHours() != null
                        ? device.safetyCheckIntervalHours()
                        : repository.findProjectSafetyCheckIntervalHours(projectId);
                Instant lastPass = device.lastPassInspectionAt();
                if (lastPass == null
                        || lastPass.plus(java.time.Duration.ofHours(intervalHours)).isBefore(Instant.now())) {
                    reasons.add("SAFETY_INSPECTION_EXPIRED");
                }
            });
        }
        // P0-3 修复（F-OPS-04 项目级兜底）：核销请求的 deviceId 由客户端可选传入、且存量可为未注册自由字符串，
        // 仅靠上方单设备校验可被「不传 / 乱传未注册 deviceId」整体绕过。此处不依赖 deviceId——只要项目下存在
        // 「在役(active)」设备且其最近 PASS 巡检超期或从未巡检，即阻断该项目核销入场（业务红线：设备巡检超期 →
        // 停止该设备/项目核销入场，直到完成合格巡检）。退役/停用/检修(inactive/retired/maintenance)设备不在役、不参与兜底。
        if (!reasons.contains("SAFETY_INSPECTION_EXPIRED")) {
            int projectIntervalHours = repository.findProjectSafetyCheckIntervalHours(projectId);
            Instant safetyNow = Instant.now();
            boolean anyActiveDeviceExpired = repository.listDevicesByProject(projectId).stream()
                    .filter(device -> "active".equals(device.status()))
                    .anyMatch(device -> {
                        int intervalHours = device.safetyCheckIntervalHours() != null
                                ? device.safetyCheckIntervalHours()
                                : projectIntervalHours;
                        Instant lastPass = device.lastPassInspectionAt();
                        return lastPass == null
                                || lastPass.plus(java.time.Duration.ofHours(intervalHours)).isBefore(safetyNow);
                    });
            if (anyActiveDeviceExpired) {
                reasons.add("SAFETY_INSPECTION_EXPIRED");
            }
        }
        // T3.2（A4 根因修，默认关）：强制建档——项目无任何在役注册设备（设备档案为空）时拒核销，
        // 从根上消除「零设备档案项目」的 fail-open（用户决策：建项目即建档、零档拒核销）。存量过渡靠开关默认关。
        if (enforceDeviceRegistry
                && repository.listDevicesByProject(projectId).stream()
                        .noneMatch(device -> "active".equals(device.status()))) {
            reasons.add("AMUSEMENT_DEVICE_REGISTRY_MISSING");
        }
        if (!repository.listActiveBlockingProjectRules(projectId).isEmpty()) {
            reasons.add("PROJECT_RULE_BLOCKED");
        }
        if (sessionId != null) {
            AmusementSessionRecord session = repository.findSessionById(sessionId)
                    .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "场次不存在或已被删除"));
            if (!"open".equals(session.status())) {
                reasons.add("SESSION_NOT_OPEN");
            }
        }
        return new AmusementOpenEligibilityResponse(projectId, sessionId, reasons.isEmpty(), List.copyOf(reasons));
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        try {
            List<T> items = action.get();
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T saved = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(saved),
                                null,
                                idempotencyKey,
                                Map.of()
                        );
                        return saved;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "unknown",
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private String businessId(Object response) {
        if (response instanceof AmusementProjectRecord record) {
            return record.id();
        }
        if (response instanceof AmusementProjectSpaceAssignmentRecord record) {
            return record.projectId();
        }
        if (response instanceof AmusementSessionRecord record) {
            return record.id();
        }
        if (response instanceof AmusementSafetyTemplateRecord record) {
            return record.id();
        }
        if (response instanceof AmusementSafetyInspectionRecord record) {
            return record.id();
        }
        if (response instanceof AmusementProjectRuleRecord record) {
            return record.id();
        }
        if (response instanceof AmusementProjectEntitlementRecord record) {
            return record.id();
        }
        if (response instanceof AmusementCapacityConfigRecord record) {
            return record.id();
        }
        if (response instanceof AmusementDeviceRecord record) {
            return record.id();
        }
        if (response instanceof AmusementOverCapacityPolicy record) {
            return record.projectId();
        }
        if (response instanceof AmusementVerificationRecord record) {
            return record.id();
        }
        if (response instanceof EntitlementSessionRecord record) {
            return record.id();
        }
        if (response instanceof AmusementReservationRecord record) {
            return record.id();
        }
        if (response instanceof AmusementQueueEntryRecord record) {
            return record.id();
        }
        if (response instanceof AmusementGroupBookingRecord record) {
            return record.id();
        }
        return "unknown";
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private int nonNegativeOrDefault(Integer value, int defaultValue, String fieldName) {
        int actual = value == null ? defaultValue : value;
        if (actual < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 不能为负数");
        }
        return actual;
    }

    private int positiveInteger(Integer value, String fieldName) {
        int actual = value == null ? 0 : value;
        if (actual <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须大于 0");
        }
        return actual;
    }

    private String enumOrDefault(String value, String defaultValue, Set<String> allowed, String fieldName) {
        String actual = trimToDefault(value, defaultValue);
        if (actual == null || !allowed.contains(actual)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return actual;
    }

    private List<String> evidence(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private String textFromMap(Map<String, Object> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
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

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
