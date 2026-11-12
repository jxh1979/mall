package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.settlement.SettlementExpiryJob;
import com.complex.platform.settlement.SettlementSettledConsumer;
import com.complex.platform.settlement.TicketDistributionSettlementService;
import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.support.InternalPayDayFixtures;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 AI 段第 214 条 214-R1、214-R2（ENUM:32「票款由娱乐平台公司统一收款」「其余各域份额自收款之日起即为应付给对应域主管公司的款项」；
 * BSM:749「其份额由跨域组合通票的收款主体（娱乐平台公司）直接与实际提供的餐饮、零售商户结算」；BSM:755「一律按总额收付、收支两条线，不得轧差」；
 * BSM:1720「与娱乐平台公司无关」；ENUM:49「其一，到期未核销票款」；第十八批裁决 51「当月核销形成的份额挂在核销所在月；从未核销、到期的部分在到期所在月
 * 另挂一笔；都等当月数据标记最终、月结出单后才挂」；决策台账 FS-021「单独列追收、按原月重算」）：月结出单后过账，娱乐平台公司对游乐公司、城市项目公司
 * 挂组合通票域间份额应付，对城市项目公司挂餐饮零售代付应收，到期未核销归属另挂一笔；纯游乐票与酒吧份额不挂；原月已过账后撤销在下月单列追收；
 * 归属主体未定或新钱路未配付账日规则时整单拒绝过账。
 *
 * <p>夹具同 {@link ExclusiveDirectPayDomainShareJdbcIntegrationTests}：按票务策略页同键建模板、绑票种，美团售出（通路费 1%、销售渠道费 8%）由付款确认内核
 * 固化两层费并出票；酒吧、餐厅员工在本店设备核销，游乐入场核销走结算消费者真路径（同该类 176-R2 用例）；平台管理员经网关过账、撤销核销。
 * 放 ticketing 包：售出要调包内的付款确认内核（同该类）。结算事实移到 2017-06 月中，综合体今天建档，该月营业日不查最终标记；按项目月结。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class PassDomainShareInterEntityPostingJdbcTests {

    private static final YearMonth MONTH = YearMonth.of(2017, 6);
    /** 北京 2017-06-15 14:00：月中，任何营业日配置下都属 6-15。 */
    private static final Instant MID_MONTH = Instant.parse("2017-06-15T06:00:00Z");
    private static final String AMUSEMENT_COMPANY = "be_amusement";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TicketOrderService orderService;
    @Autowired private SettlementSettledConsumer settledConsumer;
    @Autowired private TicketDistributionSettlementService settlement;
    @Autowired private SettlementExpiryJob expiryJob;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final long n = Long.parseLong(suffix, 16);
    private final String project = "prj_c214_" + suffix;
    private final String bar = "bar_c214_" + suffix;
    private final String restaurant = "rest_c214_" + suffix;
    private final String park = "park_c214_" + suffix;
    private final String admin = "usr_c214_admin_" + suffix;
    private final String staff = "usr_c214_staff_" + suffix;
    private final String barStaff = "usr_c214_bar_" + suffix;
    private final String device = "dev_c214_" + suffix;
    private final String barDevice = "dev_c214_bar_" + suffix;
    private final String comboType = "tt_c214_" + suffix;
    private final String pureType = "tt_c214p_" + suffix;
    private final String catalogCode = "sku_c214_" + suffix;
    private final List<String> templateIds = new ArrayList<>();
    private final List<String> orders = new ArrayList<>();
    private final List<String> companies = new ArrayList<>();
    private String entryTpl;
    private String rideTpl;
    private String diningTpl;
    private String pureTpl;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '域间份额综合体', 'active', now(), now())", project);
        merchant(bar, "域间份额酒吧", "bar");
        merchant(restaurant, "域间份额餐厅", "catering");
        merchant(park, "域间份额游乐", "amusement");
        jdbc.update("""
                INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name,
                    price_cent, status, is_draft, updated_by, created_at, updated_at)
                VALUES (?, ?, ?, 'dining', '套餐', 2000, 'active', FALSE, 'seed_c214', now(), now())
                """, catalogCode, catalogCode, restaurant);
        for (String[] t : List.of(new String[] {comboType, "跨域组合通票"}, new String[] {pureType, "纯游乐票"})) {
            jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)"
                    + " VALUES (?, ?, 'fixed', FALSE, 'draft', now(), now())", t[0], t[1]);
        }
        for (String[] d : List.of(new String[] {device, restaurant}, new String[] {barDevice, bar})) {
            jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, merchant_id,"
                    + " created_at, updated_at) VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, now(), now())", d[0], d[0], d[0], d[1]);
        }
        jdbc.update("""
                INSERT INTO channel_fee_configs (id, project_id, fee_layer, channel, fee_bps, status, is_draft)
                VALUES (?, ?, 'route', 'meituan', 100, 'active', FALSE)""", "cfc_c214r_" + suffix, project);
        jdbc.update("""
                INSERT INTO channel_fee_configs (id, project_id, fee_layer, channel, fee_bps, status, is_draft)
                VALUES (?, ?, 'sales', 'meituan', 800, 'active', FALSE)""", "cfc_c214s_" + suffix, project);
        user(admin, "platform_admin", "org_1", null, "域间份额配置过账" + n,
                List.of("ticket.entitlement.manage", "ticket.product.manage", "ticket.verify.reverse", "settlement.bill.post"));
        user(staff, "staff", project, restaurant, "域间份额餐厅核销员" + n, List.of("verification.execute"));
        user(barStaff, "staff", project, bar, "域间份额酒吧检票员" + n, List.of("verification.execute"));
        InternalPayDayFixtures.seed(jdbc);
        // 组合通票：酒吧入场 4000＋游乐 1000＋有价餐饮专属行 3000（目录价 2000，平台出资）；纯游乐票只含游乐 2000。
        entryTpl = template("域间份额酒吧 · 入场", "entry", bar, null, null);
        bind(comboType, entryTpl, 4000L, null);
        rideTpl = template("域间份额游乐 · 游乐", "amusement", park, null, null);
        bind(comboType, rideTpl, 1000L, null);
        diningTpl = template("域间份额餐厅 · 餐饮（有价）", "dining", restaurant, catalogCode, restaurant);
        bind(comboType, diningTpl, 3000L, "platform_funded");
        pureTpl = template("域间份额游乐 · 纯游乐", "amusement", park, null, null);
        bind(pureType, pureTpl, 2000L, null);
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
            removeCompany(company);
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
        jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id IN (?, ?)", comboType, pureType);
        for (String id : templateIds) {
            jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM internal_price_catalog WHERE item_code = ?", catalogCode);
        jdbc.update("DELETE FROM channel_fee_configs WHERE project_id = ?", project);
        jdbc.update("DELETE FROM ticket_types WHERE id IN (?, ?)", comboType, pureType);
        jdbc.update("DELETE FROM terminal_devices WHERE id IN (?, ?)", device, barDevice);
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
    }

    @Test
    void monthEndPostingOwesEachDomainEntityItsShareSeparatelyAndRecoversNextMonthAfterReversal() throws Exception {
        String company = cityCompany("城市项目公司甲");
        String combo = sell("a", comboType, 8000L);
        verify(code(combo, entryTpl), barStaff, bar, barDevice);
        rideVerified(combo, rideTpl);
        String dining = verify(code(combo, diningTpl), staff, restaurant, device);
        String pure = sell("p", pureType, 2000L);
        rideVerified(pure, pureTpl);
        moveToMidMonth(combo);
        moveToMidMonth(pure);

        String june = settlement.settleMonth(project, MONTH);
        postBill(june).andExpect(status().isOk());

        // 游乐行净份额 3000−销售渠道费 240−通路费 30＝2730，其中纯游乐票那张 2000−20−160＝1820 由游乐公司自收（BSM:1720），域间份额 910。
        assertThat(arAp(june + "|amusement")).as("对游乐公司挂组合通票游乐份额，纯游乐票那张不挂")
                .containsExactly("payable pass_domain_share " + AMUSEMENT_COMPANY + " 910");
        assertThat(arAp(june + "|catering")).as("对城市项目公司：域间份额 3000−240−30 应付与代付餐厅 2000 应还分笔，不轧差")
                .containsExactly("payable pass_domain_share " + company + " 2730",
                        "receivable pass_domain_direct_settlement " + company + " 2000");
        assertThat(arAp(june + "|bar")).as("酒吧域份额是娱乐平台公司自身收入，不挂").isEmpty();
        assertThat(sharePostings(june)).isEqualTo(3);
        postBill(june).andExpect(status().isConflict());
        assertThat(sharePostings(june)).as("重复过账不重复挂").isEqualTo(3);

        // 6 月单过账后，7 月撤销餐饮核销：7 月单列追收（FS-021「单独列追收、按原月重算」；BSM:804「再冲回域间份额」）。
        mockMvc.perform(post("/api/v1/ticketing/verifications/" + dining + "/reverse").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c214-rev-%s", "reason": "核销有误撤销"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());
        AppendOnlyTestBypass.update(jdbc, "UPDATE settlement_transactions SET business_at = ? WHERE order_id = ?"
                + " AND transaction_type = 'verification_reversed'", Timestamp.from(MID_MONTH.plusSeconds(30L * 86_400)), combo);

        String july = settlement.settleMonth(project, MONTH.plusMonths(1));

        assertThat(col(july, "catering", "recovery_net_cent")).as("按原月重算餐饮域间份额之差").isEqualTo(-2730L);
        assertThat(col(july, "catering", "recovery_internal_settled_cent")).isEqualTo(-2000L);
        assertThat(col(july, "catering", "inter_entity_net_cent")).as("当月本身无核销").isZero();
        postBill(july).andExpect(status().isOk());
        assertThat(arAp(july + "|catering|rcv")).as("向城市项目公司追回域间份额、退还其已还的代付额，分笔")
                .containsExactly("payable pass_domain_direct_settlement " + company + " 2000",
                        "receivable pass_domain_share " + company + " 2730");
    }

    @Test
    void expiredUnverifiedPartsArePostedApartFromVerifiedSharesWhileBarAndPureAmusementAreNot() throws Exception {
        String company = cityCompany("城市项目公司乙");
        // 甲票检票入场并核销餐饮，游乐未核销；丙票从未入场；纯游乐票从未核销。三张都到期。
        String first = sell("a", comboType, 8000L);
        verify(code(first, entryTpl), barStaff, bar, barDevice);
        verify(code(first, diningTpl), staff, restaurant, device);
        String never = sell("c", comboType, 8000L);
        String pure = sell("p", pureType, 2000L);
        jdbc.update("UPDATE issued_entitlements SET valid_to = ? WHERE order_id IN (?, ?, ?)",
                Timestamp.from(Instant.now().minusSeconds(2L * 86_400)), first, never, pure);
        expiryJob.expireUnverified(Instant.now());
        for (String order : List.of(first, never, pure)) {
            moveToMidMonth(order);
        }

        String june = settlement.settleMonth(project, MONTH);

        assertThat(col(june, "amusement", "unverified_net_cent"))
                .as("甲、丙两张组合票游乐部分各 1000−通路费 10−销售渠道费 80；纯游乐票到期归游乐公司自收、不进").isEqualTo(1820L);
        assertThat(col(june, "catering", "unverified_net_cent")).as("丙票餐饮到期 3000−30−240").isEqualTo(2730L);
        assertThat(col(june, "catering", "inter_entity_net_cent")).as("甲票餐饮核销份额另列").isEqualTo(2730L);
        assertThat(col(june, "bar", "unverified_net_cent")).as("酒吧部分到期归娱乐平台公司自身，不聚合").isZero();
        postBill(june).andExpect(status().isOk());
        assertThat(arAp(june + "|amusement|ue")).containsExactly("payable pass_unverified_attribution " + AMUSEMENT_COMPANY + " 1820");
        assertThat(arAp(june + "|catering|ue")).containsExactly("payable pass_unverified_attribution " + company + " 2730");
        assertThat(arAp(june + "|catering")).as("同月核销份额与到期归属两笔分立")
                .containsExactly("payable pass_domain_share " + company + " 2730",
                        "receivable pass_domain_direct_settlement " + company + " 2000");
        assertThat(sharePostings(june)).as("游乐无核销份额、酒吧不挂").isEqualTo(4);
        postBill(june).andExpect(status().isConflict());
        assertThat(sharePostings(june)).as("重复过账不重复挂").isEqualTo(4);
    }

    @Test
    void wholeBillIsRefusedWhileTheCateringEntityIsUnsettledOrANewRouteHasNoApprovedPayDayRule() throws Exception {
        cityCompany("城市项目公司丙");
        String second = cityCompany("城市项目公司丁");
        String combo = sell("a", comboType, 8000L);
        verify(code(combo, entryTpl), barStaff, bar, barDevice);
        verify(code(combo, diningTpl), staff, restaurant, device);
        moveToMidMonth(combo);

        String june = settlement.settleMonth(project, MONTH);
        assertThat(formatShare(june, "catering").get("beneficiary_entity_id")).as("两家有效城市项目公司，归属留空").isNull();
        postBill(june).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SETTLEMENT_DOMAIN_ENTITY_MISSING"));
        assertThat(postings(june)).as("一笔不挂（含酒吧与餐厅应付）").isZero();
        assertThat(billStatus(june)).isEqualTo("confirmed");

        removeCompany(second);
        settlement.settleMonth(project, MONTH);
        List<String> rules = jdbc.queryForList("SELECT id FROM internal_pay_day_rules WHERE kind = 'route'"
                + " AND route = 'pass_domain_share' AND status = 'approved'", String.class);
        rules.forEach(id -> jdbc.update("UPDATE internal_pay_day_rules SET status = 'pending' WHERE id = ?", id));
        try {
            postBill(june).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail_code").value("PAY_DAY_NOT_CONFIGURED"));
        } finally {
            rules.forEach(id -> jdbc.update("UPDATE internal_pay_day_rules SET status = 'approved' WHERE id = ?", id));
        }
        assertThat(postings(june)).as("新钱路缺付账日规则，整单不过账").isZero();
        assertThat(billStatus(june)).isEqualTo("confirmed");
    }

    private ResultActions postBill(String bill) throws Exception {
        return mockMvc.perform(post("/api/v1/settlement/bills/" + bill + "/post").with(session(admin, "platform_admin", "org_1", null)));
    }

    /** 某来源 id 下的往来，逐笔「方向 来源 对方 金额」。 */
    private List<String> arAp(String sourceId) {
        return jdbc.queryForList("SELECT direction || ' ' || source_type || ' ' || counterparty_id || ' ' || amount_cent FROM ar_ap_items"
                + " WHERE source_id = ? ORDER BY 1", String.class, sourceId);
    }

    private long sharePostings(String bill) {
        return jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_id LIKE ? AND source_type LIKE 'pass_%'",
                Long.class, bill + "|%");
    }

    private long postings(String bill) {
        return jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_id LIKE ?", Long.class, "%" + bill + "%");
    }

    private String billStatus(String bill) {
        return jdbc.queryForObject("SELECT status FROM settlement_bills WHERE id = ?", String.class, bill);
    }

    private Map<String, Object> formatShare(String bill, String format) {
        return jdbc.queryForMap("SELECT * FROM settlement_bill_format_shares WHERE bill_id = ? AND business_format = ?", bill, format);
    }

    /** 业态份额行某金额列；本单无该业态行记 0。 */
    private long col(String bill, String format, String column) {
        return jdbc.queryForObject("SELECT COALESCE(MAX(" + column + "), 0) FROM settlement_bill_format_shares"
                + " WHERE bill_id = ? AND business_format = ?", Long.class, bill, format);
    }

    private void moveToMidMonth(String order) {
        AppendOnlyTestBypass.update(jdbc, "UPDATE settlement_transactions SET business_at = ? WHERE order_id = ?",
                Timestamp.from(MID_MONTH), order);
    }

    private void merchant(String id, String name, String type) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'active', ?, now(), now())", id, name + n, type, project);
    }

    private String cityCompany(String name) {
        String id = "be_c214_" + suffix + "_" + companies.size();
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'project_company', 'active', now(), now())", id, name + n);
        jdbc.update("INSERT INTO business_entity_projects (business_entity_id, project_id) VALUES (?, ?)", id, project);
        companies.add(id);
        return id;
    }

    private void removeCompany(String id) {
        jdbc.update("DELETE FROM business_entity_projects WHERE business_entity_id = ?", id);
        jdbc.update("DELETE FROM business_entities WHERE id = ?", id);
    }

    /** 美团售出：付款确认内核固化通路费与销售渠道费配置，付足即自动出票（同在线到账确认）。 */
    private String sell(String tag, String type, long amount) {
        String order = "ord_c214_" + tag + "_" + suffix;
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
        orderService.confirmPayment(order, new ConfirmTicketOrderPaymentRequest("c214-pay-" + order, pay, "gw-" + order));
        orderService.issuePaidOrderAutomatically(order);
        TestSecurityActors.clear();
        return order;
    }

    /** 游乐入场核销走结算消费者真路径（同 ExclusiveDirectPayDomainShareJdbcIntegrationTests 176-R2 用例；游乐闸机入口属游乐域）。 */
    private void rideVerified(String order, String templateId) {
        Map<String, Object> ride = jdbc.queryForMap("SELECT e.id, e.allocated_revenue_cent, e.parent_ticket_id, o.ticket_type_id"
                + " FROM issued_entitlements e JOIN ticket_orders o ON o.id = e.order_id"
                + " WHERE e.order_id = ? AND e.entitlement_template_id = ?", order, templateId);
        Instant at = Instant.now();
        settledConsumer.onSettlementSettled(new SettlementSettledEvent("vf_c214amu_" + order, at, (String) ride.get("id"), order,
                (String) ride.get("ticket_type_id"), "amusement", park, 1, 1, 1, num(ride, "allocated_revenue_cent"),
                at.plusSeconds(86_400), null, null, null, project, (String) ride.get("parent_ticket_id")));
    }

    /** 按 TicketingPoliciesPage 组合通票行的请求形状建权益模板。 */
    private String template(String name, String type, String merchant, String catalog, String exclusive) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c214-tpl-%s", "name": "%s", "entitlement_type": "%s",
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

    private void bind(String type, String templateId, long componentPriceCent, String costBearer) throws Exception {
        mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + type + "/entitlements").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c214-bind-%s", "entitlement_template_id": "%s", "quantity": 1,
                                 "total_times": 1, "num_people": 1, "component_price_cent": %d%s}
                                """.formatted(UUID.randomUUID(), templateId, componentPriceCent,
                                costBearer == null ? "" : ", \"cost_bearer\": \"" + costBearer + "\"")))
                .andExpect(status().isOk());
    }

    /** 商户员工在本店核销设备上核销，返回核销记录 id。 */
    private String verify(String code, String who, String merchant, String dev) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/verifications").with(session(who, "staff", project, merchant))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c214-verify-%s", "credential_type": "qr", "credential_payload": "%s",
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
        for (String permission : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, permission);
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
