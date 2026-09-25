package com.complex.platform.access;

// 文件版本：V0.27

import com.complex.platform.access.dto.AccessGrantApplyRequest;
import com.complex.platform.access.dto.AccessGrantReviewRequest;
import com.complex.platform.access.dto.AccessUserRequest;
import com.complex.platform.access.dto.PermissionTemplateApplyRequest;
import com.complex.platform.access.dto.PermissionTemplateRequest;
import com.complex.platform.access.dto.PositionRoleTemplateRequest;
import com.complex.platform.access.dto.RoleAssignmentRequest;
import com.complex.platform.access.dto.RoleRequest;
import com.complex.platform.access.dto.StaffLevelAssignmentRequest;
import com.complex.platform.access.dto.StatusChangeRequest;
import com.complex.platform.access.dto.TemporaryGrantRequest;
import com.complex.platform.access.dto.UserPermissionOverrideRequest;
import com.complex.platform.access.dto.UserPositionAssignmentRequest;
import com.complex.platform.access.dto.UserScopeChangeRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class AccessService {

    private static final int DEFAULT_PAGE = 1;
    private static final Set<String> ALLOWED_ACCOUNT_TYPES = Set.of(
            "staff", "merchant", "merchant_admin", "supervisor", "sales", "outsourced_vendor");
    private static final String REQUEST_USER_ROLE = "user_role";
    private static final String REQUEST_USER_POSITION = "user_position";
    private static final String REQUEST_TEMPORARY_PERMISSION = "temporary_permission";
    // M087：内部生成的申请类型（不接受外部 applyGrant 直接提交）。
    static final String REQUEST_USER_SCOPE_CHANGE = "user_scope_change";
    static final String REQUEST_POSITION_HIGH_RISK_RELEASE = "position_high_risk_release";
    // M002-F01（PGM「支持员工级完全自定义配置」「授出前须先审批才生效」）：员工单独授予、解除员工显式拒绝的授权申请，只收须审批权限码。
    static final String REQUEST_USER_PERMISSION_GRANT = "user_permission_grant";
    static final String REQUEST_USER_PERMISSION_DENY_REMOVAL = "user_permission_deny_removal";

    private final AccessRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final AccessCredentialProvisioner credentialProvisioner;
    private final SupervisorReviewService supervisorReviewService;
    private final boolean grantApprovalEnabled;

    @Autowired
    public AccessService(
            AccessRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            ObjectProvider<AccessCredentialProvisioner> credentialProvisioner,
            ObjectProvider<SupervisorReviewService> supervisorReviewService
    ) {
        this(repository, idempotencyService, auditEventService, authorizationService,
                credentialProvisioner.getIfAvailable(),
                // 治理地图「四类高危权限授出前须先审批才生效」：生产不给全局逃生口——
                // 原 @Value("${access.grant_approval.enabled:true}") 一票否决四个调用点，关闭时无审计、
                // 无启动告警，静默失效（219 条 requires_approval=true 全押在它身上）。
                // 该开关唯一实际用途是让测试少走审批，故只保留包级构造器的入参。
                supervisorReviewService.getIfAvailable(), true,
                0, 0);
    }

    AccessService(
            AccessRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            AccessCredentialProvisioner credentialProvisioner,
            SupervisorReviewService supervisorReviewService,
            boolean grantApprovalEnabled,
            int summaryPositionThreshold,
            int summaryUserThreshold
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.credentialProvisioner = credentialProvisioner;
        this.supervisorReviewService = supervisorReviewService;
        this.grantApprovalEnabled = grantApprovalEnabled;
    }

    public PageResponse<PermissionTemplate> listPermissionTemplates() {
        AccessActor actor = authorizationService.currentActor();
        String orgId = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
        List<PermissionTemplate> templates = repository.listTemplates(orgId);
        auditEventService.recordSuccess(
                "permission_template.viewed",
                "access.permission_template.view",
                "permission_template",
                "all",
                null,
                null,
                Map.of("count", templates.size())
        );
        return page(templates);
    }

    public PermissionTemplate createPermissionTemplate(PermissionTemplateRequest request) {
        AccessActor actor = authorizationService.currentActor();
        String ownerScopeId = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
        return executeTemplateWrite(
                "createPermissionTemplate",
                "permission_template.changed",
                "permission_template.change_failed",
                "access.permission_template.manage",
                request,
                () -> {
                    requireKnownPermissions(request.permissionCodes());
                    PermissionTemplate template = templateFromRequest("tpl_" + UUID.randomUUID(), request, null, ownerScopeId);
                    return repository.createTemplate(template, items(template.id(), request.permissionCodes(), request.dataScope()));
                }
        );
    }

    public PermissionTemplate updatePermissionTemplate(String id, PermissionTemplateRequest request) {
        AccessActor actor = authorizationService.currentActor();
        return executeTemplateWrite(
                "updatePermissionTemplate:" + id,
                "permission_template.changed",
                "permission_template.change_failed",
                "access.permission_template.manage",
                request,
                () -> {
                    repository.lockTemplateForEdit(id); // M001-F13：锁内读前态，并发编辑按实际前态判闸、记增减
                    PermissionTemplate existing = repository.findTemplateById(id)
                            .orElseThrow(() -> ApiException.notFound("TEMPLATE_NOT_FOUND", "模板不存在：" + id));
                    if (!authorizationService.isPlatformAdmin(actor)
                            && !authorizationService.requireProjectScope(actor).equals(existing.ownerScopeId())) {
                        throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "不能修改其他组织的模板");
                    }
                    requireKnownPermissions(request.permissionCodes());
                    requireTemporaryTimeOrder(request);  // M001-F07：倒置窗口先于「延长有效期须重新授权」判定，按 400 拒绝
                    List<PermissionTemplateItem> previousItems = repository.findTemplateItems(id);
                    boolean widensWindow = (existing.validFrom() != null
                            && (request.validFrom() == null || request.validFrom().isBefore(existing.validFrom())))
                            || (existing.validUntil() != null
                            && (request.validUntil() == null || request.validUntil().isAfter(existing.validUntil())));
                    boolean changesScope = previousItems.stream().anyMatch(item ->
                            safeList(request.permissionCodes()).contains(item.permissionCode())
                                    && !java.util.Objects.equals(item.dataScope(), trimToDefault(request.dataScope(), "project")));
                    if (widensWindow || (existing.singleUse() && !Boolean.TRUE.equals(request.singleUse()))
                            || changesScope || previousItems.stream().anyMatch(item -> item.effect() == AccessRepository.Effect.DENY)
                            || !"active".equals(existing.status())) {
                        throw ApiException.conflict("TEMPLATE_REAUTHORIZATION_REQUIRED",
                                "有效期、一次性限制、数据范围或禁止规则变化须复制为新模板后重新授权");
                    }
                    Set<String> previousGrants = previousItems.stream()
                            .filter(item -> item.effect() == AccessRepository.Effect.GRANT)
                            .map(PermissionTemplateItem::permissionCode).collect(java.util.stream.Collectors.toSet());
                    List<String> added = safeList(request.permissionCodes()).stream()
                            .filter(code -> !previousGrants.contains(code)).toList();
                    requireActorCanGrant(added);
                    requireGrantApprovalIfNeeded(added);
                    PermissionTemplate template = templateFromRequest(id, request, existing.sourceTemplateId(), existing.ownerScopeId());
                    PermissionTemplate saved = repository.saveTemplate(template, items(template.id(), request.permissionCodes(), request.dataScope()));
                    // M001-F11（PGM 权限规则「权限的授予、收回……全部留痕」）：增减即时作用于仍生效的套用员工，逐人留痕。
                    List<String> removed = previousGrants.stream()
                            .filter(code -> !safeList(request.permissionCodes()).contains(code)).sorted().toList();
                    if (!added.isEmpty() || !removed.isEmpty()) {
                        for (PermissionTemplateApplication application : repository.findActiveTemplateApplications(id)) {
                            auditEventService.recordSuccess(
                                    "permission_template.effect_changed",
                                    "access.permission_template.manage",
                                    "access_user",
                                    application.targetId(),
                                    null,
                                    request.idempotencyKey(),
                                    Map.of("template_id", id, "application_id", application.id(),
                                            "added_permission_codes", added, "removed_permission_codes", removed,
                                            "operator_id", actor.id()) // M001-F14：操作人（两类认证主体都能解析），时间取事件发生时间
                            );
                        }
                    }
                    return saved;
                }
        );
    }

    public PermissionTemplate copyPermissionTemplate(String id, PermissionTemplateRequest request) {
        AccessActor actor = authorizationService.currentActor();
        return executeTemplateWrite(
                "copyPermissionTemplate:" + id,
                "permission_template.copied",
                "permission_template.copy_failed",
                "access.permission_template.manage",
                request,
                () -> {
                    if (!authorizationService.isPlatformAdmin(actor)) {
                        // M006-036：先取项目范围（无组织主体在此得到 PROJECT_SCOPE_REQUIRED），再比较归属，不再对 null 调用 equals。
                        String scope = authorizationService.requireProjectScope(actor);
                        PermissionTemplate source = repository.findTemplateById(id)
                                .orElseThrow(() -> ApiException.notFound("TEMPLATE_NOT_FOUND", "模板不存在：" + id));
                        if (source.ownerScopeId() != null && !scope.equals(source.ownerScopeId())) {
                            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "不能复制其他组织的模板");
                        }
                    }
                    String ownerScopeId = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
                    return repository.copyTemplate(id, templateFromRequest("tpl_" + UUID.randomUUID(), request, id, ownerScopeId));
                }
        );
    }

    public PermissionTemplateApplyResult applyPermissionTemplate(String id, PermissionTemplateApplyRequest request) {
        AccessActor actor = authorizationService.currentActor();
        try {
            return idempotencyService.execute(
                    "applyPermissionTemplate:" + id,
                    request.idempotencyKey(),
                    request,
                    () -> {
                        if (safeList(request.targetUserIds()).isEmpty()) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "必须选择授权目标");
                        }
                        List<String> targetUserIds = request.targetUserIds().stream()
                                .map(userId -> requireText(userId, "user_id"))
                                .toList();
                        // M001-F02（PGM 权限规则，2026-09-24 操作员裁决）：同一次套用中同一员工只能出现一次，重复即整单拒绝。
                        Set<String> seen = new HashSet<>();
                        Set<String> duplicates = new LinkedHashSet<>();
                        for (String userId : targetUserIds) {
                            if (!seen.add(userId)) {
                                duplicates.add(userId);
                            }
                        }
                        if (!duplicates.isEmpty()) {
                            throw ApiException.badRequest("VALIDATION_FAILED",
                                    "同一次套用中员工重复：" + String.join("、", duplicates));
                        }
                        // M001-F10：先按编号顺序锁目标员工行再读归属，与换项目串行。
                        repository.lockAccessUsers(targetUserIds.stream().sorted().toList());
                        List<AccessUser> targets = new ArrayList<>();
                        for (String userId : targetUserIds) {
                            AccessUser target = requireTargetUser(userId);
                            requireSameOrganization(actor, target);
                            targets.add(target);
                        }
                        repository.lockTemplateForApply(id); // M001-F13：等进行中的模板编辑提交后再读条目，快照即生效码
                        PermissionTemplate template = repository.findTemplateById(id)
                                .orElseThrow(() -> ApiException.notFound("PERMISSION_TEMPLATE_NOT_FOUND", "权限模板不存在：" + id));
                        // A1-05：apply 必须校验模板归属——非平台管理员只能套用本组织拥有的模板，
                        // 禁止借平台模板/他组织模板把高权限项下发给本组织用户（与 update/copy 同范式）。
                        if (!authorizationService.isPlatformAdmin(actor)) {
                            if (template.ownerScopeId() == null
                                    || !template.ownerScopeId().equals(actor.organizationId())) {
                                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                        "不能应用其他范围的模板：" + id);
                            }
                        }
                        // M001-F09（PGM 权限规则，2026-09-24 操作员裁决）：项目自有模板只能套给本项目员工，平台管理员同样；
                        // 跨项目使用须先复制为平台模板再套用。
                        targets.forEach(target -> requireTemplateAssignableToTarget(template, target));
                        // P0-1：套用模板授予的权限码不得超出授权人自身有效权限（堵 copy-then-apply 提权）。
                        List<PermissionTemplateItem> templateItems = repository.findTemplateItems(id);
                        List<String> templatePermissionCodes = templateItems.stream()
                                .map(PermissionTemplateItem::permissionCode)
                                .toList();
                        requireActorCanGrant(templatePermissionCodes);
                        // AUD-011：模板应用复用唯一授权政策引擎——含高风险(requiresApproval)权限的模板不得经模板入口
                        // 批量直发绕过职责分离/主管复核，与角色/岗位/临时授权同口径 fail-closed(GRANT_REQUIRES_APPROVAL)。
                        // 高风险授权须改走 applyGrant/reviewGrant 主管复核流；模板入口对高风险直接拒绝、无待审窗口，
                        // 故审批期间模板内容变化亦无从偷换。
                        requireGrantApprovalIfNeeded(templatePermissionCodes);
                        boolean singleUse = template.singleUse() || "single_use".equalsIgnoreCase(trimToDefault(request.mode(), "append"));
                        requireBoundedTemporaryTemplate(template.templateType(), template.validUntil(), singleUse);
                        // M001-F04（PGM 权限规则：授予全部留痕）：授予人记实际操作者，审计事件逐条带套用记录 id。
                        PermissionTemplateApplyResult result = new PermissionTemplateApplyResult(repository.applyTemplate(
                                id,
                                targetUserIds,
                                template.validFrom(),
                                template.validUntil(),
                                singleUse,
                                actor.id()
                        ));
                        auditEventService.recordSuccess(
                                "permission_template.applied",
                                "access.permission_template.apply",
                                "permission_template",
                                id,
                                null,
                                request.idempotencyKey(),
                                Map.of("target_count", result.applications().size(),
                                        "application_ids", result.applications().stream().map(PermissionTemplateApplication::id).toList(),
                                        // M001-F11：套用当时的权限码快照，授予与禁止分列
                                        "permission_codes", templateItems.stream().filter(item -> item.effect() == AccessRepository.Effect.GRANT)
                                                .map(PermissionTemplateItem::permissionCode).toList(),
                                        "denied_permission_codes", templateItems.stream().filter(item -> item.effect() == AccessRepository.Effect.DENY)
                                                .map(PermissionTemplateItem::permissionCode).toList())
                        );
                        return result;
                    },
                    PermissionTemplateApplyResult.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "permission_template.apply_failed",
                    "access.permission_template.apply",
                    "permission_template",
                    id,
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public PageResponse<PermissionDefinition> listPermissions() {
        List<PermissionDefinition> permissions = repository.listPermissions();
        auditEventService.recordSuccess(
                "permission.viewed",
                "access.permission.view",
                "permission",
                "all",
                null,
                null,
                Map.of("count", permissions.size())
        );
        return page(permissions);
    }

    public PageResponse<RoleRecord> listRoles() {
        AccessActor actor = authorizationService.currentActor();
        String orgId = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
        List<RoleRecord> roles = repository.listRoles(orgId);
        auditEventService.recordSuccess(
                "role.viewed",
                "access.role.view",
                "role",
                "all",
                null,
                null,
                Map.of("count", roles.size())
        );
        return page(roles);
    }

    public RoleRecord createRole(RoleRequest request) {
        AccessActor actor = authorizationService.currentActor();
        String orgId = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
        try {
            return idempotencyService.execute(
                    "createRole",
                    request.idempotencyKey(),
                    request,
                    () -> {
                        requireKnownPermissions(request.permissionCodes());
                        // P0-1：角色不得包含超出创建人自身有效权限的权限码（提权防护）。
                        requireActorCanGrant(request.permissionCodes());
                        RoleRecord role = new RoleRecord(
                                "role_" + UUID.randomUUID(),
                                trimToDefault(request.roleName(), "role"),
                                request.description(),
                                trimToDefault(request.dataScope(), "project"),
                                "active",
                                Instant.now(),
                                safeList(request.permissionCodes()),
                                orgId
                        );
                        RoleRecord saved = repository.createRole(role);
                        auditEventService.recordSuccess("role.changed", "access.role.manage", "role", saved.id(), null, request.idempotencyKey(), Map.of());
                        return saved;
                    },
                    RoleRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("role.change_failed", "access.role.manage", "role", null, null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    public RoleAssignmentRecord assignUserRole(RoleAssignmentRequest request) {
        RoleAssignmentRequest r = requireRequest(request);
        try {
            return idempotencyService.execute(
                    "assignUserRole:" + trimToDefault(r.userId(), "") + ":" + trimToDefault(r.roleId(), ""),
                    r.idempotencyKey(),
                    r,
                    () -> assignUserRoleDirect(r, true),
                    RoleAssignmentRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "access.role.assign_failed",
                    "access.role.assign",
                    "user_role",
                    request == null ? null : request.userId(),
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public RoleAssignmentRecord unassignUserRole(RoleAssignmentRequest request) {
        RoleAssignmentRequest r = requireRequest(request);
        try {
            return idempotencyService.execute(
                    "unassignUserRole:" + trimToDefault(r.userId(), "") + ":" + trimToDefault(r.roleId(), ""),
                    r.idempotencyKey(),
                    r,
                    () -> {
                        AccessActor actor = authorizationService.currentActor();
                        AccessUser targetUser = requireTargetUser(requireText(r.userId(), "user_id"));
                        RoleRecord role = requireRole(requireText(r.roleId(), "role_id"));
                        requireSameOrganization(actor, targetUser);
                        requireRoleAssignableToTarget(role, targetUser);
                        requireActorCanGrant(role.permissionCodes());
                        repository.unassignUserRole(targetUser.id(), role.id());
                        auditEventService.recordSuccess(
                                "access.role.unassigned",
                                "access.role.assign",
                                "user_role",
                                targetUser.id() + ":" + role.id(),
                                null,
                                r.idempotencyKey(),
                                Map.of("role_id", role.id())
                        );
                        return new RoleAssignmentRecord(targetUser.id(), role.id());
                    },
                    RoleAssignmentRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "access.role.unassign_failed",
                    "access.role.assign",
                    "user_role",
                    request == null ? null : request.userId(),
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public PageResponse<PositionRoleTemplate> listPositionRoleTemplates() {
        return listPositionRoleTemplates(false);
    }

    public PageResponse<PositionRoleTemplate> listPositionRoleTemplates(boolean includeArchived) {
        AccessActor actor = authorizationService.currentActor();
        List<PositionRoleTemplate> mappings = repository.listPositionRoleTemplates(
                authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor)).stream()
                .filter(mapping -> includeArchived || (mapping.archivedAt() == null && mapping.deletedAt() == null)).toList();
        auditEventService.recordSuccess(
                "access.position_role_template.viewed",
                "access.role.assign",
                "position_role_template",
                "all",
                null,
                null,
                Map.of("count", mappings.size())
        );
        return page(mappings);
    }

    @Transactional
    public PositionRoleTemplate createPositionRoleTemplate(PositionRoleTemplateRequest request) {
        PositionRoleTemplateRequest r = requireRequest(request);
        try {
            return idempotencyService.execute(
                    "createPositionRoleTemplate:" + trimToDefault(r.positionCode(), "") + ":" + trimToDefault(r.staffLevelId(), ""),
                    r.idempotencyKey(),
                    r,
                    () -> savePositionRoleTemplate(
                            "prt_" + UUID.randomUUID(),
                            r
                    ),
                    PositionRoleTemplate.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "access.position_role_template.save_failed",
                    "access.role.assign",
                    "position_role_template",
                    null,
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    @Transactional
    public PositionRoleTemplate updatePositionRoleTemplate(String id, PositionRoleTemplateRequest request) {
        PositionRoleTemplateRequest r = requireRequest(request);
        try {
            return idempotencyService.execute(
                    "updatePositionRoleTemplate:" + id,
                    r.idempotencyKey(),
                    r,
                    () -> savePositionRoleTemplate(requireText(id, "id"), r),
                    PositionRoleTemplate.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "access.position_role_template.save_failed",
                    "access.role.assign",
                    "position_role_template",
                    id,
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public void deletePositionRoleTemplate(String id) {
        throw ApiException.conflict("ARCHIVE_APPROVAL_REQUIRED", "岗位模板须先存档，填写理由申请删除，经最高权限审批后软删除");
    }

    @Transactional
    public PositionRoleTemplate changePositionTemplateLifecycle(String id, StatusChangeRequest request) {
        // M031-003（GEN-011）：生命周期操作失败也留痕，与同文件其余写口同一范式。
        try {
            return changePositionTemplateLifecycleAudited(id, request);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.position_role_template.lifecycle_failed", "access.role.assign",
                    "position_role_template", id, null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private PositionRoleTemplate changePositionTemplateLifecycleAudited(String id, StatusChangeRequest request) {
        requireRequest(request);
        AccessActor actor = authorizationService.currentActor();
        if (!authorizationService.isMasterAuthority(actor)) {
            requirePlatformAdmin(actor);
        }
        String action = requireText(request.action(), "action");
        String reason = requireText(request.reason(), "reason");
        return idempotencyService.execute("positionTemplateLifecycle:" + id, request.idempotencyKey(), request, () -> {
            PositionRoleTemplate current = repository.findPositionRoleTemplateById(id)
                    .orElseThrow(() -> ApiException.notFound("POSITION_ROLE_TEMPLATE_NOT_FOUND", "岗位模板不存在"));
            Instant now = Instant.now();
            PositionRoleTemplate updated;
            switch (action) {
                case "archive" -> {
                    if (current.archivedAt() != null || current.deletedAt() != null) throw ApiException.conflict("INVALID_STATE", "模板已存档");
                    updated = current.withLifecycle(now, null, null, null, now);
                }
                case "restore" -> {
                    if (current.archivedAt() == null) throw ApiException.conflict("INVALID_STATE", "模板未存档");
                    if (current.deleteReviewId() != null && current.deletedAt() == null) throw ApiException.conflict("DELETE_REVIEW_PENDING", "须先处理删除复核");
                    // M031-001（BSM-004「想恢复点"取消存档"即可」）：恢复即回到可定岗的启用态；同范围已有启用中的替代映射时拒绝，不静默留成停用。
                    if (repository.findPositionRoleTemplate(current.organizationId(), current.positionCode(), current.staffLevelId())
                            .filter(active -> java.util.Objects.equals(active.organizationId(), current.organizationId())).isPresent()) {
                        throw ApiException.conflict("POSITION_TEMPLATE_ACTIVE_EXISTS", "同岗位同职级已有启用中的岗位模板，须先停用或存档后再恢复");
                    }
                    updated = current.withLifecycle(null, null, null, null, now);
                }
                case "request_delete" -> {
                    if (current.archivedAt() == null || current.deletedAt() != null || current.deleteReviewId() != null) {
                        throw ApiException.conflict("INVALID_STATE", "须先存档且没有待处理删除申请");
                    }
                    requirePositionTemplateUnused(id);
                    if (authorizationService.isMasterAuthority(authorizationService.currentActor())) {
                        updated = current.withLifecycle(current.archivedAt(), now, null, reason, now);
                    } else {
                        var review = requireSupervisorReviewService().blockForAdmission("position_role_template_delete",
                                "access.position_role_template.delete", "position_role_template", id, "top_authority", 3);
                        updated = current.withLifecycle(current.archivedAt(), null, review.reviewId(), reason, now);
                    }
                }
                case "approve_delete", "reject_delete" -> {
                    if (current.archivedAt() == null || current.deletedAt() != null || current.deleteReviewId() == null) {
                        throw ApiException.conflict("INVALID_STATE", "模板没有待处理删除申请");
                    }
                    if (!authorizationService.isMasterAuthority(authorizationService.currentActor())) {
                        throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", "删除配置须由最高权限（U 盘密钥）审批");
                    }
                    boolean approve = "approve_delete".equals(action);
                    if (approve) requirePositionTemplateUnused(id);
                    var deleteReview = requireSupervisorReviewService().approve(current.deleteReviewId(), new SupervisorReviewApprovalRequest(
                            request.idempotencyKey(), reason, request.evidenceAttachmentIds(), null,
                            approve ? "approved" : "rejected", reason));
                    if (approve && !"approved".equals(deleteReview.reviewStatus())) {
                        updated = current; // M087：删除复核链尚有节点待审，模板状态不变
                    } else {
                        updated = current.withLifecycle(current.archivedAt(), approve ? now : null,
                                approve ? current.deleteReviewId() : null, current.deleteReason(), now);
                    }
                }
                default -> throw ApiException.badRequest("INVALID_ACTION", "不支持的岗位模板生命周期操作");
            }
            repository.savePositionTemplateLifecycle(updated, authorizationService.currentActor().id());
            if ("restore".equals(action)) {
                updated = new PositionRoleTemplate(updated.id(), updated.positionCode(), updated.staffLevelId(), updated.roleId(),
                        updated.templateId(), updated.organizationId(), "active", updated.createdBy(), updated.createdAt(), now);
                repository.savePositionRoleTemplate(updated);
            }
            auditEventService.recordSuccess("access.position_role_template." + action, "access.role.assign",
                    "position_role_template", id, null, request.idempotencyKey(), Map.of("reason", reason));
            return updated;
        }, PositionRoleTemplate.class);
    }

    private void requirePositionTemplateUnused(String id) {
        if (repository.countPositionTemplateUsage(id) != 0) {
            throw ApiException.conflict("POSITION_TEMPLATE_IN_USE", "岗位模板仍被员工岗位使用，不能删除");
        }
    }

    @Transactional
    public UserPositionAssignmentResult assignUserPosition(UserPositionAssignmentRequest request) {
        UserPositionAssignmentRequest r = requireRequest(request);
        try {
            return idempotencyService.execute(
                    "assignUserPosition:" + trimToDefault(r.userId(), ""),
                    r.idempotencyKey(),
                    r,
                    () -> assignUserPositionDirect(r, true),
                    UserPositionAssignmentResult.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "access.user_position.assign_failed",
                    "access.role.assign",
                    "user_position_assignment",
                    request == null ? null : request.userId(),
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    /**
     * M087-A-04/F-M087-04（PGM-1.10.1「保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，
     * 审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批；超出新模板范围的增权保持未生效」）：
     * 人员范围变更写路径——同一事务内：旧岗位派生（角色 + 模板应用）立即撤销 → 账号归属改为新范围 → 新范围岗位权限整单进主管链，
     * 审批通过后由 {@link #applyApprovedGrant} 落地（只落地新岗位模板内的权限）。
     */
    @Transactional
    public AccessGrantRequestRecord changeUserScope(String userId, UserScopeChangeRequest request) {
        UserScopeChangeRequest r = requireRequest(request);
        String id = requireText(userId, "user_id");
        try {
            return idempotencyService.execute(
                    "accessUserScopeChange:" + id,
                    r.idempotencyKey(),
                    r,
                    () -> {
                        repository.lockGrantAuthorizationFacts();
                        AccessActor actor = authorizationService.currentActor();
                        AccessUser target = lockTargetUser(id);
                        requireSameOrganization(actor, target);
                        String newOrganization = trimToDefault(r.organizationId(), target.organizationId());
                        String newMerchant = isBlank(r.merchantId()) ? null : r.merchantId().trim();
                        requireActorCanAssignToScope(actor, newOrganization, newMerchant);
                        if (java.util.Objects.equals(newOrganization, target.organizationId())
                                && java.util.Objects.equals(newMerchant, target.merchantId())) {
                            throw ApiException.badRequest("SCOPE_UNCHANGED", "所属商户与项目均未变化；同范围换岗请使用定岗入口");
                        }
                        UserPositionAssignment position = repository.findUserPositionAssignment(id).orElse(null);
                        String positionCode = trimToDefault(r.positionCode(), position == null ? "" : position.positionCode());
                        String staffLevelId = trimToDefault(r.staffLevelId(), position == null ? "" : position.staffLevelId());
                        if (positionCode.isEmpty() || staffLevelId.isEmpty()) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "范围变更须指定新范围的岗位与职级");
                        }
                        Instant now = Instant.now();
                        // M087-I7-02（PGM-1.10.1「超出新模板范围的增权保持未生效，审批通过后方可生效」）：旧范围下的非派生增权——直接装配角色、
                        // 单独模板授予、临时授权、员工单独授予（显式 GRANT）——随范围变更一并撤销；新范围内的增权须重新申请并审批通过后才生效。
                        // 员工单独的显式拒绝保留（M002-F02：与岗位高危挂起行分行存放，作废挂起只删挂起行）；模板套用整条撤销，模板内的禁止条目随之失效。
                        List<String> frozen = new ArrayList<>();
                        Set<String> frozenRoles = new HashSet<>();
                        Set<String> frozenApplications = new HashSet<>();
                        Set<String> frozenTemporary = new HashSet<>();
                        Set<String> frozenOverrides = new HashSet<>();
                        // M001-F12：有效权限读取放在一切撤销之前——它先把已到期的记录（含岗位派生套用）置过期并留痕，到期的不记成撤销。
                        for (AccessRepository.PermissionEffect effect : repository.findPermissionEffects(id, now)) {
                            if (effect.effect() != AccessRepository.Effect.GRANT) {
                                continue;
                            }
                            switch (effect.source()) {
                                case TEMPORARY_GRANT -> frozenTemporary.add(effect.sourceId());
                                case USER_OVERRIDE -> frozenOverrides.add(effect.permissionCode());
                                default -> { } // 直接角色与模板套用见下方按记录枚举
                            }
                        }
                        boolean revoked = position != null;
                        if (revoked) {
                            repository.replacePositionGrants(id, null, null, List.of(), actor.id(), now); // 旧派生立即失效
                        }
                        // M087-I5-03（PGM-1.10.1「旧岗位或旧范围派生权限立即失效」「显式拒绝优先级最高」）：旧范围岗位模板的高危挂起随旧派生作废——
                        // 删除其挂起行、挂起单置 rejected；新范围高危只能随新范围审批重新授出，旧单不得在新范围解除拒绝。
                        // M002-F02：只删挂起行，挂起前后写下的员工显式拒绝都保留。
                        List<String> cancelledHolds = new ArrayList<>();
                        for (AccessGrantRequestRecord held : repository.listPendingGrantRequests(id, REQUEST_POSITION_HIGH_RISK_RELEASE)) {
                            repository.deleteHighRiskHolds(id, held.permissionCodes());
                            repository.saveGrantRequest(withDecision(held, "rejected", actor.id(), now));
                            cancelledHolds.add(held.id());
                        }
                        // M001-F12：直接角色与模板套用按员工本身的记录枚举，不看是否产出权限码——空模板、空角色也一并撤销。
                        frozenRoles.addAll(repository.findUserRoleIds(id));
                        frozenApplications.addAll(repository.findActiveTemplateApplicationIds(id));
                        for (String roleId : frozenRoles) {
                            repository.unassignUserRole(id, roleId);
                            frozen.add("role:" + roleId);
                        }
                        if (!frozenApplications.isEmpty()) {
                            repository.revokeTemplateApplications(id, List.copyOf(frozenApplications), actor.id(), now);
                            frozenApplications.forEach(applicationId -> frozen.add("template_application:" + applicationId));
                        }
                        for (String grantId : frozenTemporary) {
                            repository.revokeTemporaryGrant(grantId, actor.id(), now);
                            frozen.add("temporary_grant:" + grantId);
                        }
                        if (!frozenOverrides.isEmpty()) {
                            repository.deleteUserPermissionOverrides(id, List.copyOf(frozenOverrides), AccessRepository.Effect.GRANT);
                            frozenOverrides.forEach(code -> frozen.add("override:" + code));
                        }
                        repository.updateUserScope(id, newOrganization, newMerchant, actor.id(), now);
                        PositionGrantContext context = resolvePositionGrant(new UserPositionAssignmentRequest(
                                r.idempotencyKey(), r.operatorNote(), id, positionCode, staffLevelId,
                                r.baseSalaryAmountCent(), r.salaryCurrency(), r.salaryDetail()), false);
                        GrantReviewPolicy policy = scopeChangeReviewPolicy(context.permissionCodes());
                        boolean moneyRelated = permissionDefinitions(context.permissionCodes()).stream().anyMatch(AccessService::isMoneyPermission);
                        String draftId = "agr_" + UUID.randomUUID();
                        SupervisorReviewDecisionRecord review = blockGrantRequest(policy,
                                moneyRelated ? "access.scope_change.money" : "access.scope_change", draftId, id, r.finalApproverId());
                        AccessGrantRequestRecord draft = new AccessGrantRequestRecord(
                                draftId, REQUEST_USER_SCOPE_CHANGE, id, context.role().id(), context.template().id(),
                                context.positionCode(), context.staffLevelId(), r.baseSalaryAmountCent(), context.salaryCurrency(),
                                r.salaryDetail(), context.permissionCodes(), null, null, false, "pending", review.reviewId(),
                                trimToDefault(r.reason(), r.operatorNote()), actor.id(), null, null, newOrganization, newMerchant, now, now);
                        AccessGrantRequestRecord saved = repository.saveGrantRequest(draft);
                        repository.saveGrantFingerprint(saved.id(), authorizationFingerprint(saved));
                        auditEventService.recordSuccess(
                                "access.user_scope.changed",
                                "access.role.assign",
                                "access_user",
                                id,
                                null,
                                r.idempotencyKey(),
                                Map.of("old_organization_id", nz(target.organizationId()), "old_merchant_id", nz(target.merchantId()),
                                        "new_organization_id", nz(newOrganization), "new_merchant_id", nz(newMerchant),
                                        "old_position_revoked", revoked, "grant_request_id", saved.id(), "review_id", nz(saved.reviewId()),
                                        "cancelled_high_risk_holds", String.join(",", cancelledHolds),
                                        "frozen_extra_grants", String.join(",", frozen))
                        );
                        return saved;
                    },
                    AccessGrantRequestRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "access.user_scope.change_failed",
                    "access.role.assign",
                    "access_user",
                    userId,
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    /** 新范围权限每次均须先审批：按模板权限最高风险取档位；模板无需审批权限时也以一级主管为下限。 */
    private GrantReviewPolicy scopeChangeReviewPolicy(List<String> permissionCodes) {
        List<PermissionDefinition> approvalRequired = approvalRequiredPermissions(permissionCodes);
        if (approvalRequired.isEmpty()) {
            return new GrantReviewPolicy("permission_grant_l1", "one_level_up", 1);
        }
        return grantReviewPolicy(approvalRequired.stream().map(PermissionDefinition::permissionCode).toList());
    }

    /** M087（PGM-1.7.1）：授权申请进主管链；起草人可指定主线最终审批人，未指定时按档位下限沿直接上级链生成。 */
    private SupervisorReviewDecisionRecord blockGrantRequest(GrantReviewPolicy policy, String actionType, String objectId,
                                                             String beneficiaryId, String finalApproverId) {
        if (isBlank(finalApproverId)) {
            return requireSupervisorReviewService().blockForAdmission(policy.policyCode(), actionType, "access_grant_request",
                    objectId, policy.approvalLevel(), policy.requiredLevel(), beneficiaryId);
        }
        return requireSupervisorReviewService().blockForAdmission(policy.policyCode(), actionType, "access_grant_request",
                objectId, policy.approvalLevel(), policy.requiredLevel(), beneficiaryId, finalApproverId.trim());
    }

    /** M087-F-06：岗位模板内的高危权限——挂起行（deny）+ 待审申请（position_high_risk_release），审批通过后删除挂起行（M002-F02）。 */
    private void holdHighRiskPositionPermissions(PositionGrantContext context, UserPositionAssignmentRequest request,
                                                 List<PermissionDefinition> highRisk, Instant now) {
        String userId = context.targetUser().id();
        // M006-R6-02（PGM-1.10.1「显式拒绝优先级最高」）：已有独立显式拒绝的码不写挂起行、不进释放单——独立拒绝不被挂起复用，
        // 作废 / 释放时也不会被删；该码在拒绝解除（须审批）前不会生效。
        // M002-F02：已挂起的码同样不重复写挂起行、不重复开释放单（模板重算不刷出重复释放单）。
        Set<String> preDenied = repository.findPermissionEffects(userId, now).stream()
                .filter(effect -> effect.effect() == AccessRepository.Effect.DENY
                        && (effect.source() == AccessRepository.Source.USER_OVERRIDE || effect.source() == AccessRepository.Source.POSITION_HOLD))
                .map(AccessRepository.PermissionEffect::permissionCode).collect(java.util.stream.Collectors.toSet());
        List<String> codes = highRisk.stream().map(PermissionDefinition::permissionCode).filter(code -> !preDenied.contains(code)).sorted().toList();
        if (codes.isEmpty()) {
            auditEventService.recordSuccess("access.user_position.high_risk_already_denied", "access.role.assign", "user_position_assignment",
                    userId, null, request.idempotencyKey(), Map.of("permission_codes", String.join(",", preDenied)));
            return;
        }
        repository.saveHighRiskHolds(userId, codes, "岗位模板内高危权限待审批（PGM-1.10.1）", context.actor().id()); // M002-F02：挂起行与员工显式拒绝分行
        GrantReviewPolicy policy = grantReviewPolicy(codes);
        boolean moneyRelated = highRisk.stream().anyMatch(AccessService::isMoneyPermission);
        String draftId = "agr_" + UUID.randomUUID();
        SupervisorReviewDecisionRecord review = blockGrantRequest(policy,
                moneyRelated ? "access.grant_request.money" : "access.grant_request", draftId, userId, null);
        AccessGrantRequestRecord release = new AccessGrantRequestRecord(
                draftId, REQUEST_POSITION_HIGH_RISK_RELEASE, userId, context.role().id(), context.template().id(),
                context.positionCode(), context.staffLevelId(), null, null, null, codes, null, null, false, "pending",
                review.reviewId(), trimToDefault(request.operatorNote(), "岗位模板内高危权限授出审批"), context.actor().id(),
                null, null, context.targetUser().organizationId(), context.targetUser().merchantId(), now, now);
        repository.saveGrantRequest(release);
        repository.saveGrantFingerprint(release.id(), authorizationFingerprint(release));
        auditEventService.recordSuccess(
                "access.user_position.high_risk_held",
                "access.role.assign",
                "user_position_assignment",
                userId,
                null,
                request.idempotencyKey(),
                Map.of("permission_codes", String.join(",", codes), "grant_request_id", release.id(), "review_id", nz(review.reviewId()))
        );
    }

    private void releaseHeldPositionPermissions(AccessGrantRequestRecord request, String idempotencyKey) {
        int removed = repository.deleteHighRiskHolds(request.targetUserId(), request.permissionCodes()); // M002-F02：只删挂起行，员工显式拒绝保留
        auditEventService.recordSuccess(
                "access.user_position.high_risk_released",
                "access.role.assign",
                "user_position_assignment",
                request.targetUserId(),
                null,
                idempotencyKey,
                Map.of("permission_codes", String.join(",", request.permissionCodes()), "removed_denies", removed)
        );
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    /**
     * P0-1 提权防护：发证/授权动作不得授予超出「授权人自身有效权限」的权限码。
     * USB 最高权限独立识别；日常管理员及其他主体所授权限码必须 ⊆ 自身有效授予集（denies 已扣减）。
     */
    private void requireActorCanGrant(List<String> permissionCodes) {
        EffectivePermissions self = authorizationService.effectivePermissions();
        if (self.wildcard()) {
            return;
        }
        java.util.Set<String> own = new java.util.HashSet<>(self.grants());
        for (String code : safeList(permissionCodes)) {
            if (code != null && !own.contains(code)) {
                throw ApiException.forbidden("GRANT_EXCEEDS_OWN_PERMISSIONS",
                        "不能授予自己不持有的权限：" + code);
            }
        }
    }

    /**
     * M006-I9-02（PGM-1.10.1「普通模板权限随保存立即生效」「既有高危不得因处于岗位模板内而免除授出前审批」）：岗位映射 / 定岗时
     * 授权人只须持有随保存立即生效的普通码；须审批的高危码由挂起单（position_high_risk_release）审批，不要求配置人先持有。
     * 授出审批关闭时全部码立即生效，仍全部校验。
     */
    private void requireActorCanGrantImmediate(List<String> permissionCodes) {
        Set<String> held = grantApprovalEnabled
                ? approvalRequiredPermissions(permissionCodes).stream().map(PermissionDefinition::permissionCode).collect(java.util.stream.Collectors.toSet())
                : Set.of();
        requireActorCanGrant(safeList(permissionCodes).stream().filter(code -> !held.contains(code)).toList());
    }

    public TemporaryGrant createTemporaryGrant(TemporaryGrantRequest request) {
        AccessActor actor = authorizationService.currentActor();
        try {
            return idempotencyService.execute(
                    "createTemporaryGrant",
                    request.idempotencyKey(),
                    request,
                    () -> createTemporaryGrantDirect(request, true),
                    TemporaryGrant.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "temporary_grant.create_failed",
                    "access.temporary_grant.create",
                    "temporary_permission_grant",
                    null,
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public TemporaryGrant revokeTemporaryGrant(String id, StatusChangeRequest request) {
        AccessActor actor = authorizationService.currentActor();
        try {
            return idempotencyService.execute(
                    "revokeTemporaryGrant:" + id,
                    request.idempotencyKey(),
                    request,
                    () -> {
                        TemporaryGrant grant = repository.findTemporaryGrantById(id)
                                .orElseThrow(() -> ApiException.notFound("TEMPORARY_GRANT_NOT_FOUND", "临时授权不存在"));
                        requireSameOrganization(actor, requireTargetUser(grant.targetUserId()));
                        TemporaryGrant revoked = repository.revokeTemporaryGrant(id, "access-api", Instant.now());
                        auditEventService.recordSuccess(
                                "permission_template.revoked",
                                "access.temporary_grant.revoke",
                                "temporary_permission_grant",
                                id,
                                null,
                                request.idempotencyKey(),
                                Map.of("reason", request.reason() == null ? "" : request.reason())
                        );
                        return revoked;
                    },
                    TemporaryGrant.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "temporary_grant.revoke_failed",
                    "access.temporary_grant.revoke",
                    "temporary_permission_grant",
                    id,
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    @Transactional
    public AccessGrantRequestRecord applyGrant(AccessGrantApplyRequest request) {
        AccessGrantApplyRequest r = requireRequest(request);
        try {
            return idempotencyService.execute(
                    "accessGrantApply:" + trimToDefault(r.requestType(), ""),
                    r.idempotencyKey(),
                    r,
                    () -> {
                        repository.lockGrantAuthorizationFacts();
                        AccessGrantRequestRecord draft = buildGrantRequest(r);
                        List<PermissionDefinition> approvalRequired = approvalRequiredPermissions(draft.permissionCodes());
                        // M007-F-04（PGM-1.10.1「员工可申请临时权限，按层级对应主管审批」）：临时权限申请不论日常/高危都走主管链；
                        // 日常权限按一级主管（permission_grant_l1）审批。角色/岗位申请仍只接受含需审批权限的申请。
                        boolean dailyTemporary = approvalRequired.isEmpty() && REQUEST_TEMPORARY_PERMISSION.equals(draft.requestType());
                        if (approvalRequired.isEmpty() && !dailyTemporary) {
                            throw ApiException.badRequest("GRANT_APPROVAL_NOT_REQUIRED",
                                    "该授权不含需审批的权限，请直接分配");
                        }
                        GrantReviewPolicy policy = dailyTemporary
                                ? new GrantReviewPolicy("permission_grant_l1", "one_level_up", 1)
                                : grantReviewPolicy(approvalRequired.stream()
                                        .map(PermissionDefinition::permissionCode)
                                        .toList());
                        // M007-A-02：被授权对象是直接受益人，不得审批也不得被转发。
                        // M007-I3-F02（PGM-1.7.1 涉钱审批 1 个工作日）：含资金/分红/退款/调账类权限的申请以涉钱动作名进链。
                        boolean moneyRelated = permissionDefinitions(draft.permissionCodes()).stream().anyMatch(AccessService::isMoneyPermission);
                        SupervisorReviewDecisionRecord review = blockGrantRequest(policy,
                                moneyRelated ? "access.grant_request.money" : "access.grant_request",
                                draft.id(), draft.targetUserId(), r.finalApproverId());
                        AccessGrantRequestRecord saved = repository.saveGrantRequest(withReview(draft, review.reviewId()));
                        repository.saveGrantFingerprint(saved.id(), authorizationFingerprint(saved));
                        auditEventService.recordSuccess(
                                "access.grant_request.applied",
                                "access.grant_request.apply",
                                "access_grant_request",
                                saved.id(),
                                null,
                                r.idempotencyKey(),
                                Map.of("request_type", saved.requestType(), "review_id", saved.reviewId())
                        );
                        return saved;
                    },
                    AccessGrantRequestRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "access.grant_request.apply_failed",
                    "access.grant_request.apply",
                    "access_grant_request",
                    null,
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    @Transactional
    public AccessGrantRequestRecord reviewGrant(String id, AccessGrantReviewRequest request) {
        AccessGrantReviewRequest r = requireRequest(request);
        try {
            return idempotencyService.execute(
                    "accessGrantReview:" + id,
                    r.idempotencyKey(),
                    r,
                    () -> {
                        repository.lockGrantAuthorizationFacts();
                        AccessGrantRequestRecord pending = repository.findGrantRequestById(requireText(id, "id"))
                                .filter(grant -> "pending".equals(grant.status()))
                                .orElseThrow(() -> ApiException.notFound("ACCESS_GRANT_REQUEST_NOT_FOUND",
                                        "授权申请不存在或已不在待审批状态：" + id));
                        AccessActor reviewer = authorizationService.currentActor();
                        if (reviewer.id().equals(pending.targetUserId())) {
                            throw ApiException.forbidden("BENEFICIARY_SELF_REVIEW_FORBIDDEN", "授权受益人不能审批自己的授权");
                        }
                        if (reviewer.id().equals(pending.requestedBy())) {
                            throw ApiException.forbidden("SUPERVISOR_SELF_REVIEW_FORBIDDEN", "申请人不能审批自己的授权");
                        }
                        String decision = normalizeDecision(r.decision());
                        if ("approve".equals(decision)) {
                            requireGrantSnapshotUnchanged(pending);
                        }
                        SupervisorReviewDecisionRecord review = requireSupervisorReviewService().approve(
                                pending.reviewId(),
                                new SupervisorReviewApprovalRequest(
                                        r.idempotencyKey(),
                                        r.operatorNote(),
                                        safeList(r.evidenceAttachmentIds()),
                                        null,
                                        "approve".equals(decision) ? "approved" : "rejected",
                                        r.comment()
                                )
                        );
                        if ("approve".equals(decision) && !"approved".equals(review.reviewStatus())) {
                            return pending; // M087：审批链尚有后续节点待审，本次只推进节点，授权不落地
                        }
                        Instant now = Instant.now();
                        AccessActor actor = authorizationService.currentActor();
                        AccessGrantRequestRecord reviewed;
                        if ("reject".equals(decision)) {
                            reviewed = repository.saveGrantRequest(withDecision(pending, "rejected", actor.id(), now));
                        } else {
                            applyApprovedGrant(pending, r.idempotencyKey());
                            reviewed = repository.saveGrantRequest(withDecision(pending, "applied", actor.id(), now));
                        }
                        auditEventService.recordSuccess(
                                "access.grant_request.reviewed",
                                "access.grant_request.review",
                                "access_grant_request",
                                reviewed.id(),
                                null,
                                r.idempotencyKey(),
                                Map.of("decision", review.decision(), "status", reviewed.status())
                        );
                        return reviewed;
                    },
                    AccessGrantRequestRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "access.grant_request.review_failed",
                    "access.grant_request.review",
                    "access_grant_request",
                    id,
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public PageResponse<AccessGrantRequestRecord> listGrantRequests(Integer windowDays, int pageNumber) {
        if (pageNumber < 1) throw ApiException.badRequest("INVALID_PAGE", "页码须大于零");
        AccessGrantPolicy policy = grantPolicy();
        int days = windowDays == null ? policy.windowDays().value() : policy.windowDays().checked(windowDays);
        AccessActor actor = authorizationService.currentActor();
        String org = authorizationService.isPlatformAdmin(actor) ? null : authorizationService.requireProjectScope(actor);
        String merchant = authorizationService.isPlatformAdmin(actor) ? null : trimToNull(actor.merchantId());
        PageResponse<AccessGrantRequestRecord> result = repository.pageGrantRequests(
                Instant.now().minus(Duration.ofDays(days)), org, merchant, pageNumber, 50);
        auditEventService.recordSuccess("access.grant_request.viewed", "access.grant_request.view",
                "access_grant_request", "all", null, null, Map.of("count", result.items().size(), "page", pageNumber));
        return result;
    }

    public PageResponse<AccessGrantRequestRecord> listGrantRequests(Integer windowDays) {
        AccessGrantPolicy policy = grantPolicy();
        int days = windowDays == null ? policy.windowDays().value() : policy.windowDays().checked(windowDays);
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        List<AccessGrantRequestRecord> requests = visibleGrantRequests(cutoff);
        auditEventService.recordSuccess(
                "access.grant_request.viewed",
                "access.grant_request.view",
                "access_grant_request",
                "all",
                null,
                null,
                Map.of("count", requests.size())
        );
        return page(requests);
    }

    public AccessGrantSummary grantSummary(Integer windowDays, Integer positionThreshold, Integer userThreshold) {
        AccessGrantPolicy policy = grantPolicy();
        if (positionThreshold != null || userThreshold != null
                || (windowDays != null && windowDays != policy.windowDays().value())) {
            throw ApiException.badRequest("QUERY_POLICY_OVERRIDE_FORBIDDEN", "统计时间窗和频繁阈值由后台授权政策配置，查询不能覆盖");
        }
        int days = policy.windowDays().value();
        int perPositionThreshold = policy.positionThreshold().value();
        int perUserThreshold = policy.userThreshold().value();
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        List<AccessGrantRequestRecord> temporaryRequests = visibleGrantRequests(cutoff).stream()
                .filter(request -> REQUEST_TEMPORARY_PERMISSION.equals(request.requestType()))
                .toList();

        Map<PositionSummaryKey, Long> byPosition = new HashMap<>();
        Map<String, Long> byUser = new HashMap<>();
        for (AccessGrantRequestRecord request : temporaryRequests) {
            String userId = trimToDefault(request.targetUserId(), "unknown");
            byUser.merge(userId, 1L, Long::sum);
            PositionSummaryKey base = positionSummaryKey(request);
            for (String permissionCode : safeList(request.permissionCodes())) {
                byPosition.merge(new PositionSummaryKey(
                        base.positionCode(),
                        base.staffLevelId(),
                        trimToDefault(permissionCode, "unknown")
                ), 1L, Long::sum);
            }
        }

        List<AccessGrantPositionSummary> positionRecommendations = byPosition.entrySet().stream()
                .filter(entry -> entry.getValue() >= perPositionThreshold)
                .sorted(Map.Entry.<PositionSummaryKey, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().positionCode())
                        .thenComparing(entry -> entry.getKey().staffLevelId())
                        .thenComparing(entry -> entry.getKey().permissionCode()))
                .map(entry -> new AccessGrantPositionSummary(
                        entry.getKey().positionCode(),
                        entry.getKey().staffLevelId(),
                        entry.getKey().permissionCode(),
                        entry.getValue(),
                        "频繁申请临时授权，建议评估配置固定权限"
                ))
                .toList();
        List<AccessGrantUserSummary> userAnomalies = byUser.entrySet().stream()
                .filter(entry -> entry.getValue() >= perUserThreshold)
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry::getKey))
                .map(entry -> new AccessGrantUserSummary(
                        entry.getKey(),
                        entry.getValue(),
                        "临时权限申请频繁，请核查异常情况"
                ))
                .toList();
        return new AccessGrantSummary(positionRecommendations, userAnomalies);
    }

    private List<AccessGrantRequestRecord> visibleGrantRequests(Instant cutoff) {
        AccessActor actor = authorizationService.currentActor();
        return repository.listGrantRequests(cutoff).stream()
                .filter(request -> canViewGrantRequest(actor, request))
                .toList();
    }

    private boolean canViewGrantRequest(AccessActor actor, AccessGrantRequestRecord request) {
        if (authorizationService.isPlatformAdmin(actor)) {
            return true;
        }
        if (!isBlank(actor.merchantId())) {
            return actor.merchantId().equals(request.merchantId());
        }
        // 2026-07-18 收尾（多综合体前置）：项目员工按本项目 scope 强制过滤——申请行带 project_id 时必须与
        // actor 项目一致（沿用 currentProjectScopeQuietly 谓词范式）；行无 project_id 时仍走目标账号同组织判定。
        String projectScope = authorizationService.currentProjectScopeQuietly().orElse(null);
        if (projectScope != null && !isBlank(request.projectId()) && !projectScope.equals(request.projectId())) {
            return false;
        }
        if (isBlank(actor.organizationId()) || isBlank(request.targetUserId())) {
            return false;
        }
        return repository.findUserById(request.targetUserId())
                .map(user -> actor.organizationId().equals(user.organizationId()))
                .orElse(false);
    }

    public PageResponse<AccessUser> listAccessUsers() {
        AccessActor actor = authorizationService.currentActor();
        List<AccessUser> users;
        if (authorizationService.isPlatformAdmin(actor)) {
            users = repository.listUsers();
        } else if (!isBlank(actor.merchantId())) {
            users = repository.listUsersByMerchant(actor.merchantId());
        } else {
            users = repository.listUsers(authorizationService.requireProjectScope(actor));
        }
        auditEventService.recordSuccess(
                "access.user.viewed",
                "access.user.view",
                "access_user",
                "all",
                null,
                null,
                Map.of("count", users.size())
        );
        return page(users);
    }

    public AccessUser createAccessUser(AccessUserRequest request) {
        AccessActor actor = authorizationService.currentActor();
        try {
            return idempotencyService.execute(
                    "createAccessUser",
                    request.idempotencyKey(),
                    request,
                    () -> {
                        String username = requireText(request.username(), "username");
                        String accountType = normalizeAccountType(trimToDefault(request.accountType(), "staff"));
                        if (!ALLOWED_ACCOUNT_TYPES.contains(accountType)) {
                            throw ApiException.badRequest("INVALID_ACCOUNT_TYPE",
                                    "账号类型不允许，请重新选择");
                        }
                        String organizationId = requireText(request.organizationId(), "organization_id");
                        if (!authorizationService.isPlatformAdmin(actor)
                                && !authorizationService.requireProjectScope(actor).equals(organizationId)) {
                            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                    "不能在其他组织下创建用户：" + organizationId);
                        }
                        String merchantId = trimToNull(request.merchantId());
                        // 与上方组织校验对称:平台管理员不受自身 merchant 归属限制(dev/local seed 带 seed-merchant)。
                        if (!authorizationService.isPlatformAdmin(actor) && !isBlank(actor.merchantId())) {
                            if (merchantId == null) {
                                merchantId = actor.merchantId();
                            } else if (!actor.merchantId().equals(merchantId)) {
                                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                        "不能在其他商户下创建用户：" + merchantId);
                            }
                        }
                        if (requiresMerchantScope(accountType) && merchantId == null) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "商户级员工账号必须指定商户");
                        }
                        if (!"outsourced_vendor".equals(accountType)) {
                            throw ApiException.conflict("STAFF_REGISTRATION_REQUIRED",
                                    "员工须先提交注册申请，由对应公司审批后生成账号和员工自助权限");
                        }
                        if (credentialProvisioner == null) {
                            throw ApiException.conflict("CREDENTIAL_PROVIDER_UNAVAILABLE", "登录凭证服务未配置，不能创建账号");
                        }
                        requireText(request.password(), "password");
                        String accountStatus = requireAccountStatus(request.status());
                        Instant now = Instant.now();
                        AccessUser user = new AccessUser(
                                "usr_" + UUID.randomUUID(),
                                username,
                                accountType,
                                organizationId,
                                merchantId,
                                accountStatus,
                                now,
                                null,
                                "manual_admin",
                                trimToNull(request.consentReference()),
                                safeList(request.evidenceAttachmentIds()),
                                trimToNull(request.operatorNote())
                        );
                        AccessUser saved = repository.createUser(user);
                        credentialProvisioner.provisionCredential(saved.id(), request.password(), saved.status(), now);
                        auditEventService.recordSuccess(
                                "access.user.changed",
                                "access.user.manage",
                                "access_user",
                                saved.id(),
                                null,
                                request.idempotencyKey(),
                                accessUserAuditMetadata(saved, request.password() != null && !request.password().isBlank())
                        );
                        return saved;
                    },
                    AccessUser.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.user.changed_failed", "access.user.manage", "access_user", null, null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private String normalizeAccountType(String accountType) {
        return accountType == null ? "staff" : accountType.toLowerCase(Locale.ROOT);
    }

    private boolean requiresMerchantScope(String accountType) {
        return Set.of("merchant", "merchant_admin", "sales").contains(accountType);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String requireAccountStatus(String status) {
        String actual = trimToDefault(status, "active");
        if (!Set.of("active", "invited", "disabled").contains(actual)) {
            throw ApiException.badRequest("INVALID_ACCOUNT_STATUS", "账号状态必须为启用、待激活或禁用");
        }
        return actual;
    }

    private Map<String, Object> accessUserAuditMetadata(AccessUser user, boolean credentialProvisioned) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("account_type", user.accountType());
        metadata.put("organization_id", user.organizationId());
        metadata.put("merchant_id", user.merchantId() == null ? "" : user.merchantId());
        metadata.put("created_via", user.createdVia());
        metadata.put("evidence_count", user.evidenceAttachmentIds().size());
        metadata.put("has_consent_reference", user.consentReference() != null && !user.consentReference().isBlank());
        metadata.put("credential_provisioned", credentialProvisioned);
        return metadata;
    }

    public PageResponse<StaffLevelRecord> listStaffLevels() {
        List<StaffLevelRecord> levels = repository.listStaffLevels();
        auditEventService.recordSuccess(
                "access.staff_level.viewed",
                "access.staff_level.view",
                "staff_level",
                "all",
                null,
                null,
                Map.of("count", levels.size())
        );
        return page(levels);
    }

    /**
     * 为员工分配职级与具体薪资（REQ-60/61）。职级必须存在；币种 P0 仅 CNY（D12）；
     * 金额整数分非负（D1）；非平台管理员不得跨组织分配；幂等 + 审计留痕。
     */
    @Transactional
    public StaffLevelAssignment assignStaffLevel(StaffLevelAssignmentRequest request) {
        AccessActor actor = authorizationService.currentActor();
        try {
            return idempotencyService.execute(
                    "assignStaffLevel:" + (request == null ? "" : request.userId()),
                    request.idempotencyKey(),
                    request,
                    () -> {
                        String userId = trimToDefault(request.userId(), "");
                        if (userId.isEmpty()) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "必须指定用户");
                        }
                        AccessUser targetUser = repository.findUserById(userId)
                                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在：" + userId));
                        requireSameOrganization(actor, targetUser);
                        if (repository.findStaffLevelById(request.staffLevelId()).isEmpty()) {
                            throw ApiException.badRequest("UNKNOWN_STAFF_LEVEL",
                                    "无法识别的员工职级：" + request.staffLevelId());
                        }
                        String currency = trimToDefault(request.salaryCurrency(), "CNY");
                        if (!"CNY".equals(currency)) {
                            throw ApiException.badRequest("UNSUPPORTED_CURRENCY",
                                    "薪资币种当前仅支持人民币，不支持所填币种");
                        }
                        if (request.baseSalaryAmountCent() != null && request.baseSalaryAmountCent() < 0) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "基本工资不能为负数");
                        }
                        // M006-G-02（PGM-1.10.1/1.10.2）：已定岗员工改职级 = 按新职级的岗位映射重算派生角色/模板，旧派生同步失效；
                        // 走与定岗相同的路径（含高危审批闸），无映射时由 resolvePositionGrant 明确拒绝，不静默保留旧模板。
                        UserPositionAssignment position = repository.findUserPositionAssignment(userId).orElse(null);
                        StaffLevelAssignment saved;
                        if (position != null && !request.staffLevelId().equals(position.staffLevelId())) {
                            saved = assignUserPositionDirect(new UserPositionAssignmentRequest(
                                    request.idempotencyKey(), request.operatorNote(), userId, position.positionCode(),
                                    request.staffLevelId(), request.baseSalaryAmountCent(), currency, request.salaryDetail()), true)
                                    .staffLevelAssignment();
                        } else {
                            saved = repository.saveStaffLevelAssignment(new StaffLevelAssignment(
                                    userId,
                                    request.staffLevelId(),
                                    request.baseSalaryAmountCent(),
                                    currency,
                                    request.salaryDetail(),
                                    targetUser.organizationId(),
                                    targetUser.merchantId(),
                                    "access-api",
                                    Instant.now()
                            ));
                        }
                        auditEventService.recordSuccess(
                                "access.staff_level.assigned",
                                "access.staff_level.manage",
                                "staff_level_assignment",
                                userId,
                                null,
                                request.idempotencyKey(),
                                Map.of("staff_level_id", request.staffLevelId())
                        );
                        return saved;
                    },
                    StaffLevelAssignment.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "access.staff_level.assign_failed",
                    "access.staff_level.manage",
                    "staff_level_assignment",
                    request == null ? null : request.userId(),
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private RoleAssignmentRecord assignUserRoleDirect(RoleAssignmentRequest request, boolean enforceApproval) {
        RoleGrantContext context = resolveRoleGrant(request, enforceApproval, enforceApproval); // M087/M002-F04：审批落地不按落地人自身权限与组织限制
        if (enforceApproval) {
            requireGrantApprovalIfNeeded(context.role().permissionCodes());
        }
        RoleAssignmentRecord saved = repository.assignUserRole(context.targetUser().id(), context.role().id());
        auditEventService.recordSuccess(
                "access.role.assigned",
                "access.role.assign",
                "user_role",
                context.targetUser().id() + ":" + context.role().id(),
                null,
                request.idempotencyKey(),
                Map.of("role_id", context.role().id())
        );
        return saved;
    }

    /**
     * M006-G-01（PGM-1.10.1「最终权限 = … + 员工单独授予 − 显式拒绝」「员工级完全自定义」）：员工级覆盖写路径。
     * deny 只减权、不设审批；grant 与直接装配同口径（高危须走授权申请，且不得超出授权人自身有效集）。幂等 + 审计留痕。
     */
    @Transactional
    public EffectivePermissions setUserPermissionOverrides(String userId, UserPermissionOverrideRequest request) {
        return changeUserPermissionOverrides(userId, request, true);
    }

    @Transactional
    public EffectivePermissions removeUserPermissionOverrides(String userId, UserPermissionOverrideRequest request) {
        return changeUserPermissionOverrides(userId, request, false);
    }

    private EffectivePermissions changeUserPermissionOverrides(String userId, UserPermissionOverrideRequest request, boolean set) {
        UserPermissionOverrideRequest r = requireRequest(request);
        String action = set ? "set" : "remove";
        try {
            return idempotencyService.execute(
                    "userPermissionOverride:" + action + ":" + trimToDefault(userId, ""),
                    r.idempotencyKey(),
                    r,
                    () -> {
                        AccessActor actor = authorizationService.currentActor();
                        if (trimToDefault(userId, "").isEmpty()) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "必须指定用户");
                        }
                        AccessUser targetUser = lockTargetUser(userId.trim());
                        requireSameOrganization(actor, targetUser);
                        AccessRepository.Effect effect = switch (trimToDefault(r.effect(), "").toLowerCase(Locale.ROOT)) {
                            case "grant" -> AccessRepository.Effect.GRANT;
                            case "deny" -> AccessRepository.Effect.DENY;
                            default -> throw ApiException.badRequest("VALIDATION_FAILED", "覆盖效果只能是授予或拒绝");
                        };
                        List<String> permissionCodes = safeList(r.permissionCodes()).stream().distinct().toList();
                        if (permissionCodes.isEmpty()) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写权限编码列表");
                        }
                        requireKnownPermissions(permissionCodes);
                        if (set && effect == AccessRepository.Effect.GRANT) {
                            requireGrantApprovalIfNeeded(permissionCodes);
                            requireActorCanGrant(permissionCodes);
                        }
                        if (!set && effect == AccessRepository.Effect.DENY) {
                            Set<String> granted = repository.findPermissionEffects(targetUser.id(), Instant.now()).stream()
                                    .filter(effectRow -> effectRow.effect() == AccessRepository.Effect.GRANT)
                                    .map(AccessRepository.PermissionEffect::permissionCode).collect(java.util.stream.Collectors.toSet());
                            if (grantApprovalEnabled) {
                                // M006-R6-02（PGM-1.10.1「显式拒绝优先级最高」「既有高危不得因处于岗位模板内而免除授出前审批」）：移除显式拒绝若使须审批的
                                // 权限从既有 GRANT 来源生效，等同一次授出，须经授出审批；岗位高危挂起行只能由挂起单（position_high_risk_release）审批释放。
                                // M002-F01：解除员工显式拒绝走授权申请 user_permission_deny_removal，获批后只删员工显式拒绝行。
                                List<String> exposed = approvalRequiredPermissions(permissionCodes).stream()
                                        .map(PermissionDefinition::permissionCode).filter(granted::contains).sorted().toList();
                                if (!exposed.isEmpty()) {
                                    throw ApiException.forbidden("GRANT_REQUIRES_APPROVAL",
                                            "移除显式拒绝将使以下须审批的权限生效，须经授出审批（员工显式拒绝请提交『解除员工显式拒绝』授权申请；"
                                                    + "岗位高危挂起须经挂起单审批释放）：" + exposed);
                                }
                            }
                            // M002-F05（P0-1）：解除拒绝会让既有授予来源的码生效，等同直接授予，不得超出操作人自身有效集；须审批码由上方审批闸处理。
                            AccessActor subject = new AccessActor(targetUser.id(), targetUser.username(), targetUser.accountType(),
                                    targetUser.organizationId(), targetUser.merchantId(), targetUser.status());
                            if (authorizationService.isMasterAuthority(subject)) {
                                // UNRULED-C（操作员 2026-09-25「不走审批，保持现状」）：最高权限目标不走审批闸，被解除的码全部生效，操作人须本人持有。
                                requireActorCanGrant(permissionCodes);
                            } else {
                                boolean wildcard = authorizationService.isPlatformAdmin(subject); // 平台管理员日常通配
                                requireActorCanGrantImmediate(permissionCodes.stream()
                                        .filter(code -> wildcard || granted.contains(code)).toList());
                            }
                        }
                        if (set) {
                            repository.saveUserPermissionOverrides(targetUser.id(), permissionCodes, effect,
                                    trimToDefault(r.reason(), r.operatorNote()), actor.id());
                        } else if (effect == AccessRepository.Effect.DENY) {
                            // M002-F05：直接解除只删员工显式拒绝；岗位高危挂起只经挂起单释放或范围变更作废。
                            repository.deleteExplicitDenies(targetUser.id(), permissionCodes);
                        } else {
                            repository.deleteUserPermissionOverrides(targetUser.id(), permissionCodes, effect);
                        }
                        auditEventService.recordSuccess(
                                "access.user_permission_override." + action,
                                "access.role.assign",
                                "user_permission_override",
                                targetUser.id(),
                                null,
                                r.idempotencyKey(),
                                Map.of(
                                        "effect", effect.name().toLowerCase(Locale.ROOT),
                                        "permission_codes", String.join(",", permissionCodes),
                                        "reason", trimToDefault(r.reason(), "")
                                )
                        );
                        return authorizationService.effectivePermissionsForUser(targetUser.id());
                    },
                    EffectivePermissions.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "access.user_permission_override." + action + "_failed",
                    "access.role.assign",
                    "user_permission_override",
                    userId,
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private UserPositionAssignmentResult assignUserPositionDirect(
            UserPositionAssignmentRequest request,
            boolean enforceApproval
    ) {
        PositionGrantContext context = resolvePositionGrant(request, enforceApproval); // M087：审批落地不按落地人自身权限限制
        requireBoundedTemporaryTemplate(context.template().templateType(), context.template().validUntil(), context.template().singleUse());
        // M087-F-06（PGM-1.10.1「同一商户、同一项目范围内的普通模板权限随保存立即生效」「既有高危不得因处于岗位模板内而免除授出前审批」）：
        // 岗位模板混有高危权限时不再整单拒绝——普通部分随保存生效，高危部分先写挂起行（M002-F02）、进审批链，审批通过后解除。
        List<PermissionDefinition> highRisk = enforceApproval && grantApprovalEnabled
                ? approvalRequiredPermissions(context.permissionCodes()) : List.of();
        Instant now = Instant.now();
        StaffLevelAssignment staffLevelAssignment = repository.saveStaffLevelAssignment(new StaffLevelAssignment(
                context.targetUser().id(),
                context.staffLevelId(),
                request.baseSalaryAmountCent(),
                context.salaryCurrency(),
                request.salaryDetail(),
                context.targetUser().organizationId(),
                context.targetUser().merchantId(),
                context.actor().id(),
                now
        ));
        UserPositionAssignment assignment = repository.saveUserPositionAssignment(new UserPositionAssignment(
                context.targetUser().id(),
                context.positionCode(),
                context.staffLevelId(),
                context.targetUser().organizationId(),
                context.targetUser().merchantId(),
                context.actor().id(),
                now
        ));
        List<PermissionTemplateApplication> applications = repository.applyTemplate(
                context.template().id(),
                List.of(context.targetUser().id()),
                context.template().validFrom(),
                context.template().validUntil(),
                context.template().singleUse(),
                context.actor().id()
        );
        repository.replacePositionGrants(context.targetUser().id(), context.role().id(), context.mappingId(),
                applications.stream().map(PermissionTemplateApplication::id).toList(), context.actor().id(), now);
        auditEventService.recordSuccess(
                "access.user_position.assigned",
                "access.role.assign",
                "user_position_assignment",
                context.targetUser().id(),
                null,
                request.idempotencyKey(),
                Map.of(
                        "position_code", context.positionCode(),
                        "staff_level_id", context.staffLevelId(),
                        "role_id", context.role().id(),
                        "template_id", context.template().id()
                )
        );
        if (!highRisk.isEmpty()) {
            holdHighRiskPositionPermissions(context, request, highRisk, now);
        }
        return new UserPositionAssignmentResult(
                assignment,
                staffLevelAssignment,
                context.role().id(),
                context.template().id(),
                applications.size()
        );
    }

    private TemporaryGrant createTemporaryGrantDirect(TemporaryGrantRequest request, boolean enforceApproval) {
        AccessActor actor = authorizationService.currentActor();
        requireKnownPermissions(request.permissionCodes());
        if (request.validUntil() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写授权失效时间");
        }
        AccessUser target = lockTargetUser(requireText(request.targetUserId(), "target_user_id"));
        if (enforceApproval) { // M002-F04：审批落地不按终审人组织判定（与角色、员工单独授予落地同口径）
            requireSameOrganization(actor, target);
        }
        if (safeList(request.permissionCodes()).isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写权限编码列表");
        }
        Instant validFrom = request.validFrom() == null ? Instant.now() : request.validFrom();
        if (!validFrom.isBefore(request.validUntil())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "授权失效时间必须晚于生效时间");
        }
        // P0-1：临时授权不得授予超出授权人自身有效权限的权限码（提权防护）。
        if (enforceApproval) { // M087：审批落地不按落地人自身权限限制
            requireActorCanGrant(request.permissionCodes());
        }
        if (enforceApproval) {
            requireGrantApprovalIfNeeded(request.permissionCodes());
        }
        TemporaryGrant grant = new TemporaryGrant(
                UUID.randomUUID().toString(),
                request.targetUserId(),
                safeList(request.permissionCodes()),
                validFrom,
                request.validUntil(),
                Boolean.TRUE.equals(request.singleUse()),
                false,
                "active",
                request.reason(),
                "access-api",
                Instant.now(),
                null,
                null
        );
        TemporaryGrant saved = repository.createTemporaryGrant(grant);
        auditEventService.recordSuccess(
                "permission_template.applied",
                "access.temporary_grant.create",
                "temporary_permission_grant",
                saved.id(),
                null,
                request.idempotencyKey(),
                Map.of("permission_count", saved.permissionCodes().size())
        );
        return saved;
    }

    private AccessGrantRequestRecord buildGrantRequest(AccessGrantApplyRequest request) {
        String type = normalizeGrantRequestType(request.requestType());
        return switch (type) {
            case REQUEST_USER_ROLE -> buildUserRoleGrantRequest(request);
            case REQUEST_USER_POSITION -> buildUserPositionGrantRequest(request);
            case REQUEST_TEMPORARY_PERMISSION -> buildTemporaryPermissionGrantRequest(request);
            case REQUEST_USER_PERMISSION_GRANT, REQUEST_USER_PERMISSION_DENY_REMOVAL -> buildUserPermissionOverrideRequest(request, type);
            default -> throw ApiException.badRequest("VALIDATION_FAILED", "不支持的申请类型：" + request.requestType());
        };
    }

    /**
     * M002-F01（PGM「支持员工级完全自定义配置」「显式拒绝优先级最高」「授出前须先审批才生效」）：员工单独授予 / 解除员工显式拒绝的授权申请。
     * 只收须审批权限码（日常码仍走员工覆盖直接操作，受授权人自身有效集校验）；建单校验申请人与目标同组织，授权依据为主管链与授权事实指纹。
     */
    private AccessGrantRequestRecord buildUserPermissionOverrideRequest(AccessGrantApplyRequest request, String type) {
        AccessActor actor = authorizationService.currentActor();
        AccessUser target = requireTargetUser(requireText(request.targetUserId(), "target_user_id"));
        requireSameOrganization(actor, target);
        List<String> codes = safeList(request.permissionCodes()).stream().distinct().sorted().toList();
        if (codes.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写权限编码列表");
        }
        requireKnownPermissions(codes);
        Set<String> needApproval = approvalRequiredPermissions(codes).stream()
                .map(PermissionDefinition::permissionCode).collect(java.util.stream.Collectors.toSet());
        List<String> daily = codes.stream().filter(code -> !needApproval.contains(code)).toList();
        if (!daily.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "以下为日常权限，请在员工覆盖中直接操作：" + daily);
        }
        if (REQUEST_USER_PERMISSION_DENY_REMOVAL.equals(type)) {
            // 解除单只能针对存在的员工显式拒绝；岗位高危挂起行不在此列（M002-F02）。
            Set<String> explicit = repository.findPermissionEffects(target.id(), Instant.now()).stream()
                    .filter(effect -> effect.effect() == AccessRepository.Effect.DENY && effect.source() == AccessRepository.Source.USER_OVERRIDE)
                    .map(AccessRepository.PermissionEffect::permissionCode).collect(java.util.stream.Collectors.toSet());
            List<String> missing = codes.stream().filter(code -> !explicit.contains(code)).toList();
            if (!missing.isEmpty()) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "以下权限没有可解除的员工显式拒绝（岗位高危挂起须经挂起单审批释放）：" + missing);
            }
        }
        Instant now = Instant.now();
        return new AccessGrantRequestRecord("agr_" + UUID.randomUUID(), type, target.id(), null, null, null, null, null, null, null,
                codes, null, null, false, "pending", null, trimToDefault(request.reason(), request.operatorNote()), actor.id(),
                null, null, null, target.merchantId(), now, now);
    }

    private AccessGrantRequestRecord buildUserRoleGrantRequest(AccessGrantApplyRequest request) {
        RoleGrantContext context = resolveRoleGrant(new RoleAssignmentRequest(
                request.idempotencyKey(),
                request.operatorNote(),
                request.targetUserId(),
                request.roleId()
        ), true, false);
        Instant now = Instant.now();
        return new AccessGrantRequestRecord(
                "agr_" + UUID.randomUUID(),
                REQUEST_USER_ROLE,
                context.targetUser().id(),
                context.role().id(),
                null,
                null,
                null,
                null,
                null,
                null,
                safeList(context.role().permissionCodes()),
                null,
                null,
                false,
                "pending",
                null,
                trimToDefault(request.reason(), request.operatorNote()),
                context.actor().id(),
                null,
                null,
                null,
                context.targetUser().merchantId(),
                now,
                now
        );
    }

    private AccessGrantRequestRecord buildUserPositionGrantRequest(AccessGrantApplyRequest request) {
        PositionGrantContext context = resolvePositionGrant(new UserPositionAssignmentRequest(
                request.idempotencyKey(),
                request.operatorNote(),
                request.targetUserId(),
                request.positionCode(),
                request.staffLevelId(),
                request.baseSalaryAmountCent(),
                request.salaryCurrency(),
                request.salaryDetail()
        ), false);
        Instant now = Instant.now();
        return new AccessGrantRequestRecord(
                "agr_" + UUID.randomUUID(),
                REQUEST_USER_POSITION,
                context.targetUser().id(),
                context.role().id(),
                context.template().id(),
                context.positionCode(),
                context.staffLevelId(),
                request.baseSalaryAmountCent(),
                context.salaryCurrency(),
                request.salaryDetail(),
                context.permissionCodes(),
                null,
                null,
                false,
                "pending",
                null,
                trimToDefault(request.reason(), request.operatorNote()),
                context.actor().id(),
                null,
                null,
                null,
                context.targetUser().merchantId(),
                now,
                now
        );
    }

    private AccessGrantRequestRecord buildTemporaryPermissionGrantRequest(AccessGrantApplyRequest request) {
        AccessActor actor = authorizationService.currentActor();
        AccessUser targetUser = requireTargetUser(requireText(request.targetUserId(), "target_user_id"));
        requireSameOrganization(actor, targetUser);
        List<String> permissionCodes = safeList(request.permissionCodes());
        if (permissionCodes.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写权限编码列表");
        }
        requireKnownPermissions(permissionCodes);
        Instant expiresAt = request.expiresAt();
        if (expiresAt == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写临时权限到期时间");
        }
        if (request.validFrom() != null && !expiresAt.isAfter(request.validFrom())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "授权结束时间必须晚于开始时间");
        }
        UserPositionAssignment position = repository.findUserPositionAssignment(targetUser.id()).orElse(null);
        String staffLevelId = position == null
                ? repository.findStaffLevelAssignment(targetUser.id()).map(StaffLevelAssignment::staffLevelId).orElse(null)
                : position.staffLevelId();
        Instant now = Instant.now();
        return new AccessGrantRequestRecord(
                "agr_" + UUID.randomUUID(),
                REQUEST_TEMPORARY_PERMISSION,
                targetUser.id(),
                null,
                null,
                position == null ? null : position.positionCode(),
                staffLevelId,
                null,
                null,
                null,
                permissionCodes,
                request.validFrom(),
                expiresAt,
                Boolean.TRUE.equals(request.singleUse()),
                "pending",
                null,
                trimToDefault(request.reason(), request.operatorNote()),
                actor.id(),
                null,
                null,
                null,
                targetUser.merchantId(),
                now,
                now
        );
    }

    private void requireGrantSnapshotUnchanged(AccessGrantRequestRecord request) {
        if (!repository.findGrantFingerprint(request.id())
                .filter(saved -> saved.equals(authorizationFingerprint(request))).isPresent()) {
            throw ApiException.conflict("GRANT_SNAPSHOT_CHANGED", "授权内容或归属已变化，请重新申请");
        }
    }

    private String authorizationFingerprint(AccessGrantRequestRecord request) {
        AccessUser target = requireTargetUser(request.targetUserId());
        List<Object> facts = new ArrayList<>();
        facts.add(java.util.Arrays.asList(target.id(), target.accountType(), target.organizationId(),
                target.merchantId(), target.status()));
        facts.add(java.util.Arrays.asList(request.requestType(), request.targetRoleId(), request.templateId(),
                request.positionCode(), request.staffLevelId(), request.baseSalaryAmountCent(), request.salaryCurrency(),
                request.salaryDetail(), request.permissionCodes().stream().sorted().toList(),
                String.valueOf(request.validFrom()), String.valueOf(request.expiresAt()), request.singleUse()));
        if (request.targetRoleId() != null) {
            RoleRecord role = requireRole(request.targetRoleId());
            facts.add(java.util.Arrays.asList(role.id(), role.organizationId(), role.status(),
                    role.dataScope(), role.permissionCodes().stream().sorted().toList()));
        }
        if (request.templateId() != null) {
            PermissionTemplate template = repository.findTemplateById(request.templateId())
                    .orElseThrow(() -> ApiException.conflict("GRANT_SNAPSHOT_CHANGED", "权限模板已变化，请重新申请"));
            facts.add(java.util.Arrays.asList(template.id(), template.ownerScopeType(), template.ownerScopeId(),
                    template.status(), template.singleUse(), String.valueOf(template.validFrom()),
                    String.valueOf(template.validUntil())));
            facts.add(repository.findTemplateItems(template.id()).stream()
                    .sorted(Comparator.comparing(PermissionTemplateItem::permissionCode)
                            .thenComparing(item -> item.effect().name()))
                    .map(item -> java.util.Arrays.asList(item.permissionCode(), item.effect().name(),
                            item.dataScope(), item.sensitive())).toList());
        }
        // M087-I2-01：范围变更申请与定岗申请一样钉住新范围的岗位映射（映射 id/角色/模板/状态），待审期间映射改动即 GRANT_SNAPSHOT_CHANGED。
        if (REQUEST_USER_POSITION.equals(request.requestType()) || REQUEST_USER_SCOPE_CHANGE.equals(request.requestType())) {
            PositionRoleTemplate mapping = repository.findPositionRoleTemplate(
                    target.organizationId(), request.positionCode(), request.staffLevelId())
                    .orElseThrow(() -> ApiException.conflict("GRANT_SNAPSHOT_CHANGED", "岗位配置已变化，请重新申请"));
            facts.add(java.util.Arrays.asList(mapping.id(), mapping.organizationId(), mapping.roleId(),
                    mapping.templateId(), mapping.status()));
        }
        if (REQUEST_USER_PERMISSION_DENY_REMOVAL.equals(request.requestType())) {
            // M002-F06（PGM「显式拒绝优先级最高」）：钉住所解除的员工显式拒绝行；待审期间被重写或删除即 GRANT_SNAPSHOT_CHANGED
            facts.add(repository.findExplicitDenies(target.id(), request.permissionCodes()).stream()
                    .map(row -> java.util.Arrays.asList(row.permissionCode(), row.reason(), row.createdBy(),
                            String.valueOf(row.createdAt())))
                    .toList());
        }
        facts.add(permissionDefinitions(request.permissionCodes()).stream()
                .sorted(Comparator.comparing(PermissionDefinition::permissionCode))
                .map(p -> java.util.Arrays.asList(p.permissionCode(), p.riskLevel(), p.requiresApproval(), p.status())).toList());
        try {
            byte[] json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(facts);
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json));
        } catch (java.security.NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("无法生成授权事实快照", exception);
        }
    }

    private void applyApprovedGrant(AccessGrantRequestRecord request, String idempotencyKey) {
        switch (request.requestType()) {
            case REQUEST_USER_ROLE -> assignUserRoleDirect(new RoleAssignmentRequest(
                    idempotencyKey,
                    request.reason(),
                    request.targetUserId(),
                    request.targetRoleId()
            ), false);
            case REQUEST_USER_POSITION -> assignUserPositionDirect(new UserPositionAssignmentRequest(
                    idempotencyKey,
                    request.reason(),
                    request.targetUserId(),
                    request.positionCode(),
                    request.staffLevelId(),
                    request.baseSalaryAmountCent(),
                    request.salaryCurrency(),
                    request.salaryDetail()
            ), false);
            case REQUEST_USER_SCOPE_CHANGE -> assignUserPositionDirect(new UserPositionAssignmentRequest(
                    idempotencyKey,
                    request.reason(),
                    request.targetUserId(),
                    request.positionCode(),
                    request.staffLevelId(),
                    request.baseSalaryAmountCent(),
                    request.salaryCurrency(),
                    request.salaryDetail()
            ), false);
            case REQUEST_POSITION_HIGH_RISK_RELEASE -> releaseHeldPositionPermissions(request, idempotencyKey);
            case REQUEST_TEMPORARY_PERMISSION -> createTemporaryGrantDirect(new TemporaryGrantRequest(
                    idempotencyKey,
                    request.reason(),
                    List.of(),
                    request.targetUserId(),
                    request.permissionCodes(),
                    request.validFrom(),
                    request.expiresAt(),
                    request.singleUse(),
                    request.reason()
            ), false);
            case REQUEST_USER_PERMISSION_GRANT, REQUEST_USER_PERMISSION_DENY_REMOVAL -> landUserPermissionOverride(request, idempotencyKey);
            default -> throw ApiException.badRequest("VALIDATION_FAILED",
                    "不支持的申请类型：" + request.requestType());
        }
    }

    /**
     * M002-F01：员工单独授予 / 解除员工显式拒绝获批落地。批准本身即授出审批，不再调 requireGrantApprovalIfNeeded；
     * 锁目标员工行，以申请时按申请人校验并钉入授权事实指纹的目标范围与完整审批链为准，不按终审人所属组织判定（M002-F04，与岗位、范围变更、
     * 角色、临时授权落地同口径）；解除单另在员工行锁下复核所钉的显式拒绝行（M002-F06）。同码已有显式拒绝时授予行照写，由有效权限扣减使其不生效。
     */
    private void landUserPermissionOverride(AccessGrantRequestRecord request, String idempotencyKey) {
        AccessActor approver = authorizationService.currentActor();
        AccessUser target = lockTargetUser(request.targetUserId());
        requireGrantSnapshotUnchanged(request); // M002-F06：审批时的比对在员工行锁之前；显式拒绝各写入口都先锁员工行，此处在锁下复核
        boolean grant = REQUEST_USER_PERMISSION_GRANT.equals(request.requestType());
        int affected = request.permissionCodes().size();
        if (grant) {
            repository.saveUserPermissionOverrides(target.id(), request.permissionCodes(), AccessRepository.Effect.GRANT,
                    trimToDefault(request.reason(), "授权申请获批"), approver.id());
        } else {
            affected = repository.deleteExplicitDenies(target.id(), request.permissionCodes()); // M002-F02：只删员工显式拒绝，挂起行不动
        }
        auditEventService.recordSuccess(
                "access.user_permission_override." + (grant ? "set" : "remove"),
                "access.role.assign",
                "user_permission_override",
                target.id(),
                null,
                idempotencyKey,
                Map.of("effect", grant ? "grant" : "deny", "permission_codes", String.join(",", request.permissionCodes()),
                        "reason", nz(request.reason()), "grant_request_id", request.id(), "affected_rows", affected)
        );
    }

    private RoleGrantContext resolveRoleGrant(RoleAssignmentRequest request, boolean requireActorScope, boolean requireGrantorCanGrant) {
        AccessActor actor = authorizationService.currentActor();
        AccessUser targetUser = lockTargetUser(requireText(request.userId(), "user_id"));
        RoleRecord role = requireRole(requireText(request.roleId(), "role_id"));
        if (requireActorScope) {
            // M002-F04（PGM 审批链「逐级审批」「转发不限层级」）：直接写与建单按操作人范围；审批落地以指纹钉住的目标事实与完整审批链为准
            requireSameOrganization(actor, targetUser);
        }
        requireRoleAssignableToTarget(role, targetUser);
        if (requireGrantorCanGrant) {
            requireActorCanGrant(role.permissionCodes());
        }
        return new RoleGrantContext(actor, targetUser, role);
    }

    private PositionGrantContext resolvePositionGrant(
            UserPositionAssignmentRequest request,
            boolean requireGrantorCanGrant
    ) {
        AccessActor actor = authorizationService.currentActor();
        String userId = requireText(request.userId(), "user_id");
        String staffLevelId = requireText(request.staffLevelId(), "staff_level_id");
        String positionCode = requireText(request.positionCode(), "position_code");
        AccessUser targetUser = lockTargetUser(userId);
        if (requireGrantorCanGrant) {
            // M087-I5-02（PGM-1.10.1 跨项目变更审批通过后方可生效）：审批落地与范围变更发起不按当前操作人的组织闸——
            // 授权来自审批链与申请指纹（目标范围已钉住）；直接定岗（enforceApproval）仍受同组织闸。
            requireSameOrganization(actor, targetUser);
        }
        if (repository.findStaffLevelById(staffLevelId).isEmpty()) {
            throw ApiException.badRequest("UNKNOWN_STAFF_LEVEL",
                    "无法识别的员工职级：" + staffLevelId);
        }
        PositionRoleTemplate mapping = repository.findPositionRoleTemplate(
                        targetUser.organizationId(), positionCode, staffLevelId)
                .orElseThrow(() -> ApiException.notFound("POSITION_ROLE_TEMPLATE_NOT_FOUND",
                        "未找到岗位角色模板：" + positionCode + "/" + staffLevelId));
        RoleRecord role = requireRole(mapping.roleId());
        PermissionTemplate template = repository.findTemplateById(mapping.templateId())
                .orElseThrow(() -> ApiException.notFound("PERMISSION_TEMPLATE_NOT_FOUND",
                        "权限模板不存在：" + mapping.templateId()));
        requireRoleAssignableToTarget(role, targetUser);
        requireTemplateAssignableToTarget(template, targetUser);
        List<String> templatePermissionCodes = repository.findTemplateItems(template.id()).stream()
                .map(PermissionTemplateItem::permissionCode)
                .toList();
        if (requireGrantorCanGrant) {
            requireActorCanGrantImmediate(role.permissionCodes());
            requireActorCanGrantImmediate(templatePermissionCodes);
        }
        String currency = trimToDefault(request.salaryCurrency(), "CNY");
        if (!"CNY".equals(currency)) {
            throw ApiException.badRequest("UNSUPPORTED_CURRENCY",
                    "薪资币种当前仅支持人民币，不支持所填币种");
        }
        if (request.baseSalaryAmountCent() != null && request.baseSalaryAmountCent() < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "基本工资不能为负数");
        }
        return new PositionGrantContext(
                actor,
                targetUser,
                positionCode,
                staffLevelId,
                role,
                template,
                mapping.id(),
                currency,
                grantPermissionCodes(role.permissionCodes(), templatePermissionCodes)
        );
    }

    private void requireGrantApprovalIfNeeded(List<String> permissionCodes) {
        if (!grantApprovalEnabled) {
            return;
        }
        List<PermissionDefinition> blocked = approvalRequiredPermissions(permissionCodes);
        if (!blocked.isEmpty()) {
            throw ApiException.forbidden("GRANT_REQUIRES_APPROVAL",
                    "以下权限的授予需要审批：" + blocked.stream() .map(PermissionDefinition::permissionCode) .sorted() .toList());
        }
    }

    private List<PermissionDefinition> approvalRequiredPermissions(List<String> permissionCodes) {
        Set<String> codes = new HashSet<>(safeList(permissionCodes));
        if (codes.isEmpty()) {
            return List.of();
        }
        return repository.listPermissions().stream()
                .filter(permission -> codes.contains(permission.permissionCode()))
                .filter(PermissionDefinition::requiresApproval)
                .toList();
    }

    private GrantReviewPolicy grantReviewPolicy(List<String> permissionCodes) {
        AccessGrantPolicy policy = grantPolicy();
        AccessGrantPolicy.ReviewRule rule = permissionDefinitions(permissionCodes).stream()
                .map(permission -> {
                    AccessGrantPolicy.ReviewRule configured = permission.riskLevel() == null ? null
                            : policy.riskReviews().get(permission.riskLevel().toLowerCase(Locale.ROOT));
                    if (configured == null) {
                        throw ApiException.badRequest("INVALID_GRANT_POLICY", "权限风险等级未配置审批策略：" + permission.permissionCode());
                    }
                    return configured;
                })
                .max(Comparator.comparingInt(AccessGrantPolicy.ReviewRule::requiredLevel)
                        .thenComparingInt(value -> switch (value.approvalLevel()) {
                            case "top_authority" -> 3; case "two_levels_up" -> 2; default -> 1;
                        })).orElseThrow(() -> ApiException.badRequest("INVALID_GRANT_POLICY", "权限风险等级未配置审批策略"));
        return new GrantReviewPolicy("permission_grant_l" + rule.requiredLevel(), rule.approvalLevel(), rule.requiredLevel());
    }

    public AccessGrantPolicy grantPolicy() {
        String json = repository.findGrantPolicy()
                .orElseThrow(() -> ApiException.conflict("GRANT_POLICY_NOT_CONFIGURED", "后台授权政策未配置"));
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, AccessGrantPolicy.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw ApiException.conflict("INVALID_GRANT_POLICY", "后台授权政策格式或上下限无效");
        }
    }

    @Transactional
    public AccessGrantPolicy updateGrantPolicy(AccessGrantPolicy.Update request) {
        // M031-003（GEN-011）：授权政策保存失败也留痕。
        try {
            return updateGrantPolicyAudited(request);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.grant_policy.change_failed", "admission.policy.manage",
                    "supervisor_review_policy", "access_grant", null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private AccessGrantPolicy updateGrantPolicyAudited(AccessGrantPolicy.Update request) {
        if (request == null || request.policy() == null) throw ApiException.badRequest("VALIDATION_FAILED", "授权政策不能为空");
        requireText(request.operatorNote(), "operator_note");
        return idempotencyService.execute("updateAccessGrantPolicy", request.idempotencyKey(), request, () -> {
            try {
                repository.saveGrantPolicy(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.policy()));
            } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
                throw new IllegalStateException("授权政策无法保存", exception);
            }
            auditEventService.recordSuccess("access.grant_policy.changed", "admission.policy.manage",
                    "supervisor_review_policy", "access_grant", null, request.idempotencyKey(),
                    Map.of("reason", request.operatorNote()));
            return request.policy();
        }, AccessGrantPolicy.class);
    }

    private List<PermissionDefinition> permissionDefinitions(Collection<String> permissionCodes) {
        Set<String> codes = new HashSet<>(permissionCodes == null ? List.of() : permissionCodes);
        if (codes.isEmpty()) {
            return List.of();
        }
        return repository.listPermissions().stream()
                .filter(permission -> codes.contains(permission.permissionCode()))
                .toList();
    }

    private List<String> grantPermissionCodes(List<String> rolePermissionCodes, List<String> templatePermissionCodes) {
        List<String> result = new ArrayList<>();
        result.addAll(safeList(rolePermissionCodes));
        result.addAll(safeList(templatePermissionCodes));
        return result.stream().filter(code -> code != null && !code.isBlank()).distinct().toList();
    }

    private AccessGrantRequestRecord withReview(AccessGrantRequestRecord request, String reviewId) {
        return new AccessGrantRequestRecord(
                request.id(),
                request.requestType(),
                request.targetUserId(),
                request.targetRoleId(),
                request.templateId(),
                request.positionCode(),
                request.staffLevelId(),
                request.baseSalaryAmountCent(),
                request.salaryCurrency(),
                request.salaryDetail(),
                request.permissionCodes(),
                request.validFrom(),
                request.expiresAt(),
                request.singleUse(),
                request.status(),
                reviewId,
                request.reason(),
                request.requestedBy(),
                request.decidedBy(),
                request.decidedAt(),
                request.projectId(),
                request.merchantId(),
                request.createdAt(),
                Instant.now()
        );
    }

    private AccessGrantRequestRecord withDecision(
            AccessGrantRequestRecord request,
            String status,
            String decidedBy,
            Instant decidedAt
    ) {
        return new AccessGrantRequestRecord(
                request.id(),
                request.requestType(),
                request.targetUserId(),
                request.targetRoleId(),
                request.templateId(),
                request.positionCode(),
                request.staffLevelId(),
                request.baseSalaryAmountCent(),
                request.salaryCurrency(),
                request.salaryDetail(),
                request.permissionCodes(),
                request.validFrom(),
                request.expiresAt(),
                request.singleUse(),
                status,
                request.reviewId(),
                request.reason(),
                request.requestedBy(),
                decidedBy,
                decidedAt,
                request.projectId(),
                request.merchantId(),
                request.createdAt(),
                decidedAt
        );
    }

    private PositionSummaryKey positionSummaryKey(AccessGrantRequestRecord request) {
        UserPositionAssignment position = request.positionCode() == null
                ? repository.findUserPositionAssignment(request.targetUserId()).orElse(null)
                : null;
        String positionCode = request.positionCode() == null && position != null
                ? position.positionCode()
                : request.positionCode();
        String staffLevelId = request.staffLevelId() == null && position != null
                ? position.staffLevelId()
                : request.staffLevelId();
        if (staffLevelId == null) {
            staffLevelId = repository.findStaffLevelAssignment(request.targetUserId())
                    .map(StaffLevelAssignment::staffLevelId)
                    .orElse("unknown");
        }
        return new PositionSummaryKey(trimToDefault(positionCode, "unknown"), trimToDefault(staffLevelId, "unknown"), "");
    }

    private SupervisorReviewService requireSupervisorReviewService() {
        if (supervisorReviewService == null) {
            throw ApiException.conflict("GRANT_APPROVAL_UNAVAILABLE", "主管复核服务不可用，请稍后重试");
        }
        return supervisorReviewService;
    }

    private String normalizeGrantRequestType(String requestType) {
        String normalized = trimToDefault(requestType, "").toLowerCase(Locale.ROOT);
        if (List.of(REQUEST_USER_ROLE, REQUEST_USER_POSITION, REQUEST_TEMPORARY_PERMISSION,
                REQUEST_USER_PERMISSION_GRANT, REQUEST_USER_PERMISSION_DENY_REMOVAL).contains(normalized)) { // M002-F01
            return normalized;
        }
        throw ApiException.badRequest("VALIDATION_FAILED", "不支持的申请类型");
    }

    private String normalizeDecision(String decision) {
        String normalized = trimToDefault(decision, "").toLowerCase(Locale.ROOT);
        if (List.of("approve", "reject").contains(normalized)) {
            return normalized;
        }
        throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过或驳回");
    }

    private PositionRoleTemplate savePositionRoleTemplate(String id, PositionRoleTemplateRequest request) {
        AccessActor actor = authorizationService.currentActor();
        requirePlatformAdmin(actor);
        String positionCode = requireText(request.positionCode(), "position_code");
        String staffLevelId = requireText(request.staffLevelId(), "staff_level_id");
        // M001-F10：映射表锁 → 在岗员工按编号 → 映射行，与定岗「员工行 → 映射行」、持授权事实锁的审批同序。
        // ponytail: 预锁之后、映射行锁之前才定岗的人要到重算时才加锁（窗口极短，PostgreSQL 检出死锁回滚一方、可同键重试）。
        repository.lockPositionRoleTemplatesForWrite();
        repository.lockAccessUsers(repository.listUserIdsAssignedToPosition(positionCode, staffLevelId).stream().sorted().toList());
        if (repository.findStaffLevelById(staffLevelId).isEmpty()) {
            throw ApiException.badRequest("UNKNOWN_STAFF_LEVEL",
                    "无法识别的员工职级：" + staffLevelId);
        }
        RoleRecord role = requireRole(requireText(request.roleId(), "role_id"));
        PermissionTemplate template = repository.findTemplateById(requireText(request.templateId(), "template_id"))
                .orElseThrow(() -> ApiException.notFound("PERMISSION_TEMPLATE_NOT_FOUND",
                        "权限模板不存在：" + request.templateId()));
        requireActorCanGrantImmediate(role.permissionCodes());
        requireActorCanGrantImmediate(repository.findTemplateItems(template.id()).stream()
                .map(PermissionTemplateItem::permissionCode)
                .toList());
        String status = trimToDefault(request.status(), "active");
        if (!Set.of("active", "inactive").contains(status)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "岗位模板状态只能是启用或停用");
        }
        Instant now = Instant.now();
        PositionRoleTemplate existing = repository.findPositionRoleTemplateById(id).orElse(null);
        if (existing != null && existing.archivedAt() != null) {
            throw ApiException.conflict("POSITION_TEMPLATE_ARCHIVED", "存档或已删除模板须经恢复后再编辑");
        }
        PositionRoleTemplate saved = repository.savePositionRoleTemplate(new PositionRoleTemplate(
                id,
                positionCode,
                staffLevelId,
                role.id(),
                template.id(),
                null,
                status,
                existing == null ? actor.id() : existing.createdBy(),
                existing == null ? now : existing.createdAt(),
                now
        ));
        auditEventService.recordSuccess(
                "access.position_role_template.saved",
                "access.role.assign",
                "position_role_template",
                saved.id(),
                null,
                request.idempotencyKey(),
                Map.of("position_code", saved.positionCode(), "staff_level_id", saved.staffLevelId())
        );
        recomputeUsersOnPositionTemplate(saved, request);
        return saved;
    }

    public EffectivePermissions effectivePermissionsForUser(String userId) {
        AccessActor actor = authorizationService.currentActor();
        requireSameOrganization(actor, requireTargetUser(userId));
        return authorizationService.effectivePermissionsForUser(userId);
    }

    /** 保存岗位模板后立即按新映射重算在岗人员派生权；停用则收回派生。 */
    private void recomputeUsersOnPositionTemplate(PositionRoleTemplate saved, PositionRoleTemplateRequest request) {
        List<String> userIds = repository.listUserIdsAssignedToPosition(saved.positionCode(), saved.staffLevelId());
        repository.lockAccessUsers(userIds.stream().sorted().toList()); // M001-F10：重列出的在岗员工（含预锁后新定岗的）按编号一次锁定
        AccessActor actor = authorizationService.currentActor();
        Instant now = Instant.now();
        if (!"active".equals(saved.status())) {
            for (String userId : userIds) {
                repository.replacePositionGrants(userId, null, null, List.of(), actor.id(), now);
            }
            return;
        }
        for (String userId : userIds) {
            StaffLevelAssignment salary = repository.findStaffLevelAssignment(userId).orElse(null);
            assignUserPositionDirect(new UserPositionAssignmentRequest(
                    request.idempotencyKey(),
                    request.operatorNote(),
                    userId,
                    saved.positionCode(),
                    saved.staffLevelId(),
                    salary == null ? null : salary.baseSalaryAmountCent(),
                    salary == null ? "CNY" : salary.salaryCurrency(),
                    salary == null ? null : salary.salaryDetail()), true);
        }
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private AccessUser requireTargetUser(String userId) {
        return repository.findUserById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在：" + userId));
    }

    // M001-F10：按员工归属写入授权的入口先锁目标员工行再读归属，与换项目（撤销旧范围全部增权）串行。
    private AccessUser lockTargetUser(String userId) {
        repository.lockAccessUsers(List.of(userId));
        return requireTargetUser(userId);
    }

    private RoleRecord requireRole(String roleId) {
        return repository.findRoleById(roleId)
                .orElseThrow(() -> ApiException.notFound("ROLE_NOT_FOUND", "角色不存在：" + roleId));
    }

    private void requireSameOrganization(AccessActor actor, AccessUser targetUser) {
        if (authorizationService.isPlatformAdmin(actor)) {
            return;
        }
        if (!authorizationService.requireProjectScope(actor).equals(targetUser.organizationId())) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                    "不能为其他组织的用户分配权限：" + targetUser.id());
        }
        if (!isBlank(actor.merchantId()) && !actor.merchantId().equals(targetUser.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "不能操作其他商户的用户");
        }
    }

    private void requireActorCanAssignToScope(AccessActor actor, String organizationId, String merchantId) {
        if (authorizationService.isPlatformAdmin(actor)) {
            return;
        }
        if (!authorizationService.requireProjectScope(actor).equals(organizationId)) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "不能把人员改挂到其他项目");
        }
        if (!isBlank(actor.merchantId()) && !actor.merchantId().equals(merchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "不能把人员改挂到其他商户");
        }
    }

    private void requirePlatformAdmin(AccessActor actor) {
        if (!authorizationService.isPlatformAdmin(actor)) {
            throw ApiException.forbidden("PLATFORM_ADMIN_REQUIRED",
                    "此操作需要平台管理员权限");
        }
    }

    private void requireRoleAssignableToTarget(RoleRecord role, AccessUser targetUser) {
        if (role.organizationId() != null && !role.organizationId().equals(targetUser.organizationId())) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                    "不能分配其他组织的角色：" + role.id());
        }
    }

    private void requireTemplateAssignableToTarget(PermissionTemplate template, AccessUser targetUser) {
        if (template.ownerScopeId() != null && !template.ownerScopeId().equals(targetUser.organizationId())) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                    "不能应用其他组织的模板：" + template.id());
        }
    }

    private PermissionTemplate executeTemplateWrite(
            String operationId,
            String successEvent,
            String failureEvent,
            String permissionCode,
            PermissionTemplateRequest request,
            java.util.function.Supplier<PermissionTemplate> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    request.idempotencyKey(),
                    request,
                    () -> {
                        PermissionTemplate template = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                "permission_template",
                                template.id(),
                                null,
                                request.idempotencyKey(),
                                Map.of("template_type", template.templateType())
                        );
                        return template;
                    },
                    PermissionTemplate.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    "permission_template",
                    null,
                    null,
                    request == null ? null : request.idempotencyKey(),
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private PermissionTemplate templateFromRequest(String id, PermissionTemplateRequest request, String sourceTemplateId, String ownerScopeId) {
        requireBoundedTemporaryTemplate(request.templateType(), request.validUntil(), Boolean.TRUE.equals(request.singleUse()));  // 建、改、复制共用
        requireTemporaryTimeOrder(request);
        Instant now = Instant.now();
        return new PermissionTemplate(
                id,
                id,
                trimToDefault(request.templateName(), id),
                request.operatorNote(),
                trimToDefault(request.templateType(), "base"),
                sourceTemplateId,
                ownerScopeId == null ? "platform" : "organization",
                ownerScopeId,
                "merge",
                request.validFrom(),
                request.validUntil(),
                Boolean.TRUE.equals(request.singleUse()),
                "active",
                request.operatorNote(),
                "access-api",
                now,
                now,
                now,
                safeList(request.permissionCodes())
        );
    }

    // M001-F05（PGM 权限规则「临时授权可设有效期或单次使用」「临时权限……有时间限制、到期自动失效」）：
    // 临时模板要么有到期时间、要么（本次）单次使用；写模板与每条套用路径在写入前共用此校验。
    private void requireBoundedTemporaryTemplate(String templateType, Instant validUntil, boolean singleUse) {
        if (isTemporaryTemplate(templateType) && validUntil == null && !singleUse) {
            throw ApiException.badRequest("VALIDATION_FAILED", "临时模板必须设置到期时间或单次使用");
        }
    }

    // M001-F05/F07：临时模板的到期时间必须晚于生效时间（倒置与零长窗口都拒绝）；修改入口在「延长有效期须重新授权」之前调用。
    private void requireTemporaryTimeOrder(PermissionTemplateRequest request) {
        if (isTemporaryTemplate(request.templateType()) && request.validFrom() != null && request.validUntil() != null
                && !request.validUntil().isAfter(request.validFrom())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "临时模板的到期时间必须晚于生效时间");
        }
    }

    private boolean isTemporaryTemplate(String templateType) {
        return "temporary".equalsIgnoreCase(trimToDefault(templateType, "base"));
    }

    private List<PermissionTemplateItem> items(String templateId, List<String> permissionCodes, String dataScope) {
        return safeList(permissionCodes).stream()
                .map(permissionCode -> new PermissionTemplateItem(
                        templateId,
                        permissionCode,
                        AccessRepository.Effect.GRANT,
                        trimToDefault(dataScope, "project"),
                        false
                ))
                .toList();
    }

    /** PGM-1.10.1 五类中的资金/核销/退款/调账/分红发放与税费：按权限域或权限码判定「涉钱」（用于审批时限）；调账只认 bill.adjust（M007-I4-A01：库存调整不是调账）。 */
    private static boolean isMoneyPermission(PermissionDefinition permission) {
        String code = permission.permissionCode() == null ? "" : permission.permissionCode().toLowerCase(Locale.ROOT);
        return "finance".equalsIgnoreCase(permission.domain() == null ? "" : permission.domain())
                || java.util.stream.Stream.of("refund", "dividend", "payout", "deposit", "settle", "bill.adjust", "waiver", "tax",
                        "verification", "payment").anyMatch(code::contains);
    }

    private void requireKnownPermissions(List<String> permissionCodes) {
        for (String permissionCode : safeList(permissionCodes)) {
            if (!repository.permissionExists(permissionCode)) {
                throw ApiException.badRequest("UNKNOWN_PERMISSION", "无法识别的权限编码：" + permissionCode);
            }
        }
    }

    private <T> PageResponse<T> page(List<T> items) {
        return new PageResponse<>(items, DEFAULT_PAGE, items.size(), items.size());
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? UUID.randomUUID().toString().replace("-", "") : slug;
    }

    private record RoleGrantContext(
            AccessActor actor,
            AccessUser targetUser,
            RoleRecord role
    ) {
    }

    private record PositionGrantContext(
            AccessActor actor,
            AccessUser targetUser,
            String positionCode,
            String staffLevelId,
            RoleRecord role,
            PermissionTemplate template,
            String mappingId,
            String salaryCurrency,
            List<String> permissionCodes
    ) {
    }

    private record GrantReviewPolicy(
            String policyCode,
            String approvalLevel,
            int requiredLevel
    ) {
    }

    private record PositionSummaryKey(
            String positionCode,
            String staffLevelId,
            String permissionCode
    ) {
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
