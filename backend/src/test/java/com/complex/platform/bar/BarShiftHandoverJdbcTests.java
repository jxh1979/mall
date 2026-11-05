package com.complex.platform.bar;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.DailyCloseFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
 * 第十八批 R 段第 142 条 142-R2（BSM:1081「当班收银员与接班人双方确认签字（电子确认同效；营业日最后一班没有接班人的，由财务跟班代替接班人确认签字），
 * 财务跟班见证后归档」；ENUM:190「日结的确认与锁定：由当班收银员完成现金清点与盘收银，接班人与其对接完成交接，并由财务跟班跟进交接完成」）：
 * 交班单双方签字、财务见证归档（最后一班由见证财务代接班人签字）；营业日有未完成交接的班次不得确认日结。
 * 开班、班结、复核、签字、见证与日结都按后台真实请求形状（蛇形键）走真实入口与真实授权链。
 * 失败的请求会把测试事务标成只回滚，之后班结的准入闸（保存点）即按失败关闭——各用例先把要班结的班次都班结完再发预期失败的请求。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarShiftHandoverJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BusinessDayResolver days;
    @Autowired private FinanceRepository financeRepository;

    private String s;
    private String bar;
    private String otherBar;
    private String cashierAId;
    private String cashierBId;
    private String otherCashierId;
    private String financeId;
    private RequestPostProcessor cashierA;
    private RequestPostProcessor cashierB;
    private RequestPostProcessor stranger;
    private RequestPostProcessor otherCashier;
    private RequestPostProcessor finance;

    @BeforeEach
    void seed() {
        s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String project = "project_bsh_" + s;
        bar = "bar_bsh_" + s;
        otherBar = "bar_bsh_other_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '交班签字综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '交班签字酒吧', 'bar', 'active', now(), now(), ?), (?, '交班签字另一酒吧', 'bar', 'active', now(), now(), ?)",
                bar, project, otherBar, project);
        // 班结过合同准入闸：两家酒吧各有一份含酒吧业务的生效合同。
        for (String merchant : new String[] {bar, otherBar}) {
            jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                    + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                    + "'effective','[\"bar\"]'::jsonb,now(),now())", "bsh_ct_" + merchant, merchant, merchant, merchant);
        }
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashierAId = "usr_bsh_a_" + s;
        cashierBId = "usr_bsh_b_" + s;
        otherCashierId = "usr_bsh_o_" + s;
        financeId = "usr_bsh_fin_" + s;
        cashierA = staffs.staff(cashierAId, "staff", project, bar, "bar.shift.manage", "bar.shift.close");
        cashierB = staffs.staff(cashierBId, "staff", project, bar, "bar.shift.manage", "bar.shift.close");
        stranger = staffs.staff("usr_bsh_x_" + s, "staff", project, bar, "bar.shift.manage", "bar.shift.close");
        otherCashier = staffs.staff(otherCashierId, "staff", project, otherBar, "bar.shift.manage", "bar.shift.close");
        finance = staffs.staff(financeId, "staff", project, null, "finance.daily_close.manage", "finance.daily_close.lock");
    }

    /** 填了接班人须是已有账号：填错账号 400（否则永远签不了、日结被卡死）。 */
    @Test
    void handoverMustBeAnExistingAccount() throws Exception {
        String shift = open(cashierA, cashierAId, bar, "ghost");
        close(cashierA, shift, "ghost", 20_000, "usr_bsh_nobody_" + s)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(containsString("接班人账号不存在")));
    }

    /** 有差额未平账不能签；平账后收银员与接班人各签本人一栏、不能重签、第三人不能签；双方签完财务才能见证归档，见证后不能再见证。 */
    @Test
    void cashierAndSuccessorSignThenFinanceWitnesses() throws Exception {
        String shift = open(cashierA, cashierAId, bar, "pair");
        close(cashierA, shift, "pair", 19_900, cashierBId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needs_review").value(true));

        sign(cashierA, shift, "early").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_NOT_BALANCED"));
        resolve(shift).andExpect(status().isOk()).andExpect(jsonPath("$.data.needs_review").value(false));

        sign(cashierA, shift, "a").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cashier_signed_at").isNotEmpty())
                .andExpect(jsonPath("$.data.successor_signed_at").doesNotExist());
        sign(cashierA, shift, "a2").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_ALREADY_SIGNED"));
        sign(stranger, shift, "x").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_SIGNER_MISMATCH"));

        witness(shift, "w1").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_NOT_SIGNED"));
        sign(cashierB, shift, "b").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successor_signed_at").isNotEmpty());
        witness(shift, "w2").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.witnessed_by_user_id").value(financeId))
                .andExpect(jsonPath("$.data.witnessed_at").isNotEmpty());
        witness(shift, "w3").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_ALREADY_WITNESSED"));
    }

    /**
     * 营业日最后一班（没有接班人）：收银员签后由见证财务代接班人签字并归档；本酒吧当营业日有未见证的班次时日结确认 409，见证后可确认；
     * 另一酒吧开班中的班次不挡本酒吧 merchant 范围的日结。
     */
    @Test
    void lastShiftWitnessCountersignsAndDayCloseWaitsForHandover() throws Exception {
        String shift = open(cashierA, cashierAId, bar, "last");
        close(cashierA, shift, "last", 20_000, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needs_review").value(false));
        open(otherCashier, otherCashierId, otherBar, "other");
        String dailyClose = generate();

        confirm(dailyClose, "c1").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_SHIFT_PENDING"));
        sign(cashierA, shift, "last").andExpect(status().isOk());
        confirm(dailyClose, "c2").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_SHIFT_PENDING"));
        ResultActions witnessed = witness(shift, "last").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.witnessed_by_user_id").value(financeId));
        assertThat(CustomerTicketPaymentFixture.read(witnessed, "$.data.successor_signed_at"))
                .isNotNull()
                .isEqualTo(CustomerTicketPaymentFixture.read(witnessed, "$.data.witnessed_at"));

        confirm(dailyClose, "c3").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    /** 开班中（未班结）的班次同样挡本酒吧当营业日的日结。 */
    @Test
    void openShiftBlocksDayClose() throws Exception {
        open(cashierA, cashierAId, bar, "still-open");
        String dailyClose = generate();
        confirm(dailyClose, "open").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_SHIFT_PENDING"));
    }

    /**
     * 第十八批 R 段第 143 条 143-R6（ENUM:190「待生成、待确认、有差异、已确认、已锁定、已归档」；ENUM:115「有差异待复核」；
     * BSM:1081「出现任何金额的差额一律进复核、定位责任，不设容差」）：本酒吧当营业日有一张有差异待复核的收银班次时，日结列表里本酒吧
     * merchant 范围的待确认日结显示 has_difference；财务复核平账后回到 pending_confirm；另一酒吧有差异待复核的班次不影响本酒吧的日结。
     */
    @Test
    void unbalancedShiftShowsDayCloseAsHasDifferenceUntilResolved() throws Exception {
        String shift = open(cashierA, cashierAId, bar, "diff");
        String other = open(otherCashier, otherCashierId, otherBar, "other-diff");
        generate();
        close(cashierA, shift, "diff", 19_900, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needs_review").value(true));
        close(otherCashier, other, "other-diff", 19_900, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needs_review").value(true));

        listed().andExpect(jsonPath("$.data.items[0].status").value("has_difference"));
        resolve(shift).andExpect(status().isOk()).andExpect(jsonPath("$.data.needs_review").value(false));
        listed().andExpect(jsonPath("$.data.items[0].status").value("pending_confirm"));
    }

    // ---------- helpers ----------

    /** 收银员开班（备用金 200 元，无收款），返回班次号。 */
    private String open(RequestPostProcessor who, String cashierId, String barId, String tag) throws Exception {
        return CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/bar/shifts/open").with(who)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bsh-open-" + tag + "-" + s, "bar_id", barId,
                                "cashier_user_id", cashierId, "opening_cash_cent", 20_000))))
                .andExpect(status().isOk()), "$.data.id");
    }

    private ResultActions close(RequestPostProcessor who, String shiftId, String tag, long declaredCashCent, String handoverTo)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bsh-close-" + tag + "-" + s);
        body.put("declared_cash_cent", declaredCashCent);
        if (handoverTo != null) {
            body.put("handover_to_user_id", handoverTo);
        }
        return mockMvc.perform(post("/api/v1/bar/shifts/" + shiftId + "/close").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions resolve(String shiftId) throws Exception {
        return action(finance, shiftId, "resolve", "bsh-resolve-" + s);
    }

    private ResultActions sign(RequestPostProcessor who, String shiftId, String tag) throws Exception {
        return action(who, shiftId, "sign", "bsh-sign-" + tag + "-" + s);
    }

    private ResultActions witness(String shiftId, String tag) throws Exception {
        return action(finance, shiftId, "witness", "bsh-witness-" + tag + "-" + s);
    }

    private ResultActions action(RequestPostProcessor who, String shiftId, String action, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/shifts/" + shiftId + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", key))));
    }

    /** 财务按后台真实请求生成本酒吧商户当营业日日结（待确认）并直插五类最终标记（143-R2 的闸不在本类考查），返回日结号。 */
    private String generate() throws Exception {
        LocalDate day = days.businessDate(null, bar, Instant.now());
        String id = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/finance/daily-closes").with(finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bsh-close-day-" + s, "operator_note", "日结",
                                "business_date", day.toString(), "scope_type", "merchant", "scope_id", bar))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_confirm")), "$.data.id");
        DailyCloseFixture.markAllFinal(financeRepository, id);
        return id;
    }

    private ResultActions confirm(String dailyCloseId, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/daily-closes/" + dailyCloseId + "/confirm").with(finance)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bsh-confirm-" + tag + "-" + s))));
    }

    /** 财务按后台日结区块的真实查询查本酒吧 merchant 范围当营业日的日结（只此一张）。 */
    private ResultActions listed() throws Exception {
        LocalDate day = days.businessDate(null, bar, Instant.now());
        return mockMvc.perform(get("/api/v1/finance/daily-closes?business_date=" + day + "&scope_type=merchant&scope_id=" + bar
                        + "&page=1&size=1").with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }
}
