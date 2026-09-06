package com.complex.platform.governance.review;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.governance.chain.ApprovalChainNode;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
public class SupervisorReviewController {

    private final SupervisorReviewService service;
    private final AuthorizationService authorizationService;

    public SupervisorReviewController(SupervisorReviewService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/supervisor-review-policies")
    public ApiResponse<PageResponse<SupervisorReviewPolicyRecord>> listPolicies() {
        authorizationService.requirePermission("governance.supervisor_review.view");
        return ApiResponse.ok(service.listPolicies());
    }

    @PostMapping("/supervisor-review/evaluate")
    public ApiResponse<SupervisorReviewDecisionRecord> evaluate(@RequestBody @jakarta.validation.Valid SupervisorReviewEvaluateRequest request) {
        authorizationService.requirePermission("governance.supervisor_review.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.evaluate(request));
    }

    @PostMapping("/supervisor-review/{review_id}/approve")
    public ApiResponse<SupervisorReviewDecisionRecord> approve(
            @PathVariable("review_id") String reviewId,
            @RequestBody @jakarta.validation.Valid SupervisorReviewApprovalRequest request
    ) {
        authorizationService.requirePermission("governance.supervisor_review.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approve(reviewId, request));
    }

    // M007-A-02（PGM-1.7.1）：待复核清单（到期/升级/转发状态）与逐单转发。
    @GetMapping("/supervisor-review/pending")
    public ApiResponse<PageResponse<SupervisorReviewRepository.PendingReview>> listPending() {
        authorizationService.requirePermission("governance.supervisor_review.view");
        return ApiResponse.ok(service.listPending(200));
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
