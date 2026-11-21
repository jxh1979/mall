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
import com.complex.platform.common.LocalZone;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
 * 第十八批 BW 段第 386 条 386-R1、386-R3（铺位到期着色分类按综合体配置）真库＋真实接口＋真实授权链回归，请求体一律按前端蛇形键。
 * 依据：BSM:1354「按距到期天数着色，分类、颜色和提前天数按综合体后台可配（业务参数，按综合体可配；其中铺位租赁状态的分类属于业务分类定义，
 * 不做后台增删）」；PGM:659「在所分配的权限内可以查看、处理该角色可查看项目的数据」「逐个勾选的只能查看被选中的项目」。
 *
 * <p>夹具随测试事务回滚：综合体 A、B（编号带随机后缀）、平台管理员、只在 A 持着色分类配置权限的员工、A 与 B 各若干已租铺位。
 * 修复前：新增着色分类不带综合体即落平台默认行、带别的综合体不核范围；看板不带参数只按平台默认着色、带综合体时与平台默认叠加；
 * 保存请求里的着色分类编号与移除标记被忽略，改与移除都变成再建一条。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SpaceExpiryColorRuleByProjectJdbcIntegrationTests {

    private static final String RULES = "/api/v1/contracts/expiry-color-rules";
    private static final String RENTAL = "/api/v1/contracts/space-rental-status";

    @Autowired private AccessRepository accessRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_b95clr_a_" + suffix;
    private final String projectB = "prj_b95clr_b_" + suffix;
    private final String category = "着色回归" + suffix;
    private final LocalDate today = LocalDate.now(LocalZone.ID);
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() {
        for (String projectId : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "着色回归综合体 " + projectId);
        }
        admin = session("adm", "platform_admin", projectA,
                role("adm", List.of(), "contract.space.color.manage", "contract.draft.manage"));
    }

    // T1 平台管理员新增不带综合体：400 VALIDATION_FAILED，不落平台默认行（修复前 200、多一条 project_id 为空的行）。
    @Test
    void platformAdminMustNameProject() throws Exception {
        int before = jdbc.queryForObject("SELECT count(*) FROM space_expiry_color_rules", Integer.class);
        save(admin, null, null, "#DC2626", 60, null, "t1")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM space_expiry_color_rules", Integer.class)).isEqualTo(before);
    }

    // T2 A 配 60 天档：不带参数的看板上 A 的 45 天后到期铺位按 A 的档着色，B 的同期铺位仍是已租（修复前不带参数只用平台默认，A 铺是已租）。
    @Test
    void boardColorsEachSpaceByItsOwnProject() throws Exception {
        save(admin, projectA, null, "#DC2626", 60, null, "t2")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectA));
        String spaceA = letSpace("t2a", projectA, 45);
        String spaceB = letSpace("t2b", projectB, 45);

        Map<String, Object> a = rental(admin, "", spaceA);
        assertThat(a.get("rental_status")).isEqualTo("expiring");
        assertThat(a.get("color")).isEqualTo("#DC2626");
        assertThat(a.get("days_to_expiry")).isEqualTo(45);
        Map<String, Object> b = rental(admin, "", spaceB);
        assertThat(b.get("rental_status")).isEqualTo("let");
        assertThat(b.get("color")).isNull();
    }

    // T3 A 只配 10 天档：A 的 20 天后到期铺位不再叠加平台默认 30 天（两种取法都是已租）；B 未配置，按平台默认即将到期（修复前 A 铺两种取法都是即将到期）。
    @Test
    void ownRulesReplacePlatformDefault() throws Exception {
        save(admin, projectA, null, "#DC2626", 10, null, "t3").andExpect(status().isOk());
        String spaceA = letSpace("t3a", projectA, 20);
        String spaceB = letSpace("t3b", projectB, 20);

        assertThat(rental(admin, "?project_id=" + projectA, spaceA).get("rental_status")).isEqualTo("let");
        assertThat(rental(admin, "", spaceA).get("rental_status")).isEqualTo("let");
        Map<String, Object> b = rental(admin, "", spaceB);
        assertThat(b.get("rental_status")).isEqualTo("expiring");
        assertThat(b.get("color")).isEqualTo("#F59E0B");
    }

    // T4 配置列表按综合体：A 只出自己的行（修复前含平台默认 secr_default_30）；B 未配置只出平台默认行；平台管理员不带参数只出平台默认行。
    @Test
    void listFollowsProject() throws Exception {
        String own = id(save(admin, projectA, null, "#DC2626", 60, null, "t4").andExpect(status().isOk()));

        List<Map<String, Object>> inA = rules(admin, "?project_id=" + projectA);
        assertThat(inA).extracting(r -> r.get("id")).contains(own).doesNotContain("secr_default_30");
        assertThat(inA).extracting(r -> r.get("project_id")).containsOnly(projectA);
        List<Map<String, Object>> inB = rules(admin, "?project_id=" + projectB);
        assertThat(inB).extracting(r -> r.get("id")).contains("secr_default_30");
        assertThat(inB).extracting(r -> r.get("project_id")).containsOnlyNulls();
        assertThat(rules(admin, "")).extracting(r -> r.get("project_id")).containsOnlyNulls();
    }

    // T5 只在 A 持配置权限的员工：配 B 403、B 无新行，看 B 403（修复前都 200）；不带综合体落本人综合体 A（修复前落平台默认）。
    @Test
    void staffIsBoundToReachableProject() throws Exception {
        RequestPostProcessor staffA = session("sa", "staff", projectA, role("sa", List.of(), "contract.space.color.manage"));
        save(staffA, projectB, null, "#DC2626", 60, null, "t5b")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM space_expiry_color_rules WHERE project_id = ?", Integer.class, projectB))
                .isZero();
        mockMvc.perform(get(RULES + "?project_id=" + projectB).with(staffA)).andExpect(status().isForbidden());
        save(staffA, null, null, "#DC2626", 60, null, "t5a")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectA));
    }

    // T6 改：带编号保存即改本综合体这一条（编号不变、A 仍只有 1 行在用），A 的 45 天后到期铺位回到已租（修复前编号被忽略、新建第二行，仍即将到期）。
    @Test
    void editUpdatesTheSameRule() throws Exception {
        String own = id(save(admin, projectA, null, "#DC2626", 60, null, "t6").andExpect(status().isOk()));
        String spaceA = letSpace("t6a", projectA, 45);

        save(admin, projectA, own, "#B91C1C", 15, null, "t6e")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(own));
        assertThat(jdbc.queryForList("SELECT id FROM space_expiry_color_rules WHERE active AND project_id = ?", String.class, projectA))
                .containsExactly(own);
        assertThat(jdbc.queryForMap("SELECT color, threshold_days FROM space_expiry_color_rules WHERE id = ?", own))
                .containsEntry("color", "#B91C1C").containsEntry("threshold_days", 15);
        assertThat(rental(admin, "?project_id=" + projectA, spaceA).get("rental_status")).isEqualTo("let");
    }

    // T7 移除：带编号与 active=false 保存后列表与看板都不再取到；A 最后一条移除后回落平台默认（修复前移除变成再建一条）。
    @Test
    void removeFallsBackToPlatformDefault() throws Exception {
        String own = id(save(admin, projectA, null, "#DC2626", 60, null, "t7").andExpect(status().isOk()));
        String spaceA = letSpace("t7a", projectA, 20);

        save(admin, projectA, own, "#DC2626", 60, false, "t7r").andExpect(status().isOk());
        assertThat(rules(admin, "?project_id=" + projectA)).extracting(r -> r.get("project_id")).containsOnlyNulls();
        Map<String, Object> a = rental(admin, "?project_id=" + projectA, spaceA);
        assertThat(a.get("rental_status")).isEqualTo("expiring");
        assertThat(a.get("color")).isEqualTo("#F59E0B");
    }

    // T8 别的综合体的行、平台默认行都改不到：404 NOT_FOUND，原行不变（修复前 200、新建一条）。
    @Test
    void otherProjectOrDefaultRuleIsNotEditable() throws Exception {
        String own = id(save(admin, projectA, null, "#DC2626", 60, null, "t8").andExpect(status().isOk()));

        save(admin, projectB, own, "#B91C1C", 15, null, "t8b")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail_code").value("NOT_FOUND"));
        save(admin, projectA, "secr_default_30", "#B91C1C", 15, null, "t8d")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail_code").value("NOT_FOUND"));
        assertThat(jdbc.queryForMap("SELECT project_id, threshold_days FROM space_expiry_color_rules WHERE id = ?", own))
                .containsEntry("project_id", projectA).containsEntry("threshold_days", 60);
        assertThat(jdbc.queryForMap("SELECT project_id, color, threshold_days, active FROM space_expiry_color_rules WHERE id = ?",
                "secr_default_30"))
                .containsEntry("project_id", null).containsEntry("color", "#F59E0B").containsEntry("threshold_days", 30)
                .containsEntry("active", true);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM space_expiry_color_rules WHERE project_id = ?", Integer.class, projectB))
                .isZero();
    }

    // T9 A 配 60 天黄、15 天红两档：A 的 10 天后到期铺位命中最紧的 15 天档着红，45 天后到期的只命中 60 天档着黄。
    @Test
    void boardPicksTightestMatchingTier() throws Exception {
        save(admin, projectA, null, "#EAB308", 60, null, "t9y").andExpect(status().isOk());
        save(admin, projectA, null, "#DC2626", 15, null, "t9r").andExpect(status().isOk());
        String near = letSpace("t9n", projectA, 10);
        String far = letSpace("t9f", projectA, 45);

        Map<String, Object> n = rental(admin, "", near);
        assertThat(n.get("rental_status")).isEqualTo("expiring");
        assertThat(n.get("color")).isEqualTo("#DC2626");
        Map<String, Object> f = rental(admin, "", far);
        assertThat(f.get("rental_status")).isEqualTo("expiring");
        assertThat(f.get("color")).isEqualTo("#EAB308");
    }

    // T10 只在 A 持配置权限的员工不带综合体读着色分类（页面默认取法）：取本人综合体 A 自己的行，不是平台默认。
    @Test
    void staffListWithoutProjectReadsOwnProject() throws Exception {
        RequestPostProcessor staffA = session("sl", "staff", projectA, role("sl", List.of(), "contract.space.color.manage"));
        String own = id(save(staffA, null, null, "#DC2626", 60, null, "t10").andExpect(status().isOk()));

        List<Map<String, Object>> list = rules(staffA, "");
        assertThat(list).extracting(r -> r.get("id")).contains(own).doesNotContain("secr_default_30");
        assertThat(list).extracting(r -> r.get("project_id")).containsOnly(projectA);
    }

    // ── 夹具、会话与取数 ─────────────────────────────────────────────────────────────────────────

    private ResultActions save(RequestPostProcessor session, String projectId, String ruleId, String color, int days, Boolean active,
            String key) throws Exception {
        String body = "{\"idempotency_key\":\"b95clr-" + key + "-" + suffix + "\","
                + (projectId == null ? "" : "\"project_id\":\"" + projectId + "\",")
                + (ruleId == null ? "" : "\"id\":\"" + ruleId + "\",")
                + (active == null ? "" : "\"active\":" + active + ",")
                + "\"category_name\":\"" + category + "\",\"color\":\"" + color + "\",\"threshold_days\":" + days + "}";
        return mockMvc.perform(post(RULES).with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private List<Map<String, Object>> rules(RequestPostProcessor session, String query) throws Exception {
        return JsonPath.read(mockMvc.perform(get(RULES + query).with(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");
    }

    private Map<String, Object> rental(RequestPostProcessor session, String query, String spaceId) throws Exception {
        List<Map<String, Object>> rows = JsonPath.read(mockMvc.perform(get(RENTAL + query).with(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");
        return rows.stream().filter(r -> spaceId.equals(r.get("space_id"))).findFirst()
                .orElseThrow(() -> new AssertionError("看板里没有铺位 " + spaceId));
    }

    /** 已租铺位：编号以 0 开头（看板按编号排序取前 1000 行，共享库里别的铺位不挤掉夹具），绑一份已进入租期、days 天后到期的生效合同。 */
    private String letSpace(String tag, String projectId, int days) {
        String spaceId = "0b95clr_sp_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, floor_no, area_square_meter, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'shop', '1F', 88.0, 'active', now(), now())", spaceId, projectId, spaceId);
        String contractId = "ctr_b95clr_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date, "
                        + "lifecycle_status, project_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'effective', ?, now(), now())",
                contractId, "mch_b95clr_" + suffix, "subj_b95clr_" + suffix, "subj_b95clr_" + suffix,
                today.minusDays(30), today.plusDays(days), projectId);
        jdbc.update("INSERT INTO contract_spaces (contract_id, space_id) VALUES (?, ?)", contractId, spaceId);
        return spaceId;
    }

    private String role(String tag, List<String> ticked, String... codes) {
        String roleId = "role_b95clr_" + tag + "_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "着色回归角色 " + tag + " " + suffix, null, "project", "active", Instant.now(),
                List.of(codes), null, false, ticked));
        return roleId;
    }

    /** 建一名给定账号类型、归属给定组织的账号并挂上角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体）。 */
    private RequestPostProcessor session(String tag, String accountType, String organizationId, String roleId) {
        String userId = "usr_b95clr_" + tag + "_" + suffix;
        accessRepository.createUser(new AccessUser(userId, userId, accountType, organizationId, null, "active", Instant.now()));
        accessRepository.assignUserRole(userId, roleId);
        AccessActor actor = new AccessActor(userId, userId, accountType, organizationId, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
