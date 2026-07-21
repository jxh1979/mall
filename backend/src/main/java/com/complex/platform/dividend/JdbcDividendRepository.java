package com.complex.platform.dividend;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.api.PageResponse;
import com.complex.platform.dividend.DividendRecords.DividendPlanRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementLineRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 持股分红 JDBC 仓储。计算单头只允许状态机/审批留痕列 UPDATE（金额列冻结）；
 * 明细行仅 INSERT（V140 append-only 触发器兜底）。三来源取数谓词见各方法注释。
 */
@Repository
class JdbcDividendRepository implements DividendRepository {

    private static final String PLAN_COLS =
            "id, merchant_id, principal_user_id, principal_name, platform_share_bps, tax_rate_bps, period_type, "
            + "revenue_domains, include_inventory_cost, expense_types, effective_from, status, note, updated_by, "
            + "created_at, updated_at, "
            + "loss_carryforward_enabled, arrears_offset_enabled, payout_floor_enabled, payout_floor_bps, "
            + "payout_floor_fixed_cent, reserve_release_mode, reserve_release_delay_periods, principal_type";

    private static final String STMT_COLS =
            "id, merchant_id, plan_id, kind, period_label, period_start, period_end, principal_user_id, "
            + "platform_share_bps, tax_rate_bps, revenue_domains, include_inventory_cost, expense_types, "
            + "revenue_cent, cost_cent, expense_cent, net_profit_cent, distributable_cent, platform_share_cent, "
            + "principal_gross_cent, tax_cent, principal_net_cent, no_cost_basis_count, status, reject_reason, "
            + "ar_ap_item_id, computed_by, submitted_by, submitted_at, approved_by, approved_at, paid_by, paid_at, "
            + "created_at, updated_at, "
            + "loss_carryforward_enabled, loss_carryforward_in_cent, loss_carryforward_applied_cent, "
            + "loss_carryforward_after_cent, arrears_offset_cent, reserve_withheld_cent, payout_cent, "
            + "principal_type, custom_deduction_cent";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcDividendRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String joinCsv(List<String> values) {
        return values == null ? "" : String.join(",", values);
    }

    private static LocalDate nullableDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    // ── 方案配置 ──────────────────────────────────────────────

    @Override
    public List<DividendPlanRecord> listPlans(String merchantId) {
        return jdbcTemplate.query(
                "SELECT " + PLAN_COLS + " FROM dividend_plans"
                        + " WHERE (:merchant_id IS NULL OR merchant_id = :merchant_id)"
                        + " ORDER BY created_at DESC, id",
                new MapSqlParameterSource().addValue("merchant_id", merchantId, Types.VARCHAR),
                this::mapPlan);
    }

