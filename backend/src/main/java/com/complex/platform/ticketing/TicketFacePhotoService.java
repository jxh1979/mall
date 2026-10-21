package com.complex.platform.ticketing;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.entry.FaceVerificationPort;
import com.complex.platform.entry.FaceVerificationResult;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * TK-06 一人多次票人脸绑定（BSM:780「一人多次票是同一个人多次入场用……首次核销后绑定使用人——购票人或被赠予人核销第一张起留存人脸，
 * 后续各次只允许同一人使用并以该人脸核对」；BSM:843 / ENUM:60 / PGM:891「一人多次票在首次核销时留存人脸，作为后续各次核销核对同一使用人的
 * 凭证，该人脸在该票用完或到期后删除；多人票与单次票不留人脸、不做票脸绑定。不设需要人脸照的票种；购票和转赠均不要求录入人脸照」；
 * BSM:890 采集人脸前先单独取得同意并留同意版本、时间和范围；BSM:1559 不自研人脸算法，按后台开关接入第三方供应商）。
 *
 * <p>2026-10-21 操作员裁决：①供应商不可用（未接入 / 故障）时不要求同意与拍照、不比对、照常核销；之后供应商可用且仍未绑定的，下一次入场按首次留脸；
 * ②供应商可用、需要留脸的那次入场客人不同意 → 不能入场（票仍未核销，可按现行未核销退款规则办理）。不做检票员目视核对，
 * 故不提供取照查看接口（留存人脸只供供应商比对，PGM:683、BSM:844）。
 *
 * <p><b>加密</b>：复用 {@link PiiCipher}(AES-GCM+KeyVault)。base64 图串经 PiiCipher.encrypt 得信封串，其 UTF-8 字节存 BYTEA。
 */
@Service
public class TicketFacePhotoService {

    /** 入场类权益（与 TicketCatalogService 入场类口径一致）。 */
    static final Set<String> ENTRY_TYPES = Set.of("entry", "admission");
    static final String BINDING_CAPTURE = "capture";
    static final String BINDING_MATCH = "match";

    // 留脸告知的模板版本：检票终端留脸告知（用途：核对同一使用人；保存：加密；删除：该票用完或到期后）每改一次字就 +1。
    static final String NOTICE_VERSION = "multi_use_face_notice_v1";

    // 单张人脸 base64 上限(字符数)：终端可任意构造 payload，这是信任边界，必须有硬上限。
    private static final int MAX_PHOTO_BASE64_CHARS = 4_000_000;

    private final TicketFacePhotoRepository repository;
    private final TicketingRepository ticketingRepository;
    private final PiiCipher piiCipher;
    // 第三方人脸比对供应商（entry 域路由，按后台开关选实现）。非 final：测试可替换为桩。
    FaceVerificationPort faceVerificationPort;

    public TicketFacePhotoService(TicketFacePhotoRepository repository, TicketingRepository ticketingRepository,
                                  PiiCipher piiCipher, FaceVerificationPort faceVerificationPort) {
        this.repository = repository;
        this.ticketingRepository = ticketingRepository;
        this.piiCipher = piiCipher;
        this.faceVerificationPort = faceVerificationPort;
    }

    /** 核销预检：供应商可用时，一人多次票未绑定 → capture、已绑定 → match；其余（含供应商不可用）→ null。 */
    String faceBinding(IssuedEntitlementRecord entitlement) {
        TicketInstanceRecord parent = singleHolderMultiUseParent(entitlement);
        if (parent == null || !faceVerificationPort.holderMatchAvailable()) {
            return null;
        }
        return repository.findBinding(parent.id()).isPresent() ? BINDING_MATCH : BINDING_CAPTURE;
    }

