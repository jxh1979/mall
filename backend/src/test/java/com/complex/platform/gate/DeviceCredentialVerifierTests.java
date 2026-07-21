package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.StaffDeviceCredentialRecord;
import com.complex.platform.access.staff.StaffProfileRecord;
import com.complex.platform.access.staff.StaffRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.TerminalDeviceRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** AUD-010 设备凭证验签 + 分级强制单测(真实 EC 密钥签/验,mock 仓储)。 */
class DeviceCredentialVerifierTests {

    private static final long NOW = Instant.parse("2026-07-20T12:00:00Z").toEpochMilli();
    private static final String DEVICE = "dev1";
    private static final String URI = "/api/v1/services/orders/o1/mark-paid";

    private TerminalDeviceRepository deviceRepo;
    private StaffRepository staffRepo;
    private AuthorizationService authz;
    private AuditEventService audit;
    private KeyPair keyPair;
    private String pubB64;

    @BeforeEach
    void setUp() throws Exception {
        deviceRepo = mock(TerminalDeviceRepository.class);
        staffRepo = mock(StaffRepository.class);
        authz = mock(AuthorizationService.class);
        audit = mock(AuditEventService.class);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        keyPair = kpg.generateKeyPair();
        pubB64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        when(deviceRepo.findByDeviceId(DEVICE)).thenReturn(Optional.of(device("staff_work_phone", "bound")));
        when(authz.currentActor()).thenReturn(new AccessActor("u1", "user", "staff", "org", null, "active"));
        when(staffRepo.findProfileByUserId("u1")).thenReturn(Optional.of(profile()));
        when(staffRepo.findActiveCredential("sp1"))
                .thenReturn(Optional.of(credential(pubB64)));
    }

