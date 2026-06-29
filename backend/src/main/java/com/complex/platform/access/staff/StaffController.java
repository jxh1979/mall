package com.complex.platform.access.staff;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.staff.dto.EmployeeNoticeAcknowledgeRequest;
import com.complex.platform.access.staff.dto.StaffDeviceBindRequest;
import com.complex.platform.access.staff.dto.StaffDeviceIntegrityReportRequest;
import com.complex.platform.access.staff.dto.StaffDeviceRebindDecisionRequest;
import com.complex.platform.access.staff.dto.StaffDeviceRevokeRequest;
import com.complex.platform.access.staff.dto.StaffRegistrationRequest;
import com.complex.platform.access.staff.dto.StaffRegistrationReviewRequest;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工三表 API（R5 T1）。权限/状态机/隔离由 {@link StaffService}（@GuardedAction + currentMerchantScope）执行；
 * 控制器仅校验幂等键并转发。member 私人手机 App + 后台管理共用本前缀。
 */
@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    @PostMapping("/registrations")
    public ApiResponse<StaffProfileRecord> submitRegistration(@RequestBody @jakarta.validation.Valid StaffRegistrationRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitRegistration(request));
    }

    @PostMapping("/registrations/{id}/review")
    public ApiResponse<StaffProfileRecord> reviewRegistration(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StaffRegistrationReviewRequest request
    ) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reviewRegistration(id, request));
    }

    @GetMapping("/profiles")
    public ApiResponse<PageResponse<StaffProfileRecord>> listProfiles() {
        return ApiResponse.ok(service.listProfiles());
    }

    @PostMapping("/device-credentials")
    public ApiResponse<StaffDeviceCredentialRecord> bindCredential(@RequestBody @jakarta.validation.Valid StaffDeviceBindRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.bindCredential(request));
    }

    @PostMapping("/device-credentials/{id}/approve-rebind")
    public ApiResponse<StaffDeviceCredentialRecord> approveRebind(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StaffDeviceRebindDecisionRequest request
    ) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveRebind(id, request));
    }

    @PostMapping("/device-credentials/{id}/report-integrity")
    public ApiResponse<StaffDeviceCredentialRecord> reportIntegrity(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StaffDeviceIntegrityReportRequest request
    ) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reportIntegrity(id, request));
    }

    @PostMapping("/device-credentials/{id}/revoke")
    public ApiResponse<StaffDeviceCredentialRecord> revokeCredential(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StaffDeviceRevokeRequest request
    ) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.revokeCredential(id, request));
    }

    @GetMapping("/notices")
    public ApiResponse<PageResponse<EmployeeNoticeRecord>> listNotices() {
        return ApiResponse.ok(service.listNotices());
    }

    @PostMapping("/notices/{id}/acknowledge")
    public ApiResponse<EmployeeNoticeRecord> acknowledge(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid EmployeeNoticeAcknowledgeRequest request
    ) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.acknowledge(id, request));
    }
}
