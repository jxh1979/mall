package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;

@Service
public class TicketOfflinePackageService {

    private static final int DEFAULT_OFFLINE_LIMIT = 200;

    private final TicketingRepository repository;
    private final AuditEventService auditEventService;
    // P1 越权修复：离线权益包此前 scope 取自请求参数（merchant_id 传 null 即返回全平台有效权益）。
    // 改为商户用户强制使用自身 scope；平台管理员（scope 为空）保留请求过滤。可为 null=兼容测试构造。
    private final AuthorizationService authorizationService;
    private final boolean offlineEntitlementsEnabled;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketOfflinePackageService(
            TicketingRepository repository,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            @Value("${complex.ticketing.offline-entitlements.enabled:false}") boolean offlineEntitlementsEnabled) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.offlineEntitlementsEnabled = offlineEntitlementsEnabled;
    }

    public TicketOfflinePackageService(
            TicketingRepository repository,
            AuditEventService auditEventService,
            AuthorizationService authorizationService) {
        this(repository, auditEventService, authorizationService, false);
    }

    public TicketOfflinePackageService(TicketingRepository repository, AuditEventService auditEventService) {
        this(repository, auditEventService, null, false);
    }

    OfflinePackageResponse getOfflineEntitlements(String merchantId, String venueId, int limit) {
        try {
            if (!offlineEntitlementsEnabled) {
                throw ApiException.forbidden("OFFLINE_ENTITLEMENTS_DISABLED",
                        "offline entitlement package is disabled for P0 online verification");
            }
            // P1 越权修复：商户用户强制按自身商户范围取数，忽略请求中的 merchant_id；平台管理员沿用请求过滤。
            String effectiveMerchantId = authorizationService == null
                    ? trimToNull(merchantId)
                    : authorizationService.currentMerchantScope().orElse(trimToNull(merchantId));
            List<OfflineEntitlementResponse> entitlements = repository.listOfflineEntitlements(
                    effectiveMerchantId, trimToNull(venueId), limit <= 0 ? DEFAULT_OFFLINE_LIMIT : limit
            );
            auditEventService.recordSuccess("terminal.offline_entitlements.viewed", "terminal.device.bind", "offline_entitlement", "all", null, null, Map.of("count", entitlements.size()));
            Instant now = Instant.now();
            return new OfflinePackageResponse(now.toString(), now.plusSeconds(3600).toString(), entitlements);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("terminal.offline_entitlements.viewed_failed", "terminal.device.bind", "offline_entitlement", "all", null, null, codeOf(exception));
            throw exception;
        }
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
