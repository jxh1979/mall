package com.complex.platform.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * P0-B 资金台账 append-only 破防三处修复的数据库层门禁（V344）。
 *
 * <p>覆盖三条防线的<b>反证</b>——直接对库下手必须被数据库拒绝：
 * <ol>
 *   <li>{@code settlement_transactions}（通票分账最高价值账本）：UPDATE / DELETE 一律拦截；</li>
 *   <li>{@code customer_points_ledger}：金额与归属等事实列冻结，FIFO 批次运行态列仍可改；</li>
 *   <li>{@code ar_ap_items}：红冲底座——冲正行必须挂原行且三个金额列全部取反，
 *       无原行的负额行一律拒绝；原行 + 冲正行 + 重述行三行相加守恒。</li>
 * </ol>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class FundLedgerAppendOnlyJdbcIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String uid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    // ---------------- ② settlement_transactions ----------------

    private String insertSettlementTransaction() {
        String id = "st_p0b_" + uid();
        jdbcTemplate.update(
                "INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,"
                        + " entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,"
                        + " verified_amount_cent, unverified_amount_cent, beneficiary_type, project_id,"
                        + " settlement_status, entitlement_valid_to, created_at, updated_at)"
                        + " VALUES (?, ?, 'verification_settled', ?, 'ticket', 1, 1, 1, 10000, 10000, 0,"
                        + " 'project', 'proj_p0b', 'draft', ?, ?, ?)",
                id, "TN-" + id, "ent_" + id,
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
        return id;
    }

    @Test
    void settlementTransactionUpdateIsBlockedByTrigger() {
        String id = insertSettlementTransaction();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE settlement_transactions SET verified_amount_cent = ? WHERE id = ?", 1L, id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void settlementTransactionDeleteIsBlockedByTrigger() {
        String id = insertSettlementTransaction();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM settlement_transactions WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void settlementTransactionCarriesAppendOnlyTrigger() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid "
                        + "WHERE t.tgname = 'trg_append_only_ud' AND c.relname = 'settlement_transactions'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // ---------------- ③ customer_points_ledger ----------------

    private String insertEarnLedger() {
        String customerId = "cust_p0b_" + uid();
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
                "INSERT INTO customers (id, phone, masked_phone, status, created_at, updated_at, phone_hash)"
                        + " VALUES (?, 'enc:v1:p0b', '1****0000', 'active', ?, ?, ?)",
                customerId, now, now, String.format("%-64s", customerId).replace(' ', '0'));
        String ledgerId = "ledger_earn_p0b_" + uid();
        jdbcTemplate.update(
                "INSERT INTO customer_points_ledger (id, customer_id, points_delta, balance_after, source_type,"
                        + " source_id, created_at, expired, change_type, status, source_doc_type, source_doc_id,"
                        + " confirmed_amount, remaining_points)"
                        + " VALUES (?, ?, 100, 0, 'earn', ?, ?, FALSE, 'earn', 'pending', 'ticket_order', ?, 0, 100)",
                ledgerId, customerId, ledgerId, now, "o_p0b_" + uid());
        return ledgerId;
    }

    @Test
    void pointsLedgerConfirmedAmountUpdateIsBlockedByTrigger() {
        String ledgerId = insertEarnLedger();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE customer_points_ledger SET confirmed_amount = 100 WHERE id = ?", ledgerId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void pointsLedgerPointsDeltaUpdateIsBlockedByTrigger() {
        String ledgerId = insertEarnLedger();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE customer_points_ledger SET points_delta = 999 WHERE id = ?", ledgerId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void pointsLedgerBatchRuntimeColumnsStayUpdatable() {
        String ledgerId = insertEarnLedger();
        // FIFO 批次运行态（可过期余额 / 过期标记 / 生命周期状态）仍须可改——与 ar_ap_items.paid/unpaid 同口径。
        assertThatCode(() -> jdbcTemplate.update(
                "UPDATE customer_points_ledger SET remaining_points = 40, expired = FALSE, status = 'confirmed'"
                        + " WHERE id = ?", ledgerId))
                .doesNotThrowAnyException();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT remaining_points FROM customer_points_ledger WHERE id = ?", Long.class, ledgerId))
                .isEqualTo(40L);
    }

    // ---------------- ① ar_ap_items 红冲底座 ----------------

    private String insertArApItem(String suffix, long amountCent, long paidCent,
                                  String originItemId, String adjustmentId) {
        String id = "arap_p0b_" + suffix;
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
                "INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,"
                        + " amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date,"
                        + " accounting_period, status, created_at, updated_at, origin_item_id, adjustment_id)"
                        + " VALUES (?, ?, 'receivable', 'cp_p0b', 'manual', ?, ?, ?, ?, 'CNY', ?, '2026-07',"
                        + " 'pending_collection', ?, ?, ?, ?)",
                id, "NO-" + id, "src_" + suffix, amountCent, paidCent, amountCent - paidCent,
                java.sql.Date.valueOf(LocalDate.parse("2026-06-30")), now, now, originItemId, adjustmentId);
        return id;
    }

    @Test
    void arApRedReversalIsStrictNegationAndConservesTotal() {
        String run = uid();
        String origin = insertArApItem("o" + run, 100000L, 20000L, null, null);
        // 冲正行：原行三列的严格取反；重述行：原额 + 补录额（+5000），已付部分原样承接。
        insertArApItem("r" + run, -100000L, -20000L, origin, "adj_" + run);
        insertArApItem("s" + run, 105000L, 20000L, origin, "adj_" + run);

        // 守恒：三行相加 = 补录后应有金额；已付/未付三列同样守恒（账平恒等对正负两侧同时成立）。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT SUM(amount_cent) FROM ar_ap_items WHERE id = ? OR origin_item_id = ?",
                Long.class, origin, origin)).isEqualTo(105000L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT SUM(paid_amount_cent) FROM ar_ap_items WHERE id = ? OR origin_item_id = ?",
                Long.class, origin, origin)).isEqualTo(20000L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT SUM(unpaid_amount_cent) FROM ar_ap_items WHERE id = ? OR origin_item_id = ?",
                Long.class, origin, origin)).isEqualTo(85000L);
        // 原行金额分文未动。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT amount_cent FROM ar_ap_items WHERE id = ?", Long.class, origin)).isEqualTo(100000L);
    }

    @Test
    void arApNegativeRowWithoutOriginIsRejected() {
        String run = uid();
        // 没有 origin_item_id 的负额行 = 凭空红字，必须被数据库拒绝（防止绕过补录审批直接冲账）。
        assertThatThrownBy(() -> insertArApItem("bad" + run, -100000L, 0L, null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_ar_ap_items_amount_positive");
    }

    @Test
    void arApCorrectionRowsMayReuseOriginalSourceDocument() {
        String run = uid();
        String origin = insertArApItem("os" + run, 100000L, 0L, null, null);
        // 冲正行/重述行刻意沿用原行 source_type/source_id（下游按来源聚合才自动净额正确），
        // 唯一索引须放行这类派生行，否则红冲根本落不下去。
        assertThatCode(() -> {
            jdbcTemplate.update(
                    "INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,"
                            + " amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, status,"
                            + " created_at, updated_at, origin_item_id, adjustment_id)"
                            + " SELECT ?, ?, direction, counterparty_id, source_type, source_id,"
                            + " -amount_cent, -paid_amount_cent, -unpaid_amount_cent, currency, due_date,"
                            + " 'reversed', created_at, updated_at, id, ?"
                            + "   FROM ar_ap_items WHERE id = ?",
                    "arap_p0b_dup" + run, "NO-dup" + run, "adj_dup_" + run, origin);
        }).doesNotThrowAnyException();
    }
}
