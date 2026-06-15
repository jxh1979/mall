package com.complex.platform.access;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

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
                SELECT id, template_code, template_name, description, template_type, source_template_id,
                       owner_scope_type, owner_scope_id, usage_mode, valid_from, valid_to, single_use,
                       status, reason, approved_by, approved_at, created_at, updated_at
                  FROM permission_templates
                 ORDER BY created_at DESC, id
                """, this::mapTemplate);
    }

    @Override
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
                    id, username, account_type, organization_id, merchant_id, status, created_at
                )
                VALUES (
                    :id, :username, :account_type, :organization_id, :merchant_id, :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    username = EXCLUDED.username,
                    account_type = EXCLUDED.account_type,
                    organization_id = EXCLUDED.organization_id,
                    merchant_id = EXCLUDED.merchant_id,
                    status = EXCLUDED.status
                """, new MapSqlParameterSource()
                .addValue("id", user.id())
                .addValue("username", user.username())
                .addValue("account_type", user.accountType())
                .addValue("organization_id", user.organizationId())
                .addValue("merchant_id", user.merchantId())
                .addValue("status", user.status())
                .addValue("created_at", timestampWithTimeZone(user.createdAt())));
        return user;
    }

    @Override
    public List<AccessUser> listUsers() {
        return jdbcTemplate.query("""
                SELECT id, username, account_type, organization_id, merchant_id, status, created_at
                  FROM access_users
                 ORDER BY username
                """, this::mapUser);
    }

    @Override
    public List<RoleRecord> listRoles() {
        return jdbcTemplate.query("""
                SELECT id, role_name, description, data_scope, status, created_at
                  FROM roles
                 ORDER BY role_name
                """, this::mapRole);
    }

    @Override
    public RoleRecord createRole(RoleRecord role) {
        jdbcTemplate.update("""
                INSERT INTO roles (
                    id, role_name, description, data_scope, status, created_at
                )
                VALUES (
                    :id, :role_name, :description, :data_scope, :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    role_name = EXCLUDED.role_name,
                    description = EXCLUDED.description,
                    data_scope = EXCLUDED.data_scope,
                    status = EXCLUDED.status
                """, new MapSqlParameterSource()
                .addValue("id", role.id())
                .addValue("role_name", role.roleName())
                .addValue("description", role.description())
                .addValue("data_scope", role.dataScope())
                .addValue("status", role.status())
                .addValue("created_at", timestampWithTimeZone(role.createdAt())));
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
    public void markSingleUsePermissionConsumed(
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
        jdbcTemplate.update("""
                UPDATE permission_template_applications
                   SET consumed = TRUE,
                       consumed_at = :consumed_at
                 WHERE id = :source_id
                   AND target_id = :user_id
                   AND single_use = TRUE
                """, parameters);
        jdbcTemplate.update("""
                UPDATE temporary_permission_grants
                   SET consumed = TRUE,
                       consumed_at = :consumed_at
                 WHERE id::text = :source_id
                   AND subject_id = :user_id
                   AND single_use = TRUE
                """, parameters);
        jdbcTemplate.update("""
                INSERT INTO permission_template_usage_logs (
                    id, application_id, user_id, permission_code, result, reason, occurred_at
                )
                VALUES (
                    :usage_log_id, :source_id, :user_id, :permission_code, 'consumed', 'single_use', :consumed_at
                )
                """, parameters);
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
                nullableInstant(resultSet, "updated_at")
        );
    }

    private AccessUser mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AccessUser(
                resultSet.getString("id"),
                resultSet.getString("username"),
                resultSet.getString("account_type"),
                resultSet.getString("organization_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at")
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
                findRolePermissionCodes(roleId)
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

    private OffsetDateTime timestampWithTimeZone(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLException {
        OffsetDateTime value = resultSet.getObject(columnName, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
