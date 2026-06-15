package com.complex.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
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

    private AuthService.LoginResponse login() {
        return service.login(new AuthService.LoginRequest(
                "p0_admin",
                "secret",
                null,
                "staff-phone-1",
                "staff_work_phone"
        ));
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
