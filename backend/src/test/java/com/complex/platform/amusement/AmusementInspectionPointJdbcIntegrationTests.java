package com.complex.platform.amusement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BS 段第 373 条 373-R1、373-R5（BSM:1300「巡检必须按点位顺序完成，缺一个点位、上一个点位就不能通过；未走完全部点位的巡检不成立、
 * 不得标记为已完成」；决策台账 DEC-R3-2026-08-04-33；第十八批裁决 76「逐点扫码」；PGM:528「④看不到别的商户和别的项目的数据」）：
 * 游乐项目巡检点位清单、按序扫码走点、未走完的巡检建不成。真库＋真实 HTTP＋真实授权链，请求体蛇形键（夹具同 AmusementSafetyEvidenceUpload229Tests）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AmusementInspectionPointJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String digits = String.valueOf(System.nanoTime());
    private final String project = "prj_a373_" + digits;
    private final String otherComplex = "prj_a373o_" + digits;
    private final String merchant = "mch_a373_" + digits;
    private final String otherMerchant = "mch_a373m_" + digits;
    private final String amusement = "amu_a373_" + digits;
    private final String plainAmusement = "amu_a373p_" + digits;
    private RequestPostProcessor staff;
    private RequestPostProcessor noManage;
    private RequestPostProcessor otherMerchantStaff;
    private RequestPostProcessor otherComplexStaff;
    private int keys;

    @BeforeEach
    void seed() {
        for (String p : List.of(project, otherComplex)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                    p, "游乐点位综合体" + p);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                + " VALUES(?,'游乐点位商户','amusement',?,'active',now(),now())", merchant, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                + " VALUES(?,'别家游乐商户','amusement',?,'active',now(),now())", otherMerchant, project);
        for (String id : List.of(amusement, plainAmusement)) {
            jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, eligibility_rule_json, merchant_id,"
                    + " created_at, updated_at) VALUES (?, '过山车', 10, 'open', '{}'::jsonb, ?, now(), now())", id, merchant);
        }
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        staff = fixture.staff("usr_a373_" + digits, "staff", project, merchant,
                "amusement.safety_inspection.create", "amusement.safety_inspection.view", "amusement.safety_template.manage",
                "evidence.attachment.create", "amusement.safety_inspection.submit");
        noManage = fixture.staff("usr_a373n_" + digits, "staff", project, merchant,
                "amusement.safety_inspection.create", "amusement.safety_inspection.view");
        otherMerchantStaff = fixture.staff("usr_a373m_" + digits, "staff", project, otherMerchant,
                "amusement.safety_inspection.create", "amusement.safety_inspection.view", "amusement.safety_template.manage");
        otherComplexStaff = fixture.staff("usr_a373o_" + digits, "staff", otherComplex, null,
                "amusement.safety_inspection.create", "amusement.safety_inspection.view", "amusement.safety_template.manage");
    }

    // (1) 配置清单：200 后读回三个点位（序号 1..3、各带点位码）、走点为空；没有安检模板管理权限、别的商户、别的综合体的公司员工都 403 且库不变。
    @Test
    void pointListIsConfiguredOnlyWithinScope() throws Exception {
        replace(staff, List.of("入口闸机", "安全压杠", "出口通道")).andExpect(status().isOk());
        mockMvc.perform(get(base()).with(staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(amusement))
                .andExpect(jsonPath("$.data.points.length()").value(3))
                .andExpect(jsonPath("$.data.points[0].seq").value(1))
                .andExpect(jsonPath("$.data.points[0].point_name").value("入口闸机"))
                .andExpect(jsonPath("$.data.points[2].seq").value(3))
                .andExpect(jsonPath("$.data.points[2].point_name").value("出口通道"))
                .andExpect(jsonPath("$.data.walk.length()").value(0));
        String stored = storedPoints();

        replace(noManage, List.of("改过的点位")).andExpect(status().isForbidden());
        replace(otherMerchantStaff, List.of("改过的点位"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        replace(otherComplexStaff, List.of("改过的点位")).andExpect(status().isForbidden());
        mockMvc.perform(get(base()).with(otherComplexStaff)).andExpect(status().isForbidden());
        assertThat(storedPoints()).isEqualTo(stored);
    }

    // (2) 按序走点：先记第 2 个 409 且无行；按 1、2 记 200、走点两行带记录时间与记录人；换键再记第 2 个 409；同键重发回同一结果、不多写。
    @Test
    void pointsAreCheckedInOrderOnce() throws Exception {
        List<String> codes = configure("入口闸机", "安全压杠", "出口通道");
        check(2, codes.get(1), "pass", "k-early")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INSPECTION_PREVIOUS_POINT_INCOMPLETE"));
        assertThat(walkRows()).isZero();

        check(1, codes.get(0), "pass", "k1").andExpect(status().isOk());
        check(2, codes.get(1), "pass", "k2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walk.length()").value(2))
                .andExpect(jsonPath("$.data.walk[1].seq").value(2))
                .andExpect(jsonPath("$.data.walk[1].point_name").value("安全压杠"))
                .andExpect(jsonPath("$.data.walk[1].checked_by").value("usr_a373_" + digits))
                .andExpect(jsonPath("$.data.walk[1].checked_at").isNotEmpty());
        check(2, codes.get(1), "pass", "k2-again")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INSPECTION_POINT_ALREADY_CHECKED"));
        check(2, codes.get(1), "pass", "k2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walk.length()").value(2));
        assertThat(walkRows()).isEqualTo(2);
    }

    // (3) 只走完 1、2 时建合格巡检（不带或带了手填检查项）都 409、无新巡检；记完第 3 个后同样请求 200，检查项三条按序、名称即点位名，走点清空；
    // 再走完一轮后带手填检查项建巡检，检查项仍取点位（请求里的 items 不看；PGM:744「前端隐藏只是辅助，后端接口必须独立校验。」）。
    @Test
    void inspectionNeedsEveryPointWalked() throws Exception {
        List<String> codes = configure("入口闸机", "安全压杠", "出口通道");
        String evidence = upload();
        check(1, codes.get(0), "pass", "w1").andExpect(status().isOk());
        check(2, codes.get(1), "pass", "w2").andExpect(status().isOk());
        createWithoutItems("c-incomplete", evidence)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INSPECTION_POINTS_INCOMPLETE"));
        createWithItems("c-incomplete-items", evidence)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INSPECTION_POINTS_INCOMPLETE"));
        assertThat(inspections()).isZero();

        check(3, codes.get(2), "pass", "w3").andExpect(status().isOk());
        createWithoutItems("c-complete", evidence)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].item_name").value("入口闸机"))
                .andExpect(jsonPath("$.data.items[1].item_name").value("安全压杠"))
                .andExpect(jsonPath("$.data.items[2].item_name").value("出口通道"))
                .andExpect(jsonPath("$.data.items[2].result").value("pass"))
                .andExpect(jsonPath("$.data.items[2].checked_at").isNotEmpty());
        assertThat(inspections()).isEqualTo(1);
        assertThat(walkRows()).isZero();

        walkAll(codes, "again");
        createWithItems("c-complete-items", evidence)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].item_name").value("入口闸机"))
                .andExpect(jsonPath("$.data.items[2].item_name").value("出口通道"));
        assertThat(inspections()).isEqualTo(2);
    }

    // (4) 有一个点位不通过而总体判合格：409 SAFETY_INSPECTION_PASS_CONFLICTS_ITEMS；带了全部通过的手填检查项也照点位记录判，409、无新巡检。
    @Test
    void failedPointConflictsWithPass() throws Exception {
        List<String> codes = configure("入口闸机", "安全压杠");
        String evidence = upload();
        check(1, codes.get(0), "pass", "f1").andExpect(status().isOk());
        check(2, codes.get(1), "fail", "f2").andExpect(status().isOk());
        createWithoutItems("c-conflict", evidence)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SAFETY_INSPECTION_PASS_CONFLICTS_ITEMS"));
        createWithItems("c-conflict-items", evidence)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SAFETY_INSPECTION_PASS_CONFLICTS_ITEMS"));
        assertThat(inspections()).isZero();
    }

    // (5) 上一个闭店时点之前记的走点不算本轮：读回为空，接着记第 2 个 409，建巡检 409。
    @Test
    void walkBeforeLastCloseDoesNotCount() throws Exception {
        List<String> codes = configure("入口闸机", "安全压杠", "出口通道");
        check(1, codes.get(0), "pass", "s1").andExpect(status().isOk());
        check(2, codes.get(1), "pass", "s2").andExpect(status().isOk());
        jdbc.update("UPDATE amusement_inspection_walk_points SET checked_at = now() - interval '2 days' WHERE project_id = ?", amusement);
        mockMvc.perform(get(base()).with(staff)).andExpect(status().isOk()).andExpect(jsonPath("$.data.walk.length()").value(0));
        check(3, codes.get(2), "pass", "s3")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INSPECTION_PREVIOUS_POINT_INCOMPLETE"));
        createWithoutItems("c-stale", upload())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INSPECTION_POINTS_INCOMPLETE"));
    }

    // (6) 走到一半换清单：走点清空。
    @Test
    void replacingPointListClearsWalk() throws Exception {
        List<String> codes = configure("入口闸机", "安全压杠");
        check(1, codes.get(0), "pass", "r1").andExpect(status().isOk());
        replace(staff, List.of("入口闸机", "安全压杠", "出口通道"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walk.length()").value(0));
        assertThat(walkRows()).isZero();
    }

    // (7) 没配点位的游乐项目：带检查项的巡检照旧 200。
    @Test
    void projectWithoutPointsKeepsItems() throws Exception {
        Map<String, Object> request = new HashMap<>(Map.of(
                "idempotency_key", key("plain"), "project_id", plainAmusement, "overall_result", "fail",
                "items", List.of(Map.of("item_code", "guardrail", "item_name", "开放前护栏检查", "result", "fail", "severity", "high"))));
        mockMvc.perform(post("/api/v1/amusement/safety-inspections").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].item_name").value("开放前护栏检查"));
    }

    // (8) 每一次巡检（含营业中抽检）都要重新按序走完：建成一次后不重走再建 409、无新行；重新走完三个点位后 200。
    @Test
    void everyInspectionNeedsAFreshWalk() throws Exception {
        List<String> codes = configure("入口闸机", "安全压杠", "出口通道");
        String evidence = upload();
        walkAll(codes, "first");
        createWithoutItems("c-open", evidence).andExpect(status().isOk());
        createWithoutItems("c-spot", evidence)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INSPECTION_POINTS_INCOMPLETE"));
        assertThat(inspections()).isEqualTo(1);
        walkAll(codes, "second");
        createWithoutItems("c-spot-2", evidence).andExpect(status().isOk());
        assertThat(inspections()).isEqualTo(2);
    }

    // 373-R5（第十八批裁决 76「逐点扫码」）：点位码随清单生成；名称重复 400；扫到别的点位的码 409 且不写；
    // 在清单末尾加点位，原有点位的码不变、新点位另有码。
    @Test
    void pointCodesMustMatchTheScannedPoint() throws Exception {
        List<String> codes = configure("入口闸机", "安全压杠");
        assertThat(codes).allSatisfy(code -> assertThat(code).matches("[A-Z0-9]{12}"));
        assertThat(codes.get(0)).isNotEqualTo(codes.get(1));
        replace(staff, List.of("入口闸机", "入口闸机"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        check(1, codes.get(1), "pass", "m1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INSPECTION_POINT_CODE_MISMATCH"));
        assertThat(walkRows()).isZero();
        check(1, codes.get(0), "pass", "m1-ok").andExpect(status().isOk());

        List<String> after = codes(replace(staff, List.of("入口闸机", "安全压杠", "出口通道")).andExpect(status().isOk()));
        assertThat(after.subList(0, 2)).isEqualTo(codes);
        assertThat(after.get(2)).matches("[A-Z0-9]{12}").isNotIn(codes);
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private String base() {
        return "/api/v1/amusement/projects/" + amusement + "/inspection-points";
    }

    private String key(String tag) {
        return "a373-" + tag + "-" + digits + "-" + (keys++);
    }

    private ResultActions replace(RequestPostProcessor who, List<String> points) throws Exception {
        return mockMvc.perform(put(base()).with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", key("points"), "points", points))));
    }

    private List<String> configure(String... names) throws Exception {
        return codes(replace(staff, List.of(names)).andExpect(status().isOk()));
    }

    private List<String> codes(ResultActions result) throws Exception {
        JsonNode points = json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data").path("points");
        List<String> codes = new java.util.ArrayList<>();
        points.forEach(point -> codes.add(point.path("point_code").asText()));
        return codes;
    }

    private ResultActions check(int seq, String code, String result, String tag) throws Exception {
        return mockMvc.perform(post(base() + "/" + seq + "/check").with(staff).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "a373-check-" + tag + "-" + digits,
                        "point_code", code, "result", result))));
    }

    private void walkAll(List<String> codes, String round) throws Exception {
        for (int i = 0; i < codes.size(); i++) {
            check(i + 1, codes.get(i), "pass", round + "-" + (i + 1)).andExpect(status().isOk());
        }
    }

    private ResultActions createWithoutItems(String tag, String evidence) throws Exception {
        return mockMvc.perform(post("/api/v1/amusement/safety-inspections").with(staff).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", key(tag), "project_id", amusement,
                        "overall_result", "pass", "evidence_attachment_ids", List.of(evidence)))));
    }

    // 走点进度读失败或开着旧版页面时，网页会照旧带手填检查项（全部通过、总体合格）。
    private ResultActions createWithItems(String tag, String evidence) throws Exception {
        return mockMvc.perform(post("/api/v1/amusement/safety-inspections").with(staff).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", key(tag), "project_id", amusement,
                        "overall_result", "pass", "evidence_attachment_ids", List.of(evidence),
                        "items", List.of(Map.of("item_code", "manual", "item_name", "手填检查项", "result", "pass"))))));
    }

    private String upload() throws Exception {
        String uploaded = mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "point.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .param("business_type", "amusement_safety_inspection").param("attachment_role", "photo").with(staff))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(uploaded).path("data").path("id").asText();
    }

    private String storedPoints() {
        return jdbc.queryForObject("SELECT inspection_points::text FROM amusement_projects WHERE id = ?", String.class, amusement);
    }

    private int walkRows() {
        return jdbc.queryForObject("SELECT count(*) FROM amusement_inspection_walk_points WHERE project_id = ?", Integer.class, amusement);
    }

    private int inspections() {
        return jdbc.queryForObject("SELECT count(*) FROM amusement_safety_inspections WHERE project_id = ?", Integer.class, amusement);
    }
}
