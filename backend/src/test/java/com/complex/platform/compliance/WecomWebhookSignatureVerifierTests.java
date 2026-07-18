package com.complex.platform.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** AUD-001：企微 webhook 签名校验器——有效签名放行；未签名/伪造/过期/缺 nonce 一律 fail-closed。 */
class WecomWebhookSignatureVerifierTests {

    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");
    private final WecomWebhookSignatureVerifier verifier =
            new WecomWebhookSignatureVerifier("wecom-webhook-test-secret-0123456789ab", false,
                    Clock.fixed(NOW, ZoneOffset.UTC));

    private String validSignature(long timestamp) {
        return verifier.sign(verifier.canonical("ext-1", "idem-1", "nonce-1", "proj-1", timestamp));
    }

    @Test
    void acceptsFreshValidSignature() {
        long ts = NOW.getEpochSecond();
        assertThatCode(() -> verifier.verify(validSignature(ts), ts, "nonce-1", "idem-1", "ext-1", "proj-1"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTamperedSignature() {
        long ts = NOW.getEpochSecond();
        assertThatThrownBy(() -> verifier.verify(validSignature(ts) + "00", ts, "nonce-1", "idem-1", "ext-1", "proj-1"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_SIGNATURE_INVALID"));
    }

    @Test
    void rejectsMismatchedFieldValues() {
        long ts = NOW.getEpochSecond();
        // 用正确签名但换掉某个签名覆盖字段（project_id）→ 期望重算失配。
        assertThatThrownBy(() -> verifier.verify(validSignature(ts), ts, "nonce-1", "idem-1", "ext-1", "proj-OTHER"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_SIGNATURE_INVALID"));
    }

    @Test
    void rejectsMissingSignatureTimestampNonce() {
        long ts = NOW.getEpochSecond();
        assertThatThrownBy(() -> verifier.verify(null, ts, "nonce-1", "idem-1", "ext-1", "proj-1"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_SIGNATURE_MISSING"));
        assertThatThrownBy(() -> verifier.verify(validSignature(ts), null, "nonce-1", "idem-1", "ext-1", "proj-1"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_TIMESTAMP_MISSING"));
        assertThatThrownBy(() -> verifier.verify(validSignature(ts), ts, " ", "idem-1", "ext-1", "proj-1"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_NONCE_MISSING"));
    }

    @Test
    void rejectsExpiredTimestamp() {
        long staleTs = NOW.getEpochSecond() - 3600;
        assertThatThrownBy(() -> verifier.verify(validSignature(staleTs), staleTs, "nonce-1", "idem-1", "ext-1", "proj-1"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_TIMESTAMP_EXPIRED"));
    }
}
