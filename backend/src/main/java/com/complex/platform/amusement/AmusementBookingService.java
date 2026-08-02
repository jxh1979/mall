package com.complex.platform.amusement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

final class AmusementBookingService {

    private static final Set<String> RESERVATION_STATUSES = Set.of("reserved", "cancelled", "completed", "no_show");
    private static final Set<String> QUEUE_STATUSES = Set.of("waiting", "called", "checked_in", "cancelled", "no_show");
    private static final Set<String> GROUP_BOOKING_TYPES = Set.of("group", "private");
    private static final Set<String> ACCEPTING_ENTRY_SESSION_STATUSES = Set.of("scheduled", "open");

    private final AmusementRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    AmusementBookingService(
            AmusementRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    PageResponse<AmusementQueueEntryRecord> listQueueEntries(int page, int size) {
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        return readPage(
                "amusement.queue.viewed",
                "amusement.queue.viewed_failed",
                "amusement.queue.view",
                "amusement_queue_entry",
                () -> repository.listQueueEntriesPaged(merchantId, page, size)
        );
    }

    AmusementQueueEntryRecord createQueueEntry(AmusementQueueEntryRequest request) {
        return executeWrite(
                "createAmusementQueueEntry",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementQueueEntryRecord.class,
                "amusement.queue.created",
                "amusement.queue.create_failed",
                "amusement.queue.manage",
                "amusement_queue_entry",
                () -> createQueueEntryInternal(request)
        );
    }

    AmusementQueueEntryRecord changeQueueEntryStatus(String queueEntryId, StatusChangeRequest request) {
        String id = requireText(queueEntryId, "queue_entry_id");
        return executeWrite(
                "changeAmusementQueueEntryStatus:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementQueueEntryRecord.class,
                "amusement.queue.status_changed",
                "amusement.queue.status_change_failed",
                "amusement.queue.manage",
                "amusement_queue_entry",
                () -> changeQueueEntryStatusInternal(id, request)
        );
    }

    PageResponse<AmusementReservationRecord> listReservations(int page, int size) {
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        return readPage(
                "amusement.reservation.viewed",
                "amusement.reservation.viewed_failed",
                "amusement.reservation.view",
                "amusement_reservation",
                () -> repository.listReservationsPaged(merchantId, page, size)
        );
    }

    AmusementReservationRecord createReservation(AmusementReservationRequest request) {
        return executeWrite(
                "createAmusementReservation",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementReservationRecord.class,
                "amusement.reservation.created",
                "amusement.reservation.create_failed",
                "amusement.reservation.manage",
                "amusement_reservation",
                () -> createReservationInternal(request)
        );
    }

    AmusementReservationRecord changeReservationStatus(String reservationId, StatusChangeRequest request) {
        String id = requireText(reservationId, "reservation_id");
        return executeWrite(
                "changeAmusementReservationStatus:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementReservationRecord.class,
                "amusement.reservation.status_changed",
                "amusement.reservation.status_change_failed",
                "amusement.reservation.manage",
                "amusement_reservation",
                () -> changeReservationStatusInternal(id, request)
        );
    }

    PageResponse<AmusementGroupBookingRecord> listGroupBookings(int page, int size) {
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        return readPage(
                "amusement.group_booking.viewed",
                "amusement.group_booking.view_failed",
                "amusement.group_booking.view",
                "amusement_group_booking",
                () -> repository.listGroupBookingsPaged(merchantId, page, size)
        );
    }

    AmusementGroupBookingRecord createGroupBooking(AmusementGroupBookingRequest request) {
        return executeWrite(
                "createAmusementGroupBooking",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementGroupBookingRecord.class,
                "amusement.group_booking.created",
                "amusement.group_booking.create_failed",
                "amusement.group_booking.manage",
                "amusement_group_booking",
                () -> createGroupBookingInternal(request)
        );
    }

    AmusementGroupBookingRecord approveGroupBooking(String bookingId, AmusementGroupBookingApprovalRequest request) {
        String id = requireText(bookingId, "group_booking_id");
        return executeWrite(
                "approveAmusementGroupBooking:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementGroupBookingRecord.class,
                "amusement.group_booking.approved",
                "amusement.group_booking.approve_failed",
                "amusement.group_booking.approve",
                "amusement_group_booking",
                () -> approveGroupBookingInternal(id, request)
        );
    }

    AmusementGroupBookingRecord releaseGroupBooking(String bookingId, AmusementGroupBookingReleaseRequest request) {
        String id = requireText(bookingId, "group_booking_id");
        return executeWrite(
                "releaseAmusementGroupBooking:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementGroupBookingRecord.class,
                "amusement.group_booking.released",
                "amusement.group_booking.release_failed",
                "amusement.group_booking.manage",
                "amusement_group_booking",
                () -> releaseGroupBookingInternal(id, request)
        );
    }

    private AmusementReservationRecord createReservationInternal(AmusementReservationRequest request) {
        AmusementReservationRequest actual = requireRequest(request);
        AmusementSessionRecord session = acceptingSession(
                requireText(actual.projectId(), "project_id"),
                requireText(actual.sessionId(), "session_id"),
                "SESSION_NOT_ACCEPTING_RESERVATION"
        );
        // 2b 越权修复：创建预约也须校验项目归属（与 changeReservationStatus 一致），防商户对他商户项目/场次写入预约。
        checkProjectOwnership(session.projectId());
        Instant now = Instant.now();
        return repository.saveReservation(new AmusementReservationRecord(
                "reservation_" + slug(actual.idempotencyKey()),
                session.projectId(),
                session.id(),
                requireText(actual.customerId(), "customer_id"),
                enumOrDefault(actual.reservationStatus(), "reserved", RESERVATION_STATUSES, "reservation_status"),
                now,
                now
        ));
    }

    private AmusementReservationRecord changeReservationStatusInternal(String reservationId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        String status = enumOrDefault(actual.action(), null, RESERVATION_STATUSES, "action");
        requireText(actual.reason(), "reason");
        AmusementReservationRecord existing = repository.findReservationById(reservationId)
                .orElseThrow(() -> ApiException.notFound("RESERVATION_NOT_FOUND", "预约不存在或已被删除"));
        checkProjectOwnership(existing.projectId());
        return repository.saveReservation(new AmusementReservationRecord(
                existing.id(),
                existing.projectId(),
                existing.sessionId(),
                existing.customerId(),
                status,
                existing.createdAt(),
                Instant.now()
        ));
    }

    private AmusementQueueEntryRecord createQueueEntryInternal(AmusementQueueEntryRequest request) {
        AmusementQueueEntryRequest actual = requireRequest(request);
        AmusementSessionRecord session = acceptingSession(
                requireText(actual.projectId(), "project_id"),
                requireText(actual.sessionId(), "session_id"),
                "SESSION_NOT_ACCEPTING_ENTRY"
        );
        // 2b 越权修复：创建排队也须校验项目归属，防跨商户写入排队。
        checkProjectOwnership(session.projectId());
        Instant now = Instant.now();
        return repository.saveQueueEntry(new AmusementQueueEntryRecord(
                "queue_" + slug(actual.idempotencyKey()),
                session.projectId(),
                session.id(),
                requireText(actual.customerId(), "customer_id"),
                trimToDefault(actual.queueNo(), "Q" + slug(actual.idempotencyKey()).substring(0, 6).toUpperCase(Locale.ROOT)),
                enumOrDefault(actual.status(), "waiting", QUEUE_STATUSES, "status"),
                now,
                now
        ));
    }

    private AmusementQueueEntryRecord changeQueueEntryStatusInternal(String queueEntryId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        String status = enumOrDefault(actual.action(), null, QUEUE_STATUSES, "action");
        requireText(actual.reason(), "reason");
        AmusementQueueEntryRecord existing = repository.findQueueEntryById(queueEntryId)
                .orElseThrow(() -> ApiException.notFound("QUEUE_ENTRY_NOT_FOUND", "排队记录不存在或已被删除"));
        checkProjectOwnership(existing.projectId());
        return repository.saveQueueEntry(new AmusementQueueEntryRecord(
                existing.id(),
                existing.projectId(),
                existing.sessionId(),
                existing.customerId(),
                existing.queueNo(),
                status,
                existing.createdAt(),
                Instant.now()
        ));
    }

    private AmusementGroupBookingRecord createGroupBookingInternal(AmusementGroupBookingRequest request) {
        AmusementGroupBookingRequest actual = requireRequest(request);
        AmusementSessionRecord session = acceptingSession(
                requireText(actual.projectId(), "project_id"),
                requireText(actual.sessionId(), "session_id"),
                "SESSION_NOT_ACCEPTING_GROUP_BOOKING"
        );
        // 2b 越权修复：创建团体预订也须校验项目归属，防跨商户写入团体预订并经 approve 锁占他商户容量。
        checkProjectOwnership(session.projectId());
        int partySize = positiveInteger(actual.partySize(), "party_size");
        int reservedCapacity = positiveInteger(actual.reservedCapacity(), "reserved_capacity");
        if (reservedCapacity > session.capacity()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "预留容量不能超过场次容量");
        }
        Instant now = Instant.now();
        return repository.saveGroupBooking(new AmusementGroupBookingRecord(
                "group_booking_" + slug(actual.idempotencyKey()),
                session.projectId(),
                session.id(),
                enumOrDefault(actual.bookingType(), "group", GROUP_BOOKING_TYPES, "booking_type"),
                requireText(actual.customerName(), "customer_name"),
                trimToNull(actual.contactPhone()),
                partySize,
                reservedCapacity,
                "pending_approval",
                null,
                trimToNull(actual.operatorNote()),
                evidence(actual.evidenceAttachmentIds()),
                null,
                null,
                now,
                now
        ));
    }

