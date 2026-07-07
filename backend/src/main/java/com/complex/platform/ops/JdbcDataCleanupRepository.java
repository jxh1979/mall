package com.complex.platform.ops;

import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.api.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 集中数据清理台账 + 清理执行仓（Tier-D）。台账 CRUD 走 data_cleanup_jobs；
 * 预览/执行只针对目标实体表【已存档且超期】的行，删前逐行再校验无活引用（元数据驱动逐表 COUNT，
 * 照 V259/V260 口径），有活引用的行跳过不删（诚实：不强删被引用的数据）。
 */
@Repository
public class JdbcDataCleanupRepository {

    /** 引用计数枚举时跳过的纯审计历史表（照 V259：其保留悬空外键属预期审计留痕，不阻断删除）。 */
    private static final java.util.Set<String> AUDIT_ONLY_TABLES = java.util.Set.of(
            "audit_events", "evidence_events", "evidence_attachments", "evidence_reviews");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcDataCleanupRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    // ---------- 目标实体：预览统计 + 样本 + 执行删除 ----------

    /** 已存档（archived_at 非空）且早于 cutoff 的行数（仅统计，不删）。 */
    long countArchivedBefore(DataCleanupScope scope, Instant cutoff) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + scope.tableName()
                        + " WHERE archived_at IS NOT NULL AND archived_at < :cutoff",
                new MapSqlParameterSource().addValue("cutoff", timestampWithTimeZone(cutoff)),
                Long.class);
        return count == null ? 0L : count;
    }

    /** 已存档且早于 cutoff 的样本（最多 limit 条，展示 id/存档时间给运营核对）。 */
    List<Map<String, Object>> sampleArchivedBefore(DataCleanupScope scope, Instant cutoff, int limit) {
        return jdbcTemplate.query(
                "SELECT id, archived_at FROM " + scope.tableName()
                        + " WHERE archived_at IS NOT NULL AND archived_at < :cutoff"
                        + " ORDER BY archived_at ASC LIMIT :limit",
                new MapSqlParameterSource()
                        .addValue("cutoff", timestampWithTimeZone(cutoff))
                        .addValue("limit", limit),
                (rs, i) -> {
                    java.util.LinkedHashMap<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("id", rs.getString("id"));
                    OffsetDateTime archivedAt = rs.getObject("archived_at", OffsetDateTime.class);
                    row.put("archivedAt", archivedAt == null ? null : archivedAt.toInstant().toString());
                    return row;
                });
    }

    /**
     * 真删：只删【已存档 + delete_status<>'approved' + 早于 cutoff + 无活引用】的行，返回实删条数。
     * 逐行取 id 再校验引用后 DELETE（有引用的跳过），保证与逐条审批删除同样的活引用闸不被绕过。
     */
    int deleteArchivedBefore(DataCleanupScope scope, Instant cutoff) {
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT id FROM " + scope.tableName()
                        + " WHERE archived_at IS NOT NULL AND archived_at < :cutoff"
                        + " AND delete_status <> 'approved'",
                new MapSqlParameterSource().addValue("cutoff", timestampWithTimeZone(cutoff)),
                String.class);
        int deleted = 0;
        for (String id : ids) {
            if (countReferences(scope, id) > 0) {
                continue; // 仍有活引用，跳过（诚实：不强删被引用行，避免连锁孤儿）。
            }
            deleted += jdbcTemplate.update(
                    "DELETE FROM " + scope.tableName() + " WHERE id = :id",
                    new MapSqlParameterSource().addValue("id", id));
        }
        return deleted;
    }

    /** 元数据驱动活引用计数：枚举 public schema 中带 referenceColumn 列的基表逐表 COUNT（跳审计表）。
     *  无 referenceColumn（如套餐）恒 0。table 名来自系统元数据非用户输入，无注入。 */
    private long countReferences(DataCleanupScope scope, String id) {
        Optional<String> column = scope.referenceColumn();
        if (column.isEmpty()) {
            return 0L;
        }
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT c.table_name
                FROM information_schema.columns c
                JOIN information_schema.tables t
                  ON t.table_schema = c.table_schema AND t.table_name = c.table_name
                WHERE c.table_schema = 'public'
                  AND c.column_name = :column_name
                  AND t.table_type = 'BASE TABLE'
                """, new MapSqlParameterSource().addValue("column_name", column.get()), String.class);
        long total = 0L;
        for (String table : tables) {
            if (AUDIT_ONLY_TABLES.contains(table) || table.equals(scope.tableName())) {
                continue; // 跳审计表 + 自身表（自身 id 列非"引用列"，仅当引用列恰为自身表某列时才排除）。
            }
            Long c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM " + table + " WHERE " + column.get() + " = :id",
                    new MapSqlParameterSource().addValue("id", id), Long.class);
            total += c == null ? 0L : c;
        }
        return total;
    }

    // ---------- 台账 CRUD ----------

    DataCleanupJobRecord insert(DataCleanupJobRecord job) {
        jdbcTemplate.update("""
                INSERT INTO data_cleanup_jobs (
                    id, scope_type, filter_params, status, preview_count, preview_sample,
                    reason, requested_by, requested_at, created_at, updated_at
                ) VALUES (
                    :id, :scope_type, CAST(:filter_params AS jsonb), :status, :preview_count, CAST(:preview_sample AS jsonb),
                    :reason, :requested_by, :requested_at, :created_at, :updated_at
                )
                """, new MapSqlParameterSource()
                .addValue("id", job.id())
                .addValue("scope_type", job.scopeType())
                .addValue("filter_params", writeJson(job.filterParams()))
                .addValue("status", job.status())
                .addValue("preview_count", job.previewCount())
                .addValue("preview_sample", writeJson(job.previewSample()))
                .addValue("reason", job.reason())
                .addValue("requested_by", job.requestedBy())
                .addValue("requested_at", timestampWithTimeZone(job.requestedAt()))
                .addValue("created_at", timestampWithTimeZone(job.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(job.updatedAt())));
        return find(job.id()).orElseThrow();
    }

    Optional<DataCleanupJobRecord> find(String id) {
        return jdbcTemplate.query("SELECT * FROM data_cleanup_jobs WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id), this::map).stream().findFirst();
    }

    List<DataCleanupJobRecord> list(String status) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT * FROM data_cleanup_jobs");
        if (status != null && !status.isBlank()) {
            sql.append(" WHERE status = :status");
            p.addValue("status", status.trim());
        }
        sql.append(" ORDER BY created_at DESC");
        return jdbcTemplate.query(sql.toString(), p, this::map);
    }

    DataCleanupJobRecord updateStatus(DataCleanupJobRecord job) {
        jdbcTemplate.update("""
                UPDATE data_cleanup_jobs SET
                    status = :status,
                    reason = :reason,
                    approved_by = :approved_by,
                    approved_at = :approved_at,
                    executed_at = :executed_at,
                    deleted_count = :deleted_count,
                    updated_at = :updated_at
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("status", job.status())
                .addValue("reason", job.reason())
                .addValue("approved_by", job.approvedBy())
                .addValue("approved_at", timestampWithTimeZone(job.approvedAt()))
                .addValue("executed_at", timestampWithTimeZone(job.executedAt()))
                .addValue("deleted_count", job.deletedCount())
                .addValue("updated_at", timestampWithTimeZone(job.updatedAt()))
                .addValue("id", job.id()));
        return find(job.id()).orElseThrow();
    }

    private DataCleanupJobRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new DataCleanupJobRecord(
                rs.getString("id"),
                rs.getString("scope_type"),
                readMap(rs.getString("filter_params")),
                rs.getString("status"),
                rs.getInt("preview_count"),
                readList(rs.getString("preview_sample")),
                rs.getString("reason"),
                rs.getString("requested_by"),
                instant(rs.getObject("requested_at", OffsetDateTime.class)),
                rs.getString("approved_by"),
                instant(rs.getObject("approved_at", OffsetDateTime.class)),
                instant(rs.getObject("executed_at", OffsetDateTime.class)),
                (Integer) rs.getObject("deleted_count"),
                instant(rs.getObject("created_at", OffsetDateTime.class)),
                instant(rs.getObject("updated_at", OffsetDateTime.class)));
    }

    private static Instant instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception e) {
            throw ApiException.badRequest("VALIDATION_FAILED", "failed to serialize json");
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    // 供 Service 换算 cutoff（保留在此仅为集中依赖，Service 亦可自算；此处不用）。
    static Instant cutoffFor(Instant now, int olderThanDays) {
        return now.minus(olderThanDays, ChronoUnit.DAYS);
    }
}
