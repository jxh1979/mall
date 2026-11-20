package com.complex.platform.inventory;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BC 段第 362 条 362-R1（真库、真实入口、真实授权链、蛇形键）：第十八批裁决 71「既然是最高权限，不应该什么都能看到吗？」；
 * BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」。最高权限（保留超管账号，组织 platform-company、无商户）
 * 读库存物料、余额、流水、库房、高价值复核、出库成本汇总、吧台消耗统计、交接链列表与详情都看两个商户、两个项目的数据（读取改取
 * {@link com.complex.platform.access.AuthorizationService#currentMerchantReadScope}）。修复前各读 403 MERCHANT_SCOPE_REQUIRED。
 * 回归：不挂商户的项目员工读物料仍 403 MERCHANT_SCOPE_REQUIRED；商户账号只看本商户；商户甲读乙的交接单详情仍 403 CROSS_MERCHANT_FORBIDDEN。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class InventoryMasterReadsJdbcIntegrationTests {

    /** 流水与出品时刻、统计期间：取远期一日，共享库里别的用例不会落在这一天。 */
    private static final String DAY = "2097-03-17";
    private static final String AT = "TIMESTAMPTZ '2097-03-17 12:00:00+08'";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private Bar a;
    private Bar b;
    private String handover;

    /** 一个综合体里一家酒吧（即商户）的夹具编号。 */
    private record Bar(String project, String merchant, String item, String balance, String movement, String warehouse,
            String review, String traceCode) {
    }

    @BeforeEach
    void seed() {
        a = bar("a", "甲");
        b = bar("b", "乙");
        handover = "r362ho_" + s;
        jdbc.update("INSERT INTO inventory_trace_handovers (id, trace_code, item_id, handover_result, overall_status, current_segment,"
                + " evidence_attachment_ids, created_at, merchant_id, order_id) VALUES (?, ?, ?, 'in_progress', 'in_progress', 'out_verify',"
                + " '[]'::jsonb, now(), ?, ?)", handover, b.traceCode(), b.item(), b.merchant(), "bo_r362_b_" + s);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** 库存台账页各读、库房、高价值复核：最高权限同时看到甲乙两商户的行。 */
    @Test
    void masterReadsEveryMerchantsInventory() throws Exception {
        assertThat(this.<String>read(master(), "/api/v1/inventory/items", "$.data.items[*].id")).contains(a.item(), b.item());
        assertThat(this.<String>read(master(), "/api/v1/inventory/stock-balances", "$.data.items[*].item_id")).contains(a.item(), b.item());
        assertThat(this.<String>read(master(), "/api/v1/inventory/stock-movements?size=200", "$.data.items[*].id"))
                .contains(a.movement(), b.movement());
        assertThat(this.<String>read(master(), "/api/v1/inventory/warehouses", "$.data.items[*].id")).contains(a.warehouse(), b.warehouse());
        assertThat(this.<String>read(master(), "/api/v1/inventory/high-value-reviews", "$.data.items[*].id")).contains(a.review(), b.review());

        String period = "?period_start=" + DAY + "&period_end=" + DAY;
        mockMvc.perform(get("/api/v1/inventory/cost-summary" + period).with(master())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outbound_movement_count").value(2));
        assertThat(this.<String>read(master(), "/api/v1/inventory/bar-consumption-stats" + period, "$.data[*].bar_id"))
                .contains(a.merchant(), b.merchant());
    }

    /** 交接链与补录页：最高权限按溯源码列出乙商户的交接单、读其详情 200；甲商户账号读乙的交接单详情仍 403。 */
    @Test
    void masterReadsAnotherMerchantsTraceHandover() throws Exception {
        assertThat(this.<String>read(master(), "/api/v1/inventory/trace-handovers?trace_code=" + b.traceCode(), "$.data[*].id"))
                .contains(handover);
        mockMvc.perform(get("/api/v1/inventory/trace-handovers/" + handover).with(master())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handover.id").value(handover));

        RequestPostProcessor merchantA = new StaffRequestFixture(jdbc).staff("r362usr_ma_" + s, "staff", a.project(), a.merchant(),
                "inventory.stock.view", "inventory.handover.scan");
        mockMvc.perform(get("/api/v1/inventory/trace-handovers/" + handover).with(merchantA)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    /**
     * 第十八批 BF 段第 351 条 351-R2（第十八批裁决 71；BSM:1164「记录入库、出库、调拨、盘点、报损、报溢和库存调整。」）：库存盘点页的盘点单列表与详情，
     * 最高权限看全部商户的；商户账号照旧只看本商户，读别家的盘点单详情仍 403。修复前最高权限两处读都 403 MERCHANT_SCOPE_REQUIRED。
     */
    @Test
    void masterReadsEveryMerchantsStocktakes() throws Exception {
        String stA = stocktake(a);
        String stB = stocktake(b);
        assertThat(this.<String>read(master(), "/api/v1/inventory/stocktakes", "$.data.items[*].id")).contains(stA, stB);
        mockMvc.perform(get("/api/v1/inventory/stocktakes/" + stB).with(master())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stocktake.id").value(stB))
                .andExpect(jsonPath("$.data.stocktake.warehouse_id").value(b.warehouse()));

        RequestPostProcessor merchantA = new StaffRequestFixture(jdbc).staff("r351usr_ma_" + s, "staff", a.project(), a.merchant(),
                "inventory.stocktake.view");
        assertThat(this.<String>read(merchantA, "/api/v1/inventory/stocktakes", "$.data.items[*].id")).contains(stA).doesNotContain(stB);
        mockMvc.perform(get("/api/v1/inventory/stocktakes/" + stB).with(merchantA)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    /** 回归：商户账号只看本商户；不挂商户的项目员工读物料仍 403 MERCHANT_SCOPE_REQUIRED（控制席 82b-R6 判定不变）。 */
    @Test
    void merchantAndProjectStaffKeepTheirScope() throws Exception {
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor merchantA = staffs.staff("r362usr_ma_" + s, "staff", a.project(), a.merchant(), "inventory.stock.view");
        assertThat(this.<String>read(merchantA, "/api/v1/inventory/items", "$.data.items[*].id")).contains(a.item()).doesNotContain(b.item());

        RequestPostProcessor projectStaff = staffs.staff("r362usr_pa_" + s, "staff", a.project(), null, "inventory.stock.view");
        mockMvc.perform(get("/api/v1/inventory/items").with(projectStaff)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
    }

    // ---------- 夹具 ----------

    /** 一个综合体、一家酒吧：物料与待出品酒水订单（夹具同 TraceHandoverOutboundOrderJdbcTests）、该订单在统计日出品、库房、余额、一条出库流水、一张高价值复核单。 */
    private Bar bar(String tag, String label) {
        String n = tag + "_" + s;
        String project = "r362prj_" + n;
        String merchant = "r362bar_" + n;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                project, "最高权限库存读取回归综合体" + label);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                merchant, "最高权限库存读取回归酒吧" + label + s, project);
        String traceCode = "TRC362-" + n;
        String orderTag = "r362_" + n;
        TraceHandoverOutboundOrderJdbcTests.seedBottleOrder(jdbc, orderTag, merchant, traceCode);
        jdbc.update("UPDATE bar_order_items SET delivery_status = 'delivered', delivered_at = " + AT + " WHERE id = ?", "boi_" + orderTag);
        String warehouse = "r362wh_" + n;
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'bar','吧台仓','active',?)",
                warehouse, project, merchant);
        String item = "itm_" + orderTag;
        String balance = "r362bal_" + n;
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,"
                + "warehouse_id,updated_at) VALUES(?,?,?,10,0,10,?,?,?,now())", balance, item, warehouse, merchant, project, warehouse);
        String movement = "r362mv_" + n;
        jdbc.update("INSERT INTO inventory_stock_movements (id, movement_type, item_id, source_location_id, target_location_id, quantity, merchant_id,"
                + " project_id, unit_cost_cent, total_cost_cent, status, created_at) VALUES (?, 'outbound', ?, ?, NULL, 1, ?, ?, 30000, 30000,"
                + " 'completed', " + AT + ")", movement, item, warehouse, merchant, project);
        String review = "r362hvr_" + n;
        jdbc.update("INSERT INTO inventory_high_value_reviews(id,subject_type,subject_id,photo_attachment_ids,video_attachment_ids,review_status,"
                + "created_at,merchant_id) VALUES(?,'damage_report',?,'[\"p\"]'::jsonb,'[\"v\"]'::jsonb,'pending',now(),?)",
                review, "r362adj_" + n, merchant);
        return new Bar(project, merchant, item, balance, movement, warehouse, review, traceCode);
    }

    /** 该酒吧吧台仓的一张盘点中的盘点单。 */
    private String stocktake(Bar bar) {
        String id = "r351st_" + bar.merchant();
        jdbc.update("INSERT INTO stocktake(id,stocktake_no,warehouse_id,merchant_id,business_date,status,created_at,updated_at)"
                + " VALUES(?,?,?,?,DATE '" + DAY + "','counting',now(),now())", id, "ST351" + bar.merchant(), bar.warehouse(), bar.merchant());
        return id;
    }

    // ---------- 请求 ----------

    private <T> List<T> read(RequestPostProcessor who, String url, String path) throws Exception {
        String body = mockMvc.perform(get(url).with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, path);
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
