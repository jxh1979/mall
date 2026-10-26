package com.complex.platform.gate;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * PGM 第 742 行「任何能力在后台没有显式开关配置行时一律默认关闭（opt-in），要启用必须有人显式打开；开关缺行不得视为开启」：
 * 终端能力开关闸查不到任何显式行即拒绝（reason=not_configured），与读取异常的「兜底关闭」（switch_fetch_failed）分开留痕。
 */
class TerminalSwitchOptInTests {

    @Test
    void missingSwitchRowRejectsWithNotConfiguredAudit() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq("bar.order.create"), any())).thenReturn(Optional.empty());
        InMemoryAuditEventRepository audit = new InMemoryAuditEventRepository();
        TerminalSwitchGuard guard = new TerminalSwitchGuard(repo, new AuditEventService(audit));

        assertThatThrownBy(() -> guard.check("bar.order.create"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(audit.events)
                .filteredOn(e -> "terminal.access_rejected".equals(e.eventName()))
                .singleElement()
                .satisfies(e -> assertThat(e.metadata())
                        .containsEntry("capability", "bar.order.create")
                        .containsEntry("reason", "not_configured"));
    }

    @Test
    void missingMasterRowRejectsTerminal() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq(TerminalSwitchGuard.MASTER_CAPABILITY), any())).thenReturn(Optional.empty());
        InMemoryAuditEventRepository audit = new InMemoryAuditEventRepository();
        TerminalSwitchGuard guard = new TerminalSwitchGuard(repo, new AuditEventService(audit));

        assertThatThrownBy(guard::checkTerminalEnabled)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(audit.events)
                .filteredOn(e -> "terminal.access_rejected".equals(e.eventName()))
                .singleElement()
                .satisfies(e -> assertThat(e.metadata()).containsEntry("reason", "not_configured"));
    }

    @Test
    void explicitEnabledRowPasses() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq("bar.order.create"), any())).thenReturn(Optional.of(
                new TerminalCapabilitySwitchRecord("tcs_optin_bar.order.create", "*", "bar.order.create", "global", "global",
                        true, "test", Instant.now(), Instant.now())));
        TerminalSwitchGuard guard = new TerminalSwitchGuard(repo, new AuditEventService(new InMemoryAuditEventRepository()));

        assertThatCode(() -> guard.check("bar.order.create")).doesNotThrowAnyException();
    }
}
