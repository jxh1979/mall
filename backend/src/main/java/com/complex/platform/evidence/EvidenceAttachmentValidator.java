package com.complex.platform.evidence;

// 文件版本：V0.01

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
            if (!"uploaded".equals(attachment.status())) {
                throw ApiException.badRequest("REFUND_EVIDENCE_NOT_UPLOADED", "退款凭证尚未完成上传");
            }
            if (refundFileStorage == null) {
                throw ApiException.conflict("REFUND_EVIDENCE_STORAGE_UNAVAILABLE", "无法核实退款凭证的实际文件");
            }
            // uploaded 元数据不足以证明对象已上传；现有本地存储必须能读取实际文件。
            // 未接入且未提供可信文件读取的直传对象，不能仅凭客户端 complete 结果用于退款。
            java.nio.file.Path file = refundFileStorage.resolveForRead(attachment.objectKey());
            try {
                long actualSize = java.nio.file.Files.size(file);
                if (actualSize <= 0 || attachment.fileSize() == null || actualSize != attachment.fileSize()) {
                    throw ApiException.conflict("REFUND_EVIDENCE_FILE_MISMATCH", "退款凭证实际文件与登记大小不符");
                }
            } catch (java.io.IOException exception) {
                throw ApiException.conflict("REFUND_EVIDENCE_FILE_UNREADABLE", "无法读取退款凭证文件");
            }
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

    private void requireKind(List<String> attachmentIds, String contentTypePrefix,
                             String fieldName, String kindLabel) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (String id : attachmentIds) {
            EvidenceAttachmentRecord attachment = repository.findAttachmentById(id)
                    .orElseThrow(() -> ApiException.badRequest("EVIDENCE_ATTACHMENT_NOT_FOUND",
                            "证据附件不存在：" + id + "（" + fieldName + "）；请先上传证据文件再提交"));
            if (contentTypePrefix != null
                    && (attachment.contentType() == null
                        || !attachment.contentType().startsWith(contentTypePrefix))) {
                throw ApiException.badRequest("EVIDENCE_ATTACHMENT_KIND_MISMATCH",
                        "证据附件 " + id + " 不是" + kindLabel + "（" + fieldName + "）");
            }
        }
    }
}
