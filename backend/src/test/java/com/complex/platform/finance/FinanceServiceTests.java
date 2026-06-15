package com.complex.platform.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.dto.ArApItemRequest;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.dto.RefundRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinanceServiceTests {

    private InMemoryFinanceRepository financeRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private FinanceService service;

    @BeforeEach
    void setUp() {
        financeRepository = new InMemoryFinanceRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new FinanceService(
                financeRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository)
        );
    }

    @Test
    void createReceivableItemComputesUnpaidAmountAndWritesFinanceArApCreated() {
        ArApItemRecord created = service.createArApItem(itemRequest("arap-create", "receivable", 128800L));

        assertThat(created.direction()).isEqualTo("receivable");
        assertThat(created.paidAmountCent()).isZero();
        assertThat(created.unpaidAmountCent()).isEqualTo(128800L);
        assertThat(created.currency()).isEqualTo("CNY");
        assertThat(created.status()).isEqualTo("pending_collection");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("finance_ar_ap.created");
    }

    @Test
    void createReceivableItemAcceptsBarBillSourceForOnAccountLedger() {
        ArApItemRecord created = service.createArApItem(new ArApItemRequest(
                "arap-bar-bill-source",
                "bar bill on account",
                List.of("att_bar_bill"),
                "receivable",
                "customer_bar_bill",
                "bar_bill",
                "bill_bar_001",
                128800L,
                LocalDate.parse("2026-06-30"),
                "2026-06"
        ));

        assertThat(created)
                .extracting(
                        ArApItemRecord::direction,
                        ArApItemRecord::sourceType,
                        ArApItemRecord::sourceId,
                        ArApItemRecord::amountCent,
                        ArApItemRecord::status
                )
                .containsExactly("receivable", "bar_bill", "bill_bar_001", 128800L, "pending_collection");
        assertThat(financeRepository.arApItemSources.values())
                .extracting(
                        ArApItemSourceRecord::arApItemId,
                        ArApItemSourceRecord::sourceType,
                        ArApItemSourceRecord::sourceId,
                        ArApItemSourceRecord::amountCent
                )
                .containsExactly(tuple(created.id(), "bar_bill", "bill_bar_001", 128800L));
    }

    @Test
    void duplicateIdempotencyKeyDoesNotCreateDuplicateItem() {
        ArApItemRequest request = itemRequest("arap-idempotent", "payable", 88000L);

        ArApItemRecord first = service.createArApItem(request);
        ArApItemRecord second = service.createArApItem(request);

        assertThat(second).isEqualTo(first);
        assertThat(financeRepository.itemSaveCount).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "finance_ar_ap.created".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void partialPaymentUpdatesLedgerToPartialPaid() {
        ArApItemRecord item = service.createArApItem(itemRequest("arap-partial", "receivable", 128800L));

        ArApPaymentResponse response = service.registerArApPayment(item.id(), paymentRequest("pay-partial", 28800L));

        assertThat(response.payment().amountCent()).isEqualTo(28800L);
        assertThat(response.item().paidAmountCent()).isEqualTo(28800L);
        assertThat(response.item().unpaidAmountCent()).isEqualTo(100000L);
        assertThat(response.item().status()).isEqualTo("partial_paid");
    }

    @Test
    void fullPaymentUpdatesLedgerToSettled() {
        ArApItemRecord item = service.createArApItem(itemRequest("arap-full", "receivable", 128800L));

        ArApPaymentResponse response = service.registerArApPayment(item.id(), paymentRequest("pay-full", 128800L));

        assertThat(response.item().paidAmountCent()).isEqualTo(128800L);
        assertThat(response.item().unpaidAmountCent()).isZero();
        assertThat(response.item().status()).isEqualTo("settled");
    }

    @Test
    void overpaymentFailsWithValidationFailed() {
        ArApItemRecord item = service.createArApItem(itemRequest("arap-overpay", "receivable", 5000L));

        assertThatThrownBy(() -> service.registerArApPayment(item.id(), paymentRequest("pay-overpay", 5001L)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("finance_payment.register_failed");
    }

    @Test
    void writeSideFinanceActionsEmitExactOpenApiAuditEvents() {
        service.createDailyClose(new DailyCloseRequest(
                "daily-close",
                "lock",
                List.of(),
                LocalDate.parse("2026-06-14"),
                "project",
                "project_1",
                false
        ));
        service.createPaymentIntent(new PaymentIntentRequest(
                "payment-intent",
                "create",
                List.of(),
                "ticket_order",
                "order_1",
                128800L,
                "CNY",
                "mock"
        ));
        service.confirmOfflinePayment("payment_1", new OfflinePaymentConfirmRequest(
                "offline-confirm",
                "confirm",
                List.of("attachment_1"),
                "payment_1",
                128800L,
                "cash",
                Instant.parse("2026-06-14T00:05:00Z")
        ));
        service.createRefund(new RefundRequest(
                "refund-create",
                "refund",
                List.of(),
                "payment_1",
                18800L,
                "guest request",
                "original_channel"
        ));

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "finance_daily_close.locked",
                        "payment.intent_created",
                        "finance_payment.registered",
                        "finance_refund.created"
                );
    }

    @Test
    void reconciliationReadModelsReturnPagesAndWriteAuditEvents() {
        financeRepository.saveReconciliationBatch(new ReconciliationBatchRecord(
                "recon_batch_1",
                "RB202606140001",
                "project",
                "project_1",
                LocalDate.parse("2026-06-14"),
                "created",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        financeRepository.saveReconciliationDifference(new ReconciliationDifferenceRecord(
                "recon_diff_1",
                "recon_batch_1",
                "amount_mismatch",
                "payment_1",
                100L,
                "open",
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        PageResponse<ReconciliationBatchRecord> batches = service.listReconciliationBatches();
        PageResponse<ReconciliationDifferenceRecord> differences = service.listReconciliationDifferences();

        assertThat(batches.items()).extracting(ReconciliationBatchRecord::batchNo).contains("RB202606140001");
        assertThat(differences.items()).extracting(ReconciliationDifferenceRecord::differenceType).contains("amount_mismatch");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("finance.reconciliation_batch.viewed", "finance_reconciliation.difference_found");
    }

    private ArApItemRequest itemRequest(String idempotencyKey, String direction, long amountCent) {
        return new ArApItemRequest(
                idempotencyKey,
                "create item",
                List.of(),
                direction,
                "merchant_1",
                "order",
                "order_1_" + idempotencyKey,
                amountCent,
                LocalDate.parse("2026-06-30"),
                "2026-06"
        );
    }

    private ArApPaymentRequest paymentRequest(String idempotencyKey, long amountCent) {
        return new ArApPaymentRequest(
                idempotencyKey,
                "register payment",
                List.of(),
                amountCent,
                "bank_transfer",
                Instant.parse("2026-06-14T01:00:00Z"),
                "bank_flow_" + idempotencyKey
        );
    }

    private static final class InMemoryFinanceRepository implements FinanceRepository {
        private final Map<String, ArApItemRecord> arApItems = new LinkedHashMap<>();
        private final Map<String, ArApItemSourceRecord> arApItemSources = new LinkedHashMap<>();
        private final Map<String, ArApPaymentRecord> arApPayments = new LinkedHashMap<>();
        private final Map<String, DailyCloseRecord> dailyCloses = new LinkedHashMap<>();
        private final Map<String, PaymentIntentRecord> paymentIntents = new LinkedHashMap<>();
        private final Map<String, OfflinePaymentConfirmationRecord> offlineConfirmations = new LinkedHashMap<>();
        private final Map<String, FinanceRefundRecord> refunds = new LinkedHashMap<>();
        private final Map<String, ReconciliationBatchRecord> reconciliationBatches = new LinkedHashMap<>();
        private final Map<String, ReconciliationDifferenceRecord> reconciliationDifferences = new LinkedHashMap<>();
        private int itemSaveCount;

        @Override
        public ArApItemRecord saveArApItem(ArApItemRecord item) {
            itemSaveCount += 1;
            arApItems.put(item.id(), item);
            return item;
        }

        @Override
        public ArApItemSourceRecord saveArApItemSource(ArApItemSourceRecord source) {
            arApItemSources.put(source.id(), source);
            return source;
        }

        @Override
        public List<ArApItemRecord> listArApItems() {
            return new ArrayList<>(arApItems.values());
        }

        @Override
        public Optional<ArApItemRecord> findArApItemById(String id) {
            return Optional.ofNullable(arApItems.get(id));
        }

        @Override
        public ArApItemRecord updateArApItemLedger(ArApItemRecord item) {
            arApItems.put(item.id(), item);
            return item;
        }

        @Override
        public ArApPaymentRecord saveArApPayment(ArApPaymentRecord payment) {
            arApPayments.put(payment.id(), payment);
            return payment;
        }

        @Override
        public DailyCloseRecord saveDailyClose(DailyCloseRecord dailyClose) {
            dailyCloses.put(dailyClose.id(), dailyClose);
            return dailyClose;
        }

        @Override
        public PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent) {
            paymentIntents.put(paymentIntent.id(), paymentIntent);
            return paymentIntent;
        }

        @Override
        public OfflinePaymentConfirmationRecord saveOfflinePaymentConfirmation(OfflinePaymentConfirmationRecord confirmation) {
            offlineConfirmations.put(confirmation.id(), confirmation);
            return confirmation;
        }

        @Override
        public FinanceRefundRecord saveRefund(FinanceRefundRecord refund) {
            refunds.put(refund.id(), refund);
            return refund;
        }

        @Override
        public List<ReconciliationBatchRecord> listReconciliationBatches() {
            return new ArrayList<>(reconciliationBatches.values());
        }

        @Override
        public ReconciliationBatchRecord saveReconciliationBatch(ReconciliationBatchRecord batch) {
            reconciliationBatches.put(batch.id(), batch);
            return batch;
        }

        @Override
        public List<ReconciliationDifferenceRecord> listReconciliationDifferences() {
            return new ArrayList<>(reconciliationDifferences.values());
        }

        @Override
        public ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord difference) {
            reconciliationDifferences.put(difference.id(), difference);
            return difference;
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
