package com.complex.platform.facility;

// 文件版本：V0.02

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** HP-F01 接警单、响应时限矩阵、值守安排、联系与接手留痕（V529）及故障处置规则（V530）的 JDBC 仓储。 */
// ponytail: 仓储直用实现类，无接口(one impl)。
@Repository
class JdbcFacilityAlarmRepository {

    private static final String ALARM_COLS = """
            id, equipment_id, work_order_id, project_id, alarm_type, priority, alarm_source, note, status, raised_at,
            response_due_at, escalation_round, fallback_to_backup_at, holder_user_id, holder_shift_id,
            holder_shift_end_at, taken_over_at, uncovered, created_at, updated_at, applied_rule_id, disposition_note,
            disposition_by, disposition_at
            """;
    private static final String RULE_COLS = """
            id, project_id, equipment_id, equipment_type, fault_scenario, handling_requirement, status,
            specialist_confirmed_by, specialist_confirmed_at, vendor_confirmed_by, vendor_confirmed_at, created_by,
            created_at, updated_at
            """;
    private static final String DUTY_SELECT = """
            SELECT d.id, d.project_id, d.shift_id, s.user_id AS shift_user_id, s.start_at AS shift_start_at,
                   s.end_at AS shift_end_at, d.duty_role, d.contact_phone, d.created_by, d.created_at,
                   d.cancelled_by, d.cancelled_at, d.cancel_reason
              FROM facility_alarm_duty_assignments d
              JOIN staff_shift_schedules s ON s.id = d.shift_id
            """;

    /** 班次概要：值守安排校验用。 */
    record ShiftRow(String id, String userId, String projectId, Instant startAt, Instant endAt) {}

    /** 设备概要：派发消息的设备名与故障处置规则的项目、分类。 */
    record EquipmentRow(String id, String name, String projectId, String equipmentType) {}

    private final NamedParameterJdbcTemplate jdbc;

    JdbcFacilityAlarmRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- 响应时限矩阵

    Optional<FacilityAlarmPolicyRecord> findActivePolicy(String alarmType, String priority) {
        return jdbc.query("""
                SELECT alarm_type, priority, response_minutes, active, created_at, updated_at
                  FROM facility_alarm_response_policies
                 WHERE alarm_type = :type AND priority = :priority AND active = TRUE
                """, Map.of("type", alarmType, "priority", priority), this::mapPolicy).stream().findFirst();
    }

    FacilityAlarmPolicyRecord savePolicy(String alarmType, String priority, int responseMinutes, boolean active, Instant now) {
        return jdbc.queryForObject("""
                INSERT INTO facility_alarm_response_policies (alarm_type, priority, response_minutes, active, created_at, updated_at)
                VALUES (:type, :priority, :minutes, :active, :now, :now)
                ON CONFLICT (alarm_type, priority) DO UPDATE SET
                    response_minutes = EXCLUDED.response_minutes, active = EXCLUDED.active, updated_at = EXCLUDED.updated_at
                RETURNING alarm_type, priority, response_minutes, active, created_at, updated_at
                """, new MapSqlParameterSource().addValue("type", alarmType).addValue("priority", priority)
                .addValue("minutes", responseMinutes).addValue("active", active)
                .addValue("now", timestampWithTimeZone(now)), this::mapPolicy);
    }

    /** 该格响应时限（不论是否仍启用）：接警单落单时已按它计时，升级与交接顺延沿用同一格。 */
    int responseMinutes(String alarmType, String priority) {
        return jdbc.queryForObject("""
                SELECT response_minutes FROM facility_alarm_response_policies WHERE alarm_type = :type AND priority = :priority
                """, Map.of("type", alarmType, "priority", priority), Integer.class);
    }

    Optional<EquipmentRow> findEquipment(String id) {
        return jdbc.query("SELECT id, name, project_id, equipment_type FROM facility_equipment WHERE id = :id",
                Map.of("id", id), (rs, n) -> new EquipmentRow(rs.getString("id"), rs.getString("name"),
                        rs.getString("project_id"), rs.getString("equipment_type"))).stream().findFirst();
    }

    // ---------------------------------------------------------------- 接警单

