package com.complex.platform.evidence;

import com.complex.platform.access.AccessRepository;
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
import java.util.TreeMap;
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
    private final AuditLabels labels;
    private final AccessRepository accessRepository;

    public AuditController(AuditEventService service, AuthorizationService authorizationService, ExportFileService exportFiles,
            AuditLabels labels, AccessRepository accessRepository) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.exportFiles = exportFiles;
        this.labels = labels;
        this.accessRepository = accessRepository;
    }

    /** 第十八批 AN 段第 227 条 227-R2：操作日志视图显示用的业务类型、操作人类型、事件名与权限中文名，每节「码、中文名」列表。 */
    public record AuditLabelsResponse(List<AuditLabels.AuditLabel> businessTypes, List<AuditLabels.AuditLabel> actorTypes,
            List<AuditLabels.AuditLabel> eventNames, List<AuditLabels.AuditLabel> permissions) {
    }

    /**
     * 第十八批 AN 段第 227 条 227-R2（PGM:287「……字段名、状态、枚举取值……必须全部为中文……」）：读标签字典，权限取权限表里启用码的中文名，
     * 再并入字典里权限表没有的权限状态与伪码（登录、无需员工权限等）。只读字典，不落留痕（不走 AccessService.listPermissions，那会落查看权限留痕）。
     */
    @GetMapping("/labels")
    public ApiResponse<AuditLabelsResponse> labels() {
        authorizationService.requirePermission("audit.operation_log.view");
        Map<String, String> permissions = new TreeMap<>();
        labels.list("extra_permissions").forEach(label -> permissions.put(label.code(), label.name()));
        accessRepository.listPermissions().forEach(permission -> permissions.put(permission.permissionCode(), permission.permissionName()));
        return ApiResponse.ok(new AuditLabelsResponse(labels.list("business_types"), labels.list("actor_types"),
                labels.list("event_names"),
                permissions.entrySet().stream().map(entry -> new AuditLabels.AuditLabel(entry.getKey(), entry.getValue())).toList()));
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
        // 第十八批 BA 段第 324 条 324-R1：读范围见 AuditEventService.ReadScope。
        AuditEventService.ReadScope scope = AuditEventService.ReadScope.of(authorizationService, "audit.operation_log.view");
        return ApiResponse.ok(service.searchOperationEvents(
                businessType, businessId, actorId, eventName, parseInstant(from), parseInstant(to), limit,
                scope.merchantId(), scope.projectIds()));
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
            ExportFileService.Table table = operationEventsTable("audit.operation_log.export", businessType, businessId, actorId, eventName,
                    from, to, limit);
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
        return operationEventsTable("audit.operation_log.view", text(filters, "business_type"), text(filters, "business_id"), text(filters, "actor_id"),
                text(filters, "event_name"), text(filters, "from"), text(filters, "to"), ((Number) filters.get("limit")).intValue());
    }

    private static String text(Map<String, Object> filters, String key) {
        Object value = filters.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * 按筛选取操作留痕（范围按当前账号与入口权限码，第十八批 BA 段第 324 条 324-R1）；导出记录 filters 记筛选参数与取数所按的商户、项目范围。
     */
    private ExportFileService.Table operationEventsTable(String permissionCode, String businessType, String businessId, String actorId,
            String eventName, String from, String to, int limit) {
        AuditEventService.ReadScope scope = AuditEventService.ReadScope.of(authorizationService, permissionCode);
        List<AuditEvent> events = service.searchOperationEvents(
                businessType, businessId, actorId, eventName, parseInstant(from), parseInstant(to), limit, scope.merchantId(),
                scope.projectIds());
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("business_type", businessType);
        filters.put("business_id", businessId);
        filters.put("actor_id", actorId);
        filters.put("event_name", eventName);
        filters.put("from", from);
        filters.put("to", to);
        filters.put("limit", limit);
        filters.put("merchant_id", scope.merchantId());
        filters.put("project_ids", scope.projectIds() == null ? null : new java.util.TreeSet<>(scope.projectIds()));
        // 第十八批 CM 段第 372 条第 2 段 372-S2-R1（PGM:686 导出记录记下导出内容范围与筛选条件）：记下取数所按的外审数据时段。
        filters.put("data_window", com.complex.platform.access.AuditDataWindow.current().map(com.complex.platform.access.AuditDataWindow::label).orElse(null));
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
