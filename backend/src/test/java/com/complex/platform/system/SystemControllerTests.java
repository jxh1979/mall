package com.complex.platform.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import java.lang.reflect.RecordComponent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class SystemControllerTests {

    private static DataSource dataSourceWithValidConnection(boolean valid) throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(valid);
        return dataSource;
    }

    @Test
    void healthReturnsUpWhenDbReachableWithoutLeakingActiveProfiles() throws SQLException {
        SystemHealthController controller = new SystemHealthController(dataSourceWithValidConnection(true));

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
    void healthReturns503WhenDbConnectionInvalid() throws SQLException {
        SystemHealthController controller = new SystemHealthController(dataSourceWithValidConnection(false));

        assertThatThrownBy(controller::health)
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void healthReturns503WhenDbUnreachable() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));
        SystemHealthController controller = new SystemHealthController(dataSource);

        assertThatThrownBy(controller::health)
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
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
