package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CB 段第 398 条 398-R1（BSM:521「所有涉及钱的配置一律版本化：每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批通过后
 * 按指定日期生效，不即改即生效。」「无法判断是否涉及钱的，一律按涉及钱处理。」；ENUM:52「通告期为业务参数，后台可配，出厂默认一个月，须同时配置上下限。」；
 * BSM:1597「异议期长度由后台配置（业务参数，按项目可配，出厂默认 10 个工作日，须同时配置上下限）」）：通票分账通告期与非积分商户账单异议期改为
 * 落草案（必填生效起始日期，须晚于今天）→ 他人在结算配置中心复核转正 → 批准后待生效 → 到日由系统生效；草案期与待生效期取值不变。
 * 真实请求（蛇形键）与真实授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SettlementNoticeObjectionVersioning398Tests {

    private static final String CONFIG = "/api/v1/settlement/config";
    private static final String NOTICE = CONFIG + "/distribution-scheme/notice-period";
    private static final String OBJECTION = "/api/v1/settlement/internal-pay-days/objection-period";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;
    @Autowired private SettlementConfigScheduledActivationService activation;
    @Autowired private InternalPayDayService payDays;

    private final ObjectMapper json = new ObjectMapper();
    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_c398_" + s;
    private RequestPostProcessor drafter;
    private RequestPostProcessor reviewer;
    private int seq;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '通告期异议期综合体', 'active', now(), now())", project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        drafter = staffs.staff("usr_c398_a_" + s, "staff", project, null, "settlement.config.manage", "settlement.config.view");
        reviewer = staffs.staff("usr_c398_b_" + s, "staff", project, null, "settlement.config.manage", "settlement.config.view");
    }

    /**
     * ① 通告期改 2 个月（下限 1、上限 3）带次日生效：保存后现行通告期不变、待复核队列有该草案；起草人本人转正 403；他人转正后待生效；
     * 到日由系统生效，原生效行下线留库。修复前即改即生效，断言红。
     */
    @Test
    void noticePeriodChangeWaitsForApprovalAndItsDate() throws Exception {
        JsonNode before = noticePeriod();
        LocalDate tomorrow = businessDays.businessDate(null, null, Instant.now()).plusDays(1);
        postJson(NOTICE, drafter, """
                {"idempotency_key": "%s", "period_value": 2, "period_unit": "month", "min_value": 1, "max_value": 3,
                 "effective_from": "%s"}""".formatted(key(), tomorrow)).andExpect(status().isOk());
        assertThat(noticePeriod()).as("保存只落草案，现行通告期不变").isEqualTo(before);

        List<String> active = jdbc.queryForList("SELECT id FROM ticket_distribution_notice_period WHERE is_draft = FALSE AND status = 'active'",
                String.class);
        String draft = jdbc.queryForObject("SELECT id FROM ticket_distribution_notice_period WHERE is_draft = TRUE AND status = 'active'",
                String.class);
        assertThat(pendingGroups(draft)).containsExactly("distribution_notice_period");
        activate("distribution_notice_period", draft, drafter).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SELF_ACTIVATION_FORBIDDEN"));
        activate("distribution_notice_period", draft, reviewer).andExpect(status().isOk());
        assertThat(statusOf("ticket_distribution_notice_period", draft)).isEqualTo("scheduled");
        assertThat(noticePeriod()).as("批准后到日前仍按原通告期").isEqualTo(before);

        activation.activateDue(Instant.now().plus(Duration.ofDays(1)));
        assertThat(noticePeriod().path("period_value").asInt()).isEqualTo(2);
        assertThat(noticePeriod().path("period_unit").asText()).isEqualTo("month");
        assertThat(active).isNotEmpty().allSatisfy(id -> assertThat(statusOf("ticket_distribution_notice_period", id)).as("原生效行下线留库")
                .isEqualTo("inactive"));
    }

    /**
     * ② 本项目异议期改 12 个工作日（下限 5、上限 15）带次日生效：保存后现行异议期与付款时点推算不变；起草人本人转正 403；他人在结算配置中心转正后待生效；
     * 到日由系统生效，付款时点推算按 12 个工作日。修复前即改即生效，断言红。
     */
    @Test
    void objectionPeriodChangeWaitsForApprovalAndItsDate() throws Exception {
        InternalPayDayFixtures.seed(jdbc);
        int before = objectionDays();
        LocalDate tomorrow = businessDays.businessDate(project, null, Instant.now()).plusDays(1);
        postJson(OBJECTION, drafter, """
                {"idempotency_key": "%s", "project_id": "%s", "working_days": 12, "min_days": 5, "max_days": 15,
                 "effective_from": "%s"}""".formatted(key(), project, tomorrow)).andExpect(status().isOk());
        assertThat(objectionDays()).as("保存只落草案，现行异议期不变").isEqualTo(before);

        String draft = jdbc.queryForObject("SELECT id FROM settlement_objection_period_policy WHERE project_id = ? AND is_draft = TRUE"
                + " AND status = 'active'", String.class, project);
        assertThat(pendingGroups(draft)).containsExactly("objection_period");
        activate("objection_period", draft, drafter).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SELF_ACTIVATION_FORBIDDEN"));
        activate("objection_period", draft, reviewer).andExpect(status().isOk());
        assertThat(statusOf("settlement_objection_period_policy", draft)).isEqualTo("scheduled");
        assertThat(objectionDays()).as("批准后到日前仍按原异议期").isEqualTo(before);
        assertThat(schedule().objectionWorkingDays()).isEqualTo(before);

        activation.activateDue(Instant.now().plus(Duration.ofDays(1)));
        assertThat(objectionDays()).isEqualTo(12);
        assertThat(schedule().objectionWorkingDays()).as("付款时点推算按到日生效的异议期").isEqualTo(12);
    }

    /** ③ 两个保存入口都必填生效起始日期：不带 400 VALIDATION_FAILED，带今天 400「生效起始日期须晚于今天（按营业日）」。修复前不收日期、保存即生效，断言红。 */
    @Test
    void effectiveFromRequired() throws Exception {
        LocalDate today = businessDays.businessDate(null, null, Instant.now());
        postJson(NOTICE, drafter, """
                {"idempotency_key": "%s", "period_value": 2, "period_unit": "month", "min_value": 1, "max_value": 3}"""
                .formatted(key())).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        postJson(NOTICE, drafter, """
                {"idempotency_key": "%s", "period_value": 2, "period_unit": "month", "min_value": 1, "max_value": 3,
                 "effective_from": "%s"}""".formatted(key(), today)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("生效起始日期须晚于今天（按营业日）"));

        LocalDate projectToday = businessDays.businessDate(project, null, Instant.now());
        postJson(OBJECTION, drafter, """
                {"idempotency_key": "%s", "project_id": "%s", "working_days": 12, "min_days": 5, "max_days": 15}"""
                .formatted(key(), project)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        postJson(OBJECTION, drafter, """
                {"idempotency_key": "%s", "project_id": "%s", "working_days": 12, "min_days": 5, "max_days": 15,
                 "effective_from": "%s"}""".formatted(key(), project, projectToday)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("生效起始日期须晚于今天（按营业日）"));
    }

    // ---------- helpers ----------

    private JsonNode noticePeriod() throws Exception {
        return read(mockMvc.perform(get(CONFIG + "/distribution-scheme").with(reviewer)).andExpect(status().isOk()))
                .path("data").path("notice_period");
    }

    private int objectionDays() throws Exception {
        return read(mockMvc.perform(get(OBJECTION).param("project_id", project).with(reviewer)).andExpect(status().isOk()))
                .path("data").path("working_days").asInt();
    }

    /** 本项目通票分账应付（本月账期、此刻过账）的付款时点推算。 */
    private InternalPayDayService.Schedule schedule() {
        LocalDate today = LocalDate.now(LocalZone.ID);
        return payDays.schedule("ticket_distribution", project, today.withDayOfMonth(1), Instant.now());
    }

    private List<String> pendingGroups(String draft) throws Exception {
        String overview = mockMvc.perform(get(CONFIG + "/overview").with(reviewer)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(overview, "$.data.pending_drafts[?(@.id == '" + draft + "')].config_group");
    }

    private ResultActions activate(String configType, String id, RequestPostProcessor who) throws Exception {
        return mockMvc.perform(post(CONFIG + "/" + configType + "/" + id + "/activate").with(who));
    }

    private ResultActions postJson(String path, RequestPostProcessor who, String body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String statusOf(String table, String id) {
        return jdbc.queryForObject("SELECT status FROM " + table + " WHERE id = ?", String.class, id);
    }

    private String key() {
        return "c398-" + (++seq) + "-" + s;
    }
}
