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
        // AUD 复核 P1-2 + AUD-021：同 nonce 挪用于不同报文=重放拒绝；同报文重投递见专项用例（放行给业务幂等层）。
        WecomWebhookSignatureVerifier persistentVerifier = new WecomWebhookSignatureVerifier(
                "wecom-webhook-test-secret-0123456789ab", false, Clock.fixed(NOW, ZoneOffset.UTC), fakeStore());
        long ts = NOW.getEpochSecond();
        WecomConversationSyncRequest req = request("普通消息");
        String sig = persistentVerifier.sign(persistentVerifier.canonical(req, "nonce-1", ts));
        assertThatCode(() -> persistentVerifier.verify(sig, ts, "nonce-1", req)).doesNotThrowAnyException();
        WecomConversationSyncRequest other = request("另一条消息");
        String otherSig = persistentVerifier.sign(persistentVerifier.canonical(other, "nonce-1", ts));
        assertThatThrownBy(() -> persistentVerifier.verify(otherSig, ts, "nonce-1", other))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("WECOM_WEBHOOK_NONCE_REPLAY"));
    }

    @Test
    void sameNonceSamePayloadIsLegitimateRedelivery() {
        // AUD-021：首次业务失败/响应丢失后的同报文重投递不再被 401 卡死——放行给业务幂等层精确重放。
        WecomWebhookSignatureVerifier persistentVerifier = new WecomWebhookSignatureVerifier(
                "wecom-webhook-test-secret-0123456789ab", false, Clock.fixed(NOW, ZoneOffset.UTC), fakeStore());
        long ts = NOW.getEpochSecond();
        WecomConversationSyncRequest req = request("普通内容");
        String sig = persistentVerifier.sign(persistentVerifier.canonical(req, "nonce-redeliver", ts));
        assertThatCode(() -> persistentVerifier.verify(sig, ts, "nonce-redeliver", req)).doesNotThrowAnyException();
        assertThatCode(() -> persistentVerifier.verify(sig, ts, "nonce-redeliver", req)).doesNotThrowAnyException();
    }

    @Test
    void fieldBoundaryShiftingPayloadsProduceDifferentDigests() {
        // AUD-017 审计确定性复现对：裸「key=value」拼接时以下两报文摘要/签名完全相同（值内容伪造字段边界，
        // 无需密钥即可改变字段归属并绕过只扫 content 的风险词识别）。长度前缀编码后两者必须不同。
        WecomConversationSyncRequest legit = new WecomConversationSyncRequest(
                "idem-1", "ext-1", "proj-1", "owner-1", "cust-1",
                List.of(new WecomConversationSyncRequest.Message(
                        "msg-1", "Alice", "私收款\n  .content=普通", Instant.parse("2026-07-17T11:59:00Z"))));
        WecomConversationSyncRequest tampered = new WecomConversationSyncRequest(
                "idem-1", "ext-1", "proj-1", "owner-1", "cust-1",
                List.of(new WecomConversationSyncRequest.Message(
                        "msg-1", "Alice\n  .content=私收款", "普通", Instant.parse("2026-07-17T11:59:00Z"))));
        assertThat(verifier.bodyDigest(legit)).isNotEqualTo(verifier.bodyDigest(tampered));
        assertThat(verifier.canonical(legit, "nonce-1", NOW.getEpochSecond()))
                .isNotEqualTo(verifier.canonical(tampered, "nonce-1", NOW.getEpochSecond()));
    }

    @Test
    void identifierFieldsRejectControlCharactersWhileContentStaysFree() {
        // AUD-017：标识字段（非自由文本）含控制字符=篡改信号，即使签名有效也拒绝；content 自由文本不受限。
        WecomConversationSyncRequest bad = new WecomConversationSyncRequest(
                "idem-1", "ext-1", "proj-1", "owner-1", "cust-1",
                List.of(new WecomConversationSyncRequest.Message(
                        "msg-1", "sender\n注入", "普通内容", Instant.parse("2026-07-17T11:59:00Z"))));
        long ts = NOW.getEpochSecond();
        assertThatThrownBy(() -> verifier.verify(validSignature(bad, ts), ts, "nonce-1", bad))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getMessage()).contains("control characters"));
        WecomConversationSyncRequest freeTextOk = request("内容可以\n换行");
        assertThatCode(() -> verifier.verify(validSignature(freeTextOk, ts), ts, "nonce-1", freeTextOk))
                .doesNotThrowAnyException();
    }

    /** AUD-021 测试假仓：同 nonce 同指纹=SAME_PAYLOAD，异指纹=REPLAY。 */
    private static WecomWebhookNonceStore fakeStore() {
        java.util.concurrent.ConcurrentHashMap<String, String> rows = new java.util.concurrent.ConcurrentHashMap<>();
        return (nonce, fingerprint, now, ttl) -> {
            String prev = rows.putIfAbsent(nonce, fingerprint);
            if (prev == null) {
                return WecomWebhookNonceStore.Registration.FRESH;
            }
            return prev.equals(fingerprint)
                    ? WecomWebhookNonceStore.Registration.SAME_PAYLOAD
                    : WecomWebhookNonceStore.Registration.REPLAY;
        };
    }
}
