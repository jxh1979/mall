// 文件版本：V0.01
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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
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
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
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
 * 第 23 条 23-R3 导出文件。PGM:687「所有导出的文件一律带密码，密码不可取消、不可由导出人自行设定。密码由导出人账号、导出时刻、导出所用设备的
 * 唯一识别号……以及接入网络等可查证要素按统一算法生成，长度不超过十位，且可由系统反向推算出对应的导出人、导出时间与导出设备；导出人可以在专门的地方
 * 查到自己导出文件的密码。导出文件中的数据不可复制、不可编辑」；PGM:686「每一次导出……都记录导出人、导出时间、导出所用设备、导出内容范围与筛选条件」；
 * PGM:685「导出须单独授权、逐次留痕」。走真实生产登录链（网页登录拿浏览器设备编号 Cookie，第十八批裁决 6）、真实授权链与单次授权，按前端键名请求。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_export_admin",
        "complex.auth.production.seed-admin.password=p0_export_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class ExportFileJdbcIntegrationTests {

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
    private final String project = "b28ex_prj_" + suffix;
    private final String otherProject = "b28ex_prj2_" + suffix;
    private final String merchant = "b28ex_mch_" + suffix;

    @BeforeEach
    void seed() {
        for (String id : List.of(project, otherProject)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','导出回归项目','active',now(),now())", id);
        }
        // 能看全平台合计（M737）：经勾了「可以查看全部项目」的角色持有查看经营分析看板的权限；导出权限只来自单次授权。
        role("dash", true, "analytics.dashboard.view");
        role("trace", false, WatermarkMarkService.TRACE_PERMISSION);
        role("ledger", false, "audit.operation_log.export", "finance.ar_ap.export");
        account("exporter", project, null, "dash");
        account("viewer", project, null);
        account("tracer", project, null, "trace");
        account("outsider", otherProject, null, "trace");
        account("merchant", project, merchant, "ledger");
    }

    @Test
    void monthlyReportExportIsPasswordProtectedUncopyableWatermarkedPdfAndUsesUpTheGrant() throws Exception {
        singleUseGrant("exporter", "analytics.report.export");
        Session exporter = webLogin("exporter");

        MockHttpServletResponse response = export(exporter, "/api/v1/analytics/reports/monthly-export?month=2026-07");

        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment").contains("filename*=UTF-8''");
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT id, exporter_id, device_no, network, export_type, filters->>'month' AS month, filters->>'scope' AS scope,
                       row_count, mark_code, editable FROM export_files WHERE exporter_id = ?""", user("exporter"));
        assertThat(row.get("device_no")).isEqualTo(exporter.deviceNo());
        assertThat(row.get("network")).isEqualTo("127.0.0.1");
        assertThat(row.get("export_type")).isEqualTo("analytics_monthly_report");
        assertThat(row.get("month")).isEqualTo("2026-07");
        assertThat(row.get("scope")).isEqualTo("全平台");
        assertThat((Integer) row.get("row_count")).isPositive();
        assertThat(row.get("editable")).isEqualTo(false);
        assertThat(consumed("exporter", "analytics.report.export")).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'analytics.monthly_report.exported' AND business_id = ?",
                Integer.class, row.get("id"))).isEqualTo(1);

        byte[] pdf = response.getContentAsByteArray();
        assertThatThrownBy(() -> Loader.loadPDF(pdf).close()).isInstanceOf(InvalidPasswordException.class);
        String password = myExportPassword(exporter, (String) row.get("id"));
        assertThat(password).matches("[A-HJKMNP-Z2-9]{10}");
        try (PDDocument doc = Loader.loadPDF(pdf, password)) {
            assertThat(doc.isEncrypted()).isTrue();
            assertThat(doc.getCurrentAccessPermission().canExtractContent()).isFalse();
            assertThat(doc.getCurrentAccessPermission().canModify()).isFalse();
            assertThat(doc.getCurrentAccessPermission().canPrint()).isTrue();
            assertThat(text(doc)).contains("月度经营对比", "指标", "同比", "净收入");
            // 每页平铺的暗纹按截屏反查同一规则能解出本文件的暗纹码
            assertThat(WatermarkQr.decode(new PDFRenderer(doc).renderImageWithDPI(0, 72))).contains((String) row.get("mark_code"));
        }
        assertThat(jdbc.queryForObject("SELECT export_file_id FROM watermark_marks WHERE code = ?", String.class, row.get("mark_code")))
                .isEqualTo(row.get("id"));
        // 「专门的地方」只列本人导出
        JsonNode others = data(mockMvc.perform(get("/api/v1/security/my-exports?page=1&size=20").cookie(webLogin("viewer").cookies())
                .header("X-Terminal-Type", "web")).andExpect(status().isOk()));
        assertThat(others.at("/items").findValuesAsText("id")).doesNotContain((String) row.get("id"));
    }

    @Test
    void auditAndLedgerExportsAreProtectedPdfsRecordingFiltersAndScope() throws Exception {
        Session account = webLogin("merchant");

        MockHttpServletResponse audit = export(account, "/api/v1/audit/operation-events/export?business_type=watermark_mark&limit=5");
        assertThat(audit.getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        Map<String, Object> auditRow = jdbc.queryForMap("""
                SELECT id, filters->>'business_type' AS business_type, filters->>'limit' AS lim, filters->>'merchant_id' AS merchant_id
                  FROM export_files WHERE exporter_id = ? AND export_type = 'audit_operation_events'""", user("merchant"));
        assertThat(auditRow.get("business_type")).isEqualTo("watermark_mark");
        assertThat(auditRow.get("lim")).isEqualTo("5");
        assertThat(auditRow.get("merchant_id")).isEqualTo(merchant);
        try (PDDocument doc = Loader.loadPDF(audit.getContentAsByteArray(), myExportPassword(account, (String) auditRow.get("id")))) {
            assertThat(text(doc)).contains("操作留痕", "事件名称");
        }

        MockHttpServletResponse ledger = export(account, "/api/v1/finance/ar-ap-items/export");
        assertThat(ledger.getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        Map<String, Object> ledgerRow = jdbc.queryForMap("""
                SELECT id, filters->>'merchant_id' AS merchant_id FROM export_files
                 WHERE exporter_id = ? AND export_type = 'finance_ar_ap_ledger'""", user("merchant"));
        assertThat(ledgerRow.get("merchant_id")).isEqualTo(merchant);
        Map<String, Object> exported = jdbc.queryForMap("""
                SELECT metadata->>'format' AS format, metadata->>'export_file_id' AS export_file_id FROM audit_events
                 WHERE event_name = 'finance_ar_ap.exported' AND actor_id = ?""", user("merchant"));
        assertThat(exported.get("format")).isEqualTo("pdf");
        assertThat(exported.get("export_file_id")).isEqualTo(ledgerRow.get("id"));
        try (PDDocument doc = Loader.loadPDF(ledger.getContentAsByteArray(), myExportPassword(account, (String) ledgerRow.get("id")))) {
            assertThat(text(doc)).contains("应收应付台账", "单据编号", "未收付金额(分)");
        }
    }

    @Test
    void invoiceListExportNeedsItsOwnSeparatelyGrantedPermissionAndTakesTheListFilters() throws Exception {
        Session exporter = webLogin("exporter");
        String url = "/api/v1/settlement/invoice-items/export?projectId=" + project + "&periodStart=2026-05-01";

        mockMvc.perform(get(url).cookie(exporter.cookies()).header("X-Terminal-Type", "web"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));

        singleUseGrant("exporter", "settlement.invoice.export");
        MockHttpServletResponse response = export(exporter, url);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT id, filters->>'project_id' AS project_id, filters->>'period_start' AS period_start, row_count
                  FROM export_files WHERE exporter_id = ? AND export_type = 'settlement_invoice_items'""", user("exporter"));
        assertThat(row.get("project_id")).isEqualTo(project);
        assertThat(row.get("period_start")).isEqualTo("2026-05-01");
        assertThat(row.get("row_count")).isEqualTo(0);
        assertThat(consumed("exporter", "settlement.invoice.export")).isTrue();
        try (PDDocument doc = Loader.loadPDF(response.getContentAsByteArray(), myExportPassword(exporter, (String) row.get("id")))) {
            assertThat(text(doc)).contains("开票清单", "开票主体", "购方");
        }
    }

    @Test
    void exportPasswordTracesBackToExporterTimeAndDeviceOnlyWithPermissionAndReach() throws Exception {
        singleUseGrant("exporter", "analytics.report.export");
        Session exporter = webLogin("exporter");
        export(exporter, "/api/v1/analytics/reports/monthly-export?month=2026-07");
        Map<String, Object> row = jdbc.queryForMap("SELECT id, exported_at FROM export_files WHERE exporter_id = ?", user("exporter"));
        String password = myExportPassword(exporter, (String) row.get("id"));

        JsonNode traced = data(lookup(webLogin("tracer"), password.toLowerCase()).andExpect(status().isOk()));
        assertThat(traced.at("/export_file_id").asText()).isEqualTo(row.get("id"));
        assertThat(traced.at("/exporter_id").asText()).isEqualTo(user("exporter"));
        assertThat(traced.at("/username").asText()).isEqualTo(user("exporter"));
        assertThat(traced.at("/device_no").asText()).isEqualTo(exporter.deviceNo());
        assertThat(traced.at("/network").asText()).isEqualTo("127.0.0.1");
        assertThat(traced.at("/export_type").asText()).isEqualTo("analytics_monthly_report");
        assertThat(traced.at("/filters/month").asText()).isEqualTo("2026-07");
        assertThat(Instant.parse(traced.at("/exported_at").asText())).isEqualTo(((java.sql.Timestamp) row.get("exported_at")).toInstant());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'security.export_password.traced' AND business_id = ?",
                Integer.class, row.get("id"))).isEqualTo(1);

        lookup(webLogin("viewer"), password).andExpect(status().isForbidden());
        lookup(webLogin("outsider"), password).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("EXPORT_PASSWORD_NOT_FOUND"));
        lookup(webLogin("tracer"), "ABCDEFGHJK").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("EXPORT_PASSWORD_NOT_FOUND"));
        // 复核：查不到、范围外被拒的反查同样留痕（每次反查写审计），失败留痕里不记输入的密码。
        for (String tag : List.of("outsider", "tracer")) {
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM audit_events WHERE event_name = 'security.export_password.trace_failed' AND event_result = 'failure'
                       AND actor_id = ? AND metadata->>'error_code' = 'EXPORT_PASSWORD_NOT_FOUND'""", Integer.class, user(tag))).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'security.export_password.trace_failed' AND metadata::text LIKE ?",
                Integer.class, "%" + password + "%")).isZero();
    }

    @Test
    void exportWithoutRegisteredBrowserDeviceIsRefused() throws Exception {
        singleUseGrant("exporter", "analytics.report.export");
        Session exporter = webLogin("exporter");
        mockMvc.perform(get("/api/v1/analytics/reports/monthly-export?month=2026-07")
                        .cookie(exporter.access(), exporter.csrf()).header("X-Terminal-Type", "web"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DEVICE_NOT_REGISTERED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM export_files WHERE exporter_id = ?", Integer.class, user("exporter"))).isZero();
    }

    // PGM:686「导出记录本身永久保存、不在任何清理范围内」：导出记录改不了、删不掉。
    @Test
    void exportFilesCannotBeUpdated() {
        insertExportFile();
        assertThatThrownBy(() -> jdbc.update("UPDATE export_files SET row_count = 1 WHERE id = ?", "exp_" + suffix))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void exportFilesCannotBeDeleted() {
        insertExportFile();
        assertThatThrownBy(() -> jdbc.update("DELETE FROM export_files WHERE id = ?", "exp_" + suffix))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    private void insertExportFile() {
        jdbc.update("""
                INSERT INTO export_files (id, exporter_id, exported_at, device_no, export_type, filters, row_count, password_digest, mark_code)
                VALUES (?, 'usr-x', now(), 'WBTEST', 'finance_ar_ap_ledger', '{}'::jsonb, 0, ?, 'ABCDEFGH')""",
                "exp_" + suffix, "0".repeat(56) + suffix);
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

    private Session webLogin(String tag) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("login_name", user(tag), "password", password(tag)))))
                .andExpect(status().isOk())
                .andReturn();
        return new Session(login.getResponse().getCookie("admin_access_token"), login.getResponse().getCookie("XSRF-TOKEN"),
                login.getResponse().getCookie(BrowserDeviceRegistry.COOKIE));
    }

    private MockHttpServletResponse export(Session session, String url) throws Exception {
        return mockMvc.perform(get(url).cookie(session.cookies()).header("X-Terminal-Type", "web"))
                .andExpect(status().isOk()).andReturn().getResponse();
    }

    /** 「我的导出」里该文件的打开密码。 */
    private String myExportPassword(Session session, String exportFileId) throws Exception {
        JsonNode page = data(mockMvc.perform(get("/api/v1/security/my-exports?page=1&size=20").cookie(session.cookies())
                .header("X-Terminal-Type", "web")).andExpect(status().isOk()));
        for (JsonNode item : page.at("/items")) {
            if (exportFileId.equals(item.at("/id").asText())) {
                return item.at("/password").asText();
            }
        }
        throw new AssertionError("我的导出里没有 " + exportFileId + "：" + page);
    }

    private ResultActions lookup(Session session, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/security/export-files/lookup")
                .cookie(session.cookies())
                .header("X-XSRF-TOKEN", session.csrf().getValue())
                .header("X-Terminal-Type", "web")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", password, "idempotency_key", "b28ex-" + UUID.randomUUID()))));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).at("/data");
    }

    // ---- 夹具 ----

    private void role(String tag, boolean viewAllProjects, String... permissions) {
        access.createRole(new RoleRecord("b28ex_" + tag + "_" + suffix, "b28ex_" + tag + "_" + suffix, "导出回归", "project", "active",
                Instant.now(), List.of(permissions), project, viewAllProjects, List.of()));
    }

    private void account(String tag, String organization, String merchantId, String... roles) {
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                user(tag), user(tag), organization, merchantId);
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, user(tag), new BCryptPasswordEncoder(10).encode(password(tag)));
        for (String role : roles) {
            access.assignUserRole(user(tag), "b28ex_" + role + "_" + suffix);
        }
    }

    private void singleUseGrant(String tag, String permission) {
        Instant now = Instant.now();
        access.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), user(tag), List.of(permission), now.minusSeconds(3600), now.plusSeconds(86_400),
                true, false, "active", "第 23 条导出回归单次授权", "seed-admin", now, null, null));
    }

    private boolean consumed(String tag, String permission) {
        return jdbc.queryForObject("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ? AND permission_code = ?",
                Boolean.class, user(tag), permission);
    }

    /** PDF 文本：按原字比对（复核：复制出来的须是原字，不是同形的康熙部首，不做 NFKC 归一）。 */
    private static String text(PDDocument doc) throws Exception {
        return new PDFTextStripper().getText(doc);
    }

    private String user(String tag) {
        return "b28ex_" + tag + "_" + suffix;
    }

    private static String password(String tag) {
        return "b28-export-" + tag + "-password";
    }

    private static KeyPair generateMasterKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
