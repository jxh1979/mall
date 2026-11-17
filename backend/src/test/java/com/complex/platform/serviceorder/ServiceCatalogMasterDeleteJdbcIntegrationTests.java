package com.complex.platform.serviceorder;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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

/**
 * 第十八批 AO 段第 249 条 249-R6（真库、真实入口、真实授权链）：服务目录删除审批只认最高权限本人（BSM:576「删除一律须由具备最高权限的人
 * 审批通过后执行，不设例外」），删前查点单占用、不分点单状态（BSM:572「只要还有占用就拦住不让删，避免删出一堆「找不到主人」的记录」），
 * 最高权限本人发起即删除（BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」）；最高权限发起遇占用 409 时整笔回滚，
 * 目录项不会卡在本人发起的待审批状态。不开测试事务，才看得到回滚；角色名中文、编号后缀纯数字。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class ServiceCatalogMasterDeleteJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String project = "r249svc_prj_" + s;
    private final String requester = "usr_r249svc_req_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','服务目录删除回归综合体','active',now(),now())", project);
    }

    /** 非最高权限审批 403 MASTER_APPROVAL_REQUIRED 并留失败痕；最高权限审批遇已完成点单引用 409 SERVICE_CATALOG_IN_USE，仍待审批。 */
    @Test
    void onlyMasterMayApproveAndItemStillOrderedIsKept() throws Exception {
        String item = item("pending", true);
        order(item);
        RequestPostProcessor approver = new StaffRequestFixture(jdbc).staff("usr_r249svc_apv_" + s, "staff", project, null, "service.catalog.approve");

        send(approver, item, "delete-approve", "{\"decision\":\"approved\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MASTER_APPROVAL_REQUIRED"));
        assertThat(jdbc.queryForList("SELECT metadata->>'error_code' FROM audit_events WHERE event_name = 'service.catalog.delete_approve_failed'"
                + " AND actor_id = ? AND business_id = ?", String.class, "usr_r249svc_apv_" + s, item))
                .containsExactly("MASTER_APPROVAL_REQUIRED");

        send(master(), item, "delete-approve", "{\"decision\":\"approved\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SERVICE_CATALOG_IN_USE"));
        assertThat(deleteStatus(item)).isEqualTo("pending");
    }

    /** 最高权限本人对没被点单引用的已存档目录项申请删除：当场删除，目录里不再出现，申请与批准各留一条痕。 */
    @Test
    void masterDeleteRequestTakesEffectAtOnce() throws Exception {
        String item = item("direct", false);

        send(master(), item, "delete-request", "{\"reason\":\"停售的服务\"}").andExpect(status().isOk());

        assertThat(catalogIds()).doesNotContain(item);
        assertThat(deleteStatus(item)).isEqualTo("approved");
        assertThat(events(item)).containsExactlyInAnyOrder("service.catalog.delete_requested", "service.catalog.delete_approved");
    }

    /** 最高权限本人对仍被点单引用的已存档目录项申请删除：409，整笔回滚——不留待审批、不留申请成功痕，只留一条申请失败痕。 */
    @Test
    void masterDeleteRequestOnItemStillOrderedIsRejectedAndRolledBack() throws Exception {
        String item = item("ordered", false);
        order(item);

        send(master(), item, "delete-request", "{\"reason\":\"停售的服务\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SERVICE_CATALOG_IN_USE"));

        List<String> statuses = JsonPath.read(catalogBody(), "$.data.items[?(@.id == '" + item + "')].delete_status");
        assertThat(statuses).containsExactly("none");
        assertThat(events(item)).containsExactly("service.catalog.delete_request_failed");
    }

    // ---------- 夹具 ----------

    /** 已存档的服务目录项；pending=true 时另一位员工已申请删除、待审批。 */
    private String item(String tag, boolean pending) {
        String id = "svc_r249_" + tag + "_" + s;
        jdbc.update("INSERT INTO service_catalog_items (id, name, price_cent, duration_minutes, active, mgmt_fee_mode, mgmt_fee_value,"
                + " archived_at, archived_by, archive_reason, delete_status, delete_requested_by, delete_requested_at, delete_reason)"
                + " VALUES (?, ?, 10000, 10, TRUE, 'none', 0, now(), ?, '停售', CASE WHEN ? THEN 'pending' ELSE 'none' END,"
                + " CASE WHEN ? THEN ? END, CASE WHEN ? THEN now() END, CASE WHEN ? THEN '下架' END)",
                id, "回归服务" + tag, requester, pending, pending, requester, pending, pending);
        return id;
    }

    /** 一张已完成的点单引用该目录项（不分点单状态都算占用）。 */
    private void order(String item) {
        jdbc.update("INSERT INTO service_orders (id, catalog_item_id, item_name, price_cent, performer_user_id, status, created_at, updated_at,"
                + " completed_at, paid_at, project_id) VALUES (?, ?, '回归服务', 10000, ?, 'completed', now(), now(), now(), now(), ?)",
                "so_r249_" + item, item, "usr_r249svc_perf_" + s, project);
    }

    // ---------- 请求与断言 ----------

    private ResultActions send(RequestPostProcessor who, String item, String action, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/services/catalog/" + item + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String catalogBody() throws Exception {
        return mockMvc.perform(get("/api/v1/services/catalog/all").with(master())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<String> catalogIds() throws Exception {
        return JsonPath.read(catalogBody(), "$.data.items[*].id");
    }

    private String deleteStatus(String item) {
        return jdbc.queryForObject("SELECT delete_status FROM service_catalog_items WHERE id = ?", String.class, item);
    }

    private List<String> events(String item) {
        return jdbc.queryForList("SELECT event_name FROM audit_events WHERE actor_id = ? AND business_id = ? AND event_name LIKE 'service.catalog.delete_%'",
                String.class, SessionIssuer.MASTER_USER_ID, item);
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
