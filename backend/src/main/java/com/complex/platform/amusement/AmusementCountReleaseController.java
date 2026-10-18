package com.complex.platform.amusement;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AmusementCountReleaseController {
    private final AmusementSessionLifecycleService service;
    public AmusementCountReleaseController(AmusementSessionLifecycleService service){this.service=service;}
    @GetMapping("/api/v1/amusement/count-releases")
    public ApiResponse<List<Map<String,Object>>> releases(@RequestParam(required=false) String projectId) {
        return ApiResponse.ok(service.releases(projectId));
    }
}
