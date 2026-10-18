package com.complex.platform.amusement;

// 文件版本：V0.01

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** CG-08a 全园故障状态读写（amusement_park_wide_faults / amusement_park_wide_fault_actions）。 */
@Repository
public class JdbcAmusementParkWideFaultRepository {

    private static final String SELECT = """
            SELECT id, project_id, status, reason, initiated_by, initiated_role, initiated_at,
                   activation_confirmed_by, activation_confirmed_role, activated_at,
                   lift_initiated_by, lift_initiated_role, lift_initiated_at, lift_reason,
                   lift_confirmed_by, lift_confirmed_role, lifted_at, created_at, updated_at
              FROM amusement_park_wide_faults
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAmusementParkWideFaultRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AmusementParkWideFaultRecord> findById(String id) {
        return jdbcTemplate.query(SELECT + " WHERE id = :id", new MapSqlParameterSource("id", id), this::map)
                .stream().findFirst();
    }

    /** 该综合体当前未解除（pending_activation / active / pending_lift）的全园故障状态。 */
    public Optional<AmusementParkWideFaultRecord> findOpenByProject(String projectId) {
        return jdbcTemplate.query(SELECT + " WHERE project_id = :project_id AND status <> 'lifted' ORDER BY created_at DESC",
                new MapSqlParameterSource("project_id", projectId), this::map).stream().findFirst();
    }

    /** 该综合体是否处于生效中的全园故障状态（售票闸依据）：生效后直至解除经双岗位确认（含待确认解除阶段）均停售。 */
    public boolean existsActive(String projectId) {
        lockStatus(false);
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM amusement_park_wide_faults WHERE project_id = :project_id AND status IN ('active', 'pending_lift')",
                new MapSqlParameterSource("project_id", projectId), Integer.class);
        return n != null && n > 0;
    }

    /** 任一综合体处于生效中（订单无法定位综合体时 fail-closed 使用）。 */
    public boolean existsAnyActive() {
        lockStatus(false);
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM amusement_park_wide_faults WHERE status IN ('active', 'pending_lift')",
                new MapSqlParameterSource(), Integer.class);
        return n != null && n > 0;
    }

    public List<AmusementParkWideFaultRecord> list(String projectId, String status) {
        return jdbcTemplate.query(SELECT + """
                 WHERE (:project_id::text IS NULL OR project_id = :project_id)
                   AND (:status::text IS NULL OR status = :status)
                 ORDER BY created_at DESC
                 LIMIT 200
                """, new MapSqlParameterSource().addValue("project_id", projectId).addValue("status", status), this::map);
    }

    public AmusementParkWideFaultRecord insert(AmusementParkWideFaultRecord r) {
        jdbcTemplate.update("""
                INSERT INTO amusement_park_wide_faults (id, project_id, status, reason, initiated_by, initiated_role, initiated_at,
                        created_at, updated_at)
                VALUES (:id, :project_id, :status, :reason, :initiated_by, :initiated_role, :initiated_at, :created_at, :updated_at)
                """, new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("project_id", r.projectId()).addValue("status", r.status())
                .addValue("reason", r.reason()).addValue("initiated_by", r.initiatedBy()).addValue("initiated_role", r.initiatedRole())
                .addValue("initiated_at", ts(r.initiatedAt())).addValue("created_at", ts(r.createdAt()))
                .addValue("updated_at", ts(r.updatedAt())));
        return r;
    }

    public AmusementParkWideFaultRecord update(AmusementParkWideFaultRecord r) {
        lockStatus(true);
        jdbcTemplate.update("""
                UPDATE amusement_park_wide_faults
                   SET status = :status,
                       activation_confirmed_by = :activation_confirmed_by,
                       activation_confirmed_role = :activation_confirmed_role,
                       activated_at = :activated_at,
                       lift_initiated_by = :lift_initiated_by,
                       lift_initiated_role = :lift_initiated_role,
                       lift_initiated_at = :lift_initiated_at,
                       lift_reason = :lift_reason,
                       lift_confirmed_by = :lift_confirmed_by,
                       lift_confirmed_role = :lift_confirmed_role,
                       lifted_at = :lifted_at,
                       updated_at = :updated_at
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("status", r.status())
                .addValue("activation_confirmed_by", r.activationConfirmedBy())
                .addValue("activation_confirmed_role", r.activationConfirmedRole())
                .addValue("activated_at", ts(r.activatedAt()))
                .addValue("lift_initiated_by", r.liftInitiatedBy()).addValue("lift_initiated_role", r.liftInitiatedRole())
                .addValue("lift_initiated_at", ts(r.liftInitiatedAt())).addValue("lift_reason", r.liftReason())
                .addValue("lift_confirmed_by", r.liftConfirmedBy()).addValue("lift_confirmed_role", r.liftConfirmedRole())
                .addValue("lifted_at", ts(r.liftedAt())).addValue("updated_at", ts(r.updatedAt())));
        return r;
    }

    public AmusementParkWideFaultActionRecord insertAction(AmusementParkWideFaultActionRecord a) {
        jdbcTemplate.update("""
                INSERT INTO amusement_park_wide_fault_actions (id, fault_id, project_id, action, actor_id, actor_role, note, created_at)
                VALUES (:id, :fault_id, :project_id, :action, :actor_id, :actor_role, :note, :created_at)
                """, new MapSqlParameterSource()
                .addValue("id", a.id()).addValue("fault_id", a.faultId()).addValue("project_id", a.projectId())
                .addValue("action", a.action()).addValue("actor_id", a.actorId()).addValue("actor_role", a.actorRole())
                .addValue("note", a.note()).addValue("created_at", ts(a.createdAt())));
        return a;
    }

    public List<AmusementParkWideFaultActionRecord> listActions(String faultId) {
        return jdbcTemplate.query("""
                SELECT id, fault_id, project_id, action, actor_id, actor_role, note, created_at
                  FROM amusement_park_wide_fault_actions
                 WHERE fault_id = :fault_id
                 ORDER BY created_at, id
                """, new MapSqlParameterSource("fault_id", faultId), (rs, n) -> new AmusementParkWideFaultActionRecord(
                rs.getString("id"), rs.getString("fault_id"), rs.getString("project_id"), rs.getString("action"),
                rs.getString("actor_id"), rs.getString("actor_role"), rs.getString("note"), instant(rs, "created_at")));
    }

    // 售票/核销读闸持共享锁至事务结束；故障生效/解除独占同锁，避免验闸后状态切换。
    private void lockStatus(boolean writing) {
        jdbcTemplate.queryForObject("SELECT " + (writing ? "pg_advisory_xact_lock" : "pg_advisory_xact_lock_shared")
                + "(hashtextextended('amusement_park_wide_fault_gate', 0))", new MapSqlParameterSource(), Object.class);
    }

    private AmusementParkWideFaultRecord map(ResultSet rs, int n) throws SQLException {
        return new AmusementParkWideFaultRecord(rs.getString("id"), rs.getString("project_id"), rs.getString("status"),
                rs.getString("reason"), rs.getString("initiated_by"), rs.getString("initiated_role"), instant(rs, "initiated_at"),
                rs.getString("activation_confirmed_by"), rs.getString("activation_confirmed_role"), instant(rs, "activated_at"),
                rs.getString("lift_initiated_by"), rs.getString("lift_initiated_role"), instant(rs, "lift_initiated_at"),
                rs.getString("lift_reason"), rs.getString("lift_confirmed_by"), rs.getString("lift_confirmed_role"),
                instant(rs, "lifted_at"), instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toInstant();
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
