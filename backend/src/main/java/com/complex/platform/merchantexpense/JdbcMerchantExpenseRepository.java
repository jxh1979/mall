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
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("period", period)
                .addValue("operator", operatorUserId, Types.VARCHAR)
                .addValue("created_at", timestampWithTimeZone(now)));
    }

    /** 物业费自动挂接：金额口径 = property_fee_bills.amount_cent（每平米单价 × 面积，不退不补）。 */
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
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("period", period)
                .addValue("operator", operatorUserId, Types.VARCHAR)
                .addValue("created_at", timestampWithTimeZone(now)));
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
