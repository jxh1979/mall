package com.complex.platform.common.idempotency;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.crypto.PiiCipher;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * XM-013（PGM-1.15.2「一律加密存放」）：response_body 是服务层返回对象的序列化（含票务订单 / 团体预订 / 客人档案的手机号），
 * 自 V399 起列为 text、只存 PiiCipher 的 enc:v1: 信封（库上 CHECK 守新写入），读回解密后再交给 IdempotencyService 反序列化；
 * 存量明文行由 IdempotencyResponseBodyPiiBackfill 启动期就地加密。与 V339 多主体联系人手机号同一范式。
 */
@Repository
class JdbcIdempotencyRepository implements IdempotencyRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PiiCipher piiCipher;

    JdbcIdempotencyRepository(NamedParameterJdbcTemplate jdbcTemplate, PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.piiCipher = piiCipher;
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
                    :response_body, :http_status, :created_at, :expires_at
                )
                ON CONFLICT (operation_id, idempotency_key) DO NOTHING
                """;
        // M724（治理地图第 303 行「两个操作同时发生冲突时，一律以“先处理成功”的那个为准」）：同一键上有别的事务未提交的占位时，
        // 这条 INSERT 等对方事务结束（对方提交 → 0 行，对方回滚 → 插入成功）。等待设上限，超时数据库报 SQLSTATE 55P03，
        // 由 IdempotencyService 转成「请求正在处理中」；上限只管这一条语句，之后恢复默认。不在事务里时 SET LOCAL 不起作用。
        // ponytail: 5 秒是写死的常量，取值小于连接池取连接超时；出现正常动作超过 5 秒且重复提交频繁的入口时再改成配置项。
        jdbcTemplate.getJdbcTemplate().execute("SET LOCAL lock_timeout = '5s'");
        boolean reserved = jdbcTemplate.update(sql, parameters(record)) == 1;
        jdbcTemplate.getJdbcTemplate().execute("SET LOCAL lock_timeout TO DEFAULT");
        return reserved;
    }

    // M724：失败的提交不占编号——接管失败可重试的行不再要求请求散列相同，行上的散列（提交范围＋请求体）换成接管者的。
    @Override
    public boolean tryUpdateWhenResponseCode(IdempotencyRecord record, String expectedResponseCode) {
        String sql = """
                UPDATE idempotency_records
                   SET request_hash = :request_hash,
                       response_code = :response_code,
                       response_body = :response_body,
                       http_status = :http_status
                 WHERE operation_id = :operation_id
                   AND idempotency_key = :idempotency_key
                   AND response_code = :expected_response_code
                """;
        MapSqlParameterSource parameters = parameters(record)
                .addValue("expected_response_code", expectedResponseCode);
        return jdbcTemplate.update(sql, parameters) == 1;
    }

    @Override
    public IdempotencyRecord complete(IdempotencyRecord record) {
        String sql = """
                UPDATE idempotency_records
                   SET response_code = :response_code,
                       response_body = :response_body,
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

    // XM-M002-J（V437）：消耗了单次授权的成功记录记下消耗人与请求摘要，重放核验按「幂等键 + 消耗人 + 请求摘要」查找。
    @Override
    public void bindSingleUseRequest(java.util.UUID recordId, String actorId, String requestDigest) {
        int updated = jdbcTemplate.update("""
                UPDATE idempotency_records
                   SET single_use_actor_id = :actor_id,
                       single_use_request_digest = :request_digest
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", recordId)
                .addValue("actor_id", actorId)
                .addValue("request_digest", requestDigest));
        if (updated != 1) {
            throw new IllegalStateException("idempotency record was not found for single-use binding");
        }
    }

    @Override
    public boolean hasCompletedSingleUseRequest(String actorId, String idempotencyKey, String requestDigest) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                           SELECT 1
                             FROM idempotency_records
                            WHERE idempotency_key = :idempotency_key
                              AND single_use_actor_id = :actor_id
                              AND single_use_request_digest = :request_digest
                              AND response_code = 'OK'
                       )
                """, new MapSqlParameterSource()
                .addValue("idempotency_key", idempotencyKey)
                .addValue("actor_id", actorId)
                .addValue("request_digest", requestDigest), Boolean.class));
    }

    private MapSqlParameterSource parameters(IdempotencyRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("operation_id", record.operationId())
                .addValue("idempotency_key", record.idempotencyKey())
                .addValue("request_hash", record.requestHash())
                .addValue("response_code", record.responseCode())
                .addValue("response_body", piiCipher.encrypt(record.responseBody()))
                .addValue("http_status", record.httpStatus())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("expires_at", timestampWithTimeZone(record.expiresAt()));
    }


    private IdempotencyRecord mapRecord(ResultSet resultSet, int rowNumber) throws SQLException {
        return new IdempotencyRecord(
                resultSet.getObject("id", java.util.UUID.class),
                resultSet.getString("operation_id"),
                resultSet.getString("idempotency_key"),
                resultSet.getString("request_hash"),
                resultSet.getString("response_code"),
                piiCipher.decrypt(resultSet.getString("response_body")),
                resultSet.getInt("http_status"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("expires_at").toInstant()
        );
    }
}
