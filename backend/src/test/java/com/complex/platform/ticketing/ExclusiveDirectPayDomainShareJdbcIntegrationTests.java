package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.settlement.SettlementSettledConsumer;
import com.complex.platform.settlement.TicketDistributionSettlementService;
import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.support.PassPoolFixtures;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 X 段第 176 条 176-R1（BSM:749「跨域组合通票扣完上述通路费和适用的销售渠道费后，再按各业态价格占比分到各域——各业态价格在售票时刻随订单
 * 一并固化、事后不得变更。零元免费权益不参与域间分配」「两者一律在拆分给各域之前从整单扣除且只扣一次，由各域按分配份额自然共担」；ENUM:51 同义；
 * BSM:804 冲回逐段生成反向结算记录）：组合通票里有价的餐饮专属直付行核销时，直付额仍按内部结算价，另记本次域份额（售票固化的业态价格占比拆得），
 * 并按域份额分摊本单付款时固化的通路费；零元平台出资餐饮行不记域份额、不分摊通路费；撤销核销的冲正行两项取反；单一酒吧门票行口径不变。
 *
 * <p>真实接口与授权链：按 TicketingPoliciesPage 组合通票同键（前端蛇形键）建模板、绑票种；第三方渠道（美团，比例模式销售渠道费）售出由付款确认内核
 * 固化通路费并自动出票；酒吧员工在本店设备检票入场（197-R1 本场检票）后，餐厅员工在本店设备核销；平台管理员撤销核销。不开测试事务（结算事实在核销事务提交后写入），@AfterEach 按订单与项目清理。
 *
 * <p>176-R2（BSM:749「由各域按分配份额自然共担」「餐饮与零售的域主管公司为城市项目公司……其份额由跨域组合通票的收款主体（娱乐平台公司）直接与实际提供的
 * 餐饮、零售商户结算」；ENUM:53「哪个主体按内部结算价向实际提供方足额结算并自行承担差额」）：同一夹具售出核销后把本单结算事实移到 2017-06 月中，
 * 综合体与酒吧今天建档，该月营业日都在建档之前、不查最终标记；按项目月结。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class ExclusiveDirectPayDomainShareJdbcIntegrationTests {

    private static final long CATALOG_PRICE_CENT = 2000L;
    private static final long BAR_PRICE_CENT = 5000L;
    private static final long DINING_PRICE_CENT = 3000L;
    private static final long ORDER_AMOUNT_CENT = BAR_PRICE_CENT + DINING_PRICE_CENT;
    private static final YearMonth MONTH = YearMonth.of(2017, 6);
    /** 北京 2017-06-15 14:00：月中，任何营业日配置下都属 6-15。 */
    private static final Instant MID_MONTH = Instant.parse("2017-06-15T06:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TicketOrderService orderService;
    @Autowired private SettlementSettledConsumer settledConsumer;
    @Autowired private TicketDistributionSettlementService settlement;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_c176_" + suffix;
    private final String bar = "bar_c176_" + suffix;
    private final String restaurant = "rest_c176_" + suffix;
    private final String admin = "usr_c176_admin_" + suffix;
    private final String staff = "usr_c176_staff_" + suffix;
    private final String device = "dev_c176_" + suffix;
    private final String barStaff = "usr_c176_bar_" + suffix;
    private final String barDevice = "dev_c176_bar_" + suffix;
    private final String typeId = "tt_c176_" + suffix;
    private final String catalogCode = "sku_c176_" + suffix;
    private final List<String> templateIds = new ArrayList<>();
    private final List<String> orders = new ArrayList<>();
    private final List<String> companies = new ArrayList<>();
    private final String park = "park_c176_" + suffix;

    @BeforeEach
    void seed() {
        long n = Long.parseLong(suffix, 16);
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '组合通票直付综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', 'active', ?, now(), now())", bar, "直付酒吧" + n, project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'catering', 'active', ?, now(), now())", restaurant, "直付餐厅" + n, project);
        jdbc.update("""
                INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name,
                    price_cent, status, is_draft, updated_by, created_at, updated_at)
                VALUES (?, ?, ?, 'dining', '套餐', ?, 'active', FALSE, 'seed_c176', now(), now())
                """, catalogCode, catalogCode, restaurant, CATALOG_PRICE_CENT);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)"
                + " VALUES (?, '跨域组合通票', 'fixed', FALSE, 'draft', now(), now())", typeId);
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, now(), now())", device, device, device, restaurant);
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, now(), now())", barDevice, barDevice, barDevice, bar);
        // 第三方渠道（美团）：通路费 1%（付款时按支付通路固化），销售渠道费比例 8%。
        jdbc.update("""
                INSERT INTO channel_fee_configs (id, project_id, fee_layer, channel, fee_bps, status, is_draft)
                VALUES (?, ?, 'route', 'meituan', 100, 'active', FALSE)""", "cfc_c176r_" + suffix, project);
        jdbc.update("""
                INSERT INTO channel_fee_configs (id, project_id, fee_layer, channel, fee_bps, status, is_draft)
                VALUES (?, ?, 'sales', 'meituan', 800, 'active', FALSE)""", "cfc_c176s_" + suffix, project);
        user(admin, "platform_admin", "org_1", null, "组合通票直付配置" + n,
                List.of("ticket.entitlement.manage", "ticket.product.manage", "ticket.verify.reverse"));
        user(staff, "staff", project, restaurant, "直付餐厅核销员" + n, List.of("verification.execute"));
        user(barStaff, "staff", project, bar, "直付酒吧检票员" + n, List.of("verification.execute"));
    }

    @AfterEach
    void cleanup() {
        TestSecurityActors.clear();
        AppendOnlyTestBypass.updateOnTable(jdbc, "pass_gift_cost_variance_ledger", "trg_append_only_ud",
                "DELETE FROM pass_gift_cost_variance_ledger WHERE project_id = ?", project);
        for (String table : List.of("settlement_items", "settlement_bill_format_shares", "pass_pool_unit_results")) {
            jdbc.update("DELETE FROM " + table + " WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)", project);
        }
        jdbc.update("DELETE FROM settlement_bills WHERE project_id = ?", project);
        for (String company : companies) {
            jdbc.update("DELETE FROM business_entity_projects WHERE business_entity_id = ?", company);
            jdbc.update("DELETE FROM business_entities WHERE id = ?", company);
        }
        for (String order : orders) {
            AppendOnlyTestBypass.update(jdbc, "DELETE FROM settlement_transactions WHERE order_id = ?", order);
            jdbc.update("DELETE FROM ticket_verifications WHERE entitlement_id IN (SELECT id FROM issued_entitlements WHERE order_id = ?)", order);
            jdbc.update("DELETE FROM issued_entitlements WHERE order_id = ?", order);
            jdbc.update("DELETE FROM ticket_instances WHERE order_id = ?", order);
            jdbc.update("DELETE FROM ticket_payment_route_fees WHERE order_id = ?", order);
            jdbc.update("DELETE FROM ticket_order_channel_fees WHERE order_id = ?", order);
            AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_payment_intents", "trg_append_only_d",
                    "DELETE FROM ticket_payment_intents WHERE business_order_id = ?", order);
            jdbc.update("DELETE FROM ticket_orders WHERE id = ?", order);
        }
        PassPoolFixtures.deleteByProject(jdbc, project);
        jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", typeId);
        for (String id : templateIds) {
            jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM internal_price_catalog WHERE item_code = ?", catalogCode);
        jdbc.update("DELETE FROM channel_fee_configs WHERE project_id = ?", project);
        jdbc.update("DELETE FROM ticket_types WHERE id = ?", typeId);
        jdbc.update("DELETE FROM terminal_devices WHERE id IN (?, ?)", device, barDevice);
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
    }

    @Test
    void paidExclusiveDiningRecordsDomainShareAndSharesRouteFeeWhileZeroPriceDiningDoesNot() throws Exception {
        String entryTpl = template("直付酒吧 · 入场", "entry", bar, null, null);
        bind(entryTpl, BAR_PRICE_CENT, null);
        String paidTpl = template("直付餐厅 · 餐饮（有价）", "dining", restaurant, catalogCode, restaurant);
        bind(paidTpl, DINING_PRICE_CENT, "platform_funded");
        String freeTpl = template("直付餐厅 · 餐饮（零元平台出资）", "dining", restaurant, catalogCode, restaurant);
        bind(freeTpl, 0L, "platform_funded");
        String order = sell();

        long paidAllocated = allocated(order, paidTpl);
        assertThat(paidAllocated).as("有价餐饮行按售票固化的业态价格占比拆得").isEqualTo(DINING_PRICE_CENT);
        // 197-R1：票内餐饮券须本场已检票，先在酒吧检票入场。
        verify(code(order, entryTpl), barStaff, bar, barDevice);
        String paidVerification = verify(code(order, paidTpl), staff, restaurant, device);
        String freeVerification = verify(code(order, freeTpl), staff, restaurant, device);

        Map<String, Object> paid = settled(paidVerification);
        assertThat(num(paid, "verified_amount_cent")).as("直付额仍按内部结算价").isEqualTo(CATALOG_PRICE_CENT);
        assertThat(paid.get("domain_share_cent")).as("有价专属直付行记本次域份额").isEqualTo(paidAllocated);
        long fixedRouteFee = jdbc.queryForObject("SELECT SUM(fee_cent) FROM ticket_payment_route_fees WHERE order_id = ?", Long.class, order);
        assertThat(fixedRouteFee).isEqualTo(80L);
        long barRouteFee = jdbc.queryForObject("SELECT route_fee_cent FROM pass_pool_snapshots WHERE order_id = ?", Long.class, order);
        // 本单累计差分：出票时酒吧份额 5000 先分得 round(80×5000÷8000)＝50，本行域份额 3000 分得 round(80×8000÷8000)−50＝30。
        assertThat(barRouteFee).isEqualTo(50L);
        assertThat(num(paid, "route_fee_cent")).as("按域份额分摊本单通路费").isEqualTo(30L);

        Map<String, Object> free = settled(freeVerification);
        assertThat(num(free, "verified_amount_cent")).isEqualTo(CATALOG_PRICE_CENT);
        assertThat(free.get("domain_share_cent")).as("零元免费权益不参与域间分配").isNull();
        assertThat(num(free, "route_fee_cent")).isZero();

        long verifiedRouteFee = jdbc.queryForObject("SELECT SUM(route_fee_cent) FROM settlement_transactions WHERE order_id = ?",
                Long.class, order);
        assertThat(barRouteFee + verifiedRouteFee).as("本单各部分通路费合计＝付款时固化的通路费").isEqualTo(fixedRouteFee);

        mockMvc.perform(post("/api/v1/ticketing/verifications/" + paidVerification + "/reverse").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c176-rev-%s", "reason": "核销有误撤销"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());
        Map<String, Object> reversed = jdbc.queryForMap("SELECT domain_share_cent, route_fee_cent, verified_amount_cent FROM settlement_transactions"
                + " WHERE verification_id = ? AND transaction_type = 'verification_reversed'", paidVerification);
        assertThat(reversed.get("domain_share_cent")).as("冲正行域份额取反").isEqualTo(-paidAllocated);
        assertThat(num(reversed, "route_fee_cent")).as("冲正行通路费取反").isEqualTo(-num(paid, "route_fee_cent"));
        assertThat(num(reversed, "verified_amount_cent")).isEqualTo(-CATALOG_PRICE_CENT);
    }

    @Test
    void singleBarTicketRowKeepsNoDomainShareAndNoRouteFee() {
        // ENUM:32 单一酒吧门票（订单挂酒吧）按专属直付记到订单酒吧；本条不改其口径：不记域份额、不分摊通路费。
        String order = "ord_c176sb_" + suffix;
        orders.add(order);
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency,
                    pay_status, issue_status, status, created_at, updated_at, project_id, bar_id)
                VALUES (?, ?, ?, 1, 'meituan', 5000, 'CNY', 'paid', 'issued', 'created', now(), now(), ?, ?)""",
                order, "ON-" + order, typeId, project, bar);
        jdbc.update("""
                INSERT INTO ticket_payment_intents (id, business_order_type, business_order_id, amount_cent, currency, channel,
                    status, created_at, updated_at) VALUES (?, 'ticket_order', ?, 5000, 'CNY', 'meituan', 'succeeded', now(), now())""",
                "pi_" + order, order);
        jdbc.update("""
                INSERT INTO ticket_payment_route_fees (payment_intent_id, order_id, project_id, payment_channel, amount_cent, fee_cent)
                VALUES (?, ?, ?, 'meituan', 5000, 50)""", "pi_" + order, order, project);
        Instant at = Instant.now();
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                "vf_c176sb_" + suffix, at, "ent_c176sb_" + suffix, order, typeId, "entry", bar,
                1, 1, 1, 5000L, at.plusSeconds(86_400), null, null, null, project, "tkt_c176sb_" + suffix));

        Map<String, Object> row = settled("vf_c176sb_" + suffix);
        assertThat(row.get("exclusive_merchant_id")).isEqualTo(bar);
        assertThat(row.get("domain_share_cent")).isNull();
        assertThat(num(row, "route_fee_cent")).isZero();
    }

    @Test
    void monthlySettlementFoldsPaidExclusiveDiningShareIntoCateringRowOwnedByCityCompany() throws Exception {
        String entryTpl = template("直付酒吧 · 入场", "entry", bar, null, null);
        bind(entryTpl, BAR_PRICE_CENT, null);
        String paidTpl = template("直付餐厅 · 餐饮（有价）", "dining", restaurant, catalogCode, restaurant);
        bind(paidTpl, DINING_PRICE_CENT, "platform_funded");
        String freeTpl = template("直付餐厅 · 餐饮（零元平台出资）", "dining", restaurant, catalogCode, restaurant);
        bind(freeTpl, 0L, "platform_funded");
        String order = sell();
        verify(code(order, entryTpl), barStaff, bar, barDevice);
        verify(code(order, paidTpl), staff, restaurant, device);
        verify(code(order, freeTpl), staff, restaurant, device);
        moveToMidMonth(order);
        String company = cityCompany("城市项目公司甲");

        String bill = settlement.settleMonth(project, MONTH);

        Map<String, Object> catering = formatShare(bill, "catering");
        assertThat(catering).as("有价餐饮专属直付份额并入业态拆分").isNotNull();
        assertThat(num(catering, "gross_revenue_cent")).as("毛额＝Σ有价行域份额，零元行不进").isEqualTo(DINING_PRICE_CENT);
        assertThat(num(catering, "route_fee_cent")).as("通路费＝按域份额分摊").isEqualTo(30L);
        assertThat(num(catering, "channel_fee_cent")).as("销售渠道费 8% 按毛额共担").isEqualTo(240L);
        assertThat(num(catering, "internal_settled_cent")).as("按内部结算价应直付额，零元行不进").isEqualTo(CATALOG_PRICE_CENT);
        assertThat(num(catering, "net_revenue_cent") - num(catering, "internal_settled_cent"))
                .as("ENUM:53 差额：3000−240−30−2000，为正归城市项目公司").isEqualTo(730L);
        assertThat(catering.get("beneficiary_kind")).isEqualTo("entity");
        assertThat(catering.get("beneficiary_entity_id")).as("ENUM:49 餐饮域 → 城市项目公司").isEqualTo(company);

        Map<String, Object> snapshot = jdbc.queryForMap(
                "SELECT bar_gross_cent, sales_channel_fee_cent, pool_cent FROM pass_pool_snapshots WHERE order_id = ?", order);
        Map<String, Object> b = jdbc.queryForMap("SELECT * FROM settlement_bills WHERE id = ?", bill);
        assertThat(num(b, "available_pool_cent")).as("酒吧可分配池仍＝售票固化池，不替餐饮担费").isEqualTo(num(snapshot, "pool_cent"));
        assertThat(num(b, "ticket_revenue_cent")).isEqualTo(num(snapshot, "bar_gross_cent") + DINING_PRICE_CENT);
        assertThat(num(b, "route_fee_cent")).as("各部分通路费合计＝付款时固化的通路费").isEqualTo(80L);
        assertThat(num(b, "channel_fee_cent")).isEqualTo(num(snapshot, "sales_channel_fee_cent") + 240L);
        assertThat(jdbc.queryForObject("SELECT final_amount_cent FROM settlement_items WHERE bill_id = ? AND merchant_id = ?"
                + " AND distribution_method = 'exclusive_full'", Long.class, bill, restaurant))
                .as("直付明细仍按内部结算价（有价、零元两行）").isEqualTo(2 * CATALOG_PRICE_CENT);

        cityCompany("城市项目公司乙");
        Map<String, Object> rerun = formatShare(settlement.settleMonth(project, MONTH), "catering");
        assertThat(num(rerun, "net_revenue_cent")).as("两家有效城市项目公司时金额照算").isEqualTo(num(catering, "net_revenue_cent"));
        assertThat(rerun.get("beneficiary_kind")).isEqualTo("entity");
        assertThat(rerun.get("beneficiary_entity_id")).as("归属主体留空").isNull();
    }

    @Test
    void fixedAmountSalesFeeIsSharedByCateringInsteadOfLoadingAmusement() throws Exception {
        // 固定金额每张 800：售票快照已扣酒吧那部分 round(800×4000÷8000)＝400，月结余下 400 按毛额摊给游乐 1000、餐饮 3000。
        jdbc.update("UPDATE channel_fee_configs SET fee_bps = 0, fixed_amount_cent = 800 WHERE id = ?", "cfc_c176s_" + suffix);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'amusement', 'active', ?, now(), now())", park, "直付游乐" + Long.parseLong(suffix, 16), project);
        String entryTpl = template("直付酒吧 · 入场", "entry", bar, null, null);
        bind(entryTpl, 4000L, null);
        String parkTpl = template("直付游乐 · 游乐", "amusement", park, null, null);
        bind(parkTpl, 1000L, null);
        String paidTpl = template("直付餐厅 · 餐饮（有价）", "dining", restaurant, catalogCode, restaurant);
        bind(paidTpl, DINING_PRICE_CENT, "platform_funded");
        String order = sell();
        verify(code(order, entryTpl), barStaff, bar, barDevice);
        verify(code(order, paidTpl), staff, restaurant, device);
        Map<String, Object> ride = jdbc.queryForMap("SELECT id, allocated_revenue_cent, parent_ticket_id FROM issued_entitlements"
                + " WHERE order_id = ? AND entitlement_template_id = ?", order, parkTpl);
        Instant at = Instant.now();
        settledConsumer.onSettlementSettled(new SettlementSettledEvent("vf_c176amu_" + suffix, at, (String) ride.get("id"), order,
                typeId, "amusement", park, 1, 1, 1, num(ride, "allocated_revenue_cent"), at.plusSeconds(86_400), null, null, null,
                project, (String) ride.get("parent_ticket_id")));
        moveToMidMonth(order);

        String bill = settlement.settleMonth(project, MONTH);

        assertThat(num(formatShare(bill, "amusement"), "channel_fee_cent")).as("游乐份额不再担餐饮那份").isEqualTo(100L);
        assertThat(num(formatShare(bill, "catering"), "channel_fee_cent")).isEqualTo(300L);
    }

    @Test
    void retailVoucherBuiltLikeComboRowVerifiesAtRetailMerchantAndSettlesToCityCompany() throws Exception {
        // 第十八批 AH 段 217-R1（ENUM:36「餐饮券、零售券」；ENUM:51「餐饮与零售的域主管公司为城市项目公司……直接与实际提供的餐饮、零售商户结算」）：
        // 后台组合配置「餐饮/零售」行选零售商户时按前端同键建 retail 权益（专属直付该商户）；先检票入场，零售商户员工在本店设备核销，
        // 月结记入零售业态、归城市项目公司。本夹具把直付商户的业态改成零售。
        jdbc.update("UPDATE merchants SET merchant_type = 'retail' WHERE id = ?", restaurant);
        String entryTpl = template("直付酒吧 · 入场", "entry", bar, null, null);
        bind(entryTpl, BAR_PRICE_CENT, null);
        String retailTpl = template("直付零售 · 零售", "retail", restaurant, catalogCode, restaurant);
        bind(retailTpl, DINING_PRICE_CENT, "platform_funded");
        String order = sell();
        verify(code(order, entryTpl), barStaff, bar, barDevice);
        String retailVerification = verify(code(order, retailTpl), staff, restaurant, device);

        Map<String, Object> settledRow = settled(retailVerification);
        assertThat(settledRow.get("exclusive_merchant_id")).as("零售券全额直付该零售商户").isEqualTo(restaurant);
        assertThat(num(settledRow, "verified_amount_cent")).as("直付额按内部结算价").isEqualTo(CATALOG_PRICE_CENT);
        moveToMidMonth(order);
        String company = cityCompany("城市项目公司丙");

        Map<String, Object> retail = formatShare(settlement.settleMonth(project, MONTH), "retail");
        assertThat(retail).as("零售券份额记入零售业态").isNotNull();
        assertThat(num(retail, "gross_revenue_cent")).isEqualTo(DINING_PRICE_CENT);
        assertThat(retail.get("beneficiary_entity_id")).as("ENUM:51 零售域 → 城市项目公司").isEqualTo(company);
    }

    private void moveToMidMonth(String order) {
        AppendOnlyTestBypass.update(jdbc, "UPDATE settlement_transactions SET business_at = ? WHERE order_id = ?",
                Timestamp.from(MID_MONTH), order);
    }

    private String cityCompany(String name) {
        String id = "be_c176_" + suffix + "_" + companies.size();
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'project_company', 'active', now(), now())", id, name + Long.parseLong(suffix, 16));
        jdbc.update("INSERT INTO business_entity_projects (business_entity_id, project_id) VALUES (?, ?)", id, project);
        companies.add(id);
        return id;
    }

    private Map<String, Object> formatShare(String bill, String format) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM settlement_bill_format_shares WHERE bill_id = ? AND business_format = ?", bill, format);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /** 第三方渠道（美团）售出：付款确认内核固化通路费与销售渠道费配置，付足即自动出票（同在线到账确认）。 */
    private String sell() {
        String order = "ord_c176_" + suffix;
        orders.add(order);
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency,
                    pay_status, issue_status, status, created_at, updated_at, project_id)
                VALUES (?, ?, ?, 1, 'meituan', ?, 'CNY', 'unpaid', 'not_issued', 'created', now(), now(), ?)""",
                order, "ON-" + order, typeId, ORDER_AMOUNT_CENT, project);
        String pay = "pi_" + order;
        jdbc.update("""
                INSERT INTO ticket_payment_intents (id, business_order_type, business_order_id, amount_cent, currency, channel,
                    status, created_at, updated_at) VALUES (?, 'ticket_order', ?, ?, 'CNY', 'meituan', 'created', now(), now())""",
                pay, order, ORDER_AMOUNT_CENT);
        TestSecurityActors.asPlatformAdmin();
        orderService.confirmPayment(order, new ConfirmTicketOrderPaymentRequest("c176-pay-" + suffix, pay, "gw-c176-" + suffix));
        orderService.issuePaidOrderAutomatically(order);
        TestSecurityActors.clear();
        return order;
    }

    /** 按 TicketingPoliciesPage 组合通票行的请求形状建权益模板。 */
    private String template(String name, String type, String merchant, String catalog, String exclusive) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c176-tpl-%s", "name": "%s", "entitlement_type": "%s",
                                 "default_total_times": 1, "default_value_cent": %s, "status": "active", "merchant_id": "%s",
                                 "catalog_item_code": %s, "exclusive_merchant_id": %s, "bar_item_ref": null}
                                """.formatted(UUID.randomUUID(), name, type, catalog == null ? "null" : "0", merchant,
                                quoted(catalog), quoted(exclusive))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String templateId = new ObjectMapper().readTree(body).path("data").path("id").asText();
        templateIds.add(templateId);
        return templateId;
    }

    private void bind(String templateId, long componentPriceCent, String costBearer) throws Exception {
        mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + typeId + "/entitlements").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c176-bind-%s", "entitlement_template_id": "%s", "quantity": 1,
                                 "total_times": 1, "num_people": 1, "component_price_cent": %d%s}
                                """.formatted(UUID.randomUUID(), templateId, componentPriceCent,
                                costBearer == null ? "" : ", \"cost_bearer\": \"" + costBearer + "\"")))
                .andExpect(status().isOk());
    }

    /** 商户员工在本店核销设备上核销，返回核销记录 id。 */
    private String verify(String code, String who, String merchant, String dev) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/verifications").with(session(who, "staff", project, merchant))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c176-verify-%s", "credential_type": "qr", "credential_payload": "%s",
                                 "device_id": "%s", "point_id": "point_%s", "business_context": {}}
                                """.formatted(UUID.randomUUID(), code, dev, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new ObjectMapper().readTree(body).path("data").path("id").asText();
    }

    private String code(String order, String templateId) {
        return jdbc.queryForObject("SELECT entitlement_code FROM issued_entitlements WHERE order_id = ? AND entitlement_template_id = ?",
                String.class, order, templateId);
    }

    private long allocated(String order, String templateId) {
        return jdbc.queryForObject("SELECT allocated_revenue_cent FROM issued_entitlements WHERE order_id = ? AND entitlement_template_id = ?",
                Long.class, order, templateId);
    }

    private Map<String, Object> settled(String verificationId) {
        return jdbc.queryForMap("SELECT * FROM settlement_transactions WHERE transaction_no = ?", "ST-VS-" + verificationId);
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }

    private void user(String userId, String accountType, String organizationId, String merchantId, String roleName,
                      List<String> permissions) {
        String roleId = "role_" + userId;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, 'active', now())", userId, userId, accountType, organizationId, merchantId);
        jdbc.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId, roleName);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与账号表当前行一致）。
    private RequestPostProcessor session(String userId, String accountType, String organizationId, String merchantId) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(userId, userId, accountType, organizationId, merchantId, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
