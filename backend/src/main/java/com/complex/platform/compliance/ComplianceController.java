// 文件版本：V0.02
package com.complex.platform.compliance;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.compliance.dto.ComplianceExpiryItemRequest;
import com.complex.platform.compliance.dto.ComplianceGraceRequests.GraceRequest;
import com.complex.platform.compliance.dto.ComplianceGraceRequests.ItemTypeConfigRequest;
import com.complex.platform.compliance.dto.ComplianceGraceRequests.RenewalRequest;
import com.complex.platform.compliance.dto.ComplianceReminderScanRequest;
import com.complex.platform.compliance.dto.ContractKeyDateRequest;
import com.complex.platform.compliance.dto.WecomConversationSyncRequest;
import com.complex.platform.compliance.dto.WecomRetentionPolicyRequest;
import com.complex.platform.compliance.dto.WecomRiskHitReviewRequest;
import com.complex.platform.gate.TerminalSwitchGuard;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {

    private final ComplianceService service;
    private final WecomConversationService wecomConversationService;
    private final AuthorizationService authorizationService;
    private final WecomWebhookSignatureVerifier wecomWebhookSignatureVerifier;
    private final TerminalSwitchGuard terminalSwitchGuard;

    public ComplianceController(
            ComplianceService service,
            WecomConversationService wecomConversationService,
            AuthorizationService authorizationService,
            WecomWebhookSignatureVerifier wecomWebhookSignatureVerifier,
            TerminalSwitchGuard terminalSwitchGuard
    ) {
        this.service = service;
        this.wecomConversationService = wecomConversationService;
        this.authorizationService = authorizationService;
        this.wecomWebhookSignatureVerifier = wecomWebhookSignatureVerifier;
        this.terminalSwitchGuard = terminalSwitchGuard;
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

    // ---------------------------------------------------------------- 第十八批 E 段第 22 条 证照宽限期、预警提前量、续期登记（PGM:531）

    @GetMapping("/item-type-configs")
    public ApiResponse<List<ComplianceItemTypeConfigRecord>> listItemTypeConfigs() {
        authorizationService.requirePermission("compliance.expiry_item.view");
        return ApiResponse.ok(service.listItemTypeConfigs());
    }

    @PutMapping("/item-type-configs/{item_type}")
    public ApiResponse<ComplianceItemTypeConfigRecord> saveItemTypeConfig(
            @PathVariable("item_type") String itemType,
            @RequestBody ItemTypeConfigRequest request
    ) {
        authorizationService.requirePermission("compliance.expiry_item.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.saveItemTypeConfig(itemType, request));
    }

    @PostMapping("/expiry-items/{id}/grace-requests")
    public ApiResponse<ComplianceExpiryItemRecord> requestGrace(@PathVariable String id, @RequestBody GraceRequest request) {
        authorizationService.requirePermission("compliance.grace_period.request");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestGrace(id, request));
    }

    @PostMapping("/expiry-items/{id}/renewal")
    public ApiResponse<ComplianceExpiryItemRecord> renewExpiryItem(@PathVariable String id, @RequestBody RenewalRequest request) {
        authorizationService.requirePermission("compliance.expiry_item.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.renewExpiryItem(id, request));
    }

    // ---------------------------------------------------------------- R5 T8 企微会话合规链

    @PostMapping("/wecom/conversations/sync")
    public ApiResponse<WecomSyncResult> syncWecomConversation(
            @RequestHeader(value = "X-Wecom-Signature", required = false) String signature,
            @RequestHeader(value = "X-Wecom-Timestamp", required = false) Long timestamp,
            @RequestHeader(value = "X-Wecom-Nonce", required = false) String nonce,
            @RequestBody @jakarta.validation.Valid WecomConversationSyncRequest request) {
        // 鉴权上下文 = webhook-signature（企微回调/拉取通道），不走 staff 权限闸。
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        // AUD-001：先过 T2 开关 integration.wecom.sync（关→TERMINAL_DISABLED），再做 HMAC 签名 + 时间窗 + nonce 校验，
        // 令未签名/伪造/过期请求 fail-closed，不再仅凭任意已认证员工放行；签名是该端点真实鉴权闸。
        terminalSwitchGuard.check(WecomConversationService.SYNC_CAPABILITY);
        // AUD 复核 P1-2：整个报文体（含 ownerStaffId/customerRef/messages）纳入验签 + nonce 持久化去重，见 verify。
        wecomWebhookSignatureVerifier.verify(signature, timestamp, nonce, request);
        return ApiResponse.ok(wecomConversationService.syncConversation(request));
    }

    @GetMapping("/wecom/conversations/{id}/messages")
    @com.complex.platform.access.ConsumesSingleUseGrant("wecom.view_raw") // M723：原文调阅看一次扣一次（操作员 2026-10-03）；脱敏调阅核的是 wecom.view，不扣
    public ApiResponse<PageResponse<WecomMessageView>> viewWecomMessages(
            @PathVariable String id,
            @RequestParam(value = "mode", required = false, defaultValue = "redacted") String mode,
            @RequestParam(value = "approval_ref", required = false) String approvalRef
    ) {
        // 原文调阅最小授权：raw 需 wecom.view_raw，脱敏展示需 wecom.view。
        // M723-F07（治理地图第 660 行原文「必须逐项单独授予并留痕」）：模式按与 WecomConversationService.viewMessages 相同的规范化
        // （空→redacted；去首尾空白、小写）判定，核权与服务调用用同一个结果；非 redacted／raw 的取值由服务层照旧 400。
        String resolvedMode = mode == null || mode.isBlank() ? "redacted" : mode.trim().toLowerCase(java.util.Locale.ROOT);
        boolean raw = "raw".equals(resolvedMode);
        authorizationService.requirePermission(raw ? "wecom.view_raw" : "wecom.view");
        // 平台合规口径 fail-closed：拒绝 merchant-scoped 调用方越权调阅他项目会话（即便权限被误授商户角色）。
        authorizationService.requirePlatformComplianceScope();
        String viewerUserId = authorizationService.currentActor().id();
        return ApiResponse.ok(wecomConversationService.viewMessages(id, resolvedMode, viewerUserId, approvalRef));
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
            @RequestBody(required = false) WecomRetentionPolicyRequest request
    ) {
        authorizationService.requirePermission("wecom.retention.manage");
        authorizationService.requirePlatformComplianceScope();
        return ApiResponse.ok(wecomConversationService.upsertRetentionPolicy(request));
    }
}