    /**
     * 扣次前的闸（规则不允许 → 拒、不扣次）。返回需在扣次成功后落库的首次留脸；无须留脸返回 null。
     * 未绑定：未同意 → MULTI_USE_FACE_CONSENT_REQUIRED；未拍照或照片非法 → MULTI_USE_FACE_CAPTURE_REQUIRED。
     * 已绑定：未拍照 → MULTI_USE_FACE_CAPTURE_REQUIRED；供应商比对不是同一人 → MULTI_USE_HOLDER_MISMATCH；当次供应商故障 → 照常核销。
     */
    PendingFace requireMultiUseHolder(IssuedEntitlementRecord entitlement, String faceCapture, Boolean consentAccepted) {
        // 剩余 0 次：本次不会扣次，交给既有的「已用完」判定（人脸绑定已随用完删除）。
        TicketInstanceRecord parent = entitlement.remainingTimes() > 0 ? singleHolderMultiUseParent(entitlement) : null;
        if (parent == null || !faceVerificationPort.holderMatchAvailable()) {
            return null;
        }
        Optional<byte[]> binding = repository.findBinding(parent.id());
        if (binding.isEmpty()) {
            if (!Boolean.TRUE.equals(consentAccepted)) {
                throw ApiException.conflict("MULTI_USE_FACE_CONSENT_REQUIRED",
                        "一人多次票首次入场须客人同意留存人脸，未同意不能入场（票仍未核销）");
            }
            return new PendingFace(parent, requireCapture(faceCapture));
        }
        String capture = requireCapture(faceCapture);
        FaceVerificationResult result = faceVerificationPort.matchHolder(
                piiCipher.decrypt(new String(binding.get(), StandardCharsets.UTF_8)), capture);
        if (result.isRejected()) {
            throw ApiException.conflict("MULTI_USE_HOLDER_MISMATCH", "与该票留存的使用人人脸不一致，不能核销");
        }
        return null;
    }

    /**
     * 扣次成功后同事务：首次留脸则落同意（主体=当前持有人，无会员时以订单为主体）并加密绑定父票；
     * 然后若该父票已无可用入场权益（用完或到期）即删除绑定。
     */
    void afterUse(IssuedEntitlementRecord used, PendingFace pending, String verificationId, String deviceId, Instant now) {
        TicketInstanceRecord parent = pending != null ? pending.parent() : singleHolderMultiUseParent(used);
        if (parent == null) {
            return;
        }
        if (pending != null) {
            String holder = ticketingRepository.findSlotHolder(parent.id(), used.slotIndex())
                    .map(TicketSlotHolderRecord::holderCustomerId)
                    .orElse(parent.currentHolderCustomerId());
            String consentId = "consent_mutf_" + UUID.randomUUID().toString().replace("-", "");
            repository.insertConsent(consentId, holder, used.orderId(), NOTICE_VERSION, deviceId, now);
            repository.saveBinding("tfp_" + UUID.randomUUID().toString().replace("-", ""), parent.id(),
                    piiCipher.encrypt(pending.photoBase64()).getBytes(StandardCharsets.UTF_8), now, consentId, verificationId);
        }
        repository.deleteIfNoUsableEntry(parent.id(), now);
    }

    /** 一人多次票：入场类权益，父票一人（num_people==1）且多次（total_times>1）。其余返回 null（单次票、多人票任何情况都不留脸）。 */
    private TicketInstanceRecord singleHolderMultiUseParent(IssuedEntitlementRecord entitlement) {
        if (entitlement.parentTicketId() == null || !ENTRY_TYPES.contains(entitlement.entitlementType())) {
            return null;
        }
        return ticketingRepository.findTicketInstanceById(entitlement.parentTicketId())
                .filter(parent -> parent.numPeople() == 1 && parent.totalTimes() > 1)
                .orElse(null);
    }

    /** 入库前的唯一校验闸：非空 + 大小封顶 + 合法 base64；不合格一律按「未拍照」拒。 */
    private static String requireCapture(String photoBase64) {
        String payload = photoBase64 == null ? "" : photoBase64.trim();
        boolean valid = !payload.isEmpty() && payload.length() <= MAX_PHOTO_BASE64_CHARS;
        if (valid) {
            try {
                Base64.getDecoder().decode(payload);
            } catch (IllegalArgumentException exception) {
                valid = false;
            }
        }
        if (!valid) {
            throw ApiException.conflict("MULTI_USE_FACE_CAPTURE_REQUIRED", "一人多次票入场须现场拍摄人脸（照片缺失或格式不正确）");
        }
        return payload;
    }

    /** 扣次成功后待落库的首次留脸。 */
    record PendingFace(TicketInstanceRecord parent, String photoBase64) {
    }

}
