package com.complex.platform.access;
// 文件版本：V0.04
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.dto.AccessGrantApplyRequest;
import com.complex.platform.access.dto.AccessGrantReviewRequest;
import com.complex.platform.access.dto.AccessUserRequest;
import com.complex.platform.access.dto.PermissionTemplateApplyRequest;
import com.complex.platform.access.dto.PermissionTemplateRequest;
import com.complex.platform.access.dto.PositionRoleTemplateRequest;
import com.complex.platform.access.dto.RoleAssignmentRequest;
import com.complex.platform.access.dto.RoleRequest;
import com.complex.platform.access.dto.StaffLevelAssignmentRequest;
import com.complex.platform.access.dto.StatusChangeRequest;
import com.complex.platform.access.dto.TemporaryGrantRequest;
import com.complex.platform.access.dto.UserPositionAssignmentRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/access")
public class AccessController {

    private final AccessService service;
    private final AuthorizationService authorizationService;

    public AccessController(AccessService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/permission-templates")
    public ApiResponse<PageResponse<PermissionTemplate>> listPermissionTemplates() {
        authorizationService.requirePermission("access.permission_template.view");
        return ApiResponse.ok(service.listPermissionTemplates());
    }

    @PostMapping("/permission-templates")
    public ApiResponse<PermissionTemplate> createPermissionTemplate(@RequestBody @jakarta.validation.Valid PermissionTemplateRequest request) {
        authorizationService.requirePermission("access.permission_template.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createPermissionTemplate(request));
    }

    @PutMapping("/permission-templates/{id}")
    public ApiResponse<PermissionTemplate> updatePermissionTemplate(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid PermissionTemplateRequest request
    ) {
        authorizationService.requirePermission("access.permission_template.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updatePermissionTemplate(id, request));
    }

    @PostMapping("/permission-templates/{id}/apply")
    public ApiResponse<PermissionTemplateApplyResult> applyPermissionTemplate(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid PermissionTemplateApplyRequest request
    ) {
        authorizationService.requirePermission("access.permission_template.apply");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.applyPermissionTemplate(id, request));
    }

    @PostMapping("/permission-templates/{id}/copy")
    public ApiResponse<PermissionTemplate> copyPermissionTemplate(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid PermissionTemplateRequest request
    ) {
        authorizationService.requirePermission("access.permission_template.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.copyPermissionTemplate(id, request));
    }

    @GetMapping("/permissions")
    public ApiResponse<PageResponse<PermissionDefinition>> listPermissions() {
        authorizationService.requirePermission("access.permission.view");
        return ApiResponse.ok(service.listPermissions());
    }

    @GetMapping("/roles")
    public ApiResponse<PageResponse<RoleRecord>> listRoles() {
        authorizationService.requirePermission("access.role.view");
        return ApiResponse.ok(service.listRoles());
    }

    @PostMapping("/roles")
    public ApiResponse<RoleRecord> createRole(@RequestBody @jakarta.validation.Valid RoleRequest request) {
        authorizationService.requirePermission("access.role.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createRole(request));
    }

    @PostMapping("/role-assignments")
    public ApiResponse<RoleAssignmentRecord> assignUserRole(@RequestBody @jakarta.validation.Valid RoleAssignmentRequest request) {
        authorizationService.requirePermission("access.role.assign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.assignUserRole(request));
    }

    @PostMapping("/role-assignments/unassign")
    public ApiResponse<RoleAssignmentRecord> unassignUserRole(@RequestBody @jakarta.validation.Valid RoleAssignmentRequest request) {
        authorizationService.requirePermission("access.role.assign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.unassignUserRole(request));
    }

    @GetMapping("/grant-policy")
    public ApiResponse<AccessGrantPolicy> grantPolicy() {
        authorizationService.requirePermission("access.grant_request.view");
        return ApiResponse.ok(service.grantPolicy());
    }

    @PostMapping("/grant-policy")
    public ApiResponse<AccessGrantPolicy> updateGrantPolicy(@RequestBody AccessGrantPolicy.Update request) {
        authorizationService.requirePermission("admission.policy.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateGrantPolicy(request));
    }

    @PostMapping("/grant-requests")
    public ApiResponse<AccessGrantRequestRecord> applyGrant(@RequestBody @jakarta.validation.Valid AccessGrantApplyRequest request) {
        authorizationService.requirePermission("access.grant_request.apply");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.applyGrant(request));
    }

    @GetMapping("/grant-requests")
    public ApiResponse<PageResponse<AccessGrantRequestRecord>> listGrantRequests(
            @RequestParam(name = "window_days", required = false) Integer windowDays,
            @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        authorizationService.requirePermission("access.grant_request.view");
        return ApiResponse.ok(service.listGrantRequests(windowDays, page));
    }

    @PostMapping("/grant-requests/{id}/review")
    public ApiResponse<AccessGrantRequestRecord> reviewGrant(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid AccessGrantReviewRequest request
    ) {
        authorizationService.requirePermissions("access.grant_request.review", "governance.supervisor_review.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reviewGrant(id, request));
    }

    @GetMapping("/grant-requests/summary")
    public ApiResponse<AccessGrantSummary> grantSummary(
            @RequestParam(name = "window_days", required = false) Integer windowDays,
            @RequestParam(name = "position_threshold", required = false) Integer positionThreshold,
            @RequestParam(name = "user_threshold", required = false) Integer userThreshold
    ) {
        authorizationService.requirePermission("access.grant_request.view");
        return ApiResponse.ok(service.grantSummary(windowDays, positionThreshold, userThreshold));
    }

    @GetMapping("/position-role-templates")
    public ApiResponse<PageResponse<PositionRoleTemplate>> listPositionRoleTemplates(
            @RequestParam(name = "include_archived", defaultValue = "false") boolean includeArchived) {
        authorizationService.requirePermission("access.role.assign");
        return ApiResponse.ok(service.listPositionRoleTemplates(includeArchived));
    }

    @PostMapping("/position-role-templates")
    public ApiResponse<PositionRoleTemplate> createPositionRoleTemplate(@RequestBody @jakarta.validation.Valid PositionRoleTemplateRequest request) {
        authorizationService.requirePermission("access.role.assign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createPositionRoleTemplate(request));
    }

    @PutMapping("/position-role-templates/{id}")
    public ApiResponse<PositionRoleTemplate> updatePositionRoleTemplate(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid PositionRoleTemplateRequest request
    ) {
        authorizationService.requirePermission("access.role.assign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updatePositionRoleTemplate(id, request));
    }

    @PostMapping("/position-role-templates/{id}/lifecycle")
    public ApiResponse<PositionRoleTemplate> changePositionTemplateLifecycle(
            @PathVariable String id, @RequestBody StatusChangeRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        if ("approve_delete".equals(request.action()) || "reject_delete".equals(request.action())) {
            authorizationService.requirePermissions("access.role.assign", "governance.supervisor_review.approve");
        } else {
            authorizationService.requirePermission("access.role.assign");
        }
        return ApiResponse.ok(service.changePositionTemplateLifecycle(id, request));
    }

    @DeleteMapping("/position-role-templates/{id}")
    public ApiResponse<Void> deletePositionRoleTemplate(@PathVariable String id) {
        authorizationService.requirePermission("access.role.assign");
        service.deletePositionRoleTemplate(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/temporary-grants")
    public ApiResponse<TemporaryGrant> createTemporaryGrant(@RequestBody @jakarta.validation.Valid TemporaryGrantRequest request) {
        authorizationService.requirePermission("access.temporary_grant.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        if (request.validUntil() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "valid_until is required");
        }
        return ApiResponse.ok(service.createTemporaryGrant(request));
    }

    @PostMapping("/temporary-grants/{id}/revoke")
    public ApiResponse<TemporaryGrant> revokeTemporaryGrant(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("access.temporary_grant.revoke");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.revokeTemporaryGrant(id, request));
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<AccessUser>> listAccessUsers() {
        authorizationService.requirePermission("access.user.view");
        return ApiResponse.ok(service.listAccessUsers());
    }

    @PostMapping("/users")
    public ApiResponse<AccessUser> createAccessUser(@RequestBody @jakarta.validation.Valid AccessUserRequest request) {
        authorizationService.requirePermission("access.user.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createAccessUser(request));
    }

    @GetMapping("/effective-permissions")
    public ApiResponse<EffectivePermissions> effectivePermissions() {
        // self 端：仅需已登录（currentActor 在未认证时抛 UNAUTHENTICATED），返回自身有效集，scope 安全。
        return ApiResponse.ok(authorizationService.effectivePermissions());
    }

    @GetMapping("/users/{id}/effective-permissions")
    public ApiResponse<EffectivePermissions> effectivePermissionsForUser(@PathVariable String id) {
        authorizationService.requirePermission("access.user.view");
        return ApiResponse.ok(authorizationService.effectivePermissionsForUser(id));
    }

    @GetMapping("/staff-levels")
    public ApiResponse<PageResponse<StaffLevelRecord>> listStaffLevels() {
        authorizationService.requirePermission("access.staff_level.view");
        return ApiResponse.ok(service.listStaffLevels());
    }

    @PutMapping("/users/{id}/staff-level")
    public ApiResponse<StaffLevelAssignment> assignStaffLevel(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StaffLevelAssignmentRequest request
    ) {
        authorizationService.requirePermission("access.staff_level.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        StaffLevelAssignmentRequest withUser = new StaffLevelAssignmentRequest(
                request == null ? null : request.idempotencyKey(),
                request == null ? null : request.operatorNote(),
                id,
                request == null ? null : request.staffLevelId(),
                request == null ? null : request.baseSalaryAmountCent(),
                request == null ? null : request.salaryCurrency(),
                request == null ? null : request.salaryDetail()
        );
        return ApiResponse.ok(service.assignStaffLevel(withUser));
    }

    @PutMapping("/users/{id}/position")
    public ApiResponse<UserPositionAssignmentResult> assignUserPosition(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid UserPositionAssignmentRequest request
    ) {
        authorizationService.requirePermission("access.role.assign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        UserPositionAssignmentRequest withUser = new UserPositionAssignmentRequest(
                request == null ? null : request.idempotencyKey(),
                request == null ? null : request.operatorNote(),
                id,
                request == null ? null : request.positionCode(),
                request == null ? null : request.staffLevelId(),
                request == null ? null : request.baseSalaryAmountCent(),
                request == null ? null : request.salaryCurrency(),
                request == null ? null : request.salaryDetail()
        );
        return ApiResponse.ok(service.assignUserPosition(withUser));
    }
}
