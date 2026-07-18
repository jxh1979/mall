package com.complex.platform.energy;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.energy.EnergyRepository.EnergyContractSnapshot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 能耗 JDBC 仓储（S4）。 */
@Repository
class JdbcEnergyRepository implements EnergyRepository {

    private static final String METER_COLS = """
            id, name, meter_type, parent_meter_id, gather_object, space_id, merchant_id, project_id,
            read_mode, active, created_at, updated_at
            """;
    private static final String READING_COLS = """
            id, meter_id, period, reading_value, prev_reading_value, consumption, status, abnormal_reason,
            read_mode, reader_id, read_at, reviewed_by, reviewed_at, created_at, updated_at
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcEnergyRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public List<EnergyMeterRecord> listMeters() {
        return jdbcTemplate.query("SELECT " + METER_COLS + " FROM energy_meters ORDER BY created_at DESC, id",
                this::mapMeter);
    }

    @Override
    public Optional<EnergyMeterRecord> findMeterById(String id) {
        return jdbcTemplate.query("SELECT " + METER_COLS + " FROM energy_meters WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapMeter).stream().findFirst();
    }

    @Override
    public EnergyMeterRecord saveMeter(EnergyMeterRecord meter) {
        jdbcTemplate.update("""
                INSERT INTO energy_meters (
                    id, name, meter_type, parent_meter_id, gather_object, space_id, merchant_id, project_id,
                    read_mode, active, created_at, updated_at
                )
                VALUES (
                    :id, :name, :meter_type, :parent_meter_id, :gather_object, :space_id, :merchant_id, :project_id,
                    :read_mode, :active, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    meter_type = EXCLUDED.meter_type,
                    parent_meter_id = EXCLUDED.parent_meter_id,
                    gather_object = EXCLUDED.gather_object,
                    space_id = EXCLUDED.space_id,
                    merchant_id = EXCLUDED.merchant_id,
                    project_id = EXCLUDED.project_id,
                    read_mode = EXCLUDED.read_mode,
                    active = EXCLUDED.active,
                    updated_at = EXCLUDED.updated_at
                """, meterParams(meter));
        return meter;
    }

    @Override
    public List<EnergyMeterReadingRecord> listReadings(String period) {
        if (period == null) {
            return jdbcTemplate.query("SELECT " + READING_COLS
                    + " FROM energy_meter_readings ORDER BY read_at DESC, id", this::mapReading);
        }
        return jdbcTemplate.query("SELECT " + READING_COLS
                        + " FROM energy_meter_readings WHERE period = :period ORDER BY read_at DESC, id",
                new MapSqlParameterSource("period", period), this::mapReading);
    }

    @Override
    public Optional<EnergyMeterReadingRecord> findReadingById(String id) {
        return jdbcTemplate.query("SELECT " + READING_COLS + " FROM energy_meter_readings WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapReading).stream().findFirst();
    }

    @Override
    public Optional<EnergyMeterReadingRecord> findReadingByMeterAndPeriod(String meterId, String period) {
        return jdbcTemplate.query("""
                SELECT %s FROM energy_meter_readings
                 WHERE meter_id = :meter_id AND period = :period
                """.formatted(READING_COLS), new MapSqlParameterSource()
                .addValue("meter_id", meterId).addValue("period", period), this::mapReading).stream().findFirst();
    }

    @Override
    public Optional<EnergyMeterReadingRecord> findLatestReadingBefore(String meterId, String period) {
        return jdbcTemplate.query("""
                SELECT %s FROM energy_meter_readings
                 WHERE meter_id = :meter_id AND period < :period
                 ORDER BY period DESC LIMIT 1
                """.formatted(READING_COLS), new MapSqlParameterSource()
                .addValue("meter_id", meterId).addValue("period", period), this::mapReading).stream().findFirst();
    }

    @Override
    public EnergyMeterReadingRecord saveReading(EnergyMeterReadingRecord reading) {
        jdbcTemplate.update("""
                INSERT INTO energy_meter_readings (
                    id, meter_id, period, reading_value, prev_reading_value, consumption, status, abnormal_reason,
                    read_mode, reader_id, read_at, reviewed_by, reviewed_at, created_at, updated_at
                )
                VALUES (
                    :id, :meter_id, :period, :reading_value, :prev_reading_value, :consumption, :status,
                    :abnormal_reason, :read_mode, :reader_id, :read_at, :reviewed_by, :reviewed_at,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    reading_value = EXCLUDED.reading_value,
                    prev_reading_value = EXCLUDED.prev_reading_value,
                    consumption = EXCLUDED.consumption,
                    status = EXCLUDED.status,
                    abnormal_reason = EXCLUDED.abnormal_reason,
                    read_mode = EXCLUDED.read_mode,
                    reader_id = EXCLUDED.reader_id,
                    read_at = EXCLUDED.read_at,
                    reviewed_by = EXCLUDED.reviewed_by,
                    reviewed_at = EXCLUDED.reviewed_at,
                    updated_at = EXCLUDED.updated_at
                """, readingParams(reading));
        return reading;
    }

    @Override
    public Optional<EnergyAnomalyThresholdRecord> findThresholdByMeterType(String meterType) {
        return jdbcTemplate.query("""
                SELECT id, meter_type, surge_pct, drop_pct, active, created_at, updated_at
                  FROM energy_anomaly_thresholds
                 WHERE meter_type = :meter_type AND active = TRUE
                """, new MapSqlParameterSource("meter_type", meterType), this::mapThreshold).stream().findFirst();
    }

    @Override
    public List<EnergyMeterReadingRecord> listChildReadings(String parentMeterId, String period) {
        return jdbcTemplate.query("""
                SELECT r.id, r.meter_id, r.period, r.reading_value, r.prev_reading_value, r.consumption,
                       r.status, r.abnormal_reason, r.read_mode, r.reader_id, r.read_at,
                       r.reviewed_by, r.reviewed_at, r.created_at, r.updated_at
                  FROM energy_meter_readings r
                  JOIN energy_meters m ON m.id = r.meter_id
                 WHERE m.parent_meter_id = :parent_meter_id AND r.period = :period
                 ORDER BY m.id
                """, new MapSqlParameterSource()
                .addValue("parent_meter_id", parentMeterId).addValue("period", period), this::mapReading);
    }

    @Override
    public List<EnergyContractSnapshot> listEffectiveContracts(String period) {
        YearMonth ym = YearMonth.parse(period);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return jdbcTemplate.query("""
                SELECT c.id AS contract_id, c.merchant_id, c.project_id, c.energy_fee_basis,
                       c.rent_terms::text AS rent_terms, m.area_square_meter
                  FROM contracts c
                  LEFT JOIN merchants m ON m.id = c.merchant_id
                 WHERE c.lifecycle_status = 'effective'
                   AND (c.effective_date IS NULL OR c.effective_date <= :period_end)
                   AND (c.expiry_date IS NULL OR c.expiry_date >= :period_start)
                """, new MapSqlParameterSource().addValue("period_start", start).addValue("period_end", end),
                this::mapContract);
    }

    @Override
    public BigDecimal sumConsumptionByGatherObject(String gatherObject, String period, String projectId) {
        BigDecimal sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(r.consumption), 0)
                  FROM energy_meter_readings r
                  JOIN energy_meters m ON m.id = r.meter_id
                 WHERE m.gather_object = :gather_object
                   AND r.period = :period
                   AND r.status = 'reviewed'
                   AND (:project_id IS NULL OR m.project_id = :project_id)
                """, new MapSqlParameterSource().addValue("gather_object", gatherObject)
                .addValue("period", period).addValue("project_id", projectId, java.sql.Types.VARCHAR), BigDecimal.class);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public BigDecimal sumMerchantConsumption(String merchantId, String period, String projectId) {
        BigDecimal sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(r.consumption), 0)
                  FROM energy_meter_readings r
                  JOIN energy_meters m ON m.id = r.meter_id
                 WHERE m.gather_object = 'merchant'
                   AND m.merchant_id = :merchant_id
                   AND r.period = :period
                   AND r.status = 'reviewed'
                   AND (:project_id IS NULL OR m.project_id = :project_id)
                """, new MapSqlParameterSource().addValue("merchant_id", merchantId)
                .addValue("period", period).addValue("project_id", projectId, java.sql.Types.VARCHAR), BigDecimal.class);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public boolean energyFeeArExists(String merchantId, String period) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM ar_ap_items
                     WHERE direction = 'receivable' AND source_type = 'energy_fee' AND source_id = :source_id
                )
                """, new MapSqlParameterSource("source_id", EnergyService.energyFeeSourceId(merchantId, period)),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public EnergyFeeSettlementRecord insertEnergyFeeAr(EnergyFeeSettlementRecord record) {
        String token = record.sourceId().substring("energy_fee_".length());
        LocalDate dueDate = YearMonth.parse(record.period()).atDay(1);
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (
                    id, item_no, direction, counterparty_id, source_type, source_id,
                    amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period,
                    status, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :item_no, 'receivable', :merchant_id, 'energy_fee', :source_id,
                    :amount, 0, :amount, 'CNY', :due_date, :period, 'pending_collection',
                    :created_at, :created_at, :merchant_id
                )
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", record.arApItemId())
                .addValue("item_no", "ENERGY-" + token)
                .addValue("merchant_id", record.merchantId())
                .addValue("source_id", record.sourceId())
                .addValue("amount", record.amountCent())
                .addValue("due_date", dueDate)
                .addValue("period", record.period())
                .addValue("created_at", timestampWithTimeZone(record.createdAt())));
        jdbcTemplate.update("""
                INSERT INTO ar_ap_item_sources (id, ar_ap_item_id, source_type, source_id, amount_cent, created_at)
                VALUES (:id, :ar_ap_item_id, 'energy_fee', :source_id, :amount, :created_at)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", "arsrc_" + token.substring(0, 28))
                .addValue("ar_ap_item_id", record.arApItemId())
                .addValue("source_id", record.sourceId())
                .addValue("amount", record.amountCent())
                .addValue("created_at", timestampWithTimeZone(record.createdAt())));
        return record;
    }

    private MapSqlParameterSource meterParams(EnergyMeterRecord r) {
        return new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("name", r.name()).addValue("meter_type", r.meterType())
                .addValue("parent_meter_id", r.parentMeterId()).addValue("gather_object", r.gatherObject())
                .addValue("space_id", r.spaceId()).addValue("merchant_id", r.merchantId())
                .addValue("project_id", r.projectId()).addValue("read_mode", r.readMode())
                .addValue("active", r.active()).addValue("created_at", timestampWithTimeZone(r.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(r.updatedAt()));
    }

    private MapSqlParameterSource readingParams(EnergyMeterReadingRecord r) {
        return new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("meter_id", r.meterId()).addValue("period", r.period())
                .addValue("reading_value", r.readingValue()).addValue("prev_reading_value", r.prevReadingValue())
                .addValue("consumption", r.consumption()).addValue("status", r.status())
                .addValue("abnormal_reason", r.abnormalReason()).addValue("read_mode", r.readMode())
                .addValue("reader_id", r.readerId()).addValue("read_at", timestampWithTimeZone(r.readAt()))
                .addValue("reviewed_by", r.reviewedBy())
                .addValue("reviewed_at", timestampWithTimeZone(r.reviewedAt()))
                .addValue("created_at", timestampWithTimeZone(r.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(r.updatedAt()));
    }

    private EnergyMeterRecord mapMeter(ResultSet rs, int n) throws SQLException {
        return new EnergyMeterRecord(rs.getString("id"), rs.getString("name"), rs.getString("meter_type"),
                rs.getString("parent_meter_id"), rs.getString("gather_object"), rs.getString("space_id"),
                rs.getString("merchant_id"), rs.getString("project_id"), rs.getString("read_mode"),
                rs.getBoolean("active"), nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private EnergyMeterReadingRecord mapReading(ResultSet rs, int n) throws SQLException {
        return new EnergyMeterReadingRecord(rs.getString("id"), rs.getString("meter_id"), rs.getString("period"),
                rs.getBigDecimal("reading_value"), rs.getBigDecimal("prev_reading_value"),
                rs.getBigDecimal("consumption"), rs.getString("status"), rs.getString("abnormal_reason"),
                rs.getString("read_mode"), rs.getString("reader_id"), nullableInstant(rs, "read_at"),
                rs.getString("reviewed_by"), nullableInstant(rs, "reviewed_at"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private EnergyAnomalyThresholdRecord mapThreshold(ResultSet rs, int n) throws SQLException {
        return new EnergyAnomalyThresholdRecord(rs.getString("id"), rs.getString("meter_type"),
                rs.getBigDecimal("surge_pct"), rs.getBigDecimal("drop_pct"), rs.getBoolean("active"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private EnergyContractSnapshot mapContract(ResultSet rs, int n) throws SQLException {
        Map<String, Object> terms = readMap(rs.getString("rent_terms"));
        return new EnergyContractSnapshot(rs.getString("contract_id"), rs.getString("merchant_id"),
                rs.getString("project_id"), rs.getString("energy_fee_basis"),
                numLong(terms.getOrDefault("energyFeeCent", terms.get("energy_fee_cent"))),
                rs.getBigDecimal("area_square_meter"),
                numLong(terms.getOrDefault("energyUnitPriceCent", terms.get("energy_unit_price_cent"))));
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Long numLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }
}
