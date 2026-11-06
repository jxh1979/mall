package com.complex.platform.finance;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
 * 第十八批 V 段第 159 条 159-R4 营业额抽成出账闸真库回归。依据：ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；
 * ENUM:257「营业额＝该商户当期在本综合体内含税实收成交总额，减去当期已成功的退款金额」；第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」。
 *
 * <p>夹具随测试事务回滚：综合体与酒吧 B 都建档 2026-06-01；B 有营业额抽成合同（turnover_only，规则 10%）与固定租金合同（fixed_only，月租 100000），
 * 2026-08-10 结清整单 10000。B 的 8 月日结除 08-20 外五类全标；08-20 的日结经真实生成、标记接口（蛇形键）由持 finance.daily_close.lock、
 * 负责范围覆盖该综合体的财务 BP 生成并标除「当日退款与冲正记录」外四类。合同工作台「排租金AR」与平台管理员出抽成账单都按前端蛇形键走真实入口与授权链。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TurnoverRentFinalSnapshotGateJdbcTests {

    private static final LocalDate AUG_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_20 = LocalDate.of(2026, 8, 20);
    private static final LocalDate AUG_31 = LocalDate.of(2026, 8, 31);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "trg_prj_" + s;
    private final String space = "trg_space_" + s;

    // 修复前出账不判五类标记：第一条 409 断言即红（返回 200）。
    @Test
    void turnoverRentIsNotBilledUntilEveryBusinessDayInPeriodHasOrdersAndRefundsMarkedFinal() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex','抽成闸综合体',?,TIMESTAMPTZ '2026-06-01 00:00:00+08',now())", project, "active");
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '抽成闸吧台区', 'shop', 'active', now(), now())", space, project);
        String bar = "trg_bar_" + s;
        String barName = "抽成闸酒吧" + s;
        bar(bar, barName, Instant.parse("2026-05-31T16:00:00Z"));
        String turnoverContract = contract("trg_ct_" + s, bar, "turnover_only", 0);
        String rule = rule(turnoverContract, bar);
        String fixedContract = contract("trg_cf_" + s, bar, "fixed_only", 100_000);
        seededBill("b", bar, 10_000, Instant.parse("2026-08-10T06:00:00Z"));

        markRangeFinal(jdbc, "bar", bar, AUG_1, AUG_20.minusDays(1));
        markRangeFinal(jdbc, "bar", bar, AUG_20.plusDays(1), AUG_31);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor financeBp = staffs.staff("usr_trg_bp_" + s, "staff", project, null, "finance.daily_close.lock");
        String close20 = JsonPath.read(mockMvc.perform(post("/api/v1/finance/daily-closes").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"trg-gen-" + s + "\",\"business_date\":\"" + AUG_20
                                + "\",\"scope_type\":\"bar\",\"scope_id\":\"" + bar + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        for (String category : List.of("orders_verifications", "production_handover", "attendance", "anomaly_resolutions")) {
            finalMark(financeBp, close20, category, "trg-" + category + "-" + s);
        }

        String notFinal = "以下营业日数据尚未标记为最终，不能出营业额抽成账单：2026-08-20 " + barName + "：当日退款与冲正记录";
        RequestPostProcessor contractBiller = staffs.staff("usr_trg_fin_" + s, "staff", project, null, "finance.ar_ap.manage");
        periodBills(contractBiller, turnoverContract, "2026-08")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TURNOVER_DATA_NOT_FINAL"))
                .andExpect(jsonPath("$.message").value(notFinal));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contract_rent_bills WHERE contract_id = ? AND period = '2026-08'",
                Integer.class, turnoverContract)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turnover_rent_sale_snapshots WHERE merchant_id = ? AND period = '2026-08'",
                Integer.class, bar)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ar_ap_items WHERE source_type = 'turnover_rent_reversal' AND counterparty_id = ?",
                Integer.class, bar)).isZero();

        RequestPostProcessor admin = staffs.staff("usr_trg_admin_" + s, "platform_admin", project, null, "finance.turnover_rent.bill");
        String billKey = "trg-bill-" + s;
        mockMvc.perform(post("/api/v1/finance/turnover-rent/bills").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + billKey + "\",\"rule_id\":\"" + rule + "\",\"period_start\":\"2026-08-01\","
                                + "\"period_end\":\"2026-08-31\",\"due_date\":\"2026-09-10\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TURNOVER_DATA_NOT_FINAL"))
                .andExpect(jsonPath("$.message").value(notFinal));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ar_ap_items WHERE source_type = 'turnover_rent' AND source_id = ?",
                Integer.class, rule)).isZero();
        assertThat(jdbc.queryForList("SELECT metadata->>'error_code' FROM audit_events WHERE event_name = 'finance.turnover_rent.bill_failed'"
                + " AND event_result = 'failure' AND idempotency_key = ?", String.class, billKey)).containsExactly("TURNOVER_DATA_NOT_FINAL");

        // 固定租金合同不取营业额，不受本闸影响。
        periodBills(contractBiller, fixedContract, "2026-08").andExpect(status().isOk());
        assertThat(rentBill(fixedContract, "2026-08")).isEqualTo(100_000L);

        finalMark(financeBp, close20, "refunds_reversals", "trg-refunds-" + s);
        periodBills(contractBiller, turnoverContract, "2026-08").andExpect(status().isOk());
        assertThat(rentBill(turnoverContract, "2026-08")).isEqualTo(1_000L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turnover_rent_sale_snapshots WHERE merchant_id = ? AND period = '2026-08'",
                Integer.class, bar)).isEqualTo(1);
    }

    /** 建档 2026-08-15T03:00:00Z（北京 11:00，8-14 营业日在 8-15 10:00 结束、不查）的酒吧：只标 08-15～08-31 即可出 8 月抽成。 */
    @Test
    void businessDaysBeforeOnboardingAreNotRequiredToBeFinal() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at)"
                + " VALUES(?,'complex','抽成闸综合体',?,TIMESTAMPTZ '2026-06-01 00:00:00+08',now())", project, "active");
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '抽成闸吧台区', 'shop', 'active', now(), now())", space, project);
        String bar = "trg_late_" + s;
        bar(bar, "抽成闸新开酒吧" + s, Instant.parse("2026-08-15T03:00:00Z"));
        String turnoverContract = contract("trg_ctl_" + s, bar, "turnover_only", 0);
        rule(turnoverContract, bar);
        seededBill("late", bar, 10_000, Instant.parse("2026-08-20T06:00:00Z"));
        markRangeFinal(jdbc, "bar", bar, LocalDate.of(2026, 8, 15), AUG_31);

        RequestPostProcessor contractBiller = new StaffRequestFixture(jdbc).staff("usr_trg_finl_" + s, "staff", project, null,
                "finance.ar_ap.manage");
        periodBills(contractBiller, turnoverContract, "2026-08").andExpect(status().isOk());
        assertThat(rentBill(turnoverContract, "2026-08")).isEqualTo(1_000L);
    }

    // ---------- helpers ----------

    private void bar(String id, String name, Instant created) {
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'bar','active',?,?,now())", id, name, project, Timestamp.from(created));
    }

    /** 本酒吧合同：收租方式 mode、月租 baseRent，2026 全年生效。 */
    private String contract(String id, String bar, String mode, long baseRent) {
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?,?,?,?,DATE '2026-01-01',DATE '2026-12-31','effective',?,?::jsonb,now(),now())""",
                id, bar, "trg_subj_" + id, "trg_subj_" + id, project,
                "{\"rentBillingMode\":\"" + mode + "\",\"baseRentCent\":" + baseRent + "}");
        return id;
    }

    /** 抽成规则：10%、无保底，已复核生效。 */
    private String rule(String contractId, String bar) {
        String id = contractId + "_rule";
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '抽成闸回归规则', 1000, 0, DATE '2020-01-01', NULL, 'active', FALSE)
                """, id, contractId, bar);
        return id;
    }

    /** 前置态：本酒吧吧台位一张整单结清的账单（带一笔未冲正收款流水）。 */
    private void seededBill(String tag, String bar, long amountCent, Instant settledAt) {
        String table = "trg_table_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at, space_id)"
                + " VALUES (?, ?, ?, ?, '抽成闸台位', 'bar_counter', 4, 0, 0, 'CNY', 'active', now(), now(), ?)",
                table, bar, bar, "TRG-" + tag.toUpperCase() + "-" + s, space);
        String order = "trg_order_" + tag + "_" + s;
        String bill = "trg_bill_" + tag + "_" + s;
        Timestamp at = Timestamp.from(settledAt);
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at)"
                + " VALUES(?,?,?,?,'staff','created',?,'CNY',?,?)", order, bar, bar, table, amountCent, at, at);
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,"
                + "deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at)"
                + " VALUES(?,?,?,?,?,?,?,0,0,?,?,'CNY','paid','settled',0,?,?)", bill, "TRG" + tag + s, order, bar, bar, table,
                amountCent, amountCent, amountCent, at, at);
        jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,settlement_amount_cent,merchant_id,settled_at)"
                + " VALUES(?,?,'aggregate_pay',?,?,?)", "trg_pay_" + tag + "_" + s, bill, amountCent, bar, at);
    }

    /** 合同工作台「排租金AR」（ContractWorkbenchPage 发 {period}，adminClient 自动补 idempotency_key 并转蛇形）。 */
    private ResultActions periodBills(RequestPostProcessor who, String contractId, String period) throws Exception {
        return mockMvc.perform(post("/api/v1/contracts/" + contractId + "/period-bills").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"trg-pb-" + UUID.randomUUID() + "\",\"period\":\"" + period + "\"}"));
    }

    private Long rentBill(String contractId, String period) {
        return jdbc.queryForList("SELECT amount_cent FROM contract_rent_bills WHERE contract_id = ? AND period = ? AND fee_type = 'rent'",
                Long.class, contractId, period).stream().findFirst().orElse(null);
    }

    /** 143-R2 五类最终标记的真实请求形状（蛇形键）。 */
    private void finalMark(RequestPostProcessor who, String dailyCloseId, String category, String key) throws Exception {
        mockMvc.perform(post("/api/v1/finance/daily-closes/" + dailyCloseId + "/final-marks").with(who)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + key + "\",\"category\":\"" + category + "\"}"))
                .andExpect(status().isOk());
    }
}
