package com.complex.platform.merchantexpense;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.api.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 商户费用归集台账 JDBC 仓储（append-only：只 INSERT ON CONFLICT DO NOTHING）。 */
@Repository
class JdbcMerchantExpenseRepository implements MerchantExpenseRepository {

    private static final String COLS =
            "id, merchant_id, period, expense_type, amount_cent, source, source_ref, note, operator_user_id, created_at";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcMerchantExpenseRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static MapSqlParameterSource filterParams(
            String merchantId, String period, String expenseType, String source) {
        return new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, Types.VARCHAR)
                .addValue("period", period, Types.VARCHAR)
                .addValue("expense_type", expenseType, Types.VARCHAR)
                .addValue("source", source, Types.VARCHAR);
    }

    private static final String FILTER_WHERE = """
             WHERE (:merchant_id IS NULL OR merchant_id = :merchant_id)
               AND (:period IS NULL OR period = :period)
               AND (:expense_type IS NULL OR expense_type = :expense_type)
               AND (:source IS NULL OR source = :source)
            """;

    @Override
    public PageResponse<MerchantExpenseEntryRecord> listEntries(
            String merchantId, String period, String expenseType, String source, int page, int pageSize) {
        MapSqlParameterSource params = filterParams(merchantId, period, expenseType, source)
                .addValue("limit", pageSize)
                .addValue("offset", (long) (page - 1) * pageSize);
        List<MerchantExpenseEntryRecord> items = jdbcTemplate.query(
                "SELECT " + COLS + " FROM merchant_expense_entries" + FILTER_WHERE
                        + " ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset",
                params, this::mapEntry);
        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM merchant_expense_entries" + FILTER_WHERE,
                params, Long.class);
        return new PageResponse<>(items, page, pageSize, total == null ? 0L : total);
    }

    @Override
    public List<MerchantExpenseSummaryRecord> summarize(String period, String merchantId) {
        return jdbcTemplate.query("""
                SELECT merchant_id, period, expense_type, SUM(amount_cent) AS total_cent
                  FROM merchant_expense_entries
                 WHERE period = :period
                   AND (:merchant_id IS NULL OR merchant_id = :merchant_id)
                 GROUP BY merchant_id, period, expense_type
                 ORDER BY merchant_id, expense_type
                """,
                new MapSqlParameterSource()
                        .addValue("period", period)
                        .addValue("merchant_id", merchantId, Types.VARCHAR),
                (rs, n) -> new MerchantExpenseSummaryRecord(
                        rs.getString("merchant_id"), rs.getString("period"),
                        rs.getString("expense_type"), rs.getLong("total_cent")));
    }

    @Override
    public Optional<MerchantExpenseEntryRecord> findById(String id) {
        return jdbcTemplate.query("SELECT " + COLS + " FROM merchant_expense_entries WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapEntry).stream().findFirst();
    }

    @Override
    public boolean insertEntry(MerchantExpenseEntryRecord entry) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO merchant_expense_entries (
                    id, merchant_id, period, expense_type, amount_cent, source, source_ref,
                    note, operator_user_id, created_at
                )
                VALUES (
                    :id, :merchant_id, :period, :expense_type, :amount_cent, :source, :source_ref,
                    :note, :operator_user_id, :created_at
                )
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", entry.id())
                .addValue("merchant_id", entry.merchantId())
                .addValue("period", entry.period())
                .addValue("expense_type", entry.expenseType())
                .addValue("amount_cent", entry.amountCent())
                .addValue("source", entry.source())
                .addValue("source_ref", entry.sourceRef(), Types.VARCHAR)
                .addValue("note", entry.note(), Types.VARCHAR)
                .addValue("operator_user_id", entry.operatorUserId(), Types.VARCHAR)
                .addValue("created_at", timestampWithTimeZone(entry.createdAt())));
        return inserted > 0;
    }

    /**
     * 能耗费自动挂接：金额口径 = ar_ap_items.amount_cent（应收全额，非已收），
     * 行 id 由上游单据 id 派生（'mex_ae_' || id）+ (source, source_ref) 部分唯一索引，双重幂等。
     */
    @Override
    public int attachEnergyFees(String period, String operatorUserId, Instant now) {
        return jdbcTemplate.update("""
                INSERT INTO merchant_expense_entries (
                    id, merchant_id, period, expense_type, amount_cent, source, source_ref,
                    note, operator_user_id, created_at
                )
                SELECT 'mex_ae_' || i.id, i.merchant_id, :period, 'energy', i.amount_cent, 'auto_energy', i.id,
                       '能耗费自动挂接（应收单 ' || i.item_no || '）', :operator, :created_at
                  FROM ar_ap_items i
                 WHERE i.direction = 'receivable'
                   AND i.source_type = 'energy_fee'
                   AND i.accounting_period = :period
                   AND i.merchant_id IS NOT NULL
                   AND i.amount_cent <> 0
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("period", period)
                .addValue("operator", operatorUserId, Types.VARCHAR)
                .addValue("created_at", timestampWithTimeZone(now)));
    }

    /**
     * 物业费自动挂接（账单路径）：金额口径 = property_fee_bills.amount_cent（每平米单价 × 面积，不退不补）。
     * amount_cent <> 0 守卫：0 元账单会撞台账 chk_mex_amount_nonzero，整期 sync 报错——直接跳过不计入。
     */
    @Override
    public int attachPropertyFees(String period, String operatorUserId, Instant now) {
        return jdbcTemplate.update("""
                INSERT INTO merchant_expense_entries (
                    id, merchant_id, period, expense_type, amount_cent, source, source_ref,
                    note, operator_user_id, created_at
                )
                SELECT 'mex_ap_' || b.id, b.merchant_id, b.period, 'property_fee', b.amount_cent, 'auto_property', b.id,
                       '物业费自动挂接（账单 ' || b.id || '）', :operator, :created_at
                  FROM property_fee_bills b
                 WHERE b.period = :period
                   AND b.amount_cent <> 0
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("period", period)
                .addValue("operator", operatorUserId, Types.VARCHAR)
                .addValue("created_at", timestampWithTimeZone(now)));
    }

    /**
     * 物业费自动挂接（合同计费路径）：合同 rentTerms.propertyFeeCent 出账落 ar_ap_items
     * source_type='contract_property_fee'（见 JdbcContractRepository.insertRentBill），与 property_fee_bills
     * 是两条独立出账路径（上游单据不同，不会重复计入）；expense_type 同归 property_fee，id 前缀 mex_acp_ 区分。
     */
    @Override
    public int attachContractPropertyFees(String period, String operatorUserId, Instant now) {
        return jdbcTemplate.update("""
                INSERT INTO merchant_expense_entries (
                    id, merchant_id, period, expense_type, amount_cent, source, source_ref,
                    note, operator_user_id, created_at
                )
                SELECT 'mex_acp_' || i.id, i.merchant_id, :period, 'property_fee', i.amount_cent, 'auto_property', i.id,
                       '物业费自动挂接（合同计费应收单 ' || i.item_no || '）', :operator, :created_at
                  FROM ar_ap_items i
                 WHERE i.direction = 'receivable'
                   AND i.source_type = 'contract_property_fee'
                   AND i.accounting_period = :period
                   AND i.merchant_id IS NOT NULL
                   AND i.amount_cent <> 0
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("period", period)
                .addValue("operator", operatorUserId, Types.VARCHAR)
                .addValue("created_at", timestampWithTimeZone(now)));
    }

    /**
     * 金额漂移报告：上游（ar_ap_items 锁定后补录审批可原地改 amount_cent）改账后，快照式挂接不自动追改。
     * 本查询对该期每个 auto 挂接行，算台账净额 = 原行 + 指向它的红字冲销行合计，与上游当前金额比对，
     * 不等则列入清单（处置 SOP：红字冲销旧行 + 手工录入修正额）。上游表按行 id 前缀区分
     * （mex_ap_=property_fee_bills，其余=ar_ap_items）。
     */
    @Override
    public List<MerchantExpenseDriftRecord> listAutoDrift(String period) {
        return jdbcTemplate.query("""
                SELECT base.id AS entry_id, base.merchant_id, base.source, base.source_ref,
                       base.net_cent,
                       COALESCE(CASE WHEN base.id LIKE 'mex\\_ap\\_%' THEN pb.amount_cent ELSE ar.amount_cent END, 0)
                           AS upstream_cent
                  FROM (
                        SELECT e.id, e.merchant_id, e.source, e.source_ref,
                               e.amount_cent + COALESCE(rv.sum_cent, 0) AS net_cent
                          FROM merchant_expense_entries e
                          LEFT JOIN (SELECT source_ref, SUM(amount_cent) AS sum_cent
                                       FROM merchant_expense_entries
                                      WHERE source = 'manual' AND source_ref IS NOT NULL
                                      GROUP BY source_ref) rv ON rv.source_ref = e.id
                         WHERE e.period = :period AND e.source <> 'manual'
                       ) base
                  LEFT JOIN ar_ap_items ar ON ar.id = base.source_ref
                  LEFT JOIN property_fee_bills pb ON pb.id = base.source_ref
                 WHERE base.net_cent <> COALESCE(
                        CASE WHEN base.id LIKE 'mex\\_ap\\_%' THEN pb.amount_cent ELSE ar.amount_cent END, 0)
                 ORDER BY base.id
                """,
                new MapSqlParameterSource("period", period),
                (rs, n) -> new MerchantExpenseDriftRecord(
                        rs.getString("entry_id"), rs.getString("merchant_id"), rs.getString("source"),
                        rs.getString("source_ref"), rs.getLong("net_cent"), rs.getLong("upstream_cent")));
    }

    private MerchantExpenseEntryRecord mapEntry(ResultSet rs, int n) throws SQLException {
        return new MerchantExpenseEntryRecord(
                rs.getString("id"),
                rs.getString("merchant_id"),
                rs.getString("period"),
                rs.getString("expense_type"),
                rs.getLong("amount_cent"),
                rs.getString("source"),
                rs.getString("source_ref"),
                rs.getString("note"),
                rs.getString("operator_user_id"),
                nullableInstant(rs, "created_at"));
    }
}
