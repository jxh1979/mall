package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 R 段第 152 条 152-R2（第十八批裁决 34「销售可以通过微信等发送邀请函，客户点击后确定自己是订台人还是参与者」，题面含「员工替客人代订卡座，
 * 或客人在一体机上订台时」；BSM:870「邀请函上的人分两种身份：开卡人（订下这个卡座的人）与被邀请人（接受邀请占用名额的人）。作为开卡人，可以在 App/小程序上
 * 看到已接受邀请的名单与各自是否已入场，并可把尚未入场的被邀请人移出邀请函」；BSM:1036「接受后该名额自动关联到接受人本人的动态入场码」；ENUM:105「成员确认
 * 接受即占用该卡座的免通票入场名额，并绑定本人的动态入场码」）：员工代订与一体机订台没挂客人账号，客人打开邀请函选「我是订台人」即把订台归到本人名下、
 * 成为开卡人；已有订台人或本人已作为参与者接受的回 409。员工按后台真实请求代订、签发、核验，客人按客人端真实请求（蛇形键）预览与接受，走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarStaffBookingInvitationHolderJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private CustomerTicketPaymentFixture f;
    private String s;
    private String bar;
    private String table;
    private StaffRequestFixture staffs;
    private RequestPostProcessor staff;

    private record Invitation(String token, String signature, String tableId, int capacity) {
    }

    @BeforeEach
    void seed() throws Exception {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        s = f.suffix;
        bar = "bar_sbh_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '代订认领酒吧', 'bar', 'active', now(), now(), ?)", bar, f.projectId);
        table = "table_sbh_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '三人卡座', 'card_seat', 3, 3, 50000, 'CNY', 'active', now(), now())",
                table, bar, bar, "SBH-" + s);
        staffs = new StaffRequestFixture(jdbc);
        staff = staffs.staff("usr_sbh_staff_" + s, "staff", f.projectId, bar, "bar.reservation.create", "bar.reservation.manage",
                "bar.reservation.view", "bar.reservation.invitation.manage", "bar.table.guest_checkin");
    }

    /** 员工代订：签发后客人 A 打开邀请函选「我是订台人」，订台归到 A 名下，A 成为开卡人，「我的订台」可见、取得本人动态入场码并在门口核验入场。 */
    @Test
    void staffBookedReservationClaimedByCustomerAsHolder() throws Exception {
        String reservation = staffBook("one");
        Invitation inv = issue(reservation, "one");
        String a = customer("a");
        preview("a", inv).andExpect(status().isOk()).andExpect(jsonPath("$.data.holder_claimable").value(true));
        confirm("a", inv, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_holder").value(true))
                .andExpect(jsonPath("$.data.member_customer_id").value(a));
        customerGet("a", "/api/v1/customer/bar-reservations").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].reservation_id").value(reservation));
        customerGet("a", "/api/v1/customer/bar-invitations/" + reservation).andExpect(status().isOk());
        String code = CustomerTicketPaymentFixture.read(
                customerGet("a", "/api/v1/customer/bar-invitations/" + reservation + "/entry-code").andExpect(status().isOk()), "$.data.code");
        entryVerify(reservation, code).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_holder").value(true))
                .andExpect(jsonPath("$.data.status").value("entered"));
        assertThat(customerIdOf(reservation)).isEqualTo(a);
        List<Map<String, Object>> holders = holders(reservation);
        assertThat(holders).hasSize(1);
        assertThat(holders.get(0)).containsEntry("member_identifier", a).containsEntry("member_customer_id", a);
    }

    /** 已有订台人：B 预览不可认领、选订台人 409；B 作为参与者接受占一个名额（开卡人行沿用、没多占），开卡人 A 可把未入场的 B 移出。 */
    @Test
    void holderTakenSoSecondCustomerJoinsAsParticipant() throws Exception {
        String reservation = staffBook("two");
        Invitation inv = issue(reservation, "two");
        customer("a");
        customer("b");
        confirm("a", inv, true).andExpect(status().isOk());
        preview("b", inv).andExpect(status().isOk()).andExpect(jsonPath("$.data.holder_claimable").value(false));
        confirm("b", inv, true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_HOLDER_TAKEN"));
        String member = CustomerTicketPaymentFixture.read(confirm("b", inv, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_holder").value(false)), "$.data.id");
        assertThat(activeMembers(reservation)).isEqualTo(2);
        remove("a", reservation, member).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("revoked"));
    }

    /** 已作为参与者接受的人再选订台人：409，订台仍未归任何人名下。 */
    @Test
    void participantAlreadyAcceptedCannotClaimHolder() throws Exception {
        String reservation = staffBook("three");
        Invitation inv = issue(reservation, "three");
        customer("c");
        confirm("c", inv, false).andExpect(status().isOk());
        confirm("c", inv, true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_ALREADY_ACCEPTED"));
        assertThat(customerIdOf(reservation)).isNull();
    }

    /** 一体机订台（付定金后签发）：同一条路，客人选订台人即归到名下、取得本人动态入场码。 */
    @Test
    void kioskReservationClaimedAsHolderTheSameWay() throws Exception {
        String space = "space_sbh_" + s;
        String device = "sbh-kiosk-" + s;
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '酒吧前台一体机', 'shop', 'active', now(), now())", space, f.projectId);
        jdbc.update("INSERT INTO bar_deposit_rules (id, bar_id, scope_type, scope_id, mode, fixed_amount_cent, cancel_free_before_minutes,"
                + " enabled, created_at, updated_at) VALUES (?, ?, 'bar', ?, 'fixed', 20000, 0, TRUE, now(), now())",
                "bar_dep_sbh_" + s, bar, bar);
        RequestPostProcessor admin = staffs.staff("usr_sbh_admin_" + s, "platform_admin", f.projectId, null, "terminal.device.bind");
        mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "sbh-bind-" + s, "device_id", device,
                                "terminal_type", "kiosk", "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "service"))))
                .andExpect(status().isOk());
        RequestPostProcessor kiosk = request -> {
            MockHttpServletRequest r = staff.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "kiosk");
            r.addHeader("X-Device-Id", device);
            return r;
        };
        String reservation = CustomerTicketPaymentFixture.read(book(kiosk, "kiosk").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_source").value("kiosk")), "$.data.id");
        mockMvc.perform(post("/api/v1/bar/reservations/" + reservation + "/pay-deposit").with(kiosk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "sbh-pay-" + s, "channel", "cash"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hold_status").value("held"));
        Invitation inv = issue(reservation, "kiosk");
        String a = customer("a");
        preview("a", inv).andExpect(status().isOk()).andExpect(jsonPath("$.data.holder_claimable").value(true));
        confirm("a", inv, true).andExpect(status().isOk()).andExpect(jsonPath("$.data.is_holder").value(true));
        customerGet("a", "/api/v1/customer/bar-invitations/" + reservation + "/entry-code").andExpect(status().isOk());
        assertThat(customerIdOf(reservation)).isEqualTo(a);
    }

    /** 客人自助订台本来就有开卡人：朋友预览不可认领，选订台人 409，订台仍归原客人。 */
    @Test
    void selfBookedReservationAlreadyHasHolder() throws Exception {
        String holder = customer("holder");
        String reservation = "reservation_sbh_self_" + s;
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO bar_reservations (id, bar_id, merchant_id, table_id, customer_id, reserved_at, guest_count,"
                + " allowed_pass_free_guest_count, deposit_amount_cent, deposit_status, deposit_channel, order_source, status,"
                + " created_terminal, created_at, updated_at, latest_arrival_at, hold_status)"
                + " VALUES (?, ?, ?, ?, ?, ?, 3, 3, 20000, 'paid', 'wechat', 'miniapp', 'reserved', 'miniapp', ?, ?, ?, 'held')",
                reservation, bar, bar, table, holder, Timestamp.from(Instant.now().plusSeconds(3600)), now, now,
                Timestamp.from(Instant.now().plusSeconds(7200)));
        Invitation inv = issue(reservation, "self");
        customer("friend");
        preview("friend", inv).andExpect(status().isOk()).andExpect(jsonPath("$.data.holder_claimable").value(false));
        confirm("friend", inv, true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_HOLDER_TAKEN"));
        assertThat(customerIdOf(reservation)).isEqualTo(holder);
    }

    /** 认领后员工吊销再签发：开卡人仍是认领的客人，开卡人行只有一行。 */
    @Test
    void claimedHolderSurvivesRevokeAndReissueWithoutDuplicate() throws Exception {
        String reservation = staffBook("six");
        Invitation inv = issue(reservation, "six");
        String a = customer("a");
        confirm("a", inv, true).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/bar/reservations/" + reservation + "/invitation/revoke").with(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "sbh-revoke-" + s))))
                .andExpect(status().isOk());
        issue(reservation, "six-again");
        List<Map<String, Object>> holders = holders(reservation);
        assertThat(holders).hasSize(1);
        assertThat(holders.get(0)).containsEntry("member_customer_id", a);
    }

    // ---------- helpers ----------

    /** 后台「卡座与订台」员工代订的真实请求形状（必录客人手机号，第十三批裁决 7）。 */
    private String staffBook(String tag) throws Exception {
        return CustomerTicketPaymentFixture.read(book(staff, tag).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_source").value("staff")), "$.data.id");
    }

    private ResultActions book(RequestPostProcessor who, String tag) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "sbh-book-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", Instant.now().plusSeconds(7200).toString());
        body.put("guest_count", 3);
        body.put("customer_phone", "138" + String.format("%08d", Math.floorMod((s + tag).hashCode(), 100_000_000)));
        return mockMvc.perform(post("/api/v1/bar/reservations").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 后台「确认并签发」（第十三批裁决 4：酒吧员工在后台确认订台后签发）。 */
    private Invitation issue(String reservation, String tag) throws Exception {
        ResultActions issued = mockMvc.perform(post("/api/v1/bar/reservations/" + reservation + "/invitation").with(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "sbh-issue-" + tag + "-" + s))))
                .andExpect(status().isOk());
        return new Invitation(CustomerTicketPaymentFixture.read(issued, "$.data.invitation_token"),
                CustomerTicketPaymentFixture.read(issued, "$.data.invitation_signature"), table,
                Integer.parseInt(CustomerTicketPaymentFixture.read(issued, "$.data.capacity")));
    }

    private String customer(String tag) {
        Instant now = Instant.now();
        String id = "cus_sbh_" + tag + "_" + s;
        String phone = "135" + String.format("%08d", Math.floorMod((s + tag).hashCode(), 100_000_000));
        customers.saveCustomer(new CustomerRecord(id, phone, "135****" + phone.substring(7), "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_sbh_" + tag + "_" + s, id, token(tag), "miniapp", now,
                now.plusSeconds(3600), "active"));
        return id;
    }

    private String token(String tag) {
        return "tok_sbh_" + tag + "_" + s;
    }

    private ResultActions customerGet(String tag, String path) throws Exception {
        return mockMvc.perform(get(path).header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token(tag)));
    }

    private ResultActions preview(String tag, Invitation inv) throws Exception {
        return mockMvc.perform(get("/api/v1/customer/bar-invitations/preview")
                .param("invitation_token", inv.token()).param("invitation_signature", inv.signature())
                .param("table_id", inv.tableId()).param("capacity", String.valueOf(inv.capacity()))
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token(tag)));
    }

    /** 客人端「我是订台人／我是参与者」的真实请求形状（as_holder 缺省即参与者）。 */
    private ResultActions confirm(String tag, Invitation inv, Boolean asHolder) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "sbh-confirm-" + UUID.randomUUID());
        body.put("invitation_token", inv.token());
        body.put("invitation_signature", inv.signature());
        body.put("table_id", inv.tableId());
        body.put("capacity", inv.capacity());
        if (asHolder != null) {
            body.put("as_holder", asHolder);
        }
        return mockMvc.perform(post("/api/v1/customer/bar-invitations/confirm").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", token(tag)).header("Idempotency-Key", body.get("idempotency_key").toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions remove(String tag, String reservation, String memberId) throws Exception {
        String key = "sbh-remove-" + UUID.randomUUID();
        return mockMvc.perform(post("/api/v1/customer/bar-invitations/" + reservation + "/members/" + memberId + "/remove")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token(tag)).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("idempotency_key", key))));
    }

    /** 后台门口入场核验（BarTableSessionsPage 的真实请求形状）。 */
    private ResultActions entryVerify(String reservation, String credential) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "sbh-entry-" + UUID.randomUUID());
        body.put("credential_payload", credential);
        body.put("point_id", bar);
        body.put("business_context", Map.of("bar_id", bar));
        return mockMvc.perform(post("/api/v1/bar/reservations/" + reservation + "/invitation/entry-verify").with(staff)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String customerIdOf(String reservation) {
        return jdbc.queryForObject("SELECT customer_id FROM bar_reservations WHERE id = ?", String.class, reservation);
    }

    private List<Map<String, Object>> holders(String reservation) {
        return jdbc.queryForList("SELECT member_identifier, member_customer_id FROM bar_reservation_invitation_members"
                + " WHERE reservation_id = ? AND is_holder", reservation);
    }

    private int activeMembers(String reservation) {
        return jdbc.queryForObject("SELECT count(*) FROM bar_reservation_invitation_members WHERE reservation_id = ? AND status <> 'revoked'",
                Integer.class, reservation);
    }
}
