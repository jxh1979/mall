package com.complex.platform.finance.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentGatewayRouterTests {

    private static TerminalCapabilitySwitchRecord row(boolean enabled) {
        return new TerminalCapabilitySwitchRecord("tcs_payment_gateway_off", "*",
                PaymentGatewayRouter.CAPABILITY, "global", "global", enabled, "test",
                Instant.now(), Instant.now());
    }

    @Test
    void switchOff_routesToDisabled_returnsReserved_noExternalCall() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(eq("*"), eq(PaymentGatewayRouter.CAPABILITY), anyMap()))
                .thenReturn(Optional.of(row(false)));
        PaymentGatewayRouter router = new PaymentGatewayRouter(repo);

        assertThat(router.isGatewayEnabled()).isFalse();
        PaymentGatewayResult r = router.initiatePayment(PaymentGatewayRequest.of("t", "id", 100L, "wechat"));
        assertThat(r.status()).isEqualTo(PaymentGatewayResult.Status.RESERVED);
        assertThat(r.requiresInternalFallback()).isTrue();
        assertThat(router.refund(PaymentGatewayRequest.of("t", "id", 100L, null)).status())
                .isEqualTo(PaymentGatewayResult.Status.RESERVED);
    }

    @Test
    void switchOn_routesToReservedSeam_returnsNotConfigured_doesNotCrash() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(eq("*"), eq(PaymentGatewayRouter.CAPABILITY), anyMap()))
                .thenReturn(Optional.of(row(true)));
        PaymentGatewayRouter router = new PaymentGatewayRouter(repo);

        assertThat(router.isGatewayEnabled()).isTrue();
        PaymentGatewayResult r = router.initiatePayment(PaymentGatewayRequest.of("t", "id", 100L, "wechat"));
        assertThat(r.status()).isEqualTo(PaymentGatewayResult.Status.NOT_CONFIGURED);
        assertThat(r.requiresInternalFallback()).isTrue();
        assertThat(router.refund(PaymentGatewayRequest.of("t", "id", 100L, null)).status())
                .isEqualTo(PaymentGatewayResult.Status.NOT_CONFIGURED);
    }

    @Test
    void noExplicitRow_treatedAsDisabled_failSafe() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), any(), anyMap())).thenReturn(Optional.empty());
        PaymentGatewayRouter router = new PaymentGatewayRouter(repo);

        assertThat(router.isGatewayEnabled()).isFalse();
        assertThat(router.initiatePayment(PaymentGatewayRequest.of("t", "id", 100L, null)).status())
                .isEqualTo(PaymentGatewayResult.Status.RESERVED);
    }

    @Test
    void switchReadFailure_failSafeToDisabled() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), any(), anyMap()))
                .thenThrow(new RuntimeException("boom"));
        PaymentGatewayRouter router = new PaymentGatewayRouter(repo);

        assertThat(router.isGatewayEnabled()).isFalse();
        assertThat(router.refund(PaymentGatewayRequest.of("t", "id", 100L, null)).status())
                .isEqualTo(PaymentGatewayResult.Status.RESERVED);
    }

    @Test
    void alwaysDisabledFactory_neverHitsGateway() {
        PaymentGatewayRouter router = PaymentGatewayRouter.alwaysDisabled();
        assertThat(router.isGatewayEnabled()).isFalse();
        assertThat(router.initiatePayment(PaymentGatewayRequest.of("t", "id", 100L, null)).status())
                .isEqualTo(PaymentGatewayResult.Status.RESERVED);
    }
}
