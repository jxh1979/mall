package com.complex.platform.access.org;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** M087-A-01 / A-03：组织层级（直接上级）与平行审批线维护端点；门禁同角色装配（access.role.assign）。 */
@RestController
@RequestMapping("/api/v1/access")
public class OrgHierarchyController {

    private final OrgHierarchyService service;
    private final AuthorizationService authorizationService;

    public OrgHierarchyController(OrgHierarchyService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/org-hierarchy")
    public ApiResponse<List<SupervisorReviewRepository.OrgSuperior>> listHierarchy(
            @RequestParam(name = "limit", defaultValue = "500") int limit) {
        authorizationService.requirePermission(OrgHierarchyService.MANAGE_PERMISSION);
        return ApiResponse.ok(service.listHierarchy(limit));
    }

    @PutMapping("/org-hierarchy/{user_id}")
    public ApiResponse<SupervisorReviewRepository.OrgSuperior> setDirectSuperior(
            @PathVariable("user_id") String userId,
            @RequestBody @jakarta.validation.Valid OrgSuperiorRequest request
    ) {
        authorizationService.requirePermission(OrgHierarchyService.MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.setDirectSuperior(userId, request));
    }

    @GetMapping("/approval-lines")
    public ApiResponse<List<SupervisorReviewRepository.ApprovalLine>> listApprovalLines() {
        authorizationService.requirePermission(OrgHierarchyService.MANAGE_PERMISSION);
        return ApiResponse.ok(service.listApprovalLines());
    }

    @PutMapping("/approval-lines/{line_code}")
    public ApiResponse<SupervisorReviewRepository.ApprovalLine> upsertApprovalLine(
            @PathVariable("line_code") String lineCode,
            @RequestBody @jakarta.validation.Valid ApprovalLineRequest request
    ) {
        authorizationService.requirePermission(OrgHierarchyService.MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.upsertApprovalLine(lineCode, request));
    }
}
