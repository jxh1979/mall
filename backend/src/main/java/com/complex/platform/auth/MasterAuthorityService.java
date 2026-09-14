package com.complex.platform.auth;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/**
 * U 盘破窗（最终验证器）登录：挑战—Ed25519 签名—放行。用途=系统刚装好无账号 / 管理员被误删 / 危急兜底时，
 * 凭 U 盘私钥登入并拿最高权限（不依赖 access_users 任何账号）。与普通管理员密码彻底分离。
 *
 * <p>R-004（2026-09-07 操作员裁定）：系统启动、破窗登录、手动清理留存数据等最高权限动作相关密钥都不保留在服务器上；
 * 服务器只留验签用的公钥（无法执行任何最高权限操作）。运行时不依赖 U 盘。
 * <p>默认关：未配置 {@code complex.auth.master.public-key} 时 dev/test 关闭，challenge/login 一律拒绝；
 * prod 缺公钥即拒绝启动（M008-A1-02：BSM-1.26.1 U 盘托管为上线必做，PGM-1.15.1 破窗是管理员全失锁时唯一恢复通道，不得静默缺省）。
 * <p>fail-closed：挑战一次性 + 短时效；错签名 / 过期 / 重放 / 未配置 全部拒绝并留痕。
 * <p>限速按来源（M008-A1-05）：匿名方刷挑战只耗尽自己来源的窗口，持钥人从另一来源仍可破窗；成功登录即重置该来源。
 * <p>留痕（M008-A1-04 / F-02，PGM-1.15.1「更换的每一步都留痕」）：每次启动记录当前验签公钥指纹
 * {@code auth.master.public_key_configured}，换钥后指纹变化可从审计流追溯。
 */
@Service
public class MasterAuthorityService {

    private static final Logger log = LoggerFactory.getLogger(MasterAuthorityService.class);

    private final SessionIssuer sessionIssuer;
    private final AuditEventService auditEventService;
    private final PublicKey publicKey;            // null = 未配置 = 功能关闭（prod 不允许）
    private final String publicKeyFingerprint;    // null = 未配置
    private final Duration challengeTtl;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Instant> challenges = new ConcurrentHashMap<>(); // challenge -> 过期时刻（一次性）

    // 4.9 破窗登录限速：每个来源 60 秒窗口内最多 5 次 challenge/login 尝试，超限 429 拒并审计留痕（防离线签名暴力尝试）。
    // ponytail: 来源 = 请求方地址（反代场景由 forward-headers-strategy 还原）；来源数超过上限整体清空，防匿名来源撑爆内存。
    private static final int RATE_LIMIT_MAX_ATTEMPTS = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofSeconds(60);
    private static final int RATE_LIMIT_SOURCE_CAP = 10_000;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private record Window(Instant startedAt, int attempts) {
    }

    private static String sourceOf(String sourceKey) {
        return sourceKey == null || sourceKey.isBlank() ? "unknown" : sourceKey.trim();
    }

    private void enforceRateLimit(String action, String sourceKey) {
        String source = sourceOf(sourceKey);
        Instant now = Instant.now();
        if (windows.size() > RATE_LIMIT_SOURCE_CAP) {
            windows.clear();
        }
        Window window = windows.compute(source, (key, old) ->
                old == null || now.isAfter(old.startedAt().plus(RATE_LIMIT_WINDOW))
                        ? new Window(now, 1)
                        : new Window(old.startedAt(), old.attempts() + 1));
        if (window.attempts() > RATE_LIMIT_MAX_ATTEMPTS) {
            auditEventService.recordFailure("master_auth." + action + "_rate_limited", "not_applicable",
                    "master_auth", source, null, null, "MASTER_AUTH_RATE_LIMITED");
            throw ApiException.tooManyRequests("MASTER_AUTH_RATE_LIMITED",
                    "主授权尝试次数过多，请稍后再试");
        }
    }