    private AmusementGroupBookingRecord approveGroupBookingInternal(String bookingId, AmusementGroupBookingApprovalRequest request) {
        AmusementGroupBookingApprovalRequest actual = requireRequest(request);
        String approvalNote = requireText(actual.approvalNote(), "approval_note");
        AmusementGroupBookingRecord existing = repository.findGroupBookingById(bookingId)
                .orElseThrow(() -> ApiException.notFound("GROUP_BOOKING_NOT_FOUND", "团体预约不存在或已被删除"));
        checkProjectOwnership(existing.projectId());
        if (!"pending_approval".equals(existing.bookingStatus())) {
            throw ApiException.conflict("GROUP_BOOKING_NOT_PENDING", "团体预约必须在待审批状态");
        }
        acceptingSession(existing.projectId(), existing.sessionId(), "SESSION_NOT_ACCEPTING_GROUP_BOOKING");
        Instant now = Instant.now();
        return repository.approveGroupBookingWithCapacityLock(
                        existing.id(),
                        approvalNote,
                        evidenceOrExisting(actual.evidenceAttachmentIds(), existing.evidenceAttachmentIds()),
                        now
                )
                .orElseThrow(() -> ApiException.conflict("SESSION_CAPACITY_LOCK_FAILED", "场次剩余容量不足"));
    }

