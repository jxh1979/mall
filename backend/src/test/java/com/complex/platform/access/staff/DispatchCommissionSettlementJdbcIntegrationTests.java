package com.complex.platform.access.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.staff.dto.StaffCommissionSettlementCloseRequest;
import com.complex.platform.access.staff.dto.StaffDispatchCreateRequest;
import com.complex.platform.access.staff.dto.StaffDispatchEndRequest;
import com.complex.platform.access.staff.dto.StaffPayrollCloseRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-07 派驻/营销人员提成：酒吧按月拨付所属主体、列入所属主体工资单（BSM「营销人员的归属与报酬」：「营销在某家酒吧促成的订单产生的提成，
 * 由该酒吧按月拨付给娱乐平台公司，列入营销本人的工资单由娱乐平台公司统一发放并代扣个人所得税；拨付作为平台内部各主体之间的普通内部结算
 * 应收应付，按三个付账日结清」；BSM「派驻人员的成本与报酬」：「每一名派驻人员的所属主体、派驻酒吧、派驻起止时间，以及由人员所属主体支付和
 * 由酒吧承担并拨付的各项金额，均须分别记录、可分别统计，不得只保存合并后的净额」）。
 *
 * <p>走真实 Flyway 迁移库，{@code @Transactional} 每用例回滚；派驻登记/结束、提成月结、薪资月结走正路 {@code StaffService}，
 * 追 {@code staff_dispatch_records}、{@code bar_commission_settlements}、{@code ar_ap_items}、{@code staff_payroll_statements} 列。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class DispatchCommissionSettlementJdbcIntegrationTests {

    private static final String MONTH = "2026-09";

    @Autowired private StaffService staffService;
    @Autowired private JdbcStaffRepository staffRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String projectId;
    private String barMerchantId;
    private String platformCompanyId;
    private String barCompanyId;
    private String dispatchedUserId;
    private String ownStaffUserId;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        projectId = "prj_cg07_" + suffix;
        barMerchantId = "mch_cg07_bar_" + suffix;
        platformCompanyId = "ent_cg07_platform_" + suffix;
        barCompanyId = "ent_cg07_bar_" + suffix;
        dispatchedUserId = "usr_cg07_a_" + suffix;
        ownStaffUserId = "usr_cg07_b_" + suffix;
        staffUser(dispatchedUserId, platformCompanyId);
        staffUser(ownStaffUserId, barCompanyId);
        // 酒吧账单与提成计提：派驻员工 A 两笔有效（3000 + 2000）+ 一笔作废（9999）；酒吧自有员工 B 一笔 1500。
        String tableId = "tbl_cg07_" + suffix;
        jdbcTemplate.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '派驻提成测试卡座', 'booth', 'available', now(), now())", tableId, barMerchantId, tableId);
        commission("a1", tableId, dispatchedUserId, 3_000L, "calculated", "2026-09-10T12:00:00Z");
        commission("a2", tableId, dispatchedUserId, 2_000L, "calculated", "2026-09-20T12:00:00Z");
        commission("a3", tableId, dispatchedUserId, 9_999L, "void", "2026-09-21T12:00:00Z");
        commission("b1", tableId, ownStaffUserId, 1_500L, "calculated", "2026-09-12T12:00:00Z");
        authenticateWithStaffPermissions();
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 月结：按（酒吧 → 所属主体 → 月）生成一条内部结算应付，金额＝该月派驻人员提成合计；重复执行不重复挂账；无派驻记录的员工不进通道。 */
    @Test
    void monthlySettlementCreatesOnePayableFromBarToStaffCompany() {
        staffService.createDispatch(new StaffDispatchCreateRequest("cg07-dsp-" + suffix, dispatchedUserId, barMerchantId,
                "2026-09-01", "派驻到酒吧"));

        Map<String, Object> first = staffService.closeCommissionSettlement(
                new StaffCommissionSettlementCloseRequest("cg07-close-1-" + suffix, MONTH));
        assertThat(first.get("settlements")).isEqualTo(1);

        List<Map<String, Object>> settlements = jdbcTemplate.queryForList(
                "SELECT id, company_id, amount_cent, calculation_count, staff_count, due_date, ar_ap_item_id"
                        + " FROM bar_commission_settlements WHERE month = ? AND merchant_id = ?", MONTH, barMerchantId);
        assertThat(settlements).hasSize(1);
        Map<String, Object> settlement = settlements.get(0);
        assertThat(settlement.get("company_id")).isEqualTo(platformCompanyId);
        assertThat(settlement.get("amount_cent")).isEqualTo(5_000L);
        assertThat(settlement.get("calculation_count")).isEqualTo(2);
        assertThat(settlement.get("staff_count")).isEqualTo(1);
        // 到期＝月末后的下一个付账日（出厂 5 / 15 / 25 → 10 月 5 日）。
        assertThat(((java.sql.Date) settlement.get("due_date")).toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 5));

        List<Map<String, Object>> payables = jdbcTemplate.queryForList(
                "SELECT id, direction, counterparty_id, amount_cent, due_date, status, merchant_id, source_id, accounting_period"
                        + " FROM ar_ap_items WHERE source_type = 'dispatch_commission_transfer' AND merchant_id = ?",
                barMerchantId);
        assertThat(payables).hasSize(1);
        Map<String, Object> payable = payables.get(0);
        assertThat(payable.get("id")).isEqualTo(settlement.get("ar_ap_item_id"));
        assertThat(payable.get("source_id")).isEqualTo(settlement.get("id"));
        assertThat(payable.get("accounting_period")).isEqualTo(MONTH);
        assertThat(jdbcTemplate.queryForObject("SELECT amount_cent FROM ar_ap_item_sources WHERE ar_ap_item_id = ?",
                Long.class, payable.get("id"))).isEqualTo(5_000L);
        assertThat(payable.get("direction")).isEqualTo("payable");
        assertThat(payable.get("counterparty_id")).isEqualTo(platformCompanyId);
        assertThat(payable.get("amount_cent")).isEqualTo(5_000L);
        assertThat(payable.get("merchant_id")).isEqualTo(barMerchantId);
        assertThat(payable.get("status")).isEqualTo("pending_payment");
        assertThat(((java.sql.Date) payable.get("due_date")).toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        // 计提不再按单对员工本人挂应付。
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ar_ap_items WHERE source_type = 'employee_commission'"
                + " AND counterparty_id IN (?, ?)", Integer.class, dispatchedUserId, ownStaffUserId)).isZero();
        // 酒吧自有员工（无派驻记录）不进本通道。
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bar_commission_settlements WHERE month = ? AND company_id = ?",
                Integer.class, MONTH, barCompanyId)).isZero();

        Map<String, Object> second = staffService.closeCommissionSettlement(
                new StaffCommissionSettlementCloseRequest("cg07-close-2-" + suffix, MONTH));
        assertThat(second.get("settlements")).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bar_commission_settlements WHERE month = ? AND merchant_id = ?",
                Integer.class, MONTH, barMerchantId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ar_ap_items WHERE source_type = 'dispatch_commission_transfer'"
                + " AND merchant_id = ?", Integer.class, barMerchantId)).isEqualTo(1);
    }

    /** 工资单：提成栏仍汇总该月酒吧提成（不变）；发放主体＝人员所属主体。 */
    @Test
    void payrollStatementCarriesPayerCompanyAndKeepsCommissionColumn() {
        staffService.closePayroll(new StaffPayrollCloseRequest("cg07-payroll-" + suffix, MONTH));

        Map<String, Object> dispatched = jdbcTemplate.queryForMap(
                "SELECT commission_cent, payer_company_id FROM staff_payroll_statements WHERE user_id = ? AND month = ?",
                dispatchedUserId, MONTH);
        assertThat(dispatched.get("commission_cent")).isEqualTo(5_000L);
        assertThat(dispatched.get("payer_company_id")).isEqualTo(platformCompanyId);
        Map<String, Object> own = jdbcTemplate.queryForMap(
                "SELECT commission_cent, payer_company_id FROM staff_payroll_statements WHERE user_id = ? AND month = ?",
                ownStaffUserId, MONTH);
        assertThat(own.get("commission_cent")).isEqualTo(1_500L);
        assertThat(own.get("payer_company_id")).isEqualTo(barCompanyId);
    }

    /** 派驻记录登记 / 重复登记拒绝 / 结束 / 列表。 */
    @Test
    void dispatchRecordLifecycle() {
        StaffDispatchRecord created = staffService.createDispatch(new StaffDispatchCreateRequest(
                "cg07-dsp-c-" + suffix, dispatchedUserId, barMerchantId, "2026-09-01", "派驻到酒吧"));
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT company_id, merchant_id, start_date, end_date, status FROM staff_dispatch_records WHERE id = ?", created.id());
        assertThat(row.get("company_id")).isEqualTo(platformCompanyId);
        assertThat(row.get("merchant_id")).isEqualTo(barMerchantId);
        assertThat(row.get("status")).isEqualTo("active");
        assertThat(row.get("end_date")).isNull();

        assertThatThrownBy(() -> staffService.createDispatch(new StaffDispatchCreateRequest(
                "cg07-dsp-dup-" + suffix, dispatchedUserId, barMerchantId, "2026-09-02", "重复")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("STAFF_DISPATCH_ALREADY_ACTIVE");

        StaffDispatchRecord ended = staffService.endDispatch(created.id(),
                new StaffDispatchEndRequest("cg07-dsp-end-" + suffix, "2026-09-30", "派驻期满"));
        assertThat(ended.status()).isEqualTo("ended");
        assertThat(jdbcTemplate.queryForMap("SELECT end_date, status FROM staff_dispatch_records WHERE id = ?", created.id()))
                .containsEntry("status", "ended");
        assertThat(((java.sql.Date) jdbcTemplate.queryForMap("SELECT end_date FROM staff_dispatch_records WHERE id = ?",
                created.id()).get("end_date")).toLocalDate()).isEqualTo(LocalDate.of(2026, 9, 30));

        assertThat(staffService.listDispatches(dispatchedUserId, null).items())
                .extracting(StaffDispatchRecord::id).containsExactly(created.id());
    }

    // ---------- helpers ----------

    private void staffUser(String userId, String companyId) {
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'merchant_staff', 'org_1', ?, 'active', now())", userId, "u_" + userId, barMerchantId);
        Instant now = Instant.now();
        staffRepository.saveProfile(new StaffProfileRecord("prof_" + userId, projectId, barMerchantId, userId,
                "派驻提成测试员工", "139" + String.format("%08d", Math.abs(userId.hashCode()) % 100_000_000), null, null,
                "approved", "usr_hr", now, null, "active", now, now,
                null, null, "none", null, null, null, true, "employed", companyId, "company_self"));
    }

    private void commission(String key, String tableId, String staffUserId, long amountCent, String status, String createdAt) {
        String orderId = "ord_cg07_" + key + "_" + suffix;
        String billId = "bill_cg07_" + key + "_" + suffix;
        Timestamp at = Timestamp.from(Instant.parse(createdAt));
        jdbcTemplate.update("INSERT INTO bar_orders (id, bar_id, table_id, order_source, status, total_amount_cent, currency,"
                + " created_at, updated_at, order_type) VALUES (?, ?, ?, 'staff', 'settled', 100000, 'CNY', ?, ?, 'standard')",
                orderId, barMerchantId, tableId, at, at);
        jdbcTemplate.update("INSERT INTO bar_bills (id, bill_no, order_id, bar_id, table_id, subtotal_amount_cent, payable_amount_cent,"
                + " paid_amount_cent, currency, payment_status, settlement_status, created_at, updated_at, merchant_id, sales_staff_id)"
                + " VALUES (?, ?, ?, ?, ?, 100000, 100000, 100000, 'CNY', 'paid', 'settled', ?, ?, ?, ?)",
                billId, billId, orderId, barMerchantId, tableId, at, at, barMerchantId, staffUserId);
        jdbcTemplate.update("INSERT INTO bar_commission_calculations (id, bill_id, sales_staff_id, sales_role_type, commission_base_cent,"
                + " commission_rate_bp, commission_amount_cent, status, created_at) VALUES (?, ?, ?, 'staff_sales', 100000, ?, ?, ?, ?)",
                "cm_cg07_" + key + "_" + suffix, billId, staffUserId, (int) (amountCent / 10), amountCent, status, at);
    }

    private void authenticateWithStaffPermissions() {
        String userId = "usr_cg07_admin_" + suffix;
        String roleId = "role_cg07_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_cg07_admin_" + suffix);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId,
                "派驻提成月结测试角色" + System.nanoTime());
        for (String code : List.of("staff.profile.review", "staff.profile.view", "staff.payroll.manage")) {
            jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        AccessActor actor = new AccessActor(userId, "u_cg07_admin_" + suffix, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }
}
