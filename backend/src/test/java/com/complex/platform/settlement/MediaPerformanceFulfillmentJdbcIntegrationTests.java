package com.complex.platform.settlement;

// 文件版本：V0.01

import static com.complex.platform.settlement.MediaPerformanceTestFixtures.BASE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * TK-29-R2 实际履约登记与确认真库回归（BSM L1145「由传媒经办人在现有逐场合同记录中登记实际履约时间、已完成和未完成内容及依据；
 * 到酒吧演出的，由酒吧指定现场责任人确认；传媒自办场次，由传媒指定主管确认，确认过程按原留痕规则记录」；PGM L431；Q37）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MediaPerformanceFulfillmentJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SHOW_DATE = "2014-03-16";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = MediaPerformanceTestFixtures.suffix();
    private final String project = "mf_prj_" + suffix;
    private final String bar = "mf_bar_" + suffix;
    private final String media = "mf_media_" + suffix;
    private final String ticketType = "mf_tt_" + suffix;
    private RequestPostProcessor manager;
    private RequestPostProcessor mediaClerk;
    private RequestPostProcessor mediaSupervisor;
    private RequestPostProcessor barOnSite;

    @BeforeEach
    void seed() {
        MediaPerformanceTestFixtures.seedProject(jdbc, project);
        MediaPerformanceTestFixtures.seedMerchant(jdbc, bar, "bar", project);
        MediaPerformanceTestFixtures.seedMerchant(jdbc, media, "media", project);
        MediaPerformanceTestFixtures.seedTicketType(jdbc, ticketType, bar);
        manager = MediaPerformanceTestFixtures.staff(jdbc, "mf_mgr_" + suffix, project, null,
                "media.performance.contract.manage", "media.performance.dispute.resolve");
        mediaClerk = MediaPerformanceTestFixtures.staff(jdbc, "mf_clerk_" + suffix, project, media,
                "media.performance.fulfillment.record", "media.performance.fulfillment.confirm");
        mediaSupervisor = MediaPerformanceTestFixtures.staff(jdbc, "mf_sup_" + suffix, project, media,
                "media.performance.fulfillment.confirm");
        barOnSite = MediaPerformanceTestFixtures.staff(jdbc, "mf_site_" + suffix, project, bar,
                "media.performance.fulfillment.confirm");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // 到酒吧演出：确认人须属该酒吧范围（现场责任人）；传媒侧人员确认 → 403，记录仍 recorded。
    @Test
    void confirm_byNonBarScopeUser_forbidden() throws Exception {
        String showId = lockedBarShow();
        record(showId, mediaClerk, null);
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment/confirm").with(mediaClerk).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"cf-" + suffix + "\"}"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT status FROM media_performance_fulfillments WHERE show_id = ?", String.class, showId))
                .isEqualTo("recorded");
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment/confirm").with(barOnSite).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"cf2-" + suffix + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"));
        assertThat(jdbc.queryForObject("SELECT confirmed_by FROM media_performance_fulfillments WHERE show_id = ?", String.class, showId))
                .isEqualTo("mf_site_" + suffix);
    }

    // 自办场次：由传媒指定主管确认，登记人本人不得自确认 → 409 MEDIA_FULFILLMENT_SELF_CONFIRM；另一位传媒主管可确认。
    @Test
    void selfHosted_confirm_byRecorderSelf_rejected() throws Exception {
        String showId = create(MediaPerformanceTestFixtures.selfHostedBody(project, media, SHOW_DATE));
        lock(showId);
        record(showId, mediaClerk, null);
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment/confirm").with(mediaClerk).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"cf-" + suffix + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MEDIA_FULFILLMENT_SELF_CONFIRM"));
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment/confirm").with(mediaSupervisor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"cf2-" + suffix + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    // 争议挂起 → 主管裁决（结论必填、裁决人/时间留痕、审计事件 media_fulfillment.dispute_resolved）→ resolved。
    @Test
    void dispute_thenResolve_writesResolutionAndAudit() throws Exception {
        String showId = lockedBarShow();
        record(showId, mediaClerk, "第三组乐队未到场");
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment/dispute").with(barOnSite).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dp-" + suffix + "\",\"dispute_reason\":\"实际只演出两组\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disputed"));
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment/dispute/resolve").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"rs-" + suffix + "\",\"resolution_note\":\"核实为两组，按两组结算\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("resolved"));
        Map<String, Object> row = jdbc.queryForMap("SELECT status, dispute_reason, resolution_note, resolved_by, resolved_at FROM media_performance_fulfillments WHERE show_id = ?", showId);
        assertThat(row.get("status")).isEqualTo("resolved");
        assertThat(row.get("dispute_reason")).isEqualTo("实际只演出两组");
        assertThat(row.get("resolution_note")).isEqualTo("核实为两组，按两组结算");
        assertThat(row.get("resolved_by")).isEqualTo("mf_mgr_" + suffix);
        assertThat(row.get("resolved_at")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'media_fulfillment.dispute_resolved' AND business_id = ?",
                Integer.class, showId)).isEqualTo(1);
    }

    // 合同未锁定（draft）不能登记履约 → 409 MEDIA_SHOW_NOT_LOCKED。
    @Test
    void record_beforeShowLocked_rejected() throws Exception {
        String showId = create(MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, SHOW_DATE));
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment").with(mediaClerk).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(MediaPerformanceTestFixtures.fulfillmentBody(SHOW_DATE, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MEDIA_SHOW_NOT_LOCKED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_performance_fulfillments WHERE show_id = ?", Integer.class, showId)).isZero();
    }

    private String lockedBarShow() throws Exception {
        String showId = create(MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, SHOW_DATE));
        lock(showId);
        return showId;
    }

    private void lock(String showId) throws Exception {
        mockMvc.perform(post(BASE + "/" + showId + "/lock").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"lock-" + showId + "\"}"))
                .andExpect(status().isOk());
    }

    private void record(String showId, RequestPostProcessor who, String uncompleted) throws Exception {
        mockMvc.perform(post(BASE + "/" + showId + "/fulfillment").with(who).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(MediaPerformanceTestFixtures.fulfillmentBody(SHOW_DATE, uncompleted))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("recorded"));
    }

    private String create(Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post(BASE).with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).path("data").path("id").asText();
    }
}
