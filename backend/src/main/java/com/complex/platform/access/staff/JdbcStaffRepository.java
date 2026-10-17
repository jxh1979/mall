package com.complex.platform.access.staff;

import com.complex.platform.common.api.ApiException;

// 文件版本：V0.15
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.LocalZone;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// ponytail: 仓储直用实现类,无接口(one impl)。
@Repository
public class JdbcStaffRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final com.complex.platform.common.crypto.PiiCipher piiCipher;

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
        jdbcTemplate.update("""
                INSERT INTO staff_payroll_adjustments (id, user_id, month, adj_type, amount_cent, reason, created_by, created_at, project_id)
                VALUES (:id, :user_id, :month, :adj_type, :amount, :reason, :created_by, :now, """
                + STAFF_PROJECT_SUBSELECT + """
                )
                ON CONFLICT DO NOTHING
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("user_id", userId).addValue("month", month)
                .addValue("adj_type", adjType).addValue("amount", amountCent).addValue("reason", reason)
                .addValue("created_by", createdBy).addValue("now", java.sql.Timestamp.from(now)));
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
                (rs, n) -> rs.getString("project_id")).stream().findFirst();
    }

    public int closePayrollMonth(String month, String closedBy, java.time.Instant now, String projectScope) {
        return closePayrollMonth(month, closedBy, now, projectScope, null);
    }

    public int closePayrollMonth(String month, String closedBy, java.time.Instant now, String projectScope,
            String merchantScope) {
        return closePayrollMonth(month, closedBy, now, projectScope, merchantScope, null, null);
    }

    public int closePayrollMonth(String month, String closedBy, java.time.Instant now, String projectScope,
            String merchantScope, String companyScope, java.util.Collection<String> projectIds) {
        if (companyScope == null && projectIds != null && projectIds.isEmpty()) {
            return 0;
        }
        // 月结重算:固定档案 + 台账提成 + 酒吧提成(bar_commission_calculations 按月/非作废) + 奖 - 罚;
        // 有任一薪资要素的员工都出单;未发放月幂等覆盖(uq user+month)。
        // 资金安全①:已发放(status='paid')的行既不重算 INSERT(WHERE NOT EXISTS 排除),
        //   也不被 ON CONFLICT 覆盖(DO UPDATE WHERE status <> 'paid' 兜底并发)。重结新单显式置 'closed'。
        return jdbcTemplate.update("""
                INSERT INTO staff_payroll_statements (id, user_id, month, base_cent, commission_cent,
                        bonus_cent, penalty_cent, leave_deduction_cent, tax_cent, total_cent, status, closed_by, closed_at, project_id, penalty_cap_bps)
                SELECT 'pay_' || gen_random_uuid(), u.user_id, :month,
                       COALESCE(b.monthly_salary_cent, 0),
                       COALESCE(c.total, 0) + COALESCE(bc.total, 0),
                       COALESCE(adj.bonus, 0),
                       capped.penalty,
                       COALESCE(lv.deduction, 0),
                       -- H批:对当月服务劳务(service_order 台账)按劳务报酬预扣预缴法定税表代扣个税(单位:分)。
                       COALESCE(tax.tax_cent, 0),
                       GREATEST(0, COALESCE(b.monthly_salary_cent, 0) + COALESCE(c.total, 0) + COALESCE(bc.total, 0)
                           + COALESCE(adj.bonus, 0) - capped.penalty - COALESCE(lv.deduction, 0) - COALESCE(tax.tax_cent, 0)),
                       'closed', :closed_by, :now, sp.project_id, cap.value
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
                  -- 个税可配置化(V217):劳务报酬预扣税表落 payroll_tax_brackets,按毛收入(svc,分)落档,
                  -- 每档公式 GREATEST(0,(svc*base_bps/10000 - flat_deduction)*rate_bps/10000 - quick_deduction)。
                  -- 整除链与原 CASE WHEN 逐分等价;税法参数变更改配置表不改代码。svc<=0 无 service_order 劳务→无档匹配→tax 0。
                  -- 🔴 R-7:个税整除链【刻意保持截断】(2026-07-28 用户拍板)——与税局申报口径对齐,改四舍五入会使代扣申报对不上;
                  --    与 DividendMath 的代扣个税同口径。全局「金额四舍五入到分」(D15.1)对本式【不适用】。
                  LEFT JOIN (SELECT sb.user_id,
                                    GREATEST(0,
                                        (sb.svc * br.income_base_bps / 10000 - br.flat_deduction_cent)
                                            * br.rate_bps / 10000 - br.quick_deduction_cent) AS tax_cent
                               FROM (SELECT user_id, SUM(amount_cent) AS svc FROM staff_commission_ledger
                                      WHERE month = :month AND source_type = 'service_order'
                                      GROUP BY user_id) sb
                               JOIN payroll_tax_brackets br
                                 ON br.category = 'labor_remuneration'
                                AND sb.svc >= br.income_floor_cent
                                AND (br.income_ceiling_cent IS NULL OR sb.svc <= br.income_ceiling_cent)) tax
                         ON tax.user_id = u.user_id
                  -- BSM-1.18.2：普通处罚合计按「总薪资」比例封顶。HTML 总薪资＝基础工资+各项补贴+绩效工资+奖金；
                  -- 现行 schema 只有固定工资与奖金两列，先取全这两项，补贴/绩效列落地后并入分母（U05-004）；提成不入分母，事假另算。
                  CROSS JOIN LATERAL (
                      SELECT COALESCE((SELECT locked.penalty_cap_bps FROM staff_payroll_statements locked
                                       WHERE locked.user_id=u.user_id AND locked.month=:month),
                                      (SELECT numeric_value FROM staff_policy_settings
                                       WHERE policy_key='payroll_penalty_cap_bps' AND scope_id='global'),2000) AS value
                  ) cap
                  CROSS JOIN LATERAL (
                      SELECT LEAST(COALESCE(adj.penalty, 0),
                          ROUND((COALESCE(b.monthly_salary_cent, 0)::numeric + COALESCE(adj.bonus, 0))
                              * cap.value
                              / 10000)) AS penalty
                  ) capped
                  -- 仅批准的事假扣薪；按当月结束的排班工时计算实际请假比例，分钟/小时均不先截断。
                  -- BSM L1450：从「当期应发薪资」扣减——基数＝固定工资+台账提成+酒吧提成+奖金（个税与处罚另算，U05-002）。
                  LEFT JOIN LATERAL (
                      SELECT ROUND((COALESCE(b.monthly_salary_cent, 0) + COALESCE(c.total, 0) + COALESCE(bc.total, 0)
                              + COALESCE(adj.bonus, 0))::numeric
                          * SUM(LEAST(EXTRACT(EPOCH FROM (sh.end_at - sh.start_at)), COALESCE(lp.seconds, 0)))
                          / NULLIF(SUM(EXTRACT(EPOCH FROM (sh.end_at - sh.start_at))), 0)) AS deduction
                        FROM staff_shift_schedules sh
                        LEFT JOIN LATERAL (
                            SELECT SUM(GREATEST(0, EXTRACT(EPOCH FROM (
                                LEAST(sh.end_at, (lr.end_date + 1)::timestamp AT TIME ZONE :local_zone)
                                - GREATEST(sh.start_at, lr.start_date::timestamp AT TIME ZONE :local_zone))))) AS seconds
                              FROM staff_leave_requests lr
                             WHERE lr.user_id = sh.user_id AND lr.status = 'approved' AND lr.leave_type = 'personal'
                        ) lp ON true
                       WHERE sh.user_id = u.user_id
                         AND (sh.end_at AT TIME ZONE :local_zone)::date BETWEEN :first AND :last
                  ) lv ON true
                  LEFT JOIN (SELECT DISTINCT ON (user_id) user_id, project_id, merchant_id, company_id FROM staff_profiles
                              ORDER BY user_id, created_at DESC) sp
                         ON sp.user_id = u.user_id
                 -- P0 项目级月结(V214):平台口径(scope=null)重算全量;项目口径仅本综合体员工,
                 -- 绝不 INSERT/覆盖其他项目的薪资单。无档案员工(sp.project_id 为 NULL)只在平台口径纳入。
                 WHERE (:project_scope::text IS NULL OR sp.project_id = :project_scope)
                   AND (:merchant_scope::text IS NULL OR sp.merchant_id = :merchant_scope)
                   AND (:all_staff OR sp.company_id = :company_scope::text OR sp.project_id IN (:project_ids))
                   -- 资金安全①:已发放的员工月份不参与重算(不 INSERT 冲突、不覆盖)。
                   AND NOT EXISTS (SELECT 1 FROM staff_payroll_statements paid
                                    WHERE paid.user_id = u.user_id AND paid.month = :month
                                      AND paid.status = 'paid')
                ON CONFLICT (user_id, month) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    base_cent = EXCLUDED.base_cent,
                    commission_cent = EXCLUDED.commission_cent,
                    bonus_cent = EXCLUDED.bonus_cent,
                    penalty_cent = EXCLUDED.penalty_cent,
                    leave_deduction_cent = EXCLUDED.leave_deduction_cent,
                    tax_cent = EXCLUDED.tax_cent,
                    total_cent = EXCLUDED.total_cent,
                    closed_by = EXCLUDED.closed_by,
                    closed_at = EXCLUDED.closed_at
                  WHERE staff_payroll_statements.status <> 'paid'
                """, withStaffScope(new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("month", month).addValue("closed_by", closedBy)
                .addValue("now", java.sql.Timestamp.from(now))
                .addValue("first", java.sql.Date.valueOf(java.time.YearMonth.parse(month).atDay(1)))
                .addValue("last", java.sql.Date.valueOf(java.time.YearMonth.parse(month).atEndOfMonth()))
                .addValue("project_scope", projectScope).addValue("merchant_scope", merchantScope)
                .addValue("local_zone", LocalZone.ID.getId()), companyScope, projectIds));
    }

    public int markPayrollPaid(String payoutId, String userId, String month, String payMethod,
            String payRef, String paidBy, java.time.Instant now) {
        // 资金安全①:发放动作。仅 status='closed' 的已定格单可发放;落发放台账 + 置 paid。
        // 幂等:staff_payroll_payouts uq(user,month) 防重复发放;状态置换只对 closed 生效。
        var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("payout_id", payoutId).addValue("user_id", userId).addValue("month", month)
                .addValue("pay_method", payMethod).addValue("pay_ref", payRef).addValue("paid_by", paidBy)
                .addValue("now", java.sql.Timestamp.from(now));
        int inserted = jdbcTemplate.update("""
                INSERT INTO staff_payroll_payouts (id, statement_id, user_id, month, paid_total_cent,
                        pay_method, pay_ref, paid_by, paid_at, project_id)
                SELECT :payout_id, s.id, s.user_id, s.month, s.total_cent,
                       :pay_method, :pay_ref, :paid_by, :now, s.project_id
                  FROM staff_payroll_statements s
                 WHERE s.user_id = :user_id AND s.month = :month AND s.status = 'closed'
                ON CONFLICT (user_id, month) DO NOTHING
                """, params);
        jdbcTemplate.update("""
                UPDATE staff_payroll_statements SET status = 'paid'
                 WHERE user_id = :user_id AND month = :month AND status = 'closed'
                """, params);
        return inserted;
    }

    public java.util.List<StaffPayrollStatementRecord> listPayrollStatements(String month, String userId,
            String projectScope) {
        return jdbcTemplate.query("""
                SELECT id, user_id, month, base_cent, commission_cent, bonus_cent, penalty_cent,
                       leave_deduction_cent, tax_cent, total_cent, closed_by, closed_at
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
                rs.getString("closed_by"), rs.getTimestamp("closed_at").toInstant());
    }

    private static final String STAFF_MERCHANT_FILTER =
            " AND (:merchant_scope::text IS NULL OR user_id IN"
            + " (SELECT user_id FROM staff_profiles WHERE merchant_id = :merchant_scope"
            + " AND delete_status <> 'approved'))";

    /** M738：人事列表与月结按「本公司 OR 登记项目 IN 集合」过滤（写法同 JdbcAnalyticsRepository 的 withProjects）。两者都 null＝不按公司、
     *  登记项目过滤（平台管理员、商户账号）；登记项目集合为空时用占位（空 IN () 是语法错误，project_id 不收空串），
     *  「无公司且空集合」调用前已提前返回。谓词写作 (:all_staff OR 公司条件 OR 登记项目 IN (:project_ids))。 */
    private static MapSqlParameterSource withStaffScope(MapSqlParameterSource params, String companyScope,
            java.util.Collection<String> projectIds) {
        return params.addValue("all_staff", companyScope == null && projectIds == null)
                .addValue("company_scope", companyScope)
                .addValue("project_ids", projectIds == null || projectIds.isEmpty() ? List.of("") : projectIds);
    }

    public com.complex.platform.common.api.PageResponse<StaffLeaveRequestRecord> listLeavesPaged(
            String status, String userId, String projectScope, int page, int size) {
        return listLeavesPaged(status, userId, projectScope, null, page, size);
    }

    // ---------- AUD-005 真分页: SQL COUNT + LIMIT/OFFSET, 真实 total, 稳定排序(id 收敛), 同 scope 谓词 ----------
    public com.complex.platform.common.api.PageResponse<StaffShiftRecord> listShiftsPaged(
            java.time.LocalDate workDate, String userId, String projectScope, String merchantScope, int page, int size) {
        return listShiftsPaged(workDate, userId, projectScope, merchantScope, null, null, page, size);
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
                "SELECT id, user_id, work_date, start_at, end_at, point_space_ids, note, created_by, created_at, updated_at"
                        + " FROM staff_shift_schedules " + where
                        + " ORDER BY work_date DESC, start_at, user_id, id LIMIT :limit OFFSET :offset",
                params, this::mapShift);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    public com.complex.platform.common.api.PageResponse<StaffAttendanceRecord> listAttendanceOnPaged(
            java.time.LocalDate date, String projectScope, String merchantScope, int page, int size) {
        return listAttendanceOnPaged(date, projectScope, merchantScope, null, null, page, size);
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

    public com.complex.platform.common.api.PageResponse<StaffPayrollStatementRecord> listPayrollStatementsPaged(
            String month, String userId, String projectScope, String merchantScope, int page, int size) {
        return listPayrollStatementsPaged(month, userId, projectScope, merchantScope, null, null, page, size);
    }

    public com.complex.platform.common.api.PageResponse<StaffPayrollStatementRecord> listPayrollStatementsPaged(
            String month, String userId, String projectScope, String merchantScope, String companyScope,
            java.util.Collection<String> projectIds, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 1000 : Math.min(size, 1000);
        if (companyScope == null && projectIds != null && projectIds.isEmpty()) {
            return new com.complex.platform.common.api.PageResponse<>(List.of(), safePage, safeSize, 0L);
        }
        String where = "WHERE month = :month AND (:user_id::text IS NULL OR user_id = :user_id)"
                + "   AND (:project_scope::text IS NULL OR project_id = :project_scope)"
                + STAFF_MERCHANT_FILTER
                + " AND (:all_staff OR user_id IN (SELECT p.user_id FROM staff_profiles p WHERE p.company_id = :company_scope::text OR p.project_id IN (:project_ids)))";
        org.springframework.jdbc.core.namedparam.MapSqlParameterSource params = withStaffScope(
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                        .addValue("month", month).addValue("user_id", userId).addValue("project_scope", projectScope).addValue("merchant_scope", merchantScope), companyScope, projectIds);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_payroll_statements " + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        java.util.List<StaffPayrollStatementRecord> items = jdbcTemplate.query(
                "SELECT id, user_id, month, base_cent, commission_cent, bonus_cent, penalty_cent, leave_deduction_cent, "
                        + "tax_cent, total_cent, closed_by, closed_at FROM staff_payroll_statements " + where
                        + " ORDER BY user_id, id LIMIT :limit OFFSET :offset",
                params, this::mapPayrollStatement);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    public com.complex.platform.common.api.PageResponse<StaffLeaveRequestRecord> listLeavesPaged(
            String status, String userId, String projectScope, String merchantScope, int page, int size) {
        return listLeavesPaged(status, userId, projectScope, merchantScope, null, null, page, size);
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
                        + "created_at, updated_at FROM staff_leave_requests " + where
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

    public java.util.Optional<StaffLeaveRequestRecord> findLeaveById(String id) {
        return jdbcTemplate.query("""
                SELECT id, user_id, leave_type, start_date, end_date, reason, status,
                       reviewed_by, review_note, created_at, updated_at
                  FROM staff_leave_requests WHERE id = :id
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource().addValue("id", id),
                this::mapLeave).stream().findFirst();
    }

    public java.util.List<StaffLeaveRequestRecord> listLeaves(String status, String userId, String projectScope) {
        return jdbcTemplate.query("""
                SELECT id, user_id, leave_type, start_date, end_date, reason, status,
                       reviewed_by, review_note, created_at, updated_at
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
        // 两区间 [s1,e1]/[s2,e2] 重叠 <=> s1<=e2 && s2<=e1;只算未撤销(pending/approved)。
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM staff_leave_requests
                               WHERE user_id = :user_id AND status IN ('pending','approved')
                                 AND start_date <= :end AND end_date >= :start)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("start", start).addValue("end", end), Boolean.class));
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
                INSERT INTO staff_shift_covers(id, shift_id, original_user_id, cover_user_id, arranged_by, reason, created_at)
                VALUES (:id, :shift, :original, :cover, :by, :reason, :at)
                """, new MapSqlParameterSource().addValue("id", cover.id()).addValue("shift", cover.shiftId())
                .addValue("original", cover.originalUserId()).addValue("cover", cover.coverUserId()).addValue("by", cover.arrangedBy())
                .addValue("reason", cover.reason()).addValue("at", java.sql.Timestamp.from(cover.createdAt())));
        return cover;
    }

    public List<StaffShiftCoverRecord> listShiftCovers(String shiftId) {
        return jdbcTemplate.query("""
                SELECT * FROM staff_shift_covers WHERE shift_id=:shift ORDER BY created_at, id
                """, Map.of("shift", shiftId), (rs, n) -> new StaffShiftCoverRecord(rs.getString("id"), rs.getString("shift_id"),
                rs.getString("original_user_id"), rs.getString("cover_user_id"), rs.getString("arranged_by"), rs.getString("reason"),
                rs.getTimestamp("created_at").toInstant()));
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
        var now = java.time.Instant.now();
        if (!after.startAt().isAfter(now) || (before != null && !before.startAt().isAfter(now))) {
            throw com.complex.platform.common.api.ApiException.conflict("SHIFT_STARTED", "班次已经开始，本次修改不能发布");
        }
        var params = new MapSqlParameterSource().addValue("id", after.id()).addValue("user_id", after.userId())
                .addValue("work_date", after.workDate()).addValue("start", java.sql.Timestamp.from(after.startAt()))
                .addValue("end", java.sql.Timestamp.from(after.endAt())).addValue("points", pointsJson(after.pointSpaceIds()))
                .addValue("note", after.note()).addValue("actor", after.createdBy()).addValue("now", java.sql.Timestamp.from(now));
        if (before == null) {
            jdbcTemplate.update("""
                    INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,note,created_by,created_at,updated_at,project_id)
                    VALUES(:id,:user_id,:work_date,:start,:end,CAST(:points AS jsonb),:note,:actor,:now,:now,
                    """ + STAFF_PROJECT_SUBSELECT + ")",params);
        } else {
            params.addValue("previous_updated", java.sql.Timestamp.from(before.updatedAt()));
            int updated = jdbcTemplate.update("""
                    UPDATE staff_shift_schedules SET work_date=:work_date,start_at=:start,end_at=:end,
                           point_space_ids=CAST(:points AS jsonb),note=:note,updated_at=:now
                     WHERE id=:id AND updated_at=:previous_updated AND start_at>:now
                    """,params);
            if (updated != 1) throw com.complex.platform.common.api.ApiException.conflict("SHIFT_CHANGED", "原排班已变更或已开始，请核对后重新申请");
        }
        jdbcTemplate.update("UPDATE staff_shift_revisions SET applied_at=:now WHERE id=:id AND applied_at IS NULL",
                Map.of("id", revision.id(),"now",java.sql.Timestamp.from(now)));
    }

    public StaffShiftRecord upsertShift(StaffShiftRecord shift) {
        jdbcTemplate.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at,
                        point_space_ids, note, created_by, created_at, updated_at, project_id)
                VALUES (:id, :user_id, :work_date, :start_at, :end_at, :points::jsonb, :note, :created_by, :now, :now, """
                + STAFF_PROJECT_SUBSELECT + """
                )
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

    public java.util.List<StaffShiftRecord> listShifts(java.time.LocalDate workDate, String userId,
            String projectScope) {
        return jdbcTemplate.query("""
                SELECT id, user_id, work_date, start_at, end_at, point_space_ids, note, created_by, created_at, updated_at
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
                SELECT id, user_id, work_date, start_at, end_at, point_space_ids, note, created_by, created_at, updated_at
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
                SELECT id, user_id, work_date, start_at, end_at, point_space_ids, note, created_by, created_at, updated_at
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
        return jdbcTemplate.query("""
                SELECT source_type, source_id, amount_cent, note, created_at
                  FROM staff_commission_ledger
                 WHERE user_id = :user_id AND month = :month
                UNION ALL
                SELECT 'bar_table' AS source_type, bill_id AS source_id,
                       commission_amount_cent AS amount_cent, CAST(NULL AS VARCHAR) AS note, created_at
                  FROM bar_commission_calculations
                 WHERE sales_staff_id = :user_id
                   AND to_char(created_at, 'YYYY-MM') = :month
                   AND status NOT IN ('void', 'cancelled')
                 ORDER BY created_at DESC
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("user_id", userId).addValue("month", month),
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
