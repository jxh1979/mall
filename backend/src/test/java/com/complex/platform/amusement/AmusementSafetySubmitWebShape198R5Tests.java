package com.complex.platform.amusement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
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
 * 第十八批 AF 段第 198 条 198-R5（集成、游乐、合规与证据）：契约删去 AmusementSafetyActionRequest 的 operator_note
 * （AmusementInspectionService.submitSafetyInspectionInternal 只读 comment、attachment_ids、evidence_attachment_ids）、游乐安全页
 * （AmusementSafetyPage）提交巡检停发写死备注后，按停发后的网页形状——idempotency_key、attachment_ids、action_result、comment——
 * 走真实 HTTP、真实授权链提交巡检。attachment_ids 照网页现发的写死值（被读取，留给第 229 条）。入口未改，本类只锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AmusementSafetySubmitWebShape198R5Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String digits = String.valueOf(System.nanoTime());

    // 发现高风险项（fail）的巡检按网页形状提交：200，巡检转已提交、待整改，巡检说明取 comment，提交动作一条。
    @Test
    void submitSafetyInspection_webShapeWithoutOperatorNote_submitted() throws Exception {
        String project = "prj_b52ws_" + digits;
        String merchant = "mch_b52ws_" + digits;
        String template = "tpl_b52ws_" + digits;
        String amusement = "amu_b52ws_" + digits;
        String inspection = "insp_b52ws_" + digits;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','巡检形状综合体','active',now(),now())",
                project);
        jdbc.update("INSERT INTO amusement_safety_templates (id, template_name, items_json, status, created_at, updated_at)"
                + " VALUES (?, '巡检形状模板', '[{\"name\":\"restraint\"}]'::jsonb, 'active', now(), now())", template);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                + " VALUES(?,'巡检形状游乐商户','amusement',?,'active',now(),now())", merchant, project);
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, safety_template_id, eligibility_rule_json,"
                + " merchant_id, created_at, updated_at) VALUES (?, '巡检形状项目', 10, 'open', ?, '{}'::jsonb, ?, now(), now())",
                amusement, template, merchant);
        jdbc.update("INSERT INTO amusement_safety_inspections (id, project_id, template_id, items_json, overall_result, inspection_status,"
                + " rectification_status, reopen_status, inspected_at, created_at, updated_at)"
                + " VALUES (?, ?, ?, '[{\"name\":\"restraint\",\"result\":\"fail\"}]'::jsonb, 'fail', 'created', 'not_required',"
                + " 'not_required', now(), now(), now())", inspection, amusement, template);
        // 巡检提交走 currentMerchantScope：由该游乐项目所属商户的员工提交。
        RequestPostProcessor inspector = new StaffRequestFixture(jdbc)
                .staff("usr_b52ws_insp_" + digits, "staff", project, merchant, "amusement.safety_inspection.submit");
        String comment = "发现高风险项，进入整改";

        mockMvc.perform(post("/api/v1/amusement/safety-inspections/" + inspection + "/submit").with(inspector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"amusement-safety-submit-b52ws-" + digits + "\","
                                + "\"attachment_ids\":[\"att-amusement-safety-submit\"],\"action_result\":\"submitted\",\"comment\":\"" + comment + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inspection_status").value("submitted"))
                .andExpect(jsonPath("$.data.rectification_status").value("pending"));

        assertThat(jdbc.queryForMap("SELECT inspection_status, rectification_status, reopen_status, operator_note"
                        + " FROM amusement_safety_inspections WHERE id = ?", inspection))
                .containsEntry("inspection_status", "submitted")
                .containsEntry("rectification_status", "pending")
                .containsEntry("reopen_status", "pending")
                .containsEntry("operator_note", comment);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_safety_actions WHERE inspection_id = ? AND action_type = 'submit'"
                + " AND comment = ?", Integer.class, inspection, comment)).isEqualTo(1);
    }
}
