package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.entry.FaceVerificationPort;
import com.complex.platform.entry.FaceVerificationRequest;
import com.complex.platform.entry.FaceVerificationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * TK-06 一人多次票首次核销留脸、后续核对同一使用人（BSM:780「购票人或被赠予人核销第一张起留存人脸，后续各次只允许同一人使用并以该人脸核对」；
 * BSM:843 / ENUM:60 / PGM:891「一人多次票在首次核销时留存人脸……该人脸在该票用完或到期后删除；多人票与单次票不留人脸、不做票脸绑定」；
 * BSM:890「采集证件、人脸……前先单独取得同意，并留同意版本、时间和范围」；BSM:1559 不自研人脸算法、按开关接入第三方供应商；
 * 2026-10-21 操作员裁决：供应商不可用时不留不核、照常核销，恢复后从下一次入场开始留脸；不同意就不能首次入场）。
 *
 * <p>真库 + MockMvc，请求按终端真实形状（蛇形键）。人脸供应商以桩替换 {@link TicketFacePhotoService} 的端口，用例结束复原。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MultiUseTicketFaceBindingJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    // 1x1 PNG 的 base64：终端拍下的入场人脸。
    private static final String FACE = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    private static final String OTHER_FACE = "b3RoZXItZmFjZQ==";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private TicketFacePhotoService facePhotoService;
    @Autowired private TicketFacePhotoPurgeJob purgeJob;
    @Autowired private PiiCipher piiCipher;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String merchant = "mch_tk06_" + suffix;
    private final String deviceId = "dev_tk06_" + suffix;
    private final StubFacePort stub = new StubFacePort();
    private Object faceTarget;
    private Object originalPort;
    private String buyer;
    private RequestPostProcessor verifier;
    private int seq;

    @BeforeEach
    void seed() {
        faceTarget = AopTestUtils.getUltimateTargetObject(facePhotoService);
        originalPort = ReflectionTestUtils.getField(faceTarget, "faceVerificationPort");
        ReflectionTestUtils.setField(faceTarget, "faceVerificationPort", stub);
        buyer = customer("buyer");
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '多次票留脸测试票', 'flat', 'published', now(), now())", "tt_tk06_" + suffix);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " created_at, updated_at) VALUES (?, '入场', 'entry', 3, 0, 'active', now(), now())", "et_tk06_" + suffix);
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', now(), now())", deviceId, "hw-" + deviceId, "DC-" + deviceId);
        verifier = staff("tk06_verifier_" + suffix, merchant);
    }

    @AfterEach
    void restore() {
        ReflectionTestUtils.setField(faceTarget, "faceVerificationPort", originalPort);
        SecurityContextHolder.clearContext();
    }

    // R2 RED：供应商可用、未绑定的一人多次票首次入场不同意留脸 → 拒（规则不允许）、不扣次、不留任何人脸或同意。
    @Test
    void multiUseFirstVerificationWithoutConsentIsRejectedAndNotConsumed() throws Exception {
        stub.available = true;
        Ticket t = ticket(1, 3);
        verify(t, FACE, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MULTI_USE_FACE_CONSENT_REQUIRED"));
        verify(t, FACE, false).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MULTI_USE_FACE_CONSENT_REQUIRED"));
        // 同意了但没拍照 → 拒。
        verify(t, null, true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MULTI_USE_FACE_CAPTURE_REQUIRED"));
        assertThat(usedTimes(t)).isZero();
        assertThat(faceConsents()).isZero();
        assertThat(TicketVerificationService.outcomeStatusForCode("MULTI_USE_FACE_CONSENT_REQUIRED")).isEqualTo("rejected");
        assertThat(TicketVerificationService.outcomeStatusForCode("MULTI_USE_FACE_CAPTURE_REQUIRED")).isEqualTo("rejected");
        assertThat(TicketVerificationService.outcomeStatusForCode("MULTI_USE_HOLDER_MISMATCH")).isEqualTo("rejected");
    }

    // R2 RED：同意 + 拍照 → 核销成功扣一次；同事务落同意（版本、时间、范围）与加密人脸，绑定父票实例。
    @Test
    void multiUseFirstVerificationStoresEncryptedFaceBoundToTicketInstanceWithConsent() throws Exception {
        stub.available = true;
        Ticket t = ticket(1, 3);
        verify(t, FACE, true).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));

        assertThat(faceConsents()).isEqualTo(1);
        Map<String, Object> consent = jdbc.queryForMap("SELECT * FROM customer_consents WHERE consent_type = 'multi_use_ticket_face'"
                + " AND customer_id = ?", buyer);
        assertThat(consent.get("accepted")).isEqualTo(true);
        assertThat(consent.get("version")).isEqualTo("multi_use_face_notice_v1");
        assertThat(consent.get("accepted_at")).isNotNull();
        assertThat(consent.get("source_terminal")).isEqualTo(deviceId);
        assertThat(usedTimes(t)).isEqualTo(1);
        Map<String, Object> binding = jdbc.queryForMap("SELECT * FROM ticket_face_photos WHERE ticket_instance_id = ?", t.parentId);
        byte[] stored = (byte[]) binding.get("photo_ciphertext");
        assertThat(new String(stored, StandardCharsets.UTF_8)).startsWith("enc:v1:").doesNotContain(FACE);
        assertThat(piiCipher.decrypt(new String(stored, StandardCharsets.UTF_8))).isEqualTo(FACE);
        assertThat(binding.get("consent_id")).isEqualTo(consent.get("id"));
        assertThat(binding.get("first_verification_id")).isEqualTo(
                jdbc.queryForObject("SELECT id FROM ticket_verifications WHERE entitlement_id = ? AND status = 'success'",
                        String.class, t.entitlementId));
    }

    // R2 RED：转赠领取后由受赠人首次核销 → 同意主体是受赠人（当前持有人），不是购票人。
    @Test
    void giftedMultiUseFirstVerificationBindsRecipient() throws Exception {
        stub.available = true;
        Ticket t = ticket(1, 3);
        String recipient = customer("recipient");
        jdbc.update("UPDATE ticket_slot_holders SET holder_customer_id = ? WHERE parent_ticket_id = ?", recipient, t.parentId);
        jdbc.update("UPDATE ticket_instances SET current_holder_customer_id = ? WHERE id = ?", recipient, t.parentId);

        verify(t, FACE, true).andExpect(status().isOk());

        assertThat(jdbc.queryForList("SELECT customer_id FROM customer_consents WHERE consent_type = 'multi_use_ticket_face'"
                + " AND customer_id IN (?, ?)", String.class, buyer, recipient)).containsExactly(recipient);
    }

    // 单次票、多人票任何情况都不留脸：即使终端带了照片与同意也不落库、不要求同意。
    @Test
    void singleUseAndMultiPersonTicketsNeverStoreFace() throws Exception {
        stub.available = true;
        Ticket single = ticket(1, 1);
        Ticket multiPerson = ticket(2, 3);
        verify(single, null, null).andExpect(status().isOk());
        verify(multiPerson, null, null).andExpect(status().isOk());
        verify(multiPerson, FACE, true).andExpect(status().isOk());
        assertThat(faceConsents()).isZero();
        assertThat(stub.matchCalls).isZero();
        preview(single).andExpect(jsonPath("$.data.face_binding").doesNotExist());
        preview(multiPerson).andExpect(jsonPath("$.data.face_binding").doesNotExist());
    }

    // R5 RED：预检只在供应商可用时给 face_binding：未绑定 capture、已绑定 match；默认（供应商未接入）为空。
    @Test
    void previewReturnsFaceBindingCaptureThenMatch() throws Exception {
        Ticket t = ticket(1, 3);
        // 默认路由（V146 开关关 / 开而未接入真实供应商）→ 视为不可用。
        ReflectionTestUtils.setField(faceTarget, "faceVerificationPort", originalPort);
        preview(t).andExpect(status().isOk()).andExpect(jsonPath("$.data.face_binding").doesNotExist());
        ReflectionTestUtils.setField(faceTarget, "faceVerificationPort", stub);
        stub.available = true;
        preview(t).andExpect(jsonPath("$.data.face_binding").value("capture"));
        verify(t, FACE, true).andExpect(status().isOk());
        preview(t).andExpect(jsonPath("$.data.face_binding").value("match"));
        stub.available = false;
        preview(t).andExpect(jsonPath("$.data.face_binding").doesNotExist());
    }

    // R3 RED：已绑定 → 供应商比对不一致 → 拒、不扣次。
    @Test
    void multiUseSecondVerificationMismatchRejectedWithoutConsuming() throws Exception {
        stub.available = true;
        Ticket t = ticket(1, 3);
        verify(t, FACE, true).andExpect(status().isOk());
        stub.matchStatus = FaceVerificationResult.Status.FAILED;
        verify(t, OTHER_FACE, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MULTI_USE_HOLDER_MISMATCH"));
        // 已绑定却不拍照 → 拒。
        verify(t, null, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MULTI_USE_FACE_CAPTURE_REQUIRED"));
        assertThat(usedTimes(t)).isEqualTo(1);
    }

    // R3：比对一致 → 扣一次；比对拿到的是解密后的留存人脸与本次拍照，同意不再重复落账。
    @Test
    void multiUseSecondVerificationMatchConsumes() throws Exception {
        stub.available = true;
        Ticket t = ticket(1, 3);
        verify(t, FACE, true).andExpect(status().isOk());
        verify(t, OTHER_FACE, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        assertThat(stub.matchCalls).isEqualTo(1);
        assertThat(stub.lastStored).isEqualTo(FACE);
        assertThat(stub.lastCapture).isEqualTo(OTHER_FACE);
        assertThat(usedTimes(t)).isEqualTo(2);
        assertThat(faceConsents()).isEqualTo(1);
    }

    // R3 RED（裁决第 1 条）：供应商不可用 → 不要求同意与拍照、不比对、照常核销；恢复后仍未绑定 → 下一次入场按首次留脸；
    // 已绑定期间供应商再次不可用 → 不比对、照常核销。
    @Test
    void vendorUnavailableVerifiesWithoutFaceThenNextEntryCapturesWhenAvailable() throws Exception {
        Ticket t = ticket(1, 3);
        stub.available = false;
        verify(t, null, null).andExpect(status().isOk());
        assertThat(usedTimes(t)).isEqualTo(1);
        assertThat(faceConsents()).isZero();

        stub.available = true;
        verify(t, null, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MULTI_USE_FACE_CONSENT_REQUIRED"));
        verify(t, FACE, true).andExpect(status().isOk());
        assertThat(usedTimes(t)).isEqualTo(2);
        assertThat(bindings(t)).isEqualTo(1);

        stub.available = false;
        stub.matchStatus = FaceVerificationResult.Status.FAILED;
        verify(t, null, null).andExpect(status().isOk());
        assertThat(stub.matchCalls).isZero();
        assertThat(usedTimes(t)).isEqualTo(3);
    }

    // R4 RED：三次票第三次核销后，绑定在同一事务里删除（该票用完）。
    @Test
    void lastUseDeletesFaceBinding() throws Exception {
        stub.available = true;
        Ticket t = ticket(1, 3);
        verify(t, FACE, true).andExpect(status().isOk());
        assertThat(faceConsents()).isEqualTo(1);
        assertThat(bindings(t)).isEqualTo(1);
        verify(t, FACE, null).andExpect(status().isOk());
        assertThat(bindings(t)).isEqualTo(1);
        verify(t, FACE, null).andExpect(status().isOk());
        assertThat(usedTimes(t)).isEqualTo(3);
        assertThat(bindings(t)).isZero();
        // 同意记录照常保留（BSM:844 保留必要的同意记录）。
        assertThat(faceConsents()).isEqualTo(1);
    }

    // R4 RED：到期仍有剩余次数 → 清理任务删除；同父票补偿补发的入场权益仍在有效期内 → 保留到它到期。
    @Test
    void expiredMultiUseFaceBindingPurgedByJobButKeptForReissuedEntitlement() throws Exception {
        stub.available = true;
        Ticket t = ticket(1, 3);
        verify(t, FACE, true).andExpect(status().isOk());
        assertThat(faceConsents()).isEqualTo(1);
        jdbc.update("UPDATE issued_entitlements SET valid_to = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(3 * 86_400)), t.entitlementId);
        String reissued = entitlement(t.parentId, t.orderId, 1, Instant.now().plusSeconds(5 * 86_400));

        purgeJob.purgeExpired(LocalDate.now());
        assertThat(bindings(t)).isEqualTo(1);

        jdbc.update("UPDATE issued_entitlements SET valid_to = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(2 * 86_400)), reissued);
        purgeJob.purgeExpired(LocalDate.now());
        assertThat(bindings(t)).isZero();
    }

    // 2026-10-21 操作员裁决补充第 1 条落地口径「不做『检票员目视核对』分支」：留存人脸只供供应商机器比对，不提供取照查看接口
    // （PGM:683 查看人脸等个人信息须先审批并全程留痕；BSM:844 原始人脸按业务必要性最短留存）。
    @Test
    void faceBindingHasNoViewEndpoint() throws Exception {
        stub.available = true;
        Ticket t = ticket(1, 3);
        verify(t, FACE, true).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ticketing/issued-entitlements/" + t.entitlementId + "/face-photo").with(verifier))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("NOT_FOUND"));
    }

    // ================= 夹具 =================

    private record Ticket(String parentId, String orderId, String entitlementId, String code) {
    }

    private Ticket ticket(int numPeople, int totalTimes) {
        String n = suffix + "_" + (++seq);
        String orderId = "ord_tk06_" + n;
        String parentId = "tkt_tk06_" + n;
        jdbc.update("INSERT INTO ticket_orders (id, order_no, customer_id, ticket_type_id, quantity, sales_channel, amount_cent,"
                + " pay_status, issue_status, status, created_at, updated_at, merchant_id) VALUES (?, ?, ?, ?, 1, 'miniapp', 5000,"
                + " 'paid', 'issued', 'created', now(), now(), ?)", orderId, "ON-" + n, buyer, "tt_tk06_" + suffix, merchant);
        jdbc.update("INSERT INTO ticket_instances (id, ticket_no, order_id, ticket_type_id, seq_in_order, num_people, total_times,"
                + " buyer_customer_id, current_holder_customer_id, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?, 'active', now(), now())",
                parentId, "T-" + n, orderId, "tt_tk06_" + suffix, numPeople, totalTimes, buyer, buyer);
        jdbc.update("INSERT INTO ticket_slot_holders (parent_ticket_id, slot_index, holder_customer_id, created_at, updated_at)"
                + " VALUES (?, 1, ?, now(), now())", parentId, buyer);
        String entitlementId = entitlement(parentId, orderId, totalTimes, Instant.now().plusSeconds(86_400));
        String code = jdbc.queryForObject("SELECT entitlement_code FROM issued_entitlements WHERE id = ?", String.class, entitlementId);
        return new Ticket(parentId, orderId, entitlementId, code);
    }

    private String entitlement(String parentId, String orderId, int totalTimes, Instant validTo) {
        String id = "ent_tk06_" + suffix + "_" + (++seq);
        jdbc.update("INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,"
                + " entitlement_type, total_times, used_times, status, valid_from, valid_to, created_at, updated_at, merchant_id,"
                + " num_people, parent_ticket_id, slot_index) VALUES (?, ?, ?, ?, ?, 'entry', ?, 0, 'active', ?, ?, now(), now(), ?, 1, ?, 1)",
                id, "tt_tk06_" + suffix, orderId, "et_tk06_" + suffix, "TK06-" + id, totalTimes,
                Timestamp.from(Instant.now().minusSeconds(86_400)), Timestamp.from(validTo), merchant, parentId);
        return id;
    }

    // 终端真实请求形状：蛇形键；不同意 / 不拍照时对应键不出现或为 false。
    private ResultActions verify(Ticket t, String faceCapture, Boolean consent) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/verifications").with(verifier)
                .contentType(MediaType.APPLICATION_JSON).content(body(t, faceCapture, consent)));
    }

    private ResultActions preview(Ticket t) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/verifications/preview").with(verifier)
                .contentType(MediaType.APPLICATION_JSON).content(body(t, null, null)));
    }

    private String body(Ticket t, String faceCapture, Boolean consent) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tk06-" + suffix + "-" + (++seq));
        body.put("credential_type", "qr");
        body.put("credential_payload", t.code);
        body.put("device_id", deviceId);
        body.put("point_id", "point_tk06");
        body.put("business_context", Map.of("scenario", "ticket_checkin"));
        if (faceCapture != null) {
            body.put("face_capture", faceCapture);
        }
        if (consent != null) {
            body.put("face_consent_accepted", consent);
        }
        return JSON.writeValueAsString(body);
    }

    private int usedTimes(Ticket t) {
        return jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, t.entitlementId);
    }

    private int bindings(Ticket t) {
        return jdbc.queryForObject("SELECT count(*) FROM ticket_face_photos WHERE ticket_instance_id = ?", Integer.class, t.parentId);
    }

    private int faceConsents() {
        return jdbc.queryForObject("SELECT count(*) FROM customer_consents WHERE consent_type = 'multi_use_ticket_face'"
                + " AND customer_id IN (SELECT id FROM customers WHERE id LIKE ?)", Integer.class, "cus_tk06_%" + suffix);
    }

    private String customer(String role) {
        String id = "cus_tk06_" + role + "_" + suffix;
        String digits = String.format("%08d", Math.floorMod((role + suffix).hashCode(), 100_000_000));
        Instant now = Instant.now();
        customerRepository.saveCustomer(new CustomerRecord(id, "137" + digits, "137****" + digits.substring(4),
                "active", null, null, now, now));
        return id;
    }

    private RequestPostProcessor staff(String userId, String merchantId) {
        return staff(userId, merchantId, "verification.execute", "verification.view");
    }

    private RequestPostProcessor staff(String userId, String merchantId, String... permissions) {
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '多次票留脸回归', 'project', 'active', now())",
                role, role);
        for (String permission : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, permission);
        }
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', 'org_tk06', ?, 'active', now())", userId, userId, merchantId);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "staff", "org_tk06", merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 第三方人脸供应商桩：可用与否、比对结果可控，并记下比对入参。 */
    static final class StubFacePort implements FaceVerificationPort {
        boolean available;
        FaceVerificationResult.Status matchStatus = FaceVerificationResult.Status.PASSED;
        int matchCalls;
        String lastStored;
        String lastCapture;

        @Override
        public FaceVerificationResult verifyEntry(FaceVerificationRequest request) {
            return FaceVerificationResult.reserved("stub");
        }

        @Override
        public boolean holderMatchAvailable() {
            return available;
        }

        @Override
        public FaceVerificationResult matchHolder(String storedPhotoBase64, String capturedPhotoBase64) {
            matchCalls++;
            lastStored = storedPhotoBase64;
            lastCapture = capturedPhotoBase64;
            return matchStatus == FaceVerificationResult.Status.PASSED
                    ? FaceVerificationResult.passed("stub")
                    : FaceVerificationResult.failed("stub", "holder_mismatch", "不是同一人");
        }
    }
}
