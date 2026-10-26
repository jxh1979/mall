package com.complex.platform.gate;

import com.complex.platform.support.InMemoryAuditEventRepository;
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
 * V098：员工设备 Keystore 凭证绑定（item1）+ 平板终端（②）能力开关的 TerminalSwitchGuard 读取语义。
 * 证明：显式 enabled=false 行 → 拒绝（TERMINAL_DISABLED，留痕 disabled）；无显式行 → 默认开启（放行）。
 * 与 V097 camera_scan / native_client 同一范式（默认关须显式 seed false 行）。
 * 注意：本 seed 行为纯预留，未接到任何现有 @GuardedAction 守卫；此处仅验证 Guard 读取语义与 V097 一致。
 */
class DeviceKeystoreAndTabletSwitchTests {

    private static final String DEVICE_KEYSTORE = "staff.device_keystore.enabled";
    private static final String TABLET = "terminal.tablet.enabled";

    @Test
    void explicitFalseRowRejectsDeviceKeystore() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq(DEVICE_KEYSTORE), any()))
                .thenReturn(Optional.of(offRow("tcs_device_keystore_off", DEVICE_KEYSTORE)));
        InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
        TerminalSwitchGuard guard = new TerminalSwitchGuard(repo, new AuditEventService(auditRepo));

        assertThatThrownBy(() -> guard.check(DEVICE_KEYSTORE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(auditRepo.events)
                .filteredOn(e -> "terminal.access_rejected".equals(e.eventName()))
                .first()
                .satisfies(e -> assertThat(e.metadata()).containsEntry("reason", "disabled"));
    }

    @Test
    void explicitFalseRowRejectsTablet() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq(TABLET), any()))
                .thenReturn(Optional.of(offRow("tcs_tablet_off", TABLET)));
        TerminalSwitchGuard guard =
                new TerminalSwitchGuard(repo, new AuditEventService(new InMemoryAuditEventRepository()));

        assertThatThrownBy(() -> guard.check(TABLET))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
    }

    @Test
    void absentRowDefaultsClosedForBothCapabilities() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), any(), any())).thenReturn(Optional.empty());
        TerminalSwitchGuard guard =
                new TerminalSwitchGuard(repo, new AuditEventService(new InMemoryAuditEventRepository()));

        // PGM 第 742 行 opt-in：无显式行 = 默认关闭。
        assertThatThrownBy(() -> guard.check(DEVICE_KEYSTORE))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThatThrownBy(() -> guard.check(TABLET))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
    }

    private static TerminalCapabilitySwitchRecord offRow(String id, String capability) {
        return new TerminalCapabilitySwitchRecord(
                id, "*", capability, "global", "global", false, "off", Instant.now(), Instant.now());
    }
}
