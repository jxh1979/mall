package com.complex.platform.system;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import java.sql.Connection;
import java.time.Instant;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    private final DataSource dataSource;

    public SystemHealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ApiResponse<SystemHealthResponse> health() {
        // Public (permitAll) readiness probe consumed by deploy/rollback wait_for_health + external
        // monitors. It MUST reflect real dependency health: a process that cannot reach the DB is NOT
        // healthy. Returning UP unconditionally let deploy/rollback declare success on a broken release
        // (AUD-036) because wait_for_health falls back to this endpoint when actuator/health 503s. On DB
        // failure we fail with 503 so curl -fsS treats the release as unhealthy.
        // Still must NOT leak deployment internals (e.g. active Spring profiles) to anonymous callers.
        boolean dbUp;
        try (Connection connection = dataSource.getConnection()) {
            dbUp = connection.isValid(2);
        } catch (Exception e) {
            dbUp = false;
        }
        if (!dbUp) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DB_UNAVAILABLE", "database not reachable");
        }
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
