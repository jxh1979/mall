package com.complex.platform.ticketing;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AP 段第 229 条 229-R7（BSM:782「……动态价按时段、容量等规则配置，可先试算再发布。」；PGM:966「已拍板：所有有过程的业务动作都必须留痕……」）：
 * 动态定价页（DynamicPricingPage）停发写死的票种、生效期、价格上下限、条件、备注与附件后，按网页新形状走真实 HTTP、真实授权链：
 * 草稿取所选票种与所填生效期、不带条件 → 条件为空、价格上下限为空；试算不带 context → 命中新规则；发布原因「后台发布」→ 草稿无备注时落规则备注、附件为空。
 * 服务端未改，本类只锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DynamicPricingWebShape229Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = String.valueOf(System.nanoTime());

    @Test
    void webShapeDraftWithoutConditionIsMatchedBySimulationAndPublishes() throws Exception {
        String project = "prj_d229_" + s;
        String ticketType = "tt_d229_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '动态定价形状综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,"
                + " holiday_price_cent, stock_required, status, validity_days, created_at, updated_at)"
                + " VALUES (?, '动态定价形状门票', 'flat', 10000, 10000, 10000, FALSE, 'draft', 1, now(), now())", ticketType);
        RequestPostProcessor manager = new StaffRequestFixture(jdbc).staff("usr_d229_" + s, "staff", project, null,
                "ticket.dynamic_pricing.manage", "ticket.dynamic_pricing.simulate", "ticket.dynamic_pricing.publish");

        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("idempotency_key", "d229-rule-" + s);
        draft.put("ticket_type_id", ticketType);
        draft.put("rule_name", "周末上浮");
        draft.put("priority", 10);
        draft.put("effective_from", "2026-12-01");
        draft.put("effective_to", "2026-12-31");
        draft.put("sales_channel", "web");
        draft.put("adjustment_type", "percent_delta");
        draft.put("adjustment_rate", 10);
        JsonNode rule = call(manager, "/api/v1/ticketing/dynamic-pricing-rules", draft);
        String ruleId = rule.path("id").asText();
        assertThat(rule.path("ticket_type_id").asText()).isEqualTo(ticketType);
        assertThat(rule.path("status").asText()).isEqualTo("draft");
        assertThat(jdbc.queryForMap("SELECT min_price_cent, max_price_cent, condition_rule_json::text AS condition, operator_note"
                        + " FROM ticket_dynamic_pricing_rules WHERE id = ?", ruleId))
                .containsEntry("min_price_cent", null)
                .containsEntry("max_price_cent", null)
                .containsEntry("condition", "{}")
                .containsEntry("operator_note", null);

        // 2026-12-15 为周二，取平日价 10000 分，上浮 10%。
        JsonNode simulation = call(manager, "/api/v1/ticketing/dynamic-pricing/simulations", Map.of(
                "idempotency_key", "d229-sim-" + s, "ticket_type_id", ticketType, "visit_date", "2026-12-15", "sales_channel", "web"));
        assertThat(simulation.path("matched_rule_id").asText()).isEqualTo(ruleId);
        assertThat(simulation.path("final_price_cent").asLong()).isEqualTo(11000L);

        JsonNode published = call(manager, "/api/v1/ticketing/dynamic-pricing-rules/" + ruleId + "/publish", Map.of(
                "idempotency_key", "d229-pub-" + s, "action", "publish", "reason", "后台发布"));
        assertThat(published.path("status").asText()).isEqualTo("published");
        assertThat(published.path("operator_note").asText()).isEqualTo("后台发布");
        assertThat(published.path("evidence_attachment_ids").size()).isZero();
    }

    /**
     * 325-R1（文档与代码一致；矩阵同类第 304、313 条先例；控制席并入：矩阵各行权限码只跟现码、不新加权限）：页面矩阵 permissions 列每个码都是现行权限码
     * （码后括注如「(导出按钮)」是说明、不是码）；ADM-TKT-002 行列出页面实调的建规则接口 POST /api/v1/ticketing/dynamic-pricing-rules 与其
     * ticket.dynamic_pricing.manage，动作与状态流照现码（规则只有草稿→发布，没有回滚接口）。只有 ticket.dynamic_pricing.view 的账号建规则 403。
     */
    @Test
    void pageMatrixPermissionsAreActiveCodesAndDynamicPricingRowFollowsCode() throws Exception {
        Path matrix = Path.of("..", "docs", "full-admin-page-permission-field-matrix-v0.1.csv");
        if (!Files.exists(matrix)) {
            matrix = Path.of("docs", "full-admin-page-permission-field-matrix-v0.1.csv");
        }
        List<String> rows = Files.readAllLines(matrix, StandardCharsets.UTF_8);
        List<String> missing = new ArrayList<>();
        for (String row : rows.subList(1, rows.size())) {
            if (row.isBlank()) {
                continue;
            }
            String[] cols = row.split(",", -1);
            for (String code : cols[6].split(";")) {
                int note = code.indexOf('(');
                String bare = (note < 0 ? code : code.substring(0, note)).trim();
                if (!bare.isEmpty() && jdbc.queryForObject("SELECT count(*) FROM permissions WHERE permission_code = ? AND status = 'active'",
                        Integer.class, bare) != 1) {
                    missing.add(cols[0] + ":" + bare);
                }
            }
        }
        assertThat(missing).as("矩阵里不是现行权限码的").isEmpty();

        String[] dynamicPricing = rows.stream().filter(row -> row.startsWith("ADM-TKT-002,")).findFirst().orElseThrow().split(",", -1);
        assertThat(dynamicPricing[6].split(";")).containsExactly("ticket.dynamic_pricing.view", "ticket.dynamic_pricing.manage",
                "ticket.dynamic_pricing.simulate", "ticket.dynamic_pricing.publish", "ticket.product.view");
        assertThat(dynamicPricing[7].split(";")).contains("POST /api/v1/ticketing/dynamic-pricing-rules");
        assertThat(dynamicPricing[8]).doesNotContain("回滚");
        assertThat(dynamicPricing[10]).isEqualTo("draft->published");

        String project = "prj_d325_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '动态定价矩阵综合体', 'active', now(), now())", project);
        RequestPostProcessor viewer = new StaffRequestFixture(jdbc).staff("usr_d325_" + s, "staff", project, null,
                "ticket.dynamic_pricing.view");
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("idempotency_key", "d325-rule-" + s);
        draft.put("ticket_type_id", "tt_d325_" + s);
        draft.put("rule_name", "周末上浮");
        draft.put("adjustment_type", "percent_delta");
        draft.put("adjustment_rate", 10);
        mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing-rules").with(viewer)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(draft)))
                .andExpect(status().isForbidden());
    }

    private JsonNode call(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post(path).with(who)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(response).path("data");
    }
}
