package com.complex.platform.evidence;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.evidence.dto.RiskAlertActionRequest;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import com.complex.platform.evidence.dto.RiskAlertResolveRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统一异常责任链 REST 端点（evidence-risk-layer-spec §8，P0 清单#5）。
 */
@RestController
@RequestMapping("/api/v1/evidence/risk-alerts")
public class RiskAlertController {

    private final RiskAlertService service;
    private final AuthorizationService authorizationService;

    public RiskAlertController(RiskAlertService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ApiResponse<RiskAlertRecord> report(@RequestBody RiskAlertReportRequest request) {
        authorizationService.requirePermission(RiskAlertService.REPORT_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.report(request));
    }

    @PostMapping("/{alert_id}/claim")
    public ApiResponse<RiskAlertRecord> claim(
            @PathVariable("alert_id") String alertId,
            @RequestBody RiskAlertActionRequest request
    ) {
        authorizationService.requirePermission(RiskAlertService.CLAIM_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.claim(alertId, request));
    }

    @PostMapping("/{alert_id}/transfer")
    public ApiResponse<RiskAlertRecord> transfer(
            @PathVariable("alert_id") String alertId,
            @RequestBody RiskAlertActionRequest request
    ) {
        authorizationService.requirePermission(RiskAlertService.TRANSFER_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.transfer(alertId, request));
    }

    @PostMapping("/{alert_id}/escalate")
    public ApiResponse<RiskAlertRecord> escalate(
            @PathVariable("alert_id") String alertId,
            @RequestBody RiskAlertActionRequest request
    ) {
        authorizationService.requirePermission(RiskAlertService.ESCALATE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.escalate(alertId, request));
    }

    @PostMapping("/{alert_id}/resolve")
    public ApiResponse<RiskAlertRecord> resolve(
            @PathVariable("alert_id") String alertId,
            @RequestBody RiskAlertResolveRequest request
    ) {
        authorizationService.requirePermission(RiskAlertService.RESOLVE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.resolve(alertId, request));
    }

    @PostMapping("/{alert_id}/dismiss")
    public ApiResponse<RiskAlertRecord> dismiss(
            @PathVariable("alert_id") String alertId,
            @RequestBody RiskAlertActionRequest request
    ) {
        authorizationService.requirePermission(RiskAlertService.DISMISS_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.dismiss(alertId, request));
    }

    @GetMapping
    public ApiResponse<List<RiskAlertRecord>> search(
            @RequestParam(name = "source_domain", required = false) String sourceDomain,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "project_id", required = false) String projectId,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        // 查询权限在 service.search 内部统一校验（含 scope 隔离）。
        return ApiResponse.ok(service.search(sourceDomain, status, projectId, limit));
    }

    @GetMapping("/{alert_id}")
    public ApiResponse<RiskAlertDetail> getDetail(@PathVariable("alert_id") String alertId) {
        return ApiResponse.ok(service.getDetail(alertId));
    }
}
