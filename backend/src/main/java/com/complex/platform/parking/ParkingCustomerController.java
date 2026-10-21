package com.complex.platform.parking;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.customer.CustomerService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HP-P01 客人端停车缴费与消费抵免查看（官方 App/小程序；出口二维码打开同一页面）。 */
@RestController
@RequestMapping("/api/v1/customer/parking")
public class ParkingCustomerController {

    private final ParkingCustomerService service;
    private final CustomerService customers;

    public ParkingCustomerController(ParkingCustomerService service, CustomerService customers) {
        this.service = service;
        this.customers = customers;
    }

    @GetMapping("/sessions")
    public ApiResponse<Map<String, Object>> lookup(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(value = "project_id", required = false) String projectId,
            @RequestParam(value = "plate_no", required = false) String plateNo,
            @RequestParam(value = "temp_code", required = false) String tempCode) {
        return ApiResponse.ok(service.lookup(customers.resolveCustomerId(token(customerToken, authorization)), projectId, plateNo, tempCode));
    }

    /** 本项目当日消费抵免（BSM「停车场」章「连接客人端，客人在官方 App/小程序缴费并查看消费抵免」）。 */
    @GetMapping("/credits")
    public ApiResponse<Map<String, Object>> credit(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(value = "project_id", required = false) String projectId) {
        return ApiResponse.ok(service.credit(customers.resolveCustomerId(token(customerToken, authorization)), projectId));
    }

    @PostMapping("/payments")
    public ApiResponse<Map<String, Object>> pay(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody(required = false) ParkingRequests.CustomerPayment request) {
        return ApiResponse.ok(service.pay(customers.resolveCustomerId(token(customerToken, authorization)), request));
    }

    @PostMapping("/payments/{payment_id}/confirm")
    public ApiResponse<Map<String, Object>> confirm(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("payment_id") String paymentId) {
        return ApiResponse.ok(service.confirm(customers.resolveCustomerId(token(customerToken, authorization)), paymentId));
    }

    private static String token(String customerToken, String authorization) {
        if (customerToken != null && !customerToken.isBlank()) {
            return customerToken.trim();
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        return customerToken;
    }
}
