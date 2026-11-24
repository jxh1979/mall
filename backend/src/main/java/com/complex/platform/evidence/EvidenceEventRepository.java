package com.complex.platform.evidence;

import java.util.List;
import java.util.Optional;
import com.complex.platform.common.api.PageResponse;

public interface EvidenceEventRepository {

    List<EvidenceEventRecord> saveAll(List<EvidenceEventRecord> records);

    /** merchantId null = no filter (platform admin); non-null = merchant-scoped. */
    List<EvidenceEventRecord> listLatest(int limit, String merchantId);

    /**
     * 服务端真分页(按 received_at DESC 稳定排序),返回真实 total,让用户可翻看更早历史(此前写死 limit=20 无法翻页)。
     * merchantId null=平台全量,非空=商户 scope(WHERE merchant_id=)——租户隔离谓词与 listLatest 同口径。
     * default 兜底基于 listLatest 内存切片供测试假仓储用;生产 Jdbc 实现 @Override 为真 SQL LIMIT/OFFSET + COUNT。
     */
    default PageResponse<EvidenceEventRecord> listPage(int page, int size, String merchantId) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 20 : size;
        List<EvidenceEventRecord> all = listLatest(Integer.MAX_VALUE, merchantId);
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(all.subList(from, to), safePage, safeSize, all.size());
    }

    /**
     * 第十八批 BA 段第 324 条 324-R2：projectScope 非 null 时只取商户归属该项目集合的证据事件（空集合为空页），启用中的综合体都在该集合内时另含不带商户的证据事件（第 370 条 370-R1）；null 同 3 参。
     */
    default PageResponse<EvidenceEventRecord> listPage(int page, int size, String merchantId, java.util.Set<String> projectScope) {
        return projectScope == null ? listPage(page, size, merchantId)
                : new PageResponse<>(List.of(), page < 1 ? 1 : page, size < 1 ? 20 : size, 0);
    }

    EvidenceAttachmentRecord saveAttachment(EvidenceAttachmentRecord attachment);

    Optional<EvidenceAttachmentRecord> findAttachmentById(String id);

    EvidenceReviewRecord saveReview(EvidenceReviewRecord review);

    /** merchantId null = no filter (platform admin); non-null = merchant-scoped. */
    List<EvidenceTimelineItem> listTimeline(String objectType, String objectId, String merchantId);
}
