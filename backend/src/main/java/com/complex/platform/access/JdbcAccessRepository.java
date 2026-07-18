package com.complex.platform.access;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcAccessRepository implements AccessRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcAccessRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean permissionExists(String permissionCode) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM permissions
                 WHERE permission_code = :permission_code
                   AND status = 'active'
                """, Map.of("permission_code", permissionCode), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public List<PermissionDefinition> listPermissions() {
        return jdbcTemplate.query("""
                SELECT permission_code, permission_name, domain, risk_level, requires_approval, status
                  FROM permissions
                 WHERE status = 'active'
                 ORDER BY domain, permission_code
                """, this::mapPermission);
    }

    @Override
    public List<PermissionTemplate> listTemplates() {
        return jdbcTemplate.query("""
                SELECT t.id, t.template_code, t.template_name, t.description, t.template_type, t.source_template_id,
                       t.owner_scope_type, t.owner_scope_id, t.usage_mode, t.valid_from, t.valid_to, t.single_use,
                       t.status, t.reason, t.approved_by, t.approved_at, t.created_at, t.updated_at,
                       COALESCE(array_agg(i.permission_code) FILTER (WHERE i.permission_code IS NOT NULL), ARRAY[]::text[]) AS permission_codes
                  FROM permission_templates t
                  LEFT JOIN permission_template_items i ON i.template_id = t.id
                 GROUP BY t.id
                 ORDER BY t.created_at DESC, t.id
                """, this::mapTemplate);
    }

    @Override
    public List<PermissionTemplate> listTemplates(String organizationId) {
        if (organizationId == null) return listTemplates();
        return jdbcTemplate.query("""
                SELECT t.id, t.template_code, t.template_name, t.description, t.template_type, t.source_template_id,
                       t.owner_scope_type, t.owner_scope_id, t.usage_mode, t.valid_from, t.valid_to, t.single_use,
                       t.status, t.reason, t.approved_by, t.approved_at, t.created_at, t.updated_at,
                       COALESCE(array_agg(i.permission_code) FILTER (WHERE i.permission_code IS NOT NULL), ARRAY[]::text[]) AS permission_codes
                  FROM permission_templates t
                  LEFT JOIN permission_template_items i ON i.template_id = t.id
                 WHERE t.owner_scope_id = :organization_id
                 GROUP BY t.id
                 ORDER BY t.created_at DESC, t.id
                """, Map.of("organization_id", organizationId), this::mapTemplate);
    }

    @Override
    public Optional<PermissionTemplate> findTemplateById(String id) {
        List<PermissionTemplate> result = jdbcTemplate.query("""
                SELECT t.id, t.template_code, t.template_name, t.description, t.template_type, t.source_template_id,
                       t.owner_scope_type, t.owner_scope_id, t.usage_mode, t.valid_from, t.valid_to, t.single_use,
                       t.status, t.reason, t.approved_by, t.approved_at, t.created_at, t.updated_at,
                       COALESCE(array_agg(i.permission_code) FILTER (WHERE i.permission_code IS NOT NULL), ARRAY[]::text[]) AS permission_codes
                  FROM permission_templates t
                  LEFT JOIN permission_template_items i ON i.template_id = t.id
                 WHERE t.id = :id
                 GROUP BY t.id
                """, Map.of("id", id), this::mapTemplate);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    @Transactional
    public PermissionTemplate saveTemplate(PermissionTemplate template, List<PermissionTemplateItem> items) {
        jdbcTemplate.update("""
                INSERT INTO permission_templates (
                    id, template_code, template_name, description, template_type, source_template_id,
                    owner_scope_type, owner_scope_id, usage_mode, valid_from, valid_to, single_use,
                    status, reason, approved_by, approved_at, created_at, updated_at
                )
                VALUES (
                    :id, :template_code, :template_name, :description, :template_type, :source_template_id,
                    :owner_scope_type, :owner_scope_id, :usage_mode, :valid_from, :valid_to, :single_use,
                    :status, :reason, :approved_by, :approved_at, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    template_code = EXCLUDED.template_code,
                    template_name = EXCLUDED.template_name,
                    description = EXCLUDED.description,
                    template_type = EXCLUDED.template_type,
                    source_template_id = EXCLUDED.source_template_id,
                    owner_scope_type = EXCLUDED.owner_scope_type,
                    owner_scope_id = EXCLUDED.owner_scope_id,
                    usage_mode = EXCLUDED.usage_mode,
                    valid_from = EXCLUDED.valid_from,
                    valid_to = EXCLUDED.valid_to,
                    single_use = EXCLUDED.single_use,
                    status = EXCLUDED.status,
                    reason = EXCLUDED.reason,
                    approved_by = EXCLUDED.approved_by,
                    approved_at = EXCLUDED.approved_at,
                    updated_at = EXCLUDED.updated_at
                """, templateParameters(template));

        jdbcTemplate.update("""
                DELETE FROM permission_template_items
                 WHERE template_id = :template_id
                """, Map.of("template_id", template.id()));
        insertTemplateItems(items);
        return template;
    }

    @Override
    public PermissionTemplate copyTemplate(String sourceTemplateId, PermissionTemplate copy) {
        List<PermissionTemplateItem> copiedItems = findTemplateItems(sourceTemplateId).stream()
                .map(item -> new PermissionTemplateItem(
                        copy.id(),
                        item.permissionCode(),
                        item.effect(),
                        item.dataScope(),
                        item.sensitive()
                ))
                .toList();
        return saveTemplate(copy, copiedItems);
    }

    @Override
    public List<PermissionTemplateItem> findTemplateItems(String templateId) {
        return jdbcTemplate.query("""
                SELECT template_id, permission_code, effect, data_scope, sensitive
                  FROM permission_template_items
                 WHERE template_id = :template_id
                 ORDER BY permission_code, effect
                """, Map.of("template_id", templateId), this::mapTemplateItem);
    }

    @Override
    public AccessUser createUser(AccessUser user) {
        jdbcTemplate.update("""
                INSERT INTO access_users (
                    id, username, account_type, organization_id, merchant_id, status, created_at,
                    phone_hash, created_via, consent_reference, evidence_attachment_ids, operator_note
                )
                VALUES (
                    :id, :username, :account_type, :organization_id, :merchant_id, :status, :created_at,
                    :phone_hash, :created_via, :consent_reference, :evidence_attachment_ids, :operator_note
                )
                ON CONFLICT (id) DO UPDATE SET
                    username = EXCLUDED.username,
                    account_type = EXCLUDED.account_type,
                    organization_id = EXCLUDED.organization_id,
                    merchant_id = EXCLUDED.merchant_id,
                    status = EXCLUDED.status,
                    phone_hash = EXCLUDED.phone_hash,
                    created_via = EXCLUDED.created_via,
                    consent_reference = EXCLUDED.consent_reference,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    operator_note = EXCLUDED.operator_note
                """, new MapSqlParameterSource()
                .addValue("id", user.id())
                .addValue("username", user.username())
                .addValue("account_type", user.accountType())
                .addValue("organization_id", user.organizationId())
                .addValue("merchant_id", user.merchantId())
                .addValue("status", user.status())
                .addValue("created_at", timestampWithTimeZone(user.createdAt()))
                .addValue("phone_hash", user.phoneHash())
                .addValue("created_via", user.createdVia())
                .addValue("consent_reference", user.consentReference())
                .addValue("evidence_attachment_ids", encodeEvidenceIds(user.evidenceAttachmentIds()))
                .addValue("operator_note", user.operatorNote()));
        return user;
    }

    @Override
    public List<AccessUser> listUsers() {
        return jdbcTemplate.query("""
                SELECT id, username, account_type, organization_id, merchant_id, status, created_at,
                       phone_hash, created_via, consent_reference, evidence_attachment_ids, operator_note
                  FROM access_users
                 ORDER BY username
                """, this::mapUser);
    }

    @Override
    public List<AccessUser> listUsers(String organizationId) {
        if (organizationId == null) return listUsers();
        return jdbcTemplate.query("""
                SELECT id, username, account_type, organization_id, merchant_id, status, created_at,
                       phone_hash, created_via, consent_reference, evidence_attachment_ids, operator_note
                  FROM access_users
                 WHERE organization_id = :organization_id
                 ORDER BY username
                """, Map.of("organization_id", organizationId), this::mapUser);
    }

    @Override
    public List<AccessUser> listUsersByMerchant(String merchantId) {
        // 商户面 scoped 读通道：按 access_users.merchant_id = token scope 过滤（AC-ISO-1，§4）。
        if (merchantId == null) return listUsers();
        return jdbcTemplate.query("""
                SELECT id, username, account_type, organization_id, merchant_id, status, created_at,
                       phone_hash, created_via, consent_reference, evidence_attachment_ids, operator_note
                  FROM access_users
                 WHERE merchant_id = :merchant_id
                 ORDER BY username
                """, Map.of("merchant_id", merchantId), this::mapUser);
    }

    @Override
    public Optional<AccessUser> findUserById(String userId) {
        List<AccessUser> result = jdbcTemplate.query("""
                SELECT id, username, account_type, organization_id, merchant_id, status, created_at,
                       phone_hash, created_via, consent_reference, evidence_attachment_ids, operator_note
                  FROM access_users
                 WHERE id = :id
                """, Map.of("id", userId), this::mapUser);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<StaffLevelRecord> listStaffLevels() {
        return jdbcTemplate.query("""
                SELECT id, main_level, sub_level, level_rank, salary_policy_summary, status, created_at
                  FROM staff_levels
                 ORDER BY level_rank
                """, this::mapStaffLevel);
    }

    @Override
    public Optional<StaffLevelRecord> findStaffLevelById(String staffLevelId) {
        List<StaffLevelRecord> result = jdbcTemplate.query("""
                SELECT id, main_level, sub_level, level_rank, salary_policy_summary, status, created_at
                  FROM staff_levels
                 WHERE id = :id
                """, Map.of("id", staffLevelId), this::mapStaffLevel);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public StaffLevelAssignment saveStaffLevelAssignment(StaffLevelAssignment assignment) {
        jdbcTemplate.update("""
                INSERT INTO staff_level_assignments (
                    user_id, staff_level_id, base_salary_amount_cent, salary_currency, salary_detail,
                    organization_id, merchant_id, assigned_by, assigned_at
                )
                VALUES (
                    :user_id, :staff_level_id, :base_salary_amount_cent, :salary_currency, :salary_detail,
                    :organization_id, :merchant_id, :assigned_by, :assigned_at
                )
                ON CONFLICT (user_id) DO UPDATE SET
                    staff_level_id = EXCLUDED.staff_level_id,
                    base_salary_amount_cent = EXCLUDED.base_salary_amount_cent,
                    salary_currency = EXCLUDED.salary_currency,
                    salary_detail = EXCLUDED.salary_detail,
                    organization_id = EXCLUDED.organization_id,
                    merchant_id = EXCLUDED.merchant_id,
                    assigned_by = EXCLUDED.assigned_by,
                    assigned_at = EXCLUDED.assigned_at
                """, new MapSqlParameterSource()
                .addValue("user_id", assignment.userId())
                .addValue("staff_level_id", assignment.staffLevelId())
                .addValue("base_salary_amount_cent", assignment.baseSalaryAmountCent())
                .addValue("salary_currency", assignment.salaryCurrency())
                .addValue("salary_detail", assignment.salaryDetail())
                .addValue("organization_id", assignment.organizationId())
                .addValue("merchant_id", assignment.merchantId())
                .addValue("assigned_by", assignment.assignedBy())
                .addValue("assigned_at", timestampWithTimeZone(assignment.assignedAt())));
        return assignment;
    }

    @Override
    public List<StaffLevelAssignment> listStaffLevelAssignmentsByMerchant(String merchantId) {
        // 商户面 scoped 读通道：按 staff_level_assignments.merchant_id = token scope 过滤（AC-ISO-1，§4）。
        return jdbcTemplate.query("""
                SELECT user_id, staff_level_id, base_salary_amount_cent, salary_currency, salary_detail,
                       organization_id, merchant_id, assigned_by, assigned_at
                  FROM staff_level_assignments
                 WHERE merchant_id = :merchant_id
                 ORDER BY user_id
                """, Map.of("merchant_id", merchantId), this::mapStaffLevelAssignment);
    }

    @Override
    public Optional<StaffLevelAssignment> findStaffLevelAssignment(String userId) {
        List<StaffLevelAssignment> result = jdbcTemplate.query("""
                SELECT user_id, staff_level_id, base_salary_amount_cent, salary_currency, salary_detail,
                       organization_id, merchant_id, assigned_by, assigned_at
                  FROM staff_level_assignments
                 WHERE user_id = :user_id
                """, Map.of("user_id", userId), this::mapStaffLevelAssignment);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<RoleRecord> listRoles() {
        return attachPermissionCodes(jdbcTemplate.query("""
                SELECT id, role_name, description, data_scope, status, created_at, organization_id
                  FROM roles
                 ORDER BY role_name
                """, this::mapRoleShallow));
    }

    @Override
    public List<RoleRecord> listRoles(String organizationId) {
        if (organizationId == null) return listRoles();
        return attachPermissionCodes(jdbcTemplate.query("""
                SELECT id, role_name, description, data_scope, status, created_at, organization_id
                  FROM roles
                 WHERE organization_id IS NULL OR organization_id = :organization_id
                 ORDER BY role_name
                """, Map.of("organization_id", organizationId), this::mapRoleShallow));
    }

    /** 4.4：列表路径不逐角色查权限（消除 1+N）——先取角色，再一次批量取全部 role_permissions 挂回。 */
    private List<RoleRecord> attachPermissionCodes(List<RoleRecord> roles) {
        if (roles.isEmpty()) {
            return roles;
        }
        List<String> ids = roles.stream().map(RoleRecord::id).toList();
        java.util.Map<String, List<String>> byRole = new java.util.LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT role_id, permission_code
                  FROM role_permissions
                 WHERE role_id IN (:ids)
                 ORDER BY role_id, permission_code
                """, new MapSqlParameterSource("ids", ids),
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        byRole.computeIfAbsent(rs.getString("role_id"), k -> new ArrayList<>())
                                .add(rs.getString("permission_code")));
        return roles.stream().map(r -> new RoleRecord(
                r.id(), r.roleName(), r.description(), r.dataScope(), r.status(), r.createdAt(),
                byRole.getOrDefault(r.id(), List.of()), r.organizationId())).toList();
    }

    private RoleRecord mapRoleShallow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RoleRecord(
                resultSet.getString("id"),
                resultSet.getString("role_name"),
                resultSet.getString("description"),
                resultSet.getString("data_scope"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                List.of(),
                resultSet.getString("organization_id"));
    }

    @Override
    @Transactional
    public RoleRecord createRole(RoleRecord role) {
        jdbcTemplate.update("""
                INSERT INTO roles (
                    id, role_name, description, data_scope, status, created_at, organization_id
                )
                VALUES (
                    :id, :role_name, :description, :data_scope, :status, :created_at, :organization_id
                )
                ON CONFLICT (id) DO UPDATE SET
                    role_name = EXCLUDED.role_name,
                    description = EXCLUDED.description,
                    data_scope = EXCLUDED.data_scope,
                    status = EXCLUDED.status,
                    organization_id = EXCLUDED.organization_id
                """, new MapSqlParameterSource()
                .addValue("id", role.id())
                .addValue("role_name", role.roleName())
                .addValue("description", role.description())
                .addValue("data_scope", role.dataScope())
                .addValue("status", role.status())
                .addValue("created_at", timestampWithTimeZone(role.createdAt()))
                .addValue("organization_id", role.organizationId()));
        jdbcTemplate.update("""
                DELETE FROM role_permissions
                 WHERE role_id = :role_id
                """, Map.of("role_id", role.id()));
        if (role.permissionCodes() != null && !role.permissionCodes().isEmpty()) {
            jdbcTemplate.batchUpdate("""
                    INSERT INTO role_permissions (role_id, permission_code)
                    VALUES (:role_id, :permission_code)
                    ON CONFLICT (role_id, permission_code) DO NOTHING
                    """, role.permissionCodes().stream()
                    .map(permissionCode -> new MapSqlParameterSource()
                            .addValue("role_id", role.id())
                            .addValue("permission_code", permissionCode))
                    .toArray(MapSqlParameterSource[]::new));
        }
        return role;
    }

    @Override
    public Optional<RoleRecord> findRoleById(String roleId) {
        List<RoleRecord> result = jdbcTemplate.query("""
                SELECT id, role_name, description, data_scope, status, created_at, organization_id
                  FROM roles
                 WHERE id = :id
                """, Map.of("id", roleId), this::mapRole);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public RoleAssignmentRecord assignUserRole(String userId, String roleId) {
        jdbcTemplate.update("""
                INSERT INTO user_roles (user_id, role_id)
                VALUES (:user_id, :role_id)
                ON CONFLICT (user_id, role_id) DO NOTHING
                """, Map.of("user_id", userId, "role_id", roleId));
        return new RoleAssignmentRecord(userId, roleId);
    }

    @Override
    public void unassignUserRole(String userId, String roleId) {
        jdbcTemplate.update("""
                DELETE FROM user_roles
                 WHERE user_id = :user_id
                   AND role_id = :role_id
                """, Map.of("user_id", userId, "role_id", roleId));
    }

    @Override
    public List<PositionRoleTemplate> listPositionRoleTemplates(String organizationId) {
        if (organizationId == null) {
            return jdbcTemplate.query("""
                    SELECT id, position_code, staff_level_id, role_id, template_id, organization_id,
                           status, created_by, created_at, updated_at
                      FROM position_role_templates
                     ORDER BY position_code, staff_level_id, organization_id NULLS FIRST
                    """, this::mapPositionRoleTemplate);
        }
        return jdbcTemplate.query("""
                SELECT id, position_code, staff_level_id, role_id, template_id, organization_id,
                       status, created_by, created_at, updated_at
                  FROM position_role_templates
                 WHERE organization_id IS NULL OR organization_id = :organization_id
                 ORDER BY position_code, staff_level_id, organization_id NULLS FIRST
                """, Map.of("organization_id", organizationId), this::mapPositionRoleTemplate);
    }

    @Override
    public Optional<PositionRoleTemplate> findPositionRoleTemplateById(String id) {
        List<PositionRoleTemplate> result = jdbcTemplate.query("""
                SELECT id, position_code, staff_level_id, role_id, template_id, organization_id,
                       status, created_by, created_at, updated_at
                  FROM position_role_templates
                 WHERE id = :id
                """, Map.of("id", id), this::mapPositionRoleTemplate);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public Optional<PositionRoleTemplate> findPositionRoleTemplate(
            String organizationId,
            String positionCode,
            String staffLevelId
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("organization_id", organizationId, java.sql.Types.VARCHAR)
                .addValue("position_code", positionCode)
                .addValue("staff_level_id", staffLevelId);
        List<PositionRoleTemplate> result = jdbcTemplate.query("""
                SELECT id, position_code, staff_level_id, role_id, template_id, organization_id,
                       status, created_by, created_at, updated_at
                  FROM position_role_templates
                 WHERE status = 'active'
                   AND position_code = :position_code
                   AND staff_level_id = :staff_level_id
                   AND (
                        (:organization_id IS NULL AND organization_id IS NULL)
                        OR (:organization_id IS NOT NULL AND (organization_id = :organization_id OR organization_id IS NULL))
                   )
                 ORDER BY CASE WHEN organization_id = :organization_id THEN 0 ELSE 1 END
                 LIMIT 1
                """, parameters, this::mapPositionRoleTemplate);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public PositionRoleTemplate savePositionRoleTemplate(PositionRoleTemplate mapping) {
        jdbcTemplate.update("""
                INSERT INTO position_role_templates (
                    id, position_code, staff_level_id, role_id, template_id, organization_id,
                    status, created_by, created_at, updated_at
                )
                VALUES (
                    :id, :position_code, :staff_level_id, :role_id, :template_id, :organization_id,
                    :status, :created_by, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    position_code = EXCLUDED.position_code,
                    staff_level_id = EXCLUDED.staff_level_id,
                    role_id = EXCLUDED.role_id,
                    template_id = EXCLUDED.template_id,
                    organization_id = EXCLUDED.organization_id,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", mapping.id())
                .addValue("position_code", mapping.positionCode())
                .addValue("staff_level_id", mapping.staffLevelId())
                .addValue("role_id", mapping.roleId())
                .addValue("template_id", mapping.templateId())
                .addValue("organization_id", mapping.organizationId())
                .addValue("status", mapping.status())
                .addValue("created_by", mapping.createdBy())
                .addValue("created_at", timestampWithTimeZone(mapping.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(mapping.updatedAt())));
        return mapping;
    }

    @Override
    public void deletePositionRoleTemplate(String id) {
        jdbcTemplate.update("""
                DELETE FROM position_role_templates
                 WHERE id = :id
                """, Map.of("id", id));
    }

    @Override
    public UserPositionAssignment saveUserPositionAssignment(UserPositionAssignment assignment) {
        jdbcTemplate.update("""
                INSERT INTO user_position_assignments (
                    user_id, position_code, staff_level_id, organization_id, merchant_id, assigned_by, assigned_at
                )
                VALUES (
                    :user_id, :position_code, :staff_level_id, :organization_id, :merchant_id, :assigned_by, :assigned_at
                )
                ON CONFLICT (user_id) DO UPDATE SET
                    position_code = EXCLUDED.position_code,
                    staff_level_id = EXCLUDED.staff_level_id,
                    organization_id = EXCLUDED.organization_id,
                    merchant_id = EXCLUDED.merchant_id,
                    assigned_by = EXCLUDED.assigned_by,
                    assigned_at = EXCLUDED.assigned_at
                """, new MapSqlParameterSource()
                .addValue("user_id", assignment.userId())
                .addValue("position_code", assignment.positionCode())
                .addValue("staff_level_id", assignment.staffLevelId())
                .addValue("organization_id", assignment.organizationId())
                .addValue("merchant_id", assignment.merchantId())
                .addValue("assigned_by", assignment.assignedBy())
                .addValue("assigned_at", timestampWithTimeZone(assignment.assignedAt())));
        return assignment;
    }

    @Override
    public Optional<UserPositionAssignment> findUserPositionAssignment(String userId) {
        List<UserPositionAssignment> result = jdbcTemplate.query("""
                SELECT user_id, position_code, staff_level_id, organization_id, merchant_id, assigned_by, assigned_at
                  FROM user_position_assignments
                 WHERE user_id = :user_id
                """, Map.of("user_id", userId), this::mapUserPositionAssignment);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<PermissionTemplateApplication> applyTemplate(
            String templateId,
            List<String> targetUserIds,
            Instant validFrom,
            Instant validUntil,
            boolean singleUse,
            String appliedBy
    ) {
        Instant appliedAt = Instant.now();
        List<PermissionTemplateApplication> applications = targetUserIds.stream()
                .map(userId -> new PermissionTemplateApplication(
                        "pta_" + UUID.randomUUID().toString().replace("-", ""),
                        templateId,
                        "user",
                        userId,
                        validFrom,
                        validUntil,
                        singleUse,
                        false,
                        "active",
                        appliedBy,
                        appliedAt,
                        null,
                        null
                ))
                .toList();
        jdbcTemplate.batchUpdate("""
                INSERT INTO permission_template_applications (
                    id, template_id, target_type, target_id, valid_from, valid_until, single_use,
                    consumed, status, applied_by, applied_at, revoked_by, revoked_at
                )
                VALUES (
                    :id, :template_id, :target_type, :target_id, :valid_from, :valid_until, :single_use,
                    :consumed, :status, :applied_by, :applied_at, :revoked_by, :revoked_at
                )
                """, applications.stream()
                .map(this::applicationParameters)
                .toArray(MapSqlParameterSource[]::new));
        return applications;
    }

    @Override
    public TemporaryGrant createTemporaryGrant(TemporaryGrant temporaryGrant) {
        List<String> permissionCodes = temporaryGrant.permissionCodes() == null
                ? List.of()
                : temporaryGrant.permissionCodes();
        String grantGroupId = temporaryGrant.id();
        for (int index = 0; index < permissionCodes.size(); index++) {
            String id = index == 0 ? temporaryGrant.id() : UUID.randomUUID().toString();
            jdbcTemplate.update("""
                    INSERT INTO temporary_permission_grants (
                        id, grant_group_id, subject_type, subject_id, permission_code, reason, granted_at, expires_at,
                        granted_by, status, valid_from, single_use, consumed
                    )
                    VALUES (
                        :id, :grant_group_id, 'user', :subject_id, :permission_code, :reason, :granted_at, :expires_at,
                        :granted_by, :status, :valid_from, :single_use, :consumed
                    )
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.fromString(id))
                    .addValue("grant_group_id", grantGroupId)
                    .addValue("subject_id", temporaryGrant.targetUserId())
                    .addValue("permission_code", permissionCodes.get(index))
                    .addValue("reason", temporaryGrant.reason())
                    .addValue("granted_at", timestampWithTimeZone(temporaryGrant.grantedAt()))
                    .addValue("expires_at", timestampWithTimeZone(temporaryGrant.validUntil()))
                    .addValue("granted_by", temporaryGrant.grantedBy())
                    .addValue("status", temporaryGrant.status())
                    .addValue("valid_from", timestampWithTimeZone(temporaryGrant.validFrom()))
                    .addValue("single_use", temporaryGrant.singleUse())
                    .addValue("consumed", temporaryGrant.consumed()));
        }
        return temporaryGrant;
    }

    @Override
    public TemporaryGrant revokeTemporaryGrant(String id, String revokedBy, Instant revokedAt) {
        TemporaryGrant existing = findTemporaryGrant(id)
                .orElseThrow(() -> com.complex.platform.common.api.ApiException.notFound(
                        "TEMPORARY_GRANT_NOT_FOUND",
                        "temporary grant not found"
                ));
        jdbcTemplate.update("""
                UPDATE temporary_permission_grants
                   SET status = 'revoked',
                       revoked_by = :revoked_by,
                       revoked_at = :revoked_at
                 WHERE grant_group_id = :grant_group_id
                   AND status = 'active'
                """, new MapSqlParameterSource()
                .addValue("grant_group_id", existing.id())
                .addValue("revoked_by", revokedBy)
                .addValue("revoked_at", timestampWithTimeZone(revokedAt)));
        return new TemporaryGrant(
                existing.id(),
                existing.targetUserId(),
                existing.permissionCodes(),
                existing.validFrom(),
                existing.validUntil(),
                existing.singleUse(),
                existing.consumed(),
                "revoked",
                existing.reason(),
                existing.grantedBy(),
                existing.grantedAt(),
                revokedBy,
                revokedAt
        );
    }

    @Override
    public AccessGrantRequestRecord saveGrantRequest(AccessGrantRequestRecord request) {
        jdbcTemplate.update("""
                INSERT INTO access_grant_requests (
                    id, request_type, target_user_id, target_role_id, template_id, position_code,
                    staff_level_id, base_salary_amount_cent, salary_currency, salary_detail,
                    permission_codes, valid_from, expires_at, single_use, status, review_id,
                    reason, requested_by, decided_by, decided_at, project_id, merchant_id,
                    created_at, updated_at
                )
                VALUES (
                    :id, :request_type, :target_user_id, :target_role_id, :template_id, :position_code,
                    :staff_level_id, :base_salary_amount_cent, :salary_currency, :salary_detail,
                    :permission_codes, :valid_from, :expires_at, :single_use, :status, :review_id,
                    :reason, :requested_by, :decided_by, :decided_at, :project_id, :merchant_id,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    request_type = EXCLUDED.request_type,
                    target_user_id = EXCLUDED.target_user_id,
                    target_role_id = EXCLUDED.target_role_id,
                    template_id = EXCLUDED.template_id,
                    position_code = EXCLUDED.position_code,
                    staff_level_id = EXCLUDED.staff_level_id,
                    base_salary_amount_cent = EXCLUDED.base_salary_amount_cent,
                    salary_currency = EXCLUDED.salary_currency,
                    salary_detail = EXCLUDED.salary_detail,
                    permission_codes = EXCLUDED.permission_codes,
                    valid_from = EXCLUDED.valid_from,
                    expires_at = EXCLUDED.expires_at,
                    single_use = EXCLUDED.single_use,
                    status = EXCLUDED.status,
                    review_id = EXCLUDED.review_id,
                    reason = EXCLUDED.reason,
                    decided_by = EXCLUDED.decided_by,
                    decided_at = EXCLUDED.decided_at,
                    project_id = EXCLUDED.project_id,
                    merchant_id = EXCLUDED.merchant_id,
                    updated_at = EXCLUDED.updated_at
                """, grantRequestParameters(request));
        return request;
    }

    @Override
    public Optional<AccessGrantRequestRecord> findGrantRequestById(String id) {
        List<AccessGrantRequestRecord> result = jdbcTemplate.query("""
                SELECT id, request_type, target_user_id, target_role_id, template_id, position_code,
                       staff_level_id, base_salary_amount_cent, salary_currency, salary_detail,
                       permission_codes, valid_from, expires_at, single_use, status, review_id,
                       reason, requested_by, decided_by, decided_at, project_id, merchant_id,
                       created_at, updated_at
                  FROM access_grant_requests
                 WHERE id = :id
                """, Map.of("id", id), this::mapGrantRequest);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<AccessGrantRequestRecord> listGrantRequests(Instant createdAtFrom) {
        if (createdAtFrom == null) {
            return jdbcTemplate.query("""
                    SELECT id, request_type, target_user_id, target_role_id, template_id, position_code,
                           staff_level_id, base_salary_amount_cent, salary_currency, salary_detail,
                           permission_codes, valid_from, expires_at, single_use, status, review_id,
                           reason, requested_by, decided_by, decided_at, project_id, merchant_id,
                           created_at, updated_at
                      FROM access_grant_requests
                     ORDER BY created_at DESC, id
                    """, this::mapGrantRequest);
        }
        return jdbcTemplate.query("""
                SELECT id, request_type, target_user_id, target_role_id, template_id, position_code,
                       staff_level_id, base_salary_amount_cent, salary_currency, salary_detail,
                       permission_codes, valid_from, expires_at, single_use, status, review_id,
                       reason, requested_by, decided_by, decided_at, project_id, merchant_id,
                       created_at, updated_at
                  FROM access_grant_requests
                 WHERE created_at >= :created_at_from
                 ORDER BY created_at DESC, id
                """, new MapSqlParameterSource()
                .addValue("created_at_from", timestampWithTimeZone(createdAtFrom)), this::mapGrantRequest);
    }

    @Override
    public List<PermissionEffect> findPermissionEffects(String userId, Instant checkedAt) {
        List<PermissionEffect> effects = new ArrayList<>();
        effects.addAll(jdbcTemplate.query("""
                SELECT ur.user_id, rp.permission_code, 'grant' AS effect, 'ROLE' AS source,
                       r.id AS source_id, NULL::timestamptz AS valid_from, NULL::timestamptz AS valid_until,
                       FALSE AS single_use, FALSE AS consumed
                  FROM user_roles ur
                  JOIN roles r ON r.id = ur.role_id AND r.status = 'active'
                  JOIN role_permissions rp ON rp.role_id = r.id
                 WHERE ur.user_id = :user_id
                """, Map.of("user_id", userId), this::mapPermissionEffect));
        effects.addAll(jdbcTemplate.query("""
                SELECT pta.target_id AS user_id, pti.permission_code, pti.effect, 'TEMPLATE_APPLICATION' AS source,
                       pta.id AS source_id, COALESCE(pta.valid_from, pt.valid_from) AS valid_from,
                       COALESCE(pta.valid_until, pt.valid_to) AS valid_until,
                       (pta.single_use OR pt.single_use) AS single_use, pta.consumed AS consumed
                  FROM permission_template_applications pta
                  JOIN permission_templates pt ON pt.id = pta.template_id AND pt.status = 'active'
                  JOIN permission_template_items pti ON pti.template_id = pt.id
                 WHERE pta.target_type = 'user'
                   AND pta.target_id = :user_id
                   AND pta.status = 'active'
                """, Map.of("user_id", userId), this::mapPermissionEffect));
        effects.addAll(jdbcTemplate.query("""
                SELECT subject_id AS user_id, permission_code, 'grant' AS effect, 'TEMPORARY_GRANT' AS source,
                       id::text AS source_id, valid_from, expires_at AS valid_until,
                       single_use, consumed
                  FROM temporary_permission_grants
                 WHERE subject_type = 'user'
                   AND subject_id = :user_id
                   AND status = 'active'
                """, Map.of("user_id", userId), this::mapPermissionEffect));
        effects.addAll(jdbcTemplate.query("""
                SELECT user_id, permission_code, effect, 'USER_OVERRIDE' AS source,
                       user_id || ':' || permission_code || ':' || effect AS source_id,
                       NULL::timestamptz AS valid_from, NULL::timestamptz AS valid_until,
                       FALSE AS single_use, FALSE AS consumed
                  FROM user_permission_overrides
                 WHERE user_id = :user_id
                """, Map.of("user_id", userId), this::mapPermissionEffect));
        return effects;
    }

    @Override
    public boolean markSingleUsePermissionConsumed(
            String sourceId,
            String permissionCode,
            String userId,
            Instant consumedAt
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("source_id", sourceId)
                .addValue("permission_code", permissionCode)
                .addValue("user_id", userId)
                .addValue("consumed_at", timestampWithTimeZone(consumedAt))
                .addValue("usage_log_id", UUID.randomUUID());
        // AND consumed = FALSE is the atomic guard against TOCTOU races.
        // If another concurrent request already consumed this grant, UPDATE returns 0 rows.
        int templateRows = jdbcTemplate.update("""
                UPDATE permission_template_applications
                   SET consumed = TRUE,
                       consumed_at = :consumed_at
                 WHERE id = :source_id
                   AND target_id = :user_id
                   AND single_use = TRUE
                   AND consumed = FALSE
                """, parameters);
        int grantRows = jdbcTemplate.update("""
                UPDATE temporary_permission_grants
                   SET consumed = TRUE,
                       consumed_at = :consumed_at
                 WHERE id::text = :source_id
                   AND subject_id = :user_id
                   AND single_use = TRUE
                   AND consumed = FALSE
                """, parameters);
        boolean consumed = templateRows > 0 || grantRows > 0;
        if (consumed) {
            jdbcTemplate.update("""
                    INSERT INTO permission_template_usage_logs (
                        id, application_id, user_id, permission_code, result, reason, occurred_at
                    )
                    VALUES (
                        :usage_log_id, :source_id, :user_id, :permission_code, 'consumed', 'single_use', :consumed_at
                    )
                    """, parameters);
        }
        return consumed;
    }

    private void insertTemplateItems(List<PermissionTemplateItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO permission_template_items (
                    template_id, permission_code, effect, data_scope, sensitive
                )
                VALUES (
                    :template_id, :permission_code, :effect, :data_scope, :sensitive
                )
                """, items.stream()
                .map(item -> new MapSqlParameterSource()
                        .addValue("template_id", item.templateId())
                        .addValue("permission_code", item.permissionCode())
                        .addValue("effect", effectCode(item.effect()))
                        .addValue("data_scope", item.dataScope())
                        .addValue("sensitive", item.sensitive()))
                .toArray(MapSqlParameterSource[]::new));
    }

    private MapSqlParameterSource templateParameters(PermissionTemplate template) {
        return new MapSqlParameterSource()
                .addValue("id", template.id())
                .addValue("template_code", template.templateCode())
                .addValue("template_name", template.templateName())
                .addValue("description", template.description())
                .addValue("template_type", template.templateType())
                .addValue("source_template_id", template.sourceTemplateId())
                .addValue("owner_scope_type", template.ownerScopeType())
                .addValue("owner_scope_id", template.ownerScopeId())
                .addValue("usage_mode", template.usageMode())
                .addValue("valid_from", timestampWithTimeZone(template.validFrom()))
                .addValue("valid_to", timestampWithTimeZone(template.validUntil()))
                .addValue("single_use", template.singleUse())
                .addValue("status", template.status())
                .addValue("reason", template.reason())
                .addValue("approved_by", template.approvedBy())
                .addValue("approved_at", timestampWithTimeZone(template.approvedAt()))
                .addValue("created_at", timestampWithTimeZone(template.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(template.updatedAt()));
    }

    private MapSqlParameterSource applicationParameters(PermissionTemplateApplication application) {
        return new MapSqlParameterSource()
                .addValue("id", application.id())
                .addValue("template_id", application.templateId())
                .addValue("target_type", application.targetType())
                .addValue("target_id", application.targetId())
                .addValue("valid_from", timestampWithTimeZone(application.validFrom()))
                .addValue("valid_until", timestampWithTimeZone(application.validUntil()))
                .addValue("single_use", application.singleUse())
                .addValue("consumed", application.consumed())
                .addValue("status", application.status())
                .addValue("applied_by", application.appliedBy())
                .addValue("applied_at", timestampWithTimeZone(application.appliedAt()))
                .addValue("revoked_by", application.revokedBy())
                .addValue("revoked_at", timestampWithTimeZone(application.revokedAt()));
    }

    private MapSqlParameterSource grantRequestParameters(AccessGrantRequestRecord request) {
        return new MapSqlParameterSource()
                .addValue("id", request.id())
                .addValue("request_type", request.requestType())
                .addValue("target_user_id", request.targetUserId())
                .addValue("target_role_id", request.targetRoleId())
                .addValue("template_id", request.templateId())
                .addValue("position_code", request.positionCode())
                .addValue("staff_level_id", request.staffLevelId())
                .addValue("base_salary_amount_cent", request.baseSalaryAmountCent())
                .addValue("salary_currency", request.salaryCurrency())
                .addValue("salary_detail", request.salaryDetail())
                .addValue("permission_codes", encodeEvidenceIds(request.permissionCodes()))
                .addValue("valid_from", timestampWithTimeZone(request.validFrom()))
                .addValue("expires_at", timestampWithTimeZone(request.expiresAt()))
                .addValue("single_use", request.singleUse())
                .addValue("status", request.status())
                .addValue("review_id", request.reviewId())
                .addValue("reason", request.reason())
                .addValue("requested_by", request.requestedBy())
                .addValue("decided_by", request.decidedBy())
                .addValue("decided_at", timestampWithTimeZone(request.decidedAt()))
                .addValue("project_id", request.projectId())
                .addValue("merchant_id", request.merchantId())
                .addValue("created_at", timestampWithTimeZone(request.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(request.updatedAt()));
    }

    private PermissionDefinition mapPermission(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PermissionDefinition(
                resultSet.getString("permission_code"),
                resultSet.getString("permission_name"),
                resultSet.getString("domain"),
                resultSet.getString("risk_level"),
                resultSet.getBoolean("requires_approval"),
                resultSet.getString("status")
        );
    }

    private PermissionTemplateItem mapTemplateItem(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PermissionTemplateItem(
                resultSet.getString("template_id"),
                resultSet.getString("permission_code"),
                effect(resultSet.getString("effect")),
                resultSet.getString("data_scope"),
                resultSet.getBoolean("sensitive")
        );
    }

    private PermissionTemplate mapTemplate(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PermissionTemplate(
                resultSet.getString("id"),
                resultSet.getString("template_code"),
                resultSet.getString("template_name"),
                resultSet.getString("description"),
                resultSet.getString("template_type"),
                resultSet.getString("source_template_id"),
                resultSet.getString("owner_scope_type"),
                resultSet.getString("owner_scope_id"),
                resultSet.getString("usage_mode"),
                nullableInstant(resultSet, "valid_from"),
                nullableInstant(resultSet, "valid_to"),
                resultSet.getBoolean("single_use"),
                resultSet.getString("status"),
                resultSet.getString("reason"),
                resultSet.getString("approved_by"),
                nullableInstant(resultSet, "approved_at"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                readPermissionCodes(resultSet)
        );
    }

    private static List<String> readPermissionCodes(ResultSet resultSet) throws SQLException {
        java.sql.Array array = resultSet.getArray("permission_codes");
        if (array == null) {
            return List.of();
        }
        String[] codes = (String[]) array.getArray();
        return codes == null ? List.of() : java.util.Arrays.asList(codes);
    }

    private AccessUser mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AccessUser(
                resultSet.getString("id"),
                resultSet.getString("username"),
                resultSet.getString("account_type"),
                resultSet.getString("organization_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                resultSet.getString("phone_hash"),
                resultSet.getString("created_via"),
                resultSet.getString("consent_reference"),
                decodeEvidenceIds(resultSet.getString("evidence_attachment_ids")),
                resultSet.getString("operator_note")
        );
    }

    private String encodeEvidenceIds(List<String> evidenceAttachmentIds) {
        if (evidenceAttachmentIds == null || evidenceAttachmentIds.isEmpty()) {
            return null;
        }
        return String.join(",", evidenceAttachmentIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList());
    }

    private List<String> decodeEvidenceIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String item : value.split(",")) {
            if (!item.isBlank()) {
                result.add(item);
            }
        }
        return result;
    }

    private StaffLevelRecord mapStaffLevel(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StaffLevelRecord(
                resultSet.getString("id"),
                resultSet.getString("main_level"),
                resultSet.getString("sub_level"),
                resultSet.getInt("level_rank"),
                resultSet.getString("salary_policy_summary"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at")
        );
    }

    private StaffLevelAssignment mapStaffLevelAssignment(ResultSet resultSet, int rowNumber) throws SQLException {
        long salary = resultSet.getLong("base_salary_amount_cent");
        Long baseSalary = resultSet.wasNull() ? null : salary;
        return new StaffLevelAssignment(
                resultSet.getString("user_id"),
                resultSet.getString("staff_level_id"),
                baseSalary,
                resultSet.getString("salary_currency"),
                resultSet.getString("salary_detail"),
                resultSet.getString("organization_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("assigned_by"),
                nullableInstant(resultSet, "assigned_at")
        );
    }

    private PositionRoleTemplate mapPositionRoleTemplate(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PositionRoleTemplate(
                resultSet.getString("id"),
                resultSet.getString("position_code"),
                resultSet.getString("staff_level_id"),
                resultSet.getString("role_id"),
                resultSet.getString("template_id"),
                resultSet.getString("organization_id"),
                resultSet.getString("status"),
                resultSet.getString("created_by"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private UserPositionAssignment mapUserPositionAssignment(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UserPositionAssignment(
                resultSet.getString("user_id"),
                resultSet.getString("position_code"),
                resultSet.getString("staff_level_id"),
                resultSet.getString("organization_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("assigned_by"),
                nullableInstant(resultSet, "assigned_at")
        );
    }

    private AccessGrantRequestRecord mapGrantRequest(ResultSet resultSet, int rowNumber) throws SQLException {
        long salary = resultSet.getLong("base_salary_amount_cent");
        Long baseSalary = resultSet.wasNull() ? null : salary;
        return new AccessGrantRequestRecord(
                resultSet.getString("id"),
                resultSet.getString("request_type"),
                resultSet.getString("target_user_id"),
                resultSet.getString("target_role_id"),
                resultSet.getString("template_id"),
                resultSet.getString("position_code"),
                resultSet.getString("staff_level_id"),
                baseSalary,
                resultSet.getString("salary_currency"),
                resultSet.getString("salary_detail"),
                decodeEvidenceIds(resultSet.getString("permission_codes")),
                nullableInstant(resultSet, "valid_from"),
                nullableInstant(resultSet, "expires_at"),
                resultSet.getBoolean("single_use"),
                resultSet.getString("status"),
                resultSet.getString("review_id"),
                resultSet.getString("reason"),
                resultSet.getString("requested_by"),
                resultSet.getString("decided_by"),
                nullableInstant(resultSet, "decided_at"),
                resultSet.getString("project_id"),
                resultSet.getString("merchant_id"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private RoleRecord mapRole(ResultSet resultSet, int rowNumber) throws SQLException {
        String roleId = resultSet.getString("id");
        return new RoleRecord(
                roleId,
                resultSet.getString("role_name"),
                resultSet.getString("description"),
                resultSet.getString("data_scope"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                findRolePermissionCodes(roleId),
                resultSet.getString("organization_id")
        );
    }

    private PermissionEffect mapPermissionEffect(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PermissionEffect(
                resultSet.getString("user_id"),
                resultSet.getString("permission_code"),
                effect(resultSet.getString("effect")),
                Source.valueOf(resultSet.getString("source")),
                resultSet.getString("source_id"),
                nullableInstant(resultSet, "valid_from"),
                nullableInstant(resultSet, "valid_until"),
                resultSet.getBoolean("single_use"),
                resultSet.getBoolean("consumed")
        );
    }

    @Override
    public Optional<TemporaryGrant> findTemporaryGrantById(String id) {
        return findTemporaryGrant(id);
    }

    private Optional<TemporaryGrant> findTemporaryGrant(String id) {
        List<TemporaryGrantRow> rows = jdbcTemplate.query("""
                WITH requested_group AS (
                    SELECT grant_group_id
                      FROM temporary_permission_grants
                     WHERE id = :id
                    UNION
                    SELECT :grant_group_id
                     WHERE EXISTS (
                         SELECT 1
                           FROM temporary_permission_grants
                          WHERE grant_group_id = :grant_group_id
                     )
                )
                SELECT id::text AS id, grant_group_id, subject_id, permission_code, reason, granted_at,
                       expires_at, granted_by, status, valid_from, single_use, consumed, revoked_by, revoked_at
                  FROM temporary_permission_grants
                 WHERE grant_group_id = (
                     SELECT grant_group_id
                       FROM requested_group
                      LIMIT 1
                 )
                 ORDER BY permission_code, id
                """, new MapSqlParameterSource()
                .addValue("id", UUID.fromString(id))
                .addValue("grant_group_id", id), this::mapTemporaryGrantRow);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        TemporaryGrantRow first = rows.get(0);
        String status = rows.stream().anyMatch(row -> "active".equals(row.status()))
                ? "active"
                : first.status();
        return Optional.of(new TemporaryGrant(
                first.grantGroupId(),
                first.subjectId(),
                rows.stream()
                        .map(TemporaryGrantRow::permissionCode)
                        .distinct()
                        .toList(),
                first.validFrom(),
                first.expiresAt(),
                first.singleUse(),
                rows.stream().allMatch(TemporaryGrantRow::consumed),
                status,
                first.reason(),
                first.grantedBy(),
                first.grantedAt(),
                first.revokedBy(),
                first.revokedAt()
        ));
    }

    private TemporaryGrantRow mapTemporaryGrantRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TemporaryGrantRow(
                resultSet.getString("id"),
                resultSet.getString("grant_group_id"),
                resultSet.getString("subject_id"),
                resultSet.getString("permission_code"),
                resultSet.getString("reason"),
                nullableInstant(resultSet, "granted_at"),
                nullableInstant(resultSet, "expires_at"),
                resultSet.getString("granted_by"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "valid_from"),
                resultSet.getBoolean("single_use"),
                resultSet.getBoolean("consumed"),
                resultSet.getString("revoked_by"),
                nullableInstant(resultSet, "revoked_at")
        );
    }

    private record TemporaryGrantRow(
            String id,
            String grantGroupId,
            String subjectId,
            String permissionCode,
            String reason,
            Instant grantedAt,
            Instant expiresAt,
            String grantedBy,
            String status,
            Instant validFrom,
            boolean singleUse,
            boolean consumed,
            String revokedBy,
            Instant revokedAt
    ) {
    }

    private List<String> findRolePermissionCodes(String roleId) {
        return jdbcTemplate.query("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = :role_id
                 ORDER BY permission_code
                """, Map.of("role_id", roleId), (resultSet, rowNumber) -> resultSet.getString("permission_code"));
    }

    private AccessRepository.Effect effect(String effect) {
        return AccessRepository.Effect.valueOf(effect.toUpperCase(java.util.Locale.ROOT));
    }

    private String effectCode(AccessRepository.Effect effect) {
        return effect.name().toLowerCase(java.util.Locale.ROOT);
    }


}
