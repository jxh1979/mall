package com.complex.platform.contract;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.contract.dto.ContractDraftRequest;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BH 段第 261 条 261-B16-R1（真库、真实入口、真实授权链、蛇形键）。依据：PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、
 * 渠道重复回调都只算一次）」；PGM:303「同一次操作重复提交时，返回第一次的结果并提示已完成。」。合同域八个写入口各一条：同一请求编号提交两次，
 * 第二次 200、带已完成标记与提示、返回的对象与第一次相同，库里不多一行、不多一条留痕。修复前第二次或 409、或多建一条（退租开账、着色分类）、或返回空列表。
 *
 * <p>夹具随测试事务回滚（同 ContractApprovalChainJdbcIntegrationTests）：项目、合同主体、起草人→经理→总经理的主线层级、三条平行审批线各自的起点人与最终审批人。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ContractOneShotReplayJdbcIntegrationTests {

    private static final List<String> LINE_CODES = List.of("business_review", "finance_review", "legal_review");

    @Autowired private ContractService contractService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectId = "b16_project_" + suffix;
    private final String subject = "be_b16_" + suffix;
    private String drafter;
    private String gm;
    private final Map<String, String> lineFinal = new LinkedHashMap<>();

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) "
                + "VALUES (?, 'complex', '合同重发回归项目', 'active', now(), now())", projectId);
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, 'management_company', 'active', now(), now())", subject, "合同重发回归合同主体");
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES (?, ?)", subject, projectId);
        drafter = user("drafter");
        String manager = user("manager");
        gm = user("gm");
        superior(drafter, manager);
        superior(manager, gm);
        for (String code : LINE_CODES) {
            String entry = user(code + "_entry");
            String fin = user(code + "_final");
            superior(entry, fin);
            jdbc.update("UPDATE approval_lines SET entry_user_id = ? WHERE line_code = ?", entry, code);
            lineFinal.put(code, fin);
        }
        String role = "role_b16_" + suffix;
        jdbc.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", role, "合同重发回归角色" + suffix);
        for (String permission : List.of("contract.draft.manage", "contract.lifecycle.activate", "contract.change.manage",
                "contract.scope_request.raise", "contract.settlement.manage", "contract.settlement.dispute.resolve", "finance.ar_ap.manage",
                "contract.space.color.manage")) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, permission);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", drafter, role);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitResentReturnsTheFirstResult() throws Exception {
        String contract = draft("sub");
        twice("/api/v1/contracts/" + contract + "/submit", "{\"idempotency_key\":\"" + key("sub") + "\",\"final_approver_id\":\"" + gm
                + "\",\"parallel_lines\":[" + lines() + "],\"reason\":\"提交审批\"}");
        assertThat(contractStatus(contract)).isEqualTo("pending_confirmation");
        assertThat(auditCount("contract.submitted", contract)).isEqualTo(1);
    }

    @Test
    void activateResentReturnsTheFirstResult() throws Exception {
        String contract = draft("act");
        String space = "sp_b16_" + suffix;
        jdbc.update("INSERT INTO spaces (id, space_name, space_type, floor_no, area_square_meter, polygon_points, status, project_id, created_at,"
                + " updated_at) VALUES (?, '合同重发回归铺位', 'shop', '1F', 80, '[]'::jsonb, 'active', ?, now(), now())", space, projectId);
        jdbc.update("UPDATE contracts SET lifecycle_status = 'pending_effective' WHERE id = ?", contract);
        twice("/api/v1/contracts/" + contract + "/activate", "{\"idempotency_key\":\"" + key("act") + "\",\"space_ids\":[\"" + space + "\"]}");
        assertThat(contractStatus(contract)).isEqualTo("effective");
        assertThat(count("contract_spaces WHERE contract_id = ?", contract)).isEqualTo(1);
        assertThat(count("ar_ap_items WHERE source_type = 'contract_deposit' AND source_id = ?", contract)).isEqualTo(1);
    }

    @Test
    void raiseChangeResentReturnsTheFirstResult() throws Exception {
        String contract = effective("chg");
        java.time.YearMonth now = java.time.YearMonth.now(com.complex.platform.common.LocalZone.ID);
        twice("/api/v1/contracts/" + contract + "/changes", "{\"idempotency_key\":\"" + key("chg") + "\",\"change_type\":\"location_change\","
                + "\"payload\":{\"base_rent_cent\":120000,\"deposit_deadline\":\"" + now.plusMonths(1).atDay(1) + "\"},"
                + "\"final_approver_id\":\"" + gm + "\",\"parallel_lines\":[" + lines() + "],\"reason\":\"经营位置变更\"}");
        assertThat(count("contract_changes WHERE contract_id = ?", contract)).isEqualTo(1);
    }

    @Test
    void scopeRequestResentReturnsTheFirstResult() throws Exception {
        String contract = effective("scp");
        String change = twice("/api/v1/contracts/" + contract + "/scope-requests", "{\"idempotency_key\":\"" + key("scp")
                + "\",\"category\":\"snack\",\"reason\":\"加卖小食\",\"grant_type\":\"permanent\",\"final_approver_id\":\"" + gm
                + "\",\"parallel_lines\":[" + lines() + "]}");
        assertThat(count("contract_changes WHERE contract_id = ?", contract)).isEqualTo(1);
        assertThat(auditCount("contract.scope_request_raised", change)).isEqualTo(1);
    }

    /** 退租开账：修复前每次提交各建一张结算单。 */
    @Test
    void settleResentReturnsTheFirstResult() throws Exception {
        String contract = effective("stl");
        String settlement = twice("/api/v1/contracts/" + contract + "/settle",
                "{\"idempotency_key\":\"" + key("stl") + "\",\"trigger_type\":\"move_out\"}");
        assertThat(count("contract_settlements WHERE contract_id = ?", contract)).isEqualTo(1);
        assertThat(auditCount("contract.settled", settlement)).isEqualTo(1);
    }

    @Test
    void resolveDisputeResentReturnsTheFirstResult() throws Exception {
        String contract = effective("dsp");
        String opened = mockMvc.perform(post("/api/v1/contracts/" + contract + "/settle").with(webSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + key("dsp_open") + "\",\"trigger_type\":\"move_out\",\"dispute_reason\":\"押金扣款有异议\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("disputed"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String settlement = JsonPath.read(opened, "$.data.id");
        twice("/api/v1/contracts/settlements/" + settlement + "/resolve-dispute",
                "{\"idempotency_key\":\"" + key("dsp") + "\",\"resolution_note\":\"按合同约定执行\"}");
        assertThat(jdbc.queryForObject("SELECT status FROM contract_settlements WHERE id = ?", String.class, settlement))
                .isEqualTo("pending_settlement");
        assertThat(auditCount("contract.dispute_resolved", settlement)).isEqualTo(1);
    }

    /** 一期全费目挂账：修复前第二次返回空列表、不带已完成标记。 */
    @Test
    void periodBillsResentReturnTheFirstResult() throws Exception {
        String contract = effective("pb");
        String url = "/api/v1/contracts/" + contract + "/period-bills";
        String body = "{\"idempotency_key\":\"" + key("pb") + "\",\"period\":\"2026-08\"}";
        String first = mockMvc.perform(post(url).with(webSession()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> ids = JsonPath.read(first, "$.data[*].id");
        assertThat(ids).isNotEmpty();
        String second = mockMvc.perform(post(url).with(webSession()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(second, "$.data[*].id")).containsExactlyElementsOf(ids);
        assertThat(count("contract_rent_bills WHERE contract_id = ?", contract)).isEqualTo(ids.size());
    }

    /** 着色分类：修复前每次提交各建一条。 */
    @Test
    void colorRuleResentReturnsTheFirstResult() throws Exception {
        String category = "即将到期" + suffix;
        twice("/api/v1/contracts/expiry-color-rules", "{\"idempotency_key\":\"" + key("clr") + "\",\"project_id\":\"" + projectId
                + "\",\"category_name\":\"" + category + "\",\"color\":\"#ff8800\",\"threshold_days\":30,\"seq\":1}");
        assertThat(count("space_expiry_color_rules WHERE category_name = ?", category)).isEqualTo(1);
    }

    // ---------- 夹具与请求 ----------

    /** 同一请求体提交两次：第一次不带已完成标记；第二次 200、带已完成标记与提示、对象编号与第一次相同。返回该编号。 */
    private String twice(String url, String body) throws Exception {
        String first = mockMvc.perform(post(url).with(webSession()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(first, "$.data.id");
        mockMvc.perform(post(url).with(webSession()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andExpect(jsonPath("$.data.id").value(id));
        return id;
    }

    /** 草拟中的合同（2026-08-01 起一年、月租 1000 元），返回合同编号。 */
    private String draft(String tag) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor(), "token", List.of()));
        try {
            return contractService.createDraft(new ContractDraftRequest(key("draft_" + tag), "mch_b16_" + suffix, subject, null, subject,
                    LocalDate.parse("2026-08-01"), LocalDate.parse("2027-07-31"), Map.of("baseRentCent", 100000),
                    "fixed_ratio", "none", null, Map.of(), List.of(), List.of(), projectId, "餐饮")).id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** 生效中的合同（押金月数快照 3）。 */
    private String effective(String tag) {
        String contract = draft(tag);
        jdbc.update("UPDATE contracts SET lifecycle_status = 'effective', snapshot = CAST(? AS jsonb) WHERE id = ?",
                "{\"depositMonths\": 3}", contract);
        return contract;
    }

    private String lines() {
        return LINE_CODES.stream()
                .map(code -> "{\"line_code\":\"" + code + "\",\"final_approver_id\":\"" + lineFinal.get(code) + "\"}")
                .collect(Collectors.joining(","));
    }

    private String contractStatus(String contractId) {
        return jdbc.queryForObject("SELECT lifecycle_status FROM contracts WHERE id = ?", String.class, contractId);
    }

    private int auditCount(String eventName, String businessId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND business_id = ?", Integer.class,
                eventName, businessId);
    }

    private int count(String fromWhere, String arg) {
        return jdbc.queryForObject("SELECT count(*) FROM " + fromWhere, Integer.class, arg);
    }

    private String user(String name) {
        String id = "usr_b16_" + name + "_" + suffix;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', ?, NULL, 'active', now())", id, id, projectId);
        return id;
    }

    private void superior(String userId, String superiorId) {
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", userId, superiorId);
    }

    private AccessActor actor() {
        return new AccessActor(drafter, drafter, "platform_admin", projectId, null, "active");
    }

    /** 起草人经后台 Web 终端（生产过滤器放入的 AccessActor 主体）。 */
    private RequestPostProcessor webSession() {
        AccessActor actor = actor();
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String key(String tag) {
        return "b16_" + tag + "_" + suffix;
    }
}
