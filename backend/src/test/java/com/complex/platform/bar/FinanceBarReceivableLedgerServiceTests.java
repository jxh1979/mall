package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.ArApItemSourceRecord;
import com.complex.platform.finance.ArApPaymentRecord;
import com.complex.platform.finance.DailyCloseRecord;
import com.complex.platform.finance.FinanceRefundRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.OfflinePaymentConfirmationRecord;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.ReconciliationBatchRecord;
import com.complex.platform.finance.ReconciliationDifferenceRecord;
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

class FinanceBarReceivableLedgerServiceTests {

    private InMemoryFinanceRepository financeRepository;
    private FinanceBarReceivableLedgerService service;

    @BeforeEach
    void setUp() {
        financeRepository = new InMemoryFinanceRepository();
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        AuditEventService auditEventService = new AuditEventService(new InMemoryAuditEventRepository());
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                auditEventService
        );
        FinanceService financeService = new FinanceService(
                financeRepository,
                operations,
                auditEventService,
                authorizationService
        );
        service = new FinanceBarReceivableLedgerService(financeService);
    }

    @Test
    void receivableCreationIsIdempotentByBillEvenWhenAdjustmentKeysDiffer() {
        BarBillRecord bill = bill();
        BarOrderRecord order = order();

        String first = service.createReceivableForOnAccountBill(
                bill,
                order,
                "adjustment-key-1",
                "approved credit",
                List.of("att_1")
        );
        String second = service.createReceivableForOnAccountBill(
                bill,
                order,
                "adjustment-key-2",
                "approved credit duplicate",
                List.of("att_2")
        );

        assertThat(second).isEqualTo(first);
        assertThat(financeRepository.itemSaveCount).isEqualTo(1);
        assertThat(financeRepository.listArApItems())
                .extracting(ArApItemRecord::sourceType, ArApItemRecord::sourceId, ArApItemRecord::amountCent)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("bar_bill", bill.id(), bill.payableAmountCent()));
    }

    private BarBillRecord bill() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarBillRecord(
                "bill_receivable_bridge",
                "BILL-BRIDGE",
                "order_receivable_bridge",
                "bar_1",
                null,
                "table_1",
                "reservation_1",
                128800L,
                0L,
                0L,
                128800L,
                0L,
                "CNY",
                "unpaid",
                "unsettled",
                "staff_sales",
                "sales",
                now,
                now
        );
    }

    private BarOrderRecord order() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarOrderRecord(
                "order_receivable_bridge",
                "bar_1",
                null,
                "table_1",
                "reservation_1",
                "customer_1",
                "staff",
                "created",
                128800L,
                "CNY",
                "web",
                null,
                "staff_sales",
                now,
                now
        );
    }

    private static final class InMemoryFinanceRepository implements FinanceRepository {
        private final Map<String, ArApItemRecord> arApItems = new LinkedHashMap<>();
        private final Map<String, ArApItemSourceRecord> arApItemSources = new LinkedHashMap<>();
        private final Map<String, ArApPaymentRecord> arApPayments = new LinkedHashMap<>();
        private final Map<String, DailyCloseRecord> dailyCloses = new LinkedHashMap<>();
        private final Map<String, FinanceRefundRecord> refunds = new LinkedHashMap<>();
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
        public List<ArApItemRecord> listArApItemsByMerchant(String merchantId) {
            return arApItems.values().stream()
                    .filter(item -> merchantId.equals(item.merchantId()))
                    .toList();
        }

        @Override
        public Optional<ArApItemRecord> findArApItemById(String id) {
            return Optional.ofNullable(arApItems.get(id));
        }

        @Override
        public Optional<ArApItemRecord> findArApItemByIdForUpdate(String id) {
            return Optional.ofNullable(arApItems.get(id));
        }

        @Override
        public ArApItemRecord updateArApItemLedger(ArApItemRecord item) {
            arApItems.put(item.id(), item);
            return item;
        }

        @Override
        public ArApPaymentRecord saveArApPayment(ArApPaymentRecord payment) {
            arApPayments.putIfAbsent(payment.id(), payment);
            return arApPayments.get(payment.id());
        }

        @Override
        public Optional<ArApPaymentRecord> findArApPaymentById(String id) {
            return Optional.ofNullable(arApPayments.get(id));
        }

        @Override
        public long sumRefundedAmountByTargetId(String targetId) {
            return refunds.values().stream()
                    .filter(r -> targetId.equals(r.targetId()))
                    .mapToLong(FinanceRefundRecord::refundAmountCent)
                    .sum();
        }

        @Override
        public DailyCloseRecord saveDailyClose(DailyCloseRecord dailyClose) {
            dailyCloses.put(dailyClose.id(), dailyClose);
            return dailyClose;
        }

        @Override
        public boolean dailyCloseExists(LocalDate businessDate, String scopeType, String scopeId) {
            return dailyCloses.values().stream().anyMatch(dc ->
                    businessDate.equals(dc.businessDate())
                            && scopeType.equals(dc.scopeType())
                            && scopeId.equals(dc.scopeId()));
        }

        @Override
        public PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent) {
            return paymentIntent;
        }

        @Override
        public OfflinePaymentConfirmationRecord saveOfflinePaymentConfirmation(OfflinePaymentConfirmationRecord confirmation) {
            return confirmation;
        }

        @Override
        public FinanceRefundRecord saveRefund(FinanceRefundRecord refund) {
            refunds.put(refund.id(), refund);
            return refund;
        }

        @Override
        public List<ReconciliationBatchRecord> listReconciliationBatches() {
            return List.of();
        }

        @Override
        public ReconciliationBatchRecord saveReconciliationBatch(ReconciliationBatchRecord batch) {
            return batch;
        }

        @Override
        public List<ReconciliationDifferenceRecord> listReconciliationDifferences() {
            return List.of();
        }

        @Override
        public ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord difference) {
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
        @Override
        public AuditEvent save(AuditEvent event) {
            return event;
        }
    }
}
