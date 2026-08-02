package com.complex.platform.auth;

import com.complex.platform.support.InMemoryAuditEventRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTests {

    private InMemoryAuditEventRepository auditEventRepository;
    private AuthService service;

    @BeforeEach
    void setUp() {
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new AuthService(
                "p0_admin",
                "secret",
                new AuditEventService(auditEventRepository)
        );
    }

    @Test
    void provisionUserRegistersLoginWithDbAlignedIdentity() {
        // 商户专区一期:dev/local 下 access_users 建的账号必须可登录,且内存身份与库 id/merchant 对齐,
        // 否则商户账号无法在 dev 后台验收(权限按库 id 装配、merchant scope 按 actor.merchantId 判)。
        service.provisionUser("usr_mz1", "mz_merchant", "Pw-1!", "staff", "org-1", "mch-1", "active");

        AuthService.LoginResponse response = service.login(new AuthService.LoginRequest(
                "mz_merchant", "Pw-1!", null, null, "merchant_portal"));
        assertThat(response.user().id()).isEqualTo("usr_mz1");
        assertThat(response.user().merchantId()).isEqualTo("mch-1");
        assertThat(response.user().accountType()).isEqualTo("staff");

        // 幂等覆盖:重复发放(改密)不抛并以新密码为准
        service.provisionUser("usr_mz1", "mz_merchant", "Pw-2!", "staff", "org-1", "mch-1", "active");
        assertThatThrownBy(() -> service.login(new AuthService.LoginRequest("mz_merchant", "Pw-1!", null, null, null)))
                .isInstanceOf(ApiException.class);
        assertThat(service.login(new AuthService.LoginRequest("mz_merchant", "Pw-2!", null, null, null))
                .user().id()).isEqualTo("usr_mz1");
    }

    @Test
    void loginUsesLoginNameAndReturnsSessionRefreshTokenAndAuditEvent() {
        AuthService.LoginResponse response = service.login(new AuthService.LoginRequest(
                "p0_admin",
                "secret",
                null,
                "staff-phone-1",
                "staff_work_phone"
        ));

        assertThat(response.accessToken()).startsWith("p0_access_");
        assertThat(response.refreshToken()).startsWith("p0_refresh_");
        assertThat(response.sessionId()).startsWith("sess_");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo("p0_admin");
        assertThat(service.authenticateToken(response.accessToken())).isPresent();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("auth.login.succeeded");
    }

    @Test
    void loginMissingLoginNameFailsWith4xxCodeAndAuditEvent() {
        assertThatThrownBy(() -> service.login(new AuthService.LoginRequest(
                null,
                "secret",
                null,
                "staff-phone-1",
                "staff_work_phone"
        )))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().is4xxClientError()).isTrue();
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                });

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("auth.login.failed");
    }

    @Test
    void nullLoginRequestFailsWith4xxCodeInsteadOfNullPointerException() {
        assertThatThrownBy(() -> service.login(null))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().is4xxClientError()).isTrue();
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                });

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("auth.login.failed");
    }

    @Test
    void refreshRotatesAccessAndRefreshTokensAndRejectsReusedRefreshTokenWithout500() {
        AuthService.LoginResponse login = login();

        AuthService.LoginResponse refresh = service.refresh(
                new AuthService.RefreshTokenRequest(login.refreshToken(), "staff-phone-1"),
                login.accessToken()
        );

        assertThat(refresh.sessionId()).isEqualTo(login.sessionId());
        assertThat(refresh.accessToken()).startsWith("p0_access_").isNotEqualTo(login.accessToken());
        assertThat(refresh.refreshToken()).startsWith("p0_refresh_").isNotEqualTo(login.refreshToken());
        assertThat(service.authenticateToken(login.accessToken())).isEmpty();
        assertThat(service.authenticateToken(refresh.accessToken())).isPresent();

        assertThatThrownBy(() -> service.refresh(
                new AuthService.RefreshTokenRequest(login.refreshToken(), "staff-phone-1"),
                refresh.accessToken()
        ))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().is4xxClientError()).isTrue();
                    assertThat(exception.code()).isEqualTo("REFRESH_TOKEN_INVALID");
                });

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "auth.login.succeeded",
                        "auth.token.refreshed",
                        "auth.token_refresh_failed"
                );
    }

    @Test
    void logoutRevokesAccessAndRefreshTokensAndInvalidSessionReturns4xxNot500() {
        AuthService.LoginResponse login = login();

        AuthService.LogoutResponse logout = service.logout(
                new AuthService.LogoutRequest(
                        "logout-once",
                        login.sessionId(),
                        "staff-phone-1",
                        "operator requested logout"
                ),
                login.accessToken()
        );

        assertThat(logout.sessionId()).isEqualTo(login.sessionId());
        assertThat(logout.status()).isEqualTo("logged_out");
        assertThat(service.authenticateToken(login.accessToken())).isEmpty();

        assertThatThrownBy(() -> service.refresh(
                new AuthService.RefreshTokenRequest(login.refreshToken(), "staff-phone-1"),
                login.accessToken()
        ))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().is4xxClientError()).isTrue();
                    assertThat(exception.code()).isEqualTo("REFRESH_TOKEN_INVALID");
                });

        AuthService.LoginResponse secondLogin = login();
        assertThatThrownBy(() -> service.logout(
                new AuthService.LogoutRequest("logout-wrong-session", "sess_missing", "staff-phone-1", null),
                secondLogin.accessToken()
        ))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().is4xxClientError()).isTrue();
                    assertThat(exception.code()).isEqualTo("SESSION_MISMATCH");
                });

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("auth.logout", "auth.logout_failed");
    }

    @Test
    void nullLogoutRequestFailsWith4xxCodeInsteadOfNullPointerException() {
        AuthService.LoginResponse login = login();

        assertThatThrownBy(() -> service.logout(null, login.accessToken()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().is4xxClientError()).isTrue();
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                });

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("auth.login.succeeded", "auth.logout_failed");
    }

    @Test
    void createUserStoresSaltedBcryptHashNotPlaintextOrSha256() {
        AuthService.UserRecord alice = service.createUser(new AuthService.CreateUserCommand(
                "alice", "secret", "staff", "org_1", "merchant_1", "active"));
        AuthService.UserRecord bob = service.createUser(new AuthService.CreateUserCommand(
                "bob", "secret", "staff", "org_1", "merchant_1", "active"));

        // bcrypt hash, not plaintext and not an unsalted SHA-256 hex digest
        assertThat(alice.passwordHash()).startsWith("$2");
        assertThat(alice.passwordHash()).isNotEqualTo("secret");
        assertThat(alice.passwordHash()).doesNotMatch("[0-9a-f]{64}");
        // salted: the same password produces a different hash per user
        assertThat(alice.passwordHash()).isNotEqualTo(bob.passwordHash());
        // login with the raw password still succeeds against the bcrypt hash
        assertThat(service.login(new AuthService.LoginRequest("alice", "secret", null, "d1", "web")).accessToken())
                .startsWith("p0_access_");
    }

    @Test
    void accessTokenIsRejectedAfterItsTtlElapses() {
        AuthService shortLived = new AuthService(
                "p0_admin", "secret", new AuditEventService(auditEventRepository), Duration.ofSeconds(-1));
        AuthService.LoginResponse login = shortLived.login(new AuthService.LoginRequest(
                "p0_admin", "secret", null, "d1", "web"));

        // The token was issued already-expired (negative TTL), so it must not authenticate.
        assertThat(shortLived.authenticateToken(login.accessToken())).isEmpty();
    }

    private AuthService.LoginResponse login() {
        return service.login(new AuthService.LoginRequest(
                "p0_admin",
                "secret",
                null,
                "staff-phone-1",
                "staff_work_phone"
        ));
    }
}
