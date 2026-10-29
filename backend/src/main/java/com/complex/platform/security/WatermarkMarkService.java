// 文件版本：V0.01
package com.complex.platform.security;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第 23 条 23-R2 暗纹码：每打开一次页面、每生成一份导出文件发一个码并留痕（账号、设备编号、接入网络、时间），反查按码找回。
 * PGM:687「暗纹可反向推算……即可算出该页面是由哪个账号、在哪一台设备上、于什么时间打开的」「打开敏感页这件事本身也要留痕，记下是谁打开的、
 * 什么时间打开的、打开的是哪一个页面、这一次看到的是哪些对象……这条留痕不提供关闭开关」。设备编号按第十八批裁决 6 取浏览器设备编号
 * （员工手机取登记设备号，见 {@link BrowserDeviceRegistry#current}）。留痕不接任何开关（DEC-R3B-2026-08-04-14）。
 */
@Service
public class WatermarkMarkService {

    public static final String TRACE_PERMISSION = "security.watermark.trace";
    // 去掉易混的 0、O、1、I、L，共 31 个字符。
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」）。 */
    public record ObjectRef(String objectType, String objectId) {
    }

    /** 发出的码与打开（生成）时间。 */
    public record Mark(String code, Instant openedAt) {
    }

    /** 反查结果：哪个账号、哪一台设备（及接入网络）、什么时间，打开的哪个页面或哪份导出文件。 */
    public record Trace(String code, String accountId, String username, String deviceNo, String network, String kind,
                        String pageId, String exportFileId, Instant openedAt) {
    }

    private final JdbcTemplate jdbc;
    private final BrowserDeviceRegistry devices;
    private final AuthorizationService authorization;
    private final AccessRepository accounts;
    private final AuditEventService audit;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    public WatermarkMarkService(JdbcTemplate jdbc, BrowserDeviceRegistry devices, AuthorizationService authorization,
                                AccessRepository accounts, AuditEventService audit, ObjectMapper objectMapper,
                                HttpServletRequest request) {
        this.jdbc = jdbc;
        this.devices = devices;
        this.authorization = authorization;
        this.accounts = accounts;
        this.audit = audit;
        this.objectMapper = objectMapper;
        this.request = request;
    }

    /**
     * 发码并落留痕（须在请求内调用）：账号＝当前登录人，设备＝本请求的设备编号（为空 409 DEVICE_NOT_REGISTERED），网络＝来源地址。
     * kind＝page（页面，带 pageId）或 export（导出文件，带 exportFileId，23-R3 用）；sensitive＝true 另写审计 security.sensitive_page.viewed。
     */
    public Mark issue(String kind, String pageId, String exportFileId, boolean sensitive, List<ObjectRef> objectRefs) {
        AccessActor actor = authorization.currentActor();
        String deviceNo = devices.current(request)
                .orElseThrow(() -> ApiException.conflict("DEVICE_NOT_REGISTERED", "请重新登录后再打开"));
        String network = BrowserDeviceRegistry.network(request);
        List<Map<String, String>> refs = (objectRefs == null ? List.<ObjectRef>of() : objectRefs).stream()
                .map(ref -> Map.of("object_type", ref.objectType(), "object_id", ref.objectId()))
                .toList();
        String code = newCode();
        Instant openedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        jdbc.update("""
                INSERT INTO watermark_marks (code, kind, account_id, device_no, network, page_id, export_file_id, sensitive, object_refs, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)""",
                code, kind, actor.id(), deviceNo, network, pageId, exportFileId, sensitive, json(refs), Timestamp.from(openedAt));
        if (sensitive) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("page_id", pageId);
            metadata.put("object_refs", refs);
            metadata.put("device_no", deviceNo);
            metadata.put("network", network);
            audit.recordSuccess("security.sensitive_page.viewed", null, "watermark_mark", code, null, null, metadata);
        }
        return new Mark(code, openedAt);
    }

    /**
     * 截屏或拍屏照片反查（调用方先核 {@link #TRACE_PERMISSION}，单独授予）：解不出码、码不存在、或目标账号不在反查人可触及范围内，一律 404
     * WATERMARK_NOT_FOUND（范围外的人借此也探不出码是否存在）。每次反查都写审计：查到 security.watermark.traced；
     * 查不到或范围外 security.watermark.trace_failed（独立事务，记解出的第一个码）。
     */
    public Trace trace(BufferedImage image) {
        AccessActor actor = authorization.currentActor();
        Set<String> codes = WatermarkQr.decode(image);
        Trace found = codes.stream()
                .map(this::find)
                .flatMap(Optional::stream)
                .findFirst()
                .filter(trace -> authorization.isMasterAuthority(actor) || accounts.findUserById(trace.accountId())
                        .map(target -> authorization.canReachUser(actor, target, TRACE_PERMISSION)).orElse(false))
                .orElseThrow(() -> {
                    audit.recordFailure("security.watermark.trace_failed", TRACE_PERMISSION, "watermark_mark",
                            codes.stream().findFirst().orElse(null), null, null, "WATERMARK_NOT_FOUND");
                    return ApiException.notFound("WATERMARK_NOT_FOUND", "图片里没有能识别的暗纹，或不在您的查看范围内");
                });
        audit.recordSuccess("security.watermark.traced", TRACE_PERMISSION, "watermark_mark", found.code(), null, null,
                Map.of("account_id", found.accountId(), "device_no", found.deviceNo(), "kind", found.kind()));
        return found;
    }

    private Optional<Trace> find(String code) {
        return jdbc.query("""
                SELECT m.code, m.account_id, u.username, m.device_no, m.network, m.kind, m.page_id, m.export_file_id, m.created_at
                  FROM watermark_marks m LEFT JOIN access_users u ON u.id = m.account_id
                 WHERE m.code = ?""", (rs, row) -> new Trace(rs.getString("code"), rs.getString("account_id"),
                rs.getString("username"), rs.getString("device_no"), rs.getString("network"), rs.getString("kind"),
                rs.getString("page_id"), rs.getString("export_file_id"), rs.getTimestamp("created_at").toInstant()), code)
                .stream().findFirst();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String newCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
