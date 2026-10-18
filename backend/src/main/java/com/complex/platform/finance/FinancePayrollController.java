package com.complex.platform.finance;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance/payroll")
public class FinancePayrollController {
    private final FinancePayrollService service;
    public FinancePayrollController(FinancePayrollService service) {this.service=service;}
    @GetMapping("/{statementId}/payment-attempts")
    public ApiResponse<FinancePayrollService.PaymentContext> attempts(@PathVariable String statementId) {return ApiResponse.ok(service.attempts(statementId));}
    @PostMapping("/{statementId}/payment-attempts")
    public ApiResponse<Map<String,Object>> prepare(@PathVariable String statementId,@RequestBody FinancePayrollService.PrepareRequest request) {return ApiResponse.ok(service.prepare(statementId,request));}
    @PostMapping("/payment-attempts/{id}/submit")
    public ApiResponse<Map<String,Object>> submit(@PathVariable String id,@RequestBody FinancePayrollService.SubmitRequest request) {return ApiResponse.ok(service.submit(id,request));}
    @PostMapping("/payment-attempts/{id}/result")
    public ApiResponse<Map<String,Object>> result(@PathVariable String id,@RequestBody FinancePayrollService.ResultRequest request) {return ApiResponse.ok(service.result(id,request));}
}
