package com.complex.platform.access;

import com.complex.platform.access.dto.AccessUserRequest;
import com.complex.platform.access.dto.PermissionTemplateApplyRequest;
import com.complex.platform.access.dto.PermissionTemplateRequest;
import com.complex.platform.access.dto.RoleRequest;
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
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AccessService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final AccessRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public AccessService(
            AccessRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    public PageResponse<PermissionTemplate> listPermissionTemplates() {
        List<PermissionTemplate> templates = repository.listTemplates();
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
        return executeTemplateWrite(
                "createPermissionTemplate",
                "permission_template.changed",
                "permission_template.change_failed",
                "access.permission_template.manage",
                request,
                () -> {
                    requireKnownPermissions(request.permissionCodes());
                    PermissionTemplate template = templateFromRequest("tpl_" + slug(request.idempotencyKey()), request, null);
                    return repository.saveTemplate(template, items(template.id(), request.permissionCodes(), request.dataScope()));
                }
        );
    }

    public PermissionTemplate updatePermissionTemplate(String id, PermissionTemplateRequest request) {
        return executeTemplateWrite(
                "updatePermissionTemplate:" + id,
                "permission_template.changed",
                "permission_template.change_failed",
                "access.permission_template.manage",
                request,
                () -> {
                    requireKnownPermissions(request.permissionCodes());
                    PermissionTemplate template = templateFromRequest(id, request, request.sourceTemplateId());
                    return repository.saveTemplate(template, items(template.id(), request.permissionCodes(), request.dataScope()));
                }
        );
    }

    public PermissionTemplate copyPermissionTemplate(String id, PermissionTemplateRequest request) {
        return executeTemplateWrite(
                "copyPermissionTemplate:" + id,
                "permission_template.copied",
                "permission_template.copy_failed",
                "access.permission_template.manage",
                request,
                () -> repository.copyTemplate(id, templateFromRequest("tpl_" + slug(request.idempotencyKey()), request, id))
        );
    }

    public PermissionTemplateApplyResult applyPermissionTemplate(String id, PermissionTemplateApplyRequest request) {
        try {
            return idempotencyService.execute(
                    "applyPermissionTemplate:" + id,
                    request.idempotencyKey(),
                    request,
                    () -> {
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
        List<RoleRecord> roles = repository.listRoles();
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
                                safeList(request.permissionCodes())
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
        try {
            return idempotencyService.execute(
                    "revokeTemporaryGrant:" + id,
                    request.idempotencyKey(),
                    request,
                    () -> {
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
        List<AccessUser> users = repository.listUsers();
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
                        AccessUser user = new AccessUser(
                                "usr_" + slug(request.username()),
                                trimToDefault(request.username(), "user"),
                                trimToDefault(request.accountType(), "staff"),
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

    private PermissionTemplate templateFromRequest(String id, PermissionTemplateRequest request, String sourceTemplateId) {
        Instant now = Instant.now();
        return new PermissionTemplate(
                id,
                id,
                trimToDefault(request.templateName(), id),
                request.operatorNote(),
                trimToDefault(request.templateType(), "base"),
                sourceTemplateId,
                "project",
                "complex",
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
