package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
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
 * 第十八批 N 段第 124 条 124-R5（真库）：PGM 第 966 行「已拍板：所有有过程的业务动作都必须留痕……」；PGM 第 738 行「访问被拒绝也要留痕，
 * 便于排查是权限问题还是开关问题。」。酒吧后台商品页保存分类、订单页维护退酒原因码，按前端蛇形键经控制器与真实授权链提交：成功落 OpenAPI
 * 声明的 x-audit-event-success（bar.category.saved、bar.void_reason_code.saved / archived），失败落 x-audit-event-failure
 * （bar.category.saved_failed）。成功事件随测试事务回滚；失败事件独立事务、不随测试回滚，一律按本次随机后缀的操作人查。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarCatalogWriteAuditJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "r5bar_prj_" + s;
    private final String bar = "r5bar_bar_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','酒吧目录留痕综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'酒吧目录留痕酒吧','bar','active',?,now(),now())",
                bar, project);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** 商品页「新建分类」：保存成功落 bar.category.saved（business_id 新分类编号）；分类名为空 400，落 bar.category.saved_failed。 */
    @Test
    void savingCategoryRecordsSavedAndAFailedSaveRecordsSavedFailed() throws Exception {
        RequestPostProcessor staff = staff("cat", "bar.category.manage");
        String id = json.readTree(mockMvc.perform(post("/api/v1/bar/catalog/categories").with(staff).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bar_id\":\"" + bar + "\",\"category_name\":\"精酿啤酒\",\"sort_order\":1,\"idempotency_key\":\"r5c1-" + s + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data/id").asText();
        assertThat(rows("bar.category.saved", "cat")).containsExactly("success|bar_product_category|" + id + "|bar.category.manage|");

        mockMvc.perform(post("/api/v1/bar/catalog/categories").with(staff).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bar_id\":\"" + bar + "\",\"category_name\":\"\",\"sort_order\":2,\"idempotency_key\":\"r5c2-" + s + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(rows("bar.category.saved_failed", "cat"))
                .containsExactly("failure|bar_product_category|new|bar.category.manage|VALIDATION_FAILED");
    }

    /** 订单页维护退酒原因码（新增、下线）：各落 bar.void_reason_code.saved / archived，business_id 原因码。 */
    @Test
    void maintainingVoidReasonCodesRecordsSavedAndArchived() throws Exception {
        RequestPostProcessor supervisor = staff("void", "bar.order_item.void_decide");
        String code = "r5_" + s;
        mockMvc.perform(post("/api/v1/bar/void-reason-codes").with(supervisor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"r5v1-" + s + "\",\"code\":\"" + code + "\",\"label_zh\":\"留痕回归原因" + s + "\",\"sort_order\":90}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/bar/void-reason-codes/" + code + "/archive").with(supervisor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"r5v2-" + s + "\",\"archive_reason\":\"不再使用\"}"))
                .andExpect(status().isOk());

        assertThat(rows("bar.void_reason_code.saved", "void"))
                .containsExactly("success|bar_void_reason_code|" + code + "|bar.order_item.void_decide|");
        assertThat(rows("bar.void_reason_code.archived", "void"))
                .containsExactly("success|bar_void_reason_code|" + code + "|bar.order_item.void_decide|");
    }

    private List<String> rows(String event, String tag) {
        return jdbc.queryForList("""
                SELECT event_result || '|' || business_type || '|' || business_id || '|' || permission_code || '|'
                       || coalesce(metadata->>'error_code', '')
                  FROM audit_events WHERE event_name = ? AND actor_id = ?""", String.class, event, userId(tag));
    }

    private String userId(String tag) {
        return "r5bar_" + tag + "_" + s;
    }

    /** 本酒吧员工（后台 Web 终端），经一个普通角色持有给定权限码。 */
    private RequestPostProcessor staff(String tag, String... codes) {
        String userId = userId(tag);
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) VALUES (?, ?, 'staff', ?, ?, 'active', now())",
                userId, userId, project, bar);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '酒吧目录留痕回归', 'project', 'active', now())",
                role, "酒吧目录留痕回归角色" + userId);
        for (String code : codes) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, bar, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
