package com.complex.platform.finance;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.finance.dto.*;
import com.complex.platform.finance.gateway.*;
import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FinanceRefundLifecycleTests {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path proofDirectory;
    private final FinanceRepository repository = mock(FinanceRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final PaymentGatewayRouter gateway = mock(PaymentGatewayRouter.class);
    private final Map<String, FinanceRefundRecord> refunds = new LinkedHashMap<>();
    private final List<FinanceRefundAttemptRecord> attempts = new ArrayList<>();
    private final Set<String> ready = new HashSet<>();
    private FinanceService service;

    @BeforeEach
    void setUp() {
        AuditEventService audit = new AuditEventService(new InMemoryAuditEventRepository());
        service = new FinanceService(repository,
                new AuditedOperationExecutor(new IdempotencyService(
                        new InMemoryIdempotencyRepository(), new ObjectMapper()), audit),
                audit, authorization, new BusinessDayResolver(4), gateway);
        service.setRefundTransactionManager(mock(org.springframework.transaction.PlatformTransactionManager.class));
        service.refundAuthorizationService = mock(RefundAuthorizationService.class);
        service.refundEvidenceValidator = mock(EvidenceAttachmentValidator.class);
        actor("finance");
        when(repository.saveRefund(any())).thenAnswer(invocation -> {
            FinanceRefundRecord refund = invocation.getArgument(0);
            refunds.put(refund.id(), refund);
            return refund;
        });
        when(repository.findRefundById(anyString(), anyBoolean())).thenAnswer(i -> Optional.ofNullable(refunds.get(i.getArgument(0))));
        doAnswer(i -> {
            FinanceRefundAttemptRecord attempt = i.getArgument(0);
            attempts.add(attempt);
            if (Set.of("submit", "retry").contains(attempt.operation())) ready.add(attempt.id());
            return null;
        }).when(repository).appendRefundAttempt(any());
        when(repository.listRefundAttempts(anyString())).thenAnswer(i -> attempts.stream()
                .filter(a -> a.refundId().equals(i.getArgument(0))).toList());
        when(repository.findReadyRefundAttempt(anyString())).thenAnswer(i -> attempts.stream()
                .filter(a -> a.refundId().equals(i.getArgument(0)) && ready.contains(a.id())).findFirst());
        when(repository.claimRefundAttempt(anyString())).thenAnswer(i -> ready.remove(i.getArgument(0)));
        when(repository.sumRefundedAmountByTargetId(anyString())).thenAnswer(i -> refunds.values().stream()
                .filter(r -> r.targetId().equals(i.getArgument(0))).mapToLong(FinanceRefundRecord::refundAmountCent).sum());
        when(repository.findArApPaymentByIdForUpdate("payment")).thenReturn(Optional.of(
                new ArApPaymentRecord("payment", "item", 10000, "wechat",
                        Instant.parse("2026-10-01T00:00:00Z"), "original-bank-flow",
                        Instant.parse("2026-10-01T00:00:00Z"))));
        when(repository.findArApPaymentById("payment")).thenAnswer(i -> repository.findArApPaymentByIdForUpdate("payment"));
        when(repository.findArApItemByIdForUpdate("item")).thenAnswer(i -> repository.findArApItemById("item"));
        when(repository.sumSucceededRefundAmountByTargetId(anyString())).thenAnswer(i -> refunds.values().stream()
                .filter(r -> r.targetId().equals(i.getArgument(0)) && "success".equals(r.status()))
                .mapToLong(FinanceRefundRecord::refundAmountCent).sum());
        itemDirection("receivable");
        when(gateway.refund(any())).thenReturn(PaymentGatewayResult.reserved("渠道尚未接入"));
    }

    private void actor(String id) {
        // FS-021-R6：共享财务退款按「可触及项目 + 商户交集」办理；本类沿用 currentMerchantScope / currentProjectScopeQuietly
        //   两个桩表达操作人范围——商户账号的商户号随桩变化，可触及项目：有项目桩＝只到该项目，商户账号＝只到本类款项所在项目，
        //   否则为平台口径。
        doAnswer(i -> new AccessActor(id, id, "staff", "org_1", authorization.currentMerchantScope().orElse(null), "active"))
                .when(authorization).currentActor();
        doAnswer(i -> authorization.currentMerchantScope()).when(authorization).currentMerchantIdQuietly();
        doAnswer(i -> authorization.currentProjectScopeQuietly()
                .map(AuthorizationService.ProjectReach::ownOnly)
                .orElseGet(() -> authorization.currentMerchantScope().isPresent()
                        ? AuthorizationService.ProjectReach.ownOnly("project")
                        : new AuthorizationService.ProjectReach(true, true, null, false, Set.of())))
                .when(authorization).projectReach(any(), any(String[].class));
    }

    private void itemDirection(String direction) {
        Instant now = Instant.parse("2026-10-01T00:00:00Z");
        when(repository.findArApItemById("item")).thenReturn(Optional.of(
                new ArApItemRecord("item", "AR-1", direction, "customer", "test", "source",
                        10000, 10000, 0, "CNY", LocalDate.of(2026, 10, 1), "2026-10",
                        "settled", now, now, "merchant", "project")));
    }

    private RefundRequest request() {
        return new RefundRequest("refund-one", "退款", List.of(),
                "payment", 3000L, "批准退回原收款", null, null);
    }

    private RefundOperationRequest operation(String key) {
        return new RefundOperationRequest(key, "核查同一笔退款", List.of(), null);
    }

    private PaymentGatewayResult result(PaymentGatewayResult.Status status, PaymentGatewayRequest request) {
        return new PaymentGatewayResult(status, "wechat", "refund-bank-flow", "渠道结果", null, request);
    }

    private RefundExecutionRequest execution(String key) {
        return new RefundExecutionRequest(key, "本人已执行原路退款", List.of("evidence"), "execution-ref", null);
    }

    private RefundResultConfirmRequest confirmation(String key, String operator) {
        return new RefundResultConfirmRequest(key, "核实同笔原路退款成功", List.of("evidence"),
                "success", 3000L, "execution-ref", operator, null);
    }

    @Test
    void payableCannotBecomeAnotherOutgoingRefund() {
        itemDirection("payable");
        assertThatThrownBy(() -> service.createRefund(request())).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("REFUND_REQUIRES_RECEIVABLE");
        verifyNoInteractions(gateway);
    }

    @Test
    void gatewayFailureRemainsFailedAndUnsettled() {
        when(gateway.refund(any())).thenAnswer(i -> result(PaymentGatewayResult.Status.FAILED, i.getArgument(0)));
        FinanceRefundRecord refund = service.createRefund(request());
        assertThat(refund.status()).isEqualTo("failed");
        assertThat(refund.succeededAt()).isNull();
        assertThat(repository.sumRefundedAmountByTargetId("payment")).isEqualTo(3000);
        assertThatThrownBy(() -> service.createRefund(new RefundRequest("too-much", "退款", List.of(),
                "payment", 8000L, "重复申请", null, null))).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs = {-1, 10001})
    void invalidStoredRefundOccupancyCannotBeUsedForAnotherPayment(long occupied) {
        when(repository.sumRefundedAmountByTargetId("payment")).thenReturn(occupied);
        assertThatThrownBy(() -> service.createRefund(request())).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("VALIDATION_FAILED");
        verifyNoInteractions(gateway);
    }

    @Test
    void hugeRefundAmountCannotOverflowExistingOutstandingAmountCheck() {
        service.createRefund(request());
        assertThatThrownBy(() -> service.createRefund(new RefundRequest("overflow", "退款", List.of(),
                "payment", Long.MAX_VALUE, "超大金额", null, null))).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("VALIDATION_FAILED");
        assertThat(repository.sumRefundedAmountByTargetId("payment")).isEqualTo(3000);
        verify(gateway, times(1)).refund(any());
    }

    @Test
    void originalPaymentChannelAndReferenceReachGateway() {
        service.createRefund(request());
        ArgumentCaptor<PaymentGatewayRequest> captor = ArgumentCaptor.forClass(PaymentGatewayRequest.class);
        verify(gateway).refund(captor.capture());
        assertThat(captor.getValue().channel()).isEqualTo("wechat");
        assertThat(captor.getValue().gatewayRef()).isEqualTo("original-bank-flow");
        assertThat(captor.getValue().businessOrderId()).startsWith("refund_attempt_");
    }

    @Test
    void reservedGatewayDoesNotMarkRefundAsPaid() {
        assertThat(service.createRefund(request()).status()).isEqualTo("requested");
    }

    @Test
    void pendingRefundCannotBeRetriedOrManuallyPaidAgain() {
        when(gateway.refund(any())).thenAnswer(i -> result(PaymentGatewayResult.Status.PENDING, i.getArgument(0)));
        FinanceRefundRecord refund = service.createRefund(request());
        assertThatThrownBy(() -> service.retryFinanceRefund(refund.id(), operation("retry"))).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("REFUND_NOT_RETRYABLE");
        assertThatThrownBy(() -> service.recordFinanceRefundExecution(refund.id(), execution("manual"))).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("REFUND_NOT_RETRYABLE");
        verify(gateway, times(1)).refund(any());
    }

    @Test
    void timeoutWithoutChannelReferenceCanBeQueriedByPersistedAttemptWithoutRepaying() {
        when(gateway.refund(any())).thenThrow(new IllegalStateException("网络超时"));
        FinanceRefundRecord pending = service.createRefund(request());
        assertThat(pending.status()).isEqualTo("processing");
        assertThat(pending.gatewayRef()).isNull();
        when(gateway.queryRefund(any(), isNull())).thenAnswer(i -> result(PaymentGatewayResult.Status.SUCCEEDED, i.getArgument(0)));
        FinanceRefundRecord success = service.queryFinanceRefund(pending.id(), operation("query"));
        assertThat(success.status()).isEqualTo("success");
        assertThat(success.succeededAt()).isNotNull();
        verify(gateway, times(1)).refund(any());
        verify(gateway, never()).queryStatus(any());
    }

    @Test
    void failedRetryUsesSameRefundAndNewAttemptAndReplayDoesNotPayTwice() {
        when(gateway.refund(any())).thenAnswer(i -> result(PaymentGatewayResult.Status.FAILED, i.getArgument(0)));
        FinanceRefundRecord failed = service.createRefund(request());
        when(gateway.refund(any())).thenAnswer(i -> result(PaymentGatewayResult.Status.SUCCEEDED, i.getArgument(0)));
        FinanceRefundRecord retried = service.retryFinanceRefund(failed.id(), operation("retry"));
        service.retryFinanceRefund(failed.id(), operation("retry"));
        assertThat(retried.id()).isEqualTo(failed.id());
        assertThat(retried.status()).isEqualTo("success");
        assertThat(refunds).hasSize(1);
        assertThat(attempts.stream().filter(a -> Set.of("submit", "retry").contains(a.operation())))
                .extracting(FinanceRefundAttemptRecord::id).doesNotHaveDuplicates().hasSize(2);
        verify(gateway, times(2)).refund(any());
    }

    @Test
    void mismatchedGatewaySuccessCannotCloseRefundOrPoisonReference() {
        when(gateway.refund(any())).thenAnswer(i -> result(PaymentGatewayResult.Status.SUCCEEDED,
                PaymentGatewayRequest.of("finance_refund", "someone-else", 3000, "wechat")));
        FinanceRefundRecord refund = service.createRefund(request());
        assertThat(refund.status()).isEqualTo("processing");
        assertThat(refund.gatewayRef()).isNull();
        assertThat(refund.succeededAt()).isNull();
    }

    @Test
    void manualResultRequiresActualOperatorExecutionAndDifferentVerifier() {
        FinanceRefundRecord refund = service.createRefund(request());
        assertThatThrownBy(() -> service.confirmFinanceRefundResult(refund.id(), confirmation("missing", "made-up")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_EXECUTION_MISMATCH");
        service.recordFinanceRefundExecution(refund.id(), execution("execute"));
        assertThatThrownBy(() -> service.confirmFinanceRefundResult(refund.id(), confirmation("self", "finance")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_SELF_CONFIRM_FORBIDDEN");
        actor("reviewer");
        assertThatThrownBy(() -> service.confirmFinanceRefundResult(refund.id(), confirmation("fake", "made-up")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_EXECUTION_MISMATCH");
        assertThat(service.confirmFinanceRefundResult(refund.id(), confirmation("correct", "finance")).status())
                .isEqualTo("success");
    }

    @Test
    void crossMerchantAndProjectCannotReadRefundOrConfirmItsResult() {
        FinanceRefundRecord refund = service.createRefund(request());
        when(authorization.currentMerchantScope()).thenReturn(Optional.of("other-merchant"));
        assertThatThrownBy(() -> service.getFinanceRefund(refund.id())).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
        when(authorization.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("other-project"));
        assertThatThrownBy(() -> service.recordFinanceRefundExecution(refund.id(), execution("cross")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CROSS_PROJECT_FORBIDDEN");
    }

    @Test
    void storedValuePayoutCannotBeAutomaticallyRetriedWithoutRechargeRoutes() {
        String id = service.recordStoredValuePayoutRefund("account", 3000, "退卡", "merchant", "project", "stored");
        assertThat(refunds.get(id).sourceType()).isEqualTo("stored_value_account");
        assertThat(refunds.get(id).projectId()).isEqualTo("project");
        assertThatThrownBy(() -> service.retryFinanceRefund(id, operation("retry-stored")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_ROUTE_UNAVAILABLE");
        verifyNoInteractions(gateway);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"requested", "processing", "failed"})
    void earlierPaymentMustWaitForLaterPaymentActualRefundSuccess(String status) {
        Instant now = Instant.parse("2026-10-02T00:00:00Z");
        ArApPaymentRecord later = new ArApPaymentRecord("later", "item", 1000, "wechat", now, "later-flow", now);
        ArApPaymentRecord first = repository.findArApPaymentByIdForUpdate("payment").orElseThrow();
        when(repository.listArApPayments("item")).thenReturn(List.of(later, first));
        refunds.put("refund_later", new FinanceRefundRecord("refund_later", "later", 1000, "待退",
                "wechat", "original", status, now, "ar_ap_payment", "merchant", "project",
                "finance", "later-flow", null, null, now, null));
        assertThatThrownBy(() -> service.createRefund(request())).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("REFUND_LATEST_PAYMENT_FIRST");
        verifyNoInteractions(gateway);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "pending,finance_refund,same,merchant",
            "uploaded,other_business,same,merchant",
            "uploaded,finance_refund,other_refund,merchant",
            "uploaded,finance_refund,same,other_merchant"
    })
    void executionRejectsUnuploadedUnboundOrForeignEvidence(String status, String businessType,
            String businessId, String merchant) {
        FinanceRefundRecord refund = service.createRefund(request());
        when(authorization.currentMerchantScope()).thenReturn(Optional.of("merchant"));
        useActualEvidenceValidator(refund, status, businessType, businessId, merchant);
        assertThatThrownBy(() -> service.recordFinanceRefundExecution(refund.id(), execution("invalid-proof")))
                .isInstanceOf(ApiException.class);
        assertThat(refunds.get(refund.id()).status()).isEqualTo("requested");
    }

    @Test
    void uploadedEvidenceBoundToThisRefundAllowsExecutionAndIndependentConfirmation() {
        FinanceRefundRecord refund = service.createRefund(request());
        when(authorization.currentMerchantScope()).thenReturn(Optional.of("merchant"));
        useActualEvidenceValidator(refund, "uploaded", "finance_refund", "same", "merchant");
        assertThat(service.recordFinanceRefundExecution(refund.id(), execution("valid-proof")).status()).isEqualTo("processing");
        actor("reviewer");
        assertThat(service.confirmFinanceRefundResult(refund.id(), confirmation("verified-proof", "finance")).status())
                .isEqualTo("success");
    }

    @Test
    void confirmationCannotUsePendingEvidenceEvenAfterValidExecution() {
        FinanceRefundRecord refund = service.createRefund(request());
        useActualEvidenceValidator(refund, "uploaded", "finance_refund", "same", "merchant");
        service.recordFinanceRefundExecution(refund.id(), execution("valid-proof"));
        actor("reviewer");
        useActualEvidenceValidator(refund, "pending", "finance_refund", "same", "merchant");
        assertThatThrownBy(() -> service.confirmFinanceRefundResult(refund.id(), confirmation("pending-proof", "finance")))
                .isInstanceOf(ApiException.class);
        assertThat(refunds.get(refund.id()).status()).isEqualTo("processing");
    }

    private void useActualEvidenceValidator(FinanceRefundRecord refund, String status, String businessType,
            String businessId, String merchant) {
        var evidenceRepository = mock(com.complex.platform.evidence.EvidenceEventRepository.class);
        Instant now = Instant.now();
        var storage = new com.complex.platform.evidence.EvidenceFileStorage(proofDirectory.toString(), 1024);
        var stored = storage.store(new org.springframework.mock.web.MockMultipartFile(
                "file", "凭证.png", "image/png", new byte[]{1, 2, 3, 4}));
        when(evidenceRepository.findAttachmentById("evidence")).thenReturn(Optional.of(
                new com.complex.platform.evidence.EvidenceAttachmentRecord("evidence", "凭证.png", "image/png", stored.fileSize(),
                        businessType, "same".equals(businessId) ? refund.id() : businessId, "proof", stored.objectKey(),
                        "/upload", status, null, now, now, merchant)));
        service.refundEvidenceValidator = new EvidenceAttachmentValidator(evidenceRepository, storage);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ticket_payment_rejection", "channel_ticket_rejection"})
    void rejectedTicketMoneyRequiresRealExecutionAndDifferentVerifier(String sourceType) {
        when(repository.listMerchantOperatingProjectIds("merchant")).thenReturn(List.of("project"));
        String id = service.recordRejectedTicketPaymentRefund(sourceType, "rejected-money", 3000,
                "wechat", "original-payment", "merchant", null, "全园故障拒收原路待退");
        FinanceRefundRecord refund = refunds.get(id);
        assertThat(refund.status()).isEqualTo("requested");
        assertThat(refund.succeededAt()).isNull();
        assertThatThrownBy(() -> service.retryFinanceRefund(id, operation("no-auto-route")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_ROUTE_UNAVAILABLE");
        useActualEvidenceValidator(refund, "uploaded", "finance_refund", "same", "merchant");
        assertThat(service.recordFinanceRefundExecution(id, execution("rejected-execution")).status()).isEqualTo("processing");
        assertThatThrownBy(() -> service.confirmFinanceRefundResult(id, confirmation("rejected-self", "finance")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_SELF_CONFIRM_FORBIDDEN");
        actor("reviewer");
        assertThat(service.confirmFinanceRefundResult(id, confirmation("rejected-verified", "finance")).status()).isEqualTo("success");
        assertThat(refunds.get(id).succeededAt()).isNotNull();
        assertThat(service.recordRejectedTicketPaymentRefund(sourceType, "rejected-money", 3000,
                "wechat", "original-payment", "merchant", null, "重复回调")).isEqualTo(id);
        assertThat(refunds.get(id).status()).isEqualTo("success");
        verifyNoInteractions(gateway);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ticket_payment_rejection", "channel_ticket_rejection"})
    void rejectedTicketMoneyWithUnknownProjectCannotPayOut(String sourceType) {
        String id = service.recordRejectedTicketPaymentRefund(sourceType, "unknown-project", 3000,
                "wechat", "original-payment", null, null, "原归属待核实");
        assertThat(refunds.get(id).projectId()).isNull();
        assertThatThrownBy(() -> service.recordFinanceRefundExecution(id, execution("unknown-execute")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_PROJECT_UNRESOLVED");
        assertThatThrownBy(() -> service.confirmFinanceRefundResult(id, confirmation("unknown-confirm", "finance")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_PROJECT_UNRESOLVED");
        assertThat(refunds.get(id).status()).isEqualTo("requested");
        verifyNoInteractions(gateway);
    }

    @Test
    void merchantCardWithoutOwnProjectUsesOnlyItsUniqueTrustedMerchantProject() {
        when(repository.listMerchantOperatingProjectIds("merchant")).thenReturn(List.of("project"));
        String id = service.recordStoredValuePayoutRefund("card-without-project", 3000, "退卡",
                "merchant", null, "merchant-card");
        FinanceRefundRecord refund = refunds.get(id);
        assertThat(refund.projectId()).isEqualTo("project");
        assertThat(refund.targetId()).isEqualTo("card-without-project");
        useActualEvidenceValidator(refund, "uploaded", "finance_refund", "same", "merchant");
        service.recordFinanceRefundExecution(id, execution("merchant-execution"));
        actor("reviewer");
        assertThat(service.confirmFinanceRefundResult(id, confirmation("merchant-confirm", "finance")).status())
                .isEqualTo("success");
        when(repository.listMerchantOperatingProjectIds("merchant")).thenReturn(List.of("new-project"));
        service.recordStoredValuePayoutRefund("card-without-project", 3000, "退卡", "merchant", null, "merchant-card");
        assertThat(refunds.get(id).projectId()).isEqualTo("project");
        assertThat(refunds.get(id).status()).isEqualTo("success");
        verify(repository, times(1)).listMerchantOperatingProjectIds("merchant");
        verifyNoInteractions(gateway);
    }

    @Test
    void unresolvedMerchantCardProjectKeepsPendingAndCannotBeExecutedOrConfirmed() {
        for (List<String> projects : List.of(List.<String>of(), List.of("project-one", "project-two"))) {
            when(repository.listMerchantOperatingProjectIds("merchant")).thenReturn(projects);
            String key = "unresolved-" + projects.size();
            String id = service.recordStoredValuePayoutRefund("card-" + key, 3000, "退卡", "merchant", null, key);
            assertThat(refunds.get(id).projectId()).isNull();
            assertThat(refunds.get(id).status()).isEqualTo("requested");
            assertThatThrownBy(() -> service.recordFinanceRefundExecution(id, execution(key + "-execute")))
                    .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_PROJECT_UNRESOLVED");
            assertThatThrownBy(() -> service.confirmFinanceRefundResult(id, confirmation(key + "-confirm", "finance")))
                    .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_PROJECT_UNRESOLVED");
            when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("project-one"));
            assertThatThrownBy(() -> service.getFinanceRefund(id)).isInstanceOf(ApiException.class)
                    .extracting("code").isEqualTo("CROSS_PROJECT_FORBIDDEN");
            when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        }
        verifyNoInteractions(gateway);
    }

    @Test
    void originalCardProjectTakesPriorityAndForeignScopesCannotProcessMerchantCardRefund() {
        String id = service.recordStoredValuePayoutRefund("card", 3000, "退卡", "merchant", "card-project", "card-project");
        verify(repository, never()).listMerchantOperatingProjectIds(anyString());
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("other-project"));
        assertThatThrownBy(() -> service.recordFinanceRefundExecution(id, execution("cross-project")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CROSS_PROJECT_FORBIDDEN");
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        when(authorization.currentMerchantScope()).thenReturn(Optional.of("other-merchant"));
        assertThatThrownBy(() -> service.recordStoredValuePayoutRefund("card", 3000, "退卡", "merchant",
                "card-project", "card-project")).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void storedValuePayoutReplayPreservesFinalStatusAndRejectsChangedSource() {
        String id = service.recordStoredValuePayoutRefund("account", 3000, "退卡", "merchant", "project", "stored-repeat");
        FinanceRefundRecord original = refunds.get(id);
        Instant now = Instant.now();
        refunds.put(id, new FinanceRefundRecord(id, original.targetId(), original.refundAmountCent(), original.reason(),
                original.refundChannel(), original.refundDirection(), "success", original.createdAt(), original.sourceType(),
                original.merchantId(), original.projectId(), original.requestedBy(), original.originalPaymentRef(),
                "manual_verified", "actual-payout", now, now));
        assertThat(service.recordStoredValuePayoutRefund("account", 3000, "退卡", "merchant", "project", "stored-repeat"))
                .isEqualTo(id);
        assertThat(refunds.get(id).status()).isEqualTo("success");
        assertThatThrownBy(() -> service.recordStoredValuePayoutRefund("other-account", 3000, "退卡",
                "merchant", "project", "stored-repeat")).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("IDEMPOTENCY_KEY_CONFLICT");
    }

    @Test
    void confirmationNeedsRealEvidenceAndExactAmount() {
        FinanceRefundRecord refund = service.createRefund(request());
        service.recordFinanceRefundExecution(refund.id(), execution("execute"));
        actor("reviewer");
        RefundResultConfirmRequest wrong = new RefundResultConfirmRequest("wrong", "复核", List.of("evidence"),
                "success", 3001L, "execution-ref", "finance", null);
        assertThatThrownBy(() -> service.confirmFinanceRefundResult(refund.id(), wrong))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_RESULT_AMOUNT_MISMATCH");
        doThrow(ApiException.badRequest("EVIDENCE_ATTACHMENT_NOT_FOUND", "附件不存在"))
                .when(service.refundEvidenceValidator).requireRefundEvidence(any(), anyString(), nullable(String.class));
        assertThatThrownBy(() -> service.confirmFinanceRefundResult(refund.id(), confirmation("proof", "finance")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("EVIDENCE_ATTACHMENT_NOT_FOUND");
    }
}
