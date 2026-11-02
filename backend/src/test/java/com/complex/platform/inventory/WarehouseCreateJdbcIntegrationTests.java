package com.complex.platform.inventory;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','库房新建综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'库房新建酒吧','bar','active',?,now(),now())",
                bar, project);
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
