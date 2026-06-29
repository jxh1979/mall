package com.complex.platform.compliance;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.compliance.dto.ComplianceExpiryItemRequest;
import com.complex.platform.compliance.dto.ComplianceReminderScanRequest;
import com.complex.platform.compliance.dto.ContractKeyDateRequest;
import com.complex.platform.compliance.dto.WecomConversationSyncRequest;
import com.complex.platform.compliance.dto.WecomRetentionPolicyRequest;
import com.complex.platform.compliance.dto.WecomRiskHitReviewRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {

    private final ComplianceService service;
    private final WecomConversationService wecomConversationService;
    private final AuthorizationService authorizationService;

    public ComplianceController(
            ComplianceService service,
            WecomConversationService wecomConversationService,
            AuthorizationService authorizationService
    ) {
        this.service = service;
        this.wecomConversationService = wecomConversationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/contract-key-dates")
    public ApiResponse<PageResponse<ContractKeyDateRecord>> listContractKeyDates() {
        authorizationService.requirePermission("compliance.contract_key_date.view");
        return ApiResponse.ok(service.listContractKeyDates());
    }

    @PostMapping("/contract-key-dates")
    public ApiResponse<ContractKeyDateRecord> createContractKeyDate(@RequestBody @jakarta.validation.Valid ContractKeyDateRequest request) {
        authorizationService.requirePermission("compliance.contract_key_date.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createContractKeyDate(request));
    }

    @GetMapping("/expiry-items")
    public ApiResponse<PageResponse<ComplianceExpiryItemRecord>> listExpiryItems() {
        authorizationService.requirePermission("compliance.expiry_item.view");
        return ApiResponse.ok(service.listExpiryItems());
    }

    @PostMapping("/expiry-items")
    public ApiResponse<ComplianceExpiryItemRecord> createExpiryItem(@RequestBody @jakarta.validation.Valid ComplianceExpiryItemRequest request) {
        authorizationService.requirePermission("compliance.expiry_item.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createExpiryItem(request));
    }

    @GetMapping("/reminders")
    public ApiResponse<PageResponse<ComplianceReminderRecord>> listReminders() {
        authorizationService.requirePermission("compliance.reminder.view");
        return ApiResponse.ok(service.listReminders());
    }

    @PostMapping("/reminders/scan")
    public ApiResponse<ComplianceReminderScanResult> scanReminders(@RequestBody @jakarta.validation.Valid ComplianceReminderScanRequest request) {
        authorizationService.requirePermission("compliance.reminder.scan");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.scanReminders(request));
    }

    // ---------------------------------------------------------------- R5 T8 企微会话合规链

    @PostMapping("/wecom/conversations/sync")
    public ApiResponse<WecomSyncResult> syncWecomConversation(@RequestBody @jakarta.validation.Valid WecomConversationSyncRequest request) {
        // 同步入站受 T2 开关 integration.wecom.sync 编程式门控（在 service 内 check，关→TERMINAL_DISABLED）。
        // 鉴权上下文 = webhook-signature（企微回调/拉取通道），不走 staff 权限闸。
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(wecomConversationService.syncConversation(request));
    }

    @GetMapping("/wecom/conversations/{id}/messages")
    public ApiResponse<PageResponse<WecomMessageView>> viewWecomMessages(
            @PathVariable String id,
            @RequestParam(value = "mode", required = false, defaultValue = "redacted") String mode,
            @RequestParam(value = "approval_ref", required = false) String approvalRef
    ) {
        // 原文调阅最小授权：raw 需 wecom.view_raw，脱敏展示需 wecom.view。
        boolean raw = "raw".equalsIgnoreCase(mode);
        authorizationService.requirePermission(raw ? "wecom.view_raw" : "wecom.view");
        // 平台合规口径 fail-closed：拒绝 merchant-scoped 调用方越权调阅他项目会话（即便权限被误授商户角色）。
        authorizationService.requirePlatformComplianceScope();
        String viewerUserId = authorizationService.currentActor().id();
        return ApiResponse.ok(wecomConversationService.viewMessages(id, mode, viewerUserId, approvalRef));
    }

    // ---------------------------------------------------------------- P0#8 企微违规命中查询 / 复核处置

    @GetMapping("/wecom/risk-hits")
    public ApiResponse<PageResponse<WecomRiskHitRecord>> listWecomRiskHits(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "conversation_id", required = false) String conversationId
    ) {
        authorizationService.requirePermission("wecom.risk_hit.view");
        authorizationService.requirePlatformComplianceScope();
        return ApiResponse.ok(wecomConversationService.listRiskHits(status, conversationId));
    }

    @PostMapping("/wecom/risk-hits/{id}/review")
    public ApiResponse<WecomRiskHitRecord> reviewWecomRiskHit(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid WecomRiskHitReviewRequest request
    ) {
        // 复核处置 reviewing/confirmed/dismissed：高敏操作，需 wecom.risk_hit.review。
        authorizationService.requirePermission("wecom.risk_hit.review");
        authorizationService.requirePlatformComplianceScope();
        String reviewerId = authorizationService.currentActor().id();
        return ApiResponse.ok(wecomConversationService.reviewRiskHit(
                id,
                request == null ? null : request.status(),
                reviewerId,
                request == null ? null : request.reviewNote()));
    }

    @GetMapping("/wecom/retention-policies")
    public ApiResponse<PageResponse<WecomRetentionPolicyRecord>> listWecomRetentionPolicies() {
        authorizationService.requirePermission("wecom.retention.manage");
        authorizationService.requirePlatformComplianceScope();
        return ApiResponse.ok(wecomConversationService.listRetentionPolicies());
    }

    @PostMapping("/wecom/retention-policies")
    public ApiResponse<WecomRetentionPolicyRecord> upsertWecomRetentionPolicy(
            @RequestBody @jakarta.validation.Valid WecomRetentionPolicyRequest request
    ) {
        authorizationService.requirePermission("wecom.retention.manage");
        authorizationService.requirePlatformComplianceScope();
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(wecomConversationService.upsertRetentionPolicy(request));
    }
}
