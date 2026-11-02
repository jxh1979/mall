package com.complex.platform.inventory;

// 文件版本：V0.01

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 M 段第 119 条 119-R1（真库）：BSM:1936「供应链连基础管理｜因为供应商、供应合同、库房、库位、采购人员、库管和审批权限都要先在基础管理中定义清楚。」；
 * BSM:1162「管理库房和库位……」；PGM:480「供应链库存要知道库房和库位。」平台员工按库存台账「库房」区块的真实请求形状（蛇形键）在库房下建库位，
 * 库位出现在库位列表与采购验收的库位选项（采购工作台选项 locations）里；同一库位编号不能再建到别的库房。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class WarehouseLocationJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "whl_prj_" + s;
    private final String bar = "whl_bar_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','库位建档综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'库位建档酒吧','bar','active',?,now(),now())",
                bar, project);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void platformStaffRegistersLocationUnderWarehouseAndItIsListedAndOfferedForPurchaseReceiving() throws Exception {
        String warehouse = createWarehouse("a", "一号库");
        String location = "A-01-" + s;
        createLocation("a", warehouse, location)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(location))
                .andExpect(jsonPath("$.data.warehouse_id").value(warehouse));
        assertThat(jdbc.queryForObject("SELECT warehouse_id FROM warehouse_locations WHERE id = ?", String.class, location)).isEqualTo(warehouse);
        assertThat(jdbc.queryForList("SELECT event_name FROM audit_events WHERE actor_id = ?", String.class, "whl_mgr_" + s))
                .contains("inventory.warehouse_location.created");

        mockMvc.perform(get("/api/v1/inventory/warehouse-locations").with(platformStaff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(location)));
        // 采购验收「实际库位编号」下拉的来源（PurchaseWorkflowService.options 的 locations）。
        mockMvc.perform(get("/api/v1/inventory/purchase-workbench-options").with(platformStaff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locations[?(@.id == '" + location + "')].warehouse_id", hasItem(warehouse)));

        // 库位编号全平台唯一：同一编号再建到第二个库房 409，原库位仍属第一个库房。
        String second = createWarehouse("b", "二号库");
        createLocation("b", second, location)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("WAREHOUSE_LOCATION_TAKEN"));
        assertThat(jdbc.queryForObject("SELECT warehouse_id FROM warehouse_locations WHERE id = ?", String.class, location)).isEqualTo(warehouse);
    }

    private String createWarehouse(String tag, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/inventory/warehouses").with(platformStaff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"admin-warehouse-" + tag + s + "\",\"project_id\":\"" + project + "\",\"merchant_id\":\"" + bar
                                + "\",\"warehouse_type\":\"main\",\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).path("data").path("id").asText();
    }

    private ResultActions createLocation(String tag, String warehouseId, String locationId) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory/warehouse-locations").with(platformStaff())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"admin-warehouse-location-" + tag + s + "\",\"warehouse_id\":\"" + warehouseId
                        + "\",\"location_id\":\"" + locationId + "\"}"));
    }

    /** 平台员工：库房管理（库房、库位的列表与新建同一权限码）与采购查看（采购工作台选项）。 */
    private RequestPostProcessor platformStaff() {
        String userId = "whl_mgr_" + s;
        if (jdbc.queryForObject("SELECT count(*) FROM access_users WHERE id = ?", Integer.class, userId) == 0) {
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                    + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", userId, userId);
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '库位建档回归', 'platform', 'active', now())",
                    role, "库位建档岗" + s);
            for (String permission : List.of("inventory.warehouse.manage", "inventory.purchase_request.view")) {
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
