package com.complex.platform.evidence;

import java.util.List;
import java.util.Optional;

public interface EvidenceEventRepository {

    List<EvidenceEventRecord> saveAll(List<EvidenceEventRecord> records);

    List<EvidenceEventRecord> listLatest(int limit);

    EvidenceAttachmentRecord saveAttachment(EvidenceAttachmentRecord attachment);

    Optional<EvidenceAttachmentRecord> findAttachmentById(String id);

    EvidenceReviewRecord saveReview(EvidenceReviewRecord review);

    List<EvidenceTimelineItem> listTimeline(String objectType, String objectId);
}
