package com.complex.platform.merchantportal;
// 文件版本：V0.05

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractSettlementRecord;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户专区(3.18)读路径的真实库验证:合同仓储的商户维度读取必须按商户谓词隔离——
 * 「我的合同」listContractsByMerchant(+ listContractSpaceIds)与「我的对账」listSettlementsByMerchant
 * 都只回本商户的数据(业务地图「商户侧页面…不看平台全局」)。
 * 商户专区服务层不提供平台全量口径(M020-001:无商户归属账号 fail-closed),故本类不承载平台全量分页用例(M022-001);
 * 仓储 listAllSettlementsPaged 现无生产调用方,去留归合同域。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantPortalJdbcIntegrationTests {

    // 注入公有接口: 运行时是 JdbcContractRepository bean, 商户维度读取走单 SQL 覆写。
    @Autowired
    private com.complex.platform.contract.ContractRepository repository;

    private ContractRecord contract(String id, String merchantId, Instant at) {
        return new ContractRecord(id, merchantId, "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                "fixed_ratio", "by_contract", 0L, Map.of(), List.of(), null, null, List.of(),
                "effective", "proj_1", null, null, null, at, at);
    }

    private ContractSettlementRecord settlement(String id, String contractId, Instant at) {
        return new ContractSettlementRecord(id, contractId, "move_out", "pending_settlement", List.of(),
                null, null, null, "proj_1", at, at);
    }

    // M022-002:「我的合同只见本商户」——真实 SQL 的 merchant_id 谓词(服务层 listMyContracts 的两条读路径)。
    @Test
    void merchantScopeContractsStayIsolated() {
        Instant now = Instant.now();
        repository.saveContract(contract("ctr_mp_e", "mch_mp_e", now));
        repository.saveContract(contract("ctr_mp_f", "mch_mp_f", now));

        assertThat(repository.listContractsByMerchant("mch_mp_e"))
                .extracting(ContractRecord::id).containsExactly("ctr_mp_e");
        assertThat(repository.listContractsByMerchant("mch_mp_f"))
                .extracting(ContractRecord::id).containsExactly("ctr_mp_f");
        assertThat(repository.listContractSpaceIds("ctr_mp_e")).as("未绑定铺位的合同回空列表").isEmpty();
    }

    @Test
    void merchantScopeSettlementsStayIsolated() {
        Instant now = Instant.now();
        repository.saveContract(contract("ctr_mp_c", "mch_mp_c", now));
        repository.saveContract(contract("ctr_mp_d", "mch_mp_d", now));
        repository.saveSettlement(settlement("stl_mp_c", "ctr_mp_c", now));
        repository.saveSettlement(settlement("stl_mp_d", "ctr_mp_d", now));

        // 「我的对账」只见本商户:结算单经合同 merchant_id 谓词隔离。
        assertThat(repository.listSettlementsByMerchant("mch_mp_c"))
                .extracting(ContractSettlementRecord::id).containsExactly("stl_mp_c");
    }

    // ---------- 18E 第 21 条：费用总表与当期综合账单（BSM:1323「商户专区须设一张费用总表……系统须向商户出具一张综合账单」） ----------

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private final String sfx = Long.toString(System.nanoTime(), 36);

    /** 按前端真实请求（GET ?month=）走真实入口与真实授权链；本期＝计费自然月，按往来对方筛本商户（积分发行费按 merchant_id），冲正行计入净额。 */
    @Test
    void feeStatementListsAllFeeNamesAndSumsCurrentMonthBill() throws Exception {
        String m = merchant("mch_fs_" + sfx, "费用总表测试店");
        String other = merchant("mch_fso_" + sfx, "别家店");
        // 固定租金 10000（已付 4000）；另一份固定租金合同的账单作废并冲正，净额 0。
        rentBill("ctr_fs1_" + sfx, m, "{\"rentBillingMode\":\"fixed_only\",\"rentMode\":\"fixed\"}",
                item("fs1_" + sfx, m, m, "contract_rent", 10000, 4000, "2026-09-01", "2026-09", null), false);
        rentBill("ctr_fs3_" + sfx, m, "{\"rentBillingMode\":\"fixed_only\"}",
                item("fs3_" + sfx, m, m, "contract_rent", 7000, 0, "2026-09-01", "2026-09", null), true);
        item("fs3v_" + sfx, m, m, "contract_rent", -7000, 0, "2026-09-01", "2026-09", "fs3_" + sfx);
        // 保底与抽成取高（ENUM:257 营业额抽成模式）→ 营业额抽成租金，金额照原值。
        rentBill("ctr_fs2_" + sfx, m, "{\"rentBillingMode\":\"higher_of\"}",
                item("fs2_" + sfx, m, m, "contract_rent", 5000, 0, "2026-09-01", "2026-09", null), false);
        item("fs4_" + sfx, m, m, "contract_property_fee", 3000, 0, "2026-09-01", "2026-09", null);
        // 物业费计费：台账不带账期、到期在 10 月，经 property_fee_bills.period 归 9 月。
        String pf = item("fs5_" + sfx, m, m, "property_fee", 2000, 0, "2026-10-15", null, null);
        jdbc.update("INSERT INTO property_fee_bills (id, project_id, period, merchant_id, area_square_meter, rate_per_square_meter_cent,"
                + " amount_cent, ar_ap_item_id) VALUES (?, 'proj_fs', '2026-09', ?, 20, 100, 2000, ?)", "pfb_fs_" + sfx, m, pf);
        item("fs6_" + sfx, m, m, "energy_fee", 1500, 0, "2026-09-01", "2026-09", null);
        // 门票综合服务费：台账账期是到期月 10 月，经 internal_payable_schedules.billing_period_start 归 9 月。
        String sb = item("fs7_" + sfx, m, m, "single_bar_service_fee", 800, 0, "2026-10-20", "2026-10", null);
        String rule = "ipdr_fs_" + sfx;
        jdbc.update("INSERT INTO internal_pay_day_rules (id, kind, day_1, day_2, day_3, effective_from, status, reason, submitted_by, decided_by)"
                + " VALUES (?, 'pay_days', 5, 15, 25, DATE '2026-01-01', 'approved', '费用总表回归', 'usr_fs_a', 'usr_fs_b')", rule);
        jdbc.update("INSERT INTO internal_payable_schedules (ar_ap_item_id, route, counterparty_id, billing_period_start, visible_at,"
                + " objection_deadline, designated_pay_day, scheduled_due_date, pay_days_rule_id, route_rule_id, objection_working_days)"
                + " VALUES (?, 'single_bar_service_fee', ?, DATE '2026-09-01', now(), DATE '2026-10-10', DATE '2026-10-20',"
                + " DATE '2026-10-20', ?, ?, 3)", sb, m, rule, rule);
        // 积分发行费：往来对方是销售主体、merchant_id＝本商户。
        item("fs8_" + sfx, "ent_seller_fs", m, "ticket_points_issuance", 600, 0, "2026-10-05", "2026-09", null);
        // 名目目录外的本商户应收：综合账单照列（名目用台账来源中文名）。
        item("fs9_" + sfx, m, m, "saved_bottle_remedy_recovery", 250, 0, "2026-09-20", "2026-09", null);
        // 不计入：往来对方是公司的派驻提成拨付（merchant_id 是本商户）、别的商户的行、8 月的行。
        item("fsx1_" + sfx, "ent_company_fs", m, "dispatch_commission_transfer", 9999, 0, "2026-09-10", "2026-09", null);
        item("fsx2_" + sfx, other, other, "energy_fee", 4444, 0, "2026-09-01", "2026-09", null);
        item("fsx3_" + sfx, m, m, "energy_fee", 3333, 0, "2026-08-01", "2026-08", null);

        RequestPostProcessor shop = new StaffRequestFixture(jdbc).staff("usr_fs_m_" + sfx, "staff", null, m, "merchant.billing.view");
        String body = mockMvc.perform(get("/api/v1/merchant-portal/fee-statement").param("month", "2026-09").with(shop))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = new ObjectMapper().readTree(body).path("data");
        assertThat(data.path("month").asText()).isEqualTo("2026-09");
        assertThat(data.path("merchant_name").asText()).isEqualTo("费用总表测试店");

        Map<String, JsonNode> fees = new LinkedHashMap<>();
        data.path("fee_items").forEach(n -> fees.put(n.path("fee_name").asText(), n));
        assertThat(fees.keySet()).containsExactly("固定租金", "每平米租金", "营业额抽成租金", "物业费", "能源费", "会员系统服务费",
                "积分发行费", "押金", "违约金", "工单用料费（商户承担）", "违规罚款", "车位月租", "门票综合服务费", "停车抵免成本");
        assertThat(fees.get("固定租金").path("amount_cent").asLong()).isEqualTo(10000);
        assertThat(fees.get("营业额抽成租金").path("amount_cent").asLong()).isEqualTo(5000);
        assertThat(fees.get("物业费").path("amount_cent").asLong()).isEqualTo(5000);
        assertThat(fees.get("能源费").path("amount_cent").asLong()).isEqualTo(1500);
        assertThat(fees.get("门票综合服务费").path("amount_cent").asLong()).isEqualTo(800);
        assertThat(fees.get("积分发行费").path("amount_cent").asLong()).isEqualTo(600);
        for (String noPath : List.of("会员系统服务费", "违规罚款")) {
            assertThat(fees.get(noPath).path("amount_cent").isNull()).as(noPath + " 没有出账路径").isTrue();
        }
        for (String zero : List.of("每平米租金", "押金", "违约金", "工单用料费（商户承担）", "车位月租", "停车抵免成本")) {
            assertThat(fees.get(zero).path("amount_cent").isNumber()).as(zero).isTrue();
            assertThat(fees.get(zero).path("amount_cent").asLong()).as(zero).isZero();
        }
        assertThat(fees.get("物业费").path("billing_basis").asText()).isEqualTo("物业费按“每平米固定单价 × 商户计租面积”按自然月计费");
        assertThat(fees.get("违规罚款").path("billing_basis").isNull()).isTrue();
        assertThat(fees.get("固定租金").path("bearer").asText()).isEqualTo("费用总表测试店");

        JsonNode bill = data.path("bill");
        Map<String, Long> lines = new LinkedHashMap<>();
        bill.path("lines").forEach(n -> lines.put(n.path("fee_name").asText(), n.path("amount_cent").asLong()));
        assertThat(lines).containsExactly(entry("固定租金", 10000L), entry("营业额抽成租金", 5000L), entry("物业费", 5000L),
                entry("能源费", 1500L), entry("积分发行费", 600L), entry("门票综合服务费", 800L), entry("存酒退出承接垫付追收", 250L));
        assertThat(bill.path("total_amount_cent").asLong()).isEqualTo(23150);
        assertThat(bill.path("paid_amount_cent").asLong()).isEqualTo(4000);
        assertThat(bill.path("unpaid_amount_cent").asLong()).isEqualTo(19150);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'merchant_portal.fee_statement.viewed'"
                + " AND business_id = ?", Integer.class, m)).isEqualTo(1);
    }

    /** 18E 复核：同一计费月两次退票——第二组冲正与重述回指第一次的重述行、账期是退票月——整条冲正链都归原计费月（BSM:755、BSM:1323）。 */
    @Test
    void feeStatementPutsEveryReversalOfASingleBarMonthIntoItsBillingMonth() throws Exception {
        String m = merchant("mch_fsr_" + sfx, "两次退票店");
        String orig = item("fsr0_" + sfx, m, m, "single_bar_service_fee", 800, 0, "2026-10-20", "2026-10", null);
        schedule(orig, m, "2026-09-01", "now()");
        item("fsr1c_" + sfx, m, m, "single_bar_service_fee", -800, 0, "2026-10-20", "2026-10", orig);
        String restated = item("fsr1b_" + sfx, m, m, "single_bar_service_fee", 700, 0, "2026-10-20", "2026-10", orig);
        item("fsr2c_" + sfx, m, m, "single_bar_service_fee", -700, 0, "2026-10-20", "2026-10", restated);
        item("fsr2b_" + sfx, m, m, "single_bar_service_fee", 600, 0, "2026-10-20", "2026-10", restated);

        RequestPostProcessor shop = new StaffRequestFixture(jdbc).staff("usr_fsr_" + sfx, "staff", null, m, "merchant.billing.view");
        assertThat(ticketFee(feeStatement(shop, "2026-09"))).isEqualTo(600);
        assertThat(ticketFee(feeStatement(shop, "2026-10"))).isZero();
    }

    /** 18E 复核：计费月已付清后退票只挂一笔应退（payable，计费月按退票当月），应退按原计费月从门票综合服务费净额扣除（BSM:755「重算原计费月服务费总额」）。 */
    @Test
    void feeStatementNetsRefundDueOfAPaidSingleBarMonthIntoItsBillingMonth() throws Exception {
        String m = merchant("mch_fsd_" + sfx, "付清后退票店");
        paidMonthWithRefundDue(m, "fsd", "now()");

        RequestPostProcessor shop = new StaffRequestFixture(jdbc).staff("usr_fsd_" + sfx, "staff", null, m, "merchant.billing.view");
        JsonNode september = feeStatement(shop, "2026-09");
        assertThat(ticketFee(september)).isEqualTo(700);
        JsonNode line = september.path("bill").path("lines").get(0);
        assertThat(line.path("fee_name").asText()).isEqualTo("门票综合服务费");
        assertThat(line.path("paid_amount_cent").asLong()).isEqualTo(800);
        assertThat(line.path("unpaid_amount_cent").asLong()).isEqualTo(-100);
        assertThat(ticketFee(feeStatement(shop, "2026-11"))).isZero();
    }

    /**
     * 第 101 条（BSM:1597「异议期自该账单在商户专区可查看之日的次日起算」、BSM:1673「月结定格 → 账单出账 → 异议期届满 → 付账日」、DEC-R8-55）：
     * 应退按自身出账日（visible_at）才从原计费月净额扣除，出账日前综合账单仍是原额。
     */
    @Test
    void feeStatementNetsRefundDueOnlyFromItsStatementDay() throws Exception {
        String m = merchant("mch_fsv_" + sfx, "应退未出账店");
        String due = paidMonthWithRefundDue(m, "fsv", "now() + interval '1 day'");
        RequestPostProcessor shop = new StaffRequestFixture(jdbc).staff("usr_fsv_" + sfx, "staff", null, m, "merchant.billing.view");
        JsonNode before = feeStatement(shop, "2026-09");
        assertThat(ticketFee(before)).as("应退出账日未到，不扣").isEqualTo(800);
        assertThat(before.path("bill").path("unpaid_amount_cent").asLong()).isZero();

        jdbc.update("UPDATE internal_payable_schedules SET visible_at = now() - interval '1 minute' WHERE ar_ap_item_id = ?", due);
        JsonNode after = feeStatement(shop, "2026-09");
        assertThat(ticketFee(after)).isEqualTo(700);
        assertThat(after.path("bill").path("unpaid_amount_cent").asLong()).isEqualTo(-100);
    }

    /** 2026-09 门票综合服务费 800 已付清，2026-11 退票挂应退 100（payable，计费月按退票当月）；应退可查看时刻按 refundVisibleAt 表达式。返回应退行。 */
    private String paidMonthWithRefundDue(String m, String tag, String refundVisibleAt) {
        String orig = item(tag + "0_" + sfx, m, m, "single_bar_service_fee", 800, 800, "2026-10-20", "2026-10", null);
        schedule(orig, m, "2026-09-01", "now()");
        String monthId = "sbfm_" + tag + "_" + sfx;
        jdbc.update("INSERT INTO single_bar_service_fee_months (id, project_id, bar_merchant_id, billing_month, ar_ap_item_id)"
                + " VALUES (?, 'proj_fs', ?, DATE '2026-09-01', ?)", monthId, m, orig);
        String due = tag + "1_" + sfx;
        jdbc.update("INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,"
                + " unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at)"
                + " VALUES (?, ?, 'payable', ?, 'single_bar_service_fee', ?, 100, 0, 100, 'CNY', DATE '2026-12-20', '2026-11',"
                + " 'pending_payment', now(), now())", due, due, m, "rf_" + tag + "_" + sfx);
        schedule(due, m, "2026-11-01", refundVisibleAt);
        jdbc.update("INSERT INTO single_bar_service_fee_adjustments (refund_id, month_id, accounting_period, before_cent, after_cent,"
                + " pending_cut_cent, refund_due_cent, refund_due_item_id) VALUES (?, ?, '2026-11', 800, 700, 0, 100, ?)",
                "rf_" + tag + "_" + sfx, monthId, due);
        return due;
    }

    private JsonNode feeStatement(RequestPostProcessor shop, String month) throws Exception {
        String body = mockMvc.perform(get("/api/v1/merchant-portal/fee-statement").param("month", month).with(shop))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new ObjectMapper().readTree(body).path("data");
    }

    private static long ticketFee(JsonNode data) {
        for (JsonNode fee : data.path("fee_items")) {
            if ("门票综合服务费".equals(fee.path("fee_name").asText())) {
                return fee.path("amount_cent").asLong();
            }
        }
        throw new AssertionError("费用总表缺门票综合服务费");
    }

    /** 内部应付计划一行（计费月＝billingStart 所在月，可查看时刻按 visibleAt 表达式），付账日规则随行新建。 */
    private void schedule(String itemId, String counterparty, String billingStart, String visibleAt) {
        String rule = "ipdr_" + itemId;
        jdbc.update("INSERT INTO internal_pay_day_rules (id, kind, day_1, day_2, day_3, effective_from, status, reason, submitted_by, decided_by)"
                + " VALUES (?, 'pay_days', 5, 15, 25, DATE '2026-01-01', 'approved', '费用总表回归', 'usr_fs_a', 'usr_fs_b')", rule);
        jdbc.update("INSERT INTO internal_payable_schedules (ar_ap_item_id, route, counterparty_id, billing_period_start, visible_at,"
                + " objection_deadline, designated_pay_day, scheduled_due_date, pay_days_rule_id, route_rule_id, objection_working_days)"
                + " VALUES (?, 'single_bar_service_fee', ?, CAST(? AS date), " + visibleAt + ", DATE '2026-10-10', DATE '2026-10-20',"
                + " DATE '2026-10-20', ?, ?, 3)", itemId, counterparty, billingStart, rule, rule);
    }

    @Test
    void feeStatementFailsClosedWithoutMerchantScopeOrPermissionAndRejectsBadMonth() throws Exception {
        String m = merchant("mch_fsn_" + sfx, "费用总表负例店");
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        mockMvc.perform(get("/api/v1/merchant-portal/fee-statement").param("month", "2026-09")
                        .with(staffs.staff("usr_fs_p_" + sfx, "staff", null, null, "merchant.billing.view")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
        mockMvc.perform(get("/api/v1/merchant-portal/fee-statement").param("month", "2026-09")
                        .with(staffs.staff("usr_fs_n_" + sfx, "staff", null, m, "contract.view")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/merchant-portal/fee-statement").param("month", "2026-13")
                        .with(staffs.staff("usr_fs_v_" + sfx, "staff", null, m, "merchant.billing.view")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    private String merchant(String id, String name) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)"
                + " VALUES (?, ?, 'catering', 'active', now(), now())", id, name);
        return id;
    }

    /** 应收台账一行；origin 非空＝冲正行（金额取负、状态已红冲）。 */
    private String item(String id, String counterparty, String merchantId, String sourceType, long amount, long paid,
            String due, String period, String origin) {
        jdbc.update("INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, amount_cent, paid_amount_cent,"
                + " unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, origin_item_id)"
                + " VALUES (?, ?, 'receivable', ?, ?, ?, ?, ?, 'CNY', CAST(? AS date), ?, ?, now(), now(), ?, ?)",
                id, id, counterparty, sourceType, amount, paid, amount - paid, due, period,
                origin == null ? "pending_collection" : "reversed", merchantId, origin);
        return id;
    }

    private void rentBill(String contractId, String merchantId, String rentTerms, String itemId, boolean voided) {
        jdbc.update("INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,"
                + " rent_terms, lifecycle_status, created_at, updated_at) VALUES (?, ?, 'subj_fs', 'subj_fs', DATE '2026-01-01',"
                + " DATE '2027-12-31', CAST(? AS jsonb), 'effective', now(), now())", contractId, merchantId, rentTerms);
        jdbc.update("INSERT INTO contract_rent_bills (id, contract_id, merchant_id, period, fee_type, amount_cent, ar_ap_item_id, voided_at)"
                + " SELECT ?, ?, ?, '2026-09', 'rent', amount_cent, id, CASE WHEN ? THEN now() END FROM ar_ap_items WHERE id = ?",
                "crb_" + contractId, contractId, merchantId, voided, itemId);
    }
}
