package com.complex.platform.terminal;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcMdmRepository implements MdmRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcMdmRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MdmAppWhitelistRecord> listAppWhitelist() {
        return jdbcTemplate.query("""
                SELECT id, package_name, app_label, scope_type, scope_id, enabled,
                       reason, created_at, updated_at
                  FROM mdm_app_whitelist
                 ORDER BY updated_at DESC, id
                 LIMIT 1000
                """, this::mapApp);
    }

    @Override
    public Optional<MdmAppWhitelistRecord> findAppWhitelistById(String id) {
        return jdbcTemplate.query("""
                SELECT id, package_name, app_label, scope_type, scope_id, enabled,
                       reason, created_at, updated_at
                  FROM mdm_app_whitelist
                 WHERE id = :id
                """, Map.of("id", id), this::mapApp).stream().findFirst();
    }

    @Override
    public MdmAppWhitelistRecord saveAppWhitelist(MdmAppWhitelistRecord record) {
        jdbcTemplate.update("""
                INSERT INTO mdm_app_whitelist (
                    id, package_name, app_label, scope_type, scope_id, enabled,
                    reason, created_at, updated_at
                )
                VALUES (
                    :id, :package_name, :app_label, :scope_type, :scope_id, :enabled,
                    :reason, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    package_name = EXCLUDED.package_name,
                    app_label = EXCLUDED.app_label,
                    scope_type = EXCLUDED.scope_type,
                    scope_id = EXCLUDED.scope_id,
                    enabled = EXCLUDED.enabled,
                    reason = EXCLUDED.reason,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("package_name", record.packageName())
                .addValue("app_label", record.appLabel())
                .addValue("scope_type", record.scopeType())
                .addValue("scope_id", record.scopeId())
                .addValue("enabled", record.enabled())
                .addValue("reason", record.reason())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt())));
        return record;
    }

    @Override
    public boolean isAppWhitelisted(String packageName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM mdm_app_whitelist
                 WHERE package_name = :package_name
                   AND enabled = TRUE
                """, Map.of("package_name", packageName), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public List<MdmSiteWhitelistRecord> listSiteWhitelist() {
        return jdbcTemplate.query("""
                SELECT id, domain, description, scope_type, scope_id, enabled,
                       reason, created_at, updated_at
                  FROM mdm_site_whitelist
                 ORDER BY updated_at DESC, id
                 LIMIT 1000
                """, this::mapSite);
    }

    @Override
    public Optional<MdmSiteWhitelistRecord> findSiteWhitelistById(String id) {
        return jdbcTemplate.query("""
                SELECT id, domain, description, scope_type, scope_id, enabled,
                       reason, created_at, updated_at
                  FROM mdm_site_whitelist
                 WHERE id = :id
                """, Map.of("id", id), this::mapSite).stream().findFirst();
    }

    @Override
    public MdmSiteWhitelistRecord saveSiteWhitelist(MdmSiteWhitelistRecord record) {
        jdbcTemplate.update("""
                INSERT INTO mdm_site_whitelist (
                    id, domain, description, scope_type, scope_id, enabled,
                    reason, created_at, updated_at
                )
                VALUES (
                    :id, :domain, :description, :scope_type, :scope_id, :enabled,
                    :reason, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    domain = EXCLUDED.domain,
                    description = EXCLUDED.description,
                    scope_type = EXCLUDED.scope_type,
                    scope_id = EXCLUDED.scope_id,
                    enabled = EXCLUDED.enabled,
                    reason = EXCLUDED.reason,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("domain", record.domain())
                .addValue("description", record.description())
                .addValue("scope_type", record.scopeType())
                .addValue("scope_id", record.scopeId())
                .addValue("enabled", record.enabled())
                .addValue("reason", record.reason())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt())));
        return record;
    }

    @Override
    public boolean isSiteWhitelisted(String domain) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM mdm_site_whitelist
                 WHERE domain = :domain
                   AND enabled = TRUE
                """, Map.of("domain", domain), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public Optional<MdmDeviceManagementRecord> findDeviceManagement(String deviceId) {
        return jdbcTemplate.query("""
                SELECT device_id, terminal_type, status, management_status, policy_status,
                       last_policy_sync_at, enrolled_at
                  FROM terminal_devices
                 WHERE device_id = :device_id
                """, Map.of("device_id", deviceId), this::mapManagement).stream().findFirst();
    }

    @Override
    public void updateDeviceManagement(
            String deviceId,
            String managementStatus,
            String policyStatus,
            Instant lastPolicySyncAt,
            Instant enrolledAt
    ) {
        jdbcTemplate.update("""
                UPDATE terminal_devices
                   SET management_status = :management_status,
                       policy_status = :policy_status,
                       last_policy_sync_at = :last_policy_sync_at,
                       enrolled_at = COALESCE(enrolled_at, :enrolled_at),
                       updated_at = now()
                 WHERE device_id = :device_id
                """, new MapSqlParameterSource()
                .addValue("device_id", deviceId)
                .addValue("management_status", managementStatus)
                .addValue("policy_status", policyStatus)
                .addValue("last_policy_sync_at", timestampWithTimeZone(lastPolicySyncAt))
                .addValue("enrolled_at", timestampWithTimeZone(enrolledAt)));
    }

    @Override
    public MdmPolicyViolationRecord saveViolation(MdmPolicyViolationRecord record) {
        jdbcTemplate.update("""
                INSERT INTO mdm_policy_violations (
                    id, device_id, violation_type, detail, occurred_at, handled, handled_note,
                    disposition, blocked_at, created_at
                )
                VALUES (
                    :id, :device_id, :violation_type, :detail, :occurred_at, :handled, :handled_note,
                    :disposition, :blocked_at, :created_at
                )
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("device_id", record.deviceId())
                .addValue("violation_type", record.violationType())
                .addValue("detail", record.detail())
                .addValue("occurred_at", timestampWithTimeZone(record.occurredAt()))
                .addValue("handled", record.handled())
                .addValue("handled_note", record.handledNote())
                .addValue("disposition", record.disposition())
                .addValue("blocked_at", timestampWithTimeZone(record.blockedAt()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt())));
        return record;
    }

    @Override
    public Optional<MdmPolicyViolationRecord> findViolationById(String id) {
        return jdbcTemplate.query("""
                SELECT id, device_id, violation_type, detail, occurred_at, handled, handled_note,
                       disposition, blocked_at, created_at
                  FROM mdm_policy_violations
                 WHERE id = :id
                """, Map.of("id", id), this::mapViolation).stream().findFirst();
    }

    @Override
    public void markViolationHandled(String id, String disposition, String handledNote, Instant blockedAt) {
        jdbcTemplate.update("""
                UPDATE mdm_policy_violations
                   SET handled = TRUE,
                       disposition = :disposition,
                       handled_note = :handled_note,
                       blocked_at = :blocked_at
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("disposition", disposition)
                .addValue("handled_note", handledNote)
                .addValue("blocked_at", timestampWithTimeZone(blockedAt)));
    }

    @Override
    public List<MdmPolicyViolationRecord> listViolations(String deviceId) {
        String sql = """
                SELECT id, device_id, violation_type, detail, occurred_at, handled, handled_note,
                       disposition, blocked_at, created_at
                  FROM mdm_policy_violations
                """
                + (deviceId == null ? "" : " WHERE device_id = :device_id")
                + " ORDER BY occurred_at DESC, id LIMIT 1000";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (deviceId != null) {
            params.addValue("device_id", deviceId);
        }
        return jdbcTemplate.query(sql, params, this::mapViolation);
    }

    private MdmAppWhitelistRecord mapApp(ResultSet rs, int rowNumber) throws SQLException {
        return new MdmAppWhitelistRecord(
                rs.getString("id"),
                rs.getString("package_name"),
                rs.getString("app_label"),
                rs.getString("scope_type"),
                rs.getString("scope_id"),
                rs.getBoolean("enabled"),
                rs.getString("reason"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at")
        );
    }

    private MdmSiteWhitelistRecord mapSite(ResultSet rs, int rowNumber) throws SQLException {
        return new MdmSiteWhitelistRecord(
                rs.getString("id"),
                rs.getString("domain"),
                rs.getString("description"),
                rs.getString("scope_type"),
                rs.getString("scope_id"),
                rs.getBoolean("enabled"),
                rs.getString("reason"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at")
        );
    }

    private MdmDeviceManagementRecord mapManagement(ResultSet rs, int rowNumber) throws SQLException {
        return new MdmDeviceManagementRecord(
                rs.getString("device_id"),
                rs.getString("terminal_type"),
                rs.getString("status"),
                rs.getString("management_status"),
                rs.getString("policy_status"),
                nullableInstant(rs, "last_policy_sync_at"),
                nullableInstant(rs, "enrolled_at")
        );
    }

    private MdmPolicyViolationRecord mapViolation(ResultSet rs, int rowNumber) throws SQLException {
        return new MdmPolicyViolationRecord(
                rs.getString("id"),
                rs.getString("device_id"),
                rs.getString("violation_type"),
                rs.getString("detail"),
                nullableInstant(rs, "occurred_at"),
                rs.getBoolean("handled"),
                rs.getString("handled_note"),
                rs.getString("disposition"),
                nullableInstant(rs, "blocked_at"),
                nullableInstant(rs, "created_at")
        );
    }
}
