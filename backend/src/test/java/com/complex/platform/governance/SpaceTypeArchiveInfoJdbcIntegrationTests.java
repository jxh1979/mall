package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BY 段第 387 条 387-R2（第十八批裁决 81「空间类型补存存档时间、存档人、理由」）：带理由存档空间类型后，空间类型列表该行
 * archived_at 非空、archived_by 为操作人、archive_reason 为所填；取消存档后三项都为空；申请删除被最高权限驳回、回到已存档时三项原样保留
 * （387-R2「驳回删除、状态回到 archived 时保留不动」，18BY 收尾复核意见 3）。真实入口、蛇形键；夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SpaceTypeArchiveInfoJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));

    @Test
    void archiveRecordsTimeActorAndReasonAndUnarchiveClearsThem() throws Exception {
        String code = "r387st_" + s;
        String id = "sptype_" + code;
        jdbc.update("INSERT INTO space_type_dictionary (id, type_code, type_name, sort_order, status, created_at, updated_at)"
                + " VALUES (?, ?, '存档回归种类', 100, 'active', now(), now())", id, code);
        String user = "usr_r387st_" + s;
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff(user, "platform_admin", null, null,
                "governance.space.view", "governance.space.manage");

        mockMvc.perform(post("/api/v1/platform/space-types/" + id + "/archive").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"不再使用的种类\"}")).andExpect(status().isOk());
        Map<String, Object> archived = row(admin, id);
        assertThat(archived.get("archived_at")).isNotNull();
        assertThat(archived.get("archived_by")).isEqualTo(user);
        assertThat(archived.get("archive_reason")).isEqualTo("不再使用的种类");

        mockMvc.perform(post("/api/v1/platform/space-types/" + id + "/unarchive").with(admin)).andExpect(status().isOk());
        Map<String, Object> restored = row(admin, id);
        assertThat(restored.get("archived_at")).isNull();
        assertThat(restored.get("archived_by")).isNull();
        assertThat(restored.get("archive_reason")).isNull();
    }

    @Test
    void rejectedDeleteKeepsArchiveTimeActorAndReason() throws Exception {
        String code = "r387sr_" + s;
        String id = "sptype_" + code;
        jdbc.update("INSERT INTO space_type_dictionary (id, type_code, type_name, sort_order, status, created_at, updated_at)"
                + " VALUES (?, ?, '驳回回归种类', 100, 'active', now(), now())", id, code);
        String user = "usr_r387sr_" + s;
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff(user, "platform_admin", null, null,
                "governance.space.view", "governance.space.manage");
        String base = "/api/v1/platform/space-types/" + id;

        mockMvc.perform(post(base + "/archive").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"不再使用的种类\"}")).andExpect(status().isOk());
        Object archivedAt = row(admin, id).get("archived_at");
        assertThat(archivedAt).isNotNull();
        mockMvc.perform(post(base + "/delete-request").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"与别的种类合并\"}")).andExpect(status().isOk());
        mockMvc.perform(post(base + "/delete-approve").with(master()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"r387sr-" + s + "\",\"decision\":\"rejected\"}")).andExpect(status().isOk());

        Map<String, Object> rejected = row(admin, id);
        assertThat(rejected.get("status")).isEqualTo("archived");
        assertThat(rejected.get("archived_at")).isEqualTo(archivedAt);
        assertThat(rejected.get("archived_by")).isEqualTo(user);
        assertThat(rejected.get("archive_reason")).isEqualTo("不再使用的种类");
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private static RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private Map<String, Object> row(RequestPostProcessor who, String id) throws Exception {
        String body = mockMvc.perform(get("/api/v1/platform/space-types").with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> rows = JsonPath.read(body, "$.data.items[?(@.id == '" + id + "')]");
        assertThat(rows).hasSize(1);
        return rows.get(0);
    }
}
