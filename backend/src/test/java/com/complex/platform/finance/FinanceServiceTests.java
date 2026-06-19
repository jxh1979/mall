package com.complex.platform.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.complex.platform.access.AccessActor;
import com.complex.platform.support.TestSecurityActors;

class FinanceServiceTests {

    private InMemoryFinanceRepository financeRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private AuthorizationService authorizationService;
    private FinanceService service;

    @BeforeEach
    void setUp() {
        financeRepository = new InMemoryFinanceRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        // Default: platform admin — no merchant filter, all writes permitted
        authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                auditEventService
        );
        service = new FinanceService(
                financeRepository,
                operations,
                auditEventService,
                authorizationService,
                new com.complex.platform.common.BusinessDayResolver(4)
        );
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    // ─── existing functional tests (preserved) ────────────────────────────────

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
        // Provide a real ar_ap_payment for the refund to reference (P1 fix requires existence check)
        ArApItemRecord item = service.createArApItem(itemRequest("arap-for-refund-audit", "receivable", 128800L));
        ArApPaymentResponse paymentResp = service.registerArApPayment(item.id(), paymentRequest("pay-for-refund-audit", 128800L));
        String paymentId = paymentResp.payment().id();

        service.createDailyClose(new DailyCloseRequest(
                "daily-close-audit",
                "lock",
                List.of(),
                LocalDate.parse("2026-06-14"),
                "project",
                "project_audit_1",
                false
        ));
        service.createPaymentIntent(new PaymentIntentRequest(
                "payment-intent-audit",
                "create",
                List.of(),
                "ticket_order",
                "order_audit_1",
                128800L,
                "CNY",
                "mock"
        ));
        service.confirmOfflinePayment("payment_intent_1", new OfflinePaymentConfirmRequest(
                "offline-confirm-audit",
                "confirm",
                List.of("attachment_1"),
                "payment_intent_1",
                128800L,
                "cash",
                Instant.parse("2026-06-14T00:05:00Z")
        ));
        service.createRefund(new RefundRequest(
                "refund-create-audit",
                "refund",
                List.of(),
                paymentId,
                18800L,
                "guest request",
                "original_channel"
        ));

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "finance_daily_close.locked",
                        "payment.intent_created",
                        "finance_offline_payment.confirmed",
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

    @Test
    void reconciliationReadsAreForbiddenForMerchantScopedCallers() {
        financeRepository.saveReconciliationBatch(new ReconciliationBatchRecord(
                "recon_batch_iso", "RB202606180001", "project", "project_1",
                LocalDate.parse("2026-06-18"), "created", Instant.parse("2026-06-18T00:00:00Z")));

        // Reconciliation has no merchant dimension; a merchant-scoped caller must not read cross-merchant data.
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_a"));

        assertThatThrownBy(() -> service.listReconciliationBatches())
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("RECONCILIATION_PLATFORM_ONLY");
        assertThatThrownBy(() -> service.listReconciliationDifferences())
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("RECONCILIATION_PLATFORM_ONLY");
    }

    // ─── P0 fix tests ─────────────────────────────────────────────────────────

    @Test
    void sameIdempotencyKeyOnDifferentItemsProducesDifferentPaymentIds() {
        // Before fix: both calls would produce id = "arap_pay_<slug(key)>", causing
        // the second to overwrite the first payment's ar_ap_item_id in the DB.
        ArApItemRecord item1 = service.createArApItem(itemRequest("arap-coll-1", "receivable", 50000L));
        ArApItemRecord item2 = service.createArApItem(itemRequest("arap-coll-2", "receivable", 50000L));

        ArApPaymentResponse r1 = service.registerArApPayment(item1.id(), paymentRequest("same-key", 50000L));
        // Use a different idempotency key so the idempotency guard doesn't block it.
        // The critical invariant: same real-money key conceptually but scoped to different items.
        ArApPaymentResponse r2 = service.registerArApPayment(item2.id(), paymentRequest("same-key-item2", 50000L));

        assertThat(r1.payment().id()).isNotEqualTo(r2.payment().id());
        assertThat(r1.payment().arApItemId()).isEqualTo(item1.id());
        assertThat(r2.payment().arApItemId()).isEqualTo(item2.id());
    }

