package com.complex.platform.partner;

import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcPartnerRepository implements PartnerRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcPartnerRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private ExternalPartnerOrgRecord mapOrg(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new ExternalPartnerOrgRecord(
                rs.getString("id"), rs.getString("project_id"), rs.getString("org_name"),
                rs.getString("org_category"), rs.getString("linked_supplier_id"),
                rs.getInt("max_subordinates"),
                rs.getTimestamp("valid_until") == null ? null : rs.getTimestamp("valid_until").toInstant(),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private PartnerSubordinateRecord mapSub(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new PartnerSubordinateRecord(
                rs.getString("id"), rs.getString("username"), rs.getString("partner_org_id"),
                rs.getString("account_type"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant());
    }

    @Override
    public ExternalPartnerOrgRecord saveOrg(ExternalPartnerOrgRecord o) {
        jdbcTemplate.update("""
                INSERT INTO external_partner_orgs
                    (id, project_id, org_name, org_category, linked_supplier_id, max_subordinates, valid_until, status, created_at, updated_at)
                VALUES (:id, :project_id, :org_name, :org_category, :linked_supplier_id, :max_subordinates, :valid_until, :status, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id, org_name = EXCLUDED.org_name, org_category = EXCLUDED.org_category,
                    linked_supplier_id = EXCLUDED.linked_supplier_id, max_subordinates = EXCLUDED.max_subordinates,
                    valid_until = EXCLUDED.valid_until, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", o.id()).addValue("project_id", o.projectId()).addValue("org_name", o.orgName())
                .addValue("org_category", o.orgCategory()).addValue("linked_supplier_id", o.linkedSupplierId())
                .addValue("max_subordinates", o.maxSubordinates())
                .addValue("valid_until", o.validUntil() == null ? null : timestampWithTimeZone(o.validUntil()))
                .addValue("status", o.status())
                .addValue("created_at", timestampWithTimeZone(o.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(o.updatedAt())));
        return o;
    }

    @Override
    public List<ExternalPartnerOrgRecord> listOrgs(String projectScope) {
        // 3.2 跨项目读隔离：项目员工只见本项目机构；null（平台管理员/全局平台员工）全量。
        return jdbcTemplate.query(
                "SELECT * FROM external_partner_orgs"
                        + (projectScope != null ? " WHERE project_id = :project_id" : "")
                        + " ORDER BY created_at DESC, id LIMIT 500",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("project_id", projectScope),
                this::mapOrg);
    }

    @Override
    public Optional<ExternalPartnerOrgRecord> findOrgById(String id) {
        return jdbcTemplate.query("SELECT * FROM external_partner_orgs WHERE id = :id",
                Map.of("id", id), this::mapOrg).stream().findFirst();
    }

    @Override
    public Optional<String> findPartnerOrgIdForUser(String userId) {
        return jdbcTemplate.queryForList(
                "SELECT partner_org_id FROM access_users WHERE id = :id AND partner_org_id IS NOT NULL",
                Map.of("id", userId), String.class).stream().findFirst();
    }

    @Override
    public java.util.Optional<String> findAccessUserAccountType(String userId) {
        return java.util.Optional.ofNullable(jdbcTemplate.query(
                "SELECT account_type FROM access_users WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource().addValue("id", userId),
                rs -> rs.next() ? rs.getString(1) : null));
    }

    @Override
    public int updateUserPartnerOrg(String userId, String partnerOrgId) {
        return jdbcTemplate.update(
                "UPDATE access_users SET partner_org_id = :org WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                        .addValue("org", partnerOrgId)
                        .addValue("id", userId));
    }

    @Override
    public List<PartnerSubordinateRecord> listSubordinates(String partnerOrgId) {
        return jdbcTemplate.query("""
                SELECT id, username, partner_org_id, account_type, status, created_at
                  FROM access_users WHERE partner_org_id = :org ORDER BY created_at DESC, id
                """, Map.of("org", partnerOrgId), this::mapSub);
    }

    @Override
    public Optional<PartnerSubordinateRecord> findSubordinate(String userId) {
        return jdbcTemplate.query("""
                SELECT id, username, partner_org_id, account_type, status, created_at
                  FROM access_users WHERE id = :id AND partner_org_id IS NOT NULL
                """, Map.of("id", userId), this::mapSub).stream().findFirst();
    }

    @Override
    public long countActiveSubordinates(String partnerOrgId) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM access_users WHERE partner_org_id = :org AND status = 'active'",
                Map.of("org", partnerOrgId), Long.class);
        return c == null ? 0 : c;
    }

    @Override
    public PartnerSubordinateRecord createSubordinate(PartnerSubordinateRecord s, String roleId) {
        jdbcTemplate.update("""
                INSERT INTO access_users (id, username, account_type, status, created_at, partner_org_id)
                VALUES (:id, :username, :account_type, :status, :created_at, :partner_org_id)
                """, new MapSqlParameterSource()
                .addValue("id", s.id()).addValue("username", s.username())
                .addValue("account_type", s.accountType()).addValue("status", s.status())
                .addValue("created_at", timestampWithTimeZone(s.createdAt()))
                .addValue("partner_org_id", s.partnerOrgId()));
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (:u, :r) ON CONFLICT (user_id, role_id) DO NOTHING",
                Map.of("u", s.id(), "r", roleId));
        return s;
    }

    @Override
    public void disableSubordinate(String userId) {
        jdbcTemplate.update("UPDATE access_users SET status = 'disabled' WHERE id = :id", Map.of("id", userId));
    }

    @Override
    public int disableExpiredOrgsAndSubordinates(Instant now) {
        var ts = timestampWithTimeZone(now);
        jdbcTemplate.update("UPDATE external_partner_orgs SET status = 'expired', updated_at = :now WHERE valid_until IS NOT NULL AND valid_until < :now AND status = 'active'",
                new MapSqlParameterSource().addValue("now", ts));
        return jdbcTemplate.update("""
                UPDATE access_users SET status = 'disabled'
                 WHERE status = 'active' AND partner_org_id IN (SELECT id FROM external_partner_orgs WHERE status = 'expired')
                """, new MapSqlParameterSource());
    }
}
