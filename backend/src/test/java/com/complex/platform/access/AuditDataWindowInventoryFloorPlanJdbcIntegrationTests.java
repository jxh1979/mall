package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CQ 段第 372 条第 6 段 372-S6-R1、R2、R3、R5（PGM:682「外部审计/监管只能查看划定的范围与时间段」；第十八批裁决 77「各清单、明细、导出只看得到
 * 这段时间发生的记录；没有发生日期的资料照常显示」）：库存隔离实物与隔离处置记录、跨主体调拨与借调单（含详情与签收差异处理）、平面图导入任务（含候选明细）、
 * 库存盘点单（含差异明细）按外审数据时段承载；隔离仓库选项、平面图与形状照常显示。真库＋真实接口＋真实角色授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AuditDataWindowInventoryFloorPlanJdbcIntegrationTests {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final String BEFORE = "2025-12-31 23:30:00+08";
    private static final String INSIDE = "2026-06-01 10:00:00+08";
    private static final String AFTER = "2027-01-01 00:00:00+08";
    private static final String[] CODES = {"inventory.stock.view", "inventory.transfer_order.view", "floor_plan.view",
            "floor_plan.import.view", "inventory.stocktake.view"};

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String p1 = "adw6_p1_" + s;
    private final String p2 = "adw6_p2_" + s;
    private final String m1 = "adw6_m1_" + s;
    private final String m2 = "adw6_m2_" + s;
    private final String w1 = "adw6_w1_" + s;
    private final String item = "adw6_item_" + s;
    private final String balance = "adw6_bal_" + s;
    private final String auditor = "usr_adw6_auditor_" + s;
    private RequestPostProcessor auditorRequest;
    private RequestPostProcessor staffRequest;

    @BeforeEach
    void setUp() throws Exception {
        for (String project : List.of(p1, p2)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','外审时段综合体','active',now(),now())",
                    project);
        }
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '外审时段商户', 'retail', ?, 'active', now(), now()), (?, '外审时段商户乙', 'retail', ?, 'active', now(), now())", m1, p1, m2, p2);
        warehouse(w1, p1, m1);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at)"
                + " VALUES(?,'外审时段酒水','liquor','瓶',0,'active',?,now(),now())", item, m1);
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,"
                + "warehouse_id,updated_at) VALUES(?,?,?,10,0,10,?,?,?,now())", balance, item, w1, m1, p1, w1);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        auditorRequest = staffs.staff(auditor, "outsourced_vendor", p1, null, CODES);
        // 对照：同项目、同码、不挂商户、不划时段的员工账号。
        staffRequest = staffs.staff("usr_adw6_staff_" + s, "staff", p1, null, CODES);
        mockMvc.perform(put("/api/v1/access/users/" + auditor + "/audit-data-window").with(master())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"adw6-" + UUID.randomUUID() + "\",\"data_from\":\"2026-01-01\",\"data_to\":\"2026-12-31\"}"))
                .andExpect(status().isOk());
    }

    // ---------- 372-S6-R1 库存隔离 ----------

    @Test
    void quarantineLotsListOnlyThoseReceivedInsideWindow() throws Exception {
        lot("before", BEFORE);
        String inside = lot("inside", INSIDE);
        lot("after", AFTER);
        String url = "/api/v1/inventory/quarantine?warehouse_id=" + w1;
        assertThat(ids(auditorRequest, url, "$.data[*].id")).containsExactly(inside);
        assertThat(ids(staffRequest, url, "$.data[*].id")).hasSize(3);
    }

    @Test
    void quarantineDispositionsFollowTheirOwnRequestedAtWhileLotOutsideWindowStillOpens() throws Exception {
        String lot = lot("before", BEFORE);
        disposition("before", lot, BEFORE);
        String inside = disposition("inside", lot, INSIDE);
        String url = "/api/v1/inventory/quarantine/" + lot + "/dispositions";
        assertThat(ids(auditorRequest, url, "$.data[*].id")).containsExactly(inside);
        assertThat(ids(staffRequest, url, "$.data[*].id")).hasSize(2);
    }

    @Test
    void quarantineWarehouseOptionsShownAsUsual() throws Exception {
        lot("before", BEFORE);
        assertThat(ids(auditorRequest, "/api/v1/inventory/quarantine/warehouses", "$.data[*].id")).contains(w1);
    }

    // ---------- 372-S6-R2 跨主体调拨与借调 ----------

    @Test
    void transferOrdersListByCreatedAtWithTotalOnSameBasis() throws Exception {
        transferOrder("before", p1, m1, BEFORE);
        String inside = transferOrder("inside", p1, m1, INSIDE);
        transferOrder("after", p1, m1, AFTER);
        transferOrder("p2", p2, m2, BEFORE);
        String body = body(auditorRequest, "/api/v1/inventory/transfer-orders");
        assertThat(JsonPath.<List<String>>read(body, "$.data.items[*].id")).containsExactly(inside);
        assertThat(total(body)).isEqualTo(1L);
        assertThat(total(body(staffRequest, "/api/v1/inventory/transfer-orders"))).isEqualTo(3L);
    }

    @Test
    void transferOrderDetailOutsideWindowForbiddenAfterProjectGate() throws Exception {
        String before = transferOrder("before", p1, m1, BEFORE);
        String inside = transferOrder("inside", p1, m1, INSIDE);
        String other = transferOrder("p2", p2, m2, BEFORE);
        forbidden(auditorRequest, "/api/v1/inventory/transfer-orders/" + before, "OUT_OF_AUDIT_DATA_WINDOW");
        mockMvc.perform(get("/api/v1/inventory/transfer-orders/" + inside).with(auditorRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(inside));
        forbidden(auditorRequest, "/api/v1/inventory/transfer-orders/" + other, "CROSS_PROJECT_FORBIDDEN");
        mockMvc.perform(get("/api/v1/inventory/transfer-orders/" + before).with(staffRequest)).andExpect(status().isOk());
    }

    @Test
    void transferFulfillmentOutsideWindowForbiddenWhileInsideListsItsLines() throws Exception {
        String before = transferOrder("before", p1, m1, BEFORE);
        String inside = transferOrder("inside", p1, m1, INSIDE);
        forbidden(auditorRequest, "/api/v1/inventory/transfer-orders/" + before + "/dispositions", "OUT_OF_AUDIT_DATA_WINDOW");
        mockMvc.perform(get("/api/v1/inventory/transfer-orders/" + inside + "/dispositions").with(auditorRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.order_id").value(inside)).andExpect(jsonPath("$.data.lines").isArray());
        mockMvc.perform(get("/api/v1/inventory/transfer-orders/" + before + "/dispositions").with(staffRequest)).andExpect(status().isOk());
    }

    // ---------- 372-S6-R3 平面图导入 ----------

    @Test
    void floorPlanImportTasksListByCreatedAtWithTotalOnSameBasis() throws Exception {
        String fp = floorPlan();
        importTask("before", fp, BEFORE);
        String inside = importTask("inside", fp, INSIDE);
        importTask("after", fp, AFTER);
        String url = "/api/v1/floor-plans/" + fp + "/imports";
        String body = body(auditorRequest, url);
        assertThat(JsonPath.<List<String>>read(body, "$.data.items[*].id")).containsExactly(inside);
        assertThat(total(body)).isEqualTo(1L);
        assertThat(total(body(staffRequest, url))).isEqualTo(3L);
    }

    @Test
    void floorPlanImportCandidatesOutsideWindowForbidden() throws Exception {
        String fp = floorPlan();
        String before = importTask("before", fp, BEFORE);
        String inside = importTask("inside", fp, INSIDE);
        String base = "/api/v1/floor-plans/" + fp + "/imports/";
        forbidden(auditorRequest, base + before + "/candidates", "OUT_OF_AUDIT_DATA_WINDOW");
        mockMvc.perform(get(base + inside + "/candidates").with(auditorRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.task.id").value(inside));
        mockMvc.perform(get(base + before + "/candidates").with(staffRequest)).andExpect(status().isOk());
    }

    @Test
    void floorPlansAndShapesBuiltBeforeWindowShownAsUsual() throws Exception {
        String fp = floorPlan();
        assertThat(ids(auditorRequest, "/api/v1/floor-plans", "$.data.items[*].id")).contains(fp);
        mockMvc.perform(get("/api/v1/floor-plans/" + fp + "/shapes").with(auditorRequest)).andExpect(status().isOk());
    }

    // ---------- 372-S6-R5 库存盘点单 ----------

    @Test
    void stocktakesListByBusinessDateWithTotalOnSameBasis() throws Exception {
        stocktake("before", w1, m1, "2025-12-31");
        String inside = stocktake("inside", w1, m1, "2026-06-01");
        stocktake("after", w1, m1, "2027-01-01");
        stocktake("p2", p2Warehouse(), null, "2025-12-31");
        String body = body(auditorRequest, "/api/v1/inventory/stocktakes");
        assertThat(JsonPath.<List<String>>read(body, "$.data.items[*].id")).containsExactly(inside);
        assertThat(total(body)).isEqualTo(1L);
        assertThat(total(body(staffRequest, "/api/v1/inventory/stocktakes"))).isEqualTo(3L);
    }

    @Test
    void stocktakeDetailOutsideWindowForbiddenAfterProjectGate() throws Exception {
        String before = stocktake("before", w1, m1, "2025-12-31");
        String inside = stocktake("inside", w1, m1, "2026-06-01");
        String other = stocktake("p2", p2Warehouse(), null, "2025-12-31");
        forbidden(auditorRequest, "/api/v1/inventory/stocktakes/" + before, "OUT_OF_AUDIT_DATA_WINDOW");
        mockMvc.perform(get("/api/v1/inventory/stocktakes/" + inside).with(auditorRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.stocktake.id").value(inside))
                .andExpect(jsonPath("$.data.variances").isArray());
        forbidden(auditorRequest, "/api/v1/inventory/stocktakes/" + other, "CROSS_PROJECT_FORBIDDEN");
        mockMvc.perform(get("/api/v1/inventory/stocktakes/" + before).with(staffRequest)).andExpect(status().isOk());
    }

    // ---------- 夹具 ----------

    private String body(RequestPostProcessor who, String url) throws Exception {
        return mockMvc.perform(get(url).with(who)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private List<String> ids(RequestPostProcessor who, String url, String path) throws Exception {
        return JsonPath.read(body(who, url), path);
    }

    private static long total(String body) {
        return ((Number) JsonPath.read(body, "$.data.total")).longValue();
    }

    private void forbidden(RequestPostProcessor who, String url, String code) throws Exception {
        mockMvc.perform(get(url).with(who)).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value(code));
    }

    private void warehouse(String id, String project, String merchant) {
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,?,'外审时段库房','active',?)",
                id, project, merchant == null ? "main" : "bar", merchant);
    }

    private String p2Warehouse() {
        String id = "adw6_w2_" + s;
        warehouse(id, p2, null);
        return id;
    }

    private String lot(String tag, String receivedAt) {
        String id = "adw6_lot_" + tag + "_" + s;
        jdbc.update("INSERT INTO inventory_quarantine_lots (id, stock_balance_id, item_id, warehouse_id, project_id, source_type, source_id, quantity,"
                + " remaining_quantity, evidence_attachment_ids, received_by, received_at) VALUES (?, ?, ?, ?, ?, 'adw_probe', ?, 1, 1,"
                + " '[\"att_adw6\"]'::jsonb, ?, CAST(? AS timestamptz))", id, balance, item, w1, p1, id, auditor, receivedAt);
        return id;
    }

    private String disposition(String tag, String lot, String requestedAt) {
        String id = "adw6_iqd_" + tag + "_" + s;
        jdbc.update("INSERT INTO inventory_quarantine_dispositions (id, lot_id, quantity, disposition_type, reason, evidence_attachment_ids, status,"
                + " requested_by, requested_at) VALUES (?, ?, 1, 'scrap', '外审时段报损', '[\"att_adw6\"]'::jsonb, 'pending_approval', ?,"
                + " CAST(? AS timestamptz))", id, lot, auditor, requestedAt);
        return id;
    }

    /**
     * 更新时刻：正常业务里状态推进、签收、解析复核都会在建立之后刷新 updated_at。时段前建的更新落进时段、时段内建的更新落到时段后，
     * 错按 updated_at 判的清单与详情即被测出。
     */
    private static String later(String createdAt) {
        return BEFORE.equals(createdAt) ? INSIDE : AFTER;
    }

    /** 门店要货单（供货主体 → 商户，已完成）。 */
    private String transferOrder(String tag, String project, String merchant, String createdAt) {
        String id = "adw6_itf_" + tag + "_" + s;
        jdbc.update("INSERT INTO inventory_transfer_orders (id, transfer_type, lending_mode, from_owner_type, from_owner_id, to_owner_type, to_owner_id,"
                + " status, project_id, created_at, updated_at) VALUES (?, 'supply', NULL, 'supply_entity', ?, 'merchant', ?, 'completed', ?,"
                + " CAST(? AS timestamptz), CAST(? AS timestamptz))", id, "adw6_se_" + s, merchant, project, createdAt, later(createdAt));
        return id;
    }

    /** 平面图建于时段之前：平面图没有发生日期，照常显示。 */
    private String floorPlan() {
        String id = "adw6_fp_" + s;
        jdbc.update("INSERT INTO mall_floor_plans (id, project_id, floor_no, plan_name, image_url, image_width, image_height, version_no, status,"
                + " created_at, updated_at) VALUES (?, ?, '1F', '外审时段平面图', 'https://img.example/fp.png', 100, 100, 'v1', 'active',"
                + " CAST(? AS timestamptz), CAST(? AS timestamptz))", id, p1, BEFORE, BEFORE);
        return id;
    }

    private String importTask(String tag, String floorPlan, String createdAt) {
        String id = "adw6_fpi_" + tag + "_" + s;
        jdbc.update("INSERT INTO floor_plan_import_tasks (id, floor_plan_id, source_format, source_name, source_hash, source_payload, status,"
                + " created_at, updated_at) VALUES (?, ?, 'svg', '外审时段导入.svg', ?, '\"<svg/>\"'::jsonb, 'parsed', CAST(? AS timestamptz),"
                + " CAST(? AS timestamptz))", id, floorPlan, "hash_" + id, createdAt, later(createdAt));
        return id;
    }

    private String stocktake(String tag, String warehouse, String merchant, String businessDate) {
        String id = "adw6_st_" + tag + "_" + s;
        jdbc.update("INSERT INTO stocktake (id, stocktake_no, warehouse_id, merchant_id, business_date, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, CAST(? AS date), 'counting', now(), now())", id, "ST" + id, warehouse, merchant, businessDate);
        return id;
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
