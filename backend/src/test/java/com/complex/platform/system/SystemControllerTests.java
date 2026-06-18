package com.complex.platform.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.api.ApiResponse;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SystemControllerTests {

    @Test
    void healthReturnsUpWithoutLeakingActiveProfiles() {
        SystemHealthController controller = new SystemHealthController();

        ApiResponse<SystemHealthController.SystemHealthResponse> response = controller.health();

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.data().ok()).isTrue();
        assertThat(response.data().status()).isEqualTo("UP");
        assertThat(response.data().service()).isEqualTo("complex-api");
        assertThat(response.data().checkedAt()).isNotNull();
        // The public health payload must not expose deployment internals (e.g. active Spring profiles).
        assertThat(Arrays.stream(SystemHealthController.SystemHealthResponse.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("activeProfiles");
    }

    @Test
    void versionReturnsServiceName() {
        SystemVersionController controller = new SystemVersionController("complex-api", "0.1.0", "local");

        ApiResponse<SystemVersionController.SystemVersionResponse> response = controller.version();

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.data().name()).isEqualTo("complex-api");
        assertThat(response.data().version()).isEqualTo("0.1.0");
        assertThat(response.data().buildTime()).isEqualTo("local");
    }
}
