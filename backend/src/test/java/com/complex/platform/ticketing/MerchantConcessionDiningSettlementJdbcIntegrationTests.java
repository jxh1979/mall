package com.complex.platform.ticketing;

// 文件版本：V0.05

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.settlement.TicketDistributionSettlementService;
import com.complex.platform.support.DailyCloseFixture;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 S 段 156-R1（BSM:751「商户让利的零元免费权益不结算；域主管公司出资的免费权益按锁定内部结算价核销后结算，未核销不结算。」；
 * BSM:1689「商户让利的零元权益不结算，域主管公司出资的零元权益按锁定内部结算价核销后结算。」）：
 * 组合通票餐饮行成本承担方为「商户让利型」时，核销结算价 0、结算事实 0、月结该餐厅专属直付不含这笔；
 * 「平台出资型」照旧按目录价直付；商户让利行不挂目录条目也能核销（不报 ENTITLEMENT_PRICE_UNRESOLVED）。
 * 真实接口与授权链：按 TicketingPoliciesPage.createAndBind 前端蛇形键建模板、绑票种，餐厅员工在本店设备核销。
 * 不开测试事务（结算事实在核销事务提交后写入）。
 * 第十八批 X 段第 197 条：票内餐饮券须本场（综合体营业日）同人份已检票入场，且每场只能核一份；各用例的餐饮挂父票 1 号人份、先核同人份入场。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class MerchantConcessionDiningSettlementJdbcIntegrationTests {

    private static final long CATALOG_PRICE_CENT = 3000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TicketingRepository ticketingRepository;

    @Autowired
    private TicketDistributionSettlementService settlementService;

    @Autowired
    private BusinessDayResolver businessDays;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_c156_" + suffix;
    private final String restaurant = "rest_c156_" + suffix;
    private final String admin = "usr_c156_admin_" + suffix;
    private final String staff = "usr_c156_staff_" + suffix;
    private final String device = "dev_c156_" + suffix;
    private final String typeId = "tt_c156_" + suffix;
    private final String catalogCode = "sku_c156_" + suffix;
    private final List<String> templateIds = new ArrayList<>();
    private final List<String> codes = new ArrayList<>();

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '让利结算综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'catering', 'active', ?, now(), now())", restaurant, "让利餐厅" + suffix, project);
        jdbc.update("""
                INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name,
                    price_cent, status, is_draft, updated_by, created_at, updated_at)
                VALUES (?, ?, ?, 'dining', '套餐', ?, 'active', FALSE, 'seed_c156', now(), now())
                """, catalogCode, catalogCode, restaurant, CATALOG_PRICE_CENT);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)"
                + " VALUES (?, '组合通票', 'fixed', FALSE, 'draft', now(), now())", typeId);
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, now(), now())", device, device, device, restaurant);
        user(admin, "platform_admin", "org_1", null, "票务策略配置" + Long.parseLong(suffix, 16),
                List.of("ticket.entitlement.manage", "ticket.product.manage"));
        user(staff, "staff", project, restaurant, "餐厅核销员" + Long.parseLong(suffix, 16), List.of("verification.execute"));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM amusement_compensation_fulfillments WHERE project_id = ?", project);
        jdbc.update("DELETE FROM amusement_fault_compensation_items WHERE project_id = ?", project);
        jdbc.update("DELETE FROM amusement_fault_compensation_batches WHERE project_id = ?", project);
        jdbc.update("DELETE FROM amusement_park_wide_faults WHERE project_id = ?", project);
        jdbc.update("DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)", project);
        jdbc.update("DELETE FROM settlement_bills WHERE project_id = ?", project);
        for (String code : codes) {
            com.complex.platform.support.AppendOnlyTestBypass.update(
                    jdbc, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
            jdbc.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", "ent_" + code);
            jdbc.update("DELETE FROM issued_entitlements WHERE id = ?", "ent_" + code);
            jdbc.update("DELETE FROM ticket_instances WHERE order_id = ?", "order_" + code);
            jdbc.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + code);
        }
        jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", typeId);
        for (String id : templateIds) {
            jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM internal_price_catalog WHERE item_code = ?", catalogCode);
        jdbc.update("DELETE FROM ticket_types WHERE id = ?", typeId);
        jdbc.update("DELETE FROM terminal_devices WHERE id = ?", device);
        jdbc.update("DELETE FROM turnover_rent_rules WHERE merchant_id = ?", restaurant);
    }

    @Test
    void merchantConcessionDiningSettlesZeroWhilePlatformFundedPaysCatalogPrice() throws Exception {
        String concession = issue(createAndBind("让利餐厅 · 餐饮（商户让利）", catalogCode, "merchant_concession"), "mc");
        String funded = issue(createAndBind("让利餐厅 · 餐饮（平台出资）", catalogCode, "platform_funded"), "pf");

        verify(concession);
        verify(funded);

        assertThat(settlementPrice(concession)).isEqualTo(0L);
        assertThat(settledAmount(concession)).isEqualTo(0L);
        assertThat(settlementPrice(funded)).isEqualTo(CATALOG_PRICE_CENT);
        assertThat(settledAmount(funded)).isEqualTo(CATALOG_PRICE_CENT);

        TestSecurityActors.asPlatformAdmin();
        YearMonth month = YearMonth.from(businessDays.businessDate(project, null, Instant.now()));
        // 第十八批 V 段 159-R2：通票月度分账只结两类数据已标记为最终的月份，先把该综合体当月整段标齐。
        DailyCloseFixture.markRangeFinal(jdbc, "project", project, month.atDay(1), month.atEndOfMonth());
        settlementService.settleMonth(project, month);
        // 该餐厅专属直付只含平台出资那一笔；商户让利那笔不产生应付。
        assertThat(jdbc.queryForObject("""
                SELECT si.final_amount_cent FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method = 'exclusive_full'
                """, Long.class, project, restaurant)).isEqualTo(CATALOG_PRICE_CENT);
    }

    @Test
    void merchantConcessionDiningWithoutCatalogItemStillVerifiesAndSettlesZero() throws Exception {
        String concession = issue(createAndBind("让利餐厅 · 餐饮（商户让利、未配目录）", null, "merchant_concession"), "nc");

        verify(concession);

        assertThat(settlementPrice(concession)).isEqualTo(0L);
        assertThat(settledAmount(concession)).isEqualTo(0L);
    }

    @Test
    void giftBindingWithoutCostBearerRejectedAndMerchantConcessionStored() throws Exception {
        // 170-R2（契约 TicketTypeEntitlementRequest.cost_bearer 按代码声明）：赠送类权益不带 cost_bearer 拒绝，带了照存。
        String templateId = template("让利餐厅 · 餐饮（成本承担方）", catalogCode, "cb");
        bind(templateId, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("TICKET_COST_BEARER_REQUIRED"));
        bind(templateId, "merchant_concession").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT cost_bearer FROM ticket_type_entitlements WHERE ticket_type_id = ? AND entitlement_template_id = ?",
                String.class, typeId, templateId)).isEqualTo("merchant_concession");
    }

    @Test
    void genericTemplateFormBodyStoresChosenProviderAndMissingMerchantRejectedInChinese() throws Exception {
        // 第十八批 W 段 177-R1（BSM:1754「每项权益都必须绑定实际提供方和审批版本，避免悬空账。」）：按 TicketingPoliciesPage
        // 通用「新建模板」的请求体蛇形键建入园模板——带 merchant_id 落库为所选提供方；不带则 400，出网报错为中文。
        String body = mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "w177-tpl-%s", "name": "入园1次", "entitlement_type": "entry",
                                 "default_total_times": 1, "default_value_cent": null, "status": "active", "merchant_id": "%s"}
                                """.formatted(UUID.randomUUID(), restaurant)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String templateId = new ObjectMapper().readTree(body).path("data").path("id").asText();
        templateIds.add(templateId);
        assertThat(jdbc.queryForObject("SELECT merchant_id FROM entitlement_templates WHERE id = ?", String.class, templateId))
                .isEqualTo(restaurant);

        mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "w177-tpl-%s", "name": "入园1次", "entitlement_type": "entry",
                                 "default_total_times": 1, "default_value_cent": null, "status": "active"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商户未填写"));
    }

    @Test
    void paidDiningExclusiveSettlementCountsInRestaurantTurnoverRentBase() throws Exception {
        // 第十八批 X 段第 175 条 175-R1（ENUM:257「未计收服务费的商户（餐饮、零售等），其通票或券核销划归本商户并已完成结算的部分计入」；
        // BSM:1347「未计收服务费的商户其已完成结算的部分计入」）：平台出资餐饮行核销 → 月结专属直付 → 人工过账挂通票分账应付 → 财务付讫 →
        // 出该餐厅营业额抽成账单，基数含这笔付款：3000 × 10% = 300。过账、付款、出账走真实入口与授权链、蛇形键。
        InternalPayDayFixtures.seed(jdbc);
        String finance = "usr_c175_fin_" + suffix;
        user(finance, "platform_admin", "org_1", null, "通票抽成财务" + Long.parseLong(suffix, 16),
                List.of("settlement.bill.post", "finance.payment.register", "finance.turnover_rent.bill"));
        String rule = "trr_c175_" + suffix;
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '通票餐饮营业额抽成规则', 1000, 0, DATE '2020-01-01', NULL, 'active', FALSE)
                """, rule, "contract_c175_" + suffix, restaurant);
        String funded = issue(createAndBind("让利餐厅 · 餐饮（平台出资）", catalogCode, "platform_funded"), "tr");
        verify(funded);

        TestSecurityActors.asPlatformAdmin();
        Instant paidAt = Instant.now();
        YearMonth month = YearMonth.from(businessDays.businessDate(project, null, paidAt));
        DailyCloseFixture.markRangeFinal(jdbc, "project", project, month.atDay(1), month.atEndOfMonth());
        String bill = settlementService.settleMonth(project, month);
        mockMvc.perform(post("/api/v1/settlement/bills/" + bill + "/post").with(session(finance, "platform_admin", "org_1", null)))
                .andExpect(status().isOk());
        String payable = jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE direction = 'payable' AND source_type = 'ticket_distribution'"
                + " AND counterparty_id = ?", String.class, restaurant);
        mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + payable + "/payments").with(session(finance, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c175-pay-%s", "amount_cent": %d, "payment_method": "bank_transfer",
                                 "payment_time": "%s"}
                                """.formatted(suffix, CATALOG_PRICE_CENT, paidAt)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/finance/turnover-rent/bills").with(session(finance, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c175-bill-%s", "rule_id": "%s", "period_start": "%s", "period_end": "%s",
                                 "due_date": "%s"}
                                """.formatted(suffix, rule, month.atDay(1), month.atEndOfMonth(), month.plusMonths(1).atDay(10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(300));
    }

    // 第十八批 X 段第 197 条 197-R1（BSM:800「该票所含的各项权益（各酒吧入场、酒水赠饮、游乐、餐饮券、零售券等），只能在完成检票环节所在的那一个
    // 场次内核销」「该窗口内票面附带的各项权益……按完整一份配足、完整可用，但仅限当场消耗……后续场次的附带权益也不得提前到当前场次使用。」；
    // BSM:780 营业日按综合体）：三次票（入场 3 次、餐饮 3 次）未检票先核餐饮 → 409 并落 rejected、不扣次、无结算行（修复前 200 即红）；
    // 检票后核一份 200，同营业日再核 → 409；挪到综合体本营业日起点之前（模拟下一场）再检票、再核 200。
    @Test
    void ticketDiningNeedsThisSessionCheckInAndOneShareEachSession() throws Exception {
        String code = issue(createAndBind("让利餐厅 · 餐饮（三次票）" + Long.parseLong(suffix, 16), catalogCode, "platform_funded"),
                "x197", 3, 3, null);
        scan(code).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_SESSION_NOT_CHECKED_IN"));
        assertThat(jdbc.queryForObject("SELECT status || '/' || result_reason FROM ticket_verifications WHERE entitlement_id = ?",
                String.class, "ent_" + code)).isEqualTo("rejected/entitlement_session_not_checked_in");
        assertThat(usedTimes(code)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM settlement_transactions WHERE entitlement_id = ?",
                Integer.class, "ent_" + code)).isZero();

        verify(code);
        scan(code).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_SESSION_SHARE_USED_UP"));
        assertThat(usedTimes(code)).isEqualTo(1);

        Instant start = businessDays.start(project, null, businessDays.businessDate(project, null, Instant.now()));
        jdbc.update("UPDATE ticket_verifications SET created_at = ? WHERE entitlement_id IN (?, ?)",
                java.sql.Timestamp.from(start.minusSeconds(60)), "ent_" + code + "-in", "ent_" + code);
        verify(code);
        assertThat(usedTimes(code)).isEqualTo(2);
    }

    // 197-R1 回归：单次票（入场 1 次、餐饮 2 次）本场两份都可核；全园故障补偿补发的餐饮行（2 次）取它的全部次数，挂在三次票人份下本场也两份都可核。
    @Test
    void singleUseTicketAndCompensationReissueDiningUseAllTimesInTheSession() throws Exception {
        InternalPayDayFixtures.seed(jdbc);
        String templateId = createAndBind("让利餐厅 · 餐饮（回归）" + Long.parseLong(suffix, 16), catalogCode, "platform_funded");
        String single = issue(templateId, "x197s", 1, 2, null);
        verify(single);
        scan(single).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        assertThat(usedTimes(single)).isEqualTo(2);

        String item = "afci_c197_" + suffix;
        String reissued = issue(templateId, "x197c", 3, 2, item);
        compensationItem(item, reissued);
        verify(reissued);
        scan(reissued).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        assertThat(usedTimes(reissued)).isEqualTo(2);
    }

    // 18X 复核修复（BSM:800「只能在完成检票环节所在的那一个场次内核销」；BSM:784「游乐（指定项目/次数/场次）」，游乐次数不是检票次数）：
    // 单次票（入场 1 次、餐饮 2 次）同人份另有一行游乐 2 次，本场餐饮两份都可核（修复前「一份」的入场次数取到游乐 2 次，第二份 409 即红）。
    @Test
    void singleUseTicketDiningShareNotDividedByAmusementTimes() throws Exception {
        String code = issue(createAndBind("让利餐厅 · 餐饮（含游乐）" + Long.parseLong(suffix, 16), catalogCode, "platform_funded"),
                "x197a", 1, 2, null);
        String ride = code + "-ride";
        codes.add(0, ride);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " created_at, updated_at) VALUES (?, ?, 'amusement', 2, 0, 'active', now(), now())",
                "et_" + ride, "游乐次数" + Long.parseLong(suffix, 16));
        templateIds.add("et_" + ride);
        Instant now = Instant.now();
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + ride, ride, typeId, "order_" + code, "et_" + ride, "amusement",
                restaurant, null, "point_" + suffix, 2, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                null, null, null, null, "ti_" + code, 1, null, null));
        verify(code);
        scan(code).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        assertThat(usedTimes(code)).isEqualTo(2);
    }

    /** 按 TicketingPoliciesPage.createAndBind 餐饮行的请求形状建模板并绑到通票（纯赠送，票面原价 0）。 */
    private String createAndBind(String name, String catalog, String costBearer) throws Exception {
        String templateId = template(name, catalog, costBearer);
        bind(templateId, costBearer).andExpect(status().isOk());
        return templateId;
    }

    private String template(String name, String catalog, String tag) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c156-tpl-%s-%s", "name": "%s", "entitlement_type": "dining",
                                 "default_total_times": 1, "default_value_cent": 0, "status": "active", "merchant_id": "%s",
                                 "catalog_item_code": %s, "exclusive_merchant_id": "%s", "bar_item_ref": null}
                                """.formatted(tag, UUID.randomUUID(), name, restaurant,
                                catalog == null ? "null" : "\"" + catalog + "\"", restaurant)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String templateId = new ObjectMapper().readTree(body).path("data").path("id").asText();
        templateIds.add(templateId);
        return templateId;
    }

    /** costBearer 为 null 时请求体不带 cost_bearer。 */
    private ResultActions bind(String templateId, String costBearer) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + typeId + "/entitlements").with(session(admin, "platform_admin", "org_1", null))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c156-bind-%s", "entitlement_template_id": "%s", "quantity": 1,
                         "total_times": 1, "num_people": 1, "component_price_cent": 0%s}
                        """.formatted(UUID.randomUUID(), templateId,
                        costBearer == null ? "" : ", \"cost_bearer\": \"" + costBearer + "\"")));
    }

    /** 售出该通票后发到客人手上的餐饮权益（专属该餐厅、纯赠送）；单次票，同人份带一项入场。 */
    private String issue(String templateId, String tag) {
        return issue(templateId, tag, 1, 1, null);
    }

    /**
     * 197-R1：订单锁定本综合体，父票 1 号人份下发入场（admission，entryTimes 次；归属本店以过餐厅员工商户范围，码＝餐饮码加「-in」）与
     * 专属餐饮（diningTimes 次；compensationItemId 非空＝全园故障补偿补发行）。
     */
    private String issue(String templateId, String tag, int entryTimes, int diningTimes, String compensationItemId) {
        String code = "c156-" + tag + "-" + suffix;
        String entry = code + "-in";
        String parent = "ti_" + code;
        codes.add(entry);
        codes.add(code);
        Instant now = Instant.now();
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.now(), "web", null, 0L, null, 0L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null, project));
        jdbc.update("INSERT INTO ticket_instances (id, ticket_no, order_id, ticket_type_id, seq_in_order, num_people, total_times,"
                + " status, created_at, updated_at) VALUES (?, ?, ?, ?, 1, 1, ?, 'active', now(), now())",
                parent, "T-" + code, "order_" + code, typeId, entryTimes);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " created_at, updated_at) VALUES (?, ?, 'admission', 1, 0, 'active', now(), now())",
                "et_" + entry, "本场入场" + Long.parseLong(suffix, 16));
        templateIds.add("et_" + entry);
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + entry, entry, typeId, "order_" + code, "et_" + entry, "admission",
                restaurant, null, "point_" + suffix, entryTimes, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                null, null, null, null, parent, 1, null, null));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "dining",
                restaurant, null, "point_" + suffix, diningTimes, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                "virtual", null, null, restaurant, parent, 1, null, null, compensationItemId));
        return code;
    }

    /** 197-R1 回归：全园故障（已解除）、已发放的补偿批次与完整补发记录（原受影响权益取同人份的入场）。 */
    private void compensationItem(String item, String code) {
        String fault = "pwf_c197_" + suffix;
        String batch = "afcb_c197_" + suffix;
        jdbc.update("INSERT INTO amusement_park_wide_faults (id, project_id, status, reason, initiated_by, initiated_role, initiated_at)"
                + " VALUES (?, ?, 'lifted', '入口系统故障', 'seed_c197', 'amusement_head', now())", fault, project);
        jdbc.update("INSERT INTO amusement_fault_compensation_batches (id, fault_id, project_id, status, reason, bearer_company_id,"
                + " validity_days, window_from, window_to, initiated_by, initiated_role, initiated_at)"
                + " VALUES (?, ?, ?, 'issued', '入口系统故障', 'le_c197', 365, now(), now(), 'seed_c197', 'amusement_head', now())",
                batch, fault, project);
        jdbc.update("INSERT INTO amusement_fault_compensation_items (id, batch_id, fault_id, project_id, entitlement_id, kind, status)"
                + " VALUES (?, ?, ?, ?, ?, 'full_reissue', 'issued')", item, batch, fault, project, "ent_" + code + "-in");
    }

    /** 餐厅员工在本店核销设备上核销；197-R1 起先核同票同人份的入场（本场检票），再核餐饮。 */
    private void verify(String code) throws Exception {
        scan(code + "-in").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        scan(code).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
    }

    private ResultActions scan(String code) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/verifications").with(session(staff, "staff", project, restaurant))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c156-verify-%s", "credential_type": "qr", "credential_payload": "%s",
                         "device_id": "%s", "point_id": "point_%s", "business_context": {}}
                        """.formatted(UUID.randomUUID(), code, device, suffix)));
    }

    private int usedTimes(String code) {
        return jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, "ent_" + code);
    }

    private Long settlementPrice(String code) {
        return jdbc.queryForObject("SELECT settlement_price_cent FROM ticket_verifications WHERE entitlement_id = ? AND status = 'success'",
                Long.class, "ent_" + code);
    }

    private Long settledAmount(String code) {
        return jdbc.queryForObject("SELECT verified_amount_cent FROM settlement_transactions WHERE entitlement_id = ?",
                Long.class, "ent_" + code);
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
