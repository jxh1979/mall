package com.complex.platform.merchantexpense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.PageResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private PlatformTransactionManager transactionManager;

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
    void autoSourceRefUniqueIndexGuardsIndependentlyOfPrimaryKey() {
        // (source, source_ref) 部分唯一索引须独立于 id 主键生效：不同 id、同 (source, source_ref) 的
        // 自动挂接行必须被拒（防绕过 id 派生规则手插重复挂接行、双计费用）。
        // 冲突 INSERT 包进 NESTED（savepoint）子事务：唯一索引冲突后中止被限制在 savepoint 内，
        // 外层事务/连接照常可用（同类前车之鉴见上方 append-only 拆分注释；不用 REQUIRES_NEW——
        // 独立连接会等外层未提交的同键插入，同线程自锁挂死）。
        repository.insertEntry(new MerchantExpenseEntryRecord(
                "mex_ae_dup_src_a", "merchant_jdbc", "2026-07", "energy", 100_00L,
                "auto_energy", "arap_dup_src", "JDBC 集成测试挂接行 A", "usr_test",
                Instant.parse("2026-07-18T00:00:00Z")));
        TransactionTemplate nested = new TransactionTemplate(transactionManager);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        assertThatThrownBy(() -> nested.execute(status -> repository.insertEntry(new MerchantExpenseEntryRecord(
                "mex_ae_dup_src_b", "merchant_jdbc", "2026-07", "energy", 100_00L,
                "auto_energy", "arap_dup_src", "JDBC 集成测试挂接行 B（应被唯一索引拒绝）", "usr_test",
                Instant.parse("2026-07-18T00:00:00Z")))))
                .hasMessageContaining("uq_mex_auto_source_ref");
        // savepoint 回滚后外层连接干净：B 未落库，A 仍在。
        assertThat(repository.findById("mex_ae_dup_src_b")).isEmpty();
        assertThat(repository.findById("mex_ae_dup_src_a")).isPresent();
    }
}
