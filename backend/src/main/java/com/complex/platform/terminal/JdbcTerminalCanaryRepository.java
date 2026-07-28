package com.complex.platform.terminal;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 终端开关灰度层仓储 JDBC 实现（V342，R-10）。 */
@Repository
class JdbcTerminalCanaryRepository implements TerminalCanaryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcTerminalCanaryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<TerminalCanaryRolloutRecord> findActiveRollout(
            String terminalType, String capabilityCode) {
        return jdbcTemplate.query("""
                SELECT id, terminal_type, capability_code, active, reason, created_at, updated_at
                  FROM terminal_capability_canary_rollouts
                 WHERE terminal_type IN (:terminal_type, '*')
                   AND capability_code = :capability_code
                   AND active = TRUE
                 ORDER BY CASE WHEN terminal_type = '*' THEN 1 ELSE 0 END, updated_at DESC, id
                 LIMIT 1
                """, Map.of(
                "terminal_type", terminalType == null ? "" : terminalType,
                "capability_code", capabilityCode == null ? "" : capabilityCode
        ), this::mapRollout).stream().findFirst();
    }

    @Override
    public List<TerminalCanaryTargetRecord> listActiveTargets(String rolloutId) {
        return jdbcTemplate.query("""
                SELECT id, rollout_id, target_type, target_id, reason, created_at, updated_at,
                       archived_at, archive_reason, archived_by
                  FROM terminal_capability_canary_targets
                 WHERE rollout_id = :rollout_id
                   AND archived_at IS NULL
                 ORDER BY created_at, id
                """, Map.of("rollout_id", rolloutId == null ? "" : rolloutId), this::mapTarget);
    }

    @Override
    public List<TerminalCanaryRolloutRecord> listRollouts() {
        return jdbcTemplate.query("""
                SELECT id, terminal_type, capability_code, active, reason, created_at, updated_at
                  FROM terminal_capability_canary_rollouts
                 ORDER BY capability_code, terminal_type, id
                """, Map.of(), this::mapRollout);
    }

    @Override
    public Optional<TerminalCanaryRolloutRecord> findRolloutById(String id) {
        return jdbcTemplate.query("""
                SELECT id, terminal_type, capability_code, active, reason, created_at, updated_at
                  FROM terminal_capability_canary_rollouts
                 WHERE id = :id
                """, Map.of("id", id == null ? "" : id), this::mapRollout).stream().findFirst();
    }

    @Override
    public Optional<TerminalCanaryRolloutRecord> findRollout(String terminalType, String capabilityCode) {
        return jdbcTemplate.query("""
                SELECT id, terminal_type, capability_code, active, reason, created_at, updated_at
                  FROM terminal_capability_canary_rollouts
                 WHERE terminal_type = :terminal_type
                   AND capability_code = :capability_code
                """, Map.of(
                "terminal_type", terminalType == null ? "" : terminalType,
                "capability_code", capabilityCode == null ? "" : capabilityCode
        ), this::mapRollout).stream().findFirst();
    }

    @Override
    public TerminalCanaryRolloutRecord saveRollout(TerminalCanaryRolloutRecord record) {
        jdbcTemplate.update("""
                INSERT INTO terminal_capability_canary_rollouts (
                    id, terminal_type, capability_code, active, reason, created_at, updated_at
                )
                VALUES (
                    :id, :terminal_type, :capability_code, :active, :reason, :created_at, :updated_at
                )
                ON CONFLICT (terminal_type, capability_code) DO UPDATE SET
                    active = EXCLUDED.active,
                    reason = EXCLUDED.reason,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("terminal_type", record.terminalType())
                .addValue("capability_code", record.capabilityCode())
                .addValue("active", record.active())
                .addValue("reason", record.reason())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt())));
        return findRollout(record.terminalType(), record.capabilityCode()).orElse(record);
    }

    @Override
    public Optional<TerminalCanaryTargetRecord> findTargetById(String id) {
        return jdbcTemplate.query("""
                SELECT id, rollout_id, target_type, target_id, reason, created_at, updated_at,
                       archived_at, archive_reason, archived_by
                  FROM terminal_capability_canary_targets
                 WHERE id = :id
                """, Map.of("id", id == null ? "" : id), this::mapTarget).stream().findFirst();
    }

    @Override
    public TerminalCanaryTargetRecord saveTarget(TerminalCanaryTargetRecord record) {
        jdbcTemplate.update("""
                INSERT INTO terminal_capability_canary_targets (
                    id, rollout_id, target_type, target_id, reason, created_at, updated_at,
                    archived_at, archive_reason, archived_by
                )
                VALUES (
                    :id, :rollout_id, :target_type, :target_id, :reason, :created_at, :updated_at,
                    :archived_at, :archive_reason, :archived_by
                )
                ON CONFLICT (rollout_id, target_type, target_id) DO UPDATE SET
                    reason = EXCLUDED.reason,
                    updated_at = EXCLUDED.updated_at,
                    archived_at = EXCLUDED.archived_at,
                    archive_reason = EXCLUDED.archive_reason,
                    archived_by = EXCLUDED.archived_by
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("rollout_id", record.rolloutId())
                .addValue("target_type", record.targetType())
                .addValue("target_id", record.targetId())
                .addValue("reason", record.reason())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()))
                .addValue("archived_at", timestampWithTimeZone(record.archivedAt()))
                .addValue("archive_reason", record.archiveReason())
                .addValue("archived_by", record.archivedBy()));
        return jdbcTemplate.query("""
                SELECT id, rollout_id, target_type, target_id, reason, created_at, updated_at,
                       archived_at, archive_reason, archived_by
                  FROM terminal_capability_canary_targets
                 WHERE rollout_id = :rollout_id
                   AND target_type = :target_type
                   AND target_id = :target_id
                """, Map.of(
                "rollout_id", record.rolloutId(),
                "target_type", record.targetType(),
                "target_id", record.targetId()
        ), this::mapTarget).stream().findFirst().orElse(record);
    }

    @Override
    public TerminalCanaryTargetRecord archiveTarget(
            String id, Instant archivedAt, String archiveReason, String archivedBy) {
        jdbcTemplate.update("""
                UPDATE terminal_capability_canary_targets
                   SET archived_at = :archived_at,
                       archive_reason = :archive_reason,
                       archived_by = :archived_by,
                       updated_at = :archived_at
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("archived_at", timestampWithTimeZone(archivedAt))
                .addValue("archive_reason", archiveReason)
                .addValue("archived_by", archivedBy));
        return findTargetById(id).orElseThrow();
    }

    private TerminalCanaryRolloutRecord mapRollout(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TerminalCanaryRolloutRecord(
                resultSet.getString("id"),
                resultSet.getString("terminal_type"),
                resultSet.getString("capability_code"),
                resultSet.getBoolean("active"),
                resultSet.getString("reason"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private TerminalCanaryTargetRecord mapTarget(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TerminalCanaryTargetRecord(
                resultSet.getString("id"),
                resultSet.getString("rollout_id"),
                resultSet.getString("target_type"),
                resultSet.getString("target_id"),
                resultSet.getString("reason"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                nullableInstant(resultSet, "archived_at"),
                resultSet.getString("archive_reason"),
                resultSet.getString("archived_by")
        );
    }
}
