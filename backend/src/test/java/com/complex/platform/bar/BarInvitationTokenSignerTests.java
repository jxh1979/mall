package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BarInvitationTokenSignerTests {

    @Test
    void prodProfileRejectsDefaultSigningSecret() {
        assertThatThrownBy(() -> new BarInvitationTokenSigner(
                BarInvitationTokenSigner.DEV_SIGNING_SECRET, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLEX_BAR_INVITATION_SIGNING_SECRET");
    }

    @Test
    void prodProfileAcceptsStrongNonDefaultSecret() {
        BarInvitationTokenSigner signer = new BarInvitationTokenSigner(
                "prod-test-bar-invitation-signing-secret-32-bytes-minimum", true);
        String canonical = signer.canonical("reservation_1", "bar_1", "table_1", 6, "invite_token_1");
        String signature = signer.sign(canonical);

        assertThat(signature).hasSize(64);
        assertThat(signer.matches(canonical, signature)).isTrue();
    }
}
