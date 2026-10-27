package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.support.NotificationSendLogRows;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 * 第十八批第 2 条：通票分账方案（参与家数档位表＋第一段名单）的草案 → 差人复核转正 → 通告 → 经通告期生效 → 版本。
 *
 * <p>依据：ENUM L52「参与家数档位表（出厂默认，业务参数、后台可配，须同时配置上下限）：第二段参与 1 家＝第 1 名 100%……
 * 参与家数档位表不设家数上限……每一行的百分比合计必须等于 100%，合计不等于 100% 的配置不得生效」「档位表变更须提前向受影响的酒吧商户
 * 发出通告，自通告送达之日起经过通告期后方可生效；通告期为业务参数，后台可配，出厂默认一个月，须同时配置上下限」「第一段的参与酒吧名单，
 * 以及第一段的固定金额或固定单价，其变更……同样须提前向受影响的酒吧商户发出通告」；操作员裁决（第十八批第 1 条）
 * 「复核转正当日系统逐家发出通告就算送达，生效日＝转正日＋通告期」。请求一律按前端真实请求形状（adminClient 蛇形键）经网关提交。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SettlementDistributionSchemeJdbcIntegrationTests {

    private static final String BASE = "/api/v1/settlement/config/distribution-scheme";
    private static final DateTimeFormatter CN_DATE = DateTimeFormatter.ofPattern("yyyy年M月d日");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PiiCipher piiCipher;
    @Autowired private ObjectMapper json;
    @Autowired private DistributionSchemeService schemes;
    @Autowired private com.complex.platform.common.BusinessDayResolver businessDays;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "tds_prj_" + suffix;
    private RequestPostProcessor drafter;
    private RequestPostProcessor approver;
    private int seq;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','分账方案项目','active',now(),now())",
                project);
        String role = "tds_role_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'结算配置','project','active',now())",
                role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions"
                + " WHERE permission_code IN('settlement.config.view','settlement.config.manage')", role);
        drafter = actor("tds_drafter_" + suffix, role);
        approver = actor("tds_approver_" + suffix, role);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 出厂默认十行逐格等于 ENUM L52，每行合计 100%，出厂即生效。 */
    @Test
    void factorySchemeIsTheTenRowsOfEnum52AndInForce() throws Exception {
        Map<Integer, List<Integer>> rows = new LinkedHashMap<>();
        jdbc.query("SELECT participant_count, share_bps FROM ticket_distribution_participant_tiers WHERE version_id = 'tdsv_factory'"
                        + " ORDER BY participant_count, rank_no",
                rs -> { rows.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>()).add(rs.getInt(2)); });
        assertThat(rows).isEqualTo(Map.of(
                1, List.of(10000),
                2, List.of(5700, 4300),
                3, List.of(4000, 3000, 3000),
                4, List.of(3100, 2300, 2300, 2300),
                5, List.of(2500, 2000, 2000, 2000, 1500),
                6, List.of(2300, 1700, 1700, 1700, 1300, 1300),
                7, List.of(2200, 1500, 1500, 1500, 1100, 1100, 1100),
                8, List.of(1800, 1400, 1400, 1400, 1000, 1000, 1000, 1000),
                9, List.of(1500, 1300, 1300, 1300, 1000, 1000, 1000, 1000, 600),
                10, List.of(1600, 1200, 1200, 1200, 900, 900, 900, 900, 600, 600)));
        assertThat(rows.values()).allSatisfy(r -> assertThat(r.stream().mapToInt(Integer::intValue).sum()).isEqualTo(10000));

        JsonNode overview = read(mockMvc.perform(get(BASE).with(drafter)).andExpect(status().isOk()));
        JsonNode factory = version(overview, "tdsv_factory");
        assertThat(factory.path("state").asText()).isEqualTo("effective");
        assertThat(overview.path("data").path("notice_period").path("period_value").asInt()).isEqualTo(1);
        assertThat(overview.path("data").path("notice_period").path("period_unit").asText()).isEqualTo("month");
    }

    /** 某家数行合计不等于 100%（3 家行 40/30/29＝99%）→ 400，指出哪一行合计多少。 */
    @Test
    void rowNotSummingTo100PercentIsRejected() throws Exception {
        saveDraft(drafter, rowsJson(3, 4000, 3000, 2900), "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("合计为 99%")));
    }

    /** 每格须同时配置上下限，缺一即 400。 */
    @Test
    void everyCellNeedsBothBounds() throws Exception {
        saveDraft(drafter, """
                [{"participant_count":1,"rank_no":1,"share_bps":10000,"min_bps":9000}]""", "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    /** 不设家数上限：「参与 11 家」行照样能配。 */
    @Test
    void participantRowBeyondTenIsAccepted() throws Exception {
        String id = draftId(saveDraft(drafter, rowsJson(11, 1500, 1000, 1000, 1000, 1000, 1000, 900, 900, 600, 600, 500), "[]")
                .andExpect(status().isOk()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_distribution_participant_tiers WHERE version_id = ? AND participant_count = 11",
                Integer.class, id)).isEqualTo(11);
    }

    /** 同一账号起草又转正 → 403 SELF_ACTIVATION_FORBIDDEN。 */
    @Test
    void drafterCannotActivateOwnDraft() throws Exception {
        String id = draftId(saveDraft(drafter, rowsJson(1, 10000), "[]").andExpect(status().isOk()));
        mockMvc.perform(post(BASE + "/" + id + "/activate").with(drafter))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SELF_ACTIVATION_FORBIDDEN"));
    }

    /**
     * 另一人复核转正：当日逐家向受影响酒吧（本项目已发布通票入场覆盖的酒吧＋第一段名单里的酒吧）发通告即视为送达
     * （商户专区通告见下一条；短信照旧发，只作提醒），生效日＝转正日＋通告期（出厂一个月）；通告期内现行版仍是旧版。
     */
    @Test
    void activationNotifiesAffectedBarsAndTakesEffectAfterNoticePeriod() throws Exception {
        String a = bar("a", "13800000001");
        String b = bar("b", "13800000002");
        String c = bar("c", "13800000003");
        bar("d", "13800000004");
        passTicketCovering(a, b);
        String id = draftId(saveDraft(drafter, rowsJson(2, 5700, 4300),
                "[{\"merchant_id\":\"" + c + "\",\"fixed_amount_cent\":1000}]").andExpect(status().isOk()));

        JsonNode activated = read(mockMvc.perform(post(BASE + "/" + id + "/activate").with(approver)).andExpect(status().isOk()));

        assertThat(activated.path("data").path("status").asText()).isEqualTo("notified");
        assertThat(activated.path("data").path("state").asText()).as("通告期未满").isEqualTo("pending");
        Instant effectiveAt = jdbc.queryForObject("SELECT effective_at FROM ticket_distribution_scheme_versions WHERE id = ?",
                java.sql.Timestamp.class, id).toInstant();
        assertThat(effectiveAt).as("生效时刻＝转正营业日＋通告期那一营业日的起点（ENUM L187）")
                .isEqualTo(businessDays.start(project, null, businessDays.businessDate(project, null, Instant.now()).plusMonths(1)));
        for (String phone : List.of("13800000001", "13800000002", "13800000003")) {
            assertThat(NotificationSendLogRows.ids(jdbc, piiCipher, "settlement", "distribution_scheme_notice", phone::equals))
                    .as("受影响酒吧各一条短信提醒：" + phone).hasSize(1);
        }
        assertThat(NotificationSendLogRows.ids(jdbc, piiCipher, "settlement", "distribution_scheme_notice", "13800000004"::equals))
                .as("未被本项目通票覆盖、也不在第一段名单的酒吧不发").isEmpty();
    }

    /**
     * 18B 复核后操作员裁决第 10 条「商户专区显示通告」：复核转正当日逐家在受影响酒吧的商户专区挂一条通告＝送达（没有联系手机号的照挂），
     * 写明变更内容与生效日；内容只放本店可见的信息——本店的第一段固定额或单价，不写其他酒吧的
     * （ENUM L52「通票分账的计算规则对该酒吧主理人完全可见……仅其他酒吧分得的具体金额不对其开放」）。
     * 商户员工经商户专区只读到本商户的通告；无商户归属的员工 403 MERCHANT_SCOPE_REQUIRED；「已送达」审计按通告挂出计。
     */
    @Test
    void activationPostsZoneNoticeToEveryAffectedBarAndMerchantReadsOnlyOwn() throws Exception {
        String a = bar("na", "13800000011");
        String b = bar("nb", null);
        String c = bar("nc", "13800000013");
        String e = bar("ne", "13800000015");
        passTicketCovering(a, b);
        String id = draftId(saveDraft(drafter, rowsJson(2, 5700, 4300), "[{\"merchant_id\":\"" + c + "\",\"fixed_amount_cent\":1000},"
                + "{\"merchant_id\":\"" + e + "\",\"per_guest_cent\":777}]").andExpect(status().isOk()));

        mockMvc.perform(post(BASE + "/" + id + "/activate").with(approver)).andExpect(status().isOk());

        String effective = businessDays.businessDate(project, null, Instant.now()).plusMonths(1).toString();
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        JsonNode cNotices = zoneNotices(staffs.staff("tds_shop_c_" + suffix, "staff", project, c, "merchant.portal.settlement.view"));
        assertThat(cNotices).as("商户专区只见本店一条").hasSize(1);
        assertThat(cNotices.get(0).path("effective_date").asText()).isEqualTo(effective);
        assertThat(cNotices.get(0).path("content").asText())
                .as("正文生效日写中文日期，与「生效日」列一致").contains("自 " + LocalDate.parse(effective).format(CN_DATE) + " 起生效")
                .contains("参与 2 家：第 1 名 57%、第 2 名 43%").contains("本店第一段：固定金额，每份票 10.00 元")
                .as("不写其他酒吧的第一段金额").doesNotContain("7.77");
        JsonNode bNotices = zoneNotices(staffs.staff("tds_shop_b_" + suffix, "staff", project, b, "merchant.portal.settlement.view"));
        assertThat(bNotices).as("没有联系手机号的酒吧照样挂通告").hasSize(1);
        assertThat(bNotices.get(0).path("content").asText()).contains("本店不在第一段名单")
                .doesNotContain("10.00").doesNotContain("7.77");
        assertThat(jdbc.queryForObject("SELECT content FROM merchant_zone_notices WHERE source_id = ? AND merchant_id = ?",
                String.class, id, e)).contains("本店第一段：固定单价，每份票 7.77 元").doesNotContain("10.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_zone_notices WHERE source_id = ?", Integer.class, id))
                .as("受影响酒吧 a、b（通票覆盖）与 c、e（第一段名单）各一条").isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT metadata->>'delivered' FROM audit_events WHERE event_name ="
                + " 'settlement.config.distribution_scheme.notice_sent' AND business_id = ?", String.class, id))
                .as("已送达按通告挂出计，不按短信").isEqualTo("4");

        mockMvc.perform(get("/api/v1/merchant-portal/notices")
                        .with(staffs.staff("tds_staff_" + suffix, "staff", project, null, "merchant.portal.settlement.view")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
    }

    /**
     * 全平台方案转正（ENUM L52「档位表变更须提前向受影响的酒吧商户发出通告」）：本项目有已生效版本时按本项目版结算
     * （{@code effectiveVersionId} 本项目版优先），所以所在项目已有不晚于本版生效的本项目版的酒吧不受本版影响、不挂通告；
     * 本项目版晚于本版生效的，中间这段时间本版仍适用，照常挂通告。
     */
    @Test
    void globalActivationSkipsBarsWhoseProjectIsAlreadyOnItsOwnVersion() throws Exception {
        String later = "tds_prj_l_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','本项目版晚生效项目','active',now(),now())",
                later);
        String own = bar("ga", "13800000021");
        String stillAffected = "tds_bar_gb_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                + " VALUES(?,?,'bar',?,'active',now(),now())", stillAffected, stillAffected, later);
        passTicketCovering(own, stillAffected);
        jdbc.update("INSERT INTO ticket_distribution_scheme_versions(id,project_id,status,notified_at,effective_at)"
                + " VALUES(?,?,'notified',now(),now() - interval '1 day')", "tdsv_own_" + suffix, project);
        jdbc.update("INSERT INTO ticket_distribution_scheme_versions(id,project_id,status,notified_at,effective_at)"
                + " VALUES(?,?,'notified',now(),now() + interval '400 days')", "tdsv_later_" + suffix, later);
        String id = draftId(mockMvc.perform(post(BASE).with(drafter).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key":"tds_global_%s","project_id":null,"tiers":%s,"first_segment":[]}
                """.formatted(suffix, rowsJson(2, 5700, 4300)))).andExpect(status().isOk()));

        mockMvc.perform(post(BASE + "/" + id + "/activate").with(approver)).andExpect(status().isOk());

        assertThat(jdbc.queryForList("SELECT merchant_id FROM merchant_zone_notices WHERE source_id = ? AND merchant_id IN (?, ?)",
                String.class, id, own, stillAffected))
                .as("本项目已由本项目版管辖的酒吧不挂；本项目版晚于本版生效的照挂").containsExactly(stillAffected);
    }

    /** 通告期数值、单位与上下限须同时填写；改成 10 天后，转正生效日＝转正日＋10 天。 */
    @Test
    void noticePeriodNeedsBoundsAndDrivesEffectiveDate() throws Exception {
        notice("{\"idempotency_key\":\"np1_" + suffix + "\",\"period_value\":10,\"period_unit\":\"day\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        notice("{\"idempotency_key\":\"np2_" + suffix + "\",\"period_value\":10,\"period_unit\":\"day\",\"min_value\":7,\"max_value\":30}")
                .andExpect(status().isOk());
        String id = draftId(saveDraft(drafter, rowsJson(1, 10000), "[]").andExpect(status().isOk()));

        mockMvc.perform(post(BASE + "/" + id + "/activate").with(approver)).andExpect(status().isOk());

        Instant effectiveAt = jdbc.queryForObject("SELECT effective_at FROM ticket_distribution_scheme_versions WHERE id = ?",
                java.sql.Timestamp.class, id).toInstant();
        assertThat(effectiveAt)
                .isEqualTo(businessDays.start(project, null, businessDays.businessDate(project, null, Instant.now()).plusDays(10)));
    }

    /**
     * 生效日按营业日切换（ENUM L187「营业日从当日上午 10:00 起算，至次日上午 10:00 止」；第十八批裁决第 1 条「生效日＝转正日＋通告期」）：
     * 通告期 1 天、生效日 D——D 当天上午 09:00 售出的票营业日仍属 D−1，按旧版固化；10:00 起售出的才按新版。
     */
    @Test
    void schemeTakesEffectAtTheStartOfTheEffectiveBusinessDay() throws Exception {
        notice("{\"idempotency_key\":\"np3_" + suffix + "\",\"period_value\":1,\"period_unit\":\"day\",\"min_value\":1,\"max_value\":30}")
                .andExpect(status().isOk());
        String id = draftId(saveDraft(drafter, rowsJson(1, 10000), "[]").andExpect(status().isOk()));

        mockMvc.perform(post(BASE + "/" + id + "/activate").with(approver)).andExpect(status().isOk());

        LocalDate effective = businessDays.businessDate(project, null, Instant.now()).plusDays(1);
        assertThat(schemes.effectiveVersionId(project, effective.atTime(9, 0).atZone(LocalZone.ID).toInstant()))
                .as("生效日当天 10:00 前仍属上一营业日，按旧版").isNotEqualTo(id);
        assertThat(schemes.effectiveVersionId(project, effective.atTime(10, 0).atZone(LocalZone.ID).toInstant()))
                .as("生效营业日起按新版").isEqualTo(id);
    }

    /**
     * 三个写口按 OpenAPI 声明：通告期写口 x-idempotency-required，缺幂等键 400；起草校验失败、通告期校验失败、自行转正被拒各写一条失败审计
     * （settlement.config.distribution_scheme.update_failed / distribution_notice_period.update_failed / draft.activate_failed）。
     */
    @Test
    void writesRejectMissingIdempotencyKeyAndAuditFailures() throws Exception {
        notice("{\"period_value\":10,\"period_unit\":\"day\",\"min_value\":7,\"max_value\":30}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        saveDraft(drafter, rowsJson(3, 4000, 3000, 2900), "[]").andExpect(status().isBadRequest());
        String id = draftId(saveDraft(drafter, rowsJson(1, 10000), "[]").andExpect(status().isOk()));
        mockMvc.perform(post(BASE + "/" + id + "/activate").with(drafter)).andExpect(status().isForbidden());

        assertThat(failures("settlement.config.distribution_notice_period.update_failed", "tds_approver_" + suffix)).isEqualTo(1);
        assertThat(failures("settlement.config.distribution_scheme.update_failed", "tds_drafter_" + suffix)).isEqualTo(1);
        assertThat(failures("settlement.config.draft.activate_failed", "tds_drafter_" + suffix)).isEqualTo(1);
    }

    /** 「补配该家数档位行」待办：暂挂的份按（项目，第二段参与家数）列出份数与第二段余额。 */
    @Test
    void suspendedUnitsShowUpAsMissingRowTodo() throws Exception {
        String bill = "sb_tds_" + suffix;
        jdbc.update("INSERT INTO settlement_bills(id,project_id,period_start,period_end) VALUES(?,?,DATE '2015-03-01',DATE '2015-03-31')",
                bill, project);
        String ticket = "tk_tds_" + suffix;
        jdbc.update("""
                INSERT INTO pass_pool_snapshots (ticket_instance_id, order_id, project_id, slots, visits, bar_gross_cent, route_fee_cent,
                    sales_channel_fee_cent, gift_cost_cent, service_fee_cent, pool_cent, scheme_version_id, covered_bar_ids)
                VALUES (?, ?, ?, 1, 1, 9000, 0, 0, 0, 0, 9000, 'tdsv_factory', CAST(? AS text[]))""",
                ticket, "o_" + ticket, project, "{" + String.join(",", java.util.Collections.nCopies(11, "x")) + "}");
        for (int i = 1; i <= 11; i++) {
            jdbc.update("""
                    INSERT INTO pass_pool_unit_results (bill_id, ticket_instance_id, slot_index, visit_no, merchant_id, segment,
                        entry_people, segment_pool_cent, amount_cent, verified, status, scheme_version_id)
                    VALUES (?, ?, 1, 1, ?, 2, 1, 9000, 0, TRUE, 'suspended', 'tdsv_factory')""", bill, ticket, "m" + i);
        }

        JsonNode overview = read(mockMvc.perform(get(BASE).with(drafter)).andExpect(status().isOk()));

        JsonNode todo = null;
        for (JsonNode row : overview.path("data").path("missing_rows")) {
            if (project.equals(row.path("project_id").asText())) {
                todo = row;
            }
        }
        assertThat(todo).as("该项目有暂挂待补配的家数行").isNotNull();
        assertThat(todo.path("participant_count").asInt()).isEqualTo(11);
        assertThat(todo.path("units").asInt()).isEqualTo(1);
        assertThat(todo.path("amount_cent").asLong()).isEqualTo(9000L);
    }

    // ───────── helpers ─────────

    private RequestPostProcessor actor(String user, String role) {
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'platform_admin','org','active',now())",
                user, user);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, "platform_admin", "org", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions saveDraft(RequestPostProcessor who, String tiersJson, String firstSegmentJson) throws Exception {
        return mockMvc.perform(post(BASE).with(who).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key":"tds_%s_%d","project_id":"%s","tiers":%s,"first_segment":%s}
                """.formatted(suffix, ++seq, project, tiersJson, firstSegmentJson)));
    }

    private JsonNode zoneNotices(RequestPostProcessor who) throws Exception {
        return read(mockMvc.perform(get("/api/v1/merchant-portal/notices").with(who)).andExpect(status().isOk())).path("data");
    }

    private int failures(String event, String actor) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND event_result = 'failure' AND actor_id = ?",
                Integer.class, event, actor);
    }

    private ResultActions notice(String body) throws Exception {
        return mockMvc.perform(post(BASE + "/notice-period").with(approver).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 一个家数行：各名次比例，上下限取比例 ±1000 基点（夹在 0～10000 内）。 */
    private static String rowsJson(int participants, int... shares) {
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < shares.length; i++) {
            cells.add("{\"participant_count\":%d,\"rank_no\":%d,\"share_bps\":%d,\"min_bps\":%d,\"max_bps\":%d}".formatted(
                    participants, i + 1, shares[i], Math.max(0, shares[i] - 1000), Math.min(10000, shares[i] + 1000)));
        }
        return "[" + String.join(",", cells) + "]";
    }

    private String draftId(ResultActions result) throws Exception {
        return read(result).path("data").asText();
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static JsonNode version(JsonNode overview, String id) {
        for (JsonNode v : overview.path("data").path("versions")) {
            if (id.equals(v.path("id").asText())) {
                return v;
            }
        }
        throw new AssertionError("版本列表里没有 " + id);
    }

    private String bar(String label, String phone) {
        String id = "tds_bar_" + label + "_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,contact_phone,project_id,status,created_at,updated_at)"
                + " VALUES(?,?,'bar',?,?,'active',now(),now())", id, id, phone == null ? null : piiCipher.encrypt(phone), project);
        return id;
    }

    /** 本项目一张已发布的酒吧通票，入场类权益覆盖给定酒吧。 */
    private void passTicketCovering(String... bars) {
        String type = "tds_tt_" + suffix;
        jdbc.update("INSERT INTO ticket_types(id,ticket_name,base_calendar_price_mode,stock_required,status,created_at,updated_at)"
                + " VALUES(?,'方案通告通票','fixed_price',FALSE,'published',now(),now())", type);
        for (String bar : bars) {
            String template = "tds_et_" + bar;
            jdbc.update("INSERT INTO entitlement_templates(id,name,entitlement_type,default_total_times,default_value_cent,status,merchant_id,"
                    + "created_at,updated_at) VALUES(?,'入场','entry',1,0,'active',?,now(),now())", template, bar);
            jdbc.update("INSERT INTO ticket_type_entitlements(id,ticket_type_id,entitlement_template_id,quantity,total_times,num_people,"
                    + "value_amount_cent,component_price_cent,created_at,updated_at) VALUES(?,?,?,1,1,1,0,5000,now(),now())",
                    "tds_bind_" + bar, type, template);
        }
    }
}
