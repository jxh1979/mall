package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
 * 第十八批 AO 段第 261 条 261-B04-R2（真库、双线程；第十八批裁决 50）：PGM:303「两个操作同时发生冲突时，一律以“先处理成功”的那个为准……
 * 其余自动失效并提示」；BSM:1201「借出时间取借出方交货扫码的时刻」；BSM:1194「借出（跨主体借调中不可消费物品交付给借入方）」。
 * 借调单发货（借出方）与取消（借入方）先锁单据行再判状态：先到者在自己的事务里做完动作后延迟 1.5 秒提交，后到者在其未提交时开始，
 * 结果与顺序执行一致——先取消后发货，发货 409 TRANSFER_ORDER_NOT_CREATED、不扣库存不写流水；先发货后取消，取消 409
 * TRANSFER_SHIPPED_REQUIRES_RETURN_OR_LOSS、单仍已发货。请求按后台调拨工作台真实形状（蛇形键、X-Terminal-Type）经控制器与真实授权链提交。
 *
 * <p>本类不开测试事务（两个线程各自提交）；夹具按随机后缀隔离、留在库里。修复前红：两种顺序下后到者都 200——先取消的单被写回已发货，
 * 先发货的单被写成已取消、借出方库存却已扣且留有借出流水。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class InventoryTransferShipCancelLockJdbcIntegrationTests {

    private static final String BASE = "/api/v1/inventory/transfer-orders";
    private static final String MANAGE = "inventory.transfer_order.manage";
    private static final String VIEW = "inventory.transfer_order.view";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private InventoryRepository repository;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "tsl_prj_" + s;
    private final String lender = "tsl_lender_" + s;
    private final String borrower = "tsl_borrower_" + s;
    private final String device = "tsl_device_" + s;
    private final String location = "tsl_loc_" + s;
    private RequestPostProcessor admin;
    private RequestPostProcessor lenderStaff;
    private RequestPostProcessor borrowerStaff;

    @BeforeEach
    void seed() {
        String entity = "tsl_ent_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','借调锁单回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'项目主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        for (String m : List.of(lender, borrower)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())",
                    m, m.equals(lender) ? "借出方商户" : "借入方商户", entity, project);
            jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())",
                    "tsl_ct_" + m, m, entity, entity);
        }
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at,non_consumable) VALUES(?,'舞台音响','equipment','台',0,'active',?,now(),now(),TRUE)", device, lender);
        // 借出方出货库位 5 台，带单价入库建立成本基准。
        repository.atomicIncreaseStockBalance(device, location, 5, "tsl_bal_" + s, lender, null, project);
        repository.saveStockMovement(new StockMovementRecord("tsl_in_" + s, "inbound", device, null, location, 5,
                100L, null, "fixture", "tsl_fixture_" + s, null, null, List.of(), "completed", Instant.now(), lender), project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("tsl_admin_" + s, "platform_admin", "org", null, MANAGE, VIEW);
        lenderStaff = staffs.staff("tsl_lender_staff_" + s, "staff", project, lender, MANAGE, VIEW);
        borrowerStaff = staffs.staff("tsl_borrower_staff_" + s, "staff", project, borrower, MANAGE, VIEW);
    }

    /** 借入方先取消（持锁未提交），借出方后发货：发货 409、单仍已取消、借出方库存不动、没有流水；再发货、再取消都 409，状态与余额不变。 */
    @Test
    void shipWaitingOnCancellationIsRejectedAndDeductsNothing() throws Exception {
        String id = createBorrow();

        firstHoldsThenSecond(cancel(id), ship(id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TRANSFER_ORDER_NOT_CREATED"));
        assertState(id, "cancelled", 5);
        assertThat(movements(id)).isEmpty();

        // 第二次还能过吗：失败的发货占位已随事务回滚，再发货仍按业务码拒；借入方换新编号再取消 409。
        mockMvc.perform(ship(id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TRANSFER_ORDER_NOT_CREATED"));
        mockMvc.perform(cancel(id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TRANSFER_ORDER_NOT_CANCELLABLE"));
        assertState(id, "cancelled", 5);
        assertThat(movements(id)).isEmpty();
    }

    /** 借出方先发货（持锁未提交），借入方后取消：取消 409、单仍已发货、余额 3、借出流水 1 条；再取消仍 409，借出方再发货回放第一次结果。 */
    @Test
    void cancellationWaitingOnShipmentIsRejectedAndTheOrderStaysShipped() throws Exception {
        String id = createBorrow();

        firstHoldsThenSecond(ship(id), cancel(id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TRANSFER_SHIPPED_REQUIRES_RETURN_OR_LOSS"));
        assertState(id, "shipped", 3);
        assertThat(movements(id)).containsExactly("lend_out");

        mockMvc.perform(cancel(id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TRANSFER_SHIPPED_REQUIRES_RETURN_OR_LOSS"));
        // 发货的提交编号就是单号：借出方再发货得到第一次的结果并标已完成，不再扣库存。
        mockMvc.perform(ship(id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true)).andExpect(jsonPath("$.data.status").value("shipped"));
        assertState(id, "shipped", 3);
        assertThat(movements(id)).containsExactly("lend_out");
    }

    // ---------- helpers ----------

    /** 平台管理员按蛇形键建借用单：借出方借 2 台音响给借入方，不带溯源码，约定归还时间在 30 天后。 */
    private String createBorrow() throws Exception {
        String body = """
                {"idempotency_key":"%s","transfer_type":"lending","lending_mode":"borrow","from_owner_type":"merchant","from_owner_id":"%s",
                 "to_owner_type":"merchant","to_owner_id":"%s","from_location_id":"%s","return_due_at":"%s",
                 "lines":[{"item_id":"%s","quantity":2,"unit_price_cent":80000,"trace_codes":[]}]}
                """.formatted(key(), lender, borrower, location, Instant.now().plus(Duration.ofDays(30)), device);
        return JsonPath.read(mockMvc.perform(post(BASE).with(admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("created"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    /** 借出方交货（发货）。 */
    private MockHttpServletRequestBuilder ship(String id) {
        return post(BASE + "/" + id + "/ship").with(lenderStaff).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key() + "\",\"scanned_trace_codes\":[]}");
    }

    /** 借入方取消。 */
    private MockHttpServletRequestBuilder cancel(String id) {
        return post(BASE + "/" + id + "/cancel").with(borrowerStaff).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key() + "\"}");
    }

    /** 先到者在自己的事务里做完动作（须 200）后延迟 1.5 秒提交；后到者在其未提交时开始、走自己的事务。返回后到者的响应。 */
    private ResultActions firstHoldsThenSecond(MockHttpServletRequestBuilder first, MockHttpServletRequestBuilder second) throws Exception {
        CountDownLatch firstDone = new CountDownLatch(1);
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        Thread holder = new Thread(() -> {
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                    try {
                        mockMvc.perform(first).andExpect(status().isOk());
                        firstDone.countDown();
                        Thread.sleep(1500);
                    } catch (Exception error) {
                        throw new IllegalStateException(error);
                    }
                });
            } catch (Throwable error) {
                firstError.set(error);
            } finally {
                firstDone.countDown();
            }
        });
        holder.start();
        assertThat(firstDone.await(30, TimeUnit.SECONDS)).isTrue();
        ResultActions result = mockMvc.perform(second);
        holder.join(30_000);
        assertThat(firstError.get()).isNull();
        return result;
    }

    private void assertState(String id, String status, int balance) {
        assertThat(jdbc.queryForObject("SELECT status FROM inventory_transfer_orders WHERE id=?", String.class, id)).isEqualTo(status);
        assertThat(jdbc.queryForObject("SELECT quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?", Integer.class, device, location))
                .isEqualTo(balance);
    }

    private List<String> movements(String id) {
        return jdbc.queryForList("SELECT movement_type FROM inventory_stock_movements WHERE business_type='inventory_transfer' AND business_id=?",
                String.class, id);
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
