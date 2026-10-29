package com.complex.platform.contract;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * 第十八批 G 段第 82 条 82b-R3（合同：列表、铺位租赁状态与既有合同写按可触及项目，起草落项目，押金退还等待期按可触及项目）
 * 真库＋真实接口＋真实角色勾选回归，请求体一律按前端蛇形键。
 * 依据：治理地图第 659 行「普通岗位没有跨项目查看权」「在所分配的权限内可以查看、处理该角色可查看项目的数据」「逐个勾选的只能查看被选中的项目」
 * 「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」；第 413 行「所有基础对象都要带“综合体项目归属”」。
 *
 * <p>夹具随测试事务回滚：项目 A、B、C 三行（编号带随机后缀），操作人都是归属 A 的员工；持有人的角色只逐个勾 B，普通员工的角色没勾项目。
 * 修复前：起草不落项目、合同列表与铺位租赁状态全平台可见、别的项目的合同能提交、持有人配不了 B 的押金退还等待期。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ContractCrossProjectReachJdbcIntegrationTests {

    @Autowired private AccessRepository accessRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_g82r3_a_" + suffix;
    private final String projectB = "prj_g82r3_b_" + suffix;
    private final String projectC = "prj_g82r3_c_" + suffix;

    @BeforeEach
    void createProjects() {
        for (String projectId : List.of(projectA, projectB, projectC)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "G82R3 " + projectId);
        }
    }

    // ①② 起草：普通员工不带项目落本人项目 A；持有人带 B 落 B，带没勾的 C 403、不落库。
    @Test
    void draftLandsOnReachableProject() throws Exception {
        RequestPostProcessor plain = staffOfA("dp", role("dp", List.of(), "contract.draft.manage"));
        String own = JsonPath.read(draft(plain, null, "c1").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(projectOf(own)).isEqualTo(projectA);

        RequestPostProcessor ticked = staffOfA("dt", role("dt", List.of(projectB), "contract.draft.manage"));
        String inB = JsonPath.read(draft(ticked, projectB, "c2").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(projectOf(inB)).isEqualTo(projectB);
        draft(ticked, projectC, "c3")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contracts WHERE merchant_id = ?", Integer.class, "mch_g82r3_c3_" + suffix))
                .isZero();
    }

    // ③ 合同列表：普通员工只看到 A 的合同，持有人只看到 B 的合同。
    @Test
    void contractListFollowsReach() throws Exception {
        String inA = contract("la", projectA, "drafting");
        String inB = contract("lb", projectB, "drafting");
        RequestPostProcessor plain = staffOfA("lp", role("lp", List.of(), "contract.draft.manage"));
        RequestPostProcessor ticked = staffOfA("lt", role("lt", List.of(projectB), "contract.draft.manage"));

        assertThat(listedIds(plain)).contains(inA).doesNotContain(inB);
        assertThat(listedIds(ticked)).contains(inB).doesNotContain(inA);
    }

    // ④ 既有合同写：普通员工提交 B 的合同 403；持有人过范围闸（到状态校验 409）；撤掉角色后同一幂等键重放 403。
    @Test
    void submitOfContractOutOfReachIsForbidden() throws Exception {
        String inB = contract("sb", projectB, "pending_confirmation");
        String body = "{\"reason\":\"提交审批\",\"idempotency_key\":\"s1-" + suffix + "\"}";
        RequestPostProcessor plain = staffOfA("sp", role("sp", List.of(), "contract.draft.manage"));
        send(plain, "/api/v1/contracts/" + inB + "/submit", body)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));

        RequestPostProcessor ticked = staffOfA("st", role("st", List.of(projectB), "contract.draft.manage"));
        send(ticked, "/api/v1/contracts/" + inB + "/submit", body)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("CONTRACT_NOT_DRAFTING"));
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", "usr_g82r3_st_" + suffix);
        send(ticked, "/api/v1/contracts/" + inB + "/submit", body).andExpect(status().isForbidden());
    }

    // ⑤ 押金退还等待期：持有人能配 B；普通员工配 B 403。
    @Test
    void depositRefundWaitFollowsReach() throws Exception {
        String url = "/api/v1/contracts/deposit-refund-wait";
        RequestPostProcessor ticked = staffOfA("wt", role("wt", List.of(projectB), "contract.deposit_refund_wait.manage"));
        send(ticked, url, waitBody("w1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        mockMvc.perform(get(url + "?project_id=" + projectB).with(ticked))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.value").value(30));

        RequestPostProcessor plain = staffOfA("wp", role("wp", List.of(), "contract.deposit_refund_wait.manage"));
        send(plain, url, waitBody("w2"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    // ⑥ 铺位租赁状态（不带参数，与页面一致）：普通员工只出 A 的铺位，持有人只出 B 的铺位。
    @Test
    void spaceRentalStatusFollowsReach() throws Exception {
        String spaceA = space("a", projectA);
        String spaceB = space("b", projectB);
        RequestPostProcessor plain = staffOfA("rp", role("rp", List.of(), "contract.draft.manage"));
        RequestPostProcessor ticked = staffOfA("rt", role("rt", List.of(projectB), "contract.draft.manage"));

        assertThat(rentalSpaceIds(plain)).contains(spaceA).doesNotContain(spaceB);
        assertThat(rentalSpaceIds(ticked)).contains(spaceB).doesNotContain(spaceA);
    }

    // ⑦ 城市项目公司员工（员工档案归属是公司编号、不是项目）起草不带项目：落地须是项目（照 LeasingService.landingProject），403、不落库。
    // 复核修复前红：200，合同 project_id 落成公司编号。
    @Test
    void draftByCompanyStaffWithoutProjectIsForbidden() throws Exception {
        String company = "be_g82r3_pc_" + suffix;
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, 'project_company', 'active', now(), now())", company, company);
        RequestPostProcessor companyStaff = staffOf("pc", company, role("pc", List.of(), "contract.draft.manage"));
        draft(companyStaff, null, "c4")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contracts WHERE merchant_id = ?", Integer.class, "mch_g82r3_c4_" + suffix))
                .isZero();
    }

    // ── 夹具、会话与取数 ─────────────────────────────────────────────────────────────────────────

    private ResultActions draft(RequestPostProcessor session, String projectId, String key) throws Exception {
        return send(session, "/api/v1/contracts", "{\"merchant_id\":\"mch_g82r3_" + key + "_" + suffix + "\","
                + "\"contract_subject_id\":\"subj_g82r3_" + suffix + "\",\"settlement_subject_id\":\"subj_g82r3_" + suffix + "\","
                + "\"effective_date\":\"2026-11-01\",\"expiry_date\":\"2027-10-31\",\"rent_terms\":{\"base_rent_cent\":100000},"
                + "\"commission_mode\":\"fixed_ratio\",\"energy_fee_basis\":\"none\","
                + (projectId == null ? "" : "\"project_id\":\"" + projectId + "\",")
                + "\"idempotency_key\":\"" + key + "-" + suffix + "\"}");
    }

    private String waitBody(String key) {
        return "{\"project_id\":\"" + projectB + "\",\"value\":30,\"min_value\":0,\"max_value\":60,\"idempotency_key\":\"" + key + "-"
                + suffix + "\"}";
    }

    private String contract(String tag, String projectId, String status) {
        String id = "ctr_g82r3_" + tag + "_" + suffix;
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?, ?, ?, ?, DATE '2026-11-01', DATE '2027-10-31', ?, ?, '{"baseRentCent":100000}'::jsonb, now(), now())""",
                id, "mch_g82r3_" + tag + "_" + suffix, "subj_g82r3_" + suffix, "subj_g82r3_" + suffix, status, projectId);
        return id;
    }

    /** 铺位编号以 0 开头：铺位租赁状态按编号排序取前 1000 行，共享库里别的铺位不挤掉夹具。 */
    private String space(String tag, String projectId) {
        String id = "0g82r3_sp_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, floor_no, area_square_meter, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'shop', '1F', 88.0, 'active', now(), now())", id, projectId, id);
        return id;
    }

    private String projectOf(String contractId) {
        return jdbc.queryForObject("SELECT project_id FROM contracts WHERE id = ?", String.class, contractId);
    }

    private List<String> listedIds(RequestPostProcessor session) throws Exception {
        return JsonPath.read(mockMvc.perform(get("/api/v1/contracts").with(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
    }

    private List<String> rentalSpaceIds(RequestPostProcessor session) throws Exception {
        return JsonPath.read(mockMvc.perform(get("/api/v1/contracts/space-rental-status").with(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data[*].space_id");
    }

    private String role(String tag, List<String> ticked, String... codes) {
        String roleId = "role_g82r3_" + tag + "_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "G82R3 " + tag + " " + suffix, null, "project", "active", Instant.now(),
                List.of(codes), null, false, ticked));
        return roleId;
    }

    private RequestPostProcessor staffOfA(String tag, String roleId) {
        return staffOf(tag, projectA, roleId);
    }

    /** 建一名归属给定组织（项目或公司）的员工、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体）。 */
    private RequestPostProcessor staffOf(String tag, String organizationId, String roleId) {
        String userId = "usr_g82r3_" + tag + "_" + suffix;
        accessRepository.createUser(new AccessUser(userId, userId, "staff", organizationId, null, "active", Instant.now()));
        accessRepository.assignUserRole(userId, roleId);
        AccessActor actor = new AccessActor(userId, userId, "staff", organizationId, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions send(RequestPostProcessor session, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
