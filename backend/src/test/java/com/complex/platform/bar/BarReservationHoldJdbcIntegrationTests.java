package com.complex.platform.bar;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03-R19 订台排他预留与最晚到店时间（真库）＋操作员裁决第十三批第 7 条员工代订核对受限。依据：
 * ENUM 第 100 行「客人自助订台支付定金成功、或员工代订提交成功时，该台位即进入已预留。一个台位同一时刻只能存在一笔有效预留（排他锁，不是展示标记）；
 * 台位未被占用——既无有效预留也未开台——时即可再次接受预订……冲突时先支付定金者得；员工代订不得覆盖已付定金的自助订单；两笔员工代订相撞先提交者得。
 * 超时自动释放后台位回到空闲……已预留只能经取消订台、超时释放、到店开台三条路径退出」；BSM 第 1062 行「无论是否支付定金，每一笔订台都必须填写最晚到店时间；
 * 超过最晚到店时间未到店的，系统自动释放该台位——已付定金的按爽约规则没收定金，未付定金的直接作废」；裁决第 7 条「员工代订必须录入客人手机号；
 * 查到会员且受限就拒绝，查不到会员的照常代订」。客人与后台请求一律按前端真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarReservationHoldJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private BarReservationTimeoutJob timeoutJob;
    @Autowired private BarRepository bars;
    @MockitoBean private PaymentGatewayRouter gateway;

    private CustomerTicketPaymentFixture f;
    private String barId;
    private String tableId;
    private String otherToken;
    private String otherPhone;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        f.stubGateway(gateway);
        barId = "bar_hold_" + f.suffix;
        tableId = table("hold");
        // 定金规则：本酒吧固定 200 元（客人自助订台需付定金，BSM 第 860 行）。
        jdbc.update("INSERT INTO bar_deposit_rules (id, bar_id, scope_type, scope_id, mode, fixed_amount_cent, cancel_free_before_minutes,"
                + " enabled, created_at, updated_at) VALUES (?, ?, 'bar', ?, 'fixed', 20000, 0, TRUE, now(), now())",
                "bar_dep_rule_" + f.suffix, barId, barId);
        Instant now = Instant.now();
        String otherId = "cus_hold_other_" + f.suffix;
        otherToken = "tok_hold_other_" + f.suffix;
        otherPhone = "136" + String.format("%08d", Math.floorMod(f.suffix.hashCode() + 7, 100_000_000));
        customers.saveCustomer(new CustomerRecord(otherId, otherPhone, "136****" + otherPhone.substring(7), "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_hold_other_" + f.suffix, otherId, otherToken, "miniapp", now,
                now.plusSeconds(3600), "active"));
    }

    /** 第十八批 AG 段 259-R4（OpenAPI 与代码一致：CustomerBarReservationRequest 的 table_id 进 required）：客人订台不带 table_id → 400「桌台未填写」。 */
    @Test
    void customerReservationWithoutTableIdRejected() throws Exception {
        Map<String, Object> body = reservationBody("no-table");
        body.remove("table_id");
        customerPost(f.token, "/api/v1/customer/bar-reservations", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("桌台未填写"));
    }

    /** 第十八批 AG 段 259-R6（OpenAPI 与代码一致：createBarTableSession 的 table_id 进 required）：后台开台不带 table_id → 400「桌台未填写」，不开台。 */
    @Test
    void openSessionWithoutTableIdRejected() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "open-no-table-" + f.suffix);
        body.put("bar_id", barId);
        body.put("business_date", java.time.LocalDate.now().toString());
        body.put("guest_count", 2);
        mockMvc.perform(post("/api/v1/bar/table-sessions").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("桌台未填写"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_table_sessions WHERE table_id = ?", Integer.class, tableId)).isZero();
    }

    @Test
    void customerReservationWithoutLatestArrivalRejected() throws Exception {
        Map<String, Object> body = reservationBody("no-latest");
        body.remove("latest_arrival_at");
        customerPost(f.token, "/api/v1/customer/bar-reservations", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE table_id = ?", Integer.class, tableId)).isZero();
    }

    @Test
    void selfReservationUnavailableWithoutDepositRuleStaffBookingUnaffected() throws Exception {
        // BSM 第 1062 行「客人自助订台必须支付定金」：酒吧未配定金规则时客人自助订台不可用，员工代订不受影响。
        jdbc.update("DELETE FROM bar_deposit_rules WHERE bar_id = ?", barId);
        customerPost(f.token, "/api/v1/customer/bar-reservations", reservationBody("no-rule"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SELF_RESERVATION_UNAVAILABLE"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE table_id = ?", Integer.class, tableId)).isZero();
        staffReserve("staff-no-rule", "13900000006").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hold_status").value("held"));
    }

    @Test
    void firstDepositConfirmedHoldsTableLoserRefundedAndCancelled() throws Exception {
        String winner = reserve(f.token, "win");
        String loser = reserve(otherToken, "lose");
        // 两位客人都已拉起付款（台位当时都空着），先到账确认者得。
        String loserIntent = CustomerTicketPaymentFixture.read(startDeposit(otherToken, loser).andExpect(status().isOk()),
                "$.data.payment_intent_id");
        payDeposit(f.token, winner).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deposit_status").value("paid"))
                .andExpect(jsonPath("$.data.hold_status").value("held"));
        f.markPaid(loserIntent);
        confirmDeposit(otherToken, loser, loserIntent).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_ALREADY_RESERVED"));
        assertThat(row(winner)).containsEntry("hold_status", "held").containsEntry("status", "reserved");
        // 败者：订台取消、不占台，已收定金按原付款渠道登记原路待退（退款处理中）。
        assertThat(row(loser)).containsEntry("status", "cancelled").containsEntry("deposit_status", "paid")
                .containsEntry("hold_status", null);
        assertThat(jdbc.queryForMap("SELECT refund_amount_cent, refund_channel, status FROM finance_refunds"
                + " WHERE source_type = 'bar_deposit_rejection' AND target_id = ?", loser))
                .containsEntry("refund_amount_cent", 20_000L).containsEntry("refund_channel", "wechat")
                .containsEntry("status", "requested");
        // 重复确认仍报占台冲突，不重复登记退款。
        confirmDeposit(otherToken, loser, jdbc.queryForObject("SELECT deposit_intent_id FROM bar_reservations WHERE id = ?",
                String.class, loser)).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM finance_refunds WHERE source_type = 'bar_deposit_rejection' AND target_id = ?",
                Integer.class, loser)).isEqualTo(1);
    }

    @Test
    void heldTableRejectsNewBookingAndOptionsMarkReserved() throws Exception {
        String winner = reserve(f.token, "held");
        payDeposit(f.token, winner).andExpect(status().isOk());
        customerPost(otherToken, "/api/v1/customer/bar-reservations", reservationBody("after-held"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_ALREADY_RESERVED"));
        mockMvc.perform(get("/api/v1/customer/bar-reservation-options").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.table_id == '" + tableId + "')].reserved").value(true));
    }

    @Test
    void staffBookingCannotOverridePaidSelfBooking() throws Exception {
        String winner = reserve(f.token, "self");
        payDeposit(f.token, winner).andExpect(status().isOk());
        staffReserve("staff-over", "13900000001")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_ALREADY_RESERVED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE table_id = ? AND hold_status = 'held'",
                Integer.class, tableId)).isEqualTo(1);
    }

    @Test
    void staffBookingHoldsOnSubmitAndSecondStaffBookingLoses() throws Exception {
        staffReserve("staff-first", "13900000002").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hold_status").value("held"));
        staffReserve("staff-second", "13900000003")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_ALREADY_RESERVED"));
        // 客人自助下单前预检：已被占的台位直接拒绝，不让客人付了钱才被退。
        customerPost(f.token, "/api/v1/customer/bar-reservations", reservationBody("after-staff"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_ALREADY_RESERVED"));
    }

    @Test
    void staffBookingRequiresCustomerPhoneAndRejectsRestrictedMember() throws Exception {
        staffReserve("staff-nophone", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        jdbc.update("UPDATE customers SET status = 'suspended', suspended_until = ? WHERE id = ?",
                Timestamp.from(Instant.now().plusSeconds(86_400)), f.customerId);
        staffReserve("staff-restricted", f.phone)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_RESTRICTED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE table_id = ?", Integer.class, tableId)).isZero();
        // 查不到会员的照常代订。
        staffReserve("staff-unknown", "13899990000").andExpect(status().isOk());
    }

    @Test
    void cancelReleasesHoldAndTableCanBeBookedAgain() throws Exception {
        String staffBooking = CustomerTicketPaymentFixture.read(staffReserve("staff-cancel", "13900000004")
                .andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(post("/api/v1/bar/reservations/" + staffBooking + "/cancel").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "cancel-" + f.suffix, "no_show", false))))
                .andExpect(status().isOk());
        assertThat(row(staffBooking)).containsEntry("hold_status", "released");
        customerPost(f.token, "/api/v1/customer/bar-reservations", reservationBody("rebook")).andExpect(status().isOk());
    }

    @Test
    void openedTableRejectsBooking() throws Exception {
        jdbc.update("UPDATE bar_tables SET status = 'opened' WHERE id = ?", tableId);
        staffReserve("staff-opened", "13900000005")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_ALREADY_RESERVED"));
    }

    @Test
    void timeoutJobForfeitsPaidVoidsUnpaidAndReleasesHold() throws Exception {
        String unpaidTable = table("unpaid");
        Instant past = Instant.now().minusSeconds(600);
        String paid = reserve(f.token, "late-paid", tableId, past);
        payDeposit(f.token, paid).andExpect(status().isOk());
        String unpaid = reserve(otherToken, "late-unpaid", unpaidTable, past);
        String notYet = reserve(otherToken, "not-yet", unpaidTable, Instant.now().plusSeconds(3600));

        // 库里可能还有别的用例留下的超时订台，只要求本例两笔都在本轮释放之列。
        assertThat(timeoutJob.releaseOverdue()).isGreaterThanOrEqualTo(2);

        assertThat(row(paid)).containsEntry("status", "cancelled").containsEntry("hold_status", "released")
                .containsEntry("deposit_status", "settled").containsEntry("deposit_settlement", "forfeited");
        // ENUM 第 103 行定金状态「已超时释放」：超时释放时未付定金直接作废。
        assertThat(row(unpaid)).containsEntry("status", "cancelled").containsEntry("deposit_status", "timeout_released");
        assertThat(row(notYet)).containsEntry("status", "reserved");
        // 释放后台位回到空闲，可再次接受预订。
        staffReserve("staff-after-timeout", "13900000006").andExpect(status().isOk());
    }

    @Test
    void openingHeldTableRequiresThatReservationAndArrivedGuestKeepsDeposit() throws Exception {
        String paid = reserve(f.token, "arrive");
        payDeposit(f.token, paid).andExpect(status().isOk());
        // ENUM 第 100 行「排他锁，不是展示标记」：不带占台的那笔订台号，散客开不了这张台。
        openSession("walk-in", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_ALREADY_RESERVED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_table_sessions WHERE table_id = ?", Integer.class, tableId)).isZero();
        // 客人到店，员工按后台开台表单带上订台号开台：占台退出，到了最晚到店时间也不按爽约没收定金。
        openSession("arrived", paid).andExpect(status().isOk());
        assertThat(row(paid)).containsEntry("hold_status", "released").containsEntry("status", "reserved");
        jdbc.update("UPDATE bar_reservations SET latest_arrival_at = now() - interval '1 minute' WHERE id = ?", paid);
        timeoutJob.releaseOverdue();
        assertThat(row(paid)).containsEntry("status", "reserved").containsEntry("deposit_status", "paid");
    }

    @Test
    void unpaidReservationOpenedOnArrivalNotExpiredByTimeoutJob() throws Exception {
        String unpaid = reserve(f.token, "arrive-unpaid");
        openSession("arrived-unpaid", unpaid).andExpect(status().isOk());
        jdbc.update("UPDATE bar_reservations SET latest_arrival_at = now() - interval '1 minute' WHERE id = ?", unpaid);
        timeoutJob.releaseOverdue();
        assertThat(row(unpaid)).containsEntry("status", "reserved").containsEntry("hold_status", "released");
    }

    @Test
    void depositArrivingAfterCancellationRefundedNotRetried() throws Exception {
        String reservation = reserve(f.token, "late-dep");
        String intentId = CustomerTicketPaymentFixture.read(startDeposit(f.token, reservation).andExpect(status().isOk()),
                "$.data.payment_intent_id");
        mockMvc.perform(post("/api/v1/bar/reservations/" + reservation + "/cancel").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "cancel-late-" + f.suffix, "no_show", false))))
                .andExpect(status().isOk());
        f.markPaid(intentId);
        confirmDeposit(f.token, reservation, intentId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_DEPOSIT_NOT_PAYABLE"));
        // 已收定金登记原路待退，付款单已入账，结算作业不再反复重试。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM finance_refunds WHERE source_type = 'bar_deposit_rejection' AND target_id = ?",
                Integer.class, reservation)).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT refund_amount_cent, refund_channel, status FROM finance_refunds"
                + " WHERE source_type = 'bar_deposit_rejection' AND target_id = ?", reservation))
                .containsEntry("refund_amount_cent", 20_000L).containsEntry("refund_channel", "wechat")
                .containsEntry("status", "requested");
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, intentId))
                .isEqualTo("succeeded");
    }

    @Test
    void depositPaymentNotLaunchedWhenTableAlreadyHeld() throws Exception {
        String winner = reserve(f.token, "held-first");
        String late = reserve(otherToken, "held-late");
        payDeposit(f.token, winner).andExpect(status().isOk());
        startDeposit(otherToken, late)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_ALREADY_RESERVED"));
        assertThat(jdbc.queryForObject("SELECT deposit_intent_id FROM bar_reservations WHERE id = ?", String.class, late)).isNull();
    }

    @Test
    void relaunchAfterChannelPaidSettlesOldIntentInsteadOfVoiding() throws Exception {
        String reservation = reserve(f.token, "relaunch");
        String first = CustomerTicketPaymentFixture.read(startDeposit(f.token, reservation).andExpect(status().isOk()),
                "$.data.payment_intent_id");
        f.markPaid(first);
        // 客人已在渠道付款、没回前台又点了一次付款：不作废已付的那笔，按到账处理，不再拉起新的一笔。
        startDeposit(f.token, reservation).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_intent_id").value(first))
                .andExpect(jsonPath("$.data.gateway_status").value("SUCCEEDED"));
        assertThat(row(reservation)).containsEntry("deposit_status", "paid").containsEntry("hold_status", "held");
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, first))
                .isEqualTo("succeeded");
    }

    @Test
    void customerReservationBelongsToTableMerchantAndDepositPaidToIt() throws Exception {
        String merchant = "mer_hold_" + f.suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '订台酒吧', 'bar', 'active', now(), now(), ?)", merchant, f.projectId);
        String merchantTable = table("merchant");
        jdbc.update("UPDATE bar_tables SET merchant_id = ? WHERE id = ?", merchant, merchantTable);
        String reservation = reserve(f.token, "merchant", merchantTable, Instant.now().plusSeconds(7200));
        assertThat(jdbc.queryForObject("SELECT merchant_id FROM bar_reservations WHERE id = ?", String.class, reservation))
                .isEqualTo(merchant);
        // BSM 第 1062 行「定金直接进该卡座所属的酒吧商户」：付款单的收款主体＝该酒吧商户（商户直收）。
        String intentId = CustomerTicketPaymentFixture.read(startDeposit(f.token, reservation).andExpect(status().isOk()),
                "$.data.payment_intent_id");
        assertThat(f.initiatedFor(intentId).payeeMerchantId()).isEqualTo(merchant);
        assertThat(f.initiatedFor(intentId).fundMode()).isEqualTo("merchant_prepaid");
        f.markPaid(intentId);
        confirmDeposit(f.token, reservation, intentId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deposit_status").value("paid"));
        // 裁决第十三批第 4 条：该酒吧的员工在后台确认订台后签发邀请函。
        mockMvc.perform(post("/api/v1/bar/reservations/" + reservation + "/invitation").with(merchantStaff(merchant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "inv-merchant-" + f.suffix))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));
    }

    @Test
    void staffBookingNormalizesFormattedPhoneBeforeRestrictionCheck() throws Exception {
        jdbc.update("UPDATE customers SET status = 'suspended', suspended_until = ? WHERE id = ?",
                Timestamp.from(Instant.now().plusSeconds(86_400)), f.customerId);
        String formatted = f.phone.substring(0, 3) + " " + f.phone.substring(3, 7) + "-" + f.phone.substring(7);
        staffReserve("staff-formatted", formatted)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_RESTRICTED"));
        staffReserve("staff-bad-phone", "12345")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void staffBookingKeepsEnteredPhoneEncryptedForReservationSms() throws Exception {
        // OBS-13-03-R1（BSM 第 1596 行「订台成功确认（员工代订的同时向客人知会代订人与订台内容）」）：员工代订没有会员编号，
        // 录入的客人手机号是订台短信唯一收件人来源——规范化后加密留存，库内不落明文。
        String phone = "135" + String.format("%08d", Math.floorMod(f.suffix.hashCode() + 3, 100_000_000));
        String formatted = phone.substring(0, 3) + " " + phone.substring(3, 7) + "-" + phone.substring(7);
        String reservation = CustomerTicketPaymentFixture.read(staffReserve("staff-phone", formatted)
                .andExpect(status().isOk()), "$.data.id");
        assertThat(bars.findReservationCustomerPhone(reservation)).contains(phone);
        assertThat(jdbc.queryForObject("SELECT customer_phone FROM bar_reservations WHERE id = ?", String.class, reservation))
                .startsWith("enc:v1:").isNotEqualTo(phone);
    }

    @Test
    void staffReservationIsDepositWaivedEvenWhenDepositRuleConfigured() throws Exception {
        // 第十八批 C 段第 15 条：BSM 第 1062 行「员工代客订台不收定金……员工代客订台不收定金、没有支付动作的，支付时间就空着」；
        // ENUM 第 103 行订台定金状态「免收（员工代订）」。本酒吧已配固定 200 元定金规则（seed），员工代订仍免收。
        String reservation = CustomerTicketPaymentFixture.read(staffReserve("staff-waived", "13900000007")
                .andExpect(status().isOk()), "$.data.id");
        assertThat(jdbc.queryForMap("SELECT deposit_status, deposit_amount_cent, deposit_intent_id, deposit_paid_at, hold_status"
                + " FROM bar_reservations WHERE id = ?", reservation))
                .containsEntry("deposit_status", "staff_waived").containsEntry("deposit_amount_cent", 0L)
                .containsEntry("deposit_intent_id", null).containsEntry("deposit_paid_at", null)
                .containsEntry("hold_status", "held");
    }

    // ---------- helpers ----------

    private String table(String tag) {
        String id = "table_hold_" + tag + "_" + f.suffix;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())",
                id, barId, "T-" + tag.toUpperCase() + "-" + f.suffix);
        return id;
    }

    private Map<String, Object> reservationBody(String tag) {
        return reservationBody(tag, tableId, Instant.now().plusSeconds(7200));
    }

    private Map<String, Object> reservationBody(String tag, String table, Instant latestArrival) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "hold-" + tag + "-" + f.suffix);
        body.put("bar_id", barId);
        body.put("table_id", table);
        body.put("table_type", "card_seat");
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", latestArrival.toString());
        body.put("guest_count", 2);
        return body;
    }

    private String reserve(String token, String tag) throws Exception {
        return reserve(token, tag, tableId, Instant.now().plusSeconds(7200));
    }

    private String reserve(String token, String tag, String table, Instant latestArrival) throws Exception {
        return CustomerTicketPaymentFixture.read(customerPost(token, "/api/v1/customer/bar-reservations",
                reservationBody(tag, table, latestArrival)).andExpect(status().isOk()), "$.data.id");
    }

    /** 发起在线付定金 → 渠道成功 → 回前台确认。 */
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

    /** 后台开台（BarTableSessionsPage 开台表单的真实请求形状；reservationId 为空＝散客开台）。 */
    private ResultActions openSession(String tag, String reservationId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "open-" + tag + "-" + f.suffix);
        body.put("bar_id", barId);
        body.put("table_id", tableId);
        body.put("business_date", java.time.LocalDate.now().toString());
        body.put("guest_count", 2);
        if (reservationId != null) {
            body.put("reservation_id", reservationId);
        }
        return mockMvc.perform(post("/api/v1/bar/table-sessions").with(platformAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 该酒吧商户的员工（商户范围＝本商户）。 */
    private RequestPostProcessor merchantStaff(String merchantId) {
        String userId = "usr_bar_mer_" + f.suffix;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'merchant_staff', NULL, ?, 'active', now())", userId, userId, merchantId);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '订台回归商户', 'merchant', 'active', now())", role, role);
        for (String code : List.of("bar.reservation.view", "bar.reservation.invitation.manage")) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "merchant_staff", null, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }

    private ResultActions confirmDeposit(String token, String reservationId, String intentId) throws Exception {
        return customerPost(token, "/api/v1/customer/bar-reservations/" + reservationId + "/deposit-payments/" + intentId + "/confirm",
                Map.of("idempotency_key", "dep-confirm-" + UUID.randomUUID()));
    }

    /** 后台代订（BarTableSessionsPage 新建订台表单的真实请求形状）。 */
    private ResultActions staffReserve(String tag, String customerPhone) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "staff-" + tag + "-" + f.suffix);
        body.put("bar_id", barId);
        body.put("table_id", tableId);
        body.put("guest_count", 2);
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", Instant.now().plusSeconds(7200).toString());
        if (customerPhone != null) {
            body.put("customer_phone", customerPhone);
        }
        return mockMvc.perform(post("/api/v1/bar/reservations").with(platformAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions customerPost(String token, String path, Map<String, Object> body) throws Exception {
        Object key = body.get("idempotency_key");
        return mockMvc.perform(post(path).header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                .header("Idempotency-Key", String.valueOf(key))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private Map<String, Object> row(String reservationId) {
        return jdbc.queryForMap("SELECT status, hold_status, deposit_status, deposit_settlement FROM bar_reservations WHERE id = ?",
                reservationId);
    }

    /** 平台员工（真实授权链：access_users＋角色→订台相关权限码；平台视角不限商户）。 */
    private RequestPostProcessor platformAdmin() {
        String userId = "usr_bar_admin_" + f.suffix;
        if (jdbc.queryForObject("SELECT count(*) FROM access_users WHERE id = ?", Integer.class, userId) == 0) {
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                    + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", userId, userId);
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                    + " VALUES (?, ?, '订台回归', 'platform', 'active', now())", role, role);
            for (String code : List.of("bar.reservation.create", "bar.reservation.manage", "bar.reservation.view",
                    "bar.reservation.invitation.manage", "bar.table.guest_checkin", "bar.table_session.create")) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
            }
            jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        }
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", null, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }
}
