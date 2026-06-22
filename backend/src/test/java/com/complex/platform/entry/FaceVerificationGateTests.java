package com.complex.platform.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * P0-4 / X-02 人脸入场闸脚手架单测。
 *
 * <p>覆盖：开关默认关（alwaysDisabled / 无显式行 / 读取异常 fail-safe）→ RESERVED 非阻塞豁免；
 * 开关显式开但无厂商 → NOT_CONFIGURED 留接缝；结果语义 requiresExemptionFallback / isRejected。
 */
class FaceVerificationGateTests {

    private static final FaceVerificationRequest REQ =
            FaceVerificationRequest.of("cred_1", "gate_dev_1", "point_1", "entry_checkin");

    @Test
    void alwaysDisabledRouterReturnsReservedNonBlocking() {
        FaceVerificationResult result = FaceVerificationRouter.alwaysDisabled().verifyEntry(REQ);
        assertThat(result.status()).isEqualTo(FaceVerificationResult.Status.RESERVED);
        assertThat(result.requiresExemptionFallback()).isTrue();
        assertThat(result.isRejected()).isFalse();
    }

    @Test
    void noExplicitSwitchRowDefaultsToDisabled() {
        TerminalDeviceRepository repository = mock(TerminalDeviceRepository.class);
        when(repository.resolveEffectiveSwitch(eq("*"), eq(FaceVerificationRouter.CAPABILITY), any()))
                .thenReturn(Optional.empty());
        FaceVerificationRouter router = new FaceVerificationRouter(repository);
        assertThat(router.isGateEnabled()).isFalse();
        assertThat(router.verifyEntry(REQ).status()).isEqualTo(FaceVerificationResult.Status.RESERVED);
    }

    @Test
    void switchReadFailureFailsSafeToDisabled() {
        TerminalDeviceRepository repository = mock(TerminalDeviceRepository.class);
        when(repository.resolveEffectiveSwitch(eq("*"), eq(FaceVerificationRouter.CAPABILITY), any()))
                .thenThrow(new RuntimeException("db down"));
        FaceVerificationRouter router = new FaceVerificationRouter(repository);
        assertThat(router.isGateEnabled()).isFalse();
        assertThat(router.verifyEntry(REQ).requiresExemptionFallback()).isTrue();
    }

    @Test
    void enabledSwitchWithoutProviderReturnsNotConfigured() {
        TerminalDeviceRepository repository = mock(TerminalDeviceRepository.class);
        when(repository.resolveEffectiveSwitch(eq("*"), eq(FaceVerificationRouter.CAPABILITY), any()))
                .thenReturn(Optional.of(new TerminalCapabilitySwitchRecord(
                        "tcs_face_verification_on", "*", FaceVerificationRouter.CAPABILITY,
                        "global", "global", true, "enabled for test", Instant.now(), Instant.now())));
        FaceVerificationRouter router = new FaceVerificationRouter(repository);
        assertThat(router.isGateEnabled()).isTrue();
        FaceVerificationResult result = router.verifyEntry(REQ);
        assertThat(result.status()).isEqualTo(FaceVerificationResult.Status.NOT_CONFIGURED);
        // 开但无厂商：仍非阻塞（业务侧走豁免回退），但语义区别于"关"。
        assertThat(result.requiresExemptionFallback()).isTrue();
    }
}
