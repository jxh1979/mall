package com.complex.platform.gate;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.StaffDeviceCredentialRecord;
import com.complex.platform.access.staff.StaffProfileRecord;
import com.complex.platform.access.staff.StaffRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.DeviceSigningKeyStore;
import com.complex.platform.terminal.TerminalDeviceRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/**
 * AUD-010 每请求设备凭证验签 + 服务端派生可信终端身份 + enforce 前置加固全套。
 *
 * <p>设备绑定型终端（staff_work_phone/tablet/kiosk）的<b>写</b>请求需对
 * {@code deviceId|timestamp|nonce|METHOD|path|sha256(body)} 的签名验签：staff_work_phone→员工 Keystore 公钥且
 * header deviceId 归属该员工（⑤）；kiosk/tablet→设备级签名公钥（{@code terminal_device_signing_keys}，②）。
 * 验签量含 body SHA-256（④）；nonce 由拦截器落库防重放（③）。
 *
 * <p>web/app 资金旁路（①）：web enforce 下拒 {@code Authorization: Bearer} 自报（合法后台走 Cookie 会话）；
 * app enforce 下拒写（客人端无公司设备凭证；app 资金是否要客人侧证明属业务口径，见 PRD）。
 *
 * <p>分级强制 {@code off|grace|enforce}（默认 grace=生产安全态）。读请求不因缺签名被拒。
 * <b>实际翻 enforce 依赖客户端实现签名 + 登记密钥（客户端侧工作，本 PR 不含）；本 PR 让服务端 ready-to-enforce。</b>
 */
@Component
public class DeviceCredentialVerifier {

    private static final Logger log = LoggerFactory.getLogger(DeviceCredentialVerifier.class);

    static final Set<String> DEVICE_BOUND_TYPES = Set.of("staff_work_phone", "tablet", "kiosk");
    private static final String STAFF_WORK_PHONE = "staff_work_phone";
    private static final String EVENT = "terminal.device_credential";
    /** 哨兵：⑤ deviceId 与所用密钥归属不符（内部返回，非真实密钥）。 */
    private static final String INVALID_OWNER = " OWNER_MISMATCH";

    enum Mode { OFF, GRACE, ENFORCE }

