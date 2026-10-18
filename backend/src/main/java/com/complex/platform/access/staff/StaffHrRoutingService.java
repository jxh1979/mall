package com.complex.platform.access.staff;

// 文件版本：V0.01

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** 人事会签只读取员工部门配置；随单传入的会签人不能改变路由。 */
@Service
public class StaffHrRoutingService {
    private final SupervisorReviewRepository reviews;
    private final AccessRepository access;
    private final AuthorizationService authorization;

    public StaffHrRoutingService(SupervisorReviewRepository reviews, AccessRepository access,
            AuthorizationService authorization) {
        this.reviews = reviews;
        this.access = access;
        this.authorization = authorization;
    }

    public Optional<String> resolve(String employeeId, String initiatorId) {
        return reviews.findDepartmentHrApprover(employeeId)
                .filter(hr -> eligible(hr, employeeId, initiatorId));
    }

    public boolean eligible(String approverId, String employeeId, String initiatorId) {
        if (approverId == null || approverId.equals(employeeId) || approverId.equals(initiatorId)) return false;
        return canReviewEmployee(approverId, employeeId, StaffApprovalService.HR_PERMISSION);
    }

    public boolean canReviewEmployee(String approverId, String employeeId, String permission) {
        var approver = access.findUserById(approverId).filter(user -> "active".equals(user.status())).orElse(null);
        var employee = access.findUserById(employeeId).orElse(null);
        if (approver == null || employee == null
                || !authorization.holdsPermissionViaRole(approverId, permission)) return false;
        if (approver.merchantId() != null && !approver.merchantId().isBlank()
                && !approver.merchantId().equals(employee.merchantId())) return false;
        var actor = new AccessActor(approver.id(), approver.username(), approver.accountType(),
                approver.organizationId(), approver.merchantId(), approver.status());
        return authorization.canReachUser(actor, employee, permission);
    }

    public void requireDirectSupervisor(String employeeId) {
        if (employeeId == null || reviews.findDirectSuperior(employeeId)
                .filter(authorization.currentActor().id()::equals).isEmpty()) {
            throw ApiException.forbidden("DIRECT_SUPERVISOR_REQUIRED", "独立员工状态变更须由该员工直属领导发起");
        }
    }
}
