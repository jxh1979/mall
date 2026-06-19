package com.complex.platform.bar;

import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BarEntryService {

    private final BarRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    public BarEntryService(
            BarRepository repository,
            AuthorizationService authorizationService,
            AuditEventService auditEventService
    ) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
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
        // T16：按 bar_deposit_rules（table_type/time_slot 优先于 bar）解析定金。无匹配规则保持向后兼容（0/not_required）。
        BarDepositRuleRecord rule = resolveDepositRule(table);
        long depositAmountCent = rule == null ? 0L : computeDepositAmount(rule, table);
        String depositStatus;
        String depositIntentId;
        if (rule == null || depositAmountCent <= 0L) {
            depositAmountCent = 0L;
            depositStatus = "not_required";
            depositIntentId = null;
        } else {
            depositStatus = "pending";
            depositIntentId = "bar_deposit_intent_" + slug(actual.idempotencyKey());
        }
        String reservationId = "reservation_" + slug(actual.idempotencyKey());
        BarReservationRecord saved = repository.saveReservation(new BarReservationRecord(
                reservationId,
                barId,
                merchantId,
                table.id(),
                trimToNull(customerId),
                requireInstant(actual.reservationTime(), "reservation_time"),
                nonNegative(actual.guestCount(), "guest_count"),
                table.allowedPassFreeGuestCount(),
                depositAmountCent,
                depositStatus,
                trimToDefault(orderSource, "staff"),
                trimToNull(actual.salespersonUserId()),
                "reserved",
                trimToDefault(terminal, "web"),
                null,
                safeList(actual.evidenceAttachmentIds()),
                now,
                now,
                trimToNull(actual.depositRule()),
                depositIntentId,
                null,
                null
        ));
        if ("pending".equals(depositStatus)) {
            auditEventService.recordSuccess("bar.reservation_deposit_required", "bar.reservation.create",
                    "bar_reservation", saved.id(), null, actual.idempotencyKey(),
                    Map.of("deposit_amount_cent", depositAmountCent, "deposit_mode", rule.mode(),
                            "deposit_intent_id", String.valueOf(depositIntentId)));
        }
        return saved;
    }

    /** T16：scope 优先级 table_type/time_slot > bar；listDepositRules 已按 scope_type 倒序返回。 */
    private BarDepositRuleRecord resolveDepositRule(BarTableRecord table) {
        List<BarDepositRuleRecord> rules = repository.listDepositRules(table.barId());
        BarDepositRuleRecord barScoped = null;
        for (BarDepositRuleRecord rule : rules) {
            if ("table_type".equals(rule.scopeType()) && rule.scopeId() != null
                    && rule.scopeId().equals(table.tableType())) {
                return rule;
            }
            if ("bar".equals(rule.scopeType()) && barScoped == null) {
                barScoped = rule;
            }
        }
        return barScoped;
    }

    private long computeDepositAmount(BarDepositRuleRecord rule, BarTableRecord table) {
        if ("fixed".equals(rule.mode())) {
            return rule.fixedAmountCent() == null ? 0L : Math.max(0L, rule.fixedAmountCent());
        }
        if ("min_consumption_ratio".equals(rule.mode())) {
            int ratioBps = rule.ratioBps() == null ? 0 : rule.ratioBps();
            return Math.max(0L, table.minConsumptionCent() * ratioBps / 10_000L);
        }
        return 0L;
    }

    BarReservationRecord payDeposit(String reservationId, BarReservationDepositActionRequest request) {
        BarReservationDepositActionRequest actual = requireRequest(request);
        BarReservationRecord reservation = requireReservation(reservationId);
        if (!"pending".equals(reservation.depositStatus())) {
            throw ApiException.conflict("BAR_DEPOSIT_NOT_PAYABLE",
                    "deposit must be in pending status to be paid");
        }
        Instant now = Instant.now();
        BarReservationRecord updated = withDeposit(reservation, "paid", reservation.depositSettlement(),
                reservation.depositIntentId(), now, now);
        BarReservationRecord saved = repository.saveReservation(updated);
        auditEventService.recordSuccess("bar.reservation_deposit_paid", "bar.reservation.manage",
                "bar_reservation", saved.id(), null, actual.idempotencyKey(),
                Map.of("deposit_amount_cent", saved.depositAmountCent(),
                        "channel", String.valueOf(actual.channel())));
        return saved;
    }

    BarReservationRecord checkInDeposit(String reservationId, BarReservationDepositActionRequest request) {
        BarReservationDepositActionRequest actual = requireRequest(request);
        BarReservationRecord reservation = requireReservation(reservationId);
        if ("cancelled".equals(reservation.status())) {
            throw ApiException.conflict("BAR_RESERVATION_NOT_CHECKINABLE", "reservation is cancelled");
        }
        if (!"paid".equals(reservation.depositStatus())) {
            throw ApiException.conflict("BAR_DEPOSIT_NOT_DEDUCTIBLE",
                    "deposit must be paid before it can be deducted at check-in");
        }
        Instant now = Instant.now();
        // 到店核销：定金转抵扣进账单（落账由 BarBillingService.createBill 按 reservation.depositAmountCent 抵扣），
        // 此处将订台标记为已结算/抵扣并留痕，状态闭环。
        BarReservationRecord updated = withDeposit(reservation, "settled", "deducted",
                reservation.depositIntentId(), reservation.depositPaidAt(), now);
        BarReservationRecord saved = repository.saveReservation(updated);
        auditEventService.recordSuccess("bar.reservation_deposit_deducted", "bar.reservation.manage",
                "bar_reservation", saved.id(), null, actual.idempotencyKey(),
                Map.of("deposit_amount_cent", saved.depositAmountCent(), "deposit_settlement", "deducted"));
        return saved;
    }

    BarReservationRecord cancelReservation(String reservationId, BarReservationCancelRequest request) {
        BarReservationCancelRequest actual = requireRequest(request);
        BarReservationRecord reservation = requireReservation(reservationId);
        if ("cancelled".equals(reservation.status())) {
            throw ApiException.conflict("BAR_RESERVATION_ALREADY_CANCELLED", "reservation is already cancelled");
        }
        if ("settled".equals(reservation.depositStatus())) {
            throw ApiException.conflict("BAR_DEPOSIT_ALREADY_SETTLED",
                    "deposit already settled; reservation cannot be cancelled");
        }
        Instant now = Instant.now();
        boolean noShow = actual.noShow() != null && actual.noShow();
        String settlement;
        String event;
        if (reservation.depositAmountCent() <= 0L || !"paid".equals(reservation.depositStatus())) {
            // 无定金或定金未支付：直接取消，无退/没收。
            settlement = null;
            event = "bar.reservation_deposit_refunded";
        } else {
            BarDepositRuleRecord rule = resolveDepositRuleByBar(reservation.barId(), null);
            int freeBeforeMinutes = rule == null ? 0 : rule.cancelFreeBeforeMinutes();
            long minutesToReservation = ChronoUnit.MINUTES.between(now, reservation.reservedAt());
            boolean withinFreeWindow = !noShow && minutesToReservation >= freeBeforeMinutes;
            if (withinFreeWindow) {
                settlement = "refunded";
                event = "bar.reservation_deposit_refunded";
            } else {
                settlement = "forfeited";
                event = "bar.reservation_deposit_forfeited";
            }
        }
        String depositStatus = settlement == null ? reservation.depositStatus() : "settled";
        BarReservationRecord cancelled = new BarReservationRecord(
                reservation.id(),
                reservation.barId(),
                reservation.merchantId(),
                reservation.tableId(),
                reservation.customerId(),
                reservation.reservedAt(),
                reservation.guestCount(),
                reservation.allowedPassFreeGuestCount(),
                reservation.depositAmountCent(),
                depositStatus,
                reservation.orderSource(),
                reservation.salesStaffId(),
                "cancelled",
                reservation.createdTerminal(),
                reservation.deviceId(),
                reservation.evidenceAttachmentIds(),
                reservation.createdAt(),
                now,
                reservation.depositRule(),
                reservation.depositIntentId(),
                reservation.depositPaidAt(),
                settlement
        );
        BarReservationRecord saved = repository.saveReservation(cancelled);
        if (settlement != null) {
            auditEventService.recordSuccess(event, "bar.reservation.manage",
                    "bar_reservation", saved.id(), null, actual.idempotencyKey(),
                    Map.of("deposit_amount_cent", saved.depositAmountCent(),
                            "deposit_settlement", settlement, "no_show", noShow));
        }
        return saved;
    }

    private BarDepositRuleRecord resolveDepositRuleByBar(String barId, String tableType) {
        List<BarDepositRuleRecord> rules = repository.listDepositRules(barId);
        BarDepositRuleRecord barScoped = null;
        for (BarDepositRuleRecord rule : rules) {
            if (tableType != null && "table_type".equals(rule.scopeType())
                    && tableType.equals(rule.scopeId())) {
                return rule;
            }
            if ("bar".equals(rule.scopeType()) && barScoped == null) {
                barScoped = rule;
            }
        }
        return barScoped;
    }

    BarDepositRuleRecord createDepositRule(BarDepositRuleRequest request) {
        BarDepositRuleRequest actual = requireRequest(request);
        String barId = requireText(actual.barId(), "bar_id");
        String mode = requireText(actual.mode(), "mode");
        if (!"fixed".equals(mode) && !"min_consumption_ratio".equals(mode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "mode must be fixed or min_consumption_ratio");
        }
        Long fixedAmountCent = actual.fixedAmountCent();
        Integer ratioBps = actual.ratioBps();
        if ("fixed".equals(mode)) {
            if (fixedAmountCent == null || fixedAmountCent < 0L) {
                throw ApiException.badRequest("VALIDATION_FAILED", "fixed_amount_cent is required for fixed mode");
            }
            ratioBps = null;
        } else {
            if (ratioBps == null || ratioBps < 0 || ratioBps > 100_000) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "ratio_bps in [0,100000] is required for min_consumption_ratio mode");
            }
            fixedAmountCent = null;
        }
        String scopeType = trimToDefault(actual.scopeType(), "bar");
        String scopeId = trimToDefault(actual.scopeId(), barId);
        int cancelFreeBeforeMinutes = actual.cancelFreeBeforeMinutes() == null
                ? 0 : Math.max(0, actual.cancelFreeBeforeMinutes());
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        return repository.saveDepositRule(new BarDepositRuleRecord(
                "bar_deposit_rule_" + slug(actual.idempotencyKey()),
                barId,
                merchantId,
                scopeType,
                scopeId,
                mode,
                fixedAmountCent,
                ratioBps,
                cancelFreeBeforeMinutes,
                true,
                now,
                now
        ));
    }

    private BarReservationRecord requireReservation(String reservationId) {
        return repository.findReservationById(requireText(reservationId, "reservation_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "bar reservation not found"));
    }

    private BarReservationRecord withDeposit(
            BarReservationRecord reservation,
            String depositStatus,
            String depositSettlement,
            String depositIntentId,
            Instant depositPaidAt,
            Instant updatedAt
    ) {
        return new BarReservationRecord(
                reservation.id(),
                reservation.barId(),
                reservation.merchantId(),
                reservation.tableId(),
                reservation.customerId(),
                reservation.reservedAt(),
                reservation.guestCount(),
                reservation.allowedPassFreeGuestCount(),
                reservation.depositAmountCent(),
                depositStatus,
                reservation.orderSource(),
                reservation.salesStaffId(),
                reservation.status(),
                reservation.createdTerminal(),
                reservation.deviceId(),
                reservation.evidenceAttachmentIds(),
                reservation.createdAt(),
                updatedAt,
                reservation.depositRule(),
                depositIntentId,
                depositPaidAt,
                depositSettlement
        );
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
