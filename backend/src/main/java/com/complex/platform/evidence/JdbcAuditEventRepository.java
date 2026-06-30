package com.complex.platform.evidence;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAuditEventRepository implements AuditEventRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcAuditEventRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public AuditEvent save(AuditEvent event) {
        String sql = """
                INSERT INTO audit_events (
                    id, occurred_at, event_name, event_result, actor_type, actor_id, auth_context,
                    terminal, permission_code, business_type, business_id, request_id, idempotency_key,
                    metadata, merchant_id, event_category, subject_type
                )
                VALUES (
                    :id, :occurred_at, :event_name, :event_result, :actor_type, :actor_id, :auth_context,
                    :terminal, :permission_code, :business_type, :business_id, :request_id, :idempotency_key,
                    CAST(:metadata AS jsonb), :merchant_id, :event_category, :subject_type
                )
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", event.id())
                .addValue("occurred_at", timestampWithTimeZone(event.occurredAt()))
                .addValue("event_name", event.eventName())
                .addValue("event_result", event.eventResult())
                .addValue("actor_type", event.actorType())
                .addValue("actor_id", event.actorId())
                .addValue("auth_context", event.authContext())
                .addValue("terminal", event.terminal())
                .addValue("permission_code", event.permissionCode())
                .addValue("business_type", event.businessType())
                .addValue("business_id", event.businessId())
                .addValue("request_id", event.requestId())
                .addValue("idempotency_key", event.idempotencyKey())
                .addValue("metadata", writeMetadata(event))
                .addValue("merchant_id", event.merchantId())
                .addValue("event_category", event.eventCategory())
                .addValue("subject_type", event.subjectType()));
        return event;
    }


    private String writeMetadata(AuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event.metadata());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("audit metadata is not serializable", exception);
        }
    }

    @Override
    public java.util.List<AuditEvent> searchOperationEvents(String businessType, String businessId,
            String actorId, String eventName, java.time.Instant from, java.time.Instant to, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, occurred_at, event_name, event_result, actor_type, actor_id, auth_context,
                       terminal, permission_code, business_type, business_id, request_id, idempotency_key,
                       metadata::text AS metadata, merchant_id, event_category, subject_type
                  FROM audit_events
                 WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (businessType != null) { sql.append(" AND business_type = :business_type"); params.addValue("business_type", businessType); }
        if (businessId != null) { sql.append(" AND business_id = :business_id"); params.addValue("business_id", businessId); }
        if (actorId != null) { sql.append(" AND actor_id = :actor_id"); params.addValue("actor_id", actorId); }
        if (eventName != null) { sql.append(" AND event_name = :event_name"); params.addValue("event_name", eventName); }
        if (from != null) { sql.append(" AND occurred_at >= :from_ts"); params.addValue("from_ts", timestampWithTimeZone(from)); }
        if (to != null) { sql.append(" AND occurred_at <= :to_ts"); params.addValue("to_ts", timestampWithTimeZone(to)); }
        sql.append(" ORDER BY occurred_at DESC, id LIMIT :limit");
        params.addValue("limit", Math.max(1, Math.min(limit, 1000)));
        return jdbcTemplate.query(sql.toString(), params, this::mapAuditRow);
    }

    private AuditEvent mapAuditRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AuditEvent(
                (java.util.UUID) rs.getObject("id"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getString("event_name"), rs.getString("event_result"),
                rs.getString("actor_type"), rs.getString("actor_id"), rs.getString("auth_context"),
                rs.getString("terminal"), rs.getString("permission_code"),
                rs.getString("business_type"), rs.getString("business_id"),
                rs.getString("request_id"), rs.getString("idempotency_key"),
                readMetadataMap(rs.getString("metadata")),
                rs.getString("merchant_id"), rs.getString("event_category"), rs.getString("subject_type"));
    }

    private java.util.Map<String, Object> readMetadataMap(String json) {
        if (json == null || json.isBlank()) { return java.util.Map.of(); }
        try {
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
        } catch (JsonProcessingException exception) {
            return java.util.Map.of();
        }
    }
}
