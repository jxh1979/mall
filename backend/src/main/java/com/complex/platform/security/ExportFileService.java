// 文件版本：V0.02
package com.complex.platform.security;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.Strings;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.crypto.SigningSecretGuard;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.common.BitMatrix;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第 23 条 23-R3 导出文件。PGM:687「所有导出的文件一律带密码，密码不可取消、不可由导出人自行设定。密码由导出人账号、导出时刻、导出所用设备的
 * 唯一识别号……以及接入网络等可查证要素按统一算法生成，长度不超过十位，且可由系统反向推算出对应的导出人、导出时间与导出设备；导出人可以在专门的地方
 * 查到自己导出文件的密码。导出文件中的数据不可复制、不可编辑」「可编辑版同样带密码与暗纹」；PGM:686「每一次导出……都记录导出人、导出时间、导出所用
 * 设备、导出内容范围与筛选条件，导出记录本身永久保存」。
 *
 * <p>导出接口只交出服务端生成的加密 PDF：A4 横版表格（中文表头、自动分页，嵌入随包的 Noto Sans SC 子集），每页按页面暗纹同一规则平铺暗纹二维码
 * （{@link WatermarkQr}，码由 {@link WatermarkMarkService} 按 kind=export 发）；AES-256 加密，用户密码＝本文件的打开密码，所有者密码随机且不落地，
 * 非可编辑版关掉复制、无障碍提取、修改、注释、填表与重组（打印不限：HTML 对文件只限复制与编辑）。密码＝HMAC-SHA256(密钥, 导出人|导出时刻（秒）|
 * 设备编号|接入网络|文件编号) 取 10 位（31 字符表，不用 0、O、1、I、L）；密码与文件都不落库，export_files 只存密码摘要，反查按摘要找回。
 */
@Service
public class ExportFileService {

    static final String DEV_SECRET = "complex-dev-export-password-secret-change-me";
    // 去掉易混的 0、O、1、I、L，共 31 个字符（同暗纹码）。
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final String FONT = "fonts/NotoSansSC-Variable.ttf";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final PDRectangle PAGE = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final float MARGIN = 36;
    private static final float TITLE_SIZE = 14;
    private static final float SIZE = 8;
    private static final float LEAD = 11;
    private static final float PAD = 3;

    /** 生成的导出文件：编号、下载文件名与加密后的 PDF。 */
    public record ExportFile(String id, String fileName, byte[] pdf) {
        /** 下载响应：application/pdf，附件名带 UTF-8 中文。 */
        public ResponseEntity<byte[]> response() {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                    .body(pdf);
        }
    }

    /** 一份导出的内容：标题、导出记录所记的筛选与取数范围、表头与各行。 */
    public record Table(String title, Map<String, Object> filters, List<String> columns, List<List<String>> rows) {
    }

    /**
     * 第 23 条 23-R4 可编辑版重取数（{@link EditableExportService}）：按原导出的筛选条件、以申请人当前的查看范围重取（审批即授权，不再要导出权限）。
     * 每种导出内容（export_files.export_type）一个实现。
     */
    public interface Source {
        String exportType();

        Table viewTable(Map<String, Object> filters);
    }

    /** 「我的导出」一行：导出要素与按要素重算的打开密码；editableRequest 为本人对这份导出最近一次可编辑版申请（23-R4，没有为 null）。 */
    public record MyExport(String id, String exportType, Map<String, Object> filters, int rowCount, Instant exportedAt,
                           String deviceNo, String network, boolean editable, String password, EditableRequest editableRequest) {
    }

    /** 可编辑版申请（主管复核单）：复核单号、审批状态、这次批准是否已发放。 */
    public record EditableRequest(String reviewId, String reviewStatus, boolean issued) {
    }

    /** 导出密码反查：导出人、导出时间、导出设备与接入网络，以及导出了什么。 */
    public record PasswordTrace(String exportFileId, String exporterId, String username, Instant exportedAt, String deviceNo,
                                String network, String exportType, Map<String, Object> filters, int rowCount) {
    }

