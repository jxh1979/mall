package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * P0-3 / X-01 门票二维码 HMAC 签名 单测。
 *
 * <p>覆盖：签名器算法（确定性 / 常量时间比对 / 密钥隔离）；签发凭证带签名；核销有效签名 → signature_valid=true 放行；
 * 篡改凭证字段（延长有效期）致签名失配 → failed/signature_invalid 且不消费权益；signer 未注入（兼容构造）→ 不签名不校验。
 */
class TicketCredentialSignatureTests {

    private static final String ENTITLEMENT_CODE = "P0-SIG-1";
    private static final String ENTITLEMENT_ID = "issued_1";
    private static final String DEVICE_ID = "dev_gate_1";
    private static final String TOKEN = "cred_token_1";
    private static final String NONCE = "nonce_sig_1";

    private final TicketCredentialSigner signer = new TicketCredentialSigner("unit-test-secret", "v1");

    private IssuedEntitlementRecord activeEntitlement() {
        Instant now = Instant.now();
        return new IssuedEntitlementRecord(
                ENTITLEMENT_ID, ENTITLEMENT_CODE, "tt_1", "order_1", "tpl_1", "ticket",
                "merchant_1", "venue_1", "point_1", 2, 0, 9900L, "active",
                null, null, now, now, 1, null,
                null, null, null, null, null, 0, null, null);
    }

    private VerificationRequest verifyRequest(String credentialPayload) {
        return new VerificationRequest(
                "verify-sig-1", "qr", credentialPayload, DEVICE_ID, "gate_1",
                Map.of("scenario", "ticket_checkin"));
    }

    private TicketCredentialRecord signedCredential(Instant now, Instant expiresAt, String signature) {
        return new TicketCredentialRecord(
                "credential_1", ENTITLEMENT_ID, "order_1", TOKEN, NONCE,
                "hash_x", "dynamic_qr", now, expiresAt, "active", 1, now, now, 60, now, signature, "v1");
    }

    // ---- 签名器算法层 ----

