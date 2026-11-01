package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 K 段第 97 条 97-R3（真库）：BSM 第 1168 行「记录吧台售卖后的酒水消耗，支持按杯、按瓶、按套餐或其他形式统计；按杯消耗按标准容量扣减，
 * 超出部分进入损耗。」；BSM 第 1065 行「按杯、按瓶、套餐」；BSM 第 1068 行「出品后退品按是否回库处理」；ENUM 第 187 行「全系统的钱账统一按「营业日」归集」。
 * 商品、套餐、下单、出品、退品与统计都按蛇形键经控制器与真实授权链提交；统计期间参数同库存台账本月出库成本卡（period_start、period_end）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarConsumptionStatsJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcBarRepository barRepository;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "bcs_prj_" + s;
    private final String farProject = "bcs_prj_far_" + s;
    private final String bar = "bcs_bar_" + s;
    private final String otherBar = "bcs_bar_other_" + s;
    private final String farBar = "bcs_bar_far_" + s;
    private final String counter = "bcs_counter_" + s;
    private final String item = "bcs_item_" + s;
    private final String location = "bcs_loc_" + s;
    private final String table = "bcs_table_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','吧台消耗综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','另一综合体','active',now(),now())", farProject);
        // 本酒吧没配损耗率阈值，按出厂 5%；吧台没配出酒量采集设备，按标准出酒量配酒。
        merchant(bar, "吧台消耗酒吧", project);
        merchant(otherBar, "同综合体另一酒吧", project);
        merchant(farBar, "另一综合体酒吧", farProject);
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '一号吧台')", counter, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, spec_volume_ml, created_at, updated_at)"
                + " VALUES (?, '某某威士忌 700 毫升瓶装', '酒类', '瓶', 'active', ?, 700, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?, ?)", "bcs_bal_" + s, item, location, bar, counter);
        // 已出品退酒按物料最近到货价判高价值（V350）：到货价低于平台阈值＝低价值，退品即时生效。
        jdbc.update("INSERT INTO inventory_stock_movements (id, movement_type, item_id, target_location_id, quantity, business_type, status,"
                + " created_at, merchant_id, project_id, unit_cost_cent, total_cost_cent) VALUES (?, 'inbound', ?, ?, 10, 'purchase_request', 'completed',"
                + " now() - interval '1 day', ?, ?, 10000, 100000)", "bcs_arrival_" + s, item, location, bar, project);
        Instant now = Instant.now();
        barRepository.saveTable(new BarTableRecord(table, bar, bar, "T-BCS-" + s, "吧台位", "bar_counter", 4, 0, 0L, "CNY", "active", now, now));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 按杯 2 杯＋1 杯（后者退品回库）、按瓶 1 瓶、2 份含 1 瓶的套餐、另送 1 杯：按杯行杯数 2、出酒量 50、损耗 3＋1（损耗不退）；按瓶 1；
     * 套餐行带套餐编号与名称、件数 2；赠送不计。别的酒吧、别的综合体的员工看不到；结束日期早于开始日期 400。出品后套餐来源仍在。
     */
    @Test
    void consumptionIsCountedByGlassByBottleAndComboWithoutGiftsAndReturnedStock() throws Exception {
        String glass = product("glass", "某某威士忌（按杯）", true);
        String bottle = product("bottle", "某某红酒（整瓶）", false);
        String combo = data(mockMvc.perform(post("/api/v1/bar/catalog/combos").with(barStaff()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bar_id\":\"" + bar + "\",\"combo_name\":\"整瓶畅饮套餐\",\"price_cent\":30000,\"status\":\"active\","
                        + "\"items\":[{\"product_id\":\"" + bottle + "\",\"quantity\":1}]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("id").asText();
        String order = data(mockMvc.perform(post("/api/v1/bar/orders").with(barStaff()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"bcs-order-" + s + "\",\"bar_id\":\"" + bar + "\",\"table_id\":\"" + table + "\",\"items\":["
                        + "{\"product_id\":\"" + glass + "\",\"quantity\":2},{\"product_id\":\"" + glass + "\",\"quantity\":1},"
                        + "{\"product_id\":\"" + bottle + "\",\"quantity\":1},{\"combo_id\":\"" + combo + "\",\"quantity\":2},"
                        + "{\"product_id\":\"" + glass + "\",\"quantity\":1,\"is_gift\":true}]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("id").asText();

        // 后台订单页「登记送达」经客户端转出的键：delivered_items[{order_item_id, quantity}]，整单一次送达。
        List<String> delivered = new ArrayList<>();
        for (Map<String, Object> line : jdbc.queryForList("SELECT id, quantity FROM bar_order_items WHERE order_id = ? ORDER BY id", order)) {
            delivered.add("{\"order_item_id\":\"" + line.get("id") + "\",\"quantity\":" + line.get("quantity") + "}");
        }
        assertThat(delivered).hasSize(5);
        mockMvc.perform(post("/api/v1/bar/orders/" + order + "/deliver").with(barStaff()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"bcs-deliver-" + s + "\",\"operator_note\":\"已送达\",\"delivery_photo_attachment_ids\":[],"
                                + "\"delivered_items\":[" + String.join(",", delivered) + "]}"))
                .andExpect(status().isOk());
        String oneGlass = jdbc.queryForObject("SELECT id FROM bar_order_items WHERE order_id = ? AND product_id = ? AND quantity = 1 AND is_gift = FALSE",
                String.class, order, glass);
        mockMvc.perform(post("/api/v1/bar/orders/" + order + "/void-item").with(platform()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"bcs-void-" + s + "\",\"order_item_id\":\"" + oneGlass + "\",\"reason\":\"客人不要了\","
                                + "\"evidence_attachment_ids\":[],\"return_to_stock\":true,\"operator_note\":\"退菜退酒\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approval_mode").value("immediate"));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        String period = "period_start=" + today.minusDays(1) + "&period_end=" + today.plusDays(1);
        List<JsonNode> rows = stats(platform(), period);
        assertThat(rows).extracting(row -> row.path("sale_form").asText()).containsExactlyInAnyOrder("by_glass", "by_bottle", "combo");
        JsonNode byGlass = row(rows, "by_glass");
        assertThat(byGlass.path("bar_name").asText()).isEqualTo("吧台消耗酒吧");
        assertThat(byGlass.path("product_id").asText()).isEqualTo(glass);
        assertThat(byGlass.path("inventory_item_id").asText()).isEqualTo(item);
        assertThat(byGlass.path("item_name").asText()).isEqualTo("某某威士忌 700 毫升瓶装");
        assertThat(byGlass.path("quantity").asLong()).isEqualTo(2);
        assertThat(byGlass.path("pour_volume_ml").asLong()).isEqualTo(50);
        assertThat(byGlass.path("loss_volume_ml").asLong()).isEqualTo(4);
        JsonNode byBottle = row(rows, "by_bottle");
        assertThat(byBottle.path("product_id").asText()).isEqualTo(bottle);
        assertThat(byBottle.path("quantity").asLong()).isEqualTo(1);
        assertThat(byBottle.path("combo_id").isNull()).isTrue();
        JsonNode comboRow = row(rows, "combo");
        assertThat(comboRow.path("combo_id").asText()).isEqualTo(combo);
        assertThat(comboRow.path("combo_name").asText()).isEqualTo("整瓶畅饮套餐");
        assertThat(comboRow.path("product_id").asText()).isEqualTo(bottle);
        assertThat(comboRow.path("quantity").asLong()).isEqualTo(2);

        assertThat(stats(barStaff(), period)).hasSize(3);
        assertThat(stats(staff("other", otherBar, project), period)).isEmpty();
        assertThat(stats(staff("far", farBar, farProject), period)).isEmpty();
        mockMvc.perform(get("/api/v1/inventory/bar-consumption-stats?period_start=" + today + "&period_end=" + today.minusDays(1)).with(platform()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        assertThat(jdbc.queryForList("SELECT combo_id FROM bar_order_items WHERE order_id = ? AND delivery_status = 'delivered' AND combo_id IS NOT NULL",
                String.class, order)).containsExactly(combo);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_order_items WHERE order_id = ? AND combo_id IS NULL", Integer.class, order)).isEqualTo(4);
    }

    // ---------- helpers ----------

    private void merchant(String id, String name, String projectId) {
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                id, name, projectId);
    }

    /** 后台商品管理「新建商品」按蛇形键建：按杯的挂吧台库存行扣库、标准出酒量 25ml；按瓶的不扣库。 */
    private String product(String tag, String name, boolean byGlass) throws Exception {
        String body = "{\"idempotency_key\":\"bcs-product-" + tag + "-" + s + "\",\"bar_id\":\"" + bar + "\",\"product_name\":\"" + name + "\","
                + "\"category\":\"spirit\",\"price_cent\":5000,\"status\":\"active\",\"catalog_item_code\":null"
                + (byGlass ? ",\"stock_deduction_required\":true,\"inventory_item_id\":\"" + item + "\",\"stock_location_id\":\"" + location
                        + "\",\"standard_pour_volume\":25}" : ",\"stock_deduction_required\":false}");
        return data(mockMvc.perform(post("/api/v1/bar/products").with(barStaff()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("id").asText();
    }

    /** 库存台账「吧台消耗统计」区块的请求；只取本酒吧的行（平台管理员看全量，库里还有别的酒吧）。 */
    private List<JsonNode> stats(RequestPostProcessor who, String period) throws Exception {
        JsonNode data = data(mockMvc.perform(get("/api/v1/inventory/bar-consumption-stats?" + period).with(who))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        List<JsonNode> rows = new ArrayList<>();
        data.forEach(row -> {
            if (bar.equals(row.path("bar_id").asText())) rows.add(row);
        });
        return rows;
    }

    private static JsonNode row(List<JsonNode> rows, String saleForm) {
        return rows.stream().filter(row -> saleForm.equals(row.path("sale_form").asText())).findFirst().orElseThrow();
    }

    private JsonNode data(String body) throws Exception {
        return json.readTree(body).path("data");
    }

    /** 本酒吧员工：商品、套餐、点单出品与库存查看权限码（退品由平台管理员办，商户员工退品要先过主管复核）。 */
    private RequestPostProcessor barStaff() {
        return staff("bar", bar, project, "bar.product.manage", "bar.combo.manage", "bar.order.create", "inventory.stock.view");
    }

    private RequestPostProcessor staff(String tag, String merchantId, String projectId, String... extraCodes) {
        String userId = "bcs_" + tag + "_" + s;
        List<String> codes = extraCodes.length == 0 ? List.of("inventory.stock.view") : List.of(extraCodes);
        ensureUser(userId, "staff", projectId, merchantId, codes);
        AccessActor actor = new AccessActor(userId, userId, "staff", projectId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 平台管理员：看全量统计、办退品。 */
    private RequestPostProcessor platform() {
        String userId = "bcs_platform_" + s;
        ensureUser(userId, "platform_admin", null, null, List.of("inventory.stock.view", "bar.order_item.void"));
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", null, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }

    private void ensureUser(String userId, String accountType, String projectId, String merchantId, List<String> codes) {
        if (jdbc.queryForObject("SELECT count(*) FROM access_users WHERE id = ?", Integer.class, userId) > 0) {
            return;
        }
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) VALUES (?, ?, ?, ?, ?, 'active', now())",
                userId, userId, accountType, projectId, merchantId);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '吧台消耗统计回归', ?, 'active', now())",
                role, "吧台消耗统计回归角色" + userId, merchantId == null ? "platform" : "project");
        for (String code : codes) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
    }
}
