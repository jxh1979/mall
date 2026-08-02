package com.complex.platform.auth;

import com.complex.platform.support.InMemoryAuditEventRepository;
import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class BearerAuthenticationFilterTests {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bearerAuthenticationDoesNotExposeRawTokenAsCredentials() throws Exception {
        AuthService authService = new AuthService(
                "p0_admin",
                "secret",
                new AuditEventService(new InMemoryAuditEventRepository())
        );
        AuthService.LoginResponse login = authService.login(new AuthService.LoginRequest(
                "p0_admin",
                "secret",
                null,
                "staff-phone-1",
                "staff_work_phone"
        ));
        BearerAuthenticationFilter filter = new BearerAuthenticationFilter(authService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/access/permissions");
        request.addHeader("Authorization", "Bearer " + login.accessToken());

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOf(AuthService.UserRecord.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials()).isNull();
    }
}
