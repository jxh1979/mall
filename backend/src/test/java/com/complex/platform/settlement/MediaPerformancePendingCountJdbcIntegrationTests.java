package com.complex.platform.settlement;

// 文件版本：V0.01

import static com.complex.platform.settlement.MediaPerformanceTestFixtures.BASE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 Y 段第 183 条 183-R2（BSM:1824「财务：日结状态、对账差异、结算待确认。」；第十八批裁决 40「数演出结算挂起待确认的、
 * 积分月结账单有未处理异议的，分两个数显示，不相加」）：总控驾驶舱「结算待确认」卡的演出结算一类——
 * GET /api/v1/settlement/media-shows/pending-settlement-count 数查看人可见场次里 status=pending 的演出结算条数，范围同场次列表。
 * 复核修复：只数现行（已锁定）版本上的；BSM:1839「卡片上的数字就是它下面明细的合计，点进去能核到每一笔」——场次列表逐行带同口径的
 * pending_settlement_count，卡片数＝列表各行之和。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MediaPerformancePendingCountJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = MediaPerformanceTestFixtures.suffix();
    private final String projectA = "mp_pa_" + suffix;
    private final String projectB = "mp_pb_" + suffix;

    @BeforeEach
    void seed() {
        for (String project : new String[] {projectA, projectB}) {
            MediaPerformanceTestFixtures.seedProject(jdbc, project);
            String media = "mp_media_" + project;
            MediaPerformanceTestFixtures.seedMerchant(jdbc, media, "media", project);
            String show = "mp_show_" + project;
            jdbc.update("INSERT INTO media_performance_shows(id,show_group_id,project_id,media_merchant_id,show_kind,show_date,status,locked_at,locked_by,created_by)"
                    + " VALUES(?,?,?,?,'self_hosted',DATE '2014-03-15','locked',now(),'seed','seed')", show, show, project, media);
            jdbc.update("INSERT INTO media_performance_settlements(id,show_id,fee_kind,status,pending_reason,created_by)"
                    + " VALUES(?,?,'ticket_share','pending','unconfirmed','seed')", show + "_pending", show);
            jdbc.update("INSERT INTO media_performance_settlements(id,show_id,fee_kind,status,created_by)"
                    + " VALUES(?,?,'fixed_fee','posted','seed')", show + "_posted", show);
        }
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // 本项目挂起 1 条、已出账 1 条 → 只数挂起的 1 条；项目 B 的那条挂起数不到（前端真实地址、真实授权链）。
    @Test
    void countsOnlyPendingSettlementsInViewersProjects() throws Exception {
        mockMvc.perform(get(BASE + "/pending-settlement-count")
                        .with(MediaPerformanceTestFixtures.staff(jdbc, "mp_va_" + suffix, projectA, null, "media.performance.view")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending_count").value(1));
    }

    @Test
    void withoutViewPermission_forbidden() throws Exception {
        mockMvc.perform(get(BASE + "/pending-settlement-count")
                        .with(MediaPerformanceTestFixtures.staff(jdbc, "mp_nv_" + suffix, projectA, null, "media.performance.contract.manage")))
                .andExpect(status().isForbidden());
    }

    // 复核修复①：v1 只约定固定费、结算挂 pending(contract_silent)，新建 v2 锁定后 v1 被取代、v2 出账——v1 那条不再计入（修前多 1）。
    @Test
    void supersededVersionPendingNotCounted() throws Exception {
        seedSupersededGroup();
        mockMvc.perform(get(BASE + "/pending-settlement-count")
                        .with(MediaPerformanceTestFixtures.staff(jdbc, "mp_vsp_" + suffix, projectA, null, "media.performance.view")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending_count").value(1));
    }

    // 复核修复②（BSM:1839）：场次列表逐行带 pending_settlement_count（现行版本上的挂起结算条数，被取代版本为 0），卡片数＝列表各行之和。
    @Test
    void cardCountEqualsSumOfListRows() throws Exception {
        String group = seedSupersededGroup();
        RequestPostProcessor viewer = MediaPerformanceTestFixtures.staff(jdbc, "mp_vsum_" + suffix, projectA, null, "media.performance.view");
        String list = mockMvc.perform(get(BASE).with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == 'mp_show_" + projectA + "')].pending_settlement_count").value(1))
                .andExpect(jsonPath("$.data[?(@.id == '" + group + "_v1')].pending_settlement_count").value(0))
                .andExpect(jsonPath("$.data[?(@.id == '" + group + "_v2')].pending_settlement_count").value(0))
                .andReturn().getResponse().getContentAsString();
        List<Integer> perRow = JsonPath.read(list, "$.data[*].pending_settlement_count");
        mockMvc.perform(get(BASE + "/pending-settlement-count").with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending_count").value(perRow.stream().mapToInt(Integer::intValue).sum()));
    }

    /** 项目 A 内同一场两个版本：v1 已被取代、名下残留 pending(contract_silent)；v2 现行已锁定、已出账。 */
    private String seedSupersededGroup() {
        String media = "mp_media_" + projectA;
        String group = "mp_grp_" + suffix;
        for (Object[] v : new Object[][] {{1, "superseded"}, {2, "locked"}}) {
            jdbc.update("INSERT INTO media_performance_shows(id,show_group_id,contract_version,project_id,media_merchant_id,show_kind,show_date,status,locked_at,locked_by,created_by)"
                    + " VALUES(?,?,?,?,?,'self_hosted',DATE '2014-03-16',?,now(),'seed','seed')", group + "_v" + v[0], group, v[0], projectA, media, v[1]);
        }
        jdbc.update("INSERT INTO media_performance_settlements(id,show_id,fee_kind,status,pending_reason,created_by)"
                + " VALUES(?,?,'fixed_fee','pending','contract_silent','seed')", group + "_v1_pending", group + "_v1");
        jdbc.update("INSERT INTO media_performance_settlements(id,show_id,fee_kind,status,created_by)"
                + " VALUES(?,?,'fixed_fee','posted','seed')", group + "_v2_posted", group + "_v2");
        return group;
    }
}
