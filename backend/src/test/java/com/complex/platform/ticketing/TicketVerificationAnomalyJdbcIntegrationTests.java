package com.complex.platform.ticketing;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.DailyCloseActionRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.support.DailyCloseFixture;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
 * TK-14 票务核销异常复核真库回归（ENUM:43「核销记录状态」：「平台已记录核销成功并扣减次数，但现场未放行或实际权益交付存在争议、需要主管核实时，
 * 进入异常复核并保留原成功记录与扣次事实；主管核实后按既有冲正、恢复次数或补偿规则处理，不自动退款。普通规则拒绝仍记「拒绝」，
 * 明确的设备或网络失败仍记「失败」，不得一律改为异常」；ENUM:221「异常状态的统一出口」：「必须由主管在系统内选择“更正后恢复”或
 * “作废并冲正”两种方式之一处置，填写原因并留痕……异常单据未处置完毕，所属日结不得确认锁定」；CG #45）。
 * 请求一律按前端真实形状（adminClient.mapKeys 蛇形键）经 MockMvc 发出。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketVerificationAnomalyJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    // 远离真实数据的营业日：日结锁定只碰这一天。
    private static final LocalDate BUSINESS_DATE = LocalDate.parse("2014-05-21");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FinanceService financeService;
    @Autowired private com.complex.platform.finance.FinanceRepository financeRepository;
    @Autowired private BusinessDayResolver businessDayResolver;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String merchant = "mch_tk14_" + suffix;
    private final String orderId = "ord_tk14_" + suffix;
    private final String entitlementId = "ent_tk14_" + suffix;
    private final String deviceId = "dev_tk14_" + suffix;
    private RequestPostProcessor verifier;
    private RequestPostProcessor supervisor;
    private RequestPostProcessor otherMerchantVerifier;
    private int seq;

    @BeforeEach
    void seed() {
        Instant now = Instant.now();
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '异常复核测试票', 'flat', 'published', now(), now())", "tt_tk14_" + suffix);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " created_at, updated_at) VALUES (?, '入场', 'bar_entry', 2, 0, 'active', now(), now())", "et_tk14_" + suffix);
        jdbc.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status,"
                + " issue_status, status, created_at, updated_at, merchant_id) VALUES (?, ?, ?, 1, 'web', 5000, 'paid', 'issued', 'created',"
                + " now(), now(), ?)", orderId, "ON-" + suffix, "tt_tk14_" + suffix, merchant);
        // 两次票，已成功核销一次（used_times=1）。
        jdbc.update("INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,"
                + " entitlement_type, total_times, used_times, status, valid_from, valid_to, created_at, updated_at, merchant_id)"
                + " VALUES (?, ?, ?, ?, ?, 'bar_entry', 2, 1, 'active', ?, ?, now(), now(), ?)",
                entitlementId, "tt_tk14_" + suffix, orderId, "et_tk14_" + suffix, "TK14-" + suffix,
                Timestamp.from(now.minusSeconds(86_400)), Timestamp.from(now.plusSeconds(86_400)), merchant);
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', now(), now())", deviceId, "hw-" + deviceId, "DC-" + deviceId);
        verifier = staff("tk14_verifier_" + suffix, "staff", merchant, "verification.execute", "verification.view");
        // 主管为平台侧账号（不挂商户），处置与冲正两项高风险码逐项授予；普通冲正的合同准入闸只对挂商户的员工按其合同判定。
        supervisor = staff("tk14_supervisor_" + suffix, "platform_admin", null, "ticket.verify.anomaly.resolve", "ticket.verify.reverse");
        otherMerchantVerifier = staff("tk14_other_" + suffix, "staff", "mch_tk14_other_" + suffix, "verification.execute");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // R1 RED：成功核销提起异常 → 新增 anomaly 行指向原记录；原记录仍是 success、扣次不变、不生成退款。
    @Test
    void raiseAnomalyOnSuccessKeepsOriginalSuccessAndUsedTimes() throws Exception {
        String original = verification("success", Instant.now());
        String anomalyId = raise(original, "现场闸机未放行，客人称已扣次").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("anomaly"))
                .andExpect(jsonPath("$.data.anomaly_of_verification_id").value(original))
                .andReturn().getResponse().getContentAsString().transform(this::dataId);

        Map<String, Object> anomaly = jdbc.queryForMap("SELECT * FROM ticket_verifications WHERE id = ?", anomalyId);
        assertThat(anomaly.get("status")).isEqualTo("anomaly");
        assertThat(anomaly.get("anomaly_of_verification_id")).isEqualTo(original);
        assertThat(anomaly.get("result_reason")).isEqualTo("现场闸机未放行，客人称已扣次");
        assertThat(anomaly.get("source_type")).isEqualTo("verification_anomaly");
        assertThat(anomaly.get("entitlement_id")).isEqualTo(entitlementId);
        assertThat(anomaly.get("merchant_id")).isEqualTo(merchant);
        assertThat(anomaly.get("settlement_price_cent")).isNull();
        assertThat(anomaly.get("anomaly_resolution")).isNull();
        assertThat(statusOf(original)).isEqualTo("success");
        assertThat(usedTimes()).isEqualTo(1);
        assertThat(refunds()).isZero();
    }

    // R1 RED：拒绝、失败的核销不得提起异常（不得一律改为异常）。
    @Test
    void raiseAnomalyOnRejectedOrFailedIsRefused() throws Exception {
        for (String state : List.of("rejected", "failed")) {
            String id = verification(state, Instant.now());
            raise(id, "争议").andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail_code").value("VERIFICATION_ANOMALY_SOURCE_NOT_SUCCESS"));
        }
        assertThat(anomalyRows()).isZero();
    }

    @Test
    void duplicateOpenAnomalyIsRefused() throws Exception {
        String original = verification("success", Instant.now());
        raise(original, "第一次提起").andExpect(status().isOk());
        raise(original, "第二次提起").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VERIFICATION_ANOMALY_OPEN"));
        assertThat(anomalyRows()).isEqualTo(1);
    }

    @Test
    void raiseAnomalyAcrossMerchantIsForbidden() throws Exception {
        String original = verification("success", Instant.now());
        mockMvc.perform(post("/api/v1/ticketing/verifications/" + original + "/anomaly").with(otherMerchantVerifier)
                        .contentType(MediaType.APPLICATION_JSON).content(body("reason", "跨商户")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(anomalyRows()).isZero();
    }

    // R2 RED：更正后恢复——只写处置留痕，原成功记录与扣次不动、不退款。
    @Test
    void resolveRestoreClosesAnomalyAndOriginalStaysSuccess() throws Exception {
        String original = verification("success", Instant.now());
        String anomalyId = raisedAnomaly(original);
        resolve(anomalyId, "restored", "核实现场已放行").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.anomaly_resolution").value("restored"));

        Map<String, Object> anomaly = jdbc.queryForMap("SELECT * FROM ticket_verifications WHERE id = ?", anomalyId);
        assertThat(anomaly.get("anomaly_resolution")).isEqualTo("restored");
        assertThat(anomaly.get("anomaly_resolution_reason")).isEqualTo("核实现场已放行");
        assertThat(anomaly.get("anomaly_resolved_by")).isEqualTo("tk14_supervisor_" + suffix);
        assertThat(anomaly.get("anomaly_resolved_at")).isNotNull();
        assertThat(statusOf(original)).isEqualTo("success");
        assertThat(usedTimes()).isEqualTo(1);
        assertThat(refunds()).isZero();
    }

    // R2 RED：作废并冲正——走既有冲正：原记录 reversed、恢复次数、新增 reversed 记录；不退款。
    @Test
    void resolveVoidReversesOriginalRestoresCountWithoutRefund() throws Exception {
        String original = verification("success", Instant.now());
        String anomalyId = raisedAnomaly(original);
        resolve(anomalyId, "voided_reversed", "核实现场未放行").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.anomaly_resolution").value("voided_reversed"));

        assertThat(statusOf(original)).isEqualTo("reversed");
        assertThat(usedTimes()).isZero();
        assertThat(jdbc.queryForObject("SELECT result_reason FROM ticket_verifications WHERE reverses_verification_id = ?",
                String.class, original)).isEqualTo("异常复核作废：核实现场未放行");
        assertThat(jdbc.queryForObject("SELECT anomaly_resolution FROM ticket_verifications WHERE id = ?", String.class, anomalyId))
                .isEqualTo("voided_reversed");
        assertThat(refunds()).isZero();
    }

    // PGM:707「不符合合同或基础定义的后续流程必须先被卡住，再按审批链逐级审批放行」：作废并冲正与直接冲正同走合同准入闸
    // （V099 ticketing_verify_reverse_over_scope）——商户生效合同经营范围不含票务时拦进复核，原记录与次数不变。
    @Test
    void resolveVoidBlockedWhenMerchantContractExcludesTicketing() throws Exception {
        jdbc.update("INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,"
                + " lifecycle_status, allowed_business_scope, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'effective',"
                + " '[\"bar\"]'::jsonb, now(), now())", "ctr_tk14_" + suffix, merchant, "cs_tk14_" + suffix, "ss_tk14_" + suffix,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        RequestPostProcessor merchantSupervisor = staff("tk14_msup_" + suffix, "staff", merchant, "ticket.verify.anomaly.resolve");
        String original = verification("success", Instant.now());
        String anomalyId = raisedAnomaly(original);
        mockMvc.perform(post("/api/v1/ticketing/verification-anomalies/" + anomalyId + "/resolve").with(merchantSupervisor)
                        .contentType(MediaType.APPLICATION_JSON).content(body("resolution", "voided_reversed", "reason", "核实未放行")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        assertThat(statusOf(original)).isEqualTo("success");
        assertThat(usedTimes()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT anomaly_resolution FROM ticket_verifications WHERE id = ?", String.class, anomalyId))
                .isNull();
    }

    @Test
    void resolveWithoutResolvePermissionReturnsPermissionDenied() throws Exception {
        String anomalyId = raisedAnomaly(verification("success", Instant.now()));
        mockMvc.perform(post("/api/v1/ticketing/verification-anomalies/" + anomalyId + "/resolve").with(verifier)
                        .contentType(MediaType.APPLICATION_JSON).content(body("resolution", "restored", "reason", "无权处置")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertThat(jdbc.queryForObject("SELECT anomaly_resolution FROM ticket_verifications WHERE id = ?", String.class, anomalyId))
                .isNull();
    }

    @Test
    void resolveRequiresReason() throws Exception {
        String anomalyId = raisedAnomaly(verification("success", Instant.now()));
        resolve(anomalyId, "restored", " ").andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT anomaly_resolution FROM ticket_verifications WHERE id = ?", String.class, anomalyId))
                .isNull();
    }

    @Test
    void plainReverseBlockedWhileAnomalyOpen() throws Exception {
        String original = verification("success", Instant.now());
        String anomalyId = raisedAnomaly(original);
        mockMvc.perform(post("/api/v1/ticketing/verifications/" + original + "/reverse").with(supervisor)
                        .contentType(MediaType.APPLICATION_JSON).content(body("reason", "普通冲正")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VERIFICATION_ANOMALY_OPEN"));
        assertThat(statusOf(original)).isEqualTo("success");
        assertThat(usedTimes()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT anomaly_resolution FROM ticket_verifications WHERE id = ?", String.class, anomalyId))
                .isNull();
    }

    // 异常处置完毕（更正后恢复）后，普通冲正照常可用。
    @Test
    void plainReverseAllowedAfterAnomalyResolved() throws Exception {
        String original = verification("success", Instant.now());
        resolve(raisedAnomaly(original), "restored", "核实已放行").andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/ticketing/verifications/" + original + "/reverse").with(supervisor)
                        .contentType(MediaType.APPLICATION_JSON).content(body("reason", "普通冲正")))
                .andExpect(status().isOk());
        assertThat(statusOf(original)).isEqualTo("reversed");
        assertThat(usedTimes()).isZero();
    }

    @Test
    void resolveTwiceIsRefused() throws Exception {
        String original = verification("success", Instant.now());
        String anomalyId = raisedAnomaly(original);
        resolve(anomalyId, "restored", "核实已放行").andExpect(status().isOk());
        resolve(anomalyId, "voided_reversed", "改作废").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VERIFICATION_ANOMALY_ALREADY_RESOLVED"));
        assertThat(statusOf(original)).isEqualTo("success");
        assertThat(usedTimes()).isEqualTo(1);
    }

    // R3 RED：原核销落在该营业日、异常待处置 → 日结不得确认（第十八批 R 段 143-R1：闸挪到确认与锁定，生成照常）；处置成 restored 后可确认。
    @Test
    void confirmDailyCloseRefusedWhileTicketVerificationAnomalyOpen() throws Exception {
        String original = verification("success", businessDayResolver.start(null, null, BUSINESS_DATE).plusSeconds(3600));
        // 直接落一条待处置异常（提起入口另有用例），只验日结闸。
        String anomalyId = "ver_tk14_anomaly_" + suffix;
        jdbc.update("INSERT INTO ticket_verifications (id, verification_no, entitlement_id, entitlement_code, credential_type,"
                + " credential_payload, device_id, point_id, status, result_reason, source_type, created_at, merchant_id, verified_people,"
                + " anomaly_of_verification_id) VALUES (?, ?, ?, ?, 'qr', 'payload', ?, 'point_tk14', 'anomaly', '现场未放行',"
                + " 'verification_anomaly', now(), ?, 1, ?)", anomalyId, "V" + anomalyId, entitlementId, "TK14-" + suffix, deviceId,
                merchant, original);

        DailyCloseFixture.asFinanceAdmin(jdbc, "usr_dc_fin_" + suffix);
        String close = financeService.createDailyClose(dailyClose()).id();
        DailyCloseFixture.markAllFinal(financeRepository, close);
        assertThatThrownBy(() -> financeService.confirmDailyClose(close, closeAction()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DAY_CLOSE_ANOMALY_PENDING"));
        assertThat(jdbc.queryForObject("SELECT status FROM finance_daily_closes WHERE id = ?", String.class, close))
                .isEqualTo("pending_confirm");

        SecurityContextHolder.clearContext();
        resolve(anomalyId, "restored", "核实已放行").andExpect(status().isOk());
        DailyCloseFixture.asFinanceAdmin(jdbc, "usr_dc_fin_" + suffix);
        assertThat(financeService.confirmDailyClose(close, closeAction()).status()).isEqualTo("confirmed");
    }

    private DailyCloseActionRequest closeAction() {
        return new DailyCloseActionRequest("tk14-close-action-" + suffix + "-" + (++seq), null);
    }

    private DailyCloseRequest dailyClose() {
        return new DailyCloseRequest("tk14-close-" + suffix + "-" + (++seq), "日结", List.of(), BUSINESS_DATE, "merchant", merchant);
    }

    private ResultActions raise(String verificationId, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/verifications/" + verificationId + "/anomaly").with(verifier)
                .contentType(MediaType.APPLICATION_JSON).content(body("reason", reason)));
    }

    private String raisedAnomaly(String original) throws Exception {
        return raise(original, "现场未放行").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString().transform(this::dataId);
    }

    private ResultActions resolve(String anomalyId, String resolution, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/verification-anomalies/" + anomalyId + "/resolve").with(supervisor)
                .contentType(MediaType.APPLICATION_JSON).content(body("resolution", resolution, "reason", reason)));
    }

    // 前端真实请求形状：adminClient 自动补 idempotency_key，键名蛇形。
    private String body(String... pairs) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tk14-" + suffix + "-" + (++seq));
        for (int i = 0; i < pairs.length; i += 2) {
            body.put(pairs[i], pairs[i + 1]);
        }
        return JSON.writeValueAsString(body);
    }

    private String dataId(String response) {
        try {
            return JSON.readTree(response).get("data").get("id").asText();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String verification(String state, Instant at) {
        String id = "ver_tk14_" + suffix + "_" + (++seq);
        jdbc.update("INSERT INTO ticket_verifications (id, verification_no, entitlement_id, entitlement_code, credential_type,"
                + " credential_payload, device_id, point_id, status, result_reason, source_type, created_at, merchant_id, verified_people,"
                + " settlement_price_cent) VALUES (?, ?, ?, ?, 'qr', 'payload', ?, 'point_tk14', ?, ?, 'android_terminal_online', ?, ?, 1, ?)",
                id, "V" + id, entitlementId, "TK14-" + suffix, deviceId, state,
                "success".equals(state) ? null : "entitlement_used_up", Timestamp.from(at), merchant,
                "success".equals(state) ? 1000L : null);
        return id;
    }

    private String statusOf(String verificationId) {
        return jdbc.queryForObject("SELECT status FROM ticket_verifications WHERE id = ?", String.class, verificationId);
    }

    private int usedTimes() {
        return jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, entitlementId);
    }

    private int refunds() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ticket_refunds WHERE target_id = ?", Integer.class, orderId);
    }

    private int anomalyRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ticket_verifications WHERE entitlement_id = ? AND status = 'anomaly'",
                Integer.class, entitlementId);
    }

    private RequestPostProcessor staff(String userId, String accountType, String merchantId, String... permissions) {
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '异常复核回归', 'project', 'active', now())",
                role, role);
        for (String permission : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, permission);
        }
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, ?, 'org_tk14', ?, 'active', now())", userId, userId, accountType, merchantId);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, accountType, "org_tk14", merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
