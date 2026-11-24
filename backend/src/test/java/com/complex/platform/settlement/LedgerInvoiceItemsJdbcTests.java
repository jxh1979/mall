package com.complex.platform.settlement;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.LocalZone;
import com.complex.platform.support.StaffRequestFixture;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BU 段第 374 条：平台内部主体之间其余往来（单店门票综合服务费、门店要货货款、营销与派驻提成拨付、酒吧报酬拨付、停车抵免成本、通用卡消费结算）
 * 按已结束月份的台账来源组进开票清单。依据：BSM:1803「平台内部各主体之间的往来需要开具发票，开票的具体规则一律按国家财税相关规定执行」；
 * BSM:1750「开票金额≤已收−已退−已开」「退款后先红冲再重开」；BSM:1760「跨月退款和调整在当期生成冲正」；决策台账 DEC-R4-2026-08-04-51。
 *
 * <p>夹具随测试事务回滚：随机后缀的综合体、人员所属主体、酒吧（收款主体为夹具主体），直接插台账行与来源月表；生成走定时作业同一入口
 * {@link LedgerInvoiceService#groupsToInvoice()}（只取本用例综合体的组，免得顺带生成共用库里别的用例残留）后逐组 generate；查看走真实接口
 * （持开票清单查看与管理权限、不挂商户的平台侧财务，蛇形键）。修复前不出任何台账来源行，各用例第一处断言即红。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class LedgerInvoiceItemsJdbcTests {

    private static final LocalDate JUNE = LocalDate.of(2026, 6, 1);
    private static final LocalDate PAY_DAY = LocalDate.of(2026, 7, 5);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private LedgerInvoiceService ledgerInvoices;
    @Autowired private JdbcSettlementInvoiceRepository invoiceRepository;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_lgi_" + s;
    private final String staffCo = "be_lgi_stf_" + s;
    private final String barCo = "be_lgi_bar_" + s;
    private final String bar = "m_lgi_bar_" + s;
    private RequestPostProcessor finance;
    private int seq;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "台账开票综合体" + s);
        entity(staffCo, "派驻人员所属公司" + s, "management_company");
        entity(barCo, "拨付酒吧经营公司" + s, "management_company");
        merchant(bar, "台账开票酒吧" + s, barCo);
        finance = new StaffRequestFixture(jdbc).staff("usr_lgi_fin_" + s, "staff", project, null,
                "settlement.invoice.manage", "settlement.invoice.view");
    }

    /** 374-R1：已结束月份的派驻提成拨付组出一行（人员所属主体开给酒吧）；本月的同类台账行不出；再跑一轮不重复生成。 */
    @Test
    void ledgerGroupsOfEndedMonthsEnterInvoiceListAndCurrentMonthWaits() throws Exception {
        item("payable", staffCo, "dispatch_commission_transfer", 50_000, "2026-06", bar, null);
        LocalDate thisMonth = LocalDate.now(LocalZone.ID).withDayOfMonth(1);
        item("payable", staffCo, "dispatch_commission_transfer", 7_000, thisMonth.toString().substring(0, 7), bar, null);

        assertThat(generate()).isEqualTo(1);

        list(JUNE).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'dispatch_commission_transfer')].amount_cent", contains(50_000)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'dispatch_commission_transfer')].income_type", contains("staff_remuneration")))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'dispatch_commission_transfer')].subject_entity_id", contains(staffCo)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'dispatch_commission_transfer')].buyer_name", contains("拨付酒吧经营公司" + s)));
        list(thisMonth).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());

        assertThat(ledgerInvoices.groupsToInvoice()).noneMatch(g -> project.equals(g.projectId()));
        assertThat(generate()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM settlement_invoice_items WHERE project_id = ?", Integer.class, project)).isEqualTo(1);
    }

    /** 374-R1：同组晚到的应付另出差额行、原行不动；同月冲回致应开额低于已开额时列入已超开且不再生成；原行作废后按新应开额重开。 */
    @Test
    void lateLedgerRowsAddDeltaLineAndReversalFlagsOverIssueThenReopens() throws Exception {
        item("payable", staffCo, "dispatch_commission_transfer", 50_000, "2026-06", bar, null);
        assertThat(generate()).isEqualTo(1);
        item("payable", staffCo, "dispatch_commission_transfer", 1_000, "2026-06", bar, null);
        assertThat(generate()).isEqualTo(1);
        list(JUNE).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'dispatch_commission_transfer')].amount_cent", containsInAnyOrder(50_000, 1_000)));

        item("receivable", staffCo, "dispatch_commission_transfer", 3_000, "2026-06", bar, null);
        assertThat(generate()).isZero();
        mockMvc.perform(get("/api/v1/settlement/invoice-items/summary").param("projectId", project).param("periodStart", JUNE.toString())
                        .with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.over_issued[?(@.income_type == 'staff_remuneration')].due_amount_cent", contains(48_000)))
                .andExpect(jsonPath("$.data.over_issued[?(@.income_type == 'staff_remuneration')].over_issued_cent", contains(3_000)));

        for (String id : jdbc.queryForList("SELECT id FROM settlement_invoice_items WHERE project_id = ?", String.class, project)) {
            mockMvc.perform(post("/api/v1/settlement/invoice-items/{id}/status", id).with(finance).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"voided\",\"remark\":\"冲回后作废重开\"}"))
                    .andExpect(status().isOk());
        }
        assertThat(generate()).isEqualTo(1);
        list(JUNE).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.status == 'pending')].amount_cent", contains(48_000)));
    }

    /** 374-R1（PGM:287「界面全中文」）：导出的来源单号写来源中文名加年月，收入类型写中文。 */
    @Test
    void exportRowShowsChineseLedgerSourceAndIncomeType() {
        item("payable", staffCo, "dispatch_commission_transfer", 50_000, "2026-06", bar, null);
        generate();

        var views = invoiceRepository.list(project, JUNE, null, null, 50, 0);
        assertThat(views).hasSize(1);
        List<String> row = SettlementInvoiceController.exportRow(views.getFirst());
        assertThat(row.get(12)).isEqualTo("派驻提成月结拨付 2026-06");
        assertThat(row.get(4)).isEqualTo("人员报酬拨付");
    }

    /** 374-R2（BSM:755）：已挂应收的计费月出一行门票综合服务费（娱乐平台公司开给酒吧），未挂应收的月不出；整张退款后月应收总额下降即列入已超开。 */
    @Test
    void singleBarServiceFeeMonthEntersInvoiceListAndRefundFlagsOverIssue() throws Exception {
        String receivable = item("receivable", bar, "single_bar_service_fee", 300, "2026-06", bar, null);
        String month = "sbfm_lgi_" + s;
        jdbc.update("INSERT INTO single_bar_service_fee_months (id, project_id, bar_merchant_id, billing_month, ar_ap_item_id) VALUES (?, ?, ?, ?, ?)",
                month, project, bar, JUNE, receivable);
        jdbc.update("INSERT INTO single_bar_service_fee_months (id, project_id, bar_merchant_id, billing_month) VALUES (?, ?, ?, ?)",
                month + "_u", project, bar, JUNE.minusMonths(1));
        ticket(month, "a", 300, null);
        ticket(month, "b", 200, "tr_lgi_b_" + s);
        ticket(month + "_u", "c", 400, null);

        generate();

        list(JUNE).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'single_bar_service_fee')].amount_cent", contains(300)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'single_bar_service_fee')].income_type", contains("ticket")))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'single_bar_service_fee')].subject_entity_id", contains("be_ent_platform")))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'single_bar_service_fee')].buyer_name", contains("拨付酒吧经营公司" + s)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'single_bar_service_fee')].period_start", contains("2026-06-01")));
        list(JUNE.minusMonths(1)).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());

        jdbc.update("UPDATE single_bar_service_fee_tickets SET refund_id = ? WHERE parent_ticket_id = ?", "tr_lgi_a_" + s, "tkt_lgi_a_" + s);
        assertThat(generate()).isZero();
        mockMvc.perform(get("/api/v1/settlement/invoice-items/summary").param("projectId", project).param("periodStart", JUNE.toString())
                        .with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.over_issued[?(@.income_type == 'ticket')].due_amount_cent", contains(0)))
                .andExpect(jsonPath("$.data.over_issued[?(@.income_type == 'ticket')].over_issued_cent", contains(300)));
    }

    /**
     * 374-R3（BSM:1201「商户按货款与供货主体结算」；BSM:1212「货款调整……不重复收款」）：供货主体对门店的要货货款按到期日所在月合成一行（签收货款为签收营业日；收货方担责的损失为财务复核通过当日营业日；货款调整的冲正与重述沿用原行到期日），
     * 货款调整的冲正与重述落在同组净掉，收货方担责的损失一并计入；往来对方是商户的借调买断不进本组。开票主体按 supply_goods 收入归属。
     */
    @Test
    void storeRequisitionPayablesEnterInvoiceListMonthlyNetOfAdjustments() throws Exception {
        String supplier = "se_lgi_" + s;
        jdbc.update("INSERT INTO supply_entities (id, name, entity_type, level, status) VALUES (?, ?, 'liquor', 'platform', 'active')",
                supplier, "中央酒水供货" + s);
        String supplyCo = entity("be_lgi_sup_" + s, "中央供货经营公司" + s, "management_company");
        String storeCo = entity("be_lgi_store_" + s, "要货门店经营公司" + s, "management_company");
        String store = merchant("m_lgi_store_" + s, "要货门店" + s, storeCo);
        jdbc.update("INSERT INTO revenue_stream_attribution (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, updated_by,"
                + " updated_at) VALUES (?, ?, 'supply_goods', ?, 'entity', 'active', 'tester', now())", "rsa_lgi_" + s, project, supplyCo);
        String signed = item("payable", supplier, "inventory_transfer", 10_000, LocalDate.of(2026, 6, 10), null, store, null);
        item("payable", supplier, "inventory_transfer", -10_000, LocalDate.of(2026, 6, 10), null, store, signed);
        item("payable", supplier, "inventory_transfer", 9_000, LocalDate.of(2026, 6, 10), null, store, signed);
        item("payable", supplier, "inventory_transfer", 5_000, LocalDate.of(2026, 6, 20), null, store, null);
        item("payable", supplier, "inventory_transfer", 2_000, LocalDate.of(2026, 6, 25), null, store, null);
        item("payable", bar, "inventory_transfer", 7_000, LocalDate.of(2026, 6, 26), null, store, null);

        // 第十八批 CD 段第 402 条 402-R3：往来对方是商户的那笔另成「商户借调货款与赔偿」行（inventory_lending），不进本组。
        assertThat(generate()).isEqualTo(2);

        list(JUNE).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'inventory_transfer')].amount_cent", contains(16_000)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'inventory_transfer')].income_type", contains("goods")))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'inventory_transfer')].subject_entity_id", contains(supplyCo)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'inventory_transfer')].buyer_name", contains("要货门店经营公司" + s)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'inventory_transfer')].invoice_item_name", contains("供货货款")));
    }

    /**
     * 374-R4（BSM:1142、1143「拨付作为平台内部各主体之间的普通内部结算应收应付」「均须分别记录、可分别统计，不得只保存合并后的净额」；ENUM:201）：
     * 同一酒吧、同一所属主体、同月的提成拨付与报酬拨付分别成行（应付减冲回应收），税率按所属主体的人员报酬拨付配置取。
     */
    @Test
    void commissionAndRemunerationTransfersEnterInvoiceListAsSeparateLines() throws Exception {
        item("payable", staffCo, "dispatch_commission_transfer", 50_000, "2026-06", bar, null);
        item("receivable", staffCo, "dispatch_commission_transfer", 8_000, "2026-06", bar, null);
        item("payable", staffCo, "bar_payroll_transfer", 3_000, "2026-06", bar, null);
        item("payable", staffCo, "bar_payroll_transfer", 2_000, "2026-06", bar, null);
        item("receivable", staffCo, "bar_payroll_transfer", 500, "2026-06", bar, null);
        jdbc.update("INSERT INTO settlement_tax_configs (id, business_entity_id, income_type, tax_type, applicable, rate_bps, status, is_draft)"
                + " VALUES (?, ?, 'staff_remuneration', 'vat', TRUE, 600, 'active', FALSE)", "stc_lgi_" + s, staffCo);

        assertThat(generate()).isEqualTo(2);

        list(JUNE).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'dispatch_commission_transfer')].amount_cent", contains(42_000)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'bar_payroll_transfer')].amount_cent", contains(4_500)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'staff_remuneration')].subject_entity_id", contains(staffCo, staffCo)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'staff_remuneration')].buyer_name",
                        contains("拨付酒吧经营公司" + s, "拨付酒吧经营公司" + s)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'staff_remuneration')].invoice_item_name", contains("人员报酬拨付", "人员报酬拨付")))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'staff_remuneration')].vat_rate_bps", contains(600, 600)));
    }

    /** 374-R5（BSM:1436）：停车抵免成本月度应收出一行停车费，城市项目公司开给承担主体。 */
    @Test
    void parkingCreditCostMonthEntersInvoiceList() throws Exception {
        String city = entity("be_lgi_city_" + s, "停车城市项目公司" + s, "project_company");
        String bearer = entity("be_lgi_bear_" + s, "停车承担商户主体" + s, "management_company");
        String receivable = item("receivable", bar, "parking_credit_cost", 1_200, "2026-06", bar, null);
        jdbc.update("INSERT INTO parking_credit_cost_months (id, project_id, merchant_id, bearer_entity_id, receiver_entity_id, cost_month, amount_cent,"
                + " ar_ap_item_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", "pccm_lgi_" + s, project, bar, bearer, city, JUNE, 1_200, receivable);

        assertThat(generate()).isEqualTo(1);

        list(JUNE).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'parking_credit_cost')].amount_cent", contains(1_200)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'parking_credit_cost')].income_type", contains("parking")))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'parking_credit_cost')].subject_entity_id", contains(city)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'parking_credit_cost')].buyer_name", contains("停车承担商户主体" + s)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'parking_credit_cost')].invoice_item_name", contains("停车费")));
    }

    /**
     * 374-R6（BSM:1704「长沙项目公司就通用卡余额的实际消费向各业态的经营主体直接付款结算」；ENUM:86；BSM:1760）：按收款方逐行开给该综合体的城市项目公司，
     * 跨月追收在当期组里抵减；收款方是酒吧商户的按餐饮、开票主体为该酒吧收款主体；只有追收的组不出行。
     */
    @Test
    void generalCardConsumptionSettlementEntersInvoiceListByPayee() throws Exception {
        String city = entity("be_lgi_city_" + s, "通用卡城市项目公司" + s, "project_company");
        jdbc.update("INSERT INTO business_entity_projects (business_entity_id, project_id) VALUES (?, ?)", city, project);
        String other = entity("be_lgi_oth_" + s, "只有追收的收款主体" + s, "management_company");
        item("payable", "be_amusement", "general_card_consume", 20_000, "2026-06", null, null);
        item("receivable", "be_amusement", "general_card_consume", 1_500, "2026-06", null, null);
        item("payable", "be_media", "general_card_consume", 7_000, "2026-06", null, null);
        item("payable", "be_ent_platform", "general_card_consume", 9_000, "2026-06", null, null);
        item("payable", bar, "general_card_consume", 6_000, "2026-06", null, null);
        item("receivable", other, "general_card_consume", 800, "2026-06", null, null);

        assertThat(generate()).isEqualTo(4);

        list(JUNE).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'game')].amount_cent", contains(18_500)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'game')].subject_entity_id", contains("be_amusement")))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'show')].amount_cent", contains(7_000)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'ticket')].amount_cent", contains(9_000)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'catering')].amount_cent", contains(6_000)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'catering')].subject_entity_id", contains(barCo)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'general_card_consume')].buyer_name",
                        contains("通用卡城市项目公司" + s, "通用卡城市项目公司" + s, "通用卡城市项目公司" + s, "通用卡城市项目公司" + s)))
                .andExpect(jsonPath("$.data.items[?(@.subject_entity_id == '" + other + "')]").isEmpty());
    }

    // ------------------------------------------------------------------

    /** 同定时作业入口，只生成本用例综合体的组。 */
    private int generate() {
        int created = 0;
        for (var g : ledgerInvoices.groupsToInvoice()) {
            if (project.equals(g.projectId())) {
                created += ledgerInvoices.generate(g, "tester");
            }
        }
        return created;
    }

    private ResultActions list(LocalDate periodStart) throws Exception {
        return mockMvc.perform(get("/api/v1/settlement/invoice-items").param("projectId", project).param("periodStart", periodStart.toString())
                .param("size", "200").with(finance));
    }

    private String entity(String id, String name, String role) {
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) VALUES (?, ?, ?, 'active', now(), now())",
                id, name, role);
        return id;
    }

    private String merchant(String id, String name, String settlementEntity) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, settlement_entity_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, ?, 'active', now(), now())", id, name, project, settlementEntity);
        return id;
    }

    private void ticket(String monthId, String key, long fee, String refundId) {
        jdbc.update("INSERT INTO single_bar_service_fee_tickets (parent_ticket_id, order_id, month_id, base_cent, config_id, fee_bps, fee_cent, refund_id)"
                + " VALUES (?, ?, ?, ?, 'csfc_lgi', 1000, ?, ?)", "tkt_lgi_" + key + "_" + s, "ord_lgi_" + s, monthId, fee * 10, fee, refundId);
    }

    /** 到期日取付账日的台账行（会计期间 period）。 */
    private String item(String direction, String counterparty, String source, long amount, String period, String merchant, String origin) {
        return item(direction, counterparty, source, amount, PAY_DAY, period, merchant, origin);
    }

    /** 一行台账；amount 为负即冲正行（须挂 origin）。 */
    private String item(String direction, String counterparty, String source, long amount, LocalDate due, String period, String merchant,
            String origin) {
        String id = "arap_lgi_" + s + "_" + (++seq);
        String state = amount < 0 ? "reversed" : "payable".equals(direction) ? "pending_payment" : "pending_collection";
        jdbc.update("INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,"
                + " unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, project_id, origin_item_id)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, 'CNY', ?, ?, ?, now(), now(), ?, ?, ?)",
                id, "ARAP-LGI-" + s + "-" + seq, direction, counterparty, source, id, amount, amount, due, period, state, merchant, project, origin);
        return id;
    }
}
