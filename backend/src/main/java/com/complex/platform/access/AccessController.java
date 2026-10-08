package com.complex.platform.access;
// 文件版本：V0.07
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.dto.AccessGrantApplyRequest;
import com.complex.platform.access.dto.AccessGrantReviewRequest;
import com.complex.platform.access.dto.AccessUserRequest;
import com.complex.platform.access.dto.PermissionTemplateApplyRequest;
import com.complex.platform.access.dto.PermissionTemplateRequest;
import com.complex.platform.access.dto.PositionRoleTemplateRequest;
import com.complex.platform.access.dto.RoleAssignmentRequest;
import com.complex.platform.access.dto.RoleRequest;
import com.complex.platform.access.dto.RoleViewableProjectsRequest;
import com.complex.platform.access.dto.StaffLevelAssignmentRequest;
import com.complex.platform.access.dto.StatusChangeRequest;
import com.complex.platform.access.dto.TemporaryGrantRequest;
import com.complex.platform.access.dto.UserPermissionOverrideRequest;
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

    // M736：建角色时逐个勾选「可查看项目」的候选清单；能建角色的人都能读（与建角色同一门禁）。
    @GetMapping("/role-project-options")
    public ApiResponse<PageResponse<RoleProjectOption>> listRoleProjectOptions() {
        authorizationService.requirePermission("access.role.manage");
        return ApiResponse.ok(service.listRoleProjectOptions());
    }

    // M736 第二步：角色分出去以后增减可查看项目；与建角色同一入口码。
    @GetMapping("/roles/{id}/viewable-projects")
    public ApiResponse<RoleViewableProjectsView> getRoleViewableProjects(@PathVariable String id) {
        authorizationService.requirePermission("access.role.manage");
        return ApiResponse.ok(service.getRoleViewableProjects(id));
    }

    @PutMapping("/roles/{id}/viewable-projects")
    public ApiResponse<RoleViewableProjectsView> updateRoleViewableProjects(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid RoleViewableProjectsRequest request
    ) {
        authorizationService.requirePermission("access.role.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateRoleViewableProjects(id, request));
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
        // M002-F42（治理地图第 651 行「只能由具备分配/取消分配角色权限的人发起」、第 656 行「临时授权可设有效期或单次使用」；操作员裁决 M002-F37）：
        // 员工单独授予、解除员工显式拒绝、岗位模板高危权限释放三类申请，申请权限与分配角色权限在入口一次核验、同一单次来源只消耗一次（服务层不再按有效集复查，
        // 复查会把本次刚消耗的来源当成不持有而误拒）。先做不消耗来源的判定：不持申请权限的照基线拒绝；不持（含显式拒绝）分配角色权限的按 F37 拒绝，都不消耗任何来源。
        if (AccessService.isRoleAssignInitiatedRequest(request == null ? null : request.requestType())) {
            if (!authorizationService.currentActorHasPermission("access.grant_request.apply")) {
                authorizationService.requirePermission("access.grant_request.apply");
            }
            if (!authorizationService.currentActorHasPermission("access.role.assign")) {
                throw ApiException.forbidden("PERMISSION_DENIED",
                        "员工单独授予、解除员工显式拒绝、岗位模板高危权限释放这三类授权申请，只能由具备分配/取消分配角色权限的人发起");
            }
            authorizationService.requirePermissions("access.grant_request.apply", "access.role.assign");
        } else {
            authorizationService.requirePermission("access.grant_request.apply");
        }
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
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写授权失效时间");
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
        return ApiResponse.ok(service.effectivePermissionsForUser(id));
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

    // M087-A-04（PGM-1.10.1「保存所属商户或发生跨项目变更」）：人员范围变更写入口，与定岗同一门禁。
    @PutMapping("/users/{id}/scope")
    public ApiResponse<AccessGrantRequestRecord> changeUserScope(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid com.complex.platform.access.dto.UserScopeChangeRequest request
    ) {
        authorizationService.requirePermission("access.role.assign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.changeUserScope(id, request));
    }

    // M006-G-01（PGM-1.10.1「员工单独授予 − 显式拒绝」）：员工级覆盖写入口，与角色装配同一门禁。
    @PostMapping("/users/{id}/permission-overrides")
    public ApiResponse<EffectivePermissions> setUserPermissionOverrides(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid UserPermissionOverrideRequest request
    ) {
        authorizationService.requirePermission("access.role.assign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.setUserPermissionOverrides(id, request));
    }

    @PostMapping("/users/{id}/permission-overrides/remove")
    public ApiResponse<EffectivePermissions> removeUserPermissionOverrides(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid UserPermissionOverrideRequest request
    ) {
        // M002-F08（操作员 2026-09-25 裁决「本人免审批，他人须审批」，本人「不要求事先持有该码」）；M002-F09（控制席按长期规则自动批准的修复）：
        // 最高权限本人会话解除自己账号上的显式拒绝不过 access.role.assign 门禁（该码本身可能正被拒绝）；服务层仍锁目标行、只删显式行。
        // 其余请求门禁不变。
        AccessActor actor = authorizationService.currentActor();
        boolean masterSelfDenyRemoval = actor != null && authorizationService.isMasterAuthority(actor) && actor.id().equals(id)
                && request != null && request.effect() != null && "deny".equalsIgnoreCase(request.effect().trim());
        if (!masterSelfDenyRemoval) {
            authorizationService.requirePermission("access.role.assign");
        }
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.removeUserPermissionOverrides(id, request));
    }
}
