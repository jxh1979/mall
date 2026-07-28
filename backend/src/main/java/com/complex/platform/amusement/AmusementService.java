package com.complex.platform.amusement;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class AmusementService implements AmusementVerificationPort {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> PROJECT_STATUSES = Set.of("open", "paused", "closed", "maintenance");
    private static final Set<String> SESSION_STATUSES = Set.of("scheduled", "open", "closed", "cancelled");
    private static final Set<String> OVERRIDE_TYPES = Set.of("allow", "deny", "pause", "resume", "release");
    private static final Set<String> RULE_TYPES = Set.of("age", "height", "weight", "capacity", "safety_notice", "manual");
    private static final Set<String> DEVICE_STATUSES = Set.of("active", "inactive", "maintenance", "retired");

    // 游乐权益类前缀（§6.4/§12.1：游乐只消费票务生成的游乐权益）。
    // entitlement_type 以 "amusement" 开头视为游乐类（amusement_single / amusement_pass / 游乐通票等）。
    private static final String AMUSEMENT_ENTITLEMENT_TYPE_PREFIX = "amusement";

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

    // T3.2（A4 根因修，功能标志 amusement.device-registry.enforce.enabled，默认关）：是否强制游乐项目建设备档案。
    // 开启后「零在役设备档案」项目拒核销，从根上消除 fail-open；默认关给存量项目建档过渡期，ops 建档后开启即生效。
    // 用字段注入（非构造）以免改动既有多处构造点；包私有便于单测直接置位。
    @org.springframework.beans.factory.annotation.Value("${amusement.device-registry.enforce.enabled:false}")
    boolean enforceDeviceRegistry;

    // 向后兼容构造器：未注入统一责任链服务（既有单测/无 SecurityContext 环境）。
    public AmusementService(
            AmusementRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            AmusementEntitlementGateway entitlementGateway,
            AmusementSafetyRepository safetyRepository
    ) {
        this(repository, idempotencyService, auditEventService, authorizationService,
                entitlementGateway, safetyRepository, null);
    }

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
        this.bookingService = new AmusementBookingService(repository, idempotencyService, auditEventService, authorizationService);
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

    public PageResponse<AmusementManualOverrideRecord> listManualOverrides() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.manual_override.viewed",
                "amusement.manual_override.view_failed",
                "amusement.verification.override",
                "amusement_manual_override",
                scope.isPresent() ? () -> repository.listManualOverridesByMerchant(scope.get()) : repository::listManualOverrides
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.verification.override", admissionAction = "amusement.manual_override", objectType = "amusement_manual_override")
    @Transactional
    public AmusementManualOverrideRecord createManualOverride(AmusementManualOverrideRequest request) {
        return executeWrite(
                "createAmusementManualOverride",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementManualOverrideRecord.class,
                "amusement.verification_overridden",
                "amusement.verification_override_failed",
                "amusement.verification.override",
                "amusement_manual_override",
                () -> createManualOverrideInternal(request)
        );
    }

    public PageResponse<AmusementProjectRecord> listProjects() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.session.viewed",
                "amusement.session.view_failed",
                "amusement.session.view",
                "amusement_project",
                scope.isPresent() ? () -> repository.listProjectsByMerchant(scope.get()) : repository::listProjects
        );
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
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        // 空间必须存在；若属于某商户，必须与项目同商户（不可指派他商户空间，currentMerchantScope 隔离）。
        String spaceMerchant = repository.findSpaceMerchant(spaceId)
                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "space not found"));
        if (!spaceMerchant.isBlank() && project.merchantId() != null && !spaceMerchant.equals(project.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "space belongs to another merchant");
        }
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !spaceMerchant.isBlank() && !scope.get().equals(spaceMerchant)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "space belongs to another merchant");
        }
        if (repository.assignProjectSpace(projectId, spaceId) == 0) {
            throw ApiException.notFound("PROJECT_NOT_FOUND", "project not found");
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
     * 已被审批通过删除（墓碑）的绑定恢复须持审批权 amusement.project_entitlement.approve（只授 role_platform_admin）。
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
                    checkProjectOwnership(id);
                    AmusementProjectEntitlementRecord existing = requireProjectEntitlement(id, type);
                    if (existing.active()) {
                        throw ApiException.conflict("PROJECT_ENTITLEMENT_NOT_ARCHIVED", "该权益绑定处于生效中，无需恢复");
                    }
                    if ("approved".equals(existing.deleteStatus())) {
                        // 墓碑复活=撤销一次最高权限批准的删除，按同级最高权限把守。
                        authorizationService.requirePermission("amusement.project_entitlement.approve");
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
                    checkProjectOwnership(id);
                    AmusementProjectEntitlementRecord existing = requireProjectEntitlement(id, type);
                    if (existing.archivedAt() == null) {
                        throw ApiException.badRequest("PROJECT_ENTITLEMENT_NOT_ARCHIVED", "请先存档再申请删除");
                    }
                    if (!"none".equals(existing.deleteStatus())) {
                        throw ApiException.conflict("PROJECT_ENTITLEMENT_DELETE_ALREADY_PENDING", "该权益绑定已有待审批的删除申请");
                    }
                    repository.requestDeleteProjectEntitlement(id, type, deleteReason, currentActorId());
                    return requireProjectEntitlement(id, type);
                }
        );
    }

    /**
     * 审批删除申请：approved → delete_status='approved'（墓碑，行留库、全读取路径过滤、可恢复，不物理删）；
     * rejected → 回 none。权限 amusement.project_entitlement.approve（只授 role_platform_admin），且申请人禁自审。
     */
    @com.complex.platform.gate.GuardedAction(capability = "amusement.project_entitlement.approve")
    @Transactional
    public AmusementProjectEntitlementRecord decideDeleteProjectEntitlement(
            String projectId, String entitlementType, String decision, String idempotencyKey) {
        String id = requireText(projectId, "project_id");
        String type = requireText(entitlementType, "entitlement_type");
        String verdict = decision == null ? null : decision.trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(verdict) && !"rejected".equals(verdict)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
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
                    checkProjectOwnership(id);
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

    private AmusementProjectEntitlementRecord requireProjectEntitlement(String projectId, String entitlementType) {
        return repository.findProjectEntitlement(projectId, entitlementType)
                .orElseThrow(() -> ApiException.notFound("PROJECT_ENTITLEMENT_NOT_FOUND", "project entitlement binding not found"));
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
                    .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
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
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
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
                    .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
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
                            "ENTITLEMENT_SESSION_NOT_FOUND", "entitlement session not found"));
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
                        "ENTITLEMENT_SESSION_NOT_FOUND", "entitlement session not found"));
        checkEntitlementSessionOwnership(session);
        String claimedBy = trimToDefault(actual.claimedBy(), "kiosk");
        Instant now = Instant.now();
        // 防错领核心：条件 UPDATE 受影响 1 行才成功；过期/已领/重复领/已取消均 0 行被拒。
        if (!repository.claimEntitlementSession(sessionId, claimedBy, now)) {
            // 区分过期与状态非法，给出明确错误码（便于一体机提示）。
            if (session.expiresAt() != null && !now.isBefore(session.expiresAt())
                    && ENTITLEMENT_SESSION_CLAIMABLE.contains(session.status())) {
                throw ApiException.conflict("ENTITLEMENT_SESSION_EXPIRED", "entitlement session has expired");
            }
            throw ApiException.conflict("ENTITLEMENT_SESSION_NOT_CLAIMABLE",
                    "entitlement session is not claimable (status=" + session.status() + ")");
        }
        return repository.findEntitlementSessionById(sessionId).orElse(session);
    }

    private void checkEntitlementSessionOwnership(EntitlementSessionRecord session) {
        authorizationService.currentMerchantScope().ifPresent(merchantId -> {
            if (!merchantId.equals(session.merchantId())) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "entitlement session does not belong to your merchant");
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
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.session.viewed",
                "amusement.session.view_failed",
                "amusement.session.view",
                "amusement_session",
                scope.isPresent() ? () -> repository.listSessionsByMerchant(scope.get()) : repository::listSessions
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.session.manage")
    @Transactional
    public AmusementSessionRecord createSession(AmusementSessionRequest request) {
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
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.verification.viewed",
                "amusement.verification.view_failed",
                "amusement.verification.view",
                "amusement_verification",
                scope.isPresent() ? () -> repository.listVerificationsByMerchant(scope.get()) : repository::listVerifications
        );
    }

    /** 阶段3 只读：按 项目/场次/方向/日期 筛选核销记录（含关联通票号）。走同域 merchant scope 隔离范式。 */
    public PageResponse<AmusementVerificationListItem> listVerificationItems(AmusementVerificationQuery query) {
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        return readPage(
                "amusement.verification.viewed",
                "amusement.verification.view_failed",
                "amusement.verification.view",
                "amusement_verification",
                () -> repository.listVerificationItems(query, merchantId)
        );
    }

    /** 阶段3 只读：并发在场容量快照。走同域 merchant scope 隔离范式。 */
    public PageResponse<AmusementCapacitySnapshotRecord> listCapacitySnapshots() {
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        return readPage(
                "amusement.verification.viewed",
                "amusement.verification.view_failed",
                "amusement.verification.view",
                "amusement_capacity_snapshot",
                () -> repository.listCapacitySnapshots(merchantId)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "amusement.verification.execute")
    @Transactional
    public AmusementVerificationRecord createVerification(VerificationRequest request) {
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
                    .orElseThrow(() -> ApiException.notFound("SAFETY_TEMPLATE_NOT_FOUND", "safety template not found"));
        }
        Instant now = Instant.now();
        return repository.saveProject(new AmusementProjectRecord(
                "project_" + slug(actual.idempotencyKey()),
                requireText(actual.projectName(), "project_name"),
                nonNegativeOrDefault(actual.capacity(), 0, "capacity"),
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
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        Instant start = actual.startTime() == null ? Instant.now() : actual.startTime();
        Instant end = actual.endTime() == null ? start.plusSeconds(3600) : actual.endTime();
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "end_time must be after start_time");
        }
        Instant now = Instant.now();
        return repository.saveSession(new AmusementSessionRecord(
                "session_" + slug(actual.idempotencyKey()),
                projectId,
                start,
                end,
                nonNegativeOrDefault(actual.capacity(), 0, "capacity"),
                0,
                0,
                enumOrDefault(actual.status(), "scheduled", SESSION_STATUSES, "status"),
                evidence(actual.evidenceAttachmentIds()),
                now,
                now
        ));
    }

    private AmusementProjectRuleRecord createProjectRuleInternal(String projectId, AmusementProjectRuleRequest request) {
        AmusementProjectRuleRequest actual = requireRequest(request);
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
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
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        String entitlementType = requireText(actual.entitlementType(), "entitlement_type");
        // 仅允许绑定游乐类权益类型（与核销在线校验 §6.4/§12.1 口径一致）。
        if (!isAmusementEntitlementType(entitlementType)) {
            throw ApiException.badRequest("ENTITLEMENT_NOT_AMUSEMENT", "entitlement_type must be an amusement entitlement type");
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
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        String pointId = trimToNull(actual.locationPointId());
        String deviceId = trimToNull(actual.deviceId());
        if (pointId == null && deviceId == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "location_point_id or device_id is required");
        }
        int maxConcurrent = nonNegativeOrDefault(actual.maxConcurrent(), -1, "max_concurrent");
        if (actual.maxConcurrent() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "max_concurrent is required");
        }
        Instant now = Instant.now();
        return repository.saveCapacityConfig(new AmusementCapacityConfigRecord(
                "amcap_" + slug(actual.idempotencyKey()),
                projectId,
                pointId,
                deviceId,
                maxConcurrent,
                trimToDefault(actual.effectiveSessionPattern(), ""),
                actual.enabled() == null || actual.enabled(),
                now,
                now
        ));
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
                .orElseThrow(() -> ApiException.notFound("CAPACITY_CONFIG_NOT_FOUND", "capacity config not found"));
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
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        if (!"open".equals(project.openStatus())) {
            throw ApiException.conflict("PROJECT_NOT_OPEN", "project is not open");
        }
        AmusementSessionRecord session = repository.findSessionById(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "session not found"));
        if (!project.id().equals(session.projectId())) {
            throw ApiException.conflict("SESSION_PROJECT_MISMATCH", "session does not belong to project");
        }
        if (!"open".equals(session.status())) {
            throw ApiException.conflict("SESSION_NOT_OPEN", "session is not open");
        }
        // F-OPS-04：核销针对具体设备时传 deviceId，准入闸校验该设备巡检有效期（过期则 SAFETY_INSPECTION_EXPIRED）。
        String deviceId = trimToNull(actual.deviceId());
        String pointId = trimToNull(textFromMap(actual.businessContext(),
                "location_point_id", "locationPointId", "point_id", "pointId"));
        return runAmusementVerification(
                project, session, credential, pointId, deviceId, staffId, actual.idempotencyKey(), Instant.now());
    }

    /**
     * 阶段1（2026-07-05 用户拍板）：票务核销 → 游乐正路分流入口（{@link AmusementVerificationPort}）。
     *
     * <p>与 controller 直连路径（{@link #createVerificationInternal} 从 businessContext 取 project/session）不同，
     * 本路径【从 point/device + now 自动定位项目与当前 open 场次】——ticketing 侧检出游乐类权益后调本方法，
     * 复用同一份准入+三维容量+消费权益+写 amusement_verifications 的正路核心（{@link #runAmusementVerification}）。
     *
     * <p>【调用方事务内】执行（不加 @Transactional/@GuardedAction：核销权限已在 ticketing verify 端点授权，
     * 本方法仅承接分流，不作为独立高危动作二次门控）。定位不到项目/场次即抛 ApiException，由 ticketing 侧原样上抛。
     */
    @Override
    public Optional<String> verifyAmusementEntry(AmusementEntryRequest request) {
        AmusementEntryRequest actual = requireRequest(request);
        String credential = requireText(actual.credentialPayload(), "credential_payload");
        String pointId = trimToNull(actual.pointId());
        String deviceId = trimToNull(actual.deviceId());
        Instant now = actual.now() == null ? Instant.now() : actual.now();
        // point/device → 项目定位：定位不到=本核销点非游乐闸 → 返回 empty，调用方回退通用旧路（不硬造关联、不报错）。
        Optional<String> resolved = repository.findProjectIdByPointOrDevice(pointId, deviceId);
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        String projectId = resolved.get();
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        // 已确认为游乐闸：以下任一失败即报错（committed，不再回退）。
        if (!"open".equals(project.openStatus())) {
            throw ApiException.conflict("PROJECT_NOT_OPEN", "project is not open");
        }
        // 当前场次定位（项目 + now，start_time≤now≤end_time 且 status=open）；无可入场次即报错。
        AmusementSessionRecord session = repository.resolveOpenSessionForProject(projectId, now)
                .orElseThrow(() -> ApiException.conflict(
                        "AMUSEMENT_NO_OPEN_SESSION", "no open amusement session at this time"));
        String staffId = trimToDefault(actual.staffId(), "staff");
        AmusementVerificationRecord record = runAmusementVerification(
                project, session, credential, pointId, deviceId, staffId, actual.idempotencyKey(), now);
        return Optional.of(record.id());
    }

    /**
     * 游乐核销正路核心（controller 直连 & ticketing 分流共用）：项目 open+巡检准入 → 识别/校验权益 →
     * 先消费票务权益（used_times+1）→ 三维容量原子扣减（任一维度失败补偿回补权益）→ 写 amusement_verifications。
     */
    private AmusementVerificationRecord runAmusementVerification(
            AmusementProjectRecord project,
            AmusementSessionRecord session,
            String credential,
            String pointId,
            String deviceId,
            String staffId,
            String idempotencyKey,
            Instant now) {
        if (!project.id().equals(session.projectId())) {
            throw ApiException.conflict("SESSION_PROJECT_MISMATCH", "session does not belong to project");
        }
        if (!"open".equals(session.status())) {
            throw ApiException.conflict("SESSION_NOT_OPEN", "session is not open");
        }
        AmusementOpenEligibilityResponse eligibility = evaluateOpenEligibility(project.id(), null, deviceId);
        if (!eligibility.allowed()) {
            throw ApiException.conflict("SAFETY_BLOCKED", "safety gate blocked amusement verification");
        }

        // §6.4 步骤 2/3：识别票/权益实例 → 在线校验（存在、active、有效期、剩余次数、游乐类、绑定本项目）。
        AmusementEntitlementView entitlement = entitlementGateway.findEntitlementByCredential(credential)
                .orElseThrow(() -> ApiException.notFound("TICKET_NOT_FOUND", "ticket entitlement not found"));
        if (!"active".equals(entitlement.status())) {
            throw ApiException.conflict("ENTITLEMENT_USED", "entitlement is not active");
        }
        if ((entitlement.validFrom() != null && now.isBefore(entitlement.validFrom()))
                || (entitlement.validTo() != null && now.isAfter(entitlement.validTo()))) {
            throw ApiException.conflict("ENTITLEMENT_EXPIRED", "entitlement is not within validity window");
        }
        if (entitlement.remainingTimes() <= 0) {
            throw ApiException.conflict("ENTITLEMENT_USED", "entitlement has no remaining times");
        }
        if (!isAmusementEntitlementType(entitlement.entitlementType())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_AMUSEMENT", "entitlement is not an amusement entitlement");
        }
        // 绑定本项目：该权益 type 在 amusement_project_entitlements 内。
        // 回退口径（V082 向后兼容）：项目无任何绑定配置时，仅按「游乐类」放行（上方已校验）。
        List<String> boundTypes = repository.listProjectEntitlementTypes(project.id());
        if (!boundTypes.isEmpty() && !boundTypes.contains(entitlement.entitlementType())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_FOR_PROJECT", "entitlement is not bound to this project");
        }

        int peopleCount = Math.max(entitlement.numPeople(), 1);
        // §6.4 步骤 4/6：先消费票务权益（原子扣 used_times），再按三维度扣容量。
        // 顺序与回滚：权益扣减失败不扣容量；容量任一维度扣减失败则回补权益（补偿），保证两者一致。
        if (!entitlementGateway.consumeEntitlement(entitlement.id())) {
            throw ApiException.conflict("ENTITLEMENT_USED", "entitlement has no remaining times");
        }
        // 清单#6 超容处置策略（§6.4/§7.18 + R6-D10）：硬上限阻断+软上限告警放行，均落
        // amusement.incident.over_capacity 事件；处置模式后台可切换（block_hard 默认/warn_only 仅告警留痕不阻断）。
        AmusementOverCapacityPolicy policy = repository.findOverCapacityPolicy(project.id());
        try {
            // §6.4 三维度取最小可放行余量：对已配置的受控维度（场次 / 点位·设备 / 项目安全上限）
            // 原子 check+increment，任一维度余量为 0 即拒绝（session_full / SESSION_CAPACITY_EXHAUSTED）；
            // 未配置维度跳过（无 capacity_config 退化为场次单维，向后兼容）。
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
                recordOverCapacity(project, session, deviceId, "hard",
                        claim.exhaustedDimension(), policy);
                throw ApiException.conflict("SESSION_CAPACITY_EXHAUSTED",
                        "session capacity exhausted (dimension=" + claim.exhaustedDimension() + ")");
            }
        } catch (RuntimeException exception) {
            // 容量扣减失败：回补已消费的权益，避免权益被白扣。
            entitlementGateway.reverseEntitlement(entitlement.id());
            throw exception;
        }
        // 软上限（运营容量）告警：放行后若场次在场达到运营容量，落 over_capacity incident + 发事件（不阻断）。
        if (policy.hasOperatingCapacity()) {
            repository.findSessionById(session.id()).ifPresent(after -> {
                if (after.usedCapacity() + after.lockedCapacity() >= policy.operatingCapacity()) {
                    recordOverCapacity(project, session, deviceId, "soft", "operating_capacity", policy);
                }
            });
        }
        // 阶段2：入场记录留痕 entitlementId + locationPointId（出场释放并发容量按同一 point/device 维度键 -N；
        // entitlementId 参与「同一权益同一场次至多一条未释放入场」并发唯一约束）。direction=entry、未释放。
        return repository.saveVerification(AmusementVerificationRecord.entry(
                "verification_" + slug(idempotencyKey),
                project.id(),
                session.id(),
                credential,
                deviceId,
                staffId,
                "success",
                null,
                List.of(),
                now,
                peopleCount,
                entitlement.id(),
                pointId));
    }

    /**
     * 阶段2（2026-07-05 用户拍板）出场核销：自由进出型（并发容量）客人/闸机出场扫码 → 并发容量 -N 释放。
     *
     * <p>据凭证定位其【未释放的成功入场】记录 → 按入场留痕的 point/device 维度键把
     * amusement_capacity_snapshot.occupied_count -N（N=入场放行人数，下限 0）→ 标记入场 released_at、落 direction=exit 记录。
     * 幂等：同一入场重复出场只释放一次（条件 UPDATE 赢家判定 + 唯一约束兜底）。
     *
     * <p>场次/批次型（仅场次维、无并发快照）出场无并发容量可释放 → decrement 命中 0 行、天然幂等跳过
     * （不误减 amusement_sessions.used_capacity）。无未释放入场（未曾入场/已出场）→ ENTITLEMENT_NO_ACTIVE_ENTRY。
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
                .orElseThrow(() -> ApiException.notFound("TICKET_NOT_FOUND", "ticket entitlement not found"));
        final String entId = entitlement.id();
        java.util.Optional<AmusementVerificationRecord> active = repository.findActiveEntryByEntitlement(entId);
        if (active.isEmpty()) {
            // 无未释放入场：要么已出场（幂等：返回既有 exit，不重复减容量），要么从未入场（报错）。
            return repository.findLatestExitByEntitlement(entId)
                    .orElseThrow(() -> ApiException.conflict(
                            "ENTITLEMENT_NO_ACTIVE_ENTRY", "no active amusement entry to release for this entitlement"));
        }
        AmusementVerificationRecord entry = active.get();
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
            return repository.findExitByEntry(entry.id()).orElse(exitRecord);
        }
        return exitRecord;
    }

    /**
     * 清单#6：超容（硬/软）落 over_capacity incident（复用 amusement_incidents 写入路径）+
     * 发 amusement.incident.over_capacity 事件（复用既有 AuditEventService）。
     *
     * @param overType 超容类型：hard=安全容量（硬上限）/ soft=运营容量（软上限）
     * @param dimension 触发维度（session / point_device / project_safety / operating_capacity）
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
                authorizationService.currentMerchantScope().orElse(null)
        ));
        // 双写统一责任链（fail-safe，不阻塞核销主链；孤岛 amusement_incidents 仍权威）。
        reportToUnifiedRiskAlerts(
                "over_capacity",
                incident.id(),
                incident.severity(),
                project.id(),
                "amusement over capacity: type=" + overType + ", dimension=" + dimension,
                "incident=" + incident.id() + "; over_type=" + overType + "; dimension=" + dimension);
        // 事故停运联动：硬超容（severity=high）自动停运停售（open→paused，CAS 幂等），与 createIncident 高危事故一致，
        // 消除"硬超容仍照常核销售票"的安全敞口；恢复营业须走巡检/检修复核（reopen），不自动恢复。
        if ("high".equals(incident.severity())) {
            safetyRepository.updateProjectOpenStatus(project.id(), "open", "paused");
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
        try {
            riskAlertService.report(new RiskAlertReportRequest(
                    incidentId + ":unified",
                    "amusement",
                    incidentId,
                    alertType,
                    mapRiskLevel(severity),
                    summary,
                    projectId,
                    reason,
                    List.of()));
        } catch (RuntimeException exception) {
            // 统一表上报失败不影响核销主链（incident 已落孤岛表 amusement_incidents）。
            auditEventService.recordFailure(
                    "amusement.risk.unified_alert_failed",
                    "amusement.verification.execute",
                    "risk_alert",
                    incidentId,
                    null,
                    incidentId + ":unified",
                    exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName());
        }
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

    private AmusementManualOverrideRecord createManualOverrideInternal(AmusementManualOverrideRequest request) {
        AmusementManualOverrideRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        String sessionId = trimToNull(actual.sessionId());
        if (sessionId != null) {
            repository.findSessionById(sessionId)
                    .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "session not found"));
        }
        String type = enumOrDefault(actual.overrideType(), null, OVERRIDE_TYPES, "override_type");
        String reason = requireText(actual.reason(), "reason");
        // 阶段2 主管释放（release）：异常清场——清零本项目并发在场快照（point_device+project 维），
        // 用于漏扫出场堆积。场次/批次型（session 维、amusement_sessions.used_capacity）不受影响。
        // 授权已由 @GuardedAction(amusement.verification.override) 限主管+审批留痕；本记录即审计凭据（谁/何时/原因/证据）。
        if ("release".equals(type)) {
            repository.clearProjectConcurrentCapacity(projectId, Instant.now());
        }
        String status = switch (type) {
            case "allow" -> "approved";
            case "deny" -> "denied";
            case "release" -> "released";
            default -> "recorded";
        };
        return repository.saveManualOverride(new AmusementManualOverrideRecord(
                "override_" + slug(actual.idempotencyKey()),
                projectId,
                sessionId,
                type,
                reason,
                status,
                trimToNull(actual.operatorNote()),
                evidence(actual.evidenceAttachmentIds()),
                Instant.now()
        ));
    }

    private void checkProjectOwnership(String projectId) {
        authorizationService.currentMerchantScope().ifPresent(merchantId -> {
            AmusementProjectRecord project = repository.findProjectById(projectId)
                    .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
            if (!merchantId.equals(project.merchantId())) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "project does not belong to your merchant");
            }
        });
    }

    /**
     * T3.1 巡检到期调度（激活 V143 已建表 amusement_inspection_tasks）：遍历每个项目的在役(active)设备，按
     * 「设备级安全检查周期（缺省回退项目级，默认 24h）」从最近 PASS 巡检时间推算到期点（从未巡检则立即到期），
     * 幂等 upsert 一张 pending 待办（同到期点不重复）；再把已到期未完成的待办批量翻 overdue。
     * 由 {@link AmusementInspectionTaskJob} 薄 @Scheduled 包装调用（默认关），亦供单测直调对拍。返回本次遍历产生的 upsert 次数。
     */
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

    private AmusementOpenEligibilityResponse evaluateOpenEligibility(String projectId, String sessionId, String deviceId) {
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        List<String> reasons = new ArrayList<>();
        if (!"open".equals(project.openStatus())) {
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
                    .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "session not found"));
            if (!"open".equals(session.status())) {
                reasons.add("SESSION_NOT_OPEN");
            }
            if (session.usedCapacity() + session.lockedCapacity() >= session.capacity()) {
                reasons.add("SESSION_CAPACITY_EXHAUSTED");
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
        if (response instanceof AmusementManualOverrideRecord record) {
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
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
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
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return actual;
    }

    private int positiveInteger(Integer value, String fieldName) {
        int actual = value == null ? 0 : value;
        if (actual <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
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
