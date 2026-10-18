package com.complex.platform.access.staff;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessCredentialProvisioner;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.dto.StaffPayrollCloseRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class PayrollTaxPayerConflictTests {
    private final JdbcStaffRepository repository = mock(JdbcStaffRepository.class);
    private final AuditEventService audit = mock(AuditEventService.class);
    private final StaffPayrollCloseRequest request = new StaffPayrollCloseRequest("close-tax", "2099-07");

    private StaffService service(RuntimeException failure) {
        AuthorizationService auth = mock(AuthorizationService.class);
        AccessActor actor = new AccessActor("operator", "operator", "platform_admin", null, null, "active");
        when(auth.currentActor()).thenReturn(actor);
        when(auth.isPlatformAdmin(actor)).thenReturn(true);
        when(auth.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        StaffService service = new StaffService(repository, mock(AccessRepository.class),
                mock(IdempotencyService.class), audit, auth, AccessCredentialProvisioner.noop());
        ReflectionTestUtils.setField(service, "payrollTaxes", mock(PayrollTaxService.class));
        when(repository.closePayrollMonth(eq("2099-07"), eq("operator"), any(Instant.class),
                isNull(), isNull(), isNull())).thenThrow(failure);
        return service;
    }

    @Test
    void knownPayerConstraintBecomesConflictAndKeepsFailureAudit() {
        var original = new DataIntegrityViolationException("insert payroll",
                new SQLException("ERROR: PAYROLL_TAX_PAYER_CHANGED: 发放主体已变化", "23514"));
        assertThatThrownBy(() -> service(original).closePayroll(request))
                .isInstanceOfSatisfying(ApiException.class, error -> {
                    assertThat(error.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(error.code()).isEqualTo("PAYROLL_TAX_PAYER_CHANGED");
                    assertThat(error.getCause()).isSameAs(original);
                });
        verifyFailure("PAYROLL_TAX_PAYER_CHANGED");
    }

    @Test
    void unrelatedDatabaseConstraintRemainsOriginal() {
        var original = new DataIntegrityViolationException("insert payroll",
                new SQLException("ERROR: another check constraint failed", "23514"));
        assertThatThrownBy(() -> service(original).closePayroll(request)).isSameAs(original);
        verifyFailure("DataIntegrityViolationException");
    }

    @Test
    void markerWithDifferentSqlStateDoesNotBecomePayerConflict() {
        var original = new DataIntegrityViolationException("insert payroll",
                new SQLException("ERROR: PAYROLL_TAX_PAYER_CHANGED: unexpected database failure", "23505"));
        assertThatThrownBy(() -> service(original).closePayroll(request)).isSameAs(original);
        verifyFailure("DataIntegrityViolationException");
    }

    @Test
    void nonDatabaseFailureWithMarkerRemainsOriginal() {
        var original = new IllegalStateException("PAYROLL_TAX_PAYER_CHANGED: unrelated failure");
        assertThatThrownBy(() -> service(original).closePayroll(request)).isSameAs(original);
        verifyFailure("IllegalStateException");
    }

    private void verifyFailure(String code) {
        verify(audit).recordFailure("staff.payroll.month_close_failed", "staff.payroll.manage",
                "staff_payroll_statement", "2099-07", null, "close-tax", code);
        verifyNoMoreInteractions(audit);
    }
}
