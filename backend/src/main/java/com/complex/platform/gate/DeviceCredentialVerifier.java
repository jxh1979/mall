package com.complex.platform.gate;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.StaffDeviceCredentialRecord;
import com.complex.platform.access.staff.StaffProfileRecord;
import com.complex.platform.access.staff.StaffRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.TerminalDeviceRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AUD-010 每请求设备凭证验签 + 服务端派生可信终端身份。
 *
 * <p><b>问题</b>:{@code X-Terminal-Type}/{@code X-Device-Id} 由客户端自报,攻击者不带凭证、直接自报
 * {@code staff_work_phone} 即可通过"仅公司设备"的资金/切换/入场闸。<b>修复</b>:对设备绑定型终端类型
 * (staff_work_phone/tablet/kiosk)的<b>写</b>请求,要求携带对 {@code deviceId|timestamp|nonce|METHOD|path}
 * 的密码学签名,以设备注册公钥(复用既有员工 Android Keystore 设备凭证 {@code staff_device_credentials}.
 * device_public_key)验签,并核对 {@code terminal_devices} 登记(存在+已绑定+类型一致)。验签通过则以
 * <b>设备登记类型</b>为可信终端类型注入 {@link TerminalIdentityHolder}(服务端真相覆盖自报头)。
 *
 * <p><b>分级强制(生产滚动安全)</b>:按终端类型可配 {@code off|grace|enforce}(默认 {@code grace}):
 * <ul>
 *   <li>{@code off}:不验签,直接放行自报类型(上线前/停用)。</li>
 *   <li>{@code grace}(默认,生产安全态):<b>签名无效必拒</b>(403);但<b>缺签名/未登记设备</b>的写请求
 *       记审计后放行(不瘫痪尚未登记签名密钥的现网终端)。</li>
 *   <li>{@code enforce}:设备绑定型写请求<b>必须</b>有效签名 + 已绑定设备,缺失/无效一律 403。</li>
 * </ul>
 * 读请求(GET/HEAD/OPTIONS)不因缺签名被拒(仅在有有效签名时派生可信类型),把强制面收敛到会移动资金/状态的写动作。
 *
 * <p>重放:无状态(时间窗 {@code ±tolerance} + nonce 纳入签名量),不建 nonce 表;资金写动作叠加既有
 * idempotency_key 去重,窗口内重放同一请求不产生二次副作用。<b>ponytail 上限</b>:签名量绑定 method+path,
 * 未绑定请求体——翻转 enforce 前若需防"截获有效签名后篡改请求体",用 ContentCachingRequestWrapper 追加 body 摘要。
 */
@Component
public class DeviceCredentialVerifier {

    private static final Logger log = LoggerFactory.getLogger(DeviceCredentialVerifier.class);

    /** 设备绑定型终端类型:其写动作需设备凭证验签(web/admin 浏览器端靠会话认证,app 为客人端,均不在内)。 */
    static final Set<String> DEVICE_BOUND_TYPES = Set.of("staff_work_phone", "tablet", "kiosk");

    private static final String EVENT = "terminal.device_credential";

    enum Mode { OFF, GRACE, ENFORCE }

    private final TerminalDeviceRepository deviceRepository;
    private final StaffRepository staffRepository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final Clock clock;

    private final Mode defaultMode;
    private final Map<String, Mode> perTypeMode;
    private final long toleranceSeconds;

    @org.springframework.beans.factory.annotation.Autowired
    public DeviceCredentialVerifier(
            TerminalDeviceRepository deviceRepository,
            StaffRepository staffRepository,
            AuthorizationService authorizationService,
            AuditEventService auditEventService,
            @Value("${access.device_credential.enforcement.default:grace}") String defaultMode,
            @Value("${access.device_credential.enforcement.staff_work_phone:}") String staffWorkPhoneMode,
            @Value("${access.device_credential.enforcement.tablet:}") String tabletMode,
            @Value("${access.device_credential.enforcement.kiosk:}") String kioskMode,
            @Value("${access.device_credential.timestamp_tolerance_seconds:300}") long toleranceSeconds) {
        this(deviceRepository, staffRepository, authorizationService, auditEventService, Clock.systemUTC(),
                defaultMode, staffWorkPhoneMode, tabletMode, kioskMode, toleranceSeconds);
    }

