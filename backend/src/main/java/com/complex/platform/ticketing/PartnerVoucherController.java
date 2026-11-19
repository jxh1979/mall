package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.ticketing.dto.PartnerVoucherConfigRequest;
import com.complex.platform.ticketing.dto.PartnerVoucherRedemptionImportRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static com.complex.platform.common.Strings.trimToNull;

/** 第三方券(批3)端点:配置券结算口径 + 接收餐厅核销对账回流并结算。 */
@RestController
@RequestMapping("/api/v1")
public class PartnerVoucherController {

    private final PartnerVoucherService service;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor operations;

    // 第十八批 BB 段 279-R1 与控制席并入②（PGM:966「已拍板：所有有过程的业务动作都必须留痕」）：两个写口的权限、范围、校验、状态冲突
    // 任一步失败经 AuditedOperationExecutor.recordingFailure 落失败留痕（带当前操作人）再原样抛出；成功留痕由服务层写。
    public PartnerVoucherController(PartnerVoucherService service, AuthorizationService authorizationService,
            AuditedOperationExecutor operations) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.operations = operations;
    }

    @PostMapping("/ticketing/partner-vouchers/configs")
    public ApiResponse<PartnerVoucherConfigRecord> createConfig(
            @RequestBody @jakarta.validation.Valid PartnerVoucherConfigRequest request) {
        String templateId = request == null ? null : trimToNull(request.entitlementTemplateId());
        return ApiResponse.ok(operations.recordingFailure("ticket.partner_voucher_config.save_failed", "ticket.entitlement.manage",
                "partner_voucher_config", templateId, null, () -> {
                    authorizationService.requirePermission("ticket.entitlement.manage");
                    return service.createConfig(request);
                }));
    }

    @PostMapping("/ticketing/partner-vouchers/redemptions")
    public ApiResponse<PartnerVoucherRedemptionRecord> importRedemption(
            @RequestBody @jakarta.validation.Valid PartnerVoucherRedemptionImportRequest request) {
        // 留痕的幂等键栏记外部核销单号（本接口按合作方加外部核销单号去重，不按网页自动附的 idempotency_key）。
        String externalRef = request == null ? null : trimToNull(request.externalRedemptionRef());
        return ApiResponse.ok(operations.recordingFailure("ticket.partner_voucher_redemption.import_failed", "verification.execute",
                "partner_voucher_redemption", null, externalRef, () -> {
                    authorizationService.requirePermission("verification.execute");
                    return service.importRedemption(request);
                }));
    }
}
