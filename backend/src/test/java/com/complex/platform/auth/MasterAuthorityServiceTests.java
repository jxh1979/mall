// 文件版本：V0.01
package com.complex.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.support.InMemoryAuditEventRepository;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * U 盘破窗登录 Ed25519 验签：正常/重放/过期/错签/未配置 全 fail-closed；按来源限速（M008-A1-05）；prod 缺公钥拒启动（M008-A1-02）；
 * 公钥指纹留痕（M008-A1-04）。纯单测（stub SessionIssuer，MockEnvironment，无 Spring 容器/DB）。
 */
class MasterAuthorityServiceTests {

    private final List<Object> auditEvents = new ArrayList<>();
    private final AuditEventService audit = new AuditEventService(event -> {
        auditEvents.add(event);
        return event;
    });
    private final AuthService.LoginResponse stubResponse = new AuthService.LoginResponse(
            "acc", "ref", "sess-1", "Bearer",
            new AuthService.ApiUser("usr_master_authority", "master-authority", SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null));
    private final SessionIssuer issuer = deviceId -> stubResponse;
    private final KeyPair keyPair = generateKeyPair();

    private static final class RecordingIssuer implements SessionIssuer {
        private final AuthService.LoginResponse response;
        private int revoked;

        private RecordingIssuer(AuthService.LoginResponse response) {
            this.response = response;
        }

        @Override
        public AuthService.LoginResponse issueMasterSession(String deviceId) {
            return response;
        }

        @Override
        public void revokeOutstandingMasterSessions() {
            revoked++;
        }
    }

