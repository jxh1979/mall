package com.complex.platform.access;

// 文件版本：V0.07

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AccessRepository {

    default void lockGrantAuthorizationFacts() {
        throw new UnsupportedOperationException("授权仓储未实现事务内定义锁");
    }

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

    default PermissionTemplate createTemplate(PermissionTemplate template, List<PermissionTemplateItem> items) {
        if (findTemplateById(template.id()).isPresent()) {
            throw new IllegalStateException("权限模板已存在");
        }
        return saveTemplate(template, items);
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

    /**
     * 按商户 scope 列出员工账号（access_users.merchant_id = token scope）。
     * 商户面数据：商户管理员只能看到归属本商户的账号。默认在内存过滤，JdbcAccessRepository 用 SQL 谓词下推。
     * 隔离闸口（AC-ISO-1）：提供 merchant_id 行级 scoped 读通道，服务端可按 token scope 过滤。
     */
    default List<AccessUser> listUsersByMerchant(String merchantId) {
        if (merchantId == null) return listUsers();
        return listUsers().stream()
                .filter(u -> merchantId.equals(u.merchantId()))
                .toList();
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

    default Optional<RoleRecord> findRoleById(String roleId) {
        return listRoles().stream().filter(r -> r.id().equals(roleId)).findFirst();
    }

    RoleRecord createRole(RoleRecord role);

    default RoleAssignmentRecord assignUserRole(String userId, String roleId) {
        throw new UnsupportedOperationException("assignUserRole not supported by this repository");
    }

    default void unassignUserRole(String userId, String roleId) {
        throw new UnsupportedOperationException("unassignUserRole not supported by this repository");
    }

    default List<PositionRoleTemplate> listPositionRoleTemplates(String organizationId) {
        return List.of();
    }

    default Optional<PositionRoleTemplate> findPositionRoleTemplateById(String id) {
        return listPositionRoleTemplates(null).stream().filter(mapping -> mapping.id().equals(id)).findFirst();
    }

    default Optional<PositionRoleTemplate> findPositionRoleTemplate(
            String organizationId,
            String positionCode,
            String staffLevelId
    ) {
        return listPositionRoleTemplates(organizationId).stream()
                .filter(mapping -> positionCode.equals(mapping.positionCode()))
                .filter(mapping -> staffLevelId.equals(mapping.staffLevelId()))
                .filter(mapping -> "active".equals(mapping.status()))
                .findFirst();
    }

    default PositionRoleTemplate savePositionRoleTemplate(PositionRoleTemplate mapping) {
        throw new UnsupportedOperationException("savePositionRoleTemplate not supported by this repository");
    }

    default void savePositionTemplateLifecycle(PositionRoleTemplate mapping, String actorId) {
        throw new UnsupportedOperationException("savePositionTemplateLifecycle not supported by this repository");
    }

    default long countPositionTemplateUsage(String id) {
        throw new UnsupportedOperationException("countPositionTemplateUsage not supported by this repository");
    }

    default void deletePositionRoleTemplate(String id) {
        throw new UnsupportedOperationException("deletePositionRoleTemplate not supported by this repository");
    }

    default UserPositionAssignment saveUserPositionAssignment(UserPositionAssignment assignment) {
        throw new UnsupportedOperationException("saveUserPositionAssignment not supported by this repository");
    }

    default Optional<UserPositionAssignment> findUserPositionAssignment(String userId) {
        return Optional.empty();
    }

    /** 在当前岗位行上替换派生授权；独立 user_roles 与模板应用不受影响。 */
    default void replacePositionGrants(String userId, String roleId, String mappingId, List<String> applicationIds,
                                      String actorId, Instant changedAt) {
        throw new UnsupportedOperationException("replacePositionGrants not supported by this repository");
    }

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

    default AccessGrantRequestRecord saveGrantRequest(AccessGrantRequestRecord request) {
        throw new UnsupportedOperationException("saveGrantRequest not supported by this repository");
    }

    default Optional<AccessGrantRequestRecord> findGrantRequestById(String id) {
        return Optional.empty();
    }

    /** 保存申请时的完整授权事实指纹，决策更新不得覆盖。 */
    default void saveGrantFingerprint(String id, String fingerprint) {
        throw new UnsupportedOperationException("saveGrantFingerprint not supported by this repository");
    }

    default Optional<String> findGrantFingerprint(String id) {
        return Optional.empty();
    }

    default com.complex.platform.common.api.PageResponse<AccessGrantRequestRecord> pageGrantRequests(
            Instant cutoff, String organizationId, String merchantId, int page, int size) {
        List<AccessGrantRequestRecord> visible = listGrantRequests(cutoff).stream().filter(request ->
                (organizationId == null || findUserById(request.targetUserId())
                        .map(user -> organizationId.equals(user.organizationId())).orElse(false))
                && (merchantId == null || merchantId.equals(request.merchantId()))).toList();
        return com.complex.platform.common.api.PageResponse.of(visible, page, size);
    }

    default Optional<String> findGrantPolicy() { return Optional.empty(); }

    default void saveGrantPolicy(String policyJson) {
        throw new UnsupportedOperationException("saveGrantPolicy not supported by this repository");
    }

    default List<AccessGrantRequestRecord> listGrantRequests(Instant createdAtFrom) {
        return List.of();
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

    /** M087-A-04（PGM-1.10.1 所属商户/跨项目变更）：账号归属改为新范围（同步员工档案的综合体/商户归属）。 */
    default void updateUserScope(String userId, String organizationId, String merchantId, String actorId, Instant changedAt) {
        throw new UnsupportedOperationException("updateUserScope");
    }

    /** M006-G-01（PGM-1.10.1 员工单独授予 − 显式拒绝）：员工级覆盖写路径；同一 (user, code, effect) 幂等覆盖。 */
    default void saveUserPermissionOverrides(String userId, List<String> permissionCodes, Effect effect, String reason, String createdBy) {
        throw new UnsupportedOperationException("saveUserPermissionOverrides");
    }

    default int deleteUserPermissionOverrides(String userId, List<String> permissionCodes, Effect effect) {
        throw new UnsupportedOperationException("deleteUserPermissionOverrides");
    }

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
