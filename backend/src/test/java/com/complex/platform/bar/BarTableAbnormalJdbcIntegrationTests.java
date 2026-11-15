package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 第十八批 AK 段第 226 条 226-R1 台位登记异常与当班主管确认恢复（真库）。依据：ENUM:100「桌台/卡座/吧台位状态｜空闲、已预留、已开台、待清台、停用、异常。……
 * 已预留只能经取消订台、超时释放、到店开台三条路径退出……异常由系统告警或人工登记进入，处置完成后由当班主管确认恢复，全程留痕。」；BSM:1058 同句；
 * 第十八批裁决 53（空闲或待清台可登记异常，已预留、已开台不能直接登记，当班主管确认恢复后回空闲）；第十批裁决 6（当班主管＝本酒吧、此刻有排班、
 * 持 bar.table.open_close 的员工）。后台请求按前端真实形状（蛇形键），走真实入口与真实授权链（access_users＋角色→权限码）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarTableAbnormalJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "bta_prj_" + s;
    private final String bar = "bta_bar_" + s;
    private final String product = "bta_product_" + s;

    private StaffRequestFixture staffs;
    /** 现场员工：持开关台权限，此刻没有排班。 */
    private RequestPostProcessor clerk;
    /** 当班主管：本酒吧账号、持开关台权限、此刻有排班。 */
    private RequestPostProcessor supervisor;
    private String seat;
    private String seat2;
    private String seat3;
    private String counter;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','台位异常综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'台位异常酒吧','bar','active',?,now(),now())",
                bar, project);
        seat = table(bar, "seat", "card_seat");
        seat2 = table(bar, "seat2", "card_seat");
        seat3 = table(bar, "seat3", "card_seat");
        counter = table(bar, "counter", "bar_counter");
        jdbc.update("INSERT INTO bar_products (id, bar_id, merchant_id, product_name, product_type, price_cent, currency,"
                + " stock_deduction_required, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '精酿啤酒', 'beer', 3800, 'CNY', FALSE, 'active', now(), now())", product, bar, bar);
        staffs = new StaffRequestFixture(jdbc);
        clerk = staffs.staff("usr_bta_clerk_" + s, "staff", project, bar, "bar.table.open_close", "bar.table_session.create",
                "bar.reservation.create", "bar.order.create");
        supervisor = staffs.staff("usr_bta_supervisor_" + s, "staff", project, bar, "bar.table.open_close");
        Instant now = Instant.now();
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,project_id) VALUES(?,?,?,?,?,?)",
                "bta_shift_" + s, "usr_bta_supervisor_" + s, LocalDate.now(), Timestamp.from(now.minusSeconds(3600)),
                Timestamp.from(now.plusSeconds(3600)), project);
    }

    @Test
    void markingIdleTableAbnormalRecordsReasonAndBlocksOpenBookingAndCounterOrder() throws Exception {
        change(clerk, seat, "abnormal", "mark", "沙发破损")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("abnormal"));
        assertThat(tableStatus(seat)).isEqualTo("abnormal");
        assertThat(audit("bar.table.abnormal_marked", seat))
                .containsEntry("reason", "沙发破损").containsEntry("status_before", "active").containsEntry("status_after", "abnormal");

        // 异常台位不能开台、不能被员工代订。
        openSession("open-abnormal", seat)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_OPENABLE"));
        staffReserve("book-abnormal", seat)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_ALREADY_RESERVED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE table_id = ? AND hold_status = 'held'", Integer.class, seat)).isZero();

        // 吧台位不经开台直接下单：异常的吧台位在下单时拦下。
        change(clerk, counter, "abnormal", "mark-counter", "吧台位灯具故障").andExpect(status().isOk());
        order("abnormal", counter)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_ABNORMAL"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_orders WHERE table_id = ?", Integer.class, counter)).isZero();
    }

    @Test
    void pendingClearTableCanBeMarkedButReservedAndOpenedTablesCannot() throws Exception {
        jdbc.update("UPDATE bar_tables SET status = 'pending_clear', pending_clear_since = now() WHERE id = ?", seat2);
        change(clerk, seat2, "abnormal", "mark-pending", "清台时发现台面开裂")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("abnormal"));
        assertThat(audit("bar.table.abnormal_marked", seat2))
                .containsEntry("reason", "清台时发现台面开裂").containsEntry("status_before", "pending_clear").containsEntry("status_after", "abnormal");

        // 已开台、已预留的不能直接登记（已预留须先取消订台；已开台先转台或关台）。
        openSession("opened", seat).andExpect(status().isOk());
        change(clerk, seat, "abnormal", "mark-opened", "客人在台上")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_ABNORMALIZABLE"));
        staffReserve("held", seat3).andExpect(status().isOk()).andExpect(jsonPath("$.data.hold_status").value("held"));
        change(clerk, seat3, "abnormal", "mark-held", "已被预订")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_ABNORMALIZABLE"));
        assertThat(List.of(tableStatus(seat), tableStatus(seat2), tableStatus(seat3))).containsExactly("opened", "abnormal", "active");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.table.abnormal_marked' AND business_id IN (?, ?)",
                Integer.class, seat, seat3)).isZero();
    }

    @Test
    void onDutySupervisorResolvesBackToIdleWhileOffDutyStaffWithSamePermissionCannot() throws Exception {
        change(clerk, seat, "abnormal", "mark", "现场纠纷暂封").andExpect(status().isOk());

        // 同样持开关台权限、此刻没有排班的本酒吧员工不能确认恢复。
        change(clerk, seat, "abnormal/resolve", "resolve-off-duty", "想恢复")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_RESOLVER_NOT_ON_DUTY"));
        assertThat(tableStatus(seat)).isEqualTo("abnormal");

        change(supervisor, seat, "abnormal/resolve", "resolve", "已处置完毕")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));
        assertThat(tableStatus(seat)).isEqualTo("active");
        assertThat(audit("bar.table.abnormal_resolved", seat))
                .containsEntry("reason", "已处置完毕").containsEntry("status_before", "abnormal").containsEntry("status_after", "active");

        // 回到空闲后可正常开台；不在异常状态的台位不能再确认恢复。
        openSession("open-resolved", seat).andExpect(status().isOk());
        assertThat(tableStatus(seat)).isEqualTo("opened");
        change(supervisor, seat, "abnormal/resolve", "resolve-again", "重复确认")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_ABNORMAL"));
    }

    @Test
    void reasonIsRequiredAndPermissionAndOwnMerchantGatesApplyWithoutConsumingIdempotencyKey() throws Exception {
        change(clerk, seat, "abnormal", "no-reason", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(tableStatus(seat)).isEqualTo("active");
        change(clerk, seat, "abnormal", "mark", "台面破损").andExpect(status().isOk());
        change(supervisor, seat, "abnormal/resolve", "resolve-no-reason", " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(tableStatus(seat)).isEqualTo("abnormal");

        RequestPostProcessor waiter = staffs.staff("usr_bta_waiter_" + s, "staff", project, bar, "bar.order.create");
        change(waiter, seat2, "abnormal", "waiter", "没有开关台权限").andExpect(status().isForbidden());
        assertThat(tableStatus(seat2)).isEqualTo("active");

        String otherBar = "bta_bar2_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'隔壁酒吧','bar','active',?,now(),now())",
                otherBar, project);
        String otherTable = table(otherBar, "other", "card_seat");
        change(clerk, otherTable, "abnormal", "cross", "登记别家的台")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(tableStatus(otherTable)).isEqualTo("active");
        // 商户闸在幂等之前：被拒的那次不占请求编号，本商户员工用同一编号照常办理。
        RequestPostProcessor otherClerk = staffs.staff("usr_bta_clerk2_" + s, "staff", project, otherBar, "bar.table.open_close");
        change(otherClerk, otherTable, "abnormal", "cross", "本店登记")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("abnormal"));
    }

    // ---------- helpers ----------

    private String table(String barId, String tag, String type) {
        String id = "bta_table_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '台位异常回归', ?, 6, 2, 0, 'CNY', 'active', now(), now())",
                id, barId, barId, "T-" + tag.toUpperCase() + "-" + s, type);
        return id;
    }

    /** 后台卡座表「登记异常」「确认恢复」按钮的请求形状：原因必填。 */
    private ResultActions change(RequestPostProcessor who, String tableId, String action, String tag, String reason) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bta-" + tag + "-" + s);
        if (reason != null) {
            body.put("reason", reason);
        }
        return mockMvc.perform(post("/api/v1/bar/tables/" + tableId + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 后台开台表单（散客开台，不带订台号）。 */
    private ResultActions openSession(String tag, String tableId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bta-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", tableId);
        body.put("business_date", LocalDate.now().toString());
        body.put("guest_count", 2);
        return mockMvc.perform(post("/api/v1/bar/table-sessions").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 后台代订（新建订台表单的真实请求形状）。 */
    private ResultActions staffReserve(String tag, String tableId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bta-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", tableId);
        body.put("guest_count", 2);
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", Instant.now().plusSeconds(7200).toString());
        body.put("customer_phone", "13900001234");
        return mockMvc.perform(post("/api/v1/bar/reservations").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 吧台位不经开台直接下单（后台点单与一体机吧台买酒同一入口）。 */
    private ResultActions order(String tag, String tableId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bta-order-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", tableId);
        body.put("items", List.of(Map.of("product_id", product, "quantity", 1)));
        return mockMvc.perform(post("/api/v1/bar/orders").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String tableStatus(String tableId) {
        return jdbc.queryForObject("SELECT status FROM bar_tables WHERE id = ?", String.class, tableId);
    }

    private Map<String, Object> audit(String event, String tableId) {
        return jdbc.queryForMap("SELECT metadata->>'reason' AS reason, metadata->>'status_before' AS status_before,"
                + " metadata->>'status_after' AS status_after FROM audit_events WHERE event_name = ? AND business_id = ?", event, tableId);
    }
}
