// 文件版本：V0.01
package com.complex.platform.compliance;

import com.complex.platform.auth.MasterAuthorityService;
import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compliance/wecom/cleanup-previews")
public class WecomCleanupController {
    private final WecomCleanupService service;
    WecomCleanupController(WecomCleanupService service) { this.service = service; }

    @PostMapping
    public ApiResponse<WecomCleanupPreview> preview(@RequestBody WecomCleanupPreview.Request request) {
        return ApiResponse.ok(service.preview(request));
    }

    @GetMapping
    public ApiResponse<List<WecomCleanupPreview>> list() { return ApiResponse.ok(service.list()); }

    @GetMapping("/{id}")
    public ApiResponse<WecomCleanupPreview> get(@PathVariable String id) { return ApiResponse.ok(service.get(id)); }

    @PostMapping("/{id}/challenge")
    public ApiResponse<MasterAuthorityService.ActionChallenge> challenge(@PathVariable String id) {
        return ApiResponse.ok(service.challenge(id));
    }

    @PostMapping("/{id}/execute")
    public ApiResponse<WecomCleanupPreview> execute(@PathVariable String id,
            @RequestBody WecomCleanupPreview.ExecuteRequest request) {
        return ApiResponse.ok(service.execute(id, request));
    }
}
