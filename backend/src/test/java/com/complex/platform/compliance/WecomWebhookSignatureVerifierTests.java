package com.complex.platform.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.compliance.dto.WecomConversationSyncRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * AUD-001 + AUD 复核 P1-2：企微 webhook 签名校验器——有效签名放行；未签名/伪造/过期/缺 nonce 一律 fail-closed；
 * 报文体（消息内容/发送者）纳入验签，篡改即失配；nonce 持久化去重，窗口内重放被拒。
 */
class WecomWebhookSignatureVerifierTests {

    private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");
    private final WecomWebhookSignatureVerifier verifier =
            new WecomWebhookSignatureVerifier("wecom-webhook-test-secret-0123456789ab", false,
                    Clock.fixed(NOW, ZoneOffset.UTC));

    private WecomConversationSyncRequest request(String content) {
        return new WecomConversationSyncRequest(
                "idem-1", "ext-1", "proj-1", "owner-1", "cust-1",
                List.of(new WecomConversationSyncRequest.Message(
                        "msg-1", "sender-1", content, Instant.parse("2026-07-17T11:59:00Z"))));
    }

    private String validSignature(WecomConversationSyncRequest req, long timestamp) {
        return verifier.sign(verifier.canonical(req, "nonce-1", timestamp));
    }

    @Test
    void acceptsFreshValidSignature() {
        long ts = NOW.getEpochSecond();
        WecomConversationSyncRequest req = request("普通消息");
        assertThatCode(() -> verifier.verify(validSignature(req, ts), ts, "nonce-1", req))
                .doesNotThrowAnyException();
    }

    // ── AUD 复核 P3：报文体 messages 含 null 元素时 bodyDigest/verify 不 NPE→500 ──────────────────────
    @Test
    void bodyDigestAndVerifyToleratesNullMessageElement() {
        long ts = NOW.getEpochSecond();
        // messages 含 null 元素（与 WecomConversationService 跳过 null message 的处理一致）
        WecomConversationSyncRequest req = new WecomConversationSyncRequest(
                "idem-1", "ext-1", "proj-1", "owner-1", "cust-1",
                java.util.Arrays.asList(
                        new WecomConversationSyncRequest.Message(
                                "msg-1", "sender-1", "普通消息", Instant.parse("2026-07-17T11:59:00Z")),
                        null));
        // bodyDigest 对含 null 的报文体仍确定可算（稳定 marker），不抛 NPE
        assertThatCode(() -> verifier.bodyDigest(req)).doesNotThrowAnyException();
        // 有效签名放行（含 null 报文体验签一致通过，不落 500）
        assertThatCode(() -> verifier.verify(validSignature(req, ts), ts, "nonce-1", req))
                .doesNotThrowAnyException();
        // 伪造签名仍 fail-closed 拒绝（是签名失配而非 NPE→500）
        assertThatThrownBy(() -> verifier.verify(validSignature(req, ts) + "00", ts, "nonce-1", req))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_SIGNATURE_INVALID"));
    }

    @Test
    void rejectsTamperedSignature() {
        long ts = NOW.getEpochSecond();
        WecomConversationSyncRequest req = request("普通消息");
        assertThatThrownBy(() -> verifier.verify(validSignature(req, ts) + "00", ts, "nonce-1", req))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_SIGNATURE_INVALID"));
    }

    @Test
    void rejectsMismatchedFieldValues() {
        long ts = NOW.getEpochSecond();
        WecomConversationSyncRequest signed = request("普通消息");
        // 用正确签名但换掉某个签名覆盖字段（project_id）→ 期望重算失配。
        WecomConversationSyncRequest tampered = new WecomConversationSyncRequest(
                signed.idempotencyKey(), signed.externalConversationId(), "proj-OTHER",
                signed.ownerStaffId(), signed.customerRef(), signed.messages());
        assertThatThrownBy(() -> verifier.verify(validSignature(signed, ts), ts, "nonce-1", tampered))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_SIGNATURE_INVALID"));
    }

    @Test
    void rejectsTamperedMessageContent() {
        long ts = NOW.getEpochSecond();
        // AUD 复核 P1-2：签名对"普通消息"，提交时把消息内容改为诱导私收款 → 报文体摘要变化 → 验签失败。
        WecomConversationSyncRequest signed = request("普通消息");
        WecomConversationSyncRequest tampered = request("加我私微，直接私收款转我");
        assertThatThrownBy(() -> verifier.verify(validSignature(signed, ts), ts, "nonce-1", tampered))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_SIGNATURE_INVALID"));
    }

    @Test
    void rejectsMissingSignatureTimestampNonce() {
        long ts = NOW.getEpochSecond();
        WecomConversationSyncRequest req = request("普通消息");
        assertThatThrownBy(() -> verifier.verify(null, ts, "nonce-1", req))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_SIGNATURE_MISSING"));
        assertThatThrownBy(() -> verifier.verify(validSignature(req, ts), null, "nonce-1", req))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_TIMESTAMP_MISSING"));
        assertThatThrownBy(() -> verifier.verify(validSignature(req, ts), ts, " ", req))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_NONCE_MISSING"));
    }

    @Test
    void rejectsExpiredTimestamp() {
        long staleTs = NOW.getEpochSecond() - 3600;
        WecomConversationSyncRequest req = request("普通消息");
        assertThatThrownBy(() -> verifier.verify(validSignature(req, staleTs), staleTs, "nonce-1", req))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_TIMESTAMP_EXPIRED"));
    }

    @Test
    void rejectsReplayedNonce() {
        // AUD 复核 P1-2：带持久化 nonce store 的校验器——同 nonce 二次提交（窗口内）判重放拒绝。
        Set<String> seen = ConcurrentHashMap.newKeySet();
        WecomWebhookNonceStore store = (nonce, now, ttl) -> seen.add(nonce);
        WecomWebhookSignatureVerifier persistentVerifier = new WecomWebhookSignatureVerifier(
                "wecom-webhook-test-secret-0123456789ab", false, Clock.fixed(NOW, ZoneOffset.UTC), store);
        long ts = NOW.getEpochSecond();
        WecomConversationSyncRequest req = request("普通消息");
        String sig = persistentVerifier.sign(persistentVerifier.canonical(req, "nonce-1", ts));
        assertThatCode(() -> persistentVerifier.verify(sig, ts, "nonce-1", req)).doesNotThrowAnyException();
        assertThatThrownBy(() -> persistentVerifier.verify(sig, ts, "nonce-1", req))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_NONCE_REPLAY"));
    }
}
