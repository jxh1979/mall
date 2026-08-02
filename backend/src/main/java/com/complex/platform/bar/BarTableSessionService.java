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
        // 可进人数（容量）独立于免票入场人数：未填时沿用免票人数口径（向后兼容），填了就按填的落库。
        int capacity = actual.capacity() == null
                ? allowedFreeGuests
                : nonNegative(actual.capacity(), "capacity");
        long minimumSpend = nonNegative(actual.minimumSpendCent(), "minimum_spend_cent");
        return repository.saveTable(new BarTableRecord(
                "table_" + slug,
                requireText(actual.barId(), "bar_id"),
                merchantId,
                "T-" + slug.toUpperCase(Locale.ROOT),
                requireText(actual.tableName(), "table_name"),
                trimToDefault(actual.tableType(), "card_seat"),
                capacity,
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
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
        checkMerchantOwnership(table.merchantId());
        if (!"active".equals(table.status())) {
            throw ApiException.conflict("BAR_TABLE_NOT_OPENABLE", "该台位未启用");
        }
        String reservationId = requireText(actual.reservationId(), "reservation_id");
        BarReservationRecord reservation = repository.findReservationById(reservationId)
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
        if (!table.id().equals(reservation.tableId())) {
            throw ApiException.conflict("BAR_TABLE_RESERVATION_MISMATCH", "该订台不是本台位的预订");
        }
        return repository.saveTable(tableWithStatus(table, "opened", Instant.now()));
    }

    // 平台直营会话的租户占位：bar_table_sessions.merchant_id 为 NOT NULL（V019），
    // 而平台管理员/平台直营桌台的 merchant_id 为空，用与 analytics 同口径的 __platform__ 占位，
    // 既满足非空约束又不落进任何商户的行级隔离查询（WHERE merchant_id = :scope 永不命中占位）。
    private static final String PLATFORM_MERCHANT_PLACEHOLDER = "__platform__";

    BarTableSessionRecord createTableSession(BarTableSessionRequest request) {
        BarTableSessionRequest actual = requireRequest(request);
        BarTableRecord table = repository.findTableById(requireText(actual.tableId(), "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
        // 会话商户归属：优先调用方商户范围，其次桌台商户，平台直营（皆空）落 __platform__ 占位满足非空约束。
        String merchantId = authorizationService.currentMerchantScope()
                .or(() -> Optional.ofNullable(trimToNull(table.merchantId())))
                .orElse(PLATFORM_MERCHANT_PLACEHOLDER);
        String barId = requireText(actual.barId(), "bar_id");
        if (!barId.equals(table.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "开台必须在台位所在的酒吧进行");
        }
        if (!"active".equals(table.status()) && !"opened".equals(table.status())) {
            throw ApiException.conflict("BAR_TABLE_NOT_OPENABLE", "该台位当前不可开台");
        }
        BarReservationRecord reservation = null;
        String reservationId = trimToNull(actual.reservationId());
        if (reservationId != null) {
            reservation = repository.findReservationById(reservationId)
                    .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
            if (!barId.equals(reservation.barId()) || !table.id().equals(reservation.tableId())) {
                throw ApiException.conflict("BAR_SCOPE_MISMATCH", "该订台不属于此台位会话");
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
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
        checkMerchantOwnership(table.merchantId());
        // 空间必须存在；且若属于某商户，必须与桌台同商户（不可指派到他商户空间，currentMerchantScope 隔离）。
        String spaceMerchant = repository.findSpaceMerchant(spaceId)
                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "铺位不存在或已被删除"));
        if (!spaceMerchant.isBlank() && table.merchantId() != null && !spaceMerchant.equals(table.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该铺位属于其他商户");
        }
        // 再以调用方商户范围兜底校验（防止桌台 merchant_id 为空时被越权指派他商户空间）。
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !spaceMerchant.isBlank() && !scope.get().equals(spaceMerchant)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该铺位属于其他商户");
        }
        if (repository.assignTableSpace(id, spaceId) == 0) {
            throw ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除");
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
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "转出台位不存在或已被删除"));
        BarTableRecord targetTable = repository.findTableById(requireText(actual.targetTableId(), "target_table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "转入台位不存在或已被删除"));
        if (!session.barId().equals(targetTable.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "转台不能跨酒吧");
        }
        if (!"active".equals(targetTable.status())) {
            throw ApiException.conflict("BAR_TABLE_NOT_TRANSFERABLE", "转入台位未启用");
        }
        Instant now = Instant.now();
        repository.saveTable(tableWithStatus(sourceTable, "pending_clear", now));
        repository.saveTable(tableWithStatus(targetTable, "opened", now));
        // 2c-5：DB 级乐观锁 CAS（替代仅内存比对的 requireSessionVersion），防并发转台双重占用。
        BarTableSessionRecord updated = sessionMovedToTable(session, targetTable, now);
        if (!repository.updateTableSessionIfVersionMatches(updated, session.sessionVersion())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话已被并发变更，请刷新后重试");
        }
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
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "并台仅限同酒吧、同商户、同营业日的台位会话");
        }
        String requestedBy = authorizationService.currentActor().id();
        BarTableRecord primaryTable = repository.findTableById(primary.tableId())
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "主台位不存在或已被删除"));
        BarTableRecord mergedTable = repository.findTableById(merged.tableId())
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "被并台位不存在或已被删除"));
        Instant now = Instant.now();
        // 2c-5：两个 session 均走 DB 级乐观锁 CAS，防并发合台重复占用。
        BarTableSessionRecord updatedPrimary = sessionWithNextVersion(primary, now);
        if (!repository.updateTableSessionIfVersionMatches(updatedPrimary, primary.sessionVersion())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话已被并发变更，请刷新后重试");
        }
        if (!repository.updateTableSessionIfVersionMatches(sessionMergedInto(merged, primary.id(), now), merged.sessionVersion())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话已被并发变更，请刷新后重试");
        }
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
            throw ApiException.badRequest("VALIDATION_FAILED", "续台截止时间必须晚于当前时间");
        }
        long additionalMinConsumption = nonNegative(actual.additionalMinConsumptionCent(), "additional_min_consumption_cent");
        long additionalDeposit = nonNegative(actual.additionalDepositAmountCent(), "additional_deposit_amount_cent");
        long additionalServiceFee = nonNegative(actual.additionalServiceFeeCent(), "additional_service_fee_cent");
        // 2c-5：DB 级乐观锁 CAS，防并发续台重复追加低消/押金。
        BarTableSessionRecord updated = sessionWithNextVersion(session, now);
        if (!repository.updateTableSessionIfVersionMatches(updated, session.sessionVersion())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话已被并发变更，请刷新后重试");
        }
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
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_SESSION_NOT_FOUND", "台位就餐会话不存在或已结束"));
        if (!"open".equals(session.status())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_NOT_ACTIVE", "台位会话不在开台状态");
        }
        return session;
    }

    private void requireSessionVersion(BarTableSessionRecord session, Long expectedVersion) {
        if (expectedVersion == null || expectedVersion != session.sessionVersion()) {
            throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话版本已变化，请刷新后重试");
        }
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的数据");
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
