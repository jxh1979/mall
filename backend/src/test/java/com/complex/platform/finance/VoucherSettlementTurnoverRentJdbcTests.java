package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
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
 * 第十八批 Q 段第 138 条 138-R3（ENUM:257「未计收服务费的商户（餐饮、零售等），其通票或券核销划归本商户并已完成结算的部分计入」；
 * BSM:1347「未计收服务费的商户其已完成结算的部分计入」；BSM:751「域主管公司出资的免费权益按锁定内部结算价核销后结算，未核销不结算」；
 * DEC-R8-98-06「券核销结算金额＝平台按内部结算价付给核销商户的钱，对未计收服务费的商户按既有裁决计入其营业额」）：
 * 餐饮商户按内部结算价已付讫的统一券与通票合作方券结算金额进营业额抽成基数，按付款时点归期；未付讫、他店、外部伙伴与酒吧商户的不计。
 * 核销、登记付款与出抽成账单按后台真实请求形状（蛇形键）走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class VoucherSettlementTurnoverRentJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private FinanceService finance;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "vst_prj_" + s;
    private final String catering = "vst_cat_" + s;
    private final String bar = "vst_bar_" + s;
    private final String contract = "vst_contract_" + s;
    private final String rule = "vst_rule_" + s;
    private final String barContract = "vst_bar_contract_" + s;

    private RequestPostProcessor cashier;
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','券结算抽成综合体','active',now(),now())", project);
        merchant(catering, "券结算餐厅", "catering");
        merchant(bar, "券结算酒吧", "bar");
        // 抽成规则：10%、无保底，已复核生效。
        rule(rule, contract, catering);
        rule("vst_bar_rule_" + s, barContract, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashier = staffs.staff("usr_vst_cashier_" + s, "staff", project, catering, "customer.voucher.redeem");
        admin = staffs.staff("usr_vst_admin_" + s, "platform_admin", project, null,
                "finance.payment.register", "finance.turnover_rent.bill", "ticket.entitlement.manage", "verification.execute");
    }

    /**
     * ① 主管公司出资的店内实物券（内部结算价 3000）由餐厅店员核销 → 挂 partner_voucher 应付；未付讫时不计 → 财务 7 月付讫 →
     * 平台管理员出 7 月抽成账单：3000 × 10% = 300；8 月不计。
     */
    @Test
    void paidInternalPriceVoucherSettlementIsBilledAsTurnoverRent() throws Exception {
        String item = "vst_item_" + s;
        String location = "vst_loc_" + s;
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '招牌套餐', 'gift', '份', 'active', ?, now(), now())", item, catering);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id)"
                + " VALUES (?, ?, ?, 5, 0, 5, now(), ?)", "vst_bal_" + s, item, location, catering);
        String traceCode = "VST-TRC-" + s;
        jdbc.update("INSERT INTO inventory_trace_codes (id, trace_code, item_id, merchant_id, status) VALUES (?, ?, ?, ?, 'registered')",
                "vst_trc_" + s, traceCode, item, catering);
        String definition = "vdef_vst_" + s;
        jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, inventory_item_id,"
                + " stock_location_id, stock_quantity, internal_price_cent, valid_days, status, transferable)"
                + " VALUES (?, '主管公司招待套餐券', 'in_store_goods', 'in_store_staff', 'internal_price', ?, ?, ?, 1, 3000, 30, 'active', TRUE)",
                definition, catering, item, location);
        String instance = "vi_vst_" + s;
        jdbc.update("INSERT INTO voucher_instances (id, definition_id, customer_id, code, source_type, source_ref, status, issued_at,"
                + " expires_at) VALUES (?, ?, ?, ?, 'marketing_grant', 'vgrant_vst', 'issued', now(), now() + interval '30 days')",
                instance, definition, "cust_vst_" + s, "VC" + instance);

        mockMvc.perform(post("/api/v1/vouchers/redemptions").with(cashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "vst-redeem-" + s,
                                "voucher_instance_id", instance, "trace_codes", List.of(traceCode)))))
                .andExpect(status().isOk());
        String payable = payableOf(catering);
        assertThat(finance.computePeriodTurnoverRent(catering, contract, "2026-07")).isZero();

        pay(payable, 3000, "2026-07-15T06:00:00Z").andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/finance/turnover-rent/bills").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "vst-bill-" + s, "rule_id", rule,
                                "period_start", "2026-07-01", "period_end", "2026-07-31", "due_date", "2026-08-10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(300));
        assertThat(finance.computePeriodTurnoverRent(catering, contract, "2026-08")).isZero();
    }

    /** ② 通票内含、以内部采购价结算给本综合体餐厅的合作方券（2000）对账回流核销 → 付讫当月计入：2000 × 10% = 200。 */
    @Test
    void paidPartnerVoucherSettlementForMerchantCounts() throws Exception {
        String template = "et_vst_" + s;
        String ticketType = "tt_vst_" + s;
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, created_at, updated_at)"
                + " VALUES (?, '通票含餐厅套餐券', 'voucher', 1, 0, 'active', now(), now())", template);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '含餐通票', 'fixed', 'active', now(), now())", ticketType);
        jdbc.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status, issue_status, status,"
                + " created_at, updated_at) VALUES (?, ?, ?, 1, 'web', 0, 'paid', 'issued', 'created', now(), now())",
                "order_vst_" + s, "ONO_VST_" + s, ticketType);
        String code = "PV-VST-" + s;
        jdbc.update("INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code, entitlement_type,"
                + " total_times, used_times, status, valid_from, valid_to, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, 'voucher', 1, 0, 'active', now() - interval '1 day', now() + interval '30 days', now(), now())",
                "ie_vst_" + s, ticketType, "order_vst_" + s, template, code);

        mockMvc.perform(post("/api/v1/ticketing/partner-vouchers/configs").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "vst-pvc-" + s, "entitlement_template_id", template,
                                "settlement_mode", "internal_purchased", "partner_kind", "merchant", "partner_id", catering,
                                "internal_price_cent", 2000))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/ticketing/partner-vouchers/redemptions").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "vst-pvr-" + s, "entitlement_code", code,
                                "redeemed_at", "2026-07-18T04:00:00Z", "external_redemption_ref", "POS-VST-" + s,
                                "source", "reconciliation_file"))))
                .andExpect(status().isOk());
        String payable = payableOf(catering);
        assertThat(finance.computePeriodTurnoverRent(catering, contract, "2026-07")).isZero();

        pay(payable, 2000, "2026-07-20T06:00:00Z").andExpect(status().isOk());

        assertThat(finance.computePeriodTurnoverRent(catering, contract, "2026-07")).isEqualTo(200L);
    }

    /**
     * ③ 本餐厅 7 月付讫 1000 计入；本餐厅未付讫的、他店的、外部伙伴的券结算应付与本餐厅其他来源的付款不计 → 100。
     * 酒吧计收门票综合服务费，其券结算付讫 5000 不计 → 0。
     */
    @Test
    void onlyThisNonBarMerchantsPaidVoucherSettlementCounts() {
        Instant july = Instant.parse("2026-07-12T06:00:00Z");
        String other = "vst_cat_other_" + s;
        merchant(other, "他店餐厅", "catering");
        payable("own", catering, "partner_voucher", 1000, july);
        payable("unpaid", catering, "partner_voucher", 4000, null);
        payable("other", other, "partner_voucher", 6000, july);
        payable("external", "ext_partner_vst_" + s, "partner_voucher", 7000, july);
        payable("refund", catering, "refund", 8000, july);
        payable("bar", bar, "partner_voucher", 5000, july);

        assertThat(finance.computePeriodTurnoverRent(catering, contract, "2026-07")).isEqualTo(100L);
        assertThat(finance.computePeriodTurnoverRent(bar, barContract, "2026-07")).isZero();
    }

    // ---------- helpers ----------

    private void merchant(String id, String name, String type) {
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,?,'active',?,now(),now())",
                id, name, type, project);
    }

    private void rule(String id, String contractId, String merchantId) {
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '券结算营业额抽成规则', 1000, 0, DATE '2020-01-01', NULL, 'active', FALSE)
                """, id, contractId, merchantId);
    }

    private String payableOf(String merchantId) {
        return jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE direction = 'payable' AND source_type = 'partner_voucher' AND counterparty_id = ?",
                String.class, merchantId);
    }

    private ResultActions pay(String payable, long amount, String paymentTime) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + payable + "/payments").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "vst-pay-" + payable, "amount_cent", amount,
                        "payment_method", "bank_transfer", "payment_time", paymentTime))));
    }

    private void payable(String tag, String counterparty, String sourceType, long amount, Instant paidAt) {
        String id = "vst_ap_" + tag + "_" + s;
        jdbc.update("INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,unpaid_amount_cent,due_date,"
                + "status,created_at,updated_at) VALUES(?,?,'payable',?,?,?,?,?,'2026-07-31','open',now(),now())",
                id, id, counterparty, sourceType, "vst_src_" + tag + "_" + s, amount, amount);
        if (paidAt != null) {
            jdbc.update("INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,created_at)"
                    + " VALUES(?,?,?,'bank_transfer',?,now())", "vst_pay_" + tag + "_" + s, id, amount, Timestamp.from(paidAt));
        }
    }
}