    @Test
    void paymentIdIncludesArApItemIdToAvoidCrossItemCollision() {
        ArApItemRecord item1 = service.createArApItem(itemRequest("arap-id-a", "receivable", 10000L));
        ArApItemRecord item2 = service.createArApItem(itemRequest("arap-id-b", "receivable", 10000L));

        ArApPaymentResponse r1 = service.registerArApPayment(item1.id(), paymentRequest("shared-key-x", 10000L));
        ArApPaymentResponse r2 = service.registerArApPayment(item2.id(), paymentRequest("shared-key-x", 10000L));

        // P0 fix: payment IDs incorporate the item ID so two items with the same idempotency
        // key produce distinct payment records rather than colliding on the same primary key.
        assertThat(r1.payment().id()).isNotEqualTo(r2.payment().id());
    }

    // ─── P1 refund validation tests ───────────────────────────────────────────

    @Test
    void refundTargetNotFoundReturnsNotFound() {
        assertThatThrownBy(() -> service.createRefund(new RefundRequest(
                "refund-no-target",
                "refund",
                List.of(),
                "nonexistent_payment_id",
                1000L,
                "test",
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("PAYMENT_NOT_FOUND");
    }

    @Test
    void refundAmountExceedingOriginalPaymentIsRejected() {
        ArApItemRecord item = service.createArApItem(itemRequest("arap-refund-limit", "receivable", 50000L));
        ArApPaymentResponse payment = service.registerArApPayment(item.id(), paymentRequest("pay-refund-limit", 50000L));

        assertThatThrownBy(() -> service.createRefund(new RefundRequest(
                "refund-too-much",
                "refund",
                List.of(),
                payment.payment().id(),
                50001L,
                "overage",
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void cumulativeRefundsCannotExceedOriginalPaymentAmount() {
        ArApItemRecord item = service.createArApItem(itemRequest("arap-cumulative-refund", "receivable", 10000L));
        ArApPaymentResponse payment = service.registerArApPayment(item.id(), paymentRequest("pay-cumulative", 10000L));
        String paymentId = payment.payment().id();

        service.createRefund(new RefundRequest("refund-part1", "refund", List.of(), paymentId, 6000L, "partial", null));

        assertThatThrownBy(() -> service.createRefund(new RefundRequest(
                "refund-part2",
                "refund",
                List.of(),
                paymentId,
                5000L,
                "over cumulative",
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void validRefundWithinOriginalPaymentAmountSucceeds() {
        ArApItemRecord item = service.createArApItem(itemRequest("arap-valid-refund", "receivable", 30000L));
        ArApPaymentResponse payment = service.registerArApPayment(item.id(), paymentRequest("pay-valid", 30000L));

        FinanceRefundRecord refund = service.createRefund(new RefundRequest(
                "refund-valid",
                "refund",
                List.of(),
                payment.payment().id(),
                15000L,
                "partial refund",
                null
        ));

        assertThat(refund.refundAmountCent()).isEqualTo(15000L);
        assertThat(refund.status()).isEqualTo("requested");
    }

    // ─── P1 DailyClose uniqueness tests ───────────────────────────────────────

    @Test
    void duplicateDailyCloseForSameScopeAndDateIsRejected() {
        service.createDailyClose(new DailyCloseRequest(
                "close-first",
                "lock",
                List.of(),
                LocalDate.parse("2026-06-15"),
                "project",
                "proj_dup_test",
                false
        ));

        assertThatThrownBy(() -> service.createDailyClose(new DailyCloseRequest(
                "close-second-different-key",
                "lock",
                List.of(),
                LocalDate.parse("2026-06-15"),
                "project",
                "proj_dup_test",
                false
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DAILY_CLOSE_EXISTS");
    }

    @Test
    void dailyCloseIdempotentRetryWithSameKeySucceeds() {
        DailyCloseRequest request = new DailyCloseRequest(
                "close-idempotent",
                "lock",
                List.of(),
                LocalDate.parse("2026-06-16"),
                "bar",
                "bar_idem_test",
                false
        );

        DailyCloseRecord first = service.createDailyClose(request);
        DailyCloseRecord second = service.createDailyClose(request);

        assertThat(second).isEqualTo(first);
    }

    // ─── P1 merchant scope isolation tests ───────────────────────────────────

    @Test
    void merchantUserSeesOnlyTheirOwnArApItems() {
        // Set up merchant A scope to create item A
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_a"));
        ArApItemRecord itemA = service.createArApItem(itemRequest("arap-merchant-a", "receivable", 10000L));
        assertThat(itemA.merchantId()).isEqualTo("merchant_a");

        // Set up merchant B scope to create item B
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_b"));
        service.createArApItem(itemRequest("arap-merchant-b", "receivable", 20000L));

        // Merchant A lists: should only see their item
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_a"));
        PageResponse<ArApItemRecord> result = service.listArApItems();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).merchantId()).isEqualTo("merchant_a");
    }

    @Test
    void platformAdminSeesAllMerchantArApItems() {
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_a"));
        service.createArApItem(itemRequest("arap-pa-a", "receivable", 10000L));

        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_b"));
        service.createArApItem(itemRequest("arap-pa-b", "receivable", 20000L));

        // Platform admin: empty scope = see all
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        PageResponse<ArApItemRecord> result = service.listArApItems();
        assertThat(result.items()).hasSize(2);
    }

    @Test
    void merchantCannotRegisterPaymentAgainstAnotherMerchantItem() {
        // Create item owned by merchant_a
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_a"));
        ArApItemRecord itemA = service.createArApItem(itemRequest("arap-cross-pay", "receivable", 50000L));

        // Merchant B tries to pay merchant A's item
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_b"));

        assertThatThrownBy(() ->
                service.registerArApPayment(itemA.id(), paymentRequest("pay-cross", 50000L))
        )
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void merchantCannotRefundAnotherMerchantPayment() {
        // Create item + payment for merchant_a
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_a"));
        ArApItemRecord itemA = service.createArApItem(itemRequest("arap-cross-refund", "receivable", 50000L));
        ArApPaymentResponse payment = service.registerArApPayment(itemA.id(), paymentRequest("pay-cross-ref", 50000L));

        // Merchant B tries to refund merchant A's payment
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_b"));

        assertThatThrownBy(() -> service.createRefund(new RefundRequest(
                "refund-cross",
                "refund",
                List.of(),
                payment.payment().id(),
                10000L,
                "cross merchant refund attempt",
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void platformAdminCanPayAndRefundAnyMerchantItem() {
        // Admin creates item (no merchant)
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        ArApItemRecord item = service.createArApItem(itemRequest("arap-admin-pay", "receivable", 50000L));
        ArApPaymentResponse payment = service.registerArApPayment(item.id(), paymentRequest("pay-admin", 50000L));

        FinanceRefundRecord refund = service.createRefund(new RefundRequest(
                "refund-admin",
                "refund",
                List.of(),
                payment.payment().id(),
                20000L,
                "admin refund",
                null
        ));

        assertThat(refund.refundAmountCent()).isEqualTo(20000L);
    }

    // ─── R5 T7：日结锁定 + 补录审批 + 多源汇总 ─────────────────────────────────

    @Test
    void lockDailyCloseAggregatesSixSourcesAndTagsSameDayItems() {
        // 六源聚合返回真实数（非种子），逐源写一行汇总。
        financeRepository.officialTicketTotals = new FinanceRepository.DailyCloseSourceTotals(500000L, 0L, 12);
        financeRepository.barTotals = new FinanceRepository.DailyCloseSourceTotals(300000L, 0L, 8);
        financeRepository.amusementTotals = new FinanceRepository.DailyCloseSourceTotals(0L, 0L, 40);
        financeRepository.supplyCostTotals = new FinanceRepository.DailyCloseSourceTotals(0L, 0L, 5);
        financeRepository.thirdpartyTotals = new FinanceRepository.DailyCloseSourceTotals(120000L, 0L, 6);

        // 同经营日(due_date=2026-06-30) 的台账应被打 locked_day_close_id。
        ArApItemRecord item = service.createArApItem(itemRequest("arap-to-lock", "receivable", 128800L));

        DailyCloseRecord close = service.createDailyClose(new DailyCloseRequest(
                "daily-close-t7",
                "lock",
                List.of(),
                LocalDate.parse("2026-06-30"),
                "project",
                "project_t7",
                false));

        PageResponse<DailyCloseSummaryRecord> summaries = service.listDailyCloseSummaries(close.id());
        assertThat(summaries.items()).extracting(DailyCloseSummaryRecord::sourceStream)
                .containsExactlyInAnyOrder(
                        "official_ticket", "channel_ticket", "bar", "amusement", "supply_cost", "thirdparty_reflow");
        assertThat(summaries.items())
                .filteredOn(s -> "official_ticket".equals(s.sourceStream()))
                .extracting(DailyCloseSummaryRecord::amountCent, DailyCloseSummaryRecord::orderCount)
                .containsExactly(tuple(500000L, 12));
        assertThat(financeRepository.findArApItemLockedDayCloseId(item.id())).contains(close.id());
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("finance.daily_close_summarized");
    }

    @Test
    void revenueNetDerivesFromTicketAndBarMinusRefundExcludingThirdparty() {
        // 财务确认口径：net = 自营售票 + 酒吧 − 退款；第三方回流单列展示，不计入 net。
        financeRepository.officialTicketTotals = new FinanceRepository.DailyCloseSourceTotals(500000L, 20000L, 12);
        financeRepository.barTotals = new FinanceRepository.DailyCloseSourceTotals(300000L, 0L, 8);
        financeRepository.thirdpartyTotals = new FinanceRepository.DailyCloseSourceTotals(120000L, 0L, 6);

        DailyCloseRecord close = service.createDailyClose(new DailyCloseRequest(
                "daily-close-net", "lock", List.of(),
                LocalDate.parse("2026-06-30"), "project", "project_net", false));

        List<DailyCloseSummaryRecord> rows = service.listDailyCloseSummaries(close.id()).items();
        // 第三方回流单列存在，且金额单列 120000，refund 不混入 net。
        assertThat(rows).filteredOn(s -> "thirdparty_reflow".equals(s.sourceStream()))
                .extracting(DailyCloseSummaryRecord::amountCent).containsExactly(120000L);

        // 退款落在 official_ticket 源的 refund_cent 列（按经营日冲减）。
        assertThat(rows).filteredOn(s -> "official_ticket".equals(s.sourceStream()))
                .extracting(DailyCloseSummaryRecord::refundCent).containsExactly(20000L);

        // net 派生：售票(500000) + 酒吧(300000) − 退款(20000) = 780000，剔除第三方 120000。
        long net = rows.stream()
                .filter(s -> "official_ticket".equals(s.sourceStream()) || "bar".equals(s.sourceStream()))
                .mapToLong(s -> s.amountCent() - s.refundCent())
                .sum();
        assertThat(net).isEqualTo(780000L);
        // 反证：若误把第三方计入 net，则会变成 900000。
        assertThat(net).isNotEqualTo(900000L);
    }

    @Test
    void lockedItemRejectsDirectPaymentWithDayCloseLocked() {
        ArApItemRecord item = service.createArApItem(itemRequest("arap-locked-pay", "receivable", 128800L));
        service.createDailyClose(new DailyCloseRequest(
                "daily-close-lock-pay", "lock", List.of(),
                LocalDate.parse("2026-06-30"), "project", "project_lp", false));

        assertThatThrownBy(() -> service.registerArApPayment(item.id(), paymentRequest("pay-blocked", 1000L)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DAY_CLOSE_LOCKED");
    }

    @Test
    void createItemOnLockedBusinessDateRejected() {
        service.createDailyClose(new DailyCloseRequest(
                "daily-close-lock-create", "lock", List.of(),
                LocalDate.parse("2026-06-30"), "project", "project_lc", false));

        assertThatThrownBy(() -> service.createArApItem(itemRequest("arap-after-lock", "receivable", 1000L)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DAY_CLOSE_LOCKED");
    }

    @Test
    void backfillApprovalAppliesAmountChangeAndRejectDoesNot() {
        stubActor("staff_finance_1");
        ArApItemRecord item = service.createArApItem(itemRequest("arap-backfill", "receivable", 100000L));
        service.createDailyClose(new DailyCloseRequest(
                "daily-close-backfill", "lock", List.of(),
                LocalDate.parse("2026-06-30"), "project", "project_bf", false));

        // 申请补录 +5000。
        ArApAdjustmentRecord requested = service.requestAdjustment(new com.complex.platform.finance.dto.ArApAdjustmentRequest(
                "adj-req-1", "backfill", List.of(), item.id(), 5000L, "late invoice"));
        assertThat(requested.approvalStatus()).isEqualTo("pending");

        // 审批通过：金额变动入账。
        ArApAdjustmentRecord approved = service.approveAdjustment(requested.id(),
                new com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest("adj-dec-1", "ok", List.of(), "approve"));
        assertThat(approved.approvalStatus()).isEqualTo("approved");
        assertThat(financeRepository.findArApItemById(item.id()).orElseThrow().amountCent()).isEqualTo(105000L);
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("finance.locked_backfill_requested", "finance.locked_backfill_approved");
    }

    @Test
    void backfillRejectDoesNotChangeLedger() {
        stubActor("staff_finance_2");
        ArApItemRecord item = service.createArApItem(itemRequest("arap-backfill-rej", "receivable", 100000L));
        service.createDailyClose(new DailyCloseRequest(
                "daily-close-backfill-rej", "lock", List.of(),
                LocalDate.parse("2026-06-30"), "project", "project_bfr", false));

        ArApAdjustmentRecord requested = service.requestAdjustment(new com.complex.platform.finance.dto.ArApAdjustmentRequest(
                "adj-req-2", "backfill", List.of(), item.id(), 7000L, "disputed"));
        ArApAdjustmentRecord rejected = service.approveAdjustment(requested.id(),
                new com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest("adj-dec-2", "no", List.of(), "reject"));

        assertThat(rejected.approvalStatus()).isEqualTo("rejected");
        assertThat(financeRepository.findArApItemById(item.id()).orElseThrow().amountCent()).isEqualTo(100000L);
    }

    @Test
    void requestAdjustmentRejectedWhenItemNotLocked() {
        stubActor("staff_finance_3");
        ArApItemRecord item = service.createArApItem(itemRequest("arap-not-locked", "receivable", 100000L));

        assertThatThrownBy(() -> service.requestAdjustment(new com.complex.platform.finance.dto.ArApAdjustmentRequest(
                "adj-req-3", "backfill", List.of(), item.id(), 1000L, "no lock")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("AR_AP_ITEM_NOT_LOCKED");
    }

    private void stubActor(String id) {
        when(authorizationService.currentActor())
                .thenReturn(new AccessActor(id, id, "staff", null, null, "active"));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    // ─── Payment gateway switch routing (V093) ────────────────────────────────

    private FinanceService financeWithGatewaySwitch(boolean enabled) {
        com.complex.platform.terminal.TerminalDeviceRepository terminalRepo =
                mock(com.complex.platform.terminal.TerminalDeviceRepository.class);
        when(terminalRepo.resolveEffectiveSwitch(
                org.mockito.ArgumentMatchers.eq("*"),
                org.mockito.ArgumentMatchers.eq(com.complex.platform.finance.gateway.PaymentGatewayRouter.CAPABILITY),
                org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Optional.of(new com.complex.platform.terminal.TerminalCapabilitySwitchRecord(
                        "tcs_payment_gateway_off", "*",
                        com.complex.platform.finance.gateway.PaymentGatewayRouter.CAPABILITY,
                        "global", "global", enabled, "test", Instant.now(), Instant.now())));
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        AuditedOperationExecutor ops = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()), auditEventService);
        return new FinanceService(financeRepository, ops, auditEventService, authorizationService,
                new com.complex.platform.common.BusinessDayResolver(4),
                new com.complex.platform.finance.gateway.PaymentGatewayRouter(terminalRepo));
    }

    @Test
    void paymentGatewayOff_createPaymentIntentRecordsReservedNoExternalCall() {
        FinanceService svc = financeWithGatewaySwitch(false);
        PaymentIntentRecord intent = svc.createPaymentIntent(new PaymentIntentRequest(
                "pi-gw-off", "create", List.of(), "ticket_order", "order_gw_off", 5000L, "CNY", "wechat"));
        assertThat(intent.gatewayStatus()).isEqualTo("RESERVED");
        assertThat(intent.gatewayProvider()).isNull();
    }

    @Test
    void paymentGatewayOn_createPaymentIntentRoutesToPortReturnsNotConfigured() {
        FinanceService svc = financeWithGatewaySwitch(true);
        PaymentIntentRecord intent = svc.createPaymentIntent(new PaymentIntentRequest(
                "pi-gw-on", "create", List.of(), "ticket_order", "order_gw_on", 5000L, "CNY", "wechat"));
        assertThat(intent.gatewayStatus()).isEqualTo("NOT_CONFIGURED");
        assertThat(intent.gatewayProvider()).isEqualTo("reserved");
    }

    @Test
    void paymentGatewayOff_refundUsesInternalManualChannel() {
        FinanceService svc = financeWithGatewaySwitch(false);
        ArApItemRecord item = svc.createArApItem(itemRequest("arap-gw-refund-off", "receivable", 30000L));
        ArApPaymentResponse payment = svc.registerArApPayment(item.id(), paymentRequest("pay-gw-off", 30000L));
        FinanceRefundRecord refund = svc.createRefund(new RefundRequest(
                "refund-gw-off", "refund", List.of(), payment.payment().id(), 10000L, "refund", null));
        assertThat(refund.refundChannel()).isEqualTo("manual");
        assertThat(refund.status()).isEqualTo("requested");
    }

    @Test
    void paymentGatewayOn_refundDoesNotCrashAndStillRecorded() {
        FinanceService svc = financeWithGatewaySwitch(true);
        ArApItemRecord item = svc.createArApItem(itemRequest("arap-gw-refund-on", "receivable", 30000L));
        ArApPaymentResponse payment = svc.registerArApPayment(item.id(), paymentRequest("pay-gw-on", 30000L));
        FinanceRefundRecord refund = svc.createRefund(new RefundRequest(
                "refund-gw-on", "refund", List.of(), payment.payment().id(), 10000L, "refund", null));
        assertThat(refund.status()).isEqualTo("requested");
        // 占位 NOT_CONFIGURED 仍属"未真正经渠道结算"→回退内部手工打款渠道（manual），不崩、不阻塞。
        assertThat(refund.refundChannel()).isEqualTo("manual");
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

    // ─── in-memory test doubles ───────────────────────────────────────────────

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
            // In-memory: same as findArApItemById (no real locking needed in unit tests)
            return Optional.ofNullable(arApItems.get(id));
        }

        @Override
        public ArApItemRecord updateArApItemLedger(ArApItemRecord item) {
            arApItems.put(item.id(), item);
            return item;
        }

        @Override
        public ArApPaymentRecord saveArApPayment(ArApPaymentRecord payment) {
            // Mirror DO NOTHING semantics: only insert if the ID is genuinely new.
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
        public boolean dailyCloseExists(java.time.LocalDate businessDate, String scopeType, String scopeId) {
            return dailyCloses.values().stream().anyMatch(dc ->
                    businessDate.equals(dc.businessDate())
                            && scopeType.equals(dc.scopeType())
                            && scopeId.equals(dc.scopeId()));
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

        // ── R5 T7：日结锁定 + 补录审批 + 多源汇总 ──────────────────────────

        private final Map<String, DailyCloseSummaryRecord> summaries = new LinkedHashMap<>();
        private final Map<String, ArApAdjustmentRecord> adjustments = new LinkedHashMap<>();
        private final Map<String, String> itemLocks = new LinkedHashMap<>();
        FinanceRepository.DailyCloseSourceTotals officialTicketTotals = FinanceRepository.DailyCloseSourceTotals.empty();
        FinanceRepository.DailyCloseSourceTotals barTotals = FinanceRepository.DailyCloseSourceTotals.empty();
        FinanceRepository.DailyCloseSourceTotals amusementTotals = FinanceRepository.DailyCloseSourceTotals.empty();
        FinanceRepository.DailyCloseSourceTotals supplyCostTotals = FinanceRepository.DailyCloseSourceTotals.empty();
        FinanceRepository.DailyCloseSourceTotals thirdpartyTotals = FinanceRepository.DailyCloseSourceTotals.empty();
        FinanceRepository.DailyCloseSourceTotals channelTicketTotals = FinanceRepository.DailyCloseSourceTotals.empty();

        @Override
        public Optional<DailyCloseRecord> findDailyCloseById(String id) {
            return Optional.ofNullable(dailyCloses.get(id));
        }

        @Override
        public DailyCloseSummaryRecord saveDailyCloseSummary(DailyCloseSummaryRecord summary) {
            summaries.put(summary.dailyCloseId() + "|" + summary.sourceStream(), summary);
            return summary;
        }

        @Override
        public List<DailyCloseSummaryRecord> listDailyCloseSummaries(String dailyCloseId) {
            return summaries.values().stream()
                    .filter(s -> dailyCloseId.equals(s.dailyCloseId()))
                    .toList();
        }

        @Override
        public int lockArApItemsForDailyClose(String dailyCloseId, LocalDate businessDate, String merchantId) {
            int locked = 0;
            for (ArApItemRecord item : arApItems.values()) {
                boolean dateMatch = businessDate.equals(item.dueDate());
                boolean merchantMatch = merchantId == null || merchantId.equals(item.merchantId());
                if (dateMatch && merchantMatch && !itemLocks.containsKey(item.id())) {
                    itemLocks.put(item.id(), dailyCloseId);
                    locked += 1;
                }
            }
            return locked;
        }

        @Override
        public Optional<String> findArApItemLockedDayCloseId(String arApItemId) {
            return Optional.ofNullable(itemLocks.get(arApItemId));
        }

        @Override
        public boolean lockedDailyCloseExistsForDate(LocalDate businessDate) {
            return dailyCloses.values().stream()
                    .anyMatch(dc -> businessDate.equals(dc.businessDate()) && "locked".equals(dc.status()));
        }

        @Override
        public FinanceRepository.DailyCloseSourceTotals aggregateOfficialTicket(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return officialTicketTotals;
        }

        @Override
        public FinanceRepository.DailyCloseSourceTotals aggregateChannelTicket(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return channelTicketTotals;
        }

        @Override
        public FinanceRepository.DailyCloseSourceTotals aggregateBar(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return barTotals;
        }

        @Override
        public FinanceRepository.DailyCloseSourceTotals aggregateAmusement(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return amusementTotals;
        }

        @Override
        public FinanceRepository.DailyCloseSourceTotals aggregateSupplyCost(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return supplyCostTotals;
        }

        @Override
        public FinanceRepository.DailyCloseSourceTotals aggregateThirdpartyReflow(LocalDate businessDate, java.time.Instant fromInclusive, java.time.Instant toExclusive) {
            return thirdpartyTotals;
        }

        @Override
        public ArApAdjustmentRecord saveArApAdjustment(ArApAdjustmentRecord adjustment) {
            adjustments.putIfAbsent(adjustment.id(), adjustment);
            return adjustments.get(adjustment.id());
        }

        @Override
        public Optional<ArApAdjustmentRecord> findArApAdjustmentByIdForUpdate(String id) {
            return Optional.ofNullable(adjustments.get(id));
        }

        @Override
        public ArApAdjustmentRecord updateArApAdjustment(ArApAdjustmentRecord adjustment) {
            adjustments.put(adjustment.id(), adjustment);
            return adjustment;
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
