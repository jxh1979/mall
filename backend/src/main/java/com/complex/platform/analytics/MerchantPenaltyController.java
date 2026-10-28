package com.complex.platform.analytics;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 第十八批 C 段第 62 条 商户处罚记录与按项目单次扣分上下限（见 {@link MerchantPenaltyService}）。 */
@RestController
public class MerchantPenaltyController {

    private final MerchantPenaltyService service;

    public MerchantPenaltyController(MerchantPenaltyService service) {
        this.service = service;
    }

    @PutMapping("/api/v1/merchant-penalty-bounds/{project_id}")
    public ApiResponse<MerchantPenaltyService.Bounds> saveBounds(@PathVariable("project_id") String projectId,
            @RequestBody MerchantPenaltyService.BoundsRequest request) {
        return ApiResponse.ok(service.saveBounds(projectId, request));
    }

    @PostMapping("/api/v1/merchant-penalties")
    public ApiResponse<MerchantPenaltyService.Penalty> record(@RequestBody MerchantPenaltyService.PenaltyRequest request) {
        return ApiResponse.ok(service.record(request));
    }

    @GetMapping("/api/v1/merchant-penalties")
    public ApiResponse<MerchantPenaltyService.PenaltyList> list(@RequestParam("project_id") String projectId,
            @RequestParam(value = "merchant_id", required = false) String merchantId) {
        return ApiResponse.ok(service.list(projectId, merchantId));
    }
}
