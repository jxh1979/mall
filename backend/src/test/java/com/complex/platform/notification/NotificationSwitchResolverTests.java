package com.complex.platform.notification;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * PGM 第 742 行「任何能力在后台没有显式开关配置行时一律默认关闭（opt-in）……开关缺行不得视为开启」：短信域开关、事件开关缺行即不发；
 * BSM 第 1598 行不可关闭的三类（MANDATORY_EVENTS）不看任何开关，缺行照常发。
 */
class NotificationSwitchResolverTests {

    private final TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
    private final NotificationSwitchResolver resolver = new NotificationSwitchResolver(repo);

    @Test
    void missingDomainRowBlocks() {
        when(repo.resolveEffectiveSwitch(eq("*"), anyString(), any())).thenReturn(Optional.empty());
        on("sms_notification");

        NotificationSwitchDecision decision = resolver.resolve("ticketing", "ticket_transfer_created");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("switch_off:sms.notify.ticketing");
    }

    @Test
    void missingEventRowBlocks() {
        when(repo.resolveEffectiveSwitch(eq("*"), anyString(), any())).thenReturn(Optional.empty());
        on("sms_notification");
        on("sms.notify.ticketing");

        NotificationSwitchDecision decision = resolver.resolve("ticketing", "ticket_transfer_created");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("switch_off:sms.notify.ticketing.ticket_transfer_created");
    }

    @Test
    void mandatoryEventWithoutAnyRowStillAllowed() {
        when(repo.resolveEffectiveSwitch(eq("*"), anyString(), any())).thenReturn(Optional.empty());

        assertThat(resolver.resolve("stored_value", "topup").allowed()).isTrue();
    }

    private void on(String capabilityCode) {
        when(repo.resolveEffectiveSwitch(eq("*"), eq(capabilityCode), any())).thenReturn(Optional.of(
                new TerminalCapabilitySwitchRecord("tcs_" + capabilityCode, "*", capabilityCode, "global", "global",
                        true, "test", Instant.now(), Instant.now())));
    }
}
