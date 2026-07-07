package com.complex.platform.facility;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 楼宇设施仓储 JDBC 实现（V165 设备台账）。 */
@Repository
class JdbcFacilityRepository implements FacilityRepository {

    private static final String EQUIP_COLS = """
            id, name, equipment_type, space_id, brand, model, vendor, responsible_user_id, status,
            maintenance_cycle_days, last_maintenance_at, next_maintenance_due, install_date, warranty_until,
            alarm_source, alarm_note, alarm_raised_at, project_id, merchant_id, created_at, updated_at,
            archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcFacilityRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<FacilityEquipmentRecord> listEquipment() {
        return jdbcTemplate.query("SELECT " + EQUIP_COLS
                + " FROM facility_equipment WHERE delete_status <> 'approved' ORDER BY created_at DESC, id",
                this::mapEquipment);
    }

    @Override
    public List<FacilityEquipmentRecord> listEquipment(boolean includeArchived) {
        String sql = "SELECT " + EQUIP_COLS + " FROM facility_equipment WHERE delete_status <> 'approved'"
                + (includeArchived ? "" : " AND archived_at IS NULL")
                + " ORDER BY created_at DESC, id";
        return jdbcTemplate.query(sql, this::mapEquipment);
    }

    @Override
    public long countEquipmentReferences(String id) {
        // 关联工单 facility_work_orders.equipment_id 为活引用（不级联），有则拒删。
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM facility_work_orders WHERE equipment_id = :id",
                new MapSqlParameterSource("id", id), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public void deleteEquipment(String id) {
        jdbcTemplate.update("UPDATE facility_equipment SET delete_status = 'approved', updated_at = now() WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    @Override
    public List<FacilityEquipmentRecord> listEquipmentByProject(String projectId) {
        return jdbcTemplate.query("SELECT " + EQUIP_COLS + """
                 FROM facility_equipment
                 WHERE project_id = :project_id
                 ORDER BY created_at DESC, id
                """, new MapSqlParameterSource("project_id", projectId), this::mapEquipment);
    }

    @Override
    public Optional<FacilityEquipmentRecord> findEquipmentById(String id) {
        return jdbcTemplate.query("SELECT " + EQUIP_COLS
                + " FROM facility_equipment WHERE id = :id AND delete_status <> 'approved'",
                new MapSqlParameterSource("id", id), this::mapEquipment).stream().findFirst();
    }

    @Override
    public void insertEquipment(FacilityEquipmentRecord r) {
        jdbcTemplate.update("""
                INSERT INTO facility_equipment (id, name, equipment_type, space_id, brand, model, vendor,
                    responsible_user_id, status, maintenance_cycle_days, last_maintenance_at, next_maintenance_due,
                    install_date, warranty_until, alarm_source, alarm_note, alarm_raised_at, project_id, merchant_id,
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason)
                VALUES (:id, :name, :equipment_type, :space_id, :brand, :model, :vendor,
                    :responsible_user_id, :status, :maintenance_cycle_days, :last_maintenance_at, :next_maintenance_due,
                    :install_date, :warranty_until, :alarm_source, :alarm_note, :alarm_raised_at, :project_id, :merchant_id,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason)
                """, params(r));
    }

    @Override
    public void updateEquipment(FacilityEquipmentRecord r) {
        jdbcTemplate.update("""
                UPDATE facility_equipment SET name=:name, equipment_type=:equipment_type, space_id=:space_id,
                    brand=:brand, model=:model, vendor=:vendor, responsible_user_id=:responsible_user_id,
                    status=:status, maintenance_cycle_days=:maintenance_cycle_days, last_maintenance_at=:last_maintenance_at,
                    next_maintenance_due=:next_maintenance_due, install_date=:install_date, warranty_until=:warranty_until,
                    alarm_source=:alarm_source, alarm_note=:alarm_note, alarm_raised_at=:alarm_raised_at,
                    project_id=:project_id, merchant_id=:merchant_id, updated_at=now(),
                    archived_at=:archived_at, archived_by=:archived_by, delete_status=:delete_status,
                    delete_requested_by=:delete_requested_by, delete_requested_at=:delete_requested_at,
                    delete_reason=:delete_reason
                 WHERE id=:id
                """, params(r));
    }

    @Override
    public List<FacilityEquipmentRecord> listMaintenanceDue(LocalDate asOf) {
        return jdbcTemplate.query("SELECT " + EQUIP_COLS + """
                 FROM facility_equipment
                 WHERE next_maintenance_due IS NOT NULL AND next_maintenance_due <= :as_of
                   AND status NOT IN ('retired','disabled')
                 ORDER BY next_maintenance_due, id
                """, new MapSqlParameterSource("as_of", asOf), this::mapEquipment);
    }

    @Override
    public List<FacilityEquipmentRecord> listMaintenanceDueByProject(LocalDate asOf, String projectId) {
        return jdbcTemplate.query("SELECT " + EQUIP_COLS + """
                 FROM facility_equipment
                 WHERE next_maintenance_due IS NOT NULL AND next_maintenance_due <= :as_of
                   AND status NOT IN ('retired','disabled')
                   AND project_id = :project_id
                 ORDER BY next_maintenance_due, id
                """, new MapSqlParameterSource("as_of", asOf).addValue("project_id", projectId), this::mapEquipment);
    }

    private MapSqlParameterSource params(FacilityEquipmentRecord r) {
        return new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("name", r.name()).addValue("equipment_type", r.equipmentType())
                .addValue("space_id", r.spaceId()).addValue("brand", r.brand()).addValue("model", r.model())
                .addValue("vendor", r.vendor()).addValue("responsible_user_id", r.responsibleUserId())
                .addValue("status", r.status()).addValue("maintenance_cycle_days", r.maintenanceCycleDays())
                .addValue("last_maintenance_at",
                        r.lastMaintenanceAt() == null ? null : timestampWithTimeZone(r.lastMaintenanceAt()))
                .addValue("next_maintenance_due", r.nextMaintenanceDue())
                .addValue("install_date", r.installDate()).addValue("warranty_until", r.warrantyUntil())
                .addValue("alarm_source", r.alarmSource()).addValue("alarm_note", r.alarmNote())
                .addValue("alarm_raised_at", r.alarmRaisedAt() == null ? null : timestampWithTimeZone(r.alarmRaisedAt()))
                .addValue("project_id", r.projectId()).addValue("merchant_id", r.merchantId())
                .addValue("archived_at", r.archivedAt() == null ? null : timestampWithTimeZone(r.archivedAt()))
                .addValue("archived_by", r.archivedBy())
                .addValue("delete_status", r.deleteStatus() == null ? "none" : r.deleteStatus())
                .addValue("delete_requested_by", r.deleteRequestedBy())
                .addValue("delete_requested_at",
                        r.deleteRequestedAt() == null ? null : timestampWithTimeZone(r.deleteRequestedAt()))
                .addValue("delete_reason", r.deleteReason());
    }

    private FacilityEquipmentRecord mapEquipment(ResultSet rs, int n) throws SQLException {
        return new FacilityEquipmentRecord(
                rs.getString("id"), rs.getString("name"), rs.getString("equipment_type"), rs.getString("space_id"),
                rs.getString("brand"), rs.getString("model"), rs.getString("vendor"),
                rs.getString("responsible_user_id"), rs.getString("status"),
                (Integer) rs.getObject("maintenance_cycle_days"), nullableInstant(rs, "last_maintenance_at"),
                rs.getObject("next_maintenance_due", LocalDate.class), rs.getObject("install_date", LocalDate.class),
                rs.getObject("warranty_until", LocalDate.class), rs.getString("alarm_source"),
                rs.getString("alarm_note"), nullableInstant(rs, "alarm_raised_at"),
                rs.getString("project_id"), rs.getString("merchant_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"),
                nullableInstant(rs, "archived_at"), rs.getString("archived_by"), rs.getString("delete_status"),
                rs.getString("delete_requested_by"), nullableInstant(rs, "delete_requested_at"),
                rs.getString("delete_reason"));
    }

    @Override
    public void setEquipmentArchiveReason(String id, String reason) {
        jdbcTemplate.update("UPDATE facility_equipment SET archive_reason = :reason WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
    }
}
