package com.complex.platform.amusement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

final class JdbcAmusementRowMapper {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> ITEM_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    JdbcAmusementRowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    AmusementSafetyTemplateRecord mapSafetyTemplate(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementSafetyTemplateRecord(
                rs.getString("id"),
                rs.getString("template_name"),
                readJson(rs.getString("items_json"), ITEM_LIST_TYPE),
                rs.getObject("effective_from", LocalDate.class),
                rs.getString("status"),
                readJson(rs.getString("evidence_attachment_ids"), STRING_LIST_TYPE),
                instant(rs, "created_at"),
                instant(rs, "updated_at"),
                rs.getString("merchant_id")
        );
    }

    AmusementProjectRecord mapProject(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementProjectRecord(
                rs.getString("id"),
                rs.getString("project_name"),
                rs.getInt("capacity"),
                rs.getString("open_status"),
                rs.getString("safety_template_id"),
                readJson(rs.getString("eligibility_rule_json"), MAP_TYPE),
                rs.getString("location_point_id"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"),
                rs.getString("merchant_id")
        );
    }

    AmusementProjectEntitlementRecord mapProjectEntitlement(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementProjectEntitlementRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("entitlement_type"),
                instant(rs, "created_at")
        );
    }

    AmusementCapacityConfigRecord mapCapacityConfig(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementCapacityConfigRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("location_point_id"),
                rs.getString("device_id"),
                rs.getInt("max_concurrent"),
                rs.getString("effective_session_pattern"),
                rs.getBoolean("enabled"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    AmusementProjectRuleRecord mapProjectRule(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementProjectRuleRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("rule_type"),
                rs.getString("rule_value"),
                rs.getString("effective_from"),
                rs.getString("effective_to"),
                rs.getBoolean("blocking"),
                rs.getString("status"),
                readJson(rs.getString("evidence_attachment_ids"), STRING_LIST_TYPE),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    AmusementSessionRecord mapSession(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementSessionRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                instant(rs, "start_time"),
                instant(rs, "end_time"),
                rs.getInt("capacity"),
                rs.getInt("used_capacity"),
                rs.getInt("locked_capacity"),
                rs.getString("status"),
                readJson(rs.getString("evidence_attachment_ids"), STRING_LIST_TYPE),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    EntitlementSessionRecord mapEntitlementSession(ResultSet rs, int rowNum) throws SQLException {
        return new EntitlementSessionRecord(
                rs.getString("id"),
                rs.getString("session_code"),
                rs.getString("entitlement_ref"),
                rs.getString("merchant_id"),
                rs.getString("project_id"),
                rs.getString("status"),
                instant(rs, "expires_at"),
                rs.getString("initiated_by"),
                rs.getString("claimed_by"),
                instant(rs, "claimed_at"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    AmusementSafetyInspectionRecord mapSafetyInspection(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementSafetyInspectionRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("template_id"),
                readJson(rs.getString("items_json"), ITEM_LIST_TYPE),
                rs.getString("overall_result"),
                rs.getString("inspection_status"),
                rs.getString("rectification_status"),
                rs.getString("reopen_status"),
                instant(rs, "inspected_at"),
                instant(rs, "submitted_at"),
                instant(rs, "rechecked_at"),
                instant(rs, "approved_at"),
                rs.getString("operator_note"),
                readJson(rs.getString("evidence_attachment_ids"), STRING_LIST_TYPE),
                instant(rs, "created_at"),
                instant(rs, "updated_at"),
                rs.getString("inspector_id"),
                rs.getString("inspection_result"),
                rs.getBoolean("abnormal_found"),
                rs.getString("abnormal_note")
        );
    }

    AmusementVerificationRecord mapVerification(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementVerificationRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("session_id"),
                rs.getString("credential_payload"),
                rs.getString("device_id"),
                rs.getString("staff_id"),
                rs.getString("verification_result"),
                rs.getString("failure_reason"),
                readJson(rs.getString("evidence_attachment_ids"), STRING_LIST_TYPE),
                instant(rs, "created_at")
        );
    }

    AmusementManualOverrideRecord mapManualOverride(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementManualOverrideRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("session_id"),
                rs.getString("override_type"),
                rs.getString("reason"),
                rs.getString("status"),
                rs.getString("operator_note"),
                readJson(rs.getString("evidence_attachment_ids"), STRING_LIST_TYPE),
                instant(rs, "created_at")
        );
    }

    AmusementDeviceStatusRecord mapDeviceStatus(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementDeviceStatusRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("device_id"),
                rs.getString("provider_code"),
                rs.getString("status"),
                instant(rs, "last_seen_at"),
                readJson(rs.getString("raw_status_json"), MAP_TYPE),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    AmusementQueueEntryRecord mapQueueEntry(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementQueueEntryRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("session_id"),
                rs.getString("customer_id"),
                rs.getString("queue_no"),
                rs.getString("status"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    AmusementReservationRecord mapReservation(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementReservationRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("session_id"),
                rs.getString("customer_id"),
                rs.getString("reservation_status"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    AmusementGroupBookingRecord mapGroupBooking(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementGroupBookingRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("session_id"),
                rs.getString("booking_type"),
                rs.getString("customer_name"),
                rs.getString("contact_phone"),
                rs.getInt("party_size"),
                rs.getInt("reserved_capacity"),
                rs.getString("booking_status"),
                rs.getString("approval_note"),
                rs.getString("operator_note"),
                readJson(rs.getString("evidence_attachment_ids"), STRING_LIST_TYPE),
                instant(rs, "approved_at"),
                instant(rs, "released_at"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    AmusementMaintenanceRecord mapMaintenanceRecord(ResultSet rs, int rowNum) throws SQLException {
        return new AmusementMaintenanceRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("device_id"),
                rs.getString("location_point_id"),
                rs.getString("maintenance_vendor_id"),
                rs.getString("engineer_id"),
                rs.getString("engineer_name"),
                instant(rs, "maintenance_at"),
                rs.getString("result"),
                rs.getString("exception_note"),
                rs.getString("source_terminal"),
                rs.getString("status"),
                rs.getString("supervisor_confirmed_by"),
                instant(rs, "confirmed_at"),
                instant(rs, "resume_business_at"),
                instant(rs, "created_at"),
                rs.getString("merchant_id")
        );
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private <T> T readJson(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("stored json payload is unreadable", exception);
        }
    }
}
