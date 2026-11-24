package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
 * 第十八批 BY 段第 387 条 387-R2（第十八批裁决 81「岗位角色模板补存理由」）：岗位模板存档（lifecycle archive）后，含已存档列表带出
 * archived_by（存档时已写入）与 archive_reason；恢复（restore）后两项为空。真实入口、蛇形键；夹具随测试事务回滚；角色与模板名中文、编号后缀纯数字。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class PositionTemplateArchiveReasonJdbcIntegrationTests {

    private static final String LIST = "/api/v1/access/position-role-templates?include_archived=true";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));

    @Test
    void archiveCarriesArchivedByAndReasonAndRestoreClearsThem() throws Exception {
        String role = "role_r387pt_" + s;
        String template = "tpl_r387pt_" + s;
        String mapping = "prt_r387_" + s;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '岗位模板存档回归', 'project', 'active', now())", role, "岗位模板存档回归角色" + s);
        jdbc.update("INSERT INTO permission_templates(id,template_name,template_code,updated_at) VALUES(?,?,?,now())",
                template, "岗位模板存档回归模板" + s, template);
        jdbc.update("INSERT INTO position_role_templates (id, position_code, staff_level_id, role_id, template_id, organization_id,"
                + " status, created_by, created_at, updated_at) VALUES (?, ?, 'lvl_m3_upper', ?, ?, NULL, 'active', 'seed', now(), now())",
                mapping, "r387pos_" + s, role, template);
        String user = "usr_r387pt_" + s;
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff(user, "platform_admin", null, null, "access.role.assign");

        lifecycle(admin, mapping, "archive", "岗位撤并");
        Map<String, Object> archived = row(admin, mapping);
        assertThat(archived.get("archived_by")).isEqualTo(user);
        assertThat(archived.get("archive_reason")).isEqualTo("岗位撤并");

        lifecycle(admin, mapping, "restore", "岗位恢复");
        Map<String, Object> restored = row(admin, mapping);
        assertThat(restored.get("archived_by")).isNull();
        assertThat(restored.get("archive_reason")).isNull();
    }

    private void lifecycle(RequestPostProcessor who, String id, String action, String reason) throws Exception {
        mockMvc.perform(post("/api/v1/access/position-role-templates/" + id + "/lifecycle").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"r387pt-" + action + "-" + s + "\",\"action\":\"" + action + "\",\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());
    }

    private Map<String, Object> row(RequestPostProcessor who, String id) throws Exception {
        String body = mockMvc.perform(get(LIST).with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> rows = JsonPath.read(body, "$.data.items[?(@.id == '" + id + "')]");
        assertThat(rows).hasSize(1);
        return rows.get(0);
    }
}
