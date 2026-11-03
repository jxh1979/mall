// 文件版本：V0.02
package com.complex.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.access.TemporaryGrant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 23 条 23-R4 可编辑版。PGM:687「确需可编辑版本用于二次加工的，须另行申请、说明用途与接收方，经最高权限审批后发放，可编辑版同样带密码与暗纹
 * 并逐次留痕」；一次批准只放行一次（SupervisorReviewRepository.markSupervisorReviewReleased，M007-A-01）。走真实生产登录链（网页登录拿浏览器
 * 设备编号 Cookie，第十八批裁决 6；最高权限走 USB 签名登录）、真实授权链与真实审批接口，请求体按前端蛇形键。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_editable_admin",
        "complex.auth.production.seed-admin.password=p0_editable_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class EditableExportJdbcIntegrationTests {

    private static final KeyPair MASTER_KEY_PAIR = generateMasterKeyPair();

    static {
        System.setProperty("java.awt.headless", "true");
    }

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) {
        registry.add("complex.auth.master.public-key",
                () -> Base64.getEncoder().encodeToString(MASTER_KEY_PAIR.getPublic().getEncoded()));
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AccessRepository access;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "b28ed_prj_" + suffix;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','可编辑版回归项目','active',now(),now())", project);
        // 能看全平台合计（M737）：经勾了「可以查看全部项目」的角色持有查看经营分析看板的权限；导出权限只来自单次授权。
        role("dash", true, "analytics.dashboard.view");
        role("approve", false, "governance.supervisor_review.approve", "governance.supervisor_review.view");
        account("exporter", "dash");
        account("other", "dash");
        account("approver", "approve");
    }

    @Test
    void editableCopyNeedsTopAuthorityApprovalAndIsIssuedOncePerApproval() throws Exception {
        Session exporter = webLogin("exporter");
        String source = exportMonthlyReport(exporter);

        JsonNode review = data(requestEditable(exporter, source, "供审计事务所核对月度数据", "某某会计师事务所").andExpect(status().isOk()));
        String reviewId = review.at("/review_id").asText();
        assertThat(review.at("/review_status").asText()).isEqualTo("pending_review");
        assertThat(review.at("/approval_level").asText()).isEqualTo("top_authority");
        assertThat(review.at("/action_type").asText()).isEqualTo("security.export.editable_copy");
        assertThat(review.at("/object_type").asText()).isEqualTo("export_file");
        assertThat(review.at("/object_id").asText()).isEqualTo(source);
        assertThat(review.at("/reason").asText()).contains("用途：供审计事务所核对月度数据", "接收方：某某会计师事务所", "月度经营对比");
        assertThat(jdbc.queryForObject("SELECT initiated_by FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId))
                .isEqualTo(user("exporter"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'security.export.editable_requested' AND business_id = ?",
                Integer.class, source)).isEqualTo(1);
        // 「我的导出」列申请状态
        JsonNode pending = myExport(exporter, source);
        assertThat(pending.at("/editable_request/review_id").asText()).isEqualTo(reviewId);
        assertThat(pending.at("/editable_request/review_status").asText()).isEqualTo("pending_review");
        assertThat(pending.at("/editable_request/issued").asBoolean()).isFalse();

        // 批准前不发放
        editableCopy(exporter, source, reviewId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("EDITABLE_EXPORT_NOT_APPROVED"));
        // 非最高权限账号批不了
        approve(webLogin("approver"), reviewId).andExpect(status().isForbidden());
        assertThat(reviewStatus(reviewId)).isEqualTo("pending_review");
        // 第十八批 N 段第 124 条 124-R5（PGM L738「访问被拒绝也要留痕，便于排查是权限问题还是开关问题。」；OpenAPI approveSupervisorReview 的
        // x-audit-event-failure）：没有主管复核审批权限的人按前端蛇形键批准 403，按本次幂等键落 governance.supervisor_review.decide_failed
        String deniedKey = "b28ed-ap-denied-" + suffix;
        approve(webLogin("other"), reviewId, deniedKey).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertThat(jdbc.queryForList("""
                SELECT business_type || '|' || business_id || '|' || permission_code || '|' || (metadata->>'error_code')
                  FROM audit_events WHERE event_name = 'governance.supervisor_review.decide_failed' AND event_result = 'failure'
                   AND idempotency_key = ?""", String.class, deniedKey))
                .containsExactly("supervisor_review|" + reviewId + "|governance.supervisor_review.approve|PERMISSION_DENIED");
        assertThat(reviewStatus(reviewId)).isEqualTo("pending_review");
        // 最高权限经真实审批接口批准
        approve(masterLogin(), reviewId).andExpect(status().isOk());
        assertThat(reviewStatus(reviewId)).isEqualTo("approved");
        int before = exportCount();

        MockHttpServletResponse copy = editableCopy(exporter, source, reviewId).andExpect(status().isOk()).andReturn().getResponse();

        assertThat(copy.getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(exportCount()).isEqualTo(before + 1);
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT id, export_type, editable, source_export_id, review_id, device_no, mark_code, filters->>'month' AS month
                  FROM export_files WHERE exporter_id = ? AND editable""", user("exporter"));
        assertThat(row.get("export_type")).isEqualTo("analytics_monthly_report");
        assertThat(row.get("source_export_id")).isEqualTo(source);
        assertThat(row.get("review_id")).isEqualTo(reviewId);
        assertThat(row.get("device_no")).isEqualTo(exporter.deviceNo());
        assertThat(row.get("month")).isEqualTo("2026-07");
        byte[] pdf = copy.getContentAsByteArray();
        assertThatThrownBy(() -> Loader.loadPDF(pdf).close()).isInstanceOf(InvalidPasswordException.class);
        JsonNode issued = myExport(exporter, (String) row.get("id"));
        assertThat(issued.at("/editable").asBoolean()).isTrue();
        try (PDDocument doc = Loader.loadPDF(pdf, issued.at("/password").asText())) {
            assertThat(doc.isEncrypted()).isTrue();
            assertThat(doc.getCurrentAccessPermission().canExtractContent()).isTrue();
            assertThat(doc.getCurrentAccessPermission().canModify()).isTrue();
            // 复核（PGM:687 可编辑版「用于二次加工」）：复制出来的是原字（月、比不被记成同形的康熙部首 ⽉、⽐），不做 NFKC 归一。
            assertThat(new PDFTextStripper().getText(doc)).contains("月度经营对比", "指标", "同比", "本月", "上月");
            assertThat(WatermarkQr.decode(new PDFRenderer(doc).renderImageWithDPI(0, 72))).contains((String) row.get("mark_code"));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'security.export.editable_downloaded' AND business_id = ?",
                Integer.class, row.get("id"))).isEqualTo(1);
        assertThat(myExport(exporter, source).at("/editable_request/issued").asBoolean()).isTrue();

        // 一次批准发放一次：再下载 409、不增行；再要须另行申请
        editableCopy(exporter, source, reviewId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("EDITABLE_EXPORT_ALREADY_ISSUED"));
        assertThat(exportCount()).isEqualTo(before + 1);
        // 124-R5（PGM L685「导出须单独授权、逐次留痕」；OpenAPI downloadEditableExportCopy 的 x-audit-event-failure）：批准前与同一次批准再下载被拒
        // 都落 security.export.editable_download_failed（business_id 原导出文件编号，错误码同响应）
        assertThat(jdbc.queryForList("""
                SELECT permission_code || '|' || (metadata->>'error_code') FROM audit_events
                 WHERE event_name = 'security.export.editable_download_failed' AND event_result = 'failure' AND business_type = 'export_file'
                   AND business_id = ?""", String.class, source))
                .containsExactlyInAnyOrder("security.export.editable_copy|EDITABLE_EXPORT_NOT_APPROVED",
                        "security.export.editable_copy|EDITABLE_EXPORT_ALREADY_ISSUED");
        // 可编辑版本身不再申请可编辑版
        requestEditable(exporter, (String) row.get("id"), "再加工", "某某").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("EXPORT_ALREADY_EDITABLE"));
    }

    @Test
    void onlyTheExporterRequestsAndPurposeAndRecipientAreRequired() throws Exception {
        Session exporter = webLogin("exporter");
        String source = exportMonthlyReport(exporter);

        requestEditable(webLogin("other"), source, "核对", "某某").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("EXPORT_NOT_OWNED"));
        requestEditable(exporter, source, "核对", " ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        requestEditable(exporter, source, null, "某某").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE object_id = ?", Integer.class, source)).isZero();
        // 别人的批准单也下不到
        editableCopy(webLogin("other"), source, "review_x").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("EXPORT_NOT_OWNED"));
    }

    /** 其余三种导出的可编辑版同样按原筛选、以申请人的查看权限重取（审批即授权，不再要导出权限）。 */
    @Test
    void ledgerAuditAndInvoiceEditableCopiesAreReFetchedWithViewPermissions() throws Exception {
        role("views", false, "audit.operation_log.view", "settlement.invoice.view", "finance.ar_ap.view");
        account("multi", "views");
        // 操作留痕导出与查看对非平台账号须在商户范围内（currentMerchantScope），同 ExportFileJdbcIntegrationTests 用商户账号
        jdbc.update("UPDATE access_users SET merchant_id = ? WHERE id = ?", "b28ed_mch_" + suffix, user("multi"));
        Session multi = webLogin("multi");
        Session master = masterLogin();
        Map<String, List<String>> cases = Map.of(
                "audit_operation_events", List.of("audit.operation_log.export",
                        "/api/v1/audit/operation-events/export?business_type=watermark_mark&limit=5", "操作留痕"),
                "finance_ar_ap_ledger", List.of("finance.ar_ap.export", "/api/v1/finance/ar-ap-items/export", "应收应付台账"),
                "settlement_invoice_items", List.of("settlement.invoice.export",
                        "/api/v1/settlement/invoice-items/export?projectId=" + project + "&periodStart=2026-05-01", "开票清单"));
        for (Map.Entry<String, List<String>> each : cases.entrySet()) {
            singleUseGrant("multi", each.getValue().get(0));
            MockHttpServletResponse exported = mockMvc.perform(get(each.getValue().get(1)).cookie(multi.cookies())
                    .header("X-Terminal-Type", "web")).andReturn().getResponse();
            assertThat(exported.getStatus()).as(each.getKey() + "：" + exported.getContentAsString()).isEqualTo(200);
            String source = jdbc.queryForObject("SELECT id FROM export_files WHERE exporter_id = ? AND export_type = ? AND NOT editable",
                    String.class, user("multi"), each.getKey());
            String reviewId = data(requestEditable(multi, source, "二次加工", "某某").andExpect(status().isOk())).at("/review_id").asText();
            approve(master, reviewId).andExpect(status().isOk());

            byte[] pdf = editableCopy(multi, source, reviewId).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

            Map<String, Object> copy = jdbc.queryForMap("SELECT id, export_type, editable FROM export_files WHERE source_export_id = ?", source);
            assertThat(copy.get("export_type")).isEqualTo(each.getKey());
            assertThat(copy.get("editable")).isEqualTo(true);
            try (PDDocument doc = Loader.loadPDF(pdf, myExport(multi, (String) copy.get("id")).at("/password").asText())) {
                assertThat(doc.getCurrentAccessPermission().canExtractContent()).isTrue();
                assertThat(new PDFTextStripper().getText(doc)).contains(each.getValue().get(2));
            }
        }
    }

    // ---- 请求 ----

    private record Session(Cookie access, Cookie csrf, Cookie device) {
        String deviceNo() {
            return device.getValue();
        }

        Cookie[] cookies() {
            return new Cookie[] {access, csrf, device};
        }
    }

    private String exportMonthlyReport(Session session) throws Exception {
        singleUseGrant("exporter", "analytics.report.export");
        mockMvc.perform(get("/api/v1/analytics/reports/monthly-export?month=2026-07").cookie(session.cookies())
                .header("X-Terminal-Type", "web")).andExpect(status().isOk());
        return jdbc.queryForObject("SELECT id FROM export_files WHERE exporter_id = ? AND NOT editable", String.class, user("exporter"));
    }

    private ResultActions requestEditable(Session session, String exportId, String purpose, String recipient) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("purpose", purpose);
        body.put("recipient", recipient);
        body.put("idempotency_key", "b28ed-" + UUID.randomUUID());
        return mockMvc.perform(post("/api/v1/security/export-files/" + exportId + "/editable-requests")
                .cookie(session.cookies())
                .header("X-XSRF-TOKEN", session.csrf().getValue())
                .header("X-Terminal-Type", "web")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions editableCopy(Session session, String exportId, String reviewId) throws Exception {
        return mockMvc.perform(get("/api/v1/security/export-files/" + exportId + "/editable-copy?review_id=" + reviewId)
                .cookie(session.cookies()).header("X-Terminal-Type", "web"));
    }

    private ResultActions approve(Session session, String reviewId) throws Exception {
        return approve(session, reviewId, "b28ed-ap-" + UUID.randomUUID());
    }

    private ResultActions approve(Session session, String reviewId, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve")
                .cookie(session.cookies())
                .header("X-XSRF-TOKEN", session.csrf().getValue())
                .header("X-Terminal-Type", "web")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("decision", "approved", "comment", "用途与接收方已核实",
                        "idempotency_key", idempotencyKey))));
    }

    /** 「我的导出」里该文件的一行。 */
    private JsonNode myExport(Session session, String exportFileId) throws Exception {
        JsonNode page = data(mockMvc.perform(get("/api/v1/security/my-exports?page=1&size=20").cookie(session.cookies())
                .header("X-Terminal-Type", "web")).andExpect(status().isOk()));
        for (JsonNode item : page.at("/items")) {
            if (exportFileId.equals(item.at("/id").asText())) {
                return item;
            }
        }
        throw new AssertionError("我的导出里没有 " + exportFileId + "：" + page);
    }

    private Session webLogin(String tag) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("login_name", user(tag), "password", password(tag)))))
                .andExpect(status().isOk())
                .andReturn();
        return session(login);
    }

    /** 最高权限：USB 签名破窗登录（同 BrowserDeviceLoginJdbcIntegrationTests）。 */
    private Session masterLogin() throws Exception {
        MvcResult challenge = mockMvc.perform(post("/api/v1/auth/master/challenge")).andExpect(status().isOk()).andReturn();
        String text = objectMapper.readTree(challenge.getResponse().getContentAsString()).at("/data/challenge").asText();
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(MASTER_KEY_PAIR.getPrivate());
        signer.update(text.getBytes(StandardCharsets.UTF_8));
        MvcResult login = mockMvc.perform(post("/api/v1/auth/master/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("challenge", text,
                                "signature", Base64.getEncoder().encodeToString(signer.sign())))))
                .andExpect(status().isOk())
                .andReturn();
        return session(login);
    }

    private static Session session(MvcResult login) {
        return new Session(login.getResponse().getCookie("admin_access_token"), login.getResponse().getCookie("XSRF-TOKEN"),
                login.getResponse().getCookie(BrowserDeviceRegistry.COOKIE));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).at("/data");
    }

    private String reviewStatus(String reviewId) {
        return jdbc.queryForObject("SELECT review_status FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId);
    }

    private int exportCount() {
        return jdbc.queryForObject("SELECT count(*) FROM export_files WHERE exporter_id = ?", Integer.class, user("exporter"));
    }

    // ---- 夹具 ----

    private void role(String tag, boolean viewAllProjects, String... permissions) {
        access.createRole(new RoleRecord("b28ed_" + tag + "_" + suffix, "b28ed_" + tag + "_" + suffix, "可编辑版回归", "project", "active",
                Instant.now(), List.of(permissions), project, viewAllProjects, List.of()));
    }

    private void account(String tag, String... roles) {
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,NULL,'active',now())",
                user(tag), user(tag), project);
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, user(tag), new BCryptPasswordEncoder(10).encode(password(tag)));
        for (String role : roles) {
            access.assignUserRole(user(tag), "b28ed_" + role + "_" + suffix);
        }
    }

    private void singleUseGrant(String tag, String permission) {
        Instant now = Instant.now();
        access.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), user(tag), List.of(permission), now.minusSeconds(3600),
                now.plusSeconds(86_400), true, false, "active", "第 23 条可编辑版回归单次授权", "seed-admin", now, null, null));
    }

    private String user(String tag) {
        return "b28ed_" + tag + "_" + suffix;
    }

    private static String password(String tag) {
        return "b28-editable-" + tag + "-password";
    }

    private static KeyPair generateMasterKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
