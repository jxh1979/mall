package com.complex.platform.compliance;

import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.time.Instant;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * AUD 复核 P1-2：企微 webhook nonce 去重（V291 wecom_webhook_nonces）。
 * INSERT ... ON CONFLICT (nonce) DO NOTHING 原子登记，命中 0 行=重放。首次登记后顺带清理超 TTL 的旧 nonce
 * （超窗 nonce 亦会被 verify 的 timestamp 窗口二次拦截，此清理仅为控表大小）。
 */
@Repository
class JdbcWecomWebhookNonceStore implements WecomWebhookNonceStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcWecomWebhookNonceStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean registerFresh(String nonce, Instant now, long ttlSeconds) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO wecom_webhook_nonces (nonce, seen_at)
                VALUES (:nonce, :seen_at)
                ON CONFLICT (nonce) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("nonce", nonce)
                .addValue("seen_at", timestampWithTimeZone(now)));
        if (inserted == 1) {
            jdbcTemplate.update("""
                    DELETE FROM wecom_webhook_nonces WHERE seen_at < :cutoff
                    """, new MapSqlParameterSource()
                    .addValue("cutoff", timestampWithTimeZone(now.minusSeconds(Math.max(ttlSeconds, 0)))));
        }
        return inserted == 1;
    }
}
