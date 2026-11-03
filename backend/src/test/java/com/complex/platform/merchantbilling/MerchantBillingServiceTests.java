package com.complex.platform.merchantbilling;

import com.complex.platform.support.InMemoryAuditEventRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.ArApAdjustmentRecord;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.ArApItemSourceRecord;
import com.complex.platform.finance.DailyCloseSummaryRecord;
import com.complex.platform.finance.ArApPaymentRecord;
import com.complex.platform.finance.DailyCloseRecord;
import com.complex.platform.finance.FinanceRefundRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.finance.OfflinePaymentConfirmationRecord;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.ReconciliationBatchRecord;
import com.complex.platform.finance.ReconciliationDifferenceRecord;
import com.complex.platform.merchantbilling.dto.MerchantBillConfirmRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillCreateRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillDisputeRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillDunningRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillLineRequest;
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

class MerchantBillingServiceTests {

    private InMemoryMerchantBillingRepository merchantBillingRepository;
    private InMemoryFinanceRepository financeRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private MerchantBillingService service;

    @BeforeEach
    void setUp() {
        merchantBillingRepository = new InMemoryMerchantBillingRepository();
        financeRepository = new InMemoryFinanceRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new MerchantBillingService(
                merchantBillingRepository,
                financeRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                noScopeAuth(),
                null,
                null
        );
    }

    @Test
    void createBillReferencesReceivableArApItemWithoutChangingFinanceLedger() {
        financeRepository.saveArApItem(arApItem("arap_merchant_1", "merchant_1", "receivable", 168800L));

        MerchantBillRecord created = service.createBill(createRequest("bill-create", "arap_merchant_1"));

        assertThat(created)
                .extracting(
                        MerchantBillRecord::merchantId,
                        MerchantBillRecord::arApItemId,
                        MerchantBillRecord::totalAmountCent,
                        MerchantBillRecord::currency,
                        MerchantBillRecord::status
                )
                .containsExactly("merchant_1", "arap_merchant_1", 168800L, "CNY", "issued");
        assertThat(created.lines())
                .extracting(MerchantBillLineRecord::feeType, MerchantBillLineRecord::amountCent)
                .containsExactly(tuple("rent", 128800L), tuple("cam", 40000L));
        assertThat(financeRepository.findArApItemById("arap_merchant_1"))
                .get()
                .extracting(ArApItemRecord::paidAmountCent, ArApItemRecord::unpaidAmountCent, ArApItemRecord::status)
                .containsExactly(0L, 168800L, "pending_collection");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("merchant.billing.create");
    }

