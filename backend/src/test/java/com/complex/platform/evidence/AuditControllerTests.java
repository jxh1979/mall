package com.complex.platform.evidence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditControllerTests {

    private final AuditEventService service = mock(AuditEventService.class);
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private final AuditController controller = new AuditController(service, authz);

    @Test
    void queryRequiresViewPermissionAndPassesFilters() {
        when(service.searchOperationEvents(any(), any(), any(), any(), any(), any(), anyInt(), any())).thenReturn(List.of());
        controller.queryOperationEvents("ticket_order", "o_x", "alice", "ticket_refund.approved",
                "2026-06-14T00:00:00Z", null, 50);
        verify(authz).requirePermission("audit.operation_log.view");
        verify(service).searchOperationEvents("ticket_order", "o_x", "alice", "ticket_refund.approved",
                Instant.parse("2026-06-14T00:00:00Z"), null, 50, null);
    }

    @Test
    void exportRequiresExportPermission() {
        when(service.searchOperationEvents(any(), any(), any(), any(), any(), any(), anyInt(), any())).thenReturn(List.of());
        controller.exportOperationEvents(null, null, null, null, null, null, 1000);
        verify(authz).requirePermission("audit.operation_log.export");
    }

    @Test
    void queryPropagatesPermissionDenied() {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "denied"))
                .when(authz).requirePermission("audit.operation_log.view");
        assertThatThrownBy(() -> controller.queryOperationEvents(null, null, null, null, null, null, 10))
                .isInstanceOf(ApiException.class);
    }
}
