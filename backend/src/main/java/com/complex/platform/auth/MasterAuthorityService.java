package com.complex.platform.auth;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * U 盘破窗（最终验证器）登录：挑战—Ed25519 签名—放行。用途=系统刚装好无账号 / 管理员被误删 / 危急兜底时，
 * 凭 U 盘私钥登入并拿最高权限（不依赖 access_users 任何账号）。与普通管理员密码彻底分离。
 *
 * <p>默认关：未配置 {@code complex.auth.master.public-key}（由 U 盘安装期 provision）时不启用，challenge/login 一律拒绝。
 * <p>fail-closed：挑战一次性 + 短时效；错签名 / 过期 / 重放 / 未配置 全部拒绝并留痕。
 */
@Service
public class MasterAuthorityService {

    private static final Logger log = LoggerFactory.getLogger(MasterAuthorityService.class);

    private final SessionIssuer sessionIssuer;
    private final AuditEventService auditEventService;
    private final PublicKey publicKey;            // null = 未配置 = 功能关闭
    private final Duration challengeTtl;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Instant> challenges = new ConcurrentHashMap<>(); // challenge -> 过期时刻（一次性）

    public MasterAuthorityService(
            SessionIssuer sessionIssuer,
            AuditEventService auditEventService,
            @Value("${complex.auth.master.public-key:}") String publicKeyBase64,
            @Value("${complex.auth.master.challenge-ttl:PT2M}") Duration challengeTtl) {
        this.sessionIssuer = sessionIssuer;
        this.auditEventService = auditEventService;
        this.challengeTtl = (challengeTtl == null || challengeTtl.isNegative() || challengeTtl.isZero())
                ? Duration.ofMinutes(2) : challengeTtl;
        this.publicKey = parsePublicKey(publicKeyBase64);
    }

    public boolean enabled() {
        return publicKey != null;
    }

    /** 发一次性短时效挑战码；未启用则拒绝。 */
    public String issueChallenge() {
        requireEnabled();
        pruneExpired();
        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        challenges.put(challenge, Instant.now().plus(challengeTtl));
        auditEventService.recordSuccess("auth.master.challenge_issued", "auth.master", "auth-session",
                "challenge", null, null, Map.of());
        return challenge;
    }

    /** 校验挑战（存在/未过期/一次性）+ Ed25519 验签，通过则签发最高权限会话。 */
    public AuthService.LoginResponse login(String challenge, String signatureBase64) {
        requireEnabled();
        Instant expiry = challenge == null ? null : challenges.remove(challenge); // 一次性：取出即消费
        if (expiry == null || Instant.now().isAfter(expiry)) {
            fail("MASTER_CHALLENGE_INVALID", "challenge missing, expired or already used");
            throw ApiException.unauthorized("MASTER_CHALLENGE_INVALID", "challenge missing, expired or already used");
        }
        if (!verifySignature(challenge, signatureBase64)) {
            fail("MASTER_SIGNATURE_INVALID", "master signature verification failed");
            throw ApiException.unauthorized("MASTER_SIGNATURE_INVALID", "master signature verification failed");
        }
        AuthService.LoginResponse response = sessionIssuer.issueMasterSession();
        auditEventService.recordSuccess("auth.master.login_succeeded", "auth.master", "auth-session",
                SessionIssuer.MASTER_USER_ID, null, null, Map.of("session_id", response.sessionId()));
        return response;
    }

    private void requireEnabled() {
        if (!enabled()) {
            throw ApiException.forbidden("MASTER_AUTHORITY_DISABLED", "master authority is not provisioned");
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
            return null; // 未 provision → 功能关闭
        }
        try {
            byte[] der = Base64.getDecoder().decode(base64.trim());
            return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception exception) {
            log.error("[master-authority] 配置的 public-key 无法解析（Ed25519 SPKI base64），功能保持关闭", exception);
            return null;
        }
    }
}
