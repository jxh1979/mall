package com.complex.platform.bar;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BarService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final String CURRENCY_CNY = "CNY";

    private final BarRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final BarReceivableLedgerService receivableLedgerService;

    public BarService(
            BarRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            BarReceivableLedgerService receivableLedgerService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.receivableLedgerService = receivableLedgerService;
    }

    @Transactional
    public BarAgeVerificationRecord createAgeVerification(VerificationRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            return idempotencyService.execute(
                    "createBarAgeVerification",
                    key,
                    request,
                    () -> {
                        BarAgeVerificationRecord record = createAgeVerificationRecord(request);
                        if ("passed".equals(record.result()) || "accepted".equals(record.result())) {
                            auditEventService.recordSuccess(
                                    "bar.age_verification.passed",
                                    "bar.entry.age_verify",
                                    "bar_age_verification",
                                    record.id(),
                                    null,
                                    key,
                                    Map.of("result", record.result())
                            );
                        } else {
                            auditEventService.recordFailure(
                                    "bar.age_verification.rejected",
                                    "bar.entry.age_verify",
                                    "bar_age_verification",
                                    record.id(),
                                    null,
                                    key,
                                    "AGE_VERIFICATION_REJECTED"
                            );
                        }
                        return record;
                    },
                    BarAgeVerificationRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "bar.age_verification.rejected",
                    "bar.entry.age_verify",
                    "bar_age_verification",
                    null,
                    null,
                    key,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public PageResponse<BarOrderRecord> listOrders() {
        return readPage("bar.order.viewed", "bar.order.view_failed", "bar.order.view", "bar_order", repository::listOrders);
    }

    @Transactional
    public BarOrderRecord createOrder(BarOrderRequest request) {
        return executeWrite(
                "createBarOrder",
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderRecord.class,
                "bar.order_created",
                "bar.order_create_failed",
                "bar.order.create",
                "bar_order",
                () -> createOrderInternal(request)
        );
    }

    @Transactional
    public BarBillRecord createOrderBill(String orderId, BarOrderBillCreateRequest request) {
        String id = requireText(orderId, "order_id");
        return executeWrite(
                "createBarOrderBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillRecord.class,
                "bar.bill.created",
                "bar.bill.created_failed",
                "bar.order.create",
                "bar_bill",
                () -> createBillInternal(id, request)
        );
    }

    @Transactional
    public BarOrderRecord deliverOrder(String orderId, BarOrderDeliveryRequest request) {
        String id = requireText(orderId, "order_id");
        return executeWrite(
                "deliverBarOrder:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderRecord.class,
                "bar.order_delivered",
                "bar.order_deliver_failed",
                "bar.order.create",
                "bar_order",
                () -> deliverOrderInternal(id, request)
        );
    }

    @Transactional
    public BarOrderItemRecord addOrderItem(String orderId, BarOrderRequest request) {
        String id = requireText(orderId, "order_id");
        return executeWrite(
                "addBarOrderItem:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderItemRecord.class,
                "bar.order_item.created",
                "bar.order_item.created_failed",
                "bar.order.create",
                "bar_order_item",
                () -> addOrderItemInternal(id, request)
        );
    }

    public PageResponse<BarProductionTicketRecord> listProductionTickets() {
        return readPage(
                "bar.production_ticket.viewed",
                "bar.production_ticket.viewed_failed",
                "bar.production_ticket.view",
                "bar_production_ticket",
                repository::listProductionTickets
        );
    }

    public PageResponse<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId) {
        String id = requireText(ticketId, "ticket_id");
        return readPage(
                "bar.production_ticket_event.viewed",
                "bar.production_ticket_event.viewed_failed",
                "bar.production_ticket.view",
                "bar_production_ticket_event",
                () -> repository.listProductionTicketEvents(id)
        );
    }

    @Transactional
    public BarProductionTicketRecord transitionProductionTicket(
            String ticketId,
            BarProductionTicketTransitionRequest request
    ) {
        String id = requireText(ticketId, "ticket_id");
        return executeWrite(
                "transitionBarProductionTicket:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarProductionTicketRecord.class,
                "bar.production_ticket.transitioned",
                "bar.production_ticket.transition_failed",
                "bar.production_ticket.manage",
                "bar_production_ticket",
                () -> transitionProductionTicketInternal(id, request)
        );
    }

    public PageResponse<BarProductRecord> listProducts() {
        return readPage("bar.product.viewed", "bar.product.viewed_failed", "bar.order.create", "bar_product", repository::listProducts);
    }

    @Transactional
    public BarProductRecord createProduct(BarProductRequest request) {
        return executeWrite(
                "createBarProduct",
                request == null ? null : request.idempotencyKey(),
                request,
                BarProductRecord.class,
                "bar.product.created",
                "bar.product.create_failed",
                "bar.order.create",
                "bar_product",
                () -> createProductInternal(request)
        );
    }

    public PageResponse<BarReservationRecord> listReservations() {
        return readPage(
                "bar.reservation.viewed",
                "bar.reservation.view_failed",
                "bar.reservation.view",
                "bar_reservation",
                repository::listReservations
        );
    }

    @Transactional
    public BarReservationRecord createReservation(BarReservationRequest request) {
        return executeWrite(
                "createBarReservation",
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationRecord.class,
                "bar.reservation.created",
                "bar.reservation.create_failed",
                "bar.reservation.create",
                "bar_reservation",
                () -> createReservationInternal(request, null, "staff", "web")
        );
    }

    @Transactional
    public BarTableGuestCheckinRecord checkInReservationGuest(String reservationId, VerificationRequest request) {
        String id = requireText(reservationId, "reservation_id");
        return executeWrite(
                "checkInBarReservationGuest:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableGuestCheckinRecord.class,
                "bar.table_guest.checked_in",
                "bar.table_guest.rejected",
                "bar.table.guest_checkin",
                "bar_table_guest_checkin",
                () -> checkInGuestInternal(id, request)
        );
    }

    public PageResponse<BarTableRecord> listTables() {
        return readPage("bar.table.viewed", "bar.table.view_failed", "bar.table.view", "bar_table", repository::listTables);
    }

    @Transactional
    public BarTableRecord createTable(BarTableRequest request) {
        return executeWrite(
                "createBarTable",
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableRecord.class,
                "bar.table.changed",
                "bar.table.change_failed",
                "bar.table.manage",
                "bar_table",
                () -> createTableInternal(request)
        );
    }

    @Transactional
    public BarTableRecord openTable(String tableId, OpenBarTableRequest request) {
        String id = requireText(tableId, "table_id");
        return executeWrite(
                "openBarTable:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableRecord.class,
                "bar.table.opened",
                "bar.table.open_failed",
                "bar.table.open_close",
                "bar_table",
                () -> openTableInternal(id, request)
        );
    }

    public PageResponse<BarTableSessionRecord> listTableSessions() {
        return readPage(
                "bar.table_session.viewed",
                "bar.table_session.view_failed",
                "bar.table_session.view",
                "bar_table_session",
                repository::listTableSessions
        );
    }

    @Transactional
    public BarTableSessionRecord createTableSession(BarTableSessionRequest request) {
        return executeWrite(
                "createBarTableSession",
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionRecord.class,
                "bar.table_session.created",
                "bar.table_session.create_failed",
                "bar.table_session.create",
                "bar_table_session",
                () -> createTableSessionInternal(request)
        );
    }

    @Transactional
    public BarTableSessionAdjustmentRecord transferTableSession(String sessionId, BarTableTransferRequest request) {
        String id = requireText(sessionId, "session_id");
        return executeWrite(
                "transferBarTableSession:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionAdjustmentRecord.class,
                "bar.table.transferred",
                "bar.table.transfer_failed",
                "bar.table_session.transfer",
                "bar_table_session_adjustment",
                () -> transferTableSessionInternal(id, request)
        );
    }

    @Transactional
    public BarTableSessionAdjustmentRecord mergeTableSessions(String sessionId, BarTableMergeRequest request) {
        String id = requireText(sessionId, "session_id");
        return executeWrite(
                "mergeBarTableSession:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionAdjustmentRecord.class,
                "bar.table.merged",
                "bar.table.merge_failed",
                "bar.table_session.merge",
                "bar_table_session_adjustment",
                () -> mergeTableSessionsInternal(id, request)
        );
    }

    @Transactional
    public BarTableSessionAdjustmentRecord renewTableSession(String sessionId, BarTableRenewRequest request) {
        String id = requireText(sessionId, "session_id");
        return executeWrite(
                "renewBarTableSession:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionAdjustmentRecord.class,
                "bar.table.renewed",
                "bar.table.renew_failed",
                "bar.table_session.renew",
                "bar_table_session_adjustment",
                () -> renewTableSessionInternal(id, request)
        );
    }

    @Transactional
    public PaymentIntentRecord createBillPayIntent(String billId, PaymentIntentRequest request) {
        String id = requireText(billId, "bill_id");
        return executeWrite(
                "createBarBillPayIntent:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                PaymentIntentRecord.class,
                "payment.intent_created",
                "payment.intent_failed",
                "finance.payment_intent.create",
                "finance_payment_intent",
                () -> createBillPayIntentInternal(id, request)
        );
    }

    @Transactional
    public BarBillRecord settleBill(String billId, BarBillSettleRequest request) {
        String id = requireText(billId, "bill_id");
        return executeWrite(
                "settleBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillRecord.class,
                "bar.bill.settled",
                "bar.bill.settle_failed",
                "finance.payment.register",
                "bar_bill",
                () -> settleBillInternal(id, request)
        );
    }

    @Transactional
    public BarBillAdjustmentRecord applyBillAdjustment(String billId, BarBillAdjustmentRequest request) {
        String id = requireText(billId, "bill_id");
        return executeWrite(
                "adjustBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillAdjustmentRecord.class,
                "bar.bill.adjusted",
                "bar.bill.adjust_failed",
                "bar.bill.adjust",
                "bar_bill_adjustment",
                () -> applyBillAdjustmentInternal(id, request)
        );
    }

    public PageResponse<BarBillOperationRecord> listBillOperations(String billId) {
        String id = requireText(billId, "bill_id");
        return readPage(
                "bar.bill_operations.viewed",
                "bar.bill_operations.view_failed",
                "bar.bill.split_merge",
                "bar_bill_operation",
                () -> repository.listBillOperations(id)
        );
    }

    @Transactional
    public BarBillOperationRecord splitBill(String billId, BarBillSplitRequest request) {
        String id = requireText(billId, "bill_id");
        return executeWrite(
                "splitBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillOperationRecord.class,
                "bar.bill.split",
                "bar.bill.split_failed",
                "bar.bill.split_merge",
                "bar_bill_operation",
                () -> splitBillInternal(id, request)
        );
    }

    @Transactional
    public BarBillOperationRecord mergeBills(String billId, BarBillMergeRequest request) {
        String id = requireText(billId, "bill_id");
        return executeWrite(
                "mergeBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillOperationRecord.class,
                "bar.bill.merged",
                "bar.bill.merge_failed",
                "bar.bill.split_merge",
                "bar_bill_operation",
                () -> mergeBillsInternal(id, request)
        );
    }

    @Transactional
    public BarCommissionCalculationRecord calculateCommission(String billId, CommissionCalculateRequest request) {
        String id = requireText(billId, "bill_id");
        return executeWrite(
                "calculateBarTableCommission:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarCommissionCalculationRecord.class,
                "bar.table_commission.calculated",
                "bar.table_commission.calculate_failed",
                "bar.table_commission.calculate",
                "bar_commission_calculation",
                () -> calculateCommissionInternal(id, request)
        );
    }

    private BarAgeVerificationRecord createAgeVerificationRecord(VerificationRequest request) {
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

    private BarTableRecord createTableInternal(BarTableRequest request) {
        BarTableRequest actual = requireRequest(request);
        Instant now = Instant.now();
        String slug = slug(actual.idempotencyKey());
        int allowedFreeGuests = nonNegative(actual.allowedFreeEntryCount(), "allowed_free_entry_count");
        long minimumSpend = nonNegative(actual.minimumSpendCent(), "minimum_spend_cent");
        return repository.saveTable(new BarTableRecord(
                "table_" + slug,
                requireText(actual.barId(), "bar_id"),
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

    private BarTableRecord openTableInternal(String tableId, OpenBarTableRequest request) {
        OpenBarTableRequest actual = requireRequest(request);
        BarTableRecord table = repository.findTableById(tableId)
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "bar table not found"));
        if (!"active".equals(table.status())) {
            throw ApiException.conflict("BAR_TABLE_NOT_OPENABLE", "bar table is not active");
        }
        String reservationId = requireText(actual.reservationId(), "reservation_id");
        BarReservationRecord reservation = repository.findReservationById(reservationId)
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "bar reservation not found"));
        if (!table.id().equals(reservation.tableId())) {
            throw ApiException.conflict("BAR_TABLE_RESERVATION_MISMATCH", "reservation is not for this table");
        }
        Instant now = Instant.now();
        return repository.saveTable(new BarTableRecord(
                table.id(),
                table.barId(),
                table.tableCode(),
                table.tableName(),
                table.tableType(),
                table.capacity(),
                table.allowedPassFreeGuestCount(),
                table.minConsumptionCent(),
                table.currency(),
                "opened",
                table.createdAt(),
                now
        ));
    }

    private BarTableSessionRecord createTableSessionInternal(BarTableSessionRequest request) {
        BarTableSessionRequest actual = requireRequest(request);
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
                requireText(actual.merchantId(), "merchant_id"),
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

    private BarTableSessionAdjustmentRecord transferTableSessionInternal(String sessionId, BarTableTransferRequest request) {
        BarTableTransferRequest actual = requireRequest(request);
        BarTableSessionRecord session = requireOpenTableSession(sessionId);
        requireSessionVersion(session, actual.sessionVersion());
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
        BarTableSessionRecord updated = repository.saveTableSession(new BarTableSessionRecord(
                session.id(),
                session.barId(),
                session.merchantId(),
                session.businessDate(),
                targetTable.id(),
                session.reservationId(),
                session.customerId(),
                session.guestCount(),
                targetTable.minConsumptionCent(),
                targetTable.allowedPassFreeGuestCount(),
                session.salesStaffId(),
                session.serviceStaffId(),
                session.status(),
                session.openedAt(),
                session.closedAt(),
                session.sourceSessionId(),
                session.sessionVersion() + 1,
                session.createdAt(),
                now
        ));
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
                requireText(actual.requestedByUserId(), "requested_by_user_id"),
                targetTable.minConsumptionCent() > session.minConsumptionSnapshotCent(),
                actual.idempotencyKey(),
                safeList(actual.evidenceAttachmentIds()),
                sessionSnapshot(session, updated)
        ));
    }

    private BarTableSessionAdjustmentRecord mergeTableSessionsInternal(String primarySessionId, BarTableMergeRequest request) {
        BarTableMergeRequest actual = requireRequest(request);
        BarTableSessionRecord primary = requireOpenTableSession(primarySessionId);
        BarTableSessionRecord merged = requireOpenTableSession(requireText(actual.mergedSessionId(), "merged_session_id"));
        requireSessionVersion(primary, actual.primarySessionVersion());
        requireSessionVersion(merged, actual.mergedSessionVersion());
        if (!primary.barId().equals(merged.barId())
                || !primary.merchantId().equals(merged.merchantId())
                || !primary.businessDate().equals(merged.businessDate())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "table sessions can only merge within same bar, merchant and business date");
        }
        BarTableRecord primaryTable = repository.findTableById(primary.tableId())
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "primary bar table not found"));
        BarTableRecord mergedTable = repository.findTableById(merged.tableId())
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "merged bar table not found"));
        Instant now = Instant.now();
        BarTableSessionRecord updatedPrimary = repository.saveTableSession(new BarTableSessionRecord(
                primary.id(),
                primary.barId(),
                primary.merchantId(),
                primary.businessDate(),
                primary.tableId(),
                primary.reservationId(),
                primary.customerId(),
                primary.guestCount(),
                primary.minConsumptionSnapshotCent(),
                primary.allowedPassFreeGuestCountSnapshot(),
                primary.salesStaffId(),
                primary.serviceStaffId(),
                primary.status(),
                primary.openedAt(),
                primary.closedAt(),
                primary.sourceSessionId(),
                primary.sessionVersion() + 1,
                primary.createdAt(),
                now
        ));
        repository.saveTableSession(new BarTableSessionRecord(
                merged.id(),
                merged.barId(),
                merged.merchantId(),
                merged.businessDate(),
                merged.tableId(),
                merged.reservationId(),
                merged.customerId(),
                merged.guestCount(),
                merged.minConsumptionSnapshotCent(),
                merged.allowedPassFreeGuestCountSnapshot(),
                merged.salesStaffId(),
                merged.serviceStaffId(),
                "merged",
                merged.openedAt(),
                now,
                primary.id(),
                merged.sessionVersion() + 1,
                merged.createdAt(),
                now
        ));
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
                requireText(actual.requestedByUserId(), "requested_by_user_id"),
                false,
                actual.idempotencyKey(),
                safeList(actual.evidenceAttachmentIds()),
                sessionSnapshot(primary, updatedPrimary)
        ));
    }

    private BarTableSessionAdjustmentRecord renewTableSessionInternal(String sessionId, BarTableRenewRequest request) {
        BarTableRenewRequest actual = requireRequest(request);
        BarTableSessionRecord session = requireOpenTableSession(sessionId);
        requireSessionVersion(session, actual.sessionVersion());
        Instant renewedUntil = requireInstant(actual.renewedUntil(), "renewed_until");
        Instant now = Instant.now();
        if (!renewedUntil.isAfter(now)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "renewed_until must be in the future");
        }
        long additionalMinConsumption = nonNegative(actual.additionalMinConsumptionCent(), "additional_min_consumption_cent");
        long additionalDeposit = nonNegative(actual.additionalDepositAmountCent(), "additional_deposit_amount_cent");
        long additionalServiceFee = nonNegative(actual.additionalServiceFeeCent(), "additional_service_fee_cent");
        BarTableSessionRecord updated = repository.saveTableSession(new BarTableSessionRecord(
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
        ));
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
                requireText(actual.requestedByUserId(), "requested_by_user_id"),
                additionalMinConsumption > 0 || additionalDeposit > 0 || additionalServiceFee > 0,
                actual.idempotencyKey(),
                safeList(actual.evidenceAttachmentIds()),
                sessionSnapshot(session, updated)
        ));
    }

    private BarReservationRecord createReservationInternal(
            BarReservationRequest request,
            String customerId,
            String orderSource,
            String terminal
    ) {
        BarReservationRequest actual = requireRequest(request);
        BarTableRecord table = repository.findTableById(requireText(actual.tableId(), "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "bar table not found"));
        String barId = requireText(actual.barId(), "bar_id");
        if (!barId.equals(table.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "reservation table is not in this bar");
        }
        Instant now = Instant.now();
        return repository.saveReservation(new BarReservationRecord(
                "reservation_" + slug(actual.idempotencyKey()),
                barId,
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
                now
        ));
    }

    private BarTableGuestCheckinRecord checkInGuestInternal(String reservationId, VerificationRequest request) {
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

    private BarProductRecord createProductInternal(BarProductRequest request) {
        BarProductRequest actual = requireRequest(request);
        Instant now = Instant.now();
        boolean stockManaged = Boolean.TRUE.equals(actual.stockDeductionRequired());
        return repository.saveProduct(new BarProductRecord(
                "product_" + slug(actual.idempotencyKey()),
                requireText(actual.barId(), "bar_id"),
                requireText(actual.productName(), "product_name"),
                requireText(actual.category(), "category"),
                nonNegative(actual.priceCent(), "price_cent"),
                CURRENCY_CNY,
                trimToNull(actual.inventoryItemId()),
                trimToNull(actual.stockLocationId()),
                stockManaged,
                trimToDefault(actual.status(), "active"),
                now,
                now
        ));
    }

    private BarOrderRecord createOrderInternal(BarOrderRequest request) {
        BarOrderRequest actual = requireRequest(request);
        String barId = requireText(actual.barId(), "bar_id");
        BarTableRecord table = repository.findTableById(requireText(actual.tableId(), "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "bar table not found"));
        if (!barId.equals(table.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "order table is not in this bar");
        }
        BarReservationRecord reservation = null;
        if (trimToNull(actual.reservationId()) != null) {
            reservation = repository.findReservationById(actual.reservationId())
                    .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "bar reservation not found"));
            if (!barId.equals(reservation.barId())) {
                throw ApiException.conflict("BAR_SCOPE_MISMATCH", "order reservation is not in this bar");
            }
        }
        String orderId = "order_" + slug(actual.idempotencyKey());
        long total = 0L;
        Instant now = Instant.now();
        List<BarOrderItemRecord> orderItems = new ArrayList<>();
        for (Map<String, Object> item : requireItems(actual.items(), "items")) {
            String productId = requireItemText(item, "product_id", "sku_id", "skuId");
            BarProductRecord product = repository.findProductById(productId)
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "bar product not found"));
            int quantity = requirePositiveItemQuantity(item, "quantity");
            long amount = product.priceCent() * quantity;
            total += amount;
            orderItems.add(new BarOrderItemRecord(
                    "item_" + slug(orderId + ":" + product.id()),
                    orderId,
                    product.id(),
                    product.productName(),
                    quantity,
                    product.priceCent(),
                    amount,
                    "pending",
                    now,
                    now
            ));
        }
        BarOrderRecord order = repository.saveOrder(new BarOrderRecord(
                orderId,
                barId,
                table.id(),
                reservation == null ? null : reservation.id(),
                reservation == null ? null : reservation.customerId(),
                "staff",
                "created",
                total,
                CURRENCY_CNY,
                "web",
                null,
                trimToNull(actual.salespersonUserId()),
                now,
                now
        ));
        orderItems.forEach(repository::saveOrderItem);
        repository.saveProductionTicket(new BarProductionTicketRecord(
                "ticket_" + slug(actual.idempotencyKey()),
                order.id(),
                order.barId(),
                "created",
                now,
                now
        ));
        return order;
    }

    private BarOrderItemRecord addOrderItemInternal(String orderId, BarOrderRequest request) {
        BarOrderRequest actual = requireRequest(request);
        BarOrderRecord order = repository.findOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
        BarOrderItemRecord last = null;
        long total = order.totalAmountCent();
        Instant now = Instant.now();
        for (Map<String, Object> item : requireItems(actual.items(), "items")) {
            String productId = requireItemText(item, "product_id", "sku_id", "skuId");
            BarProductRecord product = repository.findProductById(productId)
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "bar product not found"));
            int quantity = requirePositiveItemQuantity(item, "quantity");
            long amount = product.priceCent() * quantity;
            total += amount;
            last = repository.saveOrderItem(new BarOrderItemRecord(
                    "item_" + slug(orderId + ":" + product.id() + ":" + actual.idempotencyKey()),
                    orderId,
                    product.id(),
                    product.productName(),
                    quantity,
                    product.priceCent(),
                    amount,
                    "pending",
                    now,
                    now
            ));
        }
        repository.saveOrder(new BarOrderRecord(
                order.id(),
                order.barId(),
                order.tableId(),
                order.reservationId(),
                order.customerId(),
                order.orderSource(),
                order.status(),
                total,
                order.currency(),
                order.createdTerminal(),
                order.deviceId(),
                order.salesStaffId(),
                order.createdAt(),
                now
        ));
        return last;
    }

    private BarOrderRecord deliverOrderInternal(String orderId, BarOrderDeliveryRequest request) {
        BarOrderDeliveryRequest actual = requireRequest(request);
        BarOrderRecord order = repository.findOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
        Instant now = Instant.now();
        List<Map<String, Object>> deliveredItems = requireItems(actual.deliveredItems(), "delivered_items");
        for (Map<String, Object> delivered : deliveredItems) {
            String itemId = requireItemText(delivered, "order_item_id", "orderItemId", "item_id", "itemId");
            BarOrderItemRecord item = repository.findOrderItemById(itemId)
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_ITEM_NOT_FOUND", "bar order item not found"));
            BarProductRecord product = repository.findProductById(item.productId())
                    .orElseThrow(() -> ApiException.notFound("BAR_PRODUCT_NOT_FOUND", "bar product not found"));
            int quantity = requirePositiveItemQuantity(delivered, "quantity");
            if (quantity > item.quantity()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "delivery quantity exceeds ordered quantity");
            }
            if (product.stockDeductionRequired()) {
                decreaseStock(
                        requireText(product.inventoryItemId(), "inventory_item_id"),
                        requireText(product.stockLocationId(), "stock_location_id"),
                        quantity,
                        order.id(),
                        safeList(actual.evidenceAttachmentIds())
                );
            }
            repository.saveOrderItem(new BarOrderItemRecord(
                    item.id(),
                    item.orderId(),
                    item.productId(),
                    item.productName(),
                    item.quantity(),
                    item.unitPriceCent(),
                    item.amountCent(),
                    "delivered",
                    item.createdAt(),
                    now
            ));
        }
        BarOrderRecord deliveredOrder = repository.saveOrder(new BarOrderRecord(
                order.id(),
                order.barId(),
                order.tableId(),
                order.reservationId(),
                order.customerId(),
                order.orderSource(),
                "delivered",
                order.totalAmountCent(),
                order.currency(),
                order.createdTerminal(),
                order.deviceId(),
                order.salesStaffId(),
                order.createdAt(),
                now
        ));
        markProductionTicketDelivered(order.id(), actual, now);
        return deliveredOrder;
    }

    private BarProductionTicketRecord transitionProductionTicketInternal(
            String ticketId,
            BarProductionTicketTransitionRequest request
    ) {
        BarProductionTicketTransitionRequest actual = requireRequest(request);
        BarProductionTicketRecord current = repository.findProductionTicketById(ticketId)
                .orElseThrow(() -> ApiException.notFound("BAR_PRODUCTION_TICKET_NOT_FOUND", "bar production ticket not found"));
        String idempotencyKey = requireText(actual.idempotencyKey(), "idempotency_key");
        String fromStatus = current.ticketStatus();
        String toStatus = normalizeProductionTicketStatus(actual.targetStatus());
        String actionType = productionActionType(fromStatus, toStatus);
        Instant now = Instant.now();
        BarProductionTicketRecord updated = repository.saveProductionTicket(new BarProductionTicketRecord(
                current.id(),
                current.orderId(),
                current.barId(),
                toStatus,
                current.createdAt(),
                now
        ));
        repository.saveProductionTicketEvent(new BarProductionTicketEventRecord(
                "production_event_" + slug(current.id() + ":" + toStatus + ":" + idempotencyKey),
                current.id(),
                current.orderId(),
                current.barId(),
                actionType,
                fromStatus,
                toStatus,
                requireText(actual.stationCode(), "station_code"),
                requireText(actual.operatorUserId(), "operator_user_id"),
                trimToDefault(actual.reason(), trimToDefault(actual.operatorNote(), actionType)),
                idempotencyKey,
                safeList(actual.evidenceAttachmentIds()),
                now
        ));
        return updated;
    }

    private void markProductionTicketDelivered(
            String orderId,
            BarOrderDeliveryRequest request,
            Instant now
    ) {
        repository.findProductionTicketByOrderId(orderId)
                .filter(ticket -> !"cancelled".equals(ticket.ticketStatus()))
                .filter(ticket -> !"exception".equals(ticket.ticketStatus()))
                .filter(ticket -> !"delivered".equals(ticket.ticketStatus()))
                .ifPresent(ticket -> {
                    BarOrderDeliveryRequest actual = requireRequest(request);
                    String idempotencyKey = requireText(actual.idempotencyKey(), "idempotency_key");
                    repository.saveProductionTicket(new BarProductionTicketRecord(
                            ticket.id(),
                            ticket.orderId(),
                            ticket.barId(),
                            "delivered",
                            ticket.createdAt(),
                            now
                    ));
                    repository.saveProductionTicketEvent(new BarProductionTicketEventRecord(
                            "production_event_" + slug(ticket.id() + ":delivered:" + idempotencyKey),
                            ticket.id(),
                            ticket.orderId(),
                            ticket.barId(),
                            "deliver",
                            ticket.ticketStatus(),
                            "delivered",
                            null,
                            requireText(actual.deliveredByUserId(), "delivered_by_user_id"),
                            trimToDefault(actual.operatorNote(), "delivered by service staff"),
                            idempotencyKey,
                            safeList(actual.evidenceAttachmentIds()),
                            now
                    ));
                });
    }

    private BarBillRecord createBillInternal(String orderId, BarOrderBillCreateRequest request) {
        requireRequest(request);
        BarOrderRecord order = repository.findOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
        long subtotal = repository.listOrderItems(order.id()).stream()
                .mapToLong(BarOrderItemRecord::amountCent)
                .sum();
        BarReservationRecord reservation = order.reservationId() == null
                ? null
                : repository.findReservationById(order.reservationId()).orElse(null);
        long depositDeduct = reservation == null ? 0L : Math.min(reservation.depositAmountCent(), subtotal);
        long payable = subtotal - depositDeduct;
        Instant now = Instant.now();
        String salesRoleType = trimToNull(order.salesStaffId()) == null ? "non_sales" : "sales";
        return repository.saveBill(new BarBillRecord(
                "bill_" + slug(order.id() + ":" + now.toEpochMilli()),
                "BILL" + now.toEpochMilli(),
                order.id(),
                order.barId(),
                order.tableId(),
                order.reservationId(),
                subtotal,
                0L,
                depositDeduct,
                payable,
                0L,
                CURRENCY_CNY,
                "unpaid",
                "unsettled",
                order.salesStaffId(),
                salesRoleType,
                now,
                now
        ));
    }

    private PaymentIntentRecord createBillPayIntentInternal(String billId, PaymentIntentRequest request) {
        PaymentIntentRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        String businessOrderType = requireText(actual.businessOrderType(), "business_order_type");
        String businessOrderId = requireText(actual.businessOrderId(), "business_order_id");
        if (!"bar_bill".equals(businessOrderType) || !bill.id().equals(businessOrderId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment intent business order must match bar bill");
        }
        long amountCent = positive(actual.amountCent(), "amount_cent");
        if (amountCent != bill.payableAmountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment intent amount must match bill payable amount");
        }
        Instant now = Instant.now();
        return repository.savePaymentIntent(new PaymentIntentRecord(
                "pay_intent_" + slug(actual.idempotencyKey()),
                businessOrderType,
                businessOrderId,
                amountCent,
                trimToDefault(actual.currency(), CURRENCY_CNY),
                requireText(actual.channel(), "channel"),
                "created",
                now,
                now
        ));
    }

    private BarBillRecord settleBillInternal(String billId, BarBillSettleRequest request) {
        BarBillSettleRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        requireText(actual.paymentId(), "payment_id");
        long settlementAmount = actual.settlementAmountCent() == null
                ? bill.payableAmountCent()
                : nonNegative(actual.settlementAmountCent(), "settlement_amount_cent");
        Instant now = Instant.now();
        return repository.saveBill(new BarBillRecord(
                bill.id(),
                bill.billNo(),
                bill.orderId(),
                bill.barId(),
                bill.tableId(),
                bill.reservationId(),
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                bill.payableAmountCent(),
                settlementAmount,
                bill.currency(),
                settlementAmount >= bill.payableAmountCent() ? "paid" : "partial_paid",
                settlementAmount >= bill.payableAmountCent() ? "settled" : "partial_settled",
                bill.salesStaffId(),
                bill.salesRoleType(),
                bill.createdAt(),
                now
        ));
    }

    private BarBillAdjustmentRecord applyBillAdjustmentInternal(String billId, BarBillAdjustmentRequest request) {
        BarBillAdjustmentRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        if ("settled".equals(bill.settlementStatus())) {
            throw ApiException.conflict("BAR_BILL_NOT_ADJUSTABLE", "settled bar bill cannot be adjusted");
        }
        String type = requireText(actual.adjustmentType(), "adjustment_type");
        String reason = requireText(actual.reason(), "reason");
        String requestedBy = requireText(actual.requestedByUserId(), "requested_by_user_id");
        long beforePayable = bill.payableAmountCent();
        long amount = nonNegative(actual.amountCent(), "amount_cent");
        long afterPayable;
        String arApItemId = null;
        String paymentStatus = bill.paymentStatus();
        String settlementStatus = bill.settlementStatus();
        if ("service_fee".equals(type) || "min_spend_top_up".equals(type)) {
            amount = positive(actual.amountCent(), "amount_cent");
            afterPayable = beforePayable + amount;
        } else if ("on_account".equals(type)) {
            if (amount != 0L) {
                throw ApiException.badRequest("VALIDATION_FAILED", "on_account amount_cent must be zero");
            }
            afterPayable = beforePayable;
            paymentStatus = "on_account";
            settlementStatus = "receivable";
            if ("on_account".equals(bill.paymentStatus()) || "receivable".equals(bill.settlementStatus())) {
                throw ApiException.conflict("BAR_BILL_ALREADY_ON_ACCOUNT", "bar bill is already on account");
            }
            BarOrderRecord order = repository.findOrderById(bill.orderId())
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
            arApItemId = receivableLedgerService.createReceivableForOnAccountBill(
                    bill,
                    order,
                    requireText(actual.idempotencyKey(), "idempotency_key"),
                    reason,
                    safeList(actual.evidenceAttachmentIds())
            );
        } else {
            throw ApiException.badRequest("VALIDATION_FAILED", "unsupported bar bill adjustment type");
        }
        Instant now = Instant.now();
        repository.saveBill(new BarBillRecord(
                bill.id(),
                bill.billNo(),
                bill.orderId(),
                bill.barId(),
                bill.tableId(),
                bill.reservationId(),
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                afterPayable,
                bill.paidAmountCent(),
                bill.currency(),
                paymentStatus,
                settlementStatus,
                bill.salesStaffId(),
                bill.salesRoleType(),
                bill.createdAt(),
                now
        ));
        return repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                "bill_adjustment_" + slug(bill.id() + ":" + type + ":" + actual.idempotencyKey()),
                bill.id(),
                type,
                amount,
                beforePayable,
                afterPayable,
                reason,
                requestedBy,
                true,
                "applied",
                requireText(actual.idempotencyKey(), "idempotency_key"),
                safeList(actual.evidenceAttachmentIds()),
                arApItemId,
                billAdjustmentSnapshot(bill, afterPayable, paymentStatus, settlementStatus, arApItemId),
                now
        ));
    }

    private BarBillOperationRecord splitBillInternal(String billId, BarBillSplitRequest request) {
        BarBillSplitRequest actual = requireRequest(request);
        BarBillRecord source = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        requireBillMovable(source, "BAR_BILL_NOT_SPLITTABLE");
        long amount = positive(actual.splitAmountCent(), "split_amount_cent");
        if (amount >= source.payableAmountCent()) {
            throw ApiException.conflict("BAR_BILL_SPLIT_AMOUNT_INVALID", "split amount must be less than payable amount");
        }
        String reason = requireText(actual.reason(), "reason");
        String requestedBy = requireText(actual.requestedByUserId(), "requested_by_user_id");
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        Instant now = Instant.now();
        String generatedBillId = "bill_split_" + slug(source.id() + ":" + key);
        BarBillRecord updatedSource = new BarBillRecord(
                source.id(),
                source.billNo(),
                source.orderId(),
                source.barId(),
                source.tableId(),
                source.reservationId(),
                source.subtotalAmountCent() - amount,
                source.discountAmountCent(),
                source.depositDeductAmountCent(),
                source.payableAmountCent() - amount,
                source.paidAmountCent(),
                source.currency(),
                source.paymentStatus(),
                source.settlementStatus(),
                source.salesStaffId(),
                source.salesRoleType(),
                source.createdAt(),
                now
        );
        BarBillRecord generated = new BarBillRecord(
                generatedBillId,
                splitBillNo(source.billNo(), key),
                source.orderId(),
                source.barId(),
                source.tableId(),
                source.reservationId(),
                amount,
                0L,
                0L,
                amount,
                0L,
                source.currency(),
                "unpaid",
                "unsettled",
                source.salesStaffId(),
                source.salesRoleType(),
                now,
                now
        );
        repository.saveBill(updatedSource);
        repository.saveBill(generated);
        return repository.saveBillOperation(new BarBillOperationRecord(
                "bill_operation_" + slug(source.id() + ":split:" + key),
                "split",
                source.id(),
                generated.id(),
                generated.id(),
                amount,
                reason,
                requestedBy,
                "applied",
                key,
                safeList(actual.evidenceAttachmentIds()),
                billOperationSnapshot(source, updatedSource, generated),
                now
        ));
    }

    private BarBillOperationRecord mergeBillsInternal(String billId, BarBillMergeRequest request) {
        BarBillMergeRequest actual = requireRequest(request);
        String secondaryBillId = requireText(actual.mergedBillId(), "merged_bill_id");
        if (billId.equals(secondaryBillId)) {
            throw ApiException.conflict("BAR_BILL_MERGE_SAME_BILL", "cannot merge bill into itself");
        }
        BarBillRecord primary = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "primary bill not found"));
        BarBillRecord secondary = repository.findBillById(secondaryBillId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "secondary bill not found"));
        requireBillMovable(primary, "BAR_BILL_NOT_MERGEABLE");
        requireBillMovable(secondary, "BAR_BILL_NOT_MERGEABLE");
        if (!primary.barId().equals(secondary.barId())) {
            throw ApiException.conflict("BAR_BILL_MERGE_BAR_MISMATCH", "bills must belong to the same bar");
        }
        String reason = requireText(actual.reason(), "reason");
        String requestedBy = requireText(actual.requestedByUserId(), "requested_by_user_id");
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        long amount = secondary.payableAmountCent();
        Instant now = Instant.now();
        BarBillRecord updatedPrimary = new BarBillRecord(
                primary.id(),
                primary.billNo(),
                primary.orderId(),
                primary.barId(),
                primary.tableId(),
                primary.reservationId(),
                primary.subtotalAmountCent() + secondary.subtotalAmountCent(),
                primary.discountAmountCent() + secondary.discountAmountCent(),
                primary.depositDeductAmountCent() + secondary.depositDeductAmountCent(),
                primary.payableAmountCent() + secondary.payableAmountCent(),
                primary.paidAmountCent(),
                primary.currency(),
                primary.paymentStatus(),
                primary.settlementStatus(),
                primary.salesStaffId(),
                primary.salesRoleType(),
                primary.createdAt(),
                now
        );
        BarBillRecord mergedSecondary = new BarBillRecord(
                secondary.id(),
                secondary.billNo(),
                secondary.orderId(),
                secondary.barId(),
                secondary.tableId(),
                secondary.reservationId(),
                0L,
                0L,
                0L,
                0L,
                0L,
                secondary.currency(),
                "merged",
                "merged",
                secondary.salesStaffId(),
                secondary.salesRoleType(),
                secondary.createdAt(),
                now
        );
        repository.saveBill(updatedPrimary);
        repository.saveBill(mergedSecondary);
        return repository.saveBillOperation(new BarBillOperationRecord(
                "bill_operation_" + slug(primary.id() + ":merge:" + secondary.id() + ":" + key),
                "merge",
                primary.id(),
                secondary.id(),
                null,
                amount,
                reason,
                requestedBy,
                "applied",
                key,
                safeList(actual.evidenceAttachmentIds()),
                billOperationSnapshot(primary, updatedPrimary, secondary),
                now
        ));
    }

    private BarCommissionCalculationRecord calculateCommissionInternal(String billId, CommissionCalculateRequest request) {
        CommissionCalculateRequest actual = requireRequest(request);
        String bodyBillId = trimToNull(actual.billId());
        if (bodyBillId != null && !billId.equals(bodyBillId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "bill_id must match path");
        }
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        String roleType = trimToNull(bill.salesStaffId()) == null ? "non_sales" : "sales";
        int rateBp = "sales".equals(roleType) ? 1500 : 1000;
        long base = Math.max(0L, bill.subtotalAmountCent() - bill.discountAmountCent());
        long amount = base * rateBp / 10_000L;
        return repository.saveCommissionCalculation(new BarCommissionCalculationRecord(
                "commission_" + slug(actual.idempotencyKey()),
                bill.id(),
                bill.salesStaffId(),
                roleType,
                base,
                rateBp,
                amount,
                "calculated",
                Instant.now()
        ));
    }

    private void decreaseStock(
            String itemId,
            String locationId,
            int quantity,
            String orderId,
            List<String> evidenceAttachmentIds
    ) {
        StockBalanceSnapshot current = repository.findStockBalance(itemId, locationId)
                .orElseGet(() -> new StockBalanceSnapshot(
                        "balance_" + slug(itemId + ":" + locationId),
                        itemId,
                        locationId,
                        0,
                        0,
                        0,
                        Instant.now()
                ));
        if (current.availableQuantity() < quantity) {
            throw ApiException.conflict("STOCK_INSUFFICIENT", "available stock is insufficient");
        }
        int newQuantity = current.quantity() - quantity;
        repository.saveStockBalance(new StockBalanceSnapshot(
                current.id(),
                current.itemId(),
                current.locationId(),
                newQuantity,
                current.reservedQuantity(),
                newQuantity - current.reservedQuantity(),
                Instant.now()
        ));
        repository.saveStockMovement(new StockMovementSnapshot(
                "movement_" + slug(orderId + ":" + itemId + ":" + quantity),
                "outbound",
                itemId,
                locationId,
                null,
                quantity,
                "bar_order",
                orderId,
                null,
                null,
                evidenceAttachmentIds,
                "completed",
                Instant.now()
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

    private BarTableRecord tableWithStatus(BarTableRecord table, String status, Instant updatedAt) {
        return new BarTableRecord(
                table.id(),
                table.barId(),
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

    private String billAdjustmentSnapshot(
            BarBillRecord before,
            long afterPayableAmountCent,
            String afterPaymentStatus,
            String afterSettlementStatus,
            String arApItemId
    ) {
        return """
                {"bill_id":"%s","payable_before":%d,"payable_after":%d,"payment_status_before":"%s","payment_status_after":"%s","settlement_status_before":"%s","settlement_status_after":"%s","ar_ap_item_id":"%s"}
                """.formatted(
                jsonText(before.id()),
                before.payableAmountCent(),
                afterPayableAmountCent,
                jsonText(before.paymentStatus()),
                jsonText(afterPaymentStatus),
                jsonText(before.settlementStatus()),
                jsonText(afterSettlementStatus),
                jsonText(arApItemId)
        ).trim();
    }

    private void requireBillMovable(BarBillRecord bill, String code) {
        if (bill.paidAmountCent() > 0
                || "settled".equals(bill.settlementStatus())
                || "merged".equals(bill.settlementStatus())
                || "merged".equals(bill.paymentStatus())) {
            throw ApiException.conflict(code, "bar bill has payment, settlement, or merge state");
        }
    }

    private String splitBillNo(String sourceBillNo, String idempotencyKey) {
        String suffix = "-S" + slug(idempotencyKey).substring(0, 6).toUpperCase(Locale.ROOT);
        String prefix = sourceBillNo == null ? "BILL" : sourceBillNo;
        int maxPrefixLength = Math.max(1, 64 - suffix.length());
        return prefix.substring(0, Math.min(prefix.length(), maxPrefixLength)) + suffix;
    }

    private String billOperationSnapshot(BarBillRecord before, BarBillRecord after, BarBillRecord related) {
        return """
                {"primary_bill_id":"%s","related_bill_id":"%s","primary_payable_before":%d,"primary_payable_after":%d,"related_payable":%d}
                """.formatted(
                jsonText(before.id()),
                jsonText(related.id()),
                before.payableAmountCent(),
                after.payableAmountCent(),
                related.payableAmountCent()
        ).trim();
    }

    private String jsonText(String value) {
        return trimToDefault(value, "")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        try {
            List<T> items = action.get();
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
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
                                Map.of("operation_id", operationId)
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
                    null,
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private Instant requireInstant(Instant value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    private LocalDate requireLocalDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    private long positive(Long value, String fieldName) {
        long resolved = value == null ? 0L : value;
        if (resolved <= 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return resolved;
    }

    private long nonNegative(Long value, String fieldName) {
        long resolved = value == null ? 0L : value;
        if (resolved < 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return resolved;
    }

    private int nonNegative(Integer value, String fieldName) {
        int resolved = value == null ? 0 : value;
        if (resolved < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return resolved;
    }

    private List<Map<String, Object>> requireItems(List<Map<String, Object>> items, String fieldName) {
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return items;
    }

    private String requireItemText(Map<String, Object> item, String... names) {
        String value = textFromMap(item, names);
        return requireText(value, names[0]);
    }

    private int requirePositiveItemQuantity(Map<String, Object> item, String name) {
        Object value = item == null ? null : item.get(name);
        if (value instanceof Number number) {
            int quantity = number.intValue();
            if (quantity > 0) {
                return quantity;
            }
        }
        throw ApiException.badRequest("VALIDATION_FAILED", name + " must be positive");
    }

    @SuppressWarnings("unchecked")
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

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private String normalizeProductionTicketStatus(String targetStatus) {
        String status = requireText(targetStatus, "target_status").toLowerCase(Locale.ROOT);
        if (!List.of("accepted", "preparing", "ready", "cancelled", "exception").contains(status)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "target_status is unsupported");
        }
        return status;
    }

    private String productionActionType(String fromStatus, String toStatus) {
        String from = trimToDefault(fromStatus, "created").toLowerCase(Locale.ROOT);
        if ("created".equals(from) && "accepted".equals(toStatus)) {
            return "accept";
        }
        if ("accepted".equals(from) && "preparing".equals(toStatus)) {
            return "start_prepare";
        }
        if ("preparing".equals(from) && "ready".equals(toStatus)) {
            return "mark_ready";
        }
        if (List.of("created", "accepted", "preparing").contains(from) && "cancelled".equals(toStatus)) {
            return "cancel";
        }
        if (List.of("created", "accepted", "preparing").contains(from) && "exception".equals(toStatus)) {
            return "exception";
        }
        throw ApiException.conflict(
                "BAR_PRODUCTION_INVALID_TRANSITION",
                "bar production ticket cannot transition from " + from + " to " + toStatus
        );
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

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        return resolved.length() > 40 ? resolved.substring(0, 40) : resolved;
    }

    private String businessId(Object value) {
        if (value instanceof BarTableRecord record) {
            return record.id();
        }
        if (value instanceof BarTableSessionRecord record) {
            return record.id();
        }
        if (value instanceof BarTableSessionAdjustmentRecord record) {
            return record.id();
        }
        if (value instanceof BarReservationRecord record) {
            return record.id();
        }
        if (value instanceof BarTableGuestCheckinRecord record) {
            return record.id();
        }
        if (value instanceof BarProductRecord record) {
            return record.id();
        }
        if (value instanceof BarOrderRecord record) {
            return record.id();
        }
        if (value instanceof BarOrderItemRecord record) {
            return record.id();
        }
        if (value instanceof BarProductionTicketRecord record) {
            return record.id();
        }
        if (value instanceof BarProductionTicketEventRecord record) {
            return record.id();
        }
        if (value instanceof BarBillRecord record) {
            return record.id();
        }
        if (value instanceof BarBillAdjustmentRecord record) {
            return record.id();
        }
        if (value instanceof BarBillOperationRecord record) {
            return record.id();
        }
        if (value instanceof BarCommissionCalculationRecord record) {
            return record.id();
        }
        if (value instanceof PaymentIntentRecord record) {
            return record.id();
        }
        return null;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
