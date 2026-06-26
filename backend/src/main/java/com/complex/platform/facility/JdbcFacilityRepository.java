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
            alarm_source, alarm_note, alarm_raised_at, project_id, merchant_id, created_at, updated_at
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcFacilityRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<FacilityEquipmentRecord> listEquipment() {
        return jdbcTemplate.query("SELECT " + EQUIP_COLS + " FROM facility_equipment ORDER BY created_at DESC, id",
                this::mapEquipment);
    }

    @Override
    public Optional<FacilityEquipmentRecord> findEquipmentById(String id) {
        return jdbcTemplate.query("SELECT " + EQUIP_COLS + " FROM facility_equipment WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapEquipment).stream().findFirst();
    }

    @Override
    public void insertEquipment(FacilityEquipmentRecord r) {
        jdbcTemplate.update("""
                INSERT INTO facility_equipment (id, name, equipment_type, space_id, brand, model, vendor,
                    responsible_user_id, status, maintenance_cycle_days, last_maintenance_at, next_maintenance_due,
                    install_date, warranty_until, alarm_source, alarm_note, alarm_raised_at, project_id, merchant_id)
                VALUES (:id, :name, :equipment_type, :space_id, :brand, :model, :vendor,
                    :responsible_user_id, :status, :maintenance_cycle_days, :last_maintenance_at, :next_maintenance_due,
                    :install_date, :warranty_until, :alarm_source, :alarm_note, :alarm_raised_at, :project_id, :merchant_id)
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
                    project_id=:project_id, merchant_id=:merchant_id, updated_at=now()
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
                .addValue("project_id", r.projectId()).addValue("merchant_id", r.merchantId());
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
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }
}
