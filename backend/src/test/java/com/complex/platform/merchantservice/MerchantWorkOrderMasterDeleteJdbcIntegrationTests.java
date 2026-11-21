package com.complex.platform.merchantservice;

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
 * 第十八批 BY 段第 383 条 383-R1（真库、真实入口、真实授权链）：商户服务工单删除只认最高权限本人（BSM:577「删除一律须由具备最高权限的人
 * 审批通过后执行，不设例外，申请人不得审批自己的申请；持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」）；
 * 最高权限本人发起即删、删前占用检查照走，遇占用 409 整笔回滚。不开测试事务，才看得到回滚；角色名中文、编号后缀纯数字。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class MerchantWorkOrderMasterDeleteJdbcIntegrationTests {

    private static final String BASE = "/api/v1/merchant-service/work-orders/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String project = "r383wo_prj_" + s;
    private final String merchant = "r383wo_mch_" + s;
    private final String requester = "usr_r383wo_req_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','工单删除回归综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, '工单删除回归商户', 'retail', ?, 'active', now(), now())", merchant, project);
    }

    /** 另一员工已申请删除的已关闭工单：持批准码的普通员工审批 403 MASTER_APPROVAL_REQUIRED，仍待审批。 */
    @Test
    void nonMasterApproveDelete_403MasterApprovalRequired_stillPending() throws Exception {
        String order = order("pending", "closed", true);
        RequestPostProcessor approver = new StaffRequestFixture(jdbc).staff("usr_r383wo_apv_" + s, "staff", project, null,
                "merchant.service_work_order.approve");

        send(approver, order, "delete-approve", "{\"decision\":\"approved\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MASTER_APPROVAL_REQUIRED"));
        assertThat(deleteStatus(order)).isEqualTo("pending");
    }

    /** 最高权限本人对已存档、已关闭的工单申请删除：当场删除，含已存档列表里不再出现，申请与批准各留一条痕。 */
    @Test
    void masterDeleteRequestOnArchivedClosedOrder_takesEffect() throws Exception {
        String order = order("direct", "closed", false);

        send(master(), order, "delete-request", "{\"reason\":\"重复录入\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.delete_status").value("approved"));

        List<String> ids = JsonPath.read(listBody(), "$.data.items[*].id");
        assertThat(ids).doesNotContain(order);
        assertThat(deleteStatus(order)).isEqualTo("approved");
        assertThat(events(order)).contains("merchant_service.work_order.delete_requested", "merchant_service.work_order.delete_approved");
    }

    /** 最高权限本人对已存档但仍在处理中的工单申请删除：409 WORK_ORDER_IN_USE，整笔回滚，不留待审批。 */
    @Test
    void masterDeleteRequestOnOpenOrder_409WorkOrderInUse_rolledBack() throws Exception {
        String order = order("open", "processing", false);

        send(master(), order, "delete-request", "{\"reason\":\"试删\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("WORK_ORDER_IN_USE"));
        assertThat(deleteStatus(order)).isEqualTo("none");
    }

    // ---------- 夹具 ----------

    /** 已存档的工单；pending=true 时另一位员工已申请删除、待审批。 */
    private String order(String tag, String status, boolean pending) {
        String id = "r383wo_" + tag + "_" + s;
        jdbc.update("INSERT INTO merchant_service_work_orders (id, merchant_id, work_order_type, priority, status, title, description,"
                + " reported_by_user_id, reported_channel, closed_at, created_at, updated_at, archived_at, archived_by, archive_reason,"
                + " delete_status, delete_requested_by, delete_requested_at, delete_reason)"
                + " VALUES (?, ?, 'repair', 'medium', ?, '门锁报修', '门锁松动', ?, 'web', CASE WHEN ? = 'closed' THEN now() END, now(), now(),"
                + " now(), ?, '已结案', CASE WHEN ? THEN 'pending' ELSE 'none' END, CASE WHEN ? THEN ? END, CASE WHEN ? THEN now() END,"
                + " CASE WHEN ? THEN '重复录入' END)",
                id, merchant, status, requester, status, requester, pending, pending, requester, pending, pending);
        return id;
    }

    // ---------- 请求与断言 ----------

    private ResultActions send(RequestPostProcessor who, String order, String action, String body) throws Exception {
        return mockMvc.perform(post(BASE + order + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String listBody() throws Exception {
        return mockMvc.perform(get("/api/v1/merchant-service/work-orders?include_archived=true").with(master()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String deleteStatus(String order) {
        return jdbc.queryForObject("SELECT delete_status FROM merchant_service_work_orders WHERE id = ?", String.class, order);
    }

    private List<String> events(String order) {
        return jdbc.queryForList("SELECT event_name FROM audit_events WHERE business_id = ? AND event_name LIKE 'merchant_service.work_order.delete_%'",
                String.class, order);
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
