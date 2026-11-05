package com.complex.platform.bar;

// 文件版本：V0.05

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
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03 clients-R6 订卡邀请函客人侧＋backend-R25 接受时校验防伪标记（真库）。依据：
 * BSM 第 870 行「订卡客人可在 App/小程序查看自己的订卡邀请函二维码（注明酒吧、卡座、可进人数），转发给朋友；朋友在 App/小程序确认接受后占一个名额，
 * 该名额自动关联到接受人本人的动态入场码，到店出示本人的动态入场码入场……作为开卡人，可以在 App/小程序上看到已接受邀请的名单与各自是否已入场，
 * 并可把尚未入场的被邀请人移出邀请函、收回其名额……被邀请人一旦完成核销入场，开卡人不得再将其移出……每一次邀请、接受、移出都记录操作人与时间」；
 * BSM 第 1036 行「邀请函二维码只用于转发与接受，本身不作为入场凭证（邀请函二维码带防伪标记，被复制或篡改卡座、可进人数即无法接受）」；
 * ENUM 第 105 行「成员确认接受即占用该卡座的免通票入场名额，并绑定本人的动态入场码；已入场的成员不可再被移出邀请函」。
 * 签发按操作员裁决第十三批第 4 条「酒吧员工在后台确认订台后签发」走后台接口。客人与后台请求按前端真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarInvitationCustomerJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private BarInvitationTokenSigner signer;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String barId = "bar_inv_" + suffix;
    private final String tableId = "table_inv_" + suffix;
    private final String reservationId = "reservation_inv_" + suffix;
    private String holderToken;
    private String friendToken;
    private String friendId;
    private String otherFriendToken;
    private String token;
    private String signature;

    @BeforeEach
    void seed() throws Exception {
        String holderId = customer("holder");
        holderToken = "tok_inv_holder_" + suffix;
        friendId = customer("friend");
        friendToken = "tok_inv_friend_" + suffix;
        customer("other");
        otherFriendToken = "tok_inv_other_" + suffix;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '一号卡座', 'card_seat', 3, 3, 50000, 'CNY', 'active', now(), now())", tableId, barId, "T-INV-" + suffix);
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO bar_reservations (id, bar_id, table_id, customer_id, reserved_at, guest_count, allowed_pass_free_guest_count,"
                + " deposit_amount_cent, deposit_status, deposit_channel, order_source, status, created_terminal, created_at, updated_at,"
                + " latest_arrival_at, hold_status)"
                // 已付定金都带原付款渠道（BSM 第 1062 行「每笔定金保留原订台单、原付款人、付款渠道和金额」）：取消时按原路登记待退。
                + " VALUES (?, ?, ?, ?, ?, 3, 3, 20000, 'paid', 'wechat', 'miniapp', 'reserved', 'miniapp', ?, ?, ?, 'held')",
                reservationId, barId, tableId, holderId, Timestamp.from(Instant.now().plusSeconds(3600)), now, now,
                Timestamp.from(Instant.now().plusSeconds(7200)));
        // 裁决第 4 条：酒吧员工在后台确认订台后签发。
        ResultActions issued = mockMvc.perform(post("/api/v1/bar/reservations/" + reservationId + "/invitation").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "inv-issue-" + suffix))))
                .andExpect(status().isOk());
        token = CustomerTicketPaymentFixture.read(issued, "$.data.invitation_token");
        signature = CustomerTicketPaymentFixture.read(issued, "$.data.invitation_signature");
    }

    @Test
    void holderListsOwnReservations() throws Exception {
        mockMvc.perform(get("/api/v1/customer/bar-reservations").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", holderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].reservation_id").value(reservationId))
                .andExpect(jsonPath("$.data.items[0].table_name").value("一号卡座"))
                .andExpect(jsonPath("$.data.items[0].deposit_status").value("paid"))
                .andExpect(jsonPath("$.data.items[0].invitation_status").value("active"));
        mockMvc.perform(get("/api/v1/customer/bar-reservations").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", friendToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void previewShowsBarTableCapacityWithoutOccupying() throws Exception {
        preview(friendToken, 3, signature)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservation_id").value(reservationId))
                .andExpect(jsonPath("$.data.table_name").value("一号卡座"))
                .andExpect(jsonPath("$.data.capacity").value(3))
                .andExpect(jsonPath("$.data.remaining").value(2));
        assertThat(members()).isEqualTo(1);
    }

    @Test
    void tamperedCapacityRejectedOnPreviewAndConfirm() throws Exception {
        preview(friendToken, 10, signature)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_SIGNATURE_INVALID"));
        confirm(friendToken, 10, signature)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_SIGNATURE_INVALID"));
        assertThat(members()).isEqualTo(1);
    }

    @Test
    void wrongSignatureConfirmRejected() throws Exception {
        confirm(friendToken, 3, "0".repeat(64))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_SIGNATURE_INVALID"));
        assertThat(members()).isEqualTo(1);
    }

    @Test
    void genuineConfirmThenHolderRemovesNotEnteredMember() throws Exception {
        String memberId = CustomerTicketPaymentFixture.read(confirm(friendToken, 3, signature).andExpect(status().isOk()), "$.data.id");
        assertThat(members()).isEqualTo(2);
        remove(holderToken, memberId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("revoked"));
        assertThat(jdbc.queryForObject("SELECT status FROM bar_reservation_invitation_members WHERE id = ?", String.class, memberId))
                .isEqualTo("revoked");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'customer.bar_invitation.member_removed'"
                + " AND business_id = ?", Integer.class, reservationId)).isEqualTo(1);
        // 名额释放后可再邀请他人。
        preview(otherFriendToken, 3, signature).andExpect(status().isOk()).andExpect(jsonPath("$.data.remaining").value(2));
    }

    @Test
    void nonHolderCannotRemove() throws Exception {
        String other = CustomerTicketPaymentFixture.read(confirm(otherFriendToken, 3, signature).andExpect(status().isOk()), "$.data.id");
        confirm(friendToken, 3, signature).andExpect(status().isOk());
        remove(friendToken, other).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT status FROM bar_reservation_invitation_members WHERE id = ?", String.class, other))
                .isEqualTo("confirmed");
    }

    @Test
    void entryVerifyAcceptsFreshEntryCodeAndEnteredMemberCannotBeRemoved() throws Exception {
        String memberId = CustomerTicketPaymentFixture.read(confirm(friendToken, 3, signature).andExpect(status().isOk()), "$.data.id");
        ResultActions code = mockMvc.perform(get("/api/v1/customer/bar-invitations/" + reservationId + "/entry-code")
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", friendToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expires_at").exists())
                .andExpect(jsonPath("$.data.refresh_interval_seconds").exists());
        entryVerify(CustomerTicketPaymentFixture.read(code, "$.data.code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.status").value("entered"));
        remove(holderToken, memberId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_MEMBER_ENTERED"));
    }

    @Test
    void entryVerifyRejectsPlainMemberIdentifier() throws Exception {
        confirm(friendToken, 3, signature).andExpect(status().isOk());
        entryVerify(friendId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_ENTRY_CODE_INVALID"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservation_invitation_members WHERE reservation_id = ?"
                + " AND status = 'entered'", Integer.class, reservationId)).isZero();
    }

    @Test
    void entryVerifyRejectsExpiredEntryCode() throws Exception {
        String memberId = CustomerTicketPaymentFixture.read(confirm(friendToken, 3, signature).andExpect(status().isOk()), "$.data.id");
        entryVerify(signer.entryCode(memberId, reservationId, Instant.now().minusSeconds(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_ENTRY_CODE_INVALID"));
    }

    // ── 第十八批 Q 段第 150 条 150-R1：动态入场码带订台号（BSM:552「进场之后，工作手机扫客人二维码核销各酒吧入场、赠饮、游乐等场内权益」；
    // BSM:1036「到店出示本人的动态入场码核验入场」；ENUM:43「扫码即核销，不设先预检、后确认的两次操作」）──

    /** 客人取到的动态入场码第 2 段就是订台号：工作手机只凭扫到的码拼出核验路径，按蛇形键送入场核验即入场。 */
    @Test
    void entryCodeCarriesReservationIdSoScannerRoutesVerify() throws Exception {
        String memberId = CustomerTicketPaymentFixture.read(confirm(friendToken, 3, signature).andExpect(status().isOk()), "$.data.id");
        String code = CustomerTicketPaymentFixture.read(mockMvc.perform(get("/api/v1/customer/bar-invitations/" + reservationId + "/entry-code")
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", friendToken))
                .andExpect(status().isOk()), "$.data.code");
        String[] parts = code.split("\\.", -1);
        assertThat(parts[1]).isEqualTo(reservationId);
        assertThat(parts).hasSize(6);
        scanVerify(parts[1], code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.status").value("entered"));
    }

    /** 同一酒吧另一张订台：把第一张订台的入场码送到第二张订台的路径，码中订台号对不上即拒，不放行入场。 */
    @Test
    void entryCodeOfOneReservationRejectedOnAnotherReservationOfSameBar() throws Exception {
        confirm(friendToken, 3, signature).andExpect(status().isOk());
        String code = CustomerTicketPaymentFixture.read(mockMvc.perform(get("/api/v1/customer/bar-invitations/" + reservationId + "/entry-code")
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", friendToken))
                .andExpect(status().isOk()), "$.data.code");
        String otherTableId = "table_inv2_" + suffix;
        String otherReservationId = "reservation_inv2_" + suffix;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '二号卡座', 'card_seat', 3, 3, 50000, 'CNY', 'active', now(), now())",
                otherTableId, barId, "T-INV2-" + suffix);
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO bar_reservations (id, bar_id, table_id, customer_id, reserved_at, guest_count, allowed_pass_free_guest_count,"
                + " deposit_amount_cent, deposit_status, deposit_channel, order_source, status, created_terminal, created_at, updated_at,"
                + " latest_arrival_at, hold_status)"
                + " VALUES (?, ?, ?, ?, ?, 3, 3, 20000, 'paid', 'wechat', 'miniapp', 'reserved', 'miniapp', ?, ?, ?, 'held')",
                otherReservationId, barId, otherTableId, friendId, Timestamp.from(Instant.now().plusSeconds(3600)), now, now,
                Timestamp.from(Instant.now().plusSeconds(7200)));
        mockMvc.perform(post("/api/v1/bar/reservations/" + otherReservationId + "/invitation").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "inv-issue2-" + suffix))))
                .andExpect(status().isOk());
        scanVerify(otherReservationId, code)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_ENTRY_CODE_INVALID"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservation_invitation_members WHERE reservation_id IN (?, ?)"
                + " AND status = 'entered'", Integer.class, reservationId, otherReservationId)).isZero();
    }

    /** 工作手机扫码入场核验的请求形状（蛇形键；带本机设备号与点位，酒吧取本机所属商户）。 */
    private ResultActions scanVerify(String pathReservationId, String code) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "inv-scan-" + UUID.randomUUID());
        body.put("credential_payload", code);
        body.put("device_id", "dev_inv_" + suffix);
        body.put("point_id", "pt_inv_" + suffix);
        body.put("business_context", Map.of("bar_id", barId));
        return mockMvc.perform(post("/api/v1/bar/reservations/" + pathReservationId + "/invitation/entry-verify").with(platformAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    @Test
    void cancelledReservationRevokesInvitationForAcceptAndEntry() throws Exception {
        confirm(friendToken, 3, signature).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/bar/reservations/" + reservationId + "/cancel").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "inv-cancel-" + suffix, "no_show", false))))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT invitation_status FROM bar_reservations WHERE id = ?", String.class, reservationId))
                .isEqualTo("revoked");
        // 卡座已不在：未接受的不能再接受，已接受的取不到入场码。
        confirm(otherFriendToken, 3, signature)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_NOT_ACTIVE"));
        mockMvc.perform(get("/api/v1/customer/bar-invitations/" + reservationId + "/entry-code")
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", friendToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_NOT_ACTIVE"));
    }

    @Test
    void removedInviteeReacceptsIntoFreedSeat() throws Exception {
        String memberId = CustomerTicketPaymentFixture.read(confirm(friendToken, 3, signature).andExpect(status().isOk()), "$.data.id");
        remove(holderToken, memberId).andExpect(status().isOk());
        // BSM 第 870 行「名额释放后可再邀请他人」：被移出的人再次接受＝重新占名额，不是把移出记录当成功返回。
        confirm(friendToken, 3, signature).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.status").value("confirmed"));
        assertThat(jdbc.queryForObject("SELECT status FROM bar_reservation_invitation_members WHERE id = ?", String.class, memberId))
                .isEqualTo("confirmed");
        assertThat(members()).isEqualTo(2);
    }

    @Test
    void removedInviteeCannotReacceptWhenSeatsTaken() throws Exception {
        String memberId = CustomerTicketPaymentFixture.read(confirm(friendToken, 3, signature).andExpect(status().isOk()), "$.data.id");
        remove(holderToken, memberId).andExpect(status().isOk());
        confirm(otherFriendToken, 3, signature).andExpect(status().isOk());
        jdbc.update("UPDATE bar_reservations SET invitation_capacity = 2 WHERE id = ?", reservationId);
        jdbc.update("UPDATE bar_reservations SET invitation_signature = ? WHERE id = ?",
                signer.sign(signer.canonical(reservationId, barId, tableId, 2, token)), reservationId);
        confirm(friendToken, 2, signer.sign(signer.canonical(reservationId, barId, tableId, 2, token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_INVITATION_CAPACITY_EXHAUSTED"));
        assertThat(jdbc.queryForObject("SELECT status FROM bar_reservation_invitation_members WHERE id = ?", String.class, memberId))
                .isEqualTo("revoked");
    }

    @Test
    void customerReadsOfReservationsPreviewAndEntryCodeAudited() throws Exception {
        confirm(friendToken, 3, signature).andExpect(status().isOk());
        List<String> events = List.of("customer.bar_reservations.viewed", "customer.bar_invitation.previewed",
                "customer.bar_invitation.preview_failed", "customer.bar_invitation.entry_code_issued",
                "customer.bar_invitation.entry_code_failed");
        Map<String, Integer> before = new LinkedHashMap<>();
        for (String event : events) {
            before.put(event, auditCount(event));
        }
        mockMvc.perform(get("/api/v1/customer/bar-reservations").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", holderToken))
                .andExpect(status().isOk());
        preview(otherFriendToken, 3, signature).andExpect(status().isOk());
        preview(otherFriendToken, 10, signature).andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/customer/bar-invitations/" + reservationId + "/entry-code")
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", friendToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/customer/bar-invitations/" + reservationId + "/entry-code")
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", otherFriendToken))
                .andExpect(status().isConflict());
        // OpenAPI 声明的成功/失败审计（客人取入场凭证必须留痕）。
        for (String event : events) {
            assertThat(auditCount(event)).as(event).isGreaterThan(before.get(event));
        }
    }

    // ── 第十八批 Q 段第 151 条 151-R1：按人数登记卡座免入的两个接口已删除（BSM:1036「到店出示本人的动态入场码核验入场——邀请函二维码只用于转发与接受，
    // 本身不作为入场凭证……已确认与已入场总人数不超过卡座可进人数。」；ENUM:105「成员确认接受即占用该卡座的免通票入场名额」；ENUM:43「系统不提供人工放行」）──

    /** 按蛇形键请求原按人数登记（check-in-guest）与名额预校验（entry-allowance/verify）：路由已不存在（404）、不落登记行；门口只剩凭本人动态入场码的 entry-verify。 */
    @Test
    void headcountCheckInEndpointsAreGoneAndEntryCodeVerifyRemains() throws Exception {
        for (String path : List.of("/check-in-guest", "/entry-allowance/verify")) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("idempotency_key", "inv-headcount-" + UUID.randomUUID());
            body.put("credential_payload", friendId);
            body.put("business_context", Map.of("bar_id", barId));
            mockMvc.perform(post("/api/v1/bar/reservations/" + reservationId + path).with(platformAdmin())
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                    .andExpect(status().isNotFound());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_table_guest_checkins WHERE reservation_id = ?", Integer.class, reservationId))
                .isZero();
        String memberId = CustomerTicketPaymentFixture.read(confirm(friendToken, 3, signature).andExpect(status().isOk()), "$.data.id");
        ResultActions code = mockMvc.perform(get("/api/v1/customer/bar-invitations/" + reservationId + "/entry-code")
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", friendToken))
                .andExpect(status().isOk());
        entryVerify(CustomerTicketPaymentFixture.read(code, "$.data.code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.status").value("entered"));
    }

    // ── 第十八批 Q 段第 151 条 151-R2：新建卡座只录可进人数一个数，免通票入场名额就是它（BSM:1036「对订台签发邀请函二维码（注明酒吧、卡座、可进人数）……
    // 已确认与已入场总人数不超过卡座可进人数。」；ENUM:105「成员确认接受即占用该卡座的免通票入场名额」）──

    /** 按后台「新建卡座」真实请求（蛇形键、不带 allowed_free_entry_count）建卡座：免通票入场名额列写同一个可进人数；随后订台签发，邀请函名额＝该数。 */
    @Test
    void createTableWithCapacityOnlyWritesSameFreeEntryQuotaAndInvitationUsesIt() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "inv-table6-" + suffix);
        body.put("bar_id", barId);
        body.put("table_name", "六人卡座");
        body.put("table_type", "card_seat");
        body.put("capacity", 6);
        body.put("minimum_spend_cent", 0);
        ResultActions created = mockMvc.perform(post("/api/v1/bar/tables").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(6))
                .andExpect(jsonPath("$.data.allowed_pass_free_guest_count").value(6));
        String sixTableId = CustomerTicketPaymentFixture.read(created, "$.data.id");
        assertThat(jdbc.queryForObject("SELECT allowed_pass_free_guest_count FROM bar_tables WHERE id = ?", Integer.class, sixTableId))
                .isEqualTo(6);
        String sixReservationId = "reservation_inv6_" + suffix;
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO bar_reservations (id, bar_id, table_id, customer_id, reserved_at, guest_count, allowed_pass_free_guest_count,"
                + " deposit_amount_cent, deposit_status, deposit_channel, order_source, status, created_terminal, created_at, updated_at,"
                + " latest_arrival_at, hold_status)"
                + " VALUES (?, ?, ?, ?, ?, 6, 6, 20000, 'paid', 'wechat', 'miniapp', 'reserved', 'miniapp', ?, ?, ?, 'held')",
                sixReservationId, barId, sixTableId, friendId, Timestamp.from(Instant.now().plusSeconds(3600)), now, now,
                Timestamp.from(Instant.now().plusSeconds(7200)));
        mockMvc.perform(post("/api/v1/bar/reservations/" + sixReservationId + "/invitation").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "inv-issue6-" + suffix))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(6));
    }

    // ── 第十八批 S 段第 168 条 168-R1：契约 BarTableRequest.table_type 取值与代码一致（card_seat、vip、bar_counter、loose_table；
    // BSM 第 1058 行「桌台/卡座/吧台位」、第 1025 行「扫码点单只适用于散台」）──

    /** 按后台「新建卡座」真实请求（蛇形键）用契约取值 loose_table 建散台：200，库里 table_type 原样为 loose_table。 */
    @Test
    void createTableWithContractLooseTableTypePersistsLooseTable() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "inv-loose-" + suffix);
        body.put("bar_id", barId);
        body.put("table_name", "散台一号");
        body.put("table_type", "loose_table");
        body.put("capacity", 4);
        body.put("minimum_spend_cent", 0);
        ResultActions created = mockMvc.perform(post("/api/v1/bar/tables").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk());
        String looseId = CustomerTicketPaymentFixture.read(created, "$.data.id");
        assertThat(jdbc.queryForObject("SELECT table_type FROM bar_tables WHERE id = ?", String.class, looseId))
                .isEqualTo("loose_table");
    }

    // ── 第十八批 U 段第 170 条 170-R1（决策台账:4139「以代码和权威字典为准，只改契约」）：契约 CustomerBarReservationRequest.table_type
    // 须等于订台选项接口对该台位返回的 table_type（示例 vip_sofa 改 vip）──

    /** 客人先取订台选项，再按选项回带的 table_id、table_type 下单（蛇形键）：200；table_type 改成旧示例值 vip_sofa：404 BAR_TABLE_NOT_FOUND。 */
    @Test
    void customerReservationTableTypeMustEqualOptionValue() throws Exception {
        String vipId = "table_inv_vip_" + suffix;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '贵宾包坐', 'vip', 6, 6, 80000, 'CNY', 'active', now(), now())", vipId, barId, "T-VIP-" + suffix);
        jdbc.update("INSERT INTO bar_deposit_rules (id, bar_id, scope_type, scope_id, mode, fixed_amount_cent, cancel_free_before_minutes,"
                + " enabled, created_at, updated_at) VALUES (?, ?, 'bar', ?, 'fixed', 20000, 0, TRUE, now(), now())",
                "bar_dep_rule_inv_" + suffix, barId, barId);
        String options = mockMvc.perform(get("/api/v1/customer/bar-reservation-options").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", friendToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> types = JsonPath.read(options, "$.data.items[?(@.table_id == '" + vipId + "')].table_type");
        assertThat(types).containsExactly("vip");

        customerReserve(vipId, "vip_sofa", "inv-resv-sofa-" + suffix)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_FOUND"));
        customerReserve(vipId, types.get(0), "inv-resv-vip-" + suffix)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.table_id").value(vipId));
    }

    private ResultActions customerReserve(String table, String tableType, String key) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("bar_id", barId);
        body.put("table_id", table);
        body.put("table_type", tableType);
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", Instant.now().plusSeconds(7200).toString());
        body.put("guest_count", 2);
        return mockMvc.perform(post("/api/v1/customer/bar-reservations").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", friendToken).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private int auditCount(String event) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ?", Integer.class, event);
    }

    // ---------- helpers ----------

    private String customer(String tag) {
        Instant now = Instant.now();
        String id = "cus_inv_" + tag + "_" + suffix;
        String phone = "135" + String.format("%08d", Math.floorMod((suffix + tag).hashCode(), 100_000_000));
        customers.saveCustomer(new CustomerRecord(id, phone, "135****" + phone.substring(7), "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_inv_" + tag + "_" + suffix, id, "tok_inv_" + tag + "_" + suffix,
                "miniapp", now, now.plusSeconds(3600), "active"));
        return id;
    }

    private ResultActions preview(String customerToken, int capacity, String sig) throws Exception {
        return mockMvc.perform(get("/api/v1/customer/bar-invitations/preview")
                .param("invitation_token", token).param("invitation_signature", sig)
                .param("table_id", tableId).param("capacity", String.valueOf(capacity))
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", customerToken));
    }

    private ResultActions confirm(String customerToken, int capacity, String sig) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "inv-confirm-" + UUID.randomUUID());
        body.put("invitation_token", token);
        body.put("invitation_signature", sig);
        body.put("table_id", tableId);
        body.put("capacity", capacity);
        return mockMvc.perform(post("/api/v1/customer/bar-invitations/confirm").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", customerToken).header("Idempotency-Key", body.get("idempotency_key").toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions remove(String customerToken, String memberId) throws Exception {
        String key = "inv-remove-" + UUID.randomUUID();
        return mockMvc.perform(post("/api/v1/customer/bar-invitations/" + reservationId + "/members/" + memberId + "/remove")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", customerToken).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("idempotency_key", key))));
    }

    /** 后台门口入场核验（BarTableSessionsPage 的真实请求形状）。 */
    private ResultActions entryVerify(String credential) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "inv-entry-" + UUID.randomUUID());
        body.put("credential_payload", credential);
        body.put("point_id", barId);
        body.put("business_context", Map.of("bar_id", barId));
        return mockMvc.perform(post("/api/v1/bar/reservations/" + reservationId + "/invitation/entry-verify").with(platformAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private int members() {
        return jdbc.queryForObject("SELECT count(*) FROM bar_reservation_invitation_members WHERE reservation_id = ? AND status <> 'revoked'",
                Integer.class, reservationId);
    }

    /** 平台员工（真实授权链：access_users＋角色→订台相关权限码；平台视角不限商户）。 */
    private RequestPostProcessor platformAdmin() {
        String userId = "usr_bar_admin_" + suffix;
        if (jdbc.queryForObject("SELECT count(*) FROM access_users WHERE id = ?", Integer.class, userId) == 0) {
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                    + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", userId, userId);
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                    + " VALUES (?, ?, '订台回归', 'platform', 'active', now())", role, role);
            for (String code : List.of("bar.reservation.create", "bar.reservation.manage", "bar.reservation.view",
                    "bar.reservation.invitation.manage", "bar.table.guest_checkin", "bar.table.manage")) {
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
