package com.complex.platform.system;

import com.complex.platform.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemVersionController {

    private final String name;
    private final String version;
    private final String buildTime;

    public SystemVersionController(
            @Value("${complex.app.name:complex-api}") String name,
            @Value("${complex.app.version:0.1.0}") String version,
            @Value("${complex.app.build-time:local}") String buildTime
    ) {
        this.name = name;
        this.version = version;
        this.buildTime = buildTime;
    }

    @GetMapping("/version")
    public ApiResponse<SystemVersionResponse> version() {
        return ApiResponse.ok(new SystemVersionResponse(name, version, buildTime));
    }

    public record SystemVersionResponse(
            String name,
            String version,
            String buildTime
    ) {
    }
}