    private AmusementGroupBookingRecord releaseGroupBookingInternal(String bookingId, AmusementGroupBookingReleaseRequest request) {
        AmusementGroupBookingReleaseRequest actual = requireRequest(request);
        String reason = requireText(actual.reason(), "reason");
        AmusementGroupBookingRecord existing = repository.findGroupBookingById(bookingId)
                .orElseThrow(() -> ApiException.notFound("GROUP_BOOKING_NOT_FOUND", "团体预约不存在或已被删除"));
        checkProjectOwnership(existing.projectId());
        if (!"approved".equals(existing.bookingStatus())) {
            throw ApiException.conflict("GROUP_BOOKING_NOT_APPROVED", "只有已批准的团体预约才能释放");
        }
        Instant now = Instant.now();
        return repository.releaseGroupBookingAndCapacity(
                        existing.id(),
                        trimToDefault(existing.operatorNote(), reason),
                        evidenceOrExisting(actual.evidenceAttachmentIds(), existing.evidenceAttachmentIds()),
                        now
                )
                .orElseThrow(() -> ApiException.conflict("GROUP_BOOKING_RELEASE_FAILED", "团体预约容量释放失败，请重试"));
    }

    private void checkProjectOwnership(String projectId) {
        authorizationService.currentMerchantScope().ifPresent(merchantId -> {
            AmusementProjectRecord project = repository.findProjectById(projectId)
                    .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
            if (!merchantId.equals(project.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该游乐项目不属于当前商户");
            }
        });
    }

    private AmusementSessionRecord acceptingSession(String projectId, String sessionId, String blockedCode) {
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "游乐项目不存在或已被删除"));
        AmusementSessionRecord session = repository.findSessionById(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "场次不存在或已被删除"));
        if (!project.id().equals(session.projectId())) {
            throw ApiException.conflict("SESSION_PROJECT_MISMATCH", "场次不属于该游乐项目");
        }
        if (!ACCEPTING_ENTRY_SESSION_STATUSES.contains(session.status())) {
            throw ApiException.conflict(blockedCode, "该场次当前不接受预约或排队");
        }
        return session;
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<PageResponse<T>> action
    ) {
        try {
            PageResponse<T> page = action.get();
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    Map.of("count", page.total())
            );
            return page;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T saved = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(saved),
                                null,
                                idempotencyKey,
                                Map.of()
                        );
                        return saved;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "unknown",
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private String businessId(Object response) {
        if (response instanceof AmusementReservationRecord record) {
            return record.id();
        }
        if (response instanceof AmusementQueueEntryRecord record) {
            return record.id();
        }
        if (response instanceof AmusementGroupBookingRecord record) {
            return record.id();
        }
        return "unknown";
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private int positiveInteger(Integer value, String fieldName) {
        int actual = value == null ? 0 : value;
        if (actual <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须大于 0");
        }
        return actual;
    }

    private String enumOrDefault(String value, String defaultValue, Set<String> allowed, String fieldName) {
        String actual = trimToDefault(value, defaultValue);
        if (actual == null || !allowed.contains(actual)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return actual;
    }

    private List<String> evidence(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private List<String> evidenceOrExisting(List<String> values, List<String> existing) {
        List<String> evidence = evidence(values);
        return evidence.isEmpty() ? List.copyOf(existing == null ? List.of() : existing) : evidence;
    }

    private String slug(String value) {
        String source = requireText(value, "idempotency_key");
        UUID uuid = UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(asBytes(uuid)).substring(0, 24);
    }

    private byte[] asBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        for (int index = 0; index < 8; index++) {
            bytes[index] = (byte) (most >>> (8 * (7 - index)));
            bytes[index + 8] = (byte) (least >>> (8 * (7 - index)));
        }
        return bytes;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
