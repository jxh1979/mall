package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 BP 段第 354 条：票种写入口按商户与经营域收口。
 *
 * <p>354-R1 依据：ENUM:32「单一酒吧门票由对应酒吧商户定价、销售并确认已核销部分的收入」「酒吧通票由娱乐平台公司负责」；PGM:429「自行定价、销售并确认单一酒吧门票
 * 已核销部分的收入」；BSM:704「独立酒吧商户负责自己的经营、人员、设备、单一酒吧门票和店内收入」；BSM:707「④看不到别的商户和别的项目的数据」；BSM:588
 * 「平台管理员……拥有系统里全部日常功能权限」；BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」。
 *
 * <p>354-R2 依据：ENUM:32「酒吧通票由娱乐平台公司负责；纯游乐票和纯游乐通票由游乐公司负责」；PGM:419「单一经营域的定价、售票、内部结算和分配规则由对应域主管公司负责」；
 * BSM:749「零元免费权益不参与域间分配」。
 *
 * <p>真实接口与授权链（蛇形键、后台 Web 终端）；不开测试事务，@AfterEach 按本例数据清理。收入归属用 V355 平台默认（酒吧通票归娱乐平台公司、游乐票归游乐公司），
 * 人员所属公司取账号机构。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class TicketTypeWriteScope354Tests {

    private static final String ENT = "be_ent_platform";
    private static final String AMUSEMENT = "be_amusement";
    private static final String SCOPE = "TICKET_TYPE_SCOPE_FORBIDDEN";
    private static final String[] PERMISSIONS = {"ticket.product.manage", "ticket.product.view", "ticket.price_variant.manage",
            "ticket.dynamic_pricing.manage", "ticket.dynamic_pricing.publish", "ticket.entitlement.manage"};

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "prj_c354_" + s;
    private final String barA = "bar_c354a_" + s;
    private final String barB = "bar_c354b_" + s;
    private final String park = "amu_c354_" + s;
    private final List<String> ticketTypes = new ArrayList<>();
    /** 酒吧甲的商户账号。 */
    private RequestPostProcessor clerkA;
    /** 娱乐平台公司人员（不挂商户）。 */
    private RequestPostProcessor ent;
    /** 游乐公司人员（不挂商户）。 */
    private RequestPostProcessor amu;
    /** 不挂商户、不是平台管理员的普通员工（账号机构为本项目）。 */
    private RequestPostProcessor plain;
    private RequestPostProcessor admin;
    private Integer qrInterval;
    private int seq;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', now(), now())", project, "票种范围综合体" + s);
        merchant(barA, "票种范围酒吧甲", "bar");
        merchant(barB, "票种范围酒吧乙", "bar");
        merchant(park, "票种范围乐园", "amusement");
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        clerkA = fixture.staff("usr_c354_clerk_" + s, "staff", project, barA, PERMISSIONS);
        ent = fixture.staff("usr_c354_ent_" + s, "staff", ENT, null, PERMISSIONS);
        amu = fixture.staff("usr_c354_amu_" + s, "staff", AMUSEMENT, null, PERMISSIONS);
        plain = fixture.staff("usr_c354_plain_" + s, "staff", project, null, PERMISSIONS);
        admin = fixture.staff("usr_c354_admin_" + s, "platform_admin", "org_1", null, PERMISSIONS);
        qrInterval = currentQrInterval();
    }

    @AfterEach
    void cleanup() {
        for (String type : ticketTypes) {
            jdbc.update("DELETE FROM ticket_price_rules WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_dynamic_pricing_rules WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_rule_versions WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_gift_floors WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_types WHERE id = ?", type);
        }
        // 第三方券配置随模板级联删除。
        jdbc.update("DELETE FROM entitlement_templates WHERE merchant_id IN (?, ?, ?)", barA, barB, park);
        jdbc.update("DELETE FROM staff_dispatch_records WHERE merchant_id = ?", barA);
        jdbc.update("DELETE FROM merchants WHERE id IN (?, ?, ?)", barA, barB, park);
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
        if (qrInterval != null) {
            jdbc.update("UPDATE ticket_qr_refresh_config SET refresh_interval_seconds = ? WHERE id = 'default'", qrInterval);
        }
    }

    /**
     * 354-R1①②⑨：酒吧甲的商户账号只给本店建单一酒吧门票、只改本店的票——建别家的、建通票、改别家的一律 403，不落库、价不变；
     * 平台管理员与最高权限办理任何票种照旧。
     */
    @Test
    void merchantAccountCreatesAndRepricesOnlyItsOwnSingleBarTicket() throws Exception {
        create(clerkA, "a1", barA).andExpect(status().isOk());
        denied(create(clerkA, "a2", barB));
        denied(create(clerkA, "a3", null));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_types WHERE ticket_name IN (?, ?)", Integer.class,
                name("a2"), name("a3"))).isZero();

        String mine = singleBarTicket("a4", barA);
        String theirs = singleBarTicket("a5", barB);
        denied(reprice(clerkA, theirs, 4500, 90, "a5k"));
        assertThat(weekdayPrice(theirs)).isEqualTo(5000L);
        reprice(clerkA, mine, 5000, "a4k").andExpect(status().isOk());

        reprice(admin, theirs, 5000, "a5a").andExpect(status().isOk());
        reprice(master(), theirs, 5000, "a5m").andExpect(status().isOk());
    }

    /** 354-R1③：商户账号不能改、发布酒吧通票，也不能给它加权益。 */
    @Test
    void merchantAccountCannotHandleAPass() throws Exception {
        String pass = publishedPass("b1", barA, 5000);
        denied(reprice(clerkA, pass, 5000, "b1p"));
        denied(send(clerkA, post("/api/v1/ticketing/ticket-types/" + pass + "/publish"), """
                {"idempotency_key": "c354-pub-%s", "reason": "上架"}
                """.formatted(UUID.randomUUID())));
        denied(bind(clerkA, pass, template(barA, "entry"), 0L));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_type_entitlements WHERE ticket_type_id = ?", Integer.class, pass))
                .isEqualTo(1);
    }

    /** 354-R1④：派生价、动态价只能给本店的票建，别家的票建派生价与动态价、发布别家的动态价、存档别家的派生价一律 403。 */
    @Test
    void merchantAccountPriceRulesAndDynamicRulesOnlyOnItsOwnTicket() throws Exception {
        String mine = singleBarTicket("c1", barA);
        String theirs = singleBarTicket("c2", barB);
        denied(priceRule(clerkA, theirs));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_price_rules WHERE ticket_type_id = ?", Integer.class, theirs)).isZero();
        priceRule(clerkA, mine).andExpect(status().isOk());
        denied(dynamicRule(clerkA, theirs));
        dynamicRule(clerkA, mine).andExpect(status().isOk());

        String theirRule = id(dynamicRule(admin, theirs).andExpect(status().isOk()));
        denied(send(clerkA, post("/api/v1/ticketing/dynamic-pricing-rules/" + theirRule + "/publish"), """
                {"idempotency_key": "c354-dp-%s", "reason": "旺季调价"}
                """.formatted(UUID.randomUUID())));
        assertThat(jdbc.queryForObject("SELECT status FROM ticket_dynamic_pricing_rules WHERE id = ?", String.class, theirRule))
                .isEqualTo("draft");
        String theirPrice = id(priceRule(admin, theirs).andExpect(status().isOk()));
        denied(send(clerkA, post("/api/v1/ticketing/price-rules/" + theirPrice + "/archive"), """
                {"idempotency_key": "c354-ar-%s", "reason": "这档价先不卖"}
                """.formatted(UUID.randomUUID())));
    }

    /** 354-R1⑤⑪：权益模板只能建本店提供的；同页「第三方券配置」只能配本店的模板。 */
    @Test
    void merchantAccountTemplatesAndPartnerVoucherConfigsOnlyForItsOwnBar() throws Exception {
        denied(templateRequest(clerkA, barB, "entry", "91"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entitlement_templates WHERE name = ?", Integer.class, "票种范围权益91" + s))
                .isZero();
        String mine = id(templateRequest(clerkA, barA, "entry", "92").andExpect(status().isOk()));

        String theirs = template(barB, "entry");
        denied(partnerVoucher(clerkA, theirs));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM partner_voucher_configs WHERE entitlement_template_id = ?", Integer.class,
                theirs)).isZero();
        partnerVoucher(clerkA, mine).andExpect(status().isOk());
    }

    /** 354-R1⑥（BSM:741「二维码按后台统一设置的刷新间隔自动刷新」）：全场统一的二维码刷新间隔，商户账号不能改。 */
    @Test
    void merchantAccountCannotChangeTheParkWideQrRefreshInterval() throws Exception {
        int other = qrInterval != null && qrInterval == 20 ? 25 : 20;
        denied(send(clerkA, put("/api/v1/ticketing/qr-refresh-config"), """
                {"idempotency_key": "c354-qr-%s", "refresh_interval_seconds": %d}
                """.formatted(UUID.randomUUID(), other)));
        assertThat(currentQrInterval()).isEqualTo(qrInterval);
    }

    /** 354-R1⑦⑩：不挂商户的普通员工不能改单一酒吧门票；派驻在甲酒吧期内的人员可以改甲的票。 */
    @Test
    void staffWithoutMerchantCannotPriceASingleBarTicketButStaffDispatchedThereCan() throws Exception {
        String mine = singleBarTicket("e1", barA);
        denied(reprice(plain, mine, 5000, "e1p"));

        String dispatchedId = "usr_c354_disp_" + s;
        RequestPostProcessor dispatched = new StaffRequestFixture(jdbc).staff(dispatchedId, "staff", project, null, PERMISSIONS);
        jdbc.update("INSERT INTO staff_dispatch_records (id, user_id, company_id, merchant_id, project_id, start_date, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, DATE '2024-11-01', 'active', now())", "dsp_c354_" + s, dispatchedId, ENT, barA, project);
        reprice(dispatched, mine, 5000, "e1d").andExpect(status().isOk());
    }

    /** 354-R1⑧：范围闸在幂等之前——平台管理员用某提交编号改了乙的票，甲的商户账号拿同一编号、同一正文再发，按范围拒绝（不是提交编号已被使用）。 */
    @Test
    void sameKeyFromAnotherMerchantIsRejectedByScopeBeforeIdempotency() throws Exception {
        String theirs = singleBarTicket("f1", barB);
        String key = "same";
        reprice(admin, theirs, 4500, 90, key).andExpect(status().isOk());
        denied(reprice(clerkA, theirs, 4500, 90, key));
    }

    /** 354-R2①②⑥：已上架酒吧通票只有娱乐平台公司的人能改价，纯游乐通票只有游乐公司的人能改价；平台管理员、最高权限不受限。 */
    @Test
    void passIsHandledOnlyByItsDomainCompany() throws Exception {
        String barPass = publishedPass("g1", barA, 5000);
        denied(reprice(amu, barPass, 4500, 90, "g1a"));
        assertThat(weekdayPrice(barPass)).isEqualTo(5000L);
        reprice(ent, barPass, 5000, "g1e").andExpect(status().isOk());

        String parkPass = publishedPass("g2", park, 8000);
        denied(reprice(ent, parkPass, 8000, "g2e"));
        reprice(amu, parkPass, 8000, "g2a").andExpect(status().isOk());

        reprice(admin, barPass, 5000, "g1x").andExpect(status().isOk());
        reprice(master(), parkPass, 8000, "g2m").andExpect(status().isOk());
    }

    /**
     * 354-R2③④：游乐公司人员可以起草空票、给未上架票配酒吧有价入场（组成不设域闸），但这张酒吧域的票他不能发布、不能改价，票仍是草稿，娱乐平台公司的人可以改价；
     * 再配上游乐有价入场即成跨域组合通票，不设域闸，他可以改价。
     */
    @Test
    void otherDomainDrafterComposesButCannotPublishOrPriceASingleDomainPass() throws Exception {
        String pass = id(create(amu, "h1", null).andExpect(status().isOk()));
        bind(amu, pass, template(barA, "entry"), 5000L).andExpect(status().isOk());
        denied(send(amu, post("/api/v1/ticketing/ticket-types/" + pass + "/publish"), """
                {"idempotency_key": "c354-pub-%s", "reason": "上架"}
                """.formatted(UUID.randomUUID())));
        denied(reprice(amu, pass, 5000, "h1a"));
        assertThat(jdbc.queryForObject("SELECT status FROM ticket_types WHERE id = ?", String.class, pass)).isEqualTo("draft");
        reprice(ent, pass, 5000, "h1e").andExpect(status().isOk());

        bind(amu, pass, template(park, "entry"), 8000L).andExpect(status().isOk());
        reprice(amu, pass, 13000, "h1c").andExpect(status().isOk());
    }

    /** 354-R2⑤⑦：别域人员给已上架酒吧通票加零元权益也 403；没有任何有价权益的草稿票不设域闸。 */
    @Test
    void publishedPassRejectsZeroPricedEntitlementFromOtherDomainButEmptyDraftIsOpen() throws Exception {
        String barPass = publishedPass("i1", barA, 5000);
        denied(bind(amu, barPass, template(barA, "entry"), 0L));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_type_entitlements WHERE ticket_type_id = ?", Integer.class, barPass))
                .isEqualTo(1);

        String empty = id(create(admin, "i2", null, 0).andExpect(status().isOk()));
        reprice(plain, empty, 0, "i2p").andExpect(status().isOk());
    }

    // ─────────────────────────── 夹具 ───────────────────────────

    private static void denied(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value(SCOPE));
    }

    private String name(String tag) {
        return "票种范围票" + tag + s;
    }

    private ResultActions create(RequestPostProcessor who, String tag, String barId) throws Exception {
        return create(who, tag, barId, 5000);
    }

    /** 后台建票（草稿），成功的记下以便清理。 */
    private ResultActions create(RequestPostProcessor who, String tag, String barId, long priceCent) throws Exception {
        ResultActions result = send(who, post("/api/v1/ticketing/ticket-types"), """
                {"idempotency_key": "c354-tt-%s-%s", "ticket_name": "%s", "base_calendar_price_mode": "weekday_weekend_holiday",
                 "weekday_price_cent": %d, "weekend_price_cent": %d, "holiday_price_cent": %d, "stock_required": false, "bar_id": %s}
                """.formatted(tag, s, name(tag), priceCent, priceCent, priceCent, quoted(barId)));
        MvcResult done = result.andReturn();
        if (done.getResponse().getStatus() == 200) {
            ticketTypes.add(JsonPath.read(done.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id"));
        }
        return result;
    }

    /** 平台管理员建好的单一酒吧门票草稿：本店有价入场 50 元、售价 50 元。 */
    private String singleBarTicket(String tag, String bar) throws Exception {
        String ticket = id(create(admin, tag, bar).andExpect(status().isOk()));
        bind(admin, ticket, template(bar, "entry"), 5000L).andExpect(status().isOk());
        return ticket;
    }

    /** 已上架通票：平台管理员起草、配好该商户有价入场（售价与之相同）后置为已上架。 */
    private String publishedPass(String tag, String provider, long priceCent) throws Exception {
        String pass = id(create(admin, tag, null, priceCent).andExpect(status().isOk()));
        bind(admin, pass, template(provider, "entry"), priceCent).andExpect(status().isOk());
        jdbc.update("UPDATE ticket_types SET status = 'published' WHERE id = ?", pass);
        return pass;
    }

    /** 平台管理员建的权益模板。 */
    private String template(String merchant, String type) throws Exception {
        return id(templateRequest(admin, merchant, type, String.valueOf(++seq)).andExpect(status().isOk()));
    }

    private ResultActions templateRequest(RequestPostProcessor who, String merchant, String type, String tag) throws Exception {
        return send(who, post("/api/v1/ticketing/entitlement-templates"), """
                {"idempotency_key": "c354-tpl-%s", "name": "票种范围权益%s%s", "entitlement_type": "%s",
                 "default_total_times": 1, "default_value_cent": 0, "status": "active", "merchant_id": "%s",
                 "catalog_item_code": null, "exclusive_merchant_id": null, "bar_item_ref": null}
                """.formatted(UUID.randomUUID(), tag, s, type, merchant));
    }

    private ResultActions bind(RequestPostProcessor who, String ticket, String template, long componentPriceCent) throws Exception {
        return send(who, post("/api/v1/ticketing/ticket-types/" + ticket + "/entitlements"), """
                {"idempotency_key": "c354-bind-%s", "entitlement_template_id": "%s", "quantity": 1,
                 "total_times": 1, "num_people": 1, "component_price_cent": %d, "cost_bearer": null}
                """.formatted(UUID.randomUUID(), template, componentPriceCent));
    }

    /** 改三档售价（不打折），key 为提交编号后缀。 */
    private ResultActions reprice(RequestPostProcessor who, String ticket, long priceCent, String key) throws Exception {
        return reprice(who, ticket, priceCent, 100, key);
    }

    /** 改三档售价与折扣（各业态价格合计×折扣＝售价，过凑整误差闸），key 为提交编号后缀。 */
    private ResultActions reprice(RequestPostProcessor who, String ticket, long priceCent, int discount, String key) throws Exception {
        return send(who, put("/api/v1/ticketing/ticket-types/" + ticket + "/pricing"), """
                {"idempotency_key": "c354-price-%s-%s", "reason": "调价", "combo_discount_rate": %d, "weekday_price_cent": %d,
                 "weekend_price_cent": %d, "holiday_price_cent": %d}
                """.formatted(key, s, discount, priceCent, priceCent, priceCent));
    }

    private ResultActions priceRule(RequestPostProcessor who, String ticket) throws Exception {
        return send(who, post("/api/v1/ticketing/price-rules"), """
                {"idempotency_key": "c354-pr-%s", "ticket_type_id": "%s", "variant_type": "member", "price_mode": "derive_from_calendar_price",
                 "discount_rate": 90}
                """.formatted(UUID.randomUUID(), ticket));
    }

    private ResultActions dynamicRule(RequestPostProcessor who, String ticket) throws Exception {
        return send(who, post("/api/v1/ticketing/dynamic-pricing-rules"), """
                {"idempotency_key": "c354-dr-%s", "ticket_type_id": "%s", "rule_name": "旺季加价", "priority": 10,
                 "effective_from": "%s", "effective_to": "%s", "sales_channel": "admin", "adjustment_type": "fixed_delta",
                 "adjustment_amount_cent": 1000}
                """.formatted(UUID.randomUUID(), ticket, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30)));
    }

    private ResultActions partnerVoucher(RequestPostProcessor who, String template) throws Exception {
        return send(who, post("/api/v1/ticketing/partner-vouchers/configs"), """
                {"idempotency_key": "c354-pv-%s", "entitlement_template_id": "%s", "settlement_mode": "free_marketing",
                 "partner_kind": "merchant", "partner_id": "%s"}
                """.formatted(UUID.randomUUID(), template, barA));
    }

    private Integer currentQrInterval() {
        List<Integer> values = jdbc.queryForList("SELECT refresh_interval_seconds FROM ticket_qr_refresh_config WHERE id = 'default'",
                Integer.class);
        return values.isEmpty() ? null : values.getFirst();
    }

    private Long weekdayPrice(String ticket) {
        return jdbc.queryForObject("SELECT weekday_price_cent FROM ticket_types WHERE id = ?", Long.class, ticket);
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private ResultActions send(RequestPostProcessor who, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void merchant(String id, String merchantName, String type) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'active', ?, now(), now())", id, merchantName + s, type, project);
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
