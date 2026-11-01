package com.complex.platform.gate;

import com.complex.platform.auth.AuthCookies;
import com.complex.platform.auth.AuthService;

// 文件版本：V0.08

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.StaffProfileRecord;
import com.complex.platform.access.staff.StaffDeviceCredentialRecord;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.DeviceSigningKeyStore;
import com.complex.platform.terminal.TerminalDeviceRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

/**
 * AUD-010 每请求设备凭证验签 + 服务端派生可信终端身份 + enforce 前置加固全套。
 *
 * <p>设备绑定型终端（staff_work_phone/tablet/kiosk）的<b>写</b>请求需对
 * {@code deviceId|timestamp|nonce|METHOD|path|sha256(body)} 的签名验签：staff_work_phone→工作手机设备级公钥且
 * header deviceId 归属该员工（⑤）；所有设备绑定型终端均使用设备级签名公钥（{@code terminal_device_signing_keys}，②）。
 * 验签量含 body SHA-256（④）；nonce 由拦截器落库防重放（③）。
 * multipart 请求（第 84 条 84-R1，工作手机签名上传证据）的摘要取部件规范串：参数按名排序逐行 {@code p:<名>=<值>}，
 * 再文件按字段名排序逐行 {@code f:<名>:<原文件名>:<文件字节 SHA-256>}，换行连接后取 SHA-256（与终端 multipartBodyDigest 逐字相同）。
 *
 * <p><b>2026-08-03 收口</b>：终端类型不再由自报的 {@code X-Terminal-Type} 决定入闸与否——带
 * {@code X-Device-Id} 时一律先反查 {@code terminal_devices.terminal_type} 顶上，登记类型压过自报值。
 * 旧实现在自报头为空/不在 DEVICE_BOUND_TYPES 时早退，客户端不发头或改报 web 即免签名写入，
 * <b>把 enforce 翻开也没用</b>。反查复用验签本来就要做的那一次查询，不新增查库次数。
 *
 * <p>web/app 资金旁路（①）：web enforce 下拒 {@code Authorization: Bearer} 自报（合法后台走 Cookie 会话）。
 *
 * <p>ROOT-03-R2 客人两端（PGM 第 745 行「客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）」）：客人路径上带有效客人会话时，
 * 会话锁定的登录端（app/miniapp）即可信终端，自报类型与之不符一律拒；没有客人会话的 app/miniapp 写请求只放行取码、登录、注册、登出，
 * 其余 enforce 下拒、grace 下留痕放行。公司设备凭证不适用于客人自己的手机。
 *
 * <p>第 25 条 25-R1 商户手机应用（PGM:744「前端隐藏只是辅助，后端接口必须独立校验」）：请求带商户手机应用会话时（登录只许商户账号），
 * 会话锁定的登录端即可信终端、不看自报类型，设备取会话设备编号（第十八批裁决 19）；无会话的请求照下方原分支。
 *
 * <p>第 91 条 91-R1 商户网页门户（PGM:728「商户侧 App/Web。」、PGM:744）：商户账号（与商户手机应用登录同口径）的请求，除设备绑定型外一律按
 * {@code merchant_portal} 判总开关、模块开关与灰度，改报 web、其他终端码或不带终端头都一样；设备绑定型照设备登记与验签判定
 * （商户员工在工作手机、平板、私人手机上作业，BSM:1889-1890、BSM:1899-1901）。
 *
 * <p>第 100 条 100-R2（PGM:744；PGM:745「Web 前端（业务人员，客人无网页端）」；BSM:1874-1875）：无商户归属的员工账号（不是经销商、客人）
 * 带终端头的请求，除设备绑定型外一律按 {@code web} 判总开关、模块开关与灰度，改报商户网页门户、商户手机应用、经销商门户、移动管理端、
 * 客人端都一样（与后台网页无商户归属报 web 同口径）；不带终端头的维持未识别终端。
 *
 * <p>分级强制 {@code off|grace|enforce}（默认 grace=生产安全态）。读请求不因缺签名被拒。
 * <b>实际翻 enforce 依赖客户端实现签名 + 登记密钥（客户端侧工作，本 PR 不含）；本 PR 让服务端 ready-to-enforce。</b>
 */
@Component
public class DeviceCredentialVerifier {

    private static final Logger log = LoggerFactory.getLogger(DeviceCredentialVerifier.class);

