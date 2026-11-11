package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AE 段第 201 条 201-R3（BSM:569「存档（先下线）」；BSM:576「存档及删除都需要填写理由」「删除一律须由具备最高权限的人审批通过后执行，
 * 不设例外，申请人不得审批自己的申请」）：空间种类存档与删除入口补进 OpenAPI 后，按契约声明的请求体（蛇形键）与状态码走真实 HTTP、真实授权链，
 * 锁住现行为——入口已存在，本类只锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SpaceTypeArchiveDeleteEntryJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String digits = String.valueOf(System.nanoTime());
    private final String typeId = "sptype_b51r3_" + digits;
    private RequestPostProcessor platformAdmin;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO space_type_dictionary(id,type_code,type_name,sort_order,status,created_at,updated_at) "
                + "VALUES(?,?,?,100,'active',now(),now())", typeId, "b51r3_" + digits, "回归空间种类" + digits);
        String user = "b51r3_admin_" + digits;
        String role = "role_b51r3_" + digits;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'空间种类入口回归','project','active',now())",
                role, "空间种类管理" + digits);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'governance.space.manage')", role);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) "
                + "VALUES(?,?,'platform_admin',NULL,NULL,'active',now())", user, user);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, "platform_admin", null, null, "active");
        platformAdmin = request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    // 审批删除：控制器不校权限码，服务层要求最高权限——平台管理员（不是最高权限）带 decision=approved 403 SPACE_TYPE_DELETE_APPROVER_NOT_MASTER，种类仍待审批删除。
    @Test
    void approveDelete_byPlatformAdminNotMaster_forbidden403_stillPending() throws Exception {
        jdbc.update("UPDATE space_type_dictionary SET status = 'pending_delete' WHERE id = ?", typeId);
        jdbc.update("INSERT INTO space_type_delete_requests(id,space_type_id,reason,requested_by,requested_at,status) "
                + "VALUES(?,?,'不再使用',?,now(),'pending')", "stdr_b51r3_" + digits, typeId, "b51r3_req_" + digits);

        send(post(url("/delete-approve")), "{\"decision\":\"approved\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SPACE_TYPE_DELETE_APPROVER_NOT_MASTER"));
        assertThat(statusOf()).isEqualTo("pending_delete");
    }

    // 直删入口已停用：存在的种类恒 400 SPACE_TYPE_DELETE_REQUIRES_APPROVAL、数据不变；不存在的 404 SPACE_TYPE_NOT_FOUND。
    @Test
    void directDelete_alwaysRejected400RequiresApproval_rowKept() throws Exception {
        mockMvc.perform(delete(url("")).with(platformAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("SPACE_TYPE_DELETE_REQUIRES_APPROVAL"));
        assertThat(statusOf()).isEqualTo("active");
        mockMvc.perform(delete("/api/v1/platform/space-types/sptype_b51r3_none_" + digits).with(platformAdmin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("SPACE_TYPE_NOT_FOUND"));
    }

    // 存档：reason 必填，空请求体 400 VALIDATION_FAILED、种类不变；带 reason 200，种类转已存档并落 space_type.archived。
    @Test
    void archive_reasonRequired400_thenArchived200WithEvent() throws Exception {
        send(post(url("/archive")), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(statusOf()).isEqualTo("active");

        send(post(url("/archive")), "{\"reason\":\"配错了先下线\"}").andExpect(status().isOk());
        assertThat(statusOf()).isEqualTo("archived");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='space_type.archived' AND business_id=?",
                Integer.class, typeId)).isEqualTo(1);
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body)
            throws Exception {
        return mockMvc.perform(request.with(platformAdmin).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String statusOf() {
        return jdbc.queryForObject("SELECT status FROM space_type_dictionary WHERE id = ?", String.class, typeId);
    }

    private String url(String tail) {
        return "/api/v1/platform/space-types/" + typeId + tail;
    }
}