    private final TerminalDeviceRepository deviceRepository;
    private final StaffRepository staffRepository;
    private final DeviceSigningKeyStore deviceSigningKeyStore;
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
            DeviceSigningKeyStore deviceSigningKeyStore,
            AuthorizationService authorizationService,
            AuditEventService auditEventService,
            @Value("${access.device_credential.enforcement.default:grace}") String defaultMode,
            @Value("${access.device_credential.enforcement.staff_work_phone:}") String staffWorkPhoneMode,
            @Value("${access.device_credential.enforcement.tablet:}") String tabletMode,
            @Value("${access.device_credential.enforcement.kiosk:}") String kioskMode,
            @Value("${access.device_credential.enforcement.web:}") String webMode,
            @Value("${access.device_credential.enforcement.app:}") String appMode,
            @Value("${access.device_credential.timestamp_tolerance_seconds:300}") long toleranceSeconds) {
        this(deviceRepository, staffRepository, deviceSigningKeyStore, authorizationService, auditEventService,
                Clock.systemUTC(), defaultMode, staffWorkPhoneMode, tabletMode, kioskMode, webMode, appMode,
                toleranceSeconds);
    }

    DeviceCredentialVerifier(
            TerminalDeviceRepository deviceRepository,
            StaffRepository staffRepository,
            DeviceSigningKeyStore deviceSigningKeyStore,
            AuthorizationService authorizationService,
            AuditEventService auditEventService,
            Clock clock,
            String defaultMode,
            String staffWorkPhoneMode,
            String tabletMode,
            String kioskMode,
            String webMode,
            String appMode,
            long toleranceSeconds) {
        this.deviceRepository = deviceRepository;
        this.staffRepository = staffRepository;
        this.deviceSigningKeyStore = deviceSigningKeyStore;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.clock = clock;
        this.defaultMode = parseMode(defaultMode, Mode.GRACE);
        this.perTypeMode = new LinkedHashMap<>();
        putIfSet("staff_work_phone", staffWorkPhoneMode);
        putIfSet("tablet", tabletMode);
        putIfSet("kiosk", kioskMode);
        putIfSet("web", webMode);
        putIfSet("app", appMode);
        this.toleranceSeconds = toleranceSeconds > 0 ? toleranceSeconds : 300;
    }

    private void putIfSet(String type, String mode) {
        if (mode != null && !mode.isBlank()) {
            perTypeMode.put(type, parseMode(mode, defaultMode == null ? Mode.GRACE : defaultMode));
        }
    }

    private Mode modeFor(String type) {
        return perTypeMode.getOrDefault(type, defaultMode);
    }

    /** 派生本请求可信终端身份。设备绑定型/web/app 写请求在 enforce/无效签名下抛 403；其余返回放行态。 */
    public TerminalIdentityHolder.VerifiedTerminal verify(HttpServletRequest request) {
        String claimed = trim(request.getHeader("X-Terminal-Type"));
        String deviceId = trim(request.getHeader("X-Device-Id"));
        boolean write = isWrite(request.getMethod());

        if ("web".equals(claimed)) {
            if (write && hasBearer(request)) {
                if (modeFor("web") == Mode.ENFORCE) {
                    reject("web", deviceId, "web_requires_cookie_session");
                }
                audit("web", deviceId, "grace_bearer_self_reported_web", "success");
            }
            return new TerminalIdentityHolder.VerifiedTerminal(claimed, deviceId, false);
        }
        if ("app".equals(claimed)) {
            if (write) {
                if (modeFor("app") == Mode.ENFORCE) {
                    reject("app", deviceId, "app_requires_customer_proof");
                }
                audit("app", deviceId, "grace_allow_app_write", "success");
            }
            return new TerminalIdentityHolder.VerifiedTerminal(claimed, deviceId, false);
        }
        if (claimed == null || !DEVICE_BOUND_TYPES.contains(claimed)) {
            return new TerminalIdentityHolder.VerifiedTerminal(claimed, deviceId, false);
        }
        Mode mode = modeFor(claimed);
        if (mode == Mode.OFF) {
            return new TerminalIdentityHolder.VerifiedTerminal(claimed, deviceId, false);
        }
        Outcome outcome = tryVerify(request, claimed, deviceId);
        switch (outcome.kind()) {
            case VERIFIED:
                return new TerminalIdentityHolder.VerifiedTerminal(outcome.derivedType(), deviceId, true);
            case INVALID:
                reject(claimed, deviceId, outcome.reason());
                throw unreachable();
            case NO_PROOF:
            default:
                if (mode == Mode.ENFORCE && write) {
                    reject(claimed, deviceId, "credential_required");
                    throw unreachable();
                }
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
        if (Math.abs(clock.millis() - ts) > toleranceSeconds * 1000L) {
            return Outcome.invalid("stale_timestamp");
        }
        TerminalDeviceRecord device = deviceRepository.findByDeviceId(deviceId).orElse(null);
        if (device == null) {
            return Outcome.noProof();
        }
        if (!"bound".equals(device.status())) {
            return Outcome.invalid("device_not_bound");
        }
        if (device.terminalType() == null || !device.terminalType().equalsIgnoreCase(claimed)) {
            return Outcome.invalid("type_mismatch");
        }
        String publicKey = resolveSigningKey(claimed, device, deviceId);
        if (publicKey == null) {
            return Outcome.noProof();
        }
        if (INVALID_OWNER.equals(publicKey)) {
            return Outcome.invalid("device_owner_mismatch");
        }
        String bodyDigest = sha256Hex(cachedBody(request));
        String canonical = deviceId + "\n" + ts + "\n" + nonce + "\n"
                + request.getMethod().toUpperCase(Locale.ROOT) + "\n" + request.getRequestURI()
                + "\n" + bodyDigest;
        if (!verifySignature(publicKey, canonical, signature)) {
            return Outcome.invalid("bad_signature");
        }
        return Outcome.verified(device.terminalType());
    }

    private String resolveSigningKey(String claimed, TerminalDeviceRecord device, String deviceId) {
        if (STAFF_WORK_PHONE.equals(claimed)) {
            AccessActor actor = currentActorQuietly();
            if (actor == null || actor.id() == null) {
                return null;
            }
            StaffProfileRecord profile = staffRepository.findProfileByUserId(actor.id()).orElse(null);
            if (profile == null) {
                return null;
            }
            // ③ staffId 显式收口：无 staffId 的 bound staff_work_phone 设备不得回落用当前员工密钥 -> 视为归属不符拒。
            if (device.staffId() == null || !device.staffId().equals(profile.id())) {
                return INVALID_OWNER;
            }
            return staffRepository.findActiveCredential(profile.id())
                    .map(StaffDeviceCredentialRecord::devicePublicKey)
                    .filter(k -> k != null && !k.isBlank())
                    .orElse(null);
        }
        return deviceSigningKeyStore.findActivePublicKey(deviceId).filter(k -> !k.isBlank()).orElse(null);
    }

    private AccessActor currentActorQuietly() {
        try {
            return authorizationService.currentActor();
        } catch (RuntimeException unauthenticated) {
            return null;
        }
    }

    private static boolean hasBearer(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        return h != null && h.startsWith("Bearer ");
    }

    private static byte[] cachedBody(HttpServletRequest request) {
        CachedBodyHttpServletRequest cached = WebUtils.getNativeRequest(request, CachedBodyHttpServletRequest.class);
        return cached != null ? cached.cachedBody() : new byte[0];
    }

    private static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

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
            if (Boolean.TRUE.equals(tryVerifyWith(algo[0], algo[1], keyBytes, message, sig))) {
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
            return null;
        }
    }

    private void reject(String claimed, String deviceId, String reason) {
        audit(claimed, deviceId, reason, "failure");
        throw ApiException.forbidden("DEVICE_CREDENTIAL_INVALID",
                "device credential verification failed: " + reason);
    }

    private void audit(String claimed, String deviceId, String reason, String result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("terminal_type", claimed == null ? "unknown" : claimed);
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
