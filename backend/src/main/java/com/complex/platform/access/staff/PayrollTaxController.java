package com.complex.platform.access.staff;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台税表和认定操作；审批继续使用全平台现有审批工作台。 */
@RestController
@RequestMapping("/api/v1/staff/payroll")
public class PayrollTaxController {
    private final PayrollTaxService service;
    public PayrollTaxController(PayrollTaxService service){this.service=service;}

    @GetMapping("/tax-versions")
    public ApiResponse<List<PayrollTaxVersion>> versions(@RequestParam(required=false) String category){return ApiResponse.ok(service.versions(category));}
    @PostMapping("/tax-versions")
    public ApiResponse<PayrollTaxVersion> draft(@RequestBody PayrollTaxVersion.DraftRequest request){return ApiResponse.ok(service.draft(request));}
    @PostMapping("/tax-versions/{id}/submit")
    public ApiResponse<PayrollTaxVersion> submitVersion(@PathVariable String id,@RequestBody PayrollTaxVersion.SubmitRequest request){return ApiResponse.ok(service.submitVersion(id,request));}

    @GetMapping("/tax-assessments")
    public ApiResponse<List<PayrollTaxAssessment>> assessments(@RequestParam("user_id") String userId,@RequestParam String month){return ApiResponse.ok(service.assessments(userId,month));}
    @PostMapping("/tax-assessments")
    public ApiResponse<PayrollTaxAssessment> draftAssessment(@RequestBody PayrollTaxAssessment.DraftRequest request){return ApiResponse.ok(service.draftAssessment(request));}
    @PostMapping("/tax-assessments/{id}/submit")
    public ApiResponse<PayrollTaxAssessment> submitAssessment(@PathVariable String id,@RequestBody PayrollTaxVersion.SubmitRequest request){return ApiResponse.ok(service.submitAssessment(id,request));}
}