    @Test
    void duplicateCreateIdempotencyKeyDoesNotCreateDuplicateBill() {
        financeRepository.saveArApItem(arApItem("arap_merchant_1", "merchant_1", "receivable", 168800L));
        MerchantBillCreateRequest request = createRequest("bill-idempotent", "arap_merchant_1");

        MerchantBillRecord first = service.createBill(request);
        MerchantBillRecord second = service.createBill(request);

        assertThat(second).isEqualTo(first);
        assertThat(merchantBillingRepository.billSaveCount).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "merchant.billing.create".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void createBillRejectsPayableArApItemBecauseMerchantPortalOnlyShowsReceivables() {
        financeRepository.saveArApItem(arApItem("arap_payable", "merchant_1", "payable", 168800L));

        assertThatThrownBy(() -> service.createBill(createRequest("bill-payable", "arap_payable")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("merchant.billing.create_failed");
    }

    @Test
    void confirmBillChangesStatusAndKeepsLinesVisible() {
        financeRepository.saveArApItem(arApItem("arap_merchant_1", "merchant_1", "receivable", 168800L));
        MerchantBillRecord bill = service.createBill(createRequest("bill-confirm-create", "arap_merchant_1"));

        MerchantBillRecord confirmed = service.confirmBill(bill.id(), new MerchantBillConfirmRequest(
                "bill-confirm",
                "merchant confirmed",
                "staff_finance"
        ));

        assertThat(confirmed.status()).isEqualTo("confirmed");
        assertThat(confirmed.lines()).hasSize(2);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("merchant.billing.confirm");
    }

    @Test
    void submitDisputeChangesBillStatusAndStoresEvidence() {
        financeRepository.saveArApItem(arApItem("arap_merchant_1", "merchant_1", "receivable", 168800L));
        MerchantBillRecord bill = service.createBill(createRequest("bill-dispute-create", "arap_merchant_1"));

        MerchantBillDisputeRecord dispute = service.submitDispute(bill.id(), new MerchantBillDisputeRequest(
                "bill-dispute",
                "energy amount mismatch",
                List.of("att_meter_photo"),
                "energy_amount_mismatch",
                "Energy reading does not match meter photo",
                "staff_finance"
        ));

        assertThat(dispute)
                .extracting(
                        MerchantBillDisputeRecord::billId,
                        MerchantBillDisputeRecord::merchantId,
                        MerchantBillDisputeRecord::reasonCode,
                        MerchantBillDisputeRecord::status
                )
                .containsExactly(bill.id(), "merchant_1", "energy_amount_mismatch", "submitted");
        assertThat(dispute.evidenceAttachmentIds()).containsExactly("att_meter_photo");
        assertThat(merchantBillingRepository.findBillById(bill.id()))
                .get()
                .extracting(MerchantBillRecord::status)
                .isEqualTo("disputed");
    }

    @Test
    void recordDunningKeepsAuditableChannelAndOperator() {
        financeRepository.saveArApItem(arApItem("arap_merchant_1", "merchant_1", "receivable", 168800L));
        MerchantBillRecord bill = service.createBill(createRequest("bill-dunning-create", "arap_merchant_1"));

        MerchantBillDunningRecord record = service.recordDunning(bill.id(), new MerchantBillDunningRequest(
                "bill-dunning",
                "first reminder",
                "wecom",
                "rent_overdue_first_notice",
                "sent",
                Instant.parse("2026-06-16T02:00:00Z"),
                "staff_finance"
        ));

        assertThat(record)
                .extracting(
                        MerchantBillDunningRecord::billId,
                        MerchantBillDunningRecord::merchantId,
                        MerchantBillDunningRecord::dunningChannel,
                        MerchantBillDunningRecord::messageTemplate,
                        MerchantBillDunningRecord::status,
                        MerchantBillDunningRecord::operatorUserId
                )
                .containsExactly(bill.id(), "merchant_1", "wecom", "rent_overdue_first_notice", "sent", "staff_finance");
    }

    @Test
    void confirmBillRejectsCrossMerchantScope() {
        financeRepository.saveArApItem(arApItem("arap_merchant_1", "merchant_1", "receivable", 168800L));
        MerchantBillRecord bill = service.createBill(createRequest("bill-xmerchant-confirm", "arap_merchant_1"));

        MerchantBillingService otherMerchant = serviceWithAuth(scopedAuth("merchant_2"));
        assertThatThrownBy(() -> otherMerchant.confirmBill(bill.id(), new MerchantBillConfirmRequest(
                "bill-xmerchant-confirm-act", "confirm", "staff_finance")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
        assertThat(merchantBillingRepository.findBillById(bill.id()))
                .get().extracting(MerchantBillRecord::status).isEqualTo("issued");
    }

    @Test
    void submitDisputeRejectsCrossMerchantScope() {
        financeRepository.saveArApItem(arApItem("arap_merchant_1", "merchant_1", "receivable", 168800L));
        MerchantBillRecord bill = service.createBill(createRequest("bill-xmerchant-dispute", "arap_merchant_1"));

        MerchantBillingService otherMerchant = serviceWithAuth(scopedAuth("merchant_2"));
        assertThatThrownBy(() -> otherMerchant.submitDispute(bill.id(), new MerchantBillDisputeRequest(
                "bill-xmerchant-dispute-act", "x", List.of(), "energy_amount_mismatch", "x", "staff_finance")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void recordDunningRejectsCrossMerchantScope() {
        financeRepository.saveArApItem(arApItem("arap_merchant_1", "merchant_1", "receivable", 168800L));
        MerchantBillRecord bill = service.createBill(createRequest("bill-xmerchant-dunning", "arap_merchant_1"));

        MerchantBillingService otherMerchant = serviceWithAuth(scopedAuth("merchant_2"));
        assertThatThrownBy(() -> otherMerchant.recordDunning(bill.id(), new MerchantBillDunningRequest(
                "bill-xmerchant-dunning-act", "x", "wecom", "rent_overdue_first_notice", "sent", null, "staff_finance")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void listBillsReturnsPageAndReadAudit() {
        financeRepository.saveArApItem(arApItem("arap_merchant_1", "merchant_1", "receivable", 168800L));
        service.createBill(createRequest("bill-list", "arap_merchant_1"));

        PageResponse<MerchantBillRecord> page = service.listBills();

        assertThat(page.items()).hasSize(1);
        assertThat(page.total()).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("merchant_billing.bill.viewed");
    }

    // ── P1 Bug 1: 租户越权读 ──────────────────────────────────────────────────
    @Test
    void listBillsMerchantScopeFiltersToOwnBills() {
        financeRepository.saveArApItem(arApItem("arap_m1", "merchant_1", "receivable", 168800L));
        financeRepository.saveArApItem(arApItem("arap_m2", "merchant_2", "receivable", 168800L));
        MerchantBillingService adminService = serviceWithAuth(noScopeAuth());
        adminService.createBill(createRequestForMerchant("bill-m1-iso", "merchant_1", "arap_m1"));
        adminService.createBill(createRequestForMerchant("bill-m2-iso", "merchant_2", "arap_m2"));

        PageResponse<MerchantBillRecord> pageM1 = serviceWithAuth(scopedAuth("merchant_1")).listBills();

        assertThat(pageM1.items()).hasSize(1);
        assertThat(pageM1.items().get(0).merchantId()).isEqualTo("merchant_1");
    }

    // ── P1 Bug 2: 幂等键命名空间跨商户碰撞 ─────────────────────────────────
    @Test
    void createBillIdempotencyKeyIsIsolatedPerMerchant() {
        InMemoryIdempotencyRepository sharedRepo = new InMemoryIdempotencyRepository();
        financeRepository.saveArApItem(arApItem("arap_m1b", "merchant_1", "receivable", 168800L));
        financeRepository.saveArApItem(arApItem("arap_m2b", "merchant_2", "receivable", 168800L));
        MerchantBillingService svcM1 = serviceWithSharedIdempotency(sharedRepo, noScopeAuth());
        MerchantBillingService svcM2 = serviceWithSharedIdempotency(sharedRepo, noScopeAuth());

        MerchantBillRecord bill1 = svcM1.createBill(createRequestForMerchant("shared-key", "merchant_1", "arap_m1b"));
        MerchantBillRecord bill2 = svcM2.createBill(createRequestForMerchant("shared-key", "merchant_2", "arap_m2b"));

        assertThat(bill1.merchantId()).isEqualTo("merchant_1");
        assertThat(bill2.merchantId()).isEqualTo("merchant_2");
        assertThat(merchantBillingRepository.billSaveCount).isEqualTo(2);
    }

    // ── P1 Bug 3: 明细 ID 碰撞（同 feeType 两行静默覆盖） ──────────────────
    @Test
    void createBillWithDuplicateFeeTypeLinesIsRejected() {
        financeRepository.saveArApItem(arApItem("arap_dup", "merchant_1", "receivable", 168800L));

        assertThatThrownBy(() -> service.createBill(new MerchantBillCreateRequest(
                "bill-dup-fee",
                "test",
                "merchant_1",
                "arap_dup",
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                LocalDate.parse("2026-07-05"),
                168800L,
                "manual",
                null,
                List.of(
                        new MerchantBillLineRequest("rent", "June rent part 1", 100000L),
                        new MerchantBillLineRequest("rent", "June rent part 2", 68800L)
                )
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    private MerchantBillCreateRequest createRequest(String idempotencyKey, String arApItemId) {
        return new MerchantBillCreateRequest(
                idempotencyKey,
                "issue bill",
                "merchant_1",
                arApItemId,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                LocalDate.parse("2026-07-05"),
                168800L,
                "manual",
                "billing_batch_202606",
                List.of(
                        new MerchantBillLineRequest("rent", "June rent", 128800L),
                        new MerchantBillLineRequest("cam", "June CAM", 40000L)
                )
        );
    }

    private MerchantBillCreateRequest createRequestForMerchant(String idempotencyKey, String merchantId, String arApItemId) {
        return new MerchantBillCreateRequest(
                idempotencyKey,
                "issue bill",
                merchantId,
                arApItemId,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                LocalDate.parse("2026-07-05"),
                168800L,
                "manual",
                "billing_batch_202606",
                List.of(
                        new MerchantBillLineRequest("rent", "June rent", 128800L),
                        new MerchantBillLineRequest("cam", "June CAM", 40000L)
                )
        );
    }

    private ArApItemRecord arApItem(String id, String merchantId, String direction, long amountCent) {
        return new ArApItemRecord(
                id,
                "ARAP-" + id,
                direction,
                merchantId,
                "manual",
                "src_" + id,
                amountCent,
                0L,
                amountCent,
                "CNY",
                LocalDate.parse("2026-07-05"),
                "2026-06",
                "receivable".equals(direction) ? "pending_collection" : "pending_payment",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z"),
                merchantId
        );
    }

    private static final class InMemoryMerchantBillingRepository implements MerchantBillingRepository {
        private final Map<String, MerchantBillRecord> bills = new LinkedHashMap<>();
        private final Map<String, MerchantBillDisputeRecord> disputes = new LinkedHashMap<>();
        private final Map<String, MerchantBillDunningRecord> dunningRecords = new LinkedHashMap<>();
        private int billSaveCount;

        @Override
        public List<MerchantBillRecord> findAllBills() {
            return new ArrayList<>(bills.values());
        }

        @Override
        public List<MerchantBillRecord> findBillsByMerchantId(String merchantId) {
            return bills.values().stream()
                    .filter(bill -> merchantId.equals(bill.merchantId()))
                    .toList();
        }

        @Override
        public Optional<MerchantBillRecord> findBillById(String id) {
            return Optional.ofNullable(bills.get(id));
        }

        @Override
        public MerchantBillRecord saveBill(MerchantBillRecord record) {
            billSaveCount += 1;
            bills.put(record.id(), record);
            return record;
        }

        @Override
        public MerchantBillDisputeRecord saveDispute(MerchantBillDisputeRecord record) {
            disputes.put(record.id(), record);
            return record;
        }

        @Override
        public List<MerchantBillDisputeRecord> findDisputesByBillId(String billId) {
            return disputes.values().stream()
                    .filter(record -> record.billId().equals(billId))
                    .toList();
        }

        @Override
        public MerchantBillDunningRecord saveDunningRecord(MerchantBillDunningRecord record) {
            dunningRecords.put(record.id(), record);
            return record;
        }

        @Override
        public List<MerchantBillDunningRecord> findDunningRecordsByBillId(String billId) {
            return dunningRecords.values().stream()
                    .filter(record -> record.billId().equals(billId))
                    .toList();
        }
    }

    private static final class InMemoryFinanceRepository implements FinanceRepository {
        private final Map<String, ArApItemRecord> items = new LinkedHashMap<>();

        @Override
        public ArApItemRecord saveArApItem(ArApItemRecord item) {
            items.put(item.id(), item);
            return item;
        }

        @Override
        public ArApItemSourceRecord saveArApItemSource(ArApItemSourceRecord source) {
            return source;
        }

        @Override
        public List<ArApItemRecord> listArApItems() {
            return new ArrayList<>(items.values());
        }

        @Override
        public List<ArApItemRecord> listArApItemsByMerchant(String merchantId) {
            return items.values().stream()
                    .filter(item -> merchantId.equals(item.merchantId()))
                    .toList();
        }

        @Override
        public Optional<ArApItemRecord> findArApItemById(String id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public Optional<ArApItemRecord> findArApItemByIdForUpdate(String id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public ArApItemRecord updateArApItemLedger(ArApItemRecord item) {
            items.put(item.id(), item);
            return item;
        }

        @Override
        public ArApPaymentRecord saveArApPayment(ArApPaymentRecord payment) {
            return payment;
        }

        @Override
        public Optional<ArApPaymentRecord> findArApPaymentById(String id) {
            return Optional.empty();
        }

        @Override
        public long sumRefundedAmountByTargetId(String targetId) {
            return 0L;
        }

        @Override
        public DailyCloseRecord saveDailyClose(DailyCloseRecord dailyClose) {
            return dailyClose;
        }

        @Override
        public boolean dailyCloseExists(java.time.LocalDate businessDate, String scopeType, String scopeId) {
            return false;
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

        @Override
        public java.util.Optional<ReconciliationDifferenceRecord> findReconciliationDifferenceById(String id) {
            return java.util.Optional.empty();
        }

        @Override
        public ReconciliationDifferenceRecord closeReconciliationDifference(
                String id, String closeReason, String closedBy, java.time.Instant closedAt) {
            return difference(id);
        }

        private ReconciliationDifferenceRecord difference(String id) {
            return new ReconciliationDifferenceRecord(id, null, null, null, 0L, null, null, "system", "closed", null);
        }

        // ── R5 T7：日结锁定 + 补录审批 + 多源汇总（merchantbilling 域测试无需，提供空实现以满足接口）──
        @Override
        public Optional<DailyCloseRecord> findDailyCloseById(String id) {
            return Optional.empty();
        }

        @Override
        public DailyCloseSummaryRecord saveDailyCloseSummary(DailyCloseSummaryRecord summary) {
            return summary;
        }

        @Override
        public List<DailyCloseSummaryRecord> listDailyCloseSummaries(String dailyCloseId) {
            return List.of();
        }

        @Override
        public int lockArApItemsForDailyClose(String dailyCloseId, LocalDate businessDate, String merchantId) {
            return 0;
        }

        @Override
        public Optional<String> findArApItemLockedDayCloseId(String arApItemId) {
            return Optional.empty();
        }

        @Override
        public boolean lockedDailyCloseExistsForDate(LocalDate businessDate, String merchantId) {
            return false;
        }

        @Override
        public DailyCloseSourceTotals aggregateOfficialTicket(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return DailyCloseSourceTotals.empty();
        }

        @Override
        public DailyCloseSourceTotals aggregateChannelTicket(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return DailyCloseSourceTotals.empty();
        }

        @Override
        public DailyCloseSourceTotals aggregateBar(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return DailyCloseSourceTotals.empty();
        }

        @Override
        public DailyCloseSourceTotals aggregateAmusement(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return DailyCloseSourceTotals.empty();
        }

        @Override
        public DailyCloseSourceTotals aggregateSupplyCost(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return DailyCloseSourceTotals.empty();
        }

        @Override
        public DailyCloseSourceTotals aggregateThirdpartyReflow(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return DailyCloseSourceTotals.empty();
        }

        @Override
        public ArApAdjustmentRecord saveArApAdjustment(ArApAdjustmentRecord adjustment) {
            return adjustment;
        }

        @Override
        public Optional<ArApAdjustmentRecord> findArApAdjustmentByIdForUpdate(String id) {
            return Optional.empty();
        }

        @Override
        public ArApAdjustmentRecord updateArApAdjustment(ArApAdjustmentRecord adjustment) {
            return adjustment;
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(key(operationId, idempotencyKey)));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            return records.putIfAbsent(key(record.operationId(), record.idempotencyKey()), record) == null;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(key(record.operationId(), record.idempotencyKey()), record);
            return record;
        }

        private String key(String operationId, String idempotencyKey) {
            return operationId + ":" + idempotencyKey;
        }
    }

    private AuthorizationService noScopeAuth() {
        return new AuthorizationService(null) {
            @Override
            public Optional<String> currentMerchantScope() {
                return Optional.empty();
            }
        };
    }

    private AuthorizationService scopedAuth(String merchantId) {
        return new AuthorizationService(null) {
            @Override
            public Optional<String> currentMerchantScope() {
                return Optional.of(merchantId);
            }
        };
    }

    private MerchantBillingService serviceWithAuth(AuthorizationService auth) {
        return new MerchantBillingService(
                merchantBillingRepository,
                financeRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                auth,
                null,
                null
        );
    }

    private MerchantBillingService serviceWithSharedIdempotency(
            InMemoryIdempotencyRepository sharedRepo, AuthorizationService auth) {
        return new MerchantBillingService(
                merchantBillingRepository,
                financeRepository,
                new IdempotencyService(sharedRepo, new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                auth,
                null,
                null
        );
    }
}
