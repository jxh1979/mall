package com.complex.platform.compliance;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.SigningSecretGuard;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * AUD-001 企微会话同步入站 webhook 签名校验器。
 *
 * <p>与 {@code bar/BarInvitationTokenSigner}、{@code integration/OpenPlatformCrypto} 同 HMAC-SHA256 范式
 * （canonical 字段按字母序换行拼接 + {@link MessageDigest#isEqual} 常量时间比较），另叠加时间窗（±5min）+ nonce 防重放。
 * 企微回调/拉取通道无 staff 会话，签名 + 新鲜 timestamp + 唯一 nonce 是该端点真实鉴权闸：未签名/伪造/过期一律 fail-closed。
 * 密钥经 {@code compliance.wecom.webhook.signing.secret} 注入，生产由 env
 * {@code COMPLEX_WECOM_WEBHOOK_SIGNING_SECRET} 覆盖为强随机值（{@link SigningSecretGuard} 守卫弱/默认密钥）。
 */
@Component
public class WecomWebhookSignatureVerifier {

    static final String DEV_SIGNING_SECRET = "complex-dev-wecom-webhook-secret-change-me";
    /** 允许的时钟偏移窗口（秒）：签名 timestamp 与服务端 now 之差超过此值即判过期，配合 nonce 防重放。 */
    static final long TOLERANCE_SECONDS = 300;

    private final String secret;
    private final Clock clock;

    @Autowired
    public WecomWebhookSignatureVerifier(
            @Value("${compliance.wecom.webhook.signing.secret:complex-dev-wecom-webhook-secret-change-me}") String secret,
            Environment environment) {
        this(secret, environment.acceptsProfiles(Profiles.of("prod")), Clock.systemUTC());
    }

    WecomWebhookSignatureVerifier(String secret, boolean productionProfile, Clock clock) {
        this.secret = SigningSecretGuard.requireUsableHmacSecret(
                "compliance.wecom.webhook.signing.secret", "COMPLEX_WECOM_WEBHOOK_SIGNING_SECRET",
                secret, DEV_SIGNING_SECRET, productionProfile);
        this.clock = clock;
    }

    /**
     * 校验企微会话同步入站请求的签名 + 时间窗 + nonce。任何缺失/过期/失配一律抛 401（fail-closed）。
     *
     * @param signature 头 X-Wecom-Signature：对 canonical 串的 HMAC-SHA256 十六进制。
     * @param timestamp 头 X-Wecom-Timestamp：请求发起 epoch 秒；超 ±{@value #TOLERANCE_SECONDS}s 判过期。
     * @param nonce     头 X-Wecom-Nonce：单次随机串，参与签名（配合 timestamp 防重放）。
     */
    public void verify(String signature, Long timestamp, String nonce,
            String idempotencyKey, String externalConversationId, String projectId) {
        if (signature == null || signature.isBlank()) {
            throw ApiException.unauthorized("WECOM_WEBHOOK_SIGNATURE_MISSING", "wecom webhook signature is required");
        }
        if (timestamp == null) {
            throw ApiException.unauthorized("WECOM_WEBHOOK_TIMESTAMP_MISSING", "wecom webhook timestamp is required");
        }
        if (nonce == null || nonce.isBlank()) {
            throw ApiException.unauthorized("WECOM_WEBHOOK_NONCE_MISSING", "wecom webhook nonce is required");
        }
        long skewSeconds = Math.abs(clock.instant().getEpochSecond() - timestamp);
        if (skewSeconds > TOLERANCE_SECONDS) {
            throw ApiException.unauthorized("WECOM_WEBHOOK_TIMESTAMP_EXPIRED",
                    "wecom webhook timestamp is outside the allowed window");
        }
        String expected = sign(canonical(externalConversationId, idempotencyKey, nonce, projectId, timestamp));
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.unauthorized("WECOM_WEBHOOK_SIGNATURE_INVALID", "wecom webhook signature is invalid");
        }
    }

    /** 规范串：不可篡改字段按字段名字母序以「key=value」换行拼接（与 OpenPlatformCrypto webhookCanonical 同范式）。 */
    String canonical(String externalConversationId, String idempotencyKey, String nonce, String projectId,
            long timestamp) {
        return "external_conversation_id=" + nullToEmpty(externalConversationId)
                + "\nidempotency_key=" + nullToEmpty(idempotencyKey)
                + "\nnonce=" + nullToEmpty(nonce)
                + "\nproject_id=" + nullToEmpty(projectId)
                + "\ntimestamp=" + timestamp;
    }

    /** 对规范串计算 HMAC-SHA256，返回十六进制串。 */
    String sign(String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("wecom webhook HMAC-SHA256 is unavailable", exception);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
