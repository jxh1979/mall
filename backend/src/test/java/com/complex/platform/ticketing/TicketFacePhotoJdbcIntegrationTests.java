package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.ticketing.dto.FacePhotoConsentRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
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
        String purgeConsent = seedAcceptedFaceConsent();
        String expired = "ent_exp_" + key();
        String future = "ent_fut_" + key();
        facePhotoRepository.save("tfp_" + key(), expired, piiCipher.encrypt(PHOTO_BASE64).getBytes(StandardCharsets.UTF_8),
                "image/jpeg", now, LocalDate.now().minusDays(1), purgeConsent);   // 已过期
        facePhotoRepository.save("tfp_" + key(), future, piiCipher.encrypt(PHOTO_BASE64).getBytes(StandardCharsets.UTF_8),
                "image/jpeg", now, LocalDate.now().plusDays(1), purgeConsent);    // 未到期

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

    // ---------- ⑨ M009 F-05：订单已绑客人时，出票请求带的同意必须属于该客人——拿别人的同意拍这位客人的脸，拒 ----------
    @Test
    void issuingWithRequestPhotoAndAnotherCustomersConsentIsRejected() {
        Fixture f = seed(true, seedCustomer());
        String othersConsent = seedAcceptedFaceConsentFor(seedCustomer());
        assertThatThrownBy(() -> orderService.issueEntitlements(f.ticketTypeId, new TicketIssueRequest(
                key(), "issue", List.of(), f.orderId, "admin", List.of("dynamic_qr"), List.of(PHOTO_BASE64), othersConsent)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_CONSENT_INVALID"));
        assertThat(countPhotosForOrder(f.orderId)).isZero();
    }

    // ---------- ⑩ R-005：散客柜台同意以订单为主体落账——没有会员账号也能存一条同意（customer_id 空、order_id 非空） ----------
    @Test
    void walkInFaceConsentRowCanBeStoredWithOrderAsSubject() {
        Fixture f = seed(true);
        assertThatCode(() -> jdbcTemplate.update(
                "INSERT INTO customer_consents (id, customer_id, consent_type, accepted, version, source_terminal,"
                        + " accepted_at, created_at, updated_at, order_id)"
                        + " VALUES (?, NULL, 'ticket_face_photo', TRUE, 'v', 'counter', now(), now(), now(), ?)",
                "consent_walkin_" + key(), f.orderId)).doesNotThrowAnyException();
    }

    // ---------- ⑪ R-005：散客柜台流程——先落同意（以订单为主体）→ 出票请求带照 + 该同意 → 权益位指回它 ----------
    @Test
    void walkInConsentRecordedOnOrderIsAcceptedForThatOrdersIssue() {
        Fixture f = seed(true);
        TicketFacePhotoService.FaceConsentView consent = facePhotoService.recordWalkInFaceConsent(f.orderId, "counter", true);
        assertThat(consent.consentId()).startsWith("consent_walkin_");
        assertThat(consent.version()).isEqualTo(facePhotoService.noticeVersion());
        orderService.issueEntitlements(f.ticketTypeId, new TicketIssueRequest(
                key(), "issue", List.of(), f.orderId, "admin", List.of("dynamic_qr"), List.of(PHOTO_BASE64), consent.consentId()));
        String entitlementId = onlyEntitlementId(f.orderId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT consent_id FROM ticket_face_photos WHERE entitlement_id = ?", String.class, entitlementId))
                .isEqualTo(consent.consentId());
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT customer_id, order_id, accepted, source_terminal FROM customer_consents WHERE id = ?", consent.consentId());
        assertThat(row.get("customer_id")).isNull();
        assertThat(row.get("order_id")).isEqualTo(f.orderId);
        assertThat(row.get("accepted")).isEqualTo(true);
        assertThat(row.get("source_terminal")).isEqualTo("counter");
    }

    // ---------- ⑫ R-005：散客同意只对落账时的那张订单有效——拿 A 单的同意给 B 单出票，拒 ----------
    @Test
    void walkInConsentOfAnotherOrderIsRejected() {
        Fixture a = seed(true);
        Fixture b = seed(true);
        String consentForA = facePhotoService.recordWalkInFaceConsent(a.orderId, "counter", true).consentId();
        assertThatThrownBy(() -> orderService.issueEntitlements(b.ticketTypeId, new TicketIssueRequest(
                key(), "issue", List.of(), b.orderId, "admin", List.of("dynamic_qr"), List.of(PHOTO_BASE64), consentForA)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_CONSENT_INVALID"));
        assertThat(countPhotosForOrder(b.orderId)).isZero();
    }

    // ---------- ⑬ R-005：客人不同意 → 不落任何同意行；订单不存在 → 拒 ----------
    @Test
    void walkInConsentRefusedOrUnknownOrderIsRejected() {
        Fixture f = seed(true);
        assertThatThrownBy(() -> facePhotoService.recordWalkInFaceConsent(f.orderId, "counter", false))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_CONSENT_REQUIRED"));
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM customer_consents WHERE order_id = ?", Integer.class, f.orderId);
        assertThat(rows).isZero();
        assertThatThrownBy(() -> facePhotoService.recordWalkInFaceConsent("ticket_order_missing_" + key(), "counter", true))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("TICKET_ORDER_NOT_FOUND"));
    }

    // ---------- ⑭ F-06：商户员工（持 verification.view）取他商户权益的脸照 → CROSS_MERCHANT_FORBIDDEN、不写成功审计 ----------
    @Test
    void merchantStaffCannotViewAnotherMerchantsFacePhoto() {
        Fixture f = seed(true, null, "merchant_a");
        orderService.issueEntitlements(f.ticketTypeId, issueRequest(f, List.of(PHOTO_BASE64)));
        String entitlementId = onlyEntitlementId(f.orderId);
        long auditBefore = auditCount();
        authenticateAsMerchantStaffWithViewPermission("merchant_b");
        assertThatThrownBy(() -> facePhotoService.getForVerification(entitlementId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(auditCount()).isEqualTo(auditBefore);
    }

    // ---------- ⑮ F-06：同一商户员工取本商户权益的脸照 → 原图 + 审计 ----------
    @Test
    void merchantStaffCanViewOwnMerchantsFacePhoto() {
        Fixture f = seed(true, null, "merchant_a");
        orderService.issueEntitlements(f.ticketTypeId, issueRequest(f, List.of(PHOTO_BASE64)));
        String entitlementId = onlyEntitlementId(f.orderId);
        long auditBefore = auditCount();
        authenticateAsMerchantStaffWithViewPermission("merchant_a");
        TicketFacePhotoService.FacePhotoView view = facePhotoService.getForVerification(entitlementId);
        assertThat(view.photoBase64()).isEqualTo(PHOTO_BASE64);
        assertThat(auditCount()).isEqualTo(auditBefore + 1);
    }

    // ---------- ⑯ F-07：商户用户给他商户订单落散客同意 → CROSS_MERCHANT_FORBIDDEN、不落行 ----------
    @Test
    void merchantUserCannotRecordWalkInConsentOnAnotherMerchantsOrder() {
        Fixture f = seed(true, null, "merchant_b");
        com.complex.platform.support.TestSecurityActors.asMerchantUser("merchant_a");
        assertThatThrownBy(() -> orderService.recordWalkInFaceConsent(f.orderId,
                new FacePhotoConsentRequest(key(), true, "counter")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM customer_consents WHERE order_id = ?", Integer.class, f.orderId);
        assertThat(rows).isZero();
    }

    // ---------- ⑰ F-07：商户用户给本商户订单落散客同意 → 成功 ----------
    @Test
    void merchantUserCanRecordWalkInConsentOnOwnMerchantsOrder() {
        Fixture f = seed(true, null, "merchant_a");
        com.complex.platform.support.TestSecurityActors.asMerchantUser("merchant_a");
        TicketFacePhotoService.FaceConsentView consent = orderService.recordWalkInFaceConsent(f.orderId,
                new FacePhotoConsentRequest(key(), true, "counter"));
        assertThat(consent.consentId()).startsWith("consent_walkin_");
        assertThat(consent.orderId()).isEqualTo(f.orderId);
    }

    // ---------- ⑱ F-08：非平台、无商户绑定、却持 verification.view 的调用者 → MERCHANT_SCOPE_REQUIRED，不解密、审计不变 ----------
    @Test
    void staffWithoutMerchantBindingCannotViewFacePhotoEvenWithViewPermission() {
        Fixture f = seed(true, null, "merchant_a");
        orderService.issueEntitlements(f.ticketTypeId, issueRequest(f, List.of(PHOTO_BASE64)));
        String entitlementId = onlyEntitlementId(f.orderId);
        long auditBefore = auditCount();
        authenticateAsMerchantStaffWithViewPermission(null);
        assertThatThrownBy(() -> facePhotoService.getForVerification(entitlementId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("MERCHANT_SCOPE_REQUIRED"));
        assertThat(auditCount()).isEqualTo(auditBefore);
    }

    // ---------- ⑲ D-024：客人端（miniapp）下单需要人脸照的票种而不带照片 → 下单即拒；带满张数 + 同意 → 成单 ----------
    @Test
    void customerChannelOrderForFaceTicketRequiresPhotosAtCreation() {
        Fixture f = seed(true);
        jdbcTemplate.update("UPDATE ticket_types SET status = 'published' WHERE id = ?", f.ticketTypeId);
        assertThatThrownBy(() -> orderService.createOrder(new TicketOrderRequest(key(), null, List.of(), f.ticketTypeId, null, 1,
                LocalDate.now(), "miniapp", null, null, Map.of(), null, List.of(), null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_REQUIRED"));
        String customerId = seedCustomer();
        String consentId = seedAcceptedFaceConsentFor(customerId);
        TicketOrderRecord created = orderService.createOrder(new TicketOrderRequest(key(), null, List.of(), f.ticketTypeId, null, 1,
                LocalDate.now(), "miniapp", null, customerId, Map.of(), null, List.of(PHOTO_BASE64), consentId));
        assertThat(created.id()).isNotBlank();
        assertThat(facePhotoService.stagedForOrder(created.id())).hasSize(1);
    }

    // ---------- ⑲b D-024：柜台（admin）下单需要人脸照的票种而不带照片 → 下单即拒，无订单、库存不扣 ----------
    @Test
    void counterOrderForFaceTicketRequiresPhotosAtCreation() {
        Fixture f = seed(true);
        jdbcTemplate.update("UPDATE ticket_types SET status = 'published' WHERE id = ?", f.ticketTypeId);
        Integer soldBefore = jdbcTemplate.queryForObject("SELECT sold_quantity FROM ticket_types WHERE id = ?", Integer.class, f.ticketTypeId);
        Integer ordersBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM ticket_orders WHERE ticket_type_id = ?", Integer.class, f.ticketTypeId);
        assertThatThrownBy(() -> orderService.createOrder(new TicketOrderRequest(key(), null, List.of(), f.ticketTypeId, null, 1,
                LocalDate.now(), "admin", null, null, Map.of(), null, List.of(), null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_REQUIRED"));
        assertThat(jdbcTemplate.queryForObject("SELECT sold_quantity FROM ticket_types WHERE id = ?", Integer.class, f.ticketTypeId)).isEqualTo(soldBefore);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ticket_orders WHERE ticket_type_id = ?", Integer.class, f.ticketTypeId)).isEqualTo(ordersBefore);
    }

    // ---------- ⑳ D-024：柜台（admin）订单未留存同意 → 不能收款（支付意向拒）；留存散客同意后 → 可收款 ----------
    @Test
    void counterOrderForFaceTicketCannotTakePaymentBeforeConsent() {
        Fixture f = seed(true);
        assertThatThrownBy(() -> orderService.createPaymentIntent(f.orderId, new PaymentIntentRequest(
                key(), null, List.of(), "ticket_order", f.orderId, 5000L, "CNY", "cash")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_REQUIRED"));
        facePhotoService.recordWalkInFaceConsent(f.orderId, "counter", true);
        assertThat(orderService.createPaymentIntent(f.orderId, new PaymentIntentRequest(
                key(), null, List.of(), "ticket_order", f.orderId, 5000L, "CNY", "cash")).status()).isEqualTo("created");
    }

    // ---------- ㉑ D-024：客人端订单收款前必须已暂存满张数人脸照；无人脸照票种零影响 ----------
    @Test
    void customerOrderForFaceTicketCannotTakePaymentBeforeStagedPhotos() {
        Fixture f = seed(true);
        jdbcTemplate.update("UPDATE ticket_orders SET sales_channel = 'miniapp' WHERE id = ?", f.orderId);
        assertThatThrownBy(() -> orderService.createPaymentIntent(f.orderId, new PaymentIntentRequest(
                key(), null, List.of(), "ticket_order", f.orderId, 5000L, "CNY", "wechat")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_REQUIRED"));
        facePhotoService.stageForOrder(f.orderId, List.of(PHOTO_BASE64), 1, seedAcceptedFaceConsent());
        assertThat(orderService.createPaymentIntent(f.orderId, new PaymentIntentRequest(
                key(), null, List.of(), "ticket_order", f.orderId, 5000L, "CNY", "wechat")).status()).isEqualTo("created");
        Fixture plain = seed(false);
        assertThat(orderService.createPaymentIntent(plain.orderId, new PaymentIntentRequest(
                key(), null, List.of(), "ticket_order", plain.orderId, 5000L, "CNY", "cash")).status()).isEqualTo("created");
    }

    // ---------- ㉒ D-024：支付确认同样过闸——绕过支付意向闸直接写意向行，确认支付仍拒 ----------
    @Test
    void confirmingPaymentForFaceTicketWithoutCaptureIsRejected() {
        Fixture f = seed(true);
        Instant now = Instant.now();
        String intentId = "pay_intent_fp_" + key();
        repository.saveTicketPaymentIntent(new TicketPaymentIntentRecord(intentId, "ticket_order", f.orderId, 5000L, "CNY", "cash",
                "created", now, now));
        assertThatThrownBy(() -> orderService.confirmPayment(f.orderId, new ConfirmTicketOrderPaymentRequest(key(), intentId, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_REQUIRED"));
    }

    // ---------- ⑲c D-024 / R-005：柜台下单带满张数照片 + 客人现场确认同意 → 成单，同意以本单为主体落账，照片暂存到本单，可收款 ----------
    @Test
    void counterOrderWithPhotosAndOnSiteConsentCreatesOrderConsentAndStagedPhotos() {
        Fixture f = seed(true);
        jdbcTemplate.update("UPDATE ticket_types SET status = 'published' WHERE id = ?", f.ticketTypeId);
        TicketOrderRecord created = orderService.createOrder(new TicketOrderRequest(key(), null, List.of(), f.ticketTypeId, null, 1,
                LocalDate.now(), "admin", null, null, Map.of(), null, List.of(PHOTO_BASE64), null, true, "counter"));
        Map<String, Object> consent = jdbcTemplate.queryForMap(
                "SELECT customer_id, accepted, source_terminal FROM customer_consents WHERE order_id = ?", created.id());
        assertThat(consent.get("customer_id")).isNull();
        assertThat(consent.get("accepted")).isEqualTo(true);
        assertThat(consent.get("source_terminal")).isEqualTo("counter");
        assertThat(facePhotoService.stagedForOrder(created.id())).hasSize(1);
        assertThat(orderService.createPaymentIntent(created.id(), new PaymentIntentRequest(
                key(), null, List.of(), "ticket_order", created.id(), created.amountCent(), "CNY", "cash")).status()).isEqualTo("created");
    }

    // ---------- ⑲d D-024：柜台带照片但客人未确认同意 → 拒单，同意台账不落行 ----------
    @Test
    void counterOrderWithPhotosButNoConsentIsRejected() {
        Fixture f = seed(true);
        jdbcTemplate.update("UPDATE ticket_types SET status = 'published' WHERE id = ?", f.ticketTypeId);
        Integer consentsBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM customer_consents WHERE consent_type = 'ticket_face_photo'", Integer.class);
        assertThatThrownBy(() -> orderService.createOrder(new TicketOrderRequest(key(), null, List.of(), f.ticketTypeId, null, 1,
                LocalDate.now(), "admin", null, null, Map.of(), null, List.of(PHOTO_BASE64), null, false, "counter")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_CONSENT_REQUIRED"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM customer_consents WHERE consent_type = 'ticket_face_photo'", Integer.class)).isEqualTo(consentsBefore);
    }

    // ================= 夹具 =================

    private record Fixture(String ticketTypeId, String orderId) {
    }

    private Fixture seed(boolean requireFacePhoto) {
        return seed(requireFacePhoto, null, null);
    }

    /** customerId 非空时订单绑定该客人（M009 F-05：同意行必须属于本单客人）。 */
    private Fixture seed(boolean requireFacePhoto, String customerId) {
        return seed(requireFacePhoto, customerId, null);
    }

    /** merchantId 非空时权益模板与订单都归该商户（F-06 / F-07：商户范围闸）。 */
    private Fixture seed(boolean requireFacePhoto, String customerId, String merchantId) {
        Instant now = Instant.now();
        String suffix = key();
        String ticketTypeId = "tt_fp_" + suffix;
        repository.saveTicketType(new TicketTypeRecord(
                ticketTypeId, "face-photo-tt", "flat", 5000L, 5000L, 5000L, false, "active",
                1, 60, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        String templateId = "tpl_fp_" + suffix;
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                templateId, "amusement-tpl", requireFacePhoto ? "amusement" : "bar_drink", 1, 5000L, "active",
                merchantId, null, null, null, now, now, "virtual", null, "per_use", null, requireFacePhoto));
        repository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "bind_fp_" + suffix, ticketTypeId, templateId, 1, 1, 5000L, "default", 1, 5000L, now, now));
        // 一张真实订单：发放拆分基数 + 照片 purge_after 依赖 valid_to（票 validity 1 天）。
        String orderId = "ticket_order_fp_" + suffix;
        repository.saveTicketOrder(new TicketOrderRecord(
                orderId, "TOFP" + suffix, customerId, ticketTypeId, null, 1, LocalDate.now(), "admin", null,
                5000L, null, 5000L, 0L, "CNY", "paid", "not_issued", "created", now, now, merchantId, null, null));
        return new Fixture(ticketTypeId, orderId);
    }

    /** 带照的出票请求必须带客人同意留痕 id（M009 A1-01）：按真实链路先落一位客人 + 一条已接受的人脸照同意。 */
    private TicketIssueRequest issueRequest(Fixture f, List<String> facePhotos) {
        String consentId = facePhotos.isEmpty() ? null : seedAcceptedFaceConsent();
        return new TicketIssueRequest(key(), "issue", List.of(), f.orderId, "admin", List.of("dynamic_qr"), facePhotos,
                consentId);
    }

    private String seedAcceptedFaceConsent() {
        return seedAcceptedFaceConsentFor(seedCustomer());
    }

    private String seedCustomer() {
        Instant now = Instant.now();
        String suffix = key();
        String digits = (suffix.replaceAll("[^0-9]", "1") + "00000000").substring(0, 8);
        String customerId = "cust_fp_" + suffix;
        customerRepository.saveCustomer(new com.complex.platform.customer.CustomerRecord(
                customerId, "139" + digits, "139****" + digits.substring(4), "active", null, null, now, now, null));
        return customerId;
    }

    /** 给某位客人落一条已接受、未撤回的人脸照同意（真实台账行，可被外键与台账闸引用）。 */
    private String seedAcceptedFaceConsentFor(String customerId) {
        Instant now = Instant.now();
        String consentId = "consent_fp_" + key();
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

    /** F-06：持 verification.view 的商户员工——真实用户 / 角色 / 角色权限行（事务内，用例结束回滚）。 */
    private void authenticateAsMerchantStaffWithViewPermission(String merchantId) {
        String suffix = key();
        String userId = "usr_fp_" + suffix;
        String roleId = "role_fp_view_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', 'seed-org', ?, 'active', now())", userId, "u_" + suffix, merchantId);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId, "fp-view-" + suffix);
        jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'verification.view')", roleId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        AccessActor actor = new AccessActor(userId, "u_" + suffix, "staff", "seed-org", merchantId, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
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
