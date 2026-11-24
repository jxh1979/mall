package com.complex.platform.inventory;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 L 段 105-R1（真库）：BSM:1162「管理库房和库位，包括总库、酒吧库、吧台库存、消耗品库、礼品库和存酒区。」；ENUM:160 库房分类六类；
 * PGM:948「……库房……都要看商户归属……」。平台员工按后台库存台账「库房」区块的真实请求形状（蛇形键）新建库房，
 * 新库房出现在库房列表，也出现在采购验收接收仓库下拉的来源（采购工作台选项 warehouses）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class WarehouseCreateJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "whc_prj_" + s;
    private final String bar = "whc_bar_" + s;
    private final String projectB = "whc_prjb_" + s;
    private final String barB = "whc_barb_" + s;
    private final String warehouseA = "whc_wha_" + s;
    private final String warehouseB = "whc_whb_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','库房新建综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'库房新建酒吧','bar','active',?,now(),now())",
                bar, project);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','库房新建综合体乙','active',now(),now())", projectB);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'库房新建酒吧乙','bar','active',?,now(),now())",
                barB, projectB);
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'bar','甲酒吧库','active',?)", warehouseA, project, bar);
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'main','乙总库','active',NULL)", warehouseB, projectB);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void platformStaffCreatesWarehouseWithPageShapeAndItIsListedAndOfferedForPurchaseReceiving() throws Exception {
        String body = mockMvc.perform(post("/api/v1/inventory/warehouses").with(platformStaff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"admin-warehouse-" + s + "\",\"project_id\":\"" + project + "\",\"merchant_id\":\"" + bar
                                + "\",\"warehouse_type\":\"bar_counter\",\"name\":\"一号吧台\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = json.readTree(body).path("data").path("id").asText();

        assertThat(jdbc.queryForMap("SELECT project_id, merchant_id, warehouse_type, name, status, pour_device_equipped FROM warehouses WHERE id = ?", id))
                .containsEntry("project_id", project).containsEntry("merchant_id", bar).containsEntry("warehouse_type", "bar_counter")
                .containsEntry("name", "一号吧台").containsEntry("status", "active").containsEntry("pour_device_equipped", false);
        mockMvc.perform(get("/api/v1/inventory/warehouses").with(platformStaff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(id)));
        // 采购验收「接收仓库」下拉的来源（PurchaseWorkflowService.options 的 warehouses）。
        mockMvc.perform(get("/api/v1/inventory/purchase-workbench-options").with(platformStaff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warehouses[*].id", hasItem(id)));
    }

    /**
     * 第十八批 M 段第 113 条 113-R1（PGM:899「平台治理层要保证每个重要动作能追溯……都要能查到」；决策台账 18K 第 94 条补记「代码落 OpenAPI 声明的事件，
     * OpenAPI 不改」）：后台按库存台账真实请求逐个打开库存包六个列表，留痕事件名与 OpenAPI 声明一致（*.list），不再落旧名 *.viewed。
     */
    @Test
    void inventoryListReadsRecordOpenApiDeclaredAuditEventNames() throws Exception {
        for (String path : List.of("warehouses", "stock-batches", "risk-alerts", "supplier-reconciliations", "supplier-qualifications",
                "supplier-settlement-methods")) {
            mockMvc.perform(get("/api/v1/inventory/" + path).with(platformStaff())).andExpect(status().isOk());
        }
        assertThat(jdbc.queryForList("SELECT event_name FROM audit_events WHERE actor_id = ?", String.class, "whc_mgr_" + s))
                .contains("inventory.warehouse.list", "inventory.stock_batch.list", "inventory.risk_alert.list",
                        "inventory.supplier_reconciliation.list", "inventory.supplier_qualification.list",
                        "inventory.supplier_settlement_method.list")
                .noneMatch(name -> name.endsWith(".viewed"));
    }

    /**
     * 第十八批 BZ 段第 391 条 391-R1（BSM:1162「管理库房和库位，包括总库、酒吧库、吧台库存、消耗品库、礼品库和存酒区。」；PGM:659「角色能查看哪些项目，
     * 持有这类角色的员工就能在哪些项目新建记录、把员工改挂过去。」）：不挂商户、挂在甲、经普通角色持库房管理码的平台库管，在甲新建平台库房（归属为空）200；
     * 控制席补定：库房列表按该码可触及的项目列出（含新库房与甲酒吧的库房、不含乙的），并能在新库房下补库位。在乙建 403 CROSS_PROJECT_FORBIDDEN、乙不多落库房。
     * 修复前红：建库房、读库房列表都 403 MERCHANT_SCOPE_REQUIRED。
     */
    @Test
    void unmerchantedStorekeeperCreatesListsAndLocatesWarehousesOnlyInReachableProjects() throws Exception {
        RequestPostProcessor keeper = staff("keeper", project, null, role("plain", null));
        String id = createdId(keeper, "keeper-a", project, null);
        assertThat(jdbc.queryForMap("SELECT project_id, merchant_id FROM warehouses WHERE id = ?", id))
                .containsEntry("project_id", project).containsEntry("merchant_id", null);
        assertThat(listed(keeper)).contains(id, warehouseA).doesNotContain(warehouseB);
        mockMvc.perform(post("/api/v1/inventory/warehouse-locations").with(keeper).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"whc-loc-" + s + "\",\"warehouse_id\":\"" + id + "\",\"location_id\":\"whc_loc_" + s + "\"}"))
                .andExpect(status().isOk());

        create(keeper, "keeper-b", projectB, null).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM warehouses WHERE project_id = ?", Integer.class, projectB)).isOne();
    }

    /**
     * 391-R1（PGM:659「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」）：挂在甲、经只勾乙的可跨项目角色持码的人
     * 在乙建库房 200，库房列表只列乙的；该角色去掉乙后，用同一幂等键重放 403 CROSS_PROJECT_FORBIDDEN（范围闸在幂等之前）。修复前红：建库房 403。
     */
    @Test
    void crossProjectHolderCreatesInTickedProjectAndReplayIsGatedBeforeIdempotency() throws Exception {
        String ticked = role("ticked", projectB);
        RequestPostProcessor holder = staff("holder", project, null, ticked);
        String id = createdId(holder, "holder-b", projectB, null);
        assertThat(listed(holder)).contains(id, warehouseB).doesNotContain(warehouseA);

        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id = ?", ticked);
        create(holder, "holder-b", projectB, null).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    /** 391-R1 回归：商户账号建库房照旧落本商户，请求里带别家商户号也不改（修复前后都绿）。 */
    @Test
    void merchantAccountStillCreatesForItsOwnMerchant() throws Exception {
        RequestPostProcessor store = staff("store", project, bar, role("store", null));
        String id = createdId(store, "store-a", project, barB);
        assertThat(jdbc.queryForObject("SELECT merchant_id FROM warehouses WHERE id = ?", String.class, id)).isEqualTo(bar);
    }

    /** 391-R1（第十八批裁决 71；BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」）：最高权限在乙新建平台库房 200，库房列表列出它。修复前红：403。 */
    @Test
    void masterCreatesPlatformWarehouseInAnyProjectAndSeesIt() throws Exception {
        String id = createdId(master(), "master-b", projectB, null);
        assertThat(jdbc.queryForMap("SELECT project_id, merchant_id FROM warehouses WHERE id = ?", id))
                .containsEntry("project_id", projectB).containsEntry("merchant_id", null);
        assertThat(listed(master())).contains(id, warehouseA, warehouseB);
    }

    /** 按库存台账「库房」区块新建弹窗的请求形状（蛇形键；平台库房 merchant_id 为 null）。 */
    private ResultActions create(RequestPostProcessor who, String key, String projectId, String merchantId) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory/warehouses").with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"whc-" + key + "-" + s + "\",\"project_id\":\"" + projectId + "\",\"merchant_id\":"
                        + (merchantId == null ? "null" : "\"" + merchantId + "\"") + ",\"warehouse_type\":\"main\",\"name\":\"平台总库\"}"));
    }

    private String createdId(RequestPostProcessor who, String key, String projectId, String merchantId) throws Exception {
        String body = create(who, key, projectId, merchantId).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).path("data").path("id").asText();
    }

    private List<String> listed(RequestPostProcessor who) throws Exception {
        String body = mockMvc.perform(get("/api/v1/inventory/warehouses").with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.items[*].id");
    }

    /** 只持库房管理码的角色；tickedProject 不为空时是只勾了该项目的可跨项目角色。 */
    private String role(String tag, String tickedProject) {
        String role = "role_whc_" + tag + "_" + s;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '库房新建回归', 'project', 'active', now())",
                role, role);
        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'inventory.warehouse.manage')", role);
        if (tickedProject != null) {
            jdbc.update("INSERT INTO role_viewable_projects (role_id, viewable_id) VALUES (?, ?)", role, tickedProject);
        }
        return role;
    }

    /** 员工账号（merchantId 为空＝不挂商户），经后台 Web 终端。 */
    private RequestPostProcessor staff(String tag, String organizationId, String merchantId, String role) {
        String userId = "whc_" + tag + "_" + s;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', ?, ?, 'active', now())", userId, userId, organizationId, merchantId);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments (user_id, staff_level_id, organization_id) VALUES (?, 'lvl_m3_upper', ?)", userId, organizationId);
        return web(new AccessActor(userId, userId, "staff", organizationId, merchantId, "active"));
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户）。 */
    private RequestPostProcessor master() {
        return web(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active"));
    }

    private static RequestPostProcessor web(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }

    /** 平台员工：库房管理（列表与新建同一权限码）与采购查看（采购工作台选项）。 */
    private RequestPostProcessor platformStaff() {
        String userId = "whc_mgr_" + s;
        if (jdbc.queryForObject("SELECT count(*) FROM access_users WHERE id = ?", Integer.class, userId) == 0) {
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                    + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", userId, userId);
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '库房新建回归', 'platform', 'active', now())",
                    role, role);
            for (String permission : List.of("inventory.warehouse.manage", "inventory.purchase_request.view", "inventory.stock.view",
                    "inventory.risk_alert.view", "inventory.supplier.reconcile", "inventory.supplier.qualification.manage",
                    "inventory.supplier.settlement.manage")) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, permission);
            }
            jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        }
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", null, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }
}
