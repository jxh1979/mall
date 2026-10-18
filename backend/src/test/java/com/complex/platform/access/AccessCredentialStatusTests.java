package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

class AccessCredentialStatusTests {
    @Test
    void developmentStatusChangesPreserveTheOriginalPasswordAndIdentity() {
        AuthService auth = new AuthService("", "");
        auth.provisionUser("dealer-user", "dealer-login", "Portal#2026", "dealer", "project_main", null, "disabled");
        var request = new AuthService.LoginRequest("dealer-login", "Portal#2026", null, null, "dealer_portal");
        var adapter = new DevAccessCredentialProvisioner(auth, mock(AccessRepository.class));
        assertThatThrownBy(() -> auth.login(request)).isInstanceOf(ApiException.class);
        adapter.changeStatus("dealer-user", "active", Instant.now());
        var first = auth.login(request);
        var before = auth.authenticateToken(first.accessToken()).orElseThrow();
        adapter.changeStatus("dealer-user", "disabled", Instant.now());
        assertThatThrownBy(() -> auth.login(request)).isInstanceOf(ApiException.class);
        adapter.changeStatus("dealer-user", "active", Instant.now());
        var after = auth.authenticateToken(auth.login(request).accessToken()).orElseThrow();
        assertThat(after.passwordHash()).isEqualTo(before.passwordHash());
        assertThat(after.accountType()).isEqualTo("dealer");
        assertThat(after.organizationId()).isEqualTo("project_main");
        assertThat(after.createdAt()).isEqualTo(before.createdAt());
    }

    @Test
    void databaseStatusChangeDoesNotResetPasswordOrLockoutFields() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);
        new JdbcAccessCredentialProvisioner(jdbc).changeStatus("dealer-user", "active", Instant.now());
        var sql = ArgumentCaptor.forClass(String.class);
        var params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).update(sql.capture(), params.capture());
        assertThat(sql.getValue()).doesNotContain("password_hash", "failed_attempt_count", "locked_until", "password_changed_at");
        assertThat(params.getValue().getValue("status")).isEqualTo("active");
        assertThat(params.getValue().getValue("id")).isEqualTo("dealer-user");
    }

    @Test
    void missingCredentialCannotPretendToBeActivated() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(0);
        assertThatThrownBy(() -> new JdbcAccessCredentialProvisioner(jdbc).changeStatus("missing", "active", Instant.now()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CREDENTIAL_STATUS_CHANGE_FAILED"));
        assertThatThrownBy(() -> new AuthService("", "").changeUserStatus("missing", "active")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AccessCredentialProvisioner.noop().changeStatus("missing", "active", Instant.now()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
