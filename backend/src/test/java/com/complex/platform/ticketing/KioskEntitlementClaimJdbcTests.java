package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 M 段第 112 条 112-R2（BSM:985「扫码查看客人当前可领取的赠饮、礼物、积分兑换权益和领取状态。」；BSM:1948「……需要扫码后在一体机上
 * 清楚看到能领什么、能领多少、是否已经领取，并把领取结果回写到权益记录。」；BSM:1180「……一体机买酒或领取礼物后要回写库存消耗。」；
 * BSM:1004「……领取……都要记录设备、点位和时间。」）：管理员把设备绑定为一体机（服务模式、按空间绑点位）并在设备台账设所属商户，
 * 登录员工在一体机上扫券码查券、逐件扫物品码领取，扫门票赠饮权益码看总次数与已用次数后领取；券领取留痕记一体机设备号、终端与点位，
 * 赠饮出 0 元赠饮单并扣吧台库存（礼物领取消耗）。请求按一体机真实形状（一体机终端头、登记设备号、蛇形键）走真实入口与真实授权链。
 * 第十八批 Q 段第 148 条：赠饮须先核同票同人份这家酒吧的入场，未先核入场即核赠饮拒绝并留一条拒绝记录。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class KioskEntitlementClaimJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private JdbcTicketingRepository ticketingRepository;

    private CustomerTicketPaymentFixture f;
    private String s;
    private String bar;
    private String space;
    private String kioskDevice;
    private RequestPostProcessor admin;
    private RequestPostProcessor staff;
    private RequestPostProcessor keeper;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        s = f.suffix;
        bar = "bar_kec_" + s;
        space = "space_kec_" + s;
        kioskDevice = "kec-kiosk-" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '一体机酒吧', 'bar', 'active', now(), now(), ?)", bar, f.projectId);
        // 一体机点位是公共区域（不进可租面积口径）：148-R1 用例不开测试事务，点位行会提交留库。
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '一体机点位', 'public_area', 'active', now(), now())", space, f.projectId);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_kec_admin_" + s, "platform_admin", f.projectId, null,
                "terminal.device.bind", "terminal.device.lifecycle");
        staff = kiosk(staffs.staff("usr_kec_staff_" + s, "staff", f.projectId, bar, "customer.voucher.redeem", "verification.execute"));
        keeper = staffs.staff("usr_kec_keep_" + s, "staff", f.projectId, bar, "inventory.trace_code.register");
    }

    @Test
    void kioskShowsWhatCanBeClaimedAndClaimsVoucherAndGiftDrinkWithDeviceAndPoint() throws Exception {
        // 管理员按后台真实请求把设备绑定为一体机（服务模式、按空间绑点位）。
        String deviceRow = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kec-bind-" + s, "device_id", kioskDevice, "terminal_type", "kiosk",
                                "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "service"))))
                .andExpect(status().isOk()), "$.data.id");

        // 门票赠饮：设备还没设所属商户时说不清在哪家门店领，拒绝。
        String gift = seedGiftDrink();
        mockMvc.perform(post("/api/v1/ticketing/verifications").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(verification("kec-ver-early-" + s, gift, deviceRow)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VERIFY_DEVICE_MERCHANT_UNRESOLVED"));
        mockMvc.perform(put("/api/v1/terminals/devices/" + deviceRow + "/merchant").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kec-merchant-" + s, "merchant_id", bar, "reason", "一体机所属酒吧"))))
                .andExpect(status().isOk());

        // 券：扫券码查券看能领什么、能领多少，逐件扫物品码领取；留痕记一体机设备号、终端与点位，库存实扣。
        String voucher = goodsVoucher();
        String code = CustomerTicketPaymentFixture.read(mockMvc.perform(get("/api/v1/customer/vouchers/" + voucher + "/redeem-code")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token)).andExpect(status().isOk()), "$.data.code");
        mockMvc.perform(post("/api/v1/vouchers/redemptions/preview").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucher_kind").value("in_store_goods"))
                .andExpect(jsonPath("$.data.goods_quantity").value(1))
                .andExpect(jsonPath("$.data.block_reason").isEmpty());
        String key = "kec-redeem-" + s;
        mockMvc.perform(post("/api/v1/vouchers/redemptions").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", key, "code", code, "trace_codes", List.of("TRC-K1-" + s)))))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM voucher_instances WHERE id = ?", String.class, voucher)).isEqualTo("redeemed");
        assertThat(jdbc.queryForObject("SELECT quantity FROM inventory_stock_balances WHERE id = ?", Integer.class, "kec_bal_" + s))
                .isEqualTo(9);
        assertThat(jdbc.queryForMap("SELECT metadata->>'device_id' AS device_id, metadata->>'terminal_type' AS terminal_type,"
                        + " metadata->>'point_id' AS point_id FROM audit_events WHERE event_name = 'voucher.redeemed' AND idempotency_key = ?", key))
                .containsEntry("device_id", kioskDevice).containsEntry("terminal_type", "kiosk").containsEntry("point_id", space);

        // 门票赠饮：先看能领多少（总次数、已用次数），再领取；出 0 元赠饮单并扣吧台库存（礼物领取消耗）。
        mockMvc.perform(post("/api/v1/ticketing/verifications/preview").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(verification("kec-pre-" + s, gift, deviceRow)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.entitlement.total_times").value(1))
                .andExpect(jsonPath("$.data.entitlement.used_times").value(0));
        // 第十八批 Q 段第 148 条（ENUM:55「客人须先核销这家酒吧的入场权益、再核销这家酒吧的酒水（赠饮）权益」）：先核同票同人份这家酒吧的入场。
        mockMvc.perform(post("/api/v1/ticketing/verifications").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(verification("kec-entry-ver-" + s, "kec-entry-" + s, deviceRow)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"));
        String verificationId = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/ticketing/verifications").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(verification("kec-ver-" + s, gift, deviceRow)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success")), "$.data.id");
        assertThat(jdbc.queryForMap("SELECT device_id, point_id FROM ticket_verifications WHERE id = ?", verificationId))
                .containsEntry("device_id", deviceRow).containsEntry("point_id", space);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_gift_redemptions WHERE verification_id = ?", Integer.class, verificationId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT available_quantity FROM inventory_stock_balances WHERE item_id = ? AND location_id = 'default'",
                Integer.class, "kec_drink_" + s)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_stock_movements WHERE item_id = ? AND movement_type = 'gift_claim_consume'",
                Integer.class, "kec_drink_" + s)).isEqualTo(1);
    }

    // 第十八批 Q 段第 148 条 148-R1（ENUM:55「同一酒吧商户，客人须先核销这家酒吧的入场权益、再核销这家酒吧的酒水（赠饮）权益，不能跳过入场
    // 直接核酒水；未先核入场即核酒水一律拒绝。」；BSM:1031「没先核入场就来核酒水的，系统一律拒绝。」；CG 裁决第 46 项「正常在线核销的业务规则
    // 拒绝也记录为rejected并留痕」）：一体机员工按蛇形键先扫赠饮——409 BAR_ENTRY_NOT_VERIFIED，该赠饮只落一条 rejected/bar_entry_not_verified，
    // 不扣次、不出 0 元赠饮单、吧台库存不动；核过同票同人份这家酒吧的入场后再扫赠饮成功，出 0 元单并扣 1 杯。
    // 不开测试事务：拒绝记录在请求事务结束后另起事务写，提交后才看得见。修复前先扫赠饮即成功，第一处 409 断言即红。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void giftDrinkBeforeThisBarsEntryIsRejectedAndSucceedsAfterTheEntry() throws Exception {
        String deviceRow = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kec-q148-bind-" + s, "device_id", kioskDevice, "terminal_type", "kiosk",
                                "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "service"))))
                .andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(put("/api/v1/terminals/devices/" + deviceRow + "/merchant").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kec-q148-merchant-" + s, "merchant_id", bar, "reason", "一体机所属酒吧"))))
                .andExpect(status().isOk());
        String gift = seedGiftDrink();

        mockMvc.perform(post("/api/v1/ticketing/verifications").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(verification("kec-q148-gift-first-" + s, gift, deviceRow)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ENTRY_NOT_VERIFIED"));
        assertThat(jdbc.queryForList("SELECT status || '/' || coalesce(result_reason, '') FROM ticket_verifications WHERE entitlement_id = ?",
                String.class, "ent_" + gift)).containsExactly("rejected/bar_entry_not_verified");
        assertThat(jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, "ent_" + gift)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_gift_redemptions WHERE entitlement_id = ?", Integer.class, "ent_" + gift))
                .isZero();
        assertThat(drinkStock()).isEqualTo(5);

        mockMvc.perform(post("/api/v1/ticketing/verifications").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(verification("kec-q148-entry-" + s, "kec-entry-" + s, deviceRow)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"));
        String verificationId = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/ticketing/verifications").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(verification("kec-q148-gift-" + s, gift, deviceRow)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success")), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_gift_redemptions WHERE verification_id = ?", Integer.class, verificationId))
                .isEqualTo(1);
        assertThat(drinkStock()).isEqualTo(4);
    }

    // ---------- helpers ----------

    private int drinkStock() {
        return jdbc.queryForObject("SELECT available_quantity FROM inventory_stock_balances WHERE item_id = ? AND location_id = 'default'",
                Integer.class, "kec_drink_" + s);
    }

    /** 一体机真实形状：一体机终端头＋登记设备号（不签名，默认 grace）。 */
    private RequestPostProcessor kiosk(RequestPostProcessor who) {
        return request -> {
            MockHttpServletRequest r = who.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "kiosk");
            r.addHeader("X-Device-Id", kioskDevice);
            return r;
        };
    }

    private String verification(String key, String code, String deviceRow) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("credential_type", "qr");
        body.put("credential_payload", code);
        body.put("device_id", deviceRow);
        body.put("point_id", space);
        body.put("business_context", Map.of("scenario", "kiosk_claim"));
        return json.writeValueAsString(body);
    }

    /** 本吧店内实物券（每张 1 件）发给客人，库管登记一个物品码。 */
    private String goodsVoucher() throws Exception {
        String item = "kec_gift_" + s;
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '礼品', 'gift', '件', 'active', ?, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity,"
                + " updated_at, merchant_id) VALUES (?, ?, ?, 10, 0, 10, now(), ?)", "kec_bal_" + s, item, "kec_loc_" + s, bar);
        mockMvc.perform(post("/api/v1/inventory/trace-codes").with(keeper)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "kec-reg-" + s, "item_id", item, "trace_code", "TRC-K1-" + s))))
                .andExpect(status().isOk());
        String definition = "vdef_kec_" + s;
        jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, inventory_item_id,"
                + " stock_location_id, stock_quantity, valid_days, status, transferable) VALUES (?, '礼品兑换券', 'in_store_goods', 'in_store_staff',"
                + " 'none', ?, ?, ?, 1, 30, 'active', TRUE)", definition, bar, item, "kec_loc_" + s);
        String instance = "vi_kec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO voucher_instances (id, definition_id, customer_id, code, source_type, source_ref, status, issued_at,"
                + " expires_at) VALUES (?, ?, ?, ?, 'marketing_grant', 'vgrant_kec', 'issued', now(), now() + interval '30 days')",
                instance, definition, f.customerId, "VC" + instance);
        return instance;
    }

    /** 门票赠饮权益（提供方＝本吧，绑本吧有内部结算价、扣库物料的商品；库存 5 杯），夹具照 GiftRedemptionEndToEndTests。 */
    private String seedGiftDrink() {
        String item = "kec_drink_" + s;
        String product = "barprod_kec_" + s;
        String code = "kec-gift-" + s;
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        String itemCode = "sku_bp_" + product;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity,"
                + " allowed_pass_free_guest_count, min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, NULL, ?, '一体机桌', 'standard', 4, 0, 0, 'CNY', 'active', now(), now())", "table_" + bar, bar, "TC_" + bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at)"
                + " VALUES (?, '赠饮酒水', 'beverage', 'cup', 'active', now(), now())", item);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at)"
                + " VALUES (?, ?, 'default', 5, 0, 5, now())", "bal_" + item, item);
        jdbc.update("INSERT INTO bar_products (id, bar_id, product_name, product_type, price_cent, currency, inventory_item_id,"
                + " stock_location_id, stock_deduction_required, status, internal_settlement_price_cent, catalog_item_code, created_at, updated_at)"
                + " VALUES (?, ?, '赠饮', 'beverage', 3800, 'CNY', ?, 'default', TRUE, 'active', 4200, ?, now(), now())",
                product, bar, item, itemCode);
        jdbc.update("INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent, status, is_draft,"
                + " updated_by, created_at, updated_at) VALUES (?, ?, ?, 'beverage', '赠饮', 4200, 'active', FALSE, 'seed_kec', now(), now())",
                itemCode, itemCode, bar);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)"
                + " VALUES (?, '一体机赠饮票', 'fixed', FALSE, 'active', now(), now())", typeId);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " bar_item_ref, created_at, updated_at) VALUES (?, '赠饮', 'gift_drink', 1, 0, 'active', ?, now(), now())", templateId, product);
        Instant now = Instant.now();
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.now(), "web", null, 0L, null, 0L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null));
        // 第十八批 Q 段第 148 条：赠饮与这家酒吧的入场同属一张票的 1 号人份（发放时每张权益都带父票与人份序号）。
        String parent = "ti_" + code;
        jdbc.update("INSERT INTO ticket_instances (id, ticket_no, order_id, ticket_type_id, seq_in_order, num_people, total_times,"
                + " status, created_at, updated_at) VALUES (?, ?, ?, ?, 1, 1, 1, 'active', now(), now())",
                parent, "T-" + code, "order_" + code, typeId);
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "gift_drink",
                bar, null, bar, 1, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                null, null, null, null, parent, 1, null, null));
        String entry = "kec-entry-" + s;
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " created_at, updated_at) VALUES (?, '酒吧入场', 'admission', 1, 0, 'active', now(), now())", "et_" + entry);
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + entry, entry, typeId, "order_" + code, "et_" + entry, "admission",
                bar, null, bar, 1, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                null, null, null, null, parent, 1, null, null));
        return code;
    }
}
