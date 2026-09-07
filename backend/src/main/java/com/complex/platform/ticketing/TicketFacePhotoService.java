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
     * 购买捕获：把发放链交来的人脸照 base64 加密后存 ticket_face_photos，purge_after=valid_to+留存天数(后台可配)。
     * 由 {@link TicketOrderService} 在 require_face_photo=TRUE 的权益发放后调用；photoBase64 缺失应在调用前已拒。
     *
     * <p>M009 A1-01（BSM-1.7.3 采集人脸前单独取得同意并留同意版本、时间和范围）：权益位与暂存位一样必须挂同意留痕 id——
     * 客人端拍的照沿用暂存位那条同意（{@link #stagedForOrder}），出票请求自带的照由 {@link #requestPhotosWithConsent} 先验过；
     * 这里是最后一道闸，没有同意 id 一律不落库，库上 chk_tfp_consent_present(V395) 再兜一道。
     */
    void capture(String entitlementId, FacePhotoInput input, String contentType, Instant capturedAt, Instant validTo) {
        // F-05：末闸同样回查台账（同意可能在暂存与出票之间被撤回）；主体比对已在暂存/出票入口完成。
        String consentId = requireAcceptedConsent(input == null ? null : input.consentId(), null);
        String payload = requireValidPhoto(input.photoBase64());
        // 复用 PiiCipher(AES-GCM)：base64 图串 → 信封串 → UTF-8 字节存 BYTEA（禁明文存）。
        byte[] ciphertext = piiCipher.encrypt(payload).getBytes(StandardCharsets.UTF_8);
        LocalDate purgeAfter = (validTo == null ? Instant.now() : validTo)
                .atOffset(ZoneOffset.UTC).toLocalDate().plusDays(retentionDays());
        repository.save("tfp_" + UUID.randomUUID().toString().replace("-", ""),
                entitlementId, ciphertext, contentType == null ? "image/jpeg" : contentType,
                capturedAt == null ? Instant.now() : capturedAt, purgeAfter, consentId);
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
        // F-05：同意 id 可能来自员工端 POST /ticketing/orders 的请求体，必须回查台账并比对本单客人，不能只看非空。
        String acceptedConsentId = requireAcceptedConsent(consentId, orderId);
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
                    orderId, slot, ciphertext, "image/jpeg", now, purgeAfter, acceptedConsentId);
        }
    }

    /**
     * V359 出票取用：取某订单的暂存照(解密回 base64，按拍摄顺序)，连同每张照挂着的同意留痕 id 一起交给出票链逐张消费。
     * 无暂存返回空表(=该单没拍照，走既有 FACE_PHOTO_REQUIRED 判定)。
     * M009 A1-01：同意 id 必须跟着照片转绑到权益位，出票不能把「哪一版告知、何时同意」弄丢。
     */
    List<FacePhotoInput> stagedForOrder(String orderId) {
        List<TicketFacePhotoRepository.StagedRow> rows = repository.listStaged(orderId);
        List<FacePhotoInput> photos = new ArrayList<>(rows.size());
        for (TicketFacePhotoRepository.StagedRow row : rows) {
            photos.add(new FacePhotoInput(
                    piiCipher.decrypt(new String(row.ciphertext(), StandardCharsets.UTF_8)), row.consentId()));
        }
        return photos;
    }

    /**
     * M009 A1-01：出票请求自带照片（柜台/后台入口，TicketIssueRequest.facePhotos）时的同意闸。
     * 同意 id 来自请求方而非服务端同事务生成，所以必须回查同意台账：存在、类型为人脸照、已接受、未撤回，
     * 且订单已绑客人时必须属于该客人（F-05），否则拒。没带照片 → 空表，零影响。
     */
    List<FacePhotoInput> requestPhotosWithConsent(String orderId, List<String> photosBase64, String consentId) {
        if (photosBase64 == null || photosBase64.isEmpty()) {
            return List.of();
        }
        String id = requireAcceptedConsent(consentId, orderId);
        List<FacePhotoInput> photos = new ArrayList<>(photosBase64.size());
        for (String photo : photosBase64) {
            photos.add(new FacePhotoInput(photo, id));
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
        // F-06 / F-08：与核销预览（TicketVerificationService.enforceMerchantScope）同一把尺子、同一调用——平台管理员空 scope 可见全部；
        //   商户用户只能取本商户已发权益的脸照，否则不解密、不写成功审计；非平台且无商户绑定的持权者由
        //   currentMerchantScope() 抛 MERCHANT_SCOPE_REQUIRED（fail-closed，不吞异常）。取脸是已鉴权的读路径，没有回调 / 无上下文场景。
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(row.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权查看其他商户权益的人脸照片");
            }
        });
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

    /**
     * M009 F-05 唯一同意闸：暂存、出票请求自带照片、末端捕获三条路径共用。
     * 空 → FACE_PHOTO_CONSENT_REQUIRED；台账无此同意 / 非人脸照类型 / 未接受 / 已撤回 → FACE_PHOTO_CONSENT_INVALID；
     * orderId 非空且订单已绑客人时，同意行必须属于该客人（拿别人的同意拍这位客人的脸 → INVALID）。
     * 返回归一化后的同意 id。库上另有外键(V397)兜底：绕过服务也写不进台账里没有的 id。
     */
    private String requireAcceptedConsent(String consentId, String orderId) {
        if (consentId == null || consentId.isBlank()) {
            throw ApiException.badRequest("FACE_PHOTO_CONSENT_REQUIRED", "上传人脸照片前须先留存客人同意记录");
        }
        String id = consentId.trim();
        TicketFacePhotoRepository.FaceConsentSubject subject = repository.acceptedFaceConsent(id)
                .orElseThrow(() -> ApiException.badRequest("FACE_PHOTO_CONSENT_INVALID", "人脸照片同意记录不存在、未生效或已撤回"));
        if (orderId != null) {
            if (subject.customerId() != null) {
                // 会员同意：订单已绑客人时必须是同一人；无客人的订单可用在场会员本人勾的同意。
                String orderCustomer = repository.orderCustomerId(orderId).orElse(null);
                if (orderCustomer != null && !orderCustomer.equals(subject.customerId())) {
                    throw ApiException.badRequest("FACE_PHOTO_CONSENT_INVALID", "人脸照片同意记录不属于本单客人");
                }
            } else if (!orderId.equals(subject.orderId())) {
                // R-005 散客同意：只对落账时的那张订单有效。
                throw ApiException.badRequest("FACE_PHOTO_CONSENT_INVALID", "人脸照片同意记录不属于本单");
            }
        }
        return id;
    }

    /**
     * R-005（操作员 2026-09-07）：柜台出票现场为无会员身份的散客留存人脸照同意。客人在柜台设备上看过告知并勾选同意后，
     * 由出票人员提交；同意以订单为主体落 customer_consents（consent_type=ticket_face_photo，版本=当时那一版告知，
     * 购票人手机号由订单 buyer_phone 承载），返回同意 id 供随后的出票请求 face_consent_id 使用。
     * 未同意一律拒、不落任何行；订单不存在拒。审计与幂等由 TicketingWriteOperation.RECORD_FACE_PHOTO_CONSENT 承担。
     */
    public FaceConsentView recordWalkInFaceConsent(String orderId, String sourceTerminal, boolean accepted) {
        if (!accepted) {
            throw ApiException.badRequest("FACE_PHOTO_CONSENT_REQUIRED", "客人未同意拍摄人脸照片，不能拍照出票");
        }
        if (orderId == null || orderId.isBlank() || !repository.orderExists(orderId)) {
            throw ApiException.notFound("TICKET_ORDER_NOT_FOUND", "订单不存在或已被删除");
        }
        String terminal = sourceTerminal == null || sourceTerminal.isBlank() ? "counter" : sourceTerminal.trim();
        Instant now = Instant.now();
        String consentId = "consent_walkin_" + UUID.randomUUID().toString().replace("-", "");
        String version = noticeVersion();
        repository.insertWalkInFaceConsent(consentId, orderId, version, terminal, now);
        return new FaceConsentView(consentId, orderId, version, now);
    }

    /** 散客柜台同意落账结果：同意 id、主体订单、当时那一版告知、同意时刻。 */
    public record FaceConsentView(String consentId, String orderId, String version, Instant acceptedAt) {
    }

    /** 出票链交给存档的一张脸照：图 base64 + 这张脸对应的同意留痕 id(customer_consents.id)。 */
    public record FacePhotoInput(String photoBase64, String consentId) {
    }

    /** 核销显示端点返回体：base64 图串 + content_type，前端拼 data URI 显示。 */
    public record FacePhotoView(String entitlementId, String photoBase64, String contentType) {
    }
}
