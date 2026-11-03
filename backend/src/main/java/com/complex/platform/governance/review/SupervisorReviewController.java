package com.complex.platform.governance.review;
// 文件版本：V0.03
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AccessService;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.dto.AccessGrantReviewRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.governance.chain.ApprovalChainNode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
public class SupervisorReviewController {

    private static final String GRANT_REQUEST_OBJECT_TYPE = "access_grant_request";

    private final SupervisorReviewService service;
    private final AuthorizationService authorizationService;
    private final AccessService accessService;
    private final AuditedOperationExecutor operations;

    // 第十八批 N 段第 124 条 124-R5（PGM L738「访问被拒绝也要留痕，便于排查是权限问题还是开关问题。」）：写口任一步失败（权限、范围、校验、
    // 状态冲突）经 AuditedOperationExecutor.recordingFailure 落 OpenAPI 声明的 x-audit-event-failure 再原样抛出。
    public SupervisorReviewController(SupervisorReviewService service, AuthorizationService authorizationService, AccessService accessService,
            AuditedOperationExecutor operations) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
        this.operations = operations;
    }

    @GetMapping("/supervisor-review-policies")
    public ApiResponse<PageResponse<SupervisorReviewPolicyRecord>> listPolicies() {
        authorizationService.requirePermission("governance.supervisor_review.view");
        return ApiResponse.ok(service.listPolicies());
    }

    @PostMapping("/supervisor-review/evaluate")
    public ApiResponse<SupervisorReviewDecisionRecord> evaluate(@RequestBody @jakarta.validation.Valid SupervisorReviewEvaluateRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("governance.supervisor_review.evaluate_failed",
                "governance.supervisor_review.manage", "supervisor_review", "new", key, () -> {
                    authorizationService.requirePermission("governance.supervisor_review.manage");
                    requireIdempotencyKey(key);
                    return service.evaluate(request);
                }));
    }

    @PostMapping("/supervisor-review/{review_id}/approve")
    public ApiResponse<SupervisorReviewDecisionRecord> approve(
            @PathVariable("review_id") String reviewId,
            @RequestBody @jakarta.validation.Valid SupervisorReviewApprovalRequest request
    ) {
        // 第十八批 N 段第 124 条 124-R5：整段（两支权限判定与幂等键）放进 recordingFailure，任一步失败落 governance.supervisor_review.decide_failed；
        // 授权申请分支 AccessService.reviewGrant 另落的 access.grant_request.review_failed 照写不动（该分支失败会多一条操作级失败事件）。
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("governance.supervisor_review.decide_failed",
                "governance.supervisor_review.approve", "supervisor_review", reviewId, key, () -> {
            // M002-F40：保持原有拒绝顺序——不持主管复核审批权限者在读复核单之前即拒（PERMISSION_DENIED），不暴露复核单是否存在 / 是否在范围内；
            // 这里只做不消耗来源的判定，真正的核验与单次来源消耗在下方按复核单类别一次完成。
            if (!authorizationService.currentActorHasPermission("governance.supervisor_review.approve")) {
                authorizationService.requirePermission("governance.supervisor_review.approve"); // 无此权限：照原顺序在读复核单之前拒绝，不消耗任何来源
            }
            // M087-I5-01（PGM-1.10.1「审批通过后方可生效」、PGM-1.7.1「任一节点驳回则整单退回起草人」）：授权申请类复核单的节点裁决一律汇入
            // AccessService.reviewGrant——同一事务内推进节点、归约整单终态、落地或驳回授权申请；审批链页与授权申请页不再分叉。
            // 第十八批 E 段 19-R1（移植 M738）：通过与驳回前的读取按 governance.supervisor_review.approve 可触及的项目判发起人归属
            Optional<SupervisorReviewDecisionRecord> grantReview = service.findReview(reviewId, "governance.supervisor_review.approve")
                    .filter(review -> GRANT_REQUEST_OBJECT_TYPE.equals(review.objectType()) && "pending_review".equals(review.reviewStatus()));
            if (grantReview.isPresent()) {
                // M002-F40（治理地图第 655 行「显式拒绝优先级最高」，R-001「显式拒绝仍优先」）：授权申请的审批与授权申请页（AccessController.reviewGrant）
                // 同一门禁——两码一次核验，显式拒绝压过全部来源（含最高权限会话），同一单次来源只消耗一次。
                authorizationService.requirePermissions("access.grant_request.review", "governance.supervisor_review.approve");
            } else {
                authorizationService.requirePermission("governance.supervisor_review.approve");
            }
            requireIdempotencyKey(key);
            if (grantReview.isPresent()) {
                String decision = request.decision() == null ? "" : request.decision().trim().toLowerCase(Locale.ROOT);
                accessService.reviewGrant(grantReview.get().objectId(), new AccessGrantReviewRequest(
                        request.idempotencyKey(), request.operatorNote(), request.evidenceAttachmentIds(),
                        "approved".equals(decision) ? "approve" : "rejected".equals(decision) ? "reject" : decision, request.comment()));
                return service.findReview(reviewId, "governance.supervisor_review.approve")
                        .orElseThrow(() -> ApiException.notFound("SUPERVISOR_REVIEW_NOT_FOUND", "主管复核单不存在"));
            }
            return service.approve(reviewId, request);
        }));
    }

    // M007-A-02（PGM-1.7.1）：待复核清单（到期/升级/转发状态）与逐单转发。
    @GetMapping("/supervisor-review/pending")
    public ApiResponse<PageResponse<SupervisorReviewRepository.PendingReview>> listPending() {
        authorizationService.requirePermission("governance.supervisor_review.view");
        return ApiResponse.ok(service.listPending(200));
    }

    @GetMapping("/supervisor-review/{review_id}")
    public ApiResponse<SupervisorReviewDecisionRecord> readReview(@PathVariable("review_id") String reviewId) {
        return ApiResponse.ok(service.readReview(reviewId));
    }

    @PostMapping("/supervisor-review/{review_id}/forward")
    public ApiResponse<SupervisorReviewRepository.ReviewSchedule> forward(
            @PathVariable("review_id") String reviewId,
            @RequestBody @jakarta.validation.Valid SupervisorReviewForwardRequest request
    ) {
        authorizationService.requirePermission("governance.supervisor_review.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.forward(reviewId, request));
    }

    // M087-A-02：审批链节点（主线 + 平行线）查看；组织层级补齐后重生成未生成的链（manage 门禁）。
    @GetMapping("/supervisor-review/{review_id}/chain")
    public ApiResponse<List<ApprovalChainNode>> chain(@PathVariable("review_id") String reviewId) {
        authorizationService.requirePermission("governance.supervisor_review.view");
        return ApiResponse.ok(service.chain(reviewId));
    }

    @PostMapping("/supervisor-review/{review_id}/chain/regenerate")
    public ApiResponse<List<ApprovalChainNode>> regenerateChain(@PathVariable("review_id") String reviewId) {
        authorizationService.requirePermission("governance.supervisor_review.manage");
        return ApiResponse.ok(service.regenerateChain(reviewId));
    }
}
