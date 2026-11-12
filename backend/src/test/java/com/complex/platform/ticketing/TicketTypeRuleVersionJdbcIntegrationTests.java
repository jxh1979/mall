package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.settlement.SettlementSettledConsumer;
import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.support.PassPoolFixtures;
import com.complex.platform.support.TicketSalesContractTestFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
 * 第十八批 AJ 段第 236 条 236-R3（第十八批裁决 52 ②）：跨域组合通票首次发布经项目之上的平台权限审批、获批即上架，审批即留一版票种规则。
 *
 * <p>依据：ENUM:32「跨域组合通票的定价与域间结算规则由项目之上的平台权限统一审批」；BSM:754「凑整产生的差额按各业态价格比例，与折扣一并分摊到各域……
 * 每次发布时把凑整差额金额及各域分摊数额记入票种规则版本、发布后不得再改」；BSM:749「零元免费权益不参与域间分配」；ENUM:30「草稿（规则未发布）」；
 * PGM:528「发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级节点，逐级审批……任一节点驳回则整单退回起草人」；
 * 决策台账第 75 条 18C「项目之上的平台权限审批（……能看全部项目，不得自批）」。
 *
 * <p>236-R7（第十八批裁决 61「改动另存一份草稿版审批，审批期间照旧版卖，获批后自动换成新版」；裁决 62「已上架的非跨域票一律不许加别的经营域收费权益，
 * 要做跨域组合通票就另建票种」）：已上架跨域组合通票的改价、加权益、改最低赠酒杯数存进同一份草稿版，提交、驳回、撤回、获批换版；已上架非跨域票加别域有价权益 409。
 *
 * <p>236-R4、236-R6（BSM:756「订单保存基础价、动态价、全部命中资格价、最终选中价和规则版本。」；BSM:1754「每项权益都必须绑定实际提供方和审批版本，
 * 避免悬空账。」）：订单记下单时生效的票种规则版本，通票结算审阅流水带出该笔订单所属版本号。
 *
 * <p>真实接口与授权链：票务运营按后台同键（蛇形键）建票、建模板、绑定、发布；审批人经主管复核接口审批。不开测试事务，@AfterEach 按票种清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class TicketTypeRuleVersionJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SettlementSettledConsumer settledConsumer;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final long n = Long.parseLong(suffix, 16);
    private final String project = "prj_r3_" + suffix;
    private final String bar = "bar_r3_" + suffix;
    private final String park = "amu_r3_" + suffix;
    private final String restaurant = "rest_r3_" + suffix;
    private final String product = "bp_r3_" + suffix;
    private final String catalogCode = "sku_r3_" + suffix;
    /** 票务运营（提交人）。 */
    private final String operator = "usr_r3_op_" + suffix;
    /** 提交人的直接上级：持跨域组合通票票种规则审批权限、能看全部项目。 */
    private final String approver = "usr_r3_ap_" + suffix;
    /** 再上一级：持审批权限，但角色只勾了本项目。 */
    private final String narrow = "usr_r3_one_" + suffix;
    /** 持审批权限、能看全部项目，但不在提交人的直接上级链上。 */
    private final String outsider = "usr_r3_out_" + suffix;
    private final List<String> ticketTypes = new ArrayList<>();
    private final List<String> templates = new ArrayList<>();
    private final List<String> orders = new ArrayList<>();
    private final List<String> contractTypes = new ArrayList<>();

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '规则版本综合体', 'active', now(), now())", project);
        merchant(bar, "规则版本酒吧", "bar");
        merchant(park, "规则版本乐园", "amusement");
        merchant(restaurant, "规则版本餐厅", "catering");
        // 赠酒商品引用已获批生效的内部结算价目录条目（236-R2 发布闸）。
        jdbc.update("INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent, status, is_draft,"
                + " updated_by, created_at, updated_at) VALUES (?, ?, ?, 'beverage', '赠酒一杯', 2000, 'active', FALSE, 'seed_r3', now(), now())",
                catalogCode, catalogCode, bar);
        jdbc.update("INSERT INTO bar_products (id, bar_id, product_name, product_type, price_cent, currency, inventory_item_id,"
                + " stock_location_id, stock_deduction_required, status, internal_settlement_price_cent, catalog_item_code, created_at, updated_at)"
                + " VALUES (?, ?, '赠酒一杯', 'beverage', 3800, 'CNY', NULL, 'default', FALSE, 'active', 2000, ?, now(), now())",
                product, bar, catalogCode);

        String operatorRole = role("跨域通票发布运营" + n, "platform", false, List.of("ticket.product.manage", "ticket.product.view",
                "ticket.entitlement.manage", "ticket.order.create", "ticket.ticket.issue", "finance.settlement.view",
                "governance.supervisor_review.view", "governance.supervisor_review.approve"));
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", operator, operator);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", operator, operatorRole);

        List<String> approvePermissions = List.of("ticket.product.publish.approve", "governance.supervisor_review.view",
                "governance.supervisor_review.approve");
        String allProjects = role("跨域通票规则审批" + n, "platform", true, approvePermissions);
        String oneProject = role("跨域通票规则审批单项目" + n, "project", false, approvePermissions);
        jdbc.update("INSERT INTO role_viewable_projects (role_id, viewable_id) VALUES (?, ?)", oneProject, project);
        staff(approver, allProjects);
        staff(outsider, allProjects);
        staff(narrow, oneProject);
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", operator, approver);
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", approver, narrow);
    }

    @AfterEach
    void cleanup() {
        PassPoolFixtures.deleteByProject(jdbc, project);
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
     * 酒吧入场 100 元、游乐入场 120 元、该吧零元平台出资赠酒 1 杯，九折算出 198 元、填 199 元：发布即提交审批、票仍为草稿不可售；
     * 第 1 版记合计 220 元、算出 198 元、凑整差额 1 元，酒吧域按填价得 90.45 元（凑整 0.45、折扣 10 元）、游乐域 108.55 元（凑整 0.55、折扣 12 元），
     * 零元赠酒在权益组成里、不在各域分摊里；直接上级审批通过后票上架、第 1 版生效。
     */
    @Test
    void crossDomainPassFirstPublishGoesToPlatformApprovalAndKeepsRuleVersion() throws Exception {
        String pass = comboPass("a");

        publish(pass, approver).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));

        String versions = versions(pass);
        assertThat(JsonPath.<Integer>read(versions, "$.data.items.length()")).isEqualTo(1);
        assertThat(JsonPath.<Integer>read(versions, "$.data.items[0].version_no")).isEqualTo(1);
        assertThat(JsonPath.<String>read(versions, "$.data.items[0].status")).isEqualTo("pending_review");
        assertThat(JsonPath.<Integer>read(versions, "$.data.items[0].weekday_price_cent")).isEqualTo(19900);
        assertThat(JsonPath.<Integer>read(versions, "$.data.items[0].component_total_cent")).isEqualTo(22000);
        assertThat(JsonPath.<Integer>read(versions, "$.data.items[0].computed_price_cent")).isEqualTo(19800);
        assertThat(JsonPath.<Integer>read(versions, "$.data.items[0].rounding_diff_cent")).isEqualTo(100);
        assertThat(JsonPath.<List<String>>read(versions, "$.data.items[0].domain_allocations[*].domain"))
                .containsExactlyInAnyOrder("bar", "amusement");
        assertThat(share(versions, "bar", "share_cent")).isEqualTo(9045);
        assertThat(share(versions, "bar", "rounding_share_cent")).isEqualTo(45);
        assertThat(share(versions, "bar", "discount_share_cent")).isEqualTo(1000);
        assertThat(share(versions, "amusement", "share_cent")).isEqualTo(10855);
        assertThat(share(versions, "amusement", "rounding_share_cent")).isEqualTo(55);
        assertThat(share(versions, "amusement", "discount_share_cent")).isEqualTo(1200);
        assertThat(JsonPath.<Integer>read(versions, "$.data.items[0].entitlements.length()")).isEqualTo(3);
        assertThat(JsonPath.<List<String>>read(versions, "$.data.items[0].entitlements[?(@.component_price_cent == 0)].cost_bearer"))
                .containsExactly("platform_funded");
        assertThat(JsonPath.<List<String>>read(versions, "$.data.items[0].entitlements[*].provider_merchant_id"))
                .containsExactlyInAnyOrder(bar, bar, park);
        String versionId = JsonPath.read(versions, "$.data.items[0].id");
        assertThat(jdbc.queryForObject("SELECT project_id FROM ticket_type_rule_versions WHERE id = ?", String.class, versionId))
                .isEqualTo(project);

        send(operator, post("/api/v1/ticketing/orders"), """
                {"idempotency_key": "r3-order-%s", "ticket_type_id": "%s", "quantity": 1, "sales_channel": "front_desk"}
                """.formatted(suffix, pass))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_NOT_SELLABLE"));

        String review = JsonPath.read(versions, "$.data.items[0].review_id");
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + review).with(session(approver, "staff", project)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action_type").value("ticket.type.rule.publish"))
                .andExpect(jsonPath("$.data.object_type").value("ticket_type_rule_version"))
                .andExpect(jsonPath("$.data.reason").value(Matchers.startsWith("发布理由：")));
        String policies = mockMvc.perform(get("/api/v1/platform/supervisor-review-policies").with(session(operator, "platform_admin", "org_1")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(policies, "$.data.items[*].action_type")).contains("ticket.type.rule.publish");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'ticket_price.published' AND event_result = 'success'"
                + " AND business_id = ? AND metadata->>'rule_version_id' = ? AND metadata->>'rule_version_status' = 'pending_review'",
                Integer.class, pass, versionId)).isEqualTo(1);

        approve(approver, review, "approved").andExpect(status().isOk());

        assertThat(ticketStatus(pass)).isEqualTo("published");
        String decided = versions(pass);
        assertThat(JsonPath.<String>read(decided, "$.data.items[0].status")).isEqualTo("effective");
        assertThat(JsonPath.<String>read(decided, "$.data.items[0].decided_at")).isNotNull();
    }

    /**
     * 最终审批人：不指定 400；角色只勾了本项目 400（须能看全部项目）；能看全部项目但不在提交人直接上级链上 400 FINAL_APPROVER_NOT_IN_CHAIN，
     * 三次都不留待审版本；指定直接上级后提交人自己批 403。
     */
    @Test
    void finalApproverMustSeeAllProjectsSitOnTheChainAndNotBeTheSubmitter() throws Exception {
        String pass = comboPass("b");

        publish(pass, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        publish(pass, narrow).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("须能看全部项目")));
        publish(pass, outsider).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("FINAL_APPROVER_NOT_IN_CHAIN"));
        assertThat(JsonPath.<Integer>read(versions(pass), "$.data.items.length()")).isZero();
        assertThat(ticketStatus(pass)).isEqualTo("draft");

        publish(pass, approver).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        approve(operator, reviewOf(pass), "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        assertThat(ticketStatus(pass)).isEqualTo("draft");
    }

    /** 驳回：第 1 版回草稿、保留复核单号，票仍为草稿；再发布仍是第 1 版、重新待审。 */
    @Test
    void rejectedVersionReturnsToDraftAndIsResubmittedAsTheSameVersion() throws Exception {
        String pass = comboPass("c");
        publish(pass, approver).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        String first = reviewOf(pass);

        approve(approver, first, "rejected").andExpect(status().isOk());

        String rejected = versions(pass);
        assertThat(JsonPath.<String>read(rejected, "$.data.items[0].status")).isEqualTo("draft");
        assertThat(JsonPath.<String>read(rejected, "$.data.items[0].review_id")).isEqualTo(first);
        assertThat(ticketStatus(pass)).isEqualTo("draft");

        publish(pass, approver).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        String again = versions(pass);
        assertThat(JsonPath.<Integer>read(again, "$.data.items.length()")).isEqualTo(1);
        assertThat(JsonPath.<Integer>read(again, "$.data.items[0].version_no")).isEqualTo(1);
        assertThat(JsonPath.<String>read(again, "$.data.items[0].status")).isEqualTo("pending_review");
        assertThat(JsonPath.<String>read(again, "$.data.items[0].review_id")).isNotEqualTo(first);
    }

    /** 待审期间改价、加权益、改最低赠酒杯数、再发布一律 409，现表不动、复核单仍待审。 */
    @Test
    void pendingVersionBlocksEditsAndRepublish() throws Exception {
        String pass = comboPass("d");
        publish(pass, approver).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));

        send(operator, put("/api/v1/ticketing/ticket-types/" + pass + "/pricing"), """
                {"idempotency_key": "r3-price-%s", "reason": "改价", "combo_discount_rate": 90, "weekday_price_cent": 19800,
                 "weekend_price_cent": 19800, "holiday_price_cent": 19800}
                """.formatted(suffix))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("TICKET_RULE_VERSION_PENDING"));
        assertThat(jdbc.queryForObject("SELECT weekday_price_cent FROM ticket_types WHERE id = ?", Long.class, pass)).isEqualTo(19900L);
        bind(pass, template(restaurant, "餐饮小食", "dining", null), 0L, "platform_funded")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("TICKET_RULE_VERSION_PENDING"));
        send(operator, put("/api/v1/ticketing/ticket-types/" + pass + "/min-gift-cups"), """
                {"idempotency_key": "r3-cups-%s", "min_gift_cups": 1, "min_gift_cups_lower": 1, "min_gift_cups_upper": 2}
                """.formatted(suffix))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("TICKET_RULE_VERSION_PENDING"));
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + reviewOf(pass)).with(session(approver, "staff", project)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.review_status").value("pending_review"));

        publish(pass, approver).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_RULE_VERSION_PENDING"));
    }

    /** 酒吧通票（酒吧入场有价＋零元赠酒＋零元平台出资餐饮小食）与单一酒吧门票不是跨域组合通票：照旧直接上架、不建规则版本。 */
    @Test
    void sameDomainPassAndSingleBarTicketPublishDirectly() throws Exception {
        String barPass = createTicketType("e1", 10000L, 100, null);
        bind(barPass, template(bar, "酒吧入场", "entry", null), 10000L, null).andExpect(status().isOk());
        bind(barPass, template(bar, "平台赠酒", "gift_drink", product), 0L, "platform_funded").andExpect(status().isOk());
        bind(barPass, template(restaurant, "餐饮小食", "dining", null), 0L, "platform_funded").andExpect(status().isOk());
        publish(barPass, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        assertThat(JsonPath.<Integer>read(versions(barPass), "$.data.items.length()")).isZero();

        String single = createTicketType("e2", 5000L, 100, bar);
        bind(single, template(bar, "单店入场", "entry", null), 5000L, null).andExpect(status().isOk());
        publish(single, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        assertThat(JsonPath.<Integer>read(versions(single), "$.data.items.length()")).isZero();
    }

    /**
     * 236-R7（裁决 61）：已上架跨域组合通票加有价餐饮、改价、改最低赠酒杯数都存进同一份第 2 版草稿，现表、客人端与下单仍按第 1 版；草稿赠酒不够数时
     * 提交被发布闸拦下，补齐后提交审批、待审期间再改 409；获批后在审批同一事务里换成第 2 版（价格、新增绑定、最低杯数），第 1 版已被取代且内容不变，票一直在架。
     */
    @Test
    void publishedComboPassEditsGoIntoOneDraftVersionAndSwapInOnApproval() throws Exception {
        String pass = publishedComboPass("g");
        String dining = template(restaurant, "有价餐饮", "dining", null);

        bind(pass, dining, 3000L, null).andExpect(status().isOk());
        assertThat(bindingCount(pass)).isEqualTo(3);
        assertThat(ticketStatus(pass)).isEqualTo("published");
        String drafted = versions(pass);
        assertThat(JsonPath.<Integer>read(drafted, "$.data.items.length()")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(drafted, "$.data.items[0].version_no")).isEqualTo(2);
        assertThat(JsonPath.<String>read(drafted, "$.data.items[0].status")).isEqualTo("draft");
        assertThat(JsonPath.<List<String>>read(drafted, "$.data.items[0].entitlements[*].entitlement_template_id")).contains(dining);
        String draftId = JsonPath.read(drafted, "$.data.items[0].id");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'ticket_product_entitlement.bound'"
                + " AND event_result = 'success' AND metadata->>'rule_version_id' = ? AND metadata->>'rule_version_status' = 'draft'",
                Integer.class, draftId)).isEqualTo(1);

        reprice(pass, "g1", 22500L, 90).andExpect(status().isOk());
        send(operator, put("/api/v1/ticketing/ticket-types/" + pass + "/min-gift-cups"), """
                {"idempotency_key": "r7-cups-%s", "min_gift_cups": 2, "min_gift_cups_lower": 1, "min_gift_cups_upper": 3}
                """.formatted(suffix)).andExpect(status().isOk());
        assertThat(weekdayPrice(pass)).isEqualTo(19900L);
        assertThat(minGiftCups(pass)).isEqualTo(1);
        String saved = versions(pass);
        assertThat(JsonPath.<Integer>read(saved, "$.data.items.length()")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(saved, "$.data.items[0].weekday_price_cent")).isEqualTo(22500);
        assertThat(JsonPath.<Integer>read(saved, "$.data.items[0].min_gift_cups")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(saved, "$.data.items[0].price_check.computed_price_cent")).isEqualTo(22500);
        assertThat(onSale(pass, "weekday_price_cent")).containsExactly(19900);
        assertThat(onSale(pass, "entitlements[*]")).isNotEmpty().noneMatch(name -> String.valueOf(name).contains("有价餐饮"));

        publish(pass, approver).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PASS_BAR_GIFT_COVERAGE_INSUFFICIENT"));
        assertThat(JsonPath.<String>read(versions(pass), "$.data.items[0].status")).isEqualTo("draft");
        bind(pass, template(bar, "平台赠酒二", "gift_drink", product), 0L, "platform_funded").andExpect(status().isOk());
        publish(pass, approver).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        assertThat(JsonPath.<String>read(versions(pass), "$.data.items[0].status")).isEqualTo("pending_review");
        assertThat(ticketStatus(pass)).isEqualTo("published");
        reprice(pass, "g2", 19900L, 90).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_RULE_VERSION_PENDING"));
        bind(pass, template(park, "游乐快线", "entry", null), 1000L, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_RULE_VERSION_PENDING"));

        approve(approver, reviewOf(pass), "approved").andExpect(status().isOk());

        assertThat(ticketStatus(pass)).isEqualTo("published");
        assertThat(weekdayPrice(pass)).isEqualTo(22500L);
        assertThat(bindingCount(pass)).isEqualTo(5);
        assertThat(minGiftCups(pass)).isEqualTo(2);
        String swapped = versions(pass);
        assertThat(JsonPath.<List<String>>read(swapped, "$.data.items[?(@.version_no == 2)].status")).containsExactly("effective");
        assertThat(JsonPath.<List<String>>read(swapped, "$.data.items[?(@.version_no == 1)].status")).containsExactly("superseded");
        assertThat(JsonPath.<List<Integer>>read(swapped, "$.data.items[?(@.version_no == 1)].rounding_diff_cent")).containsExactly(100);
        assertThat(JsonPath.<List<Integer>>read(swapped,
                "$.data.items[?(@.version_no == 1)].domain_allocations[?(@.domain == 'bar')].share_cent")).containsExactly(9045);
        assertThat(onSale(pass, "weekday_price_cent")).containsExactly(22500);
        assertThat(onSale(pass, "entitlements[*]")).anyMatch(name -> String.valueOf(name).contains("有价餐饮"));
    }

    /**
     * 236-R7③（裁决 61；PGM:528「任一节点驳回则整单退回起草人」）：草稿版被驳回退回草稿、内容保留，现表照卖；撤回草稿后现表不变、再改另起新版；
     * 待审时撤回即作废复核单（审批人再批 404）；没有未结版本再撤回 409；首次发布待审的票撤回后仍是草稿、可直接改价。
     */
    @Test
    void rejectedDraftKeepsContentAndWithdrawCancelsReviewWhileCurrentVersionKeepsSelling() throws Exception {
        String pass = publishedComboPass("j");
        reprice(pass, "j1", 19800L, 90).andExpect(status().isOk());
        assertThat(weekdayPrice(pass)).isEqualTo(19900L);
        publish(pass, approver).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        String first = reviewOf(pass);
        approve(approver, first, "rejected").andExpect(status().isOk());

        String rejected = versions(pass);
        assertThat(JsonPath.<Integer>read(rejected, "$.data.items[0].version_no")).isEqualTo(2);
        assertThat(JsonPath.<String>read(rejected, "$.data.items[0].status")).isEqualTo("draft");
        assertThat(JsonPath.<Integer>read(rejected, "$.data.items[0].weekday_price_cent")).isEqualTo(19800);
        assertThat(JsonPath.<String>read(rejected, "$.data.items[0].review_id")).isEqualTo(first);
        assertThat(weekdayPrice(pass)).isEqualTo(19900L);
        assertThat(ticketStatus(pass)).isEqualTo("published");
        assertThat(onSale(pass, "weekday_price_cent")).containsExactly(19900);

        String withdrawn = withdraw(pass).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version_no").value(2))
                .andExpect(jsonPath("$.data.status").value("withdrawn"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'ticket_type.rule_version.withdrawn'"
                + " AND event_result = 'success' AND business_id = ?", Integer.class, JsonPath.<String>read(withdrawn, "$.data.id"))).isEqualTo(1);
        assertThat(weekdayPrice(pass)).isEqualTo(19900L);

        reprice(pass, "j2", 19800L, 90).andExpect(status().isOk());
        String third = versions(pass);
        assertThat(JsonPath.<Integer>read(third, "$.data.items[0].version_no")).isEqualTo(3);
        assertThat(JsonPath.<String>read(third, "$.data.items[0].status")).isEqualTo("draft");
        publish(pass, approver).andExpect(status().isOk());
        String review = reviewOf(pass);
        withdraw(pass).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("withdrawn"));
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + review).with(session(operator, "platform_admin", "org_1")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.review_status").value("rejected"));
        approve(approver, review, "approved").andExpect(status().is4xxClientError());
        assertThat(JsonPath.<String>read(versions(pass), "$.data.items[0].status")).isEqualTo("withdrawn");
        withdraw(pass).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_RULE_VERSION_NOT_OPEN"));
        assertThat(weekdayPrice(pass)).isEqualTo(19900L);
        assertThat(ticketStatus(pass)).isEqualTo("published");

        String fresh = comboPass("k");
        publish(fresh, approver).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        withdraw(fresh).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("withdrawn"));
        assertThat(ticketStatus(fresh)).isEqualTo("draft");
        reprice(fresh, "k1", 19800L, 90).andExpect(status().isOk());
        assertThat(weekdayPrice(fresh)).isEqualTo(19800L);
    }

    /**
     * 第十八批裁决 62（控制席说明「已上架票加别域零元权益不受本裁决限制」）：已上架酒吧通票加有价游乐（误差内、超误差）、已上架纯游乐通票加有价酒吧入场（缺赠酒）
     * 一律 409，绑定不落库、票不退回草稿、不建版本，失败留痕；加别域零元平台出资餐饮、加同一家吧的有价兑换券照现行。
     */
    @Test
    void publishedSingleDomainPassCannotTakePaidEntitlementFromAnotherDomain() throws Exception {
        String barPass = createTicketType("l1", 5000L, 100, null);
        bind(barPass, template(bar, "酒吧入场", "entry", null), 5000L, null).andExpect(status().isOk());
        bind(barPass, template(bar, "平台赠酒", "gift_drink", product), 0L, "platform_funded").andExpect(status().isOk());
        publish(barPass, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        String ride = template(park, "游乐入场", "entry", null);

        String key = "r7-l-" + UUID.randomUUID();
        bind(barPass, ride, 300L, null, key).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_CROSS_DOMAIN_NEW_TYPE_REQUIRED"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("另建票种")));
        bind(barPass, ride, 12000L, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_CROSS_DOMAIN_NEW_TYPE_REQUIRED"));
        assertThat(bindingCount(barPass)).isEqualTo(2);
        assertThat(ticketStatus(barPass)).isEqualTo("published");
        assertThat(JsonPath.<Integer>read(versions(barPass), "$.data.items.length()")).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'ticket_product_entitlement.bound_failed'"
                + " AND event_result = 'failure' AND idempotency_key = ?", Integer.class, key)).isEqualTo(1);

        String parkPass = createTicketType("l2", 8000L, 100, null);
        bind(parkPass, template(park, "纯游乐入场", "entry", null), 8000L, null).andExpect(status().isOk());
        publish(parkPass, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        bind(parkPass, template(bar, "新酒吧入场", "entry", null), 300L, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_CROSS_DOMAIN_NEW_TYPE_REQUIRED"));
        assertThat(bindingCount(parkPass)).isEqualTo(1);
        assertThat(ticketStatus(parkPass)).isEqualTo("published");

        bind(barPass, template(restaurant, "餐饮小食", "dining", null), 0L, "platform_funded").andExpect(status().isOk());
        bind(barPass, template(bar, "酒水兑换券", "gift_drink", product), 300L, null).andExpect(status().isOk());
        assertThat(bindingCount(barPass)).isEqualTo(4);
        assertThat(ticketStatus(barPass)).isEqualTo("published");
    }

    /**
     * 裁决 62 只管已上架的票：已上架酒吧通票加同一家吧的有价兑换券跌破误差，照 V369 退回草稿（非跨域行为不变）；草稿票再加有价游乐照现行写现表、不建版本；
     * 改价过误差后发布即跨域组合通票首次发布，提交平台权限审批，获批上架、第 1 版生效。
     */
    @Test
    void draftTicketMayBecomeCrossDomainPassThroughApproval() throws Exception {
        String pass = createTicketType("m", 5000L, 100, null);
        bind(pass, template(bar, "酒吧入场", "entry", null), 5000L, null).andExpect(status().isOk());
        bind(pass, template(bar, "平台赠酒", "gift_drink", product), 0L, "platform_funded").andExpect(status().isOk());
        publish(pass, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));

        bind(pass, template(bar, "酒水兑换券", "gift_drink", product), 2000L, null).andExpect(status().isOk());
        assertThat(ticketStatus(pass)).isEqualTo("draft");
        bind(pass, template(park, "游乐入场", "entry", null), 12000L, null).andExpect(status().isOk());
        assertThat(ticketStatus(pass)).isEqualTo("draft");
        assertThat(bindingCount(pass)).isEqualTo(4);
        assertThat(JsonPath.<Integer>read(versions(pass), "$.data.items.length()")).isZero();

        reprice(pass, "m1", 19000L, 100).andExpect(status().isOk());
        publish(pass, approver).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("draft"));
        assertThat(JsonPath.<String>read(versions(pass), "$.data.items[0].status")).isEqualTo("pending_review");
        approve(approver, reviewOf(pass), "approved").andExpect(status().isOk());
        assertThat(ticketStatus(pass)).isEqualTo("published");
        assertThat(JsonPath.<String>read(versions(pass), "$.data.items[0].status")).isEqualTo("effective");
    }

    /**
     * 236-R4（BSM:756「订单保存基础价、动态价、全部命中资格价、最终选中价和规则版本。」；裁决 61「下单定价与出票都要改成读「当前生效版」」）与
     * 236-R6（BSM:1754「每项权益都必须绑定实际提供方和审批版本，避免悬空账。」）：第 1 版生效时下单 A，记第 1 版、按 199 元；加有价餐饮 30 元并改价
     * 225 元的第 2 版获批后下单 B，记第 2 版、按 225 元，B 出票含有价餐饮；A 换版后才出票，所记仍是第 1 版；纯游乐通票（非跨域）的订单 C 不记版本。
     * 三单游乐入场核销入账后，通票结算审阅流水分别显示第 1 版、第 2 版与空（不是 0）。
     */
    @Test
    void ordersRecordRuleVersionInForceWhenPlacedAndSettlementReviewShowsIt() throws Exception {
        String pass = publishedComboPass("o");
        String parkPass = createTicketType("o2", 8000L, 100, null);
        bind(parkPass, template(park, "纯游乐入场", "entry", null), 8000L, null).andExpect(status().isOk());
        publish(parkPass, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
        salesContract(pass);
        salesContract(parkPass);
        String first = effectiveVersion(pass);

        String a = order(pass, 19900);
        assertThat(orderVersion(a)).isEqualTo(first);

        String dining = template(restaurant, "有价餐饮", "dining", null);
        bind(pass, dining, 3000L, null).andExpect(status().isOk());
        reprice(pass, "o1", 22500L, 90).andExpect(status().isOk());
        publish(pass, approver).andExpect(status().isOk());
        approve(approver, reviewOf(pass), "approved").andExpect(status().isOk());
        String second = effectiveVersion(pass);
        assertThat(second).isNotEqualTo(first);

        String b = order(pass, 22500);
        assertThat(orderVersion(b)).isEqualTo(second);
        issue(a);
        issue(b);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM issued_entitlements WHERE order_id = ? AND entitlement_template_id = ?",
                Integer.class, b, dining)).isEqualTo(1);
        assertThat(orderVersion(a)).as("出票回写订单不改下单时所记版本").isEqualTo(first);

        String c = order(parkPass, 8000);
        issue(c);
        assertThat(orderVersion(c)).isNull();

        String rowA = settleRide(a, "a");
        String rowB = settleRide(b, "b");
        String rowC = settleRide(c, "c");
        assertThat(ruleVersionNoBySettlementRow()).containsEntry(rowA, "1").containsEntry(rowB, "2").containsEntry(rowC, null);
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

    /** 票务运营在后台下单（官方后台渠道），断言下单金额，返回订单号。 */
    private String order(String type, int amountCent) throws Exception {
        String body = send(operator, post("/api/v1/ticketing/orders"), """
                {"idempotency_key": "r4-order-%s", "ticket_type_id": "%s", "quantity": 1, "sales_channel": "admin"}
                """.formatted(UUID.randomUUID(), type))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(body, "$.data.id");
        orders.add(id);
        assertThat(JsonPath.<Integer>read(body, "$.data.amount_cent")).isEqualTo(amountCent);
        return id;
    }

    private void issue(String order) throws Exception {
        send(operator, post("/api/v1/ticketing/orders/" + order + "/issue"), """
                {"idempotency_key": "r4-issue-%s", "reason": "客人在前台购票"}
                """.formatted(UUID.randomUUID())).andExpect(status().isOk());
    }

    private String orderVersion(String order) {
        return jdbc.queryForObject("SELECT ticket_type_rule_version_id FROM ticket_orders WHERE id = ?", String.class, order);
    }

    /** 该单游乐入场核销入账，走结算消费者真路径（同 ExclusiveDirectPayDomainShareJdbcIntegrationTests 游乐入场，本类不建游乐闸机），返回结算流水编号。 */
    private String settleRide(String order, String tag) {
        Map<String, Object> ride = jdbc.queryForMap("SELECT id, allocated_revenue_cent, parent_ticket_id, ticket_type_id FROM issued_entitlements"
                + " WHERE order_id = ? AND merchant_id = ? AND entitlement_type = 'entry'", order, park);
        String verification = "vf_r6_" + tag + "_" + suffix;
        Instant at = Instant.now();
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(verification, at, (String) ride.get("id"), order,
                (String) ride.get("ticket_type_id"), "amusement", park, 1, 1, 1, ((Number) ride.get("allocated_revenue_cent")).longValue(),
                at.plusSeconds(86_400), null, null, null, project, (String) ride.get("parent_ticket_id")));
        return "st_vs_" + verification;
    }

    /** 本项目通票结算审阅流水：流水编号 → 票种规则版本号原文（空值、缺键都记 null）。 */
    private Map<String, String> ruleVersionNoBySettlementRow() throws Exception {
        String body = mockMvc.perform(get("/api/v1/settlement/transactions").param("projectId", project).param("size", "50")
                        .with(session(operator, "platform_admin", "org_1")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Map<String, String> rows = new HashMap<>();
        for (JsonNode item : new ObjectMapper().readTree(body).path("data").path("items")) {
            rows.put(item.path("id").asText(), item.path("ticket_type_rule_version_no").asText(null));
        }
        return rows;
    }

    /** 首次发布经审批上架的跨域组合通票（第 1 版生效）。 */
    private String publishedComboPass(String tag) throws Exception {
        String pass = comboPass(tag);
        publish(pass, approver).andExpect(status().isOk());
        approve(approver, reviewOf(pass), "approved").andExpect(status().isOk());
        return pass;
    }

    private ResultActions reprice(String type, String tag, long priceCent, int discount) throws Exception {
        return send(operator, put("/api/v1/ticketing/ticket-types/" + type + "/pricing"), """
                {"idempotency_key": "r7-price-%s-%s", "reason": "改价", "combo_discount_rate": %d, "weekday_price_cent": %d,
                 "weekend_price_cent": %d, "holiday_price_cent": %d}
                """.formatted(tag, suffix, discount, priceCent, priceCent, priceCent));
    }

    private ResultActions withdraw(String type) throws Exception {
        return send(operator, post("/api/v1/ticketing/ticket-types/" + type + "/rule-versions/withdraw"), """
                {"idempotency_key": "r7-wd-%s", "reason": "改主意了，先不换版"}
                """.formatted(UUID.randomUUID()));
    }

    /** 客人端公开票种列表（匿名）里这张票的某个字段：在售的是哪一版（与下单计价同读现表）。 */
    private List<Object> onSale(String type, String field) throws Exception {
        String body = mockMvc.perform(get("/api/v1/customer/ticket-products").header("X-Terminal-Type", "miniapp"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.items[?(@.ticket_type_id == '" + type + "')]." + field);
    }

    private int minGiftCups(String type) throws Exception {
        String body = mockMvc.perform(get("/api/v1/ticketing/ticket-types/" + type + "/min-gift-cups")
                        .with(session(operator, "platform_admin", "org_1")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.<Integer>read(body, "$.data.min_gift_cups");
    }

    private Integer bindingCount(String type) {
        return jdbc.queryForObject("SELECT count(*) FROM ticket_type_entitlements WHERE ticket_type_id = ?", Integer.class, type);
    }

    private Long weekdayPrice(String type) {
        return jdbc.queryForObject("SELECT weekday_price_cent FROM ticket_types WHERE id = ?", Long.class, type);
    }

    /** 按组合配置写入顺序：酒吧入场 100 元、该吧零元平台出资赠酒、游乐入场 120 元；九折、平日价 199 元。 */
    private String comboPass(String tag) throws Exception {
        String pass = createTicketType(tag, 19900L, 90, null);
        bind(pass, template(bar, "酒吧入场", "entry", null), 10000L, null).andExpect(status().isOk());
        bind(pass, template(bar, "平台赠酒", "gift_drink", product), 0L, "platform_funded").andExpect(status().isOk());
        bind(pass, template(park, "游乐入场", "entry", null), 12000L, null).andExpect(status().isOk());
        return pass;
    }

    private String createTicketType(String tag, long priceCent, int discount, String barId) throws Exception {
        String body = send(operator, post("/api/v1/ticketing/ticket-types"), """
                {"idempotency_key": "r3-%s-%s", "ticket_name": "规则版本票%d%d", "base_calendar_price_mode": "weekday_weekend_holiday",
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
        String body = send(operator, post("/api/v1/ticketing/entitlement-templates"), """
                {"idempotency_key": "r3-tpl-%s", "name": "规则版本 · %s%d", "entitlement_type": "%s",
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
        return bind(type, template, componentPriceCent, costBearer, "r3-bind-" + UUID.randomUUID());
    }

    private ResultActions bind(String type, String template, long componentPriceCent, String costBearer, String key) throws Exception {
        return send(operator, post("/api/v1/ticketing/ticket-types/" + type + "/entitlements"), """
                {"idempotency_key": "%s", "entitlement_template_id": "%s", "quantity": 1,
                 "total_times": 1, "num_people": 1, "component_price_cent": %d, "cost_bearer": %s}
                """.formatted(key, template, componentPriceCent, costBearer == null ? "null" : "\"" + costBearer + "\""));
    }

    /** 后台发布：跨域组合通票带最终审批人。 */
    private ResultActions publish(String type, String finalApprover) throws Exception {
        return send(operator, post("/api/v1/ticketing/ticket-types/" + type + "/publish"), """
                {"idempotency_key": "r3-pub-%s", "action": "publish", "reason": "组合通票配好上架"%s}
                """.formatted(UUID.randomUUID(), finalApprover == null ? "" : ", \"final_approver_id\": \"" + finalApprover + "\""));
    }

    private String versions(String type) throws Exception {
        return mockMvc.perform(get("/api/v1/ticketing/ticket-types/" + type + "/rule-versions")
                        .with(session(operator, "platform_admin", "org_1")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private ResultActions approve(String who, String review, String decision) throws Exception {
        RequestPostProcessor session = operator.equals(who) ? session(operator, "platform_admin", "org_1") : session(who, "staff", project);
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decision + "\",\"comment\":\"已核对\",\"idempotency_key\":\"r3-ap-" + UUID.randomUUID() + "\"}"));
    }

    private String reviewOf(String type) {
        return jdbc.queryForObject("SELECT review_id FROM ticket_type_rule_versions WHERE ticket_type_id = ? AND status = 'pending_review'",
                String.class, type);
    }

    private Integer share(String versions, String domain, String field) {
        List<Integer> values = JsonPath.read(versions, "$.data.items[0].domain_allocations[?(@.domain == '" + domain + "')]." + field);
        assertThat(values).hasSize(1);
        return values.getFirst();
    }

    private String ticketStatus(String type) {
        return jdbc.queryForObject("SELECT status FROM ticket_types WHERE id = ?", String.class, type);
    }

    private ResultActions send(String who, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(session(who, "platform_admin", "org_1")).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void merchant(String id, String name, String type) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'active', ?, now(), now())", id, name + n, type, project);
    }

    private String role(String name, String dataScope, boolean allProjects, List<String> permissions) {
        String roleId = "role_r3_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, view_all_projects, created_at)"
                + " VALUES (?, ?, '跨域组合通票规则版本回归', ?, 'active', ?, now())", roleId, name, dataScope, allProjects);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        return roleId;
    }

    private void staff(String userId, String roleId) {
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', ?, NULL, 'active', now())", userId, userId, project);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbc.update("INSERT INTO staff_level_assignments (user_id, staff_level_id, organization_id) VALUES (?, 'lvl_m3_upper', ?)",
                userId, project);
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
