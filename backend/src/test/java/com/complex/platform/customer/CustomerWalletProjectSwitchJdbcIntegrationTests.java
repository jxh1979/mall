package com.complex.platform.customer;

// 文件版本：V0.01

import static com.complex.platform.support.CustomerProjectSwitchRequests.keyed;
import static com.complex.platform.support.CustomerProjectSwitchRequests.rejected;
import static com.complex.platform.support.CustomerTicketPaymentFixture.read;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.inventory.InventoryService;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.support.CustomerProjectSwitchRequests;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * PGM 第 736 行「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」、第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：
 * 有两个综合体时关掉甲的客人端，甲的储值卡、券、兑换项、存酒与按综合体查询的接口在服务端按对象所属综合体判——不带 X-Project-Id 或把头写成乙
 * 都 403 TERMINAL_DISABLED，写操作在幂等之前判（开着时成功过的同一请求编号，关掉后重放也拒）；乙照常。不属单一综合体的对象
 * （平台通用券、全局兑换项、不带卡范围的查询）不判。
 * 涉及：储值充值、充值入账、查卡、逐笔记录、充值规则（另含按综合体单独停储值充值模块），券动态码、券转赠发起/领取/撤回/预览
 * （含积分兑换兑出的无发券商户券），积分兑换，存酒取酒码、转存发码、转存，游乐项目与场次（匿名）、停车消费抵免；对象标识带首尾空格也绕不过。真库、真接口，客人请求按客人端真实形状；app、miniapp 翻成 enforce（与生产一致）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "access.device_credential.enforcement.app=enforce",
        "access.device_credential.enforcement.miniapp=enforce"
})
@AutoConfigureMockMvc
@Transactional
class CustomerWalletProjectSwitchJdbcIntegrationTests {

    private static final String TOPUP = "/api/v1/customer/stored-value/topup";
    private static final String SETTLE = "/api/v1/customer/stored-value/settle";
    private static final String VOUCHER_TRANSFERS = "/api/v1/customer/voucher-transfers";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private InventoryService inventoryService;
    @MockitoBean private PaymentGatewayRouter gateway;

    /** 甲＝被关的综合体，乙＝另一个综合体：各一家商户、一位客人与一张本项目储值卡。 */
    private CustomerTicketPaymentFixture a;
    private CustomerTicketPaymentFixture b;
    private CustomerProjectSwitchRequests req;

    @BeforeEach
    void seed() {
        a = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        b = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        b.stubGateway(gateway); // 渠道替身对两个综合体的付款单都生效（markPaid 一律经 b）
        req = new CustomerProjectSwitchRequests(mockMvc, jdbc, json);
    }

    /** 充值按所充卡的综合体判（商户卡取发卡商户的综合体，请求体里写别的 project_id 也绕不过）；入账按充值时服务端绑定的卡范围判。 */
    @Test
    void storedValueTopupAndSettleFollowCardProject() throws Exception {
        Map<String, Object> topup = keyed("b16-topup", "project_id", a.projectId, "amount_cent", 5000, "channel", "wechat");
        String intent = read(req.post(TOPUP, a.token, null, topup).andExpect(status().isOk()), "$.data.id");
        b.markPaid(intent);
        Map<String, Object> settle = Map.of("project_id", a.projectId, "payment_intent_id", intent);
        req.post(SETTLE, a.token, null, settle).andExpect(status().isOk());
        req.close(a.projectId);

        String bIntent = read(req.post(TOPUP, b.token, null, keyed("b16-topup", "project_id", b.projectId, "amount_cent", 5000,
                "channel", "wechat")).andExpect(status().isOk()), "$.data.id");
        b.markPaid(bIntent);
        req.post(SETTLE, b.token, null, Map.of("project_id", b.projectId, "payment_intent_id", bIntent)).andExpect(status().isOk());

        rejected(b.projectId, h -> req.post(TOPUP, a.token, h, topup));
        rejected(b.projectId, h -> req.post(SETTLE, a.token, h, settle));
        Map<String, Object> merchantCard = keyed("b16-topup", "project_id", b.projectId, "merchant_id", a.merchantId,
                "amount_cent", 5000, "channel", "wechat");
        rejected(b.projectId, h -> req.post(TOPUP, a.token, h, merchantCard));
    }

