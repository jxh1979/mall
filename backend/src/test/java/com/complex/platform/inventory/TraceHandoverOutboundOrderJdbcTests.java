package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 K 段第 94 条 94-R1 出库核验订单真库回归（BSM 第 1197 行「出库须同时核验订单和物品码」；第 1620 行「酒水或物品从库房出库、订单核验、
 * 机器人取货、后厨接收、服务员送达客人。」；第 1153 行「每一瓶需要追责的酒从库房到客人的交接过程」，只认按瓶出品的明细）：
 * 库管按溯源码列可出库的酒吧订单，建交接链须带含该物品待出品明细的订单，表头记订单。查单、建链、扫码都按前端蛇形键走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TraceHandoverOutboundOrderJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_tho_" + suffix;
    private final String merchant = "mch_tho_" + suffix;
    private final String otherMerchant = "mch_tho_o_" + suffix;
    private final String item = "itm_tho_" + suffix;
    private final String otherItem = "itm_tho_x_" + suffix;
    private final String code = "TRC-O-" + suffix;
    private RequestPostProcessor clerk;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "出库核验项目 " + suffix);
        for (String m : List.of(merchant, otherMerchant)) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                    + "VALUES (?, ?, 'bar', 'active', now(), now(), ?)", m, "出库核验商户 " + m, project);
            jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, status, created_at, updated_at) "
                    + "VALUES (?, ?, ?, 'K1', ?, 'card_seat', 'available', now(), now())", "tbl_" + m, m, m, "卡座 K1");
        }
        for (String i : List.of(item, otherItem)) {
            jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) "
                    + "VALUES(?,?,'liquor','瓶',0,'active',?,now(),now())", i, "出库核验酒水 " + i, merchant);
        }
        jdbc.update("INSERT INTO inventory_trace_codes (id, trace_code, item_id, merchant_id, status) VALUES (?, ?, ?, ?, 'registered')",
                "trc_tho_" + suffix, code, item, merchant);
        product("bottle", merchant, item, null, "黑方 整瓶");
        product("glass", merchant, item, 30, "黑方 单杯");
        product("other", merchant, otherItem, null, "芝华士 整瓶");
        product("foreign", otherMerchant, item, null, "黑方 整瓶");

        String role = "role_tho_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'出库核验回归','project','active',now())",
                role, "出库核验库管 " + suffix);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions "
                + "WHERE permission_code IN('inventory.handover.scan')", role);
        clerk = clerk("usr_tho_clerk_" + suffix, project);
    }

    /** 只列含该物品按瓶出品、待出品明细的本商户订单；已出品、已作废、别的物品、按杯商品、别的商户的订单都不列。 */
    @Test
    void outboundOrdersListOnlyOwnOrdersWithPendingBottleLinesOfTheCodesItem() throws Exception {
        String match = order("match", merchant, "bottle", 2, "pending");
        order("delivered", merchant, "bottle", 1, "delivered");
        order("voided", merchant, "bottle", 1, "voided");
        order("otheritem", merchant, "other", 1, "pending");
        order("glass", merchant, "glass", 1, "pending");
        order("foreign", otherMerchant, "foreign", 1, "pending");

        String body = mockMvc.perform(get("/api/v1/inventory/trace-codes/" + code + "/outbound-orders").with(clerk))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(body, "$.data[*].order_id")).containsExactly(match);
        assertThat(JsonPath.<String>read(body, "$.data[0].table_name")).isEqualTo("卡座 K1");
        assertThat(JsonPath.<String>read(body, "$.data[0].product_name")).isEqualTo("黑方 整瓶");
        assertThat(JsonPath.<Integer>read(body, "$.data[0].quantity")).isEqualTo(2);
    }

    /**
     * 订单不含该物品 409、表头不落行、留失败审计；不带订单 400；未登记的码 404；订单与码相符 200，表头记订单与码登记的物品，随后可扫库管出库。
     */
    @Test
    void creatingAChainVerifiesTheOrderAgainstTheRegisteredCode() throws Exception {
        String match = order("match", merchant, "bottle", 1, "pending");
        String unrelated = order("otheritem", merchant, "other", 1, "pending");

        String mismatchKey = "tho-c-mis-" + suffix;
        create(mismatchKey, code, unrelated).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("HANDOVER_ORDER_ITEM_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_trace_handovers WHERE trace_code=?", Integer.class, code)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='inventory.handover_create_failed' AND idempotency_key=?",
                Integer.class, mismatchKey)).isEqualTo(1);

        send(post("/api/v1/inventory/trace-handovers"), "{\"idempotency_key\":\"tho-c-noorder-" + suffix + "\",\"trace_code\":\"" + code
                + "\",\"device_id\":\"dev_tho_" + suffix + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        create("tho-c-unreg-" + suffix, "TRC-UNREG-" + suffix, match).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("TRACE_CODE_NOT_FOUND"));

        String created = create("tho-c-ok-" + suffix, code, match).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(created, "$.data.handover.id");
        Map<String, Object> header = jdbc.queryForMap("SELECT order_id, item_id FROM inventory_trace_handovers WHERE id=?", id);
        assertThat(header).containsEntry("order_id", match).containsEntry("item_id", item);
        send(post("/api/v1/inventory/trace-handovers/" + id + "/scan-segment"), "{\"idempotency_key\":\"tho-s-" + suffix
                + "\",\"segment\":\"out_verify\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handover.current_segment").value("kitchen_receive"));
    }

    /**
     * 18K 复核：自营酒吧的库管走公司自有员工路径（organization_id＝经营主体公司、merchant_id＝本酒吧，不等于酒吧的主项目），同样能列订单并建链
     * （BSM 第 1463 行「在商户档案上标注为自营的商户视同公司自有，其员工按公司自有员工路径办理。」）。
     */
    @Test
    void selfOperatedBarClerkUnderTheCompanyListsOrdersAndCreatesTheChain() throws Exception {
        String company = "ent_tho_" + suffix;
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) "
                + "VALUES(?,'自营酒吧经营主体','management_company','active',now(),now())", company);
        jdbc.update("UPDATE merchants SET cooperation_form='self_operated', business_entity_id=? WHERE id=?", company, merchant);
        clerk = clerk("usr_tho_self_" + suffix, company);
        String match = order("match", merchant, "bottle", 1, "pending");

        String body = mockMvc.perform(get("/api/v1/inventory/trace-codes/" + code + "/outbound-orders").with(clerk))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(body, "$.data[*].order_id")).containsExactly(match);
        create("tho-c-self-" + suffix, code, match).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handover.order_id").value(match));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 持 inventory.handover.scan 的库管账号（归属 organizationId、本商户）。 */
    private RequestPostProcessor clerk(String userId, String organizationId) {
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, organizationId, merchant);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, "role_tho_" + suffix);
        AccessActor actor = new AccessActor(userId, userId, "staff", organizationId, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 94-R1 建链前置（其他交接链真库测试共用）：登记溯源码（同一瓶装酒水），本商户一张含该酒水按瓶待出品明细的订单；返回订单号。 */
    static String seedBottleOrder(JdbcTemplate jdbc, String tag, String merchantId, String... codes) {
        String item = "itm_" + tag;
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) "
                + "VALUES(?,'交接酒水','liquor','瓶',0,'active',?,now(),now())", item, merchantId);
        for (String code : codes) {
            jdbc.update("INSERT INTO inventory_trace_codes (id, trace_code, item_id, merchant_id, status) VALUES (?, ?, ?, ?, 'registered')",
                    "trc_" + code, code, item, merchantId);
        }
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'K1', '卡座 K1', 'card_seat', 'available', now(), now())", "tbl_" + tag, merchantId, merchantId);
        jdbc.update("INSERT INTO bar_products (id, bar_id, merchant_id, product_name, product_type, price_cent, inventory_item_id, status, "
                + "created_at, updated_at) VALUES (?, ?, ?, '交接酒水 整瓶', 'liquor', 88800, ?, 'active', now(), now())", "bp_" + tag, merchantId, merchantId, item);
        jdbc.update("INSERT INTO bar_orders (id, bar_id, merchant_id, table_id, order_source, status, total_amount_cent, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, 'staff', 'submitted', 88800, now(), now())", "bo_" + tag, merchantId, merchantId, "tbl_" + tag);
        jdbc.update("INSERT INTO bar_order_items (id, order_id, product_id, product_name, quantity, unit_price_cent, amount_cent, delivery_status, "
                + "created_at, updated_at) VALUES (?, ?, ?, '交接酒水 整瓶', 1, 88800, 88800, 'pending', now(), now())", "boi_" + tag, "bo_" + tag, "bp_" + tag);
        return "bo_" + tag;
    }

    private void product(String tag, String merchantId, String itemId, Integer pourVolume, String name) {
        jdbc.update("INSERT INTO bar_products (id, bar_id, merchant_id, product_name, product_type, price_cent, inventory_item_id, "
                        + "standard_pour_volume, status, created_at, updated_at) VALUES (?, ?, ?, ?, 'liquor', 88800, ?, ?, 'active', now(), now())",
                "bp_tho_" + tag + "_" + suffix, merchantId, merchantId, name, itemId, pourVolume);
    }

    /** 订单一行、明细一行（商品取 product 的 tag）。 */
    private String order(String tag, String merchantId, String productTag, int quantity, String deliveryStatus) {
        String id = "bo_tho_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO bar_orders (id, bar_id, merchant_id, table_id, order_source, status, total_amount_cent, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, 'staff', 'submitted', 88800, now(), now())", id, merchantId, merchantId, "tbl_" + merchantId);
        String productId = "bp_tho_" + productTag + "_" + suffix;
        boolean voided = "voided".equals(deliveryStatus);
        jdbc.update("INSERT INTO bar_order_items (id, order_id, product_id, product_name, quantity, unit_price_cent, amount_cent, delivery_status, "
                        + "voided, void_reason, voided_at, created_at, updated_at) "
                        + "SELECT ?, ?, id, product_name, ?, 88800, 88800 * ?, ?, ?, ?, ?, now(), now() FROM bar_products WHERE id=?",
                "boi_tho_" + tag + "_" + suffix, id, quantity, quantity, deliveryStatus, voided, voided ? "点错" : null,
                voided ? java.sql.Timestamp.from(java.time.Instant.now()) : null, productId);
        return id;
    }

    private ResultActions create(String key, String traceCode, String orderId) throws Exception {
        return send(post("/api/v1/inventory/trace-handovers"), "{\"idempotency_key\":\"" + key + "\",\"trace_code\":\"" + traceCode
                + "\",\"order_id\":\"" + orderId + "\",\"device_id\":\"dev_tho_" + suffix + "\"}");
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(clerk).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
