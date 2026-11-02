package com.complex.platform.voucher;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.voucher.dto.VoucherDefinitionRequest;
import com.complex.platform.voucher.dto.VoucherIssueRequest;
import com.complex.platform.voucher.dto.VoucherRedeemPreviewRequest;
import com.complex.platform.voucher.dto.VoucherRedeemRequest;
import com.complex.platform.voucher.dto.VoucherRevokeRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 积分/券统一台账端点。权限：customer.voucher.view（只读）/ manage（建定义/发放/作废）/ redeem（核销）。
 * "全部券"聚合端点（/overview）读层并入通票第三方合作券（V282），不搬表。
 */
@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherController {

    // 内部结算价（成本）读脱敏权限（V279 bar 同款铁律）：无此权限的读端 internal_price_cent /
    // settlement_amount_cent 一律置 null。脱敏放在 controller（service 外）——executeWrite 幂等缓存
    // 存的是完整响应，避免脱敏值污染缓存后被有权限者重放读到 null。
    private static final String INTERNAL_PRICE_VIEW = "voucher.internal_price.view";

    private final VoucherService service;
    private final AuthorizationService authorizationService;

    public VoucherController(VoucherService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    private boolean canViewInternalPrice() {
        return authorizationService.currentActorHasPermission(INTERNAL_PRICE_VIEW);
    }

    @GetMapping("/definitions")
    public ApiResponse<PageResponse<VoucherDefinitionRecord>> listDefinitions(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "voucher_kind", required = false) String voucherKind,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "50") int pageSize) {
        PageResponse<VoucherDefinitionRecord> result = service.listDefinitions(status, voucherKind, page, pageSize);
        if (!canViewInternalPrice()) {
            result = new PageResponse<>(result.items().stream().map(VoucherDefinitionRecord::withoutInternalPrice).toList(),
                    result.page(), result.pageSize(), result.total());
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/definitions")
    public ApiResponse<VoucherDefinitionRecord> createDefinition(@RequestBody VoucherDefinitionRequest request) {
        VoucherDefinitionRecord saved = service.createDefinition(request);
        return ApiResponse.ok(canViewInternalPrice() ? saved : saved.withoutInternalPrice());
    }

    @GetMapping("/instances")
    public ApiResponse<PageResponse<VoucherInstanceRecord>> listInstances(
            @RequestParam(value = "customer_id", required = false) String customerId,
            @RequestParam(value = "definition_id", required = false) String definitionId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "source_type", required = false) String sourceType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "50") int pageSize) {
        return ApiResponse.ok(service.listInstances(customerId, definitionId, status, sourceType, page, pageSize));
    }

    @PostMapping("/instances")
    public ApiResponse<VoucherIssueResultRecord> issueVouchers(@RequestBody VoucherIssueRequest request) {
        return ApiResponse.ok(service.issueVouchers(request));
    }

    @PostMapping("/instances/{id}/revoke")
    public ApiResponse<VoucherInstanceRecord> revoke(
            @PathVariable("id") String instanceId,
            @RequestBody VoucherRevokeRequest request) {
        return ApiResponse.ok(service.revoke(instanceId, request));
    }

    @GetMapping("/redemptions")
    public ApiResponse<PageResponse<VoucherRedemptionEntryRecord>> listRedemptions(
            @RequestParam(value = "merchant_id", required = false) String merchantId,
            @RequestParam(value = "voucher_instance_id", required = false) String voucherInstanceId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "50") int pageSize) {
        PageResponse<VoucherRedemptionEntryRecord> result = service.listRedemptions(merchantId, voucherInstanceId, page, pageSize);
        if (!canViewInternalPrice()) {
            result = new PageResponse<>(result.items().stream().map(VoucherRedemptionEntryRecord::withoutInternalPrice).toList(),
                    result.page(), result.pageSize(), result.total());
        }
        return ApiResponse.ok(result);
    }

    /** 第十八批 M 段第 112 条 112-R1：按客人出示的券码查券（只读），工作手机与一体机核销前看能领什么、能领多少、是否已领。 */
    @PostMapping("/redemptions/preview")
    @com.complex.platform.access.DoesNotConsumeSingleUseGrant // M723：查券只看能否核销，真正核销才扣；不扣单次授权（操作员 2026-10-03）
    public ApiResponse<VoucherRedeemPreviewRecord> previewRedeem(@RequestBody VoucherRedeemPreviewRequest request) {
        return ApiResponse.ok(service.previewRedeem(request == null ? null : request.code()));
    }

    @PostMapping("/redemptions")
    public ApiResponse<VoucherRedemptionEntryRecord> redeem(@RequestBody VoucherRedeemRequest request) {
        VoucherRedemptionEntryRecord entry = service.redeem(request);
        return ApiResponse.ok(canViewInternalPrice() ? entry : entry.withoutInternalPrice());
    }

    @GetMapping("/overview")
    public ApiResponse<PageResponse<VoucherOverviewRecord>> overview(
            @RequestParam(value = "customer_id", required = false) String customerId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "50") int pageSize) {
        return ApiResponse.ok(service.overview(customerId, page, pageSize));
    }
}
