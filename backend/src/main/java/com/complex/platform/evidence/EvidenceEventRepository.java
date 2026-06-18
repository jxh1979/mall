package com.complex.platform.evidence;

import java.util.List;
import java.util.Optional;

public interface EvidenceEventRepository {

    List<EvidenceEventRecord> saveAll(List<EvidenceEventRecord> records);

    /** merchantId null = no filter (platform admin); non-null = merchant-scoped. */
    List<EvidenceEventRecord> listLatest(int limit, String merchantId);

    EvidenceAttachmentRecord saveAttachment(EvidenceAttachmentRecord attachment);

    Optional<EvidenceAttachmentRecord> findAttachmentById(String id);

    EvidenceReviewRecord saveReview(EvidenceReviewRecord review);

    /** merchantId null = no filter (platform admin); non-null = merchant-scoped. */
    List<EvidenceTimelineItem> listTimeline(String objectType, String objectId, String merchantId);
}
