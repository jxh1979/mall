package com.complex.platform.finance;

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
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
import com.complex.platform.finance.dto.PropertyFeeBillRequest;
import com.complex.platform.finance.dto.PropertyFeeRateRequest;
import com.complex.platform.finance.dto.CloseReconciliationDifferenceRequest;
import com.complex.platform.finance.dto.ReconciliationDifferenceRequest;
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
        // AUD-007 退款额度闸已 fail-closed 常在：默认给宽松引擎 + 操作者，令既有「额度内退款成功」用例仍过；
        // 具体拒绝/额度场景各用例自行覆盖 refundAuthorizationService/currentActor。
        service.refundAuthorizationService = new RefundAuthorizationService(permissiveRefundRepo());
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("op_default", "op_default", "staff", "org_1", null, "active"));
    }

    private static RefundAuthorizationRepository permissiveRefundRepo() {
        Instant ts = Instant.parse("2026-06-23T00:00:00Z");
        return new RefundAuthorizationRepository() {
            @Override public Optional<RefundAuthorityLimitRecord> findActiveLimit(String st, String sid, String dom, String pid) {
                return Optional.of(new RefundAuthorityLimitRecord("perm", st, sid, dom, Long.MAX_VALUE, pid, "active", ts, ts));
            }
            @Override public boolean isAutoRuleEnabled(String dom, String type, String pid) { return false; }
            @Override public RefundAuthorityLimitRecord saveLimit(RefundAuthorityLimitRecord l) { return l; }
            @Override public List<RefundAuthorityLimitRecord> listLimits(String projectId) { return List.of(); }
            @Override public RefundAutoRuleRecord saveRule(RefundAutoRuleRecord r) { return r; }
            @Override public List<RefundAutoRuleRecord> listRules(String projectId) { return List.of(); }
        };
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
    void exportArApItemsCsvHeaderIsChineseNotEnglish() {
        // 用户下载的 AR/AP 台账 CSV 表头必须是中文业务标签，不得泄露英文字段名。
        service.createArApItem(itemRequest("arap-csv-i18n", "receivable", 128800L));

        String csv = service.exportArApItems().csvText();
        String header = csv.split("\n", 2)[0];

        assertThat(header)
                .isEqualTo("单据编号,收付方向,往来对象编号,来源类型,来源编号,金额(分),已收付金额(分),未收付金额(分),币种,到期日,账期,状态");
        // 反向守卫：改回英文表头该断言必红。
        assertThat(header).doesNotContain("item_no").doesNotContain("direction");
    }

    @Test
    void exportArApItemsCsvDataRowsTranslateEnumsToChinese() {
        // 用户下载的 AR/AP 台账 CSV 数据行里，收付方向/来源类型/状态等枚举必须是中文，不得中英混用泄露英文枚举值。
        // 应收·待收款
        service.createArApItem(itemRequest("arap-csv-recv", "receivable", 50000L));
        // 应付·待付款
        service.createArApItem(itemRequest("arap-csv-pay", "payable", 60000L));
        // 应收·全额收款 → 已结清
        ArApItemRecord full = service.createArApItem(itemRequest("arap-csv-full", "receivable", 70000L));
        service.registerArApPayment(full.id(), paymentRequest("pay-csv-full", 70000L));
        // 应收·部分收款 → 部分收付
        ArApItemRecord partial = service.createArApItem(itemRequest("arap-csv-part", "receivable", 80000L));
        service.registerArApPayment(partial.id(), paymentRequest("pay-csv-part", 30000L));

        String csv = service.exportArApItems().csvText();
        String[] lines = csv.split("\n");
        // 数据行 = 去掉表头行。
        String body = String.join("\n", java.util.Arrays.copyOfRange(lines, 1, lines.length));

        // 方向中文、来源类型中文、状态中文均出现在数据行。
        assertThat(body).contains("应收").contains("应付").contains("订单");
        assertThat(body).contains("待收款").contains("待付款").contains("已结清").contains("部分收付");
        // 反向守卫：数据行不得再出现英文枚举值（改回英文映射该断言必红）。
        assertThat(body)
                .doesNotContain("receivable")
                .doesNotContain("payable")
                .doesNotContain("pending_collection")
                .doesNotContain("pending_payment")
                .doesNotContain("partial_paid")
                .doesNotContain("settled");
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
    void createPayableItemAcceptsEmployeeCommissionSourceForBarSeatCommission() {
        ArApItemRecord created = service.createArApItem(new ArApItemRequest(
                "arap-employee-commission-source",
                "bar table commission payable",
                List.of("att_commission"),
                "payable",
                "staff_sales_001",
                "employee_commission",
                "commission_001",
                19320L,
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
                .containsExactly("payable", "employee_commission", "commission_001", 19320L, "pending_payment");
        assertThat(financeRepository.arApItemSources.values())
                .extracting(
                        ArApItemSourceRecord::arApItemId,
                        ArApItemSourceRecord::sourceType,
                        ArApItemSourceRecord::sourceId,
                        ArApItemSourceRecord::amountCent
                )
                .containsExactly(tuple(created.id(), "employee_commission", "commission_001", 19320L));
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

    /**
     * V304 支付守卫（资金安全命根）：分红发放保护线留存（待释放）应付未释放前禁止付款——「待释放」是真锁；
     * 释放（re-tag → dividend_reserve_released）后同一笔应付放行可付。两个方向都有活证据（拒付 / 放行）。
     */
    @Test
    void reserveWithheldPayableBlockedFromPaymentUntilReleased() {
        Instant now = Instant.parse("2026-07-19T00:00:00Z");
        financeRepository.saveArApItem(new ArApItemRecord(
                "arap_res_guard", "ARAP-RES-GUARD", "payable", "usr_principal",
                "dividend_reserve_withheld", "divs_guard", 16000L, 0L, 16000L, "CNY",
                LocalDate.parse("2026-07-19"), "2026-07", "pending_payment", now, now, "merchant_guard"));

        // 未释放 → 拒付。
        assertThatThrownBy(() ->
                service.registerArApPayment("arap_res_guard", paymentRequest("res-guard-pay", 16000L)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_RESERVE_NOT_RELEASED"));

        // 释放：re-tag 待释放 → 已释放（金额不变）。
        assertThat(service.releaseDividendReserveWithholding("divs_guard")).isPresent();

        // 已释放 → 放行可付，落账 settled。
        ArApPaymentResponse resp = service.registerArApPayment(
                "arap_res_guard", paymentRequest("res-guard-pay2", 16000L));
        assertThat(resp.item().sourceType()).isEqualTo("dividend_reserve_released");
        assertThat(resp.item().status()).isEqualTo("settled");
        assertThat(resp.item().unpaidAmountCent()).isZero();
    }

    @Test
    void createArApItemRejectsNegativeAmountWithValidationFailed() {
        // P1-13：金额下界负向测试——createArApItem 负数金额必须被 requirePositive 拒绝（填补"只测上界"的真空，
        // 防负额反向操纵台账：负应收=凭空冲减）。
        assertThatThrownBy(() -> service.createArApItem(itemRequest("arap-negative", "receivable", -5000L)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
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
        PaymentIntentRecord auditIntent = service.createPaymentIntent(new PaymentIntentRequest(
                "payment-intent-audit",
                "create",
                List.of(),
                "ticket_order",
                "order_audit_1",
                128800L,
                "CNY",
                "mock"
        ));
        // 2d-2：confirmOffline 现要求关联真实支付意图且金额一致 → 用 createPaymentIntent 返回的 id 与金额。
        service.confirmOfflinePayment(auditIntent.id(), new OfflinePaymentConfirmRequest(
                "offline-confirm-audit",
                "confirm",
                List.of("attachment_1"),
                auditIntent.id(),
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
                "original_channel",
                "original"
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
    void confirmOfflineWithArApItemWritesBackLedger() {
        // F1 财务统一台账：线下确认收款关联应收台账项 → 同事务回写 ar_ap（paid/unpaid/status）+ 登记 payment。
        ArApItemRecord item = service.createArApItem(itemRequest("arap-f1-offline", "receivable", 50000L));
        assertThat(financeRepository.findArApItemById(item.id()).orElseThrow().unpaidAmountCent()).isEqualTo(50000L);
        PaymentIntentRecord intent = service.createPaymentIntent(new PaymentIntentRequest(
                "payment-intent-f1", "create", List.of(), "ar_ap_item", item.id(), 50000L, "CNY", "mock"));

        service.confirmOfflinePayment(intent.id(), new OfflinePaymentConfirmRequest(
                "offline-confirm-f1",
                "confirm",
                List.of("attachment_f1"),
                intent.id(),
                50000L,
                "cash",
                Instant.parse("2026-06-14T00:05:00Z"),
                item.id()));

        ArApItemRecord settled = financeRepository.findArApItemById(item.id()).orElseThrow();
        assertThat(settled.paidAmountCent()).isEqualTo(50000L);
        assertThat(settled.unpaidAmountCent()).isEqualTo(0L);
        assertThat(settled.status()).isEqualTo("settled");
    }

    @Test
    void paymentIntentSucceededRecordsInternalAccountingLedger() {
        // F2：支付意图确认成功 → 生成 direction=internal_accounting 内部核算台账（幂等、settled、不并入 net）。
        PaymentIntentRecord intent = service.createPaymentIntent(new PaymentIntentRequest(
                "payment-intent-f2", "create", List.of(), "ticket_order", "order_f2_1", 80000L, "CNY", "mock"));
        service.markPaymentIntentSucceeded(intent.id());

        ArApItemRecord internal = financeRepository.listArApItems().stream()
                .filter(i -> "internal_accounting".equals(i.direction()) && "order_f2_1".equals(i.sourceId()))
                .findFirst().orElseThrow();
        assertThat(internal.paidAmountCent()).isEqualTo(80000L);
        assertThat(internal.unpaidAmountCent()).isEqualTo(0L);
        assertThat(internal.status()).isEqualTo("settled");

        // 幂等：再次确认（已 succeeded 早返回）不新增。
        int countBefore = (int) financeRepository.listArApItems().stream()
                .filter(i -> "internal_accounting".equals(i.direction())).count();
        service.markPaymentIntentSucceeded(intent.id());
        int countAfter = (int) financeRepository.listArApItems().stream()
                .filter(i -> "internal_accounting".equals(i.direction())).count();
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void internalAccountingLedgerKeyedPerPaymentIntentNotPerBusinessOrder() {
        // 回归：同一 businessOrderId（如储值充值的 customerId）的两笔支付意图必须各生成独立内部核算台账，
        // 不被 ON CONFLICT DO UPDATE 覆盖（修正 id 仅按 type+businessOrderId 派生导致的少计）。
        PaymentIntentRecord first = service.createPaymentIntent(new PaymentIntentRequest(
                "intent-dup-1", "create", List.of(), "stored_value_topup", "customer_dup", 10000L, "CNY", "mock"));
        PaymentIntentRecord second = service.createPaymentIntent(new PaymentIntentRequest(
                "intent-dup-2", "create", List.of(), "stored_value_topup", "customer_dup", 20000L, "CNY", "mock"));
        service.markPaymentIntentSucceeded(first.id());
        service.markPaymentIntentSucceeded(second.id());

        long count = financeRepository.listArApItems().stream()
                .filter(i -> "internal_accounting".equals(i.direction()) && "customer_dup".equals(i.sourceId()))
                .count();
        assertThat(count).isEqualTo(2L); // 两笔独立归集，未被覆盖
        long total = financeRepository.listArApItems().stream()
                .filter(i -> "internal_accounting".equals(i.direction()) && "customer_dup".equals(i.sourceId()))
                .mapToLong(ArApItemRecord::amountCent).sum();
        assertThat(total).isEqualTo(30000L);
    }

    @Test
    void confirmOfflineWithoutArApItemLeavesLedgerUntouched() {
        // 向后兼容：不带 arApItemId 的纯平台收款确认不创建/不回写任何 ar_ap 台账。
        PaymentIntentRecord intent = service.createPaymentIntent(new PaymentIntentRequest(
                "payment-intent-f1b", "create", List.of(), "ticket_order", "order_f1b", 30000L, "CNY", "mock"));
        int before = financeRepository.arApPaymentCount();
        service.confirmOfflinePayment(intent.id(), new OfflinePaymentConfirmRequest(
                "offline-confirm-f1b", "confirm", List.of(), intent.id(), 30000L, "cash",
                Instant.parse("2026-06-14T00:05:00Z")));
        assertThat(financeRepository.arApPaymentCount()).isEqualTo(before);
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
                "amount",
                "payment_1",
                100L,
                "open",
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        PageResponse<ReconciliationBatchRecord> batches = service.listReconciliationBatches();
        PageResponse<ReconciliationDifferenceRecord> differences = service.listReconciliationDifferences();

        assertThat(batches.items()).extracting(ReconciliationBatchRecord::batchNo).contains("RB202606140001");
        assertThat(differences.items()).extracting(ReconciliationDifferenceRecord::differenceType).contains("amount");
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

    // ─── P0#7：对账差异强制化（R6-D07）───────────────────────────────────────

    @Test
    void pureManualDifferenceWithoutSystemTakingsIsRejected() {
        seedReconciliationBatch("recon_batch_pm");
        // 无平台侧系统取数支撑（aggregatePlatformExpectedForBusinessRef=0）→ 纯手填全字段差异被拒。
        assertThatThrownBy(() -> service.createReconciliationDifference(new ReconciliationDifferenceRequest(
                "diff-pure-manual", "note", List.of(),
                "recon_batch_pm", "amount", "payment_no_system", 9999L, "manual")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RECONCILIATION_NO_SYSTEM_EXPECTED");
        assertThat(financeRepository.listReconciliationDifferences()).isEmpty();
    }

    @Test
    void systemSourcedDifferenceTakesExpectedFromSystemNotRequestBody() {
        seedReconciliationBatch("recon_batch_sys");
        // 平台侧系统取数 expected=50000（不接受请求体手填 expected；请求体只给 actual）。
        financeRepository.platformExpectedByRef.put("payment_sys", 50000L);

        ReconciliationDifferenceRecord diff = service.createReconciliationDifference(new ReconciliationDifferenceRequest(
                "diff-system", "note", List.of(),
                "recon_batch_sys", "amount", "payment_sys", 48000L, "system"));

        assertThat(diff.expectedAmountCent()).isEqualTo(50000L); // 来自系统取数
        assertThat(diff.actualAmountCent()).isEqualTo(48000L);
        assertThat(diff.amountCent()).isEqualTo(2000L);          // 差额 = |expected − actual|
        assertThat(diff.source()).isEqualTo("system");
        assertThat(diff.status()).isEqualTo("open");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("finance_reconciliation.difference_created");
    }

    @Test
    void closeReconciliationDifferenceMarksClosedIdempotentlyWithReason() {
        // 1.6：对账差异正式关闭——落 closed + close_reason + 审计，重复 close 幂等。
        seedReconciliationBatch("recon_batch_close");
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("op_close", "op_close", "staff", "org_1", null, "active"));
        financeRepository.platformExpectedByRef.put("payment_close", 50000L);
        ReconciliationDifferenceRecord diff = service.createReconciliationDifference(new ReconciliationDifferenceRequest(
                "diff-close", "note", List.of(),
                "recon_batch_close", "amount", "payment_close", 48000L, "system"));
        assertThat(diff.status()).isEqualTo("open");

        ReconciliationDifferenceRecord closed = service.closeReconciliationDifference(diff.id(),
                new CloseReconciliationDifferenceRequest("close-1", "手工核对后差异合理，正式关闭", List.of()));
        assertThat(closed.status()).isEqualTo("closed");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("finance.reconciliation.difference_closed");

        // 幂等：同 key 重复 close 仍 closed、不报错、不翻状态。
        ReconciliationDifferenceRecord again = service.closeReconciliationDifference(diff.id(),
                new CloseReconciliationDifferenceRequest("close-1", "手工核对后差异合理，正式关闭", List.of()));
        assertThat(again.status()).isEqualTo("closed");
    }

    @Test
    void manualSourcedDifferenceWithSystemTakingsIsForcedIntoReview() {
        seedReconciliationBatch("recon_batch_man");
        // 第三方侧未接入：有平台侧系统取数支撑，允许 source=manual 录入 actual，但强制进复核。
        financeRepository.platformExpectedByRef.put("payment_man", 30000L);

        ReconciliationDifferenceRecord diff = service.createReconciliationDifference(new ReconciliationDifferenceRequest(
                "diff-manual", "note", List.of(),
                "recon_batch_man", "amount", "payment_man", 31000L, "manual"));

        assertThat(diff.source()).isEqualTo("manual");
        assertThat(diff.expectedAmountCent()).isEqualTo(30000L);
        assertThat(diff.status()).isEqualTo("needs_review");
    }

    @Test
    void invalidDifferenceTypeIsRejectedByEnum() {
        seedReconciliationBatch("recon_batch_enum");
        financeRepository.platformExpectedByRef.put("payment_enum", 10000L);
        // 非四类(amount/count/status/missing)的遗留值被拒（服务层枚举对齐 V105 CHECK）。
        assertThatThrownBy(() -> service.createReconciliationDifference(new ReconciliationDifferenceRequest(
                "diff-bad-type", "note", List.of(),
                "recon_batch_enum", "amount_mismatch", "payment_enum", 9000L, "system")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void allFourDifferenceTypesAreAccepted() {
        seedReconciliationBatch("recon_batch_4");
        financeRepository.platformExpectedByRef.put("payment_4", 40000L);
        for (String type : List.of("amount", "count", "status", "missing")) {
            ReconciliationDifferenceRecord diff = service.createReconciliationDifference(new ReconciliationDifferenceRequest(
                    "diff-" + type, "note", List.of(),
                    "recon_batch_4", type, "payment_4", 39000L, "system"));
            assertThat(diff.differenceType()).isEqualTo(type);
        }
    }

    private void seedReconciliationBatch(String batchId) {
        financeRepository.saveReconciliationBatch(new ReconciliationBatchRecord(
                batchId, "RB-" + batchId, "project", "project_1",
                LocalDate.parse("2026-06-18"), "created", Instant.parse("2026-06-18T00:00:00Z")));
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
                null,
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
                null,
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

        service.createRefund(new RefundRequest("refund-part1", "refund", List.of(), paymentId, 6000L, "partial", null, null));

        assertThatThrownBy(() -> service.createRefund(new RefundRequest(
                "refund-part2",
                "refund",
                List.of(),
                paymentId,
                5000L,
                "over cumulative",
                null,
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    // ─── P1-A：财务项目级隔离(与储值同范式) FAILING FIRST ────────────────────
    @Test
    void crossProjectSettlementIsForbidden() {
        // 项目 A 创建的应收,项目 B 的财务员不得登记收款(project_id 越权)。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentProjectScopeQuietly()).thenReturn(Optional.of("project_A"));
        ArApItemRecord item = service.createArApItem(itemRequest("arap-cross-proj", "receivable", 50000L));

        // 切到项目 B 调用方。
        when(authorizationService.currentProjectScopeQuietly()).thenReturn(Optional.of("project_B"));
        assertThatThrownBy(() -> service.registerArApPayment(item.id(), paymentRequest("pay-cross-proj", 10000L)))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("CROSS_PROJECT_FORBIDDEN");
    }

    @Test
    void sameProjectSettlementIsAllowed() {
        // 同项目正常放行(不误伤)。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentProjectScopeQuietly()).thenReturn(Optional.of("project_A"));
        ArApItemRecord item = service.createArApItem(itemRequest("arap-same-proj", "receivable", 50000L));

        ArApPaymentResponse resp = service.registerArApPayment(item.id(), paymentRequest("pay-same-proj", 10000L));
        assertThat(resp.item().paidAmountCent()).isEqualTo(10000L);
    }

    // ─── P3：现场渠道退款缺设备上下文应拒(服务端判定,非请求体 deviceId) FAILING FIRST ──
    @Test
    void onsiteChannelRefundWithoutDeviceIsRejected() {
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        ArApItemRecord item = service.createArApItem(itemRequest("arap-onsite-refund", "receivable", 30000L));
        ArApPaymentResponse payment = service.registerArApPayment(item.id(), paymentRequest("pay-onsite", 30000L));

        // 现场渠道(cash/pos)退款但 deviceId 为空 → 服务端 fail-closed 拒绝,不得绕过设备门控。
        assertThatThrownBy(() -> service.createRefund(new RefundRequest(
                "refund-onsite-nodev", "refund", List.of(), payment.payment().id(),
                10000L, "onsite cash refund", "cash", null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("DEVICE_CONTEXT_REQUIRED");
    }

    @Test
    void backOfficeChannelRefundWithoutDeviceIsAllowed() {
        // 后台渠道(manual)退款无需设备上下文,不误伤后台台账类退款。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        ArApItemRecord item = service.createArApItem(itemRequest("arap-backoffice-refund", "receivable", 30000L));
        ArApPaymentResponse payment = service.registerArApPayment(item.id(), paymentRequest("pay-backoffice", 30000L));

        FinanceRefundRecord refund = service.createRefund(new RefundRequest(
                "refund-backoffice", "refund", List.of(), payment.payment().id(),
                10000L, "manual back-office refund", "manual", null, null));
        assertThat(refund.status()).isEqualTo("requested");
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
                null,
                "cash"
        ));

        assertThat(refund.refundAmountCent()).isEqualTo(15000L);
        assertThat(refund.status()).isEqualTo("requested");
        // P2#④ 显式传入的退款方向被统一落地。
        assertThat(refund.refundDirection()).isEqualTo("cash");
    }

    @Test
    void refundWithoutTieredEngineFailsClosed() {
        // AUD-007 依赖缺失 fail-closed：分级授权引擎未注入（生产恒注入，此处模拟异常）→ 拒绝而非放行。
        service.refundAuthorizationService = null;
        ArApItemRecord item = service.createArApItem(itemRequest("arap-failclosed", "receivable", 30000L));
        ArApPaymentResponse payment = service.registerArApPayment(item.id(), paymentRequest("pay-failclosed", 30000L));
        assertThatThrownBy(() -> service.createRefund(new RefundRequest(
                "refund-failclosed", "refund", List.of(), payment.payment().id(), 10000L, "no engine", "manual", null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_AUTHORIZATION_UNAVAILABLE");
    }

    @Test
    void tieredAuthorizationBlocksOverLimitFinanceRefundButAllowsWithinLimit() {
        // Stage B（审计 A2，适用所有退款）：开启分级授权后，超额度财务退款被拒（REFUND_REVIEW_REQUIRED）、额度内放行。
        Instant ts = Instant.parse("2026-06-23T00:00:00Z");
        RefundAuthorizationRepository refundRepo = new RefundAuthorizationRepository() {
            @Override
            public Optional<RefundAuthorityLimitRecord> findActiveLimit(String st, String sid, String dom, String pid) {
                if ("staff".equals(st) && "finance".equals(dom) && "op1".equals(sid)) {
                    return Optional.of(new RefundAuthorityLimitRecord(
                            "l_op1", "staff", "op1", "finance", 20000L, null, "active", ts, ts));
                }
                return Optional.empty();
            }
            @Override public boolean isAutoRuleEnabled(String dom, String type, String pid) { return false; }
            @Override public RefundAuthorityLimitRecord saveLimit(RefundAuthorityLimitRecord l) { return l; }
            @Override public List<RefundAuthorityLimitRecord> listLimits(String projectId) { return List.of(); }
            @Override public RefundAutoRuleRecord saveRule(RefundAutoRuleRecord r) { return r; }
            @Override public List<RefundAutoRuleRecord> listRules(String projectId) { return List.of(); }
        };
        service.refundAuthorizationService = new RefundAuthorizationService(refundRepo);
        // 操作者 op1（非平台管理员→闸生效）；isPlatformAdmin 走 mock 默认 false。
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("op1", "op1", "staff", "org_1", null, "active"));

        ArApItemRecord item = service.createArApItem(itemRequest("arap-tier-refund", "receivable", 100000L));
        ArApPaymentResponse payment = service.registerArApPayment(item.id(), paymentRequest("pay-tier", 100000L));

        // 超额度（30000 > 20000）→ 复核拒，不落退款记录。
        assertThatThrownBy(() -> service.createRefund(new RefundRequest(
                "refund-tier-over", "refund", List.of(), payment.payment().id(), 30000L, "over limit", null, "cash")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_REVIEW_REQUIRED");

        // 额度内（15000 ≤ 20000）→ 放行。
        FinanceRefundRecord ok = service.createRefund(new RefundRequest(
                "refund-tier-within", "refund", List.of(), payment.payment().id(), 15000L, "within limit", null, "cash"));
        assertThat(ok.refundAmountCent()).isEqualTo(15000L);
    }

    @Test
    void refundWithoutDirectionDefaultsToOriginal() {
        ArApItemRecord item = service.createArApItem(itemRequest("arap-default-dir", "receivable", 30000L));
        ArApPaymentResponse payment = service.registerArApPayment(item.id(), paymentRequest("pay-default-dir", 30000L));

        FinanceRefundRecord refund = service.createRefund(new RefundRequest(
                "refund-default-dir", "refund", List.of(), payment.payment().id(), 5000L, "no direction given", null, null));

        // 未传方向 → 统一默认 original（原路退回）。
        assertThat(refund.refundDirection()).isEqualTo("original");
    }

    @Test
    void refundWithInvalidDirectionIsRejected() {
        ArApItemRecord item = service.createArApItem(itemRequest("arap-bad-dir", "receivable", 30000L));
        ArApPaymentResponse payment = service.registerArApPayment(item.id(), paymentRequest("pay-bad-dir", 30000L));

        assertThatThrownBy(() -> service.createRefund(new RefundRequest(
                "refund-bad-dir", "refund", List.of(), payment.payment().id(), 5000L, "bad dir", null, "wormhole")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    // ─── 跨模块闭环②a：MDM 高风险门控接入（finance 链路无 device 上下文，待 device） ───────────
    //
    // 现状：finance 支付/退款为后台财务台账操作，RefundRequest/PaymentIntentRequest 不携带 device_id，
    // 链路无设备上下文。按"不伪造假值"，门控接入点在 device_id 缺失时安全跳过（不阻断财务主链）。
    // 故下述两测验证：注入 MdmService 后，当前链路（device 缺失）不会触发门控；
    // 待端侧补传 device_id 后，调用点把真实 deviceId 传入即自动启用硬门控（接入点已就绪）。

    private FinanceService financeWithMdm(com.complex.platform.terminal.MdmService mdmService) {
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        AuditedOperationExecutor ops = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()), auditEventService);
        FinanceService svc = new FinanceService(
                financeRepository, ops, auditEventService, authorizationService,
                new com.complex.platform.common.BusinessDayResolver(4),
                com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(),
                mdmService);
        svc.refundAuthorizationService = new RefundAuthorizationService(permissiveRefundRepo());
        return svc;
    }

    @Test
    void refundWithoutDeviceContextSkipsMdmGate() {
        com.complex.platform.terminal.MdmService mdmService = mock(com.complex.platform.terminal.MdmService.class);
        FinanceService svc = financeWithMdm(mdmService);

        ArApItemRecord item = svc.createArApItem(itemRequest("arap-mdm-refund", "receivable", 30000L));
        ArApPaymentResponse payment = svc.registerArApPayment(item.id(), paymentRequest("pay-mdm", 30000L));
        FinanceRefundRecord refund = svc.createRefund(new RefundRequest(
                "refund-mdm", "refund", List.of(), payment.payment().id(), 15000L, "guest request", null, null));

        assertThat(refund.status()).isEqualTo("requested");
        // device 上下文缺失（RefundRequest 无 device_id）→ 门控安全跳过，不调用 requireManagedForHighRiskAction。
        org.mockito.Mockito.verify(mdmService, org.mockito.Mockito.never())
                .requireManagedForHighRiskAction(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createPaymentIntentWithoutDeviceContextSkipsMdmGate() {
        com.complex.platform.terminal.MdmService mdmService = mock(com.complex.platform.terminal.MdmService.class);
        FinanceService svc = financeWithMdm(mdmService);

        PaymentIntentRecord intent = svc.createPaymentIntent(new PaymentIntentRequest(
                "pi-mdm", "create", List.of(), "ticket_order", "order_mdm", 5000L, "CNY", "wechat"));

        assertThat(intent.status()).isEqualTo("created");
        // device 上下文缺失（PaymentIntentRequest 无 device_id）→ 门控安全跳过。
        org.mockito.Mockito.verify(mdmService, org.mockito.Mockito.never())
                .requireManagedForHighRiskAction(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createPaymentIntentRejectsUnknownChannel() {
        FinanceService svc = financeWithGatewaySwitch(false);

        assertThatThrownBy(() -> svc.createPaymentIntent(new PaymentIntentRequest(
                "pi-bad-channel", "create", List.of(), "ticket_order", "order_bad_channel",
                5000L, "CNY", "unsupported_gateway")))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "VALIDATION_FAILED");
    }

    // ─── 跨模块闭环②a：deviceId 真实启用门控（现场/工作机发起携带 device_id） ───────────────

    @Test
    void onsiteRefundWithUnmanagedDeviceIsRejectedByMdmGate() {
        com.complex.platform.terminal.MdmService mdmService = mock(com.complex.platform.terminal.MdmService.class);
        // 未纳管设备执行高风险动作 → MdmService 硬拒绝（开关开+未纳管/blocked/lost）。
        org.mockito.Mockito.doThrow(ApiException.forbidden("DEVICE_NOT_MANAGED", "device is not managed by MDM: dev_unmanaged"))
                .when(mdmService).requireManagedForHighRiskAction("dev_unmanaged", "finance.refund");
        FinanceService svc = financeWithMdm(mdmService);

        ArApItemRecord item = svc.createArApItem(itemRequest("arap-mdm-onsite-reject", "receivable", 30000L));
        ArApPaymentResponse payment = svc.registerArApPayment(item.id(), paymentRequest("pay-mdm-reject", 30000L));

        // 现场退款携带未纳管 deviceId → 门控真实生效，退款被拒（财务产物未写）。
        assertThatThrownBy(() -> svc.createRefund(new RefundRequest(
                "refund-mdm-onsite-reject", "refund", List.of(), payment.payment().id(),
                15000L, "guest request", null, null, "dev_unmanaged")))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "DEVICE_NOT_MANAGED");
        org.mockito.Mockito.verify(mdmService)
                .requireManagedForHighRiskAction("dev_unmanaged", "finance.refund");
    }

    @Test
    void onsiteRefundWithManagedDeviceIsAllowed() {
        com.complex.platform.terminal.MdmService mdmService = mock(com.complex.platform.terminal.MdmService.class);
        // 已纳管设备 → requireManagedForHighRiskAction 不抛（mock 默认 void 不抛）→ 放行。
        FinanceService svc = financeWithMdm(mdmService);

        ArApItemRecord item = svc.createArApItem(itemRequest("arap-mdm-onsite-ok", "receivable", 30000L));
        ArApPaymentResponse payment = svc.registerArApPayment(item.id(), paymentRequest("pay-mdm-ok", 30000L));
        FinanceRefundRecord refund = svc.createRefund(new RefundRequest(
                "refund-mdm-onsite-ok", "refund", List.of(), payment.payment().id(),
                15000L, "guest request", null, null, "dev_managed"));

        assertThat(refund.status()).isEqualTo("requested");
        // 携带已纳管 deviceId → 门控真实被调用（且未拒）。
        org.mockito.Mockito.verify(mdmService)
                .requireManagedForHighRiskAction("dev_managed", "finance.refund");
    }

    @Test
    void onsitePaymentIntentWithUnmanagedDeviceIsRejectedByMdmGate() {
        com.complex.platform.terminal.MdmService mdmService = mock(com.complex.platform.terminal.MdmService.class);
        org.mockito.Mockito.doThrow(ApiException.forbidden("DEVICE_NOT_MANAGED", "device is not managed by MDM: dev_unmanaged"))
                .when(mdmService).requireManagedForHighRiskAction("dev_unmanaged", "finance.payment");
        FinanceService svc = financeWithMdm(mdmService);

        // 现场收款（支付意图）携带未纳管 deviceId → 门控真实生效，被拒。
        assertThatThrownBy(() -> svc.createPaymentIntent(new PaymentIntentRequest(
                "pi-mdm-reject", "create", List.of(), "ticket_order", "order_mdm_reject",
                5000L, "CNY", "wechat", "dev_unmanaged")))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "DEVICE_NOT_MANAGED");
        org.mockito.Mockito.verify(mdmService)
                .requireManagedForHighRiskAction("dev_unmanaged", "finance.payment");
    }

    @Test
    void onsiteArApPaymentWithUnmanagedDeviceIsRejectedByMdmGate() {
        com.complex.platform.terminal.MdmService mdmService = mock(com.complex.platform.terminal.MdmService.class);
        org.mockito.Mockito.doThrow(ApiException.forbidden("DEVICE_NOT_MANAGED", "device is not managed by MDM: dev_unmanaged"))
                .when(mdmService).requireManagedForHighRiskAction("dev_unmanaged", "finance.payment");
        FinanceService svc = financeWithMdm(mdmService);

        ArApItemRecord item = svc.createArApItem(itemRequest("arap-mdm-pay-reject", "receivable", 30000L));

        // 现场登记收付款携带未纳管 deviceId → 门控真实生效，被拒。
        assertThatThrownBy(() -> svc.registerArApPayment(item.id(), new ArApPaymentRequest(
                "pay-mdm-onsite-reject", "register", List.of(), 15000L, "pos",
                Instant.parse("2026-06-14T01:00:00Z"), "flow_onsite", "dev_unmanaged")))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "DEVICE_NOT_MANAGED");
        org.mockito.Mockito.verify(mdmService)
                .requireManagedForHighRiskAction("dev_unmanaged", "finance.payment");
    }

    @Test
    void backofficeArApPaymentWithoutDeviceSkipsMdmGate() {
        com.complex.platform.terminal.MdmService mdmService = mock(com.complex.platform.terminal.MdmService.class);
        FinanceService svc = financeWithMdm(mdmService);

        ArApItemRecord item = svc.createArApItem(itemRequest("arap-mdm-backoffice", "receivable", 30000L));
        // 后台台账类登记收付款无 device 上下文（兼容构造，deviceId=null）→ 门控安全跳过。
        ArApPaymentResponse payment = svc.registerArApPayment(item.id(), paymentRequest("pay-mdm-backoffice", 30000L));

        assertThat(payment.payment().amountCent()).isEqualTo(30000L);
        org.mockito.Mockito.verify(mdmService, org.mockito.Mockito.never())
                .requireManagedForHighRiskAction(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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
        PageResponse<ArApItemRecord> result = service.listArApItems("", 1, 20);
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
        PageResponse<ArApItemRecord> result = service.listArApItems("", 1, 20);
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
                null,
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
                null,
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

        // P0 B-01 职责分离：审批人必须不同于发起人。
        stubActor("staff_finance_supervisor");
        // 审批通过：金额变动入账。
        ArApAdjustmentRecord approved = service.approveAdjustment(requested.id(),
                new com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest("adj-dec-1", "ok", List.of(), "approve"));
        assertThat(approved.approvalStatus()).isEqualTo("approved");
        // P0-B（V344）红冲：原行金额分文未动。
        assertThat(financeRepository.findArApItemById(item.id()).orElseThrow().amountCent()).isEqualTo(100000L);
        // 追加一条严格取反的冲正行（原行三个金额列全部取反）。
        ArApItemRecord reversal = financeRepository.findArApItemById("arap_rev_" + requested.id()).orElseThrow();
        assertThat(reversal.originItemId()).isEqualTo(item.id());
        assertThat(reversal.adjustmentId()).isEqualTo(requested.id());
        assertThat(reversal.status()).isEqualTo("reversed");
        assertThat(reversal.amountCent()).isEqualTo(-item.amountCent());
        assertThat(reversal.paidAmountCent()).isEqualTo(-item.paidAmountCent());
        assertThat(reversal.unpaidAmountCent()).isEqualTo(-item.unpaidAmountCent());
        // 追加一条重述行（原额 + 补录额），承接原行的收付方向/往来对象/来源单据/到期日/归属。
        ArApItemRecord restated = financeRepository.findArApItemById("arap_res_" + requested.id()).orElseThrow();
        assertThat(restated.originItemId()).isEqualTo(item.id());
        assertThat(restated.amountCent()).isEqualTo(105000L);
        assertThat(restated.paidAmountCent()).isEqualTo(item.paidAmountCent());
        assertThat(restated.direction()).isEqualTo(item.direction());
        assertThat(restated.counterpartyId()).isEqualTo(item.counterpartyId());
        assertThat(restated.dueDate()).isEqualTo(item.dueDate());
        // 守恒：原行 + 冲正行 + 重述行三行相加 = 补录后应有金额，且账平恒等（已付 + 未付 = 总额）逐行成立。
        ArApItemRecord origin = financeRepository.findArApItemById(item.id()).orElseThrow();
        assertThat(origin.amountCent() + reversal.amountCent() + restated.amountCent()).isEqualTo(105000L);
        assertThat(origin.paidAmountCent() + reversal.paidAmountCent() + restated.paidAmountCent())
                .isEqualTo(item.paidAmountCent());
        for (ArApItemRecord row : List.of(origin, reversal, restated)) {
            assertThat(row.paidAmountCent() + row.unpaidAmountCent()).isEqualTo(row.amountCent());
        }
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("finance.locked_backfill_requested", "finance.locked_backfill_approved");
    }

    @Test
    void backfillSelfDecisionForbiddenBySeparationOfDuties() {
        stubActor("staff_finance_self");
        ArApItemRecord item = service.createArApItem(itemRequest("arap-backfill-sod", "receivable", 100000L));
        service.createDailyClose(new DailyCloseRequest(
                "daily-close-backfill-sod", "lock", List.of(),
                LocalDate.parse("2026-06-30"), "project", "project_sod", false));
        ArApAdjustmentRecord requested = service.requestAdjustment(new com.complex.platform.finance.dto.ArApAdjustmentRequest(
                "adj-req-sod", "backfill", List.of(), item.id(), 5000L, "self decision attempt"));

        // F-OPS-07 / B-01：发起人既不能自审批、也不能自拒绝（A 版 SoD：无论 approve 还是 reject）。
        assertThatThrownBy(() -> service.approveAdjustment(requested.id(),
                new com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest("adj-dec-sod-a", "ok", List.of(), "approve")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("SELF_APPROVAL_FORBIDDEN");
        assertThatThrownBy(() -> service.approveAdjustment(requested.id(),
                new com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest("adj-dec-sod-r", "no", List.of(), "reject")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("SELF_APPROVAL_FORBIDDEN");
        // 两次自决策均被拒，金额未变。
        assertThat(financeRepository.findArApItemById(item.id()).orElseThrow().amountCent()).isEqualTo(100000L);
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
        // F-OPS-07 SoD（A 版）拦 approve+reject：拒绝人也必须不同于发起人。
        stubActor("staff_finance_2_reviewer");
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

    // ─── P1#13 库存差异 → AR/AP（R6-D08）──────────────────────────────────────

    @Test
    void approvedShortageVarianceIsPostedAsInternalAccountingCost() {
        financeRepository.seedVariance("var_shortage", "shortage", -50000L, "approved", null);

        ArApItemRecord item = service.postInventoryVariance(new com.complex.platform.finance.dto.InventoryVariancePostRequest(
                "post-shortage", "note", List.of(), "var_shortage", "loss_account"));

        assertThat(item.sourceType()).isEqualTo("inventory_difference");
        assertThat(item.direction()).isEqualTo("internal_accounting"); // C3：盘亏/报损 → 内部核算损耗成本
        assertThat(item.status()).isEqualTo("settled");                // 内部核算非待收/待付
        assertThat(item.unpaidAmountCent()).isEqualTo(0L);
        assertThat(item.amountCent()).isEqualTo(50000L);               // 取绝对值
        assertThat(item.sourceId()).isEqualTo("var_shortage");
        assertThat(financeRepository.findInventoryVarianceById("var_shortage").orElseThrow().postedArApItemId())
                .isEqualTo(item.id());
    }

    @Test
    void supplierFaultVarianceIsPostedAsPayableToSupplier() {
        financeRepository.seedVariance("var_sf", "supplier_fault", -30000L, "approved", "supplier_x");

        ArApItemRecord item = service.postInventoryVariance(new com.complex.platform.finance.dto.InventoryVariancePostRequest(
                "post-sf", "note", List.of(), "var_sf", "supplier_x"));

        assertThat(item.direction()).isEqualTo("payable");         // 供应商责任 → 向供应商挂应付索赔
        assertThat(item.counterpartyId()).isEqualTo("supplier_x");
        assertThat(item.amountCent()).isEqualTo(30000L);
    }

    @Test
    void overageVarianceIsPostedAsPayableCostReversal() {
        financeRepository.seedVariance("var_over", "overage", 12000L, "approved", null);

        ArApItemRecord item = service.postInventoryVariance(new com.complex.platform.finance.dto.InventoryVariancePostRequest(
                "post-over", "note", List.of(), "var_over", "cost_account"));

        assertThat(item.direction()).isEqualTo("payable");         // 盘盈冲成本 → payable
        assertThat(item.amountCent()).isEqualTo(12000L);
    }

    @Test
    void unreviewedVarianceCannotBePosted() {
        financeRepository.seedVariance("var_pending", "shortage", -1000L, "pending", null);

        assertThatThrownBy(() -> service.postInventoryVariance(new com.complex.platform.finance.dto.InventoryVariancePostRequest(
                "post-pending", "note", List.of(), "var_pending", "loss_account")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVENTORY_VARIANCE_NOT_APPROVED");
    }

    @Test
    void postingVarianceTwiceIsIdempotentReturningSameItem() {
        financeRepository.seedVariance("var_idem", "shortage", -7000L, "approved", null);

        ArApItemRecord first = service.postInventoryVariance(new com.complex.platform.finance.dto.InventoryVariancePostRequest(
                "post-idem-1", "note", List.of(), "var_idem", "loss_account"));
        // 不同幂等键再次提交同一已入账差异 → 返回原台账项，不重复入账。
        ArApItemRecord second = service.postInventoryVariance(new com.complex.platform.finance.dto.InventoryVariancePostRequest(
                "post-idem-2", "note", List.of(), "var_idem", "loss_account"));

        assertThat(second.id()).isEqualTo(first.id());
    }

    // ─── P1 并发数据一致性红线（FOR UPDATE / 条件 UPDATE + 行数校验）────────────

    @Test
    void concurrentDoublePostInventoryVarianceOnlyCreatesOneLedgerItem() {
        // 缺口②：并发双开台账。两个请求都越过 postedArApItemId==null 的幂等门，争相条件回填。
        // 期望：先到者成功，后到者 markInventoryVariancePostedIfUnposted 命中 0 行 → 抛 ALREADY_POSTED；最终仅 1 条应付台账。
        financeRepository.seedVariance("var_race", "supplier_fault", -40000L, "approved", "supplier_y");

        // 模拟两个并发请求都已读到 postedArApItemId==null（postInventoryVarianceInternal 前半段），各自生成台账后回填。
        // 第一次：正常成功。
        ArApItemRecord first = service.postInventoryVariance(new com.complex.platform.finance.dto.InventoryVariancePostRequest(
                "post-race-1", "note", List.of(), "var_race", "supplier_y"));
        assertThat(first.id()).isNotNull();

        // 第二个并发请求：强制绕过早期幂等门（直接调用条件回填验证红线 —— 已 posted 故命中 0 行返回 false）。
        boolean secondWins = financeRepository.markInventoryVariancePostedIfUnposted("var_race", "would_be_duplicate_item");
        assertThat(secondWins).isFalse();

        // 应付台账仅生成 1 条（来自 inventory_difference）。
        long ledgerItems = financeRepository.listArApItems().stream()
                .filter(i -> "inventory_difference".equals(i.sourceType()) && "var_race".equals(i.sourceId()))
                .count();
        assertThat(ledgerItems).isEqualTo(1L);
    }

    @Test
    void depositActionReadsBalanceUnderRowLockAndRejectsOverdraw() {
        // 缺口①：保证金余额丢更新。校验余额读取走加锁路径（currentDepositBalanceForUpdate），
        // 且锁内超额 deduct 被拒（balance 不可为负）。
        service.recordDepositAction(new com.complex.platform.finance.dto.DepositActionRequest(
                "dep-lock-c", "note", List.of(), "merchant_lock", "collect", 30000L, "init"));

        // 锁内读取的余额=30000，deduct 50000 超额 → 拒绝。
        assertThatThrownBy(() -> service.recordDepositAction(new com.complex.platform.finance.dto.DepositActionRequest(
                "dep-lock-over", "note", List.of(), "merchant_lock", "deduct", 50000L, "too much")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DEPOSIT_INSUFFICIENT");

        // 验证服务确实走了加锁读取路径。
        assertThat(financeRepository.forUpdateBalanceReads).contains("merchant_lock");
    }

    @Test
    void refundLocksTargetPaymentBeforeCumulativeCheck() {
        // 缺口③：退款累计 TOCTOU。校验退款前对 target payment 加 FOR UPDATE，且累计校验仍拒绝超额。
        ArApItemRecord item = service.createArApItem(itemRequest("arap-lock-refund", "receivable", 20000L));
        ArApPaymentResponse payment = service.registerArApPayment(item.id(), paymentRequest("pay-lock-refund", 20000L));
        String paymentId = payment.payment().id();

        // 第一笔退 12000（累计 12000 ≤ 20000）通过。
        service.createRefund(new RefundRequest("refund-lock-1", "refund", List.of(), paymentId, 12000L, "part1", null, null));
        // 第二笔退 12000（累计 24000 > 20000）被拒。
        assertThatThrownBy(() -> service.createRefund(new RefundRequest(
                "refund-lock-2", "refund", List.of(), paymentId, 12000L, "part2", null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        // 验证退款确实对 target payment 走了加锁读取路径。
        assertThat(financeRepository.forUpdatePaymentReads).contains(paymentId);
    }

    // ─── P1#14 营业额抽成计费引擎 ─────────────────────────────────────────────

    @Test
    void turnoverRentClampsBetweenFloorAndCap() {
        // 扣率 5%（500 bps），保底 10000，封顶 80000。
        financeRepository.seedTurnoverRule("rule_1", "merchant_1", 500, 10000L, 80000L);
        // 回流营业额 1,000,000 → 5% = 50000，落在 [10000, 80000] → 50000。
        financeRepository.reflowTurnoverByMerchant.put("merchant_1", 1000000L);

        ArApItemRecord item = service.billTurnoverRent(new com.complex.platform.finance.dto.TurnoverRentBillRequest(
                "bill-1", "note", List.of(), "rule_1",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30"), LocalDate.parse("2026-07-10")));

        assertThat(item.sourceType()).isEqualTo("turnover_rent");
        assertThat(item.direction()).isEqualTo("receivable");
        assertThat(item.amountCent()).isEqualTo(50000L);
    }

    @Test
    void turnoverRentStampsAccountingPeriodWithPeriodStartMonth() {
        // V299：营业额抽成应收落 accounting_period=期间起始月（YearMonth 打戳）——
        // 租金自动挂接（费用台账→分红基数）按 accounting_period 归期，缺戳=漏挂=分红基数虚高。
        financeRepository.seedTurnoverRule("rule_period", "merchant_1", 500, 10000L, 80000L);
        financeRepository.reflowTurnoverByMerchant.put("merchant_1", 1000000L);

        ArApItemRecord item = service.billTurnoverRent(new com.complex.platform.finance.dto.TurnoverRentBillRequest(
                "bill-period", "note", List.of(), "rule_period",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30"), LocalDate.parse("2026-07-10")));

        assertThat(item.accountingPeriod()).isEqualTo("2026-06");
    }

    @Test
    void turnoverRentAppliesFloorWhenComputedBelowMinimum() {
        financeRepository.seedTurnoverRule("rule_floor", "merchant_1", 500, 10000L, 80000L);
        // 营业额 100,000 → 5% = 5000 < 保底 10000 → clamp 到 10000。
        financeRepository.reflowTurnoverByMerchant.put("merchant_1", 100000L);

        ArApItemRecord item = service.billTurnoverRent(new com.complex.platform.finance.dto.TurnoverRentBillRequest(
                "bill-floor", "note", List.of(), "rule_floor",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30"), LocalDate.parse("2026-07-10")));

        assertThat(item.amountCent()).isEqualTo(10000L);
    }

    @Test
    void turnoverRentAppliesCapWhenComputedAboveMaximum() {
        financeRepository.seedTurnoverRule("rule_cap", "merchant_1", 500, 10000L, 80000L);
        // 营业额 5,000,000 → 5% = 250000 > 封顶 80000 → clamp 到 80000。
        financeRepository.reflowTurnoverByMerchant.put("merchant_1", 5000000L);

        ArApItemRecord item = service.billTurnoverRent(new com.complex.platform.finance.dto.TurnoverRentBillRequest(
                "bill-cap", "note", List.of(), "rule_cap",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30"), LocalDate.parse("2026-07-10")));

        assertThat(item.amountCent()).isEqualTo(80000L);
    }

    // ─── P0-E（v7 审计）抽成规则草案闸 ────────────────────────────────────────

    private com.complex.platform.finance.dto.TurnoverRentRuleRequest turnoverRuleRequest(String idempotencyKey) {
        return new com.complex.platform.finance.dto.TurnoverRentRuleRequest(
                idempotencyKey, "note", List.of(), "ct_p0e", "merchant_1", "阶梯提成规则",
                10000L, LocalDate.parse("2026-08-01"), null,
                List.of(new com.complex.platform.finance.dto.TurnoverRentRuleRequest.TierInput(null, 500)));
    }

    @Test
    void p0eTurnoverRentRuleLandsAsDraftAndCannotBeBilledBeforeActivation() {
        TurnoverRentRuleRecord rule = service.createTurnoverRentRule(turnoverRuleRequest("trr-p0e-draft"));

        // 建规则只落草案（不进结算）。
        assertThat(financeRepository.isTurnoverRentRuleDraft(rule.id())).isTrue();

        financeRepository.reflowTurnoverByMerchant.put("merchant_1", 1000000L);
        // 草案规则不得计费 —— 单人改动不经转正绝不生效。
        assertThatThrownBy(() -> service.billTurnoverRent(new com.complex.platform.finance.dto.TurnoverRentBillRequest(
                "bill-p0e-draft", "note", List.of(), rule.id(),
                LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-31"), LocalDate.parse("2026-09-10"))))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("TURNOVER_RENT_RULE_NOT_ACTIVATED");
    }

    @Test
    void p0eTurnoverRentRuleBillsOnlyAfterActivation() {
        TurnoverRentRuleRecord rule = service.createTurnoverRentRule(turnoverRuleRequest("trr-p0e-activated"));
        financeRepository.reflowTurnoverByMerchant.put("merchant_1", 1000000L);

        // 复核转正后（配置中心 activateDraft），同一规则才可计费：1,000,000 × 5% = 50000，高于保底 10000。
        financeRepository.activateTurnoverRule(rule.id());
        ArApItemRecord item = service.billTurnoverRent(new com.complex.platform.finance.dto.TurnoverRentBillRequest(
                "bill-p0e-activated", "note", List.of(), rule.id(),
                LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-31"), LocalDate.parse("2026-09-10")));

        assertThat(item.sourceType()).isEqualTo("turnover_rent");
        assertThat(item.amountCent()).isEqualTo(50000L);
    }

    @Test
    void p0eArchivedTurnoverRentRuleCannotBeBilled() {
        // 软删（下线）后的规则不得计费。
        financeRepository.turnoverRentRules.put("rule_archived", new TurnoverRentRuleRecord(
                "rule_archived", "ct_p0e", "merchant_1", "已下线规则", 500, 10000L, null,
                LocalDate.parse("2026-01-01"), null, "inactive", Instant.now()));
        financeRepository.reflowTurnoverByMerchant.put("merchant_1", 1000000L);

        assertThatThrownBy(() -> service.billTurnoverRent(new com.complex.platform.finance.dto.TurnoverRentBillRequest(
                "bill-p0e-archived", "note", List.of(), "rule_archived",
                LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-31"), LocalDate.parse("2026-09-10"))))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("TURNOVER_RENT_RULE_NOT_ACTIVE");
    }

    // ─── P1#14 保证金四动作台账 ───────────────────────────────────────────────

    @Test
    void depositFourActionsMaintainRunningBalance() {
        // collect 100000 → 余额 100000
        DepositLedgerRecord collect = service.recordDepositAction(new com.complex.platform.finance.dto.DepositActionRequest(
                "dep-collect", "note", List.of(), "merchant_1", "collect", 100000L, "initial"));
        assertThat(collect.balanceAfterCent()).isEqualTo(100000L);
        assertThat(collect.arApItemId()).isNull();

        // deduct 20000 → 余额 80000，联动生成应收。
        DepositLedgerRecord deduct = service.recordDepositAction(new com.complex.platform.finance.dto.DepositActionRequest(
                "dep-deduct", "note", List.of(), "merchant_1", "deduct", 20000L, "damage charge"));
        assertThat(deduct.balanceAfterCent()).isEqualTo(80000L);
        assertThat(deduct.arApItemId()).isNotNull();

        // forfeit 30000 → 余额 50000。
        DepositLedgerRecord forfeit = service.recordDepositAction(new com.complex.platform.finance.dto.DepositActionRequest(
                "dep-forfeit", "note", List.of(), "merchant_1", "forfeit", 30000L, "breach"));
        assertThat(forfeit.balanceAfterCent()).isEqualTo(50000L);

        // refund 50000 → 余额 0。
        DepositLedgerRecord refund = service.recordDepositAction(new com.complex.platform.finance.dto.DepositActionRequest(
                "dep-refund", "note", List.of(), "merchant_1", "refund", 50000L, "lease end"));
        assertThat(refund.balanceAfterCent()).isZero();
        assertThat(refund.arApItemId()).isNull();
    }

    @Test
    void depositRefundExceedingBalanceIsRejected() {
        service.recordDepositAction(new com.complex.platform.finance.dto.DepositActionRequest(
                "dep-c", "note", List.of(), "merchant_1", "collect", 10000L, "init"));

        assertThatThrownBy(() -> service.recordDepositAction(new com.complex.platform.finance.dto.DepositActionRequest(
                "dep-over", "note", List.of(), "merchant_1", "refund", 20000L, "too much")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DEPOSIT_INSUFFICIENT");
    }

    @Test
    void createDepositRefundPayablePostsPayableAndWritesLedgerRefundDecrementingBalance() {
        // #3② 根治：退押金写 deposit_ledger refund 行扣减余额 → 同商户第二笔退款按已减余额封顶，不超退。
        service.recordDepositAction(new com.complex.platform.finance.dto.DepositActionRequest(
                "dep-init2", "note", List.of(), "merchant_ref", "collect", 40000L, "init"));   // 余额 40000
        long refund1 = service.createDepositRefundPayable("merchant_ref", 30000L, "cst_ref_a");
        long refund2 = service.createDepositRefundPayable("merchant_ref", 30000L, "cst_ref_b");
        assertThat(refund1).isEqualTo(30000L);                 // 余额 40000，全退请求 30000
        assertThat(refund2).isEqualTo(10000L);                 // 余额只剩 10000 → 封顶 10000（非再退 30000）
        assertThat(refund1 + refund2).isEqualTo(40000L);       // 共退=实交，不超退
        // 第三笔余额已空 → 实退 0，不落账。
        assertThat(service.createDepositRefundPayable("merchant_ref", 5000L, "cst_ref_c")).isZero();
    }

    // ─── F-OPS-08 物业费固定计费（替代已废止 CAM 权重分摊 + 年度 true-up）──────────────

    @Test
    void propertyFeeBilledAtFixedRatePerSquareMeter() {
        // 费率 1000 分/㎡（10 元）；商户面积 120.50 ㎡ → 应收 = 1000 × 120.50 = 120500 分。
        service.createPropertyFeeRate(new PropertyFeeRateRequest(
                "pfr-1", "project_1", "2026-06", 1000L, "active"));
        financeRepository.seedMerchantArea("merchant_1", new java.math.BigDecimal("120.50"));

        List<PropertyFeeBillRecord> bills = service.billPropertyFee(new PropertyFeeBillRequest(
                "pf-bill-1", "note", List.of(), "project_1", "2026-06", List.of("merchant_1")));

        assertThat(bills).hasSize(1);
        PropertyFeeBillRecord bill = bills.get(0);
        assertThat(bill.amountCent()).isEqualTo(120500L);
        assertThat(bill.ratePerSquareMeterCent()).isEqualTo(1000L);
        assertThat(bill.arApItemId()).isNotNull();
        ArApItemRecord ar = financeRepository.findArApItemById(bill.arApItemId()).orElseThrow();
        assertThat(ar.direction()).isEqualTo("receivable");
        assertThat(ar.sourceType()).isEqualTo("property_fee");
        assertThat(ar.amountCent()).isEqualTo(120500L);
        assertThat(ar.counterpartyId()).isEqualTo("merchant_1");
    }

    @Test
    void propertyFeeBillingIsIdempotentAndNoRefundNoTopUp() {
        service.createPropertyFeeRate(new PropertyFeeRateRequest(
                "pfr-2", "project_1", "2026-06", 1000L, "active"));
        financeRepository.seedMerchantArea("merchant_1", new java.math.BigDecimal("100.00"));

        List<PropertyFeeBillRecord> first = service.billPropertyFee(new PropertyFeeBillRequest(
                "pf-bill-2a", "note", List.of(), "project_1", "2026-06", List.of("merchant_1")));
        assertThat(first).hasSize(1);
        assertThat(first.get(0).amountCent()).isEqualTo(100000L);

        // 费率事后上调，再以不同幂等键对同 (项目,期,商户) 计费 → 已计费跳过，不补不退、不二次挂账。
        service.createPropertyFeeRate(new PropertyFeeRateRequest(
                "pfr-2b", "project_1", "2026-06", 5000L, "active"));
        List<PropertyFeeBillRecord> second = service.billPropertyFee(new PropertyFeeBillRequest(
                "pf-bill-2b", "note", List.of(), "project_1", "2026-06", List.of("merchant_1")));
        assertThat(second).isEmpty();                                       // 已计费跳过
        assertThat(financeRepository.propertyFeeBillCount()).isEqualTo(1);  // 仍只有一笔
    }

    @Test
    void propertyFeeSkipsMerchantWithMissingArea() {
        service.createPropertyFeeRate(new PropertyFeeRateRequest(
                "pfr-3", "project_1", "2026-06", 1000L, "active"));
        // merchant_no_area 未登记面积 → 跳过；merchant_1 有面积 → 计费。
        financeRepository.seedMerchantArea("merchant_1", new java.math.BigDecimal("80.00"));

        List<PropertyFeeBillRecord> bills = service.billPropertyFee(new PropertyFeeBillRequest(
                "pf-bill-3", "note", List.of(), "project_1", "2026-06",
                List.of("merchant_no_area", "merchant_1")));

        assertThat(bills).hasSize(1);
        assertThat(bills.get(0).merchantId()).isEqualTo("merchant_1");
        assertThat(bills.get(0).amountCent()).isEqualTo(80000L);
    }

    @Test
    void propertyFeeRejectsWhenNoActiveRateConfigured() {
        financeRepository.seedMerchantArea("merchant_1", new java.math.BigDecimal("80.00"));
        assertThatThrownBy(() -> service.billPropertyFee(new PropertyFeeBillRequest(
                "pf-bill-4", "note", List.of(), "project_1", "2026-06", List.of("merchant_1"))))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("PROPERTY_FEE_RATE_NOT_FOUND");
    }

    @Test
    void propertyFeeExactPeriodRateOverridesProjectDefault() {
        // 默认费率 800 分/㎡（effectivePeriod=null）+ 2026-06 精确费率 1000 分/㎡。
        service.createPropertyFeeRate(new PropertyFeeRateRequest(
                "pfr-def", "project_1", null, 800L, "active"));
        service.createPropertyFeeRate(new PropertyFeeRateRequest(
                "pfr-jun", "project_1", "2026-06", 1000L, "active"));
        financeRepository.seedMerchantArea("merchant_1", new java.math.BigDecimal("10.00"));
        financeRepository.seedMerchantArea("merchant_2", new java.math.BigDecimal("10.00"));

        // 2026-06 命中精确费率 → 1000 × 10 = 10000。
        List<PropertyFeeBillRecord> jun = service.billPropertyFee(new PropertyFeeBillRequest(
                "pf-bill-jun", "note", List.of(), "project_1", "2026-06", List.of("merchant_1")));
        assertThat(jun.get(0).amountCent()).isEqualTo(10000L);

        // 2026-07 无精确费率 → 回退默认 800 × 10 = 8000。
        List<PropertyFeeBillRecord> jul = service.billPropertyFee(new PropertyFeeBillRequest(
                "pf-bill-jul", "note", List.of(), "project_1", "2026-07", List.of("merchant_2")));
        assertThat(jul.get(0).amountCent()).isEqualTo(8000L);
    }

    // ─── P1#15 日结两源非空 ───────────────────────────────────────────────────

    @Test
    void dailyCloseChannelTicketAndThirdpartySourcesAreNonEmpty() {
        // 接入真实数据源后，channel_ticket 与 thirdparty_reflow 两源可返回非零（此处经由聚合桩注入真实值，
        // 验证服务层把两源写入汇总且非空——JdbcFinanceRepository 已接入 ticket_orders/integration_order_syncs）。
        financeRepository.channelTicketTotals = new FinanceRepository.DailyCloseSourceTotals(220000L, 5000L, 7);
        financeRepository.thirdpartyTotals = new FinanceRepository.DailyCloseSourceTotals(330000L, 0L, 9);

        DailyCloseRecord close = service.createDailyClose(new DailyCloseRequest(
                "daily-close-two-src", "lock", List.of(),
                LocalDate.parse("2026-06-25"), "project", "project_two_src", false));

        List<DailyCloseSummaryRecord> rows = service.listDailyCloseSummaries(close.id()).items();
        assertThat(rows).filteredOn(s -> "channel_ticket".equals(s.sourceStream()))
                .extracting(DailyCloseSummaryRecord::amountCent, DailyCloseSummaryRecord::orderCount)
                .containsExactly(tuple(220000L, 7));
        assertThat(rows).filteredOn(s -> "thirdparty_reflow".equals(s.sourceStream()))
                .extracting(DailyCloseSummaryRecord::amountCent, DailyCloseSummaryRecord::orderCount)
                .containsExactly(tuple(330000L, 9));
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
        FinanceService svc = new FinanceService(financeRepository, ops, auditEventService, authorizationService,
                new com.complex.platform.common.BusinessDayResolver(4),
                new com.complex.platform.finance.gateway.PaymentGatewayRouter(terminalRepo));
        svc.refundAuthorizationService = new RefundAuthorizationService(permissiveRefundRepo());
        return svc;
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
                "refund-gw-off", "refund", List.of(), payment.payment().id(), 10000L, "refund", null, null));
        assertThat(refund.refundChannel()).isEqualTo("manual");
        assertThat(refund.status()).isEqualTo("requested");
    }

    @Test
    void paymentGatewayOn_refundDoesNotCrashAndStillRecorded() {
        FinanceService svc = financeWithGatewaySwitch(true);
        ArApItemRecord item = svc.createArApItem(itemRequest("arap-gw-refund-on", "receivable", 30000L));
        ArApPaymentResponse payment = svc.registerArApPayment(item.id(), paymentRequest("pay-gw-on", 30000L));
        FinanceRefundRecord refund = svc.createRefund(new RefundRequest(
                "refund-gw-on", "refund", List.of(), payment.payment().id(), 10000L, "refund", null, null));
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

        // V304 释放：re-tag source_type（前态守卫），内存版真改（默认接口实现是 no-op）。
        @Override
        public int retagArApItemSourceType(String id, String expectedSourceType, String newSourceType, Instant now) {
            ArApItemRecord cur = arApItems.get(id);
            if (cur == null || !expectedSourceType.equals(cur.sourceType())) {
                return 0;
            }
            arApItems.put(id, new ArApItemRecord(cur.id(), cur.itemNo(), cur.direction(), cur.counterpartyId(),
                    newSourceType, cur.sourceId(), cur.amountCent(), cur.paidAmountCent(), cur.unpaidAmountCent(),
                    cur.currency(), cur.dueDate(), cur.accountingPeriod(), cur.status(), cur.createdAt(), now,
                    cur.merchantId(), cur.projectId()));
            return 1;
        }

        @Override
        public ArApPaymentRecord saveArApPayment(ArApPaymentRecord payment) {
            // Mirror DO NOTHING semantics: only insert if the ID is genuinely new.
            arApPayments.putIfAbsent(payment.id(), payment);
            return arApPayments.get(payment.id());
        }

        int arApPaymentCount() {
            return arApPayments.size();
        }

        // 记录服务层是否走了 FOR UPDATE 加锁读取路径（P1 并发红线断言）。
        final List<String> forUpdatePaymentReads = new ArrayList<>();
        final List<String> forUpdateBalanceReads = new ArrayList<>();

        @Override
        public Optional<ArApPaymentRecord> findArApPaymentById(String id) {
            return Optional.ofNullable(arApPayments.get(id));
        }

        @Override
        public Optional<ArApPaymentRecord> findArApPaymentByIdForUpdate(String id) {
            forUpdatePaymentReads.add(id);
            return findArApPaymentById(id);
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
        public Optional<PaymentIntentRecord> findPaymentIntentById(String id) {
            return Optional.ofNullable(paymentIntents.get(id));
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

        @Override
        public java.util.Optional<ReconciliationDifferenceRecord> findReconciliationDifferenceById(String id) {
            return java.util.Optional.ofNullable(reconciliationDifferences.get(id));
        }

        @Override
        public ReconciliationDifferenceRecord closeReconciliationDifference(
                String id, String closeReason, String closedBy, java.time.Instant closedAt) {
            ReconciliationDifferenceRecord d = reconciliationDifferences.get(id);
            ReconciliationDifferenceRecord closed = new ReconciliationDifferenceRecord(
                    d.id(), d.batchId(), d.differenceType(), d.businessRef(), d.amountCent(),
                    d.expectedAmountCent(), d.actualAmountCent(), d.source(), "closed", d.createdAt());
            reconciliationDifferences.put(id, closed);
            return closed;
        }

        // P0#7：平台侧 expected 系统取数桩——key=business_ref → expected 金额（分）。无键=0（无系统取数支撑）。
        private final Map<String, Long> platformExpectedByRef = new LinkedHashMap<>();

        @Override
        public Optional<ReconciliationBatchRecord> findReconciliationBatchById(String id) {
            return Optional.ofNullable(reconciliationBatches.get(id));
        }

        @Override
        public long aggregatePlatformExpectedForBusinessRef(String businessRef) {
            return platformExpectedByRef.getOrDefault(businessRef, 0L);
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
        public boolean lockedDailyCloseExistsForDate(LocalDate businessDate, String merchantId) {
            // F-OPS-09：镜像 JdbcFinanceRepository 商户级口径——
            // merchantId 为空看任一 locked 日结；非空仅看 scope_type='merchant' AND scope_id=merchantId。
            return dailyCloses.values().stream()
                    .filter(dc -> businessDate.equals(dc.businessDate()) && "locked".equals(dc.status()))
                    .anyMatch(dc -> merchantId == null
                            || ("merchant".equals(dc.scopeType()) && merchantId.equals(dc.scopeId())));
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

        // ── P1#13 库存差异 → AR/AP ──────────────────────────────────────────
        private final Map<String, InventoryVarianceRecord> inventoryVariances = new LinkedHashMap<>();

        @Override
        public Optional<InventoryVarianceRecord> findInventoryVarianceById(String id) {
            return Optional.ofNullable(inventoryVariances.get(id));
        }

        @Override
        public boolean markInventoryVariancePostedIfUnposted(String inventoryVarianceId, String arApItemId) {
            // 模拟 SQL 的 WHERE ... AND posted_ar_ap_item_id IS NULL：已入账则命中 0 行返回 false。
            InventoryVarianceRecord v = inventoryVariances.get(inventoryVarianceId);
            if (v == null || v.postedArApItemId() != null) {
                return false;
            }
            inventoryVariances.put(inventoryVarianceId, new InventoryVarianceRecord(
                    v.id(), v.stocktakeId(), v.itemId(), v.batchNo(), v.bookQty(), v.countedQty(),
                    v.varianceQty(), v.varianceAmountCent(), v.reason(), v.supplierId(),
                    v.reviewStatus(), arApItemId, v.merchantId(), v.createdAt()));
            return true;
        }

        // ── P1#14 综合体计费：营业额抽成 / 保证金 / CAM ─────────────────────
        private final Map<String, TurnoverRentRuleRecord> turnoverRentRules = new LinkedHashMap<>();
        private final Map<String, Long> reflowTurnoverByMerchant = new LinkedHashMap<>();
        private final List<DepositLedgerRecord> depositLedger = new ArrayList<>();

        // P0-E：抽成规则草案闸——落草案的规则 id 记在这里，isTurnoverRentRuleDraft 据此判定（对齐真库 is_draft 列）。
        private final java.util.Set<String> draftTurnoverRuleIds = new java.util.LinkedHashSet<>();

        @Override
        public Optional<TurnoverRentRuleRecord> findTurnoverRentRuleById(String id) {
            return Optional.ofNullable(turnoverRentRules.get(id));
        }

        @Override
        public TurnoverRentRuleRecord saveTurnoverRentRule(TurnoverRentRuleRecord rule) {
            turnoverRentRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public TurnoverRentRuleRecord saveTurnoverRentRuleDraft(TurnoverRentRuleRecord rule, String updatedBy) {
            turnoverRentRules.put(rule.id(), rule);
            draftTurnoverRuleIds.add(rule.id());
            return rule;
        }

        @Override
        public boolean isTurnoverRentRuleDraft(String id) {
            return draftTurnoverRuleIds.contains(id);
        }

        /** 测试用：模拟配置中心复核转正（草案翻生效）。 */
        void activateTurnoverRule(String id) {
            draftTurnoverRuleIds.remove(id);
        }

        // 阶梯档位：真库里挂 turnover_rent_rule_tiers，双子里按 ruleId 分组存，令阶梯计费口径可被单测覆盖。
        private final Map<String, List<TurnoverRentRuleTierRecord>> turnoverRentRuleTiers = new LinkedHashMap<>();

        @Override
        public TurnoverRentRuleTierRecord saveTurnoverRentRuleTier(TurnoverRentRuleTierRecord tier) {
            turnoverRentRuleTiers.computeIfAbsent(tier.ruleId(), key -> new ArrayList<>()).add(tier);
            return tier;
        }

        @Override
        public List<TurnoverRentRuleTierRecord> listTurnoverRentRuleTiers(String ruleId) {
            return List.copyOf(turnoverRentRuleTiers.getOrDefault(ruleId, List.of()));
        }

        @Override
        public long aggregateMerchantReflowTurnover(String merchantId, Instant fromInclusive, Instant toExclusive) {
            return reflowTurnoverByMerchant.getOrDefault(merchantId, 0L);
        }

        @Override
        public DepositLedgerRecord saveDepositLedger(DepositLedgerRecord ledger) {
            depositLedger.add(ledger);
            return ledger;
        }

        @Override
        public long currentDepositBalance(String merchantId) {
            long balance = 0L;
            for (DepositLedgerRecord row : depositLedger) {
                if (merchantId.equals(row.merchantId())) {
                    balance = row.balanceAfterCent();
                }
            }
            return balance;
        }

        @Override
        public long currentDepositBalanceForUpdate(String merchantId) {
            forUpdateBalanceReads.add(merchantId);
            return currentDepositBalance(merchantId);
        }

        // ── F-OPS-08 物业费固定计费 ─────────────────────────────────────────────
        private final Map<String, PropertyFeeRateRecord> propertyFeeRates = new LinkedHashMap<>();
        private final Map<String, java.math.BigDecimal> merchantAreas = new LinkedHashMap<>();
        private final List<PropertyFeeBillRecord> propertyFeeBills = new ArrayList<>();

        @Override
        public PropertyFeeRateRecord savePropertyFeeRate(PropertyFeeRateRecord rate) {
            // 以 (项目, 期或默认) 为键 upsert，模拟唯一索引 / ON CONFLICT。
            propertyFeeRates.put(rate.projectId() + "|"
                    + (rate.effectivePeriod() == null ? "__default__" : rate.effectivePeriod()), rate);
            return rate;
        }

        @Override
        public Optional<PropertyFeeRateRecord> findEffectivePropertyFeeRate(String projectId, String period) {
            // 精确期命中优先，否则回退该项目默认费率；仅取 active。
            PropertyFeeRateRecord exact = propertyFeeRates.get(projectId + "|" + period);
            if (exact != null && "active".equals(exact.status())) {
                return Optional.of(exact);
            }
            PropertyFeeRateRecord dft = propertyFeeRates.get(projectId + "|__default__");
            if (dft != null && "active".equals(dft.status())) {
                return Optional.of(dft);
            }
            return Optional.empty();
        }

        @Override
        public List<PropertyFeeRateRecord> listPropertyFeeRates(String projectId) {
            List<PropertyFeeRateRecord> out = new ArrayList<>();
            for (PropertyFeeRateRecord r : propertyFeeRates.values()) {
                if (projectId.equals(r.projectId())) {
                    out.add(r);
                }
            }
            return out;
        }

        @Override
        public java.math.BigDecimal findMerchantAreaSquareMeter(String merchantId) {
            return merchantAreas.get(merchantId);
        }

        @Override
        public boolean propertyFeeBillExists(String projectId, String period, String merchantId) {
            return propertyFeeBills.stream().anyMatch(b ->
                    b.projectId().equals(projectId) && b.period().equals(period) && b.merchantId().equals(merchantId));
        }

        @Override
        public PropertyFeeBillRecord savePropertyFeeBill(PropertyFeeBillRecord bill) {
            // 模拟 ON CONFLICT(project_id,period,merchant_id) DO NOTHING。
            if (!propertyFeeBillExists(bill.projectId(), bill.period(), bill.merchantId())) {
                propertyFeeBills.add(bill);
            }
            return bill;
        }

        void seedMerchantArea(String merchantId, java.math.BigDecimal area) {
            merchantAreas.put(merchantId, area);
        }

        int propertyFeeBillCount() {
            return propertyFeeBills.size();
        }

        // 测试种子辅助。
        void seedVariance(String id, String reason, long varianceAmountCent, String reviewStatus, String supplierId) {
            inventoryVariances.put(id, new InventoryVarianceRecord(
                    id, "st_" + id, "item_" + id, null,
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.valueOf(varianceAmountCent),
                    varianceAmountCent, reason, supplierId, reviewStatus, null, null, Instant.now()));
        }

        void seedTurnoverRule(String id, String merchantId, int rateBps, Long minAmountCent, Long maxAmountCent) {
            turnoverRentRules.put(id, new TurnoverRentRuleRecord(
                    id, null, merchantId, "rule " + id, rateBps, minAmountCent, maxAmountCent,
                    LocalDate.parse("2026-01-01"), null, "active", Instant.now()));
        }
    }
}
