package com.complex.platform.parking;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 R2 真库、真接口：BSM L1428「道闸、车牌识别和计费由第三方停车系统执行，平台拿回入出场记录、缴费结果和车位占用；停车费率、
 * 消费抵免规则和月租车位在本平台后台配置并下发给停车系统」；L1444；L1438／L1448「超长滞留、逃费记录进异常队列」；
 * L1511「第三方可以在身份核验后读取或回写平台数据」。回写走开放平台回调（签名、防重放、幂等、适配器字段映射）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        ParkingFixture.SECRET_PROPERTY
})
@AutoConfigureMockMvc
@Transactional
class ParkingIngestJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private ParkingFixture f;

    @BeforeEach
    void seed() {
        f = new ParkingFixture(jdbc, "ing");
        f.vendor(true);
    }

    @Test
    void entryThenExitWebhookCreatesAndClosesSession() throws Exception {
        mockMvc.perform(f.webhook("vehicle_entered", "in_" + f.suffix,
                        Map.of("session_ref", "S1", "plate_no", "粤a 12345", "entered_at", "2026-10-21T02:00:00Z")))
                .andExpect(status().isOk());
        Map<String, Object> s = one("SELECT plate_no, status FROM parking_sessions WHERE project_id=? AND vendor_session_ref='S1'",
                f.project);
        assertThat(s.get("plate_no")).isEqualTo("粤A12345");
        assertThat(s.get("status")).isEqualTo("in_lot");

        mockMvc.perform(f.webhook("vehicle_exited", "out_" + f.suffix,
                        Map.of("session_ref", "S1", "exited_at", "2026-10-21T03:00:00Z", "fee_cent", 0, "paid_cent", 0)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM parking_sessions WHERE project_id=? AND vendor_session_ref='S1'",
                String.class, f.project)).isEqualTo("exited");
    }

    @Test
    void replayedIdempotencyKeyIsDeduplicated() throws Exception {
        Map<String, Object> entry = Map.of("session_ref", "S2", "plate_no", "粤B00001", "entered_at", "2026-10-21T02:00:00Z");
        mockMvc.perform(f.webhook("vehicle_entered", "dup_" + f.suffix, entry)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deduplicated").value(false));
        mockMvc.perform(f.webhook("vehicle_entered", "dup_" + f.suffix, entry)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deduplicated").value(true));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_sessions WHERE project_id=?", Integer.class, f.project)).isEqualTo(1);
    }

    @Test
    void exitWithUnpaidFeeRaisesFeeEvasionAndRiskAlert() throws Exception {
        String session = f.session("S3", "粤C00003", "2026-10-21T02:00:00Z");
        mockMvc.perform(f.webhook("vehicle_exited", "fe_" + f.suffix,
                        Map.of("session_ref", "S3", "exited_at", "2026-10-21T05:00:00Z", "fee_cent", 1500, "paid_cent", 0)))
                .andExpect(status().isOk());
        Map<String, Object> alert = one(
                "SELECT source_domain, alert_type, project_id, status FROM risk_alerts WHERE source_ref=?", session);
        assertThat(alert.get("source_domain")).isEqualTo("parking");
        assertThat(alert.get("alert_type")).isEqualTo("parking.fee_evasion");
        assertThat(alert.get("project_id")).isEqualTo(f.project);
        assertThat(alert.get("status")).isEqualTo("open");
    }

    @Test
    void vendorOverstayAnomalyEntersQueue() throws Exception {
        String session = f.session("S4", "粤D00004", "2026-10-19T02:00:00Z");
        mockMvc.perform(f.webhook("anomaly_reported", "os_" + f.suffix, Map.of("session_ref", "S4", "kind", "overstay")))
                .andExpect(status().isOk());
        assertThat(one("SELECT alert_type FROM risk_alerts WHERE source_ref=? AND source_domain='parking'", session)
                .get("alert_type")).isEqualTo("parking.overstay");
    }

    @Test
    void occupancyAndVendorPaymentWithoutPlatformRecordIsReconciled() throws Exception {
        f.session("S5", "粤E00005", "2026-10-21T02:00:00Z");
        mockMvc.perform(f.webhook("occupancy_reported", "occ_" + f.suffix, Map.of("reported_at", "2026-10-21T04:00:00Z",
                "total_spaces", 200, "occupied_total", 120, "occupied_monthly", 30, "occupied_staff", 10))).andExpect(status().isOk());
        assertThat(one("SELECT occupied_total FROM parking_occupancy_snapshots WHERE project_id=?", f.project).get("occupied_total"))
                .isEqualTo(120);
        mockMvc.perform(f.webhook("payment_reported", "pay_" + f.suffix, Map.of("payment_ref", "VP1", "session_ref", "S5",
                "amount_cent", 1000, "paid_at", "2026-10-21T04:10:00Z"))).andExpect(status().isOk());
        Map<String, Object> diff = one("""
                SELECT business_type, source_domain, difference_type, provider_amount_cent, internal_amount_cent
                  FROM integration_reconciliation_differences WHERE provider_code=? AND external_business_id='VP1'""", f.provider);
        assertThat(diff.get("business_type")).isEqualTo("parking");
        assertThat(diff.get("source_domain")).isEqualTo("thirdparty");
        assertThat(diff.get("difference_type")).isEqualTo("parking_payment_mismatch");
        assertThat(((Number) diff.get("provider_amount_cent")).longValue()).isEqualTo(1000L);
        assertThat(((Number) diff.get("internal_amount_cent")).longValue()).isZero();
    }

    @Test
    void unboundEndpointRejected() throws Exception {
        ParkingFixture other = new ParkingFixture(jdbc, "ing2");
        other.vendor(false);
        mockMvc.perform(other.webhook("vehicle_entered", "ub_" + other.suffix,
                        Map.of("session_ref", "S6", "plate_no", "粤F00006", "entered_at", "2026-10-21T02:00:00Z")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PARKING_ENDPOINT_NOT_BOUND"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_sessions WHERE vendor_session_ref='S6'", Integer.class)).isZero();
    }

    @Test
    void configPullSignedReturnsOwnProjectOnly() throws Exception {
        f.policy();
        mockMvc.perform(f.openGet("/parking/config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(f.project))
                .andExpect(jsonPath("$.data.free_minutes").value(30))
                .andExpect(jsonPath("$.data.hourly_rate_cent").value(500))
                .andExpect(jsonPath("$.data.daily_cap_cent").value(3000))
                .andExpect(jsonPath("$.data.exit_grace_minutes").value(15))
                .andExpect(jsonPath("$.data.credit_threshold_cent").value(20000))
                .andExpect(jsonPath("$.data.credit_minutes").value(120))
                .andExpect(jsonPath("$.data.staff_discount_bp").value(5000));
        // 未经应用凭证授权的端点拿不到。
        ParkingFixture other = new ParkingFixture(jdbc, "ing3");
        other.vendor(true);
        mockMvc.perform(f.openGet(other.appKey, "/parking/config")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("OPEN_ENDPOINT_FORBIDDEN"));
    }

    private Map<String, Object> one(String sql, Object... args) {
        java.util.List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        assertThat(rows).as(sql).hasSize(1);
        return rows.getFirst();
    }
}
