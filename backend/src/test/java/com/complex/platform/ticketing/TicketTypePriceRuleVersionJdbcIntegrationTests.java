package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
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
 * 第十八批 AS 段第 297 条 297-R1～297-R3（第十八批裁决 63「跨域组合通票的派生价、动态价规则也要经项目之上的平台权限审批，并记入票种规则版本。」）：
 * 票种规则版本记下派生价（会员、新人、学生、团体、渠道，含已存档）与已发布动态价两份清单，随提交审批；已上架跨域组合通票新建派生价、发布动态价
 * 照裁决 61「改动另存一份草稿版审批，审批期间照旧版卖，获批后自动换成新版」进草稿版，待审期间 409，获批写入生效。
 *
 * <p>依据：ENUM:32「跨域组合通票的定价与域间结算规则由项目之上的平台权限统一审批」；BSM:782「会员价、渠道价、动态价都在对应基础价上按打折/减免/加价/
 * 固定额派生；动态价按时段、容量等规则配置，可先试算再发布。」；BSM:569「逐条存档属日常操作，不用审批」；BSM:521「已发生的业务一律按当时生效的版本计算」。
 *
 * <p>297-R3：BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」（第十八批裁决 71）——最高权限本人提交票种规则版本即生效。
 *
 * <p>真实接口与授权链：票务运营按后台同键（蛇形键）建票、建模板、绑定、配派生价与动态价、发布；审批人经主管复核接口审批。不开测试事务，@AfterEach 按票种清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class TicketTypePriceRuleVersionJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final long n = Long.parseLong(suffix, 16);
    private final String project = "prj_p297_" + suffix;
    private final String bar = "bar_p297_" + suffix;
    private final String park = "amu_p297_" + suffix;
    private final String product = "bp_p297_" + suffix;
    private final String catalogCode = "sku_p297_" + suffix;
    /** 票务运营（提交人）：配票种、派生价、动态价并提交。 */
    private final String operator = "usr_p297_op_" + suffix;
    /** 提交人的直接上级：持跨域组合通票票种规则审批权限、能看全部项目。 */
    private final String approver = "usr_p297_ap_" + suffix;
    private final List<String> ticketTypes = new ArrayList<>();
    private final List<String> templates = new ArrayList<>();
    private final List<String> orders = new ArrayList<>();
    private final List<String> contractTypes = new ArrayList<>();

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '派生价版本综合体', 'active', now(), now())", project);
        merchant(bar, "派生价版本酒吧", "bar");
        merchant(park, "派生价版本乐园", "amusement");
        jdbc.update("INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent, status, is_draft,"
                + " updated_by, created_at, updated_at) VALUES (?, ?, ?, 'beverage', '赠酒一杯', 2000, 'active', FALSE, 'seed_p297', now(), now())",
                catalogCode, catalogCode, bar);
        jdbc.update("INSERT INTO bar_products (id, bar_id, product_name, product_type, price_cent, currency, inventory_item_id,"
                + " stock_location_id, stock_deduction_required, status, internal_settlement_price_cent, catalog_item_code, created_at, updated_at)"
                + " VALUES (?, ?, '赠酒一杯', 'beverage', 3800, 'CNY', NULL, 'default', FALSE, 'active', 2000, ?, now(), now())",
                product, bar, catalogCode);

        String operatorRole = role("派生价版本运营" + n, "platform", false, List.of("ticket.product.manage", "ticket.product.view",
                "ticket.entitlement.manage", "ticket.order.create", "ticket.price_variant.manage", "ticket.price_variant.view",
                "ticket.dynamic_pricing.manage", "ticket.dynamic_pricing.publish", "ticket.dynamic_pricing.simulate",
                "governance.supervisor_review.view", "governance.supervisor_review.approve"));
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", operator, operator);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", operator, operatorRole);

        String allProjects = role("派生价版本审批" + n, "platform", true, List.of("ticket.product.publish.approve",
                "governance.supervisor_review.view", "governance.supervisor_review.approve"));
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', ?, NULL, 'active', now())", approver, approver, project);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", approver, allProjects);
        jdbc.update("INSERT INTO staff_level_assignments (user_id, staff_level_id, organization_id) VALUES (?, 'lvl_m3_upper', ?)",
                approver, project);
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", operator, approver);
    }

    @AfterEach
    void cleanup() {
        PassPoolFixtures.deleteByProject(jdbc, project);
        jdbc.update("DELETE FROM comprehensive_service_fee_configs WHERE project_id = ?", project);
        for (String order : orders) {
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
            jdbc.update("DELETE FROM ticket_dynamic_pricing_rules WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_rule_versions WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_gift_floors WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_types WHERE id = ?", type);
        }
        for (String template : templates) {
            jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", template);
        }
        jdbc.update("DELETE FROM bar_products WHERE id = ?", product);
        jdbc.update("DELETE FROM internal_price_catalog WHERE item_code = ?", catalogCode);
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
    }

    /**
     * 297-R1：未上架跨域组合通票照现行直配会员价（九折）、学生价并存档学生价、发布后台渠道加 10 元动态价；首次发布提交审批，第 1 版记下两条派生价（含已存档）与
     * 该动态价，复核单摘要写明派生价与动态价，获批后第 1 版生效、两份清单不变；上架后存档会员价即时生效、不另起版本（BSM:569）。
     */
    @Test
    void firstPublishRecordsPriceRulesAndDynamicRulesInRuleVersion() throws Exception {
        String pass = comboPass("a");
        String member = priceRule(pass, "member", 90).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String memberId = JsonPath.read(member, "$.data.id");
        String studentId = JsonPath.read(priceRule(pass, "student", 80).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        archive(studentId).andExpect(status().isOk());
        String dynamic = dynamicRule(pass, "节假日加价");
        publishDynamic(dynamic).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));

        publish(pass).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));

        String versions = versions(pass);
        assertThat(JsonPath.<Map<String, Object>>read(versions, "$.data.items[0]")).containsKeys("price_rules", "dynamic_pricing_rules");
        assertThat(JsonPath.<List<String>>read(versions, "$.data.items[0].price_rules[*].id")).containsExactlyInAnyOrder(memberId, studentId);
        assertThat(JsonPath.<List<Object>>read(versions, "$.data.items[0].price_rules[?(@.variant_type == 'member')].discount_rate"))
                .singleElement().satisfies(rate -> assertThat(((Number) rate).intValue()).isEqualTo(90));
        assertThat(JsonPath.<List<String>>read(versions, "$.data.items[0].dynamic_pricing_rules[*].id")).containsExactly(dynamic);
        assertThat(JsonPath.<List<Integer>>read(versions, "$.data.items[0].dynamic_pricing_rules[*].adjustment_amount_cent"))
                .containsExactly(1000);
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + reviewOf(pass)).with(session(approver, "staff", project)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reason").value(Matchers.containsString("派生价：会员价 · 按基础价 90%")))
                .andExpect(jsonPath("$.data.reason").value(Matchers.containsString("动态价：节假日加价")));

        approve(reviewOf(pass)).andExpect(status().isOk());

        String decided = versions(pass);
        assertThat(JsonPath.<String>read(decided, "$.data.items[0].status")).isEqualTo("effective");
        assertThat(JsonPath.<List<String>>read(decided, "$.data.items[0].price_rules[*].id")).containsExactlyInAnyOrder(memberId, studentId);
        assertThat(JsonPath.<List<String>>read(decided, "$.data.items[0].dynamic_pricing_rules[*].id")).containsExactly(dynamic);
        assertThat(ticketStatus(pass)).isEqualTo("published");

        archive(memberId).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT archived_at IS NOT NULL FROM ticket_price_rules WHERE id = ?", Boolean.class, memberId)).isTrue();
        assertThat(JsonPath.<Integer>read(versions(pass), "$.data.items.length()")).isEqualTo(1);
    }

    /**
     * 第十八批 AW 段 338-R1（BSM:782「会员价、渠道价、动态价都在对应基础价上按打折/减免/加价/固定额派生」）：已上架跨域组合通票建「按基础价减 10 元」会员价，
     * 存进第 2 版草稿并带加减金额，复核单摘要写明；获批后现表在售、带加减金额，带会员价下单按 199－10＝189 元。
     */
    @Test
    void publishedComboPassAmountDeltaPriceRuleGoesIntoDraftVersionAndKeepsAmountOnApproval() throws Exception {
        String pass = publishedComboPass("w");
        salesContract(pass);
        String member = JsonPath.read(send(post("/api/v1/ticketing/price-rules"), """
                {"idempotency_key": "c338-pr-%s", "ticket_type_id": "%s", "variant_type": "member", "price_mode": "amount_delta",
                 "adjustment_amount_cent": -1000}
                """.formatted(UUID.randomUUID(), pass)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(JsonPath.<List<Integer>>read(versions(pass), "$.data.items[0].price_rules[*].adjustment_amount_cent"))
                .containsExactly(-1000);

        publish(pass).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + reviewOf(pass)).with(session(approver, "staff", project)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reason").value(Matchers.containsString("派生价：会员价 · 按基础价减 10.00 元")));
        approve(reviewOf(pass)).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT adjustment_amount_cent FROM ticket_price_rules WHERE id = ?", Long.class, member))
                .isEqualTo(-1000L);
        order(pass, member, 18900);
    }

    /** 297-R1 不越界：酒吧通票与单一酒吧门票不是跨域组合通票，上架后配派生价、发布动态价照现行直接生效，不建规则版本。 */
    @Test
    void sameDomainPassAndSingleBarTicketPriceRulesTakeEffectDirectly() throws Exception {
        String barPass = createTicketType("b1", 10000L, 100, null);
        bind(barPass, template(bar, "酒吧入场", "entry", null), 10000L, null).andExpect(status().isOk());
        bind(barPass, template(bar, "平台赠酒", "gift_drink", product), 0L, "platform_funded").andExpect(status().isOk());
        publish(barPass).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        priceRule(barPass, "member", 90).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("active"));
        publishDynamic(dynamicRule(barPass, "酒吧通票加价")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        assertThat(JsonPath.<Integer>read(versions(barPass), "$.data.items.length()")).isZero();

        String single = createTicketType("b2", 5000L, 100, bar);
        bind(single, template(bar, "单店入场", "entry", null), 5000L, null).andExpect(status().isOk());
        // 第十八批 AW 段 294-R1（BSM:755）：单店票发布前本项目本酒吧须有已转正的综合服务费。
        jdbc.update("INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, fee_bps, status, is_draft)"
                + " VALUES (?, ?, ?, 500, 'active', FALSE)", "csfc_" + suffix, project, bar);
        publish(single).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        priceRule(single, "student", 80).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("active"));
        assertThat(JsonPath.<Integer>read(versions(single), "$.data.items.length()")).isZero();
    }

    /**
     * 297-R2：已上架跨域组合通票（第 1 版生效）新建会员价九折、发布后台渠道加 10 元动态价，都只存进第 2 版草稿：派生价不进现表、动态价表内仍是草稿，
     * 试算可见（BSM:782「可先试算再发布」），下单仍按第 1 版 199 元且不带动态价，带草稿会员价下单 404；提交审批后待审期间再配派生价、再发布别的动态价 409；
     * 获批后派生价入表在售、动态价已发布，第 1 版已被取代且两份清单仍为空；之后下单记第 2 版、按 209 元，带会员价 189.10 元；先前订单金额与版本不变。
     */
    @Test
    void publishedComboPassPriceRuleAndDynamicPublishGoIntoDraftVersionAndTakeEffectOnApproval() throws Exception {
        String pass = publishedComboPass("c");
        salesContract(pass);
        String first = effectiveVersion(pass);

        String key = "p297-pr-" + UUID.randomUUID();
        String member = JsonPath.read(priceRule(pass, "member", 90, key).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_price_rules WHERE id = ?", Integer.class, member)).isZero();
        String drafted = versions(pass);
        assertThat(JsonPath.<Integer>read(drafted, "$.data.items[0].version_no")).isEqualTo(2);
        assertThat(JsonPath.<String>read(drafted, "$.data.items[0].status")).isEqualTo("draft");
        assertThat(JsonPath.<List<String>>read(drafted, "$.data.items[0].price_rules[*].id")).containsExactly(member);
        String draftId = JsonPath.read(drafted, "$.data.items[0].id");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'ticket_price_variant.changed'"
                + " AND event_result = 'success' AND idempotency_key = ? AND metadata->>'rule_version_id' = ?"
                + " AND metadata->>'rule_version_status' = 'draft'", Integer.class, key, draftId)).isEqualTo(1);

        String dynamic = dynamicRule(pass, "旺季加价");
        String simulated = send(post("/api/v1/ticketing/dynamic-pricing/simulations"), """
                {"idempotency_key": "p297-sim-%s", "ticket_type_id": "%s", "sales_channel": "admin"}
                """.formatted(UUID.randomUUID(), pass)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<String>read(simulated, "$.data.matched_rule_id")).isEqualTo(dynamic);
        assertThat(JsonPath.<Integer>read(simulated, "$.data.final_price_cent")).isEqualTo(20900);
        String publishKey = "p297-dyn-" + UUID.randomUUID();
        publishDynamic(dynamic, publishKey).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        assertThat(dynamicStatus(dynamic)).isEqualTo("draft");
        String staged = versions(pass);
        assertThat(JsonPath.<Integer>read(staged, "$.data.items.length()")).isEqualTo(2);
        assertThat(JsonPath.<List<String>>read(staged, "$.data.items[0].dynamic_pricing_rules[*].id")).containsExactly(dynamic);
        assertThat(JsonPath.<List<String>>read(staged, "$.data.items[0].price_rules[*].id")).containsExactly(member);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'ticket_dynamic_pricing.published.success'"
                + " AND event_result = 'success' AND idempotency_key = ? AND metadata->>'rule_version_status' = 'draft'",
                Integer.class, publishKey)).isEqualTo(1);

        String a = order(pass, null, 19900);
        assertThat(jdbc.queryForObject("SELECT dynamic_pricing_rule_id FROM ticket_orders WHERE id = ?", String.class, a)).isNull();
        assertThat(orderVersion(a)).isEqualTo(first);
        orderRequest(pass, member).andExpect(status().isNotFound()).andExpect(jsonPath("$.detail_code").value("PRICE_RULE_NOT_FOUND"));

        publish(pass).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        assertThat(JsonPath.<String>read(versions(pass), "$.data.items[0].status")).isEqualTo("pending_review");
        priceRule(pass, "student", 80).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_RULE_VERSION_PENDING"));
        publishDynamic(dynamicRule(pass, "另一条加价")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_RULE_VERSION_PENDING"));

        approve(reviewOf(pass)).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT status FROM ticket_price_rules WHERE id = ?", String.class, member)).isEqualTo("active");
        assertThat(dynamicStatus(dynamic)).isEqualTo("published");
        String swapped = versions(pass);
        assertThat(JsonPath.<List<String>>read(swapped, "$.data.items[?(@.version_no == 2)].status")).containsExactly("effective");
        assertThat(JsonPath.<List<String>>read(swapped, "$.data.items[?(@.version_no == 1)].status")).containsExactly("superseded");
        assertThat(JsonPath.<List<Object>>read(swapped, "$.data.items[?(@.version_no == 1)].price_rules[*]")).isEmpty();
        assertThat(JsonPath.<List<Object>>read(swapped, "$.data.items[?(@.version_no == 1)].dynamic_pricing_rules[*]")).isEmpty();
        String second = effectiveVersion(pass);

        String b = order(pass, null, 20900);
        assertThat(orderVersion(b)).isEqualTo(second);
        order(pass, member, 18910);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ticket_orders WHERE id = ?", Long.class, a)).isEqualTo(19900L);
        assertThat(orderVersion(a)).isEqualTo(first);
    }

    /** 297-R2（裁决 61 撤回）：草稿版里加的学生价随撤回作废、不进现表；草稿版里的动态价撤回后表内仍是草稿，可再发布进新的草稿版。 */
    @Test
    void withdrawnDraftVersionDropsStagedPriceRuleAndDynamicRuleCanBeStagedAgain() throws Exception {
        String pass = publishedComboPass("e");
        String student = JsonPath.read(priceRule(pass, "student", 80).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        String dynamic = dynamicRule(pass, "周末加价");
        publishDynamic(dynamic).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));

        send(post("/api/v1/ticketing/ticket-types/" + pass + "/rule-versions/withdraw"), """
                {"idempotency_key": "p297-wd-%s", "reason": "先不改价"}
                """.formatted(UUID.randomUUID())).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("withdrawn"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_price_rules WHERE id = ?", Integer.class, student)).isZero();
        assertThat(dynamicStatus(dynamic)).isEqualTo("draft");
        publishDynamic(dynamic).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        String again = versions(pass);
        assertThat(JsonPath.<Integer>read(again, "$.data.items[0].version_no")).isEqualTo(3);
        assertThat(JsonPath.<String>read(again, "$.data.items[0].status")).isEqualTo("draft");
        assertThat(JsonPath.<List<String>>read(again, "$.data.items[0].dynamic_pricing_rules[*].id")).containsExactly(dynamic);
        assertThat(JsonPath.<List<Object>>read(again, "$.data.items[0].price_rules[*]")).isEmpty();
    }

    /**
     * 297-R3：非最高权限首次发布跨域组合通票不带最终审批人照旧 400；最高权限本人不带最终审批人发布即上架、第 1 版生效，不建复核单，
     * 留痕「票种规则版本已生效」记本人发起。
     */
    @Test
    void masterFirstPublishOfComboPassTakesEffectWithoutApproval() throws Exception {
        String pass = comboPass("g");
        send(post("/api/v1/ticketing/ticket-types/" + pass + "/publish"), """
                {"idempotency_key": "p297-pub-%s", "reason": "组合通票配好上架"}
                """.formatted(UUID.randomUUID())).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        String key = "p297-mpub-" + UUID.randomUUID();
        masterPublish(pass, key).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));

        String versions = versions(pass);
        assertThat(JsonPath.<Integer>read(versions, "$.data.items.length()")).isEqualTo(1);
        assertThat(JsonPath.<String>read(versions, "$.data.items[0].status")).isEqualTo("effective");
        assertThat(JsonPath.<Object>read(versions, "$.data.items[0].review_id")).isNull();
        String first = JsonPath.read(versions, "$.data.items[0].id");
        assertThat(ticketStatus(pass)).isEqualTo("published");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE object_id = ?", Integer.class, first)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'ticket_type.rule_version.approved'"
                + " AND event_result = 'success' AND business_id = ? AND actor_id = ? AND idempotency_key = ?"
                + " AND metadata->>'master_self_initiated' = 'true'", Integer.class, first, SessionIssuer.MASTER_USER_ID, key)).isEqualTo(1);
    }

    /** 297-R3：已上架跨域组合通票，最高权限本人新建会员价只进草稿版，本人提交即生效——派生价入表在售、第 2 版生效、第 1 版已被取代。 */
    @Test
    void masterSubmitOfDraftVersionWithPriceRuleTakesEffectAtOnce() throws Exception {
        String pass = publishedComboPass("h");
        String member = JsonPath.read(asMaster(post("/api/v1/ticketing/price-rules"), """
                {"idempotency_key": "p297-mpr-%s", "ticket_type_id": "%s", "variant_type": "member", "price_mode": "derive_from_calendar_price",
                 "discount_rate": 90}
                """.formatted(UUID.randomUUID(), pass)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_price_rules WHERE id = ?", Integer.class, member)).isZero();

        masterPublish(pass, "p297-mpub-" + UUID.randomUUID()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("published"));

        assertThat(jdbc.queryForObject("SELECT status FROM ticket_price_rules WHERE id = ?", String.class, member)).isEqualTo("active");
        String swapped = versions(pass);
        assertThat(JsonPath.<List<String>>read(swapped, "$.data.items[?(@.version_no == 2)].status")).containsExactly("effective");
        assertThat(JsonPath.<List<String>>read(swapped, "$.data.items[?(@.version_no == 1)].status")).containsExactly("superseded");
        assertThat(JsonPath.<List<String>>read(swapped, "$.data.items[?(@.version_no == 2)].price_rules[*].id")).containsExactly(member);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_type_rule_versions WHERE ticket_type_id = ?"
                + " AND status IN ('draft', 'pending_review')", Integer.class, pass)).isZero();
    }

    /**
     * 297-R3（BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」；第十八批裁决 71）：经平台驳回、退回草稿（保留复核单号）的版本，
     * 再由最高权限本人提交即生效时不挂那张已驳回的复核单，review_id 为空；首次发布与已上架票的草稿版两条路径相同。
     */
    @Test
    void masterSubmitAfterRejectionLeavesNoReviewId() throws Exception {
        String pass = comboPass("i");
        publish(pass).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        String firstReview = reviewOf(pass);
        decide(firstReview, "rejected").andExpect(status().isOk());
        assertThat(JsonPath.<String>read(versions(pass), "$.data.items[0].review_id")).isEqualTo(firstReview);

        masterPublish(pass, "p297-mpub-" + UUID.randomUUID()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("published"));
        String first = versions(pass);
        assertThat(JsonPath.<Integer>read(first, "$.data.items[0].version_no")).isEqualTo(1);
        assertThat(JsonPath.<String>read(first, "$.data.items[0].status")).isEqualTo("effective");
        assertThat(JsonPath.<Object>read(first, "$.data.items[0].review_id")).isNull();

        priceRule(pass, "member", 90).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        publish(pass).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        String secondReview = reviewOf(pass);
        decide(secondReview, "rejected").andExpect(status().isOk());
        assertThat(JsonPath.<String>read(versions(pass), "$.data.items[0].review_id")).isEqualTo(secondReview);

        masterPublish(pass, "p297-mpub-" + UUID.randomUUID()).andExpect(status().isOk());
        String second = versions(pass);
        assertThat(JsonPath.<Integer>read(second, "$.data.items[0].version_no")).isEqualTo(2);
        assertThat(JsonPath.<String>read(second, "$.data.items[0].status")).isEqualTo("effective");
        assertThat(JsonPath.<Object>read(second, "$.data.items[0].review_id")).isNull();
    }

    /** 最高权限本人发布：不带最终审批人。 */
    private ResultActions masterPublish(String type, String key) throws Exception {
        return asMaster(post("/api/v1/ticketing/ticket-types/" + type + "/publish"), """
                {"idempotency_key": "%s", "reason": "本人核对后上架"}
                """.formatted(key));
    }

    private ResultActions asMaster(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(master()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions priceRule(String type, String variant, int discount) throws Exception {
        return priceRule(type, variant, discount, "p297-pr-" + UUID.randomUUID());
    }

    private ResultActions priceRule(String type, String variant, int discount, String key) throws Exception {
        return send(post("/api/v1/ticketing/price-rules"), """
                {"idempotency_key": "%s", "ticket_type_id": "%s", "variant_type": "%s", "price_mode": "derive_from_calendar_price",
                 "discount_rate": %d}
                """.formatted(key, type, variant, discount));
    }

    private ResultActions archive(String rule) throws Exception {
        return send(post("/api/v1/ticketing/price-rules/" + rule + "/archive"), """
                {"idempotency_key": "p297-ar-%s", "reason": "这档价先不卖"}
                """.formatted(UUID.randomUUID()));
    }

    /** 后台渠道、昨天起 30 天、每张加 10 元的动态价草稿，返回规则编号。 */
    private String dynamicRule(String type, String name) throws Exception {
        String body = send(post("/api/v1/ticketing/dynamic-pricing-rules"), """
                {"idempotency_key": "p297-dr-%s", "ticket_type_id": "%s", "rule_name": "%s", "priority": 10,
                 "effective_from": "%s", "effective_to": "%s", "sales_channel": "admin", "adjustment_type": "fixed_delta",
                 "adjustment_amount_cent": 1000}
                """.formatted(UUID.randomUUID(), type, name, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.id");
    }

    private ResultActions publishDynamic(String rule) throws Exception {
        return publishDynamic(rule, "p297-dp-" + UUID.randomUUID());
    }

    private ResultActions publishDynamic(String rule, String key) throws Exception {
        return send(post("/api/v1/ticketing/dynamic-pricing-rules/" + rule + "/publish"), """
                {"idempotency_key": "%s", "action": "publish", "reason": "旺季调价"}
                """.formatted(key));
    }

    private String dynamicStatus(String rule) {
        return jdbc.queryForObject("SELECT status FROM ticket_dynamic_pricing_rules WHERE id = ?", String.class, rule);
    }

    /** 售票链既有前置：该票种已批准、已公告、已生效的销售合同（下单时锁定）。 */
    private void salesContract(String type) {
        TicketSalesContractTestFixtures.approvedPublished(jdbc, new ObjectMapper(), type, project, bar);
        contractTypes.add(type);
    }

    private String effectiveVersion(String type) {
        return jdbc.queryForObject("SELECT id FROM ticket_type_rule_versions WHERE ticket_type_id = ? AND status = 'effective'",
                String.class, type);
    }

    private ResultActions orderRequest(String type, String priceRule) throws Exception {
        return send(post("/api/v1/ticketing/orders"), """
                {"idempotency_key": "p297-order-%s", "ticket_type_id": "%s", "quantity": 1, "sales_channel": "admin"%s}
                """.formatted(UUID.randomUUID(), type, priceRule == null ? "" : ", \"price_rule_id\": \"" + priceRule + "\""));
    }

    /** 票务运营在后台下单（官方后台渠道），断言下单金额，返回订单号。 */
    private String order(String type, String priceRule, int amountCent) throws Exception {
        String body = orderRequest(type, priceRule).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(body, "$.data.id");
        orders.add(id);
        assertThat(JsonPath.<Integer>read(body, "$.data.amount_cent")).isEqualTo(amountCent);
        return id;
    }

    private String orderVersion(String order) {
        return jdbc.queryForObject("SELECT ticket_type_rule_version_id FROM ticket_orders WHERE id = ?", String.class, order);
    }

    /** 首次发布经审批上架的跨域组合通票（第 1 版生效）。 */
    private String publishedComboPass(String tag) throws Exception {
        String pass = comboPass(tag);
        publish(pass).andExpect(status().isOk());
        approve(reviewOf(pass)).andExpect(status().isOk());
        return pass;
    }

    /** 酒吧入场 100 元、该吧零元平台出资赠酒、游乐入场 120 元；九折、平日价 199 元。 */
    private String comboPass(String tag) throws Exception {
        String pass = createTicketType(tag, 19900L, 90, null);
        bind(pass, template(bar, "酒吧入场", "entry", null), 10000L, null).andExpect(status().isOk());
        bind(pass, template(bar, "平台赠酒", "gift_drink", product), 0L, "platform_funded").andExpect(status().isOk());
        bind(pass, template(park, "游乐入场", "entry", null), 12000L, null).andExpect(status().isOk());
        return pass;
    }

    private String createTicketType(String tag, long priceCent, int discount, String barId) throws Exception {
        String body = send(post("/api/v1/ticketing/ticket-types"), """
                {"idempotency_key": "p297-%s-%s", "ticket_name": "派生价版本票%d%d", "base_calendar_price_mode": "weekday_weekend_holiday",
                 "weekday_price_cent": %d, "weekend_price_cent": %d, "holiday_price_cent": %d, "stock_required": false,
                 "combo_discount_rate": %d, "bar_id": %s}
                """.formatted(tag, suffix, n, ticketTypes.size(), priceCent, priceCent, priceCent, discount,
                        barId == null ? "null" : "\"" + barId + "\""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(body, "$.data.id");
        ticketTypes.add(id);
        return id;
    }

    private String template(String merchant, String name, String type, String barItemRef) throws Exception {
        String body = send(post("/api/v1/ticketing/entitlement-templates"), """
                {"idempotency_key": "p297-tpl-%s", "name": "派生价版本 · %s%d", "entitlement_type": "%s",
                 "default_total_times": 1, "default_value_cent": 0, "status": "active", "merchant_id": "%s",
                 "catalog_item_code": null, "exclusive_merchant_id": null, "bar_item_ref": %s}
                """.formatted(UUID.randomUUID(), name, n, type, merchant, barItemRef == null ? "null" : "\"" + barItemRef + "\""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(body, "$.data.id");
        templates.add(id);
        return id;
    }

    private ResultActions bind(String type, String template, long componentPriceCent, String costBearer) throws Exception {
        return send(post("/api/v1/ticketing/ticket-types/" + type + "/entitlements"), """
                {"idempotency_key": "p297-bind-%s", "entitlement_template_id": "%s", "quantity": 1,
                 "total_times": 1, "num_people": 1, "component_price_cent": %d, "cost_bearer": %s}
                """.formatted(UUID.randomUUID(), template, componentPriceCent, costBearer == null ? "null" : "\"" + costBearer + "\""));
    }

    /** 后台发布：带最终审批人（非跨域票种不看该字段）。 */
    private ResultActions publish(String type) throws Exception {
        return send(post("/api/v1/ticketing/ticket-types/" + type + "/publish"), """
                {"idempotency_key": "p297-pub-%s", "action": "publish", "reason": "组合通票配好上架", "final_approver_id": "%s"}
                """.formatted(UUID.randomUUID(), approver));
    }

    private String versions(String type) throws Exception {
        return mockMvc.perform(get("/api/v1/ticketing/ticket-types/" + type + "/rule-versions")
                        .with(session(operator, "platform_admin", "org_1")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private ResultActions approve(String review) throws Exception {
        return decide(review, "approved");
    }

    /** 审批人在主管复核接口办理：approved 通过、rejected 驳回。 */
    private ResultActions decide(String review, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(session(approver, "staff", project))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decision + "\",\"comment\":\"已核对\",\"idempotency_key\":\"p297-ap-" + UUID.randomUUID() + "\"}"));
    }

    private String reviewOf(String type) {
        return jdbc.queryForObject("SELECT review_id FROM ticket_type_rule_versions WHERE ticket_type_id = ? AND status = 'pending_review'",
                String.class, type);
    }

    private String ticketStatus(String type) {
        return jdbc.queryForObject("SELECT status FROM ticket_types WHERE id = ?", String.class, type);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(session(operator, "platform_admin", "org_1")).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void merchant(String id, String name, String type) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'active', ?, now(), now())", id, name + n, type, project);
    }

    private String role(String name, String dataScope, boolean allProjects, List<String> permissions) {
        String roleId = "role_p297_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, view_all_projects, created_at)"
                + " VALUES (?, ?, '跨域组合通票派生价与动态价版本回归', ?, 'active', ?, now())", roleId, name, dataScope, allProjects);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        return roleId;
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与账号表当前行一致）。
    private RequestPostProcessor session(String userId, String accountType, String organization) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(userId, userId, accountType, organization, null, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
