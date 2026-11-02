package com.complex.platform.voucher;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 M 段第 112 条 112-R1（BSM:947「工作手机扫描权益二维码，核销赠饮、礼物或其他领取资格。」；BSM:931「……扫码核销放行），
 * 一律在公司工作手机上做……」；ENUM:81「到店由店员核销领取时才实扣」；BSM:1004「……领取……都要记录设备、点位和时间。」）：
 * 工作手机扫客人出示的券动态码先查券（只读：券、库存、预占与码都不动），再逐件扫物品码核销；核销成功留痕记设备、终端与点位。
 * 请求按员工手机真实形状（工作手机终端头、登记设备号、蛇形键）走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class WorkPhoneVoucherRedeemJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private CustomerTicketPaymentFixture f;
    private String s;
    private String device;
    private String space;
    private String item;
    private String balance;
    private RequestPostProcessor cashier;
    private RequestPostProcessor otherCashier;

    @BeforeEach
    void seed() throws Exception {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        s = f.suffix;
        device = "wpv-phone-" + s;
        space = "space_wpv_" + s;
        item = "wpv_item_" + s;
        balance = "wpv_bal_" + s;
        String other = "mch_wpv_other_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '他商户', 'retail', 'active', now(), now(), ?)", other, f.projectId);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashier = workPhone(staffs.staff("usr_wpv_cash_" + s, "staff", f.projectId, f.merchantId, "customer.voucher.redeem"));
        otherCashier = workPhone(staffs.staff("usr_wpv_other_" + s, "staff", f.projectId, other, "customer.voucher.redeem"));
        RequestPostProcessor keeper = staffs.staff("usr_wpv_keep_" + s, "staff", f.projectId, f.merchantId,
                "inventory.trace_code.register");
        // 工作手机按空间绑定一个点位（登记设备号即请求头 X-Device-Id）。
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, bound_space_id,"
                + " location_point_id, status, bound_at, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'staff_work_phone', 'space', ?, ?, 'bound', now(), now(), now())",
                "td_wpv_" + s, device, device, space, space);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '礼品', 'gift', '件', 'active', ?, now(), now())", item, f.merchantId);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity,"
                + " updated_at, merchant_id) VALUES (?, ?, ?, 10, 0, 10, now(), ?)", balance, item, "wpv_loc_" + s, f.merchantId);
        for (String tag : List.of("TRC-A2", "TRC-A3")) {
            mockMvc.perform(post("/api/v1/inventory/trace-codes").with(keeper)
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                    "idempotency_key", "wpv-reg-" + UUID.randomUUID(), "item_id", item, "trace_code", code(tag)))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void workPhonePreviewsThenRedeemsInStoreGoodsVoucherAndRecordsDeviceAndPoint() throws Exception {
        String voucher = goodsVoucher(2);
        String code = redeemCode(voucher);

        preview(cashier, code).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucher_instance_id").value(voucher))
                .andExpect(jsonPath("$.data.name").value("礼品兑换券"))
                .andExpect(jsonPath("$.data.voucher_kind").value("in_store_goods"))
                .andExpect(jsonPath("$.data.goods_quantity").value(2))
                .andExpect(jsonPath("$.data.status").value("issued"))
                .andExpect(jsonPath("$.data.block_reason").isEmpty());
        // 查券只读：券仍可核销、库存与预占不动；查券留痕一条。
        assertThat(voucherStatus(voucher)).isEqualTo("issued");
        assertThat(jdbc.queryForMap("SELECT quantity, reserved_quantity FROM inventory_stock_balances WHERE id = ?", balance))
                .containsEntry("quantity", 10).containsEntry("reserved_quantity", 0);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'voucher.redeem_previewed' AND business_id = ?",
                Integer.class, voucher)).isEqualTo(1);

        preview(otherCashier, code).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        preview(cashier, "VC" + voucher).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VOUCHER_DYNAMIC_CODE_REQUIRED"));

        // 查过的码照样能核销（码未被消费）：逐件扫两个物品码，库存实扣 2；留痕记设备、终端与点位。
        String key = "wpv-redeem-" + s;
        mockMvc.perform(post("/api/v1/vouchers/redemptions").with(cashier)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", key, "code", code, "trace_codes", List.of(code("TRC-A2"), code("TRC-A3"))))))
                .andExpect(status().isOk());
        assertThat(voucherStatus(voucher)).isEqualTo("redeemed");
        assertThat(jdbc.queryForObject("SELECT quantity FROM inventory_stock_balances WHERE id = ?", Integer.class, balance)).isEqualTo(8);
        assertThat(jdbc.queryForMap("SELECT metadata->>'device_id' AS device_id, metadata->>'terminal_type' AS terminal_type,"
                        + " metadata->>'point_id' AS point_id FROM audit_events WHERE event_name = 'voucher.redeemed' AND idempotency_key = ?", key))
                .containsEntry("device_id", device).containsEntry("terminal_type", "staff_work_phone").containsEntry("point_id", space);

        // 核销后码即失效。
        preview(cashier, code).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VOUCHER_REDEEM_CODE_EXPIRED"));
    }

    // ---------- helpers ----------

    /** 员工手机真实形状：工作手机终端头＋登记设备号（不签名，默认 grace）。 */
    private RequestPostProcessor workPhone(RequestPostProcessor staff) {
        return request -> {
            MockHttpServletRequest r = staff.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "staff_work_phone");
            r.addHeader("X-Device-Id", device);
            return r;
        };
    }

    private String code(String tag) {
        return tag + "-" + s;
    }

    private String goodsVoucher(int quantity) {
        String definition = "vdef_wpv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, inventory_item_id,"
                + " stock_location_id, stock_quantity, valid_days, status, transferable) VALUES (?, '礼品兑换券', 'in_store_goods', 'in_store_staff',"
                + " 'none', ?, ?, ?, ?, 30, 'active', TRUE)", definition, f.merchantId, item, "wpv_loc_" + s, quantity);
        String instance = "vi_wpv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO voucher_instances (id, definition_id, customer_id, code, source_type, source_ref, status, issued_at,"
                + " expires_at) VALUES (?, ?, ?, ?, 'marketing_grant', 'vgrant_wpv', 'issued', now(), now() + interval '30 days')",
                instance, definition, f.customerId, "VC" + instance);
        return instance;
    }

    /** 客人券包出示的动态券码（客人端真实入口）。 */
    private String redeemCode(String voucher) throws Exception {
        return CustomerTicketPaymentFixture.read(mockMvc.perform(get("/api/v1/customer/vouchers/" + voucher + "/redeem-code")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token)).andExpect(status().isOk()), "$.data.code");
    }

    private ResultActions preview(RequestPostProcessor who, String voucherCode) throws Exception {
        return mockMvc.perform(post("/api/v1/vouchers/redemptions/preview").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("code", voucherCode))));
    }

    private String voucherStatus(String instance) {
        return jdbc.queryForObject("SELECT status FROM voucher_instances WHERE id = ?", String.class, instance);
    }
}
