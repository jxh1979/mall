package com.complex.platform.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SystemControllerTests {

    @Test
    void healthReturnsUp() {
        SystemHealthController controller = new SystemHealthController(new MockEnvironment().withProperty("spring.profiles.active", "test"));

        ApiResponse<SystemHealthController.SystemHealthResponse> response = controller.health();

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.data().ok()).isTrue();
        assertThat(response.data().status()).isEqualTo("UP");
        assertThat(response.data().service()).isEqualTo("complex-api");
        assertThat(response.data().activeProfiles()).containsExactly("test");
        assertThat(response.data().checkedAt()).isNotNull();
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
