package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
 * 第十八批 C 段第 17 条（真库）：BSM 第 1065 行 / ENUM 第 106 行「配有出酒量采集设备的吧台，实际出酒量必须由设备采集；未配备采集设备的吧台，
 * 必须按标准出酒量配酒，也按该标准计算扣减与标准损耗，不得按零损耗记账」；第十八批裁决 4「标准损耗＝标准出酒量×本酒吧损耗率阈值」。
 * 吧台＝库房分类「吧台库存」（ENUM 第 160 行）：按杯商品出酒所在吧台＝其扣库库存行所挂库房。损耗率阈值、出品与吧台设备标记都按后台页面
 * 真实请求形状（蛇形键）经控制器与真实授权链提交。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarPourStandardLossJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcBarRepository barRepository;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "pour_prj_" + s;
    private final String bar = "pour_bar_" + s;
    private final String counter = "pour_counter_" + s;
    private final String barStore = "pour_store_" + s;
    private final String item = "pour_item_" + s;
    private final String location = "pour_loc_" + s;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','出酒综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'出酒酒吧','bar','active',?,now(),now())",
                bar, project);
        // 本酒吧损耗率阈值 10%（值与上下限同配），按后台「组织与商户」酒吧商户行「损耗率阈值」的请求形状。
        mockMvc.perform(put("/api/v1/platform/merchants/" + bar + "/pour-loss-rate").with(staff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pour-loss-" + s + "\",\"value_pct\":10,\"min_pct\":5,\"max_pct\":20}"))
                .andExpect(status().isOk());
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '一号吧台')", counter, project, bar);
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar', '酒吧库')", barStore, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at) VALUES (?, '威士忌', 'liquor', 'ml', 'active', now(), now())", item);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id)"
                + " VALUES (?, ?, ?, 100, 0, 100, now(), ?, ?)", "pour_bal_" + s, item, location, bar, counter);
        Instant now = Instant.now();
        barRepository.saveTable(new BarTableRecord("pour_table_" + s, bar, bar, "T-POUR-" + s, "吧台位", "bar_counter", 4, 0, 0L, "CNY", "active", now, now));
        barRepository.saveProduct(new BarProductRecord("pour_product_" + s, bar, bar, "杯卖威士忌", "spirit", 5_000L, "CNY", item, location, true,
                "active", now, now, 25, "ml", null, false, null, null, null));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void counterWithoutPourDeviceUsesStandardVolumeAndStandardLossEquippedCounterRequiresActual() throws Exception {
        String first = order("a");
        // 吧台未配采集设备：不传实际出酒量照常出品，按标准 25ml×2 杯配酒，标准损耗＝50×10%＝5ml。
        deliver(first).andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT actual_pour_volume, loss_volume FROM bar_order_items WHERE id = ?", "pour_item_a_" + s))
                .containsEntry("actual_pour_volume", 50).containsEntry("loss_volume", 5);

        // 吧台标为配有采集设备后，出品必须带设备采集的实际出酒量。
        pourDevice(counter, true).andExpect(status().isOk()).andExpect(jsonPath("$.data.pour_device_equipped").value(true));
        String second = order("b");
        deliver(second).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("POUR_VOLUME_REQUIRED"));

        // 只有吧台库存才谈得上有无采集设备。
        pourDevice(barStore, true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("WAREHOUSE_NOT_BAR_COUNTER"));
    }

    // ---------- helpers ----------

    private String order(String tag) {
        Instant now = Instant.now();
        String order = "pour_order_" + tag + "_" + s;
        barRepository.saveOrder(new BarOrderRecord(order, bar, bar, "pour_table_" + s, null, null, "staff", "created", 10_000L, "CNY",
                "web", null, null, now, now, "standard", null));
        barRepository.saveOrderItem(new BarOrderItemRecord("pour_item_" + tag + "_" + s, order, "pour_product_" + s, "杯卖威士忌", 2, 5_000L, 10_000L,
                "pending", now, now, null, 0L, false, null, null, null, false, false, null));
        return order;
    }

    /** 后台订单页「标记送达」请求（BarOrdersPage：operator_note、delivery_photo_attachment_ids、delivered_items）经出品接口提交；不带实际出酒量。 */
    private ResultActions deliver(String order) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/deliver").with(staff())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"pour-deliver-%s","operator_note":"已送达","delivery_photo_attachment_ids":[],
                         "delivered_items":[{"order_item_id":"%s","quantity":2}]}
                        """.formatted(order, order.replace("pour_order_", "pour_item_"))));
    }

    private ResultActions pourDevice(String warehouse, boolean equipped) throws Exception {
        return mockMvc.perform(put("/api/v1/inventory/warehouses/" + warehouse + "/pour-device").with(staff())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"pour-device-" + warehouse + "\",\"pour_device_equipped\":" + equipped + "}"));
    }

    /** 真实授权链：平台员工＋商户资料管理（损耗率阈值）、出品（bar.order.create）与库房管理权限码。 */
    private RequestPostProcessor staff() {
        String userId = "pour_mgr_" + s;
        if (jdbc.queryForObject("SELECT count(*) FROM access_users WHERE id = ?", Integer.class, userId) == 0) {
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                    + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", userId, userId);
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '吧台设备回归', 'platform', 'active', now())", role, role);
            for (String code : List.of("merchant.profile.manage", "bar.order.create", "inventory.warehouse.manage")) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
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
