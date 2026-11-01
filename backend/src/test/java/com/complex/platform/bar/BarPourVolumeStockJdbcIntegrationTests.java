package com.complex.platform.bar;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 第十八批 I 段第 76 条（真库）：BSM 第 1065 行 / ENUM 第 106 行「按杯按标准出酒量扣库存、超出记损耗……未配备采集设备的吧台，
 * 必须按标准出酒量配酒，也按该标准计算扣减与标准损耗，不得按零损耗记账」；BSM 第 1168 行「按杯消耗按标准容量扣减，超出部分进入损耗。」；
 * ENUM 第 79 行「定价、库存、结算、对账一律按商品规格逐项进行」；BSM 第 1068 行「出品后退品按是否回库处理」。
 * 库存按规格记瓶数、已开瓶余量按毫升记在余额行，开新瓶才减瓶数；出酒量与损耗量记在同一条流水上。
 * 物料、商品、出品、退品与驳回都按蛇形键经控制器与真实授权链提交。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarPourVolumeStockJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcBarRepository barRepository;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "pvs_prj_" + s;
    private final String bar = "pvs_bar_" + s;
    private final String counter = "pvs_counter_" + s;
    private final String item = "pvs_item_" + s;
    private final String location = "pvs_loc_" + s;
    private final String balance = "pvs_bal_" + s;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','容量扣库综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'容量扣库酒吧','bar','active',?,now(),now())",
                bar, project);
        // 本酒吧损耗率阈值 10%（值与上下限同配），按后台「组织与商户」酒吧商户行「损耗率阈值」的请求形状。
        mockMvc.perform(put("/api/v1/platform/merchants/" + bar + "/pour-loss-rate").with(staff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pvs-loss-" + s + "\",\"value_pct\":10,\"min_pct\":5,\"max_pct\":20}"))
                .andExpect(status().isOk());
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '一号吧台')", counter, project, bar);
        // 某某威士忌 700 毫升瓶装：吧台库存 10 瓶，没有已开瓶。
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, spec_volume_ml, created_at, updated_at)"
                + " VALUES (?, '威士忌 700 毫升瓶装', 'liquor', '瓶', 'active', ?, 700, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?, ?)", balance, item, location, bar, counter);
        Instant now = Instant.now();
        barRepository.saveTable(new BarTableRecord("pvs_table_" + s, bar, bar, "T-PVS-" + s, "吧台位", "bar_counter", 4, 0, 0L, "CNY", "active", now, now));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** 76-R1：物料登记规格容量（毫升），0 不收；手工标记高价值、改安全库存都不清空容量。 */
    @Test
    void inventoryItemSpecVolumeIsRecordedValidatedAndKeptBySafetyStockAndHighValueMark() throws Exception {
        String created = json.readTree(mockMvc.perform(post("/api/v1/inventory/items").with(staff()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pvs-item-" + s + "\",\"item_name\":\"某某威士忌 700 毫升瓶装\",\"category\":\"liquor\","
                                + "\"unit\":\"瓶\",\"min_quantity\":0,\"spec_volume_ml\":700}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spec_volume_ml").value(700))
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/inventory/items").with(staff()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pvs-item-zero-" + s + "\",\"item_name\":\"零容量\",\"category\":\"liquor\","
                                + "\"unit\":\"瓶\",\"min_quantity\":0,\"spec_volume_ml\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/inventory/items/" + created + "/safety-stock").with(staff()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pvs-safety-" + s + "\",\"min_quantity\":6}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/inventory/items/" + created + "/high-value-mark").with(staff()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pvs-mark-" + s + "\",\"manual_high_value\":true,\"reason\":\"进口名酒\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/inventory/items").with(staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + created + "')].spec_volume_ml").value(700))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + created + "')].min_quantity").value(6));
    }

    /**
     * 76-R2：按杯出品按标准出酒量连同损耗一起按容量扣：未配设备 25ml×2 杯＋标准损耗 5ml，先开一瓶（10→9 瓶、余 645ml），
     * 再出 2 杯只动余量（瓶数不变、流水瓶数 0）；吧台配设备后实采 60ml 记损耗 10ml，实采 40ml 仍按标准扣 50ml、损耗 0。
     */
    @Test
    void byGlassDeliveryDeductsStandardVolumePlusLossOpeningBottlesOnlyWhenNeeded() throws Exception {
        String product = product("glass", true);

        deliver(order("a", product), null).andExpect(status().isOk());
        assertThat(balanceRow()).containsEntry("quantity", 9).containsEntry("available_quantity", 9).containsEntry("opened_volume_ml", 645);
        assertThat(movement("a")).containsEntry("movement_type", "outbound").containsEntry("quantity", 1)
                .containsEntry("volume_ml", 50).containsEntry("loss_volume_ml", 5);

        deliver(order("b", product), null).andExpect(status().isOk());
        assertThat(balanceRow()).containsEntry("quantity", 9).containsEntry("opened_volume_ml", 590);
        assertThat(movement("b")).containsEntry("quantity", 0).containsEntry("volume_ml", 50).containsEntry("loss_volume_ml", 5);

        mockMvc.perform(put("/api/v1/inventory/warehouses/" + counter + "/pour-device").with(staff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pvs-device-" + s + "\",\"pour_device_equipped\":true}"))
                .andExpect(status().isOk());
        deliver(order("c", product), 60).andExpect(status().isOk());
        assertThat(balanceRow()).containsEntry("quantity", 9).containsEntry("opened_volume_ml", 530);
        assertThat(movement("c")).containsEntry("quantity", 0).containsEntry("volume_ml", 50).containsEntry("loss_volume_ml", 10);

        deliver(order("d", product), 40).andExpect(status().isOk());
        assertThat(balanceRow()).containsEntry("quantity", 9).containsEntry("opened_volume_ml", 480);
        assertThat(movement("d")).containsEntry("quantity", 0).containsEntry("volume_ml", 50).containsEntry("loss_volume_ml", 0);
    }

    /** 76-R2：没有整瓶可开、余量又不够时拒绝出品，余额与订单明细都不动；按瓶商品照旧按件数扣整瓶。 */
    @Test
    void byGlassDeliveryWithoutEnoughVolumeIsRejectedWhileByBottleStillDeductsWholeBottles() throws Exception {
        String glass = product("glass", true);
        jdbc.update("UPDATE inventory_stock_balances SET quantity = 0, available_quantity = 0, opened_volume_ml = 20 WHERE id = ?", balance);
        deliver(order("short", glass), null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("STOCK_INSUFFICIENT"));
        assertThat(balanceRow()).containsEntry("quantity", 0).containsEntry("available_quantity", 0).containsEntry("opened_volume_ml", 20);
        assertThat(jdbc.queryForObject("SELECT delivery_status FROM bar_order_items WHERE id = ?", String.class, "pvs_oi_short_" + s))
                .isEqualTo("pending");

        jdbc.update("UPDATE inventory_stock_balances SET quantity = 10, available_quantity = 10, opened_volume_ml = 0 WHERE id = ?", balance);
        String bottle = product("bottle", false);
        deliver(order("bottle", bottle), null).andExpect(status().isOk());
        assertThat(balanceRow()).containsEntry("quantity", 8).containsEntry("opened_volume_ml", 0);
        assertThat(movement("bottle")).containsEntry("quantity", 2).containsEntry("volume_ml", null).containsEntry("loss_volume_ml", null);
    }

    /** 76-R3：按杯退品回库只加回标准×杯数到已开瓶余量（损耗不退、瓶数不变）；驳回退品按同量扣回。 */
    @Test
    void byGlassReturnToStockAddsBackOpenedVolumeAndRejectionDeductsItAgain() throws Exception {
        // 已出品退酒按物料最近到货价判高价值（V350）：到货价低于平台阈值＝低价值，退品即时生效，驳回再对称冲销。
        jdbc.update("INSERT INTO inventory_stock_movements (id, movement_type, item_id, target_location_id, quantity, business_type, status,"
                + " created_at, merchant_id, project_id, unit_cost_cent, total_cost_cent) VALUES (?, 'inbound', ?, ?, 10, 'purchase_request', 'completed',"
                + " now() - interval '1 day', ?, ?, 10000, 100000)", "pvs_arrival_" + s, item, location, bar, project);
        String product = product("glass", true);
        String order = order("void", product);
        deliver(order, null).andExpect(status().isOk());
        assertThat(balanceRow()).containsEntry("quantity", 9).containsEntry("opened_volume_ml", 645);

        String voidId = json.readTree(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/void-item").with(staff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pvs-void-" + s + "\",\"order_item_id\":\"pvs_oi_void_" + s + "\",\"reason\":\"客人不要了\","
                                + "\"evidence_attachment_ids\":[],\"return_to_stock\":true,\"operator_note\":\"退菜退酒\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approval_mode").value("immediate"))
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asText();
        assertThat(balanceRow()).containsEntry("quantity", 9).containsEntry("opened_volume_ml", 695);
        assertThat(jdbc.queryForMap("SELECT movement_type, quantity, volume_ml FROM inventory_stock_movements WHERE business_type = 'bar_order_item_void' AND business_id = ?",
                "pvs_oi_void_" + s)).containsEntry("movement_type", "inbound").containsEntry("quantity", 0).containsEntry("volume_ml", 50);

        mockMvc.perform(post("/api/v1/bar/order-item-voids/" + voidId + "/decision").with(decider())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pvs-reject-" + s + "\",\"decision\":\"rejected\"}"))
                .andExpect(status().isOk());
        assertThat(balanceRow()).containsEntry("quantity", 9).containsEntry("opened_volume_ml", 645);
        assertThat(jdbc.queryForMap("SELECT movement_type, quantity, volume_ml FROM inventory_stock_movements WHERE business_type = 'bar_order_item_void_reject' AND business_id = ?",
                "pvs_oi_void_" + s)).containsEntry("movement_type", "outbound").containsEntry("quantity", 0).containsEntry("volume_ml", 50);
    }

    /** 76-R5：按杯扣库存的商品挂没登记规格容量的物料，建商品 400 VALIDATION_FAILED，库中无该商品。 */
    @Test
    void byGlassStockDeductedProductOnItemWithoutSpecVolumeIsRejected() throws Exception {
        jdbc.update("UPDATE inventory_items SET spec_volume_ml = NULL WHERE id = ?", item);
        mockMvc.perform(post("/api/v1/bar/products").with(staff()).contentType(MediaType.APPLICATION_JSON).content(productBody("nospec", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_products WHERE bar_id = ?", Integer.class, bar)).isZero();
    }

    /**
     * 第十八批 K 段 97-R1／97-R2（BSM:1163「管理酒类、吧台消耗、客人存酒和常用消耗品」；BSM:1065「按杯按标准出酒量扣库存、超出记损耗」）：
     * 按后台库存台账「新建库存商品规格」与商品管理「库存与出酒量」两页经客户端发出的蛇形键建物料与商品；本酒吧未配损耗率阈值按出厂 5%、
     * 吧台未配设备：出品 2 杯 25ml 开一瓶（10→9 瓶），余 700−50−标准损耗 3＝647ml。
     */
    @Test
    void itemAndProductCreatedWithAdminPageKeysDeductStandardPourAndFactoryLoss() throws Exception {
        jdbc.update("UPDATE merchants SET pour_loss_rate_pct = NULL, pour_loss_rate_min_pct = NULL, pour_loss_rate_max_pct = NULL WHERE id = ?", bar);
        String pageItem = json.readTree(mockMvc.perform(post("/api/v1/inventory/items").with(staff()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pvs-page-item-" + s + "\",\"item_name\":\"某某威士忌 700 毫升瓶装\",\"category\":\"酒类\","
                                + "\"unit\":\"瓶\",\"min_quantity\":null,\"spec_volume_ml\":700}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spec_volume_ml").value(700))
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asText();
        String pageBalance = "pvs_page_bal_" + s;
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?, ?)", pageBalance, pageItem, location, bar, counter);
        String product = json.readTree(mockMvc.perform(post("/api/v1/bar/products").with(staff()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pvs-page-product-" + s + "\",\"bar_id\":\"" + bar + "\",\"product_name\":\"某某威士忌（按杯）\","
                                + "\"category\":\"spirit\",\"price_cent\":5800,\"status\":\"active\",\"catalog_item_code\":null,"
                                + "\"stock_deduction_required\":true,\"inventory_item_id\":\"" + pageItem + "\",\"stock_location_id\":\"" + location + "\","
                                + "\"standard_pour_volume\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock_deduction_required").value(true))
                .andExpect(jsonPath("$.data.standard_pour_volume").value(25))
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asText();

        deliver(order("page", product), null).andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT quantity, opened_volume_ml FROM inventory_stock_balances WHERE id = ?", pageBalance))
                .containsEntry("quantity", 9).containsEntry("opened_volume_ml", 647);
    }

    // ---------- helpers ----------

    /** 酒吧商品按蛇形键建：挂本吧台库存行扣库；按杯的标准出酒量 25ml。 */
    private String product(String tag, boolean byGlass) throws Exception {
        return json.readTree(mockMvc.perform(post("/api/v1/bar/products").with(staff()).contentType(MediaType.APPLICATION_JSON).content(productBody(tag, byGlass)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("id").asText();
    }

    private String productBody(String tag, boolean byGlass) {
        return "{\"idempotency_key\":\"pvs-product-" + tag + "-" + s + "\",\"bar_id\":\"" + bar + "\",\"product_name\":\"威士忌" + tag + "\","
                + "\"category\":\"spirit\",\"price_cent\":5000,\"status\":\"active\",\"inventory_item_id\":\"" + item + "\","
                + "\"stock_location_id\":\"" + location + "\",\"stock_deduction_required\":true"
                + (byGlass ? ",\"standard_pour_volume\":25}" : "}");
    }

    /** 下单不在本条范围：订单与一行 2 份的明细直接落库，出品走接口。 */
    private String order(String tag, String product) {
        Instant now = Instant.now();
        String order = "pvs_order_" + tag + "_" + s;
        barRepository.saveOrder(new BarOrderRecord(order, bar, bar, "pvs_table_" + s, null, null, "staff", "created", 10_000L, "CNY",
                "web", null, null, now, now, "standard", null));
        barRepository.saveOrderItem(new BarOrderItemRecord("pvs_oi_" + tag + "_" + s, order, product, "威士忌", 2, 5_000L, 10_000L,
                "pending", now, now, null, 0L, false, null, null, null, false, false, null));
        return order;
    }

    /** 后台订单页「登记送达」经客户端转出的键（delivered_items[{order_item_id, quantity}]）；配设备吧台另带实际出酒量。 */
    private ResultActions deliver(String order, Integer actualPourVolume) throws Exception {
        String itemId = order.replace("pvs_order_", "pvs_oi_");
        String pour = actualPourVolume == null ? "" : ",\"actual_pour_volume\":" + actualPourVolume;
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/deliver").with(staff())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"pvs-deliver-%s","operator_note":"已送达","delivery_photo_attachment_ids":[],
                         "delivered_items":[{"order_item_id":"%s","quantity":2%s}]}
                        """.formatted(order, itemId, pour)));
    }

    private Map<String, Object> balanceRow() {
        return jdbc.queryForMap("SELECT quantity, available_quantity, opened_volume_ml FROM inventory_stock_balances WHERE id = ?", balance);
    }

    private Map<String, Object> movement(String tag) {
        return jdbc.queryForMap("SELECT movement_type, quantity, volume_ml, loss_volume_ml FROM inventory_stock_movements"
                + " WHERE business_type = 'bar_order' AND business_id = ?", "pvs_order_" + tag + "_" + s);
    }

    /** 真实授权链：平台员工＋损耗率阈值、库房、物料、商品、出品与退品权限码。 */
    private RequestPostProcessor staff() {
        return actor("pvs_mgr_" + s, List.of("merchant.profile.manage", "inventory.warehouse.manage", "inventory.stock.move", "inventory.stock.view",
                "inventory.high_value.review", "bar.product.manage", "bar.order.create", "bar.order_item.void"));
    }

    /** 退品审批人（审批权限与申请权限分离，且不得是申请人）。 */
    private RequestPostProcessor decider() {
        return actor("pvs_sup_" + s, List.of("bar.order_item.void_decide"));
    }

    private RequestPostProcessor actor(String userId, List<String> codes) {
        if (jdbc.queryForObject("SELECT count(*) FROM access_users WHERE id = ?", Integer.class, userId) == 0) {
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                    + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", userId, userId);
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '按容量扣库回归', 'platform', 'active', now())", role, role);
            for (String code : codes) {
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
