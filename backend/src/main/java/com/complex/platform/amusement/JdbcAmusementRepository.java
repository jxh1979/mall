package com.complex.platform.amusement;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAmusementRepository implements AmusementRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcAmusementRowMapper rowMapper;
    private final com.complex.platform.common.crypto.PiiCipher piiCipher;

    JdbcAmusementRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
            com.complex.platform.common.crypto.PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.piiCipher = piiCipher;
        this.rowMapper = new JdbcAmusementRowMapper(this.objectMapper, piiCipher);
    }

    @Override
    public AmusementSafetyTemplateRecord saveSafetyTemplate(AmusementSafetyTemplateRecord template) {
        jdbcTemplate.update("""
                INSERT INTO amusement_safety_templates (
                    id, template_name, items_json, effective_from, status,
                    evidence_attachment_ids, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :template_name, CAST(:items_json AS jsonb), :effective_from, :status,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at, :merchant_id
                )
                ON CONFLICT (id) DO UPDATE SET
                    template_name = EXCLUDED.template_name,
                    items_json = EXCLUDED.items_json,
                    effective_from = EXCLUDED.effective_from,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, safetyTemplateParameters(template));
        return template;
    }

    @Override
    public List<AmusementSafetyTemplateRecord> listSafetyTemplates() {
        return jdbcTemplate.query("""
                SELECT id, template_name, items_json, effective_from, status,
                       evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM amusement_safety_templates
                 ORDER BY created_at DESC, id
                """, rowMapper::mapSafetyTemplate);
    }

    @Override
    public List<AmusementSafetyTemplateRecord> listSafetyTemplatesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, template_name, items_json, effective_from, status,
                       evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM amusement_safety_templates
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapSafetyTemplate);
    }

    @Override
    public Optional<AmusementSafetyTemplateRecord> findSafetyTemplateById(String id) {
        return jdbcTemplate.query("""
                SELECT id, template_name, items_json, effective_from, status,
                       evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM amusement_safety_templates
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapSafetyTemplate).stream().findFirst();
    }

    @Override
    public AmusementProjectRecord saveProject(AmusementProjectRecord project) {
        jdbcTemplate.update("""
                INSERT INTO amusement_projects (
                    id, project_name, capacity, open_status, safety_template_id,
                    eligibility_rule_json, location_point_id, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :project_name, :capacity, :open_status, :safety_template_id,
                    CAST(:eligibility_rule_json AS jsonb), :location_point_id, :created_at, :updated_at, :merchant_id
                )
                ON CONFLICT (id) DO UPDATE SET
                    project_name = EXCLUDED.project_name,
                    capacity = EXCLUDED.capacity,
                    open_status = EXCLUDED.open_status,
                    safety_template_id = EXCLUDED.safety_template_id,
                    eligibility_rule_json = EXCLUDED.eligibility_rule_json,
                    location_point_id = EXCLUDED.location_point_id,
                    updated_at = EXCLUDED.updated_at
                """, projectParameters(project));
        return project;
    }

    @Override
    public List<AmusementProjectRecord> listProjects() {
        return jdbcTemplate.query("""
                SELECT id, project_name, capacity, open_status, safety_template_id,
                       eligibility_rule_json, location_point_id, created_at, updated_at, merchant_id
                  FROM amusement_projects
                 ORDER BY created_at DESC, id
                """, rowMapper::mapProject);
    }

    @Override
    public List<AmusementProjectRecord> listProjectsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_name, capacity, open_status, safety_template_id,
                       eligibility_rule_json, location_point_id, created_at, updated_at, merchant_id
                  FROM amusement_projects
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapProject);
    }

    @Override
    public Optional<AmusementProjectRecord> findProjectById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_name, capacity, open_status, safety_template_id,
                       eligibility_rule_json, location_point_id, created_at, updated_at, merchant_id
                  FROM amusement_projects
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapProject).stream().findFirst();
    }

    @Override
    public Optional<String> findProjectIdByPointOrDevice(String pointId, String deviceId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("point_id", pointId)
                .addValue("device_id", deviceId);
        // 设备优先（更具体）：命中 amusement_devices 的 device_code 或 location_point_id → 其 project_id。
        if (deviceId != null || pointId != null) {
            java.util.List<String> byDevice = jdbcTemplate.query("""
                    SELECT project_id FROM amusement_devices
                     WHERE (:device_id IS NOT NULL AND device_code = :device_id)
                        OR (:point_id IS NOT NULL AND location_point_id = :point_id)
                     ORDER BY (device_code = :device_id) DESC, updated_at DESC
                     LIMIT 1
                    """, params, (rs, n) -> rs.getString("project_id"));
            if (!byDevice.isEmpty()) {
                return Optional.of(byDevice.get(0));
            }
        }
        // 退而按项目入口点位（amusement_projects.location_point_id）。
        if (pointId == null) {
            return Optional.empty();
        }
        return jdbcTemplate.query("""
                SELECT id FROM amusement_projects
                 WHERE location_point_id = :point_id
                 ORDER BY updated_at DESC
                 LIMIT 1
                """, params, (rs, n) -> rs.getString("id")).stream().findFirst();
    }

    @Override
    public Optional<AmusementSessionRecord> resolveOpenSessionForProject(String projectId, Instant now) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("now", timestampWithTimeZone(now));
        return jdbcTemplate.query("""
                SELECT id, project_id, start_time, end_time, capacity, used_capacity, locked_capacity,
                       status, evidence_attachment_ids, created_at, updated_at
                  FROM amusement_sessions
                 WHERE project_id = :project_id
                   AND status = 'open'
                   AND start_time <= :now
                   AND end_time >= :now
                 ORDER BY start_time DESC
                 LIMIT 1
                """, params, rowMapper::mapSession).stream().findFirst();
    }

    @Override
    public int assignProjectSpace(String projectId, String spaceId) {
        return jdbcTemplate.update("""
                UPDATE amusement_projects
                   SET space_id = :space_id,
                       updated_at = NOW()
                 WHERE id = :id
                """, Map.of("id", projectId, "space_id", spaceId));
    }

    @Override
    public Optional<String> findSpaceMerchant(String spaceId) {
        // spaces.merchant_id 可能为 NULL（平台/共享空间）；用 query 区分「空间不存在」与「存在但 merchant 为空」。
        List<String> rows = jdbcTemplate.query(
                "SELECT COALESCE(merchant_id, '') FROM spaces WHERE id = :id",
                Map.of("id", spaceId),
                (rs, n) -> rs.getString(1));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<AmusementProjectSpaceBacklogRecord> listProjectsWithoutSpace(Optional<String> merchantScope) {
        String base = """
                SELECT id, project_name, open_status, merchant_id
                  FROM amusement_projects
                 WHERE space_id IS NULL
                """;
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query(base + " ORDER BY created_at DESC, id", (rs, n) ->
                    new AmusementProjectSpaceBacklogRecord(
                            rs.getString("id"),
                            rs.getString("project_name"),
                            rs.getString("open_status"),
                            rs.getString("merchant_id")));
        }
        return jdbcTemplate.query(
                base + " AND merchant_id = :merchant_id ORDER BY created_at DESC, id",
                Map.of("merchant_id", merchantScope.get()),
                (rs, n) -> new AmusementProjectSpaceBacklogRecord(
                        rs.getString("id"),
                        rs.getString("project_name"),
                        rs.getString("open_status"),
                        rs.getString("merchant_id")));
    }

    @Override
    public List<AmusementProjectDeviceRegistryBacklogRecord> listProjectsMissingDeviceRegistry(Optional<String> merchantScope) {
        // T3.3：无任何在役设备档案的项目（与 evaluateOpenEligibility 强制建档判定的 noneMatch(active) 同口径）。
        String base = """
                SELECT id, project_name, open_status, merchant_id
                  FROM amusement_projects p
                 WHERE NOT EXISTS (SELECT 1 FROM amusement_devices d
                                    WHERE d.project_id = p.id AND d.status = 'active')
                """;
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query(base + " ORDER BY created_at DESC, id", (rs, n) ->
                    new AmusementProjectDeviceRegistryBacklogRecord(
                            rs.getString("id"),
                            rs.getString("project_name"),
                            rs.getString("open_status"),
                            rs.getString("merchant_id")));
        }
        return jdbcTemplate.query(
                base + " AND merchant_id = :merchant_id ORDER BY created_at DESC, id",
                Map.of("merchant_id", merchantScope.get()),
                (rs, n) -> new AmusementProjectDeviceRegistryBacklogRecord(
                        rs.getString("id"),
                        rs.getString("project_name"),
                        rs.getString("open_status"),
                        rs.getString("merchant_id")));
    }

    @Override
    public List<String> listProjectEntitlementTypes(String projectId) {
        return jdbcTemplate.queryForList("""
                SELECT entitlement_type
                  FROM amusement_project_entitlements
                 WHERE project_id = :project_id
                 ORDER BY entitlement_type
                """, Map.of("project_id", projectId), String.class);
    }

    @Override
    public AmusementProjectEntitlementRecord saveProjectEntitlement(AmusementProjectEntitlementRecord entitlement) {
        jdbcTemplate.update("""
                INSERT INTO amusement_project_entitlements (
                    id, project_id, entitlement_type, created_at
                )
                VALUES (:id, :project_id, :entitlement_type, :created_at)
                ON CONFLICT (project_id, entitlement_type) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", entitlement.id())
                .addValue("project_id", entitlement.projectId())
                .addValue("entitlement_type", entitlement.entitlementType())
                .addValue("created_at", timestampWithTimeZone(entitlement.createdAt())));
        // 返回库内权威记录（命中既有绑定时取既有 id/created_at，保证幂等可观测）。
        return findProjectEntitlement(entitlement.projectId(), entitlement.entitlementType()).orElse(entitlement);
    }

    private Optional<AmusementProjectEntitlementRecord> findProjectEntitlement(String projectId, String entitlementType) {
        return jdbcTemplate.query("""
                SELECT id, project_id, entitlement_type, created_at
                  FROM amusement_project_entitlements
                 WHERE project_id = :project_id AND entitlement_type = :entitlement_type
                """, Map.of("project_id", projectId, "entitlement_type", entitlementType),
                rowMapper::mapProjectEntitlement).stream().findFirst();
    }

    @Override
    public List<AmusementProjectEntitlementRecord> listProjectEntitlements(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, entitlement_type, created_at
                  FROM amusement_project_entitlements
                 WHERE project_id = :project_id
                 ORDER BY entitlement_type
                """, Map.of("project_id", projectId), rowMapper::mapProjectEntitlement);
    }

    @Override
    public boolean deleteProjectEntitlement(String projectId, String entitlementType) {
        int deleted = jdbcTemplate.update("""
                DELETE FROM amusement_project_entitlements
                 WHERE project_id = :project_id AND entitlement_type = :entitlement_type
                """, Map.of("project_id", projectId, "entitlement_type", entitlementType));
        return deleted > 0;
    }

    @Override
    public AmusementCapacityConfigRecord saveCapacityConfig(AmusementCapacityConfigRecord config) {
        jdbcTemplate.update("""
                INSERT INTO amusement_capacity_config (
                    id, project_id, location_point_id, device_id, max_concurrent,
                    effective_session_pattern, enabled, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :location_point_id, :device_id, :max_concurrent,
                    :effective_session_pattern, :enabled, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    location_point_id = EXCLUDED.location_point_id,
                    device_id = EXCLUDED.device_id,
                    max_concurrent = EXCLUDED.max_concurrent,
                    effective_session_pattern = EXCLUDED.effective_session_pattern,
                    enabled = EXCLUDED.enabled,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", config.id())
                .addValue("project_id", config.projectId())
                .addValue("location_point_id", config.locationPointId())
                .addValue("device_id", config.deviceId())
                .addValue("max_concurrent", config.maxConcurrent())
                .addValue("effective_session_pattern", config.effectiveSessionPattern())
                .addValue("enabled", config.enabled())
                .addValue("created_at", timestampWithTimeZone(config.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(config.updatedAt())));
        return findCapacityConfigById(config.id()).orElse(config);
    }

    @Override
    public List<AmusementCapacityConfigRecord> listCapacityConfigs(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, location_point_id, device_id, max_concurrent,
                       effective_session_pattern, enabled, created_at, updated_at
                  FROM amusement_capacity_config
                 WHERE project_id = :project_id
                 ORDER BY created_at DESC, id
                """, Map.of("project_id", projectId), rowMapper::mapCapacityConfig);
    }

    @Override
    public Optional<AmusementCapacityConfigRecord> findCapacityConfigById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_id, location_point_id, device_id, max_concurrent,
                       effective_session_pattern, enabled, created_at, updated_at
                  FROM amusement_capacity_config
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapCapacityConfig).stream().findFirst();
    }

    @Override
    public AmusementDeviceRecord saveDevice(AmusementDeviceRecord device) {
        jdbcTemplate.update("""
                INSERT INTO amusement_devices (
                    id, project_id, device_code, name, status,
                    safety_check_interval_hours, location_point_id, last_pass_inspection_at,
                    merchant_id, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :device_code, :name, :status,
                    :safety_check_interval_hours, :location_point_id, :last_pass_inspection_at,
                    :merchant_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    status = EXCLUDED.status,
                    safety_check_interval_hours = EXCLUDED.safety_check_interval_hours,
                    location_point_id = EXCLUDED.location_point_id,
                    last_pass_inspection_at = EXCLUDED.last_pass_inspection_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", device.id())
                .addValue("project_id", device.projectId())
                .addValue("device_code", device.deviceCode())
                .addValue("name", device.name())
                .addValue("status", device.status())
                .addValue("safety_check_interval_hours", device.safetyCheckIntervalHours())
                .addValue("location_point_id", device.locationPointId())
                .addValue("last_pass_inspection_at", timestampWithTimeZone(device.lastPassInspectionAt()))
                .addValue("merchant_id", device.merchantId())
                .addValue("created_at", timestampWithTimeZone(device.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(device.updatedAt())));
        return findDeviceById(device.id()).orElse(device);
    }

    @Override
    public List<AmusementDeviceRecord> listDevicesByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_code, name, status,
                       safety_check_interval_hours, location_point_id, last_pass_inspection_at,
                       merchant_id, created_at, updated_at
                  FROM amusement_devices
                 WHERE project_id = :project_id
                 ORDER BY created_at DESC, id
                """, Map.of("project_id", projectId), rowMapper::mapDevice);
    }

    @Override
    public Optional<AmusementDeviceRecord> findDeviceById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_code, name, status,
                       safety_check_interval_hours, location_point_id, last_pass_inspection_at,
                       merchant_id, created_at, updated_at
                  FROM amusement_devices
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapDevice).stream().findFirst();
    }

    @Override
    public Optional<AmusementDeviceRecord> findDeviceByCode(String projectId, String deviceCode) {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_code, name, status,
                       safety_check_interval_hours, location_point_id, last_pass_inspection_at,
                       merchant_id, created_at, updated_at
                  FROM amusement_devices
                 WHERE project_id = :project_id AND device_code = :device_code
                """, Map.of("project_id", projectId, "device_code", deviceCode), rowMapper::mapDevice)
                .stream().findFirst();
    }

    @Override
    public void updateDeviceLastPassInspection(String projectId, String deviceCode, java.time.Instant inspectedAt) {
        jdbcTemplate.update("""
                UPDATE amusement_devices
                   SET last_pass_inspection_at = :t, updated_at = :t
                 WHERE project_id = :p AND device_code = :c
                """, new MapSqlParameterSource()
                .addValue("t", timestampWithTimeZone(inspectedAt))
                .addValue("p", projectId)
                .addValue("c", deviceCode));
    }

    @Override
    public int findProjectSafetyCheckIntervalHours(String projectId) {
        Integer hours = jdbcTemplate.query("""
                SELECT safety_check_interval_hours FROM amusement_projects WHERE id = :id
                """, Map.of("id", projectId),
                (rs, n) -> (Integer) rs.getObject("safety_check_interval_hours"))
                .stream().findFirst().orElse(null);
        return hours == null ? 24 : hours;
    }

    @Override
    public void updateProjectDevicesLastPass(String projectId, java.time.Instant inspectedAt) {
        jdbcTemplate.update("""
                UPDATE amusement_devices
                   SET last_pass_inspection_at = :t, updated_at = :t
                 WHERE project_id = :p
                """, new MapSqlParameterSource()
                .addValue("t", timestampWithTimeZone(inspectedAt))
                .addValue("p", projectId));
    }

    @Override
    public void upsertInspectionTask(AmusementInspectionTaskRecord task) {
        // T3.1：自然键 (project_id, COALESCE(device_id,''), due_at) 唯一索引（V143）幂等——同到期点只生成一次。
        jdbcTemplate.update("""
                INSERT INTO amusement_inspection_tasks (
                    id, project_id, device_id, due_at, status, generated_at,
                    completed_inspection_id, completed_at, merchant_id, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :device_id, :due_at, :status, :generated_at,
                    :completed_inspection_id, :completed_at, :merchant_id, :created_at, :updated_at
                )
                ON CONFLICT (project_id, COALESCE(device_id, ''), due_at) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", task.id())
                .addValue("project_id", task.projectId())
                .addValue("device_id", task.deviceId())
                .addValue("due_at", timestampWithTimeZone(task.dueAt()))
                .addValue("status", task.status())
                .addValue("generated_at", timestampWithTimeZone(task.generatedAt()))
                .addValue("completed_inspection_id", task.completedInspectionId())
                .addValue("completed_at", timestampWithTimeZone(task.completedAt()))
                .addValue("merchant_id", task.merchantId())
                .addValue("created_at", timestampWithTimeZone(task.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(task.updatedAt())));
    }

    @Override
    public int markOverdueInspectionTasks(java.time.Instant now) {
        return jdbcTemplate.update("""
                UPDATE amusement_inspection_tasks
                   SET status = 'overdue', updated_at = :now
                 WHERE status = 'pending' AND due_at < :now
                """, new MapSqlParameterSource()
                .addValue("now", timestampWithTimeZone(now)));
    }

    @Override
    public java.util.List<AmusementInspectionTaskRecord> listInspectionTasks(java.util.Optional<String> merchantScope) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, project_id, device_id, due_at, status, generated_at, completed_inspection_id, "
                        + "completed_at, merchant_id, created_at, updated_at FROM amusement_inspection_tasks");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (merchantScope.isPresent()) {
            sql.append(" WHERE merchant_id = :merchant_id");
            params.addValue("merchant_id", merchantScope.get());
        }
        sql.append(" ORDER BY due_at ASC");
        return jdbcTemplate.query(sql.toString(), params, (rs, n) -> new AmusementInspectionTaskRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("device_id"),
                tsToInstant(rs.getTimestamp("due_at")),
                rs.getString("status"),
                tsToInstant(rs.getTimestamp("generated_at")),
                rs.getString("completed_inspection_id"),
                tsToInstant(rs.getTimestamp("completed_at")),
                rs.getString("merchant_id"),
                tsToInstant(rs.getTimestamp("created_at")),
                tsToInstant(rs.getTimestamp("updated_at"))));
    }

    private static java.time.Instant tsToInstant(java.sql.Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    @Override
    public AmusementOverCapacityPolicy findOverCapacityPolicy(String projectId) {
        return jdbcTemplate.query("""
                SELECT project_id, operating_capacity, disposal_mode, enabled
                  FROM amusement_over_capacity_policy
                 WHERE project_id = :project_id
                """, Map.of("project_id", projectId),
                (rs, n) -> new AmusementOverCapacityPolicy(
                        rs.getString("project_id"),
                        rs.getInt("operating_capacity"),
                        rs.getString("disposal_mode"),
                        rs.getBoolean("enabled")))
                .stream().findFirst()
                .orElseGet(() -> AmusementOverCapacityPolicy.disabled(projectId));
    }

    @Override
    public AmusementOverCapacityPolicy saveOverCapacityPolicy(AmusementOverCapacityPolicy policy) {
        // P0 清单#6 写入侧（V104）：按 project_id 幂等 upsert。首配置补 created_at；
        // 覆盖配置仅更新 operating_capacity/disposal_mode/enabled/updated_at（保留 created_at）。
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO amusement_over_capacity_policy (
                    project_id, operating_capacity, disposal_mode, enabled, created_at, updated_at
                )
                VALUES (
                    :project_id, :operating_capacity, :disposal_mode, :enabled, :created_at, :updated_at
                )
                ON CONFLICT (project_id) DO UPDATE SET
                    operating_capacity = EXCLUDED.operating_capacity,
                    disposal_mode = EXCLUDED.disposal_mode,
                    enabled = EXCLUDED.enabled,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("project_id", policy.projectId())
                .addValue("operating_capacity", policy.operatingCapacity())
                .addValue("disposal_mode", policy.disposalMode())
                .addValue("enabled", policy.enabled())
                .addValue("created_at", timestampWithTimeZone(now))
                .addValue("updated_at", timestampWithTimeZone(now)));
        return findOverCapacityPolicy(policy.projectId());
    }

    // ── REQ-53 权益会话（V129 entitlement_sessions）────────────────────────────
    @Override
    public EntitlementSessionRecord saveEntitlementSession(EntitlementSessionRecord session) {
        jdbcTemplate.update("""
                INSERT INTO entitlement_sessions (
                    id, session_code, entitlement_ref, merchant_id, project_id, status,
                    expires_at, initiated_by, claimed_by, claimed_at, created_at, updated_at
                )
                VALUES (
                    :id, :session_code, :entitlement_ref, :merchant_id, :project_id, :status,
                    :expires_at, :initiated_by, :claimed_by, :claimed_at, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    session_code = EXCLUDED.session_code,
                    entitlement_ref = EXCLUDED.entitlement_ref,
                    merchant_id = EXCLUDED.merchant_id,
                    project_id = EXCLUDED.project_id,
                    status = EXCLUDED.status,
                    expires_at = EXCLUDED.expires_at,
                    initiated_by = EXCLUDED.initiated_by,
                    claimed_by = EXCLUDED.claimed_by,
                    claimed_at = EXCLUDED.claimed_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", session.id())
                .addValue("session_code", session.sessionCode())
                .addValue("entitlement_ref", session.entitlementRef())
                .addValue("merchant_id", session.merchantId())
                .addValue("project_id", session.projectId())
                .addValue("status", session.status())
                .addValue("expires_at", timestampWithTimeZone(session.expiresAt()))
                .addValue("initiated_by", session.initiatedBy())
                .addValue("claimed_by", session.claimedBy())
                .addValue("claimed_at", timestampWithTimeZone(session.claimedAt()))
                .addValue("created_at", timestampWithTimeZone(session.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(session.updatedAt())));
        return session;
    }

    @Override
    public Optional<EntitlementSessionRecord> findEntitlementSessionById(String id) {
        return jdbcTemplate.query(entitlementSessionSelect() + " WHERE id = :id",
                Map.of("id", id), rowMapper::mapEntitlementSession).stream().findFirst();
    }

    @Override
    public Optional<EntitlementSessionRecord> findEntitlementSessionByCode(String sessionCode) {
        return jdbcTemplate.query(entitlementSessionSelect() + " WHERE session_code = :session_code",
                Map.of("session_code", sessionCode), rowMapper::mapEntitlementSession).stream().findFirst();
    }

    @Override
    public List<EntitlementSessionRecord> listEntitlementSessionsByMerchant(String merchantId) {
        return jdbcTemplate.query(entitlementSessionSelect()
                        + " WHERE merchant_id = :merchant_id ORDER BY created_at DESC, id",
                Map.of("merchant_id", merchantId), rowMapper::mapEntitlementSession);
    }

    @Override
    public List<EntitlementSessionRecord> listEntitlementSessions() {
        return jdbcTemplate.query(entitlementSessionSelect() + " ORDER BY created_at DESC, id",
                rowMapper::mapEntitlementSession);
    }

    @Override
    public boolean claimEntitlementSession(String id, String claimedBy, Instant now) {
        // 防错领核心：条件 UPDATE，仅 initiated/displayed 且未过期可领，受影响 1 行才成功。
        int updated = jdbcTemplate.update("""
                UPDATE entitlement_sessions
                   SET status = 'claimed',
                       claimed_by = :claimed_by,
                       claimed_at = :now,
                       updated_at = :now
                 WHERE id = :id
                   AND status IN ('initiated', 'displayed')
                   AND expires_at > :now
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("claimed_by", claimedBy)
                .addValue("now", timestampWithTimeZone(now)));
        return updated == 1;
    }

    @Override
    public boolean markEntitlementSessionDisplayed(String id, Instant now) {
        int updated = jdbcTemplate.update("""
                UPDATE entitlement_sessions
                   SET status = 'displayed',
                       updated_at = :now
                 WHERE id = :id
                   AND status = 'initiated'
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("now", timestampWithTimeZone(now)));
        return updated == 1;
    }

    private String entitlementSessionSelect() {
        return """
                SELECT id, session_code, entitlement_ref, merchant_id, project_id, status,
                       expires_at, initiated_by, claimed_by, claimed_at, created_at, updated_at
                  FROM entitlement_sessions
                """;
    }

    @Override
    public AmusementProjectRuleRecord saveProjectRule(AmusementProjectRuleRecord rule) {
        jdbcTemplate.update("""
                INSERT INTO amusement_project_rules (
                    id, project_id, rule_type, rule_value, effective_from, effective_to,
                    blocking, status, evidence_attachment_ids, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :rule_type, :rule_value, :effective_from, :effective_to,
                    :blocking, :status, CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    rule_type = EXCLUDED.rule_type,
                    rule_value = EXCLUDED.rule_value,
                    effective_from = EXCLUDED.effective_from,
                    effective_to = EXCLUDED.effective_to,
                    blocking = EXCLUDED.blocking,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, projectRuleParameters(rule));
        return rule;
    }

    @Override
    public List<AmusementProjectRuleRecord> listActiveBlockingProjectRules(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, rule_type, rule_value, effective_from, effective_to,
                       blocking, status, evidence_attachment_ids, created_at, updated_at
                  FROM amusement_project_rules
                 WHERE project_id = :project_id
                   AND blocking = TRUE
                   AND status = 'active'
                 ORDER BY created_at DESC, id
                """, Map.of("project_id", projectId), rowMapper::mapProjectRule);
    }

    @Override
    public AmusementSessionRecord saveSession(AmusementSessionRecord session) {
        jdbcTemplate.update("""
                INSERT INTO amusement_sessions (
                    id, project_id, start_time, end_time, capacity, used_capacity, locked_capacity,
                    status, evidence_attachment_ids, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :start_time, :end_time, :capacity, :used_capacity, :locked_capacity,
                    :status, CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at
                )
                -- P0 修复：saveSession 仅用于创建场次（容量增减走独立原子自增语句 incrementSessionUsedCapacityIfAvailable）。
                -- 此前 ON CONFLICT DO UPDATE 把 used_capacity/locked_capacity 重置为请求值（createSession 恒传 0,0），
                -- 用已存在场次的幂等键重放即可把在场计数清零 → 超卖。创建幂等重放应为 DO NOTHING，保留既有场次与实时计数。
                ON CONFLICT (id) DO NOTHING
                """, sessionParameters(session));
        return session;
    }

    @Override
    public List<AmusementSessionRecord> listSessions() {
        return jdbcTemplate.query("""
                SELECT id, project_id, start_time, end_time, capacity, used_capacity, locked_capacity,
                       status, evidence_attachment_ids, created_at, updated_at
                  FROM amusement_sessions
                 ORDER BY start_time DESC, id
                """, rowMapper::mapSession);
    }

    @Override
    public List<AmusementSessionRecord> listSessionsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, start_time, end_time, capacity, used_capacity, locked_capacity,
                       status, evidence_attachment_ids, created_at, updated_at
                  FROM amusement_sessions
                 WHERE project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id)
                 ORDER BY start_time DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapSession);
    }

    @Override
    public Optional<AmusementSessionRecord> findSessionById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_id, start_time, end_time, capacity, used_capacity, locked_capacity,
                       status, evidence_attachment_ids, created_at, updated_at
                  FROM amusement_sessions
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapSession).stream().findFirst();
    }

    @Override
    public boolean incrementSessionUsedCapacityIfAvailable(String sessionId) {
        return incrementSessionUsedCapacityIfAvailable(sessionId, 1);
    }

    private boolean incrementSessionUsedCapacityIfAvailable(String sessionId, int peopleCount) {
        int updated = jdbcTemplate.update("""
                UPDATE amusement_sessions
                   SET used_capacity = used_capacity + :people_count,
                       updated_at = :updated_at
                 WHERE id = :id
                   AND :people_count > 0
                   AND used_capacity + locked_capacity + :people_count <= capacity
                """, Map.of("id", sessionId, "people_count", peopleCount, "updated_at", timestampWithTimeZone(Instant.now())));
        return updated > 0;
    }

    @Override
    public CapacityClaimResult claimCapacityAcrossDimensions(CapacityClaimContext context) {
        java.util.List<String> controlled = new java.util.ArrayList<>();
        java.util.List<String> incremented = new java.util.ArrayList<>();
        Instant now = Instant.now();
        int peopleCount = Math.max(context.peopleCount(), 1);

        controlled.add("session");
        if (!incrementSessionUsedCapacityIfAvailable(context.sessionId(), peopleCount)) {
            return new CapacityClaimResult(false, "session", controlled);
        }
        incremented.add("session");

        Integer pointDeviceLimit = findPointDeviceCapacityLimit(context);
        if (pointDeviceLimit != null) {
            controlled.add("point_device");
            if (!incrementSnapshotIfAvailable(context, "point_device",
                    context.locationPointId(), context.deviceId(), pointDeviceLimit, peopleCount, now)) {
                rollbackIncremented(incremented, context, peopleCount, now);
                return new CapacityClaimResult(false, "point_device", controlled);
            }
            incremented.add("point_device");
        }

        if (context.projectSafetyCapacity() > 0) {
            controlled.add("project_safety");
            if (!incrementSnapshotIfAvailable(context, "project", null, null,
                    context.projectSafetyCapacity(), peopleCount, now)) {
                rollbackIncremented(incremented, context, peopleCount, now);
                return new CapacityClaimResult(false, "project_safety", controlled);
            }
            incremented.add("project_safety");
        }

        return new CapacityClaimResult(true, null, controlled);
    }

    private void rollbackIncremented(java.util.List<String> incremented, CapacityClaimContext context, int peopleCount, Instant now) {
        for (String dimension : incremented) {
            switch (dimension) {
                case "session" -> jdbcTemplate.update("""
                        UPDATE amusement_sessions
                           SET used_capacity = GREATEST(used_capacity - :people_count, 0),
                               updated_at = :updated_at
                         WHERE id = :id
                        """, Map.of("id", context.sessionId(), "people_count", peopleCount, "updated_at", timestampWithTimeZone(now)));
                case "point_device" -> decrementSnapshot(context, "point_device",
                        context.locationPointId(), context.deviceId(), peopleCount, now);
                case "project_safety" -> decrementSnapshot(context, "project", null, null, peopleCount, now);
                default -> { /* no-op */ }
            }
        }
    }

    /**
     * 查询该项目命中点位/设备的启用容量配置上限：优先精确匹配 (point, device)，
     * 退而匹配仅按设备或仅按点位的配置；无任何配置返回 null（该维度跳过，向后兼容）。
     */
    private Integer findPointDeviceCapacityLimit(CapacityClaimContext context) {
        if (context.locationPointId() == null && context.deviceId() == null) {
            return null;
        }
        // 命中口径：config 的点位/设备范围与本次核销点位/设备兼容（config 该列为空=通配，非空=须等于本次值），
        // 且 config 至少约束一个本次提供的维度。多条命中时取约束更具体（命中维度更多）的一条。
        return jdbcTemplate.query("""
                SELECT max_concurrent
                  FROM amusement_capacity_config
                 WHERE project_id = :project_id
                   AND enabled = TRUE
                   AND (location_point_id IS NULL OR location_point_id = :point_id)
                   AND (device_id IS NULL OR device_id = :device_id)
                   AND ((location_point_id IS NOT NULL AND :point_id IS NOT NULL)
                     OR (device_id IS NOT NULL AND :device_id IS NOT NULL))
                 ORDER BY (CASE WHEN location_point_id IS NOT NULL THEN 1 ELSE 0 END
                         + CASE WHEN device_id IS NOT NULL THEN 1 ELSE 0 END) DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("project_id", context.projectId())
                .addValue("point_id", context.locationPointId(), java.sql.Types.VARCHAR)
                .addValue("device_id", context.deviceId(), java.sql.Types.VARCHAR),
                (rs, n) -> rs.getInt("max_concurrent")).stream().findFirst().orElse(null);
    }

    /** upsert 维度快照（capacity_limit 刷新，occupied 不变）后原子占用本次人数。 */
    private boolean incrementSnapshotIfAvailable(CapacityClaimContext context, String dimension,
            String pointId, String deviceId, int limit, int peopleCount, Instant now) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", "capsnap_" + dimension + "_" + java.util.UUID.randomUUID())
                .addValue("project_id", context.projectId())
                .addValue("session_id", "session".equals(dimension) ? context.sessionId() : null)
                .addValue("point_id", pointId)
                .addValue("device_id", deviceId)
                .addValue("dimension", dimension)
                .addValue("limit", limit)
                .addValue("people_count", peopleCount)
                .addValue("updated_at", timestampWithTimeZone(now));
        jdbcTemplate.update("""
                INSERT INTO amusement_capacity_snapshot (
                    id, project_id, session_id, location_point_id, device_id,
                    dimension, capacity_limit, occupied_count, updated_at
                )
                VALUES (
                    :id, :project_id, :session_id, :point_id, :device_id,
                    :dimension, :limit, 0, :updated_at
                )
                ON CONFLICT (project_id, dimension,
                             COALESCE(session_id, ''), COALESCE(location_point_id, ''), COALESCE(device_id, ''))
                DO UPDATE SET capacity_limit = EXCLUDED.capacity_limit,
                              updated_at = EXCLUDED.updated_at
                """, params);
        int updated = jdbcTemplate.update("""
                UPDATE amusement_capacity_snapshot
                   SET occupied_count = occupied_count + :people_count,
                       updated_at = :updated_at
                 WHERE project_id = :project_id
                   AND dimension = :dimension
                   AND COALESCE(session_id, '') = COALESCE(:session_id, '')
                   AND COALESCE(location_point_id, '') = COALESCE(:point_id, '')
                   AND COALESCE(device_id, '') = COALESCE(:device_id, '')
                   AND occupied_count + :people_count <= capacity_limit
                """, params);
        return updated > 0;
    }

    private void decrementSnapshot(CapacityClaimContext context, String dimension,
            String pointId, String deviceId, int peopleCount, Instant now) {
        jdbcTemplate.update("""
                UPDATE amusement_capacity_snapshot
                   SET occupied_count = GREATEST(occupied_count - :people_count, 0),
                       updated_at = :updated_at
                 WHERE project_id = :project_id
                   AND dimension = :dimension
                   AND COALESCE(session_id, '') = COALESCE(:session_id, '')
                   AND COALESCE(location_point_id, '') = COALESCE(:point_id, '')
                   AND COALESCE(device_id, '') = COALESCE(:device_id, '')
                """, new MapSqlParameterSource()
                .addValue("project_id", context.projectId())
                .addValue("dimension", dimension)
                .addValue("session_id", "session".equals(dimension) ? context.sessionId() : null)
                .addValue("point_id", pointId)
                .addValue("device_id", deviceId)
                .addValue("people_count", peopleCount)
                .addValue("updated_at", timestampWithTimeZone(now)));
    }

    /** 阶段2 出场/主管释放：按维度键把并发在场 occupied_count -N（下限 0，never negative）。 */
    private void decrementConcurrentSnapshot(String projectId, String dimension,
            String pointId, String deviceId, int peopleCount, Instant now) {
        jdbcTemplate.update("""
                UPDATE amusement_capacity_snapshot
                   SET occupied_count = GREATEST(occupied_count - :people_count, 0),
                       updated_at = :updated_at
                 WHERE project_id = :project_id
                   AND dimension = :dimension
                   AND COALESCE(session_id, '') = ''
                   AND COALESCE(location_point_id, '') = COALESCE(:point_id, '')
                   AND COALESCE(device_id, '') = COALESCE(:device_id, '')
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("dimension", dimension)
                .addValue("point_id", pointId)
                .addValue("device_id", deviceId)
                .addValue("people_count", peopleCount)
                .addValue("updated_at", timestampWithTimeZone(now)));
    }

    @Override
    public Optional<AmusementVerificationRecord> findActiveEntryByEntitlement(String entitlementId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, credential_payload, device_id, staff_id,
                       verification_result, failure_reason, evidence_attachment_ids, created_at, verified_people,
                       direction, released_at, entry_verification_id, entitlement_id, location_point_id
                  FROM amusement_verifications
                 WHERE entitlement_id = :entitlement_id
                   AND direction = 'entry'
                   AND verification_result = 'success'
                   AND released_at IS NULL
                 ORDER BY created_at DESC, id
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("entitlement_id", entitlementId),
                rowMapper::mapVerification).stream().findFirst();
    }

    @Override
    public Optional<AmusementVerificationRecord> findLatestExitByEntitlement(String entitlementId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, credential_payload, device_id, staff_id,
                       verification_result, failure_reason, evidence_attachment_ids, created_at, verified_people,
                       direction, released_at, entry_verification_id, entitlement_id, location_point_id
                  FROM amusement_verifications
                 WHERE entitlement_id = :entitlement_id
                   AND direction = 'exit'
                 ORDER BY created_at DESC, id
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("entitlement_id", entitlementId),
                rowMapper::mapVerification).stream().findFirst();
    }

    @Override
    public Optional<AmusementVerificationRecord> findExitByEntry(String entryVerificationId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, credential_payload, device_id, staff_id,
                       verification_result, failure_reason, evidence_attachment_ids, created_at, verified_people,
                       direction, released_at, entry_verification_id, entitlement_id, location_point_id
                  FROM amusement_verifications
                 WHERE entry_verification_id = :entry_id
                   AND direction = 'exit'
                 ORDER BY created_at DESC, id
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("entry_id", entryVerificationId),
                rowMapper::mapVerification).stream().findFirst();
    }

    /**
     * 阶段2 出场核销原子释放：
     * <ol>
     *   <li>条件 UPDATE 标记入场 released_at（仅当仍未释放）——此条件即幂等/并发赢家判定，rows=0 表示已被释放。</li>
     *   <li>赢家把该入场占用的并发快照（point_device + project 维度）-N（下限 0）。</li>
     *   <li>写 direction=exit 记录（entry_verification_id 唯一约束再兜底防重）。</li>
     * </ol>
     * 返回 true=本次真实释放；false=入场已释放（幂等跳过，不重复减容量、不新增 exit）。
     */
    @Override
    public boolean releaseEntryConcurrentCapacity(AmusementVerificationRecord entry, AmusementVerificationRecord exitRecord, Instant now) {
        int marked = jdbcTemplate.update("""
                UPDATE amusement_verifications
                   SET released_at = :released_at
                 WHERE id = :id
                   AND direction = 'entry'
                   AND verification_result = 'success'
                   AND released_at IS NULL
                """, new MapSqlParameterSource()
                .addValue("id", entry.id())
                .addValue("released_at", timestampWithTimeZone(now)));
        if (marked == 0) {
            return false;
        }
        int people = Math.max(entry.verifiedPeople(), 1);
        // 与入场 incrementSnapshotIfAvailable 对称：point_device 维（若入场有 point/device）+ project 维各 -N。
        // 未配置的维度对应 0 行更新，天然幂等（含场次/批次型：无并发快照 → 全 no-op）。
        decrementConcurrentSnapshot(entry.projectId(), "point_device",
                entry.locationPointId(), entry.deviceId(), people, now);
        decrementConcurrentSnapshot(entry.projectId(), "project", null, null, people, now);
        saveVerification(exitRecord);
        return true;
    }

    /**
     * 阶段2 主管释放（异常清场）：把项目的并发在场快照（point_device + project 维度）occupied_count 清零。
     * 用于漏扫出场堆积、异常清场。场次/批次型（session 维、amusement_sessions.used_capacity）不受影响。
     * 返回清零涉及的快照行数（审计留痕参考）。
     */
    @Override
    public int clearProjectConcurrentCapacity(String projectId, Instant now) {
        return jdbcTemplate.update("""
                UPDATE amusement_capacity_snapshot
                   SET occupied_count = 0,
                       updated_at = :updated_at
                 WHERE project_id = :project_id
                   AND dimension IN ('point_device', 'project')
                   AND occupied_count > 0
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("updated_at", timestampWithTimeZone(now)));
    }

    @Override
    public boolean lockSessionCapacityIfAvailable(String sessionId, int capacityToLock) {
        int updated = jdbcTemplate.update("""
                UPDATE amusement_sessions
                   SET locked_capacity = locked_capacity + :capacity_to_lock,
                       updated_at = :updated_at
                 WHERE id = :id
                   AND :capacity_to_lock > 0
                   AND used_capacity + locked_capacity + :capacity_to_lock <= capacity
                """, Map.of(
                "id", sessionId,
                "capacity_to_lock", capacityToLock,
                "updated_at", timestampWithTimeZone(Instant.now())
        ));
        return updated > 0;
    }

    @Override
    public void releaseSessionLockedCapacity(String sessionId, int capacityToRelease) {
        jdbcTemplate.update("""
                UPDATE amusement_sessions
                   SET locked_capacity = GREATEST(locked_capacity - :capacity_to_release, 0),
                       updated_at = :updated_at
                 WHERE id = :id
                   AND :capacity_to_release > 0
                """, Map.of(
                "id", sessionId,
                "capacity_to_release", capacityToRelease,
                "updated_at", timestampWithTimeZone(Instant.now())
        ));
    }

    @Override
    public AmusementSafetyInspectionRecord saveSafetyInspection(AmusementSafetyInspectionRecord inspection) {
        jdbcTemplate.update("""
                INSERT INTO amusement_safety_inspections (
                    id, project_id, device_id, template_id, items_json, overall_result, inspection_status,
                    rectification_status, reopen_status, inspected_at, submitted_at, rechecked_at,
                    approved_at, operator_note, evidence_attachment_ids, created_at, updated_at,
                    inspector_id, inspection_result, abnormal_found, abnormal_note
                )
                VALUES (
                    :id, :project_id, :device_id, :template_id, CAST(:items_json AS jsonb), :overall_result, :inspection_status,
                    :rectification_status, :reopen_status, :inspected_at, :submitted_at, :rechecked_at,
                    :approved_at, :operator_note, CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at,
                    :inspector_id, :inspection_result, :abnormal_found, :abnormal_note
                )
                ON CONFLICT (id) DO UPDATE SET
                    device_id = EXCLUDED.device_id,
                    items_json = EXCLUDED.items_json,
                    overall_result = EXCLUDED.overall_result,
                    inspection_status = EXCLUDED.inspection_status,
                    rectification_status = EXCLUDED.rectification_status,
                    reopen_status = EXCLUDED.reopen_status,
                    inspected_at = EXCLUDED.inspected_at,
                    submitted_at = EXCLUDED.submitted_at,
                    rechecked_at = EXCLUDED.rechecked_at,
                    approved_at = EXCLUDED.approved_at,
                    operator_note = EXCLUDED.operator_note,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at,
                    inspector_id = EXCLUDED.inspector_id,
                    inspection_result = EXCLUDED.inspection_result,
                    abnormal_found = EXCLUDED.abnormal_found,
                    abnormal_note = EXCLUDED.abnormal_note
                """, safetyInspectionParameters(inspection));
        return inspection;
    }

    @Override
    public List<AmusementSafetyInspectionRecord> listSafetyInspections() {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_id, template_id, items_json, overall_result, inspection_status,
                       rectification_status, reopen_status, inspected_at, submitted_at, rechecked_at,
                       approved_at, operator_note, evidence_attachment_ids, created_at, updated_at,
                       inspector_id, inspection_result, abnormal_found, abnormal_note
                  FROM amusement_safety_inspections
                 ORDER BY created_at DESC, id
                """, rowMapper::mapSafetyInspection);
    }

    @Override
    public List<AmusementSafetyInspectionRecord> listSafetyInspectionsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_id, template_id, items_json, overall_result, inspection_status,
                       rectification_status, reopen_status, inspected_at, submitted_at, rechecked_at,
                       approved_at, operator_note, evidence_attachment_ids, created_at, updated_at,
                       inspector_id, inspection_result, abnormal_found, abnormal_note
                  FROM amusement_safety_inspections
                 WHERE project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id)
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapSafetyInspection);
    }

    @Override
    public Optional<AmusementSafetyInspectionRecord> findSafetyInspectionById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_id, template_id, items_json, overall_result, inspection_status,
                       rectification_status, reopen_status, inspected_at, submitted_at, rechecked_at,
                       approved_at, operator_note, evidence_attachment_ids, created_at, updated_at,
                       inspector_id, inspection_result, abnormal_found, abnormal_note
                  FROM amusement_safety_inspections
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapSafetyInspection).stream().findFirst();
    }

    @Override
    public Optional<AmusementSafetyInspectionRecord> latestSafetyInspectionForProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_id, template_id, items_json, overall_result, inspection_status,
                       rectification_status, reopen_status, inspected_at, submitted_at, rechecked_at,
                       approved_at, operator_note, evidence_attachment_ids, created_at, updated_at,
                       inspector_id, inspection_result, abnormal_found, abnormal_note
                  FROM amusement_safety_inspections
                 WHERE project_id = :project_id
                 ORDER BY inspected_at DESC, created_at DESC, id
                 LIMIT 1
                """, Map.of("project_id", projectId), rowMapper::mapSafetyInspection).stream().findFirst();
    }

    @Override
    public AmusementSafetyActionRecord saveSafetyAction(AmusementSafetyActionRecord action) {
        jdbcTemplate.update("""
                INSERT INTO amusement_safety_actions (
                    id, inspection_id, action_type, action_result, comment, attachment_ids, created_at
                )
                VALUES (
                    :id, :inspection_id, :action_type, :action_result, :comment,
                    CAST(:attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    action_result = EXCLUDED.action_result,
                    comment = EXCLUDED.comment,
                    attachment_ids = EXCLUDED.attachment_ids
                """, safetyActionParameters(action));
        return action;
    }

    @Override
    public AmusementVerificationRecord saveVerification(AmusementVerificationRecord verification) {
        jdbcTemplate.update("""
                INSERT INTO amusement_verifications (
                    id, project_id, session_id, credential_payload, device_id, staff_id,
                    verification_result, failure_reason, evidence_attachment_ids, created_at, verified_people,
                    direction, released_at, entry_verification_id, entitlement_id, location_point_id
                )
                VALUES (
                    :id, :project_id, :session_id, :credential_payload, :device_id, :staff_id,
                    :verification_result, :failure_reason, CAST(:evidence_attachment_ids AS jsonb), :created_at, :verified_people,
                    :direction, :released_at, :entry_verification_id, :entitlement_id, :location_point_id
                )
                ON CONFLICT (id) DO UPDATE SET
                    verification_result = EXCLUDED.verification_result,
                    failure_reason = EXCLUDED.failure_reason,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    verified_people = EXCLUDED.verified_people
                """, verificationParameters(verification));
        return verification;
    }

    @Override
    public List<AmusementVerificationRecord> listVerifications() {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, credential_payload, device_id, staff_id,
                       verification_result, failure_reason, evidence_attachment_ids, created_at, verified_people,
                       direction, released_at, entry_verification_id, entitlement_id, location_point_id
                  FROM amusement_verifications
                 ORDER BY created_at DESC, id
                """, rowMapper::mapVerification);
    }

    @Override
    public List<AmusementVerificationRecord> listVerificationsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, credential_payload, device_id, staff_id,
                       verification_result, failure_reason, evidence_attachment_ids, created_at, verified_people,
                       direction, released_at, entry_verification_id, entitlement_id, location_point_id
                  FROM amusement_verifications
                 WHERE project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id)
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapVerification);
    }

    @Override
    public List<AmusementVerificationListItem> listVerificationItems(AmusementVerificationQuery query, String merchantId) {
        // 阶段3 只读：核销记录本体 + 反查关联通票号（LEFT JOIN，未分流记录为 null）。
        // merchant scope 沿用同域范式：租户视角加 project_id IN (SELECT ... WHERE merchant_id=:merchant_id)。
        StringBuilder sql = new StringBuilder("""
                SELECT av.id, av.project_id, av.session_id, av.device_id, av.staff_id,
                       av.verification_result, av.verified_people, av.direction, av.created_at,
                       av.released_at, av.entry_verification_id, av.entitlement_id, av.location_point_id,
                       tv.verification_no AS linked_ticket_no
                  FROM amusement_verifications av
                  LEFT JOIN ticket_verifications tv ON tv.amusement_verification_id = av.id
                 WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (merchantId != null && !merchantId.isBlank()) {
            sql.append(" AND av.project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id)");
            params.addValue("merchant_id", merchantId);
        }
        if (query.projectId() != null) {
            sql.append(" AND av.project_id = :project_id");
            params.addValue("project_id", query.projectId());
        }
        if (query.sessionId() != null) {
            sql.append(" AND av.session_id = :session_id");
            params.addValue("session_id", query.sessionId());
        }
        if (query.direction() != null) {
            sql.append(" AND av.direction = :direction");
            params.addValue("direction", query.direction());
        }
        if (query.dateFrom() != null) {
            sql.append(" AND av.created_at >= :date_from");
            params.addValue("date_from", timestampWithTimeZone(query.dateFrom().atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
        }
        if (query.dateTo() != null) {
            sql.append(" AND av.created_at < :date_to");
            params.addValue("date_to", timestampWithTimeZone(query.dateTo().plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
        }
        sql.append(" ORDER BY av.created_at DESC, av.id");
        return jdbcTemplate.query(sql.toString(), params, rowMapper::mapVerificationListItem);
    }

    @Override
    public List<AmusementCapacitySnapshotRecord> listCapacitySnapshots(String merchantId) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, project_id, session_id, location_point_id, device_id,
                       dimension, capacity_limit, occupied_count, updated_at
                  FROM amusement_capacity_snapshot
                 WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (merchantId != null && !merchantId.isBlank()) {
            sql.append(" AND project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id)");
            params.addValue("merchant_id", merchantId);
        }
        sql.append(" ORDER BY dimension, project_id, updated_at DESC, id");
        return jdbcTemplate.query(sql.toString(), params, rowMapper::mapCapacitySnapshot);
    }

    @Override
    public AmusementManualOverrideRecord saveManualOverride(AmusementManualOverrideRecord override) {
        jdbcTemplate.update("""
                INSERT INTO amusement_manual_overrides (
                    id, project_id, session_id, override_type, reason, status,
                    operator_note, evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :project_id, :session_id, :override_type, :reason, :status,
                    :operator_note, CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    override_type = EXCLUDED.override_type,
                    reason = EXCLUDED.reason,
                    status = EXCLUDED.status,
                    operator_note = EXCLUDED.operator_note,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids
                """, manualOverrideParameters(override));
        return override;
    }

    @Override
    public List<AmusementManualOverrideRecord> listManualOverrides() {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, override_type, reason, status,
                       operator_note, evidence_attachment_ids, created_at
                  FROM amusement_manual_overrides
                 ORDER BY created_at DESC, id
                """, rowMapper::mapManualOverride);
    }

    @Override
    public List<AmusementManualOverrideRecord> listManualOverridesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, override_type, reason, status,
                       operator_note, evidence_attachment_ids, created_at
                  FROM amusement_manual_overrides
                 WHERE project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id)
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapManualOverride);
    }

    @Override
    public AmusementDeviceStatusRecord saveDeviceStatus(AmusementDeviceStatusRecord status) {
        jdbcTemplate.update("""
                INSERT INTO amusement_device_statuses (
                    id, project_id, device_id, provider_code, status, last_seen_at,
                    raw_status_json, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :device_id, :provider_code, :status, :last_seen_at,
                    CAST(:raw_status_json AS jsonb), :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    provider_code = EXCLUDED.provider_code,
                    status = EXCLUDED.status,
                    last_seen_at = EXCLUDED.last_seen_at,
                    raw_status_json = EXCLUDED.raw_status_json,
                    updated_at = EXCLUDED.updated_at
                """, deviceStatusParameters(status));
        return status;
    }

    @Override
    public List<AmusementDeviceStatusRecord> listDeviceStatuses() {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_id, provider_code, status, last_seen_at,
                       raw_status_json, created_at, updated_at
                  FROM amusement_device_statuses
                 ORDER BY last_seen_at DESC, id
                """, rowMapper::mapDeviceStatus);
    }

    @Override
    public List<AmusementDeviceStatusRecord> listDeviceStatusesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_id, provider_code, status, last_seen_at,
                       raw_status_json, created_at, updated_at
                  FROM amusement_device_statuses
                 WHERE project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id)
                 ORDER BY last_seen_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapDeviceStatus);
    }

    @Override
    public AmusementQueueEntryRecord saveQueueEntry(AmusementQueueEntryRecord entry) {
        jdbcTemplate.update("""
                INSERT INTO amusement_queue_entries (
                    id, project_id, session_id, customer_id, queue_no, status, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :session_id, :customer_id, :queue_no, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, queueEntryParameters(entry));
        return entry;
    }

    @Override
    public List<AmusementQueueEntryRecord> listQueueEntries() {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, customer_id, queue_no, status, created_at, updated_at
                  FROM amusement_queue_entries
                 ORDER BY created_at DESC, id
                """, rowMapper::mapQueueEntry);
    }

    @Override
    public List<AmusementQueueEntryRecord> listQueueEntriesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, customer_id, queue_no, status, created_at, updated_at
                  FROM amusement_queue_entries
                 WHERE project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id)
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapQueueEntry);
    }

    @Override
    public Optional<AmusementQueueEntryRecord> findQueueEntryById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, customer_id, queue_no, status, created_at, updated_at
                  FROM amusement_queue_entries
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapQueueEntry).stream().findFirst();
    }

    @Override
    public AmusementReservationRecord saveReservation(AmusementReservationRecord reservation) {
        jdbcTemplate.update("""
                INSERT INTO amusement_reservations (
                    id, project_id, session_id, customer_id, reservation_status, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :session_id, :customer_id, :reservation_status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    reservation_status = EXCLUDED.reservation_status,
                    updated_at = EXCLUDED.updated_at
                """, reservationParameters(reservation));
        return reservation;
    }

    @Override
    public List<AmusementReservationRecord> listReservations() {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, customer_id, reservation_status, created_at, updated_at
                  FROM amusement_reservations
                 ORDER BY created_at DESC, id
                """, rowMapper::mapReservation);
    }

    @Override
    public List<AmusementReservationRecord> listReservationsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, customer_id, reservation_status, created_at, updated_at
                  FROM amusement_reservations
                 WHERE project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id)
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapReservation);
    }

    @Override
    public Optional<AmusementReservationRecord> findReservationById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, customer_id, reservation_status, created_at, updated_at
                  FROM amusement_reservations
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapReservation).stream().findFirst();
    }

    @Override
    public AmusementMaintenanceRecord saveMaintenanceRecord(AmusementMaintenanceRecord record) {
        jdbcTemplate.update("""
                INSERT INTO amusement_maintenance_record (
                    id, project_id, device_id, location_point_id, maintenance_vendor_id,
                    engineer_id, engineer_name, maintenance_at, result, exception_note,
                    source_terminal, status, supervisor_confirmed_by, confirmed_at,
                    resume_business_at, merchant_id, created_at
                )
                VALUES (
                    :id, :project_id, :device_id, :location_point_id, :maintenance_vendor_id,
                    :engineer_id, :engineer_name, :maintenance_at, :result, :exception_note,
                    :source_terminal, :status, :supervisor_confirmed_by, :confirmed_at,
                    :resume_business_at, :merchant_id, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    supervisor_confirmed_by = EXCLUDED.supervisor_confirmed_by,
                    confirmed_at = EXCLUDED.confirmed_at,
                    resume_business_at = EXCLUDED.resume_business_at
                """, maintenanceRecordParameters(record));
        return record;
    }

    @Override
    public List<AmusementMaintenanceRecord> listMaintenanceRecords() {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_id, location_point_id, maintenance_vendor_id,
                       engineer_id, engineer_name, maintenance_at, result, exception_note,
                       source_terminal, status, supervisor_confirmed_by, confirmed_at,
                       resume_business_at, merchant_id, created_at
                  FROM amusement_maintenance_record
                 ORDER BY created_at DESC, id
                """, rowMapper::mapMaintenanceRecord);
    }

    @Override
    public List<AmusementMaintenanceRecord> listMaintenanceRecordsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_id, location_point_id, maintenance_vendor_id,
                       engineer_id, engineer_name, maintenance_at, result, exception_note,
                       source_terminal, status, supervisor_confirmed_by, confirmed_at,
                       resume_business_at, merchant_id, created_at
                  FROM amusement_maintenance_record
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapMaintenanceRecord);
    }

    @Override
    public Optional<AmusementMaintenanceRecord> findMaintenanceRecordById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_id, device_id, location_point_id, maintenance_vendor_id,
                       engineer_id, engineer_name, maintenance_at, result, exception_note,
                       source_terminal, status, supervisor_confirmed_by, confirmed_at,
                       resume_business_at, merchant_id, created_at
                  FROM amusement_maintenance_record
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapMaintenanceRecord).stream().findFirst();
    }

    @Override
    public Optional<String> findMaintenanceVendorStatus(String vendorId) {
        // 检修单位复用供应链供应商主数据（inventory_suppliers）；存在性 = 是否查到行，可用性 = status。
        return jdbcTemplate.query(
                "SELECT status FROM inventory_suppliers WHERE id = :id",
                Map.of("id", vendorId),
                (rs, n) -> rs.getString("status")).stream().findFirst();
    }

    @Override
    public boolean updateProjectOpenStatus(String projectId, String expectedCurrentStatus, String newStatus) {
        int updated = jdbcTemplate.update("""
                UPDATE amusement_projects
                   SET open_status = :new_status,
                       updated_at = :updated_at
                 WHERE id = :id
                   AND open_status = :expected_status
                """, new MapSqlParameterSource()
                .addValue("id", projectId)
                .addValue("expected_status", expectedCurrentStatus)
                .addValue("new_status", newStatus)
                .addValue("updated_at", timestampWithTimeZone(Instant.now())));
        return updated == 1;
    }

    @Override
    public AmusementGroupBookingRecord saveGroupBooking(AmusementGroupBookingRecord booking) {
        jdbcTemplate.update("""
                INSERT INTO amusement_group_bookings (
                    id, project_id, session_id, booking_type, customer_name, contact_phone,
                    party_size, reserved_capacity, booking_status, approval_note, operator_note,
                    evidence_attachment_ids, approved_at, released_at, created_at, updated_at, paid_at
                )
                VALUES (
                    :id, :project_id, :session_id, :booking_type, :customer_name, :contact_phone,
                    :party_size, :reserved_capacity, :booking_status, :approval_note, :operator_note,
                    CAST(:evidence_attachment_ids AS jsonb), :approved_at, :released_at, :created_at, :updated_at, :paid_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    booking_status = EXCLUDED.booking_status,
                    approval_note = EXCLUDED.approval_note,
                    operator_note = EXCLUDED.operator_note,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    approved_at = EXCLUDED.approved_at,
                    released_at = EXCLUDED.released_at,
                    updated_at = EXCLUDED.updated_at,
                    paid_at = EXCLUDED.paid_at
                """, groupBookingParameters(booking));
        return booking;
    }

    @Override
    public List<AmusementGroupBookingRecord> listGroupBookings() {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, booking_type, customer_name, contact_phone,
                       party_size, reserved_capacity, booking_status, approval_note, operator_note,
                       evidence_attachment_ids, approved_at, released_at, created_at, updated_at, paid_at
                  FROM amusement_group_bookings
                 ORDER BY created_at DESC, id
                """, rowMapper::mapGroupBooking);
    }

    @Override
    public List<AmusementGroupBookingRecord> listGroupBookingsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, booking_type, customer_name, contact_phone,
                       party_size, reserved_capacity, booking_status, approval_note, operator_note,
                       evidence_attachment_ids, approved_at, released_at, created_at, updated_at, paid_at
                  FROM amusement_group_bookings
                 WHERE project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id)
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapGroupBooking);
    }

    @Override
    public Optional<AmusementGroupBookingRecord> findGroupBookingById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_id, session_id, booking_type, customer_name, contact_phone,
                       party_size, reserved_capacity, booking_status, approval_note, operator_note,
                       evidence_attachment_ids, approved_at, released_at, created_at, updated_at, paid_at
                  FROM amusement_group_bookings
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapGroupBooking).stream().findFirst();
    }

    @Override
    public Optional<AmusementGroupBookingRecord> approveGroupBookingWithCapacityLock(
            String id,
            String approvalNote,
            List<String> evidenceAttachmentIds,
            Instant approvedAt
    ) {
        return jdbcTemplate.query("""
                WITH candidate AS (
                    SELECT id, session_id, reserved_capacity
                      FROM amusement_group_bookings
                     WHERE id = :id
                       AND booking_status = 'pending_approval'
                     FOR UPDATE
                ), locked_session AS (
                    UPDATE amusement_sessions session
                       SET locked_capacity = session.locked_capacity + candidate.reserved_capacity,
                           updated_at = :updated_at
                      FROM candidate
                     WHERE session.id = candidate.session_id
                       AND session.status IN ('scheduled', 'open')
                       AND session.used_capacity + session.locked_capacity + candidate.reserved_capacity <= session.capacity
                     RETURNING session.id
                ), updated_booking AS (
                    UPDATE amusement_group_bookings booking
                       SET booking_status = 'approved',
                           approval_note = :approval_note,
                           evidence_attachment_ids = CAST(:evidence_attachment_ids AS jsonb),
                           approved_at = :approved_at,
                           -- V231：本域无独立支付步，审批确认即预定确认落点，paid_at=approved_at。
                           paid_at = :approved_at,
                           updated_at = :updated_at
                      FROM candidate, locked_session
                     WHERE booking.id = candidate.id
                     RETURNING booking.id, booking.project_id, booking.session_id, booking.booking_type,
                               booking.customer_name, booking.contact_phone, booking.party_size,
                               booking.reserved_capacity, booking.booking_status, booking.approval_note,
                               booking.operator_note, booking.evidence_attachment_ids, booking.approved_at,
                               booking.released_at, booking.created_at, booking.updated_at, booking.paid_at
                )
                SELECT * FROM updated_booking
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("approval_note", approvalNote)
                .addValue("evidence_attachment_ids", writeJson(evidenceAttachmentIds))
                .addValue("approved_at", timestampWithTimeZone(approvedAt))
                .addValue("updated_at", timestampWithTimeZone(approvedAt)), rowMapper::mapGroupBooking).stream().findFirst();
    }

    @Override
    public Optional<AmusementGroupBookingRecord> releaseGroupBookingAndCapacity(
            String id,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            Instant releasedAt
    ) {
        return jdbcTemplate.query("""
                WITH candidate AS (
                    SELECT id, session_id, reserved_capacity
                      FROM amusement_group_bookings
                     WHERE id = :id
                       AND booking_status = 'approved'
                     FOR UPDATE
                ), released_session AS (
                    UPDATE amusement_sessions session
                       SET locked_capacity = GREATEST(session.locked_capacity - candidate.reserved_capacity, 0),
                           updated_at = :updated_at
                      FROM candidate
                     WHERE session.id = candidate.session_id
                     RETURNING session.id
                ), updated_booking AS (
                    UPDATE amusement_group_bookings booking
                       SET booking_status = 'released',
                           operator_note = :operator_note,
                           evidence_attachment_ids = CAST(:evidence_attachment_ids AS jsonb),
                           released_at = :released_at,
                           updated_at = :updated_at
                      FROM candidate, released_session
                     WHERE booking.id = candidate.id
                     RETURNING booking.id, booking.project_id, booking.session_id, booking.booking_type,
                               booking.customer_name, booking.contact_phone, booking.party_size,
                               booking.reserved_capacity, booking.booking_status, booking.approval_note,
                               booking.operator_note, booking.evidence_attachment_ids, booking.approved_at,
                               booking.released_at, booking.created_at, booking.updated_at, booking.paid_at
                )
                SELECT * FROM updated_booking
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("operator_note", operatorNote)
                .addValue("evidence_attachment_ids", writeJson(evidenceAttachmentIds))
                .addValue("released_at", timestampWithTimeZone(releasedAt))
                .addValue("updated_at", timestampWithTimeZone(releasedAt)), rowMapper::mapGroupBooking).stream().findFirst();
    }

    private MapSqlParameterSource safetyTemplateParameters(AmusementSafetyTemplateRecord template) {
        return new MapSqlParameterSource()
                .addValue("id", template.id())
                .addValue("template_name", template.templateName())
                .addValue("items_json", writeJson(template.items()))
                .addValue("effective_from", template.effectiveFrom())
                .addValue("status", template.status())
                .addValue("evidence_attachment_ids", writeJson(template.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(template.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(template.updatedAt()))
                .addValue("merchant_id", template.merchantId());
    }

    private MapSqlParameterSource projectParameters(AmusementProjectRecord project) {
        return new MapSqlParameterSource()
                .addValue("id", project.id())
                .addValue("project_name", project.projectName())
                .addValue("capacity", project.capacity())
                .addValue("open_status", project.openStatus())
                .addValue("safety_template_id", project.safetyTemplateId())
                .addValue("eligibility_rule_json", writeJson(project.eligibilityRule()))
                .addValue("location_point_id", project.locationPointId())
                .addValue("created_at", timestampWithTimeZone(project.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(project.updatedAt()))
                .addValue("merchant_id", project.merchantId());
    }

    private MapSqlParameterSource maintenanceRecordParameters(AmusementMaintenanceRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("project_id", record.projectId())
                .addValue("device_id", record.deviceId())
                .addValue("location_point_id", record.locationPointId())
                .addValue("maintenance_vendor_id", record.maintenanceVendorId())
                .addValue("engineer_id", record.engineerId())
                .addValue("engineer_name", record.engineerName())
                .addValue("maintenance_at", timestampWithTimeZone(record.maintenanceAt()))
                .addValue("result", record.result())
                .addValue("exception_note", record.exceptionNote())
                .addValue("source_terminal", record.sourceTerminal())
                .addValue("status", record.status())
                .addValue("supervisor_confirmed_by", record.supervisorConfirmedBy())
                .addValue("confirmed_at", timestampWithTimeZone(record.confirmedAt()))
                .addValue("resume_business_at", timestampWithTimeZone(record.resumeBusinessAt()))
                .addValue("merchant_id", record.merchantId())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()));
    }

    private MapSqlParameterSource projectRuleParameters(AmusementProjectRuleRecord rule) {
        return new MapSqlParameterSource()
                .addValue("id", rule.id())
                .addValue("project_id", rule.projectId())
                .addValue("rule_type", rule.ruleType())
                .addValue("rule_value", rule.ruleValue())
                .addValue("effective_from", rule.effectiveFrom())
                .addValue("effective_to", rule.effectiveTo())
                .addValue("blocking", rule.blocking())
                .addValue("status", rule.status())
                .addValue("evidence_attachment_ids", writeJson(rule.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(rule.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(rule.updatedAt()));
    }

    private MapSqlParameterSource sessionParameters(AmusementSessionRecord session) {
        return new MapSqlParameterSource()
                .addValue("id", session.id())
                .addValue("project_id", session.projectId())
                .addValue("start_time", timestampWithTimeZone(session.startTime()))
                .addValue("end_time", timestampWithTimeZone(session.endTime()))
                .addValue("capacity", session.capacity())
                .addValue("used_capacity", session.usedCapacity())
                .addValue("locked_capacity", session.lockedCapacity())
                .addValue("status", session.status())
                .addValue("evidence_attachment_ids", writeJson(session.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(session.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(session.updatedAt()));
    }

    private MapSqlParameterSource safetyInspectionParameters(AmusementSafetyInspectionRecord inspection) {
        return new MapSqlParameterSource()
                .addValue("id", inspection.id())
                .addValue("project_id", inspection.projectId())
                .addValue("device_id", inspection.deviceId())
                .addValue("template_id", inspection.templateId())
                .addValue("items_json", writeJson(inspection.items()))
                .addValue("overall_result", inspection.overallResult())
                .addValue("inspection_status", inspection.inspectionStatus())
                .addValue("rectification_status", inspection.rectificationStatus())
                .addValue("reopen_status", inspection.reopenStatus())
                .addValue("inspected_at", timestampWithTimeZone(inspection.inspectedAt()))
                .addValue("submitted_at", timestampWithTimeZone(inspection.submittedAt()))
                .addValue("rechecked_at", timestampWithTimeZone(inspection.recheckedAt()))
                .addValue("approved_at", timestampWithTimeZone(inspection.approvedAt()))
                .addValue("operator_note", inspection.operatorNote())
                .addValue("evidence_attachment_ids", writeJson(inspection.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(inspection.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(inspection.updatedAt()))
                .addValue("inspector_id", inspection.inspectorId())
                .addValue("inspection_result", inspection.inspectionResult())
                .addValue("abnormal_found", inspection.abnormalFound())
                .addValue("abnormal_note", inspection.abnormalNote());
    }

    private MapSqlParameterSource safetyActionParameters(AmusementSafetyActionRecord action) {
        return new MapSqlParameterSource()
                .addValue("id", action.id())
                .addValue("inspection_id", action.inspectionId())
                .addValue("action_type", action.actionType())
                .addValue("action_result", action.actionResult())
                .addValue("comment", action.comment())
                .addValue("attachment_ids", writeJson(action.attachmentIds()))
                .addValue("created_at", timestampWithTimeZone(action.createdAt()));
    }

    private MapSqlParameterSource verificationParameters(AmusementVerificationRecord verification) {
        return new MapSqlParameterSource()
                .addValue("id", verification.id())
                .addValue("project_id", verification.projectId())
                .addValue("session_id", verification.sessionId())
                .addValue("credential_payload", verification.credentialPayload())
                .addValue("device_id", verification.deviceId())
                .addValue("staff_id", verification.staffId())
                .addValue("verification_result", verification.verificationResult())
                .addValue("failure_reason", verification.failureReason())
                .addValue("evidence_attachment_ids", writeJson(verification.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(verification.createdAt()))
                .addValue("verified_people", verification.verifiedPeople())
                .addValue("direction", verification.direction() == null ? "entry" : verification.direction())
                .addValue("released_at", timestampWithTimeZone(verification.releasedAt()))
                .addValue("entry_verification_id", verification.entryVerificationId())
                .addValue("entitlement_id", verification.entitlementId())
                .addValue("location_point_id", verification.locationPointId());
    }

    private MapSqlParameterSource manualOverrideParameters(AmusementManualOverrideRecord override) {
        return new MapSqlParameterSource()
                .addValue("id", override.id())
                .addValue("project_id", override.projectId())
                .addValue("session_id", override.sessionId())
                .addValue("override_type", override.overrideType())
                .addValue("reason", override.reason())
                .addValue("status", override.status())
                .addValue("operator_note", override.operatorNote())
                .addValue("evidence_attachment_ids", writeJson(override.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(override.createdAt()));
    }

    private MapSqlParameterSource deviceStatusParameters(AmusementDeviceStatusRecord status) {
        return new MapSqlParameterSource()
                .addValue("id", status.id())
                .addValue("project_id", status.projectId())
                .addValue("device_id", status.deviceId())
                .addValue("provider_code", status.providerCode())
                .addValue("status", status.status())
                .addValue("last_seen_at", timestampWithTimeZone(status.lastSeenAt()))
                .addValue("raw_status_json", writeJson(status.rawStatus()))
                .addValue("created_at", timestampWithTimeZone(status.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(status.updatedAt()));
    }

    private MapSqlParameterSource queueEntryParameters(AmusementQueueEntryRecord entry) {
        return new MapSqlParameterSource()
                .addValue("id", entry.id())
                .addValue("project_id", entry.projectId())
                .addValue("session_id", entry.sessionId())
                .addValue("customer_id", entry.customerId())
                .addValue("queue_no", entry.queueNo())
                .addValue("status", entry.status())
                .addValue("created_at", timestampWithTimeZone(entry.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(entry.updatedAt()));
    }

    private MapSqlParameterSource reservationParameters(AmusementReservationRecord reservation) {
        return new MapSqlParameterSource()
                .addValue("id", reservation.id())
                .addValue("project_id", reservation.projectId())
                .addValue("session_id", reservation.sessionId())
                .addValue("customer_id", reservation.customerId())
                .addValue("reservation_status", reservation.reservationStatus())
                .addValue("created_at", timestampWithTimeZone(reservation.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(reservation.updatedAt()));
    }

    private MapSqlParameterSource groupBookingParameters(AmusementGroupBookingRecord booking) {
        return new MapSqlParameterSource()
                .addValue("id", booking.id())
                .addValue("project_id", booking.projectId())
                .addValue("session_id", booking.sessionId())
                .addValue("booking_type", booking.bookingType())
                .addValue("customer_name", booking.customerName())
                .addValue("contact_phone", piiCipher.encrypt(booking.contactPhone()))
                .addValue("party_size", booking.partySize())
                .addValue("reserved_capacity", booking.reservedCapacity())
                .addValue("booking_status", booking.bookingStatus())
                .addValue("approval_note", booking.approvalNote())
                .addValue("operator_note", booking.operatorNote())
                .addValue("evidence_attachment_ids", writeJson(booking.evidenceAttachmentIds()))
                .addValue("approved_at", timestampWithTimeZone(booking.approvedAt()))
                .addValue("released_at", timestampWithTimeZone(booking.releasedAt()))
                .addValue("created_at", timestampWithTimeZone(booking.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(booking.updatedAt()))
                .addValue("paid_at", timestampWithTimeZone(booking.paidAt()));
    }


    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("json payload is invalid", exception);
        }
    }

}
