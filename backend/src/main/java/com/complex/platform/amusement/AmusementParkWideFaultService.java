package com.complex.platform.amusement;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.amusement.dto.ParkWideFaultConfirmRequest;
import com.complex.platform.amusement.dto.ParkWideFaultInitiateRequest;
import com.complex.platform.amusement.dto.ParkWideFaultLiftRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-08a 全园故障状态（ENUM「全园故障与补偿通票」）：
 * 「全园故障状态由游乐公司负责人和工程或安全岗位共同确认后生效，一经生效，必须立即停止所有纯游乐门票和所有包含游乐权益的通票的
 * 新销售；全园故障状态的解除同样由游乐公司负责人和工程或安全岗位共同确认，解除后恢复上述票种的新销售」。
 *
 * <p>生效与解除各两步：一个岗位发起、另一个岗位确认；两次须不同人且分别持对应岗位权限。按综合体项目一条未解除记录。
 */
@Service
public class AmusementParkWideFaultService {

    /** 游乐公司负责人。 */
    public static final String ROLE_HEAD = "amusement_head";
    /** 工程或安全岗位。 */
    public static final String ROLE_ENGINEERING_SAFETY = "engineering_safety";

    static final String PERMISSION_VIEW = "amusement.park_fault.view";
    static final String PERMISSION_HEAD = "amusement.park_fault.confirm_as_head";
    static final String PERMISSION_ENGINEERING_SAFETY = "amusement.park_fault.confirm_as_engineering_safety";

    static final String STATUS_PENDING_ACTIVATION = "pending_activation";
    static final String STATUS_ACTIVE = "active";
    static final String STATUS_PENDING_LIFT = "pending_lift";
    static final String STATUS_LIFTED = "lifted";

    private static final String TARGET_TYPE = "amusement_park_wide_fault";

    private final JdbcAmusementParkWideFaultRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final IdempotencyService idempotencyService;

    public AmusementParkWideFaultService(JdbcAmusementParkWideFaultRepository repository,
            AuthorizationService authorizationService, AuditEventService auditEventService,
            IdempotencyService idempotencyService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.idempotencyService = idempotencyService;
    }

    public static String roleLabel(String role) {
        return ROLE_HEAD.equals(role) ? "游乐公司负责人" : ROLE_ENGINEERING_SAFETY.equals(role) ? "工程或安全岗位" : "未知岗位";
    }

