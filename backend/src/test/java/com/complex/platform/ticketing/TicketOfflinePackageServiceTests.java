package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TicketOfflinePackageServiceTests {

    @Test
    void p0DefaultDisablesOfflineEntitlementPackage() {
        TicketingRepository repository = mock(TicketingRepository.class);
        when(repository.listOfflineEntitlements("merchant_request", "venue_1", 20)).thenReturn(List.of());
        TicketOfflinePackageService service = new TicketOfflinePackageService(
                repository, new AuditEventService(event -> event), null, false);

        assertThatThrownBy(() -> service.getOfflineEntitlements("merchant_request", "venue_1", 20))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("OFFLINE_ENTITLEMENTS_DISABLED");
                });

        verify(repository, never()).listOfflineEntitlements("merchant_request", "venue_1", 20);
    }

    @Test
    void enabledPackageStillUsesAuthenticatedMerchantScope() {
        TicketingRepository repository = mock(TicketingRepository.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_token"));
        OfflineEntitlementResponse entitlement = new OfflineEntitlementResponse(
                "entitlement_1",
                "P0-ENT-1",
                "ticket",
                "merchant_token",
                "venue_1",
                "point_1",
                1,
                0,
                9900L,
                "active",
                "2026-06-30T00:00:00Z",
                "2026-07-01T00:00:00Z",
                "2026-06-30T00:00:00Z"
        );
        when(repository.listOfflineEntitlements("merchant_token", "venue_1", 200)).thenReturn(List.of(entitlement));
        TicketOfflinePackageService service = new TicketOfflinePackageService(
                repository, new AuditEventService(event -> event), authorizationService, true);

        OfflinePackageResponse response = service.getOfflineEntitlements("merchant_request", "venue_1", 0);

        assertThat(response.entitlements()).containsExactly(entitlement);
        verify(repository).listOfflineEntitlements("merchant_token", "venue_1", 200);
    }
}
