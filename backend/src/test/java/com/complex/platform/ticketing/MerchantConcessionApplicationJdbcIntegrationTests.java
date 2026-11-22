package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 BV 段第 264 条（第十八批裁决 78「商户用自己的账号提交让利申请（哪张票、哪项权益）并在线确认，运营配票种时只能从已确认的申请里选商户让利型。」；
 * BSM:1033、ENUM:53「只有商户主动申请并书面确认让利的权益才记为商户让利、不结算」）。
 * 264-R1：商户在商户专区提交并在线确认、查看本商户的申请，无商户归属或无权限即拒；264-R2：标商户让利型的绑定只能选该权益提供商户为本票提交、
 * 尚未用于本票其他权益的申请，运营按票种列出该票各商户的申请；已上架跨域组合通票的选用随草稿版，撤回草稿即释放。
 * 真实接口、蛇形键、真实授权链；不开测试事务，@AfterEach 按票种清理（申请随票种级联删除）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class MerchantConcessionApplicationJdbcIntegrationTests {

    private static final String APPLY = "merchant.portal.concession.apply";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final long n = Long.parseLong(suffix, 16);
    private final String project = "prj_tca_" + suffix;
    /** 本商户：提交让利申请的酒吧。 */
    private final String bar = "bar_tca_" + suffix;
    private final String otherBar = "bar_tca_o_" + suffix;
    private final String park = "amu_tca_" + suffix;
    private final List<String> ticketTypes = new ArrayList<>();
    private final List<String> templates = new ArrayList<>();
    private StaffRequestFixture staffs;
    /** 本商户账号（商户专区）。 */
    private RequestPostProcessor shop;
    /** 票务运营。 */
    private RequestPostProcessor operator;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '让利申请综合体', 'active', now(), now())", project);
        merchant(bar, "让利酒吧", "bar");
        merchant(otherBar, "别家酒吧", "bar");
        merchant(park, "让利乐园", "amusement");
        staffs = new StaffRequestFixture(jdbc);
        shop = staffs.staff("usr_tca_m_" + suffix, "staff", null, bar, APPLY);
        operator = staffs.staff("usr_tca_op_" + suffix, "platform_admin", "org_1", null,
                "ticket.entitlement.manage", "ticket.product.manage", "ticket.product.view");
    }

    @AfterEach
    void cleanup() {
        for (String type : ticketTypes) {
            jdbc.update("DELETE FROM ticket_concession_applications WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_rule_versions WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_types WHERE id = ?", type);
        }
        for (String template : templates) {
            jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", template);
        }
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
    }

    /** ①商户专区列出可申请的票（通票与本店单店票；别家单店票、已存档的不列），勾选确认后提交即在线确认；同一请求编号重放只认一次。 */
    @Test
    void merchantSubmitsAndConfirmsConcessionApplication() throws Exception {
        String pass = ticketType("让利通票", null, false);
        String own = ticketType("本店单店票", bar, false);
        String foreign = ticketType("别家单店票", otherBar, false);
        String archived = ticketType("已存档通票", null, true);

        String options = body(mockMvc.perform(get("/api/v1/merchant-portal/concession-ticket-types").with(shop)).andExpect(status().isOk()));
        assertThat(JsonPath.<List<String>>read(options, "$.data[*].id")).contains(pass, own).doesNotContain(foreign, archived);
        assertThat(JsonPath.<List<String>>read(options, "$.data[?(@.id == '" + pass + "')].ticket_name")).containsExactly("让利通票" + n);

        String key = "tca-apply-" + suffix;
        submit(shop, key, pass, "赠送招牌鸡尾酒一杯", "true").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket_type_id").value(pass))
                .andExpect(jsonPath("$.data.entitlement_content").value("赠送招牌鸡尾酒一杯"));
        assertThat(jdbc.queryForObject("SELECT merchant_id || '/' || confirmed_by FROM ticket_concession_applications WHERE ticket_type_id = ?",
                String.class, pass)).isEqualTo(bar + "/usr_tca_m_" + suffix);
        submit(shop, key, pass, "赠送招牌鸡尾酒一杯", "true").andExpect(status().isOk());
        assertThat(applications(pass)).isEqualTo(1);

        String mine = body(mockMvc.perform(get("/api/v1/merchant-portal/concession-applications").with(shop)).andExpect(status().isOk()));
        assertThat(JsonPath.<List<String>>read(mine, "$.data[*].ticket_name")).containsExactly("让利通票" + n);
        assertThat(JsonPath.<List<String>>read(mine, "$.data[*].entitlement_content")).containsExactly("赠送招牌鸡尾酒一杯");
        assertThat(JsonPath.<List<String>>read(mine, "$.data[*].confirmed_at")).hasSize(1).doesNotContainNull();
    }

    /** ②未确认、内容为空或超 200 字一律 400；别家单店票 404；都不落库。 */
    @Test
    void unconfirmedBlankOrForeignTicketIsRejected() throws Exception {
        String pass = ticketType("让利通票", null, false);
        String foreign = ticketType("别家单店票", otherBar, false);

        rejected(submit(shop, key(), pass, "赠送小食一份", null), 400, "VALIDATION_FAILED");
        rejected(submit(shop, key(), pass, "赠送小食一份", "false"), 400, "VALIDATION_FAILED");
        rejected(submit(shop, key(), pass, " ", "true"), 400, "VALIDATION_FAILED");
        rejected(submit(shop, key(), pass, "赠".repeat(201), "true"), 400, "VALIDATION_FAILED");
        rejected(submit(shop, key(), foreign, "赠送小食一份", "true"), 404, "TICKET_TYPE_NOT_FOUND");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_concession_applications WHERE merchant_id = ?", Integer.class, bar))
                .isZero();
    }

    /**
     * ③平台账号（有本权限码）无商户归属一律 403 MERCHANT_SCOPE_REQUIRED；商户账号没有本权限码 403 PERMISSION_DENIED；都不落库。
     * 运营侧按票种列申请（listTicketTypeConcessionApplications）只认 ticket.product.view：商户账号直接调 403 PERMISSION_DENIED
     * （PGM:744「前端隐藏只是辅助，后端接口必须独立校验」）。
     */
    @Test
    void nonMerchantAccountIsRefused() throws Exception {
        String pass = ticketType("让利通票", null, false);
        RequestPostProcessor platform = staffs.staff("usr_tca_pa_" + suffix, "platform_admin", "org_1", null, APPLY);
        RequestPostProcessor noGrant = staffs.staff("usr_tca_ng_" + suffix, "staff", null, bar, "merchant.portal.settlement.view");

        for (String path : List.of("/api/v1/merchant-portal/concession-ticket-types", "/api/v1/merchant-portal/concession-applications")) {
            rejected(mockMvc.perform(get(path).with(platform)), 403, "MERCHANT_SCOPE_REQUIRED");
            rejected(mockMvc.perform(get(path).with(noGrant)), 403, "PERMISSION_DENIED");
        }
        rejected(submit(platform, key(), pass, "赠送小食一份", "true"), 403, "MERCHANT_SCOPE_REQUIRED");
        rejected(submit(noGrant, key(), pass, "赠送小食一份", "true"), 403, "PERMISSION_DENIED");
        rejected(mockMvc.perform(get("/api/v1/ticketing/ticket-types/" + pass + "/concession-applications").with(shop)), 403,
                "PERMISSION_DENIED");

        assertThat(applications(pass)).isZero();
    }

    /**
     * ④标商户让利型须带该权益提供商户为本票提交并确认的申请：不带、别家的、别的票的、不存在的一律拒且不落库；带对了即选用，运营按票种列出看到所选权益；
     * 同一申请再用于另一项权益 409；平台出资型照旧不带。
     */
    @Test
    void concessionBindingNeedsThatMerchantsConfirmedApplication() throws Exception {
        String pass = ticketType("让利通票", null, false);
        String otherPass = ticketType("另一张通票", null, false);
        String snackName = "酒吧小食" + n;
        String snack = template(bar, snackName, "dining", bar);

        rejected(bind(pass, snack, 0L, "merchant_concession", null), 400, "TICKET_CONCESSION_APPLICATION_REQUIRED");
        assertThat(bindings(pass)).isZero();
        rejected(bind(pass, snack, 0L, "merchant_concession", ConcessionApplicationFixture.forMerchant(jdbc, pass, otherBar)),
                400, "TICKET_CONCESSION_APPLICATION_MISMATCH");
        rejected(bind(pass, snack, 0L, "merchant_concession", ConcessionApplicationFixture.forMerchant(jdbc, otherPass, bar)),
                400, "TICKET_CONCESSION_APPLICATION_MISMATCH");
        rejected(bind(pass, snack, 0L, "merchant_concession", "tca_missing_" + suffix), 400, "TICKET_CONCESSION_APPLICATION_NOT_FOUND");
        assertThat(bindings(pass)).isZero();

        String application = ConcessionApplicationFixture.forMerchant(jdbc, pass, bar);
        String bindingId = JsonPath.read(body(bind(pass, snack, 0L, "merchant_concession", application).andExpect(status().isOk())), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT binding_id FROM ticket_concession_applications WHERE id = ?", String.class, application))
                .isEqualTo(bindingId);
        String listed = listForTicket(pass);
        assertThat(JsonPath.<List<String>>read(listed, "$.data[?(@.id == '" + application + "')].binding_id")).containsExactly(bindingId);
        assertThat(JsonPath.<List<String>>read(listed, "$.data[?(@.id == '" + application + "')].template_name")).containsExactly(snackName);

        String drink = template(bar, "酒吧赠饮" + n, "dining", bar);
        rejected(bind(pass, drink, 0L, "merchant_concession", application), 409, "TICKET_CONCESSION_APPLICATION_USED");
        bind(pass, drink, 0L, "platform_funded", null).andExpect(status().isOk());
        assertThat(bindings(pass)).isEqualTo(2);
    }

    /** ⑤已上架跨域组合通票加商户让利权益只进草稿版：现表没有、运营列表显示所选权益；撤回草稿后该申请释放，可再选。 */
    @Test
    void concessionOnPublishedCrossDomainPassFollowsDraftVersion() throws Exception {
        String pass = ticketType("跨域组合通票", null, false);
        bind(pass, template(bar, "酒吧入场" + n, "entry", null), 10000L, null, null).andExpect(status().isOk());
        bind(pass, template(park, "游乐入场" + n, "entry", null), 12000L, null, null).andExpect(status().isOk());
        // 已上架（发布审批链由 TicketTypeRuleVersionJdbcIntegrationTests 覆盖，这里直接置上架）。
        jdbc.update("UPDATE ticket_types SET status = 'published' WHERE id = ?", pass);
        String snackName = "酒吧小食" + n;
        String snack = template(bar, snackName, "dining", bar);
        String application = ConcessionApplicationFixture.forMerchant(jdbc, pass, bar);

        String bindingId = JsonPath.read(body(bind(pass, snack, 0L, "merchant_concession", application).andExpect(status().isOk())), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_type_entitlements WHERE ticket_type_id = ? AND entitlement_template_id = ?",
                Integer.class, pass, snack)).isZero();
        String drafted = listForTicket(pass);
        assertThat(JsonPath.<List<String>>read(drafted, "$.data[?(@.id == '" + application + "')].binding_id")).containsExactly(bindingId);
        assertThat(JsonPath.<List<String>>read(drafted, "$.data[?(@.id == '" + application + "')].template_name")).containsExactly(snackName);

        mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + pass + "/rule-versions/withdraw").with(operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\": \"tca-wd-" + suffix + "\", \"reason\": \"先不加这项让利\"}"))
                .andExpect(status().isOk());
        String released = listForTicket(pass);
        assertThat(JsonPath.<List<Object>>read(released, "$.data[?(@.id == '" + application + "')].binding_id")).containsExactly((Object) null);
        assertThat(JsonPath.<List<Object>>read(released, "$.data[?(@.id == '" + application + "')].template_name")).containsExactly((Object) null);

        bind(pass, snack, 0L, "merchant_concession", application).andExpect(status().isOk());
    }

    /**
     * ⑥第十八批裁决 80（BSM:751「商户让利的零元免费权益不结算」；ENUM:37「收费型折扣券带价格，计入业态价格并参与通票池分配，平台按分账比例向商户付款」）：
     * 有价格的权益行不许标商户让利型，带了该商户为本票确认的申请也 400 VALIDATION_FAILED，绑定不落库、申请不被选用；同一申请用于零元权益照常。
     */
    @Test
    void pricedEntitlementCannotBeMerchantConcession() throws Exception {
        String pass = ticketType("让利通票", null, false);
        String snack = template(bar, "酒吧小食" + n, "dining", bar);
        String application = ConcessionApplicationFixture.forMerchant(jdbc, pass, bar);

        bind(pass, snack, 1800L, "merchant_concession", application).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(containsString("只有零元权益才能选商户让利")));
        assertThat(bindings(pass)).isZero();
        assertThat(jdbc.queryForObject("SELECT binding_id FROM ticket_concession_applications WHERE id = ?", String.class, application))
                .isNull();

        bind(pass, snack, 0L, "merchant_concession", application).andExpect(status().isOk());
        assertThat(bindings(pass)).isEqualTo(1);
    }

    private ResultActions submit(RequestPostProcessor who, String key, String ticketTypeId, String content, String confirmed) throws Exception {
        return mockMvc.perform(post("/api/v1/merchant-portal/concession-applications").with(who).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idempotency_key": "%s", "ticket_type_id": "%s", "entitlement_content": "%s"%s}
                        """.formatted(key, ticketTypeId, content, confirmed == null ? "" : ", \"confirmed\": " + confirmed)));
    }

    private ResultActions bind(String type, String template, long componentPriceCent, String costBearer, String application) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + type + "/entitlements").with(operator)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "tca-bind-%s", "entitlement_template_id": "%s", "quantity": 1, "total_times": 1, "num_people": 1,
                         "component_price_cent": %d, "cost_bearer": %s, "concession_application_id": %s}
                        """.formatted(UUID.randomUUID(), template, componentPriceCent, quoted(costBearer), quoted(application))));
    }

    private String listForTicket(String type) throws Exception {
        return body(mockMvc.perform(get("/api/v1/ticketing/ticket-types/" + type + "/concession-applications").with(operator))
                .andExpect(status().isOk()));
    }

    private String ticketType(String name, String barId, boolean archived) {
        String id = "tt_tca_" + suffix + "_" + ticketTypes.size();
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent, holiday_price_cent,"
                        + " stock_required, status, bar_id, archived_at, created_at, updated_at)"
                        + " VALUES (?, ?, 'weekday_weekend_holiday', 22000, 22000, 22000, FALSE, 'draft', ?, ?, now(), now())",
                id, name + n, barId, archived ? Timestamp.from(Instant.now()) : null);
        ticketTypes.add(id);
        return id;
    }

    private String template(String merchant, String name, String type, String exclusiveMerchant) throws Exception {
        String body = body(mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(operator)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "tca-tpl-%s", "name": "%s", "entitlement_type": "%s", "default_total_times": 1,
                         "default_value_cent": 0, "status": "active", "merchant_id": "%s", "catalog_item_code": null,
                         "exclusive_merchant_id": %s, "bar_item_ref": null}
                        """.formatted(UUID.randomUUID(), name, type, merchant, quoted(exclusiveMerchant))))
                .andExpect(status().isOk()));
        String id = JsonPath.read(body, "$.data.id");
        templates.add(id);
        return id;
    }

    private void merchant(String id, String name, String type) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'active', ?, now(), now())", id, name + n, type, project);
    }

    private Integer applications(String type) {
        return jdbc.queryForObject("SELECT count(*) FROM ticket_concession_applications WHERE ticket_type_id = ?", Integer.class, type);
    }

    private Integer bindings(String type) {
        return jdbc.queryForObject("SELECT count(*) FROM ticket_type_entitlements WHERE ticket_type_id = ?", Integer.class, type);
    }

    private static void rejected(ResultActions result, int status, String detailCode) throws Exception {
        result.andExpect(status().is(status)).andExpect(jsonPath("$.detail_code").value(detailCode));
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String key() {
        return "tca-" + UUID.randomUUID();
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
