package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.ticketing.dto.PartnerVoucherConfigRequest;
import com.complex.platform.ticketing.dto.PartnerVoucherRedemptionImportRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 第三方券(批3)端点:配置券结算口径 + 接收餐厅核销对账回流并结算。 */
@RestController
@RequestMapping("/api/v1")
public class PartnerVoucherController {

    private final PartnerVoucherService service;
    private final AuthorizationService authorizationService;

    public PartnerVoucherController(PartnerVoucherService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/ticketing/partner-vouchers/configs")
    public ApiResponse<PartnerVoucherConfigRecord> createConfig(
            @RequestBody @jakarta.validation.Valid PartnerVoucherConfigRequest request) {
        authorizationService.requirePermission("ticket.entitlement.manage");
        return ApiResponse.ok(service.createConfig(request));
    }

    @PostMapping("/ticketing/partner-vouchers/redemptions")
    public ApiResponse<PartnerVoucherRedemptionRecord> importRedemption(
            @RequestBody @jakarta.validation.Valid PartnerVoucherRedemptionImportRequest request) {
        authorizationService.requirePermission("verification.execute");
        return ApiResponse.ok(service.importRedemption(request));
    }
}
