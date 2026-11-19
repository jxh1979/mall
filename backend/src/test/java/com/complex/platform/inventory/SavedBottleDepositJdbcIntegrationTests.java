package com.complex.platform.inventory;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AP 段第 254 条 254-R1、254-R2、254-R4（真库、真实 HTTP、蛇形键、真实授权链）。
 * 254-R1：BSM:1170「存酒登记必须拍照、填写人工备注，并留取客人手机号和姓名/昵称。」——登记人工备注必填。
 * 254-R2：BSM:1072「……客户存酒有效期（业务参数，按项目可配，出厂默认 30 天，须同时配置上下限），登记时即固化本次适用的有效期与到期日，
 * 后续改配置不追溯已登记的存酒……」——到期日由服务端按项目有效期固化，不收请求里的到期日。
 * 254-R4：第十八批裁决 57「短信验证码或者小程序，这个点击同意都可以。」——登记前向客人手机发隐私同意验证码，客人报码核验通过才建存酒行。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SavedBottleDepositJdbcIntegrationTests {

    private static final String BASE = "/api/v1/inventory/saved-bottles";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "prj_b62dp_" + s;
    private final String merchant = "bar_b62dp_" + s;
    private final String item = "item_b62dp_" + s;
    private final String phone = "155" + s.substring(s.length() - 8);
    private RequestPostProcessor clerk;
    private RequestPostProcessor platform;

    @BeforeEach
    void seed() {
        String entity = "ent_b62dp_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','存酒登记综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'存酒登记主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'存酒登记酒吧','bar',?,?,'active',now(),now())", merchant, entity, project);
        // 员工代办转存的准入闸 inventory.saved_bottle_claim 按合同经营范围判：给本酒吧一份含 inventory 的生效合同。
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,"
                + "allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\"]'::jsonb,now(),now())",
                "ct_b62dp_" + s, merchant, entity, entity);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at)"
                + " VALUES(?,'存酒登记威士忌','liquor','瓶',0,'active',?,now(),now())", item, merchant);
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        clerk = fixture.staff("usr_b62dp_clerk_" + s, "staff", project, merchant, "bar.saved_bottle.manage");
        platform = fixture.staff("usr_b62dp_plat_" + s, "platform_admin", "org_platform", null, "bar.saved_bottle.renewal_policy.manage");
    }

    // 254-R4＋254-R2：先向客人手机发隐私同意验证码，客人报码后登记；人工备注、登记照片落库，到期日＝登记时刻＋出厂有效期 30 天。
    @Test
    void consentCodeThenRegistrationFixesExpiryAtFactoryValidity() throws Exception {
        Map<String, Object> consent = new LinkedHashMap<>();
        consent.put("idempotency_key", "b62dp-consent-" + s);
        consent.put("customer_phone", phone);
        call(post(BASE + "/deposit-consent-code"), clerk, consent)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scene").value("deposit"));

        String id = register(registration("b62dp-create-" + s, "000000", "吧台登记：客人存半瓶"));

        assertThat(jdbc.queryForObject("SELECT manual_note FROM saved_bottles WHERE id=?", String.class, id))
                .isEqualTo("吧台登记：客人存半瓶");
        assertThat(jdbc.queryForObject("SELECT registration_photo_refs::text FROM saved_bottles WHERE id=?", String.class, id))
                .contains("reg_photo_b62dp");
        assertThat(validitySeconds(id)).isEqualTo(30L * 86400);
    }

    // 254-R1＋254-R4：缺人工备注、缺验证码 → 400；验证码不对 → 409 CUSTOMER_OTP_INVALID（18AP 收尾复核修正：不回 401，401 只表示员工登录会话失效）；
    // 三者都不建存酒行。
    @Test
    void registrationRequiresManualNoteAndCustomerSmsCode() throws Exception {
        call(post(BASE), clerk, registration("b62dp-nonote-" + s, "000000", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("人工备注")));
        call(post(BASE), clerk, registration("b62dp-nocode-" + s, null, "吧台登记"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        call(post(BASE), clerk, registration("b62dp-badcode-" + s, "123456", "吧台登记"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_OTP_INVALID"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM saved_bottles WHERE item_id=?", Long.class, item)).isZero();
    }

    // 18AP 收尾复核修正：员工代客办理的持有人核码（领取、转让、代办转存共用）客人报错码 → 409 CUSTOMER_OTP_INVALID，不回 401；存酒不变。
    @Test
    void holderSmsWrongCodeIsConflictNotUnauthorized() throws Exception {
        String id = register(registration("b62dp-holder-" + s, "000000", "吧台登记"));

        Map<String, Object> renew = new LinkedHashMap<>();
        renew.put("idempotency_key", "b62dp-renew-badcode-" + s);
        renew.put("verification_code", "123456");
        call(post(BASE + "/" + id + "/renewals"), clerk, renew)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_OTP_INVALID"));

        assertThat(jdbc.queryForObject("SELECT status FROM saved_bottles WHERE id=?", String.class, id)).isEqualTo("stored");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM saved_bottle_renewals WHERE saved_bottle_id=?", Long.class, id)).isZero();
    }

    // 254-R2：项目有效期配成 45 天后新登记按 45 天固化，此前登记的到期日不变；越界、缺上下限、按酒吧配 → 400；新登记的存酒可转存。
    @Test
    void projectValidityDaysAppliesToNewRegistrationsOnly() throws Exception {
        String before = register(registration("b62dp-before-" + s, "000000", "吧台登记"));

        call(put(BASE + "/renewal-policy"), platform, validity("b62dp-pol-" + s, "project:" + project, 45, 7, 90))
                .andExpect(status().isOk());
        String after = register(registration("b62dp-after-" + s, "000000", "吧台登记"));

        assertThat(validitySeconds(after)).isEqualTo(45L * 86400);
        assertThat(validitySeconds(before)).isEqualTo(30L * 86400);

        Map<String, Object> renew = new LinkedHashMap<>();
        renew.put("idempotency_key", "b62dp-renew-" + s);
        renew.put("verification_code", "000000");
        call(post(BASE + "/" + after + "/renewals"), clerk, renew)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));

        // 失败的写请求放最后：测试事务里它们把外层事务标记为只回滚，之后准入闸的保存点求值会失败转复核（真实请求各自一个事务，无此问题）。
        call(put(BASE + "/renewal-policy"), platform, validity("b62dp-pol-over-" + s, "project:" + project, 100, 7, 90))
                .andExpect(status().isBadRequest());
        call(put(BASE + "/renewal-policy"), platform, validity("b62dp-pol-nobound-" + s, "project:" + project, 45, null, null))
                .andExpect(status().isBadRequest());
        call(put(BASE + "/renewal-policy"), platform, validity("b62dp-pol-bar-" + s, "bar:" + merchant, 45, 7, 90))
                .andExpect(status().isBadRequest());
    }

    // 325-R2（契约与代码一致）：没有 bar.saved_bottle.manage 的员工登记存酒、取酒都 403（权限闸在读存酒之前），不建存酒行；契约这两个操作都列 403。
    @Test
    void registrationAndClaimWithoutManagePermissionAreForbiddenAndContractListsForbidden() throws Exception {
        RequestPostProcessor noManage = new StaffRequestFixture(jdbc).staff("usr_b62dp_nom_" + s, "staff", project, merchant,
                "bar.saved_bottle.renewal_policy.manage");
        call(post(BASE), noManage, registration("b62dp-nomanage-" + s, "000000", "吧台登记"))
                .andExpect(status().isForbidden());
        Map<String, Object> claim = new LinkedHashMap<>();
        claim.put("idempotency_key", "b62dp-nomanage-claim-" + s);
        claim.put("verification_code", "000000");
        claim.put("bottle_label_value", "LBL-B62DP");
        claim.put("customer_name_or_nickname", "登记客人");
        call(post(BASE + "/sb_b62dp_any_" + s + "/claim"), noManage, claim)
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM saved_bottles WHERE item_id=?", Long.class, item)).isZero();

        Path contract = Path.of("..", "docs", "openapi", "openapi-p0.json");
        if (!Files.exists(contract)) {
            contract = Path.of("docs", "openapi", "openapi-p0.json");
        }
        JsonNode paths = json.readTree(Files.readString(contract)).path("paths");
        assertThat(paths.path(BASE).path("post").path("responses").has("403")).as("createSavedBottle 列 403").isTrue();
        assertThat(paths.path(BASE + "/{id}/claim").path("post").path("responses").has("403")).as("claimSavedBottle 列 403").isTrue();
    }

    private Map<String, Object> registration(String key, String code, String note) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("bar_id", merchant);
        body.put("items", List.of(Map.of("item_id", item, "quantity", 1, "bottle_name", "存酒登记威士忌", "unit", "瓶")));
        body.put("bottle_label_value", "LBL-B62DP");
        body.put("customer_phone", phone);
        body.put("customer_name_or_nickname", "登记客人");
        body.put("registration_photo_refs", List.of("reg_photo_b62dp"));
        if (note != null) {
            body.put("manual_note", note);
        }
        if (code != null) {
            body.put("verification_code", code);
        }
        return body;
    }

    private Map<String, Object> validity(String key, String scope, Integer value, Integer min, Integer max) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("policy_key", "validity_days");
        body.put("scope_id", scope);
        body.put("value", value);
        body.put("min_value", min);
        body.put("max_value", max);
        return body;
    }

    private String register(Map<String, Object> body) throws Exception {
        return JsonPath.read(call(post(BASE), clerk, body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");
    }

    private Long validitySeconds(String id) {
        return jdbc.queryForObject("SELECT EXTRACT(EPOCH FROM expire_at - created_at)::bigint FROM saved_bottles WHERE id=?",
                Long.class, id);
    }

    private ResultActions call(MockHttpServletRequestBuilder request, RequestPostProcessor who, Map<String, Object> body)
            throws Exception {
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
}