    private MasterAuthorityService service(Duration ttl, String pubKeyBase64) {
        return new MasterAuthorityService(issuer, audit, new MockEnvironment(), pubKeyBase64, ttl);
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

    @Test
    void actionProofIsBoundToActorPreviewAndDigestAndCannotBeReplayed() throws Exception {
        MasterAuthorityService svc = service(Duration.ofMinutes(2), pub());
        String digest = "a".repeat(64);
        var challenge = svc.issueActionChallenge(SessionIssuer.MASTER_USER_ID, "wecom-cleanup:one", digest);
        String signature = sign(keyPair, challenge.challenge());
        svc.verifyAction(SessionIssuer.MASTER_USER_ID, "wecom-cleanup:one", digest, challenge.challenge(), signature);
        assertThatThrownBy(() -> svc.verifyAction(SessionIssuer.MASTER_USER_ID, "wecom-cleanup:one", digest,
                challenge.challenge(), signature)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("MASTER_ACTION_CHALLENGE_INVALID"));
    }

    @Test
    void loginProofAndDifferentPreviewCannotAuthorizeCleanup() throws Exception {
        MasterAuthorityService svc = service(Duration.ofMinutes(2), pub());
        String login = svc.issueChallenge("login-only");
        String loginSignature = sign(keyPair, login);
        assertThatThrownBy(() -> svc.verifyAction(SessionIssuer.MASTER_USER_ID, "wecom-cleanup:one",
                "a".repeat(64), login, loginSignature)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("MASTER_ACTION_CHALLENGE_INVALID"));
        var action = svc.issueActionChallenge(SessionIssuer.MASTER_USER_ID, "wecom-cleanup:one", "a".repeat(64));
        String signature = sign(keyPair, action.challenge());
        assertThatThrownBy(() -> svc.verifyAction(SessionIssuer.MASTER_USER_ID, "wecom-cleanup:other",
                "a".repeat(64), action.challenge(), signature)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("MASTER_ACTION_CHALLENGE_INVALID"));
    }

    @Test
    void actionRejectsChangedDigestWrongKeyExpiredAndOrdinaryActor() throws Exception {
        for (String failure : List.of("digest", "signature", "expiry", "actor")) {
            MasterAuthorityService svc = service("expiry".equals(failure) ? Duration.ofMillis(1) : Duration.ofMinutes(2), pub());
            var challenge = svc.issueActionChallenge(SessionIssuer.MASTER_USER_ID, "wecom-cleanup:one", "a".repeat(64));
            String signature = sign("signature".equals(failure) ? generateKeyPair() : keyPair, challenge.challenge());
            if ("expiry".equals(failure)) Thread.sleep(15);
            assertThatThrownBy(() -> svc.verifyAction("actor".equals(failure) ? "ordinary-admin" : SessionIssuer.MASTER_USER_ID,
                    "wecom-cleanup:one", ("digest".equals(failure) ? "b" : "a").repeat(64),
                    challenge.challenge(), signature)).isInstanceOf(ApiException.class);
        }
    }

    // 4.9 / M008-A1-05：同一来源 60 秒窗口内超过 5 次尝试(challenge+login 合计)一律 429 拒并留审计。
    @Test
    void rateLimitsMasterAuthAttemptsWithinWindowPerSource() {
        MasterAuthorityService service = service(Duration.ofMinutes(5), pub());
        for (int i = 0; i < 5; i++) {
            service.issueChallenge("10.0.0.9"); // 前 5 次放行
        }
        assertThatThrownBy(() -> service.issueChallenge("10.0.0.9"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_AUTH_RATE_LIMITED"));
        // login 与 challenge 共享同一来源窗口(离线签名暴力尝试两个入口都要压)。
        assertThatThrownBy(() -> service.login("any", "sig", "10.0.0.9"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_AUTH_RATE_LIMITED"));
    }

    // M008-A1-05（PGM-1.15.1 破窗应急通道的可用性不由匿名方掌握）：匿名来源只耗尽自己的窗口，持钥人从另一来源仍可挑战并登录；
    // 成功登录即重置该来源窗口。
    @Test
    void anonymousFloodDoesNotStarveTheKeyHolderFromAnotherSource() throws Exception {
        MasterAuthorityService service = service(Duration.ofMinutes(5), pub());
        for (int i = 0; i < 5; i++) {
            service.issueChallenge("attacker");
        }
        assertThatThrownBy(() -> service.issueChallenge("attacker")).isInstanceOf(ApiException.class);
        String challenge = service.issueChallenge("keyholder");
        AuthService.LoginResponse response = service.login(challenge, sign(keyPair, challenge), "keyholder");
        assertThat(response.sessionId()).isEqualTo("sess-1");
        for (int i = 0; i < 5; i++) {
            service.issueChallenge("keyholder"); // 成功后窗口已重置，再 5 次不受限
        }
    }

    @Test
    void validChallengeAndSignatureIssuesMasterSession() throws Exception {
        MasterAuthorityService svc = service(Duration.ofMinutes(2), pub());
        String challenge = svc.issueChallenge("t");
        AuthService.LoginResponse resp = svc.login(challenge, sign(keyPair, challenge), "t");
        assertThat(resp.sessionId()).isEqualTo("sess-1");
        assertThat(resp.user().accountType()).isEqualTo(SessionIssuer.MASTER_ACCOUNT_TYPE);
    }

    @Test
    void replayedChallengeRejected() throws Exception {
        MasterAuthorityService svc = service(Duration.ofMinutes(2), pub());
        String challenge = svc.issueChallenge("t");
        String sig = sign(keyPair, challenge);
        svc.login(challenge, sig, "t");
        assertThatThrownBy(() -> svc.login(challenge, sig, "t"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_CHALLENGE_INVALID"));
    }

    @Test
    void expiredChallengeRejected() throws Exception {
        MasterAuthorityService svc = service(Duration.ofMillis(1), pub());
        String challenge = svc.issueChallenge("t");
        String sig = sign(keyPair, challenge);
        Thread.sleep(15);
        assertThatThrownBy(() -> svc.login(challenge, sig, "t"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_CHALLENGE_INVALID"));
    }

    @Test
    void wrongSignatureRejected() throws Exception {
        MasterAuthorityService svc = service(Duration.ofMinutes(2), pub());
        String challenge = svc.issueChallenge("t");
        String badSig = sign(generateKeyPair(), challenge); // 用别的私钥签
        assertThatThrownBy(() -> svc.login(challenge, badSig, "t"))
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
        assertThat(svc.publicKeyFingerprint()).isNull();
        assertThatThrownBy(() -> svc.issueChallenge("t"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_DISABLED"));
    }

    // M008-A1-02（BSM-1.26.1 U 盘托管为上线必做；PGM-1.15.1 破窗为管理员全失锁时唯一恢复通道）：
    // prod 启动缺验签公钥即拒绝启动，不因库凭证开关而豁免。
    @Test
    void productionRefusesToStartWithoutPublicKey() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        assertThatThrownBy(() -> new MasterAuthorityService(issuer, audit, prod, "", Duration.ofMinutes(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLEX_MASTER_AUTHORITY_PUBKEY");
        assertThat(new MasterAuthorityService(issuer, audit, prod, pub(), Duration.ofMinutes(2)).enabled()).isTrue();
        MockEnvironment prodIntegrationTest = new MockEnvironment();
        prodIntegrationTest.setActiveProfiles("prod");
        prodIntegrationTest.setProperty("complex.database.credential-separation.enforced", "false");
        assertThatThrownBy(() -> new MasterAuthorityService(issuer, audit, prodIntegrationTest, "", Duration.ofMinutes(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLEX_MASTER_AUTHORITY_PUBKEY");
    }

    // M008-A1-04 / F-02（PGM-1.15.1「更换的每一步都留痕」；BSM-1.26.1「启用 U 盘托管…都要记录」）：
    // 启动即记录当前验签公钥指纹（审计事件）；换钥后指纹不同；未配置无指纹。
    @Test
    void configuredPublicKeyFingerprintIsAuditedAtStartup() {
        MasterAuthorityService service = service(Duration.ofMinutes(2), pub());
        assertThat(service.publicKeyFingerprint()).hasSize(16)
                .isEqualTo(MasterAuthorityService.fingerprint(keyPair.getPublic()));
        assertThat(auditEvents).isNotEmpty();
        String other = Base64.getEncoder().encodeToString(generateKeyPair().getPublic().getEncoded());
        assertThat(service(Duration.ofMinutes(2), other).publicKeyFingerprint()).isNotEqualTo(service.publicKeyFingerprint());
    }

    @Test
    void loadingPublicKeyRevokesOutstandingMasterSessions() {
        RecordingIssuer withKey = new RecordingIssuer(stubResponse);
        new MasterAuthorityService(withKey, audit, new MockEnvironment(), pub(), Duration.ofMinutes(2));
        assertThat(withKey.revoked).isEqualTo(1);
        RecordingIssuer withoutKey = new RecordingIssuer(stubResponse);
        new MasterAuthorityService(withoutKey, audit, new MockEnvironment(), "", Duration.ofMinutes(2));
        assertThat(withoutKey.revoked).isEqualTo(0);
    }

    @Test
    void loadingPublicKeyInvalidatesLiveMasterAccessTokens() {
        AuthService auth = new AuthService(
                "p0_admin",
                "secret",
                new AuditEventService(new InMemoryAuditEventRepository()));
        AuthService.LoginResponse master = auth.issueMasterSession("usb-master:test");
        assertThat(auth.authenticateToken(master.accessToken())).isPresent();
        new MasterAuthorityService(auth, audit, new MockEnvironment(), pub(), Duration.ofMinutes(2));
        assertThat(auth.authenticateToken(master.accessToken())).isEmpty();
    }

    private static KeyPair generateKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
