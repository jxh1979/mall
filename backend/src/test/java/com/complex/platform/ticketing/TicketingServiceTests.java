package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTaskItemRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTasksRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import com.complex.platform.ticketing.dto.TicketTypeEntitlementRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketingServiceTests {

    private InMemoryTicketingRepository ticketingRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private TicketingService service;

    @BeforeEach
    void setUp() {
        ticketingRepository = new InMemoryTicketingRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        IdempotencyService idempotencyService = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
        AuditedOperationExecutor operations = new AuditedOperationExecutor(idempotencyService, auditEventService);
        TicketPricingService pricingService = new TicketPricingService(ticketingRepository);
        TicketVerificationService verificationService = new TicketVerificationService(ticketingRepository, event -> { });
        AuthorizationService authorizationService = new AuthorizationService(null);
        service = new TicketingService(
                ticketingRepository,
                new TicketCatalogService(ticketingRepository),
                pricingService,
                new TicketOrderService(ticketingRepository, pricingService, authorizationService),
                new TicketRefundService(ticketingRepository),
                verificationService,
                new TicketCredentialService(verificationService, auditEventService),
                new TicketOfflinePackageService(ticketingRepository, auditEventService),
                new TicketingOperationExecutor(operations)
        );
    }

    @Test
    void createTicketTypeWritesOpenApiAuditEvent() {
        TicketTypeRecord created = service.createTicketType(new TicketTypeRequest(
                "ticket-type-create",
                "create ticket type",
                List.of(),
                " P0 Pass ",
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                List.of(),
                false,
                "active"
        ));

        assertThat(created.id()).startsWith("ticket_type_");
        assertThat(created.ticketName()).isEqualTo("P0 Pass");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("ticket_product.changed");
    }

    @Test
    void createEntitlementTemplateIsIdempotent() {
        EntitlementTemplateRequest request = new EntitlementTemplateRequest(
                "template-once",
                "create template",
                List.of(),
                "P0 Entry",
                "entry",
                2,
                9900L,
                "active"
        );

        EntitlementTemplateRecord first = service.createEntitlementTemplate(request);
        EntitlementTemplateRecord second = service.createEntitlementTemplate(request);

        assertThat(second).isEqualTo(first);
        assertThat(ticketingRepository.entitlementTemplateSaveCount).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "ticket_entitlement_template.changed".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void directIssueCreatesEntitlementCodeFromBoundTemplate() {
        TicketTypeRecord ticketType = createTicketType("issue-ticket");
        EntitlementTemplateRecord template = createTemplate("issue-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "bind-template",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default"
        ));

        IssuedTicketResponse issued = service.issueTicketTypeDirectly(ticketType.id(), new TicketIssueRequest(
                "issue-direct",
                "issue",
                List.of(),
                "order_1",
                "manual",
                List.of("qr")
        ));

        assertThat(issued.issuedEntitlements()).hasSize(1);
        assertThat(issued.issuedEntitlements().getFirst().entitlementCode()).startsWith("P0-");
        assertThat(ticketingRepository.issuedEntitlements).hasSize(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_order.issued");
    }

    @Test
    void previewDoesNotConsumeRemainingTimes() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("preview");

        VerificationPreviewResponse preview = service.previewVerification(new VerificationRequest(
                "preview-once",
                "qr",
                entitlement.entitlementCode(),
                "dev_1",
                "gate_1",
                Map.of("scenario", "ticket_checkin")
        ));

        assertThat(preview.allowed()).isTrue();
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get()
                .extracting(IssuedEntitlementRecord::usedTimes)
                .isEqualTo(0);
    }

    // C1/REQ-05: 人工放行留痕 manual_overridden
    @Test
    void manualOverrideRecordsManualOverriddenVerification() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("override");
        VerificationResponse resp = service.manualOverrideVerification(new VerificationRequest(
                "override-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()),
                "supervisor release");
        assertThat(resp.status()).isEqualTo("manual_overridden");
        assertThat(ticketingRepository.verifications.values())
                .filteredOn(v -> "manual_overridden".equals(v.status()))
                .hasSize(1);
    }

    // R5 T5：多人票核销，verified_people 随权益 num_people 写入（当前默认 1）。
    @Test
    void verificationWritesVerifiedPeopleFromEntitlementNumPeople() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("verified-people");
        // 模拟多人票：把权益 num_people 改为 4（来源待票种配置，本期发放默认 1）。
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                entitlement.id(), entitlement.entitlementCode(), entitlement.ticketTypeId(), entitlement.orderId(),
                entitlement.entitlementTemplateId(), entitlement.entitlementType(), entitlement.merchantId(),
                entitlement.venueId(), entitlement.pointId(), entitlement.totalTimes(), entitlement.usedTimes(),
                entitlement.valueAmountCent(), entitlement.status(), entitlement.validFrom(), entitlement.validTo(),
                entitlement.createdAt(), entitlement.updatedAt(), 4
        ));

        service.createVerification(new VerificationRequest(
                "verified-people-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));

        assertThat(ticketingRepository.verifications.values())
                .filteredOn(v -> "success".equals(v.status()))
                .singleElement()
                .extracting(VerificationRecord::verifiedPeople)
                .isEqualTo(4);
        // used_times 一次性 +1（沿用 atomicUseEntitlement），不超 total_times。
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(1);
    }

    // R5 T5：核销冲正回滚 used_times，原记录置 reversed，新建 reversed 记录指向原记录。
    @Test
    void reverseVerificationRollsBackUsedTimesAndMarksOriginalReversed() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("reverse-rollback");
        VerificationResponse success = service.createVerification(new VerificationRequest(
                "reverse-rollback-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(1);

        VerificationResponse reversal = service.reverseVerification(
                success.id(),
                new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-rollback-rev", "误核销"),
                "supervisor_1");

        assertThat(reversal.status()).isEqualTo("reversed");
        // used_times 回滚到 0，权益恢复 active。
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes, IssuedEntitlementRecord::status)
                .containsExactly(0, "active");
        // 原记录置 reversed + reversed_by/reversed_at。
        assertThat(ticketingRepository.findVerificationById(success.id()))
                .get()
                .satisfies(v -> {
                    assertThat(v.status()).isEqualTo("reversed");
                    assertThat(v.reversedBy()).isEqualTo("supervisor_1");
                    assertThat(v.reversedAt()).isNotNull();
                });
        // 新建一条 reversed 记录 reverses_verification_id 指向原 id。
        assertThat(ticketingRepository.findVerificationById(reversal.id()))
                .get()
                .satisfies(v -> {
                    assertThat(v.status()).isEqualTo("reversed");
                    assertThat(v.reversesVerificationId()).isEqualTo(success.id());
                });
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket.verification_reversed");
    }

    // R5 T5：重复冲正被拒（幂等/重复冲正拒绝）。
    @Test
    void reverseVerificationRejectsRepeatedReverse() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("reverse-dup");
        VerificationResponse success = service.createVerification(new VerificationRequest(
                "reverse-dup-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        service.reverseVerification(success.id(),
                new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-dup-rev-1", "误核销"),
                "supervisor_1");

        // 第二次冲正（不同幂等键，非幂等回放）原记录已 reversed（非 success 且 reversed_at 非空）→ 拒绝，不二次回滚。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.reverseVerification(success.id(),
                        new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-dup-rev-2", "再次冲正"),
                        "supervisor_1"))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageMatching(".*(reversed|reversible).*");
        // used_times 不被二次回滚（仍为 0，不会变负）。
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(0);
    }

    @Test
    void verificationConsumesExactlyOneRemainingTimeAndIsIdempotent() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("verify");
        VerificationRequest request = new VerificationRequest(
                "verify-once",
                "qr",
                entitlement.entitlementCode(),
                "dev_1",
                "gate_1",
                Map.of("scenario", "ticket_checkin")
        );

        VerificationResponse first = service.createVerification(request);
        VerificationResponse second = service.createVerification(request);

        assertThat(second).isEqualTo(first);
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get()
                .extracting(IssuedEntitlementRecord::usedTimes)
                .isEqualTo(1);
        assertThat(ticketingRepository.verifications).hasSize(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "verification.succeeded".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void syncTaskAcceptsOfflineVerificationAndDoesNotConsumeDuplicateClientTaskTwice() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("sync");
        TerminalSyncTaskItemRequest task = new TerminalSyncTaskItemRequest(
                "client-task-1",
                "offline_verification",
                "2026-06-14T00:00:00Z",
                Map.of(
                        "entitlementCode", entitlement.entitlementCode(),
                        "entitlement_id", entitlement.id(),
                        "mode", "offline"
                )
        );

        SyncTasksResponse first = service.syncTerminalTasks(new TerminalSyncTasksRequest(
                "sync-first",
                "sync",
                List.of(),
                "P0-ANDROID-1",
                "staff_1",
                List.of(task)
        ));
        SyncTasksResponse duplicate = service.syncTerminalTasks(new TerminalSyncTasksRequest(
                "sync-second-key",
                "sync duplicate",
                List.of(),
                "P0-ANDROID-1",
                "staff_1",
                List.of(task)
        ));

        assertThat(first.results()).singleElement()
                .extracting(SyncTaskResult::status)
                .isEqualTo("accepted");
        assertThat(duplicate.results()).singleElement()
                .extracting(SyncTaskResult::verificationId)
                .isEqualTo(first.results().getFirst().verificationId());
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get()
                .extracting(IssuedEntitlementRecord::usedTimes)
                .isEqualTo(1);
    }

    @Test
    void createPriceRuleIsIdempotentAndListWritesViewAuditEvent() {
        TicketTypeRecord ticketType = createTicketType("price-rule-ticket");
        TicketPriceRuleRequest request = new TicketPriceRuleRequest(
                "price-rule-once",
                "student fixed",
                List.of(),
                ticketType.id(),
                "student",
                "fixed_price",
                6900L,
                7900L,
                8900L,
                null,
                "miniapp",
                Map.of("student", "verified")
        );

        TicketPriceRuleRecord first = service.createTicketPriceRule(request);
        TicketPriceRuleRecord second = service.createTicketPriceRule(request);

        assertThat(second).isEqualTo(first);
        assertThat(first.currency()).isEqualTo("CNY");
        assertThat(ticketingRepository.priceRules).hasSize(1);

        PageResponse<TicketPriceRuleRecord> page = service.listTicketPriceRules();

        assertThat(page.items()).extracting(TicketPriceRuleRecord::id).contains(first.id());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_price_variant.changed", "ticket_price_variant.viewed");
    }

    @Test
    void createPriceRuleAcceptsMemberVariantDerivedFromCalendarPrice() {
        TicketTypeRecord ticketType = createTicketType("member-price-rule-ticket");
        TicketPriceRuleRecord rule = service.createTicketPriceRule(new TicketPriceRuleRequest(
                "member-price-rule-once",
                "member discount",
                List.of(),
                ticketType.id(),
                "member",
                "derive_from_calendar_price",
                null,
                null,
                null,
                new BigDecimal("90.00"),
                null,
                Map.of("member_level", "standard")
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "member-price-order",
                "create order",
                List.of(),
                ticketType.id(),
                rule.id(),
                2,
                LocalDate.parse("2026-06-20"),
                "app",
                "13800138000",
                "customer_member"
        ));

        assertThat(rule)
                .extracting(TicketPriceRuleRecord::variantType, TicketPriceRuleRecord::priceMode)
                .containsExactly("member", "derive_from_calendar_price");
        assertThat(order.amountCent()).isEqualTo(21420L);
    }

    @Test
    void createPriceRuleRejectsUnknownTicketType() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createTicketPriceRule(new TicketPriceRuleRequest(
                        "price-rule-missing-ticket",
                        "missing",
                        List.of(),
                        "ticket_type_missing",
                        "group",
                        "derive_from_calendar_price",
                        null,
                        null,
                        null,
                        new BigDecimal("80.00"),
                        null,
                        Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("ticket type not found");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_price_variant.change_failed");
    }

    @Test
    void createTicketOrderComputesAmountAndRequiresPositiveQuantity() {
        TicketTypeRecord ticketType = createTicketType("order-ticket");
        TicketPriceRuleRecord rule = service.createTicketPriceRule(new TicketPriceRuleRequest(
                "order-fixed-rule",
                "fixed",
                List.of(),
                ticketType.id(),
                "channel",
                "fixed_price",
                8000L,
                9000L,
                10000L,
                null,
                "web",
                Map.of()
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "order-create",
                "create order",
                List.of(),
                ticketType.id(),
                rule.id(),
                2,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_1"
        ));

        assertThat(order.amountCent()).isEqualTo(18000L);
        assertThat(order.currency()).isEqualTo("CNY");
        assertThat(order.payStatus()).isEqualTo("unpaid");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_order.created");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createTicketOrder(new TicketOrderRequest(
                        "order-zero",
                        "bad quantity",
                        List.of(),
                        ticketType.id(),
                        rule.id(),
                        0,
                        LocalDate.parse("2026-06-20"),
                        "web",
                        null,
                        "customer_1"
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("quantity must be positive");
    }

    @Test
    void derivedPriceRuleUsesDecimalDiscountRateForCentRounding() {
        assertDerivedPriceRuleAmount("decimal-discount-3333", 9999L, new BigDecimal("33.33"), 3333L);
        assertDerivedPriceRuleAmount("decimal-discount-half-cent", 250L, new BigDecimal("64.60"), 162L);
    }

    @Test
    void payIntentAndIssueOrderAreIdempotent() {
        TicketTypeRecord ticketType = createTicketType("issue-order-ticket");
        EntitlementTemplateRecord template = createTemplate("issue-order-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "issue-order-bind",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default"
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "issue-order-create",
                "create",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "admin",
                "13800138000",
                "customer_issue"
        ));

        TicketPaymentIntentRecord intent = service.createTicketOrderPayIntent(order.id(), new PaymentIntentRequest(
                "issue-order-pay",
                "pay",
                List.of(),
                "ticket_order",
                order.id(),
                order.amountCent(),
                "CNY",
                "aggregate_pay"
        ));
        IssuedTicketResponse firstIssue = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "issue-order-once",
                "issue",
                "paid offline",
                List.of()
        ));
        IssuedTicketResponse secondIssue = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "issue-order-once",
                "issue",
                "paid offline",
                List.of()
        ));

        assertThat(intent.status()).isEqualTo("created");
        assertThat(firstIssue).isEqualTo(secondIssue);
        assertThat(ticketingRepository.issuedEntitlements).hasSize(1);
        assertThat(ticketingRepository.findTicketOrderById(order.id()))
                .get()
                .extracting(TicketOrderRecord::issueStatus)
                .isEqualTo("issued");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("payment.intent_created", "ticket.issued");
    }

    @Test
    void publishListIssuedAndCredentialRefreshUseShortLivedCredential() {
        TicketTypeRecord ticketType = createTicketType("credential-ticket");
        TicketTypeRecord published = service.publishTicketType(ticketType.id(), new StatusChangeRequest(
                "publish-ticket",
                "publish",
                "ready",
                List.of()
        ));
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential");

        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        assertThat(published.status()).isEqualTo("published");
        assertThat(service.listIssuedEntitlements().items())
                .extracting(IssuedEntitlementRecord::id)
                .contains(entitlement.id());
        assertThat(credential.credentialToken()).startsWith("cred_");
        assertThat(credential.expiresAt()).isAfter(credential.issuedAt());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_price.published", "ticket_issued_entitlement.viewed", "ticket_credential.refreshed");
    }

    @Test
    void credentialPayloadCanBeVerifiedAndUnknownCredentialFails() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-verify");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        VerificationPreviewResponse preview = service.previewVerification(new VerificationRequest(
                "credential-preview",
                "dynamic_qr",
                credential.credentialToken(),
                "dev_1",
                "gate_1",
                Map.of()
        ));

        assertThat(preview.allowed()).isTrue();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.previewVerification(new VerificationRequest(
                        "credential-preview-missing",
                        "dynamic_qr",
                        "cred_missing",
                        "dev_1",
                        "gate_1",
                        Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("credential not found");
    }

    @Test
    void dynamicCredentialCannotBeReplayedAfterSuccessfulVerification() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-replay");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        service.createVerification(new VerificationRequest(
                "credential-replay-first",
                "dynamic_qr",
                credential.credentialToken(),
                "dev_1",
                "gate_1",
                Map.of()
        ));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                        "credential-replay-second",
                        "dynamic_qr",
                        credential.credentialToken(),
                        "dev_1",
                        "gate_1",
                        Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("credential is expired");
    }

    // T4：刷新窗口与票种级 refresh_interval_seconds 联动（替换硬编码 300s）。
    @Test
    void credentialRefreshExpiryUsesRefreshIntervalNotHardcoded300s() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-window");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        long windowSeconds = java.time.Duration.between(credential.issuedAt(), credential.expiresAt()).getSeconds();
        TicketCredentialRecord stored = ticketingRepository.findTicketCredentialByToken(credential.credentialToken()).orElseThrow();

        assertThat(windowSeconds).isEqualTo(stored.refreshIntervalSeconds());
        assertThat(windowSeconds).isNotEqualTo(300L);
        assertThat(stored.windowStartedAt()).isEqualTo(credential.issuedAt());
        assertThat(stored.signatureValue()).isNull();
        assertThat(stored.signatureVersion()).isNull();
    }

    // T4：成功核销留 nonce/merchant_id/window_valid=true 事后核对维度，signature_valid 本期恒 null。
    @Test
    void successfulVerificationRecordsNonceMerchantAndWindowValid() {
        TestSecurityActors.asMerchantUser("mch_window_test");
        try {
            IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-window-dims");
            TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

            service.createVerification(new VerificationRequest(
                    "credential-window-dims-once",
                    "dynamic_qr",
                    credential.credentialToken(),
                    "dev_1",
                    "gate_1",
                    Map.of()
            ));

            assertThat(ticketingRepository.verifications.values())
                    .filteredOn(v -> "success".equals(v.status()))
                    .singleElement()
                    .satisfies(v -> {
                        assertThat(v.nonce()).isNotBlank();
                        assertThat(v.merchantId()).isEqualTo("mch_window_test");
                        assertThat(v.windowValid()).isTrue();
                        assertThat(v.signatureValid()).isNull();
                    });
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void dynamicCredentialVerificationStoresHashedPayloadOnly() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-payload-hash");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        service.createVerification(new VerificationRequest(
                "credential-payload-hash-once",
                "dynamic_qr",
                credential.credentialToken(),
                "dev_1",
                "gate_1",
                Map.of()
        ));

        assertThat(ticketingRepository.listVerifications())
                .extracting(VerificationRecord::credentialPayload)
                .allSatisfy(payload -> assertThat(payload)
                        .startsWith("sha256:")
                        .doesNotContain(credential.credentialToken()));
    }

    @Test
    void offlineSyncTaskStoresHashedDynamicCredentialPayloadOnly() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("sync-credential-payload-hash");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());
        TerminalSyncTaskItemRequest task = new TerminalSyncTaskItemRequest(
                "client-dynamic-credential-task",
                "offline_verification",
                "2026-06-14T00:00:00Z",
                Map.of(
                        "credentialPayload", credential.credentialToken(),
                        "pointId", "gate_1"
                )
        );

        service.syncTerminalTasks(new TerminalSyncTasksRequest(
                "sync-dynamic-credential",
                "sync dynamic credential",
                List.of(),
                "P0-ANDROID-1",
                "staff_1",
                List.of(task)
        ));

        TerminalSyncTaskRecord saved = ticketingRepository.findTerminalSyncTask("P0-ANDROID-1", "client-dynamic-credential-task")
                .orElseThrow();
        assertThat(saved.payload().get("credentialPayload")).startsWith("sha256:");
        assertThat(saved.payload().values()).doesNotContain(credential.credentialToken());
    }

    @Test
    void refundRequestAndApprovalDoNotDeleteIssuedHistory() {
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("refund");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();

        TicketRefundRecord refund = service.requestTicketRefund(new TicketRefundRequest(
                "refund-request",
                "refund",
                List.of(),
                order.id(),
                order.amountCent(),
                "customer request",
                "original"
        ));
        TicketRefundRecord approved = service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-approve",
                "approve",
                "approved",
                List.of()
        ));

        assertThat(approved.status()).isEqualTo("approved");
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode())).isPresent();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_refund.requested", "ticket_refund.approved");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(new TicketRefundRequest(
                        "refund-too-much",
                        "refund",
                        List.of(),
                        order.id(),
                        order.amountCent() + 1,
                        "too much",
                        "original"
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("refund amount exceeds order amount");
    }

    @Test
    void approveTicketRefundRejectsAlreadyApprovedRefund() {
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("refund-reapprove");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();

        TicketRefundRecord refund = service.requestTicketRefund(new TicketRefundRequest(
                "refund-reapprove-request",
                "refund",
                List.of(),
                order.id(),
                order.amountCent(),
                "customer request",
                "original"
        ));
        service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-reapprove-1", "approve", "approved", List.of()));

        // A second approval (fresh idempotency key, so not an idempotent replay) must be rejected
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.approveTicketRefund(
                        refund.id(),
                        new StatusChangeRequest("refund-reapprove-2", "approve", "approved", List.of())))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .extracting("code")
                .isEqualTo("TICKET_REFUND_NOT_APPROVABLE");
    }

    @Test
    void refundRequestCreatesEntitlementLevelLinesAndApprovalMarksLinesApproved() {
        TicketTypeRecord ticketType = createTicketType("refund-lines-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-lines-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-lines-bind",
                "bind",
                List.of(),
                template.id(),
                2,
                1,
                9900L,
                "default"
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-lines-order",
                "create",
                List.of(),
                ticketType.id(),
                null,
                2,
                LocalDate.parse("2026-06-15"),
                "admin",
                "13800138000",
                "customer_refund_lines"
        ));
        IssuedTicketResponse issued = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "refund-lines-issue-order",
                "issue",
                "issue",
                List.of()
        ));

        TicketRefundRecord refund = service.requestTicketRefund(new TicketRefundRequest(
                "refund-lines-request",
                "refund",
                List.of(),
                order.id(),
                order.amountCent(),
                "customer request",
                "original"
        ));

        PageResponse<TicketRefundLineRecord> requestedLines = service.listTicketRefundLines(refund.id());
        assertThat(requestedLines.items())
                .hasSize(2)
                .allSatisfy(line -> {
                    assertThat(line.refundId()).isEqualTo(refund.id());
                    assertThat(line.orderId()).isEqualTo(order.id());
                    assertThat(line.lineType()).isEqualTo("entitlement");
                    assertThat(line.refundAmountCent()).isEqualTo(9900L);
                    assertThat(line.remainingTimesBefore()).isEqualTo(1);
                    assertThat(line.usedTimesBefore()).isZero();
                    assertThat(line.status()).isEqualTo("requested");
                });
        assertThat(requestedLines.items())
                .extracting(TicketRefundLineRecord::entitlementId)
                .containsExactlyInAnyOrderElementsOf(ticketingRepository.listIssuedEntitlementsByOrderId(order.id()).stream()
                        .map(IssuedEntitlementRecord::id)
                        .toList());

        service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-lines-approve",
                "approve",
                "approved",
                List.of()
        ));

        assertThat(service.listTicketRefundLines(refund.id()).items())
                .extracting(TicketRefundLineRecord::status)
                .containsOnly("approved");
        assertThat(ticketingRepository.listIssuedEntitlementsByOrderId(order.id()))
                .extracting(IssuedEntitlementRecord::entitlementCode)
                .containsExactlyInAnyOrderElementsOf(issued.issuedEntitlements().stream()
                        .map(IssuedEntitlementResponse::entitlementCode)
                        .toList());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_refund.requested", "ticket_refund_line.viewed", "ticket_refund.approved");
    }

    @Test
    void dynamicPricingRuleSimulationDoesNotChangeOrderPricingUntilFutureIntegration() {
        TicketTypeRecord ticketType = createTicketType("dynamic-pricing");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-rule-create",
                "summer weekend uplift",
                List.of("att_dynamic"),
                ticketType.id(),
                "Summer weekend uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "percent_delta",
                new BigDecimal("20.00"),
                null,
                9900L,
                15000L,
                Map.of("inventory_level", "low")
        ));

        TicketDynamicPricingSimulationResponse simulation = service.simulateDynamicPricing(new TicketDynamicPricingSimulationRequest(
                "dynamic-simulate",
                ticketType.id(),
                LocalDate.parse("2026-06-20"),
                "web",
                Map.of("inventory_level", "low")
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-unaffected",
                "create order",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic"
        ));

        assertThat(draft.status()).isEqualTo("draft");
        assertThat(simulation)
                .extracting(
                        TicketDynamicPricingSimulationResponse::basePriceCent,
                        TicketDynamicPricingSimulationResponse::adjustmentCent,
                        TicketDynamicPricingSimulationResponse::finalPriceCent,
                        TicketDynamicPricingSimulationResponse::matchedRuleId
                )
                .containsExactly(11900L, 2380L, 14280L, draft.id());
        assertThat(order.amountCent()).isEqualTo(11900L);
        assertThat(order.dynamicPricingRuleId()).isNull();
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isZero();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_dynamic_pricing.changed", "ticket_dynamic_pricing.simulated", "ticket_order.created");
    }

    @Test
    void publishedDynamicPricingRuleChangesFormalOrderAmount() {
        TicketTypeRecord ticketType = createTicketType("dynamic-order-applied");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-order-applied-create",
                "publish uplift",
                List.of("att_dynamic_order"),
                ticketType.id(),
                "Published weekend uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                2000L,
                9900L,
                16000L,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-order-applied-publish", "publish", "approved for order pricing", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-applied-order",
                "create order with published dynamic pricing",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_order"
        ));

        assertThat(published.status()).isEqualTo("published");
        assertThat(order.amountCent()).isEqualTo(13900L);
        assertThat(order.dynamicPricingRuleId()).isEqualTo(published.id());
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(2000L);
    }

    @Test
    void publishedDynamicPricingRuleRequiresMatchingOrderContext() {
        TicketTypeRecord ticketType = createTicketType("dynamic-order-context");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-order-context-create",
                "conditional uplift",
                List.of(),
                ticketType.id(),
                "Conditional weekend uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "percent_delta",
                new BigDecimal("20.00"),
                null,
                9900L,
                15000L,
                Map.of("inventory_level", "low")
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-order-context-publish", "publish", "approved for conditional order pricing", List.of())
        );

        TicketOrderRecord unmatched = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-context-unmatched",
                "create without context",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_context"
        ));
        TicketOrderRecord matched = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-context-matched",
                "create with context",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_context",
                Map.of("inventory_level", "low")
        ));

        assertThat(unmatched.amountCent()).isEqualTo(11900L);
        assertThat(unmatched.dynamicPricingRuleId()).isNull();
        assertThat(matched.amountCent()).isEqualTo(14280L);
        assertThat(matched.dynamicPricingRuleId()).isEqualTo(published.id());
        assertThat(matched.baseAmountCent()).isEqualTo(11900L);
        assertThat(matched.dynamicAdjustmentCent()).isEqualTo(2380L);
    }

    @Test
    void createTicketOrderRecordsDynamicPricingTraceWhenRuleApplies() {
        TicketTypeRecord ticketType = createTicketType("dynamic-trace-applied");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-trace-applied-create",
                "publish discount",
                List.of("att_dynamic_trace"),
                ticketType.id(),
                "Published discount",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                -2000L,
                null,
                null,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-trace-applied-publish", "publish", "approved discount", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-trace-applied-order",
                "create order with published discount",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_trace"
        ));

        long unitPrice = 11900L;
        assertThat(order.baseAmountCent()).isEqualTo(unitPrice * 1);
        assertThat(order.dynamicAdjustmentCent()).isLessThan(0L);
        assertThat(order.baseAmountCent() + order.dynamicAdjustmentCent()).isEqualTo(order.amountCent());
        assertThat(order.dynamicPricingRuleId()).isEqualTo(published.id());
    }

    @Test
    void createTicketOrderRecordsZeroTraceWhenNoDynamicRule() {
        TicketTypeRecord ticketType = createTicketType("dynamic-trace-none");

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-trace-none-order",
                "create order without dynamic rule",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_trace_none"
        ));

        assertThat(order.dynamicAdjustmentCent()).isEqualTo(0L);
        assertThat(order.baseAmountCent()).isEqualTo(order.amountCent());
        assertThat(order.dynamicPricingRuleId()).isNull();
    }

    @Test
    void dynamicPricingTraceScalesWithQuantity() {
        TicketTypeRecord ticketType = createTicketType("dynamic-trace-scale");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-trace-scale-create",
                "publish discount",
                List.of("att_dynamic_scale"),
                ticketType.id(),
                "Published discount",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                -2000L,
                null,
                null,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-trace-scale-publish", "publish", "approved discount", List.of())
        );

        int quantity = 3;
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-trace-scale-order",
                "create order with quantity",
                List.of(),
                ticketType.id(),
                null,
                quantity,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_trace_scale"
        ));

        long unitPrice = 11900L;
        long unitAdjustment = -2000L;
        assertThat(order.dynamicPricingRuleId()).isEqualTo(published.id());
        assertThat(order.baseAmountCent()).isEqualTo(unitPrice * quantity);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(unitAdjustment * quantity);
        assertThat(order.baseAmountCent() + order.dynamicAdjustmentCent()).isEqualTo(order.amountCent());
    }

    @Test
    void dynamicPricingNeverProducesNegativeFinalPrice() {
        TicketTypeRecord ticketType = createTicketType("dynamic-non-negative");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-non-negative-create",
                "large discount",
                List.of(),
                ticketType.id(),
                "Large discount",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                -20000L,
                null,
                null,
                Map.of()
        ));
        service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-non-negative-publish", "publish", "approved discount", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-non-negative-order",
                "create non negative order",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_non_negative"
        ));

        assertThat(order.amountCent()).isZero();
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(-11900L);
    }

    @Test
    void publishedDynamicPricingUsesHighestPriorityMatchingRuleForFormalOrder() {
        TicketTypeRecord ticketType = createTicketType("dynamic-priority");
        TicketDynamicPricingRuleRecord lowPriority = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-priority-low",
                "low priority large uplift",
                List.of(),
                ticketType.id(),
                "Low priority large uplift",
                10,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                5000L,
                null,
                null,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord highPriority = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-priority-high",
                "high priority small uplift",
                List.of(),
                ticketType.id(),
                "High priority small uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                1000L,
                null,
                null,
                Map.of()
        ));
        service.publishDynamicPricingRule(
                lowPriority.id(),
                new StatusChangeRequest("dynamic-priority-low-publish", "publish", "approved low priority", List.of())
        );
        TicketDynamicPricingRuleRecord publishedHigh = service.publishDynamicPricingRule(
                highPriority.id(),
                new StatusChangeRequest("dynamic-priority-high-publish", "publish", "approved high priority", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-priority-order",
                "create order with competing dynamic rules",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_priority"
        ));

        assertThat(order.amountCent()).isEqualTo(12900L);
        assertThat(order.dynamicPricingRuleId()).isEqualTo(publishedHigh.id());
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(1000L);
    }

    @Test
    void dynamicPricingPublishAndNoMatchSimulationAreAudited() {
        TicketTypeRecord ticketType = createTicketType("dynamic-publish");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-publish-create",
                "weekday discount",
                List.of(),
                ticketType.id(),
                "Weekday discount",
                10,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "app",
                "fixed_delta",
                null,
                -1000L,
                6900L,
                15000L,
                Map.of("member_level", "gold")
        ));

        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-publish-once", "publish", "approved by ops", List.of("att_publish"))
        );
        TicketDynamicPricingSimulationResponse noMatch = service.simulateDynamicPricing(new TicketDynamicPricingSimulationRequest(
                "dynamic-no-match",
                ticketType.id(),
                LocalDate.parse("2026-07-01"),
                "app",
                Map.of("member_level", "gold")
        ));

        assertThat(published.status()).isEqualTo("published");
        assertThat(noMatch.matchedRuleId()).isNull();
        assertThat(noMatch.finalPriceCent()).isEqualTo(noMatch.basePriceCent());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_dynamic_pricing.published", "ticket_dynamic_pricing.simulated");
    }

    @Test
    void verificationFailsWhenEntitlementIsExpired() {
        TicketTypeRecord ticketType = createTicketType("expired-ticket");
        EntitlementTemplateRecord template = createTemplate("expired-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "expired-bind", "bind", List.of(), template.id(), 1, 2, 9900L, "default"
        ));
        IssuedTicketResponse issued = service.issueTicketTypeDirectly(ticketType.id(), new TicketIssueRequest(
                "expired-issue", "issue", List.of(), "order_expired", "manual", List.of("qr")
        ));
        String code = issued.issuedEntitlements().getFirst().entitlementCode();
        IssuedEntitlementRecord entitlement = ticketingRepository.findIssuedEntitlementByCode(code).orElseThrow();
        // Manually expire the entitlement by overwriting with past validTo
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                entitlement.id(), entitlement.entitlementCode(), entitlement.ticketTypeId(),
                entitlement.orderId(), entitlement.entitlementTemplateId(), entitlement.entitlementType(),
                entitlement.merchantId(), entitlement.venueId(), entitlement.pointId(),
                entitlement.totalTimes(), entitlement.usedTimes(), entitlement.valueAmountCent(),
                entitlement.status(), entitlement.validFrom(),
                Instant.now().minusSeconds(3600), // expired 1 hour ago
                entitlement.createdAt(), entitlement.updatedAt(), entitlement.numPeople()
        ));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                        "expired-verify", "qr", code, "dev_1", "gate_1", Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("entitlement has expired");

        VerificationPreviewResponse preview = service.previewVerification(new VerificationRequest(
                "expired-preview", "qr", code, "dev_1", "gate_1", Map.of()
        ));
        org.assertj.core.api.Assertions.assertThat(preview.allowed()).isFalse();
    }

    @Test
    void atomicVerificationPreventsDoubleUseWhenRemainingTimesExhausted() {
        TicketTypeRecord ticketType = createTicketType("atomic-ticket");
        EntitlementTemplateRecord template = createTemplate("atomic-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "atomic-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default"
        ));
        IssuedTicketResponse issued = service.issueTicketTypeDirectly(ticketType.id(), new TicketIssueRequest(
                "atomic-issue", "issue", List.of(), "order_atomic", "manual", List.of("qr")
        ));
        String code = issued.issuedEntitlements().getFirst().entitlementCode();

        service.createVerification(new VerificationRequest(
                "atomic-first", "qr", code, "dev_1", "gate_1", Map.of()
        ));

        // Second call with different idempotency key should fail – entitlement is now exhausted
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                        "atomic-second-key", "qr", code, "dev_1", "gate_1", Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("entitlement has no remaining times");

        org.assertj.core.api.Assertions.assertThat(ticketingRepository.findIssuedEntitlementByCode(code))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(1);
    }

    @Test
    void duplicateRefundRequestsCannotExceedOrderTotal() {
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("dup-refund");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();

        // First refund for full amount succeeds
        service.requestTicketRefund(new TicketRefundRequest(
                "dup-refund-1", "refund", List.of(), order.id(), order.amountCent(), "first", "original"
        ));

        // Second refund of any positive amount should fail (already reached order total)
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(new TicketRefundRequest(
                        "dup-refund-2", "refund", List.of(), order.id(), 1L, "second", "original"
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("refund amount exceeds order amount");
    }

    @Test
    void issueOrderRequiresPaymentForNonAdminChannel() {
        TicketTypeRecord ticketType = createTicketType("payment-guard");
        EntitlementTemplateRecord template = createTemplate("payment-guard-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "payment-guard-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default"
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "payment-guard-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "web", "13800138000", "customer_guard"
        ));

        org.assertj.core.api.Assertions.assertThat(order.payStatus()).isEqualTo("unpaid");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.issueTicketOrder(order.id(),
                        new StatusChangeRequest("payment-guard-issue", "issue", "force issue", List.of())
                ))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("order must be paid before issuing");
    }

    @Test
    void issueOrderToctouLockPreventsDoubleIssuance() {
        TicketTypeRecord ticketType = createTicketType("toctou-ticket");
        EntitlementTemplateRecord template = createTemplate("toctou-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "toctou-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default"
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "toctou-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", "customer_toctou"
        ));

        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "toctou-issue-once", "issue", "first issue", List.of()
        ));

        // tryLockOrderForIssuance must now return false (order is in 'issued' state)
        org.assertj.core.api.Assertions.assertThat(
                ticketingRepository.tryLockOrderForIssuance(order.id())
        ).isFalse();
        org.assertj.core.api.Assertions.assertThat(ticketingRepository.issuedEntitlements).hasSize(1);
    }

    @Test
    void offlineEntitlementFilterExcludesOtherMerchantsWhenMerchantIdPopulated() {
        TestSecurityActors.asMerchantUser("mch_filter_test");
        try {
            TicketTypeRecord ticketType = createTicketType("merchant-filter");
            EntitlementTemplateRecord template = createTemplate("merchant-filter-template", 1, 9900L);
            service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                    "merchant-filter-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default"
            ));
            service.issueTicketTypeDirectly(ticketType.id(), new TicketIssueRequest(
                    "merchant-filter-issue", "issue", List.of(), "order_mch_filter", "manual", List.of("qr")
            ));

            // Entitlement should have merchantId from the security context
            List<IssuedEntitlementRecord> entitlements = ticketingRepository.listIssuedEntitlements().stream()
                    .filter(e -> "order_mch_filter".equals(e.orderId()))
                    .toList();
            org.assertj.core.api.Assertions.assertThat(entitlements).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(entitlements.getFirst().merchantId()).isEqualTo("mch_filter_test");

            // Same merchant can see its own entitlement
            List<OfflineEntitlementResponse> visible = ticketingRepository.listOfflineEntitlements("mch_filter_test", null, 100);
            org.assertj.core.api.Assertions.assertThat(visible)
                    .extracting(OfflineEntitlementResponse::entitlementCode)
                    .contains(entitlements.getFirst().entitlementCode());

            // Different merchant cannot see it
            List<OfflineEntitlementResponse> invisible = ticketingRepository.listOfflineEntitlements("mch_other", null, 100);
            org.assertj.core.api.Assertions.assertThat(invisible)
                    .extracting(OfflineEntitlementResponse::entitlementCode)
                    .doesNotContain(entitlements.getFirst().entitlementCode());
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void unionPassEntitlementsInheritPerTemplateMerchantVenuePointNotIssuer() {
        // R5 审计点②：联合通票两个模板分属商户 A/B，出票后两权益各归属各自模板（非出票方）。
        TestSecurityActors.asMerchantUser("mch_issuer");
        try {
            Instant now = Instant.now();
            ticketingRepository.saveTicketType(new TicketTypeRecord(
                    "tt_union", "Union Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                    false, "published", null, now, now));
            ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                    "tpl_a", "Entry A", "entry", 1, 9900L, "active",
                    "mch_a", "venue_a", "gate_a", now, now));
            ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                    "tpl_b", "Entry B", "entry", 1, 9900L, "active",
                    "mch_b", "venue_b", "gate_b", now, now));
            ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                    "link_a", "tt_union", "tpl_a", 1, 1, 9900L, "default", now, now));
            ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                    "link_b", "tt_union", "tpl_b", 1, 1, 9900L, "default", now, now));

            service.issueTicketTypeDirectly("tt_union", new TicketIssueRequest(
                    "union-issue", "issue", List.of(), "order_union", "manual", List.of("qr")));

            List<IssuedEntitlementRecord> issued = ticketingRepository.issuedEntitlements.values().stream()
                    .filter(e -> "order_union".equals(e.orderId()))
                    .toList();
            assertThat(issued).hasSize(2);
            assertThat(issued).filteredOn(e -> "tpl_a".equals(e.entitlementTemplateId()))
                    .singleElement()
                    .satisfies(e -> {
                        assertThat(e.merchantId()).isEqualTo("mch_a");
                        assertThat(e.venueId()).isEqualTo("venue_a");
                        assertThat(e.pointId()).isEqualTo("gate_a");
                    });
            assertThat(issued).filteredOn(e -> "tpl_b".equals(e.entitlementTemplateId()))
                    .singleElement()
                    .satisfies(e -> {
                        assertThat(e.merchantId()).isEqualTo("mch_b");
                        assertThat(e.venueId()).isEqualTo("venue_b");
                        assertThat(e.pointId()).isEqualTo("gate_b");
                    });
            // 不得统一为出票方 mch_issuer
            assertThat(issued).extracting(IssuedEntitlementRecord::merchantId)
                    .doesNotContain("mch_issuer");
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void entitlementFallsBackToIssuerMerchantWhenTemplateMerchantNull() {
        // R5 审计点②：模板 merchant_id 为空时回退出票方（向后兼容既有单模板票）。
        TestSecurityActors.asMerchantUser("mch_issuer_fb");
        try {
            Instant now = Instant.now();
            ticketingRepository.saveTicketType(new TicketTypeRecord(
                    "tt_fb", "Single Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                    false, "published", null, now, now));
            ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                    "tpl_fb", "Entry FB", "entry", 1, 9900L, "active",
                    null, null, null, now, now));
            ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                    "link_fb", "tt_fb", "tpl_fb", 1, 1, 9900L, "default", now, now));

            service.issueTicketTypeDirectly("tt_fb", new TicketIssueRequest(
                    "fb-issue", "issue", List.of(), "order_fb", "manual", List.of("qr")));

            IssuedEntitlementRecord issued = ticketingRepository.issuedEntitlements.values().stream()
                    .filter(e -> "order_fb".equals(e.orderId()))
                    .findFirst().orElseThrow();
            assertThat(issued.merchantId()).isEqualTo("mch_issuer_fb");
            assertThat(issued.venueId()).isNull();
            assertThat(issued.pointId()).isNull();
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void validToUsesTicketTypeValidityDaysWithFallbackToOneDay() {
        // R5 审计点②：票种 validity_days=30 → valid_to=now+30天；为空回退 now+1天。
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_v30", "30d Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                false, "published", 30, now, now));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_v30", "Entry V30", "entry", 1, 9900L, "active", null, null, null, now, now));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_v30", "tt_v30", "tpl_v30", 1, 1, 9900L, "default", now, now));

        Instant before30 = Instant.now();
        service.issueTicketTypeDirectly("tt_v30", new TicketIssueRequest(
                "v30-issue", "issue", List.of(), "order_v30", "manual", List.of("qr")));
        IssuedEntitlementRecord ent30 = ticketingRepository.issuedEntitlements.values().stream()
                .filter(e -> "order_v30".equals(e.orderId())).findFirst().orElseThrow();
        long days30 = ChronoUnit.DAYS.between(ent30.validFrom(), ent30.validTo());
        assertThat(days30).isEqualTo(30);
        assertThat(ent30.validTo()).isAfter(before30.plus(29, ChronoUnit.DAYS));

        // validity_days 为空 → 回退 1 天
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_vnull", "Default Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                false, "published", null, now, now));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_vnull", "Entry Vnull", "entry", 1, 9900L, "active", null, null, null, now, now));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_vnull", "tt_vnull", "tpl_vnull", 1, 1, 9900L, "default", now, now));
        service.issueTicketTypeDirectly("tt_vnull", new TicketIssueRequest(
                "vnull-issue", "issue", List.of(), "order_vnull", "manual", List.of("qr")));
        IssuedEntitlementRecord entNull = ticketingRepository.issuedEntitlements.values().stream()
                .filter(e -> "order_vnull".equals(e.orderId())).findFirst().orElseThrow();
        assertThat(ChronoUnit.SECONDS.between(entNull.validFrom(), entNull.validTo())).isEqualTo(86_400);
    }

    private TicketTypeRecord createTicketType(String key) {
        return service.createTicketType(new TicketTypeRequest(
                key,
                "create",
                List.of(),
                "P0 Pass " + key,
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                List.of(),
                false,
                "active"
        ));
    }

    private void assertDerivedPriceRuleAmount(String key, long basePriceCent, BigDecimal discountRate, long amountCent) {
        TicketTypeRecord ticketType = service.createTicketType(new TicketTypeRequest(
                key + "-ticket",
                "create",
                List.of(),
                "P0 Decimal Pass " + key,
                "weekday_weekend_holiday",
                basePriceCent,
                basePriceCent,
                basePriceCent,
                List.of(),
                false,
                "active"
        ));
        TicketPriceRuleRecord rule = service.createTicketPriceRule(new TicketPriceRuleRequest(
                key + "-rule",
                "derive",
                List.of(),
                ticketType.id(),
                "channel",
                "derive_from_calendar_price",
                null,
                null,
                null,
                discountRate,
                "web",
                Map.of()
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                key + "-order",
                "create order",
                List.of(),
                ticketType.id(),
                rule.id(),
                1,
                LocalDate.parse("2026-06-15"),
                "web",
                "13800138000",
                "customer_" + key
        ));

        assertThat(rule.discountRate()).isEqualByComparingTo(discountRate);
        assertThat(order.amountCent()).isEqualTo(amountCent);
    }

    private EntitlementTemplateRecord createTemplate(String key, int totalTimes, long valueCent) {
        return service.createEntitlementTemplate(new EntitlementTemplateRequest(
                key,
                "create",
                List.of(),
                "P0 Entry " + key,
                "entry",
                totalTimes,
                valueCent,
                "active"
        ));
    }

    private IssuedEntitlementRecord issueSingleEntitlement(String key) {
        TicketTypeRecord ticketType = createTicketType(key + "-ticket");
        EntitlementTemplateRecord template = createTemplate(key + "-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                key + "-bind",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default"
        ));
        IssuedTicketResponse response = service.issueTicketTypeDirectly(ticketType.id(), new TicketIssueRequest(
                key + "-issue",
                "issue",
                List.of(),
                "order_" + key,
                "manual",
                List.of("qr")
        ));
        String entitlementCode = response.issuedEntitlements().getFirst().entitlementCode();
        return ticketingRepository.findIssuedEntitlementByCode(entitlementCode).orElseThrow();
    }

    private IssuedEntitlementRecord issueOrderEntitlement(String key) {
        TicketTypeRecord ticketType = createTicketType(key + "-ticket");
        EntitlementTemplateRecord template = createTemplate(key + "-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                key + "-bind",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default"
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                key + "-order",
                "create",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "admin",
                "13800138000",
                "customer_" + key
        ));
        IssuedTicketResponse response = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                key + "-issue-order",
                "issue",
                "issue",
                List.of()
        ));
        return ticketingRepository.findIssuedEntitlementByCode(response.issuedEntitlements().getFirst().entitlementCode()).orElseThrow();
    }

    private static final class InMemoryTicketingRepository implements TicketingRepository {
        private final Map<String, TicketTypeRecord> ticketTypes = new LinkedHashMap<>();
        private final Map<String, EntitlementTemplateRecord> entitlementTemplates = new LinkedHashMap<>();
        private final Map<String, TicketTypeEntitlementRecord> ticketTypeEntitlements = new LinkedHashMap<>();
        private final Map<String, IssuedEntitlementRecord> issuedEntitlements = new LinkedHashMap<>();
        private final Map<String, VerificationRecord> verifications = new LinkedHashMap<>();
        private final Map<String, TerminalSyncTaskRecord> terminalSyncTasks = new LinkedHashMap<>();
        private final Map<String, TicketPriceRuleRecord> priceRules = new LinkedHashMap<>();
        private final Map<String, TicketDynamicPricingRuleRecord> dynamicPricingRules = new LinkedHashMap<>();
        private final Map<String, TicketOrderRecord> ticketOrders = new LinkedHashMap<>();
        private final Map<String, TicketPaymentIntentRecord> paymentIntents = new LinkedHashMap<>();
        private final Map<String, TicketCredentialRecord> credentials = new LinkedHashMap<>();
        private final Map<String, TicketRefundRecord> refunds = new LinkedHashMap<>();
        private final Map<String, TicketRefundLineRecord> refundLines = new LinkedHashMap<>();
        private int entitlementTemplateSaveCount;

        @Override
        public TicketTypeRecord saveTicketType(TicketTypeRecord ticketType) {
            ticketTypes.put(ticketType.id(), ticketType);
            return ticketType;
        }

        @Override
        public List<TicketTypeRecord> listTicketTypes() {
            return new ArrayList<>(ticketTypes.values());
        }

        @Override
        public Optional<TicketTypeRecord> findTicketTypeById(String id) {
            return Optional.ofNullable(ticketTypes.get(id));
        }

        @Override
        public EntitlementTemplateRecord saveEntitlementTemplate(EntitlementTemplateRecord template) {
            entitlementTemplateSaveCount += 1;
            entitlementTemplates.put(template.id(), template);
            return template;
        }

        @Override
        public List<EntitlementTemplateRecord> listEntitlementTemplates() {
            return new ArrayList<>(entitlementTemplates.values());
        }

        @Override
        public Optional<EntitlementTemplateRecord> findEntitlementTemplateById(String id) {
            return Optional.ofNullable(entitlementTemplates.get(id));
        }

        @Override
        public TicketTypeEntitlementRecord saveTicketTypeEntitlement(TicketTypeEntitlementRecord entitlement) {
            ticketTypeEntitlements.put(entitlement.id(), entitlement);
            return entitlement;
        }

        @Override
        public List<TicketTypeEntitlementRecord> listTicketTypeEntitlements(String ticketTypeId) {
            return ticketTypeEntitlements.values().stream()
                    .filter(entitlement -> ticketTypeId.equals(entitlement.ticketTypeId()))
                    .toList();
        }

        @Override
        public IssuedEntitlementRecord saveIssuedEntitlement(IssuedEntitlementRecord entitlement) {
            issuedEntitlements.put(entitlement.id(), entitlement);
            return entitlement;
        }

        @Override
        public Optional<IssuedEntitlementRecord> findIssuedEntitlementByCode(String entitlementCode) {
            return issuedEntitlements.values().stream()
                    .filter(entitlement -> entitlementCode.equals(entitlement.entitlementCode()))
                    .findFirst();
        }

        @Override
        public IssuedEntitlementRecord updateIssuedEntitlement(IssuedEntitlementRecord entitlement) {
            issuedEntitlements.put(entitlement.id(), entitlement);
            return entitlement;
        }

        @Override
        public List<OfflineEntitlementResponse> listOfflineEntitlements(String merchantId, String venueId, int limit) {
            return issuedEntitlements.values().stream()
                    .filter(entitlement -> merchantId == null || merchantId.equals(entitlement.merchantId()))
                    .filter(entitlement -> venueId == null || venueId.equals(entitlement.venueId()))
                    .limit(limit)
                    .map(TicketEntitlementMapper::toOfflineEntitlementResponse)
                    .toList();
        }

        @Override
        public VerificationRecord saveVerification(VerificationRecord verification) {
            verifications.put(verification.id(), verification);
            return verification;
        }

        @Override
        public List<VerificationRecord> listVerifications() {
            return new ArrayList<>(verifications.values());
        }

        @Override
        public Optional<VerificationRecord> findVerificationById(String id) {
            return Optional.ofNullable(verifications.get(id));
        }

        @Override
        public void markVerificationReversed(String id, String reversedBy, java.time.Instant reversedAt) {
            VerificationRecord v = verifications.get(id);
            if (v == null) {
                return;
            }
            verifications.put(id, new VerificationRecord(
                    v.id(), v.verificationNo(), v.entitlementId(), v.entitlementCode(), v.credentialType(),
                    v.credentialPayload(), v.deviceId(), v.pointId(), "reversed", v.resultReason(), v.sourceType(),
                    v.clientTaskId(), v.createdAt(), v.nonce(), v.staffId(), v.merchantId(), v.windowValid(),
                    v.signatureValid(), v.verifiedPeople(), v.reversesVerificationId(), reversedBy, reversedAt
            ));
        }

        @Override
        public Optional<TerminalSyncTaskRecord> findTerminalSyncTask(String deviceCode, String clientTaskId) {
            return Optional.ofNullable(terminalSyncTasks.get(deviceCode + ":" + clientTaskId));
        }

        @Override
        public TerminalSyncTaskRecord saveTerminalSyncTask(TerminalSyncTaskRecord task) {
            terminalSyncTasks.put(task.deviceCode() + ":" + task.clientTaskId(), task);
            return task;
        }

        @Override
        public TicketPriceRuleRecord savePriceRule(TicketPriceRuleRecord rule) {
            priceRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<TicketPriceRuleRecord> listPriceRules() {
            return new ArrayList<>(priceRules.values());
        }

        @Override
        public Optional<TicketPriceRuleRecord> findPriceRuleById(String id) {
            return Optional.ofNullable(priceRules.get(id));
        }

        @Override
        public TicketDynamicPricingRuleRecord saveDynamicPricingRule(TicketDynamicPricingRuleRecord rule) {
            dynamicPricingRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<TicketDynamicPricingRuleRecord> listDynamicPricingRules() {
            return new ArrayList<>(dynamicPricingRules.values());
        }

        @Override
        public Optional<TicketDynamicPricingRuleRecord> findDynamicPricingRuleById(String id) {
            return Optional.ofNullable(dynamicPricingRules.get(id));
        }

        @Override
        public TicketOrderRecord saveTicketOrder(TicketOrderRecord order) {
            ticketOrders.put(order.id(), order);
            return order;
        }

        @Override
        public Optional<TicketOrderRecord> findTicketOrderById(String id) {
            return Optional.ofNullable(ticketOrders.get(id));
        }

        @Override
        public TicketOrderRecord updateTicketOrder(TicketOrderRecord order) {
            ticketOrders.put(order.id(), order);
            return order;
        }

        @Override
        public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
            return ticketOrders.values().stream()
                    .filter(order -> customerId.equals(order.customerId()))
                    .toList();
        }

        @Override
        public TicketPaymentIntentRecord saveTicketPaymentIntent(TicketPaymentIntentRecord intent) {
            paymentIntents.put(intent.id(), intent);
            return intent;
        }

        @Override
        public List<IssuedEntitlementRecord> listIssuedEntitlements() {
            return new ArrayList<>(issuedEntitlements.values());
        }

        @Override
        public List<IssuedEntitlementRecord> listIssuedEntitlementsByOrderId(String orderId) {
            return issuedEntitlements.values().stream()
                    .filter(entitlement -> orderId.equals(entitlement.orderId()))
                    .toList();
        }

        @Override
        public Optional<IssuedEntitlementRecord> findIssuedEntitlementById(String id) {
            return Optional.ofNullable(issuedEntitlements.get(id));
        }

        @Override
        public TicketCredentialRecord saveTicketCredential(TicketCredentialRecord credential) {
            credentials.put(credential.credentialToken(), credential);
            return credential;
        }

        @Override
        public Optional<TicketCredentialRecord> findTicketCredentialByToken(String credentialToken) {
            return Optional.ofNullable(credentials.get(credentialToken));
        }

        @Override
        public TicketRefundRecord saveTicketRefund(TicketRefundRecord refund) {
            refunds.put(refund.id(), refund);
            return refund;
        }

        @Override
        public Optional<TicketRefundRecord> findTicketRefundById(String id) {
            return Optional.ofNullable(refunds.get(id));
        }

        @Override
        public TicketRefundLineRecord saveTicketRefundLine(TicketRefundLineRecord line) {
            refundLines.put(line.id(), line);
            return line;
        }

        @Override
        public List<TicketRefundLineRecord> listTicketRefundLines(String refundId) {
            return refundLines.values().stream()
                    .filter(line -> refundId.equals(line.refundId()))
                    .toList();
        }

        @Override
        public long sumPendingRefundAmountByOrderId(String orderId) {
            return refunds.values().stream()
                    .filter(r -> orderId.equals(r.targetId()))
                    .filter(r -> "requested".equals(r.status()) || "approved".equals(r.status()))
                    .mapToLong(TicketRefundRecord::refundAmountCent)
                    .sum();
        }

        @Override
        public java.util.Optional<IssuedEntitlementRecord> atomicUseEntitlement(String id, java.time.Instant updatedAt) {
            IssuedEntitlementRecord entitlement = issuedEntitlements.get(id);
            if (entitlement == null || entitlement.remainingTimes() <= 0) {
                return java.util.Optional.empty();
            }
            IssuedEntitlementRecord updated = entitlement.useOnce(updatedAt);
            issuedEntitlements.put(id, updated);
            return java.util.Optional.of(updated);
        }

        @Override
        public java.util.Optional<IssuedEntitlementRecord> atomicReverseEntitlement(String id, java.time.Instant updatedAt) {
            IssuedEntitlementRecord entitlement = issuedEntitlements.get(id);
            if (entitlement == null || entitlement.usedTimes() <= 0) {
                return java.util.Optional.empty();
            }
            IssuedEntitlementRecord updated = new IssuedEntitlementRecord(
                    entitlement.id(), entitlement.entitlementCode(), entitlement.ticketTypeId(), entitlement.orderId(),
                    entitlement.entitlementTemplateId(), entitlement.entitlementType(), entitlement.merchantId(),
                    entitlement.venueId(), entitlement.pointId(), entitlement.totalTimes(),
                    Math.max(entitlement.usedTimes() - 1, 0), entitlement.valueAmountCent(),
                    "active", entitlement.validFrom(), entitlement.validTo(), entitlement.createdAt(), updatedAt,
                    entitlement.numPeople()
            );
            issuedEntitlements.put(id, updated);
            return java.util.Optional.of(updated);
        }

        @Override
        public boolean tryLockOrderForIssuance(String orderId) {
            TicketOrderRecord order = ticketOrders.get(orderId);
            if (order == null || !"not_issued".equals(order.issueStatus())) {
                return false;
            }
            ticketOrders.put(orderId, new TicketOrderRecord(
                    order.id(), order.orderNo(), order.customerId(), order.ticketTypeId(),
                    order.priceRuleId(), order.quantity(), order.visitDate(), order.salesChannel(),
                    order.buyerPhone(), order.amountCent(), order.dynamicPricingRuleId(),
                    order.baseAmountCent(), order.dynamicAdjustmentCent(), order.currency(),
                    order.payStatus(), "issuing", order.status(), order.createdAt(), order.updatedAt()
            ));
            return true;
        }

        @Override
        public String findLatestFlywayVersion() {
            return "9";
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
