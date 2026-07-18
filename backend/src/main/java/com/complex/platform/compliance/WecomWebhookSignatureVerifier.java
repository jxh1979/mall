package com.complex.platform.compliance;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.SigningSecretGuard;
import com.complex.platform.compliance.dto.WecomConversationSyncRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
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
 * 企微回调/拉取通道无 staff 会话，签名 + 新鲜 timestamp + 唯一 nonce 是该端点真实鉴权闸：未签名/伪造/过期/重放一律 fail-closed。
 * 密钥经 {@code compliance.wecom.webhook.signing.secret} 注入，生产由 env
 * {@code COMPLEX_WECOM_WEBHOOK_SIGNING_SECRET} 覆盖为强随机值（{@link SigningSecretGuard} 守卫弱/默认密钥）。
 *
 * <p>AUD 复核 P1-2：
 * <ul>
 *   <li>canonical 纳入报文体全字段（ownerStaffId/customerRef/messages[].*）的 SHA-256 摘要——任何内容字段改动都致验签失败，
 *       封死"有效签名但篡改消息内容/发送者"的缺口；</li>
 *   <li>nonce 持久化去重（{@link WecomWebhookNonceStore}，TTL≈时间窗）——同 nonce 二次提交在窗口内亦被拒（防重放）。</li>
 * </ul>
 */
@Component
public class WecomWebhookSignatureVerifier {

    static final String DEV_SIGNING_SECRET = "complex-dev-wecom-webhook-secret-change-me";
    /** 允许的时钟偏移窗口（秒）：签名 timestamp 与服务端 now 之差超过此值即判过期，配合 nonce 防重放。 */
    static final long TOLERANCE_SECONDS = 300;

    private final String secret;
    private final Clock clock;
    /** AUD 复核 P1-2：nonce 去重持久化。可空——纯单元测试（无库）构造不注入，跳过持久化去重。 */
    private final WecomWebhookNonceStore nonceStore;

    @Autowired
    public WecomWebhookSignatureVerifier(
            @Value("${compliance.wecom.webhook.signing.secret:complex-dev-wecom-webhook-secret-change-me}") String secret,
            Environment environment,
            WecomWebhookNonceStore nonceStore) {
        this(secret, environment.acceptsProfiles(Profiles.of("prod")), Clock.systemUTC(), nonceStore);
    }

    /** 纯单元测试构造（无 nonce 持久化）。 */
    WecomWebhookSignatureVerifier(String secret, boolean productionProfile, Clock clock) {
        this(secret, productionProfile, clock, null);
    }

    WecomWebhookSignatureVerifier(String secret, boolean productionProfile, Clock clock,
            WecomWebhookNonceStore nonceStore) {
        this.secret = SigningSecretGuard.requireUsableHmacSecret(
                "compliance.wecom.webhook.signing.secret", "COMPLEX_WECOM_WEBHOOK_SIGNING_SECRET",
                secret, DEV_SIGNING_SECRET, productionProfile);
        this.clock = clock;
        this.nonceStore = nonceStore;
    }

