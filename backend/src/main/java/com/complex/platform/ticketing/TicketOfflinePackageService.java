package com.complex.platform.ticketing;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TicketOfflinePackageService {

    private static final int DEFAULT_OFFLINE_LIMIT = 200;

    private final TicketingRepository repository;
    private final AuditEventService auditEventService;

    public TicketOfflinePackageService(TicketingRepository repository, AuditEventService auditEventService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
    }

    OfflinePackageResponse getOfflineEntitlements(String merchantId, String venueId, int limit) {
        try {
            List<OfflineEntitlementResponse> entitlements = repository.listOfflineEntitlements(
                    trimToNull(merchantId), trimToNull(venueId), limit <= 0 ? DEFAULT_OFFLINE_LIMIT : limit
            );
            auditEventService.recordSuccess("terminal.offline_entitlements.viewed", "terminal.device.bind", "offline_entitlement", "all", null, null, Map.of("count", entitlements.size()));
            Instant now = Instant.now();
            return new OfflinePackageResponse(now.toString(), now.plusSeconds(3600).toString(), entitlements);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("terminal.offline_entitlements.viewed_failed", "terminal.device.bind", "offline_entitlement", "all", null, null, codeOf(exception));
            throw exception;
        }
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
