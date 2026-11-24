package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CD 段第 402 条：平台内部各主体之间的三类往来进开票清单——通票免费酒水结算（出品酒吧开给娱乐平台公司）、商户之间借调买断货款与借用物赔偿
 * （借出方开给借入方）；补偿新增履约结算走真实补偿链路，见 ParkWideFaultCompensationJdbcIntegrationTests。依据：BSM:1803「平台内部各主体之间的往来需要开具发票」；
 * BSM:751「域主管公司出资的免费权益按锁定内部结算价核销后结算」；BSM:1691「娱乐平台公司出资的通票免费酒水」；BSM:1201「商户之间也可借调货物：跨商户之间的结算一律按内部结算价」
 * 「赔偿按已有约定及现行跨商户结算规则形成借入方对借出方的应付」；BSM:1750「开票金额≤已收−已退−已开」「退款后先红冲再重开」；第十八批裁决 18「之后办理、赔偿应付……都按单上的项目」。
 *
 * <p>夹具随测试事务回滚（照 LedgerInvoiceItemsJdbcTests）：随机后缀的综合体、娱乐平台主体（本综合体通票收入归属）、酒吧主体与酒吧商户，直接插台账行；
 * 生成走定时作业同一入口 {@link LedgerInvoiceService#groupsToInvoice()}（只取本用例综合体的组）后逐组 generate；查看走真实接口（平台侧财务，蛇形键）。
 * 修复前不出这三类来源行、规则表仍列通票赠送权益兑付，各用例第一处断言即红。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class InternalExchangeInvoiceItemsJdbcTests {

    private static final LocalDate JUNE = LocalDate.of(2026, 6, 1);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private LedgerInvoiceService ledgerInvoices;
    @Autowired private JdbcSettlementInvoiceRepository invoiceRepository;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_iei_" + s;
    private final String platformCo = "be_iei_plat_" + s;
    private final String barCo = "be_iei_bar_" + s;
    private final String bar = "m_iei_bar_" + s;
    private RequestPostProcessor finance;
    private int seq;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "内部往来开票综合体" + s);
        entity(platformCo, "通票娱乐平台公司" + s);
        entity(barCo, "出品酒吧经营公司" + s);
        merchant(bar, "出品酒吧" + s, barCo);
        jdbc.update("INSERT INTO revenue_stream_attribution (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, updated_by,"
                + " updated_at) VALUES (?, ?, 'all_pass_ticket', ?, 'entity', 'active', 'tester', now())", "rsa_iei_" + s, project, platformCo);
        finance = new StaffRequestFixture(jdbc).staff("usr_iei_fin_" + s, "staff", project, null,
                "settlement.invoice.manage", "settlement.invoice.view");
    }

    /**
     * 402-R1：已结束月份的通票免费酒水结算按出品酒吧合成一行（应付减同月对冲应收），收入类型 spv_service「运营服务费」，开票主体为酒吧收款主体、
     * 购方为娱乐平台公司；本月的不出；再跑一轮不重复生成。
     */
    @Test
    void passDrinkSettlementOfEndedMonthEntersInvoiceListFromBarToPlatform() throws Exception {
        passDrinkJune();
        String thisMonth = LocalDate.now(LocalZone.ID).toString().substring(0, 7);
        item("payable", bar, "pass_drink_settlement", null, 500, LocalDate.now(LocalZone.ID).plusDays(30), thisMonth, bar, project);

        assertThat(generate()).isEqualTo(1);

        list(JUNE).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'pass_drink_settlement')].amount_cent", contains(1_200)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'pass_drink_settlement')].income_type", contains("spv_service")))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'pass_drink_settlement')].invoice_item_name", contains("运营服务费")))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'pass_drink_settlement')].subject_entity_id", contains(barCo)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'pass_drink_settlement')].buyer_entity_id", contains(platformCo)));
        list(LocalDate.now(LocalZone.ID).withDayOfMonth(1)).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());

        assertThat(generate()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM settlement_invoice_items WHERE project_id = ?", Integer.class, project)).isEqualTo(1);
    }

    /** 402-R1（BSM:1750「退款后先红冲再重开」）：开票后同月再挂对冲应收，应开额降到已开额之下即列入已超开。 */
    @Test
    void passDrinkLateSameMonthReversalFlagsOverIssue() throws Exception {
        passDrinkJune();
        assertThat(generate()).isEqualTo(1);

        item("receivable", bar, "pass_drink_settlement", null, 500, JUNE.plusDays(40), "2026-06", bar, project);
        assertThat(generate()).isZero();
        summary().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.over_issued[?(@.income_type == 'spv_service')].due_amount_cent", contains(700)))
                .andExpect(jsonPath("$.data.over_issued[?(@.income_type == 'spv_service')].over_issued_cent", contains(500)));
    }

    /**
     * 402-R1 复核（平台管理员撤销核销真实写法）：对冲应收的 project_id、merchant_id 为空时，仍与同酒吧同月应付净额成组；
     * 已开票后应开额降为 0，列入已超开。
     */
    @Test
    void passDrinkPlatformAdminReversalWithEmptyProjectNetsSameGroup() throws Exception {
        LocalDate due = JUNE.plusDays(40);
        item("payable", bar, "pass_drink_settlement", null, 1_200, due, "2026-06", bar, project);
        assertThat(generate()).isEqualTo(1);

        item("receivable", bar, "pass_drink_settlement", null, 1_200, due, "2026-06", null, null);
        assertThat(generate()).isZero();
        summary().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.over_issued[?(@.income_type == 'spv_service')].due_amount_cent", contains(0)))
                .andExpect(jsonPath("$.data.over_issued[?(@.income_type == 'spv_service')].over_issued_cent", contains(1_200)));
    }

    /** 402-R1：通票赠送权益兑付「不开票」规则已停用（V760），汇总接口的映射规则表不再列它；积分与折扣券营销成本仍标不开票。 */
    @Test
    void summaryRulesNoLongerListGiveawayTransfer() throws Exception {
        summary().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rules[?(@.income_type == 'giveaway_transfer')]").isEmpty())
                .andExpect(jsonPath("$.data.rules[?(@.income_type == 'marketing')].invoiceable", contains(false)));
    }

    /**
     * 402-R3（BSM:1201）：酒吧之间借调买断签收货款（台账行不带项目，按借用单所属项目）与无法归还赔偿（台账行记单上项目）同月合成一行，
     * 收入类型 goods，开票主体为借出方收款主体、购方为借入方收款主体；往来对方是供货主体的要货货款仍在门店要货货款行。
     */
    @Test
    void merchantLendingPayablesEnterInvoiceListFromLenderToBorrower() throws Exception {
        Lending l = lending();
        String supplier = "se_iei_" + s;
        jdbc.update("INSERT INTO supply_entities (id, name, entity_type, level, status) VALUES (?, ?, 'liquor', 'platform', 'active')",
                supplier, "中央酒水供货" + s);
        item("payable", supplier, "inventory_transfer", null, 800, LocalDate.of(2026, 6, 15), null, l.borrower(), project);

        assertThat(generate()).isEqualTo(2);

        list(JUNE).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'inventory_lending')].amount_cent", contains(3_500)))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'inventory_lending')].income_type", contains("goods")))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'inventory_lending')].subject_entity_id", contains(l.lenderCo())))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'inventory_lending')].buyer_entity_id", contains(l.borrowerCo())))
                .andExpect(jsonPath("$.data.items[?(@.ledger_source == 'inventory_transfer')].amount_cent", contains(800)));
    }

    /** 402-R3（PGM:287「界面全中文」）：借调行导出的来源单号写「商户借调货款与赔偿」加年月。 */
    @Test
    void lendingExportRowShowsChineseSource() {
        lending();
        generate();

        var views = invoiceRepository.list(project, JUNE, null, null, 50, 0).stream()
                .filter(v -> "inventory_lending".equals(v.ledgerSource())).toList();
        assertThat(views).hasSize(1);
        assertThat(SettlementInvoiceController.exportRow(views.getFirst()).get(12)).isEqualTo("商户借调货款与赔偿 2026-06");
    }

    // ------------------------------------------------------------------

    /** 2026-06 两笔免费酒水应付（800、1200）与一笔同月对冲应收（800），往来对方与 merchant_id 都为出品酒吧。 */
    private void passDrinkJune() {
        LocalDate due = JUNE.plusDays(40);
        item("payable", bar, "pass_drink_settlement", null, 800, due, "2026-06", bar, project);
        item("payable", bar, "pass_drink_settlement", null, 1_200, due, "2026-06", bar, project);
        item("receivable", bar, "pass_drink_settlement", null, 800, due, "2026-06", bar, project);
    }

    /**
     * 借出酒吧 A 与借入酒吧 B（各有收款主体）、一张本项目的借用买断单；按真实写法插两笔应付：买断签收货款 3000（InventoryService.recordLendingSettlementOnReceipt：
     * source_id 为借用单号，项目与会计期间为空）、无法归还赔偿 500（InventoryTransferFlowService.postPayable：source_id 为差异处理登记号，记单上项目）。
     */
    private Lending lending() {
        String lenderCo = entity("be_iei_lend_" + s, "借出酒吧经营公司" + s);
        String borrowerCo = entity("be_iei_borr_" + s, "借入酒吧经营公司" + s);
        String lender = merchant("m_iei_lend_" + s, "借出酒吧" + s, lenderCo);
        String borrower = merchant("m_iei_borr_" + s, "借入酒吧" + s, borrowerCo);
        String order = "itf_iei_" + s;
        jdbc.update("INSERT INTO inventory_transfer_orders (id, transfer_type, lending_mode, from_owner_type, from_owner_id, to_owner_type, to_owner_id,"
                + " status, project_id) VALUES (?, 'lending', 'purchase', 'merchant', ?, 'merchant', ?, 'completed', ?)", order, lender, borrower, project);
        item("payable", lender, "inventory_transfer", order, 3_000, LocalDate.of(2026, 6, 10), null, borrower, null);
        item("payable", lender, "inventory_transfer", "itd_iei_" + s, 500, LocalDate.of(2026, 6, 20), null, borrower, project);
        return new Lending(lenderCo, borrowerCo, borrower);
    }

    private record Lending(String lenderCo, String borrowerCo, String borrower) {
    }

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

    private ResultActions summary() throws Exception {
        return mockMvc.perform(get("/api/v1/settlement/invoice-items/summary").param("projectId", project).param("periodStart", JUNE.toString())
                .with(finance));
    }

    private String entity(String id, String name) {
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'management_company', 'active', now(), now())", id, name);
        return id;
    }

    private String merchant(String id, String name, String settlementEntity) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, settlement_entity_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, ?, 'active', now(), now())", id, name, project, settlementEntity);
        return id;
    }

    /** 一行台账；sourceId 为空取自身 id。 */
    private void item(String direction, String counterparty, String source, String sourceId, long amount, LocalDate due, String period,
            String merchant, String projectId) {
        String id = "arap_iei_" + s + "_" + (++seq);
        jdbc.update("INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,"
                + " unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, project_id)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, 'CNY', ?, ?, ?, now(), now(), ?, ?)",
                id, "ARAP-IEI-" + s + "-" + seq, direction, counterparty, source, sourceId == null ? id : sourceId, amount, amount, due, period,
                "payable".equals(direction) ? "pending_payment" : "pending_collection", merchant, projectId);
    }
}
