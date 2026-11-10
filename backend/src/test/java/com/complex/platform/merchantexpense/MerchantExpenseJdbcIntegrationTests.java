package com.complex.platform.merchantexpense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.PageResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.sql.Connection;
import java.sql.Savepoint;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * V297 商户费用归集台账 JDBC 集成测试（本迁移交付时按约定未在交付会话运行，待 DB 窗口跑）：
 * 表/索引/权限种子落地、append-only 触发器拦截 UPDATE/DELETE、自动挂接幂等、分页与汇总。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class MerchantExpenseJdbcIntegrationTests {

    @Autowired
    private JdbcMerchantExpenseRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private static MerchantExpenseEntryRecord manualEntry(String id, String merchantId, long amountCent) {
        return new MerchantExpenseEntryRecord(id, merchantId, "2026-07", "labor", amountCent,
                "manual", null, "JDBC 集成测试手工录入", "usr_test", Instant.parse("2026-07-18T00:00:00Z"));
    }

    @Test
    void migrationCreatesTableAndSeedsPermissions() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT to_regclass('merchant_expense_entries')::text", String.class))
                .isEqualTo("merchant_expense_entries");
        List<String> permissions = jdbcTemplate.queryForList(
                "SELECT permission_code FROM permissions WHERE permission_code LIKE 'finance.merchant_expense.%'",
                String.class);
        assertThat(permissions).contains("finance.merchant_expense.view", "finance.merchant_expense.manage");
        List<String> adminGrants = jdbcTemplate.queryForList(
                "SELECT permission_code FROM role_permissions WHERE role_id = 'role_platform_admin' "
                        + "AND permission_code LIKE 'finance.merchant_expense.%'",
                String.class);
        assertThat(adminGrants).contains("finance.merchant_expense.view", "finance.merchant_expense.manage");
    }

    // UPDATE 与 DELETE 拆两个用例：触发器 RAISE 后整事务 abort，同一事务内第二条语句只会报
    // "current transaction is aborted"，无法断言真实拦截原因。

    @Test
    void appendOnlyTriggerBlocksUpdate() {
        repository.insertEntry(manualEntry("mex_jdbc_ao_u", "merchant_jdbc", 100_00L));
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE merchant_expense_entries SET amount_cent = 999 WHERE id = 'mex_jdbc_ao_u'"))
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void appendOnlyTriggerBlocksDelete() {
        repository.insertEntry(manualEntry("mex_jdbc_ao_d", "merchant_jdbc", 100_00L));
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM merchant_expense_entries WHERE id = 'mex_jdbc_ao_d'"))
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void insertIsIdempotentByIdAndListFiltersWork() {
        assertThat(repository.insertEntry(manualEntry("mex_jdbc_1", "merchant_jdbc", 100_00L))).isTrue();
        assertThat(repository.insertEntry(manualEntry("mex_jdbc_1", "merchant_jdbc", 100_00L))).isFalse();
        repository.insertEntry(manualEntry("mex_jdbc_2", "merchant_jdbc_other", 200_00L));

        PageResponse<MerchantExpenseEntryRecord> page =
                repository.listEntries("merchant_jdbc", "2026-07", "labor", "manual", 1, 50);
        assertThat(page.items()).extracting(MerchantExpenseEntryRecord::id).containsExactly("mex_jdbc_1");
        assertThat(page.total()).isEqualTo(1);

        List<MerchantExpenseSummaryRecord> summary = repository.summarize("2026-07", "merchant_jdbc");
        assertThat(summary).singleElement().satisfies(row -> {
            assertThat(row.expenseType()).isEqualTo("labor");
            assertThat(row.totalCent()).isEqualTo(100_00L);
        });
    }

    @Test
    void attachEnergyFeesIsIdempotentPerArApItem() {
        Instant now = Instant.parse("2026-07-18T00:00:00Z");
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (
                    id, item_no, direction, counterparty_id, source_type, source_id,
                    amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period,
                    status, created_at, updated_at, merchant_id
                ) VALUES (
                    'arap_mex_energy_1', 'ENERGY-MEX-1', 'receivable', 'merchant_jdbc', 'energy_fee',
                    'energy_fee_mex_1', 88800, 0, 88800, 'CNY', DATE '2026-07-01', '2026-07',
                    'pending_collection', now(), now(), 'merchant_jdbc'
                ) ON CONFLICT (id) DO NOTHING
                """);

        int first = repository.attachEnergyFees("2026-07", "usr_test", now);
        assertThat(first).isGreaterThanOrEqualTo(1);
        int second = repository.attachEnergyFees("2026-07", "usr_test", now);
        assertThat(second).isZero();

        MerchantExpenseEntryRecord attached = repository.findById("mex_ae_arap_mex_energy_1").orElseThrow();
        assertThat(attached.expenseType()).isEqualTo("energy");
        assertThat(attached.source()).isEqualTo("auto_energy");
        assertThat(attached.amountCent()).isEqualTo(88800L);
        assertThat(attached.sourceRef()).isEqualTo("arap_mex_energy_1");
    }

    @Test
    void attachPropertyFeesIsIdempotentPerBill() {
        Instant now = Instant.parse("2026-07-18T00:00:00Z");
        jdbcTemplate.update("""
                INSERT INTO property_fee_bills (
                    id, project_id, period, merchant_id, area_square_meter,
                    rate_per_square_meter_cent, amount_cent, ar_ap_item_id, created_at
                ) VALUES (
                    'pfb_mex_1', 'proj_jdbc', '2026-07', 'merchant_jdbc', 100.00, 2500, 250000, NULL, now()
                ) ON CONFLICT (id) DO NOTHING
                """);

        int first = repository.attachPropertyFees("2026-07", "usr_test", now);
        assertThat(first).isGreaterThanOrEqualTo(1);
        int second = repository.attachPropertyFees("2026-07", "usr_test", now);
        assertThat(second).isZero();

        MerchantExpenseEntryRecord attached = repository.findById("mex_ap_pfb_mex_1").orElseThrow();
        assertThat(attached.expenseType()).isEqualTo("property_fee");
        assertThat(attached.source()).isEqualTo("auto_property");
        assertThat(attached.amountCent()).isEqualTo(250000L);
    }

    @Test
    void attachContractRentCoversBothRentSourceTypesIdempotently() {
        // 2026-07-18 拍板：租金计入费用台账。覆盖两条租金出账路径——
        // contract_rent（合同租金，单期/批量都经 genFeeBill → insertRentBill）+ turnover_rent（营业额抽成，
        // V299 起 accounting_period=期间起始月）；同上游单据重跑不重复计入。
        Instant now = Instant.parse("2026-07-18T00:00:00Z");
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (
                    id, item_no, direction, counterparty_id, source_type, source_id,
                    amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period,
                    status, created_at, updated_at, merchant_id
                ) VALUES
                ('arap_mex_rent_1', 'RENT-MEX-1', 'receivable', 'merchant_jdbc', 'contract_rent',
                 'contract_mex|2026-07', 120000, 0, 120000, 'CNY', DATE '2026-07-01', '2026-07',
                 'pending_collection', now(), now(), 'merchant_jdbc'),
                ('arap_mex_rent_2', 'TRENT-MEX-1', 'receivable', 'merchant_jdbc', 'turnover_rent',
                 'trr_mex_1', 35000, 0, 35000, 'CNY', DATE '2026-08-05', '2026-07',
                 'pending_collection', now(), now(), 'merchant_jdbc')
                ON CONFLICT (id) DO NOTHING
                """);

        int first = repository.attachContractRent("2026-07", "usr_test", now);
        assertThat(first).isGreaterThanOrEqualTo(2);
        assertThat(repository.attachContractRent("2026-07", "usr_test", now)).isZero();

        MerchantExpenseEntryRecord contractRent = repository.findById("mex_arn_arap_mex_rent_1").orElseThrow();
        assertThat(contractRent.expenseType()).isEqualTo("rent_allocation");
        assertThat(contractRent.source()).isEqualTo("auto_rent");
        assertThat(contractRent.amountCent()).isEqualTo(120000L);
        MerchantExpenseEntryRecord turnoverRent = repository.findById("mex_arn_arap_mex_rent_2").orElseThrow();
        assertThat(turnoverRent.amountCent()).isEqualTo(35000L);
        // 汇总口径：rent_allocation 净额 = 两路租金合计（分红引擎费用侧取数点）。
        assertThat(repository.summarize("2026-07", "merchant_jdbc"))
                .anySatisfy(row -> {
                    assertThat(row.expenseType()).isEqualTo("rent_allocation");
                    assertThat(row.totalCent()).isEqualTo(155000L);
                });
    }

    @Test
    void negativeReversalRowPassesRealCheckConstraints() {
        // 红字冲销行 = 负金额：必须能通过真实 CHECK（chk_mex_amount_nonzero 只拦 0，不拦负数）落库。
        repository.insertEntry(manualEntry("mex_jdbc_neg_orig", "merchant_jdbc", 300_00L));
        MerchantExpenseEntryRecord reversal = new MerchantExpenseEntryRecord(
                "mex_r_mex_jdbc_neg_orig", "merchant_jdbc", "2026-07", "labor", -300_00L,
                "manual", "mex_jdbc_neg_orig", "JDBC 集成测试红字冲销", "usr_test",
                Instant.parse("2026-07-18T00:00:00Z"));
        assertThat(repository.insertEntry(reversal)).isTrue();
        assertThat(repository.findById("mex_r_mex_jdbc_neg_orig").orElseThrow().amountCent())
                .isEqualTo(-300_00L);
        // 汇总净额归零（分红费用基数口径对真实 SUM 生效）。
        assertThat(repository.summarize("2026-07", "merchant_jdbc"))
                .anySatisfy(row -> {
                    assertThat(row.expenseType()).isEqualTo("labor");
                    assertThat(row.totalCent()).isZero();
                });
    }

    @Test
    void autoSourceRefUniqueIndexGuardsIndependentlyOfPrimaryKey() throws Exception {
        // (source, source_ref) 部分唯一索引须独立于 id 主键生效：不同 id、同 (source, source_ref) 的
        // 自动挂接行必须被拒（防绕过 id 派生规则手插重复挂接行、双计费用）。
        // 冲突 INSERT 用事务绑定连接上的手动 savepoint 包住（NESTED 裸写法）：唯一索引冲突的 25P02
        // 中止被 savepoint 吸收，外层事务照常可用。不用 REQUIRES_NEW（独立连接会等外层未提交的
        // 同键插入，同线程自锁挂死）；也不用 TransactionTemplate+NESTED（共享事务管理器
        // nestedTransactionAllowed=false，直接抛 NestedTransactionNotSupportedException）。
        repository.insertEntry(new MerchantExpenseEntryRecord(
                "mex_ae_dup_src_a", "merchant_jdbc", "2026-07", "energy", 100_00L,
                "auto_energy", "arap_dup_src", "JDBC 集成测试挂接行 A", "usr_test",
                Instant.parse("2026-07-18T00:00:00Z")));
        Connection conn = DataSourceUtils.getConnection(dataSource);
        Savepoint sp = conn.setSavepoint();
        try {
            assertThatThrownBy(() -> repository.insertEntry(new MerchantExpenseEntryRecord(
                    "mex_ae_dup_src_b", "merchant_jdbc", "2026-07", "energy", 100_00L,
                    "auto_energy", "arap_dup_src", "JDBC 集成测试挂接行 B（应被唯一索引拒绝）", "usr_test",
                    Instant.parse("2026-07-18T00:00:00Z"))))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("uq_mex_auto_source_ref");
        } finally {
            conn.rollback(sp);
            conn.releaseSavepoint(sp);
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
        // savepoint 回滚后外层事务干净：B 未落库，A 仍在。
        assertThat(repository.findById("mex_ae_dup_src_b")).isEmpty();
        assertThat(repository.findById("mex_ae_dup_src_a")).isPresent();
    }

    @Test
    void dividendPeriodLockGuardsWritesAndSkipsAutoAttach() {
        // V302 关账锁：approved 正式分红单覆盖的 (商户,月份) → isPeriodLocked=true，自动挂接跳过该商户。
        Instant now = Instant.parse("2026-07-18T00:00:00Z");
        // 先落一张生效方案（statements.plan_id FK），再落一张 approved 全零单（满足五条恒等 CHECK）覆盖 2026-07。
        jdbcTemplate.update("""
                INSERT INTO dividend_plans (id, merchant_id, principal_user_id, platform_share_bps, tax_rate_bps,
                    period_type, revenue_domains, include_inventory_cost, expense_types, status, payout_pay_slot)
                VALUES ('divp_lock_jdbc', 'merchant_locked', 'usr_p', 8000, 2000, 'quarterly',
                    'bar,ticket', TRUE, 'labor', 'active', 1)
                ON CONFLICT (id) DO NOTHING
                """);
        jdbcTemplate.update("""
                INSERT INTO dividend_statements (id, merchant_id, plan_id, kind, period_label, period_start, period_end,
                    principal_user_id, platform_share_bps, tax_rate_bps, revenue_domains, include_inventory_cost,
                    expense_types, revenue_cent, cost_cent, expense_cent, net_profit_cent, distributable_cent,
                    platform_share_cent, principal_gross_cent, tax_cent, principal_net_cent, status)
                VALUES ('divs_lock_jdbc', 'merchant_locked', 'divp_lock_jdbc', 'official', '2026-Q3',
                    DATE '2026-07-01', DATE '2026-09-30', 'usr_p', 8000, 2000, 'bar,ticket', TRUE, 'labor',
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 'approved')
                ON CONFLICT (id) DO NOTHING
                """);

        // 锁判定：季度单覆盖 2026-07/08/09；相邻月不锁。
        assertThat(repository.isPeriodLocked("merchant_locked", "2026-07")).isTrue();
        assertThat(repository.isPeriodLocked("merchant_locked", "2026-08")).isTrue();
        assertThat(repository.isPeriodLocked("merchant_locked", "2026-06")).isFalse();
        assertThat(repository.isPeriodLocked("merchant_open", "2026-07")).isFalse();
        assertThat(repository.listLockedMerchants("2026-07")).contains("merchant_locked");

        // 自动挂接跳过关账商户：为关账商户 + 未关账商户各插一张 2026-07 能耗应收，attach 只挂未关账者。
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,
                    amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period,
                    status, created_at, updated_at, merchant_id)
                VALUES
                ('arap_lock_energy', 'ENERGY-LOCK', 'receivable', 'merchant_locked', 'energy_fee', 'ef_lock',
                 50000, 0, 50000, 'CNY', DATE '2026-07-01', '2026-07', 'pending_collection', now(), now(),
                 'merchant_locked'),
                ('arap_open_energy', 'ENERGY-OPEN', 'receivable', 'merchant_open', 'energy_fee', 'ef_open',
                 60000, 0, 60000, 'CNY', DATE '2026-07-01', '2026-07', 'pending_collection', now(), now(),
                 'merchant_open')
                ON CONFLICT (id) DO NOTHING
                """);
        repository.attachEnergyFees("2026-07", "usr_test", now);
        assertThat(repository.findById("mex_ae_arap_lock_energy")).isEmpty();   // 关账商户被 NOT EXISTS 跳过
        assertThat(repository.findById("mex_ae_arap_open_energy")).isPresent(); // 未关账商户正常挂接

        // V302 部分索引存在。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT to_regclass('ix_dividend_statements_lock')::text", String.class))
                .isEqualTo("ix_dividend_statements_lock");
    }
}