    private DeviceCredentialVerifier verifier(String defaultMode) {
        return new DeviceCredentialVerifier(deviceRepo, staffRepo, authz, audit,
                Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC),
                defaultMode, "", "", "", 300);
    }

    private MockHttpServletRequest signedRequest(String method, long ts, String nonce, String sigB64) {
        MockHttpServletRequest r = new MockHttpServletRequest(method, URI);
        r.setRequestURI(URI);
        r.addHeader("X-Terminal-Type", "staff_work_phone");
        r.addHeader("X-Device-Id", DEVICE);
        r.addHeader("X-Device-Timestamp", Long.toString(ts));
        r.addHeader("X-Device-Nonce", nonce);
        r.addHeader("X-Device-Signature", sigB64);
        return r;
    }

    private String sign(String method, long ts, String nonce) throws Exception {
        String canonical = DEVICE + "\n" + ts + "\n" + nonce + "\n" + method + "\n" + URI;
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(keyPair.getPrivate());
        s.update(canonical.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(s.sign());
    }

    @Test
    void nonDeviceBoundTypePassesThrough() {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/v1/services/orders");
        r.addHeader("X-Terminal-Type", "web");
        TerminalIdentityHolder.VerifiedTerminal vt = verifier("enforce").verify(r);
        assertThat(vt.terminalType()).isEqualTo("web");
        assertThat(vt.verified()).isFalse();
    }

    @Test
    void validSignatureDerivesTrustedTypeVerified() throws Exception {
        long ts = NOW;
        String sig = sign("POST", ts, "n1");
        TerminalIdentityHolder.VerifiedTerminal vt = verifier("enforce").verify(signedRequest("POST", ts, "n1", sig));
        assertThat(vt.verified()).isTrue();
        assertThat(vt.terminalType()).isEqualTo("staff_work_phone");
        assertThat(vt.deviceId()).isEqualTo(DEVICE);
    }

    @Test
    void enforceRejectsWriteWithoutCredential() {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", URI);
        r.setRequestURI(URI);
        r.addHeader("X-Terminal-Type", "staff_work_phone");
        assertThatThrownBy(() -> verifier("enforce").verify(r))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    @Test
    void graceAllowsWriteWithoutCredential() {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", URI);
        r.setRequestURI(URI);
        r.addHeader("X-Terminal-Type", "staff_work_phone");
        TerminalIdentityHolder.VerifiedTerminal vt = verifier("grace").verify(r);
        assertThat(vt.verified()).isFalse();
        assertThat(vt.terminalType()).isEqualTo("staff_work_phone");
    }

    @Test
    void invalidSignatureRejectedEvenInGrace() throws Exception {
        // valid signature for nonce n1, but replayed with a different nonce header -> canonical mismatch.
        String sig = sign("POST", NOW, "n1");
        assertThatThrownBy(() -> verifier("grace").verify(signedRequest("POST", NOW, "n2", sig)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    @Test
    void staleTimestampRejected() throws Exception {
        long stale = NOW - 3600_000L;
        String sig = sign("POST", stale, "n1");
        assertThatThrownBy(() -> verifier("enforce").verify(signedRequest("POST", stale, "n1", sig)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    @Test
    void unboundDeviceRejected() throws Exception {
        when(deviceRepo.findByDeviceId(DEVICE)).thenReturn(Optional.of(device("staff_work_phone", "returned")));
        String sig = sign("POST", NOW, "n1");
        assertThatThrownBy(() -> verifier("enforce").verify(signedRequest("POST", NOW, "n1", sig)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    @Test
    void readRequestNotRejectedUnderEnforceWhenNoCredential() {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", URI);
        r.setRequestURI(URI);
        r.addHeader("X-Terminal-Type", "staff_work_phone");
        TerminalIdentityHolder.VerifiedTerminal vt = verifier("enforce").verify(r);
        assertThat(vt.verified()).isFalse();
        assertThat(vt.terminalType()).isEqualTo("staff_work_phone");
    }

    @Test
    void tamperedTypeMismatchRejected() throws Exception {
        when(deviceRepo.findByDeviceId(DEVICE)).thenReturn(Optional.of(device("kiosk", "bound")));
        String sig = sign("POST", NOW, "n1");
        assertThatThrownBy(() -> verifier("enforce").verify(signedRequest("POST", NOW, "n1", sig)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    @Test
    void graceAllowsUnknownDeviceWithSignature() {
        // 未登记设备(未在 terminal_devices)携签名 → 软态,grace 放行(灰度期不硬拒尚未登记设备)。
        org.mockito.Mockito.when(deviceRepo.findByDeviceId("devX")).thenReturn(Optional.empty());
        TerminalIdentityHolder.VerifiedTerminal vt = verifier("grace").verify(unknownDeviceRequest());
        assertThat(vt.verified()).isFalse();
        assertThat(vt.terminalType()).isEqualTo("staff_work_phone");
    }

    @Test
    void enforceRejectsUnknownDeviceWrite() {
        org.mockito.Mockito.when(deviceRepo.findByDeviceId("devX")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> verifier("enforce").verify(unknownDeviceRequest()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    private static MockHttpServletRequest unknownDeviceRequest() {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", URI);
        r.setRequestURI(URI);
        r.addHeader("X-Terminal-Type", "staff_work_phone");
        r.addHeader("X-Device-Id", "devX");
        r.addHeader("X-Device-Timestamp", Long.toString(NOW));
        r.addHeader("X-Device-Nonce", "n1");
        r.addHeader("X-Device-Signature", "AAAA");
        return r;
    }

    private static TerminalDeviceRecord device(String terminalType, String status) {
        Instant now = Instant.ofEpochMilli(NOW);
        return new TerminalDeviceRecord(
                "td1", DEVICE, "code1", terminalType, "device", null, null, null,
                "merchant_1", null, null, null, "sp1", status, now, null, null, null, null, now, now);
    }

    private static StaffProfileRecord profile() {
        Instant now = Instant.ofEpochMilli(NOW);
        return new StaffProfileRecord("sp1", "proj1", null, "u1", "Name", "ph", "id", "lvl1",
                "approved", "admin", now, null, "active", now, now);
    }

    private static StaffDeviceCredentialRecord credential(String pubB64) {
        Instant now = Instant.ofEpochMilli(NOW);
        return new StaffDeviceCredentialRecord("scred1", "sp1", pubB64, "fp", "active",
                null, "trusted", null, now, null, now, now);
    }
}