    // U01-001/U07-003：私人手机也是设备绑定型——身份由员工私人手机凭证（staff_device_credentials，指纹＝X-Device-Id）派生，不自报。
    static final String STAFF_PRIVATE_PHONE = "staff_private_phone";
    public static final Set<String> DEVICE_BOUND_TYPES = Set.of("staff_work_phone", "tablet", "kiosk", STAFF_PRIVATE_PHONE);
    private static final String STAFF_WORK_PHONE = "staff_work_phone";
    private static final String MERCHANT_PORTAL = "merchant_portal";
    private static final String WEB = "web";
    /** #553：登录请求此时还没有会话 Cookie，web/经销商门户不能因缺 Cookie 拒登录。 */
    private static final String WEB_LOGIN_PATH = "/api/v1/auth/login";
    static final String CUSTOMER_PATH_PREFIX = "/api/v1/customer/";
    /** 客人取码、登录、注册、登出：此时尚无（或已失去）客人会话。 */
    private static final String CUSTOMER_AUTH_PREFIX = "/api/v1/customer/auth/";
    /** ROOT-03-R7 非会员扫码点单的两个写口：下单、付款确认。 */
    private static final java.util.regex.Pattern CUSTOMER_SCAN_ORDER_PATH =
            java.util.regex.Pattern.compile("/api/v1/customer/bar-tables/[^/]+/scan-orders(/[^/]+/confirm)?");
    static final Set<String> CUSTOMER_TERMINALS = Set.of("app", "miniapp");
    private static final String EVENT = "terminal.device_credential";
    /** 哨兵：⑤ deviceId 与所用密钥归属不符（内部返回，非真实密钥）。 */
    private static final String INVALID_OWNER = " OWNER_MISMATCH";

    enum Mode { OFF, GRACE, ENFORCE }

    private final TerminalDeviceRepository deviceRepository;
    private final JdbcStaffRepository staffRepository;
    private final DeviceSigningKeyStore deviceSigningKeyStore;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final CustomerRepository customerRepository;
    private final Clock clock;

    private final Mode defaultMode;
    private final Map<String, Mode> perTypeMode;
    private final long toleranceSeconds;

    @org.springframework.beans.factory.annotation.Autowired
    public DeviceCredentialVerifier(
            TerminalDeviceRepository deviceRepository,
            JdbcStaffRepository staffRepository,
            DeviceSigningKeyStore deviceSigningKeyStore,
            AuthorizationService authorizationService,
            AuditEventService auditEventService,
            CustomerRepository customerRepository,
            @Value("${access.device_credential.enforcement.default:grace}") String defaultMode,
            @Value("${access.device_credential.enforcement.staff_work_phone:}") String staffWorkPhoneMode,
            @Value("${access.device_credential.enforcement.tablet:}") String tabletMode,
            @Value("${access.device_credential.enforcement.kiosk:}") String kioskMode,
            @Value("${access.device_credential.enforcement.staff_private_phone:}") String staffPrivatePhoneMode,
            @Value("${access.device_credential.enforcement.web:}") String webMode,
            @Value("${access.device_credential.enforcement.app:}") String appMode,
            @Value("${access.device_credential.enforcement.miniapp:}") String miniappMode,
            @Value("${access.device_credential.timestamp_tolerance_seconds:300}") long toleranceSeconds) {
        this(deviceRepository, staffRepository, deviceSigningKeyStore, authorizationService, auditEventService,
                customerRepository, Clock.systemUTC(), defaultMode, staffWorkPhoneMode, tabletMode, kioskMode,
                staffPrivatePhoneMode, webMode, appMode, miniappMode, toleranceSeconds);
    }

