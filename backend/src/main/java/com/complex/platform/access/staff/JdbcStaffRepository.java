package com.complex.platform.access.staff;

import com.complex.platform.common.api.ApiException;

// 文件版本：V0.24
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.LocalZone;
import java.time.Instant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// ponytail: 仓储直用实现类,无接口(one impl)。
@Repository
public class JdbcStaffRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final com.complex.platform.common.crypto.PiiCipher piiCipher;

    // 第十八批第 1 条（ENUM:187）：销售提成归属月按开台或下单时点所属营业日（BusinessDayResolver#barCommissionIdsInMonth）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.common.BusinessDayResolver businessDays = new com.complex.platform.common.BusinessDayResolver();

    JdbcStaffRepository(NamedParameterJdbcTemplate jdbcTemplate,
            com.complex.platform.common.crypto.PiiCipher piiCipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.piiCipher = piiCipher;
    }

    // P0 租户隔离(V214):员工行数据按 project 归属——写入时由该员工最新档案推导,与迁移回填同口径。
    private static final String STAFF_PROJECT_SUBSELECT =
            "(SELECT p.project_id FROM staff_profiles p WHERE p.user_id = :user_id"
                    + " ORDER BY p.created_at DESC LIMIT 1)";

    // ---------------------------------------------------------------- F批 薪资/请假

    public void savePenaltyCapRevision(String reviewId, StaffNumericSetting before, StaffNumericSetting after, String actor) {
        jdbcTemplate.update("""
                INSERT INTO staff_penalty_cap_revisions(review_id,before_value,numeric_value,min_value,max_value,initiated_by)
                VALUES(:review,:before,:value,:min,:max,:actor)
                """,Map.of("review",reviewId,"before",before.value(),"value",after.value(),
                "min",after.minValue(),"max",after.maxValue(),"actor",actor));
    }

    public void applyPenaltyCapRevision(String reviewId) {
        var rows = jdbcTemplate.queryForList("SELECT * FROM staff_penalty_cap_revisions WHERE review_id=:review AND applied_at IS NULL FOR UPDATE",
                Map.of("review",reviewId));
        if (rows.isEmpty()) return;
        var row = rows.getFirst();
        // 固定全局参数行作为并发锁，审批期间已有另一份变更生效时须重新申请。
        int current = jdbcTemplate.queryForObject("SELECT numeric_value FROM staff_policy_settings WHERE policy_key='payroll_penalty_cap_bps' AND scope_id='global' FOR UPDATE",Map.of(),Integer.class);
        if (current != ((Number)row.get("before_value")).intValue()) {
            throw com.complex.platform.common.api.ApiException.conflict("POLICY_CHANGED", "扣除上限已变更，请核对后重新申请");
        }
        saveNumericSetting("payroll_penalty_cap_bps","global",new StaffNumericSetting(((Number)row.get("numeric_value")).intValue(),
                ((Number)row.get("min_value")).intValue(),((Number)row.get("max_value")).intValue()),row.get("initiated_by").toString());
        jdbcTemplate.update("UPDATE staff_penalty_cap_revisions SET applied_at=now() WHERE review_id=:review",Map.of("review",reviewId));
    }

    /** U05-005：经营主体行缺失时回落到 scope_id='global' 出厂行（值与上下限同在）；两者都缺则 fail-closed，不返回空上下限。 */
    public StaffNumericSetting numericSetting(String key, String scope) {
        return jdbcTemplate.query("""
                SELECT numeric_value, min_value, max_value FROM staff_policy_settings
                 WHERE policy_key=:key AND scope_id IN (:scope, 'global')
                 ORDER BY CASE WHEN scope_id=:scope THEN 0 ELSE 1 END
                 LIMIT 1
                """, Map.of("key", key, "scope", scope),
                (rs, n) -> new StaffNumericSetting(rs.getInt(1), rs.getInt(2), rs.getInt(3)))
                .stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("POLICY_NOT_CONFIGURED", "参数 " + key + " 未配置出厂行（含上下限），不得上线使用"));
    }

    public void saveNumericSetting(String key, String scope, StaffNumericSetting value, String actorId) {
        jdbcTemplate.update("""
                INSERT INTO staff_policy_settings(policy_key, scope_id, numeric_value, min_value, max_value, updated_by)
                VALUES(:key, :scope, :value, :min, :max, :actor)
                ON CONFLICT(policy_key, scope_id) DO UPDATE SET numeric_value=EXCLUDED.numeric_value,
                    min_value=EXCLUDED.min_value, max_value=EXCLUDED.max_value, updated_by=EXCLUDED.updated_by, updated_at=now()
                """, Map.of("key", key, "scope", scope, "value", value.value(), "min", value.minValue(),
                "max", value.maxValue(), "actor", actorId));
    }

    public void upsertBaseSalary(String userId, long monthlySalaryCent, String updatedBy, java.time.Instant now) {
        jdbcTemplate.update("""
                INSERT INTO staff_base_salaries (user_id, monthly_salary_cent, updated_by, updated_at, project_id)
                VALUES (:user_id, :salary, :updated_by, :now, """ + STAFF_PROJECT_SUBSELECT + """
                )
                ON CONFLICT (user_id) DO UPDATE SET
                    monthly_salary_cent = EXCLUDED.monthly_salary_cent,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("salary", monthlySalaryCent)
                .addValue("updated_by", updatedBy).addValue("now", java.sql.Timestamp.from(now)));
    }

    public void insertPayrollAdjustment(String id, String userId, String month, String adjType,
            long amountCent, String reason, String createdBy, java.time.Instant now) {
        insertPayrollAdjustment(id, userId, month, adjType, amountCent, reason, createdBy, now, null);
    }

    /** CG-12：reversalOf 非空即「申诉撤销冲回」记录（adj_type = penalty_reversal，负向计入处罚合计；原记录保留）。 */
    public void insertPayrollAdjustment(String id, String userId, String month, String adjType,
            long amountCent, String reason, String createdBy, java.time.Instant now, String reversalOf) {
        jdbcTemplate.update("""
                INSERT INTO staff_payroll_adjustments (id, user_id, month, adj_type, amount_cent, reason, created_by, created_at, project_id, reversal_of)
                VALUES (:id, :user_id, :month, :adj_type, :amount, :reason, :created_by, :now, """
                + STAFF_PROJECT_SUBSELECT + """
                , :reversal_of)
                ON CONFLICT DO NOTHING
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("user_id", userId).addValue("month", month)
                .addValue("adj_type", adjType).addValue("amount", amountCent).addValue("reason", reason)
                .addValue("created_by", createdBy).addValue("now", java.sql.Timestamp.from(now)).addValue("reversal_of", reversalOf));
    }

    // ── CG-12 人力操作复核链（BSM:1478 / BSM:1487）：员工状态变更、奖惩、申诉 ──

    /** 已入账奖惩记录（申诉对象）。 */
    public record AdjustmentRow(String id, String userId, String month, String adjType, long amountCent, String reason) {
    }

    public Optional<AdjustmentRow> findAdjustment(String id) {
        return jdbcTemplate.query("SELECT id, user_id, month, adj_type, amount_cent, reason FROM staff_payroll_adjustments WHERE id = :id",
                Map.of("id", id), (rs, i) -> new AdjustmentRow(rs.getString("id"), rs.getString("user_id"), rs.getString("month"),
                        rs.getString("adj_type"), rs.getLong("amount_cent"), rs.getString("reason"))).stream().findFirst();
    }

    public void saveEmploymentStatusRevision(String reviewId, String profileId, String userId, String before, String after,
            String reason, String actor, Instant effectiveAt) {
        jdbcTemplate.update("""
                INSERT INTO staff_employment_status_revisions
                    (review_id, profile_id, user_id, before_status, after_status, reason, initiated_by, effective_at)
                VALUES (:review, :profile, :user_id, :before, :after, :reason, :actor, :effective_at)
                """, new MapSqlParameterSource().addValue("review", reviewId).addValue("profile", profileId).addValue("user_id", userId)
                .addValue("before", before).addValue("after", after).addValue("reason", reason).addValue("actor", actor)
                .addValue("effective_at", timestampWithTimeZone(effectiveAt)));
    }

    public void lockEmploymentStatusSubmission(String profileId) {
        jdbcTemplate.queryForList("SELECT id FROM staff_profiles WHERE id=:id FOR UPDATE", Map.of("id", profileId));
        if (!jdbcTemplate.queryForList("""
                SELECT r.review_id FROM staff_employment_status_revisions r
                JOIN supervisor_review_decisions d ON d.review_id=r.review_id
                WHERE r.profile_id=:id AND r.applied_at IS NULL AND d.review_status IN ('pending_review','approved')
                """, Map.of("id", profileId)).isEmpty()) {
            throw com.complex.platform.common.api.ApiException.conflict("EMPLOYMENT_STATUS_PENDING", "该员工已有待审批或待生效的状态变更");
        }
    }

    public List<String> dueEmploymentStatusReviews(Instant checkedAt) {
        return jdbcTemplate.queryForList("""
                SELECT r.review_id FROM staff_employment_status_revisions r
                JOIN supervisor_review_decisions d ON d.review_id=r.review_id
                WHERE r.applied_at IS NULL AND r.effective_at<=:at AND d.review_status='approved'
                ORDER BY r.effective_at,r.review_id
                """, Map.of("at", timestampWithTimeZone(checkedAt)), String.class);
    }

    public Optional<Map<String, Object>> lockPendingEmploymentStatusRevision(String reviewId) {
        return jdbcTemplate.queryForList("""
                SELECT r.* FROM staff_employment_status_revisions r
                JOIN supervisor_review_decisions d ON d.review_id=r.review_id
                WHERE r.review_id=:review AND r.applied_at IS NULL AND d.review_status='approved' FOR UPDATE OF r
                """,
                Map.of("review", reviewId)).stream().findFirst();
    }

    public void markEmploymentStatusRevisionApplied(String reviewId) {
        jdbcTemplate.update("UPDATE staff_employment_status_revisions SET applied_at = now() WHERE review_id = :review", Map.of("review", reviewId));
    }

    public void saveAdjustmentRequest(String id, String reviewId, String userId, String month, String adjType, long amountCent,
            String reason, String actor) {
        jdbcTemplate.update("""
                INSERT INTO staff_payroll_adjustment_requests (id, review_id, user_id, month, adj_type, amount_cent, reason, initiated_by)
                VALUES (:id, :review, :user_id, :month, :adj_type, :amount, :reason, :actor)
                """, new MapSqlParameterSource().addValue("id", id).addValue("review", reviewId).addValue("user_id", userId)
                .addValue("month", month).addValue("adj_type", adjType).addValue("amount", amountCent).addValue("reason", reason).addValue("actor", actor));
    }

    public Optional<Map<String, Object>> lockPendingAdjustmentRequest(String reviewId) {
        return jdbcTemplate.queryForList("SELECT * FROM staff_payroll_adjustment_requests WHERE review_id = :review AND applied_at IS NULL FOR UPDATE",
                Map.of("review", reviewId)).stream().findFirst();
    }

    public void markAdjustmentRequestApplied(String reviewId) {
        jdbcTemplate.update("UPDATE staff_payroll_adjustment_requests SET applied_at = now() WHERE review_id = :review", Map.of("review", reviewId));
    }

    public void saveAppeal(StaffAdjustmentAppealRecord appeal) {
        jdbcTemplate.update("""
                INSERT INTO staff_payroll_adjustment_appeals (id, adjustment_id, user_id, reason, review_id, status, initiated_by, created_at)
                VALUES (:id, :adjustment, :user_id, :reason, :review, :status, :actor, :now)
                """, new MapSqlParameterSource().addValue("id", appeal.id()).addValue("adjustment", appeal.adjustmentId())
                .addValue("user_id", appeal.userId()).addValue("reason", appeal.reason()).addValue("review", appeal.reviewId())
                .addValue("status", appeal.status()).addValue("actor", appeal.initiatedBy())
                .addValue("now", java.sql.Timestamp.from(appeal.createdAt())));
    }

    public boolean hasAppealInStatus(String adjustmentId, String status) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_payroll_adjustment_appeals WHERE adjustment_id = :adjustment AND status = :status",
                Map.of("adjustment", adjustmentId, "status", status), Integer.class);
        return count != null && count > 0;
    }

    public Optional<StaffAdjustmentAppealRecord> lockPendingAppealByReview(String reviewId) {
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(hashtextextended('staff-payroll:' || user_id,0)) FROM staff_payroll_adjustment_appeals WHERE review_id=:review",
                Map.of("review",reviewId), rs -> {});
        return jdbcTemplate.query(APPEAL_SELECT + " WHERE a.review_id = :review AND a.status = 'pending' FOR UPDATE OF a",
                Map.of("review", reviewId), this::mapAppeal).stream().findFirst();
    }

    /** 与工资定格/补差同一把员工锁（staff-payroll:<user>，事务级咨询锁）：销假判「已定格月份」前先取，并发定格与销假互不漏看。 */
    public void lockPayrollEmployee(String userId) {
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(hashtextextended('staff-payroll:' || :user,0))", Map.of("user", userId), rs -> {});
    }

    public void decideAppeal(String id, String status, String reversalAdjustmentId, java.time.Instant at) {
        jdbcTemplate.update("UPDATE staff_payroll_adjustment_appeals SET status = :status, reversal_adjustment_id = :reversal, decided_at = :at WHERE id = :id",
                new MapSqlParameterSource().addValue("status", status).addValue("reversal", reversalAdjustmentId)
                        .addValue("at", java.sql.Timestamp.from(at)).addValue("id", id));
    }

    public List<StaffAdjustmentAppealRecord> listAppeals(String userId) {
        return jdbcTemplate.query(APPEAL_SELECT + " WHERE a.user_id = :user_id ORDER BY a.created_at DESC", Map.of("user_id", userId), this::mapAppeal);
    }

    private static final String APPEAL_SELECT = """
            SELECT a.id, a.adjustment_id, a.user_id, j.month, j.adj_type, j.amount_cent, a.reason, a.review_id, a.status,
                   a.reversal_adjustment_id, a.initiated_by, a.created_at, a.decided_at
              FROM staff_payroll_adjustment_appeals a
              JOIN staff_payroll_adjustments j ON j.id = a.adjustment_id
            """;

    private StaffAdjustmentAppealRecord mapAppeal(ResultSet rs, int rowNumber) throws SQLException {
        return new StaffAdjustmentAppealRecord(rs.getString("id"), rs.getString("adjustment_id"), rs.getString("user_id"),
                rs.getString("month"), rs.getString("adj_type"), rs.getLong("amount_cent"), rs.getString("reason"), rs.getString("review_id"),
                rs.getString("status"), rs.getString("reversal_adjustment_id"), rs.getString("initiated_by"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "decided_at"));
    }

    public void insertCommission(String id, String userId, String month, String sourceType,
            String sourceId, long amountCent, String note, java.time.Instant now) {
        jdbcTemplate.update("""
                INSERT INTO staff_commission_ledger (id, user_id, month, source_type, source_id, amount_cent, note, created_at, project_id)
                VALUES (:id, :user_id, :month, :source_type, :source_id, :amount, :note, :now, """
                + STAFF_PROJECT_SUBSELECT + """
                )
                -- 无目标 DO NOTHING:同时覆盖 V215(service_order 部分唯一)与 V239(manual 业务级唯一)两索引。
                ON CONFLICT DO NOTHING
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("user_id", userId).addValue("month", month)
                .addValue("source_type", sourceType).addValue("source_id", sourceId)
                .addValue("amount", amountCent).addValue("note", note)
                .addValue("now", java.sql.Timestamp.from(now)));
    }

    public java.util.Optional<String> findUserProjectId(String userId) {
        return jdbcTemplate.query(
                "SELECT project_id FROM staff_profiles WHERE user_id = :user_id ORDER BY created_at DESC LIMIT 1",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource().addValue("user_id", userId),
                (rs, n) -> rs.getString("project_id")).stream().filter(java.util.Objects::nonNull).findFirst();
    }

    /** 18E 复核后操作员裁决补充第 16 条：员工状态变更、奖惩、排班发布复核单所处理员工的登记项目（staff_profiles.project_id），每名员工一项
     *  （排班批次多名员工多项；按员工编号取最新一条档案，同 STAFF_PROJECT_SUBSELECT；没有登记项目的为 null）。 */
    public List<String> reviewSubjectProjectIds(String reviewId) {
        return jdbcTemplate.queryForList("""
                SELECT p.project_id FROM staff_employment_status_revisions r JOIN staff_profiles p ON p.id = r.profile_id
                 WHERE r.review_id = :review
                UNION ALL
                SELECT (SELECT p.project_id FROM staff_profiles p WHERE p.user_id = u.user_id ORDER BY p.created_at DESC LIMIT 1)
                  FROM (SELECT user_id FROM staff_payroll_adjustment_requests WHERE review_id = :review
                        UNION SELECT after_value->>'userId' FROM staff_shift_revisions WHERE review_id = :review) u
                """, Map.of("review", reviewId), String.class);
    }

    public int closePayrollMonth(String month, String closedBy, java.time.Instant now, String projectScope) {
        return closePayrollMonth(month, closedBy, now, projectScope, null);
    }

    public int closePayrollMonth(String month, String closedBy, java.time.Instant now, String projectScope,
            String merchantScope) {
        return closePayrollMonth(month, closedBy, now, projectScope, merchantScope, null);
    }

    public int closePayrollMonth(String month, String closedBy, java.time.Instant now, String projectScope,
            String merchantScope, String companyScope) {
        throw com.complex.platform.common.api.ApiException.conflict("PAYROLL_CONFIRMED_FACTS_REQUIRED",
                "工资须先锁定已批准方案、真实计薪事实和完整专业税额原件，再由工资定格服务形成应付");
    }

    public int markPayrollPaid(String payoutId, String userId, String month, String payMethod,
            String payRef, String paidBy, java.time.Instant now) {
        throw com.complex.platform.common.api.ApiException.conflict("PAYROLL_FINANCE_RESULT_REQUIRED",
                "人事不能标记工资已发；须由财务实际成功付款原件同步");
    }

    // ============================================================
    // CG-07 派驻记录 / 提成月结内部结算 / 三个付账日
    // ============================================================

    private static final String DISPATCH_SELECT = """
            SELECT id, user_id, company_id, merchant_id, project_id, start_date, end_date, status, note,
                   created_by, created_at, ended_by, ended_at
              FROM staff_dispatch_records
            """;

    public StaffDispatchRecord saveDispatch(StaffDispatchRecord r) {
        jdbcTemplate.update("""
                INSERT INTO staff_dispatch_records (id, user_id, company_id, merchant_id, project_id, start_date, end_date,
                        status, note, created_by, created_at, ended_by, ended_at)
                VALUES (:id, :user_id, :company_id, :merchant_id, :project_id, :start_date, :end_date,
                        :status, :note, :created_by, :created_at, :ended_by, :ended_at)
                ON CONFLICT (id) DO UPDATE SET end_date = EXCLUDED.end_date, status = EXCLUDED.status,
                        note = EXCLUDED.note, ended_by = EXCLUDED.ended_by, ended_at = EXCLUDED.ended_at
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", r.id()).addValue("user_id", r.userId()).addValue("company_id", r.companyId())
                .addValue("merchant_id", r.merchantId()).addValue("project_id", r.projectId())
                .addValue("start_date", java.sql.Date.valueOf(r.startDate()))
                .addValue("end_date", r.endDate() == null ? null : java.sql.Date.valueOf(r.endDate()))
                .addValue("status", r.status()).addValue("note", r.note()).addValue("created_by", r.createdBy())
                .addValue("created_at", java.sql.Timestamp.from(r.createdAt() == null ? java.time.Instant.now() : r.createdAt()))
                .addValue("ended_by", r.endedBy())
                .addValue("ended_at", r.endedAt() == null ? null : java.sql.Timestamp.from(r.endedAt())));
        return r;
    }

    public Optional<StaffDispatchRecord> findDispatchById(String id) {
        return jdbcTemplate.query(DISPATCH_SELECT + " WHERE id = :id", Map.of("id", id), this::mapDispatch)
                .stream().findFirst();
    }

    /** 同一员工在同一酒吧是否已有进行中的派驻。 */
    public boolean hasActiveDispatch(String userId, String merchantId) {
        Integer n = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM staff_dispatch_records
                 WHERE user_id = :user_id AND merchant_id = :merchant_id AND status = 'active'
                """, Map.of("user_id", userId, "merchant_id", merchantId), Integer.class);
        return n != null && n > 0;
    }

    public List<StaffDispatchRecord> listDispatches(String userId, String merchantId, String projectScope) {
        return jdbcTemplate.query(DISPATCH_SELECT + """
                 WHERE (:user_id::text IS NULL OR user_id = :user_id)
                   AND (:merchant_id::text IS NULL OR merchant_id = :merchant_id)
                   AND (:project_scope::text IS NULL OR project_id = :project_scope)
                 ORDER BY start_date DESC, id LIMIT 1000
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("merchant_id", merchantId).addValue("project_scope", projectScope),
                this::mapDispatch);
    }

    private StaffDispatchRecord mapDispatch(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        java.sql.Date end = rs.getDate("end_date");
        java.sql.Timestamp endedAt = rs.getTimestamp("ended_at");
        return new StaffDispatchRecord(rs.getString("id"), rs.getString("user_id"), rs.getString("company_id"),
                rs.getString("merchant_id"), rs.getString("project_id"), rs.getDate("start_date").toLocalDate(),
                end == null ? null : end.toLocalDate(), rs.getString("status"), rs.getString("note"),
                rs.getString("created_by"), rs.getTimestamp("created_at").toInstant(), rs.getString("ended_by"),
                endedAt == null ? null : endedAt.toInstant());
    }

    /**
     * 该月派驻人员在各酒吧促成订单的提成，按（酒吧商户 → 人员所属主体）汇总：
     * 提成归属月按开台或下单时点所属营业日（ENUM:187）；只计派驻记录覆盖计提日的员工（派驻起止按计提日判定），作废/取消的计提不计。
     * 行：merchant_id, company_id, project_id, amount_cent, calculation_count, staff_count。
     */
    public List<Map<String, Object>> aggregateDispatchedCommissions(String month) {
        List<String> commissions = businessDays.barCommissionIdsInMonth(java.time.YearMonth.parse(month), null);
        if (commissions.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.queryForList("""
                SELECT COALESCE(b.merchant_id, b.bar_id) AS merchant_id, d.company_id,
                       MAX(d.project_id) AS project_id,
                       COALESCE(SUM(c.commission_amount_cent), 0) AS amount_cent,
                       COUNT(c.id) AS calculation_count,
                       COUNT(DISTINCT c.sales_staff_id) AS staff_count
                  FROM bar_commission_calculations c
                  JOIN bar_bills b ON b.id = c.bill_id
                  JOIN staff_dispatch_records d
                    ON d.user_id = c.sales_staff_id
                   AND d.merchant_id = COALESCE(b.merchant_id, b.bar_id)
                   AND d.start_date <= (c.created_at AT TIME ZONE :local_zone)::date
                   AND (d.end_date IS NULL OR d.end_date >= (c.created_at AT TIME ZONE :local_zone)::date)
                 WHERE c.id IN (:commission_ids)
                   AND c.sales_staff_id IS NOT NULL
                   AND c.status NOT IN ('void', 'cancelled')
                   AND b.settlement_status = 'settled'
                 GROUP BY COALESCE(b.merchant_id, b.bar_id), d.company_id
                 ORDER BY 1, 2
                """, Map.of("commission_ids", commissions, "local_zone", com.complex.platform.common.LocalZone.ID.getId()));
    }

    private static final String SETTLEMENT_SELECT = """
            SELECT id, month, merchant_id, company_id, project_id, amount_cent, calculation_count, staff_count,
                   ar_ap_item_id, due_date, created_by, created_at
              FROM bar_commission_settlements
            """;

    /** 纯追加：同（月, 酒吧, 主体）已有记录则不覆盖（重复月结不重复挂账）。返回是否新写入。 */
    public boolean insertCommissionSettlementIfAbsent(BarCommissionSettlementRecord r) {
        int n = jdbcTemplate.update("""
                INSERT INTO bar_commission_settlements (id, month, merchant_id, company_id, project_id, amount_cent,
                        calculation_count, staff_count, ar_ap_item_id, due_date, created_by, created_at)
                VALUES (:id, :month, :merchant_id, :company_id, :project_id, :amount_cent,
                        :calculation_count, :staff_count, :ar_ap_item_id, :due_date, :created_by, :created_at)
                ON CONFLICT (month, merchant_id, company_id) DO NOTHING
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", r.id()).addValue("month", r.month()).addValue("merchant_id", r.merchantId())
                .addValue("company_id", r.companyId()).addValue("project_id", r.projectId())
                .addValue("amount_cent", r.amountCent()).addValue("calculation_count", r.calculationCount())
                .addValue("staff_count", r.staffCount()).addValue("ar_ap_item_id", r.arApItemId())
                .addValue("due_date", java.sql.Date.valueOf(r.dueDate())).addValue("created_by", r.createdBy())
                .addValue("created_at", java.sql.Timestamp.from(r.createdAt() == null ? java.time.Instant.now() : r.createdAt())));
        return n > 0;
    }

    public List<BarCommissionSettlementRecord> listCommissionSettlements(String month, String projectScope) {
        return jdbcTemplate.query(SETTLEMENT_SELECT + """
                 WHERE month = :month AND (:project_scope::text IS NULL OR project_id = :project_scope)
                 ORDER BY merchant_id, company_id
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("month", month).addValue("project_scope", projectScope), this::mapSettlement);
    }

    private BarCommissionSettlementRecord mapSettlement(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new BarCommissionSettlementRecord(rs.getString("id"), rs.getString("month"), rs.getString("merchant_id"),
                rs.getString("company_id"), rs.getString("project_id"), rs.getLong("amount_cent"),
                rs.getInt("calculation_count"), rs.getInt("staff_count"), rs.getString("ar_ap_item_id"),
                rs.getDate("due_date").toLocalDate(), rs.getString("created_by"), rs.getTimestamp("created_at").toInstant());
    }

    /** 全平台统一的三个付账日（升序）；未配置出厂行则 fail-closed。 */
    public int[] internalSettlementPayDays() {
        return jdbcTemplate.query("SELECT day_1, day_2, day_3 FROM internal_settlement_pay_days WHERE scope_id = 'global'",
                        Map.of(), (rs, n) -> new int[] {rs.getInt(1), rs.getInt(2), rs.getInt(3)})
                .stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("POLICY_NOT_CONFIGURED", "付账日未配置出厂行，不得结算"));
    }

    public java.util.List<StaffPayrollStatementRecord> listPayrollStatements(String month, String userId,
            String projectScope) {
        return jdbcTemplate.query("""
                SELECT id, user_id, month, base_cent, commission_cent, bonus_cent, penalty_cent,
                       leave_deduction_cent, tax_cent, total_cent, closed_by, closed_at, payer_company_id, tax_assessment_id, tax_version_id
                  FROM staff_payroll_statements
                 WHERE month = :month AND (:user_id::text IS NULL OR user_id = :user_id)
                   AND (:project_scope::text IS NULL OR project_id = :project_scope)
                 ORDER BY user_id
                 LIMIT 1000
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("month", month).addValue("user_id", userId).addValue("project_scope", projectScope),
                this::mapPayrollStatement);
    }

    private StaffPayrollStatementRecord mapPayrollStatement(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new StaffPayrollStatementRecord(
                rs.getString("id"), rs.getString("user_id"), rs.getString("month"),
                rs.getLong("base_cent"), rs.getLong("commission_cent"), rs.getLong("bonus_cent"),
                rs.getLong("penalty_cent"), rs.getLong("leave_deduction_cent"), rs.getLong("tax_cent"), rs.getLong("total_cent"),
                rs.getString("closed_by"), rs.getTimestamp("closed_at").toInstant(), rs.getString("payer_company_id"), rs.getString("tax_assessment_id"), rs.getString("tax_version_id"));
    }

    private static final String STAFF_MERCHANT_FILTER =
            " AND (:merchant_scope::text IS NULL OR user_id IN"
            + " (SELECT user_id FROM staff_profiles WHERE merchant_id = :merchant_scope"
            + " AND delete_status <> 'approved'))";

    public com.complex.platform.common.api.PageResponse<StaffLeaveRequestRecord> listLeavesPaged(
            String status, String userId, String projectScope, int page, int size) {
        return listLeavesPaged(status, userId, projectScope, null, page, size);
    }

    /** 第十八批 E 段 19-R1（移植 M738）：人事列表按「本公司 OR 登记项目 IN 集合」过滤（写法同 JdbcAnalyticsRepository 的 withProjects）。两者都 null＝不按公司、
     *  登记项目过滤（平台管理员、商户账号）；登记项目集合为空时用占位（空 IN () 是语法错误，project_id 不收空串），
     *  「无公司且空集合」调用前已提前返回。谓词写作 (:all_staff OR 公司条件 OR 登记项目 IN (:project_ids))。 */
    private static MapSqlParameterSource withStaffScope(MapSqlParameterSource params, String companyScope,
            java.util.Collection<String> projectIds) {
        return params.addValue("all_staff", companyScope == null && projectIds == null)
                .addValue("company_scope", companyScope)
                .addValue("project_ids", projectIds == null || projectIds.isEmpty() ? List.of("") : projectIds);
    }

    // ---------- AUD-005 真分页: SQL COUNT + LIMIT/OFFSET, 真实 total, 稳定排序(id 收敛), 同 scope 谓词 ----------
    public com.complex.platform.common.api.PageResponse<StaffShiftRecord> listShiftsPaged(
            java.time.LocalDate workDate, String userId, String projectScope, String merchantScope, int page, int size) {
        return listShiftsPaged(workDate, userId, projectScope, merchantScope, null, page, size);
    }

    public com.complex.platform.common.api.PageResponse<StaffShiftRecord> listShiftsPaged(
            java.time.LocalDate workDate, String userId, String projectScope, String merchantScope, String companyScope, int page, int size) {
        return listShiftsPaged(workDate, userId, projectScope, merchantScope, companyScope, null, page, size);
    }

    public com.complex.platform.common.api.PageResponse<StaffShiftRecord> listShiftsPaged(
            java.time.LocalDate workDate, String userId, String projectScope, String merchantScope, String companyScope,
            java.util.Collection<String> projectIds, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        if (companyScope == null && projectIds != null && projectIds.isEmpty()) {
            return new com.complex.platform.common.api.PageResponse<>(List.of(), safePage, safeSize, 0L);
        }
        String where = "WHERE (:work_date::date IS NULL OR work_date = :work_date)"
                + "   AND (:user_id::text IS NULL OR user_id = :user_id)"
                + "   AND (:project_scope::text IS NULL OR project_id = :project_scope)"
                + STAFF_MERCHANT_FILTER
                + " AND (:all_staff OR user_id IN (SELECT p.user_id FROM staff_profiles p WHERE p.company_id = :company_scope::text OR p.project_id IN (:project_ids)))";
        org.springframework.jdbc.core.namedparam.MapSqlParameterSource params = withStaffScope(
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                        .addValue("work_date", workDate).addValue("user_id", userId).addValue("project_scope", projectScope).addValue("merchant_scope", merchantScope), companyScope, projectIds);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_shift_schedules " + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        java.util.List<StaffShiftRecord> items = jdbcTemplate.query(
                "SELECT id, user_id, work_date, start_at, end_at, point_space_ids, note, created_by, created_at, updated_at, night_duty_project_id"
                        + " FROM staff_shift_schedules " + where
                        + " ORDER BY work_date DESC, start_at, user_id, id LIMIT :limit OFFSET :offset",
                params, this::mapShift);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    public com.complex.platform.common.api.PageResponse<StaffAttendanceRecord> listAttendanceOnPaged(
            java.time.LocalDate date, String projectScope, String merchantScope, int page, int size) {
        return listAttendanceOnPaged(date, projectScope, merchantScope, null, page, size);
    }

    public com.complex.platform.common.api.PageResponse<StaffAttendanceRecord> listAttendanceOnPaged(
            java.time.LocalDate date, String projectScope, String merchantScope, String companyScope, int page, int size) {
        return listAttendanceOnPaged(date, projectScope, merchantScope, companyScope, null, page, size);
    }

    public com.complex.platform.common.api.PageResponse<StaffAttendanceRecord> listAttendanceOnPaged(
            java.time.LocalDate date, String projectScope, String merchantScope, String companyScope,
            java.util.Collection<String> projectIds, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 1000 : Math.min(size, 1000);
        if (companyScope == null && projectIds != null && projectIds.isEmpty()) {
            return new com.complex.platform.common.api.PageResponse<>(List.of(), safePage, safeSize, 0L);
        }
        String where = "WHERE punched_at >= :day_start AND punched_at < :day_end"
                + "   AND (:project_scope::text IS NULL OR project_id = :project_scope)"
                + STAFF_MERCHANT_FILTER
                + " AND (:all_staff OR user_id IN (SELECT p.user_id FROM staff_profiles p WHERE p.company_id = :company_scope::text OR p.project_id IN (:project_ids)))";
        org.springframework.jdbc.core.namedparam.MapSqlParameterSource params = withStaffScope(
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                        .addValue("day_start", java.sql.Timestamp.from(date.atStartOfDay(LocalZone.ID).toInstant()))
                        .addValue("day_end", java.sql.Timestamp.from(date.plusDays(1).atStartOfDay(LocalZone.ID).toInstant()))
                        .addValue("project_scope", projectScope).addValue("merchant_scope", merchantScope), companyScope, projectIds);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_attendance_records " + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        java.util.List<StaffAttendanceRecord> items = jdbcTemplate.query(
                "SELECT id, user_id, punch_type, punched_at, method, tag_ref, space_id, device_fingerprint, shift_id, "
                        + "judgement, created_at FROM staff_attendance_records " + where
                        + " ORDER BY punched_at DESC, id LIMIT :limit OFFSET :offset",
                params, this::mapAttendance);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    public com.complex.platform.common.api.PageResponse<StaffPayrollStatementRecord> listPayrollStatementsVisible(
            String month,String userId,java.util.List<String> visibleIds,int page,int size) {
        int safePage=Math.max(1,page),safeSize=size<1?1000:Math.min(size,1000);
        var params=new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("month",month).addValue("user_id",userId);
        String where=" WHERE month=:month AND (:user_id::text IS NULL OR user_id=:user_id)";
        if(visibleIds!=null) {
            if(visibleIds.isEmpty())where+=" AND false";
            else {where+=" AND id IN (:visible_ids)";params.addValue("visible_ids",visibleIds);}
        }
        Long total=jdbcTemplate.queryForObject("SELECT count(*) FROM staff_payroll_statements"+where,params,Long.class);
        params.addValue("limit",safeSize).addValue("offset",(long)(safePage-1)*safeSize);
        var rows=jdbcTemplate.query("SELECT id,user_id,month,base_cent,commission_cent,bonus_cent,penalty_cent,leave_deduction_cent,tax_cent,total_cent,closed_by,closed_at,payer_company_id,tax_assessment_id,tax_version_id FROM staff_payroll_statements"+where+" ORDER BY user_id,id LIMIT :limit OFFSET :offset",params,this::mapPayrollStatement);
        return new com.complex.platform.common.api.PageResponse<>(rows,safePage,safeSize,total==null?0:total);
    }

    public com.complex.platform.common.api.PageResponse<StaffPayrollStatementRecord> listPayrollStatementsPaged(
            String month, String userId, String projectScope, String merchantScope, int page, int size) {
        return listPayrollStatementsPaged(month, userId, projectScope, merchantScope, null, page, size);
    }

    public com.complex.platform.common.api.PageResponse<StaffPayrollStatementRecord> listPayrollStatementsPaged(
            String month, String userId, String projectScope, String merchantScope, String companyScope, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 1000 : Math.min(size, 1000);
        String where = "WHERE month = :month AND (:user_id::text IS NULL OR user_id = :user_id)"
                + "   AND (:project_scope::text IS NULL OR project_id = :project_scope)"
                + STAFF_MERCHANT_FILTER
                + " AND (:company_scope::text IS NULL OR user_id IN (SELECT p.user_id FROM staff_profiles p WHERE p.company_id=:company_scope))";
        org.springframework.jdbc.core.namedparam.MapSqlParameterSource params =
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                        .addValue("month", month).addValue("user_id", userId).addValue("project_scope", projectScope).addValue("merchant_scope", merchantScope).addValue("company_scope", companyScope);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_payroll_statements " + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        java.util.List<StaffPayrollStatementRecord> items = jdbcTemplate.query(
                "SELECT id, user_id, month, base_cent, commission_cent, bonus_cent, penalty_cent, leave_deduction_cent, "
                        + "tax_cent, total_cent, closed_by, closed_at, payer_company_id, tax_assessment_id, tax_version_id FROM staff_payroll_statements " + where
                        + " ORDER BY user_id, id LIMIT :limit OFFSET :offset",
                params, this::mapPayrollStatement);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    public com.complex.platform.common.api.PageResponse<StaffLeaveRequestRecord> listLeavesPaged(
            String status, String userId, String projectScope, String merchantScope, int page, int size) {
        return listLeavesPaged(status, userId, projectScope, merchantScope, null, page, size);
    }

    public com.complex.platform.common.api.PageResponse<StaffLeaveRequestRecord> listLeavesPaged(
            String status, String userId, String projectScope, String merchantScope, String companyScope, int page, int size) {
        return listLeavesPaged(status, userId, projectScope, merchantScope, companyScope, null, page, size);
    }

    public com.complex.platform.common.api.PageResponse<StaffLeaveRequestRecord> listLeavesPaged(
            String status, String userId, String projectScope, String merchantScope, String companyScope,
            java.util.Collection<String> projectIds, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : Math.min(size, 500);
        if (companyScope == null && projectIds != null && projectIds.isEmpty()) {
            return new com.complex.platform.common.api.PageResponse<>(List.of(), safePage, safeSize, 0L);
        }
        String where = "WHERE (:status::text IS NULL OR status = :status)"
                + "   AND (:user_id::text IS NULL OR user_id = :user_id)"
                + "   AND (:project_scope::text IS NULL OR project_id = :project_scope)"
                + STAFF_MERCHANT_FILTER
                + " AND (:all_staff OR user_id IN (SELECT p.user_id FROM staff_profiles p WHERE p.company_id = :company_scope::text OR p.project_id IN (:project_ids)))";
        org.springframework.jdbc.core.namedparam.MapSqlParameterSource params = withStaffScope(
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                        .addValue("status", status).addValue("user_id", userId).addValue("project_scope", projectScope).addValue("merchant_scope", merchantScope), companyScope, projectIds);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_leave_requests " + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        java.util.List<StaffLeaveRequestRecord> items = jdbcTemplate.query(
                "SELECT id, user_id, leave_type, start_date, end_date, reason, status, reviewed_by, review_note, "
                        + "created_at, updated_at, revoke_requested_by, revoke_requested_at, revoke_request_reason, revoked_by, revoked_at, revoke_reason FROM staff_leave_requests " + where
                        + " ORDER BY created_at DESC, id LIMIT :limit OFFSET :offset",
                params, this::mapLeave);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    public java.util.List<PayrollTaxBracketRecord> listTaxBrackets(String category) {
        return jdbcTemplate.query("""
                SELECT id, category, seq, income_floor_cent, income_ceiling_cent, income_base_bps,
                       flat_deduction_cent, rate_bps, quick_deduction_cent, effective_from, updated_by, updated_at
                  FROM payroll_tax_brackets
                 WHERE (:category::text IS NULL OR category = :category)
                 ORDER BY category, seq
                """, new MapSqlParameterSource().addValue("category", category),
                (rs, n) -> mapTaxBracket(rs));
    }

    public PayrollTaxBracketRecord upsertTaxBracket(PayrollTaxBracketRecord record) {
        jdbcTemplate.update("""
                INSERT INTO payroll_tax_brackets (id, category, seq, income_floor_cent, income_ceiling_cent,
                        income_base_bps, flat_deduction_cent, rate_bps, quick_deduction_cent, effective_from,
                        updated_by, updated_at)
                VALUES (:id, :category, :seq, :floor, :ceiling, :base_bps, :flat, :rate_bps, :quick, :eff,
                        :updated_by, :now)
                ON CONFLICT (id) DO UPDATE SET
                    category = EXCLUDED.category, seq = EXCLUDED.seq,
                    income_floor_cent = EXCLUDED.income_floor_cent,
                    income_ceiling_cent = EXCLUDED.income_ceiling_cent,
                    income_base_bps = EXCLUDED.income_base_bps,
                    flat_deduction_cent = EXCLUDED.flat_deduction_cent,
                    rate_bps = EXCLUDED.rate_bps,
                    quick_deduction_cent = EXCLUDED.quick_deduction_cent,
                    effective_from = EXCLUDED.effective_from,
                    updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", record.id()).addValue("category", record.category())
                .addValue("seq", record.seq()).addValue("floor", record.incomeFloorCent())
                .addValue("ceiling", record.incomeCeilingCent()).addValue("base_bps", record.incomeBaseBps())
                .addValue("flat", record.flatDeductionCent()).addValue("rate_bps", record.rateBps())
                .addValue("quick", record.quickDeductionCent()).addValue("eff", record.effectiveFrom())
                .addValue("updated_by", record.updatedBy())
                .addValue("now", timestampWithTimeZone(record.updatedAt())));
        return jdbcTemplate.query("""
                SELECT id, category, seq, income_floor_cent, income_ceiling_cent, income_base_bps,
                       flat_deduction_cent, rate_bps, quick_deduction_cent, effective_from, updated_by, updated_at
                  FROM payroll_tax_brackets WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", record.id()),
                (rs, n) -> mapTaxBracket(rs)).stream().findFirst().orElse(null);
    }

    private static PayrollTaxBracketRecord mapTaxBracket(ResultSet rs) throws SQLException {
        Long ceiling = rs.getObject("income_ceiling_cent") == null ? null : rs.getLong("income_ceiling_cent");
        java.sql.Date eff = rs.getDate("effective_from");
        return new PayrollTaxBracketRecord(
                rs.getString("id"), rs.getString("category"), rs.getInt("seq"),
                rs.getLong("income_floor_cent"), ceiling, rs.getInt("income_base_bps"),
                rs.getLong("flat_deduction_cent"), rs.getInt("rate_bps"), rs.getLong("quick_deduction_cent"),
                eff == null ? null : eff.toLocalDate(), rs.getString("updated_by"),
                rs.getTimestamp("updated_at").toInstant());
    }

    public StaffLeaveRequestRecord saveLeaveRequest(StaffLeaveRequestRecord record) {
        jdbcTemplate.update("""
                INSERT INTO staff_leave_requests (id, user_id, leave_type, start_date, end_date, reason,
                        status, reviewed_by, review_note, created_at, updated_at, project_id)
                VALUES (:id, :user_id, :leave_type, :start_date, :end_date, :reason,
                        :status, :reviewed_by, :review_note, :created_at, :updated_at, """
                + STAFF_PROJECT_SUBSELECT + """
                )
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

    /** 2026-10-18 操作员裁决：员工本人申请销假——状态转「待销假审批」(revoke_requested)，另记申请人/申请时间/申请理由；原审批留痕不动。 */
    public StaffLeaveRequestRecord markLeaveRevokeRequested(String id, String requestedBy, String reason, java.time.Instant at) {
        return jdbcTemplate.query("""
                UPDATE staff_leave_requests
                   SET status = 'revoke_requested', revoke_requested_by = :by, revoke_requested_at = :at,
                       revoke_request_reason = :reason, updated_at = :at
                 WHERE id = :id
                RETURNING id, user_id, leave_type, start_date, end_date, reason, status,
                          reviewed_by, review_note, created_at, updated_at, revoke_requested_by, revoke_requested_at, revoke_request_reason, revoked_by, revoked_at, revoke_reason
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("by", requestedBy).addValue("reason", reason)
                .addValue("at", java.sql.Timestamp.from(at)), this::mapLeave).getFirst();
    }

    /** 主管驳回销假申请：回到「已批准」、假仍有效；申请三列保留为留痕。 */
    public StaffLeaveRequestRecord markLeaveRevokeRejected(String id, java.time.Instant at) {
        return jdbcTemplate.query("""
                UPDATE staff_leave_requests
                   SET status = 'approved', updated_at = :at
                 WHERE id = :id
                RETURNING id, user_id, leave_type, start_date, end_date, reason, status,
                          reviewed_by, review_note, created_at, updated_at, revoke_requested_by, revoke_requested_at, revoke_request_reason, revoked_by, revoked_at, revoke_reason
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("at", java.sql.Timestamp.from(at)), this::mapLeave).getFirst();
    }

    /** BSM L1481：主管批准销假只改状态并另记批准人/时间/说明；原「主管批准」的 reviewed_by / review_note 与本人申请三列保留。返回更新后的请假单。 */
    public StaffLeaveRequestRecord markLeaveRevoked(String id, String revokedBy, String reason, java.time.Instant at) {
        return jdbcTemplate.query("""
                UPDATE staff_leave_requests
                   SET status = 'revoked', revoked_by = :by, revoked_at = :at, revoke_reason = :reason, updated_at = :at
                 WHERE id = :id
                RETURNING id, user_id, leave_type, start_date, end_date, reason, status,
                          reviewed_by, review_note, created_at, updated_at, revoke_requested_by, revoke_requested_at, revoke_request_reason, revoked_by, revoked_at, revoke_reason
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("by", revokedBy).addValue("reason", reason)
                .addValue("at", java.sql.Timestamp.from(at)), this::mapLeave).getFirst();
    }

    public java.util.Optional<StaffLeaveRequestRecord> findLeaveById(String id) {
        return jdbcTemplate.query("""
                SELECT id, user_id, leave_type, start_date, end_date, reason, status,
                       reviewed_by, review_note, created_at, updated_at, revoke_requested_by, revoke_requested_at, revoke_request_reason, revoked_by, revoked_at, revoke_reason
                  FROM staff_leave_requests WHERE id = :id
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource().addValue("id", id),
                this::mapLeave).stream().findFirst();
    }

    public java.util.List<StaffLeaveRequestRecord> listLeaves(String status, String userId, String projectScope) {
        return jdbcTemplate.query("""
                SELECT id, user_id, leave_type, start_date, end_date, reason, status,
                       reviewed_by, review_note, created_at, updated_at, revoke_requested_by, revoke_requested_at, revoke_request_reason, revoked_by, revoked_at, revoke_reason
                  FROM staff_leave_requests
                 WHERE (:status::text IS NULL OR status = :status)
                   AND (:user_id::text IS NULL OR user_id = :user_id)
                   AND (:project_scope::text IS NULL OR project_id = :project_scope)
                 ORDER BY created_at DESC
                 LIMIT 500
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("status", status).addValue("user_id", userId)
                .addValue("project_scope", projectScope), this::mapLeave);
    }

    public boolean hasActiveLeaveOverlap(String userId, java.time.LocalDate start, java.time.LocalDate end) {
        // 两区间 [s1,e1]/[s2,e2] 重叠 <=> s1<=e2 && s2<=e1;只算未撤销(pending/approved/revoke_requested——申请销假待主管批准的假仍有效)。
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM staff_leave_requests
                               WHERE user_id = :user_id AND status IN ('pending','approved','revoke_requested')
                                 AND start_date <= :end AND end_date >= :start)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("start", start).addValue("end", end), Boolean.class));
    }

    /** BSM L1481：销假时找出已把该请假计入事假工时的定格工资月份（按锁定快照 worktime.leaveIds）。 */
    public java.util.List<String> lockedPayrollMonthsUsingLeave(String userId, String leaveId) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT month FROM staff_payroll_statements
                 WHERE user_id = :user_id
                   AND calculation_snapshot -> 'worktime' -> 'leaveIds' @> to_jsonb(:leave_id::text)
                 ORDER BY month
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("leave_id", leaveId), String.class);
    }

    private StaffLeaveRequestRecord mapLeave(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new StaffLeaveRequestRecord(
                rs.getString("id"), rs.getString("user_id"), rs.getString("leave_type"),
                rs.getObject("start_date", java.time.LocalDate.class),
                rs.getObject("end_date", java.time.LocalDate.class),
                rs.getString("reason"), rs.getString("status"), rs.getString("reviewed_by"),
                rs.getString("review_note"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(), rs.getString("revoke_requested_by"),
                rs.getTimestamp("revoke_requested_at") == null ? null : rs.getTimestamp("revoke_requested_at").toInstant(),
                rs.getString("revoke_request_reason"), rs.getString("revoked_by"),
                rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toInstant(),
                rs.getString("revoke_reason"));
    }

    public java.util.Set<java.time.LocalDate> approvedLeaveDates(String userId,
            java.time.LocalDate from, java.time.LocalDate to) {
        java.util.Set<java.time.LocalDate> out = new java.util.LinkedHashSet<>();
        jdbcTemplate.query("""
                SELECT start_date, end_date FROM staff_leave_requests
                 WHERE user_id = :user_id AND status IN ('approved', 'revoke_requested')
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

    public Optional<StaffShiftRecord> findShiftById(String id) {
        return jdbcTemplate.query("SELECT * FROM staff_shift_schedules WHERE id=:id FOR UPDATE", Map.of("id", id),
                this::mapShift).stream().findFirst();
    }

    public Optional<StaffShiftRecord> findShiftByUserStart(String userId, java.time.Instant start) {
        return jdbcTemplate.query("SELECT * FROM staff_shift_schedules WHERE user_id=:user AND start_at=:start FOR UPDATE",
                Map.of("user", userId, "start", java.sql.Timestamp.from(start)), this::mapShift).stream().findFirst();
    }

    public record ShiftRevision(String id, String reviewId, String beforeValue, String afterValue, java.time.Instant appliedAt) {}

    public void saveShiftRevision(String shiftId, String reviewId, String beforeValue, String afterValue, String actor) {
        jdbcTemplate.update("""
                INSERT INTO staff_shift_revisions(id,shift_id,review_id,before_value,after_value,initiated_by)
                VALUES(:id,:shift,:review,CAST(:before AS jsonb),CAST(:after AS jsonb),:actor)
                """, new MapSqlParameterSource().addValue("id", "rev_"+java.util.UUID.randomUUID())
                .addValue("shift", shiftId).addValue("review", reviewId).addValue("before", beforeValue)
                .addValue("after", afterValue).addValue("actor", actor));
    }

    /** U06-004：顶班记录只追加。 */
    public StaffShiftCoverRecord saveShiftCover(StaffShiftCoverRecord cover) {
        jdbcTemplate.update("""
                INSERT INTO staff_shift_covers(id, shift_id, original_user_id, cover_user_id, arranged_by, reason, created_at,
                        start_at, end_at, handover_note, cover_shift_id, source_updated_at)
                VALUES (:id, :shift, :original, :cover, :by, :reason, :at, :start, :end, :handover, :cover_shift, :source_version)
                """, new MapSqlParameterSource().addValue("id", cover.id()).addValue("shift", cover.shiftId())
                .addValue("original", cover.originalUserId()).addValue("cover", cover.coverUserId()).addValue("by", cover.arrangedBy())
                .addValue("reason", cover.reason()).addValue("at", java.sql.Timestamp.from(cover.createdAt()))
                .addValue("start", java.sql.Timestamp.from(cover.startAt())).addValue("end", java.sql.Timestamp.from(cover.endAt()))
                .addValue("handover", cover.handoverNote()).addValue("cover_shift", cover.coverShiftId())
                .addValue("source_version", java.sql.Timestamp.from(cover.sourceUpdatedAt())));
        return cover;
    }

    public List<StaffShiftCoverRecord> listShiftCovers(String shiftId) {
        return jdbcTemplate.query("""
                SELECT * FROM staff_shift_covers WHERE shift_id=:shift ORDER BY created_at, id
                """, Map.of("shift", shiftId), (rs, n) -> new StaffShiftCoverRecord(rs.getString("id"), rs.getString("shift_id"),
                rs.getString("original_user_id"), rs.getString("cover_user_id"), rs.getString("arranged_by"), rs.getString("reason"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("start_at").toInstant(), rs.getTimestamp("end_at").toInstant(),
                rs.getString("handover_note"), rs.getString("cover_shift_id"), rs.getTimestamp("source_updated_at").toInstant()));
    }

    /** U05-007：只有审批落地路径需要行锁（lockForUpdate=true）；只读历史不加 FOR UPDATE。 */
    public List<ShiftRevision> shiftRevisions(String reviewId, String shiftId, boolean lockForUpdate) {
        return jdbcTemplate.query("""
                SELECT * FROM staff_shift_revisions
                 WHERE (CAST(:review AS text) IS NULL OR review_id=:review)
                   AND (CAST(:shift AS text) IS NULL OR shift_id=:shift)
                 ORDER BY created_at,id
                """ + (lockForUpdate ? " FOR UPDATE" : ""), new MapSqlParameterSource().addValue("review", reviewId).addValue("shift", shiftId),
                (rs,n) -> new ShiftRevision(rs.getString("id"),rs.getString("review_id"),rs.getString("before_value"),
                        rs.getString("after_value"),nullableInstant(rs,"applied_at")));
    }

    public void publishShiftRevision(ShiftRevision revision, StaffShiftRecord before, StaffShiftRecord after) {
        lockAttendanceUser(after.userId());
        requireShiftDoesNotRewriteCover(after);
        if (before != null && before.nightDutyProjectId() != null) lockNightDutyProject(before.nightDutyProjectId());
        requireNightDutySchedule(after);
        var now = java.time.Instant.now();
        if (!after.startAt().isAfter(now) || (before != null && !before.startAt().isAfter(now))) {
            throw com.complex.platform.common.api.ApiException.conflict("SHIFT_STARTED", "班次已经开始，本次修改不能发布");
        }
        var params = new MapSqlParameterSource().addValue("id", after.id()).addValue("user_id", after.userId())
                .addValue("work_date", after.workDate()).addValue("start", java.sql.Timestamp.from(after.startAt()))
                .addValue("end", java.sql.Timestamp.from(after.endAt())).addValue("points", pointsJson(after.pointSpaceIds()))
                .addValue("note", after.note()).addValue("actor", after.createdBy()).addValue("now", java.sql.Timestamp.from(now))
                .addValue("night_project", after.nightDutyProjectId());
        if (before == null) {
            jdbcTemplate.update("""
                    INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,note,created_by,created_at,updated_at,night_duty_project_id,project_id)
                    VALUES(:id,:user_id,:work_date,:start,:end,CAST(:points AS jsonb),:note,:actor,:now,:now,:night_project,
                    """ + STAFF_PROJECT_SUBSELECT + ")",params);
        } else {
            params.addValue("previous_updated", java.sql.Timestamp.from(before.updatedAt()));
            int updated = jdbcTemplate.update("""
                    UPDATE staff_shift_schedules SET work_date=:work_date,start_at=:start,end_at=:end,
                           point_space_ids=CAST(:points AS jsonb),note=:note,updated_at=:now,night_duty_project_id=:night_project
                     WHERE id=:id AND updated_at=:previous_updated AND start_at>:now
                    """,params);
            if (updated != 1) throw com.complex.platform.common.api.ApiException.conflict("SHIFT_CHANGED", "原排班已变更或已开始，请核对后重新申请");
        }
        jdbcTemplate.update("UPDATE staff_shift_revisions SET applied_at=:now WHERE id=:id AND applied_at IS NULL",
                Map.of("id", revision.id(),"now",java.sql.Timestamp.from(now)));
    }

    /** PGM:429：夜班职责跟随经批准排班；同项目同日一人且时段不能重叠。 */
    public void requireNightDutySchedule(StaffShiftRecord shift) {
        String project = shift.nightDutyProjectId();
        if (project == null) return;
        if (!project.equals(findUserProjectId(shift.userId()).orElse(null))) {
            throw ApiException.conflict("NIGHT_DUTY_PROJECT_CHANGED", "员工项目归属与待批准夜班职责不一致，请重新提交");
        }
        if (!shift.workDate().equals(java.time.LocalDate.ofInstant(shift.startAt(), com.complex.platform.common.LocalZone.ID))) {
            throw ApiException.badRequest("NIGHT_DUTY_DATE_INVALID", "夜班工作日期须为该班开始所在日期");
        }
        lockNightDutyProject(project);
        Boolean conflict = jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM staff_shift_schedules
                    WHERE night_duty_project_id=:project AND id<>:id
                      AND (work_date=:date OR (start_at<:end AND end_at>:start)))
                """, new MapSqlParameterSource().addValue("project",project).addValue("id",shift.id())
                .addValue("date",shift.workDate()).addValue("start",java.sql.Timestamp.from(shift.startAt()))
                .addValue("end",java.sql.Timestamp.from(shift.endAt())),Boolean.class);
        if (Boolean.TRUE.equals(conflict)) throw ApiException.conflict("NIGHT_DUTY_CONFLICT",
                "本项目该日或相交时段已有已发布的夜班值班经理；改派须先审批取消原班职责");
    }

    private void lockNightDutyProject(String project) {
        if (jdbcTemplate.queryForList("SELECT id FROM projects WHERE id=:id FOR UPDATE",Map.of("id",project)).isEmpty()) {
            throw ApiException.badRequest("NIGHT_DUTY_PROJECT_REQUIRED", "夜班职责须归属真实项目");
        }
    }

    public void requireDistinctNightDutyDrafts(List<StaffShiftRecord> drafts) {
        var duties = drafts.stream().filter(s -> s.nightDutyProjectId()!=null).toList();
        for (int i=0;i<duties.size();i++) for (int j=i+1;j<duties.size();j++) {
            var a=duties.get(i); var b=duties.get(j);
            if (a.nightDutyProjectId().equals(b.nightDutyProjectId()) &&
                    (a.workDate().equals(b.workDate()) || (a.startAt().isBefore(b.endAt()) && a.endAt().isAfter(b.startAt())))) {
                throw ApiException.conflict("NIGHT_DUTY_CONFLICT", "同一批不能为本项目同日或相交时段安排两名夜班值班经理");
            }
        }
    }

    public record NightDuty(String originalShiftId,String effectiveShiftId) {}

    /** 原班、部分顶班和继续顶班均沿真实交接关系确定唯一当班人，不修改原班及缺勤。 */
    public Optional<NightDuty> findCurrentNightDuty(String userId,String projectId,java.time.Instant at) {
        if (!projectId.equals(findUserProjectId(userId).orElse(null))) return Optional.empty();
        var roots=jdbcTemplate.queryForList("""
                SELECT id FROM staff_shift_schedules
                 WHERE night_duty_project_id=:project AND start_at<=:at AND end_at>:at
                """,new MapSqlParameterSource().addValue("project",projectId)
                .addValue("at",java.sql.Timestamp.from(at)),String.class);
        var matches=roots.stream().flatMap(root->findEffectiveShiftHolders(root,at).stream()
                .filter(h->h.userId().equals(userId)).map(h->new NightDuty(root,h.shiftId()))).toList();
        if (matches.size()>1) throw ApiException.conflict("NIGHT_DUTY_AMBIGUOUS", "本项目当前夜班职责存在重叠，请核实排班");
        return matches.stream().findFirst();
    }

    /** 已发布班次此刻的实际当班人；endAt 为该段（原班或顶班时段）结束时刻。 */
    public record ShiftHolder(String userId,String shiftId,java.time.Instant endAt) {}

    /**
     * HP-F01：已发布班次此刻的实际当班人——原班，或沿顶班（部分顶班、继续顶班）递归到此刻无人再顶的那一段；
     * 班次此刻不在进行（未开始或已结束）返回空。夜班值班经理与设备告警接警值守共用这一份顶班解析。
     */
    public List<ShiftHolder> findEffectiveShiftHolders(String shiftId,java.time.Instant at) {
        return jdbcTemplate.query("""
                WITH RECURSIVE duty(id,user_id,end_at) AS (
                    SELECT id,user_id,end_at FROM staff_shift_schedules
                     WHERE id=:shift AND start_at<=:at AND end_at>:at
                    UNION ALL
                    SELECT c.cover_shift_id,c.cover_user_id,c.end_at FROM duty d
                      JOIN staff_shift_covers c ON c.shift_id=d.id
                     WHERE c.start_at<=:at AND c.end_at>:at
                )
                SELECT d.id,d.user_id,d.end_at FROM duty d
                 WHERE NOT EXISTS(SELECT 1 FROM staff_shift_covers c WHERE c.shift_id=d.id
                       AND c.start_at<=:at AND c.end_at>:at)
                """,new MapSqlParameterSource().addValue("shift",shiftId).addValue("at",java.sql.Timestamp.from(at)),
                (rs,n)->new ShiftHolder(rs.getString("user_id"),rs.getString("id"),nullableInstant(rs,"end_at")));
    }

    /** 普通排班审批不能回改已记录的顶班事实，或在待审期间与新增顶班形成重复应出勤。 */
    public void requireShiftDoesNotRewriteCover(StaffShiftRecord shift) {
        var params = new MapSqlParameterSource().addValue("id",shift.id()).addValue("user",shift.userId())
                .addValue("start",java.sql.Timestamp.from(shift.startAt())).addValue("end",java.sql.Timestamp.from(shift.endAt()));
        Boolean covers = jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM staff_shift_covers WHERE cover_shift_id=:id
                    OR (cover_user_id=:user AND start_at<:end AND end_at>:start))
                """,params,Boolean.class);
        if (Boolean.TRUE.equals(covers)) throw ApiException.conflict("COVER_SHIFT_LOCKED",
                "本次排班涉及已安排的顶班时段，不能改写顶班事实或重复安排");
    }

    public StaffShiftRecord upsertShift(StaffShiftRecord shift) {
        requireNightDutySchedule(shift);
        jdbcTemplate.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at,
                        point_space_ids, note, created_by, created_at, updated_at, night_duty_project_id, project_id)
                VALUES (:id, :user_id, :work_date, :start_at, :end_at, :points::jsonb, :note, :created_by, :now, :now, :night_project, """
                + STAFF_PROJECT_SUBSELECT + """
                )
                ON CONFLICT (user_id, start_at) DO UPDATE SET
                    work_date = EXCLUDED.work_date,
                    end_at = EXCLUDED.end_at,
                    point_space_ids = EXCLUDED.point_space_ids,
                    note = EXCLUDED.note,
                    night_duty_project_id = EXCLUDED.night_duty_project_id,
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
                .addValue("now", java.sql.Timestamp.from(shift.updatedAt())).addValue("night_project", shift.nightDutyProjectId()));
        return listShiftsForUserBetween(shift.userId(), shift.workDate(), shift.workDate()).stream()
                .filter(row -> row.startAt().equals(shift.startAt()))
                .findFirst().orElse(shift);
    }

    public java.util.List<StaffShiftRecord> listShifts(java.time.LocalDate workDate, String userId,
            String projectScope) {
        return jdbcTemplate.query("""
                SELECT id, user_id, work_date, start_at, end_at, point_space_ids, note, created_by, created_at, updated_at, night_duty_project_id
                  FROM staff_shift_schedules
                 WHERE (:work_date::date IS NULL OR work_date = :work_date)
                   AND (:user_id::text IS NULL OR user_id = :user_id)
                   AND (:project_scope::text IS NULL OR project_id = :project_scope)
                 ORDER BY work_date DESC, start_at, user_id
                 LIMIT 500
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("work_date", workDate).addValue("user_id", userId)
                .addValue("project_scope", projectScope), this::mapShift);
    }

    /**
     * 打卡配班用：按时间区间重叠拉候选班次，不经 work_date。
     *
     * <p>排班本来就存「几月几日几点 → 几月几日几点」（start_at/end_at），跨零点班（8/2 22:00 → 8/3 06:00）
     * 的 work_date 记的是开始那天，按 work_date 查「打卡当天」必然漏掉它——这是员工凌晨下班被记「无排班」的根因。
     * work_date 列保留不动（列表页与索引还在用）。
     */
    public java.util.List<StaffShiftRecord> listShiftsForUserOverlapping(String userId,
            java.time.Instant from, java.time.Instant to) {
        return jdbcTemplate.query("""
                SELECT id, user_id, work_date, start_at, end_at, point_space_ids, note, created_by, created_at, updated_at, night_duty_project_id
                  FROM staff_shift_schedules
                 WHERE user_id = :user_id AND end_at >= :from AND start_at <= :to
                 ORDER BY start_at
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId)
                .addValue("from", java.sql.Timestamp.from(from))
                .addValue("to", java.sql.Timestamp.from(to)), this::mapShift);
    }

    public java.util.List<StaffShiftRecord> listShiftsForUserBetween(String userId,
            java.time.LocalDate from, java.time.LocalDate to) {
        return jdbcTemplate.query("""
                SELECT id, user_id, work_date, start_at, end_at, point_space_ids, note, created_by, created_at, updated_at, night_duty_project_id
                  FROM staff_shift_schedules
                 WHERE user_id = :user_id AND work_date BETWEEN :from AND :to
                 ORDER BY start_at
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("from", from).addValue("to", to), this::mapShift);
    }

    public List<StaffShiftRecord> listShiftsEndingBetween(String userId, java.time.Instant from,
            java.time.Instant until) {
        return jdbcTemplate.query("""
                SELECT * FROM staff_shift_schedules
                 WHERE user_id = :user_id AND end_at >= :from AND end_at < :until
                 ORDER BY start_at, id
                """, new MapSqlParameterSource().addValue("user_id", userId)
                .addValue("from", java.sql.Timestamp.from(from))
                .addValue("until", java.sql.Timestamp.from(until)), this::mapShift);
    }

    public List<StaffAttendanceRecord> listAttendanceForShifts(String userId, List<String> shiftIds) {
        if (shiftIds.isEmpty()) return List.of();
        return jdbcTemplate.query("""
                SELECT * FROM staff_attendance_records
                 WHERE user_id = :user_id AND shift_id IN (:shift_ids)
                 ORDER BY punched_at, id
                """, new MapSqlParameterSource().addValue("user_id", userId)
                .addValue("shift_ids", shiftIds), this::mapAttendance);
    }

    private StaffShiftRecord mapShift(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new StaffShiftRecord(
                rs.getString("id"), rs.getString("user_id"),
                rs.getObject("work_date", java.time.LocalDate.class),
                rs.getTimestamp("start_at").toInstant(), rs.getTimestamp("end_at").toInstant(),
                parsePoints(rs.getString("point_space_ids")), rs.getString("note"), rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                null, "published", rs.getString("night_duty_project_id"));
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

    public void lockAttendanceUser(String userId) {
        jdbcTemplate.query("SELECT id FROM staff_profiles WHERE user_id=:user_id FOR UPDATE",
                Map.of("user_id", userId), (rs, n) -> rs.getString("id"));
    }

    public Optional<StaffAttendanceRecord> findFirstCheckIn(String userId, String shiftId, java.time.LocalDate day) {
        return jdbcTemplate.query("""
                SELECT * FROM staff_attendance_records
                 WHERE user_id=:user_id AND punch_type='check_in'
                   AND ((CAST(:shift_id AS text) IS NOT NULL AND shift_id=:shift_id)
                     OR (CAST(:shift_id AS text) IS NULL AND shift_id IS NULL
                         AND (punched_at AT TIME ZONE 'Asia/Shanghai')::date=:day))
                 ORDER BY punched_at, created_at, id LIMIT 1
                """, new MapSqlParameterSource().addValue("user_id", userId)
                .addValue("shift_id", shiftId).addValue("day", day), this::mapAttendance).stream().findFirst();
    }

    public StaffAttendanceRecord saveAttendance(StaffAttendanceRecord record) {
        jdbcTemplate.update("""
                INSERT INTO staff_attendance_records (id, user_id, punch_type, punched_at, method,
                        tag_ref, space_id, device_fingerprint, shift_id, judgement, created_at, project_id)
                VALUES (:id, :user_id, :punch_type, :punched_at, :method,
                        :tag_ref, :space_id, :device_fingerprint, :shift_id, :judgement, :created_at, """
                + STAFF_PROJECT_SUBSELECT + """
                )
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

    public java.util.List<StaffAttendanceRecord> listAttendanceOn(java.time.LocalDate date, String projectScope) {
        return jdbcTemplate.query("""
                SELECT id, user_id, punch_type, punched_at, method, tag_ref, space_id,
                       device_fingerprint, shift_id, judgement, created_at
                  FROM staff_attendance_records
                 WHERE punched_at >= :day_start AND punched_at < :day_end
                   AND (:project_scope::text IS NULL OR project_id = :project_scope)
                 ORDER BY punched_at DESC
                 LIMIT 1000
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("day_start", java.sql.Timestamp.from(date.atStartOfDay(LocalZone.ID).toInstant()))
                .addValue("day_end", java.sql.Timestamp.from(date.plusDays(1).atStartOfDay(LocalZone.ID).toInstant()))
                .addValue("project_scope", projectScope),
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

    public List<StaffProfileRecord> listProfilesByCompany(String companyId) {
        return jdbcTemplate.query("SELECT * FROM staff_profiles WHERE company_id=:company AND delete_status<>'approved' ORDER BY created_at DESC",
                Map.of("company",companyId),this::mapProfile);
    }

    public StaffProfileRecord saveProfile(StaffProfileRecord profile) {
        // U05-010：company_id 为写入必填，不再以 project_id 兜底（读路径已去掉 COALESCE）。
        if (profile.companyId() == null || profile.companyId().isBlank()) {
            throw ApiException.conflict("STAFF_COMPANY_REQUIRED", "员工档案必须归属公司");
        }
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (
                    id, project_id, merchant_id, user_id, name, phone, id_doc, staff_level_id,
                    registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at,
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, is_sales_position, employment_status, company_id, registration_source
                )
                VALUES (
                    :id, :project_id, :merchant_id, :user_id, :name, :phone, :id_doc, :staff_level_id,
                    :registration_status, :approved_by, :approved_at, :reject_reason, :status, :created_at, :updated_at,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason,
                    :is_sales_position, :employment_status, :company_id, :registration_source
                )
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    merchant_id = EXCLUDED.merchant_id,
                    user_id = EXCLUDED.user_id,
                    name = EXCLUDED.name,
                    phone = EXCLUDED.phone,
                    id_doc = EXCLUDED.id_doc,
                    staff_level_id = EXCLUDED.staff_level_id,
                    registration_status = EXCLUDED.registration_status,
                    approved_by = EXCLUDED.approved_by,
                    approved_at = EXCLUDED.approved_at,
                    reject_reason = EXCLUDED.reject_reason,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    archived_at = EXCLUDED.archived_at,
                    archived_by = EXCLUDED.archived_by,
                    delete_status = EXCLUDED.delete_status,
                    delete_requested_by = EXCLUDED.delete_requested_by,
                    delete_requested_at = EXCLUDED.delete_requested_at,
                    delete_reason = EXCLUDED.delete_reason,
                    is_sales_position = EXCLUDED.is_sales_position,
                    employment_status = EXCLUDED.employment_status,
                    company_id = EXCLUDED.company_id, registration_source = EXCLUDED.registration_source
                """, new MapSqlParameterSource()
                .addValue("id", profile.id())
                .addValue("project_id", profile.projectId())
                .addValue("merchant_id", profile.merchantId())
                .addValue("user_id", profile.userId())
                .addValue("name", piiCipher.encrypt(profile.name()))
                .addValue("phone", piiCipher.encrypt(profile.phone()))
                .addValue("id_doc", piiCipher.encrypt(profile.idDoc()))
                .addValue("staff_level_id", profile.staffLevelId())
                .addValue("registration_status", profile.registrationStatus())
                .addValue("approved_by", profile.approvedBy())
                .addValue("approved_at", timestampWithTimeZone(profile.approvedAt()))
                .addValue("reject_reason", profile.rejectReason())
                .addValue("status", profile.status())
                .addValue("created_at", timestampWithTimeZone(profile.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(profile.updatedAt()))
                .addValue("archived_at", timestampWithTimeZone(profile.archivedAt()))
                .addValue("archived_by", profile.archivedBy())
                .addValue("delete_status", profile.deleteStatus() == null ? "none" : profile.deleteStatus())
                .addValue("delete_requested_by", profile.deleteRequestedBy())
                .addValue("delete_requested_at", timestampWithTimeZone(profile.deleteRequestedAt()))
                .addValue("delete_reason", profile.deleteReason())
                .addValue("is_sales_position", profile.salesPosition())
                .addValue("employment_status", profile.employmentStatus())
                .addValue("company_id",profile.companyId()).addValue("registration_source",profile.registrationSource()));
        return profile;
    }

    public void recordRegistrationInitiator(String id, String actorId) {
        jdbcTemplate.update("UPDATE staff_profiles SET submitted_by = :actor WHERE id = :id",
                Map.of("id", id, "actor", actorId));
    }

    public boolean isRegistrationInitiator(String id, String actorId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM staff_profiles WHERE id = :id AND submitted_by = :actor)",
                Map.of("id", id, "actor", actorId), Boolean.class));
    }

    public Optional<StaffProfileRecord> findProfileById(String id) {
        List<StaffProfileRecord> result = jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, user_id, name, phone, id_doc, staff_level_id,
                       registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, is_sales_position, employment_status, company_id, registration_source
                  FROM staff_profiles
                 WHERE id = :id
                   AND delete_status <> 'approved'
                """, Map.of("id", id), this::mapProfile);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public List<StaffProfileRecord> listProfiles(String merchantScope) {
        if (merchantScope == null) {
            return jdbcTemplate.query("""
                    SELECT id, project_id, merchant_id, user_id, name, phone, id_doc, staff_level_id,
                           registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at,
                           archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, is_sales_position, employment_status, company_id, registration_source
                      FROM staff_profiles
                     WHERE delete_status <> 'approved'
                     ORDER BY created_at DESC, id
                    """, this::mapProfile);
        }
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, user_id, name, phone, id_doc, staff_level_id,
                       registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, is_sales_position, employment_status, company_id, registration_source
                  FROM staff_profiles
                 WHERE delete_status <> 'approved'
                   AND merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope), this::mapProfile);
    }

    public List<StaffProfileRecord> listProfiles(String merchantScope, boolean includeArchived) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, project_id, merchant_id, user_id, name, phone, id_doc, staff_level_id,
                       registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, is_sales_position, employment_status, company_id, registration_source
                  FROM staff_profiles
                 WHERE delete_status <> 'approved'
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (merchantScope != null) {
            sql.append(" AND merchant_id = :merchant_id");
            params.addValue("merchant_id", merchantScope);
        }
        if (!includeArchived) {
            sql.append(" AND archived_at IS NULL");
        }
        sql.append(" ORDER BY created_at DESC, id");
        return jdbcTemplate.query(sql.toString(), params, this::mapProfile);
    }

    public long countProfileReferences(String id) {
        // 子表 credentials/notices 走 CASCADE 不算活引用；活引用=该员工 user_id 仍挂考勤/请假/职级薪资记录。
        Long count = jdbcTemplate.queryForObject("""
                SELECT (SELECT COUNT(1) FROM staff_attendance_records a
                          JOIN staff_profiles p ON p.user_id = a.user_id WHERE p.id = :id)
                     + (SELECT COUNT(1) FROM staff_leave_requests l
                          JOIN staff_profiles p ON p.user_id = l.user_id WHERE p.id = :id)
                     + (SELECT COUNT(1) FROM staff_level_assignments la
                          JOIN staff_profiles p ON p.user_id = la.user_id WHERE p.id = :id)
                     + (SELECT COUNT(1) FROM staff_shift_schedules sh
                          JOIN staff_profiles p ON p.user_id = sh.user_id
                         WHERE p.id = :id AND sh.end_at > now())
                     + (SELECT COUNT(1) FROM staff_locker_records WHERE staff_profile_id=:id AND retrieved_at IS NULL)
                """, Map.of("id", id), Long.class);
        return count == null ? 0L : count;
    }

    public void deleteProfile(String id) {
        jdbcTemplate.update("UPDATE staff_profiles SET delete_status = 'approved', updated_at = now() WHERE id = :id", Map.of("id", id));
    }

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<StaffProfileRecord> listProfilesByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, user_id, name, phone, id_doc, staff_level_id,
                       registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, is_sales_position, employment_status, company_id, registration_source
                  FROM staff_profiles
                 WHERE delete_status <> 'approved'
                   AND project_id = :project_id
                 ORDER BY created_at DESC, id
                """, Map.of("project_id", projectId), this::mapProfile);
    }

    // ---------------------------------------------------------- staff_device_credentials

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

    public Optional<StaffDeviceCredentialRecord> findCredentialById(String id) {
        List<StaffDeviceCredentialRecord> result = jdbcTemplate.query(
                credentialSelect() + " WHERE id = :id",
                Map.of("id", id), this::mapCredential);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public boolean hasCredentialHistory(String staffProfileId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM staff_device_credentials WHERE staff_profile_id = :id)",
                Map.of("id", staffProfileId), Boolean.class));
    }

    /** U05-003：同一私人手机指纹只能绑定一名员工（生效或待审凭证）。 */
    public boolean isFingerprintBound(String fingerprint) {
        Integer n = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM staff_device_credentials
                 WHERE lower(device_fingerprint_hash) = lower(:fp) AND bind_status IN ('active', 'pending_review')
                """, Map.of("fp", fingerprint), Integer.class);
        return n != null && n > 0;
    }

    public Optional<StaffDeviceCredentialRecord> findActiveCredential(String staffProfileId) {
        List<StaffDeviceCredentialRecord> result = jdbcTemplate.query(
                credentialSelect() + " WHERE staff_profile_id = :staff_profile_id AND bind_status = 'active'",
                Map.of("staff_profile_id", staffProfileId), this::mapCredential);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
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

    public Optional<EmployeeNoticeRecord> findNoticeById(String id) {
        List<EmployeeNoticeRecord> result = jdbcTemplate.query("""
                SELECT id, staff_profile_id, notice_type, title, body, consent_required, consent_status,
                       consented_at, created_by, created_at, updated_at
                  FROM employee_notices
                 WHERE id = :id
                """, Map.of("id", id), this::mapNotice);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public List<EmployeeNoticeRecord> listNoticesForUser(String userId) {
        return jdbcTemplate.query("""
                SELECT n.* FROM employee_notices n
                  JOIN staff_profiles p ON p.id = n.staff_profile_id
                 WHERE p.user_id = :user_id AND p.delete_status <> 'approved'
                 ORDER BY n.created_at DESC, n.id
                """, Map.of("user_id", userId), this::mapNotice);
    }

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

    public java.util.Optional<StaffProfileRecord> findProfileByUserId(String userId) {
        java.util.List<StaffProfileRecord> result = jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, user_id, name, phone, id_doc, staff_level_id,
                       registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, is_sales_position, employment_status, company_id, registration_source
                  FROM staff_profiles
                 WHERE user_id = :user_id
                   AND delete_status <> 'approved'
                 ORDER BY created_at DESC
                 LIMIT 1
                """, java.util.Map.of("user_id", userId), this::mapProfile);
        return result.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(result.get(0));
    }

    public java.util.List<StaffCommissionLineRecord> listCommissionLines(String userId, String month) {
        // 酒吧提成按开台或下单时点所属营业日定月（ENUM:187，同工资单）；没有时用不会命中的空串占位（IN () 不合法）。
        List<String> commissions = businessDays.barCommissionIdsInMonth(java.time.YearMonth.parse(month), userId);
        return jdbcTemplate.query("""
                SELECT source_type, source_id, amount_cent, note, created_at
                  FROM staff_commission_ledger
                 WHERE user_id = :user_id AND month = :month
                UNION ALL
                SELECT 'bar_table' AS source_type, bill_id AS source_id,
                       commission_amount_cent AS amount_cent, CAST(NULL AS VARCHAR) AS note, created_at
                  FROM bar_commission_calculations
                 WHERE id IN (:commission_ids)
                   AND status NOT IN ('void', 'cancelled')
                 ORDER BY created_at DESC
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("month", month)
                .addValue("commission_ids", commissions.isEmpty() ? List.of("") : commissions),
                (rs, n) -> new StaffCommissionLineRecord(
                        rs.getString("source_type"), rs.getString("source_id"),
                        rs.getLong("amount_cent"), rs.getString("note"),
                        rs.getTimestamp("created_at").toInstant()));
    }

    private StaffProfileRecord mapProfile(ResultSet rs, int rowNumber) throws SQLException {
        return new StaffProfileRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("merchant_id"),
                rs.getString("user_id"),
                piiCipher.decrypt(rs.getString("name")),
                piiCipher.decrypt(rs.getString("phone")),
                piiCipher.decrypt(rs.getString("id_doc")),
                rs.getString("staff_level_id"),
                rs.getString("registration_status"),
                rs.getString("approved_by"),
                nullableInstant(rs, "approved_at"),
                rs.getString("reject_reason"),
                rs.getString("status"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"),
                nullableInstant(rs, "archived_at"),
                rs.getString("archived_by"),
                rs.getString("delete_status"),
                rs.getString("delete_requested_by"),
                nullableInstant(rs, "delete_requested_at"),
                rs.getString("delete_reason"),
                rs.getBoolean("is_sales_position"), rs.getString("employment_status"), rs.getString("company_id"), rs.getString("registration_source")
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

    public void setStaffProfileArchiveReason(String id, String reason) {
        jdbcTemplate.update("UPDATE staff_profiles SET archive_reason = :reason WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
    }
}
