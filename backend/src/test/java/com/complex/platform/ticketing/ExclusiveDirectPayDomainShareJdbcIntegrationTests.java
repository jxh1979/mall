package com.complex.platform.ticketing;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApItemRequest;
import com.complex.platform.settlement.SettlementExpiryJob;
import com.complex.platform.settlement.SettlementSettledConsumer;
import com.complex.platform.settlement.TicketDistributionSettlementService;
import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.support.PassPoolFixtures;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.support.TicketSalesContractTestFixtures;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.fasterxml.jackson.databind.JsonNode;
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
    @Autowired private SettlementExpiryJob expiryJob;
    @Autowired private FinanceService financeService;

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
    private final String shop = "shop_c216_" + suffix;
    private final String shopCatalog = "sku_c216_" + suffix;
    private final String shopDevice = "dev_c216_shop_" + suffix;
    private final String shopStaff = "usr_c216_shop_" + suffix;
    private final String barType = "tt_c212_" + suffix;
    private final String barEntryTpl = "et_c212_" + suffix;
    private final String provider = "c212-pos-" + suffix;
    private final String channelAdmin = "usr_c212_chan_" + suffix;
    private final String seller = "usr_c212_desk_" + suffix;
    private final List<Map<String, Object>> channelSwitches = new ArrayList<>();
    private final String pureType = "tt_c237_" + suffix;

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
        for (Map<String, Object> s : channelSwitches) {
            jdbc.update("UPDATE terminal_capability_switches SET enabled = ? WHERE id = ?", s.get("enabled"), s.get("id"));
        }
        for (String table : List.of("integration_order_syncs", "integration_channel_ticket_type_map", "integration_channel_store_map")) {
            jdbc.update("DELETE FROM " + table + " WHERE provider_code = ?", provider);
        }
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
            jdbc.update("DELETE FROM single_bar_service_fee_tickets WHERE order_id = ?", order);
            AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_order_sales_contracts", "ticket_sales_snapshot_immutable",
                    "DELETE FROM ticket_order_sales_contracts WHERE order_id = ?", order);
            AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_payment_intents", "trg_append_only_d",
                    "DELETE FROM ticket_payment_intents WHERE business_order_id = ?", order);
            jdbc.update("DELETE FROM ticket_orders WHERE id = ?", order);
        }
        PassPoolFixtures.deleteByProject(jdbc, project);
        jdbc.update("DELETE FROM single_bar_service_fee_months WHERE bar_merchant_id = ?", bar);
        AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_reward_config_versions", "ticket_reward_config_immutable",
                "DELETE FROM ticket_reward_config_versions WHERE ticket_type_id = ?", barType);
        jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id IN (?, ?, ?)", typeId, barType, pureType);
        for (String id : templateIds) {
            jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM internal_price_catalog WHERE item_code IN (?, ?)", catalogCode, shopCatalog);
        jdbc.update("DELETE FROM pass_gift_cost_configs WHERE project_id = ?", project);
        jdbc.update("DELETE FROM channel_fee_configs WHERE project_id = ?", project);
        jdbc.update("DELETE FROM ticket_types WHERE id IN (?, ?, ?)", typeId, barType, pureType);
        jdbc.update("DELETE FROM terminal_devices WHERE id IN (?, ?, ?)", device, barDevice, shopDevice);
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
    void singleBarTicketRowSharesRouteFeeWithoutDomainShare() {
        // ENUM:32 单一酒吧门票（订单挂酒吧）按专属直付记到订单酒吧，不记域份额；第十八批 AI 段 212-R1（ENUM:51「所有支付都会产生通路费、
        // 官方渠道同样产生」「不由该笔业务的收款主体单独承担」）：按本次核销额分摊本单付款时固化的通路费。
        String order = singleBarOrderVerified("row");

        Map<String, Object> row = settled("vf_" + order);
        assertThat(row.get("exclusive_merchant_id")).isEqualTo(bar);
        assertThat(row.get("domain_share_cent")).isNull();
        assertThat(num(row, "route_fee_cent")).as("单店票行按本次核销额分摊本单通路费").isEqualTo(50L);
    }

    @Test
    void channelSoldSingleBarTicketPaysBothFeesOutOfTheBarsDirectPayAndAReversalRecoversTheNet() throws Exception {
        // 第十八批 AI 段 212-R1（ENUM:51「任何票种经第三方渠道售出时，销售渠道费一律先从整单扣除且只扣一次」「每笔分摊的结果与所用合同版本必须留痕」；
        // ENUM:32「单一酒吧门票由对应酒吧商户定价、销售并确认已核销部分的收入」）：美团回流的单店票，酒吧员工本店设备检票入场后，月结从该酒吧
        // 直付额先扣通路费（1%）与销售渠道费（8%，付款时固化的配置），逐商户留痕；原月之后撤销核销，下月追收＝原结付净额（决策台账 FS-021
        // 「单店按该次核销原结付金额」）。
        publishSingleBarTicket(1);
        channelMaps();
        String order = reflowSingleBar("a");
        String verification = verify(code(order, barEntryTpl), barStaff, bar, barDevice);
        Map<String, Object> row = settled(verification);
        assertThat(num(row, "route_fee_cent")).as("按本次核销额分摊本单通路费 1%").isEqualTo(50L);
        assertThat(row.get("domain_share_cent")).isNull();
        moveToMidMonth(order);

        String june = settlement.settleMonth(project, MONTH);

        Map<String, Object> item = exclusiveItem(june, "exclusive_full");
        assertThat(num(item, "final_amount_cent")).as("直付额＝5000−通路费 50−销售渠道费 8%×5000").isEqualTo(4550L);
        assertThat(num(item, "single_bar_route_fee_cent")).isEqualTo(50L);
        assertThat(num(item, "single_bar_channel_fee_cent")).isEqualTo(400L);

        mockMvc.perform(post("/api/v1/ticketing/verifications/" + verification + "/reverse").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c212-rev-%s", "reason": "核销有误撤销"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());
        AppendOnlyTestBypass.update(jdbc, "UPDATE settlement_transactions SET business_at = ? WHERE order_id = ?"
                + " AND transaction_type = 'verification_reversed'", Timestamp.from(MID_MONTH.plusSeconds(30L * 86_400)), order);

        String july = settlement.settleMonth(project, MONTH.plusMonths(1));

        assertThat(num(exclusiveItem(july, "exclusive_recovery"), "final_amount_cent"))
                .as("下月单列追收＝原结付净额（已扣两层费）").isEqualTo(-4550L);
    }

    @Test
    void officialChannelSingleBarTicketPaysOnlyTheRouteFee() throws Exception {
        // 212-R1（BSM:749「官方渠道售出的票不扣销售渠道费，但仍扣通路费」）：前台员工按后台网页下单、登记现金（现金通路费 1%）、出票，
        // 酒吧员工检票入场；月结只扣通路费。
        jdbc.update("""
                INSERT INTO channel_fee_configs (id, project_id, fee_layer, channel, fee_bps, status, is_draft)
                VALUES (?, ?, 'route', 'cash', 100, 'active', FALSE)""", "cfc_c212cash_" + suffix, project);
        publishSingleBarTicket(1);
        String order = sellAtFrontDesk();
        verify(code(order, barEntryTpl), barStaff, bar, barDevice);
        moveToMidMonth(order);

        Map<String, Object> item = exclusiveItem(settlement.settleMonth(project, MONTH), "exclusive_full");

        assertThat(num(item, "final_amount_cent")).as("官方渠道：5000−通路费 50，不扣销售渠道费").isEqualTo(4950L);
        assertThat(num(item, "single_bar_route_fee_cent")).isEqualTo(50L);
        assertThat(num(item, "single_bar_channel_fee_cent")).isZero();
    }

    @Test
    void fixedAmountSalesFeeIsTakenOncePerSingleBarTicketAndNotAgainAtExpiry() throws Exception {
        // 212-R1（ENUM:48「各项均只扣一次」；ENUM:51 同句）：销售渠道费每张固定 300。甲票入场两次只扣一道；乙票只入场一次、余下一次到期，
        // 月结已按其首笔核销扣过，到期作业不再扣。
        jdbc.update("UPDATE channel_fee_configs SET fee_bps = 0, fixed_amount_cent = 300 WHERE id = ?", "cfc_c176s_" + suffix);
        publishSingleBarTicket(2);
        channelMaps();
        String first = reflowSingleBar("b");
        String second = reflowSingleBar("c");
        verify(code(first, barEntryTpl), barStaff, bar, barDevice);
        verify(code(first, barEntryTpl), barStaff, bar, barDevice);
        verify(code(second, barEntryTpl), barStaff, bar, barDevice);
        moveToMidMonth(first);
        moveToMidMonth(second);

        Map<String, Object> item = exclusiveItem(settlement.settleMonth(project, MONTH), "exclusive_full");
        assertThat(num(item, "single_bar_channel_fee_cent")).as("两张票各扣一道 300，同票两次核销不重扣").isEqualTo(600L);

        jdbc.update("UPDATE issued_entitlements SET valid_to = ? WHERE order_id = ?",
                Timestamp.from(Instant.now().minusSeconds(2L * 86_400)), second);
        expiryJob.expireUnverified(Instant.now());

        Map<String, Object> expired = jdbc.queryForMap("SELECT channel_fee_cent, channel_fee_residual_cent FROM settlement_transactions"
                + " WHERE order_id = ? AND transaction_type = 'unverified_expired'", second);
        assertThat(num(expired, "channel_fee_cent")).as("乙票月结已按首笔核销扣过固定额，到期不再扣").isZero();
        assertThat(num(expired, "channel_fee_residual_cent")).isZero();
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
        rideVerified(order, parkTpl);
        moveToMidMonth(order);

        String bill = settlement.settleMonth(project, MONTH);

        assertThat(num(formatShare(bill, "amusement"), "channel_fee_cent")).as("游乐份额不再担餐饮那份").isEqualTo(100L);
        assertThat(num(formatShare(bill, "catering"), "channel_fee_cent")).isEqualTo(300L);
    }

    @Test
    void interEntityShareLeavesOutThePureAmusementTicketTheAmusementCompanyCollectsItself() throws Exception {
        // 第十八批 AI 段 237-R1（ENUM:32「票款由娱乐平台公司统一收款」「收款主体不等于收入归属主体」「其余各域份额自收款之日起即为应付给对应域主管公司
        // 的款项」「纯游乐票和纯游乐通票由游乐公司负责」；BSM:1720「与娱乐平台公司无关」；BSM:1754「列明第三方渠道费用、域间份额」）：组合通票（酒吧入场
        // 4000＋游乐 1000＋有价餐饮 3000）与只含游乐权益的纯游乐票 2000 同月美团售出并核销；游乐行毛额两张合计，域间份额只算组合通票代收的那份。
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
        rideVerified(order, parkTpl);
        verify(code(order, paidTpl), staff, restaurant, device);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)"
                + " VALUES (?, '纯游乐票', 'fixed', FALSE, 'draft', now(), now())", pureType);
        String pureTpl = template("直付游乐 · 纯游乐", "amusement", park, null, null);
        bind(pureType, pureTpl, 2000L, null);
        String pure = sell("ord_c237_" + suffix, pureType, 2000L);
        rideVerified(pure, pureTpl);
        moveToMidMonth(order);
        moveToMidMonth(pure);

        String bill = settlement.settleMonth(project, MONTH);

        Map<String, Object> amusement = formatShare(bill, "amusement");
        assertThat(num(amusement, "gross_revenue_cent")).as("游乐行毛额＝组合票游乐份额 1000＋纯游乐票 2000").isEqualTo(3000L);
        assertThat(num(amusement, "net_revenue_cent")).as("3000−销售渠道费 8%×3000−通路费 10＋20").isEqualTo(2730L);
        assertThat(num(amusement, "inter_entity_net_cent"))
                .as("扣纯游乐票那张净额：2000−通路费 20−按毛额从本行摊回的销售渠道费 240×2000÷3000").isEqualTo(910L);
        assertThat(num(formatShare(bill, "catering"), "inter_entity_net_cent")).as("餐饮行域间份额＝净份额 3000−240−30").isEqualTo(2730L);
        assertThat(num(formatShare(bill, "bar"), "inter_entity_net_cent")).as("酒吧域份额是娱乐平台公司自身收入").isZero();
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

    @Test
    void zeroPricePlatformFundedDiningAndRetailDirectPayCountsIntoGiftActualPaid() throws Exception {
        // 第十八批 AI 段 216-R1（ENUM:52「按件计＝票面各权益数量乘提供方内部结算价之和……该笔预扣归娱乐平台公司，预扣数与日后实付数的差额也留在
        // 娱乐平台公司」；BSM:752 同句；ENUM:37「免费型折扣券即零元免费权益」）：组合通票送平台出资的零元餐饮券、零售券，预扣 G 按件计含其内部结算价；
        // 客人核销后按内部结算价直付给餐厅、零售店的钱即实付 A，差额台账与业态份额行如实记；有价餐饮行（记域份额）、商户让利零元行与单一酒吧门票行不进 A。
        long n = Long.parseLong(suffix, 16);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'retail', 'active', ?, now(), now())", shop, "直付零售店" + n, project);
        jdbc.update("""
                INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name,
                    price_cent, status, is_draft, updated_by, created_at, updated_at)
                VALUES (?, ?, ?, 'retail', '纪念品', 1000, 'active', FALSE, 'seed_c216', now(), now())
                """, shopCatalog, shopCatalog, shop);
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, now(), now())", shopDevice, shopDevice, shopDevice, shop);
        user(shopStaff, "staff", project, shop, "直付零售核销员" + n, List.of("verification.execute"));
        jdbc.update("""
                INSERT INTO pass_gift_cost_configs (id, project_id, ticket_type_id, prededuct_mode, ticket_gift_amount_cent,
                    variance_handling, status, is_draft, updated_by)
                VALUES (?, ?, ?, 'by_item', NULL, 'platform_absorb', 'active', FALSE, 'seed_c216')""", "pgc_c216_" + suffix, project, typeId);
        String entryTpl = template("直付酒吧 · 入场", "entry", bar, null, null);
        bind(entryTpl, BAR_PRICE_CENT, null);
        String paidTpl = template("直付餐厅 · 餐饮（有价）", "dining", restaurant, catalogCode, restaurant);
        bind(paidTpl, DINING_PRICE_CENT, "platform_funded");
        String freeTpl = template("直付餐厅 · 餐饮（零元平台出资）", "dining", restaurant, catalogCode, restaurant);
        bind(freeTpl, 0L, "platform_funded");
        String concessionTpl = template("直付餐厅 · 餐饮（零元商户让利）", "dining", restaurant, catalogCode, restaurant);
        bind(concessionTpl, 0L, "merchant_concession");
        String retailTpl = template("直付零售 · 零售（零元平台出资）", "retail", shop, shopCatalog, shop);
        bind(retailTpl, 0L, "platform_funded");
        String order = sell();
        verify(code(order, entryTpl), barStaff, bar, barDevice);
        verify(code(order, paidTpl), staff, restaurant, device);
        String freeVerification = verify(code(order, freeTpl), staff, restaurant, device);
        verify(code(order, concessionTpl), staff, restaurant, device);
        verify(code(order, retailTpl), shopStaff, shop, shopDevice);
        moveToMidMonth(order);
        moveToMidMonth(singleBarOrderVerified("gift"));

        String bill = settlement.settleMonth(project, MONTH);

        Map<String, Object> ledger = giftLedgerNet(bill);
        assertThat(num(ledger, "accrued")).as("预扣 G 按件计＝零元平台出资餐饮 2000＋零售 1000").isEqualTo(3000L);
        assertThat(num(ledger, "actual")).as("实付 A＝按内部结算价直付餐厅 2000＋零售店 1000；有价餐饮、商户让利、单店票行不进")
                .isEqualTo(3000L);
        assertThat(num(ledger, "variance")).isZero();
        assertThat(num(formatShare(bill, "catering"), "gift_actual_paid_cent")).as("兑付成本按提供方业态分记").isEqualTo(2000L);
        assertThat(num(formatShare(bill, "retail"), "gift_actual_paid_cent")).isEqualTo(1000L);

        mockMvc.perform(post("/api/v1/ticketing/verifications/" + freeVerification + "/reverse").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c216-rev-%s", "reason": "核销有误撤销"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());
        moveToMidMonth(order);
        Map<String, Object> rerun = giftLedgerNet(settlement.settleMonth(project, MONTH));
        assertThat(num(rerun, "actual")).as("同月撤销餐厅核销后重结：A 减 2000").isEqualTo(1000L);
        assertThat(num(rerun, "variance")).isEqualTo(2000L);
    }

    @Test
    void billReviewListsDomainSharesAllocationsAndInternalPriceByProviderToPlatformFinanceOnly() throws Exception {
        // 第十八批 AI 段 237-R2（BSM:1754「通票结算审阅财务侧设置独立审阅台账，按项目、经营域、责任主体和实际提供方列明第三方渠道费用、域间份额、
        // 域内分配、内部结算价权益、酒吧入场可分配池和责任主体承担的差额」；ENUM:53「自行承担差额」；ENUM:52「仅其他酒吧分得的具体金额不对其开放」）：
        // 组合通票（酒吧入场 4000＋游乐 1000＋有价餐饮 3000＋零元平台出资餐饮）美团售出，先检票入场再核销其余，出品酒吧兑付赠饮（内部结算价 800，
        // 经结算消费者落事实、按赠饮兑换同一请求挂应付）；另售纯游乐票并核销、美团单店票并检票入场；第二张组合票只有游乐到期。月结后平台财务读审阅明细。
        long n = Long.parseLong(suffix, 16);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'amusement', 'active', ?, now(), now())", park, "直付游乐" + n, project);
        String entryTpl = template("直付酒吧 · 入场", "entry", bar, null, null);
        bind(entryTpl, 4000L, null);
        String parkTpl = template("直付游乐 · 游乐", "amusement", park, null, null);
        bind(parkTpl, 1000L, null);
        String paidTpl = template("直付餐厅 · 餐饮（有价）", "dining", restaurant, catalogCode, restaurant);
        bind(paidTpl, DINING_PRICE_CENT, "platform_funded");
        String freeTpl = template("直付餐厅 · 餐饮（零元平台出资）", "dining", restaurant, catalogCode, restaurant);
        bind(freeTpl, 0L, "platform_funded");
        String order = sell();
        verify(code(order, entryTpl), barStaff, bar, barDevice);
        rideVerified(order, parkTpl);
        verify(code(order, paidTpl), staff, restaurant, device);
        verify(code(order, freeTpl), staff, restaurant, device);
        String drink = "vf_c237drink_" + order;
        Instant at = Instant.now();
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(drink, at, "ent_c237drink_" + suffix, order, typeId, "drink",
                bar, 1, 1, 1, 0L, at.plusSeconds(86_400), null, null, null, project,
                jdbc.queryForObject("SELECT parent_ticket_id FROM issued_entitlements WHERE order_id = ? AND entitlement_template_id = ?",
                        String.class, order, entryTpl)));
        TestSecurityActors.asPlatformAdmin();
        financeService.createArApItem(new ArApItemRequest("bar-gift-settle-" + drink, "platform-to-bar pass drink settlement: verification "
                + drink, List.of(), "payable", bar, "pass_drink_settlement", drink, 800L, java.time.LocalDate.now().plusDays(30),
                YearMonth.now().toString()));
        TestSecurityActors.clear();
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)"
                + " VALUES (?, '纯游乐票', 'fixed', FALSE, 'draft', now(), now())", pureType);
        String pureTpl = template("直付游乐 · 纯游乐", "amusement", park, null, null);
        bind(pureType, pureTpl, 2000L, null);
        String pure = sell("ord_c237_" + suffix, pureType, 2000L);
        rideVerified(pure, pureTpl);
        publishSingleBarTicket(1);
        channelMaps();
        String single = reflowSingleBar("rv");
        verify(code(single, barEntryTpl), barStaff, bar, barDevice);
        String later = sell("ord_c237b_" + suffix, typeId, ORDER_AMOUNT_CENT);
        jdbc.update("UPDATE issued_entitlements SET valid_to = ? WHERE order_id = ? AND entitlement_template_id = ?",
                Timestamp.from(Instant.now().minusSeconds(2L * 86_400)), later, parkTpl);
        expiryJob.expireUnverified(Instant.now());
        for (String o : List.of(order, pure, single, later)) {
            moveToMidMonth(o);
        }
        // 单店票的销售合同夹具已为本综合体登记一家项目公司（城市项目公司），餐饮域归它。
        String company = jdbc.queryForObject("SELECT business_entity_id FROM business_entity_projects WHERE project_id = ?",
                String.class, project);
        String bill = settlement.settleMonth(project, MONTH);

        String finance = "usr_c237_fin_" + suffix;
        user(finance, "staff", "org_1", null, "通票结算审阅财务" + n, List.of("finance.settlement.view"));
        JsonNode review = new ObjectMapper().readTree(mockMvc.perform(get("/api/v1/settlement/bills/" + bill + "/review")
                        .with(session(finance, "staff", "org_1", null)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");

        JsonNode catering = row(review.path("domain_shares"), "business_format", "catering");
        assertThat(catering.path("beneficiary_entity_id").asText()).isEqualTo(company);
        assertThat(catering.path("beneficiary_entity_name").asText()).as("责任主体名").isEqualTo("售票夹具项目公司");
        assertThat(catering.path("channel_fee_cent").asLong()).as("第三方渠道费用 8%×3000").isEqualTo(240L);
        assertThat(catering.path("route_fee_cent").asLong()).isEqualTo(30L);
        assertThat(catering.path("net_revenue_cent").asLong()).isEqualTo(2730L);
        assertThat(catering.path("inter_entity_net_cent").asLong()).as("域间份额").isEqualTo(2730L);
        assertThat(catering.path("internal_settled_cent").asLong()).as("内部结算价直付").isEqualTo(CATALOG_PRICE_CENT);
        assertThat(catering.path("difference_cent").asLong()).as("ENUM:53 责任主体所得差额 2730−2000").isEqualTo(730L);
        JsonNode amusement = row(review.path("domain_shares"), "business_format", "amusement");
        assertThat(amusement.path("beneficiary_entity_id").asText()).as("游乐域 → 游乐公司").isEqualTo("be_amusement");
        assertThat(amusement.path("beneficiary_entity_name").asText()).isEqualTo(
                jdbc.queryForObject("SELECT entity_name FROM business_entities WHERE id = 'be_amusement'", String.class));
        assertThat(amusement.path("inter_entity_net_cent").asLong()).as("域间份额不含纯游乐票").isEqualTo(910L);
        assertThat(amusement.path("unverified_net_cent").asLong()).as("第二张组合票游乐到期 1000−通路费 10−销售渠道费 80")
                .isEqualTo(910L);
        assertThat(amusement.path("difference_cent").isNull()).as("无内部结算价直付的行不列差额").isTrue();

        JsonNode allocations = review.path("allocations");
        assertThat(allocations.size()).as("本单全部分账明细都列出").isEqualTo(
                jdbc.queryForObject("SELECT count(*) FROM settlement_items WHERE bill_id = ?", Integer.class, bill));
        JsonNode dining = row(allocations, "merchant_id", restaurant, "distribution_method", "exclusive_full");
        assertThat(dining.path("merchant_name").asText()).isEqualTo("直付餐厅" + n);
        assertThat(dining.path("business_format").asText()).isEqualTo("catering");
        assertThat(dining.path("final_amount_cent").asLong()).as("有价、零元两行各按内部结算价直付").isEqualTo(2 * CATALOG_PRICE_CENT);
        JsonNode singleBar = row(allocations, "merchant_id", bar, "distribution_method", "exclusive_full");
        assertThat(singleBar.path("final_amount_cent").asLong()).isEqualTo(4550L);
        assertThat(singleBar.path("single_bar_route_fee_cent").asLong()).as("单店门票通路费").isEqualTo(50L);
        assertThat(singleBar.path("single_bar_channel_fee_cent").asLong()).as("单店门票销售渠道费").isEqualTo(400L);

        JsonNode providers = review.path("internal_price_by_provider");
        JsonNode restaurantPrice = row(providers, "merchant_id", restaurant);
        assertThat(restaurantPrice.path("paid_direct_settled_cent").asLong()).as("有价餐饮按内部结算价直付").isEqualTo(CATALOG_PRICE_CENT);
        assertThat(restaurantPrice.path("free_direct_settled_cent").asLong()).as("零元平台出资餐饮直付").isEqualTo(CATALOG_PRICE_CENT);
        assertThat(restaurantPrice.path("gift_drink_paid_cent").asLong()).isZero();
        JsonNode barPrice = row(providers, "merchant_id", bar);
        assertThat(barPrice.path("gift_drink_paid_cent").asLong()).as("出品酒吧赠饮内部结算价").isEqualTo(800L);
        assertThat(barPrice.path("paid_direct_settled_cent").asLong() + barPrice.path("free_direct_settled_cent").asLong())
                .as("单店门票票款不是内部结算价权益").isZero();

        String noView = "usr_c237_nov_" + suffix;
        user(noView, "staff", "org_1", null, "无结算审阅权限" + n, List.of("verification.execute"));
        mockMvc.perform(get("/api/v1/settlement/bills/" + bill + "/review").with(session(noView, "staff", "org_1", null)))
                .andExpect(status().isForbidden());
        String barFinance = "usr_c237_barfin_" + suffix;
        user(barFinance, "staff", project, bar, "酒吧结算查看" + n, List.of("finance.settlement.view"));
        mockMvc.perform(get("/api/v1/settlement/bills/" + bill + "/review").with(session(barFinance, "staff", project, bar)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
        mockMvc.perform(get("/api/v1/settlement/bills/sb_c237_none_" + suffix + "/review").with(session(finance, "staff", "org_1", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("SETTLEMENT_BILL_NOT_FOUND"));
    }

    /** 212-R1：已发布的酒吧单店门票（每张 5000 分、一项入场可用 entryTimes 次），有已批准的销售合同。 */
    private void publishSingleBarTicket(int entryTimes) {
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,"
                + " holiday_price_cent, stock_required, status, validity_days, bar_id, created_at, updated_at)"
                + " VALUES (?, '单店酒吧门票', 'flat', 5000, 5000, 5000, FALSE, 'published', 1, ?, now(), now())", barType, bar);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " merchant_id, entitlement_kind, consumption_window, created_at, updated_at)"
                + " VALUES (?, '单店入场', 'entry', ?, 5000, 'active', ?, 'virtual', 'per_use', now(), now())", barEntryTpl, entryTimes, bar);
        templateIds.add(barEntryTpl);
        jdbc.update("INSERT INTO ticket_type_entitlements (id, ticket_type_id, entitlement_template_id, quantity, total_times,"
                + " num_people, value_amount_cent, component_price_cent, created_at, updated_at)"
                + " VALUES (?, ?, ?, 1, ?, 1, 5000, 5000, now(), now())", "bind_c212_" + suffix, barType, barEntryTpl, entryTimes);
        TicketSalesContractTestFixtures.approvedPublished(jdbc, new ObjectMapper(), barType, project, bar);
    }

    /** 212-R1：按后台真实请求建美团门店映射与票种映射，开第三方渠道回流开关（原值在 @AfterEach 复原）。 */
    private void channelMaps() throws Exception {
        channelSwitches.addAll(jdbc.queryForList("SELECT id, enabled FROM terminal_capability_switches"
                + " WHERE capability_code = 'integration.ticket_channel.sync'"));
        jdbc.update("UPDATE terminal_capability_switches SET enabled = TRUE WHERE capability_code = 'integration.ticket_channel.sync'");
        user(channelAdmin, "platform_admin", project, null, "渠道回流配置" + Long.parseLong(suffix, 16),
                List.of("integration.channel_store_map.manage", "integration.order_sync.manage", "ticket.order.create",
                        "finance.payment_intent.create", "finance.payment.register", "ticket.ticket.issue"));
        mockMvc.perform(post("/api/v1/integration/channel-store-maps").with(session(channelAdmin, "platform_admin", project, null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c212-store-%s", "provider_code": "%s", "channel": "meituan",
                                 "external_store_id": "mt-store-%s", "merchant_id": "%s"}
                                """.formatted(suffix, provider, suffix, bar)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/integration/channel-ticket-type-maps").with(session(channelAdmin, "platform_admin", project, null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c212-product-%s", "provider_code": "%s", "channel": "meituan",
                                 "external_product_id": "mt-prod-%s", "ticket_type_id": "%s"}
                                """.formatted(suffix, provider, suffix, barType)))
                .andExpect(status().isOk());
    }

    /** 212-R1：美团订单回流登记（按后台真实请求形状），本地按票种所属酒吧出票，返回本地订单号。 */
    private String reflowSingleBar(String tag) throws Exception {
        String body = mockMvc.perform(post("/api/v1/integration/order-syncs").with(session(channelAdmin, "platform_admin", project, null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c212-reflow-%s-%s", "provider_code": "%s", "business_type": "ticket",
                                 "sync_type": "order", "external_business_id": "mt-order-%s-%s", "status": "received",
                                 "amount_cent": 5000, "currency": "CNY", "channel": "meituan", "external_channel_order_id": "mtord-%s-%s",
                                 "provider_payload": {"external_store_id": "mt-store-%s", "external_product_id": "mt-prod-%s", "quantity": 1}}
                                """.formatted(tag, suffix, provider, tag, suffix, tag, suffix, suffix, suffix)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String order = new ObjectMapper().readTree(body).path("data").path("provider_payload").path("local_ticket_order_id").asText();
        assertThat(order).as("渠道回流的单店票本地出票").isNotBlank();
        orders.add(order);
        return order;
    }

    /** 212-R1：前台员工按后台网页请求体（蛇形键）下单、登记现金收款、确认收款后出票，返回订单号。 */
    private String sellAtFrontDesk() throws Exception {
        user(seller, "staff", project, null, "单店票前台售票员" + Long.parseLong(suffix, 16), List.of("ticket.order.create",
                "ticket.order.read", "finance.payment_intent.create", "finance.payment.register", "ticket.ticket.issue"));
        RequestPostProcessor desk = session(seller, "staff", project, null);
        ObjectMapper json = new ObjectMapper();
        String created = mockMvc.perform(post("/api/v1/ticketing/orders").with(desk).contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c212-order-%s", "ticket_type_id": "%s", "quantity": 1, "sales_channel": "admin", "bar_id": "%s"}
                        """.formatted(suffix, barType, bar)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String order = json.readTree(created).path("data").path("id").asText();
        orders.add(order);
        long amount = json.readTree(created).path("data").path("amount_cent").asLong();
        String intent = json.readTree(mockMvc.perform(post("/api/v1/ticketing/orders/" + order + "/pay-intents").with(desk)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c212-intent-%s", "business_order_type": "ticket_order", "business_order_id": "%s",
                                 "amount_cent": %d, "currency": "CNY", "channel": "cash"}
                                """.formatted(suffix, order, amount)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asText();
        mockMvc.perform(post("/api/v1/ticketing/orders/" + order + "/confirm-payment").with(desk)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c212-confirm-%s", "payment_intent_id": "%s"}
                                """.formatted(suffix, intent)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/ticketing/orders/" + order + "/issue").with(desk)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c212-issue-%s", "reason": "客人在前台购票"}
                                """.formatted(suffix)))
                .andExpect(status().isOk());
        return order;
    }

    private Map<String, Object> exclusiveItem(String bill, String method) {
        return jdbc.queryForMap("SELECT * FROM settlement_items WHERE bill_id = ? AND merchant_id = ? AND distribution_method = ?",
                bill, bar, method);
    }

    /** 赠付差额台账本单净额（追加式，冲正行自带负号）。 */
    private Map<String, Object> giftLedgerNet(String bill) {
        return jdbc.queryForMap("""
                SELECT COALESCE(SUM(accrued_cost_cent), 0) AS accrued, COALESCE(SUM(actual_paid_cent), 0) AS actual,
                       COALESCE(SUM(variance_cent), 0) AS variance
                  FROM pass_gift_cost_variance_ledger WHERE bill_id = ?""", bill);
    }

    /**
     * 单一酒吧门票（订单挂酒吧）经美团售出、本单通路费 50 已固化，酒吧核销入场一次（结算消费者真路径），返回本单号。
     * ENUM:32 单一酒吧门票按专属直付记到订单酒吧。
     */
    private String singleBarOrderVerified(String tag) {
        String order = "ord_c176sb_" + tag + "_" + suffix;
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
                "vf_" + order, at, "ent_" + order, order, typeId, "entry", bar,
                1, 1, 1, 5000L, at.plusSeconds(86_400), null, null, null, project, "tkt_" + order));
        return order;
    }

    /** 游乐入场核销走结算消费者真路径（游乐闸机入口属游乐域，本类不建游乐设备）。 */
    private void rideVerified(String order, String templateId) {
        Map<String, Object> ride = jdbc.queryForMap("SELECT e.id, e.allocated_revenue_cent, e.parent_ticket_id, o.ticket_type_id"
                + " FROM issued_entitlements e JOIN ticket_orders o ON o.id = e.order_id"
                + " WHERE e.order_id = ? AND e.entitlement_template_id = ?", order, templateId);
        Instant at = Instant.now();
        settledConsumer.onSettlementSettled(new SettlementSettledEvent("vf_c176amu_" + order, at, (String) ride.get("id"), order,
                (String) ride.get("ticket_type_id"), "amusement", park, 1, 1, 1, num(ride, "allocated_revenue_cent"),
                at.plusSeconds(86_400), null, null, null, project, (String) ride.get("parent_ticket_id")));
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
        return sell("ord_c176_" + suffix, typeId, ORDER_AMOUNT_CENT);
    }

    private String sell(String order, String type, long amount) {
        orders.add(order);
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency,
                    pay_status, issue_status, status, created_at, updated_at, project_id)
                VALUES (?, ?, ?, 1, 'meituan', ?, 'CNY', 'unpaid', 'not_issued', 'created', now(), now(), ?)""",
                order, "ON-" + order, type, amount, project);
        String pay = "pi_" + order;
        jdbc.update("""
                INSERT INTO ticket_payment_intents (id, business_order_type, business_order_id, amount_cent, currency, channel,
                    status, created_at, updated_at) VALUES (?, 'ticket_order', ?, ?, 'CNY', 'meituan', 'created', now(), now())""",
                pay, order, amount);
        TestSecurityActors.asPlatformAdmin();
        orderService.confirmPayment(order, new ConfirmTicketOrderPaymentRequest("c176-pay-" + order, pay, "gw-c176-" + order));
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
        bind(typeId, templateId, componentPriceCent, costBearer);
    }

    private void bind(String type, String templateId, long componentPriceCent, String costBearer) throws Exception {
        mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + type + "/entitlements").with(session(admin, "platform_admin", "org_1", null))
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

    /** 237-R2：审阅明细数组里各键值都匹配的第一行。 */
    private static JsonNode row(JsonNode rows, String... keyValues) {
        for (JsonNode r : rows) {
            boolean match = true;
            for (int i = 0; i < keyValues.length; i += 2) {
                match &= keyValues[i + 1].equals(r.path(keyValues[i]).asText());
            }
            if (match) {
                return r;
            }
        }
        throw new AssertionError("审阅明细无匹配行 " + String.join(",", keyValues) + "：" + rows);
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
