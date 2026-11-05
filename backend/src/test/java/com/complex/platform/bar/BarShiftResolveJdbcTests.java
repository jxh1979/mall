package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
 * 第十八批 R 段第 142 条 142-R1（BSM:1081「出现任何金额的差额一律进复核、定位责任」「短款补足即为处理完毕，不另计处罚扣款。
 * 长款同样必须登记，并由财务调取视频录像查明差额产生的原因；长款当期计入该商户营业外收入并单独留痕可查」；ENUM:115「有差异待复核、已平账」；
 * PGM:664「差异认定……由该酒吧对应的财务 BP 负责」）：财务复核平账。开班、班结、复核都按后台真实请求形状（蛇形键）走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarShiftResolveJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private String s;
    private String project;
    private String otherProject;
    private String bar;
    private String financeId;
    private RequestPostProcessor cashier;
    private RequestPostProcessor finance;
    private RequestPostProcessor otherProjectFinance;
    private StaffRequestFixture staffs;

    @BeforeEach
    void seed() {
        s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        project = "project_bsr_" + s;
        otherProject = "project_bsr_other_" + s;
        bar = "bar_bsr_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '交班复核综合体', 'active', now(), now()), (?, 'complex', '交班复核别的综合体', 'active', now(), now())",
                project, otherProject);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '交班复核酒吧', 'bar', 'active', now(), now(), ?)", bar, project);
        // 班结过合同准入闸：本酒吧有一份含酒吧业务的生效合同。
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                + "'effective','[\"bar\"]'::jsonb,now(),now())", "bsr_ct_" + s, bar, bar, bar);
        staffs = new StaffRequestFixture(jdbc);
        cashier = staffs.staff("usr_bsr_cashier_" + s, "staff", project, bar, "bar.shift.manage", "bar.shift.close");
        financeId = "usr_bsr_fin_" + s;
        finance = staffs.staff(financeId, "staff", project, null, "finance.daily_close.manage");
        otherProjectFinance = staffs.staff("usr_bsr_fin_other_" + s, "staff", otherProject, null, "finance.daily_close.manage");
    }

    /** 短款：收银员与别的综合体的财务都不能复核；本综合体财务办理即平账，不收金额、不写台账；已平账再办 409。 */
    @Test
    void shortageResolvedBySameProjectFinanceWithoutLedgerRow() throws Exception {
        String shift = closedShift("short", 19_900);
        assertThat(jdbc.queryForObject("SELECT needs_review FROM bar_shifts WHERE id = ?", Boolean.class, shift)).isTrue();

        resolve(cashier, shift, Map.of("idempotency_key", "bsr-r-cashier-" + s)).andExpect(status().isForbidden());
        resolve(otherProjectFinance, shift, Map.of("idempotency_key", "bsr-r-other-" + s))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));

        resolve(finance, shift, Map.of("idempotency_key", "bsr-r-ok-" + s)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needs_review").value(false))
                .andExpect(jsonPath("$.data.cash_diff_cent").value(-100))
                .andExpect(jsonPath("$.data.resolved_by_user_id").value(financeId))
                .andExpect(jsonPath("$.data.resolved_at").isNotEmpty());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_id = ?", Integer.class, shift)).isZero();

        resolve(finance, shift, Map.of("idempotency_key", "bsr-r-again-" + s))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_NOT_PENDING_REVIEW"));
    }

    /** 长款：不填录像查明的原因 400；填了即平账，记该酒吧营业外收入（内部核算、已确认）恰 1 行；实点正好的班次不在待复核 409。 */
    @Test
    void overageNeedsCauseAndPostsNonOperatingIncome() throws Exception {
        // 先把两班都班结：失败的请求会把测试事务标成只回滚，之后班结的准入闸求值（保存点）即按失败关闭进复核。
        String shift = closedShift("over", 20_100);
        String balanced = closedShift("even", 20_000);

        resolve(finance, shift, Map.of("idempotency_key", "bsr-o-nocause-" + s)).andExpect(status().isBadRequest());
        resolve(finance, shift, Map.of("idempotency_key", "bsr-o-ok-" + s, "overage_cause", "录像见客人多付 1 元"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needs_review").value(false))
                .andExpect(jsonPath("$.data.overage_cause").value("录像见客人多付 1 元"));
        assertThat(jdbc.queryForList("SELECT direction, source_type, counterparty_id, merchant_id, amount_cent, status, project_id"
                + " FROM ar_ap_items WHERE source_id = ?", shift))
                .singleElement()
                .isEqualTo(Map.of("direction", "internal_accounting", "source_type", "bar_shift_overage",
                        "counterparty_id", bar, "merchant_id", bar, "amount_cent", 100L, "status", "settled",
                        "project_id", project));

        resolve(finance, balanced, Map.of("idempotency_key", "bsr-e-" + s))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_NOT_PENDING_REVIEW"));
    }

    /**
     * 第十八批 R 段第 142 条 142-R3（BSM:1848「财务看退款、差异、日结和结算依据。」；PGM:664「差异认定……由该酒吧对应的财务 BP 负责」）：
     * 对账页复核区块按后台真实请求读班次列表——只持日结管理码、本人项目为本综合体的财务 200 且只见本综合体酒吧的班次，
     * 别的综合体酒吧的班次不在其中；交班管理码与日结管理码都不持 403。
     */
    @Test
    void financeWithoutShiftManageListsOnlyReachableProjectShifts() throws Exception {
        String own = closedShift("list", 19_900);
        String otherBar = "bar_bsr_ob_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '交班复核别的综合体酒吧', 'bar', 'active', now(), now(), ?)", otherBar, otherProject);
        RequestPostProcessor otherCashier = staffs.staff("usr_bsr_oc_" + s, "staff", otherProject, otherBar, "bar.shift.manage");
        String foreign = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/bar/shifts/open").with(otherCashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bsr-open-foreign-" + s, "bar_id", otherBar,
                                "cashier_user_id", "usr_bsr_oc_" + s, "opening_cash_cent", 10_000))))
                .andExpect(status().isOk()), "$.data.id");

        mockMvc.perform(get("/api/v1/bar/shifts").with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(own)))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(foreign))))
                .andExpect(jsonPath("$.data.items[*].bar_id", everyItem(equalTo(bar))));
        mockMvc.perform(get("/api/v1/bar/shifts")
                        .with(staffs.staff("usr_bsr_none_" + s, "staff", project, null, "bar.order.view")))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    /** 商户侧收银员开班（备用金 200 元，无收款）后按实点现金班结，返回班次号。 */
    private String closedShift(String tag, long declaredCashCent) throws Exception {
        String shift = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/bar/shifts/open").with(cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bsr-open-" + tag + "-" + s, "bar_id", bar,
                                "cashier_user_id", "usr_bsr_cashier_" + s, "opening_cash_cent", 20_000))))
                .andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(post("/api/v1/bar/shifts/" + shift + "/close").with(cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bsr-close-" + tag + "-" + s,
                                "declared_cash_cent", declaredCashCent))))
                .andExpect(status().isOk());
        return shift;
    }

    private ResultActions resolve(RequestPostProcessor who, String shiftId, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/shifts/" + shiftId + "/resolve").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
}
