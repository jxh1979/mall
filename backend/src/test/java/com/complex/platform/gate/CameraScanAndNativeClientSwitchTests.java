package com.complex.platform.gate;

// 文件版本：V0.02

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
 * V097：摄像头扫码（#2）能力开关的 TerminalSwitchGuard 读取语义（#3 原生客户端开关行已由 V537 退役，ROOT-03-R33）。
 * 证明：显式 enabled=false 行 → 拒绝（TERMINAL_DISABLED，留痕 disabled）；无显式行 → 默认开启（放行）。
 * 与 V093 payment.gateway / V096 wecom.ai_qc 同一范式（默认关须显式 seed false 行）。
 */
class CameraScanAndNativeClientSwitchTests {

    private static final String CAMERA_SCAN = "terminal.camera_scan.enabled";

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
    void absentRowDefaultsClosed() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), any(), any())).thenReturn(Optional.empty());
        TerminalSwitchGuard guard =
                new TerminalSwitchGuard(repo, new AuditEventService(new InMemoryAuditEventRepository()));

        // PGM 第 742 行 opt-in：无显式行 = 默认关闭。
        assertThatThrownBy(() -> guard.check(CAMERA_SCAN))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
    }

    private static TerminalCapabilitySwitchRecord offRow(String id, String capability) {
        return new TerminalCapabilitySwitchRecord(
                id, "*", capability, "global", "global", false, "off", Instant.now(), Instant.now());
    }
}