    @Test
    void storedValueReadsFollowCardProject() throws Exception {
        // 甲商户的商户卡，卡行无独立 project（V175「商户卡可无独立 project」）：逐笔记录按发卡商户的综合体判。
        String aMerchantCard = "svacc_m_b16_" + a.suffix;
        Instant now = Instant.now();
        customers.saveStoredValueAccount(new CustomerStoredValueAccountRecord(aMerchantCard, a.customerId, null, a.merchantId, 0L,
                "active", now, now, null, 0L, 0L));
        req.close(a.projectId);

        req.get("/api/v1/customer/stored-value/account", b.token, null, "project_id", b.projectId)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(b.accountId));
        req.get("/api/v1/customer/stored-value/account", a.token, null).andExpect(status().isOk());
        req.get(transactionsPath(b.accountId), b.token, null).andExpect(status().isOk());
        req.get("/api/v1/customer/stored-value/topup-rules", b.token, null, "project_id", b.projectId).andExpect(status().isOk());

        rejected(b.projectId, h -> req.get("/api/v1/customer/stored-value/account", a.token, h, "project_id", a.projectId));
        rejected(b.projectId, h -> req.get(transactionsPath(a.accountId), a.token, h));
        rejected(b.projectId, h -> req.get(transactionsPath(aMerchantCard), a.token, h));
        rejected(b.projectId, h -> req.get("/api/v1/customer/stored-value/topup-rules", a.token, h, "project_id", a.projectId));
        rejected(b.projectId, h -> req.get("/api/v1/customer/stored-value/topup-rules", a.token, h,
                "project_id", b.projectId, "merchant_id", a.merchantId));
    }

    /** 券按发券商户的综合体判；平台通用券（不属单一综合体，任意核销点可核）不判。 */
    @Test
    void vouchersFollowIssuingMerchantProject() throws Exception {
        String aVoucher = voucher(a, a.merchantId);
        String aReceiver = CustomerProjectSwitchRequests.customer(customers, "recv");
        Map<String, Object> initiate = keyed("b16-vt", "voucher_instance_id", aVoucher, "channel", "link");
        ResultActions first = req.post(VOUCHER_TRANSFERS, a.token, null, initiate).andExpect(status().isOk());
        String firstId = read(first, "$.data.id");
        Map<String, Object> claim = keyed("b16-vt-claim", "claim_code", read(first, "$.data.claim_code"));
        req.post(VOUCHER_TRANSFERS + "/" + firstId + "/claim", aReceiver, null, claim).andExpect(status().isOk());
        ResultActions pending = req.post(VOUCHER_TRANSFERS, aReceiver, null,
                keyed("b16-vt", "voucher_instance_id", aVoucher, "channel", "link")).andExpect(status().isOk());
        String platformVoucher = voucher(a, null);
        String bVoucher = voucher(b, b.merchantId);
        String bReceiver = CustomerProjectSwitchRequests.customer(customers, "recv");
        req.close(a.projectId);

        req.get(redeemCodePath(bVoucher), b.token, null).andExpect(status().isOk());
        req.get(redeemCodePath(platformVoucher), a.token, null).andExpect(status().isOk());
        ResultActions bFirst = req.post(VOUCHER_TRANSFERS, b.token, null,
                keyed("b16-vt", "voucher_instance_id", bVoucher, "channel", "link")).andExpect(status().isOk());
        req.get(VOUCHER_TRANSFERS + "/preview", bReceiver, null, "claim_code", read(bFirst, "$.data.claim_code"))
                .andExpect(status().isOk());
        req.post(VOUCHER_TRANSFERS + "/" + read(bFirst, "$.data.id") + "/revoke", b.token, null, Map.of()).andExpect(status().isOk());
        ResultActions bSecond = req.post(VOUCHER_TRANSFERS, b.token, null,
                keyed("b16-vt", "voucher_instance_id", bVoucher, "channel", "link")).andExpect(status().isOk());
        req.post(VOUCHER_TRANSFERS + "/" + read(bSecond, "$.data.id") + "/claim", bReceiver, null,
                keyed("b16-vt-claim", "claim_code", read(bSecond, "$.data.claim_code"))).andExpect(status().isOk());

        rejected(b.projectId, h -> req.get(redeemCodePath(aVoucher), aReceiver, h));
        rejected(b.projectId, h -> req.post(VOUCHER_TRANSFERS, a.token, h, initiate));
        rejected(b.projectId, h -> req.post(VOUCHER_TRANSFERS + "/" + firstId + "/claim", aReceiver, h, claim));
        rejected(b.projectId, h -> req.get(VOUCHER_TRANSFERS + "/preview", a.token, h, "claim_code", read(pending, "$.data.claim_code")));
        // 业务层把领取码去首尾空格再查：前面加空格也绕不过
        rejected(b.projectId, h -> req.get(VOUCHER_TRANSFERS + "/preview", a.token, h, "claim_code",
                " " + read(pending, "$.data.claim_code")));
        rejected(b.projectId, h -> req.post(VOUCHER_TRANSFERS + "/" + read(pending, "$.data.id") + "/revoke", aReceiver, h, Map.of()));
    }

    /**
     * 积分兑换兑出的券：目录项按综合体限定、未挂券模板时按目录项派生券模板（无发券商户），取券码与转赠按兑换来源目录项的综合体判。
     */
    @Test
    void pointsRedeemedVouchersFollowCatalogItemProject() throws Exception {
        String aVoucher = redeemedVoucher(a);
        String bVoucher = redeemedVoucher(b);
        req.close(a.projectId);

        req.get(redeemCodePath(bVoucher), b.token, null).andExpect(status().isOk());
        req.post(VOUCHER_TRANSFERS, b.token, null, keyed("b16-vt", "voucher_instance_id", bVoucher, "channel", "link"))
                .andExpect(status().isOk());

        rejected(b.projectId, h -> req.get(redeemCodePath(aVoucher), a.token, h));
        rejected(b.projectId, h -> req.post(VOUCHER_TRANSFERS, a.token, h,
                keyed("b16-vt", "voucher_instance_id", aVoucher, "channel", "link")));
    }

    /** 按综合体单独停储值充值模块（customer.stored_value.topup）：充值与入账同样按所充卡、充值时绑定的卡范围判，不认 X-Project-Id。 */
    @Test
    void storedValueTopupModuleSwitchFollowsCardProject() throws Exception {
        Map<String, Object> topup = keyed("b16-topup", "project_id", a.projectId, "amount_cent", 5000, "channel", "wechat");
        String intent = read(req.post(TOPUP, a.token, null, topup).andExpect(status().isOk()), "$.data.id");
        b.markPaid(intent);
        req.close(a.projectId, "customer.stored_value.topup");

        String bIntent = read(req.post(TOPUP, b.token, null, keyed("b16-topup", "project_id", b.projectId, "amount_cent", 5000,
                "channel", "wechat")).andExpect(status().isOk()), "$.data.id");
        b.markPaid(bIntent);
        req.post(SETTLE, b.token, null, Map.of("project_id", b.projectId, "payment_intent_id", bIntent)).andExpect(status().isOk());

        rejected(b.projectId, h -> req.post(TOPUP, a.token, h, topup));
        rejected(b.projectId, h -> req.post(SETTLE, a.token, h, Map.of("project_id", a.projectId, "payment_intent_id", intent)));
    }

    /** 积分兑换按兑换项所属综合体判；兑换项不属单一综合体（全局可见）的不判。 */
    @Test
    void pointsRedemptionFollowsCatalogItemProject() throws Exception {
        String aItem = catalogItem(a.projectId, a.merchantId);
        String bItem = catalogItem(b.projectId, b.merchantId);
        String globalItem = catalogItem(null, null);
        points(a);
        points(b);
        Map<String, Object> redeem = keyed("b16-redeem", "catalog_item_id", aItem, "quantity", 1);
        req.post("/api/v1/customer/points/redemptions", a.token, null, redeem).andExpect(status().isOk());
        req.close(a.projectId);

        req.post("/api/v1/customer/points/redemptions", b.token, null, keyed("b16-redeem", "catalog_item_id", bItem, "quantity", 1))
                .andExpect(status().isOk());
        req.post("/api/v1/customer/points/redemptions", a.token, null,
                keyed("b16-redeem", "catalog_item_id", globalItem, "quantity", 1)).andExpect(status().isOk());

        rejected(b.projectId, h -> req.post("/api/v1/customer/points/redemptions", a.token, h, redeem));
    }

    /** 存酒按所在酒吧的综合体判（与转存额度同口径）。 */
    @Test
    void savedBottlesFollowBarProject() throws Exception {
        String aBottle = bottle(a);
        String bBottle = bottle(b);
        Map<String, Object> renew = keyed("b16-renew", "sms_code", "000000");
        req.post(bottlePath(aBottle) + "/renew", a.token, null, renew).andExpect(status().isOk());
        req.close(a.projectId);

        req.get(bottlePath(bBottle) + "/pickup-code", b.token, null).andExpect(status().isOk());
        req.post(bottlePath(bBottle) + "/renewal-code", b.token, null, Map.of()).andExpect(status().isOk());
        req.post(bottlePath(bBottle) + "/renew", b.token, null, keyed("b16-renew", "sms_code", "000000")).andExpect(status().isOk());

        rejected(b.projectId, h -> req.get(bottlePath(aBottle) + "/pickup-code", a.token, h));
        rejected(b.projectId, h -> req.post(bottlePath(aBottle) + "/renewal-code", a.token, h, Map.of()));
        rejected(b.projectId, h -> req.post(bottlePath(aBottle) + "/renew", a.token, h, renew));
    }

    /** 游乐项目与场次（匿名，终端类型自报）、停车消费抵免：所查的就是 project_id 所指综合体。 */
    @Test
    void amusementProjectsAndParkingCreditFollowProjectParameter() throws Exception {
        jdbc.update("""
                INSERT INTO parking_policies (project_id, free_minutes, free_minutes_min, free_minutes_max, hourly_rate_cent,
                    hourly_rate_cent_min, hourly_rate_cent_max, daily_cap_cent, daily_cap_cent_min, daily_cap_cent_max,
                    exit_grace_minutes, exit_grace_minutes_min, exit_grace_minutes_max, credit_threshold_cent,
                    credit_threshold_cent_min, credit_threshold_cent_max, credit_minutes, credit_minutes_min, credit_minutes_max,
                    staff_discount_bp, staff_discount_bp_min, staff_discount_bp_max)
                VALUES (?,30,0,120,500,0,2000,3000,0,10000,15,0,60,20000,0,100000,120,0,480,5000,0,10000)""", b.projectId);
        req.close(a.projectId);

        req.get("/api/v1/customer/amusement/projects", null, null, "project_id", b.projectId).andExpect(status().isOk());
        req.get("/api/v1/customer/parking/credits", b.token, null, "project_id", b.projectId).andExpect(status().isOk());

        rejected(b.projectId, h -> req.get("/api/v1/customer/amusement/projects", null, h, "project_id", a.projectId));
        rejected(b.projectId, h -> req.get("/api/v1/customer/amusement/projects", null, h, "project_id", " " + a.projectId));
        rejected(b.projectId, h -> req.get("/api/v1/customer/parking/credits", a.token, h, "project_id", a.projectId));
    }

    // ---------- helpers ----------

    /** 已发给该客人的券；merchantId 为空＝平台通用券（实物券，不结算）。 */
    private String voucher(CustomerTicketPaymentFixture f, String merchantId) {
        String definition = "vdef_b16_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        if (merchantId == null) {
            jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id,"
                    + " inventory_item_id, stock_location_id, stock_quantity, valid_days, status, transferable)"
                    + " VALUES (?, '平台礼品券', 'in_store_goods', 'in_store_staff', 'none', NULL, 'item_b16', 'loc_b16', 1, 30, 'active', TRUE)",
                    definition);
        } else {
            jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id,"
                    + " face_value_cent, valid_days, status, transferable)"
                    + " VALUES (?, '满百减二十', 'merchant_discount', 'in_store_staff', 'none', ?, 2000, 30, 'active', TRUE)",
                    definition, merchantId);
        }
        String instance = "vi_b16_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO voucher_instances (id, definition_id, customer_id, code, source_type, source_ref, status, issued_at,"
                + " expires_at) VALUES (?, ?, ?, ?, 'marketing_grant', 'vgrant_b16', 'issued', now(), now() + interval '30 days')",
                instance, definition, f.customerId, "VC" + instance);
        return instance;
    }

    /**
     * 积分兑换目录项，挂接兑出的券定义（统一台账）：本综合体商户的优惠券；projectId 为空＝全局可见，挂第三方合作券（不属任何商户）。
     */
    private String catalogItem(String projectId, String merchantId) {
        String id = "redeem_b16_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String definition = "vdef_" + id;
        if (merchantId == null) {
            jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, valid_days, status,"
                    + " transferable) VALUES (?, '合作方体验券', 'external_partner', 'external_reflow', 'none', 30, 'active', FALSE)",
                    definition);
        } else {
            jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id,"
                    + " face_value_cent, valid_days, status, transferable)"
                    + " VALUES (?, '积分兑换满减券', 'merchant_discount', 'in_store_staff', 'none', ?, 2000, 30, 'active', FALSE)",
                    definition, merchantId);
        }
        jdbc.update("""
                INSERT INTO customer_redemption_catalog_items (id, item_name, item_type, points_cost, inventory_item_id, stock_location_id,
                    stock_quantity, status, available_from, available_until, created_at, updated_at, project_id, voucher_definition_id)
                VALUES (?, '综合体开关测试兑换券', 'voucher', 100, 'item_b16', 'loc_b16', 1, 'active', NULL, NULL, now(), now(), ?, ?)""",
                id, projectId, definition);
        return id;
    }

    /**
     * 开关开着时客人用积分兑换本综合体限定、未挂券模板的实物礼品（按目录项派生券模板 vdef_cat_，无发券商户，发券预占礼品库存），
     * 返回兑出的券。
     */
    private String redeemedVoucher(CustomerTicketPaymentFixture f) throws Exception {
        String gift = "gift_b16_" + f.suffix;
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at)"
                + " VALUES (?, '兑换礼品', 'goods', '件', 'active', now(), now())", gift);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity,"
                + " updated_at) VALUES (?, ?, ?, 10, 0, 10, now())", "bal_" + gift, gift, "loc_" + gift);
        String item = "redeem_b16_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("""
                INSERT INTO customer_redemption_catalog_items (id, item_name, item_type, points_cost, inventory_item_id, stock_location_id,
                    stock_quantity, status, available_from, available_until, created_at, updated_at, project_id, voucher_definition_id)
                VALUES (?, '综合体开关测试礼品', 'goods', 100, ?, ?, 1, 'active', NULL, NULL, now(), now(), ?, NULL)""",
                item, gift, "loc_" + gift, f.projectId);
        points(f);
        req.post("/api/v1/customer/points/redemptions", f.token, null, keyed("b16-redeem", "catalog_item_id", item, "quantity", 1))
                .andExpect(status().isOk());
        return jdbc.queryForObject("SELECT id FROM voucher_instances WHERE customer_id = ? AND source_type = 'points_redemption'",
                String.class, f.customerId);
    }

    private void points(CustomerTicketPaymentFixture f) {
        Instant now = Instant.now();
        customers.savePoints(new CustomerPointsAccountRecord("points_" + f.customerId, f.customerId, 1000L, "base", Map.of(), 0L,
                "active", now, 0L, null, null, 0, now, now));
    }

    /** 该客人存在本综合体一家酒吧的一瓶存酒（员工登记，5 天后到期，可转存）。 */
    private String bottle(CustomerTicketPaymentFixture f) {
        String bar = req.barMerchant(f.projectId, f.suffix);
        String item = "item_b16_" + f.suffix;
        jdbc.update("""
                INSERT INTO inventory_items (id, sku_code, item_name, item_type, unit, barcode, trace_required, min_quantity, status,
                    created_at, updated_at)
                VALUES (?, ?, '存酒测试威士忌', 'wine', 'bottle', NULL, false, 0, 'active', now(), now())""", item, "sku_" + item);
        TestSecurityActors.asPlatformAdmin();
        try {
            return inventoryService.createSavedBottle(new SavedBottleRequest("b16-bottle-" + f.suffix, null, List.of(), f.customerId, bar,
                    List.of(Map.of("item_id", item, "quantity", 1, "bottle_name", "存酒测试威士忌", "unit", "bottle")),
                    Instant.now().plus(Duration.ofDays(5)), "LBL-B16", "甲", f.phone, List.of("reg_photo"), null)).id();
        } finally {
            TestSecurityActors.clear();
        }
    }

    private static String transactionsPath(String accountId) {
        return "/api/v1/customer/stored-value/accounts/" + accountId + "/transactions";
    }

    private static String redeemCodePath(String voucherInstanceId) {
        return "/api/v1/customer/vouchers/" + voucherInstanceId + "/redeem-code";
    }

    private static String bottlePath(String savedBottleId) {
        return "/api/v1/customer/saved-bottles/" + savedBottleId;
    }
}
