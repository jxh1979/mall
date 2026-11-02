package com.complex.platform.complaint;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.complaint.ComplaintFollowupService.ComplaintFollowupRecord;
import com.complex.platform.complaint.ComplaintFollowupService.ComplaintFollowupUpdateRequest;
import com.complex.platform.complaint.ComplaintFollowupService.ComplaintLoopConfirmRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第十八批 M 段第 111 条 111-R3 客诉跟进（BSM:1385「客诉须经客户关怀部确认后方可视为形成闭环」）。权限按链判（物业工单链 facility.work_order.manage、
 * 商户服务工单链 merchant.service_work_order.close，即该链员工关单的权限），在服务层链闸里查。
 */
@RestController
@RequestMapping("/api/v1/complaint-followups")
public class ComplaintFollowupController {

    private final ComplaintFollowupService service;

    public ComplaintFollowupController(ComplaintFollowupService service) {
        this.service = service;
    }

    @GetMapping("/{chain}/{work_order_id}")
    public ApiResponse<ComplaintFollowupRecord> get(@PathVariable String chain, @PathVariable("work_order_id") String workOrderId) {
        return ApiResponse.ok(service.find(chain, workOrderId));
    }

    /** 118-R1 认定投诉类别、111-R4 更正涉及门店（只物业工单链；第十八批裁决 28「客人投诉时自己选涉及的门店（选填），客户关怀部可以更正」）。 */
    @PutMapping("/{chain}/{work_order_id}")
    public ApiResponse<ComplaintFollowupRecord> update(@PathVariable String chain, @PathVariable("work_order_id") String workOrderId,
            @RequestBody ComplaintFollowupUpdateRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.update(chain, workOrderId, request));
    }

    @PostMapping("/{chain}/{work_order_id}/loop-confirm")
    public ApiResponse<ComplaintFollowupRecord> confirmLoop(@PathVariable String chain, @PathVariable("work_order_id") String workOrderId,
            @RequestBody ComplaintLoopConfirmRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmLoop(chain, workOrderId, request));
    }
}
