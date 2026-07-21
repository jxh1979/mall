package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.terminal.TerminalDeviceRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** AUD-010: 终端类型经注册设备表服务端派生 + 矛盾/停用设备 fail-closed。 */
class TerminalIdentityResolverTests {

    private final TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
    private final TerminalIdentityResolver resolver = new TerminalIdentityResolver(repo);

    private static TerminalDeviceRecord device(String deviceId, String terminalType, String status) {
        Instant now = Instant.parse("2026-07-01T00:00:00Z");
        return new TerminalDeviceRecord(
                "dev_" + deviceId, deviceId, "code_" + deviceId, terminalType, "device", null, null, null,
                "merchant_1", null, null, null, null, status, now, null, null, null, null, now, now);
    }

    @Test
    void noDeviceIdReturnsClaimUnchanged() {
        assertThat(resolver.resolveTrustedTerminalType("staff_work_phone", null)).isEqualTo("staff_work_phone");
        assertThat(resolver.resolveTrustedTerminalType("staff_work_phone", "  ")).isEqualTo("staff_work_phone");
    }

    @Test
    void unknownDeviceFallsBackToClaim() {
        when(repo.findByDeviceId("dev-x")).thenReturn(Optional.empty());
        assertThat(resolver.resolveTrustedTerminalType("staff_work_phone", "dev-x")).isEqualTo("staff_work_phone");
    }

    @Test
    void registeredBoundDeviceTypeIsAuthoritative() {
        when(repo.findByDeviceId("dev-1")).thenReturn(Optional.of(device("dev-1", "staff_work_phone", "bound")));
        assertThat(resolver.resolveTrustedTerminalType("staff_work_phone", "dev-1")).isEqualTo("staff_work_phone");
    }

    @Test
    void mismatchedClaimIsRejected() {
        when(repo.findByDeviceId("dev-2")).thenReturn(Optional.of(device("dev-2", "kiosk", "bound")));
        assertThatThrownBy(() -> resolver.resolveTrustedTerminalType("staff_work_phone", "dev-2"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_DEVICE_MISMATCH"));
    }

    @Test
    void unboundDeviceIsRejected() {
        when(repo.findByDeviceId("dev-3")).thenReturn(Optional.of(device("dev-3", "staff_work_phone", "returned")));
        assertThatThrownBy(() -> resolver.resolveTrustedTerminalType("staff_work_phone", "dev-3"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_DEVICE_NOT_BOUND"));
    }
}
