package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
 * 第十八批 BB 段第 279 条 279-R1（BSM:1606「只要一个动作有过程，就要尽量在业务发生时自动留下记录。」；BSM:1618「第三方回流失败、重复、延迟、差异。」；
 * PGM:966「已拍板：所有有过程的业务动作都必须留痕」）：员工人工导入合作方券核销回流，成功、按同一外部核销单号重复导入与失败都记入操作留痕，
 * 带操作人；留痕的幂等键栏记外部核销单号。
 * 第 280 条 280-R1（PGM:528「④看不到别的商户和别的项目的数据」；PGM:659「数据的项目与商户隔离不提供关闭开关，与导出脱敏同例。」）：
 * 导入按券所属订单的项目校验可触及范围，闸在按外部单号去重之前；订单无项目的券只有平台管理员能导入。
 * 控制席并入②（PGM:966；PGM:659「所有业务数据默认按项目隔离」）：合作方券配置留操作留痕，按权益模板提供方商户的项目做范围闸。
 * 真库、真实 HTTP、真实授权链，请求体按网页蛇形键（网页客户端给每个 POST 自动附 idempotency_key）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class PartnerVoucherRedemptionImportJdbcIntegrationTests {

    private static final String IMPORTED = "ticket.partner_voucher_redemption.imported";
    private static final String IMPORT_FAILED = "ticket.partner_voucher_redemption.import_failed";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = String.valueOf(System.nanoTime());
    private final String projectA = "pvi_prj_a_" + s;
    private final String projectB = "pvi_prj_b_" + s;
    private final String partner = "pvi_cat_" + s;
    private final String template = "et_pvi_" + s;
    private final String ticketType = "tt_pvi_" + s;
    private RequestPostProcessor admin;
    private RequestPostProcessor staffA;
    private RequestPostProcessor staffB;

    @BeforeEach
    void seed() throws Exception {
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','券回流综合体','active',now(),now())", project);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'回流餐厅','catering','active',?,now(),now())",
                partner, projectA);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '通票含餐厅套餐券', 'voucher', 2, 0, 'active', ?, now(), now())", template, partner);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '含餐通票', 'fixed', 'active', now(), now())", ticketType);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff(user("admin"), "platform_admin", projectA, null, "ticket.entitlement.manage", "verification.execute");
        staffA = staffs.staff(user("a"), "staff", projectA, null, "ticket.entitlement.manage", "verification.execute");
        staffB = staffs.staff(user("b"), "staff", projectB, null, "ticket.entitlement.manage", "verification.execute");
        config(admin).andExpect(status().isOk());
    }

    // 279-R1：A 项目票务员导入 A 项目订单下的券 → 留痕一行（操作人、记录编号、外部单号、非重复）；同一外部单号重放返回原记录、不再扣次，
    // 再记一行标重复。修复前红：没有留痕
    @Test
    void importAndDuplicateAreAuditedWithTheOperator() throws Exception {
        String code = issue("own", projectA);
        String ref = "POS-PVI-OWN-" + s;
        String id = id(importRedemption(staffA, code, ref).andExpect(status().isOk()));

        assertThat(audits(IMPORTED, user("a"))).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("event_result", "success").containsEntry("business_id", id)
                .containsEntry("idempotency_key", ref).containsEntry("duplicate", "false"));

        importRedemption(staffA, code, ref).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(id));
        assertThat(usedTimes(code)).isEqualTo(1);
        assertThat(audits(IMPORTED, user("a"))).extracting(row -> row.get("duplicate")).containsExactly("false", "true");
    }

    // 279-R1：券码不存在 → 404，失败留痕记操作人与错误码。修复前红：没有留痕
    @Test
    void failedImportIsAuditedWithTheOperator() throws Exception {
        importRedemption(staffA, "PV-PVI-NONE-" + s, "POS-PVI-NONE-" + s).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_NOT_FOUND"));

        assertThat(audits(IMPORT_FAILED, user("a"))).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("event_result", "failure").containsEntry("error_code", "ENTITLEMENT_NOT_FOUND"));
    }

    // 280-R1：A 项目员工导入 B 项目订单下的券 → 403，不扣次、不落回流记录、不挂应付，失败留痕记 CROSS_PROJECT_FORBIDDEN。修复前红：200
    @Test
    void importOfAnotherProjectsVoucherIsRejected() throws Exception {
        String code = issue("other", projectB);
        importRedemption(staffA, code, "POS-PVI-OTHER-" + s).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));

        assertThat(usedTimes(code)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM partner_voucher_redemptions WHERE entitlement_code=?", Long.class, code)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type='partner_voucher' AND counterparty_id=?", Long.class, partner)).isZero();
        assertThat(audits(IMPORT_FAILED, user("a"))).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("error_code", "CROSS_PROJECT_FORBIDDEN"));
    }

    // 280-R1：B 项目员工先用外部单号 R 导入 B 项目的券，A 项目员工拿同一券码、同一 R 重放 → 403，不返回原记录。修复前红：命中去重 200
    @Test
    void replayOfAnotherProjectsReferenceIsRejectedBeforeDeduplication() throws Exception {
        String code = issue("replay", projectB);
        String ref = "POS-PVI-REPLAY-" + s;
        importRedemption(staffB, code, ref).andExpect(status().isOk());

        importRedemption(staffA, code, ref).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    // 280-R1：平台管理员（BSM:576、第十八批裁决 71）导入 B 项目的券与订单无项目的券都 200；订单无项目的券普通账号 403。修复前红：普通账号 200
    @Test
    void platformAdminImportsAnyProjectWhileProjectlessVoucherIsAdminOnly() throws Exception {
        importRedemption(admin, issue("admin_b", projectB), "POS-PVI-ADMIN-B-" + s).andExpect(status().isOk());
        String projectless = issue("none", null);
        importRedemption(staffA, projectless, "POS-PVI-NOPRJ-A-" + s).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        importRedemption(admin, projectless, "POS-PVI-NOPRJ-ADMIN-" + s).andExpect(status().isOk());
    }

    // 控制席并入②：模板提供方商户在 A——B 项目员工配置 403、失败留痕；A 项目员工配置 200、成功留痕记操作人与模板。修复前红：B 项目员工 200
    @Test
    void configIsAuditedAndGatedByTheTemplateProviderProject() throws Exception {
        config(staffB).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(audits("ticket.partner_voucher_config.save_failed", user("b"))).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("business_id", template).containsEntry("error_code", "CROSS_PROJECT_FORBIDDEN"));

        config(staffA).andExpect(status().isOk());
        assertThat(audits("ticket.partner_voucher_config.saved", user("a"))).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("event_result", "success").containsEntry("business_id", template));
    }

    // ── 夹具 ──

    /** 在指定项目（可空）的已付订单下发一张 2 次的合作方券，返回券码。 */
    private String issue(String tag, String project) {
        String order = "order_pvi_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status, issue_status, status,"
                + " created_at, updated_at, project_id) VALUES (?, ?, ?, 1, 'web', 0, 'paid', 'issued', 'created', now(), now(), ?)",
                order, "ONO_PVI_" + tag + "_" + s, ticketType, project);
        String code = "PV-PVI-" + tag + "-" + s;
        jdbc.update("INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code, entitlement_type,"
                + " total_times, used_times, status, valid_from, valid_to, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, 'voucher', 2, 0, 'active', now() - interval '1 day', now() + interval '30 days', now(), now())",
                "ie_pvi_" + tag + "_" + s, ticketType, order, template, code);
        return code;
    }

    /** 票务策略页「核销回流导入」的请求形状（不填核销日期）。 */
    private ResultActions importRedemption(RequestPostProcessor who, String code, String ref) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/partner-vouchers/redemptions").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pvi-" + UUID.randomUUID(), "entitlement_code", code,
                        "external_redemption_ref", ref, "source", "reconciliation_file"))));
    }

    /** 票务策略页「第三方券配置」的请求形状：内部价采购券，合作方为入驻餐厅。 */
    private ResultActions config(RequestPostProcessor who) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/partner-vouchers/configs").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pvc-" + UUID.randomUUID(), "entitlement_template_id", template,
                        "settlement_mode", "internal_purchased", "partner_kind", "merchant", "partner_id", partner,
                        "internal_price_cent", 2000))));
    }

    private List<Map<String, Object>> audits(String event, String actor) {
        return jdbc.queryForList("SELECT event_result, business_id, idempotency_key, metadata->>'duplicate' AS duplicate,"
                + " metadata->>'error_code' AS error_code FROM audit_events WHERE event_name=? AND actor_id=? ORDER BY occurred_at, id", event, actor);
    }

    private int usedTimes(String code) {
        return jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE entitlement_code=?", Integer.class, code);
    }

    private String user(String tag) {
        return "usr_pvi_" + tag + "_" + s;
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }
}
