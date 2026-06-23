package com.complex.platform.bar;

import com.complex.platform.common.crypto.SigningSecretGuard;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * V155 订卡邀请函二维码 HMAC 签名器。
 *
 * <p>与门票 {@code ticketing/TicketCredentialSigner} 同范式（HmacSHA256 + {@link MessageDigest#isEqual} 常量时间比较），
 * 聚焦卡座邀请函、零跨包依赖——不复用 ticketing 包的 package-private 签名器（其 canonical 字段是门票凭证专用）。
 * 邀请函令牌为不可猜随机串、按 token 反查预订；签名进一步绑定「预订/酒吧/卡座/容量」关键字段，使转发出去的二维码
 * 被篡改容量或冒用其它卡座时签名失配，离线伪造不可行。密钥经 {@code bar.invitation.signing.secret} 注入，
 * 生产由 env {@code COMPLEX_BAR_INVITATION_SIGNING_SECRET} 覆盖。
 */
@Component
public class BarInvitationTokenSigner {

    static final String DEV_SIGNING_SECRET = "complex-dev-bar-invitation-secret-change-me";

    private final String secret;

    @Autowired
    public BarInvitationTokenSigner(
            @Value("${bar.invitation.signing.secret:complex-dev-bar-invitation-secret-change-me}") String secret,
            Environment environment) {
        this(secret, environment.acceptsProfiles(Profiles.of("prod")));
    }

    BarInvitationTokenSigner(String secret) {
        this(secret, false);
    }

    BarInvitationTokenSigner(String secret, boolean productionProfile) {
        this.secret = SigningSecretGuard.requireUsableHmacSecret(
                "bar.invitation.signing.secret", "COMPLEX_BAR_INVITATION_SIGNING_SECRET",
                secret, DEV_SIGNING_SECRET, productionProfile);
    }

    /**
     * 防伪签名规范串：覆盖邀请函不可篡改的关键字段，按字段名字母序以「key=value」换行拼接
     * （与 TicketCredentialSigner / OpenPlatformCrypto canonical 范式一致）。签发与核验必须传入完全相同字段值。
     */
    String canonical(String reservationId, String barId, String tableId, int capacity, String token) {
        return "bar_id=" + barId
                + "\ncapacity=" + capacity
                + "\nreservation_id=" + reservationId
                + "\ntable_id=" + tableId
                + "\ntoken=" + token;
    }

    /** 对规范串计算 HMAC-SHA256，返回十六进制串（写入 invitation_signature，VARCHAR(256) 容纳 64 hex 字符）。 */
    String sign(String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("bar invitation HMAC-SHA256 is unavailable", exception);
        }
    }

    /** 常量时间比较校验签名，避免计时侧信道；空签名一律返回 false。 */
    boolean matches(String canonical, String expectedSignature) {
        if (expectedSignature == null || expectedSignature.isBlank()) {
            return false;
        }
        String actual = sign(canonical);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8));
    }
}
