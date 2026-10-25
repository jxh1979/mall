package com.complex.platform.notification;

// 文件版本：V0.01

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.crypto.PiiCipher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// ponytail: 仓储直用实现类,无接口(one impl)。
@Repository
class JdbcNotificationRepository {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    // PGM L876「客人和员工的手机号……一律加密存放」：收件人手机号写时 encrypt、读时 decrypt（V547）；发送器拿内存里的明文，下发不变。
    private final PiiCipher piiCipher;

    JdbcNotificationRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper, PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.piiCipher = piiCipher;
    }

    public NotificationSendLogRecord insert(NotificationSendLogRecord record) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO notification_send_log (
                    id, request_id, channel, domain, event_key, recipient, template_code,
                    params, status, reason, provider_ref, created_at, updated_at
                )
                VALUES (
                    :id, :request_id, :channel, :domain, :event_key, :recipient, :template_code,
                    CAST(:params AS jsonb), :status, :reason, :provider_ref, :created_at, :updated_at
                )
                ON CONFLICT (request_id) DO NOTHING
                """, parameters(record));
        if (inserted == 1) {
            return record;
        }
        return findByRequestId(record.requestId())
                .orElseThrow(() -> new IllegalStateException("notification send log conflict row was not found"));
    }

    public Optional<NotificationSendLogRecord> findByRequestId(String requestId) {
        return jdbcTemplate.query("""
                SELECT id, request_id, channel, domain, event_key, recipient, template_code,
                       params::text AS params, status, reason, provider_ref, created_at, updated_at
                  FROM notification_send_log
                 WHERE request_id = :request_id
                """, Map.of("request_id", requestId), this::mapRecord).stream().findFirst();
    }

    public NotificationSendLogRecord updateStatus(
            String requestId,
            String status,
            String providerRef,
            String reason,
            Instant updatedAt
    ) {
        return jdbcTemplate.query("""
                UPDATE notification_send_log
                   SET status = :status,
                       provider_ref = :provider_ref,
                       reason = :reason,
                       updated_at = :updated_at
                 WHERE request_id = :request_id
             RETURNING id, request_id, channel, domain, event_key, recipient, template_code,
                       params::text AS params, status, reason, provider_ref, created_at, updated_at
                """, new MapSqlParameterSource()
                .addValue("request_id", requestId)
                .addValue("status", status)
                .addValue("provider_ref", providerRef)
                .addValue("reason", reason)
                .addValue("updated_at", timestampWithTimeZone(updatedAt)), this::mapRecord).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("notification send log was not found for status update"));
    }

    private MapSqlParameterSource parameters(NotificationSendLogRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("request_id", record.requestId())
                .addValue("channel", record.channel())
                .addValue("domain", record.domain())
                .addValue("event_key", record.eventKey())
                .addValue("recipient", piiCipher.encrypt(record.recipient()))
                .addValue("template_code", record.templateCode())
                .addValue("params", writeJson(record.params()))
                .addValue("status", record.status())
                .addValue("reason", record.reason())
                .addValue("provider_ref", record.providerRef())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private NotificationSendLogRecord mapRecord(ResultSet resultSet, int rowNumber) throws SQLException {
        return new NotificationSendLogRecord(
                resultSet.getString("id"),
                resultSet.getString("request_id"),
                resultSet.getString("channel"),
                resultSet.getString("domain"),
                resultSet.getString("event_key"),
                piiCipher.decrypt(resultSet.getString("recipient")),
                resultSet.getString("template_code"),
                readObjectMap(resultSet.getString("params")),
                resultSet.getString("status"),
                resultSet.getString("reason"),
                resultSet.getString("provider_ref"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("notification params are not serializable", exception);
        }
    }

    private Map<String, Object> readObjectMap(String value) {
        try {
            return value == null ? Map.of() : objectMapper.readValue(value, OBJECT_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("notification params are not readable", exception);
        }
    }
}