    DeviceCredentialVerifier(
            TerminalDeviceRepository deviceRepository,
            StaffRepository staffRepository,
            AuthorizationService authorizationService,
            AuditEventService auditEventService,
            Clock clock,
            String defaultMode,
            String staffWorkPhoneMode,
            String tabletMode,
            String kioskMode,
            long toleranceSeconds) {
        this.deviceRepository = deviceRepository;
        this.staffRepository = staffRepository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.clock = clock;
        this.defaultMode = parseMode(defaultMode, Mode.GRACE);
        this.perTypeMode = new LinkedHashMap<>();
        putIfSet("staff_work_phone", staffWorkPhoneMode);
        putIfSet("tablet", tabletMode);
        putIfSet("kiosk", kioskMode);
        this.toleranceSeconds = toleranceSeconds > 0 ? toleranceSeconds : 300;
    }

    private void putIfSet(String type, String mode) {
        if (mode != null && !mode.isBlank()) {
            perTypeMode.put(type, parseMode(mode, defaultMode == null ? Mode.GRACE : defaultMode));
        }
    }

    /**
     * 派生本请求可信终端身份。设备绑定型写请求在 enforce/无效签名下抛 403(fail-closed);其余返回放行态。
     */
    public TerminalIdentityHolder.VerifiedTerminal verify(HttpServletRequest request) {
        String claimed = trim(request.getHeader("X-Terminal-Type"));
        String deviceId = trim(request.getHeader("X-Device-Id"));
        if (claimed == null || !DEVICE_BOUND_TYPES.contains(claimed)) {
            // 非设备绑定型(web/admin/app/未声明)→ 保持原样,不验签。
            return new TerminalIdentityHolder.VerifiedTerminal(claimed, deviceId, false);
        }
        Mode mode = perTypeMode.getOrDefault(claimed, defaultMode);
        if (mode == Mode.OFF) {
            return new TerminalIdentityHolder.VerifiedTerminal(claimed, deviceId, false);
        }
        boolean write = isWrite(request.getMethod());
        Outcome outcome = tryVerify(request, claimed, deviceId);
        switch (outcome.kind()) {
            case VERIFIED:
                return new TerminalIdentityHolder.VerifiedTerminal(outcome.derivedType(), deviceId, true);
            case INVALID:
                // 提供了凭证但无效/矛盾 → 任何模式都拒绝(不能把伪造证据当放行)。
                reject(claimed, deviceId, outcome.reason());
                throw unreachable();
            case NO_PROOF:
            default:
                if (mode == Mode.ENFORCE && write) {
                    reject(claimed, deviceId, "credential_required");
                    throw unreachable();
                }
                // grace 或读请求:记审计后放行自报类型(未登记设备灰度期)。
                if (write) {
                    audit(claimed, deviceId, "grace_allow_no_credential", "success");
                }
                return new TerminalIdentityHolder.VerifiedTerminal(claimed, deviceId, false);
        }
    }

    private Outcome tryVerify(HttpServletRequest request, String claimed, String deviceId) {
        String timestamp = trim(request.getHeader("X-Device-Timestamp"));
        String nonce = trim(request.getHeader("X-Device-Nonce"));
        String signature = trim(request.getHeader("X-Device-Signature"));
        if (deviceId == null || timestamp == null || nonce == null || signature == null) {
            return Outcome.noProof();
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return Outcome.invalid("bad_timestamp");
        }
        long now = clock.millis();
        if (Math.abs(now - ts) > toleranceSeconds * 1000L) {
            return Outcome.invalid("stale_timestamp");
        }
        TerminalDeviceRecord device = deviceRepository.findByDeviceId(deviceId).orElse(null);
        if (device == null) {
            // 未登记设备:视作未登记(软)——grace 放行、enforce 拒绝,不在灰度期硬拒尚未登记的设备。
            return Outcome.noProof();
        }
        if (!"bound".equals(device.status())) {
            return Outcome.invalid("device_not_bound");
        }
        if (device.terminalType() == null || !device.terminalType().equalsIgnoreCase(claimed)) {
            return Outcome.invalid("type_mismatch");
        }
        String publicKey = resolveSigningKey();
        if (publicKey == null) {
            // 无可核对的注册公钥(设备/员工未登记签名密钥):软态——grace 放行、enforce 拒绝。
            return Outcome.noProof();
        }
        String canonical = deviceId + "\n" + ts + "\n" + nonce + "\n"
                + request.getMethod().toUpperCase(Locale.ROOT) + "\n" + request.getRequestURI();
        if (!verifySignature(publicKey, canonical, signature)) {
            return Outcome.invalid("bad_signature");
        }
        return Outcome.verified(device.terminalType());
    }

