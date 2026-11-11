package com.complex.platform.merchantservice;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.crypto.PiiCipher;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
 * 第十八批 AE 段第 201 条 201-R4（BSM:577「员工档案、设施设备、商户服务工单、卡座预订这类业务记录，现在也支持存档、以及审批后删除」
 * 「存档及删除都需要填写理由」「申请人不得审批自己的申请」）：商户服务工单存档、申请删除、审批删除按 OpenAPI 新补的契约走真实接口与真实角色授权，
 * 请求体一律按契约声明的蛇形键。入口早已存在，本类只锁现行为（契约 required、状态码、错误码与事件）。夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantWorkOrderArchiveDeleteHttpJdbcIntegrationTests {

    private static final String BASE = "/api/v1/merchant-service/work-orders/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PiiCipher cipher;
    @Autowired private MerchantServiceRepository repository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "r201r4_prj_" + suffix;
    private final String merchant = "r201r4_mch_" + suffix;
    private final String workOrder = "r201r4_wo_" + suffix;
    private RequestPostProcessor requester;
    private RequestPostProcessor approver;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','工单存档删除回归项目','active',now(),now())", project);
        jdbc.update("""
                INSERT INTO merchants(id,merchant_name,merchant_type,project_id,contact_name,contact_phone,status,created_at,updated_at)
                VALUES(?,'工单存档删除回归商户','retail',?,'主要联系人',?,'active',now(),now())""",
                merchant, project, cipher.encrypt("139" + String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000))));
        Instant now = Instant.now();
        repository.saveWorkOrder(new MerchantWorkOrderRecord(workOrder, merchant, null, null, "repair", "medium", "closed",
                "门锁报修", "门锁已更换", "usr_reporter", "web", null, null, null, now, null, null, List.of(), false, null, now, now));
        requester = account("req", role("req", "'merchant.service_work_order.close','merchant.service_work_order.approve'"));
        approver = account("apv", role("apv", "'merchant.service_work_order.approve'"));
    }

    /** 存档：契约 reason 必填——不带 400 VALIDATION_FAILED 且不存档；带 reason 200，记存档理由并落 merchant_service.work_order.archived。 */
    @Test
    void archiveRequiresReasonAndRecordsIt() throws Exception {
        send(requester, "archive", "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT archived_at IS NULL FROM merchant_service_work_orders WHERE id=?", Boolean.class, workOrder)).isTrue();

        send(requester, "archive", "{\"reason\":\"已结案存档\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archived_at").isNotEmpty());
        assertThat(jdbc.queryForObject("SELECT archive_reason FROM merchant_service_work_orders WHERE id=?", String.class, workOrder))
                .isEqualTo("已结案存档");
        assertThat(events("merchant_service.work_order.archived")).isEqualTo(1);
    }

    /** 申请删除带 reason 进入待审批；申请人审批自己的申请 403 WORK_ORDER_SELF_APPROVE_FORBIDDEN；另一审批人 decision=approved 200 并软删除。 */
    @Test
    void deleteRequestThenApprovalByAnotherApprover() throws Exception {
        send(requester, "archive", "{\"reason\":\"已结案存档\"}").andExpect(status().isOk());
        send(requester, "delete-request", "{\"reason\":\"重复录入\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.delete_status").value("pending"));

        send(requester, "delete-approve", "{\"decision\":\"approved\"}").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("WORK_ORDER_SELF_APPROVE_FORBIDDEN"));
        assertThat(deleteStatus()).isEqualTo("pending");

        send(approver, "delete-approve", "{\"decision\":\"approved\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision").value("approved"))
                .andExpect(jsonPath("$.data.deleted").value(true));
        assertThat(deleteStatus()).isEqualTo("approved");
        assertThat(events("merchant_service.work_order.delete_requested")).isEqualTo(1);
        assertThat(events("merchant_service.work_order.delete_approved")).isEqualTo(1);
    }

    private ResultActions send(RequestPostProcessor who, String action, String body) throws Exception {
        return mockMvc.perform(post(BASE + workOrder + "/" + action).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String deleteStatus() {
        return jdbc.queryForObject("SELECT delete_status FROM merchant_service_work_orders WHERE id=?", String.class, workOrder);
    }

    private int events(String eventName) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name=? AND business_id=?", Integer.class, eventName, workOrder);
    }

    private String role(String tag, String permissionCodes) {
        String role = "r201r4_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'工单存档删除回归','project','active',now())",
                role, "工单存档删除回归" + tag + "-" + Long.parseLong(suffix, 16));
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + permissionCodes + ")", role);
        return role;
    }

    /** 平台员工（归属本项目、不挂商户）：账号、角色、员工档案与职级与生产一致，以后台 Web 终端登录。 */
    private RequestPostProcessor account(String tag, String role) {
        String userId = "r201r4_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,NULL,'active',now())",
                userId, userId, project);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at) "
                + "VALUES (?, ?, NULL, ?, ?, 'approved', 'active', now(), now())", "r201r4_stf_" + tag + "_" + suffix, project, userId, tag);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