    DeviceCredentialVerifier(
            TerminalDeviceRepository deviceRepository,
            JdbcStaffRepository staffRepository,
            DeviceSigningKeyStore deviceSigningKeyStore,
            AuthorizationService authorizationService,
            AuditEventService auditEventService,
            CustomerRepository customerRepository,
            Clock clock,
            String defaultMode,
            String staffWorkPhoneMode,
            String tabletMode,
            String kioskMode,
            String staffPrivatePhoneMode,
            String webMode,
            String appMode,
            String miniappMode,
            long toleranceSeconds) {
        this.deviceRepository = deviceRepository;
        this.staffRepository = staffRepository;
        this.deviceSigningKeyStore = deviceSigningKeyStore;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.customerRepository = customerRepository;
        this.clock = clock;
        this.defaultMode = parseMode(defaultMode, Mode.GRACE);
        this.perTypeMode = new LinkedHashMap<>();
        putIfSet("staff_work_phone", staffWorkPhoneMode);
        putIfSet("tablet", tabletMode);
        putIfSet("kiosk", kioskMode);
        putIfSet(STAFF_PRIVATE_PHONE, staffPrivatePhoneMode);
        putIfSet("web", webMode);
        putIfSet("app", appMode);
        putIfSet("miniapp", miniappMode);
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

        // ROOT-03-R2：客人路径带有效客人会话 → 会话锁定的登录端即可信终端；自报类型不符任何模式都拒
        // （小程序会话拿到 App 端用、或改报别的端绕过本端开关）。
        CustomerSessionRecord session = activeCustomerSession(request);
        if (session != null) {
            if (session.scene() == null || !CUSTOMER_TERMINALS.contains(session.scene()) || !session.scene().equals(claimed)) {
                reject(claimed, deviceId, "terminal_session_mismatch");
            }
            return new TerminalIdentityHolder.VerifiedTerminal(session.scene(), null, true);
        }

        // 第 25 条 25-R1：商户手机应用会话 → 会话锁定的登录端即可信终端（同上客人会话口径），改报 merchant_portal/web 也按 merchant_app
        // 判总开关、模块开关与灰度；设备＝会话设备编号（裁决 19，暗纹与导出密码用它）。请求属性由令牌过滤器 attachSession 先放好。
        if (AuthService.MERCHANT_APP.equals(request.getAttribute("auth.terminalType"))) {
            Object sessionDevice = request.getAttribute("auth.deviceId");
            return new TerminalIdentityHolder.VerifiedTerminal(AuthService.MERCHANT_APP,
                    sessionDevice instanceof String appDevice ? appDevice : null, true);
        }

        // 服务端反查优先：登记类型压过自报头（不发头 / 改报 web 都不能绕过设备绑定型验签）。
        // 这一次查询下传 tryVerify 复用，不重复查库。
        TerminalDeviceRecord device = deviceId == null ? null
                : deviceRepository.findByDeviceId(deviceId).orElse(null);
        String terminalType = device != null && device.terminalType() != null && !device.terminalType().isBlank()
                ? device.terminalType().trim()
                : claimed;
        // 第 91 条 91-R1：商户账号的非设备绑定型请求由服务端定为商户网页门户，自报头不再改变判哪个端的开关。
        AccessActor actor = terminalType != null && DEVICE_BOUND_TYPES.contains(terminalType) ? null : currentActorQuietly();
        if (actor != null && AuthService.merchantAppAccount(actor.accountType(), actor.merchantId())) {
            terminalType = MERCHANT_PORTAL;
        } else if (actor != null && terminalType != null && (actor.merchantId() == null || actor.merchantId().isBlank())
                && !"dealer".equals(actor.accountType()) && !"customer".equals(actor.accountType())) {
            // 第 100 条 100-R2：无商户归属的员工账号带终端头的非设备绑定型请求按 Web 前端判。
            terminalType = WEB;
        }

        if (terminalType != null && DEVICE_BOUND_TYPES.contains(terminalType)) {
            Mode mode = modeFor(terminalType);
            if (mode == Mode.OFF) {
                return new TerminalIdentityHolder.VerifiedTerminal(terminalType, deviceId, false);
            }
            Outcome outcome = tryVerify(request, claimed, terminalType, device, deviceId);
            switch (outcome.kind()) {
                case VERIFIED:
                    return new TerminalIdentityHolder.VerifiedTerminal(outcome.derivedType(), deviceId, true);
                case INVALID:
                    reject(terminalType, deviceId, outcome.reason());
                    throw unreachable();
                case NO_PROOF:
                default:
                    if (mode == Mode.ENFORCE && write) {
                        reject(terminalType, deviceId, "credential_required");
                        throw unreachable();
                    }
                    if (write) {
                        audit(terminalType, deviceId, "grace_allow_no_credential", "success");
                    }
                    return new TerminalIdentityHolder.VerifiedTerminal(terminalType, deviceId, false);
            }
        }
        // U07-003/U12-005（BSM-044 三端不同用途、PGM L572 人机对应）：没有设备号的写请求 fail-close——
        // web 只认不可伪造的 Cookie 会话（admin_access_token），自报类型头本身不是证明；app 仍须客人侧证明；
        // 其它自报/缺失类型一律按无证明处理。grace 模式只留痕放行，enforce 模式拒绝。
        if (write) {
            if ("web".equals(terminalType) || "dealer_portal".equals(terminalType)) {
                // CG-11d：独立经销商 Web 门户（第十一端）与员工 Web 端同口径——浏览器 Cookie 会话即证明，不要求设备凭证。
                if (AuthCookies.readAccessToken(request) == null && !WEB_LOGIN_PATH.equals(request.getRequestURI())) {
                    if (modeFor(terminalType) == Mode.ENFORCE) {
                        reject(terminalType, deviceId, "web_requires_cookie_session");
                    }
                    audit(terminalType, deviceId, hasBearer(request) ? "grace_bearer_self_reported_web" : "grace_web_without_session", "success");
                }
            } else if (terminalType != null && CUSTOMER_TERMINALS.contains(terminalType)) {
                // ROOT-03-R2：客人两端的证明＝已验证客人会话（上面已放行）；无会话只放行取码、登录、注册、登出。
                String uri = request.getRequestURI();
                if (uri != null && uri.startsWith(CUSTOMER_AUTH_PREFIX)) {
                    audit(terminalType, deviceId, "customer_auth_without_session", "success");
                } else if (uri != null && CUSTOMER_SCAN_ORDER_PATH.matcher(uri).matches()) {
                    // ROOT-03-R7（ENUM 第 129 行「扫码点单身份：非会员可以浏览、点单和付款」）：扫码点单下单与付款确认不要求客人会话。
                    audit(terminalType, deviceId, "customer_scan_order_without_session", "success");
                } else {
                    if (modeFor(terminalType) == Mode.ENFORCE) {
                        // 带了客人令牌、但会话已过期或已失效：仍拒绝，报 401 与 CustomerService.requireCustomer 同码——客户端约定 401 才回登录页；
                        // 完全不带令牌的写请求照旧按无终端证明 403。
                        if (uri != null && uri.startsWith(CUSTOMER_PATH_PREFIX)
                                && (trim(request.getHeader("X-Customer-Token")) != null || hasBearer(request))) {
                            audit(terminalType, deviceId, "customer_session_expired", "failure");
                            throw ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "客人登录已过期，请重新登录");
                        }
                        reject(terminalType, deviceId, "customer_session_required");
                    }
                    audit(terminalType, deviceId, "grace_allow_app_write", "success");
                }
            } else {
                Mode mode = terminalType == null ? defaultMode : modeFor(terminalType);
                if (mode == Mode.ENFORCE) {
                    reject(terminalType, deviceId, "terminal_proof_required");
                }
                audit(terminalType, deviceId, "grace_allow_unproven_write", "success");
            }
        }
        return new TerminalIdentityHolder.VerifiedTerminal(terminalType, deviceId, false);
    }

    private Outcome tryVerify(HttpServletRequest request, String claimed, String terminalType,
            TerminalDeviceRecord device, String deviceId) {
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
        // 私人手机没有 terminal_devices 登记行：设备号＝员工凭证指纹，钥由 resolveSigningKey 按当前员工凭证解析。
        boolean privatePhone = device == null && STAFF_PRIVATE_PHONE.equals(terminalType);
        if (device == null && !privatePhone) {
            return Outcome.noProof();
        }
        if (device != null && !"bound".equals(device.status())) {
            return Outcome.invalid("device_not_bound");
        }
        // 自报头也报了设备绑定型、却与登记类型不符 → 篡改信号，仍按 type_mismatch 拒；
        // 自报为空/web/app 时不视为不符（登记类型已顶上，由签名而非自报头决定放行）。
        if (device != null && (device.terminalType() == null
                || (claimed != null && DEVICE_BOUND_TYPES.contains(claimed)
                        && !device.terminalType().equalsIgnoreCase(claimed)))) {
            return Outcome.invalid("type_mismatch");
        }
        String publicKey = resolveSigningKey(terminalType, device, deviceId);
        if (publicKey == null) {
            return Outcome.noProof();
        }
        if (INVALID_OWNER.equals(publicKey)) {
            return Outcome.invalid("device_owner_mismatch");
        }
        MultipartHttpServletRequest multipart = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
        String bodyDigest = multipart != null ? multipartDigest(multipart) : sha256Hex(cachedBody(request));
        String canonical = deviceId + "\n" + ts + "\n" + nonce + "\n"
                + request.getMethod().toUpperCase(Locale.ROOT) + "\n" + request.getRequestURI()
                + "\n" + bodyDigest;
        if (!verifySignature(publicKey, canonical, signature)) {
            return Outcome.invalid("bad_signature");
        }
        return Outcome.verified(device == null ? terminalType : device.terminalType());
    }

    private String resolveSigningKey(String terminalType, TerminalDeviceRecord device, String deviceId) {
        if (STAFF_PRIVATE_PHONE.equals(terminalType)) {
            // U01-001：私人手机身份＝当前员工的生效凭证（安装时生成并绑定的 Keystore 公钥），指纹须等于所报设备号。
            AccessActor actor = currentActorQuietly();
            if (actor == null || actor.id() == null) {
                return null;
            }
            StaffProfileRecord profile = staffRepository.findProfileByUserId(actor.id()).orElse(null);
            if (profile == null) {
                return null;
            }
            StaffDeviceCredentialRecord credential = staffRepository.findActiveCredential(profile.id()).orElse(null);
            if (credential == null || credential.deviceFingerprintHash() == null
                    || !credential.deviceFingerprintHash().equalsIgnoreCase(deviceId)) {
                return INVALID_OWNER;
            }
            return credential.devicePublicKey();
        }
        if (STAFF_WORK_PHONE.equals(terminalType)) {
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
        }
        return deviceSigningKeyStore.findActivePublicKey(deviceId).filter(k -> !k.isBlank()).orElse(null);
    }

    /** 客人路径上 X-Customer-Token（或 Bearer）对应的有效未过期客人会话；没有则 null。 */
    private CustomerSessionRecord activeCustomerSession(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (customerRepository == null || uri == null || !uri.startsWith(CUSTOMER_PATH_PREFIX)) {
            return null;
        }
        String token = trim(request.getHeader("X-Customer-Token"));
        if (token == null && hasBearer(request)) {
            token = trim(request.getHeader("Authorization").substring("Bearer ".length()));
        }
        if (token == null) {
            return null;
        }
        return customerRepository.findSessionByToken(token)
                .filter(s -> "active".equals(s.status()) && s.expiresAt() != null && s.expiresAt().isAfter(clock.instant()))
                .orElse(null);
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

    /** 84-R1 multipart 部件规范串的 SHA-256；文件流式取摘要，不整读进内存。读不出的文件记空摘要，验签自然不过（fail-closed）。 */
    static String multipartDigest(MultipartHttpServletRequest request) {
        List<String> lines = new ArrayList<>();
        new TreeMap<>(request.getParameterMap()).forEach((name, values) -> {
            for (String value : values) {
                lines.add("p:" + name + "=" + value);
            }
        });
        new TreeMap<>(request.getMultiFileMap()).forEach((name, files) -> {
            for (MultipartFile file : files) {
                lines.add("f:" + name + ":" + file.getOriginalFilename() + ":" + fileSha256Hex(file));
            }
        });
        return sha256Hex(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    private static String fileSha256Hex(MultipartFile file) {
        try (DigestInputStream in = new DigestInputStream(file.getInputStream(), MessageDigest.getInstance("SHA-256"))) {
            in.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(in.getMessageDigest().digest());
        } catch (IOException | java.security.NoSuchAlgorithmException e) {
            return "";
        }
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

    private void reject(String terminalType, String deviceId, String reason) {
        audit(terminalType, deviceId, reason, "failure");
        throw ApiException.forbidden("DEVICE_CREDENTIAL_INVALID",
                "设备凭据校验未通过：" + reason);
    }

    private void audit(String terminalType, String deviceId, String reason, String result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("terminal_type", terminalType == null ? "unknown" : terminalType);
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
        return ApiException.forbidden("DEVICE_CREDENTIAL_INVALID", "设备凭据校验出现内部异常，请联系管理员");
    }

    /** 写请求判定（非 GET/HEAD/OPTIONS）。终端总开关拦截器复用同一判定，两道闸对"写"的口径必须一致。 */
    static boolean isWrite(String method) {
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
