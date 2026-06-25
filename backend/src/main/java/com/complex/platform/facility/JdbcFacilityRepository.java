package com.complex.platform.facility;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

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

@Repository
class JdbcFacilityRepository implements FacilityRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcFacilityRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public FacilityEquipmentRecord saveEquipment(FacilityEquipmentRecord e) {
        jdbcTemplate.update("""
                INSERT INTO facility_equipment (id, name, equipment_type, space_id, vendor, model, install_date,
                        warranty_until, status, maintenance_cycle_days, last_maintenance_at, project_id, merchant_id,
                        created_at, updated_at)
                VALUES (:id, :name, :equipment_type, :space_id, :vendor, :model, :install_date,
                        :warranty_until, :status, :maintenance_cycle_days, :last_maintenance_at, :project_id, :merchant_id,
                        :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name, equipment_type = EXCLUDED.equipment_type, space_id = EXCLUDED.space_id,
                    vendor = EXCLUDED.vendor, model = EXCLUDED.model, install_date = EXCLUDED.install_date,
                    warranty_until = EXCLUDED.warranty_until, status = EXCLUDED.status,
                    maintenance_cycle_days = EXCLUDED.maintenance_cycle_days, last_maintenance_at = EXCLUDED.last_maintenance_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", e.id()).addValue("name", e.name()).addValue("equipment_type", e.equipmentType())
                .addValue("space_id", e.spaceId()).addValue("vendor", e.vendor()).addValue("model", e.model())
                .addValue("install_date", e.installDate()).addValue("warranty_until", e.warrantyUntil())
                .addValue("status", e.status()).addValue("maintenance_cycle_days", e.maintenanceCycleDays())
                .addValue("last_maintenance_at", timestampWithTimeZone(e.lastMaintenanceAt()))
                .addValue("project_id", e.projectId()).addValue("merchant_id", e.merchantId())
                .addValue("created_at", timestampWithTimeZone(e.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(e.updatedAt())));
        return e;
    }

    @Override
    public List<FacilityEquipmentRecord> listEquipment() {
        return jdbcTemplate.query("SELECT * FROM facility_equipment ORDER BY created_at DESC, id LIMIT 500", this::map);
    }

    @Override
    public Optional<FacilityEquipmentRecord> findEquipmentById(String id) {
        return jdbcTemplate.query("SELECT * FROM facility_equipment WHERE id = :id", Map.of("id", id), this::map)
                .stream().findFirst();
    }

    private FacilityEquipmentRecord map(ResultSet rs, int n) throws SQLException {
        return new FacilityEquipmentRecord(
                rs.getString("id"), rs.getString("name"), rs.getString("equipment_type"), rs.getString("space_id"),
                rs.getString("vendor"), rs.getString("model"),
                rs.getObject("install_date", LocalDate.class), rs.getObject("warranty_until", LocalDate.class),
                rs.getString("status"), (Integer) rs.getObject("maintenance_cycle_days"),
                nullableInstant(rs, "last_maintenance_at"), rs.getString("project_id"), rs.getString("merchant_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    @Override
    public FacilityWorkOrderRecord saveWorkOrder(FacilityWorkOrderRecord w) {
        jdbcTemplate.update("""
                INSERT INTO facility_work_orders (id, equipment_id, space_id, title, description, category, priority,
                        status, assignee, reported_by, sla_due_at, resolved_at, project_id, merchant_id, created_at, updated_at)
                VALUES (:id, :equipment_id, :space_id, :title, :description, :category, :priority,
                        :status, :assignee, :reported_by, :sla_due_at, :resolved_at, :project_id, :merchant_id, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    equipment_id = EXCLUDED.equipment_id, space_id = EXCLUDED.space_id, title = EXCLUDED.title,
                    description = EXCLUDED.description, category = EXCLUDED.category, priority = EXCLUDED.priority,
                    status = EXCLUDED.status, assignee = EXCLUDED.assignee, reported_by = EXCLUDED.reported_by,
                    sla_due_at = EXCLUDED.sla_due_at, resolved_at = EXCLUDED.resolved_at, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", w.id()).addValue("equipment_id", w.equipmentId()).addValue("space_id", w.spaceId())
                .addValue("title", w.title()).addValue("description", w.description()).addValue("category", w.category())
                .addValue("priority", w.priority()).addValue("status", w.status()).addValue("assignee", w.assignee())
                .addValue("reported_by", w.reportedBy())
                .addValue("sla_due_at", timestampWithTimeZone(w.slaDueAt()))
                .addValue("resolved_at", timestampWithTimeZone(w.resolvedAt()))
                .addValue("project_id", w.projectId()).addValue("merchant_id", w.merchantId())
                .addValue("created_at", timestampWithTimeZone(w.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(w.updatedAt())));
        return w;
    }

    @Override
    public List<FacilityWorkOrderRecord> listWorkOrders() {
        return jdbcTemplate.query("SELECT * FROM facility_work_orders ORDER BY created_at DESC, id LIMIT 500", this::mapWorkOrder);
    }

    @Override
    public Optional<FacilityWorkOrderRecord> findWorkOrderById(String id) {
        return jdbcTemplate.query("SELECT * FROM facility_work_orders WHERE id = :id", Map.of("id", id), this::mapWorkOrder)
                .stream().findFirst();
    }

    private FacilityWorkOrderRecord mapWorkOrder(ResultSet rs, int n) throws SQLException {
        return new FacilityWorkOrderRecord(
                rs.getString("id"), rs.getString("equipment_id"), rs.getString("space_id"), rs.getString("title"),
                rs.getString("description"), rs.getString("category"), rs.getString("priority"), rs.getString("status"),
                rs.getString("assignee"), rs.getString("reported_by"),
                nullableInstant(rs, "sla_due_at"), nullableInstant(rs, "resolved_at"),
                rs.getString("project_id"), rs.getString("merchant_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    @Override
    public FacilityInspectionTemplateRecord saveInspectionTemplate(FacilityInspectionTemplateRecord t) {
        jdbcTemplate.update("""
                INSERT INTO facility_inspection_templates (id, name, equipment_type, items, status, project_id, merchant_id, created_at, updated_at)
                VALUES (:id, :name, :equipment_type, CAST(:items AS jsonb), :status, :project_id, :merchant_id, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name, equipment_type = EXCLUDED.equipment_type, items = EXCLUDED.items,
                    status = EXCLUDED.status, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", t.id()).addValue("name", t.name()).addValue("equipment_type", t.equipmentType())
                .addValue("items", writeJson(t.items())).addValue("status", t.status())
                .addValue("project_id", t.projectId()).addValue("merchant_id", t.merchantId())
                .addValue("created_at", timestampWithTimeZone(t.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(t.updatedAt())));
        return t;
    }

    @Override
    public List<FacilityInspectionTemplateRecord> listInspectionTemplates() {
        return jdbcTemplate.query("SELECT * FROM facility_inspection_templates ORDER BY created_at DESC, id LIMIT 500", this::mapTemplate);
    }

    @Override
    public boolean upsertInspectionTask(FacilityInspectionTaskRecord t) {
        int affected = jdbcTemplate.update("""
                INSERT INTO facility_inspection_tasks (id, equipment_id, space_id, due_at, status, generated_at,
                        completed_record_id, completed_at, project_id, merchant_id, created_at, updated_at)
                VALUES (:id, :equipment_id, :space_id, :due_at, :status, :generated_at,
                        :completed_record_id, :completed_at, :project_id, :merchant_id, :created_at, :updated_at)
                ON CONFLICT (equipment_id, due_at) DO NOTHING
                """, taskParams(t));
        return affected > 0;
    }

    @Override
    public FacilityInspectionTaskRecord saveInspectionTask(FacilityInspectionTaskRecord t) {
        jdbcTemplate.update("""
                INSERT INTO facility_inspection_tasks (id, equipment_id, space_id, due_at, status, generated_at,
                        completed_record_id, completed_at, project_id, merchant_id, created_at, updated_at)
                VALUES (:id, :equipment_id, :space_id, :due_at, :status, :generated_at,
                        :completed_record_id, :completed_at, :project_id, :merchant_id, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status, completed_record_id = EXCLUDED.completed_record_id,
                    completed_at = EXCLUDED.completed_at, updated_at = EXCLUDED.updated_at
                """, taskParams(t));
        return t;
    }

    @Override
    public List<FacilityInspectionTaskRecord> listInspectionTasks() {
        return jdbcTemplate.query("SELECT * FROM facility_inspection_tasks ORDER BY due_at ASC, id LIMIT 500", this::mapTask);
    }

    @Override
    public Optional<FacilityInspectionTaskRecord> findInspectionTaskById(String id) {
        return jdbcTemplate.query("SELECT * FROM facility_inspection_tasks WHERE id = :id", Map.of("id", id), this::mapTask)
                .stream().findFirst();
    }

    @Override
    public int markOverdueInspectionTasks(Instant now) {
        return jdbcTemplate.update("""
                UPDATE facility_inspection_tasks SET status = 'overdue', updated_at = :now
                 WHERE status = 'pending' AND due_at < :now
                """, new MapSqlParameterSource().addValue("now", timestampWithTimeZone(now)));
    }

    @Override
    public FacilityInspectionRecordRecord saveInspectionRecord(FacilityInspectionRecordRecord r) {
        jdbcTemplate.update("""
                INSERT INTO facility_inspection_records (id, task_id, equipment_id, inspector, result, findings,
                        checked_items, project_id, merchant_id, created_at)
                VALUES (:id, :task_id, :equipment_id, :inspector, :result, :findings,
                        CAST(:checked_items AS jsonb), :project_id, :merchant_id, :created_at)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("task_id", r.taskId()).addValue("equipment_id", r.equipmentId())
                .addValue("inspector", r.inspector()).addValue("result", r.result()).addValue("findings", r.findings())
                .addValue("checked_items", writeJson(r.checkedItems()))
                .addValue("project_id", r.projectId()).addValue("merchant_id", r.merchantId())
                .addValue("created_at", timestampWithTimeZone(r.createdAt())));
        return r;
    }

    private MapSqlParameterSource taskParams(FacilityInspectionTaskRecord t) {
        return new MapSqlParameterSource()
                .addValue("id", t.id()).addValue("equipment_id", t.equipmentId()).addValue("space_id", t.spaceId())
                .addValue("due_at", timestampWithTimeZone(t.dueAt())).addValue("status", t.status())
                .addValue("generated_at", timestampWithTimeZone(t.generatedAt()))
                .addValue("completed_record_id", t.completedRecordId())
                .addValue("completed_at", timestampWithTimeZone(t.completedAt()))
                .addValue("project_id", t.projectId()).addValue("merchant_id", t.merchantId())
                .addValue("created_at", timestampWithTimeZone(t.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(t.updatedAt()));
    }

    private FacilityInspectionTemplateRecord mapTemplate(ResultSet rs, int n) throws SQLException {
        return new FacilityInspectionTemplateRecord(
                rs.getString("id"), rs.getString("name"), rs.getString("equipment_type"),
                readJson(rs.getString("items")), rs.getString("status"),
                rs.getString("project_id"), rs.getString("merchant_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private FacilityInspectionTaskRecord mapTask(ResultSet rs, int n) throws SQLException {
        return new FacilityInspectionTaskRecord(
                rs.getString("id"), rs.getString("equipment_id"), rs.getString("space_id"),
                nullableInstant(rs, "due_at"), rs.getString("status"), nullableInstant(rs, "generated_at"),
                rs.getString("completed_record_id"), nullableInstant(rs, "completed_at"),
                rs.getString("project_id"), rs.getString("merchant_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private String writeJson(List<String> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("cannot serialize json", e);
        }
    }

    private List<String> readJson(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("cannot parse json", e);
        }
    }
}
