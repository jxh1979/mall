package com.complex.platform.dividend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.dividend.DividendRecords.DividendComputeResultRecord;
import com.complex.platform.dividend.DividendRecords.DividendPlanRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementLineRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementRecord;
import com.complex.platform.dividend.dto.DividendActionRequest;
import com.complex.platform.dividend.dto.DividendComputeRequest;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.DividendPayoutPort;
import com.complex.platform.inventory.InventoryRepository;
import java.sql.Connection;
import java.sql.Savepoint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * V299 持股分红 JDBC 集成测试（本迁移交付时按约定未在交付会话运行，待共享 complex_test 串行窗口跑）：
 * 三表/索引/权限种子落地、明细行 append-only 触发器拦截、恒等式 CHECK 拦截、同期唯一部分索引、
 * 期间作业端到端（种子收入+成本+费用 → 算单 → 数字恒等三来源 → 审批挂应付）。
 * 唯一索引/CHECK 冲突用手动 savepoint 吸收（V297 教训：共享 txManager 禁 NESTED，25P02 不外溢）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class DividendJdbcIntegrationTests {

    private static final String MERCHANT = "merchant_div_jdbc";

    @Autowired
    private JdbcDividendRepository repository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private com.complex.platform.merchantexpense.MerchantExpenseRepository merchantExpenseRepository;

    @Autowired
    private AuditedOperationExecutor operations;

    @Autowired
    private BusinessDayResolver businessDayResolver;

    @Autowired
    private DividendPayoutPort payoutPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    /** I-1 职责分离：提交与审批必须不同人，测试里按动作分配不同 actor 的服务实例。 */
    private DividendService serviceWithPlatformActor(String actorId) {
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor(actorId, "platform_admin_user", "platform_admin", "org_1", null, "active"));
        return new DividendService(repository, inventoryRepository, businessDayResolver,
                authorizationService, operations, payoutPort);
    }

    private static DividendPlanRecord plan(String id, String periodType) {
        Instant now = Instant.parse("2026-07-18T00:00:00Z");
        return new DividendRecords.DividendPlanRecord(id, MERCHANT, "usr_principal_jdbc", "主理人甲",
                8000, 2000, periodType, List.of("bar", "ticket"), true,
                List.of("rent_allocation", "energy", "property_fee", "labor", "amortization", "other"),
                null, "active", null, "usr_div_jdbc", now, now,
                false, false, false, 0, 0L);
    }

    private static DividendStatementRecord statement(String id, String planId, long revenue, long cost,
            long expense, long netProfit, long distributable, long platformShare, long gross, long tax, long net) {
        // V303：三扣除全关，实发=税后应得（payout=net），结转/抵扣/留存全 0。
        Instant now = Instant.parse("2026-07-18T00:00:00Z");
        return new DividendStatementRecord(id, MERCHANT, planId, "official", "2026-07",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31"), "usr_principal_jdbc",
                8000, 2000, List.of("bar", "ticket"), true, List.of("labor"),
                revenue, cost, expense, netProfit, distributable, platformShare, gross, tax, net,
                0L, "draft", null, null, "usr_div_jdbc", null, null, null, null, null, null, now, now,
                false, 0L, 0L, 0L, 0L, 0L, net);
    }

    /**
     * V303 三扣除入账留痕的计算单构造（供 DB CHECK 拦截用例）。基口径恒等自洽（revenue=netProfit、cost/expense=0），
     * 由调用方单独corrupt某个扣除留痕列以隔离触发对应 CHECK。
     */
    private static DividendStatementRecord deductionStatement(String id, String planId, long netProfit,
            long distributable, long platformShare, long gross, long tax, long net,
            boolean lcfEnabled, long lcfIn, long lcfApplied, long lcfAfter,
            long arrearsOffset, long reserve, long payout) {
        Instant now = Instant.parse("2026-07-18T00:00:00Z");
        return new DividendStatementRecord(id, MERCHANT, planId, "official", "2026-07",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31"), "usr_principal_jdbc",
                8000, 2000, List.of("bar", "ticket"), true, List.of("labor"),
                netProfit, 0L, 0L, netProfit, distributable, platformShare,
                gross, tax, net, 0L, "draft", null, null, "usr_div_jdbc", null, null, null, null, null, null,
                now, now, lcfEnabled, lcfIn, lcfApplied, lcfAfter, arrearsOffset, reserve, payout);
    }

    /** 唯一索引/CHECK 冲突 INSERT 用事务绑定连接上的手动 savepoint 吸收，25P02 不外溢（V297 同款）。 */
    private void assertInsertRejected(Runnable insert, String messagePart) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        Savepoint savepoint = null;
        try {
            savepoint = connection.setSavepoint();
            Savepoint finalSavepoint = savepoint;
            assertThatThrownBy(insert::run)
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining(messagePart);
            connection.rollback(finalSavepoint);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("savepoint handling failed", e);
        } finally {
            if (savepoint != null) {
                try {
                    connection.releaseSavepoint(savepoint);
                } catch (java.sql.SQLException ignored) {
                    // savepoint 已在 rollback 时释放
                }
            }
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Test
    void migrationCreatesTablesAndSeedsPermissions() {
        for (String table : List.of("dividend_plans", "dividend_statements", "dividend_statement_lines")) {
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT to_regclass('" + table + "')::text", String.class)).isEqualTo(table);
        }
        List<String> permissions = jdbcTemplate.queryForList(
                "SELECT permission_code FROM permissions WHERE permission_code LIKE 'finance.dividend.%'",
                String.class);
        assertThat(permissions).contains("finance.dividend.view", "finance.dividend.manage");
        List<String> adminGrants = jdbcTemplate.queryForList(
                "SELECT permission_code FROM role_permissions WHERE role_id = 'role_platform_admin' "
                        + "AND permission_code LIKE 'finance.dividend.%'",
                String.class);
        assertThat(adminGrants).contains("finance.dividend.view", "finance.dividend.manage");
    }

    @Test
    void appendOnlyTriggerBlocksLineUpdate() {
        repository.insertPlan(plan("divp_jdbc_ao", "monthly"));
        repository.insertStatement(statement("divs_jdbc_ao_u", "divp_jdbc_ao",
                100L, 0L, 0L, 100L, 100L, 80L, 20L, 4L, 16L));
        repository.insertLine(new DividendStatementLineRecord(
                "divs_jdbc_ao_u_1", "divs_jdbc_ao_u", MERCHANT, 1, "net_profit", 100L, "测试", Instant.now()));
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE dividend_statement_lines SET amount_cent = 999 WHERE id = 'divs_jdbc_ao_u_1'"))
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void appendOnlyTriggerBlocksLineDelete() {
        repository.insertPlan(plan("divp_jdbc_ao_d", "monthly"));
        repository.insertStatement(statement("divs_jdbc_ao_d", "divp_jdbc_ao_d",
                100L, 0L, 0L, 100L, 100L, 80L, 20L, 4L, 16L));
        repository.insertLine(new DividendStatementLineRecord(
                "divs_jdbc_ao_d_1", "divs_jdbc_ao_d", MERCHANT, 1, "net_profit", 100L, "测试", Instant.now()));
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM dividend_statement_lines WHERE id = 'divs_jdbc_ao_d_1'"))
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void identityCheckRejectsInconsistentStatement() {
        repository.insertPlan(plan("divp_jdbc_chk", "monthly"));
        // net ≠ revenue − cost − expense → chk_ds_net_identity 拒绝落库（数字恒等 DB 硬保证）。
        assertInsertRejected(() -> repository.insertStatement(statement("divs_jdbc_chk_bad", "divp_jdbc_chk",
                100L, 0L, 0L, 999L, 999L, 799L, 200L, 40L, 160L)), "chk_ds_net_identity");
        // 劈分不恒等（platform+gross ≠ distributable）同样拒绝。
        assertInsertRejected(() -> repository.insertStatement(statement("divs_jdbc_chk_bad2", "divp_jdbc_chk",
                100L, 0L, 0L, 100L, 100L, 80L, 30L, 6L, 24L)), "chk_ds_split_identity");
    }

    @Test
    void uniqueActiveStatementPerPeriodAndPlanPerMerchant() {
        repository.insertPlan(plan("divp_jdbc_uq", "monthly"));
        // 同商户第二个 active 方案：部分唯一索引拒绝（insertPlan 吞 DuplicateKeyException 返回 false）。
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            Savepoint savepoint = connection.setSavepoint();
            assertThat(repository.insertPlan(plan("divp_jdbc_uq_2", "monthly"))).isFalse();
            connection.rollback(savepoint);
            connection.releaseSavepoint(savepoint);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
        repository.insertStatement(statement("divs_jdbc_uq_1", "divp_jdbc_uq",
                100L, 0L, 0L, 100L, 100L, 80L, 20L, 4L, 16L));
        // 同期同商户同类别第二张未作废单：uq_dividend_statements_period 拒绝。
        assertInsertRejected(() -> repository.insertStatement(statement("divs_jdbc_uq_2", "divp_jdbc_uq",
                200L, 0L, 0L, 200L, 200L, 160L, 40L, 8L, 32L)), "uq_dividend_statements_period");
        // 旧单作废后可重出（重算=旧单 voided + 新单）。
        repository.markVoided("divs_jdbc_uq_1", "usr_div_jdbc", Instant.now());
        repository.insertStatement(statement("divs_jdbc_uq_3", "divp_jdbc_uq",
                200L, 0L, 0L, 200L, 200L, 160L, 40L, 8L, 32L));
        assertThat(repository.findActiveStatement(MERCHANT, "official", "2026-07").orElseThrow().id())
                .isEqualTo("divs_jdbc_uq_3");
    }

    @Test
    void endToEndComputeMatchesThreeSourcesAndApprovalCreatesPayable() {
        // ── 种子三来源（月中时点，落 2026-07 经营日窗）────────────────────────
        Instant inWindow = Instant.parse("2026-07-15T12:00:00Z");
        java.time.OffsetDateTime ts = java.time.OffsetDateTime.ofInstant(inWindow, java.time.ZoneOffset.UTC);
        // 收入·售票：issued_entitlements 权益分配收入 2500.00 − ticket_refunds 退款 500.00。
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)
                VALUES ('tt_div_jdbc', '分红JDBC测试票', 'fixed', 'active', ?, ?)
                """, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                        default_value_cent, status, created_at, updated_at)
                VALUES ('et_div_jdbc', '分红JDBC权益', 'admission', 1, 0, 'active', ?, ?)
                """, ts, ts);
        // fk_issued_entitlements_order（V159 NOT VALID 对新写强制）：权益前先落票订单父行
        // （TicketTransferJdbcIntegrationTests 同款种子范式）。@Transactional 回滚自动清理，无 FK 序问题。
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent,
                        pay_status, issue_status, status, created_at, updated_at)
                VALUES ('order_div_jdbc', 'ONO_DIV_JDBC', 'tt_div_jdbc', 1, 'web', 250000,
                        'paid', 'issued', 'created', ?, ?)
                """, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id,
                        entitlement_code, entitlement_type, merchant_id, total_times, used_times,
                        allocated_revenue_cent, status, valid_from, valid_to, created_at, updated_at)
                VALUES ('ie_div_jdbc', 'tt_div_jdbc', 'order_div_jdbc', 'et_div_jdbc', 'code_div_jdbc',
                        'admission', ?, 1, 0, 250000, 'active', ?, ?, ?, ?)
                """, MERCHANT, ts, ts, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO ticket_refunds (id, target_id, refund_amount_cent, reason, refund_channel,
                        status, merchant_id, created_at, updated_at)
                VALUES ('tr_div_jdbc', 'ie_div_jdbc', 50000, '分红JDBC测试退款', 'original', 'completed', ?, ?, ?)
                """, MERCHANT, ts, ts);
        // 成本：V296 出库流水（仅源库位）带成本 600.00。
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at)
                VALUES ('item_div_jdbc', '分红JDBC物料', 'drink', '瓶', 'active', ?, ?)
                """, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_movements (id, movement_type, item_id, source_location_id,
                        target_location_id, quantity, merchant_id, unit_cost_cent, total_cost_cent,
                        status, created_at)
                VALUES ('mv_div_jdbc', 'outbound', 'item_div_jdbc', 'loc_div_jdbc', NULL, 10, ?, 6000, 60000,
                        'completed', ?)
                """, MERCHANT, ts);
        // 费用：V297 台账人工费 300.00（月份桶 2026-07）。
        jdbcTemplate.update("""
                INSERT INTO merchant_expense_entries (id, merchant_id, period, expense_type, amount_cent,
                        source, source_ref, note, operator_user_id, created_at)
                VALUES ('mex_div_jdbc', ?, '2026-07', 'labor', 30000, 'manual', NULL, '分红JDBC测试人工费',
                        'usr_div_jdbc', ?)
                """, MERCHANT, ts);
        // 费用·租金（2026-07-18 拍板计入分红基数）：合同租金应收单 400.00 → 真链路 attachContractRent 挂进台账。
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,
                        amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period,
                        status, created_at, updated_at, merchant_id)
                VALUES ('arap_rent_div_jdbc', 'RENT-DIV-JDBC', 'receivable', ?, 'contract_rent',
                        'contract_div_jdbc|2026-07', 40000, 0, 40000, 'CNY', '2026-07-01', '2026-07',
                        'pending_collection', ?, ?, ?)
                """, MERCHANT, ts, ts, MERCHANT);
        int rentAttached = merchantExpenseRepository.attachContractRent("2026-07", "usr_div_jdbc", inWindow);
        assertThat(rentAttached).isEqualTo(1);
        // 重跑幂等：同一租金应收单不重复计入。
        assertThat(merchantExpenseRepository.attachContractRent("2026-07", "usr_div_jdbc", inWindow)).isZero();

        // ── 期间作业（月度正式方案，label=2026-07）────────────────────────────
        DividendService service = serviceWithPlatformActor("usr_div_jdbc");
        repository.insertPlan(plan("divp_jdbc_e2e", "monthly"));
        DividendComputeResultRecord result = service.compute(
                new DividendComputeRequest("div-jdbc-compute-1", "2026-07", "official", MERCHANT));
        assertThat(result.computed()).hasSize(1);
        DividendStatementRecord statement = result.computed().get(0);

        // 数字恒等三来源：收入 = 250000−50000；成本 = 60000；费用 = 人工30000+租金40000；净利 = 70000。
        assertThat(statement.revenueCent()).isEqualTo(200_000L);
        assertThat(statement.costCent()).isEqualTo(60_000L);
        assertThat(statement.expenseCent()).isEqualTo(70_000L);
        assertThat(statement.netProfitCent()).isEqualTo(70_000L);
        assertThat(statement.platformShareCent()).isEqualTo(56_000L);
        assertThat(statement.principalGrossCent()).isEqualTo(14_000L);
        assertThat(statement.taxCent()).isEqualTo(2_800L);
        assertThat(statement.principalNetCent()).isEqualTo(11_200L);

        // 三来源独立复核：单头金额与来源台账逐项一致。
        Long ticketAlloc = jdbcTemplate.queryForObject(
                "SELECT SUM(allocated_revenue_cent) FROM issued_entitlements WHERE merchant_id = ?",
                Long.class, MERCHANT);
        Long ticketRefund = jdbcTemplate.queryForObject(
                "SELECT SUM(refund_amount_cent) FROM ticket_refunds WHERE merchant_id = ?", Long.class, MERCHANT);
        Long cost = jdbcTemplate.queryForObject(
                "SELECT SUM(total_cost_cent) FROM inventory_stock_movements WHERE merchant_id = ? "
                        + "AND source_location_id IS NOT NULL AND target_location_id IS NULL",
                Long.class, MERCHANT);
        Long expense = jdbcTemplate.queryForObject(
                "SELECT SUM(amount_cent) FROM merchant_expense_entries WHERE merchant_id = ?", Long.class, MERCHANT);
        assertThat(statement.revenueCent()).isEqualTo(ticketAlloc - ticketRefund);
        assertThat(statement.costCent()).isEqualTo(cost);
        assertThat(statement.expenseCent()).isEqualTo(expense);

        // 明细行落表且来源行合计 = 净利润（含独立的租金费用行）。
        List<DividendStatementLineRecord> lines = repository.listLines(statement.id(), null);
        long sourceSum = lines.stream()
                .filter(l -> !List.of("net_profit", "loss_carryforward", "platform_share", "principal_gross",
                        "tax_withheld", "principal_net", "arrears_offset", "reserve_withheld", "payout")
                        .contains(l.lineType()))
                .mapToLong(DividendStatementLineRecord::amountCent).sum();
        assertThat(sourceSum).isEqualTo(statement.netProfitCent());
        assertThat(lines).anyMatch(l -> l.lineType().equals("expense_rent_allocation") && l.amountCent() == -40_000L);
        // V303 默认全关：实发行落痕 = 税后应得，无扣除行。
        assertThat(lines).anyMatch(l -> l.lineType().equals("payout") && l.amountCent() == 11_200L);
        assertThat(lines).noneMatch(l -> l.lineType().equals("loss_carryforward"));

        // ── 审批链：draft → pending_review → approved 挂应付（真 FinanceService 端口）──
        // I-1 职责分离：提交与审批用不同 actor；同 actor 审批必须被拒。
        service.submit(statement.id(), new DividendActionRequest("div-jdbc-submit-1", null));
        assertThatThrownBy(() -> service.approve(statement.id(),
                new DividendActionRequest("div-jdbc-selfapprove-1", null)))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .satisfies(e -> assertThat(((com.complex.platform.common.api.ApiException) e).code())
                        .isEqualTo("SELF_APPROVAL_FORBIDDEN"));
        DividendService approverService = serviceWithPlatformActor("usr_div_jdbc_approver");
        DividendStatementRecord approved = approverService.approve(statement.id(),
                new DividendActionRequest("div-jdbc-approve-1", null));
        assertThat(approved.status()).isEqualTo("approved");
        assertThat(approved.arApItemId()).isNotBlank();
        Map<String, Object> payable = jdbcTemplate.queryForMap(
                "SELECT direction, counterparty_id, source_type, source_id, amount_cent, status "
                        + "FROM ar_ap_items WHERE id = ?", approved.arApItemId());
        assertThat(payable.get("direction")).isEqualTo("payable");
        assertThat(payable.get("counterparty_id")).isEqualTo("usr_principal_jdbc");
        assertThat(payable.get("source_type")).isEqualTo("dividend_payout");
        assertThat(payable.get("source_id")).isEqualTo(statement.id());
        assertThat(((Number) payable.get("amount_cent")).longValue()).isEqualTo(11_200L);
        assertThat(payable.get("status")).isEqualTo("pending_payment");

        // 已 approved 的单重复审批 → 服务层前置检查 requireStatus 拒（业务 409 非 500）。
        // 注意：这条命中的是服务层预检，不是 SQL 守卫——SQL WHERE 期望前态的负路径由下面的直调仓储用例覆盖。
        assertThatThrownBy(() -> approverService.approve(statement.id(),
                new DividendActionRequest("div-jdbc-approve-2", null)))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .satisfies(e -> assertThat(((com.complex.platform.common.api.ApiException) e).code())
                        .isEqualTo("DIVIDEND_STATE_INVALID"));

        // C-1 SQL 守卫负路径真覆盖：绕过服务层直调仓储，对已 approved 单再 markApproved →
        // UPDATE ... AND status='pending_review' 必须 0 行且状态/审批留痕不被盲改（并发双付窗口的 DB 层兜底）。
        assertThat(repository.markApproved(statement.id(), "usr_div_jdbc_other", "arap_fake", Instant.now()))
                .isZero();
        DividendStatementRecord untouched = repository.findStatementById(statement.id()).orElseThrow();
        assertThat(untouched.status()).isEqualTo("approved");
        assertThat(untouched.arApItemId()).isEqualTo(approved.arApItemId());
        assertThat(untouched.approvedBy()).isEqualTo("usr_div_jdbc_approver");

        // 标记发放收口。
        DividendStatementRecord paid = approverService.markPaid(statement.id(),
                new DividendActionRequest("div-jdbc-paid-1", null));
        assertThat(paid.status()).isEqualTo("paid");
    }

    /** V303 扣除项 DB CHECK 拦截：发放额恒等 / 亏损结转链恒等不满足的单不得落库。 */
    @Test
    void deductionCheckConstraintsRejectInconsistentStatements() {
        repository.insertPlan(plan("divp_jdbc_ded", "monthly"));
        // 发放额恒等破坏：payout ≠ principal_net − arrears − reserve → chk_ds_payout_identity。
        assertInsertRejected(() -> repository.insertStatement(deductionStatement("divs_jdbc_ded_pay",
                "divp_jdbc_ded", 1000L, 1000L, 0L, 1000L, 0L, 1000L,
                false, 0L, 0L, 0L, 0L, 0L, 999L)), "chk_ds_payout_identity");
        // 亏损结转链恒等破坏：启用时 after 应 = in − applied + max(−净利,0)=500，写 0 → chk_ds_lcf_after。
        assertInsertRejected(() -> repository.insertStatement(deductionStatement("divs_jdbc_ded_lcf",
                "divp_jdbc_ded", -500L, 0L, 0L, 0L, 0L, 0L,
                true, 0L, 0L, 0L, 0L, 0L, 0L)), "chk_ds_lcf_after");
    }

    /**
     * V303 亏损结转端到端（累计更新时点验证）：启用亏损结转的方案，某月亏损审批后结转链余额冻结，
     * 下月计算单从「已结正式单」projection 读到期初累计并冻结在 loss_carryforward_in_cent（无需另建可变台账）。
     */
    @Test
    void lossCarryforwardProjectionCarriesAcrossApprovedPeriods() {
        Instant ts = Instant.parse("2026-07-15T12:00:00Z");
        // 亏损结转开的月度方案。
        DividendPlanRecord lcfPlan = new DividendRecords.DividendPlanRecord("divp_jdbc_lcf", MERCHANT,
                "usr_principal_jdbc", "主理人甲", 8000, 2000, "monthly", List.of("bar", "ticket"), true,
                List.of("rent_allocation", "energy", "property_fee", "labor", "amortization", "other"),
                null, "active", null, "usr_div_jdbc", ts, ts,
                true, false, false, 0, 0L);
        repository.insertPlan(lcfPlan);
        // 2026-07 亏损 500.00：仅一笔人工费，无收入 → 净利 −50000。
        jdbcTemplate.update("""
                INSERT INTO merchant_expense_entries (id, merchant_id, period, expense_type, amount_cent,
                        source, source_ref, note, operator_user_id, created_at)
                VALUES ('mex_jdbc_lcf', ?, '2026-07', 'labor', 50000, 'manual', NULL, '亏损结转JDBC', 'usr_div_jdbc', ?)
                """, MERCHANT, java.time.OffsetDateTime.ofInstant(ts, java.time.ZoneOffset.UTC));

        DividendService service = serviceWithPlatformActor("usr_div_jdbc");
        DividendStatementRecord july = service.compute(
                new DividendComputeRequest("lcf-jdbc-c1", "2026-07", "official", MERCHANT)).computed().get(0);
        assertThat(july.netProfitCent()).isEqualTo(-50_000L);
        assertThat(july.distributableCent()).isZero();
        assertThat(july.lossCarryforwardInCent()).isZero();
        assertThat(july.lossCarryforwardAfterCent()).isEqualTo(50_000L);
        // 审批 7 月（实发 0，不挂应付），结转链余额生效。
        service.submit(july.id(), new DividendActionRequest("lcf-jdbc-s1", null));
        DividendService approver = serviceWithPlatformActor("usr_div_jdbc_approver");
        approver.approve(july.id(), new DividendActionRequest("lcf-jdbc-a1", null));

        // 2026-08 无收支 → 净利 0；从已结 7 月单读到期初累计 50000 并冻结，继续结转。
        DividendStatementRecord august = service.compute(
                new DividendComputeRequest("lcf-jdbc-c2", "2026-08", "official", MERCHANT)).computed().get(0);
        assertThat(august.netProfitCent()).isZero();
        assertThat(august.lossCarryforwardInCent()).isEqualTo(50_000L);
        assertThat(august.lossCarryforwardAppliedCent()).isZero();
        assertThat(august.lossCarryforwardAfterCent()).isEqualTo(50_000L);
        assertThat(august.distributableCent()).isZero();
    }

    // ── V303 欠款抵扣真实结算端到端（真调 FinanceService.settleArApItem，非 mock）──

    /** 种子售票分量收入（allocated_revenue_cent），落 2026-07 窗；无退款/成本/费用 → 净利=收入。 */
    private void seedTicketRevenue(String suffix, long allocatedCent, java.time.OffsetDateTime ts) {
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)
                VALUES (?, '分红欠款抵扣票', 'fixed', 'active', ?, ?)
                """, "tt_" + suffix, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                        default_value_cent, status, created_at, updated_at)
                VALUES (?, '分红欠款抵扣权益', 'admission', 1, 0, 'active', ?, ?)
                """, "et_" + suffix, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent,
                        pay_status, issue_status, status, created_at, updated_at)
                VALUES (?, ?, ?, 1, 'web', ?, 'paid', 'issued', 'created', ?, ?)
                """, "order_" + suffix, "ONO_" + suffix, "tt_" + suffix, allocatedCent, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id,
                        entitlement_code, entitlement_type, merchant_id, total_times, used_times,
                        allocated_revenue_cent, status, valid_from, valid_to, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'admission', ?, 1, 0, ?, 'active', ?, ?, ?, ?)
                """, "ie_" + suffix, "tt_" + suffix, "order_" + suffix, "et_" + suffix, "code_" + suffix,
                MERCHANT, allocatedCent, ts, ts, ts, ts);
    }

    /** 种子该商户一笔未清应收（direction=receivable, counterparty=merchant）。 */
    private void seedReceivable(String id, String itemNo, long unpaidCent, java.time.OffsetDateTime ts) {
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,
                        amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period,
                        status, created_at, updated_at, merchant_id)
                VALUES (?, ?, 'receivable', ?, 'manual', ?, ?, 0, ?, 'CNY', '2026-07-01', '2026-07',
                        'pending_collection', ?, ?, ?)
                """, id, itemNo, MERCHANT, id, unpaidCent, unpaidCent, ts, ts, MERCHANT);
    }

    private static DividendPlanRecord arrearsPlan(String id) {
        Instant now = Instant.parse("2026-07-18T00:00:00Z");
        return new DividendRecords.DividendPlanRecord(id, MERCHANT, "usr_principal_jdbc", "主理人甲",
                8000, 2000, "monthly", List.of("bar", "ticket"), true, List.of("labor"),
                null, "active", null, "usr_div_jdbc", now, now,
                false, true, false, 0, 0L);   // arrears_offset_enabled=true
    }

    /**
     * 欠款抵扣端到端：种子售票收入 100 万 + 未清应收两笔（6 万+4 万）→ 开欠款抵扣算单（冻结抵扣 10 万、实发 6 万）→
     * 审批时真调 settleArApItem 逐项 FOR UPDATE 结算两笔应收（dividend_offset 收款留痕）→ 按实发 6 万挂应付。
     */
    @Test
    void arrearsOffsetSettlesReceivablesAndPaysNetAtApprove() {
        java.time.OffsetDateTime ts = java.time.OffsetDateTime.parse("2026-07-15T12:00:00Z");
        seedTicketRevenue("arr", 1_000_000L, ts);
        seedReceivable("arap_r1_arr", "AR-ARR-001", 60_000L, ts);
        seedReceivable("arap_r2_arr", "AR-ARR-002", 40_000L, ts);
        repository.insertPlan(arrearsPlan("divp_jdbc_arr"));

        DividendService service = serviceWithPlatformActor("usr_div_jdbc");
        DividendStatementRecord s = service.compute(
                new DividendComputeRequest("arr-jdbc-c1", "2026-07", "official", MERCHANT)).computed().get(0);
        assertThat(s.principalNetCent()).isEqualTo(160_000L);
        assertThat(s.arrearsOffsetCent()).isEqualTo(100_000L);
        assertThat(s.payoutCent()).isEqualTo(60_000L);
        // 逐项留痕：两笔应收各一行。
        assertThat(repository.listLines(s.id(), null))
                .filteredOn(l -> l.lineType().equals("arrears_offset")).hasSize(2);

        service.submit(s.id(), new DividendActionRequest("arr-jdbc-s1", null));
        DividendService approver = serviceWithPlatformActor("usr_div_jdbc_approver");
        DividendStatementRecord approved = approver.approve(s.id(), new DividendActionRequest("arr-jdbc-a1", null));
        assertThat(approved.status()).isEqualTo("approved");

        // 两笔应收被真实结算（unpaid=0, settled），各有一笔 dividend_offset 收款留痕（bank_flow_ref=计算单号）。
        for (String rid : List.of("arap_r1_arr", "arap_r2_arr")) {
            Map<String, Object> item = jdbcTemplate.queryForMap(
                    "SELECT unpaid_amount_cent, status FROM ar_ap_items WHERE id = ?", rid);
            assertThat(((Number) item.get("unpaid_amount_cent")).longValue()).isZero();
            assertThat(item.get("status")).isEqualTo("settled");
        }
        Long offsetPayments = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM ar_ap_payments p JOIN ar_ap_items i ON i.id = p.ar_ap_item_id
                 WHERE i.merchant_id = ? AND p.payment_method = 'dividend_offset' AND p.bank_flow_ref = ?
                """, Long.class, MERCHANT, s.id());
        assertThat(offsetPayments).isEqualTo(2L);
        // 抵扣后余额 6 万挂应付（payout=principal_net−arrears）。
        Map<String, Object> payable = jdbcTemplate.queryForMap(
                "SELECT amount_cent FROM ar_ap_items WHERE source_type = 'dividend_payout' AND source_id = ?", s.id());
        assertThat(((Number) payable.get("amount_cent")).longValue()).isEqualTo(60_000L);
    }

    /**
     * 欠款抵扣并发部分清偿：冻结抵扣 10 万后应收被并发清偿至仅剩 3 万 < 冻结额 → applyDividendOffset 预检不足一分不结算 →
     * 抛 DIVIDEND_ARREARS_CHANGED；应收不半结（仍 3 万未清）、不挂应付、单不进 approved（整体回退）。
     */
    @Test
    void arrearsOffsetConflictAtApproveRollsBackNoHalfSettle() {
        java.time.OffsetDateTime ts = java.time.OffsetDateTime.parse("2026-07-15T12:00:00Z");
        seedTicketRevenue("arr2", 1_000_000L, ts);
        seedReceivable("arap_r_arr2", "AR-ARR2-001", 100_000L, ts);
        repository.insertPlan(arrearsPlan("divp_jdbc_arr2"));

        DividendService service = serviceWithPlatformActor("usr_div_jdbc");
        DividendStatementRecord s = service.compute(
                new DividendComputeRequest("arr2-jdbc-c1", "2026-07", "official", MERCHANT)).computed().get(0);
        assertThat(s.arrearsOffsetCent()).isEqualTo(100_000L);

        // 抵扣期间应收被并发部分清偿：未清 10 万 → 3 万。
        jdbcTemplate.update("""
                UPDATE ar_ap_items SET paid_amount_cent = 70000, unpaid_amount_cent = 30000, status = 'partial_paid'
                 WHERE id = 'arap_r_arr2'
                """);

        service.submit(s.id(), new DividendActionRequest("arr2-jdbc-s1", null));
        DividendService approver = serviceWithPlatformActor("usr_div_jdbc_approver");
        assertThatThrownBy(() -> approver.approve(s.id(), new DividendActionRequest("arr2-jdbc-a1", null)))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .satisfies(e -> assertThat(((com.complex.platform.common.api.ApiException) e).code())
                        .isEqualTo("DIVIDEND_ARREARS_CHANGED"));
        // 应收不半结（预检不足一分不结算，仍 3 万未清）。
        Long unpaid = jdbcTemplate.queryForObject(
                "SELECT unpaid_amount_cent FROM ar_ap_items WHERE id = 'arap_r_arr2'", Long.class);
        assertThat(unpaid).isEqualTo(30_000L);
        // 未挂应付、单未进 approved（整体回退，不半发放）。
        Long payables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ar_ap_items WHERE source_type = 'dividend_payout' AND source_id = ?",
                Long.class, s.id());
        assertThat(payables).isZero();
        assertThat(repository.findStatementById(s.id()).orElseThrow().status()).isEqualTo("pending_review");
    }
}
