package com.complex.platform.facility;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcFacilityRepository implements FacilityRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcFacilityRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
}
