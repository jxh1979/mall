package com.complex.platform.evidence;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 条目4 操作留痕跨对象查询/导出（audit_events）。供审计只读角色按业务对象/操作人/事件名/时间范围跨对象搜索、导出。
 * 与 evidence/events（证据事件、按对象时间线已 UNION audit_events）互补：本控制器解决"跨对象批量"查询入口。
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditEventService service;
    private final AuthorizationService authorizationService;

    public AuditController(AuditEventService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/operation-events")
    public ApiResponse<List<AuditEvent>> queryOperationEvents(
            @RequestParam(name = "business_type", required = false) String businessType,
            @RequestParam(name = "business_id", required = false) String businessId,
            @RequestParam(name = "actor_id", required = false) String actorId,
            @RequestParam(name = "event_name", required = false) String eventName,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        authorizationService.requirePermission("audit.operation_log.view");
        return ApiResponse.ok(service.searchOperationEvents(
                businessType, businessId, actorId, eventName, parseInstant(from), parseInstant(to), limit,
                authorizationService.currentMerchantScope().orElse(null)));
    }

    @GetMapping("/operation-events/export")
    public ApiResponse<List<AuditEvent>> exportOperationEvents(
            @RequestParam(name = "business_type", required = false) String businessType,
            @RequestParam(name = "business_id", required = false) String businessId,
            @RequestParam(name = "actor_id", required = false) String actorId,
            @RequestParam(name = "event_name", required = false) String eventName,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "limit", defaultValue = "1000") int limit) {
        authorizationService.requirePermission("audit.operation_log.export");
        return ApiResponse.ok(service.searchOperationEvents(
                businessType, businessId, actorId, eventName, parseInstant(from), parseInstant(to), limit,
                authorizationService.currentMerchantScope().orElse(null)));
    }

    private static Instant parseInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}
