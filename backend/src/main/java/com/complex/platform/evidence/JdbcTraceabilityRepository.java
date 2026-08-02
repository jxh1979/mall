package com.complex.platform.evidence;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcTraceabilityRepository implements TraceabilityRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcTraceabilityRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public TraceabilityProcessInstanceRecord saveProcessInstance(TraceabilityProcessInstanceRecord record) {
        jdbcTemplate.update("""
                INSERT INTO business_process_instances (
                    id, process_type, business_object_type, business_object_id, project_id, merchant_id,
                    venue_id, area_id, location_point_id, started_event_id, current_step_code, status,
                    started_at, completed_at
                )
                VALUES (
                    :id, :process_type, :business_object_type, :business_object_id, :project_id, :merchant_id,
                    :venue_id, :area_id, :location_point_id, :started_event_id, :current_step_code, :status,
                    :started_at, :completed_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    current_step_code = EXCLUDED.current_step_code,
                    status = EXCLUDED.status,
                    completed_at = EXCLUDED.completed_at
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("process_type", record.processType())
                .addValue("business_object_type", record.businessObjectType())
                .addValue("business_object_id", record.businessObjectId())
                .addValue("project_id", record.projectId())
                .addValue("merchant_id", record.merchantId())
                .addValue("venue_id", record.venueId())
                .addValue("area_id", record.areaId())
                .addValue("location_point_id", record.locationPointId())
                .addValue("started_event_id", record.startedEventId())
                .addValue("current_step_code", record.currentStepCode())
                .addValue("status", record.status())
                .addValue("started_at", timestampWithTimeZone(record.startedAt()))
                .addValue("completed_at", timestampWithTimeZone(record.completedAt())));
        return record;
    }

    @Override
    public Optional<TraceabilityProcessInstanceRecord> findProcessInstanceById(String id) {
        List<TraceabilityProcessInstanceRecord> result = jdbcTemplate.query(
                "SELECT * FROM business_process_instances WHERE id = :id",
                Map.of("id", id), this::mapInstance);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public TraceabilityStepRecord saveStep(TraceabilityStepRecord record) {
        jdbcTemplate.update("""
                INSERT INTO business_process_step_records (
                    id, process_instance_id, step_code, step_name, actor_type, actor_id, terminal_type,
                    device_id, location_point_id, event_id, auto_captured, capture_source,
                    requires_manual_reason, result, occurred_at
                )
                VALUES (
                    :id, :process_instance_id, :step_code, :step_name, :actor_type, :actor_id, :terminal_type,
                    :device_id, :location_point_id, :event_id, :auto_captured, :capture_source,
                    :requires_manual_reason, :result, :occurred_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    result = EXCLUDED.result
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("process_instance_id", record.processInstanceId())
                .addValue("step_code", record.stepCode())
                .addValue("step_name", record.stepName())
                .addValue("actor_type", record.actorType())
                .addValue("actor_id", record.actorId())
                .addValue("terminal_type", record.terminalType())
                .addValue("device_id", record.deviceId())
                .addValue("location_point_id", record.locationPointId())
                .addValue("event_id", record.eventId())
                .addValue("auto_captured", record.autoCaptured())
                .addValue("capture_source", record.captureSource())
                .addValue("requires_manual_reason", record.requiresManualReason())
                .addValue("result", record.result())
                .addValue("occurred_at", timestampWithTimeZone(record.occurredAt())));
        return record;
    }

    @Override
    public List<TraceabilityStepRecord> listStepsByInstance(String processInstanceId) {
        return jdbcTemplate.query(
                "SELECT * FROM business_process_step_records WHERE process_instance_id = :pid ORDER BY occurred_at, id",
                Map.of("pid", processInstanceId), this::mapStep);
    }

    @Override
    public TraceabilityAutoCaptureRate autoCaptureRate(String merchantScope, int thresholdPercent) {
        String where = merchantScope == null ? "" : "WHERE i.merchant_id = :merchant_id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("merchant_id", merchantScope);
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total_steps,
                       COALESCE(SUM(CASE WHEN s.auto_captured THEN 1 ELSE 0 END), 0) AS auto_steps
                  FROM business_process_step_records s
                  JOIN business_process_instances i ON i.id = s.process_instance_id
                """ + where,
                params,
                (rs, rowNum) -> {
                    long total = rs.getLong("total_steps");
                    long automatic = rs.getLong("auto_steps");
                    double percent = total == 0 ? 100.0 : (automatic * 100.0) / total;
                    double rounded = Math.round(percent * 10.0) / 10.0;
                    return new TraceabilityAutoCaptureRate(
                            total, automatic, rounded, thresholdPercent, rounded >= thresholdPercent);
                });
    }

    @Override
    public TraceabilityResponsibilityRecord saveResponsibility(TraceabilityResponsibilityRecord record) {
        jdbcTemplate.update("""
                INSERT INTO service_responsibility_records (
                    id, order_id, table_id, customer_id, exception_id, primary_service_staff_id,
                    assist_staff_ids, sales_staff_id, first_handler_staff_id, refunded_by, waived_by,
                    approved_by, assigned_by, shift_assignment_id, assigned_event_id, merchant_id,
                    service_start_at, service_end_at, status
                )
                VALUES (
                    :id, :order_id, :table_id, :customer_id, :exception_id, :primary_service_staff_id,
                    CAST(:assist_staff_ids AS jsonb), :sales_staff_id, :first_handler_staff_id, :refunded_by, :waived_by,
                    :approved_by, :assigned_by, :shift_assignment_id, :assigned_event_id, :merchant_id,
                    :service_start_at, :service_end_at, :status
                )
                ON CONFLICT (id) DO UPDATE SET
                    primary_service_staff_id = EXCLUDED.primary_service_staff_id,
                    assist_staff_ids = EXCLUDED.assist_staff_ids,
                    first_handler_staff_id = EXCLUDED.first_handler_staff_id,
                    refunded_by = EXCLUDED.refunded_by,
                    waived_by = EXCLUDED.waived_by,
                    approved_by = EXCLUDED.approved_by,
                    service_end_at = EXCLUDED.service_end_at,
                    status = EXCLUDED.status
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("order_id", record.orderId())
                .addValue("table_id", record.tableId())
                .addValue("customer_id", record.customerId())
                .addValue("exception_id", record.exceptionId())
                .addValue("primary_service_staff_id", record.primaryServiceStaffId())
                .addValue("assist_staff_ids", writeJson(record.assistStaffIds()))
                .addValue("sales_staff_id", record.salesStaffId())
                .addValue("first_handler_staff_id", record.firstHandlerStaffId())
                .addValue("refunded_by", record.refundedBy())
                .addValue("waived_by", record.waivedBy())
                .addValue("approved_by", record.approvedBy())
                .addValue("assigned_by", record.assignedBy())
                .addValue("shift_assignment_id", record.shiftAssignmentId())
                .addValue("assigned_event_id", record.assignedEventId())
                .addValue("merchant_id", record.merchantId())
                .addValue("service_start_at", timestampWithTimeZone(record.serviceStartAt()))
                .addValue("service_end_at", timestampWithTimeZone(record.serviceEndAt()))
                .addValue("status", record.status()));
        return record;
    }

    @Override
    public List<TraceabilityResponsibilityRecord> listResponsibilities(String merchantScope) {
        if (merchantScope == null) {
            return jdbcTemplate.query("SELECT * FROM service_responsibility_records ORDER BY service_start_at DESC, id",
                    this::mapResponsibility);
        }
        return jdbcTemplate.query(
                "SELECT * FROM service_responsibility_records WHERE merchant_id = :merchant_id ORDER BY service_start_at DESC, id",
                Map.of("merchant_id", merchantScope), this::mapResponsibility);
    }

    private TraceabilityProcessInstanceRecord mapInstance(ResultSet rs, int rowNumber) throws SQLException {
        return new TraceabilityProcessInstanceRecord(
                rs.getString("id"),
                rs.getString("process_type"),
                rs.getString("business_object_type"),
                rs.getString("business_object_id"),
                rs.getString("project_id"),
                rs.getString("merchant_id"),
                rs.getString("venue_id"),
                rs.getString("area_id"),
                rs.getString("location_point_id"),
                rs.getString("started_event_id"),
                rs.getString("current_step_code"),
                rs.getString("status"),
                nullableInstant(rs, "started_at"),
                nullableInstant(rs, "completed_at"));
    }

    private TraceabilityStepRecord mapStep(ResultSet rs, int rowNumber) throws SQLException {
        return new TraceabilityStepRecord(
                rs.getString("id"),
                rs.getString("process_instance_id"),
                rs.getString("step_code"),
                rs.getString("step_name"),
                rs.getString("actor_type"),
                rs.getString("actor_id"),
                rs.getString("terminal_type"),
                rs.getString("device_id"),
                rs.getString("location_point_id"),
                rs.getString("event_id"),
                rs.getBoolean("auto_captured"),
                rs.getString("capture_source"),
                rs.getBoolean("requires_manual_reason"),
                rs.getString("result"),
                nullableInstant(rs, "occurred_at"));
    }

    private TraceabilityResponsibilityRecord mapResponsibility(ResultSet rs, int rowNumber) throws SQLException {
        return new TraceabilityResponsibilityRecord(
                rs.getString("id"),
                rs.getString("order_id"),
                rs.getString("table_id"),
                rs.getString("customer_id"),
                rs.getString("exception_id"),
                rs.getString("primary_service_staff_id"),
                readJson(rs.getString("assist_staff_ids")),
                rs.getString("sales_staff_id"),
                rs.getString("first_handler_staff_id"),
                rs.getString("refunded_by"),
                rs.getString("waived_by"),
                rs.getString("approved_by"),
                rs.getString("assigned_by"),
                rs.getString("shift_assignment_id"),
                rs.getString("assigned_event_id"),
                rs.getString("merchant_id"),
                nullableInstant(rs, "service_start_at"),
                nullableInstant(rs, "service_end_at"),
                rs.getString("status"));
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception exception) {
            throw new IllegalStateException("cannot serialize assist_staff_ids", exception);
        }
    }

    private List<String> readJson(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(text, STRING_LIST);
        } catch (Exception exception) {
            throw new IllegalStateException("cannot deserialize assist_staff_ids", exception);
        }
    }
}
