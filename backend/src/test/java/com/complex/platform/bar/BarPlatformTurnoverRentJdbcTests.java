package com.complex.platform.bar;

// 文件版本：V0.04

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 Q 段第 138 条 138-R1、第 140 条 140-R1（BSM:1347「不论收款走平台还是走商户自有渠道均计入」「减去当期已成功的退款金额」；
 * BSM:1070「商户营业额与营业额抽成基数按打折后的实收金额计」「券抵扣的金额不计入该商户营业额与营业额抽成基数」；
 * BSM:1076「月结定格的扣留部分，扣留金额计入该商户营业额、抽成基数」；BSM:1077「小费……不计入商户营业额与营业额抽成基数」；
 * BSM:1111「挂起订单在结清前不计入……营业额抽成基数……结清后按实际结清金额计入，归属结清所在营业日」；
 * ENUM:257「营业额抽成账单在该账期的薪资月结完成、该账期月结定格时确定的服务费应发数与扣留数定数之后出账，账单一次出准，不使用预估数」；
 * BSM:1682「酒吧仍按每家酒吧配置」）：酒吧商户的平台内酒吧收入（整单已确认商品实收＋月结扣留−已退成商品分项）进营业额抽成基数，
 * 按该酒吧营业日窗取数；服务费未月结时出账 409 BAR_REVENUE_PENDING。后台网页收银开班、下单、出账、结账与平台管理员出抽成账单
 * 按真实请求形状（蛇形键）走真实入口与真实授权链。138-R4：酒吧商户已完成的服务点单（点舞）全额进营业额抽成基数，按下单时点归期，
 * 点单员下单、确认收款、完成、取消走服务点单真实入口。
 * 第十八批 U 段第 155 条 155-R1（ENUM:257「冲正金额一律按该笔款项在其原账期实际计费时所适用的档位与费率原额冲回，不按冲正发生当期的档位与费率重算，
 * 原账期已定格的账单与档位不因冲正而重算；多笔退款逐笔分别冲正，不合并折算。」；DEC-R8-54；第十八批裁决 36「原月按保底收租的，该月销售跨月退款不冲回；
 * 原月按抽成额收租的，照档位费率冲回」）：出账时逐笔快照原账期档位费率与原月是否按保底收租，跨期退款在退款当期逐笔挂营业额抽成冲正应付，不进退款当期基数。
 * 155-R2（BSM:1323、BSM:1692）：冲正应付在退款当期抵减商户综合账单的营业额抽成租金，并以红字租金分录挂进费用台账。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarPlatformTurnoverRentJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private FinanceService finance;
    @Autowired private BusinessDayResolver days;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "bpt_prj_" + s;
    private final String bar = "bpt_bar_" + s;
    private final String space = "bpt_space_" + s;
    private final String contract = "bpt_contract_" + s;
    private final String rule = "bpt_rule_" + s;

    private String table;
    private String cashierId;
    private RequestPostProcessor cashier;
    private RequestPostProcessor admin;
    private RequestPostProcessor contractBiller;
    private String product;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','抽成酒吧综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'抽成酒吧','bar','active',?,now(),now())",
                bar, project);
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '抽成酒吧吧台区', 'shop', 'active', now(), now())", space, project);
        table = table(bar, "counter", "bar_counter");
        String item = "bpt_item_" + s;
        String location = "bpt_loc_" + s;
        String counter = "bpt_counter_" + s;
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '一号吧台')", counter, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, spec_volume_ml, created_at, updated_at)"
                + " VALUES (?, '某某啤酒 330 毫升瓶装', '酒类', '瓶', 'active', ?, 330, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?, ?)", "bpt_bal_" + s, item, location, bar, counter);
        // 抽成规则：本酒吧 10%、无保底，已复核生效。
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '酒吧平台内营业额抽成规则', 1000, 0, DATE '2020-01-01', NULL, 'active', FALSE)
                """, rule, contract, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_bpt_admin_" + s, "platform_admin", project, null, "finance.turnover_rent.bill");
        // 155-R1：合同工作台「排租金AR」的后台财务账号。
        contractBiller = staffs.staff("usr_bpt_fin_" + s, "staff", project, null, "finance.ar_ap.manage");
        // 后台网页收银员：开班、建商品、下单出账、结账。
        cashierId = "usr_bpt_cashier_" + s;
        cashier = staffs.staff(cashierId, "staff", project, bar,
                "bar.shift.manage", "bar.product.manage", "bar.order.create", "finance.payment.register");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bpt-product-" + s);
        body.put("bar_id", bar);
        body.put("product_name", "某某啤酒（整瓶）");
        body.put("category", "beer");
        body.put("price_cent", 5000);
        body.put("status", "active");
        body.put("stock_deduction_required", true);
        body.put("inventory_item_id", item);
        body.put("stock_location_id", location);
        product = data(mockMvc.perform(post("/api/v1/bar/products").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk())).path("id").asText();
    }

    /** ① 网页收银开班、吧台位下单 2 瓶（10000）、出账、现金结清 → 平台管理员出该营业日所在月抽成账单：10000 × 10% = 1000。 */
    @Test
    void webCashierSettledBarBillIsBilledAsTurnoverRent() throws Exception {
        mockMvc.perform(post("/api/v1/bar/shifts/open").with(cashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bpt-shift-" + s, "bar_id", bar,
                                "cashier_user_id", cashierId, "opening_cash_cent", 0))))
                .andExpect(status().isOk());
        settle(bill("full"), "full", 10_000);

        YearMonth month = barMonth(Instant.now());
        // 第十八批 V 段 159-R4：抽成出账要求账期内营业日的订单与核销、退款与冲正已标记为最终（本用例不测该闸，整段标齐）。
        markRangeFinal(jdbc, "bar", bar, month.atDay(1), month.atEndOfMonth());
        mockMvc.perform(post("/api/v1/finance/turnover-rent/bills").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bpt-bill-" + s, "rule_id", rule,
                                "period_start", month.atDay(1).toString(), "period_end", month.atEndOfMonth().toString(),
                                "due_date", month.atEndOfMonth().plusDays(10).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(1000));
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE source_type = 'turnover_rent' AND source_id = ?",
                Long.class, rule)).isEqualTo(1000L);
    }

    /**
     * ② 口径：结清的 10000 计入；用券抵扣 2000、小费 500 的账单只计折后实收减小费 8000；部分付款（4000/10000，待付清）不计；
     * 他店结清的账单不计 → (10000 + 8000) × 10% = 1800。
     */
    @Test
    void onlyThisBarsConfirmedGoodsRevenueCounts() throws Exception {
        settle(bill("full"), "full", 10_000);
        settle(bill("partial"), "partial", 4_000);
        Instant now = Instant.now();
        String tipped = seededBill("tipped", bar, table, 10_000, 2_000, 500, 8_000, 8_500, "settled", now);
        jdbc.update("INSERT INTO bar_payroll_bill_components(id,source_bill_id,session_id,source_table_id,project_id,merchant_id,business_date,"
                + "component_type,amount_cent,original_base_cent,source_event_id,source_snapshot,collection_bill_id)"
                + " VALUES(?,?,?,?,?,?,?,'tip',500,500,?,'[]'::jsonb,?)", "bpt_tip_" + s, tipped, session(bar, table), table, project, bar,
                java.sql.Date.valueOf(barMonth(now).atDay(1)), "bpt_tip_event_" + s, tipped);
        String other = "bpt_bar_other_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'他店酒吧','bar','active',?,now(),now())",
                other, project);
        seededBill("other", other, table(other, "other", "bar_counter"), 50_000, 0, 0, 50_000, 50_000, "settled", now);

        // 第十八批 V 段 159-R4：抽成出账要求账期内营业日的订单与核销、退款与冲正已标记为最终（本用例不测该闸，整段标齐）。
        markRangeFinal(jdbc, "bar", bar, barMonth(now).atDay(1), barMonth(now).atEndOfMonth());
        assertThat(finance.computePeriodTurnoverRent(bar, contract, barMonth(now).toString())).isEqualTo(1_800L);
    }

    /** ③ 卡座服务费尚未月结（月结定格前扣留数未定）：出账 409 BAR_REVENUE_PENDING，不生成应收（ENUM:257）。 */
    @Test
    void serviceFeeAwaitingMonthCloseBlocksTurnoverRentBill() throws Exception {
        String seat = table(bar, "seat", "card_seat");
        String fee = seededBill("fee", bar, seat, 20_000, 0, 0, 22_000, 0, "unsettled", null);
        jdbc.update("INSERT INTO bar_payroll_bill_components(id,source_bill_id,session_id,source_table_id,project_id,merchant_id,business_date,"
                + "component_type,amount_cent,original_base_cent,source_event_id,source_snapshot,collection_bill_id)"
                + " VALUES(?,?,?,?,?,?,DATE '2026-07-15','service_fee',2000,20000,?,'[]'::jsonb,?)", "bpt_fee_" + s, fee, session(bar, seat),
                seat, project, bar, "bpt_fee_event_" + s, fee);

        mockMvc.perform(post("/api/v1/finance/turnover-rent/bills").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bpt-pending-" + s, "rule_id", rule,
                                "period_start", "2026-07-01", "period_end", "2026-07-31", "due_date", "2026-08-10"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_REVENUE_PENDING"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = 'turnover_rent' AND source_id = ?",
                Integer.class, rule)).isZero();
    }

    /**
     * ④（155-R1）7 月结清 10000 的账单在 8 月退成商品 4000：平台管理员先出 7 月（10000 × 10% = 1000，逐笔快照该账单 10% 段 10000、非按保底），
     * 再出 8 月（8 月结清 10000 → 1000，不扣这 4000），另挂一笔营业额抽成冲正应付 400 归 8 月；7 月应收不变（ENUM:257「不回改历史月份」）。
     * 同一规则只挂一笔 turnover_rent 应收，8 月起换一版同费率规则。
     */
    @Test
    void completedGoodsRefundIsReversedAtTheOriginalRateInTheRefundMonth() throws Exception {
        String refunded = seededBill("july", bar, table, 10_000, 0, 0, 6_000, 10_000, "settled", JULY);
        seededBill("august", bar, table, 10_000, 0, 0, 10_000, 10_000, "settled", Instant.parse("2026-08-10T06:00:00Z"));
        String refund = refund(refunded, 4_000, AUGUST);
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '酒吧平台内营业额抽成规则（8 月起）', 1000, 0, DATE '2026-08-01', NULL, 'active', FALSE)
                """, rule + "_aug", contract, bar);

        billTurnoverRent(rule, YearMonth.of(2026, 7)).andExpect(status().isOk()).andExpect(jsonPath("$.data.amount_cent").value(1000));
        assertSnapshot(refunded, "[{\"rate_bps\":1000,\"amount_cent\":10000}]", false);
        billTurnoverRent(rule + "_aug", YearMonth.of(2026, 8)).andExpect(status().isOk()).andExpect(jsonPath("$.data.amount_cent").value(1000));

        assertThat(jdbc.queryForList("SELECT amount_cent, accounting_period, source_id, merchant_id FROM ar_ap_items"
                + " WHERE source_type = 'turnover_rent_reversal' AND counterparty_id = ?", bar))
                .containsExactly(Map.of("amount_cent", 400L, "accounting_period", "2026-08", "source_id", refund, "merchant_id", bar));
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE source_type = 'turnover_rent' AND source_id = ?",
                Long.class, rule)).isEqualTo(1000L);
    }

    /**
     * ④-②（155-R1）超额累进逐笔定位：0～10000 按 5%、以上 10%；7 月 A 8000 在前、B 6000 在后 → 7 月抽成 900，A 全在 5% 段，B 拆 5% 段 2000、
     * 10% 段 4000。8 月先退 B 3000、再退 A 1000 → 两笔冲正 250、50 逐笔挂；8 月结清 5000 按 8 月自己的累计算 250，不扣这 4000。
     */
    @Test
    void crossPeriodRefundsAreReversedPerSaleAtTheirOwnSegments() throws Exception {
        contract("turnover_only", 0);
        jdbc.update("INSERT INTO turnover_rent_rule_tiers(id,rule_id,seq,upper_bound_cent,rate_bps) VALUES(?,?,1,10000,500),(?,?,2,NULL,1000)",
                "bpt_tier1_" + s, rule, "bpt_tier2_" + s, rule);
        String a = seededBill("a", bar, table, 8_000, 0, 0, 7_000, 8_000, "settled", Instant.parse("2026-07-10T06:00:00Z"));
        String b = seededBill("b", bar, table, 6_000, 0, 0, 3_000, 6_000, "settled", Instant.parse("2026-07-12T06:00:00Z"));
        seededBill("c", bar, table, 5_000, 0, 0, 5_000, 5_000, "settled", Instant.parse("2026-08-05T06:00:00Z"));
        String refundB = refund(b, 3_000, Instant.parse("2026-08-10T06:00:00Z"));
        String refundA = refund(a, 1_000, AUGUST);

        periodBills("2026-07").andExpect(status().isOk());
        assertThat(rentBill("2026-07")).isEqualTo(900L);
        assertSnapshot(a, "[{\"rate_bps\":500,\"amount_cent\":8000}]", false);
        assertSnapshot(b, "[{\"rate_bps\":500,\"amount_cent\":2000},{\"rate_bps\":1000,\"amount_cent\":4000}]", false);
        periodBills("2026-08").andExpect(status().isOk());
        assertThat(reversals()).containsExactly(entry(refundB, 250L), entry(refundA, 50L));
        assertThat(rentBill("2026-08")).isEqualTo(250L);
    }

    /**
     * ④-③（裁决 36「原月按保底收租的，该月销售跨月退款不冲回」）：取高合同月租 100000、7 月抽成 5000 → 7 月按保底收 100000，快照记按保底；
     * 8 月退 7 月商品 10000 不冲回，也不进 8 月基数（8 月结清 30000 → 抽成 3000，不扣成 2000）。
     */
    @Test
    void higherOfMonthBilledAtBaseRentIsNotReversed() throws Exception {
        String july = decision36("higher_of", 100_000, null, 50_000, 30_000);
        assertThat(rentBill("2026-07")).isEqualTo(100_000L);
        assertSnapshot(july, "[{\"rate_bps\":1000,\"amount_cent\":50000}]", true);
        assertThat(reversals()).isEmpty();
        assertThat(finance.computePeriodTurnoverRent(bar, contract, "2026-08")).isEqualTo(3_000L);
    }

    /** ④-④（裁决 36「原月按抽成额收租的，照档位费率冲回」）：7 月抽成 200000 → 8 月冲正 1000；8 月取高只比 8 月抽成 105000（ENUM:257「不跨月合并」）。 */
    @Test
    void higherOfMonthBilledOnTurnoverIsReversedAndAugustComparesItsOwnTurnover() throws Exception {
        String july = decision36("higher_of", 100_000, null, 2_000_000, 1_050_000);
        assertThat(rentBill("2026-07")).isEqualTo(200_000L);
        assertSnapshot(july, "[{\"rate_bps\":1000,\"amount_cent\":2000000}]", false);
        assertThat(reversals().values()).containsExactly(1_000L);
        assertThat(rentBill("2026-08")).isEqualTo(105_000L);
    }

    /** ④-⑤ 冲回额照原档位费率原额，不以原月抽成超出月租的差额为限：7 月抽成 100500（超 500）→ 冲正 1000。 */
    @Test
    void reversalIsNotCappedAtTheExcessOverBaseRent() throws Exception {
        decision36("higher_of", 100_000, null, 1_005_000, 0);
        assertThat(rentBill("2026-07")).isEqualTo(100_500L);
        assertThat(reversals().values()).containsExactly(1_000L);
    }

    /** ④-⑥ 抽成恰好等于月租：没有「没到保底」，算按抽成额收租 → 冲正 1000。 */
    @Test
    void turnoverEqualToBaseRentIsBilledOnTurnoverAndReversed() throws Exception {
        String july = decision36("higher_of", 100_000, null, 1_000_000, 0);
        assertSnapshot(july, "[{\"rate_bps\":1000,\"amount_cent\":1000000}]", false);
        assertThat(reversals().values()).containsExactly(1_000L);
    }

    /** ④-⑦ 纯抽成合同、规则保底 50000 生效（抽成 5000，按保底收）→ 不冲回。 */
    @Test
    void ruleMinimumMonthIsNotReversed() throws Exception {
        String july = decision36("turnover_only", 0, 50_000L, 50_000, 0);
        assertThat(rentBill("2026-07")).isEqualTo(50_000L);
        assertSnapshot(july, "[{\"rate_bps\":1000,\"amount_cent\":50000}]", true);
        assertThat(reversals()).isEmpty();
    }

    /** ④-⑧ 保底加抽成叠加：月租照收、抽成额也照收，算按抽成额收租 → 冲正 1000。 */
    @Test
    void sumContractMonthIsReversed() throws Exception {
        decision36("sum", 100_000, null, 50_000, 0);
        assertThat(rentBill("2026-07")).isEqualTo(105_000L);
        assertThat(reversals().values()).containsExactly(1_000L);
    }

    /**
     * ④-⑫（155-R2；BSM:1323「把当期各名目的费用汇总列示，使其一次看清全部应付」；BSM:1692「台账只追加、更正走红字冲销，不回改历史」）：
     * 取高合同 7 月抽成 200000、8 月抽成 105000、8 月挂冲正 1000 → 商户账号看 8 月综合账单「营业额抽成租金」＝105000−1000＝104000，7 月仍 200000；
     * 后台财务按 8 月自动挂接费用台账，冲正应付挂一条红字租金分录 −1000，本酒吧没有金额漂移。
     */
    @Test
    void reversalNetsTheMerchantStatementAndExpenseLedgerOfTheRefundMonth() throws Exception {
        decision36("higher_of", 100_000, null, 2_000_000, 1_050_000);
        String reversal = jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE source_type = 'turnover_rent_reversal' AND counterparty_id = ?",
                String.class, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor shop = staffs.staff("usr_bpt_shop_" + s, "staff", project, bar, "merchant.billing.view");
        mockMvc.perform(get("/api/v1/merchant-portal/fee-statement").param("month", "2026-07").with(shop))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fee_items[?(@.fee_name == '营业额抽成租金')].amount_cent").value(200_000));
        mockMvc.perform(get("/api/v1/merchant-portal/fee-statement").param("month", "2026-08").with(shop))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fee_items[?(@.fee_name == '营业额抽成租金')].amount_cent").value(104_000));

        RequestPostProcessor expenses = staffs.staff("usr_bpt_mex_" + s, "staff", project, null, "finance.merchant_expense.manage");
        mockMvc.perform(post("/api/v1/finance/merchant-expenses/sync-auto").with(expenses).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bpt-mex-" + s, "period", "2026-08"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.drift[?(@.merchant_id == '" + bar + "')]").isEmpty());
        assertThat(jdbc.queryForList("SELECT amount_cent, expense_type FROM merchant_expense_entries WHERE id = ?", "mex_arn_" + reversal))
                .containsExactly(Map.of("amount_cent", -1_000L, "expense_type", "rent_allocation"));
    }

    /**
     * ④-⑨ 真实流程：网页收银按后台真实形状现金结账（结清时点回拨到 7 月）、按原账单明细办理商品退款 4000（经主管复核放行）、后台财务按契约登记退款应付于 8 月付讫；
     * 合同工作台先出 7 月（1000）再出 8 月 → 挂一笔营业额抽成冲正应付 400（带商户）。
     */
    @Test
    void webCashierRefundOfAJulySaleIsReversedInAugust() throws Exception {
        contract("turnover_only", 0);
        String refunderId = "usr_bpt_refunder_" + s;
        RequestPostProcessor refunder = new StaffRequestFixture(jdbc).staff(refunderId, "staff", project, bar,
                "bar.bill.adjust", "bar.bill.refund", "finance.payment.register");
        jdbc.update("INSERT INTO refund_authority_limits (id, subject_type, subject_id, refund_domain, limit_amount_cent)"
                + " VALUES (?, 'staff', ?, 'bar', 100000000)", "ral_bpt_" + s, refunderId);
        String billId = bill("real");
        Map<String, Object> settle = new LinkedHashMap<>();
        settle.put("idempotency_key", "bpt-settle-real-" + s);
        settle.put("payment_id", "bpt-pay-real-" + s);
        settle.put("payment_channel", "cash");
        settle.put("settlement_amount_cent", 10_000);
        mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/settle").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(settle))).andExpect(status().isOk());
        jdbc.update("UPDATE bar_bill_settlements SET settled_at = ? WHERE bill_id = ?", Timestamp.from(JULY), billId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("adjustment_type", "refund");
        body.put("amount_cent", 4_000);
        body.put("reason", "客人退一瓶中的部分");
        body.put("evidence_attachment_ids", List.of());
        body.put("refund_lines", List.of(Map.of("source_bill_id", billId, "component_type", "goods", "amount_cent", 4_000)));
        body.put("idempotency_key", "bpt-refund-real-" + s);
        // 账单调整先经上一级主管复核（准入策略 bar_bill_waiver_block）：首次提交 409 建复核单，批准后原样再提交。
        mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/adjustments").with(refunder)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        jdbc.update("UPDATE supervisor_review_decisions SET review_status = 'approved', decision = 'approved', reviewer_level = 3"
                + " WHERE action_type = 'bar.bill_waiver' AND object_id = ? AND initiated_by = ? AND review_status = 'pending_review'",
                billId, refunderId);
        JsonNode adjustment = data(mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/adjustments").with(refunder)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isOk()));
        mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + adjustment.path("ar_ap_item_id").asText() + "/payments").with(refunder)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bpt-refund-pay-" + s, "amount_cent", 4_000,
                        "payment_method", "bank_transfer", "payment_time", AUGUST.toString()))))
                .andExpect(status().isOk());

        periodBills("2026-07").andExpect(status().isOk());
        assertThat(rentBill("2026-07")).isEqualTo(1_000L);
        periodBills("2026-08").andExpect(status().isOk());
        assertThat(jdbc.queryForList("SELECT source_id, amount_cent, merchant_id FROM ar_ap_items"
                + " WHERE source_type = 'turnover_rent_reversal' AND counterparty_id = ?", bar))
                .containsExactly(Map.of("source_id", adjustment.path("id").asText(), "amount_cent", 400L, "merchant_id", bar));
    }

    /**
     * ④-⑬（155-R1；ENUM:257「多笔退款逐笔分别冲正，不合并折算。」）：同一酒吧 7 月网页收银两张账单各现金结清 10000（结清时点回拨到 7 月），
     * 8 月各按原账单明细办一笔商品退款 4000、2000（退款单号由系统生成，形如 bill_adjustment_＋32 位十六进制）→ 合同工作台先出 7 月再出 8 月，
     * 挂两笔冲正应付 400、200，source_id 各为本笔退款单，台账 id 互不相同。整单号拼进台账 id 时截到只剩单号首位，
     * 第二笔的幂等键取到单号首位与第一笔相同，令撞号确定复现。
     */
    @Test
    void twoCrossPeriodRefundsOfOneBarArePostedAsTwoReversals() throws Exception {
        contract("turnover_only", 0);
        String refunderId = "usr_bpt_refunder_" + s;
        RequestPostProcessor refunder = new StaffRequestFixture(jdbc).staff(refunderId, "staff", project, bar,
                "bar.bill.adjust", "bar.bill.refund", "finance.payment.register");
        jdbc.update("INSERT INTO refund_authority_limits (id, subject_type, subject_id, refund_domain, limit_amount_cent)"
                + " VALUES (?, 'staff', ?, 'bar', 100000000)", "ral_bpt_" + s, refunderId);
        String first = bill("two1");
        settle(first, "two1", 10_000);
        String second = bill("two2");
        settle(second, "two2", 10_000);
        jdbc.update("UPDATE bar_bill_settlements SET settled_at = ? WHERE bill_id IN (?, ?)", Timestamp.from(JULY), first, second);
        String firstKey = "bpt-refund-two1-" + s;
        char head = BarBillRecords.billSlug(first + ":refund:" + firstKey).charAt(0);
        int n = 0;
        while (BarBillRecords.billSlug(second + ":refund:bpt-refund-two2-" + s + "-" + n).charAt(0) != head) {
            n++;
        }
        String refundA = webRefund(refunder, refunderId, first, 4_000, firstKey);
        String refundB = webRefund(refunder, refunderId, second, 2_000, "bpt-refund-two2-" + s + "-" + n);

        periodBills("2026-07").andExpect(status().isOk());
        assertThat(rentBill("2026-07")).isEqualTo(2_000L);
        periodBills("2026-08").andExpect(status().isOk());
        assertThat(reversals()).containsExactly(entry(refundA, 400L), entry(refundB, 200L));
        assertThat(jdbc.queryForList("SELECT id FROM ar_ap_items WHERE source_type = 'turnover_rent_reversal' AND counterparty_id = ?",
                String.class, bar)).hasSize(2).doesNotHaveDuplicates();
    }

    /**
     * ④-⑭（155-R1；ENUM:257「减去当期已成功的退款金额」）：同一酒吧 8 月网页收银现金结清一张 10000 的账单（结清时点回拨到 2026-08-05），
     * 8 月按原账单明细办一笔商品退款 4000（8 月付讫）→ 合同工作台出 8 月：同期退款从该账单里减，租金按净额 6000 计 600；
     * 该账单快照 net_amount_cent＝6000、segments 合计 6000；没有营业额抽成冲正应付。
     */
    @Test
    void sameMonthRefundIsDeductedFromItsBillAndSnapshotsTheNetAmount() throws Exception {
        contract("turnover_only", 0);
        String refunderId = "usr_bpt_refunder_" + s;
        RequestPostProcessor refunder = new StaffRequestFixture(jdbc).staff(refunderId, "staff", project, bar,
                "bar.bill.adjust", "bar.bill.refund", "finance.payment.register");
        jdbc.update("INSERT INTO refund_authority_limits (id, subject_type, subject_id, refund_domain, limit_amount_cent)"
                + " VALUES (?, 'staff', ?, 'bar', 100000000)", "ral_bpt_" + s, refunderId);
        String billId = bill("same");
        settle(billId, "same", 10_000);
        jdbc.update("UPDATE bar_bill_settlements SET settled_at = ? WHERE bill_id = ?",
                Timestamp.from(Instant.parse("2026-08-05T06:00:00Z")), billId);
        webRefund(refunder, refunderId, billId, 4_000, "bpt-refund-same-" + s);

        periodBills("2026-08").andExpect(status().isOk());
        assertThat(rentBill("2026-08")).isEqualTo(600L);
        assertThat(jdbc.queryForList("SELECT period, net_amount_cent,"
                + " (SELECT sum((seg->>'amount_cent')::bigint) FROM jsonb_array_elements(segments) seg)::bigint AS segments_total"
                + " FROM turnover_rent_sale_snapshots WHERE source_kind = 'bar_bill' AND source_id = ?", billId))
                .containsExactly(Map.of("period", "2026-08", "net_amount_cent", 6_000L, "segments_total", 6_000L));
        assertThat(reversals()).isEmpty();
    }

    /** ④-⑩ 先出 8 月：所退原账期 7 月的抽成还没出 → 409 TURNOVER_RENT_ORIGIN_UNBILLED，不留任何写入；出完 7 月再出 8 月成功。 */
    @Test
    void refundMonthBeforeItsOriginalMonthIsBilledIsRejected() throws Exception {
        contract("turnover_only", 0);
        String july = seededBill("july", bar, table, 10_000, 0, 0, 6_000, 10_000, "settled", JULY);
        String refund = refund(july, 4_000, AUGUST);

        periodBills("2026-08").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TURNOVER_RENT_ORIGIN_UNBILLED"))
                .andExpect(jsonPath("$.message").value("退款所属原账期 2026-07 的营业额抽成尚未出账，请先出原账期"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM turnover_rent_sale_snapshots WHERE merchant_id = ?", Integer.class, bar)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contract_rent_bills WHERE contract_id = ?", Integer.class, contract)).isZero();
        assertThat(reversals()).isEmpty();

        periodBills("2026-07").andExpect(status().isOk());
        periodBills("2026-08").andExpect(status().isOk());
        assertThat(reversals()).containsExactly(entry(refund, 400L));
    }

    /** ④-⑪ 纯抽成合同 8 月没有销售、只有一笔跨期退款：不出租金单，只挂一笔冲正；重复排租金不重挂。 */
    @Test
    void refundOnlyMonthPostsTheReversalOnceWithoutRentBill() throws Exception {
        contract("turnover_only", 0);
        String july = seededBill("july", bar, table, 10_000, 0, 0, 6_000, 10_000, "settled", JULY);
        String refund = refund(july, 4_000, AUGUST);

        periodBills("2026-07").andExpect(status().isOk());
        periodBills("2026-08").andExpect(status().isOk());
        periodBills("2026-08").andExpect(status().isOk());
        assertThat(rentBill("2026-08")).isNull();
        assertThat(reversals()).containsExactly(entry(refund, 400L));
    }

    /** ⑤（140-R1）本酒吧营业日起点批准为 12:00：网页收银真实结清后，结清时点为北京 2026-08-01 11:00 的这笔归 7 月账期（出厂 10:00 会归 8 月）。 */
    @Test
    void barRevenueFollowsTheBarsApprovedBusinessDay() throws Exception {
        String billId = bill("noon");
        settle(billId, "noon", 10_000);
        jdbc.update("UPDATE bar_bill_settlements SET settled_at = ? WHERE bill_id = ?",
                Timestamp.from(Instant.parse("2026-08-01T03:00:00Z")), billId);
        Instant effective = Instant.parse("2015-01-01T00:00:00Z");
        jdbc.update("""
                INSERT INTO bar_business_day_configs(id,bar_id,project_id,version,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                VALUES(?,?,?,'V0.01',12,0,23,7,0,23,?,'approved','测试已批准版本','bpt_requester',?,'usr_master',?)
                """, "bbd_bpt_" + s, bar, project, Timestamp.from(effective),
                Timestamp.from(effective.minusSeconds(7200)), Timestamp.from(effective.minusSeconds(3600)));
        days.invalidate();

        assertThat(finance.computePeriodTurnoverRent(bar, contract, "2026-07")).isEqualTo(1_000L);
        assertThat(finance.computePeriodTurnoverRent(bar, contract, "2026-08")).isZero();
    }

    /**
     * ⑥（138-R4，BSM:1122「所有服务费由公司统一收款……全额进公司账——不存在钱不进账的情况；分给服务人员的部分，是入账之后按薪资/提成/成本名录列支的支出」；
     * BSM:1714「点舞等专项服务的收款主体为该服务发生地所属的酒吧商户」；决策台账 §17「客人仍向酒吧全额付款，酒吧确认对客收入」；ENUM:290 服务完成后不可退）：
     * 点单员在公司工作手机上给本酒吧代客下单点舞 30000、确认收款、完成履约（管理费 10%）→ 全额 30000 计入；已收款后取消的、只收款未完成的、
     * 他店已完成的单不计 → 30000 × 10% = 3000。
     */
    @Test
    void completedServiceOrderCountsInFullForTheBar() throws Exception {
        String performer = "usr_bpt_dancer_" + s;
        jdbc.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at)"
                + " VALUES (?, ?, NULL, ?, '抽成舞者', 'approved', 'active', now(), now())", "bpt_sprof_" + s, project, performer);
        String dance = "bpt_dance_" + s;
        jdbc.update("INSERT INTO service_catalog_items (id, name, price_cent, duration_minutes, active, mgmt_fee_mode, mgmt_fee_value, updated_at)"
                + " VALUES (?, '点舞', 30000, 15, TRUE, 'percent', 1000, now())", dance);
        // 工作手机按空间绑定本酒吧吧台区（登记设备号即请求头 X-Device-Id）。
        String device = "bpt-phone-" + s;
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, bound_space_id,"
                + " location_point_id, status, bound_at, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'staff_work_phone', 'space', ?, ?, 'bound', now(), now(), now())",
                "td_bpt_" + s, device, device, space, space);
        RequestPostProcessor web = new StaffRequestFixture(jdbc).staff("usr_bpt_waiter_" + s, "staff", project, bar,
                "service.order.create");
        RequestPostProcessor waiter = request -> {
            MockHttpServletRequest r = web.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "staff_work_phone");
            r.addHeader("X-Device-Id", device);
            return r;
        };
        String other = "bpt_bar_svc_other_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'他店点舞酒吧','bar','active',?,now(),now())",
                other, project);

        String completed = serviceOrder(waiter, "done", dance, performer, bar, table);
        servicePost(waiter, completed, "mark-paid", "done", Map.of());
        servicePost(waiter, completed, "complete", "done", Map.of());
        String cancelled = serviceOrder(waiter, "cancel", dance, performer, bar, table);
        servicePost(waiter, cancelled, "mark-paid", "cancel", Map.of());
        servicePost(waiter, cancelled, "cancel", "cancel", Map.of("reason", "客人临时取消"));
        String paidOnly = serviceOrder(waiter, "paid", dance, performer, bar, table);
        servicePost(waiter, paidOnly, "mark-paid", "paid", Map.of());
        String elsewhere = serviceOrder(waiter, "other", dance, performer, other, null);
        servicePost(waiter, elsewhere, "mark-paid", "other", Map.of());
        servicePost(waiter, elsewhere, "complete", "other", Map.of());

        YearMonth month = barMonth(Instant.now());
        // 第十八批 V 段 159-R4：抽成出账要求账期内营业日的订单与核销、退款与冲正已标记为最终（本用例不测该闸，整段标齐）。
        markRangeFinal(jdbc, "bar", bar, month.atDay(1), month.atEndOfMonth());
        assertThat(finance.computePeriodTurnoverRent(bar, contract, month.toString())).isEqualTo(3_000L);
    }

    // ---------- helpers ----------

    private static final Instant JULY = Instant.parse("2026-07-15T06:00:00Z");
    private static final Instant AUGUST = Instant.parse("2026-08-12T06:00:00Z");
    private int refunds;

    /** 平台管理员按月出抽成账单（本文件 ① 的真实形状）。 */
    private ResultActions billTurnoverRent(String ruleId, YearMonth month) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/turnover-rent/bills").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bpt-bill-" + month + "-" + s, "rule_id", ruleId,
                        "period_start", month.atDay(1).toString(), "period_end", month.atEndOfMonth().toString(),
                        "due_date", month.atEndOfMonth().plusDays(10).toString()))));
    }

    /** 155-R1：本酒吧合同（id 即抽成规则所挂合同），收租方式 mode、月租 baseRent，2026 全年生效。 */
    private void contract(String mode, long baseRent) {
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?,?,?,?,DATE '2026-01-01',DATE '2026-12-31','effective',?,?::jsonb,now(),now())""",
                contract, bar, "bpt_subj_" + s, "bpt_subj_" + s, project,
                "{\"rentBillingMode\":\"" + mode + "\",\"baseRentCent\":" + baseRent + "}");
    }

    /** 合同工作台「排租金AR」（ContractWorkbenchPage 发 {period}，adminClient 自动补 idempotency_key 并转蛇形）。 */
    private ResultActions periodBills(String period) throws Exception {
        return mockMvc.perform(post("/api/v1/contracts/" + contract + "/period-bills").with(contractBiller)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("period", period, "idempotency_key", "bpt-pb-" + UUID.randomUUID()))));
    }

    /**
     * 裁决 36 场景：本酒吧 mode 合同（月租 baseRent）、规则 10%（保底 ruleMin），7 月结清 julySales 的账单在 8 月退成商品 10000，8 月另结清 augustSales；
     * 合同工作台依次出 7 月、8 月，返回 7 月那张账单。
     */
    private String decision36(String mode, long baseRent, Long ruleMin, long julySales, long augustSales) throws Exception {
        contract(mode, baseRent);
        jdbc.update("UPDATE turnover_rent_rules SET min_amount_cent = ? WHERE id = ?", ruleMin, rule);
        String july = seededBill("d36july", bar, table, julySales, 0, 0, julySales - 10_000, julySales, "settled", JULY);
        if (augustSales > 0) {
            seededBill("d36aug", bar, table, augustSales, 0, 0, augustSales, augustSales, "settled", Instant.parse("2026-08-05T06:00:00Z"));
        }
        refund(july, 10_000, AUGUST);
        periodBills("2026-07").andExpect(status().isOk());
        periodBills("2026-08").andExpect(status().isOk());
        return july;
    }

    private Long rentBill(String period) {
        return jdbc.queryForList("SELECT amount_cent FROM contract_rent_bills WHERE contract_id = ? AND period = ? AND fee_type = 'rent'",
                Long.class, contract, period).stream().findFirst().orElse(null);
    }

    /** 本酒吧的营业额抽成冲正应付：退款单 → 金额（按金额降序）。 */
    private Map<String, Long> reversals() {
        Map<String, Long> out = new LinkedHashMap<>();
        jdbc.query("SELECT source_id, amount_cent FROM ar_ap_items WHERE source_type = 'turnover_rent_reversal' AND counterparty_id = ?"
                + " ORDER BY amount_cent DESC", rs -> {
                    out.put(rs.getString("source_id"), rs.getLong("amount_cent"));
                }, bar);
        return out;
    }

    /** 该账单在 7 月的逐笔快照：分段（jsonb 相等）与原月是否按保底收租。 */
    private void assertSnapshot(String billId, String segments, boolean billedByMinimum) {
        assertThat(jdbc.queryForList("SELECT period, billed_by_minimum, segments = ?::jsonb AS segments_match"
                + " FROM turnover_rent_sale_snapshots WHERE source_kind = 'bar_bill' AND source_id = ?", segments, billId))
                .containsExactly(Map.of("period", "2026-07", "billed_by_minimum", billedByMinimum, "segments_match", true));
    }

    /** 前置态：已结清账单的一笔商品退款（挂退款应付并于 at 付讫，即在 at 退成），返回退款单 id。 */
    private String refund(String billId, long amountCent, Instant at) {
        String id = "bpt_rf_" + (++refunds) + "_" + s;
        String payable = "bpt_ap_" + refunds + "_" + s;
        jdbc.update("INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,unpaid_amount_cent,due_date,"
                + "status,created_at,updated_at) VALUES(?,?,'payable',?,'refund',?,?,?,DATE '2026-08-31','open',now(),now())",
                payable, payable, bar, id, amountCent, amountCent);
        jdbc.update("INSERT INTO bar_bill_adjustments(id,bill_id,adjustment_type,amount_cent,before_payable_amount_cent,after_payable_amount_cent,reason,"
                + "requested_by_user_id,approval_required,status,idempotency_key,created_at,refunded_at,ar_ap_item_id)"
                + " VALUES(?,?,'refund',?,?,0,'客人退部分商品',?,TRUE,'applied',?,?,?,?)",
                id, billId, amountCent, amountCent, cashierId, "bpt-refund-" + id, Timestamp.from(at), Timestamp.from(at), payable);
        jdbc.update("INSERT INTO bar_payroll_refund_lines(id,adjustment_id,collection_bill_id,source_bill_id,component_type,amount_cent,ar_ap_item_id)"
                + " VALUES(?,?,?,?,'goods',?,?)", "bprf_" + id, id, billId, billId, amountCent, payable);
        jdbc.update("INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,created_at)"
                + " VALUES(?,?,?,'bank_transfer',?,now())", "bpt_pay_" + id, payable, amountCent, Timestamp.from(at));
        return id;
    }

    /**
     * 后台网页收银按原账单明细办一笔商品退款（同 ④-⑨ 的真实形状：首次提交 409 建主管复核单、批准后原样再提交），
     * 后台财务按契约登记退款应付于 8 月付讫；返回系统生成的退款单号。
     */
    private String webRefund(RequestPostProcessor refunder, String refunderId, String billId, long amountCent, String key)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("adjustment_type", "refund");
        body.put("amount_cent", amountCent);
        body.put("reason", "客人退部分商品");
        body.put("evidence_attachment_ids", List.of());
        body.put("refund_lines", List.of(Map.of("source_bill_id", billId, "component_type", "goods", "amount_cent", amountCent)));
        body.put("idempotency_key", key);
        mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/adjustments").with(refunder)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        jdbc.update("UPDATE supervisor_review_decisions SET review_status = 'approved', decision = 'approved', reviewer_level = 3"
                + " WHERE action_type = 'bar.bill_waiver' AND object_id = ? AND initiated_by = ? AND review_status = 'pending_review'",
                billId, refunderId);
        JsonNode adjustment = data(mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/adjustments").with(refunder)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isOk()));
        mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + adjustment.path("ar_ap_item_id").asText() + "/payments").with(refunder)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", key + "-pay", "amount_cent", amountCent,
                        "payment_method", "bank_transfer", "payment_time", AUGUST.toString()))))
                .andExpect(status().isOk());
        return adjustment.path("id").asText();
    }

    /** 网页收银吧台位按商品与数量下单（2 瓶，10000）后生成待支付账单，返回账单 id。 */
    private String bill(String tag) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bpt-order-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", product, "quantity", "2")));
        String order = data(mockMvc.perform(post("/api/v1/bar/orders").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isOk())).path("id").asText();
        return data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bpt-bill-" + tag + "-" + s))))
                .andExpect(status().isOk())).path("id").asText();
    }

    /** 网页收银现金结账（同后台真实请求形状）。 */
    private void settle(String billId, String tag, long amountCent) throws Exception {
        String key = "bpt-settle-" + tag + "-" + s;
        mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/settle").with(cashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", key, "payment_id", key,
                                "settlement_amount_cent", amountCent, "payment_channel", "cash"))))
                .andExpect(status().isOk());
    }

    /** 点单员代客下单（同工作手机 ServiceOrderCreateReq 的蛇形键，空字段不发），返回服务单 id。 */
    private String serviceOrder(RequestPostProcessor waiter, String tag, String item, String performer, String barId, String tableId)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bpt-svc-" + tag + "-" + s);
        body.put("catalog_item_id", item);
        body.put("performer_user_id", performer);
        body.put("bar_id", barId);
        if (tableId != null) {
            body.put("table_id", tableId);
        }
        return data(mockMvc.perform(post("/api/v1/services/orders").with(waiter).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk())).path("id").asText();
    }

    /** 服务单确认收款（mark-paid）、完成履约（complete）、取消（cancel，带 reason）。 */
    private void servicePost(RequestPostProcessor waiter, String orderId, String action, String tag, Map<String, Object> extra)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>(extra);
        body.put("idempotency_key", "bpt-svc-" + action + "-" + tag + "-" + s);
        mockMvc.perform(post("/api/v1/services/orders/" + orderId + "/" + action).with(waiter).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk());
    }

    private YearMonth barMonth(Instant at) {
        return YearMonth.from(days.businessDate(null, bar, at));
    }

    private String table(String barId, String tag, String type) {
        String id = "bpt_table_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at, space_id)"
                + " VALUES (?, ?, ?, ?, '抽成台位', ?, 4, 0, 0, 'CNY', 'active', now(), now(), ?)",
                id, barId, barId, "BPT-" + tag.toUpperCase() + "-" + s, type, space);
        return id;
    }

    /** 前置态：一张账单（结清时带一笔未冲正收款流水）。 */
    private String seededBill(String tag, String barId, String tableId, long subtotal, long discount, long tip, long payable,
            long paid, String settlementStatus, Instant settledAt) {
        String order = "bpt_order_" + tag + "_" + s;
        String id = "bpt_billx_" + tag + "_" + s;
        Timestamp at = Timestamp.from(settledAt == null ? Instant.now() : settledAt);
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at)"
                + " VALUES(?,?,?,?,'staff','created',?,'CNY',?,?)", order, barId, barId, tableId, subtotal, at, at);
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,"
                + "deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at)"
                + " VALUES(?,?,?,?,?,?,?,?,0,?,?,'CNY',?,?,?,?,?)", id, "BPT" + tag + s, order, barId, barId, tableId, subtotal, discount,
                payable, paid, paid > 0 ? "paid" : "unpaid", settlementStatus, tip, at, at);
        if (paid > 0) {
            jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,settlement_amount_cent,merchant_id,settled_at)"
                    + " VALUES(?,?,'aggregate_pay',?,?,?)", "bpt_pay_" + tag + "_" + s, id, paid, barId, at);
        }
        return id;
    }

    private String session(String barId, String tableId) {
        String id = "bpt_session_" + tableId.hashCode() + "_" + s;
        Timestamp at = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,service_staff_id,status,opened_at,closed_at,created_at,updated_at)"
                + " VALUES(?,?,?,current_date,?,?,'closed',?,?,?,?)", id, barId, barId, tableId, cashierId, at, at, at, at);
        return id;
    }

    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }
}
