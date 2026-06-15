package com.complex.platform.evidence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
                    terminal, permission_code, business_type, business_id, request_id, idempotency_key, metadata
                )
                VALUES (
                    :id, :occurred_at, :event_name, :event_result, :actor_type, :actor_id, :auth_context,
                    :terminal, :permission_code, :business_type, :business_id, :request_id, :idempotency_key,
                    CAST(:metadata AS jsonb)
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
                .addValue("metadata", writeMetadata(event)));
        return event;
    }

    private OffsetDateTime timestampWithTimeZone(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private String writeMetadata(AuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event.metadata());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("audit metadata is not serializable", exception);
        }
    }
}
