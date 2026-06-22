package com.complex.platform.ticketing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * P0-3 / X-01 门票二维码 HMAC 签名器。
 *
 * <p>业务 HTML（business-system-map :692-693）要求「二维码必须带签名」。本签名器在签发动态核销凭证时对凭证关键字段
 * 计算 HMAC-SHA256 签名（写入 ticket_credentials.signature_value/signature_version），核销端按相同规范串重算并以
 * 常量时间比较校验（写入 ticket_verifications.signature_valid），使离线截图重放/篡改凭证字段（如延长有效期）必然签名失配。
 *
 * <p>密码学范式与 {@code integration/OpenPlatformCrypto} 一致（HmacSHA256 + {@link MessageDigest#isEqual} 常量时间比较），
 * 但聚焦门票凭证、零跨包依赖——不复用 integration 包的 package-private 类（其混入大量 open-platform 专用方法、跨包暴露会破坏封装）。
 *
 * <p>密钥经配置注入 {@code ticket.credential.signing.secret}，生产环境由 env {@code COMPLEX_TICKET_SIGNING_SECRET} 覆盖；
 * {@code signature_version} 标记密钥版本，为后续密钥轮转预留（本期单版本，核销端按 version 选密钥的逻辑待多版本时再扩展）。
 */
@Component
public class TicketCredentialSigner {

    private final String secret;
    private final String version;

    public TicketCredentialSigner(
            @Value("${ticket.credential.signing.secret:complex-dev-ticket-signing-secret-change-me}") String secret,
            @Value("${ticket.credential.signing.version:v1}") String version) {
        this.secret = secret;
        this.version = version;
    }

    /** 当前签名密钥版本，写入凭证 signature_version。 */
    String version() {
        return version;
    }

    /**
     * 防伪签名规范串：覆盖凭证不可篡改的关键字段（凭证 token / 权益 id / nonce / 刷新窗口锚点 / 有效期），
     * 按字段名字母序以「key=value」换行拼接（与 OpenPlatformCrypto canonical 范式一致）。
     * 签发与核销必须传入完全相同的字段值，否则签名失配。时间统一取毫秒纪元（DB TIMESTAMPTZ 微秒精度对毫秒无损）。
     */
    String canonical(
            String credentialToken, String entitlementId, String nonce, Instant windowStartedAt, Instant expiresAt) {
        return "credential_token=" + credentialToken
                + "\nentitlement_id=" + entitlementId
                + "\nexpires_at=" + epochMilli(expiresAt)
                + "\nnonce=" + nonce
                + "\nwindow_started_at=" + epochMilli(windowStartedAt);
    }

    /** 对规范串计算 HMAC-SHA256，返回十六进制串（写入 signature_value，VARCHAR(256) 容纳 64 hex 字符）。 */
    String sign(String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("ticket credential HMAC-SHA256 is unavailable", exception);
        }
    }

    /**
     * 常量时间比较校验签名，避免计时侧信道（与 OpenPlatformCrypto.verifySignature 同范式）。
     * 期望签名为空（存量未签名凭证）由调用方决定语义，此处空签名一律返回 false。
     */
    boolean matches(String canonical, String expectedSignature) {
        if (expectedSignature == null || expectedSignature.isBlank()) {
            return false;
        }
        String actual = sign(canonical);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8));
    }

    private long epochMilli(Instant instant) {
        return instant == null ? 0L : instant.toEpochMilli();
    }
}
