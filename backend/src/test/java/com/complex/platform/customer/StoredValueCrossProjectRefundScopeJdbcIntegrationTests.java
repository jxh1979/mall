package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BD 段第 376 条 376-R1（PGM:659「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」
 * 「逐个勾选的只能查看被选中的项目」；ENUM:86「综合体通用卡：……余额管理与退款均由长沙项目公司负责。」；BSM:833「账户可冻结/解冻」）：
 * 储值查卡、退卡、冻结与解冻按入口权限码可触及的项目判，认可可跨项目角色勾选的项目。
 * 账号组织取项目公司主体本身（同长沙项目公司员工自助注册后的组织），角色勾选综合体 B。真库、真实入口、真实授权链、蛇形键。
 * 修复前：查卡为空，冻结、退卡 403 CROSS_PROJECT_FORBIDDEN。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StoredValueCrossProjectRefundScopeJdbcIntegrationTests {

    private static final String BASE = "/api/v1/loyalty/stored-value/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccessRepository accessRepository;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String projectA = "r376prj_a_" + s;
    private final String projectB = "r376prj_b_" + s;
    private final String company = "r376be_" + s;
    private String customer;
    private String phone;
    private String generalA;
    private String generalB;
    private RequestPostProcessor companyStaff;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                    project, "储值跨项目回归综合体" + project);
        }
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'project_company', 'active', now(), now())", company, "储值跨项目回归项目公司" + s);
        customer = "r376cus_" + s;
        phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        customerRepository.saveCustomer(new CustomerRecord(customer, phone, "139****" + phone.substring(7), "active", null, null,
                Instant.now(), Instant.now()));
        generalA = card(projectA);
        generalB = card(projectB);
        String role = "role_r376_" + s;
        accessRepository.createRole(new RoleRecord(role, "储值跨项目回归角色" + s, null, "project", "active", Instant.now(),
                List.of("customer.stored_value.refund", "customer.stored_value.freeze"), null, false, List.of(projectB)));
        String userId = "usr_r376_" + s;
        accessRepository.createUser(new AccessUser(userId, userId, "staff", company, null, "active", Instant.now()));
        accessRepository.assignUserRole(userId, role);
        AccessActor actor = new AccessActor(userId, userId, "staff", company, null, "active");
        companyStaff = request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** ① 持勾选 B 的可跨项目角色的项目公司员工：查卡见 B 通用卡、不见 A；冻结、解冻 B 通用卡 200；退 B 通用卡全部本金 200，待退登记在 B。 */
    @Test
    void crossProjectRoleHolderHandlesTickedProjectsGeneralCard() throws Exception {
        List<String> ids = JsonPath.read(send(BASE + "refund-cards/lookup", "{\"phone\":\"" + phone + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data[*].account_id");
        assertThat(ids).contains(generalB).doesNotContain(generalA);

        send(BASE + customer + "/freeze", scopeBody(projectB)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("frozen"));
        send(BASE + customer + "/unfreeze", scopeBody(projectB)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));

        send(BASE + customer + "/refund", "{\"project_id\":\"" + projectB + "\",\"amount_cent\":5000,\"reason\":\"客人到店退卡\","
                + "\"close_account\":true,\"idempotency_key\":\"r376-refund-" + s + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.txn_type").value("refund"));
        assertThat(jdbc.queryForObject("SELECT project_id FROM finance_refunds WHERE source_type = 'stored_value_account' AND target_id = ?",
                String.class, generalB)).isEqualTo(projectB);
        assertThat(cardStatus(generalB)).isEqualTo("closed");
    }

    /** ② 同一账号冻结未勾选项目 A 的通用卡 403 CROSS_PROJECT_FORBIDDEN，卡仍有效。 */
    @Test
    void untickedProjectStaysForbidden() throws Exception {
        send(BASE + customer + "/freeze", scopeBody(projectA)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(cardStatus(generalA)).isEqualTo("active");
    }

    // ---------- 夹具与请求 ----------

    private String card(String projectId) {
        String id = "svacc_" + UUID.randomUUID().toString().replace("-", "");
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(id, customer, projectId, null, 5000L, "active",
                Instant.now(), Instant.now(), null, 5000L, 0L));
        return id;
    }

    private static String scopeBody(String projectId) {
        return "{\"project_id\":\"" + projectId + "\",\"reason\":\"客人报失\"}";
    }

    private ResultActions send(String path, String body) throws Exception {
        return mockMvc.perform(post(path).with(companyStaff).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String cardStatus(String accountId) {
        return jdbc.queryForObject("SELECT status FROM customer_stored_value_accounts WHERE id = ?", String.class, accountId);
    }
}
