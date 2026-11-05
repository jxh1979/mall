package com.complex.platform.inventory;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.evidence.EvidenceEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 G 段第 82 条 82b-R6 ②：高价值复核与盘点差异复核补写的余额与流水记到库存行所在项目，不再记复核人本人项目。
 * 真库＋真实接口，请求体按前端蛇形键。依据：PGM:659「一个商户要在别的项目经营的，由能管理商户档案的人在商户档案里加经营项目」；
 * 第十八批裁决 18 所选「对，按业务发生地（推荐）」；待开发清单第 82 条。
 *
 * <p>夹具随测试事务回滚：商户 m 主项目 A、经营项目 B（merchant_projects），B 门店库房与库存行记 B；B 门店员工报损、盘点录差异，
 * m 挂在主项目 A 的主管复核。修复前补写的流水记 A，盘盈纠账还把 B 的库存行改记成 A。</p>
 *
 * <p>第十八批 U 段第 170 条 170-R4 借用同一夹具，按蛇形键走真实入口核对契约改写所依据的代码事实：人工例外取酒条件字段名、库存调整类型闭集、通用入库拒收采购来源。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class InventoryCrossProjectReachJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private EvidenceEventService evidence;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_g82r6_a_" + suffix;
    private final String projectB = "prj_g82r6_b_" + suffix;
    private final String merchant = "mch_g82r6_" + suffix;
    private final String item = "item_g82r6_" + suffix;
    private final String warehouseB = "wh_g82r6_b_" + suffix;

    @BeforeEach
    void seed() {
        String entity = "ent_g82r6_" + suffix;
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())", project, "G82R6 " + project);
        }
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'门店主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, projectA);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())", merchant, merchant, entity, projectA);
        jdbc.update("INSERT INTO merchant_projects(merchant_id,project_id) VALUES(?,?)", merchant, projectB);
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())", "ct_g82r6_" + suffix, merchant, entity, entity);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'跨项目库存酒水','liquor','瓶',0,'active',?,now(),now())", item, merchant);
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'bar','B 门店吧台仓','active',?)", warehouseB, projectB, merchant);
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,warehouse_id,updated_at) VALUES(?,?,?,10,0,10,?,?,?,now())",
                "bal_g82r6_" + suffix, item, warehouseB, merchant, projectB, warehouseB);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // B 门店的高价值报损（照片＋视频）由挂在 A 的主管经复核接口批准：补写的报损流水记 B（修复前记复核人本人项目 A）。
    @Test
    void highValueApprovalBooksTheMovementToTheStockRowProject() throws Exception {
        arrival(600_000L);
        String reporter = "usr_g82r6_rep_" + suffix;
        RequestPostProcessor reporterSession = staff("rep", projectB, "'inventory.stock.move','inventory.stock.adjust','evidence.attachment.create'");
        String photo = upload(reporter, "evidence.jpg", "image/jpeg", "photo");
        String video = upload(reporter, "evidence.mp4", "video/mp4", "video");
        call(reporterSession, "/api/v1/inventory/stock-movements/damage",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"operator_note\":\"碰落破损\",\"from_location_id\":\"" + warehouseB
                        + "\",\"items\":[{\"item_id\":\"" + item + "\",\"quantity\":3}],\"photo_attachment_ids\":[\"" + photo
                        + "\"],\"video_attachment_ids\":[\"" + video + "\"]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_review"));
        String review = jdbc.queryForObject("SELECT id FROM inventory_high_value_reviews WHERE merchant_id=?", String.class, merchant);

        RequestPostProcessor supervisorAtA = staff("sup", projectA, "'inventory.high_value.review'");
        call(supervisorAtA, "/api/v1/inventory/high-value-reviews/" + review + "/review",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"decision\":\"approved\",\"reason\":\"核对无误\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.review_status").value("approved"));

        assertThat(jdbc.queryForObject("SELECT project_id FROM inventory_stock_movements WHERE item_id=? AND movement_type='damage'"
                + " AND business_type='inventory_stock_adjustment'", String.class, item)).isEqualTo(projectB);
    }

    // B 门店库房的盘点差异（盘盈）由挂在 A 的主管复核：库存行与 count_correction 流水都记 B（修复前库存行被改成 A、流水记 A）。
    @Test
    void varianceReviewBooksBalanceAndMovementToTheWarehouseProject() throws Exception {
        arrival(1_000L);
        RequestPostProcessor counter = staff("cnt", projectB, "'inventory.stocktake.create','inventory.stocktake.variance_record'");
        String stocktake = data(call(counter, "/api/v1/inventory/stocktakes",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"warehouse_id\":\"" + warehouseB + "\",\"remark\":\"月末盘点\"}")
                .andExpect(status().isOk())).path("id").asText();
        String variance = data(call(counter, "/api/v1/inventory/stocktakes/" + stocktake + "/variances",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"item_id\":\"" + item + "\",\"counted_qty\":12,\"reason\":\"overage\"}")
                .andExpect(status().isOk())).path("id").asText();

        RequestPostProcessor supervisorAtA = staff("vr", projectA, "'inventory.stocktake.variance_review'");
        call(supervisorAtA, "/api/v1/inventory/stocktakes/" + stocktake + "/variances/" + variance + "/review",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"decision\":\"approved\",\"reason\":\"复盘一致\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.review_status").value("approved"));

        assertThat(jdbc.queryForMap("SELECT available_quantity, project_id FROM inventory_stock_balances WHERE item_id=? AND location_id=?",
                item, warehouseB)).containsEntry("available_quantity", 12).containsEntry("project_id", projectB);
        assertThat(jdbc.queryForObject("SELECT project_id FROM inventory_stock_movements WHERE item_id=? AND movement_type='count_correction'",
                String.class, item)).isEqualTo(projectB);
    }

    // 第十八批 U 段第 170 条 170-R4 ⑥：契约字段名对齐全局蛇形命名实际读取的 orders_within1m、orders_within3m（最高权限本人）；
    // 按旧名 orders_within_1m 发的值不被读，判空返回 400 VALIDATION_FAILED。
    @Test
    void manualPickupPolicyReadsConditionKeysWithoutUnderscoreBeforeDigit() throws Exception {
        String body = "{\"idempotency_key\":\"%s\",\"project_id\":\"" + projectA + "\",\"%s\":2,\"%s\":3,"
                + "\"orders_total\":5,\"min_orders\":1,\"max_orders\":10,\"reason\":\"首次配置\"}";
        mockMvc.perform(put("/api/v1/inventory/saved-bottles/manual-pickup-policy").with(master()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(UUID.randomUUID(), "orders_within1m", "orders_within3m")))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT orders_within_1m, orders_within_3m FROM saved_bottle_manual_pickup_policy WHERE project_id=?", projectA))
                .containsEntry("orders_within_1m", 2).containsEntry("orders_within_3m", 3);

        mockMvc.perform(put("/api/v1/inventory/saved-bottles/manual-pickup-policy").with(master()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(UUID.randomUUID(), "orders_within_1m", "orders_within_3m")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    // 170-R4 ⑩：库存调整类型按代码闭集 gain/loss/damage/waste/count_correction，契约旧值 audit 被拒。
    @Test
    void stockAdjustmentRejectsAdjustmentTypeOutsideCodeSet() throws Exception {
        RequestPostProcessor keeper = staff("adj", projectB, "'inventory.stock.adjust'");
        call(keeper, "/api/v1/inventory/stock-adjustments",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"adjustment_type\":\"audit\",\"reason\":\"盘点\",\"items\":[{\"item_id\":\""
                        + item + "\",\"location_id\":\"" + warehouseB + "\",\"quantity\":1}]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("INVALID_ADJUSTMENT_TYPE"));
    }

    // 170-R4 ⑪：通用入库不收采购来源（须走采购申请收货），期初来源照常入库。
    @Test
    void stockInRejectsPurchaseSourceAndAcceptsInitial() throws Exception {
        RequestPostProcessor keeper = staff("in", projectB, "'inventory.stock.move'");
        String body = "{\"idempotency_key\":\"%s\",\"source_type\":\"%s\",\"warehouse_id\":\"" + warehouseB
                + "\",\"items\":[{\"item_id\":\"" + item + "\",\"quantity\":1}]}";
        call(keeper, "/api/v1/inventory/stock-in", body.formatted(UUID.randomUUID(), "purchase"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("STOCK_IN_PURCHASE_BYPASS_FORBIDDEN"));
        call(keeper, "/api/v1/inventory/stock-in", body.formatted(UUID.randomUUID(), "initial"))
                .andExpect(status().isOk());
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────

    /** 采购到货流水（与 PurchaseRequestService.receive 写入的行同形）：高价值判定只看最近一次实际到货价。 */
    private void arrival(long unitCostCent) {
        inventoryRepository.saveStockMovement(new StockMovementRecord("mv_arr_g82r6_" + suffix, "inbound", item, null, warehouseB, 1,
                unitCostCent, null, "purchase_request", "pr_" + item, null, null, List.of(), "completed",
                Instant.now().minusSeconds(3600), merchant), projectB);
    }

    /** 商户 m 挂在给定项目的员工（普通角色），返回其后台 Web 会话。 */
    private RequestPostProcessor staff(String tag, String project, String permissions) {
        String userId = "usr_g82r6_" + tag + "_" + suffix;
        String role = "role_g82r6_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'G82R6','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(" + permissions + ")", role);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())", userId, userId, project, merchant);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 最高权限本人的后台 Web 会话。 */
    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                projectA, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String upload(String userId, String name, String contentType, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", projectB, merchant, "active"), "test", List.of()));
        try {
            return evidence.uploadAttachment(new MockMultipartFile("file", name, contentType, "fake-bytes".getBytes()),
                    "inventory_stock_adjustment", null, role).id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private ResultActions call(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
    }
}