    @Test
    void prodProfileRejectsDefaultSigningSecret() {
        assertThatThrownBy(() -> new TicketCredentialSigner(
                TicketCredentialSigner.DEV_SIGNING_SECRET, "v1", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLEX_TICKET_SIGNING_SECRET");
    }

    @Test
    void signIsDeterministicConstantTimeAndKeyIsolated() {
        String canonical = signer.canonical(
                "cred_x", ENTITLEMENT_ID, NONCE, Instant.ofEpochMilli(1_000), Instant.ofEpochMilli(61_000));
        String signature = signer.sign(canonical);
        // 同输入同密钥确定性；HmacSHA256 = 32 字节 = 64 hex 字符（适配 signature_value VARCHAR(256)）。
        assertThat(signature).isEqualTo(signer.sign(canonical)).hasSize(64);
        assertThat(signer.matches(canonical, signature)).isTrue();
        assertThat(signer.matches(canonical, "deadbeef")).isFalse();
        assertThat(signer.matches(canonical, null)).isFalse();
        // 不同密钥对同串产生不同签名（密钥隔离）。
        assertThat(new TicketCredentialSigner("other-secret", "v1").sign(canonical)).isNotEqualTo(signature);
    }

    // ---- 签发：带签名 ----

    @Test
    void refreshCredentialSignsCredential() {
        TicketingRepository repository = mock(TicketingRepository.class);
        when(repository.terminalDeviceExists(any())).thenReturn(true);
        when(repository.findIssuedEntitlementById(ENTITLEMENT_ID)).thenReturn(Optional.of(activeEntitlement()));
        when(repository.findTicketTypeById(any())).thenReturn(Optional.empty()); // 默认刷新窗口 60s
        when(repository.saveTicketCredential(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketVerificationService service = new TicketVerificationService(
                repository, mock(ApplicationEventPublisher.class), null, null, signer, null);

        service.refreshCredential(ENTITLEMENT_ID, null);

        ArgumentCaptor<TicketCredentialRecord> captor = ArgumentCaptor.forClass(TicketCredentialRecord.class);
        verify(repository).saveTicketCredential(captor.capture());
        TicketCredentialRecord saved = captor.getValue();
        assertThat(saved.signatureValue()).isNotBlank();
        assertThat(saved.signatureVersion()).isEqualTo("v1");
        // 自洽：用保存的字段重算规范串应匹配存入的签名。
        String canonical = signer.canonical(
                saved.credentialToken(), saved.entitlementId(), saved.nonce(),
                saved.windowStartedAt(), saved.expiresAt());
        assertThat(signer.matches(canonical, saved.signatureValue())).isTrue();
    }

    @Test
    void refreshCredentialWithoutSignerLeavesSignatureNull() {
        TicketingRepository repository = mock(TicketingRepository.class);
        when(repository.terminalDeviceExists(any())).thenReturn(true);
        when(repository.findIssuedEntitlementById(ENTITLEMENT_ID)).thenReturn(Optional.of(activeEntitlement()));
        when(repository.findTicketTypeById(any())).thenReturn(Optional.empty());
        when(repository.saveTicketCredential(any())).thenAnswer(inv -> inv.getArgument(0));

        // 兼容构造（signer=null）：不签名，沿用占位 NULL 语义。
        TicketVerificationService service = new TicketVerificationService(
                repository, mock(ApplicationEventPublisher.class), null, null, null, null);

        service.refreshCredential(ENTITLEMENT_ID, null);

        ArgumentCaptor<TicketCredentialRecord> captor = ArgumentCaptor.forClass(TicketCredentialRecord.class);
        verify(repository).saveTicketCredential(captor.capture());
        assertThat(captor.getValue().signatureValue()).isNull();
        assertThat(captor.getValue().signatureVersion()).isNull();
    }

    // ---- 核销：签名校验 ----

    @Test
    void verificationWithValidSignaturePassesAndRecordsSignatureValid() {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(60);
        String canonical = signer.canonical(TOKEN, ENTITLEMENT_ID, NONCE, now, expiresAt);
        TicketCredentialRecord credential = signedCredential(now, expiresAt, signer.sign(canonical));

        TicketingRepository repository = mock(TicketingRepository.class);
        when(repository.terminalDeviceExists(any())).thenReturn(true);
        IssuedEntitlementRecord entitlement = activeEntitlement();
        when(repository.findTicketCredentialByToken(TOKEN)).thenReturn(Optional.of(credential));
        when(repository.findIssuedEntitlementById(ENTITLEMENT_ID)).thenReturn(Optional.of(entitlement));
        when(repository.atomicUseEntitlement(eq(ENTITLEMENT_ID), any()))
                .thenReturn(Optional.of(entitlement.useOnce(now)));
        when(repository.saveVerification(any())).thenAnswer(inv -> inv.getArgument(0));
        // AUD-012 凭证消费改为 CAS(mock 默认 0 会被视作并发撤销拒绝),此处按消费成功打桩。
        when(repository.markTicketCredentialConsumed(any(), any())).thenReturn(1);

        TicketVerificationService service = new TicketVerificationService(
                repository, mock(ApplicationEventPublisher.class), null, null, signer, null);

        VerificationResponse response = service.createVerification(verifyRequest(TOKEN));

        assertThat(response.status()).isEqualTo("success");
        ArgumentCaptor<VerificationRecord> captor = ArgumentCaptor.forClass(VerificationRecord.class);
        verify(repository).saveVerification(captor.capture());
        assertThat(captor.getValue().signatureValid()).isTrue();
    }

    @Test
    void tamperedCredentialFailsSignatureAndDoesNotConsumeEntitlement() {
        Instant now = Instant.now();
        // 攻击者篡改：签名对原有效期 now+60s 签发，但凭证记录的 expires_at 被改长到 now+3600s（试图延长二维码寿命）。
        Instant signedExpiry = now.plusSeconds(60);
        Instant tamperedExpiry = now.plusSeconds(3600);
        String canonical = signer.canonical(TOKEN, ENTITLEMENT_ID, NONCE, now, signedExpiry);
        TicketCredentialRecord tampered = signedCredential(now, tamperedExpiry, signer.sign(canonical));

        TicketingRepository repository = mock(TicketingRepository.class);
        when(repository.terminalDeviceExists(any())).thenReturn(true);
        when(repository.findTicketCredentialByToken(TOKEN)).thenReturn(Optional.of(tampered));
        when(repository.findIssuedEntitlementById(ENTITLEMENT_ID)).thenReturn(Optional.of(activeEntitlement()));
        when(repository.saveVerification(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketVerificationService service = new TicketVerificationService(
                repository, mock(ApplicationEventPublisher.class), null, null, signer, null);

        VerificationResponse response = service.createVerification(verifyRequest(TOKEN));

        assertThat(response.status()).isEqualTo("failed");
        assertThat(response.resultReason()).isEqualTo("signature_invalid");
        // 签名失配：不消费权益（never atomicUseEntitlement），落 failed 记录且 signature_valid=false。
        verify(repository, never()).atomicUseEntitlement(any(), any());
        ArgumentCaptor<VerificationRecord> captor = ArgumentCaptor.forClass(VerificationRecord.class);
        verify(repository).saveVerification(captor.capture());
        assertThat(captor.getValue().signatureValid()).isFalse();
    }

    @Test
    void unsignedDynamicCredentialIsRejectedWhenSignerActive() {
        // B2⑦ 门票 HMAC 对存量码强制：signer 已注入（生产）时，动态凭证若无签名（存量未签名码/被剥离签名的伪造码）
        // 必须按 signature_invalid 拒绝、不消费权益——此前 signature_valid=null 被放行（向后兼容）是漏洞。
        Instant now = Instant.now();
        TicketCredentialRecord unsigned = signedCredential(now, now.plusSeconds(60), null);

        TicketingRepository repository = mock(TicketingRepository.class);
        when(repository.terminalDeviceExists(any())).thenReturn(true);
        when(repository.findTicketCredentialByToken(TOKEN)).thenReturn(Optional.of(unsigned));
        when(repository.findIssuedEntitlementById(ENTITLEMENT_ID)).thenReturn(Optional.of(activeEntitlement()));
        when(repository.saveVerification(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketVerificationService service = new TicketVerificationService(
                repository, mock(ApplicationEventPublisher.class), null, null, signer, null);

        VerificationResponse response = service.createVerification(verifyRequest(TOKEN));

        assertThat(response.status()).isEqualTo("failed");
        assertThat(response.resultReason()).isEqualTo("signature_invalid");
        verify(repository, never()).atomicUseEntitlement(any(), any());
    }
}
