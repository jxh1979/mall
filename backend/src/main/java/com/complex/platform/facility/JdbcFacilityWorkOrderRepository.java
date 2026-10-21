package com.complex.platform.facility;

// 文件版本：V0.01

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

/** 楼宇维保/物业工单 JDBC 仓储（V166）。 */
@Repository
class JdbcFacilityWorkOrderRepository implements FacilityWorkOrderRepository {

    private static final String WORK_ORDER_COLS = """
            id, code, work_order_type, priority, status, title, description, source, space_id, equipment_id,
            reporter_id, reporter_type, assignee_id, response_due_at, handle_due_at, assigned_at, responded_at,
            completed_at, verified_at, verifier_id, satisfaction, overtime, resolution, project_id, merchant_id,
            created_at, updated_at, confirmer_id, reporter_confirmed_at, reporter_confirmed_by
            """;
    private static final String POLICY_COLS = """
            id, work_order_type, priority, response_minutes, handle_minutes, requires_acceptance, acceptance_by,
            active, created_at, updated_at
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcFacilityWorkOrderRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<FacilityWorkOrderPolicyRecord> findPolicy(String workOrderType, String priority) {
        return jdbcTemplate.query("SELECT " + POLICY_COLS + """
                 FROM facility_work_order_policy
                 WHERE work_order_type = :type AND priority = :priority AND active = true
                """, Map.of("type", workOrderType, "priority", priority), this::mapPolicy).stream().findFirst();
    }

    @Override
    public FacilityWorkOrderPolicyRecord savePolicy(FacilityWorkOrderPolicyRecord r) {
        jdbcTemplate.update("""
                INSERT INTO facility_work_order_policy (id, work_order_type, priority, response_minutes,
                    handle_minutes, requires_acceptance, acceptance_by, active, created_at, updated_at)
                VALUES (:id, :work_order_type, :priority, :response_minutes, :handle_minutes,
                    :requires_acceptance, :acceptance_by, :active, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    work_order_type = EXCLUDED.work_order_type,
                    priority = EXCLUDED.priority,
                    response_minutes = EXCLUDED.response_minutes,
                    handle_minutes = EXCLUDED.handle_minutes,
                    requires_acceptance = EXCLUDED.requires_acceptance,
                    acceptance_by = EXCLUDED.acceptance_by,
                    active = EXCLUDED.active,
                    updated_at = EXCLUDED.updated_at
                """, policyParams(r));
        return r;
    }

    @Override
    public void insertWorkOrder(FacilityWorkOrderRecord r) {
        jdbcTemplate.update("""
                INSERT INTO facility_work_orders (id, code, work_order_type, priority, status, title, description,
                    source, space_id, equipment_id, reporter_id, reporter_type, assignee_id, response_due_at,
                    handle_due_at, assigned_at, responded_at, completed_at, verified_at, verifier_id, satisfaction,
                    overtime, resolution, project_id, merchant_id, created_at, updated_at, confirmer_id,
                    reporter_confirmed_at, reporter_confirmed_by)
                VALUES (:id, :code, :work_order_type, :priority, :status, :title, :description,
                    :source, :space_id, :equipment_id, :reporter_id, :reporter_type, :assignee_id, :response_due_at,
                    :handle_due_at, :assigned_at, :responded_at, :completed_at, :verified_at, :verifier_id,
                    :satisfaction, :overtime, :resolution, :project_id, :merchant_id, :created_at, :updated_at,
                    :confirmer_id, :reporter_confirmed_at, :reporter_confirmed_by)
                """, workOrderParams(r));
    }

    @Override
    public void updateWorkOrder(FacilityWorkOrderRecord r) {
        jdbcTemplate.update("""
                UPDATE facility_work_orders SET work_order_type=:work_order_type, priority=:priority, status=:status,
                    title=:title, description=:description, source=:source, space_id=:space_id,
                    equipment_id=:equipment_id, reporter_id=:reporter_id, reporter_type=:reporter_type,
                    assignee_id=:assignee_id, response_due_at=:response_due_at, handle_due_at=:handle_due_at,
                    assigned_at=:assigned_at, responded_at=:responded_at, completed_at=:completed_at,
                    verified_at=:verified_at, verifier_id=:verifier_id, satisfaction=:satisfaction,
                    overtime=:overtime, resolution=:resolution, project_id=:project_id, merchant_id=:merchant_id,
                    updated_at=:updated_at, confirmer_id=:confirmer_id, reporter_confirmed_at=:reporter_confirmed_at,
                    reporter_confirmed_by=:reporter_confirmed_by
                 WHERE id=:id
                """, workOrderParams(r));
    }

    @Override
    public Optional<FacilityWorkOrderRecord> findById(String id) {
        return jdbcTemplate.query("SELECT " + WORK_ORDER_COLS + " FROM facility_work_orders WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapWorkOrder).stream().findFirst();
    }

    @Override
    public Optional<FacilityWorkOrderRecord> findByIdForUpdate(String id) {
        return jdbcTemplate.query("SELECT " + WORK_ORDER_COLS + " FROM facility_work_orders WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", id), this::mapWorkOrder).stream().findFirst();
    }

    @Override
    public List<FacilityWorkOrderRecord> list() {
        return jdbcTemplate.query("SELECT " + WORK_ORDER_COLS
                + " FROM facility_work_orders ORDER BY created_at DESC, id", this::mapWorkOrder);
    }

    @Override
    public java.util.Map<String, int[]> countCompletionByAssignees(
            java.util.Collection<String> assigneeIds, String projectId) {
        java.util.Map<String, int[]> result = new java.util.LinkedHashMap<>();
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return result;
        }
        jdbcTemplate.query("""
                SELECT assignee_id, COUNT(*) AS total, COUNT(completed_at) AS done
                  FROM facility_work_orders
                 WHERE assignee_id IN (:ids) AND project_id = :project_id
                 GROUP BY assignee_id
                """, new MapSqlParameterSource("ids", assigneeIds).addValue("project_id", projectId),
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> result.put(rs.getString("assignee_id"),
                        new int[] {rs.getInt("total"), rs.getInt("done")}));
        return result;
    }

    @Override
    public List<FacilityWorkOrderRecord> listAssignedToMe(String userId) {
        // 第十二批操作员裁决第 13 条：派给本人的，加「待我提单确认」的（关联设备的报修＝两道，提单确认未做，待验收或已验收）。
        return jdbcTemplate.query("SELECT " + WORK_ORDER_COLS + """
                 FROM facility_work_orders
                 WHERE assignee_id = :user_id
                    OR (confirmer_id = :user_id AND work_order_type = 'repair' AND equipment_id IS NOT NULL
                        AND reporter_confirmed_at IS NULL AND status IN ('pending_acceptance','accepted'))
                 ORDER BY created_at DESC, id
                 LIMIT 200
                """, new MapSqlParameterSource("user_id", userId), this::mapWorkOrder);
    }

    @Override
    public List<FacilityWorkOrderRecord> listAssignedTo(String assigneeId, String projectId) {
        return jdbcTemplate.query("SELECT " + WORK_ORDER_COLS + """
                 FROM facility_work_orders
                 WHERE assignee_id = :assignee_id AND project_id = :project_id
                 ORDER BY created_at DESC, id
                """, new MapSqlParameterSource("assignee_id", assigneeId).addValue("project_id", projectId),
                this::mapWorkOrder);
    }

    @Override
    public List<FacilityWorkOrderRecord> listOverdue(Instant now) {
        return jdbcTemplate.query("SELECT " + WORK_ORDER_COLS + """
                 FROM facility_work_orders
                 WHERE handle_due_at IS NOT NULL AND handle_due_at < :now AND overtime = false
                   AND status IN ('pending_assign','assigned','processing','pending_review','pending_acceptance','accepted')
                 ORDER BY handle_due_at ASC, id
                """, new MapSqlParameterSource("now", timestampWithTimeZone(now)), this::mapWorkOrder);
    }

    private MapSqlParameterSource workOrderParams(FacilityWorkOrderRecord r) {
        return new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("code", r.code()).addValue("work_order_type", r.workOrderType())
                .addValue("priority", r.priority()).addValue("status", r.status()).addValue("title", r.title())
                .addValue("description", r.description()).addValue("source", r.source())
                .addValue("space_id", r.spaceId()).addValue("equipment_id", r.equipmentId())
                .addValue("reporter_id", r.reporterId()).addValue("reporter_type", r.reporterType())
                .addValue("assignee_id", r.assigneeId())
                .addValue("response_due_at", timestampWithTimeZone(r.responseDueAt()))
                .addValue("handle_due_at", timestampWithTimeZone(r.handleDueAt()))
                .addValue("assigned_at", timestampWithTimeZone(r.assignedAt()))
                .addValue("responded_at", timestampWithTimeZone(r.respondedAt()))
                .addValue("completed_at", timestampWithTimeZone(r.completedAt()))
                .addValue("verified_at", timestampWithTimeZone(r.verifiedAt()))
                .addValue("verifier_id", r.verifierId()).addValue("satisfaction", r.satisfaction())
                .addValue("overtime", r.overtime()).addValue("resolution", r.resolution())
                .addValue("project_id", r.projectId()).addValue("merchant_id", r.merchantId())
                .addValue("created_at", timestampWithTimeZone(r.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(r.updatedAt()))
                .addValue("confirmer_id", r.confirmerId())
                .addValue("reporter_confirmed_at", timestampWithTimeZone(r.reporterConfirmedAt()))
                .addValue("reporter_confirmed_by", r.reporterConfirmedBy());
    }

    private MapSqlParameterSource policyParams(FacilityWorkOrderPolicyRecord r) {
        return new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("work_order_type", r.workOrderType())
                .addValue("priority", r.priority()).addValue("response_minutes", r.responseMinutes())
                .addValue("handle_minutes", r.handleMinutes()).addValue("requires_acceptance", r.requiresAcceptance())
                .addValue("acceptance_by", r.acceptanceBy()).addValue("active", r.active())
                .addValue("created_at", timestampWithTimeZone(r.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(r.updatedAt()));
    }

    private FacilityWorkOrderRecord mapWorkOrder(ResultSet rs, int n) throws SQLException {
        return new FacilityWorkOrderRecord(
                rs.getString("id"), rs.getString("code"), rs.getString("work_order_type"),
                rs.getString("priority"), rs.getString("status"), rs.getString("title"),
                rs.getString("description"), rs.getString("source"), rs.getString("space_id"),
                rs.getString("equipment_id"), rs.getString("reporter_id"), rs.getString("reporter_type"),
                rs.getString("assignee_id"), nullableInstant(rs, "response_due_at"),
                nullableInstant(rs, "handle_due_at"), nullableInstant(rs, "assigned_at"),
                nullableInstant(rs, "responded_at"), nullableInstant(rs, "completed_at"),
                nullableInstant(rs, "verified_at"), rs.getString("verifier_id"), nullableInt(rs, "satisfaction"),
                rs.getBoolean("overtime"), rs.getString("resolution"), rs.getString("project_id"),
                rs.getString("merchant_id"), nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"),
                rs.getString("confirmer_id"), nullableInstant(rs, "reporter_confirmed_at"),
                rs.getString("reporter_confirmed_by"));
    }

    private FacilityWorkOrderPolicyRecord mapPolicy(ResultSet rs, int n) throws SQLException {
        return new FacilityWorkOrderPolicyRecord(
                rs.getString("id"), rs.getString("work_order_type"), rs.getString("priority"),
                rs.getInt("response_minutes"), rs.getInt("handle_minutes"), rs.getBoolean("requires_acceptance"),
                rs.getString("acceptance_by"), rs.getBoolean("active"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
