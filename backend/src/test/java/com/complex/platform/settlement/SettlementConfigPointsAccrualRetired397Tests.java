package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CB 段第 397 条 397-R1（BSM:822「商户消费按商户所属业态的全国有效规则计算，可配置“每消费多少元产生一积分”（业务参数，按商户业态可配，
 * 须同时配置上下限）」）：结算配置中心的「积分累计率」业务不读（商户消费积分按业态积分规则计算），撤下写入口、转正与下线分支、总览展示；
 * 表与出厂种子留库、无读写。结算配置页真实请求（蛇形键）与真实授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SettlementConfigPointsAccrualRetired397Tests {

    private static final String BASE = "/api/v1/settlement/config";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_c397_" + s;
    private RequestPostProcessor manager;

    @BeforeEach
    void seed() {
        manager = new StaffRequestFixture(jdbc).staff("usr_c397_" + s, "staff", project, null,
                "settlement.config.manage", "settlement.config.view");
    }

    /** ① 积分累计率写入口已撤下：404 NOT_FOUND，表里不落行。修复前 200 落草案，断言红。 */
    @Test
    void savingPointsAccrualRateIsNoLongerAnEndpoint() throws Exception {
        int before = rows();
        mockMvc.perform(post(BASE + "/points-accrual-rate").with(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c397-save-%s", "project_id": "%s", "amount_cent_per_point": 50}""".formatted(s, project)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        assertThat(rows()).isEqualTo(before);
    }

    /** ② 总览不再展示积分累计率，待复核队列也不列它的草案（表里残留的草案行不再出现）。 */
    @Test
    void overviewNoLongerShowsPointsAccrualRate() throws Exception {
        String draft = pointsDraft();
        String overview = mockMvc.perform(get(BASE + "/overview").with(manager)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Object> defaults = JsonPath.read(overview, "$.data.platform_defaults[?(@.key == 'points_amount_cent_per_point')]");
        List<Object> drafts = JsonPath.read(overview, "$.data.pending_drafts[?(@.config_group == 'points_accrual_rate')]");
        List<Object> thisDraft = JsonPath.read(overview, "$.data.pending_drafts[?(@.id == '" + draft + "')]");
        assertThat(defaults).isEmpty();
        assertThat(drafts).isEmpty();
        assertThat(thisDraft).isEmpty();
    }

    /** ③ 对积分累计率草案转正：400 UNKNOWN_CONFIG_TYPE，草案行不动。修复前转正成功，断言红。 */
    @Test
    void activatingPointsAccrualRateIsUnknownConfigType() throws Exception {
        String draft = pointsDraft();
        mockMvc.perform(post(BASE + "/points_accrual_rate/" + draft + "/activate").with(manager))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("UNKNOWN_CONFIG_TYPE"));
        assertThat(jdbc.queryForObject("SELECT is_draft FROM points_accrual_rate_configs WHERE id = ?", Boolean.class, draft)).isTrue();
    }

    /** 表里残留一条该项目的积分累计率草案行（撤下前落的），起草人不是本例操作人。 */
    private String pointsDraft() {
        String id = "parc_c397_" + s;
        jdbc.update("INSERT INTO points_accrual_rate_configs (id, project_id, merchant_id, amount_cent_per_point, status, is_draft,"
                + " updated_by, updated_at) VALUES (?, ?, NULL, 50, 'active', TRUE, 'seed_c397', now())", id, project);
        return id;
    }

    private int rows() {
        return jdbc.queryForObject("SELECT count(*) FROM points_accrual_rate_configs", Integer.class);
    }
}
