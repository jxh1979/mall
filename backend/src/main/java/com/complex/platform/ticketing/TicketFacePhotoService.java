package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.NotificationWindowConfig;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.evidence.AuditEventService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 结算重构批3-6(用户2026-07-04)：游乐票购买人脸拍照存档。
 *
 * <p>口径：游乐票购买时做人脸校验、拍一张照片加密存档，核销时后台显示这张照片，核销员确认核销的是本人(防转票)。
 * 与 entry/ 人脸入场闸(判 18+ 年龄闸)是两回事——本能力=票面人脸照绑定+核销显示，独立。
 *
 * <p><b>加密</b>：复用 {@link PiiCipher}(AES-GCM+KeyVault)，不自己写加密。base64 图串经 PiiCipher.encrypt 得信封串，
 * 其 UTF-8 字节存 BYTEA；取时字节→String→PiiCipher.decrypt→base64 图串。生物识别敏感=存必加密、取必授权+审计。
 *
 * <p><b>保留期</b>：purge_after = 票 valid_to + 留存天数(后台可配，默认 30 天；PIPL 越短越安全)；
 * 到期由 {@link TicketFacePhotoPurgeJob} 硬删。
 */
@Service
public class TicketFacePhotoService {

    // 拍脸照存档保留期：票失效后 N 天自清，N 落 notification_windows(key=ticketing.face_photo_retention)后台可改，
    //   缺配置兜底 30(行为不变)。涉个人信息保护：改这个数=改生物识别照片的销毁时点，改动经 OperatingParamsService 留审计。
    private static final int RETENTION_DAYS_AFTER_VALID_TO = 30;
    private static final String RETENTION_WINDOW_KEY = "ticketing.face_photo_retention";

    // 取照查看权限：复用既有核销查看权限码(核销/运营角色持有)，无权 403。
    private static final String VIEW_PERMISSION = "verification.view";

    private final TicketFacePhotoRepository repository;
    private final PiiCipher piiCipher;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final NotificationWindowConfig windowConfig;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketFacePhotoService(TicketFacePhotoRepository repository, PiiCipher piiCipher,
                                  AuthorizationService authorizationService, AuditEventService auditEventService,
                                  NotificationWindowConfig windowConfig) {
        this.repository = repository;
        this.piiCipher = piiCipher;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.windowConfig = windowConfig;
    }

    /** 测试便捷构造:留存天数回落默认 30。 */
    public TicketFacePhotoService(TicketFacePhotoRepository repository, PiiCipher piiCipher,
                                  AuthorizationService authorizationService, AuditEventService auditEventService) {
        this(repository, piiCipher, authorizationService, auditEventService, new NotificationWindowConfig());
    }

    /** 当前生效的人脸照留存天数(后台可配，缺配置 30)。 */
    int retentionDays() {
        return windowConfig.days(RETENTION_WINDOW_KEY, RETENTION_DAYS_AFTER_VALID_TO);
    }

    /**
     * 购买捕获：把发放请求带来的人脸照 base64 加密后存 ticket_face_photos，purge_after=valid_to+留存天数(后台可配)。
     * 由 {@link TicketOrderService} 在 require_face_photo=TRUE 的权益发放后调用；photoBase64 缺失应在调用前已拒。
     */
    void capture(String entitlementId, String photoBase64, String contentType, Instant capturedAt, Instant validTo) {
        String payload = photoBase64 == null ? null : photoBase64.trim();
        if (payload == null || payload.isEmpty()) {
            // 防御：正常路径由 TicketOrderService 先拒 FACE_PHOTO_REQUIRED，不应到这。
            throw ApiException.badRequest("FACE_PHOTO_REQUIRED", "face photo payload is required for this entitlement");
        }
        // 复用 PiiCipher(AES-GCM)：base64 图串 → 信封串 → UTF-8 字节存 BYTEA（禁明文存）。
        byte[] ciphertext = piiCipher.encrypt(payload).getBytes(StandardCharsets.UTF_8);
        LocalDate purgeAfter = (validTo == null ? Instant.now() : validTo)
                .atOffset(ZoneOffset.UTC).toLocalDate().plusDays(retentionDays());
        repository.save("tfp_" + UUID.randomUUID().toString().replace("-", ""),
                entitlementId, ciphertext, contentType == null ? "image/jpeg" : contentType,
                capturedAt == null ? Instant.now() : capturedAt, purgeAfter);
    }

    /**
     * 核销显示：取某权益的人脸照(解密返回 base64)。授权限核销/运营角色(verification.view)，无权 403；
     * 每次查看写审计(谁·何时·看了哪张权益的脸照)。核销主链不改，照片仅核销页附带展示。
     */
    public FacePhotoView getForVerification(String entitlementId) {
        // 授权：无 verification.view 权限抛 403(在 requirePermission 内)。
        authorizationService.requirePermission(VIEW_PERMISSION);
        TicketFacePhotoRepository.TicketFacePhotoRow row = repository.findByEntitlementId(entitlementId)
                .orElseThrow(() -> ApiException.notFound("FACE_PHOTO_NOT_FOUND", "no face photo archived for this entitlement"));
        // 审计：敏感生物识别每次查看留痕(actor 由 AuditEventService 从安全上下文取)。
        auditEventService.recordSuccess(
                "ticket_face_photo.viewed",
                VIEW_PERMISSION,
                "issued_entitlement",
                entitlementId,
                null,
                null,
                Map.of("entitlement_id", entitlementId));
        String base64 = piiCipher.decrypt(new String(row.ciphertext(), StandardCharsets.UTF_8));
        // 防御：确保返回值是合法 base64（PiiCipher 解密失败会先 GCM 抛错，这里只兜格式）。
        Base64.getDecoder().decode(base64);
        return new FacePhotoView(entitlementId, base64, row.contentType());
    }

    /** 核销显示端点返回体：base64 图串 + content_type，前端拼 data URI 显示。 */
    public record FacePhotoView(String entitlementId, String photoBase64, String contentType) {
    }
}
