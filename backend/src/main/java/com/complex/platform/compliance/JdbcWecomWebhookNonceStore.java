package com.complex.platform.compliance;

import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.time.Instant;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * AUD 复核 P1-2 + AUD-021：企微 webhook nonce 去重（V291 建表 + V295 加报文指纹）。
 * INSERT ... ON CONFLICT (nonce) DO NOTHING 原子登记；冲突时读回已登记指纹比对：
 * 同指纹=同报文合法重投递（SAME_PAYLOAD，放行给业务幂等层），异指纹=重放（REPLAY）。
 * 首次登记后顺带清理超 TTL 旧 nonce（超窗 nonce 亦被 verify 的 timestamp 窗口二次拦截，此清理仅为控表大小）。
 */
@Repository
class JdbcWecomWebhookNonceStore implements WecomWebhookNonceStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcWecomWebhookNonceStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Registration registerFresh(String nonce, String requestFingerprint, Instant now, long ttlSeconds) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO wecom_webhook_nonces (nonce, seen_at, request_fingerprint)
                VALUES (:nonce, :seen_at, :fingerprint)
                ON CONFLICT (nonce) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("nonce", nonce)
                .addValue("seen_at", timestampWithTimeZone(now))
                .addValue("fingerprint", requestFingerprint == null ? "" : requestFingerprint));
        if (inserted == 1) {
            jdbcTemplate.update("""
                    DELETE FROM wecom_webhook_nonces WHERE seen_at < :cutoff
                    """, new MapSqlParameterSource()
                    .addValue("cutoff", timestampWithTimeZone(now.minusSeconds(Math.max(ttlSeconds, 0)))));
            return Registration.FRESH;
        }
        String existing = jdbcTemplate.query("""
                SELECT request_fingerprint FROM wecom_webhook_nonces WHERE nonce = :nonce
                """, new MapSqlParameterSource().addValue("nonce", nonce),
                rs -> rs.next() ? rs.getString(1) : null);
        boolean samePayload = existing != null && !existing.isEmpty()
                && existing.equals(requestFingerprint == null ? "" : requestFingerprint);
        return samePayload ? Registration.SAME_PAYLOAD : Registration.REPLAY;
    }
}
