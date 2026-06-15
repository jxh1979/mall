package com.complex.platform.common.idempotency;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcIdempotencyRepository implements IdempotencyRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcIdempotencyRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
        String sql = """
                SELECT id, operation_id, idempotency_key, request_hash, response_code, response_body,
                       http_status, created_at, expires_at
                  FROM idempotency_records
                 WHERE operation_id = :operation_id
                   AND idempotency_key = :idempotency_key
                """;
        return jdbcTemplate.query(sql, Map.of(
                "operation_id", operationId,
                "idempotency_key", idempotencyKey
        ), this::mapRecord).stream().findFirst();
    }

    @Override
    public boolean tryReserve(IdempotencyRecord record) {
        String sql = """
                INSERT INTO idempotency_records (
                    id, operation_id, idempotency_key, request_hash, response_code, response_body,
                    http_status, created_at, expires_at
                )
                VALUES (
                    :id, :operation_id, :idempotency_key, :request_hash, :response_code,
                    CAST(:response_body AS jsonb), :http_status, :created_at, :expires_at
                )
                ON CONFLICT (operation_id, idempotency_key) DO NOTHING
                """;
        return jdbcTemplate.update(sql, parameters(record)) == 1;
    }

    @Override
    public IdempotencyRecord complete(IdempotencyRecord record) {
        String sql = """
                UPDATE idempotency_records
                   SET response_code = :response_code,
                       response_body = CAST(:response_body AS jsonb),
                       http_status = :http_status
                 WHERE operation_id = :operation_id
                   AND idempotency_key = :idempotency_key
                   AND request_hash = :request_hash
                """;
        int updated = jdbcTemplate.update(sql, parameters(record));
        if (updated != 1) {
            throw new IllegalStateException("idempotency reservation was not found for completion");
        }
        return record;
    }

    private MapSqlParameterSource parameters(IdempotencyRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("operation_id", record.operationId())
                .addValue("idempotency_key", record.idempotencyKey())
                .addValue("request_hash", record.requestHash())
                .addValue("response_code", record.responseCode())
                .addValue("response_body", record.responseBody())
                .addValue("http_status", record.httpStatus())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("expires_at", timestampWithTimeZone(record.expiresAt()));
    }

    private OffsetDateTime timestampWithTimeZone(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private IdempotencyRecord mapRecord(ResultSet resultSet, int rowNumber) throws SQLException {
        return new IdempotencyRecord(
                resultSet.getObject("id", java.util.UUID.class),
                resultSet.getString("operation_id"),
                resultSet.getString("idempotency_key"),
                resultSet.getString("request_hash"),
                resultSet.getString("response_code"),
                resultSet.getString("response_body"),
                resultSet.getInt("http_status"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("expires_at").toInstant()
        );
    }
}
