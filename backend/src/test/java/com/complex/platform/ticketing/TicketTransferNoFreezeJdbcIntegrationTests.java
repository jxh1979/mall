package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.customer.dto.CustomerTicketTransferRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03 clients-R40 门票转赠按 BSM 第 809 行：「发起转赠后不冻结该权益：在接收人点击“确认领取”之前，原持有人仍然可以自己使用这份权益，也可以再次
 * 转赠给其他人。接收人确认领取、原持有人自行核销、原持有人再次转赠——以先处理成功的那个动作为准，其余动作自动失效并提示」；「转出时，系统须同时向转出人
 * 与受赠人明示这一场次限制。受赠人打开转赠页面时，若该人份权益所绑定的场次已经结束，页面须明确提示「该权益所在场次已结束、权益已终结」，此时不发生领取、
 * 「确认领取」不可点击；转赠动作本身不因此被阻止」。客人端请求按前端真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketTransferNoFreezeJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTicketingRepository repository;
    @Autowired private TicketTransferService transferService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JdbcTemplate jdbc;

    private final Instant now = Instant.now();

    @Test
    void holderCanRedeemWhilePendingAndTransferVoided() {
        Fixture f = seed(1);
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        assertThat(repository.atomicUseEntitlement(entitlement(f.parent, 1), now)).isPresent();
        assertThat(transfer(r.id())).containsEntry("status", "voided").containsEntry("void_reason", "holder_redeemed");
    }

    @Test
    void retransferVoidsPreviousPending() {
        Fixture f = seed(1);
        TicketTransferResponse first = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        java.util.concurrent.atomic.AtomicReference<TicketTransferResponse> second = new java.util.concurrent.atomic.AtomicReference<>();
        org.assertj.core.api.Assertions.assertThatCode(() -> second.set(transferService.initiate(f.buyer, f.buyerPhone,
                new CustomerTicketTransferRequest(null, f.parent, null, "phone", f.receiverPhone)))).doesNotThrowAnyException();
        assertThat(transfer(first.id())).containsEntry("status", "voided").containsEntry("void_reason", "holder_retransferred");
        assertThat(transfer(second.get().id())).containsEntry("status", "pending");
    }

    @Test
    void claimAfterHolderRedeemedRejectedWithVoidMessage() {
        Fixture f = seed(1);
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        repository.atomicUseEntitlement(entitlement(f.parent, 1), now);
        assertThatThrownBy(() -> transferService.claim(f.receiver, f.receiverPhone, r.id(), r.claimCode()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("TICKET_TRANSFER_VOIDED"))
                .hasMessageContaining("原持有人已使用或已改转他人");
        assertThat(holder(f.parent, 1)).isEqualTo(f.buyer);
    }

    @Test
    void previewShowsSessionRestrictionAndEnded() throws Exception {
        Fixture f = seed(2);
        String today = LocalDate.now(com.complex.platform.common.LocalZone.ID).toString();
        jdbc.update("UPDATE ticket_instances SET first_session_key = ?, first_used_at = now() WHERE id = ?", today, f.parent);
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, 2));
        preview(f, r.claimCode()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session_restricted").value(true))
                .andExpect(jsonPath("$.data.session_label").value(today))
                .andExpect(jsonPath("$.data.session_ended").value(false));
        String past = LocalDate.now(com.complex.platform.common.LocalZone.ID).minusDays(3).toString();
        jdbc.update("UPDATE ticket_instances SET first_session_key = ? WHERE id = ?", past, f.parent);
        preview(f, r.claimCode()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session_ended").value(true));
    }

    // 第十八批 AH 段第 238 条 238-R2（BSM:800「一个场次＝一次开门到关门；跨夜场次以该场次的实际闭店时间为准，不按自然日切分」；BSM:809「受赠人打开转赠页面时，
    // 若该人份权益所绑定的场次已经结束……」）：首场次键是营业日（无场次配置的票）时，转赠判场次结束取该营业日闭店（无营业日版本按出厂次日 07:00、所在地时区），
    // 不是次日零点。修复前取系统时区次日零点即红。
    @Test
    void businessDayKeyedFirstSessionEndsAtThatDaysCloseNotMidnight() {
        Fixture f = seed(2);
        jdbc.update("UPDATE ticket_instances SET first_session_key = '2031-01-15', first_used_at = now() WHERE id = ?", f.parent);
        assertThat(repository.findFirstSession(f.parent)).get().extracting(FirstSessionWindow.Session::end)
                .isEqualTo(Instant.parse("2031-01-15T23:00:00Z"));
    }

    @Test
    void claimRejectedWhenBoundSessionEnded() throws Exception {
        Fixture f = seed(2);
        String past = LocalDate.now(com.complex.platform.common.LocalZone.ID).minusDays(3).toString();
        jdbc.update("UPDATE ticket_instances SET first_session_key = ?, first_used_at = now() WHERE id = ?", past, f.parent);
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, 2));
        customerPost(f.receiverToken, "/api/v1/customer/ticket-transfers/" + r.id() + "/claim", Map.of("claim_code", r.claimCode()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_TRANSFER_SESSION_ENDED"));
        assertThat(holder(f.parent, 2)).isEqualTo(f.buyer);
        assertThat(transfer(r.id())).containsEntry("status", "pending");
    }

    @Test
    void initiateAllowedEvenIfSessionEndedAndShowsRestriction() throws Exception {
        Fixture f = seed(2);
        String past = LocalDate.now(com.complex.platform.common.LocalZone.ID).minusDays(3).toString();
        jdbc.update("UPDATE ticket_instances SET first_session_key = ?, first_used_at = now() WHERE id = ?", past, f.parent);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("parent_ticket_id", f.parent);
        body.put("slot_index", 2);
        body.put("channel", "link");
        customerPost(f.buyerToken, "/api/v1/customer/ticket-transfers", body).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.session_restricted").value(true))
                .andExpect(jsonPath("$.data.session_label").value(past))
                .andExpect(jsonPath("$.data.session_ended").value(true));
    }

    // ---------- helpers ----------

    private ResultActions preview(Fixture f, String claimCode) throws Exception {
        return mockMvc.perform(get("/api/v1/customer/ticket-transfers/preview").param("claim_code", claimCode)
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.receiverToken));
    }

    private ResultActions customerPost(String token, String path, Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new LinkedHashMap<>(body);
        String key = "tnf-" + UUID.randomUUID();
        actual.put("idempotency_key", key);
        return mockMvc.perform(post(path).header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(actual)));
    }

    private Map<String, Object> transfer(String id) {
        return jdbc.queryForMap("SELECT status, void_reason FROM ticket_transfers WHERE id = ?", id);
    }

    private String holder(String parent, int slot) {
        return repository.findSlotHolder(parent, slot).map(TicketSlotHolderRecord::holderCustomerId).orElse(null);
    }

    private String entitlement(String parent, int slot) {
        return repository.listIssuedEntitlementsByParentSlot(parent, slot).get(0).id();
    }

    private static CustomerTicketTransferRequest link(String parent, Integer slot) {
        return new CustomerTicketTransferRequest(null, parent, slot, "link", null);
    }

    private Fixture seed(int numPeople) {
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at) "
                + "VALUES ('tt_tr','转赠测试票','fixed','active', now(), now()) ON CONFLICT (id) DO NOTHING");
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, created_at, updated_at) "
                + "VALUES ('et_tr','转赠测试权益','ticket',1,100,'active', now(), now()) ON CONFLICT (id) DO NOTHING");
        Fixture f = new Fixture();
        f.buyerPhone = "138" + rnd();
        f.receiverPhone = "139" + rnd();
        f.buyer = saveCustomer(f.buyerPhone);
        f.receiver = saveCustomer(f.receiverPhone);
        f.buyerToken = session(f.buyer);
        f.receiverToken = session(f.receiver);
        f.parent = "tkt_" + UUID.randomUUID().toString().replace("-", "");
        String orderId = "ord_" + f.parent;
        jdbc.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, "
                + "pay_status, issue_status, status, created_at, updated_at) "
                + "VALUES (?, ?, 'tt_tr', 1, 'web', 0, 'paid', 'issued', 'created', now(), now())", orderId, "ONO_" + f.parent);
        repository.saveTicketInstance(new TicketInstanceRecord(
                f.parent, "TNO_" + f.parent, orderId, "tt_tr", 1, numPeople, 1,
                f.buyer, f.buyer, "active", null, null, now, now));
        for (int slot = 1; slot <= numPeople; slot++) {
            repository.upsertSlotHolder(f.parent, slot, f.buyer, now);
            String eid = "ent_" + UUID.randomUUID().toString().replace("-", "");
            repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                    eid, "code_" + eid, "tt_tr", orderId, "et_tr", "ticket", "m1", null, null,
                    1, 0, 100L, "active", now.minusSeconds(60), now.plusSeconds(86_400 * 7), now, now,
                    1, null, null, null, null, null, f.parent, slot, null, null));
        }
        return f;
    }

    private String saveCustomer(String phone) {
        String id = "cust_" + UUID.randomUUID().toString().replace("-", "");
        customerRepository.saveCustomer(new CustomerRecord(id, phone, phone.substring(0, 3) + "****" + phone.substring(7),
                "active", null, now, now, now, null));
        return id;
    }

    private String session(String customerId) {
        String token = "tok_tnf_" + UUID.randomUUID().toString().replace("-", "");
        customerRepository.saveSession(new CustomerSessionRecord("sess_" + token, customerId, token, "miniapp", now,
                now.plusSeconds(3600), "active"));
        return token;
    }

    private static String rnd() {
        return String.valueOf(100_000_00L + (long) (Math.random() * 89_999_99L));
    }

    private static final class Fixture {
        String buyer, receiver, parent, buyerPhone, receiverPhone, buyerToken, receiverToken;
    }
}
