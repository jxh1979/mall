package com.complex.platform.bar;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.ticketing.IssuedEntitlementRecord;
import com.complex.platform.ticketing.TicketOrderRecord;
import com.complex.platform.ticketing.TicketingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
 * 第十八批 C 段第 17 条同根补修（真库）：BSM 第 1065 行 / ENUM 第 106 行「未配备采集设备的吧台，必须按标准出酒量配酒，也按该标准计算扣减
 * 与标准损耗，不得按零损耗记账」覆盖通票赠饮出品；第十八批裁决 4「标准损耗＝标准出酒量×本酒吧损耗率阈值」。
 * 酒吧员工在本吧核销终端按终端真实请求形状（蛇形键）核销通票赠饮；吧台＝赠饮实际扣库库存行（物料＋default 库位）所挂库房。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarGiftPourStandardLossJdbcIntegrationTests {

    private static final long INTERNAL_PRICE_CENT = 4_200L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcBarRepository barRepository;
    @Autowired private TicketingRepository ticketingRepository;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "gpour_prj_" + s;
    private final String bar = "gpour_bar_" + s;
    private final String counter = "gpour_counter_" + s;
    private final String item = "gpour_item_" + s;
    private final String product = "gpour_product_" + s;
    private final String device = "gpour_dev_" + s;
    private final String code = "gpour-code-" + s;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','赠饮出酒综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'赠饮出酒酒吧','bar','active',?,now(),now())",
                bar, project);
        // 本酒吧损耗率阈值 10%（后台「组织与商户」酒吧商户行「损耗率阈值」请求形状）。
        mockMvc.perform(put("/api/v1/platform/merchants/" + bar + "/pour-loss-rate").with(platformStaff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"gpour-loss-" + s + "\",\"value_pct\":10,\"min_pct\":5,\"max_pct\":20}"))
                .andExpect(status().isOk());
        // 赠饮扣 default 库位；该库存行挂在本酒吧的一号吧台（吧台库存）。第十八批 I 段第 76 条：按杯按容量扣，物料为 700 毫升瓶装、库存 5 瓶。
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '一号吧台')", counter, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, spec_volume_ml, created_at, updated_at) VALUES (?, '威士忌', 'liquor', '瓶', 'active', 700, now(), now())", item);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id)"
                + " VALUES (?, ?, 'default', 5, 0, 5, now(), ?, ?)", "gpour_bal_" + s, item, bar, counter);
        Instant now = Instant.now();
        barRepository.saveTable(new BarTableRecord("gpour_table_" + s, bar, bar, "T-GPOUR-" + s, "吧台位", "bar_counter", 4, 0, 0L, "CNY", "active", now, now));
        // 按杯赠饮商品：标准出酒量 25ml、需扣库，内部结算价 42 元（目录生效条目＋商品挂引用）。
        String itemCode = "sku_gpour_" + s;
        jdbc.update("""
                INSERT INTO bar_products (id, bar_id, merchant_id, product_name, product_type, price_cent, currency,
                    inventory_item_id, stock_location_id, stock_deduction_required, status,
                    internal_settlement_price_cent, catalog_item_code, standard_pour_volume, pour_volume_unit, created_at, updated_at)
                VALUES (?, ?, ?, '杯卖威士忌', 'spirit', 5000, 'CNY', ?, 'default', TRUE, 'active', ?, ?, 25, 'ml', now(), now())
                """, product, bar, bar, item, INTERNAL_PRICE_CENT, itemCode);
        jdbc.update("""
                INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name,
                    price_cent, status, is_draft, updated_by, created_at, updated_at)
                VALUES (?, ?, ?, 'beverage', '杯卖威士忌', ?, 'active', FALSE, 'seed_gift_pour', now(), now())
                """, itemCode, itemCode, bar, INTERNAL_PRICE_CENT);
        jdbc.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, merchant_id, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, now(), now())
                """, device, "d-" + device, "DC-" + device, bar);
        seedGiftEntitlement();
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void giftAtCounterWithoutPourDeviceRecordsStandardPourAndStandardLossOnZeroAmountGiftItem() throws Exception {
        String verificationId = verifyGift();
        String order = giftOrder(verificationId);

        // 未配设备吧台：赠饮单记一行 0 元赠送明细，实际出酒量＝标准 25ml，标准损耗＝25×10%＝2.5，四舍五入为 3ml（不按零损耗记账）。
        assertThat(jdbc.queryForList("""
                SELECT product_id, quantity::int AS quantity, unit_price_cent::bigint AS unit_price_cent, amount_cent::bigint AS amount_cent,
                       delivery_status, actual_pour_volume::int AS actual_pour_volume, loss_volume::int AS loss_volume, is_gift
                  FROM bar_order_items WHERE order_id = ?
                """, order)).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("product_id", product).containsEntry("quantity", 1)
                .containsEntry("unit_price_cent", 0L).containsEntry("amount_cent", 0L)
                .containsEntry("delivery_status", "delivered").containsEntry("actual_pour_volume", 25)
                .containsEntry("loss_volume", 3).containsEntry("is_gift", true));

        // 钱不变：赠饮单仍 0 元、不开账单；平台向酒吧挂的赠饮应付仍按内部结算价一笔；库存按容量扣（开一瓶 5→4，第十八批 I 段第 76 条）。
        assertThat(jdbc.queryForObject("SELECT total_amount_cent FROM bar_orders WHERE id = ?", Long.class, order)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_bills WHERE order_id = ?", Integer.class, order)).isZero();
        assertThat(jdbc.queryForList("SELECT amount_cent FROM ar_ap_items WHERE source_type = 'pass_drink_settlement' AND source_id = ? AND direction = 'payable'",
                Long.class, verificationId)).containsExactly(INTERNAL_PRICE_CENT);
        assertThat(stock()).isEqualTo(4);
    }

    @Test
    void giftAtCounterWithPourDeviceIsFulfilledAsBeforeWithoutPourRecord() throws Exception {
        // 吧台标为配有采集设备：赠饮核销没有设备采集的实际出酒量来源（设备对接不在第 17 条），照常履约、不记出酒明细。
        mockMvc.perform(put("/api/v1/inventory/warehouses/" + counter + "/pour-device").with(platformStaff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"gpour-device-" + s + "\",\"pour_device_equipped\":true}"))
                .andExpect(status().isOk());
        String verificationId = verifyGift();
        String order = giftOrder(verificationId);

        assertThat(jdbc.queryForObject("SELECT status FROM bar_gift_redemptions WHERE verification_id = ?", String.class, verificationId))
                .isEqualTo("delivered");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_order_items WHERE order_id = ?", Integer.class, order)).isZero();
        // 第十八批 I 段第 76 条：配设备吧台的赠饮没有设备实采来源，只按标准 25ml 扣、不记损耗（开一瓶 5→4、余 675ml）。
        assertThat(balance()).containsEntry("quantity", 4).containsEntry("opened_volume_ml", 675);
        assertThat(giftMovement(order)).containsEntry("quantity", 1).containsEntry("volume_ml", 25).containsEntry("loss_volume_ml", null);
    }

    @Test
    void reversingGiftVerificationVoidsThatGiftPourItemKeepingPourAndLoss() throws Exception {
        String verificationId = verifyGift();
        String order = giftOrder(verificationId);

        // 走真实冲正入口（终端冲正请求形状，蛇形键）：按杯赠饮只把标准量加回已开瓶余量、瓶数不变（第十八批 I 段第 76 条）；本次核销记下的那行赠送明细照点单退酒写法置撤单，
        // 出酒量与损耗原值保留——冲正后权益可再核销、另起新单新明细，被冲正的这行不能仍算已出酒。
        mockMvc.perform(post("/api/v1/ticketing/verifications/" + verificationId + "/reverse").with(platformStaff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"gpour-reverse-" + s + "\",\"reason\":\"客人未取酒\"}"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT status FROM bar_gift_redemptions WHERE verification_id = ?", String.class, verificationId))
                .isEqualTo("reversed");
        assertThat(jdbc.queryForList("""
                SELECT delivery_status, voided, void_reason, voided_at IS NOT NULL AS voided_at_set, return_to_stock,
                       actual_pour_volume::int AS actual_pour_volume, loss_volume::int AS loss_volume
                  FROM bar_order_items WHERE order_id = ?
                """, order)).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("delivery_status", "voided").containsEntry("voided", true)
                .containsEntry("void_reason", "赠饮核销冲正").containsEntry("voided_at_set", true)
                .containsEntry("return_to_stock", true)
                .containsEntry("actual_pour_volume", 25).containsEntry("loss_volume", 3));
        assertThat(stock()).isEqualTo(4);
    }

    /**
     * 第十八批 I 段第 76 条 76-R4（BSM 第 1065 行「按杯按标准出酒量扣库存、超出记损耗」；BSM 第 1049 行「扣减吧台售卖消耗、赠饮消耗和报损」）：
     * 未配设备吧台按杯赠饮按标准 25ml＋标准损耗 3ml 按容量扣（开一瓶 5→4、余 672ml），礼物领取消耗 gift_claim_consume 流水（第十八批 M 段 112-R3）记开瓶数、出酒量与损耗量；
     * 冲正只把标准量 25ml 加回已开瓶余量（损耗不退）、瓶数不变，冲正流水瓶数 0、带出酒量。
     */
    @Test
    void byGlassGiftDeductsStandardPlusStandardLossByVolumeAndReversalReturnsStandardToOpenedVolume() throws Exception {
        String verificationId = verifyGift();
        String order = giftOrder(verificationId);
        assertThat(balance()).containsEntry("quantity", 4).containsEntry("available_quantity", 4).containsEntry("opened_volume_ml", 672);
        assertThat(giftMovement(order)).containsEntry("movement_type", "gift_claim_consume").containsEntry("quantity", 1)
                .containsEntry("volume_ml", 25).containsEntry("loss_volume_ml", 3);

        mockMvc.perform(post("/api/v1/ticketing/verifications/" + verificationId + "/reverse").with(platformStaff())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"gpour-reverse-volume-" + s + "\",\"reason\":\"客人未取酒\"}"))
                .andExpect(status().isOk());
        assertThat(balance()).containsEntry("quantity", 4).containsEntry("available_quantity", 4).containsEntry("opened_volume_ml", 697);
        assertThat(jdbc.queryForMap("SELECT movement_type, source_location_id, target_location_id, quantity, volume_ml, loss_volume_ml"
                        + " FROM inventory_stock_movements WHERE business_type = 'bar_gift_redemption_reverse' AND business_id = ?", order))
                .containsEntry("movement_type", "gift_claim_consume").containsEntry("source_location_id", null)
                .containsEntry("target_location_id", "default").containsEntry("quantity", 0)
                .containsEntry("volume_ml", 25).containsEntry("loss_volume_ml", null);
    }

    // ---------- helpers ----------

    /** 酒吧员工在本吧核销终端核销通票赠饮：终端请求体（TerminalApiClient.verify，蛇形键）。 */
    private String verifyGift() throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/verifications").with(barStaff())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key":"gpour-verify-%s","credential_type":"qr","credential_payload":"%s",
                                 "device_id":"%s","point_id":"%s","business_context":{}}
                                """.formatted(s, code, device, bar)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new ObjectMapper().readTree(body).get("data").get("id").asText();
    }

    private String giftOrder(String verificationId) {
        return jdbc.queryForObject("SELECT bar_order_id FROM bar_gift_redemptions WHERE verification_id = ?", String.class, verificationId);
    }

    private Integer stock() {
        return jdbc.queryForObject("SELECT available_quantity FROM inventory_stock_balances WHERE item_id = ? AND location_id = 'default'",
                Integer.class, item);
    }

    private Map<String, Object> balance() {
        return jdbc.queryForMap("SELECT quantity, available_quantity, opened_volume_ml FROM inventory_stock_balances WHERE item_id = ? AND location_id = 'default'",
                item);
    }

    private Map<String, Object> giftMovement(String order) {
        return jdbc.queryForMap("SELECT movement_type, quantity, volume_ml, loss_volume_ml FROM inventory_stock_movements"
                + " WHERE business_type = 'bar_gift_redemption' AND business_id = ?", order);
    }

    /** 通票赠饮权益：模板绑本吧按杯商品（bar_item_ref＝商品 id），提供方＝本吧，可核 1 次。 */
    private void seedGiftEntitlement() {
        Instant now = Instant.now();
        Timestamp ts = Timestamp.from(now);
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        jdbc.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES (?, '赠饮通票', 'fixed', FALSE, 'active', ?, ?)
                """, typeId, ts, ts);
        jdbc.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, bar_item_ref, created_at, updated_at)
                VALUES (?, '赠饮', 'gift_drink', 1, 0, 'active', ?, ?, ?)
                """, templateId, product, ts, ts);
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.parse("2026-06-18"), "web", null, 0L, null, 0L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null));
        // 第十八批 Q 段第 148 条（ENUM:55「客人须先核销这家酒吧的入场权益、再核销这家酒吧的酒水（赠饮）权益」）：赠饮与本吧入场同属父票
        // 1 号人份，入场已核（直接落一条核销成功记录）。
        String parent = "ti_" + code;
        jdbc.update("INSERT INTO ticket_instances (id, ticket_no, order_id, ticket_type_id, seq_in_order, num_people, total_times,"
                + " status, created_at, updated_at) VALUES (?, ?, ?, ?, 1, 1, 1, 'active', now(), now())",
                parent, "T-" + code, "order_" + code, typeId);
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "gift_drink",
                bar, null, bar, 1, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                null, null, null, null, parent, 1, null, null));
        String entry = "en-" + code;
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " created_at, updated_at) VALUES (?, '酒吧入场', 'admission', 1, 0, 'active', now(), now())", "et_" + entry);
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + entry, entry, typeId, "order_" + code, "et_" + entry, "admission",
                bar, null, bar, 1, 1, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                null, null, null, null, parent, 1, null, null));
        jdbc.update("INSERT INTO ticket_verifications (id, verification_no, entitlement_id, entitlement_code, credential_type,"
                + " credential_payload, device_id, point_id, status, source_type, created_at, merchant_id, verified_people)"
                + " VALUES (?, ?, ?, ?, 'qr', ?, ?, ?, 'success', 'android_terminal_online', now(), ?, 1)",
                "ver_" + entry, "V-" + entry, "ent_" + entry, entry, entry, device, bar, bar);
    }

    /** 本吧员工（商户账号）：执行核销。 */
    private RequestPostProcessor barStaff() {
        String userId = "gpour_staff_" + s;
        grant(userId, "staff", "project", bar, List.of("verification.execute"));
        AccessActor actor = new AccessActor(userId, userId, "staff", "org_gpour", bar, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 平台员工：商户资料管理（损耗率阈值）、库房管理（吧台设备标记）与核销冲正。 */
    private RequestPostProcessor platformStaff() {
        String userId = "gpour_mgr_" + s;
        grant(userId, "platform_admin", "platform", null, List.of("merchant.profile.manage", "inventory.warehouse.manage", "ticket.verify.reverse"));
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", null, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }

    private void grant(String userId, String accountType, String dataScope, String merchantId, List<String> codes) {
        if (jdbc.queryForObject("SELECT count(*) FROM access_users WHERE id = ?", Integer.class, userId) > 0) {
            return;
        }
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, 'active', now())", userId, userId, accountType, merchantId == null ? null : "org_gpour", merchantId);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '赠饮出酒回归', ?, 'active', now())",
                role, role, dataScope);
        for (String permission : codes) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, permission);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
    }
}
