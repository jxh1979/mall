package com.complex.platform.access.staff;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStaffRepository implements StaffRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcStaffRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------------------------------------------------------- F批 薪资/请假

    @Override
    public void upsertBaseSalary(String userId, long monthlySalaryCent, String updatedBy, java.time.Instant now) {
        jdbcTemplate.update("""
                INSERT INTO staff_base_salaries (user_id, monthly_salary_cent, updated_by, updated_at)
                VALUES (:user_id, :salary, :updated_by, :now)
                ON CONFLICT (user_id) DO UPDATE SET
                    monthly_salary_cent = EXCLUDED.monthly_salary_cent,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("salary", monthlySalaryCent)
                .addValue("updated_by", updatedBy).addValue("now", java.sql.Timestamp.from(now)));
    }

    @Override
    public void insertPayrollAdjustment(String id, String userId, String month, String adjType,
            long amountCent, String reason, String createdBy, java.time.Instant now) {
        jdbcTemplate.update("""
                INSERT INTO staff_payroll_adjustments (id, user_id, month, adj_type, amount_cent, reason, created_by, created_at)
                VALUES (:id, :user_id, :month, :adj_type, :amount, :reason, :created_by, :now)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("user_id", userId).addValue("month", month)
                .addValue("adj_type", adjType).addValue("amount", amountCent).addValue("reason", reason)
                .addValue("created_by", createdBy).addValue("now", java.sql.Timestamp.from(now)));
    }

    @Override
    public void insertCommission(String id, String userId, String month, String sourceType,
            String sourceId, long amountCent, String note, java.time.Instant now) {
        jdbcTemplate.update("""
                INSERT INTO staff_commission_ledger (id, user_id, month, source_type, source_id, amount_cent, note, created_at)
                VALUES (:id, :user_id, :month, :source_type, :source_id, :amount, :note, :now)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("user_id", userId).addValue("month", month)
                .addValue("source_type", sourceType).addValue("source_id", sourceId)
                .addValue("amount", amountCent).addValue("note", note)
                .addValue("now", java.sql.Timestamp.from(now)));
    }

    @Override
    public int closePayrollMonth(String month, String closedBy, java.time.Instant now) {
        // 月结重算:固定档案 + 台账提成 + 酒吧提成(bar_commission_calculations 按月/非作废) + 奖 - 罚;
        // 有任一薪资要素的员工都出单;幂等覆盖(uq user+month)。
        return jdbcTemplate.update("""
                INSERT INTO staff_payroll_statements (id, user_id, month, base_cent, commission_cent,
                        bonus_cent, penalty_cent, tax_cent, total_cent, closed_by, closed_at)
                SELECT 'pay_' || gen_random_uuid(), u.user_id, :month,
                       COALESCE(b.monthly_salary_cent, 0),
                       COALESCE(c.total, 0) + COALESCE(bc.total, 0),
                       COALESCE(adj.bonus, 0),
                       COALESCE(adj.penalty, 0),
                       -- H批:对当月服务劳务(service_order 台账)按劳务报酬预扣预缴法定税表代扣个税(单位:分)。
                       COALESCE(tax.tax_cent, 0),
                       COALESCE(b.monthly_salary_cent, 0) + COALESCE(c.total, 0) + COALESCE(bc.total, 0)
                           + COALESCE(adj.bonus, 0) - COALESCE(adj.penalty, 0) - COALESCE(tax.tax_cent, 0),
                       :closed_by, :now
                  FROM (SELECT user_id FROM staff_base_salaries
                        UNION SELECT user_id FROM staff_commission_ledger WHERE month = :month
                        UNION SELECT user_id FROM staff_payroll_adjustments WHERE month = :month
                        UNION SELECT sales_staff_id FROM bar_commission_calculations
                               WHERE to_char(created_at, 'YYYY-MM') = :month AND sales_staff_id IS NOT NULL) u
                  LEFT JOIN staff_base_salaries b ON b.user_id = u.user_id
                  LEFT JOIN (SELECT user_id, SUM(amount_cent) AS total FROM staff_commission_ledger
                              WHERE month = :month GROUP BY user_id) c ON c.user_id = u.user_id
                  LEFT JOIN (SELECT sales_staff_id, SUM(commission_amount_cent) AS total
                               FROM bar_commission_calculations
                              WHERE to_char(created_at, 'YYYY-MM') = :month
                                AND status NOT IN ('void', 'cancelled')
                              GROUP BY sales_staff_id) bc ON bc.sales_staff_id = u.user_id
                  LEFT JOIN (SELECT user_id,
                                    SUM(CASE WHEN adj_type = 'bonus' THEN amount_cent ELSE 0 END) AS bonus,
                                    SUM(CASE WHEN adj_type = 'penalty' THEN amount_cent ELSE 0 END) AS penalty
                               FROM staff_payroll_adjustments WHERE month = :month GROUP BY user_id) adj
                         ON adj.user_id = u.user_id
                  LEFT JOIN (SELECT user_id,
                                    CASE
                                      WHEN svc <= 0 THEN 0
                                      WHEN svc <= 400000 THEN GREATEST(0, (svc - 80000) * 20 / 100)
                                      ELSE CASE
                                        WHEN svc * 8 / 10 <= 2000000 THEN svc * 8 / 10 * 20 / 100
                                        WHEN svc * 8 / 10 <= 5000000 THEN svc * 8 / 10 * 30 / 100 - 200000
                                        ELSE svc * 8 / 10 * 40 / 100 - 700000
                                      END
                                    END AS tax_cent
                               FROM (SELECT user_id, SUM(amount_cent) AS svc FROM staff_commission_ledger
                                      WHERE month = :month AND source_type = 'service_order'
                                      GROUP BY user_id) svc_base) tax
                         ON tax.user_id = u.user_id
                ON CONFLICT (user_id, month) DO UPDATE SET
                    base_cent = EXCLUDED.base_cent,
                    commission_cent = EXCLUDED.commission_cent,
                    bonus_cent = EXCLUDED.bonus_cent,
                    penalty_cent = EXCLUDED.penalty_cent,
                    tax_cent = EXCLUDED.tax_cent,
                    total_cent = EXCLUDED.total_cent,
                    closed_by = EXCLUDED.closed_by,
                    closed_at = EXCLUDED.closed_at
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("month", month).addValue("closed_by", closedBy)
                .addValue("now", java.sql.Timestamp.from(now)));
    }

    @Override
    public java.util.List<StaffPayrollStatementRecord> listPayrollStatements(String month, String userId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, month, base_cent, commission_cent, bonus_cent, penalty_cent,
                       tax_cent, total_cent, closed_by, closed_at
                  FROM staff_payroll_statements
                 WHERE month = :month AND (:user_id::text IS NULL OR user_id = :user_id)
                 ORDER BY user_id
                 LIMIT 1000
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("month", month).addValue("user_id", userId),
                (rs, n) -> new StaffPayrollStatementRecord(
                        rs.getString("id"), rs.getString("user_id"), rs.getString("month"),
                        rs.getLong("base_cent"), rs.getLong("commission_cent"), rs.getLong("bonus_cent"),
                        rs.getLong("penalty_cent"), rs.getLong("tax_cent"), rs.getLong("total_cent"),
                        rs.getString("closed_by"), rs.getTimestamp("closed_at").toInstant()));
    }

    @Override
    public StaffLeaveRequestRecord saveLeaveRequest(StaffLeaveRequestRecord record) {
        jdbcTemplate.update("""
                INSERT INTO staff_leave_requests (id, user_id, leave_type, start_date, end_date, reason,
                        status, reviewed_by, review_note, created_at, updated_at)
                VALUES (:id, :user_id, :leave_type, :start_date, :end_date, :reason,
                        :status, :reviewed_by, :review_note, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    reviewed_by = EXCLUDED.reviewed_by,
                    review_note = EXCLUDED.review_note,
                    updated_at = EXCLUDED.updated_at
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", record.id()).addValue("user_id", record.userId())
                .addValue("leave_type", record.leaveType()).addValue("start_date", record.startDate())
                .addValue("end_date", record.endDate()).addValue("reason", record.reason())
                .addValue("status", record.status()).addValue("reviewed_by", record.reviewedBy())
                .addValue("review_note", record.reviewNote())
                .addValue("created_at", java.sql.Timestamp.from(record.createdAt()))
                .addValue("updated_at", java.sql.Timestamp.from(record.updatedAt())));
        return record;
    }

    @Override
    public java.util.Optional<StaffLeaveRequestRecord> findLeaveById(String id) {
        return jdbcTemplate.query("""
                SELECT id, user_id, leave_type, start_date, end_date, reason, status,
                       reviewed_by, review_note, created_at, updated_at
                  FROM staff_leave_requests WHERE id = :id
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource().addValue("id", id),
                this::mapLeave).stream().findFirst();
    }

    @Override
    public java.util.List<StaffLeaveRequestRecord> listLeaves(String status, String userId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, leave_type, start_date, end_date, reason, status,
                       reviewed_by, review_note, created_at, updated_at
                  FROM staff_leave_requests
                 WHERE (:status::text IS NULL OR status = :status)
                   AND (:user_id::text IS NULL OR user_id = :user_id)
                 ORDER BY created_at DESC
                 LIMIT 500
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("status", status).addValue("user_id", userId), this::mapLeave);
    }

    private StaffLeaveRequestRecord mapLeave(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new StaffLeaveRequestRecord(
                rs.getString("id"), rs.getString("user_id"), rs.getString("leave_type"),
                rs.getObject("start_date", java.time.LocalDate.class),
                rs.getObject("end_date", java.time.LocalDate.class),
                rs.getString("reason"), rs.getString("status"), rs.getString("reviewed_by"),
                rs.getString("review_note"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    @Override
    public java.util.Set<java.time.LocalDate> approvedLeaveDates(String userId,
            java.time.LocalDate from, java.time.LocalDate to) {
        java.util.Set<java.time.LocalDate> out = new java.util.LinkedHashSet<>();
        jdbcTemplate.query("""
                SELECT start_date, end_date FROM staff_leave_requests
                 WHERE user_id = :user_id AND status = 'approved'
                   AND start_date <= :to AND end_date >= :from
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("from", from).addValue("to", to), rs -> {
            java.time.LocalDate start = rs.getObject("start_date", java.time.LocalDate.class);
            java.time.LocalDate end = rs.getObject("end_date", java.time.LocalDate.class);
            for (java.time.LocalDate day = start.isBefore(from) ? from : start;
                    !day.isAfter(end.isAfter(to) ? to : end); day = day.plusDays(1)) {
                out.add(day);
            }
        });
        return out;
    }

    @Override
    public java.util.List<StaffAttendanceRecord> listAttendanceForUserBetween(String userId,
            java.time.Instant from, java.time.Instant to) {
        return jdbcTemplate.query("""
                SELECT id, user_id, punch_type, punched_at, method, tag_ref, space_id,
                       device_fingerprint, shift_id, judgement, created_at
                  FROM staff_attendance_records
                 WHERE user_id = :user_id AND punched_at >= :from AND punched_at < :to
                 ORDER BY punched_at
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId)
                .addValue("from", java.sql.Timestamp.from(from))
                .addValue("to", java.sql.Timestamp.from(to)), this::mapAttendance);
    }

    // ---------------------------------------------------------------- E1/E2 排班与考勤

    @Override
    public StaffShiftRecord upsertShift(StaffShiftRecord shift) {
        jdbcTemplate.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at,
                        point_space_ids, note, created_by, created_at, updated_at)
                VALUES (:id, :user_id, :work_date, :start_at, :end_at, :points::jsonb, :note, :created_by, :now, :now)
                ON CONFLICT (user_id, start_at) DO UPDATE SET
                    work_date = EXCLUDED.work_date,
                    end_at = EXCLUDED.end_at,
                    point_space_ids = EXCLUDED.point_space_ids,
                    note = EXCLUDED.note,
                    updated_at = EXCLUDED.updated_at
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", shift.id())
                .addValue("user_id", shift.userId())
                .addValue("work_date", shift.workDate())
                .addValue("start_at", java.sql.Timestamp.from(shift.startAt()))
                .addValue("end_at", java.sql.Timestamp.from(shift.endAt()))
                .addValue("points", pointsJson(shift.pointSpaceIds()))
                .addValue("note", shift.note())
                .addValue("created_by", shift.createdBy())
                .addValue("now", java.sql.Timestamp.from(shift.updatedAt())));
        return listShiftsForUserBetween(shift.userId(), shift.workDate(), shift.workDate()).stream()
                .filter(row -> row.startAt().equals(shift.startAt()))
                .findFirst().orElse(shift);
    }

    @Override
    public java.util.List<StaffShiftRecord> listShifts(java.time.LocalDate workDate, String userId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, work_date, start_at, end_at, point_space_ids, note, created_by, created_at, updated_at
                  FROM staff_shift_schedules
                 WHERE (:work_date::date IS NULL OR work_date = :work_date)
                   AND (:user_id::text IS NULL OR user_id = :user_id)
                 ORDER BY work_date DESC, start_at, user_id
                 LIMIT 500
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("work_date", workDate).addValue("user_id", userId), this::mapShift);
    }

    @Override
    public java.util.List<StaffShiftRecord> listShiftsForUserBetween(String userId,
            java.time.LocalDate from, java.time.LocalDate to) {
        return jdbcTemplate.query("""
                SELECT id, user_id, work_date, start_at, end_at, point_space_ids, note, created_by, created_at, updated_at
                  FROM staff_shift_schedules
                 WHERE user_id = :user_id AND work_date BETWEEN :from AND :to
                 ORDER BY start_at
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("from", from).addValue("to", to), this::mapShift);
    }

    private StaffShiftRecord mapShift(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new StaffShiftRecord(
                rs.getString("id"), rs.getString("user_id"),
                rs.getObject("work_date", java.time.LocalDate.class),
                rs.getTimestamp("start_at").toInstant(), rs.getTimestamp("end_at").toInstant(),
                parsePoints(rs.getString("point_space_ids")), rs.getString("note"), rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper POINTS_JSON =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private String pointsJson(java.util.List<String> points) {
        try {
            return POINTS_JSON.writeValueAsString(points == null ? java.util.List.of() : points);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("point_space_ids serialization failed", e);
        }
    }

    private java.util.List<String> parsePoints(String json) {
        try {
            if (json == null || json.isBlank()) {
                return java.util.List.of();
            }
            return POINTS_JSON.readValue(json,
                    POINTS_JSON.getTypeFactory().constructCollectionType(java.util.List.class, String.class));
        } catch (java.io.IOException e) {
            return java.util.List.of();
        }
    }

    @Override
    public StaffAttendanceRecord saveAttendance(StaffAttendanceRecord record) {
        jdbcTemplate.update("""
                INSERT INTO staff_attendance_records (id, user_id, punch_type, punched_at, method,
                        tag_ref, space_id, device_fingerprint, shift_id, judgement, created_at)
                VALUES (:id, :user_id, :punch_type, :punched_at, :method,
                        :tag_ref, :space_id, :device_fingerprint, :shift_id, :judgement, :created_at)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("user_id", record.userId())
                .addValue("punch_type", record.punchType())
                .addValue("punched_at", java.sql.Timestamp.from(record.punchedAt()))
                .addValue("method", record.method())
                .addValue("tag_ref", record.tagRef())
                .addValue("space_id", record.spaceId())
                .addValue("device_fingerprint", record.deviceFingerprint())
                .addValue("shift_id", record.shiftId())
                .addValue("judgement", record.judgement())
                .addValue("created_at", java.sql.Timestamp.from(record.createdAt())));
        return record;
    }

    @Override
    public java.util.List<StaffAttendanceRecord> listAttendanceForUser(String userId, java.time.Instant since) {
        return jdbcTemplate.query("""
                SELECT id, user_id, punch_type, punched_at, method, tag_ref, space_id,
                       device_fingerprint, shift_id, judgement, created_at
                  FROM staff_attendance_records
                 WHERE user_id = :user_id AND punched_at >= :since
                 ORDER BY punched_at DESC
                 LIMIT 500
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("since", java.sql.Timestamp.from(since)), this::mapAttendance);
    }

    @Override
    public java.util.List<StaffAttendanceRecord> listAttendanceOn(java.time.LocalDate date) {
        return jdbcTemplate.query("""
                SELECT id, user_id, punch_type, punched_at, method, tag_ref, space_id,
                       device_fingerprint, shift_id, judgement, created_at
                  FROM staff_attendance_records
                 WHERE punched_at >= :day_start AND punched_at < :day_end
                 ORDER BY punched_at DESC
                 LIMIT 1000
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("day_start", java.sql.Timestamp.from(date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)))
                .addValue("day_end", java.sql.Timestamp.from(date.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC))),
                this::mapAttendance);
    }

    private StaffAttendanceRecord mapAttendance(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new StaffAttendanceRecord(
                rs.getString("id"), rs.getString("user_id"), rs.getString("punch_type"),
                rs.getTimestamp("punched_at").toInstant(), rs.getString("method"), rs.getString("tag_ref"),
                rs.getString("space_id"), rs.getString("device_fingerprint"), rs.getString("shift_id"),
                rs.getString("judgement"), rs.getTimestamp("created_at").toInstant());
    }

    // ---------------------------------------------------------------- staff_profiles

    @Override
    public StaffProfileRecord saveProfile(StaffProfileRecord profile) {
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (
                    id, project_id, merchant_id, user_id, name, phone_hash, id_doc_hash, staff_level_id,
                    registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :merchant_id, :user_id, :name, :phone_hash, :id_doc_hash, :staff_level_id,
                    :registration_status, :approved_by, :approved_at, :reject_reason, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    merchant_id = EXCLUDED.merchant_id,
                    user_id = EXCLUDED.user_id,
                    name = EXCLUDED.name,
                    phone_hash = EXCLUDED.phone_hash,
                    id_doc_hash = EXCLUDED.id_doc_hash,
                    staff_level_id = EXCLUDED.staff_level_id,
                    registration_status = EXCLUDED.registration_status,
                    approved_by = EXCLUDED.approved_by,
                    approved_at = EXCLUDED.approved_at,
                    reject_reason = EXCLUDED.reject_reason,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", profile.id())
                .addValue("project_id", profile.projectId())
                .addValue("merchant_id", profile.merchantId())
                .addValue("user_id", profile.userId())
                .addValue("name", profile.name())
                .addValue("phone_hash", profile.phoneHash())
                .addValue("id_doc_hash", profile.idDocHash())
                .addValue("staff_level_id", profile.staffLevelId())
                .addValue("registration_status", profile.registrationStatus())
                .addValue("approved_by", profile.approvedBy())
                .addValue("approved_at", timestampWithTimeZone(profile.approvedAt()))
                .addValue("reject_reason", profile.rejectReason())
                .addValue("status", profile.status())
                .addValue("created_at", timestampWithTimeZone(profile.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(profile.updatedAt())));
        return profile;
    }

    @Override
    public Optional<StaffProfileRecord> findProfileById(String id) {
        List<StaffProfileRecord> result = jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, user_id, name, phone_hash, id_doc_hash, staff_level_id,
                       registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at
                  FROM staff_profiles
                 WHERE id = :id
                """, Map.of("id", id), this::mapProfile);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<StaffProfileRecord> listProfiles(String merchantScope) {
        if (merchantScope == null) {
            return jdbcTemplate.query("""
                    SELECT id, project_id, merchant_id, user_id, name, phone_hash, id_doc_hash, staff_level_id,
                           registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at
                      FROM staff_profiles
                     ORDER BY created_at DESC, id
                    """, this::mapProfile);
        }
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, user_id, name, phone_hash, id_doc_hash, staff_level_id,
                       registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at
                  FROM staff_profiles
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope), this::mapProfile);
    }

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<StaffProfileRecord> listProfilesByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, user_id, name, phone_hash, id_doc_hash, staff_level_id,
                       registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at
                  FROM staff_profiles
                 WHERE project_id = :project_id
                 ORDER BY created_at DESC, id
                """, Map.of("project_id", projectId), this::mapProfile);
    }

    // ---------------------------------------------------------- staff_device_credentials

    @Override
    public StaffDeviceCredentialRecord saveCredential(StaffDeviceCredentialRecord credential) {
        jdbcTemplate.update("""
                INSERT INTO staff_device_credentials (
                    id, staff_profile_id, device_public_key, device_fingerprint_hash, bind_status,
                    supersedes_credential_id, integrity_status, revoked_reason, bound_at, revoked_at,
                    created_at, updated_at
                )
                VALUES (
                    :id, :staff_profile_id, :device_public_key, :device_fingerprint_hash, :bind_status,
                    :supersedes_credential_id, :integrity_status, :revoked_reason, :bound_at, :revoked_at,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    device_public_key = EXCLUDED.device_public_key,
                    device_fingerprint_hash = EXCLUDED.device_fingerprint_hash,
                    bind_status = EXCLUDED.bind_status,
                    supersedes_credential_id = EXCLUDED.supersedes_credential_id,
                    integrity_status = EXCLUDED.integrity_status,
                    revoked_reason = EXCLUDED.revoked_reason,
                    bound_at = EXCLUDED.bound_at,
                    revoked_at = EXCLUDED.revoked_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", credential.id())
                .addValue("staff_profile_id", credential.staffProfileId())
                .addValue("device_public_key", credential.devicePublicKey())
                .addValue("device_fingerprint_hash", credential.deviceFingerprintHash())
                .addValue("bind_status", credential.bindStatus())
                .addValue("supersedes_credential_id", credential.supersedesCredentialId())
                .addValue("integrity_status", credential.integrityStatus())
                .addValue("revoked_reason", credential.revokedReason())
                .addValue("bound_at", timestampWithTimeZone(credential.boundAt()))
                .addValue("revoked_at", timestampWithTimeZone(credential.revokedAt()))
                .addValue("created_at", timestampWithTimeZone(credential.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(credential.updatedAt())));
        return credential;
    }

    @Override
    public Optional<StaffDeviceCredentialRecord> findCredentialById(String id) {
        List<StaffDeviceCredentialRecord> result = jdbcTemplate.query(
                credentialSelect() + " WHERE id = :id",
                Map.of("id", id), this::mapCredential);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public Optional<StaffDeviceCredentialRecord> findActiveCredential(String staffProfileId) {
        List<StaffDeviceCredentialRecord> result = jdbcTemplate.query(
                credentialSelect() + " WHERE staff_profile_id = :staff_profile_id AND bind_status = 'active'",
                Map.of("staff_profile_id", staffProfileId), this::mapCredential);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<StaffDeviceCredentialRecord> listCredentials(String staffProfileId) {
        return jdbcTemplate.query(
                credentialSelect() + " WHERE staff_profile_id = :staff_profile_id ORDER BY created_at DESC, id",
                Map.of("staff_profile_id", staffProfileId), this::mapCredential);
    }

    private String credentialSelect() {
        return """
                SELECT id, staff_profile_id, device_public_key, device_fingerprint_hash, bind_status,
                       supersedes_credential_id, integrity_status, revoked_reason, bound_at, revoked_at,
                       created_at, updated_at
                  FROM staff_device_credentials
                """;
    }

    // ---------------------------------------------------------------- employee_notices

    @Override
    public EmployeeNoticeRecord saveNotice(EmployeeNoticeRecord notice) {
        jdbcTemplate.update("""
                INSERT INTO employee_notices (
                    id, staff_profile_id, notice_type, title, body, consent_required, consent_status,
                    consented_at, created_by, created_at, updated_at
                )
                VALUES (
                    :id, :staff_profile_id, :notice_type, :title, :body, :consent_required, :consent_status,
                    :consented_at, :created_by, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    notice_type = EXCLUDED.notice_type,
                    title = EXCLUDED.title,
                    body = EXCLUDED.body,
                    consent_required = EXCLUDED.consent_required,
                    consent_status = EXCLUDED.consent_status,
                    consented_at = EXCLUDED.consented_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", notice.id())
                .addValue("staff_profile_id", notice.staffProfileId())
                .addValue("notice_type", notice.noticeType())
                .addValue("title", notice.title())
                .addValue("body", notice.body())
                .addValue("consent_required", notice.consentRequired())
                .addValue("consent_status", notice.consentStatus())
                .addValue("consented_at", timestampWithTimeZone(notice.consentedAt()))
                .addValue("created_by", notice.createdBy())
                .addValue("created_at", timestampWithTimeZone(notice.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(notice.updatedAt())));
        return notice;
    }

    @Override
    public Optional<EmployeeNoticeRecord> findNoticeById(String id) {
        List<EmployeeNoticeRecord> result = jdbcTemplate.query("""
                SELECT id, staff_profile_id, notice_type, title, body, consent_required, consent_status,
                       consented_at, created_by, created_at, updated_at
                  FROM employee_notices
                 WHERE id = :id
                """, Map.of("id", id), this::mapNotice);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<EmployeeNoticeRecord> listNotices(String merchantScope) {
        if (merchantScope == null) {
            return jdbcTemplate.query("""
                    SELECT id, staff_profile_id, notice_type, title, body, consent_required, consent_status,
                           consented_at, created_by, created_at, updated_at
                      FROM employee_notices
                     ORDER BY created_at DESC, id
                    """, this::mapNotice);
        }
        // 隔离：仅返回所属员工 merchant_id=该值的通知（跨商户不可见）。
        return jdbcTemplate.query("""
                SELECT n.id, n.staff_profile_id, n.notice_type, n.title, n.body, n.consent_required,
                       n.consent_status, n.consented_at, n.created_by, n.created_at, n.updated_at
                  FROM employee_notices n
                  JOIN staff_profiles p ON p.id = n.staff_profile_id
                 WHERE p.merchant_id = :merchant_id
                 ORDER BY n.created_at DESC, n.id
                """, Map.of("merchant_id", merchantScope), this::mapNotice);
    }

    // ---------------------------------------------------------------- row mappers

    private StaffProfileRecord mapProfile(ResultSet rs, int rowNumber) throws SQLException {
        return new StaffProfileRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("merchant_id"),
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getString("phone_hash"),
                rs.getString("id_doc_hash"),
                rs.getString("staff_level_id"),
                rs.getString("registration_status"),
                rs.getString("approved_by"),
                nullableInstant(rs, "approved_at"),
                rs.getString("reject_reason"),
                rs.getString("status"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at")
        );
    }

    private StaffDeviceCredentialRecord mapCredential(ResultSet rs, int rowNumber) throws SQLException {
        return new StaffDeviceCredentialRecord(
                rs.getString("id"),
                rs.getString("staff_profile_id"),
                rs.getString("device_public_key"),
                rs.getString("device_fingerprint_hash"),
                rs.getString("bind_status"),
                rs.getString("supersedes_credential_id"),
                rs.getString("integrity_status"),
                rs.getString("revoked_reason"),
                nullableInstant(rs, "bound_at"),
                nullableInstant(rs, "revoked_at"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at")
        );
    }

    private EmployeeNoticeRecord mapNotice(ResultSet rs, int rowNumber) throws SQLException {
        return new EmployeeNoticeRecord(
                rs.getString("id"),
                rs.getString("staff_profile_id"),
                rs.getString("notice_type"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getBoolean("consent_required"),
                rs.getString("consent_status"),
                nullableInstant(rs, "consented_at"),
                rs.getString("created_by"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at")
        );
    }
}
