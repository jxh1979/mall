package com.complex.platform.bar;

import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BarEntryService {

    private final BarRepository repository;
    private final AuthorizationService authorizationService;

    public BarEntryService(BarRepository repository, AuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    BarAgeVerificationRecord createAgeVerification(VerificationRequest request) {
        VerificationRequest actual = requireRequest(request);
        Map<String, Object> context = actual.businessContext() == null ? Map.of() : actual.businessContext();
        String result = trimToDefault(textFromMap(context, "result", "verification_result"), "passed");
        String reviewStatus = trimToDefault(textFromMap(context, "review_status", "reviewStatus"), "accepted");
        Instant now = Instant.now();
        return repository.saveAgeVerification(new BarAgeVerificationRecord(
                "age_" + slug(actual.idempotencyKey()),
                trimToDefault(textFromMap(context, "business_id", "bar_id", "barId"), actual.pointId()),
                requireText(actual.credentialPayload(), "credential_payload"),
                trimToNull(actual.deviceId()),
                trimToNull(textFromMap(context, "staff_id", "staffId")),
                result,
                "placeholder",
                reviewStatus,
                safeList(listFromMap(context, "evidence_attachment_ids", "evidenceAttachmentIds")),
                now
        ));
    }

    BarReservationRecord createReservation(BarReservationRequest request, String customerId, String orderSource, String terminal) {
        BarReservationRequest actual = requireRequest(request);
        BarTableRecord table = repository.findTableById(requireText(actual.tableId(), "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "bar table not found"));
        String barId = requireText(actual.barId(), "bar_id");
        if (!barId.equals(table.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "reservation table is not in this bar");
        }
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        return repository.saveReservation(new BarReservationRecord(
                "reservation_" + slug(actual.idempotencyKey()),
                barId,
                merchantId,
                table.id(),
                trimToNull(customerId),
                requireInstant(actual.reservationTime(), "reservation_time"),
                nonNegative(actual.guestCount(), "guest_count"),
                table.allowedPassFreeGuestCount(),
                0L,
                "not_required",
                trimToDefault(orderSource, "staff"),
                trimToNull(actual.salespersonUserId()),
                "reserved",
                trimToDefault(terminal, "web"),
                null,
                safeList(actual.evidenceAttachmentIds()),
                now,
                now,
                trimToNull(actual.depositRule())
        ));
    }

    BarTableGuestCheckinRecord checkInGuest(String reservationId, VerificationRequest request) {
        VerificationRequest actual = requireRequest(request);
        BarReservationRecord reservation = repository.findReservationById(reservationId)
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "bar reservation not found"));
        if ("cancelled".equals(reservation.status())) {
            throw ApiException.conflict("BAR_RESERVATION_NOT_CHECKINABLE", "reservation is cancelled");
        }
        Map<String, Object> context = actual.businessContext() == null ? Map.of() : actual.businessContext();
        String verificationBarId = trimToDefault(textFromMap(context, "bar_id", "barId"), actual.pointId());
        if (!reservation.barId().equals(verificationBarId)) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "card-seat free entry is limited to the reserved bar");
        }
        int checkedIn = repository.countGuestCheckins(reservation.id());
        if (checkedIn >= reservation.allowedPassFreeGuestCount()) {
            throw ApiException.conflict("FREE_GUEST_QUOTA_EXHAUSTED", "card-seat free guest quota is exhausted");
        }
        Instant now = Instant.now();
        return repository.saveGuestCheckin(new BarTableGuestCheckinRecord(
                "checkin_" + slug(actual.idempotencyKey()),
                reservation.id(),
                reservation.barId(),
                reservation.tableId(),
                requireText(actual.credentialPayload(), "credential_payload"),
                "accepted",
                checkedIn + 1,
                trimToNull(actual.deviceId()),
                trimToNull(textFromMap(context, "staff_id", "staffId")),
                safeList(listFromMap(context, "evidence_attachment_ids", "evidenceAttachmentIds")),
                now
        ));
    }

    private List<String> listFromMap(Map<String, Object> map, String... names) {
        if (map == null) {
            return List.of();
        }
        for (String name : names) {
            Object value = map.get(name);
            if (value instanceof List<?> list) {
                return list.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
            }
        }
        return List.of();
    }

    private String textFromMap(Map<String, Object> map, String... names) {
        if (map == null) {
            return null;
        }
        for (String name : names) {
            Object value = map.get(name);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, java.util.UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = java.util.UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        return resolved.length() > 40 ? resolved.substring(0, 40) : resolved;
    }
}