    private final JdbcTemplate jdbc;
    private final BrowserDeviceRegistry devices;
    private final WatermarkMarkService marks;
    private final AuthorizationService authorization;
    private final AccessRepository accounts;
    private final AuditEventService audit;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;
    private final String secret;

    public ExportFileService(JdbcTemplate jdbc, BrowserDeviceRegistry devices, WatermarkMarkService marks,
                             AuthorizationService authorization, AccessRepository accounts, AuditEventService audit,
                             ObjectMapper objectMapper, HttpServletRequest request,
                             @Value("${security.export.password-secret:" + DEV_SECRET + "}") String secret, Environment environment) {
        this.jdbc = jdbc;
        this.devices = devices;
        this.marks = marks;
        this.authorization = authorization;
        this.accounts = accounts;
        this.audit = audit;
        this.objectMapper = objectMapper;
        this.request = request;
        this.secret = SigningSecretGuard.requireUsableHmacSecret("security.export.password-secret", "COMPLEX_EXPORT_PASSWORD_SECRET",
                secret, DEV_SECRET, environment.acceptsProfiles(Profiles.of("prod")));
    }

    /** 普通（不可编辑）导出。 */
    public ExportFile create(String exportType, String title, Map<String, Object> filters, List<String> columns, List<List<String>> rows) {
        return create(exportType, title, filters, columns, rows, false, null, null);
    }

    /**
     * 生成导出文件并落导出记录（须在请求内调用）：导出人＝当前登录人，设备＝本请求的设备编号（为空 409 DEVICE_NOT_REGISTERED），网络＝来源地址；
     * filters 记请求筛选参数与本次取数所按的范围。editable、sourceExportId、reviewId 供可编辑版（23-R4）。
     */
    public ExportFile create(String exportType, String title, Map<String, Object> filters, List<String> columns, List<List<String>> rows,
                             boolean editable, String sourceExportId, String reviewId) {
        AccessActor actor = authorization.currentActor();
        String deviceNo = devices.current(request)
                .orElseThrow(() -> ApiException.conflict("DEVICE_NOT_REGISTERED", "请重新登录后再导出"));
        String network = BrowserDeviceRegistry.network(request);
        Instant exportedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String id = "exp_" + UUID.randomUUID().toString().replace("-", "");
        String code = marks.issue("export", null, id, false, List.of()).code();
        String password = password(actor.id(), exportedAt, deviceNo, network, id);
        byte[] pdf = pdf(title, columns, rows, code, password, editable);
        jdbc.update("""
                INSERT INTO export_files (id, exporter_id, exported_at, device_no, network, export_type, filters, row_count,
                                          password_digest, mark_code, editable, source_export_id, review_id)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?)""",
                id, actor.id(), Timestamp.from(exportedAt), deviceNo, network, exportType, json(filters), rows.size(),
                digest(password), code, editable, sourceExportId, reviewId);
        return new ExportFile(id, title + ".pdf", pdf);
    }

    /** 「我的导出」（PGM:687「导出人可以在专门的地方查到自己导出文件的密码」）：只列本人导出，密码按行内要素重算。 */
    public PageResponse<MyExport> mine(int page, int size) {
        String me = authorization.currentActor().id();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Long total = jdbc.queryForObject("SELECT count(*) FROM export_files WHERE exporter_id = ?", Long.class, me);
        // 23-R4：每份导出带上本人最近一次可编辑版申请（复核单），供「我的导出」显示审批状态与下载。
        List<MyExport> items = jdbc.query("""
                SELECT f.id, f.export_type, f.filters::text AS filters, f.row_count, f.exported_at, f.device_no, f.network, f.editable,
                       r.review_id, r.review_status, r.released_at
                  FROM export_files f
                  LEFT JOIN LATERAL (SELECT review_id, review_status, released_at FROM supervisor_review_decisions
                                      WHERE action_type = ? AND object_type = ? AND object_id = f.id AND initiated_by = f.exporter_id
                                      ORDER BY evaluated_at DESC LIMIT 1) r ON TRUE
                 WHERE f.exporter_id = ? ORDER BY f.exported_at DESC, f.id DESC LIMIT ? OFFSET ?""",
                (rs, row) -> {
                    Instant at = rs.getTimestamp("exported_at").toInstant();
                    String reviewId = rs.getString("review_id");
                    return new MyExport(rs.getString("id"), rs.getString("export_type"), map(rs.getString("filters")),
                            rs.getInt("row_count"), at, rs.getString("device_no"), rs.getString("network"), rs.getBoolean("editable"),
                            password(me, at, rs.getString("device_no"), rs.getString("network"), rs.getString("id")),
                            reviewId == null ? null
                                    : new EditableRequest(reviewId, rs.getString("review_status"), rs.getTimestamp("released_at") != null));
                }, EditableExportService.ACTION, EditableExportService.OBJECT_TYPE, me, safeSize, (safePage - 1) * safeSize);
        return new PageResponse<>(items, safePage, safeSize, total == null ? 0 : total);
    }

