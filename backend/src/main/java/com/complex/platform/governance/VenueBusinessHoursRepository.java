package com.complex.platform.governance;
// 文件版本：V0.01

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 场地营业时间表仓库（V226 建的 venue_business_hours，此前只有核销侧读、没有任何配置入口——只能改库）。
 *
 * <p>裸 SQL 直查，不引 ticketing 域类型（核销侧的读在 JdbcTicketingRepository，两边只共用这张表，不共用代码）。
 */
@Repository
class VenueBusinessHoursRepository {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    VenueBusinessHoursRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 全部场地的营业时间行（配置页一次拉全量，量级=场地数×7，很小）。 */
    List<VenueBusinessHoursRecord> listAll() {
        return jdbcTemplate.query("""
                SELECT id, venue_id, day_of_week, open_time, close_time, is_draft, effective_from,
                       created_at, updated_at
                  FROM venue_business_hours
                 ORDER BY venue_id, day_of_week
                """, new MapSqlParameterSource(), (rs, n) -> new VenueBusinessHoursRecord(
                rs.getString("id"),
                rs.getString("venue_id"),
                rs.getInt("day_of_week"),
                rs.getObject("open_time", LocalTime.class).format(HH_MM),
                rs.getObject("close_time", LocalTime.class).format(HH_MM),
                rs.getBoolean("is_draft"),
                rs.getObject("effective_from", LocalDate.class),
                rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", java.time.OffsetDateTime.class).toInstant()));
    }

    /** M015-009：场地＝spaces.id；返回其综合体项目归属（无项目归属返回空串），场地不存在返回 empty。 */
    Optional<String> findVenueProjectId(String venueId) {
        return jdbcTemplate.query("SELECT COALESCE(project_id, '') AS project_id FROM spaces WHERE id = :id",
                new MapSqlParameterSource("id", venueId), (rs, n) -> rs.getString("project_id")).stream().findFirst();
    }

    /** M015-009：按综合体项目过滤的营业时间行（BEQR-152 项目隔离；JOIN spaces 取 project_id）。 */
    List<VenueBusinessHoursRecord> listByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT h.id, h.venue_id, h.day_of_week, h.open_time, h.close_time, h.is_draft, h.effective_from,
                       h.created_at, h.updated_at
                  FROM venue_business_hours h
                  JOIN spaces s ON s.id = h.venue_id
                 WHERE s.project_id = :project_id
                 ORDER BY h.venue_id, h.day_of_week
                """, new MapSqlParameterSource("project_id", projectId), (rs, n) -> new VenueBusinessHoursRecord(
                rs.getString("id"),
                rs.getString("venue_id"),
                rs.getInt("day_of_week"),
                rs.getObject("open_time", LocalTime.class).format(HH_MM),
                rs.getObject("close_time", LocalTime.class).format(HH_MM),
                rs.getBoolean("is_draft"),
                rs.getObject("effective_from", LocalDate.class),
                rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", java.time.OffsetDateTime.class).toInstant()));
    }

    /** 按 (venue_id, day_of_week) 幂等覆盖（V226 唯一键）。返回落库后的行。 */
    VenueBusinessHoursRecord upsert(String id, String venueId, int dayOfWeek, LocalTime openTime,
                                    LocalTime closeTime, boolean disabled, LocalDate effectiveFrom, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO venue_business_hours (
                    id, venue_id, day_of_week, open_time, close_time, is_draft, effective_from, created_at, updated_at
                )
                VALUES (:id, :venue_id, :dow, :open_time, :close_time, :is_draft, :effective_from, :now, :now)
                ON CONFLICT (venue_id, day_of_week) DO UPDATE SET
                    open_time = EXCLUDED.open_time,
                    close_time = EXCLUDED.close_time,
                    is_draft = EXCLUDED.is_draft,
                    effective_from = EXCLUDED.effective_from,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("venue_id", venueId)
                .addValue("dow", dayOfWeek)
                .addValue("open_time", openTime)
                .addValue("close_time", closeTime)
                .addValue("is_draft", disabled)
                .addValue("effective_from", effectiveFrom)
                .addValue("now", now.atOffset(ZoneOffset.UTC)));
        return jdbcTemplate.query("""
                SELECT id, venue_id, day_of_week, open_time, close_time, is_draft, effective_from,
                       created_at, updated_at
                  FROM venue_business_hours
                 WHERE venue_id = :venue_id AND day_of_week = :dow
                """, new MapSqlParameterSource()
                .addValue("venue_id", venueId)
                .addValue("dow", dayOfWeek), (rs, n) -> new VenueBusinessHoursRecord(
                rs.getString("id"),
                rs.getString("venue_id"),
                rs.getInt("day_of_week"),
                rs.getObject("open_time", LocalTime.class).format(HH_MM),
                rs.getObject("close_time", LocalTime.class).format(HH_MM),
                rs.getBoolean("is_draft"),
                rs.getObject("effective_from", LocalDate.class),
                rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", java.time.OffsetDateTime.class).toInstant()))
                .stream().findFirst().orElseThrow();
    }
}
