package com.complex.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** U 盘破窗登录 Ed25519 验签：正常/重放/过期/错签/未配置 全 fail-closed。纯单测（stub SessionIssuer，无 Spring/DB）。 */
class MasterAuthorityServiceTests {

    private final AuditEventService audit = new AuditEventService(event -> event);
    private final AuthService.LoginResponse stubResponse = new AuthService.LoginResponse(
            "acc", "ref", "sess-1", "Bearer",
            new AuthService.ApiUser("usr_master_authority", "master-authority", "platform_admin", "platform-company", null));
    private final SessionIssuer issuer = () -> stubResponse;
    private final KeyPair keyPair = generateKeyPair();

    private MasterAuthorityService service(Duration ttl, String pubKeyBase64) {
        return new MasterAuthorityService(issuer, audit, pubKeyBase64, ttl);
    }

    private String pub() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    private String sign(KeyPair kp, String challenge) throws Exception {
        Signature s = Signature.getInstance("Ed25519");
        s.initSign(kp.getPrivate());
        s.update(challenge.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(s.sign());
    }

    // 4.9 破窗登录限速：60 秒窗口内超过 5 次尝试(challenge+login 合计)一律 429 拒并留审计。
    @Test
    void rateLimitsMasterAuthAttemptsWithinWindow() {
        MasterAuthorityService service = service(java.time.Duration.ofMinutes(5), pub());
        for (int i = 0; i < 5; i++) {
            service.issueChallenge(); // 前 5 次放行
        }
        org.assertj.core.api.Assertions.assertThatThrownBy(service::issueChallenge)
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("MASTER_AUTH_RATE_LIMITED"));
        // login 与 challenge 共享同一窗口(离线签名暴力尝试两个入口都要压)。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.login("any", "sig"))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("MASTER_AUTH_RATE_LIMITED"));
    }

    @Test
    void validChallengeAndSignatureIssuesMasterSession() throws Exception {
        MasterAuthorityService svc = service(Duration.ofMinutes(2), pub());
        String challenge = svc.issueChallenge();
        AuthService.LoginResponse resp = svc.login(challenge, sign(keyPair, challenge));
        assertThat(resp.sessionId()).isEqualTo("sess-1");
        assertThat(resp.user().accountType()).isEqualTo("platform_admin");
    }

    @Test
    void replayedChallengeRejected() throws Exception {
        MasterAuthorityService svc = service(Duration.ofMinutes(2), pub());
        String challenge = svc.issueChallenge();
        String sig = sign(keyPair, challenge);
        svc.login(challenge, sig);
        assertThatThrownBy(() -> svc.login(challenge, sig))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_CHALLENGE_INVALID"));
    }

    @Test
    void expiredChallengeRejected() throws Exception {
        MasterAuthorityService svc = service(Duration.ofMillis(1), pub());
        String challenge = svc.issueChallenge();
        String sig = sign(keyPair, challenge);
        Thread.sleep(15);
        assertThatThrownBy(() -> svc.login(challenge, sig))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_CHALLENGE_INVALID"));
    }

    @Test
    void wrongSignatureRejected() throws Exception {
        MasterAuthorityService svc = service(Duration.ofMinutes(2), pub());
        String challenge = svc.issueChallenge();
        String badSig = sign(generateKeyPair(), challenge); // 用别的私钥签
        assertThatThrownBy(() -> svc.login(challenge, badSig))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_SIGNATURE_INVALID"));
    }

    @Test
    void verifiesOpensslGeneratedSignatureCrossStack() throws Exception {
        // openssl（generate-keystore/sign-challenge 同款）生成的 SPKI DER 公钥 + raw Ed25519 签名，
        // 必须被 Java 端验签通过——钉死 openssl→Java 跨栈格式兼容（破窗登录真实链路）。
        String pubB64 = "MCowBQYDK2VwAyEA8NqN+vgtYwBz+egN2tqblp+s9LmoP/Y2mmoS8ePaDxY=";
        String challenge = "cross-stack-check";
        String sigB64 = "TsVJAm0JfRLcCcKz9Kxfgq8LaLDIpkrEIvWGv1eg2QUnaOGLFA2qTQLdVn26uevaLzLgO1pxzHd4G8IFvKWXAw==";
        java.security.PublicKey key = MasterAuthorityService.parsePublicKey(pubB64);
        assertThat(key).isNotNull();
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(key);
        verifier.update(challenge.getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(Base64.getDecoder().decode(sigB64))).isTrue();
    }

    @Test
    void disabledWhenNoPublicKey() {
        MasterAuthorityService svc = service(Duration.ofMinutes(2), "");
        assertThat(svc.enabled()).isFalse();
        assertThatThrownBy(svc::issueChallenge)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_DISABLED"));
    }

    private static KeyPair generateKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