    /**
     * 导出密码反查（调用方先核 {@link WatermarkMarkService#TRACE_PERMISSION}，单独授予）：按密码摘要找回导出记录；查不到、或导出人不在反查人可触及
     * 范围内，一律 404 EXPORT_PASSWORD_NOT_FOUND。每次反查都写审计：查到 security.export_password.traced；查不到或范围外
     * security.export_password.trace_failed（独立事务，不记输入的密码）。
     */
    public PasswordTrace lookup(String password) {
        AccessActor actor = authorization.currentActor();
        String typed = Strings.requireText(password, "password").trim().toUpperCase(Locale.ROOT);
        PasswordTrace found = jdbc.query("""
                SELECT f.id, f.exporter_id, u.username, f.exported_at, f.device_no, f.network, f.export_type, f.filters::text AS filters,
                       f.row_count
                  FROM export_files f LEFT JOIN access_users u ON u.id = f.exporter_id
                 WHERE f.password_digest = ?""", (rs, row) -> trace(rs), digest(typed))
                .stream().findFirst()
                .filter(trace -> authorization.isMasterAuthority(actor) || accounts.findUserById(trace.exporterId())
                        .map(target -> authorization.canReachUser(actor, target, WatermarkMarkService.TRACE_PERMISSION)).orElse(false))
                .orElseThrow(() -> {
                    audit.recordFailure("security.export_password.trace_failed", WatermarkMarkService.TRACE_PERMISSION, "export_file",
                            null, null, null, "EXPORT_PASSWORD_NOT_FOUND");
                    return ApiException.notFound("EXPORT_PASSWORD_NOT_FOUND", "没有这个密码对应的导出文件，或不在您的查看范围内");
                });
        audit.recordSuccess("security.export_password.traced", WatermarkMarkService.TRACE_PERMISSION, "export_file", found.exportFileId(),
                null, null, Map.of("exporter_id", found.exporterId(), "device_no", found.deviceNo(), "export_type", found.exportType()));
        return found;
    }

    /** 本人的一份导出（23-R4 申请与发放可编辑版用）。 */
    record Owned(String id, String exportType, Map<String, Object> filters, boolean editable) {
    }

