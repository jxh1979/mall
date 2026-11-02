package com.complex.platform.voucher;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerPointsAccountRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 L 段第 103 条（真库）：BSM:1197「酒水/礼品/高价值物料贴平台唯一二维码，出库须同时核验订单和物品码」；BSM:1166「……可以入库贴码，
 * 也可以出库打印码」；ENUM:81「②店内实物／服务——发券时从可用库存预占，到店由店员核销领取时才实扣」。
 * 103-R1：店员核销店内实物券须逐件扫物品码（个数＝每张券件数、不重复、已登记为本券货品且属本商户），不过券与库存都不动；
 * 103-R2：后台平台账号（无商户范围）登记物品码按物料归属商户。请求一律按后台网页真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class GiftVoucherTraceCodeRedeemJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private CustomerTicketPaymentFixture f;
    private RequestPostProcessor cashier;
    private RequestPostProcessor keeper;
    private RequestPostProcessor platform;
    private String s;
    private String itemA;
    private String itemB;
    private String location;
    private String balance;

    @BeforeEach
    void seed() throws Exception {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        s = f.suffix;
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashier = staffs.staff("usr_gtc_cash_" + s, "staff", f.projectId, f.merchantId, "customer.voucher.redeem");
        keeper = staffs.staff("usr_gtc_keep_" + s, "staff", f.projectId, f.merchantId, "inventory.trace_code.register");
        platform = staffs.staff("usr_gtc_plat_" + s, "platform_admin", null, null, "inventory.trace_code.register");
        itemA = "gtc_item_a_" + s;
        itemB = "gtc_item_b_" + s;
        location = "gtc_loc_" + s;
        balance = "gtc_bal_" + s;
        item(itemA, "礼品甲", f.merchantId);
        item(itemB, "礼品乙", f.merchantId);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?)", balance, itemA, location, f.merchantId);
        // 本商户库管按后台「登记物品码」请求形状登记：物料甲三个码、物料乙一个码；另一商户的物料甲码由 SQL 种入。
        for (String tag : List.of("TRC-A1", "TRC-A2", "TRC-A3")) {
            register(keeper, itemA, code(tag)).andExpect(status().isOk());
        }
        register(keeper, itemB, code("TRC-B")).andExpect(status().isOk());
        jdbc.update("INSERT INTO inventory_trace_codes (id, trace_code, item_id, merchant_id, status) VALUES (?, ?, ?, ?, 'registered')",
                "gtc_trc_x_" + s, code("TRC-X"), itemA, "gtc_other_merchant_" + s);
    }

    @Test
    void inStoreGoodsRedeemRequiresEachPieceScannedWithRegisteredCodeOfVoucherItem() throws Exception {
        String voucher = voucher(goods(1));
        redeem(voucher, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("TRACE_CODE_INCOMPLETE"));
        redeem(voucher, List.of(code("TRC-NONE"))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("TRACE_CODE_NOT_FOUND"));
        redeem(voucher, List.of(code("TRC-B"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("TRACE_CODE_MISMATCH"));
        redeem(voucher, List.of(code("TRC-X"))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        // 核验不过：券仍可核销、库存不动、不出库。
        assertThat(voucherStatus(voucher)).isEqualTo("issued");
        assertThat(stock()).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_stock_movements WHERE movement_type = 'gift_claim_consume' AND item_id = ?",
                Integer.class, itemA)).isZero();

        String key = "gtc-redeem-ok-" + s;
        redeem(voucher, List.of(code("TRC-A1")), key).andExpect(status().isOk());
        assertThat(voucherStatus(voucher)).isEqualTo("redeemed");
        assertThat(stock()).isEqualTo(9);
        // 物品码随核销成功审计事件留存。
        assertThat(jdbc.queryForObject("SELECT (metadata->'trace_codes')::text FROM audit_events WHERE event_name = 'voucher.redeemed' AND idempotency_key = ?",
                String.class, key)).isEqualTo("[\"" + code("TRC-A1") + "\"]");
    }

    @Test
    void twoPieceVoucherNeedsTwoDistinctCodes() throws Exception {
        String voucher = voucher(goods(2));
        redeem(voucher, List.of(code("TRC-A2"), code("TRC-A2"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("TRACE_CODE_INCOMPLETE"));
        assertThat(stock()).isEqualTo(10);
        redeem(voucher, List.of(code("TRC-A2"), code("TRC-A3"))).andExpect(status().isOk());
        assertThat(voucherStatus(voucher)).isEqualTo("redeemed");
        assertThat(stock()).isEqualTo(8);
    }

    @Test
    void nonBarMerchantDiscountVoucherStillRedeemsWithoutCodes() throws Exception {
        String voucher = voucher(discount());
        redeem(voucher, null).andExpect(status().isOk());
        assertThat(voucherStatus(voucher)).isEqualTo("redeemed");
    }

    @Test
    void platformAccountRegistersCodeUnderItemOwnerAndPlatformOwnItemStillNeedsMerchant() throws Exception {
        register(platform, itemA, code("TRC-P")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT merchant_id FROM inventory_trace_codes WHERE trace_code = ?", String.class, code("TRC-P")))
                .isEqualTo(f.merchantId);
        redeem(voucher(goods(1)), List.of(code("TRC-P"))).andExpect(status().isOk());
        assertThat(stock()).isEqualTo(9);

        String platformItem = "gtc_item_p_" + s;
        item(platformItem, "平台自有物料", null);
        register(platform, platformItem, code("TRC-Q")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_REQUIRED"));
    }

    /**
     * 第十八批 M 段第 112 条 112-R3（BSM:1194「积分兑换消耗、礼物领取消耗（三种“消耗”分开记）」；BSM:1179「积分兑换的实物券发券时预占库存、
     * 核销领取时才实扣」；ENUM:78「积分只兑换虚拟券」）：客人用积分兑换来的店内实物券核销记积分兑换消耗；营销赠发的同款券核销记礼物领取消耗。
     */
    @Test
    void pointsRedeemedVoucherConsumesAsRedeemConsumeAndGrantedVoucherAsGiftClaimConsume() throws Exception {
        String definition = goods(1);
        String catalog = "redeem_gtc_" + s;
        jdbc.update("INSERT INTO customer_redemption_catalog_items (id, item_name, item_type, points_cost, inventory_item_id, stock_location_id,"
                + " stock_quantity, status, created_at, updated_at, voucher_definition_id)"
                + " VALUES (?, '礼品兑换', 'goods', 100, ?, ?, 1, 'active', now(), now(), ?)", catalog, itemA, location, definition);
        Instant now = Instant.now();
        customers.savePoints(new CustomerPointsAccountRecord("points_" + f.customerId, f.customerId, 1000L, "base", Map.of(), 0L,
                "active", now, 0L, null, null, 0, now, now));
        String key = "gtc-points-" + s;
        mockMvc.perform(post("/api/v1/customer/points/redemptions").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", f.token).header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", key, "catalog_item_id", catalog, "quantity", 1))))
                .andExpect(status().isOk());
        String pointsVoucher = jdbc.queryForObject(
                "SELECT id FROM voucher_instances WHERE customer_id = ? AND source_type = 'points_redemption'", String.class, f.customerId);
        String code = CustomerTicketPaymentFixture.read(mockMvc.perform(get("/api/v1/customer/vouchers/" + pointsVoucher + "/redeem-code")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token)).andExpect(status().isOk()), "$.data.code");
        redeemCode(code, List.of(code("TRC-A1"))).andExpect(status().isOk());
        assertThat(consumed("redeem_consume")).isEqualTo(1);
        assertThat(consumed("gift_claim_consume")).isZero();

        RequestPostProcessor marketer = new StaffRequestFixture(jdbc)
                .staff("usr_gtc_mkt_" + s, "staff", f.projectId, f.merchantId, "customer.voucher.manage");
        String granted = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/vouchers/instances").with(marketer)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "gtc-grant-" + s, "definition_id", definition, "customer_id", f.customerId,
                                "quantity", 1))))
                .andExpect(status().isOk()), "$.data.instances[0].id");
        redeem(granted, List.of(code("TRC-A2"))).andExpect(status().isOk());
        assertThat(consumed("gift_claim_consume")).isEqualTo(1);
        assertThat(consumed("redeem_consume")).isEqualTo(1);
    }

    /**
     * 第十八批 M 段 V589（BSM:1072「转库存之后的报废、赠送、内部使用等处置必须走审批并保留操作记录」；第 112 条 112-R3）：营销赠发的店内实物券
     * 库位为存酒区库房时，核销按礼物领取消耗落存酒库房处置待审单，批准前不扣库存。
     */
    @Test
    void grantedVoucherAtSavedBottleWarehouseRedeemsIntoPendingGiftClaimDisposal() throws Exception {
        String savedWarehouse = "gtc_wsb_" + s;
        jdbc.update("INSERT INTO warehouses (id, project_id, warehouse_type, name, status, merchant_id) VALUES (?, ?, 'saved_bottle', '存酒库房', 'active', ?)",
                savedWarehouse, f.projectId, f.merchantId);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id)"
                + " VALUES (?, ?, ?, 3, 0, 3, now(), ?)", "gtc_bal_sb_" + s, itemA, savedWarehouse, f.merchantId);
        RequestPostProcessor marketer = new StaffRequestFixture(jdbc)
                .staff("usr_gtc_mkt_" + s, "staff", f.projectId, f.merchantId, "customer.voucher.manage");
        String granted = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/vouchers/instances").with(marketer)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "gtc-grant-sb-" + s, "definition_id", goods(1, savedWarehouse),
                                "customer_id", f.customerId, "quantity", 1))))
                .andExpect(status().isOk()), "$.data.instances[0].id");

        redeem(granted, List.of(code("TRC-A1"))).andExpect(status().isOk());

        assertThat(voucherStatus(granted)).isEqualTo("redeemed");
        assertThat(jdbc.queryForMap("SELECT adjustment_type, status FROM inventory_stock_adjustments WHERE item_id = ? AND location_id = ?",
                itemA, savedWarehouse)).containsEntry("adjustment_type", "gift_claim_consume").containsEntry("status", "pending_review");
        assertThat(jdbc.queryForObject("SELECT quantity FROM inventory_stock_balances WHERE location_id = ?", Integer.class, savedWarehouse))
                .isEqualTo(3);
    }

    // ---------- helpers ----------

    private String code(String tag) {
        return tag + "-" + s;
    }

    private void item(String id, String name, String merchantId) {
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, ?, 'gift', '件', 'active', ?, now(), now())", id, name, merchantId);
    }

    private String goods(int quantity) {
        return goods(quantity, location);
    }

    private String goods(int quantity, String stockLocation) {
        String id = "vdef_gtc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, inventory_item_id,"
                + " stock_location_id, stock_quantity, valid_days, status, transferable) VALUES (?, '礼品兑换券', 'in_store_goods', 'in_store_staff', 'none',"
                + " ?, ?, ?, ?, 30, 'active', TRUE)", id, f.merchantId, itemA, stockLocation, quantity);
        return id;
    }

    private String discount() {
        String id = "vdef_gtc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, face_value_cent,"
                + " valid_days, status, transferable) VALUES (?, '游乐满减券', 'merchant_discount', 'in_store_staff', 'none', ?, 2000, 30,"
                + " 'active', TRUE)", id, f.merchantId);
        return id;
    }

    private String voucher(String definition) {
        String instance = "vi_gtc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO voucher_instances (id, definition_id, customer_id, code, source_type, source_ref, status, issued_at,"
                + " expires_at) VALUES (?, ?, ?, ?, 'marketing_grant', 'vgrant_gtc', 'issued', now(), now() + interval '30 days')",
                instance, definition, f.customerId, "VC" + instance);
        return instance;
    }

    private ResultActions redeem(String instance, List<String> codes) throws Exception {
        return redeem(instance, codes, "gtc-redeem-" + UUID.randomUUID());
    }

    private ResultActions redeem(String instance, List<String> codes, String key) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("voucher_instance_id", instance);
        if (codes != null) {
            body.put("trace_codes", codes);
        }
        return mockMvc.perform(post("/api/v1/vouchers/redemptions").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions redeemCode(String redeemCode, List<String> codes) throws Exception {
        return mockMvc.perform(post("/api/v1/vouchers/redemptions").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "idempotency_key", "gtc-redeem-" + UUID.randomUUID(), "code", redeemCode, "trace_codes", codes))));
    }

    private int consumed(String movementType) {
        return jdbc.queryForObject("SELECT count(*) FROM inventory_stock_movements WHERE movement_type = ? AND item_id = ?",
                Integer.class, movementType, itemA);
    }

    private ResultActions register(RequestPostProcessor who, String item, String traceCode) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory/trace-codes").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "idempotency_key", "gtc-reg-" + UUID.randomUUID(), "item_id", item, "trace_code", traceCode))));
    }

    private String voucherStatus(String instance) {
        return jdbc.queryForObject("SELECT status FROM voucher_instances WHERE id = ?", String.class, instance);
    }

    private int stock() {
        return jdbc.queryForObject("SELECT quantity FROM inventory_stock_balances WHERE id = ?", Integer.class, balance);
    }
}
