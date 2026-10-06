package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.TemporaryGrant;
import com.complex.platform.common.crypto.PiiCipher;
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
 *
 * <p>不开测试事务（消耗要真实提交才看得见）。权益夹具照 UnlimitedInDayJdbcIntegrationTests（游乐当天无限、单人票）。
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

    @Autowired
    private PiiCipher piiCipher;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String userId = "usr_m723pv_" + suffix;
    private final String code = "m723pv-code-" + suffix;
    private final String deviceCode = "device_m723pv_" + suffix;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM entitlement_day_activations WHERE entitlement_id = ?", "ent_" + code);
        com.complex.platform.support.AppendOnlyTestBypass.update(
                jdbcTemplate, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
        jdbcTemplate.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", "ent_" + code);
        jdbcTemplate.update("DELETE FROM ticket_face_photos WHERE entitlement_id = ?", "ent_" + code);
        jdbcTemplate.update("DELETE FROM customer_consents WHERE id = ?", "consent_" + code);
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

    // 操作员 2026-10-04：核销预检附带显示人脸照「不算用掉」。只持单次「查看核销」授权（verification.view）的本商户员工
    // 取人脸照两次都 200，来源未消耗、没有消耗日志。修复前第一次取照就把授权扣掉，第二次 403。
    @Test
    void viewingTheFacePhotoShownWithThePrecheckDoesNotUseUpTheSingleUseGrant() throws Exception {
        seedEntitlement();
        // 照片行须指向同意台账里真实存在的一行（V397 外键）；同意以订单为主体落账
        jdbcTemplate.update("""
                INSERT INTO customer_consents (id, customer_id, consent_type, accepted, version, source_terminal,
                                               accepted_at, created_at, updated_at, order_id)
                VALUES (?, NULL, 'ticket_face_photo', TRUE, 'v', 'counter', now(), now(), now(), ?)
                """, "consent_" + code, "order_" + code);
        jdbcTemplate.update("""
                INSERT INTO ticket_face_photos (id, entitlement_id, photo_ciphertext, content_type, captured_at, purge_after, consent_id)
                VALUES (?, ?, ?, 'image/png', now(), CURRENT_DATE + 30, ?)
                """, "tfp_" + code, "ent_" + code,
                piiCipher.encrypt("bTcyMw==").getBytes(StandardCharsets.UTF_8), "consent_" + code);
        staffWithSingleUseGrant("verification.view");
        String url = "/api/v1/ticketing/issued-entitlements/ent_" + code + "/face-photo";

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get(url).with(session())).andExpect(status().isOk());
            assertUnconsumed();
        }
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
        return """
                {"idempotency_key": "%s", "credential_type": "qr", "credential_payload": "%s",
                 "device_id": "%s", "point_id": "point_%s", "business_context": {}}
                """.formatted(key, code, deviceCode, suffix);
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

    // 游乐当天无限、单人票（total_times=1）、虚拟权益签约价 5500 分，归属 MERCHANT；有效期前后各 10 天。
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
                VALUES (?, 'Amuse', 'amusement', 1, 0, 'active', ?, 'virtual', 5500, 'unlimited_in_day', ?, ?)
                """, templateId, venueId, ts, ts);
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.now(), "web", null, 0L, null, 0L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "amusement",
                MERCHANT, venueId, "point_" + suffix, 1, 0, 9999L, "active",
                now.minusSeconds(864000), now.plusSeconds(864000), now, now, 1, null,
                "virtual", 5500L, "unlimited_in_day", null, null, 0, null, null));
    }
}
