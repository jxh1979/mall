// 文件版本：V0.01
package com.complex.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.common.BitMatrix;
import jakarta.servlet.http.Cookie;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 23 条 23-R2 在线页面暗纹与截屏、拍屏反查。PGM:687「后台、商户专区和员工手机端的所有在线页面都带有按同一规则生成的暗纹；
 * 暗纹可反向推算：把页面的截屏图片、或对着屏幕拍摄的照片上传到系统，即可算出该页面是由哪个账号、在哪一台设备上、于什么时间打开的」。
 * 走真实生产登录链（网页登录拿浏览器设备编号 Cookie）与真实授权链，按前端蛇形键请求；截屏按前端 PageWatermark 同一组参数在服务端合成。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_watermark_admin",
        "complex.auth.production.seed-admin.password=p0_watermark_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class WatermarkTraceJdbcIntegrationTests {

    // 与前端 apps/web/src/admin/components/PageWatermark.tsx 同值（WatermarkQr 常量）：平铺间距、二维码模块边长、格内偏移、不透明度。
    private static final int TILE = WatermarkQr.TILE_PX;
    private static final int MODULE = WatermarkQr.MODULE_PX;
    private static final int OFFSET = WatermarkQr.OFFSET_PX;
    private static final double OPACITY = WatermarkQr.OPACITY;
    private static final String TRACE = "security.watermark.trace";
    private static final KeyPair MASTER_KEY_PAIR = generateMasterKeyPair();
    private static final Font PAGE_FONT;

    static {
        System.setProperty("java.awt.headless", "true");
        try (InputStream font = new ClassPathResource("fonts/NotoSansSC-Variable.ttf").getInputStream()) {
            PAGE_FONT = Font.createFont(Font.TRUETYPE_FONT, font).deriveFont(15f);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) {
        registry.add("complex.auth.master.public-key",
                () -> Base64.getEncoder().encodeToString(MASTER_KEY_PAIR.getPublic().getEncoded()));
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "b28wm_prj_" + suffix;
    private final String otherProject = "b28wm_prj2_" + suffix;

    @BeforeEach
    void seed() {
        for (String id : List.of(project, otherProject)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','暗纹回归项目','active',now(),now())", id);
        }
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'暗纹回归','project','active',now())",
                "b28wm_role_" + suffix, "b28wm_role_" + suffix);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code=?",
                "b28wm_role_" + suffix, TRACE);
        account("viewer", project, false);
        account("tracer", project, true);
        account("outsider", otherProject, true);
    }

    @Test
    void screenshotOfWatermarkedPageTracesBackToAccountDeviceAndTime() throws Exception {
        Session viewer = webLogin("viewer");
        JsonNode mark = pageView(viewer, "ADM-OV-001", false, List.of()).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString().transform(this::readTree).at("/data");
        String code = mark.at("/code").asText();
        assertThat(code).matches("[A-HJKMNP-Z2-9]{8}");
        assertThat(mark.at("/opened_at").asText()).isNotBlank();

        JsonNode traced = trace(webLogin("tracer"), png(page(code, false))).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString().transform(this::readTree).at("/data");

        assertThat(traced.at("/account_id").asText()).isEqualTo(user("viewer"));
        assertThat(traced.at("/username").asText()).isEqualTo(user("viewer"));
        assertThat(traced.at("/device_no").asText()).isEqualTo(viewer.deviceNo());
        assertThat(traced.at("/network").asText()).isEqualTo("127.0.0.1");
        assertThat(traced.at("/kind").asText()).isEqualTo("page");
        assertThat(traced.at("/page_id").asText()).isEqualTo("ADM-OV-001");
        assertThat(traced.at("/opened_at").asText()).isEqualTo(mark.at("/opened_at").asText());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='security.watermark.traced' AND business_id=?",
                Integer.class, code)).isEqualTo(1);
    }

    /** 拍屏模拟：旋转 3°、缩放 0.8、模糊、加噪各自与叠加都能解出；深色主题（深底浅色模块）同样能解出。 */
    @Test
    void photoLikeAndDarkThemeImagesStillTrace() throws Exception {
        Session viewer = webLogin("viewer");
        String code = codeOf(pageView(viewer, "ADM-FIN-001", false, List.of()));
        Session tracer = webLogin("tracer");
        BufferedImage light = page(code, false);
        BufferedImage dark = page(code, true);
        for (BufferedImage image : List.of(rotate(light, 3), scale(light, 0.8), blur(light), noise(light),
                noise(blur(scale(rotate(light, 3), 0.8))), dark, noise(blur(scale(rotate(dark, 3), 0.8))))) {
            trace(tracer, png(image)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.account_id").value(user("viewer")))
                    .andExpect(jsonPath("$.data.page_id").value("ADM-FIN-001"));
        }
        trace(tracer, jpeg(noise(blur(scale(rotate(light, 3), 0.8)))), "photo.jpg", "image/jpeg").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.device_no").value(viewer.deviceNo()));
    }

    @Test
    void imageWithoutWatermarkIsNotFoundAndNonImageIsRejected() throws Exception {
        Session tracer = webLogin("tracer");
        BufferedImage blank = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = blank.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 800, 600);
        g.dispose();
        trace(tracer, png(blank)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("WATERMARK_NOT_FOUND"));
        assertThat(traceFailures(user("tracer"))).isEqualTo(1);
        trace(tracer, "不是图片".getBytes(StandardCharsets.UTF_8), "note.txt", "text/plain").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void traceNeedsSeparatelyGrantedPermissionAndReachOfTargetAccount() throws Exception {
        Session viewer = webLogin("viewer");
        byte[] shot = png(page(codeOf(pageView(viewer, "ADM-OV-001", false, List.of())), false));

        trace(viewer, shot).andExpect(status().isForbidden());
        trace(webLogin("outsider"), shot).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("WATERMARK_NOT_FOUND"));
        // 复核：范围外被拒的反查同样留痕（每次反查写审计）。
        assertThat(traceFailures(user("outsider"))).isEqualTo(1);
    }

    private int traceFailures(String actor) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'security.watermark.trace_failed' AND event_result = 'failure'
                   AND actor_id = ? AND metadata->>'error_code' = 'WATERMARK_NOT_FOUND'""", Integer.class, actor);
    }

    @Test
    void pageViewWithoutRegisteredBrowserDeviceIsRefused() throws Exception {
        Session viewer = webLogin("viewer");
        Session withoutDevice = new Session(viewer.access(), viewer.csrf(), null);

        pageView(withoutDevice, "ADM-OV-001", false, List.of()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_NOT_REGISTERED"));
    }

    /** DEC-R3B-2026-08-04-14「兜底关闭不含留痕的写入」：web 终端总开关关掉时别的写被拒，打开页面的暗纹与留痕照常落行。 */
    @Test
    void pageViewIsStillRecordedWhenWebTerminalSwitchIsOff() throws Exception {
        Session viewer = webLogin("viewer");
        Session tracer = webLogin("tracer");
        jdbc.update("UPDATE terminal_capability_switches SET enabled = FALSE WHERE terminal_type = 'web'"
                + " AND capability_code = 'terminal.enabled' AND scope_type = 'global'");
        trace(tracer, png(page("ABCDEFGH", false))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));

        String code = codeOf(pageView(viewer, "ADM-OV-001", false, List.of()));

        assertThat(jdbc.queryForObject("SELECT account_id FROM watermark_marks WHERE code=?", String.class, code))
                .isEqualTo(user("viewer"));
    }

    @Test
    void sensitivePageViewRecordsObjectsAndAuditEvent() throws Exception {
        Session viewer = webLogin("viewer");
        String code = codeOf(pageView(viewer, "ADM-CUS-001", true, List.of(Map.of("object_type", "customer", "object_id", "cus-1"))));

        Map<String, Object> row = jdbc.queryForMap("SELECT kind, sensitive, object_refs::text AS refs, device_no FROM watermark_marks WHERE code=?", code);
        assertThat(row.get("kind")).isEqualTo("page");
        assertThat(row.get("sensitive")).isEqualTo(true);
        assertThat(readTree((String) row.get("refs"))).isEqualTo(readTree("[{\"object_type\":\"customer\",\"object_id\":\"cus-1\"}]"));
        assertThat(row.get("device_no")).isEqualTo(viewer.deviceNo());
        Map<String, Object> audit = jdbc.queryForMap("""
                SELECT actor_id, metadata->>'page_id' AS page_id, metadata->>'device_no' AS device_no, metadata->'object_refs'->0->>'object_id' AS object_id
                  FROM audit_events WHERE event_name='security.sensitive_page.viewed' AND business_id=?""", code);
        assertThat(audit.get("actor_id")).isEqualTo(user("viewer"));
        assertThat(audit.get("page_id")).isEqualTo("ADM-CUS-001");
        assertThat(audit.get("device_no")).isEqualTo(viewer.deviceNo());
        assertThat(audit.get("object_id")).isEqualTo("cus-1");
    }

    @Test
    void watermarkMarksCannotBeUpdated() {
        insertMark("UPDATEME");
        assertThatThrownBy(() -> jdbc.update("UPDATE watermark_marks SET page_id='ADM-X' WHERE code='UPDATEME'"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void watermarkMarksCannotBeDeleted() {
        insertMark("DELETEME");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM watermark_marks WHERE code='DELETEME'"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    // ---- 请求 ----

    private record Session(Cookie access, Cookie csrf, Cookie device) {
        String deviceNo() {
            return device.getValue();
        }

        Cookie[] cookies() {
            return device == null ? new Cookie[] {access, csrf} : new Cookie[] {access, csrf, device};
        }
    }

    private Session webLogin(String tag) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("login_name", user(tag), "password", password(tag)))))
                .andExpect(status().isOk())
                .andReturn();
        return new Session(login.getResponse().getCookie("admin_access_token"), login.getResponse().getCookie("XSRF-TOKEN"),
                login.getResponse().getCookie("complex_device_no"));
    }

    private ResultActions pageView(Session session, String pageId, boolean sensitive, List<Map<String, String>> refs) throws Exception {
        return mockMvc.perform(post("/api/v1/security/page-views")
                .cookie(session.cookies())
                .header("X-XSRF-TOKEN", session.csrf().getValue())
                .header("X-Terminal-Type", "web")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("page_id", pageId, "sensitive", sensitive, "object_refs", refs,
                        "idempotency_key", "b28wm-" + UUID.randomUUID()))));
    }

    private ResultActions trace(Session session, byte[] image) throws Exception {
        return trace(session, image, "shot.png", "image/png");
    }

    private ResultActions trace(Session session, byte[] bytes, String name, String type) throws Exception {
        return mockMvc.perform(multipart("/api/v1/security/watermarks/trace")
                .file(new MockMultipartFile("file", name, type, bytes))
                .cookie(session.cookies())
                .header("X-XSRF-TOKEN", session.csrf().getValue())
                .header("X-Terminal-Type", "web"));
    }

    private String codeOf(ResultActions pageView) throws Exception {
        return readTree(pageView.andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).at("/data/code").asText();
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    // ---- 截屏合成（前端 PageWatermark 同一组参数：平铺 200、模块 6、偏移 24、不透明度 0.08；浅色主题黑色模块、深色主题白色模块）----

    private static BufferedImage page(String code, boolean dark) {
        int width = 1280;
        int height = 800;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(dark ? new Color(20, 20, 20) : Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(dark ? new Color(230, 230, 230) : new Color(30, 30, 30));
        g.setFont(PAGE_FONT);
        Random random = new Random(7);
        for (int y = 40; y < height; y += 36) {
            StringBuilder line = new StringBuilder();
            for (int i = 20 + random.nextInt(50); i > 0; i--) {
                line.append((char) (0x4E00 + random.nextInt(0x9FA5 - 0x4E00)));
            }
            g.drawString(line.toString(), 20 + random.nextInt(80), y);
        }
        BitMatrix qr = WatermarkQr.encode(code);
        int alpha = (int) Math.round(OPACITY * 255);
        g.setColor(dark ? new Color(255, 255, 255, alpha) : new Color(0, 0, 0, alpha));
        g.setFont(PAGE_FONT.deriveFont(12f));
        for (int ty = 0; ty < height; ty += TILE) {
            for (int tx = 0; tx < width; tx += TILE) {
                for (int y = 0; y < qr.getHeight(); y++) {
                    for (int x = 0; x < qr.getWidth(); x++) {
                        if (qr.get(x, y)) {
                            g.fillRect(tx + OFFSET + x * MODULE, ty + OFFSET + y * MODULE, MODULE, MODULE);
                        }
                    }
                }
                g.drawString(code, tx + OFFSET, ty + OFFSET + qr.getHeight() * MODULE + 16);
            }
        }
        g.dispose();
        return image;
    }

    private static BufferedImage rotate(BufferedImage source, double degrees) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.rotate(Math.toRadians(degrees), source.getWidth() / 2.0, source.getHeight() / 2.0);
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return out;
    }

    private static BufferedImage scale(BufferedImage source, double factor) {
        int width = (int) (source.getWidth() * factor);
        int height = (int) (source.getHeight() * factor);
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    private static BufferedImage blur(BufferedImage source) {
        float[] kernel = {1 / 16f, 2 / 16f, 1 / 16f, 2 / 16f, 4 / 16f, 2 / 16f, 1 / 16f, 2 / 16f, 1 / 16f};
        return new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null).filter(source, null);
    }

    private static BufferedImage noise(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Random random = new Random(3);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getRGB(x, y);
                int n = (int) Math.round(random.nextGaussian() * 6);
                out.setRGB(x, y, (clamp(((pixel >> 16) & 255) + n) << 16) | (clamp(((pixel >> 8) & 255) + n) << 8) | clamp((pixel & 255) + n));
            }
        }
        return out;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static byte[] png(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] jpeg(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    // ---- 夹具 ----

    private void account(String tag, String organization, boolean tracer) {
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,NULL,'active',now())",
                user(tag), user(tag), organization);
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, user(tag), new BCryptPasswordEncoder(10).encode(password(tag)));
        if (tracer) {
            jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user(tag), "b28wm_role_" + suffix);
        }
    }

    private void insertMark(String code) {
        jdbc.update("INSERT INTO watermark_marks(code,kind,account_id,device_no,network,page_id) VALUES(?,'page','usr-x','WBTEST','127.0.0.1','ADM-OV-001')", code);
    }

    private String user(String tag) {
        return "b28wm_" + tag + "_" + suffix;
    }

    private static String password(String tag) {
        return "b28-watermark-" + tag + "-password";
    }

    private static KeyPair generateMasterKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
