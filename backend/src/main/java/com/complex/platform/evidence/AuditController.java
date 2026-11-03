package com.complex.platform.evidence;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.security.ExportFileService;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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
public class AuditController implements ExportFileService.Source {

    private final AuditEventService service;
    private final AuthorizationService authorizationService;
    private final ExportFileService exportFiles;

    public AuditController(AuditEventService service, AuthorizationService authorizationService, ExportFileService exportFiles) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.exportFiles = exportFiles;
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
    @com.complex.platform.access.ConsumesSingleUseGrant("audit.operation_log.export") // M723：导出导一次扣一次（操作员 2026-10-03）
    public ResponseEntity<byte[]> exportOperationEvents(
            @RequestParam(name = "business_type", required = false) String businessType,
            @RequestParam(name = "business_id", required = false) String businessId,
            @RequestParam(name = "actor_id", required = false) String actorId,
            @RequestParam(name = "event_name", required = false) String eventName,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "limit", defaultValue = "1000") int limit) {
        // 第十八批 N 段第 124 条 124-R6（PGM:683「审计人员查看或导出敏感信息（……完整操作记录导出）需先经审批并全程留痕。」、PGM:685
        // 「导出须单独授权、逐次留痕」）：导出成功落 audit.operation_log.exported（business_id 导出文件编号），权限或取数失败落
        // audit.operation_log.export_failed；写法同 FinanceService.exportArApItems。
        try {
            authorizationService.requirePermission("audit.operation_log.export");
            // 第 23 条 23-R3（PGM:687）：只交出服务端生成的带密码、带暗纹的 PDF。
            ExportFileService.Table table = operationEventsTable(businessType, businessId, actorId, eventName, from, to, limit);
            ExportFileService.ExportFile file = exportFiles.create("audit_operation_events", table.title(), table.filters(), table.columns(),
                    table.rows());
            service.recordSuccess("audit.operation_log.exported", "audit.operation_log.export", "export_file", file.id(), null, null,
                    Map.of("row_count", table.rows().size()));
            return file.response();
        } catch (RuntimeException error) {
            service.recordFailure("audit.operation_log.export_failed", "audit.operation_log.export", "export_file", null, null, null,
                    error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
            throw error;
        }
    }

    @Override
    public String exportType() {
        return "audit_operation_events";
    }

    /** 第 23 条 23-R4 可编辑版重取：同查看操作留痕（audit.operation_log.view，商户范围按当前账号），不再要导出权限。 */
    @Override
    public ExportFileService.Table viewTable(Map<String, Object> filters) {
        authorizationService.requirePermission("audit.operation_log.view");
        return operationEventsTable(text(filters, "business_type"), text(filters, "business_id"), text(filters, "actor_id"),
                text(filters, "event_name"), text(filters, "from"), text(filters, "to"), ((Number) filters.get("limit")).intValue());
    }

    private static String text(Map<String, Object> filters, String key) {
        Object value = filters.get(key);
        return value == null ? null : value.toString();
    }

    /** 按筛选取操作留痕（商户范围按当前账号）；导出记录 filters 记筛选参数与取数所按的商户范围。 */
    private ExportFileService.Table operationEventsTable(String businessType, String businessId, String actorId, String eventName,
            String from, String to, int limit) {
        String merchant = authorizationService.currentMerchantScope().orElse(null);
        List<AuditEvent> events = service.searchOperationEvents(
                businessType, businessId, actorId, eventName, parseInstant(from), parseInstant(to), limit, merchant);
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("business_type", businessType);
        filters.put("business_id", businessId);
        filters.put("actor_id", actorId);
        filters.put("event_name", eventName);
        filters.put("from", from);
        filters.put("to", to);
        filters.put("limit", limit);
        filters.put("merchant_id", merchant);
        return new ExportFileService.Table("操作留痕", filters, EXPORT_COLUMNS, events.stream().map(AuditController::exportRow).toList());
    }

    private static final List<String> EXPORT_COLUMNS = List.of("发生时间", "事件名称", "结果", "操作人类型", "操作人", "终端", "权限码",
            "业务类型", "业务编号", "请求编号", "商户编号");
    private static final DateTimeFormatter EXPORT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(LocalZone.ID);

    private static List<String> exportRow(AuditEvent event) {
        return Arrays.asList(event.occurredAt() == null ? null : EXPORT_TIME.format(event.occurredAt()), event.eventName(),
                event.eventResult(), event.actorType(), event.actorId(), event.terminal(), event.permissionCode(), event.businessType(),
                event.businessId(), event.requestId(), event.merchantId());
    }

    private static Instant parseInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}
