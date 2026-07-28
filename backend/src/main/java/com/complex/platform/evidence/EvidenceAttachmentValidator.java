package com.complex.platform.evidence;

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

    public EvidenceAttachmentValidator(EvidenceEventRepository repository) {
        this.repository = repository;
    }

    /** 所传附件 id 必须真实存在（空列表放行——「要不要传」由各业务闸自己决定）。 */
    public void requireExisting(List<String> attachmentIds, String fieldName) {
        requireKind(attachmentIds, null, fieldName, null);
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
