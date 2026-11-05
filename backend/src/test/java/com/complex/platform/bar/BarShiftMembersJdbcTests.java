package com.complex.platform.bar;

// 文件版本：V0.01

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
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
 * 第十八批 R 段第 142 条 142-R4（BSM:1081「单人当班或能够明确定位到具体收银员时，由该收银员补足；多人共同使用同一钱箱时，由整个当班组共同补足。」
 * 「班结按本班次汇总各支付方式应收」「当班收银员与接班人双方确认签字」；BSM:1489、ENUM:283「多人共同使用同一钱箱时由整个当班组共同补足」）：
 * 同一钱箱多人当班——加入钱箱、合并应收、定位责任或当班组共同补足、每名当班收银员签字。
 * 开班、加入、班结、复核、签字与见证都按后台真实请求形状（蛇形键）走真实入口与真实授权链；各笔收款按收款人直插结算流水。
 * 失败的请求会把测试事务标成只回滚，之后班结的准入闸（保存点）即按失败关闭——各用例先把要班结的班次都班结完再发预期失败的请求。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarShiftMembersJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private String s;
    private String bar;
    private String table;
    private String aId;
    private String bId;
    private String cId;
    private String dId;
    private String eId;
    private String financeId;
    private RequestPostProcessor a;
    private RequestPostProcessor b;
    private RequestPostProcessor c;
    private RequestPostProcessor d;
    private RequestPostProcessor e;
    private RequestPostProcessor otherBarCashier;
    private RequestPostProcessor finance;
    private int seq;

    @BeforeEach
    void seed() {
        s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String project = "project_bsm_" + s;
        bar = "bar_bsm_" + s;
        String otherBar = "bar_bsm_other_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '同钱箱当班综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '同钱箱当班酒吧', 'bar', 'active', now(), now(), ?), (?, '同钱箱当班另一酒吧', 'bar', 'active', now(), now(), ?)",
                bar, project, otherBar, project);
        // 班结过合同准入闸：本酒吧有一份含酒吧业务的生效合同。
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                + "'effective','[\"bar\"]'::jsonb,now(),now())", "bsm_ct_" + s, bar, bar, bar);
        table = "table_bsm_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())", table, bar, bar, "S-BSM-" + s);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        aId = "usr_bsm_a_" + s;
        bId = "usr_bsm_b_" + s;
        cId = "usr_bsm_c_" + s;
        dId = "usr_bsm_d_" + s;
        eId = "usr_bsm_e_" + s;
        financeId = "usr_bsm_fin_" + s;
        a = staffs.staff(aId, "staff", project, bar, "bar.shift.manage", "bar.shift.close");
        b = staffs.staff(bId, "staff", project, bar, "bar.shift.manage", "bar.shift.close");
        c = staffs.staff(cId, "staff", project, bar, "bar.shift.manage", "bar.shift.close");
        d = staffs.staff(dId, "staff", project, bar, "bar.shift.manage", "bar.shift.close");
        e = staffs.staff(eId, "staff", project, bar, "bar.shift.manage", "bar.shift.close");
        otherBarCashier = staffs.staff("usr_bsm_o_" + s, "staff", project, otherBar, "bar.shift.manage", "bar.shift.close");
        finance = staffs.staff(financeId, "staff", project, null, "finance.daily_close.manage");
    }

    /**
     * 甲开班后乙加入本钱箱：班结按甲与乙加入后的收款合并应收（乙加入前在自己已交的班收的不计）；短款复核不带责任人即当班组共同补足；
     * 甲签、乙未签时不能见证归档，乙签后可以。
     */
    @Test
    void memberCollectionsMergeAndEveryCashierSignsBeforeWitness() throws Exception {
        String shiftA = open(a, aId, "a");
        String shiftB = open(b, bId, "b-own");
        cash(bId, 3_000);
        close(b, shiftB, "b-own", 20_000 + 3_000, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.system_cash_cent").value(3000));
        join(b, shiftA, "b").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members[0].user_id").value(bId))
                .andExpect(jsonPath("$.data.members[0].joined_at").isNotEmpty());
        cash(aId, 10_000);
        cash(bId, 5_000);
        mockMvc.perform(get("/api/v1/bar/shifts").with(a)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + shiftA + "')].members[0].user_id").value(bId));

        close(a, shiftA, "a", 20_000 + 14_900, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.system_cash_cent").value(15000))
                .andExpect(jsonPath("$.data.cash_diff_cent").value(-100))
                .andExpect(jsonPath("$.data.needs_review").value(true));
        action(finance, shiftA, "resolve", Map.of("idempotency_key", "bsm-resolve-" + s)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needs_review").value(false))
                .andExpect(jsonPath("$.data.responsible_user_id").value(nullValue()));

        action(a, shiftA, "sign", Map.of("idempotency_key", "bsm-sign-a-" + s)).andExpect(status().isOk());
        action(finance, shiftA, "witness", Map.of("idempotency_key", "bsm-witness-1-" + s)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_NOT_SIGNED"));
        action(b, shiftA, "sign", Map.of("idempotency_key", "bsm-sign-b-" + s)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members[0].signed_at").isNotEmpty());
        action(b, shiftA, "sign", Map.of("idempotency_key", "bsm-sign-b2-" + s)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_ALREADY_SIGNED"));
        action(finance, shiftA, "witness", Map.of("idempotency_key", "bsm-witness-2-" + s)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.witnessed_by_user_id").value(financeId));
    }

    /**
     * 加入的闸：接班人不能是本钱箱成员（409 BAR_SHIFT_HANDOVER_TO_SELF）；乙加入后自己开班、再加入别的开班中班次都 409 BAR_SHIFT_ALREADY_OPEN；
     * 开班人加入自己的班次同 409；已交班的班次不能加入（409 BAR_SHIFT_NOT_OPEN）；别的酒吧的商户账号 403。
     */
    @Test
    void joinGates() throws Exception {
        String shiftA = open(a, aId, "a");
        String shiftC = open(c, cId, "c");
        String shiftD = open(d, dId, "d");
        close(d, shiftD, "d", 20_000, null).andExpect(status().isOk());
        join(b, shiftA, "b").andExpect(status().isOk());

        close(a, shiftA, "a-to-member", 20_000, bId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_HANDOVER_TO_SELF"));
        mockMvc.perform(post("/api/v1/bar/shifts/open").with(b).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bsm-open-b-" + s, "bar_id", bar,
                                "cashier_user_id", bId, "opening_cash_cent", 20_000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_ALREADY_OPEN"));
        join(b, shiftC, "b-again").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_ALREADY_OPEN"));
        join(a, shiftA, "a-self").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_ALREADY_OPEN"));
        join(e, shiftD, "e-closed").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_NOT_OPEN"));
        join(otherBarCashier, shiftA, "other").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    /** 有成员的短款班次：复核时带责任人（开班人或成员）即记该人，带非本班次当班收银员 400。 */
    @Test
    void shortageResponsibleMustBeOnShift() throws Exception {
        String shiftA = open(a, aId, "a");
        join(b, shiftA, "b").andExpect(status().isOk());
        close(a, shiftA, "a", 19_900, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cash_diff_cent").value(-100));

        action(finance, shiftA, "resolve", Map.of("idempotency_key", "bsm-resolve-x-" + s, "responsible_user_id", eId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        action(finance, shiftA, "resolve", Map.of("idempotency_key", "bsm-resolve-b-" + s, "responsible_user_id", bId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needs_review").value(false))
                .andExpect(jsonPath("$.data.responsible_user_id").value(bId));
    }

    // ---------- helpers ----------

    /** 收银员按后台真实请求开班（备用金 200 元），返回班次号。 */
    private String open(RequestPostProcessor who, String cashierId, String tag) throws Exception {
        return CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/bar/shifts/open").with(who)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bsm-open-" + tag + "-" + s, "bar_id", bar,
                                "cashier_user_id", cashierId, "opening_cash_cent", 20_000))))
                .andExpect(status().isOk()), "$.data.id");
    }

    private ResultActions join(RequestPostProcessor who, String shiftId, String tag) throws Exception {
        return action(who, shiftId, "join", Map.of("idempotency_key", "bsm-join-" + tag + "-" + s));
    }

    private ResultActions close(RequestPostProcessor who, String shiftId, String tag, long declaredCashCent, String handoverTo)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bsm-close-" + tag + "-" + s);
        body.put("declared_cash_cent", declaredCashCent);
        if (handoverTo != null) {
            body.put("handover_to_user_id", handoverTo);
        }
        return mockMvc.perform(post("/api/v1/bar/shifts/" + shiftId + "/close").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions action(RequestPostProcessor who, String shiftId, String action, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/shifts/" + shiftId + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 本酒吧一张已结清账单的一笔现金收款（收款人＝cashierId，收款时点＝现在）。 */
    private void cash(String cashierId, long cent) {
        String n = s + "_" + (++seq);
        Timestamp at = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at)"
                + " VALUES(?,?,?,?,'staff','created',?,'CNY',?,?)", "order_bsm_" + n, bar, bar, table, cent, at, at);
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,"
                + "deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,"
                + "created_at,updated_at) VALUES(?,?,?,?,?,?,?,0,0,?,?,'CNY','paid','settled',0,?,?)",
                "bill_bsm_" + n, "BSM" + n, "order_bsm_" + n, bar, bar, table, cent, cent, cent, at, at);
        jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,cashier_staff_id,settlement_amount_cent,merchant_id,settled_at)"
                + " VALUES(?,?,'cash',?,?,?,?)", "pay_bsm_" + n, "bill_bsm_" + n, cashierId, cent, bar, at);
    }
}
