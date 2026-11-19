package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
 * 第十八批 BA 段第 355 条 355-R1（BSM:1671「价格变更形成新版本、指定生效时间并只影响后续业务」；BSM:521「必须填写生效起始日期，并先存为草案、经高敏权限
 * “转正”审批通过后按指定日期生效，不即改即生效」；ENUM:187「全系统的钱账统一按「营业日」归集。」）：内部结算价每个版本填生效起始日期（须晚于提供方当前营业日），
 * 批准后待生效、到日由系统转正并同步业务快照；同一生效日后批准的覆盖先批准的；批准时日期已到 409。控制席并入（BSM:1671「实际提供商品或服务的主体提交价格，
 * 对应域主管公司审批」）：域主管公司的审批人（不挂商户的非平台管理员）打开目录列表看得到本公司该批的条目。
 *
 * <p>真实接口与授权链（蛇形键）：餐厅自己的账号提交，本项目城市项目公司的员工复核（ENUM:54「餐饮与零售商户……统一由城市项目公司审批」），平台管理员按票务策略页
 * 建权益模板引用条目。夹具随测试事务回滚。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class InternalPriceEffectiveDateJdbcIntegrationTests {

    private static final String BASE = "/api/v1/settlement/config";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;
    @Autowired private InternalPriceScheduledActivationService activation;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_c355_" + s;
    private final String restaurant = "rest_c355_" + s;
    private final String city = "be_c355_city_" + s;
    private final String codeA = "sku_c355_a_" + s;
    private RequestPostProcessor pricer;
    private RequestPostProcessor reviewer;
    private RequestPostProcessor admin;
    private LocalDate today;
    private int seq;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '内部结算价生效日综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'catering', 'active', ?, now(), now())", restaurant, "生效日餐厅" + s, project);
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'project_company', 'active', now(), now())", city, "生效日城市项目公司" + s);
        jdbc.update("INSERT INTO business_entity_projects (business_entity_id, project_id) VALUES (?, ?)", city, project);
        // 已有一版生效价 10 元（自始有效）。
        jdbc.update("INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent, status, is_draft,"
                + " updated_by, created_at, updated_at) VALUES (?, ?, ?, 'dining', '生效日套餐', 1000, 'active', FALSE, 'seed_c355', now(), now())",
                "ipc_c355_a0_" + s, codeA, restaurant);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        pricer = staffs.staff("usr_c355_price_" + s, "staff", project, restaurant, InternalPriceScope.SUBMIT);
        reviewer = staffs.staff("usr_c355_review_" + s, "staff", city, null, "settlement.config.manage", "settlement.config.view");
        admin = staffs.staff("usr_c355_admin_" + s, "platform_admin", project, null, "ticket.entitlement.manage", "ticket.product.view");
        today = businessDays.merchantBusinessDate(null, restaurant, Instant.now());
    }

    /**
     * 生效起始日期不晚于今天 400；次日的两版先后批准都待生效，目录与业务快照仍是旧价；只有待生效版本的新条目也能被权益模板引用（快照为空）；
     * 到次日由系统转正：后批准的 13 元生效、旧价与先批准的 12 元下线，两张模板快照同步，各落生效留痕。修复前请求不收生效日期、批准即生效，断言红。
     */
    @Test
    void approvedVersionsWaitForTheirEffectiveDateAndTheLaterApprovalWinsOnTheSameDay() throws Exception {
        String templateA = template(codeA);
        assertThat(agreementPrice(templateA)).isEqualTo(1000L);

        submit(codeA, 1200L, today).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        String v1200 = approve(codeA, 1200L);
        String v1300 = approve(codeA, 1300L);

        String listed = mockMvc.perform(get(BASE + "/internal-price-items").param("include_drafts", "true").with(reviewer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(listed, "$.data[?(@.id == '" + v1200 + "')].status")).containsExactly("scheduled");
        assertThat(JsonPath.<List<String>>read(listed, "$.data[?(@.id == '" + v1300 + "')].effective_from"))
                .containsExactly(today.plusDays(1).toString());
        assertThat(activePrice(codeA)).isEqualTo(1000L);
        assertThat(agreementPrice(templateA)).isEqualTo(1000L);

        String codeB = JsonPath.read(submit(null, 900L, today.plusDays(1)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");
        activate(draftOf(codeB)).andExpect(status().isOk());
        String templateB = template(codeB);
        assertThat(agreementPrice(templateB)).isNull();

        activation.activateDue(Instant.now().plus(Duration.ofDays(1)));

        assertThat(activePrice(codeA)).isEqualTo(1300L);
        assertThat(statusOf(v1200)).isEqualTo("inactive");
        assertThat(statusOf("ipc_c355_a0_" + s)).isEqualTo("inactive");
        assertThat(agreementPrice(templateA)).isEqualTo(1300L);
        assertThat(agreementPrice(templateB)).isEqualTo(900L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'settlement.config.internal_price_catalog.effective'"
                + " AND business_id = ?", Integer.class, codeA)).isEqualTo(2);
    }

    /** 批准时生效起始日期已到（按营业日）409，草案不动、由提交人改期重报。修复前批准即生效，断言红。 */
    @Test
    void approvalAfterTheEffectiveDateHasArrivedIsRejected() throws Exception {
        String code = "sku_c355_c_" + s;
        String draft = "ipc_c355_c_" + s;
        jdbc.update("INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent, status, is_draft,"
                + " updated_by, created_at, updated_at, effective_from) VALUES (?, ?, ?, 'dining', '过期草案', 800, 'active', TRUE,"
                + " 'usr_c355_price_" + s + "', now(), now(), ?)", draft, code, restaurant, today);
        activate(draft).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INTERNAL_PRICE_EFFECTIVE_DATE_PASSED"));
        assertThat(jdbc.queryForObject("SELECT is_draft FROM internal_price_catalog WHERE id = ?", Boolean.class, draft)).isTrue();
    }

    /**
     * 18BA 收尾复核修正（BSM:1671「任何商户在没有获批内部结算价之前不得为其开通需要该价格结算的权益或券」；BSM:521「按指定日期生效，不即改即生效」）：
     * 只有待生效版本的新条目还没有生效价，券（建券即要有大于 0 的生效价）引用它 400 CATALOG_ITEM_NOT_EFFECTIVE，报错说明已批准待生效的到生效起始日期后再建券。
     * 营销人员按券台账页「新建券定义」的真实请求（蛇形键）。修复前报错只叫人去结算配置中心完成复核，断言红。
     */
    @Test
    void voucherReferencingAnItemOnlyApprovedAndWaitingIsToldToWaitForTheEffectiveDate() throws Exception {
        String codeB = JsonPath.read(submit(null, 900L, today.plusDays(1)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");
        activate(draftOf(codeB)).andExpect(status().isOk());
        RequestPostProcessor marketer = new StaffRequestFixture(jdbc).staff("usr_c355_vmgr_" + s, "platform_admin", project, null,
                "customer.voucher.manage");
        String body = mockMvc.perform(post("/api/v1/vouchers/definitions").with(marketer).contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c355-voucher-%s", "name": "生效日餐饮券", "voucher_kind": "merchant_discount",
                         "redeem_method": "in_store_staff", "settlement_mode": "internal_price", "merchant_id": "%s",
                         "face_value_cent": 900, "valid_days": 30, "catalog_item_code": "%s"}
                        """.formatted(s, restaurant, codeB)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("CATALOG_ITEM_NOT_EFFECTIVE"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<String>read(body, "$.message")).contains("已批准待生效的请到生效起始日期后再建券");
    }

    // ---------- helpers ----------

    /** 内部结算价目录页「保存草案」的真实请求（蛇形键）。 */
    private ResultActions submit(String itemCode, long priceCent, LocalDate effectiveFrom) throws Exception {
        return mockMvc.perform(post(BASE + "/internal-price-item").with(pricer).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key": "c355-%s-%s", "item_code": %s, "merchant_id": "%s", "category": "dining",
                 "item_name": "生效日套餐", "price_cent": %d, "effective_from": "%s"}
                """.formatted(++seq, s, itemCode == null ? "null" : "\"" + itemCode + "\"", restaurant, priceCent, effectiveFrom)));
    }

    /** 提交次日生效的一版并由城市项目公司员工批准，返回该版本行 id。 */
    private String approve(String itemCode, long priceCent) throws Exception {
        submit(itemCode, priceCent, today.plusDays(1)).andExpect(status().isOk());
        String draft = draftOf(itemCode);
        activate(draft).andExpect(status().isOk());
        return draft;
    }

    private ResultActions activate(String draft) throws Exception {
        return mockMvc.perform(post(BASE + "/internal_price_item/" + draft + "/activate").with(reviewer));
    }

    private String draftOf(String itemCode) {
        return jdbc.queryForObject("SELECT id FROM internal_price_catalog WHERE item_code = ? AND is_draft = TRUE AND status = 'active'",
                String.class, itemCode);
    }

    /** 票务策略页建餐饮权益模板并引用目录条目（蛇形键），返回模板 id。 */
    private String template(String catalogCode) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "c355-tpl-%s", "name": "生效日餐饮%s", "entitlement_type": "dining",
                                 "default_total_times": 1, "default_value_cent": 0, "status": "active", "merchant_id": "%s",
                                 "catalog_item_code": "%s"}
                                """.formatted(UUID.randomUUID(), catalogCode, restaurant, catalogCode)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.id");
    }

    private Long agreementPrice(String template) {
        Map<String, Object> row = jdbc.queryForMap("SELECT agreement_price_cent FROM entitlement_templates WHERE id = ?", template);
        return row.get("agreement_price_cent") == null ? null : ((Number) row.get("agreement_price_cent")).longValue();
    }

    private long activePrice(String itemCode) {
        return jdbc.queryForObject("SELECT price_cent FROM internal_price_catalog WHERE item_code = ? AND is_draft = FALSE AND status = 'active'",
                Long.class, itemCode);
    }

    private String statusOf(String id) {
        return jdbc.queryForObject("SELECT status FROM internal_price_catalog WHERE id = ?", String.class, id);
    }
}