    /** 经认证员工 → 员工档案 → active 设备凭证公钥(Android Keystore)。无认证/无档案/无凭证 → null。 */
    private String resolveSigningKey() {
        AccessActor actor;
        try {
            actor = authorizationService.currentActor();
        } catch (RuntimeException unauthenticated) {
            return null;
        }
        if (actor == null || actor.id() == null) {
            return null;
        }
        Optional<StaffProfileRecord> profile = staffRepository.findProfileByUserId(actor.id());
        if (profile.isEmpty()) {
            return null;
        }
        return staffRepository.findActiveCredential(profile.get().id())
                .map(StaffDeviceCredentialRecord::devicePublicKey)
                .filter(k -> k != null && !k.isBlank())
                .orElse(null);
    }

    /** X.509(SubjectPublicKeyInfo)base64 公钥;EC→SHA256withECDSA,回退 RSA→SHA256withRSA。解析/验签异常均判失败。 */
    private boolean verifySignature(String publicKeyBase64, String message, String signatureBase64) {
        byte[] sig;
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            sig = Base64.getDecoder().decode(signatureBase64);
        } catch (IllegalArgumentException e) {
            return false;
        }
        for (String[] algo : new String[][] {{"EC", "SHA256withECDSA"}, {"RSA", "SHA256withRSA"}}) {
            Boolean ok = tryVerifyWith(algo[0], algo[1], keyBytes, message, sig);
            if (Boolean.TRUE.equals(ok)) {
                return true;
            }
        }
        return false;
    }

    private Boolean tryVerifyWith(String keyAlgo, String sigAlgo, byte[] keyBytes, String message, byte[] sig) {
        try {
            PublicKey key = KeyFactory.getInstance(keyAlgo).generatePublic(new X509EncodedKeySpec(keyBytes));
            Signature verifier = Signature.getInstance(sigAlgo);
            verifier.initVerify(key);
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(sig);
        } catch (RuntimeException | java.security.GeneralSecurityException e) {
            return null; // wrong key family or malformed → try next / fail
        }
    }

    private void reject(String claimed, String deviceId, String reason) {
        audit(claimed, deviceId, reason, "failure");
        throw ApiException.forbidden("DEVICE_CREDENTIAL_INVALID",
                "device credential verification failed: " + reason);
    }

    private void audit(String claimed, String deviceId, String reason, String result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("terminal_type", claimed);
        if (deviceId != null) {
            payload.put("device_id", deviceId);
        }
        payload.put("reason", reason);
        payload.put("result", result);
        try {
            auditEventService.recordSuccess(EVENT, "terminal.device_credential.verify",
                    "terminal_device", deviceId == null ? "unknown" : deviceId, null, null, payload);
        } catch (RuntimeException ignored) {
            log.debug("device credential audit write failed", ignored);
        }
    }

    private static ApiException unreachable() {
        return ApiException.forbidden("DEVICE_CREDENTIAL_INVALID", "unreachable");
    }

    private static boolean isWrite(String method) {
        String m = method == null ? "" : method.toUpperCase(Locale.ROOT);
        return !("GET".equals(m) || "HEAD".equals(m) || "OPTIONS".equals(m));
    }

    private static Mode parseMode(String raw, Mode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Mode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private enum Kind { VERIFIED, INVALID, NO_PROOF }

    private record Outcome(Kind kind, String derivedType, String reason) {
        static Outcome verified(String type) {
            return new Outcome(Kind.VERIFIED, type, null);
        }

        static Outcome invalid(String reason) {
            return new Outcome(Kind.INVALID, null, reason);
        }

        static Outcome noProof() {
            return new Outcome(Kind.NO_PROOF, null, null);
        }
    }
}
