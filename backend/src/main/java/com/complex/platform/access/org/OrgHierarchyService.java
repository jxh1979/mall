package com.complex.platform.access.org;

// 文件版本：V0.02

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.AuthorizationService.ProjectReach;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * M087-A-01 / A-03（PGM-1.7.1「系统按组织层级自动补全……直接上级节点」「每个部门或业务线须预先设定其平行审批的最低起点层级」；
 * PGM-1.10.1「HR 部门定每个人的层级/职位」）：组织层级（直接上级）与平行审批线的维护入口。
 * 直接上级不得是本人、不得成环；线的最低起点层级为 level_rank 1..30。全部写入留痕。
 */
@Service
public class OrgHierarchyService {

    /** 与角色装配/定岗同一门禁（组织层级决定谁能审批，属授权治理写路径）。 */
    public static final String MANAGE_PERMISSION = "access.role.assign";
    private static final int MAX_DEPTH = 50;

    private final SupervisorReviewRepository reviewRepository;
    private final AccessRepository accessRepository;
    private final AuthorizationService authorizationService;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public OrgHierarchyService(SupervisorReviewRepository reviewRepository, AccessRepository accessRepository,
                               AuthorizationService authorizationService, IdempotencyService idempotencyService,
                               AuditEventService auditEventService) {
        this.reviewRepository = reviewRepository;
        this.accessRepository = accessRepository;
        this.authorizationService = authorizationService;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public SupervisorReviewRepository.OrgSuperior setDirectSuperior(String userId, OrgSuperiorRequest request) {
        authorizationService.requirePermission(MANAGE_PERMISSION);
        String id = requireText(userId, "user_id");
        requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
        AccessActor actor = authorizationService.currentActor();
        ProjectReach reach = authorizationService.projectReach(actor, MANAGE_PERMISSION);
        accessRepository.lockGrantAuthorizationFacts();
        requireUserScope(actor, reach, accessRepository.findUserById(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在：" + id)));
        String superior = blank(request.superiorId()) ? null : request.superiorId().trim();
        if (superior != null) {
            requireUserScope(actor, reach, accessRepository.findUserById(superior)
                    .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "直接上级账号不存在：" + superior)));
        }
        String line = blank(request.lineCode()) ? null : request.lineCode().trim();
        if (line != null) {
            requireLineScope(actor, reach, reviewRepository.findApprovalLine(line)
                    .orElseThrow(() -> ApiException.badRequest("APPROVAL_LINE_NOT_CONFIGURED", "平行审批线未配置：" + line)));
        }
        return idempotencyService.execute(
                "orgHierarchySet:" + id,
                request.idempotencyKey(),
                request,
                () -> {
                    if (superior != null) {
                        if (superior.equals(id)) {
                            throw ApiException.badRequest("ORG_HIERARCHY_SELF", "直接上级不能是本人");
                        }
                        String cursor = superior;
                        for (int depth = 0; depth < MAX_DEPTH; depth++) {
                            Optional<String> up = reviewRepository.findDirectSuperior(cursor);
                            if (up.isEmpty()) {
                                break;
                            }
                            if (up.get().equals(id)) {
                                throw ApiException.conflict("ORG_HIERARCHY_CYCLE", "该设置会使汇报线成环：" + id + " → " + superior);
                            }
                            cursor = up.get();
                        }
                    }
                    Instant now = Instant.now();
                    reviewRepository.saveDirectSuperior(id, superior, line, actor.id(), now);
                    auditEventService.recordSuccess(
                            "access.org_hierarchy.superior_set",
                            MANAGE_PERMISSION,
                            "org_hierarchy",
                            id,
                            null,
                            request.idempotencyKey(),
                            Map.of("superior_id", superior == null ? "" : superior, "line_code", line == null ? "" : line,
                                    "comment", request.operatorNote() == null ? "" : request.operatorNote())
                    );
                    return new SupervisorReviewRepository.OrgSuperior(id, superior, line, actor.id(), now);
                },
                SupervisorReviewRepository.OrgSuperior.class
        );
    }

    public List<SupervisorReviewRepository.OrgSuperior> listHierarchy(int limit) {
        authorizationService.requirePermission(MANAGE_PERMISSION);
        AccessActor actor = authorizationService.currentActor();
        ProjectReach reach = authorizationService.projectReach(actor, MANAGE_PERMISSION);
        return reviewRepository.listOrgHierarchy(Integer.MAX_VALUE).stream()
                .filter(row -> canManageUser(actor, reach, row.userId()))
                .filter(row -> blank(row.superiorId()) || canManageUser(actor, reach, row.superiorId()))
                .filter(row -> blank(row.lineCode()) || reviewRepository.findApprovalLine(row.lineCode())
                        .map(line -> canManageLine(actor, reach, line)).orElse(false))
                .limit(Math.max(1, Math.min(limit, 2000))).toList();
    }

    @Transactional
    public SupervisorReviewRepository.ApprovalLine upsertApprovalLine(String lineCode, ApprovalLineRequest request) {
        try {
            return doUpsertApprovalLine(lineCode, request);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.approval_line.save_failed", MANAGE_PERMISSION,
                    "approval_line", lineCode, null, request == null ? null : request.idempotencyKey(),
                    exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private SupervisorReviewRepository.ApprovalLine doUpsertApprovalLine(String lineCode, ApprovalLineRequest request) {
        authorizationService.requirePermission(MANAGE_PERMISSION);
        String code = requireText(lineCode, "line_code");
        requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
        AccessActor actor = authorizationService.currentActor();
        ProjectReach reach = authorizationService.projectReach(actor, MANAGE_PERMISSION);
        // 与组织归属变更共用事实锁；原线范围核对也覆盖幂等重放。
        accessRepository.lockGrantAuthorizationFacts();
        Optional<SupervisorReviewRepository.ApprovalLine> previous = reviewRepository.findApprovalLine(code);
        previous.ifPresent(line -> requireLineScope(actor, reach, line));
        return idempotencyService.execute(
                "approvalLineUpsert:" + code,
                request.idempotencyKey(),
                request,
                () -> {
                    String name = requireText(request.lineName(), "line_name");
                    int minStartRank = request.minStartRank() == null ? 1 : request.minStartRank();
                    if (minStartRank < 1 || minStartRank > 30) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "平行审批最低起点层级须在 1 到 30 之间");
                    }
                    String entry = blank(request.entryUserId()) ? null : request.entryUserId().trim();
                    if (entry != null) {
                        requireUserScope(actor, reach, accessRepository.findUserById(entry)
                                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "起点人账号不存在：" + entry)));
                    }
                    String status = blank(request.status()) ? "active" : request.status().trim().toLowerCase(java.util.Locale.ROOT);
                    if (!List.of("active", "inactive").contains(status)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "状态只能是启用（active）或停用（inactive）");
                    }
                    String hr = blank(request.hrFinalApproverId()) ? null : request.hrFinalApproverId().trim();
                    if (hr != null) {
                        var user = accessRepository.findUserById(hr).filter(u -> "active".equals(u.status()))
                                .orElseThrow(() -> ApiException.badRequest("HR_APPROVER_INVALID", "部门人力负责人账号不存在或已停用"));
                        if (!authorizationService.holdsPermissionViaRole(hr, "staff.hr.review")) {
                            throw ApiException.badRequest("HR_APPROVER_LACKS_ROLE", "部门人力负责人须经角色持有人力会签权限");
                        }
                        requireUserScope(actor, reach, user);
                    }
                    SupervisorReviewRepository.ApprovalLine line =
                            new SupervisorReviewRepository.ApprovalLine(code, name, minStartRank, entry, status, hr);
                    if (previous.isEmpty()) {
                        requireLineScope(actor, reach, line);
                    }
                    Instant now = Instant.now();
                    reviewRepository.saveApprovalLine(line, actor.id(), now);
                    auditEventService.recordSuccess(
                            "access.approval_line.saved",
                            MANAGE_PERMISSION,
                            "approval_line",
                            code,
                            null,
                            request.idempotencyKey(),
                            Map.of("line_name", name, "min_start_rank", minStartRank, "entry_user_id", entry == null ? "" : entry,
                                    "hr_final_approver_id", hr == null ? "" : hr,
                                    "status", status)
                    );
                    return line;
                },
                SupervisorReviewRepository.ApprovalLine.class
        );
    }

    public List<SupervisorReviewRepository.ApprovalLine> listApprovalLines() {
        try {
            authorizationService.requirePermission(MANAGE_PERMISSION);
            AccessActor actor = authorizationService.currentActor();
            ProjectReach reach = authorizationService.projectReach(actor, MANAGE_PERMISSION);
            List<SupervisorReviewRepository.ApprovalLine> lines = reviewRepository.listApprovalLines().stream()
                    .filter(line -> canManageLine(actor, reach, line)).toList();
            auditEventService.recordSuccess("access.approval_line.listed", MANAGE_PERMISSION,
                    "approval_line", null, null, null, Map.of("count", lines.size()));
            return lines;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("access.approval_line.list_failed", MANAGE_PERMISSION,
                    "approval_line", null, null, null,
                    exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private void requireUserScope(AccessActor actor, ProjectReach reach, AccessUser user) {
        if (!authorizationService.canReachUser(reach, user)
                || (!blank(actor.merchantId()) && !actor.merchantId().equals(user.merchantId()))) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "账号不在可配置的组织范围内");
        }
    }

    private boolean canManageUser(AccessActor actor, ProjectReach reach, String userId) {
        return accessRepository.findUserById(userId)
                .map(user -> authorizationService.canReachUser(reach, user)
                        && (blank(actor.merchantId()) || actor.merchantId().equals(user.merchantId())))
                .orElse(false);
    }

    private boolean canManageLine(AccessActor actor, ProjectReach reach, SupervisorReviewRepository.ApprovalLine line) {
        var users = new LinkedHashSet<>(reviewRepository.findApprovalLineUserIds(line.lineCode()));
        if (!blank(line.hrFinalApproverId())) users.add(line.hrFinalApproverId());
        if (!blank(line.entryUserId())) users.add(line.entryUserId());
        // 无归属锚点的既有全局配置，不能用请求里新填的本项目 HR 接管。
        return users.isEmpty() ? reach.platformAdmin() || reach.allProjects()
                : users.stream().allMatch(user -> canManageUser(actor, reach, user));
    }

    private void requireLineScope(AccessActor actor, ProjectReach reach, SupervisorReviewRepository.ApprovalLine line) {
        if (!canManageLine(actor, reach, line)) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "审批线的关联人员不完全在可配置的组织范围内");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
