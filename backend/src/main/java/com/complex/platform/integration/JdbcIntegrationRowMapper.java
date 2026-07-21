package com.complex.platform.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

final class JdbcIntegrationRowMapper {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    JdbcIntegrationRowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    OpenAppCredentialRecord mapOpenAppCredential(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OpenAppCredentialRecord(
                resultSet.getString("id"),
                resultSet.getString("app_key"),
                resultSet.getString("secret_ref"),
                resultSet.getString("status"),
                readStringList(resultSet.getString("allowed_endpoint_keys")),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    OpenEndpointConfigRecord mapEndpointConfig(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OpenEndpointConfigRecord(
                resultSet.getString("id"),
                resultSet.getString("endpoint_key"),
                resultSet.getString("provider_code"),
                resultSet.getString("secret_ref"),
                resultSet.getString("status"),
                resultSet.getBoolean("webhook_enabled"),
                resultSet.getBoolean("polling_check_enabled"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    OpenApiEventRecord mapOpenApiEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OpenApiEventRecord(
                resultSet.getString("id"),
                resultSet.getString("endpoint_key"),
                resultSet.getString("provider_code"),
                resultSet.getString("event_name"),
                resultSet.getString("idempotency_key"),
                resultSet.getBoolean("signature_valid"),
                resultSet.getString("event_status"),
                readMap(resultSet.getString("payload_json")),
                readMap(resultSet.getString("result_json")),
                resultSet.getString("failure_reason"),
                instant(resultSet, "received_at"),
                instant(resultSet, "processed_at")
        );
    }

    ProviderAdapterRecord mapProviderAdapter(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProviderAdapterRecord(
                resultSet.getString("id"),
                resultSet.getString("provider_code"),
                resultSet.getString("adapter_type"),
                resultSet.getString("mode"),
                readStringList(resultSet.getString("capabilities")),
                resultSet.getString("environment"),
                resultSet.getString("status"),
                resultSet.getBoolean("webhook_enabled"),
                resultSet.getBoolean("polling_check_enabled"),
                readMap(resultSet.getString("field_mapping_json")),
                readMap(resultSet.getString("webhook_config_json")),
                readMap(resultSet.getString("polling_config_json")),
                resultSet.getString("credential_secret_ref"),
                resultSet.getBoolean("production_ready"),
                resultSet.getString("operator_note"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    IntegrationLedgerRecord mapLedger(ResultSet resultSet, int rowNumber) throws SQLException {
        return new IntegrationLedgerRecord(
                resultSet.getString("id"),
                resultSet.getString("ledger_type"),
                resultSet.getString("provider_code"),
                resultSet.getString("business_id"),
                resultSet.getString("status"),
                readMap(resultSet.getString("payload_json")),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    IntegrationOrderSyncRecord mapOrderSync(ResultSet resultSet, int rowNumber) throws SQLException {
        return new IntegrationOrderSyncRecord(
                resultSet.getString("id"),
                resultSet.getString("provider_code"),
                resultSet.getString("business_type"),
                resultSet.getString("sync_type"),
                resultSet.getString("external_business_id"),
                resultSet.getString("status"),
                resultSet.getLong("amount_cent"),
                resultSet.getString("currency"),
                resultSet.getString("channel"),
                resultSet.getString("external_channel_order_id"),
                resultSet.getString("merchant_id"),
                readMap(resultSet.getString("provider_payload_json")),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    IntegrationChannelStoreMapRecord mapChannelStoreMap(ResultSet resultSet, int rowNumber) throws SQLException {
        return new IntegrationChannelStoreMapRecord(
                resultSet.getString("id"),
                resultSet.getString("provider_code"),
                resultSet.getString("channel"),
                resultSet.getString("external_store_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    IntegrationTaskRecord mapTask(ResultSet resultSet, int rowNumber, String taskCategory) throws SQLException {
        return new IntegrationTaskRecord(
                resultSet.getString("id"),
                taskCategory,
                resultSet.getString("provider_code"),
                resultSet.getString("task_type"),
                resultSet.getString("business_type"),
                resultSet.getString("external_business_id"),
                resultSet.getString("status"),
                resultSet.getInt("retry_count"),
                resultSet.getInt("max_retry_count"),
                instant(resultSet, "next_retry_at"),
                resultSet.getString("operator_user_id"),
                resultSet.getString("resolution_note"),
                readMap(resultSet.getString("payload_json")),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    ReconciliationBatchRecord mapReconciliationBatch(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ReconciliationBatchRecord(
                resultSet.getString("id"),
                resultSet.getString("provider_code"),
                resultSet.getString("business_type"),
                localDate(resultSet, "period_start"),
                localDate(resultSet, "period_end"),
                resultSet.getString("status"),
                resultSet.getInt("expected_count"),
                resultSet.getInt("actual_count"),
                resultSet.getInt("difference_count"),
                resultSet.getLong("total_difference_cent"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    ReconciliationDifferenceRecord mapReconciliationDifference(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ReconciliationDifferenceRecord(
                resultSet.getString("id"),
                resultSet.getString("batch_id"),
                resultSet.getString("provider_code"),
                resultSet.getString("business_type"),
                resultSet.getString("external_business_id"),
                resultSet.getString("difference_type"),
                resultSet.getLong("internal_amount_cent"),
                resultSet.getLong("provider_amount_cent"),
                resultSet.getLong("difference_amount_cent"),
                resultSet.getString("status"),
                resultSet.getString("source_domain"),
                resultSet.getString("assigned_to_user_id"),
                resultSet.getString("resolution_note"),
                readMap(resultSet.getString("provider_payload_json")),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    RobotTaskEventRecord mapRobotTaskEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RobotTaskEventRecord(
                resultSet.getString("id"),
                resultSet.getString("robot_task_ref"),
                resultSet.getString("event_type"),
                resultSet.getString("handover_id"),
                readMap(resultSet.getString("payload")),
                instant(resultSet, "reported_at")
        );
    }

    VideoAnchorRecord mapVideoAnchor(ResultSet resultSet, int rowNumber) throws SQLException {
        return new VideoAnchorRecord(
                resultSet.getString("id"),
                resultSet.getString("point_id"),
                instant(resultSet, "event_time"),
                resultSet.getString("business_ref"),
                resultSet.getString("clip_index_ref"),
                instant(resultSet, "created_at")
        );
    }

    private Map<String, Object> readMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("json object field is unreadable", exception);
        }
    }

    private List<String> readStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("json array field is unreadable", exception);
        }
    }

    private Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        var timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private LocalDate localDate(ResultSet resultSet, String columnName) throws SQLException {
        var date = resultSet.getDate(columnName);
        return date == null ? null : date.toLocalDate();
    }
}
