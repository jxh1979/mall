package com.complex.platform.energy;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcEnergyRepository implements EnergyRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcEnergyRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public EnergyMeterRecord saveMeter(EnergyMeterRecord m) {
        jdbcTemplate.update("""
                INSERT INTO energy_meters (id, name, meter_type, space_id, unit, status, project_id, merchant_id, created_at, updated_at)
                VALUES (:id, :name, :meter_type, :space_id, :unit, :status, :project_id, :merchant_id, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name, meter_type = EXCLUDED.meter_type, space_id = EXCLUDED.space_id,
                    unit = EXCLUDED.unit, status = EXCLUDED.status, merchant_id = EXCLUDED.merchant_id,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", m.id()).addValue("name", m.name()).addValue("meter_type", m.meterType())
                .addValue("space_id", m.spaceId()).addValue("unit", m.unit()).addValue("status", m.status())
                .addValue("project_id", m.projectId()).addValue("merchant_id", m.merchantId())
                .addValue("created_at", timestampWithTimeZone(m.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(m.updatedAt())));
        return m;
    }

    @Override
    public List<EnergyMeterRecord> listMeters() {
        return jdbcTemplate.query("SELECT * FROM energy_meters ORDER BY created_at DESC, id LIMIT 500", this::mapMeter);
    }

    @Override
    public Optional<EnergyMeterRecord> findMeterById(String id) {
        return jdbcTemplate.query("SELECT * FROM energy_meters WHERE id = :id", Map.of("id", id), this::mapMeter)
                .stream().findFirst();
    }

    @Override
    public EnergyMeterReadingRecord saveReading(EnergyMeterReadingRecord r) {
        // 不用 ON CONFLICT：同表同期次唯一约束违例直接抛出，由 service 预检为友好错误，DB 约束兜底。
        jdbcTemplate.update("""
                INSERT INTO energy_meter_readings (id, meter_id, period, reading_value, consumption, recorded_by,
                        project_id, merchant_id, created_at)
                VALUES (:id, :meter_id, :period, :reading_value, :consumption, :recorded_by,
                        :project_id, :merchant_id, :created_at)
                """, new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("meter_id", r.meterId()).addValue("period", r.period())
                .addValue("reading_value", r.readingValue()).addValue("consumption", r.consumption())
                .addValue("recorded_by", r.recordedBy()).addValue("project_id", r.projectId())
                .addValue("merchant_id", r.merchantId())
                .addValue("created_at", timestampWithTimeZone(r.createdAt())));
        return r;
    }

    @Override
    public List<EnergyMeterReadingRecord> listReadings() {
        return jdbcTemplate.query("SELECT * FROM energy_meter_readings ORDER BY created_at DESC, id LIMIT 500", this::mapReading);
    }

    @Override
    public Optional<EnergyMeterReadingRecord> findReading(String meterId, String period) {
        return jdbcTemplate.query("SELECT * FROM energy_meter_readings WHERE meter_id = :m AND period = :p",
                Map.of("m", meterId, "p", period), this::mapReading).stream().findFirst();
    }

    @Override
    public Optional<BigDecimal> findLatestReadingValueBefore(String meterId, String period) {
        return jdbcTemplate.query("""
                SELECT reading_value FROM energy_meter_readings
                 WHERE meter_id = :m AND period < :p
                 ORDER BY period DESC LIMIT 1
                """, Map.of("m", meterId, "p", period), (rs, n) -> rs.getBigDecimal("reading_value"))
                .stream().findFirst();
    }

    private EnergyMeterRecord mapMeter(ResultSet rs, int n) throws SQLException {
        return new EnergyMeterRecord(
                rs.getString("id"), rs.getString("name"), rs.getString("meter_type"), rs.getString("space_id"),
                rs.getString("unit"), rs.getString("status"), rs.getString("project_id"), rs.getString("merchant_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private EnergyMeterReadingRecord mapReading(ResultSet rs, int n) throws SQLException {
        return new EnergyMeterReadingRecord(
                rs.getString("id"), rs.getString("meter_id"), rs.getString("period"),
                rs.getBigDecimal("reading_value"), rs.getBigDecimal("consumption"), rs.getString("recorded_by"),
                rs.getString("project_id"), rs.getString("merchant_id"), nullableInstant(rs, "created_at"));
    }
}
