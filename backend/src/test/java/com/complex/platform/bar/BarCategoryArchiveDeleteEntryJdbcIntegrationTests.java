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
import java.util.concurrent.ThreadLocalRandom;
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
 * 第十八批 AE 段第 201 条 201-R1（真库、真实入口）：酒吧商品分类的存档、取消存档、申请删除、审批删除按 OpenAPI 新补的
 * archiveBarProductCategory、unarchiveBarProductCategory、requestDeleteBarProductCategory、approveDeleteBarProductCategory
 * 声明的请求体（前端蛇形键 reason、decision）与状态码，经控制器与真实授权链提交。依据 BSM:569「就从各处的选择列表里隐藏，
 * 不再被新业务选用；但历史照样能查、能对账」、BSM:570「要删除，得先存档」「申请人不能审批自己的申请」、BSM:576「存档及删除都需要填写理由」。
 * 入口已存在，本类只锁现行为。成功事件随测试事务回滚；失败事件独立事务、不随测试回滚，一律按本次随机后缀的操作人查。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarCategoryArchiveDeleteEntryJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String project = "r201bar_prj_" + s;
    private final String bar = "r201bar_bar_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','酒吧分类存档回归综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'酒吧分类存档回归酒吧','bar','active',?,now(),now())",
                bar, project);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** 存档：reason 是契约必填，空请求体 400 VALIDATION_FAILED；带 reason 200，记存档人与存档时间；存档不写审计事件。 */
    @Test
    void archivingCategoryRequiresReasonAndRecordsArchiver() throws Exception {
        RequestPostProcessor manager = staff("mgr", "酒吧分类管理回归角色", "bar.category.manage");
        String id = createCategory(manager, "1");

        mockMvc.perform(post("/api/v1/bar/catalog/categories/" + id + "/archive").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        mockMvc.perform(post("/api/v1/bar/catalog/categories/" + id + "/archive").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"本季不再售卖\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archived_by").value(userId("mgr")))
                .andExpect(jsonPath("$.data.archived_at").isNotEmpty());

        assertThat(jdbc.queryForObject("SELECT archive_reason FROM bar_product_categories WHERE id = ?", String.class, id))
                .isEqualTo("本季不再售卖");
        assertThat(jdbc.queryForList("SELECT event_name FROM audit_events WHERE actor_id = ? AND business_type = 'bar_product_category'",
                        String.class, userId("mgr")))
                .as("存档不写审计事件（契约 not_applicable）；只有新建分类的 bar.category.saved")
                .containsExactly("bar.category.saved");
    }

    /**
     * 申请删除须先存档（400 BAR_CATEGORY_NOT_ARCHIVED）；存档后带 reason 申请 200 进入待审批；申请人审批自己的申请 403
     * BAR_CATALOG_SELF_APPROVE_FORBIDDEN；另一位持 bar.category.approve 的人 decision=approved 200 删除；删除后取消存档 404。
     */
    @Test
    void deleteRequestAndApprovalFollowContractAndRejectSelfApproval() throws Exception {
        RequestPostProcessor manager = staff("mgr", "酒吧分类管理回归角色", "bar.category.manage", "bar.category.approve");
        RequestPostProcessor approver = staff("apv", "酒吧分类审批回归角色", "bar.category.approve");
        String id = createCategory(manager, "2");

        mockMvc.perform(post("/api/v1/bar/catalog/categories/" + id + "/delete-request").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"测试遗留分类\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("BAR_CATEGORY_NOT_ARCHIVED"));
        mockMvc.perform(post("/api/v1/bar/catalog/categories/" + id + "/archive").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"测试遗留分类\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/bar/catalog/categories/" + id + "/delete-request").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"测试遗留分类\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.delete_status").value("pending"));

        mockMvc.perform(post("/api/v1/bar/catalog/categories/" + id + "/delete-approve").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"approved\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("BAR_CATALOG_SELF_APPROVE_FORBIDDEN"));
        mockMvc.perform(post("/api/v1/bar/catalog/categories/" + id + "/delete-approve").with(approver).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision").value("approved"))
                .andExpect(jsonPath("$.data.deleted").value(true));
        mockMvc.perform(post("/api/v1/bar/catalog/categories/" + id + "/unarchive").with(manager))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail_code").value("BAR_CATEGORY_NOT_FOUND"));

        assertThat(rows("bar.category.delete_request_failed", "mgr"))
                .containsExactly("failure|bar_product_category|bar.category.manage|BAR_CATEGORY_NOT_ARCHIVED");
        assertThat(rows("bar.category.delete_requested", "mgr"))
                .containsExactly("success|bar_product_category|bar.category.manage|");
        assertThat(rows("bar.category.delete_approve_failed", "mgr"))
                .containsExactly("failure|bar_product_category|bar.category.approve|BAR_CATALOG_SELF_APPROVE_FORBIDDEN");
        assertThat(rows("bar.category.delete_approved", "apv"))
                .containsExactly("success|bar_product_category|bar.category.approve|");
    }

    private String createCategory(RequestPostProcessor manager, String n) throws Exception {
        return json.readTree(mockMvc.perform(post("/api/v1/bar/catalog/categories").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bar_id\":\"" + bar + "\",\"category_name\":\"回归分类" + n + "\",\"sort_order\":1}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data/id").asText();
    }

    private List<String> rows(String event, String tag) {
        return jdbc.queryForList("""
                SELECT event_result || '|' || business_type || '|' || permission_code || '|' || coalesce(metadata->>'error_code', '')
                  FROM audit_events WHERE event_name = ? AND actor_id = ?""", String.class, event, userId(tag));
    }

    private String userId(String tag) {
        return "r201bar_" + tag + "_" + s;
    }

    /** 本酒吧员工（后台 Web 终端），经一个普通角色持有给定权限码。 */
    private RequestPostProcessor staff(String tag, String roleName, String... codes) {
        String userId = userId(tag);
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) VALUES (?, ?, 'staff', ?, ?, 'active', now())",
                userId, userId, project, bar);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '酒吧分类存档删除回归', 'project', 'active', now())",
                role, roleName + s);
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
