package com.complex.platform.access.org;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * 第十八批 AF 段第 198 条 198-R5（访问治理）：契约删去 upsertApprovalLine 内联请求体的 operator_note（OrgHierarchyService.doUpsertApprovalLine
 * 不读）、组织层级页（OrgHierarchyPage）保存平行审批线停发写死备注后，按停发后的网页形状——idempotency_key、line_name、min_start_rank、
 * entry_user_id、hr_final_approver_id、status——走真实 HTTP、真实授权链新建审批线。入口未改，本类只锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ApprovalLineUpsertWebShape198R5Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String digits = String.valueOf(System.nanoTime());

    // 本项目员工（持 access.role.assign）按网页形状新建平行审批线，起点人是本项目账号：200，库内各列照请求落库并留成功痕。
    @Test
    void upsertApprovalLine_webShapeWithoutOperatorNote_saved() throws Exception {
        String project = "prj_b52ws_" + digits;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','审批线形状综合体','active',now(),now())",
                project);
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        RequestPostProcessor manager = fixture.staff("usr_b52ws_mgr_" + digits, "staff", project, null, "access.role.assign");
        String entry = "usr_b52ws_entry_" + digits;
        fixture.staff(entry, "staff", project, null);
        String code = "b52ws_line_" + digits;

        mockMvc.perform(put("/api/v1/access/approval-lines/" + code).with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"approval-line-b52ws-" + digits + "\",\"line_name\":\"网页形状审批线\","
                                + "\"min_start_rank\":2,\"entry_user_id\":\"" + entry + "\",\"hr_final_approver_id\":null,\"status\":\"active\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.line_code").value(code))
                .andExpect(jsonPath("$.data.entry_user_id").value(entry));

        assertThat(jdbc.queryForMap("SELECT line_name, min_start_rank, entry_user_id, hr_final_approver_id, status FROM approval_lines"
                        + " WHERE line_code = ?", code))
                .containsEntry("line_name", "网页形状审批线")
                .containsEntry("min_start_rank", 2)
                .containsEntry("entry_user_id", entry)
                .containsEntry("hr_final_approver_id", null)
                .containsEntry("status", "active");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'access.approval_line.saved'"
                + " AND event_result = 'success' AND business_id = ?", Integer.class, code)).isEqualTo(1);
    }
}
