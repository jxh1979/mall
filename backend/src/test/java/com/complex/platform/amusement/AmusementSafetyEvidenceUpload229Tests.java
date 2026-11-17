package com.complex.platform.amusement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AP 段第 229 条 229-R1（PGM:966「已拍板：所有有过程的业务动作都必须留痕……」；PGM:968「……人工只补原因和证据。……」）：
 * 游乐安全页（AmusementSafetyPage）删去写死的附件编号后，合格巡检改为先按 amusement_safety_inspection 真实上传、再带回附件编号新建；
 * 不带证据的合格巡检照旧被拒（AmusementInspectionService 合格必须带证据）。
 * 229-R6（PGM:973「……状态流转不得只记状态不记时间……」；PGM:846「……整改后复检、主管确认闭环……」；BSM:1269「工程师可在一体机上填写定期检修记录……」）：
 * 网页不再写死巡检时间（由服务端取提交时刻）、复检可登记不通过、检修来源如实记 web。
 * 服务端规则未改，本类按改后网页形状走真实 HTTP、真实授权链锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AmusementSafetyEvidenceUpload229Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String digits = String.valueOf(System.nanoTime());
    private final String project = "prj_a229_" + digits;
    private final String merchant = "mch_a229_" + digits;
    private final String amusement = "amu_a229_" + digits;
    private final String vendor = "sup_a229_" + digits;
    private final String template = "tpl_a229_" + digits;
    private RequestPostProcessor staff;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','游乐巡检综合体','active',now(),now())",
                project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                + " VALUES(?,'游乐巡检商户','amusement',?,'active',now(),now())", merchant, project);
        jdbc.update("INSERT INTO amusement_safety_templates (id, template_name, items_json, status, created_at, updated_at)"
                + " VALUES (?, '开放前巡检模板', '[{\"name\":\"guardrail\"}]'::jsonb, 'active', now(), now())", template);
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, safety_template_id, eligibility_rule_json, merchant_id,"
                + " created_at, updated_at) VALUES (?, '旋转木马', 10, 'open', ?, '{}'::jsonb, ?, now(), now())", amusement, template, merchant);
        jdbc.update("INSERT INTO inventory_suppliers (id, supplier_name, supplier_type, status, created_at, updated_at)"
                + " VALUES (?, '游乐检修公司', 'general', 'active', now(), now())", vendor);
        // 巡检、复检、检修与上传都走 currentMerchantScope：由该游乐项目所属商户的员工办理。
        staff = new StaffRequestFixture(jdbc).staff("usr_a229_" + digits, "staff", project, merchant,
                "amusement.safety_inspection.create", "amusement.safety_inspection.recheck", "amusement.maintenance.submit",
                "evidence.attachment.create");
    }

    // 229-R1：先真实上传（不带业务编号，巡检尚未建立），再带回附件编号新建合格巡检；不带证据的合格巡检 400。
    @Test
    void passInspectionCarriesUploadedEvidenceAndIsRejectedWithoutIt() throws Exception {
        String uploaded = mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "guardrail.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .param("business_type", "amusement_safety_inspection").param("attachment_role", "photo").with(staff))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String attachment = json.readTree(uploaded).path("data").path("id").asText();
        assertThat(attachment).startsWith("att_");

        create("a229-pass-" + digits, "pass", List.of(attachment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence_attachment_ids.length()").value(1))
                .andExpect(jsonPath("$.data.evidence_attachment_ids[0]").value(attachment));
        create("a229-pass-none-" + digits, "pass", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("SAFETY_INSPECTION_EVIDENCE_REQUIRED"));
    }

    // 229-R6：不带巡检时间新建，巡检时间取提交时刻。
    @Test
    void inspectionWithoutInspectedAtTakesSubmitTime() throws Exception {
        Instant before = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String body = create("a229-time-" + digits, "fail", null)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Instant after = Instant.now();
        String id = json.readTree(body).path("data").path("id").asText();
        Instant inspectedAt = jdbc.queryForObject("SELECT inspected_at FROM amusement_safety_inspections WHERE id = ?", Timestamp.class, id)
                .toInstant();
        assertThat(inspectedAt).isBetween(before, after);
    }

    // 229-R6：整改已提交的巡检复检不通过 → 200，整体结论回不通过、重开状态回待处理。
    @Test
    void recheckCanRecordFail() throws Exception {
        String inspection = "insp_a229_" + digits;
        jdbc.update("INSERT INTO amusement_safety_inspections (id, project_id, template_id, items_json, overall_result, inspection_status,"
                + " rectification_status, reopen_status, inspected_at, created_at, updated_at)"
                + " VALUES (?, ?, ?, '[{\"item_code\":\"guardrail\",\"result\":\"fail\"}]'::jsonb, 'fail', 'submitted', 'submitted',"
                + " 'pending', now(), now(), now())", inspection, amusement, template);

        mockMvc.perform(post("/api/v1/amusement/safety-inspections/" + inspection + "/recheck").with(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "a229-recheck-" + digits, "action_result", "fail"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overall_result").value("fail"))
                .andExpect(jsonPath("$.data.reopen_status").value("pending"));
    }

    // 229-R6：网页提交的定期检修来源记 web。
    @Test
    void maintenanceFromWebIsRecordedAsWeb() throws Exception {
        mockMvc.perform(post("/api/v1/amusement/maintenance-records").with(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "a229-maint-" + digits, "project_id", amusement,
                                "maintenance_vendor_id", vendor, "result", "pass", "source_terminal", "web"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source_terminal").value("web"));
    }

    private org.springframework.test.web.servlet.ResultActions create(String key, String overall, List<String> evidence) throws Exception {
        Map<String, Object> request = new HashMap<>(Map.of(
                "idempotency_key", key, "operator_note", "开放前安全巡检", "project_id", amusement, "template_id", template, "overall_result", overall,
                "items", List.of(Map.of("item_code", "guardrail", "item_name", "开放前护栏检查", "result", overall, "severity", "high"))));
        if (evidence != null) {
            request.put("evidence_attachment_ids", evidence);
        }
        return mockMvc.perform(post("/api/v1/amusement/safety-inspections").with(staff)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)));
    }
}
