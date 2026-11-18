package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 第十八批 AW 段 299-R1（V665）：票种售卖设置三列的唯一写入点（整体替换）。 */
@Repository
class JdbcTicketTypeSaleRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcTicketTypeSaleRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void updateSaleSettings(String id, Instant saleStartsAt, Instant saleEndsAt, Instant salePausedAt, Instant now) {
        jdbcTemplate.update("""
                UPDATE ticket_types
                   SET sale_starts_at = :sale_starts_at, sale_ends_at = :sale_ends_at, sale_paused_at = :sale_paused_at,
                       updated_at = :now
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("sale_starts_at", utc(saleStartsAt))
                .addValue("sale_ends_at", utc(saleEndsAt))
                .addValue("sale_paused_at", utc(salePausedAt))
                .addValue("now", utc(now)));
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
