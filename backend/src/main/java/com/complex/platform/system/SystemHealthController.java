package com.complex.platform.system;

import com.complex.platform.common.api.ApiResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    @GetMapping("/health")
    public ApiResponse<SystemHealthResponse> health() {
        // This endpoint is public (permitAll) for liveness probes/monitoring; it must NOT leak
        // deployment internals such as active Spring profiles to anonymous callers.
        return ApiResponse.ok(new SystemHealthResponse(
                true,
                "UP",
                "complex-api",
                Instant.now()
        ));
    }

    public record SystemHealthResponse(
            boolean ok,
            String status,
            String service,
            Instant checkedAt
    ) {
    }
}
