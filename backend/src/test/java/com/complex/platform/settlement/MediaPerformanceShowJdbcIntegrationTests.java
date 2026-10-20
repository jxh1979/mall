package com.complex.platform.settlement;

// 文件版本：V0.01

import static com.complex.platform.settlement.MediaPerformanceTestFixtures.BASE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
 * TK-29-R1 逐场演出合同记录真库回归（BSM L1145「每场演出适用哪几种情形、各自的数值与合同版本，必须逐场记录」「随合同固化版本」
 * 「传媒自办场次……不因本条确认新增酒吧应付」；DEC-R3-2026-08-04-36；操作员裁决 2026-10-20「逐场填每张加价×已核销张数」）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MediaPerformanceShowJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SHOW_DATE = "2014-03-15";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = MediaPerformanceTestFixtures.suffix();
    private final String project = "mp_prj_" + suffix;
    private final String bar = "mp_bar_" + suffix;
    private final String media = "mp_media_" + suffix;
    private final String ticketType = "mp_tt_" + suffix;
    private RequestPostProcessor manager;

    @BeforeEach
    void seed() {
        MediaPerformanceTestFixtures.seedProject(jdbc, project);
        MediaPerformanceTestFixtures.seedMerchant(jdbc, bar, "bar", project);
        MediaPerformanceTestFixtures.seedMerchant(jdbc, media, "media", project);
        MediaPerformanceTestFixtures.seedTicketType(jdbc, ticketType, bar);
        manager = MediaPerformanceTestFixtures.staff(jdbc, "mp_mgr_" + suffix, project, null,
                "media.performance.contract.manage", "media.performance.view");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // 锁定后三种情形的数值不可改（409 MEDIA_SHOW_LOCKED，原值不动）；改动只能新建版本行（contract_version+1，旧行保留）。
    @Test
    void lockShow_freezesContractValues_andRejectsEdit() throws Exception {
        String showId = create(MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, SHOW_DATE));
        mockMvc.perform(post(BASE + "/" + showId + "/lock").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"lock-" + suffix + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("locked"));

        Map<String, Object> edit = MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, SHOW_DATE);
        edit.put("fixed_fee_cent", 99000);
        mockMvc.perform(patch(BASE + "/" + showId).with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(edit)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MEDIA_SHOW_LOCKED"));
        Map<String, Object> row = jdbc.queryForMap("SELECT status, locked_by, fixed_fee_cent, contract_version FROM media_performance_shows WHERE id = ?", showId);
        assertThat(row.get("status")).isEqualTo("locked");
        assertThat(row.get("locked_by")).isEqualTo("mp_mgr_" + suffix);
        assertThat(((Number) row.get("fixed_fee_cent")).longValue()).as("锁定后原值不动").isEqualTo(50000L);

        mockMvc.perform(post(BASE + "/" + showId + "/versions").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(edit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contract_version").value(2))
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.fixed_fee_cent").value(99000));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_performance_shows WHERE show_group_id = (SELECT show_group_id FROM media_performance_shows WHERE id = ?)",
                Integer.class, showId)).as("旧行保留、新版本另起一行").isEqualTo(2);
        assertThat(((Number) jdbc.queryForMap("SELECT fixed_fee_cent FROM media_performance_shows WHERE id = ?", showId).get("fixed_fee_cent")).longValue())
                .isEqualTo(50000L);
    }

    // 自办场次不得填固定演出费 / 酒水提成（L1145「不因本条确认新增酒吧应付」）→ 400 MEDIA_SHOW_SELF_HOSTED_NO_BAR_PAYABLE，不落行。
    @Test
    void selfHostedShow_rejectsFixedFeeOrDrinkCommission() throws Exception {
        Map<String, Object> body = MediaPerformanceTestFixtures.selfHostedBody(project, media, SHOW_DATE);
        body.put("fixed_fee_cent", 30000);
        mockMvc.perform(post(BASE).with(manager).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("MEDIA_SHOW_SELF_HOSTED_NO_BAR_PAYABLE"));
        body.remove("fixed_fee_cent");
        body.put("drink_commission_bps", 500);
        body.put("drink_base_template", "show_window");
        mockMvc.perform(post(BASE).with(manager).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("MEDIA_SHOW_SELF_HOSTED_NO_BAR_PAYABLE"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_performance_shows WHERE media_merchant_id = ?", Integer.class, media)).isZero();
    }

    // 按前端 adminClient.mapKeys 蛇形键发请求：三种情形与裁决的「每张加价 + 适用票种」逐列落库，合同版本从 1 起、draft。
    @Test
    void createShow_snakeCaseRequestShape() throws Exception {
        String showId = create(MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, SHOW_DATE));
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM media_performance_shows WHERE id = ?", showId);
        assertThat(row.get("show_kind")).isEqualTo("bar_show");
        assertThat(row.get("bar_merchant_id")).isEqualTo(bar);
        assertThat(row.get("media_merchant_id")).isEqualTo(media);
        assertThat(row.get("ticket_share_mode")).isEqualTo("ratio");
        assertThat(((Number) row.get("ticket_share_bps")).intValue()).isEqualTo(5000);
        assertThat(((Number) row.get("ticket_surcharge_per_ticket_cent")).longValue()).isEqualTo(2000L);
        assertThat(row.get("ticket_type_id")).isEqualTo(ticketType);
        assertThat(((Number) row.get("fixed_fee_cent")).longValue()).isEqualTo(50000L);
        assertThat(((Number) row.get("drink_commission_bps")).intValue()).isEqualTo(1000);
        assertThat(row.get("drink_base_template")).isEqualTo("whole_store_session");
        assertThat(((Number) row.get("contract_version")).intValue()).isEqualTo(1);
        assertThat(row.get("status")).isEqualTo("draft");
        assertThat(row.get("show_date").toString()).isEqualTo(SHOW_DATE);
    }

    // 复核 I1（PGM L303 幂等）：同一请求编号同一正文重复提交只建一场、返回第一次的结果；同号异体 409。
    @Test
    void createShow_sameKeyReplay_returnsFirstShow_andDifferentBodyConflicts() throws Exception {
        Map<String, Object> body = MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, SHOW_DATE);
        String first = create(body);
        String second = create(body);
        assertThat(second).isEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_performance_shows WHERE media_merchant_id = ?", Integer.class, media))
                .isEqualTo(1);
        body.put("fixed_fee_cent", 77000);
        mockMvc.perform(post(BASE).with(manager).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_CONFLICT"));
    }

    // 复核 M2：酒吧 / 传媒商户须属于该场项目；不属于即 400，不落行。
    @Test
    void createShow_merchantOfOtherProject_rejected() throws Exception {
        String otherProject = "mp_prj2_" + suffix;
        String otherBar = "mp_bar2_" + suffix;
        MediaPerformanceTestFixtures.seedProject(jdbc, otherProject);
        MediaPerformanceTestFixtures.seedMerchant(jdbc, otherBar, "bar", otherProject);
        Map<String, Object> body = MediaPerformanceTestFixtures.barShowBody(project, otherBar, media, ticketType, SHOW_DATE);
        body.put("ticket_share_mode", "none");
        body.remove("ticket_share_bps");
        body.remove("ticket_surcharge_per_ticket_cent");
        body.remove("ticket_type_id");
        mockMvc.perform(post(BASE).with(manager).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("MEDIA_SHOW_MERCHANT_PROJECT_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_performance_shows WHERE bar_merchant_id = ?", Integer.class, otherBar)).isZero();
    }

    // 复核 M1：列表 / 详情按 OpenAPI 声明记 media_show.listed / media_show.viewed。
    @Test
    void listAndDetail_recordDeclaredAuditEvents() throws Exception {
        String showId = create(MediaPerformanceTestFixtures.barShowBody(project, bar, media, ticketType, SHOW_DATE));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(BASE).with(manager))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(BASE + "/" + showId).with(manager))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'media_show.listed' AND actor_id = ?",
                Integer.class, "mp_mgr_" + suffix)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'media_show.viewed' AND business_id = ?",
                Integer.class, showId)).isEqualTo(1);
    }

    private String create(Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post(BASE).with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).path("data").path("id").asText();
    }
}
