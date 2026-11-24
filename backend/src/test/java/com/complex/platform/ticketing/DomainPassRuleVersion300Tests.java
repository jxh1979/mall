package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.support.PassPoolFixtures;
import com.complex.platform.support.TicketSalesContractTestFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 BP 段第 300 条 300-R1（第十八批裁决 74「要审批也留版本」：与跨域组合通票同一做法，审批人换成该域主管公司的人，提交人不能自批；改动先存草稿版，
 * 审批期间按旧版照卖，批准后自动换新版；最高权限本人提交即生效；订单记版本）：未挂酒吧的域内票（酒吧通票、纯游乐通票）也走票种规则版本。
 *
 * <p>依据：BSM:1673「影响结算的规则由各自责任主体在系统中配置、审批和版本化：域内规则由对应域主管公司负责」；ENUM:32「酒吧通票由娱乐平台公司负责；
 * 纯游乐票和纯游乐通票由游乐公司负责」；BSM:756「订单保存基础价、动态价、全部命中资格价、最终选中价和规则版本」；BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，
 * 不需要经过任何审批」；PGM:528「发起单据时只需指定该事项的最终审批人」。单一酒吧门票照现行（第十八批裁决 61 题面「单一酒吧门票仍由酒吧自己定价，不走这道审批」）。
 *
 * <p>真实接口与授权链（蛇形键、后台 Web 终端）：娱乐平台公司、游乐公司各一名提交人（账号机构为该公司）与其直接上级审批人（持票种规则审批权限）；
 * 收入归属用 V355 平台默认。不开测试事务，@AfterEach 按票种清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class DomainPassRuleVersion300Tests {

    private static final String ENT = "be_ent_platform";
    private static final String AMUSEMENT = "be_amusement";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "prj_c300_" + s;
    private final String barA = "bar_c300a_" + s;
    private final String barB = "bar_c300b_" + s;
    private final String park = "amu_c300_" + s;
    private final String restaurant = "rst_c300_" + s;
    private final String entSubmitter = "usr_c300_es_" + s;
    private final String entApprover = "usr_c300_ea_" + s;
    private final String amuSubmitter = "usr_c300_as_" + s;
    private final String amuApprover = "usr_c300_aa_" + s;
    /** 平台管理员：下单、建与发布单一酒吧门票。 */
    private final String admin = "usr_c300_ad_" + s;
    private final List<String> ticketTypes = new ArrayList<>();
    private final List<String> orders = new ArrayList<>();
    private final List<String> contractTypes = new ArrayList<>();
    private int seq;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', now(), now())", project, "域内票规则版本综合体" + s);
        merchant(barA, "域内票规则版本酒吧甲", "bar");
        merchant(barB, "域内票规则版本酒吧乙", "bar");
        for (String bar : List.of(barA, barB)) {
            // 赠酒商品引用已获批生效的内部结算价目录条目（236-R2 发布闸）。
            jdbc.update("INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent, status, is_draft,"
                    + " updated_by, created_at, updated_at) VALUES (?, ?, ?, 'beverage', '赠酒一杯', 2000, 'active', FALSE, 'seed_c300', now(), now())",
                    "sku_" + bar, "sku_" + bar, bar);
            jdbc.update("INSERT INTO bar_products (id, bar_id, product_name, product_type, price_cent, currency, inventory_item_id,"
                    + " stock_location_id, stock_deduction_required, status, internal_settlement_price_cent, catalog_item_code, created_at, updated_at)"
                    + " VALUES (?, ?, '赠酒一杯', 'beverage', 3800, 'CNY', NULL, 'default', FALSE, 'active', 2000, ?, now(), now())",
                    "bp_" + bar, bar, "sku_" + bar);
        }
        merchant(park, "域内票规则版本乐园", "amusement");
        merchant(restaurant, "域内票规则版本餐厅", "catering");

        List<String> submit = List.of("ticket.product.manage", "ticket.product.view", "ticket.entitlement.manage",
                "ticket.price_variant.manage", "governance.supervisor_review.view", "governance.supervisor_review.approve");
        List<String> approve = List.of("ticket.product.publish.approve", "governance.supervisor_review.view",
                "governance.supervisor_review.approve");
        String submitRole = role("域内票规则提交" + s, "platform", false, submit);
        String approveRole = role("域内票规则审批" + s, "platform", true, approve);
        user(entSubmitter, "staff", ENT, submitRole);
        user(entApprover, "staff", ENT, approveRole);
        user(amuSubmitter, "staff", AMUSEMENT, submitRole);
        user(amuApprover, "staff", AMUSEMENT, approveRole);
        user(admin, "platform_admin", "org_1", role("域内票规则版本平台管理" + s, "platform", false, List.of("ticket.product.manage",
                "ticket.product.view", "ticket.entitlement.manage", "ticket.order.create")));
        jdbc.update("INSERT INTO staff_level_assignments (user_id, staff_level_id, organization_id) VALUES (?, 'lvl_m3_upper', ?)",
                entApprover, ENT);
        jdbc.update("INSERT INTO staff_level_assignments (user_id, staff_level_id, organization_id) VALUES (?, 'lvl_m3_upper', ?)",
                amuApprover, AMUSEMENT);
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", entSubmitter, entApprover);
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", amuSubmitter, amuApprover);
    }

    @AfterEach
    void cleanup() {
        PassPoolFixtures.deleteByProject(jdbc, project);
        jdbc.update("DELETE FROM comprehensive_service_fee_configs WHERE project_id = ?", project);
        for (String order : orders) {
            AppendOnlyTestBypass.update(jdbc, "DELETE FROM settlement_transactions WHERE order_id = ?", order);
            jdbc.update("DELETE FROM ticket_credentials WHERE entitlement_id IN (SELECT id FROM issued_entitlements WHERE order_id = ?)", order);
            jdbc.update("DELETE FROM issued_entitlements WHERE order_id = ?", order);
            jdbc.update("DELETE FROM ticket_slot_holders WHERE parent_ticket_id IN (SELECT id FROM ticket_instances WHERE order_id = ?)", order);
            jdbc.update("DELETE FROM ticket_instances WHERE order_id = ?", order);
            AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_order_sales_contracts", "ticket_sales_snapshot_immutable",
                    "DELETE FROM ticket_order_sales_contracts WHERE order_id = ?", order);
            jdbc.update("DELETE FROM ticket_orders WHERE id = ?", order);
        }
        for (String type : contractTypes) {
            AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_reward_config_versions", "ticket_reward_config_immutable",
                    "DELETE FROM ticket_reward_config_versions WHERE ticket_type_id = ?", type);
        }
        for (String type : ticketTypes) {
            jdbc.update("DELETE FROM ticket_price_rules WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_rule_versions WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_gift_floors WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_types WHERE id = ?", type);
        }
        jdbc.update("DELETE FROM entitlement_templates WHERE merchant_id IN (?, ?, ?, ?)", barA, barB, park, restaurant);
        for (String bar : List.of(barA, barB)) {
            jdbc.update("DELETE FROM bar_products WHERE id = ?", "bp_" + bar);
            jdbc.update("DELETE FROM internal_price_catalog WHERE item_code = ?", "sku_" + bar);
        }
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
    }

    /**
     * ①②③：酒吧通票（两家酒吧有价入场，各配平台出资赠酒）由娱乐平台公司提交人发布：不带最终审批人 400、审批人是游乐公司的人 400，两次都不留版本、票仍为草稿；
     * 填娱乐平台公司的审批人即提交审批、票仍为草稿、第 1 版待审；提交人自己批 403；审批人批准后上架、第 1 版生效。
     */
    @Test
    void barPassFirstPublishGoesToTheEntertainmentPlatformCompanyApprover() throws Exception {
        String pass = barPass("a");

        publish(entSubmitter, pass, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(ticketStatus(pass)).isEqualTo("draft");
        publish(entSubmitter, pass, amuApprover).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(JsonPath.<Integer>read(versions(pass), "$.data.items.length()")).isZero();

        publish(entSubmitter, pass, entApprover).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        String pending = versions(pass);
        assertThat(JsonPath.<Integer>read(pending, "$.data.items[0].version_no")).isEqualTo(1);
        assertThat(JsonPath.<String>read(pending, "$.data.items[0].status")).isEqualTo("pending_review");
        approve(entSubmitter, reviewOf(pass)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        assertThat(ticketStatus(pass)).isEqualTo("draft");

        approve(entApprover, reviewOf(pass)).andExpect(status().isOk());
        assertThat(ticketStatus(pass)).isEqualTo("published");
        assertThat(JsonPath.<String>read(versions(pass), "$.data.items[0].status")).isEqualTo("effective");
    }

    /**
     * ④⑤（第十八批裁决 61 同一机制）：已上架酒吧通票改价（九折 180 元）只存进第 2 版草稿，现表仍 200 元；此时下单按 200 元、记第 1 版；提交并批准后现表换成 180 元，
     * 新订单记第 2 版。新建会员价也只进草稿版、不进现表。
     */
    @Test
    void publishedBarPassRepriceWaitsForApprovalAndOrdersRecordTheVersionInForce() throws Exception {
        String pass = publishedBarPass("b");
        salesContract(pass);
        String first = effectiveVersion(pass);

        reprice(entSubmitter, pass, 18000L, 90).andExpect(status().isOk());
        assertThat(weekdayPrice(pass)).isEqualTo(20000L);
        String drafted = versions(pass);
        assertThat(JsonPath.<Integer>read(drafted, "$.data.items[0].version_no")).isEqualTo(2);
        assertThat(JsonPath.<String>read(drafted, "$.data.items[0].status")).isEqualTo("draft");
        String a = order(pass, 20000);
        assertThat(orderVersion(a)).isEqualTo(first);

        String member = JsonPath.read(send(entSubmitter, post("/api/v1/ticketing/price-rules"), """
                {"idempotency_key": "c300-pr-%s", "ticket_type_id": "%s", "variant_type": "member", "price_mode": "derive_from_calendar_price",
                 "discount_rate": 90}
                """.formatted(UUID.randomUUID(), pass)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_price_rules WHERE id = ?", Integer.class, member)).isZero();

        publish(entSubmitter, pass, entApprover).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        approve(entApprover, reviewOf(pass)).andExpect(status().isOk());
        assertThat(weekdayPrice(pass)).isEqualTo(18000L);
        String second = effectiveVersion(pass);
        assertThat(second).isNotEqualTo(first);
        String b = order(pass, 18000);
        assertThat(orderVersion(b)).isEqualTo(second);
        assertThat(orderVersion(a)).isEqualTo(first);
    }

    /** ⑥：纯游乐通票由游乐公司的人审批——最终审批人填娱乐平台公司的人 400，填游乐公司的人即提交审批。 */
    @Test
    void parkPassIsApprovedByTheAmusementCompany() throws Exception {
        String pass = createTicketType(amuSubmitter, "c", 8000L, null);
        bind(amuSubmitter, pass, template(amuSubmitter, park, "游乐入场", "entry", null), 8000L, null).andExpect(status().isOk());

        publish(amuSubmitter, pass, entApprover).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(JsonPath.<Integer>read(versions(pass), "$.data.items.length()")).isZero();
        publish(amuSubmitter, pass, amuApprover).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        assertThat(JsonPath.<String>read(versions(pass), "$.data.items[0].status")).isEqualTo("pending_review");
    }

    /** ⑦⑧：最高权限本人发布酒吧通票即上架、第 1 版生效、没有复核单；单一酒吧门票照现行直接上架、不建版本。 */
    @Test
    void masterPublishesADomainPassAtOnceAndSingleBarTicketPublishesDirectly() throws Exception {
        String pass = barPass("d");
        mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + pass + "/publish").with(master())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c300-mpub-%s", "reason": "本人核对后上架"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        String versions = versions(pass);
        assertThat(JsonPath.<Integer>read(versions, "$.data.items.length()")).isEqualTo(1);
        assertThat(JsonPath.<String>read(versions, "$.data.items[0].status")).isEqualTo("effective");
        assertThat(JsonPath.<Object>read(versions, "$.data.items[0].review_id")).isNull();

        String single = createTicketType(admin, "e", 5000L, barA);
        bind(admin, single, template(admin, barA, "单店入场", "entry", null), 5000L, null).andExpect(status().isOk());
        jdbc.update("INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, fee_bps, status, is_draft)"
                + " VALUES (?, ?, ?, 500, 'active', FALSE)", "csfc_c300_" + s, project, barA);
        publish(admin, single, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        assertThat(JsonPath.<Integer>read(versions(single), "$.data.items.length()")).isZero();
    }

    /**
     * BSM:749「零元免费权益不参与域间分配」：酒吧通票另配餐饮商户提供、平台出资的零元餐饮小食，仍是娱乐平台公司负责的域内票——最终审批人填游乐公司的人
     * 400，填娱乐平台公司的人即提交审批（票仍为草稿、第 1 版待审）。
     */
    @Test
    void barPassWithZeroPricedSnackIsStillApprovedByTheEntertainmentPlatformCompany() throws Exception {
        String pass = barPassWithSnack("f");

        publish(entSubmitter, pass, amuApprover).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(containsString("最终审批人须是该公司的人员")));
        publish(entSubmitter, pass, entApprover).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        assertThat(JsonPath.<String>read(versions(pass), "$.data.items[0].status")).isEqualTo("pending_review");
    }

    /** 同样组成的酒吧通票由最高权限本人发布上架后，游乐公司的人改价 403 TICKET_TYPE_SCOPE_FORBIDDEN（ENUM:32「酒吧通票由娱乐平台公司负责」）。 */
    @Test
    void amusementStaffCannotRepriceABarPassWithZeroPricedSnack() throws Exception {
        String pass = barPassWithSnack("g");
        mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + pass + "/publish").with(master())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c300-mpub-%s", "reason": "本人核对后上架"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));

        reprice(amuSubmitter, pass, 18000L, 90).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_SCOPE_FORBIDDEN"));
    }

    // ─────────────────────────── 夹具 ───────────────────────────

    /** 酒吧通票另配餐饮商户提供、平台出资的零元餐饮小食。 */
    private String barPassWithSnack(String tag) throws Exception {
        String pass = barPass(tag);
        bind(entSubmitter, pass, template(entSubmitter, restaurant, "餐饮小食", "dining", null), 0L, "platform_funded")
                .andExpect(status().isOk());
        return pass;
    }

    /** 首次发布经娱乐平台公司审批上架的酒吧通票（第 1 版生效）。 */
    private String publishedBarPass(String tag) throws Exception {
        String pass = barPass(tag);
        publish(entSubmitter, pass, entApprover).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        approve(entApprover, reviewOf(pass)).andExpect(status().isOk());
        return pass;
    }

    /** 娱乐平台公司提交人按组合配置写入顺序起草：两家酒吧各有价入场 100 元、各配零元平台出资赠酒 1 杯；不打折、平日价 200 元。 */
    private String barPass(String tag) throws Exception {
        String pass = createTicketType(entSubmitter, tag, 20000L, null);
        for (String bar : List.of(barA, barB)) {
            bind(entSubmitter, pass, template(entSubmitter, bar, "酒吧入场", "entry", null), 10000L, null).andExpect(status().isOk());
            bind(entSubmitter, pass, template(entSubmitter, bar, "平台赠酒", "gift_drink", "bp_" + bar), 0L, "platform_funded")
                    .andExpect(status().isOk());
        }
        return pass;
    }

    private String createTicketType(String who, String tag, long priceCent, String barId) throws Exception {
        String body = send(who, post("/api/v1/ticketing/ticket-types"), """
                {"idempotency_key": "c300-%s-%s", "ticket_name": "域内票规则版本票%s%s", "base_calendar_price_mode": "weekday_weekend_holiday",
                 "weekday_price_cent": %d, "weekend_price_cent": %d, "holiday_price_cent": %d, "stock_required": false,
                 "combo_discount_rate": 100, "bar_id": %s}
                """.formatted(tag, s, tag, s, priceCent, priceCent, priceCent, barId == null ? "null" : "\"" + barId + "\""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(body, "$.data.id");
        ticketTypes.add(id);
        return id;
    }

    private String template(String who, String merchant, String name, String type, String barItemRef) throws Exception {
        String body = send(who, post("/api/v1/ticketing/entitlement-templates"), """
                {"idempotency_key": "c300-tpl-%s", "name": "域内票规则版本 · %s%d%s", "entitlement_type": "%s",
                 "default_total_times": 1, "default_value_cent": 0, "status": "active", "merchant_id": "%s",
                 "catalog_item_code": null, "exclusive_merchant_id": null, "bar_item_ref": %s}
                """.formatted(UUID.randomUUID(), name, ++seq, s, type, merchant, barItemRef == null ? "null" : "\"" + barItemRef + "\""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.id");
    }

    private ResultActions bind(String who, String type, String template, long componentPriceCent, String costBearer) throws Exception {
        return send(who, post("/api/v1/ticketing/ticket-types/" + type + "/entitlements"), """
                {"idempotency_key": "c300-bind-%s", "entitlement_template_id": "%s", "quantity": 1,
                 "total_times": 1, "num_people": 1, "component_price_cent": %d, "cost_bearer": %s}
                """.formatted(UUID.randomUUID(), template, componentPriceCent, costBearer == null ? "null" : "\"" + costBearer + "\""));
    }

    private ResultActions publish(String who, String type, String finalApprover) throws Exception {
        return send(who, post("/api/v1/ticketing/ticket-types/" + type + "/publish"), """
                {"idempotency_key": "c300-pub-%s", "reason": "通票配好上架"%s}
                """.formatted(UUID.randomUUID(), finalApprover == null ? "" : ", \"final_approver_id\": \"" + finalApprover + "\""));
    }

    private ResultActions reprice(String who, String type, long priceCent, int discount) throws Exception {
        return send(who, put("/api/v1/ticketing/ticket-types/" + type + "/pricing"), """
                {"idempotency_key": "c300-price-%s", "reason": "调价", "combo_discount_rate": %d, "weekday_price_cent": %d,
                 "weekend_price_cent": %d, "holiday_price_cent": %d}
                """.formatted(UUID.randomUUID(), discount, priceCent, priceCent, priceCent));
    }

    /** 平台管理员在后台下单（官方后台渠道），断言下单金额，返回订单号。 */
    private String order(String type, int amountCent) throws Exception {
        String body = send(admin, post("/api/v1/ticketing/orders"), """
                {"idempotency_key": "c300-order-%s", "ticket_type_id": "%s", "quantity": 1, "sales_channel": "admin"}
                """.formatted(UUID.randomUUID(), type))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(body, "$.data.id");
        orders.add(id);
        assertThat(JsonPath.<Integer>read(body, "$.data.amount_cent")).isEqualTo(amountCent);
        return id;
    }

    /** 售票链既有前置：该票种已批准、已公告、已生效的销售合同（下单时锁定）。 */
    private void salesContract(String type) {
        TicketSalesContractTestFixtures.approvedPublished(jdbc, new ObjectMapper(), type, project, barA);
        contractTypes.add(type);
    }

    private String orderVersion(String order) {
        return jdbc.queryForObject("SELECT ticket_type_rule_version_id FROM ticket_orders WHERE id = ?", String.class, order);
    }

    private String effectiveVersion(String type) {
        return jdbc.queryForObject("SELECT id FROM ticket_type_rule_versions WHERE ticket_type_id = ? AND status = 'effective'",
                String.class, type);
    }

    private String versions(String type) throws Exception {
        return mockMvc.perform(get("/api/v1/ticketing/ticket-types/" + type + "/rule-versions").with(session(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private ResultActions approve(String who, String review) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(session(who))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"approved\",\"comment\":\"已核对\",\"idempotency_key\":\"c300-ap-" + UUID.randomUUID() + "\"}"));
    }

    private String reviewOf(String type) {
        return jdbc.queryForObject("SELECT review_id FROM ticket_type_rule_versions WHERE ticket_type_id = ? AND status = 'pending_review'",
                String.class, type);
    }

    private String ticketStatus(String type) {
        return jdbc.queryForObject("SELECT status FROM ticket_types WHERE id = ?", String.class, type);
    }

    private Long weekdayPrice(String type) {
        return jdbc.queryForObject("SELECT weekday_price_cent FROM ticket_types WHERE id = ?", Long.class, type);
    }

    private ResultActions send(String who, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(session(who)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void merchant(String id, String merchantName, String type) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'active', ?, now(), now())", id, merchantName + s, type, project);
    }

    private String role(String name, String dataScope, boolean allProjects, List<String> permissions) {
        String roleId = "role_c300_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, view_all_projects, created_at)"
                + " VALUES (?, ?, '域内票规则版本回归', ?, 'active', ?, now())", roleId, name, dataScope, allProjects);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        return roleId;
    }

    private void user(String userId, String accountType, String organization, String roleId) {
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, ?, ?, NULL, 'active', now())", userId, userId, accountType, organization);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与账号表当前行一致）。
    private RequestPostProcessor session(String userId) {
        java.util.Map<String, Object> row = jdbc.queryForMap("SELECT account_type, organization_id FROM access_users WHERE id = ?", userId);
        AccessActor actor = new AccessActor(userId, userId, (String) row.get("account_type"), (String) row.get("organization_id"), null,
                "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private static RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
