package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * V097：摄像头扫码（#2）+ 微信小程序/原生 App 客户端（#3）能力开关的 TerminalSwitchGuard 读取语义。
 * 证明：显式 enabled=false 行 → 拒绝（TERMINAL_DISABLED，留痕 disabled）；无显式行 → 默认开启（放行）。
 * 与 V093 payment.gateway / V096 wecom.ai_qc 同一范式（默认关须显式 seed false 行）。
 */
class CameraScanAndNativeClientSwitchTests {

    private static final String CAMERA_SCAN = "terminal.camera_scan.enabled";
    private static final String NATIVE_CLIENT = "customer.native_client.enabled";

    @Test
    void explicitFalseRowRejectsCameraScan() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq(CAMERA_SCAN), any()))
                .thenReturn(Optional.of(offRow("tcs_camera_scan_off", CAMERA_SCAN)));
        InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
        TerminalSwitchGuard guard = new TerminalSwitchGuard(repo, new AuditEventService(auditRepo));

        assertThatThrownBy(() -> guard.check(CAMERA_SCAN))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(auditRepo.events)
                .filteredOn(e -> "terminal.access_rejected".equals(e.eventName()))
                .first()
                .satisfies(e -> assertThat(e.metadata()).containsEntry("reason", "disabled"));
    }

    @Test
    void explicitFalseRowRejectsNativeClient() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq(NATIVE_CLIENT), any()))
                .thenReturn(Optional.of(offRow("tcs_native_client_off", NATIVE_CLIENT)));
        TerminalSwitchGuard guard =
                new TerminalSwitchGuard(repo, new AuditEventService(new InMemoryAuditEventRepository()));

        assertThatThrownBy(() -> guard.check(NATIVE_CLIENT))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
    }

    @Test
    void absentRowDefaultsOpenForBothCapabilities() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), any(), any())).thenReturn(Optional.empty());
        TerminalSwitchGuard guard =
                new TerminalSwitchGuard(repo, new AuditEventService(new InMemoryAuditEventRepository()));

        // 无显式行 = 默认开启（opt-out 语义），不抛异常。
        assertThatCode(() -> guard.check(CAMERA_SCAN)).doesNotThrowAnyException();
        assertThatCode(() -> guard.check(NATIVE_CLIENT)).doesNotThrowAnyException();
    }

    private static TerminalCapabilitySwitchRecord offRow(String id, String capability) {
        return new TerminalCapabilitySwitchRecord(
                id, "*", capability, "global", "global", false, "off", Instant.now(), Instant.now());
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
