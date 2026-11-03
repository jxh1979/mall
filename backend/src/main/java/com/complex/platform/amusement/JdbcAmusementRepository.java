package com.complex.platform.amusement;

// 文件版本：V0.02
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
                    -- CG-02：整行保存改了营业态（人工改状态）即清空系统自动停运原因；营业态未变则保留。
                    auto_paused_reason = CASE WHEN amusement_projects.open_status = EXCLUDED.open_status
                                              THEN amusement_projects.auto_paused_reason ELSE NULL END,
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
                .addValue("point_id", pointId, java.sql.Types.VARCHAR)
                .addValue("device_id", deviceId, java.sql.Types.VARCHAR);
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

    // V341（R-12 软删）：绑定行全字段列（存档轴 + 删除轴），所有读取路径统一走这一串，避免漏字段。
    private static final String PROJECT_ENTITLEMENT_COLUMNS = """
            id, project_id, entitlement_type, created_at,
            archived_at, archived_by, archive_reason,
            delete_status, delete_requested_by, delete_requested_at, delete_reason
            """;

    // V341：生效中绑定的统一过滤谓词（未存档 + 未审批通过删除）。走 idx_amusement_project_entitlements_active_list 偏索引。
    private static final String PROJECT_ENTITLEMENT_ACTIVE_FILTER =
            " archived_at IS NULL AND delete_status <> 'approved' ";

    @Override
    public List<String> listProjectEntitlementTypes(String projectId) {
        // 核销绑定校验读取路径：只认生效绑定；已存档/墓碑绑定视同不存在（不再收紧核销、也不误放行）。
        return jdbcTemplate.queryForList("""
                SELECT entitlement_type
                  FROM amusement_project_entitlements
                 WHERE project_id = :project_id
                   AND""" + PROJECT_ENTITLEMENT_ACTIVE_FILTER + """
                 ORDER BY entitlement_type
                """, Map.of("project_id", projectId), String.class);
    }

    @Override
    public AmusementProjectEntitlementRecord saveProjectEntitlement(AmusementProjectEntitlementRecord entitlement) {
        // V341：唯一键 (project_id, entitlement_type) 使已存档/墓碑行继续占位，DO NOTHING 会让「重新绑定」静默失效。
        // 故复绑=恢复——命中冲突时清空存档/删除标记（保留原 id/created_at，审计留痕由 audit_events 承载）。
        jdbcTemplate.update("""
                INSERT INTO amusement_project_entitlements (
                    id, project_id, entitlement_type, created_at
                )
                VALUES (:id, :project_id, :entitlement_type, :created_at)
                ON CONFLICT (project_id, entitlement_type) DO UPDATE SET
                    archived_at = NULL,
                    archived_by = NULL,
                    archive_reason = NULL,
                    delete_status = 'none',
                    delete_requested_by = NULL,
                    delete_requested_at = NULL,
                    delete_reason = NULL
                """, new MapSqlParameterSource()
                .addValue("id", entitlement.id())
                .addValue("project_id", entitlement.projectId())
                .addValue("entitlement_type", entitlement.entitlementType())
                .addValue("created_at", timestampWithTimeZone(entitlement.createdAt())));
        // 返回库内权威记录（命中既有绑定时取既有 id/created_at，保证幂等可观测）。
        return findProjectEntitlement(entitlement.projectId(), entitlement.entitlementType()).orElse(entitlement);
    }

    @Override
    public Optional<AmusementProjectEntitlementRecord> findProjectEntitlement(String projectId, String entitlementType) {
        // 管理端单条读取：含已存档/墓碑行（存档→申请删除→审批→恢复整链都要读得到）。
        return jdbcTemplate.query("SELECT " + PROJECT_ENTITLEMENT_COLUMNS + """
                  FROM amusement_project_entitlements
                 WHERE project_id = :project_id AND entitlement_type = :entitlement_type
                """, Map.of("project_id", projectId, "entitlement_type", entitlementType),
                rowMapper::mapProjectEntitlement).stream().findFirst();
    }

    @Override
    public List<AmusementProjectEntitlementRecord> listProjectEntitlements(String projectId, boolean includeArchived) {
        return jdbcTemplate.query("SELECT " + PROJECT_ENTITLEMENT_COLUMNS + """
                  FROM amusement_project_entitlements
                 WHERE project_id = :project_id
                   AND (:include_archived = TRUE OR (""" + PROJECT_ENTITLEMENT_ACTIVE_FILTER + """
                   ))
                 ORDER BY entitlement_type
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("include_archived", includeArchived), rowMapper::mapProjectEntitlement);
    }

    @Override
    public int archiveProjectEntitlement(String projectId, String entitlementType, String reason, String actorId) {
        return jdbcTemplate.update("""
                UPDATE amusement_project_entitlements
                   SET archived_at = :archived_at, archived_by = :archived_by, archive_reason = :reason
                 WHERE project_id = :project_id AND entitlement_type = :entitlement_type
                   AND archived_at IS NULL AND delete_status = 'none'
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("entitlement_type", entitlementType)
                .addValue("archived_at", timestampWithTimeZone(Instant.now()))
                .addValue("archived_by", actorId)
                .addValue("reason", reason));
    }

    @Override
    public int restoreProjectEntitlement(String projectId, String entitlementType) {
        // 恢复：清存档标记，并把删除轴回退 none（pending 申请撤回；approved 墓碑复活——服务层已按最高权限闸把守）。
        return jdbcTemplate.update("""
                UPDATE amusement_project_entitlements
                   SET archived_at = NULL, archived_by = NULL, archive_reason = NULL,
                       delete_status = 'none', delete_requested_by = NULL,
                       delete_requested_at = NULL, delete_reason = NULL
                 WHERE project_id = :project_id AND entitlement_type = :entitlement_type
                """, Map.of("project_id", projectId, "entitlement_type", entitlementType));
    }

    @Override
    public int requestDeleteProjectEntitlement(String projectId, String entitlementType, String reason, String actorId) {
        return jdbcTemplate.update("""
                UPDATE amusement_project_entitlements
                   SET delete_status = 'pending', delete_requested_by = :by,
                       delete_requested_at = :at, delete_reason = :reason
                 WHERE project_id = :project_id AND entitlement_type = :entitlement_type
                   AND archived_at IS NOT NULL AND delete_status = 'none'
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("entitlement_type", entitlementType)
                .addValue("by", actorId)
                .addValue("at", timestampWithTimeZone(Instant.now()))
                .addValue("reason", reason));
    }

    @Override
    public int decideDeleteProjectEntitlement(String projectId, String entitlementType, boolean approved) {
        // approved 落墓碑（行不删、读取路径过滤），rejected 回 none（申请人/理由留库作留痕）。
        return jdbcTemplate.update("""
                UPDATE amusement_project_entitlements
                   SET delete_status = :status
                 WHERE project_id = :project_id AND entitlement_type = :entitlement_type
                   AND delete_status = 'pending'
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("entitlement_type", entitlementType)
                .addValue("status", approved ? "approved" : "none"));
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
        lockProjectForSafety(device.projectId());
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
        lockProjectForSafety(projectId);
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
        lockProjectForSafety(projectId);
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
        lockProjectForSafety(rule.projectId());
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
    public List<AmusementSessionRecord> listSessionsScoped(boolean unrestricted,
            java.util.Set<String> complexIds, String merchantId) {
        if (!unrestricted && complexIds.isEmpty()) return List.of();
        String sql = """
                SELECT s.id, s.project_id, s.start_time, s.end_time, s.capacity, s.used_capacity, s.locked_capacity,
                       s.status, s.evidence_attachment_ids, s.created_at, s.updated_at
                  FROM amusement_sessions s
                  JOIN amusement_projects p ON p.id = s.project_id
                 WHERE s.complex_id IS NOT NULL
                """;
        if (!unrestricted) sql += " AND s.complex_id IN (:complex_ids)";
        if (merchantId != null) sql += " AND p.merchant_id = :merchant_id";
        sql += " ORDER BY s.start_time DESC, s.id";
        return jdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("complex_ids", complexIds).addValue("merchant_id", merchantId), rowMapper::mapSession);
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
                   AND status = 'open'
                """, Map.of("id", sessionId, "people_count", peopleCount, "updated_at", timestampWithTimeZone(Instant.now())));
        return updated > 0;
    }

    @Override
    public CapacityClaimResult claimCapacityAcrossDimensions(CapacityClaimContext context) {
        if (context.projectSafetyCapacity() <= 0) {
            throw com.complex.platform.common.api.ApiException.conflict(
                    "AMUSEMENT_SAFETY_CAPACITY_MISSING", "项目尚未配置有效硬安全容量");
        }
        int peopleCount = Math.max(context.peopleCount(), 1);
        if (!incrementSnapshotIfAvailable(context, "project", null, null,
                context.projectSafetyCapacity(), peopleCount, Instant.now())) {
            return new CapacityClaimResult(false, "project_safety", List.of("project_safety"));
        }
        return new CapacityClaimResult(true, null, List.of("project_safety"));
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
     *   <li>赢家把该入场占用的项目在场人数 -N（下限 0）。</li>
     *   <li>写 direction=exit 记录（entry_verification_id 唯一约束再兜底防重）。</li>
     * </ol>
     * 返回 true=本次真实释放；false=入场已释放（幂等跳过，不重复减容量、不新增 exit）。
     */
    @Override
    public boolean releaseEntryConcurrentCapacity(AmusementVerificationRecord entry, AmusementVerificationRecord exitRecord, Instant now) {
        int marked = jdbcTemplate.update("""
                UPDATE amusement_verifications
                   SET released_at = :released_at, release_source = 'exit', release_source_id = :exit_id
                 WHERE id = :id
                   AND direction = 'entry'
                   AND verification_result = 'success'
                   AND released_at IS NULL
                """, new MapSqlParameterSource()
                .addValue("id", entry.id())
                .addValue("exit_id", exitRecord.id())
                .addValue("released_at", timestampWithTimeZone(now)));
        if (marked == 0) {
            return false;
        }
        int people = Math.max(entry.verifiedPeople(), 1);
        // 与入场项目人数占用对称，不更新历史场次或点位容量。
        decrementConcurrentSnapshot(entry.projectId(), "project", null, null, people, now);
        saveVerification(exitRecord);
        return true;
    }

    // ---- CG-03 在玩人数系统自动清零（系统主体写口，无人工入口）----

    @Override
    public int[] countStaleConcurrentSnapshots(String projectId, Instant before) {
        return jdbcTemplate.query("""
                SELECT COUNT(*) AS rows, COALESCE(SUM(occupied_count), 0) AS people
                  FROM amusement_capacity_snapshot
                 WHERE project_id = :project_id AND dimension IN ('project', 'point_device')
                   AND occupied_count > 0 AND updated_at < :before
                """, new MapSqlParameterSource().addValue("project_id", projectId)
                .addValue("before", timestampWithTimeZone(before)),
                (rs, n) -> new int[] {rs.getInt("rows"), rs.getInt("people")}).stream().findFirst().orElse(new int[] {0, 0});
    }

    @Override
    public int zeroStaleConcurrentSnapshots(String projectId, Instant before, Instant now) {
        return jdbcTemplate.update("""
                UPDATE amusement_capacity_snapshot
                   SET occupied_count = 0, updated_at = :updated_at
                 WHERE project_id = :project_id AND dimension IN ('project', 'point_device')
                   AND occupied_count > 0 AND updated_at < :before
                """, new MapSqlParameterSource().addValue("project_id", projectId)
                .addValue("before", timestampWithTimeZone(before)).addValue("updated_at", timestampWithTimeZone(now)));
    }

    @Override
    public int[] countSessionConcurrentSnapshots(String projectId, String sessionId) {
        return jdbcTemplate.query("""
                SELECT COUNT(*) AS rows, COALESCE(SUM(occupied_count), 0) AS people
                  FROM amusement_capacity_snapshot
                 WHERE project_id = :project_id AND occupied_count > 0
                   AND ((dimension = 'session' AND session_id = :session_id) OR dimension IN ('project', 'point_device'))
                """, new MapSqlParameterSource().addValue("project_id", projectId).addValue("session_id", sessionId),
                (rs, n) -> new int[] {rs.getInt("rows"), rs.getInt("people")}).stream().findFirst().orElse(new int[] {0, 0});
    }

    @Override
    public int zeroSessionConcurrentSnapshots(String projectId, String sessionId, Instant now) {
        return jdbcTemplate.update("""
                UPDATE amusement_capacity_snapshot
                   SET occupied_count = 0, updated_at = :updated_at
                 WHERE project_id = :project_id AND occupied_count > 0
                   AND ((dimension = 'session' AND session_id = :session_id) OR dimension IN ('project', 'point_device'))
                """, new MapSqlParameterSource().addValue("project_id", projectId).addValue("session_id", sessionId)
                .addValue("updated_at", timestampWithTimeZone(now)));
    }

    @Override
    public List<AmusementSessionRecord> listEndedUnclosedSessions(Instant now) {
        return jdbcTemplate.query("""
                SELECT id, project_id, start_time, end_time, capacity, used_capacity, locked_capacity, status,
                       evidence_attachment_ids, created_at, updated_at
                  FROM amusement_sessions
                 WHERE status IN ('scheduled', 'open') AND end_time <= :now
                 ORDER BY end_time, id
                """, new MapSqlParameterSource().addValue("now", timestampWithTimeZone(now)), rowMapper::mapSession);
    }

    @Override
    public boolean closeSessionIfEnded(String sessionId, Instant now) {
        return jdbcTemplate.update("""
                UPDATE amusement_sessions
                   SET status = 'closed', updated_at = :updated_at
                 WHERE id = :id AND status IN ('scheduled', 'open') AND end_time <= :now
                """, new MapSqlParameterSource().addValue("id", sessionId).addValue("now", timestampWithTimeZone(now))
                .addValue("updated_at", timestampWithTimeZone(now))) > 0;
    }

    @Override
    public boolean insertConcurrentResetLogIfAbsent(AmusementConcurrentResetLogRecord r) {
        return jdbcTemplate.update("""
                INSERT INTO amusement_concurrent_reset_log (id, project_id, scope, business_date, session_id, cleared_rows,
                        cleared_people, boundary_at, reset_at)
                VALUES (:id, :project_id, :scope, :business_date, :session_id, :cleared_rows, :cleared_people, :boundary_at, :reset_at)
                ON CONFLICT DO NOTHING
                """, new MapSqlParameterSource().addValue("id", r.id()).addValue("project_id", r.projectId())
                .addValue("scope", r.scope()).addValue("business_date", java.sql.Date.valueOf(r.businessDate()))
                .addValue("session_id", r.sessionId()).addValue("cleared_rows", r.clearedRows())
                .addValue("cleared_people", r.clearedPeople()).addValue("boundary_at", timestampWithTimeZone(r.boundaryAt()))
                .addValue("reset_at", timestampWithTimeZone(r.resetAt()))) > 0;
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
        lockProjectForSafety(inspection.projectId());
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
                ON CONFLICT (device_id) DO UPDATE SET
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
    public Map<String, Integer> countWaitingQueueEntriesByProject() {
        return jdbcTemplate.query("""
                SELECT project_id, count(*) AS waiting
                  FROM amusement_queue_entries
                 WHERE status = 'waiting'
                 GROUP BY project_id
                """, (rs, rowNum) -> Map.entry(rs.getString("project_id"), rs.getInt("waiting")))
                .stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Integer> countOpenFaultIncidentsByProject() {
        return jdbcTemplate.query("""
                SELECT amusement_project_id, count(*) AS faults
                  FROM amusement_incidents
                 WHERE incident_type = 'fault'
                   AND status IN ('created', 'processing', 'pending_external')
                   AND amusement_project_id IS NOT NULL
                 GROUP BY amusement_project_id
                """, (rs, rowNum) -> Map.entry(rs.getString("amusement_project_id"), rs.getInt("faults")))
                .stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // AUD-013 真分页: 商户维经 project_id 子查询下推, nullable -> 平台全量; total 取 COUNT 真值。
    @Override
    public com.complex.platform.common.api.PageResponse<AmusementQueueEntryRecord> listQueueEntriesPaged(
            String merchantId, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 50 : Math.min(size, 200);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR);
        String where = """
                 WHERE (:merchant_id IS NULL
                        OR project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id))
                """;
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM amusement_queue_entries" + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<AmusementQueueEntryRecord> items = jdbcTemplate.query("""
                SELECT id, project_id, session_id, customer_id, queue_no, status, created_at, updated_at
                  FROM amusement_queue_entries
                """ + where + """
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, rowMapper::mapQueueEntry);
        return new com.complex.platform.common.api.PageResponse<>(
                items, safePage, safeSize, total == null ? 0L : total);
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

    // AUD-013 真分页。
    @Override
    public com.complex.platform.common.api.PageResponse<AmusementReservationRecord> listReservationsPaged(
            String merchantId, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 50 : Math.min(size, 200);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR);
        String where = """
                 WHERE (:merchant_id IS NULL
                        OR project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id))
                """;
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM amusement_reservations" + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<AmusementReservationRecord> items = jdbcTemplate.query("""
                SELECT id, project_id, session_id, customer_id, reservation_status, created_at, updated_at
                  FROM amusement_reservations
                """ + where + """
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, rowMapper::mapReservation);
        return new com.complex.platform.common.api.PageResponse<>(
                items, safePage, safeSize, total == null ? 0L : total);
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
        // CG-02：经本通用 CAS 改动营业态（检修 maintenance→open、巡检 closed/maintenance→open 等）即不再是系统自动停运，
        // 一并清空 auto_paused_reason。
        int updated = jdbcTemplate.update("""
                UPDATE amusement_projects
                   SET open_status = :new_status,
                       auto_paused_reason = NULL,
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

    /**
     * CG-02 出场回落自动恢复开放：单条原子 UPDATE 把「仍为硬超容自动停运」与「项目维在玩人数 < 硬安全容量」两个条件
     * 和状态翻转绑在一起——并发多路出场在同一行上串行化，只有一路更新到行（返回 true），其余看到的已是 open。
     * 项目维快照行键：dimension=project 且 session/point/device 均空（与 claimCapacityAcrossDimensions 写入键一致）。
     * 硬安全容量取 amusement_projects.capacity 当前配置（>0 才算配置了硬上限）。
     */
    @Override
    public void lockProjectForSafety(String projectId) {
        jdbcTemplate.query("SELECT id FROM amusement_projects WHERE id = :id FOR NO KEY UPDATE",
                Map.of("id", projectId), (rs, n) -> rs.getString("id"));
    }

    @Override
    public boolean isHardCapacityPaused(String projectId) {
        return jdbcTemplate.query("""
                SELECT id FROM amusement_projects WHERE id = :id
                  AND open_status = 'paused' AND auto_paused_reason = 'hard_capacity'
                """, Map.of("id", projectId), (rs, n) -> rs.getString("id")).size() == 1;
    }

    @Override
    public boolean reopenProjectIfHardCapacityRecovered(String projectId, Instant now) {
        int updated = jdbcTemplate.update("""
                UPDATE amusement_projects p
                   SET open_status = 'open',
                       auto_paused_reason = NULL,
                       updated_at = :updated_at
                 WHERE p.id = :id
                   AND p.open_status = 'paused'
                   AND p.auto_paused_reason = 'hard_capacity'
                   AND p.capacity > 0
                   AND EXISTS (
                       SELECT 1
                         FROM amusement_capacity_snapshot s
                        WHERE s.project_id = p.id
                          AND s.dimension = 'project'
                          AND COALESCE(s.session_id, '') = ''
                          AND COALESCE(s.location_point_id, '') = ''
                          AND COALESCE(s.device_id, '') = ''
                          AND s.occupied_count < p.capacity)
                """, new MapSqlParameterSource()
                .addValue("id", projectId)
                .addValue("updated_at", timestampWithTimeZone(now)));
        return updated == 1;
    }

    @Override
    public java.util.OptionalInt findProjectInPlayCount(String projectId) {
        List<Integer> rows = jdbcTemplate.query("""
                SELECT occupied_count
                  FROM amusement_capacity_snapshot
                 WHERE project_id = :project_id
                   AND dimension = 'project'
                   AND COALESCE(session_id, '') = ''
                   AND COALESCE(location_point_id, '') = ''
                   AND COALESCE(device_id, '') = ''
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("project_id", projectId),
                (rs, n) -> rs.getInt("occupied_count"));
        return rows.isEmpty() ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(rows.get(0));
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

    // AUD-013 真分页。
    @Override
    public com.complex.platform.common.api.PageResponse<AmusementGroupBookingRecord> listGroupBookingsPaged(
            String merchantId, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 50 : Math.min(size, 200);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR);
        String where = """
                 WHERE (:merchant_id IS NULL
                        OR project_id IN (SELECT id FROM amusement_projects WHERE merchant_id = :merchant_id))
                """;
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM amusement_group_bookings" + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<AmusementGroupBookingRecord> items = jdbcTemplate.query("""
                SELECT id, project_id, session_id, booking_type, customer_name, contact_phone,
                       party_size, reserved_capacity, booking_status, approval_note, operator_note,
                       evidence_attachment_ids, approved_at, released_at, created_at, updated_at, paid_at
                  FROM amusement_group_bookings
                """ + where + """
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, rowMapper::mapGroupBooking);
        return new com.complex.platform.common.api.PageResponse<>(
                items, safePage, safeSize, total == null ? 0L : total);
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
                       SET updated_at = :updated_at
                      FROM candidate
                     WHERE session.id = candidate.session_id
                       AND session.status IN ('scheduled', 'open')
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
                       SET updated_at = :updated_at
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
