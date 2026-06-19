package com.complex.platform.compliance.qc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WecomQcRouterTests {

    private static TerminalCapabilitySwitchRecord row(boolean enabled) {
        return new TerminalCapabilitySwitchRecord("tcs_wecom_ai_qc_off", "*",
                WecomQcRouter.CAPABILITY, "global", "global", enabled, "test",
                Instant.now(), Instant.now());
    }

    private static WecomQcRequest req() {
        return new WecomQcRequest("conv-1", "加我私微", List.of("lure_private_contact"));
    }

    @Test
    void switchOff_routesToDisabled_returnsSkipped_noAiCall() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(eq("*"), eq(WecomQcRouter.CAPABILITY), anyMap()))
                .thenReturn(Optional.of(row(false)));
        WecomQcRouter router = new WecomQcRouter(repo);

        assertThat(router.isAiQcEnabled()).isFalse();
        assertThat(router.analyzeMessage(req()).status()).isEqualTo(WecomQcResult.Status.SKIPPED);
        assertThat(router.analyzeConversation(List.of(req(), req())))
                .allSatisfy(r -> assertThat(r.status()).isEqualTo(WecomQcResult.Status.SKIPPED))
                .hasSize(2);
    }

    @Test
    void switchOn_routesToReservedSeam_returnsNotConfigured_doesNotCrash() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(eq("*"), eq(WecomQcRouter.CAPABILITY), anyMap()))
                .thenReturn(Optional.of(row(true)));
        WecomQcRouter router = new WecomQcRouter(repo);

        assertThat(router.isAiQcEnabled()).isTrue();
        assertThat(router.analyzeMessage(req()).status()).isEqualTo(WecomQcResult.Status.NOT_CONFIGURED);
    }

    @Test
    void noExplicitRow_treatedAsDisabled_failSafe() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), any(), anyMap())).thenReturn(Optional.empty());
        WecomQcRouter router = new WecomQcRouter(repo);

        assertThat(router.isAiQcEnabled()).isFalse();
        assertThat(router.analyzeMessage(req()).status()).isEqualTo(WecomQcResult.Status.SKIPPED);
    }

    @Test
    void switchReadFailure_failSafeToDisabled() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), any(), anyMap()))
                .thenThrow(new RuntimeException("boom"));
        WecomQcRouter router = new WecomQcRouter(repo);

        assertThat(router.isAiQcEnabled()).isFalse();
        assertThat(router.analyzeMessage(req()).status()).isEqualTo(WecomQcResult.Status.SKIPPED);
    }

    @Test
    void alwaysDisabledFactory_neverInvokesAi() {
        WecomQcRouter router = WecomQcRouter.alwaysDisabled();
        assertThat(router.isAiQcEnabled()).isFalse();
        assertThat(router.analyzeMessage(req()).status()).isEqualTo(WecomQcResult.Status.SKIPPED);
    }
}
