package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.support.TestSecurityActors;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十六批第 16 条真库、真接口回归（请求体按前端 adminClient.mapKeys 后的蛇形键）：ENUM L101「账单状态 待结、部分支付、已结、部分退款、
 * 已退款、异常」；ENUM L221「任何单据进入“异常”后，必须由主管在系统内选择“更正后恢复”或“作废并冲正”两种方式之一处置，填写原因并留痕；
 * 恢复后回到进入异常之前的那个状态」；操作员裁决补充第 2 条「客人对账单提出争议时员工登记置异常。之后都走主管「更正后恢复」或「作废并冲正」」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarBillAnomalyJdbcIntegrationTests {

    private static final String BILLS = "/api/v1/bar/bills/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcBarRepository bars;
    @Autowired private FinanceService finance;
    @Autowired private BusinessDayResolver businessDays;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String merchant = "bba_mer_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private RequestPostProcessor cashier;
    private RequestPostProcessor supervisor;

    @BeforeEach
    void seed() {
        cashier = staff("cashier", "bar.bill.adjust", "finance.payment.register");
        supervisor = staff("sup", "bar.bill.anomaly.resolve");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 待结账单：员工登记争议即置异常，异常期间不能收款、没有处置权限的不能处置；主管「更正后恢复」回到待结，之后照常收款。 */
    @Test
    void disputedUnpaidBillStaysAbnormalUntilSupervisorRestores() throws Exception {
        String bill = bill("unpaid", "unsettled", 0L);
        call(cashier, bill + "/anomaly", "{\"reason\":\"客人称未点第二瓶香槟\",\"idempotency_key\":\"ra_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));
        assertThat(jdbc.queryForObject("SELECT abnormal_prior_status FROM bar_bills WHERE id=?", String.class, bill)).isEqualTo("unpaid");
        call(cashier, bill + "/settle", "{\"payment_id\":\"pay1_%s\",\"payment_channel\":\"aggregate_pay\",\"idempotency_key\":\"st1_%s\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_ABNORMAL"));
        call(cashier, bill + "/anomaly/resolve", "{\"resolution\":\"restored\",\"reason\":\"核实已点\",\"idempotency_key\":\"rs0_%s\"}")
                .andExpect(status().isForbidden());

        call(supervisor, bill + "/anomaly/resolve",
                "{\"resolution\":\"restored\",\"reason\":\"监控核实两瓶均已送达，按原账单收款\",\"idempotency_key\":\"rs_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("unpaid"));
        assertThat(jdbc.queryForObject("SELECT abnormal_prior_status FROM bar_bills WHERE id=?", String.class, bill)).isNull();
        call(cashier, bill + "/settle", "{\"payment_id\":\"pay2_%s\",\"payment_channel\":\"aggregate_pay\",\"idempotency_key\":\"st2_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("paid"));
        assertThat(successes("bar.bill_anomaly_raised", bill)).isEqualTo(1);
        assertThat(successes("bar.bill_anomaly_resolved", bill)).isEqualTo(1);
    }

    /** 已结账单：争议登记后主管「作废并冲正」——账单作废，已登记的收款流水按反结同一做法冲正；作废后不能再收款，也不能再登记争议。 */
    @Test
    void disputedSettledBillVoidedAndCollectionsReversed() throws Exception {
        String bill = bill("paid", "settled", 30000L);
        bars.saveBillSettlement(new BarBillSettlementRecord("settle_bba_" + suffix, bill, "pos", null, "POS-" + suffix,
                30000L, merchant, Instant.now()));
        call(cashier, bill + "/anomaly", "{\"reason\":\"客人称整单记错卡座\",\"idempotency_key\":\"rb_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));
        call(supervisor, bill + "/anomaly/resolve",
                "{\"resolution\":\"voided_reversed\",\"reason\":\"整单录错卡座，作废后在正确卡座重开\",\"idempotency_key\":\"vb_%s\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_status").value("void"))
                .andExpect(jsonPath("$.data.settlement_status").value("void"));
        assertThat(jdbc.queryForObject("SELECT reversed FROM bar_bill_settlements WHERE bill_id=?", Boolean.class, bill)).isTrue();
        call(cashier, bill + "/settle", "{\"payment_id\":\"pay3_%s\",\"payment_channel\":\"aggregate_pay\",\"idempotency_key\":\"st3_%s\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_VOIDED"));
        call(cashier, bill + "/anomaly", "{\"reason\":\"再登记\",\"idempotency_key\":\"rc_%s\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_VOIDED"));
    }

    /** 重复登记被拒并留失败痕；同一幂等键重放返回第一次结果、成功只记一次；处置方式只认两种；部分支付账单恢复后回到部分支付。 */
    @Test
    void repeatedRegistrationRejectedReplayIdempotentAndRestoreReturnsPriorState() throws Exception {
        String bill = bill("partial_paid", "partial_settled", 10000L);
        String raise = "{\"reason\":\"客人对低消补差有异议\",\"idempotency_key\":\"rp_%s\"}";
        call(cashier, bill + "/anomaly", raise).andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));
        call(cashier, bill + "/anomaly", raise).andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));
        call(cashier, bill + "/anomaly", "{\"reason\":\"再次登记\",\"idempotency_key\":\"rp2_%s\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_ALREADY_ABNORMAL"));
        assertThat(successes("bar.bill_anomaly_raised", bill)).isEqualTo(1);
        assertThat(failures("bar.bill_anomaly_raise_failed", "rp2_" + suffix)).isEqualTo(1);

        call(supervisor, bill + "/anomaly/resolve", "{\"resolution\":\"ignored\",\"reason\":\"不处理\",\"idempotency_key\":\"rx_%s\"}")
                .andExpect(status().isBadRequest());
        call(supervisor, bill + "/anomaly/resolve", "{\"resolution\":\"restored\",\"reason\":\"补差口径已向客人解释\",\"idempotency_key\":\"rr_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("partial_paid"))
                .andExpect(jsonPath("$.data.paid_amount_cent").value(10000));
    }

    /** ENUM L221「异常单据未处置完毕，所属日结不得确认锁定」：结账落在该营业日的账单处于异常时日结锁定被拒；主管处置后可锁。 */
    @Test
    void lockDailyCloseRefusedWhileBarBillAbnormal() throws Exception {
        LocalDate day = LocalDate.parse("2014-06-12");
        Instant at = businessDays.startInstant(day).plusSeconds(3600);
        String bill = settledBillAt(at);
        call(cashier, bill + "/anomaly", "{\"reason\":\"客人称多收一瓶\",\"idempotency_key\":\"rd_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));

        TestSecurityActors.asPlatformAdmin();
        assertThatThrownBy(() -> finance.createDailyClose(dailyClose(day)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DAY_CLOSE_ANOMALY_PENDING"));
        SecurityContextHolder.clearContext();
        call(supervisor, bill + "/anomaly/resolve", "{\"resolution\":\"restored\",\"reason\":\"核实无误\",\"idempotency_key\":\"rdr_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("paid"));
        TestSecurityActors.asPlatformAdmin();
        assertThat(finance.createDailyClose(dailyClose(day)).status()).isEqualTo("locked");
    }

    /** 与反结同一做法：结账营业日已锁定日结的，「作废并冲正」被拒（须走补录调整流程），账单仍在异常，「更正后恢复」不受影响。 */
    @Test
    void voidReverseRefusedAfterSettleDayLocked() throws Exception {
        LocalDate day = LocalDate.parse("2014-06-13");
        String bill = settledBillAt(Instant.parse("2014-06-13T12:00:00Z"));
        TestSecurityActors.asPlatformAdmin();
        assertThat(finance.createDailyClose(dailyClose(day)).status()).isEqualTo("locked");
        SecurityContextHolder.clearContext();

        call(cashier, bill + "/anomaly", "{\"reason\":\"次日客人来电称记错卡座\",\"idempotency_key\":\"rl_%s\"}")
                .andExpect(status().isOk());
        call(supervisor, bill + "/anomaly/resolve", "{\"resolution\":\"voided_reversed\",\"reason\":\"作废重开\",\"idempotency_key\":\"rlv_%s\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_LOCKED"));
        assertThat(jdbc.queryForObject("SELECT payment_status FROM bar_bills WHERE id=?", String.class, bill)).isEqualTo("abnormal");
        assertThat(jdbc.queryForObject("SELECT reversed FROM bar_bill_settlements WHERE bill_id=?", Boolean.class, bill)).isFalse();
        call(supervisor, bill + "/anomaly/resolve", "{\"resolution\":\"restored\",\"reason\":\"走补录调整\",\"idempotency_key\":\"rlr_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("paid"));
    }

    private String settledBillAt(Instant at) {
        String bill = bill("paid", "settled", 30000L);
        jdbc.update("UPDATE bar_bills SET created_at=? WHERE id=?", Timestamp.from(at), bill);
        bars.saveBillSettlement(new BarBillSettlementRecord("settle_bba_" + bill, bill, "pos", null, "POS-" + bill,
                30000L, merchant, at));
        return bill;
    }

    private DailyCloseRequest dailyClose(LocalDate day) {
        return new DailyCloseRequest("bba-close-" + suffix + "-" + seq.incrementAndGet(), "日结", List.of(), day, "merchant",
                merchant, false);
    }

    private ResultActions call(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(BILLS + path).with(who).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("%s", suffix)));
    }

    private String bill(String paymentStatus, String settlementStatus, long paidCent) {
        int n = seq.incrementAndGet();
        String bar = "bba_bar_" + suffix;
        String table = "bba_tbl_" + suffix + "_" + n;
        String order = "bba_ord_" + suffix + "_" + n;
        String bill = "bba_bill_" + suffix + "_" + n;
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(table, bar, merchant, "T-" + suffix + "-" + n, "争议回归卡座", "card_seat",
                4, 1, 0L, "CNY", "active", now, now));
        bars.saveOrder(new BarOrderRecord(order, bar, merchant, table, null, null, "staff", "billed", 30000L, "CNY", "web",
                null, null, now, now));
        bars.saveBill(new BarBillRecord(bill, "BILL-BBA-" + suffix + "-" + n, order, bar, merchant, table, null,
                30000L, 0L, 0L, 30000L, paidCent, "CNY", paymentStatus, settlementStatus, null, null, now, now));
        return bill;
    }

    private RequestPostProcessor staff(String tag, String... permissions) {
        String userId = "bba_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'merchant_staff', NULL, ?, 'active', now())", userId, userId, merchant);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '账单异常回归', 'merchant', 'active', now())", role, role);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "merchant_staff", null, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }

    private int successes(String event, String businessId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name=? AND business_id=?", Integer.class,
                event, businessId);
    }

    private int failures(String event, String idempotencyKey) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name=? AND idempotency_key=?", Integer.class,
                event, idempotencyKey);
    }
}
