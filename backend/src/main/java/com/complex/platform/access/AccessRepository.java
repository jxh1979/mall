package com.complex.platform.access;

// 文件版本：V0.14

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

    /** 当前定在该职位+职级上的账号。默认按人员列表逐条查岗位；Jdbc 仓储可下推 SQL。 */
    default List<String> listUserIdsAssignedToPosition(String positionCode, String staffLevelId) {
        return listUsers().stream()
                .filter(user -> findUserPositionAssignment(user.id())
                        .filter(assignment -> positionCode.equals(assignment.positionCode())
                                && staffLevelId.equals(assignment.staffLevelId()))
                        .isPresent())
                .map(AccessUser::id)
                .toList();
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

    /**
     * M001-F10：在当前事务内按编号顺序锁定这些员工行，锁到事务结束；按员工归属写入授权的入口与换项目在读员工归属前调用，锁内复核归属。
     * 默认空实现只给单线程内存夹具用，真实锁见 JdbcAccessRepository。
     */
    default void lockAccessUsers(List<String> userIds) {
    }

    /** M001-F11：该模板当前仍生效的全部套用记录（模板编辑时逐人留痕用）；默认空列表只给不记套用的测试夹具用，真实查询见 JdbcAccessRepository。 */
    default List<PermissionTemplateApplication> findActiveTemplateApplications(String templateId) {
        return List.of();
    }

    /** M001-F12：该员工全部 active 模板套用 id，不看模板有无条目、是否生效（换项目按记录撤销）。 */
    default List<String> findActiveTemplateApplicationIds(String userId) {
        throw new UnsupportedOperationException("findActiveTemplateApplicationIds not supported by this repository");
    }

    /** M001-F12：该员工全部直接装配角色 id，不看角色有无权限码、是否生效。 */
    default List<String> findUserRoleIds(String userId) {
        throw new UnsupportedOperationException("findUserRoleIds not supported by this repository");
    }

    /** M001-F13：修改模板前取该模板写锁，锁到事务结束，锁内读前态；默认空实现只给单线程内存夹具用。 */
    default void lockTemplateForEdit(String templateId) {
    }

    /** M001-F13：套用模板前取该模板共享锁，等进行中的编辑提交后再读条目；默认空实现只给单线程内存夹具用。 */
    default void lockTemplateForApply(String templateId) {
    }

    /** M001-F10：保存岗位映射前先取映射表写意向锁，与授权事实锁先到先得；默认空实现只给单线程内存夹具用。 */
    default void lockPositionRoleTemplatesForWrite() {
    }

    /** M001-F03：把全部员工已到期仍标 active 的模板套用置 expired 并各写一条到期日志，返回处理条数（到期作业调用）。 */
    default int expireLapsedTemplateApplications(Instant checkedAt) {
        throw new UnsupportedOperationException("expireLapsedTemplateApplications not supported by this repository");
    }

    TemporaryGrant createTemporaryGrant(TemporaryGrant temporaryGrant);

    TemporaryGrant revokeTemporaryGrant(String id, String revokedBy, Instant revokedAt);

    /** M087-I7-02：撤销某账号的若干仍生效的模板授予（范围变更时冻结旧范围下的单独模板授予）；返回撤销行数。 */
    default int revokeTemplateApplications(String userId, List<String> applicationIds, String revokedBy, Instant revokedAt) {
        throw new UnsupportedOperationException("revokeTemplateApplications not supported by this repository");
    }

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

    /** M087-I5-03：某账号某类型的待审授权申请（范围变更时作废旧范围高危挂起）。 */
    // ponytail: filters the full list; add a targeted query if access_grant_requests grows large
    default List<AccessGrantRequestRecord> listPendingGrantRequests(String targetUserId, String requestType) {
        return listGrantRequests(null).stream()
                .filter(request -> targetUserId.equals(request.targetUserId()) && requestType.equals(request.requestType())
                        && "pending".equals(request.status()))
                .toList();
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

    /**
     * M006-G-01（PGM-1.10.1 员工单独授予 − 显式拒绝）：员工级覆盖写路径；同一 (user, code, effect) 幂等覆盖。
     * M002-F02：只写 origin=explicit 行，岗位高危挂起行另走 {@link #saveHighRiskHolds}。
     */
    default void saveUserPermissionOverrides(String userId, List<String> permissionCodes, Effect effect, String reason, String createdBy) {
        throw new UnsupportedOperationException("saveUserPermissionOverrides");
    }

    /** M002-F02：删除该员工这些码、该 effect 的全部行（员工显式行与岗位高危挂起行一并删除）。 */
    default int deleteUserPermissionOverrides(String userId, List<String> permissionCodes, Effect effect) {
        throw new UnsupportedOperationException("deleteUserPermissionOverrides");
    }

    /** M002-F02：写岗位高危挂起行（origin=position_hold，只能是 deny），与员工显式拒绝分行存放。 */
    default void saveHighRiskHolds(String userId, List<String> permissionCodes, String reason, String createdBy) {
        throw new UnsupportedOperationException("saveHighRiskHolds");
    }

    /** M002-F02：只删岗位高危挂起行，员工显式拒绝不动；返回删除行数。 */
    default int deleteHighRiskHolds(String userId, List<String> permissionCodes) {
        throw new UnsupportedOperationException("deleteHighRiskHolds");
    }

    /** M002-F01：只删员工显式拒绝行，岗位高危挂起行不动；返回删除行数。 */
    default int deleteExplicitDenies(String userId, List<String> permissionCodes) {
        throw new UnsupportedOperationException("deleteExplicitDenies");
    }

    /** M002-F06：该员工这些码的员工显式拒绝行（按码排序），解除单以此钉住所解除的拒绝版本。 */
    default List<ExplicitDenyRow> findExplicitDenies(String userId, List<String> permissionCodes) {
        throw new UnsupportedOperationException("findExplicitDenies");
    }

    record ExplicitDenyRow(String permissionCode, String reason, String createdBy, Instant createdAt) {
    }

    enum Effect {
        GRANT,
        DENY
    }

    enum Source {
        ROLE,
        TEMPLATE_APPLICATION,
        TEMPORARY_GRANT,
        USER_OVERRIDE,
        POSITION_HOLD // M002-F02：岗位高危挂起行（覆盖表 origin=position_hold）
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
