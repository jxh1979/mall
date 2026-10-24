package com.complex.platform.gate;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.StaffDeviceCredentialRecord;
import com.complex.platform.access.staff.StaffProfileRecord;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.DeviceSigningKeyStore;
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
    private JdbcStaffRepository staffRepo;
    private AuthorizationService authz;
    private AuditEventService audit;
    private DeviceSigningKeyStore signingKeyStore;
    private CustomerRepository customerRepo;
    private KeyPair keyPair;
    private String pubB64;

    @BeforeEach
    void setUp() throws Exception {
        deviceRepo = mock(TerminalDeviceRepository.class);
        staffRepo = mock(JdbcStaffRepository.class);
        authz = mock(AuthorizationService.class);
        audit = mock(AuditEventService.class);
        signingKeyStore = mock(DeviceSigningKeyStore.class);
        customerRepo = mock(CustomerRepository.class);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        keyPair = kpg.generateKeyPair();
        pubB64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        when(deviceRepo.findByDeviceId(DEVICE)).thenReturn(Optional.of(device("staff_work_phone", "bound")));
        when(authz.currentActor()).thenReturn(new AccessActor("u1", "user", "staff", "org", null, "active"));
        when(staffRepo.findProfileByUserId("u1")).thenReturn(Optional.of(profile()));
        when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.of(pubB64));
    }

    private DeviceCredentialVerifier verifier(String defaultMode) {
        return new DeviceCredentialVerifier(deviceRepo, staffRepo, signingKeyStore, authz, audit, customerRepo,
                Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC),
                defaultMode, "", "", "", "", "", "", 300);
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
        String canonical = DEVICE + "\n" + ts + "\n" + nonce + "\n" + method + "\n" + URI
                + "\n" + emptyBodyDigest();
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(keyPair.getPrivate());
        s.update(canonical.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(s.sign());
    }

    @Test
    void nonDeviceBoundTypePassesThrough() {
        // U07-003/U12-005：无设备号的写请求 fail-close——自报 web 且无 Cookie 会话在 enforce 下拒；有 Cookie 会话才以未验签态放行。
        MockHttpServletRequest bare = new MockHttpServletRequest("POST", "/api/v1/services/orders");
        bare.addHeader("X-Terminal-Type", "web");
        assertThatThrownBy(() -> verifier("enforce").verify(bare))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/v1/services/orders");
        r.addHeader("X-Terminal-Type", "web");
        r.setCookies(new jakarta.servlet.http.Cookie("admin_access_token", "session"));
        TerminalIdentityHolder.VerifiedTerminal vt = verifier("enforce").verify(r);
        assertThat(vt.terminalType()).isEqualTo("web");
        assertThat(vt.verified()).isFalse();
        MockHttpServletRequest unknown = new MockHttpServletRequest("POST", "/api/v1/services/orders");
        assertThatThrownBy(() -> verifier("enforce").verify(unknown))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
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
    void privatePhoneKeyCannotReplaceWorkPhoneKey() throws Exception {
        when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.empty());
        when(staffRepo.findActiveCredential("sp1")).thenReturn(Optional.of(credential(pubB64)));
        assertThatThrownBy(() -> verifier("enforce").verify(signedRequest("POST", NOW, "n1", sign("POST", NOW, "n1"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
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

    private String sign(String method, long ts, String nonce, String bodyDigest) throws Exception {
        String canonical = DEVICE + "\n" + ts + "\n" + nonce + "\n" + method + "\n" + URI + "\n" + bodyDigest;
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(keyPair.getPrivate());
        s.update(canonical.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(s.sign());
    }

    private static String digest(byte[] body) throws Exception {
        return java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(body));
    }

    private DeviceCredentialVerifier verifierWith(String defaultMode, String webMode, String appMode) {
        return verifierWith(defaultMode, webMode, appMode, "");
    }

    private DeviceCredentialVerifier verifierWith(String defaultMode, String webMode, String appMode, String miniappMode) {
        return new DeviceCredentialVerifier(deviceRepo, staffRepo, signingKeyStore, authz, audit, customerRepo,
                Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC),
                defaultMode, "", "", "", webMode, appMode, miniappMode, 300);
    }

    // ---------- ROOT-03-R2 客人两端：证明＝已验证客人会话，会话锁定登录端 ----------

    private MockHttpServletRequest customerRequest(String method, String uri, String terminal, String token) {
        MockHttpServletRequest r = new MockHttpServletRequest(method, uri);
        r.setRequestURI(uri);
        if (terminal != null) {
            r.addHeader("X-Terminal-Type", terminal);
        }
        if (token != null) {
            r.addHeader("X-Customer-Token", token);
        }
        return r;
    }

    private void session(String token, String scene) {
        when(customerRepo.findSessionByToken(token)).thenReturn(Optional.of(new CustomerSessionRecord(
                "s_" + token, "customer_1", token, scene, Instant.ofEpochMilli(NOW).minusSeconds(60),
                Instant.ofEpochMilli(NOW).plusSeconds(3600), "active")));
    }

    @Test
    void appWriteWithMatchingCustomerSessionIsVerified() {
        session("tok_app", "app");
        TerminalIdentityHolder.VerifiedTerminal vt = verifierWith("grace", "", "enforce", "enforce")
                .verify(customerRequest("POST", "/api/v1/customer/ticket-orders", "app", "tok_app"));
        assertThat(vt.terminalType()).isEqualTo("app");
        assertThat(vt.verified()).isTrue();
        assertThat(vt.deviceId()).isNull();
    }

    @Test
    void miniappSessionTokenUsedAsAppRejectedSessionMismatch() {
        session("tok_mini", "miniapp");
        // 任何模式都拒（含 off/grace）：会话锁定登录端；改报 web 或不报类型同样拒。
        for (String claimed : new String[] {"app", "web", "tablet", null}) {
            assertThatThrownBy(() -> verifierWith("off", "", "", "")
                    .verify(customerRequest("GET", "/api/v1/customer/orders", claimed, "tok_mini")))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
        }
    }

    @Test
    void appWriteWithoutSessionRejectedInEnforce() {
        assertThatThrownBy(() -> verifierWith("grace", "", "enforce", "")
                .verify(customerRequest("POST", "/api/v1/customer/ticket-orders", "app", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
        // 带了客人令牌但会话已过期或已失效：仍拒绝，但报 401 客人登录已过期（客户端约定 401 才回登录页），不报设备凭据错误。
        when(customerRepo.findSessionByToken("tok_old")).thenReturn(Optional.of(new CustomerSessionRecord(
                "s_old", "customer_1", "tok_old", "app", Instant.ofEpochMilli(NOW).minusSeconds(7200),
                Instant.ofEpochMilli(NOW).minusSeconds(60), "active")));
        assertThatThrownBy(() -> verifierWith("grace", "", "enforce", "")
                .verify(customerRequest("POST", "/api/v1/customer/ticket-orders", "app", "tok_old")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CUSTOMER_UNAUTHENTICATED");
                    assertThat(e.status().value()).isEqualTo(401);
                });
        // grace 下留痕放行（自报类型，未验证）。
        TerminalIdentityHolder.VerifiedTerminal vt = verifierWith("grace", "", "grace", "")
                .verify(customerRequest("POST", "/api/v1/customer/ticket-orders", "app", null));
        assertThat(vt.verified()).isFalse();
    }

    @Test
    void customerLoginPathAllowedWithoutSessionInEnforce() {
        for (String uri : new String[] {"/api/v1/customer/auth/phone-login", "/api/v1/customer/auth/sms-codes",
                "/api/v1/customer/auth/register", "/api/v1/customer/auth/logout"}) {
            TerminalIdentityHolder.VerifiedTerminal vt = verifierWith("grace", "", "enforce", "enforce")
                    .verify(customerRequest("POST", uri, "miniapp", null));
            assertThat(vt.terminalType()).isEqualTo("miniapp");
            assertThat(vt.verified()).isFalse();
        }
    }

    /** ROOT-03-R7（ENUM 第 129 行「扫码点单身份：非会员可以浏览、点单和付款」）：enforce 下无会话只放行扫码点单下单与付款确认两个写口。 */
    @Test
    void anonymousScanOrderWritesAllowedWithoutSessionInEnforce() {
        for (String uri : new String[] {"/api/v1/customer/bar-tables/t1/scan-orders",
                "/api/v1/customer/bar-tables/t1/scan-orders/bso_1/confirm"}) {
            org.assertj.core.api.Assertions.assertThatCode(() -> verifierWith("grace", "", "enforce", "enforce")
                    .verify(customerRequest("POST", uri, "miniapp", null))).as(uri).doesNotThrowAnyException();
            TerminalIdentityHolder.VerifiedTerminal vt = verifierWith("grace", "", "enforce", "enforce")
                    .verify(customerRequest("POST", uri, "miniapp", null));
            assertThat(vt.terminalType()).isEqualTo("miniapp");
            assertThat(vt.verified()).isFalse();
        }
        assertThatThrownBy(() -> verifierWith("grace", "", "enforce", "enforce")
                .verify(customerRequest("POST", "/api/v1/customer/bar-tables/t1/scan-orders/bso_1/refund", "miniapp", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    @Test
    void miniappEnforcementModeConfigurable() {
        // miniapp 只在配置 enforce 时拒无会话写；未配置沿用 default（grace）留痕放行。
        assertThatThrownBy(() -> verifierWith("grace", "", "", "enforce")
                .verify(customerRequest("POST", "/api/v1/customer/consents", "miniapp", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
        assertThat(verifierWith("grace", "", "", "")
                .verify(customerRequest("POST", "/api/v1/customer/consents", "miniapp", null)).terminalType())
                .isEqualTo("miniapp");
    }

    @Test
    void webWithBearerRejectedUnderEnforceButAllowedUnderGrace() {
        org.springframework.mock.web.MockHttpServletRequest r =
                new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/v1/services/orders/o1/mark-paid");
        r.addHeader("X-Terminal-Type", "web");
        r.addHeader("Authorization", "Bearer abc.def.ghi");
        assertThatThrownBy(() -> verifierWith("grace", "enforce", "").verify(r))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
        TerminalIdentityHolder.VerifiedTerminal vt = verifier("grace").verify(r);
        assertThat(vt.terminalType()).isEqualTo("web");
        assertThat(vt.verified()).isFalse();
    }

    @Test
    void webWithCookieAllowedUnderEnforce() {
        org.springframework.mock.web.MockHttpServletRequest r =
                new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/v1/services/orders/o1/mark-paid");
        r.addHeader("X-Terminal-Type", "web");
        // U07-003：web 写请求的证明是不可伪造的 Cookie 会话，不是自报头。
        r.setCookies(new jakarta.servlet.http.Cookie("admin_access_token", "session"));
        TerminalIdentityHolder.VerifiedTerminal vt = verifierWith("grace", "enforce", "").verify(r);
        assertThat(vt.terminalType()).isEqualTo("web");
        assertThat(vt.verified()).isFalse();
    }

    @Test
    void appWriteRejectedUnderEnforce() {
        org.springframework.mock.web.MockHttpServletRequest r =
                new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/v1/services/orders");
        r.addHeader("X-Terminal-Type", "app");
        assertThatThrownBy(() -> verifierWith("grace", "", "enforce").verify(r))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    @Test
    void deviceOwnerMismatchRejected() throws Exception {
        when(staffRepo.findProfileByUserId("u1")).thenReturn(Optional.of(otherProfile()));
        String sig = sign("POST", NOW, "n1");
        assertThatThrownBy(() -> verifier("enforce").verify(signedRequest("POST", NOW, "n1", sig)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    @Test
    void staffWorkPhoneWithNullStaffIdRejected() throws Exception {
        // 无 staffId 的 bound staff_work_phone 设备 -> 归属不符拒（不回落用当前员工密钥）。
        when(deviceRepo.findByDeviceId(DEVICE)).thenReturn(Optional.of(deviceNullStaff()));
        String sig = sign("POST", NOW, "n1");
        assertThatThrownBy(() -> verifier("enforce").verify(signedRequest("POST", NOW, "n1", sig)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    private static TerminalDeviceRecord deviceNullStaff() {
        Instant now = Instant.ofEpochMilli(NOW);
        return new TerminalDeviceRecord(
                "td1", DEVICE, "code1", "staff_work_phone", "device", null, null, null,
                "merchant_1", null, null, null, null, "bound", now, null, null, null, null, now, now);
    }

    @Test
    void kioskUsesDeviceLevelKey() throws Exception {
        when(deviceRepo.findByDeviceId(DEVICE)).thenReturn(Optional.of(device("kiosk", "bound")));
        when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.of(pubB64));
        org.springframework.mock.web.MockHttpServletRequest r = signedRequest("POST", NOW, "n1", sign("POST", NOW, "n1"));
        r.removeHeader("X-Terminal-Type");
        r.addHeader("X-Terminal-Type", "kiosk");
        TerminalIdentityHolder.VerifiedTerminal vt = verifier("enforce").verify(r);
        assertThat(vt.verified()).isTrue();
        assertThat(vt.terminalType()).isEqualTo("kiosk");
    }

    @Test
    void bodyTamperFailsSignature() throws Exception {
        byte[] signedBody = "{\"amount\":100}".getBytes(StandardCharsets.UTF_8);
        byte[] tamperedBody = "{\"amount\":999}".getBytes(StandardCharsets.UTF_8);
        String sig = sign("POST", NOW, "n1", digest(signedBody));
        org.springframework.mock.web.MockHttpServletRequest mock = signedRequest("POST", NOW, "n1", sig);
        mock.setContent(tamperedBody);
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(mock);
        assertThatThrownBy(() -> verifier("enforce").verify(wrapped))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    @Test
    void bodySignedMatchesVerifies() throws Exception {
        byte[] body = "{\"amount\":100}".getBytes(StandardCharsets.UTF_8);
        String sig = sign("POST", NOW, "n1", digest(body));
        org.springframework.mock.web.MockHttpServletRequest mock = signedRequest("POST", NOW, "n1", sig);
        mock.setContent(body);
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(mock);
        TerminalIdentityHolder.VerifiedTerminal vt = verifier("enforce").verify(wrapped);
        assertThat(vt.verified()).isTrue();
    }

    // ---- 2026-08-03 收口：自报 X-Terminal-Type 不再决定是否进设备绑定型验签闸 ----

    /** 只带 X-Device-Id、不发终端类型头：服务端反查登记类型 kiosk，enforce 下缺签名的写请求仍被拒。 */
    @Test
    void omittedTerminalHeaderStillEntersDeviceBoundGate() {
        when(deviceRepo.findByDeviceId(DEVICE)).thenReturn(Optional.of(device("kiosk", "bound")));
        assertThatThrownBy(() -> verifier("enforce").verify(deviceIdOnly(null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    /** 改报 web 想走非设备绑定型旁路：登记类型 kiosk 压过自报值，enforce 下照拒。 */
    @Test
    void selfReportedWebCannotEscapeDeviceBoundGate() {
        when(deviceRepo.findByDeviceId(DEVICE)).thenReturn(Optional.of(device("kiosk", "bound")));
        assertThatThrownBy(() -> verifier("enforce").verify(deviceIdOnly("web")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
    }

    /** grace 下不硬拒，但派生出的可信终端类型是登记的 kiosk（不是自报的 web）——总开关据此判定。 */
    @Test
    void registeredTypeWinsOverSelfReportedUnderGrace() {
        when(deviceRepo.findByDeviceId(DEVICE)).thenReturn(Optional.of(device("kiosk", "bound")));
        TerminalIdentityHolder.VerifiedTerminal vt = verifier("grace").verify(deviceIdOnly("web"));
        assertThat(vt.terminalType()).isEqualTo("kiosk");
        assertThat(vt.verified()).isFalse();
    }

    private static MockHttpServletRequest deviceIdOnly(String claimedType) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", URI);
        r.setRequestURI(URI);
        if (claimedType != null) {
            r.addHeader("X-Terminal-Type", claimedType);
        }
        r.addHeader("X-Device-Id", DEVICE);
        return r;
    }

    private static StaffProfileRecord otherProfile() {
        Instant now = Instant.ofEpochMilli(NOW);
        return new StaffProfileRecord("spOther", "proj1", null, "u1", "Other", "ph", "id", "lvl1",
                "approved", "admin", now, null, "active", now, now);
    }

    private static String emptyBodyDigest() throws Exception {
        return java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(new byte[0]));
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
