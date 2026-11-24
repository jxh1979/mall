package com.complex.platform.amusement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 CF 段第 407 条 407-R1（BSM:1271「每个游乐项目可以关联多台设备，并区分「关键安全设备」与「辅助设备」……每台关键安全设备属于哪一种，
 * 须在设备台账中登记……登记内容变更须经工程或安全审批。项目上线后要把某台关键安全设备改为辅助设备，必须重新经过工程或安全审批，并且只能在该项目停运后生效」；
 * 决策台账「项目首次上线前，由游乐公司提出设备关键／辅助分类，工程或安全岗位复核通过后生效」「设备分类和每次变更必须保留申请人、复核人、理由、生效时间和
 * 历史版本」）：建档必带分级与理由，首次与每次变更都进工程或安全复核、批准才生效并留历史；开放中的项目不能批准关键降为辅助。
 * 真库、真实 HTTP、真实授权链与蛇形键；复核人为发起人直接上级且经角色持有分级复核权限（指定为最终审批人）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AmusementDeviceClassificationJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private PlatformTransactionManager transactions;

    private final String digits = String.valueOf(System.nanoTime());
    private final String project = "prj_a407_" + digits;
    private final String merchant = "mch_a407_" + digits;
    private final String amusement = "amu_a407_" + digits;
    private RequestPostProcessor manager;
    private RequestPostProcessor engineer;
    private int keys;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                project, "设备分级综合体" + digits);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                + " VALUES(?,'设备分级商户','amusement',?,'active',now(),now())", merchant, project);
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, eligibility_rule_json, merchant_id,"
                + " created_at, updated_at) VALUES (?, '过山车', 10, 'open', '{}'::jsonb, ?, now(), now())", amusement, merchant);
        manager = account("manager", "amusement.device.manage", "amusement.device.view",
                "governance.supervisor_review.view", "governance.supervisor_review.approve");
        engineer = account("engineer", "amusement.device.class.approve", "amusement.device.view",
                "governance.supervisor_review.view", "governance.supervisor_review.approve");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("manager"), user("engineer"));
    }

    /** 建档不带分级 400、关键不带依赖关系 400；带辅助建成后分级未生效、历史一行待复核；申请人本人复核 403；工程岗批准后生效。 */
    @Test
    void firstClassificationTakesEffectOnlyAfterEngineeringReview() throws Exception {
        createDevice("d0", Map.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        createDevice("d0", Map.of("safety_class", "critical")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_devices WHERE project_id=?", Integer.class, amusement)).isZero();

        String device = id(createDevice("d1", Map.of("safety_class", "auxiliary")).andExpect(status().isOk()));
        assertThat(column(device, "safety_class")).isNull();
        String history = history();
        assertThat(JsonPath.<List<Object>>read(history, "$.data[*].id")).hasSize(1);
        assertThat(JsonPath.<String>read(history, "$.data[0].status")).isEqualTo("pending_review");
        assertThat(JsonPath.<String>read(history, "$.data[0].to_class")).isEqualTo("auxiliary");
        assertThat(JsonPath.<String>read(history, "$.data[0].requested_by")).isEqualTo(user("manager"));
        String review = JsonPath.read(history, "$.data[0].review_id");

        approve(manager, review).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        approve(engineer, review).andExpect(status().isOk());
        assertThat(column(device, "safety_class")).isEqualTo("auxiliary");
        String decided = history();
        assertThat(JsonPath.<String>read(decided, "$.data[0].status")).isEqualTo("approved");
        assertThat(JsonPath.<String>read(decided, "$.data[0].decided_by")).isEqualTo(user("engineer"));
    }

    /**
     * 关键（依赖）设备申请降为辅助：项目开放中批准 409、仍是关键；项目停运（检修）后批准生效；同一设备已有待复核申请再申请 409；与现生效分级相同 409。
     */
    @Test
    void downgradeOfCriticalDeviceOnlyTakesEffectAfterProjectStops() throws Exception {
        String device = id(createDevice("d2", Map.of("safety_class", "critical", "dependency", "dependent")).andExpect(status().isOk()));
        approve(engineer, JsonPath.read(history(), "$.data[0].review_id")).andExpect(status().isOk());
        assertThat(column(device, "safety_class")).isEqualTo("critical");
        assertThat(column(device, "dependency")).isEqualTo("dependent");

        String change = change(device, "down", Map.of("safety_class", "auxiliary")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.from_class").value("critical"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String review = JsonPath.read(change, "$.data.review_id");

        approveInSavepoint(review).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_DOWNGRADE_REQUIRES_PROJECT_STOPPED"));
        assertThat(column(device, "safety_class")).isEqualTo("critical");
        change(device, "again", Map.of("safety_class", "critical", "dependency", "independent")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CLASS_CHANGE_PENDING"));

        jdbc.update("UPDATE amusement_projects SET open_status='maintenance' WHERE id=?", amusement);
        approve(engineer, review).andExpect(status().isOk());
        assertThat(column(device, "safety_class")).isEqualTo("auxiliary");
        assertThat(column(device, "dependency")).isNull();
        change(device, "same", Map.of("safety_class", "auxiliary")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CLASS_UNCHANGED"));
        assertThat(JsonPath.<List<Object>>read(history(), "$.data[*].id")).hasSize(2);
    }

    /**
     * 收尾复核修正（BSM:1271「登记内容变更须经工程或安全审批」；决策台账「工程或安全岗位复核通过后生效」）：指定的最终审批人是只持通用复核权限、
     * 不持分级复核权限的上级时建档 400 FINAL_APPROVER_LACKS_ROLE；辅助设备带依赖关系 400 VALIDATION_FAILED；理由为空 400 VALIDATION_FAILED；都不建设备。
     */
    @Test
    void classificationReviewerMustHoldTheClassApprovePermission() throws Exception {
        account("boss", "amusement.device.view", "governance.supervisor_review.view", "governance.supervisor_review.approve");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("engineer"), user("boss"));
        inSavepoint(() -> createDevice("d5", Map.of("safety_class", "auxiliary", "final_approver_id", user("boss"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("FINAL_APPROVER_LACKS_ROLE"));
        createDevice("d6", Map.of("safety_class", "auxiliary", "dependency", "independent")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        createDevice("d7", Map.of("safety_class", "auxiliary", "operator_note", " ")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_devices WHERE project_id=?", Integer.class, amusement)).isZero();
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private String key(String tag) {
        return "a407-" + tag + "-" + digits + "-" + (keys++);
    }

    /** 网页设备台账新建表单实发形状：设备码、名称、周期、分级（关键时带依赖关系）、理由、最终审批人。 */
    private ResultActions createDevice(String code, Map<String, String> classification) throws Exception {
        Map<String, Object> body = new HashMap<>(classification);
        body.put("idempotency_key", key("device-" + code));
        body.put("device_code", code + "_" + digits);
        body.put("name", "设备" + code);
        body.put("status", "active");
        body.put("safety_check_interval_hours", 24);
        body.putIfAbsent("operator_note", "按安全评估定为该分级");
        body.putIfAbsent("final_approver_id", user("engineer"));
        return send(manager, post("/api/v1/amusement/projects/" + amusement + "/devices"), body);
    }

    private ResultActions change(String device, String tag, Map<String, String> classification) throws Exception {
        Map<String, Object> body = new HashMap<>(classification);
        body.put("idempotency_key", key("change-" + tag));
        body.put("reason", "改造后复评");
        body.put("final_approver_id", user("engineer"));
        return send(manager, post("/api/v1/amusement/projects/" + amusement + "/devices/" + device + "/classification-changes"), body);
    }

    private String history() throws Exception {
        return mockMvc.perform(get("/api/v1/amusement/projects/" + amusement + "/devices/classification-changes").with(manager))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private String column(String device, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM amusement_devices WHERE id=?", String.class, device);
    }

    private ResultActions approve(RequestPostProcessor who, String review) throws Exception {
        return send(who, post("/api/v1/platform/supervisor-review/" + review + "/approve"),
                Map.of("decision", "approved", "comment", "工程岗复核同意", "idempotency_key", key("approve")));
    }

    /** 批准放在保存点里：批准事务抛错被标为只回滚时回到保存点，等同生产中整笔批准回滚。 */
    private ResultActions approveInSavepoint(String review) {
        return inSavepoint(() -> approve(engineer, review));
    }

    /** 请求放在保存点里：请求事务抛错被标为只回滚时回到保存点，等同生产中整笔回滚。 */
    private ResultActions inSavepoint(java.util.concurrent.Callable<ResultActions> call) {
        TransactionTemplate nested = new TransactionTemplate(transactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        ResultActions[] result = new ResultActions[1];
        try {
            nested.executeWithoutResult(tx -> {
                try {
                    result[0] = call.call();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (UnexpectedRollbackException rolledBack) {
            // 预期：请求失败把事务标为只回滚，保存点已回滚
        }
        return result[0];
    }

    private ResultActions send(RequestPostProcessor who, MockHttpServletRequestBuilder request, Object body) throws Exception {
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 只授权限表里已有的码（新码随迁移落库）。 */
    private RequestPostProcessor account(String tag, String... permissions) {
        String userId = user(tag);
        String role = "role_a407_" + tag + "_" + digits;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'设备分级回归','project','active',now())",
                role, "设备分级回归角色" + tag.length() + digits);
        for (String permission : permissions) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code=?",
                    role, permission);
        }
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchant);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "usr_a407_" + tag + "_" + digits;
    }
}
