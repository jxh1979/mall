package com.complex.platform.access;

import com.complex.platform.access.dto.AccessGrantRequestCreateRequest;
import com.complex.platform.access.dto.AccessGrantRequestDecisionRequest;
import com.complex.platform.access.dto.AccessUserRequest;
import com.complex.platform.access.dto.PermissionTemplateApplyRequest;
import com.complex.platform.access.dto.PermissionTemplateRequest;
import com.complex.platform.access.dto.RoleRequest;
import com.complex.platform.access.dto.StaffLevelAssignmentRequest;
import com.complex.platform.access.dto.StatusChangeRequest;
import com.complex.platform.access.dto.TemporaryGrantRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AccessService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_ACCOUNT_TYPES = Set.of("staff", "merchant", "merchant_admin", "supervisor", "sales");

    private final AccessRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final AccessCredentialProvisioner credentialProvisioner;
    private final SupervisorReviewService supervisorReviewService;

    // ④ S2：开后高危授权（目标权限 requires_approval=true）不得直接装配，须走授权申请→主管复核流；默认关＝维持现状。
    @Value("${access.grant_approval.enabled:false}")
    boolean grantApprovalEnabled;

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
                credentialProvisioner.getIfAvailable(AccessCredentialProvisioner::noop),
                supervisorReviewService.getIfAvailable(() -> null));
    }

    AccessService(
            AccessRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this(repository, idempotencyService, auditEventService, authorizationService, AccessCredentialProvisioner.noop(), null);
    }

    AccessService(
            AccessRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            AccessCredentialProvisioner credentialProvisioner
    ) {
        this(repository, idempotencyService, auditEventService, authorizationService, credentialProvisioner, null);
    }

    AccessService(
            AccessRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            AccessCredentialProvisioner credentialProvisioner,
            SupervisorReviewService supervisorReviewService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.credentialProvisioner = credentialProvisioner == null ? AccessCredentialProvisioner.noop() : credentialProvisioner;
        this.supervisorReviewService = supervisorReviewService;
    }

    public PageResponse<PermissionTemplate> listPermissionTemplates() {
        AccessActor actor = authorizationService.currentActor();
        String orgId = authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId();
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
        String ownerScopeId = authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId();
        return executeTemplateWrite(
                "createPermissionTemplate",
                "permission_template.changed",
                "permission_template.change_failed",
                "access.permission_template.manage",
                request,
                () -> {
                    requireKnownPermissions(request.permissionCodes());
                    PermissionTemplate template = templateFromRequest("tpl_" + slug(request.idempotencyKey()), request, null, ownerScopeId);
                    return repository.saveTemplate(template, items(template.id(), request.permissionCodes(), request.dataScope()));
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
                    if (!authorizationService.isPlatformAdmin(actor)) {
                        PermissionTemplate existing = repository.findTemplateById(id)
                                .orElseThrow(() -> ApiException.notFound("TEMPLATE_NOT_FOUND", "template not found: " + id));
                        if (existing.ownerScopeId() == null || !actor.organizationId().equals(existing.ownerScopeId())) {
                            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "cannot modify a template owned by another organization");
                        }
                    }
                    requireKnownPermissions(request.permissionCodes());
                    // Preserve the existing owner when updating
                    String ownerScopeId = authorizationService.isPlatformAdmin(actor)
                            ? null
                            : actor.organizationId();
                    PermissionTemplate template = templateFromRequest(id, request, request.sourceTemplateId(), ownerScopeId);
                    return repository.saveTemplate(template, items(template.id(), request.permissionCodes(), request.dataScope()));
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
                        PermissionTemplate source = repository.findTemplateById(id)
                                .orElseThrow(() -> ApiException.notFound("TEMPLATE_NOT_FOUND", "template not found: " + id));
                        if (source.ownerScopeId() != null && !actor.organizationId().equals(source.ownerScopeId())) {
                            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "cannot copy a template owned by another organization");
                        }
                    }
                    String ownerScopeId = authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId();
                    return repository.copyTemplate(id, templateFromRequest("tpl_" + slug(request.idempotencyKey()), request, id, ownerScopeId));
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
                        if (!authorizationService.isPlatformAdmin(actor)) {
                            for (String userId : safeList(request.targetUserIds())) {
                                AccessUser targetUser = repository.findUserById(userId)
                                        .orElseThrow(() -> ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                                "target user not found in your organization: " + userId));
                                if (!actor.organizationId().equals(targetUser.organizationId())) {
                                    throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                            "cannot apply template to a user in another organization: " + userId);
                                }
                            }
                        }
                        PermissionTemplate template = repository.findTemplateById(id)
                                .orElseThrow(() -> ApiException.notFound("PERMISSION_TEMPLATE_NOT_FOUND", "permission template not found: " + id));
                        // A1-05：apply 必须校验模板归属——非平台管理员只能套用本组织拥有的模板，
                        // 禁止借平台模板/他组织模板把高权限项下发给本组织用户（与 update/copy 同范式）。
                        if (!authorizationService.isPlatformAdmin(actor)) {
                            if (template.ownerScopeId() == null
                                    || !template.ownerScopeId().equals(actor.organizationId())) {
                                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                        "cannot apply a template owned by another scope: " + id);
                            }
                        }
                        // P0-1：套用模板授予的权限码不得超出授权人自身有效权限（堵 copy-then-apply 提权）。
                        requireActorCanGrant(repository.findTemplateItems(id).stream()
                                .map(PermissionTemplateItem::permissionCode)
                                .toList());
                        boolean singleUse = template.singleUse() || "single_use".equalsIgnoreCase(trimToDefault(request.mode(), "append"));
                        PermissionTemplateApplyResult result = new PermissionTemplateApplyResult(repository.applyTemplate(
                                id,
                                safeList(request.targetUserIds()),
                                template.validFrom(),
                                template.validUntil(),
                                singleUse,
                                "access-api"
                        ));
                        auditEventService.recordSuccess(
                                "permission_template.applied",
                                "access.permission_template.apply",
                                "permission_template",
                                id,
                                null,
                                request.idempotencyKey(),
                                Map.of("target_count", result.applications().size())
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
        String orgId = authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId();
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
        String orgId = authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId();
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
                                "role_" + slug(request.roleName()),
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

    /** ④ S1：用户↔角色装配。派角色＝授该角色全部权限，过提权闸 + 跨组织闸；幂等 + 审计。 */
    public AccessUser assignUserRole(String userId, String roleId, String idempotencyKey) {
        AccessActor actor = authorizationService.currentActor();
        try {
            return idempotencyService.execute("assignUserRole:" + userId + ":" + roleId, idempotencyKey,
                    Map.of("userId", trimToDefault(userId, ""), "roleId", trimToDefault(roleId, "")),
                    () -> {
                        AccessUser target = requireUser(userId);
                        RoleRecord role = requireRole(roleId);
                        requireSameOrgForRoleAssignment(actor, target, role);
                        // 提权防护：派角色＝授该角色全部权限，授权人须自身已持有（复用 createRole 同款闸）。
                        requireActorCanGrant(role.permissionCodes());
                        // ④ S2：含高危权限的角色不得直接派，须走授权申请→复核流（开关开时）。
                        requireDirectGrantAllowed(role.permissionCodes());
                        repository.assignUserRole(target.id(), role.id());
                        auditEventService.recordSuccess("access.user_role.assigned", "access.role.assign",
                                "access_user", target.id(), null, idempotencyKey, Map.of("role_id", role.id()));
                        return target;
                    }, AccessUser.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.user_role.assign_failed", "access.role.assign",
                    "access_user", userId, null, idempotencyKey, codeOf(exception));
            throw exception;
        }
    }

    /** ④ S1：解绑用户角色。DELETE 天然幂等，无需幂等键。 */
    public AccessUser unassignUserRole(String userId, String roleId) {
        AccessActor actor = authorizationService.currentActor();
        try {
            AccessUser target = requireUser(userId);
            RoleRecord role = requireRole(roleId);
            requireSameOrgForRoleAssignment(actor, target, role);
            repository.unassignUserRole(target.id(), role.id());
            auditEventService.recordSuccess("access.user_role.unassigned", "access.role.assign",
                    "access_user", target.id(), null, null, Map.of("role_id", role.id()));
            return target;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.user_role.unassign_failed", "access.role.assign",
                    "access_user", userId, null, null, codeOf(exception));
            throw exception;
        }
    }

    /** ④ S1：给角色加一条权限（运行时）。过提权闸 + 自有角色闸 + 已知权限校验；幂等 + 审计。 */
    public RoleRecord addRolePermission(String roleId, String permissionCode, String idempotencyKey) {
        AccessActor actor = authorizationService.currentActor();
        try {
            return idempotencyService.execute("addRolePermission:" + roleId + ":" + permissionCode, idempotencyKey,
                    Map.of("roleId", trimToDefault(roleId, ""), "permissionCode", trimToDefault(permissionCode, "")),
                    () -> {
                        RoleRecord role = requireRole(roleId);
                        requireOwnedRole(actor, role);
                        requireKnownPermissions(List.of(requireText(permissionCode, "permissionCode")));
                        // 提权防护：不得给角色加授权人自身没有的权限。
                        requireActorCanGrant(List.of(permissionCode));
                        // ④ S2：高危权限不得直接加进角色，须走授权申请→复核流（开关开时）。
                        requireDirectGrantAllowed(List.of(permissionCode));
                        repository.addRolePermission(role.id(), permissionCode.trim());
                        auditEventService.recordSuccess("access.role_permission.added", "access.role.manage",
                                "role", role.id(), null, idempotencyKey, Map.of("permission_code", permissionCode.trim()));
                        return requireRole(role.id());
                    }, RoleRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.role_permission.add_failed", "access.role.manage",
                    "role", roleId, null, idempotencyKey, codeOf(exception));
            throw exception;
        }
    }

    /** ④ S1：从角色移除一条权限。DELETE 天然幂等。 */
    public RoleRecord removeRolePermission(String roleId, String permissionCode) {
        AccessActor actor = authorizationService.currentActor();
        try {
            RoleRecord role = requireRole(roleId);
            requireOwnedRole(actor, role);
            repository.removeRolePermission(role.id(), requireText(permissionCode, "permissionCode"));
            auditEventService.recordSuccess("access.role_permission.removed", "access.role.manage",
                    "role", role.id(), null, null, Map.of("permission_code", permissionCode.trim()));
            return requireRole(role.id());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.role_permission.remove_failed", "access.role.manage",
                    "role", roleId, null, null, codeOf(exception));
            throw exception;
        }
    }

    private AccessUser requireUser(String userId) {
        return repository.findUserById(requireText(userId, "userId"))
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "access user not found: " + userId));
    }

    private RoleRecord requireRole(String roleId) {
        return repository.findRoleById(requireText(roleId, "roleId"))
                .orElseThrow(() -> ApiException.notFound("ROLE_NOT_FOUND", "role not found: " + roleId));
    }

    /** 非平台管理员：目标用户须同组织，且角色须本组织或全局（不得跨组织派角色）。 */
    private void requireSameOrgForRoleAssignment(AccessActor actor, AccessUser target, RoleRecord role) {
        if (authorizationService.isPlatformAdmin(actor)) {
            return;
        }
        if (actor.organizationId() == null || !actor.organizationId().equals(target.organizationId())) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                    "cannot assign role to a user in another organization: " + target.id());
        }
        if (role.organizationId() != null && !actor.organizationId().equals(role.organizationId())) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                    "cannot assign a role owned by another organization: " + role.id());
        }
    }

    /** 非平台管理员：只能改本组织自有角色（全局角色仅平台管理员可改）。 */
    private void requireOwnedRole(AccessActor actor, RoleRecord role) {
        if (authorizationService.isPlatformAdmin(actor)) {
            return;
        }
        if (role.organizationId() == null || !actor.organizationId().equals(role.organizationId())) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                    "cannot modify a role owned by another scope: " + role.id());
        }
    }

    /** ④ S2：开关开 且 任一目标权限 requires_approval → 拒直接装配，导向授权申请流。关时维持现状（直接装配）。 */
    private void requireDirectGrantAllowed(List<String> permissionCodes) {
        if (!grantApprovalEnabled) {
            return;
        }
        for (String code : safeList(permissionCodes)) {
            if (code != null && repository.permissionRequiresApproval(code)) {
                throw ApiException.conflict("GRANT_REQUIRES_APPROVAL",
                        "permission '" + code + "' requires approval; submit an access grant request");
            }
        }
    }

    public PageResponse<AccessGrantRequestRecord> listGrantRequests() {
        List<AccessGrantRequestRecord> items = repository.listGrantRequests();
        auditEventService.recordSuccess("access.grant_request.viewed", "access.role.assign",
                "access_grant_request", "all", null, null, Map.of("count", items.size()));
        return page(items);
    }

    /**
     * ④ S2：创建高危授权申请（pending）。校验同直接装配（跨组织 + 提权闸 + 确属高危），再经既有主管复核 evaluate 生成待批 review。
     * 不立即落 user_roles/role_permissions——审批通过后由 approveGrantRequest 落地。
     */
    public AccessGrantRequestRecord createGrantRequest(AccessGrantRequestCreateRequest request) {
        AccessActor actor = authorizationService.currentActor();
        try {
            return idempotencyService.execute("createGrantRequest", request == null ? null : request.idempotencyKey(),
                    request,
                    () -> {
                        requireSupervisorReviewAvailable();
                        String type = requireText(request.requestType(), "requestType");
                        RoleRecord role = requireRole(request.roleId());
                        List<String> grantedCodes;
                        String targetUserId = null;
                        String permissionCode = null;
                        if ("user_role".equals(type)) {
                            AccessUser target = requireUser(request.userId());
                            requireSameOrgForRoleAssignment(actor, target, role);
                            grantedCodes = role.permissionCodes();
                            targetUserId = target.id();
                        } else if ("role_permission".equals(type)) {
                            requireOwnedRole(actor, role);
                            permissionCode = requireText(request.permissionCode(), "permissionCode");
                            requireKnownPermissions(List.of(permissionCode));
                            grantedCodes = List.of(permissionCode);
                        } else {
                            throw ApiException.badRequest("VALIDATION_FAILED", "requestType must be user_role or role_permission");
                        }
                        // 申请人自身也须能授（提权闸）；并确认确属高危（否则无需走审批流，直接装配即可）。
                        requireActorCanGrant(grantedCodes);
                        if (grantedCodes.stream().noneMatch(repository::permissionRequiresApproval)) {
                            throw ApiException.badRequest("GRANT_NOT_HIGH_RISK",
                                    "target grants no approval-required permission; assign directly instead");
                        }
                        Instant now = Instant.now();
                        String id = "agr_" + UUID.randomUUID();
                        SupervisorReviewDecisionRecord review = supervisorReviewService.evaluate(new SupervisorReviewEvaluateRequest(
                                request.idempotencyKey() + ":review", request.reason(), List.of(),
                                "permission_grant", "access_grant_request", id, 0L, request.reason()));
                        AccessGrantRequestRecord saved = repository.saveGrantRequest(new AccessGrantRequestRecord(
                                id, type, targetUserId, role.id(), permissionCode, "pending", review.reviewId(),
                                trimToNull(request.reason()), actor.id(), null, null, null, null, now, now));
                        auditEventService.recordSuccess("access.grant_request.created", "access.role.assign",
                                "access_grant_request", id, null, request.idempotencyKey(),
                                Map.of("request_type", type, "review_id", review.reviewId()));
                        return saved;
                    },
                    AccessGrantRequestRecord.class);
        } catch (RuntimeException e) {
            auditEventService.recordFailure("access.grant_request.create_failed", "access.role.assign",
                    "access_grant_request", null, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /**
     * ④ S2：审批授权申请。经既有主管复核 approve（发起人≠审批人 SoD、职级 rank、角色闸）；
     * 通过则落 user_roles/role_permissions 并标 applied，驳回则标 rejected。申请须 pending。
     */
    public AccessGrantRequestRecord approveGrantRequest(String grantRequestId, AccessGrantRequestDecisionRequest request) {
        AccessActor actor = authorizationService.currentActor();
        String id = requireText(grantRequestId, "grantRequestId");
        try {
            return idempotencyService.execute("approveGrantRequest:" + id, request == null ? null : request.idempotencyKey(),
                    request,
                    () -> {
                        requireSupervisorReviewAvailable();
                        AccessGrantRequestRecord gr = repository.findGrantRequestById(id)
                                .orElseThrow(() -> ApiException.notFound("GRANT_REQUEST_NOT_FOUND", "grant request not found: " + id));
                        if (!"pending".equals(gr.status())) {
                            throw ApiException.conflict("GRANT_REQUEST_NOT_PENDING", "grant request is not pending: " + id);
                        }
                        String decision = requireText(request.decision(), "decision");
                        SupervisorReviewDecisionRecord reviewed = supervisorReviewService.approve(gr.reviewId(),
                                new SupervisorReviewApprovalRequest(request.idempotencyKey() + ":approve", request.comment(),
                                        List.of(), null, decision, request.comment()));
                        Instant now = Instant.now();
                        String finalStatus = "approved".equals(reviewed.decision()) ? "applied" : "rejected";
                        if ("applied".equals(finalStatus)) {
                            if ("user_role".equals(gr.requestType())) {
                                repository.assignUserRole(gr.targetUserId(), gr.targetRoleId());
                            } else {
                                repository.addRolePermission(gr.targetRoleId(), gr.permissionCode());
                            }
                        }
                        AccessGrantRequestRecord updated = new AccessGrantRequestRecord(
                                gr.id(), gr.requestType(), gr.targetUserId(), gr.targetRoleId(), gr.permissionCode(),
                                finalStatus, gr.reviewId(), gr.reason(), gr.requestedBy(), actor.id(), now,
                                gr.projectId(), gr.merchantId(), gr.createdAt(), now);
                        repository.saveGrantRequest(updated);
                        auditEventService.recordSuccess("access.grant_request.decided", "governance.supervisor_review.approve",
                                "access_grant_request", id, null, request.idempotencyKey(),
                                Map.of("decision", decision, "final_status", finalStatus));
                        return updated;
                    },
                    AccessGrantRequestRecord.class);
        } catch (RuntimeException e) {
            auditEventService.recordFailure("access.grant_request.decide_failed", "governance.supervisor_review.approve",
                    "access_grant_request", id, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    private void requireSupervisorReviewAvailable() {
        if (supervisorReviewService == null) {
            throw ApiException.conflict("GRANT_APPROVAL_UNAVAILABLE", "supervisor review service is not available");
        }
    }

    /**
     * P0-1 提权防护：发证/授权动作不得授予超出「授权人自身有效权限」的权限码。
     * 平台管理员（wildcard）可授予任意权限；其余主体所授权限码必须 ⊆ 自身有效授予集（denies 已扣减）。
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
                        "cannot grant a permission you do not hold: " + code);
            }
        }
    }

    public TemporaryGrant createTemporaryGrant(TemporaryGrantRequest request) {
        AccessActor actor = authorizationService.currentActor();
        try {
            return idempotencyService.execute(
                    "createTemporaryGrant",
                    request.idempotencyKey(),
                    request,
                    () -> {
                        requireKnownPermissions(request.permissionCodes());
                        // P0 A-01：临时授权必须校验目标用户租户归属，禁止跨组织/向平台用户越权授权
                        // （与 applyPermissionTemplate 同范式，fail-closed）。
                        if (!authorizationService.isPlatformAdmin(actor)) {
                            AccessUser targetUser = repository.findUserById(request.targetUserId())
                                    .orElseThrow(() -> ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                            "target user not found in your organization: " + request.targetUserId()));
                            if (actor.organizationId() == null
                                    || !actor.organizationId().equals(targetUser.organizationId())) {
                                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                        "cannot grant to a user in another organization: " + request.targetUserId());
                            }
                        }
                        // P0-1：临时授权不得授予超出授权人自身有效权限的权限码（提权防护）。
                        requireActorCanGrant(request.permissionCodes());
                        TemporaryGrant grant = new TemporaryGrant(
                                UUID.randomUUID().toString(),
                                request.targetUserId(),
                                safeList(request.permissionCodes()),
                                request.validFrom() == null ? Instant.now() : request.validFrom(),
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
                    },
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
                        if (!authorizationService.isPlatformAdmin(actor)) {
                            TemporaryGrant grant = repository.findTemporaryGrantById(id)
                                    .orElseThrow(() -> ApiException.notFound("TEMPORARY_GRANT_NOT_FOUND", "temporary grant not found: " + id));
                            AccessUser targetUser = repository.findUserById(grant.targetUserId())
                                    .orElseThrow(() -> ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                            "grant target user not found in your organization"));
                            if (!actor.organizationId().equals(targetUser.organizationId())) {
                                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                        "cannot revoke a grant belonging to another organization");
                            }
                        }
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

    public PageResponse<AccessUser> listAccessUsers() {
        AccessActor actor = authorizationService.currentActor();
        List<AccessUser> users;
        if (authorizationService.isPlatformAdmin(actor)) {
            users = repository.listUsers();
        } else if (!isBlank(actor.merchantId())) {
            users = repository.listUsersByMerchant(actor.merchantId());
        } else {
            users = repository.listUsers(actor.organizationId());
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
                                    "accountType '" + accountType + "' is not permitted; allowed values: " + ALLOWED_ACCOUNT_TYPES);
                        }
                        String organizationId = requireText(request.organizationId(), "organization_id");
                        if (!authorizationService.isPlatformAdmin(actor)
                                && !isBlank(actor.organizationId())
                                && !actor.organizationId().equals(organizationId)) {
                            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                    "cannot create a user in another organization: " + organizationId);
                        }
                        String merchantId = trimToNull(request.merchantId());
                        if (!isBlank(actor.merchantId())) {
                            if (merchantId == null) {
                                merchantId = actor.merchantId();
                            } else if (!actor.merchantId().equals(merchantId)) {
                                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                        "cannot create a user in another merchant: " + merchantId);
                            }
                        }
                        if (requiresMerchantScope(accountType) && merchantId == null) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "merchant_id is required for merchant-scoped staff accounts");
                        }
                        Instant now = Instant.now();
                        AccessUser user = new AccessUser(
                                "usr_" + slug(username),
                                username,
                                accountType,
                                organizationId,
                                merchantId,
                                trimToDefault(request.status(), "active"),
                                now,
                                hashNullable(request.phone()),
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

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String hashNullable(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(trimmed.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is required", exception);
        }
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
                            throw ApiException.badRequest("VALIDATION_FAILED", "userId is required");
                        }
                        AccessUser targetUser = repository.findUserById(userId)
                                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "access user not found: " + userId));
                        if (!authorizationService.isPlatformAdmin(actor)
                                && !actor.organizationId().equals(targetUser.organizationId())) {
                            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN",
                                    "cannot assign a staff level to a user in another organization: " + userId);
                        }
                        if (repository.findStaffLevelById(request.staffLevelId()).isEmpty()) {
                            throw ApiException.badRequest("UNKNOWN_STAFF_LEVEL",
                                    "unknown staff_level_id: " + request.staffLevelId());
                        }
                        String currency = trimToDefault(request.salaryCurrency(), "CNY");
                        if (!"CNY".equals(currency)) {
                            throw ApiException.badRequest("UNSUPPORTED_CURRENCY",
                                    "P0 only supports CNY salary currency, got: " + currency);
                        }
                        if (request.baseSalaryAmountCent() != null && request.baseSalaryAmountCent() < 0) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "baseSalaryAmountCent must be >= 0");
                        }
                        StaffLevelAssignment assignment = new StaffLevelAssignment(
                                userId,
                                request.staffLevelId(),
                                request.baseSalaryAmountCent(),
                                currency,
                                request.salaryDetail(),
                                targetUser.organizationId(),
                                targetUser.merchantId(),
                                "access-api",
                                Instant.now()
                        );
                        StaffLevelAssignment saved = repository.saveStaffLevelAssignment(assignment);
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
                now
        );
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

    private void requireKnownPermissions(List<String> permissionCodes) {
        for (String permissionCode : safeList(permissionCodes)) {
            if (!repository.permissionExists(permissionCode)) {
                throw ApiException.badRequest("UNKNOWN_PERMISSION", "unknown permission_code: " + permissionCode);
            }
        }
    }

    private <T> PageResponse<T> page(List<T> items) {
        return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
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

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
