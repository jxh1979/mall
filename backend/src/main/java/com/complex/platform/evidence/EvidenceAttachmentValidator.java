package com.complex.platform.evidence;

// 文件版本：V0.02

import com.complex.platform.common.api.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 附件证据存在性校验（双证据闭环）。
 *
 * <p>改造前：退酒的 {@code evidence_attachment_ids}、库存高价值复核的 photo/video 两栏，
 * <b>都只校「列表非空」</b>——传两个随手编的字符串就算「有双证据」，
 * 而且同一个假 id 可以同时填进照片栏和视频栏。本类把「所传附件必须真实存在」补上，
 * 并按服务端判定的 {@code content_type} 区分照片/视频，让「照片+视频双证据」名副其实。
 *
 * <p>先决条件：证据文件必须先经 {@code POST /api/v1/evidence/attachments/upload} 真正传上来
 * （本批一并落地），否则会出现「要求传但传不了」的窗口。
 */
// ponytail: 逐个 findAttachmentById（每次退酒/复核最多几个 id），不做批量 IN 查询——量级不值得。
@Service
public class EvidenceAttachmentValidator {

    private final EvidenceEventRepository repository;
    private final EvidenceFileStorage refundFileStorage;

    public EvidenceAttachmentValidator(EvidenceEventRepository repository) {
        this(repository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EvidenceAttachmentValidator(EvidenceEventRepository repository, EvidenceFileStorage refundFileStorage) {
        this.repository = repository;
        this.refundFileStorage = refundFileStorage;
    }

    /** 所传附件 id 必须真实存在（空列表放行——「要不要传」由各业务闸自己决定）。 */
    public void requireExisting(List<String> attachmentIds, String fieldName) {
        requireKind(attachmentIds, null, fieldName, null);
    }

    /** 退款执行/核实凭证须真实上传、绑定本笔退款，并处于调用者可访问的商户范围。 */
    public void requireRefundEvidence(List<String> attachmentIds, String refundId, String merchantScope) {
        for (String id : attachmentIds) {
            EvidenceAttachmentRecord attachment = repository.findAttachmentById(id)
                    .orElseThrow(() -> ApiException.badRequest("EVIDENCE_ATTACHMENT_NOT_FOUND", "证据附件不存在：" + id));
            if (merchantScope != null && !merchantScope.equals(attachment.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权引用其他商户的退款凭证");
            }
            if (!"finance_refund".equals(attachment.businessType()) || !refundId.equals(attachment.businessId())) {
                throw ApiException.badRequest("REFUND_EVIDENCE_BINDING_MISMATCH", "凭证必须绑定当前退款");
            }
            requireUploaded(attachment, "REFUND_EVIDENCE");
        }
    }

    /** 客户端完成登记不能代替真实文件；提交和复核均须检查存储。 */
    public void requireUploaded(EvidenceAttachmentRecord attachment) {
        requireUploaded(attachment, "EVIDENCE");
    }

    private void requireUploaded(EvidenceAttachmentRecord attachment, String errorPrefix) {
        if (!"uploaded".equals(attachment.status())) {
            throw ApiException.badRequest(errorPrefix + "_NOT_UPLOADED", "证据附件尚未完成上传");
        }
        if (refundFileStorage == null) {
            throw ApiException.conflict(errorPrefix + "_STORAGE_UNAVAILABLE", "无法核实证据附件的实际文件");
        }
        java.nio.file.Path file = refundFileStorage.resolveForRead(attachment.objectKey());
        try (java.io.InputStream input = java.nio.file.Files.newInputStream(file)) {
            long actualSize = java.nio.file.Files.size(file);
            if (actualSize <= 0 || attachment.fileSize() == null || actualSize != attachment.fileSize()) {
                throw ApiException.conflict(errorPrefix + "_FILE_MISMATCH", "证据附件实际文件与登记大小不符");
            }
            if (input.read() < 0) {
                throw ApiException.conflict(errorPrefix + "_FILE_UNREADABLE", "无法读取证据附件文件");
            }
        } catch (java.io.IOException exception) {
            throw ApiException.conflict(errorPrefix + "_FILE_UNREADABLE", "无法读取证据附件文件");
        }
    }

    /** 所传附件必须存在且为照片。 */
    public void requirePhotos(List<String> attachmentIds, String fieldName) {
        requireKind(attachmentIds, "image/", fieldName, "照片");
    }

    /** 所传附件必须存在且为视频。 */
    public void requireVideos(List<String> attachmentIds, String fieldName) {
        requireKind(attachmentIds, "video/", fieldName, "视频");
    }

    /** 高价值退品生效前，两种实际文件必须齐备。 */
    public void requirePhotoAndVideo(List<String> attachmentIds) {
        boolean photo = false;
        boolean video = false;
        for (String id : attachmentIds) {
            EvidenceAttachmentRecord attachment = repository.findAttachmentById(id)
                    .orElseThrow(() -> ApiException.badRequest("EVIDENCE_ATTACHMENT_NOT_FOUND", "证据附件不存在：" + id));
            requireUploaded(attachment);
            photo |= attachment.contentType() != null && attachment.contentType().startsWith("image/");
            video |= attachment.contentType() != null && attachment.contentType().startsWith("video/");
        }
        if (!photo || !video) {
            throw ApiException.badRequest("EVIDENCE_PHOTO_VIDEO_REQUIRED", "高价值退品须同时具备照片和视频证据");
        }
    }

    private void requireKind(List<String> attachmentIds, String contentTypePrefix,
                             String fieldName, String kindLabel) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (String id : attachmentIds) {
            EvidenceAttachmentRecord attachment = repository.findAttachmentById(id)
                    .orElseThrow(() -> ApiException.badRequest("EVIDENCE_ATTACHMENT_NOT_FOUND",
                            "证据附件不存在：" + id + "（" + fieldName + "）；请先上传证据文件再提交"));
            requireUploaded(attachment);
            if (contentTypePrefix != null
                    && (attachment.contentType() == null
                        || !attachment.contentType().startsWith(contentTypePrefix))) {
                throw ApiException.badRequest("EVIDENCE_ATTACHMENT_KIND_MISMATCH",
                        "证据附件 " + id + " 不是" + kindLabel + "（" + fieldName + "）");
            }
        }
    }
}
