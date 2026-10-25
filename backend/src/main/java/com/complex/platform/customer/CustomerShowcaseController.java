package com.complex.platform.customer;

// 文件版本：V0.02

import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ROOT-03 clients-R9 客人端只读展示与首页内容（编辑后经审批发布），见 {@link CustomerShowcaseService}。 */
@RestController
public class CustomerShowcaseController {

    private final CustomerShowcaseService service;
    private final CustomerProjectSwitch projectSwitch;

    public CustomerShowcaseController(CustomerShowcaseService service, CustomerProjectSwitch projectSwitch) {
        this.service = service;
        this.projectSwitch = projectSwitch;
    }

    @GetMapping("/api/v1/customer/projects")
    public ApiResponse<List<CustomerShowcaseService.Project>> listProjects() {
        return ApiResponse.ok(service.listProjects());
    }

    @GetMapping("/api/v1/customer/amusement/projects")
    public ApiResponse<List<CustomerShowcaseService.AmusementProject>> listAmusementProjects(
            @RequestParam(name = "project_id", required = false) String projectId) {
        // PGM 第 744 行：所查的就是 project_id 所指综合体，按它判按综合体的客人端开关（匿名，终端类型自报）。
        projectSwitch.project(projectId);
        return ApiResponse.ok(service.listAmusementProjects(projectId));
    }

    @GetMapping("/api/v1/home-contents/{project_id}")
    public ApiResponse<CustomerShowcaseService.HomeContent> getHomeContent(@PathVariable("project_id") String projectId) {
        return ApiResponse.ok(service.getHomeContent(projectId));
    }

    @PostMapping("/api/v1/home-contents/{project_id}/publish")
    public ApiResponse<CustomerShowcaseService.HomeContent> publishHomeContent(@PathVariable("project_id") String projectId,
            @RequestBody CustomerShowcaseService.HomeContentRequest request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        // 裁决第 5 条：先存待审原文（独立提交，含编辑权限与项目范围校验），再过准入审批；被拦下送审时审批人可读到这份原文。
        service.saveDraft(projectId, request);
        return ApiResponse.ok(service.publishHomeContent(projectId, request));
    }
}