    void insertAlarm(FacilityAlarmRecord a) {
        jdbc.update("""
                INSERT INTO facility_alarms (id, equipment_id, work_order_id, project_id, alarm_type, priority,
                    alarm_source, note, status, raised_at, response_due_at, created_at, updated_at)
                VALUES (:id, :equipment_id, :work_order_id, :project_id, :alarm_type, :priority,
                    :alarm_source, :note, :status, :raised_at, :response_due_at, :created_at, :updated_at)
                """, new MapSqlParameterSource()
                .addValue("id", a.id()).addValue("equipment_id", a.equipmentId())
                .addValue("work_order_id", a.workOrderId()).addValue("project_id", a.projectId())
                .addValue("alarm_type", a.alarmType()).addValue("priority", a.priority())
                .addValue("alarm_source", a.alarmSource()).addValue("note", a.note()).addValue("status", a.status())
                .addValue("raised_at", timestampWithTimeZone(a.raisedAt()))
                .addValue("response_due_at", timestampWithTimeZone(a.responseDueAt()))
                .addValue("created_at", timestampWithTimeZone(a.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(a.updatedAt())));
    }

    Optional<FacilityAlarmRecord> findAlarm(String id) {
        return jdbc.query("SELECT " + ALARM_COLS + " FROM facility_alarms WHERE id = :id",
                Map.of("id", id), this::mapAlarm).stream().findFirst();
    }

    boolean awaitingTakeoverForWorkOrder(String workOrderId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM facility_alarms WHERE work_order_id = :work_order_id AND status = 'awaiting_takeover')
                """, Map.of("work_order_id", workOrderId), Boolean.class));
    }

    Optional<FacilityAlarmRecord> findAlarmForUpdate(String id) {
        return jdbc.query("SELECT " + ALARM_COLS + " FROM facility_alarms WHERE id = :id FOR UPDATE",
                Map.of("id", id), this::mapAlarm).stream().findFirst();
    }

    void markFallbackToBackup(String id, Instant at) {
        jdbc.update("UPDATE facility_alarms SET fallback_to_backup_at = :at, updated_at = :at WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("at", timestampWithTimeZone(at)));
    }

    void markTakenOver(String id, String userId, String shiftId, Instant shiftEndAt, Instant at) {
        jdbc.update("""
                UPDATE facility_alarms SET status = 'taken_over', holder_user_id = :user, holder_shift_id = :shift,
                       holder_shift_end_at = :shift_end, taken_over_at = :at, updated_at = :at
                 WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("user", userId).addValue("shift", shiftId)
                .addValue("shift_end", timestampWithTimeZone(shiftEndAt)).addValue("at", timestampWithTimeZone(at)));
    }

    void markUncovered(String id) {
        jdbc.update("UPDATE facility_alarms SET uncovered = TRUE WHERE id = :id", Map.of("id", id));
    }

    /** 待接手且已到响应时限的接警单。 */
    List<FacilityAlarmRecord> listDueForEscalation(Instant now) {
        return jdbc.query("SELECT " + ALARM_COLS + """
                 FROM facility_alarms WHERE status = 'awaiting_takeover' AND response_due_at <= :now
                 ORDER BY response_due_at, id
                """, Map.of("now", timestampWithTimeZone(now)), this::mapAlarm);
    }

    /** 升级一轮：轮次加一、响应时限顺延；只在仍待接手且轮次未被别处推进时生效。 */
    boolean advanceEscalation(String id, int fromRound, Instant nextDue, Instant now) {
        return jdbc.update("""
                UPDATE facility_alarms SET escalation_round = :to, response_due_at = :due, updated_at = :now
                 WHERE id = :id AND status = 'awaiting_takeover' AND escalation_round = :from
                """, new MapSqlParameterSource().addValue("id", id).addValue("from", fromRound).addValue("to", fromRound + 1)
                .addValue("due", timestampWithTimeZone(nextDue)).addValue("now", timestampWithTimeZone(now))) == 1;
    }

    /** 接手人此刻已不当班：那一段班次已结束，或领班给那一段追加了此刻生效的顶班（V422 已开始班次只追加顶班记录）。 */
    private static final String HOLDER_OFF_DUTY = """
            (a.holder_shift_end_at <= :now OR EXISTS (SELECT 1 FROM staff_shift_covers c
                 WHERE c.shift_id = a.holder_shift_id AND c.start_at <= :now AND c.end_at > :now))
            """;

    /** 已接手、接手人此刻已不当班、且关联工单未关闭的接警单。 */
    List<FacilityAlarmRecord> listHandoverDue(Instant now) {
        return jdbc.query("SELECT a.* FROM facility_alarms a JOIN facility_work_orders w ON w.id = a.work_order_id"
                + " WHERE a.status = 'taken_over' AND w.status <> 'closed' AND " + HOLDER_OFF_DUTY
                + " ORDER BY a.holder_shift_end_at, a.id", Map.of("now", timestampWithTimeZone(now)), this::mapAlarm);
    }

    /** 换班交接：回到待接手、清持有人（接手历史留在 facility_alarm_takeovers），轮次加一并重新计响应时限。 */
    boolean requireHandover(String id, int fromRound, Instant nextDue, Instant now) {
        return jdbc.update("""
                UPDATE facility_alarms a SET status = 'awaiting_takeover', holder_user_id = NULL, holder_shift_id = NULL,
                       holder_shift_end_at = NULL, taken_over_at = NULL, escalation_round = :to, response_due_at = :due,
                       updated_at = :now
                 WHERE a.id = :id AND a.status = 'taken_over' AND a.escalation_round = :from AND """ + HOLDER_OFF_DUTY,
                new MapSqlParameterSource().addValue("id", id).addValue("from", fromRound).addValue("to", fromRound + 1)
                .addValue("due", timestampWithTimeZone(nextDue)).addValue("now", timestampWithTimeZone(now))) == 1;
    }

    void recordDisposition(String id, String ruleId, String note, String by, Instant at) {
        jdbc.update("""
                UPDATE facility_alarms SET applied_rule_id = :rule, disposition_note = :note, disposition_by = :by,
                       disposition_at = :at, updated_at = :at
                 WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("rule", ruleId).addValue("note", note)
                .addValue("by", by).addValue("at", timestampWithTimeZone(at)));
    }

    /** 未覆盖、尚未记录处置要求且工单未关闭的接警单（当班专业处置人员待判定）。 */
    List<FacilityAlarmRecord> listUncoveredUndisposed() {
        return jdbc.query("""
                SELECT a.* FROM facility_alarms a JOIN facility_work_orders w ON w.id = a.work_order_id
                 WHERE a.uncovered AND a.disposition_at IS NULL AND w.status <> 'closed'
                 ORDER BY a.raised_at, a.id
                """, this::mapAlarm);
    }

    /** 第十八批 G 段 82b-R1：projectIds 为 null 不限项目（平台管理员），否则只取这些项目的（空集合即空表），在库里筛、不占 500 条名额。 */
    List<FacilityAlarmRecord> listAlarms(Collection<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query("SELECT " + ALARM_COLS + " FROM facility_alarms" + (projectIds == null ? "" : " WHERE project_id IN (:projects)")
                + " ORDER BY raised_at DESC, id LIMIT 500", new MapSqlParameterSource().addValue("projects", projectIds), this::mapAlarm);
    }

    List<FacilityAlarmRecord> listAwaitingTakeover() {
        return jdbc.query("SELECT " + ALARM_COLS
                + " FROM facility_alarms WHERE status = 'awaiting_takeover' ORDER BY response_due_at, id", this::mapAlarm);
    }

    /** 本人持有、且关联工单未关闭的接警单。 */
    List<FacilityAlarmRecord> listHeldOpenBy(String userId) {
        return jdbc.query("""
                SELECT a.* FROM facility_alarms a JOIN facility_work_orders w ON w.id = a.work_order_id
                 WHERE a.status = 'taken_over' AND a.holder_user_id = :user AND w.status <> 'closed'
                 ORDER BY a.taken_over_at DESC, a.id
                """, Map.of("user", userId), this::mapAlarm);
    }

    // ---------------------------------------------------------------- 联系与接手留痕

    void insertAttempt(FacilityAlarmRecord.Attempt t) {
        jdbc.update("""
                INSERT INTO facility_alarm_contact_attempts (id, alarm_id, round, user_id, duty_role, channel, outcome,
                    provider_ref, reason, attempted_at)
                VALUES (:id, :alarm_id, :round, :user_id, :duty_role, :channel, :outcome, :provider_ref, :reason, :at)
                """, new MapSqlParameterSource().addValue("id", t.id()).addValue("alarm_id", t.alarmId())
                .addValue("round", t.round()).addValue("user_id", t.userId()).addValue("duty_role", t.dutyRole())
                .addValue("channel", t.channel()).addValue("outcome", t.outcome())
                .addValue("provider_ref", t.providerRef()).addValue("reason", t.reason())
                .addValue("at", timestampWithTimeZone(t.attemptedAt())));
    }

    List<FacilityAlarmRecord.Attempt> listAttempts(Collection<String> alarmIds) {
        if (alarmIds.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT id, alarm_id, round, user_id, duty_role, channel, outcome, provider_ref, reason, attempted_at
                  FROM facility_alarm_contact_attempts WHERE alarm_id IN (:ids) ORDER BY attempted_at, id
                """, Map.of("ids", alarmIds), (rs, n) -> new FacilityAlarmRecord.Attempt(rs.getString("id"),
                rs.getString("alarm_id"), rs.getInt("round"), rs.getString("user_id"), rs.getString("duty_role"),
                rs.getString("channel"), rs.getString("outcome"), rs.getString("provider_ref"), rs.getString("reason"),
                nullableInstant(rs, "attempted_at")));
    }

    boolean hasTakeover(String alarmId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM facility_alarm_takeovers WHERE alarm_id = :id)",
                Map.of("id", alarmId), Boolean.class));
    }

    void insertTakeover(FacilityAlarmRecord.Takeover t) {
        jdbc.update("""
                INSERT INTO facility_alarm_takeovers (id, alarm_id, user_id, shift_id, duty_role, kind, note, taken_over_at)
                VALUES (:id, :alarm_id, :user_id, :shift_id, :duty_role, :kind, :note, :at)
                """, new MapSqlParameterSource().addValue("id", t.id()).addValue("alarm_id", t.alarmId())
                .addValue("user_id", t.userId()).addValue("shift_id", t.shiftId()).addValue("duty_role", t.dutyRole())
                .addValue("kind", t.kind()).addValue("note", t.note()).addValue("at", timestampWithTimeZone(t.takenOverAt())));
    }

    List<FacilityAlarmRecord.Takeover> listTakeovers(Collection<String> alarmIds) {
        if (alarmIds.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT id, alarm_id, user_id, shift_id, duty_role, kind, note, taken_over_at
                  FROM facility_alarm_takeovers WHERE alarm_id IN (:ids) ORDER BY taken_over_at, id
                """, Map.of("ids", alarmIds), (rs, n) -> new FacilityAlarmRecord.Takeover(rs.getString("id"),
                rs.getString("alarm_id"), rs.getString("user_id"), rs.getString("shift_id"), rs.getString("duty_role"),
                rs.getString("kind"), rs.getString("note"), nullableInstant(rs, "taken_over_at")));
    }

    // ---------------------------------------------------------------- 接警值守安排

    Optional<ShiftRow> findShift(String shiftId) {
        return jdbc.query("SELECT id, user_id, project_id, start_at, end_at FROM staff_shift_schedules WHERE id = :id",
                Map.of("id", shiftId), (rs, n) -> new ShiftRow(rs.getString("id"), rs.getString("user_id"),
                        rs.getString("project_id"), nullableInstant(rs, "start_at"), nullableInstant(rs, "end_at")))
                .stream().findFirst();
    }

    boolean activeDutyExists(String shiftId, String dutyRole) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM facility_alarm_duty_assignments
                               WHERE shift_id = :shift AND duty_role = :role AND cancelled_at IS NULL)
                """, Map.of("shift", shiftId, "role", dutyRole), Boolean.class));
    }

    void insertDuty(String id, String projectId, String shiftId, String dutyRole, String contactPhone, String createdBy,
            Instant now) {
        jdbc.update("""
                INSERT INTO facility_alarm_duty_assignments (id, project_id, shift_id, duty_role, contact_phone, created_by, created_at)
                VALUES (:id, :project, :shift, :role, :phone, :by, :now)
                """, new MapSqlParameterSource().addValue("id", id).addValue("project", projectId)
                .addValue("shift", shiftId).addValue("role", dutyRole).addValue("phone", contactPhone)
                .addValue("by", createdBy).addValue("now", timestampWithTimeZone(now)));
    }

    Optional<FacilityAlarmDutyRecord> findDuty(String id, boolean forUpdate) {
        return jdbc.query(DUTY_SELECT + " WHERE d.id = :id" + (forUpdate ? " FOR UPDATE OF d" : ""),
                Map.of("id", id), this::mapDuty).stream().findFirst();
    }

    void cancelDuty(String id, String cancelledBy, String reason, Instant at) {
        jdbc.update("""
                UPDATE facility_alarm_duty_assignments SET cancelled_by = :by, cancelled_at = :at, cancel_reason = :reason
                 WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("by", cancelledBy)
                .addValue("reason", reason).addValue("at", timestampWithTimeZone(at)));
    }

    /** 82b-R1：同 {@link #listAlarms}，projectIds 为 null 不限项目。 */
    List<FacilityAlarmDutyRecord> listDuties(Collection<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(DUTY_SELECT + (projectIds == null ? "" : " WHERE d.project_id IN (:projects)")
                + " ORDER BY s.start_at DESC, d.duty_role, d.id LIMIT 500",
                new MapSqlParameterSource().addValue("projects", projectIds), this::mapDuty);
    }

    /** 本项目此刻在进行的已发布班次上、未撤销且属指定角色的值守安排。 */
    List<FacilityAlarmDutyRecord> listActiveDutiesAt(String projectId, Collection<String> roles, Instant at) {
        return jdbc.query(DUTY_SELECT + """
                 WHERE d.project_id = :project AND d.cancelled_at IS NULL AND d.duty_role IN (:roles)
                   AND s.start_at <= :at AND s.end_at > :at
                 ORDER BY d.duty_role, d.id
                """, new MapSqlParameterSource().addValue("project", projectId).addValue("roles", roles)
                .addValue("at", timestampWithTimeZone(at)), this::mapDuty);
    }

    // ---------------------------------------------------------------- 故障处置规则

    void insertFaultRule(FacilityAlarmRecord.FaultRule r) {
        jdbc.update("""
                INSERT INTO facility_fault_handling_rules (id, project_id, equipment_id, equipment_type, fault_scenario,
                    handling_requirement, status, created_by, created_at, updated_at)
                VALUES (:id, :project, :equipment, :type, :scenario, :requirement, :status, :by, :at, :at)
                """, new MapSqlParameterSource().addValue("id", r.id()).addValue("project", r.projectId())
                .addValue("equipment", r.equipmentId()).addValue("type", r.equipmentType())
                .addValue("scenario", r.faultScenario()).addValue("requirement", r.handlingRequirement())
                .addValue("status", r.status()).addValue("by", r.createdBy()).addValue("at", timestampWithTimeZone(r.createdAt())));
    }

    Optional<FacilityAlarmRecord.FaultRule> findFaultRule(String id, boolean forUpdate) {
        return jdbc.query("SELECT " + RULE_COLS + " FROM facility_fault_handling_rules WHERE id = :id"
                + (forUpdate ? " FOR UPDATE" : ""), Map.of("id", id), this::mapRule).stream().findFirst();
    }

    /** 记一方确认（vendor＝维保单位，否则设备专业负责人）；两方都已确认即生效。 */
    void confirmFaultRule(String id, boolean vendor, String by, Instant at) {
        String side = vendor ? "vendor" : "specialist";
        jdbc.update("UPDATE facility_fault_handling_rules SET " + side + "_confirmed_by = :by, " + side
                + "_confirmed_at = :at, updated_at = :at WHERE id = :id", new MapSqlParameterSource().addValue("id", id)
                .addValue("by", by).addValue("at", timestampWithTimeZone(at)));
        jdbc.update("""
                UPDATE facility_fault_handling_rules SET status = 'active'
                 WHERE id = :id AND specialist_confirmed_at IS NOT NULL AND vendor_confirmed_at IS NOT NULL
                """, Map.of("id", id));
    }

    void retireFaultRule(String id, Instant at) {
        jdbc.update("UPDATE facility_fault_handling_rules SET status = 'retired', updated_at = :at WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("at", timestampWithTimeZone(at)));
    }

    /** 82b-R1：同 {@link #listAlarms}，projectIds 为 null 不限项目。 */
    List<FacilityAlarmRecord.FaultRule> listFaultRules(Collection<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query("SELECT " + RULE_COLS + " FROM facility_fault_handling_rules"
                + (projectIds == null ? "" : " WHERE project_id IN (:projects)") + " ORDER BY created_at DESC, id LIMIT 500",
                new MapSqlParameterSource().addValue("projects", projectIds), this::mapRule);
    }

    /** 覆盖该设备的生效规则：按该设备登记的，或本项目同分类通用的。 */
    List<FacilityAlarmRecord.FaultRule> listActiveRulesFor(String projectId, String equipmentId, String equipmentType) {
        if (projectId == null) return List.of();
        return jdbc.query("SELECT " + RULE_COLS + """
                 FROM facility_fault_handling_rules
                 WHERE status = 'active' AND project_id = :project
                   AND (equipment_id = :equipment OR (equipment_id IS NULL AND equipment_type = :type))
                 ORDER BY equipment_id NULLS LAST, created_at, id
                """, new MapSqlParameterSource().addValue("project", projectId).addValue("equipment", equipmentId)
                .addValue("type", equipmentType), this::mapRule);
    }

    // ---------------------------------------------------------------- row mappers

    private FacilityAlarmRecord.FaultRule mapRule(ResultSet rs, int n) throws SQLException {
        return new FacilityAlarmRecord.FaultRule(rs.getString("id"), rs.getString("project_id"), rs.getString("equipment_id"),
                rs.getString("equipment_type"), rs.getString("fault_scenario"), rs.getString("handling_requirement"),
                rs.getString("status"), rs.getString("specialist_confirmed_by"), nullableInstant(rs, "specialist_confirmed_at"),
                rs.getString("vendor_confirmed_by"), nullableInstant(rs, "vendor_confirmed_at"), rs.getString("created_by"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private FacilityAlarmPolicyRecord mapPolicy(ResultSet rs, int n) throws SQLException {
        return new FacilityAlarmPolicyRecord(rs.getString("alarm_type"), rs.getString("priority"),
                rs.getInt("response_minutes"), rs.getBoolean("active"), nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"));
    }

    private FacilityAlarmRecord mapAlarm(ResultSet rs, int n) throws SQLException {
        return new FacilityAlarmRecord(rs.getString("id"), rs.getString("equipment_id"), rs.getString("work_order_id"),
                rs.getString("project_id"), rs.getString("alarm_type"), rs.getString("priority"),
                rs.getString("alarm_source"), rs.getString("note"), rs.getString("status"),
                nullableInstant(rs, "raised_at"), nullableInstant(rs, "response_due_at"), rs.getInt("escalation_round"),
                nullableInstant(rs, "fallback_to_backup_at"), rs.getString("holder_user_id"),
                rs.getString("holder_shift_id"), nullableInstant(rs, "holder_shift_end_at"),
                nullableInstant(rs, "taken_over_at"), rs.getBoolean("uncovered"), nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"), rs.getString("applied_rule_id"), rs.getString("disposition_note"),
                rs.getString("disposition_by"), nullableInstant(rs, "disposition_at"));
    }

    private FacilityAlarmDutyRecord mapDuty(ResultSet rs, int n) throws SQLException {
        return new FacilityAlarmDutyRecord(rs.getString("id"), rs.getString("project_id"), rs.getString("shift_id"),
                rs.getString("shift_user_id"), nullableInstant(rs, "shift_start_at"), nullableInstant(rs, "shift_end_at"),
                rs.getString("duty_role"), rs.getString("contact_phone"), rs.getString("created_by"),
                nullableInstant(rs, "created_at"), rs.getString("cancelled_by"), nullableInstant(rs, "cancelled_at"),
                rs.getString("cancel_reason"));
    }
}