    /** 当前账号本人导出的这份文件；不存在或不是本人导出的一律 403 EXPORT_NOT_OWNED。 */
    Owned owned(String exportFileId) {
        String me = authorization.currentActor().id();
        return jdbc.query("SELECT id, export_type, filters::text AS filters, editable FROM export_files WHERE id = ? AND exporter_id = ?",
                        (rs, row) -> new Owned(rs.getString("id"), rs.getString("export_type"), map(rs.getString("filters")),
                                rs.getBoolean("editable")), exportFileId, me)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.forbidden("EXPORT_NOT_OWNED", "只能为本人导出的文件申请或下载可编辑版"));
    }

    /** 按行内要素重算打开密码（统一算法，导出时与「我的导出」同用）。 */
    String password(String exporterId, Instant exportedAt, String deviceNo, String network, String exportFileId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(String.join("|", exporterId, exportedAt.toString(), deviceNo, network == null ? "" : network,
                    exportFileId).getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(hash).getLong() >>> 1;
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                out.append(ALPHABET.charAt((int) (value % ALPHABET.length())));
                value /= ALPHABET.length();
            }
            return out.toString();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    static String digest(String password) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** 加密 PDF：A4 横版表格、每页平铺暗纹；非可编辑版不可复制、不可编辑。 */
    static byte[] pdf(String title, List<String> columns, List<List<String>> rows, String code, String password, boolean editable) {
        try (PDDocument doc = new PDDocument(); InputStream fontFile = new ClassPathResource(FONT).getInputStream()) {
            PDType0Font font = PDType0Font.load(doc, fontFile, true);
            // 实际写进去的字：字符编码（Identity-H 下即原字形号）→ 码位，用来重写文字层（见 toUnicode）。
            Map<Integer, Integer> drawn = new TreeMap<>();
            List<String> header = new ArrayList<>();
            for (String column : columns) {
                header.add(safe(font, column, drawn));
            }
            List<List<String>> body = new ArrayList<>();
            for (List<String> row : rows) {
                List<String> cells = new ArrayList<>();
                for (int i = 0; i < columns.size(); i++) {
                    cells.add(safe(font, i < row.size() && row.get(i) != null ? row.get(i) : "", drawn));
                }
                body.add(cells);
            }
            new Sheet(doc, font, safe(font, code, drawn), safe(font, title, drawn), header, widths(font, header, body)).write(body);
            AccessPermission permission = new AccessPermission();
            if (!editable) {
                permission.setCanExtractContent(false);
                permission.setCanExtractForAccessibility(false);
                permission.setCanModify(false);
                permission.setCanModifyAnnotations(false);
                permission.setCanFillInForm(false);
                permission.setCanAssembleDocument(false);
            }
            byte[] owner = new byte[24];
            RANDOM.nextBytes(owner);
            StandardProtectionPolicy policy = new StandardProtectionPolicy(HexFormat.of().formatHex(owner), password, permission);
            policy.setEncryptionKeyLength(256);
            // 先不加密存一次（PDFBox 此时子集化字体并按字体 cmap 生成 ToUnicode），重新打开后把 ToUnicode 换成实际写进去的字，再加密保存。
            ByteArrayOutputStream plain = new ByteArrayOutputStream();
            doc.save(plain);
            try (PDDocument done = Loader.loadPDF(plain.toByteArray())) {
                PDStream toUnicode = toUnicode(done, drawn);
                for (PDPage page : done.getPages()) {
                    PDResources resources = page.getResources();
                    for (COSName name : resources.getFontNames()) {
                        resources.getFont(name).getCOSObject().setItem(COSName.TO_UNICODE, toUnicode);
                    }
                }
                done.protect(policy);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                done.save(out);
                return out.toByteArray();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** 字体里没有的字换成「?」，换行保留，其他控制字符换空格（PDF 文本不能直接画它们）；写进去的字记进 drawn。 */
    private static String safe(PDType0Font font, String text, Map<Integer, Integer> drawn) throws IOException {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '\n') {
                out.append('\n');
                continue;
            }
            String ch = Character.isISOControl(cp) ? " " : new String(Character.toChars(cp));
            try {
                draw(font.encode(ch), ch, drawn);
                out.append(ch);
            } catch (IllegalArgumentException missing) {
                draw(font.encode("?"), "?", drawn);
                out.append('?');
            }
        }
        return out.toString();
    }

    private static void draw(byte[] code, String ch, Map<Integer, Integer> drawn) {
        drawn.putIfAbsent(((code[0] & 0xFF) << 8) | (code[1] & 0xFF), ch.codePointAt(0));
    }

    /**
     * 复核（PGM:687 可编辑版「用于二次加工」）：文字层（ToUnicode）按实际写进去的字重写。PDFBox 按字体 cmap 反查字形、同一字形取最小码位，
     * 字体里与康熙部首（U+2F00 起）、部首补充（U+2E80 起）共用字形的常用字（一、日、月、方、比、金、门、长……）会被记成部首，复制出来就不是原字。
     */
    private static PDStream toUnicode(PDDocument doc, Map<Integer, Integer> drawn) throws IOException {
        StringBuilder cmap = new StringBuilder("/CIDInit /ProcSet findresource begin\n12 dict begin\nbegincmap\n"
                + "/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def\n/CMapName /Adobe-Identity-UCS def\n"
                + "/CMapType 2 def\n1 begincodespacerange\n<0000> <FFFF>\nendcodespacerange\n");
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(drawn.entrySet());
        for (int from = 0; from < entries.size(); from += 100) {
            List<Map.Entry<Integer, Integer>> block = entries.subList(from, Math.min(from + 100, entries.size()));
            cmap.append(block.size()).append(" beginbfchar\n");
            for (Map.Entry<Integer, Integer> entry : block) {
                cmap.append('<').append(String.format("%04X", entry.getKey())).append("> <")
                        .append(HexFormat.of().withUpperCase().formatHex(Character.toString(entry.getValue()).getBytes(StandardCharsets.UTF_16BE)))
                        .append(">\n");
            }
            cmap.append("endbfchar\n");
        }
        cmap.append("endcmap\nCMapName currentdict /CMap defineresource pop\nend\nend\n");
        return new PDStream(doc, new ByteArrayInputStream(cmap.toString().getBytes(StandardCharsets.US_ASCII)), COSName.FLATE_DECODE);
    }

    private static float width(PDType0Font font, String text) throws IOException {
        float max = 0;
        for (String line : text.split("\n", -1)) {
            max = Math.max(max, font.getStringWidth(line) / 1000 * SIZE);
        }
        return max;
    }

    /** 列宽：先按各列最宽内容，放得下的列照原宽，放不下的平分剩下的宽度（超出部分在格内折行）。 */
    private static float[] widths(PDType0Font font, List<String> header, List<List<String>> body) throws IOException {
        int n = header.size();
        float[] natural = new float[n];
        for (int i = 0; i < n; i++) {
            natural[i] = width(font, header.get(i));
            for (List<String> row : body) {
                natural[i] = Math.max(natural[i], width(font, row.get(i)));
            }
            natural[i] += 2 * PAD;
        }
        float[] out = new float[n];
        boolean[] fixed = new boolean[n];
        float left = PAGE.getWidth() - 2 * MARGIN;
        int open = n;
        for (boolean changed = true; changed && open > 0; ) {
            changed = false;
            float share = left / open;
            for (int i = 0; i < n; i++) {
                if (!fixed[i] && natural[i] <= share) {
                    out[i] = natural[i];
                    fixed[i] = true;
                    left -= natural[i];
                    open--;
                    changed = true;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            if (!fixed[i]) {
                out[i] = left / open;
            }
        }
        return out;
    }

    /** 按列宽折行：换行符处断开，超宽处逐字断开。 */
    private static List<String> wrap(PDType0Font font, String text, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); ) {
                int cp = paragraph.codePointAt(i);
                i += Character.charCount(cp);
                String next = line + new String(Character.toChars(cp));
                if (line.length() > 0 && font.getStringWidth(next) / 1000 * SIZE > width) {
                    lines.add(line.toString());
                    line = new StringBuilder(new String(Character.toChars(cp)));
                } else {
                    line = new StringBuilder(next);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    /** 逐页写表：每页标题、表头，行放不下换页（单行比整页还高的照画，不再换页）；关页前在最上层平铺暗纹。 */
    private static final class Sheet {
        private final PDDocument doc;
        private final PDType0Font font;
        private final String code;
        private final BitMatrix qr;
        private final String title;
        private final List<String> header;
        private final float[] widths;
        private PDPageContentStream out;
        private float y;
        private boolean rowsOnPage;

        Sheet(PDDocument doc, PDType0Font font, String code, String title, List<String> header, float[] widths) {
            this.doc = doc;
            this.font = font;
            this.code = code;
            this.qr = WatermarkQr.encode(code);
            this.title = title;
            this.header = header;
            this.widths = widths;
        }

        void write(List<List<String>> body) throws IOException {
            newPage();
            for (List<String> row : body) {
                row(row, false);
            }
            closePage();
        }

        private void newPage() throws IOException {
            if (out != null) {
                closePage();
            }
            PDPage page = new PDPage(PAGE);
            doc.addPage(page);
            out = new PDPageContentStream(doc, page);
            y = PAGE.getHeight() - MARGIN;
            out.beginText();
            out.setFont(font, TITLE_SIZE);
            out.newLineAtOffset(MARGIN, y - TITLE_SIZE);
            show(title);
            out.endText();
            y -= TITLE_SIZE + 10;
            rowsOnPage = false;
            row(header, true);
        }

        private void row(List<String> cells, boolean head) throws IOException {
            List<List<String>> lines = new ArrayList<>();
            int count = 1;
            for (int i = 0; i < widths.length; i++) {
                List<String> cell = wrap(font, cells.get(i), widths[i] - 2 * PAD);
                lines.add(cell);
                count = Math.max(count, cell.size());
            }
            float height = count * LEAD + 2 * PAD;
            if (!head && rowsOnPage && y - height < MARGIN) {
                newPage();
            }
            float x = MARGIN;
            for (int i = 0; i < widths.length; i++) {
                if (head) {
                    out.setNonStrokingColor(0.9f, 0.9f, 0.9f);
                    out.addRect(x, y - height, widths[i], height);
                    out.fill();
                }
                out.setStrokingColor(0.6f, 0.6f, 0.6f);
                out.setLineWidth(0.5f);
                out.addRect(x, y - height, widths[i], height);
                out.stroke();
                out.setNonStrokingColor(0f, 0f, 0f);
                List<String> cell = lines.get(i);
                for (int k = 0; k < cell.size(); k++) {
                    if (cell.get(k).isEmpty()) {
                        continue;
                    }
                    out.beginText();
                    out.setFont(font, SIZE);
                    out.newLineAtOffset(x + PAD, y - PAD - SIZE - k * LEAD);
                    show(cell.get(k));
                    out.endText();
                }
                x += widths[i];
            }
            y -= height;
            rowsOnPage |= !head;
        }

        /** 逐字绘制、不套连字：整串交给 showText 时 PDFBox 按字体 GSUB 把 ff、fi 等合成连字字形，连字不在 drawn 里、复制出来是别的字，字宽也与 width() 逐字算的不一致。 */
        private void show(String text) throws IOException {
            for (int cp : text.codePoints().toArray()) {
                out.showText(Character.toString(cp));
            }
        }

        /** 暗纹：与页面暗纹同一组参数（平铺 200、模块 6、格内偏移 24、不透明度 0.08，单位为点），二维码下附码的小字。 */
        private void closePage() throws IOException {
            PDExtendedGraphicsState faint = new PDExtendedGraphicsState();
            faint.setNonStrokingAlphaConstant((float) WatermarkQr.OPACITY);
            out.saveGraphicsState();
            out.setGraphicsStateParameters(faint);
            out.setNonStrokingColor(0f, 0f, 0f);
            float top = PAGE.getHeight();
            for (int ty = 0; ty < PAGE.getHeight(); ty += WatermarkQr.TILE_PX) {
                for (int tx = 0; tx < PAGE.getWidth(); tx += WatermarkQr.TILE_PX) {
                    for (int my = 0; my < qr.getHeight(); my++) {
                        for (int mx = 0; mx < qr.getWidth(); mx++) {
                            if (qr.get(mx, my)) {
                                out.addRect(tx + WatermarkQr.OFFSET_PX + mx * WatermarkQr.MODULE_PX,
                                        top - (ty + WatermarkQr.OFFSET_PX + (my + 1) * WatermarkQr.MODULE_PX),
                                        WatermarkQr.MODULE_PX, WatermarkQr.MODULE_PX);
                            }
                        }
                    }
                    out.fill();
                    out.beginText();
                    out.setFont(font, 12);
                    out.newLineAtOffset(tx + WatermarkQr.OFFSET_PX, top - (ty + WatermarkQr.OFFSET_PX + qr.getHeight() * WatermarkQr.MODULE_PX + 16));
                    show(code);
                    out.endText();
                }
            }
            out.restoreGraphicsState();
            out.close();
            out = null;
        }
    }

    private PasswordTrace trace(ResultSet rs) throws SQLException {
        return new PasswordTrace(rs.getString("id"), rs.getString("exporter_id"), rs.getString("username"),
                rs.getTimestamp("exported_at").toInstant(), rs.getString("device_no"), rs.getString("network"),
                rs.getString("export_type"), map(rs.getString("filters")), rs.getInt("row_count"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
