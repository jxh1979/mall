package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
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
 * 第十八批 AK 段第 230 条 230-R1 人工取消订台限店长及以上、原因必填进留痕（真库）。依据：BSM:1058「已预留只能经取消订台、超时释放、到店开台三条路径
 * 退出；店长及以上可人工取消预留，必须填原因并留痕。」；ENUM:100 同句；PGM:966「所有有过程的业务动作都必须留痕」。
 * 后台请求按「卡座与订台」页取消订台的真实形状（蛇形键 idempotency_key、reason），走真实入口与真实授权链（access_users＋角色→权限码）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarReservationManualCancelJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarReservationTimeoutJob timeoutJob;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "brc_prj_" + s;
    private final String bar = "brc_bar_" + s;

    private StaffRequestFixture staffs;
    /** 店长：持人工取消订台权限（不持订台管理权限）。 */
    private RequestPostProcessor manager;
    /** 店员：只持员工代订与订台管理权限。 */
    private RequestPostProcessor clerk;
    private String seat;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','取消订台综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'取消订台酒吧','bar','active',?,now(),now())",
                bar, project);
        seat = "brc_table_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '取消订台回归', 'card_seat', 6, 2, 0, 'CNY', 'active', now(), now())", seat, bar, bar, "T-" + s);
        staffs = new StaffRequestFixture(jdbc);
        manager = staffs.staff("usr_brc_manager_" + s, "staff", project, bar, "bar.reservation.cancel", "bar.reservation.create");
        clerk = staffs.staff("usr_brc_clerk_" + s, "staff", project, bar, "bar.reservation.manage", "bar.reservation.create");
    }

    @Test
    void managerCancelsReservationWithReasonRecordedAndTableReleased() throws Exception {
        String reservation = reserve("first");
        assertThat(row(reservation)).containsEntry("status", "reserved").containsEntry("hold_status", "held");

        cancel(manager, reservation, "cancel", "客人来电取消")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));

        assertThat(row(reservation)).containsEntry("status", "cancelled").containsEntry("hold_status", "released");
        assertThat(jdbc.queryForMap("SELECT metadata->>'reason' AS reason, metadata->>'no_show' AS no_show, actor_id, permission_code"
                + " FROM audit_events WHERE event_name = 'bar.reservation.cancelled' AND business_id = ?", reservation))
                .containsEntry("reason", "客人来电取消").containsEntry("no_show", "false")
                .containsEntry("actor_id", "usr_brc_manager_" + s).containsEntry("permission_code", "bar.reservation.cancel");
        // 占台已释放：同一台位可再被预订。
        assertThat(row(reserve("again"))).containsEntry("hold_status", "held");
    }

    @Test
    void cancelWithoutReasonIsRejectedWhileTimeoutReleaseNeedsNoReason() throws Exception {
        String reservation = reserve("no-reason");

        cancel(manager, reservation, "cancel-missing", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        cancel(manager, reservation, "cancel-blank", "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(row(reservation)).containsEntry("status", "reserved").containsEntry("hold_status", "held");
        assertThat(cancelledEvents(reservation)).isZero();

        // 超时释放不经人工取消入口：照常释放，不要求原因。
        jdbc.update("UPDATE bar_reservations SET latest_arrival_at = ? WHERE id = ?", Timestamp.from(Instant.now().minusSeconds(600)), reservation);
        timeoutJob.releaseOverdue();
        assertThat(row(reservation)).containsEntry("status", "cancelled").containsEntry("hold_status", "released");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.reservation_timeout_released' AND business_id = ?",
                Integer.class, reservation)).isEqualTo(1);
    }

    @Test
    void staffHoldingOnlyReservationManageCannotCancel() throws Exception {
        String reservation = reserve("clerk");

        cancel(clerk, reservation, "cancel-clerk", "店员想取消").andExpect(status().isForbidden());

        assertThat(row(reservation)).containsEntry("status", "reserved").containsEntry("hold_status", "held");
        assertThat(cancelledEvents(reservation)).isZero();
    }

    @Test
    void otherMerchantManagerIsRejectedBeforeIdempotency() throws Exception {
        String reservation = reserve("cross");
        String otherBar = "brc_bar2_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'隔壁酒吧','bar','active',?,now(),now())",
                otherBar, project);
        RequestPostProcessor otherManager = staffs.staff("usr_brc_manager2_" + s, "staff", project, otherBar, "bar.reservation.cancel");

        cancel(otherManager, reservation, "cancel-cross", "取消别家的订台")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("BAR_MERCHANT_SCOPE"));

        assertThat(row(reservation)).containsEntry("status", "reserved").containsEntry("hold_status", "held");
        // 商户闸在幂等之前：被拒的那次不落幂等记录。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_records WHERE idempotency_key = ?", Integer.class,
                "brc-cancel-cross-" + s)).isZero();
    }

    // ---------- helpers ----------

    /** 后台代订（新建订台表单的真实请求形状）：提交成功即占台。 */
    private String reserve(String tag) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "brc-book-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", seat);
        body.put("guest_count", 2);
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", Instant.now().plusSeconds(7200).toString());
        body.put("customer_phone", "13900001234");
        String response = mockMvc.perform(post("/api/v1/bar/reservations").with(manager)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(response).path("data").path("id").asText();
    }

    /** 后台「卡座与订台」页取消订台的请求形状：原因必填。 */
    private ResultActions cancel(RequestPostProcessor who, String reservationId, String tag, String reason) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "brc-" + tag + "-" + s);
        if (reason != null) {
            body.put("reason", reason);
        }
        return mockMvc.perform(post("/api/v1/bar/reservations/" + reservationId + "/cancel").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private Map<String, Object> row(String reservationId) {
        return jdbc.queryForMap("SELECT status, hold_status FROM bar_reservations WHERE id = ?", reservationId);
    }

    private int cancelledEvents(String reservationId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.reservation.cancelled' AND business_id = ?",
                Integer.class, reservationId);
    }
}
