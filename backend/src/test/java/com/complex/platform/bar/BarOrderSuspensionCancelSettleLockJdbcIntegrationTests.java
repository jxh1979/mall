package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 AU 段第 277 条 277-R1（真库、双线程）：BSM:1108「取消审批生效与付款成功只能有一项先完成，先完成者生效」；PGM:303「两个操作同时发生冲突时，
 * 一律以“先处理成功”的那个为准……其余自动失效并提示」。经理审批挂起取消与收银结账写同一张账单和同一条挂起：审批改为先锁账单行再锁挂起行（与结账同序），
 * 两者同时发生时不再互等成死锁、落败方 500；结账先成功的，审批等到账单锁后看到挂起已结清，回 409 BAR_SUSPENSION_NOT_CANCEL_PENDING。
 * 收银端先锁住账单（模拟结账已在进行），审批随即发出，持锁方 1.5 秒后在同一事务里结账并提交。请求按后台订单页与收银端真实形状
 * （蛇形键、X-Terminal-Type: web）经控制器与真实授权链提交。
 *
 * <p>本类不开测试事务（两个线程各自提交）；夹具按随机后缀隔离、留在库里。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class BarOrderSuspensionCancelSettleLockJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String bar = "bsl_bar_" + s;
    private final String wine = "bsl_wine_" + s;
    private RequestPostProcessor cashier;
    private RequestPostProcessor manager;

    @BeforeEach
    void seed() throws Exception {
        String project = "bsl_prj_" + s;
        String company = "bsl_ent_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','挂起取消锁序综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'挂起取消锁序经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'挂起取消锁序酒吧','bar',?,?,?,?,'active',now(),now())", bar, company, company, company, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'包厢区','unit','active',now(),now())",
                "bsl_space_" + s, project, bar);
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                + "'effective','[\"bar\"]'::jsonb,now(),now())", "bsl_ct_" + s, bar, bar, bar);
        // 包厢开账单要有该营业日已批准的收费规则（服务费率取 0，账单金额即消费额）。
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_bsl')
                """, "bsl_rule_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        // 整瓶出品扣库存：干红到货价 80 元（低价值，退品即时生效）吧台 10 瓶。
        String item = "bsl_item_" + s;
        String location = "bsl_loc_" + s;
        String counter = "bsl_wh_" + s;
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '包厢吧台')", counter, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '干红 750 毫升', '酒类', '瓶', 'active', ?, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id, project_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?, ?, ?)", "bsl_bal_" + s, item, location, bar, counter, project);
        jdbc.update("INSERT INTO inventory_item_cost_state(item_id,merchant_id,on_hand_quantity,total_cost_cent,avg_unit_cost_cent,has_cost_basis)"
                + " VALUES(?,?,10,80000,8000,TRUE)", item, bar);
        jdbc.update("INSERT INTO inventory_stock_movements (id, movement_type, item_id, target_location_id, quantity, business_type, status,"
                + " created_at, merchant_id, project_id, unit_cost_cent, total_cost_cent) VALUES (?, 'inbound', ?, ?, 1, 'purchase_request', 'completed',"
                + " now() - interval '1 day', ?, ?, 8000, 8000)", "bsl_arrival_" + s, item, location, bar, project);
        Instant now = Instant.now();
        bars.saveProduct(new BarProductRecord(wine, bar, bar, "干红", "wine", 30000L, "CNY", item, location, true, "active",
                now, now, null, null, null, false, null, null, null));
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashier = staffs.staff("bsl_cashier_" + s, "staff", project, bar, "bar.order.create", "finance.payment.register", "bar.bill.hang",
                "bar.bill.hang_cancel", "bar.order_item.void", "evidence.attachment.create", "bar.table.open_close");
        manager = staffs.staff("bsl_manager_" + s, "staff", project, bar, "bar.bill.hang_cancel");
    }

    /**
     * 包厢已出品挂起单，收银员发起取消（取消审批中）；收银端锁住账单正在结账时，另一位经理批准取消：结账照常 200 提交，
     * 经理审批等到账单锁后回 409 BAR_SUSPENSION_NOT_CANCEL_PENDING（不再死锁 500）；挂起已结清、账单已结清、所列明细未作废、没有退菜单。
     */
    @Test
    void managerApprovalWaitingOnSettlementConflictsInsteadOfDeadlock() throws Exception {
        String table = "bsl_table_" + s;
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(table, bar, bar, "T-" + s, "包厢", "vip", 4, 0, 0L, "CNY", "opened", now, now));
        bars.assignTableSpace(table, "bsl_space_" + s);
        bars.saveTableSession(new BarTableSessionRecord("bsl_session_" + s, bar, bar, LocalDate.now(), table, null, null, 2,
                0L, 0, null, null, "walk_in", "open", now, null, null, 1L, now, now));
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("idempotency_key", "bsl-order-" + s);
        order.put("bar_id", bar);
        order.put("table_id", table);
        order.put("items", List.of(Map.of("product_id", wine, "quantity", 1)));
        String orderId = data(perform(staffPost("/api/v1/bar/orders", cashier, order)).andExpect(status().isOk()), "/data/id");
        String bill = data(perform(staffPost("/api/v1/bar/orders/" + orderId + "/bill", cashier, Map.of("idempotency_key", "bsl-bill-" + s)))
                .andExpect(status().isOk()), "/data/id");
        String itemId = jdbc.queryForObject("SELECT id FROM bar_order_items WHERE order_id = ?", String.class, orderId);
        Map<String, Object> deliver = new LinkedHashMap<>();
        deliver.put("idempotency_key", "bsl-deliver-" + s);
        deliver.put("operator_note", "已送达");
        deliver.put("delivery_photo_attachment_ids", List.of());
        deliver.put("delivered_items", List.of(Map.of("order_item_id", itemId, "quantity", 1)));
        perform(staffPost("/api/v1/bar/orders/" + orderId + "/deliver", cashier, deliver)).andExpect(status().isOk());
        perform(staffPost("/api/v1/bar/orders/" + orderId + "/suspend", cashier,
                Map.of("idempotency_key", "bsl-hang-" + s, "reason", "客人稍后付款"))).andExpect(status().isOk());
        Map<String, Object> cancel = new LinkedHashMap<>();
        cancel.put("idempotency_key", "bsl-cancel-" + s);
        cancel.put("order_item_ids", List.of(itemId));
        cancel.put("reason", "客人整瓶未开，退回");
        cancel.put("evidence_attachment_ids", List.of());
        perform(staffPost("/api/v1/bar/orders/" + orderId + "/suspension/cancel", cashier, cancel)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancel_pending_approval"));

        String key = "bsl-settle-" + s;
        Map<String, Object> settle = new LinkedHashMap<>();
        settle.put("idempotency_key", key);
        settle.put("payment_id", key);
        settle.put("settlement_amount_cent", 30000);
        settle.put("payment_channel", "cash");
        CountDownLatch billLocked = new CountDownLatch(1);
        AtomicReference<Throwable> holderError = new AtomicReference<>();
        Thread holder = new Thread(() -> {
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                    try {
                        jdbc.queryForList("SELECT id FROM bar_bills WHERE id = ? FOR UPDATE", String.class, bill);
                        billLocked.countDown();
                        Thread.sleep(1500);
                        mockMvc.perform(staffPost("/api/v1/bar/bills/" + bill + "/settle", cashier, settle)).andExpect(status().isOk());
                    } catch (Exception error) {
                        throw new IllegalStateException(error);
                    }
                });
            } catch (Throwable error) {
                holderError.set(error);
            } finally {
                billLocked.countDown();
            }
        });
        holder.start();
        assertThat(billLocked.await(30, TimeUnit.SECONDS)).isTrue();
        ResultActions decision = perform(staffPost("/api/v1/bar/orders/" + orderId + "/suspension/cancel-decision", manager,
                Map.of("idempotency_key", "bsl-decide-" + s, "decision", "approve", "reason", "核对过现场")));
        holder.join(30_000);

        decision.andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_SUSPENSION_NOT_CANCEL_PENDING"));
        assertThat(holderError.get()).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM bar_order_suspensions WHERE order_id = ?", String.class, orderId)).isEqualTo("settled");
        assertThat(jdbc.queryForObject("SELECT settlement_status FROM bar_bills WHERE id = ?", String.class, bill)).isEqualTo("settled");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_order_item_voids WHERE order_id = ?", Integer.class, orderId)).isZero();
        assertThat(jdbc.queryForObject("SELECT voided FROM bar_order_items WHERE id = ?", Boolean.class, itemId)).isFalse();
    }

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder staffPost(String path, RequestPostProcessor who, Map<String, Object> body) throws Exception {
        return post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request);
    }

    private String data(ResultActions result, String pointer) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).at(pointer).asText();
    }
}
