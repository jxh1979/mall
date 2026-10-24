package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * ROOT-03-R29（BSM 第 986 行「启用售票能力的一体机可由授权服务人员登录后辅助客人购票和查询订单……客人用手机号短信验证绑定，在官方 App/小程序
 * 查看门票」）：一体机上授权服务人员给客人手机号发绑定验证码，客人报码后随下单（sales_channel=kiosk 的 sms_code）核验。
 */
@RestController
public class KioskBindCodeController {

    private final CustomerService customers;
    private final AuthorizationService authorizationService;

    public KioskBindCodeController(CustomerService customers, AuthorizationService authorizationService) {
        this.customers = customers;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/api/v1/ticketing/kiosk-bind-codes")
    public ApiResponse<Map<String, Object>> requestKioskBindCode(@RequestBody(required = false) Map<String, String> request) {
        authorizationService.requirePermission("ticket.order.create");
        return ApiResponse.ok(customers.requestKioskBindCode(request == null ? null : request.get("phone")));
    }
}
