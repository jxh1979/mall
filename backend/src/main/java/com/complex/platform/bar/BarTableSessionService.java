package com.complex.platform.bar;

import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BarTableSessionService {

    private static final String CURRENCY_CNY = "CNY";

    private final BarRepository repository;
    private final AuthorizationService authorizationService;

    public BarTableSessionService(BarRepository repository, AuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    BarTableRecord createTable(BarTableRequest request) {
        BarTableRequest actual = requireRequest(request);
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        String slug = slug(actual.idempotencyKey());
        int allowedFreeGuests = nonNegative(actual.allowedFreeEntryCount(), "allowed_free_entry_count");
        long minimumSpend = nonNegative(actual.minimumSpendCent(), "minimum_spend_cent");
        return repository.saveTable(new BarTableRecord(
                "table_" + slug,
                requireText(actual.barId(), "bar_id"),
                merchantId,
                "T-" + slug.toUpperCase(Locale.ROOT),
                requireText(actual.tableName(), "table_name"),
                trimToDefault(actual.tableType(), "card_seat"),
                Math.max(allowedFreeGuests, 0),
                allowedFreeGuests,
                minimumSpend,
                CURRENCY_CNY,
                trimToDefault(actual.status(), "active"),
                now,
                now
        ));
    }

    BarTableRecord openTable(String tableId, OpenBarTableRequest request) {
        OpenBarTableRequest actual = requireRequest(request);
        BarTableRecord table = repository.findTableById(tableId)
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "bar table not found"));
        checkMerchantOwnership(table.merchantId());
        if (!"active".equals(table.status())) {
            throw ApiException.conflict("BAR_TABLE_NOT_OPENABLE", "bar table is not active");
        }
        String reservationId = requireText(actual.reservationId(), "reservation_id");
        BarReservationRecord reservation = repository.findReservationById(reservationId)
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "bar reservation not found"));
        if (!table.id().equals(reservation.tableId())) {
            throw ApiException.conflict("BAR_TABLE_RESERVATION_MISMATCH", "reservation is not for this table");
        }
        return repository.saveTable(tableWithStatus(table, "opened", Instant.now()));
    }

    BarTableSessionRecord createTableSession(BarTableSessionRequest request) {
        BarTableSessionRequest actual = requireRequest(request);
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        BarTableRecord table = repository.findTableById(requireText(actual.tableId(), "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "bar table not found"));
        String barId = requireText(actual.barId(), "bar_id");
        if (!barId.equals(table.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "table session must be opened in the table bar");
        }
        if (!"active".equals(table.status()) && !"opened".equals(table.status())) {
            throw ApiException.conflict("BAR_TABLE_NOT_OPENABLE", "bar table is not openable");
        }
        BarReservationRecord reservation = null;
        String reservationId = trimToNull(actual.reservationId());
        if (reservationId != null) {
            reservation = repository.findReservationById(reservationId)
                    .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "bar reservation not found"));
            if (!barId.equals(reservation.barId()) || !table.id().equals(reservation.tableId())) {
                throw ApiException.conflict("BAR_SCOPE_MISMATCH", "reservation is not for this table session");
            }
        }
        Instant now = Instant.now();
        repository.saveTable(tableWithStatus(table, "opened", now));
        return repository.saveTableSession(new BarTableSessionRecord(
                "session_" + slug(actual.idempotencyKey()),
                barId,
                merchantId,
                requireLocalDate(actual.businessDate(), "business_date"),
                table.id(),
                reservationId,
                trimToDefault(actual.customerId(), reservation == null ? null : reservation.customerId()),
                nonNegative(actual.guestCount(), "guest_count"),
                table.minConsumptionCent(),
                table.allowedPassFreeGuestCount(),
                trimToNull(actual.salesStaffId()),
                trimToNull(actual.serviceStaffId()),
                "open",
                now,
                null,
                null,
                1L,
                now,
                now
        ));
    }

    BarTableSpaceAssignmentRecord assignTableSpace(String tableId, BarTableSpaceAssignmentRequest request) {
        BarTableSpaceAssignmentRequest actual = requireRequest(request);
        String id = requireText(tableId, "table_id");
        String spaceId = requireText(actual.spaceId(), "space_id");
        BarTableRecord table = repository.findTableById(id)
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "bar table not found"));
        checkMerchantOwnership(table.merchantId());
        // 空间必须存在；且若属于某商户，必须与桌台同商户（不可指派到他商户空间，currentMerchantScope 隔离）。
        String spaceMerchant = repository.findSpaceMerchant(spaceId)
                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "space not found"));
        if (!spaceMerchant.isBlank() && table.merchantId() != null && !spaceMerchant.equals(table.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "space belongs to another merchant");
        }
        // 再以调用方商户范围兜底校验（防止桌台 merchant_id 为空时被越权指派他商户空间）。
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !spaceMerchant.isBlank() && !scope.get().equals(spaceMerchant)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "space belongs to another merchant");
        }
        if (repository.assignTableSpace(id, spaceId) == 0) {
            throw ApiException.notFound("BAR_TABLE_NOT_FOUND", "bar table not found");
        }
        return new BarTableSpaceAssignmentRecord(id, spaceId, Instant.now());
    }

    java.util.List<BarTableSpaceBacklogRecord> listTablesWithoutSpace() {
        return repository.listTablesWithoutSpace(authorizationService.currentMerchantScope());
    }

    BarTableSessionAdjustmentRecord transferTableSession(String sessionId, BarTableTransferRequest request) {
        BarTableTransferRequest actual = requireRequest(request);
        BarTableSessionRecord session = requireOpenTableSession(sessionId);
        checkMerchantOwnership(session.merchantId());
        requireSessionVersion(session, actual.sessionVersion());
        String requestedBy = authorizationService.currentActor().id();
        BarTableRecord sourceTable = repository.findTableById(session.tableId())
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "source bar table not found"));
        BarTableRecord targetTable = repository.findTableById(requireText(actual.targetTableId(), "target_table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "target bar table not found"));
        if (!session.barId().equals(targetTable.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "table transfer cannot cross bar");
        }
        if (!"active".equals(targetTable.status())) {
            throw ApiException.conflict("BAR_TABLE_NOT_TRANSFERABLE", "target bar table is not active");
        }
        Instant now = Instant.now();
        repository.saveTable(tableWithStatus(sourceTable, "pending_clear", now));
        repository.saveTable(tableWithStatus(targetTable, "opened", now));
        BarTableSessionRecord updated = repository.saveTableSession(sessionMovedToTable(session, targetTable, now));
        return repository.saveTableSessionAdjustment(newAdjustment(
                "transfer",
                updated,
                null,
                null,
                sourceTable.id(),
                targetTable.id(),
                session.minConsumptionSnapshotCent(),
                targetTable.minConsumptionCent(),
                null,
                0L,
                0L,
                0L,
                requireText(actual.reason(), "reason"),
                requestedBy,
                targetTable.minConsumptionCent() > session.minConsumptionSnapshotCent(),
                actual.idempotencyKey(),
                safeList(actual.evidenceAttachmentIds()),
                sessionSnapshot(session, updated)
        ));
    }

    BarTableSessionAdjustmentRecord mergeTableSessions(String primarySessionId, BarTableMergeRequest request) {
        BarTableMergeRequest actual = requireRequest(request);
        BarTableSessionRecord primary = requireOpenTableSession(primarySessionId);
        checkMerchantOwnership(primary.merchantId());
        BarTableSessionRecord merged = requireOpenTableSession(requireText(actual.mergedSessionId(), "merged_session_id"));
        checkMerchantOwnership(merged.merchantId());
        requireSessionVersion(primary, actual.primarySessionVersion());
        requireSessionVersion(merged, actual.mergedSessionVersion());
        if (!primary.barId().equals(merged.barId())
                || !Objects.equals(primary.merchantId(), merged.merchantId())
                || !primary.businessDate().equals(merged.businessDate())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "table sessions can only merge within same bar, merchant and business date");
        }
        String requestedBy = authorizationService.currentActor().id();
        BarTableRecord primaryTable = repository.findTableById(primary.tableId())
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "primary bar table not found"));
        BarTableRecord mergedTable = repository.findTableById(merged.tableId())
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "merged bar table not found"));
        Instant now = Instant.now();
        BarTableSessionRecord updatedPrimary = repository.saveTableSession(sessionWithNextVersion(primary, now));
        repository.saveTableSession(sessionMergedInto(merged, primary.id(), now));
        repository.saveTable(tableWithStatus(mergedTable, "pending_clear", now));
        return repository.saveTableSessionAdjustment(newAdjustment(
                "merge",
                updatedPrimary,
                merged.id(),
                primary.id(),
                mergedTable.id(),
                primaryTable.id(),
                primary.minConsumptionSnapshotCent(),
                primary.minConsumptionSnapshotCent(),
                null,
                0L,
                0L,
                0L,
                requireText(actual.reason(), "reason"),
                requestedBy,
                false,
                actual.idempotencyKey(),
                safeList(actual.evidenceAttachmentIds()),
                sessionSnapshot(primary, updatedPrimary)
        ));
    }

    BarTableSessionAdjustmentRecord renewTableSession(String sessionId, BarTableRenewRequest request) {
        BarTableRenewRequest actual = requireRequest(request);
        BarTableSessionRecord session = requireOpenTableSession(sessionId);
        checkMerchantOwnership(session.merchantId());
        requireSessionVersion(session, actual.sessionVersion());
        String requestedBy = authorizationService.currentActor().id();
        Instant renewedUntil = requireInstant(actual.renewedUntil(), "renewed_until");
        Instant now = Instant.now();
        if (!renewedUntil.isAfter(now)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "renewed_until must be in the future");
        }
        long additionalMinConsumption = nonNegative(actual.additionalMinConsumptionCent(), "additional_min_consumption_cent");
        long additionalDeposit = nonNegative(actual.additionalDepositAmountCent(), "additional_deposit_amount_cent");
        long additionalServiceFee = nonNegative(actual.additionalServiceFeeCent(), "additional_service_fee_cent");
        BarTableSessionRecord updated = repository.saveTableSession(sessionWithNextVersion(session, now));
        return repository.saveTableSessionAdjustment(newAdjustment(
                "renew",
                updated,
                null,
                null,
                session.tableId(),
                session.tableId(),
                session.minConsumptionSnapshotCent(),
                session.minConsumptionSnapshotCent() + additionalMinConsumption,
                renewedUntil,
                additionalMinConsumption,
                additionalDeposit,
                additionalServiceFee,
                requireText(actual.reason(), "reason"),
                requestedBy,
                additionalMinConsumption > 0 || additionalDeposit > 0 || additionalServiceFee > 0,
                actual.idempotencyKey(),
                safeList(actual.evidenceAttachmentIds()),
                sessionSnapshot(session, updated)
        ));
    }

    private BarTableSessionRecord requireOpenTableSession(String sessionId) {
        BarTableSessionRecord session = repository.findTableSessionById(sessionId)
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_SESSION_NOT_FOUND", "bar table session not found"));
        if (!"open".equals(session.status())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_NOT_ACTIVE", "bar table session is not open");
        }
        return session;
    }

    private void requireSessionVersion(BarTableSessionRecord session, Long expectedVersion) {
        if (expectedVersion == null || expectedVersion != session.sessionVersion()) {
            throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "bar table session version changed");
        }
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "access to another merchant's resource is not allowed");
        }
    }

    private BarTableRecord tableWithStatus(BarTableRecord table, String status, Instant updatedAt) {
        return new BarTableRecord(
                table.id(),
                table.barId(),
                table.merchantId(),
                table.tableCode(),
                table.tableName(),
                table.tableType(),
                table.capacity(),
                table.allowedPassFreeGuestCount(),
                table.minConsumptionCent(),
                table.currency(),
                status,
                table.createdAt(),
                updatedAt
        );
    }

    private BarTableSessionRecord sessionMovedToTable(BarTableSessionRecord session, BarTableRecord table, Instant now) {
        return new BarTableSessionRecord(
                session.id(),
                session.barId(),
                session.merchantId(),
                session.businessDate(),
                table.id(),
                session.reservationId(),
                session.customerId(),
                session.guestCount(),
                table.minConsumptionCent(),
                table.allowedPassFreeGuestCount(),
                session.salesStaffId(),
                session.serviceStaffId(),
                session.status(),
                session.openedAt(),
                session.closedAt(),
                session.sourceSessionId(),
                session.sessionVersion() + 1,
                session.createdAt(),
                now
        );
    }

    private BarTableSessionRecord sessionWithNextVersion(BarTableSessionRecord session, Instant now) {
        return new BarTableSessionRecord(
                session.id(),
                session.barId(),
                session.merchantId(),
                session.businessDate(),
                session.tableId(),
                session.reservationId(),
                session.customerId(),
                session.guestCount(),
                session.minConsumptionSnapshotCent(),
                session.allowedPassFreeGuestCountSnapshot(),
                session.salesStaffId(),
                session.serviceStaffId(),
                session.status(),
                session.openedAt(),
                session.closedAt(),
                session.sourceSessionId(),
                session.sessionVersion() + 1,
                session.createdAt(),
                now
        );
    }

    private BarTableSessionRecord sessionMergedInto(BarTableSessionRecord session, String primarySessionId, Instant now) {
        return new BarTableSessionRecord(
                session.id(),
                session.barId(),
                session.merchantId(),
                session.businessDate(),
                session.tableId(),
                session.reservationId(),
                session.customerId(),
                session.guestCount(),
                session.minConsumptionSnapshotCent(),
                session.allowedPassFreeGuestCountSnapshot(),
                session.salesStaffId(),
                session.serviceStaffId(),
                "merged",
                session.openedAt(),
                now,
                primarySessionId,
                session.sessionVersion() + 1,
                session.createdAt(),
                now
        );
    }

    private BarTableSessionAdjustmentRecord newAdjustment(
            String type,
            BarTableSessionRecord session,
            String sourceSessionId,
            String targetSessionId,
            String fromTableId,
            String toTableId,
            long minConsumptionBeforeCent,
            long minConsumptionAfterCent,
            Instant renewedUntil,
            long additionalMinConsumptionCent,
            long additionalDepositAmountCent,
            long additionalServiceFeeCent,
            String reason,
            String requestedByUserId,
            boolean approvalRequired,
            String idempotencyKey,
            List<String> evidenceAttachmentIds,
            String snapshotJson
    ) {
        String key = requireText(idempotencyKey, "idempotency_key");
        return new BarTableSessionAdjustmentRecord(
                "adjustment_" + slug(type + ":" + key),
                session.id(),
                type,
                sourceSessionId,
                targetSessionId,
                fromTableId,
                toTableId,
                session.barId(),
                session.merchantId(),
                session.businessDate(),
                minConsumptionBeforeCent,
                minConsumptionAfterCent,
                renewedUntil,
                additionalMinConsumptionCent,
                additionalDepositAmountCent,
                additionalServiceFeeCent,
                reason,
                requestedByUserId,
                approvalRequired,
                "applied",
                key,
                evidenceAttachmentIds,
                snapshotJson,
                Instant.now()
        );
    }

    private String sessionSnapshot(BarTableSessionRecord before, BarTableSessionRecord after) {
        return """
                {"session_id":"%s","from_table_id":"%s","to_table_id":"%s","version_before":%d,"version_after":%d}
                """.formatted(
                jsonText(before.id()),
                jsonText(before.tableId()),
                jsonText(after.tableId()),
                before.sessionVersion(),
                after.sessionVersion()
        ).trim();
    }

}
