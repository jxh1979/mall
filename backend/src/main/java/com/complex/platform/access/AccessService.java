package com.complex.platform.access;

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
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AccessService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_ACCOUNT_TYPES = Set.of("staff", "merchant", "supervisor");

    private final AccessRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public AccessService(
            AccessRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
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
                        PermissionTemplateApplyResult result = new PermissionTemplateApplyResult(repository.applyTemplate(
                                id,
                                safeList(request.targetUserIds()),
                                null,
                                null,
                                false,
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

    public TemporaryGrant createTemporaryGrant(TemporaryGrantRequest request) {
        try {
            return idempotencyService.execute(
                    "createTemporaryGrant",
                    request.idempotencyKey(),
                    request,
                    () -> {
                        requireKnownPermissions(request.permissionCodes());
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
        String orgId = authorizationService.isPlatformAdmin(actor) ? null : actor.organizationId();
        List<AccessUser> users = repository.listUsers(orgId);
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
        try {
            return idempotencyService.execute(
                    "createAccessUser",
                    request.idempotencyKey(),
                    request,
                    () -> {
                        String accountType = trimToDefault(request.accountType(), "staff");
                        if (!ALLOWED_ACCOUNT_TYPES.contains(accountType)) {
                            throw ApiException.badRequest("INVALID_ACCOUNT_TYPE",
                                    "accountType '" + accountType + "' is not permitted; allowed values: " + ALLOWED_ACCOUNT_TYPES);
                        }
                        AccessUser user = new AccessUser(
                                "usr_" + slug(request.username()),
                                trimToDefault(request.username(), "user"),
                                accountType,
                                request.organizationId(),
                                request.merchantId(),
                                trimToDefault(request.status(), "active"),
                                Instant.now()
                        );
                        AccessUser saved = repository.createUser(user);
                        auditEventService.recordSuccess("access.user.changed", "access.user.manage", "access_user", saved.id(), null, request.idempotencyKey(), Map.of());
                        return saved;
                    },
                    AccessUser.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.user.changed_failed", "access.user.manage", "access_user", null, null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
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
