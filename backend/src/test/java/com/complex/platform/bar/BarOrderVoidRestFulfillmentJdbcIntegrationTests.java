package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.TicketRewardConfig;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AU 段收尾复核（真库、真实入口与授权链）：BSM:823「按整笔有效消费全部实际履约完成的时点统一起算等待期……已取消或已退款部分按原规则从有效消费中排除」；
 * ENUM:112「原订单和责任人继续按实际送达、既有退撤或异常流程处理并留痕」；BSM:1613「谁送达」。先送一部分、再把其余未送明细退掉，剩下的已全部送达：
 * 整单与出品票据置已送达、服务责任记录 completed，预付会员的商户消费待确认积分与成长值按扣除退掉部分后的有效消费生成（与全部送达那条路同一收尾）。
 * 请求按后台订单页与收银端真实形状（蛇形键、X-Terminal-Type: web）经控制器提交。本类 @Transactional 回滚（积分规则版本只在本事务内可见）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarOrderVoidRestFulfillmentJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private CustomerRepository customers;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String bar = "vrf_bar_" + s;
    private final String table = "vrf_table_" + s;
    private final String beer = "vrf_beer_" + s;
    private RequestPostProcessor cashier;

    @BeforeEach
    void seed() throws Exception {
        String project = "vrf_prj_" + s;
        String company = "vrf_ent_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','退余项收尾综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'退余项收尾酒吧','bar',?,?,?,?,'active',now(),now())", bar, company, company, company, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'卡座区','unit','active',now(),now())",
                "vrf_space_" + s, project, bar);
        // 卡座开账单要有该营业日已批准的收费规则（服务费率取 0，账单金额即消费额）。
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_vrf')
                """, "bpr_vrf_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        // 退菜退酒过合同准入闸：本酒吧有一份含酒吧业务的生效合同。
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                + "'effective','[\"bar\"]'::jsonb,now(),now())", "vrf_ct_" + s, bar, bar, bar);
        Instant now = Instant.now();
        bars.saveProduct(new BarProductRecord(beer, bar, bar, "精酿啤酒", "beer", 500L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        // 卡座在本酒吧铺位上、已开台（散客）。
        bars.saveTable(new BarTableRecord(table, bar, bar, "K-" + s, "卡座", "card_seat", 4, 0, 0L, "CNY", "opened", now, now));
        bars.assignTableSpace(table, "vrf_space_" + s);
        bars.saveTableSession(new BarTableSessionRecord("vrf_session_" + s, bar, bar, LocalDate.now(), table, null, null, 2,
                0L, 0, null, null, "walk_in", "open", now, null, null, 1L, now, now));
        cashier = new StaffRequestFixture(jdbc).staff("vrf_cashier_" + s, "staff", project, bar, "bar.order.create", "bar.order.view",
                "finance.payment.register", "bar.order_item.void", "bar.bill.refund");
        // 酒吧业态商户积分与成长值规则（每 1 元 1 分、1 成长值）与等待期 24 小时，本事务内生效。
        Instant effective = now.minusSeconds(5);
        config("merchant_points", "bar", rate(100L), effective);
        config("merchant_growth", "bar", rate(100L), effective);
        String entity = "vrf_be_" + s;
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'积分结算中心','platform','active',now(),now())", entity);
        config("national", null, new TicketRewardConfig.Values(null, null, null, 24, 1, 720, new BigDecimal("0.01"), new BigDecimal("0.001"),
                new BigDecimal("100"), entity, null, null, null, List.of(), 24, 1, 720), effective);
    }

    /**
     * 会员客人在卡座点两行（1 瓶 500、2 瓶 1000；卡座须先付款才能出品），开单后先结清 1500 元（录会员手机号）；先送 2 瓶那一行（部分送达：整单未送达、服务记 active、
     * 不生成积分），再退掉未出品的 1 瓶（即时取消、已结账单按退款冲正 500）：整单置已送达、出品票据置已送达、服务责任另记 completed，
     * 该会员的商户消费奖励生成一条，有效消费 1000 元。修复前红：收尾只在送达接口里判，退菜后订单一直停在未送达、只有 active、积分不生成。
     */
    @Test
    void voidingTheRestAfterPartialDeliveryClosesOrderTicketServiceAndCreatesMemberReward() throws Exception {
        String member = "c_vrf_" + s;
        String phone = "139" + String.format("%08d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000L));
        Instant now = Instant.now();
        customers.saveCustomer(new CustomerRecord(member, phone, "***", "active", null, now, now, now));

        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("idempotency_key", "vrf-order-" + s);
        orderBody.put("bar_id", bar);
        orderBody.put("table_id", table);
        orderBody.put("items", List.of(Map.of("product_id", beer, "quantity", 1), Map.of("product_id", beer, "quantity", 2)));
        String order = data(post(orderBody, "/api/v1/bar/orders").andExpect(status().isOk()), "/data/id");
        String bill = data(post(Map.of("idempotency_key", "vrf-bill-" + s), "/api/v1/bar/orders/" + order + "/bill")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payable_amount_cent").value(1500)), "/data/id");
        Map<String, Object> settle = new LinkedHashMap<>();
        settle.put("idempotency_key", "vrf-settle-" + s);
        settle.put("payment_id", "vrf-settle-" + s);
        settle.put("settlement_amount_cent", 1500);
        settle.put("payment_channel", "cash");
        settle.put("member_phone", phone);
        post(settle, "/api/v1/bar/bills/" + bill + "/settle").andExpect(status().isOk());
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT id, quantity FROM bar_order_items WHERE order_id = ? ORDER BY quantity", order);
        String single = (String) lines.get(0).get("id");
        String pair = (String) lines.get(1).get("id");

        Map<String, Object> deliver = new LinkedHashMap<>();
        deliver.put("idempotency_key", "vrf-deliver-" + s);
        deliver.put("operator_note", "已送达");
        deliver.put("delivery_photo_attachment_ids", List.of());
        deliver.put("delivered_items", List.of(Map.of("order_item_id", pair, "quantity", 2)));
        post(deliver, "/api/v1/bar/orders/" + order + "/deliver").andExpect(status().isOk());
        assertThat(orderStatus(order)).isNotEqualTo("delivered");
        assertThat(serviceStatuses(order)).containsExactly("active");
        assertThat(rewardCount(bill)).isZero();

        Map<String, Object> voidBody = new LinkedHashMap<>();
        voidBody.put("idempotency_key", "vrf-void-" + s);
        voidBody.put("order_item_id", single);
        voidBody.put("reason", "客人不要了");
        voidBody.put("evidence_attachment_ids", List.of());
        voidBody.put("return_to_stock", false);
        voidBody.put("bill_id", bill);
        voidBody.put("operator_note", "退菜退酒");
        post(voidBody, "/api/v1/bar/orders/" + order + "/void-item").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.void_type").value("cancel"));

        assertThat(orderStatus(order)).isEqualTo("delivered");
        assertThat(jdbc.queryForObject("SELECT ticket_status FROM bar_production_tickets WHERE order_id = ?", String.class, order))
                .isEqualTo("delivered");
        assertThat(serviceStatuses(order)).containsExactlyInAnyOrder("active", "completed");
        assertThat(rewardCount(bill)).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT customer_id, effective_amount_cent FROM merchant_consumption_rewards WHERE source_type = 'bar_bill'"
                + " AND source_id = ?", bill)).containsEntry("customer_id", member).containsEntry("effective_amount_cent", 1000L);
    }

    // ---------- helpers ----------

    private static TicketRewardConfig.Values rate(long cents) {
        return new TicketRewardConfig.Values(cents, 1L, 1_000_000L, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    private void config(String kind, String format, TicketRewardConfig.Values values, Instant effective) throws Exception {
        jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,business_format,version,payload,effective_at,published_at,status,created_by,created_at,approved_at)"
                + " VALUES(?,?,?,?,?::jsonb,?,?,'approved','test-reviewer',?,?)",
                "trc_" + UUID.randomUUID().toString().replace("-", ""), kind, format, "V" + Math.floorMod(UUID.randomUUID().hashCode(), 900_000) + ".09",
                json.writeValueAsString(values), Timestamp.from(effective), Timestamp.from(effective.minusSeconds(60)),
                Timestamp.from(effective.minusSeconds(120)), Timestamp.from(effective.minusSeconds(60)));
    }

    private ResultActions post(Map<String, Object> body, String path) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String data(ResultActions result, String pointer) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).at(pointer).asText();
    }

    private String orderStatus(String order) {
        return jdbc.queryForObject("SELECT status FROM bar_orders WHERE id = ?", String.class, order);
    }

    /** 该订单的服务责任记录各条状态（同一事务内时间可能相同，不按时间取最新一行）。 */
    private List<String> serviceStatuses(String order) {
        return jdbc.queryForList("SELECT status FROM service_responsibility_records WHERE order_id = ?", String.class, order);
    }

    private int rewardCount(String bill) {
        return jdbc.queryForObject("SELECT count(*) FROM merchant_consumption_rewards WHERE source_type = 'bar_bill' AND source_id = ?",
                Integer.class, bill);
    }
}