    @Override
    public Optional<DividendPlanRecord> findPlanById(String id) {
        return jdbcTemplate.query("SELECT " + PLAN_COLS + " FROM dividend_plans WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapPlan).stream().findFirst();
    }

    @Override
    public Optional<DividendPlanRecord> findActivePlanByMerchant(String merchantId) {
        return jdbcTemplate.query(
                "SELECT " + PLAN_COLS + " FROM dividend_plans WHERE merchant_id = :merchant_id AND status = 'active'",
                new MapSqlParameterSource("merchant_id", merchantId), this::mapPlan).stream().findFirst();
    }

    @Override
    public List<DividendPlanRecord> listActivePlans() {
        return jdbcTemplate.query(
                "SELECT " + PLAN_COLS + " FROM dividend_plans WHERE status = 'active' ORDER BY merchant_id, id",
                new MapSqlParameterSource(), this::mapPlan);
    }

    @Override
    public boolean insertPlan(DividendPlanRecord plan) {
        try {
            return jdbcTemplate.update("""
                    INSERT INTO dividend_plans (
                        id, merchant_id, principal_user_id, principal_name, platform_share_bps, tax_rate_bps,
                        period_type, revenue_domains, include_inventory_cost, expense_types, effective_from,
                        status, note, updated_by, created_at, updated_at,
                        loss_carryforward_enabled, arrears_offset_enabled, payout_floor_enabled,
                        payout_floor_bps, payout_floor_fixed_cent,
                        reserve_release_mode, reserve_release_delay_periods, principal_type)
                    VALUES (:id, :merchant_id, :principal_user_id, :principal_name, :platform_share_bps,
                            :tax_rate_bps, :period_type, :revenue_domains, :include_inventory_cost, :expense_types,
                            :effective_from, :status, :note, :updated_by, :created_at, :updated_at,
                            :loss_carryforward_enabled, :arrears_offset_enabled, :payout_floor_enabled,
                            :payout_floor_bps, :payout_floor_fixed_cent,
                            :reserve_release_mode, :reserve_release_delay_periods, :principal_type)
                    ON CONFLICT (id) DO NOTHING
                    """, planParams(plan)) > 0;
        } catch (DuplicateKeyException conflict) {
            // uq_dividend_plans_active_merchant：同商户已有生效中方案。
            return false;
        }
    }

    @Override
    public boolean updatePlan(DividendPlanRecord plan) {
        try {
            return doUpdatePlan(plan) > 0;
        } catch (DuplicateKeyException conflict) {
            // uq_dividend_plans_active_merchant：ended 改回 active 时同商户已有另一生效方案。
            return false;
        }
    }

    private int doUpdatePlan(DividendPlanRecord plan) {
        return jdbcTemplate.update("""
                UPDATE dividend_plans SET
                    principal_user_id = :principal_user_id,
                    principal_name = :principal_name,
                    platform_share_bps = :platform_share_bps,
                    tax_rate_bps = :tax_rate_bps,
                    period_type = :period_type,
                    revenue_domains = :revenue_domains,
                    include_inventory_cost = :include_inventory_cost,
                    expense_types = :expense_types,
                    effective_from = :effective_from,
                    status = :status,
                    note = :note,
                    updated_by = :updated_by,
                    updated_at = :updated_at,
                    loss_carryforward_enabled = :loss_carryforward_enabled,
                    arrears_offset_enabled = :arrears_offset_enabled,
                    payout_floor_enabled = :payout_floor_enabled,
                    payout_floor_bps = :payout_floor_bps,
                    payout_floor_fixed_cent = :payout_floor_fixed_cent,
                    reserve_release_mode = :reserve_release_mode,
                    reserve_release_delay_periods = :reserve_release_delay_periods,
                    principal_type = :principal_type
                 WHERE id = :id
                """, planParams(plan));
    }

    private static MapSqlParameterSource planParams(DividendPlanRecord plan) {
        return new MapSqlParameterSource()
                .addValue("id", plan.id())
                .addValue("merchant_id", plan.merchantId())
                .addValue("principal_user_id", plan.principalUserId())
                .addValue("principal_name", plan.principalName(), Types.VARCHAR)
                .addValue("platform_share_bps", plan.platformShareBps())
                .addValue("tax_rate_bps", plan.taxRateBps())
                .addValue("period_type", plan.periodType())
                .addValue("revenue_domains", joinCsv(plan.revenueDomains()))
                .addValue("include_inventory_cost", plan.includeInventoryCost())
                .addValue("expense_types", joinCsv(plan.expenseTypes()))
                .addValue("effective_from", plan.effectiveFrom(), Types.DATE)
                .addValue("status", plan.status())
                .addValue("note", plan.note(), Types.VARCHAR)
                .addValue("updated_by", plan.updatedBy(), Types.VARCHAR)
                .addValue("created_at", timestampWithTimeZone(plan.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(plan.updatedAt()))
                .addValue("loss_carryforward_enabled", plan.lossCarryforwardEnabled())
                .addValue("arrears_offset_enabled", plan.arrearsOffsetEnabled())
                .addValue("payout_floor_enabled", plan.payoutFloorEnabled())
                .addValue("payout_floor_bps", plan.payoutFloorBps())
                .addValue("payout_floor_fixed_cent", plan.payoutFloorFixedCent())
                .addValue("reserve_release_mode", plan.reserveReleaseMode())
                .addValue("reserve_release_delay_periods", plan.reserveReleaseDelayPeriods())
                .addValue("principal_type", plan.principalType());
    }

    private DividendPlanRecord mapPlan(ResultSet rs, int rowNum) throws SQLException {
        // ponytail: 每行一次子查询装填自定义扣除项（N+1，方案数量小=每商户一张生效方案，可接受）；
        // 若方案量级增大改批量 IN 装填。
        return new DividendPlanRecord(
                rs.getString("id"),
                rs.getString("merchant_id"),
                rs.getString("principal_user_id"),
                rs.getString("principal_name"),
                rs.getInt("platform_share_bps"),
                rs.getInt("tax_rate_bps"),
                rs.getString("period_type"),
                splitCsv(rs.getString("revenue_domains")),
                rs.getBoolean("include_inventory_cost"),
                splitCsv(rs.getString("expense_types")),
                nullableDate(rs, "effective_from"),
                rs.getString("status"),
                rs.getString("note"),
                rs.getString("updated_by"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"),
                rs.getBoolean("loss_carryforward_enabled"),
                rs.getBoolean("arrears_offset_enabled"),
                rs.getBoolean("payout_floor_enabled"),
                rs.getInt("payout_floor_bps"),
                rs.getLong("payout_floor_fixed_cent"),
                rs.getString("reserve_release_mode"),
                rs.getInt("reserve_release_delay_periods"),
                rs.getString("principal_type"),
                listCustomDeductions(rs.getString("id")));
    }

    // ── V309 自定义扣除项 ──────────────────────────────────────

    @Override
    public List<DividendRecords.DividendCustomDeductionRecord> listCustomDeductions(String planId) {
        return jdbcTemplate.query("""
                SELECT id, plan_id, name, source_type, amount_cent, ratio_bps, ratio_basis, enabled, sort_order
                  FROM dividend_deduction_items
                 WHERE plan_id = :plan_id
                 ORDER BY sort_order, id
                """, new MapSqlParameterSource("plan_id", planId),
                (rs, n) -> new DividendRecords.DividendCustomDeductionRecord(
                        rs.getString("id"), rs.getString("plan_id"), rs.getString("name"),
                        rs.getString("source_type"), rs.getLong("amount_cent"), rs.getInt("ratio_bps"),
                        rs.getString("ratio_basis"), rs.getBoolean("enabled"), rs.getInt("sort_order")));
    }

    @Override
    public void replaceCustomDeductions(
            String planId, List<DividendRecords.DividendCustomDeductionRecord> items) {
        // 整表替换（先删后插）：配置类，历史单靠算式明细快照留痕，不受此重置影响。
        jdbcTemplate.update("DELETE FROM dividend_deduction_items WHERE plan_id = :plan_id",
                new MapSqlParameterSource("plan_id", planId));
        for (DividendRecords.DividendCustomDeductionRecord item : items) {
            jdbcTemplate.update("""
                    INSERT INTO dividend_deduction_items (
                        id, plan_id, name, source_type, amount_cent, ratio_bps, ratio_basis, enabled, sort_order)
                    VALUES (:id, :plan_id, :name, :source_type, :amount_cent, :ratio_bps, :ratio_basis,
                            :enabled, :sort_order)
                    """, new MapSqlParameterSource()
                    .addValue("id", item.id())
                    .addValue("plan_id", planId)
                    .addValue("name", item.name())
                    .addValue("source_type", item.sourceType())
                    .addValue("amount_cent", item.amountCent())
                    .addValue("ratio_bps", item.ratioBps())
                    .addValue("ratio_basis", item.ratioBasis())
                    .addValue("enabled", item.enabled())
                    .addValue("sort_order", item.sortOrder()));
        }
    }

    // ── 计算单 ────────────────────────────────────────────────

    private static final String STMT_FILTER_WHERE = """
             WHERE (:merchant_id IS NULL OR merchant_id = :merchant_id)
               AND (:kind IS NULL OR kind = :kind)
               AND (:period_label IS NULL OR period_label = :period_label)
               AND (:status IS NULL OR status = :status)
            """;

    @Override
    public PageResponse<DividendStatementRecord> pageStatements(
            String merchantId, String kind, String periodLabel, String status, int page, int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, Types.VARCHAR)
                .addValue("kind", kind, Types.VARCHAR)
                .addValue("period_label", periodLabel, Types.VARCHAR)
                .addValue("status", status, Types.VARCHAR)
                .addValue("limit", pageSize)
                .addValue("offset", (long) (page - 1) * pageSize);
        List<DividendStatementRecord> items = jdbcTemplate.query(
                "SELECT " + STMT_COLS + " FROM dividend_statements" + STMT_FILTER_WHERE
                        + " ORDER BY period_label DESC, merchant_id, created_at DESC LIMIT :limit OFFSET :offset",
                params, this::mapStatement);
        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM dividend_statements" + STMT_FILTER_WHERE, params, Long.class);
        return new PageResponse<>(items, page, pageSize, total == null ? 0L : total);
    }

    @Override
    public Optional<DividendStatementRecord> findStatementById(String id) {
        return jdbcTemplate.query("SELECT " + STMT_COLS + " FROM dividend_statements WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapStatement).stream().findFirst();
    }

    @Override
    public Optional<DividendStatementRecord> findActiveStatement(String merchantId, String kind, String periodLabel) {
        return jdbcTemplate.query(
                "SELECT " + STMT_COLS + " FROM dividend_statements"
                        + " WHERE merchant_id = :merchant_id AND kind = :kind AND period_label = :period_label"
                        + " AND status <> 'voided'",
                new MapSqlParameterSource()
                        .addValue("merchant_id", merchantId)
                        .addValue("kind", kind)
                        .addValue("period_label", periodLabel),
                this::mapStatement).stream().findFirst();
    }

    @Override
    public long priorLossCarryforwardCent(String merchantId, LocalDate periodStart) {
        // 该商户 period_start 之前最近一张已结正式单的结转链余额（无则 0）；draft/preview/voided 不计。
        Long balance = jdbcTemplate.query("""
                SELECT loss_carryforward_after_cent
                  FROM dividend_statements
                 WHERE merchant_id = :merchant_id AND kind = 'official'
                   AND status IN ('approved', 'paid')
                   AND period_end < :period_start
                 ORDER BY period_end DESC, created_at DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                        .addValue("merchant_id", merchantId)
                        .addValue("period_start", periodStart, Types.DATE),
                (rs, n) -> rs.getLong("loss_carryforward_after_cent")).stream().findFirst().orElse(0L);
        return balance == null ? 0L : balance;
    }

    @Override
    public void insertStatement(DividendStatementRecord s) {
        jdbcTemplate.update("""
                INSERT INTO dividend_statements (
                    id, merchant_id, plan_id, kind, period_label, period_start, period_end, principal_user_id,
                    platform_share_bps, tax_rate_bps, revenue_domains, include_inventory_cost, expense_types,
                    revenue_cent, cost_cent, expense_cent, net_profit_cent, distributable_cent, platform_share_cent,
                    principal_gross_cent, tax_cent, principal_net_cent, no_cost_basis_count, status, reject_reason,
                    ar_ap_item_id, computed_by, submitted_by, submitted_at, approved_by, approved_at, paid_by,
                    paid_at, created_at, updated_at,
                    loss_carryforward_enabled, loss_carryforward_in_cent, loss_carryforward_applied_cent,
                    loss_carryforward_after_cent, arrears_offset_cent, reserve_withheld_cent, payout_cent,
                    principal_type, custom_deduction_cent)
                VALUES (:id, :merchant_id, :plan_id, :kind, :period_label, :period_start, :period_end,
                    :principal_user_id, :platform_share_bps, :tax_rate_bps, :revenue_domains,
                    :include_inventory_cost, :expense_types, :revenue_cent, :cost_cent, :expense_cent,
                    :net_profit_cent, :distributable_cent, :platform_share_cent, :principal_gross_cent, :tax_cent,
                    :principal_net_cent, :no_cost_basis_count, :status, :reject_reason, :ar_ap_item_id,
                    :computed_by, NULL, NULL, NULL, NULL, NULL, NULL, :created_at, :updated_at,
                    :loss_carryforward_enabled, :loss_carryforward_in_cent, :loss_carryforward_applied_cent,
                    :loss_carryforward_after_cent, :arrears_offset_cent, :reserve_withheld_cent, :payout_cent,
                    :principal_type, :custom_deduction_cent)
                """, new MapSqlParameterSource()
                .addValue("id", s.id())
                .addValue("merchant_id", s.merchantId())
                .addValue("plan_id", s.planId())
                .addValue("kind", s.kind())
                .addValue("period_label", s.periodLabel())
                .addValue("period_start", s.periodStart(), Types.DATE)
                .addValue("period_end", s.periodEnd(), Types.DATE)
                .addValue("principal_user_id", s.principalUserId())
                .addValue("platform_share_bps", s.platformShareBps())
                .addValue("tax_rate_bps", s.taxRateBps())
                .addValue("revenue_domains", joinCsv(s.revenueDomains()))
                .addValue("include_inventory_cost", s.includeInventoryCost())
                .addValue("expense_types", joinCsv(s.expenseTypes()))
                .addValue("revenue_cent", s.revenueCent())
                .addValue("cost_cent", s.costCent())
                .addValue("expense_cent", s.expenseCent())
                .addValue("net_profit_cent", s.netProfitCent())
                .addValue("distributable_cent", s.distributableCent())
                .addValue("platform_share_cent", s.platformShareCent())
                .addValue("principal_gross_cent", s.principalGrossCent())
                .addValue("tax_cent", s.taxCent())
                .addValue("principal_net_cent", s.principalNetCent())
                .addValue("no_cost_basis_count", s.noCostBasisCount())
                .addValue("status", s.status())
                .addValue("reject_reason", s.rejectReason(), Types.VARCHAR)
                .addValue("ar_ap_item_id", s.arApItemId(), Types.VARCHAR)
                .addValue("computed_by", s.computedBy(), Types.VARCHAR)
                .addValue("created_at", timestampWithTimeZone(s.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(s.updatedAt()))
                .addValue("loss_carryforward_enabled", s.lossCarryforwardEnabled())
                .addValue("loss_carryforward_in_cent", s.lossCarryforwardInCent())
                .addValue("loss_carryforward_applied_cent", s.lossCarryforwardAppliedCent())
                .addValue("loss_carryforward_after_cent", s.lossCarryforwardAfterCent())
                .addValue("arrears_offset_cent", s.arrearsOffsetCent())
                .addValue("reserve_withheld_cent", s.reserveWithheldCent())
                .addValue("payout_cent", s.payoutCent())
                .addValue("principal_type", s.principalType())
                .addValue("custom_deduction_cent", s.customDeductionCent()));
    }

    @Override
    public void insertLine(DividendStatementLineRecord line) {
        jdbcTemplate.update("""
                INSERT INTO dividend_statement_lines (
                    id, statement_id, merchant_id, seq, line_type, amount_cent, detail, created_at)
                VALUES (:id, :statement_id, :merchant_id, :seq, :line_type, :amount_cent, :detail, :created_at)
                """, new MapSqlParameterSource()
                .addValue("id", line.id())
                .addValue("statement_id", line.statementId())
                .addValue("merchant_id", line.merchantId())
                .addValue("seq", line.seq())
                .addValue("line_type", line.lineType())
                .addValue("amount_cent", line.amountCent())
                .addValue("detail", line.detail(), Types.VARCHAR)
                .addValue("created_at", timestampWithTimeZone(line.createdAt())));
    }

    @Override
    public List<DividendStatementLineRecord> listLines(String statementId, String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, statement_id, merchant_id, seq, line_type, amount_cent, detail, created_at
                  FROM dividend_statement_lines
                 WHERE statement_id = :statement_id
                   AND (:merchant_id IS NULL OR merchant_id = :merchant_id)
                 ORDER BY seq
                """, new MapSqlParameterSource()
                        .addValue("statement_id", statementId)
                        .addValue("merchant_id", merchantId, Types.VARCHAR),
                (rs, n) -> new DividendStatementLineRecord(
                        rs.getString("id"), rs.getString("statement_id"), rs.getString("merchant_id"),
                        rs.getInt("seq"), rs.getString("line_type"), rs.getLong("amount_cent"),
                        rs.getString("detail"), nullableInstant(rs, "created_at")));
    }

    // ── 状态机列更新（金额列冻结：以下 UPDATE 只触状态/审批留痕列）──────────
    // C-1 资金安全：每条 UPDATE 带期望前态谓词——READ_COMMITTED 下 approve×reject 并发若无守卫，
    // 可造出「已驳回单挂着活应付」再重算重挂=同期双付。0 行由服务层抛 DIVIDEND_STATE_INVALID。

    @Override
    public int markVoided(String id, String actor, Instant now) {
        return jdbcTemplate.update("""
                UPDATE dividend_statements SET status = 'voided', voided_by = :actor, voided_at = :now,
                       updated_at = :now
                 WHERE id = :id AND status IN ('preview', 'draft', 'rejected')
                """, statusParams(id, actor, now));
    }

    @Override
    public int markSubmitted(String id, String actor, Instant now) {
        return jdbcTemplate.update("""
                UPDATE dividend_statements SET status = 'pending_review', submitted_by = :actor,
                       submitted_at = :now, updated_at = :now
                 WHERE id = :id AND status = 'draft'
                """, statusParams(id, actor, now));
    }

    @Override
    public int markApproved(String id, String actor, String arApItemId, Instant now) {
        return jdbcTemplate.update("""
                UPDATE dividend_statements SET status = 'approved', approved_by = :actor, approved_at = :now,
                       ar_ap_item_id = :ar_ap_item_id, updated_at = :now
                 WHERE id = :id AND status = 'pending_review'
                """, statusParams(id, actor, now).addValue("ar_ap_item_id", arApItemId, Types.VARCHAR));
    }

    @Override
    public int markRejected(String id, String actor, String reason, Instant now) {
        return jdbcTemplate.update("""
                UPDATE dividend_statements SET status = 'rejected', reject_reason = :reason,
                       rejected_by = :actor, rejected_at = :now, updated_at = :now
                 WHERE id = :id AND status = 'pending_review'
                """, statusParams(id, actor, now).addValue("reason", reason));
    }

    @Override
    public int markPaid(String id, String actor, Instant now) {
        return jdbcTemplate.update("""
                UPDATE dividend_statements SET status = 'paid', paid_by = :actor, paid_at = :now,
                       updated_at = :now
                 WHERE id = :id AND status = 'approved'
                """, statusParams(id, actor, now));
    }

    private static MapSqlParameterSource statusParams(String id, String actor, Instant now) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("actor", actor, Types.VARCHAR)
                .addValue("now", timestampWithTimeZone(now));
    }

    private DividendStatementRecord mapStatement(ResultSet rs, int rowNum) throws SQLException {
        return new DividendStatementRecord(
                rs.getString("id"),
                rs.getString("merchant_id"),
                rs.getString("plan_id"),
                rs.getString("kind"),
                rs.getString("period_label"),
                nullableDate(rs, "period_start"),
                nullableDate(rs, "period_end"),
                rs.getString("principal_user_id"),
                rs.getInt("platform_share_bps"),
                rs.getInt("tax_rate_bps"),
                splitCsv(rs.getString("revenue_domains")),
                rs.getBoolean("include_inventory_cost"),
                splitCsv(rs.getString("expense_types")),
                rs.getLong("revenue_cent"),
                rs.getLong("cost_cent"),
                rs.getLong("expense_cent"),
                rs.getLong("net_profit_cent"),
                rs.getLong("distributable_cent"),
                rs.getLong("platform_share_cent"),
                rs.getLong("principal_gross_cent"),
                rs.getLong("tax_cent"),
                rs.getLong("principal_net_cent"),
                rs.getLong("no_cost_basis_count"),
                rs.getString("status"),
                rs.getString("reject_reason"),
                rs.getString("ar_ap_item_id"),
                rs.getString("computed_by"),
                rs.getString("submitted_by"),
                nullableInstant(rs, "submitted_at"),
                rs.getString("approved_by"),
                nullableInstant(rs, "approved_at"),
                rs.getString("paid_by"),
                nullableInstant(rs, "paid_at"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"),
                rs.getBoolean("loss_carryforward_enabled"),
                rs.getLong("loss_carryforward_in_cent"),
                rs.getLong("loss_carryforward_applied_cent"),
                rs.getLong("loss_carryforward_after_cent"),
                rs.getLong("arrears_offset_cent"),
                rs.getLong("reserve_withheld_cent"),
                rs.getLong("payout_cent"),
                rs.getString("principal_type"),
                rs.getLong("custom_deduction_cent"));
    }

    // ── V304 发放保护线留存·释放 ──────────────────────────────

    @Override
    public boolean insertReserveRelease(DividendRecords.DividendReserveReleaseRecord release) {
        try {
            return jdbcTemplate.update("""
                    INSERT INTO dividend_reserve_releases (
                        id, statement_id, merchant_id, principal_user_id, reserve_cent, ar_ap_item_id,
                        trigger_type, released_by, released_at, note)
                    VALUES (:id, :statement_id, :merchant_id, :principal_user_id, :reserve_cent, :ar_ap_item_id,
                        :trigger_type, :released_by, :released_at, :note)
                    ON CONFLICT (statement_id) DO NOTHING
                    """, new MapSqlParameterSource()
                    .addValue("id", release.id())
                    .addValue("statement_id", release.statementId())
                    .addValue("merchant_id", release.merchantId())
                    .addValue("principal_user_id", release.principalUserId())
                    .addValue("reserve_cent", release.reserveCent())
                    .addValue("ar_ap_item_id", release.arApItemId())
                    .addValue("trigger_type", release.triggerType())
                    .addValue("released_by", release.releasedBy(), Types.VARCHAR)
                    .addValue("released_at", timestampWithTimeZone(release.releasedAt()))
                    .addValue("note", release.note(), Types.VARCHAR)) > 0;
        } catch (DuplicateKeyException conflict) {
            // uq_dividend_reserve_releases_statement：同一计算单已释放（并发兜底）。
            return false;
        }
    }

    private static final String RESERVE_LEDGER_WHERE = """
             WHERE s.kind = 'official' AND s.status IN ('approved', 'paid') AND s.reserve_withheld_cent > 0
               AND (:merchant_id IS NULL OR s.merchant_id = :merchant_id)
               AND (:released_filter IS NULL
                    OR (:released_filter = 'released' AND r.id IS NOT NULL)
                    OR (:released_filter = 'pending' AND r.id IS NULL))
            """;

    @Override
    public PageResponse<DividendRecords.DividendReserveLedgerRecord> pageReserveLedger(
            String merchantId, String releasedFilter, int page, int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, Types.VARCHAR)
                .addValue("released_filter", releasedFilter, Types.VARCHAR)
                .addValue("limit", pageSize)
                .addValue("offset", (long) (page - 1) * pageSize);
        List<DividendRecords.DividendReserveLedgerRecord> items = jdbcTemplate.query("""
                SELECT s.id AS statement_id, s.merchant_id, s.principal_user_id, s.period_label,
                       s.period_start, s.period_end, s.status AS statement_status, s.reserve_withheld_cent,
                       r.id AS release_id, r.trigger_type, r.released_by, r.released_at
                  FROM dividend_statements s
                  LEFT JOIN dividend_reserve_releases r ON r.statement_id = s.id
                """ + RESERVE_LEDGER_WHERE + """
                 ORDER BY (r.id IS NOT NULL), s.period_label DESC, s.merchant_id LIMIT :limit OFFSET :offset
                """, params, this::mapReserveLedger);
        Long total = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM dividend_statements s
                  LEFT JOIN dividend_reserve_releases r ON r.statement_id = s.id
                """ + RESERVE_LEDGER_WHERE, params, Long.class);
        return new PageResponse<>(items, page, pageSize, total == null ? 0L : total);
    }

    private DividendRecords.DividendReserveLedgerRecord mapReserveLedger(ResultSet rs, int rowNum)
            throws SQLException {
        boolean released = rs.getString("release_id") != null;
        return new DividendRecords.DividendReserveLedgerRecord(
                rs.getString("statement_id"),
                rs.getString("merchant_id"),
                rs.getString("principal_user_id"),
                rs.getString("period_label"),
                nullableDate(rs, "period_start"),
                nullableDate(rs, "period_end"),
                rs.getString("statement_status"),
                rs.getLong("reserve_withheld_cent"),
                released,
                rs.getString("trigger_type"),
                rs.getString("released_by"),
                nullableInstant(rs, "released_at"));
    }

    @Override
    public List<DividendRecords.DividendReserveReleaseCandidateRecord> listReserveReleaseCandidates() {
        return jdbcTemplate.query("""
                SELECT s.id AS statement_id, s.merchant_id, s.principal_user_id, s.reserve_withheld_cent,
                       s.period_end, p.period_type, p.reserve_release_mode, p.reserve_release_delay_periods
                  FROM dividend_statements s
                  JOIN dividend_plans p ON p.id = s.plan_id
                 WHERE s.kind = 'official' AND s.status IN ('approved', 'paid') AND s.reserve_withheld_cent > 0
                   AND NOT EXISTS (SELECT 1 FROM dividend_reserve_releases r WHERE r.statement_id = s.id)
                 ORDER BY s.period_end, s.id
                """, new MapSqlParameterSource(),
                (rs, n) -> new DividendRecords.DividendReserveReleaseCandidateRecord(
                        rs.getString("statement_id"),
                        rs.getString("merchant_id"),
                        rs.getString("principal_user_id"),
                        rs.getLong("reserve_withheld_cent"),
                        nullableDate(rs, "period_end"),
                        rs.getString("period_type"),
                        rs.getString("reserve_release_mode"),
                        rs.getInt("reserve_release_delay_periods")));
    }

    // ── 三来源期间取数 ─────────────────────────────────────────

    private static MapSqlParameterSource windowParams(String merchantId, Instant from, Instant to) {
        return new MapSqlParameterSource()
                .addValue("merchant_id", merchantId)
                .addValue("from", timestampWithTimeZone(from))
                .addValue("to", timestampWithTimeZone(to));
    }

    private long sumOrZero(String sql, MapSqlParameterSource params) {
        Long total = jdbcTemplate.queryForObject(sql, params, Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public long sumBarRevenueCent(String merchantId, Instant from, Instant to) {
        // DashboardAggregationJob revenue.net 商户行级同谓词（区间版）：结账实收，冲正行不计。
        return sumOrZero("""
                SELECT COALESCE(SUM(settlement_amount_cent), 0)
                  FROM bar_bill_settlements
                 WHERE merchant_id = :merchant_id
                   AND settled_at >= :from AND settled_at < :to
                   AND reversed = FALSE
                """, windowParams(merchantId, from, to));
    }

    @Override
    public long sumBarRefundCent(String merchantId, Instant from, Instant to) {
        return sumOrZero("""
                SELECT COALESCE(SUM(a.amount_cent), 0)
                  FROM bar_bill_adjustments a
                  JOIN bar_bills b ON b.id = a.bill_id
                 WHERE b.merchant_id = :merchant_id
                   AND a.adjustment_type = 'refund'
                   AND a.refunded_at >= :from AND a.refunded_at < :to
                """, windowParams(merchantId, from, to));
    }

    @Override
    public long sumTicketAllocatedCent(String merchantId, Instant from, Instant to) {
        // allocated_revenue_cent IS NULL（direct 出票/无订单）不计，保守不归任何商户（清单#4 同口径）。
        return sumOrZero("""
                SELECT COALESCE(SUM(allocated_revenue_cent), 0)
                  FROM issued_entitlements
                 WHERE merchant_id = :merchant_id
                   AND allocated_revenue_cent IS NOT NULL
                   AND created_at >= :from AND created_at < :to
                """, windowParams(merchantId, from, to));
    }

    @Override
    public long sumTicketRefundCent(String merchantId, Instant from, Instant to) {
        return sumOrZero("""
                SELECT COALESCE(SUM(refund_amount_cent), 0)
                  FROM ticket_refunds
                 WHERE merchant_id = :merchant_id
                   AND status = 'success'
                   AND succeeded_at >= :from AND succeeded_at < :to
                """, windowParams(merchantId, from, to));
    }

    @Override
    public List<DividendRecords.ExpenseTypeSumRecord> sumExpenseByType(
            String merchantId, List<String> months, List<String> expenseTypes) {
        if (months == null || months.isEmpty() || expenseTypes == null || expenseTypes.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT expense_type, SUM(amount_cent) AS total_cent
                  FROM merchant_expense_entries
                 WHERE merchant_id = :merchant_id
                   AND period IN (:months)
                   AND expense_type IN (:expense_types)
                 GROUP BY expense_type
                 ORDER BY expense_type
                """, new MapSqlParameterSource()
                        .addValue("merchant_id", merchantId)
                        .addValue("months", months)
                        .addValue("expense_types", expenseTypes),
                (rs, n) -> new DividendRecords.ExpenseTypeSumRecord(
                        rs.getString("expense_type"), rs.getLong("total_cent")));
    }
}
