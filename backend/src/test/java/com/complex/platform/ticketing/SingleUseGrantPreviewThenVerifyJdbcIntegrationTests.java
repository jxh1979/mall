package com.complex.platform.ticketing;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.TemporaryGrant;
import com.complex.platform.common.api.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * M723（操作员 2026-10-03：「预览、查询不扣一次性授权，只有真正核销、真正执行的那一次才扣。李四可以先预览再核销。」）：
 * 真实库、真实接口回归。只持单次「执行核销」授权的员工先预览（POST /ticketing/verifications/preview）再核销
 * （POST /ticketing/verifications）：预览不消耗，核销成功且授权恰好用掉一次；之后再预览、再核销都 403。
 * 修复前预览就把授权扣掉，第一处「未消耗」断言即红，随后的核销 403。
 * 去掉 TicketingController.previewVerification 上的 DoesNotConsumeSingleUseGrant 标记则红。
 * 操作员 2026-10-04：核销预检附带显示人脸照「不算用掉」；价格仿真「不算用掉」——各一条。
 * M723-F04：预览用同一请求编号重复提交，响应不提示已完成（查看、预览、查询不提示）。
 * M723-F08（第 656 行「操作被拒绝或失败的不算用掉」）：以正常返回报告失败的核销（过刷新窗口、签名失配）不算用掉——失败核销记录保留、
 * 来源仍未消耗，同一请求编号重复提交返回第一次的失败结果，随后有效票码核销成功恰好消耗一次。
 * 边界守护：终端离线同步批次里有失败的离线核销时，同步操作本身成功，单次「终端设备绑定」授权照常算用掉。
 *
 * <p>不开测试事务（消耗要真实提交才看得见）。权益使用普通单次酒水，专门验证一次性授权；游乐营业日由专用真实库用例覆盖。
 * 消耗日志与审计事件只追加不删，按后缀隔离留在库里。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class SingleUseGrantPreviewThenVerifyJdbcIntegrationTests {

    private static final String VERIFY = "verification.execute";
    private static final String MERCHANT = "merchant_a";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private TicketingRepository ticketingRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String userId = "usr_m723pv_" + suffix;
    private final String code = "m723pv-code-" + suffix;
    private final String deviceCode = "device_m723pv_" + suffix;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM terminal_sync_tasks WHERE device_code = ?", "DC-" + deviceCode);
        jdbcTemplate.update("DELETE FROM entitlement_day_activations WHERE entitlement_id = ?", "ent_" + code);
        com.complex.platform.support.AppendOnlyTestBypass.update(
                jdbcTemplate, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
        jdbcTemplate.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", "ent_" + code);
        jdbcTemplate.update("DELETE FROM ticket_credentials WHERE entitlement_id = ?", "ent_" + code);
        jdbcTemplate.update("DELETE FROM issued_entitlements WHERE id = ?", "ent_" + code);
        jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + code);
        jdbcTemplate.update("DELETE FROM entitlement_templates WHERE id = ?", "et_" + code);
        jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", "tt_" + code);
        jdbcTemplate.update("DELETE FROM terminal_devices WHERE id = ?", deviceCode);
        jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE subject_id = ?", userId);
        jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", userId);
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "m723pv-%" + suffix);
    }

    @Test
    void aSingleUseVerifierCanPreviewFirstAndTheVerificationThenUsesTheGrantExactlyOnce() throws Exception {
        seedDevice();
        seedEntitlement();
        staffWithSingleUseGrant(VERIFY);

        // 预览两次：都放行、都不扣
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/ticketing/verifications/preview").with(session())
                            .contentType(MediaType.APPLICATION_JSON).content(verifyJson("m723pv-preview-" + i + "-" + suffix)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.allowed").value(true));
            assertUnconsumed();
            assertThat(usedTimes()).isZero();
        }

        // M723-F04：预览用同一请求编号重复提交（第二次由幂等层返回第一次的结果），两次响应消息都是 success，不提示已完成
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/ticketing/verifications/preview").with(session())
                            .contentType(MediaType.APPLICATION_JSON).content(verifyJson("m723pv-preview-same-" + suffix)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"));
            assertUnconsumed();
        }

        // 真正核销：成功，授权恰好用掉一次
        mockMvc.perform(post("/api/v1/ticketing/verifications").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content(verifyJson("m723pv-verify-" + suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"));
        assertConsumedOnce();
        assertThat(usedTimes()).isEqualTo(1);

        // 授权已用掉：再预览、再核销都被拒，不再有新的消耗
        mockMvc.perform(post("/api/v1/ticketing/verifications/preview").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content(verifyJson("m723pv-preview-after-" + suffix)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        mockMvc.perform(post("/api/v1/ticketing/verifications").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content(verifyJson("m723pv-verify-again-" + suffix)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertConsumedOnce();
        assertThat(usedTimes()).isEqualTo(1);
    }

    // M723-F08（第 656 行「操作被拒绝或失败的不算用掉」；操作员 2026-10-03「只有真正核销、真正执行的那一次才扣」）：只持单次「执行核销」
    // 授权的本商户员工经真实接口核销——①过刷新窗口的动态票码：200、status=failed／window_expired，失败核销记录在，来源未消耗，本次消耗
    // 已撤回（消耗日志只追加：一条 consumed 行对一条 released 行，有效消耗为零）；②签名失配：同上；③用①的请求编号重复提交：200、
    // 第一次的失败结果、提示已完成，仍未消耗；④随后用有效票码核销成功，恰好消耗一次。③放在④之前：④之后来源已被别的请求用掉，
    // 重复提交①走的是「已消耗来源的重放」判定，不是本项要证的路径。修复前①处「来源未消耗」即红（失败核销随请求事务按成功提交，
    // 单次来源一起用掉）。
    @Test
    void aVerificationThatReportsFailureInItsResponseDoesNotUseUpTheSingleUseGrant() throws Exception {
        seedDevice();
        seedEntitlement();
        staffWithSingleUseGrant(VERIFY);
        String expiredToken = "cred_m723f08_expired_" + suffix;
        String tamperedToken = "cred_m723f08_tampered_" + suffix;
        Instant now = Instant.now();
        // 过刷新窗口：窗口 60 秒、窗口起点 10 分钟前（窗口判定在签名核对之前，签名值不被读）；签名失配：窗口有效、签名值不是按规范串算出的
        seedCredential("tcred_expired_" + code, expiredToken, now.minusSeconds(600), "not-checked-window-expired");
        seedCredential("tcred_tampered_" + code, tamperedToken, now, "deadbeef");

        // ① 过刷新窗口
        String expiredKey = "m723pv-f08-expired-" + suffix;
        String first = mockMvc.perform(post("/api/v1/ticketing/verifications").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content(verifyJson(expiredKey, expiredToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.status").value("failed"))
                .andExpect(jsonPath("$.data.result_reason").value("window_expired"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(verificationOutcomes()).containsExactly("failed/window_expired");
        assertReleased(1);
        assertThat(usedTimes()).isZero();

        // ② 签名失配
        mockMvc.perform(post("/api/v1/ticketing/verifications").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content(verifyJson("m723pv-f08-tampered-" + suffix, tamperedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("failed"))
                .andExpect(jsonPath("$.data.result_reason").value("signature_invalid"));
        assertThat(verificationOutcomes()).containsExactly("failed/window_expired", "failed/signature_invalid");
        assertReleased(2);
        assertThat(usedTimes()).isZero();

        // ③ 用①的请求编号重复提交：幂等层返回第一次的失败结果并提示已完成，不再核销、不消耗（权限核验处新用掉的来源随请求事务回滚）
        String replayed = mockMvc.perform(post("/api/v1/ticketing/verifications").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content(verifyJson(expiredKey, expiredToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(data(replayed)).isEqualTo(data(first));
        assertThat(verificationOutcomes()).containsExactly("failed/window_expired", "failed/signature_invalid");
        assertReleased(2);

        // ④ 有效票码核销成功：恰好消耗一次
        mockMvc.perform(post("/api/v1/ticketing/verifications").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content(verifyJson("m723pv-f08-verify-" + suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"));
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, userId)).containsExactly(true);
        // 恰好消耗一次：新增一条 consumed、没有新的 released（①② 各留一对 consumed＋released，③ 随请求事务回滚不留行）
        assertThat(consumptionLogs()).isEqualTo(3);
        assertThat(releasedLogs()).isEqualTo(2);
        assertThat(verificationOutcomes()).hasSize(3);
        assertThat(verificationOutcomes().get(2)).startsWith("success/");
        assertThat(usedTimes()).isEqualTo(1);
    }

    // M723-F08 的边界守护：终端离线同步（POST /terminals/sync-tasks）与在线核销共用同一个核销方法，但同步批次受理即这次同步操作
    // 成功。只持单次「终端设备绑定」授权（terminal.device.bind）的本商户员工同步一条过刷新窗口的离线核销任务：200、同步任务 accepted、
    // 落一条 failed/window_expired 核销记录，本次用掉的单次授权照常算用掉——来源已消耗、恰好一条 consumed、没有 released；
    // 授权已用掉，再同步被拒。把业务失败标记放回共用的核销方法（verifyEntitlement）则红：切面会把这次消耗撤回。
    @Test
    void aTerminalSyncBatchWithAFailedOfflineVerificationStillUsesUpTheSingleUseGrant() throws Exception {
        seedDevice();
        seedEntitlement();
        staffWithSingleUseGrant("terminal.device.bind");
        String expiredToken = "cred_m723f08_sync_" + suffix;
        seedCredential("tcred_sync_" + code, expiredToken, Instant.now().minusSeconds(600), "not-checked-window-expired");

        mockMvc.perform(post("/api/v1/terminals/sync-tasks").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content(syncJson("m723pv-f08-sync-" + suffix, "task-1", expiredToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].status").value("accepted"));
        assertThat(verificationOutcomes()).containsExactly("failed/window_expired");
        assertThat(jdbcTemplate.queryForList("SELECT status FROM terminal_sync_tasks WHERE device_code = ?",
                String.class, "DC-" + deviceCode)).containsExactly("accepted");
        assertConsumedOnce();
        assertThat(releasedLogs()).isZero();
        assertThat(usedTimes()).isZero();

        mockMvc.perform(post("/api/v1/terminals/sync-tasks").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content(syncJson("m723pv-f08-sync-again-" + suffix, "task-2", expiredToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertConsumedOnce();
        assertThat(releasedLogs()).isZero();
    }

    // 离线同步请求：设备码（terminal_devices.device_code）＋一条离线核销任务，票码放在任务载荷里
    private String syncJson(String key, String clientTaskId, String credentialPayload) {
        return """
                {"idempotency_key": "%s", "device_code": "DC-%s", "staff_id": "%s",
                 "tasks": [{"client_task_id": "%s-%s", "task_type": "offline_verification", "occurred_at": "%s",
                            "payload": {"credentialPayload": "%s", "pointId": "point_%s"}}]}
                """.formatted(key, deviceCode, userId, clientTaskId, suffix, Instant.now(), credentialPayload, suffix);
    }

    // 来源未消耗；本次消耗已撤回：consumed 行与 released 行各 rows 条、相抵为零
    private void assertReleased(int rows) {
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, userId)).containsExactly(false);
        assertThat(consumptionLogs()).isEqualTo(rows);
        assertThat(releasedLogs()).isEqualTo(rows);
    }

    private Integer releasedLogs() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM permission_template_usage_logs WHERE user_id = ? AND result = 'released'",
                Integer.class, userId);
    }

    private List<String> verificationOutcomes() {
        return jdbcTemplate.queryForList(
                "SELECT status || '/' || coalesce(result_reason, '') FROM ticket_verifications WHERE entitlement_id = ? ORDER BY created_at",
                String.class, "ent_" + code);
    }

    private JsonNode data(String body) throws Exception {
        return new ObjectMapper().readTree(body).get("data");
    }

    // 动态票码（cred_ 前缀）：核销按 window_started_at + refresh_interval_seconds（60 秒）判窗口，再按签名值核对；
    // 票码以哈希存库（saveTicketCredential 自行计算）。
    private void seedCredential(String id, String token, Instant windowStartedAt, String signatureValue) {
        Instant now = Instant.now();
        ticketingRepository.saveTicketCredential(new TicketCredentialRecord(
                id, "ent_" + code, "order_" + code, token, "nonce_" + id, null, "dynamic_qr",
                windowStartedAt, now.plusSeconds(3600), "active", 1, now, now, 60,
                windowStartedAt, signatureValue, "v1"));
    }

    // 操作员 2026-10-04：价格仿真「不算用掉」。只持单次「动态定价仿真」授权（ticket.dynamic_pricing.simulate）的员工仿真两次都 200，
    // 来源未消耗、没有消耗日志。修复前第一次仿真就把授权扣掉，第二次 403。
    // 去掉 TicketingController.simulateDynamicPricing 上的 DoesNotConsumeSingleUseGrant 标记则红。
    @Test
    void aPriceSimulationDoesNotUseUpTheSingleUseGrant() throws Exception {
        java.sql.Timestamp ts = java.sql.Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, weekday_price_cent, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'M723 Type', 'fixed', 10000, FALSE, 'active', ?, ?)
                """, "tt_" + code, ts, ts);
        staffWithSingleUseGrant("ticket.dynamic_pricing.simulate");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing/simulations").with(session())
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"idempotency_key": "m723pv-simulate-%d-%s", "ticket_type_id": "%s", "sales_channel": "web"}
                                    """.formatted(i, suffix, "tt_" + code)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.base_price_cent").value(10000));
            assertUnconsumed();
        }
    }

    private void staffWithSingleUseGrant(String permissionCode) {
        Instant now = Instant.now();
        accessRepository.createUser(new AccessUser(userId, userId, "staff", "org", MERCHANT, "active", now));
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId, List.of(permissionCode),
                now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "M723 单次授权回归",
                "seed-admin", now, null, null));
        assertUnconsumed();
    }

    private String verifyJson(String key) {
        return verifyJson(key, code);
    }

    private String verifyJson(String key, String credentialPayload) {
        return """
                {"idempotency_key": "%s", "credential_type": "qr", "credential_payload": "%s",
                 "device_id": "%s", "point_id": "point_%s", "business_context": {}}
                """.formatted(key, credentialPayload, deviceCode, suffix);
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与员工表当前行一致）。
    private RequestPostProcessor session() {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(userId, userId, "staff", "org", MERCHANT, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private void assertUnconsumed() {
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, userId)).containsExactly(false);
        assertThat(consumptionLogs()).isZero();
    }

    private void assertConsumedOnce() {
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, userId)).containsExactly(true);
        assertThat(consumptionLogs()).isEqualTo(1);
    }

    private Integer consumptionLogs() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM permission_template_usage_logs WHERE user_id = ? AND result = 'consumed'",
                Integer.class, userId);
    }

    private int usedTimes() {
        return jdbcTemplate.queryForObject(
                "SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, "ent_" + code);
    }

    private void seedDevice() {
        java.sql.Timestamp ts = java.sql.Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    status, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, ?)
                """, deviceCode, "dev-" + deviceCode, "DC-" + deviceCode, ts, ts);
    }

    // 普通单次酒水权益，归属 MERCHANT；有效期前后各 10 天。
    private void seedEntitlement() {
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        String venueId = "venue_m723pv_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'M723 Type', 'fixed', FALSE, 'active', ?, ?)
                """, typeId, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, venue_id, entitlement_kind, agreement_price_cent,
                    consumption_window, created_at, updated_at)
                VALUES (?, '单次酒水', 'bar_drink', 1, 0, 'active', ?, 'virtual', 5500, 'per_use', ?, ?)
                """, templateId, venueId, ts, ts);
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.now(), "web", null, 0L, null, 0L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "bar_drink",
                MERCHANT, venueId, "point_" + suffix, 1, 0, 9999L, "active",
                now.minusSeconds(864000), now.plusSeconds(864000), now, now, 1, null,
                "virtual", 5500L, "per_use", null, null, 0, null, null));
    }
}
