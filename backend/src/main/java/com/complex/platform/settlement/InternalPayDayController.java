package com.complex.platform.settlement;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FS-021-R3：三个付账日、钱路指派规则（提交/审批）与非积分商户账单异议期（按项目）配置入口。 */
@RestController
@RequestMapping("/api/v1/settlement/internal-pay-days")
public class InternalPayDayController {

    private final InternalPayDayService service;

    public InternalPayDayController(InternalPayDayService service) {
        this.service = service;
    }

    @GetMapping("/rules")
    public ApiResponse<List<InternalPayDayService.Rule>> listRules() {
        return ApiResponse.ok(service.rules());
    }

    @PostMapping("/rules")
    public ApiResponse<InternalPayDayService.Rule> submitRule(@RequestBody InternalPayDayService.RuleRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submit(request));
    }

    @PostMapping("/rules/{rule_id}/decision")
    public ApiResponse<InternalPayDayService.Rule> decideRule(@PathVariable("rule_id") String ruleId,
                                                              @RequestBody InternalPayDayService.DecisionRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.decide(ruleId, request));
    }

    @GetMapping("/objection-period")
    public ApiResponse<InternalPayDayService.ObjectionPolicy> getObjectionPeriod(
            @RequestParam(value = "project_id", required = false) String projectId) {
        return ApiResponse.ok(service.getObjectionPolicy(projectId));
    }

    @PostMapping("/objection-period")
    public ApiResponse<InternalPayDayService.ObjectionPolicy> saveObjectionPeriod(
            @RequestBody InternalPayDayService.ObjectionPolicyRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.saveObjectionPolicy(request));
    }
}
