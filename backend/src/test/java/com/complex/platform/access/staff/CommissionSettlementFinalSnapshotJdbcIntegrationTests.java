package com.complex.platform.access.staff;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.closeWithMarks;
import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
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
 * 第十八批 V 段第 159 条 159-R8：派驻与营销人员提成月结拨付闸。依据：ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；
 * BSM:1142「营销在某家酒吧促成的订单产生的提成，由该酒吧按月拨付给娱乐平台公司」；BSM:1143「由该酒吧按月拨付给人员所属主体」；
 * 决策台账 §25 Q32「正式提成仅用该项计算依赖的最终确认数据……不要求无关五类数据全最终」；第十八批裁决 35「酒吧逐店逐日查」。
 *
 * <p>夹具随测试事务回滚：两家酒吧 X、Y 都在 2026-01-01 建档，各有一名派驻员工 2026-09-10 一笔已结账计提 3000 分；X 的员工属人事本公司 A，
 * Y 的员工属另一主体 P（不在人事范围内）。X 的 bar 范围 9-01～19、9-21～30 五类标齐，9-20 由财务 BP 经真实接口生成日结并逐类标记；
 * Y 当月没有日结。人事与财务 BP 都是真库角色，请求走真实接口与授权链，请求体按前端蛇形键。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CommissionSettlementFinalSnapshotJdbcIntegrationTests {

    private static final String MONTH = "2026-09";
    private static final LocalDate D20 = LocalDate.of(2026, 9, 20);

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "prj_csf_" + s;
    private final String companyA = "ent_csf_a_" + s;
    private final String companyP = "ent_csf_p_" + s;
    private final String barX = "mer_csf_x_" + s;
    private final String barY = "mer_csf_y_" + s;
    private final String barXName = "提成拨付闸酒吧甲" + s;
    private final String barYName = "提成拨付闸酒吧乙" + s;
    private RequestPostProcessor hr;
    private RequestPostProcessor financeBp;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', now(), now())", project, "提成拨付闸综合体" + s);
        for (String[] bar : List.of(new String[] {barX, barXName}, new String[] {barY, barYName})) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                    + " VALUES (?, ?, 'bar', ?, 'active', TIMESTAMPTZ '2026-01-01 00:00:00+08', now())", bar[0], bar[1], project);
        }
        dispatchedCommission("x", companyA, barX);
        dispatchedCommission("y", companyP, barY);
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        hr = fixture.staff("usr_csf_hr_" + s, "staff", companyA, null, "staff.payroll.manage");
        financeBp = fixture.staff("usr_csf_bp_" + s, "staff", project, null, "finance.daily_close.lock", "finance.daily_close.manage");
        markRangeFinal(jdbc, "bar", barX, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 19));
        markRangeFinal(jdbc, "bar", barX, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 30));
    }

    // 修复前不看最终标记直接拨付：第一条断言（409）即红。范围外的酒吧 Y 当月没有日结，不拦本人范围内 X 的组。
    @Test
    void closeWaitsUntilRefundsOfTheBarAreFinalAndIgnoresGroupsOutOfScope() throws Exception {
        String close = JsonPath.read(postJson(financeBp, "/api/v1/finance/daily-closes",
                "{\"idempotency_key\":\"csf-gen-" + key() + "\",\"business_date\":\"" + D20 + "\",\"scope_type\":\"bar\",\"scope_id\":\""
                        + barX + "\"}").andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        for (String category : List.of("orders_verifications", "production_handover", "attendance", "anomaly_resolutions")) {
            finalMark(close, category);
        }

        closeCommissions().andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"))
                .andExpect(jsonPath("$.detail_code").value("COMMISSION_DATA_NOT_FINAL"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString(D20 + " " + barXName + "：当日退款与冲正记录")))
                .andExpect(jsonPath("$.message").value(Matchers.not(Matchers.containsString(barYName))));
        assertThat(settlementCount(barX)).isZero();
        assertThat(transferAmounts(barX)).isEmpty();

        finalMark(close, "refunds_reversals");
        closeCommissions().andExpect(status().isOk()).andExpect(jsonPath("$.data.settlements").value(1));
        assertThat(transferAmounts(barX)).containsExactly(3000L);
        assertThat(settlementCount(barY)).isZero();
    }

    // Q32「不要求无关五类数据全最终」：9-20 只缺当日考勤记录时照常拨付（修复前后都绿，防闸收得过宽）。
    @Test
    void closeDoesNotWaitForUnrelatedAttendance() throws Exception {
        closeWithMarks(jdbc, "bar", barX, D20, List.of("orders_verifications", "refunds_reversals", "production_handover",
                "anomaly_resolutions"));
        closeCommissions().andExpect(status().isOk()).andExpect(jsonPath("$.data.settlements").value(1));
        assertThat(transferAmounts(barX)).containsExactly(3000L);
    }

    private ResultActions closeCommissions() throws Exception {
        return postJson(hr, "/api/v1/staff/commission-settlements/close",
                "{\"idempotency_key\":\"csf-close-" + key() + "\",\"month\":\"" + MONTH + "\"}");
    }

    private void finalMark(String dailyCloseId, String category) throws Exception {
        postJson(financeBp, "/api/v1/finance/daily-closes/" + dailyCloseId + "/final-marks",
                "{\"idempotency_key\":\"csf-" + category + "-" + key() + "\",\"category\":\"" + category + "\"}")
                .andExpect(status().isOk());
    }

    private ResultActions postJson(RequestPostProcessor session, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int settlementCount(String bar) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM bar_commission_settlements WHERE month = ? AND merchant_id = ?",
                Integer.class, MONTH, bar);
    }

    private List<Long> transferAmounts(String bar) {
        return jdbc.queryForList("SELECT amount_cent FROM ar_ap_items WHERE source_type = 'dispatch_commission_transfer' AND merchant_id = ?",
                Long.class, bar);
    }

    /** 员工（所属主体 companyId）自 9-01 派驻到该酒吧，北京 9-10 12:00 下单、已付清结账的账单及 3000 分提成计提（该酒吧 9-10 营业日）。 */
    private void dispatchedCommission(String tag, String companyId, String bar) {
        String user = "usr_csf_" + tag + "_" + s, table = "tbl_csf_" + tag + "_" + s, order = "ord_csf_" + tag + "_" + s;
        String bill = "bill_csf_" + tag + "_" + s;
        String at = "2026-09-10T12:00:00+08:00";
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', ?, NULL, 'active', now())", user, user, companyId);
        jdbc.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '提成拨付闸卡座', 'booth', 'available', now(), now())", table, bar, table);
        jdbc.update("INSERT INTO bar_orders (id, bar_id, table_id, order_source, status, total_amount_cent, currency, created_at, updated_at,"
                + " order_type) VALUES (?, ?, ?, 'staff', 'settled', 100000, 'CNY', ?::timestamptz, ?::timestamptz, 'standard')",
                order, bar, table, at, at);
        jdbc.update("INSERT INTO bar_bills (id, bill_no, order_id, bar_id, table_id, subtotal_amount_cent, payable_amount_cent,"
                + " paid_amount_cent, currency, payment_status, settlement_status, created_at, updated_at, merchant_id, sales_staff_id)"
                + " VALUES (?, ?, ?, ?, ?, 100000, 100000, 100000, 'CNY', 'paid', 'settled', ?::timestamptz, ?::timestamptz, ?, ?)",
                bill, bill, order, bar, table, at, at, bar, user);
        jdbc.update("INSERT INTO bar_commission_calculations (id, bill_id, sales_staff_id, sales_role_type, commission_base_cent,"
                + " commission_rate_bp, commission_amount_cent, status, created_at)"
                + " VALUES (?, ?, ?, 'staff_sales', 100000, 300, 3000, 'calculated', ?::timestamptz)", "cm_csf_" + tag + "_" + s, bill, user, at);
        jdbc.update("INSERT INTO staff_dispatch_records (id, user_id, company_id, merchant_id, project_id, start_date, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, DATE '2026-09-01', 'active', now())", "dsp_csf_" + tag + "_" + s, user, companyId, bar, project);
    }

    private static String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
