package com.complex.platform.facility;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Building inspection JDBC repository (V167). */
@Repository
class JdbcFacilityInspectionRepository implements FacilityInspectionRepository {

    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> LIST_STRING_TYPE = new TypeReference<>() {
    };
    private static final String TEMPLATE_COLS = """
            id, name, inspection_type, cycle_days, abnormal_mode, checklist::text AS checklist, scope_note,
            active, project_id, created_at, updated_at
            """;
    private static final String TASK_COLS = """
            id, template_id, inspection_type, status, title, schedule_mode, route_points::text AS route_points,
            assignee_id, scheduled_date, due_date, started_at, completed_at, work_order_id, project_id, space_id,
            created_at, updated_at
            """;
    private static final String RECORD_COLS = """
            id, task_id, result, photos::text AS photos, qr_code, gps_lat, gps_lng,
            checklist_result::text AS checklist_result, note, abnormal_note, inspector_id, recorded_at, created_at
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcFacilityInspectionRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public List<FacilityInspectionTemplateRecord> listTemplates() {
        return jdbcTemplate.query("SELECT " + TEMPLATE_COLS
                + " FROM facility_inspection_templates ORDER BY created_at DESC, id", this::mapTemplate);
    }

    @Override
    public List<FacilityInspectionTemplateRecord> listSchedulableTemplates() {
        return jdbcTemplate.query("SELECT " + TEMPLATE_COLS + """
                 FROM facility_inspection_templates
                 WHERE active = true AND cycle_days IS NOT NULL
                 ORDER BY updated_at ASC, id
                """, this::mapTemplate);
    }

    @Override
    public Optional<FacilityInspectionTemplateRecord> findTemplateById(String id) {
        return jdbcTemplate.query("SELECT " + TEMPLATE_COLS + " FROM facility_inspection_templates WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapTemplate).stream().findFirst();
    }

    @Override
    public void insertTemplate(FacilityInspectionTemplateRecord r) {
        jdbcTemplate.update("""
                INSERT INTO facility_inspection_templates (id, name, inspection_type, cycle_days, abnormal_mode,
                    checklist, scope_note, active, project_id, created_at, updated_at)
                VALUES (:id, :name, :inspection_type, :cycle_days, :abnormal_mode, CAST(:checklist AS jsonb),
                    :scope_note, :active, :project_id, :created_at, :updated_at)
                """, templateParams(r));
    }

    @Override
    public List<FacilityInspectionTaskRecord> listTasks() {
        return jdbcTemplate.query("SELECT " + TASK_COLS
                + " FROM facility_inspection_tasks ORDER BY created_at DESC, id", this::mapTask);
    }

    @Override
    public Optional<FacilityInspectionTaskRecord> findTaskById(String id) {
        return jdbcTemplate.query("SELECT " + TASK_COLS + " FROM facility_inspection_tasks WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapTask).stream().findFirst();
    }

    @Override
    public Optional<LocalDate> latestScheduledDateForTemplate(String templateId) {
        return jdbcTemplate.query("""
                SELECT scheduled_date
                  FROM facility_inspection_tasks
                 WHERE template_id = :template_id AND scheduled_date IS NOT NULL
                 ORDER BY scheduled_date DESC LIMIT 1
                """, new MapSqlParameterSource("template_id", templateId),
                (rs, n) -> rs.getObject("scheduled_date", LocalDate.class)).stream().findFirst();
    }

    @Override
    public boolean existsTaskForTemplateDate(String templateId, LocalDate scheduledDate) {
        return !jdbcTemplate.query("""
                SELECT id FROM facility_inspection_tasks
                 WHERE template_id = :template_id AND scheduled_date = :scheduled_date LIMIT 1
                """, new MapSqlParameterSource("template_id", templateId).addValue("scheduled_date", scheduledDate),
                (rs, n) -> rs.getString("id")).isEmpty();
    }

    @Override
    public void insertTask(FacilityInspectionTaskRecord r) {
        jdbcTemplate.update("""
                INSERT INTO facility_inspection_tasks (id, template_id, inspection_type, status, title, schedule_mode,
                    route_points, assignee_id, scheduled_date, due_date, started_at, completed_at, work_order_id,
                    project_id, space_id, created_at, updated_at)
                VALUES (:id, :template_id, :inspection_type, :status, :title, :schedule_mode,
                    CAST(:route_points AS jsonb), :assignee_id, :scheduled_date, :due_date, :started_at,
                    :completed_at, :work_order_id, :project_id, :space_id, :created_at, :updated_at)
                """, taskParams(r));
    }

    @Override
    public void updateTask(FacilityInspectionTaskRecord r) {
        jdbcTemplate.update("""
                UPDATE facility_inspection_tasks SET template_id=:template_id, inspection_type=:inspection_type,
                    status=:status, title=:title, schedule_mode=:schedule_mode,
                    route_points=CAST(:route_points AS jsonb), assignee_id=:assignee_id,
                    scheduled_date=:scheduled_date, due_date=:due_date, started_at=:started_at,
                    completed_at=:completed_at, work_order_id=:work_order_id, project_id=:project_id,
                    space_id=:space_id, updated_at=:updated_at
                 WHERE id=:id
                """, taskParams(r));
    }

    @Override
    public void updateTaskWorkOrderId(String taskId, String workOrderId, Instant updatedAt) {
        jdbcTemplate.update("""
                UPDATE facility_inspection_tasks
                   SET work_order_id = :work_order_id, updated_at = :updated_at
                 WHERE id = :task_id
                """, new MapSqlParameterSource("task_id", taskId)
                .addValue("work_order_id", workOrderId)
                .addValue("updated_at", timestampWithTimeZone(updatedAt)));
    }

    @Override
    public void insertRecord(FacilityInspectionRecordRecord r) {
        jdbcTemplate.update("""
                INSERT INTO facility_inspection_records (id, task_id, result, photos, qr_code, gps_lat, gps_lng,
                    checklist_result, note, abnormal_note, inspector_id, recorded_at, created_at)
                VALUES (:id, :task_id, :result, CAST(:photos AS jsonb), :qr_code, :gps_lat, :gps_lng,
                    CAST(:checklist_result AS jsonb), :note, :abnormal_note, :inspector_id, :recorded_at, :created_at)
                """, recordParams(r));
    }

    @Override
    public List<FacilityInspectionRecordRecord> listRecords(String taskId) {
        return jdbcTemplate.query("SELECT " + RECORD_COLS + """
                 FROM facility_inspection_records
                 WHERE task_id = :task_id
                 ORDER BY recorded_at DESC, id
                """, new MapSqlParameterSource("task_id", taskId), this::mapRecord);
    }

    private MapSqlParameterSource templateParams(FacilityInspectionTemplateRecord r) {
        return new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("name", r.name()).addValue("inspection_type", r.inspectionType())
                .addValue("cycle_days", r.cycleDays()).addValue("abnormal_mode", r.abnormalMode())
                .addValue("checklist", writeJson(r.checklist() == null ? List.of() : r.checklist()))
                .addValue("scope_note", r.scopeNote()).addValue("active", r.active())
                .addValue("project_id", r.projectId()).addValue("created_at", timestampWithTimeZone(r.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(r.updatedAt()));
    }

    private MapSqlParameterSource taskParams(FacilityInspectionTaskRecord r) {
        return new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("template_id", r.templateId())
                .addValue("inspection_type", r.inspectionType()).addValue("status", r.status())
                .addValue("title", r.title()).addValue("schedule_mode", r.scheduleMode())
                .addValue("route_points", writeJson(r.routePoints() == null ? List.of() : r.routePoints()))
                .addValue("assignee_id", r.assigneeId()).addValue("scheduled_date", r.scheduledDate())
                .addValue("due_date", r.dueDate()).addValue("started_at", timestampWithTimeZone(r.startedAt()))
                .addValue("completed_at", timestampWithTimeZone(r.completedAt()))
                .addValue("work_order_id", r.workOrderId()).addValue("project_id", r.projectId())
                .addValue("space_id", r.spaceId()).addValue("created_at", timestampWithTimeZone(r.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(r.updatedAt()));
    }

    private MapSqlParameterSource recordParams(FacilityInspectionRecordRecord r) {
        return new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("task_id", r.taskId()).addValue("result", r.result())
                .addValue("photos", writeJson(r.photos() == null ? List.of() : r.photos()))
                .addValue("qr_code", r.qrCode()).addValue("gps_lat", r.gpsLat()).addValue("gps_lng", r.gpsLng())
                .addValue("checklist_result", writeJson(r.checklistResult() == null ? List.of() : r.checklistResult()))
                .addValue("note", r.note()).addValue("abnormal_note", r.abnormalNote())
                .addValue("inspector_id", r.inspectorId()).addValue("recorded_at", timestampWithTimeZone(r.recordedAt()))
                .addValue("created_at", timestampWithTimeZone(r.createdAt()));
    }

    private FacilityInspectionTemplateRecord mapTemplate(ResultSet rs, int n) throws SQLException {
        return new FacilityInspectionTemplateRecord(
                rs.getString("id"), rs.getString("name"), rs.getString("inspection_type"),
                (Integer) rs.getObject("cycle_days"), rs.getString("abnormal_mode"), readListMap(rs.getString("checklist")),
                rs.getString("scope_note"), rs.getBoolean("active"), rs.getString("project_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private FacilityInspectionTaskRecord mapTask(ResultSet rs, int n) throws SQLException {
        return new FacilityInspectionTaskRecord(
                rs.getString("id"), rs.getString("template_id"), rs.getString("inspection_type"),
                rs.getString("status"), rs.getString("title"), rs.getString("schedule_mode"),
                readListMap(rs.getString("route_points")), rs.getString("assignee_id"),
                rs.getObject("scheduled_date", LocalDate.class), rs.getObject("due_date", LocalDate.class),
                nullableInstant(rs, "started_at"), nullableInstant(rs, "completed_at"),
                rs.getString("work_order_id"), rs.getString("project_id"), rs.getString("space_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private FacilityInspectionRecordRecord mapRecord(ResultSet rs, int n) throws SQLException {
        return new FacilityInspectionRecordRecord(
                rs.getString("id"), rs.getString("task_id"), rs.getString("result"), readListString(rs.getString("photos")),
                rs.getString("qr_code"), rs.getBigDecimal("gps_lat"), rs.getBigDecimal("gps_lng"),
                readListMap(rs.getString("checklist_result")), rs.getString("note"), rs.getString("abnormal_note"),
                rs.getString("inspector_id"), nullableInstant(rs, "recorded_at"), nullableInstant(rs, "created_at"));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize json", exception);
        }
    }

    private List<Map<String, Object>> readListMap(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<String> readListString(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_STRING_TYPE);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }
}
