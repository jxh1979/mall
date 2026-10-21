package com.complex.platform.parking;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 R9 真库、真接口（请求体按前端蛇形键）：BSM L1438「识别失败、道闸故障、客人对缴费有争议时，由当班保安人工核对后放行，
 * 人工放行必须记录原因、操作人、车牌和时间，进留痕与异常；免费放行和减免须经物业主管审批」；L1448「人工放行、减免……留痕并进异常队列」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        ParkingFixture.SECRET_PROPERTY
})
@AutoConfigureMockMvc
@Transactional
class ParkingManualReleaseJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SupervisorReviewService reviews;

    private ParkingFixture f;
    private RequestPostProcessor guard;

    @BeforeEach
    void seed() {
        f = new ParkingFixture(jdbc, "rel");
        f.policy();
        f.vendor(true);
        guard = f.account("guard", f.role("guard", "parking.release.record"), null);
        f.account("boss", f.role("boss", "parking.release.approve", "governance.supervisor_review.view",
                "governance.supervisor_review.approve"), null);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", f.user("guard"), f.user("boss"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void offShiftGuardRejected() throws Exception {
        release("{\"plate_no\":\"粤A10001\",\"release_reason\":\"gate_fault\",\"reason\":\"道闸卡杆\",\"waive_type\":\"none\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PARKING_GUARD_NOT_ON_DUTY"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_manual_releases WHERE project_id=?", Integer.class, f.project)).isZero();
    }

    @Test
    void releaseRecordsReasonOperatorPlateTimeAndRiskAlert() throws Exception {
        onShift();
        String id = id(release("{\"plate_no\":\"粤a 10002\",\"release_reason\":\"recognition_failed\",\"reason\":\"车牌污损识别失败\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("released"))
                .andExpect(jsonPath("$.data.operator_id").value(f.user("guard"))));
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT plate_no, release_reason, reason, operator_id, released_at, operator_shift_id, risk_alert_id
                  FROM parking_manual_releases WHERE id=?""", id);
        assertThat(row.get("plate_no")).isEqualTo("粤A10002");
        assertThat(row.get("release_reason")).isEqualTo("recognition_failed");
        assertThat(row.get("reason")).isEqualTo("车牌污损识别失败");
        assertThat(row.get("released_at")).isNotNull();
        assertThat(row.get("operator_shift_id")).isEqualTo("pk_shift_" + f.suffix);
        Map<String, Object> alert = jdbc.queryForMap("SELECT source_domain, alert_type, summary, project_id FROM risk_alerts WHERE alert_id=?",
                row.get("risk_alert_id"));
        assertThat(alert.get("source_domain")).isEqualTo("parking");
        assertThat(alert.get("alert_type")).isEqualTo("parking.manual_release");
        assertThat((String) alert.get("summary")).contains("粤A10002", "识别失败", f.user("guard"));
    }

    @Test
    void missingReasonOrPlateRejected() throws Exception {
        onShift();
        release("{\"plate_no\":\"粤A10003\",\"release_reason\":\"gate_fault\"}").andExpect(status().isBadRequest());
        release("{\"release_reason\":\"gate_fault\",\"reason\":\"道闸故障\"}").andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_manual_releases WHERE project_id=?", Integer.class, f.project)).isZero();
    }

    @Test
    void freeReleaseRequiresSupervisorApproval() throws Exception {
        onShift();
        String id = id(release("{\"plate_no\":\"粤A10004\",\"release_reason\":\"fee_dispute\",\"reason\":\"客人称已缴\","
                + "\"waive_type\":\"free\",\"final_approver_id\":\"" + f.user("boss") + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval")));
        String review = jdbc.queryForObject("SELECT review_id FROM parking_manual_releases WHERE id=?", String.class, id);
        assertThat(review).isNotNull();
        // 发起的保安自己不能审批。
        assertThatThrownBy(() -> decide(f.user("guard"), review, "approved")).isInstanceOf(RuntimeException.class);
        decide(f.user("boss"), review, "approved");
        assertThat(jdbc.queryForObject("SELECT status FROM parking_manual_releases WHERE id=?", String.class, id)).isEqualTo("approved");
    }

    @Test
    void approvedReductionSuppressesFeeEvasion() throws Exception {
        onShift();
        String covered = f.session("R1", "粤A10005", "2026-10-21T02:00:00Z");
        String review = jdbc.queryForObject("SELECT review_id FROM parking_manual_releases WHERE id=?", String.class,
                id(release("{\"plate_no\":\"粤A10005\",\"release_reason\":\"fee_dispute\",\"reason\":\"减免争议部分\","
                        + "\"waive_type\":\"reduction\",\"reduction_cent\":500,\"final_approver_id\":\"" + f.user("boss") + "\"}")
                        .andExpect(status().isOk())));
        decide(f.user("boss"), review, "approved");
        exit("R1", 500);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM risk_alerts WHERE source_ref=? AND alert_type='parking.fee_evasion'",
                Integer.class, covered)).isZero();

        // 对照：减免被驳回的，出场欠费照常进逃费异常。
        String rejected = f.session("R2", "粤A10006", "2026-10-21T02:00:00Z");
        String review2 = jdbc.queryForObject("SELECT review_id FROM parking_manual_releases WHERE id=?", String.class,
                id(release("{\"plate_no\":\"粤A10006\",\"release_reason\":\"fee_dispute\",\"reason\":\"减免争议部分\","
                        + "\"waive_type\":\"reduction\",\"reduction_cent\":500,\"final_approver_id\":\"" + f.user("boss") + "\"}")
                        .andExpect(status().isOk())));
        exit("R2", 500);
        decide(f.user("boss"), review2, "rejected");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM risk_alerts WHERE source_ref=? AND alert_type='parking.fee_evasion'",
                Integer.class, rejected)).isEqualTo(1);
    }

    /** 保安先在停车系统开闸、出场回写先到，再到平台登记：放行记录关联同车牌本营业日刚离场的那条会话。 */
    @Test
    void releaseAfterExitLinksJustExitedSession() throws Exception {
        onShift();
        String session = f.session("R3", "粤A10007", java.time.Instant.now().minusSeconds(3600).toString());
        mockMvc.perform(f.webhook("vehicle_exited", "x_R3_" + f.suffix, Map.of("session_ref", "R3", "exited_at",
                java.time.Instant.now().minusSeconds(5).toString(), "fee_cent", 500, "paid_cent", 0))).andExpect(status().isOk());
        String id = id(release("{\"plate_no\":\"粤A10007\",\"release_reason\":\"fee_dispute\",\"reason\":\"客人称已扫码缴费\","
                + "\"waive_type\":\"free\",\"final_approver_id\":\"" + f.user("boss") + "\"}").andExpect(status().isOk()));
        assertThat(jdbc.queryForObject("SELECT session_id FROM parking_manual_releases WHERE id=?", String.class, id)).isEqualTo(session);
    }

    private void onShift() {
        jdbc.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, project_id)
                VALUES (?, ?, current_date, now() - interval '2 hours', now() + interval '6 hours', ?)""",
                "pk_shift_" + f.suffix, f.user("guard"), f.project);
    }

    private ResultActions release(String json) throws Exception {
        String body = json.substring(0, json.length() - 1) + ",\"project_id\":\"" + f.project + "\",\"idempotency_key\":\"rel_"
                + UUID.randomUUID() + "\"}";
        return mockMvc.perform(post("/api/v1/parking/manual-releases").with(guard).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void exit(String ref, long fee) throws Exception {
        mockMvc.perform(f.webhook("vehicle_exited", "x_" + ref + "_" + f.suffix,
                Map.of("session_ref", ref, "exited_at", "2026-10-21T06:00:00Z", "fee_cent", fee, "paid_cent", 0))).andExpect(status().isOk());
    }

    private void decide(String userId, String review, String decision) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", f.project, null, "active"), "test", List.of()));
        try {
            reviews.approve(review, new SupervisorReviewApprovalRequest("dec_" + UUID.randomUUID(), "核对放行原因", List.of(), null,
                    decision, "物业主管审批"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }
}
