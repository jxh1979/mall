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
 * 操作员裁决第十三批第 11 条「客人端自助申请」（「在客人 App／小程序加「申请退票」：只对本人可退的未用票，提交后走与员工退款同一套审批和原路退回」）
 * 真库、真接口：客人端请求按真实形状（蛇形键、X-Terminal-Type、X-Customer-Token）；员工审批走后台真实接口。
 * 第 8 条「先退票才能注销」：申请中仍拦注销，退完放行。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerTicketRefundJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private TicketingRepository tickets;
    @Autowired private PlatformTransactionManager txManager;

    private CustomerTicketPaymentFixture f;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 20_000L);
        jdbc.update("UPDATE ticket_types SET refundable = TRUE WHERE id = ?", f.ticketTypeId);
    }

    @Test
    void customerAppliesStaffApprovesWithEntityRefundGoesBackToCardThenDeregistrationAllowed() throws Exception {
        String order = paidOrder();
        String ticket = parentTicket(order);
        assertThat(f.cardBalance()).isEqualTo(15_000L);

        String key = "b14-refund-" + f.suffix;
        String refund = CustomerTicketPaymentFixture.read(apply(f.token, key, ticket)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket_instance_id").value(ticket))
                .andExpect(jsonPath("$.data.refund_amount_cent").value(5000))
                .andExpect(jsonPath("$.data.status").value("requested")), "$.data.id");
        // 同一张票同一幂等键重复提交：返回同一申请；换键再提交按既有「退款中」拒。
        apply(f.token, key, ticket).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(refund));
        refused(() -> apply(f.token, "b14-refund-again-" + f.suffix, ticket), 409, "TICKET_INSTANCE_REFUND_IN_PROGRESS");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT reason_code, responsible_entity_id, requested_by, status FROM ticket_refunds WHERE id = ?", refund);
        assertThat(row.get("reason_code")).isEqualTo("ticket_type_rule");
        assertThat(row.get("responsible_entity_id")).isNull();
        assertThat(row.get("requested_by")).isEqualTo(f.customerId);
        assertThat(row.get("status")).isEqualTo("requested");
        myRefunds().andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(refund))
                .andExpect(jsonPath("$.data.items[0].status").value("requested"));

        // 第 8 条：申请处理中仍列在注销预检的可退票清单里、仍拦注销。
        preview().andExpect(jsonPath("$.data.refundable_tickets.length()").value(1));
        refused(this::deregister, 409, "CUSTOMER_REFUNDABLE_TICKETS_HELD");

        // 员工审批：客人没填退款责任主体，批准时须补填（BSM「系统完整记录退款原因、责任主体」）。
        RequestPostProcessor approver = new StaffRequestFixture(jdbc)
                .staff("usr_b14rf_apv_" + f.suffix, "platform_admin", f.projectId, null, "ticket.refund.approve");
        String entity = jdbc.queryForObject("SELECT business_entity_id FROM merchants WHERE id = ?", String.class, f.merchantId);
        refused(() -> approve(approver, refund, Map.of()), 400, "REFUND_RESPONSIBLE_ENTITY_REQUIRED");
        approve(approver, refund, Map.of("responsible_entity_id", entity))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"));
        Map<String, Object> approved = jdbc.queryForMap(
                "SELECT responsible_entity_id, approved_by, status FROM ticket_refunds WHERE id = ?", refund);
        assertThat(approved.get("responsible_entity_id")).isEqualTo(entity);
        assertThat(approved.get("approved_by")).isEqualTo("usr_b14rf_apv_" + f.suffix);
        assertThat(approved.get("status")).isEqualTo("success");
        // 储值付的票款原路回原卡。
        assertThat(f.cardBalance()).isEqualTo(20_000L);
        assertThat(jdbc.queryForList("SELECT status FROM issued_entitlements WHERE order_id = ?", String.class, order))
                .containsOnly("refunded");
        myRefunds().andExpect(jsonPath("$.data.items[0].status").value("success"));

        // 退完放行注销。
        preview().andExpect(jsonPath("$.data.refundable_tickets.length()").value(0));
        deregister().andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, f.customerId))
                .isEqualTo("deregistered");
    }

    @Test
    void customerCannotApplyForAnotherCustomersTicket() throws Exception {
        String order = paidOrder();
        String ticket = parentTicket(order);
        String other = "cus_b14rf_b_" + f.suffix;
        String otherToken = "cust_token_b14rf_b_" + f.suffix;
        String otherPhone = "136" + String.format("%08d", Math.floorMod(other.hashCode(), 100_000_000));
        Instant now = Instant.now();
        customers.saveCustomer(new CustomerRecord(other, otherPhone, "136****" + otherPhone.substring(7), "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_b14rf_b_" + f.suffix, other, otherToken, "miniapp", now,
                now.plusSeconds(3600), "active"));

        apply(otherToken, "b14-refund-other-" + f.suffix, ticket)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_ORDER_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_refunds WHERE target_id = ?", Integer.class, order)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM ticket_instances WHERE id = ?", String.class, ticket)).isEqualTo("active");
    }

    @Test
    void nonRefundableAndUsedTicketsRejectedWithExistingCodes() throws Exception {
        String order = paidOrder();
        String ticket = parentTicket(order);
        jdbc.update("UPDATE ticket_types SET refundable = FALSE WHERE id = ?", f.ticketTypeId);
        apply(f.token, "b14-refund-nr-" + f.suffix, ticket)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_NOT_REFUNDABLE"));

        jdbc.update("UPDATE ticket_types SET refundable = TRUE WHERE id = ?", f.ticketTypeId);
        String entitlement = jdbc.queryForObject("SELECT id FROM issued_entitlements WHERE order_id = ?", String.class, order);
        assertThat(tickets.atomicUseEntitlement(entitlement, Instant.now())).isPresent();
        apply(f.token, "b14-refund-used-" + f.suffix, ticket)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_INSTANCE_VERIFIED_NO_REFUND"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_refunds WHERE target_id = ?", Integer.class, order)).isZero();
    }

    // ---------- helpers ----------

    /** 客人端「申请退票」真实请求形状：蛇形键 {idempotency_key, ticket_instance_id}。 */
    private ResultActions apply(String token, String key, String ticketInstanceId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("ticket_instance_id", ticketInstanceId);
        return mockMvc.perform(post("/api/v1/customer/ticket-refunds").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", token).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    private ResultActions myRefunds() throws Exception {
        return mockMvc.perform(get("/api/v1/customer/ticket-refunds").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", f.token)).andExpect(status().isOk());
    }

    private ResultActions preview() throws Exception {
        return mockMvc.perform(get("/api/v1/customer/deregister/preview").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", f.token)).andExpect(status().isOk());
    }

    private ResultActions deregister() throws Exception {
        return mockMvc.perform(post("/api/v1/customer/deregister").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(Map.of("waive_balance", true))));
    }

    private interface Request {
        ResultActions perform() throws Exception;
    }

    /** 被拒的请求在保存点里执行并回滚（生产里每个请求各自一个事务、被拒即整笔回滚），之后的请求照常进行。 */
    private void refused(Request request, int httpStatus, String detailCode) {
        TransactionTemplate ownRequest = new TransactionTemplate(txManager);
        ownRequest.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        ownRequest.executeWithoutResult(tx -> {
            try {
                request.perform().andExpect(status().is(httpStatus)).andExpect(jsonPath("$.detail_code").value(detailCode));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            tx.setRollbackOnly();
        });
    }

    /** 后台审批真实请求形状（蛇形键）。 */
    private ResultActions approve(RequestPostProcessor staff, String refundId, Map<String, Object> extra) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "b14rf-approve-" + UUID.randomUUID());
        body.put("action", "approve");
        body.put("reason", "票种规则允许退款");
        body.putAll(extra);
        return mockMvc.perform(post("/api/v1/ticketing/refunds/" + refundId + "/approve").with(staff)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    private String paidOrder() throws Exception {
        String orderId = f.createOrderId(1);
        f.payWithStoredValue(orderId, null).andExpect(status().isOk());
        assertThat(f.entitlements(orderId)).isEqualTo(1);
        return orderId;
    }

    private String parentTicket(String orderId) {
        return jdbc.queryForObject("SELECT id FROM ticket_instances WHERE order_id = ?", String.class, orderId);
    }
}