    public MasterAuthorityService(
            SessionIssuer sessionIssuer,
            AuditEventService auditEventService,
            Environment environment,
            @Value("${complex.auth.master.public-key:}") String publicKeyBase64,
            @Value("${complex.auth.master.challenge-ttl:PT2M}") Duration challengeTtl) {
        this.sessionIssuer = sessionIssuer;
        this.auditEventService = auditEventService;
        this.challengeTtl = (challengeTtl == null || challengeTtl.isNegative() || challengeTtl.isZero())
                ? Duration.ofMinutes(2) : challengeTtl;
        this.publicKey = parsePublicKey(publicKeyBase64);
        // 真实生产 = prod profile 且未带 prod-profile 集成测试豁免（与 ProductionDatabaseCredentialGuard 同一开关，
        // 仅对确以共享 dev 库跑 prod-profile 的集成测试置 false）。
        boolean production = environment != null && environment.acceptsProfiles(Profiles.of("prod"))
                && Boolean.parseBoolean(environment.getProperty("complex.database.credential-separation.enforced", "true"));
        if (production && this.publicKey == null) {
            // M008-A1-02：生产缺验签公钥 = 应急恢复通道不存在，且无人知晓；fail-closed 阻止启动（与 ProductionDatabaseCredentialGuard 同范式）。
            throw new IllegalStateException(
                    "生产环境必须配置 U 盘破窗验签公钥 COMPLEX_MASTER_AUTHORITY_PUBKEY（应急恢复通道不可缺省）");
        }
        this.publicKeyFingerprint = this.publicKey == null ? null : fingerprint(this.publicKey);
        if (this.publicKeyFingerprint != null) {
            // M008-A1-04 / F-02：每次启动记录当前验签公钥指纹，换钥（重新 provision）后指纹变化即为系统侧留痕。
            auditEventService.recordSuccess("auth.master.public_key_configured", "auth.master", "auth-session",
                    "master-public-key", null, null, Map.of("fingerprint", this.publicKeyFingerprint));
        }
    }

    public boolean enabled() {
        return publicKey != null;
    }

    /** 当前验签公钥指纹（SPKI DER 的 SHA-256 前 16 位十六进制）；未配置为 null。 */
    public String publicKeyFingerprint() {
        return publicKeyFingerprint;
    }

    /** 发一次性短时效挑战码；未启用则拒绝。sourceKey = 请求来源（限速分桶）。 */
    public String issueChallenge(String sourceKey) {
        enforceRateLimit("challenge", sourceKey);
        requireEnabled();
        pruneExpired();
        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        challenges.put(challenge, Instant.now().plus(challengeTtl));
        auditEventService.recordSuccess("auth.master.challenge_issued", "auth.master", "auth-session",
                "challenge", null, null, Map.of("source", sourceOf(sourceKey)));
        return challenge;
    }

    /** 校验挑战（存在/未过期/一次性）+ Ed25519 验签，通过则签发最高权限会话并重置该来源的限速窗口。 */
    public AuthService.LoginResponse login(String challenge, String signatureBase64, String sourceKey) {
        enforceRateLimit("login", sourceKey);
        requireEnabled();
        Instant expiry = challenge == null ? null : challenges.remove(challenge); // 一次性：取出即消费
        if (expiry == null || Instant.now().isAfter(expiry)) {
            fail("MASTER_CHALLENGE_INVALID", "challenge missing, expired or already used");
            throw ApiException.unauthorized("MASTER_CHALLENGE_INVALID", "授权挑战不存在、已过期或已被使用");
        }
        if (!verifySignature(challenge, signatureBase64)) {
            fail("MASTER_SIGNATURE_INVALID", "master signature verification failed");
            throw ApiException.unauthorized("MASTER_SIGNATURE_INVALID", "主授权签名校验未通过");
        }
        AuthService.LoginResponse response = sessionIssuer.issueMasterSession("usb-master:" + sourceOf(sourceKey));
        windows.remove(sourceOf(sourceKey));
        auditEventService.recordSuccess("auth.master.login_succeeded", "auth.master", "auth-session",
                SessionIssuer.MASTER_USER_ID, null, null,
                Map.of("session_id", response.sessionId(), "source", sourceOf(sourceKey)));
        return response;
    }

    private void requireEnabled() {
        if (!enabled()) {
            throw ApiException.forbidden("MASTER_AUTHORITY_DISABLED", "主授权尚未配置");
        }
    }

    private boolean verifySignature(String challenge, String signatureBase64) {
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            return false;
        }
        try {
            byte[] signature = Base64.getDecoder().decode(signatureBase64.trim());
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(challenge.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signature);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    private void fail(String code, String reason) {
        auditEventService.recordFailure("auth.master.login_failed", "auth.master", "auth-session",
                null, null, null, code);
        log.warn("[master-authority] break-glass login rejected: {} ({})", code, reason);
    }

    private void pruneExpired() {
        Instant now = Instant.now();
        challenges.entrySet().removeIf(e -> now.isAfter(e.getValue()));
    }

    static PublicKey parsePublicKey(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null; // 未 provision → 功能关闭（prod 由构造函数拒绝启动）
        }
        try {
            byte[] der = Base64.getDecoder().decode(base64.trim());
            return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception exception) {
            log.error("[master-authority] 配置的 public-key 无法解析（Ed25519 SPKI base64），功能保持关闭", exception);
            return null;
        }
    }

    static String fingerprint(PublicKey key) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getEncoded())).substring(0, 16);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 digest unavailable", exception);
        }
    }
}
