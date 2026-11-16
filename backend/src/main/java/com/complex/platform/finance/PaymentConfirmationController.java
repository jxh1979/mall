package com.complex.platform.finance;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 第十八批 AM 段第 241 条 241-R1（BSM:1803 付款确认）：查某付款对象的付款确认与该类必带平行线；查看权限按对象类型，见 {@link PaymentConfirmationService#list}。 */
@RestController
@RequestMapping("/api/v1/finance")
public class PaymentConfirmationController {

    private final PaymentConfirmationService service;

    public PaymentConfirmationController(PaymentConfirmationService service) {
        this.service = service;
    }

    @GetMapping("/payment-confirmations")
    public ApiResponse<PaymentConfirmationService.View> listPaymentConfirmations(
            @RequestParam(name = "object_type", required = false) String objectType,
            @RequestParam(name = "object_id", required = false) String objectId) {
        return ApiResponse.ok(service.list(objectType, objectId));
    }
}
