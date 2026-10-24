package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.complex.platform.ticketing.TicketingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ROOT-03-R21 账户注销闭环真库、真接口（客人端请求按真实形状：POST /api/v1/customer/deregister 蛇形
 * {"waive_balance":true,"abandon_saved_bottles":true}）。BSM 第 863 行「发起注销时系统须提醒其名下尚有存酒与储值卡余额，客人可以选择不取存酒、
 * 放弃余额，径行注销……注销后：客人的姓名、手机号、人脸、证件等个人信息一律删除……积分、券一律清零，未核销的票按本文件「退款」条的退票规则处理」；
 * ENUM 第 68 行「已注销会员的手机号可以重新注册，新账户与原账户之间不建立任何关联」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerDeregisterClosureJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private LoyaltyService loyalty;
    @Autowired private CustomerService customerService;
    @Autowired private TicketingRepository tickets;
    @Autowired private PlatformTransactionManager txManager;

    private CustomerTicketPaymentFixture f;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 20_000L);
    }

    @Test
    void heldSavedBottlesBlockUntilAbandonConfirmed() throws Exception {
        String bottle = savedBottle();
        deregister(Map.of("waive_balance", true))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_SAVED_BOTTLES_HELD"));
        assertThat(accountStatus(f.customerId)).isEqualTo("active");
        assertThat(jdbc.queryForObject("SELECT status FROM saved_bottles WHERE id = ?", String.class, bottle)).isEqualTo("stored");

        deregister(Map.of("waive_balance", true, "abandon_saved_bottles", true)).andExpect(status().isOk());
        // 放弃的存酒按到期规则转为酒吧自有（置 expired），存酒上的客人姓名与手机号删除。
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, customer_name_or_nickname, customer_phone, customer_phone_hash FROM saved_bottles WHERE id = ?", bottle);
        assertThat(row.get("status")).isEqualTo("expired");
        assertThat(row.get("customer_name_or_nickname")).isNull();
        assertThat(row.get("customer_phone")).isNull();
        assertThat(row.get("customer_phone_hash")).isNull();
    }

    @Test
    void previewListsSavedBottles() throws Exception {
        String bottle = savedBottle();
        mockMvc.perform(get("/api/v1/customer/deregister/preview").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stored_value_cards[0].account_id").value(f.accountId))
                .andExpect(jsonPath("$.data.stored_value_cards[0].principal_cent").value(20_000))
                .andExpect(jsonPath("$.data.saved_bottles[0].id").value(bottle))
                .andExpect(jsonPath("$.data.saved_bottles[0].bottle_name").value("注销测试威士忌"));
    }

    @Test
    void deregisterClearsPointsVouchersAndPersonalData() throws Exception {
        loyalty.earnPoints(f.customerId, 300, "earn", "dereg_src_" + f.suffix, "注销前积分", 30);
        String voucher = issuedVoucher();
        jdbc.update("UPDATE customers SET birth_date = DATE '1990-05-01' WHERE id = ?", f.customerId);
        String phoneHash = jdbc.queryForObject("SELECT phone_hash FROM customers WHERE id = ?", String.class, f.customerId);

        deregister(Map.of("waive_balance", true, "abandon_saved_bottles", true)).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT balance FROM customer_points_accounts WHERE customer_id = ?", Long.class, f.customerId))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_points_ledger WHERE customer_id = ? AND points_delta = -300"
                + " AND description = '账户注销清零'", Integer.class, f.customerId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM voucher_instances WHERE id = ?", String.class, voucher)).isEqualTo("revoked");
        Map<String, Object> customer = jdbc.queryForMap(
                "SELECT status, phone, phone_hash, masked_phone, birth_date FROM customers WHERE id = ?", f.customerId);
        assertThat(customer.get("status")).isEqualTo("deregistered");
        assertThat(customer.get("phone")).isNull();
        assertThat(customer.get("phone_hash")).isNull();
        assertThat(customer.get("masked_phone")).isNull();
        assertThat(customer.get("birth_date")).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM customer_sessions WHERE customer_token_hash IS NOT NULL AND customer_id = ?",
                String.class, f.customerId)).isEqualTo("revoked");
        // 仅留注销时点与该手机号盲索引的标记（一年计数与新人判定）。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_deregistration_marks WHERE phone_hash = ?", Integer.class, phoneHash))
                .isEqualTo(1);
        // 第 10 条「只脱去与客人的关联标识」、BSM 第 863 行「改以不可反查的编号记录」：拿标记的注册、注销时点（含按留存期倒推的注销时点）
        // 精确查 customers 查不到账户，凭手机号查到标记也反查不出客人编号。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers c JOIN customer_deregistration_marks m ON m.phone_hash = ?"
                + " WHERE c.created_at = m.registered_at OR c.updated_at = m.deregistered_at"
                + " OR c.updated_at = m.purge_after - interval '365 days'", Integer.class, phoneHash)).isZero();
    }

    @Test
    void nonRefundableUnusedTicketForfeited() throws Exception {
        String forfeitOrder = paidOrder();
        // 票种不允许退款（出厂默认）：「主动放弃不构成退款理由」，未用权益作废。
        deregister(Map.of("waive_balance", true)).andExpect(status().isOk());
        assertThat(jdbc.queryForList("SELECT status FROM issued_entitlements WHERE order_id = ?", String.class, forfeitOrder))
                .containsOnly("forfeited");
        assertThat(jdbc.queryForObject("SELECT buyer_phone FROM ticket_orders WHERE id = ?", String.class, forfeitOrder)).isNull();
    }

    @Test
    void refundableUnusedTicketBlocksDeregistrationUntilRefunded() throws Exception {
        // 操作员裁决第十三批第 8 条「先退票才能注销」：名下有可退的未用票时，注销页提醒客人先申请退票，退完才能注销。
        String order = paidOrder();
        jdbc.update("UPDATE ticket_types SET refundable = TRUE WHERE id = ?", f.ticketTypeId);
        String ticket = jdbc.queryForObject("SELECT id FROM issued_entitlements WHERE order_id = ?", String.class, order);
        mockMvc.perform(get("/api/v1/customer/deregister/preview").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundable_tickets.length()").value(1))
                .andExpect(jsonPath("$.data.refundable_tickets[0].id").value(ticket));
        deregisterRefusedForRefundableTickets();
        assertThat(accountStatus(f.customerId)).isEqualTo("active");
        assertThat(jdbc.queryForList("SELECT status FROM issued_entitlements WHERE order_id = ?", String.class, order))
                .containsOnly("active");

        // 退票走后台真实接口（蛇形键）：申请后处理中仍拦；审批通过（权益已退）后名下再无此类票，即可注销。
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        String entity = jdbc.queryForObject("SELECT business_entity_id FROM merchants WHERE id = ?", String.class, f.merchantId);
        String refund = CustomerTicketPaymentFixture.read(staffPost(
                staffs.staff("usr_dereg_req_" + f.suffix, "platform_admin", f.projectId, null, "ticket.refund.request"),
                "/api/v1/ticketing/refunds", Map.of("target_id", order, "reason", "客人注销前申请退票",
                        "reason_code", "ticket_type_rule", "responsible_entity_id", entity))
                .andExpect(status().isOk()), "$.data.id");
        deregisterRefusedForRefundableTickets();
        staffPost(staffs.staff("usr_dereg_apv_" + f.suffix, "platform_admin", f.projectId, null, "ticket.refund.approve"),
                "/api/v1/ticketing/refunds/" + refund + "/approve", Map.of("action", "approve", "reason", "票种允许退款"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForList("SELECT status FROM issued_entitlements WHERE order_id = ?", String.class, order))
                .containsOnly("refunded");
        deregister(Map.of("waive_balance", true)).andExpect(status().isOk());
        assertThat(accountStatus(f.customerId)).isEqualTo("deregistered");
    }

    @Test
    void partiallyUsedRefundableTicketDoesNotBlockAndLeftoverForfeited() throws Exception {
        // BSM 第 804 行「门票或权益一经开始核销（含部分核销）即不可退款」：可退票种的通票入场已核销、赠饮未用——整张已不可退，不挡注销；
        // 按第 863 行「未核销的票按本文件「退款」条的退票规则处理」，剩下的赠饮与不可退票种一样作废，不留在已注销账户上。
        jdbc.update("UPDATE ticket_types SET refundable = TRUE WHERE id = ?", f.ticketTypeId);
        String drink = "et_dereg_drink_" + f.suffix;
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " merchant_id, entitlement_kind, consumption_window, created_at, updated_at)"
                + " VALUES (?, '赠饮', 'amusement', 1, 100, 'active', ?, 'virtual', 'per_use', now(), now())", drink, f.merchantId);
        jdbc.update("INSERT INTO ticket_type_entitlements (id, ticket_type_id, entitlement_template_id, quantity, total_times,"
                + " num_people, value_amount_cent, component_price_cent, created_at, updated_at)"
                + " VALUES (?, ?, ?, 1, 1, 1, 100, 0, now(), now())", "bind_dereg_drink_" + f.suffix, f.ticketTypeId, drink);
        String order = f.createOrderId(1);
        f.payWithStoredValue(order, null).andExpect(status().isOk());
        assertThat(f.entitlements(order)).isEqualTo(2);
        String entry = jdbc.queryForObject("SELECT id FROM issued_entitlements WHERE order_id = ? AND entitlement_template_id <> ?",
                String.class, order, drink);
        assertThat(tickets.atomicUseEntitlement(entry, Instant.now())).isPresent();

        mockMvc.perform(get("/api/v1/customer/deregister/preview").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundable_tickets.length()").value(0));
        deregister(Map.of("waive_balance", true)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM issued_entitlements WHERE order_id = ? AND entitlement_template_id = ?",
                String.class, order, drink)).isEqualTo("forfeited");
    }

    @Test
    void buyerHoldingOnlyPartOfRefundableTicketNotBlockedAndOnlyOwnSlotForfeited() throws Exception {
        // 操作员裁决第十三批第 12 条「按不可退作废本人份（推荐）」：只持有部分人份的票不算「本人可退」，不拦注销；注销时只作废本人那几份未用权益。
        SharedTicket t = refundableTwoPersonTicketSecondSlotClaimed();
        preview(f.token).andExpect(jsonPath("$.data.refundable_tickets.length()").value(0));
        deregister(f.token, Map.of("waive_balance", true)).andExpect(status().isOk());
        assertThat(slotStatus(t.parent(), 1)).isEqualTo("forfeited");
        assertThat(slotStatus(t.parent(), 2)).isEqualTo("active");
    }

    @Test
    void receiverHoldingOnlyPartOfRefundableTicketNotBlockedAndOnlyOwnSlotForfeited() throws Exception {
        SharedTicket t = refundableTwoPersonTicketSecondSlotClaimed();
        preview(t.receiverToken()).andExpect(jsonPath("$.data.refundable_tickets.length()").value(0));
        deregister(t.receiverToken(), Map.of("waive_balance", true)).andExpect(status().isOk());
        assertThat(slotStatus(t.parent(), 1)).isEqualTo("active");
        assertThat(slotStatus(t.parent(), 2)).isEqualTo("forfeited");
    }

    @Test
    void buyerDeregistrationVoidsOnlyOwnSlotTransfersNotReceiversRetransfer() throws Exception {
        // BSM 第 809 行「接收人确认领取后可以继续……再次转赠」「以先处理成功的那个动作为准」；操作员裁决第十三批第 12 条「只作废本人那几份未用权益」：
        // A 的可退双人票第 2 人份已归 B、B 再转给 C 未领取；A 注销只作废 A 本人人份上的在途转赠，B→C 照旧在途。
        SharedTicket t = refundableTwoPersonTicketSecondSlotClaimed();
        String own = startLinkTransfer(f.token, t.parent(), 1, "dereg-own-" + f.suffix);
        String retransfer = startLinkTransfer(t.receiverToken(), t.parent(), 2, "dereg-retr-" + f.suffix);
        deregister(f.token, Map.of("waive_balance", true)).andExpect(status().isOk());
        assertThat(transferStatus(own)).isEqualTo("voided");
        assertThat(transferStatus(retransfer)).isEqualTo("pending");
        assertThat(slotStatus(t.parent(), 2)).isEqualTo("active");
    }

    @Test
    void buyerBlacklistKeepsReceiversOwnPendingRetransfer() throws Exception {
        // 拉黑「其他权益按账户注销专条处置」（docs/decisions 2026-10-18 #15）：同上，只作废被拉黑人本人人份，B→C 照旧在途。
        SharedTicket t = refundableTwoPersonTicketSecondSlotClaimed();
        String retransfer = startLinkTransfer(t.receiverToken(), t.parent(), 2, "bl-retr-" + f.suffix);
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin("usr_bl_retr_" + f.suffix);
        try {
            customerService.blacklistCustomer(f.customerId, "店内斗殴");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
        assertThat(slotStatus(t.parent(), 1)).isEqualTo("forfeited");
        assertThat(transferStatus(retransfer)).isEqualTo("pending");
    }

    @Test
    void samePhoneCanRegisterNewAccountAfterDeregistration() throws Exception {
        deregister(Map.of("waive_balance", true)).andExpect(status().isOk());
        Map<String, String> terms = currentTerms();
        String body = mockMvc.perform(post("/api/v1/customer/auth/register").header("X-Terminal-Type", "miniapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("phone", f.phone, "sms_code", "000000", "birth_date", "1995-03-01",
                                "member_terms_version", terms.get("member_terms"), "privacy_terms_version", terms.get("privacy_policy")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String newId = com.jayway.jsonpath.JsonPath.read(body, "$.data.customer_id");
        assertThat(newId).isNotEqualTo(f.customerId);
        assertThat(customers.findCustomerByPhone(f.phone)).get().extracting(CustomerRecord::id).isEqualTo(newId);
        // 新账户与原账户无关联：原账户订单、存储值卡都不归新账户。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_stored_value_accounts WHERE customer_id = ?", Integer.class, newId))
                .isZero();
    }

    @Test
    void blacklistDisposesPointsVouchersTicketsAndPendingTransfersButKeepsPhone() throws Exception {
        loyalty.earnPoints(f.customerId, 300, "earn", "bl_src_" + f.suffix, "拉黑前积分", 30);
        String voucher = issuedVoucher();
        String order = paidOrder();
        String parent = jdbc.queryForObject("SELECT parent_ticket_id FROM issued_entitlements WHERE order_id = ? LIMIT 1",
                String.class, order);
        String transfer = com.jayway.jsonpath.JsonPath.read(f.customerPost("/api/v1/customer/ticket-transfers",
                        Map.of("idempotency_key", "bl-transfer-" + f.suffix, "parent_ticket_id", parent, "slot_index", 1,
                                "channel", "link"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                "$.data.id");

        // 两级批准终审后执行的拉黑（CustomerAccountStatusService 以终审人身份调入）：其他权益按「账户注销」专条处置（docs/decisions 2026-10-18 #15）。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin("usr_bl_head_" + f.suffix);
        try {
            customerService.blacklistCustomer(f.customerId, "店内斗殴");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }

        assertThat(jdbc.queryForObject("SELECT balance FROM customer_points_accounts WHERE customer_id = ?", Long.class, f.customerId))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM voucher_instances WHERE id = ?", String.class, voucher)).isEqualTo("revoked");
        assertThat(jdbc.queryForObject("SELECT status FROM ticket_transfers WHERE id = ?", String.class, transfer)).isEqualTo("voided");
        assertThat(jdbc.queryForList("SELECT status FROM issued_entitlements WHERE order_id = ?", String.class, order))
                .containsOnly("forfeited");
        // 拉黑不删个人信息：手机号保留（解除黑名单时才释放）。
        assertThat(jdbc.queryForObject("SELECT phone_hash FROM customers WHERE id = ?", String.class, f.customerId)).isNotNull();
    }

    // ---------- helpers ----------

    private ResultActions deregister(Map<String, Object> body) throws Exception {
        return deregister(f.token, body);
    }

    private ResultActions deregister(String token, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/customer/deregister").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    private ResultActions preview(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/customer/deregister/preview").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", token)).andExpect(status().isOk());
    }

    private record SharedTicket(String parent, String receiverToken) {
    }

    /**
     * 可退票种的双人票：整张归买票人时仍列为可退、拦注销；随后买票人把第 2 人份转给 B、B 确认领取（客人端真实请求，蛇形键）。
     */
    private SharedTicket refundableTwoPersonTicketSecondSlotClaimed() throws Exception {
        jdbc.update("UPDATE ticket_types SET refundable = TRUE WHERE id = ?", f.ticketTypeId);
        jdbc.update("UPDATE ticket_type_entitlements SET num_people = 2 WHERE ticket_type_id = ?", f.ticketTypeId);
        String order = f.createOrderId(1);
        f.payWithStoredValue(order, null).andExpect(status().isOk());
        assertThat(f.entitlements(order)).isEqualTo(2);
        String parent = jdbc.queryForObject("SELECT id FROM ticket_instances WHERE order_id = ?", String.class, order);
        preview(f.token).andExpect(jsonPath("$.data.refundable_tickets.length()").value(2));
        deregisterRefusedForRefundableTickets();

        String receiver = "cus_dereg_b_" + f.suffix;
        String receiverToken = "cust_token_dereg_b_" + f.suffix;
        String receiverPhone = "136" + String.format("%08d", Math.floorMod(receiver.hashCode(), 100_000_000));
        Instant now = Instant.now();
        customers.saveCustomer(new CustomerRecord(receiver, receiverPhone, "136****" + receiverPhone.substring(7), "active", null, null,
                now, now));
        customers.saveSession(new CustomerSessionRecord("sess_dereg_b_" + f.suffix, receiver, receiverToken, "miniapp", now,
                now.plusSeconds(3600), "active"));
        String body = f.customerPost("/api/v1/customer/ticket-transfers", Map.of("idempotency_key", "dereg-tr-" + f.suffix,
                        "parent_ticket_id", parent, "slot_index", 2, "channel", "link"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String transfer = com.jayway.jsonpath.JsonPath.read(body, "$.data.id");
        String claimKey = "dereg-claim-" + f.suffix;
        mockMvc.perform(post("/api/v1/customer/ticket-transfers/" + transfer + "/claim").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", receiverToken).header("Idempotency-Key", claimKey)
                        .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(Map.of("idempotency_key", claimKey,
                                "claim_code", com.jayway.jsonpath.JsonPath.read(body, "$.data.claim_code")))))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT holder_customer_id FROM ticket_slot_holders WHERE parent_ticket_id = ? AND slot_index = 2",
                String.class, parent)).isEqualTo(receiver);
        return new SharedTicket(parent, receiverToken);
    }

    private String slotStatus(String parent, int slot) {
        return jdbc.queryForObject("SELECT status FROM issued_entitlements WHERE parent_ticket_id = ? AND slot_index = ?",
                String.class, parent, slot);
    }

    /** 客人端发起链接转赠（真实请求形状，蛇形键），返回转赠编号。 */
    private String startLinkTransfer(String token, String parent, int slot, String key) throws Exception {
        String body = mockMvc.perform(post("/api/v1/customer/ticket-transfers").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", token).header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("idempotency_key", key, "parent_ticket_id", parent, "slot_index", slot,
                                "channel", "link"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return com.jayway.jsonpath.JsonPath.read(body, "$.data.id");
    }

    private String transferStatus(String transfer) {
        return jdbc.queryForObject("SELECT status FROM ticket_transfers WHERE id = ?", String.class, transfer);
    }

    /** 名下有可退未用票 → 注销 409；该请求在保存点里执行并回滚（生产里每个请求各自一个事务、被拒即整笔回滚）。 */
    private void deregisterRefusedForRefundableTickets() {
        TransactionTemplate ownRequest = new TransactionTemplate(txManager);
        ownRequest.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        ownRequest.executeWithoutResult(tx -> {
            try {
                deregister(Map.of("waive_balance", true, "abandon_saved_bottles", true))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.detail_code").value("CUSTOMER_REFUNDABLE_TICKETS_HELD"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            tx.setRollbackOnly();
        });
    }

    private ResultActions staffPost(RequestPostProcessor staff, String path, Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new LinkedHashMap<>(body);
        actual.put("idempotency_key", "dereg-" + UUID.randomUUID());
        return mockMvc.perform(post(path).with(staff).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(actual)));
    }

    private String paidOrder() throws Exception {
        String orderId = f.createOrderId(1);
        f.payWithStoredValue(orderId, null).andExpect(status().isOk());
        assertThat(f.entitlements(orderId)).isEqualTo(1);
        return orderId;
    }

    private String savedBottle() {
        String item = "ii_dereg_" + f.suffix;
        String id = "sb_dereg_" + f.suffix;
        jdbc.update("INSERT INTO inventory_items (id, sku_code, item_name, item_type, unit, trace_required, min_quantity, status,"
                + " created_at, updated_at) VALUES (?, ?, '注销测试威士忌', 'wine', 'bottle', false, 0, 'active', now(), now())", item, item);
        jdbc.update("""
                INSERT INTO saved_bottles (id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity, remaining_quantity, unit,
                    expire_at, status, created_at, updated_at, customer_name_or_nickname, customer_phone_hash)
                VALUES (?, ?, ?, ?, '注销测试威士忌', 1, 1, 'bottle', ?, 'stored', now(), now(), '张先生', 'hash_dereg')
                """, id, f.customerId, f.merchantId, item, java.sql.Timestamp.from(Instant.now().plusSeconds(86_400 * 10)));
        return id;
    }

    private String issuedVoucher() {
        String definition = "vd_dereg_" + f.suffix;
        String instance = "vi_dereg_" + f.suffix;
        jdbc.update("""
                INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, inventory_item_id,
                    stock_location_id, stock_quantity, internal_price_cent, status, created_at, updated_at)
                VALUES (?, '注销测试券', 'in_store_goods', 'in_store_staff', 'internal_price', ?, 'inv_dereg', 'loc_dereg', 1, 100,
                    'active', now(), now())
                """, definition, f.merchantId);
        jdbc.update("""
                INSERT INTO voucher_instances (id, definition_id, customer_id, code, source_type, status, issued_at)
                VALUES (?, ?, ?, ?, 'marketing_grant', 'issued', now())
                """, instance, definition, f.customerId, "VC" + f.suffix);
        return instance;
    }

    private Map<String, String> currentTerms() throws Exception {
        String body = mockMvc.perform(get("/api/v1/customer/terms").header("X-Terminal-Type", "miniapp"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return Map.of("member_terms", com.jayway.jsonpath.JsonPath.read(body, "$.data.member_terms.version"),
                "privacy_policy", com.jayway.jsonpath.JsonPath.read(body, "$.data.privacy_policy.version"));
    }

    private String accountStatus(String customerId) {
        return jdbc.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, customerId);
    }
}
