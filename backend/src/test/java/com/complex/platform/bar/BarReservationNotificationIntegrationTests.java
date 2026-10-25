package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.finance.dto.RefundExecutionRequest;
import com.complex.platform.finance.dto.RefundResultConfirmRequest;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * OBS-13-03 订台相关短信（真库，不包测试事务：短信在业务事务提交后才发，须让业务真正提交）。依据：BSM 第 1596 行「给客人：……
 * 订台成功确认（员工代订的同时向客人知会代订人与订台内容）、最晚到店时间前提醒（提前量业务参数，按项目可配，出厂默认 30 分钟，须同时配置上下限）、
 * 超时释放台位通知、定金没收通知、定金原路退回通知、订台取消确认」；BSM 第 1062 行「退款处理中的记录继续关联原付款，只有实际退款成功才记为已退」；
 * DEC-R8-97-20。短信供应商未接入，只断言触发与留痕（notification_send_log 一行，request_id＝barres:事件:订台号），不断言送达。
 * 请求一律按前端真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class BarReservationNotificationIntegrationTests {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jz1sAAAAASUVORK5CYII=");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private BarRepository bars;
    @Autowired private BarReservationTimeoutJob timeoutJob;
    @Autowired private BarArrivalReminderService arrivalReminders;
    @Autowired private BarDepositSettlementService depositSettlements;
    @Autowired private BarReceivableLedgerService barLedger;
    @Autowired private FinanceService finance;
    @Autowired private AccessRepository access;
    @Autowired private EvidenceEventService evidence;
    @Autowired private PiiCipher piiCipher;
    @MockitoBean private PaymentGatewayRouter gateway;

    private CustomerTicketPaymentFixture f;
    private String barId;
    private String otherToken;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        f.stubGateway(gateway);
        // 酒吧即商户，挂在本综合体项目下（订台项目按 bar_id 取 merchants.project_id）。
        barId = "bar_sms_" + f.suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '订台短信酒吧', 'bar', 'active', now(), now(), ?)", barId, f.projectId);
        // 定金 200 元；订台时间前 30 分钟以上取消免没收。
        jdbc.update("INSERT INTO bar_deposit_rules (id, bar_id, scope_type, scope_id, mode, fixed_amount_cent, cancel_free_before_minutes,"
                + " enabled, created_at, updated_at) VALUES (?, ?, 'bar', ?, 'fixed', 20000, 30, TRUE, now(), now())",
                "bar_sms_rule_" + f.suffix, barId, barId);
        Instant now = Instant.now();
        String otherId = "cus_sms_other_" + f.suffix;
        otherToken = "tok_sms_other_" + f.suffix;
        String otherPhone = "136" + String.format("%08d", Math.floorMod(f.suffix.hashCode() + 11, 100_000_000));
        customers.saveCustomer(new CustomerRecord(otherId, otherPhone, "136****" + otherPhone.substring(7), "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_sms_other_" + f.suffix, otherId, otherToken, "miniapp", now,
                now.plusSeconds(3600), "active"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void staffBookingConfirmsToEnteredPhoneWithBookedByAndReplayDoesNotResend() throws Exception {
        String table = table("staff");
        String phone = phone(1);
        String formatted = phone.substring(0, 3) + " " + phone.substring(3, 7) + "-" + phone.substring(7);
        Instant latest = Instant.now().plusSeconds(7200);
        String reservation = read(staffReserve("staff", table, formatted, latest).andExpect(status().isOk()));
        // 幂等重放同一请求：不重复发。
        staffReserve("staff", table, formatted, latest).andExpect(status().isOk());

        assertThat(sends(reservation, "confirmed")).isEqualTo(1);
        Map<String, Object> log = jdbc.queryForMap("SELECT domain, recipient, params->>'booked_by' AS booked_by,"
                + " params->>'table_name' AS table_name, params->>'guest_count' AS guest_count FROM notification_send_log"
                + " WHERE request_id = ?", "barres:confirmed:" + reservation);
        assertThat(log).containsEntry("domain", "bar_reservation")
                .containsEntry("booked_by", "王经理").containsEntry("table_name", "卡座").containsEntry("guest_count", "2");
        // PGM L876「客人和员工的手机号……一律加密存放」：短信留痕的收件人同样落密文，读出解密即录入的手机号。
        assertThat((String) log.get("recipient")).startsWith("enc:v1:");
        assertThat(piiCipher.decrypt((String) log.get("recipient"))).isEqualTo(phone);
    }

    @Test
    void customerDepositHoldConfirmsToMemberPhone() throws Exception {
        String reservation = reserve(f.token, "self", table("self"), Instant.now().plusSeconds(7200));
        assertThat(sends(reservation, "confirmed")).as("未付定金、未占台不发订台成功").isZero();
        payDeposit(f.token, reservation).andExpect(status().isOk());
        assertThat(sends(reservation, "confirmed")).isEqualTo(1);
        String recipient = jdbc.queryForObject("SELECT recipient FROM notification_send_log WHERE request_id = ?", String.class,
                "barres:confirmed:" + reservation);
        assertThat(recipient).startsWith("enc:v1:");
        assertThat(piiCipher.decrypt(recipient)).isEqualTo(f.phone);
    }

    @Test
    void lostDepositRaceCancelsThenRefundSmsOnlyAfterRefundSucceeds() throws Exception {
        String table = table("race");
        String winner = reserve(f.token, "win", table, Instant.now().plusSeconds(7200));
        String loser = reserve(otherToken, "lose", table, Instant.now().plusSeconds(7200));
        String loserIntent = CustomerTicketPaymentFixture.read(startDeposit(otherToken, loser).andExpect(status().isOk()),
                "$.data.payment_intent_id");
        payDeposit(f.token, winner).andExpect(status().isOk());
        f.markPaid(loserIntent);
        confirmDeposit(otherToken, loser, loserIntent).andExpect(status().isConflict());

        assertThat(sends(loser, "cancelled")).isEqualTo(1);
        assertThat(sends(loser, "deposit_refunded")).as("原路待退尚未退成").isZero();
        refundSucceeds(loser, 20_000L);
        assertThat(deposit(loser)).containsEntry("deposit_status", "settled").containsEntry("deposit_settlement", "refunded");
        assertThat(sends(loser, "deposit_refunded")).isEqualTo(1);
    }

    @Test
    void cancelWithinFreeWindowRegistersRefundAndRefundedOnlyAfterSuccess() throws Exception {
        String reservation = reserve(f.token, "free", table("free"), Instant.now().plusSeconds(7200));
        payDeposit(f.token, reservation).andExpect(status().isOk());
        cancel(reservation, false).andExpect(status().isOk());

        // BSM 第 1062 行「退款处理中的记录继续关联原付款，只有实际退款成功才记为已退」：取消照常、定金保持已支付（退款处理中）。
        assertThat(deposit(reservation)).containsEntry("status", "cancelled").containsEntry("deposit_status", "paid");
        assertThat(jdbc.queryForMap("SELECT id, status, refund_amount_cent, refund_channel FROM finance_refunds"
                + " WHERE source_type = 'bar_deposit_rejection' AND target_id = ?", reservation))
                .containsEntry("status", "requested").containsEntry("refund_amount_cent", 20_000L)
                .containsEntry("refund_channel", "wechat")
                .hasEntrySatisfying("id", id -> assertThat((String) id).startsWith("refund_rejected_"));
        assertThat(sends(reservation, "cancelled")).isEqualTo(1);
        assertThat(sends(reservation, "deposit_refunded")).isZero();

        refundSucceeds(reservation, 20_000L);
        assertThat(deposit(reservation)).containsEntry("deposit_status", "settled").containsEntry("deposit_settlement", "refunded");
        assertThat(sends(reservation, "deposit_refunded")).isEqualTo(1);
    }

    @Test
    void noShowCancelSendsCancelledAndForfeited() throws Exception {
        String reservation = reserve(f.token, "noshow", table("noshow"), Instant.now().plusSeconds(7200));
        payDeposit(f.token, reservation).andExpect(status().isOk());
        cancel(reservation, true).andExpect(status().isOk());
        assertThat(sends(reservation, "cancelled")).isEqualTo(1);
        assertThat(sends(reservation, "deposit_forfeited")).isEqualTo(1);
    }

    @Test
    void timeoutReleasesHeldPaidButNotUnheldUnpaid() throws Exception {
        Instant past = Instant.now().minusSeconds(600);
        String paid = reserve(f.token, "late-paid", table("late-paid"), past);
        payDeposit(f.token, paid).andExpect(status().isOk());
        String unpaid = reserve(otherToken, "late-unpaid", table("late-unpaid"), past);

        timeoutJob.releaseOverdue();

        assertThat(sends(paid, "timeout_released")).isEqualTo(1);
        assertThat(sends(paid, "deposit_forfeited")).isEqualTo(1);
        // 未付定金、从未占台的自助订台超时作废：没有台位被释放，不发。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_send_log WHERE request_id LIKE ?", Integer.class,
                "barres:%:" + unpaid)).isZero();
    }

    @Test
    void closingBelowMinimumSpendForfeits() throws Exception {
        String table = table("close");
        String reservation = reserve(f.token, "close", table, Instant.now().plusSeconds(7200));
        payDeposit(f.token, reservation).andExpect(status().isOk());
        openSession("close", table, reservation).andExpect(status().isOk());
        String session = jdbc.queryForObject("SELECT id FROM bar_table_sessions WHERE reservation_id = ?", String.class, reservation);

        depositSettlements.forfeitAtClose(session);

        assertThat(sends(reservation, "deposit_forfeited")).isEqualTo(1);
    }

    @Test
    void thresholdRefundPayableSettledSendsRefunded() throws Exception {
        String reservation = reserve(f.token, "threshold", table("threshold"), Instant.now().plusSeconds(7200));
        payDeposit(f.token, reservation).andExpect(status().isOk());
        String payer = "usr_sms_pay_" + f.suffix;
        access.createUser(new AccessUser(payer, payer, "platform_admin", "org_1", null, "active", Instant.now()));
        access.saveUserPermissionOverrides(payer, List.of("finance.payment.register"), AccessRepository.Effect.GRANT,
                "定金退款实付测试授权", payer);
        TestSecurityActors.asPlatformAdmin(payer);
        String itemId = barLedger.createDepositRefundPayable(bars.findReservationById(reservation).orElseThrow());
        assertThat(sends(reservation, "deposit_refunded")).as("只挂退款应付不发").isZero();

        finance.registerArApPayment(itemId, new ArApPaymentRequest("sms-pay-" + f.suffix, "原路退回定金", List.of(), 20_000L,
                "aggregate_pay", Instant.now(), "flow_sms_" + f.suffix, null));

        assertThat(deposit(reservation)).containsEntry("deposit_settlement", "refunded_on_threshold");
        assertThat(sends(reservation, "deposit_refunded")).isEqualTo(1);
    }

    @Test
    void arrivalReminderUsesFactory30MinutesThenProjectPolicyAndOnlyHeld() throws Exception {
        Instant now = Instant.now();
        String near = read(staffReserve("near", table("near"), phone(2), now.plusSeconds(20 * 60)).andExpect(status().isOk()));
        String far = read(staffReserve("far", table("far"), phone(3), now.plusSeconds(40 * 60)).andExpect(status().isOk()));
        String unheld = reserve(otherToken, "unheld", table("unheld"), now.plusSeconds(20 * 60));

        arrivalReminders.remindApproaching(Instant.now());
        arrivalReminders.remindApproaching(Instant.now());

        assertThat(sends(near, "arrival_reminder")).as("出厂 30 分钟内，只发一次").isEqualTo(1);
        assertThat(sends(far, "arrival_reminder")).isZero();
        assertThat(sends(unheld, "arrival_reminder")).as("未付定金未占台不提醒").isZero();

        savePolicy(f.projectId, platformAdmin(), Map.of("remind_minutes", 60, "min_minutes", 10, "max_minutes", 120))
                .andExpect(status().isOk());
        arrivalReminders.remindApproaching(Instant.now());
        assertThat(sends(far, "arrival_reminder")).as("本项目改为 60 分钟").isEqualTo(1);
    }

    @Test
    void arrivalReminderPolicyRequiresBoundsAndProjectScope() throws Exception {
        mockMvc.perform(get("/api/v1/bar/arrival-reminder-policies/" + f.projectId).with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value("__global__"))
                .andExpect(jsonPath("$.data.remind_minutes").value(30));
        savePolicy(f.projectId, platformAdmin(), Map.of("remind_minutes", 45))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        savePolicy(f.projectId, platformAdmin(), Map.of("remind_minutes", 200, "min_minutes", 10, "max_minutes", 120))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        savePolicy(f.projectId, merchantStaff(), Map.of("remind_minutes", 45, "min_minutes", 10, "max_minutes", 120))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        savePolicy(f.projectId, projectStaff("prj_other_" + f.suffix), Map.of("remind_minutes", 45, "min_minutes", 10, "max_minutes", 120))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        savePolicy(f.projectId, platformAdmin(), Map.of("remind_minutes", 45, "min_minutes", 10, "max_minutes", 120))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.remind_minutes").value(45));
        mockMvc.perform(get("/api/v1/bar/arrival-reminder-policies/" + f.projectId).with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(f.projectId))
                .andExpect(jsonPath("$.data.remind_minutes").value(45))
                .andExpect(jsonPath("$.data.min_minutes").value(10))
                .andExpect(jsonPath("$.data.max_minutes").value(120));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.arrival_reminder_policy_updated'"
                + " AND business_id = ?", Integer.class, f.projectId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.arrival_reminder_policy_update_failed'"
                + " AND business_id = ?", Integer.class, f.projectId)).isEqualTo(4);
    }

    // ---------- helpers ----------

    private int sends(String reservationId, String event) {
        return jdbc.queryForObject("SELECT count(*) FROM notification_send_log WHERE domain = 'bar_reservation' AND event_key = ?"
                + " AND request_id = ?", Integer.class, event, "barres:" + event + ":" + reservationId);
    }

    private Map<String, Object> deposit(String reservationId) {
        return jdbc.queryForMap("SELECT status, deposit_status, deposit_settlement FROM bar_reservations WHERE id = ?", reservationId);
    }

    private String phone(int n) {
        return "135" + String.format("%08d", Math.floorMod(f.suffix.hashCode() * 7 + n, 100_000_000));
    }

    private String table(String tag) {
        String id = "table_sms_" + tag + "_" + f.suffix;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())",
                id, barId, barId, "S-" + tag.toUpperCase() + "-" + f.suffix);
        return id;
    }

    private Map<String, Object> reservationBody(String tag, String table, Instant latestArrival) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "sms-" + tag + "-" + f.suffix);
        body.put("bar_id", barId);
        body.put("table_id", table);
        body.put("table_type", "card_seat");
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", latestArrival.toString());
        body.put("guest_count", 2);
        return body;
    }

    private String reserve(String token, String tag, String table, Instant latestArrival) throws Exception {
        return read(customerPost(token, "/api/v1/customer/bar-reservations", reservationBody(tag, table, latestArrival))
                .andExpect(status().isOk()));
    }

    private ResultActions payDeposit(String token, String reservationId) throws Exception {
        String intentId = CustomerTicketPaymentFixture.read(startDeposit(token, reservationId).andExpect(status().isOk()),
                "$.data.payment_intent_id");
        f.markPaid(intentId);
        return confirmDeposit(token, reservationId, intentId);
    }

    private ResultActions startDeposit(String token, String reservationId) throws Exception {
        return customerPost(token, "/api/v1/customer/bar-reservations/" + reservationId + "/deposit-payments",
                Map.of("idempotency_key", "dep-" + UUID.randomUUID(), "channel", "wechat"));
    }

    private ResultActions confirmDeposit(String token, String reservationId, String intentId) throws Exception {
        return customerPost(token, "/api/v1/customer/bar-reservations/" + reservationId + "/deposit-payments/" + intentId + "/confirm",
                Map.of("idempotency_key", "dep-confirm-" + UUID.randomUUID()));
    }

    /** 后台代订（BarTableSessionsPage 新建订台表单的真实请求形状）。 */
    private ResultActions staffReserve(String tag, String table, String customerPhone, Instant latestArrival) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "sms-staff-" + tag + "-" + f.suffix);
        body.put("bar_id", barId);
        body.put("table_id", table);
        body.put("guest_count", 2);
        body.put("reservation_time", latestArrival.minusSeconds(600).toString());
        body.put("latest_arrival_at", latestArrival.toString());
        body.put("customer_phone", customerPhone);
        return mockMvc.perform(post("/api/v1/bar/reservations").with(platformAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 后台取消 / 爽约（BarTableSessionsPage 取消订台的真实请求形状）。 */
    private ResultActions cancel(String reservationId, boolean noShow) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/reservations/" + reservationId + "/cancel").with(platformAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "sms-cancel-" + UUID.randomUUID(), "no_show", noShow))));
    }

    private ResultActions openSession(String tag, String table, String reservationId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "sms-open-" + tag + "-" + f.suffix);
        body.put("bar_id", barId);
        body.put("table_id", table);
        body.put("business_date", java.time.LocalDate.now().toString());
        body.put("guest_count", 2);
        body.put("reservation_id", reservationId);
        return mockMvc.perform(post("/api/v1/bar/table-sessions").with(platformAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 后台 CustomerFacingSettingsSections 到店前提醒保存（adminClient 转蛇形键并带幂等键）。 */
    private ResultActions savePolicy(String projectId, RequestPostProcessor actor, Map<String, Object> values) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>(values);
        body.put("idempotency_key", "sms-policy-" + UUID.randomUUID());
        return mockMvc.perform(put("/api/v1/bar/arrival-reminder-policies/" + projectId).with(actor)
                .header("Idempotency-Key", String.valueOf(body.get("idempotency_key")))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /**
     * 该酒吧商户财务按原路办理订台定金的原路待退（定金直进酒吧商户，退款由本商户办）：甲登记执行凭证、乙独立核实成功
     * （同 FinanceProjectScopeRefundJdbcIntegrationTests 口径；商户账号办财务动作须有生效合同准入）。
     */
    private void refundSucceeds(String reservationId, long amountCent) {
        String refundId = jdbc.queryForObject("SELECT id FROM finance_refunds WHERE source_type = 'bar_deposit_rejection'"
                + " AND target_id = ?", String.class, reservationId);
        String role = "sms_fin_role_" + f.suffix;
        if (jdbc.queryForObject("SELECT count(*) FROM roles WHERE id = ?", Integer.class, role) == 0) {
            jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                    + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,'subj_sms','subj_sms',current_date-1,"
                    + "current_date+365,'effective','[\"finance\"]'::jsonb,now(),now())", "sms_c_" + f.suffix, barId);
            access.createRole(new RoleRecord(role, "酒吧财务测试角色" + System.nanoTime(), null, "project", "active", Instant.now(),
                    List.of("finance.ar_ap.view", "finance.payment.register", "finance.refund.create", "evidence.attachment.create"),
                    null, false, List.of()));
        }
        String financeA = financeUser(role);
        String financeB = financeUser(role);
        String reference = "RF-" + UUID.randomUUID().toString().substring(0, 8);
        actAs(financeA);
        finance.recordFinanceRefundExecution(refundId, new RefundExecutionRequest(UUID.randomUUID().toString(), "沿原收款路径执行",
                List.of(proof(refundId)), reference, null));
        actAs(financeB);
        finance.confirmFinanceRefundResult(refundId, new RefundResultConfirmRequest(UUID.randomUUID().toString(), "独立核对渠道结果",
                List.of(proof(refundId)), "success", amountCent, reference, financeA, null));
        SecurityContextHolder.clearContext();
    }

    private String financeUser(String role) {
        String id = "sms_fin_" + UUID.randomUUID();
        access.createUser(new AccessUser(id, id, "staff", f.projectId, barId, "active", Instant.now()));
        access.assignUserRole(id, role);
        jdbc.update("INSERT INTO refund_authority_limits(id,subject_type,subject_id,refund_domain,limit_amount_cent,project_id,status)"
                + " VALUES(?,'staff',?,'finance',1000000,NULL,'active')", "ral_" + id, id);
        return id;
    }

    private void actAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", f.projectId, barId, "active"), "test", List.of()));
    }

    private String proof(String refundId) {
        return evidence.uploadAttachment(new MockMultipartFile("file", "原路退款凭证.png", "image/png", PNG),
                "finance_refund", refundId, "payment_receipt").id();
    }

    private ResultActions customerPost(String token, String path, Map<String, Object> body) throws Exception {
        Object key = body.get("idempotency_key");
        return mockMvc.perform(post(path).header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                .header("Idempotency-Key", String.valueOf(key))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private static String read(ResultActions result) throws Exception {
        return CustomerTicketPaymentFixture.read(result, "$.data.id");
    }

    /** 平台员工（真实授权链；员工档案姓名＝王经理，供员工代订知会代订人）。 */
    private RequestPostProcessor platformAdmin() {
        String userId = "usr_sms_admin_" + f.suffix;
        if (jdbc.queryForObject("SELECT count(*) FROM access_users WHERE id = ?", Integer.class, userId) == 0) {
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                    + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", userId, userId);
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                    + " VALUES (?, ?, '订台短信回归', 'platform', 'active', now())", role, "订台短信测试角色" + System.nanoTime());
            for (String code : List.of("bar.reservation.create", "bar.reservation.manage", "bar.reservation.view",
                    "bar.table_session.create")) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
            }
            jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
            jdbc.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, phone, registration_status, status,"
                    + " created_at, updated_at) VALUES (?, ?, NULL, ?, ?, ?, 'approved', 'active', now(), now())",
                    "sp_sms_" + f.suffix, f.projectId, userId, piiCipher.encrypt("王经理"), piiCipher.encrypt(phone(9)));
        }
        return actor(new AccessActor(userId, userId, "platform_admin", null, null, "active"));
    }

    /** 该酒吧商户的员工（商户范围主体）。 */
    private RequestPostProcessor merchantStaff() {
        return staffWith("usr_sms_mer_" + f.suffix, f.projectId, barId, "merchant");
    }

    /** 他项目的项目员工。 */
    private RequestPostProcessor projectStaff(String project) {
        return staffWith("usr_sms_prj_" + f.suffix, project, null, "project");
    }

    private RequestPostProcessor staffWith(String userId, String project, String merchant, String dataScope) {
        if (jdbc.queryForObject("SELECT count(*) FROM access_users WHERE id = ?", Integer.class, userId) == 0) {
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                    + " VALUES (?, ?, 'staff', ?, ?, 'active', now())", userId, userId, project, merchant);
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                    + " VALUES (?, ?, '订台提醒参数回归', ?, 'active', now())", role, "订台提醒参数测试角色" + System.nanoTime(), dataScope);
            for (String code : List.of("bar.reservation.manage", "bar.reservation.view")) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
            }
            jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        }
        return actor(new AccessActor(userId, userId, "staff", project, merchant, "active"));
    }

    private static RequestPostProcessor actor(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }
}
