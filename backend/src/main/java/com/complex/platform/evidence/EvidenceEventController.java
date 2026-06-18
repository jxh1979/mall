package com.complex.platform.evidence;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.evidence.dto.EvidenceAttachmentBindRequest;
import com.complex.platform.evidence.dto.EvidenceAttachmentCompleteRequest;
import com.complex.platform.evidence.dto.EvidenceAttachmentUploadIntentRequest;
import com.complex.platform.evidence.dto.EvidenceReviewRequest;
import com.complex.platform.evidence.dto.EventBatchRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceEventController {

    private final EvidenceEventService service;
    private final AuthorizationService authorizationService;

    public EvidenceEventController(EvidenceEventService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/events")
    public ApiResponse<EventBatchResponse> createEvents(@RequestBody EventBatchRequest request) {
        authorizationService.requirePermission("evidence.event.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createEvents(request));
    }

    @GetMapping("/events")
    public ApiResponse<List<EvidenceEventRecord>> listEvents(@RequestParam(defaultValue = "20") int limit) {
        authorizationService.requirePermission("evidence.event.view");
        return ApiResponse.ok(service.listEvents(limit));
    }

    @PostMapping("/attachments/upload-intents")
    public ApiResponse<EvidenceAttachmentRecord> createAttachmentUploadIntent(
            @RequestBody EvidenceAttachmentUploadIntentRequest request
    ) {
        authorizationService.requirePermission("evidence.attachment.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createAttachmentUploadIntent(request));
    }

    @PostMapping("/attachments/{attachment_id}/bind")
    public ApiResponse<EvidenceAttachmentRecord> bindAttachment(
            @PathVariable("attachment_id") String attachmentId,
            @RequestBody EvidenceAttachmentBindRequest request
    ) {
        authorizationService.requirePermission("evidence.attachment.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.bindAttachment(attachmentId, request));
    }

    @PostMapping("/attachments/{attachment_id}/complete")
    public ApiResponse<EvidenceAttachmentRecord> completeAttachment(
            @PathVariable("attachment_id") String attachmentId,
            @RequestBody EvidenceAttachmentCompleteRequest request
    ) {
        authorizationService.requirePermission("evidence.attachment.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.completeAttachment(attachmentId, request));
    }

    @PostMapping("/reviews")
    public ApiResponse<EvidenceReviewRecord> createEvidenceReview(@RequestBody EvidenceReviewRequest request) {
        authorizationService.requirePermission("evidence.review.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createEvidenceReview(request));
    }

    @GetMapping("/timelines/{object_type}/{object_id}")
    public ApiResponse<List<EvidenceTimelineItem>> getTimeline(
            @PathVariable("object_type") String objectType,
            @PathVariable("object_id") String objectId
    ) {
        authorizationService.requirePermission("evidence.timeline.view");
        return ApiResponse.ok(service.getTimeline(objectType, objectId));
    }
}
