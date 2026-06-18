package com.complex.platform.access;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AccessRepository {

    List<PermissionDefinition> listPermissions();

    List<PermissionTemplate> listTemplates();

    /**
     * Lists templates visible to the given organization. Null = platform admin (all templates).
     * Default delegates to listTemplates() and filters in memory; JdbcAccessRepository overrides with SQL.
     */
    default List<PermissionTemplate> listTemplates(String organizationId) {
        if (organizationId == null) return listTemplates();
        return listTemplates().stream()
                .filter(t -> organizationId.equals(t.ownerScopeId()))
                .toList();
    }

    default Optional<PermissionTemplate> findTemplateById(String id) {
        return listTemplates().stream().filter(t -> t.id().equals(id)).findFirst();
    }

    PermissionTemplate saveTemplate(PermissionTemplate template, List<PermissionTemplateItem> items);

    PermissionTemplate copyTemplate(String sourceTemplateId, PermissionTemplate copy);

    List<PermissionTemplateItem> findTemplateItems(String templateId);

    AccessUser createUser(AccessUser user);

    List<AccessUser> listUsers();

    /**
     * Lists users belonging to the given organization. Null = platform admin (all users).
     */
    default List<AccessUser> listUsers(String organizationId) {
        if (organizationId == null) return listUsers();
        return listUsers().stream()
                .filter(u -> organizationId.equals(u.organizationId()))
                .toList();
    }

    default Optional<AccessUser> findUserById(String userId) {
        return listUsers().stream().filter(u -> u.id().equals(userId)).findFirst();
    }

    List<RoleRecord> listRoles();

    /**
     * Lists roles visible to the given organization: global roles (organizationId=null) + own org roles.
     * Null organizationId = platform admin (all roles).
     */
    default List<RoleRecord> listRoles(String organizationId) {
        if (organizationId == null) return listRoles();
        return listRoles().stream()
                .filter(r -> r.organizationId() == null || organizationId.equals(r.organizationId()))
                .toList();
    }

    RoleRecord createRole(RoleRecord role);

    List<PermissionTemplateApplication> applyTemplate(
            String templateId,
            List<String> targetUserIds,
            Instant validFrom,
            Instant validUntil,
            boolean singleUse,
            String appliedBy
    );

    TemporaryGrant createTemporaryGrant(TemporaryGrant temporaryGrant);

    TemporaryGrant revokeTemporaryGrant(String id, String revokedBy, Instant revokedAt);

    default Optional<TemporaryGrant> findTemporaryGrantById(String id) {
        return Optional.empty();
    }

    /** 职级定义列表（参考数据，平台全局）。默认空，JdbcAccessRepository 覆盖。 */
    default List<StaffLevelRecord> listStaffLevels() {
        return List.of();
    }

    default Optional<StaffLevelRecord> findStaffLevelById(String staffLevelId) {
        return listStaffLevels().stream().filter(l -> l.id().equals(staffLevelId)).findFirst();
    }

    /** 落库员工职级与薪资归属（upsert by user_id）。 */
    default StaffLevelAssignment saveStaffLevelAssignment(StaffLevelAssignment assignment) {
        throw new UnsupportedOperationException("saveStaffLevelAssignment not supported by this repository");
    }

    default Optional<StaffLevelAssignment> findStaffLevelAssignment(String userId) {
        return Optional.empty();
    }

    boolean permissionExists(String permissionCode);

    List<PermissionEffect> findPermissionEffects(String userId, Instant checkedAt);

    /**
     * Atomically marks a single-use permission as consumed.
     * Returns true if this call performed the consume; false if already consumed by a concurrent request.
     */
    boolean markSingleUsePermissionConsumed(String sourceId, String permissionCode, String userId, Instant consumedAt);

    enum Effect {
        GRANT,
        DENY
    }

    enum Source {
        ROLE,
        TEMPLATE_APPLICATION,
        TEMPORARY_GRANT,
        USER_OVERRIDE
    }

    record PermissionEffect(
            String userId,
            String permissionCode,
            Effect effect,
            Source source,
            String sourceId,
            Instant validFrom,
            Instant validUntil,
            boolean singleUse,
            boolean consumed
    ) {
    }
}