    /**
     * 校验企微会话同步入站请求的签名 + 时间窗 + 报文体摘要 + nonce 去重。任何缺失/过期/失配/重放一律抛 401（fail-closed）。
     *
     * @param signature 头 X-Wecom-Signature：对 canonical 串（含报文体摘要）的 HMAC-SHA256 十六进制。
     * @param timestamp 头 X-Wecom-Timestamp：请求发起 epoch 秒；超 ±{@value #TOLERANCE_SECONDS}s 判过期。
     * @param nonce     头 X-Wecom-Nonce：单次随机串，参与签名并持久化去重（配合 timestamp 防重放）。
     * @param request   完整报文体：其 idempotencyKey/externalConversationId/projectId + ownerStaffId/customerRef/messages 全部纳入签名。
     */
    public void verify(String signature, Long timestamp, String nonce, WecomConversationSyncRequest request) {
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
        // AUD-017：标识字段拒绝控制字符（自由文本 content 除外）——结构样式文本混进标识字段=篡改信号。
        validateIdentifierFields(request, nonce);
        String expected = sign(canonical(request, nonce, timestamp));
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.unauthorized("WECOM_WEBHOOK_SIGNATURE_INVALID", "wecom webhook signature is invalid");
        }
        // AUD 复核 P1-2 + AUD-021：验签通过后原子登记 nonce+报文指纹。异指纹=nonce 被挪用于不同报文，判重放拒绝；
        // 同指纹=同一报文合法重投递，放行给业务幂等层精确重放——首次业务失败/响应丢失后的重试不再被 401 卡死。
        if (nonceStore != null
                && nonceStore.registerFresh(nonce, bodyDigest(request), clock.instant(), TOLERANCE_SECONDS)
                        == WecomWebhookNonceStore.Registration.REPLAY) {
            throw ApiException.unauthorized("WECOM_WEBHOOK_NONCE_REPLAY", "wecom webhook nonce was already used");
        }
    }

    /**
     * 规范串：不可篡改字段按字段名字母序以「key=编码值」换行拼接（首项 body_digest 覆盖报文体全字段摘要）。
     * AUD-017：所有取自请求的值经 {@link #lengthPrefixed} 长度前缀编码——值内嵌换行/「  .content=」等
     * 结构样式文本无法移动字段边界，规范串解析唯一，杜绝跨字段碰撞。
     */
    String canonical(WecomConversationSyncRequest request, String nonce, long timestamp) {
        return "body_digest=" + bodyDigest(request)
                + "\nexternal_conversation_id=" + lengthPrefixed(request == null ? null : request.externalConversationId())
                + "\nidempotency_key=" + lengthPrefixed(request == null ? null : request.idempotencyKey())
                + "\nnonce=" + lengthPrefixed(nonce)
                + "\nproject_id=" + lengthPrefixed(request == null ? null : request.projectId())
                + "\ntimestamp=" + timestamp;
    }

    /**
     * 报文体全字段规范化摘要（SHA-256 十六进制）：ownerStaffId/customerRef + messages 逐条的
     * externalMessageId/senderRef/content/sentAt 与消息条数全部纳入。任一字段被篡改/增删都改变摘要 → 验签失败。
     * AUD-017：每个值长度前缀编码——此前「key=value」裸拼接时，senderRef='Alice\n  .content=私收款' 与
     * content 前移的篡改报文会产生完全相同的摘要（字段边界被值内容伪造），持有在途合法报文者无需密钥即可
     * 改变字段归属并绕过只扫 content 的风险词识别。
     */
    String bodyDigest(WecomConversationSyncRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request != null) {
            sb.append("owner_staff_id=").append(lengthPrefixed(request.ownerStaffId())).append('\n');
            sb.append("customer_ref=").append(lengthPrefixed(request.customerRef())).append('\n');
            List<WecomConversationSyncRequest.Message> messages =
                    request.messages() == null ? List.of() : request.messages();
            sb.append("message_count=").append(messages.size()).append('\n');
            int index = 0;
            for (WecomConversationSyncRequest.Message message : messages) {
                if (message == null) {
                    // AUD 复核 P3：null 消息元素写稳定 marker（不静默跳过），对含 null 的报文体摘要仍确定可验；
                    // 与 WecomConversationService 跳过 null message 的处理一致，避免 NPE→500。
                    sb.append("message[").append(index++).append("]=null\n");
                    continue;
                }
                sb.append("message[").append(index++).append("].external_message_id=")
                        .append(lengthPrefixed(message.externalMessageId())).append('\n');
                sb.append("  .sender_ref=").append(lengthPrefixed(message.senderRef())).append('\n');
                sb.append("  .content=").append(lengthPrefixed(message.content())).append('\n');
                sb.append("  .sent_at=").append(message.sentAt() == null ? "" : message.sentAt().toString()).append('\n');
            }
        }
        return HexFormat.of().formatHex(sha256(sb.toString()));
    }

    /**
     * AUD-017 长度前缀编码：{@code <UTF-8字节数>:<原值>}（null 视同空串=「0:」）。
     * 解析从长度读起、按字节数取值，值内容（含换行/等号/字段名样式文本）不可能被解释为字段边界。
     */
    private static String lengthPrefixed(String value) {
        String actual = value == null ? "" : value;
        return actual.getBytes(StandardCharsets.UTF_8).length + ":" + actual;
    }

    /** AUD-017：标识字段（会话/项目/幂等键/人员引用/消息id/发送者）不得含控制字符；content 为自由文本不限。 */
    private static void validateIdentifierFields(WecomConversationSyncRequest request, String nonce) {
        requireNoControlChars(nonce, "nonce");
        if (request == null) {
            return;
        }
        requireNoControlChars(request.externalConversationId(), "external_conversation_id");
        requireNoControlChars(request.projectId(), "project_id");
        requireNoControlChars(request.idempotencyKey(), "idempotency_key");
        requireNoControlChars(request.ownerStaffId(), "owner_staff_id");
        requireNoControlChars(request.customerRef(), "customer_ref");
        List<WecomConversationSyncRequest.Message> messages =
                request.messages() == null ? List.of() : request.messages();
        for (WecomConversationSyncRequest.Message message : messages) {
            if (message == null) {
                continue;
            }
            requireNoControlChars(message.externalMessageId(), "external_message_id");
            requireNoControlChars(message.senderRef(), "sender_ref");
        }
    }

    private static void requireNoControlChars(String value, String field) {
        if (value == null) {
            return;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        field + " must not contain control characters");
            }
        }
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

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
