package com.complex.platform.compliance;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.compliance.dto.ComplianceExpiryItemRequest;
import com.complex.platform.compliance.dto.ComplianceReminderScanRequest;
import com.complex.platform.compliance.dto.ContractKeyDateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {

    private final ComplianceService service;
    private final AuthorizationService authorizationService;

    public ComplianceController(ComplianceService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/contract-key-dates")
    public ApiResponse<PageResponse<ContractKeyDateRecord>> listContractKeyDates() {
        authorizationService.requirePermission("compliance.contract_key_date.view");
        return ApiResponse.ok(service.listContractKeyDates());
    }

    @PostMapping("/contract-key-dates")
    public ApiResponse<ContractKeyDateRecord> createContractKeyDate(@RequestBody ContractKeyDateRequest request) {
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
    public ApiResponse<ComplianceExpiryItemRecord> createExpiryItem(@RequestBody ComplianceExpiryItemRequest request) {
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
    public ApiResponse<ComplianceReminderScanResult> scanReminders(@RequestBody ComplianceReminderScanRequest request) {
        authorizationService.requirePermission("compliance.reminder.scan");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.scanReminders(request));
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
    }
}
