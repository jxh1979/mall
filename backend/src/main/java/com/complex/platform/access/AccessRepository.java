package com.complex.platform.access;

// 文件版本：V0.19

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    /** 内存夹具没有人事记录；真实仓储须同时核已生效状态与已批准到点的停离职。 */
    default boolean isEmploymentBusinessBlocked(String userId, Instant checkedAt) {
        return false;
    }

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
     * M722-F05：按号读账号行的项目、商户、账号类型、状态与行版本（账号行每次被改写都会变）。
     * 默认由 findUserById 派生、行版本为空（不支持行版本的仓储只做四项比较）；JdbcAccessRepository 覆写。
     */
    default Optional<UserIdentity> findUserIdentity(String userId) {
        return findUserById(userId).map(user -> new UserIdentity(
                user.organizationId(), user.merchantId(), user.accountType(), user.status(), null));
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

    /**
     * M736：可跨项目角色能触及的归属全集——项目表全部，加城市项目公司，加在用的域主管公司；顶层平台主体与库里查不到的归属不在其中。
     * 操作员 2026-09-25 所选选项原文：「持有者只能触及归属在项目表或项目公司下的账号与数据；平台公司员工、平台管理员、最高权限、
     * 外部机构账号一律碰不到」；2026-10-01 所选选项原文：「域主管公司也按「项目」对待，持有者可以跨项目处理它们的员工与数据（只算在用的）。」
     * 默认空集：查不到就谁也跨不出本人项目；JdbcAccessRepository 覆写。
     */
    default Set<String> findProjectOrganizationIds() {
        return Set.of();
    }

    /** M736：建角色时逐个勾选「可查看项目」的候选清单，与 {@link #findProjectOrganizationIds} 同一集合，三类各自成条。默认空表。 */
    default List<RoleProjectOption> listRoleProjectOptions() {
        return List.of();
    }

    /** M736 第二步：改勾选、减少落地先取 roles 表 ROW EXCLUSIVE，与授权事实锁对 roles 的 SHARE 先到先得；必须先于任何员工行。 */
    default void lockRolesForEdit() {
    }

    /** M736 第二步：角色行 NO KEY UPDATE：两个人同时改同一角色在这里排队；与下面的共享锁互斥。 */
    default void lockRoleForEdit(String roleId) {
    }

    /** M736 第二步：分配、定岗、申请建单读角色之前取角色行共享锁（员工行之后），与改勾选的行锁互斥。 */
    default void lockRoleForGrant(String roleId) {
    }

    /** M736 第二步：当前持有该角色的员工编号（直接分配的并上岗位派生的），按编号排序。 */
    default List<String> listRoleHolderIds(String roleId) {
        return List.of();
    }

    /** M736 第二步：某员工的全部待批条目：角色编号 → 条目集合（原样返回，可能含保留标记 AuthorizationService.HELD_SINCE_UNTICKED）。 */
    default Map<String, Set<String>> findRoleViewableProjectHolds(String userId) {
        return Map.of();
    }

    /** M736 第二步：某角色的全部待批条目：员工编号 → 条目集合，按员工编号排序（原样返回，可能含保留标记，同上）。 */
    default Map<String, Set<String>> listRoleViewableProjectHolds(String roleId) {
        return Map.of();
    }

    /** M736 第二步：给某员工对某角色写待批条目（已有的跳过）。 */
    default void addRoleViewableProjectHolds(String userId, String roleId, List<String> viewableIds) {
        throw new UnsupportedOperationException("addRoleViewableProjectHolds");
    }

    /** M736 第二步：删掉某员工对某角色的全部待批条目，返回删掉的行数。 */
    default int deleteRoleViewableProjectHolds(String userId, String roleId) {
        throw new UnsupportedOperationException("deleteRoleViewableProjectHolds");
    }

    /** M736 第二步：往角色上加勾选：viewAllProjects 为真则把「可以查看全部项目」置真；viewableIds 逐条插入（已有的跳过）。清单行不删。 */
    default void addRoleViewableProjects(String roleId, boolean viewAllProjects, List<String> viewableIds) {
        throw new UnsupportedOperationException("addRoleViewableProjects");
    }

    /** M736 第二步：该角色带待减标记的条目；「可以查看全部项目」带标记时含 "*"。 */
    default Set<String> findRoleViewableProjectRemovals(String roleId) {
        return Set.of();
    }

    /** M736 第二步：把复核单号记到被减的条目上（已带标记的不改）；viewAllProjects 为真则记到「可以查看全部项目」上。角色的勾选不变。 */
    default void markRoleViewableProjectRemoval(String roleId, boolean viewAllProjects, List<String> viewableIds, String reviewId) {
        throw new UnsupportedOperationException("markRoleViewableProjectRemoval");
    }

    /**
     * M736 第二步：按复核单号落地减少：删带该单号的勾选行；roles 上带该单号的把「全部」置假并清标记；删这些条目上所有人的待批。
     * 返回去掉的条目（含 "*"）。
     */
    default List<String> applyRoleViewableProjectRemoval(String roleId, String reviewId) {
        return List.of();
    }

    /** M736 第二步：驳回：把该复核单号的待减标记清空，别的不动。 */
    default void clearRoleViewableProjectRemoval(String roleId, String reviewId) {
    }

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

    /**
     * M002-F22：实际挂在这张岗位映射上的员工（映射停用、所引权限模板被修改时对账用）。默认按岗位+职级（内存夹具）；
     * Jdbc 按 user_position_assignments.assigned_position_template_id 取人。
     */
    default List<String> listUserIdsOnPositionTemplate(PositionRoleTemplate mapping) {
        return listUserIdsAssignedToPosition(mapping.positionCode(), mapping.staffLevelId());
    }

    /** M002-F32：员工岗位行当前的岗位派生角色 id（派生前判断改动是否带来新的授出来源）；没有岗位或未记录为空。 */
    default Optional<String> findAssignedPositionRoleId(String userId) {
        return Optional.empty();
    }

    /** M002-F20：员工岗位行当前挂的岗位映射 id（派生前比对是否仍是同一映射）；没有岗位或未记录为空。 */
    default Optional<String> findAssignedPositionTemplateId(String userId) {
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

    /**
     * M001-F10：在当前事务内按编号顺序锁定这些员工行，锁到事务结束；按员工归属写入授权的入口与换项目在读员工归属前调用，锁内复核归属。
     * 默认空实现只给单线程内存夹具用，真实锁见 JdbcAccessRepository。
     */
    default void lockAccessUsers(List<String> userIds) {
    }

    /** M002-F32：员工岗位行记下的岗位派生套用（任意状态，含已消耗、已到期）；默认空列表表示不保留、照常重新套用。 */
    default List<PermissionTemplateApplication> findPositionApplications(String userId) {
        return List.of();
    }

    /** M001-F11：该模板当前仍生效的全部套用记录（模板编辑时逐人留痕用）；默认空列表只给不记套用的测试夹具用，真实查询见 JdbcAccessRepository。 */
    default List<PermissionTemplateApplication> findActiveTemplateApplications(String templateId) {
        return List.of();
    }

    /**
     * M001-F17：该模板还有没有未撤销、未消耗、套用行自身截止未过的套用（不看模板窗口——只因模板收窄而失效、到期作业还没处理的
     * 也算，放宽模板会让它不经套用、不留痕地复活）。修改入口的放宽闸据此判断；默认抛异常（fail-closed），新的仓库实现必须覆写。
     */
    default boolean hasUnfinishedTemplateApplications(String templateId) {
        throw new UnsupportedOperationException("hasUnfinishedTemplateApplications not supported by this repository");
    }

    /** M001-F12：该员工全部 active 模板套用 id，不看模板有无条目、是否生效（换项目按记录撤销）。 */
    default List<String> findActiveTemplateApplicationIds(String userId) {
        throw new UnsupportedOperationException("findActiveTemplateApplicationIds not supported by this repository");
    }

    /** M001-F12：该员工全部直接装配角色 id，不看角色有无权限码、是否生效。 */
    default List<String> findUserRoleIds(String userId) {
        throw new UnsupportedOperationException("findUserRoleIds not supported by this repository");
    }

    /** M002-F20（[10]）：改模板先取模板表意向锁，再锁在岗员工行，最后锁模板行（见 AccessService.updatePermissionTemplate）。 */
    default void lockPermissionTemplatesForEdit() {
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

    /** M001-F17：同上，只处理该模板的套用（模板截止收窄到已过去时，修改事务内调用）。 */
    default int expireLapsedTemplateApplications(String templateId, Instant checkedAt) {
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

    /** M087-I5-03：某账号某类型的待审授权申请（岗位高危挂起对账、再次申请）。默认扫全部申请过滤；Jdbc 定向查询。 */
    default List<AccessGrantRequestRecord> listPendingGrantRequests(String targetUserId, String requestType) {
        return listGrantRequests(null).stream()
                .filter(request -> targetUserId.equals(request.targetUserId()) && requestType.equals(request.requestType())
                        && "pending".equals(request.status()))
                .toList();
    }

    /** M002-F20：某员工的全部授权申请（任意类型、任意状态，按创建时间先后）；Jdbc 定向查询。 */
    default List<AccessGrantRequestRecord> listGrantRequestsForTarget(String targetUserId) {
        return listGrantRequests(null).stream().filter(request -> targetUserId.equals(request.targetUserId()))
                .sorted(java.util.Comparator.comparing(AccessGrantRequestRecord::createdAt)).toList();
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

    /**
     * XM-M002-J：同上，并在消耗日志里记下用掉它的那次请求（幂等键、请求摘要），供同一请求的重放认出。
     * 默认不记请求信息（内存仓 / 桩），其重放照旧拒绝。
     */
    default boolean markSingleUsePermissionConsumed(String sourceId, String permissionCode, String userId, Instant consumedAt,
                                                    String idempotencyKey, String requestDigest) {
        return markSingleUsePermissionConsumed(sourceId, permissionCode, userId, consumedAt);
    }

    /** XM-M002-J：该员工以该幂等键发起的这次请求（请求摘要）用掉的单次来源编号（临时授权组号 / 模板套用号）。 */
    default List<String> singleUseSourcesConsumedBy(String userId, String idempotencyKey, String requestDigest) {
        return List.of();
    }

    /**
     * M723-F08（第 656 行「操作被拒绝或失败的不算用掉」）：撤回该操作人在这次请求（幂等键、请求摘要）里对该来源的单次消耗——
     * 来源改回未消耗、消耗时间清空，并留下撤回留痕。内存仓 / 桩不做。
     */
    default void releaseSingleUsePermissionConsumption(String sourceId, String permissionCode, String actorId,
                                                       String idempotencyKey, String requestDigest) {
    }

    /**
     * M723-F10（第 656 行「同一次操作重复提交时，直接返回第一次的结果并提示已完成」）：该来源的这个权限码是否正是这名操作人
     * 以这个幂等键、这个请求摘要用掉的（且未被撤回）。内存仓 / 桩一律否（抢不到即拒绝）。
     */
    default boolean singleUseSourceConsumedByRequest(String sourceId, String permissionCode, String userId,
                                                     String idempotencyKey, String requestDigest) {
        return false;
    }

    /**
     * XM-M002-J：是否持有尚未用掉的单次授权（临时授权或模板套用，状态有效即算，不看生效时段——只许多报、不许漏报）。
     * 请求事务切面据此决定是否把整个请求包进一个事务，使单次消耗随业务一起提交或回滚。
     */
    default boolean hasUnconsumedSingleUseGrant(String userId) {
        return findPermissionEffects(userId, Instant.now()).stream()
                .anyMatch(effect -> effect.effect() == Effect.GRANT && effect.singleUse() && !effect.consumed());
    }

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

    /** M722-F05：账号行的项目、商户、账号类型、状态与行版本（仓储不支持行版本时为空）。 */
    record UserIdentity(String organizationId, String merchantId, String accountType, String status, String rowVersion) {
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