    public static String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case STATUS_PENDING_ACTIVATION -> "待第二岗位确认生效";
            case STATUS_ACTIVE -> "生效中（已停售纯游乐票与含游乐权益通票）";
            case STATUS_PENDING_LIFT -> "待第二岗位确认解除（仍停售）";
            case STATUS_LIFTED -> "已解除";
            default -> "未知状态";
        };
    }

    /** 发起全园故障状态：发起岗位任一（负责人或工程/安全）；同综合体已有未解除记录则拒绝。 */
    @Transactional
    public AmusementParkWideFaultRecord initiate(ParkWideFaultInitiateRequest request) {
        String permission = permissionForRoleOrView(request == null ? null : request.role());
        try {
            String role = requireRoleWithPermission(request == null ? null : request.role());
            // 第十八批 G 段 82b-R5：项目须在发起岗位权限码可触及的范围内（含可跨项目角色勾选的项目），不带取本人项目
            String projectId = authorizationService.requireReachableProject(request.projectId(), permission,
                    AuthorizationService.PROJECT_OUT_OF_REACH);
            String reason = requireText(request.reason(), "必须填写全园故障情形（全部游乐项目不可运行，或供电、消防、入口系统等公共关键设施故障）");
            return idempotencyService.execute("initiateParkWideFault", request.idempotencyKey(), request, () -> {
                repository.findOpenByProject(projectId).ifPresent(open -> {
                    throw ApiException.conflict("PARK_WIDE_FAULT_ALREADY_OPEN",
                            "该综合体已有未解除的全园故障状态（" + statusLabel(open.status()) + "），不得重复发起");
                });
                AccessActor actor = authorizationService.currentActor();
                Instant now = Instant.now();
                String id = "apwf_" + UUID.randomUUID().toString().replace("-", "");
                AmusementParkWideFaultRecord record = repository.insert(new AmusementParkWideFaultRecord(
                        id, projectId, STATUS_PENDING_ACTIVATION, reason, actor.id(), role, now,
                        null, null, null, null, null, null, null, null, null, null, now, now));
                trace(record, "initiate", actor.id(), role, reason, now);
                auditEventService.recordSuccess("amusement.park_fault.initiated", permission, TARGET_TYPE, id, null,
                        request.idempotencyKey(), Map.of("project_id", projectId, "role", role));
                return record;
            }, AmusementParkWideFaultRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("amusement.park_fault.initiate_failed", permission, TARGET_TYPE,
                    request == null ? null : request.projectId(), null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /** 第二岗位确认生效：须与发起人不同人、且为另一岗位；确认即生效（自此停售）。 */
    @Transactional
    public AmusementParkWideFaultRecord confirmActivation(String faultId, ParkWideFaultConfirmRequest request) {
        String permission = permissionForRoleOrView(request == null ? null : request.role());
        try {
            String role = requireRoleWithPermission(request == null ? null : request.role());
            AmusementParkWideFaultRecord fault = requireFaultInScope(faultId, permission);
            return idempotencyService.execute("confirmParkWideFaultActivation", request.idempotencyKey(), request, () -> {
                if (!STATUS_PENDING_ACTIVATION.equals(fault.status())) {
                    throw ApiException.conflict("PARK_WIDE_FAULT_NOT_PENDING_ACTIVATION",
                            "该全园故障状态不在待确认生效阶段（当前：" + statusLabel(fault.status()) + "）");
                }
                AccessActor actor = authorizationService.currentActor();
                requireSecondPerson(fault.initiatedBy(), fault.initiatedRole(), actor, role);
                Instant now = Instant.now();
                AmusementParkWideFaultRecord updated = repository.update(fault.withActivation(actor.id(), role, now));
                trace(updated, "confirm_activation", actor.id(), role, request.note(), now);
                auditEventService.recordSuccess("amusement.park_fault.activated", permission, TARGET_TYPE, faultId, null,
                        request.idempotencyKey(), Map.of("project_id", fault.projectId(), "role", role,
                                "initiated_by", fault.initiatedBy(), "initiated_role", fault.initiatedRole()));
                return updated;
            }, AmusementParkWideFaultRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("amusement.park_fault.activate_failed", permission, TARGET_TYPE, faultId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /** 发起解除：仅生效中的记录；发起岗位任一。 */
    @Transactional
    public AmusementParkWideFaultRecord initiateLift(String faultId, ParkWideFaultLiftRequest request) {
        String permission = permissionForRoleOrView(request == null ? null : request.role());
        try {
            String role = requireRoleWithPermission(request == null ? null : request.role());
            String reason = requireText(request.reason(), "必须填写解除依据（公共关键设施或游乐项目恢复情况）");
            AmusementParkWideFaultRecord fault = requireFaultInScope(faultId, permission);
            return idempotencyService.execute("initiateParkWideFaultLift", request.idempotencyKey(), request, () -> {
                if (!STATUS_ACTIVE.equals(fault.status())) {
                    throw ApiException.conflict("PARK_WIDE_FAULT_NOT_ACTIVE",
                            "只有生效中的全园故障状态可以发起解除（当前：" + statusLabel(fault.status()) + "）");
                }
                AccessActor actor = authorizationService.currentActor();
                Instant now = Instant.now();
                AmusementParkWideFaultRecord updated = repository.update(fault.withLiftInitiated(actor.id(), role, reason, now));
                trace(updated, "initiate_lift", actor.id(), role, reason, now);
                auditEventService.recordSuccess("amusement.park_fault.lift_initiated", permission, TARGET_TYPE, faultId, null,
                        request.idempotencyKey(), Map.of("project_id", fault.projectId(), "role", role));
                return updated;
            }, AmusementParkWideFaultRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("amusement.park_fault.lift_initiate_failed", permission, TARGET_TYPE, faultId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /** 第二岗位确认解除：须与解除发起人不同人、且为另一岗位；确认即解除（恢复销售）。 */
    @Transactional
    public AmusementParkWideFaultRecord confirmLift(String faultId, ParkWideFaultConfirmRequest request) {
        String permission = permissionForRoleOrView(request == null ? null : request.role());
        try {
            String role = requireRoleWithPermission(request == null ? null : request.role());
            AmusementParkWideFaultRecord fault = requireFaultInScope(faultId, permission);
            return idempotencyService.execute("confirmParkWideFaultLift", request.idempotencyKey(), request, () -> {
                if (!STATUS_PENDING_LIFT.equals(fault.status())) {
                    throw ApiException.conflict("PARK_WIDE_FAULT_NOT_PENDING_LIFT",
                            "该全园故障状态不在待确认解除阶段（当前：" + statusLabel(fault.status()) + "）");
                }
                AccessActor actor = authorizationService.currentActor();
                requireSecondPerson(fault.liftInitiatedBy(), fault.liftInitiatedRole(), actor, role);
                Instant now = Instant.now();
                AmusementParkWideFaultRecord updated = repository.update(fault.withLifted(actor.id(), role, now));
                trace(updated, "confirm_lift", actor.id(), role, request.note(), now);
                auditEventService.recordSuccess("amusement.park_fault.lifted", permission, TARGET_TYPE, faultId, null,
                        request.idempotencyKey(), Map.of("project_id", fault.projectId(), "role", role,
                                "lift_initiated_by", fault.liftInitiatedBy(), "lift_initiated_role", fault.liftInitiatedRole()));
                return updated;
            }, AmusementParkWideFaultRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("amusement.park_fault.lift_failed", permission, TARGET_TYPE, faultId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    public PageResponse<AmusementParkWideFaultRecord> list(String projectId, String status) {
        authorizationService.requirePermission(PERMISSION_VIEW);
        List<AmusementParkWideFaultRecord> items = listInReach(projectId, project -> repository.list(project, trimToNull(status)));
        auditEventService.recordSuccess("amusement.park_fault.viewed", PERMISSION_VIEW, TARGET_TYPE, "list", null, null,
                Map.of("count", items.size()));
        return new PageResponse<>(items, 1, Math.max(1, items.size()), items.size());
    }

    public PageResponse<AmusementParkWideFaultActionRecord> listActions(String faultId) {
        authorizationService.requirePermission(PERMISSION_VIEW);
        AmusementParkWideFaultRecord fault = requireFaultInScope(faultId, PERMISSION_VIEW);
        List<AmusementParkWideFaultActionRecord> items = repository.listActions(fault.id());
        return new PageResponse<>(items, 1, Math.max(1, items.size()), items.size());
    }

    // ---------- helpers ----------

    /**
     * 「由游乐公司负责人和工程或安全岗位共同确认」：第二次确认须为另一人（不得同一人换岗位两次确认）、且为另一岗位
     * （两个不同人但同一岗位不构成共同确认）。
     */
    static void requireSecondPerson(String firstActorId, String firstRole, AccessActor actor, String role) {
        if (actor.id().equals(firstActorId)) {
            throw ApiException.forbidden("PARK_WIDE_FAULT_SAME_PERSON_FORBIDDEN",
                    "全园故障状态须由游乐公司负责人与工程或安全岗位两人共同确认，发起人不得自行确认");
        }
        if (role.equals(firstRole)) {
            throw ApiException.conflict("PARK_WIDE_FAULT_ROLE_PAIR_REQUIRED",
                    "发起方岗位为『" + roleLabel(firstRole) + "』，第二次确认须由『" + roleLabel(otherRole(firstRole)) + "』作出");
        }
    }

    private static String otherRole(String role) {
        return ROLE_HEAD.equals(role) ? ROLE_ENGINEERING_SAFETY : ROLE_HEAD;
    }

    private void trace(AmusementParkWideFaultRecord fault, String action, String actorId, String role, String note, Instant at) {
        repository.insertAction(new AmusementParkWideFaultActionRecord(
                "apwfa_" + UUID.randomUUID().toString().replace("-", ""), fault.id(), fault.projectId(), action, actorId, role,
                trimToNull(note), at));
    }

    String requireRoleWithPermission(String role) {
        String r = trimToNull(role);
        if (!ROLE_HEAD.equals(r) && !ROLE_ENGINEERING_SAFETY.equals(r)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "岗位只能是游乐公司负责人（amusement_head）或工程或安全岗位（engineering_safety）");
        }
        authorizationService.requirePermission(permissionFor(r));
        return r;
    }

    static String permissionFor(String role) {
        return ROLE_HEAD.equals(role) ? PERMISSION_HEAD : PERMISSION_ENGINEERING_SAFETY;
    }

    private static String permissionForRoleOrView(String role) {
        String r = trimToNull(role);
        return ROLE_HEAD.equals(r) || ROLE_ENGINEERING_SAFETY.equals(r) ? permissionFor(r) : PERMISSION_VIEW;
    }

    /**
     * 第十八批 G 段 82b-R5（PGM 第 659 行「在所分配的权限内可以查看、处理该角色可查看项目的数据」）：记录所在项目须在入口权限码
     * 可触及的范围内（含可跨项目角色勾选的项目；平台管理员不限），否则 403 CROSS_PROJECT_FORBIDDEN。写入口在幂等执行体之前调用。
     */
    private AmusementParkWideFaultRecord requireFaultInScope(String faultId, String permission) {
        AmusementParkWideFaultRecord fault = repository.findById(faultId)
                .orElseThrow(() -> ApiException.notFound("PARK_WIDE_FAULT_NOT_FOUND", "全园故障状态记录不存在"));
        if (!authorizationService.canReachProject(authorizationService.currentActor(), fault.projectId(), permission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "该全园故障状态不在本权限可触及的项目范围内");
        }
        return fault;
    }

    /**
     * 第十八批 G 段 82b-R5：全园故障与补偿批次列表按 amusement.park_fault.view 可触及的项目取——平台管理员照旧（带项目只看该项目、
     * 不带看全部）；其余带项目的须可触及（否则 403 CROSS_PROJECT_FORBIDDEN），不带的取全部可触及项目。
     * ponytail: 逐项目各查一次，可触及项目多到成为负担时改成一条 IN 查询。
     */
    <T> List<T> listInReach(String projectId, java.util.function.Function<String, List<T>> query) {
        String requested = trimToNull(projectId);
        if (authorizationService.isPlatformAdmin(authorizationService.currentActor())) {
            return query.apply(requested);
        }
        AuthorizationService.ProjectReach reach = authorizationService.projectReach(authorizationService.currentActor(), PERMISSION_VIEW);
        if (requested != null && !reach.covers(requested)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", AuthorizationService.PROJECT_OUT_OF_REACH);
        }
        if (requested != null) {
            return query.apply(requested);
        }
        return reach.onlyOwnProject().map(query)
                .orElseGet(() -> reach.organizationIds().stream().flatMap(project -> query.apply(project).stream()).toList());
    }

    private static String requireText(String value, String message) {
        String v = trimToNull(value);
        if (v == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", message);
        }
        return v;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String codeOf(RuntimeException exception) {
        return exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR";
    }
}
