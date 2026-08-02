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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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
 *
 * <p><b>订单暂存位(V359)</b>：客人端 H5 下单时自己拍的脸照先按订单暂存(加密同一套)，后台出票时
 * {@link TicketOrderService} 取出转绑到已发权益并删暂存。暂存 purge_after=下单日+留存天数，不出票也会被清理 job 删。
 */
@Service
public class TicketFacePhotoService {

    // 拍脸照存档保留期：票失效后 N 天自清，N 落 notification_windows(key=ticketing.face_photo_retention)后台可改，
    //   缺配置兜底 30(行为不变)。涉个人信息保护：改这个数=改生物识别照片的销毁时点，改动经 OperatingParamsService 留审计。
    private static final int RETENTION_DAYS_AFTER_VALID_TO = 30;
    private static final String RETENTION_WINDOW_KEY = "ticketing.face_photo_retention";

    // 取照查看权限：复用既有核销查看权限码(核销/运营角色持有)，无权 403。
    private static final String VIEW_PERMISSION = "verification.view";

    // V368 拍照告知的模板版本：客人端那块告知(用途/保存方式/留存天数/不拍的后果)每改一次字就 +1。
    private static final String NOTICE_TEMPLATE_VERSION = "face_photo_notice_v1";

    // 单张脸照 base64 上限(字符数)：~4MB base64 ≈ 3MB 原图，正常自拍(640x480 JPEG)只有几十 KB。
    //   客人端可任意构造 payload，这是信任边界，必须有硬上限——否则一次下单能把整个库塞满。
    private static final int MAX_PHOTO_BASE64_CHARS = 4_000_000;

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
     * V368 拍照告知的版本号：落进同意台账的 version 列，将来被问「这位客人同意的是哪一版告知」时靠它定位。
     *
     * <p>模板版本(NOTICE_TEMPLATE_VERSION) + 当时那一版实际告知的留存天数。留存天数是后台可配的，
     * 今天告知 30 天、明天运营改成 60 天，那两批客人同意的不是同一件事——所以天数进版本号，不能只记模板号。
     *
     * <p><b>改告知文案必须 bump {@link #NOTICE_TEMPLATE_VERSION}</b>：文案正文在客人端
     * apps/web/src/customer/pages/BuyTicketPage.tsx 的「本票需要入场人脸照」告知块，
     * 改了那段字而不改这里，留痕就会指向一版并不存在的告知。
     */
    String noticeVersion() {
        return NOTICE_TEMPLATE_VERSION + "@retention_" + retentionDays() + "d";
    }

    /**
     * 购买捕获：把发放请求带来的人脸照 base64 加密后存 ticket_face_photos，purge_after=valid_to+留存天数(后台可配)。
     * 由 {@link TicketOrderService} 在 require_face_photo=TRUE 的权益发放后调用；photoBase64 缺失应在调用前已拒。
     */
    void capture(String entitlementId, String photoBase64, String contentType, Instant capturedAt, Instant validTo) {
        String payload = requireValidPhoto(photoBase64);
        // 复用 PiiCipher(AES-GCM)：base64 图串 → 信封串 → UTF-8 字节存 BYTEA（禁明文存）。
        byte[] ciphertext = piiCipher.encrypt(payload).getBytes(StandardCharsets.UTF_8);
        LocalDate purgeAfter = (validTo == null ? Instant.now() : validTo)
                .atOffset(ZoneOffset.UTC).toLocalDate().plusDays(retentionDays());
        repository.save("tfp_" + UUID.randomUUID().toString().replace("-", ""),
                entitlementId, ciphertext, contentType == null ? "image/jpeg" : contentType,
                capturedAt == null ? Instant.now() : capturedAt, purgeAfter);
    }

    /**
     * V359 客人端下单暂存：把 H5 拍的脸照按订单加密暂存，等后台出票时转绑到已发权益。
     * 张数上限=订单票数(一张票一张脸)，超出直接拒——客人端可任意构造 payload，这里是信任边界。
     *
     * <p>V368 同意闸：这是「客人自己的脸」进系统的唯一一格，所以「没有同意就不该有照片」也钉在这里——
     * consentId 空一律拒，任何调用方(客人端下单/将来别的入口)都绕不过。consentId 由 CustomerService
     * 在同一个事务里往同意台账(customer_consents)落一条留痕后给出，库上 chk_tfp_consent_present 再兜一道。
     */
    void stageForOrder(String orderId, List<String> photosBase64, int quantity, String consentId) {
        if (photosBase64 == null || photosBase64.isEmpty()) {
            return;
        }
        if (consentId == null || consentId.isBlank()) {
            throw ApiException.badRequest("FACE_PHOTO_CONSENT_REQUIRED",
                    "上传人脸照片前须先留存客人同意记录");
        }
        if (photosBase64.size() > Math.max(1, quantity)) {
            throw ApiException.badRequest("FACE_PHOTO_COUNT_EXCEEDED",
                    "人脸照片数量不能超过购票数量");
        }
        LocalDate purgeAfter = LocalDate.now(ZoneOffset.UTC).plusDays(retentionDays());
        Instant now = Instant.now();
        for (int slot = 0; slot < photosBase64.size(); slot++) {
            String payload = requireValidPhoto(photosBase64.get(slot));
            byte[] ciphertext = piiCipher.encrypt(payload).getBytes(StandardCharsets.UTF_8);
            repository.saveStaged("tfp_" + UUID.randomUUID().toString().replace("-", ""),
                    orderId, slot, ciphertext, "image/jpeg", now, purgeAfter, consentId);
        }
    }

    /**
     * V359 出票取用：取某订单的暂存照(解密回 base64，按拍摄顺序)。出票链把它们当作 facePhotos 逐张消费。
     * 无暂存返回空表(=该单没拍照，走既有 FACE_PHOTO_REQUIRED 判定)。
     */
    List<String> stagedForOrder(String orderId) {
        List<byte[]> rows = repository.listStagedCiphertexts(orderId);
        List<String> photos = new ArrayList<>(rows.size());
        for (byte[] ciphertext : rows) {
            photos.add(piiCipher.decrypt(new String(ciphertext, StandardCharsets.UTF_8)));
        }
        return photos;
    }

    /** V359：出票转绑完成即删暂存（生物识别数据不多留一秒）。 */
    void discardStagedForOrder(String orderId) {
        repository.deleteStaged(orderId);
    }

    /**
     * 核销显示：取某权益的人脸照(解密返回 base64)。授权限核销/运营角色(verification.view)，无权 403；
     * 每次查看写审计(谁·何时·看了哪张权益的脸照)。核销主链不改，照片仅核销页附带展示。
     */
    public FacePhotoView getForVerification(String entitlementId) {
        // 授权：无 verification.view 权限抛 403(在 requirePermission 内)。
        authorizationService.requirePermission(VIEW_PERMISSION);
        TicketFacePhotoRepository.TicketFacePhotoRow row = repository.findByEntitlementId(entitlementId)
                .orElseThrow(() -> ApiException.notFound("FACE_PHOTO_NOT_FOUND", "该权益没有已存档的人脸照片"));
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

    /**
     * 入库前的唯一校验闸：非空 + 合法 base64 + 大小封顶。发放捕获与下单暂存都走这里，
     * 免得哪条路径漏校验就成了任意字节写入口。
     */
    private String requireValidPhoto(String photoBase64) {
        String payload = photoBase64 == null ? null : photoBase64.trim();
        if (payload == null || payload.isEmpty()) {
            // 防御：正常路径由 TicketOrderService 先拒 FACE_PHOTO_REQUIRED，不应到这。
            throw ApiException.badRequest("FACE_PHOTO_REQUIRED", "该权益必须上传人脸照片");
        }
        if (payload.length() > MAX_PHOTO_BASE64_CHARS) {
            throw ApiException.badRequest("FACE_PHOTO_TOO_LARGE", "人脸照片超过允许大小");
        }
        try {
            Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("FACE_PHOTO_INVALID", "人脸照片必须为 base64 编码的图片数据");
        }
        return payload;
    }

    /** 核销显示端点返回体：base64 图串 + content_type，前端拼 data URI 显示。 */
    public record FacePhotoView(String entitlementId, String photoBase64, String contentType) {
    }
}
