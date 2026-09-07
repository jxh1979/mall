package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算重构批3-6(用户2026-07-04)：游乐票购买人脸拍照存档 端到端(真库+真 PiiCipher)。
 * 覆盖：①require_face_photo=TRUE 不带照拒；②带照存密文≠明文+可解密回原图；③=FALSE 不带照正常发放无照片行(回归)；
 *      ④取照端点有权解密+写审计、无权 403；⑤清理 job 删过期不删未到期。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TicketFacePhotoJdbcIntegrationTests {

    @Autowired private JdbcTicketingRepository repository;
    @Autowired private TicketOrderService orderService;
    @Autowired private TicketFacePhotoService facePhotoService;
    @Autowired private TicketFacePhotoRepository facePhotoRepository;
    @Autowired private TicketFacePhotoPurgeJob purgeJob;
    @Autowired private PiiCipher piiCipher;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private com.complex.platform.customer.CustomerRepository customerRepository;

    // 一张 1x1 像素的 PNG 图字节（原图），base64 后作为购买捕获的人脸照 payload。
    private static final byte[] RAW_IMAGE_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    private static final String PHOTO_BASE64 = Base64.getEncoder().encodeToString(RAW_IMAGE_BYTES);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ---------- ① require_face_photo=TRUE 不带照 → FACE_PHOTO_REQUIRED 拒 ----------
    @Test
    void issuingFaceRequiredEntitlementWithoutPhotoIsRejected() {
        Fixture f = seed(true);
        assertThatThrownBy(() -> orderService.issueEntitlements(f.ticketTypeId, issueRequest(f, List.of())))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_REQUIRED"));
        // 拒后无照片行（本用例事务回滚前可见：不应有该权益的照）。
        assertThat(countPhotosForOrder(f.orderId)).isZero();
    }

    // ---------- ② 带照发放 → 存密文≠明文、能 PiiCipher 解密回原图 ----------
    @Test
    void issuingWithPhotoStoresEncryptedAndDecryptsBackToOriginal() {
        Fixture f = seed(true);
        orderService.issueEntitlements(f.ticketTypeId, issueRequest(f, List.of(PHOTO_BASE64)));

        String entitlementId = onlyEntitlementId(f.orderId);
        byte[] stored = jdbcTemplate.queryForObject(
                "SELECT photo_ciphertext FROM ticket_face_photos WHERE entitlement_id = ?", byte[].class, entitlementId);
        assertThat(stored).isNotNull();
        // 证伪核心：存的密文字节必须 ≠ 原图字节（存库前确有加密）。
        assertThat(stored).isNotEqualTo(RAW_IMAGE_BYTES);
        // 也不得等于原图的 base64 明文字节。
        assertThat(stored).isNotEqualTo(PHOTO_BASE64.getBytes(StandardCharsets.UTF_8));
        // 密文是 PiiCipher 信封串（enc:v1: 前缀）。
        assertThat(new String(stored, StandardCharsets.UTF_8)).startsWith("enc:v1:");
        // 能解密回原图 base64、再解回原图字节。
        String decodedBase64 = piiCipher.decrypt(new String(stored, StandardCharsets.UTF_8));
        assertThat(decodedBase64).isEqualTo(PHOTO_BASE64);
        assertThat(Base64.getDecoder().decode(decodedBase64)).isEqualTo(RAW_IMAGE_BYTES);
        // M009 A1-01：权益位脸照必须指回那条同意留痕（BSM-1.7.3 留同意版本、时间和范围）。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT consent_id FROM ticket_face_photos WHERE entitlement_id = ?", String.class, entitlementId))
                .startsWith("consent_fp_");
    }

    // ---------- ③ require_face_photo=FALSE 不带照 → 正常发放、无照片行（回归） ----------
    @Test
    void issuingNonFaceEntitlementWithoutPhotoIssuesNormallyWithNoPhotoRow() {
        Fixture f = seed(false);
        IssuedTicketResponse issued = orderService.issueEntitlements(f.ticketTypeId, issueRequest(f, List.of()));
        assertThat(issued.issuedEntitlements()).hasSize(1);
        assertThat(countPhotosForOrder(f.orderId)).isZero();
    }

    // ---------- ④ 取照端点：有权角色解密得原图 + 写审计；无权角色 403 ----------
    @Test
    void getForVerificationReturnsPhotoAndWritesAuditForAuthorizedActor() {
        Fixture f = seed(true);
        orderService.issueEntitlements(f.ticketTypeId, issueRequest(f, List.of(PHOTO_BASE64)));
        String entitlementId = onlyEntitlementId(f.orderId);

        authenticateAs("platform_admin");
        long auditBefore = auditCount();
        TicketFacePhotoService.FacePhotoView view = facePhotoService.getForVerification(entitlementId);
        assertThat(view.photoBase64()).isEqualTo(PHOTO_BASE64);
        assertThat(Base64.getDecoder().decode(view.photoBase64())).isEqualTo(RAW_IMAGE_BYTES);
        // 每次查看写审计（ticket_face_photo.viewed）。
        assertThat(auditCount()).isEqualTo(auditBefore + 1);
        assertThat(latestFacePhotoViewAudit()).isEqualTo(entitlementId);
    }

    @Test
    void getForVerificationDeniesActorWithoutPermission() {
        Fixture f = seed(true);
        orderService.issueEntitlements(f.ticketTypeId, issueRequest(f, List.of(PHOTO_BASE64)));
        String entitlementId = onlyEntitlementId(f.orderId);

        authenticateAs("staff"); // 非 platform_admin、无 verification.view 授权 → 403。
        assertThatThrownBy(() -> facePhotoService.getForVerification(entitlementId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("PERMISSION_DENIED"));
    }

    // ---------- ⑤ 清理 job：删 purge_after<今天、不删未到期 ----------
    @Test
    void purgeDeletesExpiredKeepsFuture() {
        Instant now = Instant.now();
        String expired = "ent_exp_" + key();
        String future = "ent_fut_" + key();
        facePhotoRepository.save("tfp_" + key(), expired, piiCipher.encrypt(PHOTO_BASE64).getBytes(StandardCharsets.UTF_8),
                "image/jpeg", now, LocalDate.now().minusDays(1), "consent_purge_fixture");   // 已过期
        facePhotoRepository.save("tfp_" + key(), future, piiCipher.encrypt(PHOTO_BASE64).getBytes(StandardCharsets.UTF_8),
                "image/jpeg", now, LocalDate.now().plusDays(1), "consent_purge_fixture");    // 未到期

        int deleted = purgeJob.purgeExpired(LocalDate.now());
        assertThat(deleted).isGreaterThanOrEqualTo(1);
        assertThat(facePhotoRepository.findByEntitlementId(expired)).isEmpty();
        assertThat(facePhotoRepository.findByEntitlementId(future)).isPresent();
    }

    // ---------- ⑥ M009 A1-01：出票请求自带照片（柜台/后台入口）没有同意留痕 → 拒，且不落照片行 ----------
    @Test
    void issuingWithRequestPhotoButNoConsentIsRejected() {
        Fixture f = seed(true);
        assertThatThrownBy(() -> orderService.issueEntitlements(f.ticketTypeId, new TicketIssueRequest(
                key(), "issue", List.of(), f.orderId, "admin", List.of("dynamic_qr"), List.of(PHOTO_BASE64))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_CONSENT_REQUIRED"));
        assertThat(countPhotosForOrder(f.orderId)).isZero();
    }

    // ---------- ⑦ M009 A1-01：库上 CHECK 对权益位同样生效——绕过服务直接 INSERT 无同意的权益位脸照被拒 ----------
    @Test
    void entitlementPhotoRowWithoutConsentIsRejectedByDatabase() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO ticket_face_photos (id, entitlement_id, photo_ciphertext, content_type, captured_at, purge_after)"
                        + " VALUES (?, ?, ?, 'image/jpeg', now(), CURRENT_DATE + 1)",
                "tfp_no_consent_" + key(), "ent_no_consent_" + key(), "enc:v1:x".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // ---------- ⑧ M009 A1-01：出票请求带的同意 id 必须对应一条已生效的人脸照同意，瞎填的拒 ----------
    @Test
    void issuingWithRequestPhotoAndUnknownConsentIsRejected() {
        Fixture f = seed(true);
        assertThatThrownBy(() -> orderService.issueEntitlements(f.ticketTypeId, new TicketIssueRequest(
                key(), "issue", List.of(), f.orderId, "admin", List.of("dynamic_qr"), List.of(PHOTO_BASE64),
                "consent_does_not_exist")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_CONSENT_INVALID"));
        assertThat(countPhotosForOrder(f.orderId)).isZero();
    }

    // ================= 夹具 =================

    private record Fixture(String ticketTypeId, String orderId) {
    }

    private Fixture seed(boolean requireFacePhoto) {
        Instant now = Instant.now();
        String suffix = key();
        String ticketTypeId = "tt_fp_" + suffix;
        repository.saveTicketType(new TicketTypeRecord(
                ticketTypeId, "face-photo-tt", "flat", 5000L, 5000L, 5000L, false, "active",
                1, 60, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        String templateId = "tpl_fp_" + suffix;
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                templateId, "amusement-tpl", requireFacePhoto ? "amusement" : "bar_drink", 1, 5000L, "active",
                null, null, null, null, now, now, "virtual", null, "per_use", null, requireFacePhoto));
        repository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "bind_fp_" + suffix, ticketTypeId, templateId, 1, 1, 5000L, "default", 1, 5000L, now, now));
        // 一张真实订单：发放拆分基数 + 照片 purge_after 依赖 valid_to（票 validity 1 天）。
        String orderId = "ticket_order_fp_" + suffix;
        repository.saveTicketOrder(new TicketOrderRecord(
                orderId, "TOFP" + suffix, null, ticketTypeId, null, 1, LocalDate.now(), "admin", null,
                5000L, null, 5000L, 0L, "CNY", "paid", "not_issued", "created", now, now, null, null, null));
        return new Fixture(ticketTypeId, orderId);
    }

    /** 带照的出票请求必须带客人同意留痕 id（M009 A1-01）：按真实链路先落一位客人 + 一条已接受的人脸照同意。 */
    private TicketIssueRequest issueRequest(Fixture f, List<String> facePhotos) {
        String consentId = facePhotos.isEmpty() ? null : seedAcceptedFaceConsent();
        return new TicketIssueRequest(key(), "issue", List.of(), f.orderId, "admin", List.of("dynamic_qr"), facePhotos,
                consentId);
    }

    private String seedAcceptedFaceConsent() {
        Instant now = Instant.now();
        String suffix = key();
        String digits = (suffix.replaceAll("[^0-9]", "1") + "00000000").substring(0, 8);
        String customerId = "cust_fp_" + suffix;
        customerRepository.saveCustomer(new com.complex.platform.customer.CustomerRecord(
                customerId, "139" + digits, "139****" + digits.substring(4), "active", null, null, now, now, null));
        String consentId = "consent_fp_" + suffix;
        customerRepository.saveConsent(new com.complex.platform.customer.CustomerConsentRecord(
                consentId, customerId, "ticket_face_photo", true, facePhotoService.noticeVersion(), "counter",
                now, null, now, now));
        return consentId;
    }

    private String onlyEntitlementId(String orderId) {
        List<IssuedEntitlementRecord> list = repository.listIssuedEntitlementsByOrderId(orderId);
        assertThat(list).hasSize(1);
        return list.get(0).id();
    }

    private int countPhotosForOrder(String orderId) {
        Integer c = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM ticket_face_photos p
                  JOIN issued_entitlements e ON e.id = p.entitlement_id
                 WHERE e.order_id = ?
                """, Integer.class, orderId);
        return c == null ? 0 : c;
    }

    private long auditCount() {
        Long c = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'ticket_face_photo.viewed'", Long.class);
        return c == null ? 0 : c;
    }

    private String latestFacePhotoViewAudit() {
        return jdbcTemplate.queryForObject("""
                SELECT business_id FROM audit_events
                 WHERE event_name = 'ticket_face_photo.viewed'
                 ORDER BY occurred_at DESC LIMIT 1
                """, String.class);
    }

    private void authenticateAs(String accountType) {
        AccessActor actor = new AccessActor(
                "user_" + key(), "u", accountType, "seed-org", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    private String key() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
