package com.complex.platform.system;

import com.complex.platform.common.api.ApiResponse;
import java.time.Instant;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    private final Environment environment;

    public SystemHealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/health")
    public ApiResponse<SystemHealthResponse> health() {
        return ApiResponse.ok(new SystemHealthResponse(
                true,
                "UP",
                "complex-api",
                environment.getActiveProfiles(),
                Instant.now()
        ));
    }

    public record SystemHealthResponse(
            boolean ok,
            String status,
            String service,
            String[] activeProfiles,
            Instant checkedAt
    ) {
    }
}
