package com.complex.platform.finance;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.dto.RefundRequest;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Transactional
class FinanceRefundLifecycleJdbcIntegrationTests {
    @Autowired JdbcFinanceRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    private FinanceRefundRecord refund(String id, String target, String merchant, String project, String status) {
        Instant now = Instant.now();
        return new FinanceRefundRecord(id, target, 3000, "已批准原路退回", "wechat", "original", status, now,
                "ar_ap_payment", merchant, project, "operator", "original-flow", null, null, now,
                "success".equals(status) ? now : null);
    }

    @Test
    void rejectedPaymentBacklogHasStableOriginalSourceAndCannotRegressSuccessfulResult() {
        AuthorizationService auth = mock(AuthorizationService.class);
        when(auth.currentActor()).thenReturn(new AccessActor("operator", "operator", "platform_admin", "org", null, "active"));
        FinanceService service = new FinanceService(repository, null, mock(AuditEventService.class), auth, null,
                PaymentGatewayRouter.alwaysDisabled(), null);
        String id = service.recordRejectedTicketPaymentRefund("channel_ticket_rejection", "sync-rejected-test", 8800,
                "meituan", "external-payment-1", null, "project-one", "停售拒收后核实原路退");
        FinanceRefundRecord pending = repository.findRefundById(id, true).orElseThrow();
        assertThat(pending.status()).isEqualTo("requested");
        assertThat(pending.originalPaymentRef()).isEqualTo("external-payment-1");
        assertThat(pending.succeededAt()).isNull();
        Instant now = Instant.now();
        repository.saveRefund(new FinanceRefundRecord(pending.id(), pending.targetId(), pending.refundAmountCent(),
                pending.reason(), pending.refundChannel(), pending.refundDirection(), "success", pending.createdAt(),
                pending.sourceType(), pending.merchantId(), pending.projectId(), pending.requestedBy(),
                pending.originalPaymentRef(), "manual_verified", "real-refund-rejected-test", now, now));
        assertThat(service.recordRejectedTicketPaymentRefund("channel_ticket_rejection", "sync-rejected-test", 8800,
                "meituan", "external-payment-1", null, "project-one", "停售拒收后核实原路退")).isEqualTo(id);
        assertThat(repository.findRefundById(id, true).orElseThrow().status()).isEqualTo("success");
        assertThat(repository.sumRefundedAmountByTargetId("sync-rejected-test")).isZero();
        assertThatThrownBy(() -> service.recordRejectedTicketPaymentRefund("channel_ticket_rejection", "sync-rejected-test", 9900,
                "meituan", "external-payment-1", null, "project-one", "错误重复回流"))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);
    }

    @Test
    void rejectedTicketProjectComesFromUniqueOriginalMerchantNotOperatorScope() {
        AuthorizationService auth = mock(AuthorizationService.class);
        when(auth.currentActor()).thenReturn(new AccessActor("operator", "operator", "platform_admin", "org", null, "active"));
        FinanceRepository source = mock(FinanceRepository.class, org.mockito.AdditionalAnswers.delegatesTo(repository));
        when(source.listMerchantOperatingProjectIds("original-bar")).thenReturn(List.of("original-project"));
        when(source.listMerchantOperatingProjectIds("shared-bar")).thenReturn(List.of("project-a", "project-b"));
        FinanceService service = new FinanceService(source, null, mock(AuditEventService.class), auth, null,
                PaymentGatewayRouter.alwaysDisabled(), null);
        String original = service.recordRejectedTicketPaymentRefund("ticket_payment_rejection", "scope-original", 800,
                "wechat", "original-payment", null, "original-bar", "停售后原路待退");
        assertThat(source.findRefundById(original, true).orElseThrow().projectId()).isEqualTo("original-project");
        String ambiguous = service.recordRejectedTicketPaymentRefund("ticket_payment_rejection", "scope-ambiguous", 800,
                "wechat", "ambiguous-payment", null, "shared-bar", "项目归属待核实");
        assertThat(source.findRefundById(ambiguous, true).orElseThrow().projectId()).isNull();
        when(auth.currentProjectScopeQuietly()).thenReturn(java.util.Optional.of("caller-project"));
        String unknown = service.recordRejectedTicketPaymentRefund("ticket_payment_rejection", "scope-unknown", 800,
                "wechat", "unknown-payment", null, null, "项目归属待核实");
        assertThat(source.findRefundById(unknown, true).orElseThrow().projectId()).isNull();
        assertThatThrownBy(() -> service.recordRejectedTicketPaymentRefund("ticket_payment_rejection", "scope-forbidden", 800,
                "wechat", "other-payment", null, "original-bar", "禁止改挂操作人项目"))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .extracting(e -> ((com.complex.platform.common.api.ApiException)e).code()).isEqualTo("CROSS_PROJECT_FORBIDDEN");
    }

    @Test
    void scopeAndFailedOutstandingAmountsSurviveDatabaseRoundTrip() {
        repository.saveRefund(refund("rf_scope_a", "target-a", "merchant-a", "project-a", "failed"));
        repository.saveRefund(refund("rf_scope_b", "target-b", "merchant-b", "project-b", "requested"));
        assertThat(repository.findRefundById("rf_scope_a", true).orElseThrow().status()).isEqualTo("failed");
        assertThat(repository.sumRefundedAmountByTargetId("target-a")).isEqualTo(3000);
        assertThat(repository.sumSucceededRefundAmountByTargetId("target-a")).isZero();
        assertThat(repository.listRefunds("merchant-a", "project-a"))
                .extracting(FinanceRefundRecord::id).contains("rf_scope_a").doesNotContain("rf_scope_b");
        assertThat(repository.listRefunds("merchant-a", "project-b")).isEmpty();
    }

    @Test
    void storedValueAccountWithSameIdentifierDoesNotOccupyPaymentRefundAmount() {
        repository.saveRefund(refund("rf_source_payment", "same-target", "merchant", "project", "requested"));
        Instant now = Instant.now();
        repository.saveRefund(new FinanceRefundRecord("rf_source_stored", "same-target", 2000, "储值待退",
                "original", "original", "requested", now, "stored_value_account", "merchant", "project",
                "operator", null, null, null, now, null));
        assertThat(repository.sumRefundedAmountByTargetId("same-target")).isEqualTo(3000);
    }

    @Test
    void merchantProjectResolutionIncludesPrimaryAndAdditionalProjectsWithoutDuplicates() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String merchant = "rf_merchant_" + suffix;
        String first = "rf_project_a_" + suffix;
        String second = "rf_project_b_" + suffix;
        for (String project : List.of(first, second)) {
            jdbc.update("INSERT INTO projects (id,venue_type,name,status,created_at,updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                    project, project);
        }
        jdbc.update("INSERT INTO merchants (id,merchant_name,merchant_type,status,created_at,updated_at) VALUES (?, ?, 'bar', 'active', now(), now())",
                merchant, merchant);
        assertThat(repository.listMerchantOperatingProjectIds(merchant)).isEmpty();
        jdbc.update("UPDATE merchants SET project_id=? WHERE id=?", first, merchant);
        jdbc.update("INSERT INTO merchant_projects (merchant_id,project_id) VALUES (?,?)", merchant, first);
        assertThat(repository.listMerchantOperatingProjectIds(merchant)).containsExactly(first);
        jdbc.update("INSERT INTO merchant_projects (merchant_id,project_id) VALUES (?,?)", merchant, second);
        assertThat(repository.listMerchantOperatingProjectIds(merchant)).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void dispatchAttemptCanOnlyBeClaimedOnceAndEvidenceRemainsReadable() {
        FinanceRefundRecord refund = repository.saveRefund(refund("rf_claim", "target", "merchant", "project", "requested"));
        FinanceRefundAttemptRecord attempt = new FinanceRefundAttemptRecord("rf_attempt_claim", refund.id(),
                "submit", "requested", null, null, "原路退款", "original-flow", "operator", null,
                List.of("proof-one", "proof-two"), Instant.now());
        repository.appendRefundAttempt(attempt);
        assertThat(repository.findReadyRefundAttempt(refund.id())).isPresent();
        assertThat(repository.claimRefundAttempt(attempt.id())).isTrue();
        assertThat(repository.claimRefundAttempt(attempt.id())).isFalse();
        assertThat(repository.findReadyRefundAttempt(refund.id())).isEmpty();
        assertThat(repository.listRefundAttempts(refund.id()).getFirst().evidenceAttachmentIds())
                .containsExactly("proof-one", "proof-two");
    }

    @Test
    void sameSuccessfulChannelRefundCannotSettleTwoRefunds() {
        Instant now = Instant.now();
        FinanceRefundRecord first = new FinanceRefundRecord("rf_unique_a", "ta", 3000, "退款", "wechat", "original",
                "success", now, "ar_ap_payment", "ma", "pa", "operator", "orig-a",
                "wechat", "same-refund-flow", now, now);
        repository.saveRefund(first);
        FinanceRefundRecord second = new FinanceRefundRecord("rf_unique_b", "tb", 3000, "退款", "wechat", "original",
                "success", now, "ar_ap_payment", "mb", "pb", "operator", "orig-b",
                "wechat", "same-refund-flow", now, now);
        assertThatThrownBy(() -> repository.saveRefund(second)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentRefundRequestsCannotSpendMoreThanOriginalPayment() throws Exception {
        String key = UUID.randomUUID().toString().replace("-", "");
        String itemId = "rf_race_item_" + key;
        String paymentId = "rf_race_pay_" + key;
        Instant now = Instant.now();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            repository.saveArApItem(new ArApItemRecord(itemId, itemId, "receivable", "customer", "refund_race_test",
                    key, 10000, 10000, 0, "CNY", LocalDate.of(2099, 1, 1), "2099-01",
                    "settled", now, now, null, null));
            repository.saveArApPayment(new ArApPaymentRecord(paymentId, itemId, 10000, "wechat", now,
                    "original-" + key, now));
        });
        FinanceRepository competing = mock(FinanceRepository.class, org.mockito.AdditionalAnswers.delegatesTo(repository));
        java.util.concurrent.CountDownLatch bothBeforeLock = new java.util.concurrent.CountDownLatch(2);
        doAnswer(invocation -> {
            bothBeforeLock.countDown();
            assertThat(bothBeforeLock.await(15, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            return repository.findArApItemByIdForUpdate(itemId);
        }).when(competing).findArApItemByIdForUpdate(itemId);
        PaymentGatewayRouter gateway = mock(PaymentGatewayRouter.class);
        when(gateway.refund(any())).thenReturn(com.complex.platform.finance.gateway.PaymentGatewayResult.reserved("未接入"));
        try (var threads = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.List<java.util.concurrent.Future<Object>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 2; i++) {
                String attemptKey = key + "-" + i;
                futures.add(threads.submit(() -> {
                    AuthorizationService auth = mock(AuthorizationService.class);
                    when(auth.currentActor()).thenReturn(new AccessActor("operator", "operator", "staff", "org", null, "active"));
                    when(auth.projectReach(any(), any(String[].class)))
                            .thenReturn(new AuthorizationService.ProjectReach(true, true, null, false, java.util.Set.of()));
                    AuditEventService audit = new AuditEventService(new InMemoryAuditEventRepository());
                    IdempotencyService idempotency = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
                    idempotency.setTransactionManager(transactionManager);
                    FinanceService service = new FinanceService(competing, new AuditedOperationExecutor(idempotency, audit),
                            audit, auth, new BusinessDayResolver(), gateway);
                    service.setRefundTransactionManager(transactionManager);
                    service.refundAuthorizationService = mock(RefundAuthorizationService.class);
                    try {
                        return service.createRefund(new RefundRequest(attemptKey, "批准退款", List.of(), paymentId,
                                6000L, "并发原路退款", null, null));
                    } catch (com.complex.platform.common.api.ApiException exception) {
                        return exception.code();
                    }
                }));
            }
            java.util.List<Object> results = new java.util.ArrayList<>();
            for (var future : futures) results.add(future.get(30, java.util.concurrent.TimeUnit.SECONDS));
            assertThat(results.stream().filter(FinanceRefundRecord.class::isInstance)).hasSize(1);
            assertThat(results.stream().filter("VALIDATION_FAILED"::equals)).hasSize(1);
        }
        assertThat(repository.sumRefundedAmountByTargetId(paymentId)).isEqualTo(6000);
        verify(gateway, times(1)).refund(any());
        // 保留唯一测试交易及其 append-only 付款记录，后续查询按测试来源或 id 隔离。
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void originalAttemptIsCommittedBeforeGatewayAndTimeoutCannotEraseIt() {
        String key = UUID.randomUUID().toString().replace("-", "");
        String itemId = "rf_item_" + key;
        String paymentId = "rf_payment_" + key;
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Instant now = Instant.now();
        tx.executeWithoutResult(status -> {
            repository.saveArApItem(new ArApItemRecord(itemId, itemId, "receivable", "customer", "refund_test",
                    key, 10000, 10000, 0, "CNY", LocalDate.of(2099, 1, 1), "2099-01",
                    "settled", now, now, null, null));
            repository.saveArApPayment(new ArApPaymentRecord(paymentId, itemId, 10000, "wechat", now,
                    "original-" + key, now));
        });
        // 本例须提交真实事务；采用 UUID 数据隔离，保留 append-only 流水及关联证据，不做删除。
        {
            AuthorizationService auth = mock(AuthorizationService.class);
            when(auth.currentActor()).thenReturn(new AccessActor("operator", "operator", "staff", "org", null, "active"));
            when(auth.projectReach(any(), any(String[].class)))
                    .thenReturn(new AuthorizationService.ProjectReach(true, true, null, false, java.util.Set.of()));
            PaymentGatewayRouter gateway = mock(PaymentGatewayRouter.class);
            when(gateway.refund(any())).thenAnswer(invocation -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM finance_refunds r JOIN finance_refund_attempts a "
                        + "ON a.refund_id=r.id WHERE r.target_id=? AND r.status='processing' AND a.dispatch_state='claimed'",
                        Integer.class, paymentId)).isEqualTo(1);
                throw new IllegalStateException("退款请求发送后连接超时");
            });
            AuditEventService audit = new AuditEventService(new InMemoryAuditEventRepository());
            IdempotencyService idempotency = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
            idempotency.setTransactionManager(transactionManager);
            FinanceService service = new FinanceService(repository, new AuditedOperationExecutor(idempotency, audit),
                    audit, auth, new BusinessDayResolver(), gateway);
            service.setRefundTransactionManager(transactionManager);
            service.refundAuthorizationService = mock(RefundAuthorizationService.class);
            RefundRequest request = new RefundRequest(key, "批准退款", List.of(), paymentId, 3000L, "核实原路退款", null, null);
            FinanceRefundRecord refund = service.createRefund(request);
            assertThat(refund.status()).isEqualTo("processing");
            assertThat(repository.findRefundById(refund.id(), false).orElseThrow().status()).isEqualTo("processing");
            service.createRefund(request);
            verify(gateway, times(1)).refund(any());
        }
    }
}
